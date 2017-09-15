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
 * This exception class will be the base for creating many exception classes
 * related to channel factories.
 * 
 */
public class ChannelFactoryException extends ChannelException {

    /** Serialization ID string */
    private static final long serialVersionUID = 2493186982674438118L;

    /**
     * Constructor.
     * 
     * @param message
     */
    public ChannelFactoryException(String message) {
        super(message);
    }

    /**
     * Constructor.
     */
    public ChannelFactoryException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param message
     * @param cause
     */
    public ChannelFactoryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor.
     * 
     * @param cause
     */
    public ChannelFactoryException(Throwable cause) {
        super(cause);
    }
}
