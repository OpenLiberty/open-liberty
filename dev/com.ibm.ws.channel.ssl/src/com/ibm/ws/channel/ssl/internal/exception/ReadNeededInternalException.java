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
 * along that an additional read must be done in order to get enough data for
 * the SSL engine to run unencryption.
 */
public class ReadNeededInternalException extends ChannelException {

    /** Serialization ID string */
    private static final long serialVersionUID = -3236620232328367856L;

    /**
     * Constructor.
     * 
     * @param message
     */
    public ReadNeededInternalException(String message) {
        super(message);
    }

}
