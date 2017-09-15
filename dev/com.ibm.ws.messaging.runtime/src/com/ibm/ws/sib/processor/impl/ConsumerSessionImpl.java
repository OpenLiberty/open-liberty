/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.impl;

import java.util.LinkedList;
import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.processor.MPConsumerSession;
import com.ibm.ws.sib.processor.MPSubscription;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPMessageNotLockedException;
import com.ibm.ws.sib.processor.exceptions.SIMPTemporaryDestinationNotFoundException;
import com.ibm.ws.sib.processor.impl.corespitrace.CoreSPIConsumerSession;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.LocalConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.MPDestinationSession;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AsynchConsumerCallback;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.StoppableAsynchConsumerCallback;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;

/**
 * @author tevans
 */
public final class ConsumerSessionImpl implements MPConsumerSession, MPDestinationSession
{
    // NLS for component
    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    //trace
    private static final TraceComponent tc =
                    SibTr.register(
                                   ConsumerSessionImpl.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    //A reference to the messageProcessor that this ConsumerSession is associated with
    private final MessageProcessor _messageProcessor;
    //indicates if messages can be optimistically assigned to the consumer
    private final boolean _enableReadAhead;
    // indicates if this consumer is forward scanning or not
    private final boolean _forwardScanning;

    private final ConnectionImpl _connection;
    private final LocalConsumerPoint _localConsumerPoint;

    private final SIDestinationAddress _destAddr;

    private final boolean _bifurcatable;
    private final boolean _ignoreInitialIndoubts;

    /**
     * The long id for this consumer
     */
    private long _consumerId;
    /**
     * A list of any associated Bifurcated Consumers
     */
    private List<BifurcatedConsumerSessionImpl> _bifurcatedConsumers;

    private final SIBUuid12 uuid;

    // Nasty hack, see MPConsumerSession for justification
    private boolean bumpRedeliveryOnBifurcatedClose = true;

    /**
     * Create a new consumer session.
     * 
     * @param destination The destination to consume from
     * @param destAddr The destination address
     * @param state The consumer dispatcher state properties for this consumer session
     * @param connection The connection used to create this consumer session
     * @throws SIResourceException Thrown if there was a problem in the message store
     * @throws SINonDurableSubscriptionMismatchException
     * @throws SIDestinationAlreadyExistsException Thrown if the Subscription already exists!
     */
    ConsumerSessionImpl(
                        DestinationHandler destination,
                        SIDestinationAddress destAddr,
                        ConsumerDispatcherState state,
                        ConnectionImpl connection,
                        boolean enableReadAhead,
                        boolean forwardScanning,
                        Reliability unrecoverableReliability,
                        boolean bifurcatable,
                        boolean ignoreInitialIndoubts,
                        boolean gatherMessages)

        throws SINotPossibleInCurrentConfigurationException,
        SITemporaryDestinationNotFoundException, SISessionDroppedException,
        SIDestinationLockedException, SIDurableSubscriptionMismatchException, SINonDurableSubscriptionMismatchException,
        SIDiscriminatorSyntaxException, SISelectorSyntaxException,
        SIDurableSubscriptionNotFoundException, SISessionUnavailableException,
        SIIncorrectCallException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "ConsumerSessionImpl",
                        new Object[] { destination,
                                      destAddr,
                                      state,
                                      connection,
                                      Boolean.valueOf(enableReadAhead),
                                      Boolean.valueOf(forwardScanning),
                                      unrecoverableReliability,
                                      Boolean.valueOf(bifurcatable),
                                      Boolean.valueOf(ignoreInitialIndoubts),
                                      Boolean.valueOf(gatherMessages) });

        // Store the parameters
        _connection = connection;
        _messageProcessor = connection.getMessageProcessor();
        _enableReadAhead = enableReadAhead;
        _forwardScanning = forwardScanning;
        _bifurcatable = bifurcatable;
        _ignoreInitialIndoubts = ignoreInitialIndoubts;
        _destAddr = destAddr;
        this.uuid = new SIBUuid12();

        // If this isn't mediated, then set the messaging engine bus for the
        // destination address (WS requirement 204231
        // It can only be the local bus as we've already checked for a foreign bus

        ((JsDestinationAddress) destAddr).setBusName(_messageProcessor.getMessagingEngineBus());

        // See if the caller has fixed this consumer to a particular ME (either explicity
        // via an ME in the address or implicitly to the local one using isLocalOnly())
        // (Applicable to PtoP only)
        JsDestinationAddress jsDestAddr = (JsDestinationAddress) destAddr;

        if ((!destination.isPubSub()))
        {
            ConsumerManager consumerManager = destination.getLocalPtoPConsumerManager();

            // If we're fixed to the local ME, check that this ME has a queue point defined.
            // And if it does, fix us onto it. If we don't have one we ignore the isLocalOnly flag
            if ((jsDestAddr.getME() == null) && jsDestAddr.isLocalOnly() && (consumerManager != null))
            {
                jsDestAddr.setME(_messageProcessor.getMessagingEngineUuid());
            }

            // Check that if there is a local queue point, then the creation of it is
            // complete.  The local consumerdispatcher is created locked and unlocked
            // when the transaction under which the local queue point is created gets
            // committed
            if (consumerManager == null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "ConsumerDispatcher not found ");
            }
            else
            {
                if (consumerManager.isLocked())
                {
                    if (consumerManager.getDestination().isTemporary())
                    {
                        SIMPTemporaryDestinationNotFoundException e = new SIMPTemporaryDestinationNotFoundException(
                                        nls.getFormattedMessage(
                                                                "DESTINATION_IS_LOCKED_ERROR_CWSIP0133",
                                                                new Object[] { destination.getName(),
                                                                              _messageProcessor.getMessagingEngineName() },
                                                                null));

                        SibTr.exception(tc, e);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "ConsumerSessionImpl", e);
                        throw e;

                    }

                    SINotPossibleInCurrentConfigurationException e = new SINotPossibleInCurrentConfigurationException(
                                    nls.getFormattedMessage(
                                                            "DESTINATION_IS_LOCKED_ERROR_CWSIP0133",
                                                            new Object[] { destination.getName(),
                                                                          _messageProcessor.getMessagingEngineName() },
                                                            null));

                    SibTr.exception(tc, e);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "ConsumerSessionImpl", e);
                    throw e;
                }
            }
        }

        //Create a new LocalConsumerPoint for this consumer session
        _localConsumerPoint = new JSLocalConsumerPoint(destination, jsDestAddr, state, this,
                        unrecoverableReliability, bifurcatable, gatherMessages);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "ConsumerSessionImpl", this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#receiveNoWait(com.ibm.wsspi.sib.core.SITransaction)
     */
    @Override
    public SIBusMessage receiveNoWait(SITransaction siTran)
                    throws SIErrorException,
                    SISessionUnavailableException, SIIncorrectCallException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "receiveNoWait",
                        new Object[] { this, siTran });

        checkTransaction(siTran, "TRANSACTION_RECEIVE_USAGE_ERROR_CWSIP0777");

        SIBusMessage jsMsg = null;

        boolean sucessful = false;

        //Try to receive a message from the LCP
        try
        {
            jsMsg =
                            _localConsumerPoint.receive(LocalConsumerPoint.NO_WAIT,
                                                        (TransactionCommon) siTran);
            sucessful = true;
        } catch (SINotPossibleInCurrentConfigurationException e)
        {
            // No FFDC code needed.    

            // This case indicates that the destination has changed in some way
            // Probably receiveAllowed = false.
            if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
                SibTr.exit(CoreSPIConsumerSession.tc, "receiveNoWait", "SISessionUnavailableException");
            throw new SISessionUnavailableException(
                            nls.getFormattedMessage(
                                                    "CONSUMER_CLOSED_ERROR_CWSIP0177",
                                                    new Object[] { _destAddr.getDestinationName(),
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }

        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
            UserTrace.trace_Receive(siTran, (JsMessage) jsMsg, _destAddr, getIdInternal());

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "receiveNoWait", jsMsg);
        //return either the got message or null
        return jsMsg;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#receiveWithWait(com.ibm.wsspi.sib.core.SITransaction, long)
     */
    @Override
    public SIBusMessage receiveWithWait(SITransaction siTran,
                                        long timeout)
                    throws SISessionUnavailableException, SISessionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(
                        CoreSPIConsumerSession.tc,
                        "receiveWithWait",
                        new Object[] { this, new Long(timeout), siTran });

        checkTransaction(siTran, "TRANSACTION_RECEIVE_USAGE_ERROR_CWSIP0777");

        SIBusMessage jsMsg = null;

        boolean sucessful = false;

        //Try to receive a message from the LCP
        try
        {
            jsMsg =
                            _localConsumerPoint.receive(timeout,
                                                        (TransactionCommon) siTran);
            sucessful = true;
        } catch (SINotPossibleInCurrentConfigurationException e)
        {
            // No FFDC code needed.

            // This case indicates that the destination has changed in some way
            // Probably receiveAllowed = false.
            if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
                SibTr.exit(CoreSPIConsumerSession.tc, "receiveWithWait", "SISessionUnavailableException");
            throw new SISessionUnavailableException(
                            nls.getFormattedMessage(
                                                    "CONSUMER_CLOSED_ERROR_CWSIP0177",
                                                    new Object[] { _destAddr.getDestinationName(),
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }

        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
            UserTrace.trace_Receive(siTran, (JsMessage) jsMsg, _destAddr, getIdInternal());

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "receiveWithWait", jsMsg);

        //return the message or null
        return jsMsg;
    }

    /**
     * Adds the bifurcated consumer to the list of associated consumers.
     * 
     * @param consumer
     */
    protected void attachBifurcatedConsumer(BifurcatedConsumerSessionImpl consumer)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "attachBifurcatedConsumer", consumer);

        // Create a bifurcated list if required
        if (_bifurcatedConsumers == null)
        {
            synchronized (this)
            {
                if (_bifurcatedConsumers == null)
                    _bifurcatedConsumers = new LinkedList<BifurcatedConsumerSessionImpl>();
            }
        }

        synchronized (_bifurcatedConsumers)
        {
            _bifurcatedConsumers.add(consumer);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "attachBifurcatedConsumer");
    }

    /**
     * Removes the bifurcated consumer to the list of associated consumers.
     * 
     * @param consumer
     * @throws SISessionDroppedException
     */
    protected void removeBifurcatedConsumer(BifurcatedConsumerSessionImpl consumer) throws SIResourceException, SISessionDroppedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeBifurcatedConsumer", consumer);

        synchronized (_bifurcatedConsumers)
        {
            _bifurcatedConsumers.remove(consumer);
        }

        // Cleanup after the bifurcated consumer (unlock any messages it owns)
        _localConsumerPoint.cleanupBifurcatedConsumer(consumer);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeBifurcatedConsumer");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#close()
     */
    @Override
    public void close()
                    throws SIResourceException, SIConnectionLostException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "close", this);

        //perform the actual closing operations
        _close();

        //remove ourselves from the connection
        _connection.removeConsumerSession(this);

        if (_bifurcatedConsumers != null)
        {
            // Close any associated Bifurcated Consumers.
            // We take a copy of the list of bifucated consumers, as the close
            // method of each will remove itself from the list (so an iterator
            // would see concurrent modifications).
            // We let the bifurcated consumer do the work, as they need to
            // unlock the associated messages (and the code is re-used from
            // ConnectionImpl.close() which also calls into the BCS directly).
            BifurcatedConsumerSessionImpl[] bifurcatedConsumersCopy;
            synchronized (_bifurcatedConsumers)
            {
                bifurcatedConsumersCopy = new BifurcatedConsumerSessionImpl[_bifurcatedConsumers.size()];
                bifurcatedConsumersCopy = _bifurcatedConsumers.toArray(bifurcatedConsumersCopy);
            }
            for (int i = 0; i < bifurcatedConsumersCopy.length; i++) {
                bifurcatedConsumersCopy[i]._close();
            }
            _bifurcatedConsumers = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "close");
    }

    /**
     * Performs any operations required to close this consumer session,
     * but it does not modify any references which the connection might have.
     */
    void _close()
                    throws SIResourceException, SIConnectionLostException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "_close");

        try
        {
            //close the LCP
            _localConsumerPoint.close();
        } catch (SINotPossibleInCurrentConfigurationException e)
        {
            // No FFDC code needed

            //Do nothing ... probably means that the Destination is being deleted
            //and so the LCP will be closed anyway
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "_close", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "_close");
    }

    /**
     * Check that this consumer session is not closed.
     * 
     * @throws SIObjectClosedException
     */
    void checkNotClosed() throws SISessionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkNotClosed");

        try
        {
            synchronized (_localConsumerPoint)
            {
                _localConsumerPoint.checkNotClosed();
            }
        } catch (SISessionUnavailableException e)
        {
            // No FFDC code needed
            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkNotClosed", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkNotClosed");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#registerAsynchConsumerCallback(com.ibm.wsspi.sib.core.AsynchConsumerCallback, int, boolean)
     */
    @Override
    public void registerAsynchConsumerCallback(
                                               AsynchConsumerCallback callback,
                                               int maxActiveMessages,
                                               long messageLockExpiry,
                                               int maxBatchSize,
                                               OrderingContext context)
                    throws SISessionUnavailableException, SISessionDroppedException,
                    SIErrorException,
                    SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "registerAsynchConsumerCallback",
                        new Object[] { this,
                                      callback,
                                      new Integer(maxActiveMessages),
                                      new Long(messageLockExpiry),
                                      new Integer(maxBatchSize),
                                      context });
        registerAsynchConsumerCallback(callback,
                                       maxActiveMessages,
                                       messageLockExpiry,
                                       maxBatchSize,
                                       null,
                                       false,
                                       context);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "registerAsynchConsumerCallback");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#registerAsynchConsumerCallback(com.ibm.wsspi.sib.core.AsynchConsumerCallback, int, boolean)
     */
    @Override
    public void registerAsynchConsumerCallback(
                                               AsynchConsumerCallback callback,
                                               int maxActiveMessages,
                                               long messageLockExpiry,
                                               int maxBatchSize,
                                               Reliability unrecoverableReliability,
                                               boolean inLine,
                                               OrderingContext context)
                    throws SISessionDroppedException, SISessionUnavailableException, SIErrorException, SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "registerAsynchConsumerCallback",
                        new Object[] { this,
                                      callback,
                                      new Integer(maxActiveMessages),
                                      new Long(messageLockExpiry),
                                      new Integer(maxBatchSize),
                                      unrecoverableReliability,
                                      Boolean.valueOf(inLine),
                                      context });

        //register the AsynchConsumer with the LCP
        _localConsumerPoint.registerAsynchConsumer(
                                                   callback,
                                                   maxActiveMessages,
                                                   messageLockExpiry,
                                                   maxBatchSize,
                                                   unrecoverableReliability,
                                                   inLine,
                                                   (OrderingContextImpl) context,
                                                   null // No callback busy lock is used for non-mediation consumers.
        );

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "registerAsynchConsumerCallback");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#deregisterAsynchConsumerCallback()
     */
    @Override
    public void deregisterAsynchConsumerCallback()
                    throws SISessionUnavailableException, SISessionDroppedException,
                    SIErrorException,
                    SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "deregisterAsynchConsumerCallback", this);
        //Deregister the AsynchConsumer by registering null with the LCP
        //the other params don't matter
        registerAsynchConsumerCallback(null, 1, 0, 1, null);
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "deregisterAsynchConsumerCallback");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#start(boolean)
     */
    @Override
    public void start(boolean deliverImmediately)
                    throws SISessionUnavailableException, SISessionDroppedException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "start",
                        new Object[] { this, Boolean.valueOf(deliverImmediately) });
        //start the LCP
        _localConsumerPoint.start(deliverImmediately);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "start");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#stop()
     */
    @Override
    public void stop()
                    throws SISessionUnavailableException, SISessionDroppedException,
                    SIResourceException, SIConnectionLostException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "stop", this);

        //stop the LCP
        _localConsumerPoint.stop();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "stop");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#getConnection()
     */
    @Override
    public SICoreConnection getConnection() throws SISessionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "getConnection", this);

        checkNotClosed();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "getConnection", _connection);
        //Return the connection used to create this consumer session
        return _connection;
    }

    /**
     * Internal getter method which bypasses the not closed check.
     * 
     * @return
     */
    protected SICoreConnection getConnectionInternal()
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
        {
            SibTr.entry(CoreSPIConsumerSession.tc, "getConnectionInternal", this);
            SibTr.exit(CoreSPIConsumerSession.tc, "getConnectionInternal", _connection);
        }
        //Return the connection used to create this consumer session
        return _connection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#unlockSet(long[])
     */
    @Override
    public void unlockSet(SIMessageHandle[] msgHandles)
                    throws SIMPMessageNotLockedException, SISessionUnavailableException, SIConnectionLostException,
                    SIIncorrectCallException, SIResourceException, SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "unlockSet",
                        new Object[] { this, SIMPUtils.messageHandleArrayToString(msgHandles) });

        //pass the unlockSet call on to the LCP
        _localConsumerPoint.processMsgSet(msgHandles, null, null, true, false, false, true);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "unlockSet");

    }

    @Override
    public void unlockSet(SIMessageHandle[] msgHandles, boolean incrementLockCount)
                    throws SIMPMessageNotLockedException, SISessionUnavailableException, SIConnectionLostException,
                    SIIncorrectCallException, SIResourceException, SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "unlockSet",
                        new Object[] { this, SIMPUtils.messageHandleArrayToString(msgHandles), Boolean.valueOf(incrementLockCount) });

        //pass the unlockSet call on to the LCP
        _localConsumerPoint.processMsgSet(msgHandles, null, null, true, false, false, incrementLockCount);

        if (CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "unlockSet");

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#deleteSet(long[], com.ibm.wsspi.sib.core.SITransaction)
     */
    @Override
    public void deleteSet(SIMessageHandle[] msgHandles, SITransaction siTran)
                    throws SIMPMessageNotLockedException, SISessionUnavailableException, SIConnectionLostException,
                    SIIncorrectCallException, SIResourceException, SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "deleteSet",
                        new Object[] { this, SIMPUtils.messageHandleArrayToString(msgHandles), siTran });

        checkTransaction(siTran, "TRANSACTION_DELETE_USAGE_ERROR_CWSIP0778");

        //pass the deleteSet call on to the LCP
        _localConsumerPoint.processMsgSet(msgHandles, (TransactionCommon) siTran, null, false, true, false, true);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "deleteSet");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#unlockAll()
     */
    @Override
    public void unlockAll()
                    throws SISessionUnavailableException, SISessionDroppedException,
                    SIResourceException, SIConnectionLostException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "unlockAll", this);

        //pass the unlockAll call on to the LCP
        try
        {
            _localConsumerPoint.unlockAll();
        } catch (SIMPMessageNotLockedException e)
        {
            //No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(tc, e);
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "unlockAll");
    }

    /**
     * Gets the unique ID of the connection associated with this session
     * (Used for noLocal matching of subscriptions)
     * 
     * @return the connection's unique ID
     */
    public SIBUuid12 getConnectionUuid()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getConnectionUuid");

        //get the connection's unique id
        SIBUuid12 uuid = _connection.getUuid();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getConnectionUuid", uuid);

        //return the connection id
        return uuid;
    }

    /**
     * Gets the enableReadAhead setting
     * (Used for remote get)
     * 
     * @return the enableReadAhead property of the session
     */
    public boolean getReadAhead()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getReadAhead");
            SibTr.exit(tc, "getReadAhead", Boolean.valueOf(_enableReadAhead));
        }

        return _enableReadAhead;
    }

    /**
     * Gets the forwardScanning setting
     * (Used for MQ-like behaviour)
     * 
     * @return the forwardScanning property of the session
     */
    public boolean getForwardScanning()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getForwardScanning");
            SibTr.exit(tc, "getForwardScanning", Boolean.valueOf(_forwardScanning));
        }

        return _forwardScanning;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#activateAsynchConsumer()
     */
    @Override
    public void activateAsynchConsumer(boolean deliverImmediately)
                    throws SIErrorException, SISessionUnavailableException, SIIncorrectCallException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "activateAsynchConsumer",
                        new Object[] { this, Boolean.valueOf(deliverImmediately) });

        _localConsumerPoint.runIsolatedAsynch(deliverImmediately);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "activateAsynchConsumer");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.DestinationSession#getDestinationAddress()
     */
    @Override
    public SIDestinationAddress getDestinationAddress()
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled()) {
            SibTr.entry(
                        CoreSPIConsumerSession.tc,
                        "getDestinationAddress", this);
            SibTr.exit(CoreSPIConsumerSession.tc, "getDestinationAddress", _destAddr);
        }
        return _destAddr;
    }

    public LocalConsumerPoint getLocalConsumerPoint()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getLocalConsumerPoint");
            SibTr.exit(tc, "getLocalConsumerPoint", _localConsumerPoint);
        }
        return _localConsumerPoint;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#getId()
     * 
     * The id is generated by the Message processor. Each new Consumer is assigned
     * one at Construction time and the value is returned via the getId call.
     */
    @Override
    public long getId()
                    throws SISessionUnavailableException, SISessionDroppedException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "getId", this);

        _localConsumerPoint.checkNotClosed();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "getId", new Long(_consumerId));
        return _consumerId;
    }

    /**
     * Gets the id for the consumer without any checking.
     * 
     * @return the id for this consumer session
     */
    public long getIdInternal()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getIdInternal");
            SibTr.exit(tc, "getIdInternal", new Long(_consumerId));
        }
        return _consumerId;
    }

    /**
     * Retrieve the MPSubscription object that represents the subscription (durable or non-durable)
     * that this ConsumerSession is feeding from
     * 
     * This function is only available on locally homed subscriptions
     * 
     * Performing this against a queue consumer results in a SIDurableSubscriptionNotFoundException
     */
    @Override
    public MPSubscription getSubscription()
                    throws SIDurableSubscriptionNotFoundException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getSubscription");

        ConsumerDispatcher cd = (ConsumerDispatcher) _localConsumerPoint.getConsumerManager();
        MPSubscription mpSubscription = cd.getMPSubscription();
        if (mpSubscription == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
                SibTr.exit(CoreSPIConsumerSession.tc, "getSubscription", "SIDurableSubscriptionNotFoundException");
            throw new SIDurableSubscriptionNotFoundException(
                            nls.getFormattedMessage(
                                                    "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0146",
                                                    new Object[] {
                                                                  null,
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getSubscription", mpSubscription);
        return mpSubscription;
    }

    /**
     * Sets the id for this consumer.
     * 
     * @param id
     */
    void setId(long id)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setId", new Long(id));

        _consumerId = id;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setId");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPConsumerSession#relockMessageUnderCursor()
     */
    @Override
    public SIBusMessage relockMessageUnderAsynchCursor() throws SISessionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "relockMessageUnderAsynchCursor");

        SIBusMessage msg = null;

        msg = _localConsumerPoint.relockMessageUnderAsynchCursor();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "relockMessageUnderAsynchCursor", msg);

        return msg;
    }

    /**
     * @return boolean bifurcatable
     */
    protected boolean isBifurcatable()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isBifurcatable");
            SibTr.exit(tc, "isBifurcatable", Boolean.valueOf(_bifurcatable));
        }

        return _bifurcatable;
    }

    /**
     * @return boolean ignoreInitialIndoubts
     */
    protected boolean ignoreInitialIndoubts()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "ignoreInitialIndoubts");
            SibTr.exit(tc, "ignoreInitialIndoubts", Boolean.valueOf(_ignoreInitialIndoubts));
        }

        return _ignoreInitialIndoubts;
    }

    private void checkTransaction(SITransaction siTran, String msg) throws SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkTransaction", new Object[] { siTran, msg });

        if (siTran != null && !((TransactionCommon) siTran).isAlive())
        {
            SIIncorrectCallException e =
                            new SIIncorrectCallException(nls.getFormattedMessage(
                                                                                 msg,
                                                                                 new Object[] { _destAddr },
                                                                                 null));

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkTransaction", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkTransaction");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPDestinationSession#getUuid()
     */
    @Override
    public SIBUuid12 getUuid()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getUuid");
            SibTr.exit(tc, "getUuid", uuid);
        }
        return uuid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPConsumerSession#setBifurcatedConsumerCloseRedeliveryMode(boolean)
     */
    @Override
    public synchronized void setBifurcatedConsumerCloseRedeliveryMode(boolean bumpRedeliveryOnClose)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "setBifurcatedConsumerCloseRedeliveryMode", Boolean.valueOf(bumpRedeliveryOnClose));
            SibTr.exit(tc, "setBifurcatedConsumerCloseRedeliveryMode");
        }
        bumpRedeliveryOnBifurcatedClose = bumpRedeliveryOnClose;
    }

    protected synchronized boolean getBifurcatedConsumerCloseRedeliveryMode()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getBifurcatedConsumerCloseRedeliveryMode");
            SibTr.exit(tc, "getBifurcatedConsumerCloseRedeliveryMode", Boolean.valueOf(bumpRedeliveryOnBifurcatedClose));
        }
        return bumpRedeliveryOnBifurcatedClose;
    }

    @Override
    public void registerStoppableAsynchConsumerCallback(
                                                        StoppableAsynchConsumerCallback callback,
                                                        int maxActiveMessages,
                                                        long messageLockExpiry,
                                                        int maxBatchSize,
                                                        OrderingContext extendedMessageOrderingContext,
                                                        int maxSequentialFailures,
                                                        long hiddenMessageDelay)
                    throws SISessionUnavailableException, SISessionDroppedException, SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "registerStoppableAsynchConsumerCallback",
                        new Object[] {
                                      callback,
                                      Integer.valueOf(maxActiveMessages),
                                      Long.valueOf(messageLockExpiry),
                                      Integer.valueOf(maxBatchSize),
                                      extendedMessageOrderingContext,
                                      maxSequentialFailures,
                                      hiddenMessageDelay });

        registerStoppableAsynchConsumerCallback(
                                                callback,
                                                maxActiveMessages,
                                                messageLockExpiry,
                                                maxBatchSize,
                                                null,
                                                false,
                                                extendedMessageOrderingContext,
                                                maxSequentialFailures,
                                                hiddenMessageDelay);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerStoppableAsynchConsumerCallback");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPConsumerSession#registerStoppableAsynchConsumerCallback()
     */

    @Override
    public void registerStoppableAsynchConsumerCallback(
                                                        StoppableAsynchConsumerCallback callback,
                                                        int maxActiveMessages,
                                                        long messageLockExpiry,
                                                        int maxBatchSize,
                                                        Reliability unrecoverableReliability,
                                                        boolean inLine,
                                                        OrderingContext extendedMessageOrderingContext,
                                                        int maxSequentialFailures,
                                                        long hiddenMessageDelay)
                    throws SISessionUnavailableException, SISessionDroppedException, SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerStoppableAsynchConsumerCallback",
                        new Object[] { callback,
                                      maxActiveMessages,
                                      messageLockExpiry,
                                      maxBatchSize,
                                      unrecoverableReliability,
                                      inLine,
                                      extendedMessageOrderingContext,
                                      maxSequentialFailures,
                                      hiddenMessageDelay });

        //register the AsynchConsumer with the LCP
        _localConsumerPoint.registerStoppableAsynchConsumer(
                                                            callback,
                                                            maxActiveMessages,
                                                            messageLockExpiry,
                                                            maxBatchSize,
                                                            unrecoverableReliability,
                                                            inLine,
                                                            (OrderingContextImpl) extendedMessageOrderingContext,
                                                            null, // No callback busy lock is used for non-mediation consumers.
                                                            maxSequentialFailures,
                                                            hiddenMessageDelay);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerStoppableAsynchConsumerCallback");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#deregisterAsynchConsumerCallback()
     */
    @Override
    public void deregisterStoppableAsynchConsumerCallback()
                    throws SISessionUnavailableException, SISessionDroppedException,
                    SIErrorException,
                    SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "deregisterStoppableAsynchConsumerCallback", this);
        //Deregister the AsynchConsumer by registering null with the LCP
        //the other params don't matter
        registerStoppableAsynchConsumerCallback(null, 1, 0, 1, null, 0, 0);
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "deregisterStoppableAsynchConsumerCallback");
    }

    @Override
    public void unlockAll(boolean incrementUnlockCount)
                    throws SISessionUnavailableException, SISessionDroppedException,
                    SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SIIncorrectCallException
    {

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConsumerSession.tc, "unlockAll", new Object[] { this, incrementUnlockCount });

        //pass the unlockAll call on to the LCP
        try
        {
            if (incrementUnlockCount == true)
            {// if incrementUnlockCount is true call unlockAll()
                unlockAll();
            }
            else
            {// here the incrementUnlockCount will always be false
                _localConsumerPoint.unlockAll(incrementUnlockCount);
            }
        } catch (SIMPMessageNotLockedException e)
        {
            //No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(tc, e);
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConsumerSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConsumerSession.tc, "unlockAll", incrementUnlockCount);

    }
}
