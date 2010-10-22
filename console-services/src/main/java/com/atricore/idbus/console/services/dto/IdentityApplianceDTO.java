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

package com.atricore.idbus.console.services.dto;

import java.io.Serializable;

/**
 * Author: Dejan Maric
 */
public class IdentityApplianceDTO implements Serializable {

    private static final long serialVersionUID = 871536646583177663L;

    private long id;

    private String state;

    private String name;

    private String displayName;

    private String description;

    private String namespace;
    
    private IdentityApplianceDefinitionDTO idApplianceDefinition;

    private String idApplianceDefinitionBin;
    
    private IdentityApplianceDeploymentDTO idApplianceDeployment;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public IdentityApplianceDefinitionDTO getIdApplianceDefinition() {
        return idApplianceDefinition;
    }

    public void setIdApplianceDefinition(IdentityApplianceDefinitionDTO idApplianceDefinition) {
        this.idApplianceDefinition = idApplianceDefinition;
    }

    public IdentityApplianceDeploymentDTO getIdApplianceDeployment() {
        return idApplianceDeployment;
    }

    public void setIdApplianceDeployment(IdentityApplianceDeploymentDTO idApplianceDeployment) {
        this.idApplianceDeployment = idApplianceDeployment;
    }

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

    public String getIdApplianceDefinitionBin() {
        return idApplianceDefinitionBin;
    }

    public void setIdApplianceDefinitionBin(String idApplianceDefinitionBin) {
        this.idApplianceDefinitionBin = idApplianceDefinitionBin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentityApplianceDTO)) return false;

        IdentityApplianceDTO appliance = (IdentityApplianceDTO) o;

        if(id == 0) return false;

        if (id != appliance.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}