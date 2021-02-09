/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.informix.jdbcx;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.PooledConnection;

public class IfxConnectionPoolDataSource implements javax.sql.ConnectionPoolDataSource {

    private String _userName = " ";
    private String _password = " ";
    private String _selectMethod = " ";
    private String _url = " ";

    // The HATABLE is configured by a servlet at runtime. When testing startup
    // we want to be able
    // to configure the table for the next restart but don't want our change to
    // be visible during
    // shutdown.
    private static boolean testingFailoverAtRuntime = true;

    // CTOR
    public IfxConnectionPoolDataSource() {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource called");

        // Load the JDBC driver to "register" the SQL Server DriverManager class
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver").getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException
                        | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("SIMHADB: IfxConnectionPoolDataSource - have registered driver");
    }

    @Override
    public PooledConnection getPooledConnection() throws SQLException {
        System.out.println("SIMHADB: IfxConnectionPoolDataSource.getPooledConnection");

        // These parameters have been set through the JDBCDriverService class.
        System.out.println("SIMHADB: getPooledConnection URL - " + _url);
        System.out.println("SIMHADB: getPooledConnection user - " + _userName + ", password - " + _password + ", selectMethod - " + _selectMethod);
        Properties mssqlProps = new Properties();
        mssqlProps.put("user", _userName);
        mssqlProps.put("password", _password);
        mssqlProps.put("selectMethod", _selectMethod);

        // For MS SQL Server call the getConnection method of the DriverManager class. The DriverManager is "registered" in the IfxConnectionPoolDataSource constructor.
        //
        // Note this is not a pooled connection but a raw connection is perfectly good for our requirements.
        Connection conn = DriverManager.getConnection(_url, mssqlProps);

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
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        // TODO Auto-generated method stub

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
        // TODO Auto-generated method stub
        return null;
    }
}
