/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 * Copyright 2017 SmartBear Software
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.microprofile.openapi.models.media;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.Reference;

/**
 * The Schema Object allows the definition of input and output data types. These types can be objects, but also primitives and arrays. This object is
 * an extended subset of the <a href="https://tools.ietf.org/html/draft-wright-json-schema-00">JSON Schema Specification Wright Draft 00</a>.
 * <p>
 * For more information about the properties, see <a href="http://json-schema.org/">JSON Schema Core</a> and
 * <a href= "https://tools.ietf.org/html/draft-wright-json-schema-validation-00">JSON Schema Validation</a>. Unless stated otherwise, the property
 * definitions follow the JSON Schema.
 * <p>
 * Any time a Schema Object can be used, a Reference Object can be used in its place. This allows referencing an existing definition instead of
 * defining the same schema again.
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#schemaObject">OpenAPI Specification Schema Object</a>
 */
public interface Schema extends Extensible, Constructible, Reference<Schema> {

    
    /**
    * The values allowed for the in field.
    */
   enum SchemaType {
       INTEGER("integer"), NUMBER("number"), BOOLEAN("boolean"), 
       STRING("string"), OBJECT("object"), ARRAY("array");

       private final String value;

       SchemaType(String value) {
           this.value = value;
       }

       @Override
       public String toString() {
           return String.valueOf(value);
       }
   }

    /**
     * Returns the discriminator property from this schema instance.
     *
     * @return the discriminator that is used to differentiate between the schemas which may satisfy the payload description
     **/
    Discriminator getDiscriminator();

    /**
     * Sets the discriminator property of this schema instance to the given object.
     *
     * @param discriminator the object that is used to differentiate between the schemas which may satisfy the payload description
     */
    void setDiscriminator(Discriminator discriminator);

    /**
     * Sets the discriminator property of this schema instance to the given object.
     *
     * @param discriminator the object that is used to differentiate between the schemas which may satisfy the payload description
     * @return the current Schema instance
     */
    Schema discriminator(Discriminator discriminator);

    /**
     * Returns the title property from this schema instance.
     *
     * @return the title assigned to this schema
     **/
    String getTitle();

    /**
     * Sets the title property of this schema instance to the given string.
     *
     * @param title a title to assign to this schema
     */
    void setTitle(String title);

    /**
     * Sets the title property of this schema instance to the given string.
     *
     * @param title a title to assign to this schema
     * @return the current Schema instance
     */
    Schema title(String title);

    /**
     * Returns the default value property from this schema instance.
     *
     * @return the default value object
     **/
    Object getDefaultValue();

    /**
     * Set the default value property of this schema instance to the value given.
     *
     * @param defaultValue a value to use as the default
     */
    void setDefaultValue(Object defaultValue);

    /**
     * Set the default value property of this schema instance to the value given.
     *
     * @param defaultValue a value to use as the default
     * @return the current Schema instance
     */
    Schema defaultValue(Object defaultValue);

    /**
     * Returns the enumerated list of values allowed for objects defined by this schema.
     *
     * @return the list of values allowed for objects defined by this schema
     */
    List<Object> getEnumeration();

    /**
     * Sets the enumerated list of values allowed for objects defined by this schema.
     *
     * @param enumeration a list of values allowed
     */
    void setEnumeration(List<Object> enumeration);

    Schema enumeration(List<Object> enumeration);

    /**
     * Adds an item of the appropriate type to the enumerated list of values allowed.
     *
     * @param enumeration an object to add to the enumerated values
     * @return current schema instance
     */
    Schema addEnumeration(Object enumeration);

    /**
     * Returns the multipleOf property from this schema instance.
     * <p>
     * minimum: 0
     *
     * @return the positive number that restricts the value of the object
     **/
    BigDecimal getMultipleOf();

    /**
     * Sets the multipleOf property of this schema instance to the value given.
     *
     * @param multipleOf a positive number that restricts the value of objects described by this schema
     */
    void setMultipleOf(BigDecimal multipleOf);

    /**
     * Sets the multipleOf property of this schema instance to the value given.
     *
     * @param multipleOf a positive number that restricts the value of objects described by this schema
     * @return the current Schema instance
     */
    Schema multipleOf(BigDecimal multipleOf);

    /**
     * Returns the maximum property from this schema instance.
     *
     * @return the maximum value of a numeric object
     **/
    BigDecimal getMaximum();

    /**
     * Sets the maximum property of this schema instance to the value given.
     *
     * @param maximum specifies the maximum numeric value of objects defined by this schema
     */
    void setMaximum(BigDecimal maximum);

    /**
     * Sets the maximum property of this schema instance to the value given.
     *
     * @param maximum specifies the maximum numeric value of objects defined by this schema
     * @return the current Schema instance
     */
    Schema maximum(BigDecimal maximum);

    /**
     * Returns the exclusiveMaximum property from this schema instance.
     *
     * @return whether the numeric value of objects must be less than the maximum property
     **/
    Boolean getExclusiveMaximum();

    /**
     * Sets the exclusiveMaximum property of this schema instance to the value given.
     *
     * @param exclusiveMaximum when true the numeric value of objects defined by this schema must be less than indicated by the maximum property
     */
    void setExclusiveMaximum(Boolean exclusiveMaximum);

    /**
     * Sets the exclusiveMaximum property of this schema instance to the value given.
     *
     * @param exclusiveMaximum when true the numeric value of objects defined by this schema must be less than indicated by the maximum property
     * @return the current Schema instance
     */
    Schema exclusiveMaximum(Boolean exclusiveMaximum);

    /**
     * Returns the minimum property from this schema instance.
     *
     * @return the minimum value of a numeric object
     **/
    BigDecimal getMinimum();

    /**
     * Sets the minimum property of this schema instance to the value given.
     *
     * @param minimum specifies the minimum numeric value of objects defined by this schema
     */
    void setMinimum(BigDecimal minimum);

    /**
     * Sets the minimum property of this schema instance to the value given.
     *
     * @param minimum specifies the minimum numeric value of objects defined by this schema
     * @return the current Schema instance
     */
    Schema minimum(BigDecimal minimum);

    /**
     * Returns the exclusiveMinimum property from this schema instance.
     *
     * @return whether the numeric value of objects must be greater than the minimum property
     **/
    Boolean getExclusiveMinimum();

    /**
     * Sets the exclusiveMinimum property of this schema instance to the value given.
     *
     * @param exclusiveMinimum when true the numeric value of objects defined by this schema must be greater than indicated by the minimum property
     */
    void setExclusiveMinimum(Boolean exclusiveMinimum);

    /**
     * Sets the exclusiveMinimum property of this schema instance to the value given.
     *
     * @param exclusiveMinimum when true the numeric value of objects defined by this schema must be greater than indicated by the minimum property
     * @return the current Schema instance
     */
    Schema exclusiveMinimum(Boolean exclusiveMinimum);

    /**
     * Returns the maxLength property from this schema instance.
     * <p>
     * minimum: 0
     *
     * @return the maximum length of objects e.g. strings
     **/
    Integer getMaxLength();

    /**
     * Sets the maxLength property of this schema instance to the value given.
     *
     * @param maxLength the maximum length of objects defined by this schema
     */
    void setMaxLength(Integer maxLength);

    /**
     * Sets the maxLength property of this schema instance to the value given.
     *
     * @param maxLength the maximum length of objects defined by this schema
     * @return the current Schema instance
     */
    Schema maxLength(Integer maxLength);

    /**
     * Returns the minLength property from this schema instance.
     * <p>
     * minimum: 0
     *
     * @return the minimum length of objects e.g. strings
     **/
    Integer getMinLength();

    /**
     * Sets the minLength property of this schema instance to the value given.
     *
     * @param minLength the minimum length of objects defined by this schema
     */
    void setMinLength(Integer minLength);

    /**
     * Sets the minLength property of this schema instance to the value given.
     *
     * @param minLength the minimum length of objects defined by this schema
     * @return the current Schema instance
     */
    Schema minLength(Integer minLength);

    /**
     * Returns the pattern property from this schema instance.
     *
     * @return the regular expression which restricts the value of an object e.g. a string
     **/
    String getPattern();

    /**
     * Sets the pattern property of this schema instance to the string given.
     *
     * @param pattern the regular expression which restricts objects defined by this schema
     */
    void setPattern(String pattern);

    /**
     * Sets the pattern property of this schema instance to the string given.
     *
     * @param pattern the regular expression which restricts objects defined by this schema
     * @return the current Schema instance
     */
    Schema pattern(String pattern);

    /**
     * Returns the maxItems property from this schema instance.
     * <p>
     * minimum: 0
     *
     * @return the maximum number of elements in the object e.g. array elements
     **/
    Integer getMaxItems();

    /**
     * Sets the maxItems property of this schema instance to the value given.
     *
     * @param maxItems the maximum number of elements in objects defined by this schema e.g. array elements
     */
    void setMaxItems(Integer maxItems);

    /**
     * Sets the maxItems property of this schema instance to the value given.
     *
     * @param maxItems the maximum number of elements in objects defined by this schema e.g. array elements
     * @return the current Schema instance
     */
    Schema maxItems(Integer maxItems);

    /**
     * Returns the minItems property from this schema instance.
     * <p>
     * minimum: 0
     *
     * @return the minimum number of elements in the object e.g. array elements
     **/
    Integer getMinItems();

    /**
     * Sets the minItems property of this schema instance to the value given.
     *
     * @param minItems the minimum number of elements in objects defined by this schema e.g. array elements
     */
    void setMinItems(Integer minItems);

    /**
     * Sets the minItems property of this schema instance to the value given.
     *
     * @param minItems the minimum number of elements in objects defined by this schema e.g. array elements
     * @return the current Schema instance
     */
    Schema minItems(Integer minItems);

    /**
     * Returns the uniqueItems property from this schema instance.
     *
     * @return whether to ensure items are unique
     **/
    Boolean getUniqueItems();

    /**
     * Sets the uniqueItems property of this schema instance to the value given.
     *
     * @param uniqueItems ensure the items (e.g. array elements) are unique in objects defined by this schema
     */
    void setUniqueItems(Boolean uniqueItems);

    /**
     * Sets the uniqueItems property of this schema instance to the value given.
     *
     * @param uniqueItems ensure the items (e.g. array elements) are unique in objects defined by this schema
     * @return the current Schema instance
     */
    Schema uniqueItems(Boolean uniqueItems);

    /**
     * Returns the maxProperties property from this schema instance.
     * <p>
     * minimum: 0
     *
     * @return the maximum number of properties allowed in the object
     **/
    Integer getMaxProperties();

    /**
     * Sets the maxProperties property of this schema instance to the value given.
     *
     * @param maxProperties limit the number of properties in objects defined by this schema
     */
    void setMaxProperties(Integer maxProperties);

    /**
     * Sets the maxProperties property of this schema instance to the value given.
     *
     * @param maxProperties limit the number of properties in objects defined by this schema
     * @return the current Schema instance
     */
    Schema maxProperties(Integer maxProperties);

    /**
     * Returns the minProperties property from this schema instance.
     * <p>
     * minimum: 0
     *
     * @return the minimum number of properties allowed in the object
     **/
    Integer getMinProperties();

    /**
     * Sets the minProperties property of this schema instance to the value given.
     *
     * @param minProperties limit the number of properties in objects defined by this schema
     */
    void setMinProperties(Integer minProperties);

    /**
     * Sets the minProperties property of this schema instance to the value given.
     *
     * @param minProperties limit the number of properties in objects defined by this schema
     * @return the current Schema instance
     */
    Schema minProperties(Integer minProperties);

    /**
     * Returns the required property from this schema instance.
     *
     * @return the list of fields required in objects defined by this schema
     **/
    List<String> getRequired();

    /**
     * Sets the list of fields required in objects defined by this schema.
     *
     * @param required the list of fields required in objects defined by this schema
     */
    void setRequired(List<String> required);

    /**
     * Sets the list of fields required in objects defined by this schema.
     *
     * @param required the list of fields required in objects defined by this schema
     * @return the current Schema instance
     */
    Schema required(List<String> required);

    /**
     * Adds the name of an item to the list of fields required in objects defined by this schema.
     *
     * @param required the name of an item required in objects defined by this schema instance
     * @return the current Schema instance
     */
    Schema addRequired(String required);

    /**
     * Returns the type property from this schema.
     *
     * @return the type used in this schema. Default value <b>must</b> be <code>null</code>
     **/
    SchemaType getType();

    /**
     * Sets the type used by this schema to the string given.
     *
     * @param type the type used by this schema or <code>null</code> for
     * reference schemas
     */
    void setType(SchemaType type);

    /**
     * Sets the type used by this schema to the string given.
     *
     * @param type the type used by this schema or <code>null</code> for
     * reference schemas
     * @return the current Schema instance
     */
    Schema type(SchemaType type);

    /**
     * Returns a schema which describes properties not allowed in objects defined by the current schema.
     *
     * @return the not property's schema
     **/
    Schema getNot();

    /**
     * Sets the not property to a schema which describes properties not allowed in objects defined by the current schema.
     *
     * @param not the schema which describes properties not allowed
     */
    void setNot(Schema not);

    /**
     * Sets the not property to a schema which describes properties not allowed in objects defined by the current schema.
     *
     * @param not the schema which describes properties not allowed
     * @return the current Schema instance
     */
    Schema not(Schema not);

    /**
     * Returns the properties defined in this schema.
     *
     * @return a map which associates property names with the schemas that describe their contents
     **/
    Map<String, Schema> getProperties();

    /**
     * Sets the properties of this schema instance to the map provided.
     *
     * @param properties a map which associates property names with the schemas that describe their contents
     */
    void setProperties(Map<String, Schema> properties);

    /**
     * Sets the properties of this schema instance to the map provided.
     *
     * @param properties a map which associates property names with the schemas that describe their contents
     * @return the current Schema instance
     */
    Schema properties(Map<String, Schema> properties);

    /**
     * Adds a schema property of the provided name using the given schema.
     *
     * @param key the name of a new schema property
     * @param propertySchema the schema which describes the properties of the named property
     * @return the current Schema instance
     */
    Schema addProperty(String key, Schema propertySchema);

    /**
     * Returns the value of the "additionalProperties" setting, which indicates whether 
     * properties not otherwise defined are allowed.  This setting MUST either be a {@link Boolean}
     * or {@link Schema}.
     * 
     * <ul>
     *   <li>If "additionalProperties" is true, then any additional properties are allowed.</li>
     *
     *   <li>If "additionalProperties" is false, then only properties covered by the "properties"
     *   and "patternProperties" are allowed.</li>
     *
     *   <li>If "additionalProperties" is a Schema, then additional properties are allowed but
     *   should conform to the Schema.</li>
     * </ul>
     *
     * @return this schema's additionalProperties property
     */
    Object getAdditionalProperties();

    /**
     * Sets the schema which defines additional properties not defined by "properties" or "patternProperties".
     * See the javadoc for {@link Schema#getAdditionalProperties()} for more details on this setting.  Note 
     * that this version of the setter is mutually exclusive with the Boolean variants.
     *
     * @param additionalProperties a schema which defines additional properties
     */
    void setAdditionalProperties(Schema additionalProperties);
    
    /**
     * Sets the value of "additionalProperties" to either True or False.  See the javadoc for 
     * {@link Schema#getAdditionalProperties()} for more details on this setting.  Note that
     * this version of the setter is mutually exclusive with the {@link Schema} variants.
     *
     * @param additionalProperties a schema which defines additional properties
     */
    void setAdditionalProperties(Boolean additionalProperties);

    /**
     * Sets the schema which defines additional properties not defined by "properties" or "patternProperties".
     * See the javadoc for {@link Schema#getAdditionalProperties()} for more details on this setting.  Note 
     * that this version of the setter is mutually exclusive with the Boolean variants.
     *
     * @param additionalProperties a schema which defines additional properties
     * @return the current Schema instance
     */
    Schema additionalProperties(Schema additionalProperties);

    /**
     * Sets the value of "additionalProperties" to either True or False.  See the javadoc for 
     * {@link Schema#getAdditionalProperties()} for more details on this setting.  Note that
     * this version of the setter is mutually exclusive with the {@link Schema} variants.
     *
     * @param additionalProperties a schema which defines additional properties
     * @return the current Schema instance
     */
    Schema additionalProperties(Boolean additionalProperties);

    /**
     * Returns a description of the purpose of this schema.
     *
     * @return a string containing a description
     **/
    String getDescription();

    /**
     * Sets the description property of this schema to the given string.
     *
     * @param description a string containing a description of the purpose of this schema
     */
    void setDescription(String description);

    /**
     * Sets the description property of this schema to the given string.
     *
     * @param description a string containing a description of the purpose of this schema
     * @return the current Schema instance
     */
    Schema description(String description);

    /**
     * Returns the format property from this schema instance. This property clarifies the data type specified in the type property.
     *
     * @return a string describing the format of the data in this schema
     **/
    String getFormat();

    /**
     * Sets the format property of this schema instance to the given string. The value may be one of the formats described in the OAS or a user
     * defined format.
     *
     * @param format the string specifying the data format
     */
    void setFormat(String format);

    /**
     * Sets the format property of this schema instance to the given string. The value may be one of the formats described in the OAS or a user
     * defined format.
     *
     * @param format the string specifying the data format
     * @return the current Schema instance
     */
    Schema format(String format);

    /**
     * Returns the nullable property from this schema instance which indicates whether null is a valid value.
     *
     * @return the nullable property
     **/
    Boolean getNullable();

    /**
     * Sets the nullable property of this schema instance. Specify true if this schema will allow null values.
     *
     * @param nullable a boolean value indicating this schema allows a null value.
     */
    void setNullable(Boolean nullable);

    /**
     * Sets the nullable property of this schema instance. Specify true if this schema will allow null values.
     *
     * @param nullable a boolean value indicating this schema allows a null value.
     * @return the current Schema instance
     */
    Schema nullable(Boolean nullable);

    /**
     * Returns the readOnly property from this schema instance.
     *
     * @return indication that the schema is only valid in a response message
     **/
    Boolean getReadOnly();

    /**
     * Sets the readOnly property of this schema. Only valid when the schema is the property in an object.
     *
     * @param readOnly true indicates the schema should not be sent as part of a request message
     */
    void setReadOnly(Boolean readOnly);

    /**
     * Sets the readOnly property of this schema. Only valid when the schema is the property in an object.
     *
     * @param readOnly true indicates the schema should not be sent as part of a request message
     * @return the current Schema instance
     */
    Schema readOnly(Boolean readOnly);

    /**
     * Returns the writeOnly property from this schema instance.
     *
     * @return indication that the schema is only valid in a request message
     **/
    Boolean getWriteOnly();

    /**
     * Sets the writeOnly property of this schema. Only valid when the schema is the property in an object.
     *
     * @param writeOnly true indicates the schema should not be sent as part of a response message
     */
    void setWriteOnly(Boolean writeOnly);

    /**
     * Sets the writeOnly property of this schema. Only valid when the schema is the property in an object.
     *
     * @param writeOnly true indicates the schema should not be sent as part of a response message
     * @return the current Schema instance
     */
    Schema writeOnly(Boolean writeOnly);

    /**
     * Returns the example property from this schema instance.
     *
     * @return an object which is an example of an instance of this schema
     **/
    Object getExample();

    /**
     * Sets the example property of this schema instance. To represent examples that cannot be naturally represented in JSON or YAML, a string value
     * can be used to contain the example with escaping where necessary.
     *
     * @param example an object which is an instance of this schema
     */
    void setExample(Object example);

    /**
     * Sets the example property of this schema instance. To represent examples that cannot be naturally represented in JSON or YAML, a string value
     * can be used to contain the example with escaping where necessary.
     *
     * @param example an object which is an instance of this schema
     * @return the current Schema instance
     */
    Schema example(Object example);

    /**
     * Returns the externalDocs property from this schema instance.
     *
     * @return additional external documentation for this schema
     **/
    ExternalDocumentation getExternalDocs();

    /**
     * Sets the externalDocs property of this schema to the indicated value.
     *
     * @param externalDocs an additional external documentation object
     */
    void setExternalDocs(ExternalDocumentation externalDocs);

    /**
     * Sets the externalDocs property of this schema to the indicated value.
     *
     * @param externalDocs an additional external documentation object
     * @return the current Schema instance
     */
    Schema externalDocs(ExternalDocumentation externalDocs);

    /**
     * Returns the deprecated property from this schema instance.
     *
     * @return indication that the schema is deprecated and should be transitioned out of usage
     **/
    Boolean getDeprecated();

    /**
     * Sets the deprecated property of this schema. This specifies that the schema is deprecated and should be transitioned out of usage
     *
     * @param deprecated true to indicate this schema is deprecated
     */
    void setDeprecated(Boolean deprecated);

    /**
     * Sets the deprecated property of this schema. This specifies that the schema is deprecated and should be transitioned out of usage
     *
     * @param deprecated true to indicate this schema is deprecated
     * @return the current Schema instance
     */
    Schema deprecated(Boolean deprecated);

    /**
     * Returns the xml property from this schema instance.
     *
     * @return a metadata object that allows for more fine-tuned XML model definitions
     **/
    XML getXml();

    /**
     * Sets the xml property of this schema instance. It may only be set on properties schemas and adds additional metadata to describe the XML
     * representation of this property.
     *
     * @param xml a metadata object to describe the XML representation of this property
     */
    void setXml(XML xml);

    /**
     * Sets the xml property of this schema instance. It may only be set on properties schemas and adds additional metadata to describe the XML
     * representation of this property.
     *
     * @param xml a metadata object to describe the XML representation of this property
     * @return the current Schema instance
     */
    Schema xml(XML xml);
    
    
    /**
     * Returns the schema used for all the elements of an ArraySchema instance (array).
     *
     * @return the schema used for all the elements
     **/
    Schema getItems();

    /**
     * Set the schema used for all the elements of an ArraySchema instance (array).
     *
     * @param items the schema used by this array
     */
    void setItems(Schema items);

    /**
     * Set the schema used for all the elements of an ArraySchema instance (array).
     *
     * @param items the schema used by this array
     * @return the current ArraySchema instance
     */
    Schema items(Schema items);
    
    /**
     * Returns the schemas used by the allOf property in a ComposedSchema instance.
     *
     * @return the list of schemas used by the allOf property
     **/
    List<Schema> getAllOf();

    /**
     * Sets the schemas used by the allOf property of this schema.
     * 
     * @param allOf the list of schemas used by the allOf property
     */
    void setAllOf(List<Schema> allOf);

    /**
     * Sets the schemas used by the allOf property of this schema.
     * 
     * @param allOf the list of schemas used by the allOf property
     * @return the current ComposedSchema instance
     */
    Schema allOf(List<Schema> allOf);

    /**
     * Adds the given schema to this ComposedSchema's list of schemas used by the allOf property.
     * 
     * @param allOf a schema to use with the allOf property
     * @return the current ComposedSchema instance
     */
    Schema addAllOf(Schema allOf);

    /**
     * Returns the schemas used by the anyOf property in a ComposedSchema instance.
     *
     * @return the list of schemas used by the anyOf property
     **/
    List<Schema> getAnyOf();

    /**
     * Sets the schemas used by the anyOf property of this schema.
     * 
     * @param anyOf the list of schemas used by the anyOf property
     */
    void setAnyOf(List<Schema> anyOf);

    /**
     * Sets the schemas used by the anyOf property of this schema.
     * 
     * @param anyOf the list of schemas used by the anyOf property
     * @return the current ComposedSchema instance
     */
    Schema anyOf(List<Schema> anyOf);

    /**
     * Adds the given schema to this ComposedSchema's list of schemas used by the anyOf property.
     * 
     * @param anyOf a schema to use with the anyOf property
     * @return the current ComposedSchema instance
     */
    Schema addAnyOf(Schema anyOf);

    /**
     * Returns the schemas used by the oneOf property in a ComposedSchema instance.
     *
     * @return the list of schemas used by the oneOf property
     **/
    List<Schema> getOneOf();

    /**
     * Sets the schemas used by the oneOf property of this schema.
     * 
     * @param oneOf the list of schemas used by the oneOf property
     */
    void setOneOf(List<Schema> oneOf);

    /**
     * Sets the schemas used by the oneOf property of this schema.
     * 
     * @param oneOf the list of schemas used by the oneOf property
     * @return the current ComposedSchema instance
     */
    Schema oneOf(List<Schema> oneOf);

    /**
     * Adds the given schema to this ComposedSchema's list of schemas used by the oneOf property.
     * 
     * @param oneOf a schema to use with the oneOf property
     * @return the current ComposedSchema instance
     */
    Schema addOneOf(Schema oneOf);

}