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
 **/
@Target({ ElementType.PARAMETER })
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
     * This is a REQUIRED property. The content of the request body.
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
     * Reference value to a RequestBody object.
     *
     * @return reference to a request body
     **/
    String ref() default "";

}
