/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.logging.internal_fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(FATRunner.class)
public class TelemetryMessagesTest extends FATServletClient {

    private static Class<?> c = TelemetryMessagesTest.class;

    public static final String APP_NAME = "MpTelemetryLogApp";
    public static final String SERVER_NAME = "TelemetryMessage";

    //This test will run on all mp 2.0 repeats to ensure we have some test coverage on all versions.
    //I chose this one because TelemetryMessages is core to this bucket
    @ClassRule
    public static RepeatTests rt = TelemetryActions.telemetry20Repeats();

    @Server(SERVER_NAME)
    public static LibertyServer server;

    public static final String SERVER_XML_ALL_SOURCES = "allSources.xml";

    @BeforeClass
    public static void initialSetup() throws Exception {
        server.saveServerConfiguration();
    }

    @Before
    public void testSetup() throws Exception {
        setupServerApp(server);
        server.startServer();
    }

    static LibertyServer setupServerApp(LibertyServer s) throws Exception {
        ShrinkHelper.defaultApp(s, APP_NAME, new DeployOptions[] { DeployOptions.SERVER_ONLY }, "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp");
        return s;
    }

    @After
    public void testTearDown() throws Exception {
        server.stopServer();

        // Restore the server configuration, after each test case.
        server.restoreServerConfiguration();
    }

    /**
     * Ensures Liberty messages are correctly bridged and all attributes are present.
     */
    @Test
    public void testTelemetryMessages() throws Exception {
        testTelemetryMessages(server, null);
    }

    static void testTelemetryMessages(LibertyServer s, Consumer<List<String>> consoleConsumer) throws Exception {
        String line = s.waitForStringInLog("CWWKF0011I", s.getConsoleLogFile());

        //Sleep for 5s to ensure messages/console log files are in sync
        TimeUnit.SECONDS.sleep(5);

        RemoteFile consoleLog = s.getConsoleLogFile();
        s.setMarkToEndOfLog(consoleLog);
        RemoteFile messageLog = s.getDefaultLogFile();
        s.setMarkToEndOfLog(messageLog);

        List<String> linesMessagesLog = s.findStringsInLogsUsingMark("^(?!.*scopeInfo).*\\[.*$", messageLog);
        List<String> linesConsoleLog = s.findStringsInLogsUsingMark(".*scopeInfo.*", consoleLog);

        if (consoleConsumer != null) {
            consoleConsumer.accept(linesConsoleLog);
        }

        //Check if number of lines found in messages.log and console.log match.
        boolean logSizeMatches = (linesMessagesLog.size() == linesConsoleLog.size());

        if (!logSizeMatches) {
            if (!linesMessagesLog.isEmpty()) {
                Log.info(c, "testTelemetryMessages", "First messages.log line: " + linesMessagesLog.get(0));
                Log.info(c, "testTelemetryMessages", "Last messages.log line: " + linesMessagesLog.get(linesMessagesLog.size() - 1));
            }

            if (!linesConsoleLog.isEmpty()) {
                Log.info(c, "testTelemetryMessages", "First console.log line: " + linesConsoleLog.get(0));
                Log.info(c, "testTelemetryMessages", "Last console.log line: " + linesConsoleLog.get(linesConsoleLog.size() - 1));
            }
        }

        assertTrue("Messages.log and Telemetry console logs don't match. messageLogSize=" + linesMessagesLog.size() + ", consoleLogSize=" + linesConsoleLog.size(), logSizeMatches);

        Map<String, String> myMap = new HashMap<String, String>() {
            {
                put("io.openliberty.type", "liberty_message");
                put("io.openliberty.message_id", "CWWKF0011I");
                put("io.openliberty.module", "com.ibm.ws.kernel.feature.internal.FeatureManager");
                put("thread.id", "");
                put("thread.name", "");
                put("io.openliberty.sequence", "");
            }
        };

        assertNotNull("CWWKF0011I log could not be found.", line);
        assertTrue("MPTelemetry did not log the correct message", line.contains("The TelemetryMessage server is ready to run a smarter planet."));
        TestUtils.checkJsonMessage(line, myMap);
    }

    /*
     * Ensures application messages are bridged.
     */
    @Test
    public void testTelemetryApplicationMessages() throws Exception {
        TestUtils.runApp(server, "logServlet");

        String line = server.waitForStringInLog("info message", server.getConsoleLogFile());

        Map<String, String> myMap = new HashMap<String, String>() {
            {
                put("io.openliberty.ext.app_name", "MpTelemetryLogApp");
                put("io.openliberty.type", "liberty_message");
                put("io.openliberty.module", "io.openliberty.microprofile.telemetry.logging.internal.fat.MpTelemetryLogApp.MpTelemetryServlet");
                put("thread.id", "");
                put("thread.name", "");
                put("io.openliberty.sequence", "");
            }
        };

        assertNotNull("info message log could not be found.", line);
        assertTrue("MPTelemetry did not log the correct message", line.contains("info message"));
        assertTrue("MPTelemetry did not log the correct log level", line.contains("INFO"));

        TestUtils.checkJsonMessage(line, myMap);

    }

    /*
     * Ensures all log types have the correct log levels.
     */
    @Test
    public void testTelemetryLogLevels() throws Exception {
        RemoteFile consoleLogFile = server.getConsoleLogFile();
        setConfig(SERVER_XML_ALL_SOURCES, consoleLogFile, server);

        TestUtils.runApp(server, "logServlet");

        String infoLine = server.waitForStringInLog("info message", server.getConsoleLogFile());

        assertNotNull("Info message could not be found.", infoLine);
        assertTrue("Incorrect log level was logged.", infoLine.contains("INFO"));

        String severeLine = server.waitForStringInLog("severe message", 5000, server.getConsoleLogFile());
        String warningLine = server.waitForStringInLog("warning message", 5000, server.getConsoleLogFile());
        String sysOutLine = server.waitForStringInLog("^(?=.*System.out.println)(?=.*scopeInfo).*$", 5000, server.getConsoleLogFile());
        String sysErrLine = server.waitForStringInLog("^(?=.*System.err.println)(?=.*scopeInfo).*$", 5000, server.getConsoleLogFile());
        String configTraceLine = server.waitForStringInLog("config trace", 5000, server.getConsoleLogFile());
        String fineLine = server.waitForStringInLog("fine trace", 5000, server.getConsoleLogFile());
        String finerLine = server.waitForStringInLog("finer trace", 5000, server.getConsoleLogFile());
        String finestLine = server.waitForStringInLog("finest trace", 5000, server.getConsoleLogFile());

        assertNotNull("Severe message could not be found.", severeLine);
        assertTrue("Incorrect log level was logged.", severeLine.contains("ERROR"));

        assertNotNull("Warning message could not be found.", warningLine);
        assertTrue("Incorrect log level was logged.", warningLine.contains("WARN"));

        assertNotNull("System.out.println message could not be found.", sysOutLine);
        assertTrue("Incorrect log level was logged.", sysOutLine.contains("INFO"));

        assertNotNull("System.err.println message could not be found.", sysErrLine);
        assertTrue("Incorrect log level was logged.", sysErrLine.contains("WARN"));

        assertNotNull("Config trace message could not be found.", configTraceLine);
        assertTrue("Incorrect log level was logged.", configTraceLine.contains("DEBUG4"));

        assertNotNull("Fine trace message could not be found.", fineLine);
        assertTrue("Incorrect log level was logged.", fineLine.contains("DEBUG2"));

        assertNotNull("Finer trace message could not be found.", finerLine);
        assertTrue("Incorrect log level was logged.", finerLine.contains("DEBUG"));

        assertNotNull("Finest message could not be found.", finestLine);
        assertTrue("Incorrect log level was logged.", finestLine.contains("TRACE"));

    }

    private static String setConfig(String fileName, RemoteFile logFile, LibertyServer server) throws Exception {
        server.setMarkToEndOfLog(logFile);
        server.setServerConfigurationFile(fileName);
        return server.waitForStringInLogUsingMark("CWWKG0017I.*|CWWKG0018I.*");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}