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

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.microprofile.openapi.annotations.headers.Header;

/**
 * Single encoding definition to be applied to single Schema Object
 * 
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#encodingObject">Encoding Object</a>
 **/

@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Encoding {

    /**
     * The name of this encoding object instance. 
     * This property is a key in an encoding map of a MediaType object and MUST exist in a schema as a property.
     * 
     * @return the name of this encoding instance
     **/
    String name() default "";

    /**
     * The Content-Type for encoding a specific property. Default value depends on the property type.
     * <p>
     * For example, for binary string - contentType is application/octet-stream, for primitive types - text/plain, for object - application/json.
     * The value can be a specific media type (e.g. application/json), a wildcard media type (e.g. image/*), 
     * or a comma-separated list of the two types.
     * </p>
     * @return the contentType property of this encoding instance
     **/
    String contentType() default "";

    /**
     * Style describes how the encoding value will be serialized depending on the type of the parameter value.
     * This property SHALL be ignored if the request body media type is not application/x-www-form-urlencoded.
     * <p>
     * Default values include: form, spaceDelimited, pipeDelimited, and deepObject.
     * </p>
     * @return the style of this encoding instance
     **/
    String style() default "";

    /**
     * When explode is set to true, property values of type array or object generate separate parameters for each value of the array, 
     * or key-value-pair of the map. <p>
     * For other types of properties this property has no effect. When style is form, the default value is true. </p>
     * For all other styles, the default value is false.
     * 
     * @return whether or not this array type encoding will have separate parameters generated for each array value
     **/
    boolean explode() default false;

    /**
     * Determines whether the encoding instance value SHOULD allow reserved characters, 
     * as defined by RFC3986 to be included without percent-encoding.
     * <p>
     * See RFC3986 for full definition of reserved characters.
     * </p>
     * @return whether or not this encoding instance allows reserved characters
     **/
    boolean allowReserved() default false;

    /**
     * An array of headers that corresponds to a map of headers in the encoding model.
     * Allows additional information to be provided as headers.
     * <p>
     * For example, Content-Disposition.
     * </p>
     * Content-Type is described separately and SHALL be ignored in this section. 
     * This property SHALL be ignored if the request body media type is not a multipart.
     * 
     * @return the array of headers for this encoding instance
     */
    Header[] headers() default {};

}
