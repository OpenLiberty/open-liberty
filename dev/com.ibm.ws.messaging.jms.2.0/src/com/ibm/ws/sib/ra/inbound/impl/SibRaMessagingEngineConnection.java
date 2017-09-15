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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.resource.ResourceException;
import javax.resource.spi.IllegalStateException;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ejbcontainer.mdb.MDBMessageEndpointFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration;
import com.ibm.ws.sib.ra.inbound.SibRaMessageDeletionMode;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AbstractConsumerSession;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;
import com.ibm.wsspi.sib.core.SICoreConnectionFactorySelector;
import com.ibm.wsspi.sib.core.SICoreConnectionListener;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.selector.FactoryType;
import com.ibm.wsspi.sib.core.trm.SibTrmConstants;

/**
 * Represents a connection to a messaging engine.
 */
final class SibRaMessagingEngineConnection {

    /**
     * The endpoint activation that led to the creation of this connection.
     */
    private final SibRaEndpointActivation _endpointActivation;

    /**
     * The configuration for the endpoint activation.
     */
    private final SibRaEndpointConfiguration _endpointConfiguration;

    /**
     * The bus containing the messaging engine.
     */
    private final String _busName;

    /**
     * The connection to the messaging engine.
     */
    private final SICoreConnection _connection;

    /**
     * The listener for connection events.
     */
    private final SibRaConnectionListener _connectionListener;

    /**
     * A map from destination addresses to listeners.
     */
    private final Map<SIDestinationAddress, SibRaListener> _listeners =
                    Collections.synchronizedMap(new HashMap<SIDestinationAddress, SibRaListener>());

    /**
     * Flag indicating whether this connection has been closed.
     */
    private volatile boolean _closed = false;

    private final AtomicInteger _dispatcherCount = new AtomicInteger(0);

    /**
     * Flag indicating whether code is being executed on z/OS.
     */
/*
 * private static final boolean IS_ZOS = PlatformHelperFactory
 * .getPlatformHelper().isZOS();
 */

    /**
     * The component to use for trace for the
     * <code>SibRaMessagingEngineConnection</code> inner class.
     */
    private static final TraceComponent TRACE = SibRaUtils
                    .getTraceComponent(SibRaMessagingEngineConnection.class);

    /**
     * The component to use for trace for the
     * <code>SibRaConnectionListener</code> inner class.
     */
    private static final TraceComponent LISTENER_TRACE = SibRaUtils
                    .getTraceComponent(SibRaConnectionListener.class);

    /**
     * The component to use for trace for the
     * <code>SibRaConnectionEventThread</code> inner class.
     */
    private static final TraceComponent EVENT_TRACE = SibRaUtils
                    .getTraceComponent(SibRaConnectionEventThread.class);

    /**
     * Provides access to NLS enabled messages.
     */
    private static TraceNLS NLS = SibRaUtils.getTraceNls();

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaMessagingEngineConnection.class
                    .getName();

    /**
     * Constructor. Creates a connection to the given messaging engine.
     * 
     * @param endpointActivation
     *            the endpoint activation for which this connection is being
     *            created
     * @param messagingEngine
     *            the messaging engine to connect to
     * @throws ResourceException
     *             if a connection cannot be created
     */
    SibRaMessagingEngineConnection(
                                   final SibRaEndpointActivation endpointActivation,
                                   final JsMessagingEngine messagingEngine) throws ResourceException {

        final String methodName = "SibRaMessagingEngineConnection";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] {
                                                               endpointActivation, messagingEngine });
        }

        _endpointActivation = endpointActivation;
        _endpointConfiguration = endpointActivation.getEndpointConfiguration();
        _busName = messagingEngine.getBusName();

        /*
         * Obtain message processor SICoreConnectionFactory
         */
        final SICoreConnectionFactory factory = (SICoreConnectionFactory) messagingEngine
                        .getMessageProcessor();

        try {

            /*
             * Create an connection
             */
            _connection = createConnection(factory, _endpointConfiguration
                            .getUserName(), _endpointConfiguration.getPassword(), null, _busName);

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(this, TRACE, "Obtained Connection <" + _connection + ">");
            }

            /*
             * Add connection event listener
             */
            _connectionListener = new SibRaConnectionListener();
            _connection.addConnectionListener(_connectionListener);

        } catch (final SIAuthenticationException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:279:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceException(NLS.getFormattedMessage(
                                                                "CREATE_CONNECTION_CWSIV0954", new Object[] { exception,
                                                                                                             factory }, null), exception);

        } catch (SINotAuthorizedException sinae) {

            FFDCFilter.processException(sinae, CLASS_NAME + "."
                                               + methodName, "1:290:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, sinae);
            }

            if (_endpointConfiguration.getUserName() == null) {
                // If we have no username and we are here then there is a
                // configuration problem with the authentication alias on the activation spec
                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "CREATE_CONNECTION_CWSIV0958", new Object[] { sinae,
                                                                                                                 messagingEngine.getUuid(),
                                                                                                                 _endpointConfiguration.getActivationSpec() }, null), sinae);
            } else {
                // There is a correct authentication alias but the user is not authorized
                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "CREATE_CONNECTION_CWSIV0959", new Object[] { sinae,
                                                                                                                 messagingEngine.getUuid(),
                                                                                                                 _endpointConfiguration.getActivationSpec() }, null), sinae);
            }

        } catch (final Exception wsse) {
            FFDCFilter.processException(wsse, CLASS_NAME + "." + methodName, "1:312:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, wsse);
            }

            throw new ResourceException(NLS.getFormattedMessage(
                                                                "CREATE_CONNECTION_CWSIV0950", new Object[] { wsse }, null), wsse);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Constructor. Creates a connection to the given local messaging engine.
     * 
     * @param endpointActivation
     *            the endpoint activation for which this connection is being
     *            created
     * @param busName
     *            the name of the bus on which the messaging engine resides
     * @param meUuid
     *            the UUID of the messaging engine to connect to
     * @throws ResourceException
     *             if a connection cannot be created
     */
    SibRaMessagingEngineConnection(
                                   final SibRaEndpointActivation endpointActivation,
                                   final String busName, final String meUuid) throws ResourceException {

        final String methodName = "SibRaMessagingEngineConnection";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] {
                                                               endpointActivation, busName, meUuid });
        }

        _endpointActivation = endpointActivation;
        _endpointConfiguration = endpointActivation.getEndpointConfiguration();
        _busName = busName;

        try {

            final SICoreConnectionFactory factory = SICoreConnectionFactorySelector
                            .getSICoreConnectionFactory(FactoryType.TRM_CONNECTION);

            /*
             * Create TRM properties
             */
            final Map<String, String> properties = new HashMap<String, String>();
            properties.put(SibTrmConstants.BUSNAME, busName);
            // 382822 Removed the following line - we should not restrict the SR
            // to only connecting to an ME on the local server. It is possible
            // the MDB is consuming from a remote ME.
//            properties.put(SibTrmConstants.CONNECTION_PROXIMITY,
//                    SibTrmConstants.CONNECTION_PROXIMITY_SERVER);
            properties.put(SibTrmConstants.TARGET_TYPE,
                           SibTrmConstants.TARGET_TYPE_MEUUID);
            properties.put(SibTrmConstants.TARGET_GROUP, meUuid);
            properties.put(SibTrmConstants.TARGET_SIGNIFICANCE,
                           SibTrmConstants.TARGET_SIGNIFICANCE_REQUIRED);

            final String targetTransportChain = _endpointConfiguration.getTargetTransportChain();
            if ((targetTransportChain != null) && !"".equals(targetTransportChain)) {
                properties.put(SibTrmConstants.TARGET_TRANSPORT_CHAIN,
                               targetTransportChain);
            }

            // SIB0130.ra Added Provider endpoints to the map
            properties.put(SibTrmConstants.PROVIDER_ENDPOINTS,
                           _endpointConfiguration.getProviderEndpoints());

            /*
             * Create an connection
             */
            _connection = createConnection(factory, _endpointConfiguration
                            .getUserName(), _endpointConfiguration.getPassword(),
                                           properties, busName);

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(this, TRACE, "Obtained Connection <" + _connection + ">");
            }

            /*
             * Add connection event listener
             */
            _connectionListener = new SibRaConnectionListener();
            _connection.addConnectionListener(_connectionListener);

        } catch (final SIAuthenticationException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:428:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceException(NLS.getFormattedMessage(
                                                                "CREATE_CONNECTION_CWSIV0955", new Object[] { exception,
                                                                                                             meUuid }, null), exception);
        } catch (SINotAuthorizedException sinae) {

            FFDCFilter.processException(sinae, CLASS_NAME + "."
                                               + methodName, "1:438:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, sinae);
            }

            if (_endpointConfiguration.getUserName() == null) {
                // If we have no username and we are here then there is a
                // configuration problem with the authentication alias on the activation spec
                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "CREATE_CONNECTION_CWSIV0958", new Object[] { sinae,
                                                                                                                 meUuid, _endpointConfiguration.getActivationSpec() }, null), sinae);
            } else {
                // There is a correct authentication alias but the user is not authorized
                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "CREATE_CONNECTION_CWSIV0959", new Object[] { sinae,
                                                                                                                 meUuid, _endpointConfiguration.getActivationSpec() }, null), sinae);
            }

        } catch (final SIException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:471:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceException(NLS.getFormattedMessage(
                                                                "CREATE_CONNECTION_CWSIV0951", new Object[] { exception,
                                                                                                             meUuid }, null), exception);

        } catch (final SIErrorException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:482:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceException(NLS.getFormattedMessage(
                                                                "CREATE_CONNECTION_CWSIV0951", new Object[] { exception,
                                                                                                             meUuid }, null), exception);

        } catch (final Exception wsse) {
            FFDCFilter.processException(wsse, CLASS_NAME + "." + methodName, "1:460:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, wsse);
            }

            throw new ResourceException(NLS.getFormattedMessage(
                                                                "CREATE_CONNECTION_CWSIV0950", new Object[] { wsse }, null), wsse);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Constructor. Creates a connection to the given bus.
     * 
     * @param endpointActivation
     *            the endpoint activation for which this connection is being
     *            created
     * @param busName
     *            the name of the bus on which the messaging engine resides
     * @throws SIException
     *             if a connection cannot be created
     */
    SibRaMessagingEngineConnection(
                                   final SibRaEndpointActivation endpointActivation,
                                   final String busName) throws SIException, ResourceException {

        final String methodName = "SibRaMessagingEngineConnection";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] {
                                                               endpointActivation, busName });
        }

        _endpointActivation = endpointActivation;
        _endpointConfiguration = endpointActivation.getEndpointConfiguration();

        _busName = busName;

        final SICoreConnectionFactory factory = SICoreConnectionFactorySelector
                        .getSICoreConnectionFactory(FactoryType.TRM_CONNECTION);

        /*
         * Create TRM properties
         */
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put(SibTrmConstants.BUSNAME, busName);
        properties.put(SibTrmConstants.TARGET_GROUP, _endpointConfiguration.getTarget());
        properties.put(SibTrmConstants.TARGET_TYPE, _endpointConfiguration.getTargetType());
        properties.put(SibTrmConstants.TARGET_SIGNIFICANCE, _endpointConfiguration.getTargetSignificance());

        // SIB0130.ra Added Provider endpoints to the map
        properties.put(SibTrmConstants.PROVIDER_ENDPOINTS,
                       _endpointConfiguration.getProviderEndpoints());

        final String targetTransportChain = _endpointConfiguration
                        .getTargetTransportChain();
        if ((targetTransportChain != null) && !"".equals(targetTransportChain)) {
            properties.put(SibTrmConstants.TARGET_TRANSPORT_CHAIN,
                           targetTransportChain);
        }

        /*
         * Create an connection
         */
        try {
            _connection = createConnection(factory, _endpointConfiguration
                            .getUserName(), _endpointConfiguration.getPassword(),
                                           properties, _busName);

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(this, TRACE, "Obtained Connection <" + _connection + ">");
            }

            /*
             * Add connection event listener
             */
            _connectionListener = new SibRaConnectionListener();
            _connection.addConnectionListener(_connectionListener);

        } catch (SIAuthenticationException siae) {
            FFDCFilter.processException(siae, CLASS_NAME + "."
                                              + methodName, "1:458:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, siae);
            }
            throw new ResourceException(NLS.getFormattedMessage(
                                                                "CREATE_CONNECTION_CWSIV0962", new Object[] { siae, _busName, getActivationSpecId() }, null), siae);
        } catch (SINotAuthorizedException sinae) {

            FFDCFilter.processException(sinae, CLASS_NAME + "."
                                               + methodName, "1:568:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, sinae);
            }

            if (_endpointConfiguration.getUserName() == null) {
                // If we have no username and we are here then there is a
                // configuration problem with the authentication alias on the activation spec
                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "CREATE_CONNECTION_CWSIV0960", new Object[] { sinae,
                                                                                                                 _busName, _endpointConfiguration.getActivationSpec() }, null), sinae);
            } else {
                // There is a correct authentication alias but the user is not authorized
                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "CREATE_CONNECTION_CWSIV0961", new Object[] { sinae,
                                                                                                                 _busName, _endpointConfiguration.getActivationSpec() }, null), sinae);
            }
        } catch (final Exception wsse) {
            FFDCFilter.processException(wsse, CLASS_NAME + "." + methodName, "1:589:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, wsse);
            }

            throw new ResourceException(NLS.getFormattedMessage(
                                                                "CREATE_CONNECTION_CWSIV0950", new Object[] { wsse }, null), wsse);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * @return the exact activation specification id or just return the toString() of activation specification
     *         object as used in other places of this class.
     */
    private String getActivationSpecId() {
        String activationSpecId = null;
        if (_endpointActivation.getMessageEndpointFactory() instanceof MDBMessageEndpointFactory) {
            activationSpecId = ((MDBMessageEndpointFactory) _endpointActivation.getMessageEndpointFactory()).getActivationSpecId();
        } else {
            activationSpecId = _endpointConfiguration.getActivationSpec().toString();
        }

        return activationSpecId;
    }

    /**
     * Constructor. Creates a connection to the given bus. Uses the supplied target data.
     * 
     * @param endpointActivation
     *            the endpoint activation for which this connection is being
     *            created
     * @param busName
     *            the name of the bus on which the messaging engine resides
     * @throws SIException
     *             if a connection cannot be created
     */
    SibRaMessagingEngineConnection(
                                   final SibRaEndpointActivation endpointActivation,
                                   final String busName, final String targetType,
                                   final String targetSignificance, final String target) throws SIException, ResourceException {
        final String methodName = "SibRaMessagingEngineConnection";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] {
                                                               endpointActivation, busName, targetType, targetSignificance, target });
        }

        _endpointActivation = endpointActivation;
        _endpointConfiguration = endpointActivation.getEndpointConfiguration();

        _busName = busName;

        final SICoreConnectionFactory factory = SICoreConnectionFactorySelector
                        .getSICoreConnectionFactory(FactoryType.TRM_CONNECTION);

        /*
         * Create TRM properties
         */
        final Map<String, String> properties = new HashMap<String, String>();

        // chetan liberty change
        // Necessary parametes to connect to ME
        // TRM will then decide if it has to connect to local or Remote ME
        // SIB0130.ra Added Provider endpoints to the map
        properties.put(SibTrmConstants.PROVIDER_ENDPOINTS,
                       _endpointConfiguration.getProviderEndpoints());
        properties.put(SibTrmConstants.TARGET_TRANSPORT_TYPE,
                       _endpointConfiguration.getTargetTransport());
        properties.put(SibTrmConstants.BUSNAME,
                       _endpointConfiguration.getBusName());
        /*
         * Create an connection
         */
        try {
            _connection = createConnection(factory, _endpointConfiguration
                            .getUserName(), _endpointConfiguration.getPassword(),
                                           properties, _busName);

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(this, TRACE, "Obtained Connection <" + _connection + ">");
            }

            /*
             * Add connection event listener
             */
            _connectionListener = new SibRaConnectionListener();
            if (_connection != null)
                _connection.addConnectionListener(_connectionListener);

        } catch (SIAuthenticationException siae) {
            FFDCFilter.processException(siae, CLASS_NAME + "."
                                              + methodName, "1:564:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, siae);
            }
            throw new ResourceException(NLS.getFormattedMessage(
                                                                "CREATE_CONNECTION_CWSIV0962", new Object[] { siae, _busName, getActivationSpecId() }, null), siae);
        } catch (SINotAuthorizedException sinae) {
            FFDCFilter.processException(sinae, CLASS_NAME + "."
                                               + methodName, "1:681:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, sinae);
            }

            if (_endpointConfiguration.getUserName() == null) {
                // If we have no username and we are here then there is a
                // configuration problem with the authentication alias on the activation spec
                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "CREATE_CONNECTION_CWSIV0960", new Object[] { sinae,
                                                                                                                 _busName, _endpointConfiguration.getActivationSpec() }, null), sinae);
            } else {
                // There is a correct authentication alias but the user is not authorized
                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "CREATE_CONNECTION_CWSIV0961", new Object[] { sinae,
                                                                                                                 _busName, _endpointConfiguration.getActivationSpec() }, null), sinae);
            }
        } catch (final Exception wsse) {
            FFDCFilter.processException(wsse, CLASS_NAME + "." + methodName, "1:705:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, wsse);
            }

            throw new ResourceException(NLS.getFormattedMessage(
                                                                "CREATE_CONNECTION_CWSIV0950", new Object[] { wsse }, null), wsse);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * Accessor for the core connection.
     * 
     * @return the core connection
     */
    SICoreConnection getConnection() {

        return _connection;

    }

    /**
     * Returns the endpoint activation.
     * 
     * @return the endpoint activation
     */
    SibRaEndpointActivation getEndpointActivation() {

        return _endpointActivation;

    }

    /**
     * Returns the endpoint configuration.
     * 
     * @return the endpoint configuration
     */
    SibRaEndpointConfiguration getEndpointConfiguration() {

        return _endpointConfiguration;

    }

    /**
     * Returns the bus name.
     * 
     * @return the bus name
     */
    String getBusName() {

        return _busName;

    }

    /**
     * Creates a new listener to the given destination.
     * 
     * @param destination
     *            the destination to listen to
     * @return a listener
     * @throws ResourceException
     *             if the creation fails
     */
    SibRaListener createListener(final SIDestinationAddress destination, MessageEndpointFactory messageEndpointFactory)
                    throws ResourceException {

        final String methodName = "createListener";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { destination, messageEndpointFactory });
        }

        if (_closed) {

            throw new IllegalStateException(NLS
                            .getString("LISTENER_CLOSED_CWSIV0952"));

        }

        final SibRaListener listener;

        listener = new SibRaSingleProcessListener(this, destination, messageEndpointFactory);
        _listeners.put(destination, listener);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, listener);
        }
        return listener;

    }

    /**
     * Gets a dispatcher for the given session and messages.
     * 
     * @param session
     *            the session locking the messages
     * @return the dispatcher
     * @throws ResourceException
     *             if a dispatcher could not be created
     */
    SibRaDispatcher createDispatcher(final AbstractConsumerSession session,
                                     final Reliability unrecoveredReliability,
                                     final int maxFailedDeliveries,
                                     final int sequentialFailureThreshold)
                    throws ResourceException {

        final String methodName = "createDispatcher";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName,
                        new Object[] { session, unrecoveredReliability, maxFailedDeliveries, sequentialFailureThreshold });
        }

        if (_closed) {

            throw new IllegalStateException(NLS
                            .getString("LISTENER_CLOSED_CWSIV0953"));

        }

        final SibRaDispatcher dispatcher;

        if (_endpointActivation.isEndpointMethodTransactional()) {

            if (_endpointConfiguration.getShareDataSourceWithCMP()) {

                dispatcher = new SibRaSynchronizedDispatcher(this,
                                session, _endpointActivation, unrecoveredReliability, maxFailedDeliveries,
                                sequentialFailureThreshold);

            } else {

                dispatcher = new SibRaTransactionalDispatcher(this,
                                session, _endpointActivation, _busName, unrecoveredReliability, maxFailedDeliveries,
                                sequentialFailureThreshold);

            }

        } else {

            if (SibRaMessageDeletionMode.BATCH.equals(_endpointConfiguration
                            .getMessageDeletionMode())) {

                dispatcher = new SibRaBatchMessageDeletionDispatcher(
                                this, session, _endpointActivation, unrecoveredReliability, maxFailedDeliveries,
                                sequentialFailureThreshold);

            } else if (SibRaMessageDeletionMode.SINGLE
                            .equals(_endpointConfiguration.getMessageDeletionMode())) {

                dispatcher = new SibRaSingleMessageDeletionDispatcher(
                                this, session, _endpointActivation, unrecoveredReliability, maxFailedDeliveries,
                                sequentialFailureThreshold);

            } else {

                dispatcher = new SibRaNonTransactionalDispatcher(this,
                                session, _endpointActivation, unrecoveredReliability, maxFailedDeliveries,
                                sequentialFailureThreshold);

            }

        }

        _dispatcherCount.incrementAndGet();
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "Creating a dispatcher, there are now " + _dispatcherCount.get() + " open dispatchers");
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, dispatcher);
        }
        return dispatcher;

    }

    /**
     * Closes the given dispatcher.
     * 
     * @param dispatcher
     *            the dispatcher to close.
     */
    void closeDispatcher(final SibRaDispatcher dispatcher) {

        final String methodName = "closeDispatcher";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        dispatcher.close();
        _dispatcherCount.decrementAndGet();
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "Removing a dispatcher - there are " + _dispatcherCount.get() + " left");
        }
        synchronized (_dispatcherCount) {
            _dispatcherCount.notifyAll();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Closes the connection and any associated listeners and dispatchers
     */
    void close() {
        final String methodName = "close";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        close(false);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * Closes this connection and any associated listeners and dispatchers.
     */
    void close(boolean alreadyClosed) {

        final String methodName = "close";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, alreadyClosed);
        }

        _closed = true;

        /*
         * 238811:
         * Stop all of the listeners - do not close them as the dispatchers
         * might still be using them. Close them after the dispatchers have
         * stopped.
         */

        /*
         * We close dispatchers as soon as they are finished with so no longer have to
         * close them here.
         */

        if (!alreadyClosed) {
            for (final Iterator iterator = _listeners.values().iterator(); iterator
                            .hasNext();) {
                final SibRaListener listener = (SibRaListener) iterator.next();
                listener.stop();
            }
        }

        // The connection is closing so throw away the connection name
        SibRaDispatcher.resetMEName();

        // We want this thread to wait until all the dispatchers have finished their work
        // (which will happen when the dispatcherCount hits 0. We put the thread into a
        // wait state if there are any dispatchers still going, the thread will be notified
        // each time a dispatcher closes (when it has finished its work).
        synchronized (_dispatcherCount) {
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(this, TRACE, "There are still " + _dispatcherCount.get() + " open dispatchers");
            }

            if (_dispatcherCount.get() > 0) {
                Long mdbQuiescingTimeout = getMDBQuiescingTimeoutProperty();
                waitForDispatchersToFinish(mdbQuiescingTimeout);
            }
        }

        /*
         * Close all of the listeners
         */
        if (!alreadyClosed) {
            for (final Iterator iterator = _listeners.values().iterator(); iterator
                            .hasNext();) {
                final SibRaListener listener = (SibRaListener) iterator.next();
                listener.close();
            }
        }
        _listeners.clear();

        /*
         * Close the connection
         */

        try {

            _connection.removeConnectionListener(_connectionListener);
            // It should be ok to call close on an already closed connection but comms
            // FFDCs thjis (processor seems ok with it though). No need to close the connection
            // again anyway so we'll check and if we are being called as part of MeTerminated or
            // connectionError then the connection will have been closed so dont close again.
            if (!alreadyClosed) {
                _connection.close();
            }

        } catch (final SIException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:1023:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

        } catch (final SIErrorException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:1031:1.59", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Waits for the dispatchers to finish there work before returning.
     * The MDBQuiesingTimeout value determines how long wait for all the
     * dispatchers. If a negative number is used then we don't wait at all,
     * if a 0 is passed in then we wait until the dispatchers have actually
     * finished (default). Any other number is how long we wait before carrying on
     * with the close.
     * 
     * @param mdbQuiescingTimeout
     */
    private void waitForDispatchersToFinish(Long mdbQuiescingTimeout) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
            SibTr.entry(this, TRACE, "waitForDispatchersToFinish", mdbQuiescingTimeout);

        final long wakeupInterval = 10000; //How long we wait for.
        long startTime = System.currentTimeMillis();
        if (mdbQuiescingTimeout >= 0) // if we have a negative custom property then we don't wait at all.
        {
            while (_dispatcherCount.get() > 0) {
                long timeWaiting = System.currentTimeMillis() - startTime; //How long we have been waiting so far
                long timeToWaitThisTimeRound;

                if (mdbQuiescingTimeout > 0) //We have the custom property set to a time
                {
                    long timeLeftToWait = mdbQuiescingTimeout - timeWaiting; // Calculate how much time we have left
                    if (timeLeftToWait <= 0) {
                        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                            SibTr.debug(this, TRACE, "Breaking out of loop as we have finished waiting");

                        break; // We have run out of time, so break from the loop
                    }
                    timeToWaitThisTimeRound = Math.min(timeLeftToWait, wakeupInterval);
                } else {
                    timeToWaitThisTimeRound = wakeupInterval;
                }

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                    SibTr.debug(this, TRACE, "Entering wait for " + timeToWaitThisTimeRound + "ms. Waited " + timeWaiting + "ms so far. " + _dispatcherCount.get()
                                             + " dispatcher(s) still active");

                try {
                    _dispatcherCount.wait(timeToWaitThisTimeRound); //Lets do the actual wait
                } catch (InterruptedException ex) {
                    // No FFDC code needed
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
            SibTr.exit(this, TRACE, "waitForDispatchersToFinish");
    }

    private Long getMDBQuiescingTimeoutProperty() {
        // PM12339 has more info on this
        // 0 -> default value in tWAS. i.e Waiting indefintely for dispatchers to finish
        return 0L;
    }

    /**
     * Creates this connection using either the Auth Alias supplied or, if the property is set
     * the WAS server subject.
     * 
     * @param factory
     *            The SICoreConnectionFactory used to make the connection.
     * @param name
     *            The userid to use for secure connections
     * @param password
     *            The password to use for secure connections
     * @param properties
     *            The Map of properties to use when making the connection
     * 
     * @return the return value is the SICoreConnection object
     * 
     * @throws
     */
    SICoreConnection createConnection(SICoreConnectionFactory factory, String name, String password, Map properties, String busName)
                    throws SIException, SIErrorException, Exception {

        final String methodName = "createConnection";

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { factory, name, "password not traced", properties, busName });
        }

        SICoreConnection result;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "Creating connection with Userid and password");
        }
        result = factory.createConnection(name, password, properties);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createConnection", result);
        }

        return result;
    }

    /**
     * Listener used to receive notification of connection events.
     */
    private final class SibRaConnectionListener implements
                    SICoreConnectionListener {

        @Override
        public void asynchronousException(final ConsumerSession session,
                                          final Throwable throwable) {

            final String methodName = "asynchronousException";
            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.entry(this, LISTENER_TRACE, methodName, new Object[] {
                                                                            session, throwable });
            }

            // PK60700 asynchronous call to endpoint activation
            SibRaConnectionEventThread.enqueueEvent(
                                                    SibRaConnectionEventThread.SESSION_ERROR_EVENT,
                                                    SibRaMessagingEngineConnection.this, _endpointActivation,
                                                    session, throwable);

            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.exit(this, LISTENER_TRACE, methodName);
            }

        }

        @Override
        public void meQuiescing(final SICoreConnection connection) {

            final String methodName = "meQuiescing";
            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.entry(this, LISTENER_TRACE, methodName, connection);
            }

            // PK60700 asynchronous call to endpoint activation
            SibRaConnectionEventThread.enqueueEvent(
                                                    SibRaConnectionEventThread.ME_QUIESCING_EVENT,
                                                    SibRaMessagingEngineConnection.this, _endpointActivation,
                                                    null, null);

            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.exit(this, LISTENER_TRACE, methodName);
            }

        }

        @Override
        public void commsFailure(final SICoreConnection connection,
                                 final SIConnectionLostException exception) {

            final String methodName = "commsFailure";
            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.entry(this, LISTENER_TRACE, methodName, new Object[] {
                                                                            connection, exception });
            }

            // PK60700 asynchronous call to endpoint activation
            SibRaConnectionEventThread.enqueueEvent(
                                                    SibRaConnectionEventThread.CONNECTION_ERROR_EVENT,
                                                    SibRaMessagingEngineConnection.this, _endpointActivation,
                                                    null, exception);

            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.exit(this, LISTENER_TRACE, methodName);
            }

        }

        @Override
        public void meTerminated(final SICoreConnection connection) {

            final String methodName = "meTerminated";
            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.entry(this, LISTENER_TRACE, methodName, connection);
            }

            // PK60700 asynchronous call to endpoint activation
            SibRaConnectionEventThread.enqueueEvent(
                                                    SibRaConnectionEventThread.ME_TERMINATED_EVENT,
                                                    SibRaMessagingEngineConnection.this, _endpointActivation,
                                                    null, null);

            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.exit(this, LISTENER_TRACE, methodName);
            }

        }

    }

    /**
     * PK60700 It is unsafe to handle ME events on the thread which invokes the
     * callback, as closing a connection involves a synchronous exchange
     * with the ME - which in turn may need to be woken with an event.
     * Instead, events are queued for a maxa of one instance this thread,
     * which starts and stops as required. Each item will most likely itself
     * schedule a re-connect for a later time (after closing or attempting
     * to establish a connection), so immediate execution is not a priority.
     */
    private static class SibRaConnectionEventThread implements Runnable {

        public static final int ME_QUIESCING_EVENT = 0;
        public static final int ME_TERMINATED_EVENT = 1;
        public static final int SESSION_ERROR_EVENT = 2;
        public static final int CONNECTION_ERROR_EVENT = 3;

        /** Work item queued to the thread */
        private static class SibRaConnectionEvent {
            public int event_id;
            public SibRaMessagingEngineConnection connection;
            public SibRaEndpointActivation endpointActivation;
            public ConsumerSession session;
            public Throwable throwable;

            private SibRaConnectionEvent(int event_id,
                                         SibRaMessagingEngineConnection connection,
                                         SibRaEndpointActivation endpointActivation,
                                         ConsumerSession session,
                                         Throwable throwable) {
                this.event_id = event_id;
                this.connection = connection;
                this.endpointActivation = endpointActivation;
                this.session = session;
                this.throwable = throwable;
            }
        }

        /** Monitor for enqueing work for the thread */
        private static final Object workSync = new Object();

        /** Queue of work for the thread */
        private static final LinkedList workQueue = new LinkedList();

        /** Our thread, if it's running */
        private static SibRaConnectionEventThread threadInstance = null;

        /** Enqueue an event for asynchronous processing */
        public static void enqueueEvent(int event_id,
                                        SibRaMessagingEngineConnection connection,
                                        SibRaEndpointActivation endpointActivation,
                                        ConsumerSession session,
                                        Throwable throwable) {
            final String methodName = "enqueueEvent";
            if (TraceComponent.isAnyTracingEnabled() && EVENT_TRACE.isEntryEnabled()) {
                SibTr.entry(EVENT_TRACE, methodName,
                            new Object[] { event_id, connection, endpointActivation,
                                          session, throwable });
            }

            // Synchronize our activity with the thread
            synchronized (workSync) {

                // Do we need to create a thread?
                if (threadInstance == null) {

                    // Execute our thread
                    try {

                        SibRaConnectionEventThread newThread = new SibRaConnectionEventThread();
                        threadInstance = newThread;
                        Thread th = new Thread(newThread);
                        th.start();

                    } catch (Throwable e) {
                        // Something went wrong creating the thread. We will invoke on this thread.
                        FFDCFilter.processException(e, CLASS_NAME + "."
                                                       + methodName, "1:1423:1.59");
                        if (TraceComponent.isAnyTracingEnabled() && EVENT_TRACE.isDebugEnabled())
                            SibTr.exception(EVENT_TRACE, e);
                        if (e instanceof ThreadDeath)
                            throw (ThreadDeath) e;
                    }
                }

                // Enqueue our work
                if (threadInstance != null) {
                    workQueue.addLast(
                                    new SibRaConnectionEvent(event_id, connection, endpointActivation, session, throwable));
                }
                // If something prevented us executing asynchronously, invoke on this thread
                else {
                    executeCallback(event_id, connection, endpointActivation, session, throwable);
                }
            } // drop the lock

            if (TraceComponent.isAnyTracingEnabled() && EVENT_TRACE.isEntryEnabled()) {
                SibTr.exit(EVENT_TRACE, methodName);
            }
        }

        /** Run method de-queues and executes items until none are left, then ends. */
        @Override
        public void run() {

            final String methodName = "run";
            if (TraceComponent.isAnyTracingEnabled() && EVENT_TRACE.isEntryEnabled()) {
                SibTr.entry(this, EVENT_TRACE, methodName);
            }

            boolean eventsRemain = true;
            while (eventsRemain) {

                // Synchronize when accessing the queue of work, or updating thread state
                SibRaConnectionEvent event = null;
                synchronized (workSync) {
                    int eventDepth = workQueue.size();
                    if (TraceComponent.isAnyTracingEnabled() && EVENT_TRACE.isDebugEnabled())
                        SibTr.debug(this, EVENT_TRACE, eventDepth + " queued events");
                    eventsRemain = (eventDepth > 0);
                    if (eventsRemain) {
                        event = (SibRaConnectionEvent) workQueue.removeFirst();
                    } else {
                        // We end this thread - any further events need a new one.
                        threadInstance = null;
                    }
                } // Drop the lock

                if (event != null) {
                    // Handle the event, ensuring that a failure processing one
                    // event does not bring down the thread.
                    try {
                        executeCallback(event.event_id, event.connection,
                                        event.endpointActivation, event.session, event.throwable);
                    } catch (Throwable e) {
                        FFDCFilter.processException(e, CLASS_NAME + "."
                                                       + methodName, "1:1483:1.59");
                        if (TraceComponent.isAnyTracingEnabled() && EVENT_TRACE.isDebugEnabled())
                            SibTr.exception(EVENT_TRACE, e);
                        if (e instanceof ThreadDeath)
                            throw (ThreadDeath) e;
                    }
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && EVENT_TRACE.isEntryEnabled()) {
                SibTr.exit(this, EVENT_TRACE, methodName);
            }

        }

        /**
         * Invoke the endpoint activation for an event
         * 
         * @param event_id
         * @param connection
         * @param endpointActivation
         * @param session
         * @param throwable
         */
        private static void executeCallback(int event_id,
                                            SibRaMessagingEngineConnection connection,
                                            SibRaEndpointActivation endpointActivation,
                                            ConsumerSession session,
                                            Throwable throwable) {
            final String methodName = "executeCallback";
            if (TraceComponent.isAnyTracingEnabled() && EVENT_TRACE.isEntryEnabled()) {
                SibTr.entry(EVENT_TRACE, methodName,
                            new Object[] { event_id, connection, endpointActivation,
                                          session, throwable });
            }

            // Invoke the correct method on the endpoint activation
            switch (event_id) {
                case ME_QUIESCING_EVENT:
                    endpointActivation.messagingEngineQuiescing(connection);
                    break;
                case ME_TERMINATED_EVENT:
                    endpointActivation.messagingEngineTerminated(connection);
                    break;
                case SESSION_ERROR_EVENT:
                    endpointActivation.sessionError(connection, session, throwable);
                    break;
                case CONNECTION_ERROR_EVENT:
                    endpointActivation.connectionError(connection, (SIException) throwable);
                    break;
            }

            if (TraceComponent.isAnyTracingEnabled() && EVENT_TRACE.isEntryEnabled()) {
                SibTr.exit(EVENT_TRACE, methodName);
            }
        }

    }

    /**
     * Returns the number of listeners created from this connection
     * 
     * @return the number of listeners being used on this connection
     */
    int getNumberListeners() {
        return _listeners.size();
    }

    boolean isClosed() {
        return _closed;
    }

    /**
     * Gets the application server
     * 
     * @return The ApplicationServer object the MDB is starting in.
     */
    //lohith liberty change
/*
 * protected ApplicationServer getServer()
 * {
 * final String methodName = "getServer";
 * if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
 * SibTr.entry(this, TRACE, methodName);
 * }
 * 
 * ApplicationServer as = null;
 * try
 * {
 * // Attempt to get hold of the application server.
 * as = (ApplicationServer) AccessController.doPrivileged(new PrivilegedExceptionAction()
 * {
 * public Object run() throws Exception
 * {
 * return WsServiceRegistry.getService(this, ApplicationServer.class);
 * }
 * });
 * }
 * catch (Exception e)
 * {
 * // Failed to get the application server. Null will be returned
 * FFDCFilter.processException(e, CLASS_NAME + "."
 * + methodName, "1:1580:1.59", this);
 * }
 * 
 * if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
 * SibTr.exit(this, TRACE, methodName, as);
 * }
 * return as;
 * }
 */

}
