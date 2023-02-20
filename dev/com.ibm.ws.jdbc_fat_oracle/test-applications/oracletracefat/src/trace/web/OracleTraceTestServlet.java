/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package trace.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@DataSourceDefinition(
                      name = "java:comp/env/jdbc/conn_prop_dsd",
                      className = "oracle.jdbc.pool.OracleDataSource",
                      url = "${env.ORACLE_URL}",
                      user = "${env.ORACLE_USER}",
                      password = "${env.ORACLE_PASSWORD}",
                      properties = {
                                     "connectionProperties=" +
                                     "oracle.net.ssl_version=1.2;" +
                                     "oracle.net.authentication_services=(TCPS);" +
                                     "javax.net.ssl.keyStore=path-to-keystore/keystore.p12;\n" +
                                     "javax.net.ssl.keyStoreType=PKCS12;\n" +
                                     "javax.net.ssl.keyStorePassword=${env.SSL_PASSWORD};\n" +
                                     "javax.net.ssl.trustStore= path-to-keystore/keystore.p12;\n" +
                                     "javax.net.ssl.trustStoreType=PKCS12;\n" +
                                     "javax.net.ssl.trustStorePassword=${env.SSL_PASSWORD}"
                      })

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/OracleTraceTestServlet")
public class OracleTraceTestServlet extends FATServlet {

    @Resource //The default datasource
    private DataSource ds;

    @Resource(lookup = "jdbc/default-dup")
    private DataSource ds_dup;

    @Resource(lookup = "jdbc/conn-prop-ds")
    private DataSource conn_prop_ds;

    @Resource(lookup = "jdbc/conn-prop-ds-generic")
    private DataSource conn_prop_ds_generic;

    @Resource(lookup = "java:comp/env/jdbc/conn_prop_dsd")
    private DataSource conn_prop_dsd;

    //helper method
    public static void insert(DataSource ds, int key, String value) throws SQLException {
        try (Connection conn = ds.getConnection();
                        PreparedStatement ps = conn.prepareStatement("INSERT INTO MYTABLE VALUES(?,?)");) {
            ps.setInt(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    //These tests are not expected to actually create an SSL connection to our Oracle Database
    //The oracle test container does not have SSL connections enabled.  Instead we are just
    //using these connections to make sure the trust/key store passwords are not traced.
    @Test
    public void testDSUsingConnProps() throws Exception {
        insert(conn_prop_ds, 1, "one");
    }

    @Test
    public void testDSUsingConnPropsGeneric() throws Exception {
        insert(conn_prop_ds_generic, 2, "two");
    }

    @Test
    public void testDSDUsingConnProps() throws Exception {
        insert(conn_prop_dsd, 3, "three");
    }

    public void testReadOnlyInfo() throws Exception {
        try (Connection conn = conn_prop_ds.getConnection();
                        PreparedStatement ps = conn.prepareStatement("INSERT INTO MYTABLE VALUES(?,?)");) {
            conn.setReadOnly(false);
            ps.setInt(1, 4);
            ps.setString(2, "four");
            ps.executeUpdate();
        }

        try (Connection conn = conn_prop_ds.getConnection();
                        PreparedStatement ps = conn.prepareStatement("INSERT INTO MYTABLE VALUES(?,?)");) {
            conn.setReadOnly(false);
            ps.setInt(1, 5);
            ps.setString(2, "five");
            ps.executeUpdate();
        }
    }

    public void testOracleLogging(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (request.getParameter("iteration") == null) {
            throw new RuntimeException("iteration null");
        }
        Integer iteration = Integer.parseInt(request.getParameter("iteration"));
        insert(ds, 10 + iteration, "tens");
    }

    public void testOracleLoggingAgain(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (request.getParameter("iteration") == null) {
            throw new RuntimeException("iteration null");
        }
        Integer iteration = Integer.parseInt(request.getParameter("iteration"));
        insert(ds_dup, 10 + iteration, "tens");
    }
}
