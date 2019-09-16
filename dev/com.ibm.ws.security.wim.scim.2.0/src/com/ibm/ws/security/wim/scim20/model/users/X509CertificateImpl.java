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

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.users.X509Certificate;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "value", "display", "type", "primary" })
public class X509CertificateImpl implements X509Certificate {

    @JsonProperty("display")
    private String display;

    @JsonProperty("primary")
    private Boolean primary;

    @JsonProperty("type")
    private String type;

    @JsonProperty("value")
    private byte[] value;

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
        X509CertificateImpl other = (X509CertificateImpl) obj;
        if (display == null) {
            if (other.display != null) {
                return false;
            }
        } else if (!display.equals(other.display)) {
            return false;
        }
        if (primary == null) {
            if (other.primary != null) {
                return false;
            }
        } else if (!primary.equals(other.primary)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        if (!Arrays.equals(value, other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public String getDisplay() {
        return this.display;
    }

    @Override
    public Boolean getPrimary() {
        return this.primary;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public byte[] getValue() {
        return this.value.clone();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((display == null) ? 0 : display.hashCode());
        result = prime * result + ((primary == null) ? 0 : primary.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + Arrays.hashCode(value);
        return result;
    }

    @Override
    public void setDisplay(String display) {
        this.display = display;
    }

    @Override
    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void setValue(byte[] value) {
        this.value = value == null ? null : value.clone();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("X509CertificateImpl [");
        if (display != null) {
            builder.append("display=");
            builder.append(display);
            builder.append(", ");
        }
        if (primary != null) {
            builder.append("primary=");
            builder.append(primary);
            builder.append(", ");
        }
        if (type != null) {
            builder.append("type=");
            builder.append(type);
            builder.append(", ");
        }
        if (value != null) {
            builder.append("value=");
            builder.append(Arrays.toString(value));
        }
        builder.append("]");
        return builder.toString();
    }
}
