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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ResourceAdapterInternalException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.resource.ResourceRefInfo.Property;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnection;
import com.ibm.ws.sib.api.jmsra.JmsJcaSession;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;

/**
 * Implementation of the connection interface between the JMS API and resource
 * adapter. Provides factory methods for creating a session. Holds a reference
 * to a <code>SICoreConnection</code>.
 */
@SuppressWarnings("deprecation")
final class JmsJcaConnectionImpl implements JmsJcaConnection {

    /**
     * The parent connection factory from which this connection was created.
     */
    private final JmsJcaConnectionFactoryImpl _connectionFactory;

    /**
     * The core connection associated with this connection.
     */
    private SICoreConnection _coreConnection;

    /**
     * The set of JCA sessions created from this connection and currently open.
     */
    private final Set _sessions = new HashSet();

    /**
     * The request info to be used for subsequent session creation.
     */
    private final JmsJcaConnectionRequestInfo _requestInfo;

    /**
     * A session is created as a by product of creating the connection. The
     * first call to createSession should return this instance and set this
     * variable to false
     */
    private boolean firstSessionCached = false;

    /**
     * Flag to keep track of whether the connection has been closed.
     */
    private boolean connectionClosed = false;

    private Thread _connectionCreateThread = null;
    private Object _connectionCreateUOW = null;

    /**
     * Set to indicate if the current JVM is a WAS JVM or not. Should always be accessed via the isRunningInWAS method.
     */
    private static Boolean inWAS = null;

    private static TraceComponent TRACE = SibTr.register(
                                                         JmsJcaConnectionImpl.class, JmsraConstants.MSG_GROUP,
                                                         JmsraConstants.MSG_BUNDLE);

    private static TraceNLS NLS = TraceNLS
                    .getTraceNLS(JmsraConstants.MSG_BUNDLE);

    private static final String FFDC_PROBE_1 = "1";

    private static final String CLASS_NAME = JmsJcaConnectionImpl.class
                    .getName();

    // Constructor deleted           SIB0048c.ra.1
    /**
     * Constructor.
     * 
     * @param connectionFactory
     *            the parent connection factory
     * @param coreConnection
     *            the core connection associated with this connection
     * @param firstSession
     *            the first session for this connection
     * @param cri
     *            the ConnectionRequestInfo instance to associate with this connection
     */
    JmsJcaConnectionImpl(final JmsJcaConnectionFactoryImpl connectionFactory,
                         final SICoreConnection coreConnection,
                         final JmsJcaSessionImpl firstSession,
                         final JmsJcaConnectionRequestInfo cri) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "JmsJcaConnectionImpl", new Object[] {
                                                                           connectionFactory, coreConnection, firstSession, cri });
        }

        _connectionFactory = connectionFactory;
        _coreConnection = coreConnection;

        // Attempts to cache the given session and stores the related information needed
        // to safely use the cached session later. If not running in WAS the session will not
        // be cached because the checks to ensure it is safe to do so can only work within
        // a WAS JVM.
        if (isRunningInWAS()) {
            // it is only safe to cache the first session if later, when it is used, we can check
            // that it is used on the same thread and in the same transaction.  The transaction check
            // only works if running inside WAS.
            _connectionCreateThread = Thread.currentThread();
            _connectionCreateUOW = getCurrentUOWCoord();
            _sessions.add(firstSession);
            firstSession.setParentConnection(this);
            firstSessionCached = true;
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                SibTr.debug(TRACE, "first session cached.");
        } else {
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                SibTr.debug(TRACE, "Not in WAS, so not caching the first session.");
            firstSessionCached = true; // We are caching this for now but will not use it.
            // Although we are not using the first session it has been created and as such
            // will need to be closed. We can either close the session now or cache it and 
            // close it when the connection is closed or the user tried to obtain the first 
            // connection
            _sessions.add(firstSession);
        }
        _requestInfo = cri;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "JmsJcaConnectionImpl");
        }

    }

    // Constructor deleted           SIB0048c.ra.1
    /**
     * Constructor.
     * 
     * @param connectionFactory
     *            the parent connection factory
     * @param coreConnection
     *            the core connection associated with this connection
     * @param cri
     *            the ConnectionRequestInfo instance to associate with this connection
     */
    JmsJcaConnectionImpl(final JmsJcaConnectionFactoryImpl connectionFactory,
                         final SICoreConnection coreConnection,
                         final JmsJcaConnectionRequestInfo cri) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "JmsJcaConnectionImpl", new Object[] {
                                                                           connectionFactory, coreConnection, cri });
        }

        _connectionFactory = connectionFactory;
        _coreConnection = coreConnection;
        _requestInfo = cri;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "JmsJcaConnectionImpl");
        }
    }

    /**
     * Creates a <code>JmsJcaSession</code> that shares the core connection
     * from this connection. When first called it returns the session that was
     * created when this connection was requested. On subsequent calls,
     * constructs a <code>JmsJcaConnectionRequestInfo</code> containing this
     * connection. This is passed, along with managed connection factory from
     * the parent connection factory, to the <code>allocateConnection</code>
     * method on the connection manager which returns a session. In either case
     * the transacted flag is set on the session before it is returned.
     * 
     * @param transacted
     *            a flag indicating whether, in the absence of a global or
     *            container local transaction, work should be performed inside
     *            an application local transaction
     * @return the session
     * @throws ResourceException
     *             if the JCA runtime fails to allocate a managed connection
     * @throws IllegalStateException
     *             if this connection has been closed
     * @throws SIException
     *             if the core connection cannot be cloned
     * @throws SIErrorException
     *             if the core connection cannot be cloned
     */
    @Override
    synchronized public JmsJcaSession createSession(boolean transacted)
                    throws ResourceException, IllegalStateException, SIException,
                    SIErrorException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createSession", new Object[] { Boolean
                            .valueOf(transacted) });
        }

        JmsJcaSessionImpl session;

        if (connectionClosed) {
            throw new IllegalStateException(NLS.getFormattedMessage(
                                                                    ("ILLEGAL_STATE_CWSJR1085"),
                                                                    new Object[] { "createSession" }, null));
        }

        // If the firstSessionCached is true and the thread it was created on is different
        // to the current thread this method is called on then change the cached boolean
        // and delete the session as we don't want access to it
        // PK57931 also remove the cached session if the current tx is different to the one 
        // at session create time.
        // If we are not running in was the blow away the cached connection too.
        if (firstSessionCached &&
            ((!(Thread.currentThread().equals(_connectionCreateThread) && isCurrentUOW(_connectionCreateUOW)))
            || (!isRunningInWAS())))
        {
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                SibTr.debug(TRACE, "clearing first session");
            firstSessionCached = false;
            _connectionCreateUOW = null;
            _connectionCreateThread = null;
            //remove the session
            //it should be the only one in the set, but check first
            if (_sessions.size() == 1)
            {
                final Iterator iterator = _sessions.iterator();
                final Object object = iterator.next();
                if (!(object instanceof JmsJcaSessionImpl)) {
                    throw new ResourceAdapterInternalException(NLS
                                    .getFormattedMessage(("EXCEPTION_RECEIVED_CWSJR1083"),
                                                         new Object[] { "createSession",
                                                                       object.getClass().getName(),
                                                                       getClass().getName() }, null));
                }
                session = (JmsJcaSessionImpl) object;
                session.close(false);

                _sessions.clear();
            }
            else
            {
                throw new ResourceAdapterInternalException(NLS
                                .getFormattedMessage(("EXCEPTION_RECEIVED_CWSJR1082"),
                                                     new Object[] { "createSession",
                                                                   "" + _sessions.size() }, null));
            }
        }

        // If this is the first call to this method we can return the session
        // that was created before (which should be the only session in the
        // set). We should also be on the same thread as we have reset the cached value
        // above if we weren't
        if (firstSessionCached)
        {
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(
                            TRACE,
                            "Using the first session that was created as part of the createConnection process");
            }

            if (_sessions.size() != 1) {
                throw new ResourceAdapterInternalException(NLS
                                .getFormattedMessage(("EXCEPTION_RECEIVED_CWSJR1082"),
                                                     new Object[] { "createSession",
                                                                   "" + _sessions.size() }, null));
            }

            final Iterator iterator = _sessions.iterator();
            final Object object = iterator.next();
            if (!(object instanceof JmsJcaSessionImpl)) {
                throw new ResourceAdapterInternalException(NLS
                                .getFormattedMessage(("EXCEPTION_RECEIVED_CWSJR1083"),
                                                     new Object[] { "createSession",
                                                                   object.getClass().getName(),
                                                                   getClass().getName() }, null));
            }
            firstSessionCached = false;
            _connectionCreateUOW = null;
            _connectionCreateThread = null;
            session = (JmsJcaSessionImpl) object;

        } else {

            // We have already returned the initial session so we should create
            // a new session here.
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr
                                .debug(this, TRACE,
                                       "We have already used the first session... creating a new one");
            }

            // Feature 169626.14
            if (_connectionFactory.isManaged()) {

                // True if we aren using container managed authentication
                boolean containerAuth = false;

                // The containter managed authentication alias (only relevant if containerAuth is true)
                String containerAlias = null;

                try
                {

                    ConnectionManager cm = _connectionFactory.getConnectionManager();

                    // Obtain authentication data in case an exception is thrown. This data is then used to give a more 
                    // meaningful error message. The information is obtained now as it may be altered later on. This is 
                    // only valid when running inside WAS.
                    if (isRunningInWAS())
                    {

                        if (cm instanceof com.ibm.ws.jca.adapter.WSConnectionManager)
                        {

                            ResourceRefInfo info = ((com.ibm.ws.jca.adapter.WSConnectionManager) cm).getResourceRefInfo();
                            containerAuth = info.getAuth() == com.ibm.ws.javaee.dd.common.ResourceRef.AUTH_CONTAINER;
                            for (Property loginConfigProp : info.getLoginPropertyList())
                                if ("DefaultPrincipalMapping".equals(loginConfigProp.getName()))
                                    containerAlias = loginConfigProp.getValue();
                        }
                    }

                    final Object object = cm.allocateConnection(
                                                                _connectionFactory
                                                                                .getManagedConnectionFactory(),
                                                                _requestInfo);

                    if (!(object instanceof JmsJcaSessionImpl)) {

                        throw new ResourceAdapterInternalException(NLS
                                        .getFormattedMessage(
                                                             ("EXCEPTION_RECEIVED_CWSJR1084"),
                                                             new Object[] {
                                                                           "createSession",
                                                                           object.getClass().getName(),
                                                                           JmsJcaSessionImpl.class
                                                                                           .getName() }, null));

                    }

                    session = (JmsJcaSessionImpl) object;

                } catch (final ResourceException exception)
                {

                    FFDCFilter.processException(exception, CLASS_NAME
                                                           + ".createSession", FFDC_PROBE_1, this);

                    Throwable cause = exception.getCause();

                    // This section will look at the cause of an exception thrown while creating a connection. If the cause
                    // is a security based exeption (SIAuthenticationException or SINotAuthorizedException) then a new 
                    // exception is created to supply the user with more information.

                    // pnickoll: We would like to pass the "cause" as a linked exception to the following exceptions
                    if (isRunningInWAS())
                    {
                        if (cause instanceof SIAuthenticationException)
                        {
                            SIAuthenticationException authentEx = null;
                            if (containerAuth)
                            {
                                if (containerAlias != null && !containerAlias.isEmpty()) {
                                    authentEx = new SIAuthenticationException(NLS.getFormattedMessage("CONTAINER_AUTHENTICATION_EXCEPTION_1073",
                                                                                                      new Object[] { containerAlias, }, null));
                                } else {
                                    authentEx = new SIAuthenticationException(NLS.getString("AUTHENTICATION_EXCEPTION_1077"));
                                }
                            }
                            else
                            {
                                authentEx = new SIAuthenticationException(NLS.getFormattedMessage("APPLICATION_AUTHENTICATION_EXCEPTION_1074",
                                                                                                  new Object[] { _connectionFactory.getUserName() }, null));
                            }

                            throw authentEx;
                        }
                        else if (cause instanceof SINotAuthorizedException)
                        {
                            if (containerAuth)
                            {
                                if (containerAlias == null)
                                {
                                    throw new SINotAuthorizedException(NLS.getString("CONTAINER_AUTHORIZATION_EXCEPTION_1075"));
                                }
                            }
                            else
                            {
                                String userName = _connectionFactory.getUserName();
                                if ((userName == null) || ("".equals(userName)))
                                {
                                    throw new SINotAuthorizedException(NLS.getString("CONTAINER_AUTHORIZATION_EXCEPTION_1076"));
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

                // The coreConnection could have been recreated during allocateConnection
                // if we had to create a new ManagedConnection. So reset the coreConnection
                // for this JmsJcaConnection if the two are different. Different means that
                // one is not Equivalent to the other which means different userid or different
                // physical connection
                SICoreConnection managedConnectionsCoreConnection = session.getManagedConnection().getCoreConnection();
                if (!managedConnectionsCoreConnection.isEquivalentTo(_coreConnection))
                {
                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                        SibTr.debug(this, TRACE, "New ManagedConnections coreConnection is not equivalent to our _coreConnection");
                    }
                    try
                    {
                        // We are closing the connection here for completeness. The fact that createSession
                        // has driven us to determine that clonedconnection from this _coreConnection is now
                        // closed will typically mean all conversations on the physcial connection are closed.
                        // To stop a possible issue with this _coreConnection not being closed we will call
                        // close now just before we overwrite it below. From the javadoc of close, calling close
                        // on a already closed connection should have not effect, but we will catch all exceptions
                        // to be safe.
                        _coreConnection.close(false);
                    } catch (Exception e)
                    {
                        // No FFDC needed
                        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                            SibTr.debug(this, TRACE, "Exception caught while closing _coreConnection that we are replacing: " + e);
                        }
                    }

                    // clone the connection so that the JMSJcaConnection object uses a different
                    // conversation to the mangedConnection, this is so the managedConnection can
                    // control close/destroy etc without impacting the conversation doing the 
                    // jms connection/session work.
                    _coreConnection = managedConnectionsCoreConnection.cloneConnection();
                    // Now reset the SIcoreConnection in the requestInfo to later calls to
                    //  createSession will pass in the new coreConnection to base further
                    //  clones on.
                    _requestInfo.setSICoreConnection(_coreConnection);
                }
            }
            else
            {
                session = new JmsJcaSessionImpl();
            }

            session.setParentConnection(this);
            _sessions.add(session);

        }

        session.setTransacted(transacted);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createSession", session);
        }

        return session;

    }

    /**
     * Returns the core connection created for, and associated with, this
     * connection.
     * 
     * @return the core connection
     * @throws IllegalStateException
     *             if this connection has been closed
     */
    @Override
    public SICoreConnection getSICoreConnection() throws IllegalStateException {

        if (connectionClosed) {
            throw new IllegalStateException(NLS.getFormattedMessage(
                                                                    ("ILLEGAL_STATE_CWSJR1086"),
                                                                    new Object[] { "getSICoreConnection" }, null));
        }
        return _coreConnection;

    }

    /**
     * Closes this connection, its associated core connection and any sessions
     * created from it.
     * 
     * Used in Outbound Diagram 8
     * 
     * @throws SIErrorException
     *             if the associated core connection failed to close
     * @throws SIResourceException
     *             if the associated core connection failed to close
     * @throws SIIncorrectCallException
     *             if the associated core connection failed to close
     * @throws SIConnectionLostException
     *             if the associated core connection failed to close
     */
    @Override
    synchronized public void close() throws SIConnectionLostException,
                    SIIncorrectCallException, SIResourceException, SIErrorException,
                    SIConnectionDroppedException, SIConnectionUnavailableException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "close");
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
        {
            SibTr.debug(TRACE, "We have " + _sessions.size() + " left open - closing them");
        }

        // Close all the sessions created from this connection.
        for (final Iterator iterator = _sessions.iterator(); iterator.hasNext();) {

            final Object object = iterator.next();
            if (object instanceof JmsJcaSessionImpl) {
                final JmsJcaSessionImpl session = (JmsJcaSessionImpl) object;
                session.close(false);
            }
            iterator.remove();

        }

        if (_coreConnection != null) {
            _coreConnection.close();
        }

        connectionClosed = true;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "close");
        }

    }

    /**
     * Removes the session from the set of sessions
     * 
     * @param session
     *            the session to be removed
     */
    void removeSession(final JmsJcaSessionImpl session) {
        _sessions.remove(session);
    }

    /**
     * Returns the connection manager.
     * 
     * @return the connection manager
     */
    ConnectionManager getConnectionManager() {
        return _connectionFactory.getConnectionManager();
    }

    /**
     * Returns the connection factory.
     * 
     * @return the connection factory
     */
    JmsJcaConnectionFactoryImpl getConnectionFactory() {
        return _connectionFactory;
    }

    /**
     * Returns a string representation of this object
     * 
     * @return String The string describing this object
     */
    @Override
    public String toString() {

        StringBuffer sb = new StringBuffer("[");
        sb.append(getClass().getName());
        sb.append("@");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" <connectionFactory=");
        sb.append(_connectionFactory);
        sb.append("> <coreConnection=");
        sb.append(_coreConnection);
        sb.append("> <firstSessionCached=");
        sb.append(firstSessionCached);
        sb.append("> <connectionClosed=");
        sb.append(connectionClosed);
        sb.append("> <sessions=");
        sb.append(_sessions);
        sb.append(">]");
        return sb.toString();

    }

    /**
     * Returns true if running in WAS. The check is to see if com.ibm.tx.jta.Transaction.TransactionManagerFactory
     * is on the classpath.
     * 
     * @return if running in WAS
     */
    private static boolean isRunningInWAS() {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, "isRunningInWAS");
        }

        if (inWAS == null) {
            inWAS = Boolean.TRUE;
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, "isRunningInWAS", inWAS);
        }
        return inWAS.booleanValue();
    }

    /**
     * This method uses reflection to try and obtain the current UOW, if we are not inside WAS
     * null is returned.
     * 
     * @return The current UOW
     */
    private static final Object getCurrentUOWCoord() {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, "getCurrentUOWCoord");
        }

        Object currentUOW = null;
        if (isRunningInWAS()) {
            currentUOW = EmbeddableTransactionManagerFactory.getUOWCurrent().getUOWCoord();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, "getCurrentUOWCoord", currentUOW);
        }
        return currentUOW;
    }

    /**
     * Checks if the UOW (ie transaction) is the UOW currently active on the thread.
     * If there is no current UOW and the parameter is null, true is returned. This check
     * only works if the WAS TransactionManager is available.
     * 
     * @param ouw the UOW of check
     * @return if running in WAS and uow matches the current UOW on the thread, otherwise false.
     */
    private static final boolean isCurrentUOW(Object uow) {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, "isCurrentUOW", uow);
        }

        boolean isSameUOW = false;

        if (isRunningInWAS()) {
            Object currentUOW = getCurrentUOWCoord();

            if (uow == null) {
                isSameUOW = currentUOW == null;
            } else {
                isSameUOW = uow.equals(currentUOW);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, "isCurrentUOW", Boolean.valueOf(isSameUOW));
        }
        return isSameUOW;
    }

}
