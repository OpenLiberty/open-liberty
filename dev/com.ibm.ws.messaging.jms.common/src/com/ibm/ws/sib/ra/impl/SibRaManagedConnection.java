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

package com.ibm.ws.sib.ra.impl;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
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
 * Implementation of <code>ManagedConnection</code> for core SPI resource
 * adapter. The managed connection is associated with a core SPI connection from
 * creation. This is used to create <code>SITransaction</code> objects to
 * represent the current container transaction. The managed connection supports
 * both lazy enlistment and dissociation.
 */
final class SibRaManagedConnection implements ManagedConnection,
        LazyEnlistableManagedConnection, DissociatableManagedConnection {

    /**
     * The managed connection factory from which this managed connection was
     * created.
     */
    private final SibRaManagedConnectionFactory _factory;

    /**
     * The associated <code>SICoreConnection</code>.
     */
    private final SICoreConnection _coreConnection;

    /**
     * The connection information that created the above core connection.
     */
    private final SibRaConnectionInfo _connectionInfo;

    /**
     * The set of <code>ConnectionEventListener</code> objects registered with
     * this managed connection.
     */
    private final Set _eventListeners = Collections.synchronizedSet(new HashSet());

    /**
     * The log writer for this managed connection. Not used.
     */
    private PrintWriter _logWriter;

    /**
     * The <code>SibRaConnection</code> handles currently associated with this
     * managed connection.
     */
    private final Set _connections = Collections.synchronizedSet(new HashSet());

    /**
     * The <code>LocalTransaction</code> for this managed connection. Created
     * lazily by <code>getLocalTransaction</code>.
     */
    private SibRaLocalTransaction _localTransaction;

    /**
     * The <code>XAResource</code> for this managed connection. Created lazily
     * by <code>getXAResource</code>.
     */
    private SibRaXaResource _xaResource;

    /**
     * The <code>ManagedConnectionMetaData</code> for this managed connection.
     * Created lazily by <code>getMetaData</code>.
     */
    private ManagedConnectionMetaData _metaData;

    /**
     * The connection listener for the core connection.
     */
    private final SibRaConnectionListener _connectionListener;

    /**
     * The last exception returned when there is a problem with getting a connection
     */
    private Exception _connectionException;

    /**
     * Indicates if this managedConnection has a valid connection. Will be
     * false when the me is terminated
     */
    private boolean _validConnection = true;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaManagedConnection.class);

    /**
     * The component to use for <code>LocalTransaction</code> trace.
     */
    private static final TraceComponent LOCAL_TRAN_TRACE = SibRaUtils
            .getTraceComponent(SibRaLocalTransaction.class);

    /**
     * The component to use for <code>LocalTransaction</code> trace.
     */
    private static final TraceComponent XA_RESOURCE_TRACE = SibRaUtils
            .getTraceComponent(SibRaXaResource.class);

    /**
     * Trace component fot the <code>ConnectionListener</code> trace.
     */
    private static TraceComponent LISTENER_TRACE = SibRaUtils
            .getTraceComponent (SibRaConnectionListener.class);

    /**
     * The <code>TraceNLS</code> to use with trace.
     */
    private static final TraceNLS NLS = SibRaUtils.getTraceNls();

    private static final String FFDC_PROBE_1 = "1";

    private static final String FFDC_PROBE_2 = "2";

    private static final String FFDC_PROBE_3 = "3";

    private static final String FFDC_PROBE_4 = "4";

    private static final String FFDC_PROBE_5 = "5";

    private static final String FFDC_PROBE_6 = "6";

    private static final String FFDC_PROBE_7 = "7";

    private static final String FFDC_PROBE_8 = "8";

    private static final String FFDC_PROBE_9 = "9";

    private static final String FFDC_PROBE_10 = "10";

    private static final String FFDC_PROBE_11 = "11";

    private static final String FFDC_PROBE_12 = "12";

    /**
     * Constructor.
     *
     * @param factory
     *            the managed connection factory on which this managed
     *            connection was created
     * @param connectionInfo
     *            the connection information with which the core conneciton was
     *            created
     * @param coreConnection
     *            the core connection
     */
    SibRaManagedConnection(final SibRaManagedConnectionFactory factory,
            final SibRaConnectionInfo connectionInfo,
            final SICoreConnection coreConnection) throws SIConnectionDroppedException,
            SIConnectionUnavailableException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "SibRaManagedConnection", new Object[] {
                    factory, connectionInfo, coreConnection });
        }

        _factory = factory;
        _connectionInfo = connectionInfo;
        _coreConnection = coreConnection;

        _connectionListener = new SibRaConnectionListener();
        _coreConnection.addConnectionListener(_connectionListener);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "SibRaManagedConnection");
        }

    }

    /**
     * Returns a connection handle for this managed connection. The resource
     * adapter does not support re-authentication so
     * <code>matchManagedConnection</code> should already have checked that
     * the <code>Subject</code> and request information are suitable for this
     * managed connection. The connection handle is given a clone of the core
     * SPI connection associated with this managed connection as is the request
     * information.
     *
     * @param containerSubject
     *            the container subject
     * @param requestInfo
     *            the connection request information
     * @return the connection handle
     * @throws ResourceException
     *             if the clone of the core SPI connection fails
     */
    public Object getConnection(final Subject containerSubject,
            final ConnectionRequestInfo requestInfo) throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr
                    .entry(this, TRACE, "getConnection", new Object[] {
                            SibRaUtils.subjectToString(containerSubject),
                            requestInfo });
        }

        SibRaConnection connection = null;

        if (requestInfo instanceof SibRaConnectionRequestInfo) {

            final SibRaConnectionRequestInfo sibRaRequestInfo = (SibRaConnectionRequestInfo) requestInfo;
            SICoreConnection coreConnection = null;

            try {
                _connectionException = null;
                coreConnection = _coreConnection.cloneConnection();

            } catch (final SIConnectionUnavailableException exception) {
                
                // No FFDC Code Needed
                // We will catch SIConnectionUnavailableException and SIConnectionDroppedException here
                connectionErrorOccurred(exception, false);
                _connectionException = exception;
                _validConnection = false;                         //PK60857
                
            } catch (SIException exception) {

                FFDCFilter
                        .processException(
                                exception,
                                "com.ibm.ws.sib.ra.impl.SibRaManagedConnection.getConnection",
                                FFDC_PROBE_1, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                connectionErrorOccurred(exception, false);
                _connectionException = exception;
                _validConnection = false;

            } catch (SIErrorException exception) {

                FFDCFilter
                        .processException(
                                exception,
                                "com.ibm.ws.sib.ra.impl.SibRaManagedConnection.getConnection",
                                FFDC_PROBE_7, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                connectionErrorOccurred(exception, false);
                _connectionException = exception;
                _validConnection = false;
            }

            if (coreConnection != null)
            {
              sibRaRequestInfo.setCoreConnection(coreConnection);
              connection = new SibRaConnection(this, sibRaRequestInfo,
                  coreConnection);
              _connections.add(connection);
            }
            else
            {
              connection = new SibRaConnection(this, sibRaRequestInfo, coreConnection);
            }

        } else {
            ResourceAdapterInternalException exception = new ResourceAdapterInternalException(NLS.getFormattedMessage(
                            "UNRECOGNISED_REQUEST_INFO_CWSIV0401", new Object[] {
                                    requestInfo, SibRaConnectionRequestInfo.class },
                            null));
            if (TRACE.isEventEnabled()) {
                 SibTr.exception(this, TRACE, exception);
            }
            throw exception;

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getConnection", connection);
        }
        return connection;

    }

    /**
     * Destroys this managed connection. Invalidates any current connection
     * handles and closes the core SPI connection.
     */
    public void destroy() throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "destroy");
        }

        // In the event of an error the connection manager may call destroy
        // without calling cleanup. We need to do the cleanup logic without
        // calling cleanup as we could throw an exception from cleanup.
        // Invalidate any currently associated connections
        for (Iterator iterator = _connections.iterator(); iterator.hasNext();) {

            final SibRaConnection connection = (SibRaConnection) iterator
                    .next();
            connection.invalidate();
        }

        try {

            // Close the core connection
            // when destroy calls close connections then connection cannot be reset for future usage so force close by passing boolean true.-PM39926
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

        } catch (SIException exception) {

            FFDCFilter.processException(exception,
                    "com.ibm.ws.sib.ra.impl.SibRaManagedConnection.destroy",
                    FFDC_PROBE_2, this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceException(NLS.getFormattedMessage(
                    "CONNECTION_CLOSE_CWSIV0402", new Object[] { exception },
                    null), exception);

        } catch (SIErrorException exception) {

            FFDCFilter.processException(exception,
                    "com.ibm.ws.sib.ra.impl.SibRaManagedConnection.destroy",
                    FFDC_PROBE_8, this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceException(NLS.getFormattedMessage(
                    "CONNECTION_CLOSE_CWSIV0402", new Object[] { exception },
                    null), exception);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "destroy");
        }

    }

    /**
     * Cleans up this managed connection prior to returning it to the free pool.
     * Invalidates any connection handles still associated with the managed
     * connection as, in the normal case, they would all have been dissociated
     * before cleanup was started.
     */
    public void cleanup() throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "cleanup");
        }

        // Invalidate any currently associated connections
        for (Iterator iterator = _connections.iterator(); iterator.hasNext();) {

            final SibRaConnection connection = (SibRaConnection) iterator
                    .next();
            connection.invalidate();

        }

        // If we have a connection exception then we must have hit a problem with getting the
        // physical connection. We need to throw an exception here to force the destroy of this
        // managed connection else it goes in the free pool. I better solution would be to expose
        // an api that we can call on jca, this is work in progress.
        if (_connectionException != null)
        {
          //The string used here is searched for by jca so that it won't log the error
            ResourceException exception = new ResourceException("Skip logging for this failing connection");
            if (TRACE.isEventEnabled()) {
              SibTr.exception(this, TRACE, exception);
            }
            throw exception;
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "cleanup");
        }

    }

    /**
     * Associates an existing connection handle with this managed connection.
     * The connection handle may still be associated with another managed
     * connection or may be dissociated.
     *
     * @param connection
     *            the connection handle
     * @throws ResourceAdapterInternalException
     *             if the connection does not belong to this resource adapter
     */
    public void associateConnection(final Object connection)
            throws ResourceAdapterInternalException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "associateConnection", connection);
        }

        if (connection instanceof SibRaConnection) {

            final SibRaConnection sibRaConnection = (SibRaConnection) connection;

            // Dissociate it from its old managed connection
            final SibRaManagedConnection oldManagedConnection = sibRaConnection
                    .getManagedConnection();
            if (oldManagedConnection != null) {
                oldManagedConnection._connections.remove(connection);
            }

            // Associate it with this managed connection
            sibRaConnection.setManagedConnection(this);
            _connections.add(sibRaConnection);

        } else {

            final ResourceAdapterInternalException exception = new ResourceAdapterInternalException(
                    NLS.getFormattedMessage(
                            "UNRECOGNISED_CONNECTION_CWSIV0403", new Object[] {
                                    connection, SibRaConnection.class }, null));
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw exception;

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "associateConnection");
        }

    }

    /**
     * Adds a connection event listener. Typically the connection manager will
     * register itself using this mechanism.
     *
     * @param listener
     *            the listener to add
     */
    public void addConnectionEventListener(
            final ConnectionEventListener listener) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "addConnectionEventListener", listener);
        }

        _eventListeners.add(listener);

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
    public void removeConnectionEventListener(
            final ConnectionEventListener listener) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "removeConnectionEventListener", listener);
        }

        _eventListeners.remove(listener);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "removeConnectionEventListener");
        }

    }

    /**
     * Gets an <code>XAResource</code> for this managed connection. The
     * <code>XAResource</code> implements <code>RecoverableXAResource</code>
     * so that the connection manager will ask it for its recovery token prior
     * to enlistment.
     *
     * @return the <code>XAResource</code>
     * @throws ResourceException
     *             if an <code>SIXAResource</code> could not be created
     */
    public XAResource getXAResource() throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getXAResource");
        }

        if (_xaResource == null) {

            _xaResource = new SibRaXaResource();

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getXAResource", _xaResource);
        }
        return _xaResource;

    }

    /**
     * Gets a <code>LocalTransaction</code> for this managed connection.
     *
     * @return the <code>LocalTransaction</code>
     */
    public LocalTransaction getLocalTransaction() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getLocalTransaction");
        }

        if (_localTransaction == null) {

            _localTransaction = new SibRaLocalTransaction();

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getLocalTransaction", _localTransaction);
        }
        return _localTransaction;

    }

    /**
     * Gets the meta data for this managed connection.
     *
     * @return the meta data
     */
    public ManagedConnectionMetaData getMetaData() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getMetaData");
        }

        if (_metaData == null) {

            _metaData = new SibRaMetaData();
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
     */
    public void setLogWriter(final PrintWriter logWriter) {

        _logWriter = logWriter;

    }

    /**
     * Gets the log writer for this managed connection.
     *
     * @return the log writer
     */
    public PrintWriter getLogWriter() {

        return _logWriter;

    }

    /**
     * Called by the connection manager to dissociate all the current connection
     * handles from this managed connection.
     */
    public void dissociateConnections() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "dissociateConnections");
        }

        for (Iterator iterator = _connections.iterator(); iterator.hasNext();) {

            final SibRaConnection connection = (SibRaConnection) iterator
                    .next();
            connection.setManagedConnection(null);

        }

        _connections.clear();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "dissociateConnections");
        }

    }

    /**
     * Returns <code>true</code> if the given parameters provide a suitable
     * match with this managed connection. If the core connection is not
     * <code>null</code> it must be equivalent to the core connection for this
     * managed connection. If it is <code>null</code> then the connection
     * information must be the same as for this managed connection. Used by
     * <code>matchManagedConnections</code>.
     *
     * @param connectionInfo
     *            the connection information
     * @param coreConnection
     *            the core connection
     * @return flag indicating whether there is a match
     */
    boolean matches(final SibRaConnectionInfo connectionInfo,
            final SICoreConnection coreConnection) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "matches", new Object[] { connectionInfo,
                    coreConnection });
        }

        final boolean match;
        
        if (!isValid ())
        {
            match = false;
        }
        else if (coreConnection != null) {

            match = _coreConnection.isEquivalentTo(coreConnection);

        } else {

            match = _connectionInfo.equals(connectionInfo);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "matches", Boolean.valueOf(match));
        }
        return match;

    }

    /**
     * Returns the current container transaction. If we don't already have a
     * transaction and the given connection manager supports it, perform a lazy
     * enlist and then check again.
     *
     * @param connectionManager
     *            the connection manager
     * @return the current container transaction
     * @throws ResourceException
     *             if lazy enlistment fails
     */
    SITransaction getContainerTransaction(
            final ConnectionManager connectionManager) throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getContainerTransaction",
                    connectionManager);
        }

        SITransaction currentTransaction = getCurrentTransaction();

        if ((currentTransaction == null)
                && (connectionManager instanceof LazyEnlistableConnectionManager)) {

            ((LazyEnlistableConnectionManager) connectionManager)
                    .lazyEnlist(this);
            currentTransaction = getCurrentTransaction();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getContainerTransaction",
                    currentTransaction);
        }
        return currentTransaction;

    }

    /**
     * Returns the current transaction associated with this managed connection.
     * If the connection manager supports lazy enlistment then this may not
     * represent the current transaction on the thread.
     *
     * @return the current transaction
     */
    private SITransaction getCurrentTransaction() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getCurrentTransaction");
        }

        SITransaction currentTransaction = null;

        // Is there a current local transaction?

        if (_localTransaction != null) {

            currentTransaction = _localTransaction.getCurrentTransaction();

        }

        // If not, is there a current global transaction?

        if ((currentTransaction == null) && (_xaResource != null)) {

            currentTransaction = _xaResource.getCurrentTransaction();

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr
                    .exit(this, TRACE, "getCurrentTransaction",
                            currentTransaction);
        }
        return currentTransaction;

    }

    /**
     * Used to indicate that an application local transaction has been started.
     * Notifies the connection event listeners.
     *
     * @param connection
     *            the connection on which the transaction was started
     */
    void localTransactionStarted(final SibRaConnection connection) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "localTransactionStarted", connection);
        }

        final ConnectionEvent event = new ConnectionEvent(this,
                ConnectionEvent.LOCAL_TRANSACTION_STARTED);
        event.setConnectionHandle(connection);

        for (Iterator iterator = _eventListeners.iterator(); iterator.hasNext();) {

            final ConnectionEventListener listener = (ConnectionEventListener) iterator
                    .next();
            listener.localTransactionStarted(event);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "localTransactionStarted");
        }
    }

    /**
     * Used to indicate that an application local transaction has been
     * committed. Notifies the connection event listeners.
     *
     * @param connection
     *            the connection on which the transaction was started
     */
    void localTransactionCommitted(final SibRaConnection connection) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "localTransactionCommitted", connection);
        }

        final ConnectionEvent event = new ConnectionEvent(this,
                ConnectionEvent.LOCAL_TRANSACTION_COMMITTED);
        event.setConnectionHandle(connection);

        for (Iterator iterator = _eventListeners.iterator(); iterator.hasNext();) {

            final ConnectionEventListener listener = (ConnectionEventListener) iterator
                    .next();
            listener.localTransactionCommitted(event);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "localTransactionStarted");
        }

    }

    /**
     * Used to indicate that an application local transaction has been rolled
     * back. Notifies the connection event listeners.
     *
     * @param connection
     *            the connection on which the transaction was started
     */
    void localTransactionRolledBack(final SibRaConnection connection) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "localTransactionRolledBack", connection);
        }

        final ConnectionEvent event = new ConnectionEvent(this,
                ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK);
        event.setConnectionHandle(connection);

        for (Iterator iterator = _eventListeners.iterator(); iterator.hasNext();) {

            final ConnectionEventListener listener = (ConnectionEventListener) iterator
                    .next();
            listener.localTransactionRolledback(event);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "localTransactionRolledBack");
        }

    }

    /**
     * Used to indicate that a connection has been closed. Notifies the
     * connection event listeners and removes the connection from the set of
     * open connections.
     *
     * @param connection
     *            the connection being closed
     */
    void connectionClosed(final SibRaConnection connection) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "connectionClosed", connection);
        }

        final ConnectionEvent event = new ConnectionEvent(this,
                ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(connection);

        for (Iterator iterator = _eventListeners.iterator(); iterator.hasNext();) {

            final ConnectionEventListener listener = (ConnectionEventListener) iterator
                    .next();
            listener.connectionClosed(event);

        }

        _connections.remove(connection);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "connectionClosed");
        }

    }

    /**
     * Notifies the connection even listeners that a connection error has
     * occurred.
     *
     * @param exception
     *            the exception, if any, that is the cause of the error
     * @param callJCAListener whether we shoud call the listeners or not
     */
    final void connectionErrorOccurred(final Exception exception, boolean callJCAListener) {

        final String methodName = "connectionErrorOccurred";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] {exception, Boolean.valueOf(callJCAListener)});
        }

        /*
         * Only send the event if the server isn't shutting down anyway
         */

        if (!SibRaEngineComponent.isServerStopping()) {

            final ConnectionEvent event = new ConnectionEvent(this,
                    ConnectionEvent.CONNECTION_ERROR_OCCURRED, exception);

            // Copy list to protect against concurrent modification by listener
            final List copy;
            synchronized (_eventListeners) {
                copy = new ArrayList(_eventListeners);
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
     * @return the last exception we gathered when there was an error
     */
    public Exception getConnectionException()
    {
      return _connectionException;
    }

    public boolean isValid()
    {
      return _validConnection;
    }

    /**
     * Implementation of <code>ManagedConnectionMetaData</code> for the core
     * SPI resource adapter.
     */
    private final class SibRaMetaData implements ManagedConnectionMetaData {

        /**
         * Returns the product name
         *
         * @return the product name
         */
        public String getEISProductName() {

            return "";

        }

        /**
         * Returns the product version
         *
         * @return the product version
         */
        public String getEISProductVersion() {

            return "";

        }

        /**
         * Returns the maximum number of connections (or zero if there is no
         * maximum).
         *
         * @return the maximum number of connections
         */
        public int getMaxConnections() {

            // No maximum
            return 0;

        }

        /**
         * Returns the user name for this managed connection.
         *
         * @return the user name
         */
        public String getUserName() {

            return _connectionInfo.getUserName();

        }

    }

    /**
     * Implementation of <code>LocalTransaction</code> for the core SPI
     * resource adapter. Creates a new <code>SIUncoordinatedTransaction</code>
     * on <code>begin</code> and then delegates the <code>commit</code> or
     * <code>rollback</code> to this. If a completion call is received with no
     * corresponding <code>begin</code> then this indicates that the container
     * is attempting to complete a local transaction started by the application.
     */
    private final class SibRaLocalTransaction implements LocalTransaction {

        /**
         * The current <code>SIUncoordinatedTransaction</code>. Set by
         * <code>begin</code> and unset by <code>commit</code>/
         * <code>rollback</code>.
         */
        private SIUncoordinatedTransaction _uncoordinatedTransaction;

        /**
         * Begins a container local transaction. Creates a core SPI
         * <code>SIUncoordinatedTransaction</code>.
         *
         * @throws LocalTransactionException
         *             if there is already an active container local transaction
         * @throws ResourceException
         *             if the creation of the
         *             <code>SIUncoordinatedTransaction</code> fails
         */
        public void begin() throws ResourceException {

            if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRAN_TRACE.isEntryEnabled()) {
                SibTr.entry(this, LOCAL_TRAN_TRACE, "begin");
            }

            // Check that we don't already have an active transaction
            if (_uncoordinatedTransaction != null) {

                final LocalTransactionException exception = new LocalTransactionException(
                        NLS.getString("ACTIVE_LOCAL_TRAN_CWSIV0404"));
                if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRAN_TRACE.isEventEnabled()) {
                    SibTr.exception(this, LOCAL_TRAN_TRACE, exception);
                }
                throw exception;

            }

            try {

                _uncoordinatedTransaction = _coreConnection
                        .createUncoordinatedTransaction();

            } catch (SIException exception) {

                FFDCFilter
                        .processException(
                                exception,
                                "com.ibm.ws.sib.ra.impl.SibRaManagedConnection$SibRaLocalTransaction.begin",
                                FFDC_PROBE_3, this);
                if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRAN_TRACE.isEventEnabled()) {
                    SibTr.exception(this, LOCAL_TRAN_TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                        "LOCAL_TRAN_BEGIN_CWSIV0405",
                        new Object[] { exception }, null), exception);

            } catch (SIErrorException exception) {

                FFDCFilter
                        .processException(
                                exception,
                                "com.ibm.ws.sib.ra.impl.SibRaManagedConnection$SibRaLocalTransaction.begin",
                                FFDC_PROBE_9, this);
                if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRAN_TRACE.isEventEnabled()) {
                    SibTr.exception(this, LOCAL_TRAN_TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                        "LOCAL_TRAN_BEGIN_CWSIV0405",
                        new Object[] { exception }, null), exception);

            }

            if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRAN_TRACE.isEntryEnabled()) {
                SibTr.exit(this, LOCAL_TRAN_TRACE, "begin");
            }

        }

        /**
         * Commits a local transaction. If there is an active container local
         * transaction, this is committed otherwise the current connection
         * handle is asked to commit any outstanding application local
         * transaction.
         *
         * @throws ResourceException
         *             if the commit fails
         */
        public void commit() throws ResourceException {

            if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRAN_TRACE.isEntryEnabled()) {
                SibTr.entry(this, LOCAL_TRAN_TRACE, "commit");
            }

            if (_uncoordinatedTransaction == null) {

                // Commit application local transactions

                for (Iterator iterator = _connections.iterator(); iterator
                        .hasNext();) {

                    final SibRaConnection connection = (SibRaConnection) iterator
                            .next();
                    connection.commitApplicationLocalTransaction();

                }

            } else {

                try {

                    _uncoordinatedTransaction.commit();

                } catch (SIException exception) {

                    FFDCFilter
                            .processException(
                                    exception,
                                    "com.ibm.ws.sib.ra.impl.SibRaManagedConnection$SibRaLocalTranssaction.commit",
                                    FFDC_PROBE_4, this);
                    if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRAN_TRACE.isEventEnabled()) {
                        SibTr.exception(this, LOCAL_TRAN_TRACE, exception);
                    }
                    throw new ResourceException(NLS.getFormattedMessage(
                            "LOCAL_TRAN_COMMIT_CWSIV0406",
                            new Object[] { exception }, null), exception);

                } catch (SIErrorException exception) {

                    FFDCFilter
                            .processException(
                                    exception,
                                    "com.ibm.ws.sib.ra.impl.SibRaManagedConnection$SibRaLocalTranssaction.commit",
                                    FFDC_PROBE_10, this);
                    if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRAN_TRACE.isEventEnabled()) {
                        SibTr.exception(this, LOCAL_TRAN_TRACE, exception);
                    }
                    throw new ResourceException(NLS.getFormattedMessage(
                            "LOCAL_TRAN_COMMIT_CWSIV0406",
                            new Object[] { exception }, null), exception);

                }
                finally
                {
                  // set the uncoordinatedTransaction to null, so we can begin a new one later
                  _uncoordinatedTransaction = null;
                }

            }

            if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRAN_TRACE.isEntryEnabled()) {
                SibTr.exit(this, LOCAL_TRAN_TRACE, "commit");
            }

        }

        /**
         * Rolls back a local transaction. If there is an active container local
         * transaction, this is rolled back otherwise the current connection
         * handle is asked to rollback any outstanding application local
         * transaction.
         *
         * @throws ResourceException
         *             if the rollback fails
         */
        public void rollback() throws ResourceException {

            if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRAN_TRACE.isEntryEnabled()) {
                SibTr.entry(this, LOCAL_TRAN_TRACE, "rollback");
            }

            if (_uncoordinatedTransaction == null) {

                // Rollback application local transactions

                for (Iterator iterator = _connections.iterator(); iterator
                        .hasNext();) {

                    final SibRaConnection connection = (SibRaConnection) iterator
                            .next();
                    connection.rollbackApplicationLocalTransaction();

                }

            } else {

                try {

                    _uncoordinatedTransaction.rollback();

                } catch (SIException exception) {

                    FFDCFilter
                            .processException(
                                    exception,
                                    "com.ibm.ws.sib.ra.impl.SibRaManagedConnection$SibRaLocalTranssaction.rollback",
                                    FFDC_PROBE_5, this);
                    if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRAN_TRACE.isEventEnabled()) {
                        SibTr.exception(this, LOCAL_TRAN_TRACE, exception);
                    }
                    // Don't throw an exception as this is during a rollback and we
                    // assume that a rollback will happen regardless as to whether the
                    // rollback call was success.

                } catch (SIErrorException exception) {

                    FFDCFilter
                            .processException(
                                    exception,
                                    "com.ibm.ws.sib.ra.impl.SibRaManagedConnection$SibRaLocalTranssaction.rollback",
                                    FFDC_PROBE_11, this);
                    if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRAN_TRACE.isEventEnabled()) {
                        SibTr.exception(this, LOCAL_TRAN_TRACE, exception);
                    }
                    // Don't throw an exception as this is during a rollback and we
                    // assume that a rollback will happen regardless as to whether the
                    // rollback call was success.

                }
                finally
                {
                  // set the uncoordinatedTransaction to null, so we can begin a new one later
                  _uncoordinatedTransaction = null;
                }

            }

            if (TraceComponent.isAnyTracingEnabled() && LOCAL_TRAN_TRACE.isEntryEnabled()) {
                SibTr.exit(this, LOCAL_TRAN_TRACE, "rollback");
            }

        }

        /**
         * Returns the current <code>SIUncoordinatedTransaction</code>
         * associated with this managed connection representing a container
         * local transaction.
         *
         * @return the current transaction, if any
         */
        private SIUncoordinatedTransaction getCurrentTransaction() {

            return _uncoordinatedTransaction;

        }

    }

    /**
     * Implementation of <code>RecoverableXAResource</code> for the core SPI
     * resource adapter. Holds a reference to a core SPI
     * <code>SIXAResource</code> to which methods delegate and obtains a
     * recovery token from the <code>SibRaRecoveryManager</code>.
     */
    private final class SibRaXaResource implements XAResource {

        /**
         * The <code>SIXAResouce</code> to which methods delegate.
         */
        private final SIXAResource _delegateXaResource;


        /**
         * Constructor. Obtains an <code>SIXAResource</code> from the managed
         * connection's core SPI connection and a corresponding recovery token.
         *
         * @throws ResourceException
         *             if the delegate <code>XAResource</code> could not be
         *             created
         */
        public SibRaXaResource() throws ResourceException {

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.entry(this, TRACE, "SibRaXaResource");
            }


            try {

                _delegateXaResource = _coreConnection.getSIXAResource();

            } catch (SIException exception) {

                FFDCFilter
                        .processException(
                                exception,
                                "com.ibm.ws.sib.ra.impl.SibRaManagedConnection$SibRaXaResource.SibRaXaResource",
                                FFDC_PROBE_6, this);
                if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEventEnabled()) {
                    SibTr.exception(this, XA_RESOURCE_TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                        "XARESOURCE_CREATE_CWSIV0408",
                        new Object[] { exception }, null), exception);

            } catch (SIErrorException exception) {

                FFDCFilter
                        .processException(
                                exception,
                                "com.ibm.ws.sib.ra.impl.SibRaManagedConnection$SibRaXaResource.SibRaXaResource",
                                FFDC_PROBE_12, this);
                if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEventEnabled()) {
                    SibTr.exception(this, XA_RESOURCE_TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                        "XARESOURCE_CREATE_CWSIV0408",
                        new Object[] { exception }, null), exception);

            }

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.exit(this, TRACE, "SibRaXaResource");
            }

        }


        /**
         * Commits the transaction represented by the given <code>Xid</code>.
         * Delegates.
         *
         * @param xid
         *            the <code>Xid</code>
         * @param onePhase
         *            flag indicating whether a one phase optimisation is being
         *            used
         * @throws XAException
         *             if the delegation fails
         */
        public void commit(final Xid xid, final boolean onePhase)
                throws XAException {

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.entry(this, XA_RESOURCE_TRACE, "commit", new Object[] {
                        xid, Boolean.valueOf(onePhase) });
            }

            _delegateXaResource.commit(xid, onePhase);

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.exit(this, XA_RESOURCE_TRACE, "commit");
            }

        }

        /**
         * Ends the association between this resource manager and the
         * transaction represented by the given <code>Xid</code>.
         *
         * @param xid
         *            the <code>Xid</code>
         * @param flags
         *            one of <code>TMSUCCESS</code>,<code>TMFAIL</code>,
         *            or <code>TMSUSPEND</code>
         * @throws XAException
         *             if the delegation fails
         */
        public void end(final Xid xid, final int flags) throws XAException {

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.entry(this, XA_RESOURCE_TRACE, "end", new Object[] { xid, flags });
            }

            _delegateXaResource.end(xid, flags);

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.exit(this, XA_RESOURCE_TRACE, "end");
            }

        }

        /**
         * Tell the resource manager to forget about a heuristically completed
         * transaction branch represented by the given <code>Xid</code>.
         * Delegates.
         *
         * @param xid
         *            the <code>Xid</code>
         * @throws XAException
         *             if the delegation fails
         */
        public void forget(final Xid xid) throws XAException {

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.entry(this, XA_RESOURCE_TRACE, "forget", xid);
            }

            _delegateXaResource.forget(xid);

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.exit(this, XA_RESOURCE_TRACE, "forget");
            }

        }

        /**
         * Returns the transaction timeout. Delegates.
         *
         * @return the transaction timeout
         * @throws XAException
         *             if the delegation fails
         */
        public int getTransactionTimeout() throws XAException {

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.entry(this, XA_RESOURCE_TRACE, "getTransactionTimeout");
            }

            final int result = _delegateXaResource.getTransactionTimeout();

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.exit(this, XA_RESOURCE_TRACE, "getTransactionTimeout", result);
            }
            return result;

        }

        /**
         * Returns <code>true</code> if the given <code>XAResource</code>
         * represents the same resource manager as this one. Delegates.
         *
         * @return <code>true</code> if it is the same resource manager.
         * @throws XAException
         *             if the delegation fails
         */
        public boolean isSameRM(final XAResource other) throws XAException {

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.entry(this, XA_RESOURCE_TRACE, "isSameRM", other);
            }

            final boolean sameRm;

            // Unwrap the other XAResource if is one of ours
            if (other instanceof SibRaXaResource) {

                final SibRaXaResource otherSibRaXaResource = (SibRaXaResource) other;
                sameRm = _delegateXaResource
                        .isSameRM(otherSibRaXaResource._delegateXaResource);

            } else {

                sameRm = _delegateXaResource.isSameRM(other);

            }

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.exit(this, XA_RESOURCE_TRACE, "isSameRM", Boolean
                        .valueOf(sameRm));
            }
            return sameRm;

        }

        /**
         * Prepares the transaction represented by the given <code>Xid</code>.
         * Delegates.
         *
         * @param xid
         *            the xid
         * @return the resource manager's vote of <code>XA_RDONLY</code> or
         *         <code>XA_OK</code>
         * @throws XAException
         *             if the delegation fails
         */
        public int prepare(final Xid xid) throws XAException {

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.entry(this, XA_RESOURCE_TRACE, "prepare", xid);
            }

            final int result = _delegateXaResource.prepare(xid);

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.exit(this, XA_RESOURCE_TRACE, "prepare", result);
            }
            return result;

        }

        /**
         * Returns an array of indoubt <code>Xid</code> s for this resource
         * manager. Delegates.
         *
         * @param flags
         *            one of <code>TMSTARTRSCAN</code>,
         *            <code>TMENDRSCAN</code> and <code>TMNOFLAGS</code>
         * @return the array of <code>Xid</code> s
         * @throws XAException
         *             if the delegation fails
         */
        public Xid[] recover(final int flags) throws XAException {

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.entry(this, XA_RESOURCE_TRACE, "recover", flags);
            }

            final Xid[] result = _delegateXaResource.recover(flags);

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.exit(this, XA_RESOURCE_TRACE, "recover", result);
            }
            return result;

        }

        /**
         * Rolls back the transaction associated with the given <code>Xid</code>.
         *
         * @param xid
         *            the <code>Xid</code>
         * @throws XAException
         *             if the delegation fails
         */
        public void rollback(final Xid xid) throws XAException {

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.entry(this, XA_RESOURCE_TRACE, "rollback", xid);
            }

            _delegateXaResource.rollback(xid);

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.exit(this, XA_RESOURCE_TRACE, "rollback");
            }

        }

        /**
         * Sets the transaction timeout. Delegates.
         *
         * @param timeout
         *            the timeout
         * @return <code>true</code> if the timeout is set successfully
         * @throws XAException
         *             if the delegation fails
         */
        public boolean setTransactionTimeout(final int timeout)
                throws XAException {

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.entry(this, XA_RESOURCE_TRACE, "setTransactionTimeout",
                        Integer.valueOf(timeout));
            }

            final boolean result = _delegateXaResource
                    .setTransactionTimeout(timeout);

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.exit(this, XA_RESOURCE_TRACE, "setTransactionTimeout",
                        Boolean.valueOf(result));
            }
            return result;

        }

        /**
         * Starts the association between the given <code>Xid</code> and this
         * resource manager. Delegates.
         *
         * @param xid
         *            the <code>Xid</code>
         * @param flags
         *            one of <code>TMNOFLAGS</code>,<code>TMJOIN</code>,
         *            or <code>TMRESUME</code>
         * @throws XAException
         *             if the delegation fails
         */
        public void start(final Xid xid, final int flags) throws XAException {

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.entry(this, XA_RESOURCE_TRACE, "start", new Object[] {
                        xid, flags });
            }

            _delegateXaResource.start(xid, flags);

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.exit(this, XA_RESOURCE_TRACE, "start");
            }

        }

        /**
         * Returns the associated <code>SIXAResource</code> if currently
         * enlisted in a transaction.
         *
         * @return the current transaction
         */
        private SIXAResource getCurrentTransaction() {

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.entry(this, XA_RESOURCE_TRACE, "getCurrentTransaction");
            }

            final SIXAResource currentTransaction;

            if (_delegateXaResource.isEnlisted()) {

                currentTransaction = _delegateXaResource;

            } else {

                currentTransaction = null;

            }

            if (TraceComponent.isAnyTracingEnabled() && XA_RESOURCE_TRACE.isEntryEnabled()) {
                SibTr.exit(this, XA_RESOURCE_TRACE, "getCurrentTransaction",
                        currentTransaction);
            }
            return currentTransaction;

        }

    }

    /**
     * Listener used to receive notfication of the failure of the core
     * connection associated with this managed connection.
     */
    private final class SibRaConnectionListener implements
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
        public void meQuiescing(final SICoreConnection connection) {

            if (TraceComponent.isAnyTracingEnabled() && LISTENER_TRACE.isEntryEnabled()) {
                final String methodName = "meQuiescing";
                SibTr.entry(this, LISTENER_TRACE, methodName, connection);
                SibTr.exit(this, LISTENER_TRACE, methodName);
            }

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
