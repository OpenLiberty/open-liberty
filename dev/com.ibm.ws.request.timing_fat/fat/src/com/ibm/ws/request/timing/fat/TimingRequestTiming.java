/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.timing.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class TimingRequestTiming {
    @Server("RequestTimingServer")
    public static LibertyServer server;

    private static final String MESSAGE_LOG = "logs/messages.log";

    @Rule
    public TestName name = new TestName();

    @Before
    public void setupTestStart() throws Exception {
        if (server != null && !server.isStarted()) {
            server.startServer();
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        JavaInfo java = JavaInfo.forCurrentVM();
        ShrinkHelper.defaultDropinApp(server, "jdbcTestPrj_3", "com.ibm.ws.request.timing");
        int javaVersion = java.majorVersion();
        if (javaVersion != 8) {
            CommonTasks.writeLogMsg(Level.INFO, " Java version = " + javaVersion + " - It is higher than 8, adding --add-exports...");
            server.copyFileToLibertyServerRoot("add-exports/jvm.options");
        }
        CommonTasks.writeLogMsg(Level.INFO, " starting server...");
        server.startServer();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("TRAS0112W", "TRAS0115W", "TRAS0114W");
        }
    }

    /*
     * <requestTiming slowRequestThreshold="9s" hungRequestThreshold="20s">
     * <timing
     * eventType="websphere.servlet.service"
     * slowRequestThreshold="5s"
     * hungRequestThreshold="7s">
     * </timing>
     * </requestTiming>
     */
    @Test
    public void testTimingLocalOverridingGlobal() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for - <global - slow : 9s , hung : 20s> <timing - Slow : 5s , hung : 7s>");
        server.setServerConfigurationFile("server_timing_local.xml");
        waitForConfigurationUpdate();

        server.setMarkToEndOfLog();

        createRequests(9000, 1);

        server.waitForStringInLog("TRAS0112W", 10000); // adding timeout for wait as 10s
        server.waitForStringInLog("TRAS0114W", 10000);

        int slow = fetchSlowRequestWarningsCount();
        int hung = fetchHungRequestWarningsCount();
        assertTrue("Expected > 0 slow request warning but found : " + slow, (slow > 0));

        assertTrue("Expected > 0 hung request warning but found : " + hung, (hung > 0));

        CommonTasks.writeLogMsg(Level.INFO, "***** timing works - local settings override global settings. *****");
    }

    /*
     * <requestTiming slowRequestThreshold="-1" hungRequestThreshold="3s">
     * <timing
     * eventType="websphere.servlet.service"
     * slowRequestThreshold="3s"
     * hungRequestThreshold="4s">
     * </timing>
     * </requestTiming>
     */
    @Test
    public void testTimingLocalEnableSlowRequest() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for  <global : slow : -1 , hung : 3s ><timing - slow : 3s , hung : 4s>");
        server.setServerConfigurationFile("server_timing_global_NoSlowReq.xml");
        server.waitForStringInLog("CWWKG0017I", 30000);

        server.setMarkToEndOfLog();

        createRequests(6000, 1);

        server.waitForStringInLog("TRAS0112W", 10000);
        server.waitForStringInLog("TRAS0114W", 10000);

        int slow = fetchSlowRequestWarningsCount();
        int hung = fetchHungRequestWarningsCount();

        //Retry the request again
        if (slow == 0) {
            CommonTasks.writeLogMsg(Level.INFO, "$$$$ -----> Retry because no slow request warning found!");
            createRequests(6000, 1);
            server.waitForStringInLogUsingMark("TRAS0112W", 10000);
            slow = fetchSlowRequestWarningsCount();
        }

        assertTrue("Expected > 0 slow request warning but found : " + slow, (slow > 0));

        assertTrue("Expected > 0 hung request warning but found : " + hung, (hung > 0));

        CommonTasks.writeLogMsg(Level.INFO, "***** timing works - local config enables slow request even if disabled in Global config *****");
    }

    /*
     * <requestTiming>
     * <timing
     * eventType="websphere.servlet.service"
     * slowRequestThreshold="3s"
     * hungRequestThreshold="5s">
     * </timing>
     * </requestTiming>
     */
    @Test
    public void testDynamicTimingEnableDisable() throws Exception {
        //Set to default Configuration of Request Timing feature
        server.setServerConfigurationFile("server_original.xml");
        server.waitForStringInLogUsingMark("CWWKG0017I", 30000);
        CommonTasks.writeLogMsg(Level.INFO, "****  Started server without <timing> : default configurations for Request timing");
        createRequests(12000, 1);

        server.waitForStringInLogUsingMark("TRAS0112W", 10000);
        int p_slow = fetchSlowRequestWarningsCount();
        int p_hung = fetchHungRequestWarningsCount();

        assertTrue("Expected 1 slow request warning but found : " + p_slow, (p_slow > 0));
        assertTrue("Expected 0 hung request warning but found : " + p_hung, (p_hung == 0));

        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> UPDATE server configuration thresholds for - <global - defaults> <timing - Slow : 3s , hung : 5s>");
        server.setServerConfigurationFile("server_timing_localOnly.xml");
        server.waitForStringInLogUsingMark("CWWKG0017I", 30000);

        createRequests(8000, 1);

        int slow = fetchSlowRequestWarningsCount();
        int hung = fetchHungRequestWarningsCount();

        server.waitForStringInLogUsingMark("TRAS0112W", 10000);
        server.waitForStringInLogUsingMark("TRAS0114W", 10000);

        assertTrue("Expected  > 0 slow request warnings but found : " + slow, ((slow - p_slow) > 0));
        assertTrue("Expected 1 or more hung request warning but found : " + hung, (hung > 0));

        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> UPDATE server configuration: Remove <timing>");
        server.setServerConfigurationFile("server_original.xml");
        server.waitForStringInLogUsingMark("CWWKG0017I", 30000);

        createRequests(12000, 1);

        int n_slow = fetchSlowRequestWarningsCount();
        int n_hung = fetchHungRequestWarningsCount();

        assertTrue("Expected > 0 slow request warning but found : " + n_slow, ((n_slow - slow) > 0));
        assertTrue("Expected 0 hung request warning but found : " + n_hung, ((n_hung - hung) == 0));

        CommonTasks.writeLogMsg(Level.INFO, "***** timing works - Dynamic enable and disable *****");
    }

    /**
     * Verify that an exact match on context info over-rides global defaults.
     */
    @Test
    public void testContextInfoExactMatchOverrideDefault() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for - <global - slow : 5s , hung : 10s> <timing - Slow : 120s , hung : 120s>");
        server.setServerConfigurationFile("contextInfoPattern/server_timing_1.xml");
        server.waitForStringInLog("CWWKG0017I", 30000);

        server.setMarkToEndOfLog();

        // Should sleep and not hit hung request threshold since the context info specific settings
        // are longer than the global defaults.
        long startTime = System.nanoTime();
        createRequests(12000, 1);
        long elapsedSeconds = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        assertTrue("Test did not run long enough (elapsedSeconds)", elapsedSeconds >= 12);

        // Make sure we didn't get any hung request messages
        assertTrue("Test should not have found any slow request messages", fetchSlowRequestWarningsCount() == 0);
        assertTrue("Test should not have found any hung request messages", fetchHungRequestWarningsCount() == 0);

        // OK now test the opposite - that we do timeout because we matched the context info specific
        // settings.
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for - <global - slow : 120s , hung : 120s> <timing - Slow : 5s , hung : 10s>");
        server.setServerConfigurationFile("contextInfoPattern/server_timing_2.xml");
        server.waitForStringInLog("CWWKG0017I", 30000);

        server.setMarkToEndOfLog();

        // Should sleep and hit hung request threshold since the context info specific settings
        // are shorter than the global thresholds.
        startTime = System.nanoTime();
        createRequests(12000, 1);
        elapsedSeconds = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        assertTrue("Test did not run long enough (elapsedSeconds)", elapsedSeconds >= 12);

        // Make sure we had some hung request messages
        assertTrue("Test should have found slow request messages", fetchSlowRequestWarningsCount() > 0);
        assertTrue("Test should have found hung request messages", fetchHungRequestWarningsCount() > 0);

        CommonTasks.writeLogMsg(Level.INFO, "***** Testcase testContextInfoExactMatchOverrideDefault pass *****");
    }

    /**
     * Verify that a wild-card match on context info over-rides global defaults.
     */
    @Test
    public void testContextInfoWildCardMatchOverrideDefault() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for - <global - slow : 5s , hung : 10s> <timing - Slow : 120s , hung : 120s>");
        server.setServerConfigurationFile("contextInfoPattern/server_timing_3.xml");
        server.waitForStringInLog("CWWKG0017I", 30000);

        server.setMarkToEndOfLog();

        // Should sleep and not hit hung request threshold since the context info specific settings
        // are longer than the global thresholds.
        long startTime = System.nanoTime();
        createRequests(12000, 1);
        long elapsedSeconds = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        assertTrue("Test did not run long enough (elapsedSeconds)", elapsedSeconds >= 12);

        // Make sure we didn't get any hung request messages
        assertTrue("Test should not have found any slow request messages", fetchSlowRequestWarningsCount() == 0);
        assertTrue("Test should not have found any hung request messages", fetchHungRequestWarningsCount() == 0);

        // OK now test the opposite - that we do timeout because we matched the context info specific
        // settings.
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for - <global - slow : 120s , hung : 120s> <timing - Slow : 5s , hung : 10s>");
        server.setServerConfigurationFile("contextInfoPattern/server_timing_4.xml");
        server.waitForStringInLog("CWWKG0017I", 30000);

        server.setMarkToEndOfLog();

        // Should sleep and hit hung request threshold since the context info specific settings
        // are shorter than the global thresholds.
        startTime = System.nanoTime();
        createRequests(12000, 1);
        elapsedSeconds = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        assertTrue("Test did not run long enough (elapsedSeconds)", elapsedSeconds >= 12);

        // Make sure we had some hung request messages
        assertTrue("Test should have found slow request messages", fetchSlowRequestWarningsCount() > 0);
        assertTrue("Test should have found hung request messages", fetchHungRequestWarningsCount() > 0);

        CommonTasks.writeLogMsg(Level.INFO, "***** Testcase testContextInfoWildCardMatchOverrideDefault pass *****");
    }

    /*
     * <requestTiming slowRequestThreshold="3s" hungRequestThreshold="6s">
     * <timing
     * eventType="websphere.datasource.execute"
     * slowRequestThreshold="10s"
     * hungRequestThreshold="12s">
     * </timing>
     * </requestTiming>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTimingGlobalConfig() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for  <global : slow : 3s , hung : 6s><timing - Slow : 10s , hung : 12s>");
        server.setServerConfigurationFile("server_timing_global.xml");
        waitForConfigurationUpdate();

        createRequests(20000, 1);

        server.waitForStringInLogUsingMark("TRAS0112W", 10000);
        server.waitForStringInLogUsingMark("TRAS0114W", 10000);

        int slow = fetchSlowRequestWarningsCount();
        int hung = fetchHungRequestWarningsCount();

        assertTrue("Expected > 1 slow request warnings but found : " + slow, (slow > 1));

        assertTrue("Expected > 1 hung request warning but found : " + hung, (hung > 1));

        CommonTasks.writeLogMsg(Level.INFO, "***** timing works - global settings applies for rest of the root event types. *****");
    }

    /*
     * <requestTiming>
     * <timing
     * eventType="websphere.servlet.service"
     * slowRequestThreshold="3s"
     * hungRequestThreshold="5s">
     * </timing>
     * </requestTiming>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTimingLocalConfigOnly() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for  <global : defaults ><timing - Slow : 3s , hung : 5s>");
        server.setServerConfigurationFile("server_timing_localOnly.xml");
        waitForConfigurationUpdate();

        server.setMarkToEndOfLog();

        createRequests(7000, 1);

        server.waitForStringInLogUsingMark("TRAS0112W", 10000);
        server.waitForStringInLogUsingMark("TRAS0114W", 10000);

        int slow = fetchSlowRequestWarningsCount();

        // Retry the request again
        if (slow == 0) {
            CommonTasks.writeLogMsg(Level.INFO, "$$$$ -----> Retry because no slow request warning found!");
            createRequests(7000, 1);
            server.waitForStringInLogUsingMark("TRAS0112W", 10000);
            slow = fetchSlowRequestWarningsCount();
        }

        int hung = fetchHungRequestWarningsCount();

        assertTrue("Expected > 0 slow request warning but found : " + slow, (slow > 0));

        assertTrue("Expected 1 hung request warning but found : " + hung, (hung > 0));

        CommonTasks.writeLogMsg(Level.INFO, "***** timing works - local config works.. Global config is not specified *****");

    }

    /*
     * <requestTiming>
     * <timing
     * eventType="websphere.datasource.execute"
     * slowRequestThreshold="3s"
     * hungRequestThreshold="5s">
     * </timing>
     * </requestTiming>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTimingGlobalConfigNotSpecified() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for  <global : defaults ><timing - Slow : 3s , hung : 5s>");
        server.setServerConfigurationFile("server_timing_NoGlobal.xml");
        waitForConfigurationUpdate();

        server.setMarkToEndOfLog();

        createRequests(11000, 1);

        server.waitForStringInLogUsingMark("TRAS0112W", 10000);

        int slow = fetchSlowRequestWarningsCount();

        // Retry the request again
        if (slow == 0) {
            CommonTasks.writeLogMsg(Level.INFO, "$$$$ -----> Retry because no slow request warning found!");
            createRequests(11000, 1);
            server.waitForStringInLogUsingMark("TRAS0112W", 10000);
            slow = fetchSlowRequestWarningsCount();
        }

        int hung = fetchHungRequestWarningsCount();

        assertTrue("Expected 1 slow request warning but found : " + slow, (slow > 0));

        assertTrue("Expected 0 hung request warning but found : " + hung, (hung == 0));

        CommonTasks.writeLogMsg(Level.INFO, "***** timing works - local config works.. Global config is not specified *****");
    }

    /*
     * <requestTiming slowRequestThreshold="9s" hungRequestThreshold="20s">
     * <timing
     * eventType="websphere.servlet.service"
     * slowRequestThreshold="-1"
     * hungRequestThreshold="3s">
     * </timing>
     * </requestTiming>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTimingLocalNegativeSlowThreshold() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for  <global : slow : 9s , hung : 20s ><timing - Slow : -1 , hung : 3s>");
        server.setServerConfigurationFile("server_timing_local_NoSlowReq.xml");
        waitForConfigurationUpdate();

        server.setMarkToEndOfLog();

        createRequests(5000, 1);

        server.waitForStringInLogUsingMark("TRAS0114W", 10000);

        int slow = fetchSlowRequestWarningsCount();
        int hung = fetchHungRequestWarningsCount();

        assertTrue("Expected 0 slow request warning but found : " + slow, (slow == 0));

        assertTrue("Expected 1 hung request warning but found : " + hung, (hung > 0));

        CommonTasks.writeLogMsg(Level.INFO, "***** timing works - local config disables slow request for value smaller that 1 *****");
    }

    /*
     * <requestTiming slowRequestThreshold="2s" hungRequestThreshold="4s">
     * <timing eventType="websphere.servlet.service">
     * </timing>
     * </requestTiming>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTimingLocalInheritsGlobalConfig() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for  <global : slow : 2s , hung : 4s ><timing>");
        server.setServerConfigurationFile("server_timing_local_inherits.xml");
        waitForConfigurationUpdate();

        server.setMarkToEndOfLog();

        createRequests(6000, 1);

        server.waitForStringInLogUsingMark("TRAS0112W", 15000);
        server.waitForStringInLogUsingMark("TRAS0114W", 10000);

        int slow = fetchSlowRequestWarningsCount();

        if (slow == 0) {
            CommonTasks.writeLogMsg(Level.INFO, "$$$$ -----> Retry because no slow request warning found!");
            createRequests(6000, 1);
            server.waitForStringInLog("TRAS0112W", 15000);
            slow = fetchSlowRequestWarningsCount();
        }

        int hung = fetchHungRequestWarningsCount();

        assertTrue("Expected > 1 slow request warnings but found : " + slow, (slow > 1));

        assertTrue("Expected 1 hung request warning but found : " + hung, (hung > 0));

        CommonTasks.writeLogMsg(Level.INFO, "***** timing works - local config inherits Global config if local config is not specified *****");
    }

    /*
     * <requestTiming>
     * <timing eventType="websphere.servlet.service"/>
     * </requestTiming>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTimingLocalGlobalNoConfig() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for - <global -default> <timing - default>");
        server.setServerConfigurationFile("server_timing_local_global_noConfig.xml");
        waitForConfigurationUpdate();

        createRequests(11000, 1);

        server.waitForStringInLogUsingMark("TRAS0112W", 10000);

        int slow = fetchSlowRequestWarningsCount();
        int hung = fetchHungRequestWarningsCount();
        assertTrue("Expected 1 slow request warning but found : " + slow, (slow > 0));

        assertTrue("Expected 0 hung request warning but found : " + hung, (hung == 0));

        CommonTasks.writeLogMsg(Level.INFO, "***** timing works - local and global picks default if no config specified *****");
    }

    /*
     * <requestTiming slowRequestThreshold="1s" hungRequestThreshold="2s">
     * <timing
     * eventType="websphere.servlet.service"
     * slowRequestThreshold="0"
     * hungRequestThreshold="0">
     * </timing>
     * </requestTiming>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTimingLocalDisableSlowHungRequest() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for - <global : slow : 1s , hung : 2s ><timing - slow : 0 , hung : 0>");
        server.setServerConfigurationFile("server_timing_NoSlowHungReqs.xml");
        waitForConfigurationUpdate();

        createRequests(11000, 1);

        int slow = fetchSlowRequestWarningsCount();
        int hung = fetchHungRequestWarningsCount();
        assertTrue("Expected 0 slow request warning but found : " + slow, (slow == 0));

        assertTrue("Expected 0 hung request warning but found : " + hung, (hung == 0));

        CommonTasks.writeLogMsg(Level.INFO, "***** timing works - local disables slow and hung requests *****");
    }

    /*
     * <requestTiming>
     * <timing
     * eventType="websphere.servlet.service"
     * slowRequestThreshold="2s"
     * hungRequestThreshold="9m">
     * </timing>
     * </requestTiming>
     * <requestTiming slowRequestThreshold="3s" hungRequestThreshold="6s"/>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTimingGlobalConfigFollowsLocal() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for - <global : slow : 3s , hung : 6s ><timing - slow : 2s , hung : 9m>");
        server.setServerConfigurationFile("server_timing_global_follows_local.xml");
        waitForConfigurationUpdate();

        createRequests(5000, 1);

        server.waitForStringInLogUsingMark("TRAS0112W", 15000);

        int slow = fetchSlowRequestWarningsCount();

        if (slow == 0) {
            CommonTasks.writeLogMsg(Level.INFO, "$$$$ -----> Retry because no slow request warning found!");
            createRequests(5000, 1);
            server.waitForStringInLog("TRAS0112W", 15000);
            slow = fetchSlowRequestWarningsCount();
        }

        int hung = fetchHungRequestWarningsCount();
        assertTrue("Expected > 1 slow request warnings but found : " + slow, (slow > 1));

        assertTrue("Expected 0 hung request warning but found : " + hung, (hung == 0));

        CommonTasks.writeLogMsg(Level.INFO, "***** timing works - local overrides global specified after it *****");
    }

    /*
     * <requestTiming slowRequestThreshold="9s" hungRequestThreshold="20s">
     * <timing
     * eventType="websphere.servlet.service"
     * slowRequestThreshold="5s"
     * hungRequestThreshold="7s">
     * </timing>
     * </requestTiming>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testDynamicTimingUpdate() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for - <global - slow : 9s , hung : 20s> <timing - Slow : 5s , hung : 7s>");
        server.setServerConfigurationFile("server_timing_local.xml");
        waitForConfigurationUpdate();

        createRequests(9000, 1);

        server.waitForStringInLogUsingMark("TRAS0112W", 10000);
        server.waitForStringInLogUsingMark("TRAS0114W", 10000);

        int slow = fetchSlowRequestWarningsCount();
        int hung = fetchHungRequestWarningsCount();
        assertTrue("Expected > 0 slow request warnings but found : " + slow, (slow > 0));

        assertTrue("Expected 1 hung request warning but found : " + hung, (hung > 0));
        server.setMarkToEndOfLog();

        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> UPDATED server configuration thresholds for  <global : defaults ><timing - Slow : 3s , hung : 5s>");
        server.setServerConfigurationFile("server_timing_localOnly.xml");
        waitForConfigurationUpdate();

        createRequests(7000, 1);

        server.waitForStringInLogUsingMark("TRAS0112W", 10000);
        server.waitForStringInLogUsingMark("TRAS0114W", 10000);

        slow = fetchSlowRequestWarningsCount() - slow;
        hung = fetchHungRequestWarningsCount() - hung;

        assertTrue("Expected > 0 slow request warnings but found : " + slow, (slow > 0));

        assertTrue("Expected  > 0 hung request warnings but found : " + hung, (hung > 0));

        CommonTasks.writeLogMsg(Level.INFO, "***** timing works - dynamic updating timing *****");
    }

    /*
     * <requestTiming includeContextInfo="false">
     * <timing eventType="websphere.servlet.service"
     * contextInfoPattern="Pattern"
     * slowRequestThreshold="5s"/>
     * </requestTiming>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTimingContextInfoConflict() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration ctx info conflict");
        server.setServerConfigurationFile("server_timing_ctxinfo_conflict.xml");
        server.waitForStringInLog("CWWKG0017I", 30000);

        // Look for context info conflict message
        assertNotNull("Expected to find context info conflict message", server.waitForStringInLog("TRAS3302W"));

        CommonTasks.writeLogMsg(Level.INFO, "***** timing works - context info conflict detected. *****");

        server.setMarkToEndOfLog();

        // Clean up the mess we made.
        if (server != null && server.isStarted()) {
            server.setServerConfigurationFile("server_original.xml");
            server.waitForStringInLog("CWWKG0017I", 30000);
            server.stopServer("TRAS3302W.*"); // stop the server, expecting the warning message.
        }
    }

    /*
     * <requestTiming includeContextInfo="false">
     * <timing eventType="websphere.servlet.service"
     * slowRequestThreshold="5s"/>
     * </requestTiming>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTimingContextInfoNoConflict() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration ctx info no conflict");
        server.setServerConfigurationFile("server_timing_ctxinfo_no_conflict.xml");
        server.waitForStringInLog("CWWKG0017I", 30000);

        // Look for context info conflict message
        assertNull("Did not expect to find context info conflict message", server.waitForStringInLog("TRAS3302W", 10000));

        CommonTasks.writeLogMsg(Level.INFO, "***** timing works - context info conflict not detected. *****");
    }

    /**
     * Verify that when we don't match wild-cards we take the defaults.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testContextInfoWildCardNoMatch() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for - <global - slow : 120s , hung : 120s> <timing - Slow : 5s , hung : 10s>");
        server.setServerConfigurationFile("contextInfoPattern/server_timing_5.xml");
        server.waitForStringInLog("CWWKG0017I", 30000);

        server.setMarkToEndOfLog();

        // Should sleep and not hit hung request threshold since the global defaults
        // are longer than all the context-info-specific settings.
        long startTime = System.nanoTime();
        createRequests(12000, 1);
        long elapsedSeconds = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        assertTrue("Test did not run long enough (elapsedSeconds)", elapsedSeconds >= 12);

        // Make sure we didn't get any hung request messages
        assertTrue("Test should not have found any slow request messages", fetchSlowRequestWarningsCount() == 0);
        assertTrue("Test should not have found any hung request messages", fetchHungRequestWarningsCount() == 0);

        // OK now test the opposite - that we do timeout because the global settings are short.
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for - <global - slow : 5s , hung : 10s> <timing - Slow : 120s , hung : 120s>");
        server.setServerConfigurationFile("contextInfoPattern/server_timing_6.xml");
        server.waitForStringInLog("CWWKG0017I", 30000);

        server.setMarkToEndOfLog();

        // Should sleep and hit the hung request threshold since the context info specific settings
        // are longer than the global defaults.
        startTime = System.nanoTime();
        createRequests(12000, 1);
        elapsedSeconds = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        assertTrue("Test did not run long enough (elapsedSeconds)", elapsedSeconds >= 12);

        // Make sure we had some hung request messages
        assertTrue("Test should have found slow request messages", fetchSlowRequestWarningsCount() > 0);
        assertTrue("Test should have found hung request messages", fetchHungRequestWarningsCount() > 0);

        CommonTasks.writeLogMsg(Level.INFO, "***** Testcase pass *****");
    }

    /**
     * Verify that we can completely disable a timer for an exact context info match.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testContextInfoExactMatchDisableDefault() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for - <global - slow : 5s , hung : 10s> <timing - Slow : 120s , hung : 120s>");
        server.setServerConfigurationFile("contextInfoPattern/server_timing_7.xml");
        server.waitForStringInLog("CWWKG0017I", 30000);

        server.setMarkToEndOfLog();

        // Should sleep and not hit hung request threshold since the context info specific settings
        // are longer than the global defaults.
        long startTime = System.nanoTime();
        createRequests(12000, 1);
        long elapsedSeconds = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        assertTrue("Test did not run long enough (elapsedSeconds)", elapsedSeconds >= 12);

        // Make sure we didn't get any hung request messages
        assertTrue("Test should not have found any slow request messages", fetchSlowRequestWarningsCount() == 0);
        assertTrue("Test should not have found any hung request messages", fetchHungRequestWarningsCount() == 0);

        // OK now test the opposite - that we do timeout because we matched the context info specific
        // settings.
        CommonTasks.writeLogMsg(Level.INFO, "**** >>>>> server configuration thresholds for - <global - slow : 120s , hung : 120s> <timing - Slow : 5s , hung : 10s>");
        server.setServerConfigurationFile("contextInfoPattern/server_timing_8.xml");
        server.waitForStringInLog("CWWKG0017I", 30000);

        server.setMarkToEndOfLog();

        // Should sleep and not hit hung request threshold since the context info specific settings
        // are longer than the global defaults.
        startTime = System.nanoTime();
        createRequests(12000, 1);
        elapsedSeconds = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        assertTrue("Test did not run long enough (elapsedSeconds)", elapsedSeconds >= 12);

        // Make sure we had some hung request messages
        assertTrue("Test should have found slow request messages", fetchSlowRequestWarningsCount() > 0);
        assertTrue("Test should have found hung request messages", fetchHungRequestWarningsCount() > 0);

        CommonTasks.writeLogMsg(Level.INFO, "***** Testcase testContextInfoExactMatchDisableDefault pass *****");
    }

    private void waitForConfigurationUpdate() throws Exception {
        server.waitForStringInLogUsingMark("CWWKG0017I", 30000);
    }

    private void createRequests(int delayInMilliSeconds, int noOfTimes) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_3?sleepTime=" + delayInMilliSeconds);
        CommonTasks.writeLogMsg(Level.INFO, "------> creating " + noOfTimes + " requests");
        CommonTasks.writeLogMsg(Level.INFO, "Calling jdbcTestPrj_3 Application with URL=" + url.toString());
        HttpURLConnection con;
        BufferedReader br;
        while (noOfTimes > 0) {
            con = getHttpConnection(url);
            br = getConnectionStream(con);
            br.readLine();
            noOfTimes--;
        }
    }

    private int fetchSlowRequestWarningsCount() throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "------> Slow Request Warning : " + line);
        }
        return lines.size();
    }

    private int fetchHungRequestWarningsCount() throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0114W", MESSAGE_LOG);
        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "------> Hung Request Warning : " + line);
        }
        return lines.size();
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
