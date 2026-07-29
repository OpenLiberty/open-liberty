/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.tools;

/**
 * Indicates that the caller is not authorized to invoke a {@link Tool} method.
 * <p>
 * If a method annotated with {@link Tool} throws a {@link ToolCallUnauthorizedException} then the
 * framework returns an HTTP 403 Forbidden response to the client, in the same way as when the
 * framework-level authorization check (via {@code @DenyAll}, {@code @RolesAllowed}, etc.) fails.
 * The HTTP response body will contain the exception message as plain text
 * ({@code Content-Type: text/plain}); no JSON-RPC envelope is included.
 * <p>
 * This allows application code to perform fine-grained authorization checks inside a {@code @Tool}
 * method body and signal an authorization failure using the standard HTTP 403 status code.
 * <p>
 * {@link ToolCallUnauthorizedException} extends {@link ToolCallException} so that it is treated
 * consistently as a first-class tool exception in the framework, while still being routed to the
 * HTTP 403 path rather than the business-error {@code isError: true} path.
 */
public class ToolCallUnauthorizedException extends ToolCallException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new {@link ToolCallUnauthorizedException} with the given message.
     *
     * @param message a description of why authorization failed
     */
    public ToolCallUnauthorizedException(String message) {
        super(message);
    }

    /**
     * Creates a new {@link ToolCallUnauthorizedException} with the given message and cause.
     *
     * @param message a description of why authorization failed
     * @param cause the underlying cause
     */
    public ToolCallUnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new {@link ToolCallUnauthorizedException} with the given cause.
     *
     * @param cause the underlying cause
     */
    public ToolCallUnauthorizedException(Throwable cause) {
        super(cause);
    }

}
