/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.wim.exception;

/**
 *
 * A generic vmm application exception to indicate to the caller
 * that there was a problem with the current request due to incorrect inputs
 * from the caller.
 **/
public class WIMApplicationException extends WIMException {

    private static final long serialVersionUID = -3611174431883761382L;

    /**
     * Creates the vmm application exception.
     */
    public WIMApplicationException() {
        super();
    }

    /**
     * Creates the WIMApplicationException.
     *
     * @param message The message or message key of the exception.
     **/
    public WIMApplicationException(String key, String message) {
        super(key, message);
    }

    /**
     * Creates the WIMApplicationException.
     *
     * @param cause The cause of the exception.
     **/
    public WIMApplicationException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates the WIMApplicationException.
     *
     * @param cause The cause of the exception.
     **/
    public WIMApplicationException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

}