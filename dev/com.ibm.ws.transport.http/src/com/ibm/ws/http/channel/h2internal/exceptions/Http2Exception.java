/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal.exceptions;

import com.ibm.ws.http.channel.h2internal.Constants;

/**
 *
 */
public class Http2Exception extends Exception {

    private static final long serialVersionUID = -3862412104838742589L;

    int errorCode = Constants.NO_ERROR;
    String errorString = "NO_ERROR";
    boolean connectionError = true;

    public Http2Exception(String s) {
        super(s);
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public String getErrorString() {
        return this.errorString;
    }

    public void setConnectionError(boolean isConnectionError) {
        this.connectionError = isConnectionError;
    }

    public boolean isConnectionError() {
        return this.connectionError;
    }
}
