/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
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
package com.ibm.ws.request.timing.hung.fat;

import static componenttest.annotation.SkipForRepeat.EE10_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE8_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class HungRequestTiming {

    private static final String MESSAGE_LOG = "logs/messages.log";
    private static final String TRACE_LOG = "logs/trace.log";

    private static final String SERVER_NAME = "HungRequestTimingServer";

    // Need to set a formatter to use in the test cases.
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "TestWebApp", "com.ibm.testwebapp");
        CommonTasks.writeLogMsg(Level.INFO, " starting server...");
        server.startServer();
    }

    @Before
    public void setupTestStart() throws Exception {
        if (server != null && !server.isStarted()) {
            server.startServer();
        }

        // Allow the configuration to change back to the original and ensure the update is finished before starting a test
        server.setServerConfigurationFile("server_original.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);
        server.setMarkToEndOfLog();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("TRAS0114W", "TRAS0115W");
        }
    }

    @Test
    public void testParallelMultipleRequests() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "Setting hung threshold as 2s");
        server.setServerConfigurationFile("server_hungRequestThreshold2.xml");
        String srvConfigCompletedMsg = server.waitForStringInLog("CWWKG0017I", 90000);

        assertNotNull("The server configuration was successfully updated message was not found!", srvConfigCompletedMsg);

        CommonTasks.writeLogMsg(Level.INFO, "************ Thread 1 - Runs 10s  ************");
        CommonTasks.writeLogMsg(Level.INFO, "************ Thread 2 - Runs 14s ************");

        HungRequestThread request1 = new HungRequestThread(10000);
        HungRequestThread request2 = new HungRequestThread(14000);

        request1.start();
        request2.start();

        request1.join();
        request2.join();

        server.waitForStringInLog("TRAS0114W", 60000);
        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
        assertTrue("Expected 2 or more Hung detection warnings but found : " + lines.size(), (lines.size() > 1));

        CommonTasks.writeLogMsg(Level.INFO, "----> Hung Request Warning : \n" + lines.get(0) + "\n" + lines.get(1));

        CommonTasks.writeLogMsg(Level.INFO, "****** Hung Request Timing works for concurrent requests ******");
    }

    @Test
    public void testHungRequestDisable() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "Setting hung threshold as 0");
        server.setServerConfigurationFile("server_hungRequestThreshold0.xml");
        String srvConfigCompletedMsg = server.waitForStringInLog("CWWKG0017I", 90000);

        assertNotNull("The server configuration was successfully updated message was not found!", srvConfigCompletedMsg);

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestWebApp/TestServlet?sleepTime=4500");
        CommonTasks.writeLogMsg(Level.INFO, "Calling TestWebApp Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        br.readLine();

        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
        assertTrue("Hung detection warning found!!!", (lines.size() == 0));
        CommonTasks.writeLogMsg(Level.INFO, "****** Hung Request Timing is disabled for 0 hungRequestThreshold ******");
    }

    @Test
    @SkipForRepeat({ EE8_FEATURES, EE9_FEATURES, EE10_FEATURES })
    public void testHungRequestIntrospector() throws Exception {
        final String METHOD_NAME = "testHungRequestIntrospector";

        CommonTasks.writeLogMsg(Level.INFO, "Setting up multiple hung request timing elements");
        server.setServerConfigurationFile("server_hungRequestThreshold1.xml");
        String srvConfigCompletedMsg = server.waitForStringInLog("CWWKG0017I", 90000);

        assertNotNull("The server configuration was successfully updated message was not found!", srvConfigCompletedMsg);

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestWebApp/TestServlet?sleepTime=1000");
        CommonTasks.writeLogMsg(Level.INFO, "Calling TestWebApp Application once with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        br.readLine();

        url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestWebApp/FirstIntrospectorTestServlet");
        CommonTasks.writeLogMsg(Level.INFO, "Calling TestWebApp Application twice with URL=" + url.toString());
        for (int x = 0; x < 2; x++) {
            con = getHttpConnection(url);
            br = getConnectionStream(con);
            br.readLine();
        }

        url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestWebApp/SecondIntrospectorTestServlet");
        CommonTasks.writeLogMsg(Level.INFO, "Calling TestWebApp Application twice with URL=" + url.toString());
        for (int x = 0; x < 2; x++) {
            con = getHttpConnection(url);
            br = getConnectionStream(con);
            br.readLine();
        }

        // Should have 5 requests now.  Lets get the introspector and make sure it reports correctly.
        server.serverDump();

        File[] filesAfterDump = new File(server.getInstallRoot() + File.separator + "usr" + File.separator + "servers" + File.separator + SERVER_NAME).listFiles();

        File dumpFile = new File("");
        for (File f : filesAfterDump) {
            String fileName = f.getName();
            Log.info(this.getClass(), METHOD_NAME, "Found file: " + fileName);
            if (fileName.startsWith(SERVER_NAME + ".dump") && fileName.endsWith(".zip")) {
                dumpFile = f;
                break;
            }
        }

        if (dumpFile.getPath().compareTo("") == 0) {
            fail("The Dump File was not found");
        }

        Map<String, Integer> requestMap = new HashMap<String, Integer>();
        int totalRequestCount = 0;

        ZipFile zipFile = new ZipFile(dumpFile);
        for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
            ZipEntry entry = en.nextElement();
            String entryName = entry.getName();
            if (entryName.endsWith("HungRequestProbeExtensionIntrospector.txt")) {
                InputStream inputstream = zipFile.getInputStream(entry);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputstream));
                String line;
                int i = 0;
                Pattern p = Pattern.compile("Context info pattern: (.*) Request count: (.*)");
                while ((line = reader.readLine()) != null) {
                    Log.info(this.getClass(), METHOD_NAME, "Run" + i + ": " + line);
                    Matcher m = p.matcher(line.trim());
                    if (m.matches()) {
                        String contextInfo = m.group(1);
                        Integer requestCount = Integer.parseInt(m.group(2));
                        Log.info(this.getClass(), METHOD_NAME, "Found " + contextInfo + " " + requestCount);
                        requestMap.put(contextInfo.trim(), requestCount);
                        totalRequestCount += requestCount;
                    }
                    i++;
                }
                reader.close();
                inputstream.close();
            }
        }

        zipFile.close();

        // Check our results.
        assertTrue("Total request count was wrong, should be 5 but was " + totalRequestCount, (totalRequestCount == 5));
        assertTrue(requestMap.containsKey("TestWebApp | TestServlet"));
        assertTrue(requestMap.get("TestWebApp | TestServlet") == 1);
        assertTrue(requestMap.containsKey("TestWebApp | FirstIntrospectorTestServlet"));
        assertTrue(requestMap.get("TestWebApp | FirstIntrospectorTestServlet") == 2);
        assertTrue(requestMap.containsKey("TestWebApp | ThirdIntrospectorTestServlet"));
        assertTrue(requestMap.get("TestWebApp | ThirdIntrospectorTestServlet") == 0);
        assertTrue(requestMap.containsKey("*"));
        assertTrue(requestMap.get("*") == 2);

        CommonTasks.writeLogMsg(Level.INFO, "****** Hung Request Introspector is counting requests correctly ******");
    }

    @Test
    public void testHungDynamicThresholdUpdate() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "Setting hung threshold as 2s");
        server.setServerConfigurationFile("server_hungRequestThreshold2.xml");
        server.waitForStringInLog("CWWKG0017I", 90000);
        server.setMarkToEndOfLog();

        createRequest(4000);

        CommonTasks.writeLogMsg(Level.INFO, "Waiting for hung detection warning");
        server.waitForStringInLog("TRAS0114W", 30000);
        server.setMarkToEndOfLog();

        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
        int previous = lines.size();

        // Retry the request again, since sometimes in the SOE builds, the feature update takes
        // some time, and the request is created before the feature is properly updated,
        // and the requestTiming warning does not registered in time.
        if (previous == 0) {
            CommonTasks.writeLogMsg(Level.INFO, "$$$$ -----> Retry the request because no hung request warning found!");
            createRequest(4000);
            server.waitForStringInLog("TRAS0114W", 30000);
            lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
            previous = lines.size();
        }

        CommonTasks.writeLogMsg(Level.INFO, "----> 1 - No of Hung detection warnings found : " + previous);
        assertTrue("No Hung detection warning found!!!", (previous > 0));

        String line1 = lines.get(previous - 1);
        CommonTasks.writeLogMsg(Level.INFO, "----> Hung Request Warning 1 : " + line1);
        assertTrue("Hung warning does not show that request was hung for 2s as expected..", isCorrectDuration(line1, 2000));

        CommonTasks.writeLogMsg(Level.INFO, "Setting hung threshold as 3s");
        server.setServerConfigurationFile("server_hungRequestThreshold3.xml");
        server.waitForStringInLogUsingMark("CWWKG0017I", 90000);
        server.setMarkToEndOfLog();

        createRequest(4000);

        CommonTasks.writeLogMsg(Level.INFO, "Waiting for hung detection warning");
        server.waitForStringInLogUsingMark("TRAS0114W", 30000);
        server.setMarkToEndOfLog();

        lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
        int current = lines.size();
        CommonTasks.writeLogMsg(Level.INFO, "----> 2 - No of Hung detection warnings found : " + current);

        // Retry the request again, since sometimes in the SOE builds, the feature update takes
        // some time, and the request is created before the feature is properly updated,
        // and the requestTiming warning does not registered in time.
        if (current - previous == 0) {
            CommonTasks.writeLogMsg(Level.INFO, "$$$$ -----> Retry the request because no hung request warning found!");
            createRequest(4000);
            server.waitForStringInLogUsingMark("TRAS0114W", 30000);
            lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
            current = lines.size();
        }
        assertTrue("No new Hung detection warning found!!!", (current - previous > 0));

        String line2 = lines.get(current - 1);
        CommonTasks.writeLogMsg(Level.INFO, "----> Hung RequestWarning 2 : " + line2);
        assertTrue("Hung warning does not show that request was hung for 3s as expected..", isCorrectDuration(line2, 3000));

        CommonTasks.writeLogMsg(Level.INFO, "***** Dynamic Update of HungRequestThreshold works as expected! *****");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testHungRequestDynamicEnable() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "Starting server without Request Timing Feature..");
        server.setServerConfigurationFile("server_withOutReqTiming.xml");
        server.waitForStringInLog("CWWKF0007I", 90000); // feature update started message
        String temp = server.waitForStringInLog("CWWKF0013I", 90000); // feature removed message
        assertNotNull("Could not find message - Server removed Request Timing and Request Probe Features.", temp);
        server.waitForStringInLog("CWWKF0008I", 90000); // feature update completed message

        CommonTasks.writeLogMsg(Level.INFO, " ------> Request Timing feature removed : " + temp);

        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "-----> Updating server configuration to ADD Request Timing feature..");
        server.setServerConfigurationFile("server_original.xml");

        server.waitForStringInLogUsingMark("CWWKF0012I", 30000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("CWWKF0012I", MESSAGE_LOG);
        boolean featureFound = false;
        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, " ------> Config Update Warning : " + line);
            if (line.contains("requestTiming-1.0")) {
                featureFound = true;
            }
        }
        assertTrue("Request Timing Feature is not added..", featureFound);

        CommonTasks.writeLogMsg(Level.INFO, "********* Added Request Timing Feature..! *********");

        server.setMarkToEndOfLog();
        // Disabling thread dumps, so server stops gracefully, instead of waiting for all thread dumps to be generated.
        CommonTasks.writeLogMsg(Level.INFO, "Setting hung threshold as 2s, with thread dumps disabled");
        server.setServerConfigurationFile("server_hungRequestThreshold2_disableThreadDumps.xml");

        server.waitForStringInLogUsingMark("CWWKG0017I", 50000); // server config successfully completed.
        server.waitForStringInLogUsingMark("CWWKF0012I", 10000); // server successfully installed requestTiming feature.
        server.waitForStringInLogUsingMark("CWWKF0008I", 10000); // feature update completed.

        createRequest(3000);

        server.waitForStringInLogUsingMark("TRAS0114W", 90000);
        lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
        int numOfRTWarnings = lines.size();
        // Retry the request again, since sometimes in the SOE builds, the feature update takes
        // some time, and the request is created before the feature is properly updated,
        // and the requestTiming warning does not get registered in time.
        if (numOfRTWarnings == 0) {
            CommonTasks.writeLogMsg(Level.INFO, "$$$$ -----> Retry the request because no hung request warning found!");
            createRequest(3000);
            server.waitForStringInLogUsingMark("TRAS0114W", 90000);
            lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
            numOfRTWarnings = lines.size();
        }

        CommonTasks.writeLogMsg(Level.INFO, "---> No. of Hung warnings : " + numOfRTWarnings);
        assertTrue("No Hung detection warning found!!!", (numOfRTWarnings > 0));

        CommonTasks.writeLogMsg(Level.INFO, "********** Hung Request Timing works when added dynamically **********");
    }

    @Test
    @Mode(TestMode.FULL)
    @SkipForRepeat({ EE8_FEATURES, EE9_FEATURES, EE10_FEATURES })
    public void testHungRequestDynamicDisable() throws Exception {
        // Disabling thread dumps, so server stops gracefully, instead of waiting for all thread dumps to be generated.
        CommonTasks.writeLogMsg(Level.INFO, "Setting hung threshold as 2s, with thread dumps disabled");
        server.setServerConfigurationFile("server_hungRequestThreshold2_disableThreadDumps.xml");
        String srvConfigCompletedMsg = server.waitForStringInLog("CWWKG0017I|CWWKG0018I", 90000);

        assertNotNull("The server configuration was successfully updated message was not found!", srvConfigCompletedMsg);

        createRequest(3000);

        CommonTasks.writeLogMsg(Level.INFO, "Waiting for hung detection warning");
        server.waitForStringInLog("TRAS0114W", 30000);

        server.setMarkToEndOfLog();

        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
        int previous = lines.size();
        // Retry the request again, since sometimes in the SOE builds, the feature update takes
        // some time, and the request is created before the feature is properly updated,
        // and the requestTiming warning does not registered in time.
        if (previous == 0) {
            CommonTasks.writeLogMsg(Level.INFO, "$$$$ -----> Retry the request because no hung request warning found!");
            createRequest(4000);
            server.waitForStringInLog("TRAS0114W", 30000);
            lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
            previous = lines.size();
        }
        assertTrue("No Hung detection warning found!!!", (previous > 0));

        CommonTasks.writeLogMsg(Level.INFO, "----> Hung Request Warning : \n" + lines.get(0));

        CommonTasks.writeLogMsg(Level.INFO, "***** Removing Threshold Configuration  *****");
        server.setServerConfigurationFile("server_original.xml");
        server.waitForStringInLogUsingMark("CWWKG0017I", 90000);

        createRequest(3000);

        lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
        CommonTasks.writeLogMsg(Level.INFO, "---> No of Hung detection warnings found : " + lines.size());
        assertTrue("Hung detection warning found when none was expected!!!", ((lines.size() - previous) == 0));

        CommonTasks.writeLogMsg(Level.INFO, "***** Removing Request Timing Feature  *****");
        server.setServerConfigurationFile("server_withOutReqTiming.xml");
        server.waitForStringInLogUsingMark("CWWKF0013I", 30000);

        lines = server.findStringsInFileInLibertyServerRoot("CWWKF0013I", MESSAGE_LOG);

        boolean requestTimingFeatureRemoved = false;
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).contains("requestTiming")) {
                CommonTasks.writeLogMsg(Level.INFO, "------> Request Timing feature disabled!" + "\n Message found  : " + lines.get(0));
                requestTimingFeatureRemoved = true;
            }
        }

        assertTrue("Request timing feature is not disabled..", requestTimingFeatureRemoved);

        CommonTasks.writeLogMsg(Level.INFO, "********* Removed Request Timing Feature..! *********");
    }

    @Test
    @Mode(TestMode.FULL)
    @SkipForRepeat({ EE8_FEATURES, EE9_FEATURES, EE10_FEATURES })
    public void testSequentialHungMultipleRequests() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "Setting hung threshold as 2s");
        server.setServerConfigurationFile("server_hungRequestThreshold2.xml");

        server.waitForStringInLog("CWWKG0017I|CWWKG0018I", 90000);

        createRequest(3000);

        // The next request will have 59secs to complete before the next Java core gets generated
        // The Java cores get generated 1min apart from each other
        long finishTimeOfFirstRequest = System.currentTimeMillis();

        server.waitForStringInLog("TRAS0114W", 90000);
        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
        int previous = lines.size();
        // Retry the request again, since sometimes in the SOE builds, the feature update takes
        // some time, and the request is created before the feature is properly updated,
        // and the requestTiming warning does not registered in time.
        if (previous == 0) {
            CommonTasks.writeLogMsg(Level.INFO, "$$$$ -----> Retry the request because no hung request warning found!");
            createRequest(4000);
            finishTimeOfFirstRequest = System.currentTimeMillis();
            server.waitForStringInLog("TRAS0114W", 90000);
            lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
            previous = lines.size();
        }
        CommonTasks.writeLogMsg(Level.INFO, "---> No. of Hung warnings : " + previous);
        assertTrue("Hung detection warning found!!!", (previous > 0));
        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "---> Hung warning : " + line);
        }

        // wait for request 1 to complete
        server.waitForStringInLog("TRAS0115W", 30000);

        server.setMarkToEndOfLog();

        CommonTasks.writeLogMsg(Level.INFO, "****** Request 2 ******");

        createRequest(3000);

        server.waitForStringInLogUsingMark("TRAS0114W", 90000);
        lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
        int current = lines.size();
        CommonTasks.writeLogMsg(Level.INFO, "---> No. of Hung warnings : " + current);
        assertTrue("Hung detection warning found!!!", ((current - previous) > 0));
        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "---> Hung warning : " + line);
        }

        //Java core created ID : CWWKE0068I
        lines = server.findStringsInFileInLibertyServerRoot("CWWKE0068I", MESSAGE_LOG);
        int cores = lines.size();
        CommonTasks.writeLogMsg(Level.INFO, "---> No. of java cores created : " + cores);
        if (System.currentTimeMillis() - finishTimeOfFirstRequest > 59000) {
            fail("Test case testSequentialHungMultipleRequests is inconclusive as the second request was not able to complete within"
                 + "one minute, thus allowing the possibility of another Java core to be generated");
        } else {
            assertTrue("Expected 1 Java core.. But found : " + cores, (cores == 1));
        }

        CommonTasks.writeLogMsg(Level.INFO, "****** Hung Request Multiple Requests works as expected! ******");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testHungRequestTiming() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "Setting hung threshold as 30s");
        server.setServerConfigurationFile("server_hungRequestThreshold30.xml");
        String srvConfigCompletedMsg = server.waitForStringInLog("CWWKG0017I|CWWKG0018I", 90000);

        assertNotNull("The server configuration was successfully updated message was not found!", srvConfigCompletedMsg);

        createRequest(290000); // We must wait this long to see if more than 3 java cores are generated (we expect only 3)

        CommonTasks.writeLogMsg(Level.INFO, "Waiting for hung detection warning");
        server.waitForStringInLog("TRAS0114W", 90000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
        assertTrue("No Hung detection warning found!!!", (lines.size() > 0));

        CommonTasks.writeLogMsg(Level.INFO, "----> Hung Request Warning : \n" + lines.get(0));
        CommonTasks.writeLogMsg(Level.INFO, "Waiting for Java cores to be generated");
        // Waiting for just one of the Java core request message is enough since we already make the request sleep for about 4.83min
        server.waitForStringInLog("CWWKE0067I", 30000);
        server.waitForStringInLog("TRAS0115W", 30000);

        lines = server.findStringsInFileInLibertyServerRoot("TRAS0115W", MESSAGE_LOG);
        assertTrue("Hung completion message not found!!", (lines.size() > 0));
        CommonTasks.writeLogMsg(Level.INFO, "----> Hung completion message : " + lines.get(0));

        lines = server.findStringsInFileInLibertyServerRoot("CWWKE0067I", MESSAGE_LOG);
        CommonTasks.writeLogMsg(Level.INFO, "----> Number of Java dumps : " + lines.size());
        assertTrue("Expected 3 Java Dumps but found : " + lines.size(), (lines.size() == 3));

        List<String> timerStartLine = server.findStringsInFileInLibertyServerRoot("Starting thread dump scheduler", TRACE_LOG);
        server.waitForStringInLog("CWWKE0068I", 30000); //  Wait for the Java core created message, with the file path.
        lines = server.findStringsInFileInLibertyServerRoot("CWWKE0068I", MESSAGE_LOG);
        assertTrue("No Java core generated warnings found!", (lines.size() > 0));

        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "----> Dump file : " + line);
        }

        int javaCoreCount = 0;
        LocalTime prevDumpFileTime = null;
        LocalTime currDumpFileTime = null;
        Duration timeDiffBetweenDumps = null;

        for (String line : lines) {
            String javaDumpName = line.substring(line.lastIndexOf("java"));
            CommonTasks.writeLogMsg(Level.INFO, "----> Processing Java dump file : " + javaDumpName);

            if (javaDumpName != null && !javaDumpName.isEmpty()) {
                javaCoreCount++;
                String time_HHmmss = "";
                String seconds = "";

                // Parse the Hours, minutes and seconds from the Java Dump File name
                // e.g javacore.20240815.144030.98608.0001.txt
                time_HHmmss = javaDumpName.substring(18, 24); // This would be 144030, from the above example.
                CommonTasks.writeLogMsg(Level.INFO, "----> time_HHmmss : " + time_HHmmss);

                if (javaCoreCount == 1) {
                    // Processing the first file, just need to store the time it was generated, to be used later.
                    CommonTasks.writeLogMsg(Level.INFO, "Processing the first java dump file.");
                    prevDumpFileTime = LocalTime.parse(time_HHmmss, FORMATTER);
                    continue;
                } else {
                    currDumpFileTime = LocalTime.parse(time_HHmmss, FORMATTER);
                }

                // Find the time elapsed between the current dump file and the previous dump file.
                timeDiffBetweenDumps = Duration.between(prevDumpFileTime, currDumpFileTime);

                if (timeDiffBetweenDumps.isNegative()) {
                    // This handles the case when the dump files are generated at 23:59pm and 00:00 (midnight), the next day.
                    // Adds another day to the Duration time difference to handle the negative.
                    CommonTasks.writeLogMsg(Level.INFO, "The java dumps were generated across two days.");
                    timeDiffBetweenDumps = timeDiffBetweenDumps.plusDays(1);
                }

                long elapsedMins = timeDiffBetweenDumps.toMinutes() % 60; // There are 60 mins in an hour, extracting only the minutes.
                long elapsedSecs = timeDiffBetweenDumps.getSeconds() % 60; // There are 60 seconds in a minute, extracting only the seconds.
                CommonTasks.writeLogMsg(Level.INFO, "The java dump file " + javaDumpName + " was generated after " + elapsedMins + " minute and " + elapsedSecs + " seconds.");

                if (elapsedMins == 0) {
                    // Handles the case, when there are system/env related issues, and the java dumps are generated around 50+ secs, and not in the full 1 min.
                    assertTrue("Java dumps are generated less than 1 min apart.", (elapsedSecs > 50));
                } else {
                    // This verifies if the java dumps are generated 1 minute apart, including intermittent cases where the system might be really slow,
                    // and causes the dumps to be generated within 1 min and 40 seconds, which is tolerable.
                    assertTrue("Java dumps are NOT generated 1 min apart.", (elapsedMins == 1 && elapsedSecs <= 40));
                }

                // Cache the current dump file time, to compare with the next dump file.
                prevDumpFileTime = currDumpFileTime;
            }
        }

        CommonTasks.writeLogMsg(Level.INFO, "----> Java cores are generated 1 min apart");

        assertTrue("Expected 3 Hung detection warnings but found : " + javaCoreCount, (javaCoreCount == 3));

        CommonTasks.writeLogMsg(Level.INFO, "****** Hung Request Timing is works as expected ******");
    }

    private void createRequest(int duration) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestWebApp/TestServlet?sleepTime=" + duration);
        CommonTasks.writeLogMsg(Level.INFO, "Calling TestWebApp Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        br.readLine();
    }

    private boolean isCorrectDuration(String warning, int durationInMilliSecs) {
        String line = warning.substring(warning.indexOf("has been running on thread"));
        line = line.substring(line.indexOf("for") + 13, line.indexOf("ms"));
        int duration = new Double(line).intValue();
        if (duration >= durationInMilliSecs) {
            return true;
        }

        return false;
    }

    class HungRequestThread extends Thread {

        long duration = 6000;

        public HungRequestThread(long duration) {
            this.duration = duration;
        }

        @Override
        public void run() {
            try {
                URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestWebApp/TestServlet?sleepTime=" + duration);
                HttpURLConnection con = getHttpConnection(url);
                BufferedReader br = getConnectionStream(con);
                br.readLine();
            } catch (Exception e) {
                // nothing for now
            }
        }
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
}