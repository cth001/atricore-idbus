package com.atricore.idbus.console.lifecycle.main.domain.metadata;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class Activation extends Connection {
                                                                    
    private static final long serialVersionUID = 3889745220384784875L;

    private ExecutionEnvironment executionEnv;

    private ServiceResource resource;

    private ServiceProvider sp;

    public ExecutionEnvironment getExecutionEnv() {
        return executionEnv;
    }

    public void setExecutionEnv(ExecutionEnvironment executionEnv) {
        this.executionEnv = executionEnv;
    }

    public ServiceResource getResource() {
        return resource;
    }

    public void setResource(ServiceResource resource) {
        this.resource = resource;
    }

    public ServiceProvider getSp() {
        return sp;
    }

    public void setSp(ServiceProvider sp) {
        this.sp = sp;
    }
}
