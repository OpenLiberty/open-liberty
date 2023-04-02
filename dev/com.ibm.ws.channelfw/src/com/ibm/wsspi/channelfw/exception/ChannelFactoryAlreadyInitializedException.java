/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.channelfw.exception;

/**
 * {@link com.ibm.wsspi.channelfw.ChannelFactory} implementations
 * should throw this if they get initialized more than once.
 * 
 */
public class ChannelFactoryAlreadyInitializedException extends ChannelFactoryException {

    /** Serialization ID string */
    private static final long serialVersionUID = 6325483035210549501L;

    /**
     * Constructor with an exception message.
     * 
     * @param message
     */
    public ChannelFactoryAlreadyInitializedException(String message) {
        super(message);
    }

    /**
     * Constructor with no message or cause.
     */
    public ChannelFactoryAlreadyInitializedException() {
        super();
    }

    /**
     * Constructor with an exception message and a cause.
     * 
     * @param message
     * @param cause
     */
    public ChannelFactoryAlreadyInitializedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Contructor with a cause but no exception message.
     * 
     * @param cause
     */
    public ChannelFactoryAlreadyInitializedException(Throwable cause) {
        super(cause);
    }

}
