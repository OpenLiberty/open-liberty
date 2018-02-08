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

package org.eclipse.microprofile.openapi.annotations.servers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation represents a Server used in an operation or used by all operations in an
 * OpenAPI document. 
 * <p>
 * When a Server annotation appears on a method the server is added to the corresponding
 * OpenAPI operation servers field.
 * <p>
 * When a Server annotation appears on a type then the server is added to all the operations 
 * defined in that type except for those operations which already have one or more servers 
 * defined. The server is also added to the servers defined in the root level of the 
 * OpenAPI document. 
 * <p>
 * This annotation is {@link java.lang.annotation.Repeatable Repeatable}.
 * <p>
 * <b>Note:</b> If both {@link org.eclipse.microprofile.openapi.annotations.servers.Server Server} and 
 * {@link org.eclipse.microprofile.openapi.annotations.servers.Servers Servers} annotation are specified on the same type,
 * the server definitions will be combined.
 * @see <a href=
 * "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#server-object">
 *      OpenAPI Specification Server Object</a>
 **/
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Servers.class)
@Inherited
public @interface Server {
    /**
     * A URL to the target host. This URL supports Server Variables and may be
     * relative, to indicate that the host location is relative to the location
     * where the OpenAPI definition is being served. Variable substitutions will
     * be made when a variable is named in {brackets}. This is a REQUIRED
     * property.
     * 
     * @return URL to the target host
     **/
    String url() default "";

    /**
     * An optional string describing the host designated by the URL. CommonMark
     * syntax MAY be used for rich text representation.
     * 
     * @return description of the host designated by URL
     **/
    String description() default "";

    /**
     * An array of variables used for substitution in the server's URL template.
     * 
     * @return array of variables
     **/
    ServerVariable[] variables() default {};

}
