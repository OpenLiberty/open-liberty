/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import junit.framework.Assert;

/**
 *
 */
@RunWith(FATRunner.class)
public class HandlerTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("SampleSourceHandlerServer");
    private static LibertyServer MsgServer = LibertyServerFactory.getLibertyServer("MsgServer");
    private static LibertyServer traceServer = LibertyServerFactory.getLibertyServer("TraceSourceHandlerServer");

    public static final String TRACE_LOG = "logs/trace.log";
    private static final String MESSAGE_LOG = "logs/messages.log";
    private static final String TRACEHANDLERIMPL_LOG = "logs/tracehandlerimpl.log";
    private static final String ACCESS_LOG_1 = "logs/http_access_1.log";

    private static final Class<?> c = HandlerTest.class;

    private static final String SAMPLE_SOURCE_HANDLER_BUNDLE_JAR = "sample.source.handler";
    private static final String SAMPLE_SOURCE_HANDLER_FEATURE = "sampleSourceHandler-1.0";

    private static final String MSG_SOURCE_HANDLER_BUNDLE_JAR = "message.source.handler";
    private static final String FFDC_SOURCE_HANDLER_BUNDLE_JAR = "ffdc.source.handler";

    private static final String ACCESSLOG_SOURCE_HANDLER_BUNDLE_JAR = "accesslog.source.handler";
    private static final String TRACE_SOURCE_HANDLER_BUNDLE_JAR = "trace.source.handler";
    private static final String TRACE_SOURCE_HANDLER_FEATURE = "sampleTraceSourceHandler-1.0";

    private static final String USER = "admin";
    private static final String USERPWD = "adminpwd";

    private static final CountDownLatch lock = new CountDownLatch(1);

    private static URL url;

    @Rule
    public TestName testName2 = new TestName();

    public String testName = "";

    @BeforeClass
    public static void startTest() throws Exception {

        server.installSystemBundle(SAMPLE_SOURCE_HANDLER_BUNDLE_JAR);
        server.installSystemBundle(FFDC_SOURCE_HANDLER_BUNDLE_JAR);
        server.installSystemBundle(MSG_SOURCE_HANDLER_BUNDLE_JAR);
        server.installSystemFeature(SAMPLE_SOURCE_HANDLER_FEATURE);
        ShrinkHelper.defaultDropinApp(server, "TestApp", "com.ibm.ws.collector.manager", "collector.manager_fat.app");
        // server.startServer();

        MsgServer.installSystemBundle(SAMPLE_SOURCE_HANDLER_BUNDLE_JAR);
        MsgServer.installSystemBundle(MSG_SOURCE_HANDLER_BUNDLE_JAR);
        MsgServer.installSystemFeature(SAMPLE_SOURCE_HANDLER_FEATURE);
        ShrinkHelper.defaultDropinApp(MsgServer, "TestApp", "com.ibm.ws.collector.manager", "collector.manager_fat.app");
        // MsgServer.startServer();

        traceServer.installSystemBundle(ACCESSLOG_SOURCE_HANDLER_BUNDLE_JAR);
        traceServer.installSystemBundle(TRACE_SOURCE_HANDLER_BUNDLE_JAR);
        traceServer.installSystemFeature(TRACE_SOURCE_HANDLER_FEATURE);
        ShrinkHelper.defaultDropinApp(traceServer, "TestApp", "com.ibm.ws.collector.manager", "collector.manager_fat.app");

        // traceServer.startServer();
    }

    @Before
    public void setUp() throws Exception {
        testName = testName2.getMethodName();

        // Server
        if (testName.matches("testSourceHandlerFeatureInstall|" +
                             "testSampleSourceHandler|" +
                             "testGCSource|" +
                             "testFFDCSource|" +
                             "testDuplicateFFDC|")) {
            if (server != null && !server.isStarted()) {
                server.startServer();
            }
        }

        // MsgServer
        if (testName.matches("testMessageSource|")) {
            if (MsgServer != null && !MsgServer.isStarted()) {
                MsgServer.startServer();
            }
        }

        // TraceServer
        if (testName.matches("testTraceSource.*|" +
                             "testAccessLogSource|")) {
            if (traceServer != null && !traceServer.isStarted()) {
                traceServer.startServer();
            }
        }
    }

    @Test
    public void testSourceHandlerFeatureInstall() {

        testName = "testSourceHandlerFeatureInstall";
        try {
            server.setMarkToEndOfLog();

            Log.info(c, testName, "Started server with sample feature");

            Log.info(c, testName, "Verifying  sampleSourceHandler feature is enabled");
            List<String> lines = server.findStringsInFileInLibertyServerRoot("CWWKF0012I", MESSAGE_LOG);

            String line = lines.get(0);
            Log.info(c, testName, "----> Line : " + line);

            assertTrue("Sample Features are not enabled", (line.contains("sampleSourceHandler-1.0")));

            Log.info(c, testName, "***** sampleSourceHandler Feature is Enabled *****");
        } catch (Exception e) {

        }
    }

    @Test
    public void testMessageSource() throws Exception {
        testName = "testMessageSource";

        if (MsgServer != null && !MsgServer.isStarted()) {
            MsgServer.startServer();
        }

        MsgServer.waitForStringInLog("CWWKF0011I", 30000);
        url = new URL("http://" + MsgServer.getHostname() + ":" + MsgServer.getHttpSecondaryPort() + "/TestApp/");

        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        Log.info(c, testName, " Output of br for TestLoggingLevel servlet : " + br.readLine());

        Log.info(c, testName, " Starting To Wait Now ------> ");
        MsgServer.waitForStringInTrace("Received message event");

        //Received message event: MessageLogData [datetime=2015-12-09T18:27:30.308+0530, threadId=40,
        //loggerName=com.ibm.ws.kernel.feature.internal.FeatureManager, logLevel=AUDIT, messageId=CWWKF0012I,
        //message=CWWKF0012I: The server installed the following features: [jsp-2.2, servlet-3.0, sampleSourceHandler-1.0, timedexit-1.0]., methodName=null, className=com.ibm.ws.kernel.feature.internal.FeatureManager, extensions={}, sequence=10]
        List<String> json = new ArrayList<String>();
        json.add("datetime");
        json.add("threadId");
        json.add("module");
        json.add("loglevel");
        json.add("messageId");
        json.add("message");

        List<String> lines = MsgServer.findStringsInFileInLibertyServerRoot("Received message event", TRACE_LOG);
        Log.info(c, testName, "----> Received message events.. : " + lines.size());
        boolean sysOut = false;
        boolean sysErr = false;
        boolean utilLogSevere = false;
        boolean utilLogWarn = false;
        boolean utilLogInfo = false;
        boolean checkMsgID = false;
        boolean allLevels = false;
        boolean rawAllLevels = false;
        if (lines.size() > 0) {
            String line = lines.get(0);
            Log.info(c, testName, "----> Message Event : " + line);
            for (String field : json) {
                if (!line.contains(field)) {
                    fail(field + " missing..");
                }
            }

            List<String> levels = new ArrayList<String>();
            levels.add("severity=I");
            levels.add("severity=W");
            levels.add("severity=A");
            levels.add("severity=E");
            levels.add("severity=F");

            List<String> rawLevels = new ArrayList<String>();
            rawLevels.add("loglevel=INFO");
            rawLevels.add("loglevel=WARNING");
            rawLevels.add("loglevel=AUDIT");
            rawLevels.add("loglevel=ERROR");
            rawLevels.add("loglevel=FATAL");

            boolean info = false;
            boolean audit = false;

            boolean raw_info = false;
            boolean raw_audit = false;

            for (String rec : lines) {
                Log.info(c, testName, "----> rec ******** " + rec);

                if (!checkMsgID) {
                    String msgId = rec.substring(rec.indexOf("messageId="));
                    msgId = msgId.substring(0, msgId.indexOf(","));
                    Log.info(c, testName, "----> MessageID = " + msgId);
                    assertFalse("MessageId contains ;", msgId.contains(";"));
                    assertFalse("MessageId contains :", msgId.contains(":"));

                    Log.info(c, testName, "----> MessageID does not contain ; or :");
                    checkMsgID = true;
                }

                if ((!sysOut) && rec.contains("loggerName=O")) {

                    assertTrue("MessageId not null for system out..", rec.contains("messageId=null"));
                    Log.info(c, testName, "----> MessageID null for sys out");
                    sysOut = true;

                    assertTrue("whether logLevelRaw has complete word for SystemOut", rec.contains("loglevel=SystemOut"));

                } else if ((!sysErr) && rec.contains("loggerName=R")) {

                    assertTrue("MessageId not null for system err..", rec.contains("messageId=null"));
                    Log.info(c, testName, "----> MessageID null for sys err");
                    sysErr = true;
                    assertTrue("whether logLevelRaw has complete word for System error", rec.contains("loglevel=SystemErr"));

                } else if (rec.contains("loggerName=ezlogger")) {
                    if (!(utilLogSevere && utilLogWarn && utilLogInfo)) {
                        if (rec.contains("loglevel=E")) {

                            utilLogSevere = true;
                            assertTrue("MessageId not null for util logger - severe level", rec.contains("messageId=null"));
                            Log.info(c, testName, "----> MessageID null for util logger - severe level");
                            assertTrue("whether logLevelRaw has complete word for Error", rec.contains("loglevel=ERROR") || rec.contains("logLevelRaw=SEVERE"));
                        } else if (rec.contains("loglevel=W")) {

                            utilLogWarn = true;
                            assertTrue("MessageId not null for util logger - warning level", rec.contains("messageId=null"));
                            Log.info(c, testName, "----> MessageID null for util logger - warning level");
                            assertTrue("whether logLevelRaw has complete word for WARNING", rec.contains("loglevel=WARNING"));
                        } else if (rec.contains("loglevel=I")) {

                            utilLogInfo = true;
                            assertTrue("MessageId not null for util logger - info level", rec.contains("messageId=null"));
                            Log.info(c, testName, "----> MessageID null for util logger - info level");
                            assertTrue("whether logLevelRaw has complete word for INFO", rec.contains("logevel=INFO"));
                        }
                    }
                } else {
                    Iterator<String> lvls = levels.iterator();
                    while (lvls.hasNext()) {
                        String level = lvls.next();
                        if (rec.contains(level)) {
                            Log.info(c, testName, "----> Found level : " + level);
                            lvls.remove();
                            if (level.contains("loglevel=I")) {
                                info = true;
                            } else if (level.contains("loglevel=A")) {
                                audit = true;
                            }
                        }
                    }

                    lvls = rawLevels.iterator();
                    while (lvls.hasNext()) {
                        String level = lvls.next();
                        Log.info(c, testName, "----> checking whether raw logger level found or not for the rec(" + rec + ")?: " + level + "::" + rec.contains(level));
                        if (rec.contains(level)) {
                            Log.info(c, testName, "----> Found raw loggerlevel : " + level);
                            lvls.remove();
                            if (level.contains("loglevel=INFO")) {
                                raw_info = true;
                            } else if (level.contains("loglevel=AUDIT")) {
                                raw_audit = true;
                            }
                        }
                    }

                }
                allLevels = levels.isEmpty();

                if (!allLevels) {
                    allLevels = (info && audit);
                }
                rawAllLevels = rawLevels.isEmpty();
                if (!rawAllLevels) {
                    rawAllLevels = (raw_info && raw_audit);
                }
                if (checkMsgID && utilLogInfo && utilLogWarn && utilLogSevere && sysOut && sysErr && allLevels) {
                    break;
                }
            }
        } else {
            fail("No message events received!");
        }

        assertTrue("Could not find some of the log levels", allLevels);

        assertTrue("whether all types of loggerName(rawLoggerName) values have been found ?", rawAllLevels);
        Log.info(c, testName, "********** Message Events received as expected ***********");
    }

    @Test
    public void testSampleSourceHandler() throws Exception {
        testName = "testSampleSourceHandler";

        try {
            server.setMarkToEndOfLog();

            server.waitForStringInTrace("Adding event: 2", 30000);
            server.waitForStringInTrace("Received event: 2", 30000);

            List<String> lines = server.findStringsInFileInLibertyServerRoot("Adding event:", TRACE_LOG);

            assertFalse("No events were added", (lines.size() == -1));
            Log.info(c, testName, "Number of events added ---> " + lines.size());
            //Looking for added events.
            int[] foundEvents = { 1, 1, 1, 1 };

            for (String line : lines) {
                if (line.contains("Adding event: 1")) {
                    foundEvents[0] = 0;
                } else if (line.contains("Adding event: 2")) {
                    foundEvents[1] = 0;

                }
                Log.info(c, testName, "Looking for Added Events ---> " + line);
            }

            //Looking for received events.

            lines = server.findStringsInFileInLibertyServerRoot("Received event:", TRACE_LOG);
            assertFalse("No events were received", (lines.size() == -1));
            Log.info(c, testName, "Number of events received ---> " + lines.size());
            for (String line : lines) {
                if (line.contains("Received event: 1")) {
                    foundEvents[2] = 0;
                } else if (line.contains("Received event: 2")) {
                    foundEvents[3] = 0;
                }
                Log.info(c, testName, "Looking for Received Events ---> " + line);
            }

            int count = 0;
            for (int i = 0; i <= 3; i++) {
                if (foundEvents[i] == 0) {
                    count++;
                }
            }
            assertTrue("Not all the events are received", (count == 4));
            Log.info(c, testName, "---> Found all the added and received events..");

            Log.info(c, testName, "***** All the added events by source are received by handler as expected. *****");
        } catch (Exception e) {

        }
    }

    @Test
    public void testTraceSourceForLibertyLogging() throws Exception {
        final String testName = testName2.getMethodName(); // "testTraceSourceForLibertyLogging";
        final long TWENTY_SECONDS = 20 * 1000;
        final String REST_TRACELOGGER_URL = "https://" + traceServer.getHostname() + ":" + traceServer.getHttpSecondarySecurePort() + "/ibm/api/test/tracelogger";
        String messageKey;
        Log.info(c, testName, "Inside Test method : " + testName);

        // ***** Wait for Server to completely start
        traceServer.waitForStringInLog("CWWKF0011I:"); // wait for TraceSourceHandlerServer to be ready
        traceServer.waitForStringInLog("JUST-WAIT-WILL-NOT-FIND-STRING", TWENTY_SECONDS);

//            // ***** Check if the TraceSource has been activated
//            lines = server.findStringsInFileInLibertyServerRoot("Activating com.ibm.ws.logging.source.TraceSource", TRACE_LOG);
//            assertTrue("TraceSource service NOT activated", !lines.isEmpty());
//            Log.info(c, testName, "****** TraceSource service is activated ");

        // ***** Wait for TraceHandler to activate
        assertTrue("TraceHandler NOT activated",
                   traceServer.waitForStringInLog("Activating trace.source.handler.TraceHandlerImpl",
                                                  traceServer.getFileFromLibertyServerRoot(TRACEHANDLERIMPL_LOG)) != null);
        // ***** Wait for buffer manager to set
        assertTrue("BufferManager has not set in timely fasion",
                   traceServer.waitForStringInLog("Setting buffer manager trace.source.handler.TraceHandlerImpl",
                                                  traceServer.getFileFromLibertyServerRoot(TRACEHANDLERIMPL_LOG)) != null);

        // ***** Call Mock REST URL, and write trace logs *****
        messageKey = testName + "_" + System.currentTimeMillis();
        String path = REST_TRACELOGGER_URL + "?messageKey=" + messageKey;
        Log.info(c, testName, "path=" + path + "::User=" + USER + "::password=" + USERPWD);
        String response = HandlerTestHelper.getHttpResponseAsString(path, USER, USERPWD);
        Log.info(c, testName, path + " - Response:" + response);

        // ***** Look for logs in trace.log
        HandlerTestHelper.assertFoundAllLogs("%s log NOT found in " + TRACE_LOG,
                                             HandlerTestHelper.findAllLogsFromRESTHandlerTraceLogger(messageKey, traceServer, TRACE_LOG));

        // ***** Look for logs in tracehandlerimpl.log
        HandlerTestHelper.assertFoundTraceLogsOnly("%s log NOT received in TraceHandler (" + TRACEHANDLERIMPL_LOG + ")",
                                                   "%s log received in TraceHandler (" + TRACEHANDLERIMPL_LOG + ")",
                                                   HandlerTestHelper.findAllLogsFromRESTHandlerTraceLogger(messageKey, traceServer, TRACEHANDLERIMPL_LOG));

    }

    @Test
    public void testTraceSourceForJULogging() throws Exception {
        final String testName = testName2.getMethodName(); // "testTraceSourceForJULogging";
        final long TWENTY_SECONDS = 20 * 1000;
        final String TESTAPP_JULLOGGER_URL = "http://" + traceServer.getHostname() + ":" + traceServer.getHttpSecondaryPort() + "/TestApp/jullogger";
        String messageKey;
        Log.info(c, testName, "Inside Test method : " + testName);

        // ***** Wait for Server to completely start
        traceServer.waitForStringInLog("CWWKF0011I:"); // wait for TraceSourceHandlerServer to be ready
        traceServer.waitForStringInLog("JUST-WAIT-WILL-NOT-FIND-STRING", TWENTY_SECONDS);

//        // ***** Check if the TraceSource has been activated
//        lines = traceServer.findStringsInFileInLibertyServerRoot("Activating com.ibm.ws.logging.source.TraceSource", TRACE_LOG);
//        assertTrue("TraceSource service NOT activated", !lines.isEmpty());
//        Log.info(c, testName, "****** TraceSource service is activated ");

        // ***** Wait for TraceHandler to activate
        assertTrue("TraceHandler NOT activated",
                   traceServer.waitForStringInLog("Activating trace.source.handler.TraceHandlerImpl",
                                                  traceServer.getFileFromLibertyServerRoot(TRACEHANDLERIMPL_LOG)) != null);
        // ***** Wait for buffer manager to set
        assertTrue("BufferManager has not set in timely fasion",
                   traceServer.waitForStringInLog("Setting buffer manager trace.source.handler.TraceHandlerImpl",
                                                  traceServer.getFileFromLibertyServerRoot(TRACEHANDLERIMPL_LOG)) != null);

        // ***** Call jul.jsp, to write logs using java.util.logging *****
        Log.info(c, testName, "*** Call jul.jsp to generate logs ");
        messageKey = testName + "_" + System.currentTimeMillis();
        String path = TESTAPP_JULLOGGER_URL + "?messageKey=" + messageKey;
        String response = HttpUtils.getHttpResponseAsString(path);
        Log.info(c, testName, path + " - Response:" + response);

        // ***** Look for logs in trace.log
        HandlerTestHelper.assertFoundAllLogs("%s log NOT found in " + TRACE_LOG,
                                             HandlerTestHelper.findAllLogsForTestAppJUL_jsp(traceServer, messageKey, TRACE_LOG));

        // ***** Look for logs in tracehandlerimpl.log
        HandlerTestHelper.assertFoundTraceLogsOnly("%s log NOT received in TraceHandler (" + TRACEHANDLERIMPL_LOG + ")",
                                                   "%s log received in TraceHandler (" + TRACEHANDLERIMPL_LOG + ")",
                                                   HandlerTestHelper.findAllLogsForTestAppJUL_jsp(traceServer, messageKey, TRACEHANDLERIMPL_LOG));

        HandlerTestHelper.verifyLoglevelRawInTraceLogger(messageKey, traceServer, TRACEHANDLERIMPL_LOG);
    }

    // com.ibm._jsp._exp$Exception* are available in exp.jsp in testwebapp.war
    @Test
    @AllowedFFDC({ "java.lang.RuntimeException",
                   "com.ibm._jsp._exp$ExceptionOne",
                   "com.ibm._jsp._exp$ExceptionTwo",
                   "com.ibm._jsp._exp$ExceptionThree",
                   "com.ibm._jsp._exp$ExceptionFour"
    })
    public void testFFDCSource() throws Exception {

        testName = "testFFDCSource";
        List<String> lines;
        String errorMessage;
        String path;
        String seqenceNum;

        lock.await(10, TimeUnit.SECONDS); // wait for sometime for server to start
        server.waitForStringInLog("CWWKF0011I:"); // wait for SampleSourceHandlerServer to be ready

        lines = server.findStringsInFileInLibertyServerRoot("Activating com.ibm.ws.logging.ffdc.source.FFDCSource", TRACE_LOG);
        assertTrue("FFDCSource service NOT activated", !lines.isEmpty());
        Log.info(c, testName, "****** FFDCSource service is activated ");

        // ***** TEST WITH RUNTIME EXCEPTION *****
        Log.info(c, testName, "*** Call exp.jsp with RuntimeException to generate FFDC ");
        // Create a RuntimeException, and generate a FFDC
        errorMessage = testName + "_error_message_" + System.currentTimeMillis();
        path = "/TestApp/exp.jsp?message=" + errorMessage;
        seqenceNum = "[0-9]+_0000000000001";
        // Call the exp.jsp in testwebapp, to throw RuntimeException
        HandlerTestHelper.callAndVerifyFFDCEvent(c, testName, server, path, errorMessage, seqenceNum);

        // ***** TEST WITH CUSTOM EXCEPTION ONE *****
        Log.info(c, testName, "*** Call exp.jsp with Custom ExceptionOne to generate FFDC ");
        // Create a RuntimeException, and generate a FFDC
        errorMessage = testName + "_error_message_" + System.currentTimeMillis();
        path = "/TestApp/exp.jsp?ExceptionOne=true&message=" + errorMessage;
        seqenceNum = "[0-9]+_0000000000002";
        // Call the exp.jsp in testwebapp, to throw RuntimeException
        HandlerTestHelper.callAndVerifyFFDCEvent(c, testName, server, path, errorMessage, seqenceNum);

        Log.info(c, testName, "*************** End of testFFDCSource ***************");
    }

    @Test
    public void testAccessLogSource() throws Exception {
        final String testName = testName2.getMethodName(); // "testAccessLogSource";
        final long FIVE_SECONDS = 5 * 1000;
        final long TWENTY_SECONDS = 20 * 1000;
        final long THIRTY_SECONDS = 30 * 1000;
        final String TESTAPP_JULLOGGER_URL = "http://" + traceServer.getHostname() + ":" + traceServer.getHttpSecondaryPort() + "/TestApp/jullogger";
        final String REST_TRACELOGGER_URL = "https://" + traceServer.getHostname() + ":" + traceServer.getHttpSecondarySecurePort() + "/ibm/api/test/tracelogger";
        final String queryString;
        List<String> lines;
        String messageKey;
        String path;
        String response;
        Log.info(c, testName, "Inside Test method : " + testName);

        // ***** Wait for Server to completely start
        traceServer.waitForStringInLog("JUST-WAIT-WILL-NOT-FIND-STRING", TWENTY_SECONDS);
        assertTrue("TraceSourceHandlerServer NOT completely started",
                   traceServer.waitForStringInLog("CWWKF0011I:") != null); // wait for TraceSourceHandlerServer to be ready

        // ***** Wait for TraceHandler to activate
        // assertTrue("AccessLogHandler NOT activated",
        //           traceServer.waitForStringInTrace("Activating accesslog.source.handler.AccessLogHandlerImpl") != null);

        messageKey = testName + "_" + System.currentTimeMillis();
        queryString = "messageKey=" + messageKey + "&paramWithoutValue=" + testName;

        final String queryString0;

        // ***** (1) Call Jullogger URL *****
        Log.info(c, testName, "*** Call jullogger url");
        path = TESTAPP_JULLOGGER_URL + "?" + queryString;
        response = HttpUtils.getHttpResponseAsString(path);
        Log.info(c, testName, path + " - Response:" + response);

        // ***** (2) Call Mock REST URL, and write trace logs *****
        path = REST_TRACELOGGER_URL + "?" + queryString;
        response = HandlerTestHelper.getHttpResponseAsString(path, USER, USERPWD);
        Log.info(c, testName, path + " - Response:" + response);

        // ***** Wait for few seconds, before reading logs files
        traceServer.waitForStringInLog("JUST-WAIT-WILL-NOT-FIND-STRING", TWENTY_SECONDS);

        // find log with messageKey in ACCESS_LOG_1, after event log from default logger.
        lines = traceServer.findStringsInFileInLibertyServerRoot(messageKey, ACCESS_LOG_1);
        Assert.assertEquals("Expected no of lines with messageKey(" + messageKey + ") not found in " + ACCESS_LOG_1, 2, lines.size());

        lines = traceServer.findStringsInTrace("Received AccessLog event.*" + messageKey);
        Assert.assertEquals("All Accesslog events received with messageKey(" + messageKey + ") not received", 2, lines.size());

        // ***** verify
        String firstLine = lines.get(1);
        Map<String, String> expectedFieldAndValues = new HashMap<String, String>() {
            {
                // check field and value
                put("ibm_uriPath", "/ibm/api/test/tracelogger");
                put("ibm_requestMethod", "GET");
                put("ibm_responseCode", "200");
                put("ibm_queryString", queryString);
                put("ibm_requestPort", String.valueOf(traceServer.getHttpSecondarySecurePort()));
            }
        };
        assertAccessLogDataIsValid(firstLine, expectedFieldAndValues);
    }

    @Test
    @AllowedFFDC({ "java.lang.RuntimeException",
                   "com.ibm._jsp._exp$ExceptionOne",
                   "com.ibm._jsp._exp$ExceptionTwo",
                   "com.ibm._jsp._exp$ExceptionThree",
                   "com.ibm._jsp._exp$ExceptionFour" })
    public void testDuplicateFFDC() throws Exception {
        testName = "testDuplicateFFDC";

        List<String> lines;
        List<String> lines2;
        //Checking for Server Initialization Message
        assertNotNull("FAIL: Did not receive CWWKF0011I ready to run a smarter planet message",
                      server.waitForStringInLog("CWWKF0011I"));

        //Checking for Application Installation Message
        assertNotNull("FAIL: Did not receive CWWKT0016I Web application available",
                      server.waitForStringInLog("CWWKT0016I"));

        //Checking for the FFDC Source

        assertNotNull("FAIL: Did not receive \"Activating com.ibm.ws.logging.ffdc.source.FFDCSource\" in trace.log",
                      server.waitForStringInTrace("Activating com.ibm.ws.logging.ffdc.source.FFDCSource"));
        Log.info(c, testName, "DuplicateFFDC: FFDCSource service is activated ");
        //URL call
        String msgString = "duplicateFFDC" + "_WAS_LIBERTY_" + System.currentTimeMillis();
        url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestApp/exp.jsp?ExceptionThree=true&message=" + msgString);

        HttpURLConnection conct = getHttpConnection(url);
        conct.connect();

        //Checks the Trace.log for the first FFDC
        Log.info(c, testName, "DuplicateFFDC: Connected: " + url);
        Log.info(c, testName, "DuplicateFFDC: " + conct.getResponseCode());
        String waitMsg = server.waitForStringInTraceUsingMark("message=" + msgString);
        Log.info(c, testName, "DuplicateFFDC: Waiting for the message " + waitMsg + " in trace ");
        lines = server.findStringsInFileInLibertyServerRoot("message=" + msgString, TRACE_LOG);
        Log.info(c, testName, "DuplicateFFDC: " + lines);
        assertTrue(msgString, !lines.isEmpty());
        Log.info(c, testName, "DuplicateFFDC: " + msgString + " found");

        //Calling the url Again
        String msgString2 = "duplicateFFDC" + "_WAS_LIBERTY_" + System.currentTimeMillis();
        URL url2 = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestApp/exp.jsp?ExceptionThree=true&message=" + msgString2);
        HttpURLConnection conct2 = getHttpConnection(url2);
        conct2.connect();

        //Checks the Trace.log for the duplicate FFDC
        Log.info(c, testName, "DuplicateFFDC: Connected: " + url2);
        Log.info(c, testName, "DuplicateFFDC: " + conct.getResponseCode());
        //Message never appears. So,including timeout to avoid long wait.
        waitMsg = server.waitForStringInTraceUsingMark("message=" + msgString2, 20000);
        Log.info(c, testName, "DuplicateFFDC: Waiting for the message " + waitMsg + " in trace ");
        lines2 = server.findStringsInFileInLibertyServerRoot("message=" + msgString2, TRACE_LOG);
        Log.info(c, testName, "DuplicateFFDC:" + lines2);
        assertTrue(msgString2, lines2.isEmpty());
        Log.info(c, testName, testName + " Verified");
        Log.info(c, testName, "DuplicateFFDC: " + msgString2 + " not found");
    }

    /**
     * Asserts the field names and values provided in expectedFieldAndValues matches to toString output.
     *
     * @param line                   The line which the toString() output of AccessLogData will be parsed
     * @param expectedFieldAndValues Matches the fieldname and values, if no value is provided, only fieldname will be matched.
     */
    private void assertAccessLogDataIsValid(String line, Map<String, String> expectedFieldsAndValues) {
        // Received AccessLog event: AccessLogData [uriPath=/ibm/api/test/tracelogger, requestMethod=GET, remoteHost=127.0.0.1, userAgent=Java/1.7.0, requestProtocol=HTTP/1.1, responseSize=27, responseCode=200, elapsedTime=148067, queryString=messageKey=testAccessLogSource_1453114767723, requestStartTime=33636731463302, sequence=33636731463302_0000000000002]
        // 2020-05-11 - ibm_requestStartTime was never printed to JSON logs but would be seen in trace. It has been moved around to require the option jsonAccessLogFields=logFormat to print to JSON logs. Tests are in CustomAccessLogFieldsTest.java
        String[] expectedFieldsWithNonNullValue = new String[] { "ibm_uriPath", "ibm_requestMethod", "ibm_responseCode",
                                                                 "ibm_queryString", "ibm_requestHost", "ibm_requestPort",
                                                                 "ibm_remoteHost", "ibm_userAgent", "ibm_requestProtocol",
                                                                 "ibm_bytesReceived", "ibm_responseCode", "ibm_elapsedTime" };
        String startTag = "GenericData [";
        int start = line.indexOf(startTag);
        assertTrue("AccessLogData not found", start != -1);
        int end = line.indexOf(']', start);

        String strData = line.substring(start + startTag.length(), end);
        String[] splitArray = strData.split(",");

        // parse all fields & values
        Map<String, String> data = new HashMap<String, String>();
        for (String fieldAndValue : splitArray) {
            Log.info(c, "assertAccessLogDataIsValid", "fieldAndValue:" + fieldAndValue);
            int idx = fieldAndValue.indexOf('=');
            String key = fieldAndValue.substring(0, idx).trim();
            String value = fieldAndValue.substring(idx + 1);
            Log.info(c, "assertAccessLogDataIsValid", "key=" + key + " value=" + value);
            if (value.equals("null")) {
                value = null;
            }
            Log.info(c, "assertAccessLogDataIsValid", " value=" + value);
            data.put(key, value);
        }

        // fields with non-null value
        for (String fieldName : expectedFieldsWithNonNullValue) {
            assertTrue(fieldName + " field not found", data.containsKey(fieldName));
            String actualValue = data.get(fieldName);
            Assert.assertNotNull(fieldName + " value does not match.", actualValue);
        }

        // match field & values
        for (Entry<String, String> entry : expectedFieldsAndValues.entrySet()) {
            String fieldName = entry.getKey();
            String expectedValue = entry.getValue();

            assertTrue(fieldName + " field not found", data.containsKey(fieldName));
            String actualValue = data.get(fieldName);
            Assert.assertEquals(fieldName + " value does not match.", expectedValue, actualValue);
        }

        assertTrue("Sequence field not found", data.containsKey("ibm_sequence"));
        assertTrue("Seqence does not match the required patteren", data.get("ibm_sequence").matches("[0-9]+_[0-9]{13}"));

        // check time values are in micro-secs, and should be ideally between few mins range.
        long expectedStartTimeRange = System.currentTimeMillis() - (2 * 60 * 1000); // start less 2mins
        long expectedEndTimeRange = System.currentTimeMillis();

        long timestamp = Long.parseLong(data.get("ibm_datetime"));
        assertTrue("Timestamp is not in vaild time range."
                   + "( timestamp=" + timestamp
                   + ", expectedStartTimeRange=" + expectedStartTimeRange
                   + ", expectedEndTimeRange=" + expectedEndTimeRange + " ) ", (timestamp > expectedStartTimeRange && timestamp < expectedEndTimeRange));
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

    @After
    public void tearDown() {
        try {
            if (server != null && server.isStarted()) {
                server.stopServer();
            }
        } catch (Exception e) {
        }
        try {
            if (MsgServer != null && MsgServer.isStarted()) {
                MsgServer.stopServer();
            }
        } catch (Exception e) {
        }
        try {
            if (traceServer != null && traceServer.isStarted()) {
                traceServer.stopServer();
            }
        } catch (Exception e) {
        }
    }

    @AfterClass
    public static void completeTest() {
        try {
            server.stopServer();
        } catch (Exception e) {
        }
        try {
            MsgServer.stopServer();
        } catch (Exception e1) {
        }
        try {
            traceServer.stopServer();
        } catch (Exception e) {
        }
    }
}
