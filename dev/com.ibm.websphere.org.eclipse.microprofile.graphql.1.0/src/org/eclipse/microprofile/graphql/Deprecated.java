/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.microprofile.graphql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field/method as being deprecated, with a reason.
 * You should also be able to use java.lang.Deprecated, but then you can not provide a reason
 * <br><br>
 * For example, a user might annotate a method as such:
 * 
 * <pre>
 * {@literal @}InputType(name = "StarshipInput", description = "Input type for a starship")
 * public class Starship {
 *     private String id;
 *     {@literal @}Deprecated("Field is deprecated!")
 *     private String name;
 *     private float length;
 *
 *     // getters/setters...
 * }
 * </pre>
 *
 * Schema generation of this would result in a stanza such as:
 * 
 * <pre>
 * type Starship {
 *   id: String
 *   name: String @deprecated(reason: "Field is deprecated!")
 *   length: Float
 * }
 * </pre>
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Deprecated {

    /**
     * @return the reason and/or alternative to use.
     */
    String value() default "Deprecated";
}