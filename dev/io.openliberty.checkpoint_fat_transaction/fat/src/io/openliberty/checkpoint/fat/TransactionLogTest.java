/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.deleteTranlogDir;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.FATSuite.stopServer;
import static io.openliberty.checkpoint.fat.util.FATUtils.LOG_SEARCH_TIMEOUT;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.DerbyNetworkUtilities;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Verify checkpoint behaviors over transaction logging configurations.
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class TransactionLogTest extends FATServletClient {

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new EE8FeatureReplacementAction().forServers("checkpointTransactionServletStartupDbLog", "checkpointTransactionServlet", "checkpointTransactionDbLog"))
                    .andWith(new JakartaEE9Action().forServers("checkpointTransactionServletStartupDbLog", "checkpointTransactionServlet", "checkpointTransactionDbLog")
                                    .fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers("checkpointTransactionServletStartupDbLog", "checkpointTransactionServlet", "checkpointTransactionDbLog")
                                    .fullFATOnly());

    static final String APP_NAME = "transactionservlet";
    static final String SERVLET_NAME = APP_NAME + "/SimpleServlet";

    static final String APP2_NAME = "transactionservletstartup";
    static final String SERVLET2_NAME = APP_NAME + "/StartupServlet";

    // Use sever.env vars to override these config vars in server.xml
    static final int DERBY_TXLOG_PORT = 1619;
    static final String DERBY_DS_JNDINAME = "jdbc/derby";
    static final String TX_LOG_DIR = "${server.config.dir}NEW_TRANLOG_DIR";
    static final String TX_RETRY_INT = "11";

    @Server("checkpointTransactionServlet")
    public static LibertyServer serverTranLog;

    @Server("checkpointTransactionDbLog")
    public static LibertyServer serverDbTranLog;

    @Server("checkpointTransactionServletStartupDbLog")
    public static LibertyServer serverServletStartupDbTranLog;

    TestMethod testMethod;

    @BeforeClass
    public static void setupClass() throws Exception {
        serverTranLog.saveServerConfiguration();
        serverDbTranLog.saveServerConfiguration();
        serverServletStartupDbTranLog.saveServerConfiguration();
    }

    @Before
    public void setUp() throws Exception {
        ShrinkHelper.cleanAllExportedArchives();

        testMethod = getTestMethod(TestMethod.class, testName);
        switch (testMethod) {
            case testCheckpointBasIgnoresRosWhenSqlRecoveryLog:
                DerbyNetworkUtilities.startDerbyNetwork(DERBY_TXLOG_PORT);

                serverServletStartupDbTranLog.restoreServerConfiguration();

                ShrinkHelper.defaultApp(serverServletStartupDbTranLog, APP2_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "servlets.startup.*");

                Consumer<LibertyServer> preRestoreLogic = checkpointServer -> {
                    // Override the app datasource, tranlog datasource, and transactions
                    // configurations for restore.
                    File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
                    try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                        serverEnvWriter.println("DERBY_TXLOG_PORT=" + DERBY_TXLOG_PORT);
                        serverEnvWriter.println("DERBY_DS_JNDINAME=" + DERBY_DS_JNDINAME);
                        serverEnvWriter.println("TX_RETRY_INT=" + TX_RETRY_INT);
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                    // Verify the application started during checkpoint
                    assertNull("'CWWKZ0001I: Application " + APP2_NAME + " started' message not found in log.",
                               serverServletStartupDbTranLog.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP2_NAME, 1000));
                };
                serverServletStartupDbTranLog.setCheckpoint(new CheckpointInfo(CheckpointPhase.BEFORE_APP_START, false, preRestoreLogic));
                serverServletStartupDbTranLog.startServer();
                break;
            case testCheckpointNoRosWithDefaultRecoveryLog:
                serverTranLog.restoreServerConfiguration();

                ShrinkHelper.defaultApp(serverTranLog, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "servlets.simple.*");

                // Set recoverOnStartup to false (default value)
                ServerConfiguration config = serverTranLog.getServerConfiguration();
                config.getTransaction().setRecoverOnStartup(false);
                serverTranLog.updateServerConfiguration(config);

                deleteTranlogDir(serverTranLog);

                // Simulate configure.sh: prime the server caches
                serverTranLog.startServer();
                stopServer(serverTranLog);

                Consumer<LibertyServer> preRestoreLogic1 = checkpointServer -> {
                    // Override the app datasource, but not the default transactionLogDirectory
                    File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
                    try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                        serverEnvWriter.println("DERBY_DS_JNDINAME=" + DERBY_DS_JNDINAME);
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                };
                serverTranLog.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, preRestoreLogic1));
                serverTranLog.startServer();
                break;
            case testCheckpointRosWithDefaultRecoveryLog:
                serverTranLog.restoreServerConfiguration();

                ShrinkHelper.defaultApp(serverTranLog, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "servlets.simple.*");

                deleteTranlogDir(serverTranLog); // clean up

                // Cycling the server with ROS=true will initialize the recovery logs in ${server.output.dir}/tranlog/,
                // the default transactionLogDirectory. This step simulates a container build optimization
                // to populate the java SCC and server caches (container layer) before server checkpoint.
                serverTranLog.startServer();
                stopServer(serverTranLog);

                Consumer<LibertyServer> preRestoreLogic4 = checkpointServer -> {
                    // Override the app datasource, but not the default transactionLogDirectory
                    File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
                    try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                        serverEnvWriter.println("DERBY_DS_JNDINAME=" + DERBY_DS_JNDINAME);
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                };
                serverTranLog.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, preRestoreLogic4));
                serverTranLog.startServer();
                break;
            case testTranactionLogDirectoryUpdatesAtRestore:
                serverTranLog.restoreServerConfiguration();

                ShrinkHelper.defaultApp(serverTranLog, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "servlets.simple.*");

                deleteTranlogDir(serverTranLog); // Clean up

                Consumer<LibertyServer> preRestoreLogic2 = checkpointServer -> {
                    // Override the app datasource and transaction configurations for restore.
                    File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
                    try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                        serverEnvWriter.println("DERBY_DS_JNDINAME=" + DERBY_DS_JNDINAME);
                        serverEnvWriter.println("TX_LOG_DIR=" + TX_LOG_DIR);
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                };
                serverTranLog.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, preRestoreLogic2));
                serverTranLog.startServer();
                break;
            case testSqlRecoveryLogUpdatesUpdatesAtRestore:
                DerbyNetworkUtilities.startDerbyNetwork(DERBY_TXLOG_PORT);

                serverDbTranLog.restoreServerConfiguration();

                ShrinkHelper.defaultApp(serverDbTranLog, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "servlets.simple.*");

                Consumer<LibertyServer> preRestoreLogic3 = checkpointServer -> {
                    // Override the app datasource, tranlog datasource, and transactions
                    // configurations for restore.
                    File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
                    try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                        serverEnvWriter.println("DERBY_TXLOG_PORT=" + DERBY_TXLOG_PORT);
                        serverEnvWriter.println("DERBY_DS_JNDINAME=" + DERBY_DS_JNDINAME);
                        serverEnvWriter.println("TX_RETRY_INT=" + TX_RETRY_INT);
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                    // Verify the application starts during checkpoint
                    assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                                  serverDbTranLog.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                    assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                  serverDbTranLog.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
                };
                serverDbTranLog.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, preRestoreLogic3);
                serverDbTranLog.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
                serverDbTranLog.startServer();
                break;
            default:
                break;
        }
    }

    @After
    public void tearDown() throws Exception {
        switch (testMethod) {
            case testCheckpointBasIgnoresRosWhenSqlRecoveryLog:
                try {
                    stopServer(serverServletStartupDbTranLog, "WTRN0017W");
                } finally {
                    DerbyNetworkUtilities.stopDerbyNetwork(DERBY_TXLOG_PORT);
                }
                break;
            case testCheckpointNoRosWithDefaultRecoveryLog:
                stopServer(serverTranLog, "WTRN0017W");
                break;
            case testCheckpointRosWithDefaultRecoveryLog:
                stopServer(serverTranLog, "WTRN0017W");
                break;
            case testTranactionLogDirectoryUpdatesAtRestore:
                stopServer(serverTranLog, "WTRN0017W");
                break;
            case testSqlRecoveryLogUpdatesUpdatesAtRestore:
                try {
                    stopServer(serverDbTranLog, "WTRN0017W");
                } finally {
                    DerbyNetworkUtilities.stopDerbyNetwork(DERBY_TXLOG_PORT);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Verify the TM does not run initial local recovery during checkpoint at beforeAppStart.
     * when dataSourceRef is set -- i,e, when recovery logs will store to an RDBMS. This
     * test ensures checkpoint overrides recoverOnStartup="true".
     */
    @Test
    public void testCheckpointBasIgnoresRosWhenSqlRecoveryLog() throws Exception {
        assertNull("Local recovery must not start during checkpoint BAS when SQL recovery logging is configured, but did",
                   serverServletStartupDbTranLog.waitForStringInLogUsingMark("CWRLS0010I:.*checkpointTransactionServletStartupDbLog", 1000));

        serverServletStartupDbTranLog.checkpointRestore();

        assertNotNull("The StartupServlet.init() method should complete a user transaction during restore, but did not.",
                      serverServletStartupDbTranLog.waitForStringInLogUsingMark("StartupServlet init completed without exception"));
    }

    /**
     * Verify the TM does not run initial local recovery during checkpoint for the default
     * tranlog and recoverOnStartup configuration.
     */
    @Test
    public void testCheckpointNoRosWithDefaultRecoveryLog() throws Exception {
        // The test app does not require a TX  at server start, so neither the setup to
        // start+stop the server nor the server checkpoint should start initial local recovery.
        assertTrue("The default recovery log should not exist before nor after checkpoint, but does.",
                   !serverTranLog.fileExistsInLibertyServerRoot("/tranlog"));

        assertNull("Local recovery should not start during checkpoint for the default transaction configuration, but did",
                   serverTranLog.waitForStringInLogUsingMark("CWRLS0010I:.*checkpointTransactionServlet", 1000));

        serverTranLog.checkpointRestore();

        runTest(serverTranLog, "testLTCAfterGlobalTran");
    }

    /**
     * Verify a server checkpoint preserves and uses the default tranlog when it
     * already exists from a previous server run, as it would when exercised in a container
     * build. The TM should run initial local recovery during checkpoint.
     */
    @Test
    public void testCheckpointRosWithDefaultRecoveryLog() throws Exception {
        assertTrue("The default recovery log should exist before and after checkpoint, but does not.",
                   serverTranLog.fileExistsInLibertyServerRoot("/tranlog"));

        assertNotNull("Local recovery should start during checkpoint for the default transaction configuration, but did not",
                      serverTranLog.waitForStringInLogUsingMark("CWRLS0010I:.*checkpointTransactionServlet"));

        serverTranLog.checkpointRestore();

        runTest(serverTranLog, "testLTCAfterGlobalTran");
    }

    /**
     * Verify a restored server logs transactions to the transactionLogDirectory
     * overridden by server.env. The test ensures the transaction configuration
     * updates during checkpoint-restore and starts recovery using the updated
     * transactionLogDirectory.
     */
    @Test
    public void testTranactionLogDirectoryUpdatesAtRestore() throws Exception {
        assertTrue("Server checkpoint should preserve directory ${server.output.dir}/tranlog, but did not.",
                   serverTranLog.fileExistsInLibertyServerRoot("/tranlog"));

        serverTranLog.checkpointRestore();

        assertTrue("The restored server should not remove directory ${server.output.dir}/tranlog, but did.",
                   serverTranLog.fileExistsInLibertyServerRoot("/tranlog"));

        runTest(serverTranLog, "testLTCAfterGlobalTran");

        assertTrue("The restored server should log transactions to directory ${server.output.dir}NEW_TRANLOG_DIR, but did not.",
                   serverTranLog.fileExistsInLibertyServerRoot("/NEW_TRANLOG_DIR"));
    }

    /**
     * Verify a restored server logs transactions a data source, where
     * the data source configuration and the transaction configuration both
     * update during checkpoint-restore.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException",
                    "javax.resource.spi.ResourceAllocationException" })
    public void testSqlRecoveryLogUpdatesUpdatesAtRestore() throws Exception {
        serverDbTranLog.checkpointRestore();

        // Exercise a transaction and start logging to the data source.
        // The server will throw an exception and fail this test when
        // the TM cannot establish a connection to the database.
        runTest(serverDbTranLog, "testLTCAfterGlobalTran");
    }

    private void runTest(LibertyServer ls, String testName) throws Exception {
        StringBuilder sb = null;
        try {
            sb = runTestWithResponse(ls, SERVLET_NAME, testName);
        } finally {
            Log.info(this.getClass(), testName, testName + " returned: " + sb);
        }
    }

    static enum TestMethod {
        testCheckpointBasIgnoresRosWhenSqlRecoveryLog,
        testCheckpointNoRosWithDefaultRecoveryLog,
        testCheckpointRosWithDefaultRecoveryLog,
        testTranactionLogDirectoryUpdatesAtRestore,
        testSqlRecoveryLogUpdatesUpdatesAtRestore,
        unknown;
    }
}
