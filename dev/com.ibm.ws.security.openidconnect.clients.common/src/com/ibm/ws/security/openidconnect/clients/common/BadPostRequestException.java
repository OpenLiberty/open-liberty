/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

public class BadPostRequestException extends Exception {

    private static final long serialVersionUID = 1L;
    String errorMessage = null;
    int iStatusCode = 400;

    public BadPostRequestException(String message, int statusCode) {
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
