/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.ras;

/**
 * This class is the base class of all Ras exceptions. It extends from the
 * exception class com.ibm.ws.exception.WsException to enable exception chaining.
 * <p>
 * All other Ras exceptions must extend this exception.
 */
public class RasException extends com.ibm.ws.exception.WsException {

    private static final long serialVersionUID = -3174194708446299559L;

    /**
     * Default constructor.
     */
    RasException() {
        super();
    }

    /**
     * Constructor that takes a Throwable to be chained.
     * 
     * @param throwable The Throwable to be chained
     */
    RasException(Throwable throwable) {
        super(throwable);
    }

    /**
     * Constructor that takes a String message.
     * 
     * @param message caller-specified text
     */
    RasException(String message) {
        super(message);
    }

    /**
     * Constructor that takes a String message and a Throwable to chain
     * 
     * @param message caller-specified text
     * @param throwable The Throwable that is to be chained.
     */
    RasException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
