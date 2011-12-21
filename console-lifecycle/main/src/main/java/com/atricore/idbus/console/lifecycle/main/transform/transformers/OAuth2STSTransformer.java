package com.atricore.idbus.console.lifecycle.main.transform.transformers;

import com.atricore.idbus.console.lifecycle.main.domain.metadata.IdentityProvider;
import com.atricore.idbus.console.lifecycle.main.exception.TransformException;
import com.atricore.idbus.console.lifecycle.main.transform.TransformEvent;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Bean;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Beans;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.oauth2.common.AESTokenEncrypter;
import org.atricore.idbus.capabilities.oauth2.common.HMACTokenSigner;
import org.atricore.idbus.capabilities.oauth2.main.OAuth2Mediator;
import org.atricore.idbus.capabilities.oauth2.main.emitter.OAuth2AccessTokenEmitter;
import org.atricore.idbus.kernel.main.mediation.provider.IdentityProviderImpl;

import java.util.Collection;

import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.*;
import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.newBean;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class OAuth2STSTransformer extends AbstractTransformer {

    private static final Log logger = LogFactory.getLog(STSTransformer.class);

    @Override
    public boolean accept(TransformEvent event) {
        return event.getData() instanceof IdentityProvider &&
                !((IdentityProvider)event.getData()).isRemote();
    }

    @Override
    public void before(TransformEvent event) throws TransformException {

        Beans idpBeans = (Beans) event.getContext().get("idpBeans");

        IdentityProvider provider = (IdentityProvider) event.getData();

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


        Collection<Bean> mediators = getBeansOfType(idpBeans, OAuth2Mediator.class.getName());

        if (mediators.size() != 1)
            throw new TransformException("Too many/few mediators defined " + mediators.size());

        Bean mediatorBean = mediators.iterator().next();

        // Inject identity Manager
        setPropertyRef(oauth2StsEmitter, "identityManager", idpBean.getName() + "-identity-manager");

        String oauth2Key = provider.getOauth2Key();

        /* Configure AES encryption */
        Bean aesEncrypter = newAnonymousBean(AESTokenEncrypter.class);
        setPropertyValue(aesEncrypter, "base64key", oauth2Key);
        setPropertyBean(oauth2StsEmitter, "tokenEncrypter", aesEncrypter);

        /* Configure H-MAC signature support */
        Bean hmacSigner = newAnonymousBean(HMACTokenSigner.class);
        setPropertyValue(hmacSigner, "key", oauth2Key);
        setPropertyBean(oauth2StsEmitter, "tokenSigner", hmacSigner);

        // Add emitter to STS
        addPropertyBean(sts, "emitters", oauth2StsEmitter);
    }

}
