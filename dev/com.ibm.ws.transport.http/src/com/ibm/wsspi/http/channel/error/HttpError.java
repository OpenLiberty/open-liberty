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
package com.ibm.wsspi.http.channel.error;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.genericbnf.GenericKeys;
import com.ibm.wsspi.http.channel.exception.HttpErrorException;

/**
 * The interface to use to send errors back to the client and close with
 * the given exception.
 * 
 * @ibm-private-in-use
 */
public class HttpError extends GenericKeys {

    /** Exception associated with this error value */
    private Exception myException = null;
    /** Message to use if we create a new exception each time */
    private String msg = null;

    /**
     * Constructor for an HTTP error message that will create an error exception
     * using the input message each time the exception is queried.
     * 
     * @param errorCode
     * @param message
     */
    public HttpError(int errorCode, String message) {
        super("HttpErrorValues" + errorCode, errorCode);
        this.msg = message;
    }

    /**
     * Constructor for an HTTP error object that will always use the provided
     * exception instance.
     * 
     * @param errorCode
     * @param e
     */
    public HttpError(int errorCode, Exception e) {
        super("HttpErrorValues" + errorCode, errorCode);
        this.myException = e;
    }

    /**
     * Query the value of the error code (Http status value).
     * 
     * @return int
     */
    public int getErrorCode() {
        return getOrdinal();
    }

    /**
     * Query the value of the error body.
     * 
     * @return WsByteBuffer[]
     */
    public WsByteBuffer[] getErrorBody() {
        // Does not currently provide anything
        return null;
    }

    /**
     * Query the exception for this error code.
     * 
     * @return Exception
     */
    public Exception getClosingException() {
        if (null == this.myException) {
            // create a new one now
            return new HttpErrorException(this.msg);
        }
        // otherwise return our static instance
        return this.myException;
    }

}
