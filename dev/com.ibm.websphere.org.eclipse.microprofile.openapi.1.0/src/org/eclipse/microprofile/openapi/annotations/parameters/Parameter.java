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

package org.eclipse.microprofile.openapi.annotations.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterStyle;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Describes a single operation parameter
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#parameterObject">OpenAPI Specification Parameter
 *      Object</a>
 **/
@Target({ ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Parameters.class)
@Inherited
public @interface Parameter {
    /**
     * The name of the parameter. Parameter names are case sensitive.
     * It is a REQUIRED property unless this is only a reference to a parameter instance.
     * <p>
     * When the parameter is defined within {@link org.eclipse.microprofile.openapi.annotations.Components}, 
     * the name will be used as the key to add this parameter to the 'parameters' map for reuse.
     * <p>
     * If in is "path", the name field MUST correspond to the associated path segment from the path field in the Paths Object. 
     * See Path Templating for further information.
     * </p>
     * If in is "header" and the name field is "Accept", "Content-Type" or "Authorization", the parameter definition SHALL be ignored.
     * <p>
     * For all other cases, the name corresponds to the parameter name used by the in property.
     * </p>
     * @return this parameter's name
     **/
    String name() default "";

    /**
     * The location of the parameter. It is a REQUIRED property unless this is only a reference to a parameter instance.
     * <p>
     * Possible values are specified in ParameterIn enum. Ignored when empty string.
     * </p>
     * @return this parameter's location
     **/
    ParameterIn in() default ParameterIn.DEFAULT;

    /**
     * A brief description of the parameter. This could contain examples of use. 
     * CommonMark syntax MAY be used for rich text representation.
     * 
     * @return this parameter's description
     **/
    String description() default "";

    /**
     * Determines whether this parameter is mandatory. 
     * <p>
     * If the parameter location is "path", this property is REQUIRED and its value MUST be true.
     * Otherwise, the property may be included and its default value is false.
     * </p>
     * @return whether or not this parameter is required
     **/
    boolean required() default false;

    /**
     * Specifies that a parameter is deprecated and SHOULD be transitioned out of usage.
     * 
     * @return whether or not this parameter is deprecated
     **/
    boolean deprecated() default false;

    /**
     * When true, allows sending an empty value. If false, the parameter will be considered \&quot;null\&quot; if no value is present. 
     * <p>
     * This may create validation errors when the parameter is required. 
     * Valid only for query parameters and allows sending a parameter with an empty value. 
     * </p>
     * If style is used, and if behavior is n/a (cannot be serialized), the value of allowEmptyValue SHALL be ignored.
     * 
     * @return whether or not this parameter allows empty values
     **/
    boolean allowEmptyValue() default false;

    /**
     * Describes how the parameter value will be serialized depending on the type of the parameter value.
     * <p> 
     * Default values (based on value of in): 
     * for query - form; for path - simple; for header - simple; for cookie - form. 
     * </p>
     * Ignored if the properties content or array are specified.
     * 
     * @return the style of this parameter
     **/
    ParameterStyle style() default ParameterStyle.DEFAULT;

    /**
     * When this is true, parameter values of type array or object generate separate parameters 
     * for each value of the array or key-value pair of the map. 
     * <p>
     * For other types of parameters this property has no effect. 
     * When style is form, the default value is true. For all other styles, the default value is false.
     * </p> 
     * Ignored if the properties content or array are specified.
     * 
     * @return whether or not to expand individual array members
     **/
    Explode explode() default Explode.DEFAULT;

    /**
     * Determines whether the parameter value SHOULD allow reserved characters, as defined by RFC3986. 
     * <p>
     * This property only applies to parameters with an in value of query.  
     * Ignored if the properties content or array are specified.
     * </p>
     * @return whether or not this parameter allows reserved characters
     **/
    boolean allowReserved() default false;

    /**
     * The schema defining the type used for the parameter. 
     * Ignored if the properties content or array are specified.
     * 
     * @return the schema of this parameter
     **/
    Schema schema() default @Schema();

    /**
     * The representation of this parameter, for different media types.
     * 
     * @return the content of this parameter
     **/
    Content[] content() default {};

    /**
     * Allows this parameter to be marked as hidden
     * 
     * @return whether or not this parameter is hidden
     */
    boolean hidden() default false;

    /**
     * Provides an array examples of the schema.
     * Each example SHOULD contain a value in the correct format as specified in the parameter encoding.
     * Furthermore, if referencing a schema which contains an example, the examples value SHALL override the example provided by the schema. 
     * <p>
     * When associated with a specific media type, the example string shall be parsed by the consumer to be
     * treated as an object or an array.
     * </p> 
     * Ignored if the properties content or array are specified.
     * 
     * @return the list of examples for this parameter
     **/
    ExampleObject[] examples() default {};

    /**
     * Provides an example of the schema. 
     * The example SHOULD match the specified schema and encoding properties if present. 
     * Furthermore, if referencing a schema which contains an example, the example value SHALL override the example provided by the schema. 
     * To represent examples of media types that cannot naturally be represented in JSON or YAML, 
     * a string value can contain the example with escaping where necessary.
     * <p>
     * When associated with a specific media type, the example string SHALL be parsed by the consumer to be treated
     * as an object or an array. 
     * </p>
     * Ignored if the properties examples, content or array are specified.
     * 
     * @return an example of the parameter
     **/
    String example() default "";

    /**
     * Reference value to a Parameter object.
     * <p>
     * This property provides a reference to an object defined elsewhere. This property and
     * all other properties are mutually exclusive. If other properties are defined in addition
     * to the ref property then the result is undefined.
     *
     * @return reference to a parameter
     **/
    String ref() default "";
}
