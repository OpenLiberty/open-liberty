/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

import static org.junit.Assert.assertNotNull;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;
import com.ibm.ws.transaction.web.AuthServlet;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Example Shrinkwrap FAT project:
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that not all @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@RunWith(FATRunner.class)
public class ContainerAuthDBTranlogTest extends FATServletClient {
    private static final Class<?> c = ContainerAuthDBTranlogTest.class;
    private static final int LOG_SEARCH_TIMEOUT = 300000;
    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/AuthServlet";

    @Server("com.ibm.ws.transaction.dblog")
    @TestServlet(servlet = AuthServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverBase;

    @Server("com.ibm.ws.transaction.dblog_containerAuth")
    @TestServlet(servlet = AuthServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverConAuth;

    @Server("com.ibm.ws.transaction.dblog_containerAuthBadUser")
    @TestServlet(servlet = AuthServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverConAuthBadUser;

    @Server("com.ibm.ws.transaction.dblog_containerAuthEmbed")
    @TestServlet(servlet = AuthServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverEmbed;

    @Server("com.ibm.ws.transaction.dblog_containerAuthEmbedBadUser")
    @TestServlet(servlet = AuthServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverEmbedBadUser;

    public static String[] serverNames = new String[] {
                                                        "com.ibm.ws.transaction.dblog",
                                                        "com.ibm.ws.transaction.dblog_containerAuth",
                                                        "com.ibm.ws.transaction.dblog_containerAuthBadUser",
                                                        "com.ibm.ws.transaction.dblog_containerAuthEmbed",
                                                        "com.ibm.ws.transaction.dblog_containerAuthEmbedBadUser",
    };

    public static SetupRunner runner = new SetupRunner() {
        @Override
        public void run(LibertyServer s) throws Exception {
            setUp(s);
        }
    };

    @BeforeClass
    public static void init() throws Exception {
        Log.info(c, "init", "BeforeClass");

        ShrinkHelper.defaultApp(serverBase, APP_NAME, "com.ibm.ws.transaction.web.*");
        ShrinkHelper.defaultApp(serverConAuth, APP_NAME, "com.ibm.ws.transaction.web.*");
        ShrinkHelper.defaultApp(serverConAuthBadUser, APP_NAME, "com.ibm.ws.transaction.web.*");
        ShrinkHelper.defaultApp(serverEmbed, APP_NAME, "com.ibm.ws.transaction.web.*");
        ShrinkHelper.defaultApp(serverEmbedBadUser, APP_NAME, "com.ibm.ws.transaction.web.*");
    }

    public static void setUp(LibertyServer server) throws Exception {
        JdbcDatabaseContainer<?> testContainer = TxTestContainerSuite.testContainer;
        //Get driver name
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        server.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

            @Override
            public Void run() throws Exception {
                ShrinkHelper.cleanAllExportedArchives();
                return null;
            }
        });
    }

    /**
     * Test a standard config
     */
    @Test
    public void testNoContainerAuth() throws Exception {
        final String method = "testNoContainerAuth";

        // Start serverBase
        FATUtils.startServers(runner, serverBase);

        // Server appears to have started ok. Check for key string to see whether recovery has succeeded
        String notConfigStr = serverBase.waitForStringInTrace("ContainerAuthData NOT configured");
        // There should be a match so fail if there is not.
        assertNotNull("Container authentication has been unexpectedly configured", notConfigStr);
        // Lastly stop serverBase
        // "WTRN0075W", "WTRN0076W", "CWWKE0701E" error messages are expected/allowed
        FATUtils.stopServers(serverBase);
    }

    @Test
    @AllowedFFDC(value = { "javax.resource.spi.SecurityException", "com.ibm.ws.recoverylog.spi.InternalLogException",
                           "javax.resource.spi.ResourceAllocationException" })
    public void testContainerAuth() throws Exception {
        final String method = "testContainerAuth";

        // Start serverConAuth
        FATUtils.startServers(runner, serverConAuth);

        // Check for key string to see whether it succeeded
        String configStrRestart = serverConAuth.waitForStringInTrace("ContainerAuthData IS configured");
        // There should be a match so fail if there is not.
        assertNotNull("Container authentication has unexpectedly not been configured", configStrRestart);
        // Do a little tx work
        runTest(serverConAuth, "testUserTranLookup");

        FATUtils.stopServers(serverConAuth);
    }

    @Test
    @AllowedFFDC(value = { "javax.resource.spi.SecurityException", "com.ibm.ws.recoverylog.spi.InternalLogException",
                           "javax.resource.spi.ResourceAllocationException" })
    public void testContainerAuthBadUser() throws Exception {
        final String method = "testContainerAuthBadUser";

        // Start serverConAuthBadUser
        FATUtils.startServers(runner, serverConAuthBadUser);

        // Server appears to have started ok. Check for key string to see whether recovery has succeeded
        String configStrRestart = serverConAuthBadUser.waitForStringInTrace("ContainerAuthData IS configured");
        // There should be a match so fail if there is not.
        assertNotNull("Container authentication has unexpectedly not been configured", configStrRestart);
        // Do a little tx work
        runTest(serverConAuthBadUser, "testUserTranLookup");

        // Container authentication is configured but to an invalid user name. The recovery log should fail.
        String logFailStr = serverConAuthBadUser.waitForStringInLog("CWRLS0008_RECOVERY_LOG_FAILED");
        assertNotNull("Recovery log did not fail", logFailStr);

        FATUtils.stopServers(serverConAuthBadUser);
    }

    @Test
    @AllowedFFDC(value = { "javax.resource.spi.SecurityException", "com.ibm.ws.recoverylog.spi.InternalLogException",
                           "javax.resource.spi.ResourceAllocationException" })
    public void testContainerAuthEmbed() throws Exception {
        final String method = "testContainerAuthEmbed";

        // Start serverEmbed
        FATUtils.startServers(runner, serverEmbed);
        // Check for key string to see whether it succeeded
        String configStrRestart = serverEmbed.waitForStringInTrace("ContainerAuthData IS configured");
        // There should be a match so fail if there is not.
        assertNotNull("Container authentication has unexpectedly not been configured", configStrRestart);

        // Do a little tx work
        runTest(serverEmbed, "testUserTranLookup");

        FATUtils.stopServers(serverEmbed);
    }

    @Test
    @AllowedFFDC(value = { "javax.resource.spi.SecurityException", "com.ibm.ws.recoverylog.spi.InternalLogException",
                           "javax.resource.spi.ResourceAllocationException" })
    public void testContainerAuthEmbedBadUser() throws Exception {
        final String method = "testContainerAuthEmbedBadUser";

        // Start serverEmbedBadUser
        FATUtils.startServers(runner, serverEmbedBadUser);
        // Check for key string to see whether it succeeded
        String configStrRestart = serverEmbedBadUser.waitForStringInTrace("ContainerAuthData IS configured");
        // There should be a match so fail if there is not.
        assertNotNull("Container authentication has unexpectedly not been configured", configStrRestart);

        // Do a little tx work
        runTest(serverEmbedBadUser, "testUserTranLookup");

        // Container authentication is configured but to an invalid user name. The recovery log should fail.
        String logFailStr = serverEmbedBadUser.waitForStringInLog("CWRLS0008_RECOVERY_LOG_FAILED");
        assertNotNull("Recovery log did not fail", logFailStr);

        FATUtils.stopServers(serverEmbedBadUser);
    }

    /**
     * Runs the test
     */
    private void runTest(LibertyServer server, String testName) throws Exception {
        StringBuilder sb = null;
        try {
            sb = runTestWithResponse(server, SERVLET_NAME, testName);

        } catch (Throwable e) {
        }
        Log.info(this.getClass(), testName, testName + " returned: " + sb);

    }
}
