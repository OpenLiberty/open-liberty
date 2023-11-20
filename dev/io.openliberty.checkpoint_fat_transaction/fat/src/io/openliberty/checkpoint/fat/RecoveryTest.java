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

import static io.openliberty.checkpoint.fat.util.FATUtils.LOG_SEARCH_TIMEOUT;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@CheckpointTest
public class RecoveryTest extends RecoveryTestBase {

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EE8FeatureReplacementAction().forServers(SERVER_NAME));

    @Server(SERVER_NAME)
    public static LibertyServer server;

    static final String TX_RETRY_INT = "11";
    static final String TX_LOG_DIR = "${server.output.dir}RESTORE_TRANLOG";

    @BeforeClass
    public static void setUpClass() throws Exception {
        Log.info(RecoveryTest.class, "subBefore", server.getServerName());

        ShrinkHelper.cleanAllExportedArchives();

        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "servlets.recovery.*");

        Consumer<LibertyServer> preRestoreLogic = checkpointServer -> {
            // Env var change that triggers the transaction config to update at restore
            File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
            try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                serverEnvWriter.println("TX_RETRY_INT=" + TX_RETRY_INT);
                serverEnvWriter.println("TX_LOG_DIR=" + TX_LOG_DIR);
            } catch (FileNotFoundException e) {
                throw new UncheckedIOException(e);
            }
            // Verify the application starts during checkpoint
            assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before restore",
                          checkpointServer.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
            assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                          checkpointServer.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
        };
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, preRestoreLogic);
        server.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
        server.startServer();

        setUp(server);
    }

}