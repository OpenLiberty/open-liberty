/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.scim20.model;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.Error;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "schemas", "detail", "scimType", "status" })
public class ErrorImpl implements Error {

    public static final String SCHEMA_URI = "urn:ietf:params:scim:api:messages:2.0:Error";

    @JsonProperty("detail")
    private String detail;

    @JsonProperty("schemas")
    private final List<String> schemas;

    @JsonProperty("scimType")
    private String scimType;

    @JsonProperty("status")
    private Integer status;

    public ErrorImpl() {
        schemas = Arrays.asList(new String[] { SCHEMA_URI });
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
        ErrorImpl other = (ErrorImpl) obj;
        if (detail == null) {
            if (other.detail != null) {
                return false;
            }
        } else if (!detail.equals(other.detail)) {
            return false;
        }
        if (schemas == null) {
            if (other.schemas != null) {
                return false;
            }
        } else if (!schemas.equals(other.schemas)) {
            return false;
        }
        if (scimType == null) {
            if (other.scimType != null) {
                return false;
            }
        } else if (!scimType.equals(other.scimType)) {
            return false;
        }
        if (status == null) {
            if (other.status != null) {
                return false;
            }
        } else if (!status.equals(other.status)) {
            return false;
        }
        return true;
    }

    @Override
    public String getDetail() {
        return detail;
    }

    @Override
    public List<String> getSchemas() {
        return schemas;
    }

    @Override
    public String getScimType() {
        return scimType;
    }

    @Override
    public Integer getStatus() {
        return status;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((detail == null) ? 0 : detail.hashCode());
        result = prime * result + ((schemas == null) ? 0 : schemas.hashCode());
        result = prime * result + ((scimType == null) ? 0 : scimType.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        return result;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public void setScimType(String scimType) {
        this.scimType = scimType;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ErrorImpl [");
        if (detail != null) {
            builder.append("detail=");
            builder.append(detail);
            builder.append(", ");
        }
        if (schemas != null) {
            builder.append("schemas=");
            builder.append(schemas);
            builder.append(", ");
        }
        if (scimType != null) {
            builder.append("scimType=");
            builder.append(scimType);
            builder.append(", ");
        }
        if (status != null) {
            builder.append("status=");
            builder.append(status);
        }
        builder.append("]");
        return builder.toString();
    }
}
