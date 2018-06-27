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
 * Exception while processing memory information.
 */
public class MemoryInformationException extends Exception {
    private static final long serialVersionUID = -2279626416148709559L;

    /**
     * {@inheritDoc}
     */
    public MemoryInformationException() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public MemoryInformationException(String message) {
        super(message);
    }

    /**
     * {@inheritDoc}
     */
    public MemoryInformationException(Throwable cause) {
        super(cause);
    }

    /**
     * {@inheritDoc}
     */
    public MemoryInformationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     */
    public MemoryInformationException(String message, Throwable cause, boolean enableSuppression,
                                      boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}