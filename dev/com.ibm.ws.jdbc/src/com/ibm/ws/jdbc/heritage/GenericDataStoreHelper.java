/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.heritage;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import javax.resource.ResourceException;
import javax.security.auth.Subject;
import javax.transaction.xa.XAException;

/**
 * Extension point for compatibility with data store helpers.
 */
public abstract class GenericDataStoreHelper {
    /**
     * Cleans up a connection before it is returned to the connection
     * pool for later reuse. This method is also used when checking for invalid connections.
     *
     * @param conn the connection to clean up.
     * @return true if any standard connection property was modified, otherwise false.
     * @exception SQLException if an error occurs while cleaning up the connection.
     */
    public abstract boolean doConnectionCleanup(Connection conn) throws SQLException;

    /**
     * Invoked after the last active connection handle is closed.
     * This provides an opportunity to undo connection setup that was previously performed by
     * <code>doConnectionSetupPerGetConnection</code>.
     *
     * @param conn the connection to clean up.
     * @param isCMP always false.
     * @param unused always null.
     * @return boolean false indicates no connection cleanup is performed by this method, true otherwise. Default is false as its a no-op.
     * @throws SQLException if it fails.
     */
    public abstract boolean doConnectionCleanupPerCloseConnection(Connection conn, boolean isCMP, Object unused) throws SQLException;

    /**
     * Configures a connection before first use. This method is invoked only
     * when a new connection to the database is created. It is not invoked when connections
     * are reused from the connection pool.
     *
     * @param conn the connection to set up.
     * @exception SQLException if connection setup cannot be completed successfully.
     */
    public abstract void doConnectionSetup(Connection conn) throws SQLException;

    /**
     * Invoked per getConnection request when the connection handle count is 1,
     * meaning that the second, third, and so forth sharable connection handles are skipped over.
     *
     * @param conn connection to set up.
     * @param isCMP always false.
     * @param props java.util.Map with String key of "SUBJECT" and its value being the
     *        <code>javax.security.auth.Subject</code> if a Subject is available.
     * @return true if any connection setup is performed by this method, otherwise false.
     * @throws SQLException if it fails.
     */
    public abstract boolean doConnectionSetupPerGetConnection(Connection conn, boolean isCMP, Object props) throws SQLException;

    /**
     * Invoked prior to a connection being used in a transaction.
     *
     * @param subject subject for the newly requested connection if container authentication is used, otherwise null.
     * @param user user name for the newly requested connection. Null if container authentication is used and a subject is provided.
     * @param conm the connection.
     * @param reauthRequired indicates whether reauthentication is required to get the connection in sync with the subject or user name.
     * @param props <code>java.util.Properties</code> containing a property with key, "FIRST_TIME_CALLED", and value of "true" or "false"
     *        depending on whether or not this is the first time invoking this method for the specified connection.
     * @throws SQLException to indicate failure of this method.
     */
    public abstract void doConnectionSetupPerTransaction(Subject subject, String user, Connection conn, boolean reauthRequired, Object props) throws SQLException;

    /**
     * Cleans up a statement before the statement is placed in the statement cache.
     *
     * @param stmt the PreparedStatement.
     * @exception SQLException if an error occurs cleaning up the statement.
     */
    public abstract void doStatementCleanup(PreparedStatement stmt) throws SQLException;

    /**
     * Returns the default to use for transaction isolation level when not specified another way.
     *
     * @return transaction isolation level constant from java.sql.Connection
     * @throws ResourceException if raised by the data store helper.
     */
    public abstract int getIsolationLevel() throws ResourceException;

    /**
     * Returns metadata for the data store helper.
     *
     * @return metadata.
     */
    public abstract DataStoreHelperMetaData getMetaData();

    /**
     * Overrides the PrintWriter for JDBC driver trace.
     *
     * @return PrintWriter to use for JDBC driver trace.
     */
    public abstract PrintWriter getPrintWriter();

    /**
     * Provides additional logging information for an <code>XAException</code>.
     *
     * @param xae the exception.
     * @return detailed information about the exception.
     */
    public abstract String getXAExceptionContents(XAException xae);

    /**
     * Determines if the exception indicates that the connection ought to be
     * removed from or kept out of the connection pool.
     *
     * @param x the exception to check.
     * @return true to avoid pooling the connection, otherwise false.
     */
    public abstract boolean isConnectionError(SQLException x);

    /**
     * Determines if the exception indicates an unsupported operation.
     *
     * @param x the exception.
     * @return true if the exception indicates an unsupported operation, otherwise false.
     */
    public abstract boolean isUnsupported(SQLException x);

    /**
     * Used to identify an exception and possibly replace it (if replaceExceptions=true).
     *
     * @param x an exception.
     * @return the exception to identify as or replace with.
     */
    public abstract SQLException mapException(SQLException x);

    /**
     * Adds an XA start flag for loosely coupled transaction branches.
     *
     * @param xaStartFlags XA start flags to add to.
     * @return updated XA start flags which are a combination of the flags supplied to this method
     *         and the flag for loosely coupled transaction branches.
     */
    public abstract int modifyXAFlag(int xaStartFlags);

    /**
     * Supplies the dataSource configuration to the data store helper.
     *
     * @param config AtomicReference to the dataSource configuration.
     */
    public abstract void setConfig(Object config);

    /**
     * Overrides identification of SQLExceptions by supplying a map of
     * SQL state (a type String key) or error code (a type Integer key) to
     * <code>com.ibm.websphere.ce.cm.PortableSQLException</code> subclass
     * or <code>Void.class</code> (which indicates to ignore).
     */
    public abstract void setUserDefinedMap(@SuppressWarnings("rawtypes") Map map);
}
