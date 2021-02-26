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
package com.ibm.ws.request.timing.fat;

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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class SlowRequestTiming {
    @Server("RequestTimingServer")
    public static LibertyServer server;
    private static final String MESSAGE_LOG = "logs/messages.log";

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "jdbcTestPrj_3", "com.ibm.ws.request.timing");
        CommonTasks.writeLogMsg(Level.INFO, " starting server..");
        server.startServer();
    }

    @Before
    public void setupTestStart() throws Exception {
        if (server != null && !server.isStarted()) {
            server.startServer();
        }
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("TRAS0112W", "TRAS0113I", "TRAS0114W", "TRAS0115W", "CWWKG0083W");
        }
    }

    private void createRequest(String option) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_3" + option);
        CommonTasks.writeLogMsg(Level.INFO, "Calling jdbcTestPrj_3 Application with URL= " + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        br.readLine();
        CommonTasks.writeLogMsg(Level.INFO, "Output of br for jdbcTestPrj_3 servlet" + br.readLine());
    }

    @Test
    public void testSlowRequestTiming() throws Exception {
        //Set to default Configuration of Request Timing feature
        server.setServerConfigurationFile("server_original.xml");
        server.waitForStringInLogUsingMark("CWWKG0017I", 30000);

        // Access the application ... (This request will be slow ... sleep for 15secs)
        createRequest("?sleepTime=15000");

        // Wait for the slow request warning message in message.log (default) with a timeout of 60 sec
        String slowRequestWarningLine = server.waitForStringInLogUsingMark("TRAS0112W", 60000);

        // Wait for the slow request completion message in message.log (default) with a timeout of 60 sec
        String slowRequestCompletedLine = server.waitForStringInLogUsingMark("TRAS0113I", 60000);

        // The following two variables will determine if the messages were written correctly to message.log or not
        int warningLineExist = (slowRequestWarningLine == null) ? 0 : 1;
        int completedLineExist = (slowRequestCompletedLine == null) ? 0 : 1;

        CommonTasks.writeLogMsg(Level.INFO, " Size : " + warningLineExist);
        assertTrue("No slow request warning found!", warningLineExist == 1);
        CommonTasks.writeLogMsg(Level.INFO, "---->Slow Request Timer Warning : " + slowRequestWarningLine);

        CommonTasks.writeLogMsg(Level.INFO, " Size : " + completedLineExist);
        assertTrue("No slow request completion info message found!", completedLineExist == 1);
        CommonTasks.writeLogMsg(Level.INFO, "---->Slow Request Timer Completion Info Message : " + slowRequestCompletedLine);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("\\.[0-9]+ms", MESSAGE_LOG);
        server.setMarkToEndOfLog();

        String eventTypes[] = { "websphere.servlet.service", "websphere.datasource.executeUpdate", "websphere.session.setAttribute", "websphere.session.getAttribute" };

        int eventTypesCount[] = { 0, 0, 0, 0 };
        String slowRecord = "";
        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, " ---- > Line : " + line);
            String req = fetchSlowRequestExceedingRecords(line);
            if (req != null) {
                slowRecord = req;
                break;
            }
        }

        for (String line : lines) {
            for (int i = 0; i < eventTypes.length; i++) {
                if (line.contains(eventTypes[i])) {
                    eventTypesCount[i] = 1;
                }
            }

        }
        int eventCount = 0;
        for (int value : eventTypesCount) {
            if (value == 1) {
                eventCount++;
            }
        }

        CommonTasks.writeLogMsg(Level.INFO, "----> SlowRequest : " + slowRecord);

        assertTrue("All Event Types are NOT Present", (eventCount == 4));
        CommonTasks.writeLogMsg(Level.INFO, "All Event Types Present : " + (eventCount == 4));

        CommonTasks.writeLogMsg(Level.INFO, "$$$$$$$$ ---> Verified!");

        assertTrue("Root event type not found..", slowRecord.contains("websphere.servlet.service"));
        CommonTasks.writeLogMsg(Level.INFO, "Root event type : " + slowRecord.contains("websphere.servlet.service"));
    }

    @Test
    public void testSlowReqTimingIncludeContextInfo() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> Updating server with configuration : includeContextInfo=false");

        server.setServerConfigurationFile("server_NOContextInfo.xml");

        server.waitForStringInLog("CWWKG0017I", 30000);

        createRequest("?sleepTime=3000");

        server.waitForStringInLog("TRAS0112W");

        List<String> lines = server.findStringsInFileInLibertyServerRoot("ms", MESSAGE_LOG);

        for (String rec : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "------> Line :" + rec);

            if (rec.contains("|")) {
                fail("Found pattern! : " + rec);
            }
        }

        CommonTasks.writeLogMsg(Level.INFO, "***** includeContextInfo works for slow request timing.. *****");
    }

    @Test
    public void testSlowReqMonitorIntervals() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "-------> Setting slowRequestThreshold to 1 second");
        server.setServerConfigurationFile("server_slowRequestThreshold1.xml");

        server.waitForStringInLog("CWWKG0017I", 30000);

        createRequest("");
        server.waitForStringInLog("TRAS0112W", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);

        int records = lines.size();
        CommonTasks.writeLogMsg(Level.INFO, "-------> Slow request timing records : " + records);

        String message = "This request will continue to be monitored, and a further warning will be logged should the request be running after another ";
        lines = server.findStringsInFileInLibertyServerRoot(message, MESSAGE_LOG);
        server.setMarkToEndOfLog();

        int multiple = lines.size();
        CommonTasks.writeLogMsg(Level.INFO, "-------> Timing : " + multiple);

        int expected;
        int multiply = 2;
        boolean reset = false;

        for (String line : lines) {
            String duration = line.substring(message.length()).replace("s.", "").trim();

            int time = new Integer(duration);
            expected = multiply;

            if (time == expected) {
                CommonTasks.writeLogMsg(Level.INFO, " ----> Expected Actual : " + time + " ***** Expected : " + expected);

            } else if (time < expected) {
                reset = true;
                CommonTasks.writeLogMsg(Level.INFO, " ******** > New Request Starts here ********* ");
                CommonTasks.writeLogMsg(Level.INFO, " ----> Expected Actual : " + time + " ***** Expected : 2");

            } else {
                CommonTasks.writeLogMsg(Level.INFO, "******** > UNexpected Actual : " + time + " ***** > Expected : " + expected);
                assertFalse("Slow Request Timer has jumped the multiple!", time > expected);
            }
            if (reset) {
                multiply = 2;
                reset = false;
            }
            multiply = multiply * 2;
        }
    }

    @Test
    public void testSlowReqTimingTurnOff() throws Exception {
        server.setServerConfigurationFile("server_slowRequestThreshold0.xml");
        server.waitForStringInLog("CWWKG0017I");

        createRequest("?sleepTime=11000");

        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);

        List<String> slowRequestCompletedLines = server.findStringsInFileInLibertyServerRoot("TRAS0113I", MESSAGE_LOG);

        server.setMarkToEndOfLog();

        assertTrue("Slow Request Warnings found when slowRequestThreshold is Zero..", (lines.size() == 0));

        assertTrue("Slow Request Completion Information Messages found when slowRequestThreshold is Zero..", (slowRequestCompletedLines.size() == 0));

        CommonTasks.writeLogMsg(Level.INFO, "****** Slow request timing is turned off for 0 slowRequestThreshold ******");
    }

//    public void testSlowReqSampleRateOdd() throws Exception {
//
//        this.logStep("**** >>>>> Updating server with configuration : sampleRate=3");
//        server.setServerConfigurationFile("server_sampleRate3.xml");
//        this.logStepCompleted();
//        server.waitForStringInLog("CWWKG0017I", 30000);
//
//        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_3/");
//        this.logStep("----> Creating 7 requests...");
//
//        HttpURLConnection con;
//        BufferedReader br;
//        int count = 1;
//        List<String> lines;
//        int found = 0;
//        int previous = 0;
//        while (count <= 7) {
//            CommonTasks.writeLogMsg(Level.INFO, count + " -----> Calling jdbcTestPrj_3 Application with URL=" + url.toString());
//            con = getHttpConnection(url);
//            br = getConnectionStream(con);
//            br.readLine();
//            if (count % 3 == 0) {
//
//                server.waitForStringInLog("TRAS0112W", 15000);
//                lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
//                CommonTasks.writeLogMsg(Level.INFO, " Size : " + lines.size());
//                if (lines.size() - previous == 1) {
//                    found++;
//                }
//                else {
//                    fail("No slow request warning found for request no : " + count);
//
//                }
//                previous = lines.size();
//                CommonTasks.writeLogMsg(Level.INFO, "-------> Slow warning : " + lines.get(found - 1));
//            }
//            count++;
//        }
//        this.logStepCompleted();
//
//        lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
//        assertTrue("Found " + lines.size() + " slow warning, but expected was " + found, (lines.size() == found));
//
//        CommonTasks.writeLogMsg(Level.INFO, "******* Slow request timing works for Odd Sample Rate*******");
//
//    }

//    public void testSlowReqSampleRateEven() throws Exception {
//
//        this.logStep("**** >>>>> Updating server with configuration : sampleRate=2");
//        server.setServerConfigurationFile("server_sampleRate2.xml");
//        this.logStepCompleted();
//        server.waitForStringInLog("CWWKG0017I", 30000);
//
//        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_3/");
//        this.logStep("----> Creating 4 requests...");
//
//        HttpURLConnection con;
//        BufferedReader br;
//        int count = 1;
//        List<String> lines;
//        int found = 0;
//        int previous = 0;
//        while (count <= 4) {
//            CommonTasks.writeLogMsg(Level.INFO, count + " -----> Calling jdbcTestPrj_3 Application with URL=" + url.toString());
//            con = getHttpConnection(url);
//            br = getConnectionStream(con);
//            br.readLine();
//            if (count % 2 == 0) {
//
//                server.waitForStringInLog("TRAS0112W", 15000);
//                lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
//                CommonTasks.writeLogMsg(Level.INFO, " Size : " + lines.size());
//                if (lines.size() - previous == 1) {
//                    found++;
//                }
//                else {
//                    fail("No slow request warning found for request no : " + count);
//
//                }
//                previous = lines.size();
//                CommonTasks.writeLogMsg(Level.INFO, "-------> Slow warning : " + lines.get(found - 1));
//            }
//            count++;
//        }
//        this.logStepCompleted();
//
//        lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
//        assertTrue("Found " + lines.size() + " slow warning, but expected was " + found, (lines.size() == found));
//
//        CommonTasks.writeLogMsg(Level.INFO, "******* Slow request timing works for Even Sample Rate*******");
//
//    }

//    public void testSlowReqSampleRateNegative() throws Exception {
//
//        this.logStep("**** >>>>> Updating server with configuration : sampleRate=-2");
//        server.setServerConfigurationFile("server_sampleRateNeg2.xml");
//        this.logStepCompleted();
//        server.waitForStringInLog("CWWKG0017I", 30000);
//
//        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_3/");
//        this.logStep("----> Creating 2 requests...");
//
//        HttpURLConnection con;
//        BufferedReader br;
//        int count = 1;
//        List<String> lines;
//        while (count <= 2) {
//            CommonTasks.writeLogMsg(Level.INFO, count + " -----> Calling jdbcTestPrj_3 Application with URL=" + url.toString());
//            con = getHttpConnection(url);
//            br = getConnectionStream(con);
//            br.readLine();
//            count++;
//        }
//        this.logStepCompleted();
//
//        server.waitForStringInLog("TRAS0112W", 15000);
//        lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
//        CommonTasks.writeLogMsg(Level.INFO, " Size : " + lines.size());
//
//        assertTrue("Expected 2 slow request warning  but found : " + lines.size(), (lines.size() == 2));
//
//        CommonTasks.writeLogMsg(Level.INFO, "******* Slow request timing works for Negative Sample Rate*******");
//
//    }

    @Test
    public void testSlowReqSampleRateZero() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> Updating server with configuration : sampleRate=0");
        server.setServerConfigurationFile("server_sampleRate0.xml");
        server.waitForStringInLog("CWWKG0017I", 30000);

        createRequest("?sleepTime=3000");

        server.waitForStringInLog("TRAS0112W", 15000);
        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
        CommonTasks.writeLogMsg(Level.INFO, " Size : " + lines.size());

        assertTrue("Expected 1 (or more) slow request warning  but found : " + lines.size(), (lines.size() > 0));

        CommonTasks.writeLogMsg(Level.INFO, "******* Slow request timing works for Zero Sample Rate*******");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testDynamicEnableWithNoContextInfo() throws Exception {
        //Step 1 - Remove Request Timing feature
        CommonTasks.writeLogMsg(Level.INFO, "-----> Updating server configuration to REMOVE Request Timing feature..");
        server.setServerConfigurationFile("server_withOutReqTiming.xml");

        boolean removed = isRequestTimingRemoved();
        assertTrue("Could not find message - Server removed Request Timing Feature.", removed);
        CommonTasks.writeLogMsg(Level.INFO, " ------ > Request Timing feature removed : " + removed);

        //Step 2 - Add Request Timing feature
        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "-----> Updating server configuration to ADD Request Timing feature..");
        server.setServerConfigurationFile("server_original.xml");

        boolean added = isFeatureEnabled();

        assertTrue("Could not find message - Server added Request Timing Feature.", added);

        CommonTasks.writeLogMsg(Level.INFO, "********* Added Request Timing Feature..! *********");
        server.waitForStringInLogUsingMark("CWWKF0008I", 120000);
        server.setMarkToEndOfLog();

        //Step 3 - Updating server configuration - ContextInfo = false
        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "-----> Updating server configuration to Set contextInfo as FALSE..");
        server.setServerConfigurationFile("server_NOContextInfo.xml");
        waitForConfigurationUpdate();

        //Step 4 - Create a request for 3 seconds
        createRequest("?sleepTime=3000");
        server.waitForStringInLogUsingMark("TRAS0112W", 30000);

        //Step 5 - Check if contextInfo is disabled as expected
        List<String> lines = server.findStringsInFileInLibertyServerRoot("ms", MESSAGE_LOG);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            CommonTasks.writeLogMsg(Level.INFO, "-------->  " + line);

            assertFalse("contextInfo found!", line.contains("|"));

        }

        CommonTasks.writeLogMsg(Level.INFO, "--------> contextInfo is disabled properly..");
        CommonTasks.writeLogMsg(Level.INFO, "******* Adding requestTiming Element in runtime Works! *******");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testDynamicDisableWithNoContextInfo() throws Exception {
        //Step 1 -  Update server configuration - ContextInfo = false , Threshold = 2s
        CommonTasks.writeLogMsg(Level.INFO, "---> Updating server with ContextInfo = false");
        server.setServerConfigurationFile("server_NOContextInfo.xml");
        waitForConfigurationUpdate();

        CommonTasks.writeLogMsg(Level.INFO, "-----> Started Server with contextInfo disabled");

        //Step 2 -  create request for 3seconds and look for slow request warnings (they should have no contextInfo)
        createRequest("?sleepTime=3000");

        int slowCount = fetchNoOfslowRequestWarnings();
        assertTrue("No Slow request timing records found! : ", (slowCount > 0));

        List<String> lines = server.findStringsInFileInLibertyServerRoot("ms", MESSAGE_LOG);
        for (String line : lines) {
            assertFalse("contextInfo found when it was disabled..", line.contains("|"));
        }

        CommonTasks.writeLogMsg(Level.INFO, "---------> As expected : Pattern Not Found!  ...");

        //Step 3 - Reset back to a configuration with contextInfo enabled.
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("server_slowRequestThreshold1.xml");
        waitForConfigurationUpdate();

        //Step 4 - Create request for 3 seconds and look for slow request warning, should contain contextInfo now.
        createRequest("?sleepTime=3000");

        server.waitForStringInLogUsingMark("TRAS0112W", 20000);

        lines = server.findStringsInLogsUsingMark("ms", MESSAGE_LOG);

        for (String line : lines) {
            if (!line.contains("TRAS0112W") && !line.contains("TRAS0113I") && !line.contains("CWWKG0028A")) {
                assertTrue("contextInfo is missing...", line.contains("|"));
            }
        }

        CommonTasks.writeLogMsg(Level.INFO, "**************  Removed request timing element **************");

        //Step 5 - Remove Request Timing Feature
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("server_withOutReqTiming.xml");

        boolean removed = isRequestTimingRemoved();
        assertTrue("Request timing feature is not disabled..", removed);

        CommonTasks.writeLogMsg(Level.INFO, "********* Removed Request Timing Feature..! *********");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testSlowReqTimingThreshold() throws Exception {
        //Step 1 - Set slowRequestThreshold = 4 s
        CommonTasks.writeLogMsg(Level.INFO, "Updating server with slowRequestThreshold : 4 seconds...");
        server.setServerConfigurationFile("server_slowRequestThreshold4.xml");
        waitForConfigurationUpdate();

        //Step 2 - Create request of 5 seconds and see that slow request warning is coming for 4 seconds.
        createRequest("?sleepTime=5000");
        int slowCount = fetchNoOfslowRequestWarnings();
        assertTrue("No slow request warning found!", (slowCount > 0));

        //Step 3 - Set slowRequestThreshold = 7 s
        CommonTasks.writeLogMsg(Level.INFO, "Updating server with slowRequestThreshold : 7 seconds...");
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("server_slowRequestThreshold7.xml");
        waitForConfigurationUpdate();

        //Step 4 - Create request of 8 seconds and see that slow request warning is coming for 7 seconds.
        createRequest("?sleepTime=8000");

        slowCount = fetchNoOfslowRequestWarnings();

        assertTrue("Expected slow request record for 7s threshold is 1... but found : " + slowCount, (slowCount > 0));

        CommonTasks.writeLogMsg(Level.INFO, "******* slowRequestThreshold works!  *******");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testSlowReqDynamicPatternUpdate() throws Exception {
        //Step 1 - Update server configuration - ContextInfo = false , Threshold = 2s
        CommonTasks.writeLogMsg(Level.INFO, "--------> Started Server with Pattern disabled..");
        server.setServerConfigurationFile("server_NOContextInfo.xml");
        waitForConfigurationUpdate();

        //Step 2 -  create request for 3seconds and look for slow request warnings do not have contextInfo
        createRequest("?sleepTime=3000");

        int slowCount = fetchNoOfslowRequestWarnings();
        //Retry the request again
        if (slowCount == 0) {
            CommonTasks.writeLogMsg(Level.INFO, "$$$$ -----> Retry because no slow request warning found!");
            createRequest("?sleepTime=3000");
            slowCount = fetchNoOfslowRequestWarnings();
        }
        assertTrue("No Slow request timing records found! : ", (slowCount > 0));

        List<String> lines = server.findStringsInFileInLibertyServerRoot("ms", MESSAGE_LOG);

        for (String line : lines) {
            assertFalse("Pattern found when it is disabled..", (line.contains("|")));
        }
        CommonTasks.writeLogMsg(Level.INFO, "******** As Expected : Pattern disabled ******* ");

        //Step 3 - Enable contextInfo
        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "------> Updated server Configuration : Enabled Pattern.. ");
        server.setServerConfigurationFile("server_TrueContextInfo.xml");
        waitForConfigurationUpdate();

        //Step 4 -  create request for 3 seconds and look for slow request warnings have contextInfo
        createRequest("?sleepTime=3000");

        lines = server.findStringsInLogsUsingMark("ms", MESSAGE_LOG);
        server.setMarkToEndOfLog();

        for (String line : lines) {
            if (!line.contains("TRAS0112W") && !line.contains("TRAS0113I") && !line.contains("CWWKG0028A")) {
                assertTrue("Pattern NOT found when it is enabled..", (line.contains("|")));
            }
        }

        CommonTasks.writeLogMsg(Level.INFO, "*********** As Expected : Pattern enabled ********* ");
    }

    /*
     * This is already tested in testDynamicDisableWithNoContextInfo
     *
     * @Test
     *
     * @Mode(TestMode.FULL)
     * public void testSlowReqDynamicPatternRemove() throws Exception {
     * //Step 1 - Update server configuration - ContextInfo = false , Threshold = 2s
     * CommonTasks.writeLogMsg(Level.INFO, "--------> Starting Server with Pattern disabled..");
     * server.setServerConfigurationFile("server_NOContextInfo.xml");
     * waitForConfigurationUpdate();
     *
     * //Step 2 - create request for 3seconds and look for slow request warnings do not have contextInfo
     * createRequest("?sleepTime=3000");
     *
     * List<String> lines = server.findStringsInFileInLibertyServerRoot("ms", MESSAGE_LOG);
     *
     * for (String line : lines) {
     * assertFalse("contextInfo found when it is disabled..", (line.contains("|")));
     * }
     * CommonTasks.writeLogMsg(Level.INFO, "******** As Expected : Pattern disabled ******* ");
     *
     * //Step 3 - Update server configuration - default of request timing feature.
     * server.setMarkToEndOfLog();
     * server.setServerConfigurationFile("server_original.xml");
     * waitForConfigurationUpdate();
     *
     * //Step 3 - create request for 11 seconds and look for slow request warnings have contextInfo
     * createRequest("?sleepTime=11000");
     * server.waitForStringInLogUsingMark("TRAS0112W", 30000);
     *
     * lines = server.findStringsInLogsUsingMark("ms", MESSAGE_LOG);
     * server.setMarkToEndOfLog();
     *
     * for (String line : lines) {
     * if (!line.contains("TRAS0112W") && !line.contains("TRAS0113I") && !line.contains("CWWKG0028A")) {
     * assertTrue("contextInfo NOT found when it is enabled by default ..", (line.contains("|")));
     * }
     * }
     *
     * CommonTasks.writeLogMsg(Level.INFO, "*********** As Expected : Pattern enabled ********* ");
     * }
     */

    @Test
    @Mode(TestMode.FULL)
    public void testSlowReqSampleRateOdd() throws Exception {
        //Step 1 - Update server configuration - sampleRate=3, Threshold = "3s"
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> Updating server with configuration : sampleRate=3");
        server.setServerConfigurationFile("server_sampleRate3.xml");
        waitForConfigurationUpdate();

        //Step 2 - create 3 requests of 4s each - Every 3rd sample should generate a slow request warning.
        for (int count = 1; count < 4; count++) {
            createRequest("?sleepTime=4000");
        }

        //slow request warning:   Fetch ID and check if it ends with AC which confirms its 3rd request
        //TRAS0112W: Request AADJi05x9AW_AAAAAAAAAAC has been running on thread 00000023 for at least 3001.386ms
        server.waitForStringInLogUsingMark("TRAS0112W", 30000);
        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
        List<String> IDs = new ArrayList<String>();
        for (String msg : lines) {
            int start = msg.indexOf(" Request ") + "Request ".length();
            int end = msg.indexOf(" has");

            String ID = msg.substring(start, end).trim();
            IDs.add(ID);
        }
        for (String reqID : IDs) {
            CommonTasks.writeLogMsg(Level.INFO, "-----> Request ID : " + reqID);
            if (!(reqID.substring(reqID.length() - 2).equals("AC"))) {
                fail("Found slow request warning for even no. request : " + reqID);
            }
        }

        CommonTasks.writeLogMsg(Level.INFO, "******* Slow request timing works for Odd Sample Rate*******");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testSlowReqSampleRateEven() throws Exception {
        //Step 1 - Update server configuration - sampleRate=2, Threshold = "3s"
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> Updating server with configuration : sampleRate=2");
        server.setServerConfigurationFile("server_sampleRate2.xml");
        waitForConfigurationUpdate();

        //Step 2 - create 2 requests of 4s each - Every 2nd sample should generate a slow request warning.
        for (int count = 1; count < 3; count++) {
            createRequest("?sleepTime=4000");
        }

        //slow request warning:   Fetch ID and check if it ends with AB which confirms its 2nd request
        //TRAS0112W: Request AADJi05x9AW_AAAAAAAAAAB has been running on thread 00000023 for at least 3001.386ms

        server.waitForStringInLogUsingMark("TRAS0112W", 30000);
        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
        List<String> IDs = new ArrayList<String>();

        for (String msg : lines) {
            int start = msg.indexOf(" Request ") + "Request ".length();
            int end = msg.indexOf(" has");

            String ID = msg.substring(start, end).trim();
            IDs.add(ID);
        }
        for (String reqID : IDs) {
            CommonTasks.writeLogMsg(Level.INFO, "-----> Request ID : " + reqID);
            if (!(reqID.substring(reqID.length() - 2).equals("AB"))) {
                fail("Found slow request warning for odd no. request : " + reqID);
            }
        }

        CommonTasks.writeLogMsg(Level.INFO, "******* Slow request timing works for Even Sample Rate*******");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testSlowReqSampleRateNegative() throws Exception {
        //Step 1 - Update server configuration - sampleRate=-2, Threshold = "3s"
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> Updating server with configuration : sampleRate=-2");
        server.setServerConfigurationFile("server_sampleRateNeg2.xml");
        waitForConfigurationUpdate();

        //Step 2 - Create 2 requests of 4 seconds each and verify that it works like sampleRate 1.
        createRequest("?sleepTime=4000");
        createRequest("?sleepTime=4000");

        int slowCount = fetchNoOfslowRequestWarnings();
        assertTrue("Expected >1 slow request warning  but found : " + slowCount, (slowCount > 1));

        CommonTasks.writeLogMsg(Level.INFO, "******* Slow request timing works for Negative Sample Rate*******");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testSlowReqSampleRateDynamicUpdate() throws Exception {
        //Step 1 - Update server configuration - sampleRate=2, Threshold = "3s"
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> Updating server with configuration : sampleRate=2");
        server.setServerConfigurationFile("server_sampleRate2.xml");
        waitForConfigurationUpdate();

        //Step 2 - Create 2 requests of 4 seconds each.
        createRequest("?sleepTime=4000");
        createRequest("?sleepTime=4000");
        int slowCount = fetchNoOfslowRequestWarnings();

        assertTrue("No slow warning found for sampleRate 2!", (slowCount > 0));

        //Step 3 - Update server configuration - sampleRate=1, Threshold = "3s"
        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> Updating server with configuration : sampleRate=1");
        server.setServerConfigurationFile("server_sampleRate1.xml");
        waitForConfigurationUpdate();

        createRequest("?sleepTime=4000");
        int newslowCount = fetchNoOfslowRequestWarnings();

        assertTrue("No slow warning found for sampleRate 1!", (newslowCount - slowCount > 0));

        CommonTasks.writeLogMsg(Level.INFO, "******* Slow request timing works for Dynamic Update*******");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testSlowReqSampleRateDynamicEnable() throws Exception {
        //Step 1 - Update server configuration - Enable Request Timing
        CommonTasks.writeLogMsg(Level.INFO, "**** Starting server with default Request Timing configuration");
        server.setServerConfigurationFile("server_original.xml");

        //Step 2 -Create Request for 11 seconds
        createRequest("?sleepTime=11000");

        int slowCount = fetchNoOfslowRequestWarnings();
        assertTrue("No slow request warning found..", (slowCount > 0));

        //Step 3 - Update server configuration - sampleRate=2, Threshold = "3s"
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> Updating server with configuration : sampleRate=2");
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("server_sampleRate2.xml");
        waitForConfigurationUpdate();

        //Step 3 - Create 2 requests of 4 seconds each and verify that it works like sampleRate 2.
        createRequest("?sleepTime=4000");
        createRequest("?sleepTime=4000");
        int currentCount = fetchNoOfslowRequestWarnings() - slowCount;

        assertTrue("No slow warning found for sampleRate 2!", (currentCount > 0));
        CommonTasks.writeLogMsg(Level.INFO, "***** SampleRate Dynamic Enablement works as expected! *****");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testSlowReqSampleRateDynamicDisable() throws Exception {
        //Step 1 - Update server configuration - sampleRate=2, Threshold = "3s"
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> Updating server with configuration : sampleRate=2");
        server.setServerConfigurationFile("server_sampleRate2.xml");
        waitForConfigurationUpdate();

        //Step 2 -Create 2 Requests for 4 seconds
        createRequest("?sleepTime=4000");
        createRequest("?sleepTime=4000");

        int slowCount = fetchNoOfslowRequestWarnings();
        assertTrue("No slow warning found for sampleRate=2!", (slowCount > 0));

        //Step 3 - Update server configuration - reset to default configuration of Request Timing
        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "**** **** >>>>> Updating server with default Request Timing configuration");
        server.setServerConfigurationFile("server_original.xml");
        waitForConfigurationUpdate();

        //Step 4 -Create Request for 11 seconds
        createRequest("?sleepTime=11000");

        int currentCount = fetchNoOfslowRequestWarnings() - slowCount;
        assertTrue("No slow warning found for default configuration!", (currentCount > 0));

        CommonTasks.writeLogMsg(Level.INFO, "***** SampleRate Dynamic Disablement works as expected! *****");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testMaxSlowRequestTimingWarnings() throws Exception {
        //Step 1 - Update server configuration - Threshold = 3s
        CommonTasks.writeLogMsg(Level.INFO, "$$$ Updated Server Configuration  : Slow Request Threshold = 3s");
        server.setServerConfigurationFile("server_slowRequestThreshold3.xml");

        //Step 2 - create request of 13seconds. Fetch slow request warnings.
        createRequest("?sleepTime=13000");

        int warnings = fetchNoOfslowRequestWarnings();
        CommonTasks.writeLogMsg(Level.INFO, "$$$ -> No of Slow Request warnings : " + warnings);

        assertTrue("Expected 3 slow request warnings but found " + warnings, (warnings == 3));
    }

    private String fetchSlowRequestExceedingRecords(String record) {
        if (record == null || record.startsWith("[")) {
            return null;
        }

        String dur = record.substring(0, record.indexOf("ms"));

        if (dur.contains(".")) {
            dur = dur.substring(0, dur.indexOf("."));
        }

        int duration = new Integer(dur.trim()).intValue();

        if (duration > 1000) {
            return record;
        }

        return null;
    }

    /**
     * This method is used to get a connection stream from an HTTP connection. It
     * gives the output from the webpage that it gets from the connection
     *
     * @param con The connection to the HTTP address
     * @return The Output from the webpage
     */
    private BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        return br;
    }

    /**
     * This method creates a connection to a webpage and then returns the connection
     *
     * @param url The Http Address to connect to
     * @return The connection to the Http address
     */
    private HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }

    private boolean isRequestTimingRemoved() throws Exception {
        server.waitForStringInLogUsingMark("CWWKG0017I", 20000);
        server.waitForStringInLogUsingMark("CWWKF0013I", 60000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("CWWKF0013I", MESSAGE_LOG);
        boolean removed = false;
        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "----> Feature removed : " + line);
            if (line.contains("requestTiming-1.0")) {
                removed = true;
            }
        }
        return removed;
    }

    private boolean isFeatureEnabled() throws Exception {
        server.waitForStringInLogUsingMark("CWWKF0012I", 50000);
        List<String> lines = server.findStringsInFileInLibertyServerRoot("CWWKF0012I", MESSAGE_LOG);
        boolean enabled = false;
        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "----> Feature added : " + line);
            if (line.contains("requestTiming-1.0")) {
                enabled = true;
            }
        }
        return enabled;
    }

    private int fetchNoOfslowRequestWarnings() throws Exception {
        server.waitForStringInLogUsingMark("TRAS0112W", 30000);
        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "----> slow request warning : " + line);
        }
        return lines.size();
    }

    private void waitForConfigurationUpdate() throws Exception {
        server.waitForStringInLogUsingMark("CWWKG0017I", 30000);
    }
}