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

import static io.openliberty.checkpoint.fat.FATSuite.configureEnvVariable;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class LogsVerificationTest {
    @Rule
    public TestName testName = new TestName();
    public static final String APP_NAME = "app2";

    @Server("checkpointfat.log.verification.test")
    public static LibertyServer server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "app2");
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Test
    public void testMessagesAndTraceLogsCreatedNewOnCheckpointRestore() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer(getTestMethodNameOnly(testName) + ".log");

        server.checkpointRestore();

        server.stopServer(false, "");
        server.checkpointRestore();

        server.stopServer(false, "");
        server.checkpointRestore();

        RemoteFile logsDirectory = new RemoteFile(server.getMachine(), server.getLogsRoot());

        RemoteFile[] logs = logsDirectory.list(false);
        int actualMessagesLogCount = 0;
        int actualTraceLogCount = 0;

        // Default number of messages and trace log is 2. Even though server is restored more than 2 times the max log files will be 2
        int expectedMessagesLogCount = 2;
        int expectedTraceLogCount = 2;

        for (RemoteFile f : logs) {
            if (f.getName().startsWith("messages") && f.getName().endsWith(".log")) {
                actualMessagesLogCount++;
            } else if (f.getName().startsWith("trace") && f.getName().endsWith(".log")) {
                actualTraceLogCount++;
            }

        }

        assertEquals("Expected number of messages logs not found", expectedMessagesLogCount, actualMessagesLogCount);
        assertEquals("Expected number of trace logs not found", expectedTraceLogCount, actualTraceLogCount);
    }

    @Test
    public void testSetMaxLogFiles() throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("com.ibm.ws.logging.max.files", "4");
        FATSuite.configureBootStrapProperties(server, properties);

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer(getTestMethodNameOnly(testName) + ".log");

        server.checkpointRestore();

        server.stopServer(false, "");
        server.checkpointRestore();

        server.stopServer(false, "");
        server.checkpointRestore();

        RemoteFile logsDirectory = new RemoteFile(server.getMachine(), server.getLogsRoot());

        RemoteFile[] logs = logsDirectory.list(false);
        int actualMessagesLogCount = 0;
        int actualTraceLogCount = 0;

        // The bootstrap property com.ibm.ws.logging.max.files is set to 4
        int expectedMessagesLogCount = 4;
        int expectedTraceLogCount = 4;

        for (RemoteFile f : logs) {
            if (f.getName().startsWith("messages") && f.getName().endsWith(".log")) {
                actualMessagesLogCount++;
            } else if (f.getName().startsWith("trace") && f.getName().endsWith(".log")) {
                actualTraceLogCount++;
            }

        }

        assertEquals("Expected number of messages logs not found", expectedMessagesLogCount, actualMessagesLogCount);
        assertEquals("Expected number of trace logs not found", expectedTraceLogCount, actualTraceLogCount);
    }

    @Test
    public void testRestoreWorksAfterMessagesLogIsDeleted() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer(getTestMethodNameOnly(testName) + ".log");
        assertEquals("Expected checkpoint message not found", 1, server.findStringsInLogs("CWWKC0451I", server.getDefaultLogFile()).size());

        RemoteFile messagesLog = server.getDefaultLogFile();

        assertTrue("Messages log not deleted", messagesLog.delete());

        server.checkpointRestore();

        assertEquals("Checkpoint message was not expected here", 0, server.findStringsInLogs("CWWKC0451I", server.getDefaultLogFile()).size());
        assertEquals("Expected restore message not found", 1, server.findStringsInLogs("CWWKC0452I", server.getDefaultLogFile()).size());
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
    }

    @Test
    public void testRestoreHideMessageTRAS3001I() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer(getTestMethodNameOnly(testName) + ".log");
        assertEquals("Expected checkpoint message not found", 1, server.findStringsInLogs("CWWKC0451I", server.getDefaultLogFile()).size());

        server.checkpointRestore();
        assertNotNull("Expected TRAS3001I message not found", server.waitForStringInLogUsingMark("TRAS3001I", 100));
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
    }

    @Test
    public void testVariableSourceDirUpdateDuringRestore() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer(getTestMethodNameOnly(testName) + ".log");

        configureEnvVariable(server, singletonMap("VARIABLE_SOURCE_DIRS", "testSrcDir"));

        server.checkpointRestore();
        // CWWKG0017I: The server configuration was successfully updated
        assertEquals("Expected restore message not found", 1, server.findStringsInLogs("CWWKG0017I", server.getDefaultLogFile()).size());

    }

    @Test
    public void testEnvMessageFormatUpdateOnRestore() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer(getTestMethodNameOnly(testName) + ".log");

        RemoteFile messagesLog = new RemoteFile(server.getMachine(), server.getLogsRoot() + "/messages.log");
        try (JsonReader jsonReader = Json.createReader(new FileReader(messagesLog.getAbsolutePath()))) {
            assertFalse("Message format should not be json", isValidJson(jsonReader));
        }

        // Updating the message format to json using server.env
        configureEnvVariable(server, singletonMap("WLP_LOGGING_MESSAGE_FORMAT", "json"));

        server.checkpointRestore();

        try (JsonReader jsonReader = Json.createReader(new FileReader(messagesLog.getAbsolutePath()))) {
            assertTrue("Message format should be json", isValidJson(jsonReader));
        }
    }

    private boolean isValidJson(JsonReader reader) {
        try {
            reader.readObject();
        } catch (JsonParsingException e) {
            return false;
        }
        return true;
    }

    @After
    public void tearDown() throws Exception {
        try {
            server.stopServer();
        } finally {
            configureEnvVariable(server, emptyMap());
        }
    }

}
