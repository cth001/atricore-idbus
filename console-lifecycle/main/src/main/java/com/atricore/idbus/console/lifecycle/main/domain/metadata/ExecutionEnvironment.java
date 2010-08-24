package com.atricore.idbus.console.lifecycle.main.domain.metadata;

import java.io.Serializable;

/**
 * Author: Dejan Maric
 */
public class ExecutionEnvironment implements Serializable {

    private long id;
    private String name;
    private String description;
    private String installUri;

    private static final long serialVersionUID = 175340870033867780L;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstallUri() {
        return installUri;
    }

    public void setInstallUri(String installUri) {
        this.installUri = installUri;
    }
}
