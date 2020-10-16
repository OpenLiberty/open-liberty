/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.Executor;

import javax.resource.ResourceException;
import javax.resource.spi.LazyAssociatableConnectionManager;
import javax.resource.spi.LazyEnlistableConnectionManager;
import javax.resource.spi.ManagedConnection;
import javax.security.auth.Subject;

import com.ibm.adapter.AdapterUtil;
import com.ibm.adapter.ConnectionEventSender;
import com.ibm.adapter.Reassociateable;
import com.ibm.adapter.spi.ManagedConnectionFactoryImpl;
import com.ibm.adapter.spi.ManagedConnectionImpl;
import com.ibm.adapter.spi.StateManager;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * This connection object will be used by the BMP/Servlet user. It has a
 * reference to ManagedConnection.
 *
 * For smart handle supported case, there are three connection handle states:
 * ACTIVE, INACTIVE and CLOSED; for non-smart handle supported case, there are
 * only two connection handle states: ACTIVE and CLOSED.
 * <p>
 */
public class JdbcConnection extends JdbcObject implements Connection, Reassociateable {

    private static final TraceComponent tc = Tr.register(JdbcConnection.class);

    /** The underlying JDBC Connection implementation object. */
    Connection connImpl;

    /**
     * SPI ManagedConnection containing a Connection impl object from the JDBC
     * driver.
     */
    ManagedConnectionImpl managedConn;

    /** SPI ManagedConnectionFactory - needed for handle reassociation. */
    ManagedConnectionFactoryImpl mcf;

    /** J2C Connection Manager instance - also needed for handle reassociation. */
    private javax.resource.spi.ConnectionManager cm;

    // LIDB????
    /**
     * The same Connection Manager instance, with interfaces for lazy
     * enlistment. If the managed connection doesn't support lazy enlistment,
     * this instance is null.
     */
    private LazyEnlistableConnectionManager lazyEnlistableCM;

    // LIDB????
    /**
     * The same Connection Manager instance, with interfaces for lazy
     * association. If the managed connection doesn't support lazy enlistment,
     * this instance is null.
     */
    private LazyAssociatableConnectionManager lazyAssociatableCM; // [LIDB2110.16]

    // LIDB???? - Remove cmConfigData field

    /**
     * Indicates whether this handle is reserved for reassociation with its
     * current MC.
     */
    private boolean isReserved;

    // LIDB???? - Remove isShareable field

    /**
     * A read-only copy of the Subject from the ManagedConnection, for
     * reassociation.
     */
    private Subject subject;

    /** A read-only copy of the ConnectionRequestInfo, for reassociation. */
    private javax.resource.spi.ConnectionRequestInfo connRequestInfo;

    /** AutoCommit value, for reassociation only. */
    private boolean autoCommit;

    /** Whether connection ic closed */
    private boolean isClosed;

    private int rsType, rsConcurrency, fetchSize;

    private final boolean isMCLazyEnlistable;
    private final boolean isMCLazyAssociatable;

    /**
     * Create a WebSphere JDBC Connection wrapper. To associate a JDBC
     * Connection with a CCI Connection, the initializeForCCI() method must be
     * called.
     *
     * @param mc
     *            the Managed Connection containing the JDBC Connection
     *            implementation class.
     * @param conn
     *            the JDBC Connection implementation class to be wrapped.
     */
    public JdbcConnection(ManagedConnectionImpl mc, Connection conn) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>",
                     new Object[] { mc, AdapterUtil.toString(conn) });

        managedConn = mc;
        connImpl = conn;

        mcf = managedConn.getManagedConnectionFactory();

        // Use the managed connection as our synchronization object since it
        // controls the
        // transaction states.
        syncObject = managedConn;

        isMCLazyEnlistable = mc.isLazyEnlistable();
        isMCLazyAssociatable = mc.isLazyAssociatable();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "<init>", "Lazy Enlistable ? " + isMCLazyEnlistable);
            Tr.debug(tc, "<init>", "Lazy Associatable ? "
                                   + isMCLazyAssociatable);
        }

        if (childWrappers == null)
            childWrappers = new Vector(2);

        autoCommit = mc.getAutoCommit();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);
    }

    /**
     * Initialize the Connection handle. An initialize method must be invoked by
     * the DataSource before the handle is used.
     *
     * @param connectionManager
     *            the ConnectionManager associated with the DataSource.
     *
     * @return this Connection handle, initialized and ready for use.
     */
    final Connection initialize(
                                javax.resource.spi.ConnectionManager connectionManager) throws SQLException {
        cm = connectionManager;

        // LIDB???? starts
        try {
            lazyEnlistableCM = (LazyEnlistableConnectionManager) cm;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "ConnectionManager is Lazy Enlistable");
        } catch (ClassCastException cce) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "ConnectionManager is not Lazy Enlistable");
        }

        try {
            lazyAssociatableCM = (LazyAssociatableConnectionManager) cm;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "ConnectionManager is Lazy Associatable");
        } catch (ClassCastException cce) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "ConnectionManager is not Lazy Associatable");
        }
        // LIDB???? ends

        // LIDB??? - remove initialization of cmConfigData and isShareable

        rsType = ResultSet.TYPE_FORWARD_ONLY;
        rsConcurrency = ResultSet.CONCUR_READ_ONLY;

        if (state != INACTIVE)
            connImpl.clearWarnings(); // LIDB????

        return this;
    }

    /**
     * Call this method before any operation which should implicitly begin a
     * transaction.
     *
     * This method is mostly a no-op for CCI Connections.
     *
     * Check the autocommit flag on the connection. If autocommit = off, and
     * there is no global transaction, and a local transaction is not already
     * started, then implicitly start a local transaction (by firing a
     * LOCAL_TRANSACTION_STARTED ConnectionEvent)
     *
     * @throws SQLException
     *             if an error occurs or the current state is not valid.
     */
    @Override
    void beginTransactionIfNecessary() throws SQLException {
        boolean globalTransactionStarted = false;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "beginTransactionIfNecessary");

        switch (managedConn.getTransactionState()) {
            case StateManager.GLOBAL_TRANSACTION_ACTIVE:
                globalTransactionStarted = true;

            case StateManager.LOCAL_TRANSACTION_ACTIVE:

                // Already in a Local or Global Transaction, there is no need to
                // implicitly
                // begin or send a dirty signal for enlisting. Just return.

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Transaction is active.");
                break;

            case StateManager.NO_TRANSACTION_ACTIVE:

                // We are not aware of being in any transaction. If deferred
                // enlistment is
                // enabled, we may still need to enlist in a Global Transaction.
                // Otherwise,
                // if autoCommit is off, we need to implicitly begin a Local
                // Transaction.
                // The Transaction Manager can tell us for sure if we're in a Global
                // Transaction or not.

                if (AdapterUtil.inGlobalTransaction()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "In Global transaction.");

                    // The transaction hasn't been enlisted yet.

                    if (isMCLazyEnlistable) {
                        try {

                            managedConn.lazyEnlist(lazyEnlistableCM);

                            globalTransactionStarted = managedConn
                                            .getTransactionState() == StateManager.GLOBAL_TRANSACTION_ACTIVE;
                        } catch (ResourceException resX) {

                            // DELETE THIS - need to change the debug information
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Error enlisting lazily.", resX);

                            throw AdapterUtil.toSQLException(resX);
                        }
                    }
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Not In Global transaction.");

                    // LIDB???? - remove cmConfigData check

                    // If autoCommit is on, we don't want a transaction, just
                    // return.
                    if (managedConn.getAutoCommit())
                        break;

                    // Otherwise we need to implicity begin a local transaction.

                    try {
                        managedConn.processLocalTransactionStartedEvent(this);
                    } catch (ResourceException ex) {
                        throw AdapterUtil.toSQLException(ex);
                    }

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Local transaction started for " + this);
                }

                break;

            default:

                // In a transaction state that is not valid. Throw an exception.

                throw new SQLException("Invalid transaction state: "
                                       + managedConn.getTransactionStateAsString());
        }

    }

    /**
     * Clear the warnings.
     */
    @Override
    public final void clearWarnings() throws SQLException {
        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                reactivate();
            } else {
                throw new RuntimeException("State should not be INACTIVE when smart handle is not supported.");

            }
        }

        try {
            connImpl.clearWarnings();
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>
     * Perform any wrapper-specific close logic. This method is called by the
     * default WSJdbcObject close method.
     * </p>
     *
     * <p>
     * The Connection close method is responsible for requesting the
     * ManagedConnection to send a CONNECTION CLOSED ConnectionEvent to all
     * listeners.
     * </p>
     *
     * @return SQLException the first error to occur while closing the object.
     */
    @Override
    SQLException closeWrapper() {
        SQLException sqlX = null;

        // Send a Connection Closed Event to notify the Managed Connection of
        // the close -- if
        // we are associated with a ManagedConnection to notify.

        if (managedConn != null) {
            try {
                managedConn.processConnectionClosedEvent(this);
            } catch (ResourceException resX) {
                if (sqlX == null)
                    sqlX = AdapterUtil.toSQLException(resX);
                else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "DSA_INTERNAL_ERR_WARNING", new Object[] {
                                                                                "An unexpected error occurred sending a connectionClosed event"
                                                                                + System.getProperty("line.separator"),
                                                                                resX });
                }
            }
        } else if (isMCLazyAssociatable
                   && cm instanceof com.ibm.ws.j2c.ConnectionManager) {
            // We are not associated with a ManagedConnection, so notify the
            // WebSphere
            // ConnectionManager directly. [d138049.1.2]

            // 05/03/04: 
            // inactiveConnectionClosed() method is on the external
            // com.ibm.websphere.j2c.ConnectionManager
            // interface, not the internal com.ibm.ws.j2c.ConnectionManager
            // interface.
            // ((com.ibm.ws.j2c.ConnectionManager)
            // cm).inactiveConnectionClosed(this);
            ((com.ibm.websphere.j2c.ConnectionManager) cm)
                            .inactiveConnectionClosed(this);
        }

        // Destroy the connection wrapper, except for the state, numOps, and
        // syncObject.
        // These values will be needed to detect attempted use of the wrapper in
        // the CLOSED
        // state.

        connImpl = null;
        managedConn = null;
        connRequestInfo = null;
        subject = null;
        mcf = null;
        cm = null;

        // DELETE THIS -- need to set to null
        lazyEnlistableCM = null;
        lazyAssociatableCM = null;

        return sqlX;
    }

    /**
     * Fires a LOCAL_TRANSACTION_COMMITTED ConnectionEvent, which commits and
     * ends the local transaction.
     *
     * @throws SQLException
     *             if an error occurs during the commit.
     */
    @Override
    public void commit() throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "commit", this);

        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "commit", "no-op; state is INACTIVE");
                return;
            } else {

            }
        }

        try {
            synchronized (syncObject) {
                // Commit during a global transaction is an error.

                if (managedConn.inGlobalTransaction()) {
                    SQLException sqlX = new SQLException("Connection.commit is not allowed in global transaction");

                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "commit", sqlX);
                    throw sqlX;
                }

                // Commit while in no transaction is just a no-op.
                // Otherwise, if it's an application level local tran, commit
                // it.

                if (managedConn.getTransactionState() == StateManager.LOCAL_TRANSACTION_ACTIVE) {
                    // LIDB???? - Remove cmConfigData check.

                    managedConn.processLocalTransactionCommittedEvent(this);
                } // end synchronization
            }
        } catch (ResourceException ex) {

            SQLException sqlX = AdapterUtil.toSQLException(ex);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", "Exception");
            throw sqlX;
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "commit");
    }

    @Override
    public final Statement createStatement() throws SQLException {
        return createStatement(rsType, rsConcurrency);
    }

    @Override
    public final Statement createStatement(int type, int concurrency) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "createStatement",
                     new Object[] { this,
                                    AdapterUtil.getResultSetTypeString(type),
                                    AdapterUtil.getConcurrencyModeString(concurrency) });

        Statement stmt;

        try {
            if (state == INACTIVE) {
                if (isMCLazyAssociatable) {
                    reactivate();
                } else {
                    throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
                }
            }

            // Synchronize to make sure the transaction cannot be ended until
            // after the
            // statement is created.

            synchronized (syncObject) {
                beginTransactionIfNecessary();
                stmt = connImpl.createStatement(type, concurrency);
            }

            // Wrap the Statement.
            stmt = new JdbcStatement(stmt, this);
            childWrappers.add(stmt);

        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createStatement", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createStatement", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createStatement", stmt);
        return stmt;
    }

    /**
     * <p>
     * This method should be called by the ManagedConnection cleanup to
     * dissociate any remaining ACTIVE Connection handles.
     * </p>
     *
     * <p>
     * Transitions the Connection handle to the INACTIVE state. Retrieves and
     * stores all information needed for reassociation.
     * </p>
     *
     * <p>
     * This method is intended only for use by internal WebSphere code, although
     * we have no mechanism in place to prevent applications from invoking it.
     * If they do use it, the only side effects will be dissociating the handle.
     * </p>
     *
     * @throws ResourceException
     *             if the Connection handle is closed or a fatal error occurs on
     *             dissociation.
     */
    @Override
    public synchronized void dissociate() throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "dissociate",
                     new Object[] { this, this.getManagedConnection() });

        switch (state) {
            case ACTIVE:

                // Only ACTIVE (or INACTIVE reserved) handles may be dissociated.
                break;

            case INACTIVE:

                // A reserved handle may still be dissociated. This makes the handle
                // available
                // for reassociation with any ManagedConnection.
                if (!isMCLazyAssociatable) {
                    throw new RuntimeException("State should not be INACTIVE when smart handle is not supported.");

                }

                if (isReserved) {
                    if (tc.isEventEnabled())
                        Tr.event(tc, "Unreserving handle for dissociation", this);

                    isReserved = false;
                    break;
                } else {
                    // Dissociating an unreserved INACTIVE handle is just a no-op.
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "dissociate", "Already dissociated.");
                    return;
                }

            case CLOSED:

                // Dissociating a CLOSED handle is an error.
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "dissociate", "Exception");
                throw new ResourceException("Connection object is already closed");
        }

        // Track the Subject and ConnectionRequestInfo, which will be needed to
        // reassociate to a new ManagedConnection. Also save the autoCommit,
        // which
        // we will need to reestablish on the new ManagedConnection we are
        // reassociated to. [d138654]

        connRequestInfo = managedConn.createConnectionRequestInfo();
        subject = managedConn.getSubject();
        // autoCommit = managedConn.getAutoCommit();

        // Notify the ManagedConnection of the dissociation so it can remove
        // this
        // handle from its list.

        managedConn.dissociateHandle(this);

        // Null out references to the current ManagedConnection. The syncObject,
        // which usually points to the ManagedConnection, is temporarily
        // switched
        // to the numOps counter.

        connImpl = null;
        managedConn = null;
        syncObject = null;

        if (isMCLazyAssociatable) {
            // Mark the state as INACTIVE
            state = INACTIVE;
            if (tc.isEventEnabled())
                Tr.event(tc, "state --> INACTIVE");
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "dissociate");
    }

    /**
     * Send a CONNECTION_ERROR_OCCURRED ConnectionEvent to all listeners of the
     * Managed Connection.
     *
     * @param SQLException
     *            that's causing us to send the event, or null if none.
     */
    public void fireConnectionErrorEvent(SQLException sqlX) {
        switch (state) {
            case ACTIVE:
                try {
                    // Let the Managed Connection handle any duplicate events.
                    managedConn.processConnectionErrorOccurredEvent(this, sqlX);
                } catch (NullPointerException nullX) {
                    if (tc.isEventEnabled())
                        Tr.event(
                                 tc,
                                 "Handle CLOSED or INACTIVE. Not sending CONNECTION_ERROR_OCCURRED.",
                                 this);
                }
                break;

            case INACTIVE:
                if (!isMCLazyAssociatable) {
                    throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
                }

                try {
                    // LIDB1181.28.1 - ConnectionError events cannot be sent for
                    // INACTIVE handles.

                    if (tc.isEventEnabled())
                        Tr.event(
                                 tc,
                                 "Handle is INACTIVE. Not sending CONNECTION_ERROR_OCCURRED.",
                                 this);

                    close();
                } catch (SQLException closeX) {

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Error closing connection:", closeX);
                }
                break;

            case CLOSED:

                // LIDB1319.22 - If a close was already completed, do not send an
                // event.
                if (tc.isEventEnabled())
                    Tr.event(
                             tc,
                             "Connection already closed. Not sending CONNECTION_ERROR_OCCURRED.",
                             this);
        }
    }

    @Override
    public final boolean getAutoCommit() throws SQLException {
        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                reactivate();
            } else {
                throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
            }
        }

        try {
            return managedConn.getAutoCommit();
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Retrieve the ManagedConnection this Connection handle is currently
     * associated with.
     *
     * @return the ManagedConnection, or null if not associated.
     */
    @Override
    public ManagedConnection getManagedConnection() {
        return managedConn;
    }

    /**
     * Retrieve the ManagedConnection this Connection handle is currently
     * associated with.
     *
     * @return the ManagedConnection, or null if not associated.
     */
    public ConnectionEventSender getManagedConnectionAsSender() {
        try {
            if (state == INACTIVE) {
                if (isMCLazyAssociatable) {
                    reactivate();
                } else {
                    throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
                }
            }
        } catch (SQLException sqle) {
            throw new RuntimeException("reactivating the handle failed: "
                                       + sqle.getMessage());
        }

        return managedConn;
    }

    /**
     * Get catalog of the connection.
     *
     * @return The catalog
     */
    @Override
    public final String getCatalog() throws SQLException {
        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                reactivate();
            } else {
                throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
            }
        }

        try {
            return managedConn.getCatalog();
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @return the Connection wrapper for this object, or null if none is
     *         available.
     */
    @Override
    public final JdbcObject getConnectionWrapper() {
        return this;
    }

    /**
     * @return the underlying JDBC implementation object which we are wrapping.
     */
    @Override
    final Object getJDBCImplObject() {
        return connImpl;
    }

    /**
     * Get DatabaseMetaData.
     *
     * @return DatabaseMetaData.
     */
    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getMetaData", this);

        try {
            if (state == INACTIVE) {
                if (isMCLazyAssociatable) {
                    reactivate();
                } else {
                    throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
                }
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "getMetaData");
            return connImpl.getMetaData();
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getMetaData", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getMetaData", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

    }

    /**
     * @return the trace component for the WSJdbcConnection.
     */
    @Override
    final TraceComponent getTracer() {
        return tc;
    }

    /**
     * Get transaction isolation level of the connection.
     *
     * @return The transaction isolation level
     */
    @Override
    public final int getTransactionIsolation() throws SQLException {
        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                reactivate();
            } else {
                throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
            }
        }

        try {
            return managedConn.getTransactionIsolation();
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public final Map getTypeMap() throws SQLException {
        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                reactivate();
            } else {
                throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
            }
        }

        try {
            return managedConn.getTypeMap();
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public final SQLWarning getWarnings() throws SQLException {
        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                reactivate();
            } else {
                throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
            }
        }

        try {
            return connImpl.getWarnings();
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @return relevant FFDC information for the JDBC object, formatted as a
     *         String array.
     */
    @Override
    public String[] introspectSelf() {
        com.ibm.ws.rsadapter.FFDCLogger info = new com.ibm.ws.rsadapter.FFDCLogger(500, this);

        info.eoln();
        info.introspect("ConnectionRequestInfo:", connRequestInfo);
        info.introspect("ManagedConnectionFactory:", mcf);
        info.introspect("ManagedConnection:", managedConn);

        // put back return info.toStringArray();
        String[] array = info.toStringArray();
        System.out
                        .println("MH: Looking at contents of JdbcConnection introspect:\n");
        for (int i = 0; i < array.length; i++) {
            String oneElement = array[i];
            System.out.println("Element **" + i + "** of the array is **"
                               + oneElement + "**");
        }
        return array;
    }

    /**
     * Collects FFDC information specific to this JDBC wrapper. Formats this
     * information to the provided FFDC logger. This method is used by
     * introspectAll to collect any wrapper specific information.
     *
     * @param info
     *            FFDCLogger on which to record the FFDC information.
     */
    @Override
    void introspectWrapperSpecificInfo(com.ibm.ws.rsadapter.FFDCLogger info) {

        info.append("Transaction Manager global transaction status is",
                    AdapterUtil.getGlobalTranStatusAsString());

        info.append("Underlying Connection: " + AdapterUtil.toString(connImpl),
                    connImpl);
        info.append("Connection Manager:", cm);

        info.append("Handle is reserved? " + isReserved);
        info.append("Default ResultSet Type:",
                    AdapterUtil.getResultSetTypeString(rsType));
        info.append("Default ResultSet Concurrency:",
                    AdapterUtil.getConcurrencyModeString(rsConcurrency));
        info.append("Default FetchSize: " + fetchSize);
        info.append("AutoCommit value for reassociation: " + autoCommit);

    }

    /**
     * @return true if this object is closed, otherwise false.
     */
    @Override
    public final boolean isClosed() {
        return state == CLOSED;
    }

    @Override
    public final boolean isReadOnly() throws SQLException {
        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                reactivate();
            } else {
                throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
            }
        }

        try {
            return managedConn.isReadOnly();
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @return true if the handle is reserved for reassociation with its current
     *         ManagedConnection, otherwise false.
     */
    @Override
    public final boolean isReserved() {
        return isReserved;
    }

    // LIDB????
    /**
     * @return true if this Connection is shareable; false if it is
     *         non-shareable.
     *
     * @throws SQLException
     *             if the Connection is closed.
     */
    public final boolean isShareable() throws SQLException {
        try {
            return false;
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public final String nativeSQL(String sql) throws SQLException {
        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                reactivate();
            } else {
                throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
            }
        }

        try {
            return connImpl.nativeSQL(sql);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public final CallableStatement prepareCall(String sql) throws SQLException {
        // If an AccessIntent is available, get the Statement fetch size and
        // ResultSet
        // type and concurrency from the AccessIntent. This is non-standard
        // behavior for
        // JDBC, but only applies when an AccessIntent is available. [LIDB1449]
        // rsType and rsConcurrency are now initialized with the default values
        // if no
        // AccessIntent is provided, so there is no need for a check anymore.

        return prepareCall(sql, rsType, rsConcurrency);
    }

    @Override
    public final CallableStatement prepareCall(String sql, int type,
                                               int concurrency) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "prepareCall",
                     new Object[] { this, sql,
                                    AdapterUtil.getResultSetTypeString(type),
                                    AdapterUtil.getConcurrencyModeString(concurrency) });

        CallableStatement cstmt;

        try {
            if (state == INACTIVE) {
                if (isMCLazyAssociatable) {
                    reactivate();
                } else {
                    throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
                }
            }

            synchronized (syncObject) {
                // Synchronize to make sure the transaction cannot be ended
                // until after the
                // statement is created.

                beginTransactionIfNecessary();
                cstmt = connImpl.prepareCall(sql, type, concurrency);
            }

            // Wrap the Callable
            cstmt = new JdbcCallableStatement(cstmt, this);
            childWrappers.add(cstmt);
        } catch (SQLException sqlX) {

            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareCall", "Exception");
            throw sqlX;
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareCall", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "prepareCall", cstmt);
        return cstmt;
    }

    @Override
    public final PreparedStatement prepareStatement(String sql) throws SQLException {
        return prepareStatement(sql, rsType, rsConcurrency);
    }

    @Override
    public final PreparedStatement prepareStatement(String sql, int type,
                                                    int concurrency) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "prepareStatement",
                     new Object[] { this, sql,
                                    AdapterUtil.getResultSetTypeString(type),
                                    AdapterUtil.getConcurrencyModeString(concurrency), });

        PreparedStatement pstmt;

        try {
            if (state == INACTIVE) {
                if (isMCLazyAssociatable) {
                    reactivate();
                } else {
                    throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
                }
            }
            // Only create a PSCacheKey if statement caching is enabled.
            // [d139351.16]

            synchronized (syncObject) {
                // Synchronize to make sure the transaction cannot be ended
                // until after
                // the statement is created.

                beginTransactionIfNecessary();
                pstmt = connImpl.prepareStatement(sql, type, concurrency);
            }

            pstmt = new JdbcPreparedStatement(pstmt, this);
            childWrappers.add(pstmt);
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareStatement", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareStatement", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "prepareStatement", pstmt);
        return pstmt;
    }

    /**
     * Returns an INACTIVE connection handle to an ACTIVE state by reassociating
     * it with a new ManagedConnection. If this same handle is first reactivated
     * on another thread then this method becomes a no-op.
     *
     * @throws SQLException
     *             if an error occurs reactivating the handle.
     */
    synchronized final void reactivate() throws SQLException {
        if (!isMCLazyAssociatable) {
            throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
        }

        if (state != INACTIVE)
            return;

        if (tc.isEventEnabled())
            Tr.event(tc, "Requesting implicit reactivation.");

        // If implicit reassociation is not enabled for this handle then the CM
        // is
        // responsible for reassociating all handles before use. It's an error
        // if any are
        // still INACTIVE.

        if (!isMCLazyEnlistable)
            throw new SQLException("reactiveate() should not be called if the MC doesn't support lazy association");

        try {
            // Request the ConnectionManager to associate this handle with a
            // Managed
            // Connection. Since we were able to successfully reach the
            // dissociated
            // state we may assume the ConnectionManager supports reassociation.

            // LIDB???? - Lazy associate the connection.
            lazyAssociatableCM.associateConnection(this, mcf, connRequestInfo);

            // ((com.ibm.ws.j2c.ConnectionManager) cm).associateConnection(mcf,
            // subject, connRequestInfo, this);
        } catch (ResourceException reassociationX) {
            if (tc.isDebugEnabled())
                Tr.warning(tc, "REASSOCIATION_ERR", reassociationX);
            throw AdapterUtil.toSQLException(reassociationX);
        }
    }

    /**
     * <p>
     * Reassociates this Connection handle with a new ManagedConnection. This
     * includes reassociating the underlying Connection object, but NOT the
     * child objects of the Connection, which must all be closed at this point.
     * It is an error to reassociate a handle which is not in the inactive
     * state.
     * </p>
     *
     * @param mc
     *            the new ManagedConnection to associate this handle with.
     * @param connImplObject
     *            the new underlying JDBC Connection object to associate this
     *            handle with.
     *
     * @throws ResourceException
     *             if an incorrect key is supplied, if the handle is not ready
     *             for reassociation, or if an error occurs during the
     *             reassociation.
     */
    @Override
    public synchronized void reassociate(ManagedConnection mc,
                                         Connection connImplObject) throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "reassociate",
                     new Object[] { this, mc,
                                    AdapterUtil.toString(connImplObject) });

        // Verify the caller is allowed to call this method.
        // If the key is nulled out, the Connection must be closed.
        // Reassociating closed
        // handles is not valid.

        if (isMCLazyAssociatable && isReserved) {

            // Handle is reserved for reassociation with its current
            // ManagedConnection,
            // so we better have the same ManagedConnection.

            if (mc != managedConn) {
                String message = "Connection handle is reserved for reassociation with a specific "
                                 + "ManagedConnection, which does not match the ManagedConnection "
                                 + "provided.";

                if (tc.isDebugEnabled())
                    Tr.debug(tc, message,
                             new Object[] { this, managedConn, mc });

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "reassociate", "Exception");

                throw new ResourceException("Handle is reserved for another MC");
            }

            if (tc.isEventEnabled())
                Tr.event(tc,
                         "Handle is reserved, reassociating back to original ManagedConnection.");

            // Since the dissociation was never honored, there's not much to do
            // for the
            // reassociation. Just set the state to ACTIVE and unreserve the
            // handle.

            isReserved = false;
            state = ACTIVE;
            if (tc.isEventEnabled())
                Tr.event(tc, "state --> ACTIVE");

            if (tc.isEntryEnabled())
                Tr.exit(tc, "reassociate");
            return;
        }
        // ELSE handle is not reserved; continue with reassociation as usual.

        // A Connection handle may only be reassociated from an INACTIVE state.

        if (isMCLazyAssociatable && state != INACTIVE) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "reassociate", "Exception");

            throw new ResourceException("Cannot reassociate a connection not in INACTIVE state.");
        }

        // Switch to the new ManagedConnection. The ConnectionManager and
        // CMConfigData
        // will remain the same.

        managedConn = (ManagedConnectionImpl) mc;
        syncObject = managedConn;
        connImpl = connImplObject;

        // Update the autoCommit value on the underlying ManagedConnection.
        // [d144779]

        /*
         * try { if (tc.isDebugEnabled()) Tr.debug(tc,
         * "set autoCommit to "+autoCommit);
         * managedConn.setAutoCommit(autoCommit); } catch (SQLException sqle) {
         * throw new ResourceException(sqle.getMessage()); }
         */

        connRequestInfo = null;
        subject = null;

        // Transition the handle back to the ACTIVE state.
        // This all occurs within the reactivate method, which is synchronized.

        state = ACTIVE;
        if (tc.isEventEnabled())
            Tr.event(tc, "state --> ACTIVE");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "reassociate");
    }

    /**
     * <p>
     * Reserve this Connection handle for reassociation only with its current
     * ManagedConnection. This optimization allows child objects of the handle
     * also associated with the ManagedConnection (or associated with underlying
     * objects of the ManagedConnection) to be left open across reassociations.
     * This method should only be used when the guarantee can be made that the
     * handle will always be reassociated with the same ManagedConnection.
     * </p>
     *
     * <p>
     * The handle remains marked as reserved until either of the following
     * occurs:
     * </p>
     * <ol>
     * <li>A reassociation is requested back to the original handle, or
     * <li>The reserve is overridden with an explicit request to dissociate.
     * </ol>
     *
     * <p>
     * A handle remains in the ACTIVE state while it is "reserved".
     * </p>
     *
     * @throws ResourceException
     *             if an incorrect key is supplied or if the handle may not be
     *             reserved from its current state.
     */
    @Override
    public void reserve() throws ResourceException {
        if (!isMCLazyAssociatable) {
            throw new RuntimeException("reserve() should not be called when smart handle is not supported.");
        }

        isReserved = true;
        if (tc.isEventEnabled())
            Tr.event(tc, "Reserving handle", this);

        // A reserved handle is considered dissociated, so state should be
        // INACTIVE.

        state = INACTIVE;
        if (tc.isEventEnabled())
            Tr.event(tc, "state --> INACTIVE");
    }

    /**
     * Fires a LOCAL_TRANSACTION_ROLLEDBACK ConnectionEvent, which rolls back
     * and ends the local transaction.
     *
     * @throws SQLException
     *             if an error occurs during the rollback.
     */
    @Override
    public void rollback() throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "rollback", this);

        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "rollback", "no-op; state is INACTIVE");
                return;
            } else {
                throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
            }
        }

        try {
            synchronized (syncObject) {
                // Rollback during a global transaction is an error.

                if (managedConn.inGlobalTransaction()) {
                    SQLException sqlX = new SQLException("Connection.rollback is not allowed in global transaction.");

                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "rollback", sqlX);
                    throw sqlX;
                }

                // Rollback while in no transaction is just a no-op.
                // Otherwise, if it's an application level local tran, roll it
                // back.

                if (managedConn.getTransactionState() == StateManager.LOCAL_TRANSACTION_ACTIVE) {
                    // LIDB???? - remove cmConfigData
                    managedConn.processLocalTransactionRolledbackEvent(this);
                }
            } // end synchronization
        } catch (ResourceException resX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "rollback", "Exception");
            throw AdapterUtil.toSQLException(resX);
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "rollback", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "rollback");
    }

    /**
     * @param runtimeX
     *            a RuntimeException which occurred, indicating the wrapper may
     *            be closed or inactive. If the handle is inactive due to a
     *            concurrent dissociation on another thread then we close it and
     *            throw an SQLException.
     *
     * @return the RuntimeException to throw if it isn't.
     */
    @Override
    final RuntimeException runtimeXIfNotClosed(RuntimeException runtimeX) throws SQLException {
        switch (state) {
            case INACTIVE:

                if (tc.isDebugEnabled())
                    Tr.debug(tc,
                             "Connection dissociated on another thread while performing an operation. "
                                 + "Closing the Connection handle.");

                close();

            case CLOSED:
                throw new SQLException("Connection " + this + " is closed");
        }

        return runtimeX;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setAutoCommit requested by application.");

        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                reactivate();
            } else {
                throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
            }
        }

        try {

            // Setters are not permitted when multiple handles are sharing the
            // same ManagedConnection.

            // LIDB????
            if (managedConn.getHandleCount() > 1)
                throw new SQLException("Operation setAutoCommit() is not allowed for shared connection");

            // Synchronize to make sure autoCommit doesn't change while checking
            // whether to
            // implicitly begin a transaction, or during commit or rollback.

            synchronized (syncObject) {
                int tranState = managedConn.getTransactionState();

                if (autoCommit
                    && tranState != StateManager.NO_TRANSACTION_ACTIVE) {
                    // setAutoCommit(true) is not allowed in a
                    // GlobalTransaction.

                    if (managedConn.inGlobalTransaction()) {
                        throw new SQLException("Operation setAutoCommit(true) is not allowed in global transaction");
                    } else {

                        commit();
                    }
                }

                managedConn.setAutoCommit(autoCommit);
                this.autoCommit = autoCommit;
            }
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public final void setCatalog(String cLog) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setCatalog", new Object[] { this, cLog });

        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                reactivate();
            } else {
                throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
            }
        }

        try {
            // When in a global transaction, setters are only allowed on
            // Connections in
            // Non-Sharing mode. d128009

            // Fix DSRA9250E exception message. d139899

            // LIDB????
            if (managedConn.getHandleCount() > 1)
                throw new SQLException("Operation setCatalog is not allowed for shared connection");

            managedConn.setCatalog(cLog);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public final void setReadOnly(boolean readOnly) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setReadOnly", new Object[] { this,
                                                       readOnly ? Boolean.TRUE : Boolean.FALSE });

        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                reactivate();
            } else {
                throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
            }
        }

        try {
            // When in a global transaction, setters are only allowed on
            // Connections in
            // Non-Sharing mode. d128009

            // Fix DSRA9250E exception message. d139899

            // LIDB????
            if (managedConn.getHandleCount() > 1)
                throw new SQLException("Operation setReadOnly is not allowed for shared connection");

            managedConn.setReadOnly(readOnly);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public final void setTransactionIsolation(int level) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setTransactionIsolation requested by application.");

        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                reactivate();
            } else {
                throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
            }
        }

        try {

            // It's an error to set the isolation level while in a transaction.
            // Even though
            // this violates the JDBC spec, we allow the value to be set while
            // in a local
            // transaction. It is left up to the underlying driver to enforce
            // the spec by
            // throwing an exception. Apparently DB2 wants to allow this
            // behavior.

            // DELETE THIS

            // LIDB????
            if (managedConn.getHandleCount() > 1)
                throw new SQLException("Operation setTransactionIsolation is not allowed for shared connection");

            if (managedConn.inGlobalTransaction())
                throw new SQLException("Operation setTransactionIsolation is not allowed inn global transaction");

            managedConn.setTransactionIsolation(level);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public final void setTypeMap(Map map) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setTypeMap", new Object[] { this, map });

        if (state == INACTIVE) {
            if (isMCLazyAssociatable) {
                reactivate();
            } else {
                throw new RuntimeException("State cannot be INACTIVE when smart handle is not supported.");
            }
        }

        try {
            // When in a global transaction, setters are only allowed on
            // Connections in
            // Non-Sharing mode. d128009

            // Fix DSRA9250E exception message. d139899

            // LIDB????
            if (managedConn.getHandleCount() > 1)
                throw new SQLException("Operation setTypeMap is not allowed for shared connection");

            managedConn.setTypeMap(map);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Indicates whether the handle supports implicit reactivation. Implicit
     * reactivation means that an inactive connection handle will implicitly
     * request reassociation when used. For example, if the handle state is
     * inactive and a createStatement operation is requested, the handle will
     * implicitly reassociate with a new underlying connection and continue the
     * operation.
     *
     * @return true if the handle supports implicit reactivation, otherwise
     *         false.
     */
    @Override
    public final boolean supportsImplicitReactivation() {
        // return mcf.isLazyAssociatable();
        return mcf.getLazyAssociatable().equals(Boolean.TRUE);
    }

    @Override
    public void close() throws SQLException {
        TraceComponent tc = getTracer();

        if (tc.isEntryEnabled())
            Tr.entry(tc, "close", this);

        // Make sure we only get closed once.

        synchronized (this) {
            if (state == CLOSED) // already closed, just return
            {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Already closed.");
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "close");
                return;
            }

            state = CLOSED;
        }

        if (tc.isEventEnabled())
            Tr.event(tc, "state --> " + getStateString());

        SQLException sqlX = closeWrapper();

        if (sqlX == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "close");
        } else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "close", sqlX);
            throw sqlX;
        }
    }

    /**
     * <p>
     * Changes the holdability of ResultSet objects created using this
     * Connection object to the given holdability.
     * </P>
     *
     * @param holdability
     *            a ResultSet holdability constant; either
     *            ResultSet.HOLD_CURSORS_OVER_COMMIT or
     *            ResultSet.CLOSE_CURSORS_AT_COMMIT
     *
     * @exception SQLException
     *                If a database access occurs, the given parameter is not a
     *                ResultSet constant indicating holdability, or the given
     *                holdability is not supported
     */
    @Override
    public void setHoldability(int holdability) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setHoldability requested by application.");

        if (state == INACTIVE)
            reactivate();

        try {
            // When in a global transaction, setters are only allowed on
            // Connections in
            // Non-Sharing mode. d128009

            // Fix DSRA9250E exception message. d139899

            // LIDB????
            if (managedConn.getHandleCount() > 1)
                throw new SQLException("Operation setHoldability is not allowed for shared connection");

            // d119259 - Call the method on the mc not on the physical
            // connection

            managedConn.setHoldability(holdability);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>
     * Retrieves the current holdability of ResultSet objects created using this
     * Connection object.
     * </p>
     *
     * @return the holdability, one of ResultSet.HOLD_CURSORS_OVER_COMMIT or
     *         ResultSet.CLOSE_CURSORS_AT_COMMIT
     *
     * @exception SQLException
     *                If a database access occurs
     */
    @Override
    public int getHoldability() throws SQLException {
        if (state == INACTIVE)
            reactivate();

        try {
            return managedConn.getHoldability();
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    // LIDB2040.4.3 ends

    // LIDB2040.4.4 starts
    /**
     * <p>
     * Creates an unnamed savepoint in the current transaction and returns the
     * new Savepoint object that represents it.
     * <p>
     *
     * @return the new Savepoint object
     *
     * @exception SQLException
     *                If a database access error occurs or this Connection
     *                object is currently in auto-commit mode.
     */
    @Override
    public java.sql.Savepoint setSavepoint() throws SQLException {
        if (state == INACTIVE)
            reactivate();

        try {
            return managedConn.setSavepoint();
        } catch (SQLException ex) {

            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>
     * Creates a savepoint with the given name in the current transaction and
     * returns the new Savepoint object that represents it.
     * </p>
     *
     * @param name
     *            a String containing the name of the savepoint
     *
     * @return the new Savepoint object
     *
     * @exception SQLException
     *                f a database access error occurs or this Connection object
     *                is currently in auto-commit mode
     */
    @Override
    public java.sql.Savepoint setSavepoint(String name) throws SQLException {
        if (state == INACTIVE)
            reactivate();

        try {
            return managedConn.setSavepoint(name);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>
     * Undoes all changes made after the given Savepoint object was set. This
     * method should be used only when auto-commit has been disabled.
     * </p>
     *
     * @param savepoint
     *            the Savepoint object to roll back to
     *
     * @exception SQLException
     *                If a database access error occurs, the Savepoint object is
     *                no longer valid, or this Connection object is currently in
     *                auto-commit mode
     */
    @Override
    public void rollback(java.sql.Savepoint savepoint) throws SQLException {
        if (state == INACTIVE)
            reactivate();

        try {
            managedConn.rollback(savepoint);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>
     * Removes the given Savepoint object from the current transaction. Any
     * reference to the savepoint after it have been removed will cause an
     * SQLException to be thrown.
     * </p>
     *
     * @param savepoint
     *            the Savepoint object to be removed
     *
     * @exception SQLException
     *                If a database access error occurs or the given Savepoint
     *                object is not a valid savepoint in the current transaction
     */
    @Override
    public void releaseSavepoint(java.sql.Savepoint savepoint) throws SQLException {
        if (state == INACTIVE)
            reactivate();

        try {
            managedConn.releaseSavepoint(savepoint);
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>
     * Creates a Statement object that will generate ResultSet objects with the
     * given type, concurrency, and holdability. This method is the same as the
     * createStatement method above, but it allows the default result set type,
     * concurrency, and holdability to be overridden.
     * </p>
     *
     * @param resultSetType
     *            one of the following ResultSet constants:
     *            ResultSet.TYPE_FORWARD_ONLY,
     *            ResultSet.TYPE_SCROLL_INSENSITIVE, or
     *            ResultSet.TYPE_SCROLL_SENSITIVE
     * @param resultSetConcurrency
     *            one of the following ResultSet constants:
     *            ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
     * @param resultSetHoldability
     *            one of the following ResultSet constants:
     *            ResultSet.HOLD_CURSORS_OVER_COMMIT or
     *            ResultSet.CLOSE_CURSORS_AT_COMMIT
     *
     * @return a new Statement object that will generate ResultSet objects with
     *         the given type, concurrency, and holdability
     *
     * @exception SQLException
     *                If a database access error occurs or the given parameters
     *                are not ResultSet constants indicating type, concurrency,
     *                and holdability
     */
    @Override
    public java.sql.Statement createStatement(int resultSetType,
                                              int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        // we cannot make createStatement(resultSetType, resultSetConcurrency)
        // to call
        // createStatement(resultSetType, resultSetConcurrency,
        // resultSetHoldability) since
        // the default resultSetHoldability is different for different backends.

        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "createStatement",
                     new Object[] {
                                    this,
                                    AdapterUtil.getResultSetTypeString(resultSetType),
                                    AdapterUtil
                                                    .getConcurrencyModeString(resultSetConcurrency),
                                    AdapterUtil
                                                    .getCursorHoldabilityString(resultSetHoldability) });

        Statement stmt;

        try {
            if (state == INACTIVE)
                reactivate();

            // Synchronize to make sure the transaction cannot be ended until
            // after the
            // statement is created.

            synchronized (syncObject) {
                beginTransactionIfNecessary();
                stmt = connImpl.createStatement(resultSetType,
                                                resultSetConcurrency, resultSetHoldability);
            }

            // Wrap the Statement.
            // FIX ME -- add cursor holdability
            stmt = new JdbcStatement(stmt, this);
            childWrappers.add(stmt);
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createStatement", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createStatement", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createStatement", stmt);
        return stmt;
    }

    /**
     * <p>
     * Creates a PreparedStatement object that will generate ResultSet objects
     * with the given type, concurrency, and holdability.
     * </p>
     *
     * <p>
     * This method is the same as the prepareStatement method above, but it
     * allows the default result set type, concurrency, and holdability to be
     * overridden.
     * </p>
     *
     * @param sql
     *            a String object that is the SQL statement to be sent to the
     *            database; may contain one or more ? IN parameters
     * @param resultSetType
     *            one of the following ResultSet constants:
     *            ResultSet.TYPE_FORWARD_ONLY,
     *            ResultSet.TYPE_SCROLL_INSENSITIVE, or
     *            ResultSet.TYPE_SCROLL_SENSITIVE
     * @param resultSetConcurrency
     *            one of the following ResultSet constants:
     *            ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
     * @param resultSetHoldability
     *            one of the following ResultSet constants:
     *            ResultSet.HOLD_CURSORS_OVER_COMMIT or
     *            ResultSet.CLOSE_CURSORS_AT_COMMIT
     *
     * @return a new PreparedStatement object, containing the pre-compiled SQL
     *         statement, that will generate ResultSet objects with the given
     *         type, concurrency, and holdability
     *
     * @exception SQLException
     *                If a database access error occurs or the given parameters
     *                are not ResultSet constants indicating type, concurrency,
     *                and holdability
     */
    @Override
    public java.sql.PreparedStatement prepareStatement(String sql, int type,
                                                       int concurrency, int holdability) throws SQLException {

        // we cannot make prepareStatement(resultSetType, resultSetConcurrency)
        // to call
        // prepareStatement(resultSetType, resultSetConcurrency,
        // resultSetHoldability) since
        // the default resultSetHoldability is different for different backends.

        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "prepareStatement",
                     new Object[] { this, sql,
                                    AdapterUtil.getResultSetTypeString(type),
                                    AdapterUtil.getConcurrencyModeString(concurrency),
                                    AdapterUtil.getCursorHoldabilityString(holdability) });

        PreparedStatement pstmt;

        try {
            if (state == INACTIVE)
                reactivate();

            // Only create a PSCacheKey if statement caching is enabled.
            // [d139351.16]
            synchronized (syncObject) {
                // Synchronize to make sure the transaction cannot be ended
                // until after
                // the statement is created.

                beginTransactionIfNecessary();

                pstmt = connImpl.prepareStatement(sql, type, concurrency);
            }

            // FIX ME - add cusor holdability
            pstmt = new JdbcPreparedStatement(pstmt, this);

            childWrappers.add(pstmt);
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareStatement", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareStatement", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "prepareStatement", pstmt); // [d164609]
        return pstmt; // [d164609]
    }

    /**
     * <p>
     * Creates a CallableStatement object that will generate ResultSet objects
     * with the given type and concurrency.
     * </p>
     *
     * <p>
     * This method is the same as the prepareCall method above, but it allows
     * the default result set type, result set concurrency type and holdability
     * to be overridden.
     * </p>
     *
     * @param sql
     *            a String object that is the SQL statement to be sent to the
     *            database; may contain on or more ? parameters
     * @param resultSetType
     *            one of the following ResultSet constants:
     *            ResultSet.TYPE_FORWARD_ONLY,
     *            ResultSet.TYPE_SCROLL_INSENSITIVE, or
     *            ResultSet.TYPE_SCROLL_SENSITIVE
     * @param resultSetConcurrency
     *            one of the following ResultSet constants:
     *            ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
     * @param resultSetHoldability
     *            one of the following ResultSet constants:
     *            ResultSet.HOLD_CURSORS_OVER_COMMIT or
     *            ResultSet.CLOSE_CURSORS_AT_COMMIT
     *
     * @return a new CallableStatement object, containing the pre-compiled SQL
     *         statement, that will generate ResultSet objects with the given
     *         type, concurrency, and holdability
     *
     * @exception SQLException
     *                If a database access error occurs or the given parameters
     *                are not ResultSet constants indicating type, concurrency,
     *                and holdability
     */
    @Override
    public java.sql.CallableStatement prepareCall(String sql, int type,
                                                  int concurrency, int holdability) throws SQLException {

        // we cannot make prepareCall(sql, resultSetType, resultSetConcurrency)
        // to call
        // prepareCall(sql, resultSetType, resultSetConcurrency,
        // resultSetHoldability) since
        // the default resultSetHoldability is different for different backends.

        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "prepareCall",
                     new Object[] { this, sql,
                                    AdapterUtil.getResultSetTypeString(type),
                                    AdapterUtil.getConcurrencyModeString(concurrency),
                                    AdapterUtil.getCursorHoldabilityString(holdability) });

        CallableStatement cstmt;

        try {
            if (state == INACTIVE)
                reactivate();

            synchronized (syncObject) {
                // Synchronize to make sure the transaction cannot be ended
                // until after
                // the statement is created.

                beginTransactionIfNecessary();

                cstmt = connImpl.prepareCall(sql, type, concurrency);
            }

            // Wrap the CallableStatement.
            // FIX ME - add cursor holdability
            cstmt = new JdbcCallableStatement(cstmt, this); // [d164609]

            childWrappers.add(cstmt); // [d164609]
        } catch (SQLException sqlX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareCall", "Exception");
            throw AdapterUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareCall", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "prepareCall", cstmt); // [d164609]
        return cstmt; // [d164609]
    }

    /**
     * <p>
     * Creates a default PreparedStatement object that has the capability to
     * retrieve auto-generated keys. The given constant tells the driver whether
     * it should make auto-generated keys available for retrieval. This
     * parameter is ignored if the SQL statement is not an INSERT statement.
     * </p>
     *
     * <p>
     * <b>Note</b>: This method is optimized for handling parametric SQL
     * statements that benefit from precompilation. If the driver supports
     * precompilation, the method prepareStatement will send the statement to
     * the database for precompilation. Some drivers may not support
     * precompilation. In this case, the statement may not be sent to the
     * database until the PreparedStatement object is executed. This has no
     * direct effect on users; however, it does affect which methods throw
     * certain SQLExceptions.
     * </p>
     *
     * <p>
     * Result sets created using the returned PreparedStatement object will by
     * default be type TYPE_FORWARD_ONLY and have a concurrency level of
     * CONCUR_READ_ONLY.
     * </p>
     *
     * @param sql
     *            an SQL statement that may contain one or more '?' IN parameter
     *            placeholders
     * @param autoGeneratedKeys
     *            a flag indicating whether auto-generated keys should be
     *            returned; one of Statement.RETURN_GENERATED_KEYS or
     *            Statement.NO_GENERATED_KEYS
     *
     * @return a new PreparedStatement object, containing the pre-compiled SQL
     *         statement, that will have the capability of returning
     *         auto-generated keys
     *
     * @exception SQLException
     *                If a database access error occurs or the given parameter
     *                is not a Statement constant indicating whether
     *                auto-generated keys should be returned
     */
    @Override
    public java.sql.PreparedStatement prepareStatement(String sql,
                                                       int autoGeneratedKeys) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "prepareStatement", new Object[] { this, sql,
                                                            AdapterUtil.getAutoGeneratedKeyString(autoGeneratedKeys) });

        PreparedStatement pstmt;

        try {
            if (state == INACTIVE)
                reactivate();

            // Only create a PSCacheKey if statement caching is enabled.
            // [d139351.16]

            synchronized (syncObject) {
                // Synchronize to make sure the transaction cannot be ended
                // until after
                // the statement is created.

                beginTransactionIfNecessary();

                pstmt = connImpl.prepareStatement(sql, autoGeneratedKeys);
            }

            // FIX ME - add cursor holdability
            pstmt = new JdbcPreparedStatement(pstmt, this); // [d164609]

            childWrappers.add(pstmt);
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareStatement", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareStatement", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "prepareStatement", pstmt); // [d164609]
        return pstmt; // [d164609]
    }

    /**
     * <p>
     * Creates a default PreparedStatement object capable of returning the
     * auto-generated keys designated by the given array. This array contains
     * the indexes of the columns in the target table that contain the
     * auto-generated keys that should be made available. This array is ignored
     * if the SQL statement is not an INSERT statement.
     * </p>
     *
     * <p>
     * An SQL statement with or without IN parameters can be pre-compiled and
     * stored in a PreparedStatement object. This object can then be used to
     * efficiently execute this statement multiple times.
     * </p>
     *
     * <p>
     * <b>Note</b>: This method is optimized for handling parametric SQL
     * statements that benefit from precompilation. If the driver supports
     * precompilation, the method prepareStatement will send the statement to
     * the database for precompilation. Some drivers may not support
     * precompilation. In this case, the statement may not be sent to the
     * database until the PreparedStatement object is executed. This has no
     * direct effect on users; however, it does affect which methods throw
     * certain SQLExceptions.
     * </p>
     *
     * <p>
     * Result sets created using the returned PreparedStatement object will by
     * default be type TYPE_FORWARD_ONLY and have a concurrency level of
     * CONCUR_READ_ONLY.
     * </p>
     *
     * @param sql
     *            an SQL statement that may contain one or more '?' IN parameter
     *            placeholders
     * @param columnIndexes
     *            an array of column indexes indicating the columns that should
     *            be returned from the inserted row or rows
     *
     * @return a new PreparedStatement object, containing the pre-compiled
     *         statement, that is capable of returning the auto-generated keys
     *         designated by the given array of column indexes
     *
     * @exception SQLException
     *                If a database access error occurs
     */
    @Override
    public java.sql.PreparedStatement prepareStatement(String sql,
                                                       int[] columnIndexes) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "prepareStatement", new Object[] { this, sql,
                                                            columnIndexes });

        PreparedStatement pstmt;

        try {
            if (state == INACTIVE)
                reactivate();

            // Only create a PSCacheKey if statement caching is enabled.
            // [d139351.16]

            synchronized (syncObject) {
                // Synchronize to make sure the transaction cannot be ended
                // until after
                // the statement is created.

                beginTransactionIfNecessary();

                pstmt = connImpl.prepareStatement(sql, columnIndexes);
            }

            // FIX ME - add cusor holdability
            pstmt = new JdbcPreparedStatement(pstmt, this); // [d164609]

            childWrappers.add(pstmt);
        } catch (SQLException ex) {

            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareStatement", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareStatement", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "prepareStatement", pstmt); // [d164609]
        return pstmt; // [d164609]
    }

    /**
     * <p>
     * Creates a default PreparedStatement object capable of returning the
     * auto-generated keys designated by the given array. This array contains
     * the names of the columns in the target table that contain the
     * auto-generated keys that should be returned. This array is ignored if the
     * SQL statement is not an INSERT statement.
     * </p>
     *
     *
     * <p>
     * An SQL statement with or without IN parameters can be pre-compiled and
     * stored in a PreparedStatement object. This object can then be used to
     * efficiently execute this statement multiple times.
     * </p>
     *
     * <p>
     * <b>Note</b>: This method is optimized for handling parametric SQL
     * statements that benefit from precompilation. If the driver supports
     * precompilation, the method prepareStatement will send the statement to
     * the database for precompilation. Some drivers may not support
     * precompilation. In this case, the statement may not be sent to the
     * database until the PreparedStatement object is executed. This has no
     * direct effect on users; however, it does affect which methods throw
     * certain SQLExceptions.
     * </p>
     *
     * <p>
     * Result sets created using the returned PreparedStatement object will by
     * default be type TYPE_FORWARD_ONLY and have a concurrency level of
     * CONCUR_READ_ONLY.
     * </p>
     *
     * @param sql
     *            an SQL statement that may contain one or more '?' IN parameter
     *            placeholders
     * @param columnNames
     *            an array of column names indicating the columns that should be
     *            returned from the inserted row or rows
     *
     * @return a new PreparedStatement object, containing the pre-compiled
     *         statement, that is capable of returning the auto-generated keys
     *         designated by the given array of column names
     *
     * @exception SQLException
     *                If a database access error occurs
     */
    @Override
    public java.sql.PreparedStatement prepareStatement(String sql,
                                                       String[] columnNames) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "prepareStatement", new Object[] { this, sql,
                                                            columnNames });

        PreparedStatement pstmt;

        try {
            if (state == INACTIVE)
                reactivate();

            // Only create a PSCacheKey if statement caching is enabled.
            // [d139351.16]

            synchronized (syncObject) {
                // Synchronize to make sure the transaction cannot be ended
                // until after
                // the statement is created.

                beginTransactionIfNecessary();

                pstmt = connImpl.prepareStatement(sql, columnNames);
            }

            // FIX ME - add cursor holdability
            pstmt = new JdbcPreparedStatement(pstmt, this); // [d164609]

            childWrappers.add(pstmt);
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareStatement", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareStatement", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "prepareStatement", pstmt); // [d164609]
        return pstmt; // [d164609]
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return null;
    }

    @Override
    public Blob createBlob() throws SQLException {
        return null;
    }

    @Override
    public Clob createClob() throws SQLException {
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
        return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return null;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return null;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return null;
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return null;
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return false;
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {}

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {}

    // @Override //java7
    @Override
    public void abort(Executor executor) {
        throw new UnsupportedOperationException();
    }

    // @Override //Java7
    @Override
    public int getNetworkTimeout() {
        throw new UnsupportedOperationException();
    }

    // @Override //Java7
    @Override
    public String getSchema() {
        throw new UnsupportedOperationException();
    }

    // @Override //Java7
    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) {
        throw new UnsupportedOperationException();
    }

    // @Override // Java 7
    @Override
    public void setSchema(String schema) {
        throw new UnsupportedOperationException();
    }

}
