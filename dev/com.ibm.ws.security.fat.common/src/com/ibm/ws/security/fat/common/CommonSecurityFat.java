/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import com.ibm.ws.security.fat.common.apps.CommonFatApplications;
import com.ibm.ws.security.fat.common.logging.CommonFatLoggingUtils;
import com.ibm.ws.security.fat.common.servers.ServerFileUtils;
import com.ibm.ws.security.fat.common.servers.ServerTracker;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

public class CommonSecurityFat {

    protected static Class<?> thisClass = CommonSecurityFat.class;
    public static ServerFileUtils serverFileUtils = new ServerFileUtils();

    @Rule
    public final TestName testName = new TestName();

    protected static ServerTracker serverTracker = new ServerTracker();

    protected CommonFatLoggingUtils loggingUtils = new CommonFatLoggingUtils();

    protected String _testName = null;

    protected static void setUpAndStartServer(LibertyServer server, String startingConfigFile) throws Exception {
        deployApps(server);
        startServer(server, startingConfigFile);
    }

    protected static void startServer(LibertyServer server, String startingConfigFile) throws Exception {
        serverTracker.addServer(server);
        // copy an expanded server config to server.xml (bypass setServerConfigurationFile as our config source path is differnt
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getServerRoot(), "server.xml", serverFileUtils.expandAndBackupCfgFile(server, startingConfigFile));
        // save this "server startup config" for use in restoring a servers config between tests later.
        server.saveServerConfiguration();
        server.startServer();
    }

    protected static void deployApps(LibertyServer server) throws Exception {
        CommonFatApplications.deployTestMarkerApp(server);
    }

    @BeforeClass
    public static void commonBeforeClass() throws Exception {
        serverTracker = new ServerTracker();
    }

    @Before
    public void commonBeforeTest() {
        _testName = testName.getMethodName();
        loggingUtils.printMethodName("STARTING TEST CASE: " + _testName);
        logTestCaseInServerLogs(serverTracker, _testName, "STARTING");
    }

    @After
    public void commonAfterTest() {
        logTestCaseInServerLogs(serverTracker, _testName, "ReStoringConfig");
        for (LibertyServer server : serverTracker.getServers()) {
            try {
                Log.info(thisClass, "commonAfterTest", "Restoring server: " + server.getServerName());
                server.restoreServerConfiguration();
                server.waitForConfigUpdateInLogUsingMark(server.listAllInstalledAppsForValidation());
            } catch (Exception e) {
                e.printStackTrace(System.out);
                Log.info(thisClass, "commonAfterTest", "**********************FAILED to restore original server configuration**********************");
            }
        }
        loggingUtils.printMethodName("ENDING TEST CASE: " + _testName);
        logTestCaseInServerLogs(serverTracker, _testName, "ENDING");
    }

    @AfterClass
    public static void commonAfterClass() throws Exception {
        serverTracker.stopAllServers();
    }

    public void logTestCaseInServerLogs(ServerTracker serverTracker, String testName, String actionToLog) {
        Set<LibertyServer> testServers = serverTracker.getServers();
        for (LibertyServer server : testServers) {
            loggingUtils.logTestCaseInServerLog(server, testName, actionToLog);
            try {
                server.setMarkToEndOfLog(server.getDefaultLogFile());
            } catch (Exception e) {
                Log.error(thisClass, "Failed to set mark to end of default log file for server " + server.getServerName(), e);
            }
        }
    }

    public void reconfigureServer(LibertyServer server, String newConfig, String... waitForMessages) throws Exception {

        reconfigureServer(server, Constants.COMMON_CONFIG_DIR, newConfig, waitForMessages);
    }

    public void reconfigureServer(LibertyServer server, String configDir, String newConfig, String... waitForMessages) throws Exception {

        String newServerCfg = serverFileUtils.expandAndBackupCfgFile(server, configDir + "/" + newConfig, _testName);
        Log.info(thisClass, "reconfigureServer", "Reconfiguring server to use new config: " + newConfig);
        server.setMarkToEndOfLog();
        //        server.replaceServerConfiguration(serverFileUtils.getServerFileLoc(server) + "/" + newServerCfg);
        //        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), serverFileLoc, "server.xml", newServerConfigFile);
        //        server.copyFileToLibertyServerRoot(newServerCfg);
        //        server.copyFileToLibertyServerRoot(serverFileUtils.getServerFileLoc(server), server.getServerFileLoc(server), newServerCfg);
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), serverFileUtils.getServerFileLoc(server), "server.xml", newServerCfg);

        Thread.sleep(200); // Sleep for 200ms to ensure we do not process the file "too quickly" by a subsequent call
        //        setServerConfigurationFile(newConfigFile);
        server.waitForConfigUpdateInLogUsingMark(server.listAllInstalledAppsForValidation(), waitForMessages);
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
