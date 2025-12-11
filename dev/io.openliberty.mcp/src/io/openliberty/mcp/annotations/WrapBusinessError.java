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
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/WrapBusinessError.java
 * Modifications have been made.
 *******************************************************************************/
package io.openliberty.mcp.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.openliberty.mcp.tools.ToolCallException;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/**
 * Wraps a matching exception thrown from a "feature" method with an exception that represents a business logic error and is
 * automatically converted to a failed response.
 * <p>
 * For example, if a {@link Tool} method throws an exception it's wrapped with a {@link ToolCallException} which is
 * automatically converted to a failed {@link ToolResponse}.
 *
 * @see Tool
 * @see ToolCallException
 */
@InterceptorBinding
@Retention(RUNTIME)
@Target({ METHOD, TYPE })
public @interface WrapBusinessError {

    /**
     * The exception is only wrapped automatically if it's assignable from any of the specified classes.
     */
    @Nonbinding
    public Class<? extends Throwable>[] value() default { Exception.class };
}
