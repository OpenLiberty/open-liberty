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
 * A generic vmm application exception to indicate to the caller
 * that there was a problem with the current request due to incorrect inputs
 * from the caller.
 **/
public class WIMSystemException extends WIMException {

    private static final long serialVersionUID = -9080586676695903077L;

    /**
     * Constructs a WIMSystemException with no message key, no detail message, and no cause.
     */
    public WIMSystemException() {
        super();
    }

    /**
     * Constructs a WIMSystemException with the specified message key and detail message.
     *
     * @param key The message key.
     * @param message The detail message.
     */
    public WIMSystemException(String key, String message) {
        super(key, message);
    }

    /**
     * Constructs a WIMSystemException with the specified cause.
     *
     * @param cause The cause of the exception.
     */
    public WIMSystemException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a WIMSystemException with the specified message key, detail message, and cause.
     *
     * @param key The message key
     * @param message The detail message.
     * @param cause The cause of the exception.
     */
    public WIMSystemException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

}