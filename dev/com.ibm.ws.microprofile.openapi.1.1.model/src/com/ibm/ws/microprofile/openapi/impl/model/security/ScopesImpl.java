/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.openapi.impl.model.security;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.security.Scopes;

import com.ibm.ws.microprofile.openapi.model.utils.OpenAPIUtils;

/**
 * Scopes
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#scopedObject"
 */

public class ScopesImpl extends LinkedHashMap<String, String> implements Scopes {
    public ScopesImpl() {}

    private java.util.Map<String, Object> extensions = null;

    @Override
    public Scopes addScope(String name, String item) {
        this.put(name, item);
        return this;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ScopesImpl) {
            ScopesImpl scopes = (ScopesImpl) o;
            return Objects.equals(this.extensions, scopes.extensions) &&
                   super.equals(o);
        } else {
            return super.equals(o);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(extensions, super.hashCode());
    }

    @Override
    public java.util.Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public Scopes addExtension(String name, Object value) {
        if (this.extensions == null) {
            this.extensions = new java.util.HashMap<>();
        }
        this.extensions.put(name, value);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void removeExtension(String key) {
        if (this.extensions != null) {
            this.extensions.remove(key);
        }
    }

    @Override
    public void setExtensions(java.util.Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Scopes {\n");
        sb = (super.toString() != null) ? sb.append("    ").append(toIndentedString(super.toString())).append("\n") : sb.append("");
        sb = (extensions != null) ? sb.append("    extensions: ").append(OpenAPIUtils.mapToString(extensions)).append("\n") : sb.append("");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Converts the given object to string with each line indented by 4 spaces
     * (except the first line).
     * This method adds formatting to the general toString() method.
     *
     * @param o Java object to be represented as String
     * @return Formatted String representation of the object
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getScopes() {
        return Collections.unmodifiableMap(this);
    }

    /** {@inheritDoc} */
    @Override
    public void removeScope(String key) {
        remove(key);

    }

    /** {@inheritDoc} */
    @Override
    public void setScopes(Map<String, String> scopes) {
        clear();
        putAll(scopes);
    }

}
