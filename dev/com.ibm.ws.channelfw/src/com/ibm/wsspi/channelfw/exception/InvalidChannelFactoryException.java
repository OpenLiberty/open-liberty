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
 * Channel Factory is invalid or otherwise not found.
 */
public class InvalidChannelFactoryException extends ChannelFactoryException {

    /** Serialization ID string */
    private static final long serialVersionUID = 1592448367216093522L;

    /**
     * Constructor for InvalidChannelFactoryException.
     * 
     * @param message
     */
    public InvalidChannelFactoryException(String message) {
        super(message);
    }

    /**
     * Constructor with no message or cause.
     */
    public InvalidChannelFactoryException() {
        super();
    }

    /**
     * Constructor with an exception message and a cause.
     * 
     * @param message
     * @param cause
     */
    public InvalidChannelFactoryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with no message but with a cause.
     * 
     * @param cause
     */
    public InvalidChannelFactoryException(Throwable cause) {
        super(cause);
    }

}
