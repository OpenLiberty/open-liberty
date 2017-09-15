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
 * This class is used for reporting trace string parsing errors.
 * It extends from the exception class RasException as should all
 * Ras related exceptions.
 */
public class MalformedTraceStringException extends RasException {

    private static final long serialVersionUID = -4722157347311978259L;

    /**
     * Default constructor.
     */
    MalformedTraceStringException() {
        super();
    }

    /**
     * Constructor that takes a Throwable to be chained.
     * 
     * @param throwable The Throwable to be chained
     */
    MalformedTraceStringException(Throwable throwable) {
        super(throwable);
    }

    /**
     * Constructor that takes a String message.
     * 
     * @param message caller-specified text
     */
    MalformedTraceStringException(String message) {
        super(message);
    }

    /**
     * Constructor that takes a String message and a Throwable to chain
     * 
     * @param message caller-specified text
     * @param throwable The Throwable that is to be chained.
     */
    MalformedTraceStringException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
