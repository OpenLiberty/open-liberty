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

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import com.ibm.adapter.AdapterUtil;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * This class is a wrapper class for Statement.
 */
public class JdbcStatement extends JdbcObject implements Statement {

    private static final TraceComponent tc = Tr.register(JdbcStatement.class);

    /** The underlying Statement object. */
    Statement stmtImpl;

    java.sql.ResultSet rsImpl;

    /**
     * Do not use. Constructor exists only for PreparedStatement wrapper.
     */
    JdbcStatement() {}

    /**
     * Create a WebSphere Statement wrapper.
     *
     * @param stmtImplObject the JDBC Statement implementation class to wrap.
     * @param connWrapper the WebSphere JDBC Connection wrapper creating this statement.
     */
    JdbcStatement(Statement stmtImplObject, JdbcConnection connWrapper) {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "<init>",
                     new Object[] {
                                    AdapterUtil.toString(stmtImplObject),
                                    connWrapper });

        stmtImpl = stmtImplObject;
        parentWrapper = connWrapper;

        // I don't wrap ResultSet, so there is no child objects.
        childWrappers = null;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "addBatch", new Object[] { this, sql });

        try {
            stmtImpl.addBatch(sql);
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "addBatch", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void cancel() throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "cancel", this);

        try {
            stmtImpl.cancel();
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "Cancel", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "cancel", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "cancel");
    }

    @Override
    public void clearBatch() throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "clearBatch", this);

        try {
            stmtImpl.clearBatch();
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "clearBatch", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void clearWarnings() throws SQLException {
        try {
            stmtImpl.clearWarnings();
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "clearWarnings", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Close the ResultSet for this Statement, if it's still around. Close even if the execute
     * is not a query, as stated in the java.sql.Statement API.
     */
    final void closeResultSets() {
        try {
            rsImpl.close();
        } catch (SQLException ex) {
        } finally {
            rsImpl = null;
        }
    }

    /**
     * Perform any wrapper-specific close logic. This method is called by the default
     * JdbcObject close method.
     *
     * @return SQLException the first error to occur while closing the object.
     */
    @Override
    SQLException closeWrapper() {
        // Indicate the statement is closed by setting the parent object's statement to null.
        // This will allow us to be garbage collected.

        try // Connection wrapper can close at any time.
        {
            parentWrapper.childWrappers.remove(this);
        } catch (RuntimeException runtimeX) {
            // No FFDC code needed; parent wrapper might be closed.
            if (parentWrapper.state != CLOSED)
                throw runtimeX;
        }

        try // Close the JDBC driver ResultSet implemenation object.
        {
            stmtImpl.close();
        } catch (SQLException closeX) {
            stmtImpl = null;
            return closeX;
        }

        stmtImpl = null;
        return null;
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "execute", new Object[] { this, sql });

        boolean result = false;

        try {
            // Synchronize to make sure the transaction cannot be ended until after the
            // statement completes.

            synchronized (parentWrapper.syncObject) {
                parentWrapper.beginTransactionIfNecessary();

                if (rsImpl != null)
                    closeResultSets();

                result = stmtImpl.execute(sql);
            }
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "execute", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "execute", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "execute", result ? "QUERY" : "UPDATE");
        return result;
    }

    @Override
    public int[] executeBatch() throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "executeBatch", this);

        int[] results;

        try {
            // Synchronize to make sure the transaction cannot be ended until after the
            // statement completes.

            synchronized (parentWrapper.syncObject) {
                parentWrapper.beginTransactionIfNecessary();

                if (rsImpl != null)
                    closeResultSets();

                results = stmtImpl.executeBatch();
            }
        } catch (BatchUpdateException batchX) {

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Check for Connection Error",
                         new Object[] {
                                        "SQL STATE:  " + batchX.getSQLState(),
                                        "ERROR CODE: " + batchX.getErrorCode(),
                                        batchX });

            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeBatch", "Exception");
            throw batchX;
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeBatch", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeBatch", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled()) {
            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < results.length; i++)
                sb.append(i).append(' ');

            Tr.exit(tc, "executeBatch", new String(sb));
        }

        return results;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "executeQuery", new Object[] { this, sql });

        try {
            // Synchronize to make sure the transaction cannot be ended until after the
            // statement completes.

            synchronized (parentWrapper.syncObject) {
                parentWrapper.beginTransactionIfNecessary();

                if (rsImpl != null)
                    closeResultSets();

                rsImpl = stmtImpl.executeQuery(sql);
            }
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeQuery", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeQuery", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "executeQuery", rsImpl);
        return rsImpl;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "executeUpdate", new Object[] { this, sql });

        int numUpdates;

        try {
            // Synchronize to make sure the transaction cannot be ended until after the
            // statement completes.

            synchronized (parentWrapper.syncObject) {
                parentWrapper.beginTransactionIfNecessary();

                if (rsImpl != null)
                    closeResultSets();

                numUpdates = stmtImpl.executeUpdate(sql);
            }
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeUpdate", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeUpdate", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "executeUpdate", new Integer(numUpdates));
        return numUpdates;
    }

    @Override
    public final Connection getConnection() throws SQLException {
        Connection conn = (Connection) parentWrapper;

        if (state == CLOSED || conn == null)
            throw new SQLException("Object statement is closed");

        return conn;
    }

    /**
     * @return the Connection wrapper for this object, or null if none is available.
     */
    @Override
    public final JdbcObject getConnectionWrapper() {
        return parentWrapper;
    }

    @Override
    public int getFetchDirection() throws SQLException {
        try {
            return stmtImpl.getFetchDirection();
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public final int getFetchSize() throws SQLException {
        if (state == CLOSED)
            throw new SQLException("Object Statement is closed.");

        return stmtImpl.getFetchSize();
    }

    /**
     * @return the underlying JDBC implementation object which we are wrapping.
     */
    @Override
    final Object getJDBCImplObject() {
        return stmtImpl;
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        try {
            return stmtImpl.getMaxFieldSize();
        } catch (SQLException ex) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "getMaxFieldSize",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public int getMaxRows() throws SQLException {
        try {
            return stmtImpl.getMaxRows();
        } catch (SQLException ex) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "getMaxRows",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getMoreResults", this);

        boolean hasMoreResults;

        try {
            // Synchronize on the connection wrapper's syncObject to prevent concurrent access
            // with the getResultSet method.

            synchronized (parentWrapper.syncObject) {
                if (rsImpl != null)
                    closeResultSets();
                hasMoreResults = stmtImpl.getMoreResults();
            }
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getMoreResults", "Exception");
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "getMoreResults",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            if (tc.isEntryEnabled())
                Tr.exit(tc, "execute", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getMoreResults", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(
                    tc,
                    "getMoreResults",
                    hasMoreResults ? Boolean.TRUE : Boolean.FALSE);

        return hasMoreResults;
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        try {
            return stmtImpl.getQueryTimeout();
        } catch (SQLException ex) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "getQueryTimeout",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getResultSet", this);

        try {
            // Synchronize on the Connection's sync object to prevent concurrent access with
            // the execute methods, which must begin a transaction on the connection, close
            // open result sets, and execute.

            synchronized (parentWrapper.syncObject) {
                // Choose the ResultSet wrapper based on Connection wrapper type.
                // This method can return a null Result Set, which we shouldn't wrap.

                rsImpl = stmtImpl.getResultSet();
                childWrappers.add(rsImpl);
            }
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getResultSet", "Exception");
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "getResultSet",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getResultSet", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getResultSet", rsImpl);
        return rsImpl;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        try {
            return stmtImpl.getResultSetConcurrency();
        } catch (SQLException ex) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "getResultSetConcurrency",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public int getResultSetType() throws SQLException {
        try {
            return stmtImpl.getResultSetType();
        } catch (SQLException ex) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "getResultSetType",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @return the trace component for the JdbcStatement.
     */
    @Override
    TraceComponent getTracer() {
        return tc;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getUpdateCount", this);

        int updateCount;

        try {
            updateCount = stmtImpl.getUpdateCount();
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getUpdateCount", "Exception");
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "getUpdateCount",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getUpdateCount", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getUpdateCount", new Integer(updateCount));
        return updateCount;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        try {
            return stmtImpl.getWarnings();
        } catch (SQLException ex) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "getWarnings",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * Collects FFDC information specific to this JDBC wrapper. Formats this information to
     * the provided FFDC logger. This method is used by introspectAll to collect any wrapper
     * specific information.
     *
     * @param info FFDCLogger on which to record the FFDC information.
     */
    @Override
    void introspectWrapperSpecificInfo(com.ibm.ws.rsadapter.FFDCLogger info) {
        info.append(
                    "Underlying Statement: " + AdapterUtil.toString(stmtImpl),
                    stmtImpl);
    }

    /**
     * @param runtimeX a RuntimeException which occurred, indicating the wrapper may be closed.
     *
     * @return the RuntimeException to throw if it isn't.
     */
    @Override
    final RuntimeException runtimeXIfNotClosed(RuntimeException runtimeX) throws SQLException {
        if (state == CLOSED)
            throw new SQLException("Object Statement is closed.");

        return runtimeX;
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setCursorName", new Object[] { this, name });

        try {
            stmtImpl.setCursorName(name);
        } catch (SQLException ex) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "setCursorName",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }

    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "setEscapeProcessing",
                     new Object[] { this, enable ? Boolean.TRUE : Boolean.FALSE });

        try {
            stmtImpl.setEscapeProcessing(enable);
        } catch (SQLException ex) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "setEscapeProcessing",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "setFetchDirection",
                     new Object[] { this, new Integer(direction) });

        try {
            stmtImpl.setFetchDirection(direction);
        } catch (SQLException ex) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "setFetchDirection",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public final void setFetchSize(int rows) throws SQLException {
        if (state == CLOSED)
            throw new SQLException("Object Statement is closed");

        if (rows < 0)
            throw new SQLException("Fetch size should not be negative");
        // d144049

        // Set the requested fetchSize. This value will be enforced before executing the
        // statement. [d139351.11]

        if (tc.isDebugEnabled())
            Tr.debug(tc, "set fetchSize --> " + rows, this);
        stmtImpl.setFetchSize(rows);
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "setMaxFieldSize",
                     new Object[] { this, new Integer(max) });

        try {
            stmtImpl.setMaxFieldSize(max);
        } catch (SQLException ex) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "setMaxFieldSize",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setMaxRows", new Object[] { this, new Integer(max) });

        try {
            stmtImpl.setMaxRows(max);
        } catch (SQLException ex) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "setMaxRows",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "setQueryTimeout",
                     new Object[] { this, new Integer(seconds) });

        try {
            stmtImpl.setQueryTimeout(seconds);
        } catch (SQLException ex) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "setQueryTimeout",
                         new Object[] {
                                        "SQL STATE:  " + ex.getSQLState(),
                                        "ERROR CODE: " + ex.getErrorCode(),
                                        ex });
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Moves to this Statement object's next result, deals with any current ResultSet
     * object(s) according to the instructions specified by the given flag, and returns
     * true if the next result is a ResultSet object. </p>
     *
     * <p>There are no more results when the following is true: </p>
     * 
     * <pre>
     *  (!getMoreResults() && (getUpdateCount() == -1)
     * </pre>
     * 
     * <p>
     *
     * @param current one of the following Statement constants indicating what should happen
     *            to current ResultSet objects obtained using the method getResultSetCLOSE_CURRENT_RESULT,
     *            KEEP_CURRENT_RESULT, or CLOSE_ALL_RESULTS
     *
     * @return true if the next result is a ResultSet object; false if it is an update count
     *         or there are no more results
     *
     * @exception SQLException If a database access error occurs
     */
    @Override
    public boolean getMoreResults(int current) throws SQLException { // LIDB2040.4.3
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getMoreResults", new Object[] {
                                                          this, AdapterUtil.getResultSetCloseString(current) });

        boolean hasMoreResults = stmtImpl.getMoreResults(current);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getMoreResults", hasMoreResults ? Boolean.TRUE : Boolean.FALSE);

        return hasMoreResults;
    }

    /**
     * Method getGeneratedKeys.
     * <p>Retrieves any auto-generated keys created as a result of executing this Statement
     * object. If this Statement object did not generate any keys, an empty ResultSet object
     * is returned. </p>
     *
     * @return ResultSet object containing the auto-generated key(s) generated by the
     *         execution of this Statement object
     *
     * @exception SQLException If a database access error occurs
     */
    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        try {
            return stmtImpl.getGeneratedKeys();
        } catch (SQLException ex) {
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Executes the given SQL statement and signals the driver with the given flag about
     * whether the auto-generated keys produced by this Statement object should be made
     * available for retrieval. </p>
     *
     * @param sql must be an SQL INSERT, UPDATE or DELETE statement or an SQL statement that
     *            returns nothing
     * @param autoGeneratedKeys a flag indicating whether auto-generated keys should be made
     *            available for retrieval; one of the following constants: Statement.RETURN_GENERATED_KEYS
     *            Statement.NO_GENERATED_KEYS
     *
     * @return either the row count for INSERT, UPDATE or DELETE statements, or 0 for SQL
     *         statements that return nothing
     *
     * @exception SQLException If a database access error occurs, the given SQL statement
     *                returns a ResultSet object, or the given constant is not one of those allowed
     */
    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "executeUpdate", new Object[] { this, sql,
                                                         new Integer(autoGeneratedKeys) });

        int numUpdates;

        try {
            // Synchronize to make sure the transaction cannot be ended until after the
            // statement completes.

            synchronized (parentWrapper.syncObject) {
                parentWrapper.beginTransactionIfNecessary();

                numUpdates = stmtImpl.executeUpdate(sql, autoGeneratedKeys);
            }
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeUpdate", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeUpdate", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "executeUpdate", new Integer(numUpdates));
        return numUpdates;
    }

    /**
     * <p>Executes the given SQL statement and signals the driver that the auto-generated
     * keys indicated in the given array should be made available for retrieval. The driver
     * will ignore the array if the SQL statement is not an INSERT statement. </p>
     *
     * @param sql an SQL INSERT, UPDATE or DELETE statement or an SQL statement that returns
     *            nothing, such as an SQL DDL statement
     * @param columnIndexes an array of column indexes indicating the columns that should
     *            be returned from the inserted row
     *
     * @return either the row count for INSERT, UPDATE, or DELETE statements, or 0 for SQL
     *         statements that return nothing
     *
     * @exception SQLException If a database access error occurs or the SQL statement returns
     *                a ResultSet object
     */
    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "executeUpdate", new Object[] { this, sql,
                                                         columnIndexes });

        int numUpdates;

        try {
            // Synchronize to make sure the transaction cannot be ended until after the
            // statement completes.

            synchronized (parentWrapper.syncObject) {
                parentWrapper.beginTransactionIfNecessary();

                numUpdates = stmtImpl.executeUpdate(sql, columnIndexes);
            }
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeUpdate", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeUpdate", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "executeUpdate", new Integer(numUpdates));
        return numUpdates;
    }

    /**
     * <p>Executes the given SQL statement and signals the driver that the auto-generated
     * keys indicated in the given array should be made available for retrieval. The driver
     * will ignore the array if the SQL statement is not an INSERT statement. </p>
     *
     * @param sql an SQL INSERT, UPDATE or DELETE statement or an SQL statement that returns
     *            nothing
     * @param columnNames an array of the names of the columns that should be returned from
     *            the inserted row
     *
     * @return either the row count for INSERT, UPDATE, or DELETE statements, or 0 for SQL
     *         statements that return nothing
     *
     * @exception SQLException If a database access error occurs
     */
    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "executeUpdate", new Object[] { this, sql,
                                                         columnNames });

        int numUpdates;

        try {
            // Synchronize to make sure the transaction cannot be ended until after the
            // statement completes.

            synchronized (parentWrapper.syncObject) {
                parentWrapper.beginTransactionIfNecessary();

                numUpdates = stmtImpl.executeUpdate(sql, columnNames);
            }
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeUpdate", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeUpdate", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "executeUpdate", new Integer(numUpdates));
        return numUpdates;
    }

    /**
     * <p>Executes the given SQL statement, which may return multiple results, and signals
     * the driver that any auto-generated keys should be made available for retrieval. The
     * driver will ignore this signal if the SQL statement is not an INSERT statement. </p>
     *
     * <p>In some (uncommon) situations, a single SQL statement may return multiple result
     * sets and/or update counts. Normally you can ignore this unless you are (1) executing
     * a stored procedure that you know may return multiple results or (2) you are dynamically
     * executing an unknown SQL string. </p>
     *
     * <p>The execute method executes an SQL statement and indicates the form of the first
     * result. You must then use the methods getResultSet or getUpdateCount to retrieve the
     * result, and getMoreResults to move to any subsequent result(s). </p>
     *
     * @param sql any SQL statement
     * @param autoGeneratedKeys a constant indicating whether auto-generated keys should be
     *            made available for retrieval using the method getGeneratedKeys; one of the following
     *            constants: Statement.RETURN_GENERATED_KEYS or Statement.NO_GENERATED_KEYS
     *
     * @return true if the first result is a ResultSet object; false if it is an update count
     *         or there are no results
     *
     * @exception SQLException If a database access error occurs
     */
    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "execute", new Object[] { this, sql,
                                                   new Integer(autoGeneratedKeys) });

        boolean result;

        try {
            // Synchronize to make sure the transaction cannot be ended until after the
            // statement completes.

            synchronized (parentWrapper.syncObject) {
                parentWrapper.beginTransactionIfNecessary();

                result = stmtImpl.execute(sql, autoGeneratedKeys);
            }
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "execute", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "execute", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "execute", result ? "QUERY" : "UPDATE");
        return result;
    }

    /**
     * <p>Executes the given SQL statement, which may return multiple results, and signals
     * the driver that the auto-generated keys indicated in the given array should be made
     * available for retrieval. This array contains the indexes of the columns in the target
     * table that contain the auto-generated keys that should be made available. The driver
     * will ignore the array if the given SQL statement is not an INSERT statement. </p>
     *
     * <p>Under some (uncommon) situations, a single SQL statement may return multiple result
     * sets and/or update counts. Normally you can ignore this unless you are (1) executing a
     * stored procedure that you know may return multiple results or (2) you are dynamically
     * executing an unknown SQL string. </p>
     *
     * <p>The execute method executes an SQL statement and indicates the form of the first
     * result. You must then use the methods getResultSet or getUpdateCount to retrieve the
     * result, and getMoreResults to move to any subsequent result(s). </p>
     *
     * @param sql any SQL statement
     * @param columnIndexes an array of the indexes of the columns in the inserted row that
     *            should be made available for retrieval by a call to the method getGeneratedKeys
     *
     * @return true if the first result is a ResultSet object; false if it is an update count
     *         or there are no results
     *
     * @exception SQLException If a database access error occurs
     */
    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "execute", new Object[] { this, sql,
                                                   columnIndexes });

        boolean result;

        try {
            // Synchronize to make sure the transaction cannot be ended until after the
            // statement completes.

            synchronized (parentWrapper.syncObject) {
                parentWrapper.beginTransactionIfNecessary();

                result = stmtImpl.execute(sql, columnIndexes);
            }
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "execute", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "execute", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "execute", result ? "QUERY" : "UPDATE");
        return result;
    }

    /**
     * <p>Executes the given SQL statement, which may return multiple results, and signals
     * the driver that the auto-generated keys indicated in the given array should be made
     * available for retrieval. This array contains the names of the columns in the target
     * table that contain the auto-generated keys that should be made available. The driver
     * will ignore the array if the given SQL statement is not an INSERT statement. </p>
     *
     * <p>In some (uncommon) situations, a single SQL statement may return multiple result
     * sets and/or update counts. Normally you can ignore this unless you are (1) executing
     * a stored procedure that you know may return multiple results or (2) you are dynamically
     * executing an unknown SQL string. </p>
     *
     * <p>The execute method executes an SQL statement and indicates the form of the first
     * result. You must then use the methods getResultSet or getUpdateCount to retrieve the
     * result, and getMoreResults to move to any subsequent result(s). </p>
     *
     * @param sql any SQL statement
     * @param columnNames an array of the names of the columns in the inserted row that should
     *            be made available for retrieval by a call to the method getGeneratedKeys
     *
     * @return true if the next result is a ResultSet object; false if it is an update count
     *         or there are no more results
     *
     * @exception SQLException If a database access error occurs
     */
    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "execute", new Object[] { this, sql,
                                                   columnNames });

        boolean result;

        try {
            // Synchronize to make sure the transaction cannot be ended until after the
            // statement completes.

            synchronized (parentWrapper.syncObject) {
                parentWrapper.beginTransactionIfNecessary();

                result = stmtImpl.execute(sql, columnNames);
            }
        } catch (SQLException ex) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "execute", "Exception");
            throw AdapterUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "execute", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "execute", result ? "QUERY" : "UPDATE");
        return result;
    }

    /**
     * <p>Retrieves the result set holdability for ResultSet objects generated by this
     * Statement object. </p>
     *
     * @return either ResultSet.HOLD_CURSORS_OVER_COMMIT or ResultSet.CLOSE_CURSORS_AT_COMMIT
     *
     * @exception SQLException If a database access error occurs
     */
    @Override
    public int getResultSetHoldability() throws SQLException {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    // d162314.1 ends

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return false;
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return false;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {}

    //@Override //Java7
    @Override
    public void closeOnCompletion() {
        throw new UnsupportedOperationException();
    }

    //@Override //Java7
    @Override
    public boolean isCloseOnCompletion() {
        throw new UnsupportedOperationException();
    }

}
