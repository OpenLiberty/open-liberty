/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.crypto;

/**
 * Exception thrown when PQC (Post-Quantum Cryptography) operations fail.
 */
public class PQCException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new PQCException with the specified detail message.
     *
     * @param message the detail message
     */
    public PQCException(String message) {
        super(message);
    }

    /**
     * Constructs a new PQCException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public PQCException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Made with Bob
