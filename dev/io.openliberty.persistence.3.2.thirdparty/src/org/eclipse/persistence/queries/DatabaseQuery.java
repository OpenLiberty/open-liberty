/*
 * Copyright (c) 1998, 2024 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 1998, 2025 IBM Corporation. All rights reserved.
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
//     07/16/2009-2.0 Guy Pelletier
//       - 277039: JPA 2.0 Cache Usage Settings
//     10/15/2010-2.2 Guy Pelletier
//       - 322008: Improve usability of additional criteria applied to queries at the session/EM
//     10/29/2010-2.2 Michael O'Brien
//       - 325167: Make reserved # bind parameter char generic to enable native SQL pass through
//     04/01/2011-2.3 Guy Pelletier
//       - 337323: Multi-tenant with shared schema support (part 2)
//     05/24/2011-2.3 Guy Pelletier
//       - 345962: Join fetch query when using tenant discriminator column fails.
//     06/30/2011-2.3.1 Guy Pelletier
//       - 341940: Add disable/enable allowing native queries
//     07/13/2012-2.5 Guy Pelletier
//       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
//     11/05/2012-2.5 Guy Pelletier
//       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
//     08/11/2012-2.5 Guy Pelletier
//       - 393867: Named queries do not work when using EM level Table Per Tenant Multitenancy.
//     08/11/2014-2.5 Rick Curtis
//       - 440594: Tolerate invalid NamedQuery at EntityManager creation.
//     09/03/2015 - Will Dazey
//       - 456067 : Added support for defining query timeout units
package org.eclipse.persistence.queries;

import org.eclipse.persistence.config.ParameterDelimiterType;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.DescriptorQueryManager;
import org.eclipse.persistence.descriptors.partitioning.PartitioningPolicy;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.exceptions.EclipseLinkException;
import org.eclipse.persistence.exceptions.OptimisticLockException;
import org.eclipse.persistence.exceptions.QueryException;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.internal.databaseaccess.Accessor;
import org.eclipse.persistence.internal.databaseaccess.DatabaseCall;
import org.eclipse.persistence.internal.databaseaccess.DatasourcePlatform;
import org.eclipse.persistence.internal.expressions.SQLStatement;
import org.eclipse.persistence.internal.helper.ConversionManager;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.helper.InvalidObject;
import org.eclipse.persistence.internal.queries.CallQueryMechanism;
import org.eclipse.persistence.internal.queries.DatabaseQueryMechanism;
import org.eclipse.persistence.internal.queries.DatasourceCallQueryMechanism;
import org.eclipse.persistence.internal.queries.ExpressionQueryMechanism;
import org.eclipse.persistence.internal.queries.JPQLCallQueryMechanism;
import org.eclipse.persistence.internal.queries.StatementQueryMechanism;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.internal.sessions.UnitOfWorkImpl;
import org.eclipse.persistence.internal.sessions.remote.RemoteSessionController;
import org.eclipse.persistence.internal.sessions.remote.Transporter;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.sessions.DataRecord;
import org.eclipse.persistence.sessions.DatabaseRecord;
import org.eclipse.persistence.sessions.SessionProfiler;
import org.eclipse.persistence.sessions.remote.DistributedSession;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * <b>Purpose</b>: Abstract class for all database query objects. DatabaseQuery
 * is a visible class to the EclipseLink user. Users create an appropriate query
 * by creating an instance of a concrete subclasses of DatabaseQuery.
 *
 * <p>
 * <b>Responsibilities</b>:
 * <ul>
 * <li>Provide a common protocol for query objects.
 * <li>Defines a generic execution interface.
 * <li>Provides query property values
 * <li>Holds arguments to the query
 * </ul>
 *
 * @author Yvon Lavoie
 * @since TOPLink/Java 1.0
 */
public abstract class DatabaseQuery implements Cloneable, Serializable {
    /** INTERNAL: Property used for batch fetching in non object queries. */
    public static final String BATCH_FETCH_PROPERTY = "BATCH_FETCH_PROPERTY";

    /**
     * Queries can be given a name and registered with a descriptor to allow
     * common queries to be reused.
     */
    protected String name;

    /**
     * Arguments can be given and specified to predefined queries to allow
     * reuse.
     */
    protected List<String> arguments;

    /**
     * PERF: Argument fields are cached in prepare to avoid rebuilding on each
     * execution.
     */
    protected List<DatabaseField> argumentFields;

    /**
     * Arguments values can be given and specified to predefined queries to
     * allow reuse.
     */
    protected List<Object> argumentValues;

    /** Needed to differentiate queries with the same name. */
    protected List<Class<?>> argumentTypes;

    /** Used to build a list of argumentTypes by name pre-initialization */
    protected List<String> argumentTypeNames;

    /** Used for parameter retreival in JPQL **/
    public enum ParameterType {POSITIONAL, NAMED}

    protected List<ParameterType> argumentParameterTypes;

    /** The descriptor cached on the prepare for object level queries. */
    protected transient ClassDescriptor descriptor;

    /** The list of descriptors this query deals with. Set via JPA processing for table per tenant queries */
    protected List<ClassDescriptor> descriptors;

    /**
     * The query mechanism determines the mechanism on how the database will be
     * accessed.
     */
    protected DatabaseQueryMechanism queryMechanism;

    /**
     * A redirector allows for a queries execution to be the execution of a
     * piece of code.
     */
    protected QueryRedirector redirector;

    /**
     * Can be set to true in the case there is a redirector or a default
     * redirector but the user does not want the query redirected.
     */
    protected boolean doNotRedirect = false;

    /** Flag used for a query to bypass the identitymap and unit of work. */

    // Bug#3476483 - Restore shouldMaintainCache to previous state after reverse
    // of bug fix 3240668
    protected boolean shouldMaintainCache;

    /** JPA flags to control the shared cache */
    protected boolean shouldRetrieveBypassCache = false;
    protected boolean shouldStoreBypassCache = false;

    /**
     * Property used to override a persistence unit level that disallows native
     * SQL queries.
     * @see org.eclipse.persistence.sessions.Project#setAllowNativeSQLQueries(boolean) Project.setAllowNativeSQLQueries(boolean)
     */
    protected Boolean allowNativeSQLQuery;

    /** Internally used by the mappings as a temporary store. */
    protected Map<Object, Object> properties;

    /**
     * Only used after the query is cloned for execution to store the session
     * under which the query was executed.
     */
    protected transient AbstractSession session;

    /**
     * Only used after the query is cloned for execution to store the execution
     * session under which the query was executed.
     */
    protected transient AbstractSession executionSession;

    /**
     * Connection to use for database access, required for server session
     * connection pooling.
     * There can be multiple connections with partitioning and replication.
     */
    protected transient Collection<Accessor> accessors;

    /**
     * Mappings and the descriptor use parameterized mechanisms that will be
     * translated with the data from the row.
     */
    protected AbstractRecord translationRow;

    /**
     * Internal flag used to bypass user define queries when executing one for
     * custom sql/query support.
     */
    protected boolean isUserDefined;

    /**
     * Internal flag used to bypass user define queries when executing one for
     * custom sql/query support.
     */
    protected boolean isUserDefinedSQLCall;

    /** Policy that determines how the query will cascade to its object's parts. */
    protected int cascadePolicy;

    /** Used to override the default session in the session broker. */
    protected String sessionName;

    /** Queries prepare common stated in themselves. */
    protected boolean isPrepared;

    /** Used to indicate whether or not the call needs to be cloned. */
    protected boolean shouldCloneCall;

    /**
     * Allow for the prepare of queries to be turned off, this allow for dynamic
     * non-pre SQL generated queries.
     */
    protected boolean shouldPrepare;

    /**
     * List of arguments to check for null.
     * If any are null, the query needs to be re-prepared.
     */
    protected List<DatabaseField> nullableArguments;

    /** Bind all arguments to the SQL statement. */

    // Has False, Undefined or True value. In case of Undefined -
    // Session's shouldBindAllParameters() defines whether to bind or not.
    protected Boolean shouldBindAllParameters;

    /**
     * Cache the prepared statement, this requires full parameter binding as
     * well.
     */

    // Has False, Undefined or True value. In case of Undefined -
    // Session's shouldCacheAllStatements() defines whether to cache or not.
    protected Boolean shouldCacheStatement;

    /** Use the WrapperPolicy for the objects returned by the query */
    protected boolean shouldUseWrapperPolicy;

    /**
     * Table per class requires multiple query executions. Internally we prepare
     * those queries and cache them against the source mapping's selection
     * query. When queries are executed they are cloned so we need a mechanism
     * to keep a reference back to the actual selection query so that we can
     * successfully look up and chain query executions within a table per class
     * inheritance hierarchy.
     */
    protected DatabaseMapping sourceMapping;

    /**
     * queryTimeout has three possible settings: DefaultTimeout, NoTimeout, and
     * 1..N This applies to both DatabaseQuery.queryTimeout and
     * DescriptorQueryManager.queryTimeout
     * <p>
     * DatabaseQuery.queryTimeout: - DefaultTimeout: get queryTimeout from
     * DescriptorQueryManager - NoTimeout, 1..N: overrides queryTimeout in
     * DescriptorQueryManager
     * <p>
     * DescriptorQueryManager.queryTimeout: - DefaultTimeout: get queryTimeout
     * from parent DescriptorQueryManager. If there is no parent, default to
     * NoTimeout - NoTimeout, 1..N: overrides parent queryTimeout
     */
    protected int queryTimeout;

    protected TimeUnit queryTimeoutUnit;

    protected boolean shouldReturnGeneratedKeys;

    /* Used as default for read, means shallow write for modify. */
    public static final int NoCascading = 1;

    /*
     * Used as default for write, used for refreshing to refresh the whole
     * object.
     */
    public static final int CascadePrivateParts = 2;

    /*
     * Currently not supported, used for deep write/refreshes/reads in the
     * future.
     */
    public static final int CascadeAllParts = 3;

    /* Used by the unit of work. */
    public static final int CascadeDependentParts = 4;

    /*
     * Used by aggregate Collections: As aggregates delete at update time,
     * cascaded deletes must know to stop when entering postDelete for a
     * particular mapping. Only used by the aggregate collection when update is
     * occurring in a UnitOfWork CR 2811
     */
    public static final int CascadeAggregateDelete = 5;

    /*
     * Used when refreshing should check the mappings to determine if a
     * particular mapping should be cascaded.
     */
    public static final int CascadeByMapping = 6;

    /** Used for adding hints to the query string in oracle */
    protected String hintString;

    /*
     * Stores the FlushMode of this Query. This is only applicable when executed
     * in a flushable UnitOfWork and will be ignored otherwise.
     */
    protected Boolean flushOnExecute;

    /**
     * PERF: Determines if the query has already been cloned for execution, to
     * avoid duplicate cloning.
     */
    protected boolean isExecutionClone;

    /** PERF: Store if this query will use the descriptor custom query. */
    protected volatile Boolean isCustomQueryUsed;

    /** Allow connection unwrapping to be configured. */
    protected boolean isNativeConnectionRequired;

    /**
     * Return the name to use for the query in performance monitoring.
     */
    protected transient String monitorName;

    /** Allow additional validation to be performed before using the update call cache */
    protected boolean shouldValidateUpdateCallCacheUse;

    /** Allow queries to be targeted at specific connection pools. */
    protected PartitioningPolicy partitioningPolicy;


    /** Allow the reserved pound char used to delimit bind parameters to be overridden */
    protected String parameterDelimiter;

    /**
     * PUBLIC: Initialize the state of the query
     */
    protected DatabaseQuery() {
        this.shouldMaintainCache = true;
        // bug 3524620: lazy-init query mechanism
        // this.queryMechanism = new ExpressionQueryMechanism(this);
        this.isUserDefined = false;
        this.cascadePolicy = NoCascading;
        this.isPrepared = false;
        this.shouldUseWrapperPolicy = true;
        this.queryTimeout = DescriptorQueryManager.DefaultTimeout;
        this.queryTimeoutUnit = DescriptorQueryManager.DefaultTimeoutUnit;
        this.shouldPrepare = true;
        this.shouldCloneCall = false;
        this.shouldBindAllParameters = null;
        this.shouldCacheStatement = null;
        this.isExecutionClone = false;
        this.shouldValidateUpdateCallCacheUse = false;
        this.parameterDelimiter = ParameterDelimiterType.DEFAULT;
    }

    /**
     * PUBLIC:
     * Return the query's partitioning policy.
     */
    public PartitioningPolicy getPartitioningPolicy() {
        return partitioningPolicy;
    }

    /**
     * PUBLIC:
     * Set the query's partitioning policy.
     * A PartitioningPolicy is used to partition, load-balance or replicate data across multiple difference databases
     * or across a database cluster such as Oracle RAC.
     * Partitioning can provide improved scalability by allowing multiple database machines to service requests.
     * Setting a policy on a query will override the descriptor and session defaults.
     */
    public void setPartitioningPolicy(PartitioningPolicy partitioningPolicy) {
        this.partitioningPolicy = partitioningPolicy;
    }

    /**
     * INTERNAL:
     * Return the name to use for the query in performance monitoring.
     */
    public String getMonitorName() {
        if (monitorName == null) {
            resetMonitorName();
        }
        return monitorName;
    }

    /**
     * INTERNAL:
     * Return the name to use for the query in performance monitoring.
     */
    public void resetMonitorName() {
        if (getReferenceClassName() == null) {
            this.monitorName = getClass().getSimpleName() + ":" + getName();
        } else {
            this.monitorName = getClass().getSimpleName() + ":" + getReferenceClassName() + ":" + getName();
        }
    }

    /**
     * PUBLIC: Add the argument named argumentName. This will cause the
     * translation of references of argumentName in the receiver's expression,
     * with the value of the argument as supplied to the query in order from
     * executeQuery()
     */
    public void addArgument(String argumentName) {
        addArgument(argumentName, Object.class);
    }

    /**
     * PUBLIC: Add the argument named argumentName and its class type. This will
     * cause the translation of references of argumentName in the receiver's
     * expression, with the value of the argument as supplied to the query in
     * order from executeQuery(). Specifying the class type is important if
     * identically named queries are used but with different argument lists.
     */
    public void addArgument(String argumentName, Class<?> type) {
        addArgument(argumentName, type, false);
    }

    /**
     * INTERNAL: Add the argument named argumentName.  This method was added to maintain
     * information about whether parameters are positional or named for JPQL query introspeciton
     * API
     */
    public void addArgument(String argumentName, Class<?> type, ParameterType parameterType) {
        addArgument(argumentName, type, parameterType, false);
    }

    /**
     * PUBLIC: Add the argument named argumentName and its class type. This will
     * cause the translation of references of argumentName in the receiver's
     * expression, with the value of the argument as supplied to the query in
     * order from executeQuery(). Specifying the class type is important if
     * identically named queries are used but with different argument lists.
     * If the argument can be null, and null must be treated differently in the
     * generated SQL, then nullable should be set to true.
     */
    public void addArgument(String argumentName, Class<?> type, boolean nullable) {
        getArguments().add(argumentName);
        getArgumentTypes().add(type);
        if(type != null) {
            getArgumentTypeNames().add(type.getName());
        }
        if (nullable) {
            getNullableArguments().add(new DatabaseField(argumentName));
        }
    }

    /**
     * INTERNAL: Add the argument named argumentName.  This method was added to maintain
     * information about whether parameters are positional or named for JPQL query introspeciton
     * API
     */
    public void addArgument(String argumentName, Class<?> type, ParameterType argumentParameterType, boolean nullable) {
        addArgument(argumentName, type, nullable);
        getArgumentParameterTypes().add(argumentParameterType);
    }

    /**
     * PUBLIC: Add the argument named argumentName and its class type. This will
     * cause the translation of references of argumentName in the receiver's
     * expression, with the value of the argument as supplied to the query in
     * order from executeQuery(). Specifying the class type is important if
     * identically named queries are used but with different argument lists.
     */
    public void addArgument(String argumentName, String typeAsString) {
        getArguments().add(argumentName);
        // bug 3197587
        getArgumentTypes().add(Helper.getObjectClass(ConversionManager.loadClass(typeAsString)));
        getArgumentTypeNames().add(typeAsString);
    }

    /**
     * INTERNAL: Add an argument to the query, but do not resolve the class yet.
     * This is useful for building a query without putting the domain classes on
     * the classpath for the Mapping Workbench.
     */
    public void addArgumentByTypeName(String argumentName, String typeAsString) {
        getArguments().add(argumentName);
        getArgumentTypeNames().add(typeAsString);
    }

    /**
     * PUBLIC: Add the argumentValue. Argument values must be added in the same
     * order the arguments are defined.
     */
    public void addArgumentValue(Object argumentValue) {
        getArgumentValues().add(argumentValue);
    }

    /**
     * PUBLIC: Add the argumentValues to the query. Argument values must be
     * added in the same order the arguments are defined.
     */
    public void addArgumentValues(List theArgumentValues) {
        getArgumentValues().addAll(theArgumentValues);
    }

    /**
     * PUBLIC: Used to define a store procedure or SQL query. This may be used
     * for multiple SQL executions to be mapped to a single query. This cannot
     * be used for cursored selects, delete alls or does exists.
     */
    public void addCall(Call call) {
        setQueryMechanism(call.buildQueryMechanism(this, getQueryMechanism()));
        // Must un-prepare is prepare as the SQL may change.
        setIsPrepared(false);
    }

    /**
     * PUBLIC: Used to define a statement level query. This may be used for
     * multiple SQL executions to be mapped to a single query. This cannot be
     * used for cursored selects, delete all(s) or does exists.
     */
    public void addStatement(SQLStatement statement) {
        // bug 3524620: lazy-init query mechanism
        if (!hasQueryMechanism()) {
            setQueryMechanism(new StatementQueryMechanism(this));
        } else if (!getQueryMechanism().isStatementQueryMechanism()) {
            setQueryMechanism(new StatementQueryMechanism(this));
        }
        ((StatementQueryMechanism) getQueryMechanism()).getSQLStatements().add(statement);
        // Must un-prepare is prepare as the SQL may change.
        setIsPrepared(false);
    }

    /**
     * PUBLIC: Bind all arguments to any SQL statement.
     */
    public void bindAllParameters() {
        setShouldBindAllParameters(true);
    }

    /**
     * INTERNAL: In the case of EJBQL, an expression needs to be generated.
     * Build the required expression.
     */
    protected void buildSelectionCriteria(AbstractSession session) {
        this.getQueryMechanism().buildSelectionCriteria(session);
    }

    /**
     * PUBLIC: Cache the prepared statements, this requires full parameter
     * binding as well.
     */
    public void cacheStatement() {
        setShouldCacheStatement(true);
    }

    /**
     * PUBLIC: Cascade the query and its properties on the queries object(s) and
     * all objects related to the queries object(s). This includes private and
     * independent relationships, but not read-only relationships. This will
     * still stop on uninstantiated indirection objects except for deletion.
     * Great caution should be used in using the property as the query may
     * effect a large number of objects. This policy is used by the unit of work
     * to ensure persistence by reachability.
     */
    public void cascadeAllParts() {
        setCascadePolicy(CascadeAllParts);
    }

    /**
     * PUBLIC: Cascade the query and its properties on the queries object(s) and
     * all related objects where the mapping has been set to cascade the merge.
     */
    public void cascadeByMapping() {
        setCascadePolicy(CascadeByMapping);
    }

    /**
     * INTERNAL: Used by unit of work, only cascades constraint dependencies.
     */
    public void cascadeOnlyDependentParts() {
        setCascadePolicy(CascadeDependentParts);
    }

    /**
     * PUBLIC: Cascade the query and its properties on the queries object(s) and
     * all privately owned objects related to the queries object(s). This is the
     * default for write and delete queries. This policy should normally be used
     * for refreshing, otherwise you could refresh half of any object.
     */
    public void cascadePrivateParts() {
        setCascadePolicy(CascadePrivateParts);
    }

    /**
     * INTERNAL: Ensure that the descriptor has been set.
     */
    public void checkDescriptor(AbstractSession session) throws QueryException {
    }

    /**
     * INTERNAL: Check to see if this query already knows the return value
     * without performing any further work.
     */
    public Object checkEarlyReturn(AbstractSession session, AbstractRecord translationRow) {
        return null;
    }

    /**
     * INTERNAL: Check to see if a custom query should be used for this query.
     * This is done before the query is copied and prepared/executed. null means
     * there is none.
     */
    protected DatabaseQuery checkForCustomQuery(AbstractSession session, AbstractRecord translationRow) {
        return null;
    }

    /**
     * INTERNAL: Check to see if this query needs to be prepare and prepare it.
     * The prepare is done on the original query to ensure that the work is not
     * repeated.
     */
    public void checkPrepare(AbstractSession session, AbstractRecord translationRow) {
        this.checkPrepare(session, translationRow, false);
    }

    /**
     * INTERNAL: Call the prepare on the query.
     */
    public void prepareInternal(AbstractSession session) {
        setSession(session);
        try {
            prepare();
        } finally {
            setSession(null);
        }
    }

    /**
     * INTERNAL: Check to see if this query needs to be prepare and prepare it.
     * The prepare is done on the original query to ensure that the work is not
     * repeated.
     */
    public void checkPrepare(AbstractSession session, AbstractRecord translationRow, boolean force) {
        try {
            // This query is first prepared for global common state, this must be synced.
            if (!this.isPrepared) {// Avoid the monitor is already prepare, must
                // If this query will use the custom query, do not prepare.
                if ((!force) && (!this.shouldPrepare || !((DatasourcePlatform)session.getDatasourcePlatform()).shouldPrepare(this)
                        || (checkForCustomQuery(session, translationRow) != null))) {
                    return;
                }
                // check again for concurrency.
                // Profile the query preparation time.
                session.startOperationProfile(SessionProfiler.QueryPreparation, this, SessionProfiler.ALL);
                // Prepared queries cannot be custom as then they would never have
                // been prepared.
                synchronized (this) {
                    if (!isPrepared()) {
                        // When custom SQL is used there is a possibility that the
                        // SQL contains the # token (for stored procedures or temporary tables).
                        // Avoid this by telling the call if this is custom SQL with parameters.
                        // This must not be called for SDK calls.
                        if ((isReadQuery() || isDataModifyQuery()) && isCallQuery() && (getQueryMechanism() instanceof CallQueryMechanism)
                                && ((translationRow == null) || (translationRow.isEmpty() && !translationRow.hasSopObject()))) {
                            // Must check for read object queries as the row will be
                            // empty until the prepare.
                            if (isReadObjectQuery() || isUserDefined()) {
                                ((CallQueryMechanism) getQueryMechanism()).setCallHasCustomSQLArguments();
                            }
                        } else if (isCallQuery() && (getQueryMechanism() instanceof CallQueryMechanism)) {
                            ((CallQueryMechanism) getQueryMechanism()).setCallHasCustomSQLArguments();
                        }
                        setSession(session);// Session is required for some init stuff.
                        prepare();
                        setSession(null);
                        setIsPrepared(true);// MUST not set prepare until done as
                        // other thread may hit before finishing the prepare.
                    }
                }
                // Profile the query preparation time.
                session.endOperationProfile(SessionProfiler.QueryPreparation, this, SessionProfiler.ALL);
            }
        } catch (QueryException knownFailure) {
            // Set the query, as prepare can be called directly.
            if (knownFailure.getQuery() == null) {
                knownFailure.setQuery(this);
                knownFailure.setSession(session);
            }
            throw knownFailure;
        } catch (EclipseLinkException knownFailure) {
            throw knownFailure;
        } catch (RuntimeException unexpectedFailure) {
            throw QueryException.prepareFailed(unexpectedFailure, this);
        }
    }

    /**
     * INTERNAL: Clone the query
     */
    @Override
    public Object clone() {
        try {
            DatabaseQuery cloneQuery = (DatabaseQuery) super.clone();

            // partial fix for 3054240
            // need to pay attention to other components of the query, too MWN
            if (cloneQuery.properties != null) {
                if (cloneQuery.properties.isEmpty()) {
                    cloneQuery.properties = null;
                } else {
                    cloneQuery.properties = new HashMap<>(this.properties);
                }
            }

            // bug 3524620: now that the query mechanism is lazy-init'd,
            // only clone the query mechanism if we have one.
            if (this.queryMechanism != null) {
                cloneQuery.queryMechanism = this.queryMechanism.clone(cloneQuery);
            }
            cloneQuery.isPrepared = this.isPrepared; // Setting some things may trigger unprepare.
            // JPA 3.2 cache bypass flags must be preserved in clones
            cloneQuery.shouldRetrieveBypassCache = this.shouldRetrieveBypassCache;
            cloneQuery.shouldStoreBypassCache = this.shouldStoreBypassCache;
            return cloneQuery;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /**
     * INTERNAL Used to give the subclasses opportunity to copy aspects of the
     * cloned query to the original query.
     */
    protected void clonedQueryExecutionComplete(DatabaseQuery query, AbstractSession session) {
        // no-op for this class
    }

    /**
     * INTERNAL: Convert all the class-name-based settings in this query to
     * actual class-based settings This method is implemented by subclasses as
     * necessary.
     *
     */
    public void convertClassNamesToClasses(ClassLoader classLoader) {
        // note: normally we would fix the argument types here, but they are
        // already
        // lazily instantiated
    }

    /**
     * PUBLIC: Do not Bind all arguments to any SQL statement.
     */
    public void dontBindAllParameters() {
        setShouldBindAllParameters(false);
    }

    /**
     * PUBLIC: Don't cache the prepared statements, this requires full parameter
     * binding as well.
     */
    public void dontCacheStatement() {
        setShouldCacheStatement(false);
    }

    /**
     * PUBLIC: Do not cascade the query and its properties on the queries
     * object(s) relationships. This does not effect the queries private parts
     * but only the object(s) direct row-level attributes. This is the default
     * for read queries and can be used in writing if it is known that only
     * row-level attributes changed, or to resolve circular foreign key
     * dependencies.
     */
    public void dontCascadeParts() {
        setCascadePolicy(NoCascading);
    }

    /**
     * PUBLIC: Set for the identity map (cache) to be ignored completely. The
     * cache check will be skipped and the result will not be put into the
     * identity map. This can be used to retrieve the exact state of an object
     * on the database. By default the identity map is always maintained.
     */
    public void dontMaintainCache() {
        setShouldMaintainCache(false);
    }

    /**
     * INTERNAL: Execute the query
     *
     * @exception DatabaseException
     *                - an error has occurred on the database.
     * @exception OptimisticLockException
     *                - an error has occurred using the optimistic lock feature.
     * @return - the result of executing the query.
     */
    public abstract Object executeDatabaseQuery() throws DatabaseException, OptimisticLockException;

    /**
     * INTERNAL: Override query execution where Session is a UnitOfWork.
     * <p>
     * If there are objects in the cache return the results of the cache lookup.
     *
     * @param unitOfWork
     *            - the session in which the receiver will be executed.
     * @param translationRow
     *            - the arguments
     * @exception DatabaseException
     *                - an error has occurred on the database.
     * @exception OptimisticLockException
     *                - an error has occurred using the optimistic lock feature.
     * @return An object, the result of executing the query.
     */
    public Object executeInUnitOfWork(UnitOfWorkImpl unitOfWork, AbstractRecord translationRow) throws DatabaseException, OptimisticLockException {
        return execute(unitOfWork, translationRow);
    }

    /**
     * INTERNAL: Execute the query. If there are objects in the cache return the
     * results of the cache lookup.
     *
     * @param session
     *            - the session in which the receiver will be executed.
     * @exception DatabaseException
     *                - an error has occurred on the database.
     * @exception OptimisticLockException
     *                - an error has occurred using the optimistic lock feature.
     * @return An object, the result of executing the query.
     */
    public Object execute(AbstractSession session, AbstractRecord translationRow) throws DatabaseException, OptimisticLockException {
        DatabaseQuery queryToExecute = this;
        // JPQL call may not have defined the reference class yet, so need to use prepare.
        if (isJPQLCallQuery() && isObjectLevelReadQuery()) {
            ((ObjectLevelReadQuery)this).checkPrePrepare(session);
        } else {
            checkDescriptor(session);
        }
        QueryRedirector localRedirector = getRedirectorForQuery();
        // refactored redirection for bug 3241138
        if (localRedirector != null) {
            return redirectQuery(localRedirector, queryToExecute, session, translationRow);
        }

        // Bug 5529564 - If this is a user defined selection query (custom SQL),
        // prepare the query before hand so that we may look up the correct fk
        // values from the query parameters when checking early return.
        if (queryToExecute.isCustomSelectionQuery() && queryToExecute.shouldPrepare()) {
            queryToExecute.checkPrepare(session, translationRow);
        }

        // This allows the query to check the cache or return early without
        // doing any work.
        Object earlyReturn = queryToExecute.checkEarlyReturn(session, translationRow);
        // If know not to exist (checkCacheOnly, deleted, null primary key),
        // return null.
        if (earlyReturn == InvalidObject.instance) {
            return null;
        }
        if (earlyReturn != null) {
            return earlyReturn;
        }

        boolean hasCustomQuery = false;
        if (!isPrepared() && shouldPrepare()) {
            // Prepared queries cannot be custom as then they would never have
            // been prepared.
            DatabaseQuery customQuery = checkForCustomQuery(session, translationRow);
            if (customQuery != null) {
                hasCustomQuery = true;
                // The custom query will be used not the original.
                queryToExecute = customQuery;
            }
        }

        // PERF: Queries need to be cloned for execution as they may be
        // concurrently reused, and execution specific state is stored in the
        // clone.
        // In some case the query is known to be a one off, or cloned elsewhere
        // so the query keeps track if it has been cloned already.
        queryToExecute = session.prepareDatabaseQuery(queryToExecute);

        boolean prepare = queryToExecute.shouldPrepare(translationRow, session);
        if (prepare) {
            queryToExecute.checkPrepare(session, translationRow);
        }

        // Then cloned for concurrency and repeatable execution.
        if (!queryToExecute.isExecutionClone()) {
            queryToExecute = (DatabaseQuery) queryToExecute.clone();
        }
        // Check for query argument values.
        if ((this.argumentValues != null) && (!this.argumentValues.isEmpty()) && translationRow.isEmpty()) {
            translationRow = rowFromArguments(this.argumentValues, session);
        }
        queryToExecute.setTranslationRow(translationRow);

        // If the prepare has been disable the clone is prepare dynamically to
        // not parameterize the SQL.
        if (!prepare) {
            queryToExecute.setIsPrepared(false);
            queryToExecute.setTranslationRow(translationRow);
            queryToExecute.checkPrepare(session, translationRow, true);
        }
        queryToExecute.setSession(session);
        if (hasCustomQuery) {
            prepareCustomQuery(queryToExecute);
            localRedirector = queryToExecute.getRedirector();
            // refactored redirection for bug 3241138
            if (localRedirector != null) {
                return redirectQuery(localRedirector, queryToExecute, session, queryToExecute.getTranslationRow());
            }
        }
        queryToExecute.prepareForExecution();

        // Then executed.
        Object result = queryToExecute.executeDatabaseQuery();

        // Give the subclasses the opportunity to retrieve aspects of the cloned
        // query.
        clonedQueryExecutionComplete(queryToExecute, session);
        return result;
    }

    /**
     * INTERNAL: Extract the correct query result from the transporter.
     */
    public Object extractRemoteResult(Transporter transporter) {
        return transporter.getObject();
    }

    /**
     * INTERNAL: Return the accessor.
     */
    public Accessor getAccessor() {
        if ((this.accessors == null) || (this.accessors.isEmpty())) {
            return null;
        }
        if (this.accessors instanceof List) {
            return ((List<Accessor>)this.accessors).get(0);
        }
        return this.accessors.iterator().next();
    }

    /**
     * INTERNAL: Return the accessors.
     */
    public Collection<Accessor> getAccessors() {
        return this.accessors;
    }

    /**
     * INTERNAL: Return the arguments for use with the pre-defined query option
     */
    public List<String> getArguments() {
        if (this.arguments == null) {
            this.arguments = new ArrayList<>();
        }
        return this.arguments;
    }

    /**
     * INTERNAL:
     * Used to calculate parameter types in JPQL
     */
    public List<ParameterType> getArgumentParameterTypes(){
        if (argumentParameterTypes == null){
            this.argumentParameterTypes = new ArrayList<>();
        }
        return this.argumentParameterTypes;
    }

    /**
     * INTERNAL: Return the argumentTypes for use with the pre-defined query
     * option
     */
    public List<Class<?>> getArgumentTypes() {
        if ((this.argumentTypes == null) || (this.argumentTypes.isEmpty() && (this.argumentTypeNames != null) && !this.argumentTypeNames.isEmpty())) {
            this.argumentTypes = new ArrayList<>();
            // Bug 3256198 - lazily initialize the argument types from their
            // class names
            if (this.argumentTypeNames != null) {
                Iterator<String> args = this.argumentTypeNames.iterator();
                while (args.hasNext()) {
                    String argumentTypeName = args.next();
                    this.argumentTypes.add(Helper.getObjectClass(ConversionManager.loadClass(argumentTypeName)));
                }
            }
        }
        return this.argumentTypes;
    }

    /**
     * INTERNAL: Return the argumentTypeNames for use with the pre-defined query
     * option These are used pre-initialization to construct the argumentTypes
     * list.
     */
    public List<String> getArgumentTypeNames() {
        if (argumentTypeNames == null) {
            argumentTypeNames = new ArrayList<>();
        }
        return argumentTypeNames;
    }

    /**
     * INTERNAL: Set the argumentTypes for use with the pre-defined query option
     */
    public void setArgumentTypes(List<Class<?>> argumentTypes) {
        this.argumentTypes = argumentTypes;
        // bug 3256198 - ensure the list of type names matches the argument
        // types.
        getArgumentTypeNames().clear();
        for (Class<?> type : argumentTypes) {
            this.argumentTypeNames.add(type.getName());
        }
    }

    /**
     * INTERNAL: Set the argumentTypes for use with the pre-defined query option
     */
    public void setArgumentTypeNames(List<String> argumentTypeNames) {
        this.argumentTypeNames = argumentTypeNames;
    }

    /**
     * INTERNAL: Set the arguments for use with the pre-defined query option.
     * Maintain the argumentTypes as well.
     */
    public void setArguments(List<String> arguments) {
        List<Class<?>> types = new ArrayList<>(arguments.size());
        List<String> typeNames = new ArrayList<>(arguments.size());
        List<DatabaseField> typeFields = new ArrayList<>(arguments.size());
        int size = arguments.size();
        for (int index = 0; index < size; index++) {
            types.add(Object.class);
            typeNames.add("java.lang.Object");
            DatabaseField field = new DatabaseField(arguments.get(index));
            typeFields.add(field);
        }
        this.arguments = arguments;
        this.argumentTypes = types;
        this.argumentTypeNames = typeNames;
        this.argumentFields = typeFields;
    }

    /**
     * INTERNAL: Return the argumentValues for use with argumented queries.
     */
    public List<Object> getArgumentValues() {
        if (this.argumentValues == null) {
            this.argumentValues = new ArrayList<>();
        }
        return this.argumentValues;
    }

    /**
     * INTERNAL: Set the argumentValues for use with argumented queries.
     */
    public void setArgumentValues(List<Object> theArgumentValues) {
        this.argumentValues = theArgumentValues;
    }

    /**
     * OBSOLETE: Return the call for this query. This call contains the SQL and
     * argument list.
     *
     * @see #getDatasourceCall()
     */
    public DatabaseCall getCall() {
        Call call = getDatasourceCall();
        if (call instanceof DatabaseCall) {
            return (DatabaseCall) call;
        } else {
            return null;
        }
    }

    /**
     * ADVANCED: Return the call for this query. This call contains the SQL and
     * argument list.
     *
     * @see #prepareCall(org.eclipse.persistence.sessions.Session, DataRecord) prepareCall(Session, Record)
     */
    public Call getDatasourceCall() {
        Call call = null;
        if (this.queryMechanism instanceof DatasourceCallQueryMechanism mechanism) {
            call = mechanism.getCall();
            // If has multiple calls return the first one.
            if ((call == null) && mechanism.hasMultipleCalls()) {
                call = mechanism.getCalls().get(0);
            }
        }
        if ((call == null) && (this.queryMechanism != null) && this.queryMechanism.isJPQLCallQueryMechanism()) {
            call = ((JPQLCallQueryMechanism) this.queryMechanism).getJPQLCall();
        }
        return call;
    }

    /**
     * ADVANCED: Return the calls for this query. This method can be called for
     * queries with multiple calls This call contains the SQL and argument list.
     *
     * @see #prepareCall(org.eclipse.persistence.sessions.Session, DataRecord) prepareCall(Session, Record)
     */
    public List getDatasourceCalls() {
        List calls = new Vector();
        if (getQueryMechanism() instanceof DatasourceCallQueryMechanism mechanism) {

            // If has multiple calls return the first one.
            if (mechanism.hasMultipleCalls()) {
                calls = mechanism.getCalls();
            } else {
                calls.add(mechanism.getCall());
            }
        }
        if ((calls.isEmpty()) && getQueryMechanism().isJPQLCallQueryMechanism()) {
            calls.add(((JPQLCallQueryMechanism) getQueryMechanism()).getJPQLCall());
        }
        return calls;
    }

    /**
     * INTERNAL: Return the cascade policy.
     */
    public int getCascadePolicy() {
        return cascadePolicy;
    }

    /**
     * INTERNAL: Return the descriptor assigned with the reference class
     */
    public ClassDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * INTERNAL:
     * This is here only for JPA queries and currently only populated for JPA
     * queries. JPAQuery is a jpa class and currently not part of the core.
     */
    public List<ClassDescriptor> getDescriptors() {
        return null;
    }

    /**
     * INTERNAL:
     * TopLink_sessionName_domainClass.  Cached in properties
     */
     public String getDomainClassNounName(String sessionName) {
        if (getProperty("DMSDomainClassNounName") == null) {
            StringBuilder buffer = new StringBuilder("EclipseLink");
            if (sessionName != null) {
                buffer.append(sessionName);
            }
            if (getReferenceClassName() != null) {
                buffer.append("_");
                buffer.append(getReferenceClassName());
            }
            setProperty("DMSDomainClassNounName", buffer.toString());
        }
        return (String)getProperty("DMSDomainClassNounName");
     }

    /**
     * PUBLIC: Return the name of the query
     */
    public String getName() {
        return name;
    }

    /**
     * INTERNAL:
     * Return the String used to delimit an SQL parameter.
     */
    public String getParameterDelimiter() {
        if(null == parameterDelimiter || parameterDelimiter.isEmpty()) {
            parameterDelimiter = ParameterDelimiterType.DEFAULT;
        }
        return parameterDelimiter;
    }

    /**
     * INTERNAL:
     * Return the char used to delimit an SQL parameter.
     */
    public char getParameterDelimiterChar() {
        return getParameterDelimiter().charAt(0);
    }

    /**
     * INTERNAL: Property support for use by mappings.
     */
    public Map<Object, Object> getProperties() {
        if (this.properties == null) {
            // Lazy initialize to conserve space and allocation time.
            this.properties = new HashMap<>();
        }
        return this.properties;
    }

    /**
     * INTERNAL: Property support used by mappings to store temporary stuff in
     * the query.
     */
    public synchronized Object getProperty(Object property) {
        if (this.properties == null) {
            return null;
        }
        return this.properties.get(property);
    }

    /**
     * INTERNAL:
     * TopLink_sessionName_domainClass_queryClass_queryName (if exist).  Cached in properties
     */
    public String getQueryNounName(String sessionName) {
        if (getProperty("DMSQueryNounName") == null) {
            StringBuilder buffer = new StringBuilder(getDomainClassNounName(sessionName));
            buffer.append("_");
            buffer.append(getClass().getSimpleName());
            if (getName() != null) {
                buffer.append("_");
                buffer.append(getName());
            }
            setProperty("DMSQueryNounName", buffer.toString());
        }
        return (String)getProperty("DMSQueryNounName");
    }

    /**
     * INTERNAL: Return the mechanism assigned to the query
     */
    public DatabaseQueryMechanism getQueryMechanism() {
        // Bug 3524620 - lazy init
        if (this.queryMechanism == null) {
            this.queryMechanism = new ExpressionQueryMechanism(this);
        }
        return this.queryMechanism;
    }

    /**
     * INTERNAL: Check if the mechanism has been set yet, used for lazy init.
     */
    public boolean hasQueryMechanism() {
        return (this.queryMechanism != null);
    }

    /**
     * PUBLIC: Return the number of seconds the driver will wait for a Statement
     * to execute to the given number of seconds.
     *
     * @see DescriptorQueryManager#getQueryTimeout()
     */
    public int getQueryTimeout() {
        return queryTimeout;
    }

    /**
     * PUBLIC: Return the unit of time the driver will wait for a Statement to
     * execute.
     * 
     * @see DescriptorQueryManager#getQueryTimeoutUnit()
     */
    public TimeUnit getQueryTimeoutUnit() {
        return queryTimeoutUnit;
    }

    /**
     * INTERNAL: Returns the specific default redirector for this query type.
     * There are numerous default query redirectors. See ClassDescriptor for
     * their types.
     */
    protected QueryRedirector getDefaultRedirector() {
        return this.descriptor.getDefaultQueryRedirector();
    }

    /**
     * PUBLIC: Return the query redirector. A redirector can be used in a query
     * to replace its execution with the execution of code. This can be used for
     * named or parameterized queries to allow dynamic configuration of the
     * query base on the query arguments.
     *
     * @see QueryRedirector
     */
    public QueryRedirector getRedirectorForQuery() {
        if (doNotRedirect) {
            return null;
        }
        if (redirector != null) {
            return redirector;
        }
        if (descriptor != null) {
            return getDefaultRedirector();
        }
        return null;
    }

    /**
     * PUBLIC: Return the query redirector. A redirector can be used in a query
     * to replace its execution with the execution of code. This can be used for
     * named or parameterized queries to allow dynamic configuration of the
     * query base on the query arguments.
     *
     * @see QueryRedirector
     */
    public QueryRedirector getRedirector() {
        if (doNotRedirect) {
            return null;
        }
        return redirector;
    }

    /**
     * PUBLIC: Return the domain class associated with this query. By default
     * this is null, but should be overridden in subclasses.
     */
    public Class<?> getReferenceClass() {
        return null;
    }

    /**
     * INTERNAL: return the name of the reference class. Added for Mapping
     * Workbench removal of classpath dependency. Overridden by subclasses.
     */
    public String getReferenceClassName() {
        return null;
    }

    /**
     * PUBLIC: Return the selection criteria of the query. This should only be
     * used with expression queries, null will be returned for others.
     */
    public Expression getSelectionCriteria() {
        if (this.queryMechanism == null) {
            return null;
        }
        return this.queryMechanism.getSelectionCriteria();
    }

    /**
     * INTERNAL:
     * TopLink_sessionName_domainClass_queryClass_queryName (if exist)_operationName (if exist).  Cached in properties
     */
    public String getSensorName(String operationName, String sessionName) {
        if (operationName == null) {
            return getQueryNounName(sessionName);
        }
        @SuppressWarnings({"unchecked"})
        Hashtable<String, String> sensorNames = (Hashtable<String, String>) getProperty("DMSSensorNames");
        if (sensorNames == null) {
            sensorNames = new Hashtable<>();
            setProperty("DMSSensorNames", sensorNames);
        }
        String sensorName = sensorNames.get(operationName);
        if (sensorName == null) {
            StringBuilder buffer = new StringBuilder(getQueryNounName(sessionName));
            buffer.append("_");
            buffer.append(operationName);
            sensorName = buffer.toString();
            sensorNames.put(operationName, sensorName);
        }
        return sensorName;
    }

    /**
     * INTERNAL: Return the current session.
     */
    public AbstractSession getSession() {
        return session;
    }

    /**
     * INTERNAL: Return the execution session. This is the session used to build
     * objects returned by the query.
     */
    public AbstractSession getExecutionSession() {
        if (this.executionSession == null) {
            if (getSession() != null) {
                this.executionSession = getSession().getExecutionSession(this);
            }
        }
        return this.executionSession;
    }

    /**
     * INTERNAL: Set the execution session. This is the session used to build
     * objects returned by the query.
     */
    protected void setExecutionSession(AbstractSession executionSession) {
        this.executionSession = executionSession;
    }

    /**
     * PUBLIC: Return the name of the session that the query should be executed
     * under. This can be with the session broker to override the default
     * session.
     */
    public String getSessionName() {
        return sessionName;
    }

    /**
     * PUBLIC: Return the SQL statement of the query. This can only be used with
     * statement queries.
     */
    public SQLStatement getSQLStatement() {
        return ((StatementQueryMechanism) getQueryMechanism()).getSQLStatement();
    }

    /**
     * PUBLIC: Return the JPQL string of the query.
     */
    public String getJPQLString() {
        return getEJBQLString();
    }

    /**
     * PUBLIC: Return the EJBQL string of the query.
     */
    public String getEJBQLString() {
        if (!(getQueryMechanism().isJPQLCallQueryMechanism())) {
            return null;
        }
        JPQLCall call = ((JPQLCallQueryMechanism) getQueryMechanism()).getJPQLCall();
        return call.getEjbqlString();
    }

    /**
     * PUBLIC: Return the current database hint string of the query.
     */
    public String getHintString() {
        return hintString;
    }

    /**
     * ADVANCED: Return the SQL string of the query. This can be used for SQL
     * queries. This can also be used for normal queries if they have been
     * prepared, (i.e. query.prepareCall()).
     *
     * @see #prepareCall(org.eclipse.persistence.sessions.Session, DataRecord) prepareCall(Session, Record)
     */
    public String getSQLString() {
        Call call = getDatasourceCall();
        if (call == null) {
            return null;
        }
        if (!(call instanceof SQLCall)) {
            return null;
        }

        return ((SQLCall) call).getSQLString();
    }

    /**
     * ADVANCED: Return the SQL strings of the query. Used for queries with
     * multiple calls This can be used for SQL queries. This can also be used
     * for normal queries if they have been prepared, (i.e.
     * query.prepareCall()).
     *
     * @see #prepareCall(org.eclipse.persistence.sessions.Session, DataRecord) prepareCall(Session, Record)
     */
    public List getSQLStrings() {
        List calls = getDatasourceCalls();
        if ((calls == null) || calls.isEmpty()) {
            return null;
        }
        Vector returnSQL = new Vector(calls.size());
        Iterator iterator = calls.iterator();
        while (iterator.hasNext()) {
            Call call = (Call) iterator.next();
            if (!(call instanceof SQLCall)) {
                return null;
            }
            returnSQL.add(((SQLCall) call).getSQLString());
        }
        return returnSQL;
    }

    /**
     * INTERNAL: Returns the internal tri-state value of shouldBindParameters
     * used far cascading these settings
     */
    public Boolean getShouldBindAllParameters() {
        return this.shouldBindAllParameters;
    }

    /**
     * INTERNAL:
     */
    public DatabaseMapping getSourceMapping() {
        return sourceMapping;
    }

    /**
     * ADVANCED: This can be used to access a queries translated SQL if they
     * have been prepared, (i.e. query.prepareCall()). The Record argument is
     * one of (Record, XMLRecord) that contains the query arguments.
     *
     * @see #prepareCall(org.eclipse.persistence.sessions.Session, DataRecord)
     */
    public String getTranslatedSQLString(org.eclipse.persistence.sessions.Session session, DataRecord translationRow) {
        prepareCall(session, translationRow);
        // CR#2859559 fix to use Session and Record interfaces not impl classes.
        CallQueryMechanism queryMechanism = (CallQueryMechanism) getQueryMechanism();
        if (queryMechanism.getCall() == null) {
            return null;
        }
        SQLCall call = (SQLCall) queryMechanism.getCall().clone();
        call.setUsesBinding(false);
        call.translate((AbstractRecord) translationRow, queryMechanism.getModifyRow(), (AbstractSession) session);
        return call.getSQLString();
    }

    /**
     * ADVANCED: This can be used to access a queries translated SQL if they
     * have been prepared, (i.e. query.prepareCall()). This method can be used
     * for queries with multiple calls.
     *
     * @see #prepareCall(org.eclipse.persistence.sessions.Session, DataRecord) prepareCall(Session, Record)
     */
    public List getTranslatedSQLStrings(org.eclipse.persistence.sessions.Session session, DataRecord translationRow) {
        prepareCall(session, translationRow);
        CallQueryMechanism queryMechanism = (CallQueryMechanism) getQueryMechanism();
        if ((queryMechanism.getCalls() == null) || queryMechanism.getCalls().isEmpty()) {
            return null;
        }
        Vector calls = new Vector(queryMechanism.getCalls().size());
        Iterator iterator = queryMechanism.getCalls().iterator();
        while (iterator.hasNext()) {
            SQLCall call = (SQLCall) iterator.next();
            call = (SQLCall) call.clone();
            call.setUsesBinding(false);
            call.translate((AbstractRecord) translationRow, queryMechanism.getModifyRow(), (AbstractSession) session);
            calls.add(call.getSQLString());
        }
        return calls;
    }

    /**
     * INTERNAL: Return the row for translation
     */
    public AbstractRecord getTranslationRow() {
        return translationRow;
    }

    /**
     * INTERNAL: returns true if the accessor has already been set. The
     * getAccessor() will attempt to lazily initialize it.
     */
    public boolean hasAccessor() {
        return this.accessors != null;
    }

    /**
     * INTERNAL: Return if any properties exist in the query.
     */
    public boolean hasProperties() {
        return (properties != null) && (!properties.isEmpty());
    }

    /**
     * INTERNAL: Return if any arguments exist in the query.
     */
    public boolean hasArguments() {
        return (arguments != null) && (!arguments.isEmpty());
    }

    /**
     * PUBLIC: Return if a name of the session that the query should be executed
     * under has been specified. This can be with the session broker to override
     * the default session.
     */
    public boolean hasSessionName() {
        return sessionName != null;
    }

    /**
     * PUBLIC: Session's shouldBindAllParameters() defines whether to bind or
     * not (default setting)
     */
    public void ignoreBindAllParameters() {
        this.shouldBindAllParameters = null;
    }

    /**
     * PUBLIC: Session's shouldCacheAllStatements() defines whether to cache or
     * not (default setting)
     */
    public void ignoreCacheStatement() {
        this.shouldCacheStatement = null;
    }

    /**
     * PUBLIC: Return true if this query uses SQL, a stored procedure, or SDK
     * call.
     */
    public boolean isCallQuery() {
        return (this.queryMechanism != null) && this.queryMechanism.isCallQueryMechanism();
    }

    /**
     * INTERNAL: Returns true if this query has been created as the result of
     * cascading a delete of an aggregate collection in a UnitOfWork CR 2811
     */
    public boolean isCascadeOfAggregateDelete() {
        return this.cascadePolicy == CascadeAggregateDelete;
    }

    /**
     * PUBLIC: Return if this is a data modify query.
     */
    public boolean isDataModifyQuery() {
        return false;
    }

    /**
     * PUBLIC: Return if this is a data read query.
     */
    public boolean isDataReadQuery() {
        return false;
    }

    /**
     * PUBLIC: Return if this is a value read query.
     */
    public boolean isValueReadQuery() {
        return false;
    }

    /**
     * PUBLIC: Return if this is a direct read query.
     */
    public boolean isDirectReadQuery() {
        return false;
    }

    /**
     * PUBLIC: Return if this is a delete all query.
     */
    public boolean isDeleteAllQuery() {
        return false;
    }

    /**
     * PUBLIC: Return if this is a delete object query.
     */
    public boolean isDeleteObjectQuery() {
        return false;
    }

    /**
     * PUBLIC: Return true if this query uses an expression query mechanism
     */
    public boolean isExpressionQuery() {
        return (this.queryMechanism == null) || this.queryMechanism.isExpressionQueryMechanism();
    }

    /**
     * PUBLIC: Return true if this is a modify all query.
     */
    public boolean isModifyAllQuery() {
        return false;
    }

    /**
     * PUBLIC: Return true if this is a modify query.
     */
    public boolean isModifyQuery() {
        return false;
    }

    /**
     * PUBLIC: Return true if this is an update all query.
     */
    public boolean isUpdateAllQuery() {
        return false;
    }

    /**
     * PUBLIC: Return true if this is an update object query.
     */
    public boolean isUpdateObjectQuery() {
        return false;
    }

    /**
     * PUBLIC: If executed against a RepeatableWriteUnitOfWork if this attribute
     * is true EclipseLink will write changes to the database before executing
     * the query.
     */
    public Boolean getFlushOnExecute() {
        return this.flushOnExecute;
    }

    /**
     * PUBLIC: Return true if this is an insert object query.
     */
    public boolean isInsertObjectQuery() {
        return false;
    }

    /**
     * PUBLIC: Return true if this is an object level modify query.
     */
    public boolean isObjectLevelModifyQuery() {
        return false;
    }

    /**
     * PUBLIC: Return true if this is an object level read query.
     */
    public boolean isObjectLevelReadQuery() {
        return false;
    }

    /**
     * PUBLIC: Return if this is an object building query.
     */
    public boolean isObjectBuildingQuery() {
        return false;
    }

    /**
     * INTERNAL: Queries are prepared when they are executed and then do not
     * need to be prepared on subsequent executions. This method returns true if
     * this query has been prepared. Updating the settings on a query will
     * 'un-prepare' the query.
     */
    public boolean isPrepared() {
        return isPrepared;
    }

    /**
     * PUBLIC: Return true if this is a read all query.
     */
    public boolean isReadAllQuery() {
        return false;
    }

    /**
     * PUBLIC: Return true if this is a read object query.
     */
    public boolean isReadObjectQuery() {
        return false;
    }

    /**
     * PUBLIC: Return true if this is a read query.
     */
    public boolean isReadQuery() {
        return false;
    }

    /**
     * PUBLIC: Return true if this is a report query.
     */
    public boolean isReportQuery() {
        return false;
    }

    /**
     * PUBLIC: Return true if this is a result set mapping query.
     */
    public boolean isResultSetMappingQuery() {
        return false;
    }

    /**
     * PUBLIC: Return true if this query uses an SQL query mechanism .
     */
    public boolean isSQLCallQuery() {
        // BUG#2669342 CallQueryMechanism and isCallQueryMechanism have
        // different meaning as SDK return true but isn't.
        Call call = getDatasourceCall();
        return (call != null) && (call instanceof SQLCall);
    }

    /**
     * PUBLIC: Return true if this query uses an JPQL query mechanism .
     */
    public boolean isJPQLCallQuery() {
        return ((this.queryMechanism != null)
                && this.queryMechanism.isJPQLCallQueryMechanism()
                && ((JPQLCallQueryMechanism)this.queryMechanism).getJPQLCall() != null);
    }

    /**
     * INTERNAL: Return true if the query is a custom user defined query.
     */
    public boolean isUserDefined() {
        return isUserDefined;
    }

    /**
     * INTERNAL: Return true if the query is a custom user defined SQL call query.
     */
    public boolean isUserDefinedSQLCall() {
        return this.isUserDefinedSQLCall;
    }

    /**
     * INTERNAL: Return true if the query uses default properties. This is used
     * to determine if this query is cacheable. i.e. does not use any properties
     * that may conflict with another query with the same EJBQL or selection
     * criteria.
     */
    public boolean isDefaultPropertiesQuery() {
        return (!this.isUserDefined) && (this.shouldPrepare) && (this.queryTimeout == DescriptorQueryManager.DefaultTimeout)
            && (this.hintString == null) && (this.shouldBindAllParameters == null) && (this.shouldCacheStatement == null) && (this.shouldUseWrapperPolicy);
    }

    /**
     * PUBLIC: Return true if this is a write object query.
     */
    public boolean isWriteObjectQuery() {
        return false;
    }

    /**
     * PUBLIC: Set for the identity map (cache) to be maintained. This is the
     * default.
     */
    public void maintainCache() {
        setShouldMaintainCache(true);
    }

    /**
     * INTERNAL: This is different from 'prepareForExecution' in that this is
     * called on the original query, and the other is called on the copy of the
     * query. This query is copied for concurrency so this prepare can only
     * setup things that will apply to any future execution of this query.
     * <p>
     * Resolve the queryTimeout using the DescriptorQueryManager if required.
     */
    protected void prepare() throws QueryException {
        // If the queryTimeout is DefaultTimeout, resolve using the
        // DescriptorQueryManager.
        if (this.queryTimeout == DescriptorQueryManager.DefaultTimeout) {
            if (this.descriptor == null) {
                setQueryTimeout(this.session.getQueryTimeoutDefault());
                if(this.session.getQueryTimeoutUnitDefault() == null) {
                    this.session.setQueryTimeoutUnitDefault(DescriptorQueryManager.DefaultTimeoutUnit);
                }
                setQueryTimeoutUnit(this.session.getQueryTimeoutUnitDefault());
            } else {
                int timeout = this.descriptor.getQueryManager().getQueryTimeout();
                // No timeout means none set, so use the session default.
                if ((timeout == DescriptorQueryManager.DefaultTimeout) || (timeout == DescriptorQueryManager.NoTimeout)) {
                    timeout = this.session.getQueryTimeoutDefault();
                }
                setQueryTimeout(timeout);

                //Bug #456067
                TimeUnit timeoutUnit = this.descriptor.getQueryManager().getQueryTimeoutUnit();
                if(timeoutUnit == DescriptorQueryManager.DefaultTimeoutUnit) {
                    timeoutUnit = this.session.getQueryTimeoutUnitDefault();
                }
                setQueryTimeoutUnit(timeoutUnit);
            }
        }

        this.argumentFields = buildArgumentFields();

        getQueryMechanism().prepare();
        resetMonitorName();
    }

    /**
     * INTERNAL: Copy all setting from the query. This is used to morph queries
     * from one type to the other. By default this calls prepareFromQuery, but
     * additional properties may be required to be copied as prepareFromQuery
     * only copies properties that affect the SQL.
     */
    public void copyFromQuery(DatabaseQuery query) {
        prepareFromQuery(query);
        this.cascadePolicy = query.cascadePolicy;
        this.flushOnExecute = query.flushOnExecute;
        this.arguments = query.arguments;
        this.nullableArguments = query.nullableArguments;
        this.argumentTypes = query.argumentTypes;
        this.argumentTypeNames = query.argumentTypeNames;
        this.argumentValues = query.argumentValues;
        this.queryTimeout = query.queryTimeout;
        this.queryTimeoutUnit = query.queryTimeoutUnit;
        this.redirector = query.redirector;
        this.sessionName = query.sessionName;
        this.shouldBindAllParameters = query.shouldBindAllParameters;
        this.shouldCacheStatement = query.shouldCacheStatement;
        this.shouldMaintainCache = query.shouldMaintainCache;
        this.shouldPrepare = query.shouldPrepare;
        this.shouldUseWrapperPolicy = query.shouldUseWrapperPolicy;
        this.properties = query.properties;
        this.doNotRedirect = query.doNotRedirect;
        this.shouldRetrieveBypassCache = query.shouldRetrieveBypassCache;
        this.shouldStoreBypassCache = query.shouldStoreBypassCache;
        this.parameterDelimiter = query.parameterDelimiter;
        this.shouldCloneCall = query.shouldCloneCall;
        this.partitioningPolicy = query.partitioningPolicy;
    }

    /**
     * INTERNAL: Prepare the query from the prepared query. This allows a
     * dynamic query to prepare itself directly from a prepared query instance.
     * This is used in the JPQL parse cache to allow preparsed queries to be
     * used to prepare dynamic queries. This only copies over properties that
     * are configured through JPQL.
     */
    public void prepareFromQuery(DatabaseQuery query) {
        setQueryMechanism((DatabaseQueryMechanism) query.getQueryMechanism().clone());
        getQueryMechanism().setQuery(this);
        this.descriptor = query.descriptor;
        this.hintString = query.hintString;
        this.isCustomQueryUsed = query.isCustomQueryUsed;
        this.argumentFields = query.argumentFields;
        // this.properties = query.properties; - Cannot set properties and CMP
        // stores finders there.
    }

    /**
     * ADVANCED: Pre-generate the call/SQL for the query. This method takes a
     * Session and an implementor of Record (DatebaseRow or XMLRecord). This can
     * be used to access the SQL for a query without executing it. To access the
     * call use, query.getCall(), or query.getSQLString() for the SQL. Note the
     * SQL will have argument markers in it (i.e. "?"). To translate these use
     * query.getTranslatedSQLString(session, translationRow).
     *
     * @see #getCall()
     * @see #getSQLString()
     * @see #getTranslatedSQLString(org.eclipse.persistence.sessions.Session,
     *      DataRecord)
     */
    public void prepareCall(org.eclipse.persistence.sessions.Session session, DataRecord translationRow) throws QueryException {
        // CR#2859559 fix to use Session and Record interfaces not impl classes.
        checkPrepare((AbstractSession) session, (AbstractRecord) translationRow, true);
    }

    /**
     * INTERNAL: Set the properties needed to be cascaded into the custom query.
     */
    protected void prepareCustomQuery(DatabaseQuery customQuery) {
        // Nothing by default.
    }

    /**
     * INTERNAL: Prepare the receiver for execution in a session. In particular,
     * set the descriptor of the receiver to the ClassDescriptor for the
     * appropriate class for the receiver's object.
     */
    public void prepareForExecution() throws QueryException {
    }

    protected void prepareForRemoteExecution() {
    }

    /**
     * INTERNAL: Use a EclipseLink redirector to redirect this query to a
     * method. Added for bug 3241138
     *
     */
    public Object redirectQuery(QueryRedirector redirector, DatabaseQuery queryToRedirect, AbstractSession session, AbstractRecord translationRow) {
        if (redirector == null) {
            return null;
        }
        DatabaseQuery queryToExecute = (DatabaseQuery) queryToRedirect.clone();
        queryToExecute.setRedirector(null);

        // since we deal with a clone when using a redirector, the descriptor
        // will
        // get set on the clone, but not on the original object. So before
        // returning
        // the results, set the descriptor from the clone onto this object
        // (original
        // query) - GJPP, BUG# 2692956
        Object toReturn = redirector.invokeQuery(queryToExecute, translationRow, session);
        setDescriptor(queryToExecute.getDescriptor());
        return toReturn;
    }

    protected Object remoteExecute() {
        this.session.startOperationProfile(SessionProfiler.Remote, this, SessionProfiler.ALL);
        Transporter transporter = ((DistributedSession) this.session).getRemoteConnection().remoteExecute((DatabaseQuery) this.clone());
        this.session.endOperationProfile(SessionProfiler.Remote, this, SessionProfiler.ALL);
        return extractRemoteResult(transporter);
    }

    /**
     * INTERNAL:
     */
    public Object remoteExecute(AbstractSession session) {
        setSession(session);
        prepareForRemoteExecution();
        return remoteExecute();
    }

    /**
     * INTERNAL: Property support used by mappings.
     */
    public void removeProperty(Object property) {
        getProperties().remove(property);
    }

    /**
     * INTERNAL: replace the value holders in the specified result object(s)
     */
    public Map replaceValueHoldersIn(Object object, RemoteSessionController controller) {
        // by default, do nothing
        return null;
    }

    /**
     * ADVANCED: JPA flag used to control the behavior of the shared cache. This
     * flag specifies the behavior when data is retrieved by the find methods
     * and by the execution of queries. Calling this method will set a retrieve
     * bypass to true.
     */
    public void retrieveBypassCache() {
        setShouldRetrieveBypassCache(true);
    }

    /**
     * INTERNAL: Build the list of arguments fields from the argument names and
     * types.
     */
    public List<DatabaseField> buildArgumentFields() {
        List<String> arguments = getArguments();
        List<Class<?>> argumentTypes = getArgumentTypes();
        List<DatabaseField> argumentFields = new ArrayList<>(arguments.size());
        int size = arguments.size();
        for (int index = 0; index < size; index++) {
            DatabaseField argumentField = new DatabaseField(arguments.get(index));
            argumentField.setType(argumentTypes.get(index));
            argumentFields.add(argumentField);
        }

        return argumentFields;
    }

    /**
     * INTERNAL: Translate argumentValues into a database row.
     */
    public AbstractRecord rowFromArguments(List argumentValues, AbstractSession session) throws QueryException {
        List<DatabaseField> argumentFields = this.argumentFields;

        // PERF: argumentFields are set in prepare, but need to be built if
        // query is not prepared.
        if (!isPrepared() || (argumentFields == null)) {
            argumentFields = buildArgumentFields();
        }

        if (argumentFields.size() != argumentValues.size()) {
            throw QueryException.argumentSizeMismatchInQueryAndQueryDefinition(this);
        }

        int argumentsSize = argumentFields.size();
        AbstractRecord row = new DatabaseRecord(argumentsSize);
        for (int index = 0; index < argumentsSize; index++) {
            row.put(argumentFields.get(index), argumentValues.get(index));
        }

        return row;
    }

    /**
     * INTERNAL:
     * Set the list of connection accessors to execute the query on.
     */
    public void setAccessors(Collection<Accessor> accessors) {
        this.accessors = accessors;
    }

    /**
     * INTERNAL: Set the accessor, the query must always use the same accessor
     * for database access. This is required to support connection pooling.
     */
    public void setAccessor(Accessor accessor) {
        if (accessor == null) {
            this.accessors = null;
            return;
        }
        List<Accessor> accessors = new ArrayList<>(1);
        accessors.add(accessor);
        this.accessors = accessors;
    }

    /**
     * PUBLIC: Used to define a store procedure or SQL query.
     */
    public void setDatasourceCall(Call call) {
        if (call == null) {
            return;
        }
        setQueryMechanism(call.buildNewQueryMechanism(this));
    }

    /**
     * PUBLIC: Used to define a store procedure or SQL query.
     */
    public void setCall(Call call) {
        setDatasourceCall(call);
        isUserDefinedSQLCall = true;
    }

    /**
     * INTERNAL: Set the cascade policy.
     */
    public void setCascadePolicy(int policyConstant) {
        cascadePolicy = policyConstant;
    }

    /**
     * INTERNAL: Set the descriptor for the query.
     */
    public void setDescriptor(ClassDescriptor descriptor) {
        // If the descriptor changed must unprepare as the SQL may change.
        if (this.descriptor != descriptor) {
            setIsPrepared(false);
        }
        this.descriptor = descriptor;
    }

    /**
     * PUBLIC: Set the JPQL string of the query. If arguments are required in
     * the string they will be preceded by ":" then the argument name. The JPQL
     * arguments must also be added as argument to the query.
     */
    public void setJPQLString(String jpqlString) {
        setEJBQLString(jpqlString);
    }

    /**
     * PUBLIC: Set the EJBQL string of the query. If arguments are required in
     * the string they will be preceded by "?" then the argument number.
     */
    public void setEJBQLString(String ejbqlString) {
        // Added the check for when we are building the query from the
        // deployment XML
        if ((ejbqlString != null) && (!ejbqlString.isEmpty())) {
            JPQLCallQueryMechanism mechanism = new JPQLCallQueryMechanism(this, new JPQLCall(ejbqlString));
            setQueryMechanism(mechanism);
        }
    }

    /**
     * PUBLIC: If executed against a RepeatableWriteUnitOfWork if this attribute
     * is true EclipseLink will write changes to the database before executing
     * the query.
     */
    public void setFlushOnExecute(Boolean flushMode) {
        this.flushOnExecute = flushMode;
    }

    /**
     * Used to set a database hint string on the query. This should be the full
     * hint string including the comment delimiters. The hint string will be
     * generated into the SQL string after the SELECT/INSERT/UPDATE/DELETE
     * instruction.
     * <p>
     * <b>Example:</b>
     * <pre>
     * readAllQuery.setHintString("/*+ index(scott.emp ix_emp) * /");
     * </pre>
     * would result in SQL like:
     * <pre>select /*+ index(scott.emp ix_emp) * / from scott.emp emp_alias</pre>
     * <p>
     * This method will cause a query to re-prepare if it has already been
     * executed.
     *
     * @param newHintString
     *            the hint string to be added into the SQL call.
     */
    public void setHintString(String newHintString) {
        hintString = newHintString;
        setIsPrepared(false);
    }

    /**
     * INTERNAL: If changes are made to the query that affect the derived SQL or
     * Call parameters the query needs to be prepared again.
     * <p>
     * Automatically called internally.
     */
    public void setIsPrepared(boolean isPrepared) {
        this.isPrepared = isPrepared;
        if (!isPrepared) {
            this.isCustomQueryUsed = null;
            if (this.queryMechanism != null) {
                this.queryMechanism.unprepare();
            }
        }
    }

    /**
     * INTERNAL: PERF: Return if the query is an execution clone. This allow the
     * clone during execution to be avoided in the cases when the query has
     * already been clone elsewhere.
     */
    public boolean isExecutionClone() {
        return isExecutionClone;
    }

    /**
     * INTERNAL: PERF: Set if the query is an execution clone. This allow the
     * clone during execution to be avoided in the cases when the query has
     * already been clone elsewhere.
     */
    public void setIsExecutionClone(boolean isExecutionClone) {
        this.isExecutionClone = isExecutionClone;
    }

    /**
     * INTERNAL: PERF: Return if this query will use the descriptor custom query
     * instead of executing itself.
     */
    public Boolean isCustomQueryUsed() {
        return this.isCustomQueryUsed;
    }

    /**
     * INTERNAL: If the query mechanism is a call query mechanism and there are
     * no arguments on the query then it must be a foreign reference custom
     * selection query.
     */
    protected boolean isCustomSelectionQuery() {
        return getQueryMechanism().isCallQueryMechanism() && getArguments().isEmpty();
    }

    /**
     * INTERNAL: PERF: Set if this query will use the descriptor custom query
     * instead of executing itself.
     * @param isCustomQueryUsed Custom query flag as {@code boolean}.
     */
    protected void setIsCustomQueryUsed(final boolean isCustomQueryUsed) {
        this.isCustomQueryUsed = isCustomQueryUsed;
    }

    /**
     * INTERNAL: Set if the query is a custom user defined query.
     */
    public void setIsUserDefined(boolean isUserDefined) {
        this.isUserDefined = isUserDefined;
    }

    /**
     * INTERNAL: Set if the query is a custom user defined sql call query.
     */
    public void setIsUserDefinedSQLCall(boolean isUserDefinedSQLCall) {
        this.isUserDefinedSQLCall = isUserDefinedSQLCall;
    }

    /**
     * PUBLIC: Set the query's name. Queries can be named and added to a
     * descriptor or the session and then referenced by name.
     */
    public void setName(String queryName) {
        name = queryName;
    }

    /**
     * INTERNAL:
     * Set the String char used to delimit an SQL parameter.
     */
    public void setParameterDelimiter(String aParameterDelimiter) {
        // 325167: if the parameterDelimiter is invalid - use the default # symbol
        if(null == aParameterDelimiter || aParameterDelimiter.isEmpty()) {
            aParameterDelimiter = ParameterDelimiterType.DEFAULT;
        }
        parameterDelimiter = aParameterDelimiter;
    }

    /**
     * INTERNAL: Property support used by mappings.
     */
    public void setProperties(Map<Object, Object> properties) {
        this.properties = properties;
    }

    /**
     * INTERNAL: Property support used by mappings to store temporary stuff.
     */
    public synchronized void setProperty(Object property, Object value) {
        getProperties().put(property, value);
    }

    /**
     * Set the query mechanism for the query.
     */
    protected void setQueryMechanism(DatabaseQueryMechanism queryMechanism) {
        this.queryMechanism = queryMechanism;
        // Must un-prepare is prepare as the SQL may change.
        setIsPrepared(false);
    }

    /**
     * PUBLIC: Set the number of seconds the driver will wait for a Statement to
     * execute to the given number of seconds. If the limit is exceeded, a
     * DatabaseException is thrown.
     * <p>
     * queryTimeout - the new query timeout limit in seconds; DefaultTimeout is
     * the default, which redirects to DescriptorQueryManager's queryTimeout.
     *
     * @see DescriptorQueryManager#setQueryTimeout(int)
     *
     */
    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
        this.shouldCloneCall = true;
    }

    public void setQueryTimeoutUnit(TimeUnit queryTimeoutUnit) {
        this.queryTimeoutUnit = queryTimeoutUnit;
        this.shouldCloneCall = true;
    }

    /**
     * PUBLIC: Set the query redirector. A redirector can be used in a query to
     * replace its execution with the execution of code. This can be used for
     * named or parameterized queries to allow dynamic configuration of the
     * query base on the query arguments.
     *
     * @see QueryRedirector
     */
    public void setRedirector(QueryRedirector redirector) {
        this.redirector = redirector;
        this.doNotRedirect = false;
        this.setIsPrepared(false);
    }

    /**
     * PUBLIC: To any user of this object. Set the selection criteria of the
     * query. This method be used when dealing with expressions.
     */
    public void setSelectionCriteria(Expression expression) {
        // Do not overwrite the call if the expression is null.
        if ((expression == null) && (!getQueryMechanism().isExpressionQueryMechanism())) {
            return;
        }
        if (!getQueryMechanism().isExpressionQueryMechanism()) {
            setQueryMechanism(new ExpressionQueryMechanism(this, expression));
        } else {
            ((ExpressionQueryMechanism) getQueryMechanism()).setSelectionCriteria(expression);
        }

        // Must un-prepare is prepare as the SQL may change.
        setIsPrepared(false);
    }

    /**
     * INTERNAL: Set the session for the query
     */
    public void setSession(AbstractSession session) {
        this.session = session;
        this.executionSession = null;
    }

    /**
     * PUBLIC: Set the name of the session that the query should be executed
     * under. This can be with the session broker to override the default
     * session.
     */
    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    /**
     * PUBLIC: Bind all arguments to any SQL statement.
     */
    public void setShouldBindAllParameters(boolean shouldBindAllParameters) {
        this.shouldBindAllParameters = shouldBindAllParameters;
        setIsPrepared(false);
    }

    /**
     * INTERNAL: Sets the internal tri-state value of shouldBindAllParams Used
     * to cascade this value to other queries
     */
    public void setShouldBindAllParameters(Boolean bindAllParams) {
        this.shouldBindAllParameters = bindAllParams;
    }

    /**
     * PUBLIC: Cache the prepared statements, this requires full parameter
     * binding as well.
     */
    public void setShouldCacheStatement(boolean shouldCacheStatement) {
        this.shouldCacheStatement = shouldCacheStatement;
        setIsPrepared(false);
    }

    /**
     * PUBLIC: Set if the identity map (cache) should be used or not. If not the
     * cache check will be skipped and the result will not be put into the
     * identity map. By default the identity map is always maintained.
     */
    public void setShouldMaintainCache(boolean shouldMaintainCache) {
        this.shouldMaintainCache = shouldMaintainCache;
    }

    /**
     * PUBLIC: Set if the query should be prepared. EclipseLink automatically
     * prepares queries to generate their SQL only once, one each execution of
     * the query the SQL does not need to be generated again only the arguments
     * need to be translated. This option is provide to disable this
     * optimization as in can cause problems with certain types of queries that
     * require dynamic SQL based on their arguments.
     * <p>
     * These queries include:
     * <ul>
     * <li>Expressions that make use of 'equal' where the argument value has the
     * potential to be null, this can cause problems on databases that require
     * IS NULL, instead of = NULL.
     * <li>Expressions that make use of 'in' and that use parameter binding,
     * this will cause problems as the in values must be bound individually.
     * </ul>
     */
    public void setShouldPrepare(boolean shouldPrepare) {
        this.shouldPrepare = shouldPrepare;
        setIsPrepared(false);
    }

    /**
     * ADVANCED: JPA flag used to control the behavior of the shared cache. This
     * flag specifies the behavior when data is retrieved by the find methods
     * and by the execution of queries.
     */
    public void setShouldRetrieveBypassCache(boolean shouldRetrieveBypassCache) {
        this.shouldRetrieveBypassCache = shouldRetrieveBypassCache;
    }

    /**
     * ADVANCED: JPA flag used to control the behavior of IDENTITY generation. This
     * flag specifies the behavior when data is retrieved by the find methods
     * and by the execution of queries.
     * <p>
     * This flag is only applicable to Insert queries and will only apply if the database
     * platform supports {@link java.sql.Statement#RETURN_GENERATED_KEYS}
     */
    public void setShouldReturnGeneratedKeys(boolean shouldReturnGeneratedKeys) {
        this.shouldReturnGeneratedKeys = shouldReturnGeneratedKeys;
    }

    /**
     * ADVANCED: JPA flag used to control the behavior of the shared cache. This
     * flag specifies the behavior when data is read from the database and when
     * data is committed into the database.
     */
    public void setShouldStoreBypassCache(boolean shouldStoreBypassCache) {
        this.shouldStoreBypassCache = shouldStoreBypassCache;
    }

    /**
     * INTERNAL:
     * Set if additional validation should be performed before the query uses
     * the update call cache.
     */
    public void setShouldValidateUpdateCallCacheUse(boolean shouldCheckUpdateCallCacheUse) {
        this.shouldValidateUpdateCallCacheUse = shouldCheckUpdateCallCacheUse;
    }

    /**
     * ADVANCED: The wrapper policy can be enable on a query.
     */
    public void setShouldUseWrapperPolicy(boolean shouldUseWrapperPolicy) {
        this.shouldUseWrapperPolicy = shouldUseWrapperPolicy;
    }

    /**
     * INTERNAL:
     */
    public void setSourceMapping(DatabaseMapping sourceMapping) {
        this.sourceMapping = sourceMapping;
    }

    /**
     * PUBLIC: To any user of this object. Set the SQL statement of the query.
     * This method should only be used when dealing with statement objects.
     */
    public void setSQLStatement(SQLStatement sqlStatement) {
        setQueryMechanism(new StatementQueryMechanism(this, sqlStatement));
    }

    /**
     * PUBLIC: To any user of this object. Set the SQL string of the query. This
     * method should only be used when dealing with user defined SQL strings. If
     * arguments are required in the string they will be preceded by "#" then
     * the argument name. Warning: Allowing an unverified SQL string to be
     * passed into this method makes your application vulnerable to SQL
     * injection attacks.
     */
    public void setSQLString(String sqlString) {
        // Added the check for when we are building the query from the
        // deployment XML
        if ((sqlString != null) && (!sqlString.isEmpty())) {
            setCall(new SQLCall(sqlString));
        }
    }

    /**
     * INTERNAL: Set the row for translation
     */
    public void setTranslationRow(AbstractRecord translationRow) {
        this.translationRow = translationRow;
    }

    /**
     * PUBLIC: Bind all arguments to any SQL statement.
     */
    public boolean shouldBindAllParameters() {
        return Boolean.TRUE.equals(shouldBindAllParameters);
    }

    /**
     * INTERNAL:
     * Return true if this individual query should allow native a SQL call
     * to be issued.
     */
    public boolean shouldAllowNativeSQLQuery(boolean projectAllowsNativeQueries) {
        // If allow native SQL query is undefined, use the project setting
        // otherwise use the allow native SQL setting.
        return (allowNativeSQLQuery == null) ? projectAllowsNativeQueries : allowNativeSQLQuery;
    }

    /**
     * PUBLIC: Cache the prepared statements, this requires full parameter
     * binding as well.
     */
    public boolean shouldCacheStatement() {
        return Boolean.TRUE.equals(shouldCacheStatement);
    }

    /**
     * PUBLIC: Flag used to determine if all parts should be cascaded
     */
    public boolean shouldCascadeAllParts() {
        return this.cascadePolicy == CascadeAllParts;
    }

    /**
     * PUBLIC: Mappings should be checked to determined if the current operation
     * should be cascaded to the objects referenced.
     */
    public boolean shouldCascadeByMapping() {
        return this.cascadePolicy == CascadeByMapping;
    }

    /**
     * INTERNAL: Flag used for unit of works cascade policy.
     */
    public boolean shouldCascadeOnlyDependentParts() {
        return this.cascadePolicy == CascadeDependentParts;
    }

    /**
     * PUBLIC: Flag used to determine if any parts should be cascaded
     */
    public boolean shouldCascadeParts() {
        return this.cascadePolicy != NoCascading;
    }

    /**
     * PUBLIC: Flag used to determine if any private parts should be cascaded
     */
    public boolean shouldCascadePrivateParts() {
        return (this.cascadePolicy == CascadePrivateParts) || (this.cascadePolicy == CascadeAllParts);
    }

    /**
     * INTERNAL: Flag used to determine if the call needs to be cloned.
     */
    public boolean shouldCloneCall() {
        return shouldCloneCall;
    }

    /**
     * PUBLIC: Local shouldBindAllParameters() should be ignored, Session's
     * shouldBindAllParameters() should be used.
     */
    public boolean shouldIgnoreBindAllParameters() {
        return shouldBindAllParameters == null;
    }

    /**
     * PUBLIC: Local shouldCacheStatement() should be ignored, Session's
     * shouldCacheAllStatements() should be used.
     */
    public boolean shouldIgnoreCacheStatement() {
        return shouldCacheStatement == null;
    }

    /**
     * PUBLIC: Return if the identity map (cache) should be used or not. If not
     * the cache check will be skipped and the result will not be put into the
     * identity map. By default the identity map is always maintained.
     */
    public boolean shouldMaintainCache() {
        return shouldMaintainCache;
    }

    /**
     * PUBLIC: Return if the query should be prepared. EclipseLink automatically
     * prepares queries to generate their SQL only once, one each execution of
     * the query the SQL does not need to be generated again only the arguments
     * need to be translated. This option is provide to disable this
     * optimization as in can cause problems with certain types of queries that
     * require dynamic SQL based on their arguments.
     * <p>
     * These queries include:
     * <ul>
     * <li>Expressions that make use of 'equal' where the argument value has the
     * potential to be null, this can cause problems on databases that require
     * IS NULL, instead of = NULL.
     * <li>Expressions that make use of 'in' and that use parameter binding,
     * this will cause problems as the in values must be bound individually.
     * </ul>
     */
    public boolean shouldPrepare() {
        return shouldPrepare;
    }

    /**
     * INTERNAL:
     * Check if the query should be prepared, or dynamic, depending on the arguments.
     * This allows null parameters to affect the SQL, such as stored procedure default values,
     * or IS NULL, or insert defaults.
     */
    public boolean shouldPrepare(AbstractRecord translationRow, AbstractSession session) {
        if (!this.shouldPrepare) {
            return false;
        }
        if (!((DatasourcePlatform)session.getDatasourcePlatform()).shouldPrepare(this)) {
            this.shouldPrepare = false;
            return false;
        }
        if (this.nullableArguments != null) {
            for (DatabaseField argument : this.nullableArguments) {
                if (translationRow.get(argument) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * ADVANCED: JPA flag used to control the behavior of the shared cache. This
     * flag specifies the behavior when data is retrieved by the find methods
     * and by the execution of queries.
     */
    public boolean shouldRetrieveBypassCache() {
        return this.shouldRetrieveBypassCache;
    }

    /**
     * ADVANCED: JPA flag used to control the behavior of IDENTITY generation. This
     * flag specifies the behavior when data is retrieved by the find methods
     * and by the execution of queries.
     * <p>
     * This flag is only applicable to Insert queries and will only apply if the database
     * platform supports {@link java.sql.Statement#RETURN_GENERATED_KEYS}
     */
    public boolean shouldReturnGeneratedKeys() {
        return this.shouldReturnGeneratedKeys;
    }

    /**
     * ADVANCED: JPA flag used to control the behavior of the shared cache. This
     * flag specifies the behavior when data is read from the database and when
     * data is committed into the database.
     */
    public boolean shouldStoreBypassCache() {
        return this.shouldStoreBypassCache;
    }

    /**
     * ADVANCED: The wrapper policy can be enabled on a query.
     */
    public boolean shouldUseWrapperPolicy() {
        return shouldUseWrapperPolicy;
    }

    /**
     * ADVANCED:
     * Return true if additional validation should be performed before the query uses
     * the update call cache, false otherwise.
     */
    public boolean shouldValidateUpdateCallCacheUse() {
        return shouldValidateUpdateCallCacheUse;
    }

    /**
     * ADVANCED: JPA flag used to control the behavior of the shared cache. This
     * flag specifies the behavior when data is read from the database and when
     * data is committed into the database. Calling this method will set a store
     * bypass to true.
     * <p>
     * Note: For a cache store mode of REFRESH, see refreshIdentityMapResult()
     * from ObjectLevelReadQuery.
     */
    public void storeBypassCache() {
        setShouldStoreBypassCache(true);
    }

    @Override
    public String toString() {
        String referenceClassString = "";
        String nameString = "";
        String queryString = "";
        if (getReferenceClass() != null) {
            referenceClassString = "referenceClass=" + getReferenceClass().getSimpleName() + " ";
        }
        if ((getName() != null) && (!getName().isEmpty())) {
            nameString = "name=\"" + getName() + "\" ";
        }
        if (isSQLCallQuery()) {
            queryString = "sql=\"" + getSQLString() + "\"";
        } else if (isJPQLCallQuery()) {
            queryString = "jpql=\"" + getJPQLString() + "\"";
        }
        return getClass().getSimpleName() + "(" + nameString + referenceClassString + queryString + ")";
    }

    /**
     * ADVANCED: Set if the descriptor requires usage of a native (unwrapped)
     * JDBC connection. This may be required for some Oracle JDBC support when a
     * wrapping DataSource is used.
     */
    public void setIsNativeConnectionRequired(boolean isNativeConnectionRequired) {
        this.isNativeConnectionRequired = isNativeConnectionRequired;
    }

    /**
     * ADVANCED: Return if the descriptor requires usage of a native (unwrapped)
     * JDBC connection. This may be required for some Oracle JDBC support when a
     * wrapping DataSource is used.
     */
    public boolean isNativeConnectionRequired() {
        return isNativeConnectionRequired;
    }

    /**
     * This method is used in combination with redirected queries. If a
     * redirector is set on the query or there is a default redirector on the
     * Descriptor setting this value to true will force EclipseLink to ignore
     * the redirector during execution. This setting will be used most often
     * when reexecuting the query within a redirector.
     */
    public boolean getDoNotRedirect() {
        return doNotRedirect;
    }

    /**
     * This method is used in combination with redirected queries. If a
     * redirector is set on the query or there is a default redirector on the
     * Descriptor setting this value to true will force EclipseLink to ignore
     * the redirector during execution. This setting will be used most often
     * when reexecuting the query within a redirector.
     */
    public void setDoNotRedirect(boolean doNotRedirect) {
        this.doNotRedirect = doNotRedirect;
    }

    /**
     * INTERNAL:
     * Return temporary map of batched objects.
     */
    @SuppressWarnings({"unchecked"})
    public Map<Object, Object> getBatchObjects() {
        return (Map<Object, Object>)getProperty(BATCH_FETCH_PROPERTY);
    }

    /**
     * INTERNAL:
     * Set temporary map of batched objects.
     */
    public void setBatchObjects(Map<Object, Object> batchObjects) {
        setProperty(BATCH_FETCH_PROPERTY, batchObjects);
    }

    /**
     * INTERNAL:
     * Set to true if this individual query should be marked to bypass a
     * persistence unit level disallow SQL queries flag.
     */
    public void setAllowNativeSQLQuery(Boolean allowNativeSQLQuery) {
        this.allowNativeSQLQuery = allowNativeSQLQuery;
    }

    /**
     * INTERNAL:
     * Return if the query has any nullable arguments.
     */
    public boolean hasNullableArguments() {
        return (this.nullableArguments != null) && !this.nullableArguments.isEmpty();
    }

    /**
     * INTERNAL:
     * Return the list of arguments to check for null.
     * If any are null, the query needs to be re-prepared.
     */
    public List<DatabaseField> getNullableArguments() {
        if (this.nullableArguments == null) {
            this.nullableArguments = new ArrayList<>();
        }
        return nullableArguments;
    }

    /**
     * INTERNAL:
     * Set the list of arguments to check for null.
     * If any are null, the query needs to be re-prepared.
     */
    public void setNullableArguments(List<DatabaseField> nullableArguments) {
        this.nullableArguments = nullableArguments;
    }
}
