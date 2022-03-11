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
package com.informix.database;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 *
 */
public class ConnectionManager {

    public static String dbProductName = "Microsoft SQL Server";
    public static String dbVersionNumber = "v1";

    public ConnectionManager() {
        System.out.println("SIMHADB: ConnectionManager called");
        registerDriver();
    }

    private void registerDriver() {
        System.out.println("SIMHADB: registerDriver called");

        // Load the JDBC driver to "register" the SQL Server DriverManager class
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver").getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException
                        | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("SIMHADB: registerDriver - have registered driver");
    }

    public Connection getDriverConnection(String serverName, int portNumber, String userName, String password, String selectMethod, String databaseName,
                                          String driverType) throws SQLException {
        Connection conn = null;
        System.out.println("SIMHADB: ConnectionManager.getDriverConnection");
        String url = "jdbc:sqlserver://" + serverName + ":" + portNumber;
        System.out.println("SIMHADB: getDriverConnection URL - " + url);
        System.out.println("SIMHADB: getDriverConnection user - " + userName + ", password - " + password + ", selectMethod - " + selectMethod);
        Properties mssqlProps = new Properties();
        mssqlProps.put("user", userName);
        mssqlProps.put("password", password);
        mssqlProps.put("selectMethod", selectMethod);
        mssqlProps.put("portNumber", String.valueOf(portNumber));
        // For MS SQL Server call the getConnection method of the DriverManager class. The DriverManager is "registered" in the IfxConnectionPoolDataSource constructor.
        //
        // Note this is not a pooled connection but a raw connection is perfectly good for our requirements.
        conn = DriverManager.getConnection(url, mssqlProps);
        System.out.println("SIMHADB: getDriverConnection got connection - " + conn);

        return conn;
    }

    public static int resetTransactionIsolationLevel(int level) {
        // A noop
        int newlevel = level;
        return newlevel;
    }
}
