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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.IllegalStateException;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.DestinationAliasDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.SIBExceptionBase;
import com.ibm.ws.sib.admin.SIBExceptionDestinationNotFound;
import com.ibm.ws.sib.ra.SibRaEngineComponent;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointInvoker;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.DestinationType;

/**
 * Endpoint activation used when the configuration explicitly specifies a
 * destination from which messages should be consumed.
 */
final class SibRaStaticDestinationEndpointActivation extends
        SibRaEndpointActivation {

    /**
     * Random number generator for picking a messaging engine to connect to.
     */
    private static final Random _random = new Random();

    /**
     * The connection to a remote messaging engine, if any.
     */
    private SibRaMessagingEngineConnection _remoteConnection;

    /**
     * Flag indicating whether the required destination is localized by any
     * local messaging engines.
     */
    private boolean _remoteDestination;

    /**
     * Flag indicating whether a remote messaging engine is needed.
     */
    private boolean _remoteMessagingEngine;

    /**
     * The timer to use when attempting to reconnect to remote messaging engines
     * and destinations.
     */
    private Timer _timer;

    /**
     * The retry interval for reconnection attempts (30 seconds).
     */
    private int RETRY_INTERVAL = 30000;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaStaticDestinationEndpointActivation.class);

    /**
     * Provides access to NLS enabled messages.
     */
    private static TraceNLS NLS = SibRaUtils.getTraceNls();

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaStaticDestinationEndpointActivation.class
            .getName();

    private static final String FFDC_PROBE_1 = "1";

    private static final String FFDC_PROBE_2 = "2";

    private static final String FFDC_PROBE_3 = "3";

    private static final String FFDC_PROBE_4 = "4";

    private static final String FFDC_PROBE_5 = "5";

    private static final String FFDC_PROBE_6 = "6";

    private static final String FFDC_PROBE_7 = "7";

   
    /**
     * Constructor. Determines the transactionality of the endpoint method and
     * registers a messaging engine listener.
     *
     * @param resourceAdapter
     *            the resource adapter on which the activation was created
     * @param messageEndpointFactory
     *            the message endpoint factory for this activation
     * @param endpointConfiguration
     *            the endpoint configuration for this activation
     * @param endpointInvoker
     *            the endpoint invoker for this activation
     * @throws ResourceException
     *             if the activation was not successful
     */
    SibRaStaticDestinationEndpointActivation(
            final SibRaResourceAdapterImpl resourceAdapter,
            final MessageEndpointFactory messageEndpointFactory,
            final SibRaEndpointConfiguration endpointConfiguration,
            final SibRaEndpointInvoker endpointInvoker)
            throws ResourceException {

        super(resourceAdapter, messageEndpointFactory, endpointConfiguration,
                endpointInvoker);

        final String methodName = "SibRaStaticDestinationEndpointActivation";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] {
                    resourceAdapter, messageEndpointFactory,
                    endpointConfiguration, endpointInvoker });
        }

        /*
         * Create a timer for use during reconnection attempts
         */

        _timer = resourceAdapter.getBootstrapContext().createTimer();

        /*
         * Determine whether there are any local messaging engines defined
         */

        final JsMessagingEngine[] localMessagingEngines = SibRaEngineComponent
                .getMessagingEngines(_endpointConfiguration.getBusName());

        if (0 == localMessagingEngines.length) {

            /*
             * Mark destination as remote as it may not be localized by the
             * messaging engine that we connect to
             */

            _remoteDestination = true;
            _remoteMessagingEngine = true;
            createRemoteListener(true);

        } else {

            /*
             * Register as a messaging engine listener and process any active
             * messaging engines
             */

            final JsMessagingEngine[] activeMessagingEngines = SibRaEngineComponent
                    .registerMessagingEngineListener(this,
                            _endpointConfiguration.getBusName());

            for (int i = 0; i < activeMessagingEngines.length; i++) {

                final JsMessagingEngine messagingEngine = activeMessagingEngines[i];

                if (isListenerRequired(messagingEngine)) {

                    createListener(messagingEngine);

                }

            }

            /*
             * Put out a warning message if no connections were created
             */

            if (_connections.size() == 0) {

                SibTr.warning(TRACE, "NO_ACTIVE_MES_CWSIV0759",
                        new Object[] { _endpointConfiguration.getBusName() });

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, methodName);
        }

    }

    /**
     * If the messaging engine is one of the required set or there are no
     * required messaging engines and there are no other connections then create
     * a listener.
     *
     * @param messagingEngine
     *            the messaging engine to connect to
     * @throws ResourceException
     *             if determining the required messaging engines fails or if
     *             creation of a listener fails
     */
    synchronized protected void addMessagingEngine(
            final JsMessagingEngine messagingEngine) throws ResourceException {

        final String methodName = "addMessagingEngine";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, messagingEngine);
        }

        if (isListenerRequired(messagingEngine)) {

            createListener(messagingEngine);

            SibTr.info(TRACE, "NEW_CONSUMER_CWSIV0764", new Object[] {
                    _endpointConfiguration.getBusName(),
                    messagingEngine.getName(),
                    _endpointConfiguration.getDestination()
                            .getDestinationName() });

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Called on deactivation of this endpoint.
     */
    void deactivate() {

        final String methodName = "deactivate";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        super.deactivate();

        synchronized(this) {

          if (_remoteConnection != null) {

              _remoteConnection.close();

          }

        }

        _timer.cancel();    //PK54585

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Determines if a listener is required for the given messaging engine.
     *
     * @param messagingEngine
     *            the messaging engine
     * @return <code>true</code> if the messaging engine is in the required
     *         set or the set is empty and there are currently no connections,
     *         otherwise <code>false</code>
     * @throws ResourceException
     *             if the required messaging engines could not be determined
     */
    private boolean isListenerRequired(final JsMessagingEngine messagingEngine)
            throws ResourceException {

        final String methodName = "isListenerRequired";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, messagingEngine);
        }

        boolean listenerRequired = false;

        /*
         * Determine the required messaging engines
         */

        final Set requiredMessagingEngines = getRequiredMessagingEngines(messagingEngine);

        if (requiredMessagingEngines.size() > 0) {

            /*
             * A particular set of messaging engines is required - create a
             * listener if this is one of them
             */

            if (requiredMessagingEngines.contains(messagingEngine.getUuid()
                    .toString())) {

                listenerRequired = true;

            }

        } else {

            /*
             * A connection to any one messaging engine is required - create a
             * listener if we don't already have a connection to another
             * messaging engine
             */

            if (_connections.size() == 0) {

                listenerRequired = true;

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, Boolean
                    .valueOf(listenerRequired));
        }
        return listenerRequired;

    }

    /**
     * Validates the given destination information with administration.
     *
     * @param messagingEngine
     *            the messaging engine with which to validate
     * @param busName
     *            the name of the bus for the destination
     * @param destinationName
     *            the name of the destination
     * @param destinationType
     *            the type of the destination
     * @return the UUID for the destination if valid
     * @throws NotSupportedException
     *             if the destination does not exist, is foreign, is of the
     *             incorrect type or is an alias to a destination that is not
     *             valid
     * @throws ResourceAdapterInternalException
     *             if the destination definition is not of a known type of
     *             cannot be obtained
     */
    private static String validateDestination(
            final JsMessagingEngine messagingEngine, final String busName,
            final String destinationName, final DestinationType destinationType)
            throws NotSupportedException, ResourceAdapterInternalException {

        final String methodName = "validateDestination";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, methodName, new Object[] { messagingEngine,
                    busName, destinationName, destinationType });
        }

        /*
         * The destination may contain a null bus name so, for clarity in
         * messages, use the name from the messaging engine if this is the case
         */
        final String resolvedBusName = (((busName == null) || ""
                .equals(busName)) ? messagingEngine.getBusName() : busName);

        final String uuid;

        try {

            /*
             * Obtain destination definition
             */

            final BaseDestinationDefinition destinationDefinition = messagingEngine
                    .getSIBDestination(busName, destinationName);

            /*
             * Foreign destinations are not supported
             */

            if (destinationDefinition.isForeign()) {

                throw new NotSupportedException(NLS.getFormattedMessage(
                        ("FOREIGN_DESTINATION_CWSIV0754"), new Object[] {
                                destinationName, resolvedBusName }, null));

            }

            if (destinationDefinition.isLocal()) {

                /*
                 * Local destinations must be of the correct type
                 */

                final DestinationDefinition localDefinition = (DestinationDefinition) destinationDefinition;

                if (destinationType
                        .equals(localDefinition.getDestinationType())) {

                    uuid = destinationDefinition.getUUID().toString();

                } else {

                    /*
                     * The destination resolved to the wrong type
                     */

                    throw new NotSupportedException(NLS.getFormattedMessage(
                            ("INCORRECT_TYPE_CWSIV0755"), new Object[] {
                                    destinationName, resolvedBusName,
                                    destinationType,
                                    localDefinition.getDestinationType() },
                            null));

                }

            } else if (destinationDefinition.isAlias()) {

                /*
                 * Recursively check an alias destination
                 */

                final DestinationAliasDefinition aliasDefinition = (DestinationAliasDefinition) destinationDefinition;
                uuid = validateDestination(messagingEngine, aliasDefinition
                        .getTargetBus(), aliasDefinition.getTargetName(),
                        destinationType);

            } else {

                /*
                 * Unknown destination type
                 */

                throw new ResourceAdapterInternalException(
                        NLS
                                .getFormattedMessage(
                                        ("UNKNOWN_TYPE_CWSIV0756"),
                                        new Object[] { destinationName,
                                                resolvedBusName }, null));

            }

        } catch (final SIBExceptionDestinationNotFound exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                    + methodName, FFDC_PROBE_1);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(TRACE, exception);
            }
            throw new NotSupportedException(NLS.getFormattedMessage(
                    ("NOT_FOUND_CWSIV0757"), new Object[] { destinationName,
                            resolvedBusName }, null), exception);

        } catch (final SIBExceptionBase exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                    + methodName, FFDC_PROBE_2);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(TRACE, exception);
            }
            throw new ResourceAdapterInternalException(NLS
                    .getFormattedMessage(("UNEXPECTED_EXCEPTION_CWSIV0758"),
                            new Object[] { destinationName, resolvedBusName,
                                    exception }, null), exception);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, methodName, uuid);
        }
        return uuid;

    }

    /**
     * Determines which of the local messaging engines the resource adapter
     * should try and obtain connections to.
     *
     * @param messagingEngine
     *            the messaging engine to use for configuration information
     * @return the set of required messaging engines
     * @throws NotSupportedException
     *             if there are no configured messaging engines or the
     *             destination is invalid
     * @throws ResourceAdapterInternalException
     *             if destination configuration cannot be obtained
     */
    private Set getRequiredMessagingEngines(
            final JsMessagingEngine messagingEngine)
            throws NotSupportedException, ResourceAdapterInternalException {

        final String methodName = "getRequiredMessagingEngines";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        final Set<String> requiredMessagingEngines = new HashSet<String>();

        /*
         * Obtain the local messaging engines configured on the given bus
         */

        final JsMessagingEngine[] localMessagingEngines = SibRaEngineComponent
                .getMessagingEngines(_endpointConfiguration.getBusName());

        /*
         * Validate the specified destination
         */

        final String destinationUuid = validateDestination(messagingEngine,
                _endpointConfiguration.getDestination().getBusName(),
                _endpointConfiguration.getDestination().getDestinationName(),
                _endpointConfiguration.getDestinationType());

        if (DestinationType.TOPICSPACE == _endpointConfiguration
                .getDestinationType()) {

            if (_endpointConfiguration.isDurableSubscription()) {

                /*
                 * For a durable subscription, if the durable subscription home
                 * is local then only connect to that
                 */

                _remoteDestination = true;

                for (int i = 0; i < localMessagingEngines.length; i++) {

                    if (localMessagingEngines[i].getName()
                            .equals(
                                    _endpointConfiguration
                                            .getDurableSubscriptionHome())) {

                        requiredMessagingEngines.add(localMessagingEngines[i]
                                .getUuid().toString());
                        _remoteDestination = false;
                        break;

                    }

                }

            } else {

                /*
                 * For a non-durable subscription the destination is never
                 * remote
                 */

                _remoteDestination = false;

            }

        } else {

            /*
             * For a destination other than a topic, connect to all local
             * messaging engines that have a localisation
             */

            final Set destinationLocalisations;
            try {

                destinationLocalisations = messagingEngine
                        .getSIBDestinationLocalitySet(_endpointConfiguration
                                .getDestination().getBusName(), destinationUuid);

            } catch (final SIBExceptionBase exception) {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, FFDC_PROBE_3, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceAdapterInternalException(NLS
                        .getFormattedMessage(
                                ("DESTINATION_LOCALITY_CWSIV0753"),
                                new Object[] { exception, destinationUuid },
                                null), exception);

            }

            _remoteDestination = true;

            for (int i = 0; i < localMessagingEngines.length; i++) {

                final String meUuid = localMessagingEngines[i].getUuid()
                        .toString();
                if (destinationLocalisations.contains(meUuid)) {

                    requiredMessagingEngines.add(meUuid);
                    _remoteDestination = false;

                }

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, methodName, requiredMessagingEngines);
        }
        return requiredMessagingEngines;

    }

    /**
     * Creates a listener to the configured destination on the given messaging
     * engine.
     *
     * @param messagingEngine
     *            the messaging engine
     * @throws ResourceException
     *             if the creation of the listener fails
     */
    private void createListener(final JsMessagingEngine messagingEngine)
            throws ResourceException {

        final String methodName = "createListener";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, messagingEngine);
        }

        final SibRaMessagingEngineConnection connection = getConnection(messagingEngine);
        createListener(connection);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Creates a listener to the configured destination using the given
     * connection.
     *
     * @param connection
     *            the connection to the messaging engine
     * @throws ResourceException
     *             if the creation of the listener fails
     */
    private void createListener(final SibRaMessagingEngineConnection connection)
            throws ResourceException {

        final String methodName = "createListener";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, connection);
        }

        final SIDestinationAddress destination = _endpointConfiguration
                .getDestination();

        try {

            connection.createListener(destination, _messageEndpointFactory);

        } catch (final IllegalStateException exception) {

            // No FFDC code needed
            throw exception;

        } catch (final ResourceException exception) {

            // No FFDC code needed

            if (_remoteDestination) {

                SibTr.warning(TRACE, SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS, "ATTACH_FAILED_CWSIV0769", new Object[] {
                        destination.getDestinationName(),
                        _endpointConfiguration.getBusName(), this, exception });

                /*
                 * Try again 30 seconds later
                 */

                _timer.schedule(new TimerTask() {

                    public void run() {

                        try {

                            createListener(connection);

                        } catch (final ResourceException retryException) {
                            // No FFDC code needed
                        }

                    }
                }, RETRY_INTERVAL);

            } else {

                throw exception;

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Overrides the parent method so that, if the messaging engine that is
     * stopping was the any one messaging engine to which a connection had been
     * made, if there are still active messaging engines then connect to one of
     * those instead.
     *
     * @param messagingEngine
     *            the messaging engine that is stopping
     * @param mode
     *            the mode with which the engine is stopping
     */
    public synchronized void messagingEngineStopping(
            final JsMessagingEngine messagingEngine, final int mode) {

        final String methodName = "messagingEngineStopping";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] {
                    messagingEngine, mode });
        }

        super.messagingEngineStopping(messagingEngine, mode);

        try {

            /*
             * If a connection to any messaging engine is required, there are
             * now no connections but there are still other messaging engines
             * available, then connect to another one at random
             */

            final Set requiredMessagingEngines = getRequiredMessagingEngines(messagingEngine);
            final JsMessagingEngine[] activeMessagingEngines = SibRaEngineComponent
                    .getActiveMessagingEngines(_endpointConfiguration
                            .getBusName());

            if ((requiredMessagingEngines.size() == 0)
                    && (_connections.size() == 0)) {

                if (activeMessagingEngines.length > 0) {

                    final int randomIndex = _random
                            .nextInt(activeMessagingEngines.length);

                    createListener(activeMessagingEngines[randomIndex]);

                } else {

                    SibTr.info(TRACE, "LAST_ME_CWSIV0768", new Object[] {
                            messagingEngine.getName(),
                            messagingEngine.getBus(), this });

                }

            }

        } catch (final ResourceException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                    + methodName, FFDC_PROBE_4, this);
            SibTr.error(TRACE, "MESSAGING_ENGINE_STOPPING_CWSIV0765",
                    new Object[] { exception, messagingEngine.getName(),
                            messagingEngine.getBusName() });

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Notifies the listener that the given messaging engine is initializing.
     *
     * @param messagingEngine
     *            the messaging engine
     */
    public synchronized void messagingEngineInitializing(
            final JsMessagingEngine messagingEngine) {

        final String methodName = "messagingEngineInitializing";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, messagingEngine);
        }

        if (_remoteMessagingEngine
                && messagingEngine.getBusName().equals(
                        _endpointConfiguration.getBusName())) {

            /*
             * A remote connection is no longer required. Stop any timers
             * attempting to connect and close any existing remote connection.
             */

            SibTr.info(TRACE, "ME_INITIALIZING_CWSIV0778", new Object[] {
                    messagingEngine.getName(),
                    _endpointConfiguration.getBusName(), this });

            _remoteMessagingEngine = false;
            if (_remoteConnection != null) {
                _remoteConnection.close();
            }
            _timer.cancel();

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Notifies the listener that the given messaging engine is being destroyed.
     *
     * @param messagingEngine
     *            the messaging engine
     */
    public void messagingEngineDestroyed(final JsMessagingEngine messagingEngine) {

        final String methodName = "messagingEngineDestroyed";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, messagingEngine);
        }

        /*
         * If there are no longer any local messaging engines on the required
         * bus, switch to a remote messaging engine
         */

        final JsMessagingEngine[] localMessagingEngines = SibRaEngineComponent
                .getMessagingEngines(_endpointConfiguration.getBusName());

        if (0 == localMessagingEngines.length) {

            /*
             * The last local messaging engine for the required bus has been
             * destroyed; attempt to connect to a remote messaging engine
             * instead
             */

            SibTr.info(TRACE, "ME_DESTROYED_CWSIV0779", new Object[] {
                    messagingEngine.getName(),
                    _endpointConfiguration.getBusName(), this });

            _remoteDestination = true;

            // Synchronized so that other threads will see that we're connecting to a remote ME
            synchronized(this) {
                _remoteMessagingEngine = true;
            }

            createRemoteListenerDeactivateOnException(true);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Notifies the listener that the given messaging engine has been reloaded
     * following a configuration change to the bus on which the engine resides.
     *
     * @param engine
     *            the messaging engine that has been reloaded
     */
    public void messagingEngineReloaded(final JsMessagingEngine engine) {

        final String methodName = "messagingEngineReloaded";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, engine);
        }

        boolean engineStarted = false;
        // Get all the active ME's that are on the same bus as the reloaded ME
        JsMessagingEngine[] engines = SibRaEngineComponent.getActiveMessagingEngines(engine.getBusName());

        for (int i = 0; i < engines.length; i++)
        {
          // Now check if the reloaded ME is part of the active ME's (i.e. a started ME
          if (engines[i].equals(engine))
          {
            //It is active so lets mark the engine as started
            engineStarted = true;
            break;
          }
        }

        // Only do this logic on a started engine. If the engine isn't started then when
        // the start is called we will do the same logic of trying to create a listener,
        // so there is no need to wait here till it starts.
        if (engineStarted)
        {
          try
          {

            /*
             * If there is not already a connection to the messaging engine and
             * a listener is still required given the new configuration, create
             * one now
             */

            if (!_connections.containsKey(engine.getUuid().toString())
                    && isListenerRequired(engine)) {

                createListener(engine);

            }

          }
          catch (final ResourceException exception)
          {

            // No FFDC code needed

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

            SibTr.error(TRACE, "RELOAD_FAILED_CWSIV0773", new Object[] {
                    exception, engine.getName(), engine.getBusName(), this });

          }
        } // end of if(engineStarted)

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Indicates that an error has been detected on the given session.
     *
     * @param connection
     *            the parent connection for the session
     * @param session
     *            the session
     * @param throwable
     *            the error
     */
    void sessionError(final SibRaMessagingEngineConnection connection,
            final ConsumerSession session, final Throwable throwable) {

        final String methodName = "sessionError";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                    session });
        }

        final SIDestinationAddress destination = session
                .getDestinationAddress();

        if (_remoteDestination) {

            /*
             * Output a message to the admin console and attempt to create a new
             * listener
             */

            SibTr.warning(TRACE, "CONSUMER_FAILED_CWSIV0770", new Object[] {
                    destination.getDestinationName(),
                    _endpointConfiguration.getBusName(), this, throwable });

            try {

                createListener(connection);

            } catch (final ResourceException exception) {

                // No FFDC code needed

            }

        } else {

            if (SibRaEngineComponent.isMessagingEngineReloading(connection
                    .getConnection().getMeUuid())) {

                /*
                 * Output a warning to the admin console and close the
                 * connection - will be recreated during engineReloaded if
                 * required
                 */

                SibTr.warning(TRACE, "FAILURE_DURING_RELOAD_CWSIV0774",
                        new Object[] { throwable,
                                destination.getDestinationName(),
                                connection.getConnection().getMeName(),
                                destination.getBusName(), this });

                closeConnection(connection.getConnection().getMeUuid());

            } else {

                /*
                 * Output a message to the admin console and deactivate the
                 * endpoint
                 */

                SibTr.error(TRACE, "SESSION_ERROR_CWSIV0766", new Object[] {
                        throwable, destination.getDestinationName(),
                        connection.getConnection().getMeName(),
                        destination.getBusName(), this });

                deactivate();

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Indicates that an error has been detected on the given connection.
     *
     * @param connection
     *            the connection
     * @param exception
     *            the error
     */
    synchronized void connectionError(
            final SibRaMessagingEngineConnection connection,
            final SIException exception) {

        final String methodName = "connectionError";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                    exception });
        }

        if (_remoteMessagingEngine) {

            if (connection.equals(_remoteConnection)) {

                /*
                 * We have lost our remote connection, output a warning to the
                 * admin console, close the connection and schedule an attempt
                 * to create a new one
                 */

                SibTr.warning(TRACE, "CONNECTION_ERROR_CWSIV0776",
                        new Object[] { connection.getConnection().getMeName(),
                                _endpointConfiguration.getBusName(), this,
                                exception });

                _remoteConnection.close();
                _remoteConnection = null;

                scheduleCreateRemoteListener();

            } else {

                /*
                 * We have already closed the remote connection
                 */
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(this, TRACE, "Connection " + connection
                            + " not equal to current remote connection "
                            + _remoteConnection);
                }

            }

        } else {

            /*
             * Output a message to the admin console and deactivate the endpoint
             */

            SibTr.error(TRACE, "CONNECTION_ERROR_CWSIV0767", new Object[] {
                    exception, connection.getConnection().getMeName(), this });
            deactivate();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * Indicates that the messaging engine for the given connection has
     * terminated.
     *
     * @param connection
     *            the connection
     */
    synchronized void messagingEngineTerminated(
            final SibRaMessagingEngineConnection connection) {

        final String methodName = "messagingEngineTerminated";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, connection);
        }

        if (connection.equals(_remoteConnection)) {

            /*
             * We have lost our remote connection, output a warning to the admin
             * console, close the connection and schedule an attempt to create a
             * new one
             */

            SibTr.warning(TRACE, "ME_TERMINATED_CWSIV0780", new Object[] {
                    connection.getConnection().getMeName(),
                    _endpointConfiguration.getBusName(), this });

            _remoteConnection.close();
            _remoteConnection = null;
            scheduleCreateRemoteListener();

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Indicates that the messaging engine for the given connection is
     * quiescing.
     *
     * @param connection
     *            the connection
     */
    synchronized void messagingEngineQuiescing(
            final SibRaMessagingEngineConnection connection) {

        final String methodName = "messagingEngineQuiescing";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, connection);
        }

        if (connection.equals(_remoteConnection)) {

            /*
             * We have lost our remote connection, output a warning to the admin
             * console, close the connection and schedule an attempt to create a
             * new one
             */

            SibTr.warning(TRACE, "ME_QUIESCING_CWSIV0781", new Object[] {
                    connection.getConnection().getMeName(),
                    _endpointConfiguration.getBusName(), this });

            _remoteConnection.close();
            _remoteConnection = null;
            scheduleCreateRemoteListener();

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Creates a connection to any messaging engine on the required bus and then
     * registers a listener. If the attempt fails with an
     * <code>SIResourceException</code> schedules another attempt after the
     * retry interval.
     *
     * @param initialAttempt
     *            indicates that this is the first attempt at creating the
     *            remote listener and that a warning should be output if it
     *            fails and retry is started
     * @throws ResourceException
     *             if the attempt fails for some reason other than with a
     *             <code>SIResourceException</code>
     */
    synchronized private void createRemoteListener(boolean initialAttempt)
            throws ResourceException {

        final String methodName = "createRemoteListener";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, Boolean
                    .valueOf(initialAttempt));
        }

        /*
         * Check that a remote connection is still required
         */

        if (_remoteMessagingEngine) {

            try {

                _remoteConnection = new SibRaMessagingEngineConnection(this,
                        _endpointConfiguration.getBusName());

                createListener(_remoteConnection);

            } catch (final SIResourceException exception) {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, FFDC_PROBE_5, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }

                /*
                 * This may be a transient failure, schedule a retry attempt
                 */

                if (!initialAttempt) {

                    SibTr.warning(TRACE, "CONNECT_FAILED_CWSIV0775",
                            new Object[] {
                                    _endpointConfiguration.getDestination()
                                            .getDestinationName(),
                                    _endpointConfiguration.getBusName(), this,
                                    exception });

                }

                scheduleCreateRemoteListener();

            } catch (final SIException exception) {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, FFDC_PROBE_6, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                        "CONNECT_FAILED_CWSIV0782", new Object[] {
                                _endpointConfiguration.getDestination()
                                        .getDestinationName(),
                                _endpointConfiguration.getBusName(), this,
                                exception }, null), exception);

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Schedules the creation of a remote listener to take place after the retry
     * interval.
     */
    private void scheduleCreateRemoteListener() {

        final String methodName = "scheduleCreateRemoteListener";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        _timer.schedule(new TimerTask() {

            public void run() {

                createRemoteListenerDeactivateOnException(false);

                synchronized(SibRaStaticDestinationEndpointActivation.this) {

                    if (_remoteConnection != null) {

                        /*
                         * Successfully created a connection
                         */

                        SibTr.info(TRACE, "CONNECTED_CWSIV0777", new Object[] {
                                _remoteConnection.getConnection().getMeName(),
                                _endpointConfiguration.getDestination()
                                        .getDestinationName(),
                                _endpointConfiguration.getBusName(),
                                SibRaStaticDestinationEndpointActivation.this });

                    }

                }

            }
        }, RETRY_INTERVAL);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Creates a connection to any messaging engine on the required bus and then
     * registers a listener. If the attempt fails with an
     * <code>SIResourceException</code> schedules another attempt after the
     * retry interval. If the attempt fails with some other exception then the
     * endpoint is deactivated.
     *
     * @param initialAttempt
     *            indicates that this is the first attempt at creating the
     *            remote listener and that a warning should be output if it
     *            fails and retry is started
     */
    private void createRemoteListenerDeactivateOnException(
            final boolean initialAttempt) {

        final String methodName = "createRemoteListenerDeactivateOnException";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, Boolean
                    .valueOf(initialAttempt));
        }

        try {

            createRemoteListener(initialAttempt);

        } catch (final ResourceException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                    + methodName, FFDC_PROBE_7, this);

            SibTr.error(TRACE, "CONNECT_FAILED_CWSIV0783", new Object[] {
                    _endpointConfiguration.getDestination()
                            .getDestinationName(),
                    _endpointConfiguration.getBusName(), this, exception });

            deactivate();

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
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
        // The sync here is probably unnecessary (in that a dirty value would probably
        // be accepable), but I'd like to allow findbugs to spot other inconsistent usages
        synchronized(this) {

          generator.addField("remoteConnection", _remoteConnection);

        }
        generator.addField("remoteDestination", _remoteDestination);
        generator.addField("timer", _timer);
        return generator;

    }

}
