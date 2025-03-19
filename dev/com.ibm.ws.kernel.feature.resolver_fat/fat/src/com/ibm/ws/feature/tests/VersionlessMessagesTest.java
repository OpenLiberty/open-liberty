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

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class VersionlessMessagesTest {

    public static final String SERVER_NAME_PLATFORM_VARIABLE_VERSION_NOT_VALID = "msg_48E_platformVariableVersionNotValid";
    public static final String SERVER_NAME_PLATFORM_VERSIONS_IN_CONFLICT = "msg_49E_platformVersionsInConflict";
    public static final String SERVER_NAME_UNKNOWN_PLATFORM = "msg_52E_unknownPlatform";
    public static final String SERVER_NAME_RESOLVED_PLATFORMS = "msg_53I_ResolvedPlatforms";
    public static final String SERVER_NAME_NO_VERSION_OF_FEATURE_EXISTS_FOR_PLATFORM = "msg_54E_NoFeatureVersionExistsForPlatform";
    public static final String SERVER_NAME_NO_CONFIGURED_PLATFORM = "msg_55E_NoConfiguredPlatform";

    private static LibertyServer server;
    private String[] allowedMessages = null;

    @Before
    public void before() {
        //
    }

    @After
    public void after() throws Exception {
        server.stopServer(allowedMessages);
        allowedMessages = null;
    }

    @Test // 48E
    public void versionless_PreferredPlatformVariable_VersionNotValid() throws Exception {

        initTest(SERVER_NAME_PLATFORM_VARIABLE_VERSION_NOT_VALID, "", "jsp", "javaee-0.1");

        // Expect message: "CWWKF0048E: The {0} value of the PREFERRED_PLATFORM_VERSIONS platform environment variable does not contain a valid version"
        allowedMessages = new String[] { "CWWKF0050E", "CWWKF0055E", "CWWKF0048E" };
        startServer_CheckLogs("CWWKF0048E:");
    }

    @Test // 49E
    public void versionless_PlatformVersionsInConflict() throws Exception {

        initTest(SERVER_NAME_PLATFORM_VERSIONS_IN_CONFLICT, "javaee-7.0, javaee-8.0", "servlet, jsp", null);

        // Expect message: "CWWKF0049E: The following configured platform versions are in conflict: {0}"
        allowedMessages = new String[] { "CWWKF0050E", "CWWKF0055E", "CWWKF0049E" };
        startServer_CheckLogs("CWWKF0049E:");
    }

    //        50E shows up in the other error cases.
    // 50E  CWWKF0050E: The {0} versionless feature cannot be resolved

    @Test // 52E
    public void versionless_unknownPlatform() throws Exception {

        initTest(SERVER_NAME_UNKNOWN_PLATFORM, "fred-1.0", "jsp", null);

        // Expect message: "CWWKF0052E: The {0} platform element is not a known platform"
        startServer_CheckLogs("CWWKF0052E:");
        allowedMessages = new String[] { "CWWKF0050E", "CWWKF0055E", "CWWKF0052E" };
    }

    @Test // 53I
    public void versionless_resolvedPlatforms() throws Exception {

        initTest(SERVER_NAME_RESOLVED_PLATFORMS, "javaee-8.0", "jsp", null);

        // Expect message: CWWKF0053I: Feature resolution selected the {0} platforms.
        allowedMessages = new String[] { "CWWKF0053I" };
        startServer_CheckLogs("CWWKF0053I:");
    }

    @Test // 54E
    public void versionless_NoFeatureVersionExistsForPlatform() throws Exception {

        initTest(SERVER_NAME_NO_VERSION_OF_FEATURE_EXISTS_FOR_PLATFORM, "microProfile-5.0", "mpTelemetry", null);

        // Expect message: "CWWKF0054E: The {0} versionless feature does not have a version that belongs to the {1} platform.
        allowedMessages = new String[] { "CWWKF0050E", "CWWKF0055E", "CWWKF0054E" };
        startServer_CheckLogs("CWWKF0054E:");
    }

    @Test // 54E
    public void versionless_NoFeatureVersionExistsForPlatformJakartaee8() throws Exception {

        initTest(SERVER_NAME_NO_VERSION_OF_FEATURE_EXISTS_FOR_PLATFORM, "jakartaee-8.0", "data", null);

        // Expect message: "CWWKF0054E: The {0} versionless feature does not have a version that belongs to the {1} platform.
        allowedMessages = new String[] { "CWWKF0050E", "CWWKF0055E", "CWWKF0054E" };
        startServer_CheckLogs("CWWKF0054E:");
    }

    @Test // 55E
    public void versionless_NoConfiguredPlatform() throws Exception {

        initTest(SERVER_NAME_NO_CONFIGURED_PLATFORM, "", "servlet, jsp", null);

        // Expect message: CWWKF0055E: The {0} versionless features do not have a configured platform.
        allowedMessages = new String[] { "CWWKF0050E", "CWWKF0055E" };
        startServer_CheckLogs("CWWKF0055E:");
    }

    private static void startServer_CheckLogs(String expectedMessage) {
        final String METHOD_NAME = "startServer_CheckLogs";
        try {
            server.startServer();
        } catch (Exception e) {
            System.out.println("Exception caught in " + METHOD_NAME + ": " + e.getMessage());
        }

        String lineInLog = server.waitForStringInLog(expectedMessage);
        assertNotNull("The expected log message, " + expectedMessage + ", was not found.", lineInLog);
    }

    public void createInputData(String directory, List<String> platforms, List<String> features, String preferredPlatformVersions) throws IOException {
        createServerXml(directory, platforms, features);
        if (preferredPlatformVersions != null && !preferredPlatformVersions.isEmpty()) {
            createServerEnv(directory, preferredPlatformVersions);
        }

        createJvmOptions(directory);
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

    private static void createServerEnv(String directory, String preferredPlatformVersions) throws IOException {
        File file = new File(directory, "server.env");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("PREFERRED_PLATFORM_VERSIONS=" + preferredPlatformVersions + "\n");
        }
        displayFile(file);
    }

    // !!! This can/should be removed after GA
    private static void createJvmOptions(String directory) throws IOException {
        System.out.println("JVM NOT set for beta.");

        // System.out.println("JVM set for beta.")
        // File file = new File(directory, "jvm.options");
        // try (FileWriter writer = new FileWriter(file)) {
        //     writer.write("-Dcom.ibm.ws.beta.edition=true\n");
        // }
        // displayFile(file);
    }

    private void initTest(String serverName, String platforms, String features, String preferredVersionsVariable) throws Exception {
        displayStartMessage(serverName);
        String serverDirPath = System.getProperty("user.dir") + "/publish/servers/" + serverName;
        File serverDir = new File(serverDirPath);

        serverDir.mkdirs();
        System.out.println("-- SERVER DIRECTORY -- " + serverDir);

        List<String> platformList;
        List<String> featureList;

        platformList = (platforms != null) ? Arrays.asList(platforms.split(",")) : null;
        featureList = (features != null) ? Arrays.asList(features.split(",")) : null;

        createInputData(serverDir.getAbsolutePath(), platformList, featureList, preferredVersionsVariable);
        server = LibertyServerFactory.getLibertyServer(serverName);
    }

    private static void displayStartMessage(String msg) {
        System.out.println("\n- $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ - START TEST - " + msg + " - $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$\n");
        System.out.println("Current directory [" + System.getProperty("user.dir") + "]");
    }

    private static void displayFile(File file) throws IOException {
        System.out.println(file.getAbsolutePath());

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}