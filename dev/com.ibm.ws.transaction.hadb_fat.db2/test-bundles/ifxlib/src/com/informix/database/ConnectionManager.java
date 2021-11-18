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

/**
 *
 */
public class ConnectionManager {
    public static String dbProductName = "DB2";
    public static String dbVersionNumber = "v1";

    public ConnectionManager() {
        System.out.println("SIMHADB: ConnectionManager called");
        registerDriver();
    }

    private void registerDriver() {
        System.out.println("SIMHADB: registerDriver called");

        // Load the JDBC driver to "register" the DB2 DriverManager class
        try {
            Class.forName("com.ibm.db2.jcc.DB2Driver").getConstructor().newInstance();
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

        String url = "jdbc:db2:" + "//" + serverName + ":" + portNumber + "/" + databaseName;
        System.out.println("SIMHADB: getDriverConnection URL - " + url);
        System.out.println("SIMHADB: getDriverConnection user - " + userName + ", password - " + password + ", selectMethod - " + selectMethod);

        // For DB2 call the getConnection method of the DriverManager class. The DriverManager is "registered" in the IfxConnectionPoolDataSource constructor.
        //
        // Note this is not a pooled connection but a raw connection is perfectly good for our requirements.
        conn = DriverManager.getConnection(url, userName, password);
        System.out.println("SIMHADB: getDriverConnection got connection - " + conn);

        return conn;
    }

    public static int resetTransactionIsolationLevel(int level) {
        // In Oracle set to READ COMMITTED
        System.out.println("SIMHADB: resetTransactionIsolationLevel, level was - " + level + ", set to READ_COMMITTED");
        int newlevel = java.sql.Connection.TRANSACTION_READ_COMMITTED;
        return newlevel;
    }
}
