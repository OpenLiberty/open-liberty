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
package com.ibm.ws.security.mp.jwt.error;

/**
 * Represents an exception while processing the request with micro profile jwt.
 *
 */
public class MpJwtProcessingException extends Exception {

    /**  */
    private static final long serialVersionUID = 1L;

    public MpJwtProcessingException(String message) {
        super(message);
    }

    public MpJwtProcessingException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
