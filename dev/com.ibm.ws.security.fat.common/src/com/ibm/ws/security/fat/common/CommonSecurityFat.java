/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common;

import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.Description;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.FatWatcher;
import com.ibm.ws.security.fat.common.logging.CommonFatLoggingUtils;
import com.ibm.ws.security.fat.common.servers.ServerTracker;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.ServerFileUtils;

public class CommonSecurityFat {

    protected static Class<?> thisClass = CommonSecurityFat.class;
    public static ServerFileUtils serverFileUtils = new ServerFileUtils();

    @Rule
    public final TestName testName = new TestName();

    protected static ServerTracker serverTracker = new ServerTracker();
    protected static ServerTracker skipRestoreServerTracker = new ServerTracker();

    protected CommonFatLoggingUtils loggingUtils = new CommonFatLoggingUtils();

    protected String _testName = null;

    @BeforeClass
    public static void commonBeforeClass() throws Exception {
        Log.info(thisClass, "commonBeforeClass", "Starting Class");
        serverTracker = new ServerTracker();
        skipRestoreServerTracker = new ServerTracker();
    }

    @Before
    public void commonBeforeTest() {
        _testName = testName.getMethodName();
        loggingUtils.printMethodName("STARTING TEST CASE: " + _testName);
        logTestCaseInServerLogs("STARTING");
    }

    @After
    public void commonAfterTest() {
        restoreTestServers();
        loggingUtils.printMethodName("ENDING TEST CASE: " + _testName);
        logTestCaseInServerLogs("ENDING");
    }

    @AfterClass
    public static void commonAfterClass() throws Exception {
        Log.info(thisClass, "commonAfterClass", " from CommonSecurityFat");
        serverTracker.stopAllServers();
        Log.info(thisClass, "commonAfterClass", "Ending Class");
    }

    public void logTestCaseInServerLogs(String actionToLog) {
        Set<LibertyServer> testServers = serverTracker.getServers();
        for (LibertyServer server : testServers) {
            if (server != null && !server.isStarted()) {
                continue;
            }
            loggingUtils.logTestCaseInServerLog(server, _testName, actionToLog);
            try {
                server.setMarkToEndOfLog(server.getDefaultLogFile());
            } catch (Exception e) {
                Log.error(thisClass, "Failed to set mark to end of default log file for server " + server.getServerName(), e);
            }
        }
    }

    /**
     * Restore all running server configurations to their startup state.
     * Override this method if a particular test class does NOT want
     * to have servers restored between tests or if you only want
     * to restore a specific subset of servers.
     */
    public void restoreTestServers() {
        logTestCaseInServerLogs("ReStoringConfig");
        for (LibertyServer server : serverTracker.getServers()) {
            try {
                if (skipRestoreServerTracker.trackerContains(server)) {
                    Log.info(thisClass, "restoreTestServers", "Restore of server: " + server.getServerName() + " has been skipped");
                } else {
                    Log.info(thisClass, "restoreTestServers", "Restoring server: " + server.getServerName());
                    server.restoreServerConfigurationAndWaitForApps();
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                Log.info(thisClass, "restoreTestServers", "**********************FAILED to restore original server configuration**********************");
            }
        }
    }

    public void logTestCaseMarkerInServerLogs(String actionToLog, String infoToLog) {
        Set<LibertyServer> testServers = serverTracker.getServers();
        for (LibertyServer server : testServers) {
            if (server != null && !server.isStarted()) {
                continue;
            }
            loggingUtils.logTestCaseInServerLog(server, _testName + ": " + infoToLog, actionToLog);
            try {
                server.setMarkToEndOfLog(server.getDefaultLogFile());
            } catch (Exception e) {
                Log.error(thisClass, "Failed to set mark to end of default log file for server " + server.getServerName(), e);
            }
        }
    }

    @Rule
    public FatWatcher watchman = new FatWatcher() {
        @Override
        public void failed(Throwable e, Description description) {

            String methodName = "failed";
            Log.info(thisClass, methodName, _testName + ": Test failed");
            Log.info(thisClass, methodName, "");
            Log.info(thisClass, methodName, "TTTTT EEEEE  SSSS TTTTT   FFFFF  AAA  IIIII L     EEEEE DDDD");
            Log.info(thisClass, methodName, "  T   E     S       T     F     A   A   I   L     E     D   D");
            Log.info(thisClass, methodName, "  T   EEE    SSS    T     FFF   AAAAA   I   L     EEE   D   D");
            Log.info(thisClass, methodName, "  T   E         S   T     F     A   A   I   L     E     D   D");
            Log.info(thisClass, methodName, "  T   EEEEE SSSS    T     F     A   A IIIII LLLLL EEEEE DDDD");
            Log.info(thisClass, methodName, "");
            super.failed(e, description);
        }

        @Override
        public void succeeded(Description description) {

            String methodName = "succeeded";
            Log.info(thisClass, methodName, _testName + ": Test succeeded");
            Log.info(thisClass, methodName, "");
            Log.info(thisClass, methodName, "TTTTT EEEEE  SSSS TTTTT   PPPP   AAA   SSSS SSSSS EEEEE DDDD");
            Log.info(thisClass, methodName, "  T   E     S       T     P   P A   A S     S     E     D   D");
            Log.info(thisClass, methodName, "  T   EEE    SSS    T     PPPP  AAAAA  SSS   SSS  EEE   D   D");
            Log.info(thisClass, methodName, "  T   E         S   T     F     A   A     S     S E     D   D");
            Log.info(thisClass, methodName, "  T   EEEEE SSSS    T     F     A   A SSSS  SSSS  EEEEE DDDD");
            Log.info(thisClass, methodName, "");
            super.succeeded(description);
        }
    };

}
