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
 **/
public class WIMException extends Exception {

    private static final long serialVersionUID = 2213794407328217976L;

    private String messageKey = null;

    /**
     * Default Constructor
     **/
    public WIMException() {
        super();
    }

    /**
     * Creates the WIMException.
     *
     * @param message The message or message key of the exception.
     **/
    public WIMException(String key, String message) {
        super(message);
        messageKey = key;
    }

    public WIMException(String message) {
        super(message);
    }

    /**
     * Creates the WIMException.
     *
     * @param cause The cause of the exception.
     **/
    public WIMException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates the WIMException.
     *
     * @param message The error message.
     * @param cause The cause of the exception.
     **/
    public WIMException(String key, String message, Throwable cause) {
        super(message, cause);
        messageKey = key;
    }

    /**
     * Return the message key.
     **/
    public String getMessageKey() {
        return messageKey;
    }
}