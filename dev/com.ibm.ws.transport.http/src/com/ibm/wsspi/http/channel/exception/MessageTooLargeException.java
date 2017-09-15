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

/**
 * As an incoming message is being read, it may exceed the configured limit for
 * an acceptable message size. If an application channel is involved at this
 * point, then the body request APIs that discovered the excessive messsage
 * size will use this exception to notify the caller of the error. A typical
 * HTTP error response at this point would be the "413 Request Entity Too Large"
 * status code.
 */
public class MessageTooLargeException extends IllegalHttpBodyException {

    /** Serialization ID value */
    static final private long serialVersionUID = -6773650949819504802L;

    /**
     * Constructor for this exception
     * 
     * @param msg
     */
    public MessageTooLargeException(String msg) {
        super(msg);
    }

}