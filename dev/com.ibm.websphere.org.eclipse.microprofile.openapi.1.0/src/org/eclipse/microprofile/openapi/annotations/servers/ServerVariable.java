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

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An object representing a Server Variable for server URL template substitution.
 * 
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#server-variable-object">ServerVariable Object</a>
 **/
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ServerVariable {
    /**
     * The name of this server variable. This is a REQUIRED property.
     * 
     * @return the name of the server variable
     **/
    String name();

    /**
     * An array of enum values for this variable. This field maps to the enum property in the OAS schema and to enumeration field of ServerVariable
     * model.
     * 
     * @return array of possible values for this ServerVariable
     **/
    String[] enumeration() default {};

    /**
     * The default value of this server variable. This is a REQUIRED property.
     * 
     * @return the defualt value of this server variable
     **/
    String defaultValue();

    /**
     * An optional description for the server variable. CommonMark syntax can be used for rich text representation.
     * 
     * @return the description of this server variable
     **/
    String description() default "";

}
