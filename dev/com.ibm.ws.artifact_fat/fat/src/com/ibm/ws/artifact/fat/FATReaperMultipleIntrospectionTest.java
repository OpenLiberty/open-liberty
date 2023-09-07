/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.artifact.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import java.beans.Transient;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Assert;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.impl.LibertyServer;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;


@RunWith(FATRunner.class)
@Mode(FULL)
public class FATReaperMultipleIntrospectionTest{
    private static void logInfo(String methodName, String outputString){
        FATLogging.info(FATReaperMultipleIntrospectionTest.class, methodName, outputString);
    }

    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        return FATReaperMultipleIntrospectionTest.class.getClassLoader().loadClass(className);
    }
    
    //
    
    public static final String serverName = "com.ibm.ws.artifact.zipIntrospections";
    
    @Server(FATReaperMultipleIntrospectionTest.serverName)
    public static LibertyServer server;

    private void startServer() throws Exception {
        server.startServer();
    }

    private void performDump() throws Exception {
        server.executeServerScript("dump", null);
    }
    
    private static WebArchive makeHoldingWebArchive(String webAppName, String warName)
            throws Exception {

        WebArchive webArchive = ShrinkWrap.create(WebArchive.class, warName);
        webArchive.addClass( loadClass("com.ibm.ws.artifact.fat.zip.HoldingServlet") );

        String resourcesPath = "test-applications/HoldingServlet/resources/";
        File resourcesFile = new File(resourcesPath);
        if ( resourcesFile.exists() ) {
            webArchive = (WebArchive) ShrinkHelper.addDirectory(webArchive, resourcesPath);
        }

        return webArchive;
    }

    private static void addWebModule(
            String webAppName, String warName,
            WebArchive webArchive) throws Exception{

        ShrinkHelper.exportAppToServer(server, webArchive);
        server.addInstalledAppForValidation(webAppName);
    }

    private static void addWebApps(
            int count,
            List<String> appNames, List<String> warNames) throws Exception {

        // Only update the server configuration once: The update is mildly
        // expensive.  The update becomes actually expensive when a large
        // number of updates is made, which is the case when 45 applications
        // are added.

        ServerConfiguration serverConfig = server.getServerConfiguration();
        
        for ( int appNo = 1; appNo <= count; appNo++) {
            String webAppName = "app" + appNo;
            String warName = webAppName + ".war";
            
            appNames.add(webAppName);
            warNames.add(warName);

            WebArchive webArchive = makeHoldingWebArchive(webAppName, warName);
            addWebModule(webAppName, warName, webArchive);

            String warPath = server.getServerRoot() + "/apps/" + warName;            
            serverConfig.addApplication(webAppName, warPath, "war");
        }

        server.updateServerConfiguration(serverConfig);
    }

    //

    public static File getLatestDumpFile() throws Exception {
        String methodName = "getLatestDumpFile";

        File latestDumpFile = LookupZipArchive.getLatestDump(server);
        Assert.assertNotNull("Server dump not found in [ " + server.getServerRoot() + " ]", latestDumpFile);

        logInfo(methodName, "Located server dump [ " + latestDumpFile.getAbsolutePath() + " ]");
        return latestDumpFile;
    }

    public static String ZIP_INTROSPECTOR_NAME = "ZipCachingIntrospector.txt";

    private static ZipCacheIntrospection getIntrospection(LookupZipArchive dumpArchive) throws Exception {
        Assert.assertTrue(
            "Zip introspection [ " + ZIP_INTROSPECTOR_NAME + " ]" +
            " not found in [ " + dumpArchive.getName() + " ]",
            dumpArchive.hasEntry(ZIP_INTROSPECTOR_NAME) );

        return new ZipCacheIntrospection(
            () -> dumpArchive.getInputStream(ZIP_INTROSPECTOR_NAME) );
    }
    
    private static ZipCacheIntrospection getLatestIntrospection() throws Exception {
        // Keep the dump archive open only as long as necessary.
        try ( LookupZipArchive latestDumpArchive = new LookupZipArchive( getLatestDumpFile() ) ) {
            return getIntrospection(latestDumpArchive);
        }
    }
    
    private static List<String> getLatestZipHandleArchiveNames(String methodName) throws Exception {
        List<String> activeAndCached = getLatestIntrospection().getZipHandleArchiveNames();
        
        logInfo(methodName, "Zip handle archives:");
        logInfo(methodName, "  [ " + activeAndCached + " ]");

        return activeAndCached;
    }

    private static List<String> getLatestActiveAndPending() throws Exception {
        return getLatestIntrospection().getOpenAndActiveArchiveNames();
    }
    
    private static List<String> getLatestActiveAndPending(String methodName) throws Exception {
        List<String> archiveNames = getLatestActiveAndPending();
    
        logInfo(methodName, "Active and pending archives:");
        logInfo(methodName, "  [ " + archiveNames + " ]");

        return archiveNames;
    }

    private static final boolean EXPECT_EMPTY = true;

    private void verifyActiveAndPending(String methodName,
            boolean expectEmpty, String failureMessage) throws Exception {

        performDump();
        List<String> latestActiveAndPending = getLatestActiveAndPending(methodName);
        
        if ( expectEmpty ) {
            Assert.assertTrue(failureMessage, latestActiveAndPending.isEmpty());
        } else {
            Assert.assertFalse(failureMessage, latestActiveAndPending.isEmpty());            
        }
    }
    
    //

    /**
     * Test API: Setup for running tests: Save the initial, blank, server
     * configuration.  After each test, restore to that configuration.
     * 
     * @throws Exception Thrown if the server configuration could not be saved.
     */
    @BeforeClass
    public static void setUpClass() throws Exception{
        server.saveServerConfiguration();
    }

    /**
     * Test API: Tear down a single test: Stop the server, remove all
     * installed applications, then restore the server configuration.
     *  
     * @throws Exception Thrown if any of the tear down steps failed.
     */
    @After
    public void tearDown() throws Exception{
        String methodName = "tearDown";

        if ( (server != null) && server.isStarted() ) {
            server.stopServer();
        }

        server.removeAllInstalledAppsForValidation();
        logInfo(methodName, "Unlinked installed applications");

        server.restoreServerConfiguration();
        logInfo(methodName, "Restored server configuration");

        //

        File appsFile = new File( server.getServerRoot() + "/apps" );
        String appsPath = appsFile.getAbsolutePath();

        logInfo(methodName, "Deleting applications folder [ " + appsPath + " ]");
        server.deleteDirectoryFromLibertyServerRoot("apps");
        Assert.assertFalse("Applications folder still exists", appsFile.exists());

        logInfo(methodName, "Recreating applications folder");
        appsFile.mkdir();
        Assert.assertTrue("Applications folder does not exist", appsFile.exists());
    }

    //
    
    /** Control parameter: Hold web module resources. */    
    private static final boolean DO_HOLD = true;
    
    /** Control parameter: Release web module resources. */    
    private static final boolean DO_RELEASE = false;
    
    /**
     * Hold then release servlet resources.
     *
     * Trim the response text.
     *
     * @param appName The application which is to be reached.
     * @param hold Control parameter. True to acquire resources; false
     *     to release resources.
     *
     * @throws IOException Thrown if the request cannot be processed.
     *
     * @return The response.
     */
    private String doGet(String appName, boolean hold) throws IOException {
        String methodName = "doGet";
        logInfo(methodName, "App [ " + appName + " ] Hold [ " + hold + " ]");
        
        String request = appName + "/" + ( hold ? "?hold" : "?release" );
        logInfo(methodName, "Request [ " + request + " ]");
        
        String response = HttpUtils.getHttpResponseAsString(server, request).trim();
        logInfo(methodName, "Response [ " + response + " ]");

        return response;
    } 

    private void verifyGet(
        String appName, boolean hold,
        String expectedResponse, String failureMessage) throws IOException {

        String actualResponse = doGet(appName, hold); // throws IOException

        Assert.assertEquals(failureMessage, expectedResponse, actualResponse);
    }
    
    // import componenttest.custom.junit.runner.Mode.TestMode;    
    /**
     * Case: Verify that all application archives are handled
     *       when adding many applications.
     */
    @Test
    public void testManyApps() throws Exception {
        String methodName = "checkManyApps";

        int numApps = 45;
        List<String> webAppNames = new ArrayList<>(numApps);
        List<String> warNames = new ArrayList<>(numApps);
        addWebApps(numApps, webAppNames, warNames);

        startServer();

        performDump();
        List<String> archiveNames = getLatestZipHandleArchiveNames(methodName);

        logInfo(methodName, "Expected [ " + warNames + " ]");
        logInfo(methodName, "  ... plus copies of [ TestJar.jar ]");
        logInfo(methodName, "Actual [ " + archiveNames + " ]");

        for ( String warName : warNames ) {
            Assert.assertTrue(
                    "Failed to locate archive [ " + warName + "]",
                    archiveNames.remove(warName));
            Assert.assertTrue(
                    "Failed to locate [ TestJar.jar ] for [ " + warName + " ]",
                    archiveNames.remove("TestJar.jar"));
        }
    }

    /**
     * Case: Verify application archives are moved between open and
     *       closed states by application activity.
     */
    @Test
    public void testMoveToOpen() throws Exception{
        String methodName = "testMoveToOpen";

        int numApps = 2;
        List<String> webAppNames = new ArrayList<>(numApps);
        List<String> warNames = new ArrayList<>(numApps);
        addWebApps(numApps, webAppNames, warNames);

        startServer();

        boolean isFirst = true;
        
        for ( String webAppName : webAppNames ) {
            logInfo(methodName, "Testing [ " + webAppName + " ]");

            if ( isFirst ) {
                isFirst = false; // Redundant with the prior loop.

                logInfo(methodName, "PRE-HOLD: Verifying inactive archives.");
                verifyActiveAndPending(methodName,
                        EXPECT_EMPTY,
                        "Non-empty active and pending (pre-hold)");
            }

            logInfo(methodName, "POST-HOLD: Verifying active archives.");
            verifyGet(webAppName, DO_HOLD,
                    "hold: Success",
                    "Unexpected post-hold response");
            verifyActiveAndPending(methodName,
                    !EXPECT_EMPTY,
                    "Expecting at least two active post-hold archives");
                    
            logInfo(methodName, "POST-RELEASE: Verifying active archives.");

            verifyGet(webAppName, DO_RELEASE,
                    "release: Success",
                    "Unexpected post-release response");

            // Sleep to give the server enough time to process the released
            // resources.  10 seconds is approximate.  Adjust as needed.

            int sleepSec = 10;
            logInfo(methodName, "POST-RELEASE: Sleep for [ " + sleepSec + " ] seconds");
            Thread.sleep(sleepSec * 1000);

            verifyActiveAndPending(methodName,
                    EXPECT_EMPTY,
                    "Non-empty active and pending post-release archives");
        }
    }
}
