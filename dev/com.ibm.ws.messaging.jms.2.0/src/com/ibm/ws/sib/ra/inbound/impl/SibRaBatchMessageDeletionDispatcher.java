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

import java.util.ArrayList;
import java.util.List;

import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpoint;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AbstractConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;

/**
 * Dispatches messages to a message endpoint that permits successful messages to
 * be deleted in a batch after processing e.g. JMS
 * <code>DUPS_OK_AKNOWLEDGE</code>. Keeps track of messages that are
 * delivered successfully and deletes them in <code>cleanup</code>.
 */
final class SibRaBatchMessageDeletionDispatcher extends
        SibRaNonTransactionalDispatcher {

    /**
     * List of successfully delivered messages.
     */
    private final List _successfulMessages = new ArrayList();

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaBatchMessageDeletionDispatcher.class);

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaNonTransactionalDispatcher.class
            .getName();

    private static final String FFDC_PROBE_1 = "1";

  
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
     * @param unrecoveredReliability
     *            the unrecoveredReliability value for the destination
     * @param maxFailedDeliveries
     *            the maxFailedDeliveries value for the destination
     * @param sequentialFailureThreshold
     *            the sequentialFailureThreshold value for the destination
     * @throws ResourceException
     *             if the dispatcher cannot be successfully created
     */
    SibRaBatchMessageDeletionDispatcher(final SibRaMessagingEngineConnection connection,
            final AbstractConsumerSession session,
            final SibRaEndpointActivation endpointActivation,
            final Reliability unrecoveredReliability,
            final int maxFailedDeliveries,
            final int sequentialFailureThreshold)
            throws ResourceException {

        super(connection, session, endpointActivation, unrecoveredReliability, maxFailedDeliveries, sequentialFailureThreshold);

        if (TRACE.isEntryEnabled()) {
            final String methodName = "SibRaBatchMessageDeletionDispatcher";
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                    session, endpointActivation });
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Invoked after delivery of a message. If the message delivery was
     * successful, add the message to the set to be deleted by
     * <code>cleanup</code>.
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

            _successfulMessages.add(message);

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Invoked after all messages in the batch have been delivered. Deletes any
     * successful messages.
     */
    protected void cleanup() {

        final String methodName = "cleanup";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        super.cleanup();

        if (_successfulMessages.size() > 0) {

            try {

                deleteMessages(getMessageHandles(_successfulMessages), null);

            } catch (final ResourceException exception) {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, FFDC_PROBE_1, this);
                if (TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }

            } finally {

                _successfulMessages.clear();

            }

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Returns a string generator containing the fields for this class.
     * 
     * @return a string generator
     */
    protected SibRaStringGenerator getStringGenerator() {

        final SibRaStringGenerator generator = super.getStringGenerator();
        generator.addField("successfulMessages", _successfulMessages);
        return generator;

    }

}
