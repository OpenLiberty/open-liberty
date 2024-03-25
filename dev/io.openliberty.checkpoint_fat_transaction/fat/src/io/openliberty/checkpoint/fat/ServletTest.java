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

import static io.openliberty.checkpoint.fat.FATSuite.stopServer;
import static io.openliberty.checkpoint.fat.util.FATUtils.LOG_SEARCH_TIMEOUT;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import servlets.simple.SimpleServlet;

/**
 * ServletTest is a basic bringup test for transaction management services
 * and the JTA provider for checkpoint and restore.
 *
 * The test verifies that...
 *
 * Servlets have JNDI access to transaction mgmt APIs and can perform basic
 * tx operations upon restoring a server checkpointed AFTER_APP_START.
 *
 * Changes to the datasource configuration update during restore before the
 * datasource is injected into the test servlet.
 *
 * Changes to the transaction configuration and transaction mgmt services update
 * at restore.
 *
 * Initial recovery executes during checkpoint, where the logs created in the
 * default directory (${server.output.dir}/tranlog) are used during restore.
 *
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class ServletTest extends FATServletClient {

    static final String SERVER_NAME = "checkpointTransactionServlet";

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EE8FeatureReplacementAction().forServers(SERVER_NAME))
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    static final String APP_NAME = "transactionservlet";
    static final String SERVLET_NAME = APP_NAME + "/SimpleServlet";

    @Server(SERVER_NAME)
    @TestServlet(servlet = SimpleServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    // Overrides for config vars in server.xml
    static final String DERBY_DS_JNDINAME = "jdbc/derby";
    static final String TX_LOG_DIR = "${server.output.dir}TRANLOG_DIR";
    static final int TX_RETRY_INT = 15;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.cleanAllExportedArchives();

        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "servlets.simple.*");

        Consumer<LibertyServer> preRestoreLogic = checkpointServer -> {
            // Env vars that override the application datasource and transaction
            // configurations at restore; use the default tx log dir
            File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
            try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                serverEnvWriter.println("DERBY_DS_JNDINAME=" + DERBY_DS_JNDINAME);
                serverEnvWriter.println("TX_RETRY_INT =" + TX_RETRY_INT);
                //serverEnvWriter.println("TX_LOG_DIR=" + TX_LOG_DIR);
            } catch (FileNotFoundException e) {
                throw new UncheckedIOException(e);
            }
            // Verify the application starts during checkpoint
            assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                          checkpointServer.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
            assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                          checkpointServer.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
        };
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, preRestoreLogic);
        server.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
        server.startServer();
        server.checkpointRestore();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        stopServer(server, "WTRN0017W"); // Unable to begin nested tran; nested trans not supported
    }

}
