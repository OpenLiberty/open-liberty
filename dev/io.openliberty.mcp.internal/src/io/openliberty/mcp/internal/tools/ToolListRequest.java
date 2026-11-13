/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.tools;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.mcp.internal.requests.McpOperationRequest;

/**
 * A request to list tools
 *
 * @param request the parent operation request
 * @param cursorValue the cursor, or {@code null} if no cursor was provided
 */
@Trivial
public record ToolListRequest(McpOperationRequest request, String cursorValue) {

    public ToolListRequest {
        requireNonNull(request, "request is required");
    }

    /**
     * Gets the cursor
     *
     * @return the cursor, or an empty {@code Optional} if no cursor was sent
     */
    public Optional<String> cursor() {
        return Optional.ofNullable(cursorValue);
    }
}
