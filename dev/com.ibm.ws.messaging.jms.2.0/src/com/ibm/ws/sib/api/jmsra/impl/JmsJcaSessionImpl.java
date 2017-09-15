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
package com.ibm.ws.sib.api.jmsra.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
//Sanjay Liberty Changes
/*
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.IllegalStateException;
import javax.resource.spi.LazyAssociatableConnectionManager;
import javax.resource.spi.LocalTransactionException;
import javax.resource.spi.ManagedConnectionFactory;
*/
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.LazyAssociatableConnectionManager;
import javax.resource.spi.LocalTransactionException;
import javax.resource.spi.ManagedConnectionFactory;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.api.jmsra.JmsJcaSession;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SIUncoordinatedTransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * Provides a scope within which JMS work may be performed and, in particular,
 * transactional integration with the application server. There will be a
 * one-to-one relationship between JMS sessions and objects implementing this
 * interface. The resource adapter supports disassociation so the session will
 * be associated with a managed connection except between dissociate and
 * reassociate. Created by the unfortunately named <code>getConnection</code>
 * method on the managed connection.
 */
final class JmsJcaSessionImpl implements JmsJcaSession {

    /**
     * The managed connection associated with this session.
     */
    private JmsJcaManagedConnection _managedConnection;

    /**
     * The parent connection.
     */
    private JmsJcaConnectionImpl _connection;

    /**
     * Flag indicated whether the session is transacted.
     */
    private boolean _transacted;

    /**
     * The current application local transaction.
     */
    private SIUncoordinatedTransaction _applicationLocalTransaction;

    /**
     * The request information that led to the creation of this session.
     */
    private final JmsJcaConnectionRequestInfo _requestInfo;

    /**
     * Flag to keep track of whether the connection has been closed.
     */
    private boolean _sessionClosed = false;

    /**
     * Flag to keep track of whether the connection has been closed.
     */
    private boolean _sessionInvalidated = false;

    private static TraceComponent TRACE = SibTr.register(
            JmsJcaSessionImpl.class, JmsraConstants.MSG_GROUP,
            JmsraConstants.MSG_BUNDLE);

    private static TraceNLS NLS = TraceNLS
            .getTraceNLS(JmsraConstants.MSG_BUNDLE);

    private static final String CLASS_NAME = JmsJcaSessionImpl.class.getName();

    /**
     * Constructs a <code>JmsJcaSessionImpl</code> given an initial managed
     * connection.
     *
     * @param managedConnection
     *            the managed connection
     * @param requestInfo
     *            The connection request info used to create the session.
     */
    JmsJcaSessionImpl(final JmsJcaManagedConnection managedConnection,
            final JmsJcaConnectionRequestInfo requestInfo) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "JmsJcaSessionImpl", new Object[] {
                    managedConnection, requestInfo});
        }

        _managedConnection = managedConnection;
        _requestInfo = requestInfo;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "JmsJcaSessionImpl");
        }

    }

    /**
     * Constructs a <code>JmsJcaSessionImpl</code>
     */
    JmsJcaSessionImpl() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "JmsJcaSessionImpl");
        }

        _requestInfo = null;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "JmsJcaSessionImpl");
        }

    }

    /**
     * A convenience method that returns the core connection associated with
     * this session's connection.
     *
     * @return the core connection
     * @throws IllegalStateException
     *             if this session has been closed or invalidated
     */
    public SICoreConnection getSICoreConnection() throws IllegalStateException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getSICoreConnection");
        }

        if (_sessionClosed) {
            throw new IllegalStateException(NLS.getFormattedMessage(
                    ("ILLEGAL_STATE_CWSJR1123"),
                    new Object[] { "getSICoreConnection"}, null));
        }
        if (_sessionInvalidated) {
            throw new IllegalStateException(NLS.getFormattedMessage(
                    ("ILLEGAL_STATE_CWSJR1124"),
                    new Object[] { "getSICoreConnection"}, null));
        }

        SICoreConnection coreConnection = null;
        if (_connection != null) {
            coreConnection = _connection.getSICoreConnection();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getSICoreConnection", coreConnection);
        }
        return coreConnection;

    }

    /**
     * Returns the current core transaction object for this session.
     * Re-associates with a managed connection if currently disassociated. If
     * there is currently an application local transaction associated with this
     * session then there cannot be a container transaction so this local
     * transaction is returned immediately. Otherwise, calls
     * <code>getCurrentTransaction</code> on the managed connection to obtain
     * the current container global or local transaction. If there is one, this
     * is returned to the caller. If <code>null</code> is returned and the
     * session is transacted then a local transaction is created on the
     * associated core connection, <code>localTransactionStarted</code> called
     * on the managed connection and the local transaction returned. Otherwise,
     * <code>null</code> is returned.
     *
     * @return the current transaction, if any
     * @throws IllegalStateException
     *             if this session has been closed or invalidated. This can also
     *             be thrown if the connection is not associated with a managed
     *             connection and the connection manager does not support lazy
     *             enlistment
     * @throws ResourceException
     *             if a lazy enlist was required and failed
     * @throws SIException
     *             if the call resulted in a failed attempt to start a
     *             transaction
     * @throws SIErrorException
     *             if the call resulted in a failed attempt to start a
     *             transaction
     */
    public SITransaction getCurrentTransaction() throws IllegalStateException,
            ResourceException, SIException, SIErrorException
    {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getCurrentTransaction");
        }

        if (_sessionClosed) {
            throw new IllegalStateException(NLS.getFormattedMessage(
                    ("ILLEGAL_STATE_CWSJR1130"),
                    new Object[] { "getCurrentTransaction"}, null));
        }
        if (_sessionInvalidated) {
            throw new IllegalStateException(NLS.getFormattedMessage(
                    ("ILLEGAL_STATE_CWSJR1134"),
                    new Object[] { "getCurrentTransaction"}, null));
        }

        SITransaction transaction = null;

        if (_applicationLocalTransaction != null) {
            transaction = _applicationLocalTransaction;
        } else {

            if (!_connection.getConnectionFactory().isManaged()) {

                if (getTransacted()) {
                    _applicationLocalTransaction = _connection
                            .getSICoreConnection()
                            .createUncoordinatedTransaction();

                    // set rtn value..
                    transaction = _applicationLocalTransaction;
                }

            } else {

               transaction = getManagedConnection().getCurrentTransaction(
                            _connection.getConnectionManager());

                if ((transaction == null) && (getTransacted())) {

                    try {

                        _applicationLocalTransaction = _connection
                                .getSICoreConnection()
                                .createUncoordinatedTransaction();

                    } catch (final SIException exception) {

                        FFDCFilter.processException(exception, CLASS_NAME
                                + ".getCurrentTransaction", "1:306:1.47", this);
                        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                            SibTr.exception(this, TRACE, exception);
                        }
                        throw exception;

                    } catch (final SIErrorException exception) {

                        FFDCFilter.processException(exception, CLASS_NAME
                                + ".getCurrentTransaction", "1:315:1.47", this);
                        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                            SibTr.exception(this, TRACE, exception);
                        }
                        throw exception;

                    }

                    _managedConnection.localTransactionStarted();

                    // set rtn value..
                    transaction = _applicationLocalTransaction;
                }

            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, "getCurrentTransaction", transaction);
        }
        return transaction;
    }

    /**
     * Returns the transacted flag. This will be set before the object is
     * returned from <code>createSession</code> which may, in the case of the
     * first session, be some time after the construction of the session. This
     * flag indicates whether, in the absence of a global or container local
     * transaction, work should be performed inside an application local
     * transaction.
     *
     * @return the transacted flag
     * @throws javax.resource.spi.IllegalStateException
     *             if this session has been closed or invalidated
     */
    public boolean getTransacted() throws IllegalStateException {

        if (_sessionClosed) {
            throw new IllegalStateException(NLS.getFormattedMessage(
                    ("ILLEGAL_STATE_CWSJR1131"),
                    new Object[] { "getTransacted"}, null));
        }
        if (_sessionInvalidated) {
            throw new IllegalStateException(NLS.getFormattedMessage(
                    ("ILLEGAL_STATE_CWSJR1135"),
                    new Object[] { "getTransacted"}, null));
        }

        return _transacted;
    }

    /**
     * Commits the current application local transaction. Calls
     * <code>localTransactionCommitted</code> on the managed connection so
     * that the connection manager is notified.
     *
     * Used in Outbound Diagram 16
     *
     * @throws IllegalStateException
     *             if this session has been closed. This can also
     *             be thrown if the connection is not associated with a managed
     *             connection and the connection manager does not support lazy
     *             enlistment
     * @throws LocalTransactionException
     *             if there is currently no local transaction for this session
     * @throws ResourceException
     *             if a lazy enlist was required and failed
     * @throws SIException
     *             if the commit fails
     * @throws SIErrorException
     *             if the commit fails
     */
    public void commitLocalTransaction() throws IllegalStateException,
            LocalTransactionException, ResourceException, SIException,
            SIErrorException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "commitLocalTransaction");
        }

        if (_sessionClosed) {
            throw new IllegalStateException(NLS.getFormattedMessage(
                    ("ILLEGAL_STATE_CWSJR1132"),
                    new Object[] { "commitLocalTransaction"}, null));
        }
        if (_sessionInvalidated) {
            throw new IllegalStateException(NLS.getFormattedMessage(
                    ("ILLEGAL_STATE_CWSJR1136"),
                    new Object[] { "commitLocalTransaction"}, null));
        }
        if (!getTransacted()) {
            throw new LocalTransactionException(NLS.getFormattedMessage(
                    ("ILLEGAL_STATE_CWSJR1125"),
                    new Object[] { "commitLocalTransaction"}, null));
        }

        if (_applicationLocalTransaction == null) {

            if (_connection.getConnectionFactory().isManaged()) {

                final SITransaction transaction = getManagedConnection()
                        .getCurrentTransaction(
                                _connection.getConnectionManager());

                if (transaction != null) {

                    // PK57931 if transaction is a local tran and _applicationLocalTransaction is null
                    // this means someone else created the local tran
                    if (transaction instanceof SIUncoordinatedTransaction) {
                         if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                             SibTr.debug(TRACE, "_applicationLocalTransaction was null but current tran is local.  Committing local tran.");
                         }
                         ((SIUncoordinatedTransaction)transaction).commit();
                    } else {
                        // transaction is a global tran but this method is commitLocalTransaction
                        throw new LocalTransactionException(NLS
                                .getFormattedMessage(("ILLEGAL_STATE_CWSJR1138"),
                                        new Object[] { "commitLocalTransaction"},
                                        null));
                    }
                }

            }

            // Quietly do nothing if we don't have a global or container local
            // transaction and we are transacted (the application local
            // transaction has not yet been created).

        } else {

            try {

                _applicationLocalTransaction.commit();

            } catch (SIRollbackException rolledbackException)   {

                // No FFDC code needed

                // The commit failed with a rollback exception so inform the
                // connection manager (vai the managed connection) that the
                // transaction has been rolled back. Then reset the transaction
                // to null so we don't need to roll it back when the session
                // is closed.
                if (_connection.getConnectionFactory().isManaged()) {
                    _managedConnection.localTransactionRolledBack();
                }

                _applicationLocalTransaction = null;

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, rolledbackException);
                }

                throw rolledbackException;

            } catch (final SIException exception) {

                FFDCFilter.processException(exception, CLASS_NAME
                        + ".commitLocalTranaction", "1:473:1.47", this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw exception;

            } catch (final SIErrorException exception) {

                FFDCFilter.processException(exception, CLASS_NAME
                        + ".commitLocalTranaction", "1:482:1.47", this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw exception;

            }

            if (_connection.getConnectionFactory().isManaged()) {
                _managedConnection.localTransactionCommitted();
            }

            _applicationLocalTransaction = null;

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "commitLocalTransaction");
        }

    }

    /**
     * Rolls back the current application local transaction. Calls
     * <code>localTransactionRolledBack</code> on the managed connection so
     * that the connection manager is notified.
     *
     * @throws IllegalStateException
     *             if this session has been closed. This can also
     *             be thrown if the connection is not associated with a managed
     *             connection and the connection manager does not support lazy
     *             enlistment
     * @throws LocalTransactionException
     *             if there is currently no local transaction for this session
     * @throws ResourceException
     *             if a lazy enlist was required and failed
     * @throws SIException
     *             if the rollback fails
     * @throws SIErrorException
     *             if the rollback fails
     */
    public void rollbackLocalTransaction() throws IllegalStateException,
            LocalTransactionException, ResourceException, SIException,
            SIErrorException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "rollbackLocalTransaction");
        }

        if (_sessionClosed) {
            throw new IllegalStateException(NLS.getFormattedMessage(
                    ("ILLEGAL_STATE_CWSJR1133"),
                    new Object[] { "rollbackLocalTransaction"}, null));
        }
        if (_sessionInvalidated) {
            throw new IllegalStateException(NLS.getFormattedMessage(
                    ("ILLEGAL_STATE_CWSJR1137"),
                    new Object[] { "rollbackLocalTransaction"}, null));
        }
        if (!getTransacted()) {
            throw new LocalTransactionException(NLS.getFormattedMessage(
                    ("ILLEGAL_STATE_CWSJR1139"),
                    new Object[] { "rollbackLocalTransaction"}, null));
        }

        if (_applicationLocalTransaction == null) {

            if (_connection.getConnectionFactory().isManaged()) {

                final SITransaction transaction = getManagedConnection()
                        .getCurrentTransaction(
                                _connection.getConnectionManager());

                if (transaction != null) {

                    // PK57931 if transaction is a local tran and _applicationLocalTransaction is null
                    // this means someone else created the local tran
                    if (transaction instanceof SIUncoordinatedTransaction) {
                         if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                             SibTr.debug(TRACE, "_applicationLocalTransaction was null but current tran is local.  Rolling back local tran.");
                         }
                         ((SIUncoordinatedTransaction)transaction).rollback();
                    } else {
                        // transaction is a global tran but this method is rollbackLocalTransaction
                        throw new LocalTransactionException(NLS
                                .getFormattedMessage(("ILLEGAL_STATE_CWSJR1140"),
                                        new Object[] { "rollbackLocalTransaction"},
                                        null));
                    }
                }
            }

            // Quietly do nothing if we don't have a global or container local
            // transaction
            // and we are transacted (the application local transaction has not
            // yet
            // been created).

        } else {

            try {

                _applicationLocalTransaction.rollback();

            } catch (final SIException exception) {

                FFDCFilter.processException(exception, CLASS_NAME
                        + ".rollbackLocalTransaction", "1:589:1.47", this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw exception;

            } catch (final SIErrorException exception) {

                FFDCFilter.processException(exception, CLASS_NAME
                        + ".rollbackLocalTransaction", "1:598:1.47", this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw exception;

            }

            if (_connection.getConnectionFactory().isManaged()) {
                _managedConnection.localTransactionRolledBack();
            }

            _applicationLocalTransaction = null;

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "rollbackLocalTransaction");
        }

    }

    /**
     * Closes this session.
     * @throws SIErrorException
     * @throws SIResourceException
     * @throws SIIncorrectCallException
     * @throws SIConnectionLostException
     */
    public void close() throws SIConnectionLostException,
                  SIIncorrectCallException, SIResourceException, SIErrorException {
        close(true);
    }

    /**
     * Gets the managed connection, this will reassociate itself with a managed
     * connection if we don't have one and the connection manager is a
     * <code>LazyAssociatableConnectionManager</code>.
     *
     * @return the managed connection
     * @throws IllegalStateException
     *             if we don't have a managed connection and the connection
     *             manager doesn't support lazy enlistment
     */
    JmsJcaManagedConnection getManagedConnection() throws IllegalStateException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getManagedConnection");
        }

        if (_managedConnection == null) {

            final ConnectionManager connectionManager = _connection
                    .getConnectionManager();

            if (connectionManager == null) {
                throw new IllegalStateException(NLS.getFormattedMessage(
                        ("EXCEPTION_RECEIVED_CWSJR1126"),
                        new Object[] { "getManagedConnection"}, null));
            }

            if (connectionManager instanceof LazyAssociatableConnectionManager) {

                try {

                    final ManagedConnectionFactory managedConnectionFactory = _connection
                            .getConnectionFactory()
                            .getManagedConnectionFactory();
                    ((LazyAssociatableConnectionManager) connectionManager)
                            .associateConnection(this,
                                    managedConnectionFactory, _requestInfo);

                } catch (final ResourceException exception) {

                    FFDCFilter.processException(exception, CLASS_NAME
                            + "getManagedConnection", "1:673:1.47", this);
                    SibTr.exception(this, TRACE, exception);
                    throw new IllegalStateException(NLS.getFormattedMessage(
                            ("EXCEPTION_RECEIVED_CWSJR1121"), new Object[] {
                                    "getManagedConnection", exception}, null),
                            exception);

                }

            } else {

                throw new IllegalStateException(NLS.getFormattedMessage(
                        ("EXCEPTION_RECEIVED_CWSJR1122"), new Object[] {
                                "getManagedConnection",
                                connectionManager.getClass().getName(),
                                LazyAssociatableConnectionManager.class
                                        .getName()}, null));

            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getManagedConnection", _managedConnection);
        }
        return _managedConnection;
    }

    /**
     * Close this session. Calls <code>sessionClosed</code> on the managed
     * connection so that the connection manager is notified.
     *
     * @param removeFromConnection
     *            Whether to remove this session from the connection's set of
     *            sessions or not
     * @throws SIErrorException
     * @throws SIResourceException
     * @throws SIIncorrectCallException
     * @throws SIConnectionLostException
     */
    void close(boolean removeFromConnection) throws SIConnectionLostException,
               SIIncorrectCallException, SIResourceException, SIErrorException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "close", Boolean

                    .valueOf(removeFromConnection));
        }

        if (_applicationLocalTransaction != null) {

            try
            {
              _applicationLocalTransaction.rollback();
            }
            catch (SIIncorrectCallException e)
            {
              //NO FFDC code needed
              
              // transaction has already completed, assume abort
              SibTr.debug(TRACE, "Caught a SIIncorrectCallException which means the operation has already been completed: " + e);
            }

            if (_connection.getConnectionFactory().isManaged()) {
                _managedConnection.localTransactionRolledBack();
            }

            _applicationLocalTransaction = null;
        }

        if (_managedConnection != null) {
            _managedConnection.sessionClosed(this);
        }

        if (removeFromConnection) {
            _connection.removeSession(this);
        }

        _sessionClosed = true;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "close");
        }

    }

    /**
     * Commits any unresolved application local transaction work on this
     * session. Called by <code>LocalTransaction.commit</code> when it has not
     * received a <code>begin</code> i.e. the resolution-control is
     * application and the unresolved-action is commit.
     *
     * @throws LocalTransactionException
     *             if there is no unresolved local transaction to commit
     * @throws SIErrorException
     * @throws SIResourceException
     * @throws SIIncorrectCallException
     * @throws SIConnectionLostException
     * @throws SIRollbackException
     */
    void commitUnresolvedLocalTransaction() throws LocalTransactionException,
            SIRollbackException, SIConnectionLostException,
            SIIncorrectCallException, SIResourceException, SIErrorException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "commitUnresolvedLocalTransaction");
        }

        if (_applicationLocalTransaction != null) {

            _applicationLocalTransaction.commit();

        } else {

            throw new LocalTransactionException(NLS.getFormattedMessage(
                    ("ILLEGAL_STATE_CWSJR1141"),
                    new Object[] { "commitUnresolvedLocalTransaction"}, null));

        }

        _applicationLocalTransaction = null;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "commitUnresolvedLocalTransaction");
        }

    }

    /**
     * Rolls back any unresolved application local transaction work on this
     * session. Called by <code>LocalTransaction.rollback</code> when it has
     * not received a <code>begin</code> i.e. when the resolution-control is
     * application and the unresolved-action is rollback.
     *
     * @throws LocalTransactionException
     *             if there is no unresolved local transaction to rollback
     * @throws SIErrorException
     * @throws SIResourceException
     * @throws SIIncorrectCallException
     * @throws SIConnectionLostException
     */
    void rollbackUnresolvedLocalTransaction() throws LocalTransactionException,
                          SIConnectionLostException, SIIncorrectCallException,
                          SIResourceException, SIErrorException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "rollbackUnresolvedLocalTransaction");
        }

        if (_applicationLocalTransaction != null) {

            _applicationLocalTransaction.rollback();

        } else {

            throw new LocalTransactionException(NLS.getFormattedMessage(
                    ("ILLEGAL_STATE_CWSJR1142"),
                    new Object[] { "rollbackUnresolvedLocalTransaction"}, null));

        }
        _applicationLocalTransaction = null;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "rollbackUnresolvedLocalTransaction");
        }

    }

    /**
     * Dissociates this session from its current managed connection.
     *
     */
    void dissociate() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "dissociate");
        }

        _managedConnection = null;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "dissociate");
        }

    }

    /**
     * Associates this session with a new managed connection.
     *
     * @param managedConnection
     *            the new managed connection
     */
    void associate(final JmsJcaManagedConnection managedConnection) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "associate", managedConnection);
        }

        if (_managedConnection != null) {
            _managedConnection.disassociateSession(this);
        }
        _managedConnection = managedConnection;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "associate");
        }

    }

    /**
     * Returns the parent connection for this session.
     *
     * @return the parent connection
     */
    JmsJcaConnectionImpl getConnection() {
        return _connection;
    }

    /**
     * Sets the transacted flag specified on <code>createSession</code>. In
     * the case of the first session this may be some time after construction.
     * This flag indicates whether, in the absence of a global or container
     * local transaction, work should be performed inside an application local
     * transaction.
     *
     * @param transacted
     *            the transacted flag
     */
    void setTransacted(boolean transacted) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "setTransacted", Boolean
                    .valueOf(transacted));
        }

        _transacted = transacted;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "setTransacted");
        }

    }

    /**
     * Marks this session as invalid.
     */
    void invalidate() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "invalidate");
        }

        _sessionInvalidated = true;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "invalidate");
        }

    }

    /**
     * Sets the parent connection for this session.
     *
     * @param connection
     *            the parent connection
     */
    void setParentConnection(final JmsJcaConnectionImpl connection) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "setParentConnection", connection);
        }

        _connection = connection;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "setParentConnection");
        }

    }

    /**
     * Returns a string representation of this object
     *
     * @return String The string describing this object
     */
    public String toString() {

        final StringBuffer sb = new StringBuffer("[");
        sb.append(getClass().getName());
        sb.append("@");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" <managedConnection=");
        sb.append(System.identityHashCode(_managedConnection));
        sb.append("> <connection=");
        sb.append(System.identityHashCode(_connection));
        sb.append("> <transacted=");
        sb.append(_transacted);
        sb.append("> <applicationLocalTransaction=");
        sb.append(_applicationLocalTransaction);
        sb.append("> <reqInfo=");
        sb.append(_requestInfo);
        sb.append("> <sessionClosed=");
        sb.append(_sessionClosed);
        sb.append("> <sessionInvalidated=");
        sb.append(_sessionInvalidated);
        sb.append(">]");
        return sb.toString();

    }

}
