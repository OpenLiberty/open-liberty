/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.http;

public class HttpResponseNot200Exception extends Exception {

    private static final long serialVersionUID = 1L;

    private String url;
    private int statusCode;
    private String errMsg;
    
    public HttpResponseNot200Exception(String url, int statusCode, String errMsg) {
        this.url = url;
        this.statusCode = statusCode;
        this.errMsg = errMsg;
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrMsg() {
        return errMsg;
    }

}
