package com.atricore.idbus.console.lifecycle.main.transform.transformers.oauth2;

import com.atricore.idbus.console.lifecycle.main.domain.metadata.*;
import com.atricore.idbus.console.lifecycle.main.exception.TransformException;
import com.atricore.idbus.console.lifecycle.main.transform.TransformEvent;
import com.atricore.idbus.console.lifecycle.main.transform.transformers.AbstractTransformer;
import com.atricore.idbus.console.lifecycle.main.transform.transformers.sso.STSTransformer;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Bean;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Beans;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.oauth2.common.AESTokenEncrypter;
import org.atricore.idbus.capabilities.oauth2.common.HMACTokenSigner;
import org.atricore.idbus.capabilities.oauth2.main.OAuth2IdPMediator;
import org.atricore.idbus.capabilities.oauth2.main.emitter.OAuth2AccessTokenEmitter;
import org.atricore.idbus.kernel.main.mediation.provider.IdentityProviderImpl;

import java.util.Collection;

import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.*;
import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.newBean;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class OAuth2STSTransformer extends AbstractTransformer {

    private static final Log logger = LogFactory.getLog(com.atricore.idbus.console.lifecycle.main.transform.transformers.sso.STSTransformer.class);

    @Override
    public boolean accept(TransformEvent event) {
        // Only work for Local IdPs with OAuth 2.0 support enabled
        if (event.getData() instanceof IdentityProvider &&
                !((IdentityProvider)event.getData()).isRemote() &&
                ((IdentityProvider)event.getData()).isOauth2Enabled()) {
            return true;
        }

        if (event.getData() instanceof ServiceProviderChannel) {

            // TODO : Check for OAUTH2 Enabled on proxy

            ServiceProviderChannel spChannel = (ServiceProviderChannel) event.getData();
            FederatedConnection fc = (FederatedConnection) event.getContext().getParentNode();

            if (fc.getRoleA() instanceof ExternalSaml2IdentityProvider && fc.getRoleA().isRemote())
                return true;
            // TODO : Change this once the front-end supports it
            /* return spChannel.isOverrideProviderSetup() && fc.getRoleA() instanceof ExternalSaml2IdentityProvider && fc.getRoleA().isRemote();
            */
            if (fc.getRoleB() instanceof ExternalSaml2IdentityProvider && fc.getRoleB().isRemote()) {
                return true;
                // TODO : Change this once the front-end supports it
                /* return spChannel.isOverrideProviderSetup() && fc.getRoleB() instanceof ExternalSaml2IdentityProvider && fc.getRoleB().isRemote(); */
            }

        }

        return false;
    }

    @Override
    public void before(TransformEvent event) throws TransformException {
        boolean isProxy = false;

        FederatedProvider provider = null;
        if (event.getData() instanceof FederatedProvider) {
            provider = (FederatedProvider) event.getData();
            isProxy = false;
        } else if (event.getData() instanceof ServiceProviderChannel) {
            ServiceProviderChannel spChannel = (ServiceProviderChannel) event.getData();
            FederatedConnection fc = (FederatedConnection) event.getContext().getParentNode();
            isProxy = true;
            if (fc.getRoleA() instanceof ExternalSaml2IdentityProvider && fc.getRoleA().isRemote())
                provider = fc.getRoleA();
            else if (fc.getRoleB() instanceof ExternalSaml2IdentityProvider && fc.getRoleB().isRemote()) {
                provider = fc.getRoleB();
            }
        }


        Beans idpBeans = isProxy ? (Beans) event.getContext().get("idpProxyBeans") : (Beans) event.getContext().get("idpBeans");

        //provider = (IdentityProvider) event.getData();

        if (logger.isTraceEnabled())
            logger.trace("Generating OAUTH2 STS Beans for IdP " + provider.getName());


        // ----------------------------------------
        // Get IDP Bean
        // ----------------------------------------
        Bean idpBean = null;
        Collection<Bean> b = getBeansOfType(idpBeans, IdentityProviderImpl.class.getName());
        if (b.size() != 1) {
            throw new TransformException("Invalid IdP definition count : " + b.size());
        }
        idpBean = b.iterator().next();

        // ----------------------------------------
        // STS, must be already created
        // ----------------------------------------
        Bean sts = getBean(idpBeans, idpBean.getName() + "-sts");

        // ----------------------------------------
        // Emitters
        // ----------------------------------------
        Bean oauth2StsEmitter = newBean(idpBeans,
                idpBean.getName() + "-oauth2-accesstoken-emitter",
                OAuth2AccessTokenEmitter.class.getName());
        setPropertyValue(oauth2StsEmitter, "id", oauth2StsEmitter.getName());

        // identityPlanRegistry
        setPropertyRef(oauth2StsEmitter, "identityPlanRegistry", "identity-plans-registry");

        Collection<Bean> mediators = getBeansOfType(idpBeans, OAuth2IdPMediator.class.getName());

        if (mediators.size() != 1)
            throw new TransformException("Too many/few mediators defined " + mediators.size());

        Bean mediatorBean = mediators.iterator().next();

        Bean idMgr = getBean(idpBeans, idpBean.getName() + "-identity-manager");

        // Inject identity Manager
        if (idMgr != null) {
            setPropertyRef(oauth2StsEmitter, "identityManager", idpBean.getName() + "-identity-manager");
        }

        String oauth2Key = "@WSX3edc";// TODO !!!!! provider.getOauth2Key();

        /* Configure AES encryption */
        Bean aesEncrypter = newAnonymousBean(AESTokenEncrypter.class);
        setPropertyValue(aesEncrypter, "base64key", oauth2Key);
        setPropertyBean(oauth2StsEmitter, "tokenEncrypter", aesEncrypter);

        /* Configure H-MAC signature support */
        Bean hmacSigner = newAnonymousBean(HMACTokenSigner.class);
        setPropertyValue(hmacSigner, "key", oauth2Key);
        setPropertyBean(oauth2StsEmitter, "tokenSigner", hmacSigner);
        setPropertyValue(oauth2StsEmitter, "emitWhenNotTargeted", "true");

        // Add emitter to STS : the emitter MUST be the first in the list (or run before SAML2) // TODO : Mange dependencies ?
        insertPropertyBean(sts, "emitters", oauth2StsEmitter);
    }

}