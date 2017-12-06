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
package com.ibm.ws.microprofile.openapi.impl.model.headers;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;

/**
 * Header
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#headerObject"
 */

public class HeaderImpl implements Header {
    private String description = null;
    private String $ref = null;
    private Boolean required = null;
    private Boolean deprecated = null;
    private Boolean allowEmptyValue = null;

    private Style style = null;
    private Boolean explode = null;
    private Schema schema = null;
    private Map<String, Example> examples = null;
    private Object example = null;
    private Content content = null;
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
    public Header description(String description) {
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
    public Header required(Boolean required) {
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
    public Header deprecated(Boolean deprecated) {
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
    public Header allowEmptyValue(Boolean allowEmptyValue) {
        this.allowEmptyValue = allowEmptyValue;
        return this;
    }

    @Override
    public Style getStyle() {
        return style;
    }

    @Override
    public void setStyle(Style style) {
        this.style = style;
    }

    @Override
    public Header style(Style style) {
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
    public Header explode(Boolean explode) {
        this.explode = explode;
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
    public Header schema(Schema schema) {
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
    public Header examples(Map<String, Example> examples) {
        this.examples = examples;
        return this;
    }

    @Override
    public Header addExample(String key, Example examplesItem) {
        if (this.examples == null) {
            this.examples = new HashMap<String, Example>();
        }
        this.examples.put(key, examplesItem);
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
    public Header example(Object example) {
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
    public Header content(Content content) {
        this.content = content;
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
        HeaderImpl header = (HeaderImpl) o;
        return Objects.equals(this.description, header.description) &&
               Objects.equals(this.required, header.required) &&
               Objects.equals(this.deprecated, header.deprecated) &&
               Objects.equals(this.allowEmptyValue, header.allowEmptyValue) &&
               Objects.equals(this.style, header.style) &&
               Objects.equals(this.explode, header.explode) &&
               Objects.equals(this.schema, header.schema) &&
               Objects.equals(this.examples, header.examples) &&
               Objects.equals(this.example, header.example) &&
               Objects.equals(this.content, header.content) &&
               Objects.equals(this.extensions, header.extensions) &&
               Objects.equals(this.$ref, header.$ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, required, deprecated, allowEmptyValue, style, explode, schema, examples, example, content, extensions, $ref);
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
    public String getRef() {
        return $ref;
    }

    @Override
    public void setRef(String $ref) {
        if ($ref != null && ($ref.indexOf(".") == -1 && $ref.indexOf("/") == -1)) {
            $ref = "#/components/headers/" + $ref;
        }
        this.$ref = $ref;
    }

    @Override
    public Header ref(String $ref) {
        this.$ref = $ref;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Header {\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    required: ").append(toIndentedString(required)).append("\n");
        sb.append("    deprecated: ").append(toIndentedString(deprecated)).append("\n");
        sb.append("    allowEmptyValue: ").append(toIndentedString(allowEmptyValue)).append("\n");
        sb.append("    style: ").append(toIndentedString(style)).append("\n");
        sb.append("    explode: ").append(toIndentedString(explode)).append("\n");
        sb.append("    schema: ").append(toIndentedString(schema)).append("\n");
        sb.append("    examples: ").append(toIndentedString(examples)).append("\n");
        sb.append("    example: ").append(toIndentedString(example)).append("\n");
        sb.append("    content: ").append(toIndentedString(content)).append("\n");
        sb.append("    $ref: ").append(toIndentedString($ref)).append("\n");
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
