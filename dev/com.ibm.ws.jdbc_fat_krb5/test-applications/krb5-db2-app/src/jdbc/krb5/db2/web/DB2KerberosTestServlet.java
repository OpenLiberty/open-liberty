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
import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException;

import javax.annotation.Resource;
import javax.security.auth.login.LoginException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/DB2KerberosTestServlet")
public class DB2KerberosTestServlet extends FATServlet {

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

    // The jdbc/noAuth datasource would normally fail due to no credentials,
    // but since we have bound the 'krb5Auth' auth alias to 'java:app/env/jdbc/reboundAuth'
    // in the ibm-web-bnd.xml it will work
    @Resource(lookup = "jdbc/noAuth", name = "java:app/env/jdbc/reboundAuth")
    DataSource reboundAuth;

    @Test
    @AllowedFFDC
    public void testNonKerberosConnectionRejected() throws Exception {
        try (Connection con = noKrb5.getConnection()) {
            throw new Exception("Should not be able to obtain a non-kerberos connection from a kerberos-only database");
        } catch (SQLInvalidAuthorizationSpecException e) {
            System.out.println("Got expected error getting non-kerberos connection");
        }
    }

    @Test
    public void testKerberosBasicConnection() throws Exception {
        try (Connection con = krb5DataSource.getConnection()) {
            con.createStatement().execute("SELECT 1 FROM SYSIBM.SYSDUMMY1");
        }
    }

    @Test
    public void testKerberosXAConnection() throws Exception {
        try (Connection con = krb5XADataSource.getConnection()) {
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
