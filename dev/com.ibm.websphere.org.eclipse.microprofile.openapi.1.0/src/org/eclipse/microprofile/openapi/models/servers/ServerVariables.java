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

package org.eclipse.microprofile.openapi.models.servers;

import java.util.Map;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;

/**
 * ServerVariables
 *
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#serverVariablesObject">ServerVariables Object</a>
 */
public interface ServerVariables extends Constructible, Extensible, Map<String, ServerVariable> {

    /**
     * This method adds a key-value item to a ServerVariables instance from the name-item parameter pair and returns the modified instance.
     *
     * @param name the name of ServerVariable instance
     * @param serverVariable the ServerVariable instance
     * @return ServerVariables instance with the added name-item pair.
     */
    ServerVariables addServerVariable(String name, ServerVariable serverVariable);

}