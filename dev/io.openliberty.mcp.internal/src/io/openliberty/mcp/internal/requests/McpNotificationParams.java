/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import jakarta.json.bind.annotation.JsonbNillable;

@JsonbNillable(value = false)
public class McpNotificationParams {

    //Cancelled Notification params
    private McpRequestId requestId;
    private String reason;

    public McpRequestId getRequestId() {
        return requestId;
    }

    public void setRequestId(McpRequestId requestId) {
        this.requestId = requestId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
