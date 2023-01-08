/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.security.common.http;

abstract class AbstractHttpResponseException extends Exception {

    private static final long serialVersionUID = 1L;

    private String url;
    private int statusCode;
    private String nlsMessage;
    
    public AbstractHttpResponseException(String url, int statusCode, String nlsMessage) {
        this(url, statusCode, nlsMessage, null);
    }

    public AbstractHttpResponseException(String url, int statusCode, String nlsMessage, Exception cause) {
        super(cause);
        this.url = url;
        this.statusCode = statusCode;
        this.nlsMessage = nlsMessage;
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getNlsMessage() {
        return nlsMessage;
    }

}
