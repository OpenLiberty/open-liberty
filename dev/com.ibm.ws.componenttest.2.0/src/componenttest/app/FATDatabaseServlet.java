/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.app;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

/**
 * Helper methods for working with JDBC tables. In general, any methods that take a
 * <code>Connection</code> object use the connection without closing it. Any methods
 * that take a <code>DataSource</code> object get, use, and close their own connection.
 */
@SuppressWarnings("serial")
public abstract class FATDatabaseServlet extends FATServlet {
    /**
     * Creates a table with the given name and schema. Gets a new connection and closes it.
     */
    public static void dropAndCreateTable(DataSource ds, String tableName, String tableSchema) {
        try {
            Connection conn = ds.getConnection();
            try {
                dropTableSafely(conn, tableName);
                createTable(conn, tableName, tableSchema);
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * Creates a table with the given name and the following schema:<br>
     * <code>id int not null primary key, val varchar(30)</code><br><br>
     * Gets a new connection and closes it.
     */
    public static void dropAndCreateBasicTable(DataSource ds, String tableName) {
        try {
            Connection conn = ds.getConnection();
            try {
                dropTableSafely(conn, tableName);
                createBasicTable(conn, tableName);
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * Creates a table with the given name and schema, using an existing connection.
     * Does not close the connection.
     */
    public static void dropAndCreateTable(Connection conn, String tableName, String tableSchema) throws SQLException {
        dropTableSafely(conn, tableName);
        createTable(conn, tableName, tableSchema);
    }

    /**
     * Creates a table with the given name and the following schema:<br>
     * <code>id int not null primary key, val varchar(30)</code><br>
     * Does not close the connection.
     */
    public static void dropAndCreateBasicTable(Connection conn, String tableName) throws SQLException {
        dropTableSafely(conn, tableName);
        createBasicTable(conn, tableName);
    }

    /**
     * Creates a table with the given name and the following schema:<br>
     * <code>id int not null primary key, val varchar(30)</code><br>
     * Does not close the connection.
     */
    public static void createBasicTable(Connection conn, String tableName) throws SQLException {
        createTable(conn, tableName, "id int not null primary key, val varchar(30)");
    }

    public static void createTable(DataSource ds, String tableName, String tableSchema) {
        try {
            Connection conn = ds.getConnection();
            try {
                createTable(conn, tableName, tableSchema);
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * Creates a table with the given name and schema. Does not close the connection.
     */
    public static void createTable(Connection conn, String tableName, String tableSchema) throws SQLException {
        Statement stmt1 = conn.createStatement();
        stmt1.executeUpdate("create table " + tableName + " (" + tableSchema + ")");
        stmt1.close();
    }

    /**
     * Drops a table. Gets a new connection and closes it.
     */
    public static void dropTable(DataSource ds, String tableName) {
        try {
            Connection conn = ds.getConnection();
            try {
                dropTableSafely(conn, tableName);
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * Drops a table, and does not throw an exception if the table does not exist.
     * Does not close the connection.
     */
    public static void dropTableSafely(Connection conn, String tableName) {
        try {
            Statement stmt1 = conn.createStatement();
            try {
                stmt1.executeUpdate("drop table " + tableName);
            } catch (SQLException ignore) {
            }
            stmt1.close();
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * Drops a table with the given name. Does not close the connection.
     */
    public static void dropTable(Connection conn, String tableName) throws SQLException {
        Statement stmt1 = conn.createStatement();
        stmt1.executeUpdate("drop table " + tableName);
        stmt1.close();
    }

    /**
     * Deletes all contents of a table. Opens a new connection and closes it.
     */
    public static void clearTable(DataSource ds, String tableName) {
        try {
            Connection conn = ds.getConnection();
            try {
                clearTable(conn, tableName);
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * Deletes all contents of a table using an existing connection. Does not close the connection.
     */
    public static void clearTable(Connection conn, String tableName) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("DELETE FROM " + tableName);
        conn.close();
    }

}