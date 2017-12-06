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

package org.eclipse.microprofile.openapi.annotations.media;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;

/**
 * The Schema Object allows the definition of input and output data types. 
 * These types can be objects, but also primitives and arrays. 
 * This object is an extended subset of the JSON Schema Specification Wright Draft 00.
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#schemaObject">OpenAPI Specification Schema Object</a>
 **/
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Schema {
    /**
     * Provides a java class as implementation for this schema. 
     * When provided, additional information in the Schema annotation (except for type
     * information) will augment the java class after introspection.
     * 
     * @return a class that implements this schema
     **/
    Class<?> implementation() default Void.class;

    /**
     * Provides a java class to be used to disallow matching properties.
     * Inline or referenced schema MUST be of a Schema Object and not a standard JSON Schema.
     * 
     * @return a class with disallowed properties
     **/
    Class<?> not() default Void.class;

    /**
     * Provides an array of java class implementations which can be used to describe multiple acceptable schemas. 
     * If more than one match the derived schemas, a validation error will occur.
     * <p>
     * Inline or referenced schema MUST be of a Schema Object and not a standard JSON Schema.
     * </p>
     * @return the list of possible classes for a single match
     **/
    Class<?>[] oneOf() default {};

    /**
     * Provides an array of java class implementations which can be used to describe multiple acceptable schemas. 
     * If any match, the schema will be considered valid.
     * <p>
     * Inline or referenced schema MUST be of a Schema Object and not a standard JSON Schema.
     * </p>
     * @return the list of possible class matches
     **/
    Class<?>[] anyOf() default {};

    /**
     * Provides an array of java class implementations which can be used to describe multiple acceptable schemas. 
     * If all match, the schema will be considered valid.
     * <p>
     * Inline or referenced schema MUST be of a Schema Object and not a standard JSON Schema.
     * </p>
     * @return the list of classes to match
     **/
    Class<?>[] allOf() default {};

    /**
     * The name of the schema or property.
     * <p>
     * The name is REQUIRED when the schema is defined within {@link org.eclipse.microprofile.openapi.annotations.Components}. The 
     * name will be used as the key to add this schema to the 'schemas' map for reuse.
     * </p>
     * 
     * @return the name of the schema
     **/
    String name() default "";

    /**
     * A title to explain the purpose of the schema.
     * 
     * @return the title of the schema
     **/
    String title() default "";

    /**
     * Constrains a value such that when divided by the multipleOf, the remainder must be an integer. 
     * Ignored if the value is 0.
     * 
     * @return the multiplier constraint of the schema
     **/
    double multipleOf() default 0;

    /**
     * Sets the maximum numeric value for a property. 
     * Ignored if the value is an empty string.
     * 
     * @return the maximum value for this schema
     **/
    String maximum() default "";

    /**
     * If true, makes the maximum value exclusive, or a less-than criteria.
     * 
     * @return the exclusive maximum value for this schema
     **/
    boolean exclusiveMaximum() default false;

    /**
     * Sets the minimum numeric value for a property. 
     * Ignored if the value is an empty string or not a number.
     * 
     * @return the minimum value for this schema
     **/
    String minimum() default "";

    /**
     * If true, makes the minimum value exclusive, or a greater-than criteria.
     * 
     * @return the exclusive minimum value for this schema
     **/
    boolean exclusiveMinimum() default false;

    /**
     * Sets the maximum length of a string value. 
     * Ignored if the value is negative.
     * 
     * @return the maximum length of this schema
     **/
    int maxLength() default Integer.MAX_VALUE;

    /**
     * Sets the minimum length of a string value. 
     * Ignored if the value is negative.
     * 
     * @return the minimum length of this schema
     **/
    int minLength() default 0;

    /**
     * A pattern that the value must satisfy. 
     * Ignored if the value is an empty string.
     * 
     * @return the pattern of this schema
     **/
    String pattern() default "";

    /**
     * Constrains the number of arbitrary properties when additionalProperties is defined. 
     * Ignored if value is 0.
     * 
     * @return the maximum number of properties for this schema
     **/
    int maxProperties() default 0;

    /**
     * Constrains the number of arbitrary properties when additionalProperties is defined. 
     * Ignored if value is 0.
     * 
     * @return the minimum number of properties for this schema
     **/
    int minProperties() default 0;

    /**
     * Allows multiple properties in an object to be marked as required.
     * 
     * @return the list of required schema properties
     **/
    String[] requiredProperties() default {};

    /**
     * Mandates whether the annotated item is required or not.
     * 
     * @return whether or not this schema is required
     **/
    boolean required() default false;

    /**
     * A description of the schema.
     * 
     * @return this schema's description
     **/
    String description() default "";

    /**
     * Provides an optional override for the format.
     * <p> 
     * If a consumer is unaware of the meaning of the format, they shall fall back to using the basic type without format. 
     * For example, if \&quot;type: integer, format: int128\&quot; were used to designate a very large integer, most consumers
     * will not understand how to handle it, and fall back to simply \&quot;type: integer\&quot;
     * </p>
     * @return this schema's format
     **/
    String format() default "";

    /**
     * Reference value to a Schema definition.
     * <p>
     * This property provides a reference to an object defined elsewhere. This property and
     * all other properties are mutually exclusive. If other properties are defined in addition
     * to the ref property then the result is undefined.
     * 
     * @return a reference to a schema definition
     **/
    String ref() default "";

    /**
     * Allows sending a null value for the defined schema.
     * 
     * @return whether or not this schema is nullable
     **/
    boolean nullable() default false;

    /**
     * Relevant only for Schema "properties" definitions. 
     * Declares the property as "read only". This means that it MAY be sent as part of a response but SHOULD NOT be sent as part of the request. 
     * <p>
     * If the property is marked as readOnly being true and is in the required list, the required will take effect on the response only. 
     * A property MUST NOT be marked as both readOnly and writeOnly being true. 
     * </p> 
     * @return whether or not this schema is read only
     **/
    boolean readOnly() default false;

    /**
     * Relevant only for Schema "properties" definitions. 
     * Declares the property as "write only". Therefore, it MAY be sent as part of a request but SHOULD NOT be sent as part of the response.
     * <p>
     * If the property is marked as writeOnly being true and is in the required list, the required will take effect on the request only. 
     * A property MUST NOT be marked as both readOnly and writeOnly being true.
     * </p>
     * @return whether or not this schema is write only
     **/
    boolean writeOnly() default false;

    /**
     * A free-form property to include an example of an instance for this schema.
     * <p>
     * To represent examples that cannot be naturally represented in JSON or YAML, 
     * a string value is used to contain the example with escaping where necessary.
     * </p>
     * When associated with a specific media type, the example string shall be parsed 
     * by the consumer to be treated as an object or an array.
     * @return an example of this schema
     **/
    String example() default "";

    /**
     * Additional external documentation for this schema.
     * 
     * @return additional schema documentation
     **/
    ExternalDocumentation externalDocs() default @ExternalDocumentation();

    /**
     * Specifies that a schema is deprecated and SHOULD be transitioned out of usage.
     * 
     * @return whether or not this schema is deprecated
     **/
    boolean deprecated() default false;

    /**
     * Provides an override for the basic type of the schema.
     * <p>
     * Value MUST be a string. Multiple types via an array are not supported.
     * </p> 
     * MUST be a valid type per the OpenAPI Specification.
     * 
     * @return the type of this schema
     **/
    SchemaType type() default SchemaType.DEFAULT;;

    /**
     * Provides a list of enum values. 
     * Corresponds to the enum property in the OAS schema and the enumeration property in the schema model.
     * 
     * @return a list of allowed schema values
     */
    String[] enumeration() default {};

    /**
     * Provides a default value.
     * The default value represents what would be assumed by the consumer of the input as the value of the schema 
     * if one is not provided. 
     * <p>
     * Unlike JSON Schema, the value MUST conform to the defined type for the Schema Object defined at the same level. 
     * </p>
     * For example, if type is string, then default can be "foo" but cannot be 1.
     * 
     * @return the default value of this schema
     */
    String defaultValue() default "";

    /**
     * Provides a discriminator property value.
     * Adds support for polymorphism. 
     * <p>
     * The discriminator is an object name that is used to differentiate between other schemas 
     * which may satisfy the payload description.
     * </p>
     * @return the discriminator property
     */
    String discriminatorProperty() default "";

    /**
     * An array of discriminator mappings.
     * 
     * @return the discriminator mappings for this schema
     */
    DiscriminatorMapping[] discriminatorMapping() default {};

    /**
     * Allows schema to be marked as hidden.
     * 
     * @return whether or not this schema is hidden
     */
    boolean hidden() default false;
    
    /**
     * Only applicable if type=array.  Sets the maximum number of items in an array.
     * This integer MUST be greater than, or equal to, 0.
     * <p>
     * An array instance is valid against "maxItems" if its size is less than, or equal to, the value of this keyword.
     * </p> 
     * Ignored if value is Integer.MIN_VALUE.
     * 
     * @return the maximum number of items in this array
     **/
    int maxItems() default Integer.MIN_VALUE;

    /**
     * Only applicable if type=array.  Sets the minimum number of items in an array.
     * This integer MUST be greater than, or equal to, 0. 
     * <p>
     * An array instance is valid against "minItems" if its size is greater than, or equal to, the value of this keyword.
     * </p>
     * Ignored if value is Integer.MAX_VALUE.
     * 
     * @return the minimum number of items in this array
     **/
    int minItems() default Integer.MAX_VALUE;

    /**
     * Only applicable if type=array.  Determines if the items in the array SHOULD be unique.
     * <p>
     * If false, the instance validates successfully.
     * If true, the instance validates successfully if all of its elements are unique.
     * </p>
     * @return whether the items in this array are unique
     **/
    boolean uniqueItems() default false;
}
