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

package com.ibm.ws.security.wim.scim20.model.users;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.users.Name;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "formatted", "familyName", "givenName", "middleName", "honorificPrefix",
                             "honorificSuffix" })
public class NameImpl implements Name {

    @JsonProperty("familyName")
    private String familyName;

    @JsonProperty("formatted")
    private String formatted;

    @JsonProperty("givenName")
    private String givenName;

    @JsonProperty("honorificPrefix")
    private String honorificPrefix;

    @JsonProperty("honorificSuffix")
    private String honorificSuffix;

    @JsonProperty("middleName")
    private String middleName;

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
        NameImpl other = (NameImpl) obj;
        if (familyName == null) {
            if (other.familyName != null) {
                return false;
            }
        } else if (!familyName.equals(other.familyName)) {
            return false;
        }
        if (formatted == null) {
            if (other.formatted != null) {
                return false;
            }
        } else if (!formatted.equals(other.formatted)) {
            return false;
        }
        if (givenName == null) {
            if (other.givenName != null) {
                return false;
            }
        } else if (!givenName.equals(other.givenName)) {
            return false;
        }
        if (honorificPrefix == null) {
            if (other.honorificPrefix != null) {
                return false;
            }
        } else if (!honorificPrefix.equals(other.honorificPrefix)) {
            return false;
        }
        if (honorificSuffix == null) {
            if (other.honorificSuffix != null) {
                return false;
            }
        } else if (!honorificSuffix.equals(other.honorificSuffix)) {
            return false;
        }
        if (middleName == null) {
            if (other.middleName != null) {
                return false;
            }
        } else if (!middleName.equals(other.middleName)) {
            return false;
        }
        return true;
    }

    @Override
    public String getFamilyName() {
        return this.familyName;
    }

    @Override
    public String getFormatted() {
        return this.formatted;
    }

    @Override
    public String getGivenName() {
        return this.givenName;
    }

    @Override
    public String getHonorificPrefix() {
        return this.honorificPrefix;
    }

    @Override
    public String getHonorificSuffix() {
        return this.honorificSuffix;
    }

    @Override
    public String getMiddleName() {
        return this.middleName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((familyName == null) ? 0 : familyName.hashCode());
        result = prime * result + ((formatted == null) ? 0 : formatted.hashCode());
        result = prime * result + ((givenName == null) ? 0 : givenName.hashCode());
        result = prime * result + ((honorificPrefix == null) ? 0 : honorificPrefix.hashCode());
        result = prime * result + ((honorificSuffix == null) ? 0 : honorificSuffix.hashCode());
        result = prime * result + ((middleName == null) ? 0 : middleName.hashCode());
        return result;
    }

    @Override
    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    @Override
    public void setFormatted(String formatted) {
        this.formatted = formatted;
    }

    @Override
    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    @Override
    public void setHonorificPrefix(String honorificPrefix) {
        this.honorificPrefix = honorificPrefix;
    }

    @Override
    public void setHonorificSuffix(String honorificSuffix) {
        this.honorificSuffix = honorificSuffix;
    }

    @Override
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NameImpl [");
        if (familyName != null) {
            builder.append("familyName=");
            builder.append(familyName);
            builder.append(", ");
        }
        if (formatted != null) {
            builder.append("formatted=");
            builder.append(formatted);
            builder.append(", ");
        }
        if (givenName != null) {
            builder.append("givenName=");
            builder.append(givenName);
            builder.append(", ");
        }
        if (honorificPrefix != null) {
            builder.append("honorificPrefix=");
            builder.append(honorificPrefix);
            builder.append(", ");
        }
        if (honorificSuffix != null) {
            builder.append("honorificSuffix=");
            builder.append(honorificSuffix);
            builder.append(", ");
        }
        if (middleName != null) {
            builder.append("middleName=");
            builder.append(middleName);
        }
        builder.append("]");
        return builder.toString();
    }
}
