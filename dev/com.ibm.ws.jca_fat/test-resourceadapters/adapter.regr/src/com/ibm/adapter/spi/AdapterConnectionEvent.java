/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.spi;

import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ManagedConnection;

/**
 * ConnectionEvent class for this adapter.
 */
public class AdapterConnectionEvent extends ConnectionEvent {

    /** The INTERACTION PENDING event constant. */
    public static final int INTERACTION_PENDING = com.ibm.websphere.j2c.ConnectionEventListener.INTERACTION_PENDING;

    /** The exception about to be thrown to the application, or null if none. */
    private Exception exception;

    /**
     * Construct a ConnectionEvent object.
     *
     * @param source ManagedConnection that is the source of the event.
     * @param eid type of the Connection event.
     * @param ex exception about to be thrown to the application, or null if none.
     * @param handle Connection handle on which the error occurred, or null if none.
     */
    public AdapterConnectionEvent(ManagedConnection source, int eid, Exception ex, Object handle) {

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
     * @param source the ManagedConnection that is the source of the event.
     * @param eid ConnectionEvent constant indicating the type of ConnectionEvent.
     * @param ex the exception about to be throw to the application, or null if none.
     * @param handle Connection handle on which the error occurred, or null if none.
     *
     * @return ConnectionEvent with the specified parameters.
     */
    public final AdapterConnectionEvent recycle(ManagedConnection source, int eid, Exception ex,
                                                Object handle) {
        this.source = source;
        id = eid;
        exception = ex;
        setConnectionHandle(handle);

        return this;
    }
}
