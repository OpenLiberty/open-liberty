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
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/ToolResponseEncoder.java
 * Modifications have been made.
 *******************************************************************************/
package io.openliberty.mcp.encoders;

import io.openliberty.mcp.tools.ToolResponse;

/**
 * Encodes an object as {@link ToolResponse}.
 * <p>
 * If a tool response encoder exists and matches a specific return type then it always takes precedence over matching
 * {@link ContentEncoder}.
 * <p>
 * Implementation classes must be CDI beans. Qualifiers are ignored. {@link jakarta.enterprise.context.Dependent} beans are
 * reused during encoding.
 * <p>
 * Encoders may define the priority with {@link jakarta.annotation.Priority}. An encoder with higher priority takes precedence.
 *
 * @param <TYPE>
 * @see ToolResponse
 * @see Tool
 */
public interface ToolResponseEncoder<TYPE> extends Encoder<TYPE, ToolResponse> {

}