/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;

/**
 *
 */
public enum RequestMethod {

    TOOLS_CALL("tools/call"),
    TOOLS_LIST("tools/list"),
    INITIALIZE("initialize"),
    INITIALIZED("notifications/initialized"),
    CANCELLED("notifications/cancelled"),
    PING("ping");

    private static final Map<String, RequestMethod> METHODS_BY_NAME;
    private final String methodName;

    /**
     * @param string
     */
    RequestMethod(String methodName) {
        this.methodName = methodName;
    }

    static {
        METHODS_BY_NAME = new HashMap<>();
        for (RequestMethod m : values()) {
            METHODS_BY_NAME.put(m.getMethodName(), m);
        }
    }

    public String getMethodName() {
        return methodName;
    }

    public static RequestMethod getForMethodName(String methodName) throws NoSuchElementException {
        RequestMethod result = METHODS_BY_NAME.get(methodName);
        if (result == null) {
            throw new JSONRPCException(JSONRPCErrorCode.METHOD_NOT_FOUND, List.of(methodName + " not found"));
        }
        return result;
    }
}
