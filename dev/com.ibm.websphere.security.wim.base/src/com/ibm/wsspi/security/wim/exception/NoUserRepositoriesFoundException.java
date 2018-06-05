/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 * Exception received when a request to do a user registry or repository is made
 * and there are no user registries or repositories configured.
 */
public class NoUserRepositoriesFoundException extends WIMApplicationException {

    private static final long serialVersionUID = -1653887434897158146L;

    /**
     * Constructs a NoUserRepositoriesFoundException with no message key, no detail message, and no cause.
     */
    public NoUserRepositoriesFoundException() {
        super();
    }

    /**
     * Constructs a NoUserRepositoriesFoundException with the specified message key and detail message.
     *
     * @param key The message key.
     * @param message The detail message.
     */
    public NoUserRepositoriesFoundException(String key, String message) {
        super(key, message);
    }

    /**
     * Constructs a NoUserRepositoriesFoundException with the specified message key, detail message, and cause.
     *
     * @param key The message key.
     * @param message The detail message.
     * @param cause The cause.
     */

    public NoUserRepositoriesFoundException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

    /**
     * Constructs a NoUserRepositoriesFoundException with the specified cause.
     *
     * @param cause The cause.
     */
    public NoUserRepositoriesFoundException(Throwable cause) {
        super(cause);
    }
}