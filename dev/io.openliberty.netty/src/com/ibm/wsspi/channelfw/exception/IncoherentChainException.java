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
 * Chain is incoherent...otherwise the interfaces do not mesh correctly.
 */
public class IncoherentChainException extends ChainException {

    /** Serialization ID string */
    private static final long serialVersionUID = 6901609329105430273L;

    /**
     * Constructor with an exception message string.
     * 
     * @param message
     */
    public IncoherentChainException(String message) {
        super(message);
    }

    /**
     * Constructor with no message or cause.
     */
    public IncoherentChainException() {
        super();
    }

    /**
     * Constructor with an exception message string and a cause.
     * 
     * @param message
     * @param cause
     */
    public IncoherentChainException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with no exception message but with a cause.
     * 
     * @param cause
     */
    public IncoherentChainException(Throwable cause) {
        super(cause);
    }

}
