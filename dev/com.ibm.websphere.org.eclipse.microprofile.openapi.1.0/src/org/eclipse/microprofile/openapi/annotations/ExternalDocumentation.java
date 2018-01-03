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
 * This annotation allows referencing an external resource for extended documentation.
 * <p>
 * When it is applied to a method the value of the annotation is added to the corresponding 
 * OpenAPI operation definition.
 * <p>
 * When it is applied to a type and one or more of the fields are not empty strings the
 * annotation value is added to the OpenAPI document root. If more than one non-empty 
 * annotation is applied to a type in the application or if the externalDocs field of the 
 * OpenAPIDefinition annotation is supplied the results are not defined. 
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#externalDocumentationObject">OpenAPI Specification
 *      External Documentation Object</a>
 **/
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ExternalDocumentation {

    /**
     * A short description of the target documentation.
     * 
     * @return the documentation description
     **/
    String description() default "";

    /**
     * The URL for the target documentation. Value must be in the format of a URL.
     * 
     * @return the documentation URL
     **/
    String url() default "";

}
