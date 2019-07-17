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

package com.ibm.ws.security.wim.scim20.model.serviceprovider;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.serviceprovider.AuthenticationScheme;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "type", "name", "description", "specUri", "documentationUri" })
public class AuthenticationSchemeImpl implements AuthenticationScheme {

    @JsonProperty("description")
    private String description;

    @JsonProperty("documentationUri")
    private String documentationUri;

    @JsonProperty("name")
    private String name;

    @JsonProperty("specUri")
    private String specUri;

    @JsonProperty("type")
    private String type;

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
        AuthenticationSchemeImpl other = (AuthenticationSchemeImpl) obj;
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (documentationUri == null) {
            if (other.documentationUri != null) {
                return false;
            }
        } else if (!documentationUri.equals(other.documentationUri)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (specUri == null) {
            if (other.specUri != null) {
                return false;
            }
        } else if (!specUri.equals(other.specUri)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getDocumentationUri() {
        return this.documentationUri;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getSpecUri() {
        return this.specUri;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((documentationUri == null) ? 0 : documentationUri.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((specUri == null) ? 0 : specUri.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    /**
     * Get a description of the authentication scheme.
     *
     * @return A description of the authentication scheme.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get an HTTP-addressable URL pointing to the authentication scheme's usage
     * documentation.
     *
     * @return An HTTP-addressable URL pointing to the authentication scheme's
     *         usage documentation.
     */
    public void setDocumentationUri(String documentationUri) {
        this.documentationUri = documentationUri;
    }

    /**
     * Get the common authentication scheme name, e.g., HTTP Basic.
     *
     * @return The common authentication scheme name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get an HTTP-addressable URL pointing to the authentication scheme's
     * specification.
     *
     * @return An HTTP-addressable URL pointing to the authentication scheme's
     *         specification.
     */
    public void setSpecUri(String specUri) {
        this.specUri = specUri;
    }

    /**
     * Get the authentication scheme.
     *
     * @return The authentication scheme.
     */
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AuthenticationSchemeImpl [");
        if (description != null) {
            builder.append("description=");
            builder.append(description);
            builder.append(", ");
        }
        if (documentationUri != null) {
            builder.append("documentationUri=");
            builder.append(documentationUri);
            builder.append(", ");
        }
        if (name != null) {
            builder.append("name=");
            builder.append(name);
            builder.append(", ");
        }
        if (specUri != null) {
            builder.append("specUri=");
            builder.append(specUri);
            builder.append(", ");
        }
        if (type != null) {
            builder.append("type=");
            builder.append(type);
        }
        builder.append("]");
        return builder.toString();
    }
}
