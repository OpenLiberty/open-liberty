/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.exceptions.jsonrpc;

/**
 * Indicates an exception which should result in a specific response
 * <p>
 * Mostly used so that all exceptions of this type can be rethrown and not processed
 */
public abstract sealed class McpResponseException extends RuntimeException permits HttpResponseException, JSONRPCException {

    private static final long serialVersionUID = 1L;

    protected McpResponseException() {}

    protected McpResponseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    protected McpResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    protected McpResponseException(String message) {
        super(message);
    }

    protected McpResponseException(Throwable cause) {
        super(cause);
    }

}
