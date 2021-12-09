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
package com.ibm.websphere.rsadapter;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.Subject;
import javax.transaction.xa.XAException;

import com.ibm.websphere.appprofile.accessintent.AccessIntent;
import com.ibm.websphere.ce.cm.DuplicateKeyException;
import com.ibm.websphere.ce.cm.StaleConnectionException;

/**
 * Simulates the legacy GenericDataStoreHelper, which is selected for the test JDBC driver
 * in the absence of heritage helperClass configuration.
 */
public class GenericDataStoreHelper {
    private final int defaultQueryTimeout;

    private AtomicReference<?> dsConfigRef;

    private Map<Object, Class<?>> exceptionIdentificationOverrides = Collections.emptyMap();

    private static final Map<Object, Class<?>> exceptionMap = new HashMap<Object, Class<?>>();
    {
        exceptionMap.put("08001", StaleConnectionException.class);
        exceptionMap.put("08003", StaleConnectionException.class);
        exceptionMap.put("08006", StaleConnectionException.class);
        exceptionMap.put("08S01", StaleConnectionException.class);
        exceptionMap.put("40003", StaleConnectionException.class);
        exceptionMap.put("55032", StaleConnectionException.class);
        exceptionMap.put("S1000", StaleConnectionException.class);
        exceptionMap.put(23505, DuplicateKeyException.class);
    }

    public GenericDataStoreHelper(Properties props) {
        String value = props == null ? null : props.getProperty("queryTimeout");
        defaultQueryTimeout = value == null || value.length() <= 0 ? 0 : Integer.parseInt(value);
    }

    public boolean doConnectionCleanup(Connection con) throws SQLException {
        return false;
    }

    public boolean doConnectionCleanupPerCloseConnection(Connection con, boolean isCMP, Object unused) throws SQLException {
        return false;
    }

    public void doConnectionSetup(Connection con) throws SQLException {
    }

    public boolean doConnectionSetupPerGetConnection(Connection con, boolean isCMP, Object props) throws SQLException {
        return false;
    }

    public void doConnectionSetupPerTransaction(Subject subject, String user, Connection con, boolean reauthRequired, Object props) throws SQLException {
    }

    public boolean doesStatementCacheIsoLevel() {
        return false;
    }

    public void doStatementCleanup(PreparedStatement stmt) throws SQLException {
        stmt.setCursorName(null);
        stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
        stmt.setMaxFieldSize(0);
        stmt.setMaxRows(0);

        Integer queryTimeout = dsConfigRef == null ? null : (Integer) readConfig("queryTimeout");
        if (queryTimeout == null)
            queryTimeout = defaultQueryTimeout;
        stmt.setQueryTimeout(queryTimeout);
    }

    public int getIsolationLevel(AccessIntent unused) {
        return Connection.TRANSACTION_READ_COMMITTED;
    }

    public GenericDataStoreHelper getMetaData() {
        return this;
    }

    public PrintWriter getPrintWriter() {
        return null;
    }

    public String getXAExceptionContents(XAException x) {
        return "cause: " + x.getCause();
    }

    public boolean isConnectionError(SQLException x) {
        return x instanceof SQLRecoverableException
               || x instanceof SQLNonTransientConnectionException
               || x instanceof StaleConnectionException
               || mapException(x) instanceof StaleConnectionException;
    }

    public boolean isUnsupported(SQLException x) {
        String sqlState = x.getSQLState();
        int errorCode = x.getErrorCode();
        return x instanceof SQLFeatureNotSupportedException
               || 0x0A000 == Math.abs(errorCode)
               || sqlState != null && sqlState.startsWith("0A")
               || sqlState != null && sqlState.startsWith("HYC00");
    }

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

    public int modifyXAFlag(int xaStartFlags) {
        return xaStartFlags;
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

    public void setConfig(Object configRef) {
        dsConfigRef = (AtomicReference<?>) configRef;
    }

    @SuppressWarnings("unchecked")
    public void setUserDefinedMap(@SuppressWarnings("rawtypes") Map map) {
        exceptionIdentificationOverrides = map;
    }

    public boolean supportsGetCatalog() {
        return true;
    }

    public boolean supportsGetNetworkTimeout() {
        return true;
    }

    public boolean supportsGetSchema() {
        return true;
    }

    public boolean supportsGetTypeMap() {
        return true;
    }

    public boolean supportsIsReadOnly() {
        return true;
    }

    public boolean supportsUOWDetection() {
        return false;
    }
}