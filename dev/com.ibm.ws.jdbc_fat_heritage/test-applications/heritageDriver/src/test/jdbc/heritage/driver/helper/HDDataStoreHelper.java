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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.security.auth.Subject;

import com.ibm.ws.jdbc.heritage.AccessIntent;
import com.ibm.ws.jdbc.heritage.DataStoreHelper;
import com.ibm.ws.jdbc.heritage.DataStoreHelperMetaData;

import test.jdbc.heritage.driver.HDConnection;

/**
 * Data store helper for the test JDBC driver.
 */
public class HDDataStoreHelper implements DataStoreHelper {
    private final HDDataStoreHelperMetaData metadata = new HDDataStoreHelperMetaData();

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
    public int getIsolationLevel(AccessIntent unused) {
        return Connection.TRANSACTION_SERIALIZABLE;
    }

    @Override
    public DataStoreHelperMetaData getMetaData() {
        return metadata;
    }
}