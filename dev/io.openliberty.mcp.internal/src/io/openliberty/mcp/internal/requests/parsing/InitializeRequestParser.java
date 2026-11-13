/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests.parsing;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.mcp.internal.McpInternal;
import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.requests.ImplementationInfoImpl;
import io.openliberty.mcp.internal.requests.InitializeRequest;
import io.openliberty.mcp.internal.requests.McpOperationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;

@ApplicationScoped
public class InitializeRequestParser {

    private final Jsonb jsonb;

    // CDI no-args constructor
    InitializeRequestParser() {
        jsonb = null;
    }

    @Inject
    public InitializeRequestParser(@McpInternal Jsonb jsonb) {
        this.jsonb = jsonb;
    }

    @FFDCIgnore(NoSuchElementException.class)
    public InitializeRequest parse(McpOperationRequest opRequest) {
        InitializeRequestTO to = CommonParser.parseParamsTransferObject(jsonb, opRequest.args(), InitializeRequestTO.class);

        McpProtocolVersion protocolVersion;
        try {
            protocolVersion = McpProtocolVersion.parse(to.protocolVersion());
        } catch (NoSuchElementException e) {
            // Client requested an unrecognized protocol
            protocolVersion = null;
        }

        // The protocol requires these fields, we allow them to be missing to avoid changing behavior
        Map<String, Object> capabilities = to.capabilities() != null ? to.capabilities() : Collections.emptyMap();
        ImplementationInfoImpl clientInfo = to.clientInfo() != null ? to.clientInfo() : ImplementationInfoImpl.UNKNOWN;

        return new InitializeRequest(protocolVersion, capabilities, clientInfo);
    }
}
