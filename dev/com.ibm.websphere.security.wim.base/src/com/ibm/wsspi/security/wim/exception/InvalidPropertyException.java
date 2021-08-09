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
 * Member Manager application exception to indicate that the attribute specified by the caller is invalid.
 */
public class InvalidPropertyException extends WIMApplicationException {

    private static final long serialVersionUID = 1960694189457977904L;

    /**
     * Creates the Invalid Attribute Exception
     */
    public InvalidPropertyException() {
        super();
    }

    /**
     * Creates the Invalid Attribute Exception
     *
     * @param message The message or message key of the exception.
     */
    public InvalidPropertyException(String key, String message) {
        super(key, message);
    }

    /**
     * Creates the Invalid Attribute Exception
     *
     * @param message The message or message key of the exception.
     * @param cause The cause of the exception.
     */
    public InvalidPropertyException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

    /**
     * Creates the Invalid Attribute Exception
     *
     * @param cause The cause of the exception.
     */
    public InvalidPropertyException(Throwable cause) {
        super(cause);
    }
}