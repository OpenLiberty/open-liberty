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

package com.ibm.ws.security.wim.scim20.model.schemas;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.Meta;
import com.ibm.websphere.security.wim.scim20.model.schemas.Schema;
import com.ibm.websphere.security.wim.scim20.model.schemas.SchemaAttribute;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "id", "name", "description", "attributes" })
public class SchemaImpl implements Schema {

    @JsonProperty("attributes")
    private List<SchemaAttribute> attributes;

    @JsonProperty("description")
    private String description;

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

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
        SchemaImpl other = (SchemaImpl) obj;
        if (attributes == null) {
            if (other.attributes != null) {
                return false;
            }
        } else if (!attributes.equals(other.attributes)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public List<SchemaAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getExternalId() {
        return null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Meta getMeta() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<String> getSchemas() {
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /**
     * Set the list of service provider attributes.
     *
     * @param attributes
     *            List of service provider attributes.
     */
    public void setAttributes(List<SchemaAttribute> attributes) {
        this.attributes = attributes;
    }

    /**
     * Set the schema's human-readable description.
     *
     * @param description
     *            The schema's human-readable description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Set the unique URI of the schema.
     *
     * @param id
     *            The unique URI of the schema.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Set the schema's human-readable name.
     *
     * @param name
     *            The schema's human-readable name.
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SchemaImpl [");
        if (attributes != null) {
            builder.append("attributes=");
            builder.append(attributes);
            builder.append(", ");
        }
        if (description != null) {
            builder.append("description=");
            builder.append(description);
            builder.append(", ");
        }
        if (id != null) {
            builder.append("id=");
            builder.append(id);
            builder.append(", ");
        }
        if (name != null) {
            builder.append("name=");
            builder.append(name);
        }
        builder.append("]");
        return builder.toString();
    }
}
