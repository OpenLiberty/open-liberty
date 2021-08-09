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
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.BrowserSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * Implementation of <code>BrowserSession</code> for core SPI resource
 * adapter. Holds a real <code>BrowserSession</code> to which methods
 * delegate.
 */
final class SibRaBrowserSession extends SibRaDestinationSession implements
        BrowserSession {

    /**
     * The <code>BrowserSession</code> to which calls are delegated.
     */
    private final BrowserSession _delegate;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaBrowserSession.class);

 

    /**
     * Constructor.
     * 
     * @param connection
     *            the connection on which this session was created
     * @param delegate
     *            the session to which calls should be delegated
     */
    SibRaBrowserSession(final SibRaConnection connection,
            final BrowserSession delegate) {

        super(connection, delegate);

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "SibRaBrowserSession", new Object[]{
                    connection, delegate});
        }

        _delegate = delegate;

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "SibRaBrowserSession");
        }

    }

    /**
     * Browses the next message. Checks that the session is valid and then
     * delegates.
     * 
     * @throws SIConnectionUnavailableException
     *             if the session is not valid
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SISessionUnavailableException
     *             if the delegation fails
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     * @throws SISessionDroppedException
     *             if the delegation fails
     */
    public SIBusMessage next() throws SISessionDroppedException, SIConnectionDroppedException, SISessionUnavailableException, SIConnectionUnavailableException, SIConnectionLostException, SINotAuthorizedException, SIResourceException, SIErrorException {

        checkValid();

        return _delegate.next();

    }

    /**
     * Resets the browse cursor. Checks that the session is valid and then
     * delegates.
     * 
     * @throws SIConnectionUnavailableException
     *             if the session is not valid
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SISessionUnavailableException
     *             if the delegation fails
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     * @throws SISessionDroppedException
     *             if the delegation fails
     */
    public void reset() throws SISessionDroppedException, SIConnectionDroppedException, SISessionUnavailableException, SIConnectionUnavailableException, SIConnectionLostException, SIResourceException, SIErrorException {

        checkValid();

        _delegate.reset();

    }

}
