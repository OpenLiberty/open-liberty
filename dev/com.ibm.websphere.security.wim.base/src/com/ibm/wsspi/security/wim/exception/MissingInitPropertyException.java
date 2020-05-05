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
 * Initialization exception specifying that the initialization failed because an expected configuration
 * property was not found.
 */
public class MissingInitPropertyException extends InitializationException {

    private static final long serialVersionUID = 9005324454219277762L;

    /**
     * Constructs a MissingInitPropertyException with no message key, no detail message, and no cause.
     */
    public MissingInitPropertyException() {
        super();
    }

    /**
     * Constructs a MissingInitPropertyException with the specified message key and detail message.
     *
     * @param key The message key.
     * @param message The detail message.
     */
    public MissingInitPropertyException(String key, String message) {
        super(key, message);
    }

    /**
     * Constructs a MissingInitPropertyException with the specified cause.
     *
     * @param cause The cause.
     */
    public MissingInitPropertyException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a MissingInitPropertyException with the specified message key, detail message, and cause.
     *
     * @param key The message key.
     * @param message The detail message.
     * @param cause The cause.
     */
    public MissingInitPropertyException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

}
