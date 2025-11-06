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

import java.io.File;
import java.util.Calendar;

import org.junit.*;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class MessageTraceFileNameTimedRolloverTest {

    private static final Class<?> c = MessageTraceFileNameTimedRolloverTest.class;
    private static final String SERVER_NAME = "com.ibm.ws.logging.messagetracerollover";

    private static LibertyServer server;
    private static RemoteFile bootstrapFile = null;

    @Before
    public void cleanLogsBeforeEach() throws Exception {
        // ensure no residue between tests
        server.deleteDirectoryFromLibertyServerRoot("logs/");
        clearBootstrap(); // avoid leakage of traceFileName across tests
    }

    /** Write bootstrap content (overwrites). */
    private static void writeBootstrap(String content) throws Exception {
        if (bootstrapFile.exists()) {
            bootstrapFile.delete();
        }
        try (java.io.OutputStream os = bootstrapFile.openForWriting(false)) {
            os.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /**
     * Configure bootstrap.properties for these tests.
     * If {@code value} is null we explicitly set a conservative trace spec (*=info)
     * to prevent the FAT framework from enabling real trace and creating trace.log.
     * Otherwise we set the requested trace file name (e.g., "stdout").
     */
    private static void setBootstrapLoggingOverrides(String value) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("bootstrap.include=../testports.properties\n");
        if (value == null) {
            // Neutralize trace so framework doesn't create trace.log
            sb.append("com.ibm.ws.logging.trace.specification=*=info\n");
        } else {
            sb.append("com.ibm.ws.logging.trace.file.name=").append(value).append("\n");
        }
        writeBootstrap(sb.toString());
    }

    /** Clear bootstrap to just include test ports. */
    private static void clearBootstrap() throws Exception {
        writeBootstrap("bootstrap.include=../testports.properties\n");
    }

    /** Wait up to timeoutMs for a file to exist under logs/ */
    private static boolean waitForFileExists(String pathFromRoot, long timeoutMs) throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (server.fileExistsInLibertyServerRoot(pathFromRoot))
                return true;
            Thread.sleep(250);
        }
        return false;
    }

    /** Wait up to timeoutMs for any rolled files with the given prefix (e.g., "trace", "test") */
    private static int waitForRolledFiles(String prefix, long timeoutMs) {
        File logsDir = new File(server.getLogsRoot());
        long end = System.currentTimeMillis() + timeoutMs;
        int count = 0;
        while (System.currentTimeMillis() < end) {
            File[] rolled = logsDir.listFiles((dir, name) -> name.startsWith(prefix + "_") && name.endsWith(".log"));
            count = (rolled == null) ? 0 : rolled.length;
            if (count > 0)
                return count;
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                /* no-op */ }
        }
        return count;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);

        // Get the bootstrap.properties file reference
        bootstrapFile = server.getServerBootstrapPropertiesFile();

        // Clean server state
        if (server.isStarted()) {
            server.stopServer();
        }
        server.deleteDirectoryFromLibertyServerRoot("logs/");

        // Ensure a clean bootstrap baseline before any tests
        clearBootstrap();

        // Save original server configuration
        server.saveServerConfiguration();
    }

    @After
    public void tearDownAfterEachTest() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
        // Restore original server configuration after each test
        server.restoreServerConfiguration();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test 1: Configure TimedLogRollover (in server.env) and set traceFileName=stdout in bootstrap.properties,
     * verify trace.log is not created, only message.log is rolled over.
     */
    @Test
    public void testTraceFileNameStdoutBootstrapWithTimedRollover() throws Exception {
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "=== TEST 1: Requirements Check ===");
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "✓ Requirement: TimedLogRollover in server.env");
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "✓ Requirement: traceFileName=stdout in bootstrap.properties");
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "✓ Requirement: trace.log should NOT be created");
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "✓ Requirement: messages.log should roll over");

        setBootstrapLoggingOverrides("stdout");
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "Set traceFileName=stdout in bootstrap.properties");

        waitForBeginningOfMinute();
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "Started at beginning of minute: " + new java.util.Date());

        server.startServer();
        server.waitForStringInLog("CWWKF0011I");

        // Verify trace.log is NOT created
        boolean traceLogExists = server.fileExistsInLibertyServerRoot("logs/trace.log");
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "trace.log exists: " + traceLogExists + " (should be FALSE)");
        assertFalse("trace.log should NOT be created when traceFileName=stdout", traceLogExists);
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "VERIFIED: trace.log NOT created");

        // Verify messages.log exists
        boolean messageLogExists = server.fileExistsInLibertyServerRoot("logs/messages.log");
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "messages.log exists: " + messageLogExists + " (should be TRUE)");
        assertTrue("messages.log should exist", messageLogExists);
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "VERIFIED: messages.log exists");

        // Calculate next rollover time
        Calendar nextRollover = getNextRolloverTime(0, 1);
        long waitTime = nextRollover.getTimeInMillis() - System.currentTimeMillis() + 5000;

        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "Next rollover at: " + nextRollover.getTime());
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "Waiting " + (waitTime / 1000) + " seconds for rollover...");

        if (waitTime > 0) {
            Thread.sleep(waitTime);
        }

        // Check for rolled message files
        File logsDir = new File(server.getLogsRoot());
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "Checking directory: " + logsDir.getAbsolutePath());

        File[] allFiles = logsDir.listFiles();
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "All files in logs directory:");
        if (allFiles != null) {
            for (File f : allFiles) {
                Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "  - " + f.getName());
            }
        }

        File[] rolledFiles = logsDir.listFiles((dir, name) -> name.startsWith("messages_") && name.endsWith(".log"));

        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "Found " + (rolledFiles != null ? rolledFiles.length : 0) + " rolled message files");
        assertTrue("Message log should have rolled over", rolledFiles != null && rolledFiles.length > 0);
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "VERIFIED: messages.log rolled over");
        Log.info(c, "testTraceFileNameStdoutBootstrapWithTimedRollover", "=== TEST 1: COMPLETE - All requirements verified ===");
    }

    /**
     * Test 2: Configure TimedLogRollover (in server.env) and traceFileName=stdout in server.xml,
     * verify the trace.log is NOT created and NOT rolled.
     */
    @Test
    public void testTraceFileNameStdoutServerXmlWithTimedRollover() throws Exception {
        Log.info(c, "testTraceFileNameStdoutServerXmlWithTimedRollover", "=== TEST 2 ===");

        setBootstrapLoggingOverrides(null);

        server.startServer();
        server.waitForStringInLog("CWWKF0011I");

        // Wait for config update in console.log
        RemoteFile consoleLog = server.getConsoleLogFile();
        server.setMarkToEndOfLog(consoleLog);
        server.setServerConfigurationFile("server_trace_stdout.xml");

        assertNotNull("Config update should complete (console)",
                      server.waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I", 40000L, consoleLog));

        // No trace.log should be created
        assertFalse("trace.log should NOT be created when traceFileName=stdout",
                    server.fileExistsInLibertyServerRoot("logs/trace.log"));

        // Verify no trace_* rollover happens on next boundary
        waitForBeginningOfMinute();
        Calendar next = getNextRolloverTime(0, 1);
        long waitMs = next.getTimeInMillis() - System.currentTimeMillis() + 5000;
        if (waitMs > 0)
            Thread.sleep(waitMs);

        assertEquals("trace_* rolled files must be ZERO when writing to stdout",
                     0, waitForRolledFiles("trace", 3000));

        Log.info(c, "testTraceFileNameStdoutServerXmlWithTimedRollover", "=== TEST 2 COMPLETE ===");
    }

    /**
     * Test 3: TimedLogRollover (server.env) + start with traceFileName=stdout in bootstrap.properties,
     * then dynamically update to traceFileName=trace.log in server.xml, and verify trace.log CREATES and ROLLS.
     */
    @Test
    public void testDynamicTraceFileNameStdoutToTraceLog() throws Exception {
        Log.info(c, "testDynamicTraceFileNameStdoutToTraceLog", "=== TEST 3 ===");

        setBootstrapLoggingOverrides("stdout");

        server.startServer();
        server.waitForStringInLog("CWWKF0011I");

        assertFalse("trace.log should not exist initially when traceFileName=stdout",
                    server.fileExistsInLibertyServerRoot("logs/trace.log"));

        // Update to trace.log and confirm via console.log
        RemoteFile consoleLog = server.getConsoleLogFile();
        server.setMarkToEndOfLog(consoleLog);
        Log.info(c, "testDynamicTraceFileNameStdoutToTraceLog", "Updating to server_trace_log.xml...");
        server.setServerConfigurationFile("server_trace_log.xml");

        assertNotNull("Config update should complete (console)",
                      server.waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I", 40000L, consoleLog));

        assertTrue("trace.log should be created after dynamic update",
                   waitForFileExists("logs/trace.log", 15000));

        // And it must roll on the next boundary
        Calendar next = getNextRolloverTime(0, 1);
        long waitMs = next.getTimeInMillis() - System.currentTimeMillis() + 5000;
        if (waitMs > 0)
            Thread.sleep(waitMs);

        assertTrue("trace_* rolled files should exist after update to trace.log",
                   waitForRolledFiles("trace", 5000) > 0);

        Log.info(c, "testDynamicTraceFileNameStdoutToTraceLog", "=== TEST 3: COMPLETE ===");
    }

    /**
     * Test 4: With TimedLogRollover, dynamically update traceFileName to test.log,
     * verify test.log CREATES and ROLLS, and verify trace_* does NOT roll.
     */
    @Test
    public void testDynamicTraceFileNameToCustomFile() throws Exception {
        Log.info(c, "testDynamicTraceFileNameToCustomFile", "=== TEST 4 ===");

        setBootstrapLoggingOverrides(null);

        server.startServer();
        server.waitForStringInLog("CWWKF0011I");

        boolean initialTraceLog = server.fileExistsInLibertyServerRoot("logs/trace.log");
        Log.info(c, "testDynamicTraceFileNameToCustomFile", "Initial trace.log exists: " + initialTraceLog);

        // Update to test.log and confirm via console.log
        RemoteFile consoleLog = server.getConsoleLogFile();
        server.setMarkToEndOfLog(consoleLog);
        Log.info(c, "testDynamicTraceFileNameToCustomFile", "Updating to server_test_log.xml...");
        server.setServerConfigurationFile("server_test_log.xml");

        assertNotNull("Config update should complete (console)",
                      server.waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I", 40000L, consoleLog));

        assertTrue("test.log should be created after dynamic update",
                   waitForFileExists("logs/test.log", 15000));

        // On next boundary: test_* should roll, trace_* must NOT roll
        Calendar next = getNextRolloverTime(0, 1);
        long waitMs = next.getTimeInMillis() - System.currentTimeMillis() + 5000;
        if (waitMs > 0)
            Thread.sleep(waitMs);

        int testRolled = waitForRolledFiles("test", 5000);
        int traceRolled = waitForRolledFiles("trace", 2000);

        assertTrue("test_* rolled files should exist", testRolled > 0);
        assertEquals("trace_* must NOT roll when traceFileName points to test.log", 0, traceRolled);

        Log.info(c, "testDynamicTraceFileNameToCustomFile", "=== TEST 4: COMPLETE ===");
    }

    /**
     * Test 5: Repeat scenario 4 using messageFileName, instead.
     * (Don’t block on CWWKG0017I; just wait for the file to appear.)
     */
    @Test
    public void testDynamicMessageFileNameToCustomFile() throws Exception {
        Log.info(c, "testDynamicMessageFileNameToCustomFile", "=== TEST 5: Requirements Check ===");
        Log.info(c, "testDynamicMessageFileNameToCustomFile", "✓ Requirement: Dynamically update messageFileName to custom_message.log");
        Log.info(c, "testDynamicMessageFileNameToCustomFile", "✓ Requirement: Verify custom_message.log gets rolled over");

        // Let server.xml control (no bootstrap override)
        setBootstrapLoggingOverrides(null);

        server.startServer();
        server.waitForStringInLog("CWWKF0011I");

        server.setMarkToEndOfLog();
        Log.info(c, "testDynamicMessageFileNameToCustomFile", "Updating to server_custom_message.xml...");
        server.setServerConfigurationFile("server_custom_message.xml");

        // Wait for the file to appear instead of waiting on CWWKG0017I
        assertTrue("custom_message.log should appear after config update",
                   waitForFileExists("logs/custom_message.log", 50000));

        // Then wait for the next rollover and verify a rolled file exists
        Calendar nextRollover = getNextRolloverTime(0, 1);
        long waitTime = nextRollover.getTimeInMillis() - System.currentTimeMillis() + 5000;
        if (waitTime > 0)
            Thread.sleep(waitTime);

        int rolled = waitForRolledFiles("custom_message", 5000);
        Log.info(c, "testDynamicMessageFileNameToCustomFile", "custom_message_* rolled count: " + rolled);
        assertTrue("custom_message_* rolled files should exist", rolled > 0);

        Log.info(c, "testDynamicMessageFileNameToCustomFile", "=== TEST 5: COMPLETE ===");
    }

    /**
     * Test 6: Configure traceFileName=stdout in server.xml, verify the trace.log is NOT created.
     * (This scenario depends on issue #31949 - Cannot stop generating trace.log file)
     */
    @Test
    public void testTraceFileNameStdoutNoTraceLogCreated() throws Exception {
        Log.info(c, "testTraceFileNameStdoutNoTraceLogCreated", "=== TEST 6 ===");

        setBootstrapLoggingOverrides(null);

        server.startServer();
        server.waitForStringInLog("CWWKF0011I");

        // Update to stdout and confirm via console.log
        RemoteFile consoleLog = server.getConsoleLogFile();
        server.setMarkToEndOfLog(consoleLog);
        server.setServerConfigurationFile("server_trace_stdout.xml");

        assertNotNull("Config update should complete (console)",
                      server.waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I", 40000L, consoleLog));

        assertFalse("trace.log should NOT be created when traceFileName=stdout",
                    server.fileExistsInLibertyServerRoot("logs/trace.log"));

        assertTrue("messages.log should still be created normally",
                   server.fileExistsInLibertyServerRoot("logs/messages.log"));

        Log.info(c, "testTraceFileNameStdoutNoTraceLogCreated", "=== TEST 6: COMPLETE ===");
    }

    // ---- helpers for timing/rollover ----

    private static void waitForBeginningOfMinute() {
        // Wait for the beginning of the minute
        if (Calendar.getInstance().get(Calendar.SECOND) != 0) {
            try {
                Thread.sleep((60000 - Calendar.getInstance().get(Calendar.SECOND) * 1000));
                Thread.sleep(2000); // padding
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    private static Calendar getNextRolloverTime(int rolloverStartHour, int rolloverInterval) {
        // Set calendar start time
        Calendar sched = Calendar.getInstance();
        sched.set(Calendar.HOUR_OF_DAY, rolloverStartHour);
        sched.set(Calendar.MINUTE, 0);
        sched.set(Calendar.SECOND, 0);
        sched.set(Calendar.MILLISECOND, 0);

        Calendar currCal = Calendar.getInstance();

        if (currCal.before(sched)) {
            while (currCal.before(sched)) {
                sched.add(Calendar.MINUTE, rolloverInterval * (-1));
            }
            sched.add(Calendar.MINUTE, rolloverInterval);
        } else if (currCal.after(sched)) {
            while (currCal.after(sched)) {
                sched.add(Calendar.MINUTE, rolloverInterval);
            }
        } else if (currCal.equals(sched)) {
            sched.add(Calendar.MINUTE, rolloverInterval);
        }
        return sched;
    }
}
