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
 * Excludes an otherwise mapped element. Mostly useful to e.g. mark a field as excluded in the GraphQL input type only.
 * <br><br>
 * The behavior is different depending on where <b>@Ignore</b> annotation is placed:
 *  <ul>
 *      <li><b>On field</b>: Field is ignored in both graphql type and input type.</li>
 *      <li><b>On getter</b>: Field is ignored in the graphql type.</li>
 *      <li><b>On setter</b>: Field is ignored in the graphql input type.</li>
 *  </ul>
 * <br><br>
 * For example, a user might annotate a class' properties and/or getters/setters as such:
 * <pre>
 * {@literal @}Type(name = "Starship", description = "A starship in StarWars")
 * {@literal @}InputType(name = "StarshipInput", description = "Input object for a starship")
 * public class Starship {
 *     private String id;
 *     private String name;
 *     private float length;
 *     {@literal @}Ignore
 *     private String color;
 *     private float mass;
 *
 *     {@literal @}Ignore
 *     public void setLength(float length) {
 *         this.length = length;
 *     }
 *
 *     {@literal @}Ignore
 *     public float getMass() {
 *         return mass;
 *     }
 *
 *     // other getters/setters...
 * }
 * </pre>
 *
 * Schema generation of this would result in a stanza such as:
 * <pre>
 * # A starship from Starwars
 * type Starship {
 *   id: String
 *   name: String
 *   length: Float
 * }
 *
 * # Input object for a starship
 * input Starship {
 *   id: String
 *   name: String
 *   mass: Float
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Documented
public @interface Ignore {

}
