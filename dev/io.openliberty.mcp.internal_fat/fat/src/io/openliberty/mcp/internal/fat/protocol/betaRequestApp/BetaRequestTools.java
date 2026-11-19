/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.protocol.betaRequestApp;

import java.util.TreeSet;

import org.mcpjava.server.McpRequest;
import org.mcpjava.server.tools.Tool;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Tools used by the beta protocol version tests to check what {@link McpRequest} returns for 2026-07-28.
 * <p>
 * These are kept out of BasicTools because the full BasicTools tool list is asserted in other tests.
 */
@ApplicationScoped
public class BetaRequestTools {

    /**
     * Reports what the {@link McpRequest} returns for the protocol version, client capabilities and client info.
     * <p>
     * Capabilities are reported as a sorted list of their top level names so that the result is deterministic.
     */
    @Tool
    public String describeMcpRequest(McpRequest request) {
        return "version=" + request.protocolVersion()
               + ";caps=" + new TreeSet<>(request.rawClientCapabilities().keySet())
               + ";client=" + request.clientInfo().name();
    }
}
