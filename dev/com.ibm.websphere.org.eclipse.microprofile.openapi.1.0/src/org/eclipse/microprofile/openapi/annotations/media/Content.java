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

/**
 * This object provides schema and examples for a particular media type.
 * 
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#mediaTypeObject">OpenAPI Specification Media Type
 *      Object</a>
 **/
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Content {
    /**
     * The media type that this object applies to.
     *
     * @return the media type value
     **/
    String mediaType() default "";

    /**
     * An array of examples used to show the use of the associated schema.
     *
     * @return the list of examples
     **/
    ExampleObject[] examples() default {};

    /**
     * The schema defining the type used for the request body.
     *
     * @return the schema of this media type
     **/
    Schema schema() default @Schema();

    /**
     * An array of encoding corresponding to a map of property name and its encoding information in MediaType model. 
     * The key, being the property name, MUST exist in the schema as a property.
     * <p> 
     * The encoding object SHALL only apply to requestBody objects when the media type is multipart or application/x-www-form-urlencoded.
     * </p>
     * @return the array of encodings
     */
    Encoding[] encoding() default {};

}
