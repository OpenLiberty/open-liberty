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
 * Exception specifically with a chain group.
 * 
 */
public class ChainGroupException extends ChannelFrameworkException {

    /** Serialization ID string */
    private static final long serialVersionUID = -7819400101199566906L;

    /**
     * Constructor with an exception message.
     * 
     * @param message
     */
    public ChainGroupException(String message) {
        super(message);
    }

    /**
     * Constructor with no message or cause.
     */
    public ChainGroupException() {
        super();
    }

    /**
     * Constructor with an exception message and a cause.
     * 
     * @param message
     * @param cause
     */
    public ChainGroupException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with a cause but no message.
     * 
     * @param cause
     */
    public ChainGroupException(Throwable cause) {
        super(cause);
    }
}
