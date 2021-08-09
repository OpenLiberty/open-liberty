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
 * This exception is thrown if an invalid weight is given in a channel
 * configuration.
 */
public class InvalidWeightException extends ChannelException {

    /** Serialization ID string */
    private static final long serialVersionUID = -3045401376510042751L;

    /**
     * Constructor with an exception message.
     * 
     * @param message
     */
    public InvalidWeightException(String message) {
        super(message);
    }

    /**
     * Constructor with no message or cause.
     */
    public InvalidWeightException() {
        super();
    }

    /**
     * Contructor with an exception message and a cause.
     * 
     * @param message
     * @param cause
     */
    public InvalidWeightException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with no exception message but with a cause.
     * 
     * @param cause
     */
    public InvalidWeightException(Throwable cause) {
        super(cause);
    }

}
