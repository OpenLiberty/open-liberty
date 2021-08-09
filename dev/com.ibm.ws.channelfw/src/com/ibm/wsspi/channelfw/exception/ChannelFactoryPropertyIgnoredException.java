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
 * This exception is thrown by channel factory implementations when a
 * property is being set that the channel factory cannot handle.
 */
public class ChannelFactoryPropertyIgnoredException extends ChannelFactoryException {

    /** Serialization ID string */
    private static final long serialVersionUID = -6471004761179894209L;

    /**
     * Constructor.
     * 
     * @param message
     */
    public ChannelFactoryPropertyIgnoredException(String message) {
        super(message);
    }

    /**
     * Constructor.
     */
    public ChannelFactoryPropertyIgnoredException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param message
     * @param cause
     */
    public ChannelFactoryPropertyIgnoredException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor.
     * 
     * @param cause
     */
    public ChannelFactoryPropertyIgnoredException(Throwable cause) {
        super(cause);
    }
}