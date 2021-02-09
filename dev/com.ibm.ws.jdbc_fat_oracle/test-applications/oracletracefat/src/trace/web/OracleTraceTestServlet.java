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
package trace.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@DataSourceDefinition(
                      name = "java:comp/env/jdbc/conn_prop_dsd",
                      className = "oracle.jdbc.pool.OracleDataSource",
                      url = "${env.URL}",
                      user = "${env.USER}",
                      password = "${env.PASSWORD}",
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

    @Resource(lookup = "jdbc/conn-prop-ds")
    private DataSource conn_prop_ds;

    @Resource(lookup = "jdbc/conn-prop-ds-generic")
    private DataSource conn_prop_ds_generic;

    @Resource(lookup = "java:comp/env/jdbc/conn_prop_dsd")
    private DataSource conn_prop_dsd;

    //helper method
    public static void insert(DataSource ds, int key, String value) throws SQLException {
        Connection conn = ds.getConnection();

        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO MYTABLE VALUES(?,?)");
            ps.setInt(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
            ps.close();
        } finally {
            conn.close();
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
}
