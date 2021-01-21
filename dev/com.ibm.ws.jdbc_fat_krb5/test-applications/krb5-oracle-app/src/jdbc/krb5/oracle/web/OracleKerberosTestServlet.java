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
package jdbc.krb5.oracle.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/OracleKerberosTestServlet")
public class OracleKerberosTestServlet extends FATServlet {

    @Resource(lookup = "jdbc/nokrb5")
    DataSource noKrb5;

    @Resource(lookup = "jdbc/krb/basic")
    DataSource krb5DataSource;

    @Resource(lookup = "jdbc/krb/xa")
    DataSource krb5XADataSource;

    @Resource(lookup = "jdbc/krb/invalidPrincipal")
    DataSource invalidPrincipalDs;

    @Resource(lookup = "jdbc/krb/DataSource")
    DataSource krb5RegularDs;

    /**
     * Getting a connection too soon after the initial ticket is obtained can cause intermittent
     * issues where we getConnection() fails with: Oracle Error ORA-12631
     * These timing issues can be reproduced in a standalone JDBC program, which indicates that
     * we aren't doing anything wrong in the Liberty code, and instead this is due a Oracle driver
     * or DB issue which would require an Oracle support contract to investigate further
     */
    private static Connection getConnectionWithRetry(DataSource ds) throws SQLException {
        SQLException firstEx = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                return ds.getConnection();
            } catch (Exception e) {
                //Try to find nested SQLExceptions
                SQLException found = null;
                for (Throwable t = e; t.getCause() != null; t = t.getCause()) {
                    if (t instanceof SQLException) {
                        found = (SQLException) t;
                    }
                }
                //If none was found throw original exception
                if (found == null)
                    throw e;
                //Keep track of the first SQLException we found
                if (firstEx == null)
                    firstEx = found;
                //Check to see if we failed with ORA-12631 if so attempt again
                if (found.getMessage() != null && found.getMessage().contains("ORA-12631")) {
                    System.out.println("getConnection attempt " + attempt + " failed with ORA-12631");
                    waitFor(3_000);
                    continue;
                    //Otherwise, throw the original exception
                } else {
                    throw e;
                }
            }
        }
        //After 5 attempts, throw the first SQLException we stored
        throw firstEx;
    }

    private static void waitFor(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // see getConnectionWithRetry for reasoning behind wait
        waitFor(750);
        super.doGet(request, response);
    }

    @Test
    public void testNonKerberosConnection() throws Exception {
        try (Connection con = getConnectionWithRetry(noKrb5)) {
            con.createStatement().execute("SELECT 1 FROM DUAL");
        }
    }

    /**
     * Get a connection from a javax.sql.ConnectionPoolDataSource
     */
    @Test
    public void testKerberosBasicConnection() throws Exception {
        try (Connection con = getConnectionWithRetry(krb5DataSource)) {
            con.createStatement().execute("SELECT 1 FROM DUAL");
        }
    }

    /**
     * Get a connection from a javax.sql.XADataSource
     */
    @Test
    public void testKerberosXAConnection() throws Exception {
        try (Connection con = getConnectionWithRetry(krb5XADataSource)) {
            con.createStatement().execute("SELECT 1 FROM DUAL");
        }
    }

    /**
     * Get a connection from a javax.sql.DataSource
     */
    @Test
    public void testKerberosRegularConnection() throws Exception {
        try (Connection con = getConnectionWithRetry(krb5RegularDs)) {
            con.createStatement().execute("SELECT 1 FROM DUAL");
        }
    }

    @Test
    @AllowedFFDC
    public void testInvalidPrincipal() throws Exception {
        try (Connection con = invalidPrincipalDs.getConnection()) {
            con.createStatement().execute("SELECT 1 FROM DUAL");
            fail("Should not be able to get a connection using an invalid principal");
        } catch (SQLException expected) {
            Throwable cause = expected.getCause();
            assertNotNull("Expected SQLException to have a cause", cause);
            assertEquals("javax.resource.ResourceException", cause.getClass().getCanonicalName());

            cause = cause.getCause();
            assertNotNull("Expected ResourceException to have a cause", cause);
            assertTrue("Expected cause to be instanceof LoginException but was: " + cause.getClass().getCanonicalName(),
                       cause instanceof LoginException);
        }
    }

    /**
     * Get two connection handles from the same datasource.
     * Ensure that both connection handles share the same managed connection (i.e. phyiscal connection)
     * to prove that Subject reuse is working
     */
    @Test
    public void testConnectionReuse() throws Exception {
        String managedConn1 = null;
        String managedConn2 = null;

        try (Connection conn = getConnectionWithRetry(krb5DataSource)) {
            managedConn1 = getManagedConnectionID(conn);
            System.out.println("Managed connection 1 is: " + managedConn1);
        }

        try (Connection conn = getConnectionWithRetry(krb5DataSource)) {
            managedConn2 = getManagedConnectionID(conn);
            System.out.println("Managed connection 2 is: " + managedConn2);
        }

        assertEquals("Expected two connections from the same datasource to share the same underlying managed connection",
                     managedConn1, managedConn2);
    }

    /**
     * Get the managed connection ID of a given Connection
     * The managed connection ID is an implementation detail of Liberty that a real app would never care
     * about, but it's a simple way for us to verify that the underlying managed connections are being
     * reused.
     */
    private String getManagedConnectionID(Connection conn1) {
        for (Class<?> clazz = conn1.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                Field f1 = clazz.getDeclaredField("managedConn");
                f1.setAccessible(true);
                String mc1 = String.valueOf(f1.get(conn1));
                f1.setAccessible(false);
                return mc1;
            } catch (Exception ignore) {
            }
        }
        throw new RuntimeException("Did not find field 'managedConn' on " + conn1.getClass());
    }

}
