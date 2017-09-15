/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.security.crypto;

/**
 * Exception thrown when an error occurred while encrypting the data.
 * @ibm-spi
 */
public class PasswordEncryptException extends Exception {
    private static final long serialVersionUID = 2510989550436833115L;

    /**
     * Constructs an PasswordEncryptException with no message.
     */
    public PasswordEncryptException() {
        super();
    }

    /**
     * Constructs an PasswordEncryptException with the specified message.
     * 
     * @param message the detail message.
     */
    public PasswordEncryptException(String message) {
        super(message);
    }

    /**
     * Constructs an PasswordEncryptException with the specified cause.
     * 
     * @param cause the cause.
     */
    public PasswordEncryptException(Exception cause) {
        super(cause);
    }

    /**
     * Constructs an PasswordEncryptException with the specified message and cause.
     * 
     * @param message the detail message.
     * @param cause the cause.
     */
    public PasswordEncryptException(String message, Exception cause) {
        super(message, cause);
    }
}
