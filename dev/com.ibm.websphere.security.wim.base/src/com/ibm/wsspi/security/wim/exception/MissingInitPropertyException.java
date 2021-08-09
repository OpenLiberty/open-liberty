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
 *
 * Initialization exception specifying that the initialization failed because an expected configuration
 * property was not found.
 */
public class MissingInitPropertyException extends InitializationException {

    private static final long serialVersionUID = 9005324454219277762L;

    /**
     * Creates the Missing Initialization Property Exception
     */
    public MissingInitPropertyException() {
        super();
    }

    /**
     * Creates the Missing Initialization Property Exception
     *
     * @param message The message or message key of the exception.
     */
    public MissingInitPropertyException(String key, String message) {
        super(key, message);
    }

    /**
     * @param cause
     */
    public MissingInitPropertyException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public MissingInitPropertyException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

}
