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
 * This is an exception that can be thrown by a channel when it is unable to
 * start,
 * but thinks that waiting for some period of time and retrying may result in
 * success.
 * A good example of this is when device side channels are started soon after
 * they
 * are stopped. If a bind error takes place because the socket is still tied up
 * in
 * its close down processing, then an exception like this can be thrown to flag
 * the
 * caller that the socket may be freeing up shortly.
 */
public class RetryableChannelException extends ChannelException {

    /** Serialization ID string */
    private static final long serialVersionUID = 4611158931491249843L;

    /**
     * Constructor with an exception message.
     * 
     * @param message
     */
    public RetryableChannelException(String message) {
        super(message);
    }

    /**
     * Constructor with no message or cause.
     */
    public RetryableChannelException() {
        super();
    }

    /**
     * Constructor with an exception message and a cause.
     * 
     * @param message
     * @param cause
     */
    public RetryableChannelException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with no message but with a cause.
     * 
     * @param cause
     */
    public RetryableChannelException(Throwable cause) {
        super(cause);
    }

}