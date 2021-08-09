/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal.exception;

import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * This exception is used internally by the SSL channel to pass information
 * along that the results of a decryption indicate that the SSL session
 * as terminated.
 */
public class SessionClosedException extends ChannelException {

    /** Serialization ID string */
    private static final long serialVersionUID = 2648809003861385674L;

    /**
     * Constructor.
     * 
     * @param message
     */
    public SessionClosedException(String message) {
        super(message);
    }

}
