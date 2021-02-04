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
 * This is a base exception class which will be extended by all
 * the other channel framework exceptions. The purpose of it is
 * to consolidate all the different types of the exceptions into
 * a single type for use in method signatures.
 */
public class ChannelFrameworkException extends Exception {

    /** Serialization ID string */
    private static final long serialVersionUID = 7351509803790105244L;

    /**
     * Constructor.
     * 
     * @param message
     */
    public ChannelFrameworkException(String message) {
        super(message);
    }

    /**
     * Constructor.
     */
    public ChannelFrameworkException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param message
     * @param cause
     */
    public ChannelFrameworkException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor.
     * 
     * @param cause
     */
    public ChannelFrameworkException(Throwable cause) {
        super(cause);
    }
}
