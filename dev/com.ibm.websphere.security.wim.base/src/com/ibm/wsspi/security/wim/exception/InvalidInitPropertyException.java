/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.wim.exception;

public class InvalidInitPropertyException extends WIMApplicationException {

    private static final long serialVersionUID = 7520481823714145286L;

    /**
     * Constructs a InvalidInitPropertyException with no message key, no detail message, and no cause.
     */
    public InvalidInitPropertyException() {
        super();
    }

    /**
     * Constructs a InvalidInitPropertyException with the specified message key and detail message.
     *
     * @param key The message key.
     * @param message The detail message.
     */
    public InvalidInitPropertyException(String key, String message) {
        super(key, message);
    }

    /**
     * Constructs a InvalidInitPropertyException with the specified cause.
     *
     * @param cause The cause.
     */
    public InvalidInitPropertyException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a InvalidInitPropertyException with the specified message key, detail message, and cause.
     *
     * @param key The message key.
     * @param message The detail message.
     * @param cause The cause.
     */
    public InvalidInitPropertyException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }
}
