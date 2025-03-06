/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package tests;

import static org.junit.Assert.assertNotNull;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Transaction;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.transaction.fat.util.TxFATServletClient;
import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class ContainerAuthTest extends FATServletClient {
    private static final Class<?> c = ContainerAuthTest.class;

    private static String CONFIGURED_MARKER = "ContainerAuthData is configured";

    // Move to TxFATServletClient
    protected LibertyServer[] serversToCleanup;
    protected String[] toleratedMsgs = new String[] { ".*" };

    // Move to TxFATServletClient
    @After
    public void cleanup() throws Exception {
        try {
            // If any servers have been added to the serversToCleanup array, we'll stop them now
            // test is long gone so we don't care about messages & warnings anymore
            if (serversToCleanup != null && serversToCleanup.length > 0) {
                final String serverNames[] = new String[serversToCleanup.length];
                int i = 0;
                for (LibertyServer s : serversToCleanup) {
                    serverNames[i++] = s.getServerName();
                }
                Log.info(TxFATServletClient.class, "cleanup", "Cleaning " + String.join(", ", serverNames));
                FATUtils.stopServers(toleratedMsgs, serversToCleanup);
            } else {
                Log.info(TxFATServletClient.class, "cleanup", "No servers to stop");
            }

            if (serversToCleanup != null && serversToCleanup.length > 0) {
                // Clean up XA resource files
                serversToCleanup[0].deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);

                // Remove tranlog DB
                serversToCleanup[0].deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
            }
        } finally {
            serversToCleanup = null;
            toleratedMsgs = new String[] { ".*" };
        }
    }

    public static final String APP_NAME = "containerAuth";
    public static final String SERVLET_NAME = APP_NAME + "/AuthServlet";

    @Server("containerAuth")
    public static LibertyServer conAuth;

    @Server("containerAuthBadUser")
    public static LibertyServer conAuthBadUser;

    @Server("containerAuthEmbed")
    public static LibertyServer conAuthEmbed;

    @Server("containerAuthEmbedBadUser")
    public static LibertyServer conAuthEmbedBadUser;

    private static ShrinkHelper.DeployOptions[] NO_DEPLOY_OPTIONS = new ShrinkHelper.DeployOptions[] {};

    public static SetupRunner runner = new SetupRunner() {
        @Override
        public void run(LibertyServer s) throws Exception {
            setUp(s);
        }
    };

    @BeforeClass
    public static void init() throws Exception {
        Log.info(c, "init", "BeforeClass");

        final WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "web.*");
        ShrinkHelper.exportAppToServer(conAuth, app, NO_DEPLOY_OPTIONS);
        ShrinkHelper.exportAppToServer(conAuthBadUser, app, NO_DEPLOY_OPTIONS);
        ShrinkHelper.exportAppToServer(conAuthEmbed, app, NO_DEPLOY_OPTIONS);
        ShrinkHelper.exportAppToServer(conAuthEmbedBadUser, app, NO_DEPLOY_OPTIONS);
    }

    public static void setUp(LibertyServer server) throws Exception {
        JdbcDatabaseContainer<?> testContainer = TxTestContainerSuite.testContainer;
        //Get driver name
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        server.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
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

    @Test
    public void testDynamicContainerAuth() throws Exception {

        serversToCleanup = new LibertyServer[] { conAuth };

        FATUtils.startServers(runner, conAuth);

        // There should be a match so fail if there is not.
        assertNotNull("Container authentication has unexpectedly not been configured", conAuth.waitForStringInTrace(CONFIGURED_MARKER));

        // Do a little tx work
        runTest(conAuth, SERVLET_NAME, "testUserTranLookup");

        // Update server.xml to use a different authData
        conAuth.setMarkToEndOfLog();
        ServerConfiguration serverConfig = conAuth.getServerConfiguration();
        DataSource tranlogDataSource = serverConfig.getDataSources().getById("tranlogDataSource");
        tranlogDataSource.setContainerAuthDataRef("auth3");
        conAuth.updateServerConfiguration(serverConfig);
        conAuth.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        // Do a little more tx work
        runTest(conAuth, SERVLET_NAME, "testUserTranLookup");
    }

    @Test
    public void testDynamicContainerAuthEmbed() throws Exception {

        serversToCleanup = new LibertyServer[] { conAuthEmbed };

        FATUtils.startServers(runner, conAuthEmbed);

        // There should be a match so fail if there is not.
        assertNotNull("Container authentication has unexpectedly not been configured", conAuthEmbed.waitForStringInTrace(CONFIGURED_MARKER));

        // Do a little tx work
        runTest(conAuthEmbed, SERVLET_NAME, "testUserTranLookup");

        // Update server.xml to use a different authData
        conAuthEmbed.setMarkToEndOfLog();
        ServerConfiguration serverConfig = conAuthEmbed.getServerConfiguration();
        Transaction transaction = serverConfig.getTransaction();
        ConfigElementList<DataSource> tranlogDataSources = transaction.getDataSources();
        tranlogDataSources.get(0).setContainerAuthDataRef("auth3");
        conAuthEmbed.updateServerConfiguration(serverConfig);
        conAuthEmbed.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        // Do a little more tx work
        runTest(conAuthEmbed, SERVLET_NAME, "testUserTranLookup");
    }

    @Test
    public void testContainerAuth() throws Exception {

        serversToCleanup = new LibertyServer[] { conAuth };

        FATUtils.startServers(runner, conAuth);

        // There should be a match so fail if there is not.
        assertNotNull("Container authentication has unexpectedly not been configured", conAuth.waitForStringInTrace(CONFIGURED_MARKER));

        // Do a little tx work
        runTest(conAuth, SERVLET_NAME, "testUserTranLookup");
    }

    @Test
    @ExpectedFFDC(value = { "javax.resource.spi.SecurityException", "javax.resource.spi.ResourceAllocationException" })
    public void testContainerAuthBadUser() throws Exception {

        serversToCleanup = new LibertyServer[] { conAuthBadUser };

        FATUtils.startServers(runner, conAuthBadUser);

        // There should be a match so fail if there is not.
        assertNotNull("Container authentication has unexpectedly not been configured", conAuthBadUser.waitForStringInTrace(CONFIGURED_MARKER));

        // Do a little tx work
        runTest(conAuthBadUser, SERVLET_NAME, "testUserTranLookup");

        // Container authentication is configured but to an invalid user name. The recovery log should fail.
        String logFailStr = conAuthBadUser.waitForStringInLog("CWRLS0008_RECOVERY_LOG_FAILED");
        assertNotNull("Recovery log did not fail", logFailStr);
    }

    @Test
    public void testContainerAuthEmbed() throws Exception {

        serversToCleanup = new LibertyServer[] { conAuthEmbed };

        FATUtils.startServers(runner, conAuthEmbed);

        // There should be a match so fail if there is not.
        assertNotNull("Container authentication has unexpectedly not been configured", conAuthEmbed.waitForStringInTrace(CONFIGURED_MARKER));

        // Do a little tx work
        runTest(conAuthEmbed, SERVLET_NAME, "testUserTranLookup");
    }

    @Test
    @ExpectedFFDC(value = { "javax.resource.spi.SecurityException", "javax.resource.spi.ResourceAllocationException" })
    public void testContainerAuthEmbedBadUser() throws Exception {

        serversToCleanup = new LibertyServer[] { conAuthEmbedBadUser };

        FATUtils.startServers(runner, conAuthEmbedBadUser);

        assertNotNull("Container authentication has unexpectedly not been configured", conAuthEmbedBadUser.waitForStringInTrace(CONFIGURED_MARKER));

        // Do a little tx work
        runTest(conAuthEmbedBadUser, SERVLET_NAME, "testUserTranLookup");

        // Container authentication is configured but to an invalid user name. The recovery log should fail.
        String logFailStr = conAuthEmbedBadUser.waitForStringInLog("CWRLS0008_RECOVERY_LOG_FAILED");
        assertNotNull("Recovery log did not fail", logFailStr);
    }
}
