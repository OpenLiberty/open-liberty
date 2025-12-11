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
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/ToolCallException.java
 * Modifications have been made.
 *******************************************************************************/
package io.openliberty.mcp.tools;

import io.openliberty.mcp.annotations.Tool;

/**
 * Indicates a business logic error in a {@link Tool} method.
 * <p>
 * If a method annotated with {@link Tool} throws an exception that is an instance of {@link ToolCallException} then it is
 * automatically converted to a failed {@link ToolResponse}. The message of the exception is used as the text of the result
 * content.
 */
public class ToolCallException extends RuntimeException {

    private static final long serialVersionUID = 6214164159077697693L;

    public ToolCallException(String message, Throwable cause) {
        super(message, cause);
    }

    public ToolCallException(String message) {
        super(message);
    }

    public ToolCallException(Throwable cause) {
        super(cause);
    }

}
