package org.atricore.idbus.kernel.main.provisioning.spi.response;

import org.atricore.idbus.kernel.main.provisioning.domain.GroupAttributeDefinition;

public class FindGroupAttributeByNameResponse extends AbstractProvisioningResponse {

    private static final long serialVersionUID = -1998476899156498718L;

    private GroupAttributeDefinition groupAttribute;

    public GroupAttributeDefinition getGroupAttribute() {
        return groupAttribute;
    }

    public void setGroupAttribute(GroupAttributeDefinition groupAttribute) {
        this.groupAttribute = groupAttribute;
    }
}
