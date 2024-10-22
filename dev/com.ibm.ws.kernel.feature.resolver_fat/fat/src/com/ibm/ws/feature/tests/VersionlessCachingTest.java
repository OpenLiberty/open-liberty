/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.feature.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class VersionlessCachingTest {

    public static final String SERVER_NAME = "VersionlessCachingTestServer";
    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static final boolean CLEAN_START = true; // pass to server start to start with --clean option
    public static final boolean DIRTY_START = false; // pass to server start to start without --clean option

    public static final boolean SET_LOG_MARK = true;
    public static final boolean DONT_SET_LOG_MARK = false;

    private static LibertyServer server;
    private static String[] allowedMessages = null;
    private static boolean TEST_PASSED = false;

    @BeforeClass
    public static void beforeClass() throws Exception {
        createServerDirectory();
    }

    @Before
    public void before() throws Exception {
        //
    }

    @After
    public void after() throws Exception {
        System.out.println(" --- AFTER TEST ---");
        if (!TEST_PASSED) {
            System.out.println(" --------------- >> TEST FAILED");
            displayFile(new File(server.getLogsRoot() + "/console.log"));
        }
        stopServer(allowedMessages);
        allowedMessages = null;
        TEST_PASSED = false;
    }

    /**
     * Test that adding a feature is noticed after restarting server
     *
     * setup:
     * - platform: javaee-7.0 in server.xml.
     * - features: ejb (versionless)
     * - No env var set
     * - start server
     *
     * test:
     * - add servlet to server.xml
     * - ensure that servlet-3.1 was added
     */
    @Test
    public void addedFeatureIsNoticedWhileServerRunning() throws Exception {
        displayTestStartMessage("addedFeatureIsNoticedWhileServerRunning");
        createServer("javaee-7.0", "ejb", null);
        startServer(CLEAN_START, SET_LOG_MARK);

        // UPDATE CONFIG - adding servlet
        displayHeading('*', "Update config by adding versionless feature \"servlet\"");
        List<String> platforms = Arrays.asList("javaee-7.0");
        List<String> features = Arrays.asList("ejb", "servlet");
        createServerXml(server.getServerRoot(), platforms, features);

        // WAIT FOR THE RESULT
        String lineInLog = waitForStringInLog(RESOLUTION_MSG);
        String expectedFeature = "servlet-3.1";
        assertNotNull("Resolution message, " + RESOLUTION_MSG + " not found.", lineInLog);
        assertTrue("Resolution message should contain " + expectedFeature + ".", lineInLog.contains(expectedFeature));

        System.out.println("PASSED -  addedFeatureIsNoticedWhileServerRunning");
        TEST_PASSED = true;
    }

    /**
     * Test that adding a feature is noticed after restarting server
     *
     * setup:
     * - platform: javaee-7.0 in server.xml.
     * - features: ejb (versionless)
     * - No env var set
     * - start & stop server
     *
     * test:
     * - add servlet to server.xml
     * - start server (dirty)
     * - ensure that servlet-3.1 was added
     */
    @Test
    public void addedFeatureIsNoticedAfterStart() throws Exception {
        displayTestStartMessage("addedFeatureIsNoticedAfterStart");
        createServer("javaee-7.0", "ejb", null);
        startServer(CLEAN_START, SET_LOG_MARK);
        stopServer(allowedMessages);

        // UPDATE CONFIG - adding servlet
        displayHeading('*', "Update config by adding versionless feature \"servlet\"");
        List<String> platforms = Arrays.asList("javaee-7.0");
        List<String> features = Arrays.asList("ejb", "servlet");
        createServerXml(server.getServerRoot(), platforms, features);

        startServer(DIRTY_START, DONT_SET_LOG_MARK);

        // WAIT FOR THE RESULT
        String lineInLog = waitForStringInLog(RESOLUTION_MSG);
        String expectedFeature = "servlet-3.1";
        assertNotNull("Resolution message, " + RESOLUTION_MSG + " not found.", lineInLog);
        assertTrue("Resolution message should contain " + expectedFeature + ".", lineInLog.contains(expectedFeature));

        System.out.println("PASSED - addedFeatureIsNoticedAfterStart");
        TEST_PASSED = true;
    }

    /**
     * Test that if we remove a feature from server.xml while server is stopped,
     * it is noticed after restart.
     *
     * setup:
     * - platform: javaee-7.0 in server.xml.
     * - features: ejb, servlet (versionless)
     * - No env var set
     * - start & stop server
     *
     * test:
     * - remove servlet from server.xml
     * - start server (dirty)
     * - ensure that servlet-3.1 was removed
     */
    @Test
    public void removedFeatureIsNoticedAfterStart() throws Exception {
        displayTestStartMessage("removedFeatureIsNoticedAfterStart");
        createServer("javaee-7.0", "ejb, servlet", null);
        startServer(CLEAN_START, SET_LOG_MARK);
        stopServer(allowedMessages);

        // UPDATE CONFIG - server stopped
        displayHeading('*', "Update config by removing versionless feature \"servlet\"");
        List<String> platforms = Arrays.asList("javaee-7.0");
        List<String> features = Arrays.asList("ejb"); // removes servlet
        createServerXml(server.getServerRoot(), platforms, features);

        startServer(DIRTY_START, DONT_SET_LOG_MARK);

        // WAIT FOR THE RESULT
        System.out.println("waiting for string in log --- ");
        String lineInLog = waitForStringInLog(RESOLUTION_MSG);
        assertNotNull("Resolution message, " + RESOLUTION_MSG + " not found.", lineInLog);
        String unExpectedFeature = "servlet-3.1";
        assertFalse("Resolution still contains " + unExpectedFeature, lineInLog.contains(unExpectedFeature));

        System.out.println("PASSED - removedFeatureIsNoticedAfterStart");
        TEST_PASSED = true;
    }

    /**
     * Test that if we change the platform in a running server, the change is noticed.
     *
     * setup:
     * - platform: javaee-8.0 in server.xml.
     * - feature: jsp
     * - No env var set
     * - start server
     *
     * test:
     * - change plaform to jakartaee-9.1
     * - check for feature servlet-5.0
     */
    @Test
    public void changeInPlatformIsNoticedWhileServerRunning() throws Exception {
        displayTestStartMessage("changeInPlatformIsNoticedWhileServerRunning");
        createServer("javaee-8.0", "jsp", null);
        startServer(CLEAN_START, SET_LOG_MARK);

        // UPDATE CONFIG - server running
        displayHeading('*', "Update config by adding changing platform to  \"jakartaee-9.1\"");
        List<String> platforms = Arrays.asList("jakartaee-9.1");
        List<String> features = Arrays.asList("jsp");
        createServerXml(server.getServerRoot(), platforms, features);

        // WAIT FOR RESULT
        String expectedFeature = "servlet-5.0";
        String lineInLog = waitForStringInLog(RESOLUTION_MSG);
        assertNotNull("Resolution message, " + RESOLUTION_MSG + " not found.", lineInLog);
        assertTrue("Resolution message should contain " + expectedFeature + ".", lineInLog.contains(expectedFeature));

        System.out.println("PASSED - changeInPlatformIsNoticedWhileServerRunning");
        TEST_PASSED = true;
    }

    /**
     * Test that if we change the platform in server.xml it is noticed
     * upon restart and we don't use the cached config.
     *
     * setup:
     * - platform: javaee-8.0 in server.xml.
     * - feature: jsp
     * - No env var set
     * - start & stop server
     *
     * test:
     * - change plaform to jakartaee-9.1
     * - start server (dirty)
     * - check for feature servlet-5.0
     */
    @Test
    public void changeInPlatformIsNoticedAfterServerStart() throws Exception {
        displayTestStartMessage("changeInPlatformIsNoticedAfterServerStart");
        createServer("javaee-8.0", "jsp", null);
        startServer(CLEAN_START, SET_LOG_MARK);

        stopServer(allowedMessages);

        // UPDATE CONFIG - server stopped
        displayHeading('*', "Update config by changing platform to  \"jakartaee-9.1\"");
        List<String> platforms = Arrays.asList("jakartaee-9.1");
        List<String> features = Arrays.asList("jsp");
        createServerXml(server.getServerRoot(), platforms, features);

        startServer(DIRTY_START, DONT_SET_LOG_MARK);

        // WAIT FOR RESULT
        String expectedFeature = "servlet-5.0";
        String lineInLog = waitForStringInLog(RESOLUTION_MSG);
        assertNotNull("Resolution message, " + RESOLUTION_MSG + " not found.", lineInLog);
        assertTrue("Resolution message should contain " + expectedFeature + ".", lineInLog.contains(expectedFeature));

        System.out.println("PASSED - changeInPlatformIsNoticedAfterServerStart");
        TEST_PASSED = true;
    }

    /**
     * Test ensures cache is not being used if we make a change to server.env. This test
     * uses a different method to start the server, since the usual server.start() does
     * not cause the server script to be run, and therefore does not cause server.env
     * to be read.
     *
     * setup:
     * - no platform in server.xml
     * - env var set to javaee-8.0
     * - start and stop server.
     *
     * part 1:
     * - change env var to jakartaee-9.1 in server.env
     * - start server (must be dirty start)
     * - check that servlet 5.0 is installed
     * - stop server
     *
     * part 2:
     * - unset env var
     * - start server (must be dirty start)
     * - check for error msg, because no plaform is defined
     */
    @Test
    public void changeInPreferredPlatformVariableIsNoticedAfterServerStart() throws Exception {
        displayTestStartMessage("changeInPreferredPlatformVariableIsNoticedAfterServerStart");

        // Not entirely sure this OS check is necessary.  I think it was for variable expansion
        // in the server.env, but this test does not rely on variable expansion/resolution
        // Implementation will not work on Windows because server.env is not executed on Windows.
        // There are some ASCII - EBCDIC issues that would have to be worked out for Z/OS.
        if (!OS.contains("linux") && !OS.contains("mac os")) {
            System.out.println("Returning.  Test not valid on " + OS);
            TEST_PASSED = true;
            return;
        }

        // SETUP - no platform in server.xml, but env var is set in server.env
        createServer(null, "jsp", "javaee-8.0");
        startServerUsingServerScript(null);
        assertTrue("the server should have been started", server.isStarted());
        stopServer(allowedMessages);

        // PART 1: UPDATE CONFIG - while server stopped. Add environment variable
        displayHeading('*', "Part 1: Update config by changing PREFERRED_PLATFORM_VERSIONS to \"jakartaee-9.1\"");
        createServerEnv(server.getInstallRoot() + "/usr/servers/" + SERVER_NAME, "jakartaee-9.1");

        startServerUsingServerScript(null);

        // WAIT FOR RESULT
        String expectedFeature = "servlet-5.0";
        String lineInLog = waitForStringInLog(RESOLUTION_MSG);
        assertNotNull("Resolution message, " + RESOLUTION_MSG + " not found.", lineInLog);
        assertTrue("Resolution message should contain " + expectedFeature + ".", lineInLog.contains(expectedFeature));

        // END of PART 1
        displayHeading('*', "Part 1 PASSED ... stopping server");
        stopServer(allowedMessages);

        // Part 2: UPDATE CONFIG - while server stopped.  Remove the environment variable
        displayHeading('*', "Part 2: Update config by unsetting PREFERRED_PLATFORM_VERSIONS=");
        createServerEnv(server.getInstallRoot() + "/usr/servers/" + SERVER_NAME, "");
        displayFile(new File(server.getInstallRoot() + "/usr/servers/" + SERVER_NAME, "server.xml"));

        startServerUsingServerScript(null);

        // WAIT FOR RESULT  -- Expect this to fail because there is NO platform
        String expectedMessage = "CWWKF0055E"; //"[ERROR   ] CWWKF0055E: The [jsp] versionless features do not have a configured platform."
        lineInLog = waitForStringInLog(expectedMessage);
        assertNotNull("Resolution message, " + expectedMessage + " not found.", lineInLog);

        System.out.println("PASSED - changeInPreferredPlatformVariableIsNoticedAfterServerStart");
        TEST_PASSED = true;
        allowedMessages = new String[] { "CWWKF0050E", "CWWKF0055E", "CWWKF0048E" };
    }

    /**
     * Test ensures cache is not being used if we remove a platform and DO have a
     * PREFERRED_PLATFORM_VERSIONS env var set. This test
     * uses a different method to start the server, since the usual server.start() does
     * not cause the server script to be run, and therefore does not cause server.env
     * to be read.
     *
     * setup:
     * - platform in server.xml AND environment variable set to a different platform
     * (note server.xml overrides the environment variable.
     * - platform in server.xml set to jakartaee-9.1
     * - env var set to javaee-8.0
     * - feature set to jsp
     * - start server - assert servlet-5.0 installed
     * - stop server
     *
     * test:
     * - remove platform from server.xml
     * - start server (must be dirty start)
     * - check that servlet 4.0 is installed since we'll be using javaee-8.0 from the env var
     */
    @Test
    public void removePlatformIsNoticedAfterServerStart() throws Exception {
        displayTestStartMessage("removePlatformIsNoticedAfterServerStart");

        // Not entirely sure this OS check is necessary.  I think it was for variable expansion
        // in the server.env, but this test does not rely on variable expansion/resolution.
        // Implementation will not work on Windows because server.env is not executed on Windows.
        // There are some ASCII - EBCDIC issues that would have to be worked out for Z/OS.
        if (!OS.contains("linux") && !OS.contains("mac os")) {
            System.out.println("Returning.  Test not valid on " + OS);
            TEST_PASSED = true;
            return;
        }

        // SETUP
        createServer("jakartaee-9.1", "jsp", "javaee-8.0");
        displayFile(new File(server.getInstallRoot() + "/usr/servers/" + SERVER_NAME, "server.xml"));
        startServerUsingServerScript(null);
        assertTrue("the server should have been started", server.isStarted());
        String expectedFeature = "servlet-5.0";
        String lineInLog = waitForStringInLog(RESOLUTION_MSG);
        assertNotNull("Resolution message, " + RESOLUTION_MSG + " not found.", lineInLog);
        assertTrue("Resolution message should contain " + expectedFeature + ".", lineInLog.contains(expectedFeature));
        stopServer(allowedMessages);

        // UPDATE CONFIG - while server stopped. Remove platform from server.xml
        displayHeading('*', "Update config by removing platform from server.xml");
        createServerXml(server.getInstallRoot() + "/usr/servers/" + SERVER_NAME,
                        null, // no platform
                        Arrays.asList("jsp"));

        startServerUsingServerScript(null);

        // WAIT FOR RESULT
        expectedFeature = "servlet-4.0"; // because platform is now from the env var
        lineInLog = waitForStringInLog(RESOLUTION_MSG);
        assertNotNull("Resolution message, " + RESOLUTION_MSG + " not found.", lineInLog);
        assertTrue("Resolution message should contain " + expectedFeature + ".", lineInLog.contains(expectedFeature));
        System.out.println("PASSED - removePlatformIsNoticedAfterServerStart");
        TEST_PASSED = true;
    }

    /**
     * Test ensures cache is not being used if we remove a platform and DO have a
     * PREFERRED_PLATFORM_VERSIONS env var set. This test
     * uses a different method to start the server, since the usual server.start() does
     * not cause the server script to be run, and therefore does not cause server.env
     * to be read.
     *
     * setup:
     * - no platform in server.xml
     * - env var set to javaee-8.0
     * - feature set to jsp
     * - start server - assert servlet-4.0 installed
     * - stop server
     * test:
     * - add platform (jakartaee-9.1) to server.xml
     * - start server (must be dirty start)
     * - check that servlet 5.0 is installed since we'll be using jakartaee-9.1 from the server.xml
     */
    @Test
    public void addPlatformIsNoticedAfterServerStart() throws Exception {
        displayTestStartMessage("addPlatformIsNoticedAfterServerStart");

        // Not entirely sure this OS check is necessary.  I think it was for variable expansion
        // in the server.env, but this test does not rely on variable expansion/resolution.
        // Implementation will not work on Windows because server.env is not executed on Windows.
        // There are some ASCII - EBCDIC issues that would have to be worked out for Z/OS.
        if (!OS.contains("linux") && !OS.contains("mac os")) {
            System.out.println("Returning.  Test not valid on " + OS);
            TEST_PASSED = true;
            return;
        }

        // SETUP
        createServer(null, "jsp", "javaee-8.0");
        startServerUsingServerScript(null);
        assertTrue("the server should have been started", server.isStarted());
        String expectedFeature = "servlet-4.0";
        String lineInLog = waitForStringInLog(RESOLUTION_MSG);
        assertNotNull("Resolution message, " + RESOLUTION_MSG + " not found.", lineInLog);
        assertTrue("Resolution message should contain " + expectedFeature + ".", lineInLog.contains(expectedFeature));
        stopServer(allowedMessages);

        // UPDATE CONFIG - while server stopped. Remove platform from server.xml
        displayHeading('*', "Update config by removing platform from server.xml");
        createServerXml(server.getInstallRoot() + "/usr/servers/" + SERVER_NAME,
                        Arrays.asList("jakartaee-9.1"), // platform
                        Arrays.asList("jsp")); // feature

        startServerUsingServerScript(null);

        // WAIT FOR RESULT
        expectedFeature = "servlet-5.0"; // because platform is now in server.xml
        lineInLog = waitForStringInLog(RESOLUTION_MSG);
        assertNotNull("Resolution message, " + RESOLUTION_MSG + " not found.", lineInLog);
        assertTrue("Resolution message should contain " + expectedFeature + ".", lineInLog.contains(expectedFeature));
        System.out.println("PASSED - addPlatformIsNoticedAfterServerStart");
        TEST_PASSED = true;
    }

    /**
     * Test ensures cache is not being used if we modify PREFERRED_PLATFORM_VERSIONS env var.
     * This test uses a different method to start the server, since the usual server.start()
     * does not cause the server script to be run, and therefore does not cause server.env
     * to be read.
     *
     * setup:
     * - no platform in server.xml
     * - env var set to javaee-8.0
     * - feature set to jsp
     * - start server - assert servlet-4.0 installed
     * - stop server
     * test:
     * - update preferred platform versions in server.env to (jakartaee-9.1, javaee-8.0) to server.xml
     * - start server (must be dirty start)
     * - check that servlet 5.0 is installed since we'll be using jakartaee-9.1 from the env var
     */
    @Test
    public void preferredPatformValueChangeIsNoticedAfterServerStart() throws Exception {
        displayTestStartMessage("preferredPatformValueChangeIsNoticedAfterServerStart");

        // Not entirely sure this OS check is necessary.  I think it was for variable expansion
        // in the server.env, but this test does not rely on variable expansion/resolution.
        // Implementation will not work on Windows because server.env is not executed on Windows.
        // There are some ASCII - EBCDIC issues that would have to be worked out for Z/OS.
        if (!OS.contains("linux") && !OS.contains("mac os")) {
            System.out.println("Returning.  Test not valid on " + OS);
            TEST_PASSED = true;
            return;
        }

        // SETUP
        createServer(null, "jsp", "javaee-8.0");
        startServerUsingServerScript(null);
        assertTrue("the server should have been started", server.isStarted());
        String expectedFeature = "servlet-4.0";
        String lineInLog = waitForStringInLog(RESOLUTION_MSG);
        assertNotNull("Resolution message, " + RESOLUTION_MSG + " not found.", lineInLog);
        assertTrue("Resolution message should contain " + expectedFeature + ".", lineInLog.contains(expectedFeature));
        stopServer(allowedMessages);

        // UPDATE CONFIG - while server stopped. Remove platform from server.xml
        displayHeading('*', "Update config by changing variable PREFERRED_PLATFORM_VERSIONS");
        createServerEnv(server.getInstallRoot() + "/usr/servers/" + SERVER_NAME, "jakartaee-9.1,javaee-8.0");

        startServerUsingServerScript(null);

        // WAIT FOR RESULT
        expectedFeature = "servlet-5.0"; // because platform is now in server.xml
        lineInLog = waitForStringInLog(RESOLUTION_MSG);
        assertNotNull("Resolution message, " + RESOLUTION_MSG + " not found.", lineInLog);
        assertTrue("Resolution message should contain " + expectedFeature + ".", lineInLog.contains(expectedFeature));
        System.out.println("PASSED - preferredPatformValueChangeIsNoticedAfterServerStart");
        TEST_PASSED = true;
    }

//            ------------------------------------------------------------------------
// END TESTs  ------------------------------------------------------------------------
//            ------------------------------------------------------------------------

    public static void createInputData(String directory, List<String> platforms, List<String> features, String preferredPlatformVersions) throws IOException {
        createServerXml(directory, platforms, features);
        if (preferredPlatformVersions != null && !preferredPlatformVersions.isEmpty()) {
            createServerEnv(directory, preferredPlatformVersions);
        }

        // Call this to enable beta flag
        //createJvmOptions(directory);
    }

    // Call this to enable beta flag
    @SuppressWarnings("unused")
    private static void createJvmOptions(String directory) throws IOException {
        File file = new File(directory, "jvm.options");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("-Dcom.ibm.ws.beta.edition=true\n");
        }
        displayFile(file);
    }

    private static void createServerEnv(String directory, String preferredPlatformVersions) throws IOException {
        File file = new File(directory, "server.env");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("PREFERRED_PLATFORM_VERSIONS=" + preferredPlatformVersions + "\n");
        }
        displayFile(file);
    }

    /**
     * Creates and configures server in autoFVT/publish/servers/ dir. Then copies the server
     * to build.image/wlp/usr/servers/<SERVER_NAME> dir.
     *
     * @param platforms - comma separated list of platforms to be inserted in <platform> elements in server.xml
     * @param features - comma separated list of features to be inserted in <feature> elements in server.xml
     * @param preferredPlatformVersions - Value to set for PREFERRED_PLATFORMS_VARIABLE environment
     * @throws Exception
     */
    private static void createServer(String platforms,
                                     String features,
                                     String preferredPlatformVersions) throws Exception {

        File serverDir = createServerDirectory();

        // Configure server
        List<String> platformList = (platforms != null) ? Arrays.asList(platforms.split(",")) : null;
        List<String> featureList = (features != null) ? Arrays.asList(features.split(",")) : null;
        createInputData(serverDir.getAbsolutePath(), platformList, featureList, preferredPlatformVersions);

        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
    }

    private static void createServerXml(String directory, List<String> platforms, List<String> features) throws IOException {
        File file = new File(directory, "server.xml");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("<server>\n");
            writer.write("\n");
            writer.write("    <featureManager>\n");
            if (platforms != null) {
                for (String platform : platforms) {
                    if (platform != null && !platform.isEmpty()) {
                        writer.write("        <platform>" + platform.trim() + "</platform>\n");
                    }
                }
            }
            if (features != null) {
                for (String feature : features) {
                    if (feature != null && !feature.isEmpty()) {
                        writer.write("        <feature>" + feature.trim() + "</feature>\n");
                    }
                }
            }
            writer.write("        <feature>timedexit-1.0</feature>\n"); // This is probably not needed, but here because most fat tests include it (usually by importing ../fatTestCommon.xml which doesn't exist).
            writer.write("    </featureManager>\n\n");

            writer.write("    <httpEndpoint id=\"defaultHttpEndpoint\"\n");
            writer.write("              httpPort=\"9080\"\n");
            writer.write("              httpsPort=\"9443\" />\n\n");

            //writer.write("    <include location=\"../fatTestCommon.xml\"/>\n\n");

            writer.write("</server>\n");
        }
        displayFile(file);
    }

    private static File createServerDirectory() {
        String serverDirPath = System.getProperty("user.dir") + "/publish/servers/" + SERVER_NAME;
        File serverDir = new File(serverDirPath);
        serverDir.mkdirs();
        System.out.println("SERVER DIRECTORY  [" + serverDir + "]");
        return serverDir;
    }

    private static void displayTestStartMessage(String msg) {
        displayHeading('$', " - START TEST - " + msg, 2);
        System.out.println("Current directory [" + System.getProperty("user.dir") + "]");
    }

    public static void displayHeading(char c, String message) {
        displayHeading(c, message, 0);
    }

    public static void displayHeading(char c, String message, int verticalExpansion) {
        int length = message.length();
        StringBuilder border = new StringBuilder(length + 4);
        for (int i = 0; i < length + 4; i++) {
            border.append(c);
        }

        StringBuilder blankLine = new StringBuilder(length + 4);
        if (verticalExpansion > 0) {
            blankLine.append(c);
            for (int i = 0; i < length + 2; i++) {
                blankLine.append(' ');
            }
            blankLine.append(c);
        }

        System.out.println();
        System.out.println(border.toString());
        if (verticalExpansion > 0) {
            for (int i = 0; i < verticalExpansion; i++) {
                System.out.println(blankLine.toString());
            }
        }
        System.out.println(c + " " + message + " " + c);
        if (verticalExpansion > 0) {
            for (int i = 0; i < verticalExpansion; i++) {
                System.out.println(blankLine.toString());
            }
        }
        System.out.println(border.toString());
        System.out.println();
    }

    private static void displayFile(File file) throws IOException {

        displayHeading('-', "Displaying File: " + file.getAbsolutePath());

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException ioe) {
            System.out.println("Caught Exception: " + ioe.getMessage());
            throw ioe;
        }
        System.out.println();
    }

    public static Set<String> listFilesInDirectory(String dir) throws IOException {
        System.out.println("\nListing files in directory ---- [" + dir + "] -------");
        Set<String> set;
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            set = stream.filter(file -> !Files.isDirectory(file)).map(Path::getFileName).map(Path::toString).collect(Collectors.toSet());
        }
        System.out.println("End of List -----------------------\n");
        return set;
    }

    public static final String RESOLUTION_MSG = "CWWKF0012I";
    public static final String FAILED_RESOLUTION_MSG = "CWWKF0001E";

    public static final String TEST_MSGS = RESOLUTION_MSG + "|" + FAILED_RESOLUTION_MSG;

    // COUNTERS for Server Starts & Stops
    private static int STARTS = 0;
    private static int STOPS = 0;

    private static void startServer(boolean cleanOrDirty, boolean setLogMark) throws Exception {
        displayHeading('*', "Starting server " + ++STARTS);
        server.startServer(cleanOrDirty);

        // Check for server ready
        String serverReady = server.waitForStringInLog("CWWKF0011I");
        if (serverReady == null) {
            System.out.println("Timed out waiting for server ready message, CWWKF0011I");
        } else {
            System.out.println(" Server ready!");
        }

        if (setLogMark) {
            server.setMarkToEndOfLog();
        }
    }

    /**
     * Start the server using server.getMachine().execute(<server start command>). This is
     * useful when you want to read something from server.env.
     *
     * @param envVars Properties containing environment variable names and values.
     * @throws Exception
     */
    private static String startServerUsingServerScript(Properties envVars) throws Exception {

        // Execute the server start command and display stdout and stderr
        String executionDir = server.getInstallRoot() + File.separator + "bin";
        String command = "." + File.separator + "server";
        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        displayHeading('*', "Starting server " + ++STARTS + " via 'server start command' ");
        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);

        String stdoutAndstdErr = "\nStandard Out:\n[\n" + po.getStdout() + "\n]" +
                                 "\nStandard Error:\n[\n" + po.getStderr() + "\n]\n";
        System.out.println(stdoutAndstdErr);

        // Check for server ready
        String serverReady = server.waitForStringInLog("CWWKF0011I");
        if (serverReady == null) {
            System.out.println("Timed out waiting for server ready message, CWWKF0011I");
        }

        // Because we didn't start the server using the LibertyServer APIs, we need
        // to have it detect its started state so it will stop and save logs properly
        server.resetStarted();
        return stdoutAndstdErr;
    }

    private static void stopServer(String[] messagesAllowed) throws Exception {
        displayHeading('*', "Stopping server " + ++STOPS);
        // On the OS's where we drop out of a test that is non linux/mac os, we never create the server,
        // thus when the @after clean up is called, the serverStop executes on a null server.
        if (server != null) {
            server.stopServer(messagesAllowed);
        }
    }

    private static String waitForStringInLog(String s) throws Exception {
        System.out.println(" --- waiting for string [ " + s + "] in log --- ");
        String lineInLog = server.waitForStringInLog(s, 15000);

        System.out.println(" --- here's what I found:");
        System.out.println(lineInLog + "\n\n");

        server.setMarkToEndOfLog();
        return lineInLog;
    }
}