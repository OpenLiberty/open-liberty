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
 * Exception thrown when an error occurred while decrypting the data.
 * @ibm-spi
 */
public class PasswordDecryptException extends Exception {

    private static final long serialVersionUID = 7895710950547149371L;

    /**
     * Constructs an PasswordDecryptException with no detail message.
     */
    public PasswordDecryptException() {
        super();
    }

    /**
     * Constructs an PasswordDecryptException with the specified detail message.
     * 
     * @param message the detail message.
     */
    public PasswordDecryptException(String message) {
        super(message);
    }

    /**
     * Constructs an PasswordDecryptException with the specified cause.
     *
     * @param cause the cause.
     */
    public PasswordDecryptException(Exception cause) {
        super(cause);
    }

    /**
     * Constructs an PasswordDecryptException with the specified message and cause.
     * 
     * @param message the detail message.
     * @param cause the cause.
     */
    public PasswordDecryptException(String message, Exception cause) {
        super(message, cause);
    }

}
