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

package com.ibm.ws.security.wim.scim20.model.resourcetype;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.Meta;
import com.ibm.websphere.security.wim.scim20.model.resourcetype.ResourceType;
import com.ibm.websphere.security.wim.scim20.model.resourcetype.SchemaExtension;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "schemas", "id", "name", "description", "endpoint", "schema", "schemaExtensions" })
public class ResourceTypeImpl implements ResourceType {

    public static final String SCHEMA_URN = "urn:ietf:params:scim:schemas:core:2.0:ResourceType";

    @JsonProperty("description")
    private String description;

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("schema")
    private String schema;

    @JsonProperty("schemaExtensions")
    private List<SchemaExtension> schemaExtensions;

    @JsonProperty("schemas")
    private final List<String> schemas;

    public ResourceTypeImpl() {
        schemas = Arrays.asList(new String[] { SCHEMA_URN });
    }

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
        ResourceTypeImpl other = (ResourceTypeImpl) obj;
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (endpoint == null) {
            if (other.endpoint != null) {
                return false;
            }
        } else if (!endpoint.equals(other.endpoint)) {
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
        if (schema == null) {
            if (other.schema != null) {
                return false;
            }
        } else if (!schema.equals(other.schema)) {
            return false;
        }
        if (schemaExtensions == null) {
            if (other.schemaExtensions != null) {
                return false;
            }
        } else if (!schemaExtensions.equals(other.schemaExtensions)) {
            return false;
        }
        if (schemas == null) {
            if (other.schemas != null) {
                return false;
            }
        } else if (!schemas.equals(other.schemas)) {
            return false;
        }
        return true;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getEndpoint() {
        return endpoint;
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
    public String getSchema() {
        return schema;
    }

    @Override
    public List<SchemaExtension> getSchemaExtensions() {
        return schemaExtensions;
    }

    @Override
    public List<String> getSchemas() {
        return Collections.unmodifiableList(schemas);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((schema == null) ? 0 : schema.hashCode());
        result = prime * result + ((schemaExtensions == null) ? 0 : schemaExtensions.hashCode());
        result = prime * result + ((schemas == null) ? 0 : schemas.hashCode());
        return result;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public void setSchemaExtensions(List<SchemaExtension> schemaExtensions) {
        this.schemaExtensions = schemaExtensions;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ResourceTypeImpl [");
        if (description != null) {
            builder.append("description=");
            builder.append(description);
            builder.append(", ");
        }
        if (endpoint != null) {
            builder.append("endpoint=");
            builder.append(endpoint);
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
            builder.append(", ");
        }
        if (schema != null) {
            builder.append("schema=");
            builder.append(schema);
            builder.append(", ");
        }
        if (schemaExtensions != null) {
            builder.append("schemaExtensions=");
            builder.append(schemaExtensions);
            builder.append(", ");
        }
        if (schemas != null) {
            builder.append("schemas=");
            builder.append(schemas);
        }
        builder.append("]");
        return builder.toString();
    }
}
