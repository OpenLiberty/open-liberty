/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.rest.handler.helper;

/**
 * This error is thrown by the RESTHandlerContainer when an internal error is encountered by a RESTHandler.
 * 
 * @ibm-spi
 */
public class RESTHandlerInternalError extends RuntimeException {

    private static final long serialVersionUID = -3647481857680022528L;

    private int statusCode = 500;

    public RESTHandlerInternalError(Exception e) {
        super(e);
    }

    public RESTHandlerInternalError(String msg) {
        super(msg);
    }

    public void setStatusCode(int code) {
        statusCode = code;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
