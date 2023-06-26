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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.DerbyNetworkUtilities;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Verify checkpoint behaviors over transaction logging configurations.
 */
@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class TransactionLogTest extends FATServletClient {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers("checkpointTransactionServlet", "checkpointTransactionDbLog").fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers("checkpointTransactionServlet", "checkpointTransactionDbLog").fullFATOnly());

    static final String APP_NAME = "transactionservlet";
    static final String SERVLET_NAME = APP_NAME + "/SimpleServlet";

    // User sever.env vars to override these config vars in server.xml
    static final int DERBY_TXLOG_PORT = 1619;
    static final String DERBY_DS_JNDINAME = "jdbc/derby";
    static final String TX_LOG_DIR = "${server.config.dir}NEW_TRANLOG_DIR";
    static final String TX_RETRY_INT = "11";

    static LibertyServer serverTranLog;
    static LibertyServer serverDbTranLog;

    TestMethod testMethod;
    Consumer<LibertyServer> preRestoreLogic;

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        preRestoreLogic = null;
        switch (testMethod) {
            case testCheckpointRemovesDefaultTranlogDir:
                serverTranLog = LibertyServerFactory.getLibertyServer("checkpointTransactionServlet");
                ShrinkHelper.defaultApp(serverTranLog, APP_NAME, "servlets.simple.*");

                // Initialize tran logs in ${server.output.dir}/tranlog
                deleteTranlogDir(serverTranLog);
                serverTranLog.startServer();
                stopServer(serverTranLog);

                serverTranLog.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, null));
                serverTranLog.startServer();
                break;
            case testUpdateTranlogDirAtRestore:
                serverTranLog = LibertyServerFactory.getLibertyServer("checkpointTransactionServlet");
                ShrinkHelper.defaultApp(serverTranLog, APP_NAME, "servlets.simple.*");

                deleteTranlogDir(serverTranLog); // Clear up

                preRestoreLogic = checkpointServer -> {
                    // Override the app datasource and transaction configurations for restore.
                    File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
                    try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                        serverEnvWriter.println("DERBY_DS_JNDINAME=" + DERBY_DS_JNDINAME);
                        serverEnvWriter.println("TX_LOG_DIR=" + TX_LOG_DIR);
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                };
                serverTranLog.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, preRestoreLogic));
                serverTranLog.startServer();
                break;
            case testUpdateTranlogDatasourceAtRestore:
                DerbyNetworkUtilities.startDerbyNetwork(DERBY_TXLOG_PORT);

                serverDbTranLog = LibertyServerFactory.getLibertyServer("checkpointTransactionDbLog");
                ShrinkHelper.defaultApp(serverDbTranLog, APP_NAME, "servlets.simple.*");

                preRestoreLogic = checkpointServer -> {
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
                serverDbTranLog.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, preRestoreLogic);
                serverDbTranLog.setServerStartTimeout(300000);
                serverDbTranLog.startServer();
                break;
            default:
                break;
        }
    }

    @After
    public void tearDown() throws Exception {
        switch (testMethod) {
            case testCheckpointRemovesDefaultTranlogDir:
                stopServer(serverTranLog, "WTRN0017W");
                break;
            case testUpdateTranlogDirAtRestore:
                stopServer(serverTranLog, "WTRN0017W");
                break;
            case testUpdateTranlogDatasourceAtRestore:
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

    @AfterClass
    public static void tearDownClass() throws Exception {
        ShrinkHelper.cleanAllExportedArchives();
    }

    /**
     * Verify checkpoint at=applications fails when transactions log to file and the
     * transaction log directory already exists.
     */
    @Test
    public void testCheckpointRemovesDefaultTranlogDir() throws Exception {
        // Checkpoint should fail iff the pre-existing tran logs could not
        // be deleted, which we can't test for. But we can ensure the TM
        // otherwise removed the tran logs.
        assertTrue("Checkpoint should have the deleted transaction log directory, but did not.",
                   !serverTranLog.fileExistsInLibertyServerRoot("/tranlog"));

        serverTranLog.checkpointRestore();
    }

    /**
     * Verify a restored server logs transactions only to a directory path
     * overridden by server.env. The test ensures the transaction configuration
     * updates during checkpoint-restore and starts recovery using the updated
     * transactionLogDirectory.
     */
    @Test
    public void testUpdateTranlogDirAtRestore() throws Exception {
        assertTrue("The transaction log directory configured in server.xml should not exist, but it does.",
                   !serverTranLog.fileExistsInLibertyServerRoot("/tranlog"));

        serverTranLog.checkpointRestore();
        runTest(serverTranLog, "testLTCAfterGlobalTran");

        assertTrue("The server should log transactions to directory ${server.output.dir}NEW_TRANLOG_DIR, but did not.",
                   serverTranLog.fileExistsInLibertyServerRoot("/NEW_TRANLOG_DIR"));

        assertTrue("The server should not log transactions to the directory configured in server.xml, but did.",
                   !serverTranLog.fileExistsInLibertyServerRoot("/tranlog"));
    }

    /**
     * Verify a restored server logs transactions a data source, where
     * the data source configuration and the transaction configuration both
     * update during checkpoint-restore.
     */
    @Test
    public void testUpdateTranlogDatasourceAtRestore() throws Exception {
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
        testCheckpointRemovesDefaultTranlogDir,
        testUpdateTranlogDirAtRestore,
        testUpdateTranlogDatasourceAtRestore,
        unknown;
    }
}
