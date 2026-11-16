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
import java.util.Map;
import java.util.NoSuchElementException;

/**
 *
 */
public enum RequestMethod {

    TOOLS_CALL("tools/call", MethodType.REQUEST),
    TOOLS_LIST("tools/list", MethodType.REQUEST),
    INITIALIZE("initialize", MethodType.PRE_INIT_REQUEST),
    INITIALIZED("notifications/initialized", MethodType.NOTIFICATION),
    CANCELLED("notifications/cancelled", MethodType.NOTIFICATION),
    PING("ping", MethodType.PRE_INIT_REQUEST);

    private static final Map<String, RequestMethod> METHODS_BY_NAME;
    private final String methodName;
    private final MethodType methodType;

    public enum MethodType {
        PRE_INIT_REQUEST,
        REQUEST,
        NOTIFICATION
    }

    /**
     * @param string
     */
    RequestMethod(String methodName, MethodType methodType) {
        this.methodName = methodName;
        this.methodType = methodType;
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

    public MethodType getMethodType() {
        return methodType;
    }

    public boolean isPreInit() {
        return methodType == MethodType.PRE_INIT_REQUEST;
    }

    public static RequestMethod getForMethodName(String methodName) throws NoSuchElementException {
        RequestMethod result = METHODS_BY_NAME.get(methodName);
        if (result == null) {
            throw new NoSuchElementException(methodName);
        }
        return result;
    }
}
