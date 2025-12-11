/*
 * Copyright (c) 1998, 2025 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2025 IBM Corporation. All rights reserved.
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
//     08/11/2012-2.5 Guy Pelletier
//       - 393867: Named queries do not work when using EM level Table Per Tenant Multitenancy.
//     08/23/2023: Tomas Kraus
//       - New Jakarta Persistence 3.2 Features
package org.eclipse.persistence.internal.jpa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.Parameter;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;
import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.exceptions.QueryException;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.internal.databaseaccess.DatasourcePlatform;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.jpa.querydef.ParameterExpressionImpl;
import org.eclipse.persistence.internal.localization.ExceptionLocalization;
import org.eclipse.persistence.internal.queries.ContainerPolicy;
import org.eclipse.persistence.internal.queries.JPQLCallQueryMechanism;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.jpa.JpaQuery;
import org.eclipse.persistence.queries.Cursor;
import org.eclipse.persistence.queries.DataReadQuery;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.queries.JPAQueryBuilder;
import org.eclipse.persistence.queries.ModifyQuery;
import org.eclipse.persistence.queries.ObjectLevelReadQuery;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.ReadObjectQuery;
import org.eclipse.persistence.queries.ReadQuery;
import org.eclipse.persistence.queries.ReportQuery;
import org.eclipse.persistence.queries.ResultSetMappingQuery;
import org.eclipse.persistence.queries.SQLResultSetMapping;
import org.eclipse.persistence.sessions.DatabaseRecord;

/**
 * Concrete JPA query class. The JPA query wraps a DatabaseQuery which is
 * executed.
 */
public class EJBQueryImpl<X> extends QueryImpl implements JpaQuery<X> {
    /**
     * Base constructor for EJBQueryImpl. Initializes basic variables.
     */
    protected EJBQueryImpl(EntityManagerImpl entityManager) {
        super(entityManager);
    }

    /**
     * Create an EJBQueryImpl with a DatabaseQuery.
     */
    public EJBQueryImpl(DatabaseQuery query, EntityManagerImpl entityManager) {
        super(query, entityManager);
        // Inherit applicable hints from EntityManager
        inheritEntityManagerHints();
    }

    /**
     * Build an EJBQueryImpl based on the given jpql string.
     */
    public EJBQueryImpl(String jpql, EntityManagerImpl entityManager) {
        this(jpql, entityManager, false);
    }

    /**
     * Build an EJBQueryImpl based on the given jpql string and result class.
     */
    public EJBQueryImpl(String jpql, EntityManagerImpl entityManager, Class<?> resultClass) {
        this(jpql, entityManager, false, resultClass);
    }

    /**
     * Create an EJBQueryImpl with either a query name or an jpql string.
     *
     * @param queryDescription a Java Persistence query string
     * @param entityManager EntityManager instance
     * @param isNamedQuery
     *            determines whether to treat the queryDescription as jpql or a
     *            query name.
     */
    public EJBQueryImpl(String queryDescription, EntityManagerImpl entityManager, boolean isNamedQuery) {
        this(queryDescription, entityManager, isNamedQuery, null);
    }

    /**
     * Create an EJBQueryImpl with either a query name or an jpql string.
     *
     * @param queryDescription a Java Persistence query string
     * @param entityManager EntityManager instance
     * @param isNamedQuery
     *            determines whether to treat the queryDescription as jpql or a
     *            query name.
     * @param resultClass the type of the query result
     */
    public EJBQueryImpl(String queryDescription, EntityManagerImpl entityManager, boolean isNamedQuery, Class<?> resultClass) {
        super(entityManager);
        if (isNamedQuery) {
            this.queryName = queryDescription;
        } else {
            if (databaseQuery == null) {
                AbstractSession session = entityManager.getActiveSessionIfExists();
                databaseQuery = buildEJBQLDatabaseQuery(null, queryDescription, entityManager.getActiveSessionIfExists(), null, null, session.getDatasourcePlatform().getConversionManager().getLoader(), resultClass);
            }
        }
        // Inherit applicable hints from EntityManager
        inheritEntityManagerHints();
    }

    /**
     * Build a DatabaseQuery from an jpql string.
     *
     * @param session
     *            the session to get the descriptors for this query for.
     * @return a DatabaseQuery representing the given jpql.
     */
    public static DatabaseQuery buildEJBQLDatabaseQuery(String jpql, AbstractSession session) {
        return buildEJBQLDatabaseQuery(null, jpql, session, null, null, session.getDatasourcePlatform().getConversionManager().getLoader());
    }

    /**
     * Build a DatabaseQuery from an JPQL string.
     *
     * @param jpqlQuery
     *            the JPQL string.
     * @param session
     *            the session to get the descriptors for this query for.
     * @param hints
     *            a list of hints to be applied to the query.
     * @return a DatabaseQuery representing the given jpql.
     */
    public static DatabaseQuery buildEJBQLDatabaseQuery(String queryName, String jpqlQuery, AbstractSession session, Enum lockMode, Map<String, Object> hints, ClassLoader classLoader) {
        return buildEJBQLDatabaseQuery(queryName, jpqlQuery, session, lockMode, hints, classLoader, null);
    }

    /**
     * Build a DatabaseQuery from an JPQL string.
     *
     * @param jpqlQuery
     *            the JPQL string.
     * @param session
     *            the session to get the descriptors for this query for.
     * @param hints
     *            a list of hints to be applied to the query.
     * @param resultClass
     *            the type of the query result
     * @return a DatabaseQuery representing the given jpql.
     */
    public static DatabaseQuery buildEJBQLDatabaseQuery(String queryName, String jpqlQuery, AbstractSession session, Enum lockMode, Map<String, Object> hints, ClassLoader classLoader, Class<?> resultClass) {
        // PERF: Check if the JPQL has already been parsed.
        // Only allow queries with default properties to be parse cached.
        boolean isCacheable = (queryName == null) && (hints == null);
        DatabaseQuery databaseQuery = null;
        if (isCacheable) {
            databaseQuery = session.getProject().getJPQLParseCache().get(jpqlQuery);
        }
        if ((databaseQuery == null) || (!databaseQuery.isPrepared())) {
            JPAQueryBuilder queryBuilder = session.getQueryBuilder();
            databaseQuery = queryBuilder.buildQuery(jpqlQuery, session);

            // If the query uses fetch joins, need to use JPA default of not
            // filtering duplicates.
            if (databaseQuery.isReadAllQuery()) {
                ReadAllQuery readAllQuery = (ReadAllQuery) databaseQuery;
                if (readAllQuery.hasJoining() && (readAllQuery.getDistinctState() == ReadAllQuery.DONT_USE_DISTINCT)) {
                    readAllQuery.setShouldFilterDuplicates(false);
                }
                if (databaseQuery.isReportQuery()) {
                    ReportQuery reportQuery = (ReportQuery) databaseQuery;
                    reportQuery.setResultClass(resultClass);
                    Class<?> pkClass = reportQuery.getDescriptor().getCMPPolicy().getPKClass();
                    if (pkClass != null && pkClass == reportQuery.getResultClass() && reportQuery.hasIDFunctionSelectItemOnly()) {
                        reportQuery.setReturnPKClassInstance();
                    }
                }
            } else if (databaseQuery.isModifyQuery()) {
                // By default, do not batch modify queries, as row count must be returned.
                ((ModifyQuery)databaseQuery).setIsBatchExecutionSupported(false);
            }

            ((JPQLCallQueryMechanism) databaseQuery.getQueryMechanism()).getJPQLCall().setIsParsed(true);

            // Apply the lock mode.
            if (lockMode != null && !lockMode.name().equals(ObjectLevelReadQuery.NONE)) {
                if (databaseQuery.isObjectLevelReadQuery()) {
                    // If setting the lock mode returns true, we were unable to
                    // set the lock mode, throw an exception.
                    if (((ObjectLevelReadQuery) databaseQuery).setLockModeType(lockMode.name(), session)) {
                        throw new PersistenceException(ExceptionLocalization.buildMessage("ejb30-wrong-lock_called_without_version_locking-index", null));
                    }
                } else {
                    throw new IllegalArgumentException(ExceptionLocalization.buildMessage("invalid_lock_query", null));
                }
            }

            // Apply any query hints.
            databaseQuery = applyHints(hints, databaseQuery, classLoader, session);

            // If a primary key query, switch to read-object to allow cache hit.
            if (databaseQuery.isReadAllQuery() && !databaseQuery.isReportQuery() && ((ReadAllQuery)databaseQuery).shouldCheckCache()) {
                ReadAllQuery readQuery = (ReadAllQuery)databaseQuery;
                if ((readQuery.getContainerPolicy().getContainerClass() == ContainerPolicy.getDefaultContainerClass())
                        && (!readQuery.hasHierarchicalExpressions())) {
                    databaseQuery.checkDescriptor(session);
                    Expression selectionCriteria = databaseQuery.getSelectionCriteria();
                    if ((selectionCriteria != null)
                            && (databaseQuery.getDescriptor().getObjectBuilder().isPrimaryKeyExpression(true, selectionCriteria, session)
                            || (databaseQuery.getDescriptor().getCachePolicy().isIndexableExpression(selectionCriteria, databaseQuery.getDescriptor(), session)))) {
                        ReadObjectQuery newQuery = new ReadObjectQuery();
                        newQuery.copyFromQuery(databaseQuery);
                        databaseQuery = newQuery;
                    }
                }
            }

            if (isCacheable) {
                // Prepare query as hint may cause cloning (but not un-prepare
                // as in read-only).
                databaseQuery.checkPrepare(session, new DatabaseRecord());
                session.getProject().getJPQLParseCache().put(jpqlQuery, databaseQuery);
            }
        }

        return databaseQuery;
    }

    /**
     * Build a ReadAllQuery from a class and sql string.
     */
    public static DatabaseQuery buildSQLDatabaseQuery(Class<?> resultClass, String sqlString, ClassLoader classLoader, AbstractSession session) {
        return buildSQLDatabaseQuery(resultClass, sqlString, null, classLoader, session);
    }

    /**
     * Build a ReadAllQuery for class and sql string.
     *
     * @param hints
     *            a list of hints to be applied to the query.
     */
    public static DatabaseQuery buildSQLDatabaseQuery(Class<?> resultClass, String sqlString, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        ReadAllQuery query = new ReadAllQuery(resultClass);
        query.setCall(((DatasourcePlatform)session.getPlatform(resultClass)).buildNativeCall(sqlString));
        query.setIsUserDefined(true);

        // apply any query hints
        return applyHints(hints, query, classLoader, session);
    }

    /**
     * Build a DataReadQuery from a sql string.
     */
    public static DatabaseQuery buildSQLDatabaseQuery(String sqlString, ClassLoader classLoader, AbstractSession session) {
        return buildSQLDatabaseQuery(sqlString, new HashMap<>(), classLoader, session);
    }

    /**
     * Build a DataReadQuery from a sql string.
     */
    public static DatabaseQuery buildSQLDatabaseQuery(String sqlString, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        DataReadQuery query = new DataReadQuery();
        query.setResultType(DataReadQuery.AUTO);
        query.setSQLString(sqlString);
        query.setIsUserDefined(true);

        // apply any query hints
        return applyHints(hints, query, classLoader, session);
    }

    /**
     * Build a ResultSetMappingQuery from a sql result set mapping name and sql
     * string.
     */
    public static DatabaseQuery buildSQLDatabaseQuery(String sqlResultSetMappingName, String sqlString, ClassLoader classLoader, AbstractSession session) {
        return buildSQLDatabaseQuery(sqlResultSetMappingName, sqlString, null, classLoader, session);
    }

    /**
     * Build a ResultSetMappingQuery from a sql result set mapping name and sql
     * string.
     *
     * @param hints
     *            a list of hints to be applied to the query.
     */
    public static DatabaseQuery buildSQLDatabaseQuery(String sqlResultSetMappingName, String sqlString, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        ResultSetMappingQuery query = new ResultSetMappingQuery();
        query.setSQLResultSetMappingName(sqlResultSetMappingName);
        query.setCall(((DatasourcePlatform)session.getDatasourcePlatform()).buildNativeCall(sqlString));
        query.setIsUserDefined(true);

        // apply any query hints
        return applyHints(hints, query, classLoader, session);
    }

    /**
     * Build a ResultSetMappingQuery from a sql result set mapping name and sql
     * string.
     *
     * @param hints
     *            a list of hints to be applied to the query.
     */
    public static DatabaseQuery buildSQLDatabaseQuery(SQLResultSetMapping sqlResultSetMapping, String sqlString, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        ResultSetMappingQuery query = new ResultSetMappingQuery();
        query.addSQLResultSetMapping(sqlResultSetMapping);
        query.setCall(((DatasourcePlatform)session.getDatasourcePlatform()).buildNativeCall(sqlString));
        query.setIsUserDefined(true);

        // apply any query hints
        return applyHints(hints, query, classLoader, session);
    }

    /**
     * Set an implementation-specific hint. If the hint name is not recognized,
     * it is silently ignored.
     *
     * @return the same query instance
     * @throws IllegalArgumentException
     *             if the second argument is not valid for the implementation
     */
    @Override
    public TypedQuery<X> setHint(String hintName, Object value) {
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
    @SuppressWarnings("unchecked")
    public EJBQueryImpl<X> setLockMode(LockModeType lockMode) {
        return (EJBQueryImpl<X>) super.setLockMode(lockMode);
    }


    // Based on EntityManagerImpl#getQueryHints(Object,OperationType)
    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        return FindOptionUtils.getCacheRetrieveMode(entityManager.getAbstractSession(), getDatabaseQuery().getProperties());
    }

    @Override
    public TypedQuery<X> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        FindOptionUtils.setCacheRetrieveMode(getDatabaseQuery().getProperties(), cacheRetrieveMode);
        setHint(QueryHints.CACHE_RETRIEVE_MODE, cacheRetrieveMode);
        return this;
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        return FindOptionUtils.getCacheStoreMode(entityManager.getAbstractSession(), getDatabaseQuery().getProperties());
    }

    @Override
    public TypedQuery<X> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        FindOptionUtils.setCacheStoreMode(getDatabaseQuery().getProperties(), cacheStoreMode);
        setHint(QueryHints.CACHE_STORE_MODE, cacheStoreMode);
        return this;
    }

    @Override
    public Integer getTimeout() {
        return FindOptionUtils.getTimeout(entityManager.getAbstractSession(), getDatabaseQuery().getProperties());
    }

    @Override
    public TypedQuery<X> setTimeout(Integer timeout) {
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
     * but is specifically designed for createQuery() operations.
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
     * Non-standard method to return results of a ReadQuery that has a
     * containerPolicy that returns objects as a collection rather than a List
     *
     * @return Collection of results
     */
    @Override
    public Collection getResultCollection() {
        // bug51411440: need to throw IllegalStateException if query
        // executed on closed em
        this.entityManager.verifyOpenWithSetRollbackOnly();
        setAsSQLReadQuery();
        propagateResultProperties();
        // bug:4297903, check container policy class and throw exception if its
        // not the right type
        DatabaseQuery query = getDatabaseQueryInternal();
        try {
            if (query.isReadAllQuery()) {
                Class<?> containerClass = ((ReadAllQuery) getDatabaseQueryInternal()).getContainerPolicy().getContainerClass();
                if (!Helper.classImplementsInterface(containerClass, ClassConstants.Collection_Class)) {
                    throw QueryException.invalidContainerClass(containerClass, ClassConstants.Collection_Class);
                }
            } else if (query.isReadObjectQuery()) {
                List<Object> resultList = new ArrayList<>();
                Object result = executeReadQuery();
                if (result != null) {
                    resultList.add(executeReadQuery());
                }
                return resultList;
            } else if (!query.isReadQuery()) {
                throw new IllegalStateException(ExceptionLocalization.buildMessage("incorrect_query_for_get_result_collection"));
            }

            return (Collection) executeReadQuery();
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
     * Non-standard method to return results of a ReadQuery that uses a Cursor.
     *
     * @return Cursor on results, either a CursoredStream, or ScrollableCursor
     */
    @Override
    public Cursor getResultCursor() {
        // bug51411440: need to throw IllegalStateException if query executed on closed em
        this.entityManager.verifyOpenWithSetRollbackOnly();
        try {
            setAsSQLReadQuery();
            propagateResultProperties();
            // bug:4297903, check container policy class and throw exception if its
            // not the right type
            if (getDatabaseQueryInternal() instanceof ReadAllQuery) {
                if (!((ReadAllQuery) getDatabaseQueryInternal()).getContainerPolicy().isCursorPolicy()) {
                    Class<?> containerClass = ((ReadAllQuery) getDatabaseQueryInternal()).getContainerPolicy().getContainerClass();
                    throw QueryException.invalidContainerClass(containerClass, Cursor.class);
                }
            } else if (getDatabaseQueryInternal() instanceof ReadObjectQuery) {
                // bug:4300879, no support for ReadObjectQuery if a collection is required
                throw QueryException.incorrectQueryObjectFound(getDatabaseQueryInternal(), ReadAllQuery.class);
            } else if (!(getDatabaseQueryInternal() instanceof ReadQuery)) {
                throw new IllegalStateException(ExceptionLocalization.buildMessage("incorrect_query_for_get_result_collection"));
            }

            Object result = executeReadQuery();
            return (Cursor) result;
        } catch (LockTimeoutException e) {
            throw e;
        } catch (PersistenceException | IllegalStateException exception) {
            setRollbackOnly();
            throw exception;
        } catch (RuntimeException exception) {
            setRollbackOnly();
            throw new PersistenceException(exception);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public X getSingleResult() {
        return (X) super.getSingleResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public X getSingleResultOrNull() {
        return (X) super.getSingleResultOrNull();
    }

    /**
     * Set the position of the first result to retrieve.
     *
     * @param startPosition
     *            position of the first result, numbered from 0
     * @return the same query instance
     */
    @Override
    public EJBQueryImpl setFirstResult(int startPosition) {
        return (EJBQueryImpl) super.setFirstResult(startPosition);
    }

    /**
     * Set the flush mode type to be used for the query execution.
     *
     */
    @Override
    public EJBQueryImpl setFlushMode(FlushModeType flushMode) {
        return (EJBQueryImpl) super.setFlushMode(flushMode);
    }

    /**
     * Set the maximum number of results to retrieve.
     *
     * @return the same query instance
     */
    @Override
    public EJBQueryImpl setMaxResults(int maxResult) {
        return (EJBQueryImpl) super.setMaxResults(maxResult);
    }

    /**
     * Bind an instance of java.util.Calendar to a positional parameter.
     *
     * @return the same query instance
     */
    @Override
    public TypedQuery setParameter(int position, Calendar value, TemporalType temporalType) {
        entityManager.verifyOpenWithSetRollbackOnly();
        return setParameter(position, convertTemporalType(value, temporalType));
    }

    /**
     * Bind an instance of java.util.Date to a positional parameter.
     *
     * @return the same query instance
     */
    @Override
    public TypedQuery setParameter(int position, Date value, TemporalType temporalType) {
        entityManager.verifyOpenWithSetRollbackOnly();
        return setParameter(position, convertTemporalType(value, temporalType));
    }

    /**
     * Bind an argument to a positional parameter.
     *
     * @return the same query instance
     */
    @Override
    public TypedQuery setParameter(int position, Object value) {
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
     * @throws IllegalArgumentException
     *             if position does not correspond to a parameter of the query
     */
    @Override
    public TypedQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
        entityManager.verifyOpenWithSetRollbackOnly();
        if (param == null)
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NULL_PARAMETER_PASSED_TO_SET_PARAMETER"));
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
     * @param param
     *            object
     * @return the same query instance
     * @throws IllegalArgumentException
     *             if position does not correspond to a parameter of the query
     */
    @Override
    public TypedQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
        if (param == null)
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NULL_PARAMETER_PASSED_TO_SET_PARAMETER"));
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
     * Set the value of a Parameter object.
     *
     * @param param
     *            parameter to be set
     * @param value
     *            parameter value
     * @return query instance
     * @throws IllegalArgumentException
     *             if parameter does not correspond to a parameter of the query
     */
    @Override
    public <T> TypedQuery setParameter(Parameter<T> param, T value) {
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
     */
    @Override
    public TypedQuery setParameter(String name, Calendar value, TemporalType temporalType) {
        entityManager.verifyOpenWithSetRollbackOnly();
        return setParameter(name, convertTemporalType(value, temporalType));
    }

    /**
     * Bind an instance of java.util.Date to a named parameter.
     *
     * @return the same query instance
     */
    @Override
    public TypedQuery setParameter(String name, Date value, TemporalType temporalType) {
        entityManager.verifyOpenWithSetRollbackOnly();
        return setParameter(name, convertTemporalType(value, temporalType));
    }

    /**
     * Bind an argument to a named parameter.
     *
     * @param name
     *            the parameter name
     * @return the same query instance
     */
    @Override
    public TypedQuery setParameter(String name, Object value) {
        try {
            entityManager.verifyOpen();
            setParameterInternal(name, value, false);
            return this;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + this.databaseQuery + ")";
    }
}
