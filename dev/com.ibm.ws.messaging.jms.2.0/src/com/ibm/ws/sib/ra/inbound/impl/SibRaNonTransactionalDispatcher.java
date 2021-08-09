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
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AbstractConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SIUncoordinatedTransaction;

/**
 * Dispatches messages to a non-transactional message endpoint. Keeps track of
 * messages that are not delivered successfully and unlocks them in
 * <code>cleanup</code>.
 */
class SibRaNonTransactionalDispatcher extends SibRaDispatcher {

    /**
     * The list of unsuccessful messages ids.
     */
    private final List _unsuccessfulMessages = new ArrayList();

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaNonTransactionalDispatcher.class);

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaNonTransactionalDispatcher.class
            .getName();

    private static final String FFDC_PROBE_1 = "1";
    private static final String FFDC_PROBE_2 = "2";
    
 
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
    SibRaNonTransactionalDispatcher(final SibRaMessagingEngineConnection connection,
            final AbstractConsumerSession session,
            final SibRaEndpointActivation endpointActivation,
	    final Reliability unrecoveredReliability,
            final int maxFailedDeliveries,
            final int sequentialFailureThreshold)
            throws ResourceException {

        super(connection, session, endpointActivation, unrecoveredReliability, maxFailedDeliveries, sequentialFailureThreshold);

        final String methodName = "SibRaNonTransactionalDispatcher";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                    session, endpointActivation });
        }

        _deleteUnrecoverableMessages = !endpointActivation
                .isEndpointMethodTransactional();

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Returns the transaction, if any, used to deliver the message.
     * 
     * @return always returns <code>null</code>
     */
    protected SITransaction getTransaction() {

        if (TRACE.isEntryEnabled()) {
            final String methodName = "getTransaction";
            SibTr.entry(this, TRACE, methodName);
            SibTr.exit(this, TRACE, methodName, null);
        }
        return null;

    }

    /**
     * Creates an endpoint with no <code>XAResource</code>.
     * 
     * @return the endpont
     * @throws UnavailableException
     *             if the endpoint could not be created
     */
    final protected MessageEndpoint createEndpoint()
            throws UnavailableException {

        final String methodName = "createEndpoint";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        final MessageEndpoint endpoint = _endpointFactory.createEndpoint(null);

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, endpoint);
        }
        return endpoint;

    }

    /**
     * Invoked before delivery of a message.
     * 
     * @param message
     *            the message that is about to be delivered
     * @param endpoint The messaage endpoint
     */
    protected final void beforeDelivery(final SIBusMessage message, MessageEndpoint endpoint) {

        if (TRACE.isEntryEnabled()) {
            final String methodName = "beforeDelivery";
            SibTr.entry(this, TRACE, methodName, new Object [] { message, endpoint});
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Invoked before delivery of a message.
     * 
     */
    protected final void beforeDelivery(MessageEndpoint endpoint) {

        if (TRACE.isEntryEnabled()) {
            final String methodName = "beforeDelivery";
            SibTr.entry(this, TRACE, methodName, endpoint);
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Invoked after delivery of a message. If the message delivery was not
     * successful, add the message identifier to the set to be unlocked by
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

        if (!success && processMessage(message)) {

            _unsuccessfulMessages.add(message);

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Invoked after all messages in the batch have been delivered. Unlocks any
     * unsuccessful messages.
     */
    protected void cleanup() {

        final String methodName = "cleanup";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        if (_unsuccessfulMessages.size() > 0) {

            try {

                // 250710: Fix to workaround the problem where failed messages are
                // unlocked but the retry count is not incremented. Perform a
                // delete on the messages under a transaction and then roll back
                // the transaction - this will cause the messages to be unlocked
                // and have their retry count incremented. If the an exception
                // is thrown during this processing the messages will remain locked.
                try
				{
                	SIUncoordinatedTransaction localTran = _connection.createUncoordinatedTransaction();
                	deleteMessages(getMessageHandles(_unsuccessfulMessages), localTran);
                	localTran.rollback();
				}
                catch (SIException ex)
				{
                    FFDCFilter.processException(ex, CLASS_NAME + "."
                            + methodName, FFDC_PROBE_2, this);
                    if (TRACE.isEventEnabled()) {
                        SibTr.exception(this, TRACE, ex);
                    }
				}
//                unlockMessages(_unsuccessfulMessages);

            } catch (final ResourceException exception) {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, FFDC_PROBE_1, this);
                if (TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }

            } finally {

                _unsuccessfulMessages.clear();

            }

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Indicates whether the given message needs to be processed (deleted or
     * unlocked) i.e. that it has not already been deleted during
     * <code>consumeMessages</code>.
     * 
     * @param message
     *            the message
     * @return whether the message should be processed
     */
    protected boolean processMessage(final SIBusMessage message) 
    {
      Reliability preInvokeMessageReliability = (Reliability)_reliabilityPreInvoke.get(message.getMessageHandle());
      boolean canDeleteMessage;
      if (preInvokeMessageReliability != null)
      {
        canDeleteMessage = (_unrecoverableReliability.compareTo(preInvokeMessageReliability) >= 0);
      }
      else  // if the pre invoke reliability is not in the map then go back to using the message which most of the time wil be fine
      {
        canDeleteMessage = (_unrecoverableReliability.compareTo(message.getReliability()) >= 0);
      }

      return !(_deleteUnrecoverableMessages && canDeleteMessage);

    }

    /**
     * Returns a string generator containing the fields for this class.
     * 
     * @return a string generator
     */
    protected SibRaStringGenerator getStringGenerator() {

        final SibRaStringGenerator generator = super.getStringGenerator();
        generator.addField("unsuccessfulMessages", _unsuccessfulMessages);
        return generator;

    }

    /**
     * This method is not used for non transactional MDB's
     * 
     * @param The message handle for the message to be read
     */
	SIBusMessage readMessage(SIMessageHandle handle) {
		return null;
	}

}
