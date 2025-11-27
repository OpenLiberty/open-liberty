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
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/DefaultValueConverter.java
 * Modifications have been made.
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import io.openliberty.mcp.annotations.ToolArg;

/**
 * Converts default values from {@link String} to an argument object of a specific type.
 * <p>
 * Converters are discovered automatically
 * <p>
 * Implementations must declare a public no-args constructor.
 * <p>
 * An implementation may be annotated with {@link Priority}. If multiple converters of the same priority exist for a specific
 * argument type, then only the converter with highest priority is registered.
 *
 * @see ToolArg#defaultValue()
 * @param <TYPE> the argument type
 */
public interface DefaultValueConverter<TYPE> {

    /**
     * @param defaultValue (must not be {@code null})
     * @return the converted object
     */
    TYPE convert(String defaultValue);

}
