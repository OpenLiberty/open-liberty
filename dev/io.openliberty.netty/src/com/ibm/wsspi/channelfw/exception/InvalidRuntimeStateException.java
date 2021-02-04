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
package com.ibm.wsspi.channelfw.exception;

/**
 * Exception indicating a particular channel framework API call was made during
 * in invalid runtime state for the framework.
 * 
 */
public class InvalidRuntimeStateException extends ChainException {

    /** Serialization ID string */
    private static final long serialVersionUID = 400959244233494560L;

    /**
     * Constructor.
     * 
     * @param message
     */
    public InvalidRuntimeStateException(String message) {
        super(message);
    }

    /**
     * Constructor.
     */
    public InvalidRuntimeStateException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param message
     * @param cause
     */
    public InvalidRuntimeStateException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor.
     * 
     * @param cause
     */
    public InvalidRuntimeStateException(Throwable cause) {
        super(cause);
    }

}
