/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.rest.internal.resources;

/**
 * The exception contains an httpResponseCode. RequestHandlers may throw 
 * this exception and the dispatcher shall send an error response using
 * the exception's httpResponseCode and message.
 */
public class RequestException extends Exception {
    private int httpResponseCode;
    public RequestException(int httpResponseCode, String msg) {
        super(msg);
        this.httpResponseCode = httpResponseCode;
    }
    public RequestException(int httpResponseCode, String msg, Exception initCause) {
        super(msg, initCause);
        this.httpResponseCode = httpResponseCode;
    }
    public int getHttpResponseCode() {
        return httpResponseCode;
    }
}