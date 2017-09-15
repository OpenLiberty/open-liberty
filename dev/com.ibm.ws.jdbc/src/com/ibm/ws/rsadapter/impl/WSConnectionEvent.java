/*******************************************************************************
 * Copyright (c) 2001, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.impl;

import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ManagedConnection;

/**
 * WebSphere subclass of ConnectionEvent which allows ConnectionEvents to be reused.
 */
public class WSConnectionEvent extends ConnectionEvent {
    private static final long serialVersionUID = 41329220203996022L; 

    /**
     * Constant to indicate that only the connection the event was fired on
     * is to be destroyed, regardless of the purge policy.
     */
    public static final int SINGLE_CONNECTION_ERROR_OCCURRED = 51; // value of com.ibm.websphere.j2c.ConnectionEvent.SINGLE_CONNECTION_ERROR_OCCURRED

    /**
     * Constant to indicate that no message should be logged for this error, as it
     * was initiated by the application or by JMS
     */
    public static final int CONNECTION_ERROR_OCCURRED_NO_EVENT = 52; // value of com.ibm.websphere.j2c.ConnectionEvent.CONNECTION_ERROR_OCCURRED_NO_EVENT

    /** The exception about to be thrown to the application, or null if none. */
    private Exception exception;

    /**
     * Construct a reusable ConnectionEvent object.
     * 
     * @param source ManagedConnection that is the source of the event.
     * 
     */
    public WSConnectionEvent(ManagedConnection source) {
        super(source, 0);
    }

    /**
     * Construct a ConnectionEvent object.
     * 
     * @param source ManagedConnection that is the source of the event.
     * @param eid type of the Connection event.
     * @param ex exception about to be thrown to the application, or null if none.
     * @param handle Connection handle on which the error occurred, or null if none.
     */
    public WSConnectionEvent(ManagedConnection source, int eid, Exception ex, Object handle) {
        super(source, eid);
        exception = ex;
        setConnectionHandle(handle);
    }

    /**
     * @return the exception about to be thrown. May be null if there is no exception.
     */
    @Override
    public final Exception getException() {
        return exception;
    }

    /**
     * Recycle this ConnectionEvent by replacing the current values with those for the new
     * event.
     * 
     * @param eid ConnectionEvent constant indicating the type of ConnectionEvent.
     * @param ex the exception about to be throw to the application, or null if none.
     * @param handle Connection handle on which the error occurred, or null if none.
     * 
     * @return ConnectionEvent with the specified parameters.
     */
    public final WSConnectionEvent recycle(int eid, Exception ex, Object handle) 
    {
        id = eid;
        exception = ex;
        setConnectionHandle(handle);

        return this;
    }
}
