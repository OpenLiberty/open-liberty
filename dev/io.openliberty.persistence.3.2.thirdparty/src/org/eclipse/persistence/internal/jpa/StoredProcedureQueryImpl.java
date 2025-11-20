/*
 * Copyright (c) 2012, 2024 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2025 IBM Corporation. All rights reserved.
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
//     Zoltan NAGY & tware - updated support for MaxRows
//     11/01/2010-2.2 Guy Pelletier
//       - 322916: getParameter on Query throws NPE
//     11/09/2010-2.1 Michael O'Brien
//       - 329089: PERF: EJBQueryImpl.setParamenterInternal() move indexOf check inside non-native block
//     02/08/2012-2.4 Guy Pelletier
//       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
//     06/20/2012-2.5 Guy Pelletier
//       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
//     07/13/2012-2.5 Guy Pelletier
//       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
//     08/24/2012-2.5 Guy Pelletier
//       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
//     09/13/2012-2.5 Guy Pelletier
//       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
//     09/27/2012-2.5 Guy Pelletier
//       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
//     11/05/2012-2.5 Guy Pelletier
//       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
//     08/23/2023: Tomas Kraus
//       - New Jakarta Persistence 3.2 Features
package org.eclipse.persistence.internal.jpa;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.QueryTimeoutException;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TemporalType;

import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.internal.databaseaccess.*;
import org.eclipse.persistence.internal.databaseaccess.DatasourceCall.ParameterType;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.jpa.querydef.ParameterExpressionImpl;
import org.eclipse.persistence.internal.localization.ExceptionLocalization;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.queries.DataReadQuery;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.ResultSetMappingQuery;
import org.eclipse.persistence.queries.SQLResultSetMapping;
import org.eclipse.persistence.queries.StoredProcedureCall;

/**
 * Concrete JPA query class. The JPA query wraps a StoredProcesureQuery which
 * is executed.
 */
public class StoredProcedureQueryImpl extends QueryImpl implements StoredProcedureQuery {
    protected boolean hasMoreResults;

    // Call will be returned from an execute. From it you can get the result set.
    protected DatabaseCall executeCall;
    protected Statement executeStatement;
    protected int executeResultSetIndex = -1;

    // If the procedure returns output cursor(s), we'll use them to satisfy
    // getResultList and getSingleResult calls so keep track of our index.
    protected int outputCursorIndex = -1;
    protected boolean isOutputCursorResultSet = false;

    /**
     * Base constructor for StoredProcedureQueryImpl. Initializes basic variables.
     */
    protected StoredProcedureQueryImpl(EntityManagerImpl entityManager) {
        super(entityManager);
    }

    /**
     * Create an StoredProcedureQueryImpl with a DatabaseQuery.
     */
    public StoredProcedureQueryImpl(DatabaseQuery query, EntityManagerImpl entityManager) {
        super(query, entityManager);
        // Inherit applicable hints from EntityManager
        inheritEntityManagerHints();
    }

    /**
     * Create an StoredProcedureQueryImpl with a query name.
     */
    public StoredProcedureQueryImpl(String name, EntityManagerImpl entityManager) {
        super(entityManager);
        this.queryName = name;
        // Inherit applicable hints from EntityManager
        inheritEntityManagerHints();
    }

    /**
     * Build the given result set into a list objects. Assumes there is an
     * execute call available and therefore should not be called unless an
     * execute statement was issued by the user.
     */
    protected List<?> buildResultRecords(ResultSet resultSet) {
        try {
            AbstractSession session = (AbstractSession) getActiveSession();
            DatabaseAccessor accessor = (DatabaseAccessor) executeCall.getQuery().getAccessor();

            executeCall.setFields(null);
            executeCall.matchFieldOrder(resultSet, accessor, session);
            ResultSetMetaData metaData = resultSet.getMetaData();

            List<AbstractRecord> result =  new Vector<>();
            while (resultSet.next()) {
                result.add(accessor.fetchRow(executeCall.getFields(), executeCall.getFieldsArray(), resultSet, metaData, session));
            }

            // The result set must be closed in case the statement is cached and not closed.
            resultSet.close();

            return result;
        } catch (Exception e) {
            setRollbackOnly();
            throw new PersistenceException(e);
        }
    }

    /**
     * Build a ResultSetMappingQuery from a sql result set mapping name and a
     * stored procedure call.
     * <p>
     * This is called from a named stored procedure that employs result set
     * mapping name(s) which should be available from the session.
     */
    public static DatabaseQuery buildResultSetMappingNameQuery(List<String> resultSetMappingNames, StoredProcedureCall call) {
        ResultSetMappingQuery query = new ResultSetMappingQuery();
        call.setReturnMultipleResultSetCollections(call.hasMultipleResultSets() && ! call.isMultipleCursorOutputProcedure());
        query.setCall(call);
        query.setIsUserDefined(true);
        query.setSQLResultSetMappingNames(resultSetMappingNames);
        return query;
    }

    /**
     * Build a ResultSetMappingQuery from a sql result set mapping name and a
     * stored procedure call.
     * <p>
     * This is called from a named stored procedure that employs result set
     * mapping name(s) which should be available from the session.
     */
    public static DatabaseQuery buildResultSetMappingNameQuery(List<String> resultSetMappingNames, StoredProcedureCall call, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        // apply any query hints
        DatabaseQuery hintQuery = applyHints(hints, buildResultSetMappingNameQuery(resultSetMappingNames, call) , classLoader, session);

        // apply any query arguments
        applyArguments(call, hintQuery);

        return hintQuery;
    }

    /**
     * Build a ResultSetMappingQuery from the sql result set mappings given
     *  a stored procedure call.
     * <p>
     * This is called from a named stored procedure query that employs result
     * class name(s). The resultSetMappings are build from these class name(s)
     * and are not available from the session.
     */
    public static DatabaseQuery buildResultSetMappingQuery(List<SQLResultSetMapping> resultSetMappings, StoredProcedureCall call) {
        ResultSetMappingQuery query = new ResultSetMappingQuery();
        call.setReturnMultipleResultSetCollections(call.hasMultipleResultSets() && ! call.isMultipleCursorOutputProcedure());
        query.setCall(call);
        query.setIsUserDefined(true);
        query.setSQLResultSetMappings(resultSetMappings);
        return query;
    }

    /**
     * Build a ResultSetMappingQuery from the sql result set mappings given
     *  a stored procedure call.
     * <p>
     * This is called from a named stored procedure query that employs result
     * class name(s). The resultSetMappings are build from these class name(s)
     * and are not available from the session.
     */
    public static DatabaseQuery buildResultSetMappingQuery(List<SQLResultSetMapping> resultSetMappings, StoredProcedureCall call, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        // apply any query hints
        DatabaseQuery hintQuery = applyHints(hints, buildResultSetMappingQuery(resultSetMappings, call), classLoader, session);

        // apply any query arguments
        applyArguments(call, hintQuery);

        return hintQuery;
    }

    /**
     * Build a ReadAllQuery from a class and stored procedure call.
     */
    public static DatabaseQuery buildStoredProcedureQuery(Class<?> resultClass, StoredProcedureCall call, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        DatabaseQuery query = new ReadAllQuery(resultClass);
        query.setCall(call);
        query.setIsUserDefined(true);

        // apply any query hints
        query = applyHints(hints, query, classLoader, session);

        // apply any query arguments
        applyArguments(call, query);

        return query;
    }

    /**
     * Build a DataReadQuery with the stored procedure call given.
     */
    public static DatabaseQuery buildStoredProcedureQuery(StoredProcedureCall call, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        DataReadQuery query = new DataReadQuery();
        query.setResultType(DataReadQuery.AUTO);

        query.setCall(call);
        query.setIsUserDefined(true);

        // apply any query hints
        DatabaseQuery hintQuery = applyHints(hints, query, classLoader, session);

        // apply any query arguments
        applyArguments(call, hintQuery);

        return hintQuery;
    }

    /**
     * Build a ResultSetMappingQuery from a sql result set mapping name and a
     * stored procedure call.
     */
    public static DatabaseQuery buildStoredProcedureQuery(String sqlResultSetMappingName, StoredProcedureCall call, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        ResultSetMappingQuery query = new ResultSetMappingQuery();
        query.setSQLResultSetMappingName(sqlResultSetMappingName);
        query.setCall(call);
        query.setIsUserDefined(true);

        // apply any query hints
        DatabaseQuery hintQuery = applyHints(hints, query, classLoader, session);

        // apply any query arguments
        applyArguments(call, hintQuery);

        return hintQuery;
    }

    /**
     * Call this method to close any open connections to the database.
     */
    @Override
    public void close() {
        if (executeCall != null) {
            DatabaseQuery query = executeCall.getQuery();
            AbstractSession session = query.getSession();

            // Release the accessors acquired for the query.
            for (Accessor accessor : query.getAccessors()) {
                session.releaseReadConnection(accessor);
            }

            try {
                if (executeStatement != null) {
                    DatabaseAccessor accessor = (DatabaseAccessor) query.getAccessor();
                    accessor.releaseStatement(executeStatement, query.getSQLString(), executeCall, session);
                }
            } catch (SQLException exception) {
                // Catch the exception and log a message.
                session.log(SessionLog.WARNING, SessionLog.CONNECTION, "exception_caught_closing_statement", exception);
            }
        }

        executeCall = null;
        executeStatement = null;
    }

    /**
     * Returns true if the first result corresponds to a result set, and false
     * if it is an update count or if there are no results other than through
     * INOUT and OUT parameters, if any.
     * @return true if first result corresponds to result set
     * @throws QueryTimeoutException if the query execution exceeds the query
     * timeout value set and only the statement is rolled back
     * @throws PersistenceException if the query execution exceeds the query
     * timeout value set and the transaction is rolled back
     */
    @Override
    public boolean execute() {
        try {
            entityManager.verifyOpen();

            if (! getDatabaseQueryInternal().isResultSetMappingQuery()) {
                throw new IllegalStateException(ExceptionLocalization.buildMessage("incorrect_spq_query_for_execute"));
            }

            getResultSetMappingQuery().setIsExecuteCall(true);
            executeCall = (DatabaseCall) executeReadQuery();
            executeStatement = executeCall.getStatement();

            // Add this query to the entity manager open queries list.
            // The query will be closed in the following cases:
            // Within a transaction:
            //  - on commit
            //  - on rollback
            // Outside of a transaction:
            //  - em close
            // Other safeguards, we will close the query if/when
            //  - we hit the end of the results.
            //  - this query is garbage collected (finalize method)
            //
            // Deferring closing the call avoids having to go through all the
            // results now (and building all the result objects) and things
            // remain on a as needed basis from the statement.
            entityManager.addOpenQuery(this);

            hasMoreResults = executeCall.getExecuteReturnValue();

            // If execute returned false but we have output cursors then return
            // true and build the results from the output cursors.
            if (!hasMoreResults && getCall().hasOutputCursors()) {
                hasMoreResults = true;
                outputCursorIndex = 0;
                isOutputCursorResultSet = true;
            }

            return hasMoreResults;
        } catch (LockTimeoutException exception) {
            throw exception;
        } catch (PersistenceException | IllegalStateException exception) {
            setRollbackOnly();
            throw exception;
        } catch (RuntimeException exception) {
            setRollbackOnly();
            throw new PersistenceException(exception);
        }
    }

    /**
     * Execute an update or delete statement (from a stored procedure query).
     * @return the number of entities updated or deleted
     */
    @Override
    public int executeUpdate() {
        try {
            // Need to throw TransactionRequiredException if there is no active transaction
            entityManager.checkForTransaction(true);

            // Legacy: we could have a data read query or a read all query, so
            // clearly we shouldn't be executing an update on it. As of JPA 2.1
            // API we always create a result set mapping query to interact with
            // a stored procedure.
            // Also if the result set mapping query has result set mappings
            // defined, then it's clearly expecting result sets and we can be
            // preemptive in throwing an exception.
            if (! getDatabaseQueryInternal().isResultSetMappingQuery() || getResultSetMappingQuery().hasResultSetMappings()) {
                throw new IllegalStateException(ExceptionLocalization.buildMessage("incorrect_spq_query_for_execute_update"));
            }

            // If the return value is true indicating a result set then throw an exception.
            if (execute()) {
                throw new IllegalStateException(ExceptionLocalization.buildMessage("incorrect_spq_query_for_execute_update"));
            } else {
                return getUpdateCount();
            }
        } catch (LockTimeoutException exception) {
            throw exception;
        } catch (PersistenceException | IllegalStateException e) {
            setRollbackOnly();
            throw e;
        } catch (RuntimeException exception) {
            setRollbackOnly();
            throw new PersistenceException(exception);
        } finally {
            close(); // Close the connection once we're done.
        }
    }

    /**
     * Finalize method in case the query is not closed.
     */
    @Override
    @SuppressWarnings("removal")
    public void finalize() {
        close();
    }

    /**
     * Return the stored procedure call associated with this query.
     */
    protected StoredProcedureCall getCall() {
        return (StoredProcedureCall) getDatabaseQueryInternal().getCall();
    }

    /**
     * Return the internal map of parameters.
     */
    @Override
    protected Map<String, Parameter<?>> getInternalParameters() {
        if (parameters == null) {
            parameters = new HashMap<>();

            int index = 0;

            for (Object parameter : getCall().getParameters()) {
                ParameterType parameterType = getCall().getParameterTypes().get(index);
                String argumentName = getCall().getProcedureArgumentNames().get(index);

                DatabaseField field = null;

                if (parameterType == ParameterType.INOUT) {
                    field = (DatabaseField) ((Object[]) parameter)[0];
                } else if (parameterType == ParameterType.IN) {
                    field = (DatabaseField) parameter;
                } else if (parameterType == ParameterType.OUT || parameterType == ParameterType.OUT_CURSOR) {
                    if (parameter instanceof OutputParameterForCallableStatement) {
                        field = ((OutputParameterForCallableStatement) parameter).getOutputField();
                    } else {
                        field = (DatabaseField) parameter;
                    }
                }

                // If field is not null (one we care about) then add it, otherwise continue.
                if (field != null) {
                    // If the argument name is null then it is a positional parameter.
                    if (argumentName == null) {
                        parameters.put(field.getName(), new ParameterExpressionImpl(null, field.getType(), Integer.parseInt(field.getName())));
                    } else {
                        parameters.put(field.getName(), new ParameterExpressionImpl(null, field.getType(), field.getName()));
                    }
                }

                ++index;
            }
        }

        return parameters;
    }

    /**
     * Used to retrieve the values passed back from the procedure through INOUT
     * and OUT parameters. For portability, all results corresponding to result
     * sets and update counts must be retrieved before the values of output
     * parameters.
     * @param position parameter position
     * @return the result that is passed back through the parameter
     * @throws IllegalArgumentException if the position does not correspond to a
     * parameter of the query or is not an INOUT or OUT parameter
     */
    @Override
    public Object getOutputParameterValue(int position) {
        entityManager.verifyOpen();

        if (isValidCallableStatement()) {
            try {
                Object obj = executeCall.getOutputParameterValue((CallableStatement) executeStatement, position - 1, entityManager.getAbstractSession());

                if (obj instanceof ResultSet) {
                    // If a result set is returned we have to build the objects.
                    return getResultSetMappingQuery().buildObjectsFromRecords(buildResultRecords((ResultSet) obj), ++executeResultSetIndex);
                } else {
                    return obj;
                }
            } catch (Exception exception) {
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("jpa21_invalid_parameter_position", new Object[] { position, exception.getMessage() }), exception);
            }
        }

        return null;
    }

    /**
     * Used to retrieve the values passed back from the procedure through INOUT
     * and OUT parameters. For portability, all results corresponding to result
     * sets and update counts must be retrieved before the values of output
     * parameters.
     * @param parameterName name of the parameter as registered or specified in
     *        metadata
     * @return the result that is passed back through the parameter
     * @throws IllegalArgumentException if the parameter name does not
     * correspond to a parameter of the query or is not an INOUT or OUT parameter
     */
    @Override
    public Object getOutputParameterValue(String parameterName) {
        entityManager.verifyOpen();

        if (isValidCallableStatement()) {
            try {
                Object obj = executeCall.getOutputParameterValue((CallableStatement) executeStatement, parameterName, entityManager.getAbstractSession());

                if (obj instanceof ResultSet) {
                    // If a result set is returned we have to build the objects.
                    return getResultSetMappingQuery().buildObjectsFromRecords(buildResultRecords((ResultSet) obj), ++executeResultSetIndex);
                }
                return obj;
            } catch (Exception exception) {
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("jpa21_invalid_parameter_name", new Object[] { parameterName, exception.getMessage() }), exception);
            }
        }

        return null;
    }

    private boolean hasPositionalParameters() {
        for (Parameter parameter: this.getParameters()) {
            if (parameter.getName() != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Execute the query and return the query results as a List.
     * @return a list of the results
     */
    @Override
    public List getResultList() {
        // bug51411440: need to throw IllegalStateException if query
        // executed on closed em
        this.entityManager.verifyOpenWithSetRollbackOnly();
        try {
            // If there is no execute statement, the user has not called
            // execute and is simply calling getResultList directly on the query.
            if (executeStatement == null) {
                // If it's not a result set mapping query (as of JPA 2.1 we
                // always create a result set mapping query to interact with a
                // stored procedure) then throw an exception.
                if (! getDatabaseQueryInternal().isResultSetMappingQuery()) {
                    throw new IllegalStateException(ExceptionLocalization.buildMessage("incorrect_spq_query_for_get_result_list"));
                }

                // If the return value is false indicating no result set then throw an exception.
                if (execute()) {
                    return getResultList();
                } else {
                    throw new IllegalStateException(ExceptionLocalization.buildMessage("incorrect_spq_query_for_get_result_list"));
                }
            } else {
                if (hasMoreResults()) {
                    if (isOutputCursorResultSet) {
                        // Return result set list for the current outputCursorIndex.
                        List results = null;
                        if (hasPositionalParameters()) {
                            results = (List) getOutputParameterValue(getCall().getOutputCursors().get(outputCursorIndex++).getIndex() + 1);
                        } else {
                            results = (List) getOutputParameterValue(getCall().getOutputCursors().get(outputCursorIndex++).getName());
                        }

                        // Update the hasMoreResults flag.
                        hasMoreResults = (outputCursorIndex < getCall().getOutputCursors().size());

                        return results;
                    } else {
                        // Build the result records first.
                        List result = buildResultRecords(executeStatement.getResultSet());

                        // Move the result pointer.
                        moveResultPointer();

                        return getResultSetMappingQuery().buildObjectsFromRecords(result, ++executeResultSetIndex);
                    }
                } else {
                    return null;
                }
            }
        } catch (LockTimeoutException e) {
            throw e;
        } catch (PersistenceException | IllegalStateException e) {
            setRollbackOnly();
            throw e;
        } catch (Exception e) {
            setRollbackOnly();
            throw new PersistenceException(e);
        }
    }

    /**
     * Return the ResultSetMappingQuery for this stored procedure query.
     * NOTE: Methods assumes associated database query is a ResultSetMappingQuery.
     */
    protected ResultSetMappingQuery getResultSetMappingQuery() {
        if (executeCall != null) {
            return (ResultSetMappingQuery) executeCall.getQuery();
        } else {
            return (ResultSetMappingQuery) getDatabaseQuery();
        }
    }

    @Override
    public Object getSingleResultOrNull() {
        return getSingleResult(false);
    }

    @Override
    public Object getSingleResult() {
        return getSingleResult(true);
    }

    private Object getSingleResult(boolean failOnEmpty) {
        // bug51411440: need to throw IllegalStateException if query
        // executed on closed em
        this.entityManager.verifyOpenWithSetRollbackOnly();
        try {
            // If there is no execute statement, the user has not called
            // execute and is simply calling getSingleResult directly on the query.
            if (executeStatement == null) {
                // If it's not a result set mapping query (as of JPA 2.1 we
                // always create a result set mapping query to interact with a
                // stored procedure) then throw an exception.
                if (! getDatabaseQueryInternal().isResultSetMappingQuery()) {
                    throw new IllegalStateException(ExceptionLocalization.buildMessage("incorrect_spq_query_for_get_single_result"));
                }

                // If the return value is true indicating a result set then
                // build and return the single result.
                if (execute()) {
                    return getSingleResult(failOnEmpty);
                } else {
                    throw new IllegalStateException(ExceptionLocalization.buildMessage("incorrect_spq_query_for_get_result_list"));
                }
            } else {
                if (hasMoreResults()) {
                    // Build the result records first.
                    List<?> results;

                    if (isOutputCursorResultSet) {
                        // Return result set list for the current outputCursorIndex.
                        if (hasPositionalParameters()) {
                            results = (List<?>) getOutputParameterValue(getCall().getOutputCursors().get(outputCursorIndex++).getIndex() + 1);
                        } else {
                            results = (List<?>) getOutputParameterValue(getCall().getOutputCursors().get(outputCursorIndex++).getName());
                        }

                        // Update the hasMoreResults flag.
                        hasMoreResults = (outputCursorIndex < getCall().getOutputCursors().size());
                    } else {
                        // Build the result records first.
                        List<?> result = buildResultRecords(executeStatement.getResultSet());

                        // Move the result pointer.
                        moveResultPointer();

                        results = getResultSetMappingQuery().buildObjectsFromRecords(result, ++executeResultSetIndex);
                    }

                    if (results.size() > 1) {
                        throwNonUniqueResultException(ExceptionLocalization.buildMessage("too_many_results_for_get_single_result", null));
                    } else if (results.isEmpty()) {
                        if (failOnEmpty) {
                            throwNoResultException(ExceptionLocalization.buildMessage("no_entities_retrieved_for_get_single_result", null));
                        } else {
                            return null;
                        }
                    }
                    // If hasMoreResults is true, we should throw an exception here.
                    if (results.size() > 1 || hasMoreResults) {
                        throwNonUniqueResultException(ExceptionLocalization.buildMessage("too_many_results_for_get_single_result", null));
                    }

                    return results.get(0);
                } else {
                    return null;
                }
            }
        } catch (LockTimeoutException e) {
            throw e;
        } catch (PersistenceException | IllegalStateException e) {
            setRollbackOnly();
            throw e;
        } catch (Exception e) {
            setRollbackOnly();
            throw new PersistenceException(e);
        } finally {
            close(); // Close the connection once we're done.
        }
    }

    /**
     * Returns the update count or -1 if there is no pending result
     * or if the next result is not an update count.
     * @return update count or -1 if there is no pending result or
     * if the next result is not an update count
     * @throws QueryTimeoutException if the query execution exceeds
     * the query timeout value set and only the statement is
     * rolled back
     * @throws PersistenceException if the query execution exceeds
     * the query timeout value set and the transaction
     * is rolled back
     */
    @Override
    public int getUpdateCount() {
        entityManager.verifyOpenWithSetRollbackOnly();

        if (executeStatement != null) {
            try {
                int updateCount = executeStatement.getUpdateCount();

                // Moving the result pointer when -1 is reached doesn't seem
                // to be an issue for the jbdc driver, however as a safeguard,
                // once -1 is reached don't bother trying to move the pointer
                // as there is no need to do so.
                if (updateCount > -1) {
                    moveResultPointer();
                }
                return updateCount;
            } catch (SQLException e) {
                throw getDetailedException(DatabaseException.sqlException(e, executeCall, executeCall.getQuery().getAccessor(), executeCall.getQuery().getSession(), false));
            }
        }

        return -1;
    }

    /**
     * Returns true if the next result corresponds to a result set, and false if
     * it is an update count or if there are no results other than through INOUT
     * and OUT parameters, if any.
     *
     * @return true if next result corresponds to result set
     * @throws QueryTimeoutException if the query execution exceeds the query
     * timeout value set and only the statement is rolled back
     * @throws PersistenceException if the query execution exceeds the query
     * timeout value set and the transaction is rolled back
     */
    @Override
    public boolean hasMoreResults() {
        entityManager.verifyOpen();

        return hasMoreResults;
    }

    /**
     * Returns true if the execute statement for this query is 1) not null (i.e.
     * query has been executed and 2) is an instance of callable statement,
     * meaning there are out parameters associated with it.
     */
    protected boolean isValidCallableStatement() {
        if (executeStatement == null) {
            throw new IllegalStateException(ExceptionLocalization.buildMessage("jpa21_invalid_call_on_un_executed_query"));
        }

        if (! (executeStatement instanceof CallableStatement)) {
            throw new IllegalStateException(ExceptionLocalization.buildMessage("jpa21_invalid_call_with_no_output_parameters"));
        }

        return true;
    }

    /**
     * INTERNAL:
     * Move the pointer up and update our has more results flag.
     * Once there are no result sets left, this will always return false.
     */
    private void moveResultPointer() {
        try {
            hasMoreResults = executeStatement.getMoreResults();
        } catch (SQLException e) {
            // swallow it.
            hasMoreResults = false;
        }
    }

    /**
     * Register a positional parameter. All positional parameters must be
     * registered.
     *
     * @param position parameter position
     * @param type type of the parameter
     * @param mode parameter mode
     * @return the same query instance
     */
    @Override
    @SuppressWarnings({"rawtypes"})
    public StoredProcedureQuery registerStoredProcedureParameter(int position, Class type, ParameterMode mode) {
        entityManager.verifyOpenWithSetRollbackOnly();
        StoredProcedureCall call = (StoredProcedureCall) getDatabaseQuery().getCall();

        if (mode.equals(ParameterMode.IN)) {
            call.addUnamedArgument(String.valueOf(position), type);
        } else if (mode.equals(ParameterMode.OUT)) {
            call.addUnamedOutputArgument(String.valueOf(position), type);
        } else if (mode.equals(ParameterMode.INOUT)) {
            call.addUnamedInOutputArgument(String.valueOf(position), String.valueOf(position), type);
        } else if (mode.equals(ParameterMode.REF_CURSOR)) {
            call.useUnnamedCursorOutputAsResultSet(position);
        }

        // Force a re-calculate of the parameters.
        this.parameters = null;

        return this;
    }

    /**
     * Register a named parameter. When using parameter names, all parameters
     * must be registered in the order in which they occur in the parameter list
     * of the stored procedure.
     *
     * @param parameterName name of the parameter as registered or
     *        specified in metadata
     * @param type type of the parameter
     * @param mode parameter mode
     * @return the same query instance
     */
    @Override
    @SuppressWarnings({"rawtypes"})
    public StoredProcedureQuery registerStoredProcedureParameter(String parameterName, Class type, ParameterMode mode) {
        entityManager.verifyOpenWithSetRollbackOnly();
        StoredProcedureCall call = (StoredProcedureCall) getDatabaseQuery().getCall();

        if (mode.equals(ParameterMode.IN)) {
            call.addNamedArgument(parameterName, parameterName, type);
        } else if (mode.equals(ParameterMode.OUT)) {
            call.addNamedOutputArgument(parameterName, parameterName, type);
        } else if (mode.equals(ParameterMode.INOUT)) {
            call.addNamedInOutputArgument(parameterName, parameterName, parameterName, type);
        } else if (mode.equals(ParameterMode.REF_CURSOR)) {
            call.useNamedCursorOutputAsResultSet(parameterName);
        }

        // Force a re-calculate of the parameters.
        this.parameters = null;

        return this;
    }

    /**
     * Set the position of the first result to retrieve.
     *
     * @param startPosition
     *            position of the first result, numbered from 0
     * @return the same query instance
     */
    @Override
    public StoredProcedureQueryImpl setFirstResult(int startPosition) {
        throw new IllegalStateException(ExceptionLocalization.buildMessage("operation_not_supported", new Object[]{"setFirstResult", "StoredProcedureQuery"}));
    }

    /**
     * Set the flush mode type to be used for the query execution.
     * The flush mode type applies to the query regardless of the
     * flush mode type in use for the entity manager.
     * @param flushMode flush mode
     * @return the same query instance
     */
    @Override
    public StoredProcedureQueryImpl setFlushMode(FlushModeType flushMode) {
        return (StoredProcedureQueryImpl) super.setFlushMode(flushMode);
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        return FindOptionUtils.getCacheRetrieveMode(entityManager.getAbstractSession(), getDatabaseQuery().getProperties());
    }

    @Override
    public StoredProcedureQueryImpl setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        FindOptionUtils.setCacheRetrieveMode(getDatabaseQuery().getProperties(), cacheRetrieveMode);
        setHint(QueryHints.CACHE_RETRIEVE_MODE, cacheRetrieveMode);
        return this;
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        return FindOptionUtils.getCacheStoreMode(entityManager.getAbstractSession(), getDatabaseQuery().getProperties());
    }

    @Override
    public StoredProcedureQueryImpl setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        FindOptionUtils.setCacheStoreMode(getDatabaseQuery().getProperties(), cacheStoreMode);
        setHint(QueryHints.CACHE_STORE_MODE, cacheStoreMode);
        return this;
    }

    @Override
    public Integer getTimeout() {
        return FindOptionUtils.getTimeout(entityManager.getAbstractSession(), getDatabaseQuery().getProperties());
    }

    @Override
    public StoredProcedureQueryImpl setTimeout(Integer timeout) {
        FindOptionUtils.setTimeout(getDatabaseQuery().getProperties(), timeout);
        setHint(QueryHints.QUERY_TIMEOUT, timeout);
        return this;
    }

    /**
     * Inherit applicable query hints from the EntityManager.
     * This method is called during query construction to automatically propagate
     * EntityManager-level settings to newly created queries.
     *
     * Currently propagates cache-related hints (CACHE_RETRIEVE_MODE and CACHE_STORE_MODE)
     * from EntityManager to queries.
     * - CACHE_RETRIEVE_MODE only applies to ObjectLevelReadQuery (SELECT queries)
     * - CACHE_STORE_MODE applies to both ObjectLevelReadQuery (SELECT) and ModifyQuery (UPDATE/DELETE)
     *
     * This follows the same pattern as EntityManagerImpl.getQueryHints() (lines 2840-2899)
     * but is specifically designed for createStoredProcedureQuery() operations.
     */
    protected void inheritEntityManagerHints() {
        if (entityManager == null || entityManager.properties == null) {
            return;
        }

        DatabaseQuery dbQuery = getDatabaseQuery();
        if (dbQuery == null) {
            return;
        }

        Map<String, Object> emProperties = entityManager.properties;

        // CACHE_RETRIEVE_MODE only applies to ObjectLevelReadQuery (SELECT queries)
        if (dbQuery.isObjectLevelReadQuery() && emProperties.containsKey(QueryHints.CACHE_RETRIEVE_MODE)) {
            setHint(QueryHints.CACHE_RETRIEVE_MODE, emProperties.get(QueryHints.CACHE_RETRIEVE_MODE));
        }

        // CACHE_STORE_MODE applies to all query types:
        // - For ObjectLevelReadQuery: controls whether results are stored in cache after reading
        // - For ModifyQuery: controls whether cache is invalidated after UPDATE/DELETE
        if (emProperties.containsKey(QueryHints.CACHE_STORE_MODE)) {
            setHint(QueryHints.CACHE_STORE_MODE, emProperties.get(QueryHints.CACHE_STORE_MODE));
        }
    }

    /**
     * Set a query property or hint. The hints elements may be used to specify
     * query properties and hints. Properties defined by this specification must
     * be observed by the provider. Vendor-specific hints that are not
     * recognized by a provider must be silently ignored. Portable applications
     * should not rely on the standard timeout hint. Depending on the database
     * in use, this hint may or may not be observed.
     *
     * @param hintName name of the property or hint
     * @param value value for the property or hint
     * @return the same query instance
     * @throws IllegalArgumentException if the second argument is not valid for
     * the implementation
     */
    @Override
    public StoredProcedureQuery setHint(String hintName, Object value) {
        try {
            entityManager.verifyOpen();
            setHintInternal(hintName, value);
            return this;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * Set the lock mode type to be used for the query execution.
     *
     * @throws IllegalStateException
     *             if not a Java Persistence query language SELECT query
     */
    @Override
    public StoredProcedureQueryImpl setLockMode(LockModeType lockMode) {
        return (StoredProcedureQueryImpl) super.setLockMode(lockMode);
    }

    /**
     * Set the maximum number of results to retrieve.
     *
     * @return the same query instance
     */
    @Override
    public StoredProcedureQueryImpl setMaxResults(int maxResult) {
        throw new IllegalStateException(ExceptionLocalization.buildMessage("operation_not_supported", new Object[]{"setMaxResults", "StoredProcedureQuery"}));
    }

    /**
     * Bind an instance of java.util.Calendar to a positional parameter.
     *
     * @return the same query instance
     * @throws IllegalArgumentException if position does not correspond to a
     * positional parameter of the query or if the value argument is of
     * incorrect type
     */
    @Override
    public StoredProcedureQuery setParameter(int position, Calendar value, TemporalType temporalType) {
        entityManager.verifyOpenWithSetRollbackOnly();
        return setParameter(position, convertTemporalType(value, temporalType));
    }

    /**
     * Bind an instance of java.util.Date to a positional parameter.
     *
     * @return the same query instance
     * @throws IllegalArgumentException if position does not correspond to a
     * positional parameter of the query or if the value argument is of
     * incorrect type
     */
    @Override
    public StoredProcedureQuery setParameter(int position, Date value, TemporalType temporalType) {
        entityManager.verifyOpenWithSetRollbackOnly();
        return setParameter(position, convertTemporalType(value, temporalType));
    }

    /**
     * Bind an argument to a positional parameter.
     *
     * @return the same query instance
     * @throws IllegalArgumentException if position does not correspond to a
     * positional parameter of the query or if the argument is of incorrect type
     */
    @Override
    public StoredProcedureQuery setParameter(int position, Object value) {
        try {
            entityManager.verifyOpen();
            setParameterInternal(position, value);
            return this;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * Bind an instance of java.util.Calendar to a Parameter object.
     *
     * @return the same query instance
     * @throws IllegalArgumentException if the parameter does not correspond to
     * a parameter of the query
     */
    @Override
    public StoredProcedureQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
        if (param == null) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NULL_PARAMETER_PASSED_TO_SET_PARAMETER"));
        }
        //bug 402686: type validation
        String position = getParameterId(param);
        ParameterExpressionImpl parameter = (ParameterExpressionImpl) this.getInternalParameters().get(position);
        if (parameter == null ) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NO_PARAMETER_WITH_NAME", new Object[] { param.toString(), this.databaseQuery }));
        }
        if (!parameter.getParameterType().equals(param.getParameterType())) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("INCORRECT_PARAMETER_TYPE", new Object[] { position, param.getParameterType() }));
        }
        return this.setParameter(position, value, temporalType);
    }

    /**
     * Bind an instance of java.util.Date to a Parameter object.
     *
     * @return the same query instance
     * @throws IllegalArgumentException if the parameter does not correspond to
     * a parameter of the query
     */
    @Override
    public StoredProcedureQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
        if (param == null) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NULL_PARAMETER_PASSED_TO_SET_PARAMETER"));
        }
        //bug 402686: type validation
        String position = getParameterId(param);
        ParameterExpressionImpl parameter = (ParameterExpressionImpl) this.getInternalParameters().get(position);
        if (parameter == null ) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NO_PARAMETER_WITH_NAME", new Object[] { param.toString(), this.databaseQuery }));
        }
        if (!parameter.getParameterType().equals(param.getParameterType())) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("INCORRECT_PARAMETER_TYPE", new Object[] { position, param.getParameterType() }));
        }
        return this.setParameter(position, value, temporalType);
    }

    /**
     * Bind the value of a Parameter object.
     *
     * @return the same query instance
     * @throws IllegalArgumentException if the parameter does not correspond to
     * a parameter of the query
     */
    @Override
    public <T> StoredProcedureQuery setParameter(Parameter<T> param, T value) {
        if (param == null) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NULL_PARAMETER_PASSED_TO_SET_PARAMETER"));
        }
        //bug 402686: type validation
        String position = getParameterId(param);
        ParameterExpressionImpl parameter = (ParameterExpressionImpl) this.getInternalParameters().get(position);
        if (parameter == null ) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NO_PARAMETER_WITH_NAME", new Object[] { param.toString(), this.databaseQuery }));
        }
        if (!parameter.getParameterType().equals(param.getParameterType())) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("INCORRECT_PARAMETER_TYPE", new Object[] { position, param.getParameterType() }));
        }
        return this.setParameter(position, value);
    }

    /**
     * Bind an instance of java.util.Calendar to a named parameter.
     *
     * @return the same query instance
     * @throws IllegalArgumentException if the parameter name does not
     * correspond to a parameter of the query or if the value argument is of
     * incorrect type
     */
    @Override
    public StoredProcedureQuery setParameter(String name, Calendar value, TemporalType temporalType) {
        entityManager.verifyOpenWithSetRollbackOnly();
        return setParameter(name, convertTemporalType(value, temporalType));
    }

    /**
     * Bind an instance of java.util.Date to a named parameter.
     *
     * @return the same query instance
     * @throws IllegalArgumentException if the parameter name does not
     * correspond to a parameter of the query or if the value argument is of
     * incorrect type
     */
    @Override
    public StoredProcedureQuery setParameter(String name, Date value, TemporalType temporalType) {
        entityManager.verifyOpenWithSetRollbackOnly();
        return setParameter(name, convertTemporalType(value, temporalType));
    }

    /**
     * Bind an argument to a named parameter.
     *
     * @return the same query instance
     * @throws IllegalArgumentException if the parameter name does not
     * correspond to a parameter of the query or if the argument is of incorrect
     * type
     */
    @Override
    public StoredProcedureQuery setParameter(String name, Object value) {
        try {
            entityManager.verifyOpen();
            setParameterInternal(name, value, false);
            return this;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * Bind an argument to a named or indexed parameter.
     *
     * @param name
     *            the parameter name
     * @param value
     *            to bind
     * @param isIndex
     *            defines if index or named
     */
    @Override
    protected void setParameterInternal(String name, Object value, boolean isIndex) {
        Parameter<?> parameter = this.getInternalParameters().get(name);
        StoredProcedureCall call = (StoredProcedureCall) getDatabaseQuery().getCall();
        if (parameter == null) {
            if (isIndex) {
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("ejb30-wrong-argument-index", new Object[] { name, call.getProcedureName() }));
            } else {
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("ejb30-wrong-argument-name", new Object[] { name, call.getProcedureName() }));
            }
        }
        if (!isValidActualParameter(value, parameter.getParameterType())) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("ejb30-incorrect-parameter-type", new Object[] { name, value.getClass(), parameter.getParameterType(), call.getProcedureName() }));
        }
        this.parameterValues.put(name, value);
    }
}

