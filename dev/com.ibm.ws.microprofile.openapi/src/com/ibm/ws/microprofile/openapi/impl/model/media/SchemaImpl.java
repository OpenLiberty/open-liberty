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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.XML;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Schema
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#schemaObject"
 */
public class SchemaImpl implements Schema {
    protected Object _default;

    private String name;
    private String title = null;
    private BigDecimal multipleOf = null;
    private BigDecimal maximum = null;
    private Boolean exclusiveMaximum = null;
    private BigDecimal minimum = null;
    private Boolean exclusiveMinimum = null;
    private Integer maxLength = null;
    private Integer minLength = null;
    private String pattern = null;
    private Integer maxItems = null;
    private Integer minItems = null;
    private Boolean uniqueItems = null;
    private Integer maxProperties = null;
    private Integer minProperties = null;
    private List<String> required = null;
    private SchemaType type = null;
    private Schema not = null;
    private Map<String, Schema> properties = null;
    private String description = null;
    private String format = null;
    private String $ref = null;
    private Boolean nullable = null;
    private Boolean readOnly = null;
    private Boolean writeOnly = null;
    protected Object example = null;
    private ExternalDocumentation externalDocs = null;
    private Boolean deprecated = null;
    private XML xml = null;
    private java.util.Map<String, Object> extensions = null;
    protected List<Object> _enum = null;
    private Discriminator discriminator = null;

    private List<Schema> anyOf = null;
    private List<Schema> allOf = null;
    private List<Schema> oneOf = null;

    private Object additionalProperties;
    private Schema items = null;

    @Override
    public Discriminator getDiscriminator() {
        return discriminator;
    }

    @Override
    public void setDiscriminator(Discriminator discriminator) {
        this.discriminator = discriminator;
    }

    @Override
    public Schema discriminator(Discriminator discriminator) {
        this.discriminator = discriminator;
        return this;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public Schema title(String title) {
        this.title = title;
        return this;
    }

    @Override
    public Object getDefaultValue() {
        return _default;
    }

    @Override
    public void setDefaultValue(Object _default) {
        this._default = _default;
    }

    @Override
    public List<Object> getEnumeration() {
        return _enum;
    }

    @Override
    public void setEnumeration(List<Object> _enum) {
        this._enum = _enum;
    }

    @Override
    public Schema addEnumeration(Object _enumItem) {
        if (this._enum == null) {
            this._enum = new ArrayList<>();
        }
        this._enum.add(_enumItem);
        return this;
    }

    @Override
    public BigDecimal getMultipleOf() {
        return multipleOf;
    }

    @Override
    public void setMultipleOf(BigDecimal multipleOf) {
        this.multipleOf = multipleOf;
    }

    @Override
    public Schema multipleOf(BigDecimal multipleOf) {
        this.multipleOf = multipleOf;
        return this;
    }

    @Override
    public BigDecimal getMaximum() {
        return maximum;
    }

    @Override
    public void setMaximum(BigDecimal maximum) {
        this.maximum = maximum;
    }

    @Override
    public Schema maximum(BigDecimal maximum) {
        this.maximum = maximum;
        return this;
    }

    @Override
    public Boolean getExclusiveMaximum() {
        return exclusiveMaximum;
    }

    @Override
    public void setExclusiveMaximum(Boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    @Override
    public Schema exclusiveMaximum(Boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
        return this;
    }

    @Override
    public BigDecimal getMinimum() {
        return minimum;
    }

    @Override
    public void setMinimum(BigDecimal minimum) {
        this.minimum = minimum;
    }

    @Override
    public Schema minimum(BigDecimal minimum) {
        this.minimum = minimum;
        return this;
    }

    @Override
    public Boolean getExclusiveMinimum() {
        return exclusiveMinimum;
    }

    @Override
    public void setExclusiveMinimum(Boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    @Override
    public Schema exclusiveMinimum(Boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
        return this;
    }

    @Override
    public Integer getMaxLength() {
        return maxLength;
    }

    @Override
    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public Schema maxLength(Integer maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    @Override
    public Integer getMinLength() {
        return minLength;
    }

    @Override
    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    @Override
    public Schema minLength(Integer minLength) {
        this.minLength = minLength;
        return this;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public Schema pattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    @Override
    public Integer getMaxItems() {
        return maxItems;
    }

    @Override
    public void setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
    }

    @Override
    public Schema maxItems(Integer maxItems) {
        this.maxItems = maxItems;
        return this;
    }

    @Override
    public Integer getMinItems() {
        return minItems;
    }

    @Override
    public void setMinItems(Integer minItems) {
        this.minItems = minItems;
    }

    @Override
    public Schema minItems(Integer minItems) {
        this.minItems = minItems;
        return this;
    }

    @Override
    public Boolean getUniqueItems() {
        return uniqueItems;
    }

    @Override
    public void setUniqueItems(Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
    }

    @Override
    public Schema uniqueItems(Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
        return this;
    }

    @Override
    public Integer getMaxProperties() {
        return maxProperties;
    }

    @Override
    public void setMaxProperties(Integer maxProperties) {
        this.maxProperties = maxProperties;
    }

    @Override
    public Schema maxProperties(Integer maxProperties) {
        this.maxProperties = maxProperties;
        return this;
    }

    @Override
    public Integer getMinProperties() {
        return minProperties;
    }

    @Override
    public void setMinProperties(Integer minProperties) {
        this.minProperties = minProperties;
    }

    @Override
    public Schema minProperties(Integer minProperties) {
        this.minProperties = minProperties;
        return this;
    }

    @Override
    public List<String> getRequired() {
        return required;
    }

    @Override
    public void setRequired(List<String> required) {
        /*
         * List<String> list = new ArrayList<>();
         * if (required != null) {
         * for (String req : required) {
         * if (this.properties == null) {
         * list.add(req);
         * } else if (this.properties.containsKey(req)) {
         * list.add(req);
         * }
         * }
         * }
         * Collections.sort(list);
         * if (list.size() == 0) {
         * list = null;
         * }
         */
        this.required = required;
    }

    @Override
    public Schema required(List<String> required) {
        this.required = required;
        return this;
    }

    @Override
    public Schema addRequired(String requiredItem) {
        if (this.required == null) {
            this.required = new ArrayList<String>();
        }
        this.required.add(requiredItem);
        Collections.sort(required);
        return this;
    }

    @Override
    public SchemaType getType() {
        return type;
    }

    @Override
    public void setType(SchemaType type) {
        this.type = type;
    }

    @Override
    public Schema type(SchemaType type) {
        this.type = type;
        return this;
    }

    @Override
    public Schema getNot() {
        return not;
    }

    @Override
    public void setNot(Schema not) {
        this.not = not;
    }

    @Override
    public Schema not(Schema not) {
        this.not = not;
        return this;
    }

    @Override
    public Map<String, Schema> getProperties() {
        return properties;
    }

    @Override
    public void setProperties(Map<String, Schema> properties) {
        this.properties = properties;
    }

    @Override
    public Schema properties(Map<String, Schema> properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public Schema addProperty(String key, Schema propertiesItem) {
        if (this.properties == null) {
            this.properties = new LinkedHashMap<String, Schema>();
        }
        this.properties.put(key, propertiesItem);
        return this;
    }

    @Override
    public Object getAdditionalProperties() {
        return additionalProperties;
    }

    @Override
    public void setAdditionalProperties(Schema additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    @Override
    public Schema additionalProperties(Schema additionalProperties) {
        this.additionalProperties = additionalProperties;
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
    public Schema description(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public Schema format(String format) {
        this.format = format;
        return this;
    }

    @Override
    public String getRef() {
        return $ref;
    }

    @Override
    public void setRef(String $ref) {
        if ($ref != null && ($ref.indexOf(".") == -1 && $ref.indexOf("/") == -1)) {
            $ref = "#/components/schemas/" + $ref;
        }
        this.$ref = $ref;
    }

    @Override
    public Schema ref(String ref) {

        setRef(ref);
        return this;
    }

    @Override
    public Boolean getNullable() {
        return nullable;
    }

    @Override
    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    @Override
    public Schema nullable(Boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    @Override
    public Boolean getReadOnly() {
        return readOnly;
    }

    @Override
    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public Schema readOnly(Boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    @Override
    public Boolean getWriteOnly() {
        return writeOnly;
    }

    @Override
    public void setWriteOnly(Boolean writeOnly) {
        this.writeOnly = writeOnly;
    }

    @Override
    public Schema writeOnly(Boolean writeOnly) {
        this.writeOnly = writeOnly;
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
    public Schema example(Object example) {
        this.example = example;
        return this;
    }

    @Override
    public ExternalDocumentation getExternalDocs() {
        return externalDocs;
    }

    @Override
    public void setExternalDocs(ExternalDocumentation externalDocs) {
        this.externalDocs = externalDocs;
    }

    @Override
    public Schema externalDocs(ExternalDocumentation externalDocs) {
        this.externalDocs = externalDocs;
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
    public Schema deprecated(Boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    @Override
    public XML getXml() {
        return xml;
    }

    @Override
    public void setXml(XML xml) {
        this.xml = xml;
    }

    @Override
    public Schema xml(XML xml) {
        this.xml = xml;
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
        SchemaImpl schema = (SchemaImpl) o;
        return Objects.equals(this.name, schema.name) &&
               Objects.equals(this.title, schema.title) &&
               Objects.equals(this.multipleOf, schema.multipleOf) &&
               Objects.equals(this.maximum, schema.maximum) &&
               Objects.equals(this.exclusiveMaximum, schema.exclusiveMaximum) &&
               Objects.equals(this.minimum, schema.minimum) &&
               Objects.equals(this.exclusiveMinimum, schema.exclusiveMinimum) &&
               Objects.equals(this.maxLength, schema.maxLength) &&
               Objects.equals(this.minLength, schema.minLength) &&
               Objects.equals(this.pattern, schema.pattern) &&
               Objects.equals(this.maxItems, schema.maxItems) &&
               Objects.equals(this.minItems, schema.minItems) &&
               Objects.equals(this.uniqueItems, schema.uniqueItems) &&
               Objects.equals(this.maxProperties, schema.maxProperties) &&
               Objects.equals(this.minProperties, schema.minProperties) &&
               Objects.equals(this.required, schema.required) &&
               Objects.equals(this.type, schema.type) &&
               Objects.equals(this.not, schema.not) &&
               Objects.equals(this.properties, schema.properties) &&
               Objects.equals(this.additionalProperties, schema.additionalProperties) &&
               Objects.equals(this.description, schema.description) &&
               Objects.equals(this.format, schema.format) &&
               Objects.equals(this.$ref, schema.$ref) &&
               Objects.equals(this.nullable, schema.nullable) &&
               Objects.equals(this.readOnly, schema.readOnly) &&
               Objects.equals(this.writeOnly, schema.writeOnly) &&
               Objects.equals(this.example, schema.example) &&
               Objects.equals(this.externalDocs, schema.externalDocs) &&
               Objects.equals(this.deprecated, schema.deprecated) &&
               Objects.equals(this.xml, schema.xml) &&
               Objects.equals(this.extensions, schema.extensions) &&
               Objects.equals(this._enum, schema._enum) &&
               Objects.equals(this.discriminator, schema.discriminator) &&
               Objects.equals(this._default, schema._default);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, title, multipleOf, maximum, exclusiveMaximum, minimum, exclusiveMinimum, maxLength, minLength, pattern, maxItems,
                            minItems, uniqueItems, maxProperties, minProperties, required, type, not, properties, additionalProperties, description, format, $ref,
                            nullable, readOnly, writeOnly, example, externalDocs, deprecated, xml, extensions, _enum, discriminator, _default);
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
    public Schema enumeration(List<Object> enumeration) {
        this._enum = enumeration;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Schema getItems() {
        return this.items;
    }

    /** {@inheritDoc} */
    @Override
    public void setItems(Schema items) {
        this.items = items;
    }

    /** {@inheritDoc} */
    @Override
    public Schema items(Schema items) {
        this.items = items;
        return this;
    }

    @Override
    public List<Schema> getAllOf() {
        return allOf;
    }

    @Override
    public void setAllOf(List<Schema> allOf) {
        this.allOf = allOf;
    }

    @Override
    public Schema allOf(List<Schema> allOf) {
        this.allOf = allOf;
        return this;
    }

    @Override
    public Schema addAllOf(Schema allOf) {
        if (this.allOf == null)
            this.allOf = new ArrayList<>();
        this.allOf.add(allOf);
        return this;
    }

    @Override
    public List<Schema> getAnyOf() {
        return this.anyOf;
    }

    @Override
    public void setAnyOf(List<Schema> anyOf) {
        this.anyOf = anyOf;

    }

    @Override
    public Schema anyOf(List<Schema> anyOf) {
        this.anyOf = anyOf;
        return this;
    }

    @Override
    public Schema addAnyOf(Schema anyOf) {
        if (this.anyOf == null)
            this.anyOf = new ArrayList<>();
        this.anyOf.add(anyOf);
        return this;
    }

    @Override
    public List<Schema> getOneOf() {
        return this.oneOf;
    }

    @Override
    public void setOneOf(List<Schema> oneOf) {
        this.oneOf = oneOf;

    }

    @Override
    public Schema oneOf(List<Schema> oneOf) {
        this.oneOf = oneOf;
        return this;
    }

    @Override
    public Schema addOneOf(Schema oneOf) {
        if (this.oneOf == null)
            this.oneOf = new ArrayList<>();
        this.oneOf.add(oneOf);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Schema {\n");

        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    multipleOf: ").append(toIndentedString(multipleOf)).append("\n");
        sb.append("    maximum: ").append(toIndentedString(maximum)).append("\n");
        sb.append("    exclusiveMaximum: ").append(toIndentedString(exclusiveMaximum)).append("\n");
        sb.append("    minimum: ").append(toIndentedString(minimum)).append("\n");
        sb.append("    exclusiveMinimum: ").append(toIndentedString(exclusiveMinimum)).append("\n");
        sb.append("    maxLength: ").append(toIndentedString(maxLength)).append("\n");
        sb.append("    minLength: ").append(toIndentedString(minLength)).append("\n");
        sb.append("    pattern: ").append(toIndentedString(pattern)).append("\n");
        sb.append("    maxItems: ").append(toIndentedString(maxItems)).append("\n");
        sb.append("    minItems: ").append(toIndentedString(minItems)).append("\n");
        sb.append("    uniqueItems: ").append(toIndentedString(uniqueItems)).append("\n");
        sb.append("    maxProperties: ").append(toIndentedString(maxProperties)).append("\n");
        sb.append("    minProperties: ").append(toIndentedString(minProperties)).append("\n");
        sb.append("    required: ").append(toIndentedString(required)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    not: ").append(toIndentedString(not)).append("\n");
        sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
        sb.append("    additionalProperties: ").append(toIndentedString(additionalProperties)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    format: ").append(toIndentedString(format)).append("\n");
        sb.append("    $ref: ").append(toIndentedString($ref)).append("\n");
        sb.append("    nullable: ").append(toIndentedString(nullable)).append("\n");
        sb.append("    readOnly: ").append(toIndentedString(readOnly)).append("\n");
        sb.append("    writeOnly: ").append(toIndentedString(writeOnly)).append("\n");
        sb.append("    example: ").append(toIndentedString(example)).append("\n");
        sb.append("    externalDocs: ").append(toIndentedString(externalDocs)).append("\n");
        sb.append("    deprecated: ").append(toIndentedString(deprecated)).append("\n");
        sb.append("    xml: ").append(toIndentedString(xml)).append("\n");
        sb.append("    discriminator: ").append(toIndentedString(discriminator)).append("\n");
        sb.append("    items: ").append(toIndentedString(items)).append("\n");
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
    public Schema defaultValue(Object defaultValue) {
        this._default = defaultValue;
        return this;
    }

    @JsonIgnore
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;

    }

    public Schema name(String name) {
        this.name = name;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void setAdditionalProperties(Boolean additionalProperties) {
        this.additionalProperties = additionalProperties;

    }

    /** {@inheritDoc} */
    @Override
    public Schema additionalProperties(Boolean additionalProperties) {
        this.additionalProperties = additionalProperties;
        return this;
    }

}