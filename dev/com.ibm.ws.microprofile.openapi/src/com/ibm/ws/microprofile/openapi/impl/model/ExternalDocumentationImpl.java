/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl.model;

import java.util.Objects;

import org.eclipse.microprofile.openapi.models.ExternalDocumentation;

import com.ibm.ws.microprofile.openapi.Constants;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

/**
 * ExternalDocumentation
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#externalDocumentationObject"
 */

public class ExternalDocumentationImpl implements ExternalDocumentation {
    private String description = null;
    private String url = null;
    private java.util.Map<String, Object> extensions = null;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public ExternalDocumentation description(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public ExternalDocumentation url(String url) {
        this.url = url;
        return this;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExternalDocumentationImpl externalDocumentation = (ExternalDocumentationImpl) o;
        return Objects.equals(this.description, externalDocumentation.description) &&
               Objects.equals(this.url, externalDocumentation.url) &&
               Objects.equals(this.extensions, externalDocumentation.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, url, extensions);
    }

    @Override
    public java.util.Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public void addExtension(String name, Object value) {
        if (this.extensions == null) {
            this.extensions = new java.util.HashMap<>();
        }
        this.extensions.put(name, value);
    }

    @Override
    public void setExtensions(java.util.Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ExternalDocumentation {\n");
        sb = (description != null) ? sb.append("    description: ").append(toIndentedString(description)).append("\n") : sb.append("");
        sb = (url != null) ? sb.append("    url: ").append(toIndentedString(url)).append("\n") : sb.append("");
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

}
