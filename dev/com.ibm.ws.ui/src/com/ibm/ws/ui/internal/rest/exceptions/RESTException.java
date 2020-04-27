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

import com.ibm.ws.ui.internal.rest.HTTPConstants;

/**
 * <p>Base RESTException used to indicate that our RESTHandler encountered
 * non-success (non-200) flow. This method should be thrown by the doX methods
 * when an error path is encountered.</p>
 * 
 * <p>The status code set in here will influence the response a client receives.</p>
 */
public class RESTException extends Exception implements HTTPConstants {
    private static final long serialVersionUID = 1L;
    private final int status;
    private final String contentType;
    private final Object payload;

    /**
     * Constructs a generic RESTException.
     * 
     * @param status The HTTP status code to return.
     */
    public RESTException(final int status) {
        this.status = status;
        this.contentType = null;
        this.payload = null;
    }

    /**
     * Constructs a RESTException with a response payload.
     * 
     * @param status The HTTP status code to return.
     * @param contentType The media type of the payload
     * @param payload The payload to set in the response
     */
    public RESTException(final int status, final String contentType, final Object payload) {
        this.status = status;
        this.contentType = contentType;
        this.payload = payload;
    }

    /**
     * Retrieve the HTTP status code.
     * 
     * @return The HTTP status code
     */
    public final int getStatus() {
        return status;
    }

    /**
     * Retrieve the content type of the payload.
     * 
     * @return The payload's content type.
     */
    public final String getContentType() {
        return contentType;
    }

    /**
     * Retrieve the payload to set in the response.
     * 
     * @return The payload
     */
    public final Object getPayload() {
        return payload;
    }
}
