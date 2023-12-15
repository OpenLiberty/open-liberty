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

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static io.openliberty.checkpoint.fat.FATSuite.updateVariableConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class AuditTest extends FATServletClient {

    public static final String APP_NAME = "app2";

    public static final String SERVER_NAME = "checkpointAudit";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, APP_NAME);
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
                             });
        server.startServer(getTestMethodNameOnly(testName) + ".log");
    }

    @Test
    public void testAuditMaxFilesUpdate() throws Exception {
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");

        server.stopServer(false, "");

        assertAuditLogsCount(1);

        server.checkpointRestore();

        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");

        server.stopServer(false, "");

        assertAuditLogsCount(1); // audit maxFiles is set to 1 in server.xml

        // update the audit maxFiles to 2
        updateVariableConfig(server, "maxFiles", "2");

        server.checkpointRestore();

        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");

        assertAuditLogsCount(2); // audit maxFiles is set to 2 in server.xml
    }

    private void assertAuditLogsCount(int expectedAuditLogFilesCount) throws Exception {
        RemoteFile logsDirectory = new RemoteFile(server.getMachine(), server.getLogsRoot());

        RemoteFile[] logs = logsDirectory.list(false);
        int actualAuditLogFilesCount = 0;

        for (RemoteFile f : logs) {
            if (f.getName().startsWith("audit") && f.getName().endsWith(".log")) {
                actualAuditLogFilesCount++;
            }
        }

        assertEquals("Expected number of audit logs not found", expectedAuditLogFilesCount, actualAuditLogFilesCount);
    }

    @After
    public void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }
}
