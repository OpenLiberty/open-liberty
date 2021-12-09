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
 * <p>(HTTP 415) RESTException used to indicate that the requested media type is not supported.</p>
 */
public class MediaTypeNotSupportedException extends RESTException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a RESTException to indicate that the HTTP media type is not supported.
     */
    public MediaTypeNotSupportedException() {
        super(HTTP_MEDIA_TYPE_NOT_SUPPORTED);
    }
}
