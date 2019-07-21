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
import com.ibm.websphere.security.wim.scim20.model.serviceprovider.Bulk;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "supported", "maxOperations", "maxPayloadSize" })
public class BulkImpl implements Bulk {

    @JsonProperty("maxOperations")
    private final Integer maxOperations = 0;

    @JsonProperty("maxPayloadSize")
    private final Integer maxPayloadSize = 0;

    @JsonProperty("supported")
    private final Boolean supported = Boolean.FALSE;

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
        BulkImpl other = (BulkImpl) obj;
        if (maxOperations == null) {
            if (other.maxOperations != null) {
                return false;
            }
        } else if (!maxOperations.equals(other.maxOperations)) {
            return false;
        }
        if (maxPayloadSize == null) {
            if (other.maxPayloadSize != null) {
                return false;
            }
        } else if (!maxPayloadSize.equals(other.maxPayloadSize)) {
            return false;
        }
        if (supported == null) {
            if (other.supported != null) {
                return false;
            }
        } else if (!supported.equals(other.supported)) {
            return false;
        }
        return true;
    }

    @Override
    public Integer getMaxOperations() {
        return this.maxOperations;
    }

    @Override
    public Integer getMaxPayloadSize() {
        return this.maxPayloadSize;
    }

    @Override
    public Boolean getSupported() {
        return this.supported;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((maxOperations == null) ? 0 : maxOperations.hashCode());
        result = prime * result + ((maxPayloadSize == null) ? 0 : maxPayloadSize.hashCode());
        result = prime * result + ((supported == null) ? 0 : supported.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BulkImpl [");
        if (maxOperations != null) {
            builder.append("maxOperations=");
            builder.append(maxOperations);
            builder.append(", ");
        }
        if (maxPayloadSize != null) {
            builder.append("maxPayloadSize=");
            builder.append(maxPayloadSize);
            builder.append(", ");
        }
        if (supported != null) {
            builder.append("supported=");
            builder.append(supported);
        }
        builder.append("]");
        return builder.toString();
    }
}
