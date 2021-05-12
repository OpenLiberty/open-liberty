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
package jdbc.krb5.pg.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.security.auth.login.LoginException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PgKerberosTestServlet")
public class PgKerberosTestServlet extends FATServlet {

    @Resource(lookup = "jdbc/nokrb5")
    DataSource noKrb5;

    @Resource(lookup = "jdbc/krb/basic")
    DataSource krb5DataSource;

    @Resource(lookup = "jdbc/krb/xa")
    DataSource krb5XADataSource;

    @Resource(lookup = "jdbc/krb/DataSource")
    DataSource krb5RegularDs;

    @Resource(lookup = "jdbc/krb/invalidPrincipal")
    DataSource invalidPrincipalDs;

    /**
     * PostgreSQL is configured to require kerberos connections.
     * Expect a basic user/password connection to fail
     */
    @Test
    @AllowedFFDC
    public void testNonKerberosConnectionFails() throws Exception {
        try (Connection con = noKrb5.getConnection()) {
            fail("Should not be able to obtain a DB connection using basic user/pass when kerberos is configured on the DB");
        } catch (SQLException expected) {
            System.out.println("Caught expected SQLException");
        }
    }

    /**
     * Get a connection from a javax.sql.ConnectionPoolDataSource
     */
    @Test
    public void testKerberosBasicConnection() throws Exception {
        try (Connection con = krb5DataSource.getConnection()) {
            con.createStatement().execute("SELECT 1");
        }
    }

    /**
     * Get a connection from a javax.sql.XADataSource
     */
    @Test
    public void testKerberosXAConnection() throws Exception {
        try (Connection con = krb5XADataSource.getConnection()) {
            con.createStatement().execute("SELECT 1");
        }
    }

    /**
     * Get a connection from a javax.sql.DataSource
     */
    @Test
    public void testKerberosRegularConnection() throws Exception {
        try (Connection con = krb5RegularDs.getConnection()) {
            con.createStatement().execute("SELECT 1");
        }
    }

    @Test
    @AllowedFFDC
    public void testInvalidPrincipal() throws Exception {
        try (Connection con = invalidPrincipalDs.getConnection()) {
            con.createStatement().execute("SELECT 1");
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
}
