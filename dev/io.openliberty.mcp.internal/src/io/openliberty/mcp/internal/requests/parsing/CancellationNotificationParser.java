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

import java.util.List;

import io.openliberty.mcp.internal.McpInternal;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.CancellationNotification;
import io.openliberty.mcp.internal.requests.McpOperationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;

/**
 *
 */
@ApplicationScoped
public class CancellationNotificationParser {

    private Jsonb jsonb;

    CancellationNotificationParser() {}

    @Inject
    public CancellationNotificationParser(@McpInternal Jsonb jsonb) {
        this.jsonb = jsonb;
    }

    public CancellationNotification parse(McpOperationRequest opRequest) {
        CancellationNotificationTO to = jsonb.fromJson(opRequest.args().toString(), CancellationNotificationTO.class);

        if (to.requestId() == null) {
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of("requestId missing"));
        }

        return new CancellationNotification(opRequest, to.requestId(), to.reason());
    }

}
