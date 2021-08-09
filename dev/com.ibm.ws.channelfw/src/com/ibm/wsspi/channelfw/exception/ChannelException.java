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
 * related to channels.
 */
public class ChannelException extends ChannelFrameworkException {

    /** Serialization ID string */
    private static final long serialVersionUID = 4309702246400782423L;

    private boolean suppressFFDC = false;

    /**
     * Constructor.
     * 
     * @param message
     */
    public ChannelException(String message) {
        super(message);
    }

    /**
     * Constructor.
     */
    public ChannelException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param message
     * @param cause
     */
    public ChannelException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor.
     * 
     * @param cause
     */
    public ChannelException(Throwable cause) {
        super(cause);
    }

    public void suppressFFDC(boolean suppress) {
        suppressFFDC = suppress;
    }

    public boolean suppressFFDC() {
        return suppressFFDC;
    }

}
