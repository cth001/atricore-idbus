package com.atricore.idbus.console.lifecycle.main.domain.metadata;

import java.io.Serializable;
import java.util.Set;

public class AuthenticationService implements Serializable {

    private static final long serialVersionUID = 707964094237007668L;

    private long id;

    private String name;

    private String displayName;

    private String description;

    private Set<DelegatedAuthentication> delegatedAuthentications;

    private double x;
    private double y;
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

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

    public Set<DelegatedAuthentication> getDelegatedAuthentications() {
        return delegatedAuthentications;
    }

    public void setDelegatedAuthentications(Set<DelegatedAuthentication> delegatedAuthentications) {
        this.delegatedAuthentications = delegatedAuthentications;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticationService)) return false;

        AuthenticationService that = (AuthenticationService) o;

        if(id == 0) return false;

        if (id != that.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}