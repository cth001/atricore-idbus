package com.atricore.idbus.console.lifecycle.main.transform.transformers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.josso.main.JossoMediator;
import org.atricore.idbus.capabilities.josso.main.PartnerAppMapping;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.*;
import com.atricore.idbus.console.lifecycle.main.exception.TransformException;
import com.atricore.idbus.console.lifecycle.main.transform.IdProjectModule;
import com.atricore.idbus.console.lifecycle.main.transform.IdProjectResource;
import com.atricore.idbus.console.lifecycle.main.transform.TransformEvent;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.*;
import org.atricore.idbus.capabilities.samlr2.main.SamlR2CircleOfTrustManager;
import org.atricore.idbus.capabilities.samlr2.main.binding.SamlR2BindingFactory;
import org.atricore.idbus.capabilities.samlr2.main.binding.logging.SSOLogMessageBuilder;
import org.atricore.idbus.capabilities.samlr2.main.binding.logging.SamlR2LogMessageBuilder;
import org.atricore.idbus.capabilities.samlr2.main.sp.SamlR2SPMediator;
import org.atricore.idbus.capabilities.samlr2.main.sp.plans.SPInitiatedAuthnReqToSamlR2AuthnReqPlan;
import org.atricore.idbus.capabilities.samlr2.main.sp.plans.SamlR2SloRequestToSamlR2RespPlan;
import org.atricore.idbus.capabilities.samlr2.support.binding.SamlR2Binding;
import org.atricore.idbus.capabilities.samlr2.support.core.SamlR2KeystoreKeyResolver;
import org.atricore.idbus.capabilities.samlr2.support.core.encryption.XmlSecurityEncrypterImpl;
import org.atricore.idbus.capabilities.samlr2.support.core.signature.JSR105SamlR2SignerImpl;
import org.atricore.idbus.capabilities.samlr2.support.federation.OneToOneAccountLinkEmitter;
import org.atricore.idbus.capabilities.samlr2.support.federation.OneToOneIdentityMapper;
import org.atricore.idbus.capabilities.samlr2.support.metadata.SAMLR2MetadataConstants;
import org.atricore.idbus.kernel.main.federation.AccountLinkLifecycleImpl;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrustImpl;
import org.atricore.idbus.kernel.main.federation.metadata.ResourceCircleOfTrustMemberDescriptorImpl;
import org.atricore.idbus.kernel.main.mediation.binding.BindingChannelImpl;
import org.atricore.idbus.kernel.main.mediation.camel.component.logging.CamelLogMessageBuilder;
import org.atricore.idbus.kernel.main.mediation.camel.component.logging.HttpLogMessageBuilder;
import org.atricore.idbus.kernel.main.mediation.camel.logging.DefaultMediationLogger;
import org.atricore.idbus.kernel.main.mediation.channel.IdPChannelImpl;
import org.atricore.idbus.kernel.main.mediation.osgi.OsgiIdentityMediationUnit;
import org.atricore.idbus.kernel.main.mediation.provider.BindingProviderImpl;
import org.atricore.idbus.kernel.main.mediation.provider.ServiceProviderImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.*;
import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.newBean;
import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.setPropertyValue;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class SPTransformer extends AbstractTransformer {
    
    private static final Log logger = LogFactory.getLog(IdPTransformer.class);

    @Override
    public boolean accept(TransformEvent event) {
        return event.getData() instanceof ServiceProvider;
    }

    @Override
    public void before(TransformEvent event) throws TransformException {

        ServiceProvider provider = (ServiceProvider) event.getData();

        Date now = new Date();

        Beans spBeans = new Beans();

        Description descr = new Description();
        descr.getContent().add(provider.getName() + " : SP Configuration generated by Atricore Identity Bus Server on " + now.toGMTString());
        descr.getContent().add(provider.getDescription());

        Beans baseBeans = (Beans) event.getContext().get("beans");
        Beans beansOsgi = (Beans) event.getContext().get("beansOsgi");
        String baseSamlDestPath = (String) event.getContext().get("baseSamlDestPath");

        // TODO : Can we asure that there is only one IdP and that it's the prefered one ? This should be part of SP definition
        Beans idpBeans = (Beans) event.getContext().get("idpBeans");
        
        spBeans.setDescription(descr);

        // Publish root element so that other transformers can use it.
        event.getContext().put("spBeans", spBeans);

        if (logger.isDebugEnabled())
            logger.debug("Generating SP " + provider.getName() + " configuration model");
        
        // Define all required beans! (We can break down this in the future ...)

        // ----------------------------------------
        // Service Provider
        // ----------------------------------------

        Bean sp = newBean(spBeans, normalizeBeanName(provider.getName()),
                ServiceProviderImpl.class.getName());

        // Name
        setPropertyValue(sp, "name", sp.getName());

        // Role
        if (!provider.getRole().equals(ProviderRole.SSOServiceProvider)) {
            logger.warn("Provider "+provider.getId()+" is not defined as SP, forcing role! ");
        }
        setPropertyValue(sp, "role", SAMLR2MetadataConstants.SPSSODescriptor_QNAME.toString());

        // unitContainer
        setPropertyRef(sp, "unitContainer", event.getContext().getCurrentModule().getId() + "-container");

        // COT Manager
        Collection<Bean> cotMgrs = getBeansOfType(baseBeans, SamlR2CircleOfTrustManager.class.getName());
        if (cotMgrs.size() == 1) {
            Bean cotMgr = cotMgrs.iterator().next();
            setPropertyRef(sp, "cotManager", cotMgr.getName());
        }

        // State Manager
        setPropertyRef(sp, "stateManager", event.getContext().getCurrentModule().getId() + "-state-manager");
        
        // ----------------------------------------
        // Service Provider Mediator
        // ----------------------------------------
        Bean spMediator = newBean(spBeans, sp.getName() + "-samlr2-mediator",
                SamlR2SPMediator.class.getName());

        Collection<Bean> idpMds = getBeansOfType(idpBeans, ResourceCircleOfTrustMemberDescriptorImpl.class.getName());
        Bean idpMd = idpMds.iterator().next();

        setPropertyValue(spMediator, "preferredIdpAlias", getPropertyValue(idpMd, "alias"));
        setPropertyValue(spMediator, "preferredIdpSSOBinding", SamlR2Binding.SAMLR2_POST.getValue());
        setPropertyValue(spMediator, "preferredIdpSLOBinding", SamlR2Binding.SAMLR2_POST.getValue());
        //this is set from BPTransformer
        //setPropertyValue(spMediator, "spBindingACS", "http://localhost:8081/IDBUS/BP1/SSO/ACS/ARTIFACT");
        //setPropertyValue(spMediator, "spBindingSLO", "http://localhost:8081/IDBUS/BP1/SSO/SLO/ARTIFACT");
        // TODO RETROFIT  : String bpLocation = resolveLocationUrl(((BindingProvider)provider.getBindingChannel().getTarget()).getBindingChannel().getLocation());
        // TODO RETROFIT  : setPropertyValue(spMediator, "spBindingACS", bpLocation + "/SSO/ACS/ARTIFACT");
        // TODO RETROFIT  : setPropertyValue(spMediator, "spBindingSLO", bpLocation + "/SSO/SLO/ARTIFACT");
        
        setPropertyValue(spMediator, "logMessages", true);

        // artifactQueueManager
        setPropertyRef(spMediator, "artifactQueueManager", event.getContext().getCurrentModule().getId() + "-aqm");

        // bindingFactory
        setPropertyBean(spMediator, "bindingFactory", newAnonymousBean(SamlR2BindingFactory.class));

        // logger
        List<Bean> spLogBuilders = new ArrayList<Bean>();
        spLogBuilders.add(newAnonymousBean(SamlR2LogMessageBuilder.class));
        spLogBuilders.add(newAnonymousBean(SSOLogMessageBuilder.class));
        spLogBuilders.add(newAnonymousBean(CamelLogMessageBuilder.class));
        spLogBuilders.add(newAnonymousBean(HttpLogMessageBuilder.class));

        Bean spLogger = newAnonymousBean(DefaultMediationLogger.class.getName());
        spLogger.setName(sp.getName() + "-mediation-logger");
        setPropertyValue(spLogger, "category", "idbus.mediation.wire." + sp.getName());
        setPropertyAsBeans(spLogger, "messageBuilders", spLogBuilders);
        setPropertyBean(spMediator, "logger", spLogger);

        // errorUrl
        // TODO RETROFIT  : setPropertyValue(spMediator, "errorUrl", resolveLocationBaseUrl(provider.getBindingChannel().getLocation()) + "/idbus-ui/error.do");

        SamlR2ProviderConfig cfg = (SamlR2ProviderConfig) provider.getConfig();

        // ----------------------------------------
        // Signer
        // ----------------------------------------
        if (cfg != null && cfg.getSigner() != null) {

            String signerResourceFileName = cfg.getSigner().getStore().getName() + "." +
                    ("PKCS#12".equalsIgnoreCase(cfg.getSigner().getType()) ? "pkcs12" : "jks");

            IdProjectResource<byte[]> signerResource = new IdProjectResource<byte[]>(idGen.generateId(),
                    baseSamlDestPath + sp.getName() + "/", signerResourceFileName,
                    "binary", cfg.getSigner().getStore().getValue());
            signerResource.setClassifier("byte");

            Bean signer = newBean(spBeans, sp.getName() + "-samlr2-signer", JSR105SamlR2SignerImpl.class);
            signer.setInitMethod("init");

            Description signerDescr = new Description();
            signerDescr.getContent().add(cfg.getSigner().getDisplayName());
            signer.setDescription(signerDescr);

            Bean keyResolver = newAnonymousBean(SamlR2KeystoreKeyResolver.class);
            setPropertyValue(keyResolver, "keystoreType", cfg.getSigner().getType());
            setPropertyValue(keyResolver, "keystoreFile", "classpath:" + baseSamlDestPath + sp.getName() + "/" + signerResourceFileName);
            setPropertyValue(keyResolver, "keystorePass", cfg.getSigner().getPassword());
            setPropertyValue(keyResolver, "privateKeyAlias", cfg.getSigner().getPrivateKeyName());
            setPropertyValue(keyResolver, "privateKeyPass", cfg.getSigner().getPrivateKeyPassword());
            setPropertyValue(keyResolver, "certificateAlias", cfg.getSigner().getCertificateAlias());

            setPropertyBean(signer, "keyResolver", keyResolver);
            setPropertyBean(spMediator, "signer", signer);

            event.getContext().getCurrentModule().addResource(signerResource);

            // signer
            setPropertyRef(spMediator, "signer", signer.getName());
        }

        // ----------------------------------------
        // Encrypter
        // ----------------------------------------
        if (cfg != null && cfg.getEncrypter() != null) {

            String encrypterResourceFileName = cfg.getSigner().getStore().getName() + "." +
                    ("PKCS#12".equalsIgnoreCase(cfg.getSigner().getType()) ? "pkcs12" : "jks");

            IdProjectResource<byte[]> encrypterResource = new IdProjectResource<byte[]>(idGen.generateId(),
                    baseSamlDestPath + sp.getName() + "/", encrypterResourceFileName,
                    "binary", cfg.getSigner().getStore().getValue());
            encrypterResource.setClassifier("byte");

            Bean encrypter = newBean(spBeans, sp.getName() + "-samlr2-encrypter", XmlSecurityEncrypterImpl.class);

            setPropertyValue(encrypter, "symmetricKeyAlgorithmURI", "http://www.w3.org/2001/04/xmlenc#aes128-cbc");
            setPropertyValue(encrypter, "kekAlgorithmURI", "http://www.w3.org/2001/04/xmlenc#rsa-1_5");

            Bean keyResolver = newAnonymousBean(SamlR2KeystoreKeyResolver.class);
            setPropertyValue(keyResolver, "keystoreType", cfg.getEncrypter().getType());
            setPropertyValue(keyResolver, "keystoreFile", "classpath:" + baseSamlDestPath + sp.getName() + "/" + encrypterResourceFileName);
            setPropertyValue(keyResolver, "keystorePass", cfg.getEncrypter().getPassword());
            setPropertyValue(keyResolver, "privateKeyAlias", cfg.getEncrypter().getPrivateKeyName());
            setPropertyValue(keyResolver, "privateKeyPass", cfg.getEncrypter().getPrivateKeyPassword());
            setPropertyValue(keyResolver, "certificateAlias", cfg.getEncrypter().getCertificateAlias());

            setPropertyBean(encrypter, "keyResolver", keyResolver);
            setPropertyBean(spMediator, "encrypter", encrypter);

            event.getContext().getCurrentModule().addResource(encrypterResource);

            // encrypter
            setPropertyRef(spMediator, "encrypter", encrypter.getName());
        }

        
        Bean spMd = newBean(spBeans, sp.getName() + "-md", ResourceCircleOfTrustMemberDescriptorImpl.class);
        setPropertyValue(spMd, "id", spMd.getName());
        // TODO RETROFIT  : setPropertyValue(spMd, "alias", resolveLocationUrl(provider.getBindingChannel().getLocation()) + "/SAML2/MD");
        setPropertyValue(spMd, "resource", "classpath:" + baseSamlDestPath + sp.getName() + "/" + sp.getName() + "-samlr2-metadata.xml");
        
        // accountLinkLifecycle
        Bean accountLinkLifecycle = newBean(spBeans, sp.getName() + "-account-link-lifecycle", AccountLinkLifecycleImpl.class);

        // TODO RETROFIT  : if (provider.getDefaultChannel() != null && ((IdentityProviderChannel)provider.getDefaultChannel()).getIdentityVault() != null) {
        // TODO RETROFIT  :     setPropertyRef(accountLinkLifecycle, "identityStore", sp.getName() + "-identity-store");
        // TODO RETROFIT  : }

        // accountLinkEmitter
        Bean accountLinkEmitter = newBean(spBeans, sp.getName() + "-account-link-emitter", OneToOneAccountLinkEmitter.class);

        // identityMapper
        Bean identityMapper = newBean(spBeans, sp.getName() + "-identity-mapper", OneToOneIdentityMapper.class);

        // idp channel plans
        Bean sloToSamlPlan = newBean(spBeans, sp.getName() + "-spsso-samlr2sloreq-to-samlr2resp-plan", SamlR2SloRequestToSamlR2RespPlan.class);
        setPropertyRef(sloToSamlPlan, "bpmsManager", "bpms-manager");
        
        Bean spAuthnToSamlPlan = newBean(spBeans, sp.getName() + "-idpunsolicitedresponse-to-subject-plan", SPInitiatedAuthnReqToSamlR2AuthnReqPlan.class);
        setPropertyRef(spAuthnToSamlPlan, "bpmsManager", "bpms-manager");
        
        // ----------------------------------------
        // MBean
        // ----------------------------------------
        Bean mBean = newBean(spBeans, sp.getName() + "-mbean", "org.atricore.idbus.capabilities.samlr2.management.internal.ServiceProviderMBeanImpl");
        setPropertyRef(mBean, "serviceProvider", sp.getName());

        Bean mBeanExporter = newBean(spBeans, sp.getName() + "-mbean-exporter", "org.springframework.jmx.export.MBeanExporter");
        setPropertyRef(mBeanExporter, "server", "mBeanServer");

        // mbeans
        List<Entry> mBeans = new ArrayList<Entry>();
        
        Bean mBeanKey = newBean(spBeans, mBean.getName() + "-key", String.class);
        setConstructorArg(mBeanKey, "java.lang.String", "org.atricore.idbus." +
                event.getContext().getCurrentModule().getId() +
                ":type=ServiceProvider,name=" + sp.getName());

        Entry mBeanEntry = new Entry();
        mBeanEntry.setKeyRef(mBeanKey.getName());
        mBeanEntry.setValueRef(mBean.getName());
        mBeans.add(mBeanEntry);

        setPropertyAsMapEntries(mBeanExporter, "beans", mBeans);

        // mbean assembler
        /*Bean mBeanAssembler = newAnonymousBean("org.springframework.jmx.export.assembler.MethodNameBasedMBeanInfoAssembler");

        List<Prop> props = new ArrayList<Prop>();

        Prop prop = new Prop();
        prop.setKey("org.atricore.idbus." + event.getContext().getCurrentModule().getId() +
                ":type=ServiceProvider,name=" + sp.getName());
        prop.getContent().add("listSessions,listUserSessions,listSessionsAsTable,listUserSessionsAsTable,listStatesAsTable,listStateEntriesAsTable");
        props.add(prop);

        setPropertyValue(mBeanAssembler, "methodMappings", props);

        setPropertyBean(mBeanExporter, "assembler", mBeanAssembler);*/

        // -------------------------------------------------------
        // Define Session Manager bean
        // -------------------------------------------------------
        Bean sessionManager = newBean(spBeans, sp.getName() + "-session-manager",
                "org.atricore.idbus.kernel.main.session.service.SSOSessionManagerImpl");

        // Properties
        setPropertyValue(sessionManager, "maxInactiveInterval", "30");
        setPropertyValue(sessionManager, "maxSessionsPerUser", "-1");
        setPropertyValue(sessionManager, "invalidateExceedingSessions", "false");
        setPropertyValue(sessionManager, "sessionMonitorInterval", "10000");

        // Session ID Generator
        Bean sessionIdGenerator = newAnonymousBean("org.atricore.idbus.kernel.main.session.service.SessionIdGeneratorImpl");
        setPropertyValue(sessionIdGenerator, "algorithm", "MD5");

        // Session Store
        //Bean sessionStore = newAnonymousBean("org.atricore.idbus.idojos.memorysessionstore.MemorySessionStore");
        Bean sessionStore = newAnonymousBean("org.atricore.idbus.idojos.ehcachesessionstore.EHCacheSessionStore");
        sessionStore.setInitMethod("init");
        setPropertyRef(sessionStore, "cacheManager", event.getContext().getCurrentModule().getId() + "-cache-manager");
        setPropertyValue(sessionStore, "cacheName", event.getContext().getCurrentModule().getId() +
                "-" + sp.getName() + "-sessionsCache");
        
        // Wiring
        setPropertyBean(sessionManager, "sessionIdGenerator", sessionIdGenerator);
        setPropertyBean(sessionManager, "sessionStore", sessionStore);
    }

    @Override
    public Object after(TransformEvent event) throws TransformException {

        ServiceProvider provider = (ServiceProvider) event.getData();
        IdProjectModule module = event.getContext().getCurrentModule();
        Beans baseBeans = (Beans) event.getContext().get("beans");
        Beans spBeans = (Beans) event.getContext().get("spBeans");
        Beans bpBeans = (Beans) event.getContext().get("bpBeans");
        
        Bean spBean = getBeansOfType(spBeans, ServiceProviderImpl.class.getName()).iterator().next();

        List<Bean> bc = new ArrayList<Bean>();
        
        // Wire IdP Channels
        Collection<Bean> channels = getBeansOfType(spBeans, IdPChannelImpl.class.getName());
        for (Bean b : channels) {
            String channelProvider = getPropertyRef(b, "provider");
            if (channelProvider != null && !channelProvider.equals(spBean.getName())) {
                bc.add(b);
            } else {
                setPropertyRef(spBean, "channel", b.getName());
            }
        }

        if (bc.size() > 0)
            setPropertyAsRefs(spBean, "channels", bc);

        // Wire provider to COT
        Collection<Bean> cots = getBeansOfType(baseBeans, CircleOfTrustImpl.class.getName());
        if (cots.size() == 1) {
            Bean cot = cots.iterator().next();
            addPropertyBeansAsRefsToSet(cot, "providers", spBean);
            String dependsOn = cot.getDependsOn();
            if (dependsOn == null || dependsOn.equals("")) {
                cot.setDependsOn(spBean.getName());
            } else {
                cot.setDependsOn(dependsOn + "," + spBean.getName());
            }
        }

        // Mediation Unit
        Collection<Bean> mus = getBeansOfType(baseBeans, OsgiIdentityMediationUnit.class.getName());
        if (mus.size() == 1) {
            Bean mu = mus.iterator().next();
            Collection<Bean> bindingChannels = getBeansOfType(spBeans, BindingChannelImpl.class.getName());
            for (Bean b : bindingChannels) {
                addPropertyBeansAsRefs(mu, "channels", b);
            }
        } else {
            throw new TransformException("One and only one Identity Mediation Unit is expected, found " + mus.size());
        }

        // BP partnerAppMappings
        Bean bpBean = null;
        Collection<Bean> bps = getBeansOfType(bpBeans, BindingProviderImpl.class.getName());
        if (bps.size() == 1) {
            bpBean = bps.iterator().next();
        } else {
            throw new TransformException("One and only one Binding Provider is expected, found " + bps.size());
        }
        
        Bean bindingMediator = null;
        Collection<Bean> bindingMediators = getBeansOfType(bpBeans, JossoMediator.class.getName());
        if (bindingMediators.size() == 1) {
            bindingMediator = bindingMediators.iterator().next();
        } else {
            throw new TransformException("One and only one Josso Mediator is expected, found " + bindingMediators.size());
        }

        Bean partnerappKeyBean = newBean(bpBeans, spBean.getName() + "-key", String.class);
        setConstructorArg(partnerappKeyBean, "java.lang.String", provider.getName());

        Bean partnerappBean = newBean(bpBeans, bpBean.getName() + "-" + spBean.getName() + "-partnerapp-mapping", PartnerAppMapping.class);
        setPropertyValue(partnerappBean, "partnerAppId", provider.getName());
        Bean memberDescriptorBean = null;
        Collection<Bean> b = getBeansOfType(spBeans, ResourceCircleOfTrustMemberDescriptorImpl.class.getName());
        if (b.size() != 1) {
            throw new TransformException("Invalid resource COT member descriptor count : " + b.size());
        }
        memberDescriptorBean = b.iterator().next();
        setPropertyValue(partnerappBean, "spAlias", getPropertyValue(memberDescriptorBean, "alias"));
        setPropertyValue(partnerappBean, "partnerAppSLO", resolveLocationUrl(provider));
        setPropertyValue(partnerappBean, "partnerAppACS", resolveLocationUrl(provider) + "josso_security_check");

        Entry partnerappMapping = new Entry();
        partnerappMapping.setKeyRef(partnerappKeyBean.getName());
        partnerappMapping.setValueRef(partnerappBean.getName());
        
        addEntryToMap(bindingMediator, "partnerAppMappings", partnerappMapping);
        
        IdProjectResource<Beans> rBeans =  new IdProjectResource<Beans>(idGen.generateId(), spBean.getName(), spBean.getName(), "spring-beans", spBeans);
        rBeans.setClassifier("jaxb");
        rBeans.setNameSpace(spBean.getName());

        module.addResource(rBeans);

        return rBeans;
    }
}
