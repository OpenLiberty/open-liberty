/*******************************************************************************
 * Copyright (c) 2001, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTimeoutException; 
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Wrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jdbc.osgi.JDBCRuntimeVersion;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.wsspi.uow.UOWManager; 
import com.ibm.wsspi.uow.UOWManagerFactory; 

/**
 * This class wraps a java.sql.Statement object.
 */
public class WSJdbcStatement extends WSJdbcObject implements Statement {
    private static final TraceComponent tc = Tr.register(
                                                         WSJdbcStatement.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * Setter methods for vendor properties. When invoked via the wrapper pattern or
     * WSCallHelper, the haveStatementPropertiesChanged indicator should be set so that
     * we later invoke the data store helper to reset properties to the original value
     * if the statement is cached.
     * 
     */
    static final Set<String> VENDOR_PROPERTY_SETTERS = new HashSet<String>();
    static {
        VENDOR_PROPERTY_SETTERS.add("setLongDataCacheSize");
        VENDOR_PROPERTY_SETTERS.add("setResponseBuffering");

        // Oracle methods
        VENDOR_PROPERTY_SETTERS.add("setLobPrefetchSize"); 
        VENDOR_PROPERTY_SETTERS.add("defineColumnType");
        VENDOR_PROPERTY_SETTERS.add("defineColumnTypeBytes");
        VENDOR_PROPERTY_SETTERS.add("defineColumnTypeChars");
        VENDOR_PROPERTY_SETTERS.add("setExecuteBatch");
        VENDOR_PROPERTY_SETTERS.add("setRowPrefetch");

    }

    /** The underlying Statement object. */
    protected Statement stmtImpl; 

    /** The current fetchSize value. */
    protected int currentFetchSize; 

    /** The requested fetchSize value. */
    int requestedFetchSize;

    /** Indicates whether any statement properties have changed. */
    protected boolean haveStatementPropertiesChanged;

    /** Indicate the cursor holdability value */
    protected int holdability; 

    /**
     * The poolability hint for this statement. The default for java.sql.Statement is FALSE.
     * For prepared statements and callable statements, the default is TRUE.
     * 
     */
    protected boolean poolabilityHint;

    /**
     * Indicates if any parameters are set on the prepared batch statement that haven't been
     * executed yet. This value applies only to PreparedStatements and CallableStatements.
     */
    protected boolean hasBatchParameters;

    /**
     * Value of the query timeout before syncing it with the transaction timeout.
     * When statements from unsharable connections are kept open after the JTA transaction ends,
     * we need to restore this original query timeout value before executing and queries outside
     * of a JTA transaction. A value of NULL indicates that we are not tracking any previous value.
     * 
     */
    private Integer queryTimeoutBeforeSync;

    /**
     * Indicates if the user has explicitly specified a query timeout via the
     * Statement.setQueryTimeout interface. In this case, the
     * syncQueryTimeoutWithTransactionTimeout data source property should be ignored.
     * 
     */
    private boolean queryTimeoutSetByUser;
    
    protected boolean closeOnCompletion = false;
    
    /**
     * Do not use. Constructor exists only for PreparedStatement wrapper.
     */
    public WSJdbcStatement() 
    {}

    /**
     * Create a WebSphere Statement wrapper.
     * 
     * @param stmtImplObject the JDBC Statement implementation class to wrap.
     * @param connWrapper the WebSphere JDBC Connection wrapper creating this statement.
     * @param theHoldability the cursor holdability value of this statement
     */
    public WSJdbcStatement(Statement stmtImplObject, WSJdbcConnection connWrapper, int theHoldability) 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "<init>", AdapterUtil.toString(stmtImplObject), connWrapper, AdapterUtil.getCursorHoldabilityString(theHoldability));

        stmtImpl = stmtImplObject;
        init(connWrapper); 

        mcf = parentWrapper.mcf;

        holdability = theHoldability; 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>");
    }

    /**
     * @see java.sql.Statement#addBatch(String)
     */
    public void addBatch(String sql) throws SQLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "addBatch", sql); 

        try {
            stmtImpl.addBatch(sql);
        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "addBatch", ex); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.Statement#cancel()
     */
    public void cancel() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "cancel");

        try {
            stmtImpl.cancel();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.cancel", "94", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "cancel", "Exception");
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "cancel", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "cancel");
    }

    /**
     * @see java.sql.Statement#clearBatch()
     */
    public void clearBatch() throws SQLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "clearBatch"); 

        try {
            stmtImpl.clearBatch();

            // Reset the batch parameter flag when the batch is cleared. 
            hasBatchParameters = false;
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.clearBatch", "116", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @see java.sql.Statement#clearWarnings()
     */
    public void clearWarnings() throws SQLException {
        try {
            stmtImpl.clearWarnings();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.clearWarnings", "136", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * <p>Close the first result set stored in childWrapper.</p>
     * 
     * <p>When this method is called, the childWrapper is guaranteed to be not null.</p>
     */
    final protected void closeAndRemoveResultSet() {
        closeAndRemoveResultSet(false); 
    }

    /**
     * <p>Close the first result set stored in childWrapper.</p>
     * 
     * @param closeWrapperOnly boolean flag to indicate that only wrapper-closure activities
     *            should be performed, but close of the underlying object is unnecessary.
     * 
     *            <p>When this method is called, the childWrapper is guaranteed to be not null.</p>
     */
    final protected void closeAndRemoveResultSet(boolean closeWrapperOnly) { 
        //  close and remove the first result set

        //  - remove childWrapper != null check since the precondition of this method
        // is that the childWrapper is not null
        try {
            ((WSJdbcObject) childWrapper).close(closeWrapperOnly); 
        } catch (SQLException ex) {
            // Just trace the error since we need to continue
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.closeAndRemoveResultSet",
                                        "275", this);
            Tr.warning(tc, "ERR_CLOSING_OBJECT", childWrapper, ex);
        }

        childWrapper = null;

    }

    /**
     * Close the ResultSet for this Statement, if it's still around. Close even if the execute
     * is not a query, as stated in the java.sql.Statement API.
     * 
     * <p>When this method is called, childWrappers are guaranteed to have at least one element.</p>
     */
    final protected void closeAndRemoveResultSets() {
        closeAndRemoveResultSets(false); 
    }

    /**
     * Close the ResultSet for this Statement, if it's still around. Close even if the execute
     * is not a query, as stated in the java.sql.Statement API.
     * 
     * @param closeWrapperOnly boolean flag to indicate that only wrapper-closure activities
     *            should be performed, but close of the underlying object is unnecessary.
     * 
     *            <p>When this method is called, childWrappers are guaranteed to have at least one element.</p>
     */
    final protected void closeAndRemoveResultSets(boolean closeWrapperOnly) 
    {
        //  Close and remove all the result sets in the childWrappers

        //  - remove childWrappers.isEmpty() check since the precondition of this method
        // is that childWrappers have at least one element.
        for (int i = childWrappers.size() - 1; i > -1; i--) {
            try {
                ((WSJdbcObject) childWrappers.get(i)).close(closeWrapperOnly); 
            } catch (SQLException ex) {
                // Just trace the error since we need to continue
                FFDCFilter.processException(
                                            ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.closeAndRemoveResultSets", "277", this);
                Tr.warning(tc, "ERR_CLOSING_OBJECT", ex);
            }
        }
    }

    /**
     * Perform any wrapper-specific close logic. This method is called by the default
     * WSJdbcObject close method.
     * 
     * @param closeWrapperOnly boolean flag to indicate that only wrapper-closure activities
     *            should be performed, but close of the underlying object is unnecessary.
     * 
     * @return SQLException the first error to occur while closing the object.
     * @see com.ibm.ws.rsadapter.jdbc.WSJdbcObject#closeWrapper(boolean)
     */
    protected SQLException closeWrapper(boolean closeWrapperOnly) 
    {
        // Indicate the statement is closed by setting the parent object's statement to null.
        // This will allow us to be garbage collected.

        try // Connection wrapper can close at any time.
        {
            parentWrapper.childWrappers.remove(this);
        } catch (RuntimeException runtimeX) {
            // No FFDC code needed; parent wrapper might be closed.
            if (parentWrapper.state != State.CLOSED)
                throw runtimeX;
        }

        try // Close the JDBC driver ResultSet implemenation object.
        {
            stmtImpl.close();
        } catch (SQLException closeX) {
            FFDCFilter.processException(closeX,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.closeWrapper", "314", this);

            stmtImpl = null;
            return WSJdbcUtil.mapException(this, closeX);
        }

        stmtImpl = null;
        return null;
    }

    /**
     * Creates a wrapper for the supplied ResultSet object.
     * 
     * @param rsetImplObject a ResultSet that needs a wrapper.
     * 
     * @return the ResultSet wrapper if a valid ResultSet. Null if the ResultSet is null.
     */
    protected WSJdbcResultSet createResultSetWrapper(ResultSet rsetImplObject) 
    {
        return rsetImplObject == null ? null : mcf.jdbcRuntime.newResultSet(rsetImplObject, this);
    }

    /**
     * Update statement properties before executing the statement.
     * This method syncs the query timeout to the transaction timeout when appropriate
     * and updates the current fetchSize according to the requested fetchSize value, if needed.
     * This method should be invoked before executing the statement. 
     * 
     * @throws SQLException if an error occurs updating the Statement properties.
     */
    protected final void enforceStatementProperties() throws SQLException 
    {
        // Synchronization not needed since this method will always be called from execute
        // methods which are already synchronized.
        // RRA does not support multithreaded access so synchronization is no
        // longer needed on the execute methods either.

        if (requestedFetchSize != currentFetchSize) {
            stmtImpl.setFetchSize(requestedFetchSize);
            currentFetchSize = requestedFetchSize;
        }

        // Data source property syncQueryTimeoutWithTransactionTimeout indicates that we
        // should set the query timeout to equal the amount of time remaining in the JTA
        // transaction timeout, if a JTA transaction is active, and if no query timeout
        // has been programmatically set the by the user.
        if (dsConfig.get().syncQueryTimeoutWithTransactionTimeout && !queryTimeoutSetByUser) 
        {
            final boolean trace = TraceComponent.isAnyTracingEnabled();

            // If in a JTA transaction, then set query timeout to time remaining in JTA transaction
            if (((WSJdbcConnection) parentWrapper).managedConn.isGlobalTransactionActive()) {
                // Find out how much time remains in the JTA transaction timeout
                UOWManager uowmgr = UOWManagerFactory.getUOWManager();
                long expireTime = uowmgr.getUOWExpiration();
                
                // 0 means tran is not set to timeout
                if (expireTime != 0l) {
                    long remainingTime = expireTime - System.currentTimeMillis();
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc,
                                 "Milliseconds remaining in transaction timeout: " + remainingTime);

                    // Raise an error if the transaction should have already timed out.
                    if (remainingTime <= 0l)
                        throw new SQLTimeoutException(
                                                      "Transaction timeout " + (-remainingTime) + " ms ago.",
                                        "25000"); // Invalid transaction state

                    // Convert to seconds, rounding up
                    remainingTime = (remainingTime + 999l) / 1000l;

                    // Track the previous value, in order to restore after transaction ends
                    if (queryTimeoutBeforeSync == null)
                        queryTimeoutBeforeSync = stmtImpl.getQueryTimeout();

                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc,
                                 "Setting query timeout to " + remainingTime);

                    stmtImpl.setQueryTimeout((int) remainingTime);
                    haveStatementPropertiesChanged = true;
                } else {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Transaction has no timeout set");
                }

            }
            // If not in JTA transaction, then might need to restore to previous value
            else if (queryTimeoutBeforeSync != null) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "Restoring query timeout to " + queryTimeoutBeforeSync);

                stmtImpl.setQueryTimeout(queryTimeoutBeforeSync);
                queryTimeoutBeforeSync = null;
            }
        }
    }


    /**
     * @see java.sql.Statement#execute(String)
     */
    public boolean execute(String sql) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "execute", sql);

        boolean result;

        try {
            if (childWrapper != null) {
                closeAndRemoveResultSet();
            }

            if (childWrappers != null && !childWrappers.isEmpty()) {
                closeAndRemoveResultSets();
            }

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            result = stmtImpl.execute(sql);

        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "execute", ex); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "execute", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "execute", result ? "QUERY" : "UPDATE");
        return result;
    }

    /**
     * @see java.sql.Statement#executeBatch()
     */
    public int[] executeBatch() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "executeBatch");

        int[] results;

        try {
            if (childWrapper != null) {
                closeAndRemoveResultSet();
            }

            if (childWrappers != null && !childWrappers.isEmpty()) {
                closeAndRemoveResultSets();
            }

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            results = stmtImpl.executeBatch();

            // Batch parameters are cleared after executing the batch.  So reset the
            // batch parameter flag. 

            hasBatchParameters = false; 

        } catch (BatchUpdateException batchX) {
            // No FFDC code needed. Might be an application error. 

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "Check for Connection Error", AdapterUtil.getStackTraceWithState(batchX));

            // Ask the helper if this is a Connection Error, but don't actually map
            // the exception, since the user would have no way of knowing it's a
            // BatchUpdateException if we map it to something else.

            try {
                WSJdbcConnection connWrapper = (WSJdbcConnection) parentWrapper;

                if (mcf.getHelper().isConnectionError(batchX)) {
                    if (tc.isEventEnabled())
                        Tr.event(this, tc,
                                 "Encountered a Stale Connection: ", connWrapper);

                    connWrapper.fireConnectionErrorEvent(batchX, true);
                }
            } catch (NullPointerException nullX) {
                // No FFDC code needed; we might be closed.
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "executeBatch", "Exception");
                throw runtimeXIfNotClosed(nullX);
            }

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeBatch", "Exception");
            throw batchX;
        } catch (SQLException sqlX) {
            // No FFDC code needed. Might be an application error. 
            // map as usual
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeBatch", sqlX); 
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeBatch", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "executeBatch", Arrays.toString(results));
        return results;
    }



    public ResultSet executeQuery(String sql) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "executeQuery", sql);

        //  - change type from ResultSet to WSJdbcResultSet
        WSJdbcResultSet rsetWrapper = null;

        try {
            if (childWrapper != null) {
                closeAndRemoveResultSet();
            }

            if (childWrappers != null && !childWrappers.isEmpty()) {
                closeAndRemoveResultSets();
            }

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            childWrapper = rsetWrapper = createResultSetWrapper(stmtImpl.executeQuery(sql));

        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeQuery", ex); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeQuery", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }
        
        rsetWrapper.sql = sql;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "executeQuery", childWrapper);
        return rsetWrapper;
    }

    public int executeUpdate(String sql) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "executeUpdate", sql);

        int numUpdates;

        try {
            if (childWrapper != null) {
                closeAndRemoveResultSet();
            }

            if (childWrappers != null && !childWrappers.isEmpty()) {
                closeAndRemoveResultSets();
            }

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            numUpdates = stmtImpl.executeUpdate(sql);

        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeUpdate", ex); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeUpdate", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "executeUpdate", numUpdates);
        return numUpdates;
    }

    public final Connection getConnection() throws SQLException {
        Connection conn = (Connection) parentWrapper;

        if (state == State.CLOSED || conn == null)
            throw createClosedException("Statement"); 

        return conn;
    }

    /**
     * @return the Connection wrapper for this object, or null if none is available.
     */
    final protected WSJdbcObject getConnectionWrapper() 
    {
        return parentWrapper;
    }

    public int getFetchDirection() throws SQLException {
        try {
            return stmtImpl.getFetchDirection();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.getFetchDirection", "551", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public final int getFetchSize() throws SQLException {
        if (state == State.CLOSED)
            throw createClosedException("Statement"); 

        // Return the requested fetchSize since this value will be enforced before executing
        // the statement. This is done because certain drivers, like Oracle, actually reparse
        // PreparedStatements when fetchSize is changed. By delaying the set, we will often be
        // able to avoid the need to perform it. 

        return requestedFetchSize;
    }

    /**
     * @return the underlying JDBC implementation object which we are wrapping.
     */
    final protected Wrapper getJDBCImplObject() 
    {
        return stmtImpl;
    }

    public int getMaxFieldSize() throws SQLException {
        try {
            return stmtImpl.getMaxFieldSize();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.getMaxFieldSize", "591", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getMaxRows() throws SQLException {
        try {
            return stmtImpl.getMaxRows();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.getMaxRows", "611", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public boolean getMoreResults() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "getMoreResults");

        boolean hasMoreResults;
        boolean deferCloseOnCompletion = closeOnCompletion; // Save closeOnCompletion value
        closeOnCompletion = false; // temporarily disable closeOnCompletion because there may
        // be a short time here where the statement doesn't have reference to any more ResultSets

        try {
            //  close all the result sets
            if (childWrapper != null) {
                closeAndRemoveResultSet(true); 
            }

            if (childWrappers != null && !childWrappers.isEmpty()) {
                closeAndRemoveResultSets(true); 
            }

            //  - move this call AFTER children are closed, as in WAS 5.1
            hasMoreResults = stmtImpl.getMoreResults();
            
            // If there are truly no more ResultSets for this Statement and closeOnCompletion
            // is enabled, close the Statement
            if(deferCloseOnCompletion && !hasMoreResults && mcf.getHelper().getUpdateCount(this) == -1){
                this.close();
            }

        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.getMoreResults", "655", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getMoreResults", "Exception");
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getMoreResults", "Exception");
            throw runtimeXIfNotClosed(nullX);
        } finally {
            // Restore initial closeOnCompletion value
            closeOnCompletion = deferCloseOnCompletion;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "getMoreResults",
                    hasMoreResults ? Boolean.TRUE : Boolean.FALSE);

        return hasMoreResults;
    }

    public int getQueryTimeout() throws SQLException {
        try {
            return stmtImpl.getQueryTimeout();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.getQueryTimeout", "675", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public ResultSet getResultSet() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "getResultSet");

        WSJdbcResultSet rsetWrapper; 

        try {
            // Choose the ResultSet wrapper based on Connection wrapper type.
            // This method can return a null Result Set, which we shouldn't wrap.

            ResultSet rsetImpl = stmtImpl.getResultSet();

            if (rsetImpl == null) {
                rsetWrapper = null;
            } else {
                //  - If the childWrapper is null, and the childWrappers is null or
                // empty, set the result set to childWrapper;
                // Otherwise, add the result set to childWrappers

                if (childWrapper == null && (childWrappers == null || childWrappers.isEmpty())) {
                    childWrapper = rsetWrapper = createResultSetWrapper(rsetImpl);
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Set the result set to child wrapper");
                } else {
                    if (childWrappers == null)
                        childWrappers = new ArrayList<Wrapper>(5); 
                    rsetWrapper = createResultSetWrapper(rsetImpl);
                    childWrappers.add(rsetWrapper);
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(this, tc,
                                 "Add the result set to child wrappers list."); 
                }
            }

        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.getResultSet", "717", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getResultSet", "Exception");
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getResultSet", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "getResultSet", rsetWrapper);
        return rsetWrapper;
    }

    public int getResultSetConcurrency() throws SQLException {
        try {
            return stmtImpl.getResultSetConcurrency();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.getResultSetConcurrency", "737", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public int getResultSetType() throws SQLException {
        try {
            return stmtImpl.getResultSetType();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.getResultSetType", "757", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    /**
     * @return the trace component for the WSJdbcStatement.
     */
    protected TraceComponent getTracer() 
    {
        return tc;
    }

    public int getUpdateCount() throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "getUpdateCount");

        int updateCount;

        try {
            updateCount = stmtImpl.getUpdateCount();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.getUpdateCount", "800", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getUpdateCount", "Exception");
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getUpdateCount", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "getUpdateCount", updateCount);
        return updateCount;
    }

    public SQLWarning getWarnings() throws SQLException {
        try {
            return stmtImpl.getWarnings();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.getWarnings", "820", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
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
    protected void introspectWrapperSpecificInfo(com.ibm.ws.rsadapter.FFDCLogger info) 
    {
        info.append("Underlying Statement: " + AdapterUtil.toString(stmtImpl), stmtImpl);
        info.append("Statement properties have changed? " + haveStatementPropertiesChanged);
        info.append("Poolability hint: " + (poolabilityHint ? "POOLABLE" : "NOT POOLABLE")); 
    }

    /**
     * Return the poolability hint for this statement, regardless of whether statement caching
     * is enabled, and regardless of whether the application server or underlying JDBC driver
     * are even capable of pooling this type of statement.
     * 
     * @return the poolability hint.
     * 
     * @throws SQLException if closed.
     * 
     */
    public final boolean isPoolable() throws SQLException {
        if (state == State.CLOSED)
            throw createClosedException("Statement");

        // We return the application server's hint.
        // If the underlying JDBC driver implements statement pooling and JDBC 4.0, it will
        // have the same value.  If the underlying JDBC driver doesn't implement (or if it
        // implements it incorrectly), then we don't want to ask it anyways.

        return poolabilityHint;
    }

    /**
     * @param runtimeX a RuntimeException which occurred, indicating the wrapper may be closed.
     * 
     * @throws SQLRecoverableException if the wrapper is closed and exception mapping is disabled. 
     * 
     * @return the RuntimeException to throw if it isn't.
     */
    final protected RuntimeException runtimeXIfNotClosed(RuntimeException runtimeX)
                    throws SQLException 
    {
        if (state == State.CLOSED)
            throw createClosedException("Statement"); 

        return runtimeX;
    }

    public void setCursorName(String name) throws SQLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setCursorName", name); 

        try {
            stmtImpl.setCursorName(name);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.setCursorName", "840", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        haveStatementPropertiesChanged = true;
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setEscapeProcessing", enable); 

        try {
            stmtImpl.setEscapeProcessing(enable);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.setEscapeProcessing", "860", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

    }

    public void setFetchDirection(int direction) throws SQLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setFetchDirection", AdapterUtil.getFetchDirectionString(direction)); 

        try {
            stmtImpl.setFetchDirection(direction);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.setFetchDirection", "880", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        haveStatementPropertiesChanged = true;
    }

    public final void setFetchSize(int rows) throws SQLException {
        if (state == State.CLOSED || rows < 0) {
            if (state == State.CLOSED)
                throw createClosedException("Statement"); 

            if (rows < 0)
                throw new SQLDataException( 
                AdapterUtil.getNLSMessage("NO_NEGATIVE_FETCH_SIZES"), "22003", null); 
        }

        // Set the requested fetchSize. This value will be enforced before executing the
        // statement. 

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "requested fetchSize --> " + rows); 
        requestedFetchSize = rows;
    }

    public void setMaxFieldSize(int max) throws SQLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setMaxFieldSize", max); 

        try {
            stmtImpl.setMaxFieldSize(max);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.setMaxFieldSize", "920", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        haveStatementPropertiesChanged = true;
    }

    public void setMaxRows(int max) throws SQLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setMaxRows", max); 

        try {
            stmtImpl.setMaxRows(max);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.setMaxRows", "940", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        haveStatementPropertiesChanged = true;
    }

    /**
     * See JDBC 4.0 JavaDoc API for details.
     * 
     */
    public void setPoolable(boolean isPoolable) throws SQLException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "setPoolable", isPoolable);

        if (state == State.CLOSED) {
            SQLException closedX = createClosedException("Statement");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "setPoolable", closedX);
            throw closedX;
        }

        try {
            // Configure the hint on the underlying JDBC driver. The JDBC 4.0 JavaDoc APIs
            // indicate the hint applies to both the JDBC driver and the application server.
            if (mcf.jdbcDriverSpecVersion >= 40)
                stmtImpl.setPoolable(isPoolable);
        } catch (SQLException sqlX) {
            FFDCFilter.processException(sqlX, getClass().getName() + ".setPoolable", "1733", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "setPoolable", sqlX);
            throw WSJdbcUtil.mapException(this, sqlX);
        }
        catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".setPoolable", "1708", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "setPoolable", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".setPoolable", "1715", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "setPoolable", err);
            throw err;
        }

        // Configure the value on the application server.  Poolability is just a hint,
        // so we allow either value to be set, regardless of whether the hint can be
        // followed.

        poolabilityHint = isPoolable;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "setPoolable");
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setQueryTimeout", seconds); 

        try {
            stmtImpl.setQueryTimeout(seconds);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.setQueryTimeout", "960", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        queryTimeoutSetByUser = true; 
        haveStatementPropertiesChanged = true;
    }

    /**
     * If the cursor holdability value is CLOSE_CURSORS_AT_COMMIT, then close all the ResultSet wrappers;
     * if the cursor holdability value is HOLD_CURSORS_OVER_COMMIT or 0, then don't close the ResultSet wrappers.
     */

    void closeResultSetsIfNecessary() { 
        if (holdability == ResultSet.CLOSE_CURSORS_AT_COMMIT) {
            closeChildWrappers();
        }
    }

    /**
     * <p>Moves to this Statement object's next result, deals with any current ResultSet
     * object(s) according to the instructions specified by the given flag, and returns
     * true if the next result is a ResultSet object. </p>
     * 
     * <p>There are no more results when the following is true: </p>
     * <pre> (!getMoreResults() && (getUpdateCount() == -1)</pre>
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
    public boolean getMoreResults(int current) throws SQLException { 

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "getMoreResults", AdapterUtil.getResultSetCloseString(current));

        boolean hasMoreResults = false;
        boolean deferCloseOnCompletion = closeOnCompletion; // Save closeOnCompletion value
        closeOnCompletion = false; // temporarily disable closeOnCompletion because there may
        // be a short time here where this Statement doesn't have reference to any more ResultSets

        try {
            switch (current) {
                case Statement.CLOSE_ALL_RESULTS:

                    // Close all the result sets
                    if (childWrapper != null) {
                        closeAndRemoveResultSet(true); 
                    }

                    if (childWrappers != null && !childWrappers.isEmpty()) {
                        closeAndRemoveResultSets(true); 
                    }
                    break;

                case Statement.CLOSE_CURRENT_RESULT:
                    // close the current result set. The current result set is always
                    // at the end of the childWrappers list. If childWrappers is empty,
                    // the current result set is childWrapper.

                    if (childWrappers == null || childWrappers.isEmpty()) {

                        // close and remove the current result.
                        if (childWrapper != null)
                            closeAndRemoveResultSet(true); 

                    } else {

                        // get the current result from childWrappers and close it
                        ((WSJdbcResultSet) childWrappers.get(childWrappers.size() - 1)).close(true); 
                    }
                    break;

                case Statement.KEEP_CURRENT_RESULT:
                    // do nothing in this case
                    break;
            }

            //  - move this call AFTER children are closed, as in WAS 5.1
            hasMoreResults = stmtImpl.getMoreResults(current);
            
            // If there are truly no more ResultSets for this Statement and closeOnCompletion
            // is enabled, close the Statement
            if(deferCloseOnCompletion && !hasMoreResults && (Statement.KEEP_CURRENT_RESULT != current) && mcf.getHelper().getUpdateCount(this) == -1){
                this.close();
            }

        } catch (SQLException ex) {
            FFDCFilter.processException(
                                        ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.getMoreResults(int)",
                                        "1280", this);

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getMoreResults", "Exception");
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getMoreResults", "Exception");
            throw runtimeXIfNotClosed(nullX);
        } finally {
            // Restore initial closeOnCompletion value
            closeOnCompletion = deferCloseOnCompletion;
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "getMoreResults", hasMoreResults ? Boolean.TRUE : Boolean.FALSE);

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
    public ResultSet getGeneratedKeys() throws SQLException { 

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "getGeneratedKeys");

        ResultSet rsetImpl = null;
        WSJdbcResultSet rsetWrapper = null;

        try {
            rsetImpl = stmtImpl.getGeneratedKeys();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.getGeneratedKeys", "1138", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getGeneratedKeys", "Exception");
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getGeneratedKeys", "Exception");

            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }

        if (childWrapper == null && (childWrappers == null || childWrappers.isEmpty())) {
            childWrapper = rsetWrapper = createResultSetWrapper(rsetImpl);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "Set the result set to child wrapper");
        } else {
            if (childWrappers == null)
                childWrappers = new ArrayList<Wrapper>(5); 
            rsetWrapper = createResultSetWrapper(rsetImpl);
            childWrappers.add(rsetWrapper);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc,
                         "Add the result set to child wrappers list."); 
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "getGeneratedKeys", rsetWrapper);

        return rsetWrapper;
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
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException { 

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "executeUpdate", sql, AdapterUtil.getAutoGeneratedKeyString(autoGeneratedKeys));

        int numUpdates;

        try {
            if (childWrapper != null) {
                closeAndRemoveResultSet();
            }

            if (childWrappers != null && !childWrappers.isEmpty()) {
                closeAndRemoveResultSets();
            }

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            numUpdates = stmtImpl.executeUpdate(sql, autoGeneratedKeys);

        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeUpdate", ex); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeUpdate", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "executeUpdate", numUpdates);
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
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException { 

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "executeUpdate", sql, Arrays.toString(columnIndexes));

        int numUpdates;

        try {
            if (childWrapper != null) {
                closeAndRemoveResultSet();
            }

            if (childWrappers != null && !childWrappers.isEmpty()) {
                closeAndRemoveResultSets();
            }

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            numUpdates = stmtImpl.executeUpdate(sql, columnIndexes);

        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeUpdate", ex); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeUpdate", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "executeUpdate", numUpdates);
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
    public int executeUpdate(String sql, String[] columnNames) throws SQLException { 

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "executeUpdate", sql, Arrays.toString(columnNames));

        int numUpdates;

        try {
            if (childWrapper != null) {
                closeAndRemoveResultSet();
            }

            if (childWrappers != null && !childWrappers.isEmpty()) {
                closeAndRemoveResultSets();
            }

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            numUpdates = stmtImpl.executeUpdate(sql, columnNames);

        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeUpdate", ex); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "executeUpdate", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "executeUpdate", numUpdates);
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
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException { 

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "execute", sql, AdapterUtil.getAutoGeneratedKeyString(autoGeneratedKeys));

        boolean result;

        try {
            if (childWrapper != null) {
                closeAndRemoveResultSet();
            }

            if (childWrappers != null && !childWrappers.isEmpty()) {
                closeAndRemoveResultSets();
            }

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            result = stmtImpl.execute(sql, autoGeneratedKeys);

        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "execute", ex); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "execute", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "execute", result ? "QUERY" : "UPDATE");
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
    public boolean execute(String sql, int[] columnIndexes) throws SQLException { 

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "execute", sql, Arrays.toString(columnIndexes));

        boolean result;

        try {
            if (childWrapper != null) {
                closeAndRemoveResultSet();
            }

            if (childWrappers != null && !childWrappers.isEmpty()) {
                closeAndRemoveResultSets();
            }

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            result = stmtImpl.execute(sql, columnIndexes);

        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "execute", ex); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "execute", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "execute", result ? "QUERY" : "UPDATE");
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
    public boolean execute(String sql, String[] columnNames) throws SQLException { 

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "execute", sql, Arrays.toString(columnNames));

        boolean result;

        try {
            if (childWrapper != null) {
                closeAndRemoveResultSet();
            }

            if (childWrappers != null && !childWrappers.isEmpty()) {
                closeAndRemoveResultSets();
            }

            parentWrapper.beginTransactionIfNecessary();

            enforceStatementProperties();

            result = stmtImpl.execute(sql, columnNames);

        } catch (SQLException ex) {
            // No FFDC code needed. Might be an application error. 
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "execute", ex); 
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "execute", "Exception");
            throw runtimeXIfNotClosed(nullX);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "execute", result ? "QUERY" : "UPDATE");
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
    public int getResultSetHoldability() throws SQLException { 
        try {
            return stmtImpl.getResultSetHoldability();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.getResultSetHoldability", "1770", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    public void closeOnCompletion() throws SQLException {
        if(mcf.beforeJDBCVersion(JDBCRuntimeVersion.VERSION_4_1))
            throw new SQLFeatureNotSupportedException();
        
        // If method is called on an already closed Statement
        if (isClosed()) {
            SQLException sqle = createClosedException("Statement");
            FFDCFilter.processException(sqle, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement41.closeOnCompletion", "45", this);
            throw sqle;
        }

        // If close on completion is already enabled, return
        if (closeOnCompletion)
            return;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "closeOnCompletion enabled");
        closeOnCompletion = true;
    }

    public boolean isCloseOnCompletion() throws SQLException {
        if(mcf.beforeJDBCVersion(JDBCRuntimeVersion.VERSION_4_1))
            throw new SQLFeatureNotSupportedException();
        
        // If method is called on an already closed Statement
        if (isClosed()) {
            SQLException sqle = createClosedException("Statement");
            FFDCFilter.processException(sqle, "com.ibm.ws.rsadapter.jdbc.WSJdbcStatement41.isCloseOnCompletion", "62", this);
            throw sqle;
        }

        return closeOnCompletion;
    }
    
    public long getCompatibleUpdateCount() throws SQLException {
        return stmtImpl.getUpdateCount();
    }
}