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

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.servers.Server;

/**
 * Describes a single API operation on a path.
 **/
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Operation {
    /**
     * The HTTP method for this operation.
     *
     * @return the HTTP method of this operation
     **/
    String method() default "";

    /**
     * A list of tags for API documentation control.
     * <p>
     * Tags can be used for logical grouping of operations by resources or any other qualifier.
     * </p>
     * @return the list of tags associated with this operation
     **/
    String[] tags() default {};

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
     * Additional external documentation for this operation.
     *
     * @return external documentation associated with this operation instance
     **/
    ExternalDocumentation externalDocs() default @ExternalDocumentation();

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
     * An array of parameters applicable for this operation, 
     * which will be added to any automatically detected parameters in the method itself.
     * <p>
     * The list MUST NOT include duplicated parameters. 
     * A unique parameter is defined by a combination of a name and location.
     * </p>
     * @return the list of parameters for this operation
     **/
    Parameter[] parameters() default {};

    /**
     * The request body applicable for this operation.
     *
     * @return the request body of this operation
     **/
    RequestBody requestBody() default @RequestBody();

    /**
     * This is a REQUIRED property of an operation instance.
     * <p>
     * The list of possible responses as they are returned from executing this operation.
     * </p>
     * @return the list of responses for this operation
     **/
    APIResponse[] responses() default {};

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
     * A declaration of which security mechanisms can be used for this operation.
     * Only one of the security requirement objects need to be satisfied to authorize a request. 
     * <p>
     * This definition overrides any declared top-level security. 
     * To remove a top-level security declaration, an empty array can be used.
     * </p>
     * @return the list of security mechanisms for this operation
     */
    SecurityRequirement[] security() default {};

    /**
     * An alternative server array to service this operation.
     * <p>
     * If an alternative server object is specified at the Path Item Object or Root level, 
     * it will be overridden by this value.
     * </p>
     * @return the list of servers hosting this operation
     **/
    Server[] servers() default {};

    /**
     * The list of optional extensions.
     *
     * @return an optional array of extensions
     */
    Extension[] extensions() default {};

    /**
     * Allows this operation to be marked as hidden
     * 
     * @return whether or not this operation is hidden
     */
    boolean hidden() default false;
}
