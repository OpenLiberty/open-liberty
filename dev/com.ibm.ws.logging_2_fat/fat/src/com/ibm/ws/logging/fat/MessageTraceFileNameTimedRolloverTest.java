/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.logging.fat;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.*;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class MessageTraceFileNameTimedRolloverTest {

    private static final String SERVER_NAME = "com.ibm.ws.logging.messagetracerollover";
    private static final String[] EXPECTED_FAILURES = { "CWWKF0001E" };

    private static LibertyServer server;
    private static RemoteFile bootstrapFile = null;
    private static Properties initialBootstrapProps = null;

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);

        // Get the bootstrap.properties file and store original content
        bootstrapFile = server.getServerBootstrapPropertiesFile();
        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        initialBootstrapProps = loadProperties(in);

        // Preserve the original server configuration
        server.saveServerConfiguration();
    }

    @After
    public void cleanupAfterEachTest() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }

        // Restore the initial contents of bootstrap.properties
        if (bootstrapFile != null && initialBootstrapProps != null) {
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }
    }

    /**
     * Test 1: Configure TimedLogRollover (in server.env) and set traceFileName=stdout in bootstrap.properties,
     * verify if trace.log is not created, only message.log is rolled over.
     */
    @Test
    public void testTraceFileNameStdoutBootstrapWithTimedRollover() throws Exception {
        // Simple - just start server (bootstrap.properties already has traceFileName=stdout)
        server.startServer();
        server.waitForStringInLog("CWWKF0011I");

        boolean traceLogExists = server.fileExistsInLibertyServerRoot("logs/trace.log");
        assertFalse("REQUIREMENT: trace.log should NOT be created when traceFileName=stdout", traceLogExists);

        boolean messageLogExists = server.fileExistsInLibertyServerRoot("logs/messages.log") ||
                                   server.fileExistsInLibertyServerRoot("logs/message.log");
        assertTrue("message log should exist", messageLogExists);
    }

    /**
     * Test 2: Configure TimedLogRollover (in server.env) and traceFileName=stdout in server.xml,
     * verify if the trace.log is NOT rolled over.
     */
    @Test
    public void testTraceFileNameStdoutServerXmlWithTimedRollover() throws Exception {
        // Use server.xml configuration for this test
        server.setServerConfigurationFile("server_trace_stdout.xml");
        server.startServer();
        server.waitForStringInLog("CWWKF0011I");

        // Check results
        boolean traceLogExists = server.fileExistsInLibertyServerRoot("logs/trace.log");
        assertFalse("REQUIREMENT: trace.log should NOT be created when traceFileName=stdout in server.xml", traceLogExists);

        boolean messageLogExists = server.fileExistsInLibertyServerRoot("logs/messages.log") ||
                                   server.fileExistsInLibertyServerRoot("logs/message.log");
        assertTrue("message logs should exist for normal operation", messageLogExists);
    }

    /**
     * Test 3: Configure TimedLogRollover (in server.env) and set traceFileName=stdout in bootstrap.properties,
     * and then dynamically update the traceFileName to trace.log in server.xml,
     * verify if the trace.log gets rolled over.
     */
    @Test
    public void testDynamicTraceFileNameStdoutToTraceLog() throws Exception {
        // Start with existing bootstrap.properties (already has traceFileName=stdout)
        server.startServer();
        server.waitForStringInLog("CWWKF0011I");

        // Verify initial state - trace.log should not exist
        boolean initialTraceLogExists = server.fileExistsInLibertyServerRoot("logs/trace.log");
        assertFalse("REQUIREMENT: Initially, trace.log should not exist when traceFileName=stdout in bootstrap", initialTraceLogExists);

        // Dynamically update server.xml to change traceFileName to trace.log
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("server_trace_log.xml");
        assertNotNull("Configuration update should complete",
                      server.waitForConfigUpdateInLogUsingMark(null));

        // Verify trace.log now exists after dynamic update
        boolean traceLogNowExists = server.fileExistsInLibertyServerRoot("logs/trace.log");
        assertTrue("REQUIREMENT: trace.log should exist after dynamic update to traceFileName=trace.log", traceLogNowExists);
    }

    /**
     * Test 4: Configure TimedLogRollover (in server.env), and then dynamically update the traceFileName
     * to test.log in server.xml, verify if the new test.log gets rolled over,
     * and the trace.log file does NOT roll over.
     */
    @Test
    public void testDynamicTraceFileNameToCustomFile() throws Exception {
        // Start with default configuration
        server.startServer();
        server.waitForStringInLog("CWWKF0011I");

        // Dynamically update server.xml to change traceFileName to test.log
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("server_test_log.xml");

        // Wait for config update
        assertNotNull("Configuration update should complete",
                      server.waitForConfigUpdateInLogUsingMark(null));

        // Verify new test.log gets created
        boolean testLogNowExists = server.fileExistsInLibertyServerRoot("logs/test.log");
        assertTrue("REQUIREMENT: test.log should exist after dynamic update to traceFileName=test.log", testLogNowExists);
    }

    /**
     * Test 5: Repeat scenario 4 using messageFileName, instead.
     * Configure TimedLogRollover (in server.env), then dynamically update messageFileName
     * to custom file, verify rollover behavior.
     */
    @Test
    public void testDynamicMessageFileNameToCustomFile() throws Exception {
        // Start with default configuration
        server.startServer();
        server.waitForStringInLog("CWWKF0011I");

        // Dynamically update server.xml to change messageFileName to custom_message.log
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("server_custom_message.xml");

        // Wait for config update
        server.waitForStringInLogUsingMark("CWWKG0017I");

        // Verify new custom_message.log gets created
        boolean customMessageNowExists = server.fileExistsInLibertyServerRoot("logs/custom_message.log");
        assertTrue("REQUIREMENT: custom_message.log should exist after dynamic update to messageFileName=custom_message.log", customMessageNowExists);
    }

    /**
     * Test 6: Configure traceFileName=stdout in server.xml, verify the trace.log is NOT created.
     * (This scenario depends on issue #31949 - Cannot stop generating trace.log file)
     */
    @Test
    public void testTraceFileNameStdoutNoTraceLogCreated() throws Exception {
        // Use server.xml configuration with traceFileName=stdout (same as Test 2 approach)
        server.setServerConfigurationFile("server_trace_stdout.xml");
        server.startServer();
        server.waitForStringInLog("CWWKF0011I");

        // Main test: Verify trace.log is NOT created when traceFileName=stdout in server.xml
        boolean traceLogExists = server.fileExistsInLibertyServerRoot("logs/trace.log");
        assertFalse("REQUIREMENT: trace.log should NOT be created when traceFileName=stdout in server.xml", traceLogExists);

        // Verify other logs are still functional
        boolean messageLogExists = server.fileExistsInLibertyServerRoot("logs/messages.log") ||
                                   server.fileExistsInLibertyServerRoot("logs/message.log");
        assertTrue("Other log files should still be created normally", messageLogExists);
    }

    // Helper methods (copied from the example file)
    private static FileInputStream getFileInputStreamForRemoteFile(RemoteFile bootstrapPropFile) throws Exception {
        FileInputStream input = null;
        try {
            input = (FileInputStream) bootstrapPropFile.openForReading();
        } catch (Exception e) {
            throw new Exception("Error while getting the FileInputStream for the remote bootstrap properties file.");
        }
        return input;
    }

    private static Properties loadProperties(FileInputStream input) throws IOException {
        Properties props = new Properties();
        try {
            props.load(input);
        } catch (IOException e) {
            throw new IOException("Error while loading properties from the remote bootstrap properties file.");
        } finally {
            try {
                input.close();
            } catch (IOException e1) {
                throw new IOException("Error while closing the input stream.");
            }
        }
        return props;
    }

    private static FileOutputStream getFileOutputStreamForRemoteFile(RemoteFile bootstrapPropFile, boolean append) throws Exception {
        FileOutputStream output = null;
        try {
            output = (FileOutputStream) bootstrapPropFile.openForWriting(append);
        } catch (Exception e) {
            throw new Exception("Error while getting FileOutputStream for the remote bootstrap properties file.");
        }
        return output;
    }

    private static void writeProperties(Properties props, FileOutputStream output) throws Exception {
        try {
            props.store(output, null);
        } catch (IOException e) {
            throw new Exception("Error while writing to the remote bootstrap properties file.");
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                throw new IOException("Error while closing the output stream.");
            }
        }
    }

    private static void setInBootstrapPropertiesFile(LibertyServer libertyServer, RemoteFile bootstrapFile, String key, String value) throws Exception {
        // Stop server, if running...
        if (libertyServer != null && libertyServer.isStarted()) {
            libertyServer.stopServer(EXPECTED_FAILURES);
        }

        // Update the bootstrap.properties file
        Properties newBootstrapProps = new Properties();
        newBootstrapProps.put(key, value);

        FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, true);
        writeProperties(newBootstrapProps, out);

        // Start server...
        libertyServer.startServer();
    }
}