/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.event.logging;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class FATTest {
    private static final String SERVER_NAME = "EventLoggingServer";
    private static final Class<?> logClass = FATTest.class;
    private static final String TRACE_LOG = "logs/trace.log";
    private static final String MESSAGES_LOG = "logs/messages.log";
    private static final String APP_NAME = "jdbcTestPrj_1";
    private static final Class<?> c = FATTest.class;
    private static URL url;

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(c, "EventLogging - setUp", "------> Starting server..");
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "com.ibm.ws.request.timing");
        server.startServer();
        Log.info(c, "EventLogging - setUp", "------> Stared server..");
        url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_1/");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLogging() throws Exception {
        server.setServerConfigurationFile("server_eventLogging_original.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        server.setMarkToEndOfLog();
        Log.info(c, "testEventLogging", "------> Default log file path : " + server.getDefaultLogFile().getAbsolutePath());

        // Access the application ...

        Log.info(c, "testEventLogging", "Calling jdbcTestPrj_1 Application with URL = " + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLogging", " Output of br for jdbcTestPrj_1 servlet : " + br.readLine());

        server.waitForStringInLogUsingMark("END", 30000);
        // Checking if the event logging messages are present or not

        List<String> lines = server.findStringsInLogsAndTraceUsingMark("END requestID=");

        Log.info(c, "testEventLogging", "After findStrings..");

        assertTrue("EventLog Message did not appear ", (lines.size() > 0));

        for (String line : lines) {
            Log.info(c, "testEventLogging", "------> END Line  : " + line);
            if ((!(line.contains("contextInfo="))) && (!(line.contains("TRAS3100I")))) {
                fail("Pattern missing..");
            }
        }
        Log.info(c, "testEventLoggingMinDuration", "******** Event Logging Enabled! *********");
        Log.exiting(logClass, name.getMethodName());
    }

    @Mode(TestMode.LITE)
    @Test
    public void testEventLoggingWithNoPattern() throws Exception {
        server.setServerConfigurationFile("server_NoContextInfo.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingWithNoPattern", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingWithNoPattern", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END* | eventType=websphere.servlet.service | duration=*",
                                                                         TRACE_LOG);
        server.setMarkToEndOfLog();

        for (String line : lines) {
            Log.info(c, "testEventLoggingWithNoPattern", "--------> END line : " + line);

            if (line.contains("contextInfo=")) {
                fail("Pattern Found..");
            }
        }

        Log.info(c, "testEventLoggingWithNoPattern", "******** includePatterns works as expected! ********");

    }

    @Mode(TestMode.LITE)
    @Test
    public void testEventLoggingMinDuration() throws Exception {
        server.setServerConfigurationFile("server_minDuration.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingMaxDuration", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingMinDuration", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            Log.info(c, "testEventLoggingMinDuration", "------> END line : " + line);

            int duration = fetchDuration(line);
            assertTrue("Records are found with duration smaller than minDuration.", (duration > 11500));

        }

        Log.info(c, "testEventLoggingMinDuration", "******** minDuration works as expected! ********");
    }

    private int fetchDuration(String line) {
        String dur = "";
        String durWithMS = "";
        if (line.contains("duration")) {
            int startIndex = line.indexOf("duration") + "duration=".length();
            durWithMS = line.substring(startIndex);
            dur = durWithMS.substring(0, durWithMS.indexOf("ms"));
            if (dur.contains(".")) {
                dur = dur.substring(0, dur.indexOf("."));
            }
        }
        return new Integer(dur).intValue();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingLogModeExit() throws Exception {
        server.setServerConfigurationFile("server_logModeExit.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingLogModeExit", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeExit", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END* | eventType=websphere.servlet.service", TRACE_LOG);

        server.setMarkToEndOfLog();

        assertTrue("END records not found for logMode Exit!", (lines.size() > 0));
        for (String line : lines) {
            Log.info(c, "testEventLoggingLogModeExit", "-------->   END line : " + line);
        }

        lines = server.findStringsInFileInLibertyServerRoot(" BEGIN ", TRACE_LOG);

        assertTrue("BEGIN records found for logMode Exit!", (lines.size() == 0));
        Log.info(c, "testEventLoggingLogModeExit", "******** logMode Exit works as expected! ********");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingLogModeEntry() throws Exception {

        server.setServerConfigurationFile("server_logModeEntry.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingLogModeEntry", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeEntry", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("BEGIN", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot(" END ", TRACE_LOG);
        server.setMarkToEndOfLog();

        assertTrue("END records found when logMode is Entry!", (lines.size() == 0));

        lines = server.findStringsInFileInLibertyServerRoot("BEGIN* | eventType=websphere.servlet.service", TRACE_LOG);
        assertTrue("BEGIN records NOT found when logMode is Entry!", (lines.size() > 0));

        for (String line : lines) {
            Log.info(c, "testEventLoggingLogModeEntry", "--------> BEGIN  line : " + line);
        }
        Log.info(c, "testEventLoggingLogModeEntry", " ********* logMode Entry works as expected! ********");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testEventLoggingLogModeEntryExit() throws Exception {
        server.setServerConfigurationFile("server_logModeEntryExit.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingLogModeEntryExit", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeEntryExit", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END* | eventType=websphere.servlet.service",
                                                                         TRACE_LOG);

        server.setMarkToEndOfLog();

        assertTrue("END records not found for logMode EntryExit!", (lines.size() > 0));
        for (String line : lines) {
            Log.info(c, "testEventLoggingLogModeEntry", "-------> END line : " + line);
        }

        lines = server.findStringsInFileInLibertyServerRoot("BEGIN* | eventType=websphere.servlet.service",
                                                            TRACE_LOG);

        assertTrue("BEGIN records not found for logMode EntryExit!", (lines.size() > 0));
        for (String line : lines) {
            Log.info(c, "testEventLoggingLogModeEntryExit", "-------> BEGIN  line : " + line);
        }

        Log.info(c, "testEventLoggingLogModeEntryExit", " ********* logMode EntryExit works as expected! ********");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingEventTypes() throws Exception {
        server.setServerConfigurationFile("server_eventTypes.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingEventTypes", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingEventTypes", " Output of br for jdbcTestPrj_1 servlet : " + br.readLine());
        server.waitForStringInLog("END", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        server.setMarkToEndOfLog();

        String types[] = new String[] { "eventType=websphere.datasource.executeUpdate", "eventType=websphere.datasource.execute", "eventType=websphere.servlet.service" };
        int i = 0;
        for (String line : lines) {
            Log.info(c, "testEventLoggingEventTypes", "----------> END : " + line);
            if (line.contains(types[i])) {
                Log.info(c, "testEventLoggingEventTypes", "------ > Found event type: " + types[i]);
                i++;
            }
            if (i == 3) {
                break;
            }
        }
        assertTrue("Could not find the servlet or JDBC event type.", (i == 3));
        Log.info(c, "testEventLoggingEventTypes", "--------> Included Servlet and JDBC event types.. : " + i);

        Log.info(c, "testEventLoggingEventTypes", " ********* EventTypes works as expected! ********");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testEventLoggingDynamicUpdate() throws Exception {
        server.setServerConfigurationFile("server_updateAll.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingDynamicUpdate", "Calling jdbcTestPrj_1 Application with URL = " + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingDynamicUpdate", " Output of br for jdbcTestPrj_1 servlet : " + br.readLine());
        server.waitForStringInLog("BEGIN", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot(" END ", MESSAGES_LOG);

        assertTrue("END enteries found when logMode is entry.", (lines.size() == 0));

        lines = server.findStringsInFileInLibertyServerRoot("BEGIN", MESSAGES_LOG);

        server.setMarkToEndOfLog();

        int begin = lines.size();
        assertTrue("BEGIN records not found for entry logMode", (begin > 0));

        Log.info(c, "testEventLoggingDynamicUpdate", "--------> logMode is Entry.. ");

        String record = lines.get(0);
        if (record.contains("contextInfo=")) {
            fail("Pattern Found..");
        }

        Log.info(c, "testEventLoggingDynamicUpdate", "--------> Patten is disabled.");

        String types[] = new String[] { "eventType=websphere.datasource.executeUpdate", "eventType=websphere.datasource.execute", "eventType=websphere.servlet.service" };

        for (String line : lines) {
            for (int i = 0; i < 3; i++) {
                if (line.contains(types[i])) {
                    Log.info(c, "testEventLoggingDynamicUpdate", "Found : " + types[i]);
                    types[i] = "1";
                }
            }
        }
        int countTypes = 0;
        for (String type : types) {
            if (type.equals("1")) {
                countTypes++;
            }
        }

        assertTrue("Could not find the servlet or JDBC event type.", (countTypes == 3));
        Log.info(c, "testEventLoggingEventTypes", "--------> Included Servlet and JDBC event types..");

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("server_updateAllsample.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        for (int count = 0; count < 5; count++) {
            con = getHttpConnection(url);
            br = getConnectionStream(con);
            Log.info(c, "testEventLoggingDynamicUpdate", count + " : Output of br for jdbcTestPrj_1 servlet : " + br.readLine());
        }

        lines = server.findStringsInFileInLibertyServerRoot("com.ibm.ws.request.timing.TestJDBC", MESSAGES_LOG);
        for (int index = 1; index < lines.size(); index++) {
            Log.info(c, "testEventLoggingDynamicUpdate", ">>>>>> LINE : " + lines.get(index));
        }
        assertTrue("sampleRate is not sampling as expected.", ((lines.size() - 1) == 3));

        Log.info(c, "testEventLoggingEventTypes", " ********* Dynamic update works as expected! ********");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingRemoveElement() throws Exception {
        server.setServerConfigurationFile("server_removeElement.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingRemoveElement", "Calling jdbcTestPrj_1 Application with URL = " + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingRemoveElement", " Output of br for jdbcTestPrj_1 servlet : " + br.readLine());
        server.waitForStringInLog("BEGIN", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("BEGIN", MESSAGES_LOG);

        int begin = lines.size();
        assertTrue("BEGIN records not found for entry logMode", (begin > 0));
        Log.info(c, "testEventLoggingRemoveElement", "--------> logMode is Entry.. ");
        server.setMarkToEndOfLog();

        String record = lines.get(0);
        if (record.contains("contextInfo=")) {
            fail("Pattern Found..");
        }
        Log.info(c, "testEventLoggingRemoveElement", "--------> Patten is disabled.");

        server.setServerConfigurationFile("server_eventLogging_original.xml");
        Log.info(c, "testEventLoggingRemoveElement", "--------> Removed EventLogging element");

        server.waitForStringInLog("CWWKG0017I", 90000);

        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingRemoveElement", " Output of br for jdbcTestPrj_1 servlet : " + br.readLine());
        server.waitForStringInLog("BEGIN", 30000);

        lines = server.findStringsInFileInLibertyServerRoot("BEGIN", MESSAGES_LOG);
        begin = lines.size() - begin;

        server.setMarkToEndOfLog();

        lines = server.findStringsInFileInLibertyServerRoot("END", MESSAGES_LOG);

        assertTrue("Can still find BEGIN records, logMode functionality is at risk!", (begin == 0));
        assertTrue("No END records found, logMode functionality is at risk!", (lines.size() > 0));

        Log.info(c, "testEventLoggingRemoveElement", "--------> logMode is exit as default eventLogging feature");

        for (String line : lines) {
            if (!(line.contains("contextInfo="))) {
                fail(" **** Pattern NOT Found.. ****");
            }
        }

        Log.info(c, "testEventLoggingRemoveElement", "--------> Pattern enabled as default eventLogging feature");

        Log.info(c, "testEventLoggingRemoveElement", " ********* Removing loganalysis element works as expected! *****");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingRemoveFeature() throws Exception {
        server.setServerConfigurationFile("server_eventLogging_original.xml");

        Log.info(c, "testEventLoggingRemoveFeature", "Calling jdbcTestPrj_1 Application with URL = " + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingRemoveFeature", " Output of br for jdbcTestPrj_1 servlet : " + br.readLine());

        // Waiting for server configuration to complete message
        server.waitForStringInLog("CWWKG0017I", 90000);

        // Wait for the END Event to occur
        String endLine = server.waitForStringInLog("END", 90000);
        Log.info(c, "testEventLoggingRemoveFeature", " Waiting for END line : " + endLine);

        if (endLine == null) {
            Log.info(c, "testEventLoggingRemoveFeature", "Calling jdbcTestPrj_1 Application with URL again = " + url.toString());
            con = getHttpConnection(url);
            br = getConnectionStream(con);
            Log.info(c, "testEventLoggingRemoveFeature", " Output of br for jdbcTestPrj_1 servlet again : " + br.readLine());

            // Wait for the END Event to occur again
            server.waitForStringInLog("END", 90000);
        }

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", MESSAGES_LOG);

        int end = lines.size();
        assertTrue("EventLogging feature is not enabled.. ", (lines.size() > 0));
        Log.info(c, "testEventLoggingRemoveFeature", "-----> Found eventLogging records..");

        server.setServerConfigurationFile("server_NOeventLogging.xml");
        server.setMarkToEndOfLog();

        server.waitForStringInLog("CWWKT0016I", 90000);

        Log.info(c, "testEventLoggingRemoveFeature", " *********** Removed eventLogging feature.. ***********");
        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingRemoveFeature", " Output of br for jdbcTestPrj_1 servlet : " + br.readLine());

        lines = server.findStringsInFileInLibertyServerRoot("END", MESSAGES_LOG);
        for (int i = end; i < lines.size(); i++) {
            String line = lines.get(i);
            Log.info(c, "testEventLoggingRemoveFeature", " ------> line : " + line);
        }

        assertTrue("eventLogging not disabled properly, Can see END records still..", ((lines.size() - end) == 0));
        Log.info(c, "testEventLoggingRemoveFeature", " ****** Removing eventLogging feature works as expected ******");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingAddFeature() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("server_NOeventLogging.xml");

        Log.info(c, "testEventLoggingAddFeature", "Calling jdbcTestPrj_1 Application with URL = " + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingAddFeature", " Output of br for jdbcTestPrj_1 servlet : " + br.readLine());

        List<String> lines = server.findStringsInLogsAndTraceUsingMark(" END ");
        assertTrue("EventLogging feature is enabled.. ", (lines.size() == 0));
        Log.info(c, "testEventLoggingAddFeature", "-----> No eventLogging records..");

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("server_eventLogging_original.xml");
        server.waitForStringInLogUsingMark("CWWKF0012I", 90000);

        Log.info(c, "testEventLoggingAddFeature", "Added eventLogging feature..");
        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingAddFeature", " Output of br for jdbcTestPrj_1 servlet : " + br.readLine());
        server.waitForStringInLogUsingMark("END", 30000);

        lines = server.findStringsInLogsAndTraceUsingMark("END");

        assertTrue("EventLogging feature is not enabled.. ", (lines.size() > 0));
        Log.info(c, "testEventLoggingAddFeature", "-----> Found eventLogging records.. END records found: " + lines.size());

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("server_updateAll.xml");
        server.waitForStringInLogUsingMark("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingAddFeature", "Added eventLogging element to update the default attribute values..");

        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingAddFeature", " Output of br for jdbcTestPrj_1 servlet : " + br.readLine());
        server.waitForStringInLogUsingMark("BEGIN", 30000);

        lines = server.findStringsInLogsAndTraceUsingMark("BEGIN");
        assertTrue("BEGIN records not found for entry logMode", (lines.size() > 0));
        Log.info(c, "testEventLoggingAddFeature", "--------> logMode is Entry.. ");

        String record = lines.get(0);
        if (record.contains("contextInfo=")) {
            Log.info(c, "testEventLoggingAddFeature", "--------> record = " + record);
            fail("Pattern Found..");
        }
        Log.info(c, "testEventLoggingAddFeature", "--------> Patten is disabled.");

        Log.info(c, "testEventLoggingLogModeEntry", " ********* Adding eventLogging feature works as expected! ********");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingPatternUpdate() throws Exception {
        server.setServerConfigurationFile("server_NoContextInfo.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingPatternUpdate", "--------> Started server with patternRequired = false");

        Log.info(c, "testEventLoggingPatternUpdate", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingPatternUpdate", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END* | eventType=websphere.servlet.service | duration=*",
                                                                         MESSAGES_LOG);
        server.setMarkToEndOfLog();

        int previous = lines.size();
        for (String line : lines) {
            Log.info(c, "testEventLoggingPatternUpdate", "1 --------> END line : " + line);
            assertFalse("Pattern found!", (line.contains("contextInfo=")));
        }
        Log.info(c, "testEventLoggingPatternUpdate", " **** Pattern not found **** ");

        server.setServerConfigurationFile("server_TrueContextInfo.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingPatternUpdate", "--------> Updated server configuration :  patternRequired = true");

        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeUpdate", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        lines = server.findStringsInFileInLibertyServerRoot("END* | eventType=websphere.servlet.service | contextInfo=*",
                                                            MESSAGES_LOG);
        for (String line : lines) {
            Log.info(c, "testEventLoggingPatternUpdate", "2 --------> END line : " + line);
        }

        for (int index = previous; index < lines.size(); index++) {
            String line = lines.get(index);
            Log.info(c, "testEventLoggingPatternUpdate", "3 --------> END line : " + line);

            assertTrue("Pattern not found!", (line.contains("contextInfo=")));

        }
        Log.info(c, "testEventLoggingPatternUpdate", " **** Pattern found **** ");

        Log.info(c, "testEventLoggingPatternUpdate", "******** includePatterns dynamic update works as expected! ********");

    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingLogModeUpdate1() throws Exception {
        server.setServerConfigurationFile("server_logModeEntryExit.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingLogModeUpdate1", "--------> Started server with logMode = EntryExit");

        Log.info(c, "testEventLoggingLogModeUpdate1", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeUpdate1", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        server.setMarkToEndOfLog();

        int end = lines.size();
        Log.info(c, "testEventLoggingLogModeUpdate1", "--------> END 1 : " + end);
        assertTrue("END records not found for logMode EntryExit!", (end > 0));

        lines = server.findStringsInFileInLibertyServerRoot("BEGIN", TRACE_LOG);
        int begin = lines.size();
        Log.info(c, "testEventLoggingLogModeUpdate1", "--------> BEGIN 1 : " + begin);
        assertTrue("BEGIN records not found for logMode EntryExit!", (begin > 0));

        Log.info(c, "testEventLoggingLogModeUpdate1", "******** logMode EntryExit has both Entry Exit records.. ********");
        server.setServerConfigurationFile("server_logModeEntry.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeUpdate1", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("BEGIN", 30000);

        Log.info(c, "testEventLoggingLogModeUpdate1", "--------> Updated server configuration :  logMode = Entry");

        lines = server.findStringsInFileInLibertyServerRoot("BEGIN", TRACE_LOG);
        Log.info(c, "testEventLoggingLogModeUpdate1", "--------> BEGIN 2 : " + (lines.size() - begin));
        assertTrue("BEGIN records NOT found for logMode Entry!", ((lines.size() - begin) > 0));

        lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        Log.info(c, "testEventLoggingLogModeUpdate1", "--------> END 2 : " + (lines.size() - end));
        assertTrue("END records FOUND for logMode Entry!", ((lines.size() - end) == 0));

        Log.info(c, "testEventLoggingLogModeUpdate1", " ******** logMode dynamic update  (EntryExit -> Entry) works as expected! ********");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingLogModeUpdate3() throws Exception {
        server.setServerConfigurationFile("server_logModeEntryExit.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingLogModeUpdate3", "--------> Started server with logMode = EntryExit");

        Log.info(c, "testEventLoggingLogModeUpdate3", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeUpdate3", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        int end = lines.size();
        Log.info(c, "testEventLoggingLogModeUpdate3", "--------> END 1 : " + end);
        assertTrue("END records not found for logMode EntryExit!", (end > 0));

        lines = server.findStringsInFileInLibertyServerRoot("BEGIN", TRACE_LOG);

        server.setMarkToEndOfLog();

        int begin = lines.size();
        Log.info(c, "testEventLoggingLogModeUpdate3", "--------> BEGIN 1 : " + begin);
        assertTrue("BEGIN records not found for logMode EntryExit!", (begin > 0));

        Log.info(c, "testEventLoggingLogModeUpdate3", "******** logMode EntryExit has both Entry Exit records.. ********");

        server.setServerConfigurationFile("server_logModeExit.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeUpdate3", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);
        Log.info(c, "testEventLoggingLogModeUpdate3", "--------> Updated server configuration :  logMode = Exit");

        lines = server.findStringsInFileInLibertyServerRoot("BEGIN", TRACE_LOG);
        Log.info(c, "testEventLoggingLogModeUpdate3", "--------> BEGIN 2 : " + (lines.size() - begin));
        assertTrue("BEGIN records found for logMode Exit!", ((lines.size() - begin) == 0));

        lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        Log.info(c, "testEventLoggingLogModeUpdate3", "--------> END 2 : " + (lines.size() - end));
        assertTrue("END records FOUND for logMode Exit!", ((lines.size() - end) != 0));

        Log.info(c, "testEventLoggingLogModeUpdate3", "******** logMode dynamic update (EntryExit -> Exit) works as expected! ********");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingLogModeUpdate2() throws Exception {
        server.setServerConfigurationFile("server_logModeExit.xml");
        Log.info(c, "testEventLoggingLogModeUpdate2", "--------> Started server with logMode = Exit");

        Log.info(c, "testEventLoggingLogModeUpdate2", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeUpdate2", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        int end = lines.size();
        Log.info(c, "testEventLoggingLogModeUpdate2", "--------> END 1 : " + end);
        assertTrue("END records not found for logMode Exit!", (end > 0));

        lines = server.findStringsInLogsAndTraceUsingMark("BEGIN");
        int begin = lines.size();
        Log.info(c, "testEventLoggingLogModeUpdate2", "--------> BEGIN 1 : " + begin);
        assertTrue("BEGIN records FOUND for logMod Exit!", (begin == 0));

        server.setMarkToEndOfLog();

        Log.info(c, "testEventLoggingLogModeUpdate2", " ********* logMode Exit has Exit records ONLY.. *********");

        server.setServerConfigurationFile("server_logModeEntryExit.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);
        Log.info(c, "testEventLoggingLogModeUpdate2", "--------> Updated server configuration :  logMode = EntryExit");

        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeUpdate2", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());

        server.waitForStringInLog("BEGIN", 30000);
        lines = server.findStringsInFileInLibertyServerRoot("BEGIN", TRACE_LOG);
        Log.info(c, "testEventLoggingLogModeUpdate2", "--------> BEGIN 2 : " + (lines.size() - begin));
        assertTrue("BEGIN records NOT found for logMode EntryExit!", ((lines.size() - begin) > 0));

        server.waitForStringInLog("END", 30000);
        lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        Log.info(c, "testEventLoggingLogModeUpdate", "--------> END 2 : " + (lines.size() - end));
        assertTrue("END records NOT found for logMode EntryExit!", ((lines.size() - end) > 0));

        Log.info(c, "testEventLoggingLogModeUpdate", " ********* logMode dynamic update ( Exit -> EntryExit ) works as expected!  ********* ");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingMinDurationUpdate() throws Exception {
        server.setServerConfigurationFile("server_minDuration.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingMinDurationUpdate", "--------> Started server with minDuration = 11500ms");

        Log.info(c, "testEventLoggingMinDurationUpdate", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingMinDurationUpdate", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        int end = lines.size();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            Log.info(c, "testEventLoggingMinDurationUpdate", "------> END line : " + line);
            int duration = fetchDuration(line);
            assertTrue("Records are found with duration smaller than minDuration.", (duration > 11500));

            Log.info(c, "testEventLoggingMinDurationUpdate", "------> Duration : " + duration + "ms  > 11500ms");
        }

        server.setServerConfigurationFile("server_minDuration2.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingMinDurationUpdate", "--------> Updated server configuration :  minDuration = 6000ms");

        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingMinDurationUpdate", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        Log.info(c, "testEventLoggingMinDurationUpdate", "------> size  : " + (lines.size() - end));

        if (!((lines.size() - end) == 0)) {
            for (int i = end; i < lines.size(); i++) {
                String line = lines.get(i);
                Log.info(c, "testEventLoggingMinDurationUpdate", "------> END line : " + line);
                int duration = fetchDuration(line);
                assertTrue("Records are found with duration smaller than minDuration.", (duration > 6000));

                Log.info(c, "testEventLoggingMinDurationUpdate", "------> Duration : " + duration + "ms  > 6000ms");
            }
        }
        Log.info(c, "testEventLoggingMinDurationUpdate", "******** minDuration dynamic update works as expected! ********");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingEventTypesUpdate() throws Exception {
        server.setServerConfigurationFile("server_eventTypes2.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingEventTypesUpdate", "-----> Started server with eventType : service and execute");

        Log.info(c, "testEventLoggingEventTypesUpdate", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingEventTypesUpdate", " Output of br for jdbcTestPrj_1 servlet : " + br.readLine());
        server.waitForStringInLog("END", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        server.setMarkToEndOfLog();

        int previous = lines.size();
        String types[] = new String[] { "eventType=websphere.datasource.execute", "eventType=websphere.servlet.service" };

        for (String line : lines) {
            for (int i = 0; i < 2; i++) {
                if (line.contains(types[i])) {
                    Log.info(c, "testEventLoggingDynamicUpdate", "Found : " + types[i]);
                    types[i] = "1";
                }
            }
        }
        int countTypes = 0;
        for (String type : types) {
            if (type.equals("1")) {
                countTypes++;
            }
        }

        assertTrue("Could not find the servlet or JDBC event type.", (countTypes == 2));

        server.setServerConfigurationFile("server_eventTypes.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingEventTypesUpdate", "--------> Updated server configuration :  eventType");

        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingEventTypesUpdate", " Output of br for jdbcTestPrj_1 servlet : " + br.readLine());
        server.waitForStringInLog("END", 30000);

        lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);

        Log.info(c, "testEventLoggingEventTypesUpdate", "------> size : " + (lines.size() - previous));

        for (int index = previous; index < lines.size(); index++) {
            String line = lines.get(index);
            for (int i = 0; i < 2; i++) {
                if (line.contains(types[i])) {
                    Log.info(c, "testEventLoggingDynamicUpdate", "Found : " + types[i]);
                    types[i] = "1";
                }
            }
        }
        countTypes = 0;
        for (String type : types) {
            if (type.equals("1")) {
                countTypes++;
            }
        }

        assertTrue("Could not find the servlet or JDBC event type.", (countTypes == 2));

        Log.info(c, "testEventLoggingEventTypesUpdate", "--------> Included Servlet and JDBC event types.. : " + countTypes);

        Log.info(c, "testEventLoggingEventTypesUpdate", " ********* EventTypes dynamic update works as expected! ********");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testEventLoggingPatternRemove() throws Exception {
        server.setServerConfigurationFile("server_NoContextInfo.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingPatternRemove", "--------> Started server with patternRequired = false");

        Log.info(c, "testEventLoggingPatternRemove", "--------> Waiting for jdbcTestPrj_1 Application to become available");
        server.waitForStringInLog("CWWKT0016I", 90000);

        Log.info(c, "testEventLoggingPatternRemove", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingPatternRemove", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        server.setMarkToEndOfLog();

        int previous = lines.size();
        for (String line : lines) {
            Log.info(c, "testEventLoggingPatternRemove", "--------> END line : " + line);
            assertFalse("Pattern Found!", (line.contains("contextInfo=")));
        }
        Log.info(c, "testEventLoggingPatternRemove", " **** Pattern NOT found **** ");

        server.setServerConfigurationFile("server_eventLogging_original.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingPatternUpdate", " $$$$$$ ---->  Removed eventLogging element... <---- $$$$$$ ");

        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeUpdate", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);

        for (int index = previous; index < lines.size(); index++) {
            String line = lines.get(index);
            Log.info(c, "testEventLoggingPatternUpdate", "--------> END line : " + line);

            assertTrue("Pattern not found!", (line.contains("contextInfo=")));

        }
        Log.info(c, "testEventLoggingPatternUpdate", " **** Pattern found **** ");

        Log.info(c, "testEventLoggingPatternUpdate", "******** Dynamically Removing eventLogging element works for includePatterns! ********");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingLogModeRemove() throws Exception {
        server.setServerConfigurationFile("server_logModeEntry.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingLogModeRemove", "--------> Started server with logMode = Entry");

        Log.info(c, "testEventLoggingLogModeRemove", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeRemove", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        server.setMarkToEndOfLog();

        int end = lines.size();
        Log.info(c, "testEventLoggingLogModeRemove", "--------> END 1: " + end);
        assertTrue("END records found for logMode Enrty!", (end == 0));

        lines = server.findStringsInFileInLibertyServerRoot("BEGIN", TRACE_LOG);
        int begin = lines.size();
        Log.info(c, "testEventLoggingLogModeRemove", "--------> BEGIN  1 : " + begin);
        assertTrue("BEGIN records not FOUND for logMod Exit!", (begin > 0));

        Log.info(c, "testEventLoggingLogModeRemove", " ********* logMode Entry has BEGIN records ONLY.. *********");

        server.setServerConfigurationFile("server_eventLogging_original.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingLogModeRemove", " $$$$$$ ---->  Removed eventLogging element... <---- $$$$$$ ");

        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeRemove", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        lines = server.findStringsInFileInLibertyServerRoot("BEGIN", TRACE_LOG);
        Log.info(c, "testEventLoggingLogModeRemove", "--------> BEGIN 2 : " + (lines.size() - begin));
        assertTrue("BEGIN records Found for default logMode!", ((lines.size() - begin) == 0));

        lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        Log.info(c, "testEventLoggingLogModeRemove", "--------> END 2 : " + (lines.size() - end));
        assertTrue("END records NOT found for logMode EntryExit!", ((lines.size() - end) > 0));

        Log.info(c, "testEventLoggingLogModeRemove", " ********* Dynamically Removing eventLogging element works for logMode!  ********* ");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingLogModeRemove2() throws Exception {
        server.setServerConfigurationFile("server_logModeEntryExit.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingLogModeRemove2", "--------> Started server with logMode = EntryExit");

        Log.info(c, "testEventLoggingLogModeRemove2", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeRemove2", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        server.setMarkToEndOfLog();

        int end = lines.size();
        Log.info(c, "testEventLoggingLogModeRemove2", "--------> END 1: " + end);
        assertTrue("END records NOT found for logMode EntryExit!", (end > 0));

        lines = server.findStringsInFileInLibertyServerRoot("BEGIN", TRACE_LOG);
        int begin = lines.size();
        Log.info(c, "testEventLoggingLogModeRemove2", "--------> BEGIN  1 : " + begin);
        assertTrue("BEGIN records not FOUND for logMod EntryExit!", (begin > 0));

        Log.info(c, "testEventLoggingLogModeRemove2", " ********* logMode EntryExit has both BEGIN and END records.. *********");

        server.setServerConfigurationFile("server_eventLogging_original.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingLogModeRemove2", " $$$$$$ ---->  Removed eventLogging element... <---- $$$$$$ ");

        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingLogModeRemove2", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        lines = server.findStringsInFileInLibertyServerRoot("BEGIN", TRACE_LOG);
        Log.info(c, "testEventLoggingLogModeRemove2", "--------> BEGIN 2 : " + (lines.size() - begin));
        assertTrue("BEGIN records Found for default logMode!", ((lines.size() - begin) == 0));

        lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        Log.info(c, "testEventLoggingLogModeRemove2", "--------> END 2 : " + (lines.size() - end));
        assertTrue("END records NOT found for logMode EntryExit!", ((lines.size() - end) > 0));

        Log.info(c, "testEventLoggingLogModeRemove2", " ********* Dynamically Removing eventLogging element works for logMode - EntryExit!  ********* ");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingMinDurationRemove() throws Exception {
        server.setServerConfigurationFile("server_minDuration3.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingMinDurationRemove", "--------> Started server with minDuration = 25000ms");

        Log.info(c, "testEventLoggingMinDurationRemove", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingMinDurationRemove", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        server.setMarkToEndOfLog();

        for (String line : lines) {
            Log.info(c, "testEventLoggingMinDurationRemove", "with minDuration = 25000ms ------> END line : " + line);
            int duration = fetchDuration(line);

            Log.info(c, "testEventLoggingMinDurationRemove", "with minDuration = 25000ms ------> Duration : " + duration + "ms ");
        }

        int end = lines.size();
        Log.info(c, "testEventLoggingMinDurationRemove", "------> END size : " + end);
        assertTrue("Record was not expected for 25000ms minDuration. : ", (end == 0));

        server.setServerConfigurationFile("server_eventLogging_original.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingMinDurationRemove", " $$$$$$ ---->  Removed eventLogging element... <---- $$$$$$ ");

        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingMinDurationRemove", " Output of br for jdbcTestPrj_1 servlet" + br.readLine());
        server.waitForStringInLog("END", 30000);

        lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        Log.info(c, "testEventLoggingMinDurationRemove", "------> END size  : " + lines.size());
        assertTrue("Records not Found for default minDuration!", (lines.size() > 0));

        for (String line : lines) {
            Log.info(c, "testEventLoggingMinDurationRemove", "with default minDuration = 1s ------> END line : " + line);
            int duration = fetchDuration(line);

            Log.info(c, "testEventLoggingMinDurationRemove", "with default minDuration = 1s ------> Duration : " + duration + "ms ");
        }

        Log.info(c, "testEventLoggingMinDurationRemove", "******** Dynamically Removing eventLogging element works for minDuration! ********");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingEventTypesRemove() throws Exception {
        server.setServerConfigurationFile("server_eventTypes.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingEventTypesRemove", "-----> Started server with eventType : servlet and JDBC");

        Log.info(c, "testEventLoggingEventTypesRemove", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingEventTypesRemove", " Output of br for jdbcTestPrj_1 servlet : " + br.readLine());
        server.waitForStringInLog("END", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        server.setMarkToEndOfLog();

        int previous = lines.size();
        String types[] = new String[] { "eventType=websphere.servlet.service", "eventType=websphere.datasource.execute", "eventType=websphere.datasource.executeUpdate" };

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            for (int i = 0; i < 3; i++) {
                if (line.contains(types[i])) {
                    Log.info(c, "testEventLoggingDynamicUpdate", "Found : " + types[i]);
                    types[i] = "1";
                }
            }
        }
        int countTypes = 0;
        for (String type : types) {
            if (type.equals("1")) {
                countTypes++;
            }
        }

        assertTrue("Could not find the servlet or JDBC event type.", (countTypes == 3));

        server.setServerConfigurationFile("server_eventLogging_original.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingMinDurationRemove", " $$$$$$ ---->  Removed eventLogging element... <---- $$$$$$ ");

        con = getHttpConnection(url);
        br = getConnectionStream(con);
        Log.info(c, "testEventLoggingEventTypesRemove", " Output of br for jdbcTestPrj_1 servlet : " + br.readLine());
        server.waitForStringInLog("END", 30000);

        lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);

        Log.info(c, "testEventLoggingEventTypesRemove", "------> size : " + (lines.size() - previous));

        int count = 0;
        for (int index = previous; index < lines.size(); index++) {
            String line = lines.get(index);
            Log.info(c, "testEventLoggingEventTypesRemove", "------> line : " + line);
            if (line.contains("eventType=websphere.servlet.service")) {
                count++;
            }
        }
        assertTrue("Found JDBC event type for default setting", (count == (lines.size() - previous)));

        Log.info(c, "testEventLoggingEventevent typesRemove", "--------> Included Servlet event type ONLY as default.. : " + count);

        Log.info(c, "testEventLoggingEventTypesRemove", " ********* Dynamically Removing eventLogging element works for EventTypes! ********");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testEventLoggingSampleRate() throws Exception {
        server.setServerConfigurationFile("server_sampleRate.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingSampleRate", "--------> Started Server with Sample rate 2");

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_1/");
        Log.info(c, "testEventLoggingSampleRate", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        int count = 1;
        List<String> lines;
        HttpURLConnection con;
        BufferedReader br;
        int endRecords = 0;

        while (count != 4) {
            con = getHttpConnection(url);
            br = getConnectionStream(con);
            Log.info(c, "testEventLoggingEvenSampleRate_IDs", count + " : Output of br for jdbcTestPrj_1 servlet" + br.readLine());
            if (count % 2 == 0) {
                lines = server.findStringsInFileInLibertyServerRoot("END requestID", TRACE_LOG);
                for (String line : lines) {
                    Log.info(c, "testEventLoggingSampleRate", "--------> END line : [" + line + "]");
                    if (!line.contains("AB")) {
                        fail("No event logging record logged for Sample rate : 2");
                    }
                }
            }
            count++;
        }
        lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        for (String line : lines) {
            Log.info(c, "testEventLoggingSampleRate", "------> line : " + line);
            if (line.contains("contextInfo=jdbcTestPrj_1 | com.ibm.ws.request.timing.TestJDBC")) {
                endRecords++;
            }
        }

        Log.info(c, "testEventLoggingSampleRate", "---------> No of event logging records : " + endRecords);

        assertTrue("The expected sample for count : 1, Actual :" + endRecords, (endRecords == 1));

        Log.info(c, "testEventLoggingSampleRate", " ********* SampleRate (Even nos) works as expected! ********");

    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingSampleRateOdd() throws Exception {
        server.setServerConfigurationFile("server_sampleRate3.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingSampleRateOdd", "--------> Started Server with Sample rate 3");

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_1/");
        Log.info(c, "testEventLoggingSampleRateOdd", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        int count = 1;
        List<String> lines;
        HttpURLConnection con;
        BufferedReader br;

        int endRecords = 0;

        while (count != 4) {
            con = getHttpConnection(url);
            br = getConnectionStream(con);
            Log.info(c, "testEventLoggingEvenSampleRate_IDs", count + " : Output of br for jdbcTestPrj_1 servlet" + br.readLine());
            if (count % 3 == 0) {
                lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
                for (String line : lines) {
                    if (!line.contains("AC")) {
                        fail("No event logging record logged for Sample rate : 3");
                    }
                }
            }
            count++;
        }
        lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        for (String line : lines) {
            Log.info(c, "testEventLoggingSampleRate", "------> line : " + line);
            if (line.contains("contextInfo=jdbcTestPrj_1 | com.ibm.ws.request.timing.TestJDBC")) {
                endRecords++;
            }
        }
        Log.info(c, "testEventLoggingSampleRate", count + "---------> Size : " + endRecords);

        assertTrue("The expected sample for count : 1, Actual :" + endRecords, (endRecords == 1));

        Log.info(c, "testEventLoggingSampleRateOdd", " ********* SampleRate (Odd nos) works as expected! ********");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingSampleRateZero() throws Exception {
        server.setServerConfigurationFile("server_sampleRate0.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingSampleRateUpdate", "--------> Started Server with Sample rate 0");

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_1/");
        Log.info(c, "testEventLoggingSampleRateUpdate", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, "testEventLoggingSampleRateUpdate", " : Output of br for jdbcTestPrj_1 servlet" + br.readLine());

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        Log.info(c, "testEventLoggingSampleRateUpdate", "*****  No of Warnings : " + lines.size());
        int warnings = 0;
        for (String line : lines) {
            if ((line.contains("contextInfo=jdbcTestPrj_1")) && line.contains("com.ibm.ws.request.timing.TestJDBC")) {
                warnings++;
            }
        }

        assertTrue("The expected number of entries are 1 for the sampling rate 0, Actual are : " + warnings, (warnings == 1));
        Log.info(c, "testEventLoggingSampleRateUpdate", "******** SampleRate 0 works as expected! ********");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingSampleRateUpdate() throws Exception {
        server.setServerConfigurationFile("server_sampleRate.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingSampleRateUpdate", "--------> Started Server with Sample rate 2");

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_1/");
        Log.info(c, "testEventLoggingSampleRateUpdate", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        int count = 0;
        HttpURLConnection con;
        BufferedReader br;
        while (count != 5) {
            con = getHttpConnection(url);
            br = getConnectionStream(con);
            Log.info(c, "testEventLoggingSampleRateUpdate", count + " : Output of br for jdbcTestPrj_1 servlet" + br.readLine());
            count++;
        }

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        int previous = 0;
        for (String line : lines) {
            Log.info(c, "testEventLoggingSampleRateUpdate", "*****  line : " + line);
            if (line.contains("com.ibm.ws.request.timing.TestJDBC")) {
                previous++;
            }
        }

        assertTrue("The expected number of enteries are 2 for the sampling rate 2, Actual are : " + previous, (previous == 2));

        server.setMarkToEndOfLog();

        server.setServerConfigurationFile("server_sampleRate4.xml");
        Log.info(c, "testEventLoggingSampleRateUpdate", "--------> Updated server configuration :  sampleRate = 4");

        count = 0;
        while (count != 5) {
            con = getHttpConnection(url);
            br = getConnectionStream(con);
            Log.info(c, "testEventLoggingSampleRateUpdate", count + " : Output of br for jdbcTestPrj_1 servlet" + br.readLine());
            count++;
        }
        lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        int current = 0;
        for (String line : lines) {
            Log.info(c, "testEventLoggingSampleRateUpdate", "*****  line : " + line);
            if (line.contains("com.ibm.ws.request.timing.TestJDBC")) {
                current++;
            }
        }
        Log.info(c, "testEventLoggingSampleRateUpdate", "-------> size : " + current + " ---> previous : " + previous);
        assertTrue("The expected number of entry is 1 for the sampling rate 4..", ((current - previous) == 1));

        Log.info(c, "testEventLoggingSampleRateUpdate", "********** SampleRate dynamic update works as expected! ********");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEventLoggingSampleRateRemove() throws Exception {
        server.setServerConfigurationFile("server_sampleRate.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);

        Log.info(c, "testEventLoggingSampleRateRemove", "--------> Started Server with Sample rate 2");

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_1/");
        Log.info(c, "testEventLoggingSampleRateRemove", "Calling jdbcTestPrj_1 Application with URL=" + url.toString());
        int count = 0;
        HttpURLConnection con;
        BufferedReader br;
        while (count != 5) {
            con = getHttpConnection(url);
            br = getConnectionStream(con);
            Log.info(c, "testEventLoggingSampleRateRemove", count + " : Output of br for jdbcTestPrj_1 servlet" + br.readLine());
            count++;
        }
        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        int previous = 0;
        for (String line : lines) {
            Log.info(c, "testEventLoggingSampleRateUpdate", "*****  line : " + line);
            if (line.contains("com.ibm.ws.request.timing.TestJDBC")) {
                previous++;
            }
        }
        Log.info(c, "testEventLoggingMinDurationRemove", "Before Event Logging feature removal -------> size : " + lines.size() + " ---> previous : " + previous);

        assertTrue("The expected number of enteries are 2 for the sampling rate 2, Actual are : " + previous, (previous == 2));

        server.setServerConfigurationFile("server_eventLogging_original.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);
        Log.info(c, "testEventLoggingMinDurationRemove", " $$$$$$ ---->  Removed eventLogging element... <---- $$$$$$ ");

        count = 0;
        while (count != 3) {
            con = getHttpConnection(url);
            br = getConnectionStream(con);
            Log.info(c, "testEventLoggingMinDurationRemove", count + " : Output of br for jdbcTestPrj_1 servlet" + br.readLine());
            count++;
        }
        lines = server.findStringsInFileInLibertyServerRoot("END", TRACE_LOG);
        int current = 0;
        for (String line : lines) {
            Log.info(c, "testEventLoggingSampleRateUpdate", "*****  line : " + line);
            if (line.contains("com.ibm.ws.request.timing.TestJDBC")) {
                current++;
            }
        }
        Log.info(c, "testEventLoggingMinDurationRemove",
                 "After Event Logging feature removal -------> size : " + lines.size() + " ---> current : " + current + " ---> previous : " + previous);
        assertTrue("The expected number of entries are 3 for the default sampling rate..", ((current - previous) == 3));

        Log.info(c, "testEventLoggingSampleRateUpdate", "********** Dynamically Removing eventLogging element works for SampleRate! ********");

    }

    /**
     * This method is used to get a connection stream from an HTTP connection. It
     * gives the output from the webpage that it gets from the connection
     *
     * @param con The connection to the HTTP address
     *
     * @return The Output from the webpage
     */
    private BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        return br;
    }

    /**
     * This method creates a connection to a webpage and then reutrns the connection
     *
     * @param url The Http Address to connect to
     * @return The connection to the http address
     */
    private HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }

    @Before
    public void setup() throws Exception {
        if (server != null && !server.isStarted()) {
            server.startServer();
        }
    }

    @After
    public void tearDown() throws Exception {
        server.setTraceMarkToEndOfDefaultTrace();
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKG0011W", // Configuration validation did not succeed, for value out of range. (sample rate=0)
                              "CWWKG0083W"); // Validation failure did not succeed, for sample rate=0
        }
    }
}