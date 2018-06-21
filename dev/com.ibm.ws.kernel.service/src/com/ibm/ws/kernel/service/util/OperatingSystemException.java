/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.util;

/**
 * Exception while performing operating system action.
 */
public class OperatingSystemException extends Exception {

    private static final long serialVersionUID = 6990206095591336940L;

    /**
     * {@inheritDoc}
     */
    public OperatingSystemException() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public OperatingSystemException(String message) {
        super(message);
    }

    /**
     * {@inheritDoc}
     */
    public OperatingSystemException(Throwable cause) {
        super(cause);
    }

    /**
     * {@inheritDoc}
     */
    public OperatingSystemException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     */
    public OperatingSystemException(String message, Throwable cause, boolean enableSuppression,
                                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}