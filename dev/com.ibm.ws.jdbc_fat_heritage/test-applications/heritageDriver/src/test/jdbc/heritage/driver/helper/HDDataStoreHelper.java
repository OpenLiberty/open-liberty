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
package test.jdbc.heritage.driver.helper;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.Subject;
import javax.transaction.xa.XAException;

import com.ibm.websphere.ce.cm.DuplicateKeyException;
import com.ibm.websphere.ce.cm.StaleConnectionException;
import com.ibm.websphere.ce.cm.StaleStatementException;
import com.ibm.ws.jdbc.heritage.AccessIntent;
import com.ibm.ws.jdbc.heritage.DataStoreHelperMetaData;
import com.ibm.ws.jdbc.heritage.GenericDataStoreHelper;

import test.jdbc.heritage.driver.HDConnection;

/**
 * Data store helper for the test JDBC driver.
 */
public class HDDataStoreHelper extends GenericDataStoreHelper {
    private final HDDataStoreHelperMetaData metadata = new HDDataStoreHelperMetaData();

    private final int defaultQueryTimeout;

    private AtomicReference<?> dsConfigRef;

    private Map<Object, Class<?>> exceptionIdentificationOverrides;

    private static final Map<Object, Class<?>> exceptionMap = new HashMap<Object, Class<?>>();
    {
        exceptionMap.put(44098, StaleConnectionException.class); // vendor-specific error code for when isValid fails
        exceptionMap.put("22013", StaleStatementException.class);
        exceptionMap.put("08000", StaleConnectionException.class);
        exceptionMap.put("08001", HeritageDBStaleConnectionException.class);
        exceptionMap.put("08003", StaleConnectionException.class);
        exceptionMap.put("08004", StaleConnectionException.class); // identifyException removes this mapping
        exceptionMap.put("08006", StaleConnectionException.class);
        exceptionMap.put("0A000", HeritageDBFeatureUnavailableException.class);
        exceptionMap.put("23000", DuplicateKeyException.class);
    }

    public HDDataStoreHelper(Properties props) {
        String value = props == null ? null : props.getProperty("queryTimeout");
        defaultQueryTimeout = value == null || value.length() <= 0 ? 0 : Integer.parseInt(value);
    }

    @Override
    public boolean doConnectionCleanup(Connection con) throws SQLException {
        ((HDConnection) con).setClientInfoKeys(); // defaults
        return false;
    }

    @Override
    public boolean doConnectionCleanupPerCloseConnection(Connection con, boolean isCMP, Object unused) throws SQLException {
        ((HDConnection) con).cleanupCount.incrementAndGet();
        try (CallableStatement stmt = con.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_STATISTICS_TIMING(0)")) {
            stmt.execute();
        }
        return true;
    }

    @Override
    public void doConnectionSetup(Connection con) throws SQLException {
        ((HDConnection) con).setExceptionIdentificationOverrides(exceptionIdentificationOverrides);

        try (CallableStatement stmt = con.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_RUNTIMESTATISTICS(1)")) {
            stmt.setPoolable(false);
            stmt.execute();
        }
    }

    @Override
    public boolean doConnectionSetupPerGetConnection(Connection con, boolean isCMP, Object props) throws SQLException {
        ((HDConnection) con).setupCount.incrementAndGet();
        try (CallableStatement stmt = con.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_STATISTICS_TIMING(1)")) {
            stmt.execute();
        }
        return true;
    }

    @Override
    public void doConnectionSetupPerTransaction(Subject subject, String user, Connection con, boolean reauthRequired, Object props) throws SQLException {
        AtomicInteger count = ((HDConnection) con).transactionCount;
        boolean first = Boolean.parseBoolean(((Properties) props).getProperty("FIRST_TIME_CALLED"));
        if (first)
            count.set(1);
        else
            count.incrementAndGet();
    }

    @Override
    public void doStatementCleanup(PreparedStatement stmt) throws SQLException {
        stmt.setCursorName(null);
        stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
        stmt.setMaxFieldSize(HDConnection.DEFAULT_MAX_FIELD_SIZE);
        stmt.setMaxRows(0);

        Integer queryTimeout = dsConfigRef == null ? null : (Integer) readConfig("queryTimeout");
        if (queryTimeout == null)
            queryTimeout = defaultQueryTimeout;
        stmt.setQueryTimeout(queryTimeout);
    }

    @Override
    public int getIsolationLevel(AccessIntent unused) {
        return Connection.TRANSACTION_SERIALIZABLE;
    }

    @Override
    public DataStoreHelperMetaData getMetaData() {
        return metadata;
    }

    @Override
    public PrintWriter getPrintWriter() {
        // Redirects to System.out instead of OpenLiberty trace, which will cause output to go into message.log where the test can scan for it
        return new PrintWriter(new OutputStreamWriter(System.out));
    }

    @Override
    public String getXAExceptionContents(XAException x) {
        // This ought to be unreachable for non-xa-capable javax.sql.DataSource.
        throw new UnsupportedOperationException("This driver does not provide an XADataSource.");
    }

    @Override
    public boolean isConnectionError(SQLException x) {
        return x instanceof SQLRecoverableException
               || x instanceof SQLNonTransientConnectionException
               || x instanceof StaleConnectionException
               || mapException(x) instanceof StaleConnectionException;
    }

    @Override
    public SQLException mapException(SQLException x) {
        String sqlState = x.getSQLState();
        int errorCode = x.getErrorCode();
        Class<?> exceptionClass = exceptionIdentificationOverrides.get(errorCode);
        if (exceptionClass == null) {
            exceptionClass = exceptionIdentificationOverrides.get(sqlState);
            if (exceptionClass == null) {
                exceptionClass = exceptionMap.get(errorCode);
                if (exceptionClass == null) {
                    exceptionClass = exceptionMap.get(sqlState);
                    if (exceptionClass == null)
                        return x;
                }
            }
        }

        System.out.println("datastorehelper.mapException sqlState: " + sqlState + ", errorCode: " + errorCode + " " + x.getClass().getName());
        System.out.println("  --> " + exceptionClass.getName());
        System.out.println("  based on map:  " + exceptionMap);
        System.out.println("  and overrides: " + exceptionIdentificationOverrides);

        if (Void.class.equals(exceptionClass))
            return x;

        try {
            @SuppressWarnings("unchecked")
            final Class<? extends SQLException> sqlXClass = (Class<? extends SQLException>) exceptionClass;
            return AccessController.doPrivileged((PrivilegedExceptionAction<SQLException>) () -> {
                Constructor<? extends SQLException> ctor = sqlXClass.getConstructor(SQLException.class);
                return ctor.newInstance(x);
            });
        } catch (PrivilegedActionException privX) {
            return new SQLException(privX);
        }
    }

    private Object readConfig(String fieldName) {
        Object dsConfig = dsConfigRef.get();
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<?>) () -> //
            dsConfig.getClass().getField(fieldName).get(dsConfig));
        } catch (PrivilegedActionException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public void setConfig(Object configRef) {
        dsConfigRef = (AtomicReference<?>) configRef;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setUserDefinedMap(@SuppressWarnings("rawtypes") Map map) {
        exceptionIdentificationOverrides = map;
    }
}