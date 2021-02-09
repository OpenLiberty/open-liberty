/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.osgi.v40;

import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.concurrent.Executor;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.adapter.WSConnectionManager;
import com.ibm.ws.jdbc.osgi.JDBCRuntimeVersion;
import com.ibm.ws.rsadapter.impl.StatementCacheKey;
import com.ibm.ws.rsadapter.impl.WSConnectionRequestInfoImpl;
import com.ibm.ws.rsadapter.impl.WSManagedConnectionFactoryImpl;
import com.ibm.ws.rsadapter.impl.WSRdbManagedConnectionImpl;
import com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource;
import com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData;
import com.ibm.ws.rsadapter.jdbc.WSJdbcObject;
import com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement;
import com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet;
import com.ibm.ws.rsadapter.jdbc.WSJdbcStatement;

@Trivial
@Component(property = { "version=4.0", "service.ranking:Integer=40" })
public class JDBC40Runtime implements JDBCRuntimeVersion {
    @Override
    public Version getVersion() {
        return VERSION_4_0;
    }

    @Override
    public WSJdbcConnection newConnection(WSRdbManagedConnectionImpl mc, Connection conn, Object key, Object currentThreadID) {
        return new WSJdbcConnection(mc, conn, key, currentThreadID);
    }

    @Override
    public WSJdbcDatabaseMetaData newDatabaseMetaData(DatabaseMetaData metaDataImpl,
                                                      WSJdbcConnection connWrapper) throws SQLException {
        return new WSJdbcDatabaseMetaData(metaDataImpl, connWrapper);
    }

    @Override
    public WSJdbcDataSource newDataSource(WSManagedConnectionFactoryImpl mcf, WSConnectionManager connMgr) {
        return new WSJdbcDataSource(mcf, connMgr);
    }

    @Override
    public WSJdbcStatement newStatement(Statement stmtImplObject, WSJdbcConnection connWrapper, int theHoldability) {
        return new WSJdbcStatement(stmtImplObject, connWrapper, theHoldability);
    }

    @Override
    public WSJdbcPreparedStatement newPreparedStatement(PreparedStatement pstmtImplObject, WSJdbcConnection connWrapper,
                                                        int theHoldability, String pstmtSQL) throws SQLException {
        return new WSJdbcPreparedStatement(pstmtImplObject, connWrapper, theHoldability, pstmtSQL);
    }

    @Override
    public WSJdbcPreparedStatement newPreparedStatement(PreparedStatement pstmtImplObject, WSJdbcConnection connWrapper,
                                                        int theHoldability, String pstmtSQL,
                                                        StatementCacheKey pstmtKey) throws SQLException {
        return new WSJdbcPreparedStatement(pstmtImplObject, connWrapper, theHoldability, pstmtSQL, pstmtKey);
    }

    @Override
    public WSJdbcCallableStatement newCallableStatement(CallableStatement cstmtImplObject, WSJdbcConnection connWrapper,
                                                        int theHoldability, String cstmtSQL) throws SQLException {
        return new WSJdbcCallableStatement(cstmtImplObject, connWrapper, theHoldability, cstmtSQL);
    }

    @Override
    public WSJdbcCallableStatement newCallableStatement(CallableStatement cstmtImplObject, WSJdbcConnection connWrapper,
                                                        int theHoldability, String cstmtSQL,
                                                        StatementCacheKey cstmtKey) throws SQLException {
        return new WSJdbcCallableStatement(cstmtImplObject, connWrapper, theHoldability, cstmtSQL, cstmtKey);
    }

    @Override
    public WSJdbcResultSet newResultSet(ResultSet rsImpl, WSJdbcObject parent) {
        return new WSJdbcResultSet(rsImpl, parent);
    }

    @Override
    public BatchUpdateException newBatchUpdateException(BatchUpdateException copyFrom, String newMessage) {
        return new BatchUpdateException(newMessage, copyFrom.getSQLState(), copyFrom.getErrorCode(), copyFrom.getUpdateCounts());
    }

    @Override
    public void doSetSchema(Connection sqlConn, String schema) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public String doGetSchema(Connection sqlConn) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void doAbort(Connection sqlConn, Executor ex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void doSetNetworkTimeout(Connection sqlConn, Executor ex, int millis) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int doGetNetworkTimeout(Connection sqlConn) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Connection buildConnection(DataSource ds, String user, String password, WSConnectionRequestInfoImpl cri) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public PooledConnection buildPooledConnection(ConnectionPoolDataSource ds, String user, String password, WSConnectionRequestInfoImpl cri) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public XAConnection buildXAConnection(XADataSource ds, String user, String password, WSConnectionRequestInfoImpl cri) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void doSetShardingKeys(Connection con, Object shardingKey, Object superShardingKey) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean doSetShardingKeysIfValid(Connection con, Object shardingKey, Object superShardingKey, int timeout) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void beginRequest(Connection con) {
    }

    @Override
    public void endRequest(Connection con) {
    }
}
