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
package com.ibm.ws.wsat.part.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.sql.DataSource;

public class DBAccess {
    public String tableName;

    // Try to ensure TABLE exists
    public DBAccess(String table) {
        tableName = table;
        try {
            
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource)ctx.lookup("jdbc/TestDB");
            Connection conn = ds.getConnection();
            try {
                Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery("SELECT * FROM " + tableName);
                rs.close();
                s.close();
            } catch (Exception ee) {
                Statement s = conn.createStatement();
                s.execute("CREATE TABLE " + tableName + " (VALUE VARCHAR(200))");
                s.close();
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public String readValue() {
        String result = null;

        DataSource ds = null;
        Connection conn = null;
        Statement statement = null;
        try {
            InitialContext ctx = new InitialContext();
            ds = (DataSource)ctx.lookup("jdbc/TestDB");

            conn = ds.getConnection();
            statement = conn.createStatement();

            ResultSet rs = statement.executeQuery("SELECT * FROM " + tableName);
            while (rs.next()) {
                result = rs.getString(1);
            }
            rs.close();
            statement.close();

        } catch (Exception e) {
            e.printStackTrace();
            result = e.toString();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        }

        return result;
    }

    public void writeValue(String value) {
        DataSource ds = null;
        Connection conn = null;
        Statement statement = null;
        try {
            InitialContext ctx = new InitialContext();
            ds = (DataSource)ctx.lookup("jdbc/TestDB");

            conn = ds.getConnection();
            statement = conn.createStatement();

            statement.execute("INSERT INTO " + tableName + " VALUES('" + value + "')");
            statement.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }

    public void clearValues() {
        DataSource ds = null;
        Connection conn = null;
        Statement statement = null;
        try {
            InitialContext ctx = new InitialContext();
            ds = (DataSource)ctx.lookup("jdbc/TestDB");

            conn = ds.getConnection();
            statement = conn.createStatement();

            statement.execute("DELETE FROM " + tableName);
            statement.execute("INSERT INTO " + tableName + " VALUES('0')");
            statement.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        }
    }
}
