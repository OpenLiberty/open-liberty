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
 * Discriminators added to a running DiscriminationProcess or discrimination
 * had errors during execution.
 * 
 */
public class DiscriminationProcessException extends ChainException {

    /** Serialization ID string */
    private static final long serialVersionUID = -3060883482540305521L;

    /**
     * Constructor with an exception message.
     * 
     * @param message
     */
    public DiscriminationProcessException(String message) {
        super(message);
    }

    /**
     * Constructor with no message or cause.
     */
    public DiscriminationProcessException() {
        super();
    }

    /**
     * Constructor with an exception message and a cause.
     * 
     * @param message
     * @param cause
     */
    public DiscriminationProcessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with no message but with a cause.
     * 
     * @param cause
     */
    public DiscriminationProcessException(Throwable cause) {
        super(cause);
    }
}
