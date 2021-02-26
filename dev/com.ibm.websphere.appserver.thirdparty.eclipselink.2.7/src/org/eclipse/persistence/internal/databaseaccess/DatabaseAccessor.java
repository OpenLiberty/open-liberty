/*
 * Copyright (c) 1998, 2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 1998, 2018 IBM Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation from Oracle TopLink
//     Vikram Bhatia - bug fix for releasing temporary LOBs after conversion
//     02/08/2012-2.4 Guy Pelletier
//       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
//     07/13/2012-2.5 Guy Pelletier
//       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
//     08/24/2012-2.5 Guy Pelletier
//       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
//     11/05/2012-2.5 Guy Pelletier
//       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
//     01/08/2012-2.5 Guy Pelletier
//       - 389090: JPA 2.1 DDL Generation Support
//     02/19/2015 - Rick Curtis
//       - 458877 : Add national character support
package org.eclipse.persistence.internal.databaseaccess;

// javase imports
import static org.eclipse.persistence.internal.helper.DatabaseField.NULL_SQL_TYPE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.exceptions.QueryException;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.helper.LOBValueWriter;
import org.eclipse.persistence.internal.helper.NonSynchronizedVector;
import org.eclipse.persistence.internal.helper.ThreadCursoredList;
import org.eclipse.persistence.internal.localization.ExceptionLocalization;
import org.eclipse.persistence.internal.localization.ToStringLocalization;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.internal.sessions.ArrayRecord;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.mappings.structures.ObjectRelationalDataTypeDescriptor;
// EclipseLink imports
import org.eclipse.persistence.queries.Call;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.sessions.DatabaseRecord;
import org.eclipse.persistence.sessions.Login;
import org.eclipse.persistence.sessions.SessionProfiler;

/**
 * INTERNAL:
 *    DatabaseAccessor is private to EclipseLink. It encapsulates low level database operations (such as executing
 *    SQL and reading data by row). Database accessor defines a protocol by which EclipseLink may invoke these
 *    operations. <p>
 *    DatabaseAccessor also defines a single reference through which all configuration dependent behavior may
 *    be invoked. <p>
 *
 *    DabaseAccessor implements the following behavior. <ul>
 *    <li> Connect and disconnect from the database.
 *    <li> Execute SQL statements on the database, returning results.
 *    <li> Handle auto-commit and transactions.
 *    </ul>
 *    DatabaseAccessor dispatches the following protocols to its platform reference. <ul>
 *    <li> Provision of database platform specific type names.
 *    </ul>
 *    DatabaseAccessor dispatches the following protocols to the schema object. <ul>
 *    <li> Creation and deletion of schema objects.
 *    </ul>
 *    @see DatabasePlatform
 *    @since TOPLink/Java 1.0
 */
public class DatabaseAccessor extends DatasourceAccessor {

    /** PERF: Backdoor to disabling dynamic statements. Reverts to old prepared statement usage if set. */
    public static boolean shouldUseDynamicStatements = true;

    /** Stores statement handles for common used prepared statements. */
    protected Map<String, Statement> statementCache;

    /** Cache of the connection's java.sql.DatabaseMetaData */
    protected DatabaseMetaData metaData;

    /** This attribute will be used to store the currently active Batch Mechanism */
    protected BatchWritingMechanism activeBatchWritingMechanism;

    /**
     * These two attributes store the available BatchWritingMechanisms.  We sacrifice a little space to
     * prevent the work involved in recreating these objects each time a different type of SQL statement is
     * executed.  Depending on user behavior we may want to review this.
     */
    protected DynamicSQLBatchWritingMechanism dynamicSQLMechanism;
    protected ParameterizedSQLBatchWritingMechanism parameterizedMechanism;

    // Bug 2804663 - Each DatabaseAccessor holds on to its own LOBValueWriter instance
    protected LOBValueWriter lobWriter;

    /** PERF: Cache the statement object for dynamic SQL execution. */
    protected Statement dynamicStatement;
    protected boolean isDynamicStatementInUse;

    public DatabaseAccessor() {
        super();
        this.lobWriter = null;
        this.isDynamicStatementInUse = false;
    }

    /**
     * Create a database accessor with the given connection.
     */
    public DatabaseAccessor(Object connection) {
        this();
        this.datasourceConnection = connection;
    }

    /**
     * Lazy init the dynamic SQL mechanism.
     */
    protected DynamicSQLBatchWritingMechanism getDynamicSQLMechanism() {
        if (this.dynamicSQLMechanism == null) {
            this.dynamicSQLMechanism = new DynamicSQLBatchWritingMechanism(this);
        }
        return this.dynamicSQLMechanism;
    }

    /**
     * Lazy init the parameterized SQL mechanism.
     */
    protected ParameterizedSQLBatchWritingMechanism getParameterizedMechanism() {
        if (this.parameterizedMechanism == null) {
            this.parameterizedMechanism = new ParameterizedSQLBatchWritingMechanism(this);
        }
        return this.parameterizedMechanism;
    }

    /**
     * Execute any deferred select calls stored in the LOBValueWriter instance.
     * This method will typically be called by the CallQueryMechanism object.
     * Bug 2804663.
     *
     * @see org.eclipse.persistence.internal.helper.LOBValueWriter
     * @see org.eclipse.persistence.internal.queries.CallQueryMechanism#insertObject()
     */
    @Override
    public void flushSelectCalls(AbstractSession session) {
        if (lobWriter != null) {
            lobWriter.buildAndExecuteSelectCalls(session);
        }
    }

    /**
     * Return the LOBValueWriter instance.  Lazily initialize the instance.
     * Bug 2804663.
     *
     * @see org.eclipse.persistence.internal.helper.LOBValueWriter
     */
    public LOBValueWriter getLOBWriter() {
        if (lobWriter == null) {
            lobWriter = new LOBValueWriter(this);
        }
        return lobWriter;
    }

    /**
     * Allocate a statement for dynamic SQL execution.
     * Either return the cached dynamic statement, or a new statement.
     * This statement must be released after execution.
     */
    public synchronized Statement allocateDynamicStatement(Connection connection) throws SQLException {
        if (dynamicStatement == null) {
            dynamicStatement = connection.createStatement();
        }
        if (isDynamicStatementInUse()) {
            return connection.createStatement();
        }
        setIsDynamicStatementInUse(true);
        return dynamicStatement;
    }

    /**
     * Return the cached statement for dynamic SQL execution is in use.
     * Used to handle concurrency for the dynamic statement, this
     * method must only be called from within a synchronized method/block.
     */
    public boolean isDynamicStatementInUse() {
        return isDynamicStatementInUse;
    }

    /**
     * Set the platform.
     * This should be set to the session's platform, not the connections
     * which may not be configured correctly.
     */
    @Override
    public void setDatasourcePlatform(DatasourcePlatform platform) {
        super.setDatasourcePlatform(platform);
        // lobWriter may have been left from a different platform type.
        this.lobWriter = null;
    }

    /**
     * Set if the cached statement for dynamic SQL execution is in use.
     * Used to handle concurrency for the dynamic statement.
     */
    public synchronized void setIsDynamicStatementInUse(boolean isDynamicStatementInUse) {
        this.isDynamicStatementInUse = isDynamicStatementInUse;
    }

    /**
     * Begin a transaction on the database. This means toggling the auto-commit option.
     */
    @Override
    public void basicBeginTransaction(AbstractSession session) throws DatabaseException {
        try {
            if (getPlatform().supportsAutoCommit()) {
                getConnection().setAutoCommit(false);
            } else {
                getPlatform().beginTransaction(this);
            }
        } catch (SQLException exception) {
            DatabaseException commException = processExceptionForCommError(session, exception, null);
            if (commException != null) throw commException;
            throw DatabaseException.sqlException(exception, this, session, false);
        }
    }

    /**
     * If logging is turned on and the JDBC implementation supports meta data then display connection info.
     */
    @Override
    protected void buildConnectLog(AbstractSession session) {
        try {
            // Log connection information.
            if (session.shouldLog(SessionLog.CONFIG, SessionLog.CONNECTION)) {// Avoid printing if no logging required.
                DatabaseMetaData metaData = getConnectionMetaData();
                Object[] args = { metaData.getURL(), metaData.getUserName(), metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion(), metaData.getDriverName(), metaData.getDriverVersion(), Helper.cr() + "\t" };
                session.log(SessionLog.CONFIG, SessionLog.CONNECTION, "connected_user_database_driver", args, this);
            }
        } catch (Exception exception) {
            // Some databases do not support metadata, ignore exception.
            session.warning("JDBC_driver_does_not_support_meta_data", SessionLog.CONNECTION);
        }
    }

    /**
     * Build a row from the output parameters of a sp call.
     */
    public AbstractRecord buildOutputRow(CallableStatement statement, DatabaseCall call, AbstractSession session) throws DatabaseException {
        try {
            return call.buildOutputRow(statement, this, session);
        } catch (SQLException exception) {
            DatabaseException commException = processExceptionForCommError(session, exception, null);
            if (commException != null) throw commException;
            throw DatabaseException.sqlException(exception, this, session, false);
        }
    }

    /**
     * Return the field sorted in the correct order corresponding to the result set.
     * This is used for cursored selects where custom sql was provided.
     * If the fields passed in are null, this means that the field are not known and should be
     * built from the column names.  This case occurs for DataReadQuery's.
     */
    public Vector buildSortedFields(Vector fields, ResultSet resultSet, AbstractSession session) throws DatabaseException {
        Vector sortedFields;
        try {
            Vector columnNames = getColumnNames(resultSet, session);
            if (fields == null) {// Means fields not known.
                sortedFields = columnNames;
            } else {
                sortedFields = sortFields(fields, columnNames);
            }
        } catch (SQLException exception) {
            DatabaseException commException = processExceptionForCommError(session, exception, null);
            if (commException != null) throw commException;
            throw DatabaseException.sqlException(exception, this, session, false);
        }
        return sortedFields;
    }

    /**
     * Connect to the database.
     * Exceptions are caught and re-thrown as EclipseLink exceptions.
     * Must set the transaction isolation.
     */
    @Override
    protected void connectInternal(Login login, AbstractSession session) throws DatabaseException {
        super.connectInternal(login, session);
        checkTransactionIsolation(session);
        try {
            session.getPlatform().initializeConnectionData(getConnection());
        } catch (java.sql.SQLException sqlEx) {
            DatabaseException commException = processExceptionForCommError(session, sqlEx, null);
            if (commException != null) throw commException;
            throw DatabaseException.sqlException(sqlEx, this, session, false);
        }
    }

    /**
     * Check to see if the transaction isolation needs to
     * be set for the newly created connection. This must
     * be done outside of a transaction.
     * Exceptions are caught and re-thrown as EclipseLink exceptions.
     */
    protected void checkTransactionIsolation(AbstractSession session) throws DatabaseException {
        if ((!this.isInTransaction) && (this.login != null) && (((DatabaseLogin)this.login).getTransactionIsolation() != -1)) {
            try {
                getConnection().setTransactionIsolation(((DatabaseLogin)this.login).getTransactionIsolation());
            } catch (java.sql.SQLException sqlEx) {
                DatabaseException commException = processExceptionForCommError(session, sqlEx, null);
                if (commException != null) throw commException;
                throw DatabaseException.sqlException(sqlEx, this, session, false);
            }
        }
    }

    /**
     * Flush the statement cache.
     * Each statement must first be closed.
     */
    public void clearStatementCache(AbstractSession session) {
        if (hasStatementCache()) {
            for (Statement statement : getStatementCache().values()) {
                try {
                    statement.close();
                } catch (SQLException exception) {
                    // an exception can be raised if
                    // a statement is closed twice.
                }
            }
            this.statementCache = null;
        }

        // Close cached dynamic statement.
        if (this.dynamicStatement != null) {
            try {
                this.dynamicStatement.close();
            } catch (SQLException exception) {
                // an exception can be raised if
                // a statement is closed twice.
            }
            this.dynamicStatement = null;
            this.setIsDynamicStatementInUse(false);
        }
    }

    /**
     * Clone the accessor.
     */
    @Override
    public Object clone() {
        DatabaseAccessor accessor = (DatabaseAccessor)super.clone();
        accessor.dynamicSQLMechanism = null;
        if (this.activeBatchWritingMechanism != null) {
            accessor.activeBatchWritingMechanism = this.activeBatchWritingMechanism.clone();
        }
        accessor.parameterizedMechanism = null;
        accessor.statementCache = null;
        return accessor;
    }

    /**
     * Close the result set of the cursored stream.
     */
    public void closeCursor(ResultSet resultSet, AbstractSession session) throws DatabaseException {
        try {
            resultSet.close();
        } catch (SQLException exception) {
            DatabaseException commException = processExceptionForCommError(session, exception, null);
            if (commException != null) throw commException;
            throw DatabaseException.sqlException(exception, this, session, false);
        }
    }

    /**
     * INTERNAL:
     * Closes a PreparedStatement (which is supposed to close it's current resultSet).
     * Factored out to simplify coding and handle exceptions.
     */
    public void closeStatement(Statement statement, AbstractSession session, DatabaseCall call) throws SQLException {
        if (statement == null) {
            decrementCallCount();
            return;
        }

        DatabaseQuery query = ((call == null)? null : call.getQuery());
        try {
            session.startOperationProfile(SessionProfiler.StatementExecute, query, SessionProfiler.ALL);
            statement.close();
        } finally {
            session.endOperationProfile(SessionProfiler.StatementExecute, query, SessionProfiler.ALL);
            decrementCallCount();
            // If this is the cached dynamic statement, release it.
            if (statement == this.dynamicStatement) {
                this.dynamicStatement = null;
                // The dynamic statement is cached and only closed on disconnect.
                setIsDynamicStatementInUse(false);
            }
        }
    }

    /**
     *    Commit a transaction on the database. First flush any batched statements.
     */
    @Override
    public void commitTransaction(AbstractSession session) throws DatabaseException {
        this.writesCompleted(session);
        super.commitTransaction(session);
    }

    /**
     * Commit a transaction on the database. This means toggling the auto-commit option.
     */
    @Override
    public void basicCommitTransaction(AbstractSession session) throws DatabaseException {
        try {
            if (getPlatform().supportsAutoCommit()) {
                getConnection().commit();
                getConnection().setAutoCommit(true);
            } else {
                getPlatform().commitTransaction(this);
            }
        } catch (SQLException exception) {
            DatabaseException commException = processExceptionForCommError(session, exception, null);
            if (commException != null) throw commException;
            throw DatabaseException.sqlException(exception, this, session, false);
        }
    }

    /**
     * Advance the result set and return a Record populated
     * with values from the next valid row in the result set. Intended solely
     * for cursored stream support.
     */
    public AbstractRecord cursorRetrieveNextRow(Vector fields, ResultSet resultSet, AbstractSession session) throws DatabaseException {
        try {
            if (resultSet.next()) {
                return fetchRow(fields, resultSet, resultSet.getMetaData(), session);
            } else {
                return null;
            }
        } catch (SQLException exception) {
            DatabaseException commException = processExceptionForCommError(session, exception, null);
            if (commException != null) throw commException;
            throw DatabaseException.sqlException(exception, this, session, false);
        }
    }

    /**
     * Advance the result set and return a DatabaseRow populated
     * with values from the next valid row in the result set. Intended solely
     * for scrollable cursor support.
     */
    public AbstractRecord cursorRetrievePreviousRow(Vector fields, ResultSet resultSet, AbstractSession session) throws DatabaseException {
        try {
            if (resultSet.previous()) {
                return fetchRow(fields, resultSet, resultSet.getMetaData(), session);
            } else {
                return null;
            }
        } catch (SQLException exception) {
            DatabaseException commException = processExceptionForCommError(session, exception, null);
            if (commException != null) throw commException;
            throw DatabaseException.sqlException(exception, this, session, false);
        }
    }

    /**
     * Close the connection.
     */
    @Override
    public void closeDatasourceConnection() throws DatabaseException {
        try {
            getConnection().close();
        } catch (SQLException exception) {
            throw DatabaseException.sqlException(exception, this, null, false);
        }
    }

    /**
     * Disconnect from the datasource.
     * Added for bug 3046465 to ensure the statement cache is cleared.
     */
    @Override
    public void disconnect(AbstractSession session) throws DatabaseException {
        clearStatementCache(session);
        super.disconnect(session);
    }

    /**
     * Close the accessor's connection.
     * This is used only for external connection pooling
     * when it is intended for the connection to be reconnected in the future.
     */
    @Override
    public void closeConnection() {
        // Unfortunately do not have the session to pass, fortunately it is not used.
        clearStatementCache(null);
        super.closeConnection();
    }

    /**
     * Execute the EclipseLink dynamically batched/concatenated statement.
     */
    protected void executeBatchedStatement(PreparedStatement statement, AbstractSession session) throws DatabaseException {
        try {
            executeDirectNoSelect(statement, null, session);
        } catch (RuntimeException exception) {
            try {// Ensure that the statement is closed, but still ensure that the real exception is thrown.
                closeStatement(statement, session, null);
            } catch (SQLException closeException) {
            }

            throw exception;
        }

        // This is in a separate try block to ensure that the real exception is not masked by the close exception.
        try {
            closeStatement(statement, session, null);
        } catch (SQLException exception) {
            //With an external connection pool the connection may be null after this call, if it is we will
            //be unable to determine if it is a connection based exception so treat it as if it wasn't.
            DatabaseException commException = processExceptionForCommError(session, exception, null);
            if (commException != null) throw commException;
            throw DatabaseException.sqlException(exception, this, session, false);
        }
    }

    /**
     * Execute the call.
     * The execution can differ slightly depending on the type of call.
     * The call may be parameterized where the arguments are in the translation row.
     * The row will be empty if there are no parameters.
     * @return depending of the type either the row count, row or vector of rows.
     */
    @Override
    public Object executeCall(Call call, AbstractRecord translationRow, AbstractSession session) throws DatabaseException {
        // Keep complete implementation.
        return basicExecuteCall(call, translationRow, session, true);
    }

    /**
     * Execute the call.
     * The execution can differ slightly depending on the type of call.
     * The call may be parameterized where the arguments are in the translation row.
     * The row will be empty if there are no parameters.
     * @return depending of the type either the row count, row or vector of rows.
     */
    @Override
    public Object basicExecuteCall(Call call, AbstractRecord translationRow, AbstractSession session) throws DatabaseException {
        return basicExecuteCall(call, translationRow, session, true);
    }

    /**
     * Execute the call.
     * The execution can differ slightly depending on the type of call.
     * The call may be parameterized where the arguments are in the translation row.
     * The row will be empty if there are no parameters.
     * @return depending of the type either the row count, row or vector of rows.
     */
    public Object basicExecuteCall(Call call, AbstractRecord translationRow, AbstractSession session, boolean batch) throws DatabaseException {
        Statement statement = null;
        Object result = null;
        DatabaseCall dbCall = null;
        ResultSet resultSet = null;// only used if this is a read query
        try {
            dbCall = (DatabaseCall)call;
        } catch (ClassCastException e) {
            throw QueryException.invalidDatabaseCall(call);
        }

        // If the login is null, then this accessor has never been connected.
        if (this.login == null) {
            throw DatabaseException.databaseAccessorNotConnected();
        }

        if (batch && isInBatchWritingMode(session)) {
            // if there is nothing returned and we are not using optimistic locking then batch
            //if it is a StoredProcedure with in/out or out parameters then do not batch
            //logic may be weird but we must not batch if we are not using JDBC batchwriting and we have parameters
            // we may want to refactor this some day
            if (dbCall.isBatchExecutionSupported()) {
                // this will handle executing batched statements, or switching mechanisms if required
                getActiveBatchWritingMechanism(session).appendCall(session, dbCall);
                //bug 4241441: passing 1 back to avoid optimistic lock exceptions since there
                // is no way to know if it succeeded on the DB at this point.
                return Integer.valueOf(1);
            } else {
                getActiveBatchWritingMechanism(session).executeBatchedStatements(session);
            }
        }

        try {
            incrementCallCount(session);
            if (session.shouldLog(SessionLog.FINE, SessionLog.SQL)) {// Avoid printing if no logging required.
                session.log(SessionLog.FINE, SessionLog.SQL, dbCall.getLogString(this), (Object[])null, this, false);
            }
            session.startOperationProfile(SessionProfiler.SqlPrepare, dbCall.getQuery(), SessionProfiler.ALL);
            try {
                statement = dbCall.prepareStatement(this, translationRow, session);
            } finally {
                session.endOperationProfile(SessionProfiler.SqlPrepare, dbCall.getQuery(), SessionProfiler.ALL);
            }

            // effectively this means that someone is executing an update type query.
            if (dbCall.isExecuteUpdate()) {
                dbCall.setExecuteReturnValue(execute(dbCall, statement, session));
                dbCall.setStatement(statement);
                this.possibleFailure = false;
                return dbCall;
            } else if (dbCall.isNothingReturned()) {
                result = executeNoSelect(dbCall, statement, session);
                this.writeStatementsCount++;
                if (dbCall.isLOBLocatorNeeded()) {
                    // add original (insert or update) call to the LOB locator
                    // Bug 2804663 - LOBValueWriter is no longer a singleton
                    getLOBWriter().addCall(dbCall);
                }
            } else if ((!dbCall.getReturnsResultSet() || (dbCall.getReturnsResultSet() && dbCall.shouldBuildOutputRow()))) {
                result = session.getPlatform().executeStoredProcedure(dbCall, (PreparedStatement)statement, this, session);
                this.storedProcedureStatementsCount++;
            } else {
                resultSet = executeSelect(dbCall, statement, session);
                this.readStatementsCount++;
                if (!dbCall.shouldIgnoreFirstRowSetting() && dbCall.getFirstResult() != 0) {
                    resultSet.absolute(dbCall.getFirstResult());
                }
                dbCall.matchFieldOrder(resultSet, this, session);

                if (dbCall.isCursorReturned()) {
                    dbCall.setStatement(statement);
                    dbCall.setResult(resultSet);
                    this.possibleFailure = false;
                    return dbCall;
                }
                result = processResultSet(resultSet, dbCall, statement, session);
            }
            if (result instanceof ThreadCursoredList) {
                this.possibleFailure = false;
                return result;
            }
            // Log any warnings on finest.
            if (session.shouldLog(SessionLog.FINEST, SessionLog.SQL)) {// Avoid printing if no logging required.
                SQLWarning warning = statement.getWarnings();
                while (warning != null) {
                    String message = warning.getMessage() + ":" + warning.getSQLState() + " - " + warning.getCause();
                    // 325605: This log will not be tracked by QuerySQLTracker
                    session.log(SessionLog.FINEST, SessionLog.SQL, message, (Object[])null, this, false);
                    warning = warning.getNextWarning();
                }
            }
        } catch (SQLException exception) {
            //If this is a connection from an external pool then closeStatement will close the connection.
            //we must test the connection before that happens.
            RuntimeException exceptionToThrow = processExceptionForCommError(session, exception, dbCall);

            try {// Ensure that the statement is closed, but still ensure that the real exception is thrown.
                closeStatement(statement, session, dbCall);
            } catch (Exception closeException) {
            }
            if (exceptionToThrow == null){
                //not a comm failure :
                throw DatabaseException.sqlException(exception, dbCall, this, session, false);
            }
            throw exceptionToThrow;

        } catch (RuntimeException exception) {
            try {// Ensure that the statement is closed, but still ensure that the real exception is thrown.
                closeStatement(statement, session, dbCall);
            } catch (Exception closeException) {
            }
            if (exception instanceof DatabaseException) {
                ((DatabaseException)exception).setCall(dbCall);
                if(((DatabaseException)exception).getAccessor() == null) {
                    ((DatabaseException)exception).setAccessor(this);
                }
            }
            throw exception;
        }

        // This is in a separate try block to ensure that the real exception is not masked by the close exception.
        try {
            // Allow for caching of statement, forced closes are not cache as they failed execution so are most likely bad.
            releaseStatement(statement, dbCall.getSQLString(), dbCall, session);
        } catch (SQLException exception) {
            //With an external connection pool the connection may be null after this call, if it is we will
            //be unable to determine if it is a connection based exception so treat it as if it wasn't.
            DatabaseException commException = processExceptionForCommError(session, exception, null);
            if (commException != null) {
                throw commException;
            }
            throw DatabaseException.sqlException(exception, this, session, false);
        }

        this.possibleFailure = false;
        return result;
    }

    /**
     * Fetch all the rows from the result set.
     */
    public Object processResultSet(ResultSet resultSet, DatabaseCall call, Statement statement, AbstractSession session) throws SQLException {
        Object result = null;
        ResultSetMetaData metaData = resultSet.getMetaData();

        // If there are no columns (and only an update count) throw an exception.
        if (metaData.getColumnCount() == 0 && statement.getUpdateCount() > -1) {
            resultSet.close();
            throw new IllegalStateException(ExceptionLocalization.buildMessage("jpa21_invalid_call_with_no_result_sets_returned"));
        }

        session.startOperationProfile(SessionProfiler.RowFetch, call.getQuery(), SessionProfiler.ALL);
        try {
            if (call.isOneRowReturned()) {
                if (resultSet.next()) {
                    if (call.isLOBLocatorNeeded()) {
                        //if Oracle BLOB/CLOB field is being written, and the thin driver is used, the driver 4k
                        //limit bug prevent the call from directly writing to the table if the LOB value size exceeds 4k.
                        //Instead, a LOB locator is retrieved and value is then piped into the table through the locator.
                        // Bug 2804663 - LOBValueWriter is no longer a singleton
                        getLOBWriter().fetchLocatorAndWriteValue(call, resultSet);
                    } else {
                        result = fetchRow(call.getFields(), call.getFieldsArray(), resultSet, metaData, session);
                    }
                    if (resultSet.next()) {
                        // Raise more rows event, some apps may interpret as error or warning.
                        if (session.hasEventManager()) {
                            session.getEventManager().moreRowsDetected(call);
                        }
                    }
                }
            } else {
                boolean hasMultipleResultsSets = call.hasMultipleResultSets();
                Vector results = null;
                boolean hasMoreResultsSets = true;
                while (hasMoreResultsSets) {
                    boolean hasNext = resultSet.next();
                    // PERF: Optimize out simple empty case.
                    if (hasNext) {
                        if (session.isConcurrent()) {
                            // If using threading return the cursored list,
                            // do not close the result or statement as the rows are being fetched by the thread.
                            return buildThreadCursoredResult(call, resultSet, statement, metaData, session);
                        } else {
                            results = new Vector(16);
                            while (hasNext) {
                                results.add(fetchRow(call.getFields(), call.getFieldsArray(), resultSet, metaData, session));
                                hasNext = resultSet.next();
                            }
                        }
                    } else {
                        results = new Vector(0);
                    }
                    if (result == null) {
                        if (call.returnMultipleResultSetCollections()) {
                            result = new Vector();
                            ((List) result).add(results);
                        } else {
                            result = results;
                        }
                    } else {
                        if (call.returnMultipleResultSetCollections()) {
                            ((List)result).add(results);
                        } else {
                            ((List)result).addAll(results);
                        }
                    }
                    if (hasMultipleResultsSets) {
                        hasMoreResultsSets = statement.getMoreResults();
                        if (hasMoreResultsSets) {
                            resultSet = statement.getResultSet();
                            metaData = resultSet.getMetaData();
                            call.setFields(null);
                            call.matchFieldOrder(resultSet, this, session);
                        }
                    } else {
                        hasMoreResultsSets = false;
                    }
                }
            }
            resultSet.close();// This must be closed in case the statement is cached and not closed.
        } finally {
            session.endOperationProfile(SessionProfiler.RowFetch, call.getQuery(), SessionProfiler.ALL);
        }
        return result;
    }

    /**
     * This allows for the rows to be fetched concurrently to the objects being built.
     * This code is not currently publicly supported.
     */
    protected Vector buildThreadCursoredResult(final DatabaseCall dbCall, final ResultSet resultSet, final Statement statement, final ResultSetMetaData metaData, final AbstractSession session) {
        final ThreadCursoredList results = new ThreadCursoredList(20);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    session.startOperationProfile(SessionProfiler.RowFetch, dbCall.getQuery(), SessionProfiler.ALL);
                    try {
                        // Initial next was already validated before this method is called.
                        boolean hasNext = true;
                        while (hasNext) {
                            results.add(fetchRow(dbCall.getFields(), dbCall.getFieldsArray(), resultSet, metaData, session));
                            hasNext = resultSet.next();
                        }
                        resultSet.close();// This must be closed in case the statement is cached and not closed.
                    } catch (SQLException exception) {
                        //If this is a connection from an external pool then closeStatement will close the connection.
                        //we must test the connection before that happens.
                        RuntimeException exceptionToThrow = processExceptionForCommError(session, exception, dbCall);
                        try {// Ensure that the statement is closed, but still ensure that the real exception is thrown.
                            closeStatement(statement, session, dbCall);
                        } catch (Exception closeException) {
                        }
                        if (exceptionToThrow == null){
                            results.throwException(DatabaseException.sqlException(exception, dbCall, DatabaseAccessor.this, session, false));
                        }
                        results.throwException(exceptionToThrow);
                    } catch (RuntimeException exception) {
                        try {// Ensure that the statement is closed, but still ensure that the real exception is thrown.
                            closeStatement(statement, session, dbCall);
                        } catch (Exception closeException) {
                        }
                        if (exception instanceof DatabaseException) {
                            ((DatabaseException)exception).setCall(dbCall);
                        }
                        results.throwException(exception);
                    } finally {
                        session.endOperationProfile(SessionProfiler.RowFetch, dbCall.getQuery(), SessionProfiler.ALL);
                    }

                    // This is in a separate try block to ensure that the real exception is not masked by the close exception.
                    try {
                        // Allow for caching of statement, forced closes are not cache as they failed execution so are most likely bad.
                        DatabaseAccessor.this.releaseStatement(statement, dbCall.getSQLString(), dbCall, session);
                    } catch (SQLException exception) {
                        //With an external connection pool the connection may be null after this call, if it is we will
                        //be unable to determine if it is a connection based exception so treat it as if it wasn't.
                        DatabaseException commException = processExceptionForCommError(session, exception, dbCall);
                        if (commException != null) results.throwException(commException);
                        results.throwException(DatabaseException.sqlException(exception, DatabaseAccessor.this, session, false));
                    }
                } finally {
                    results.setIsComplete(true);
                    session.releaseReadConnection(DatabaseAccessor.this);
                }
            }
        };
        dbCall.returnCursor();
        session.getServerPlatform().launchContainerRunnable(runnable);

        return results;
    }

    /**
     * Execute the statement.
     */
    public Integer executeDirectNoSelect(Statement statement, DatabaseCall call, AbstractSession session) throws DatabaseException {
        int rowCount = 0;

        try {
            if (call != null) {
                session.startOperationProfile(SessionProfiler.StatementExecute, call.getQuery(), SessionProfiler.ALL);
            } else {
                session.startOperationProfile(SessionProfiler.StatementExecute, null, SessionProfiler.ALL);
            }
            if ((call != null) && call.isDynamicCall(session)) {
                rowCount = statement.executeUpdate(call.getSQLString());
            } else {
                rowCount = ((PreparedStatement)statement).executeUpdate();
            }

            if ((!getPlatform().supportsAutoCommit()) && (!this.isInTransaction)) {
                getPlatform().autoCommit(this);
            }
        } catch (SQLException exception) {
            if (!getPlatform().shouldIgnoreException(exception)) {
                DatabaseException commException = processExceptionForCommError(session, exception, call);
                if (commException != null) throw commException;
                throw DatabaseException.sqlException(exception, this, session, false);
            }
        } finally {
            if (call != null) {
                session.endOperationProfile(SessionProfiler.StatementExecute, call.getQuery(), SessionProfiler.ALL);
            } else {
                session.endOperationProfile(SessionProfiler.StatementExecute, null, SessionProfiler.ALL);
            }
        }

        return Integer.valueOf(rowCount);
    }

    /**
     * Execute the batched statement through the JDBC2 API.
     */
    protected int executeJDK12BatchStatement(Statement statement, DatabaseCall dbCall, AbstractSession session, boolean isStatementPrepared) throws DatabaseException {
        int returnValue =0;
        try {
            //bug 4241441: executeBatch moved to the platform, and result returned to batch mechanism
            returnValue = this.getPlatform().executeBatch(statement, isStatementPrepared);
        } catch (SQLException exception) {
            //If this is a connection from an external pool then closeStatement will close the connection.
            //we must test the connection before that happens.
            DatabaseException commException = processExceptionForCommError(session, exception, dbCall);
            if (commException != null) throw commException;
            try {// Ensure that the statement is closed, but still ensure that the real exception is thrown.
                closeStatement(statement, session, dbCall);
            } catch (SQLException closeException) {
            }

            throw DatabaseException.sqlException(exception, this, session, false);
        } catch (RuntimeException exception) {
            try {// Ensure that the statement is closed, but still ensure that the real exception is thrown.
                closeStatement(statement, session, dbCall);
            } catch (SQLException closeException) {
            }

            throw exception;
        }

        // This is in a separate try block to ensure that the real exception is not masked by the close exception.
        try {
            // if we are called from the ParameterizedBatchWritingMechanism then dbCall will not be null
            //and we should try an release the statement
            if (dbCall != null) {
                releaseStatement(statement, dbCall.getSQLString(), dbCall, session);
            } else {
                closeStatement(statement, session, null);
            }
        } catch (SQLException exception) {
            DatabaseException commException = processExceptionForCommError(session, exception, dbCall);
            if (commException != null) throw commException;
            throw DatabaseException.sqlException(exception, this, session, false);
        }
        return returnValue;
    }

    /**
     * Execute the statement.
     */
    protected Integer executeNoSelect(DatabaseCall call, Statement statement, AbstractSession session) throws DatabaseException {
        Integer rowCount = executeDirectNoSelect(statement, call, session);

        // Allow for procs with outputs to be raised as events for error handling.
        if (call.shouldBuildOutputRow()) {
            AbstractRecord outputRow = buildOutputRow((CallableStatement)statement, call, session);
            call.getQuery().setProperty("output", outputRow);
            if (session.hasEventManager()) {
                session.getEventManager().outputParametersDetected(outputRow, call);
            }
        }

        return rowCount;
    }

    /**
     * Execute the statement.
     */
    public boolean execute(DatabaseCall call, Statement statement, AbstractSession session) throws SQLException {
        boolean result;

        session.startOperationProfile(SessionProfiler.StatementExecute, call.getQuery(), SessionProfiler.ALL);
        try {
            if (call.isDynamicCall(session)) {
                result = statement.execute(call.getSQLString());
            } else {
                result = ((PreparedStatement)statement).execute();
            }
        } finally {
            session.endOperationProfile(SessionProfiler.StatementExecute, call.getQuery(), SessionProfiler.ALL);
        }

        return result;
    }

    /**
     * Execute the statement.
     */
    public ResultSet executeSelect(DatabaseCall call, Statement statement, AbstractSession session) throws SQLException {
        ResultSet resultSet;

        session.startOperationProfile(SessionProfiler.StatementExecute, call.getQuery(), SessionProfiler.ALL);
        try {
            if (call.isDynamicCall(session)) {
                resultSet = statement.executeQuery(call.getSQLString());
            } else {
                resultSet = ((PreparedStatement)statement).executeQuery();
            }
        } finally {
            session.endOperationProfile(SessionProfiler.StatementExecute, call.getQuery(), SessionProfiler.ALL);
        }

        // Allow for procs with outputs to be raised as events for error handling.
        if (call.shouldBuildOutputRow() && getPlatform().isOutputAllowWithResultSet()) {
            AbstractRecord outputRow = buildOutputRow((CallableStatement)statement, call, session);
            call.getQuery().setProperty("output", outputRow);
            if (session.hasEventManager()) {
                session.getEventManager().outputParametersDetected(outputRow, call);
            }
        }

        return resultSet;
    }

    /**
     * Return a new DatabaseRow.<p>
     * Populate the row from the data in cursor. The fields representing the results
     * and the order of the results are stored in fields.
     * <p><b>NOTE</b>:
     * Make sure that the field name is set.  An empty field name placeholder is
     * used in the sortFields() method when the number of fields defined does not
     * match the number of column names available on the database.
     * PERF: This method must be highly optimized.
     */
    protected AbstractRecord fetchRow(Vector fields, ResultSet resultSet, ResultSetMetaData metaData, AbstractSession session) throws DatabaseException {
        int size = fields.size();
        Vector values = NonSynchronizedVector.newInstance(size);
        // PERF: Pass platform and optimize data flag.
        DatabasePlatform platform = getPlatform();
        boolean optimizeData = platform.shouldOptimizeDataConversion();
        for (int index = 0; index < size; index++) {
            DatabaseField field = (DatabaseField)fields.elementAt(index);
            // Field can be null for fetch groups.
            if (field != null) {
                values.add(getObject(resultSet, field, metaData, index + 1, platform, optimizeData, session));
            } else {
                values.add(null);
            }
        }

        // Row creation is optimized through sharing the same fields for the entire result set.
        return new DatabaseRecord(fields, values);
    }

    /**
     * Return a new DatabaseRow.<p>
     * Populate the row from the data in cursor. The fields representing the results
     * and the order of the results are stored in fields.
     * <p><b>NOTE</b>:
     * Make sure that the field name is set.  An empty field name placeholder is
     * used in the sortFields() method when the number of fields defined does not
     * match the number of column names available on the database.
     * PERF: This method must be highly optimized.
     */
    public AbstractRecord fetchRow(Vector fields, DatabaseField[] fieldsArray, ResultSet resultSet, ResultSetMetaData metaData, AbstractSession session) throws DatabaseException {
        int size = fieldsArray.length;
        Object[] values = new Object[size];
        // PERF: Pass platform and optimize data flag.
        DatabasePlatform platform = getPlatform();
        boolean optimizeData = platform.shouldOptimizeDataConversion();
        for (int index = 0; index < size; index++) {
            DatabaseField field = fieldsArray[index];
            // Field can be null for fetch groups.
            if (field != null) {
                values[index] = getObject(resultSet, field, metaData, index + 1, platform, optimizeData, session);
            } else {
                values[index] = null;
            }
        }

        // Row creation is optimized through sharing the same fields for the entire result set.
        return new ArrayRecord(fields, fieldsArray, values);
    }
    public void populateRow(DatabaseField[] fieldsArray, Object[] values, ResultSet resultSet, ResultSetMetaData metaData, AbstractSession session, int startIndex, int endIndex) throws DatabaseException {
        // PERF: Pass platform and optimize data flag.
        DatabasePlatform platform = getPlatform();
        boolean optimizeData = platform.shouldOptimizeDataConversion();
        for (int index = startIndex; index < endIndex; index++) {
            DatabaseField field = fieldsArray[index];
            // Field can be null for fetch groups.
            if (field != null) {
                values[index] = getObject(resultSet, field, metaData, index + 1, platform, optimizeData, session);
            }
        }
    }

    /**
     * INTERNAL:
     * This method is used internally to return the active batch writing mechanism to batch the statement
     */
    public BatchWritingMechanism getActiveBatchWritingMechanism(AbstractSession session) {
        if (this.activeBatchWritingMechanism == null) {
            // If the platform defines a custom mechanism, then use it.
            if (((DatabasePlatform)this.platform).getBatchWritingMechanism() != null) {
                this.activeBatchWritingMechanism = ((DatabasePlatform)this.platform).getBatchWritingMechanism().clone();
                this.activeBatchWritingMechanism.setAccessor(this, session);
            } else {
                this.activeBatchWritingMechanism = getParameterizedMechanism();
            }
        }
        return this.activeBatchWritingMechanism;
    }

    /**
     * Get a description of table columns available in a catalog.
     *
     * <P>Only column descriptions matching the catalog, schema, table
     * and column name criteria are returned.  They are ordered by
     * TABLE_SCHEM, TABLE_NAME and ORDINAL_POSITION.
     *
     * <P>Each column description has the following columns:
     *  <OL>
     *    <LI><B>TABLE_CAT</B> String => table catalog (may be null)
     *    <LI><B>TABLE_SCHEM</B> String => table schema (may be null)
     *    <LI><B>TABLE_NAME</B> String => table name
     *    <LI><B>COLUMN_NAME</B> String => column name
     *    <LI><B>DATA_TYPE</B> short => SQL type from java.sql.Types
     *    <LI><B>TYPE_NAME</B> String => Data source dependent type name
     *    <LI><B>COLUMN_SIZE</B> int => column size.  For char or date
     *        types this is the maximum number of characters, for numeric or
     *        decimal types this is precision.
     *    <LI><B>BUFFER_LENGTH</B> is not used.
     *    <LI><B>DECIMAL_DIGITS</B> int => the number of fractional digits
     *    <LI><B>NUM_PREC_RADIX</B> int => Radix (typically either 10 or 2)
     *    <LI><B>NULLABLE</B> int => is NULL allowed?
     *      <UL>
     *      <LI> columnNoNulls - might not allow NULL values
     *      <LI> columnNullable - definitely allows NULL values
     *      <LI> columnNullableUnknown - nullability unknown
     *      </UL>
     *    <LI><B>REMARKS</B> String => comment describing column (may be null)
     *     <LI><B>COLUMN_DEF</B> String => default value (may be null)
     *    <LI><B>SQL_DATA_TYPE</B> int => unused
     *    <LI><B>SQL_DATETIME_SUB</B> int => unused
     *    <LI><B>CHAR_OCTET_LENGTH</B> int => for char types the
     *       maximum number of bytes in the column
     *    <LI><B>ORDINAL_POSITION</B> int    => index of column in table
     *      (starting at 1)
     *    <LI><B>IS_NULLABLE</B> String => "NO" means column definitely
     *      does not allow NULL values; "YES" means the column might
     *      allow NULL values.  An empty string means nobody knows.
     *  </OL>
     *
     * @param catalog a catalog name; "" retrieves those without a
     * catalog; null means drop catalog name from the selection criteria
     * @param schemaPattern a schema name pattern; "" retrieves those
     * without a schema
     * @param tableNamePattern a table name pattern
     * @param columnNamePattern a column name pattern
     * @return a Vector of DatabaseRows.
     */
    @Override
    public Vector getColumnInfo(String catalog, String schema, String tableName, String columnName, AbstractSession session) throws DatabaseException {
        if (session.shouldLog(SessionLog.FINEST, SessionLog.QUERY)) {// Avoid printing if no logging required.
            Object[] args = { catalog, schema, tableName, columnName };
            session.log(SessionLog.FINEST, SessionLog.QUERY, "query_column_meta_data_with_column", args, this);
        }
        Vector result = new Vector();
        ResultSet resultSet = null;
        try {
            incrementCallCount(session);
            resultSet = getConnectionMetaData().getColumns(catalog, schema, tableName, columnName);
            Vector fields = buildSortedFields(null, resultSet, session);
            ResultSetMetaData metaData = resultSet.getMetaData();

            while (resultSet.next()) {
                result.addElement(fetchRow(fields, resultSet, metaData, session));
            }
            resultSet.close();
        } catch (SQLException sqlException) {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (SQLException closeException) {
            }
            DatabaseException commException = processExceptionForCommError(session, sqlException, null);
            if (commException != null) throw commException;
            // Ensure that real exception is thrown.
            throw DatabaseException.sqlException(sqlException, this, session, false);
        } finally {
            decrementCallCount();
        }
        return result;
    }

    /**
     * Return the column names from a result sets meta data as a vector of DatabaseFields.
     * This is required for custom SQL execution only,
     * as generated SQL already knows the fields returned.
     */
    protected Vector getColumnNames(ResultSet resultSet, AbstractSession session) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        Vector columnNames = new Vector(metaData.getColumnCount());

        for (int index = 0; index < metaData.getColumnCount(); index++) {
            // Changed the following code to use metaData#getColumnLabel() instead of metaData.getColumnName()
            // This is as required by JDBC spec to access metadata for queries using column aliases.
            // Reconsider whether to migrate this change to other versions of Eclipselink with older native query support
            String columnName = metaData.getColumnLabel(index + 1);
            if ((columnName == null) || columnName.equals("")) {
                columnName = "C" + (index + 1);// Some column may be unnamed.
            }
            DatabaseField column = new DatabaseField(columnName);

            // Force field names to upper case is set.
            if (getPlatform().shouldForceFieldNamesToUpperCase()) {
                column.useUpperCaseForComparisons(true);
            }
            columnNames.addElement(column);
        }
        return columnNames;
    }

    /**
     * Return the receiver's connection to its data source. A connection is used to execute queries on,
     * and retrieve data from, a data source.
     * @see java.sql.Connection
     */
    @Override
    public Connection getConnection() throws DatabaseException {
        return (Connection)this.datasourceConnection;
    }

    /**
     * Return the platform.
     */
    public DatabasePlatform getPlatform() {
        return (DatabasePlatform)platform;
    }

    /**
     * return the cached metaData
     */
    public DatabaseMetaData getConnectionMetaData() throws SQLException {
        return getConnection().getMetaData();
    }

    /**
     * Return an object retrieved from resultSet with the getObject() method.
     * Optimize the get for certain type to avoid double conversion.
     * <b>NOTE</b>: This method handles a virtual machine error thrown when retrieving times & dates from Oracle or Sybase.
     */
    public Object getObject(ResultSet resultSet, DatabaseField field, ResultSetMetaData metaData, int columnNumber, DatabasePlatform platform, boolean optimizeData, AbstractSession session) throws DatabaseException {
        Object value = null;
        try {
            // PERF: Cache the JDBC type in the field to avoid JDBC call.
            int type = field.sqlType;
            if (type == NULL_SQL_TYPE) {
                type = metaData.getColumnType(columnNumber);
                field.setSqlType(type);
            }

            if (optimizeData) {
                try {
                    value = getObjectThroughOptimizedDataConversion(resultSet, field, type, columnNumber, platform, session);
                    // Since null cannot be distinguished from no optimization done, this is return for no-op.
                    if (value == null) {
                        return null;
                    }
                    if (value == this) {
                        value = null;
                    }
                } catch (SQLException exception) {
                    // Log the exception and try non-optimized data conversion
                    if (session.shouldLog(SessionLog.WARNING, SessionLog.SQL)) {
                        session.logThrowable(SessionLog.WARNING, SessionLog.SQL, exception);
                    }
                }
            }
            if (value == null) {
                if ((type == Types.LONGVARBINARY) && platform.usesStreamsForBinding()) {
                    //can read large binary data as a stream
                    InputStream tempInputStream;
                    tempInputStream = resultSet.getBinaryStream(columnNumber);
                    if (tempInputStream != null) {
                        try {
                            ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream();
                            int tempInt = tempInputStream.read();
                            while (tempInt != -1) {
                                tempOutputStream.write(tempInt);
                                tempInt = tempInputStream.read();
                            }
                            value = tempOutputStream.toByteArray();
                        } catch (IOException exception) {
                            throw DatabaseException.errorReadingBlobData();
                        }
                    }
                } else {
                    value = platform.getObjectFromResultSet(resultSet, columnNumber, type, session);
                    // PERF: only perform blob check on non-optimized types.
                    // CR2943 - convert early if the type is a BLOB or a CLOB.
                    if (isBlob(type)) {
                        // EL Bug 294578 - Store previous value of BLOB so that temporary objects can be freed after conversion
                        Object originalValue = value;
                        value = platform.convertObject(value, ClassConstants.APBYTE);
                        platform.freeTemporaryObject(originalValue);
                    } else if (isClob(type)) {
                        // EL Bug 294578 - Store previous value of CLOB so that temporary objects can be freed after conversion
                        Object originalValue = value;
                        value = platform.convertObject(value, ClassConstants.STRING);
                        platform.freeTemporaryObject(originalValue);
                    } else if (isArray(type)){
                        //Bug6068155 convert early if type is Array and Structs.
                        value = ObjectRelationalDataTypeDescriptor.buildArrayObjectFromArray(value);
                    } else if (isStruct(type, value)){
                        //Bug6068155 convert early if type is Array and Structs.
                        value=ObjectRelationalDataTypeDescriptor.buildArrayObjectFromStruct(value);
                    }
                }
            }
            // PERF: Avoid wasNull check, null is return from the get call for nullable classes.
            if ((!optimizeData) && resultSet.wasNull()) {
                value = null;
            }
        } catch (SQLException exception) {
            DatabaseException commException = processExceptionForCommError(session, exception, null);
            if (commException != null) throw commException;
            throw DatabaseException.sqlException(exception, this, session, false);
        }

        return value;
    }

    /**
     * Handle the conversion into java optimally through calling the direct type API.
     * If the type is not one that can be optimized return null.
     */
    protected Object getObjectThroughOptimizedDataConversion(ResultSet resultSet, DatabaseField field, int type, int columnNumber, DatabasePlatform platform, AbstractSession session) throws SQLException {
        Object value = this;// Means no optimization, need to distinguish from null.
        Class fieldType = field.type;

        if (platform.shouldUseGetSetNString() && (type == Types.NVARCHAR || type == Types.NCHAR)) {
            value = resultSet.getNString(columnNumber);
            if (type == Types.NCHAR && value != null && platform.shouldTrimStrings()) {
                value = Helper.rightTrimString((String) value);
            }
            return value;
        }else if (type == Types.VARCHAR || type == Types.CHAR || type == Types.NVARCHAR || type == Types.NCHAR) {
            // CUSTOM PATCH for oracle drivers because they don't respond to getObject() when using scrolling result sets.
            // Chars may require blanks to be trimmed.
            value = resultSet.getString(columnNumber);
            if ((type == Types.CHAR || type == Types.NCHAR) && (value != null) && platform.shouldTrimStrings()) {
                value = Helper.rightTrimString((String)value);
            }
            return value;
        } else if (fieldType == null) {
            return this;
        }
        boolean isPrimitive = false;

        // Optimize numeric values to avoid conversion into big-dec and back to primitives.
        if ((fieldType == ClassConstants.PLONG) || (fieldType == ClassConstants.LONG)) {
            value = Long.valueOf(resultSet.getLong(columnNumber));
            isPrimitive = ((Long)value).longValue() == 0l;
        } else if ((fieldType == ClassConstants.INTEGER) || (fieldType == ClassConstants.PINT)) {
            value = Integer.valueOf(resultSet.getInt(columnNumber));
            isPrimitive = ((Integer)value).intValue() == 0;
        } else if ((fieldType == ClassConstants.FLOAT) || (fieldType == ClassConstants.PFLOAT)) {
            value = Float.valueOf(resultSet.getFloat(columnNumber));
            isPrimitive = ((Float)value).floatValue() == 0f;
        } else if ((fieldType == ClassConstants.DOUBLE) || (fieldType == ClassConstants.PDOUBLE)) {
            value = Double.valueOf(resultSet.getDouble(columnNumber));
            isPrimitive = ((Double)value).doubleValue() == 0d;
        } else if ((fieldType == ClassConstants.SHORT) || (fieldType == ClassConstants.PSHORT)) {
            value = Short.valueOf(resultSet.getShort(columnNumber));
            isPrimitive = ((Short)value).shortValue() == 0;
        } else if ((fieldType == ClassConstants.BOOLEAN) || (fieldType == ClassConstants.PBOOLEAN))  {
            if(session.getProject().allowConvertResultToBoolean()) {
                value = Boolean.valueOf(resultSet.getBoolean(columnNumber));
                isPrimitive = ((Boolean)value).booleanValue() == false;
            }
        } else if ((type == Types.TIME) || (type == Types.DATE) || (type == Types.TIMESTAMP)) {
            if (Helper.shouldOptimizeDates) {
                // Optimize dates by avoid conversion to timestamp then back to date or time or util.date.
                String dateString = resultSet.getString(columnNumber);
                value = platform.convertObject(dateString, fieldType);
            } else {
                // PERF: Optimize dates by calling direct get method if type is Date or Time,
                // unfortunately the double conversion is unavoidable for Calendar and util.Date.
                if (fieldType == ClassConstants.SQLDATE) {
                    value = resultSet.getDate(columnNumber);
                } else if (fieldType == ClassConstants.TIME) {
                    value = resultSet.getTime(columnNumber);
                } else if (fieldType == ClassConstants.TIMESTAMP) {
                    value = resultSet.getTimestamp(columnNumber);
                } else if (fieldType == ClassConstants.TIME_LTIME) {
                    final java.sql.Timestamp ts = resultSet.getTimestamp(columnNumber);
                    value = ts != null ? ts.toLocalDateTime().toLocalTime()
                            : platform.getConversionManager().getDefaultNullValue(ClassConstants.TIME_LTIME);
                } else if (fieldType == ClassConstants.TIME_LDATE) {
                    final java.sql.Date dt = resultSet.getDate(columnNumber);
                    value = dt != null ? dt.toLocalDate()
                            : platform.getConversionManager().getDefaultNullValue(ClassConstants.TIME_LDATE);
                } else if (fieldType == ClassConstants.TIME_LDATETIME) {
                    final java.sql.Timestamp ts = resultSet.getTimestamp(columnNumber);
                    value = ts != null ? ts.toLocalDateTime()
                            : platform.getConversionManager().getDefaultNullValue(ClassConstants.TIME_LDATETIME);
                } else if (fieldType == ClassConstants.TIME_OTIME) {
                    final java.sql.Timestamp ts = resultSet.getTimestamp(columnNumber);
                    value = ts != null ? ts.toLocalDateTime().toLocalTime().atOffset(java.time.OffsetDateTime.now().getOffset())
                            : platform.getConversionManager().getDefaultNullValue(ClassConstants.TIME_OTIME);
                } else if (fieldType == ClassConstants.TIME_ODATETIME) {
                    final java.sql.Timestamp ts = resultSet.getTimestamp(columnNumber);
                    value = ts != null ? java.time.OffsetDateTime.ofInstant(ts.toInstant(), java.time.ZoneId.systemDefault())
                            : platform.getConversionManager().getDefaultNullValue(ClassConstants.TIME_ODATETIME);
                }
            }
        } else if (fieldType == ClassConstants.BIGINTEGER) {
            value = resultSet.getBigDecimal(columnNumber);
            if (value != null) return ((BigDecimal)value).toBigInteger();
        } else if (fieldType == ClassConstants.BIGDECIMAL) {
            value = resultSet.getBigDecimal(columnNumber);
         }

        // PERF: Only check for null for primitives.
        if (isPrimitive && resultSet.wasNull()) {
            value = null;
        }

        return value;
    }

    /**
     * Return if the accessor has any cached statements.
     * This should be used to avoid lazy instantiation of the cache.
     */
    protected boolean hasStatementCache() {
        return (statementCache != null) && (!statementCache.isEmpty());
    }

    /**
     * The statement cache stores a fixed sized number of prepared statements.
     */
    protected synchronized Map<String, Statement> getStatementCache() {
        if (statementCache == null) {
            statementCache = new HashMap<String, Statement>(50);
        }
        return statementCache;
    }

    /**
     * Get a description of tables available in a catalog.
     *
     * <P>Only table descriptions matching the catalog, schema, table
     * name and type criteria are returned.  They are ordered by
     * TABLE_TYPE, TABLE_SCHEM and TABLE_NAME.
     *
     * <P>Each table description has the following columns:
     *  <OL>
     *    <LI><B>TABLE_CAT</B> String => table catalog (may be null)
     *    <LI><B>TABLE_SCHEM</B> String => table schema (may be null)
     *    <LI><B>TABLE_NAME</B> String => table name
     *    <LI><B>TABLE_TYPE</B> String => table type.  Typical types are "TABLE",
     *            "VIEW",    "SYSTEM TABLE", "GLOBAL TEMPORARY",
     *            "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
     *    <LI><B>REMARKS</B> String => explanatory comment on the table
     *  </OL>
     *
     * <P><B>Note:</B> Some databases may not return information for
     * all tables.
     *
     * @param catalog a catalog name; "" retrieves those without a
     * catalog; null means drop catalog name from the selection criteria
     * @param schemaPattern a schema name pattern; "" retrieves those
     * without a schema
     * @param tableNamePattern a table name pattern
     * @param types a list of table types to include; null returns all types
     * @return a Vector of DatabaseRows.
     */
    @Override
    public Vector getTableInfo(String catalog, String schema, String tableName, String[] types, AbstractSession session) throws DatabaseException {
        if (session.shouldLog(SessionLog.FINEST, SessionLog.QUERY)) {// Avoid printing if no logging required.
            Object[] args = { catalog, schema, tableName };
            session.log(SessionLog.FINEST, SessionLog.QUERY, "query_column_meta_data", args, this);
        }
        Vector result = new Vector();
        ResultSet resultSet = null;
        try {
            incrementCallCount(session);
            resultSet = getConnectionMetaData().getTables(catalog, schema, tableName, types);
            Vector fields = buildSortedFields(null, resultSet, session);
            ResultSetMetaData metaData = resultSet.getMetaData();

            while (resultSet.next()) {
                result.addElement(fetchRow(fields, resultSet, metaData, session));
            }
            resultSet.close();
        } catch (SQLException sqlException) {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (SQLException closeException) {
            }
            DatabaseException commException = processExceptionForCommError(session, sqlException, null);
            if (commException != null) throw commException;
            // Ensure that real exception is thrown.
            throw DatabaseException.sqlException(sqlException, this, session, false);
        } finally {
            decrementCallCount();
        }
        return result;
    }

    /**
     *    Return true if the receiver is currently connected to a data source. Return false otherwise.
     */
    @Override
    public boolean isDatasourceConnected() {
        try {
            return !getConnection().isClosed();
        } catch (SQLException exception) {
            return false;
        }
    }

    /**
     * Return the batch writing mode.
     */
    protected boolean isInBatchWritingMode(AbstractSession session) {
        return getPlatform().usesBatchWriting() && this.isInTransaction;
    }

    /**
     * Prepare the SQL statement for the call.
     * First check if the statement is cached before building a new one.
     * Currently the SQL string is used as the cache key, this may have to be switched if it becomes a performance problem.
     */
    public Statement prepareStatement(DatabaseCall call, AbstractSession session) throws SQLException {
        return prepareStatement(call, session,false);
    }

    /**
     * Prepare the SQL statement for the call.
     * First check if the statement is cached before building a new one.
     * @param unwrapConnection boolean flag set to true to unwrap the connection before preparing the statement in the
     *  case of a parameterized call.
     */
    public Statement prepareStatement(DatabaseCall call, AbstractSession session, boolean unwrapConnection) throws SQLException {
        Statement statement = null;
        if (call.usesBinding(session) && call.shouldCacheStatement(session)) {
            // Check the cache by sql string, must synchronize check and removal.
            Map statementCache = getStatementCache();
            synchronized (statementCache) {
                statement = (PreparedStatement)statementCache.get(call.getSQLString());
                if (statement != null) {
                    // Need to remove to allow concurrent statement execution.
                    statementCache.remove(call.getSQLString());
                }
            }
        }

        if (statement == null) {
            Connection nativeConnection = getConnection();
            if (nativeConnection==null){
                throw DatabaseException.databaseAccessorConnectionIsNull(this, session);
            }

            // Unwrap the connection if required.
            // This needs to be done in some cases before the statement is created to ensure the statement
            // and result set are not wrapped.
            if (unwrapConnection || call.isNativeConnectionRequired()) {
                nativeConnection = getPlatform().getConnection(session, nativeConnection);
            }
            if (call.isCallableStatementRequired()) {
                // Callable statements are used for StoredProcedures and PLSQL blocks.
                if (call.isResultSetScrollable()) {
                    statement = nativeConnection.prepareCall(call.getSQLString(), call.getResultSetType(), call.getResultSetConcurrency());
                    statement.setFetchSize(call.getResultSetFetchSize());
                } else {
                    statement = nativeConnection.prepareCall(call.getSQLString());
                }
            } else if (call.isResultSetScrollable()) {
                // Scrollable statements are used for ScrollableCursors.
                statement = nativeConnection.prepareStatement(call.getSQLString(), call.getResultSetType(), call.getResultSetConcurrency());
                statement.setFetchSize(call.getResultSetFetchSize());
            } else if (call.isDynamicCall(session)) {
                // PERF: Dynamic statements are used for dynamic SQL.
                statement = allocateDynamicStatement(nativeConnection);
            } else {
                statement = nativeConnection.prepareStatement(call.getSQLString());
            }
        }

        return statement;
    }

    /**
     * Prepare the SQL statement for the call.
     * First check if the statement is cached before building a new one.
     */
    public PreparedStatement prepareStatement(String sql, AbstractSession session, boolean callable) throws SQLException {
        PreparedStatement statement = null;
        // Check the cache by sql string, must synchronize check and removal.
        if (getPlatform().shouldCacheAllStatements()) {
            Map statementCache = getStatementCache();
            synchronized (statementCache) {
                statement = (PreparedStatement)statementCache.get(sql);
                if (statement != null) {
                    // Need to remove to allow concurrent statement execution.
                    statementCache.remove(sql);
                }
            }
        }

        if (statement == null) {
            Connection nativeConnection = getConnection();
            if (nativeConnection == null ) {
                throw DatabaseException.databaseAccessorConnectionIsNull(this, session);
            }
            if (callable) {
                // Callable statements are used for StoredProcedures and PLSQL blocks.
                statement = nativeConnection.prepareCall(sql);
            } else {
                statement = nativeConnection.prepareStatement(sql);
            }
        }

        return statement;
    }

    /**
     * This method is used to process an SQL exception and determine if the exception
     * should be passed on for further processing.
     * If the Exception was communication based then a DatabaseException will be return.
     * If the method did not process the message of it was not a comm failure then null
     * will be returned.
     */
    public DatabaseException processExceptionForCommError(AbstractSession session, SQLException exception, Call call) {
        if (session.getLogin().isConnectionHealthValidatedOnError((DatabaseCall)call, this)
                && (getConnection() != null)
                && session.getServerPlatform().wasFailureCommunicationBased(exception, this, session)) {
            setIsValid(false);
            setPossibleFailure(false);
            //store exception for later as we must close the statement.
            return DatabaseException.sqlException(exception, call, this, session, true);
        } else {
            return null;
        }
    }

    /**
     * Attempt to save some of the cost associated with getting a fresh connection.
     * Assume the DatabaseDriver has been cached, if appropriate.
     * Note: Connections that are participating in transactions will not be refreshed.^M
     * Added for bug 3046465 to ensure the statement cache is cleared
     */
    @Override
    protected void reconnect(AbstractSession session) {
        clearStatementCache(session);
        super.reconnect(session);
    }

    /**
     * Release the statement through closing it or putting it back in the statement cache.
     */
    public void releaseStatement(Statement statement, String sqlString, DatabaseCall call, AbstractSession session) throws SQLException {
        if (((call == null) && getPlatform().shouldCacheAllStatements())
                || ((call != null) && call.usesBinding(session) && call.shouldCacheStatement(session))) {
            Map<String, Statement> statementCache = getStatementCache();
            synchronized (statementCache) {
                PreparedStatement preparedStatement = (PreparedStatement)statement;
                if (!statementCache.containsKey(sqlString)) {// May already be there by other thread.
                    preparedStatement.clearParameters();
                    // Bug 5709179 - reset statement settings on cached statements (dminsky) - inclusion of reset
                    if (call != null) {
                        resetStatementFromCall(preparedStatement, call);
                    }
                    if (statementCache.size() > getPlatform().getStatementCacheSize()) {
                        // Currently one is removed at random...
                        PreparedStatement removedStatement = (PreparedStatement)statementCache.remove(statementCache.keySet().iterator().next());
                        closeStatement(removedStatement, session, call);
                    } else {
                        decrementCallCount();
                    }
                    statementCache.put(sqlString, preparedStatement);
                } else {
                    // CR... Must close the statement if not cached.
                    closeStatement(statement, session, call);
                }
            }
        } else if (statement == this.dynamicStatement) {
            // The dynamic statement is cached and only closed on disconnect.
            // Bug 5709179 - reset statement settings on cached statements (dminsky) - moved to its own method
            if (call != null) {
                resetStatementFromCall(statement, call);
            }
            setIsDynamicStatementInUse(false);
            decrementCallCount();
        } else {
            closeStatement(statement, session, call);
        }
    }

    /**
     * Reset the Query Timeout, Max Rows, Resultset fetch size on the Statement
     * if the DatabaseCall has values which differ from the default settings.
     * For Bug 5709179 - reset settings on cached statements
     */
    protected void resetStatementFromCall(Statement statement, DatabaseCall call) throws SQLException {
        if (call.getQueryTimeout() > 0) {
            statement.setQueryTimeout(0);
        }
        if (call.getMaxRows() > 0) {
            statement.setMaxRows(0);
        }
        if (call.getResultSetFetchSize() > 0) {
            statement.setFetchSize(0);
        }
    }

    /**
     * Rollback a transaction on the database. This means toggling the auto-commit option.
     */
    @Override
    public void rollbackTransaction(AbstractSession session) throws DatabaseException {
        getActiveBatchWritingMechanism(session).clear();
        super.rollbackTransaction(session);
    }

    /**
     * Rollback a transaction on the database. This means toggling the auto-commit option.
     */
    @Override
    public void basicRollbackTransaction(AbstractSession session) throws DatabaseException {
        try {
            if (getPlatform().supportsAutoCommit()) {
                getConnection().rollback();
                getConnection().setAutoCommit(true);
            } else {
                getPlatform().rollbackTransaction(this);
            }
        } catch (SQLException exception) {
            DatabaseException commException = processExceptionForCommError(session, exception, null);
            if (commException != null) throw commException;
            throw DatabaseException.sqlException(exception, this, session, false);
        }
    }

    /**
     *  INTERNAL:
     *  This method is used to set the active Batch Mechanism on the accessor.
     */
    public void setActiveBatchWritingMechanismToParameterizedSQL() {
        this.activeBatchWritingMechanism = getParameterizedMechanism();

        //Bug#3214927 The size for ParameterizedBatchWriting represents the number of statements
        //and the max size is only 100.
        if (((DatabaseLogin)this.login).getMaxBatchWritingSize() == DatabasePlatform.DEFAULT_MAX_BATCH_WRITING_SIZE) {
            ((DatabaseLogin)this.login).setMaxBatchWritingSize(DatabasePlatform.DEFAULT_PARAMETERIZED_MAX_BATCH_WRITING_SIZE);
        }
    }

    /**
     *  INTERNAL:
     *  This method is used to set the active Batch Mechanism on the accessor.
     */
    public void setActiveBatchWritingMechanismToDynamicSQL() {
        this.activeBatchWritingMechanism = getDynamicSQLMechanism();
        // Bug#3214927-fix - Also the size must be switched back when switch back from param to dynamic.
        if (((DatabaseLogin)this.login).getMaxBatchWritingSize() == DatabasePlatform.DEFAULT_PARAMETERIZED_MAX_BATCH_WRITING_SIZE) {
            ((DatabaseLogin)this.login).setMaxBatchWritingSize(DatabasePlatform.DEFAULT_MAX_BATCH_WRITING_SIZE);
        }
    }

    /**
     *  INTERNAL:
     *  This method is used to set the active Batch Mechanism on the accessor.
     */
    public void setActiveBatchWritingMechanism(BatchWritingMechanism mechanism) {
        this.activeBatchWritingMechanism = mechanism;
    }
    /**
     * The statement cache stores a fixed sized number of prepared statements.
     */
    protected void setStatementCache(Hashtable statementCache) {
        this.statementCache = statementCache;
    }

    /**
     * This method will sort the fields in correct order based
     * on the column names.
     */
    protected Vector sortFields(Vector fields, Vector columnNames) {
        Vector sortedFields = new Vector(columnNames.size());
        Vector eligableFields = (Vector)fields.clone();// Must clone to allow removing to support the same field twice.
        Enumeration columnNamesEnum = columnNames.elements();
        boolean valueFound;
        DatabaseField field;
        DatabaseField column;//DatabaseField from the columnNames vector
        while (columnNamesEnum.hasMoreElements()) {
            field = null;
            valueFound = false;
            column = (DatabaseField)columnNamesEnum.nextElement();
            Enumeration fieldEnum = eligableFields.elements();
            while (fieldEnum.hasMoreElements()) {
                field = (DatabaseField)fieldEnum.nextElement();
                if(field != null && field.equals(column)){
                    valueFound = true;
                    sortedFields.addElement(field);
                    break;
                }
            }

            if (valueFound) {
                // The eligible fields must be maintained as two field can have the same name, but different tables.
                eligableFields.removeElement(field);
            } else {
                // Need to add a place holder in case the column is not in the fields vector
                sortedFields.addElement(new DatabaseField());
            }
        }
        return sortedFields;
    }

    @Override
    public String toString() {
        StringWriter writer = new StringWriter();
        writer.write("DatabaseAccessor(");
        if (isConnected()) {
            writer.write(ToStringLocalization.buildMessage("connected", (Object[])null));
        } else {
            writer.write(ToStringLocalization.buildMessage("disconnected", (Object[])null));
        }
        writer.write(")");
        return writer.toString();
    }

    /**
     * Return if the JDBC type is a ARRAY type.
     */
    private boolean isArray(int type) {
        return (type == Types.ARRAY);
    }

    /**
     * Return if the JDBC type is a binary type such as blob.
     */
    public static boolean isBlob(int type) {
        return (type == Types.BLOB) || (type == Types.LONGVARBINARY);
    }

    /**
     * Return if the JDBC type is a large character type such as clob.
     */
    public static boolean isClob(int type) {
        return (type == Types.CLOB) || (type == Types.LONGVARCHAR) || (type == DatabasePlatform.Types_NCLOB) || (type == Types.LONGNVARCHAR);
    }

    /**
     * Return if the JDBC type is a STRUCT type.
     */
    private boolean isStruct(int type, Object value) {
        return (type == Types.STRUCT && (value instanceof java.sql.Struct));
    }

    /**
     * This method will be called after a series of writes have been issued to
     * mark where a particular set of writes has completed.  It will be called
     * from commitTransaction and may be called from writeChanges.   Its main
     * purpose is to ensure that the batched statements have been executed
     */
    @Override
    public void writesCompleted(AbstractSession session) {
        if (isConnected && isInBatchWritingMode(session)) {
            getActiveBatchWritingMechanism(session).executeBatchedStatements(session);
        }
    }
}
