/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.server.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigMonitorElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.exception.TopologyException;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class ServerConfigTest {

    /**  */
    private static final String RESTART_SERVLET_URL = "/restart/restart?testName=";
    /**  */
    private static final String VARIABLE_IMPORT_SERVER = "com.ibm.ws.config.import.variables";
    public static final String CHECK_VARIABLE_IMPORT = "checkVariableImport";

    // Since we have tracing enabled give server longer timeout to start up.
    private static final long SERVER_START_TIMEOUT = 30 * 1000;
    public static final String CHECK_VARIABLE_IMPORT_UPDATE = "checkVariableImport2";
    private static final String VARIABLE_IMPORT_UPDATE_FILE = "import.variable/server.xml";

    private static WebArchive restartApp;

    @BeforeClass
    public static void setUp() throws Exception {
        restartApp = ShrinkHelper.buildDefaultApp("restart", "test.server.config.restart");
    }

    @Test
    public void testRestart() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.restart");
        ShrinkHelper.exportAppToServer(server, restartApp);
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);
        server.startServer("before.log");
        // Wait for the application to be installed before proceeding
        assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKF0008I")); // Feature update completed
        assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKZ0001I.* restart"));

        try {
            // run the test one
            test(server, "/restart/restart?testName=before");

            server.stopServer(false);

            // switch to new configuration
            server.copyFileToLibertyServerRoot("restart/server.xml");

            server.startServer("after.log", false);
            // Wait for the application to be installed before proceeding
            assertNotNull("The restart application never came up the second time", server.waitForStringInLog("CWWKF0008I")); // Feature update completed
            assertNotNull("The restart application never came up the second time", server.waitForStringInLog("CWWKZ0001I.* restart"));

            // run the test two
            test(server, "/restart/restart?testName=after");
        } finally {
            server.stopServer();
            server.deleteFileFromLibertyInstallRoot("lib/com.ibm.ws.config.metatype_1.0.jar");
        }
    }

    @Test
    public void testRefresh() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.refresh");
        ShrinkHelper.exportAppToServer(server, restartApp);
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);
        server.startServer("refresh.log");

        // Wait for the application to be installed before proceeding
        assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKZ0001I.* restart"));

        try {
            // run the test one
            test(server, "/restart/restart?testName=before");

            // switch to new configuration
            server.copyFileToLibertyServerRoot("refresh/server.xml");
            // Wait for the application to be installed before proceeding
            assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKZ0001I.* restart"));

            // cause configuration refresh
            test(server, "/restart/restart?testName=refresh");
            // Wait for the application to be installed before proceeding
            assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKZ0001I.* restart"));

            // run the test two
            test(server, "/restart/restart?testName=after");
        } finally {
            server.stopServer();
        }
    }

    @Test
    public void testVariableRestart() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.restart.var");
        ShrinkHelper.exportAppToServer(server, restartApp);
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);
        server.startServer("restart-var-before.log");
        // Wait for the application to be installed before proceeding
        assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKZ0001I.* restart"));

        try {
            // run the test one
            test(server, "/restart/restart?testName=beforeVariable");

            server.stopServer(false);

            // switch to new bootstrap.properties
            server.copyFileToLibertyServerRoot("restart.var/bootstrap.properties");

            server.startServer("restart-var-after.log", false);
            // Wait for the application to be installed before proceeding
            assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKZ0001I.* restart"));

            // run the test two
            test(server, "/restart/restart?testName=afterVariable");
        } finally {
            server.stopServer();
        }
    }

    @Test
    public void testValidate() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.validate");
        ShrinkHelper.exportAppToServer(server, restartApp);
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);

        server.copyFileToLibertyInstallRoot("lib/features", "metatype/metatype-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib", "bundles/com.ibm.ws.config.metatype.jar");

        server.startServer();

        try {
            assertStringsPresentInLog(server, new String[] { "CWWKG0102I.*person", "firstName.*Jane" });
            assertStringsPresentInLog(server, new String[] { "CWWKG0102I(?=.*ejb)(?=.*threadPool)", "minThreads.*5" });
            assertStringsPresentInLog(server, new String[] { "CWWKG0102I.*quickStartSecurity" });
        } finally {
            server.stopServer("CWWKS0900E");
        }
    }

    @Test
    public void testValidateUpdateFileTag() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.validateConfig");
        ShrinkHelper.exportAppToServer(server, restartApp);
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);
        server.startServer();

        try {
            assertASCIIFileTag(server.getDefaultLogFile());
        } finally {
            server.stopServer();
        }
    }

    @Test
    public void testRelativeImports() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.import");
        ShrinkHelper.exportAppToServer(server, restartApp);
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);
        server.startServer("imports.log");
        // Wait for the application to be installed before proceeding
        assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKZ0001I.* restart"));

        try {
            // run the test one
            test(server, "/restart/restart?testName=checkImport");
        } finally {
            server.stopServer();
        }
    }

    @Test
    public void testImportWithVariables() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer(VARIABLE_IMPORT_SERVER);
        ShrinkHelper.exportAppToServer(server, restartApp);
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);

        server.setConsoleLogName("varimports.log");
        ArrayList<String> args = new ArrayList<String>();
        args.add("--");
        args.add("--import1=./common/common.xml");

        server.startServerWithArgs(true, true, true, false, "start", args, true);

        // Wait for the application to be installed before proceeding
        assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKZ0001I.* restart"));

        try {
            // run the test one
            test(server, RESTART_SERVLET_URL + CHECK_VARIABLE_IMPORT);

            // switch to updated config
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile(VARIABLE_IMPORT_UPDATE_FILE);
            server.waitForConfigUpdateInLogUsingMark(null);

            test(server, RESTART_SERVLET_URL + CHECK_VARIABLE_IMPORT_UPDATE);
        } finally {
            server.stopServer();
        }

    }

    @Test
    public void testRefreshError() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.refresh.error");
        ShrinkHelper.exportAppToServer(server, restartApp);
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);
        server.startServer("refresh-error.log");

        // Wait for the application to be installed before proceeding
        assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKZ0001I.* restart"));

        try {
            // run the test one
            test(server, "/restart/restart?testName=before");

            // switch to new (bad) configuration
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("refresh/bad-server.xml");

            // cause configuration refresh
            test(server, "/restart/restart?testName=refresh");

            // wait for error
            assertNotNull("No refresh error", server.waitForStringInLog("CWWKG0014E.*"));

            // run the test one again since nothing has changed
            test(server, "/restart/restart?testName=before");

            // switch to new (good) configuration
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("refresh/server.xml");

            // cause configuration refresh
            test(server, "/restart/restart?testName=refresh");

            // run the test two
            test(server, "/restart/restart?testName=after");
        } finally {
            server.stopServer("CWWKG0014E");
        }
    }

    /**
     * This test makes sure that you can update the server configuration and that the changes will be picked up when using a polled style of update trigger. It will then disable
     * the polling to make sure changes are no longer picked up.
     *
     * @throws Exception
     */
    @Test
    public void testServerConfigUpdating() throws Exception {
        LibertyServer server = LibertyServerFactory.getStartedLibertyServer("com.ibm.ws.config.update");
        ShrinkHelper.exportAppToServer(server, restartApp);

        try {
            // The server has no update trigger so first set one to polled.  Polled is the default so this change should be picked up
            ServerConfiguration config = server.getServerConfiguration();
            ConfigMonitorElement configMonitor = config.getConfig();
            configMonitor.setUpdateTrigger("polled");
            server.updateServerConfiguration(config);
            assertNotNull("The server configuration was not updated when setting it to polled", server.waitForStringInLog("CWWKG0017I"));

            long startTime = System.currentTimeMillis();
            // Now update the server configuration to make sure that still works
            server.setMarkToEndOfLog();
            config.addApplication("inexistent", ".", "war");
            server.updateServerConfiguration(config);
            assertNotNull("The server configuration was not updated when adding a feature", server.waitForStringInLog("CWWKG0017I"));

            // Now turn off server config monitoring and make sure the updating stops
            server.setMarkToEndOfLog();
            configMonitor.setUpdateTrigger("disabled");
            server.updateServerConfiguration(config);
            assertNotNull("The server configuration was not updated for disabling monitoring", server.waitForStringInLog("CWWKG0017I"));

            /*
             * Updating is now turned off so do another update and make sure that the message is NOT displayed in the log (we wait for 4 messages but expect 3). The default wait
             * time for this is 30s which is a bit long but also machine specific so make a note of how long it took to update the config during the last 2 updates and wait for
             * that long.
             */
            long updateDuration = System.currentTimeMillis() - startTime;
            server.setMarkToEndOfLog();
            config.removeApplicationsByName("inexistent");
            server.updateServerConfiguration(config);
            assertNull("The server configuration was updated even though monitoring is disabled", server.waitForStringInLog("CWWKG0017I", updateDuration));
        } finally {
            server.stopServer();
        }
    }

    /**
     * This test just makes sure that if the server config update trigger is set to mbean then it doesn't monitor the file.
     *
     * @throws Exception
     */
    @Test
    public void testMbeanConfigUpdate() throws Exception {
        LibertyServer server = LibertyServerFactory.getStartedLibertyServer("com.ibm.ws.config.update.mbean");

        try {
            // This server has and update trigger set to mbean so the file should not be monitored, update it and make sure a config update isn't triggered
            ServerConfiguration config = server.getServerConfiguration();
            config.getFeatureManager().getFeatures().add("servlet-3.1");
            server.updateServerConfiguration(config);
            //we are waiting for a message not to appear in the log - 10 seconds is probably long enough instead of the default 2 min
            // (note that, for whatever reason, LibertyServer doubles the timeout we pass in)
            assertNull("The server configuration was updated when it shouldn't of been monitoring the file", server.waitForStringInLog("CWWKG0017I", 5000));
        } finally {
            server.stopServer();
        }
    }

    private void test(LibertyServer server, String testUri) throws Exception {
        HttpURLConnection con = null;
        try {
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + testUri);
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            assertNotNull(is);

            String output = read(is);
            System.out.println(output);
            assertTrue(output, output.trim().startsWith("Test Passed"));
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private static String read(InputStream in) throws IOException {
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString();
    }

    private void assertStringsPresentInLog(LibertyServer server, String[] patterns) throws IOException {
        for (String pattern : patterns) {
            String match = server.waitForStringInLog(pattern);
            // Wait for the application to be installed before proceeding
            assertNotNull("No lines found matching the pattern: " + pattern, match);
        }
    }

    private void assertASCIIFileTag(RemoteFile file) throws Exception {

        String systemOS = System.getProperty("os.name");
        if (systemOS.equals("z/OS")) {
            String[] cmdArray = new String[] { "chtag", "-p", file.getAbsolutePath() };
            Process p = Runtime.getRuntime().exec(cmdArray);
            p.waitFor();
            String line = "";
            StringBuffer exMsg = new StringBuffer();
            BufferedReader buferr = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = buferr.readLine()) != null) {
                exMsg.append(line + ". ");
            }
            buferr.close();

            String result[] = exMsg.toString().split("\\s");
            assertEquals("t", result[0]);
            assertEquals("ISO8859-1", result[1]);
        }
    }

    @Test
    public void testCaseSensitivity() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.casesensitivity");
        // shouldn't take long to fail but it should start pretty fast
        server.setServerStartTimeout(5000);
        try {
            server.startServer("casesensitivity.log");
        } finally {
            server.stopServer();
        }
    }

    /**
     * Test a bad required include (<include location="location/that/doesnt/exist.xml"/>)
     * for startup and update. When the variable onError is set to FAIL, the runtime should
     * bring down the server on startup and not recognize ANY config changes on update.
     *
     * @throws Exception
     */
    @Test
    public void testBadRequiredIncludeFAIL() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.import.error");
        ShrinkHelper.exportAppToServer(server, restartApp);
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getServerRoot(), "bootstrap.properties",
                                               server.pathToAutoFVTTestFiles + "/import.error/bootstrap-onError-FAIL.properties", false,
                                               server.getServerRoot());
        server.setServerConfigurationFile("import.error/original.xml");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);

        try {
            // Start out with bad include and onError=FAIL
            server.startServerExpectFailure("include-error.log", true, true);
            server.postStopServerArchive();

            // Now swap in a valid server.xml to test dynamic config update with onError=FAIL
            server.setServerConfigurationFile("import.error/server.xml");

            server.startServer("include-update-error.log");

            // Wait for the application to be installed before proceeding
            assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKZ0001I.* restart"));

            // ensure that the config elements work
            test(server, "/restart/restart?testName=before");

            // switch to new (bad) configuration to test a config update with a bad include
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("import.error/bad-include-server2.xml");

            // cause configuration refresh
            test(server, "/restart/restart?testName=refresh");

            // make sure that both errors messages are present, CWWKG0015E is the summary message
            // that comes out on an update with errors and onError=FAIL
            assertNotNull(server.waitForStringInLogUsingMark("CWWKG0015E"));
            assertNotNull(server.waitForStringInLogUsingMark("CWWKG0090E"));

            // ensure that the config elements were not updated
            test(server, "/restart/restart?testName=before");

        } finally {
            if (server.isStarted()) {
                server.stopServer("CWWKG0015E", "CWWKG0090E");
            }
        }
    }

    /**
     * Test a bad required include (<include location="location/that/doesnt/exist.xml"/>)
     * for startup and update. When the variable onError is not set to FAIL (i.e. INFO, WARN),
     * the runtime should be tolerant and keep the server running while ignoring the bad
     * required include.
     *
     * For a config update, all changes made to the config should be recognized and only
     * the bad required include should be ignored.
     *
     * @throws Exception
     */
    @Test
    public void testBadRequiredIncludeWARN() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.import.error");
        ShrinkHelper.exportAppToServer(server, restartApp);
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);

        try {
            // Change to default setting of onError=WARN, but swap in a server.xml that has
            // malformed xml.
            server.copyFileToLibertyServerRoot("import.error/bootstrap.properties");
            server.setServerConfigurationFile("import.error/malformed-server.xml");

            // Start server with malformed xml and onError=WARN. The server will come up with no configuration,
            // which results in an exception because timed exit isn't enabled.
            boolean caught = false;
            try {
                server.startServer("malformed-xml.log", true, true);
            } catch (TopologyException ex) {
                caught = true;
                server.stopServer("CWWKG0014E", "CWWKF0009W", "CWWKG0090E");
            }

            assertTrue("There should be an exception because timedexit is not enabled", caught);

            // Now provide the original server.xml with a bad required include to ensure
            // that the server comes up and reports the bad include.
            server.setServerConfigurationFile("import.error/bad-include-server.xml");
            server.startServer("include-warn.log");

            // Wait for the application to be installed before proceeding
            assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKZ0001I.* restart"));

            // find the warning in messages.log CWWKG0090E
            List<String> includeErrors = server.findStringsInLogs("CWWKG0090E");
            assertEquals("Should have only seen the include error 1 time upon server startup: " + includeErrors, 1, includeErrors.size());

            // ensure that the config elements work
            test(server, "/restart/restart?testName=before");

            // switch to new (bad) configuration to test a config update with a bad include
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("import.error/bad-include-server2.xml");

            // cause configuration refresh
            test(server, "/restart/restart?testName=refresh");

            // ensure that the error message indicating a bad include was provided is there
            includeErrors = server.findStringsInLogs("CWWKG0090E");
            assertEquals("Should have only seen the include error 2 times since starting the server: " + includeErrors, 2, includeErrors.size());

            // ensure that the modified config elements work
            test(server, "/restart/restart?testName=after");

        } finally {
            if (server.isStarted()) {
                server.stopServer("CWWKG0090E");
            }
        }
    }

    @Test
    public void testBadRequiredIncludeModifyOnError() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.import.error");
        ShrinkHelper.exportAppToServer(server, restartApp);
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);

        try {
            // Change to default setting of onError=WARN, but swap in a server.xml that has
            // malformed xml.
            server.copyFileToLibertyServerRoot("import.error/bootstrap.properties");

            // Now provide the original server.xml with a bad required include to ensure
            // that the server comes up and reports the bad include.
            server.setServerConfigurationFile("import.error/bad-include-server.xml");
            server.startServer("include-warn.log");

            // Wait for the application to be installed before proceeding
            assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKZ0001I.* restart"));

            // find the warning in messages.log CWWKG0090E
            List<String> includeErrors = server.findStringsInLogs("CWWKG0090E");
            assertEquals("Should have only seen the include error 1 time upon server startup: " + includeErrors, 1, includeErrors.size());

            // ensure that the config elements work
            test(server, "/restart/restart?testName=before");

            // Update onError to IGNORE
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("import.error/bad-include-server-onError-ignore.xml");

            // cause configuration refresh
            test(server, "/restart/restart?testName=refresh");

            // ensure that the error message indicating a bad include was provided is there
            includeErrors = server.findStringsInLogs("CWWKG0090E");
            assertEquals("Should have only seen the include error 2 times since starting the server: " + includeErrors, 2, includeErrors.size());

            // switch to new (bad) configuration to test a config update with a bad include
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("import.error/bad-include-server-onError-ignore2.xml");

            // cause configuration refresh
            test(server, "/restart/restart?testName=refresh");

            // ensure that the error message indicating a bad include was provided is there
            includeErrors = server.findStringsInLogs("CWWKG0090E");
            assertEquals("Should not see an include error with onError=IGNORE" + includeErrors, 2, includeErrors.size());

            // ensure that the modified config elements work
            test(server, "/restart/restart?testName=after");

        } finally {
            if (server.isStarted()) {
                server.stopServer("CWWKG0090E");
            }
        }
    }

    @Test
    public void testVariableMissingName() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.import.error");
        ShrinkHelper.exportAppToServer(server, restartApp);
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);

        try {
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getServerRoot(), "bootstrap.properties",
                                                   server.pathToAutoFVTTestFiles + "/import.error/bootstrap-onError-FAIL.properties", false,
                                                   server.getServerRoot());
            server.setServerConfigurationFile("variable.error/missing-name.xml");

            // With OnError=FAIL, the server should fail to start
            server.startServerExpectFailure("variable-name-fail.log", true, true);

            // Change OnError to WARN and try again
            server.copyFileToLibertyServerRoot("import.error/bootstrap.properties");

            server.startServer("variable-name-warn.log", true, true);

            // Wait for the application to be installed before proceeding
            assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKZ0001I.* restart"));

            // find the warning in messages.log CWWKG0091E
            List<String> includeErrors = server.findStringsInLogs("CWWKG0091E");
            assertEquals("Should have only seen the include error 1 time upon server startup: " + includeErrors, 1, includeErrors.size());

            // ensure that the config elements work
            test(server, "/restart/restart?testName=before");

            // switch to new (bad) configuration to test a config update with a bad variable
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("variable.error/missing-name2.xml");

            // cause configuration refresh
            test(server, "/restart/restart?testName=refresh");

            // ensure that the error message indicating a bad variable was provided is there
            includeErrors = server.findStringsInLogs("CWWKG0091E");
            assertEquals("Should have only seen the include error 2 times since starting the server: " + includeErrors, 2, includeErrors.size());

            // ensure that the modified config elements work
            test(server, "/restart/restart?testName=after");

        } finally {
            if (server.isStarted()) {
                server.stopServer("CWWKG0091E");
            }
        }
    }

    @Test
    public void testVariableMissingValue() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.import.error");
        ShrinkHelper.exportAppToServer(server, restartApp);
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);

        try {
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getServerRoot(), "bootstrap.properties",
                                                   server.pathToAutoFVTTestFiles + "/import.error/bootstrap-onError-FAIL.properties", false,
                                                   server.getServerRoot());
            server.setServerConfigurationFile("variable.error/missing-value.xml");

            // With OnError=FAIL, the server should fail to start
            server.startServerExpectFailure("variable-value-fail.log", true, true);

            // Change OnError to WARN and try again
            server.copyFileToLibertyServerRoot("import.error/bootstrap.properties");

            server.startServer("variable-name-warn.log", true, true);

            // Wait for the application to be installed before proceeding
            assertNotNull("The restart application never came up", server.waitForStringInLog("CWWKZ0001I.* restart"));

            // find the warning in messages.log CWWKG0092E
            List<String> includeErrors = server.findStringsInLogs("CWWKG0092E");
            assertEquals("Should have only seen the include error 1 time upon server startup: " + includeErrors, 1, includeErrors.size());

            // ensure that the config elements work
            test(server, "/restart/restart?testName=before");

            // switch to new (bad) configuration to test a config update with a bad variable
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("variable.error/missing-value2.xml");

            // cause configuration refresh
            test(server, "/restart/restart?testName=refresh");

            // ensure that the error message indicating a bad variable was provided is there
            includeErrors = server.findStringsInLogs("CWWKG0092E");
            assertEquals("Should have only seen the include error 2 times since starting the server: " + includeErrors, 2, includeErrors.size());

            // ensure that the modified config elements work
            test(server, "/restart/restart?testName=after");

        } finally {
            if (server.isStarted()) {
                server.stopServer("CWWKG0092E");
            }
        }
    }

}
