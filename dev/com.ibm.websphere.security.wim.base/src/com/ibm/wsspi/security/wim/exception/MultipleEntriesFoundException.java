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
 * vmm application exception to indicate to the caller that the multiple entries are found when only one entry is expected.
 */
public class MultipleEntriesFoundException extends WIMApplicationException {

    private static final long serialVersionUID = -5745701539262116645L;

    /**
     * Creates the Multiple Entries Found Exception
     */
    public MultipleEntriesFoundException() {
        super();
    }

    /**
     * Creates the Multiple Entries Found Exception
     *
     * @param message The message or message key of the exception.
     */
    public MultipleEntriesFoundException(String key, String message) {
        super(key, message);
    }

    /**
     * Creates the Multiple Entries Found Exception
     *
     * @param message The message or message key of the exception.
     * @param cause The cause of the exception.
     */
    public MultipleEntriesFoundException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

    /**
     * Creates the Multiple Entries Found Exception
     *
     * @param cause The cause of the exception.
     */
    public MultipleEntriesFoundException(Throwable cause) {
        super(cause);
    }
}