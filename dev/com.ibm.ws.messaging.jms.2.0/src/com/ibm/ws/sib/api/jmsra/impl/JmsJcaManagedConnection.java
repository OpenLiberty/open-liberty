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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.DissociatableManagedConnection;
import javax.resource.spi.LazyEnlistableConnectionManager;
import javax.resource.spi.LazyEnlistableManagedConnection;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.LocalTransactionException;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.ra.SibRaEngineComponent;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionListener;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SIUncoordinatedTransaction;
import com.ibm.wsspi.sib.core.SIXAResource;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;

/**
 * Creates core connections using application, default or container credentials
 * and manages a core connection to provide global and container local
 * transaction support.
 */
public class JmsJcaManagedConnection implements ManagedConnection,
                LazyEnlistableManagedConnection, DissociatableManagedConnection {

    /**
     * The parent managed connection factory.
     */
    private final JmsJcaManagedConnectionFactoryImpl _managedConnectionFactory;

    /**
     * The core connection for this managed connection.
     */
    protected final SICoreConnection _coreConnection;

    /**
     * The connection listener for the core connection.
     */
    protected final JmsJcaConnectionListener _connectionListener;

    /**
     * The user details for this connection. If none are provided then the
     * entire subject will be used for authentication.
     */
    private final JmsJcaUserDetails _userDetails;

    /**
     * The subject to use for authentication if no user name and password could
     * be retrieved.
     */
    private final Subject _subject;

    /**
     * The set of currently open sessions on this managed connection.
     */
    private final Set<JmsJcaSessionImpl> _sessions = Collections.synchronizedSet(new HashSet<JmsJcaSessionImpl>());

    /**
     * List of connection event listeners.
     */
    private final List<ConnectionEventListener> _connectionListeners = new ArrayList<ConnectionEventListener>();

    /**
     * The <code>LocalTransaction</code> object for this managed connection.
     * Created lazily.
     */
    private JmsJcaManagedConnection.JmsJcaLocalTransaction _localTransaction;

    /**
     * The <code>XAResource</code> object for this managed connection. Created
     * lazily.
     */
    private SIXAResource _xaResource;

    /**
     * The meta data object for this managed connection. Created lazily.
     */
    private JmsJcaManagedConnectionMetaData _metaData;

    /**
     * Indicates if this managedConnection has a valid connection. Will be
     * false when the me is terminated
     */
    private boolean _validConnection = true;

    /**
     * The log writer.
     */
    private PrintWriter _logWriter;

    private static TraceComponent TRACE = SibTr.register(
                                                         JmsJcaManagedConnection.class, JmsraConstants.MSG_GROUP,
                                                         JmsraConstants.MSG_BUNDLE);

    private static TraceComponent LOCAL_TRANSACTION_TRACE = SibTr.register(
                                                                           JmsJcaLocalTransaction.class, JmsraConstants.MSG_GROUP,
                                                                           JmsraConstants.MSG_BUNDLE);

    private static TraceComponent LISTENER_TRACE = SibTr.register(
                                                                  JmsJcaConnectionListener.class, JmsraConstants.MSG_GROUP,
                                                                  JmsraConstants.MSG_BUNDLE);

    private static TraceNLS NLS = TraceNLS
                    .getTraceNLS(JmsraConstants.MSG_BUNDLE);

    private static final String CLASS_NAME = JmsJcaManagedConnection.class
                    .getName();

    /**
     * Constructs a managed connection.
     * 
     * @param managedConnectionFactory
     *            the parent managed connection factory
     * @param coreConnection
     *            the initial connection
     * @param userDetails
     *            the user details specified when the core connection was
     *            created
     * @param subject
     *            the subject
     * @throws SIConnectionUnavailableException
     *             if the core connection is no longer available
     * @throws SIConnectionDroppedException
     *             if the core connection has been dropped
     */
    JmsJcaManagedConnection(
                            final JmsJcaManagedConnectionFactoryImpl managedConnectionFactory,
                            final SICoreConnection coreConnection,
                            final JmsJcaUserDetails userDetails, final Subject subject)
        throws SIConnectionDroppedException,
        SIConnectionUnavailableException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "JmsJcaManagedConnection", new Object[] {
                                                                              managedConnectionFactory, coreConnection, userDetails,
                                                                              subjectToString(subject) });
        }

        _managedConnectionFactory = managedConnectionFactory;
        _coreConnection = coreConnection;
        _userDetails = userDetails;
        _subject = subject;

        _connectionListener = new JmsJcaConnectionListener();
        _coreConnection.addConnectionListener(_connectionListener);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "JmsJcaManagedConnection");
        }

    }

    /**
     * Creates a session handle to this managed connection. The request
     * information will, by now, contain a connection handle. This is passed,
     * along with this managed connection, on the construction of the new
     * session.
     * 
     * @param requestSubject
     *            the container provided subject, if any
     * @param requestInfo
     *            the application request information
     * @return the connection
     * @throws ResourceException
     *             generic exception
     */
    @Override
    final public Object getConnection(final Subject requestSubject,
                                      final ConnectionRequestInfo requestInfo) throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getConnection", new Object[] {
                                                                    subjectToString(requestSubject), requestInfo });
        }

        // Ensure that the credentials for this request match those passed to
        // the constructor (as we don't support reauthentication)

        final JmsJcaUserDetails requestUserDetails = _managedConnectionFactory
                        .getUserDetails(requestSubject, requestInfo);

        if (_userDetails != null) {

            if (!(_userDetails.equals(requestUserDetails))) {

                throw new ResourceException(NLS.getFormattedMessage(
                                                                    ("AUTHENTICATION_ERROR_CWSJR1103"), new Object[] {
                                                                                                                      "JmsJcaManagedConnection.getConnection",
                                                                                                                      _userDetails.getUserName(),
                                                                                                                      (requestUserDetails == null) ? null
                                                                                                                                      : requestUserDetails.getUserName() },
                                                                    null));

            }

        } else {

            if (!(_subject.equals(requestSubject))) {

                throw new ResourceException(
                                NLS
                                                .getFormattedMessage(
                                                                     ("AUTHENTICATION_ERROR_CWSJR1117"),
                                                                     new Object[] { "JmsJcaManagedConnection.getConnection" },
                                                                     null));
            }

        }

        JmsJcaSessionImpl session = null;

        if (requestInfo == null || requestInfo instanceof JmsJcaConnectionRequestInfo) {
            session = new JmsJcaSessionImpl(this, (JmsJcaConnectionRequestInfo) requestInfo);
            _sessions.add(session);

        } else {

            throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                                                                               ("EXCEPTION_RECEIVED_CWSJR1101"), new Object[] {
                                                                                                                               "getConnection",
                                                                                                                               JmsJcaConnectionRequestInfo.class.getName(),
                                                                                                                               requestInfo.getClass().getName() }, null));

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getConnection", session);
        }
        return session;

    }

    /**
     * Destroys this managed connection. Called when a connection error has
     * occurred or the managed connection has timed out in the free pool. Marks
     * any associated sessions as invalid. Closes the associated core
     * connection.
     * 
     * @throws ResourceException
     *             generic exception
     */
    @Override
    final public void destroy() throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "destroy");
        }

        // Invalidate all of the sessions

        for (final Iterator iterator = _sessions.iterator(); iterator.hasNext();) {

            final Object object = iterator.next();
            if (object instanceof JmsJcaSessionImpl) {
                final JmsJcaSessionImpl session = (JmsJcaSessionImpl) object;
                session.invalidate();
            }

        }

        // Empty the set
        _sessions.clear();

        try {

            // Deregister the connection listener
            _coreConnection.removeConnectionListener(_connectionListener);

            // Close the core connection
            // when destroy calls close connections then connection cannot be reset for future usage so force close by passing boolean true. - PM39926
            _coreConnection.close(true);

        } catch (final SIConnectionLostException exception) {

            // No FFDC code needed
            // d352473
            // We are remote to the ME and the ME connection has been lost
            // we shall surpress this exception as we want the destroy to complete

        } catch (final SIConnectionDroppedException exception) {

            // No FFDC code needed
            // d352473
            // We are remote to the ME and the ME connection has been dropped
            // we shall surpress this exception as we want the destroy to complete

        } catch (final SIException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "destroy",
                                        "1:408:1.91", this);
            throw new ResourceException(NLS.getFormattedMessage(
                                                                "EXCEPTION_RECEIVED_CWSJR1110", new Object[] { exception,
                                                                                                              "createManagedConnection" }, null), exception);

        } catch (final SIErrorException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "destroy",
                                        "1:416:1.91", this);
            throw new ResourceException(NLS.getFormattedMessage(
                                                                "EXCEPTION_RECEIVED_CWSJR1110", new Object[] { exception,
                                                                                                              "createManagedConnection" }, null), exception);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "destroy");
        }

    }

    /**
     * Cleans up this managed connection so that it can be returned to the free
     * pool. Any sessions that are still associated should be invalidated.
     * 
     * @throws ResourceException
     *             generic exception
     */
    @Override
    final public void cleanup() throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "cleanup");
        }

        // Invalidate all of the sessions

        for (final Iterator iterator = _sessions.iterator(); iterator.hasNext();) {
            final Object object = iterator.next();
            if (object instanceof JmsJcaSessionImpl) {
                ((JmsJcaSessionImpl) object).invalidate();
            }
        }
        _sessions.clear();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "cleanup");
        }

    }

    /**
     * Associates the given session with this managed connection. Removes the
     * session from its previous managed connection, if any, and adds it to the
     * set for this connection. Sets this managed connection on the session.
     * 
     * @param object
     *            the session to associate
     * @throws ResourceException
     *             generic exception
     */
    @Override
    final public void associateConnection(Object object)
                    throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "associateConnection", object);
        }

        if (object instanceof JmsJcaSessionImpl) {

            final JmsJcaSessionImpl session = (JmsJcaSessionImpl) object;
            session.associate(this);
            _sessions.add(session);

        } else {

            throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                                                                               ("INVALID_SESSION_CWSJR1104"), new Object[] {
                                                                                                                            "associateConnection",
                                                                                                                            JmsJcaSessionImpl.class.getName(),
                                                                                                                            (object == null ? "null" : object.getClass()
                                                                                                                                            .getName()) }, null));
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "associateConnection");
        }

    }

    /**
     * Dissassociates the given session from this managed connection.
     * 
     * @param session
     *            the session to disassociate
     */
    final void disassociateSession(final JmsJcaSessionImpl session) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "disassociateSession", session);
        }

        _sessions.remove(session);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "disassociateSession");
        }

    }

    /**
     * Adds a connection event listener.
     * 
     * @param listener
     *            the listener to add
     */
    @Override
    final public void addConnectionEventListener(
                                                 final ConnectionEventListener listener) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "addConnectionEventListener", listener);
        }

        synchronized (_connectionListeners) {
            _connectionListeners.add(listener);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "addConnectionEventListener");
        }

    }

    /**
     * Removes a connection event listener.
     * 
     * @param listener
     *            the listener to remove
     */
    @Override
    final public void removeConnectionEventListener(
                                                    final ConnectionEventListener listener) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "removeConnectionEventListener", listener);
        }

        synchronized (_connectionListeners) {
            _connectionListeners.remove(listener);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "removeConnectionEventListener");
        }

    }

    /**
     * Returns an <code>XAResource</code> for this managed connection. When
     * running in WebSphere this is an inner class that implements the
     * <code>RecoverableXAResource</code> interface and delegates to an
     * <code>SIXAResource</code> from the core connection. When running
     * outside of WebSphere the <code>SIXAResource</code> is returned
     * directly.
     * 
     * @return the <code>XAResource</code>
     * @throws NotSupportedException
     *             if running outside of WebSphere and a messaging engine has
     *             not been identified on the connection factory
     * @throws ResourceException
     *             generic exception
     */
    @Override
    final public XAResource getXAResource() throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getXAResource");
        }

        if (_coreConnection == null) {

            throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                                                                               "EXCEPTION_RECEIVED_CWSJR1106",
                                                                               new Object[] { "getXAResource" }, null));

        }

        if (_xaResource == null) {

            try {
                _xaResource = _coreConnection.getSIXAResource();
            } catch (final SIException exception) {

                FFDCFilter.processException(exception, CLASS_NAME
                                                       + "getXAResource", "1:649:1.91", this);

                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "EXCEPTION_RECEIVED_CWSJR1111", new Object[] {
                                                                                                                  exception, "getXAResource" }, null), exception);

            } catch (final SIErrorException exception) {

                FFDCFilter.processException(exception, CLASS_NAME
                                                       + "getXAResource", "1:658:1.91", this);

                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "EXCEPTION_RECEIVED_CWSJR1111", new Object[] {
                                                                                                                  exception, "getXAResource" }, null), exception);

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getXAResource", _xaResource);
        }
        return _xaResource;

    }

    /**
     * Returns a <code>LocalTransaction</code> for this managed connection.
     * This is an inner class so is able to create transaction objects on the
     * managed connection's core connection.
     * 
     * @return the <code>LocalTransaction</code>
     * @throws ResourceException
     *             generic exception
     */
    @Override
    final public LocalTransaction getLocalTransaction()
                    throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getLocalTransaction");
        }

        if (_coreConnection == null) {

            throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                                                                               "EXCEPTION_RECEIVED_CWSJR1107",
                                                                               new Object[] { "getLocalTransaction" }, null));

        }

        if (_localTransaction == null) {
            _localTransaction = new JmsJcaLocalTransaction();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getLocalTransaction", _localTransaction);
        }
        return _localTransaction;

    }

    /**
     * Returns the meta data information for this Jetstream connection.
     * 
     * @return the metadata information
     * @throws ResourceException
     *             generic exception
     */
    @Override
    final public ManagedConnectionMetaData getMetaData()
                    throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getMetaData");
        }

        if (_metaData == null) {
            _metaData = new JmsJcaManagedConnectionMetaData();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getMetaData", _metaData);
        }
        return _metaData;

    }

    /**
     * Sets the log writer for this managed connection.
     * 
     * @param logWriter
     *            the log writer
     * @throws ResourceException
     *             generic exception
     */
    @Override
    final public void setLogWriter(final PrintWriter logWriter)
                    throws ResourceException {
        _logWriter = logWriter;
    }

    /**
     * Gets the log writer from this managed connection or, if none has been
     * set, the log writer from the associated managed connection factory.
     * 
     * @return the log writer
     * @throws javax.resource.ResourceException
     *             generic exception
     */
    @Override
    final public PrintWriter getLogWriter() throws ResourceException {
        if (_logWriter == null) {
            return _managedConnectionFactory.getLogWriter();
        }
        return _logWriter;
    }

    /**
     * Dissociates any sessions currently associated with this managed
     * connection.
     * 
     * @throws ResourceException
     *             generic exception
     */
    @Override
    final public void dissociateConnections() throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, "dissociateConnections");
        }

        // Dissociate sessions

        for (final Iterator iterator = _sessions.iterator(); iterator.hasNext();) {
            final Object object = iterator.next();
            if (object instanceof JmsJcaSessionImpl) {
                ((JmsJcaSessionImpl) object).dissociate();
                iterator.remove();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "dissociateConnections");
        }

    }

    /**
     * Returns the current global or container local transaction, if any, for
     * this managed connection. If there is currently no transaction associated
     * with this managed connection, calls <code>lazyEnlist</code> to ensure
     * that one has not been started.
     * 
     * @param connectionManager
     *            the connection manager for the handle requesting the
     *            transaction
     * @return the current transaction
     * @throws ResourceException
     *             if a lazy enlist fails
     */
    final SITransaction getCurrentTransaction(
                                              final ConnectionManager connectionManager) throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr
                            .entry(this, TRACE, "getCurrentTransaction",
                                   connectionManager);
        }

        // Are we already active in a transaction?
        SITransaction currentTransaction = getActiveTransaction();

        if ((currentTransaction == null)
            && (connectionManager instanceof LazyEnlistableConnectionManager)) {

            // We are not active in a transaction but the connection manager
            // supports lazy enlistment so there may be a current transaction
            // that we don't know about yet

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(this, TRACE,
                            "Lazy enlisting on the connection manager");
            }

            try {

                ((LazyEnlistableConnectionManager) connectionManager)
                                .lazyEnlist(this);

            } catch (final ResourceException exception) {

                FFDCFilter.processException(exception, CLASS_NAME
                                                       + "getCurrentTransaction", "1:837:1.91", this);
                SibTr.error(TRACE, "EXCEPTION_RECEIVED_CWSJR1102", exception);
                throw exception;

            }

            // Are we active in a transaction now?
            currentTransaction = getActiveTransaction();

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getCurrentTransaction", currentTransaction);
        }
        return currentTransaction;

    }

    /**
     * Returns the current global or container local transaction, if any, for
     * this managed connection. Unlike
     * <code>getCurrentTransation<code> it does not do a
     * <code>lazyEnlist</code> if there is not already an active transaction.
     * 
     * @return the current transaction
     */
    protected SITransaction getActiveTransaction() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getActiveTransaction");
        }

        SITransaction activeTransaction = null;

        if ((_xaResource != null) && _xaResource.isEnlisted()) {

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(this, TRACE,
                            "Current transaction is an XA global transaction");
            }

            activeTransaction = _xaResource;

        } else if ((_localTransaction != null)
                   && (_localTransaction.getLocalSITransaction() != null)) {

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                SibTr.debug(TRACE, "Current transaction is a "
                                   + "container-managed local transaction");
            }

            activeTransaction = _localTransaction.getLocalSITransaction();

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getActiveTransaction", activeTransaction);
        }
        return activeTransaction;

    }

    /**
     * Called to indicate that the session associated with this managed
     * connection has begun a local transaction. Notifies the connection event
     * listeners.
     */
    final void localTransactionStarted() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "localTransactionStarted");
        }

        final ConnectionEvent event = new ConnectionEvent(this,
                        ConnectionEvent.LOCAL_TRANSACTION_STARTED);

        // Copy list to protect against concurrent modification by listener

        final List<ConnectionEventListener> copy;
        synchronized (_connectionListeners) {
            copy = new ArrayList<ConnectionEventListener>(_connectionListeners);
        }

        for (final Iterator iterator = copy.iterator(); iterator.hasNext();) {
            final Object object = iterator.next();
            if (object instanceof ConnectionEventListener) {
                ((ConnectionEventListener) object)
                                .localTransactionStarted(event);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "localTransactionStarted");
        }

    }

    /**
     * Called to indicate that the local transaction on the session associated
     * with this managed connection has been committed by the application.
     * Notifies the connection event listeners.
     * 
     */
    final void localTransactionCommitted() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "localTransactionCommited");
        }

        final ConnectionEvent event = new ConnectionEvent(this,
                        ConnectionEvent.LOCAL_TRANSACTION_COMMITTED);

        // Copy list to protect against concurrent modification by listener
        final List<ConnectionEventListener> copy;
        synchronized (_connectionListeners) {
            copy = new ArrayList<ConnectionEventListener>(_connectionListeners);
        }

        for (final Iterator iterator = copy.iterator(); iterator.hasNext();) {
            final Object object = iterator.next();
            if (object instanceof ConnectionEventListener) {
                ((ConnectionEventListener) object)
                                .localTransactionCommitted(event);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "localTransactionCommitted");
        }

    }

    /**
     * Called to indicate that the local transaction on the session associated
     * with this managed connection has been rolled back by the application.
     * Notifies the connection event listeners.
     */
    final void localTransactionRolledBack() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "localTransactionRolledBack");
        }

        final ConnectionEvent event = new ConnectionEvent(this,
                        ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK);

        // Copy list to protect against concurrent modification by listener
        final List<ConnectionEventListener> copy;
        synchronized (_connectionListeners) {
            copy = new ArrayList<ConnectionEventListener>(_connectionListeners);
        }

        for (final Iterator iterator = copy.iterator(); iterator.hasNext();) {
            final Object object = iterator.next();
            if (object instanceof ConnectionEventListener) {
                ((ConnectionEventListener) object)
                                .localTransactionRolledback(event);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "localTransactionRolledBack");
        }

    }

    /**
     * Called by a session to indicate that it has been closed. Removes the
     * session from the set held by this managed connection and notifies the
     * connection event listeners (which includes the connection manager).
     * 
     * @param session
     *            the session that has been closed
     */
    final void sessionClosed(final JmsJcaSessionImpl session) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "sessionClosed", session);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(this, TRACE, "Sending connection closed events to the "
                                     + _connectionListeners.size() + " listeners");
        }

        final ConnectionEvent event = new ConnectionEvent(this,
                        ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(session);

        // Copy list to protect against concurrent modification by listener
        final List<ConnectionEventListener> copy;
        synchronized (_connectionListeners) {
            copy = new ArrayList<ConnectionEventListener>(_connectionListeners);
        }

        for (final Iterator iterator = copy.iterator(); iterator.hasNext();) {
            final Object object = iterator.next();
            if (object instanceof ConnectionEventListener) {
                ((ConnectionEventListener) object).connectionClosed(event);
            }
        }

        _sessions.remove(session);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "sessionClosed");
        }

    }

    /**
     * Notifies the connection even listeners that a connection error has
     * occurred.
     * 
     * @param exception
     *            the exception, if any, that is the cause of the error
     * @param callJCAListener whether we should call the listeners on not
     */
    final void connectionErrorOccurred(final Exception exception, boolean callJCAListener) {

        final String methodName = "connectionErrorOccurred";

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { exception, Boolean.valueOf(callJCAListener) });
        }

        /*
         * Only send the event if the server isn't shutting down anyway
         */

        if (!SibRaEngineComponent.isServerStopping()) {

            final ConnectionEvent event = new ConnectionEvent(this,
                            ConnectionEvent.CONNECTION_ERROR_OCCURRED, exception);

            // Copy list to protect against concurrent modification by listener
            final List<ConnectionEventListener> copy;
            synchronized (_connectionListeners) {
                copy = new ArrayList<ConnectionEventListener>(_connectionListeners);
            }

            // We will probably want to look into some how figuring out that the listener
            // that we have is the JCA one and not some other one that is registered as we
            // want to call those ones regardless of the flag. Today we only expect JCA listeners
            // to be registered
            if (callJCAListener)
            {
                for (final Iterator iterator = copy.iterator(); iterator.hasNext();) {
                    final Object object = iterator.next();
                    if (object instanceof ConnectionEventListener) {
                        ((ConnectionEventListener) object)
                                        .connectionErrorOccurred(event);
                    }
                }
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Returns <code>true</code> if the given subject and connection match
     * those for this managed connection.
     * 
     * @param subject
     *            the subject
     * @param coreConnection
     *            the connection
     * @return if the parameters match
     */
    final boolean match(final Subject subject,
                        final SICoreConnection coreConnection) {

        boolean subjectsMatch = (_subject == null) ? (subject == null)
                        : (_subject.equals(subject));
        boolean coreConnectionsMatch = (coreConnection == null)
                                       || coreConnection.isEquivalentTo(_coreConnection);

        return subjectsMatch && coreConnectionsMatch && isValid();

    }

    /**
     * Returns <code>true</code> if the given user details and connection
     * match those for this managed connection.
     * 
     * @param userDetails
     *            the user details
     * @param coreConnection
     *            the connection
     * @return if the parameters match
     */
    final boolean match(JmsJcaUserDetails userDetails,
                        SICoreConnection coreConnection) {

        boolean usersMatch = (_userDetails == null) ? (userDetails == null)
                        : (_userDetails.equals(userDetails));
        boolean coreConnectionsMatch = (coreConnection == null)
                                       || coreConnection.isEquivalentTo(_coreConnection);

        return usersMatch && coreConnectionsMatch && isValid();
    }

    /**
     * Returns the core connection for this managed connection.
     * 
     * @return the core connection
     */
    final SICoreConnection getCoreConnection() {
        return _coreConnection;
    }

    /**
     * Returns a string representation of this object.
     * 
     * @return the string representing this object
     */
    @Override
    final public String toString() {

        final StringBuffer sb = new StringBuffer("[");
        sb.append(this.getClass().getName());
        sb.append("@");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" ");
        toStringFields(sb);
        sb.append("]");
        return sb.toString();

    }

    /**
     * Appends the fields for this object to the given buffer for the
     * <code>toString</code> method.
     * 
     * @param buffer
     *            the buffer to appends the fields to
     */
    protected void toStringFields(final StringBuffer buffer) {

        buffer.append("<managedConnectionFactory=");
        buffer.append(_managedConnectionFactory);
        buffer.append("> <coreConnection=");
        buffer.append(_coreConnection);
        buffer.append("> <localTransaction=");
        buffer.append(_localTransaction);
        buffer.append("> <xaResource=");
        buffer.append(_xaResource);
        buffer.append("> <metaData=");
        buffer.append(_metaData);
        buffer.append("> <userDetails=");
        buffer.append(_userDetails);
        buffer.append("> <subject=");
        buffer.append(subjectToString(_subject));
        buffer.append("> <logWriter=");
        buffer.append(_logWriter);
        buffer.append("> <sessions=");
        buffer.append(_sessions);

        // Don't call toString on listeners as this will result in recursive calls under JBoss and a StackOverFlow
        buffer.append("> <connectionListeners=[");
        for (int i = 0; i < _connectionListeners.size(); i++) {
            Object o = _connectionListeners.get(i);
            if (i > 0)
                buffer.append(",");
            buffer.append(o.getClass().getName() + "@" + o.hashCode());
        }
        buffer.append("]>");

    }

    /**
     * Converts a subject to a printable form taking care not access the private
     * credentials which requires additional permissions.
     * 
     * @param subject
     *            the subject (may be <code>null</code>)
     * @return a printable from
     */
    public static String subjectToString(final Subject subject) {

        final String result;

        if (subject == null) {

            result = "null";

        } else {

            StringBuffer buffer = new StringBuffer("[");
            buffer.append(subject.getClass().getName());
            buffer.append("@");
            buffer
                            .append(Integer.toHexString(System
                                            .identityHashCode(subject)));
            buffer.append(" <principals=");
            buffer.append(subject.getPrincipals());
            buffer.append(">]");
            result = buffer.toString();

        }

        return result;

    }

    public boolean isValid()
    {
        return _validConnection;
    }

    /**
     * Implementation of the <code>LocalTransaction</code> interface for the
     * Jetstream managed connection. Provides support for container local
     * transactions and container resolution of application local transactions.
     */
    private final class JmsJcaLocalTransaction implements LocalTransaction {

        /**
         * The core transaction. This is set by the begin method.
         */
        private SIUncoordinatedTransaction localSITransaction;

        /**
         * Begins a container local transaction. Obtains a core connection for
         * this managed connection and creates a local transaction object on it.
         * 
         * @throws LocalTransactionException
         *             if an uncoordinated transaction could not be created
         */
        @Override
        public void begin() throws LocalTransactionException {

            if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRANSACTION_TRACE.isEntryEnabled()) {
                SibTr.entry(this, LOCAL_TRANSACTION_TRACE, "begin");
            }

            if (localSITransaction == null) {

                try {

                    localSITransaction = _coreConnection
                                    .createUncoordinatedTransaction();

                } catch (final SIException exception) {

                    FFDCFilter.processException(exception, CLASS_NAME
                                                           + ".JmsJcaLocalTransaction.begin", "1:1293:1.91",
                                                this);
                    throw new LocalTransactionException(NLS
                                    .getFormattedMessage(
                                                         "EXCEPTION_RECEIVED_CWSJR1112",
                                                         new Object[] { exception, "begin" }, null),
                                    exception);

                } catch (final SIErrorException exception) {

                    FFDCFilter.processException(exception, CLASS_NAME
                                                           + ".JmsJcaLocalTransaction.begin", "1:1304:1.91",
                                                this);
                    throw new LocalTransactionException(NLS
                                    .getFormattedMessage(
                                                         "EXCEPTION_RECEIVED_CWSJR1112",
                                                         new Object[] { exception, "begin" }, null),
                                    exception);

                }

            } else {

                throw new LocalTransactionException(NLS.getFormattedMessage(
                                                                            "INVALID_SESSION_CWSJR1105", null, null));

            }

            if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRANSACTION_TRACE.isEntryEnabled()) {
                SibTr.exit(this, LOCAL_TRANSACTION_TRACE, "begin");
            }

        }

        /**
         * If there is currently a core local transaction held by this then
         * commit it, otherwise commit any unresolved local transaction work on
         * the sessions associated with the parent managed connection.
         * 
         * @throws LocalTransactionException
         *             if the uncoordinated transaction could not be committed
         *             or, in the absence of a container local transaction,
         *             there was no unresolved local transaction work
         */
        @Override
        public void commit() throws LocalTransactionException {

            if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRANSACTION_TRACE.isEntryEnabled()) {
                SibTr.entry(this, LOCAL_TRANSACTION_TRACE, "commit");
            }

            if (localSITransaction != null) {

                try {

                    localSITransaction.commit();

                } catch (final SIException exception) {

                    FFDCFilter.processException(exception, CLASS_NAME
                                                           + "JmsJcaLocalTransaction.commit", "1:1352:1.91",
                                                this);
                    throw new LocalTransactionException(
                                    NLS.getFormattedMessage(
                                                            "EXCEPTION_RECEIVED_CWSJR1113",
                                                            new Object[] { exception, "commit" }, null),
                                    exception);

                } catch (final SIErrorException exception) {

                    FFDCFilter.processException(exception, CLASS_NAME
                                                           + "JmsJcaLocalTransaction.commit", "1:1363:1.91",
                                                this);
                    throw new LocalTransactionException(
                                    NLS.getFormattedMessage(
                                                            "EXCEPTION_RECEIVED_CWSJR1113",
                                                            new Object[] { exception, "commit" }, null),
                                    exception);

                } finally
                {
                    // set the localTransction to null, so we can begin a new one later
                    localSITransaction = null;
                }

            } else {

                // There is no container local transaction so commit any
                // unresolved application local transaction work

                for (final Iterator iterator = _sessions.iterator(); iterator
                                .hasNext();) {

                    final Object object = iterator.next();

                    if (object instanceof JmsJcaSessionImpl) {

                        JmsJcaSessionImpl session = (JmsJcaSessionImpl) object;
                        try {

                            session.commitUnresolvedLocalTransaction();

                        } catch (final SIException exception) {

                            FFDCFilter.processException(exception, CLASS_NAME
                                                                   + "JmsJcaLocalTransaction.commit",
                                                        "1:1399:1.91", this);
                            throw new LocalTransactionException(
                                            NLS
                                                            .getFormattedMessage(
                                                                                 "EXCEPTION_RECEIVED_CWSJR1114",
                                                                                 new Object[] { exception,
                                                                                               "commit" }, null),
                                            exception);

                        } catch (final SIErrorException exception) {

                            FFDCFilter.processException(exception, CLASS_NAME
                                                                   + "JmsJcaLocalTransaction.commit",
                                                        "1:1412:1.91", this);
                            throw new LocalTransactionException(
                                            NLS
                                                            .getFormattedMessage(
                                                                                 "EXCEPTION_RECEIVED_CWSJR1114",
                                                                                 new Object[] { exception,
                                                                                               "commit" }, null),
                                            exception);

                        }

                    }

                }

            }

            if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRANSACTION_TRACE.isEntryEnabled()) {
                SibTr.exit(this, LOCAL_TRANSACTION_TRACE, "commit");
            }

        }

        /**
         * If there is currently a core local transaction held by this then roll
         * it back, otherwise roll back any unresolved local transaction work on
         * the sessions associated with the parent managed connection.
         * 
         * @throws LocalTransactionException
         *             if the uncoordinated transaction could not be committed
         *             or, in the absence of a container local transaction,
         *             there was no unresolved local transaction work
         */
        @Override
        public void rollback() throws LocalTransactionException {

            if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRANSACTION_TRACE.isEntryEnabled()) {
                SibTr.entry(this, LOCAL_TRANSACTION_TRACE, "rollback");
            }

            if (localSITransaction != null) {

                try {

                    localSITransaction.rollback();

                } catch (final SIException exception) {

                    FFDCFilter.processException(exception, CLASS_NAME
                                                           + "JmsJcaLocalTransaction.rollback", "1:1460:1.91",
                                                this);
                    // Don't throw an exception as this is during a rollback and we
                    // assume that a rollback will happen regardless as to whether the
                    // rollback call was success.

                } catch (final SIErrorException exception) {

                    FFDCFilter.processException(exception, CLASS_NAME
                                                           + "JmsJcaLocalTransaction.rollback", "1:1469:1.91",
                                                this);
                    // Don't throw an exception as this is during a rollback and we
                    // assume that a rollback will happen regardless as to whether the
                    // rollback call was success.
                } finally
                {
                    // set the localTransction to null, so we can begin a new one later
                    localSITransaction = null;
                }

            } else {

                // Rollback all of the sessions
                for (final Iterator iterator = _sessions.iterator(); iterator
                                .hasNext();) {

                    final Object object = iterator.next();
                    if (object instanceof JmsJcaSessionImpl) {

                        final JmsJcaSessionImpl session = (JmsJcaSessionImpl) object;
                        try {

                            session.rollbackUnresolvedLocalTransaction();

                        } catch (final SIException exception) {

                            FFDCFilter.processException(exception, CLASS_NAME
                                                                   + "JmsJcaLocalTransaction.rollback",
                                                        "1:1499:1.91", this);
                            // Don't throw an exception as this is during a rollback and we
                            // assume that a rollback will happen regardless as to whether the
                            // rollback call was success.

                        } catch (final SIErrorException exception) {

                            FFDCFilter.processException(exception, CLASS_NAME
                                                                   + "JmsJcaLocalTransaction.rollback",
                                                        "1:1508:1.91", this);
                            // Don't throw an exception as this is during a rollback and we
                            // assume that a rollback will happen regardless as to whether the
                            // rollback call was success.;

                        }
                    }
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRANSACTION_TRACE.isEntryEnabled()) {
                SibTr.exit(this, LOCAL_TRANSACTION_TRACE, "rollback");
            }

        }

        /**
         * Returns the local SITransaction.
         * 
         * @return the SITransaction
         */
        SITransaction getLocalSITransaction() {
            return localSITransaction;
        }

        /**
         * Returns a string representation of this object.
         * 
         * @return the string representing this object
         */
        @Override
        public String toString() {

            final StringBuffer sb = new StringBuffer("[");
            sb.append(this.getClass().getName());
            sb.append("@");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" <localSITransaction=");
            sb.append(localSITransaction);
            sb.append(">]");
            return sb.toString();

        }

    }

    /**
     * The meta data for a Jetstream managed connection.
     */
    private final class JmsJcaManagedConnectionMetaData implements
                    ManagedConnectionMetaData {

        /**
         * Returns the product name &quote;WebSphere JMS&quote;
         * 
         * @return the product name
         * @throws javax.resource.ResourceException
         *             generic exception
         */
        @Override
        public String getEISProductName() throws ResourceException {
            return "WebSphere JMS";
        }

        /**
         * Returns the product version &quote;1.0&quote;.
         * 
         * @return the product version
         * @throws javax.resource.ResourceException
         *             generic exception
         */
        @Override
        public String getEISProductVersion() throws ResourceException {
            return "1.0";
        }

        /**
         * Returns zero as there is no maximum number of connections.
         * 
         * @return the maximum number of connections
         * @throws javax.resource.ResourceException
         *             generic exception
         */
        @Override
        public int getMaxConnections() throws ResourceException {
            return 0;
        }

        /**
         * Returns the user name used to create the connection currently
         * associated with this managed connection.
         * 
         * @return the user name
         * @throws javax.resource.ResourceException
         *             generic exception
         */
        @Override
        public String getUserName() throws ResourceException {

            String userName = null;
            if (_userDetails != null) {
                userName = _userDetails.getUserName();
            }
            return userName;

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
            sb.append("]");
            return sb.toString();

        }

    }

    /**
     * Listener used to receive notfication of the failure of the core
     * connection associated with this managed connection.
     */
    private final class JmsJcaConnectionListener implements
                    SICoreConnectionListener {

        /**
         * Notifies the listener of an asynchronous exception on the given
         * consumer. Does nothing.
         * 
         * @param consumer
         *            the consumer associated with the exception
         * @param exception
         *            the exception
         */
        @Override
        public void asynchronousException(final ConsumerSession consumer,
                                          final Throwable exception) {

            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                final String methodName = "asynchronousException";
                SibTr.entry(this, LISTENER_TRACE, methodName, new Object[] {
                                                                            consumer, exception });
                SibTr.exit(this, LISTENER_TRACE, methodName);
            }

        }

        /**
         * Notifies the listener when the messaging engine to which it is
         * connected is quiescing. Does nothing.
         * 
         * @param connection
         *            the connection
         */
        @Override
        public void meQuiescing(final SICoreConnection connection) {
            final String methodName = "meQuiescing";
            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled())
                SibTr.entry(this, LISTENER_TRACE, methodName, connection);

            _validConnection = false;

            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled())
                SibTr.exit(this, LISTENER_TRACE, methodName);
        }

        /**
         * Notifies the listener when a remote connection fails. Sends a
         * connection error event to the connection manager.
         * 
         * @param connection
         *            the connection
         * @param exception
         *            the exception
         */
        @Override
        public void commsFailure(final SICoreConnection connection,
                                 final SIConnectionLostException exception) {

            final String methodName = "commsFailure";
            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.entry(this, LISTENER_TRACE, methodName, new Object[] {
                                                                            connection, exception });
            }

            // Pass false as we don't want to call the JCA listener when we
            // are on a different thread to the orignal connection
            connectionErrorOccurred(exception, false);

            _validConnection = false;

            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.exit(this, LISTENER_TRACE, methodName);
            }

        }

        /**
         * Notifies the listener when the messaging engine to which it is
         * connected terminates. Sends a connection error event to the
         * connection manager.
         * 
         * @param connection
         *            the connection
         */
        @Override
        public void meTerminated(final SICoreConnection connection) {

            final String methodName = "meTerminated";
            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.entry(this, LISTENER_TRACE, methodName, connection);
            }

            // Pass false as we don't want to call the JCA listener when we
            // are on a different thread to the orignal connection
            connectionErrorOccurred(null, false);

            _validConnection = false;

            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                SibTr.exit(this, LISTENER_TRACE, methodName);
            }

        }

    }

}
