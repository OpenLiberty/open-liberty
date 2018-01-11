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

package org.eclipse.microprofile.openapi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a single API operation on a path.
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#operationObject">OpenAPI Specification Operation
 *      Object</a>
 **/
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Operation {

    /**
     * Provides a brief description of what this operation does.
     *
     * @return a summary of this operation
     **/
    String summary() default "";

    /**
     * A verbose description of the operation behaviour.
     * CommonMark syntax MAY be used for rich text representation.
     *
     * @return a description of this operation
     **/
    String description() default "";

    /**
     * Unique string used to identify the operation. 
     * The id MUST be unique among all operations described in the API.
     * <p>
     * Tools and libraries MAY use the operationId to uniquely identify an operation, 
     * therefore, it is RECOMMENDED to follow common programming naming conventions.
     * </p>
     * @return the ID of this operation
     **/
    String operationId() default "";

    /**
     * Allows an operation to be marked as deprecated. 
     * Alternatively use the @Deprecated annotation.
     * <p>
     * Consumers SHOULD refrain from usage of a deprecated operation.
     * </p>
     * @return whether or not this operation is deprecated
     **/
    boolean deprecated() default false;

    /**
     * Allows this operation to be marked as hidden
     * 
     * @return whether or not this operation is hidden
     */
    boolean hidden() default false;
}
