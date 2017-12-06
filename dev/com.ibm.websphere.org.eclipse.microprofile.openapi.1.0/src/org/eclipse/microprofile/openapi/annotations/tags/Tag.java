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

package org.eclipse.microprofile.openapi.annotations.tags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;

/**
 * This object represents a tag. A tag is meta-information you can use to help
 * organize your API endpoints and it can appear in two contexts.
 * <p>
 * Define tag objects on a method that implements an operation or on a type that
 * contains operations. They will be gathered and stored in the root object of
 * the OpenAPI document.
 * <p>
 * If the tag annotation is associated with a method that corresponds to an
 * operation then the tag name will be added to the OpenAPI document and also be
 * added to the operation tags (see @Operation).
 * <p>
 * If more than one tag is defined with the same name then only one tag with that name
 * will appear in the OpenAPI document. 
 * <p>
 * If more than one tag is defined with the same name and only one tag contains a 
 * description that is a non-empty string then that description will be preserved in 
 * the OpenAPI document. If more than one non-empty description is specified the results 
 * are implementation dependent. 
 * <p>
 * If more than one tag is defined with the same name and only one tag contains an
 * external documentation that is defined with at least one non-empty string then that 
 * external documentation will be preserved in the OpenAPI document. If more than one 
 * non-empty external documentation is specified the results are implementation dependent. 
 * <pre>
 * &#64;Tag(name = "luggage", description = "Operations related to luggage handling.")
 * &#64;GET
 * public Location getLuggage(LuggageID id) {
 *     return getLuggageLocation(id);
 * }
 * </pre>
 * <p>
 * If the tag annotation is applied to a type then the tag is defined in OpenAPI and a
 * reference is added to each operation in the type.
 * <p>
 * Operations in your application can contain references to tag definitions to
 * help organize the software. The @Operation annotation may contain a list of
 * tag names.
 * 
 * <pre>
 * &#64;Operation(summary = "Track luggage in the international division", 
 *      tags = { "luggage", "international" }, ...
 * </pre>
 * 
 * @see <a href=
 *      "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#tagObject">
 *      OpenAPI Specification Tag Object</a>
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Tags.class)
@Inherited
public @interface Tag {

    /**
     * The name of this tag. It is a REQUIRED property unless this is only a reference to a tag instance.
     *
     * @return the name of this tag
     */
    String name() default "";

    /**
     * A short description for this tag.
     *
     * @return the description of this tag
     */
    String description() default "";

    /**
     * Additional external documentation for this tag.
     *
     * @return the external documentation for this tag
     */
    ExternalDocumentation externalDocs() default @ExternalDocumentation();

    /**
     * Reference value to a Tag object.
     * <p>
     * This property provides a reference to an object defined elsewhere. This property and
     * all other properties are mutually exclusive. If other properties are defined in addition
     * to the ref property then the result is undefined.
     *
     * @return reference to a tag
     **/
    String ref() default "";
}
