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
package com.ibm.ws.microprofile.openapi.impl.model.parameters;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

import com.ibm.ws.microprofile.openapi.Constants;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

/**
 * Parameter
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#parameterObject"
 */

public class ParameterImpl implements Parameter {
    private String name = null;
    protected In in = null;
    private String description = null;
    private Boolean required = null;
    private Boolean deprecated = null;
    private Boolean allowEmptyValue = null;
    private String $ref = null;

    private Parameter.Style style = null;
    private Boolean explode = null;
    private Boolean allowReserved = null;
    private Schema schema = null;
    private Map<String, Example> examples = null;
    private Object example = null;
    private Content content = null;
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
    public Parameter name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public In getIn() {
        return in;
    }

    @Override
    public void setIn(In in) {
        if (in == In.PATH) {
            this.required = true;
        }
        this.in = in;
    }

    @Override
    public Parameter in(In in) {
        setIn(in);
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
    public Parameter description(String description) {
        this.description = description;
        return this;
    }

    @Override
    public Boolean getRequired() {
        return required;
    }

    @Override
    public void setRequired(Boolean required) {
        this.required = required;
    }

    @Override
    public Parameter required(Boolean required) {
        this.required = required;
        return this;
    }

    @Override
    public Boolean getDeprecated() {
        return deprecated;
    }

    @Override
    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    @Override
    public Parameter deprecated(Boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    @Override
    public Boolean getAllowEmptyValue() {
        return allowEmptyValue;
    }

    @Override
    public void setAllowEmptyValue(Boolean allowEmptyValue) {
        this.allowEmptyValue = allowEmptyValue;
    }

    @Override
    public Parameter allowEmptyValue(Boolean allowEmptyValue) {
        this.allowEmptyValue = allowEmptyValue;
        return this;
    }

    @Override
    public Parameter.Style getStyle() {
        return style;
    }

    @Override
    public void setStyle(Parameter.Style style) {
        this.style = style;
    }

    @Override
    public Parameter style(Parameter.Style style) {
        this.style = style;
        return this;
    }

    @Override
    public Boolean getExplode() {
        return explode;
    }

    @Override
    public void setExplode(Boolean explode) {
        this.explode = explode;
    }

    @Override
    public Parameter explode(Boolean explode) {
        this.explode = explode;
        return this;
    }

    @Override
    public Boolean getAllowReserved() {
        return allowReserved;
    }

    @Override
    public void setAllowReserved(Boolean allowReserved) {
        this.allowReserved = allowReserved;
    }

    @Override
    public Parameter allowReserved(Boolean allowReserved) {
        this.allowReserved = allowReserved;
        return this;
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    @Override
    public Parameter schema(Schema schema) {
        this.schema = schema;
        return this;
    }

    @Override
    public Map<String, Example> getExamples() {
        return examples;
    }

    @Override
    public void setExamples(Map<String, Example> examples) {
        this.examples = examples;
    }

    @Override
    public Parameter examples(Map<String, Example> examples) {
        this.examples = examples;
        return this;
    }

    @Override
    public Parameter addExample(String key, Example example) {
        if (this.examples == null) {
            this.examples = new HashMap<String, Example>();
        }
        this.examples.put(key, example);
        return this;
    }

    @Override
    public Object getExample() {
        return example;
    }

    @Override
    public void setExample(Object example) {
        this.example = example;
    }

    @Override
    public Parameter example(Object example) {
        this.example = example;
        return this;
    }

    @Override
    public Content getContent() {
        return content;
    }

    @Override
    public void setContent(Content content) {
        this.content = content;
    }

    @Override
    public Parameter content(Content content) {
        this.content = content;
        return this;
    }

    @Override
    public String getRef() {
        return $ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && (ref.indexOf(".") == -1 && ref.indexOf("/") == -1)) {
            ref = "#/components/parameters/" + ref;
        }
        this.$ref = ref;
    }

    @Override
    public Parameter ref(String ref) {
        setRef(ref);
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
        ParameterImpl parameter = (ParameterImpl) o;
        return Objects.equals(this.name, parameter.name) &&
               Objects.equals(this.in, parameter.in) &&
               Objects.equals(this.description, parameter.description) &&
               Objects.equals(this.required, parameter.required) &&
               Objects.equals(this.deprecated, parameter.deprecated) &&
               Objects.equals(this.allowEmptyValue, parameter.allowEmptyValue) &&
               Objects.equals(this.style, parameter.style) &&
               Objects.equals(this.explode, parameter.explode) &&
               Objects.equals(this.allowReserved, parameter.allowReserved) &&
               Objects.equals(this.schema, parameter.schema) &&
               Objects.equals(this.examples, parameter.examples) &&
               Objects.equals(this.example, parameter.example) &&
               Objects.equals(this.content, parameter.content) &&
               Objects.equals(this.extensions, parameter.extensions) &&
               Objects.equals(this.$ref, parameter.$ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, in, description, required, deprecated, allowEmptyValue, style, explode, allowReserved, schema, examples, example, content, extensions, $ref);
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
        sb.append("class Parameter {\n");
        sb = (name != null) ? sb.append("    name: ").append(toIndentedString(name)).append("\n") : sb.append("");
        sb = (in != null) ? sb.append("    in: ").append(toIndentedString(in)).append("\n") : sb.append("");
        sb = (description != null) ? sb.append("    description: ").append(toIndentedString(description)).append("\n") : sb.append("");
        sb = (required != null) ? sb.append("    required: ").append(toIndentedString(required)).append("\n") : sb.append("");
        sb = (deprecated != null) ? sb.append("    deprecated: ").append(toIndentedString(deprecated)).append("\n") : sb.append("");
        sb = (allowEmptyValue != null) ? sb.append("    allowEmptyValue: ").append(toIndentedString(allowEmptyValue)).append("\n") : sb.append("");
        sb = (style != null) ? sb.append("    style: ").append(toIndentedString(style)).append("\n") : sb.append("");
        sb = (explode != null) ? sb.append("    explode: ").append(toIndentedString(explode)).append("\n") : sb.append("");
        sb = (allowReserved != null) ? sb.append("    allowReserved: ").append(toIndentedString(allowReserved)).append("\n") : sb.append("");
        sb = (schema != null) ? sb.append("    schema: ").append(toIndentedString(schema)).append("\n") : sb.append("");
        sb = (examples != null) ? sb.append("    examples: ").append(OpenAPIUtils.mapToString(examples)).append("\n") : sb.append("");
        sb = (example != null) ? sb.append("    example: ").append(toIndentedString(example)).append("\n") : sb.append("");
        sb = (content != null) ? sb.append("    content: ").append(toIndentedString(content)).append("\n") : sb.append("");
        sb = ($ref != null) ? sb.append("    $ref: ").append(toIndentedString($ref)).append("\n") : sb.append("");
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

}
