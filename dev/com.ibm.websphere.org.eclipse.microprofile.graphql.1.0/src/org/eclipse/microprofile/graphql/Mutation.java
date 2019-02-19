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
 * Specifies that the annotated method provides the implementation (ie. the resolver) for a GraphQL mutation.
 * <br><br>
 * For example, a user might annotate a method as such:
 * <pre>
 * public class CharacterService {
 *     {@literal @}Mutation(name = "addCharacter", description = "Save a new character")
 *     public Character save(Character character) {
 *         //...
 *     }
 * }
 * </pre>
 *
 * Schema generation of this would result in a stanza such as:
 * <pre>
 * type Mutation {
 *     # Save a new character
 *     addCharacter(character: CharacterInput): Character
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface Mutation {

    /**
     * @return the name of the GraphQL mutation.
     */
    String value() default "";

    /**
     * @return the textual description of the mutation to be included as a comment in the schema.
     */
    String description() default "";

    /**
     * @return a non-empty string will indicate that this mutation is deprecated and provides the reason for the deprecation.
     */
    String deprecationReason() default "";
}