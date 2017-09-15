/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.ssl;

/**
 * @author IBM Corporation
 * @ibm-spi
 */
public class WSPKIException extends Exception {

    private static final long serialVersionUID = -1572688748868920424L;

    /**
     * Create a new WSPKIException with an empty description string.
     */
    public WSPKIException() {
        super();
    }

    /**
     * Create a new WSPKIException with the associated string description.
     * 
     * @param message
     *            String describing the exception.
     */
    public WSPKIException(String message) {
        super(message);
    }

    /**
     * Create a new WSPKIException with the associated string description and
     * cause.
     * 
     * @param message
     *            the String describing the exception.
     * @param cause
     */
    public WSPKIException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new WSPKIException with the cause
     * 
     * @param cause
     *            the throwable cause of the exception.
     */
    public WSPKIException(Throwable cause) {
        super(cause);
    }
}
