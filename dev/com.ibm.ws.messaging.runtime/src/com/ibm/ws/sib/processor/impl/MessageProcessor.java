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

import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SINotSupportedException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.websphere.sib.management.SibNotificationConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.messaging.security.Authentication;
import com.ibm.ws.messaging.security.Authorization;
import com.ibm.ws.messaging.security.MessagingSecurityException;
import com.ibm.ws.messaging.security.RuntimeSecurityService;
import com.ibm.ws.messaging.security.authentication.MessagingAuthenticationException;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.ForeignBusDefinition;
import com.ibm.ws.sib.admin.JsAdminService;
import com.ibm.ws.sib.admin.JsBus;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsEngineComponent;
import com.ibm.ws.sib.admin.JsMEConfig;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.LWMConfig;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.admin.MQLinkDefinition;
import com.ibm.ws.sib.admin.RuntimeEventListener;
import com.ibm.ws.sib.admin.SIBExceptionBase;
import com.ibm.ws.sib.admin.SIBExceptionDestinationNotFound;
import com.ibm.ws.sib.admin.internal.JsAdminFactory;
import com.ibm.ws.sib.admin.internal.JsMainAdminComponentImpl;
import com.ibm.ws.sib.comms.ClientConnection;
import com.ibm.ws.sib.comms.MEConnectionListener;
import com.ibm.ws.sib.matchspace.impl.Matching;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.impl.ControlMessageFactory;
import com.ibm.ws.sib.mfp.impl.JsMessageHandleFactory;
import com.ibm.ws.sib.mfp.impl.SchemaStore;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.Administrator;
import com.ibm.ws.sib.processor.CommandHandler;
import com.ibm.ws.sib.processor.ExceptionDestinationHandler;
import com.ibm.ws.sib.processor.MPCoreConnection;
import com.ibm.ws.sib.processor.SIMPAdmin;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.SIMPFactory;
import com.ibm.ws.sib.processor.exceptions.SIMPConnectionUnavailableException;
import com.ibm.ws.sib.processor.exceptions.SIMPDestinationAlreadyExistsException;
import com.ibm.ws.sib.processor.exceptions.SIMPMessageProcessorCorruptException;
import com.ibm.ws.sib.processor.impl.corespitrace.CoreSPIConnFactory;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableResource;
import com.ibm.ws.sib.processor.impl.interfaces.MPCallsToUnitTestHandler;
import com.ibm.ws.sib.processor.impl.store.BatchHandler;
import com.ibm.ws.sib.processor.impl.store.MessageProcessorStore;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.processor.io.MPIO;
import com.ibm.ws.sib.processor.matching.MessageProcessorMatching;
import com.ibm.ws.sib.processor.matching.TopicAuthorization;
import com.ibm.ws.sib.processor.proxyhandler.MultiMEProxyHandler;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.MessageProcessorControl;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.SearchResultsObjectPool;
import com.ibm.ws.sib.processor.utils.StoppableThreadCache;
import com.ibm.ws.sib.processor.utils.ThreadPoolListenerImpl;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.security.auth.AuthUtils;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.trm.contact.CommsErrorListener;
import com.ibm.ws.sib.trm.dlm.DestinationLocationManager;
import com.ibm.ws.sib.trm.impl.TrmConstantsImpl;
import com.ibm.ws.sib.trm.links.LinkManager;
import com.ibm.ws.sib.trm.links.mql.MQLinkManager;
import com.ibm.ws.sib.trm.topology.RoutingManager;
import com.ibm.ws.sib.trm.topology.TopologyListener;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.util.ObjectPool;
import com.ibm.ws.util.ThreadPool;
import com.ibm.ws.util.ThreadPool.ThreadPoolQueueIsFullException;
import com.ibm.ws.util.ThreadPoolListener;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;
import com.ibm.wsspi.sib.core.SICoreConnectionListener;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;

//Venu liberty change. removed JsEngineComponentWithEventListener interface from MP implementation
public final class MessageProcessor implements JsEngineComponent,
                SICoreConnectionFactory, SIMPAdmin,
                SIMPFactory, ControllableResource {
    private static final TraceComponent tc = SibTr.register(
                                                            MessageProcessor.class, SIMPConstants.MP_TRACE_GROUP,
                                                            SIMPConstants.RESOURCE_BUNDLE);

    /**
     * NLS for component
     */
    private static final TraceNLS nls = TraceNLS
                    .getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    /**
     * The SearchResultsObjectPool Object Pool that is used to cache search
     * results objects that are used by the MatchSpace.
     */
    private SearchResultsObjectPool _searchResultsPool;

    private ControlAdapter _controlAdapter;

    // attributes set by admin at startup
    private long _highMessageThreshold = 1000;

    private SIMPTransactionManager _txManager;
    private ThreadPool _consumerThreadPool;
    private ThreadPool _systemThreadPool;
    private JsMessagingEngine _engine;
    private MessageStore _msgStore;
    private volatile boolean _started;
    /** Starting is used to indicate that the messaging engine is starting */
    private boolean _starting = false;
    private static HashMap<String, Object> _factories = null; // HashMap of
    // Factories per
    // JVM
    private HashMap<String, Object> _meFactories = null; // HashMap of Factories
    // per ME
    private boolean _discardMsgsAfterQueueDeletion;
    private boolean _isSingleServer = false;
    private boolean _singleServerSet = false;

    private Set<SIBUuid8> _messagingEnginesOnBus;
    /** The list of connections that this messaging engine contains */
    private HashMap<SICoreConnection, SICoreConnection> _connections;
    private final LockManager _connectionsLockManager = new LockManager();

    private ConsumerList _consumerList = null;

    // Listener registered with TRM to find out about changes to the
    // sets of messaging engines localizing destinations
    private DestinationChangeListener _destinationChangeListener;
    private LinkChangeListener _linkChangeListener;

    private AdministratorImpl _administrator;
    private DestinationManager _destinationManager;
    private DynamicConfigManager _dynamicConfigManager;
    private MessageProcessorMatching _messageProcessorMatching;
    private MultiMEProxyHandler _multiMEProxyHandler;

    /** The connection used to create the System Destinations */
    private MPCoreConnection _connectionToMP;
    /** The Queue Name used to receive Subscription update messages */
    private JsDestinationAddress _proxyHandlerDestAddr;
    private JsDestinationAddress _tdReceiverAddr;

    private MPIO _mpio;

    private DestinationLocationManager _destinationLocationManager = null;
    private LinkManager _linkManager = null;
    private MQLinkManager _mqLinkManager = null;
    /**
     * The singleton instance of the control message factory for creating
     * messages.
     */
    private static ControlMessageFactory _controlMessageFactory;

    /**
     * Singleton root itemstream for the entire presistent storage structure.
     * 174199.2.1
     */
    private MessageProcessorStore _persistentStore;

    /** The MP Alarm manager instance, used for handling all alarms */
    private MPAlarmManager _mpAlarmManager;
    private ObjectPool _batchedTimeoutManagerEntryPool;

    private BatchHandler _publicationBatchHandler;
    private BatchHandler _targetBatchHandler;
    private BatchHandler _sourceBatchHandler;

    /** Support for destination access control */
    private AccessChecker _accessChecker;

    /** Support for authorisation utilities */
    private final AuthUtils _authorisationUtils;

    /** Support for discriminator access control */
    private TopicAuthorization _topicAuthorization;

    /** Flag that indicates whether the bus is secure or not */
    private boolean _isBusSecure = false;
    /**
     * A handler for calls from MP to a unit test.
     * <p>
     * With the shipped run-time, this will always be null. If set to non-null,
     * then places in the product code which try to call the unit test will
     * work.
     * <p>
     * This is primarily used by unit tests to have tight control over
     * asynchronous events, so the order of events in tests can be controlled.
     */
    private static MPCallsToUnitTestHandler _mpCallsToUnitTestHandler = null;

    /**
     * Indicator as to whether a dynamic config change has effected the bus-wide
     * definitions
     */
    private boolean _busReloaded = false;

    /** Interface to SelectionCriteria factory. */
    private SelectionCriteriaFactory _selectionCriteriaFactory;

    /** Singleton JSMessageHandleFactory. */
    private static JsMessageHandleFactory _jsMessageHandleFactory;

    private MulticastPropertiesImpl _multicastProperties = null;
    // RuntimeEventListener for firing Notifications
    private RuntimeEventListener _runtimeEventListener;
    private final StoppableThreadCache _stoppableThreadCache;

    /**
     * Lock used when starting/stopping MP
     */
    private final Object _mpStartStopLock = new Object();

    /** Table of CommandHandlers against registration keys */
    private HashMap<String, CommandHandler> _registeredHandlers = null;

    // Anchor for Custom Properties object

    private final CustomProperties _customProperties = new CustomProperties();

    //Venu mock mock 
    //In twas locality Set is localization MEs for Queuepoint
    // in this version of Liberty profile, it just containts current ME uuid
    HashSet _localistySet = new HashSet();

    // Security code changes for Liberty: Sharath Start
    private final RuntimeSecurityService runtimeSecurityService = RuntimeSecurityService.SINGLETON_INSTANCE;
    private Authentication _authentication;
    private Authorization _authorization;

    // Security code changes for Liberty: Sharath End

    /**
     * Constructor of the Message Processor object
     */
    public MessageProcessor() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "MessageProcessor");

        _started = false;

        _consumerList = new ConsumerList(this);

        _stoppableThreadCache = new StoppableThreadCache();

        _registeredHandlers = new HashMap<String, CommandHandler>();

        _authorisationUtils = new AuthUtils();

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "MessageProcessor", this);
    }

    /**
     * Indicates whether the MP has started or not.
     */
    @Override
    public boolean isStarted() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "isStarted");
            SibTr.exit(tc, "isStarted", new Boolean(_started));
        }

        return _started;
    }

    /**
     * Connection locks are obtained on the connection lock manager.
     * 
     * @return
     */
    protected SICoreConnection createConnection() throws SIResourceException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createConnection");

        SICoreConnection newConn = new ConnectionImpl(this, null, null);

        // Lock the connections so that we can't be stopped midway through a
        // create
        _connectionsLockManager.lock();

        try {
            // Now we have a lock make sure we are started.
            checkStarted();

            synchronized (_connections) {
                _connections.put(newConn, newConn);
            }
        } finally {
            _connectionsLockManager.unlock();
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createConnection", newConn);

        return newConn;
    }

    /**
     * Removes a connection from the list of connections
     * 
     * @param connection
     *            The connection to remove.
     */
    protected void removeConnection(ConnectionImpl connection) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "removeConnection", connection);

        synchronized (_connections) {
            _connections.remove(connection);
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "removeConnection");
    }

    protected SICoreConnection createConnection(Subject subject,
                                                boolean system, Map connectionProperties)
                    throws SIResourceException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createConnection", new Object[] {
                                                              Boolean.valueOf(system), connectionProperties });

        try {
            if (runtimeSecurityService.isUnauthenticated(subject)) {
                //114580
                String userName = runtimeSecurityService.getUniqueUserName(subject);
                throw new SIResourceException(nls.
                                getFormattedMessage("USER_NOT_AUTHENTICATED_MSE1009",
                                                    new Object[] { userName }, "User " + userName + " is not authenticated"));
            }
        } catch (MessagingAuthenticationException mae) {
            throw new SIResourceException(mae.getMessage());
            //114580
        } catch (MessagingSecurityException mse) {
            throw new SIResourceException(mse.getMessage());
        }

        SICoreConnection newConn = new ConnectionImpl(this, subject,
                        connectionProperties);

        // Lock the connections so that we can't be stopped midway through a
        // create
        _connectionsLockManager.lock();

        try {
            // Now we have a lock make sure we are started.
            if (!system)
                checkStarted();

            synchronized (_connections) {
                _connections.put(newConn, newConn);
            }
        } finally {
            _connectionsLockManager.unlock();
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createConnection", newConn);

        return newConn;
    }

    HashMap<SICoreConnection, SICoreConnection> getConnections() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "getConnections");

        HashMap<SICoreConnection, SICoreConnection> connections = null;

        synchronized (_connections) {
            connections = (HashMap<SICoreConnection, SICoreConnection>) _connections
                            .clone();
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "getConnections", connections);

        return connections;
    }

    /**
     * Adds a consumer to the message procoessor's list of consumers.
     * 
     * @param consumer
     */
    void addConsumer(ConsumerSessionImpl consumer) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "addConsumer", consumer);

        _consumerList.add(consumer);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "addConsumer");
    }

    /**
     * Gets a consumer from the list of consumers on this messaging engine
     * 
     * @param id
     *            The id to find from the list.
     */
    ConsumerSessionImpl getConsumer(long id) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "getConsumer", new Long(id));

        ConsumerSessionImpl consumer = _consumerList.get(id);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "getConsumer", consumer);

        return consumer;
    }

    /**
     * Removes a consumer from the list of all consumers on this messaging
     * engine.
     * 
     * @param consumer
     */
    void removeConsumer(ConsumerSessionImpl consumer) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "removeConsumer", consumer);

        _consumerList.remove(consumer);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "removeConsumer");
    }

    public MessageStore getMessageStore() {
        return _msgStore;
    }

    // 181226 made public method for trace association
    public JsMessagingEngine getMessagingEngine() {
        return _engine;
    }

    public String getMessagingEngineName() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getMessagingEngineName");
            SibTr.exit(tc, "getMessagingEngineName", _engine.getName());
        }

        return _engine.getName();
    }

    /**
     * Gets the bus the messaging engine belongs to.
     * 
     * @return
     */
    public String getMessagingEngineBus() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "getMessagingEngineBus");
        String returnString = _engine.getBusName();
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "getMessagingEngineBus", returnString);
        return returnString;
    }

    /**
     * Gets a reference to the actual bus which this messaging engine belongs
     * to.
     * 
     * @return
     */
    public JsBus getBus() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "getBus");
        JsBus jsBus = (JsBus) _engine.getBus();
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "getBus", jsBus);
        return jsBus;
    }

    /**
     * Gets the UUID of the bus the messaging engine belongs to.
     * 
     * @return
     */
    public SIBUuid8 getMessagingEngineBusUuid() {
        JsBus jsBus = (JsBus) _engine.getBus();
        return jsBus.getUuid();
    }

    public DestinationManager getDestinationManager() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getDestinationManager");
            SibTr.exit(tc, "getDestinationManager", _destinationManager);
        }

        return _destinationManager;
    }

    @Override
    public Administrator getAdministrator() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getAdministrator");
            SibTr.exit(tc, "getAdministrator", _administrator);
        }

        return _administrator;
    }

    /**
     * getProxyHandler returns the reference to the MultiMEProxyHandler for
     * handling with proxy requests
     * 
     * @return MultiMEProxyHandler The ProxyHandler instance
     * 
     */
    public MultiMEProxyHandler getProxyHandler() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getProxyHandler");
            SibTr.exit(tc, "getProxyHandler", _multiMEProxyHandler);
        }

        return _multiMEProxyHandler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.sib.core.SICoreConnectionFactory#createConnection(java.
     * lang.String, java.lang.String, java.util.Map)
     * 
     * 174695 The null username check should occur in the sib.security package,
     * so we just pass through
     */
    @Override
    public SICoreConnection createConnection(String userName, String password,
                                             Map connectionProperties) throws SIResourceException,
                    SINotAuthorizedException, SIAuthenticationException {
        if (TraceComponent.isAnyTracingEnabled()
            && CoreSPIConnFactory.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnFactory.tc, "createConnection",
                        new Object[] { userName, connectionProperties });

        // Check that the Messaging Engine is started
        checkStarted();

        // Security context associated with logged-on principal.
        Subject subject = null;

        // Security Code changes for Liberty: Sharath Start
        try {
            subject = _authentication.login(userName, password);
        } catch (MessagingAuthenticationException e) {
            throw new SIAuthenticationException(e.getMessage());
        }
        SICoreConnection connection = createConnection(subject, connectionProperties);
        if (TraceComponent.isAnyTracingEnabled()
            && CoreSPIConnFactory.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnFactory.tc, "createConnection", connection);
        return connection;
        // Security Code changes for Liberty: Sharath End
    }

    /**
     * @see com.ibm.ws.sib.processor.SIMPFactory#createConnection(Subject,Properties)
     */
    @Override
    public SICoreConnection createConnection(Subject subject,
                                             Map connectionProperties) throws SIResourceException,
                    SINotAuthorizedException, SIAuthenticationException {

        if (TraceComponent.isAnyTracingEnabled()
            && CoreSPIConnFactory.tc.isEntryEnabled()) {
            SibTr.entry(CoreSPIConnFactory.tc, "createConnection",
                        connectionProperties);
        }

        // Check that the Messaging Engine is started
        checkStarted();

        // Security Code changes for Liberty: Sharath Start
        SICoreConnection connection = null;
        // There is no need to do a login here again, we have a subject
        // We have to just check if the Subject which we have received is authenticated or not
        try {
            if (!runtimeSecurityService.isUnauthenticated(subject)) {
                connection = createConnection(subject, false, connectionProperties);
            } else {
                //114580
                String userName = runtimeSecurityService.getUniqueUserName(subject);
                throw new SIAuthenticationException(nls.getFormattedMessage(
                                                                            "USER_NOT_AUTHENTICATED_MSE1009", new Object[] { userName },
                                                                            "User " + userName + " is not authenticated"));
            }
        } catch (MessagingAuthenticationException mae) {
            throw new SIAuthenticationException(mae.getMessage());
            //114580
        } catch (MessagingSecurityException mse) {
            throw new SIAuthenticationException(mse.getMessage());
        }
        if (TraceComponent.isAnyTracingEnabled()
            && CoreSPIConnFactory.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnFactory.tc, "createConnection", connection);
        return connection;
        // Security Code changes for Liberty: Sharath End
    }

    // Security Code changes for Liberty Comms: Sharath Start
    @Override
    public SICoreConnection createConnection(ClientConnection cc,
                                             String credentialType, String userid, String password)
                    throws SIResourceException, SINotAuthorizedException,
                    SIAuthenticationException {

        if (TraceComponent.isAnyTracingEnabled()
            && CoreSPIConnFactory.tc.isEntryEnabled()) {
            SibTr.entry(CoreSPIConnFactory.tc, "createConnection",
                        new Object[] { cc, credentialType, userid, password });
        }

        // Check that the Messaging Engine is started
        checkStarted();

        if ("".equals(userid))
            userid = null;

        SICoreConnection connection = null;
        SSLSession sslSession = cc.getMetaData().getSSLSession();

        if (credentialType.equals(TrmConstantsImpl.CREDENTIAL_USERID_PASSWORD)) {
            Certificate[] peerCerts = null;
            // Get hold of SSL credentials if userID null
            if (userid == null) {
                if (sslSession != null) {
                    try {
                        // Attempt to get hold of peer certificates
                        if (TraceComponent.isAnyTracingEnabled()
                            && tc.isDebugEnabled())
                            SibTr.debug(tc,
                                        "Authentication: attempting to retrieve SSL Peer certificates");
                        peerCerts = sslSession.getPeerCertificates();
                    } catch (SSLPeerUnverifiedException sslPUE) {
                        // No FFDC Code Needed
                        // No action needed - this will be handled by the
                        // anonymous login section below
                        if (TraceComponent.isAnyTracingEnabled()
                            && tc.isDebugEnabled())
                            SibTr.debug(tc,
                                        "Authentication: Error obtaining SSL certificates >"
                                                        + sslPUE + "<");
                    }
                }
            }
            // Call login depending on credentials provided
            if (userid != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc,
                                "Authentication: Login using user ID and password");
                connection = createConnection(userid, password, null);
            } else if ((peerCerts != null) && (peerCerts.length > 0)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc,
                                "Authentication: Login using SSL Peer certificates");
                connection = createConnection(peerCerts);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(
                                tc,
                                "Authentication: Anonymous Login using user ID and password; note that these may be null");
                connection = createConnection(userid, password, null);
            }
        }

        //mark connection  type comms/commsssl. Explicitly porting connection to ConnectionImpl as all connection in MP are off type ConnectionImpl
        if (null != sslSession)
            ((ConnectionImpl) connection).setConnectionType(SIMPConstants.MP_VIACOMMSSSL_CONNECTION);
        else
            ((ConnectionImpl) connection).setConnectionType(SIMPConstants.MP_VIACOMMS_CONNECTION);

        if (TraceComponent.isAnyTracingEnabled()
            && CoreSPIConnFactory.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnFactory.tc, "createConnection", connection);
        return connection;
    }

    /**
     * Create Connection using the certificate (used for SSL)
     * 
     * @param certificate
     * @return Connection
     * @throws SIAuthenticationException
     * @throws SIResourceException
     * @throws SINotAuthorizedException
     */
    private SICoreConnection createConnection(Certificate[] certificate) throws SIAuthenticationException, SIResourceException, SINotAuthorizedException {
        if (TraceComponent.isAnyTracingEnabled()
            && CoreSPIConnFactory.tc.isEntryEnabled()) {
            SibTr.entry(CoreSPIConnFactory.tc, "createConnection", certificate);
        }

        // Check that the Messaging Engine is started
        checkStarted();

        Subject subject = null;

        try {
            subject = _authentication.login(certificate);
        } catch (MessagingAuthenticationException e) {
            throw new SIAuthenticationException(e.getMessage());
        }
        SICoreConnection connection = createConnection(subject, null);
        if (TraceComponent.isAnyTracingEnabled()
            && CoreSPIConnFactory.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnFactory.tc, "createConnection", connection);
        return connection;
    }

    // Security Code changes for Liberty Comms: Sharath End

    /**
     * Checks that the ME is started
     * 
     * @throws SINotSupportedException
     *             if the ME isn't started
     * 
     *             180489
     */
    private void checkStarted() throws SIResourceException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "checkStarted");

        synchronized (_mpStartStopLock) {
            if (!isStarted()) {
                if (TraceComponent.isAnyTracingEnabled()
                    && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkStarted", "ME not started");

                throw new SIResourceException(nls.getFormattedMessage(
                                                                      "MESSAGE_PROCESSOR_NOT_STARTED_ERROR_CWSIP0211",
                                                                      new Object[] { getMessagingEngineName(),
                                                                                    getMessagingEngineBus() }, null));
            }
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "checkStarted");
    }

    /**
     * <p>
     * Creates a destination definition.
     * </p>
     * 
     * @return The destination definition
     */
    public DestinationDefinition createDestinationDefinition(
                                                             DestinationType destinationType, String destinationName) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createDestinationDefinition", new Object[] {
                                                                         destinationType, destinationName });

        JsAdminFactory factory = (JsAdminFactory) getSingletonInstance(SIMPConstants.JS_ADMIN_FACTORY);

        DestinationDefinition destDef = factory.createDestinationDefinition(
                                                                            destinationType, destinationName);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createDestinationDefinition", destDef);

        return destDef;
    }

    /**
     * <p>
     * Creates a localization definition.
     * </p>
     * 
     * @return The localization definition
     */
    public LocalizationDefinition createLocalizationDefinition(
                                                               String destinationName) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createLocalizationDefinition", destinationName);

        JsAdminFactory factory = (JsAdminFactory) getSingletonInstance(SIMPConstants.JS_ADMIN_FACTORY);

        LocalizationDefinition locDef = factory
                        .createLocalizationDefinition(destinationName);

        // Set the high and low message thresholds to sensible values, otherwise
        // the defaults
        // are both MAX_VALUE (which cannot be valid, and breaks any message
        // depth checks) (510343)
        locDef.setDestinationHighMsgs(getHighMessageThreshold());
        locDef.setDestinationLowMsgs((getHighMessageThreshold() / 10) * 8);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createLocalizationDefinition", locDef);

        return locDef;
    }

    /**
     * <p>
     * Creates an MQLink definition.
     * </p>
     * 
     * @return The MQLink definition
     */
    public MQLinkDefinition createMQLinkDefinition(String mqLinkUuid) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createMQLinkDefinition",
                        new Object[] { mqLinkUuid });

        JsAdminFactory factory = (JsAdminFactory) getSingletonInstance(SIMPConstants.JS_ADMIN_FACTORY);

        MQLinkDefinition mqLinkDef = factory.createMQLinkDefinition(mqLinkUuid);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createMQLinkDefinition", mqLinkDef);

        return mqLinkDef;
    }

    /**
     * return a new reference to an implementation of
     * ExceptionDestinationHandler. Accepts a name of a destination that could
     * not be delivered to OR null if there is no destination.
     * 
     * @param name
     *            - The name of the destination that could not be delivered to.
     * @return ExceptionDestinationHandlerImpl
     */
    @Override
    public ExceptionDestinationHandler createExceptionDestinationHandler(
                                                                         SIDestinationAddress address) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createExceptionDestinationHandler", address);

        ExceptionDestinationHandler handler = new ExceptionDestinationHandlerImpl(
                        address, this);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createExceptionDestinationHandler", handler);

        return handler;
    }

    /**
     * return a new reference to an implementation of
     * ExceptionDestinationHandler. Accepts a name of a destination that could
     * not be delivered to OR null if there is no destination.
     * 
     * @param name
     *            - The name of the destination that could not be delivered to.
     * @return ExceptionDestinationHandlerImpl
     */
    @Override
    public ExceptionDestinationHandler createLinkExceptionDestinationHandler(
                                                                             SIBUuid8 mqLinkUuid) throws SIException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr
                            .entry(tc, "createLinkExceptionDestinationHandler",
                                   mqLinkUuid);

        MQLinkHandler mqLink = _destinationManager.getMQLinkLocalization(
                                                                         mqLinkUuid, true);

        ExceptionDestinationHandler handler = new ExceptionDestinationHandlerImpl(
                        mqLink);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createLinkExceptionDestinationHandler", handler);

        return handler;
    }

    /**
     * Get an singleton factory from the list of stored factories created at
     * initialise time.
     * 
     * @param className
     *            - The class for which factory instance we want
     * @return The singleton instance
     */

    public static Object getSingletonInstance(String className) {
        return _factories.get(className);
    }

    /**
     * Get an factory per ME from the list of stored factories created at
     * initialise time.
     * 
     * @param className
     *            - The class for which factory instance we want
     * @return The instance fopr this ME
     */
    public Object getMEInstance(String className) {
        return _meFactories.get(className);
    }

    /**
     * Initialises the MessageProcessor class. Called by the
     * JsEngineComponent.initialize
     * 
     * @param engine
     *            The JsMessagingEngine for this MessageProcessor.
     */
    @Override
    public void initialize(JsMessagingEngine engine) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "initialize", engine);

        this._engine = engine;

        //Venu mock mock
        //This is needed for MELockOwner in PersistentMessageStoreImpl' initialize. However the MELockOwer object not really relevant
        // as uuid check is disabled in PersistentMessageStoreImpl
        // In case of warm start, after reconstiturion _engine Messaging Engine Uuid is reset from restored Uuid
        this._engine.setMEUUID(new SIBUuid8());

        _mpio = new MPIO(this);

        _meFactories = new HashMap<String, Object>();
        try {
            _meFactories.put(SIMPConstants.JS_MBEAN_FACTORY, engine.getMBeanFactory());
        } catch (Exception e) {

            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.MessageProcessor.initialize",
                                        "1:1202:1.445",
                                        this);

            SIErrorException finalE =
                            new SIErrorException(
                                            nls.getFormattedMessage(
                                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                    new Object[] { "com.ibm.ws.sib.processor.impl.MessageProcessor", "1:1209:1.445", e },
                                                                    null),
                                            e);

            SibTr.exception(tc, finalE);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] { "com.ibm.ws.sib.processor.impl.MessageProcessor", "1:1215:1.445", SIMPUtils.getStackTrace(e) });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "initialize", finalE);

            throw finalE;

        }

        // Obtain instances of all singleton factories to be used in mp.
        // Initialize is the only place we can get the instance and handle
        // and exceptions correctly.

        if (_factories == null) {
            _factories = new HashMap<String, Object>();
            try {
                _factories.put(SIMPConstants.JS_DESTINATION_ADDRESS_FACTORY,
                               JsMainAdminComponentImpl.getSIDestinationAddressFactory());
                _factories.put(SIMPConstants.SI_DESTINATION_ADDRESS_FACTORY,
                               JsMainAdminComponentImpl.getSIDestinationAddressFactory());
                _factories.put(SIMPConstants.CONTROL_MESSAGE_FACTORY,
                               ControlMessageFactory.getInstance());
                _factories.put(SIMPConstants.JS_ADMIN_FACTORY, JsAdminFactory
                                .getInstance());

                // Matching instance
                _factories.put(SIMPConstants.MATCHING_INSTANCE, Matching
                                .getInstance());

                // SelectionCriteria
                _factories.put(SIMPConstants.JS_SELECTION_CRITERIA_FACTORY,
                               JsMainAdminComponentImpl.getSelectionCriteriaFactory());

                _factories.put(SIMPConstants.JS_MESSAGE_HANDLE_FACTORY,
                               JsMessageHandleFactory.getInstance());
            } catch (Exception e) {
                // FFDC
                FFDCFilter
                                .processException(
                                                  e,
                                                  "com.ibm.ws.sib.processor.impl.MessageProcessor.initialize",
                                                  "1:1261:1.445", this);

                SIErrorException finalE = new SIErrorException(
                                nls
                                                .getFormattedMessage(
                                                                     "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                     new Object[] {
                                                                                   "com.ibm.ws.sib.processor.impl.MessageProcessor",
                                                                                   "1:1268:1.445", e }, null), e);

                SibTr.exception(tc, finalE);
                SibTr
                                .error(
                                       tc,
                                       "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                       new Object[] {
                                                     "com.ibm.ws.sib.processor.impl.MessageProcessor",
                                                     "1:1274:1.445",
                                                     SIMPUtils.getStackTrace(e) });

                if (TraceComponent.isAnyTracingEnabled()
                    && tc.isEntryEnabled())
                    SibTr.exit(tc, "initialize", finalE);

                throw finalE;
            }
        }
        // Determine if messages on deleted destinations should be discarded
        // _discardMsgsAfterQueueDeletion =
        // (_engine.getBus()).getBoolean(CT_SIBus.DISCARDMSGSAFTERQUEUEDELETION_NAME,
        // CT_SIBus.DISCARDMSGSAFTERQUEUEDELETION_DEFAULT);

        // Venu temp
        // this property is not configured in Libery profile and Admin has not
        // exposed this...
        // so hardcoding to false.
        _discardMsgsAfterQueueDeletion = false;

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "initialize");
    }

    /**
     * @see com.ibm.ws.sib.admin.JsEngineComponent#start()
     */
    @Override
    public void start(int startMode) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "start");

        try {
            synchronized (_mpStartStopLock) {
                _starting = true;
            }

            //lohith liberty change
            //_msgStore.start();
            // Has XD registered a MessagingEngineControlListener
            //lohith liberty change
            /*
             * _meControlListener = registerMEControlListener();
             * 
             * // If we have a listener, notify it that the ME has started
             * if (_meControlListener != null) {
             * // Instantiate a new control object that XD can call
             * _messagingEngineControl = new MessagingEngineControlImpl(this);
             * 
             * // Get reference to the MessageController object
             * if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
             * SibTr.debug(tc, "Calling registerMEController ",
             * new Object[] { _messagingEngineControl,
             * getMessagingEngineBus(),
             * getMessagingEngineName() });
             * 
             * _messageController = _meControlListener.registerMEController(
             * _messagingEngineControl, getMessagingEngineBus(),
             * getMessagingEngineName());
             * 
             * if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
             * SibTr.debug(tc, "Returned from registerMEController with",
             * _messageController);
             * 
             * // If XD returned a MessageController object, then drive
             * // notifyMEStarting against it.
             * if (_messageController != null) {
             * // Assign consumer to a consumer set
             * if (TraceComponent.isAnyTracingEnabled()
             * && tc.isDebugEnabled())
             * SibTr.debug(tc, "Calling notifyMEStarting ");
             * 
             * _messageController.notifyMEStarting();
             * 
             * if (TraceComponent.isAnyTracingEnabled()
             * && tc.isDebugEnabled())
             * SibTr.debug(tc, "Returned from notifyMEStarting");
             * }
             * }
             */
            startInternal(startMode);
        } catch (Exception e) {
            FFDCFilter.processException(e,
                                        "com.ibm.ws.sib.processor.impl.MessageProcessor.start",
                                        "1:1351:1.445", this);

            // Stop all the threads that have been started
            _stoppableThreadCache.stopAllThreads();

            /*
             * At the moment any exception which reaches the very top level of
             * MP start (this method) will be treated as one which is serious is
             * enough to signal HA that the ME should be shutdown and not
             * restarted elsewhere without manual intervention to fix the
             * problem. In HA parlance, it is a global failure.
             * 
             * However, it is possible that in some future time some failures
             * could be considered local (ones which could be fixed by restart
             * the ME on another WAS). At which point there can be another catch
             * block in this try block.
             * 
             * We catch both ordinary and runtime exceptions. A runtime
             * exception such as an NPE should cause the ME to fail without
             * failover (which could stop other application servers in the
             * cluster. This is identically to the old behaviour caused by
             * throwing out runtime exceptions. It is also the case that some
             * persistence problems could end up throwing runtime exceptions
             * (such as MessageStoreRuntimeException if one of their callbacks
             * to us throws an exception).
             */
            /*
             * JsHealthMonitor hm = (JsHealthMonitor) _engine;
             * hm.reportGlobalError();
             */

            SIErrorException finalE = new SIErrorException(
                            nls
                                            .getFormattedMessage(
                                                                 "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                 new Object[] {
                                                                               "com.ibm.ws.sib.prWsWsocessor.impl.MessageProcessor",
                                                                               "1:1386:1.445", e }, null), e);

            // Log to console
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002", new Object[] {
                                                                                "com.ibm.ws.sib.processor.impl.MessageProcessor",
                                                                                "1:1394:1.445", SIMPUtils.getStackTrace(e) });

            SibTr.exception(tc, finalE);

            // Mark that the server hasn't started
            synchronized (_mpStartStopLock) {
                _started = false;
            }

            if (TraceComponent.isAnyTracingEnabled()
                && tc.isEntryEnabled())
                SibTr.exit(tc, "start", finalE);

            // We used to throw the exception out to admin to halt startup. But
            // according to ha this should now do absolutely nothing now that MP
            // is
            // being started asynchronously by HA (as opposed to synchronously
            // before)
            //
            // However, we will keep throwing in case we need to revert to
            // synchronous
            // startup (not an impossibility). Also, it makes it easier to
            // diagnose
            // code problems in unit testing.
            throw finalE;
        } finally {
            synchronized (_mpStartStopLock) {
                _starting = false;
                _mpStartStopLock.notify();
            }
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "start");
    }

    /**
     * This is where start things actually happen.
     * 
     * @throws MessageStoreException
     */
    private void startInternal(int startMode) throws MessageStoreException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "startInternal", new Integer(startMode));

        /**
         * The following code has been moved from initialize under defect 233412
         * as the PMI stats config needs to be refreshed in start() following a
         * failover restart.
         */

        // lohith liberty change

        // Removing PMI related classses

        /*
         * // register SIB.processor component's StatsTemplateLookup class
         * try {
         * Class lookupClass = Class
         * .forName("com.ibm.ws.pmi.preprocess.sib_processor_impl_StatsTemplateLookup");
         * 
         * 
         * 
         * StatsFactory .registerStatsTemplateLookup((StatsTemplateLookup)
         * lookupClass .newInstance());
         * 
         * } catch (Exception e) {
         * // FFDC
         * FFDCFilter
         * .processException(
         * e,
         * "com.ibm.ws.sib.processor.impl.MessageProcessor.startInternal",
         * "1:1470:1.445", this);
         * 
         * SIErrorException finalE = new SIErrorException(
         * nls
         * .getFormattedMessage(
         * "INTERNAL_MESSAGING_ERROR_CWSIP0002",
         * new Object[] {
         * "com.ibm.ws.sib.processor.impl.MessageProcessor",
         * "1:1477:1.445", e }, null), e);
         * 
         * SibTr.exception(tc, finalE);
         * SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002", new Object[] {
         * "com.ibm.ws.sib.processor.impl.MessageProcessor",
         * "1:1483:1.445", SIMPUtils.getStackTrace(e) });
         * 
         * if (TraceComponent.isAnyTracingEnabled()
         * && tc.isEntryEnabled())
         * SibTr.exit(tc, "startInternal", finalE);
         * 
         * throw finalE;
         * }
         */

        /*
         * The following code has been moved from initialise under defect
         * 170161.1 as a change to the startup order means we cannot use the
         * message store until it has been driven through its start() method.
         */

        _msgStore = (MessageStore) _engine.getMessageStore();
        _txManager = new SIMPTransactionManager(this, _msgStore);

        _mpAlarmManager = new MPAlarmManager(this,
                        SIMPConstants.MPAM_PERCENT_LATE,
                        SIMPConstants.MPAM_ALARM_POOL_SIZE);

        // create the batch handlers
        _publicationBatchHandler = new BatchHandler(
                        SIMPConstants.PUBLICATION_BATCH_SIZE,
                        SIMPConstants.PUBLICATION_BATCH_TIMEOUT, _txManager,
                        _mpAlarmManager);
        _sourceBatchHandler = new BatchHandler(_customProperties
                        .get_source_batch_size(), _customProperties
                        .get_source_batch_timeout(), _txManager, _mpAlarmManager);
        _targetBatchHandler = new BatchHandler(_customProperties
                        .get_target_batch_size(), _customProperties
                        .get_target_batch_timeout(), _txManager, _mpAlarmManager);

        // Create the access checker for destination authorization
        String theBus = _engine.getBusName();

        // Create the MP access checker
        _accessChecker = new AccessChecker(this);

        // Security Code changes for Liberty: Sharath Start
        _authentication = runtimeSecurityService.getAuthenticationInstance();
        _authorization = runtimeSecurityService.getAuthorizationInstance();
        // Security Code changes for Liberty: Sharath End

        // Set the security enablement flag
        _isBusSecure = _authorisationUtils.isBusSecure(getMessagingEngineBus());

        // Create a new Object Pool for the search results objects.
        _searchResultsPool = new SearchResultsObjectPool(
                        "MatchingSearchResults", _customProperties
                                        .get_searchResultsPoolSize());

        // Set up selection criteria factory
        _selectionCriteriaFactory = (SelectionCriteriaFactory) getSingletonInstance(SIMPConstants.JS_SELECTION_CRITERIA_FACTORY);

        _connections = new HashMap<SICoreConnection, SICoreConnection>();

        /*
         * Recover persistent state information or create structure if
         * necessary. Feature 174199.2.1
         */
        boolean warmStarted = reconstitute(startMode);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isDebugEnabled())
            SibTr.debug(tc, "Warm start state : " + warmStarted);

        if (!warmStarted) {
            //Venu mock mock
            //Create ME uuid
            this._engine.setMEUUID(new SIBUuid8());

            coldStart();
        } else {

            //Venu  mock mock
            this._engine.setMEUUID(getMessagingEngineUuid());

            // alert the destinationManager.that we are in the reconcile
            // phase of startup. This allows the manager to build lists of
            // destinations that will
            // need to be deleted.
            _destinationManager.prepareToReconcile();
        }

        if (_linkManager == null) {
            // Venu liberty change.
            // For this verion of Liberty profile. no TrmMeMain. Hence removing the code
            // and making linkManager as NULL
            // try again to get the link manager
            _linkManager = null;
        }
        if (_linkManager != null)
            _linkManager.setChangeListener(_linkChangeListener);
        else {
            if (TraceComponent.isAnyTracingEnabled()
                && tc.isDebugEnabled())
                SibTr.debug(tc, "Link Manager is null");
        }

        //Venu Liberty change:
        // Now Admin wil not be having List of localisation to a destination
        //For this version of Liberty profile, it is this ME.
        _localistySet.add(_engine.getUuid().toString());

        // Load Admin/WCCM defined destinations
        _engine.loadLocalizations();

        /*
         * Tick generator has to be initialised after
         * 
         * 1. Any previously persistently stored tick is available. 2 An
         * initially cold started persistent store is added to the msgstore.
         * This is because the tick generator immediately persists its tick
         * value when it is created.
         * 
         * Outside of these restrictions, we can initialise independently of
         * other objects in the start sequence.
         * 
         * Feature 174199.2.11
         */
        initializeTickGenerator();

        // create the temporary destination receiver
        try {
            _tdReceiverAddr = SIMPUtils.createJsSystemDestinationAddress(
                                                                         SIMPConstants.TDRECEIVER_SYSTEM_DESTINATION_PREFIX,
                                                                         getMessagingEngineUuid());
            _tdReceiverAddr.setBusName(getMessagingEngineBus());
            if (!_destinationManager.destinationExists(_tdReceiverAddr)) {
                _tdReceiverAddr = _destinationManager
                                .createSystemDestination(SIMPConstants.TDRECEIVER_SYSTEM_DESTINATION_PREFIX);
            }
        } catch (SIException e) {
            FFDCFilter
                            .processException(
                                              e,
                                              "com.ibm.ws.sib.processor.impl.MessageProcessor.startInternal",
                                              "1:1615:1.445", this);

            // SIException will occur if there's some problem creating the
            // temporary destination receiver.
            SIErrorException finalE = new SIErrorException(
                            nls
                                            .getFormattedMessage(
                                                                 "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                 new Object[] {
                                                                               "com.ibm.ws.sib.processor.impl.MessageProcessor",
                                                                               "1:1626:1.445", e }, null), e);

            SibTr.exception(tc, finalE);

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002", new Object[] {
                                                                                "com.ibm.ws.sib.processor.impl.MessageProcessor",
                                                                                "1:1636:1.445", SIMPUtils.getStackTrace(e) });

            if (TraceComponent.isAnyTracingEnabled()
                && tc.isEntryEnabled())
                SibTr.exit(tc, "startInternal", finalE);

            throw finalE;
        }

        // Create the dynamic config manager
        createDynamicConfigManager();

        if (warmStarted) {
            // Write to console that reconciliation has started
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Messaging engine " + getMessagingEngineName() + " is starting to reconcile the destination.");

            // First check that it is safe to delete unreconciled destinations
            _destinationManager.validateUnreconciled();
            // Reconcile WCCM and MP.reconstituted data for local destinations
            _destinationManager.reconcileLocal();

            // Venu mock mock .. no reconcileMQLinks, no reconcileLocalLinks, no reconcileMQLinkPubSubBridgeQs
            // no reconcileRemote, no reconcileRemoteTemporary, no  reconcileRemoteLinks
            // for this version of Liberty profile
            /*
             * // Reconcile WCCM and MP.reconstituted data for mqlinks
             * _destinationManager.reconcileMQLinks();
             * 
             * // Reconcile WCCM and MP.reconstituted data for local links
             * _destinationManager.reconcileLocalLinks();
             * 
             * // Reconcile any system destinations and topicspaces no longer
             * // required by
             * // the MQ pubsub bridge
             * _destinationManager.reconcileMQLinkPubSubBridgeQs();
             * 
             * // Reconcile WCCM and MP.reconstituted data for remote destinations
             * _destinationManager.reconcileRemote();
             * 
             * // Reconcile WCCM and MP.reconstituted data for remote temporary
             * // destinations
             * _destinationManager.reconcileRemoteTemporary();
             * 
             * // Reconcile WCCM and MP.reconstituted data for remote links
             * _destinationManager.reconcileRemoteLinks();
             */

            // Write to console that reconciliation has completed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Messaging engine " + getMessagingEngineName() + " has finished reconciling the destination.");

            // Spin off an asynchronous deletion thread to clean up
            _destinationManager.start();
        }

        // Venu mock mock ...
        // For now dont call _multiMEProxyHandler.initalised();

        /*
         * // Indicate that this has now started.
         * try {
         * _multiMEProxyHandler.initalised();
         * } catch (SIResourceException e1) {
         * // No FFDC code needed
         * if (TraceComponent.isAnyTracingEnabled()
         * && tc.isEntryEnabled())
         * SibTr.exit(tc, "startInternal", "SIErrorException");
         * throw new SIErrorException(e1);
         * }
         */

        // Start the expirer as the very last startup action so that we can be
        // sure
        // we have everything required to process an expiration set up.
        _msgStore.expirerStart();

        _msgStore.deliveryDelayManagerStart();

        synchronized (_mpStartStopLock) {
            _started = true;
        }

        // They will wait until WAS has also started before continuing into the
        // started or stopped state.
        _destinationManager.announceMPStarted(startMode);

        // If security is enabled then we'll load discriminator ACLs
        if (_isBusSecure) {
            _accessChecker.listTopicAuthorisations();
        }

        // Create the MulticastProperties object if multicast is enabled
        if (_customProperties.get_multicastEnabled())
            _multicastProperties = new MulticastPropertiesImpl(
                            _customProperties.get_multicastInterfaceAddress(),
                            _customProperties.get_multicastPort(), _customProperties
                                            .get_multicastPacketSize(), _customProperties
                                            .get_multicastTTL(), _customProperties
                                            .get_multicastGroupAddress(), _customProperties
                                            .get_multicastUseReliableRMM());

        // If the WAS started event occured before MP was started, then call it
        // now
        synchronized (_mpStartStopLock) {
            if (isWASOpenForEBusiness())
                serverStarted();
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "startInternal");
    }

    /**
     * MP is warning everyone that it is about to stop.
     * <p>
     * Mediations should stop as a result.
     */
    private void announceMPStopping() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "announceMPStopping");

        if (_destinationManager != null)
            _destinationManager.announceMPStopping();

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "announceMPStopping");
    }

    /**
     * Indicates whether WAS has announced whether it is open for e-business or
     * not yet.
     */
    private volatile boolean _isWASOpenForEBusiness = false;

    /**
     * Allows the mediations to ask whether WAS has announced that it is open
     * for e-business or not yet.
     * 
     * @return
     */
    public boolean isWASOpenForEBusiness() {
        return _isWASOpenForEBusiness;
    }

    @Override
    public void destroy() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "destroy");

        // Drive the DestinationManager destroy() method
        if (_destinationManager != null)
            _destinationManager.destroy();

        // 189890
        if (_consumerThreadPool != null) {
            _consumerThreadPool.shutdownNow();
            _consumerThreadPool = null;
        }

        if (_systemThreadPool != null) {
            _systemThreadPool.shutdownNow();
            _systemThreadPool = null;
        }
        _factories = null;

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "destroy");
    }

    /**
     * Stops the message processor
     * 
     * Implements the stop method in the JsEngineComponent
     * 
     * @param mode
     *            The mode that stop has been issued
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent.stop
     */
    @Override
    public void stop(int mode) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "stop", new Integer(mode));

        try {
            // Check whether the stop is from COMMS server to stop MP
            // connection. In that case just call closeConnection and return
            if (mode == JsConstants.ME_STOP_COMMS_CONNECTIONS) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc,
                                "Stop request from Comms server for COMMS connections");
                closeConnections(JsConstants.ME_STOP_QUIESCE,
                                 SIMPConstants.MP_VIACOMMS_CONNECTION);
                return;
            }

            if (mode == JsConstants.ME_STOP_COMMS_SSL_CONNECTIONS) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc,
                                "Stop request from Comms server for COMMS SSL connections");
                closeConnections(JsConstants.ME_STOP_QUIESCE,
                                 SIMPConstants.MP_VIACOMMSSSL_CONNECTION);
                return;
            }
        } catch (Exception e) {
            //Absorb the exception and log in trace
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                if (mode == JsConstants.ME_STOP_COMMS_CONNECTIONS)
                    SibTr.debug(tc, "Failure in closing COMMS connections", e);
                else
                    SibTr.debug(tc, "Failure in closing COMMS over SSL connections", e);

            }
            return;
        }

        try {
            // Tell everything we control that MP is stopping now.
            announceMPStopping();

            // Block all new create Connection calls and all calls to
            // connection.close by obtaining the exclusive lock
            _connectionsLockManager.lockExclusive();

            synchronized (_mpStartStopLock) {
                if (_starting) {
                    if (TraceComponent.isAnyTracingEnabled()
                        && tc.isDebugEnabled())
                        SibTr.debug(tc, "Stop waiting until start complete");

                    try {
                        _mpStartStopLock.wait();
                    } catch (InterruptedException e) {
                        // No FFDC code needed
                        if (TraceComponent
                                        .isAnyTracingEnabled()
                            && tc.isDebugEnabled())
                            SibTr.exception(tc, e);
                    }
                }

                if (!_started) {
                    if (TraceComponent.isAnyTracingEnabled()
                        && tc.isEntryEnabled())
                        SibTr.exit(tc, "stop", "Returning as not started.");
                    return;
                }

                _started = false;

                // Stop the mpio to stop processing any more remote messages.
                _mpio.stop();

                // Stop all the threads that have been started
                _stoppableThreadCache.stopAllThreads();

                // Stop expiry first - while stopping we don't want any messages
                // to
                // expire.
                _msgStore.expirerStop();

                _msgStore.deliveryDelayManagerStop();

                // If the destination manager was not found at startup, then
                // this
                // object will be null.
                if (_destinationManager == null) {
                    if (TraceComponent.isAnyTracingEnabled()
                        && tc.isEntryEnabled())
                        SibTr.exit(tc, "stop", "Null DM");
                    return;
                }

                _destinationManager.stop(mode);

                //close all in-process connections .
                closeConnections(mode, SIMPConstants.MP_INPROCESS_CONNECTION);

                // Stop the administrator
                _administrator.stop();

                // Tell MFP that the message store is about to be stopped.
                SchemaStore.messageStoreStoppingNotify(_msgStore);

            } // end sync
        } finally {
            _connectionsLockManager.unlockExclusive();
            // _persistentStore=null is not needed as during reconstitute it is created from messagestore store as a fresh object.
            if (_consumerThreadPool != null) {
                _consumerThreadPool.shutdownNow();
                _consumerThreadPool = null;
            }
            if (_systemThreadPool != null) {
                _systemThreadPool.shutdownNow();
                _systemThreadPool = null;
            }

        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "stop");
    }

    /**
     ** Closes SICoreConnection (i.e ConnectionImpl).. </br>
     ** if MP_INPROCESS_CONNECTION : closes all ConnectionImpl </br>
     ** if MP_VIACOMMS_CONNECTION : closes ConnectionImpl which are connected via comms </br>
     ** if MP_VIACOMMSSSL_CONNECTION : closes ConnectionImpl which are connection vias comms SSL. </br>
     * 
     ** No harm in case if the same connections are closed by multiple components...
     */
    private void closeConnections(int mode, int connectionType) {

        //ME_STOP_QUIESCE is called only from unit test. ME_STOP_QUIESCE can be removed.
        if (mode == JsConstants.ME_STOP_QUIESCE) {
            // now we do the business of each connection
            Iterator<SICoreConnection> connectionIterator = getConnections()
                            .keySet().iterator();
            while (connectionIterator.hasNext()) {
                // quiesce for each listener
                ConnectionImpl currentConnection = (ConnectionImpl) connectionIterator
                                .next();
                try {

                    if (((connectionType == SIMPConstants.MP_VIACOMMS_CONNECTION) && (currentConnection.getConnectionType() == SIMPConstants.MP_VIACOMMS_CONNECTION))
                        ||
                        ((connectionType == SIMPConstants.MP_VIACOMMSSSL_CONNECTION) && (currentConnection.getConnectionType() == SIMPConstants.MP_VIACOMMSSSL_CONNECTION))
                        ||
                        ((connectionType == SIMPConstants.MP_INPROCESS_CONNECTION) && (currentConnection.getConnectionType() == SIMPConstants.MP_INPROCESS_CONNECTION)))
                    {
                        SICoreConnectionListener[] listeners = currentConnection
                                        .getConnectionListeners();
                        for (int listenIndex = 0; listenIndex < listeners.length; listenIndex++) {
                            listeners[listenIndex]
                                            .meQuiescing(currentConnection);
                        }

                        // now stop this connection
                        currentConnection._close(false);

                        // now register this stop on each listener
                        listeners = currentConnection
                                        .getConnectionListeners();
                        for (int listenIndex = 0; listenIndex < listeners.length; listenIndex++) {
                            listeners[listenIndex]
                                            .meTerminated(currentConnection);
                            // Remove the connection listener from the list.
                            currentConnection
                                            .removeConnectionListener(listeners[listenIndex]);
                        }
                        //now stop this connection
                        //PM72328.dev: Close connection after ME terminated set on connection listeners 
                        currentConnection._close(false);
                    }

                } catch (SIMPConnectionUnavailableException exception) {
                    // No FFDC code needed
                } catch (SIException coreException) {
                    FFDCFilter
                                    .processException(
                                                      coreException,
                                                      "com.ibm.ws.sib.processor.impl.MessageProcessor.stop",
                                                      "1:1938:1.445", this);
                }

            } // end while
        } // end if
        else {
            // now we do the business of each connection
            Iterator<SICoreConnection> connectionIterator = getConnections()
                            .keySet().iterator();
            while (connectionIterator.hasNext()) {
                // stop for each listener
                ConnectionImpl currentConnection = (ConnectionImpl) connectionIterator
                                .next();

                // stop this connection depending on connectinType.
                try {
                    if (((connectionType == SIMPConstants.MP_VIACOMMS_CONNECTION) && (currentConnection.getConnectionType() == SIMPConstants.MP_VIACOMMS_CONNECTION))
                        ||
                        ((connectionType == SIMPConstants.MP_VIACOMMSSSL_CONNECTION) && (currentConnection.getConnectionType() == SIMPConstants.MP_VIACOMMSSSL_CONNECTION))
                        ||
                        ((connectionType == SIMPConstants.MP_INPROCESS_CONNECTION) && (currentConnection.getConnectionType() == SIMPConstants.MP_INPROCESS_CONNECTION)))
                    {
                        currentConnection._close(false);

                        // now register this stop on each listener
                        SICoreConnectionListener[] listeners = currentConnection
                                        .getConnectionListeners();
                        for (int listenIndex = 0; listenIndex < listeners.length; listenIndex++) {
                            listeners[listenIndex]
                                            .meTerminated(currentConnection);
                            // Remove the connection listener from the list.
                            currentConnection
                                            .removeConnectionListener(listeners[listenIndex]);
                        }
                    }

                } catch (SIMPConnectionUnavailableException exception) {
                    // No FFDC code needed
                } catch (SIException coreException) {
                    FFDCFilter
                                    .processException(
                                                      coreException,
                                                      "com.ibm.ws.sib.processor.impl.MessageProcessor.stop",
                                                      "1:1976:1.445", this);
                }
            }
        } // end if
    }

    /**
     * Specific cold start procedure for the Message Processor.
     */
    private void coldStart() throws MessageStoreException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "coldStart");

        /*
         * Cold starting. Everything here needs to be started if we aren't
         * retrieving previous destinations/state from the message store.
         */
        createPersistentStore();

        createDestinationManager();

        _proxyHandlerDestAddr = SIMPUtils.createJsSystemDestinationAddress(
                                                                           SIMPConstants.PROXY_SYSTEM_DESTINATION_PREFIX,
                                                                           getMessagingEngineUuid());

        createProxyHandler();

        // Connection creation requires a DestinationManager

        //Venu Liberty change: for mockup comment the below line to avoid Security classes loading
        //createSystemConnection();

        initializeNonPersistent();

        // D261769 - The destinationChangeListener needs to be created before
        // any AIH
        // is recreated, and as the destinationLocationManager is only avaiable
        // after
        // initializeNonPersistent we have to place this code here for
        // coldStarts
        // Before loading localizations, start listening to changes to the sets
        // of messaging
        // engines localizing destinations
        _destinationChangeListener = new DestinationChangeListener(this);
        _linkChangeListener = new LinkChangeListener(this);

        //Venu Liberty change. No _destinationLocationManager 
        //_destinationLocationManager.setChangeListener(_destinationChangeListener);

        createControlAdapter();

        //Venu Liberty change: for mockup, no proxy as admin is yet to define them
        //createProxySystemDestination();

        configureNeighbours();

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "coldStart");
    }

    /**
     * Initialize non-persistent fields and objects. These are common to both
     * warm and cold start. This function should only be called after persistent
     * values have either been recovered or initially created.
     * <p>
     * Feature 174199.2.8
     */
    private void initializeNonPersistent() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "initializeNonPersistent");

        // Initialise MatchSpace
        _messageProcessorMatching = new MessageProcessorMatching(this);

        // Create the access checker for discriminator authorization
        _topicAuthorization = new TopicAuthorization(this);

        _searchResultsPool.setTopicAuthorization(_topicAuthorization);

        // set the authorizer into the general MP Access Checker
        _accessChecker.setTopicAuthorization(_topicAuthorization);

        // Create the administrator
        // Needs to be done before D3 initialise as it is used by D3.
        // Needs to be done after a DestinationManager has been recovered/setup
        _administrator = new AdministratorImpl(this);

        // Venu Liberty change
        //making all TRM related objects to NULL
        // These objects all togehter can have been removed, however as the decission had been take to 
        // keep ME-ME communication, retaining them so that ME-ME related artifacts would get built
        /*
         * // 175358 Locate TRM class using JsConstants
         * TrmMeMain trm = (TrmMeMain) _engine
         * .getEngineComponent(JsConstants.SIB_CLASS_TO_ENGINE);
         * 
         * if (trm == null) {
         * if (TraceComponent.isAnyTracingEnabled()
         * && tc.isEntryEnabled())
         * SibTr.exit(tc, "initializeNonPersistent", "WsRuntimeException");
         * 
         * // Log error to console
         * SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001", new Object[] {
         * "com.ibm.ws.sib.processor.impl.MessageProcessor",
         * "1:2117:1.445" });
         * 
         * throw new SIErrorException(nls.getFormattedMessage(
         * "INTERNAL_MESSAGING_ERROR_CWSIP0001", new Object[] {
         * "com.ibm.ws.sib.processor.impl.MessageProcessor",
         * "1:2122:1.445" }, null));
         * }
         * 
         * _destinationLocationManager = trm.getDestinationLocationManager();
         * _linkManager = trm.getLinkManager();
         * _mqLinkManager = trm.getMQLinkManager();
         * RoutingManager routingManager = trm.getRoutingManager();
         * CommsErrorListener errorListener = trm.getCommsErrorListener();
         */
        _destinationLocationManager = null;
        _linkManager = null;
        _mqLinkManager = null;
        RoutingManager routingManager = null;
        CommsErrorListener errorListener = null;

        _mpio.init(errorListener, routingManager);

        try {
            // Venu Liberty change:
            //commenting below line to avoid PMI
            //_rmImpl = SIBPmiRm.getInstance();
        } catch (Exception e) {
            FFDCFilter
                            .processException(
                                              e,
                                              "com.ibm.ws.sib.processor.impl.MessageProcessor.initializeNonPersistent",
                                              "1:2142:1.445", this);

            SIErrorException e2 = new SIErrorException(nls.getFormattedMessage(
                                                                               "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                               new Object[] {
                                                                                             "com.ibm.ws.sib.processor.impl.MessageProcessor",
                                                                                             "1:2150:1.445", e }, null), e);

            SibTr.exception(tc, e2);

            if (TraceComponent.isAnyTracingEnabled()
                && tc.isEntryEnabled())
                SibTr.exit(tc, "initializeNonPersistent", e2);

            throw e2;
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "initializeNonPersistent");
    }

    /**
     * Loads the Neighbours from Admin
     * 
     * @return
     */
    private void configureNeighbours() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "configureNeighbours");

        try {
            JsAdminService service = JsMainAdminComponentImpl.getJsAdminService();

            // Check that the admin service is initialized before calling it.
            // Tell TRM the set of messaging engines on the bus
            _messagingEnginesOnBus = new HashSet<SIBUuid8>();
            // Add this messaging engine to the list.
            // messagingEnginesOnBus.add(getMessagingEngineUuid());

            Set mes = null;

            // Check that the admin service is initialized before calling it.
            if (service.isInitialized())
                mes = service.getMessagingEngineSet(_engine.getBusName());

            if (mes != null) {
                Iterator iterator = mes.iterator();

                // Create a transaction for creating the Neighbours with.
                LocalTransaction transaction = _txManager
                                .createLocalTransaction(true);

                // Cycle through the list of messaging engines.
                while (iterator.hasNext()) {
                    SIBUuid8 jsEngineUUID = new SIBUuid8((String) iterator
                                    .next());

                    if (!jsEngineUUID.equals(new SIBUuid8(_engine.getUuid()))) {
                        // Create the Neighbour
                        _multiMEProxyHandler
                                        .createNeighbour(jsEngineUUID, _engine
                                                        .getBusName(),
                                                         (Transaction) transaction);
                    }

                    _messagingEnginesOnBus.add(jsEngineUUID);
                }

                transaction.commit();
            }
        } catch (SIException e) {
            // FFDC
            FFDCFilter
                            .processException(
                                              e,
                                              "com.ibm.ws.sib.processor.impl.MessageProcessor.configureNeighbours",
                                              "1:2223:1.445", this);

            SibTr.exception(tc, e);

            SIErrorException finalE = new SIErrorException(
                            nls
                                            .getFormattedMessage(
                                                                 "MESSAGE_PROCESSOR_CONFIGURE_NEIGHBOURS_ERROR_CWSIP0391",
                                                                 new Object[] { getMessagingEngineName(), e },
                                                                 null), e);

            if (TraceComponent.isAnyTracingEnabled()
                && tc.isEntryEnabled())
                SibTr.exit(tc, "configureNeighbours", finalE);

            SibTr.error(tc,
                        "MESSAGE_PROCESSOR_CONFIGURE_NEIGHBOURS_ERROR_CWSIP0391",
                        new Object[] { getMessagingEngineName(),
                                      SIMPUtils.getStackTrace(e) });

            throw finalE;
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "configureNeighbours");
    }

    /**
     * Reconstitute the Message Processor from state stored in the MessageStore
     * (warm restart).
     * <p>
     * This involves,
     * 
     * 1. Reconstituting MessageProcessor's persisted state information.
     * 
     * 2. Initializing non-persistent MessageProcessor fields and objects.
     * 
     * 3. Travelling down the tree structure of ItemStreams, retrieving and
     * reconstituting objects as appropriate (Destination Manager, Destination
     * Handlers, etc.)
     * <p>
     * Feature 174199.2.4
     * <p>
     * 
     * @return true if reconstitution successful, false if nothing existed to
     *         reconstitute or there was a fatal error while reconstituting.
     */
    private boolean reconstitute(int startMode) throws MessageStoreException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "reconstitute", new Integer(startMode));

        boolean reconstituted = false;

        /*
         * 174199.2
         * 
         * If you want to force the Message Processor to ignore existing
         * database information on restart for debugging purposes, comment out
         * this line.
         * 
         * Commenting out this line will not delete previous database
         * information, but will rather store a second persistent data
         * structure.
         */
        _persistentStore = (MessageProcessorStore) _msgStore
                        .findFirstMatching(new ClassEqualsFilter(
                                        MessageProcessorStore.class));

        if (_persistentStore != null) {
            /*
             * Retrieve and initialize an existing DestinationManager
             */
            _destinationManager = (DestinationManager) _persistentStore
                            .findFirstMatchingItemStream(new ClassEqualsFilter(
                                            DestinationManager.class));

            // Sanity - A PersistentStore should not be in the MessageStore
            // without
            // a DestinationManager!
            if (null == _destinationManager) {
                SIMPMessageProcessorCorruptException e = new SIMPMessageProcessorCorruptException(
                                nls
                                                .getFormattedMessage(
                                                                     "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                                     new Object[] {
                                                                                   "com.ibm.ws.sib.processor.impl.MessageProcessor",
                                                                                   "1:2308:1.445" }, null));

                FFDCFilter
                                .processException(
                                                  e,
                                                  "com.ibm.ws.sib.processor.impl.MessageProcessor.reconstitute",
                                                  "1:2314:1.445", this);

                SibTr.exception(tc, e);
                SibTr
                                .error(
                                       tc,
                                       "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                       new Object[] {
                                                     "com.ibm.ws.sib.processor.impl.MessageProcessor",
                                                     "1:2321:1.445" });
                if (TraceComponent.isAnyTracingEnabled()
                    && tc.isEntryEnabled())
                    SibTr.exit(tc, "reconstitute", e);
                throw e;
            }

            // The administrator initialization in initializeNonPersistent needs
            // the DestinationManager to already have been recovered.
            // Defect 178509
            initializeNonPersistent();

            // D261769 - The destinationChangeListener needs to be created
            // before any AIH
            // is recreated, and as the destinationLocationManager is only
            // avaiable after
            // initializeNonPersistent we have to place this code here for
            // warmStarts.
            // Before loading localizations, start listening to changes to the
            // sets of messaging
            // engines localizing destinations
            _destinationChangeListener = new DestinationChangeListener(this);
            _linkChangeListener = new LinkChangeListener(this);
            // Venu mock mock 
            // For now no _destinationLocationManager
            /*
             * _destinationLocationManager
             * .setChangeListener(_destinationChangeListener);
             */

            // Restore the manager's state data, but not its destinations
            _destinationManager.initializeNonPersistent(this);

            // The MP control adapter has to be created after the DM has been
            // initialized so that a reference to DestinationLookups can be
            // obtained
            createControlAdapter();

            //Venu mock mock  no ProxyHandler for this version of Liberty profile

            /*
             * // Recreate the ProxyHandler
             * _multiMEProxyHandler = (MultiMEProxyHandler) _persistentStore
             * .findFirstMatchingItemStream(new ClassEqualsFilter(
             * MultiMEProxyHandler.class));
             * 
             * // Sanity - A PersistentStore should not be in the MessageStore
             * // without
             * // a multiMEProxyHandler!
             * if (null == _multiMEProxyHandler) {
             * SIMPMessageProcessorCorruptException e = new SIMPMessageProcessorCorruptException(
             * nls
             * .getFormattedMessage(
             * "INTERNAL_MESSAGING_ERROR_CWSIP0001",
             * new Object[] {
             * "com.ibm.ws.sib.processor.impl.MessageProcessor",
             * "1:2363:1.445" }, null));
             * 
             * FFDCFilter
             * .processException(
             * e,
             * "com.ibm.ws.sib.processor.impl.MessageProcessor.reconstitute",
             * "1:2369:1.445", this);
             * 
             * SibTr.exception(tc, e);
             * SibTr
             * .error(
             * tc,
             * "INTERNAL_MESSAGING_ERROR_CWSIP0001",
             * new Object[] {
             * "com.ibm.ws.sib.processor.impl.MessageProcessor",
             * "1:2376:1.445" });
             * if (TraceComponent.isAnyTracingEnabled()
             * && tc.isEntryEnabled())
             * SibTr.exit(tc, "reconstitute", SIMPUtils.getStackTrace(e));
             * throw e;
             * }
             * 
             * _multiMEProxyHandler.initialiseNonPersistent(this, _txManager);
             */

            // Venu mock mock ...  no SystemConnection no recoverNeighbours and no  ConfigureNeighbours

            /*
             * 
             * // Connection creation requires a DestinationManager
             * createSystemConnection();
             * 
             * _proxyHandlerDestAddr = SIMPUtils.createJsSystemDestinationAddress(
             * SIMPConstants.PROXY_SYSTEM_DESTINATION_PREFIX,
             * getMessagingEngineUuid());
             * 
             * // Recover the Neighbours for the ProxyHandler
             * try {
             * _multiMEProxyHandler.recoverNeighbours();
             * } catch (SIResourceException e) {
             * // FFDC
             * FFDCFilter
             * .processException(
             * e,
             * "com.ibm.ws.sib.processor.impl.MessageProcessor.reconstitute",
             * "1:2402:1.445", this);
             * 
             * if (TraceComponent.isAnyTracingEnabled()
             * && tc.isEntryEnabled())
             * SibTr.exit(tc, "reconstitute", "SIErrorException");
             * throw new SIErrorException(e);
             * }
             * 
             * // Read the Neighbour configuration
             * configureNeighbours();
             */

            // Reconstitute destinations
            try {
                _destinationManager.reconstitute(startMode);
            } catch (Exception e) {
                FFDCFilter
                                .processException(
                                                  e,
                                                  "com.ibm.ws.sib.processor.impl.MessageProcessor.reconstitute",
                                                  "1:2422:1.445", this);

                SIErrorException finalE = new SIErrorException(
                                nls
                                                .getFormattedMessage(
                                                                     "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                     new Object[] {
                                                                                   "com.ibm.ws.sib.processor.impl.MessageProcessor",
                                                                                   "1:2431:1.445", e }, null), e);

                SibTr.exception(tc, finalE);
                if (TraceComponent.isAnyTracingEnabled()
                    && tc.isEntryEnabled())
                    SibTr.exit(tc, "reconstitute", finalE);
                throw finalE;
            }

            reconstituted = true;
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstitute", new Boolean(reconstituted));

        return reconstituted;
    }

    /**
     * In a cold start ME Environment, the proxy handler needs to be created.
     */
    private void createProxyHandler() throws MessageStoreException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createProxyHandler");

        _multiMEProxyHandler = new MultiMEProxyHandler(this, _txManager);
        _persistentStore.addItemStream(_multiMEProxyHandler, _txManager
                        .createAutoCommitTransaction());

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createProxyHandler");
    }

    /**
     * Create this message processor's destination manager.
     */
    private void createDestinationManager() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createDestinationManager");

        _destinationManager = new DestinationManager(this, _persistentStore);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createDestinationManager");
    }

    /**
     * Create this message processor's dynamic config manager.
     */
    private void createDynamicConfigManager() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createDynamicConfigManager");

        _dynamicConfigManager = new DynamicConfigManager(this);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createDynamicConfigManager");
    }

    /**
     * Create this message processor's persistent store.
     */
    private void createPersistentStore() throws MessageStoreException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createPersistentStore");

        _persistentStore = new MessageProcessorStore(new SIBUuid8(_engine.getUuid()),
                        _msgStore, _txManager);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createPersistentStore");
    }

    /**
     * Set up a connection to the message processor for any internal components
     * that require the creation of producers/consumer.
     */
    private void createSystemConnection() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createSystemConnection");

        Subject subject = _authorisationUtils.getSIBServerSubject();

        try {
            _connectionToMP = (MPCoreConnection) createConnection(subject,
                                                                  true, null);
        } catch (SIResourceException e) {
            // FFDC
            FFDCFilter
                            .processException(
                                              e,
                                              "com.ibm.ws.sib.processor.impl.MessageProcessor.createSystemConnection",
                                              "1:2529:1.445", this);

            // Won't ever be thrown
            if (TraceComponent.isAnyTracingEnabled()
                && tc.isEntryEnabled())
                SibTr.exit(tc, "createSystemConnection", "SIErrorException "
                                                         + e);
            throw new SIErrorException(e);
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createSystemConnection");
    }

    /**
     * Method to create the System default exception destination. There will be
     * a default exception destination per Messaging Engine.
     */
    public DestinationDefinition createSystemDefaultExceptionDestination()
                    throws SIResourceException, SIMPDestinationAlreadyExistsException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createSystemDefaultExceptionDestination");

        // Set up a suitable definition
        DestinationDefinition defaultExceptionDestDef = createDestinationDefinition(
                                                                                    DestinationType.QUEUE,
                                                                                    SIMPConstants.SYSTEM_DEFAULT_EXCEPTION_DESTINATION
                                                                                                    + getMessagingEngineName());

        // Set up a suitable qos
        defaultExceptionDestDef
                        .setMaxReliability(Reliability.ASSURED_PERSISTENT);
        defaultExceptionDestDef
                        .setDefaultReliability(Reliability.ASSURED_PERSISTENT);
        defaultExceptionDestDef
                        .setUUID(SIMPUtils
                                        .createSIBUuid12(SIMPConstants.SYSTEM_DEFAULT_EXCEPTION_DESTINATION
                                                         + getMessagingEngineName()));

        // Destination is localized on this ME
        Set<String> destinationLocalizingMEs = new HashSet<String>();
        destinationLocalizingMEs.add(getMessagingEngineUuid().toString());

        // Create the destination
        _destinationManager
                        .createDestinationLocalization(defaultExceptionDestDef,
                                                       createLocalizationDefinition(defaultExceptionDestDef
                                                                       .getName()), destinationLocalizingMEs, false);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createSystemDefaultExceptionDestination",
                       defaultExceptionDestDef);

        return defaultExceptionDestDef;
    }

    /**
     * <p>
     * This method creates the System destinations required for MP. Primarily,
     * this is the Queue that is used for receiving Proxy subscription update
     * messages (SYSTEM.MENAME.PROXY.QUEUE)
     * 
     * <P>
     * It is also used to create the SYSTEM.DEFAULT.EXCEPTION.DESTINATION queue
     * 
     * <p>
     * This is designed for a cold start of the ME. It may be that on a warm
     * start, the destinations already exist, so this routine is not required.
     * </p>
     * <p>
     * The administrator needs to be instantiated before this function can be
     * called.
     */
    private void createProxySystemDestination() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createProxySystemDestination");

        try {
            _proxyHandlerDestAddr = _connectionToMP
                            .createSystemDestination(SIMPConstants.PROXY_SYSTEM_DESTINATION_PREFIX);
        } catch (SIException e) {
            FFDCFilter
                            .processException(
                                              e,
                                              "com.ibm.ws.sib.processor.impl.MessageProcessor.createProxySystemDestination",
                                              "1:2614:1.445", this);

            /**
             * Logic error. The message processor must be authorized to create
             * its own destinations
             */
            // TODO: Surely this should be a more specific message indicating
            // this
            // problem? If there is already something quite specific, we can
            // eliminate this stage and just let the core exception
            // fall through to start()
            SIErrorException finalE = new SIErrorException(
                            nls
                                            .getFormattedMessage(
                                                                 "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                 new Object[] {
                                                                               "com.ibm.ws.sib.processor.impl.MessageProcessor",
                                                                               "1:2629:1.445", e }, null), e);

            SibTr.exception(tc, finalE);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002", new Object[] {
                                                                                "com.ibm.ws.sib.processor.impl.MessageProcessor",
                                                                                "1:2635:1.445", SIMPUtils.getStackTrace(e) });

            if (TraceComponent.isAnyTracingEnabled()
                && tc.isEntryEnabled())
                SibTr.exit(tc, "createProxySystemDestination",
                           "SIErrorException");

            throw finalE;
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createProxySystemDestination");
    }

    /**
     * Create this message processor's tick generator. If we've warm started we
     * use a recovered tick value.
     * <p>
     * Feature 174199.2.8
     */
    private void initializeTickGenerator() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "initializeTickGenerator");

        // we do not want ticks to start at zero - it causes the first message
        // to go missing
        long tick = -1;
        try {
            tick = _msgStore.getUniqueTickCount();
        } catch (PersistenceException e) {
            // No FFDC code needed
            // Should be ok to carry on in the result of a persistence failure
            // as
            // we are only trying to increment the tick
            if (TraceComponent.isAnyTracingEnabled()
                && TraceComponent.isAnyTracingEnabled()
                && tc.isEventEnabled())
                SibTr.exception(tc, e);
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "initializeTickGenerator", new Long(tick));
    }

    /**
     * Starts a new thread from the MP Consumers Thread Pool Only consumer
     * threads should use this thread pool.
     * 
     * @param runnable
     * @throws InterruptedException
     */
    public void startNewThread(Runnable runnable) throws InterruptedException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "startNewThread");

        if (_consumerThreadPool == null) {
            createConsumerThreadPool();
        }
        try {
            _consumerThreadPool.execute(runnable,
                                        ThreadPool.EXPAND_WHEN_QUEUE_IS_FULL_WAIT_AT_LIMIT);
        } catch (ThreadPoolQueueIsFullException e) {
            // Exception should never occur as we are waiting when the queue is
            // full.
            FFDCFilter
                            .processException(
                                              e,
                                              "com.ibm.ws.sib.processor.impl.MessageProcessor.startNewThread",
                                              "1:2705:1.445", this);
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "startNewThread");
    }

    /**
     * If the consumer thread pool hasn't been created, create one here.
     */
    private synchronized void createConsumerThreadPool() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createConsumerThreadPool");
        if (_consumerThreadPool == null) {
            // 214163 Add a threadpool listener to associate threads in trace
            // with the messaging engine.
            _consumerThreadPool = new ThreadPool(
                            "Consumer " + getMessagingEngineName(),
                            0,
                            _customProperties.get_max_consumer_threadpool_size(),
                            new ThreadPoolListener[] { new ThreadPoolListenerImpl(this) });
            _consumerThreadPool
                            .setRequestBufferExpansionLimit(Integer.MAX_VALUE);
        }
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createConsumerThreadPool");
    }

    /**
     * Starts a new thread from the MP System Thread Pool Only system threads
     * should start call start using this method.
     * 
     * @param runnable
     * @throws InterruptedException
     */
    public void startNewSystemThread(Runnable runnable)
                    throws InterruptedException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "startNewSystemThread", runnable);

        if (_systemThreadPool == null) {
            createSystemThreadPool();
        }

        try {
            _systemThreadPool.execute(runnable,
                                      ThreadPool.EXPAND_WHEN_QUEUE_IS_FULL_WAIT_AT_LIMIT);
        } catch (ThreadPoolQueueIsFullException e) {
            // Exception should never occur as we are waiting when the queue is
            // full.
            FFDCFilter
                            .processException(
                                              e,
                                              "com.ibm.ws.sib.processor.impl.MessageProcessor.startNewSystemThread",
                                              "1:2758:1.445", this);
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "startNewSystemThread");
    }

    /**
     * If the consumer thread pool hasn't been created, create one here.
     */
    private synchronized void createSystemThreadPool() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createSystemThreadPool");
        if (_systemThreadPool == null) {
            // 214163 Add a threadpool listener to associate threads in trace
            // with the messaging engine.
            _systemThreadPool = new ThreadPool(
                            "System " + getMessagingEngineName(),
                            1,
                            _customProperties.get_max_system_threadpool_size(),
                            new ThreadPoolListener[] { new ThreadPoolListenerImpl(this) });
            _systemThreadPool.setRequestBufferExpansionLimit(Integer.MAX_VALUE);

            // note that setting this to true means that the MAX_SYSTEM_THREADS
            // value is ignored
            _systemThreadPool.setGrowAsNeeded(true);
        }
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createSystemThreadPool");
    }

    /**
     * setConfig is implemented from the interface JsEngineComponent and is used
     * to set properties on the MP
     * 
     * @param config
     *            the JsEObject config of the messaging engine
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent.setConfig
     */
    @Override
    public void setConfig(LWMConfig meConfig) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "setConfig", new Object[] { meConfig });

        _highMessageThreshold = ((JsMessagingEngine) meConfig).getMEThreshold();

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "setConfig");
    }

    /**
     * setCustomProperty is implemented from the interface JsEngineComponent and
     * is used to set properties on the MP
     * 
     * @param propertyName
     *            The name of the Attribute
     * @param propertyValue
     *            The value of the Attribute
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent.setCustomProperty
     */
    @Override
    public void setCustomProperty(String propertyName, String propertyValue) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "setCustomProperty", new Object[] { propertyName,
                                                               propertyValue });

        _customProperties.setProperty(propertyName, propertyValue);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "setCustomProperty");
    }

    /**
     * Returns the messageProcessorMatching.
     * 
     * @return MessageProcessorMatching
     */
    public MessageProcessorMatching getMessageProcessorMatching() {
        return _messageProcessorMatching;
    }

    /**
     * Returns the accessChecker.
     * 
     * @return accessChecker
     */
    public AccessChecker getAccessChecker() {
        return _accessChecker;
    }

    /**
     * Returns the Authorization Utils.
     * 
     * @return authorisationUtils
     */
    public AuthUtils getAuthorisationUtils() {
        return _authorisationUtils;
    }

    /**
     * Returns the discriminatorAccessChecker.
     * 
     * @return topicAuthorization
     */
    public TopicAuthorization getDiscriminatorAccessChecker() {
        return _topicAuthorization;
    }

    /**
     * Method to return the name of the Subscription Proxy Queue used to receive
     * the proxy updates
     * 
     * @return String, the proxy Q name.
     */
    public JsDestinationAddress getProxyHandlerDestAddr() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getProxyHandlerDestAddr");
            SibTr.exit(tc, "getProxyHandlerDestAddr", _proxyHandlerDestAddr);
        }
        return _proxyHandlerDestAddr;
    }

    /**
     * Gets the SearchResultsObjectPool instance
     * 
     * @return the object pool
     */
    public SearchResultsObjectPool getSearchResultsObjectPool() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getSearchResultsObjectPool");
            SibTr.exit(tc, "getSearchResultsObjectPool", _connectionToMP);
        }
        return _searchResultsPool;
    }

    /**
     * Method to return the connection that the SYSTEM queue was created on.
     * This connection is used wherever an internal producerSession or
     * consumerSession is required.
     * 
     * @return SICoreConnection The connection made for registering internal
     *         consumers/producers.
     */
    public MPCoreConnection getSystemConnection() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getSystemConnection");
            SibTr.exit(tc, "getSystemConnection", _connectionToMP);
        }

        return _connectionToMP;
    }

    /**
     * @return
     */
    long getApiMajorVersion() {
        return SIMPConstants.API_MAJOR_VERSION;
    }

    long getApiMinorVersion() {
        return SIMPConstants.API_MINOR_VERSION;
    }

    /**
     * @return
     */
    String getApiLevelDescription() {
        return SIMPConstants.API_LEVEL_DESCRIPTION;
    }

    /**
     * Returns the messagingEngineUuid
     * 
     * @return String
     */
    public SIBUuid8 getMessagingEngineUuid() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "getMessagingEngineUuid");
        SIBUuid8 uuid = _persistentStore.getMessagingEngineUuid();
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "getMessagingEngineUuid", uuid);
        return uuid;
    }

    /**
     * Returns the next tick for the message.
     * 
     * @return
     */
    public long nextTick() throws SIResourceException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "nextTick");
        long tick = -1;
        try {
            tick = _msgStore.getUniqueTickCount();
        } catch (PersistenceException e) {
            // No FFDC code needed

            if (TraceComponent.isAnyTracingEnabled()
                && TraceComponent.isAnyTracingEnabled()
                && tc.isEventEnabled())
                SibTr.exception(tc, e);
            // Rethrow this as an SIResourceException
            if (TraceComponent.isAnyTracingEnabled()
                && tc.isEntryEnabled())
                SibTr.exit(tc, "nextTick", "SIResourceException");

            // TODO could do with a new message to indicate that there was a
            // persistence problem storing the tick.
            throw new SIResourceException(e);
        }
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "nextTick", new Long(tick));
        return tick;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.SIMPFactory#getMEConnectionListener()
     */
    @Override
    public MEConnectionListener getMEConnectionListener() {
        return _mpio;
    }

    // for test purposes only
    public MPIO getMPIO() {
        return _mpio;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.SIMPFactory#getTopologyListener()
     */
    @Override
    public TopologyListener getTopologyListener() {
        return _mpio;
    }

    /**
     * Creates the control message factory single instance
     * 
     * @return the single instance of the control message factory.
     */
    public static ControlMessageFactory getControlMessageFactory() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "getControlMessageFactory");

        if (_controlMessageFactory == null)
            _controlMessageFactory = (ControlMessageFactory) getSingletonInstance(SIMPConstants.CONTROL_MESSAGE_FACTORY);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "getControlMessageFactory", _controlMessageFactory);
        return _controlMessageFactory;
    }

    /**
     * Creates the message handle factory single instance
     * 
     * @return the single instance of the message handle factory.
     */
    public static JsMessageHandleFactory getJsMessageHandleFactory() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "getJsMessageHandleFactory");

        if (_jsMessageHandleFactory == null)
            _jsMessageHandleFactory = (JsMessageHandleFactory) getSingletonInstance(SIMPConstants.JS_MESSAGE_HANDLE_FACTORY);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "getJsMessageHandleFactory", _jsMessageHandleFactory);
        return _jsMessageHandleFactory;
    }

    /**
     * Add an item to the Message Processor ItemStream.
     * 
     * @throws SIResourceException
     * 
     * @see com.ibm.ws.sib.msgstore.AbstractItem#add(Item, Transaction)
     */
    public void add(Item item, TransactionCommon transaction)
                    throws MessageStoreException, SIResourceException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "add", new Object[] { item, transaction });

        Transaction msTran = resolveAndEnlistMsgStoreTransaction(transaction);
        _persistentStore.addItem(item, msTran);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "add");
    }

    /**
     * Get the Transaction Manager
     */
    public SIMPTransactionManager getTXManager() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "getTXManager");

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "getTXManager", _txManager);

        return _txManager;
    }

    /**
     * Get the Target Batch Handler
     */
    public BatchHandler getTargetBatchHandler() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "getTargetBatchHandler");

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "getTargetBatchHandler", _targetBatchHandler);

        return _targetBatchHandler;
    }

    /**
     * Get the Source Batch Handler
     */
    public BatchHandler getSourceBatchHandler() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "getSourceBatchHandler");

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "getSourceBatchHandler", _sourceBatchHandler);

        return _sourceBatchHandler;
    }

    /**
     * Get the Publication Batch Handler
     */
    public BatchHandler getPublicationBatchHandler() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getPublicationBatchHandler");
            SibTr.exit(tc, "getPublicationBatchHandler", _sourceBatchHandler);
        }

        return _publicationBatchHandler;
    }

    public StoppableThreadCache getStoppableThreadCache() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getStoppableThreadCache");
            SibTr.exit(tc, "getStoppableThreadCache", _stoppableThreadCache);
        }
        return _stoppableThreadCache;
    }

    public MPAlarmManager getAlarmManager() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getAlarmManager");
            SibTr.exit(tc, "getAlarmManager", _mpAlarmManager);
        }

        return _mpAlarmManager;
    }

    public ObjectPool getBatchedTimeoutManagerEntryPool() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "getBatchedTimeoutManagerEntryPool");

        if (_batchedTimeoutManagerEntryPool == null)
            _batchedTimeoutManagerEntryPool = new ObjectPool("BTMEntryPool",
                            SIMPConstants.BTM_ENTRY_POOL_SIZE);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "getBatchedTimeoutManagerEntryPool");

        return _batchedTimeoutManagerEntryPool;
    }

    /**
     * Returns the destinationLocationManager.
     * 
     * @return DestinationLocationManager
     */
    public DestinationLocationManager getDestinationLocationManager() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getDestinationLocationManager");
            SibTr.exit(tc, "getDestinationLocationManager",
                       _destinationLocationManager);
        }
        return _destinationLocationManager;
    }

    /**
     * Returns the LinkManager.
     * 
     * @return LinkManager
     */
    public LinkManager getLinkManager() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getLinkManager");
            SibTr.exit(tc, "getLinkManager");
        }
        return _linkManager;
    }

    /**
     * Returns the MQLinkManager.
     * 
     * @return MQLinkManager
     */
    MQLinkManager getMQLinkManager() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getMQLinkManager");
            SibTr.exit(tc, "getMQLinkManager");
        }
        return _mqLinkManager;
    }

    /**
     * Return a set of UUIDs for the Messaging Engines which localize a
     * specified destination
     * 
     * @param busName
     * @param uuid
     * @param newSet
     * @return
     */

    // Venu temp
    // For now Liberty profile is having only one ME and Admin is not having
    // UUID information
    // Hence just setting set to the current ME UUID

    public Set getSIBDestinationLocalitySet(String busName, String uuid,
                                            boolean newSet) throws SIBExceptionDestinationNotFound,
                    SIBExceptionBase {

        /*
         * if (TraceComponent.isAnyTracingEnabled() &&
         * tc.isEntryEnabled()) SibTr.entry(tc, "getSIBDestinationLocalitySet",
         * new Object[] { busName, uuid, new Boolean(newSet) });
         * 
         * Set results = _engine.getSIBDestinationLocalitySet(busName, uuid,
         * newSet);
         * 
         * if (TraceComponent.isAnyTracingEnabled() &&
         * tc.isEntryEnabled()) SibTr.exit(tc, "getSIBDestinationLocalitySet",
         * results);
         */

        //Venu Liberty change:
        //_localistySet is filled up with this ME Uuid in startInternal();
        // This function would get called only after _localistySet is filled up at lease once

        return _localistySet;
    }

    /**
     * @return
     */
    public JsDestinationAddress getTDReceiverAddr() {
        return _tdReceiverAddr;
    }

    /**
     * Method discardMsgsAfterQueueDeletion.
     * <p>
     * Should messages on deleted destinations be discarded or put to the
     * exception destination?
     * </p>
     * 
     * @return boolean
     */
    public boolean discardMsgsAfterQueueDeletion() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "discardMsgsAfterQueueDeletion");
            SibTr.exit(tc, "discardMsgsAfterQueueDeletion", new Boolean(
                            _discardMsgsAfterQueueDeletion));
        }

        return _discardMsgsAfterQueueDeletion;
    }

    /**
     * Set _discardMsgsAfterQueueDeletion to true.
     */
    public void setDiscardMsgsAfterQueueDeletion() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setDiscardMsgsAfterQueueDeletion");
            SibTr.exit(tc, "setDiscardMsgsAfterQueueDeletion");
        }

        _discardMsgsAfterQueueDeletion = true;
    }

    /**
     * @return highMessageThreshold
     */
    public long getHighMessageThreshold() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getHighMessageThreshold");
            SibTr.exit(tc, "getHighMessageThreshold", new Long(
                            _highMessageThreshold));
        }
        return _highMessageThreshold;
    }

    /**
     * @return definedSendWindow
     */
    public int getDefinedSendWindow() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getDefinedSendWindow");
            SibTr.exit(tc, "getDefinedSendWindow", new Integer(
                            _customProperties.get_definedSendWindow()));
        }
        return _customProperties.get_definedSendWindow();
    }

    /**
     * <p>
     * Get the destination change listener that is registered with TRM for
     * changes to the WLM destination groups
     * </p>
     * 
     * @return DestinationChangeListener
     */
    public DestinationChangeListener getDestinationChangeListener() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getDestinationChangeListener");
            SibTr.exit(tc, "getDestinationChangeListener",
                       _destinationChangeListener);
        }
        return _destinationChangeListener;
    }

    /**
     * <p>
     * Get the definition for the named foreign bus
     * </p>
     * 
     * @param busName
     * @return
     */
    public ForeignBusDefinition getForeignBus(String busName) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "getForeignBus", busName);

        JsBus jsBus = (JsBus) _engine.getBus();
        ForeignBusDefinition foreignBusDefinition = jsBus
                        .getForeignBus(busName);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "getForeignBus", foreignBusDefinition);

        return foreignBusDefinition;
    }

    /**
     * Map the name of an ME to its Uuid.
     * 
     * @param name
     *            The name of the ME to map.
     * @return The SIBUuid8 for the ME.
     */
    public SIBUuid8 mapMeNameToUuid(String name) {
        SIBUuid8 result = null;

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "mapMeNameToUuid", name);

        // Be tolerant of bad args
        if (name != null) {
            // First, short circuit the local ME
            if (name.equals(getMessagingEngineName()))
                result = getMessagingEngineUuid();
            else
                ;// Venu liberty change. Here the code was going to TrmMeMain
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "mapMeNameToUuid", result);

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.ibm.ws.sib.processor.impl.interfaces.ControllableResource#
     * getControlAdapter()
     */
    @Override
    public ControlAdapter getControlAdapter() {
        return _controlAdapter;
    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.ibm.ws.sib.processor.impl.interfaces.ControllableResource#
     * createControlAdapter()
     */
    @Override
    public void createControlAdapter() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "createControlAdapter");
        _controlAdapter = new MessageProcessorControl(this);
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "createControlAdapter");
    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.ibm.ws.sib.processor.impl.interfaces.ControllableResource#
     * registerControlAdapterAsMBean()
     */
    @Override
    public void registerControlAdapterAsMBean() {}

    /*
     * (non-Javadoc)
     * 
     * @seecom.ibm.ws.sib.processor.impl.interfaces.ControllableResource#
     * deregisterControlAdapterMBean()
     */
    @Override
    public void deregisterControlAdapterMBean() {}

    /**
     * Allows the unit test framework to be called from the shipped MP code.
     * 
     * @param newHandler
     *            The handler to which calls from MP code to unit test code can
     *            pass through.
     */
    public static void setMPCallsToUnitTestHandler(
                                                   MPCallsToUnitTestHandler newHandler) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "setMPCallsToUnitTestHandler");
            SibTr.exit(tc, "setMPCallsToUnitTestHandler");
        }
        _mpCallsToUnitTestHandler = newHandler;
    }

    /**
     * Allows other parts of the MP code to obtain a reference to the unit test
     * curretly running, if required.
     * <p>
     * Generally this is required for tight control of the behaviour of async
     * events which MP has.
     * <p>
     * Also used by MP to report unit test failures which won't get reported in
     * the field... as it will be re-tried at a later point.
     * 
     * @return null if the MP code is running in a production environment.
     */
    public static MPCallsToUnitTestHandler getMPCallsToUnitTestHandler() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getMPCallsToUnitTestHandler");
            SibTr.exit(tc, "getMPCallsToUnitTestHandler",
                       _mpCallsToUnitTestHandler);
        }
        return _mpCallsToUnitTestHandler;
    }

    /**
     * Indicates that the WAS server is now open for E-business.
     * <p>
     * This event is flowed to all instances of mediation point, as it opens the
     * "gate" wherebye mediation work can now begin in anger.
     * <p>
     * Part of the JsEngineComponent interface.
     */
    @Override
    public void serverStarted() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "serverStarted");

        synchronized (_mpStartStopLock) {

            if (!_started || _isWASOpenForEBusiness) {
                if (TraceComponent.isAnyTracingEnabled()
                    && tc.isEntryEnabled())
                    SibTr.exit(tc, "serverStarted",
                               "Returning as ME not started " + _started
                                               + " or already announced "
                                               + _isWASOpenForEBusiness);
                return;
            }

            _isWASOpenForEBusiness = true;

        }

        _destinationManager.announceWASOpenForEBusiness();

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "serverStarted");
    }

    /**
     * Indicates that the WAS server is closing for E-business.
     * <p>
     * This event is propagated to all instances of mediation point, so the
     * mediations can all be closed properly.
     * <p>
     * Part of the JsEngineComponent interface.
     */
    @Override
    public void serverStopping() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "serverStopping");

        synchronized (_mpStartStopLock) {
            _isWASOpenForEBusiness = false;
        }

        // If the destination manager is null, then don't even attempt this.
        // There is a possibility that the destination manager instance is
        // corrupt.
        if (_destinationManager != null)
            _destinationManager.announceWASClosedForEBusiness();

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "serverStopping");
    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.ibm.ws.sib.processor.impl.interfaces.ControllableResource#
     * dereferenceControlAdapter()
     */
    @Override
    public void dereferenceControlAdapter() {
        _controlAdapter.dereferenceControllable();
        _controlAdapter = null;
    }

    /**
     * Return this message processors dynamic config manager
     */
    public DynamicConfigManager getDynamicConfigManager() {
        return _dynamicConfigManager;
    }

    /**
     * @return
     */
    public boolean isBusSecure() {
        return _isBusSecure;
    }

    public void setBusSecure(boolean secure) {
        _isBusSecure = secure;
    }

    boolean isMulticastEnabled() {
        return _customProperties.get_multicastEnabled();
    }

    MulticastPropertiesImpl getMulticastProperties() {
        return _multicastProperties;
    }

    public long getID() {
        long id = AbstractItem.NO_ID;
        try {
            id = _persistentStore.getID();
        } catch (MessageStoreException e) {
            // FFDC
            FFDCFilter.processException(e,
                                        "com.ibm.ws.sib.processor.impl.MessageProcessor.getID",
                                        "1:3640:1.445", this);

            SibTr.exception(tc, e);
        }
        return id;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#busReloaded(ConfigObject,
     * boolean, boolean, boolean)
     */
    public void busReloaded(JsMEConfig newBus, boolean busChanged,
                            boolean destChg, boolean medChg) {

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "busReloaded");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.ws.sib.admin.JsEngineComponent#engineReloaded(com.ibm.ws.sib.
     * admin.JsMessagingEngine)
     */
    @Override
    public void engineReloaded(Object newEngine) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "engineReloaded", newEngine);
        // At this point, a dynamic configuartion change has occurred, admin
        // have updated all our
        // localized destinations and we should now make use of the new
        // configuration and drive
        // the update of any remote destinations if needed
        _engine = (JsMessagingEngine) newEngine;

        if (_busReloaded) {
            // The bus-wide definitions did change so we must refresh the
            // destinations that are
            // not localised to this ME.
            _dynamicConfigManager.refreshDestinations();
        }

        // Reset the indicator that bus-wide definitions have changed, now that
        // the updates have
        // been made.
        _busReloaded = false;
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "engineReloaded");
    }

    /**
     * @return
     */
    public SelectionCriteriaFactory getSelectionCriteriaFactory() {
        return _selectionCriteriaFactory;
    }

    /**
     * Returns whether the ME is running as part of a stand alone server.
     * 
     * @return
     */
    final public boolean isSingleServer() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "isSingleServer");
        if (!_singleServerSet) {
            JsAdminService service = JsMainAdminComponentImpl.getJsAdminService();
            if (service.isInitialized())
                _isSingleServer = service.isStandaloneServer();
            else
                // If admin service isn't initialised, then assume we are single
                // service.
                _isSingleServer = true;

            _singleServerSet = true;
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "isSingleServer", new Boolean(_isSingleServer));
        return _isSingleServer;
    }

    /**
     * Method to allow the ME to be set as though it is running as part of a
     * standalone server or in an ND environment. Provided for unit-tests.
     * 
     * @param ss
     */
    public void setSingleServer(boolean ss) {
        _singleServerSet = true;
        _isSingleServer = ss;
    }

    final LockManager getConnectionLockManager() {
        return _connectionsLockManager;
    }

    /**
     * <p>
     * Get the link change listener that is registered with TRM for changes to
     * the WLM link groups
     * </p>
     * 
     * @return LinkChangeListener
     */
    public LinkChangeListener getLinkChangeListener() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getLinkChangeListener");
            SibTr.exit(tc, "getLinkChangeListener", _linkChangeListener);
        }
        return _linkChangeListener;
    }

    /**
     * Set MessageProcessor's RuntimeEventListener.
     * 
     * @param listener
     *            A RuntimeEventListener
     */
    public void setRuntimeEventListener(RuntimeEventListener listener) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "setRuntimeEventListener", listener);
        _runtimeEventListener = listener;
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "setRuntimeEventListener");
    }

    /**
     * Get MessageProcessor's RuntimeEventListener.
     */
    public RuntimeEventListener getRuntimeEventListener() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getRuntimeEventListener");
            SibTr.exit(tc, "getRuntimeEventListener", _runtimeEventListener);
        }
        return _runtimeEventListener;
    }

    /**
     * Fire an event notification of type TYPE_SIB_SECURITY_NOT_AUTHENTICATED
     * with reason SECURITY_REASON_NOT_AUTHENTICATED.
     * 
     * @param newState
     */
    private void fireNotAuthenticatedEvent(String userName) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "fireNotAuthenticatedEvent", userName);

        // Check that we have a RuntimeEventListener
        if (_runtimeEventListener != null) {
            // Build the message for the Notification
            String message = nls.getFormattedMessage(
                                                     "USER_NOT_AUTHORIZED_ERROR_CWSIP0301", new Object[] {
                                                                                                          userName, getMessagingEngineName(),
                                                                                                          getMessagingEngineBus() }, null);

            // Build the properties for the Notification
            Properties props = new Properties();

            props.put(SibNotificationConstants.KEY_OPERATION,
                      SibNotificationConstants.OPERATION_CONNECT);
            props.put(SibNotificationConstants.KEY_SECURITY_USERID, userName);
            props.put(SibNotificationConstants.KEY_SECURITY_REASON,
                      SibNotificationConstants.SECURITY_REASON_NOT_AUTHENTICATED);
            // Fire the event
            _runtimeEventListener
                            .runtimeEventOccurred(
                                                  _engine,
                                                  SibNotificationConstants.TYPE_SIB_SECURITY_NOT_AUTHENTICATED,
                                                  message, props);
        } else {
            if (TraceComponent.isAnyTracingEnabled()
                && tc.isDebugEnabled())
                SibTr.debug(tc, "Null RuntimeEventListener, cannot fire event");
        }
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "fireNotAuthenticatedEvent");
    }

    /**
     * Fire an event notification of type TYPE_SIB_SECURITY_NOT_AUTHENTICATED,
     * with reason SECURITY_REASON_NO_USERID.
     * 
     * @param newState
     */
    private void fireNullUserEvent() {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "fireNullUserEvent");

        // Check that we have a RuntimeEventListener
        if (_runtimeEventListener != null) {
            // Build the message for the Notification
            String message = nls.getFormattedMessage(
                                                     "NULL_USER_NOT_AUTHORIZED_ERROR_CWSIP0303",
                                                     new Object[] { getMessagingEngineName(),
                                                                   getMessagingEngineBus() }, null);

            // Build the properties for the Notification
            Properties props = new Properties();

            props.put(SibNotificationConstants.KEY_OPERATION,
                      SibNotificationConstants.OPERATION_CONNECT);
            props.put(SibNotificationConstants.KEY_SECURITY_USERID, "");
            props.put(SibNotificationConstants.KEY_SECURITY_REASON,
                      SibNotificationConstants.SECURITY_REASON_NO_USERID);
            // Fire the event
            _runtimeEventListener
                            .runtimeEventOccurred(
                                                  _engine,
                                                  SibNotificationConstants.TYPE_SIB_SECURITY_NOT_AUTHENTICATED,
                                                  message, props);
        } else {
            if (TraceComponent.isAnyTracingEnabled()
                && tc.isDebugEnabled())
                SibTr.debug(tc, "Null RuntimeEventListener, cannot fire event");
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "fireNullUserEvent");
    }

    /**
     * @param transactionCommon
     * @return
     * @throws SIResourceException
     */
    public Transaction resolveAndEnlistMsgStoreTransaction(
                                                           TransactionCommon transactionCommon) throws SIResourceException {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "resolveAndEnlistMsgStoreTransaction",
                        transactionCommon);

        Transaction msgStoreTran = getTXManager()
                        .resolveAndEnlistMsgStoreTransaction(transactionCommon);

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "resolveAndEnlistMsgStoreTransaction", msgStoreTran);

        return msgStoreTran;
    }

    /*
     * @parm key
     * 
     * @parm command handler
     */
    public void registerCommandHandler(String key, CommandHandler handler) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "registerCommandHandler", new Object[] { key,
                                                                    handler });

        // Add handler to table
        // Note that if there was already a handler with this key it's table
        // entry will
        // be overwritten
        synchronized (_registeredHandlers) {
            _registeredHandlers.put(key, handler);
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "registerCommandHandler");
    }

    /*
     * Gets handler from table. Returns null if not in table
     * 
     * @parm key
     * 
     * @return command handler or null if not in table
     */
    public CommandHandler getRegisteredCommandHandler(String key) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr
                            .entry(tc, "getRegisteredCommandHandler",
                                   new Object[] { key });

        // Get handler from table. Return null if not found
        CommandHandler cHandler = null;
        synchronized (_registeredHandlers) {
            cHandler = _registeredHandlers.get(key);
        }

        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.exit(tc, "getRegisteredCommandHandler", cHandler);

        return cHandler;
    }

    // Getter method for the Custom Properties object

    public CustomProperties getCustomProperties() {
        return _customProperties;
    }

    @Override
    public void busReloaded(Object newBus, boolean busChanged, boolean destChg,
                            boolean medChg) {
        if (TraceComponent.isAnyTracingEnabled()
            && tc.isEntryEnabled())
            SibTr.entry(tc, "busReloaded", new Object[] { newBus,
                                                         new Boolean(busChanged), new Boolean(destChg),
                                                         new Boolean(medChg) });
        // Do nothing now, but remember that the bus-wide definitions of
        // destinations
        // has changed.
        _busReloaded = true;

        // TODO Auto-generated method stub

    }

    // Security Changes for Liberty Messaging: Sharath Start
    /**
     * Get the Authentication proxy instance
     * 
     * @return the authentication
     */
    public Authentication getAuthentication() {
        return _authentication;
    }

    /**
     * Get the Authorization proxy instance
     * 
     * @return the authorization
     */
    public Authorization getAuthorization() {
        return _authorization;
    }
    // Security Changes for Liberty Messaging: Sharath End

}
