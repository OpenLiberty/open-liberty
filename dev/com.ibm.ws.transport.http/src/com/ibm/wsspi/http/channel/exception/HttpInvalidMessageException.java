/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel.exception;

import java.io.IOException;

/**
 * Exception that represents when an invalid message has been sent out such
 * that the connection is now "broken". Examples include when the content-length
 * does not match the actual number of bytes sent with the message, causing the
 * other end of the socket to be unable to properly read the body.
 */
public class HttpInvalidMessageException extends IOException {

    /** Serialization ID value */
    static final private long serialVersionUID = -899943671770861147L;

    /**
     * Constructor for this exception
     * 
     * @param msg
     */
    public HttpInvalidMessageException(String msg) {
        super(msg);
    }
}
