package com.atricore.idbus.console.lifecycle.main.transform.transformers;

import com.atricore.idbus.console.lifecycle.main.domain.IdentityAppliance;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.AuthenticationMechanism;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.IdentityProvider;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.TwoFactorAuthentication;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.WindowsAuthentication;
import com.atricore.idbus.console.lifecycle.main.exception.TransformException;
import com.atricore.idbus.console.lifecycle.main.transform.TransformEvent;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Bean;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Beans;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.sso.main.binding.SamlR2BindingFactory;
import org.atricore.idbus.capabilities.sso.main.binding.logging.SamlR2LogMessageBuilder;
import org.atricore.idbus.capabilities.sso.main.claims.SSOClaimsMediator;
import org.atricore.idbus.capabilities.sso.support.auth.AuthnCtxClass;
import org.atricore.idbus.capabilities.sso.support.binding.SSOBinding;
import org.atricore.idbus.kernel.main.mediation.camel.component.logging.CamelLogMessageBuilder;
import org.atricore.idbus.kernel.main.mediation.camel.component.logging.HttpLogMessageBuilder;
import org.atricore.idbus.kernel.main.mediation.camel.logging.DefaultMediationLogger;
import org.atricore.idbus.kernel.main.mediation.claim.ClaimChannelImpl;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpointImpl;
import org.atricore.idbus.kernel.main.mediation.osgi.OsgiIdentityMediationUnit;
import org.atricore.idbus.kernel.main.mediation.provider.IdentityProviderImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.*;
import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.addPropertyBeansAsRefs;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class TwoFactorAuthenticationClaimsChannelTransformer extends AbstractTransformer {

    private static final Log logger = LogFactory.getLog(TwoFactorAuthenticationClaimsChannelTransformer.class);

    @Override
    public boolean accept(TransformEvent event) {
        if (!(event.getData() instanceof IdentityProvider))
            return false;
        IdentityProvider idp = (IdentityProvider)event.getData();
        if (idp.isRemote())
            return false;

        if (idp.getAuthenticationMechanisms() == null)
            return false;

        for (AuthenticationMechanism a : idp.getAuthenticationMechanisms()) {
            if (a instanceof TwoFactorAuthentication)
                return true;
        }

        // None of the authn mechanisms is supported!
        return false;
    }

    /**
     *  TODO : Support multiple claim channels per IDP!
     * @param event
     * @throws com.atricore.idbus.console.lifecycle.main.exception.TransformException
     */
    @Override
    public void before(TransformEvent event) throws TransformException {

        Beans idpBeans = (Beans) event.getContext().get("idpBeans");

        IdentityProvider provider = (IdentityProvider) event.getData();
        IdentityAppliance appliance = event.getContext().getProject().getIdAppliance();

        if (logger.isTraceEnabled())
            logger.trace("Generating Claim Channel Beans for IDP Channel " + provider.getName());

        Beans baseBeans = (Beans) event.getContext().get("beans");
        Beans beansOsgi = (Beans) event.getContext().get("beansOsgi");

        Bean idpBean = null;
        Collection<Bean> b = getBeansOfType(idpBeans, IdentityProviderImpl.class.getName());
        if (b.size() != 1) {
            throw new TransformException("Invalid IdP definition count : " + b.size());
        }
        idpBean = b.iterator().next();



        for (AuthenticationMechanism authnMechanism : provider.getAuthenticationMechanisms()) {
            // Bind authn is a variant of basic authn
            if (authnMechanism instanceof TwoFactorAuthentication) {

                // ----------------------------------------
                // Claims Channel
                // ----------------------------------------
                String claimChannelBeanName = normalizeBeanName(authnMechanism.getName() + "-claims-channel");

                Bean claimChannelBean = newBean(idpBeans, claimChannelBeanName, ClaimChannelImpl.class);

                // name
                setPropertyValue(claimChannelBean, "name", claimChannelBean.getName());

                // location
                String s = authnMechanism.getDelegatedAuthentication().getAuthnService().getName().toUpperCase();
                String locationUrl = resolveLocationUrl(provider) + "/CC/" + s;
                setPropertyValue(claimChannelBean, "location", locationUrl);

                // endpoints
                List<Bean> ccEndpoints = new ArrayList<Bean>();

                Bean cc2faArtifact = newAnonymousBean(IdentityMediationEndpointImpl.class);
                cc2faArtifact.setName(idpBean.getName() + "-cc-2fa-artifact");
                setPropertyValue(cc2faArtifact, "name", cc2faArtifact.getName());
                setPropertyValue(cc2faArtifact, "binding", SSOBinding.SSO_ARTIFACT.getValue());
                setPropertyValue(cc2faArtifact, "location", "/2FA/ARTIFACT");
                setPropertyValue(cc2faArtifact, "responseLocation", "/2FA/POST-RESP");
                setPropertyValue(cc2faArtifact, "type", AuthnCtxClass.TIME_SYNC_TOKEN_AUTHN_CTX.getValue());
                ccEndpoints.add(cc2faArtifact);

                Bean cc2faPost = newAnonymousBean(IdentityMediationEndpointImpl.class);
                cc2faPost.setName(idpBean.getName() + "-cc-2fa-post");
                setPropertyValue(cc2faPost, "name", cc2faPost.getName());
                setPropertyValue(cc2faPost, "binding", SSOBinding.SSO_POST.getValue());
                setPropertyValue(cc2faPost, "location", "/2FA/POST");
                setPropertyValue(cc2faPost, "type", AuthnCtxClass.TIME_SYNC_TOKEN_AUTHN_CTX.getValue());
                ccEndpoints.add(cc2faPost);

                setPropertyAsBeans(claimChannelBean, "endpoints", ccEndpoints);

                // ----------------------------------------
                // Claims Mediator
                // ----------------------------------------
                Bean ccMediator = newBean(idpBeans, claimChannelBeanName + "-mediator", SSOClaimsMediator.class);

                // logMessages
                setPropertyValue(ccMediator, "logMessages", true);

                // 2faAuthnUILocation
                setPropertyValue(ccMediator, "twoFactorAuthnUILocation", resolveLocationBaseUrl(provider) + "/idbus-ui/claims/username-passcode.do");

                // artifactQueueManager
                setPropertyRef(ccMediator, "artifactQueueManager", provider.getIdentityAppliance().getName() + "-aqm");

                // bindingFactory
                setPropertyBean(ccMediator, "bindingFactory", newAnonymousBean(SamlR2BindingFactory.class));

                List<Bean> ccLogBuilders = new ArrayList<Bean>();
                ccLogBuilders.add(newAnonymousBean(SamlR2LogMessageBuilder.class));
                ccLogBuilders.add(newAnonymousBean(CamelLogMessageBuilder.class));
                ccLogBuilders.add(newAnonymousBean(HttpLogMessageBuilder.class));

                Bean ccLogger = newBean(idpBeans, claimChannelBeanName + "-mediation-logger", DefaultMediationLogger.class.getName());
                setPropertyValue(ccLogger, "category", appliance.getNamespace() + "." + appliance.getName() + ".wire.cc1");
                setPropertyAsBeans(ccLogger, "messageBuilders", ccLogBuilders);

                // logger
                setPropertyBean(ccMediator, "logger", ccLogger);

                // errorUrl
                setPropertyValue(ccMediator, "errorUrl", resolveLocationBaseUrl(provider) + "/idbus-ui/error.do");

                // warningUrl
                setPropertyValue(ccMediator, "warningUrl", resolveLocationBaseUrl(provider) + "/idbus-ui/warn/policy-enforcement.do");

                // identityMediator
                setPropertyRef(claimChannelBean, "identityMediator", ccMediator.getName());

                // provider
                setPropertyRef(claimChannelBean, "provider", idpBean.getName());

                // unitContainer
                setPropertyRef(claimChannelBean, "unitContainer", provider.getIdentityAppliance().getName() + "-container");

                // Mediation Unit
                Collection<Bean> mus = getBeansOfType(baseBeans, OsgiIdentityMediationUnit.class.getName());
                if (mus.size() == 1) {
                    Bean mu = mus.iterator().next();
                    addPropertyBeansAsRefs(mu, "channels", claimChannelBean);
                } else {
                    throw new TransformException("One and only one Identity Mediation Unit is expected, found " + mus.size());
                }
            }
        }


    }
}
