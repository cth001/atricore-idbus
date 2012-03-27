package com.atricore.idbus.console.brandservice.main;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class CustomBrandingDefinition extends BrandingDefinition {

    private String customSsoAppClazz;

    private String customOpenIdAppClazz;
    
    private String bundleSymbolicName;

    private String bundleUri;

    public String getBundleUri() {
        return bundleUri;
    }

    public void setBundleUri(String bundleUri) {
        this.bundleUri = bundleUri;
    }

    public String getCustomSsoAppClazz() {
        return customSsoAppClazz;
    }

    public void setCustomSsoAppClazz(String customSsoAppClazz) {
        this.customSsoAppClazz = customSsoAppClazz;
    }

    public String getCustomOpenIdAppClazz() {
        return customOpenIdAppClazz;
    }

    public void setCustomOpenIdAppClazz(String customOpenIdAppClazz) {
        this.customOpenIdAppClazz = customOpenIdAppClazz;
    }

    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    public void setBundleSymbolicName(String bundleSymbolicName) {
        this.bundleSymbolicName = bundleSymbolicName;
    }
}
