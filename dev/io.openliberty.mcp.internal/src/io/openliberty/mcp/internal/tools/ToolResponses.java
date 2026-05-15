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

import org.mcpjava.server.Cancellation.OperationCancelledException;
import org.mcpjava.server.tools.ToolResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class ToolResponses {

    private static final TraceComponent tc = Tr.register(ToolResponses.class);

    /**
     * Create a ToolResponse for a business exception. The response will include the exception message.
     *
     * @param t the business exception
     * @return the tool response
     */
    public static ToolResponse createBusinessErrorResponse(Throwable t) {
        String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
        return ToolResponse.ofError(msg);
    }

    /**
     * Create a ToolResponse for a non-business exception.
     * The response will indicate an internal server error and the exception details will be logged.
     * Special handling for OperationCancelledException: returns a cancellation error response without logging.
     *
     * @param t the non-business exception
     * @param toolName the name of the tool
     * @return the tool response
     */
    public static ToolResponse createNonBusinessErrorResponse(Throwable t, String toolName) {
        // Handle OperationCancelledException specially - return error response but don't log
        if (t instanceof OperationCancelledException) {
            return ToolResponse.ofError("Operation was cancelled");
        }

        Tr.error(tc,
                 "CWMCM0010E.internal.server.error.detailed",
                 toolName,
                 t);
        return ToolResponse.ofError(Tr.formatMessage(tc, "internal.server.error"));
    }

}
