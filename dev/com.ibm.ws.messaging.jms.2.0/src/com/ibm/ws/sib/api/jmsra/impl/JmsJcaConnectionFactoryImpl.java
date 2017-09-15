/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jmsra.impl;

import java.util.Map;

import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ResourceAdapterInternalException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.resource.ResourceRefInfo.Property;
import com.ibm.ws.sib.api.jms.service.JmsServiceFacade;
import com.ibm.ws.sib.api.jms.ute.UTEHelperFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnection;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnectionFactory;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;
import com.ibm.wsspi.sib.core.SICoreConnectionFactorySelector;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.selector.FactoryType;

/**
 * Implementaiton of connection factory interface between JMS API and resource
 * adapter. Provides factory methods for creating a connection. Holds references
 * to a managed connection factory and, in a managed environment, a connection
 * manager.
 */
final class JmsJcaConnectionFactoryImpl implements JmsJcaConnectionFactory {

    /**
     * The managed connection factory this connection factory was created from.
     */
    private final JmsJcaManagedConnectionFactoryImpl _managedConnectionFactory;

    /**
     * The connection manager passed on creation of this factory or
     * <code>null</code> in a non-managed environment.
     */
    private ConnectionManager _connectionManager;

    /**
     * Flag indicating whether this connection factory was created in a managed
     * environment.
     */
    private final boolean _managed;

    private static final String FFDC_PROBE_1 = "1";

    private static final String FFDC_PROBE_2 = "2";

    private static final String FFDC_PROBE_3 = "3";

    private static final String FFDC_PROBE_4 = "4";

    private static final String FFDC_PROBE_5 = "5";

    private static final String FFDC_PROBE_6 = "6";

    private static final String FFDC_PROBE_7 = "7";

    private static final String CLASS_NAME = JmsJcaConnectionFactoryImpl.class
                    .getName();

    private static final long serialVersionUID = 6803409986837640579L;

    private static TraceComponent TRACE = SibTr.register(
                                                         JmsJcaConnectionFactoryImpl.class, JmsraConstants.MSG_GROUP,
                                                         JmsraConstants.MSG_BUNDLE);

    private static TraceNLS NLS = TraceNLS
                    .getTraceNLS(JmsraConstants.MSG_BUNDLE);

    /**
     * Constructs a connection factory handle associated with the given managed
     * connection factory and connection manager. The <code>isManaged</code>
     * method will then return <code>true</code>.
     * 
     * @param managedConnectionFactory
     *            the managed connection
     * @param connectionManager
     *            the connection manager
     */
    JmsJcaConnectionFactoryImpl(
                                final JmsJcaManagedConnectionFactoryImpl managedConnectionFactory,
                                final ConnectionManager connectionManager) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr
                            .entry(this, TRACE, "JmsJcaConnectionFactoryImpl",
                                   new Object[] { managedConnectionFactory,
                                                 connectionManager });
        }

        _managedConnectionFactory = managedConnectionFactory;
        _connectionManager = connectionManager;

        /*
         * As of now setting _managed=false in case of client container. This would make all connections in client container
         * as non managed. in tWAS Java EE Client (i.e client container) does not support connection pooling
         */
        _managed = !JmsServiceFacade.isClientContainer();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "JmsJcaConnectionFactoryImpl");
        }
    }

    /**
     * Constructs a connection factory handle associated with the given managed
     * connection factory andno connection manager. The <code>isManaged</code>
     * method will then return <code>false</code>.
     * 
     * @param managedConnectionFactory
     *            the managed connection factory
     */
    JmsJcaConnectionFactoryImpl(
                                final JmsJcaManagedConnectionFactoryImpl managedConnectionFactory) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "JmsJcaConnectionFactoryImpl",
                        new Object[] { managedConnectionFactory });
        }

        _managedConnectionFactory = managedConnectionFactory;
        _managed = false;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "JmsJcaConnectionFactoryImpl");
        }
    }

    /**
     * Creates a new <code>JmsJcaConnection</code>. An empty
     * <code>JmsJcaConnectionRequestInfo</code> object is passed to the
     * <code>allocateConnection</code> method of the associated connection
     * manager. This returns a new <code>JmsJcaSession</code> from which the
     * associated <code>JmsJcaConnnection</code> can be obtained. This will
     * contain a core connection authenticated with the credentials from the
     * container or, if none are passed, those from the managed connection
     * factory. The session is held by the connection and returned on the first
     * call to <code>createSession</code>.
     * 
     * @return the connection
     * @throws javax.resource.ResourceException
     *             if the JCA runtime fails to allocate a connection
     * @throws SIException
     *             if the creation of the core connection fails
     * @throws SIErrorException
     *             if the creation of the core connection fails
     */
    @Override
    public JmsJcaConnection createConnection() throws ResourceException,
                    SIException, SIErrorException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createConnection");
        }

        JmsJcaConnection connection = null;

        JmsJcaConnectionRequestInfo requestInfo = new JmsJcaConnectionRequestInfo();

        // Used for WAS specific code where we can relay a more useful error message to the user
        // if we receive a SINotAuthorisedException or a SIAuthenticationException when calling allocateConnection
        boolean runningInWAS = false;

        // True if we aren using container managed authentication
        boolean containerAuth = false;

        // The containter managed authentication alias (only relevant if containerAuth is true)
        String containerAlias = null;

        try {

            if (_managed) {

                /*
                 * In a managed environment obtain a managed connection
                 */

                // Obtain authentication data in case an exception is thrown. This data is then used to give a more
                // meaningful error message. The information is obtained now as it may be altered later on. This is
                // only valid when running inside WAS.
                if (_connectionManager instanceof com.ibm.ws.jca.adapter.WSConnectionManager)
                {
                    runningInWAS = true;
                    ResourceRefInfo info = ((com.ibm.ws.jca.adapter.WSConnectionManager) _connectionManager).getResourceRefInfo();
                    containerAuth = info.getAuth() == com.ibm.ws.javaee.dd.common.ResourceRef.AUTH_CONTAINER;
                    for (Property loginConfigProp : info.getLoginPropertyList())
                        if ("DefaultPrincipalMapping".equals(loginConfigProp.getName()))
                            containerAlias = loginConfigProp.getValue();
                }

                // Try and allocate a connection.

                Object object = null;
                boolean tryAgain = true;

                // Keep going around the while loop until we manage to get a valid connection
                do {

                    // Try and allocate a connection.
                    object = _connectionManager.allocateConnection(
                                                                   _managedConnectionFactory, requestInfo);

                    // Ensure that we have a session of the right type.
                    if (object instanceof JmsJcaSessionImpl) {

                        // Get the core connection from the managed connection
                        SICoreConnection coreConnection = ((JmsJcaSessionImpl) object).getManagedConnection().getCoreConnection();

                        // PM07867 It is very important that the connection request info structure
                        // has a reference to a core connection, that is connected to the same
                        // ME as our saved core connection. If a new connection was created as part of
                        // allocateConnection then this will already be the case.
                        // However, if an existing connection was re-used from the pool, no
                        // connection will have been set on the connection request info, and
                        // JCA sessions created from this JCA connection might end up connecting
                        // to different messaging engines, preventing transaction completion.
                        if (requestInfo.getSICoreConnection() == null) {
                            requestInfo.setSICoreConnection(coreConnection);
                        }

                        SICoreConnection clonedCoreConnection = null;

                        // Clone the managed connection owned core connection for use by the connection
                        // object which we will return to JCA. Previous connection failures will only
                        // become apparent here so we catch any cloning exception and handle it as a
                        // connection failure on the managed connection.

                        Exception clonedException = null;
                        if (coreConnection != null) {
                            try {
                                clonedCoreConnection = coreConnection.cloneConnection();
                            } catch (SIException e) {
                                // No FFDC code needed
                                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                                    SibTr.debug(TRACE, "Clone connection failed");
                                JmsJcaManagedConnection mc = ((JmsJcaSessionImpl) object).getManagedConnection();
                                mc.connectionErrorOccurred(e, true); // Inform JCA of connection failure on this Managed Connection
                                clonedException = e;
                            } catch (SIErrorException e) {
                                // No FFDC code needed
                                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                                    SibTr.debug(TRACE, "Clone connection failed");
                                JmsJcaManagedConnection mc = ((JmsJcaSessionImpl) object).getManagedConnection();
                                mc.connectionErrorOccurred(e, true); // Inform JCA of connection failure on this Managed Connection
                                clonedException = e;
                            }
                        }

                        if (clonedCoreConnection != null) {
                            connection = new JmsJcaConnectionImpl(this, clonedCoreConnection, (JmsJcaSessionImpl) object, requestInfo);
                        }

                        tryAgain = (clonedCoreConnection == null);

                        if (tryAgain)
                        {
                            SibTr.info(TRACE, NLS.getFormattedMessage(("CONNECTION_ERROR_RETRY_CWSJR1067"),
                                                                      new Object[] { clonedException }, null));

                            // We need to try again so we clone and change the cri (incremenet counter) which 
                            // forces j2c to create a new managed connection.  Cloning is needed to prevent
                            // a broken connection in the shared pool being returned because it has a
                            // cri == this cri (PM31826)
                            requestInfo = (JmsJcaConnectionRequestInfo) requestInfo.clone();
                            requestInfo.incrementRequestCounter();
                        }

                    } else {

                        throw new ResourceAdapterInternalException(
                                        NLS
                                                        .getFormattedMessage(
                                                                             ("EXCEPTION_RECEIVED_CWSJR1061"),
                                                                             new Object[] {
                                                                                           "createConnection",
                                                                                           JmsJcaSessionImpl.class
                                                                                                           .getName(),
                                                                                           object.getClass().getName() },
                                                                             null));

                    }

                } while (tryAgain);

            } else {

                // We are not managed.

                final SICoreConnection coreConnection = createCoreConnection();

                // Now we have enough info to create a connection we should go
                // ahead and create one.
                connection = new JmsJcaConnectionImpl(this, coreConnection, requestInfo);

            }

        } catch (final ResourceException exception)
        {
            FFDCFilter.processException(exception, CLASS_NAME
                                                   + ".createConnection", FFDC_PROBE_1, this);

            Throwable cause = exception.getCause();

            // This section will look at the cause of an exception thrown while creating a connection. If the cause
            // is a security based exeption (SIAuthenticationException or SINotAuthorizedException) then a new
            // exception is created to supply the user with more information.

            // pnickoll: We would like to pass the "cause" as a linked exception to the following exceptions
            if (runningInWAS)
            {
                if (cause instanceof SIAuthenticationException)
                {
                    SIAuthenticationException authentEx = null;
                    if (containerAuth)
                    {
                        if (containerAlias != null && !containerAlias.isEmpty()) {
                            authentEx = new SIAuthenticationException(NLS.getFormattedMessage("CONTAINER_AUTHENTICATION_EXCEPTION_1068",
                                                                                              new Object[] { containerAlias, }, null));
                        }
                        else {

                            authentEx = new SIAuthenticationException(NLS.getString("AUTHENTICATION_EXCEPTION_1077"));
                        }
                    }
                    else
                    {
                        authentEx = new SIAuthenticationException(NLS.getString("AUTHENTICATION_EXCEPTION_1077"));
                    }

                    throw authentEx;
                }
                else if (cause instanceof SINotAuthorizedException)
                {
                    if (containerAuth)
                    {
                        if (containerAlias == null)
                        {
                            throw new SINotAuthorizedException(NLS.getString("CONTAINER_AUTHORIZATION_EXCEPTION_1070"));
                        }
                    }
                    else
                    {
                        String userName = getUserName();
                        if ((userName == null) || ("".equals(userName)))
                        {
                            throw new SINotAuthorizedException(NLS.getString("CONTAINER_AUTHORIZATION_EXCEPTION_1071"));
                        }
                    }
                }
            }

            if (cause instanceof SIException)
            {
                throw (SIException) cause;
            }
            else if (cause instanceof SIErrorException)
            {
                throw (SIErrorException) cause;
            }

            throw exception;

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, "createConnection", connection);
        }

        return connection;
    }

    /**
     * Creates a new <code>JmsJcaConnection</code>. An
     * <code>JmsJcaConnectionRequestInfo</code> object containing the given
     * user name and password is passed to the <code>allocateConnection</code>
     * method of the associated connection manager. This returns a new
     * <code>JmsJcaSession</code> from which the associated
     * <code>JmsJcaConnnection</code> can be obtained. This will contain a
     * core connection authenticated with the credentials from the container or,
     * if none are passed, those from the application. The session is held by
     * the connection and returned on the first call to
     * <code>createSession</code>.
     * 
     * @param userName
     *            the application provided user name
     * @param password
     *            the application provided password
     * @return the connection
     * @throws javax.resource.ResourceException
     *             if the JCA runtime fails to allocate a connection
     * @throws SIException
     *             if the creation of the core connection fails
     * @throws SIErrorException
     *             if the creation of the core connection fails
     */
    @Override
    public JmsJcaConnection createConnection(final String userName,
                                             final String password) throws ResourceException, SIException,
                    SIErrorException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createConnection", new Object[] {
                                                                       userName, "*****" });
        }

        JmsJcaConnection connection = null;

        JmsJcaConnectionRequestInfo requestInfo = new JmsJcaConnectionRequestInfo(userName, password);

        try {

            if (_managed) {

                /*
                 * In a managed environment obtain a managed connection
                 */

                Object object = null;
                boolean tryAgain = true;

                do
                {

                    // Try and allocate a connection.
                    object = _connectionManager.allocateConnection(
                                                                   _managedConnectionFactory, requestInfo);

                    // Ensure that we have a session of the right type.
                    if (object instanceof JmsJcaSessionImpl) {

                        // Get the core connection from the managed connection
                        SICoreConnection coreConnection = ((JmsJcaSessionImpl) object).getManagedConnection().getCoreConnection();

                        // PM21176 (PM07867) It is very important that the connection request info structure
                        // has a reference to a core connection, that is connected to the same
                        // ME as our saved core connection. If a new connection was created as part of
                        // allocateConnection then this will already be the case.
                        // However, if an existing connection was re-used from the pool, no
                        // connection will have been set on the connection request info, and
                        // JCA sessions created from this JCA connection might end up connecting
                        // to different messaging engines, preventing transaction completion.
                        if (requestInfo.getSICoreConnection() == null) {
                            requestInfo.setSICoreConnection(coreConnection);
                        }

                        SICoreConnection clonedCoreConnection = null;

                        // Clone the managed connection owned core connection for use by the connection
                        // object which we will return to JCA. Previous connection failures will only
                        // become apparent here so we catch any cloning exception and handle it as a
                        // connection failure on the managed connection.

                        Exception clonedException = null;
                        if (coreConnection != null) {
                            try {
                                clonedCoreConnection = coreConnection.cloneConnection();
                            } catch (SIException e) {
                                // No FFDC code needed
                                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                                    SibTr.debug(TRACE, "Clone connection failed");
                                JmsJcaManagedConnection mc = ((JmsJcaSessionImpl) object).getManagedConnection();
                                mc.connectionErrorOccurred(e, true); // Inform JCA of connection failure on this Managed Connection
                                clonedException = e;
                            } catch (SIErrorException e) {
                                // No FFDC code needed
                                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                                    SibTr.debug(TRACE, "Clone connection failed");
                                JmsJcaManagedConnection mc = ((JmsJcaSessionImpl) object).getManagedConnection();
                                mc.connectionErrorOccurred(e, true); // Inform JCA of connection failure on this Managed Connection
                                clonedException = e;
                            }
                        }

                        if (clonedCoreConnection != null) {
                            connection = new JmsJcaConnectionImpl(this, clonedCoreConnection, (JmsJcaSessionImpl) object, requestInfo);
                        }

                        tryAgain = (clonedCoreConnection == null);

                        if (tryAgain)
                        {
                            SibTr.info(TRACE, NLS.getFormattedMessage(("CONNECTION_ERROR_RETRY_CWSJR1067"),
                                                                      new Object[] { clonedException }, null));

                            // We need to try again so we clone and change the cri (incremenet counter) which 
                            // forces j2c to create a new managed connection.  Cloning is needed to prevent
                            // a broken connection in the shared pool being returned because it has a
                            // cri == this cri (PM31826)
                            requestInfo = (JmsJcaConnectionRequestInfo) requestInfo.clone();
                            requestInfo.incrementRequestCounter();
                        }

                    } else {

                        throw new ResourceAdapterInternalException(
                                        NLS
                                                        .getFormattedMessage(
                                                                             ("EXCEPTION_RECEIVED_CWSJR1061"),
                                                                             new Object[] {
                                                                                           "createConnection",
                                                                                           JmsJcaSessionImpl.class
                                                                                                           .getName(),
                                                                                           object.getClass().getName() },
                                                                             null));

                    }

                } while (tryAgain);

            } else {

                /*
                 * Non-managed
                 */

                final SICoreConnection coreConnection = createCoreConnection(
                                                                             userName, password);

                // Now we have enough info to create a connection we should go
                // ahead and create one.
                connection = new JmsJcaConnectionImpl(this, coreConnection, requestInfo);

            }

        } catch (final ResourceException exception) {

            FFDCFilter.processException(exception, CLASS_NAME
                                                   + "createConnection", FFDC_PROBE_3, this);
            Throwable cause = exception.getCause();

            if (cause instanceof SIAuthenticationException)
            {
                throw new SIAuthenticationException(NLS.getFormattedMessage("APPLICATION_AUTHENTICATION_EXCEPTION_1072",
                                                                            new Object[] { userName }, null));
            }
            else if (cause instanceof SIException)
            {
                throw (SIException) cause;
            }
            else if (cause instanceof SIErrorException)
            {
                throw (SIErrorException) cause;
            }

            throw exception;
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createConnection", connection);
        }
        return connection;

    }

    /**
     * Create a core connection
     * 
     * @return the core connection
     * @throws ResourceException
     *             if the creation fails Feature 169626.14
     */
    SICoreConnection createCoreConnection() throws ResourceException {

        return createCoreConnection(_managedConnectionFactory.getUserName(),
                                    _managedConnectionFactory.getPassword());

    }

    /**
     * Creates a core connection
     * 
     * @param userName
     *            the username
     * @param password
     *            the password
     * @return the core connection
     * @throws ResourceException
     *             if the creation fails Feature 169626.14
     */
    SICoreConnection createCoreConnection(final String userName,
                                          final String password) throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createCoreConnection", new Object[] {
                                                                           userName, "*****" });
        }

        SICoreConnectionFactory coreConnectionFactory = null;

        if (!UTEHelperFactory.jmsTestEnvironmentEnabled) {

            try {

                coreConnectionFactory = SICoreConnectionFactorySelector
                                .getSICoreConnectionFactory(FactoryType.TRM_CONNECTION);

            } catch (final SIException exception) {

                FFDCFilter.processException(exception, CLASS_NAME
                                                       + ".createManagedConnection", FFDC_PROBE_5, this);
                throw new ResourceException(NLS
                                .getFormattedMessage("EXCEPTION_RECEIVED_CWSJR1064",
                                                     new Object[] { exception,
                                                                   "getSICoreConnectionFactory" }, null),
                                exception);

            }

        } else {

            coreConnectionFactory = UTEHelperFactory.getHelperInstance()
                            .setupJmsTestEnvironment();

        }

        if (coreConnectionFactory == null) {
            throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                                                                               ("SICORECONNECTION_ERROR_CWSJR1066"),
                                                                               new Object[] { "createManagedConnection" }, null));
        }

        final SICoreConnection coreConnection;

        try {

            final Map trmProperties = _managedConnectionFactory
                            .getTrmProperties();
            coreConnection = coreConnectionFactory.createConnection(userName,
                                                                    password, trmProperties);

        } catch (final SIException exception) {

            FFDCFilter
                            .processException(
                                              exception,
                                              CLASS_NAME
                                                              + ".createManagedConnection (Subject, ConnectionRequestInfo)",
                                              FFDC_PROBE_6, this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

            throw new ResourceException(NLS.getFormattedMessage(
                                                                "EXCEPTION_RECEIVED_CWSJR1065", new Object[] { exception,
                                                                                                              "createManagedConnection" }, null), exception);

        } catch (final SIErrorException exception) {

            FFDCFilter
                            .processException(
                                              exception,
                                              CLASS_NAME
                                                              + ".createManagedConnection (Subject, ConnectionRequestInfo)",
                                              FFDC_PROBE_7, this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

            throw new ResourceException(NLS.getFormattedMessage(
                                                                "EXCEPTION_RECEIVED_CWSJR1065", new Object[] { exception,
                                                                                                              "createManagedConnection" }, null), exception);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createCoreConnection", coreConnection);
        }
        return coreConnection;

    }

    /**
     * Returns the JMS client ID set on the associated managed connection
     * factory or null if none was set.
     * 
     * Used in Outbound Diagram 9
     * 
     * @return the client ID
     */
    @Override
    public String getClientID() {
        return _managedConnectionFactory.getClientID();
    }

    /**
     * Returns the JMS NonPersistentMapping set on the associated managed
     * connection factory or null if none was set.
     * 
     * Used in Outbound Diagram 9
     * 
     * @return the NonPersistentMapping
     */
    @Override
    public String getNonPersistentMapping() {
        return _managedConnectionFactory.getNonPersistentMapping();
    }

    /**
     * Returns the JMS PersistentMapping set on the associated managed
     * connection factory or null if none was set.
     * 
     * Used in Outbound Diagram 9
     * 
     * @return the PersistentMapping
     */
    @Override
    public String getPersistentMapping() {
        return _managedConnectionFactory.getPersistentMapping();
    }

    /**
     * Returns the DurableSubscriptionHome property
     * 
     * Used in Outbound Diagram 9
     * 
     * @return the DurableSubscriptionHome
     */
    @Override
    public String getDurableSubscriptionHome() {
        return _managedConnectionFactory.getDurableSubscriptionHome();
    }

    /**
     * Returns ReadAhead property
     * 
     * Used in Outbound Diagram 9
     * 
     * @return ReadAhead
     */
    @Override
    public String getReadAhead() {
        return _managedConnectionFactory.getReadAhead();
    }

    /**
     * Gets the prefix for temporary queue names.
     * 
     * @return the prefix
     */
    @Override
    public String getTemporaryQueueNamePrefix() {
        return _managedConnectionFactory.getTemporaryQueueNamePrefix();
    }

    /**
     * Gets the prefix for temporary topic names.
     * 
     * @return the prefix
     */
    @Override
    public String getTemporaryTopicNamePrefix() {
        return _managedConnectionFactory.getTemporaryTopicNamePrefix();
    }

    /**
     * Gets the busName
     * 
     * @return the busName
     */
    @Override
    public String getBusName() {
        return _managedConnectionFactory.getBusName();
    }

    /**
     * Gets the userName
     * 
     * @return the userName
     */
    @Override
    public String getUserName() {
        return _managedConnectionFactory.getUserName();
    }

    /**
     * Gets the password
     * 
     * @return the password
     */
    @Override
    public String getPassword() {
        return _managedConnectionFactory.getPassword();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaConnectionFactory#getTarget()
     */
    @Override
    public String getTarget() {
        return _managedConnectionFactory.getTarget();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaConnectionFactory#getTargetSignificance()
     */
    @Override
    public String getTargetSignificance() {
        return _managedConnectionFactory.getTargetSignificance();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaConnectionFactory#getTargetType()
     */

    @Override
    public String getTargetType() {
        return _managedConnectionFactory.getTargetType();
    }

    /**
     * Gets the target inbound transport chain.
     * 
     * @return the target transport chain
     */
    @Override
    public String getTargetTransportChain() {
        return _managedConnectionFactory.getTargetTransportChain();
    }

    /**
     * Gets the providerEndpoints
     * 
     * @return the providerEndpoints
     */
    @Override
    public String getProviderEndpoints() {
        return _managedConnectionFactory.getRemoteServerAddress();
    }

    /**
     * Gets the connectionProximity
     * 
     * @return the connectionProximity
     */
    @Override
    public String getConnectionProximity() {
        return _managedConnectionFactory.getConnectionProximity();
    }

    /**
     * Returns the shareDurableSubscription details
     * 
     * @return the shareDurableSubscription property
     */
    @Override
    public String getShareDurableSubscriptions() {
        return _managedConnectionFactory.getShareDurableSubscriptions();
    }

    /**
     * Returns the subscription protocol
     * 
     * @return the subscription protocol property
     */
    @Override
    public String getSubscriptionProtocol() {
        return _managedConnectionFactory.getSubscriptionProtocol();
    }

    /**
     * Returns the multicast interface
     * 
     * @return the multicast interface property
     */
    @Override
    public String getMulticastInterface() {
        return _managedConnectionFactory.getMulticastInterface();
    }

    /**
     * Returns the property indicating if the producer will modify the payload after setting it.
     * 
     * @return String containing the property value.
     */
    @Override
    public String getProducerDoesNotModifyPayloadAfterSet() {
        return _managedConnectionFactory.getProducerDoesNotModifyPayloadAfterSet();
    }

    /**
     * Returns the property indicating if the consumer will modify the payload after getting it.
     * 
     * @return String containing the property value.
     */
    @Override
    public String getConsumerDoesNotModifyPayloadAfterGet() {
        return _managedConnectionFactory.getConsumerDoesNotModifyPayloadAfterGet();
    }

    /**
     * Returns true if this connection factory was created in a managed
     * environment. This is determined by whether or not a connection manager
     * was passed on its creation. This can be used by the JMS API to determine
     * whether or not to throw exceptions on methods that are not permitted in a
     * managed environment i.e. within the EJB or web container.
     * 
     * @return true iff this connection factory is managed
     */
    @Override
    public boolean isManaged() {
        return _managed;
    }

    /**
     * Returns the connectionManager.
     * 
     * @return ConnectionManager
     */
    ConnectionManager getConnectionManager() {
        return _connectionManager;
    }

    /**
     * Returns the managedConnectionFactory.
     * 
     * @return JmsJcaManagedConnectionFactoryImpl
     */
    JmsJcaManagedConnectionFactoryImpl getManagedConnectionFactory() {
        return _managedConnectionFactory;
    }

    /**
     * Compares this object with the supplied object
     * 
     * @param other -
     *            The object to compare
     * @return True if the objects match and false otherwise
     */
    @Override
    public boolean equals(Object other) {

        if (!(other instanceof JmsJcaConnectionFactoryImpl)) {
            return false;
        }
        if (other == this) {
            return true;
        }

        final boolean equal;
        if (_managedConnectionFactory != null) {

            equal = _managedConnectionFactory
                            .equals(((JmsJcaConnectionFactoryImpl) other)
                                            .getManagedConnectionFactory());

        } else {

            equal = ((JmsJcaConnectionFactoryImpl) other)
                            .getManagedConnectionFactory() == null;

        }
        return equal;

    }

    @Override
    public int hashCode() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "hashCode");
        }
        int hash = 31;
        hash = 17 * hash + (_managed ? 0 : 1);
        hash = 17
               * hash
               + (_managedConnectionFactory == null ? 0
                               : _managedConnectionFactory.hashCode());
        hash = 17
               * hash
               + (_connectionManager == null ? 0 : _connectionManager
                               .hashCode());
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "hashCode", new Integer(hash));
        }

        return hash;
    }

    /*
     * @see javax.naming.Referenceable#getReference()
     */
    @Override
    public Reference getReference() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
            SibTr.entry(this, TRACE, "getReference");

        final Reference reference = _managedConnectionFactory.getReference();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
            SibTr.exit(this, TRACE, "getReference", reference);

        return reference;
    }

    /*
     * @see javax.resource.Referenceable#setReference(Reference)
     */
    @Override
    public void setReference(Reference reference) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
            SibTr.entry(this, TRACE, "setReference", reference);

        _managedConnectionFactory.setReference(reference);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
            SibTr.exit(this, TRACE, "setReference");
    }

    /**
     * Returns a string representation of this object
     * 
     * @return String The string describing this object
     */
    @Override
    public String toString() {

        final StringBuffer sb = new StringBuffer("[");
        sb.append(this.getClass().getName());
        sb.append("@");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" <managedConnectionFactory=");
        sb.append(_managedConnectionFactory);
        sb.append("> <connectionManager=");
        sb.append(_connectionManager);
        sb.append("> <managed=");
        sb.append(_managed);
        sb.append(">]");
        return sb.toString();

    }

}
