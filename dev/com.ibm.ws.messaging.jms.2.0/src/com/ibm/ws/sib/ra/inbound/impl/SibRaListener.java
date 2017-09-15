/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.ra.inbound.impl;

import java.lang.reflect.Method;
import java.util.List;

import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ejbcontainer.mdb.MDBMessageEndpointFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.api.jms.service.JmsServiceFacade;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration;
import com.ibm.ws.sib.ra.inbound.SibRaReadAhead;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.LockedMessageEnumeration;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;
import com.ibm.wsspi.sib.core.StoppableAsynchConsumerCallback;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionAlreadyExistsException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;
import com.ibm.wsspi.sib.pacing.AsynchDispatchScheduler;
import com.ibm.wsspi.sib.pacing.AsynchResumeCallback;
import com.ibm.wsspi.sib.pacing.MessagePacingControl;
import com.ibm.wsspi.sib.pacing.MessagePacingControlFactory;

/**
 * Attaches to a destination to listen for messages and allocates them to a
 * dispatcher for delivery to a message-driven bean.
 */
abstract class SibRaListener implements StoppableAsynchConsumerCallback {

    /**
     * The connection to the messaging engine.
     */
    protected final SibRaMessagingEngineConnection _connection;

    /**
     * The session with which this listener is registered as a consumer.
     */
    protected final ConsumerSession _session;

    /**
     * Factory for creating selection criteria.
     */
    private final SelectionCriteriaFactory _selectionCriteriaFactory;

    /**
     * The maximum active message count. This is set to the maximum concurrency
     * defined on the endpoint configuration.
     */
    protected int _maxActiveMessages;

    /**
     * The destination this session is consuming from
     */
    protected final SIDestinationAddress _destinationAddress;

    /**
     * Indicates whether message ordering is on
     */
    protected final boolean _strictMessageOrdering;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
                    .getTraceComponent(SibRaListener.class);

    /**
     * Provides access to NLS enabled messages.
     */
    private static TraceNLS NLS = SibRaUtils.getTraceNls();

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaListener.class.getName();

    /**
     * The unrecoveredReliabilty set on the consumer session
     */
    protected Reliability _unrecoverableReliability;

    /**
     * Flag indicating whether messages that are of a lower or equal <code>reliabilty</code>
     * than the consumer sessions <code>unrecoveredReliability</code> should be deleted in <
     * code>consumeMessages</code>. This is <code>true</code> if the endpoint method is non-transactional.
     */
    protected final boolean _deleteUnrecoverableMessages;

    /**
     * The sequentialFailureThreshold value for the destination we are listening too
     */
    protected int _sequentialFailureThreshold;

    /**
     * The maxFailedDeliveries value for the destination we are listening too
     */
    protected int _maxFailedDeliveries;

    private final MessageEndpointFactory _messageEndpointFactory;

    private boolean _sessionStopped = false;

    /**
     * True if SIB message pacing says the session should be started. The
     * SibRaSingleProcessListener._workCountLock lock must be held when altering this.
     */
    protected volatile boolean sibPacingSessionStarted = true;
    /**
     * True if MPC message pacing says the session should be started. The
     * SibRaListener.this lock must be held when altering this.
     */
    private volatile boolean mpcPacingSessionStarted = true;
    /**
     * True if the session has been started. This variable is only affected by stop() and
     * start() calls.
     */
    private volatile boolean sessionStarted = false;
    /**
     * True when startSession() is already starting the session.
     */
    private volatile boolean sessionStarting = false;
    /** True when a callback is waiting to be notified */
    protected boolean callbackWaiting = false;
    /** True when we're inside consumeMessages() - which indicates we're in a stop/start dirty window */
    protected boolean insideConsumeMessages = false;
    /** A lock which the asyncCallback objects will wait upon */
    private final Object asyncResumeCallbackWaitLock = new Object() {};
    /** We use this lock when we're checking the sessionStarting variable */
    private final Object sessionStartingLock = new Object() {};
    /** This lock is used to check and alter the insideConsumeMessages variable */
    protected Object insideConsumeMessagesLock = new Object() {};

    /**
     * Constructor. Creates a consumer session and registers a callback with it.
     * 
     * @param connection
     *            a connection to the messaging engine
     * @param destination
     *            the destination to listen on
     * @param messageEndpointFactory
     *            the message endpoint factory
     * @throws ResourceException
     *             if a session could not be created or the callback
     *             could not be registered
     */
    SibRaListener(final SibRaMessagingEngineConnection connection,
                  final SIDestinationAddress destination, MessageEndpointFactory messageEndpointFactory) throws ResourceException {

        final String methodName = "SibRaListener";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                                                               destination, messageEndpointFactory });
        }

        _connection = connection;
        _destinationAddress = destination;
        _deleteUnrecoverableMessages = !connection
                        .getEndpointActivation().isEndpointMethodTransactional();
        _messageEndpointFactory = messageEndpointFactory;

        try
        {
            _selectionCriteriaFactory = JmsServiceFacade.getSelectionCriteriaFactory();

            /*
             * Create consumer session
             */
            _session = createSession(destination);
            _strictMessageOrdering = connection.getConnection().getDestinationConfiguration(destination).isStrictOrderingRequired();

            /*
             * Obtain the maximum active message count
             */
            _maxActiveMessages = connection.getEndpointConfiguration().getMaxConcurrency();

//        AJW - Keep this logic here as we might want it in the future if we decide that the RA handles the SMF value
//              We must put back in the nls messages as they were removed

            String exceptionDestination = _session.getConnection().getDestinationConfiguration(_destinationAddress).getExceptionDestination();

            // Get hold of the sequential message failure limit, set on the activation spec
            int configuredAutoStopSequentialMessageFailure = connection.getEndpointConfiguration().getAutoStopSequentialMessageFailure();

            // If we're using message ordering, or have no exception destination, and we've asked to stop endpoints on repeated message failure
            // (in which case configuredAutoStopSequentialMessageFailure will be non-zero) then override the sequential message failure to one
            // as specified in the docs.
            if ((_strictMessageOrdering || exceptionDestination == null) && (configuredAutoStopSequentialMessageFailure > 0))
            {
                // Override the configured sequentialMessageFailure to one
                configuredAutoStopSequentialMessageFailure = 1;
                // Output a new message indicating that the value has been overridden
                if (_strictMessageOrdering)
                {
                    SibTr.warning(TRACE, NLS.getFormattedMessage("MAXSEQUENTIALMESSAGEFAILURE_CONFIG_VALUE_CHANGED_CWSIV0906",
                                                                 new Object[] { Integer.valueOf(_connection.getEndpointConfiguration().getAutoStopSequentialMessageFailure()),
                                                                               Integer.valueOf(configuredAutoStopSequentialMessageFailure) }, null));
                }
                else
                {
                    SibTr.warning(TRACE, NLS.getFormattedMessage("MAXSEQUENTIALMESSAGEFAILURE_EXCEPTION_DESTINATION_CWSIV0907",
                                                                 new Object[] { Integer.valueOf(_connection.getEndpointConfiguration().getAutoStopSequentialMessageFailure()),
                                                                               Integer.valueOf(configuredAutoStopSequentialMessageFailure) }, null));
                }
            }

            /*
             * Register with session
             */
            int maxBatchSize = connection.getEndpointConfiguration()
                            .getMaxBatchSize();

            if (_strictMessageOrdering)
            {
                // Change the max work count so we only call one mdb at a time.
                _maxActiveMessages = 1;
                maxBatchSize = 1;

                // Output a message to indicate we are changing the value of max
                // concurrency
                SibTr.warning(TRACE, "MAXCONCURRENCY_CONFIG_VALUE_CHANGED_CWSIV1101", new Object[]
                { _connection.getEndpointConfiguration().getMaxConcurrency(), _maxActiveMessages });
            }

            MessagePacingControl mpc = MessagePacingControlFactory.getInstance();
            if (mpc != null && mpc.isActive())
            {
                maxBatchSize = mpc.overrideMaxBatchSize(connection.getEndpointConfiguration().getBusName(),
                                                        destination.getDestinationName(), maxBatchSize);
            }

            _sequentialFailureThreshold = connection.getEndpointConfiguration().getMaxSequentialMessageFailure();
            try
            {
                _maxFailedDeliveries = _connection.getConnection().getDestinationConfiguration(_session.getDestinationAddress()).getMaxFailedDeliveries();
            } catch (SIException exception)
            {
                FFDCFilter.processException(exception, CLASS_NAME + "."
                                                       + methodName, "1:350:1.68", this);
                if (TRACE.isEventEnabled())
                {
                    SibTr.exception(this, TRACE, exception);
                }

                // if we couldn't get hold of the maxfailedDeliveries then we should set the threshold for
                // sequential failures to -1 (i.e. off)
                _sequentialFailureThreshold = -1;
                if (TRACE.isDebugEnabled())
                {
                    SibTr.debug(this, TRACE, "_sequentialFailureThreshold is now -1");
                }
            }

            // Do not use the _maxActiveMessages variable but call the subclasses
            // implementation of getMaxActiveMessages in case they decide to not use
            // the _maxActiveMessages variable.
            final int maxActiveMessages = getMaxActiveMessages();
            final long messageLockExpiry = getMessageLockExpiry();

            try
            {
                long failingMsgDelay = connection.getEndpointConfiguration().getFailingMessageDelay();
                _session.registerStoppableAsynchConsumerCallback(this, maxActiveMessages,
                                                                 messageLockExpiry, maxBatchSize, null, configuredAutoStopSequentialMessageFailure, failingMsgDelay);
            } catch (SIIncorrectCallException exception)
            {
                // No FFDC code needed

                // If we have caught this exception it might be due to the fact that the server is a
                // pre v7 server. so lets try to register a normal asynch consumer.
                _session.registerAsynchConsumerCallback(this, maxActiveMessages,
                                                        messageLockExpiry, maxBatchSize, null);

                //TODO output message to say we aren't using a stoppable asynch consumer callback
            }

        } catch (final SIException exception)
        {
            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:393:1.68", this);

            /*
             * Cleanup any resources we may have created
             */
            close();

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceException(NLS
                            .getFormattedMessage("LISTENER_CREATION_CWSIV0900",
                                                 new Object[] { exception,
                                                               destination.getDestinationName(),
                                                               connection.getBusName(),
                                                               connection.getConnection() }, null),
                            exception);

        } catch (final SIErrorException exception)
        {
            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:415:1.68", this);

            /*
             * Cleanup any resources we may have created
             */
            close();

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceException(NLS
                            .getFormattedMessage("LISTENER_CREATION_CWSIV0900",
                                                 new Object[] { exception,
                                                               destination.getDestinationName(),
                                                               connection.getBusName(),
                                                               connection.getConnection() }, null),
                            exception);

        } catch (final Exception exception)
        {
            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:437:1.68", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceException(NLS.getFormattedMessage(
                                                                "SELECTION_FACTORY_CWSIV0901", new Object[] { exception },
                                                                null), exception);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * 
     * This method starts the consumer session allowing messages to
     * be sent to our consumeMessages method.
     * 
     * @throws ResourceException
     *             if the session could not be started
     */
    void startConsumer() throws ResourceException
    {
        final String methodName = "startConsumer";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        try {
            sessionStarted = true;
            startSession(false);
            _sessionStopped = false;

        } catch (final SIException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:476:1.68", this);

            /*
             * Cleanup any resources we may have created
             */
            close();

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceException(NLS
                            .getFormattedMessage("LISTENER_CREATION_CWSIV0900",
                                                 new Object[] { exception,
                                                               _session.getDestinationAddress().getDestinationName(),
                                                               _connection.getBusName(),
                                                               _connection.getConnection() }, null),
                            exception);

        } catch (final SIErrorException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:497:1.68", this);

            /*
             * Cleanup any resources we may have created
             */
            close();

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceException(NLS
                            .getFormattedMessage("LISTENER_CREATION_CWSIV0900",
                                                 new Object[] { exception,
                                                               _session.getDestinationAddress().getDestinationName(),
                                                               _connection.getBusName(),
                                                               _connection.getConnection() }, null),
                            exception);

        } catch (final Exception exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:518:1.68", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceException(NLS.getFormattedMessage(
                                                                "SELECTION_FACTORY_CWSIV0901", new Object[] { exception },
                                                                null), exception);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * Creates a consumer session to the given destination on the given
     * connection. For durable subscriptions this may involve creating or
     * re-creating the durable subscription first.
     * 
     * @param destination
     *            the destination to attach to
     * @return the session
     * @throws SIDurableSubscriptionAlreadyExistsException
     *             if the creation fails
     * @throws SIDurableSubscriptionNotFoundException
     *             if the creation fails
     * @throws SIDurableSubscriptionMismatchException
     *             if the creation fails
     * @throws SIConnectionDroppedException
     *             if the creation fails
     * @throws SIConnectionUnavailableException
     *             if the creation fails
     * @throws SIConnectionLostException
     *             if the creation fails
     * @throws SILimitExceededException
     *             if the creation fails
     * @throws SINotAuthorizedException
     *             if the creation fails
     * @throws SIDestinationLockedException
     *             if the creation fails
     * @throws SITemporaryDestinationNotFoundException
     *             if the creation fails
     * @throws SIResourceException
     *             if the creation fails
     * @throws SIErrorException
     *             if the creation fails
     * @throws SIIncorrectCallException
     *             if the creation fails
     * @throws SINotPossibleInCurrentConfigurationException
     *             if the creation fails
     */
    private final ConsumerSession createSession(
                                                final SIDestinationAddress destination)
                    throws SIDurableSubscriptionAlreadyExistsException,
                    SIDurableSubscriptionNotFoundException,
                    SIDurableSubscriptionMismatchException,
                    SIConnectionDroppedException, SIConnectionUnavailableException,
                    SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException, SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException, SIResourceException,
                    SIErrorException, SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException {

        final String methodName = "createSession";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { destination });
        }

        /*
         * Determine session properties
         */

        final SibRaEndpointConfiguration endpointConfiguration = _connection
                        .getEndpointConfiguration();

        final Reliability reliability = null;
        _unrecoverableReliability = _connection
                        .getEndpointActivation().isEndpointMethodTransactional() ? Reliability.NONE
                        : Reliability.BEST_EFFORT_NONPERSISTENT;

        final boolean enableReadAhead;
        final boolean noLocal = false;

        final DestinationType destinationType = endpointConfiguration
                        .getDestinationType();

        final String selector = endpointConfiguration.getMessageSelector();
        final String discriminator = endpointConfiguration.getDiscriminator();
        final SelectionCriteria selectionCriteria = _selectionCriteriaFactory
                        .createSelectionCriteria(discriminator, selector,
                                                 endpointConfiguration.getSelectorDomain());

        // Distributed will also set this to true in order that max active messages can be used
        // by processor. Max active messages is only adhered to when bifurcatable is set to true 
        // (It was initially only used by z/os), for now we'll always set this to true so that
        // processor will adhere to max active messages. We'll leave the variable here as opposed
        // to passing the value true directly in to the create consumer methods in case processor
        // change to use max active messages for non bifurcatable sessions.
        final boolean bifurcatable = true;
        final String alternateUser = null;

        /*
         * Create consumer session
         */

        final SICoreConnection connection = _connection.getConnection();

        ConsumerSession session = null;

        if (endpointConfiguration.isDurableSubscription()) {

            /*
             * Durable subscription
             */

            final String subscriptionName = endpointConfiguration
                            .getDurableSubscriptionName();

            final String durableSubscriptionHome = endpointConfiguration
                            .getDurableSubscriptionHome();

            final String subscriptionDurability = endpointConfiguration.getSubscriptionDurability();

            //default value non sharable
            boolean supportsMultipleConsumers=false;

            //So just check whether subscriptionDurability is shared durable subscription or not.
            //if yes set supportsMultipleConsumers to true. otherwise leave as it is i.e false
            if (subscriptionDurability.equalsIgnoreCase("DurableShared"))
                supportsMultipleConsumers = true;

            /*
             * By default, read ahead is only supported for unshared
             * subscriptions
             */

            if (SibRaReadAhead.DEFAULT.equals(endpointConfiguration
                            .getReadAhead())) {
                enableReadAhead = !supportsMultipleConsumers;
            } else {
                enableReadAhead = SibRaReadAhead.ON
                                .equals(endpointConfiguration.getReadAhead());
            }

            try {

                session = connection
                                .createConsumerSessionForDurableSubscription(
                                                                             subscriptionName, durableSubscriptionHome,
                                                                             destination, selectionCriteria,
                                                                             supportsMultipleConsumers, noLocal,
                                                                             reliability, enableReadAhead,
                                                                             _unrecoverableReliability, bifurcatable,
                                                                             alternateUser);

            } catch (final SIDurableSubscriptionNotFoundException exception) {

                // No FFDC code needed

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }

                /*
                 * Durable subscription doesn't exist so create it now and
                 * attempt to create consumer again
                 */

                connection.createDurableSubscription(subscriptionName,
                                                     durableSubscriptionHome, destination,
                                                     selectionCriteria, supportsMultipleConsumers, noLocal,
                                                     alternateUser);

                session = connection
                                .createConsumerSessionForDurableSubscription(
                                                                             subscriptionName, durableSubscriptionHome,
                                                                             destination, selectionCriteria,
                                                                             supportsMultipleConsumers, noLocal,
                                                                             reliability, enableReadAhead,
                                                                             _unrecoverableReliability, bifurcatable,
                                                                             alternateUser);

            } catch (final SIDurableSubscriptionMismatchException exception) {

                // No FFDC code needed

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }

                /*
                 * Durable subscription exists but doesn't match the given
                 * parameters so delete it, create a new one and attempt to
                 * create consumer again
                 */

                connection.deleteDurableSubscription(subscriptionName,
                                                     durableSubscriptionHome);

                connection.createDurableSubscription(subscriptionName,
                                                     durableSubscriptionHome, destination,
                                                     selectionCriteria, supportsMultipleConsumers, noLocal,
                                                     alternateUser);

                session = connection
                                .createConsumerSessionForDurableSubscription(
                                                                             subscriptionName, durableSubscriptionHome,
                                                                             destination, selectionCriteria,
                                                                             supportsMultipleConsumers, noLocal,
                                                                             reliability, enableReadAhead,
                                                                             _unrecoverableReliability, bifurcatable,
                                                                             alternateUser);

            }

        } else {

            /*
             * Queue, port, service or non-durable topic
             */

            //subscriptionName is needed for shared non-durable subscriptions
            final String subscriptionName = endpointConfiguration.getDurableSubscriptionName();

            //get subscriptionDurability from config. Possibilities are NonDurable/NonDurableShared
            final String subscriptionDurability = endpointConfiguration.getSubscriptionDurability();

            boolean isNonDurableShared = false;
            if (subscriptionDurability.equalsIgnoreCase("NonDurableShared"))
                isNonDurableShared = true;

            /*
             * By default, enable read ahead only for non-durable non-shared topics
             */

            if (SibRaReadAhead.DEFAULT.equals(endpointConfiguration
                            .getReadAhead())) {
                enableReadAhead = (DestinationType.TOPICSPACE
                                .equals(destinationType)) && !isNonDurableShared;
            } else {
                enableReadAhead = SibRaReadAhead.ON
                                .equals(endpointConfiguration.getReadAhead());
            }

            if (!isNonDurableShared) {

                // AllowMessageGathering is a config option on JMSQueues, if we have a value
                // then try to use it, if not use the method call that does not support
                // this field. When connecting to an older ME then we will get a SIIncorrectCallException
                // at which point we should revert to the older style method call.
                String allowMessageGathering = _connection.getEndpointConfiguration().getAllowMessageGathering();
                //Either Non-Durable Non Shared Topic or Queue consumer. Leaving the old code as it is 
                if (allowMessageGathering != null)
                {
                    boolean allow = (allowMessageGathering.equals(ApiJmsConstants.GATHER_MESSAGES_ON));
                    try
                    {
                        session = connection.createConsumerSession(destination,
                                                                   destinationType, selectionCriteria, reliability,
                                                                   enableReadAhead, noLocal, _unrecoverableReliability,
                                                                   bifurcatable, alternateUser, true, allow, null);
                    } catch (SIIncorrectCallException ex)
                    {
                        // No FFDC code needed
                        // This will happen when we connect to an old ME, in this case rever
                        // to the old method call.
                        session = connection.createConsumerSession(destination,
                                                                   destinationType, selectionCriteria, reliability,
                                                                   enableReadAhead, noLocal, _unrecoverableReliability,
                                                                   bifurcatable, alternateUser);
                    }
                }
                else
                {
                    session = connection.createConsumerSession(destination,
                                                               destinationType, selectionCriteria, reliability,
                                                               enableReadAhead, noLocal, _unrecoverableReliability,
                                                               bifurcatable, alternateUser);
                }
            } else {
                //Non Durable Shared consumer. Use the new API createSharedConsumerSession
                session = connection.createSharedConsumerSession(subscriptionName,
                                                                 destination,
                                                                 destinationType,
                                                                 selectionCriteria,
                                                                 reliability,
                                                                 enableReadAhead,
                                                                 true, //supportMultipleConsumers
                                                                 noLocal,
                                                                 _unrecoverableReliability,
                                                                 bifurcatable,
                                                                 alternateUser,
                                                                 true, //ignoreInitialIndoubts is true by default
                                                                 false, //allowMessageGathering is false for topic
                                                                 null // message control properties, passing null. Not used.
                );

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, session);
        }
        return session;

    }

    /**
     * Close this listener. Stops the consumer session and closes it.
     */
    final void close() {

        final String methodName = "close";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        try {

            if (_session != null) {
                sessionStarted = false;
                stopIfRequired();
                _session.close();
            }

        } catch (final SISessionDroppedException exception) {
            // No FFDC code needed
            // We shall ingnore this exception because it is due to the
            //  consumer session been close by MP as the destination
            //  has been deleted or the receiveAllowed=false been applied

        } catch (final SIException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:827:1.68", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

        } catch (final SIErrorException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:835:1.68", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Stop this listener. Stops the consumer session.
     */
    final void stop() {

        final String methodName = "stop";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        try {

            // Make sure we have a session and that it hasn't already been stopped
            // If an MDB is stopped by the system due to messages failing to be delivered 
            // the session will be stopped and the consumerSessionStopped method on the
            // StoppableAsynchConsumerCallback interface on this class will be called.
            // This will cause the _sessionStopped field to be set to true (which indicates
            // that this listener is in the process of shutting down). We do not then 
            // need to call stop again as the session is already stopped. When the MDB is 
            // resumed a new SibRaListener object is created and so we don't need to reset 
            // the _sessionStopped variable.
            if ((_session != null) && (!_sessionStopped))
            {
                sessionStarted = false;
                stopIfRequired();
            }

        } catch (final SIException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:878:1.68", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

        } catch (final SIErrorException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:886:1.68", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * The consumeMessages method is invoked by the message processor with an enumeration
     * containing one or more messages locked to this consumer. The consumeMessages method
     * will either call the internalConsumeMessage method directly, or, if XD wishes to suspend
     * the processing this method will be called then XD decides to resume processing. This
     * method retrieves the messages from the enumeration. It then schedules a piece of work
     * that will create the dispatcher on a new thread.
     * 
     * @param lpe
     *            the enumeration of locked messages
     * @param asynchDispatchScheduler
     *            the XD asynchDispatchScheduler, either the cached version or the non-cached
     *            version depending on whether we have suspended or not
     */
    public abstract void internalConsumeMessages(LockedMessageEnumeration lpe,
                                                 AsynchDispatchScheduler asynchDispatchScheduler);

    /**
     * Invoked by the message processor with an enumeration containing one or
     * more messages locked to this consumer. Retrieves the messages from the
     * enumeration. Schedules a piece of work that will create the dispatcher on
     * a new thread.
     * 
     * @param lockedMessages
     *            the enumeration of locked messages
     */
    @Override
    public synchronized void consumeMessages(final LockedMessageEnumeration lockedMessages) {

        final String methodName = "consumeMessages";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, lockedMessages);
        }

        // PM74709.dev - don't acquire "insideConsumeMessagesLock" when startSession() is already starting the
        // session, this may create a deadlock.
        if (sessionStarting) {
            if (TRACE.isDebugEnabled()) {
                SibTr.debug(this, TRACE, "Return this method, as startSession() is in progress.");
            }
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                SibTr.exit(this, TRACE, methodName);
            }
            return;
        }

        synchronized (insideConsumeMessagesLock)
        {
            insideConsumeMessages = true;
        }

        MessagePacingControl mpc = MessagePacingControlFactory.getInstance();
        if (mpc != null && mpc.isActive()) {

            // we are about to check if XD wants to suspend a context.  We need to give each context
            // its own callback as it is the context that is suspended and not the listener as a whole;
            // although we do map an XD suspend to a session stop.
            AsynchResumeCallbackImpl asyncResumeCallback = new AsynchResumeCallbackImpl();
            AsynchDispatchScheduler aysnDispatchScheduler = mpc.preAsynchDispatch(_connection
                            .getEndpointConfiguration().getBusName(),
                                                                                  _destinationAddress.getDestinationName(),
                                                                                  asyncResumeCallback);

            // we don't need to worry that XD will resume before we have cached the LME
            // because we hold SibRaListener lock (resume runnable takes SibRaListener
            // lock ensuring we have cached the LME, and it is visible
            if (aysnDispatchScheduler.suspendAsynchDispatcher()) {
                mpcPacingSessionStarted = false;
                asyncResumeCallback.createCachedEnumeration(lockedMessages, aysnDispatchScheduler);
            } else {

                // We are not suspended so pass in the asynchdispatchScheduler we
                // just obtained
                internalConsumeMessages(lockedMessages, aysnDispatchScheduler);
            }
        }
        else {
            // MessagingPacing is not active so we don't have a asynchDispatchScheduler
            internalConsumeMessages(lockedMessages, null);
        }

        // No need to check if startSession() is in progress before acquiring "insideConsumeMessagesLock"
        // as there will not be an attempt to start the session when "insideConsumeMessages" is true.

        synchronized (insideConsumeMessagesLock)
        {
            insideConsumeMessages = false;

            // Check to see if we need to stop the session. This is needed here because any
            // pacing logic that kicked in whilst insideConsumeMessages was true, wouldn't
            // have actually stopped a session - just altered flags. So we'll do the stop here
            // if we need to.
            try {
                stopIfRequired();
            } catch (final SIException exception) {
                FFDCFilter.processException(exception, CLASS_NAME + "."
                                                       + methodName, "1:979:1.68", this);
                if (TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
            } catch (final SIErrorException exception) {
                FFDCFilter.processException(exception, CLASS_NAME + "."
                                                       + methodName, "1:986:1.68", this);
                if (TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Indicates whether the resource adapter is using bifurcated consumer
     * sessions. If so, the <code>ConsumerSession</code> created by the
     * listener needs to be bifurcatable.
     * 
     * @return flag indicating whether bifurcated sessions are used
     */
    abstract boolean isSessionBifurcated();

    /**
     * Returns the maximum number of active messages that should be associated
     * with this listener at any one time.
     * 
     * @return the maximum number of active messages
     */
    abstract int getMaxActiveMessages();

    /**
     * Returns the expiry time for message locks.
     * 
     * @return the message lock expiry
     */
    abstract long getMessageLockExpiry();

    /**
     * Converts a list of messages to an array of message handles.
     * 
     * @param messages
     *            the messages
     * @return the array of message handles
     */
    static SIMessageHandle[] getMessageHandles(final List messages) {

        final SIMessageHandle[] messageHandles = new SIMessageHandle[messages
                        .size()];
        for (int i = 0; i < messageHandles.length; i++) {
            final SIBusMessage message = (SIBusMessage) messages.get(i);
            messageHandles[i] = message.getMessageHandle();
        }
        return messageHandles;

    }

    /**
     * Returns a string representation of this object.
     * 
     * @return a string representation
     */
    @Override
    public final String toString() {

        return getStringGenerator().getStringRepresentation();

    }

    /**
     * Perform any processing on a message that is required whilst creating the
     * cached locked message enumeration.
     * 
     * @param message The message to process
     * @param lockedMessages The locked message enumeration the message is in. The cursor
     *            will currently be pointing at the message passed as a parameter.
     * @throws SIIncorrectCallException
     * @throws SIResourceException
     * @throws SIMessageNotLockedException
     * @throws SILimitExceededException
     * @throws SIConnectionLostException
     * @throws SIConnectionUnavailableException
     * @throws SISessionUnavailableException
     * @throws SIConnectionDroppedException
     * @throws SISessionDroppedException
     */
    protected abstract void processCachedMessage(SIBusMessage message,
                                                 LockedMessageEnumeration lockedMessages) throws SISessionDroppedException,
                    SIConnectionDroppedException, SISessionUnavailableException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SILimitExceededException, SIMessageNotLockedException,
                    SIResourceException, SIIncorrectCallException;

    /**
     * Returns a string generator containing the fields for this class.
     * 
     * @return a string generator
     */
    protected SibRaStringGenerator getStringGenerator() {

        final SibRaStringGenerator generator = new SibRaStringGenerator(this);
        generator.addParent("connection", _connection);
        generator.addField("session", _session);
        return generator;

    }

    /*
     * Deactivate the endpoint (pause the endpoint) via the JCA Mbean.
     * 
     * This method is invoked when the sequential message failure threshold
     * has been hit
     */
    private void processEndpointDeActivation()
    {
        final String methodName = "processEndpointDeActivation";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] {});
        }

        String endpointName = null;
        if (_messageEndpointFactory instanceof MDBMessageEndpointFactory)
        {
            try {
                endpointName = ((MDBMessageEndpointFactory) _messageEndpointFactory).getMDBKey().toString();
            } catch (Exception e) {
                FFDCFilter.processException(e,
                                            "com.ibm.ws.sib.ra.inbound.impl.SibRaListener",
                                            "1059", this);
            }

            // Output a message that we are about to pause the endpoint
            SibTr.warning(TRACE, "MESSAGE_ENDPOINT_PAUSED_AUTONOMICALLY_CWSIV0902",
                          new Object[] { endpointName, _destinationAddress });

            // call bus sec action
            try
            {
                // Use reflection here as the class should not be loaded for the stand alone
                // RA (this method should not be driven by the standalone RA and calling it
                // may invoke the use of BusSecurityAction which is not bundled with the stand alone
                // RA.
                Class<?> busAction = Class.forName("com.ibm.ws.sib.ra.inbound.impl.SibRaBusSecurityAction");
                Method performBusSecurityAction = busAction.getDeclaredMethod("performBusSecurityAction",
                                                                              new Class[] { String.class, SIDestinationAddress.class });
                performBusSecurityAction.invoke(null, new Object[] { endpointName, _destinationAddress });
            } catch (Exception ex)
            {
                FFDCFilter.processException(ex, CLASS_NAME + "." + methodName, "1:1128:1.68");
                SibTr.error(TRACE, "INVOKE_MBEAN_EXCEPTION__CWSIV0903",
                            new Object[] { endpointName, _destinationAddress, ex });
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    @Override
    public void consumerSessionStopped()
    {
        final String methodName = "consumerSessionStopped";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, new Object[] {});
        }
        _sessionStopped = true;

        processEndpointDeActivation();
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * Request that the session starts. This will only occur if both SIB pacing AND MPC
     * pacing agree that the session should start.
     * 
     * @param deliverImmediately is true if a message consumer can call consumeMessages() before
     *            start() has returned
     * @throws SIException
     */
    protected void startSession(boolean deliverImmediately) throws SIException
    {
        final String methodName = "startSession";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { this, new Boolean(deliverImmediately) });
        }

        boolean sessionStartingByThisThread = false;

        // Check to see if we need to do the session start ourselves
        synchronized (sessionStartingLock)
        {
            if (!sessionStarting)
            {
                /**
                 * Start *if*:
                 * SIB pacing says we should start
                 * XD says we should start
                 * Our global start switch is on
                 */
                if (sibPacingSessionStarted && mpcPacingSessionStarted && sessionStarted)
                {
                    sessionStarting = true;
                    sessionStartingByThisThread = true;
                }
            }

            if (TRACE.isDebugEnabled())
            {
                SibTr.debug(TRACE, methodName, "sibPacingSessionStarted: " + sibPacingSessionStarted
                                               + "\nmpcPacingSessionStarted: " + mpcPacingSessionStarted
                                               + "\ncallbackWaiting: " + callbackWaiting
                                               + "\nsessionStarted: " + sessionStarted
                                               + "\nsessionStartingByThisThread: " + sessionStartingByThisThread);
            }
        }

        if (sessionStartingByThisThread)
        {
            /*
             * We'll only start if there's no callback waiting. If a callback is waiting then
             * we'll simply wake that up, and the callback will perform the start for us.
             * Otherwise we'll do the start here
             */
            boolean startRequired = false;
            synchronized (asyncResumeCallbackWaitLock)
            {
                if (callbackWaiting)
                {
                    asyncResumeCallbackWaitLock.notifyAll();
                    callbackWaiting = false;
                }
                else
                    startRequired = true;
            }
            if (startRequired)
                _session.start(deliverImmediately);

            sessionStarting = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * Stop the session if it has been requested. It's only valid to do this *if* you're a thread
     * which already has the processor AsynchConsumerBusy lock
     * 
     * @throws SIException
     */
    protected void stopIfRequired() throws SIException
    {
        final String methodName = "stopIfRequired";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { this });
        }

        if (TRACE.isDebugEnabled())
        {
            SibTr.debug(TRACE, methodName, "sibPacingSessionStarted: " + sibPacingSessionStarted + "\nmpcPacingSessionStarted: " + mpcPacingSessionStarted + "\nsessionStarted: "
                                           + sessionStarted);
        }

        if (!sibPacingSessionStarted || !mpcPacingSessionStarted || !sessionStarted)
        {
            _session.stop();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * Use this class to correlate locked messages with the asynch scheduler by implementing resume().
     * We do this now because XD's notion of suspending refers to the context that we get back from
     * preAsynchDispatch(), and not the listener itself. So we can end up using a suspended context
     * to process messages in a resume() call.
     * 
     * Users of this class must ensure they hold the SibRaListener lock to ensure visibility of the
     * cached LME.
     */
    private class AsynchResumeCallbackImpl implements AsynchResumeCallback, Runnable {

        // this callback's messages
        private SibRaLockedMessageEnumeration _cachedEnumeration = null;

        // this callback's scheduler
        private AsynchDispatchScheduler _cachedAsynchDispatchScheduler;

        /**
         * set this callback up with the lme and scheduler
         * 
         * @param lme
         * @param asynchDispatchScheduler
         */
        public void createCachedEnumeration(LockedMessageEnumeration lme
                                            , AsynchDispatchScheduler asynchDispatchScheduler) {

            final String methodName = "createCachedEnumeration";
            if (TRACE.isEntryEnabled()) {
                SibTr.entry(this, TRACE, methodName, new Object[] { lme, asynchDispatchScheduler });
            }

            _cachedEnumeration = new SibRaLockedMessageEnumeration();
            _cachedAsynchDispatchScheduler = asynchDispatchScheduler;

            SIBusMessage message = null;

            try {

                // Go through all the messages in the locked message enumeration
                while ((message = lme.nextLocked()) != null) {

                    // Add the message to the cached enumeration
                    _cachedEnumeration.add(message);

                    // This can be overridden by the derived classes if they need to perform
                    // any work on the message.
                    processCachedMessage(message, lme);

                }

            } catch (final Throwable throwable) {

                FFDCFilter.processException(throwable, CLASS_NAME + "." + methodName,
                                            "1:1310:1.68", this);
                SibTr.error(TRACE, "RETRIEVE_MESSAGES_CWSIV1100", new Object[] {
                                                                                throwable, lme });
            }

            if (TRACE.isEntryEnabled()) {
                SibTr.exit(this, TRACE, methodName);
            }

        }

        /**
         * This method is called by XD when they wish to resume the consumer session.
         * The messages that were cached when the session was stopped are processed and
         * then the consumer session is started.
         */
        @Override
        public void resume() {

            final String methodName = "resume";
            if (TRACE.isEntryEnabled()) {
                SibTr.entry(this, TRACE, methodName, this);
            }

            // PK60008 We cannot safely synchronize on the listener lock this thread, as this is an SPI interface
            // which could be called at any time.  Deadlocks have been encountered, both internal
            // within SIBus and from interactions with locks obtained by XD code.
            // For this reason, we dispatch the work in the following runnable onto another thread.

            // Execute the callback work asynchronously
            try {

                Thread raListenerThread = new Thread(this);
                raListenerThread.setName("SIBRAListenerThread");
                raListenerThread.start();
            } catch (IllegalStateException ie) {
                // No FFDC code needed
                if (TRACE.isDebugEnabled())
                    SibTr.exception(TRACE, ie);
            }

            if (TRACE.isEntryEnabled()) {
                SibTr.exit(this, TRACE, methodName);
            }
        }

        @Override
        public void run() {

            final String methodName = "resume$Runnable";
            if (TRACE.isEntryEnabled()) {
                SibTr.entry(this, TRACE, methodName);
            }

            mpcPacingSessionStarted = true;

            // If SIB message pacing is currently throttling, we need to wait. We'll
            // be notified when it's time to start again.
            synchronized (asyncResumeCallbackWaitLock) {
                while (!sibPacingSessionStarted)
                {
                    try
                    {
                        callbackWaiting = true;
                        asyncResumeCallbackWaitLock.wait();
                    } catch (InterruptedException e) {
                        // No FFDC code needed
                    }
                }
            }

            // Synchronize (on the outer class) while we are dispatching any cached LME,
            // this ensures any XD invoked suspend completes and caches an outstanding LME.
            synchronized (SibRaListener.this) {
                // Do we have a cached LME to dispatch?
                if (_cachedEnumeration != null) { // should not be possible!
                    // Pass in the cached versions of the LME and AsynchDispatchScheduler
                    if (TRACE.isDebugEnabled()) {
                        SibTr.debug(this, TRACE, "resuming with", new Object[] { _cachedEnumeration, _cachedAsynchDispatchScheduler });
                    }
                    internalConsumeMessages(_cachedEnumeration, _cachedAsynchDispatchScheduler);
                    // Release our references to the messages, now they are dispatched.
                    _cachedEnumeration = null;
                    _cachedAsynchDispatchScheduler = null;
                }
            }

            // Drop the synchronization before we call start on the session.
            // This is because in a comms case other code holds comms locks before
            // taking the SibRaListener lock, so we could hit an A-B B-A deadlock.

            // for every suspend, the session is stopped so for every resume the session is started,
            // thus ensuring that we never end up stopped and never started as suspends always preceed
            // resumes.

            try {
                _session.start(true);
            } catch (final SIException exception) {

                FFDCFilter.processException(exception, CLASS_NAME
                                                       + "." + methodName, "1:1419:1.68", this);
                if (TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }

            } catch (final SIErrorException exception) {

                FFDCFilter.processException(exception, CLASS_NAME
                                                       + "." + methodName, "1:1427:1.68", this);
                if (TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }

            }

            if (TRACE.isEntryEnabled()) {
                SibTr.exit(this, TRACE, methodName);
            }
        }

    }
}
