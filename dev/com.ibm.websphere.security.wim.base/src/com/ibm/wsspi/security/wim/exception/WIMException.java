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
 * Base class representing a virtual member manager exception. This can be extended to create
 * component specific exceptions.
 *
 */
public class WIMException extends Exception {

    private static final long serialVersionUID = 2213794407328217976L;

    private String messageKey = null;

    /**
     * Constructs a WIMException with no message key, no detail message, and no cause.
     */
    public WIMException() {
        super();
    }

    /**
     * Constructs a WIMException with the specified message key and detail message.
     *
     * @param key The message key.
     * @param message The detail message.
     */
    public WIMException(String key, String message) {
        super(message);
        messageKey = key;
    }

    /**
     * Constructs a WIMException with the specified detail message.
     *
     * @param message The detail message.
     */
    public WIMException(String message) {
        super(message);
    }

    /**
     * Constructs a WIMException with the specified cause.
     *
     * @param cause The cause of the exception.
     */
    public WIMException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a WIMException with the specified message key, detail message, and cause.
     *
     * @param key The message key
     * @param message The detail message.
     * @param cause The cause of the exception.
     */
    public WIMException(String key, String message, Throwable cause) {
        super(message, cause);
        messageKey = key;
    }

    /**
     * Return the message key.
     *
     * @return The message key.
     **/
    public String getMessageKey() {
        return messageKey;
    }
}