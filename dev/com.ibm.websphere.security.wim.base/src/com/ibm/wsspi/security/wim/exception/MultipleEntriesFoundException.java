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
 * VMM application exception to indicate to the caller that the multiple entries are found when only one entry is expected.
 */
public class MultipleEntriesFoundException extends WIMApplicationException {

    private static final long serialVersionUID = -5745701539262116645L;

    /**
     * Constructs a MultipleEntriesFoundException with no message key, no detail message, and no cause.
     */
    public MultipleEntriesFoundException() {
        super();
    }

    /**
     * Constructs a MultipleEntriesFoundException with the specified message key and detail message.
     *
     * @param key The message key.
     * @param message The detail message.
     */
    public MultipleEntriesFoundException(String key, String message) {
        super(key, message);
    }

    /**
     * Constructs a MultipleEntriesFoundException with the specified message key, detail message, and cause.
     *
     * @param key The message key.
     * @param message The detail message.
     * @param cause The cause.
     */
    public MultipleEntriesFoundException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

    /**
     * Constructs a MultipleEntriesFoundException with the specified cause.
     *
     * @param cause The cause.
     */
    public MultipleEntriesFoundException(Throwable cause) {
        super(cause);
    }
}