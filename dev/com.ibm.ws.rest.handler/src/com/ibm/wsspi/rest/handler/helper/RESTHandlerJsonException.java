/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.rest.handler.helper;

public class RESTHandlerJsonException extends RuntimeException {

    private static final long serialVersionUID = -3647481857680022528L;

    private int statusCode;

    private boolean isMessageContentJson;

    public RESTHandlerJsonException(Exception e, int statusCode) {
        super(e);
        this.statusCode = statusCode;
    }

    public RESTHandlerJsonException(String msg, int statusCode) {
        super(msg);
        this.statusCode = statusCode;
    }

    public RESTHandlerJsonException(Exception e, int statusCode, boolean isMessageContentJSON) {
        super(e);
        this.statusCode = statusCode;
        this.isMessageContentJson = isMessageContentJSON;
    }

    public RESTHandlerJsonException(String msg, int statusCode, boolean isMessageContentJSON) {
        super(msg);
        this.statusCode = statusCode;
        this.isMessageContentJson = isMessageContentJSON;
    }

    public void setStatusCode(int code) {
        statusCode = code;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isMessageContentJSON() {
        return isMessageContentJson;
    }
}
