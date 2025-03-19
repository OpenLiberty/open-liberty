/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.informix.jdbcx;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.PooledConnection;

import com.informix.database.ConnectionManager;

public class IfxConnectionPoolDataSource implements javax.sql.ConnectionPoolDataSource {

    private String _userName = " ";
    private String _password = " ";
    private String _selectMethod = " ";
    private String _url = " ";
    private int _portNumber;
    private String _serverName = " ";
    private String _databaseName = " ";
    private String _driverType = " ";
    private ConnectionManager connManager;

    // The HATABLE is configured by a servlet at runtime. When testing startup
    // we want to be able
    // to configure the table for the next restart but don't want our change to
    // be visible during
    // shutdown.
    private static boolean testingFailoverAtRuntime = true;

    // CTOR
    public IfxConnectionPoolDataSource() {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource called");

        // Instantiate the class that has RDBMS vendor specific function
        connManager = new ConnectionManager();
    }

    @Override
    public PooledConnection getPooledConnection() throws SQLException {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.getPooledConnection");

        Connection conn = connManager.getDriverConnection(_serverName, _portNumber, _userName, _password, _selectMethod, _databaseName, _driverType);

        System.out.println("SIMHADB: getPooledConnection got connection - " + conn);
        IfxPooledConnection utc = new IfxPooledConnection(conn);
        System.out.println("SIMHADB: getPooledConnection return " + utc);
        return utc;
    }

    @Override
    public PooledConnection getPooledConnection(String theUser, String thePassword) throws SQLException {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.getPooledConnection(U,P)");

        IfxPooledConnection utc = null;

        System.out.println("SIMHADB: getPooledConnection return " + utc);
        return utc;
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
    }

    public String getPassword() {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.getPassword");
        return " ";
    }

    public void setPassword(String password) {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.setPassword: " + password);
        _password = password;
    }

    public String getSelectMethod() {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.getSelectMethod");
        return " ";
    }

    public void setSelectMethod(String selectMethod) {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.setSelectMethod: " + selectMethod);
        _selectMethod = selectMethod;
    }

    public int getPortNumber() {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.getPortNumber");
        return 0;
    }

    public void setPortNumber(int portNumber) {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.setPortNumber: " + portNumber);
        _portNumber = portNumber;
    }

    public String getUser() {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.getUser");
        return " ";
    }

    public void setUser(String user) {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.setUser: " + user);
        _userName = user;
    }

    public String getURL() {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.getURL");
        return _url;
    }

    public void setURL(String url) {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.setURL: " + url);
        _url = url;
    }

    public String getServerName() {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.getServerName");
        return " ";
    }

    public void setServerName(String create) {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.setServerName: " + create);
        _serverName = create;
    }

    public String getDatabaseName() {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.getDatabaseName");
        return " ";
    }

    public void setDatabaseName(String dbname) {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.setDatabaseName: " + dbname);
        _databaseName = dbname;
    }

    public String getDriverType() {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.getDriverType");
        return " ";
    }

    public void setDriverType(String driverType) {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.setDriverType: " + driverType);
        _driverType = driverType;
    }

    public String getCreateDatabase() {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.getCreateDatabase");
        return " ";
    }

    public void setCreateDatabase(String createDatabase) {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.setCreateDatabase: " + createDatabase);
    }

    /**
     * @return the startupPhase
     */
    public static boolean isTestingFailoverAtRuntime() {
        return testingFailoverAtRuntime;
    }

    /**
     * @param startupPhase
     *            the startupPhase to set
     */
    public static void setTestingFailoverAtRuntime(boolean startupPhase) {
        IfxConnectionPoolDataSource.testingFailoverAtRuntime = startupPhase;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sql.CommonDataSource#getParentLogger()
     */
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
