/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class JwtBuilderCheckpointTest {
    private static final String APP_NAME = "jwtbuilderstartup";

    @Rule
    public TestName testName = new TestName();

    @Server("checkpointJwtBuilder")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.defaultEERepeat("checkpointJwtBuilder");

    @Before
    public void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_NAME);
        FATSuite.copyAppsAppToDropins(server, APP_NAME);

        // Checkpoint at AFTER_APP_START.
        // autoRestore=false: the framework pauses after the snapshot is taken so the postCheckpointLambda can run while the server is frozen.
        // No ports are open while the server is frozen, so the lambda can only inspect the logs.
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false,
                             s -> {
                                 assertNotNull("jwtbuilderstartup app did not start before checkpoint",
                                               s.waitForStringInLogUsingMark("CWWKZ0001I:.*" + APP_NAME, 0));
                                 // The app's startup listener called JwtBuilder.create() before the checkpoint was taken
                                 // and printed the resulting exception message to the log.
                                 assertNotNull("Expected CWWKS6073E from JwtBuilder.create() during checkpoint",
                                               s.waitForStringInLogUsingMark("CWWKS6073E", 0));
                             });
        server.startServer(FATSuite.getTestMethodNameOnly(testName) + ".log");
    }

    @After
    public void tearDown() throws Exception {
        // Pass the expected error message ID so stopServer() does not fail on it
        server.stopServer("CWWKS6073E");
    }

    @Test
    public void testJwtBuilderCannotBuildDuringCheckpointButCanBuildAfterRestore() throws Exception {
        // The assertion that the JwtBuilder failed during checkpoint lives in the postCheckpointLambda,
        // which ran during startServer() in setUp(). Restore so the server can be stopped cleanly.
        server.checkpointRestore();

        // Test that the JwtBuilder works as normal after restore
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                          + "/" + APP_NAME + "/build");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        StringBuilder responseBody = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseBody.append(line);
            }
        }
        String response = responseBody.toString();

        // Assert the workload-identity claim was injected into the built JWT
        assertTrue("Expected workload-identity claim not found in response: " + response, response.contains("workload-identity="));
        assertFalse("Workload-identity claim was not injected: " + response, response.contains("workload-identity=null"));
        assertFalse("Unexpected exception building JWT after restore: " + response, response.contains("Caught an exception"));
        // Verify the workload-identity claim is as expected
        String workloadIdentity = response.substring("workload-identity=".length()).trim();
        String[] parts = workloadIdentity.split(",");
        // Validates the format contract: exactly 4 parts
        assertEquals("workload-identity claim should have 4 comma-separated parts", 4, parts.length);
        // parts[0] = hostname
        // parts[1] = usr dir
        // parts[2] = server name
        // parts[3] = app name
        assertFalse("hostname part should not be empty", parts[0].isEmpty());
        assertFalse("usr dir part should not be empty", parts[1].isEmpty());
        assertEquals("server name part should match", "checkpointJwtBuilder", parts[2].trim());
        assertEquals("app name part should match", APP_NAME, parts[3].trim());
    }
}
