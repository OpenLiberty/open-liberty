/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class LogServiceTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("LogServiceServer");

    private static final String KEY_LEVEL = "level";
    private static final String KEY_MSG = "msg";
    private static final String KEY_THROW = "throw";
    private static final String KEY_SERVICE = "service";
    private static final String KEY_EVENT = "event";

    private static final String LEVEL_TRACE = "TRACE";
    private static final String LEVEL_DEBUG = "DEBUG";
    private static final String LEVEL_INFO = "INFO";
    private static final String LEVEL_WARN = "WARN";
    private static final String LEVEL_ERROR = "ERROR";
    private static final String LEVEL_AUDIT = "AUDIT";

    private static final String MSG_AUDIT = "testAuditMessage";
    private static final String MSG_ERROR = "testErrorMessage";
    private static final String MSG_WARN = "testWarnMessage";
    private static final String MSG_INFO = "testInfoMessage";
    private static final String MSG_DEBUG = "testDebugMessage";
    private static final String MSG_TRACE = "testTraceMessage";

    private static final String THROW_AUDIT = "throwAuditMessage";
    private static final String THROW_ERROR = "throwErrorMessage";
    private static final String THROW_WARN = "throwWarnMessage";
    private static final String THROW_INFO = "throwInfoMessage";
    private static final String THROW_DEBUG = "throwDebugMessage";
    private static final String THROW_TRACE = "throwTraceMessage";

    private static final String EVENT_BUNDLE = "bundle";
    private static final String EVENT_SERVICE = "service";
    private static final String EVENT_FRAMEWORK = "framework";

    private static final String LOGSERVICE_TESTER_BUNDLE_JAR = "logservice.tester";
    private static final String LOGSERVICE_TEST_FEATURE = "logServiceTest-1.0";

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void startTest() throws Exception {
        server.installSystemBundle(LOGSERVICE_TESTER_BUNDLE_JAR);
        server.installSystemFeature(LOGSERVICE_TEST_FEATURE);
        server.startServer();
        // Since a WAB is used we need to wait for WAB context root to be ready
        server.waitForStringInLog("CWWKT0016I");
        // force initialization of the LogTester servlet
        log("startTest", LEVEL_AUDIT, null, false);
    }

    @Before
    public void resetLogMark() throws Exception {
        server.setMarkToEndOfLog();
        server.setMarkToEndOfLog(server.getConsoleLogFile());
        server.setTraceMarkToEndOfDefaultTrace();
    }

    @AfterClass
    public static void endTest() throws Exception {
        server.stopServer("CWWKE0700W", "CWWKE0701E");
        server.uninstallSystemBundle(LOGSERVICE_TESTER_BUNDLE_JAR);
        server.uninstallSystemFeature(LOGSERVICE_TEST_FEATURE);
    }

    private void setTraceSpecification(String traceSpecification) throws Exception {
        ServerConfiguration sc = server.getServerConfiguration();
        if (traceSpecification == null) {
            traceSpecification = "osgilogging=debug";
        } else {
            traceSpecification = "osgilogging=debug:" + traceSpecification;
        }
        sc.getLogging().setTraceSpecification(traceSpecification);
        server.updateServerConfiguration(sc);
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    private static void event(String type) throws IOException {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://").append(server.getHostname()).append(':').append(server.getHttpDefaultPort());
        urlBuilder.append("/logServiceTester/log?" + KEY_EVENT + "=" + type);
        URL url = new URL(urlBuilder.toString());
        String resp = HttpUtils.getHttpResponseAsString(url);
        assertTrue("Unexpected resp: " + resp, resp.contains("DONE"));
    }

    private static void log(String msg, String level, String throwMsg, boolean includeService) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        if (msg != null) {
            params.put(KEY_MSG, msg);
        }
        if (level != null) {
            params.put(KEY_LEVEL, level);
        }
        if (throwMsg != null) {
            params.put(KEY_THROW, throwMsg);
        }
        if (includeService) {
            params.put(KEY_SERVICE, String.valueOf(includeService));
        }
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://").append(server.getHostname()).append(':').append(server.getHttpDefaultPort());
        urlBuilder.append("/logServiceTester/log");
        char delim = '?';
        for (Entry<String, String> param : params.entrySet()) {
            urlBuilder.append(delim);
            urlBuilder.append(param.getKey()).append('=').append(param.getValue());
            delim = '&';
        }
        URL url = new URL(urlBuilder.toString());
        String resp = HttpUtils.getHttpResponseAsString(url);
        assertTrue("Unexpected resp: " + resp, resp.contains("DONE"));
    }

    @Test
    public void testTrace() throws Exception {
        // we turn all logservice and should see all logs
        setTraceSpecification("logservice=all");

        log(MSG_TRACE, LEVEL_TRACE, null, false);
        log(MSG_DEBUG, LEVEL_DEBUG, null, false);
        log(MSG_INFO, LEVEL_INFO, null, false);
        log(MSG_WARN, LEVEL_WARN, null, false);
        log(MSG_ERROR, LEVEL_ERROR, null, false);
        log(MSG_AUDIT, LEVEL_AUDIT, null, false);

        // find the expected audit message first, so we don't have to wait for others
        assertByWaitingForExpectMessage(MSG_AUDIT);
        assertExpectMessage(MSG_ERROR, true);
        assertExpectMessage(MSG_WARN, true);
        assertExpectMessage(MSG_INFO, false);
        assertExpectMessage(MSG_DEBUG, false);
        assertExpectMessage(MSG_TRACE, false);
    }

    @Test
    public void testDebug() throws Exception {
        // we turn all logservice and should see all logs
        setTraceSpecification("logservice=debug");

        log(MSG_TRACE, LEVEL_TRACE, null, false);
        log(MSG_DEBUG, LEVEL_DEBUG, null, false);
        log(MSG_INFO, LEVEL_INFO, null, false);
        log(MSG_WARN, LEVEL_WARN, null, false);
        log(MSG_ERROR, LEVEL_ERROR, null, false);
        log(MSG_AUDIT, LEVEL_AUDIT, null, false);

        // find the expected audit message first, so we don't have to wait for others
        assertByWaitingForExpectMessage(MSG_AUDIT);
        assertExpectMessage(MSG_ERROR, true);
        assertExpectMessage(MSG_WARN, true);
        assertExpectMessage(MSG_INFO, false);
        assertExpectMessage(MSG_DEBUG, false);
        assertNotExpectMessage(MSG_TRACE);
    }

    @Test
    public void testInfo() throws Exception {
        // we turn all logservice and should see all logs
        setTraceSpecification("logservice=info");

        log(MSG_TRACE, LEVEL_TRACE, null, false);
        log(MSG_DEBUG, LEVEL_DEBUG, null, false);
        log(MSG_INFO, LEVEL_INFO, null, false);
        log(MSG_WARN, LEVEL_WARN, null, false);
        log(MSG_ERROR, LEVEL_ERROR, null, false);
        log(MSG_AUDIT, LEVEL_AUDIT, null, false);

        // find the expected audit message first, so we don't have to wait for others
        assertByWaitingForExpectMessage(MSG_AUDIT);
        assertExpectMessage(MSG_ERROR, true);
        assertExpectMessage(MSG_WARN, true);
        assertExpectMessage(MSG_INFO, false);
        assertNotExpectMessage(MSG_DEBUG);
        assertNotExpectMessage(MSG_TRACE);
    }

    @Test
    public void testWarn() throws Exception {
        // we turn all logservice and should see all logs
        setTraceSpecification("logservice=warning");

        log(MSG_TRACE, LEVEL_TRACE, null, false);
        log(MSG_DEBUG, LEVEL_DEBUG, null, false);
        log(MSG_INFO, LEVEL_INFO, null, false);
        log(MSG_WARN, LEVEL_WARN, null, false);
        log(MSG_ERROR, LEVEL_ERROR, null, false);
        log(MSG_AUDIT, LEVEL_AUDIT, null, false);

        // find the expected audit message first, so we don't have to wait for others
        assertByWaitingForExpectMessage(MSG_AUDIT);
        assertExpectMessage(MSG_ERROR, true);
        assertExpectMessage(MSG_WARN, true);
        assertNotExpectMessage(MSG_INFO);
        assertNotExpectMessage(MSG_DEBUG);
        assertNotExpectMessage(MSG_TRACE);
    }

    @Test
    public void testError() throws Exception {
        // we turn all logservice and should see all logs
        setTraceSpecification("logservice=error");

        log(MSG_TRACE, LEVEL_TRACE, null, false);
        log(MSG_DEBUG, LEVEL_DEBUG, null, false);
        log(MSG_INFO, LEVEL_INFO, null, false);
        log(MSG_WARN, LEVEL_WARN, null, false);
        log(MSG_ERROR, LEVEL_ERROR, null, false);
        log(MSG_AUDIT, LEVEL_AUDIT, null, false);

        // find the expected audit message first, so we don't have to wait for others
        assertByWaitingForExpectMessage(MSG_AUDIT);
        assertExpectMessage(MSG_ERROR, true);
        assertNotExpectMessage(MSG_WARN);
        assertNotExpectMessage(MSG_INFO);
        assertNotExpectMessage(MSG_DEBUG);
        assertNotExpectMessage(MSG_TRACE);
    }

    @Test
    public void testAudit() throws Exception {
        // we turn off logservice, but that cannot prevent OSGI audit messages
        setTraceSpecification("logservice=off");

        log(MSG_TRACE, LEVEL_TRACE, null, false);
        log(MSG_DEBUG, LEVEL_DEBUG, null, false);
        log(MSG_INFO, LEVEL_INFO, null, false);
        log(MSG_WARN, LEVEL_WARN, null, false);
        log(MSG_ERROR, LEVEL_ERROR, null, false);
        log(MSG_AUDIT, LEVEL_AUDIT, null, false);

        // find the expected audit message first, so we don't have to wait for others
        assertByWaitingForExpectMessage(MSG_AUDIT);
        assertNotExpectMessage(MSG_ERROR);
        assertNotExpectMessage(MSG_WARN);
        assertNotExpectMessage(MSG_INFO);
        assertNotExpectMessage(MSG_DEBUG);
        assertNotExpectMessage(MSG_TRACE);
    }

    @Test
    public void testThrowableLog() throws Exception {
        // we turn all logservice and should see all logs
        setTraceSpecification("logservice=all");
        log(MSG_TRACE, LEVEL_TRACE, THROW_TRACE, false);
        log(MSG_DEBUG, LEVEL_DEBUG, THROW_DEBUG, false);
        log(MSG_INFO, LEVEL_INFO, THROW_INFO, false);
        log(MSG_WARN, LEVEL_WARN, THROW_WARN, false);
        log(MSG_ERROR, LEVEL_ERROR, THROW_ERROR, false);
        log(MSG_AUDIT, LEVEL_AUDIT, THROW_AUDIT, false);

        // find the expected audit message first, so we don't have to wait for others
        assertByWaitingForExpectMessage(MSG_AUDIT);
        assertExpectMessage(MSG_ERROR, true);
        assertExpectMessage(MSG_WARN, true);
        assertExpectMessage(MSG_INFO, false);
        assertExpectMessage(MSG_DEBUG, false);
        assertExpectMessage(MSG_TRACE, false);
        // check for throw message also
        assertExpectMessage("RuntimeException: " + THROW_ERROR, true);
        assertExpectMessage("RuntimeException: " + THROW_WARN, true);
        assertExpectMessage("RuntimeException: " + THROW_INFO, false);
        assertExpectMessage("RuntimeException: " + THROW_DEBUG, false);
        assertExpectMessage("RuntimeException: " + THROW_TRACE, false);
    }

    @Test
    public void testEventLogServiceAll() throws Exception {
        setTraceSpecification("logservice=all");
        event(EVENT_BUNDLE);
        assertBundleEvents("bundle.1", "bundle.2");

        resetLogMark();
        event(EVENT_SERVICE);
        assertServiceEvents("bundle.1", "bundle.2");

        resetLogMark();
        event(EVENT_FRAMEWORK);
        assertFrameworkEvents(true);
    }

    @Test
    public void testEventOSGiEventsAll() throws Exception {
        setTraceSpecification("OSGi.Events=all");
        event(EVENT_BUNDLE);
        assertBundleEvents("bundle.1", "bundle.2");

        resetLogMark();
        event(EVENT_SERVICE);
        assertServiceEvents("bundle.1", "bundle.2");

        resetLogMark();
        event(EVENT_FRAMEWORK);
        assertFrameworkEvents(true);
    }

    @Test
    public void testEventOSGiEventsOff() throws Exception {
        setTraceSpecification("OSGi.Events=off");
        event(EVENT_BUNDLE);
        assertBundleEvents();

        resetLogMark();
        event(EVENT_SERVICE);
        assertServiceEvents();

        resetLogMark();
        event(EVENT_FRAMEWORK);
        assertFrameworkEvents(false);
    }

    @Test
    public void testEventBundleScoped() throws Exception {
        setTraceSpecification("bundle.1=all");
        event(EVENT_BUNDLE);
        assertBundleEvents("bundle.1");
    }

    @Test
    public void testEventBundleOff() throws Exception {
        // info level should not capture event logs
        setTraceSpecification("bundle.1=info");
        event(EVENT_BUNDLE);
        assertBundleEvents();
    }

    @Test
    public void testEventServiceScoped() throws Exception {
        setTraceSpecification("bundle.1=all");
        event(EVENT_SERVICE);
        assertServiceEvents("bundle.1");
    }

    @Test
    public void testEventServiceOff() throws Exception {
        // info level should not capture event logs
        setTraceSpecification("bundle.1=info");
        event(EVENT_SERVICE);
        assertServiceEvents();
    }

    private void assertBundleEvents(String... expectedBundles) throws Exception {
        flushLog("finalBundleEventsMsg");

        checkForTraceMessage("BundleEvent STOPPING", expectedBundles.length);
        checkForTraceMessage("BundleEvent STOPPED", expectedBundles.length);
        checkForTraceMessage("BundleEvent STARTING", expectedBundles.length);
        checkForTraceMessage("BundleEvent STARTED", expectedBundles.length);
        checkForTraceMessage("LoggerName:Events.Bundle", expectedBundles.length * 4);
        for (String bundle : expectedBundles) {
            checkForTraceMessage("Event:org.osgi.framework.BundleEvent.*" + bundle, 4);
        }
    }

    private void assertServiceEvents(String... expectedBundles) throws Exception {
        flushLog("finalServiceEventsMsg");

        checkForTraceMessage("ServiceEvent REGISTERED", expectedBundles.length);
        checkForTraceMessage("ServiceEvent UNREGISTERING", expectedBundles.length);
        checkForTraceMessage("LoggerName:Events.Service", expectedBundles.length * 2);
        checkForTraceMessage("ServiceRef:", expectedBundles.length * 2);
        checkForTraceMessage("Event:org.osgi.framework.ServiceEvent", expectedBundles.length * 2);
    }

    /** Checks if the logs appear in trace **/
    private void assertFrameworkEvents(boolean expected) throws Exception {
        flushLog("finalFrameworkEventsMsg");

        int num = expected ? 1 : 0;
        checkForTrace("FrameworkEvent PACKAGES REFRESHED", num);
        checkForTrace("LoggerName:Events.Framework", num);
        checkForTrace("Event:org.osgi.framework.FrameworkEvent", num);

    }

    private void checkForTraceMessage(String msg, int expectedNum) throws Exception {
        // make sure it is not in console log
        assertTrue("Found unexpected message in console log: " + msg, server.findStringsInLogsUsingMark(msg, server.getConsoleLogFile()).isEmpty());
        List<String> found = server.findStringsInLogsAndTraceUsingMark(msg);
        assertEquals("Did not find the expected number of messages: " + msg, expectedNum, found.size());
    }

    private void checkForTrace(String msg, int expectedNum) throws Exception {
        // make sure it is not in console log
        assertTrue("Found unexpected message in console log: " + msg, server.findStringsInLogsUsingMark(msg, server.getConsoleLogFile()).isEmpty());
        if (expectedNum > 0) {
            server.waitForStringInTraceUsingMark(msg); //wait for string in trace log
        }
        List<String> found = server.findStringsInLogsAndTraceUsingMark(msg);
        assertEquals("Did not find the expected number of messages: " + msg, expectedNum, found.size());
    }

    @Test
    public void testServiceTrace() throws Exception {
        doTestServiceReference(MSG_TRACE, LEVEL_TRACE, false);
    }

    @Test
    public void testServiceDebug() throws Exception {
        doTestServiceReference(MSG_DEBUG, LEVEL_DEBUG, false);
    }

    @Test
    public void testServiceInfo() throws Exception {
        doTestServiceReference(MSG_INFO, LEVEL_INFO, false);
    }

    @Test
    public void testServiceWarn() throws Exception {
        doTestServiceReference(MSG_WARN, LEVEL_WARN, true);
    }

    @Test
    public void testServiceError() throws Exception {
        doTestServiceReference(MSG_ERROR, LEVEL_ERROR, true);
    }

    @Test
    public void testServiceAudit() throws Exception {
        doTestServiceReference(MSG_AUDIT, LEVEL_AUDIT, true);
    }

    private void doTestServiceReference(String msg, String level, boolean consoleMsg) throws Exception {
        // we turn all logservice and should see all logs
        setTraceSpecification("logservice=all");
        log(msg, level, null, true);
        flushLog("finalMsg");

        assertExpectMessage(msg, consoleMsg);
        assertExpectMessage("ServiceRef:", consoleMsg);
    }

    private static void assertByWaitingForExpectMessage(String msg) {
        assertNotNull("Did not find expected message: " + msg, server.waitForStringInLogUsingMark(msg));
    }

    private static void assertExpectMessage(String msg, boolean consoleMsg) throws Exception {
        if (consoleMsg) {
            assertFalse("Did not find expected message in console log: " + msg, server.findStringsInLogsUsingMark(msg, server.getConsoleLogFile()).isEmpty());
        } else {
            // make sure it is not in console log
            assertTrue("Found unexpected message in console log: " + msg, server.findStringsInLogsUsingMark(msg, server.getConsoleLogFile()).isEmpty());
            assertFalse("Did not find expected message: " + msg, server.findStringsInLogsAndTraceUsingMark(msg).isEmpty());
        }
    }

    private static void assertNotExpectMessage(String msg) throws Exception {
        assertTrue("Found unexpected message: " + msg, server.findStringsInLogsAndTraceUsingMark(msg).isEmpty());
    }

    private static void flushLog(String msg) throws IOException {
        log(msg, LEVEL_AUDIT, null, false);
        // first wait for a final message to make sure it is there
        assertByWaitingForExpectMessage(msg);
    }
}
