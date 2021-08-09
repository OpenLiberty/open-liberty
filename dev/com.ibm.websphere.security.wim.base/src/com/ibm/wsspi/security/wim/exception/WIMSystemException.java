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
 * A generic vmm application exception to indicate to the caller
 * that there was a problem with the current request due to incorrect inputs
 * from the caller.
 **/
public class WIMSystemException extends WIMException {

    private static final long serialVersionUID = -9080586676695903077L;

    /**
     * Creates the virtual member manager system level exception.
     */
    public WIMSystemException() {
        super();
    }

    /**
     * Creates the WIMSystemException.
     *
     * @param message The message or message key of the exception.
     **/
    public WIMSystemException(String key, String message) {
        super(key, message);
    }

    /**
     * Creates the WIMSystemException.
     *
     * @param cause The cause of the exception.
     **/
    public WIMSystemException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates the WIMSystemException.
     *
     * @param cause The cause of the exception.
     **/
    public WIMSystemException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

}