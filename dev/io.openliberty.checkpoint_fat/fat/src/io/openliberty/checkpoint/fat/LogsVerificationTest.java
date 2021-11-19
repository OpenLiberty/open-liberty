/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class LogsVerificationTest {

    public static final String APP_NAME = "app2";

    @Server("FATServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "app2");
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    private void configureBootStrapProperties(Map<String, String> properties) throws Exception, IOException, FileNotFoundException {
        Properties bootStrapProperties = new Properties();
        File bootStrapPropertiesFile = new File(server.getFileFromLibertyServerRoot("bootstrap.properties").getAbsolutePath());
        bootStrapProperties.put("bootstrap.include", "../testports.properties");
        bootStrapProperties.putAll(properties);
        try (OutputStream out = new FileOutputStream(bootStrapPropertiesFile)) {
            bootStrapProperties.store(out, "");
        }
    }

    @Test
    public void testMessagesAndTraceLogsCreatedNewOnCheckpointRestore() throws Exception {
        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
        server.startServer();

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
        configureBootStrapProperties(properties);

        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
        server.startServer();

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
        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
        server.startServer();
        assertEquals("Expected checkpoint message not found", 1, server.findStringsInLogs("CWWKC0451I", server.getDefaultLogFile()).size());

        RemoteFile messagesLog = server.getDefaultLogFile();

        assertTrue("Messages log not deleted", messagesLog.delete());

        server.checkpointRestore();

        assertEquals("Checkpoint message was not expected here", 0, server.findStringsInLogs("CWWKC0451I", server.getDefaultLogFile()).size());
        assertEquals("Expected restore message not found", 1, server.findStringsInLogs("CWWKC0452I", server.getDefaultLogFile()).size());
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

}
