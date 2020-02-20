/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client;

import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class BadRequestException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(BadRequestException.class, TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    private static final long serialVersionUID = 1L;
    String errorMessage = null;
    int iStatusCode = 400;

    public BadRequestException(String message, int statusCode) {
        super(ACCESS_DENIED, message, null);
        this.errorMessage = message;
        this.iStatusCode = statusCode;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @return the iStatusCode
     */
    public int getStatusCode() {
        return iStatusCode;
    }
}
