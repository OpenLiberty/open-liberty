/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.osgi;

import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Executor;

import javax.resource.spi.ConnectionManager;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.osgi.framework.Version;

import com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource;
import com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData;
import com.ibm.ws.rsadapter.jdbc.WSJdbcObject;
import com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement;
import com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet;
import com.ibm.ws.rsadapter.jdbc.WSJdbcStatement;
import com.ibm.ws.rsadapter.impl.StatementCacheKey;
import com.ibm.ws.rsadapter.impl.WSConnectionRequestInfoImpl;
import com.ibm.ws.rsadapter.impl.WSManagedConnectionFactoryImpl;
import com.ibm.ws.rsadapter.impl.WSRdbManagedConnectionImpl;

/**
 * Interface used to proxy method calls to external packages which may require
 * a higher java version.
 */
public interface JDBCRuntimeVersion {

    public static final String VERSION = "version";
    public static final Version VERSION_4_0 = new Version(4, 0, 0);
    public static final Version VERSION_4_1 = new Version(4, 1, 0);
    public static final Version VERSION_4_2 = new Version(4, 2, 0);
    public static final Version VERSION_4_3 = new Version(4, 3, 0);

    public static final String SUPER_SHARDING_KEY_UNCHANGED = "UNCHANGED";

    public Version getVersion();

    // JDBC wrapper object constructor delegates
    public WSJdbcConnection newConnection(WSRdbManagedConnectionImpl mc,
                                          Connection conn,
                                          Object key,
                                          Object currentThreadID);

    public WSJdbcDatabaseMetaData newDatabaseMetaData(DatabaseMetaData metaDataImpl,
                                                      WSJdbcConnection connWrapper) throws SQLException;

    public WSJdbcDataSource newDataSource(WSManagedConnectionFactoryImpl mcf, ConnectionManager connMgr);

    public WSJdbcStatement newStatement(Statement stmtImplObject, WSJdbcConnection connWrapper, int theHoldability);

    public WSJdbcPreparedStatement newPreparedStatement(PreparedStatement pstmtImplObject, WSJdbcConnection connWrapper,
                                                        int theHoldability, String pstmtSQL) throws SQLException;

    public WSJdbcPreparedStatement newPreparedStatement(PreparedStatement pstmtImplObject, WSJdbcConnection connWrapper,
                                                        int theHoldability, String pstmtSQL,
                                                        StatementCacheKey pstmtKey) throws SQLException;

    public WSJdbcCallableStatement newCallableStatement(CallableStatement cstmtImplObject, WSJdbcConnection connWrapper,
                                                        int theHoldability, String cstmtSQL) throws SQLException;

    public WSJdbcCallableStatement newCallableStatement(CallableStatement cstmtImplObject, WSJdbcConnection connWrapper,
                                                        int theHoldability, String cstmtSQL,
                                                        StatementCacheKey cstmtKey) throws SQLException;

    public WSJdbcResultSet newResultSet(ResultSet rsImpl, WSJdbcObject parent);

    // JDBC 4.1 Connection methods
    public void doSetSchema(Connection sqlConn, String schema) throws SQLException;
    public String doGetSchema(Connection sqlConn) throws SQLException;
    public void doAbort(Connection sqlConn, Executor ex) throws SQLException;
    public void doSetNetworkTimeout(Connection sqlConn, Executor ex, int millis) throws SQLException;
    public int doGetNetworkTimeout(Connection sqlConn) throws SQLException;
    
    // JDBC 4.2 BatchUpdateException constructor
    public BatchUpdateException newBatchUpdateException(BatchUpdateException copyFrom, String newMessage);

    // JDBC 4.3 connection builder and Connection methods
    public Connection buildConnection(DataSource ds, String user, String password, WSConnectionRequestInfoImpl cri) throws SQLException;
    public PooledConnection buildPooledConnection(ConnectionPoolDataSource ds, String user, String password, WSConnectionRequestInfoImpl cri) throws SQLException;
    public XAConnection buildXAConnection(XADataSource ds, String user, String password, WSConnectionRequestInfoImpl cri) throws SQLException;
    public void doSetShardingKeys(Connection con, Object shardingKey, Object superShardingKey) throws SQLException;
    public boolean doSetShardingKeysIfValid(Connection con, Object shardingKey, Object superShardingKey, int timeout) throws SQLException;
    public void beginRequest(Connection con) throws SQLException;
    public void endRequest(Connection con) throws SQLException;
}
