/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter;

import java.sql.SQLException; 
import java.sql.SQLNonTransientException;
import java.util.Arrays; 
import java.util.Collections;
import java.util.List; 
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable; 
import com.ibm.ws.jca.cm.ConnectorService;
import com.ibm.ws.jdbc.internal.DataSourceDef;
import com.ibm.ws.jdbc.internal.PropertyService;
import com.ibm.ws.rsadapter.impl.WSManagedConnectionFactoryImpl;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;
import com.ibm.wsspi.resource.ResourceFactory;

/**
 * Represents the data source configuration.
 */
public class DSConfig implements FFDCSelfIntrospectable {
    private static final TraceComponent tc = Tr.register(DSConfig.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    // WebSphere data source property names and values
    public static final String
                    BEGIN_TRAN_FOR_SCROLLING_APIS = "beginTranForResultSetScrollingAPIs",
                    BEGIN_TRAN_FOR_VENDOR_APIS = "beginTranForVendorAPIs",
                    COMMIT_OR_ROLLBACK_ON_CLEANUP = "commitOrRollbackOnCleanup",
                    CONNECTION_MANAGER_REF = "connectionManagerRef",
                    CONNECTION_SHARING = "connectionSharing",
                    CONTAINER_AUTH_DATA_REF = "containerAuthDataRef",
                    ENABLE_BEGIN_END_REQUEST = "enableBeginEndRequest", // Not a supported property. Only for internal testing/experimentation.
                    ENABLE_CONNECTION_CASTING = "enableConnectionCasting",
                    ENABLE_MULTITHREADED_ACCESS_DETECTION = "enableMultithreadedAccessDetection", // currently disabled in liberty profile
                    JDBC_DRIVER_REF = "jdbcDriverRef",
                    ON_CONNECT = "onConnect",
                    QUERY_TIMEOUT = "queryTimeout",
                    RECOVERY_AUTH_DATA_REF = "recoveryAuthDataRef",
                    STATEMENT_CACHE_SIZE = "statementCacheSize",
                    SUPPLEMENTAL_JDBC_TRACE = "supplementalJDBCTrace",
                    SYNC_QUERY_TIMEOUT_WITH_TRAN_TIMEOUT = "syncQueryTimeoutWithTransactionTimeout",
                    TYPE = "type",
                    VALIDATION_TIMEOUT = "validationTimeout";

    /**
     * List of dataSource properties.
     */
    public static final List<String> DATA_SOURCE_PROPS =
                    Collections.unmodifiableList(Arrays.asList(
                                                               BEGIN_TRAN_FOR_SCROLLING_APIS,
                                                               BEGIN_TRAN_FOR_VENDOR_APIS,
                                                               COMMIT_OR_ROLLBACK_ON_CLEANUP,
                                                               CONNECTION_MANAGER_REF,
                                                               CONNECTION_SHARING,
                                                               CONTAINER_AUTH_DATA_REF,
                                                               DataSourceDef.isolationLevel.name(),
                                                               ENABLE_BEGIN_END_REQUEST, // Not a supported property. Only for internal testing/experimentation.
                                                               ENABLE_CONNECTION_CASTING,
                                                               JDBC_DRIVER_REF,
                                                               ON_CONNECT,
                                                               QUERY_TIMEOUT,
                                                               RECOVERY_AUTH_DATA_REF,
                                                               STATEMENT_CACHE_SIZE,
                                                               SUPPLEMENTAL_JDBC_TRACE,
                                                               SYNC_QUERY_TIMEOUT_WITH_TRAN_TIMEOUT,
                                                               DataSourceDef.transactional.name(),
                                                               TYPE,
                                                               VALIDATION_TIMEOUT
                                                               ));

    /**
     * Determines whether or not to enlist in a transaction for methods that scroll a result set.
     */
    public final boolean beginTranForResultSetScrollingAPIs;

    /**
     * Determines whether or not to enlist in a transaction when methods are invoked via the
     * wrapper pattern.
     */
    public final boolean beginTranForVendorAPIs;

    /**
     * COMMIT_OR_ROLLBACK_ON_CLEANUP indicates whether we will rollback or commit on cleanup.
     * 
     * If the DB supports UOW detection this property will only be applied when we are in a DB UOW.
     * Otherwise we will always apply this property.
     * 
     * If this property is not specified, any detected implicit transactions will be rolled back. Any
     * undetected implicit transactions must be dealt with by the application.
     */
    public final CommitOrRollbackOnCleanup commitOrRollbackOnCleanup;

    /**
     * Determines how connections are matched for sharing.
     */
    public final ConnectionSharing connectionSharing;

    /**
     * Component with access to various core services needed for JDBC/connection management
     */
    public final ConnectorService connectorSvc;

    /**
     * <p>Not a supported property. Only for internal testing/experimentation.</p>
     *
     * <p>Controls whether the Connection.beginRequest and Connection.endRequest hints are sent to the
     * JDBC driver. These methods were introduced in JDBC 4.3, but unfortunately with insufficient
     * guarantees that these methods will not lead to the JDBC driver corrupting the connection state.
     * At the time of writing this, we have found only a single JDBC 4.3 driver in existence.
     * Its documentation describes a vendor-specific implementation of endRequest that is severely
     * destructive in nature (closing prepared statements, altering connection state without the
     * prompting of the application). If the first and only JDBC 4.3 has interpreted the poorly written
     * JDBC 4.3 requirements around begin/endRequest in a way that led them to implement this behavior
     * that is contrary to the intent to the specification (invocation of these methods is supposed to
     * be transparent!), we can expect that other JDBC drivers will do similar things. Therefore, given
     * that beginRequest/endRequest are optional, we cannot see any good reason to subject our users
     * to this sort of instability and risk, and so OpenLiberty will neither supply these "hints" to
     * the JDBC driver by default nor in any other supported mode of operation.</p>
     *
     * <p>It should be noted that per the JDBC 4.3 JavaDoc for java.sql.Connection, the
     * connection pooling manager does not need to call beginRequest and endRequest if
     * <ul>
     * <li>The connection pool caches PooledConnection objects
     * <li>Returns a logical connection handle when getConnection is called by the application
     * <li>The logical Connection is closed by calling Connection.close prior to returning the PooledConnection to the cache.
     * </ul>
     * The OpenLiberty implementation meets all of the above requirements.
     * Our connection pool does cache PooledConnection (we also cache XAConnection, which is a type of PooledConnection, and Connection).
     * We always return a logical handle (WSJdbc*Connection) to the application when it invokes getConnection.
     * The application uses the Connection.close API to close its logical connection handle, and after
     * that we decide whether to return the underlying PooledConnection (or XAConnection and so forth)
     * to the cache. For example, we might decide to destroy it instead of caching it if a connection error
     * has occurred.
     * It should also be noted that requirements of JCA/Java EE in some cases supersede or alter how the
     * JDBC spec is complied with.  For example, in the case of sharable connections, the application
     * is able to have multiple logical handles to the same underlying PooledConnection.  In light of the
     * intent of the JDBC spec, when JCA connection sharing is used, we ensure that each of these logical
     * handles has either been closed with Connection.close or otherwise dissociated by crossing a
     * sharing scope boundary before we allow the PooledConnection (or XAConnection and so forth) to be
     * returned to the cache.
     * </p>
     *
     * <p>WARNING: usage of this property can lead to unstable and unpredictable behavior.
     * Do not enable this in production.</p> 
     */
    public final boolean enableBeginEndRequest;

    /**
     * Indicates to automatically create a dynamic proxy for interfaces implemented by the connection. 
     */
    public final boolean enableConnectionCasting;

    /**
     * Indicates whether or not to detect multithreaded access.
     */
    public final boolean enableMultithreadedAccessDetection;

    /**
     * Iterator over data source properties. For use by constructor only.
     */
    private NavigableMap<String, Object> entries;

    /**
     * A data source property name/value pair. For use by constructor only.
     */
    private Map.Entry<String, Object> entry;

    /**
     * config.displayId of the data source.
     */
    public final String id;

    /**
     * Default isolation level for new connections.
     */
    public final int isolationLevel;

    /**
     * JNDI name.
     */
    public final String jndiName;

    /**
     * List of SQL commands to execute once per newly established connection. Can be null if none are configured.
     */
    public final String[] onConnect;

    /**
     * Sets a default query timeout, which is the number of seconds (0 means infinite)
     * which a SQL statement may execute before timing out. This default value is overridden
     * during a JTA transaction if custom property syncQueryTimeoutWithTransactionTimeout is
     * enabled. Default value is null (no default query timeout).
     */
    public final Integer queryTimeout;

    /**
     * Maximum cached statements per connection.
     */
    public final int statementCacheSize;

    /**
     * Whether or not supplemental JDBC tracing should be enabled
     */
    public final Boolean supplementalJDBCTrace;

    /**
     * Use the time remaining (if any) in a JTA transaction as the default query timeout
     * for SQL statements. Default value is false.
     */
    public final boolean syncQueryTimeoutWithTransactionTimeout;

    /**
     * Determines whether or not to participate in JTA transactions.
     */
    public final boolean transactional;

    /**
     * Used for timing out connection from the pool which are being validated.
     */
    public final int validationTimeout;

    /**
     * JDBC driver vendor data source properties.
     */
    public final Properties vendorProps;

    /**
     * Constructor for modified configuration.
     * 
     * @param source configuration to copy from.
     * @param wProps WebSphere data source properties
     * @throws Exception if an error occurs.
     */
    public DSConfig(DSConfig source, NavigableMap<String, Object> wProps) throws Exception {
        this(source.id, source.jndiName, wProps, source.vendorProps, source.connectorSvc);
    }

    /**
     * Constructor for new configuration.
     * 
     * @param id the id of the data source.
     * @param jndi the JNDI name of the data source.
     * @param wProps WebSphere data source properties
     * @param vProps JDBC driver vendor data source properties
     * @param connectorSvc connector service instance
     * @throws Exception if an error occurs
     */
    public DSConfig(String id, String jndi,
                    NavigableMap<String, Object> wProps, Properties vProps,
                    ConnectorService connectorSvc) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, getClass().getSimpleName(), new Object[] { jndi, wProps });

        this.connectorSvc = connectorSvc;
        this.id = id;
        jndiName = jndi;
        vendorProps = vProps;

        entries = wProps;
        entry = entries.pollFirstEntry();

        beginTranForResultSetScrollingAPIs = remove(BEGIN_TRAN_FOR_SCROLLING_APIS, true);
        beginTranForVendorAPIs = remove(BEGIN_TRAN_FOR_VENDOR_APIS, true);
        CommitOrRollbackOnCleanup commitOrRollback = remove(COMMIT_OR_ROLLBACK_ON_CLEANUP, null, CommitOrRollbackOnCleanup.class);
        connectionSharing = remove(CONNECTION_SHARING, ConnectionSharing.MatchOriginalRequest, ConnectionSharing.class);
        enableBeginEndRequest = remove(ENABLE_BEGIN_END_REQUEST, false); // Not a supported property. Only for internal testing/experimentation.
        enableConnectionCasting = remove(ENABLE_CONNECTION_CASTING, false);
        enableMultithreadedAccessDetection = false;
        isolationLevel = remove(DataSourceDef.isolationLevel.name(), -1, -1, null, -1, 0, 1, 2, 4, 8, 16, 4096);
        onConnect = remove(ON_CONNECT, (String[]) null);
        queryTimeout = remove(QUERY_TIMEOUT, (Integer) null, 0, TimeUnit.SECONDS);
        statementCacheSize = remove(STATEMENT_CACHE_SIZE, 10, 0, null); // If UCP support is ever added to Liberty, can document how to disable statement caching
        supplementalJDBCTrace = remove(SUPPLEMENTAL_JDBC_TRACE, (Boolean) null);
        syncQueryTimeoutWithTransactionTimeout = remove(SYNC_QUERY_TIMEOUT_WITH_TRAN_TIMEOUT, false);
        transactional = remove(DataSourceDef.transactional.name(), true);
        validationTimeout = remove(VALIDATION_TIMEOUT, -1, 0, TimeUnit.SECONDS);

        commitOrRollbackOnCleanup = commitOrRollback == null
                        ? (transactional ? null : CommitOrRollbackOnCleanup.rollback)
                                        : commitOrRollback;

        if (trace && tc.isDebugEnabled() && entry != null)
            Tr.debug(this, tc, "unknown attributes: " + entries);
        // TODO: when we have a stricter variant of onError, apply it to unrecognized attributes
        //while (entry != null) {
        //    SQLException ex = AdapterUtil.ignoreWarnOrFail(tc, null, SQLException.class, "PROP_NOT_FOUND", jndiName == null ? id : jndiName, entry.getKey());
        //    if (ex != null)
        //        throw ex;
        //    entry = entries.pollFirstEntry();
        //}
        entries = null;

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, getClass().getSimpleName(), this);
    }

    /**
     * Returns information to log on first failure.
     * 
     * @return information to log on first failure.
     */
    @Override
    public String[] introspectSelf() {
        List<?> nameValuePairs = Arrays.asList(
                                               BEGIN_TRAN_FOR_SCROLLING_APIS, beginTranForResultSetScrollingAPIs,
                                               BEGIN_TRAN_FOR_VENDOR_APIS, beginTranForVendorAPIs,
                                               COMMIT_OR_ROLLBACK_ON_CLEANUP, commitOrRollbackOnCleanup,
                                               CONNECTION_SHARING, connectionSharing,
                                               DataSourceDef.isolationLevel.name(), isolationLevel,
                                               ResourceFactory.JNDI_NAME, jndiName,
                                               ENABLE_CONNECTION_CASTING, enableConnectionCasting,
                                               QUERY_TIMEOUT, queryTimeout,
                                               STATEMENT_CACHE_SIZE, statementCacheSize,
                                               SUPPLEMENTAL_JDBC_TRACE, supplementalJDBCTrace,
                                               SYNC_QUERY_TIMEOUT_WITH_TRAN_TIMEOUT, syncQueryTimeoutWithTransactionTimeout,
                                               DataSourceDef.transactional.name(), transactional
                                               );
        return new String[] {
                             toString(),
                             nameValuePairs.toString(),
                             PropertyService.hidePasswords(vendorProps).toString() };
    }

    /**
     * Remove properties up to and including the specified property. Return the property if found.
     * 
     * @param name name of the property.
     * @param defaultValue default value to use if not found.
     * @return value of the property if found. Otherwise the default value.
     */
    private Boolean remove(String name, Boolean defaultValue) throws SQLException {
        Boolean value = null;

        for (int diff; value == null && entry != null && (diff = entry.getKey().compareTo(name)) <= 0; entry = entries.pollFirstEntry()) {
            if (diff == 0) // matched
                value = entry.getValue() instanceof Boolean ? (Boolean) entry.getValue()
                                : Boolean.parseBoolean((String) entry.getValue());
            else {
                // TODO: when we have a stricter variant of onError, apply it to unrecognized attributes
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "unrecognized attribute: " + entry.getKey());
                //SQLException ex = AdapterUtil.ignoreWarnOrFail(tc, null, SQLException.class, "PROP_NOT_FOUND", jndiName == null ? id : jndiName, entry.getKey());
                //if (ex != null)
                //    throw ex;
            }
        }

        return value == null ? defaultValue : value;
    }

    /**
     * Remove properties up to and including the specified property. Return the property if found.
     * 
     * @param name name of the property.
     * @param defaultValue default value to use if not found.
     * @param type the enumeration class.
     * @return value of the property if found. Otherwise the default value.
     * @throws Exception if an error occurs.
     */
    private <E extends Enum<E>> E remove(String name, E defaultValue, Class<E> type) throws Exception {
        E value = null;

        for (int diff; value == null && entry != null && (diff = entry.getKey().compareTo(name)) <= 0; entry = entries.pollFirstEntry()) {
            if (diff == 0) // matched
                try {
                    value = (E) E.valueOf(type, (String) entry.getValue());
                } catch (Exception x) {
                    x = connectorSvc.ignoreWarnOrFail(null, x, x.getClass(), "UNSUPPORTED_VALUE_J2CA8011", entry.getValue(), name, jndiName == null ? id : jndiName);
                    if (x != null)
                        throw x;
                }
            else {
                // TODO: when we have a stricter variant of onError, apply it to unrecognized attributes
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "unrecognized attribute: " + entry.getKey());
                //SQLException ex = AdapterUtil.ignoreWarnOrFail(tc, null, SQLException.class, "PROP_NOT_FOUND", jndiName == null ? id : jndiName, entry.getKey());
                //if (ex != null)
                //    throw ex;
            }
        }

        return value == null ? defaultValue : value;
    }

    /**
     * Remove properties up to and including the specified property. Return the property if found.
     * 
     * @param name name of the property.
     * @param defaultValue default value to use if not found.
     * @param min minimum permitted value
     * @param units units for duration type. Null if not a duration.
     * @param range range of permitted values, in ascending order. If unspecified, then the range is min..Integer.MAX_VALUE
     * @return value of the property if found. Otherwise the default value.
     */
    private Integer remove(String name, Integer defaultValue, int min, TimeUnit units, int... range) throws Exception {
        Object value = null;

        for (int diff; value == null && entry != null && (diff = entry.getKey().compareTo(name)) <= 0; entry = entries.pollFirstEntry()) {
            if (diff == 0) // matched
                value = entry.getValue();
            else {
                // TODO: when we have a stricter variant of onError, apply it to unrecognized attributes
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "unrecognized attribute: " + entry.getKey());
                //SQLException ex = AdapterUtil.ignoreWarnOrFail(tc, null, SQLException.class, "PROP_NOT_FOUND", jndiName == null ? id : jndiName, entry.getKey());
                //if (ex != null)
                //    throw ex;
            }
        }

        long l;
        if (value == null)
            return defaultValue;
        else if (value instanceof Number)
            l = ((Number) value).longValue();
        else
            try {
                l = units == null ? Integer.parseInt((String) value)
                                : MetatypeUtils.evaluateDuration((String) value, units);
            } catch (Exception x) {
                x = connectorSvc.ignoreWarnOrFail(null, x, x.getClass(), "UNSUPPORTED_VALUE_J2CA8011", value, name, jndiName == null ? id : jndiName);
                if (x == null)
                    return defaultValue;
                else
                    throw x;
            }

        if (l < min || l > Integer.MAX_VALUE || range.length > 0 && Arrays.binarySearch(range, (int) l) < 0) {
            SQLNonTransientException x = connectorSvc.ignoreWarnOrFail
                            (null, null, SQLNonTransientException.class, "UNSUPPORTED_VALUE_J2CA8011", value, name, jndiName == null ? id : jndiName);
            if (x == null)
                return defaultValue;
            else
                throw x;
        }

        return (int) l;
    }

    /**
     * Remove properties up to and including the specified property. Return the property if found.
     * 
     * @param name name of the property.
     * @param defaultValue default value to use if not found.
     * @return value of the property if found. Otherwise the default value.
     */
    private String[] remove(String name, String[] defaultValue) throws SQLException {
        String[] value = null;

        for (int diff; value == null && entry != null && (diff = entry.getKey().compareTo(name)) <= 0; entry = entries.pollFirstEntry()) {
            if (diff == 0) // matched
                value = entry.getValue() instanceof String[] ? (String[]) entry.getValue() : new String[] { (String) entry.getValue() };
            else {
                // TODO: when we have a stricter variant of onError, apply it to unrecognized attributes
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "unrecognized attribute: " + entry.getKey());
                //SQLException ex = AdapterUtil.ignoreWarnOrFail(tc, null, SQLException.class, "PROP_NOT_FOUND", jndiName == null ? id : jndiName, entry.getKey());
                //if (ex != null)
                //    throw ex;
            }
        }

        return value == null ? defaultValue : value;
    }
}