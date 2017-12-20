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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.microprofile.openapi.annotations.media.Content;

/**
 * Describes a single request body.
 * 
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#requestBodyObject">requestBody Object</a>
 **/
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RequestBody {
    /**
     * A brief description of the request body.
     * <p>
     * This could contain examples of use. 
     * CommonMark syntax MAY be used for rich text representation.
     * </p>
     * @return description of this requestBody instance
     **/
    String description() default "";

    /**
     * The content of the request body. It is a REQUIRED property unless this is only a reference to a request body instance.
     * 
     * @return content of this requestBody instance
     **/
    Content[] content() default {};

    /**
     * Determines if the request body is required in the request.
     * 
     * @return whether or not this requestBody is required
     **/
    boolean required() default false;

    /**
     * The unique name to identify this request body. Unless this annotation is used on the actual request body parameter,
     * it is required to match the name of that parameter so the appropriate association can be made.  When the request body
     * is defined within {@link org.eclipse.microprofile.openapi.annotations.Components}. The name will be used as the key to 
     * add this request body to the 'requestBodies' map for reuse.
     * 
     * @return this request body's name
     **/
    String name() default "";
    
    /**
     * Reference value to a RequestBody object.
     * <p>
     * This property provides a reference to an object defined elsewhere. This property and
     * all other properties are mutually exclusive. If other properties are defined in addition
     * to the ref property then the result is undefined.
     *
     * @return reference to a request body
     **/
    String ref() default "";

}
