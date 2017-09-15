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
 * This exception is thrown if the channel name is equal to that of another
 * channels or is invlaid.
 */
public class InvalidChannelNameException extends ChannelException {

    /** Serialization ID string */
    private static final long serialVersionUID = 8153822118205179608L;

    /**
     * Constructor with an exception message.
     * 
     * @param message
     */
    public InvalidChannelNameException(String message) {
        super(message);
    }

    /**
     * Constructor with no message or cause.
     */
    public InvalidChannelNameException() {
        super();
    }

    /**
     * Constructor with an exception message and a cause.
     * 
     * @param message
     * @param cause
     */
    public InvalidChannelNameException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with a cause but no message.
     * 
     * @param cause
     */
    public InvalidChannelNameException(Throwable cause) {
        super(cause);
    }
}
