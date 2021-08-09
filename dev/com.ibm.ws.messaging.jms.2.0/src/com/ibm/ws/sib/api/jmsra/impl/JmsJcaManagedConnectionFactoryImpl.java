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
package com.ibm.ws.sib.api.jmsra.impl;

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsConnectionFactory;
import com.ibm.websphere.sib.api.jms.JmsFactoryFactory;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.sib.api.jms.JmsRAFactoryFactory;
import com.ibm.ws.sib.api.jms.ute.UTEHelperFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaManagedConnectionFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaReferenceUtils;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.utils.PasswordSuppressingHashMap;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;
import com.ibm.wsspi.sib.core.SICoreConnectionFactorySelector;
import com.ibm.wsspi.sib.core.selector.FactoryType;
import com.ibm.wsspi.sib.core.trm.SibTrmConstants;

/**
 * Implementation of managed connection factory class for JMS resource adapter.
 */
public class JmsJcaManagedConnectionFactoryImpl implements JmsJcaManagedConnectionFactory, FFDCSelfIntrospectable, ResourceAdapterAssociation {

    /**
     * The log writer passed to this managed connection factory.
     */
    private transient PrintWriter _logWriter;

    /**
     * The factory for JMS API connection factories.
     */
    private transient JmsRAFactoryFactory _jmsFactoryFactory;

    /**
     * The map of properties for this managed connection factory.
     */
    private Map _properties = new PasswordSuppressingHashMap();

    static final String CONN_FACTORY_TYPE = "javax.jms.ConnectionFactory";

    private static final String FFDC_PROBE_1 = "1";

    private static final String FFDC_PROBE_2 = "2";

    private static final String FFDC_PROBE_3 = "3";

    private static final String FFDC_PROBE_4 = "4";

    private static final String FFDC_PROBE_5 = "5";

    private static final String FFDC_PROBE_6 = "6";

    private static final String FFDC_PROBE_7 = "7";

    private static final String FFDC_PROBE_8 = "8";

    private static final String CLASS_NAME = JmsJcaManagedConnectionFactoryImpl.class
                    .getName();

    private static final long serialVersionUID = 8124956584686200082L;

    /**
     * Map of property name to their default values. If the relevant property
     * has this value it is to be omitted from JNDI references (and, correspondingly, if
     * the JNDI reference does not have this property, it is to be assumed to have this
     * value
     */
    private static Map<String, Object> defaultJNDIProperties = new HashMap<String, Object>();

    String temporaryQueueNamePrefix;

    private ResourceAdapter _resourceAdapter;

    private static TraceComponent TRACE = SibTr.register(
                                                         JmsJcaManagedConnectionFactoryImpl.class, JmsraConstants.MSG_GROUP,
                                                         JmsraConstants.MSG_BUNDLE);

    private static TraceNLS NLS = TraceNLS
                    .getTraceNLS(JmsraConstants.MSG_BUNDLE);

    static {
        // Initialise the default JNDI properties map
        defaultJNDIProperties.put(JmsraConstants.PRODUCER_DOES_NOT_MODIFY_PAYLOAD_AFTER_SET, ApiJmsConstants.MIGHT_MODIFY_PAYLOAD);
        defaultJNDIProperties.put(JmsraConstants.CONSUMER_DOES_NOT_MODIFY_PAYLOAD_AFTER_GET, ApiJmsConstants.MIGHT_MODIFY_PAYLOAD);
    }

    /**
     * Default constructor.
     */
    public JmsJcaManagedConnectionFactoryImpl() {

        // Initialise properties Map and set the default values
        setBusName(JmsraConstants.DEFAULT_BUS_NAME);
        setClientID(JmsraConstants.DEFAULT_CLIENT_ID);
        setNonPersistentMapping(ApiJmsConstants.MAPPING_EXPRESS_NONPERSISTENT);
        setPersistentMapping(ApiJmsConstants.MAPPING_RELIABLE_PERSISTENT);
        setPassword(JmsraConstants.DEFAULT_PASSWORD);
        setUserName(JmsraConstants.DEFAULT_USER_NAME);

        // 176645.2
        setDurableSubscriptionHome(JmsraConstants.DEFAULT_DURABLE_SUB_HOME);
        setReadAhead(ApiJmsConstants.READ_AHEAD_DEFAULT);

        // 188482.2
        _properties.put(JmsraConstants.TEMP_QUEUE_NAME_PREFIX,
                        JmsraConstants.DEFAULT_TEMP_QUEUE_NAME_PREFIX);
        _properties.put(JmsraConstants.TEMP_TOPIC_NAME_PREFIX,
                        JmsraConstants.DEFAULT_TEMP_TOPIC_NAME_PREFIX);

        // 181802.1.1 206397.5.1
        setTarget(JmsraConstants.DEFAULT_TARGET);
        setTargetType(JmsraConstants.DEFAULT_TARGET_TYPE);
        setTargetSignificance(JmsraConstants.DEFAULT_TARGET_SIGNIFICANCE);
        setTargetTransportChain(JmsraConstants.DEFAULT_TARGET_TRANSPORT_CHAIN);
        setRemoteServerAddress(JmsraConstants.DEFAULT_PROVIDER_ENDPOINTS);
        setTargetTransport(JmsraConstants.DEFAULT_TARGET_TRANSPORT);
        setConnectionProximity(JmsraConstants.DEFAULT_CONNECTION_PROXIMITY);

        // 192474.1
        setShareDurableSubscriptions(ApiJmsConstants.SHARED_DSUBS_NEVER);

        // 188050.2
        setShareDataSourceWithCMP(JmsraConstants.DEFAULT_SHARE_DATA_SOURCE_WITH_CMP);

        // 247845.4
        setSubscriptionProtocol(SibTrmConstants.SUBSCRIPTION_PROTOCOL_DEFAULT);
        setMulticastInterface(SibTrmConstants.MULTICAST_INTERFACE_DEFAULT);

        // SIB0121.jms.1: set default values for performance enhancing properties
        setProducerDoesNotModifyPayloadAfterSet(ApiJmsConstants.MIGHT_MODIFY_PAYLOAD);
        setConsumerDoesNotModifyPayloadAfterGet(ApiJmsConstants.MIGHT_MODIFY_PAYLOAD);
    }

    /**
     * Creates a JMS connection factory. This connection factory is given a
     * <code>JmsJcaConnectionFactory</code> which, in turn, is associated with
     * this managed connection factory and uses the given connection manager.
     * 
     * @param connectionManager
     *            the connection manager
     * @return the JMS connection factory
     * @throws javax.resource.ResourceException
     *             generic exception
     */
    @Override
    final public Object createConnectionFactory(
                                                final ConnectionManager connectionManager) throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createConnectionFactory",
                        connectionManager);
        }

        final JmsJcaConnectionFactoryImpl connectionFactory = new JmsJcaConnectionFactoryImpl(
                        this, connectionManager);
        final JmsRAFactoryFactory jmsRAFactory = getJmsRAFactoryFactory();
        final ConnectionFactory jmsConnectionFactory = createJmsConnFactory(
                                                                            jmsRAFactory, connectionFactory);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createConnectionFactory",
                       jmsConnectionFactory);
        }
        return jmsConnectionFactory;

    }

    /**
     * Creates a JMS connection factory. This connection factory is given a
     * <code>JmsJcaConnectionFactory</code> which, in turn, is associated with
     * this managed connection factory and uses a default connection manager.
     * 
     * @return the JMS connection factory
     * @throws ResourceException
     *             generic exception
     */
    @Override
    final public Object createConnectionFactory() throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createConnectionFactory");
        }

        final JmsJcaConnectionFactoryImpl connectionFactory = new JmsJcaConnectionFactoryImpl(
                        this);
        final JmsRAFactoryFactory jmsRAFactory = getJmsRAFactoryFactory();
        final ConnectionFactory jmsConnectionFactory = createJmsConnFactory(
                                                                            jmsRAFactory, connectionFactory, this);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createConnectionFactory",
                       jmsConnectionFactory);
        }
        return jmsConnectionFactory;

    }

    /**
     * Creates a managed connection.
     * 
     * @param subject
     *            the subject
     * @param requestInfo
     *            the request information
     * @return the managed connection
     * @throws ResourceException
     *             generic exception
     */
    @Override
    final public ManagedConnection createManagedConnection(
                                                           final Subject subject, final ConnectionRequestInfo requestInfo)
                    throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createManagedConnection", new Object[] {
                                                                              JmsJcaManagedConnection.subjectToString(subject),
                                                                              requestInfo });
        }

        // If we have some request information then see if it already has a core
        // connection associated with it

        JmsJcaConnectionRequestInfo jmsJcaRequestInfo = null;
        SICoreConnection requestCoreConnection = null;
        SICoreConnection coreConnection = null;

        if (requestInfo != null) {

            // Check we have the right type of request information

            if (!(requestInfo instanceof JmsJcaConnectionRequestInfo)) {

                throw new ResourceAdapterInternalException(NLS
                                .getFormattedMessage(("EXCEPTION_RECEIVED_CWSJR1022"),
                                                     new Object[] {
                                                                   "createManagedConnection",
                                                                   JmsJcaConnectionRequestInfo.class
                                                                                   .getName(),
                                                                   requestInfo.getClass().getName() },
                                                     null));

            }

            jmsJcaRequestInfo = (JmsJcaConnectionRequestInfo) requestInfo;
            requestCoreConnection = jmsJcaRequestInfo.getSICoreConnection();

            if (requestCoreConnection != null) {
                // Check that the core connection is still available, ie hasn't been closed. If the
                // core connection is no longer available, null it out and continue without it.

                try {
                    // There is no isAvailable() method on a core connection we use getConnectionListeners()
                    // which is believed to be the cheapest alternative method.
                    requestCoreConnection.getConnectionListeners();

                } catch (final SIException e) {
                    // No FFDC code needed
                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                        SibTr.debug(TRACE, "Connection Request Info core connection no longer available");
                    jmsJcaRequestInfo.setSICoreConnection(null);
                    requestCoreConnection = null;
                } catch (final SIErrorException e) {
                    // No FFDC code needed
                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                        SibTr.debug(TRACE, "Connection Request Info core connection no longer available");
                    jmsJcaRequestInfo.setSICoreConnection(null);
                    requestCoreConnection = null;
                }
            }
        }

        // See if we can get a user name and password from the subject or
        // request information

        final JmsJcaUserDetails userDetails = getUserDetails(subject,
                                                             requestInfo);

        // If we didn't get any request information or we did but it didn't
        // contain a core connection, create a new one

        if (requestCoreConnection == null) {

            // Obtain a core connection factory

            final SICoreConnectionFactory coreConnectionFactory;

            if (!UTEHelperFactory.jmsTestEnvironmentEnabled) {

                try {

                    coreConnectionFactory = SICoreConnectionFactorySelector
                                    .getSICoreConnectionFactory(FactoryType.TRM_CONNECTION);

                } catch (final SIException exception) {

                    FFDCFilter.processException(exception, CLASS_NAME
                                                           + ".createManagedConnection", FFDC_PROBE_5, this);
                    throw new ResourceException(NLS.getFormattedMessage(
                                                                        "EXCEPTION_RECEIVED_CWSJR1021", new Object[] {
                                                                                                                      exception, "getSICoreConnectionFactory" },
                                                                        null), exception);

                }

            } else {

                coreConnectionFactory = UTEHelperFactory.getHelperInstance()
                                .setupJmsTestEnvironment();

            }

            // Check we have obtained a core connection factory successfully

            if (coreConnectionFactory == null) {

                throw new ResourceAdapterInternalException(NLS
                                .getFormattedMessage(
                                                     ("SICORECONNECTION_ERROR_CWSJR1023"),
                                                     new Object[] { "createManagedConnection" },
                                                     null));

            }

            // Create a connection

            try {

                final Map trmProperties = getTrmProperties();

                if (userDetails == null) {

                    requestCoreConnection = coreConnectionFactory
                                    .createConnection(subject, trmProperties);

                } else {

                    requestCoreConnection = coreConnectionFactory
                                    .createConnection(userDetails.getUserName(),
                                                      userDetails.getPassword(), trmProperties);

                }

            } catch (final SIException exception) {

                FFDCFilter.processException(exception, CLASS_NAME
                                                       + ".createManagedConnection", FFDC_PROBE_1, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "EXCEPTION_RECEIVED_CWSJR1028", new Object[] {
                                                                                                                  exception, "createManagedConnection" }, null),
                                exception);

            } catch (final SIErrorException exception) {

                FFDCFilter.processException(exception, CLASS_NAME
                                                       + ".createManagedConnection", FFDC_PROBE_2, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "EXCEPTION_RECEIVED_CWSJR1028", new Object[] {
                                                                                                                  exception, "createManagedConnection" }, null),
                                exception);

            }

            // If we have some request information, set the core connection back
            // into it

            if (jmsJcaRequestInfo != null) {

                jmsJcaRequestInfo.setSICoreConnection(requestCoreConnection);

            }

            // This is a new coreConnection so set this parent coreConnection in 
            // the managedConnection by assigning it to the coreConnection here.
            // The managedconnection will now have full control over the parent
            // coreconnection.
            coreConnection = requestCoreConnection;
        }
        else
        {

            try {
                // We are here as we already have a valid coreconnection in our requestInfo, this
                // means we already have a managedConnection that is looking at the parent, so we
                // need to create a clone of that coreConnection to pass into our new managedConnection.

                // Create a clone to give to the managed connection object which will be stored in the 
                // J2C connection pool.
                coreConnection = requestCoreConnection.cloneConnection();
            } catch (final SIException e) {
                FFDCFilter.processException(e, CLASS_NAME + ".createManagedConnection", FFDC_PROBE_7, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled())
                    SibTr.exception(this, TRACE, e);
                throw new ResourceException(NLS.getFormattedMessage("EXCEPTION_RECEIVED_CWSJR1028",
                                                                    new Object[] { e, "createManagedConnection" },
                                                                    null), e);
            } catch (final SIErrorException e) {
                FFDCFilter.processException(e, CLASS_NAME + ".createManagedConnection", FFDC_PROBE_8, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled())
                    SibTr.exception(this, TRACE, e);
                throw new ResourceException(NLS.getFormattedMessage("EXCEPTION_RECEIVED_CWSJR1028",
                                                                    new Object[] { e, "createManagedConnection" },
                                                                    null), e);
            }
        }

        final JmsJcaManagedConnection managedConnection;

        try {

            // Create a managed connection

            if ((getShareDataSourceWithCMP() != null)
                && (getShareDataSourceWithCMP().booleanValue())) {

                // ShareDataSourceWithCMP is set - return a managed connection
                // implementing SynchronizationProvider

                managedConnection = new JmsJcaManagedConnectionSynchronizationProvider(
                                this, coreConnection, userDetails, subject);

            } else {

                // ShareDataSourceWithCMP is not set - return a normal managed
                // connection

                managedConnection = new JmsJcaManagedConnection(this,
                                coreConnection, userDetails, subject);

            }

        } catch (final SIException exception) {

            FFDCFilter.processException(exception, CLASS_NAME
                                                   + ".createManagedConnection", FFDC_PROBE_3, this);
            throw new ResourceException(NLS.getFormattedMessage(
                                                                "EXCEPTION_RECEIVED_CWSJR1026", new Object[] { exception,
                                                                                                              "createManagedConnection" }, null), exception);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createManagedConnection",
                       managedConnection);
        }
        return managedConnection;

    }

    /**
     * Returns a matching connection from the candidate set of connections.
     * 
     * @param connectionSet
     *            the candidate set of connections
     * @param subject
     *            the subject
     * @param requestInfo
     *            the request information
     * @return the matching connection, if any
     */
    @Override
    final public ManagedConnection matchManagedConnections(
                                                           final Set connectionSet, final Subject subject,
                                                           final ConnectionRequestInfo requestInfo) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, "matchManagedConnections", new Object[] {
                                                                        connectionSet,
                                                                        JmsJcaManagedConnection.subjectToString(subject),
                                                                        requestInfo });
        }

        final SICoreConnection coreConnection = (requestInfo instanceof JmsJcaConnectionRequestInfo) ? ((JmsJcaConnectionRequestInfo) requestInfo)
                        .getSICoreConnection()
                        : null;

        final JmsJcaUserDetails userDetails = getUserDetails(subject,
                                                             requestInfo);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {

            if (userDetails != null) {
                SibTr.debug(TRACE, "Got a username and password");
            } else {
                SibTr.debug(TRACE, "Using subject");
            }

        }

        JmsJcaManagedConnection matchedConnection = null;

        // Go through the set of managed connections and try and match one

        for (final Iterator iterator = connectionSet.iterator(); iterator
                        .hasNext();) {

            final Object object = iterator.next();

            // Skip over any non JmsJcaManagedConnections
            if (object instanceof JmsJcaManagedConnection) {

                final JmsJcaManagedConnection managedConnection = (JmsJcaManagedConnection) object;

                // If we have a user name and password from either the
                // requestInfo or subject then we must try and match againgst
                // those
                if (userDetails != null) {

                    if (managedConnection.match(userDetails, coreConnection)) {

                        matchedConnection = managedConnection;
                        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                            SibTr
                                            .debug(TRACE,
                                                   "Matched a connection against the subject username and password");
                        }
                        break;

                    }

                } else {

                    // This is a subject where we couldnt get the userName and
                    // password from..

                    if (managedConnection.match(subject, coreConnection)) {

                        matchedConnection = managedConnection;
                        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                            SibTr.debug(TRACE,
                                        "Matched a connection against the subject");
                        }
                        break;

                    }

                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, "matchManagedConnections", matchedConnection);
        }
        return matchedConnection;

    }

    /**
     * Sets the log writer.
     * 
     * @param logWriter
     *            the log writer
     */
    @Override
    final public void setLogWriter(PrintWriter logWriter) {
        _logWriter = logWriter;
    }

    /**
     * Returns the log writer.
     * 
     * @return the log writer
     */
    @Override
    final public PrintWriter getLogWriter() {
        return _logWriter;
    }

    /**
     * Returns the hashCode for this managed connection factory.
     * 
     * @return the hash code
     */
    @Override
    final public int hashCode() {

        int hash = 11;
        hash = 23 * hash + (getBusName() == null ? 0 : getBusName().hashCode());
        hash = 23 * hash
               + (getClientID() == null ? 0 : getClientID().hashCode());
        hash = 23 * hash
               + (getUserName() == null ? 0 : getUserName().hashCode());
        hash = 23 * hash
               + (getPassword() == null ? 0 : getPassword().hashCode());
        hash = 23
               * hash
               + (getNonPersistentMapping() == null ? 0
                               : getNonPersistentMapping().hashCode());
        hash = 23
               * hash
               + (getPersistentMapping() == null ? 0 : getPersistentMapping()
                               .hashCode());
        hash = 23
               * hash
               + (getDurableSubscriptionHome() == null ? 0
                               : getDurableSubscriptionHome().hashCode());
        hash = 23 * hash
               + (getReadAhead() == null ? 0 : getReadAhead().hashCode());
        hash = 23
               * hash
               + (getTemporaryQueueNamePrefix() == null ? 0
                               : getTemporaryQueueNamePrefix().hashCode());
        hash = 23
               * hash
               + (getTemporaryTopicNamePrefix() == null ? 0
                               : getTemporaryTopicNamePrefix().hashCode());
        hash = 23 * hash + (getTarget() == null ? 0 : getTarget().hashCode());
        hash = 23 * hash
               + (getTargetType() == null ? 0 : getTargetType().hashCode());
        hash = 23
               * hash
               + (getTargetSignificance() == null ? 0
                               : getTargetSignificance().hashCode());
        hash = 23
               * hash
               + (getTargetTransportChain() == null ? 0
                               : getTargetTransportChain().hashCode());
        hash = 23
               * hash
               + (getRemoteServerAddress() == null ? 0 : getRemoteServerAddress()
                               .hashCode());
        hash = 23
               * hash
               + (getConnectionProximity() == null ? 0
                               : getConnectionProximity().hashCode());
        hash = 23
               * hash
               + (getShareDataSourceWithCMP() == null ? 0
                               : getShareDataSourceWithCMP().hashCode());
        hash = 23
               * hash
               + (getShareDurableSubscriptions() == null ? 0
                               : getShareDurableSubscriptions().hashCode());
        hash = 23
               * hash
               + (getSubscriptionProtocol() == null ? 0
                               : getSubscriptionProtocol().hashCode());
        hash = 23
               * hash
               + (getMulticastInterface() == null ? 0
                               : getMulticastInterface().hashCode());

        return hash;

    }

    /**
     * Checks if this managed connection factory is equal to another managed
     * connection factory.
     * 
     * @param other
     *            the other instance
     * @return true if two instances are equal
     */
    @Override
    final public boolean equals(final Object other) {

        if (this == other) {
            return true;
        }

        if (!(other instanceof JmsJcaManagedConnectionFactoryImpl)) {
            return false;
        }

        JmsJcaManagedConnectionFactoryImpl otherFact = (JmsJcaManagedConnectionFactoryImpl) other;

        if ((getBusName() == null) && (otherFact.getBusName() != null)) {
            return false;
        }
        if ((getClientID() == null) && (otherFact.getClientID() != null)) {
            return false;
        }
        if ((getUserName() == null) && (otherFact.getUserName() != null)) {
            return false;
        }
        if ((getPassword() == null) && (otherFact.getPassword() != null)) {
            return false;
        }
        if ((getNonPersistentMapping() == null)
            && (otherFact.getNonPersistentMapping() != null)) {
            return false;
        }
        if ((getPersistentMapping() == null)
            && (otherFact.getPersistentMapping() != null)) {
            return false;
        }
        if ((getDurableSubscriptionHome() == null)
            && (otherFact.getDurableSubscriptionHome() != null)) {
            return false;
        }
        if ((getReadAhead() == null) && (otherFact.getReadAhead() != null)) {
            return false;
        }
        if ((getTemporaryQueueNamePrefix() == null)
            && (otherFact.getTemporaryQueueNamePrefix() != null)) {
            return false;
        }
        if ((getTemporaryTopicNamePrefix() == null)
            && (otherFact.getTemporaryTopicNamePrefix() != null)) {
            return false;
        }
        if ((getTarget() == null) && (otherFact.getTarget() != null)) {
            return false;
        }
        if ((getTargetType() == null) && (otherFact.getTargetType() != null)) {
            return false;
        }
        if ((getTargetSignificance() == null)
            && (otherFact.getTargetSignificance() != null)) {
            return false;
        }
        if ((getTargetTransportChain() == null)
            && (otherFact.getTargetTransportChain() != null)) {
            return false;
        }
        if ((getRemoteServerAddress() == null)
            && (otherFact.getRemoteServerAddress() != null)) {
            return false;
        }
        if ((getConnectionProximity() == null)
            && (otherFact.getConnectionProximity() != null)) {
            return false;
        }
        if ((getShareDataSourceWithCMP() == null)
            && (otherFact.getShareDataSourceWithCMP() != null)) {
            return false;
        }
        if ((getShareDurableSubscriptions() == null)
            && (otherFact.getShareDurableSubscriptions() != null)) {
            return false;
        }
        if ((getSubscriptionProtocol() == null)
            && (otherFact.getSubscriptionProtocol() != null)) {
            return false;
        }
        if ((getMulticastInterface() == null)
            && (otherFact.getMulticastInterface() != null)) {
            return false;
        }

        boolean retVal = true;

        if ((getBusName() != null) && (retVal)) {
            retVal = getBusName().equals(otherFact.getBusName());
        }
        if ((getClientID() != null) && (retVal)) {
            retVal = getClientID().equals(otherFact.getClientID());
        }
        if ((getUserName() != null) && (retVal)) {
            retVal = getUserName().equals(otherFact.getUserName());
        }
        if ((getPassword() != null) && (retVal)) {
            retVal = getPassword().equals(otherFact.getPassword());
        }
        if ((getNonPersistentMapping() != null) && (retVal)) {
            retVal = getNonPersistentMapping().equals(
                                                      otherFact.getNonPersistentMapping());
        }
        if ((getPersistentMapping() != null) && (retVal)) {
            retVal = getPersistentMapping().equals(
                                                   otherFact.getPersistentMapping());
        }
        if ((getDurableSubscriptionHome() != null) && (retVal)) {
            retVal = getDurableSubscriptionHome().equals(
                                                         otherFact.getDurableSubscriptionHome());
        }
        if ((getReadAhead() != null) && (retVal)) {
            retVal = getReadAhead().equals(otherFact.getReadAhead());
        }
        if ((getTemporaryQueueNamePrefix() != null) && (retVal)) {
            retVal = getTemporaryQueueNamePrefix().equals(
                                                          otherFact.getTemporaryQueueNamePrefix());
        }
        if ((getTemporaryTopicNamePrefix() != null) && (retVal)) {
            retVal = getTemporaryTopicNamePrefix().equals(
                                                          otherFact.getTemporaryTopicNamePrefix());
        }
        if ((getTarget() != null) && (retVal)) {
            retVal = getTarget().equals(otherFact.getTarget());
        }
        if ((getTargetType() != null) && (retVal)) {
            retVal = getTargetType().equals(otherFact.getTargetType());
        }
        if ((getTargetSignificance() != null) && (retVal)) {
            retVal = getTargetSignificance().equals(
                                                    otherFact.getTargetSignificance());
        }
        if ((getTargetTransportChain() != null) && (retVal)) {
            retVal = getTargetTransportChain().equals(
                                                      otherFact.getTargetTransportChain());
        }
        if ((getRemoteServerAddress() != null) && (retVal)) {
            retVal = getRemoteServerAddress().equals(
                                                     otherFact.getRemoteServerAddress());
        }
        if ((getConnectionProximity() != null) && (retVal)) {
            retVal = getConnectionProximity().equals(
                                                     otherFact.getConnectionProximity());
        }
        if ((getShareDataSourceWithCMP() != null) && (retVal)) {
            retVal = getShareDataSourceWithCMP().equals(
                                                        otherFact.getShareDataSourceWithCMP());
        }
        if ((getShareDurableSubscriptions() != null) && (retVal)) {
            retVal = getShareDurableSubscriptions().equals(
                                                           otherFact.getShareDurableSubscriptions());
        }
        if ((getSubscriptionProtocol() != null) && (retVal)) {
            retVal = getSubscriptionProtocol().equals(
                                                      otherFact.getSubscriptionProtocol());
        }
        if ((getMulticastInterface() != null) && (retVal)) {
            retVal = getMulticastInterface().equals(
                                                    otherFact.getMulticastInterface());
        }

        return retVal;

    }

    /**
     * Sets the client ID for this connection factory. Must be set to use
     * durable subscriptions.
     * 
     * @param clientID
     *            the client ID
     */
    @Override
    final public void setClientID(final String clientID) {
        _properties.put(JmsraConstants.CLIENT_ID, clientID);
    }

    /**
     * Returns the client ID.
     * 
     * @return the client ID
     */
    @Override
    final public String getClientID() {
        return (String) _properties.get(JmsraConstants.CLIENT_ID);
    }

    /**
     * Sets the bus name to connect to.
     * 
     * @param busName
     *            the bus name
     */
    @Override
    final public void setBusName(final String busName) {
        _properties.put(SibTrmConstants.BUSNAME, busName);
    }

    /**
     * Returns the bus name to connect to.
     * 
     * @return the bus name
     */
    @Override
    final public String getBusName() {
        return (String) _properties.get(SibTrmConstants.BUSNAME);
    }

    /**
     * Sets the default password for use when none is specified by the
     * application or container.
     * 
     * @param password
     *            the default password
     */
    @Override
    final public void setPassword(final String password) {
        _properties.put(JmsraConstants.PASSWORD, password);
    }

    /**
     * Returns the default password for use when none is specified by the
     * application or container.
     * 
     * @return the default password
     */
    @Override
    final public String getPassword() {
        return (String) _properties.get(JmsraConstants.PASSWORD);
    }

    /**
     * Sets the default user name for use when none is provided by the
     * application or container.
     * 
     * @param userName
     *            the default user name
     */
    @Override
    final public void setUserName(final String userName) {
        _properties.put(JmsraConstants.USERNAME, userName);
    }

    /**
     * Returns the default user name for use when none is specified by the
     * application or container.
     * 
     * @return the default user name
     */
    @Override
    final public String getUserName() {
        return (String) _properties.get(JmsraConstants.USERNAME);
    }

    /**
     * Returns the durable subscription home.
     * 
     * @return the durable subscription home
     */
    @Override
    public String getDurableSubscriptionHome() {
        return (String) _properties.get(JmsraConstants.DURABLE_SUB_HOME);
    }

    /**
     * Sets the durable subscription home.
     * 
     * @param durableSubscriptionHome
     *            the durable subscription home
     */
    @Override
    public void setDurableSubscriptionHome(final String durableSubscriptionHome) {
        _properties.put(JmsraConstants.DURABLE_SUB_HOME,
                        durableSubscriptionHome);
    }

    /**
     * Returns the read ahead.
     * 
     * @return the read ahead
     */
    @Override
    public String getReadAhead() {
        return (String) _properties.get(JmsraConstants.READ_AHEAD);
    }

    /**
     * Sets the read ahead.
     * 
     * @param readAhead
     *            the read ahead
     */
    @Override
    public void setReadAhead(final String readAhead) {
        _properties.put(JmsraConstants.READ_AHEAD, readAhead);
    }

    /**
     * Gets the prefix for temporary queue names.
     * 
     * @return the prefix
     */
    @Override
    public String getTemporaryQueueNamePrefix() {
        return (String) _properties.get(JmsraConstants.TEMP_QUEUE_NAME_PREFIX);
    }

    /**
     * Sets the prefix for temporary queue names.
     * 
     * @param prefix
     *            the prefix
     * @throws JMSException
     *             if the prefix is longer than 12 characters
     */
    @Override
    public void setTemporaryQueueNamePrefix(final String prefix)
                    throws JMSException {
         
          temporaryQueueNamePrefix = prefix;
         // defect 99056
        _properties.put(JmsraConstants.TEMP_QUEUE_NAME_PREFIX, prefix);

        if ((prefix != null) && (prefix.length() > 12)) {

            final JMSException exception = new JMSException(NLS
                            .getFormattedMessage(("DESTINATION_PREFIX_LONG_CWSJR1025"),
                                                 new Object[] { prefix }, null));
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw exception;
        }

    }

    /**
     * Gets the prefix for temporary topic names.
     * 
     * @return the prefix
     */
    @Override
    public String getTemporaryTopicNamePrefix() {
        return (String) _properties.get(JmsraConstants.TEMP_TOPIC_NAME_PREFIX);
    }

    /**
     * Sets the prefix for temporary queue names.
     * 
     * @param prefix
     *            the prefix
     * @throws JMSException
     *             if the prefix is longer than 12 characters
     */
    @Override
    public void setTemporaryTopicNamePrefix(final String prefix)
                    throws JMSException {
        // defect 99056
        _properties.put(JmsraConstants.TEMP_TOPIC_NAME_PREFIX, prefix);
        if ((prefix != null) && (prefix.length() > 12)) {

            final JMSException exception = new JMSException(NLS
                            .getFormattedMessage(("DESTINATION_PREFIX_LONG_CWSJR1029"),
                                                 new Object[] { prefix }, null));
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

            throw exception;
        }

    }

    /**
     * Returns the non-persistent mapping.
     * 
     * @return the non-persistent mapping
     */
    @Override
    public String getNonPersistentMapping() {
        return (String) _properties.get(JmsraConstants.NON_PERSISTENT_MAP);
    }

    /**
     * Sets the non-persistent mapping.
     * 
     * @param nonPersistentMapping
     *            the non-persistent mapping
     */
    @Override
    public void setNonPersistentMapping(final String nonPersistentMapping) {
        _properties
                        .put(JmsraConstants.NON_PERSISTENT_MAP, nonPersistentMapping);
    }

    /**
     * Returns the persistent mapping.
     * 
     * @return the persistent mapping
     */
    @Override
    public String getPersistentMapping() {
        return (String) _properties.get(JmsraConstants.PERSISTENT_MAP);
    }

    /**
     * Sets the persistent mapping.
     * 
     * @param persistentMapping
     *            the persistent mapping
     */
    @Override
    public void setPersistentMapping(String persistentMapping) {
        _properties.put(JmsraConstants.PERSISTENT_MAP, persistentMapping);
    }

    /**
     * Gets the target .
     * 
     * @return the target
     */
    @Override
    public String getTarget() {
        return (String) _properties.get(SibTrmConstants.TARGET_GROUP);
    }

    /**
     * Sets the target.
     * 
     * @param target
     *            the target
     */
    @Override
    public void setTarget(final String target) {
        _properties.put(SibTrmConstants.TARGET_GROUP, target);
    }

    /**
     * Gets the target type.
     * 
     * @return the target type
     */
    @Override
    public String getTargetType() {
        return (String) _properties.get(SibTrmConstants.TARGET_TYPE);
    }

    /**
     * Sets the target type.
     * 
     * @param targetType
     *            the target type
     */
    @Override
    public void setTargetType(final String targetType) {
        _properties.put(SibTrmConstants.TARGET_TYPE, targetType);
    }

    /**
     * Gets the target significance.
     * 
     * @return the target significance
     */
    @Override
    public String getTargetSignificance() {
        return (String) _properties.get(SibTrmConstants.TARGET_SIGNIFICANCE);
    }

    /**
     * Sets the target significance.
     * 
     * @param targetSignificance
     *            the target significance
     */
    @Override
    public void setTargetSignificance(final String targetSignificance) {
        _properties
                        .put(SibTrmConstants.TARGET_SIGNIFICANCE, targetSignificance);
    }

    /**
     * Returns the name of the target inbound transport chain.
     * 
     * @return the target transport chain
     */
    @Override
    public String getTargetTransportChain() {
        return (String) _properties.get(SibTrmConstants.TARGET_TRANSPORT_CHAIN);
    }

    /**
     * Sets the name of the target inbound transport chain.
     * 
     * @param targetTransportChain
     *            the target transport chain
     */
    @Override
    public void setTargetTransportChain(String targetTransportChain) {
        _properties.put(SibTrmConstants.TARGET_TRANSPORT_CHAIN,
                        targetTransportChain);
    }

    /**
     * Gets the connection name
     * 
     * @return the connection name
     */
    @Override
    public String getRemoteServerAddress() {
        return (String) _properties.get(SibTrmConstants.PROVIDER_ENDPOINTS);
    }

    /**
     * Sets the connection name
     * 
     * @param remoteServerAddress
     */
    @Override
    public void setRemoteServerAddress(final String remoteServerAddress) {
        _properties.put(SibTrmConstants.PROVIDER_ENDPOINTS, remoteServerAddress);
    }

    /**
     * Gets the target transport
     * 
     * @return the target transport
     */
    @Override
    public String getTargetTransport() {
        return (String) _properties.get(SibTrmConstants.TARGET_TRANSPORT_TYPE);
    }

    /**
     * Sets the target transport
     * 
     * @param targetTransport
     *            the target transport
     */
    @Override
    public void setTargetTransport(final String targetTransport) {
        _properties.put(SibTrmConstants.TARGET_TRANSPORT_TYPE, targetTransport);
    }

    /**
     * Returns the flag indicating whether the connection to the messaging
     * engine database should be shared with that use for container-managed
     * persistence.
     * 
     * @return <code>true</code> if the connection should be shared, otherwise
     *         <code>false</code>
     */
    @Override
    public Boolean getShareDataSourceWithCMP() {
        return (Boolean) _properties
                        .get(JmsraConstants.SHARE_DATA_SOURCE_WITH_CMP);
    }

    /**
     * Sets the flag indicating whether the connection to the messaging engine
     * database should be shared with that use for container-managed
     * persistence.
     * 
     * @param sharing
     *            <code>true</code> if the connection should be shared,
     *            otherwise <code>false</code>
     */
    @Override
    public void setShareDataSourceWithCMP(final Boolean sharing) {
        _properties.put(JmsraConstants.SHARE_DATA_SOURCE_WITH_CMP, sharing);
    }

    /**
     * Gets the connection proximity.
     * 
     * @return the connection proximity
     */
    @Override
    public String getConnectionProximity() {
        return (String) _properties.get(SibTrmConstants.CONNECTION_PROXIMITY);
    }

    /**
     * Set the connection proximity.
     * 
     * @param connectionProximity
     *            the connection proximity
     */
    @Override
    public void setConnectionProximity(final String connectionProximity) {
        _properties.put(SibTrmConstants.CONNECTION_PROXIMITY,
                        connectionProximity);
    }

    /**
     * Gets the durable subscription sharing property.
     * 
     * @return the durable subscription sharing property
     */
    @Override
    public String getShareDurableSubscriptions() {
        return (String) _properties.get(JmsraConstants.SHARE_DURABLE_SUBS);
    }

    /**
     * Sets the durable subscription sharing property.
     * 
     * @param sharedDurSubs
     *            the durable subscription sharing property
     */
    @Override
    public void setShareDurableSubscriptions(String sharedDurSubs) {
        _properties.put(JmsraConstants.SHARE_DURABLE_SUBS, sharedDurSubs);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaActivationSpec#setShareDurableSubscription(java.lang.Boolean)
     */
    @Override
    public void setShareDurableSubscription(Boolean shareDurSubs) {
        if (shareDurSubs)
            setShareDurableSubscriptions(ApiJmsConstants.SHARED_DSUBS_ALWAYS);
        else
            setShareDurableSubscriptions(ApiJmsConstants.SHARED_DSUBS_NEVER);
    }

    /**
     * Gets the subscription protocol property.
     * 
     * @return the subscription protocol property
     */
    @Override
    public String getSubscriptionProtocol() {
        return (String) _properties.get(SibTrmConstants.SUBSCRIPTION_PROTOCOL);
    }

    /**
     * Sets the subscription protocol property.
     * 
     * @param subscriptionProtocol
     *            the subscription protocol property
     */
    @Override
    public void setSubscriptionProtocol(String subscriptionProtocol) {
        _properties.put(SibTrmConstants.SUBSCRIPTION_PROTOCOL, subscriptionProtocol);
    }

    /**
     * Gets the multicast interface property.
     * 
     * @return the multicast interface property
     */
    @Override
    public String getMulticastInterface() {
        return (String) _properties.get(SibTrmConstants.MULTICAST_INTERFACE);
    }

    /**
     * Sets the multicast interface property.
     * 
     * @param multicastInterface
     *            the multicast interface property
     */
    @Override
    public void setMulticastInterface(String multicastInterface) {
        _properties.put(SibTrmConstants.MULTICAST_INTERFACE, multicastInterface);
    }

    /**
     * Gets the property indicating if the producer will modify the payload after setting it.
     * 
     * @return String containing the property value.
     */
    @Override
    public String getProducerDoesNotModifyPayloadAfterSet() {
        return (String) _properties.get(JmsraConstants.PRODUCER_DOES_NOT_MODIFY_PAYLOAD_AFTER_SET);
    }

    /**
     * Sets the property that indicates if the producer will modify the payload after setting it.
     * 
     * @param propertyValue - A string containing the property value.
     */
    @Override
    public void setProducerDoesNotModifyPayloadAfterSet(String propertyValue) {
        _properties.put(JmsraConstants.PRODUCER_DOES_NOT_MODIFY_PAYLOAD_AFTER_SET, propertyValue);
    }

    /**
     * Gets the property indicating if the consumer will modify the payload after getting it.
     * 
     * @return String containing the property value.
     */
    @Override
    public String getConsumerDoesNotModifyPayloadAfterGet() {
        return (String) _properties.get(JmsraConstants.CONSUMER_DOES_NOT_MODIFY_PAYLOAD_AFTER_GET);
    }

    /**
     * Sets the property that indicates if the consumer will modify the payload after getting it.
     * 
     * @param propertyValue containing the property value.
     */
    @Override
    public void setConsumerDoesNotModifyPayloadAfterGet(String propertyValue) {
        _properties.put(JmsraConstants.CONSUMER_DOES_NOT_MODIFY_PAYLOAD_AFTER_GET, propertyValue);
    }

    /**
     * Returns an object containing the username and password with which to
     * connect. In order of precedence:
     * <ol>
     * <li>If passed a <code>Subject</code> and it contains a
     * <code>PasswordCredential</code> for this managed connection factory
     * then use the user name and password from that.</li>
     * <li>If passed a <code>Subject</code> and it does not contain a
     * suitable <code>PasswordCredential</code> then return <code>null</code>
     * so that we attempt to authenticate with the entire <code>Subject</code>/
     * </li>
     * <li>If we are not passed a <code>Subject</code> but the application
     * specified a username and password then use those.</li>
     * <li>If we are not passed a <code>Subject</code> and the application
     * did not specify a username and password then use the defaults specified
     * on this managed connection factory.</li>
     * </ol>
     * 
     * @param subject
     *            subject to check for user name and password
     * @param requestInfo
     *            request information to check for user name and password
     * @return object containing user name and password or <code>null</code>
     *         if the subject should be used
     */
    JmsJcaUserDetails getUserDetails(final Subject subject,
                                     final ConnectionRequestInfo requestInfo) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getUserDetails", new Object[] {
                                                                     JmsJcaManagedConnection.subjectToString(subject),
                                                                     requestInfo });
        }

        JmsJcaUserDetails result = null;

        if (subject == null) {

            // We don't have a subject (case 3 or 4)

            String userName = null;
            String password = null;

            if (requestInfo instanceof JmsJcaConnectionRequestInfo) {

                // See if the request information contains any user details
                JmsJcaConnectionRequestInfo jmsJcaConnReq = (JmsJcaConnectionRequestInfo) requestInfo;
                result = jmsJcaConnReq.getUserDetails();

            }

            if (result == null) {

                // The request information did not contain any user details
                // (case 4)

                userName = getUserName();
                password = getPassword();
                result = new JmsJcaUserDetails(userName, password);

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(this, TRACE, "Using default credentials from "
                                             + "managed connection factory");
                }

            } else {

                // The request information did contain user details (case 3)
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(this, TRACE,
                                "Using credentials passed by application");
                }

            }

        } else {

            // We were passed a Subject (case 1 or 2)

            // See if the credential set for the Subject contains a
            // PasswordCredential for this managed connection factory

            PasswordCredential credential = (PasswordCredential) AccessController
                            .doPrivileged(new PrivilegedAction() {

                                @Override
                                public Object run() {

                                    final Set credentialSet = subject
                                                    .getPrivateCredentials(PasswordCredential.class);

                                    for (final Iterator iterator = credentialSet
                                                    .iterator(); iterator.hasNext();) {

                                        final Object next = iterator.next();
                                        if (next instanceof PasswordCredential) {

                                            final PasswordCredential subjectCredential = (PasswordCredential) next;

                                            if (JmsJcaManagedConnectionFactoryImpl.this
                                                            .equals(subjectCredential
                                                                            .getManagedConnectionFactory())) {

                                                return subjectCredential;

                                            }
                                        }
                                    }
                                    return null;
                                }
                            });

            if (credential == null) {

                // Subject didn't contain a suitable PasswordCredential (case 2)

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(this, TRACE,
                                "No PasswordCredential in Subject - "
                                                + "using Subject for authentication");
                }

            } else {

                // Subject did contain a PasswordCredential (case 1)

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(TRACE, "Using PasswordCredential from Subject");
                }
                result = new JmsJcaUserDetails(credential.getUserName(), String
                                .valueOf(credential.getPassword()));

            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            if (result != null) {
                SibTr.debug(this, TRACE, "Credential contains userName", result
                                .getUserName());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getUserDetails", result);
        }
        return result;

    }

    /**
     * Returns the map of properties required by TRM.
     * 
     * @return the map of TRM properties
     */
    Map getTrmProperties() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getTrmProperties");
        }

        final Map trmProperties = new HashMap();

        final String trmBusName = getBusName();
        if ((trmBusName != null) && (!trmBusName.equals(""))) {
            trmProperties.put(SibTrmConstants.BUSNAME, trmBusName);
        }

        final String trmTarget = getTarget();
        if ((trmTarget != null) && (!trmTarget.equals(""))) {
            trmProperties.put(SibTrmConstants.TARGET_GROUP, trmTarget);
        }

        final String trmTargetType = getTargetType();
        if ((trmTargetType != null) && (!trmTargetType.equals(""))) {
            trmProperties.put(SibTrmConstants.TARGET_TYPE, trmTargetType);
        }

        final String trmTargetSignificance = getTargetSignificance();
        if ((trmTargetSignificance != null)
            && (!trmTargetSignificance.equals(""))) {
            trmProperties.put(SibTrmConstants.TARGET_SIGNIFICANCE,
                              trmTargetSignificance);
        }

        final String trmTargetTransportChain = getTargetTransportChain();
        if ((trmTargetTransportChain != null)
            && (!trmTargetTransportChain.equals(""))) {
            trmProperties.put(SibTrmConstants.TARGET_TRANSPORT_CHAIN,
                              trmTargetTransportChain);
        }

        final String trmProviderEndpoints = getRemoteServerAddress();
        if ((trmProviderEndpoints != null)
            && (!trmProviderEndpoints.equals(""))) {
            trmProperties.put(SibTrmConstants.PROVIDER_ENDPOINTS,
                              trmProviderEndpoints);
        }

        final String trmTargetTransport = getTargetTransport();
        if ((trmTargetTransport != null)
            && (!trmTargetTransport.equals(""))) {
            trmProperties.put(SibTrmConstants.TARGET_TRANSPORT_TYPE,
                              trmTargetTransport);
        }

        final String trmConnectionProximity = getConnectionProximity();
        if ((trmConnectionProximity != null)
            && (!trmConnectionProximity.equals(""))) {
            trmProperties.put(SibTrmConstants.CONNECTION_PROXIMITY,
                              trmConnectionProximity);
        }

        final String trmSubscriptionProtocol = getSubscriptionProtocol();
        if ((trmSubscriptionProtocol != null)
            && (!trmSubscriptionProtocol.equals(""))) {
            trmProperties.put(SibTrmConstants.SUBSCRIPTION_PROTOCOL,
                              trmSubscriptionProtocol);
        }

        final String trmMulticastInterface = getMulticastInterface();
        if ((trmMulticastInterface != null)
            && (!trmMulticastInterface.equals(""))) {
            trmProperties.put(SibTrmConstants.MULTICAST_INTERFACE,
                              trmMulticastInterface);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getTrmProperties", trmProperties);
        }
        return trmProperties;

    }

    private JmsRAFactoryFactory getJmsRAFactoryFactory()
                    throws ResourceException {

        if (_jmsFactoryFactory == null) {

            try {

                JmsFactoryFactory jmsFactory = JmsFactoryFactory.getInstance();

                if (jmsFactory instanceof JmsRAFactoryFactory) {

                    _jmsFactoryFactory = (JmsRAFactoryFactory) jmsFactory;

                } else {

                    throw new ResourceAdapterInternalException(
                                    NLS
                                                    .getFormattedMessage(
                                                                         ("JMS_CONNECTION_FAIL_CWSJR1024"),
                                                                         new Object[] {
                                                                                       "getJmsRAFactoryFactory",
                                                                                       JmsRAFactoryFactory.class
                                                                                                       .getName(),
                                                                                       jmsFactory == null ? "null"
                                                                                                       : jmsFactory
                                                                                                                       .getClass()
                                                                                                                       .getName() },
                                                                         null));
                }

            } catch (final JMSException exception) {

                FFDCFilter.processException(exception, CLASS_NAME
                                                       + ".getJmsRAFactoryFactory", FFDC_PROBE_5, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceException(exception);

            }
        }

        return _jmsFactoryFactory;

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
        sb.append(" <logWriter=");
        sb.append(_logWriter);
        sb.append("> <busName=");
        sb.append(getBusName());
        sb.append("> <clientID=");
        sb.append(getClientID());
        sb.append("> <userName=");
        sb.append(getUserName());
        sb.append("> <password=");
        sb.append(getPassword() == null ? null : "*****");
        sb.append("> <nonPersistentMapping=");
        sb.append(getNonPersistentMapping());
        sb.append("> <persistentMapping=");
        sb.append(getPersistentMapping());
        sb.append("> <durableSubscriptionHome=");
        sb.append(getDurableSubscriptionHome());
        sb.append("> <readAhead=");
        sb.append(getReadAhead());
        sb.append("> <temporaryQueueNamePrefix=");
        sb.append(getTemporaryQueueNamePrefix());
        sb.append("> <temporaryTopicNamePrefix=");
        sb.append(getTemporaryTopicNamePrefix());
        sb.append("> <target=");
        sb.append(getTarget());
        sb.append("> <targetSignificance=");
        sb.append(getTargetSignificance());
        sb.append("> <targetTransportChain=");
        sb.append(getTargetTransportChain());
        sb.append("> <targetType=");
        sb.append(getTargetType());
        sb.append("> <RemoteServerAddress=");
        sb.append(getRemoteServerAddress());
        sb.append("> <TargetTransport=");
        sb.append(getTargetTransport());
        sb.append("> <connectionProximity=");
        sb.append(getConnectionProximity());
        sb.append("> <shareDataSourceWithCMP=");
        sb.append(getShareDataSourceWithCMP());
        sb.append("> <shareDurableSubscriptions=");
        sb.append(getShareDurableSubscriptions());
        sb.append("> <cachedFactory=");
        sb.append(_jmsFactoryFactory);
        sb.append("> <producerDoesNotModifyPayloadAfterSet=");
        sb.append(getProducerDoesNotModifyPayloadAfterSet());
        sb.append("> <consumerDoesNotModifyPayloadAfterGet=");
        sb.append(getConsumerDoesNotModifyPayloadAfterGet());
        sb.append(">]");
        return sb.toString();

    }

    /**
     * Method to return information about this factory for FFDC
     * 
     * @return An array of strings to be added to the FFDC log
     */
    @Override
    public String[] introspectSelf() {
        return new String[] { toString() };
    }

    /**
     * Method to return the connection type.
     * 
     * @return the connection type
     */
    String getConnectionType() {
        return CONN_FACTORY_TYPE;
    }

    /**
     * Returns a reference for this managed connection factory.
     * 
     * @return a reference for this managed connection factory
     */
    Reference getReference() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getReference");
        }

        // Create a reference object describing this class
        final Reference reference = new Reference(getConnectionType(),
                        getClass().getName(), null);

        // Make sure no-one can pull the rug from beneath us.
        synchronized (_properties) {

            // Convert the map of properties into an encoded form, where the
            // keys have the necessary prefix on the front, and the values are
            // all Strings.
            final Map encodedMap = JmsJcaReferenceUtils.getInstance()
                            .getStringEncodedMap(_properties, defaultJNDIProperties);

            // Now turn the encoded map into the reference items.
            for (final Iterator iterator = encodedMap.entrySet().iterator(); iterator
                            .hasNext();) {

                final Map.Entry entry = (Map.Entry) iterator.next();
                final String prefixedKey = (String) entry.getKey();
                final String stringForm = (String) entry.getValue();

                // Store the prefixed key and value in string form.
                reference.add(new StringRefAddr(prefixedKey, stringForm));

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, "getReference", reference);
        }
        return reference;

    }

    /**
     * Initializes this managed connection factory using the given reference.
     * 
     * @param reference
     *            the reference
     */
    public void setReference(final Reference reference) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "setReference", reference);
        }

        // Make sure no-one can pull the rug from beneath us.
        synchronized (_properties) {
            _properties = JmsJcaReferenceUtils.getInstance()
                            .getMapFromReference(reference, defaultJNDIProperties);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "setReference");
        }

    }

    /**
     * Constructs an object factory which constructs a managed connection
     * factory, calls the non-managed createConnectionFactory and then calls the
     * setReference on the JMSConnFactory.
     * 
     * @param object
     *            the referenceable object to be created
     * @param name
     *            the name
     * @param context
     *            the naming context
     * @param environment
     *            the environment
     * @return a JmsConnectionFactory or null if it could not obtain one.
     */
    @Override
    public Object getObjectInstance(final Object object, final Name name,
                                    final Context context, final Hashtable environment)
                    throws Exception {

        JmsConnectionFactory jmsConnectionFactory = null;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getObjectInstance", new Object[] {
                                                                        object, name, context, environment });
        }

        if (object instanceof Reference) {

            final Reference reference = (Reference) object;
            final String clsName = reference.getClassName();

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(this, TRACE, "class name is " + clsName);
            }

            if ((JmsJcaManagedConnectionFactoryImpl.CONN_FACTORY_TYPE
                            .equals(clsName))
                || (JmsJcaManagedQueueConnectionFactoryImpl.QUEUE_CONN_FACTORY_TYPE
                                .equals(clsName))
                || (JmsJcaManagedTopicConnectionFactoryImpl.TOPIC_CONN_FACTORY_TYPE
                                .equals(clsName))) {

                try {

                    jmsConnectionFactory = (JmsConnectionFactory) createConnectionFactory();
                    jmsConnectionFactory.setReference(reference);

                } catch (final ResourceException exception) {

                    FFDCFilter.processException(exception, CLASS_NAME
                                                           + "getObjectInstance", FFDC_PROBE_6, this);
                    throw new ResourceAdapterInternalException(NLS
                                    .getFormattedMessage(
                                                         "EXCEPTION_RECEIVED_CWSJR1027",
                                                         new Object[] { exception,
                                                                       "getObjectInstance" }, null),
                                    exception);

                }
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getObjectInstance", jmsConnectionFactory);
        }
        return jmsConnectionFactory;

    }

    ConnectionFactory createJmsConnFactory(
                                           final JmsRAFactoryFactory jmsFactory,
                                           final JmsJcaConnectionFactoryImpl connFactory) {
        return jmsFactory.createConnectionFactory(connFactory);
    }

    ConnectionFactory createJmsConnFactory(
                                           final JmsRAFactoryFactory jmsFactory,
                                           final JmsJcaConnectionFactoryImpl connFactory,
                                           final JmsJcaManagedConnectionFactory fact) {
        return jmsFactory.createConnectionFactory(connFactory, fact);
    }

    public Set getInvalidConnections(Set connectionSet) throws ResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getInvalidConnections", connectionSet);
        }

        Set invalidConnections = new HashSet();
        //Check if our managed connections are invalid
        // Iterate over the potential matches
        for (final Iterator iterator = connectionSet.iterator(); iterator.hasNext();) {

            final Object object = iterator.next();

            // Check that it is one of ours
            if (object instanceof JmsJcaManagedConnection)
            {
                final JmsJcaManagedConnection potentialInvalidConnection = (JmsJcaManagedConnection) object;
                if (!potentialInvalidConnection.isValid())
                {
                    invalidConnections.add(potentialInvalidConnection);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getInvalidConnections", invalidConnections);
        }
        return invalidConnections;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return _resourceAdapter;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter resourceAdapter)
                    throws ResourceException {
        _resourceAdapter = resourceAdapter;

    }

}