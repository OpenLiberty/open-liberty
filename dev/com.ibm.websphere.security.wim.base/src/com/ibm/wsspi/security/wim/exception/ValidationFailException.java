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
 * vmm application exception to indicate that validation of the
 * the request failed.
 */
public class ValidationFailException extends WIMApplicationException {

    private static final long serialVersionUID = 3080618528019111415L;

    /**
     * Creates the Validation Fail Exception
     */
    public ValidationFailException() {
        super();
    }

    /**
     * Creates the Validation Fail Exception
     *
     * @param message The message or message key of the exception.
     */
    public ValidationFailException(String key, String message) {
        super(key, message);
    }

    /**
     * Creates the Validation Fail Exception
     *
     * @param message The message or message key of the exception.
     * @param cause The cause of the exception.
     */
    public ValidationFailException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

    /**
     * Creates the Validation Fail Exception
     *
     * @param cause The cause of the exception.
     */
    public ValidationFailException(Throwable cause) {
        super(cause);
    }
}