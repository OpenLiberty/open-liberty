/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.scim20.model.extensions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.extensions.WIMIdentifier;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "uniqueId", "uniqueName", "externalId", "externalName", "repositoryId" })
public class WIMIdentifierImpl implements WIMIdentifier {

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("externalName")
    private String externalName;

    @JsonProperty("repositoryId")
    private String repositoryId;

    @JsonProperty("uniqueId")
    private String uniqueId;

    @JsonProperty("uniqueName")
    private String uniqueName;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        WIMIdentifierImpl other = (WIMIdentifierImpl) obj;
        if (externalId == null) {
            if (other.externalId != null) {
                return false;
            }
        } else if (!externalId.equals(other.externalId)) {
            return false;
        }
        if (externalName == null) {
            if (other.externalName != null) {
                return false;
            }
        } else if (!externalName.equals(other.externalName)) {
            return false;
        }
        if (repositoryId == null) {
            if (other.repositoryId != null) {
                return false;
            }
        } else if (!repositoryId.equals(other.repositoryId)) {
            return false;
        }
        if (uniqueId == null) {
            if (other.uniqueId != null) {
                return false;
            }
        } else if (!uniqueId.equals(other.uniqueId)) {
            return false;
        }
        if (uniqueName == null) {
            if (other.uniqueName != null) {
                return false;
            }
        } else if (!uniqueName.equals(other.uniqueName)) {
            return false;
        }
        return true;
    }

    @Override
    public String getExternalId() {
        return externalId;
    }

    @Override
    public String getExternalName() {
        return externalName;
    }

    @Override
    public String getRepositoryId() {
        return repositoryId;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public String getUniqueName() {
        return uniqueName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((externalId == null) ? 0 : externalId.hashCode());
        result = prime * result + ((externalName == null) ? 0 : externalName.hashCode());
        result = prime * result + ((repositoryId == null) ? 0 : repositoryId.hashCode());
        result = prime * result + ((uniqueId == null) ? 0 : uniqueId.hashCode());
        result = prime * result + ((uniqueName == null) ? 0 : uniqueName.hashCode());
        return result;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setExternalName(String externalName) {
        this.externalName = externalName;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WIMIdentifierImpl [");
        if (externalId != null) {
            builder.append("externalId=");
            builder.append(externalId);
            builder.append(", ");
        }
        if (externalName != null) {
            builder.append("externalName=");
            builder.append(externalName);
            builder.append(", ");
        }
        if (repositoryId != null) {
            builder.append("repositoryId=");
            builder.append(repositoryId);
            builder.append(", ");
        }
        if (uniqueId != null) {
            builder.append("uniqueId=");
            builder.append(uniqueId);
            builder.append(", ");
        }
        if (uniqueName != null) {
            builder.append("uniqueName=");
            builder.append(uniqueName);
        }
        builder.append("]");
        return builder.toString();
    }

}
