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

import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.McpRequest;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

public class HeaderValidation {

    private static final TraceComponent tc = Tr.register(HeaderValidation.class);

    /** The key in the request {@code _meta} which carries the protocol version from 2026-07-28 */
    static final String META_PROTOCOL_VERSION = "io.modelcontextprotocol/protocolVersion";

    public static boolean acceptContains(String acceptHeader, String mime) {
        if (acceptHeader == null || mime == null) {
            return false;
        }
        if (acceptHeader.equals("*/*")) {
            return true;
        }
        String target = mime.toLowerCase(java.util.Locale.ROOT);
        for (String part : acceptHeader.split(",")) {
            String value = part.trim().toLowerCase(java.util.Locale.ROOT);
            value = value.split(";")[0];
            if (value.equals("*/*"))
                return true;

            if (value.startsWith(target))
                return true;

            // Match type/* e.g., text/*
            if (value.endsWith("/*")) {
                String type = value.substring(0, value.indexOf("/"));
                if (target.startsWith(type + "/"))
                    return true;
            }
        }
        return false;
    }

    /**
     * Checks that the MCP-Protocol-Version header matches the protocol version carried in the request body.
     * <p>
     * From protocol version 2026-07-28 the header value must match the
     * {@code io.modelcontextprotocol/protocolVersion} field in the request's {@code params._meta}. Earlier
     * versions do not carry the version in the body so no check is made for them. The specification does not define
     * header requirements for notifications, so they are not checked either.
     *
     * @param headerVersion the protocol version read from the MCP-Protocol-Version header
     * @param request the parsed request
     * @throws JSONRPCException with {@link JSONRPCErrorCode#HEADER_MISMATCH} if the values do not match or
     *             the body does not carry a protocol version
     */
    public static void validateProtocolVersionMatchesBody(McpProtocolVersion headerVersion, McpRequest request) throws JSONRPCException {
        if (headerVersion != McpProtocolVersion.V_2026_07_28 || request.id() == null) {
            return;
        }

        String bodyVersion = null;
        JsonObject params = request.params();
        if (params != null && params.get("_meta") instanceof JsonObject meta) {
            JsonValue value = meta.get(META_PROTOCOL_VERSION);
            if (value instanceof JsonString str) {
                bodyVersion = str.getString();
            } else if (value != null) {
                bodyVersion = value.toString();
            }
        }

        if (!headerVersion.getVersion().equals(bodyVersion)) {
            String message = buildVersionMismatchMessage(headerVersion.getVersion(), bodyVersion);
            throw new JSONRPCException(JSONRPCErrorCode.HEADER_MISMATCH, List.of(message));
        }
    }

    private static String buildVersionMismatchMessage(String headerVersion, String bodyVersion) {
        return bodyVersion == null
                                   ? Tr.formatMessage(tc, "header.mismatch.protocol.version.missing",
                                                      headerVersion, META_PROTOCOL_VERSION)
                                   : Tr.formatMessage(tc, "header.mismatch.protocol.version",
                                                      headerVersion, META_PROTOCOL_VERSION, bodyVersion);
    }

}
