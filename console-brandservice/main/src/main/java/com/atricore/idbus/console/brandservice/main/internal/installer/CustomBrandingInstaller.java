package com.atricore.idbus.console.brandservice.main.internal.installer;

import com.atricore.idbus.console.brandservice.main.BrandingDefinition;
import com.atricore.idbus.console.brandservice.main.BrandingServiceException;
import com.atricore.idbus.console.brandservice.main.CustomBrandingDefinition;
import com.atricore.idbus.console.brandservice.main.spi.BrandManager;
import org.osgi.framework.Bundle;

import java.util.Dictionary;
import java.util.Enumeration;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class CustomBrandingInstaller extends OsgiBrandingInstaller {
    
    public boolean canHandle(BrandingDefinition def) {
        return def instanceof CustomBrandingDefinition;
    }


    public BrandingDefinition  install(BrandingDefinition def) throws BrandingServiceException {
        try { 
            CustomBrandingDefinition cd = (CustomBrandingDefinition) def;
            
            String bundleUri = cd.getBundleUri();
            Bundle bundle = getBundleContext().installBundle(bundleUri, null);
            
            // TODO : Validate the bundle ! It should be a fragment HOST for SSO UI!
            
            Dictionary d = bundle.getHeaders();
            String hostBundle = (String) d.get("Fragment-Host");
            if (hostBundle == null)
                throw new BrandingServiceException("The bundle provided for external custom branding must contain a 'Fragment-Host' header");
            
            if (!BrandManager.SSO_UI_BUNDLE.equals(hostBundle))
                throw new BrandingServiceException("The bundle provided for external custom branding must contain a 'Fragment-Host' header for " + BrandManager.SSO_UI_BUNDLE);

            cd.setBundleSymbolicName(bundle.getSymbolicName());

            return cd;

        } catch (Exception e) {
            throw new BrandingServiceException(e);
        }
        
    }


}
