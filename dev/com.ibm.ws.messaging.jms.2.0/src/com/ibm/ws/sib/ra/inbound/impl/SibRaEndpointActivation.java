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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.resource.ResourceException;
import javax.resource.spi.IllegalStateException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ejbcontainer.mdb.MDBMessageEndpointFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec;
import com.ibm.ws.sib.ra.SibRaEngineComponent;
import com.ibm.ws.sib.ra.SibRaMessagingEngineListener;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointInvoker;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.ra.SibRaActivationSpec;

/**
 * Instances of sub-classes of this abstract class represent the activation of
 * an endpoint. Implements <code>SibRaMessagingEngineListener</code> to
 * receive notification of the activation and deactivation of messaging engines.
 * Sub-classes are responsible for determining which destinations on which
 * messaging engines should be connected to.
 */
abstract class SibRaEndpointActivation implements SibRaMessagingEngineListener {

    /**
     * The message endpoint factory for this activation.
     */
    protected final MessageEndpointFactory _messageEndpointFactory;

    /**
     * The endpoint configuration for this activation.
     */
    protected final SibRaEndpointConfiguration _endpointConfiguration;

    /**
     * The endpoint invoker for this activation.
     */
    private final SibRaEndpointInvoker _endpointInvoker;

    /**
     * The work manager associated with the resource adapter on which this
     * endpoint was created.
     */
    private final WorkManager _workManager;

    /**
     * A map from messaging engine UUIDs to
     * <code>SibRaMessagingEngineConnection</code> instances.
     */
    protected Map <String, SibRaMessagingEngineConnection> _connections = new HashMap <String, SibRaMessagingEngineConnection> ();

    /**
     * Flag indicating whether the endpoint method is transactional.
     */
    private final boolean _endpointMethodTransactional;

    /**
     * Flag indicating whether the endpoint is still active.
     */
    private boolean _active = true;

    /**
     * The Resource Adapter
     */
    protected ResourceAdapter _resourceAdapter;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaEndpointActivation.class);

    /**
     * Provides access to NLS enabled messages.
     */
    private static TraceNLS NLS = SibRaUtils.getTraceNls();

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaEndpointActivation.class
            .getName();

 
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
     *             if the activation fails
     */
    SibRaEndpointActivation(final SibRaResourceAdapterImpl resourceAdapter,
            final MessageEndpointFactory messageEndpointFactory,
            final SibRaEndpointConfiguration endpointConfiguration,
            final SibRaEndpointInvoker endpointInvoker)
            throws ResourceException {

        final String methodName = "SibRaEndpointActivation";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] {
                    resourceAdapter, messageEndpointFactory,
                    endpointConfiguration, endpointInvoker });
        }

        _messageEndpointFactory = messageEndpointFactory;
        _endpointConfiguration = endpointConfiguration;
        _endpointInvoker = endpointInvoker;
        _resourceAdapter = resourceAdapter;


        /**
         * If the RA's max concurrency is higher than the JCA's max concurency
         * then drop the RA's max concurrency to equal the JCA one and log a warning.
         */
        String j2eeName = null;
        // the max pool size set on the message endpoint factory
    	Integer jcaMax = null;
    	if (_messageEndpointFactory instanceof MDBMessageEndpointFactory) {

    		try {
    			// get the j2eeName
    			j2eeName = ((MDBMessageEndpointFactory)_messageEndpointFactory).getMDBKey().toString();
    			// get pool size from ejb container, earlier JCA used to provide
    			// Currently in liberty pool size is not dynamic in nature. But in future it will be 
    			// dynamic and we will have to support that.
    			jcaMax = ((MDBMessageEndpointFactory)_messageEndpointFactory).getMaxEndpoints();
    			
    		}catch (Exception e) {
    			FFDCFilter.processException(e,
    					"com.ibm.ws.sib.ra.inbound.impl.SibRaEndpointActivation",
    					"173", this);
    		}

    		// Get the RA's max pool size
    		int raMax = _endpointConfiguration.getMaxConcurrency();

    		// If the JCA poolsize is smaller than the RA one then reduce the RA one
    		// to be the same as the JCA one. Do not reduce the RA poolsize if the 
    		// jca max size is 0. 0 means there is no limit and as such we should
    		// adhere to the settings in the act spec.
    		if ((jcaMax.intValue () < raMax) && (jcaMax.intValue() > 0))
    		{
    			SibTr.warning(TRACE, "MAX_CONCURRENCY_CWSIV0551", new Object[] { j2eeName, raMax, jcaMax, jcaMax });

    			if (_endpointConfiguration.isJMSRa())
    			{
    				JmsJcaActivationSpec actSpec = (JmsJcaActivationSpec) _endpointConfiguration.getActivationSpec();
    				actSpec.setMaxConcurrency(jcaMax);
    			}
    			else
    			{
    				SibRaActivationSpec actSpec = (SibRaActivationSpec) _endpointConfiguration.getActivationSpec();
    				actSpec.setMaxConcurrency(jcaMax);
    			}
    		}
    	}
        /*
         * Obtain endpoint method
         */

        final Method endpointMethod = _endpointInvoker.getEndpointMethod();

        /*
         * Determine if endpoint method is transactional
         */

        try {

            _endpointMethodTransactional = _messageEndpointFactory
                    .isDeliveryTransacted(endpointMethod);

        } catch (final NoSuchMethodException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                    + methodName, "1:247:1.40", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

            throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                    "NO_METHOD_CWSIV0550", new Object[] {
                            messageEndpointFactory, exception }, null),
                    exception);
        }

        /*
         * Obtain work manager from bootstrap context
         */

        _workManager = resourceAdapter.getBootstrapContext().getWorkManager();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Processes a messaging engine on messaging engine start.
     *
     * @param messagingEngine
     *            the messaging engine
     * @throws ResourceException
     *             if the processing fails
     */
    abstract void addMessagingEngine(JsMessagingEngine messagingEngine)
            throws ResourceException;

    /**
     * Called on deactivation of this endpoint.
     */
    void deactivate() {

        final String methodName = "deactivate";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        _active = false;

        /*
         * Deregister the messaging engine listener
         */

        SibRaEngineComponent.deregisterMessagingEngineListener(this,
                _endpointConfiguration.getBusName());

        /*
         * Close all of the connections
         */

        synchronized (_connections) {

            for (final Iterator iterator = _connections.values().iterator(); iterator
                    .hasNext();) {

                final SibRaMessagingEngineConnection connection = (SibRaMessagingEngineConnection) iterator
                        .next();
                connection.close();

            }

            _connections.clear();

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Accessor for the message endpoint factory.
     *
     * @return the message endpoint factory
     */
    final MessageEndpointFactory getMessageEndpointFactory() {

        return _messageEndpointFactory;

    }

    /**
     * Accessor for the endpoint configuration.
     *
     * @return the endpoint configuration
     */
    final SibRaEndpointConfiguration getEndpointConfiguration() {

        return _endpointConfiguration;

    }

    /**
     * Accessor for the endpoint invoker.
     *
     * @return the endpoint invoker
     */
    final SibRaEndpointInvoker getEndpointInvoker() {

        return _endpointInvoker;

    }

    /**
     * Accessor for the work manager.
     *
     * @return the work manager
     */
    final WorkManager getWorkManager() {

        return _workManager;

    }

    /**
     * Accessor for endpoint method transactionality.
     *
     * @return the endpoint method transactionality
     */
    final boolean isEndpointMethodTransactional() {

        return _endpointMethodTransactional;

    }

    /**
     * Returns a connection to the given messaging engine.
     *
     * @param messagingEngine
     *            the messaging engine for which a connection is required
     * @return the connection
     * @throws IllegalStateException
     *             if the endpoint is no longer active
     * @throws ResourceException
     *             if a new connection is required and the creation fails
     */
    protected final SibRaMessagingEngineConnection getConnection(
            final JsMessagingEngine messagingEngine) throws ResourceException {

        final String methodName = "getConnection";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, messagingEngine);
        }

        SibRaMessagingEngineConnection connection;

        if (_active) {

            synchronized (_connections) {

                /*
                 * Do we already have a connection?
                 */

                connection = (SibRaMessagingEngineConnection) _connections
                        .get(messagingEngine.getUuid().toString());

                /*
                 * If not, create a new one and add it to the map
                 */

                if (connection == null) {

                    connection = new SibRaMessagingEngineConnection(this,
                            messagingEngine);
                    _connections.put(messagingEngine.getUuid().toString(),
                            connection);

                }

            }

        } else {

            throw new IllegalStateException(NLS.getFormattedMessage(
                    "ENDPOINT_DEACTIVATED_CWSIV0554", new Object[] {
                            messagingEngine.getUuid(),
                            messagingEngine.getBusName(), this }, null));

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, connection);
        }
        return connection;

    }

    /**
     * Returns a connection to the given messaging engine.
     *
     * @param busName
     *            the name of the bus on which the messaging engine resides
     * @param meUuid
     *            the UUID of the messaging engine for which a connection is
     *            required
     * @return the connection
     * @throws IllegalStateException
     *             if the endpoint is no longer active
     * @throws ResourceException
     *             if a new connection is required and the creation fails
     */
    protected final SibRaMessagingEngineConnection getConnection(
            final String busName, final String meUuid) throws ResourceException {

        final String methodName = "getConnection";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName,
                    new Object[] { busName, meUuid });
        }

        SibRaMessagingEngineConnection connection;

        if (_active) {

            synchronized (_connections) {

                /*
                 * Do we already have a connection?
                 */

                connection = (SibRaMessagingEngineConnection) _connections
                        .get(meUuid);

                /*
                 * If not, create a new one and add it to the map
                 */

                if (connection == null) {

                    connection = new SibRaMessagingEngineConnection(this,
                            busName, meUuid);
                    _connections.put(meUuid, connection);

                }

            }

        } else {

            throw new IllegalStateException(NLS.getFormattedMessage(
                    "ENDPOINT_DEACTIVATED_CWSIV0554", new Object[] { meUuid,
                            busName, this }, null));

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, connection);
        }
        return connection;

    }

    /**
     * Closes the connection for the given messaging engine if there is one
     * open.
     *
     * @param messagingEngine
     *            the messaging engine to close the connection for
     */
    protected final void closeConnection(final JsMessagingEngine messagingEngine) {

        final String methodName = "closeConnection";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, messagingEngine);
        }

        closeConnection(messagingEngine.getUuid().toString());

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Closes the connection for the given messaging engine if there is one
     * open.
     *
     * @param meUuid
     *            the UUID for the messaging engine to close the connection for
     */
    protected void closeConnection(final String meUuid) 
    {
        final String methodName = "closeConnection";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) 
        {
            SibTr.entry(this, TRACE, methodName, meUuid);
        }
        
        closeConnection (meUuid, false);
        
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) 
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }
    /**
     * Closes the connection for the given messaging engine if there is one
     * open.
     *
     * @param meUuid
     *            the UUID for the messaging engine to close the connection for
     * @param alreadyClosed
     *            if the connection has already been closed
     */
    protected void closeConnection(final String meUuid, boolean alreadyClosed) {

        final String methodName = "closeConnection";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object [] { meUuid, alreadyClosed });
        }

        final SibRaMessagingEngineConnection connection;
        synchronized (_connections) {

            connection = (SibRaMessagingEngineConnection) _connections
                    .remove(meUuid);

        }

        if (connection != null) {
            connection.close(alreadyClosed);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Called to indicate that a messaging engine has been started. Adds it to
     * the set of active messaging engines.
     *
     * @param messagingEngine
     *            the messaging engine that is starting
     */
    public void messagingEngineStarting(final JsMessagingEngine messagingEngine) {

        final String methodName = "messagingEngineStarting";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, messagingEngine);
        }

        try {

            addMessagingEngine(messagingEngine);

        } catch (final ResourceException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                    + methodName, "1:608:1.40", this);
            SibTr.error(TRACE, "MESSAGING_ENGINE_STARTING_CWSIV0555",
                    new Object[] { exception, messagingEngine.getName(),
                            messagingEngine.getBusName(), this });

            // To reach this point would mean a more serious error has occurred like
            // the destination does not exist. Deactivate the MDB so that the user
            // knows there is a problem. This behaviour is consistent with the MDB
            // being deactivated if the MDB fails during initial startup (an exception
            // thrown to J2C and they would deactivate the MDB).
            deactivate();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Called to indicate that a messaging engine is stopping. Removes it from
     * the set of active messaging engines and closes any open connection.
     *
     * @param messagingEngine
     *            the messaging engine that is stopping
     * @param mode
     *            the stop mode
     */
    public void messagingEngineStopping(
            final JsMessagingEngine messagingEngine, final int mode) {

        final String methodName = "messagingEngineStopping";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] {
                    messagingEngine, mode });
        }

        closeConnection(messagingEngine);

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
    abstract void sessionError(final SibRaMessagingEngineConnection connection,
            final ConsumerSession session, final Throwable throwable);

    /**
     * Indicates that an error has been detected on the given connection.
     *
     * @param connection
     *            the connection
     * @param exception
     *            the error
     */
    abstract void connectionError(
            final SibRaMessagingEngineConnection connection,
            final SIException exception);

    /**
     * Indicates that the messaging engine for the given connection has
     * terminated.
     *
     * @param connection
     *            the connection
     */
    abstract void messagingEngineTerminated(
            SibRaMessagingEngineConnection connection);

    /**
     * Indicates that the messaging engine for the given connection is
     * quiescing.
     *
     * @param connection
     *            the connection
     */
    abstract void messagingEngineQuiescing(
            SibRaMessagingEngineConnection connection);

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    public final String toString() {

        return getStringGenerator().getStringRepresentation();

    }

    /**
     * Returns a string generator containing the fields for this class.
     *
     * @return a string generator
     */
    protected SibRaStringGenerator getStringGenerator() {

        final SibRaStringGenerator generator = new SibRaStringGenerator(this);
        generator.addField("active", _active);
        generator.addField("connections", _connections);
        generator.addField("messageEndpointFactory", _messageEndpointFactory);
        generator.addField("endpointConfiguration", _endpointConfiguration);
        generator.addField("endpointInvoker", _endpointInvoker);
        generator.addField("workManager", _workManager);
        generator.addField("endpointMethodTransactional",
                _endpointMethodTransactional);
        return generator;

    }

}
