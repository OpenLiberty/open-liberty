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
package com.ibm.ws.microprofile.openapi.impl.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;

import com.ibm.ws.microprofile.openapi.model.utils.OpenAPIUtils;

/**
 * Paths
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#pathsObject"
 */
public class PathsImpl extends LinkedHashMap<String, PathItem> implements Paths {
    /**  */
    private static final long serialVersionUID = 1L;

    public PathsImpl() {}

    private java.util.Map<String, Object> extensions = null;

    @Override
    public Paths addPathItem(String name, PathItem item) {
        if (item == null) {
            return this;
        }
        this.put(name, item);
        return this;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof PathsImpl) {
            PathsImpl paths = (PathsImpl) o;
            return Objects.equals(this.extensions, paths.extensions) &&
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
    public Paths addExtension(String name, Object value) {
        if (value == null) {
            return this;
        }
        if (this.extensions == null) {
            this.extensions = new java.util.HashMap<>();
        }
        this.extensions.put(name, value);
        return this;
    }

    @Override
    public void setExtensions(java.util.Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    /** {@inheritDoc} */
    @Override
    public void removeExtension(String key) {
        if (this.extensions != null) {
            this.extensions.remove(key);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Paths {\n");

        sb = (super.toString() != null) ? sb.append("    ").append(toIndentedString(super.toString())).append("\n") : sb.append("");
        sb = (extensions != null) ? sb.append("    extensions: ").append(OpenAPIUtils.mapToString(extensions)).append("\n") : sb.append("");

        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, PathItem> getPathItems() {
        return Collections.unmodifiableMap(this);
    }

    /** {@inheritDoc} */
    @Override
    public void removePathItem(String pathItem) {
        remove(pathItem);
    }

    /** {@inheritDoc} */
    @Override
    public void setPathItems(Map<String, PathItem> arg0) {
        clear();
        this.putAll(arg0);
    }

}
