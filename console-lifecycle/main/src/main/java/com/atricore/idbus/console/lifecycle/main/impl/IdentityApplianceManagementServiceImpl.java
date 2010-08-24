/*
 * Atricore IDBus
 *
 *   Copyright 2009, Atricore Inc.
 *
 *   This is free software; you can redistribute it and/or modify it
 *   under the terms of the GNU Lesser General Public License as
 *   published by the Free Software Foundation; either version 2.1 of
 *   the License, or (at your option) any later version.
 *
 *   This software is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *   Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this software; if not, write to the Free
 *   Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *   02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.atricore.idbus.console.lifecycle.main.impl;

import com.atricore.idbus.console.lifecycle.main.domain.dao.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.atricore.idbus.console.lifecycle.main.domain.IdentityAppliance;
import com.atricore.idbus.console.lifecycle.main.domain.IdentityApplianceState;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.*;
import com.atricore.idbus.console.lifecycle.main.exception.IdentityServerException;
import com.atricore.idbus.console.lifecycle.main.spi.*;
import com.atricore.idbus.console.lifecycle.main.spi.request.*;
import com.atricore.idbus.console.lifecycle.main.spi.response.*;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.annotation.Transactional;

import javax.jdo.*;

import java.util.*;

public class IdentityApplianceManagementServiceImpl implements IdentityApplianceManagementService{

	private static final Log logger = LogFactory.getLog(IdentityApplianceManagementServiceImpl.class);

    private IdentityApplianceBuilder builder;

    private IdentityApplianceRegistry registry;

    private IdentityApplianceDeployer deployer;

    private IdentityApplianceDAO identityApplianceDAO;

    private IdentityApplianceDefinitionDAO identityApplianceDefinitionDAO;

    private IdentitySourceDAO identitySourceDAO;

    private UserInformationLookupDAO userInformationLookupDAO;

    private AccountLinkagePolicyDAO accountLinkagePolicyDAO;

    private AuthenticationContractDAO authenticationContractDAO;

    private AuthenticationMechanismDAO authenticationMechanismDAO;

    private AttributeProfileDAO attributeProfileDAO;

    private AuthenticationAssertionEmissionPolicyDAO authenticationAssertionEmissionPolicyDAO;

    private ResourceDAO resourceDAO;
    
    @Transactional
    public BuildIdentityApplianceResponse buildIdentityAppliance(BuildIdentityApplianceRequest request) throws IdentityServerException {
        try {
            IdentityAppliance appliance = identityApplianceDAO.findById(Long.parseLong(request.getApplianceId()));
            appliance = buildAppliance(appliance, request.isDeploy());
            appliance = identityApplianceDAO.detachCopy(appliance, FetchPlan.FETCH_SIZE_GREEDY);
            return new BuildIdentityApplianceResponse(appliance);
	    } catch (Exception e){
	        logger.error("Error building identity appliance", e);
	        throw new IdentityServerException(e);
        }
    }

    /**
     * Deploys an already existing Identity Appliance.  
     * The appliance was previously created or imported and can by found in the list of appliances.
     */
    @Transactional
    public DeployIdentityApplianceResponse deployIdentityAppliance(DeployIdentityApplianceRequest req) throws IdentityServerException {
        try {
            IdentityAppliance appliance = identityApplianceDAO.findById(Long.parseLong(req.getApplianceId()));
            appliance = deployAppliance(appliance);
            if (req.getStartAppliance()) {
                appliance = startAppliance(appliance);
            }
            appliance = identityApplianceDAO.detachCopy(appliance, FetchPlan.FETCH_SIZE_GREEDY);
            return new DeployIdentityApplianceResponse(appliance, true);
	    } catch (Exception e){
	        logger.error("Error deploying identity appliance", e);
	        throw new IdentityServerException(e);
	    }
    }

    /**
     * Undeploys an Identity Appliance.
     * The appliance was previously deployed, if the appliance is running this will first attempt to stop it.
     */
    @Transactional
    public UndeployIdentityApplianceResponse undeployIdentityAppliance(UndeployIdentityApplianceRequest req) throws IdentityServerException {
        try {
            IdentityAppliance appliance = identityApplianceDAO.findById(Long.parseLong(req.getApplianceId()));
            appliance = undeployAppliance(appliance);
            appliance = identityApplianceDAO.detachCopy(appliance, FetchPlan.FETCH_SIZE_GREEDY);
            return new UndeployIdentityApplianceResponse (appliance);
	    } catch (Exception e){
	        logger.error("Error undeploying identity appliance", e);
	        throw new IdentityServerException(e);
	    }
    }

    @Transactional
    public StartIdentityApplianceResponse startIdentityAppliance(StartIdentityApplianceRequest req) throws IdentityServerException {
        try {
            IdentityAppliance appliance = identityApplianceDAO.findById(Long.parseLong(req.getId()));
            appliance = startAppliance(appliance);
            appliance = identityApplianceDAO.detachCopy(appliance, FetchPlan.FETCH_SIZE_GREEDY);
            return new StartIdentityApplianceResponse (appliance);
	    } catch (Exception e){
	        logger.error("Error starting identity appliance", e);
	        throw new IdentityServerException(e);
	    }
    }

    @Transactional
    public StopIdentityApplianceResponse stopIdentityAppliance(StopIdentityApplianceRequest req) throws IdentityServerException {
        try {
            IdentityAppliance appliance = identityApplianceDAO.findById(Long.parseLong(req.getId()));
            appliance = stopAppliance(appliance);
            appliance = identityApplianceDAO.detachCopy(appliance, FetchPlan.FETCH_SIZE_GREEDY);
            return new StopIdentityApplianceResponse (appliance);
	    } catch (Exception e){
	        logger.error("Error stopping identity appliance", e);
	        throw new IdentityServerException(e);
	    }
    }

    public ImportIdentityApplianceResponse importIdentityAppliance(ImportIdentityApplianceRequest request) {
        throw new UnsupportedOperationException("Not Supported!");
    }

    public ExportIdentityApplianceResponse exportIdentityAppliance(ExportIdentityApplianceRequest request) {
        throw new UnsupportedOperationException("Not Supported!");
    }

    @Transactional
    public ImportApplianceDefinitionResponse importApplianceDefinition(ImportApplianceDefinitionRequest request) throws IdentityServerException {

        try {

            if (logger.isTraceEnabled())
                logger.trace("Importing appliance definition \n" + request.getDescriptor() + "\n");

            // 1. Instantiate beans
            GenericApplicationContext ctx = new GenericApplicationContext();
            ctx.setClassLoader(getClass().getClassLoader());

            XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
            xmlReader.loadBeanDefinitions(new ByteArrayResource(request.getDescriptor().getBytes()));
            ctx.refresh();

            Map<String, IdentityApplianceDefinition> definitions = ctx.getBeansOfType(IdentityApplianceDefinition.class);
            if (definitions.size() < 1 )
                throw new IdentityServerException("No Identity Appliance Definition found in the given descriptor!");
            if (definitions.size() > 1)
                throw new IdentityServerException("Only one Identity Appliance definition is supported per descriptor. (found "+definitions.size()+")");

            IdentityApplianceDefinition applianceDef = definitions.values().iterator().next();
            if (logger.isDebugEnabled())
                    logger.debug("Received Identity Appliance Definition : [" +
                            applianceDef.getId() + "] " +
                    applianceDef.getName() + ":" +
                    applianceDef.getDescription());

            applianceDef.setRevision(1);
            applianceDef.setLastModification(new Date());

            // 2. Create Identity Appliance

            if (logger.isTraceEnabled())
                logger.trace("Creating Identity Appliance");

            IdentityAppliance appliance = new IdentityAppliance ();

            appliance.setIdApplianceDefinition(applianceDef);
            appliance.setState(IdentityApplianceState.PROJECTED.toString());

            appliance = identityApplianceDAO.save(appliance);

            if (logger.isTraceEnabled())
                logger.trace("Created Identity Appliance " + appliance.getId());

            // 4. Return the appliance
            ImportApplianceDefinitionResponse response = new ImportApplianceDefinitionResponse();
            response.setAppliance(appliance);

            return response;

        } catch (Exception e) {
	        logger.error("Error importing identity appliance", e);
	        throw new IdentityServerException(e);
	    }
    }

    @Transactional
    public ManageIdentityApplianceLifeCycleResponse manageIdentityApplianceLifeCycle(ManageIdentityApplianceLifeCycleRequest req) throws IdentityServerException {
        try {
            String appId = req.getApplianceId();
            Long id = Long.parseLong(appId);

            IdentityAppliance appliance = identityApplianceDAO.findById(id);

            switch (req.getAction()) {
                case START:
                    startAppliance(appliance);
                    break;
                case STOP:
                    stopAppliance(appliance);
                    break;
                case RESTART:
                    restartAppliance(appliance);
                    break;
                case UNINSTALL:
                    undeployAppliance(appliance);
                    break;
                default:
                    throw new UnsupportedOperationException("Appliance lifecycle management action not supported: " + req.getAction());
            }

            appliance = identityApplianceDAO.detachCopy(appliance, FetchPlan.FETCH_SIZE_GREEDY);
            ManageIdentityApplianceLifeCycleResponse response = new ManageIdentityApplianceLifeCycleResponse(req.getAction(), appliance);
            response.setStatusCode(StatusCode.STS_OK);
            return response;
        } catch (Exception e){
	        logger.error("Error processing identity appliance lifecycle action", e);
	        throw new IdentityServerException(e);
	    }
    }

    @Transactional
    public AddIdentityApplianceResponse addIdentityAppliance(AddIdentityApplianceRequest req) throws IdentityServerException {
        AddIdentityApplianceResponse res = null;
        try {
            IdentityAppliance appliance = identityApplianceDAO.save(req.getIdentityAppliance());

            if (appliance.getIdApplianceDefinition() == null)
                throw new IdentityServerException("Appliances must contain an appliance definition!");

            IdentityApplianceDefinition applianceDef = appliance.getIdApplianceDefinition();
            applianceDef.setRevision(1);
            applianceDef.setLastModification(new Date());

            appliance = identityApplianceDAO.save(appliance);
            appliance = identityApplianceDAO.detachCopy(appliance, 6);
            res = new AddIdentityApplianceResponse();
            res.setAppliance(appliance);
        } catch (Exception e){
	        logger.error("Error adding identity appliance", e);
	        throw new IdentityServerException(e);
	    }
        return res;
    }

    @Transactional
    public UpdateIdentityApplianceResponse updateIdentityAppliance(UpdateIdentityApplianceRequest request) throws IdentityServerException {
        UpdateIdentityApplianceResponse res = null;
        try {
            IdentityAppliance appliance = request.getAppliance();
            IdentityApplianceDefinition applianceDef = appliance.getIdApplianceDefinition();

            applianceDef.setLastModification(new Date());
            applianceDef.setRevision(applianceDef.getRevision() + 1);

            identityApplianceDAO.save(appliance);
            appliance = identityApplianceDAO.detachCopy(appliance, 6);
            res = new UpdateIdentityApplianceResponse(appliance);
        } catch (Exception e){
	        logger.error("Error updating identity appliance", e);
	        throw new IdentityServerException(e);
	    }
        return res;
    }

    @Transactional
    public LookupIdentityApplianceByIdResponse lookupIdentityApplianceById(LookupIdentityApplianceByIdRequest request) throws IdentityServerException {
        LookupIdentityApplianceByIdResponse res = null;
        try {
            IdentityAppliance appliance = identityApplianceDAO.findById(Long.parseLong(request.getIdentityApplianceId()));
            appliance = identityApplianceDAO.detachCopy(appliance, 6);
            res = new LookupIdentityApplianceByIdResponse();
            res.setIdentityAppliance(appliance);
        } catch (Exception e){
	        logger.error("Error looking for identity appliance", e);
	        throw new IdentityServerException(e);
	    }
        return res;
    }

    @Transactional
    public RemoveIdentityApplianceResponse removeIdentityAppliance(RemoveIdentityApplianceRequest req) throws IdentityServerException{
        try {
            //First delete deployment data to prevent reference error when deleting providers
            IdentityAppliance appliance = req.getIdentityAppliance();
            appliance.setIdApplianceDeployment(null);
            appliance = identityApplianceDAO.save(appliance);
            
            //Next, delete providers (and channels with them) to prevent reference error when performing cascade-delete on vaults (while deleting appliance)
            appliance.getIdApplianceDefinition().setProviders(null);
            appliance = identityApplianceDAO.save(appliance);

            //After that delete the appliance
            this.remove(appliance);
            RemoveIdentityApplianceResponse res = new RemoveIdentityApplianceResponse();
            return res;
        } catch (Exception e){
	        logger.error("Error removing identity appliance", e);
	        throw new IdentityServerException(e);
	    }
    }

    @Transactional
    public ListIdentityAppliancesResponse listIdentityAppliances(ListIdentityAppliancesRequest req) throws IdentityServerException {
        ListIdentityAppliancesResponse res = null;
        try {
            Collection<IdentityAppliance> appliances = identityApplianceDAO.list(req.isStartedOnly());
            appliances = identityApplianceDAO.detachCopyAll(appliances, 6);

            res = new ListIdentityAppliancesResponse();
            res.setIdentityAppliances(appliances);
        } catch (Exception e){
	        logger.error("Error listing identity appliances", e);
	        throw new IdentityServerException(e);
	    }
        return res;
    }

    public ListIdentityAppliancesByStateResponse listIdentityAppliancesByState(ListIdentityAppliancesByStateRequest req) throws IdentityServerException {
        throw new UnsupportedOperationException("Not Supported!");
    }

    @Transactional
    public AddIdentityApplianceDefinitionResponse addIdentityApplianceDefinition(AddIdentityApplianceDefinitionRequest req)
            throws IdentityServerException {

        AddIdentityApplianceDefinitionResponse res = null;
        try {

            /* TODO : Improve
            if (!URLValidator.validateUrl(req.getLocation())){
                String msg = "URL Location is invalid :"+req.getLocation();
                logger.error(msg);
                throw new IdentityApplianceMetadataManagementException("URL Location invalid :" + req.getLocation());
            }
            */

            logger.debug("Persisting identity appliance definition with name: " + req.getIdentityApplianceDefinition().getName());

            //TODO : Check if the idApplianceDefinition (and entire tree) is correct
            identityApplianceDefinitionDAO.save(req.getIdentityApplianceDefinition());
	    } catch (Exception e){
	        logger.error("Error adding identity appliance definition", e);
	        throw new IdentityServerException(e);

	    }
		return res;
    }

    @Transactional
    public LookupIdentityApplianceDefinitionByIdResponse lookupIdentityApplianceDefinitionById(LookupIdentityApplianceDefinitionByIdRequest request) throws IdentityServerException {
        LookupIdentityApplianceDefinitionByIdResponse res = null;
        try {
            logger.debug("Finding identity appliance definition by ID : "+ request.getIdentityApplianceDefinitionId());
            IdentityApplianceDefinition iad = identityApplianceDefinitionDAO.findById(Long.parseLong(request.getIdentityApplianceDefinitionId()));
            iad = identityApplianceDefinitionDAO.detachCopy(iad, 3);  //fetching providers and channels as well
            res = new LookupIdentityApplianceDefinitionByIdResponse();
            res.setIdentityApplianceDefinition(iad);
	    } catch (Exception e){
	        logger.error("Error retrieving identity appliance definition with id : " + request.getIdentityApplianceDefinitionId(), e);
	        throw new IdentityServerException(e);
	    }
		return res;
    }

    @Transactional
    public ListIdentityApplianceDefinitionsResponse listIdentityApplianceDefinitions(ListIdentityApplianceDefinitionsRequest req) throws IdentityServerException {
        ListIdentityApplianceDefinitionsResponse res = new ListIdentityApplianceDefinitionsResponse();
        try {
            logger.debug("Listing all identity appliance definitions");
            Collection result = identityApplianceDefinitionDAO.findAll();
            res.getIdentityApplianceDefinitions().addAll(identityApplianceDefinitionDAO.detachCopyAll(result, 3));  //fetching providers and channels as well
        } catch (Exception e){
	        logger.error("Error retrieving identity appliance definitions!!!", e);
	        throw new IdentityServerException(e);
	    }
        return res;
    }

    /***************************************************************
     * List methods
     ***************************************************************/

    @Transactional
    public ListIdentityVaultsResponse listIdentityVaults(ListIdentityVaultsRequest req) throws IdentityServerException {
        ListIdentityVaultsResponse res = new ListIdentityVaultsResponse();
        try {
            logger.debug("Listing all identity vaults");
            Collection result = identitySourceDAO.findAll();
            res.getIdentityVaults().addAll(identitySourceDAO.detachCopyAll(result, 2));  //fetching user lookup information as well
        } catch (Exception e){
	        logger.error("Error retrieving identity vaults!!!", e);
	        throw new IdentityServerException(e);
	    }
        return res;
    }

    @Transactional
    public ListUserInformationLookupsResponse listUserInformationLookups(ListUserInformationLookupsRequest req) throws IdentityServerException {
        ListUserInformationLookupsResponse res = new ListUserInformationLookupsResponse();
        try {
            logger.debug("Listing all user information lookups");
            Collection result = userInformationLookupDAO.findAll();
            res.getUserInfoLookups().addAll(userInformationLookupDAO.detachCopyAll(result, 1));
        } catch (Exception e){
	        logger.error("Error retrieving user information lookups!!!", e);
	        throw new IdentityServerException(e);
	    }
        return res;
    }

    @Transactional
    public ListAccountLinkagePoliciesResponse listAccountLinkagePolicies(ListAccountLinkagePoliciesRequest req) throws IdentityServerException {
        ListAccountLinkagePoliciesResponse res = new ListAccountLinkagePoliciesResponse();
        try {
            logger.debug("Listing all account linkage policies");
            Collection result = accountLinkagePolicyDAO.findAll();
            res.getAccountLinkagePolicies().addAll(accountLinkagePolicyDAO.detachCopyAll(result, 1));
        } catch (Exception e){
	        logger.error("Error retrieving account linkage policies!!!", e);
	        throw new IdentityServerException(e);
	    }
        return res;
    }

    @Transactional
    public ListAuthenticationContractsResponse listAuthenticationContracts(ListAuthenticationContractsRequest req) throws IdentityServerException {
        ListAuthenticationContractsResponse res = new ListAuthenticationContractsResponse();
        try {
            logger.debug("Listing all authentication contracts");
            Collection result = authenticationContractDAO.findAll();
            res.getAuthContracts().addAll(authenticationContractDAO.detachCopyAll(result, 1));
        } catch (Exception e){
	        logger.error("Error retrieving authentication contracts!!!", e);
	        throw new IdentityServerException(e);
	    }
        return res;
    }

    @Transactional
    public ListAuthenticationMechanismsResponse listAuthenticationMechanisms(ListAuthenticationMechanismsRequest req) throws IdentityServerException {
        ListAuthenticationMechanismsResponse res = new ListAuthenticationMechanismsResponse();
        try {
            logger.debug("Listing all authentication mechanisms");
            Collection result = authenticationMechanismDAO.findAll();
            res.getAuthMechanisms().addAll(authenticationMechanismDAO.detachCopyAll(result, 1));
        } catch (Exception e){
	        logger.error("Error retrieving authentication mechanisms!!!", e);
	        throw new IdentityServerException(e);
	    }
        return res;
    }

    @Transactional
    public ListAttributeProfilesResponse listAttributeProfiles(ListAttributeProfilesRequest req) throws IdentityServerException {
        ListAttributeProfilesResponse res = new ListAttributeProfilesResponse();
        try {
            logger.debug("Listing all attribute profiles");
            Collection result = attributeProfileDAO.findAll();
            res.getAttributeProfiles().addAll(attributeProfileDAO.detachCopyAll(result, 1));
        } catch (Exception e){
	        logger.error("Error retrieving attribute profiles!!!", e);
	        throw new IdentityServerException(e);
	    }
        return res;
    }

    @Transactional
    public ListAuthAssertionEmissionPoliciesResponse listAuthAssertionEmissionPolicies(ListAuthAssertionEmissionPoliciesRequest req) throws IdentityServerException {
        ListAuthAssertionEmissionPoliciesResponse res = new ListAuthAssertionEmissionPoliciesResponse();
        try {
            logger.debug("Listing all authentication assertion emission policies");
            Collection result = authenticationAssertionEmissionPolicyDAO.findAll();
            res.getAuthEmissionPolicies().addAll(authenticationAssertionEmissionPolicyDAO.detachCopyAll(result, 1));
        } catch (Exception e){
	        logger.error("Error retrieving authentication assertion emission policies!!!", e);
	        throw new IdentityServerException(e);
	    }
        return res;
    }

    /***************************************************************
     * Lookup methods
     ***************************************************************/

    @Transactional
    public LookupIdentityVaultByIdResponse lookupIdentityVaultById(LookupIdentityVaultByIdRequest req) throws IdentityServerException {
        LookupIdentityVaultByIdResponse res = null;
        try {
            logger.debug("Finding identity vault by ID : "+ req.getIdentityVaultId());
            IdentitySource identitySource = identitySourceDAO.findById(req.getIdentityVaultId());
            res = new LookupIdentityVaultByIdResponse();
            res.setIdentityVault(identitySourceDAO.detachCopy(identitySource, 2));
	    } catch (Exception e){
	        logger.error("Error retrieving identity vault with id : " + req.getIdentityVaultId(), e);
	        throw new IdentityServerException(e);
	    }
		return res;
    }

    @Transactional
    public LookupUserInformationLookupByIdResponse lookupUserInformationLookupById(LookupUserInformationLookupByIdRequest req) throws IdentityServerException {
        LookupUserInformationLookupByIdResponse res = null;
        try {
            logger.debug("Finding user information lookup by ID : "+ req.getUserInformationLookupId());
            UserInformationLookup userInformationLookup = userInformationLookupDAO.findById(req.getUserInformationLookupId());
            res = new LookupUserInformationLookupByIdResponse();
            res.setUserInfoLookup(userInformationLookupDAO.detachCopy(userInformationLookup, 1));
	    } catch (Exception e){
	        logger.error("Error retrieving user information lookup with id : " + req.getUserInformationLookupId(), e);
	        throw new IdentityServerException(e);
	    }
		return res;
    }

    @Transactional
    public LookupAccountLinkagePolicyByIdResponse lookupAccountLinkagePolicyById(LookupAccountLinkagePolicyByIdRequest req) throws IdentityServerException {
        LookupAccountLinkagePolicyByIdResponse res = null;
        try {
            logger.debug("Finding account linkage policy by ID : "+ req.getAccountLinkagePolicyId());
            AccountLinkagePolicy policy = accountLinkagePolicyDAO.findById(req.getAccountLinkagePolicyId());
            res = new LookupAccountLinkagePolicyByIdResponse();
            res.setAccountLinkagePolicy(accountLinkagePolicyDAO.detachCopy(policy, 1));
	    } catch (Exception e){
	        logger.error("Error retrieving account linkage policy with id : " + req.getAccountLinkagePolicyId(), e);
	        throw new IdentityServerException(e);
	    }
		return res;
    }

    @Transactional
    public LookupAuthenticationContractByIdResponse lookupAuthenticationContractById(LookupAuthenticationContractByIdRequest req) throws IdentityServerException {
        LookupAuthenticationContractByIdResponse res = null;
        try {
            logger.debug("Finding authentication contract by ID : "+ req.getAuthenticationContactId());
            AuthenticationContract authenticationContract = authenticationContractDAO.findById(req.getAuthenticationContactId());
            res = new LookupAuthenticationContractByIdResponse();
            res.setAuthenticationContract(authenticationContractDAO.detachCopy(authenticationContract, 1));
	    } catch (Exception e){
	        logger.error("Error retrieving authentication contract with id : " + req.getAuthenticationContactId(), e);
	        throw new IdentityServerException(e);
	    }
		return res;
    }

    @Transactional
    public LookupAuthenticationMechanismByIdResponse lookupAuthenticationMechanismById(LookupAuthenticationMechanismByIdRequest req) throws IdentityServerException {
        LookupAuthenticationMechanismByIdResponse res = null;
        try {
            logger.debug("Finding authentication mechanism by ID : "+ req.getAuthMechanismId());
            AuthenticationMechanism authenticationMechanism = authenticationMechanismDAO.findById(req.getAuthMechanismId());
            res = new LookupAuthenticationMechanismByIdResponse();
            res.setAuthenticationMechanism(authenticationMechanismDAO.detachCopy(authenticationMechanism, 1));
	    } catch (Exception e){
	        logger.error("Error retrieving authentication mechanism with id : " + req.getAuthMechanismId(), e);
	        throw new IdentityServerException(e);
	    }
		return res;
    }

    @Transactional
    public LookupAttributeProfileByIdResponse lookupAttributeProfileById(LookupAttributeProfileByIdRequest req) throws IdentityServerException {
        LookupAttributeProfileByIdResponse res = null;
        try {
            logger.debug("Finding attribute profile by ID : "+ req.getAttributeProfileId());
            AttributeProfile attributeProfile = attributeProfileDAO.findById(req.getAttributeProfileId());
            res = new LookupAttributeProfileByIdResponse();
            res.setAttributeProfile(attributeProfileDAO.detachCopy(attributeProfile, 1));
	    } catch (Exception e){
	        logger.error("Error retrieving attribute profile with id : " + req.getAttributeProfileId(), e);
	        throw new IdentityServerException(e);
	    }
		return res;
    }

    @Transactional
    public LookupAuthAssertionEmissionPolicyByIdResponse lookupAuthAssertionEmissionPolicyById(LookupAuthAssertionEmissionPolicyByIdRequest req) throws IdentityServerException {
        LookupAuthAssertionEmissionPolicyByIdResponse res = null;
        try {
            logger.debug("Finding authentication assertion emission policy by ID : "+ req.getAuthAssertionEmissionPolicyId());
            AuthenticationAssertionEmissionPolicy policy = authenticationAssertionEmissionPolicyDAO.findById(req.getAuthAssertionEmissionPolicyId());
            res = new LookupAuthAssertionEmissionPolicyByIdResponse();
            res.setPolicy(authenticationAssertionEmissionPolicyDAO.detachCopy(policy, 1));
	    } catch (Exception e){
	        logger.error("Error retrieving authentication assertion emission policy with id : " + req.getAuthAssertionEmissionPolicyId(), e);
	        throw new IdentityServerException(e);
	    }
		return res;
    }

    @Transactional
    public AddResourceResponse addResource(AddResourceRequest req) throws IdentityServerException {
        AddResourceResponse res = null;
        try {
            logger.debug("Persisting resource with name: " + req.getResource().getName());
            Resource resource = resourceDAO.save(req.getResource());
            res = new AddResourceResponse();
            res.setResource(resourceDAO.detachCopy(resource, FetchPlan.FETCH_SIZE_GREEDY));
	    } catch (Exception e){
	        logger.error("Error adding resource", e);
	        throw new IdentityServerException(e);
	    }
		return res;
    }

    @Transactional
    public LookupResourceByIdResponse lookupResourceById(LookupResourceByIdRequest req) throws IdentityServerException {
        LookupResourceByIdResponse res = null;
        try {
            Long id = Long.parseLong(req.getResourceId());
            Resource resource = resourceDAO.findById(id);
            resource = resourceDAO.detachCopy(resource, FetchPlan.FETCH_SIZE_GREEDY);
            res = new LookupResourceByIdResponse();
            res.setResource(resource);
        } catch (Exception e){
	        logger.error("Error looking for resource", e);
	        throw new IdentityServerException(e);
	    }
        return res;
    }

// -------------------------------------------------< Properties >

    public IdentityApplianceBuilder getBuilder() {
        return builder;
    }

    public void setBuilder(IdentityApplianceBuilder builder) {
        this.builder = builder;
    }

    public IdentityApplianceRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(IdentityApplianceRegistry registry) {
        this.registry = registry;
    }

    public IdentityApplianceDeployer getDeployer() {
        return deployer;
    }

    public void setDeployer(IdentityApplianceDeployer deployer) {
        this.deployer = deployer;
    }

    public void setIdentityApplianceDAO(IdentityApplianceDAO identityApplianceDAO) {
        this.identityApplianceDAO = identityApplianceDAO;
    }

    public void setIdentityApplianceDefinitionDAO(IdentityApplianceDefinitionDAO identityApplianceDefinitionDAO) {
        this.identityApplianceDefinitionDAO = identityApplianceDefinitionDAO;
    }

    public void setIdentityVaultDAO(IdentitySourceDAO identitySourceDAO) {
        this.identitySourceDAO = identitySourceDAO;
    }

    public void setUserInformationLookupDAO(UserInformationLookupDAO userInformationLookupDAO) {
        this.userInformationLookupDAO = userInformationLookupDAO;
    }

    public void setAccountLinkagePolicyDAO(AccountLinkagePolicyDAO accountLinkagePolicyDAO) {
        this.accountLinkagePolicyDAO = accountLinkagePolicyDAO;
    }

    public void setAuthenticationContractDAO(AuthenticationContractDAO authenticationContractDAO) {
        this.authenticationContractDAO = authenticationContractDAO;
    }

    public void setAuthenticationMechanismDAO(AuthenticationMechanismDAO authenticationMechanismDAO) {
        this.authenticationMechanismDAO = authenticationMechanismDAO;
    }

    public void setAttributeProfileDAO(AttributeProfileDAO attributeProfileDAO) {
        this.attributeProfileDAO = attributeProfileDAO;
    }

    public void setAuthenticationAssertionEmissionPolicyDAO(AuthenticationAssertionEmissionPolicyDAO authenticationAssertionEmissionPolicyDAO) {
        this.authenticationAssertionEmissionPolicyDAO = authenticationAssertionEmissionPolicyDAO;
    }

    public void setResourceDAO(ResourceDAO resourceDAO) {
        this.resourceDAO = resourceDAO;
    }
    // -------------------------------------------------< Protected Utils , they need transactional context !>
    
    protected IdentityAppliance startAppliance(IdentityAppliance appliance) throws IdentityServerException {
        if (logger.isDebugEnabled())
            logger.debug("Starting Identity Appliance " + appliance.getId());

        if (appliance.getState().equals(IdentityApplianceState.PROJECTED.toString()))
            appliance = buildAppliance(appliance, true);

        appliance = deployer.start(appliance);
        appliance = identityApplianceDAO.save(appliance);

        return appliance;
    }

    protected IdentityAppliance stopAppliance(IdentityAppliance appliance) throws IdentityServerException {
        if (logger.isDebugEnabled())
            logger.debug("Stopping Identity Appliance " + appliance.getId());

        appliance = deployer.stop(appliance);
        appliance = identityApplianceDAO.save(appliance);

        return appliance;
    }

    protected IdentityAppliance restartAppliance(IdentityAppliance appliance) throws IdentityServerException {

        if (logger.isDebugEnabled())
            logger.debug("Restarting Identity Appliance " + appliance.getId());

        appliance = stopAppliance(appliance);
        appliance = startAppliance(appliance);

        return appliance;
    }

    protected IdentityAppliance undeployAppliance(IdentityAppliance appliance) throws IdentityServerException {
        if (logger.isDebugEnabled())
            logger.debug("Undeploying Identity Appliance " + appliance.getId());


        if (appliance.getState().equals(IdentityApplianceState.STARTED.toString()))
            appliance = stopAppliance(appliance);

        // Install it
        appliance = deployer.undeploy(appliance);

        // Store it
        appliance = identityApplianceDAO.save(appliance);
        return appliance;

    }

    protected void remove(IdentityAppliance appliance) throws IdentityServerException {
        try {
        	logger.debug("Deleting identity appliance with id: " + appliance.getId());
            identityApplianceDAO.delete(appliance.getId());
        } catch (Exception e){
            logger.error("Error removing a Identity Appliance",e);
            throw new IdentityServerException(e);
        }
    }

    protected IdentityAppliance deployAppliance(IdentityAppliance appliance) throws IdentityServerException {

        if (logger.isDebugEnabled())
            logger.debug("Deploying Identity Appliance " + appliance.getId());

        if (appliance.getState().equals(IdentityApplianceState.STARTED.toString()))
            appliance = stopAppliance(appliance);

        if (appliance.getState().equals(IdentityApplianceState.INSTALLED.toString()))
            appliance = undeployAppliance(appliance);

        if (appliance.getIdApplianceDeployment() == null)
            appliance = buildAppliance(appliance, false);

        // Install it
        appliance = deployer.deploy(appliance);

        // Store it
        appliance = identityApplianceDAO.save(appliance);
        return appliance;
    }


    protected IdentityAppliance buildAppliance(IdentityAppliance appliance, boolean deploy) throws IdentityServerException {

        if (logger.isDebugEnabled())
            logger.debug("Building Identity Appliance [deploy:"+deploy+"]" + appliance.getId());

        // quick fix (sort providers: identity providers -> binding provider -> service providers -> binding provider -> service providers, ...)
        Set<Provider> providers = appliance.getIdApplianceDefinition().getProviders();
        List<Provider> sortedProviders = new ArrayList<Provider>();
        for (Provider provider : providers) {
            if (provider instanceof ServiceProvider) {
                sortedProviders.add(provider);
            }
        }
        Collections.sort(sortedProviders, new ServiceProviderComparator());
        for (Provider provider : providers) {
            if (provider instanceof IdentityProvider) {
                sortedProviders.add(0, provider);
            } else if (provider instanceof BindingProvider) {
                int i = 0;
                boolean added = false;
                for (Provider sortedProvider : sortedProviders) {
                    // TODO RETROFIT  :
                    /*
                    if (sortedProvider instanceof ServiceProvider &&
                            ((ServiceProvider) sortedProvider).getBindingChannel().getTarget().equals(provider)) {
                        sortedProviders.add(i, provider);
                        added = true;
                        break;
                    }
                    */
                    i++;
                }
                if (!added) {
                    sortedProviders.add(provider);
                }
            }
        }

        // TODO RETROFIT  :appliance.getIdApplianceDefinition().setProviders(sortedProviders);

        // Build the appliance
        appliance = builder.build(appliance);

        // Install it
        if (deploy)
            appliance = deployAppliance(appliance);

        // Store it
        appliance = identityApplianceDAO.save(appliance);
        return appliance;
    }

    public class ServiceProviderComparator implements Comparator<Provider> {
        public int compare(Provider sp1, Provider sp2) {

            // TODO RETROFIT  : if (((ServiceProvider)sp1).getBindingChannel().getTarget().equals(((ServiceProvider)sp2).getBindingChannel().getTarget())) return 0;
            // TODO RETROFIT  : else return 1;
            return 1;
        }
    }
}
