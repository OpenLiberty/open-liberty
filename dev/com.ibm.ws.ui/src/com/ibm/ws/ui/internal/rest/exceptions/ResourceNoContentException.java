/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
 *
 */
public class ResourceNoContentException extends RESTException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a RESTException to indicate that there is no additional content to send in
     * the payload body.
     */
    public ResourceNoContentException() {
        super(HTTP_NO_CONTENT);
    }

}
