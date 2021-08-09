/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.io.async;

import java.io.IOException;

/**
 * Checked exception thrown when a problem occurs scheduling the asynchronous call, not a problem with the call itself.
 * <p>
 * If a call-out is successfully made the results are always sent back in a future (the future may wrap an exception that indicates a
 * problem with the underlying operation). Problems of his type means that the underlying operating system was unwilling to accept the
 * asynchronous request, or that an internal error occurred.
 * </p>
 */
public class AsyncException extends IOException {
    // required SUID since this is serializable
    private static final long serialVersionUID = 5894453373641317981L;

    static final int AIO_INTERNAL_ERROR = -99;
    private final int errorCode;
    private final String platformMessage;

    /*
     * This constructor (java.lang.String, int) is called in the native code to create instances of the exception when there are problems in
     * the native code. DO NOT MODIFY THIS CONSTRUCTOR without making a corresponding change to the native code.
     */
    // IMPROVEMENT: we should get rid of this constructor, and just have the native code
    // put everything it needs into the exception string
    AsyncException(String message, String platformMessage, int errorCode) {
        super(message);

        this.errorCode = errorCode;
        this.platformMessage = platformMessage;
    }

    /**
     * Create an exception that represents an internal error.
     * 
     * @param message
     *            a String message suitable for display to the user.
     */
    public AsyncException(String message) {
        super(message);

        this.errorCode = AIO_INTERNAL_ERROR;
        this.platformMessage = AsyncProperties.aio_internal_error;
    }

    /**
     * Answer a representation of the receiver suitable for displaying to a user.
     * 
     * @see java.lang.Throwable#toString()
     */
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append(this.getClass().getName());
        buffer.append("("); //$NON-NLS-1$
        buffer.append(getLocalizedMessage());
        buffer.append(", ["); //$NON-NLS-1$

        if (errorCode == AIO_INTERNAL_ERROR) {
            buffer.append("Internal Error"); //$NON-NLS-1$
        } else {
            buffer.append(platformMessage.trim());
            buffer.append(", rc="); //$NON-NLS-1$
            buffer.append(errorCode);
        }

        buffer.append("])"); //$NON-NLS-1$

        return buffer.toString();
    }
}