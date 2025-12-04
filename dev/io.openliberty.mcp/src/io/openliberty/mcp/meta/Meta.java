/*******************************************************************************
 * Copyright (c) contributors to https://github.com/quarkiverse/quarkus-mcp-server
 * Copyright (c) 2025 IBM Corporation and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/Meta.java
 * Modifications have been made.
 *******************************************************************************/
package io.openliberty.mcp.meta;

import jakarta.json.JsonObject;

/**
 * Additional metadata sent from the client to the server.
 * <p>
 * All feature methods can accept this class as a parameter. It will be automatically injected before the
 * method is invoked.
 */
public interface Meta {

    /**
     *
     * @param key
     * @return the value for the given key, or {@code null}
     */
    Object getValue(MetaKey key);

    /**
     * If {@code _meta} is not present then an empty {@link JsonObject} is returned.
     *
     * @return the JSON representation of {@code _meta}, never {@code null}
     */
    JsonObject asJsonObject();

}