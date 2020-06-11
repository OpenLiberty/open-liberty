/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package db2.web;

import java.sql.Connection;
import java.sql.SQLInvalidAuthorizationSpecException;

import javax.annotation.Resource;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JDBCKerberosTestServlet")
@ServletSecurity(@HttpConstraint(rolesAllowed = "Manager", transportGuarantee = TransportGuarantee.NONE))
public class JDBCKerberosTestServlet extends FATServlet {

    @Resource(lookup = "jdbc/db2ds")
    DataSource ds;

    @Resource(lookup = "jdbc/nokrb5")
    DataSource noKrb5;

    @Test
    public void exampleTest(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        System.out.println("@AGG auth type: " + req.getAuthType());
        System.out.println("@AGG remote usr: " + req.getRemoteUser());
        System.out.println("@AGG principal: " + req.getUserPrincipal());

        System.out.println("Attempting non-kerberos connection...");
        try (Connection con = noKrb5.getConnection()) {
            con.createStatement().execute("SELECT 1 FROM SYSIBM.SYSDUMMY1");
            System.out.println("Used non-kerberos connection OK");
        }

        System.out.println("Attempting kerberos connection...");
        try {
            try (Connection con = ds.getConnection()) {
                con.createStatement().execute("SELECT 1 FROM SYSIBM.SYSDUMMY1");
                System.out.println("Used kerberos connection OK");
            }
        } catch (SQLInvalidAuthorizationSpecException expected) {
            System.out.println("Got expected invalid auth exception");
            expected.printStackTrace();
        }
    }

}
