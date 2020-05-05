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
 * A generic WIM application exception to indicate to the caller
 * that there was a problem with the current request due to incorrect inputs
 * from the caller.
 */
public class WIMApplicationException extends WIMException {

    private static final long serialVersionUID = -3611174431883761382L;

    /**
     * Constructs a WIMApplicationException with no message key, no detail message, and no cause.
     */
    public WIMApplicationException() {
        super();
    }

    /**
     * Constructs a WIMApplicationException with the specified message key and detail message.
     *
     * @param key The message key.
     * @param message The message or message key of the exception.
     */
    public WIMApplicationException(String key, String message) {
        super(key, message);
    }

    /**
     * Creates the WIMApplicationException with the specified cause.
     *
     * @param cause The cause of the exception.
     */
    public WIMApplicationException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates the WIMApplicationException with the specified message key, message and cause.
     *
     * @param key The message key.
     * @param message The detail message.
     * @param cause The cause of the exception.
     */
    public WIMApplicationException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }
}