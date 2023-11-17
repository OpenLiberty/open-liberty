/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class LogsChangeTraceSpecTest {
    @Rule
    public TestName testName = new TestName();
    public static final String APP_NAME = "app2";

    @Server("checkpointfat.log.change.tracespec")
    public static LibertyServer server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "app2");
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Before
    public void startServer() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer(getTestMethodNameOnly(testName) + ".log");
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
        server.deleteAllDropinConfigurations();
    }

    @Test
    public void testSetTraceSpec() throws Exception {
        server.addDropinOverrideConfiguration("dropinConfigChange/enableCheckpointTrace.xml");
        // Must set the logs root to the configured one; otherwise the check for successful restore will fail
        server.setLogsRoot(server.getServerRoot() + File.separator + "restore_log_dir" + File.separator);
        server.checkpointRestore();
    }

}
