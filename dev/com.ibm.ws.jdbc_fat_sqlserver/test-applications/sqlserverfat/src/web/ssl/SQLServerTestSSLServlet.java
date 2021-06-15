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
package web.ssl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SQLServerTestSSLServlet")
public class SQLServerTestSSLServlet extends FATServlet {

    private static final int RETRY_COUNT = 6;
    private static final long RETRY_WAIT = Duration.ofSeconds(10).toMillis();

    @Resource(lookup = "jdbc/sqlserver-ssl-unsecure")
    DataSource unSecureDs;

    @Resource(lookup = "jdbc/sqlserver-ssl-secure")
    DataSource secureDs;

    private Connection getConnectionWithRetry(DataSource ds) throws SQLException, InterruptedException {
        Connection con = null;
        SQLException firstException = null;
        for (int i = 0; i++ < RETRY_COUNT; Thread.sleep(RETRY_WAIT)) {
            try {
                System.out.println("Attempting to create a connection with retry. Attempt: " + i);
                con = ds.getConnection();
            } catch (SQLException sqle) {
                if (firstException == null) {
                    firstException = sqle;
                }

                //08S01 is a communication error assume that the database process is still starting
                if (sqle.getSQLState() == "08S01") {
                    //ignore
                } else {
                    throw sqle;
                }
            }
        }

        if (con != null) {
            return con;
        } else {
            System.out.println("Attempted get connection " + RETRY_COUNT + " time(s) and was unable to get connection.");
            throw firstException;
        }
    }

    public void testConnectionWithSSLSecure() throws Exception {
        try (Connection con = getConnectionWithRetry(secureDs); Statement stmt = con.createStatement()) {
            stmt.executeUpdate("INSERT INTO MYTABLE VALUES (1, 'one')");
            ResultSet rs = stmt.executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID = 1");
            assertTrue("Query should have returned a result", rs.next());
            assertEquals("Unexpected value returned", "one", rs.getString(1));
        }
    }

    @Test
    public void testConnectionWithSSLUnSecure() throws Exception {
        try (Connection con = getConnectionWithRetry(unSecureDs); Statement stmt = con.createStatement()) {
            stmt.executeUpdate("INSERT INTO MYTABLE VALUES (2, 'two')");
            ResultSet rs = stmt.executeQuery("SELECT STRVAL FROM MYTABLE WHERE ID = 2");
            assertTrue("Query should have returned a result", rs.next());
            assertEquals("Unexpected value returned", "two", rs.getString(1));
        }
    }
}
