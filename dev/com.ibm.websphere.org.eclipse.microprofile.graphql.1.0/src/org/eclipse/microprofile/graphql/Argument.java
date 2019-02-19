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
 * Controls the mapping of a method's parameter to an argument of a GraphQL operation (query/mutation/subscription).
 * <br><br>
 * For example, a user might annotate a method's parameter as such:
 * <pre>
 * public class CharacterService {
 *     {@literal @}Query(value = "searchByName",
 *                 description = "Search characters by name")
 *     public List{@literal <}Character{@literal >} getByName(
 *                      {@literal @}Argument(value = "name", defaultValue = "Han Solo", description = "Name to search for") String name) {
 *         //...
 *     }
 * }
 * </pre>
 *
 * Schema generation of this would result in a stanza such as:
 * <pre>
 * type Query {
 *         # Search characters by name
 *         # name: Name to search for. Default value: Han Solo.
 *         searchByName(name: String = "Han Solo"): [Character]
 *     }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Argument {

    /**
     * @return the name to use for the GraphQL argument.
     */
    String value();

    /**
     * @return the textual description of the GraphQL argument to be included as a comment in the schema.
     */
    String description() default "";

    String defaultValue() default "";

}