/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.tools.parsing;

import io.openliberty.mcp.internal.McpInternal;
import io.openliberty.mcp.internal.requests.McpOperationRequest;
import io.openliberty.mcp.internal.tools.ToolListRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;

@ApplicationScoped
public class ToolListRequestParser {

    private Jsonb jsonb;

    // CDI no-op constructor
    ToolListRequestParser() {}

    @Inject
    public ToolListRequestParser(@McpInternal Jsonb jsonb) {
        this.jsonb = jsonb;
    }

    public ToolListRequest parse(McpOperationRequest opRequest) {
        ToolListRequestTO requestTO = jsonb.fromJson(opRequest.args().toString(), ToolListRequestTO.class);

        String cursor = normalizeCursor(requestTO.cursor());

        return new ToolListRequest(opRequest, cursor);
    }

    private static String normalizeCursor(String cursor) {
        if (cursor != null && !cursor.isEmpty()) {
            return cursor;
        } else {
            return null;
        }
    }
}
