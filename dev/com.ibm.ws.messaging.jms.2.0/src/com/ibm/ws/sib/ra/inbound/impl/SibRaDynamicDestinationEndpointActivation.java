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

import com.ibm.websphere.ras.TraceComponent;
//Sanjay Liberty Changes
import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.ra.SibRaEngineComponent;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointInvoker;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.DestinationAvailability;
import com.ibm.wsspi.sib.core.DestinationListener;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;

/**
 * Endpoint activation used when the configuration does not explicitly specify a
 * destination from which messages should be consumed. Instead, the resource
 * adapter will attach to all destinations of the given type on the given bus.
 * If no bus is named then it will attach to destinations on all buses.
 */
final class SibRaDynamicDestinationEndpointActivation extends
        SibRaEndpointActivation {

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaDynamicDestinationEndpointActivation.class);

    /**
     * The component to use for trace for the
     * <code>SibRaConnectionListener</code> inner class.
     */
    private static final TraceComponent LISTENER_TRACE = SibRaUtils
            .getTraceComponent(SibRaDestinationListener.class);

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaDynamicDestinationEndpointActivation.class
            .getName();

    private static final String FFDC_PROBE_1 = "1";

    private static final String FFDC_PROBE_2 = "2";

    private static final String FFDC_PROBE_3 = "3";

    private static final String FFDC_PROBE_4 = "4";

   
    /**
     * Constructor.
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
     *             if the activation fails
     */
    SibRaDynamicDestinationEndpointActivation(
            final SibRaResourceAdapterImpl resourceAdapter,
            final MessageEndpointFactory messageEndpointFactory,
            final SibRaEndpointConfiguration endpointConfiguration,
            final SibRaEndpointInvoker endpointInvoker)
            throws ResourceException {

        super(resourceAdapter, messageEndpointFactory, endpointConfiguration,
                endpointInvoker);

        final String methodName = "SibRaDynamicDestinationEndpointActivation";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] {
                    resourceAdapter, messageEndpointFactory,
                    endpointConfiguration, endpointInvoker });
        }

        /*
         * Register as a messaging engine listener and process initial messaging
         * engines
         */

        final JsMessagingEngine[] activeMessagingEngines = SibRaEngineComponent
                .registerMessagingEngineListener(this, _endpointConfiguration
                        .getBusName());

        for (int i = 0; i < activeMessagingEngines.length; i++) {

            addMessagingEngine(activeMessagingEngines[i]);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Connects to the given messaging engine. Registers a destination listener
     * and creates listeners for each of the current destinations.
     *
     * @param messagingEngine
     *            the messaging engine to connect to
     */
    synchronized protected void addMessagingEngine(
            final JsMessagingEngine messagingEngine) {

        final String methodName = "addMessagingEngine";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, messagingEngine);
        }

        SibRaMessagingEngineConnection connection = null;
        try {

            /*
             * Get a connection for the messaging engine
             */

            connection = getConnection(messagingEngine);
            final SICoreConnection coreConnection = connection.getConnection();

            if (coreConnection instanceof SICoreConnection) {

                /*
                 * Create destination listener
                 */

                final DestinationListener destinationListener = new SibRaDestinationListener(
                        connection, _messageEndpointFactory);

                /*
                 * Determine destination type
                 */

                final DestinationType destinationType = _endpointConfiguration
                        .getDestinationType();

                /*
                 * Register destination listener
                 */

                final SIDestinationAddress[] destinations = coreConnection
                        .addDestinationListener(null,
                                destinationListener,
                                destinationType,
                                DestinationAvailability.RECEIVE);

                /*
                 * Create a listener for each destination ...
                 */

                for (int j = 0; j < destinations.length; j++) {

                    try {

                        connection.createListener(destinations[j], _messageEndpointFactory);

                    } catch (final ResourceException exception) {

                        FFDCFilter.processException(exception, CLASS_NAME + "."
                                + methodName, FFDC_PROBE_1, this);
                        SibTr.error(TRACE, "CREATE_LISTENER_FAILED_CWSIV0803",
                                new Object[] { exception,
                                        destinations[j].getDestinationName(),
                                        messagingEngine.getName(),
                                        messagingEngine.getBusName() });

                    }

                }

            }

        } catch (final SIException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                    + methodName, FFDC_PROBE_2, this);
            SibTr.error(TRACE, "ADD_DESTINATION_LISTENER_FAILED_CWSIV0804",
                    new Object[] { exception, messagingEngine.getName(),
                            messagingEngine.getBusName() });
            closeConnection(messagingEngine);

        } catch (final ResourceException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                    + methodName, FFDC_PROBE_3, this);
            SibTr.error(TRACE, "CREATE_CONNECTION_FAILED_CWSIV0801",
                    new Object[] { exception, messagingEngine.getName(),
                            messagingEngine.getBusName() });
            closeConnection(messagingEngine);

        }

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

        /*
         * Output a message to the console if the exception is not an
         * SINotPossibleInCurrentConfigurationException since this is thrown
         * when destinations are deleted and we would then expect this
         * exception to be thrown.
         *
         * d364674 - if a destination is deleted while we are listening then
         *   we will receive session dropped here. Use a warning message rather
         *   than an error (there may be other causes for session dropped that
         *   we still want to warn about).
         */

        if (throwable instanceof SISessionDroppedException)
        {
          final SIDestinationAddress destination = session
          .getDestinationAddress();
          SibTr.warning(TRACE, "SESSION_DROPPED_CWSIV0808", new Object[] {
              destination.getDestinationName(),
              connection.getConnection().getMeName(),
              destination.getBusName(),
              throwable});

        } else if (! (throwable instanceof SINotPossibleInCurrentConfigurationException))
        {
            final SIDestinationAddress destination = session
                .getDestinationAddress();
            SibTr.error(TRACE, "SESSION_ERROR_CWSIV0806", new Object[] { throwable,
                destination.getDestinationName(),
          connection.getConnection().getMeName(),
          destination.getBusName(), this });
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
    void connectionError(final SibRaMessagingEngineConnection connection,
            final SIException exception) {

        /*
         * Output a message to the admin console and close the connection
         */

        SibTr.error(TRACE, "CONNECTION_ERROR_CWSIV0807", new Object[] {
                exception, connection.getConnection().getMeName(), this });

        synchronized (_connections) {

            _connections.remove(connection.getConnection().getMeUuid());

        }

        connection.close();

    }

    /**
     * Indicates that the messaging engine for the given connection has
     * terminated.
     *
     * @param connection
     *            the connection
     */
    void messagingEngineTerminated(
            final SibRaMessagingEngineConnection connection) {

        /*
         * This method should not be called as the JsEngineComponent should be
         * notified first
         */

    }

    /**
     * Indicates that the messaging engine for the given connection is
     * quiescing.
     *
     * @param connection
     *            the connection
     */
    void messagingEngineQuiescing(
            final SibRaMessagingEngineConnection connection) {

        /*
         * This method should not be called as the JsEngineComponent should be
         * notified first
         */

    }

    /**
     * Notifies the listener that the given messaging engine is initializing.
     *
     * @param messagingEngine
     *            the messaging engine
     */
    public void messagingEngineInitializing(
            final JsMessagingEngine messagingEngine) {

        // Do nothing

    }

    /**
     * Notifies the listener that the given messaging engine is being destroyed.
     *
     * @param messagingEngine
     *            the messaging engine
     */
    public void messagingEngineDestroyed(final JsMessagingEngine messagingEngine) {

        // Do nothing

    }

    /**
     * Notifies the listener that the given messaging engine has been reloaded
     * following a configuration change to the bus on which the engine resides.
     * Does nothing. Any destinations that have been removed will just close
     * their consumers. Any new destinations will be notified to the destination
     * listener.
     *
     * @param engine
     *            the messaging engine that has been reloaded
     */
    public void messagingEngineReloaded(final JsMessagingEngine engine) {

        // Do nothing

    }

    /**
     * Destination listener to create new listeners as and when new destinations
     * become available.
     */
    private static final class SibRaDestinationListener implements
            DestinationListener {

        /**
         * The connection on which to create listeners.
         */
        private final SibRaMessagingEngineConnection _connection;

        private MessageEndpointFactory _messageEndpointFactory;

        /**
         * Constructor.
         *
         * @param connection
         *            the connection on which to create listeners
         */
        private SibRaDestinationListener(
                final SibRaMessagingEngineConnection connection,
                MessageEndpointFactory messageEndpointFactory) {

            final String methodName = "SibRaDestinationListener";
            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.entry(this, LISTENER_TRACE, methodName, new Object[] {connection, messageEndpointFactory});
            }

            _connection = connection;
            _messageEndpointFactory = messageEndpointFactory;

            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.exit(this, LISTENER_TRACE, methodName);
            }

        }

        /**
         * Called when a new destination is created. Creates a listener for the
         * destination.
         *
         * @param connection
         *            the connection on which the listener is registered
         * @param destination
         *            the new destination
         * @param availability
         *            the availability of the destination
         */
        public void destinationAvailable(final SICoreConnection connection,
                final SIDestinationAddress destination,
                final DestinationAvailability availability) {

            final String methodName = "destinationAvailable";
            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.entry(this, LISTENER_TRACE, methodName, new Object[] {
                        connection, destination, availability });
            }

            try {

                _connection.createListener(destination, _messageEndpointFactory);

            } catch (final ResourceException exception) {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, FFDC_PROBE_4, this);
                SibTr.error(TRACE, "CREATE_LISTENER_FAILED_CWSIV0805",
                        new Object[] { exception,
                                destination.getDestinationName(),
                                connection.getMeName(),
                                destination.getBusName() });

            }

            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.exit(this, LISTENER_TRACE, methodName);
            }

        }

        public String toString() {

            final SibRaStringGenerator generator = new SibRaStringGenerator(
                    this);
            return generator.getStringRepresentation();

        }

    }

}
