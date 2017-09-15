/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.metatype.helper.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LibertyServerUtils;

public class FATTest {

    private static final LibertyServer server = LibertyServerFactory
                    .getLibertyServer("com.ibm.ws.kernel.metatype.helperServer");
    private static final String baseURL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                                          + "/fileset";
    private final Class<?> c = FATTest.class;

    // Servlet Strings
    private static final String BASE = "This is Fileset servlet.";
    private static final String BASE_ERROR = "The servlet was not available or did not return the expected content";
    private static final String SERVICE = "Found 1 fileset service.";
    private static final String SERVICE_ERROR = "The Fileset service was not found, or too many were found.";
    private static final String FILES = "Fileset contained file: ";
    private static final String FILES_ERROR = "The fileset retrieved by the servlet did not match that expected.";

    private static final String TEST_FOLDER = "usr/shared/resources/fileset_test_bundles";
    private static final String GEN_JAR_NAME = "test3.jar";
    private static final String GEN_JAR_PATH = TEST_FOLDER + "/" + GEN_JAR_NAME;

    private static final String INIT_MSG_RE = "FileMonitor init completed for fileset";
    private static final String SCAN_MSG_RE = "FileMonitor scan completed for fileset";

    // default to timeout after 5 seconds for the servlet content to be what we expect
    // and for log/trace messages we are waiting for to appear
    private static final int CONTENT_TIMEOUT = 5;

    @BeforeClass
    public static void setUp() throws Exception {
        // clean up from any failed runs
        cleanUpGeneratedFiles();
    }

    @Rule
    public TestName name = new TestName();

    public String testName = "";
    public String currentLog = "";

    @Before
    public void setServerConfig() throws Exception {
        // set the current test name
        testName = name.getMethodName();

        Log.info(c, testName, "=== Preparing for test " + testName + " ===");
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/filesetfatlibertyinternals-1.0.mf");

        server.startServer(testName + "_console.log");
        Log.info(c, testName, "===== Starting test " + testName + " =====");
    }

    @After
    public void cleanUpAfter() throws Exception {
        Log.info(c, testName, "=== Cleaning up from test " + testName + " ===");

        cleanUpGeneratedFiles();

        //stop the server and collect the logs
        server.stopServer(true);

        // switch back to the monitored_fileset/server.xml after each test
        setConfigurationFile("monitored_fileset");
    }

    private static void cleanUpGeneratedFiles() throws Exception {
        server.deleteFileFromLibertyInstallRoot(GEN_JAR_PATH);
    }

    @AfterClass
    public static void cleanUpSharedFolder() throws Exception {
        server.deleteFileFromLibertyInstallRoot(TEST_FOLDER);
    }

    @Test
    public void testServlet() throws Exception {
        String[] expected = new String[] { BASE };

        verifyContent("", expected, BASE_ERROR);
    }

    @Test
    public void testFilesetService() throws Exception {
        String[] expected = new String[] { BASE, SERVICE };

        verifyContent("Service", expected, SERVICE_ERROR);
    }

    @Test
    public void testFilesetFiles() throws Exception {

        verifyJarsOneAndTwo();
    }

    @Test
    public void testFilesetFilesUpdatedByMonitor() throws Exception {

        // check the fileset has only one and two
        verifyJarsOneAndTwo();

        // now copy three into place
        copyTestJar();

        // wait for the scan complete message
        waitForTraceMessage(SCAN_MSG_RE);

        // verify that the fileset has jars one, two and three
        verifyJarsOneTwoThree();
    }

    @Test
    public void testFilesetFilesMonitorDisabled() throws Exception {

        // hack to give time for setup to copy files over since monitor will be disabled. This 
        // should ensure the server gets a snapshot of the directory after the setup copy 
        // has completed. Ideally it would be good to have some mechanism to detect when setup has 
        // completed but since the problem is occurring frequently  the tactical hack
        // was opted for to try and keep the builds green.
        try {
            Thread.sleep(1000);
        } catch (Exception ex) {
        }

        // change the configuration to disable monitoring and wait for the
        // update trace
        setConfigurationFileAndWait("unmonitored_fileset");

        // verify that the fileset contains jars one and two
        verifyJarsOneAndTwo();

        // move the test3.jar into the fileset location
        copyTestJar();

        // check that the test3.jar has not appeared in the list of fileset
        // files because we are not monitoring
        verifyJarsOneAndTwo();

        // since we are not scanning there aren't really any messages we can
        // check for
        // instead we'll just verify again that the third jar hasn't appeared
        verifyJarsOneAndTwo();
    }

    @Test
    public void testFilesetFilesMonitorReenabled() throws Exception {

        // disable the monitor and check for jars one and two
        setConfigurationFileAndWait("unmonitored_fileset");
        verifyJarsOneAndTwo();

        // update the config to re-enable the monitor
        setConfigurationFileAndWait("monitored_fileset");

        // move the test3.jar into the fileset location
        copyTestJar();

        //we should see two init complete messages, one from when the server came up
        //and a second for when monitoring was re-enabled.
        waitForTraceToMatchExpression(".*" + INIT_MSG_RE + ".*" + INIT_MSG_RE + ".*");

        // check that the fileset has been updated
        verifyJarsOneTwoThree();

    }

    @Test
    public void testCommaSeparatedFiles() throws Exception {

        // move the test3.jar into the fileset location
        copyTestJar();

        //verify that fileset can be specified with comma separated jar files
        setConfigurationFileAndWait("fileset_list");
        verifyJarsOneTwoThree();
    }

    @Test
    // recursion is no longer supported
    @Ignore
    public void testFilesetFilesUpdatedByConfig() throws Exception {

        // set the modified server.xml to be the server configuraiton
        setConfigurationFileAndWait("config_change_fileset");

        // now check that the fileset returns jars one, two and four
        String[] expected = new String[] { BASE, FILES + "test1.jar", FILES + "test2.jar", FILES + "test4.jar" };

        verifyContent("Files", expected, FILES_ERROR);
    }

    private void copyTestJar() throws Exception {
        // process to get a new jar into the resource folder
        // use the FAT utility to copy the test3.jar to the install root
        server.copyFileToLibertyInstallRoot(GEN_JAR_NAME);
        // copy form the install root to the resources location
        RemoteFile jarDest = new RemoteFile(server.getMachine(), LibertyServerUtils.makeJavaCompatible(server
                        .getInstallRoot()
                                                                                                       + "/" + GEN_JAR_PATH));
        RemoteFile rootJar = server.getFileFromLibertyInstallRoot(GEN_JAR_NAME);
        assertTrue("Copy of the generated jar file failed, the test will fail.", rootJar.copyToDest(jarDest));
        // now delete the original in the install root
        if (rootJar.exists())
            rootJar.delete();

        // now sleep for a second to allow the FileMonitor to run (it is set to
        // run every 500ms)
        Thread.sleep(1000);
    }

    private List<String> getContent(String urlQuerySuffix) throws Exception {
        String logMsg = "getContent - " + testName;
        String suffix = ("".equals(urlQuerySuffix)) ? "" : "?" + urlQuerySuffix;
        URL url = new URL(baseURL + suffix);
        Log.info(c, logMsg, "Calling servlet URL=" + url.toString());

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        // read the page contents
        String line = br.readLine();
        List<String> lines = new ArrayList<String>();
        while (line != null) {
            lines.add(line);
            line = br.readLine();
        }
        con.disconnect();
        Log.info(c, logMsg, "Retrieved content: " + lines);
        return lines;
    }

    private void verifyContent(String urlQuerySuffix, String[] expect, String message) throws Exception {

        List<String> actualContent = null;
        Collection<String> expectedContent = Arrays.asList(expect);

        long timeout = System.currentTimeMillis() + (CONTENT_TIMEOUT * 1000);
        while (!!!expectedContent.equals(actualContent) && System.currentTimeMillis() < timeout) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                /* ignore */
            }
            actualContent = getContent(urlQuerySuffix);
        }

        assertEquals(message, expectedContent, actualContent);
    }

    private void verifyJarsOneAndTwo() throws Exception {
        String[] expected = new String[] { BASE, FILES + "test1.jar", FILES + "test2.jar" };

        verifyContent("Files", expected, FILES_ERROR);
    }

    private void verifyJarsOneTwoThree() throws Exception {
        String[] expected = new String[] { BASE, FILES + "test1.jar", FILES + "test2.jar", FILES + "test3.jar" };

        verifyContent("Files", expected, FILES_ERROR);
    }

    private void setConfigurationFile(String serverxmlPathPrefix) throws Exception {
        // set the server configuration file
        server.setServerConfigurationFile(serverxmlPathPrefix + "/server.xml");
    }

    private void setConfigurationFileAndWait(String serverxmlPathPrefix) throws Exception {
        setConfigurationFile(serverxmlPathPrefix);

        //wait 5 seconds for the config updated or config didn't change messages
        server.waitForStringInLog("CWWKG0017|CWWKG0018", 5000);
    }

    private RemoteFile getTraceFile() throws Exception {
        // first wait up to 5 seconds for the log file to appear
        RemoteFile logFile = null;
        long timeout = System.currentTimeMillis() + 5000;
        while (logFile == null && System.currentTimeMillis() < timeout) {
            logFile = server.getMatchingLogFile("trace.*");
            if (logFile == null) {
                Thread.sleep(500);
            }
        }
        return logFile;
    }

    private void waitForTraceMessage(String msgToWaitFor) throws Exception {
        // search the trace log to check for the update
        String found = server.waitForStringInLog(msgToWaitFor, 5000, getTraceFile());
        if (found == null) {
            Log.warning(c, "Did not find the string \"" + msgToWaitFor + "\" in the log");
        } else {
            Log.info(c, "waitForTraceMessage", "Found message " + msgToWaitFor);
        }
    }

    private void waitForTraceToMatchExpression(String regEx) throws Exception {

        RemoteFile traceFile = getTraceFile();

        StringBuilder traceContent = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(traceFile.openForReading()));
        try {
            long timeout = System.currentTimeMillis() + (CONTENT_TIMEOUT * 1000);
            boolean matched = false;
            Pattern p = Pattern.compile(regEx);
            while (System.currentTimeMillis() < timeout) {
                String nextLine = reader.readLine();
                if (nextLine != null) {
                    traceContent.append(nextLine);
                } else {
                    //if we reached the end of the file we should wait a while before looping again
                    Thread.sleep(100);
                }
                Matcher m = p.matcher(traceContent.toString());
                if (m.matches()) {
                    matched = true;
                    break;
                }
            }

            // search the trace log to check for the update
            if (!!!matched) {
                Log.warning(c, "Did not find the expression \"" + regEx + "\" in the log");
            } else {
                Log.info(c, "waitForTraceMessages", "Found expression " + regEx);
            }
        } finally {
            reader.close();
        }
    }

}
