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
package com.ibm.ws.sib.ra.inbound.impl;

import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpoint;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AbstractConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;

/**
 * Dispatches messages to a message endpoint that requires each successful
 * message to be deleted immediately after processing e.g. JMS
 * <code>AUTO_AKNOWLEDGE</code>. Deletes successful messages in
 * <code>afterDelivery</code>.
 */
final class SibRaSingleMessageDeletionDispatcher extends
        SibRaNonTransactionalDispatcher {

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaSingleMessageDeletionDispatcher.class);

    /**
     * Constructor.
     * 
     * @param connection
     *            the parent connection for the session
     * @param session
     *            the session from which the messages were received
     * @param endpointActivation
     *            the endpoint activation which lead to these messages being
     *            received
     * @throws ResourceException
     *             if the dispatcher cannot be successfully created
     */
    SibRaSingleMessageDeletionDispatcher(final SibRaMessagingEngineConnection connection,
            final AbstractConsumerSession session,
            final SibRaEndpointActivation endpointActivation,
            final Reliability unrecoveredReliability,
            final int maxFailedDeliveries,
            final int sequentialFailureThreshold)
            throws ResourceException {

        super(connection, session, endpointActivation, unrecoveredReliability, maxFailedDeliveries, sequentialFailureThreshold);

        if (TRACE.isEntryEnabled()) {
            final String methodName = "SibRaSingleMessageDeletionDispatcher";
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                    session, endpointActivation });
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Invoked after delivery of a message. If the message delivery was
     * successful, deletes the message.
     * 
     * @param message
     *            the message that was delivered
     * @param success
     *            flag indicating whether delivery was successful
     * @throws ResourceException
     *             if after delivery failed
     */
    protected void afterDelivery(final SIBusMessage message, MessageEndpoint endpoint, 
            final boolean success) throws ResourceException {

        final String methodName = "afterDelivery";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { message,
                    Boolean.valueOf(success) });
        }

        super.afterDelivery(message, endpoint, success);

        if (success && processMessage(message)) {

            deleteMessage(message, null);

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

}
