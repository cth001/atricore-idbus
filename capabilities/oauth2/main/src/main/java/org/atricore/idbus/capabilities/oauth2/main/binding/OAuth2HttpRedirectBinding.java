package org.atricore.idbus.capabilities.oauth2.main.binding;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.common.oauth._2_0.protocol.AccessTokenResponseType;
import org.atricore.idbus.kernel.main.federation.metadata.EndpointDescriptor;
import org.atricore.idbus.kernel.main.mediation.Channel;
import org.atricore.idbus.kernel.main.mediation.MediationMessage;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.AbstractMediationHttpBinding;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationMessage;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class OAuth2HttpRedirectBinding extends AbstractMediationHttpBinding {

    private static final Log logger = LogFactory.getLog(OAuth2HttpRedirectBinding.class);

    public OAuth2HttpRedirectBinding(Channel channel) {
        super(OAuth2Binding.OAUTH2_REDIRECT.getValue(), channel);
    }

    public MediationMessage createMessage(CamelMediationMessage message) {
        throw new UnsupportedOperationException("Not Implemented!");
    }

    public void copyMessageToExchange(CamelMediationMessage oauth2Out, Exchange exchange) {
        MediationMessage out = oauth2Out.getMessage();
        EndpointDescriptor ed = out.getDestination();

        assert ed != null : "Mediation Response MUST Provide a destination";

        String restfulQueryStr = ed.getResponseLocation() != null ? ed.getResponseLocation() : ed.getLocation();

        if (out.getContent() instanceof AccessTokenResponseType) {
            // This could be some kind of token, lets find out ...
            if (out.getContentType().equals("AccessTokenResponse")) {

                // We're sending an access token
                String token = ((AccessTokenResponseType) out.getContent()).getAccessToken();
                restfulQueryStr += (restfulQueryStr.contains("?") ? "&" : "?");
                try {
                    restfulQueryStr += "access_token=" + URLEncoder.encode(token, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    logger.error("Cannot encode access token : " + e.getMessage(), e);
                    throw new RuntimeException("Cannot encode access token : " + e.getMessage(), e);
                }

            } else if (out.getContentType().equals("ErrorCode")) {

                // We're sending an error
                restfulQueryStr += (restfulQueryStr.contains("?") ? "&" : "?");
                try {
                    String errorCode = ((AccessTokenResponseType) out.getContent()).getError().value();
                    restfulQueryStr += "error_code=" + URLEncoder.encode(errorCode, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    logger.error("Cannot encode error code : " + e.getMessage(), e);
                    throw new RuntimeException("Cannot encode error code : " + e.getMessage(), e);
                }

            } else {
                throw new IllegalStateException("String Content type supported for OAuth2 HTTP Restful bidning " + out.getContentType() + " ["+out.getContent()+"]");
            }
        } else {
            throw new IllegalStateException("Content type supported for OAuth2 HTTP Redirect bidning " + out.getContentType() + " ["+out.getContent()+"]");
        }

        Message httpOut = exchange.getOut();
        String oauth2ResfulLocation = restfulQueryStr;

        if (logger.isDebugEnabled())
            logger.debug("Redirecting to " + oauth2ResfulLocation);

        try {

            // ------------------------------------------------------------
            // Prepare HTTP Resposne
            // ------------------------------------------------------------
            copyBackState(out.getState(), exchange);

            httpOut.getHeaders().put("Cache-Control", "no-cache, no-store");
            httpOut.getHeaders().put("Pragma", "no-cache");
            httpOut.getHeaders().put("http.responseCode", 302);
            httpOut.getHeaders().put("Content-Type", "text/html");
            httpOut.getHeaders().put("Location", oauth2ResfulLocation);
            handleCrossOriginResourceSharing(exchange);

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }
}
