/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.exceptions.jsonrpc;

import java.io.Serial;
import java.util.List;

/**
 *
 */
public class MCPRequestValidationException extends JSONRPCException {
    @Serial
    private static final long serialVersionUID = -62283199421949679L;

    public MCPRequestValidationException(List<String> errors) {
        super(JSONRPCErrorCode.INVALID_REQUEST, errors);
    }
}
