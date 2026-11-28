/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa;

/**
 * Exception thrown when LTPA keystore operations fail.
 * 
 * This exception is used for errors related to:
 * <ul>
 *   <li>Loading or saving LTPA keystores (PKCS12)</li>
 *   <li>Accessing keys from keystores</li>
 *   <li>Invalid keystore passwords</li>
 *   <li>Corrupted or invalid keystore files</li>
 *   <li>Missing required keys in keystores</li>
 * </ul>
 */
public class LTPAKeystoreException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new LTPA keystore exception with the specified detail message.
     * 
     * @param message the detail message
     */
    public LTPAKeystoreException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new LTPA keystore exception with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public LTPAKeystoreException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new LTPA keystore exception with the specified cause.
     * 
     * @param cause the cause of this exception
     */
    public LTPAKeystoreException(Throwable cause) {
        super(cause);
    }
}

// Made with Bob
