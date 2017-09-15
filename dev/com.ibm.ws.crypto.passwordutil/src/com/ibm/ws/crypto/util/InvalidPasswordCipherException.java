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
package com.ibm.ws.crypto.util;

/**
 * Exception thrown when a password provided is invalid.
 */
public class InvalidPasswordCipherException extends Exception {

    private static final long serialVersionUID = -4137060197629244051L;

    /**
     * Constructor.
     */
    public InvalidPasswordCipherException() {
        super();
    }

    /**
     * @param string
     */
    public InvalidPasswordCipherException(String string) {
        super(string);
    }

}
