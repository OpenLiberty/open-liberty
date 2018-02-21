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

package com.ibm.ws.microprofile.openapi.impl.model.examples;

import org.eclipse.microprofile.openapi.models.examples.Example;

import com.ibm.ws.microprofile.openapi.Constants;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

/**
 * Example
 */

public class ExampleImpl implements Example {
    private String summary = null;
    private String description = null;
    private Object value = null;
    private String externalValue = null;
    private String $ref = null;
    private java.util.Map<String, Object> extensions = null;

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Override
    public Example summary(String summary) {
        this.summary = summary;
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
    public Example description(String description) {
        this.description = description;
        return this;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public Example value(Object value) {
        this.value = value;
        return this;
    }

    @Override
    public String getExternalValue() {
        return externalValue;
    }

    @Override
    public void setExternalValue(String externalValue) {
        this.externalValue = externalValue;
    }

    @Override
    public Example externalValue(String externalValue) {
        this.externalValue = externalValue;
        return this;
    }

    @Override
    public String getRef() {
        return $ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && (ref.indexOf(".") == -1 && ref.indexOf("/") == -1)) {
            ref = "#/components/examples/" + ref;
        }
        this.$ref = ref;
    }

    @Override
    public Example ref(String $ref) {
        setRef($ref);
        return this;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExampleImpl)) {
            return false;
        }

        ExampleImpl example = (ExampleImpl) o;

        if (summary != null ? !summary.equals(example.summary) : example.summary != null) {
            return false;
        }
        if (description != null ? !description.equals(example.description) : example.description != null) {
            return false;
        }
        if (value != null ? !value.equals(example.value) : example.value != null) {
            return false;
        }
        if (externalValue != null ? !externalValue.equals(example.externalValue) : example.externalValue != null) {
            return false;
        }
        if ($ref != null ? !$ref.equals(example.$ref) : example.$ref != null) {
            return false;
        }
        return extensions != null ? extensions.equals(example.extensions) : example.extensions == null;

    }

    @Override
    public int hashCode() {
        int result = summary != null ? summary.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (externalValue != null ? externalValue.hashCode() : 0);
        result = 31 * result + ($ref != null ? $ref.hashCode() : 0);
        result = 31 * result + (extensions != null ? extensions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Example {\n");
        sb = (!toIndentedString(summary).equals(Constants.NULL_VALUE)) ? sb.append("    summary: ").append(toIndentedString(summary)).append("\n") : sb.append("");
        sb = (!toIndentedString(description).equals(Constants.NULL_VALUE)) ? sb.append("    description: ").append(toIndentedString(description)).append("\n") : sb.append("");
        sb = (!toIndentedString(value).equals(Constants.NULL_VALUE)) ? sb.append("    value: ").append(toIndentedString(value)).append("\n") : sb.append("");
        sb = (!toIndentedString(externalValue).equals(Constants.NULL_VALUE)) ? sb.append("    externalValue: ").append(toIndentedString(externalValue)).append("\n") : sb.append("");
        sb = (!toIndentedString($ref).equals(Constants.NULL_VALUE)) ? sb.append("    $ref: ").append(toIndentedString($ref)).append("\n") : sb.append("");
        sb = (!toIndentedString(extensions).equals(Constants.NULL_VALUE)) ? sb.append("    extensions: ").append(OpenAPIUtils.mapToString(extensions)).append("\n") : sb.append("");

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
