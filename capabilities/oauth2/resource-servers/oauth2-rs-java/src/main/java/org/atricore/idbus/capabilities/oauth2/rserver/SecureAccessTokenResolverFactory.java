package org.atricore.idbus.capabilities.oauth2.rserver;

import org.apache.commons.codec.binary.Base64;
import org.atricore.idbus.capabilities.oauth2.common.AESTokenEncrypter;
import org.atricore.idbus.capabilities.oauth2.common.HMACTokenSigner;

import java.io.IOException;

/**
 * Secure resolver factory, for now it fixes HMAC-SHA1 and AES for signing and encrypting
 *
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class SecureAccessTokenResolverFactory extends AccessTokenResolverFactory {

    public static final String SHARED_SECRECT_PROPERTY = "org.atricore.idbus.capabilities.oauth2.key";

    public static final String SHARED_SECRECT_SIGN_PROPERTY = "org.atricore.idbus.capabilities.oauth2.signKey";

    public static final String SHARED_SECRECT_ENC_PROPERTY = "org.atricore.idbus.capabilities.oauth2.encryptKey";

    public static final String TOKEN_VALIDITY_INTERVAL_PROPERTY = "org.atricore.idbus.capabilities.oauth2.accessTokenValidityInterval";

    public AccessTokenResolver doMakeResolver() {

        // Resolver
        try {

            if (config == null)
                loadConfig();

            SecureAccessTokenResolverImpl r = new SecureAccessTokenResolverImpl();
            String defaultKey = config.getProperty(SHARED_SECRECT_PROPERTY);
            String encKey = config.getProperty(SHARED_SECRECT_ENC_PROPERTY, defaultKey);
            String signKey = config.getProperty(SHARED_SECRECT_SIGN_PROPERTY, defaultKey);
            long tkValidityInterval = Long.parseLong(config.getProperty(TOKEN_VALIDITY_INTERVAL_PROPERTY, "30000"));

            if (defaultKey == null && signKey == null)
                throw new RuntimeException("Secure Token Resolver requires a signature verification key");

            if (defaultKey == null && encKey == null)
                throw new RuntimeException("Secure Token Resolver requires an encryption verification key");

            // HMAC Signer
            HMACTokenSigner signer = new HMACTokenSigner();
            signer.setKey(signKey);
            r.setTokenSigner(signer);

            // AES Encrypter
            AESTokenEncrypter encrypter = new AESTokenEncrypter();
            encrypter.setBase64key(encKey);
            r.setTokenEncrypter(encrypter);

            r.setTokenValidityInterval(tkValidityInterval);

            return r;

        } catch (OAuth2RServerException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
