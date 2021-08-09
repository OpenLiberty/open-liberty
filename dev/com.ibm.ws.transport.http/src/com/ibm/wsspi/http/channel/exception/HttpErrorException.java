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
 * Exceptions for defined Http Errors.
 * 
 * @ibm-private-in-use
 */
public class HttpErrorException extends Exception {

    /** Serialization ID value */
    static final private long serialVersionUID = -8639277872739606826L;

    /**
     * Constructor for this exception
     * 
     * @param message
     */
    public HttpErrorException(String message) {
        super(message);
    }
}
