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
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.sib.core.AbstractConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;

/**
 * Dispatches messages to a transactional message endpoint using a
 * <code>Synchronization</code> rather than an <code>XAResource</code> to
 * drive deletion of the message.
 */
class SibRaSynchronizedDispatcher extends SibRaDispatcher {

    /**
     * The <code>SITransaction</code> registered with the transaction and used
     * to delete the messages.
     */
    private SITransaction _transaction;

    /**
     * Flag indicating if a transaction was rolled back during message delivery
     * (for transactional dispatchers only).
     */
    private boolean _transactionRolledBack = false;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaSynchronizedDispatcher.class);

    /**
     * Provides access to NLS enabled messages.
     */
    private static TraceNLS NLS = SibRaUtils.getTraceNls();

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaSynchronizedDispatcher.class
            .getName();

    private static final String FFDC_PROBE_1 = "1";

    private static final String FFDC_PROBE_2 = "2";

    private static final String FFDC_PROBE_3 = "3";

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
    protected SibRaSynchronizedDispatcher(final SibRaMessagingEngineConnection connection,
            final AbstractConsumerSession session,
            final SibRaEndpointActivation endpointActivation,
	    final Reliability unrecoveredReliability,
            final int maxFailedDeliveries,
            final int sequentialFailureThreshold)
            throws ResourceException {

        super(connection, session, endpointActivation, unrecoveredReliability, maxFailedDeliveries, sequentialFailureThreshold);

        final String methodName = "SibRaSynchronizedDispatcher";

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                    session, endpointActivation });
        }
        
        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Returns the transaction, if any, used to deliver the message.
     * 
     * @return the transaction
     * @throws ResourceException
     *             if the transaction could not be obtained
     */
    protected SITransaction getTransaction() throws ResourceException {

        final String methodName = "getTransaction";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        if (_transaction == null) {

            try {

                _transaction = _connection.createUncoordinatedTransaction(false);

            } catch (final Exception exception) {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, FFDC_PROBE_1, this);
                if (TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                        "UNCOORD_TRAN_CWSIV1201", new Object[] { _connection,
                                exception }, null), exception);

            }
        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, _transaction);
        }
        return _transaction;

    }

    /**
     * Creates an endpoint. Passes a null <code>XAResource</code> on the
     * creation.
     * 
     * @return the endpoint
     * @throws ResourceException
     *             if the endpoint could not be created
     */
    protected MessageEndpoint createEndpoint() throws ResourceException {

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
     * Invoked before delivery of a message. Calls <code>beforeDelivery</code>
     * on the endpoint and then deletes the message under that transaction.
     * 
     * @param endpoint 
     *            the message endpoint we are using
     * @throws ResourceAdapterInternalException
     *             if the endpoint method does not exist or the
     *             <code>XAResource</code> is not enlisted
     * @throws ResourceException
     *             if before delivery failed
     */
    protected void beforeDelivery(final MessageEndpoint endpoint)
            throws ResourceException {

        final String methodName = "beforeDelivery";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, endpoint);
        }

        /*
         * Invoke beforeDelivery on the endpoint
         */

        try {

            endpoint.beforeDelivery(_invoker.getEndpointMethod());

        } catch (final NoSuchMethodException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                    + methodName, FFDC_PROBE_2, this);
            if (TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                    ("BEFORE_DELIVERY_CWSIV1200"), new Object[] { exception,
                            endpoint }, null), exception);

        }
        
        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Invoked before delivery of a message. Calls <code>beforeDelivery</code>
     * on the endpoint and then deletes the message under that transaction.
     * 
     * @param message
     *            the message that is about to be delivered
     * @param endpoint 
     *            the message endpoint we are using
     * @throws ResourceAdapterInternalException
     *             if the endpoint method does not exist or the
     *             <code>XAResource</code> is not enlisted
     * @throws ResourceException
     *             if before delivery failed
     */
    protected void beforeDelivery(final SIBusMessage message, final MessageEndpoint endpoint)
            throws ResourceException {

        final String methodName = "beforeDelivery";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object [] { message, endpoint });
        }
        
        // This will call before delivery on the endpoint
        beforeDelivery (endpoint);

        /*
         * Delete the message under the transaction
         */
        deleteMessage(message, getTransactionForDelete ());

    }
    
    /**
     * The method reads a message from a handle. This is z/os specific method
     * 
     * @param The handle associated with the message to read
     * @return The message
     */
    protected SIBusMessage readMessage (SIMessageHandle handle) 
         throws ResourceException, SIMessageNotLockedException {
    	
        final String methodName = "beforeDelivery";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, handle);
        }
        
        /*
         * Delete the message under the transaction
         */
        SIBusMessage message = readAndDeleteMessage(handle, getTransactionForDelete ());
    
        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, message);
        }
        
        return message;

    }

    /**
     * This method gets the transaction that can be used for deleting a message (at the beforeDelivery
     * stage for distributed and in readMessage method for z/os).
     * 
     * @return A transaction to delete a message under
     * @throws ResourceException 
     *             if the transaction could not be obtained.
     */
    private SITransaction getTransactionForDelete () throws ResourceException {
    	
        final String methodName = "getTransactionForDelete";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        /*
         * Register a synchronization to drive the deletion of the message on
         * completion
         */

        final SITransaction transaction = getTransaction();

        if (transaction instanceof Synchronization) {

            try {
            	
            	EmbeddableWebSphereTransactionManager transactionManager = EmbeddableTransactionManagerFactory.getTransactionManager();
                final UOWCoordinator coordinator = EmbeddableTransactionManagerFactory.getUOWCurrent().getUOWCoord();
               

                // The inner class here is used to wrap the transaction (which implements
                // Synchronization itself) and to check after completion to see if the
                // transaction was rolled back.
                
                transactionManager.registerSynchronization(coordinator,
                        new Synchronization() {

                            public void beforeCompletion() {

                                // deletegate to the transaction
                                ((Synchronization) transaction)
                                        .beforeCompletion();

                            }

                            public void afterCompletion(int status) {

                                // deletegate to the transaction
                                ((Synchronization) transaction)
                                        .afterCompletion(status);
                                _transactionRolledBack = !(Status.STATUS_COMMITTED == status);

                            }

                        }, EmbeddableTransactionManagerFactory.getTransactionManager().SYNC_TIER_INNER);

            } catch (final Exception exception) {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, FFDC_PROBE_3, this);
                if (TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                        "REGISTER_SYNC_CWSIV1202", new Object[] { transaction,
                                exception }, null), exception);

            }

        } else {

            throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                    "SYNC_CWSIV1203", new Object[] { transaction,
                            Synchronization.class.getName() }, null));

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, transaction);
        }

    	return transaction;
    }

    /**
     * Invoked after delivery of a message. Calls <code>afterDelivery</code>
     * on the endpoint.
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

        /*
         * Null out the transaction reference so that we create a new one for
         * the next delivery
         */

        _transaction = null;

        endpoint.afterDelivery();

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Invoked after all messages in the batch have been delivered. Does
     * nothing.
     */
    protected void cleanup() {

        if (TRACE.isEntryEnabled()) {
            final String methodName = "cleanup";
            SibTr.entry(this, TRACE, methodName);
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
        generator.addField("transaction", _transaction);
        return generator;

    }
    
    protected boolean isTransactionRolledBack () {
        
        return _transactionRolledBack;
        
    }

}
