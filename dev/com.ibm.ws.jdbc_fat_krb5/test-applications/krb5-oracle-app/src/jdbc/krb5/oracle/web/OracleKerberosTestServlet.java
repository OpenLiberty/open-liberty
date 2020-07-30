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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            // Getting a connection too soon after the initial ticket is obtained can cause intermittent
            // issues where we getConnection() fails with: Oracle Error ORA-12631
            // These timing issues can be reproduced in a standalone JDBC program, which indicates that
            // we aren't doing anything wrong in the Liberty code, and instead this is due a Oracle driver
            // or DB issue which would require an Oracle support contract to investigate further
            Thread.sleep(750);
        } catch (InterruptedException e) {
        }
        super.doGet(request, response);
    }

    @Test
    public void testNonKerberosConnection() throws Exception {
        try (Connection con = noKrb5.getConnection()) {
            con.createStatement().execute("SELECT 1 FROM DUAL");
        }
    }

    @Test
    public void testKerberosBasicConnection() throws Exception {
        try (Connection con = krb5DataSource.getConnection()) {
            con.createStatement().execute("SELECT 1 FROM DUAL");
        }
    }

    @Test
    public void testKerberosXAConnection() throws Exception {
        try (Connection con = krb5XADataSource.getConnection()) {
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

}
