/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.derby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATDatabaseServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/DerbyLoadFromAppServlet")
public class DerbyLoadFromAppServlet extends FATDatabaseServlet {
    @Resource
    private DataSource defaultDataSource;

    // Use a data source with generic properties element where the dataSource's jdbcDriver
    // specifies a data source class name, but is configured without any library,
    // in which case JDBC driver classes are loaded from the application's thread context class loader.
    @Test
    public void testDefaultDataSource() throws Exception {
        Connection con = defaultDataSource.getConnection("DerbyLoadFromAppServlet", "pwd1");
        try {
            assertEquals("Apache Derby", con.getMetaData().getDatabaseProductName());
        } finally {
            con.close();
        }
    }

    // Use a data source with derby properties element where the dataSource is configured
    // without any library, in which case the JDBC driver class name is inferred from the fact
    // that the derby properties element is used, and JDBC driver classes are loaded from the
    // application's thread context class loader.
    @Test
    public void testDerbyDataSource() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/derby");
        Connection con = ds.getConnection();
        try {
            assertEquals("Apache Derby", con.getMetaData().getDatabaseProductName());
        } finally {
            con.close();
        }
    }

    // This basic test verifies that the application can at least load classes from the Derby library that it includes
    @Test
    public void testLoadDerbyClass() throws Exception {
        Class.forName("org.apache.derby.jdbc.EmbeddedDataSource");
    }

    // Obtain a connection with a user/password that is unique to this method and a corresponding method
    // in LoadFromAppServlet, and verify that the underlying JDBC driver loaded is the Derby Embedded JDBC driver
    // which is found in this application and not the fake "Mini" JDBC driver from the other application.
    @Test
    public void testMatchingByAppLoader() throws Exception {
        Connection con = defaultDataSource.getConnection("testMatchingByAppLoader", "pwd1");
        try {
            DatabaseMetaData metadata = con.getMetaData();
            assertEquals("Apache Derby Embedded JDBC Driver", metadata.getDriverName());
            assertEquals("testMatchingByAppLoader", metadata.getUserName());
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation()); // isn't REPEATABLE_READ because ProxyDataSource with generic properties is used
            assertEquals(null, con.getCatalog());
            assertFalse(con.isReadOnly());
        } finally {
            con.close();
        }
    }
}
