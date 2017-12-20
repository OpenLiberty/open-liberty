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
 * organize your API end-points and it can appear in two contexts.
 * <p>
 * Define tag objects on a method that implements an operation or on a class that
 * contains operations. They will be gathered and stored in the root object of
 * the OpenAPI document.
 * <p>
 * If the tag annotation definition is associated with a method that corresponds to an
 * operation then the tag name will be added to the OpenAPI document and also be
 * added to the operation tags.
 * <p>
 * Reference to a Tag can be created by setting the 'ref' attribute.
 * <p>
 * If the Tag annotation is specified on a class then each operation in the class will inherit the tag unless the operation
 * specifies Tag(s). An operation can specify an empty Tag to not to be associated with any tag(s) inherited from the class.
 * <p>
 * This annotation is {@link java.lang.annotation.Repeatable Repeatable}.
 * <p>
 * If more than one tag is defined with the same name then only one tag with that name will appear 
 * in the OpenAPI document and the results are implementation dependent. 
 * <p>
 * <b>Note:</b> If both {@link org.eclipse.microprofile.openapi.annotations.tags.Tag Tag} and 
 * {@link org.eclipse.microprofile.openapi.annotations.tags.Tags Tags} annotations are specified on the same method or class,
 * then both tag definitions should be applied.
 * <pre>
 * &#64;Tag(name = "luggage", description = "Operations related to luggage handling.")
 * &#64;GET
 * public Location getLuggage(LuggageID id) {
 *     return getLuggageLocation(id);
 * }
 * </pre>
 * 
 * <pre>
 * &#64;Tag(ref = "Bookings")
 * &#64;GET
 * public Location getBookings() {
 *     return Response.ok().entity(bookings.values()).build();
 * }
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
     * The name of this tag. The name must be unique and is case sensitive. 
     * It is a REQUIRED property unless this is only a reference to a tag instance.
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
