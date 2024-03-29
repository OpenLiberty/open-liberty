/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
package com.ibm.wsspi.rest.handler.helper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This error is thrown by the RESTHandlerContainer when a request's media type is not supported.
 * 
 * @ibm-spi
 */
public class RESTHandlerUnsupportedMediaType extends RuntimeException {

    private static final long serialVersionUID = -3647481857680022528L;

    private static final TraceComponent tc = Tr.register(RESTHandlerUnsupportedMediaType.class);

    private int statusCode = 415; // Unsupported Media Type

    public RESTHandlerUnsupportedMediaType(String mediaType) {
        super(Tr.formatMessage(tc, "UNSUPPORTED_MEDIA_TYPE", mediaType));
    }

    public void setStatusCode(int code) {
        statusCode = code;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
