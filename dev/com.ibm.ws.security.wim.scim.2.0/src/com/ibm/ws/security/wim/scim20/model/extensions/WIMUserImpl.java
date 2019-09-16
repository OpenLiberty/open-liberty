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

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.extensions.WIMIdentifier;
import com.ibm.websphere.security.wim.scim20.model.extensions.WIMUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "identifier", "uid" })
public class WIMUserImpl implements WIMUser {

    public static final String SCHEMA_URN = "urn:ietf:params:scim:schemas:extension:wim:2.0:User";

    /**
     * Contains any extended properties for a WIMUser. TreeMap sorts
     * alphabetically.
     */
    public Map<String, Object> extendedPropertyValues = new TreeMap<String, Object>();

    @JsonProperty("identifier")
    private WIMIdentifier identifier;

    @JsonProperty("uid")
    private String uid;

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
        WIMUserImpl other = (WIMUserImpl) obj;
        if (extendedPropertyValues == null) {
            if (other.extendedPropertyValues != null) {
                return false;
            }
        } else if (!extendedPropertyValues.equals(other.extendedPropertyValues)) {
            return false;
        }
        if (identifier == null) {
            if (other.identifier != null) {
                return false;
            }
        } else if (!identifier.equals(other.identifier)) {
            return false;
        }
        if (uid == null) {
            if (other.uid != null) {
                return false;
            }
        } else if (!uid.equals(other.uid)) {
            return false;
        }
        return true;
    }

    @Override
    @JsonAnyGetter
    public Map<String, Object> getExtendedProperties() {
        return Collections.unmodifiableMap(extendedPropertyValues);
    }

    @Override
    public WIMIdentifier getIdentifier() {
        return this.identifier;
    }

    @Override
    public String getUid() {
        return this.uid;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((extendedPropertyValues == null) ? 0 : extendedPropertyValues.hashCode());
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime * result + ((uid == null) ? 0 : uid.hashCode());
        return result;
    }

    @Override
    @JsonAnySetter
    public void setExtendedProperty(String property, Object value) {
        extendedPropertyValues.put(property, value);
    }

    public void setIdentifier(WIMIdentifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WIMUserImpl [");
        if (extendedPropertyValues != null) {
            builder.append("extendedPropertyValues=");
            builder.append(extendedPropertyValues);
            builder.append(", ");
        }
        if (identifier != null) {
            builder.append("identifier=");
            builder.append(identifier);
            builder.append(", ");
        }
        if (uid != null) {
            builder.append("uid=");
            builder.append(uid);
        }
        builder.append("]");
        return builder.toString();
    }
}
