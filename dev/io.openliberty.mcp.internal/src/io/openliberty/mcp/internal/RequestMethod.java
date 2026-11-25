/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import static io.openliberty.mcp.internal.McpProtocolVersion.V_2026_07_28;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 *
 */
public enum RequestMethod {

    TOOLS_CALL("tools/call", MethodType.REQUEST, anyVersion()),
    TOOLS_LIST("tools/list", MethodType.REQUEST, anyVersion()),
    INITIALIZE("initialize", MethodType.PRE_INIT_REQUEST, removedSinceVersion(V_2026_07_28)),
    INITIALIZED("notifications/initialized", MethodType.NOTIFICATION, removedSinceVersion(V_2026_07_28)),
    CANCELLED("notifications/cancelled", MethodType.NOTIFICATION, anyVersion()),
    PING("ping", MethodType.PRE_INIT_REQUEST, removedSinceVersion(V_2026_07_28));

    private static final Map<String, RequestMethod> METHODS_BY_NAME;
    private final String methodName;
    private final MethodType methodType;
    private final Predicate<McpProtocolVersion> versionPredicate;

    public enum MethodType {
        PRE_INIT_REQUEST,
        REQUEST,
        NOTIFICATION
    }

    /**
     * @param methodName the JSON-RPC method name
     * @param methodType the type of method
     * @param versionPredicate the predicate to test whether this method is valid in a protocol version
     */
    RequestMethod(String methodName, MethodType methodType, Predicate<McpProtocolVersion> versionPredicate) {
        this.methodName = methodName;
        this.methodType = methodType;
        this.versionPredicate = versionPredicate;
    }

    private static Predicate<McpProtocolVersion> anyVersion() {
        return requestedVersion -> true;
    }

    private static Predicate<McpProtocolVersion> removedSinceVersion(McpProtocolVersion cutOff) {
        return requestedVersion -> requestedVersion == null || requestedVersion.isOlderThan(cutOff);
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

    /**
     * Returns whether this method exists in the given protocol version.
     *
     * @param version the protocol version, or {@code null} if it is not known
     * @return {@code true} if the method is valid for {@code version}, otherwise {@code false}
     */
    public boolean isSupportedIn(McpProtocolVersion version) {
        return versionPredicate.test(version);
    }

    public static RequestMethod getForMethodName(String methodName) throws NoSuchElementException {
        RequestMethod result = METHODS_BY_NAME.get(methodName);
        if (result == null) {
            throw new NoSuchElementException(methodName);
        }
        return result;
    }
}
