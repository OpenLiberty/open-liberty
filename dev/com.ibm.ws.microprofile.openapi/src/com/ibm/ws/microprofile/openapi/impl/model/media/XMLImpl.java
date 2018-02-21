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
package com.ibm.ws.microprofile.openapi.impl.model.media;

import java.util.Objects;

import org.eclipse.microprofile.openapi.models.media.XML;

import com.ibm.ws.microprofile.openapi.Constants;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

/**
 * XML
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#xmlObject"
 */

public class XMLImpl implements XML {
    private String name = null;
    private String namespace = null;
    private String prefix = null;
    private Boolean attribute = null;
    private Boolean wrapped = null;
    private java.util.Map<String, Object> extensions = null;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public XML name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public XML namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public XML prefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public Boolean getAttribute() {
        return attribute;
    }

    @Override
    public void setAttribute(Boolean attribute) {
        this.attribute = attribute;
    }

    @Override
    public XML attribute(Boolean attribute) {
        this.attribute = attribute;
        return this;
    }

    @Override
    public Boolean getWrapped() {
        return wrapped;
    }

    @Override
    public void setWrapped(Boolean wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public XML wrapped(Boolean wrapped) {
        this.wrapped = wrapped;
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
        XMLImpl XML = (XMLImpl) o;
        return Objects.equals(this.name, XML.name) &&
               Objects.equals(this.namespace, XML.namespace) &&
               Objects.equals(this.prefix, XML.prefix) &&
               Objects.equals(this.attribute, XML.attribute) &&
               Objects.equals(this.wrapped, XML.wrapped) &&
               Objects.equals(this.extensions, XML.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, namespace, prefix, attribute, wrapped, extensions);
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
        sb.append("class XML {\n");
        sb = (name != null) ? sb.append("    name: ").append(toIndentedString(name)).append("\n") : sb.append("");
        sb = (namespace != null) ? sb.append("    namespace: ").append(toIndentedString(namespace)).append("\n") : sb.append("");
        sb = (prefix != null) ? sb.append("    prefix: ").append(toIndentedString(prefix)).append("\n") : sb.append("");
        sb = (attribute != null) ? sb.append("    attribute: ").append(toIndentedString(attribute)).append("\n") : sb.append("");
        sb = (wrapped != null) ? sb.append("    wrapped: ").append(toIndentedString(wrapped)).append("\n") : sb.append("");
        sb = (extensions != null) ? sb.append("    extensions: ").append(OpenAPIUtils.mapToString(extensions)).append("\n") : sb.append("");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
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

}
