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

/**
 * This class contains the common code between the Scalable and Colocating endpoint activations classes.
 * This class extends SibRaEndpointActivation which contains common code between the static and 
 * dynamic destination endpoint activation classes. This class will house further common code between the 
 * two new static destination endpoint activation classes (SibRaScalableEndpointActivation and 
 * SibRaColocatingEndpointActivation). 
 * 
 * With the wild carding of destinations line item the dynamic destination endpoint activation may be able
 * to merge with the new static destination endpoint activation classes. In this case the SibRaEndpointActivation
 * and SibRaCommonEndpointActivation classes can probably be merged into one class (SibRaEndpointActivation
 * common code for both Static and Dynamic endpoint activations and SibRaCommonEndpointActivation is currently only
 * for the static endpoint activations).
 * 
 */

package com.ibm.ws.sib.ra.inbound.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.IllegalStateException;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ejbcontainer.mdb.MDBMessageEndpointFactory;
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
import com.ibm.wsspi.sib.core.DestinationAvailability;
import com.ibm.wsspi.sib.core.DestinationListener;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.trm.SibTrmConstants;

/**
 * This is the base class for the endpoint activation objects that were introduced in line item SIB0102.
 * This class is used to connect an MDB to messaging engine(s) dependant upon the destination type, locality
 * of the ME and user specified target preferences
 */
public abstract class SibRaCommonEndpointActivation extends SibRaEndpointActivation
{
    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
                    .getTraceComponent(SibRaCommonEndpointActivation.class);

    /**
     * The component to use for trace for the point to point destination strategy
     */
    private static final TraceComponent P2PTRACE = SibRaUtils
                    .getTraceComponent(PointToPointStrategy.class);

    /**
     * The component to use for trace for the durable pub sub destination strategy
     */
    private static final TraceComponent DPSTRACE = SibRaUtils
                    .getTraceComponent(PointToPointStrategy.class);

    /**
     * The component to use for trace for the non durable pub sub destination strategy
     */
    private static final TraceComponent NDPSTRACE = SibRaUtils
                    .getTraceComponent(PointToPointStrategy.class);

    /**
     * The component to use for trace for the non durable pub sub destination strategy
     */
    private static final TraceComponent DESTTRACE = SibRaUtils
                    .getTraceComponent(DestinationStrategy.class);

    /**
     * The component to use for trace for the
     * <code>SibRaConnectionListener</code> inner class.
     */
    private static final TraceComponent LISTENER_TRACE = SibRaUtils
                    .getTraceComponent(SibRaDestinationListener.class);

    /**
     * Provides access to NLS enabled messages.
     */
    private static TraceNLS NLS = SibRaUtils.getTraceNls();

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaCommonEndpointActivation.class.getName();

    /**
     * Random number generator for picking a messaging engine to connect to.
     */
    private static final Random _random = new Random();

    /**
     * The destination strategy object this MDB is using
     */
    protected final DestinationStrategy _destinationStrategy;

    /**
     * The retry interval to try
     */
    protected int _retryInterval;

    /**
     * Is the connection to a remote ME
     */
    protected boolean _connectedRemotely;

    /**
     * Are we connected to MEs that are in our preferred target list (if no target data is set then any
     * connection is labelled as preferred).
     */
    protected boolean _connectedToPreferred;

    /**
     * To save the code repeatedly asking the endpoint activation for the target data we'll keep a
     * copy of that data ourselves
     */
    protected String _targetType;
    protected String _targetSignificance;
    protected String _target;

    protected BootstrapContext _bootstrapContext;

    /**
     * The timer to use when attempting to recheck available messaging engines
     */
    protected Timer _timer;

    /**
     * Lock on this when accessing the _timer.
     */
    private final Object _timerLock = new Object();

    /**
     * Constructor. Create and registers a message engine listener. Create an inner class
     * strategy object that is dependant upon the destination type associated with this
     * activation. Lastly it will attempt to create a connection (and consumer) to
     * the desired messaging engine(s).
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
    public SibRaCommonEndpointActivation(SibRaResourceAdapterImpl resourceAdapter,
                                         MessageEndpointFactory messageEndpointFactory,
                                         SibRaEndpointConfiguration endpointConfiguration,
                                         SibRaEndpointInvoker endpointInvoker) throws ResourceException
    {
        super(resourceAdapter, messageEndpointFactory, endpointConfiguration, endpointInvoker);
        final String methodName = "SibRaCommonEndpointActivation";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, new Object[] {
                                                               resourceAdapter, messageEndpointFactory,
                                                               endpointConfiguration, endpointInvoker });
        }

        // Create a strategy object to seperate out the destination specific code from the main class.
        if (DestinationType.TOPICSPACE.equals(_endpointConfiguration.getDestinationType()))
        {
            if (_endpointConfiguration.isDurableSubscription())
            {
                _destinationStrategy = new DurablePubSubStrategy(this);
            }
            else
            {
                _destinationStrategy = new NonDurablePubSubStrategy(this);
            }
        }
        else
        {
            _destinationStrategy = new PointToPointStrategy(this);
        }

        _targetType = _endpointConfiguration.getTargetType();
        _targetSignificance = _endpointConfiguration.getTargetSignificance();
        _target = _endpointConfiguration.getTarget();
        _bootstrapContext = resourceAdapter.getBootstrapContext();

        // Retry interval specified in seconds, convert to milliseconds
        _retryInterval = _endpointConfiguration.getRetryInterval() * 1000;

        // If no retry specified then use the default 30 seconds (there should always be 
        // a value though and 0 is not allowed).
        if (_retryInterval <= 0)
        {
            _retryInterval = 30000;
        }

        SibRaEngineComponent.registerMessagingEngineListener(this, _endpointConfiguration.getBusName());

        try
        {
            timerLoop();
        } catch (Throwable t)
        {
            FFDCFilter.processException(t, CLASS_NAME + "."
                                           + methodName, "1:289:1.45", this);
            deactivate();
            if (t instanceof ResourceException)
            {
                throw (ResourceException) t;
            }
            else if (t instanceof RuntimeException)
            {
                throw (RuntimeException) t;
            }
            else if (t instanceof Error)
            {
                throw (Error) t;
            }
            else
            {
                throw new ResourceException(t);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(TRACE, methodName);
        }
    }

    /**
     * This method will be driven by a timer pop or by the MDB starting up. This is the "entry point"
     */
    void timerLoop() throws ResourceException
    {
        final String methodName = "timerLoop";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName);
        }

        checkMEs(getMEsToCheck());

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(TRACE, methodName);
        }
    }

    /**
     * This methoid is overriden by derived classes. It is used to obtain a list of messaging engines to see which
     * MEs we could connect to. For SibRaColocatingEndpointActivation this all local MEs (both running and stopped),
     * for SibRaScalableEndpointActivation this just returns the running MEs.
     * 
     * @return a list of MEs which are available to connect to.
     */
    abstract JsMessagingEngine[] getMEsToCheck();

    abstract boolean onlyConnectToDSH();

    /**
     * This method will check the supplied MEs to see if they are suitable for connecting to.
     * 
     * @param MEList A list of local MEs which are on the desired bus and may be suitable for connecting to
     * @throws ResourceException
     */
    void checkMEs(JsMessagingEngine[] MEList) throws ResourceException
    {
        final String methodName = "checkMEs";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, new Object[] { MEList });
        }

        // Filter out any non preferred MEs. User specified target data is used to perform this filter.
        // If no target data is set then all the MEs are considered "preferred" for point to point and
        // non durable pub sub, but none of them are considered preferred for durable pub sub (which has
        // a preference for the durable subscription home)
        JsMessagingEngine[] preferredMEs = _destinationStrategy.getPreferredLocalMEs(MEList);

        // TODO: Can we wrapper the connect call if a try catch block, if engine is being reloaded then absorb the
        // exception (trace a warning) and let us kick off a timer (if one is needed).

        // Try to connect to the list of filtered MEs.
        try {
            connect(preferredMEs, _targetType, _targetSignificance, _target, true);
            SibTr.info(TRACE, "TARGETTED_CONNECTION_SUCCESSFUL_CWSIV0556", new Object[] { ((MDBMessageEndpointFactory) _messageEndpointFactory).getActivationSpecId(),
                                                                                         _endpointConfiguration.getDestination().getDestinationName() });
        } catch (Exception e) {

            // After attempting to create connections check to see if we should continue to check for more connections
            // or not. If we should then kick of a timer to try again after a user specified interval.
            SibTr.warning(TRACE, SibTr.Suppressor.ALL_FOR_A_WHILE, "CONNECT_FAILED_CWSIV0782",
                          new Object[] { _endpointConfiguration.getDestination().getDestinationName(),
                                        _endpointConfiguration.getBusName(),
                                        ((MDBMessageEndpointFactory) _messageEndpointFactory).getActivationSpecId(),
                                        e });
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
            {
                SibTr.debug(TRACE, "Failed to obtain a connection - retry after a set interval");
            }
            clearTimer();
            // deactivate will close the connections
            // The connection might be successful but session might fail due to authorization error
            // Hence before retrying, old connection must be closed
            deactivate();
            kickOffTimer();

        }

        //its possible that there was no exception thrown and connection was not successfull
        // in that case a check is made to see if an retry attempt is needed
        if (_destinationStrategy.isTimerNeeded())
        {
            clearTimer();
            kickOffTimer();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(TRACE, methodName);
        }
    }

    /**
     * Kicks of a timer to attempt to create connections after a user specified interval.
     * 
     * @throws UnavailableException
     */
    void kickOffTimer() throws UnavailableException
    {
        final String methodName = "kickOffTimer";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName);
        }

        synchronized (_timerLock)
        {
            // Another timer is already running - no need to create a new one
            if (_timer != null)
            {
                return;
            }

            _timer = _bootstrapContext.createTimer();

            _timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    try
                    {
                        synchronized (_timerLock)
                        {
                            _timer.cancel();
                            _timer = null;
                            timerLoop();
                        }
                    }
                    catch (final ResourceException exception)
                    {
                        FFDCFilter.processException(exception, CLASS_NAME + "."
                                                               + methodName, "1:420:1.45", this);

                        SibTr.error(TRACE, "CONNECT_FAILED_CWSIV0783", new Object[] {
                                                                                     _endpointConfiguration.getDestination()
                                                                                                     .getDestinationName(),
                                                                                     _endpointConfiguration.getBusName(), this, exception });

                    }

                }
            }, _retryInterval);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(TRACE, methodName);
        }
    }

    /**
     * Stops any existing timers and resets the _timer variable
     */
    public void clearTimer()
    {
        synchronized (_timerLock)
        {
            if (_timer != null)
            {
                _timer.cancel();
                _timer = null;
            }
        }
    }

    /**
     * Attempts to connect to an ME. Attempts will be made to the supplied ME list if the
     * list is not empty (these are local MEs). If the list is empty then TRM will be called
     * to attempt to get a connection.
     * 
     * @param MEList A list of local MEs that match the target data. Having no target data means
     *            that all MEs will match for point to point and non durable pub sub and no messaging
     *            engines will match for durable pub sub
     * @param targetType Whether the target is an ME or BusMember
     * @param targetSignificance Whether the target significance is required or preferred
     * @param target The target itself
     * @param isPreferred Whether we are attempting to connect to a preferred ME. This will
     *            be true for the first attempts, but if target significance is set to preferred and no
     *            connection was made to a preferred ME then this method will be called again to locaet
     *            any ME but this time isPreferred will be set to false.
     * @throws ResourceException
     */
    void connect(JsMessagingEngine[] MEList, String targetType, String targetSignificance, String target,
                 boolean isPreferred) throws ResourceException
    {
        final String methodName = "connect";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, new Object[] { MEList, targetType, targetSignificance, target, isPreferred });
        }

        // TRM will decide if we have to connect to local or remote ME we needn't do a check
        connectUsingTrm(targetType, targetSignificance, target, isPreferred);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(TRACE, methodName);
        }
    }

    /**
     * This method is called if no local MEs are found which match the target data.
     * 
     * @param targetType Whether the target is an ME or BusMember
     * @param targetSignificance Whether the target significance is required or preferred
     * @param target The target itself
     * @param isPreferred Whether we are attempting to connect to a preferred ME. This will
     *            be true for the first attempts, but if target significance is set to preferred and no
     *            connection was made to a preferred ME then this method will be called again to locate
     *            any ME but this time isPreferred will be set to false.
     * @throws ResourceException
     */
    void connectUsingTrm(String targetType, String targetSignificance, String target, boolean isPreferred)
                    throws ResourceException
    {
        final String methodName = "connectUsingTrm";

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, new Object[] { targetType, targetSignificance,
                                                               target, isPreferred });
        }

        //We always have the information whether to connect to local or remote ME
        connectUsingTrmWithTargetData(targetType, targetSignificance, target);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(TRACE, methodName);
        }
    }

    /**
     * This method will try to connect to an ME using the supplied target properties
     * 
     * @param targetType - The type of target
     * @param targetSignificance - Target significane (preferred or required)
     * @param target - The name of the target
     * @throws ResourceException
     */
    public void connectUsingTrmWithTargetData(String targetType, String targetSignificance, String target)
                    throws ResourceException
    {
        final String methodName = "connectUsingTrmWithTargetData";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, new Object[] { targetType, targetSignificance, target });
        }

        synchronized (_connections)
        {
            // At this point in the code path we can be sure that:
            // 1. There are no local preferred ME's (this method would not be called if there were)
            // 2. Target data has been set (we are in the else block of the "if (target == null)" block
            // 
            // Since we have been passed target data it means we are attempting to create a connection
            // to a preferred (or required) ME (no target data is ever passed if we are trying to create
            // a connection to a non preferred ME). With this in mind, if we are currently connected to
            // a non preferred ME (_connectToPreferred is false) or we do not currently have a connection
            // then we'll try and create a connection that matches our target data. If a connection
            // is created then we should close any non preferred connections (if they are any).
            if ((!_connectedToPreferred) || (_connections.size() == 0))
            {
                // Set to required (even if user opted for preferred)
                // Pass targetType and target from user data.
                SibRaMessagingEngineConnection newConnection = null;
                try
                {
                    newConnection = new SibRaMessagingEngineConnection(this, _endpointConfiguration.getBusName(),
                                    targetType, SibTrmConstants.TARGET_SIGNIFICANCE_REQUIRED, target);

                    dropNonPreferredConnections();

                    if (newConnection.getConnection() != null) {
                        _connections.put(newConnection.getConnection().getMeUuid(), newConnection);
                        createListener(newConnection);

                    }

                    if (_connections.size() > 0)
                    {
                        _connectedRemotely = checkIfRemote(_connections.values()
                                        .iterator().next());
                        _connectedToPreferred = true;

                        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                        {
                            SibTr.debug(TRACE, "We have connect <remote=" + _connectedRemotely + " > <preferred="
                                               + _connectedToPreferred + ">");
                        }
                    }
                } catch (final SIResourceException exception)
                {
                    // No FFDC code needed
                    // We are potentially connecting remotely so this error may be transient
                    // Possibly the remote ME is not available
                    SibTr.warning(TRACE, SibTr.Suppressor.ALL_FOR_A_WHILE, "TARGETTED_CONNECTION_FAILED_CWSIV0787",
                                  new Object[] { targetType, targetSignificance, target, _endpointConfiguration.getBusName() });
                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                    {
                        SibTr.debug(TRACE, "Failed to obtain a connection - retry after a set interval");
                    }
                } catch (final SIException exception)
                {
                    FFDCFilter.processException(exception, CLASS_NAME + "." + methodName, "1:711:1.45", this);

                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled())
                    {
                        SibTr.exception(this, TRACE, exception);
                    }

                    throw new ResourceException(NLS.getFormattedMessage("CONNECT_FAILED_CWSIV0782", new Object[] {
                                                                                                                  _endpointConfiguration.getDestination().getDestinationName(),
                                                                                                                  _endpointConfiguration.getBusName(), this, exception }, null), exception);
                }
            }
        } // Sync block

        // Failed to get a preferred one, try for any ME next.
        // Also if we have connected to a remote non preferred ME we may wish to try again for a local
        // non preferred ME (We know there is not a local preferred ME as we would not be in this
        // method if there were).
        if (((_connections.size() == 0) || (_connectedRemotely && !_connectedToPreferred && _destinationStrategy
                        .isDropRemoteNonPreferredForLocalNonPreferred()))
            && (targetSignificance.equals(SibTrmConstants.TARGET_SIGNIFICANCE_PREFERRED)))
        {
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
            {
                SibTr.debug(TRACE,
                            "Could not obtain the preferred connection - try again without any target preferences");
            }
            // For durable pub sub there are no local preferred MEs, for point to point and non durable pub
            // sub then they all count as preferred
            connect(_destinationStrategy.isDurablePubsSub() ? null : getMEsToCheck(), null, null, null, false);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(TRACE, methodName);
        }
    }

    /**
     * This method will create a listener to the specified destination
     * 
     * @param connection The connection to an ME
     */
    private void createSingleListener(final SibRaMessagingEngineConnection connection) throws ResourceException
    {
        final String methodName = "createSingleListener";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, connection);
        }

        final SIDestinationAddress destination = _endpointConfiguration.getDestination();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
        {
            SibTr.debug(TRACE, "Creating a consumer to consume from destination "
                               + destination + " on ME " + connection.getConnection().getMeName());
        }

        try
        {
            connection.createListener(destination, _messageEndpointFactory);
            SibTr.info(TRACE, "CONNECTED_CWSIV0777", new Object[] {
                                                                   connection.getConnection().getMeName(),
                                                                   _endpointConfiguration.getDestination()
                                                                                   .getDestinationName(),
                                                                   _endpointConfiguration.getBusName() });
        } catch (final IllegalStateException exception)
        {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
            {
                SibTr.debug(TRACE, "Failed to create a session - blowing away the connection - rethrow the exception");
            }

            _connections.remove(connection.getConnection().getMeUuid());
            connection.close();
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
            {
                SibTr.exit(this, TRACE, methodName);
            }
            throw exception;
        } catch (final ResourceException exception)
        {
            // No FFDC code needed

            // Failed to create a consumer so blow away the connection and try
            // again
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
            {
                SibTr.debug(TRACE, "Failed to create a session - blowing away the connection - a retry should occur");
                SibTr.debug(TRACE, "Exception cause was " + exception.getCause());
            }

            _connections.remove(connection.getConnection().getMeUuid());
            connection.close();
            throw exception;
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * This method will create a listener to each destination that matches the wildcarded destination
     * 
     * @param connection The connection to an ME
     */
    private void createMultipleListeners(final SibRaMessagingEngineConnection connection) throws ResourceException
    {
        final String methodName = "createMultipleListeners";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, connection);
        }

        try
        {
            final SICoreConnection coreConnection = connection.getConnection();

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
                            .addDestinationListener(_endpointConfiguration.getDestinationName(),
                                                    destinationListener,
                                                    destinationType,
                                                    DestinationAvailability.RECEIVE);

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
            {
                SibTr.debug(P2PTRACE, "Found " + destinations.length + " destinations that make the wildcard");
            }

            /*
             * Create a listener for each destination ...
             */

            for (int j = 0; j < destinations.length; j++)
            {
                try
                {
                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                    {
                        SibTr.debug(P2PTRACE, "Creating a consumer for destination " + destinations[j]);
                    }
                    connection.createListener(destinations[j], _messageEndpointFactory);
                } catch (final ResourceException exception)
                {
                    FFDCFilter.processException(exception, CLASS_NAME + "."
                                                           + methodName, "1:877:1.45", this);
                    SibTr.error(TRACE, "CREATE_LISTENER_FAILED_CWSIV0803",
                                new Object[] { exception,
                                              destinations[j].getDestinationName(),
                                              connection.getConnection().getMeName(),
                                              connection.getBusName() });
                }
            }
        } catch (final SIException exception)
        {
            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:889:1.45", this);
            SibTr.error(TRACE, "ADD_DESTINATION_LISTENER_FAILED_CWSIV0804",
                        new Object[] { exception, connection.getConnection().getMeName(),
                                      connection.getBusName() });
            _connections.remove(connection.getConnection().getMeUuid());
            connection.close();
        }

        if (connection.getNumberListeners() == 0)
        {
            SibTr.warning(TRACE, "NO_LISTENERS_CREATED_CWSIV0809", new Object[] { _endpointConfiguration.getDestinationName(),
                                                                                 connection.getConnection().getMeName(),
                                                                                 connection.getBusName() });
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * Creates a listener to the configured destination using the given connection.
     * 
     * @param connection
     *            the connection to the messaging engine
     * @throws ResourceException
     *             if the creation of the listener fails
     */
    private void createListener(final SibRaMessagingEngineConnection connection)
                    throws ResourceException
    {

        final String methodName = "createListener";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, connection);
        }

        if (_endpointConfiguration.getUseDestinationWildcard())
        {
            createMultipleListeners(connection);
        }
        else
        {
            createSingleListener(connection);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * This method checks to see if the specified connection is to a remote ME.
     * It will check the name of the ME the connection is connected to againgst the
     * list of local MEs that are on the locla server.
     * 
     * @param conn The connection to check.
     * @return True if the connection passed is a connection to a remote ME
     */
    boolean checkIfRemote(SibRaMessagingEngineConnection conn)
    {
        final String methodName = "checkIfRemote";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, new Object[] { conn });
        }

        String meName = conn.getConnection().getMeName();
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
        {
            SibTr.debug(TRACE, "Connections's ME name " + meName);
        }
        boolean remote = true;
        JsMessagingEngine[] localMEs = SibRaEngineComponent.getActiveMessagingEngines(_endpointConfiguration.getBusName());
        for (int i = 0; i < localMEs.length; i++)
        {
            JsMessagingEngine me = localMEs[i];
            String localName = me.getName();

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
            {
                SibTr.debug(TRACE, "Checking ME name " + localName);
            }

            if (localName.equals(meName))
            {
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                {
                    SibTr.debug(TRACE, "Me name matched, the connection is local");
                }
                remote = false;
                break;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(TRACE, methodName, remote);
        }

        return remote;
    }

    /**
     * This method will close any connections that are considered "non preferred". Non
     * preferred connections are only created when target significane is set to preferred
     * and the system was not able to create a connection that match the target data.
     */
    void dropNonPreferredConnections()
    {
        final String methodName = "dropNonPreferredConnections";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName);
        }

        // If we are connected to a non preferred then we will NEVER be connect to a preferred as 
        // well, there is no mixing of connection types.
        if (!_connectedToPreferred)
        {
            closeConnections();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(TRACE, methodName);
        }
    }

    /**
     * If we are connected remotely then drop the connection.
     */
    void dropRemoteConnections()
    {
        final String methodName = "dropRemoteConnections";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName);
        }

        if (_connectedRemotely)
        {
            closeConnections();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(TRACE, methodName);
        }
    }

    /**
     * Close all the connections we currently have open.
     */
    void closeConnections()
    {
        final String methodName = "closeConnections";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName);
        }

        synchronized (_connections)
        {
            Collection<SibRaMessagingEngineConnection> cons = _connections.values();
            Iterator<SibRaMessagingEngineConnection> iter = cons.iterator();
            while (iter.hasNext())
            {
                SibRaMessagingEngineConnection connection = iter.next();
                connection.close();
            }
            _connections.clear();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(TRACE, methodName);
        }
    }

    /**
     * This method checks to see if the supplied ME matches the target data being used.
     * 
     * @param ME The messaging engine to check
     * @return True if the specified ME matches the target data.
     */

    //lohith liberty change, there They are trying to confirm the target which is always true in liberty
    boolean matchesTargetData(JsMessagingEngine ME)
    {/*
      * final String methodName = "matchesTargetData";
      * if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
      * {
      * SibTr.entry(this, TRACE, methodName, new Object[] { ME });
      * }
      * 
      * if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
      * {
      * SibTr.debug(TRACE, "Target type " + _targetType);
      * }
      * 
      * // Check preference data
      * boolean matchesPreference = false;
      * 
      * if (_targetType.equals(SibTrmConstants.TARGET_TYPE_BUSMEMBER))
      * {
      * if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
      * {
      * SibTr.debug(TRACE, "Target type is BusMember");
      * }
      * // Work out the bus member name. We use this later to connect to the correct
      * // bus member in the bus.
      * String busMember;
      * try
      * {
      * Server serverService = AccessController.doPrivileged(new PrivilegedExceptionAction<Server>()
      * {
      * public Server run() throws Exception
      * {
      * return (Server) WsServiceRegistry.getService(this, Server.class);
      * }
      * });
      * String clusterName = serverService.getClusterName();
      * String nodeName = serverService.getNodeName();
      * String appServerName = serverService.getName();
      * 
      * // Ensure that the busmember is fully qualified by the node
      * // name, but only in the app server case.
      * if(clusterName != null)
      * {
      * busMember = clusterName;
      * }
      * else
      * {
      * busMember = nodeName + "." + appServerName;
      * }
      * if (_target.equals (busMember))
      * {
      * matchesPreference = true;
      * }
      * }
      * catch (Exception ex)
      * {
      * FFDCFilter.processException(ex, CLASS_NAME + "."
      * + methodName, "1:1132:1.45", this);
      * }
      * }
      * else if (_targetType.equals (SibTrmConstants.TARGET_TYPE_ME))
      * {
      * if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
      * {
      * SibTr.debug(TRACE, "Target type is ME");
      * }
      * if (_target.equals (ME.getName()))
      * {
      * matchesPreference = true;
      * }
      * }
      * else if (_targetType.equals(SibTrmConstants.TARGET_TYPE_CUSTOM))
      * {
      * if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
      * {
      * SibTr.debug(TRACE, "Target type is custom - available ones are " + SibRaEngineComponent.getCustomGroups(ME.getUuid().toString ()));
      * }
      * Set customGroups = SibRaEngineComponent.getCustomGroups(ME.getUuid().toString ());
      * 
      * if (customGroups.contains(_target))
      * {
      * matchesPreference = true;
      * }
      * }
      * else
      * {
      * if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
      * {
      * SibTr.debug(TRACE, "Target type does not match <" + SibTrmConstants.TARGET_TYPE_BUSMEMBER +
      * "> <" + SibTrmConstants.TARGET_TYPE_ME + "> <" +
      * SibTrmConstants.TARGET_TYPE_CUSTOM + ">");
      * }
      * }
      * 
      * if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
      * {
      * SibTr.exit(TRACE, methodName, matchesPreference);
      * }
      * 
      * return matchesPreference;
      */

        return true;
    }

    /**
     * Deactivates the MDB
     */
    @Override
    void deactivate()
    {
        final String methodName = "deactivate";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName);
        }

        clearTimer();

        super.deactivate();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * This method is the "shoulder tap" we get when a new local messaging engine has
     * just started up. The code will check to see if a connection is required to this new
     * messaging engine.
     */
    @Override
    void addMessagingEngine(JsMessagingEngine messagingEngine) throws ResourceException
    {
        final String methodName = "addMessagingEngine";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, messagingEngine);
        }
        if ((_destinationStrategy.isNonDurablePubSub()) && (_connections.size() > 0))
        {
            // Ignore for non durable pub sub
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
            {
                SibTr.exit(this, TRACE, methodName);
            }
            return;
        }
        else
        {
            clearTimer();
            checkMEs(new JsMessagingEngine[] { messagingEngine });
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * An error has been detected on the connection, drop the connection and, if necessary,
     * try to create a new connection
     */
    @Override
    void connectionError(SibRaMessagingEngineConnection connection, SIException exception)
    {
        String methodName = "connectionError";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection, exception });
        }

        /*
         * We have lost our remote connection, output a warning to the
         * admin console, close the connection and schedule an attempt
         * to create a new one
         */

        SibTr.warning(TRACE, "CONNECTION_ERROR_CWSIV0776",
                      new Object[] { connection.getConnection().getMeName(),
                                    _endpointConfiguration.getBusName(), this,
                                    exception });

        dropConnection(connection, false, true, true);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * The messaging engine is stopping, drop the connection and, if necessary,
     * try to create a new connection
     */
    @Override
    void messagingEngineQuiescing(SibRaMessagingEngineConnection connection)
    {
        final String methodName = "messagingEngineQuiescing";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, connection);
        }

        SibTr.info(TRACE, "ME_QUIESCING_CWSIV0785", new Object[] {
                                                                  connection.getConnection().getMeName(),
                                                                  _endpointConfiguration.getBusName() });

        // Change the last parameters to true to stop the connection trying to stop the
        // consumer. This is because the event is now processed async and the connection may get 
        // closed before our thread tries to stop the consumer session.
        dropConnection(connection, false, true, true);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * The messaging engine has stopped, drop the connection and, if necessary,
     * try to create a new connection
     */
    @Override
    void messagingEngineTerminated(SibRaMessagingEngineConnection connection)
    {
        final String methodName = "messagingEngineTerminated";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, connection);
        }

        SibTr.info(TRACE, "ME_TERMINATED_CWSIV0786", new Object[] {
                                                                   connection.getConnection().getMeName(),
                                                                   _endpointConfiguration.getBusName() });
        dropConnection(connection, false, true, true);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * A session error has occured on the connection, drop the connection and, if necessary,
     * try to create a new connection
     */
    @Override
    void sessionError(SibRaMessagingEngineConnection connection, ConsumerSession session, Throwable throwable)
    {
        final String methodName = "sessionError";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection, session });
        }

        final SIDestinationAddress destination = session.getDestinationAddress();
        SibTr.warning(TRACE, "CONSUMER_FAILED_CWSIV0770", new Object[] {
                                                                        destination.getDestinationName(),
                                                                        _endpointConfiguration.getBusName(), this, throwable });

        dropConnection(connection, true, true, false);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * Overrides the parent method so that if there are no connections left it will
     * check to see if a new connection can be made
     * 
     * @param messagingEngine
     *            the messaging engine that is stopping
     * @param mode
     *            the mode with which the engine is stopping
     */
    @Override
    public synchronized void messagingEngineStopping(
                                                     final JsMessagingEngine messagingEngine, final int mode)
    {

        final String methodName = "messagingEngineStopping";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, new Object[] { messagingEngine, Integer.valueOf(mode) });
        }

        SibTr.info(TRACE, "ME_STOPPING_CWSIV0784", new Object[] {
                                                                 messagingEngine.getName(),
                                                                 messagingEngine.getBus() });

//        dropConnection (messagingEngine.getUuid().toString(), null, true);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * Drop the connection to the specified ME. If we have no connections left
     * then try to create a new connection.
     * 
     * @param meUuid The Uuid of the ME we should drop our connection to.
     */
    void dropConnection(SibRaMessagingEngineConnection connection, boolean isSessionError, boolean retryImmediately, boolean alreadyClosed)
    {
        String methodName = "dropConnection";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName,
                        new Object[] { connection, Boolean.valueOf(isSessionError), Boolean.valueOf(retryImmediately), Boolean.valueOf(alreadyClosed) });
        }

        SibRaMessagingEngineConnection connectionCheck = null;
        String meUuid = connection.getConnection().getMeUuid();

        synchronized (_connections)
        {
            /*
             * Does the connection that we've been passed, still exist?
             */
            connectionCheck = _connections.get(meUuid);

            /*
             * If the connection object that we've been passed is the same object as that retrieved from _connections, close it.
             */
            if (connection == connectionCheck)
            {
                closeConnection(meUuid, alreadyClosed);
            }
        }

        // If we are reloading the engine then don't try and reconnect now. Wait until we get an engine 
        // reloaded shoulder tap. Since we don't know if remote MEs are having an engine reloaded we will fail 
        // the connection and deactivate the MDB. The connection is only passed in as a parameter from session
        // error method call.
        if (!isSessionError ||
            (!SibRaEngineComponent.isMessagingEngineReloading(meUuid)))
        {
            // PM49608 The lock hierarchy is first _timerLock and then _connections
            // This hierarchy is necessary to avoid deadlock
            synchronized (_timerLock)
            {
                synchronized (_connections)
                {
                    // If this was our last (or only) connection then kick off another loop to check if
                    // we can obtain a new connection.
                    if (_connections.size() == 0)
                    {
                        try
                        {
                            clearTimer();
                            if (retryImmediately)
                            {
                                timerLoop();
                            }
                            else
                            {
                                kickOffTimer();
                            }
                        } catch (final ResourceException resEx)
                        {
                            FFDCFilter.processException(resEx, CLASS_NAME + "."
                                                               + methodName, "1:1434:1.45", this);

                            SibTr.error(TRACE, "CONNECT_FAILED_CWSIV0783", new Object[] {
                                                                                         _endpointConfiguration.getDestination()
                                                                                                         .getDestinationName(),
                                                                                         _endpointConfiguration.getBusName(), this, resEx });
                        }
                    }
                }
            }
        }
        else
        {
            // Stop any timers as no point in trying again until we have finished reloading the engine.
            clearTimer();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * The messaging engine has been destroyed, nothing to do here.
     */
    @Override
    public void messagingEngineDestroyed(JsMessagingEngine messagingEngine)
    {
        final String methodName = "messagingEngineDestroyed";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, messagingEngine);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * A new messaging engine has been initialised, we don't care so nothing to do here.
     */
    @Override
    public void messagingEngineInitializing(JsMessagingEngine messagingEngine)
    {
        final String methodName = "messagingEngineInitializing";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, messagingEngine);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
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
    @Override
    public void messagingEngineReloaded(JsMessagingEngine engine)
    {
        final String methodName = "messagingEngineReloaded";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, engine);
        }

        // We are now catching problems when ME's are not started and not throwing an error.
        // As such this check is not needed and is causing problems. The problem is that
        // we could have a cluster of MEs with only one ME running. If a queue is deleted
        // then recreated only the MDB that is sitting alongside the ME will reconnect, 
        // the other MEs will wait for the local ME to start to try to reconnect but this
        // may never happen so we need to try to reconnect at this point.
        try
        {
            clearTimer();
            timerLoop();
        } catch (final ResourceException exception)
        {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled())
            {
                SibTr.exception(this, TRACE, exception);
            }

            SibTr.error(TRACE, "RELOAD_FAILED_CWSIV0773", new Object[] {
                                                                        exception, engine.getName(), engine.getBusName(), this });
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
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
                                                   + methodName, "1:1653:1.45");
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(TRACE, exception);
            }
            throw new NotSupportedException(NLS.getFormattedMessage(
                                                                    ("NOT_FOUND_CWSIV0757"), new Object[] { destinationName,
                                                                                                           resolvedBusName }, null), exception);

        } catch (final SIBExceptionBase exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                                                   + methodName, "1:1664:1.45");
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
     * Abstract base class for the different destination strategies used by
     * SibRaCommonEndpointActivation
     */

    abstract class DestinationStrategy
    {
        SibRaCommonEndpointActivation _parent;

        /**
         * Constructor, tracks the parent enpoint activation object.
         * 
         * @param parent
         */
        DestinationStrategy(SibRaCommonEndpointActivation parent)
        {
            _parent = parent;
        }

        /**
         * Checks if the destination strategy is point to point
         * 
         * @return true if the destination stratey is point to point, false otherwise
         */
        public boolean isPointToPoint()
        {
            return (this instanceof PointToPointStrategy);
        }

        /**
         * Only point to point will drop a remote non preferred conn for a local non preferred conn so
         * return false and let PointToPoint override.
         * 
         * @return false
         */
        public boolean isDropRemoteNonPreferredForLocalNonPreferred()
        {
            return false;
        }

        /**
         * Checks if the destination strategy is durable pub sub
         * 
         * @return true if the destination stratey is durable pub sub, false otherwise
         */
        public boolean isDurablePubsSub()
        {
            return (this instanceof DurablePubSubStrategy);
        }

        /**
         * Checks if the destination strategy is non durable pub sub
         * 
         * @return true if the destination stratey is non durable pub sub, false otherwise
         */
        public boolean isNonDurablePubSub()
        {
            return (this instanceof NonDurablePubSubStrategy);
        }

        /**
         * Given a set of local MEs this method will filter out the non preferred MEs.
         * If no target data is set then all the MEs are classified as preferred. This is
         * overridden in the durable pub sub case sa for durable pub sub, if no target data
         * is set then none of the the MEs are considered preferred (a preference for the
         * durable subscription home would be used instead).
         * 
         * @param MEList The list of local MEs to check.
         * @return The list of MEs that are considered preferred (that match the target
         *         data if any target data is set).
         */
        JsMessagingEngine[] getPreferredLocalMEs(JsMessagingEngine[] MEList)
        {
            final String methodName = "getPreferredLocalMEs";
            if (TraceComponent.isAnyTracingEnabled() && DESTTRACE.isEntryEnabled())
            {
                SibTr.entry(this, DESTTRACE, methodName, new Object[] { MEList });
            }

            JsMessagingEngine[] preferredMEs;
            if ((_target == null) || ("".equals(_target)))
            {
                preferredMEs = MEList;
            }
            else
            {
                List<JsMessagingEngine> prefList = new ArrayList<JsMessagingEngine>();
                for (int i = 0; i < MEList.length; i++)
                {
                    JsMessagingEngine ME = MEList[i];
                    if (matchesTargetData(ME))
                    {
                        prefList.add(ME);
                    }
                }
                preferredMEs = new JsMessagingEngine[prefList.size()];
                prefList.toArray(preferredMEs);
            }

            if (TraceComponent.isAnyTracingEnabled() && DESTTRACE.isEntryEnabled())
            {
                SibTr.exit(DESTTRACE, methodName, preferredMEs);
            }

            return preferredMEs;
        }

        /**
         * Checks to see if the timer should be kicked off to attempt to create a connection
         * (which could be the first connection or it could be searching for an improved
         * connection).
         * 
         * @return True if the timer should be kicked off
         */
        public abstract boolean isTimerNeeded();

    }

    /**
     * Point to point destination strategy
     */
    class PointToPointStrategy extends DestinationStrategy
    {
        PointToPointStrategy(SibRaCommonEndpointActivation parent)
        {
            super(parent);
        }

        /**
         * Only point to point will drop a remote non preferred conn for a local non preferred conn so
         * return true
         * 
         * @return true
         */
        @Override
        public boolean isDropRemoteNonPreferredForLocalNonPreferred()
        {
            return true;
        }

        /**
         * This method checks to see if we need to kick of the timer. If there
         * have not been any connections made then the timer should be used. If we have
         * a connection and it is not a connection to a local preferred ME then the timer is
         * needed. There is no point kicking off the timer if we have a local preferred ME
         * as we can not get a better connection. We can however connect to more local MEs if
         * they localise the destination but we will get a shoulder tap for that and the timer
         * is not needed.
         */
        @Override
        public boolean isTimerNeeded()
        {
            final String methodName = "isTimerNeeded";
            if (TraceComponent.isAnyTracingEnabled() && P2PTRACE.isEntryEnabled())
            {
                SibTr.entry(this, P2PTRACE, methodName);
            }

//            boolean localPreferredConnection = _connectedToPreferred && !_connectedRemotely;
//            boolean timerNeeded = ((_connections.size () == 0) || (!localPreferredConnection));
            // We don't need to check if the connection is local or not. For a timer we only need
            // to know if we are connected to a preferred (or required) target or not.
            // A local preferred connection is better than a remote preferred one but if a local
            // one becomes available we will get a shoulder tap.
            boolean timerNeeded = ((_connections.size() == 0) || (!_connectedToPreferred));

            if (TraceComponent.isAnyTracingEnabled() && P2PTRACE.isEntryEnabled())
            {
                SibTr.exit(P2PTRACE, methodName, timerNeeded);
            }

            return timerNeeded;
        }

    }

    /**
     * Durable pub sub connection strategy
     */
    class DurablePubSubStrategy extends DestinationStrategy
    {
        DurablePubSubStrategy(SibRaCommonEndpointActivation parent)
        {
            super(parent);
        }

        /**
         * This method overrides the default behaviour in the base class. If no target
         * data is set then the list of preferred MEs is empty (durable pub sub has a
         * preference for the durable subscription home so none of the local MEs will
         * be said ot match the target data).
         */
        @Override
        JsMessagingEngine[] getPreferredLocalMEs(JsMessagingEngine[] MEList)
        {
            final String methodName = "getPreferredLocalMEs";
            if (TraceComponent.isAnyTracingEnabled() && DPSTRACE.isEntryEnabled())
            {
                SibTr.entry(this, DPSTRACE, methodName, new Object[] { MEList });
            }

            JsMessagingEngine[] preferredMEs;

            // If we have no target data and we want to restrict ourselves to only the DSH then return null here
            if (((_target == null) || ("".equals(_target))) && (onlyConnectToDSH()))
            {
                preferredMEs = null;
            }
            else
            {
                preferredMEs = super.getPreferredLocalMEs(MEList);
            }

            if (TraceComponent.isAnyTracingEnabled() && DPSTRACE.isEntryEnabled())
            {
                SibTr.exit(DPSTRACE, methodName, preferredMEs);
            }

            return preferredMEs;
        }

        /**
         * The timer is only needed if we don't yet have a connection or we are connected
         * to a non preferred ME (this only happens if the target significance is set
         * to preferred and we were unable to match that preference and instead obtained a
         * connection to a non preferred ME).
         */
        @Override
        public boolean isTimerNeeded()
        {
            final String methodName = "isTimerNeeded";
            if (TraceComponent.isAnyTracingEnabled() && DPSTRACE.isEntryEnabled())
            {
                SibTr.entry(this, DPSTRACE, methodName);
            }

            boolean timerNeeded = ((_connections.size() == 0) || (!_connectedToPreferred));

            if (TraceComponent.isAnyTracingEnabled() && DPSTRACE.isEntryEnabled())
            {
                SibTr.exit(DPSTRACE, methodName, timerNeeded);
            }

            return timerNeeded;
        }

    }

    /**
     * Non durable pub sub connection strategy
     */
    class NonDurablePubSubStrategy extends DestinationStrategy
    {
        NonDurablePubSubStrategy(SibRaCommonEndpointActivation parent)
        {
            super(parent);
        }

        /**
         * For non durable pub sub, any of the local MEs will suffice. A connection to a random ME in the
         * supplied list will be made.
         */
        public void connectToLocalMEs(JsMessagingEngine[] MEList) throws ResourceException
        {
            final String methodName = "connectToLocalMEs";
            if (TraceComponent.isAnyTracingEnabled() && NDPSTRACE.isEntryEnabled())
            {
                SibTr.entry(this, NDPSTRACE, methodName, new Object[] { MEList });
            }

            if (_connections.size() == 0)
            {
                final int randomIndex = _random.nextInt(MEList.length);
                SibRaMessagingEngineConnection newConnection = getConnection(MEList[randomIndex]);
                createListener(newConnection);
            }

            if (TraceComponent.isAnyTracingEnabled() && NDPSTRACE.isEntryEnabled())
            {
                SibTr.exit(NDPSTRACE, methodName);
            }
        }

        /**
         * A timer is only needed for non durable pub sub if no connection has yet been made. Non durable
         * pub sub will never drop a connection in favour of another one.
         */
        @Override
        public boolean isTimerNeeded()
        {
            final String methodName = "isTimerNeeded";
            if (TraceComponent.isAnyTracingEnabled() && NDPSTRACE.isEntryEnabled())
            {
                SibTr.entry(this, NDPSTRACE, methodName);
            }

            boolean timerNeeded = (_connections.size() == 0);

            if (TraceComponent.isAnyTracingEnabled() && NDPSTRACE.isEntryEnabled())
            {
                SibTr.exit(NDPSTRACE, methodName, timerNeeded);
            }

            return timerNeeded;
        }

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

        private final MessageEndpointFactory _messageEndpointFactory;

        /**
         * Constructor.
         * 
         * @param connection
         *            the connection on which to create listeners
         */
        private SibRaDestinationListener(final SibRaMessagingEngineConnection connection,
                                         MessageEndpointFactory messageEndpointFactory)
        {
            final String methodName = "SibRaDestinationListener";
            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled())
            {
                SibTr.entry(this, LISTENER_TRACE, methodName, new Object[] { connection, messageEndpointFactory });
            }

            _connection = connection;
            _messageEndpointFactory = messageEndpointFactory;

            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled())
            {
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
        @Override
        public void destinationAvailable(final SICoreConnection connection,
                                         final SIDestinationAddress destination,
                                         final DestinationAvailability availability)
        {
            final String methodName = "destinationAvailable";
            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled())
            {
                SibTr.entry(this, LISTENER_TRACE, methodName, new Object[] { connection, destination, availability });
            }

            try
            {
                _connection.createListener(destination, _messageEndpointFactory);
            } catch (final ResourceException exception)
            {
                FFDCFilter.processException(exception, CLASS_NAME + "."
                                                       + methodName, "1:2400:1.45", this);
                SibTr.error(TRACE, "CREATE_LISTENER_FAILED_CWSIV0805",
                            new Object[] { exception,
                                          destination.getDestinationName(),
                                          connection.getMeName(),
                                          destination.getBusName() });
            }

            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled())
            {
                SibTr.exit(this, LISTENER_TRACE, methodName);
            }
        }

        /**
         * toString method for the inner class
         */
        @Override
        public String toString()
        {
            final SibRaStringGenerator generator = new SibRaStringGenerator(this);
            return generator.getStringRepresentation();
        }

    }
}
