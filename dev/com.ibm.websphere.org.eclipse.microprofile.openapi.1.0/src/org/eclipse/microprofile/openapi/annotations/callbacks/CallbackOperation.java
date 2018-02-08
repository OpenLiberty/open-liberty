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

package org.eclipse.microprofile.openapi.annotations.callbacks;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

/**
 * Describes a single API callback operation.
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#operationObject">OpenAPI Specification Operation
 *      Object</a>
 **/
@Target({ })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface CallbackOperation {
    /**
     * The HTTP method for this callback operation.
     *
     * @return the HTTP method of this callback operation
     **/
    String method() default "";

    /**
     * Provides a brief description of what this callback operation does.
     *
     * @return a summary of this callback operation
     **/
    String summary() default "";

    /**
     * A verbose description of the callback operation behavior.
     * CommonMark syntax MAY be used for rich text representation.
     *
     * @return a description of this callback operation
     **/
    String description() default "";

    /**
     * Additional external documentation for this callback operation.
     *
     * @return external documentation associated with this callback operation instance
     **/
    ExternalDocumentation externalDocs() default @ExternalDocumentation();

    /**
     * An array of parameters applicable for this callback operation, 
     * which will be added to any automatically detected parameters in the method itself.
     * <p>
     * The list MUST NOT include duplicated parameters. 
     * A unique parameter is defined by a combination of a name and location.
     * </p>
     * @return the list of parameters for this callback operation
     **/
    Parameter[] parameters() default {};

    /**
     * The request body applicable for this callback operation.
     *
     * @return the request body of this callback operation
     **/
    RequestBody requestBody() default @RequestBody();

    /**
     * This is a REQUIRED property of an callback operation instance.
     * <p>
     * The list of possible responses as they are returned from executing this callback operation.
     * </p>
     * @return the list of responses for this callback operation
     **/
    APIResponse[] responses() default {};

    /**
     * A declaration of which security mechanisms can be used for this callback operation.
     * Only one of the security requirement objects need to be satisfied to authorize a request. 
     * <p>
     * This definition overrides any declared top-level security. 
     * To remove a top-level security declaration, an empty array can be used.
     * @return the list of security mechanisms for this callback operation
     */
    SecurityRequirement[] security() default {};

    /**
     * The list of optional extensions.
     *
     * @return an optional array of extensions
     */
    Extension[] extensions() default {};
}