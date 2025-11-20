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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class JSONRPCException extends RuntimeException {
    private static final TraceComponent tc = Tr.register(JSONRPCException.class);
    private static final long serialVersionUID = 1L;
    private JSONRPCErrorCode errorCode;
    private Object data;

    public JSONRPCException(JSONRPCErrorCode errorCode, Object data) {
        this.errorCode = errorCode;
        this.data = data;
    }

    /**
     * @return the errorCode
     */
    public JSONRPCErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * @return the data
     */
    public Object getData() {
        return data;
    }

}
