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
 * This error is thrown by the RESTHandlerContainer when a particular HTTP method is not allowed.
 * 
 * @ibm-spi
 */
public class RESTHandlerMethodNotAllowedError extends RuntimeException {

    private static final long serialVersionUID = -3647481857680022528L;

    private final int statusCode = 405; //"Method Not Allowed"
    private final String allowedMethods;

    public RESTHandlerMethodNotAllowedError(String allowedMethods) {
        super();
        this.allowedMethods = allowedMethods;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

}
