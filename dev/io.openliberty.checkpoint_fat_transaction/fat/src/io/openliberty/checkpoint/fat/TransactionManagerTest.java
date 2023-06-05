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

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.FATSuite.stopServer;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Transaction;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.DerbyNetworkUtilities;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Verify startup behaviors for the transaction manager and recovery services.
 */
@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class TransactionManagerTest extends FATServletClient {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers("checkpointTransactionServlet", "checkpointTransactionDbLog").fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers("checkpointTransactionServlet", "checkpointTransactionDbLog").fullFATOnly());

    static final String APP_NAME = "transactionservlet";
    static final String SERVLET_NAME = APP_NAME + "/SimpleServlet";

    static LibertyServer serverTranLogRecOnStart;
    static LibertyServer serverTranDbLogNoRecOnStart;

    TestMethod testMethod;
    Transaction tm;

    static String DERBY_DS_JNDINAME = "jdbc/derby"; // Differs from server config
    static final int DERBY_TXLOG_PORT = 9099; // Same as server config

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        Consumer<LibertyServer> preRestoreLogic;
        switch (testMethod) {
            case testTransactionManagerStartsDuringRestore:
                serverTranLogRecOnStart = LibertyServerFactory.getLibertyServer("checkpointTransactionServlet");

                ShrinkHelper.defaultApp(serverTranLogRecOnStart, APP_NAME, "servlets.simple.*");

                preRestoreLogic = checkpointServer -> {
                    // The datasource jndiName in server.xml is invalid. At restore reconfigure
                    // the jndiName to that used by SimpleServlet
                    File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
                    try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                        serverEnvWriter.println("DERBY_DS_JNDINAME=" + DERBY_DS_JNDINAME);
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                    assertNull("The transaction manager started during checkpoint",
                               serverTranLogRecOnStart.waitForStringInTraceUsingMark("doStartup0", 1000));
                    assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log.",
                                  serverTranLogRecOnStart.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                    assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                  serverTranLogRecOnStart.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
                };
                serverTranLogRecOnStart.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, preRestoreLogic);
                serverTranLogRecOnStart.setServerStartTimeout(300000);
                serverTranLogRecOnStart.startServer();
                break;
            case testRecoveryBeginsAfterStartup:
                DerbyNetworkUtilities.startDerbyNetwork(DERBY_TXLOG_PORT);

                serverTranDbLogNoRecOnStart = LibertyServerFactory.getLibertyServer("checkpointTransactionDbLog");
                ServerConfiguration config = serverTranDbLogNoRecOnStart.getServerConfiguration();
                config.getTransaction().setRecoverOnStartup(false);
                serverTranDbLogNoRecOnStart.updateServerConfiguration(config);

                ShrinkHelper.defaultApp(serverTranDbLogNoRecOnStart, APP_NAME, "servlets.simple.*");

                preRestoreLogic = checkpointServer -> {
                    File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
                    try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                        serverEnvWriter.println("DERBY_DS_JNDINAME=" + DERBY_DS_JNDINAME);
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                    assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log.",
                                  serverTranDbLogNoRecOnStart.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                    assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                  serverTranDbLogNoRecOnStart.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
                };
                serverTranDbLogNoRecOnStart.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, preRestoreLogic);
                serverTranDbLogNoRecOnStart.setServerStartTimeout(300000);
                serverTranDbLogNoRecOnStart.startServer();
                break;
            default:
                break;
        }
    }

    @After
    public void tearDown() throws Exception {
        switch (testMethod) {
            case testTransactionManagerStartsDuringRestore:
                stopServer(serverTranLogRecOnStart, "WTRN0017W");
                break;
            case testRecoveryBeginsAfterStartup:
                try {
                    stopServer(serverTranDbLogNoRecOnStart, "WTRN0017W");
                } finally {
                    DerbyNetworkUtilities.stopDerbyNetwork(DERBY_TXLOG_PORT);
                }
                break;
            default:
                break;
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        ShrinkHelper.cleanAllExportedArchives();
    }

    /**
     * Ensure the Transaction Manager service startup (TMImpl.doStart()) executes
     * during restore, only, for a server checkpointed at=applications.
     *
     * When recoverOnStartup=true the call to TMImpl.doStart() will discover or
     * initialize the tran logs and attempt to recover transactions during server startup.
     *
     * These behaviors must execute during restore, only.
     */
    @Test
    public void testTransactionManagerStartsDuringRestore() throws Exception {

        assertFalse("After checkpoint the tranlog directory \\`tranlog\\' should not exist in the server output directory, but it does",
                    serverTranLogRecOnStart.fileExistsInLibertyServerRoot("/tranlog"));

        serverTranLogRecOnStart.checkpointRestore();

        assertNotNull("Recovery processing did not begin during server startup, but should when isRecoveryOnStartup=true",
                      serverTranLogRecOnStart.waitForStringInLogUsingMark("CWRLS0010I:.*checkpointTransactionServlet"));

        // Quick verification of TM service operation
        runTest("testTransactionEnlistment", serverTranLogRecOnStart);
    }

    /**
     * Ensure the TM service does not begin recovery before server startup completes
     * during restore.
     *
     * The TM logs to an RDB (dataSourceRef="tranlogDataSource") and should not
     * start recovery during server startup when recoverOnStartup="false".
     */
    @Test
    public void testRecoveryBeginsAfterStartup() throws Exception {

        serverTranDbLogNoRecOnStart.checkpointRestore();

        assertNull("Recovery processing began during server startup, but should not when isRecoveryOnStartup=false",
                   serverTranDbLogNoRecOnStart.waitForStringInLogUsingMark("CWRLS0010I:.*checkpointTransactionDbLog", 1000));

        runTest("testTransactionEnlistment", serverTranDbLogNoRecOnStart);
    }

    private void runTest(String testName, LibertyServer ls) throws Exception {
        StringBuilder sb = null;
        try {
            sb = runTestWithResponse(ls, SERVLET_NAME, testName);
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), testName, testName + " returned: " + sb);
    }

    static enum TestMethod {
        testTransactionManagerStartsDuringRestore,
        testRecoveryBeginsAfterStartup,
        unknown;
    }
}
