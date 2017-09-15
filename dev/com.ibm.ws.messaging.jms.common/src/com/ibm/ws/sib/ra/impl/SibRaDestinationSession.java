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

package com.ibm.ws.sib.ra.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationSession;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * Implementation of <code>DestinationSession</code> for core SPI resource
 * adapter. Holds a real <code>DestinationSession</code> to which methods
 * delegate.
 */
abstract class SibRaDestinationSession implements DestinationSession {

    /**
     * The connection on which this session was created.
     */
    protected final SibRaConnection _parentConnection;

    /**
     * The <code>DestinationSession</code> to which calls are delegated.
     */
    private final DestinationSession _delegateSession;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaDestinationSession.class);

    /**
     * The <code>TraceNLS</code> to use with trace.
     */
    private static final TraceNLS NLS = SibRaUtils.getTraceNls();

 
    /**
     * Constructor.
     * 
     * @param connection
     *            the connection on which this session was created
     * @param delegate
     *            the session to which calls should be delegated
     */
    public SibRaDestinationSession(final SibRaConnection connection,
            final DestinationSession delegate) {

        final String methodName = "SibRaDestinationSession";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                    delegate });
        }

        _parentConnection = connection;
        _delegateSession = delegate;

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Closes this session. Delegates.
     * 
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     */
    public void close() throws SIConnectionLostException, SIResourceException,
            SIErrorException, SIConnectionDroppedException {

        final String methodName = "close";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        _delegateSession.close();

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Returns the parent connection. Checks that the session is valid,
     * delegates to ensure the correct exception behaviour and then returns the
     * parent <code>SibRaConnection</code>.
     * 
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid or has been closed
     * @throws SISessionUnavailableException
     *             if the session is not valid or has been closed
     * @throws SIConnectionDroppedException
     *             if the connection has been closed by the bus
     * @throws SISessionDroppedException
     *             if the session has been closed by the bus
     */
    public SICoreConnection getConnection() throws SISessionDroppedException,
            SIConnectionDroppedException, SISessionUnavailableException,
            SIConnectionUnavailableException {

        final String methodName = "getConnection";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        checkValid();

        // Delegate in order to get the correct exception behaviour...
        _delegateSession.getConnection();

        // ...then just return the parent connection

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, _parentConnection);
        }
        return _parentConnection;

    }

    /**
     * Returns the destination address associated with this session. Delegates.
     */
    public SIDestinationAddress getDestinationAddress() {

        final String methodName = "getDestinationAddress";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        final SIDestinationAddress address = _delegateSession
                .getDestinationAddress();

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, _parentConnection);
        }
        return address;

    }

    /**
     * Returns a string representation of this object.
     * 
     * @return the string representation
     */
    public String toString() {

        final StringBuffer buffer = SibRaUtils.startToString(this);
        SibRaUtils.addFieldToString(buffer, "parentConnection",
                _parentConnection);
        SibRaUtils
                .addFieldToString(buffer, "delegateSession", _delegateSession);
        SibRaUtils.endToString(buffer);

        return buffer.toString();
    }

    /**
     * Checks that the parent connection has not been invalidated by the
     * connection manager.
     * 
     * @throws SISessionUnavailableException
     *             if the connection has been invalidated
     */
    protected void checkValid() throws SISessionUnavailableException {

        if (!_parentConnection.isValid()) {

            final SISessionUnavailableException exception = new SISessionUnavailableException(
                    NLS.getString("INVALID_SESSION_CWSIV0200"));
            if (TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw exception;

        }

    }

}
