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
package com.ibm.ws.microprofile.openapi.impl.model.servers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.servers.ServerVariable;

import com.ibm.ws.microprofile.openapi.model.utils.OpenAPIUtils;

/**
 * ServerVariable
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#serverVariableObject"
 */

public class ServerVariableImpl implements ServerVariable {
    private List<String> _enum = null;
    private String _default = null;
    private String description = null;
    private java.util.Map<String, Object> extensions = null;

    @Override
    public List<String> getEnumeration() {
        return _enum;
    }

    @Override
    public void setEnumeration(List<String> enumeration) {
        this._enum = enumeration;
    }

    @Override
    public ServerVariable enumeration(List<String> enumeration) {
        this._enum = enumeration;
        return this;
    }

    @Override
    public ServerVariable addEnumeration(String enumeration) {
        if (this._enum == null) {
            this._enum = new ArrayList<String>();
        }
        this._enum.add(enumeration);
        return this;
    }

    @Override
    public String getDefaultValue() {
        return _default;
    }

    @Override
    public void setDefaultValue(String _default) {
        this._default = _default;
    }

    @Override
    public ServerVariable defaultValue(String _default) {
        this._default = _default;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public ServerVariable description(String description) {
        this.description = description;
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
        ServerVariableImpl serverVariable = (ServerVariableImpl) o;
        return Objects.equals(this._enum, serverVariable._enum) &&
               Objects.equals(this._default, serverVariable._default) &&
               Objects.equals(this.description, serverVariable.description) &&
               Objects.equals(this.extensions, serverVariable.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_enum, _default, description, extensions);
    }

    @Override
    public java.util.Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public ServerVariable addExtension(String name, Object value) {
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
        sb.append("class ServerVariable {\n");

        sb = (_enum != null) ? sb.append("    _enum: ").append(toIndentedString(_enum)).append("\n") : sb.append("");
        sb = (_default != null) ? sb.append("    _default: ").append(toIndentedString(_default)).append("\n") : sb.append("");
        sb = (description != null) ? sb.append("    description: ").append(toIndentedString(description)).append("\n") : sb.append("");
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
    public void removeEnumeration(String enumeration) {
        _enum.remove(enumeration);

    }

}
