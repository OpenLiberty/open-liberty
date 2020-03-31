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
package web.other;

import static org.junit.Assert.assertEquals;
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
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATDatabaseServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/LoadFromAppServlet")
public class LoadFromAppServlet extends FATDatabaseServlet {
    // TODO this turned out to be a good test of loading a JDBC driver from the app in combination with a login module. Cannot enable it yet because that function isn't complete.
    //@Resource(name = "java:app/env/jdbc/ddsLoadFromAppWithWebModLoginModule")
    //private DataSource loadedFromAppDataSourceWithWebModLoginModule;

    @Resource(name = "java:app/env/jdbc/sldsLoginModuleFromTopLevelJarInApp", lookup = "jdbc/sharedLibDataSource")
    private DataSource sldsLoginModuleFromTopLevelJarInApp;

    @Resource(name = "java:app/env/jdbc/sldsLoginModuleFromWebAppNotFoundInEAR", lookup = "jdbc/sharedLibDataSource")
    private DataSource sldsLoginModuleFromWebAppNotFound;

    @Resource(name = "java:app/env/jdbc/sldsLoginModuleFromWebModuleInApp", lookup = "jdbc/sharedLibDataSource")
    private DataSource sldsLoginModuleFromWebModuleInApp;

    @Override
    public void init(ServletConfig config) throws ServletException {
    }

    // This basic test verifies that the application cannot load Derby classes because it does not package a Derby library.
    @Test
    public void testCannotLoadDerbyClass() throws Exception {
        try {
            Class<?> loaded = Class.forName("org.apache.derby.jdbc.EmbeddedDataSource");
            fail("Should not be able to load Derby class " + loaded);
        } catch (ClassNotFoundException x) { // pass
        }
    }

    // Use a data source with generic properties element where the dataSource's jdbcDriver
    // specifies a data source class name, but is configured without any library,
    // in which case JDBC driver classes are loaded from the application's thread context class loader.
    // @Test // Enable once different datasource impl is used per app class loader
    public void testDefaultDataSource() throws Exception {
        DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");
        Connection con = ds.getConnection("LoadFromAppServlet", "pwd1");
        try {
            assertEquals("MiniDatabase", con.getMetaData().getDatabaseProductName());
        } finally {
            con.close();
        }
    }

    // Use a data source with derby properties element where the dataSource is configured
    // without any library, in which case the JDBC driver class name is inferred from the fact
    // that the derby properties element is used, and JDBC driver classes attempt to load from the
    // application's thread context class loader, but in this case, the application doesn't
    // include a Derby driver. Expect a failure.
    // @Test // TODO write this test once different data source impl is used per app class loader
    public void testDerbyDataSourceUnavailable() throws Exception {
        // TODO catch expected failure for: DataSource ds = InitialContext.doLookup("jdbc/derby");
    }

    // Use a data source that is backed by a java.sql.Driver which is packaged with the application.
    @Test
    public void testDriverLoadedFromApp() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/miniDriver");
        Connection con = ds.getConnection("driveruser1", "driverpwd1");
        try {
            assertEquals("driverdb", con.getCatalog());

            DatabaseMetaData mdata = con.getMetaData();
            assertEquals("MiniJDBC", mdata.getDriverName());
            assertEquals("driveruser1", mdata.getUserName());
        } finally {
            con.close();
        }
    }

    // Use a data source that is backed by a data source that is packaged with the application,
    // where no information is provided about the vendor data source class name such that it must
    // be inferred from the detected java.sql.Driver impl class.
    @Test
    public void testInferDataSourceFromDriverPackage() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/miniDataSource");
        assertEquals(330, ds.getLoginTimeout());

        Connection con = ds.getConnection("dsuser1", "dspwd1");
        try {
            assertEquals("minidb", con.getCatalog());

            DatabaseMetaData mdata = con.getMetaData();
            assertEquals("MiniJDBC", mdata.getDriverName());
            assertEquals("dsuser1", mdata.getUserName());
        } finally {
            con.close();
        }
    }

    /**
     * testLoginModuleFromEJBModule1 - verify that a login module that is packaged within an EJB module
     * is used to authenticate to a data source when its resource reference specifies to use that login module.
     */
    @Test
    public void testLoginModuleFromEJBModule1() throws Exception {
        Executor bean = InitialContext.doLookup("java:global/otherApp/ejb1/FirstBean!java.util.concurrent.Executor");
        bean.execute(() -> {
            try {
                DataSource ds = (DataSource) InitialContext.doLookup("java:comp/env/jdbc/dsref");
                try (Connection con = ds.getConnection()) {
                    DatabaseMetaData mdata = con.getMetaData();
                    String userName = mdata.getUserName();
                    assertEquals("ejb1user", userName);
                }
            } catch (RuntimeException x) {
                throw x;
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
        });
    }

    /**
     * testLoginModuleFromEJBModule2 - verify that a login module that is packaged within an EJB module
     * is used to authenticate to a data source when its resource reference specifies to use that login module.
     */
    @Test
    public void testLoginModuleFromEJBModule2() throws Exception {
        Executor bean = InitialContext.doLookup("java:global/otherApp/ejb2/SecondBean!java.util.concurrent.Executor");
        bean.execute(() -> {
            try {
                DataSource ds = (DataSource) InitialContext.doLookup("java:comp/env/jdbc/dsref");
                try (Connection con = ds.getConnection()) {
                    DatabaseMetaData mdata = con.getMetaData();
                    String userName = mdata.getUserName();
                    assertEquals("ejb2user", userName);
                }
            } catch (RuntimeException x) {
                throw x;
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
        });
    }

    /**
     * testLoginModuleFromTopLevelJarInAppLoaded - verify that a login module that is packaged within a JAR at the top level
     * is used to authenticate to a data source when its resource reference specifies to use that login module.
     * Attempt this from the web module and both EJB modules.
     */
    @Test
    public void testLoginModuleFromTopLevelJarInApp() throws Exception {
        // from web module
        try (Connection con = sldsLoginModuleFromTopLevelJarInApp.getConnection()) {
            DatabaseMetaData metadata = con.getMetaData();
            assertEquals("appuser", metadata.getUserName());
        }

        // from the first EJB module
        Executor bean = InitialContext.doLookup("java:global/otherApp/ejb1/FirstBean!java.util.concurrent.Executor");
        bean.execute(() -> {
            // the following runs in the EJB module
            try {
                DataSource ds = (DataSource) InitialContext.doLookup("java:app/env/jdbc/sldsLoginModuleFromTopLevelJarInApp");
                try (Connection con = ds.getConnection()) {
                    DatabaseMetaData mdata = con.getMetaData();
                    String userName = mdata.getUserName();
                    assertEquals("appuser", userName);
                }
            } catch (RuntimeException x) {
                throw x;
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
        });

        // from the second EJB module
        bean = InitialContext.doLookup("java:global/otherApp/ejb2/SecondBean!java.util.concurrent.Executor");
        bean.execute(() -> {
            try {
                // the following runs in the EJB module
                DataSource ds = (DataSource) InitialContext.doLookup("java:app/env/jdbc/sldsLoginModuleFromTopLevelJarInApp");
                try (Connection con = ds.getConnection()) {
                    DatabaseMetaData mdata = con.getMetaData();
                    String userName = mdata.getUserName();
                    assertEquals("appuser", userName);
                }
            } catch (RuntimeException x) {
                throw x;
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
        });
    }

    /**
     * testLoginModuleFromWebAppNotFoundInOtherApp - verify that a login module that is packaged within a separate web application
     * that does not match the classProviderRef CANNOT be used to authenticate to a data source when its resource reference
     * specifies to use that login module.
     */
    @AllowedFFDC({
                   "javax.security.auth.login.LoginException", // no login modules for notFoundInEarLogin
                   "javax.resource.ResourceException" // chains the LoginException
    })
    @Test
    public void testLoginModuleFromWebAppNotFoundInOtherApp() throws Exception {
        try (Connection con = sldsLoginModuleFromWebAppNotFound.getConnection()) {
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
     * testLoginModuleFromWebModuleInEAR - verify that a login module that is packaged within a web module within an enterprise application
     * can be used to authenticate to a data source when its resource reference specifies to use that login module.
     */
    @Test
    public void testLoginModuleFromWebModuleInEAR() throws Exception {
        try (Connection con = sldsLoginModuleFromWebModuleInApp.getConnection()) {
            DatabaseMetaData metadata = con.getMetaData();
            String userName = metadata.getUserName();
            assertEquals("webuser", userName);
        }
    }

    // Obtain a connection with a user/password that is unique to this method and a corresponding method
    // in DerbyLoadFromAppServlet, and verify that the underlying JDBC driver loaded is the fake "Mini" JDBC driver
    // which is found in this application and not Derby from the other application.
    // @Test // TODO enable once we can match connections based on application class loader
    public void testMatchingByAppLoader() throws Exception {
        DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");
        Connection con = ds.getConnection("testMatchingByAppLoader", "pwd1");
        try {
            DatabaseMetaData metadata = con.getMetaData();
            assertEquals("MiniJDBC", metadata.getDriverName());
            assertEquals("testMatchingByAppLoader", metadata.getUserName());
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
            assertEquals("memory:ds1", con.getCatalog());
            assertTrue(con.isReadOnly());
        } finally {
            con.close();
        }
    }

    /**
     * testWebInboundLoginModule - log in with the default login module for web inbound traffic,
     * for which the location of the login module class is configured via classProviderRef.
     */
    @Test
    public void testWebInboundLoginModule() throws Exception {
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
