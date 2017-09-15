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

package com.ibm.websphere.sib.exception;

import com.ibm.ws.sib.utils.Reasonable;

/**
 * SIException is the superclass of all SIBus Exceptions.
 * 
 * @ibm-was-base
 * @ibm-api
 */
public abstract class SIException extends Exception {

    /**
     * Construct a new SIException when no additional details can be provided
     * 
     */
    public SIException() {
        super();
    }

    /**
     * Constructor for when the Exception is to be thrown because another
     * Exception has been caught during the copy.
     * 
     * @param cause The original Throwable which has caused this to be thrown.
     */
    public SIException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor for when the Exception is to be thrown for a reason other than
     * that an Exception has been caught during the copy.
     * 
     * @param message A String giving information about the problem which caused this to be thrown.
     */
    public SIException(String message) {
        super(message);
    }

    /**
     * Constructor for when the Exception is to be thrown because another
     * Exception has been caught during the copy and additional information is
     * to be included.
     * 
     * @param message A String giving information about the problem which caused this to be thrown.
     * @param cause The original Throwable which has caused this to be thrown.
     */
    public SIException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns an integer that indicates the reason a message could not be
     * delivered. Some subclasses of SIException can indicate a failure to deliver
     * a message (i.e. those thrown by SICoreConnection.createProducerSession and
     * send). These subclasses will include a specific exception reason, chosen
     * from those documented in com.ibm.websphere.sib.SIRCConstants, to indicate
     * the reason that the message could not be delivered. All other
     * exceptions will just return SIRC0001_DELIVERY_ERROR.
     * 
     * @return the exception reason
     */
    public int getExceptionReason() {
        Throwable cause = getCause();

        if (cause instanceof Reasonable)
            return ((Reasonable) cause).getExceptionReason();
        else if (cause instanceof SIException)
            return ((SIException) cause).getExceptionReason();
        else if (cause instanceof SIErrorException)
            return ((SIErrorException) cause).getExceptionReason();
        else
            return Reasonable.DEFAULT_REASON;
    }

    /**
     * Returns an array of "exception inserts", which are used in conjunction with
     * the exception reason to identify the reason that a message could not be
     * delivered. For example, if getExceptionReason returns a value of
     * SIRC0003_DESTINATION_NOT_FOUND, the array will contain, as its only element,
     * the name of the destination that could not be found. See
     * getExceptionInserts() for further information.
     * 
     * @see #getExceptionReason
     * 
     * @return exception inserts
     */
    public String[] getExceptionInserts() {
        Throwable cause = getCause();

        if (cause instanceof Reasonable)
            return ((Reasonable) cause).getExceptionInserts();
        else if (cause instanceof SIException)
            return ((SIException) cause).getExceptionInserts();
        else if (cause instanceof SIErrorException)
            return ((SIErrorException) cause).getExceptionInserts();
        else
            return Reasonable.DEFAULT_INSERTS;
    }
}
