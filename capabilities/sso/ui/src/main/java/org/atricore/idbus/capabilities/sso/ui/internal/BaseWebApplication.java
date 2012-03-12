package org.atricore.idbus.capabilities.sso.ui.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.SharedResources;
import org.apache.wicket.markup.parser.filter.RelativePathPrefixHandler;
import org.apache.wicket.markup.resolver.IComponentResolver;
import org.apache.wicket.protocol.http.WebApplication;
import org.atricore.idbus.capabilities.sso.ui.BasePage;
import org.atricore.idbus.capabilities.sso.ui.BrandingResource;
import org.atricore.idbus.capabilities.sso.ui.WebAppConfig;
import org.atricore.idbus.capabilities.sso.ui.WebBranding;
import org.atricore.idbus.capabilities.sso.ui.resources.AppResourceLocator;
import org.atricore.idbus.capabilities.sso.ui.spi.ApplicationRegistry;
import org.atricore.idbus.capabilities.sso.ui.spi.WebBrandingService;
import org.ops4j.pax.wicket.api.PaxWicketBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO : Implement a resource locator that can search for resources (pages, images, properties) in other bundles
 *
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public abstract class BaseWebApplication extends WebApplication {

    private static final Log logger = LogFactory.getLog(BaseWebApplication.class);

    private boolean ready;

    // Dependency injection does not work for application objects (pax-wicket)!

    protected ApplicationRegistry appConfigRegistry;

    protected WebBrandingService brandingService;

    protected WebBranding branding;

    protected List<AppResource> appResources = new ArrayList<AppResource>();

    public BaseWebApplication() {
        super();
    }

    public ApplicationRegistry getAppConfigRegistry() {
        return appConfigRegistry;
    }

    public void setAppConfigRegistry(ApplicationRegistry appConfigRegistry) {
        this.appConfigRegistry = appConfigRegistry;
    }

    public WebBrandingService getBrandingService() {
        return brandingService;
    }

    public void setBrandingService(WebBrandingService brandingService) {
        this.brandingService = brandingService;
    }

    public boolean isReady() {
        return ready;
    }
    
    

    @Override
    protected void init() {
        super.init();
        preInit();
        mountPages();
    }

    protected void mountPages() {

    }

    protected void preInit() {

    }

    /**
     * Injected services are available here
     */
    protected void postInit() {

        List<IComponentResolver> currentList = getPageSettings().getComponentResolvers();
        List<IComponentResolver> newComponentsList = new ArrayList<IComponentResolver>(currentList.size());

        for (IComponentResolver iComponentResolver : currentList) {
            if (iComponentResolver instanceof RelativePathPrefixHandler) {
                newComponentsList.add(new IdBusRelativePathPrefixHandler(getAppConfig().getMountPoint()));
            } else {
                newComponentsList.add(iComponentResolver);
            }
        }

        getPageSettings().getComponentResolvers().clear();
        getPageSettings().getComponentResolvers().addAll(newComponentsList);

        getMarkupSettings().setMarkupParserFactory(new IdBusMarkupParserFactory(getAppConfig()));
        
        // TODO : Instead of taking resources list from branding, also support scanning specific packages of specific bundles !!!! 
        if (branding != null) {

            // TODO : Implement this!
            branding.getResourceBundles();

            // Mount branding shared resources at base mount point
            for (BrandingResource resource : branding.getResources()) {
                // All shared resource MUST be scoped to AppResourceLocator
                if (resource.isShared()) {
                    ResourceReference ref = new ResourceReference(AppResourceLocator.class, resource.getPath());
                    this.appResources.add(new AppResource(resource, ref));
                    mountSharedResource("/" + resource.getPath(), ref.getSharedResourceKey());
                    if (logger.isTraceEnabled())
                        logger.trace("Mounting shared resource ["+resource.getId()+"] at /" + resource.getPath());
                }
            }
        }

    }

    public WebBranding getBranding() {
        return branding;
    }

    public WebAppConfig getAppConfig() {
        WebAppConfig cfg = appConfigRegistry.lookupConfig(getApplicationKey());
        if (cfg == null)
            logger.error("No configuration found for Wicket application " + getApplicationKey());

        return cfg;
    }

    public List<AppResource> getAppResources() {
        return appResources;
    }

    public final void config(ApplicationRegistry appConfigRegistry, WebBrandingService brandingService) {
        this.appConfigRegistry = appConfigRegistry;
        this.brandingService = brandingService;
        String brandingId = getAppConfig().getBrandingId();
        branding = brandingService.lookup(brandingId);
        postInit();
        this.ready = true;
    }

    public class AppResource implements Serializable {

        private BrandingResource resource;

        private ResourceReference ref;

        public AppResource(BrandingResource resource, ResourceReference ref) {
            this.resource = resource;
            this.ref = ref;
        }

        public BrandingResource getResource() {
            return resource;
        }

        public ResourceReference getRef() {
            return ref;
        }
    }



}