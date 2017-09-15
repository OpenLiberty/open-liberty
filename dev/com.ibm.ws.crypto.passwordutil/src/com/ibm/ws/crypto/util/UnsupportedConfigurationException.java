/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.util;

/**
 * Exception thrown when the password provided for decoding is invalid.
 */
public class UnsupportedConfigurationException extends Exception {

    private static final long serialVersionUID = -6976724223307570873L;

    /**
     * Create a new UnsupportedConfigurationException with an empty string description.
     */
    public UnsupportedConfigurationException() {
        super();
    }

    /**
     * Create a new UnsupportedConfigurationException with the associated string description.
     * 
     * @param message the String describing the exception.
     */
    public UnsupportedConfigurationException(String message) {
        super(message);
    }

    /**
     * Create a new CustomRegistryException with the string description and Throwable root cause.
     * 
     * @param message the String describing the exception.
     * @param cause the Throwable root cause.
     */
    public UnsupportedConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
