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
import com.ibm.websphere.security.wim.scim20.model.users.UserGroup;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "value", "$ref", "display", "type" })
public class UserGroupImpl implements UserGroup {

    /**
     * Group membership type "direct".
     */
    public static final String TYPE_DIRECT = "direct";

    /**
     * Group membership type "indirect".
     */
    public static final String TYPE_INDIRECT = "indirect";

    @JsonProperty("display")
    private String display;

    @JsonProperty("$ref")
    private String ref;

    @JsonProperty("type")
    private String type;

    @JsonProperty("value")
    private String value;

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
        UserGroupImpl other = (UserGroupImpl) obj;
        if (display == null) {
            if (other.display != null) {
                return false;
            }
        } else if (!display.equals(other.display)) {
            return false;
        }
        if (ref == null) {
            if (other.ref != null) {
                return false;
            }
        } else if (!ref.equals(other.ref)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public String getDisplay() {
        return this.display;
    }

    @Override
    public String getRef() {
        return this.ref;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((display == null) ? 0 : display.hashCode());
        result = prime * result + ((ref == null) ? 0 : ref.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    /**
     * Set a human-readable name, primarily used for display purposes.
     *
     * @param display
     *            A human-readable name, primarily used for display purposes
     */
    public void setDisplay(String display) {
        this.display = display;
    }

    /**
     * Set the URI of the corresponding 'Group' resource to which the user
     * belongs.
     *
     * @param ref
     *            The URI of the corresponding 'Group' resource to which the
     *            user belongs.
     */
    public void setRef(String ref) {
        this.ref = ref;
    }

    /**
     * Set a label indicating the attribute's function, e.g., 'direct' or
     * 'indirect'.
     *
     * @param type
     *            A label indicating the attribute's function.
     *
     * @see #TYPE_DIRECT
     * @see #TYPE_INDIRECT
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Set the identifier of the user's group.
     *
     * @param value
     *            The identifier of the user's group.
     */
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserGroupImpl [");
        if (display != null) {
            builder.append("display=");
            builder.append(display);
            builder.append(", ");
        }
        if (ref != null) {
            builder.append("ref=");
            builder.append(ref);
            builder.append(", ");
        }
        if (type != null) {
            builder.append("type=");
            builder.append(type);
            builder.append(", ");
        }
        if (value != null) {
            builder.append("value=");
            builder.append(value);
        }
        builder.append("]");
        return builder.toString();
    }
}
