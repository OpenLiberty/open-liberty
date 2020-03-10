/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATDatabaseServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/DerbyLoadFromAppServlet")
public class DerbyLoadFromAppServlet extends FATDatabaseServlet {
    @Resource
    private DataSource defaultDataSource;

    @Resource(name = "java:module/env/jdbc/sldsLoginModuleFromOtherApp", lookup = "jdbc/sharedLibDataSource")
    private DataSource sldsLoginModuleFromOtherApp;

    @Resource(name = "java:module/env/jdbc/sldsLoginModuleFromEnterpriseAppNotFoundInWebApp", lookup = "jdbc/sharedLibDataSource")
    private DataSource sldsLoginModuleFromOtherAppNotFound;

    @Resource(name = "java:module/env/jdbc/sldsLoginModuleFromWebApp", lookup = "jdbc/sharedLibDataSource")
    private DataSource sldsLoginModuleFromWebApp;

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

    /**
     * testLoginModuleFromWebApp - verify that a login module that is packaged within the web application can be used
     * by an EJB within the web application to authenticate to a data source when its resource reference specifies to use that login module.
     */
    @Test
    public void testLoginModuleFromEJBInWebApp() throws Exception {
        Executor bean = InitialContext.doLookup("java:global/derbyApp/BeanInWebApp!java.util.concurrent.Executor");
        bean.execute(() -> {
            try {
                DataSource ds = (DataSource) InitialContext.doLookup("java:comp/env/jdbc/dsref");
                try (Connection con = ds.getConnection()) {
                    DatabaseMetaData mdata = con.getMetaData();
                    String userName = mdata.getUserName();
                    assertEquals("webappuser", userName);
                }
            } catch (RuntimeException x) {
                throw x;
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
        });
    }

    /**
     * testLoginModuleFromOtherApp - verify that a login module that is packaged within another application can be used to authenticate
     * a data source that is defined in the current web application, where its resource reference specifies to use that login module.
     */
    @Test
    public void testLoginModuleFromOtherApp() throws Exception {
        try (Connection con = sldsLoginModuleFromOtherApp.getConnection()) {
            DatabaseMetaData metadata = con.getMetaData();
            assertEquals("appuser", metadata.getUserName());
        }
    }

    /**
     * testLoginModuleFromOtherAppNotFoundInWebApp - verify that a login module that is packaged within a separate enterprise application
     * that does not match the classProviderRef CANNOT be used to authenticate to a data source when its resource reference
     * specifies to use that login module.
     */
    @AllowedFFDC({
                   "javax.security.auth.login.LoginException", // no login modules for notFoundInWebAppLogin
                   "javax.resource.ResourceException" // chains the LoginException
    })
    @Test
    public void testLoginModuleFromOtherAppNotFoundInWebApp() throws Exception {
        try (Connection con = sldsLoginModuleFromOtherAppNotFound.getConnection()) {
            DatabaseMetaData metadata = con.getMetaData();
            fail("authenticated as user " + metadata.getUserName());
        } catch (SQLException x) {
            Throwable cause = x;
            while (cause != null && !(cause instanceof LoginException))
                cause = cause.getCause();
            if (!(cause instanceof LoginException))
                throw x;
        }
    }

    /**
     * testLoginModuleFromWebApp - verify that a login module that is packaged within the web application can be used to authenticate
     * to a data source when its resource reference specifies to use that login module.
     */
    @Test
    public void testLoginModuleFromWebApp() throws Exception {
        try (Connection con = sldsLoginModuleFromWebApp.getConnection()) {
            DatabaseMetaData metadata = con.getMetaData();
            assertEquals("webappuser", metadata.getUserName());
        }
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

    /**
     * testWebInboundLoginModuleFromWebApp - log in with the default login module for web inbound traffic,
     * for which the location of the login module class is configured via classProviderRef.
     */
    @Test
    public void testWebInboundLoginModuleFromWebApp() throws Exception {
        final List<String> users = new ArrayList<String>();
        final LoginContext loginContext = new LoginContext("system.WEB_INBOUND");

        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
            @Override
            public Void run() throws Exception {
                loginContext.login();
                try {
                    Subject subject = loginContext.getSubject();
                    for (PasswordCredential cred : subject.getPrivateCredentials(PasswordCredential.class)) {
                        String user = cred.getUserName();
                        users.add(user);
                        if ("webInboundUser".equals(user))
                            Assert.assertArrayEquals("webInboundPwd".toCharArray(), cred.getPassword());
                    }
                } finally {
                    loginContext.logout();
                }
                return null;
            }
        });

        assertTrue("Found in private credentials: " + users.toString(), users.contains("webInboundUser"));
    }
}
