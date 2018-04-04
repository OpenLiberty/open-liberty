/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.wim.exception;

public class EntityAlreadyExistsException extends WIMApplicationException {

    private static final long serialVersionUID = 454023319370639664L;

    /**
     * Constructs a EntityAlreadyExistsException with no message key, no detail message, and no cause.
     */
    public EntityAlreadyExistsException() {
        super();
    }

    /**
     * Constructs a EntityAlreadyExistsException with the specified message key and detail message.
     *
     * @param key The message key.
     * @param message The detail message.
     */
    public EntityAlreadyExistsException(String key, String message) {
        super(key, message);
    }

    /**
     * Constructs a EntityAlreadyExistsException with the specified cause.
     *
     * @param cause The cause.
     */
    public EntityAlreadyExistsException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a EntityAlreadyExistsException with the specified message key, detail message, and cause.
     *
     * @param key The message key.
     * @param message The detail message.
     * @param cause The cause.
     */
    public EntityAlreadyExistsException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

}
