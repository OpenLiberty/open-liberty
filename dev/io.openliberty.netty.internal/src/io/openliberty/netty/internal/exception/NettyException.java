/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal.exception;

/**
 * This exception class will be the base for exception classes related to netty 
 * integrations.
 * 
 * Taken from {@link com.ibm.wsspi.channelfw.exception.ChannelException}
 */
public class NettyException extends Exception {

    private static final long serialVersionUID = 7351566603790105244L;

    private boolean suppressFFDC = false;

    /**
     * Constructor.
     * 
     * @param message
     */
    public NettyException(String message) {
        super(message);
    }

    /**
     * Constructor.
     */
    public NettyException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param message
     * @param cause
     */
    public NettyException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor.
     * 
     * @param cause
     */
    public NettyException(Throwable cause) {
        super(cause);
    }

    public void suppressFFDC(boolean suppress) {
        suppressFFDC = suppress;
    }

    public boolean suppressFFDC() {
        return suppressFFDC;
    }
}
