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
@WebServlet(urlPatterns = "/JDBCKerberosTestServlet")
public class DB2KerberosTestServlet extends FATServlet {

    @Resource(lookup = "jdbc/nokrb5")
    DataSource noKrb5;

    @Resource(lookup = "jdbc/krb/basic")
    DataSource krb5DataSource;

    @Resource(lookup = "jdbc/krb/xa")
    DataSource krb5XADataSource;

    @Resource(lookup = "jdbc/krb/invalidPrincipal")
    DataSource invalidPrincipalDs;

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

}
