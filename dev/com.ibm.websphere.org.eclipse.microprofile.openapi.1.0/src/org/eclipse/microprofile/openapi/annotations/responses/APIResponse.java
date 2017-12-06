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

package org.eclipse.microprofile.openapi.annotations.responses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.links.Link;
import org.eclipse.microprofile.openapi.annotations.media.Content;

/**
 * The ApiResponse annotation corresponds to the OpenAPI Response model object which 
 * describes a single response from an API Operation, including design-time,
 * static links to operations based on the response.
 * <p>
 * When this annotation is applied to a method the response is added to the responses
 * defined in the corresponding OpenAPI operation. If the operation already has a 
 * response with the specified responseCode the annotation on the method is ignored. 
 * 
 * <pre>
 * &#64;ApiResponse(responseCode="200", description="Calculate load size", content=
 *     [ &#64;Content(mediaType="application/json", Schema=&#64;Schema(type="integer")) ] )
 * &#64;GET
 * public getLuggageWeight(Flight id) {
 *     return getBagWeight(id) + getCargoWeight(id);
 * }
 * </pre>
 * 
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#responseObject"
 * 
 **/
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(APIResponses.class)
public @interface APIResponse {
    /**
     * A short description of the response. It is a REQUIRED property unless this is only a reference to a response instance.
     * 
     * @return description of the response.
     **/
    String description() default "";

    /**
     * The HTTP response code, or 'default', for the supplied response. May only have 1 default entry.
     * 
     * @return HTTP response code for this response instance or default
     **/
    String responseCode() default "default";

    /**
     * An array of response headers. Allows additional information to be included with response.
     * <p>
     * RFC7230 states header names are case insensitive. If a response header is defined with the name "Content-Type", it SHALL be ignored.
     * 
     * @return array of headers for this response instance
     **/
    Header[] headers() default {};

    /**
     * An array of operation links that can be followed from the response.
     * 
     * @return array of operation links for this response instance
     **/
    Link[] links() default {};

    /**
     * An array containing descriptions of potential response payloads for different media types.
     * 
     * @return content of this response instance
     **/
    Content[] content() default {};

    /**
     * The unique name to identify this response. Only REQUIRED when the response is defined
     * within {@link org.eclipse.microprofile.openapi.annotations.Components}. The name will
     * be used as the key to add this response to the 'responses' map for reuse.
     * 
     * @return this response's name
     **/
    String name() default "";

    /**
     * Reference value to a Response object.
     * <p>
     * This property provides a reference to an object defined elsewhere. This property and
     * all other properties are mutually exclusive. If other properties are defined in addition
     * to the ref property then the result is undefined.
     *
     * @return reference to a response
     **/
    String ref() default "";

}
