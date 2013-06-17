package org.atricore.idbus.capabilities.sso.main.select.selectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.sso.main.SSOException;
import org.atricore.idbus.capabilities.sso.main.select.SSOEntitySelectorMediator;
import org.atricore.idbus.capabilities.sso.main.select.internal.EntitySelectionState;
import org.atricore.idbus.capabilities.sso.main.select.spi.AbstractEntitySelector;
import org.atricore.idbus.capabilities.sso.main.select.spi.EntitySelectionContext;
import org.atricore.idbus.capabilities.sso.support.auth.AuthnCtxClass;
import org.atricore.idbus.capabilities.sso.support.binding.SSOBinding;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrustManager;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrustMemberDescriptor;
import org.atricore.idbus.kernel.main.federation.metadata.EndpointDescriptor;
import org.atricore.idbus.kernel.main.federation.metadata.EndpointDescriptorImpl;
import org.atricore.idbus.kernel.main.mediation.channel.FederationChannel;
import org.atricore.idbus.kernel.main.mediation.claim.ClaimChannel;
import org.atricore.idbus.kernel.main.mediation.claim.UserClaim;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpoint;
import org.atricore.idbus.kernel.main.mediation.provider.FederatedProvider;
import org.atricore.idbus.kernel.main.mediation.provider.IdentityProvider;
import org.atricore.idbus.kernel.main.mediation.select.SelectorChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: sgonzalez@atriocore.com
 * @date: 6/12/13
 */
public class UserSelectedIdPEntitySelector extends AbstractEntitySelector {

    private static final Log logger = LogFactory.getLog(RequestedIdPEntitySelector.class);

    @Override
    public boolean canHandle(EntitySelectionContext ctx) {
        return true;
    }

    public CircleOfTrustMemberDescriptor selectCotMember(EntitySelectionContext ctx, SelectorChannel channel) throws SSOException {

        CircleOfTrustMemberDescriptor idp = null;

        // Try with selected IDP alias first
        {
            UserClaim idpAlias = (UserClaim) ctx.getUserClaim(SELECTED_IDP_ALIAS_ATTR);
            if (idpAlias != null) {

                String idpAliasName = (String) idpAlias.getValue();

                if (logger.isDebugEnabled())
                    logger.debug("Using IdP alias " + idpAlias.getValue());

                // Support both encoded and decoded IDP alias values
                idp = ctx.getCotManager().lookupMemberByAlias(idpAliasName);
                if (idp == null) {
                    String decodedIdpAlias = new String(Base64.decodeBase64(idpAliasName.getBytes()));
                    idp = ctx.getCotManager().lookupMemberByAlias(decodedIdpAlias);
                }

            }
        }

        // Now try with selected IDP ID
        {
            UserClaim idpId = (UserClaim) ctx.getUserClaim(SELECTED_IDP_ID_ATTR);
            if (idpId != null && idp == null) {

                String idpIdName = (String) idpId.getValue();
                if (logger.isDebugEnabled())
                    logger.debug("Using IdP ID " + idpId);

                idp = ctx.getCotManager().lookupMemberById(idpIdName);
                if (idp == null) {
                    String decodedIdpId = new String(Base64.decodeBase64(idpIdName.getBytes()));
                    idp = ctx.getCotManager().lookupMemberById(decodedIdpId);
                }
            }
        }

        return idp;


    }

    @Override
    public List<EndpointDescriptor> getUserClaimsEndpoints(EntitySelectionState selectionState, SelectorChannel channel) {
        CircleOfTrustManager cotMgr = channel.getProvider().getCotManager();


        String applianceName = channel.getProvider().getUnitContainer().getName();

        SSOEntitySelectorMediator mediator = (SSOEntitySelectorMediator) channel.getIdentityMediator();

        List<EndpointDescriptor> endpoints = new ArrayList<EndpointDescriptor>();

        // We need to build a URL like: http://<host>:<port>/IDBUS-UI/<appliance>/SSO/IDPS
        String location = mediator.getUiLocation() + "/IDPS";

        EndpointDescriptor ed = new EndpointDescriptorImpl(
                "SelectUserClaimsEndpoint",
                "UserClaimsRequest",
                SSOBinding.SSO_ARTIFACT.toString(),
                location,
                null);

        endpoints.add(ed);


        if (endpoints.size() > 0)
            return endpoints;

        return null;
    }
}