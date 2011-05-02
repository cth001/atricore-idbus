package com.atricore.idbus.console.services.dto;

public class DirectoryAuthenticationServiceDTO extends AuthenticationServiceDTO {

    private static final long serialVersionUID = -2637953445913433166L;

    private String initialContextFactory;
    private String providerUrl;
    private boolean performDnSearch;
    private String securityAuthentication;

    /*
    private String securityPrincipal;
    private String securityCredential;
    */

    public String getInitialContextFactory() {
        return initialContextFactory;
    }

    public void setInitialContextFactory(String initialContextFactory) {
        this.initialContextFactory = initialContextFactory;
    }

    public String getProviderUrl() {
        return providerUrl;
    }

    public void setProviderUrl(String providerUrl) {
        this.providerUrl = providerUrl;
    }

    public boolean isPerformDnSearch() {
        return performDnSearch;
    }

    public void setPerformDnSearch(boolean performDnSearch) {
        this.performDnSearch = performDnSearch;
    }

    public String getSecurityAuthentication() {
        return securityAuthentication;
    }

    public void setSecurityAuthentication(String securityAuthentication) {
        this.securityAuthentication = securityAuthentication;
    }

    /*
    public String getSecurityCredential() {
        return securityCredential;
    }

    public void setSecurityCredential(String securityCredential) {
        this.securityCredential = securityCredential;
    }

    public String getSecurityPrincipal() {
        return securityPrincipal;
    }

    public void setSecurityPrincipal(String securityPrincipal) {
        this.securityPrincipal = securityPrincipal;
    }
    */

}
