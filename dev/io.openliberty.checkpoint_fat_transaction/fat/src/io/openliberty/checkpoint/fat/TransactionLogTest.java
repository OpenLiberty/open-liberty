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

import static io.openliberty.checkpoint.fat.FATSuite.deleteTranlogDb;
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

    static String DERBY_TXLOG_DS_JNDINAME = "jdbc/tranlogDataSource"; // Must differ from server.xml

    static LibertyServer serverTranLog;
    static LibertyServer serverTranDbLog;

    TestMethod testMethod;

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        switch (testMethod) {
            case testCheckpointRemovesDefaultTranlogDir:
                serverTranLog = LibertyServerFactory.getLibertyServer("checkpointTransactionServlet");
                ShrinkHelper.defaultApp(serverTranLog, APP_NAME, "servlets.simple.*");

                // Initialize tran logs in ${server.output.dir}/tranlog
                deleteTranlogDir(serverTranLog);
                serverTranLog.startServer();
                stopServer(serverTranLog);

                serverTranLog.setCheckpoint(new CheckpointInfo(CheckpointPhase.APPLICATIONS, false, null));
                serverTranLog.startServer();
                break;
            case testTransactionDbLogBasicConnection:
                serverTranDbLog = LibertyServerFactory.getLibertyServer("checkpointTransactionDbLog");
                ShrinkHelper.defaultApp(serverTranDbLog, APP_NAME, "servlets.simple.*");

                Consumer<LibertyServer> preRestoreLogic = checkpointServer -> {
                    // Configure the tran db log datasource's jndiName used by the tx service at restore
                    File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
                    try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                        serverEnvWriter.println("DERBY_TXLOG_DS_JNDINAME=" + DERBY_TXLOG_DS_JNDINAME);
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                    // Verify the application starts during checkpoint
                    assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                                  serverTranDbLog.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                    assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                  serverTranDbLog.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
                };
                serverTranDbLog.setCheckpoint(CheckpointPhase.APPLICATIONS, false, preRestoreLogic);
                serverTranDbLog.setServerStartTimeout(300000);
                serverTranDbLog.startServer();
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
            case testTransactionDbLogBasicConnection:
                stopServer(serverTranDbLog, "WTRN0017W");
                deleteTranlogDb(serverTranDbLog);
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
        assertTrue("Checkpoint should have the deleted transaction log directory, but did not.",
                   !serverTranLog.fileExistsInLibertyServerRoot("/tranlog"));
    }

    // TODO: The tran log datasource does not reconfigure at restore. The issue will
    // be a limitation or require further work to resolve. The following test is ideal
    // to verify tranlog DS configuration at restore.

    /**
     * Test basic database connectivity w/ transactions logging to an RDB.
     */
    @Test
    public void testTransactionDbLogBasicConnection() throws Exception {
        serverTranDbLog.checkpointRestore();
        runTest("testBasicConnection", serverTranDbLog);
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
        testCheckpointRemovesDefaultTranlogDir,
        testTransactionDbLogBasicConnection,
        unknown;
    }
}
