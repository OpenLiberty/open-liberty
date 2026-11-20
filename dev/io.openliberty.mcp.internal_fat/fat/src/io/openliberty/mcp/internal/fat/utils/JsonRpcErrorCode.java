/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.utils;

/**
 * The standard JSON-RPC 2.0 error codes, for use with {@link JsonRpcResponseMatcher#withErrorCode(int)}.
 */
public final class JsonRpcErrorCode {

    /** Invalid JSON was received by the server. */
    public static final int PARSE_ERROR = -32700;
    /** The JSON sent is not a valid JSON-RPC request object. */
    public static final int INVALID_REQUEST = -32600;
    /** The requested method does not exist or is not available. */
    public static final int METHOD_NOT_FOUND = -32601;
    /** The method's parameters are invalid. */
    public static final int INVALID_PARAMS = -32602;
    /** An internal JSON-RPC error occurred in the server. */
    public static final int INTERNAL_ERROR = -32603;

    private JsonRpcErrorCode() {}
}
