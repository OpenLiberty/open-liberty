/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.rest.exceptions;

/**
 * <p>(HTTP 400) RESTException used to indicate that the client request was bad.</p>
 */
public class BadRequestException extends RESTException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a RESTException to indicate that the request was (somehow) bad.
     */
    public BadRequestException() {
        super(HTTP_BAD_REQUEST);
    }

    /**
     * Constructs a RESTException to indicate that the requestwas (somehow) bad,
     * and provides a payload with a description as to what occurred.
     * 
     * @param contentType The media type of the payload
     * @param payload The payload to set in the response
     */
    public BadRequestException(String contentType, Object payload) {
        super(HTTP_BAD_REQUEST, contentType, payload);
    }

}
