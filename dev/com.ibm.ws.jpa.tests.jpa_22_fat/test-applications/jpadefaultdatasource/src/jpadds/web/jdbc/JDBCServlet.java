/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package jpadds.web.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

@WebServlet(urlPatterns = { "/JDBCServlet" })
public class JDBCServlet extends HttpServlet {
    @Resource
    private UserTransaction tx;

    @Resource(lookup = "jdbc/JTA_DS1")
    private DataSource ds1Jta;

    @Resource(lookup = "jdbc/NJTA_DS1")
    private DataSource ds1Rl;

    @Resource(lookup = "jdbc/JTA_DS2")
    private DataSource ds2Jta;

    @Resource(lookup = "jdbc/NJTA_DS2")
    private DataSource ds2Rl;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        execRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        execRequest(req, resp);
    }

    private void execRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Enter JDBCServlet execRequest");
        Connection conn = null;
        try {
            conn = ds1Rl.getConnection();
            System.out.println("Creating tables for database #1...");
            createTables(conn, 1);
            conn.close();

            conn = ds2Rl.getConnection();
            System.out.println("Creating tables for database #2...");
            createTables(conn, 2);
            // Closed by finally block

            System.out.println("CREATE TABLES GOOD.");
            resp.getOutputStream().println("CREATE TABLES GOOD.");
        } catch (SQLException e) {
            throw new ServletException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable t) {
                }
            }

            System.out.println("Exit JDBCServlet execRequest");
        }
    }

    private void createTables(Connection conn, int id) throws ServletException {
        System.out.println("Executing createTables id = " + id);
        PreparedStatement ps = null;
        try {
            String sql = "CREATE TABLE DEFDSENTITY (ID INTEGER NOT NULL, STRDATA VARCHAR(255), PRIMARY KEY (ID))";
            System.out.println("Executing SQL: " + sql);
            ps = conn.prepareStatement(sql);
            boolean result1 = ps.execute();
            if (!result1) {
                int updateCount = ps.getUpdateCount();
                if (updateCount != 0) {
                    throw new RuntimeException("Failed to create new table: " + updateCount);
                }

            } else {
                throw new RuntimeException("A problem occurred creating table.");
            }
            ps.close();

            String sql2 = "INSERT INTO DEFDSENTITY (ID, STRDATA) VALUES (?, ?)";
            System.out.println("Executing SQL: " + sql2);
            ps = conn.prepareStatement(sql2);
            ps.setInt(1, id);
            ps.setString(2, Integer.toString(id));
            boolean result2 = ps.execute();
            if (!result2) {
                int updateCount = ps.getUpdateCount();
                if (updateCount != 1) {
                    throw new RuntimeException("Failed to insert into table: " + updateCount);
                }
            } else {
                throw new RuntimeException("A problem occurred inserting a row into the table.");
            }
            ps.close();

            if (!conn.getAutoCommit())
                conn.commit();
        } catch (SQLException e) {
            throw new ServletException(e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Throwable t) {
                }
            }
        }
    }
}
