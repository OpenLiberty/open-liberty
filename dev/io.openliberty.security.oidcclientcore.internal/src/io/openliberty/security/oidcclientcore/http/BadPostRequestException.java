/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.http;

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
    @Override
    public String getMessage() {
        return errorMessage;
    }

    /**
     * @return the iStatusCode
     */
    public int getStatusCode() {
        return iStatusCode;
    }

}
