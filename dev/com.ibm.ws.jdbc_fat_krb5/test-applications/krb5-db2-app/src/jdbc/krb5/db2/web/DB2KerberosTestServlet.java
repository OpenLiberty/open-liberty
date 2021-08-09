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
package jdbc.krb5.db2.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.security.auth.login.LoginException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/DB2KerberosTestServlet")
public class DB2KerberosTestServlet extends FATServlet {

    @Resource
    UserTransaction tran;

    @Resource(lookup = "jdbc/nokrb5")
    DataSource noKrb5;

    @Resource(lookup = "jdbc/krb/basic")
    DataSource krb5DataSource;

    @Resource(lookup = "jdbc/krb/xa")
    DataSource krb5XADataSource;

    @Resource(lookup = "jdbc/krb/invalidPrincipal")
    DataSource invalidPrincipalDs;

    @Resource(lookup = "jdbc/krb/basicPassword")
    DataSource basicPassword;

    @Resource(lookup = "jdbc/krb/DataSource")
    DataSource regularDs;

    // The jdbc/noAuth datasource would normally fail due to no credentials,
    // but since we have bound the 'krb5Auth' auth alias to 'java:app/env/jdbc/reboundAuth'
    // in the ibm-web-bnd.xml it will work
    @Resource(lookup = "jdbc/noAuth", name = "java:app/env/jdbc/reboundAuth")
    DataSource reboundAuth;

    // Uses kerberos authData for container auth and recovery auth
    // Unshareable so we we can have multiple XA resources to cause in-doubt tran
    @Resource(lookup = "jdbc/krb/xaRecovery", shareable = false)
    DataSource xaRecoveryDs;

    /**
     * Attempt to get a connection from a datasource that has basic auth configured, which
     * should fail because the backend DB2 database has been configured to require Kerberos
     */
    @Test
    @AllowedFFDC
    public void testNonKerberosConnectionRejected() throws Exception {
        try (Connection con = noKrb5.getConnection()) {
            throw new Exception("Should not be able to obtain a non-kerberos connection from a kerberos-only database");
        } catch (SQLInvalidAuthorizationSpecException e) {
            System.out.println("Got expected error getting non-kerberos connection");
        }
    }

    /**
     * Get a connection with a javax.sql.ConnectionPoolDataSource
     */
    @Test
    public void testKerberosBasicConnection() throws Exception {
        try (Connection con = krb5DataSource.getConnection()) {
            con.createStatement().execute("SELECT 1 FROM SYSIBM.SYSDUMMY1");
        }
    }

    public void testTicketCache() throws Exception {
        try (Connection con = krb5DataSource.getConnection()) {
            con.createStatement().execute("SELECT 1 FROM SYSIBM.SYSDUMMY1");
        }
    }

    public void testTicketCacheExpired() throws Exception {
        try (Connection con = krb5DataSource.getConnection()) {
            fail("The expired ticket should result in a LoginException");
        } catch (SQLException expected) {
            Throwable cause1 = expected.getCause();
            if (cause1.getClass().getCanonicalName().contains("ResourceException")) { // javax.resource.ResourceException is not on the cp
                Throwable cause2 = cause1.getCause();
                if (cause2 instanceof LoginException) {
                    System.out.println("Caught expected SQLException with nested LoginException");
                    return;
                }
            }
            throw expected;
        }
    }

    public void testTicketCacheInvalid() throws Exception {
        try (Connection con = krb5DataSource.getConnection()) {
            fail("Should not be able to get a connection with invalid krb5TicketCache");
        } catch (SQLException expected) {
            Throwable cause1 = expected.getCause();
            if (cause1.getClass().getCanonicalName().contains("ResourceException")) { // javax.resource.ResourceException is not on the cp
                Throwable cause2 = cause1.getCause();
                if (cause2 instanceof LoginException) {
                    System.out.println("Caught expected SQLException with nested LoginException");
                    return;
                }
            }
            throw expected;
        }
    }

    public void testPasswordInvalid() throws Exception {
        try (Connection con = krb5DataSource.getConnection()) {
            fail("Should not be able to get a connection with invalid krb5TicketCache");
        } catch (SQLException expected) {
            Throwable cause1 = expected.getCause();
            if (cause1.getClass().getCanonicalName().contains("ResourceException")) { // javax.resource.ResourceException is not on the cp
                Throwable cause2 = cause1.getCause();
                if (cause2 instanceof LoginException) {
                    System.out.println("Caught expected SQLException with nested LoginException");
                    return;
                }
            }
            throw expected;
        }
    }

    /**
     * Get a connection with a javax.sql.XADatasource
     */
    @Test
    public void testKerberosXAConnection() throws Exception {
        try (Connection con = krb5XADataSource.getConnection()) {
            con.createStatement().execute("SELECT 1 FROM SYSIBM.SYSDUMMY1");
        }
    }

    /**
     * Get a connection with a javax.sql.Datasource
     */
    @Test
    public void testKerberosRegularConnection() throws Exception {
        try (Connection con = regularDs.getConnection()) {
            con.createStatement().execute("SELECT 1 FROM SYSIBM.SYSDUMMY1");
        }
    }

    /**
     * Test getting a connection from a datasource that has no auth-data configured
     * in server.xml, which would normally fail, but has a valid auth-data alias
     * configured in ibm-web-bnd.xml and therefore should be able to get a connection
     */
    @Test
    public void testReboundAuthAlias() throws Exception {
        try (Connection con = reboundAuth.getConnection()) {
            con.createStatement().execute("SELECT 1 FROM SYSIBM.SYSDUMMY1");
        }
    }

    /**
     * Before this test runs the server config is modified to remove the keytab attribute from
     * kerberos config. Then, we attempt to get a connection to a datasource that supplies a
     * password using <authData password="..."/>
     */
    public void testBasicPassword() throws Exception {
        try (Connection con = basicPassword.getConnection()) {
            con.createStatement().execute("SELECT 1 FROM SYSIBM.SYSDUMMY1");
        }
    }

    /**
     * Test that attempting to get connection using a bogus principal (i.e. bogus username)
     * is rejected. This is a negative test that will give us confidence that the positive
     * tests are actually using kerberos.
     */
    @Test
    @AllowedFFDC
    public void testInvalidPrincipal() throws Exception {
        try (Connection con = invalidPrincipalDs.getConnection()) {
            con.createStatement().execute("SELECT 1 FROM SYSIBM.SYSDUMMY1");
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

        try (Connection conn = krb5DataSource.getConnection()) {
            managedConn1 = getManagedConnectionID(conn);
            System.out.println("Managed connection 1 is: " + managedConn1);
        }

        try (Connection conn = krb5DataSource.getConnection()) {
            managedConn2 = getManagedConnectionID(conn);
            System.out.println("Managed connection 2 is: " + managedConn2);
        }

        assertEquals("Expected two connections from the same datasource to share the same underlying managed connection",
                     managedConn1, managedConn2);

    }

    /**
     * Cause an in-doubt transaction and verify that XA recovery resolves it.
     * The recoveryAuthData should be used for recovery.
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "javax.transaction.xa.XAException", "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
    public void testXARecovery() throws Throwable {
        initTable(xaRecoveryDs);
        Connection[] cons = new Connection[3];
        tran.begin();
        try {
            // Use unsharable connections, so that they all get their own XA resources
            cons[0] = xaRecoveryDs.getConnection();
            cons[1] = xaRecoveryDs.getConnection();
            cons[2] = xaRecoveryDs.getConnection();

            String dbProductName = cons[0].getMetaData().getDatabaseProductName().toUpperCase();
            System.out.println("Product Name is " + dbProductName);

            PreparedStatement pstmt;
            pstmt = cons[0].prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Edina");
            pstmt.setInt(2, 47941);
            pstmt.setString(3, "Hennepin");
            pstmt.executeUpdate();
            pstmt.close();

            pstmt = cons[1].prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "St. Louis Park");
            pstmt.setInt(2, 45250);
            pstmt.setString(3, "Hennepin");
            pstmt.executeUpdate();
            pstmt.close();

            pstmt = cons[2].prepareStatement("insert into cities values (?, ?, ?)");
            pstmt.setString(1, "Moorhead");
            pstmt.setInt(2, 38065);
            pstmt.setString(3, "Clay");
            pstmt.executeUpdate();
            pstmt.close();

            System.out.println("Intentionally causing in-doubt transaction");
            TestXAResource.assignSuccessLimit(1, cons);
            try {
                tran.commit();
                throw new Exception("Commit should not have succeeded because the test infrastructure is supposed to cause an in-doubt transaction.");
            } catch (HeuristicMixedException x) {
                TestXAResource.removeSuccessLimit(cons);
                System.out.println("Caught expected HeuristicMixedException: " + x.getMessage());
            }
        } catch (Throwable x) {
            TestXAResource.removeSuccessLimit(cons);
            try {
                tran.rollback();
            } catch (Throwable t) {
            }
            throw x;
        } finally {
            for (Connection con : cons)
                if (con != null)
                    try {
                        con.close();
                    } catch (Throwable x) {
                    }
        }

        // At this point, the transaction is in-doubt.
        // We won't be able to access the data until the transaction manager recovers
        // the transaction and resolves it.
        //
        // A connection configured with TRANSACTION_SERIALIZABLE is necessary in
        // order to allow the recovery to kick in before using the connection.

        System.out.println("attempting to access data (only possible after recovery)");
        try (Connection con = xaRecoveryDs.getConnection()) {
            assertEquals("Isolation level must be 8 (TRANSACTION_SERIALIZABLE) for XA recovery",
                         Connection.TRANSACTION_SERIALIZABLE, con.getTransactionIsolation());

            PreparedStatement pstmt = con.prepareStatement("select name, population, county from cities where name = ?");

            /*
             * Poll for results once a second for 60 seconds.
             * Most databases will have XA recovery done by this point
             *
             */
            Set<String> cities = new HashSet<>();
            for (int count = 0; cities.size() < 3 && count < 60; Thread.sleep(1000)) {
                if (!cities.contains("Edina")) {
                    pstmt.setString(1, "Edina");
                    if (pstmt.executeQuery().next())
                        cities.add("Edina");
                }

                if (!cities.contains("St. Louis Park")) {
                    pstmt.setString(1, "St. Louis Park");
                    if (pstmt.executeQuery().next())
                        cities.add("St. Louis Park");
                }

                if (!cities.contains("Moorhead")) {
                    pstmt.setString(1, "Moorhead");
                    if (pstmt.executeQuery().next())
                        cities.add("Moorhead");
                }
                count++;
                System.out.println("Attempt " + count + " to retrieve recovered XA data. Current status: " + cities);
                if (cities.size() == 3)
                    break; // success
            }

            if (cities.size() < 3)
                throw new Exception("Missing entry in database. Results: " + cities);
            else
                System.out.println("successfully accessed the data");
        }
    }

    /**
     * clears table of all data to ensure fresh start for this test.
     *
     * @param datasource the data source to clear the table for
     */
    private void initTable(DataSource datasource) throws Exception {
        try (Connection con = datasource.getConnection()) {
            Statement st = con.createStatement();
            st.execute("DROP TABLE IF EXISTS cities");
            st.execute("create table cities (name varchar(50) not null primary key, population int, county varchar(30))");
        }

        // End the current LTC and get a new one, so that test methods start from the correct place
        tran.begin();
        tran.commit();
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
