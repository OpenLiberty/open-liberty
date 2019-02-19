/*
 *******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

/**
 * APIs for building a code-first GraphQL endpoint, for example:
 * <pre>
 * public class CharacterService {
 *     {@literal @}Query(value = "friendsOf",
 *                 description = "Returns all the friends of a character")
 *     public List{@literal <}Character{@literal >} getFriendsOf(Character character) {
 *         // ... 
 *     }
 * }
 * </pre>
 * @since 1.0
 */
@org.osgi.annotation.versioning.Version("1.0")
package org.eclipse.microprofile.graphql;
