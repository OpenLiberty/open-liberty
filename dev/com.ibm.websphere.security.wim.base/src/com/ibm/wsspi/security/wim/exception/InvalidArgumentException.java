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

public class InvalidArgumentException extends WIMApplicationException {

    private static final long serialVersionUID = -5472875166242746112L;

    /**
     * Constructs a InvalidArgumentException with no message key, no detail message, and no cause.
     */
    public InvalidArgumentException() {
        super();
    }

    /**
     * Constructs a InvalidArgumentException with the specified message key and detail message.
     *
     * @param key The message key.
     * @param message The detail message.
     */
    public InvalidArgumentException(String key, String message) {
        super(key, message);
    }

    /**
     * Constructs a InvalidArgumentException with the specified cause.
     *
     * @param cause The cause.
     */
    public InvalidArgumentException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a InvalidArgumentException with the specified message key, detail message, and cause.
     *
     * @param key The message key.
     * @param message The detail message.
     * @param cause The cause.
     */
    public InvalidArgumentException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

}
