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

import static io.openliberty.checkpoint.fat.FATSuite.setMockCheckpoint;
import static io.openliberty.checkpoint.fat.util.FATUtils.LOG_SEARCH_TIMEOUT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Transaction;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class MockServletTest extends FATServletClient {

    static final String SERVER_NAME = "checkpointTransactionMockServlet";

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EE8FeatureReplacementAction().forServers(SERVER_NAME));

    static final String APP_NAME = "transactionservlet";
    static final String SERVLET_NAME = APP_NAME + "/SimpleServlet";

    @Server(SERVER_NAME)
    //@TestServlet(servlet = SimpleServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    private static String DERBY_DS_JNDINAME = "jdbc/derby";
    private static String TRANLOG_DIR = "${server.output.dir}tranlogDir";

    @BeforeClass
    public static void beforeClass() throws Exception {
        ShrinkHelper.cleanAllExportedArchives();

        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "servlets.simple.*");

        // When checkpoint-restore uses stubbed criu operation, server.env vars  will
        // override config vars at checkpoint, only. To override config vars at restore,
        // set a breakpoint in SymbolRegistry.resolveStringSymbols() and manually override
        // values returned by System.getEnv(var-name). Restore will run a config update
        // when it detects the changes. Or, dynamically update the config vars in server.xml
        // after the restored server is ready to run a smarter planet.
        File serverEnvFile = new File(server.getServerRoot() + "/server.env");
        try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
            serverEnvWriter.println("DERBY_DS_JNDINAME=" + DERBY_DS_JNDINAME);
            serverEnvWriter.println("TRANLOG_DIR=" + TRANLOG_DIR);
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
        setMockCheckpoint(server, CheckpointPhase.AFTER_APP_START);
        server.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server.isStarted()) {
            server.stopServer("WTRN0017W");
        }
    }

    //@Test
    public void testMockDynamicUpdateLTCAfterGlobalTran() throws Exception {
        setMockCheckpoint(server, CheckpointPhase.AFTER_APP_START);
        try {
            server.startServer();
            // Update config vars used by datasource and transaction
            ServerConfiguration serverConfig = server.getServerConfiguration();
            List<Variable> serverVars = serverConfig.getVariables();
            for (Variable sv : serverVars) {
                if (sv.getName() == "TRANLOG_DIR")
                    sv.setValue(TRANLOG_DIR);
                if (sv.getName() == "DERBY_DS_JNDINAME")
                    sv.setValue(DERBY_DS_JNDINAME);
            }
            Transaction txConfig = serverConfig.getTransaction();
            txConfig.setTransactionLogDirectory(TRANLOG_DIR);
            server.updateServerConfiguration(serverConfig);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), new String[] {});

            // Verify tm operation known to be sensitive to config update
            runTest("testLTCAfterGlobalTran", server);
        } finally {
            if (server.isStarted()) {
                server.stopServer("WTRN0017W");
            }
        }
    }

    @Test
    public void testMockLTCAfterGlobalTran() throws Exception {
        // Verify tm operation known to be sensitive to config update
        runTest("testLTCAfterGlobalTran", server);
    }

    private void runTest(String testName, LibertyServer ls) throws Exception {
        StringBuilder sb = null;
        try {
            sb = runTestWithResponse(ls, SERVLET_NAME, testName);
        } finally {
            Log.info(this.getClass(), testName, testName + " returned: " + sb);
        }
    }

}
