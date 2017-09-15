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
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ProducerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * Implementation of <code>ProducerSession</code> for core SPI resource
 * adapter. Holds a real <code>ProducerSession</code> to which methods
 * delegate.
 */
final class SibRaProducerSession extends SibRaDestinationSession implements
        ProducerSession {

    /**
     * The <code>ProducerSession</code> to which calls are delegated.
     */
    private final ProducerSession _delegate;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaProducerSession.class);

 
    /**
     * Constructor.
     * 
     * @param connection
     *            the connection on which this session was created
     * @param delegate
     *            the session to which calls should be delegated
     */
    SibRaProducerSession(final SibRaConnection connection,
            final ProducerSession delegate) {

        super(connection, delegate);

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "SibRaProducerSession", new Object[]{
                    connection, delegate});
        }

        _delegate = delegate;

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "SibRaProducerSession");
        }

    }

    /**
     * Sends a message. Checks that the session is valid. Maps the transaction
     * parameter before delegating.
     * 
     * @param msg
     *            the message to send
     * @param tran
     *            the transaction to send the message under
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SINotPossibleInCurrentConfigurationException
     *             if the delegation fails
     * @throws SIIncorrectCallException
     *             if the transaction parameter is not valid given the current
     *             application and container transactions
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the current container transaction cannot be determined
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
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
    public void send(final SIBusMessage msg, final SITransaction tran)
            throws SISessionDroppedException, SIConnectionDroppedException,
            SISessionUnavailableException, SIConnectionUnavailableException,
            SIConnectionLostException, SILimitExceededException,
            SINotAuthorizedException, SIResourceException, SIErrorException,
            SIIncorrectCallException,
            SINotPossibleInCurrentConfigurationException {

        checkValid();

        _delegate.send(msg, _parentConnection.mapTransaction(tran));

    }

}
