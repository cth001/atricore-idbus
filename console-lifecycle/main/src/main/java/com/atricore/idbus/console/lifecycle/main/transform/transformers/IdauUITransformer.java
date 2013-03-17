package com.atricore.idbus.console.lifecycle.main.transform.transformers;

import com.atricore.idbus.console.brandservice.main.domain.BrandingDefinition;
import com.atricore.idbus.console.brandservice.main.BrandingServiceException;
import com.atricore.idbus.console.brandservice.main.domain.CustomBrandingDefinition;
import com.atricore.idbus.console.brandservice.main.spi.BrandManager;
import com.atricore.idbus.console.lifecycle.main.domain.IdentityAppliance;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.*;
import com.atricore.idbus.console.lifecycle.main.transform.*;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.*;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.osgi.Service;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.pax.wicket.Application;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.kernel.main.mediation.provider.ServiceProvider;

import java.util.Date;
import java.util.HashMap;

import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.*;
import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.setPropertyValue;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */

public class IdauUITransformer extends AbstractTransformer {

    private BrandManager brandManager;

    private static Log logger = LogFactory.getLog(IdauBaseComponentsTransformer.class);

    @Override
    public boolean accept(TransformEvent event) {
        return event.getData() instanceof IdentityApplianceDefinition;
    }

    @Override
    public void before(TransformEvent event) {

        IdentityApplianceDefinition ida = (IdentityApplianceDefinition) event.getData();
        IdentityAppliance appliance = event.getContext().getProject().getIdAppliance();
        IdApplianceTransformationContext context = event.getContext();
        IdProjectModule module = context.getCurrentModule();

        Date now = new Date();

        // ----------------------------------------
        // UI Beans
        // ----------------------------------------

        Beans idauBeansUi = newBeans(ida.getName() + " UI : IdAU Configuration generated by Atricore Console on " + now.toGMTString());
        String uiBasePath = "IDBUS-UI";
        Location uiLocation = ida.getUiLocation();
        if (uiLocation != null) {
            uiBasePath = resolveLocationPath(uiLocation);
        }

        // ----------------------------------------
        // SSO Capability application
        //
        // General purpose appliance application class
        // ----------------------------------------
        {

            String path = module.getPath();
            String pkg = module.getPackage();
            String ssoAppClazz = "SSOUIApplication";
            String parentClazz = "org.atricore.idbus.capabilities.sso.ui.internal.SSOUIApplication";


            {
                IdProjectSource s = new IdProjectSource(ssoAppClazz, path, ssoAppClazz, "java", "extends");
                s.setExtension("java");
                s.setClassifier("velocity");

                java.util.Map<String, Object> params = new HashMap<String, Object>();
                params.put("package", pkg);
                params.put("clazz", ssoAppClazz);
                params.put("parentClazz", parentClazz);
                s.setParams(params);
                module.addSource(s);
            }

            // Each IDP must have its own application. but we also need some generic pages for non-idp error display
            Application ssoUiApp = new Application();
            ssoUiApp.setId(normalizeBeanName(ida.getName() + "-sso-ui"));
            ssoUiApp.setApplicationName(ida.getName().toLowerCase() + "-sso-ui");
            //ssoUiApp.setClazz(pkg + "." + ssoAppClazz); // DO NOT USE THE GENERATED CLASS WITH WICKET 6.X
            ssoUiApp.setClazz(parentClazz);
            ssoUiApp.setMountPoint(uiBasePath + "/" + ida.getName().toUpperCase() + "/SSO");
            ssoUiApp.setInjectionSource("spring");

            idauBeansUi.getImportsAndAliasAndBeen().add(ssoUiApp);


            // App Configuration
            Bean appCfgBean = newBean(idauBeansUi, ssoUiApp.getId() + "-cfg", "org.atricore.idbus.capabilities.sso.ui.WebAppConfig");
            setPropertyValue(appCfgBean, "appName", ssoUiApp.getId());
            setPropertyValue(appCfgBean, "mountPoint", ssoUiApp.getMountPoint());
            if (ida.getUserDashboardBranding() != null) {
                setPropertyValue(appCfgBean, "brandingId", ida.getUserDashboardBranding().getId());
            }

            // Export App Configuration
            Service appCfgBeanOsgi = new Service();
            appCfgBeanOsgi.setId(appCfgBean.getName() + "-osgi");
            appCfgBeanOsgi.setRef(appCfgBean.getName());
            appCfgBeanOsgi.setInterface("org.atricore.idbus.capabilities.sso.ui.WebAppConfig");

            idauBeansUi.getImportsAndAliasAndBeen().add(appCfgBeanOsgi);


        }

        // ----------------------------------------
        // OpenID Capability application
        // ----------------------------------------
        // TODO Create per-idp application (Move this to OPENID IDP UI TRANSFOMER !)
        {

            String path = module.getPath();
            String pkg = module.getPackage();
            String idpAppClazz = "OpenIDIdPApplication";
            String parentClazz = "org.atricore.idbus.capabilities.openid.ui.internal.OpenIDIdPApplication";

            IdProjectSource s = new IdProjectSource(idpAppClazz, path, idpAppClazz, "java", "extends");
            s.setExtension("java");
            s.setClassifier("velocity");

            java.util.Map<String, Object> params = new HashMap<String, Object>();
            params.put("package", pkg);
            params.put("clazz", idpAppClazz);
            params.put("parentClazz", parentClazz);
            s.setParams(params);
            module.addSource(s);

            Application openIdUiApp = new Application();
            openIdUiApp.setId(ida.getName().toLowerCase() + "-openid-ui");
            openIdUiApp.setApplicationName(ida.getName().toLowerCase() + "-openid-ui");
            //openIdUiApp.setClazz(pkg + "." + idpAppClazz);  // DO NOT USE THE GENERATED CLASS WITH WICKET 6.X
            openIdUiApp.setClazz(parentClazz);
            openIdUiApp.setMountPoint(uiBasePath + "/" + ida.getName().toUpperCase() + "/OPENID");
            openIdUiApp.setInjectionSource("spring");

            idauBeansUi.getImportsAndAliasAndBeen().add(openIdUiApp);

            // App Configuration
            Bean appCfgBean = newBean(idauBeansUi, openIdUiApp.getId() + "-cfg", "org.atricore.idbus.capabilities.sso.ui.WebAppConfig");
            setPropertyValue(appCfgBean, "appName", openIdUiApp.getId());
            setPropertyValue(appCfgBean, "mountPoint", openIdUiApp.getMountPoint());
            if (ida.getUserDashboardBranding() != null) {
                setPropertyValue(appCfgBean, "brandingId", ida.getUserDashboardBranding().getId());

            }

            // Export App Configuration
            Service appCfgBeanOsgi = new Service();
            appCfgBeanOsgi.setId(appCfgBean.getName() + "-osgi");
            appCfgBeanOsgi.setRef(appCfgBean.getName());
            appCfgBeanOsgi.setInterface("org.atricore.idbus.capabilities.sso.ui.WebAppConfig");

            idauBeansUi.getImportsAndAliasAndBeen().add(appCfgBeanOsgi);



        }

        // ----------------------------------------
        // Add all the beans to the list
        // ----------------------------------------
        IdProjectResource<Beans> rBeansUi =  new IdProjectResource<Beans>(idGen.generateId(), "beans-ui", "spring-beans", idauBeansUi);
        rBeansUi.setClassifier("jaxb");
        module.addResource(rBeansUi);

    }

    public BrandManager getBrandManager() {
        return brandManager;
    }

    public void setBrandManager(BrandManager brandManager) {
        this.brandManager = brandManager;
    }
}