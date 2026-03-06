/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/

 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.conformance.tests;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.conformanceTestApp.ConformanceTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

@RunWith(FATRunner.class)
/*
 * This FAT test runs MCP conformance tests from https://github.com/modelcontextprotocol/conformance.
 * This help ensure that the Liberty implementation of the MCP server is compliant with the MCP spec.
 *
 */
public class ConformanceTests extends FATServletClient {

    // File locations & names (Source file and Container files)
    private static final MountableFile SRC_PACKAGE_JSON = MountableFile.forClasspathResource("/conformance-tests/package.json");
    private static final MountableFile SRC_PACKAGE_LOCK_JSON = MountableFile.forClasspathResource("/conformance-tests/package-lock.json");
    private static final String CONTAINER_WORKING_DIR = "/tmp";
    private static final String CONTAINER_PACKAGE_JSON = CONTAINER_WORKING_DIR + "/package.json";
    private static final String CONTAINER_PACKAGE_LOCK_JSON = CONTAINER_WORKING_DIR + "/package-lock.json";

    // Node.js specific
    private static final String INSTALL_MCP_CONFORMANCE_PACKAGE = "npm ci --omit=dev";
    private static final String CHECK_MCP_CONFORMANCE_PACKAGE_VERSION = "npx --no-install @modelcontextprotocol/conformance --version";
    private static final String MCP_CONFORMANCE_TEST_VERSION = "0.1.9";

    // Artifactory
    private static final String PATH_TO_ARTIFACTORY_NPM_MIRROR = "/artifactory/api/npm/wasliberty-npm-remote/";
    private static final String FAT_TEST_PREFIX = "fat.test.";
    private static final String ARTIFACTORY_TOKEN_KEY = FAT_TEST_PREFIX + "artifactory.download.token";
    private static final String ARTIFACTORY_USER_KEY = FAT_TEST_PREFIX + "artifactory.download.user";
    private static final String ARTIFACTORY_SERVER_KEY = FAT_TEST_PREFIX + "artifactory.download.server";
    private static final String ARTIFACTORY_FORCE_EXTERNAL_KEY = FAT_TEST_PREFIX + "artifactory.force.external.repo";
    private static String artToken;
    private static String artUser;
    private static String artServer;

    // Container config
    private static String MCP_SERVER_URL_FROM_CONTAINER;
    private static final String DOCKER_REGISTRY = "public.ecr.aws/docker/library/node:20-alpine";

    @Server("mcp-conformance-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/conformanceTests");
    private final static Logger logger = Logger.getLogger(ConformanceTests.class.getName());

    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>(DOCKER_REGISTRY).withCommand("tail", "-f", "/dev/null")
                                                                                         .withAccessToHost(true)
                                                                                         .withWorkingDirectory(CONTAINER_WORKING_DIR)
                                                                                         .withCopyFileToContainer(SRC_PACKAGE_JSON, CONTAINER_PACKAGE_JSON)
                                                                                         .withCopyFileToContainer(SRC_PACKAGE_LOCK_JSON, CONTAINER_PACKAGE_LOCK_JSON)
                                                                                         .withLogConsumer(new SimpleLogConsumer(ConformanceTests.class, "nodejs-container"));

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "conformanceTests.war").addPackage(ConformanceTools.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        server.startServer();
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$")); // regex matches string that ends with /mcp e.g. "MCP server endpoint: http://macbookpro.home:8010/toolTest/mcp"
        setupContainer();
    }

    private static void setupContainer() throws IOException, InterruptedException {
        final int localServerPort = server.getHttpDefaultPort();
        MCP_SERVER_URL_FROM_CONTAINER = "http://host.testcontainers.internal:" + localServerPort + "/conformanceTests/mcp";
        Testcontainers.exposeHostPorts(localServerPort);

        if (artifactoryConfigIsPresent()) {
            String credentials = artUser + ":" + artToken;
            String credentialsBase64 = java.util.Base64.getEncoder().encodeToString(credentials.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String artifactoryServer = artServer + PATH_TO_ARTIFACTORY_NPM_MIRROR;
            String npmMirrorSetup = "npm config set registry https://" + artifactoryServer + " && " +
                                    "npm config set //" + artifactoryServer + ":_auth ";

            runCommandInContainer(npmMirrorSetup + credentialsBase64);
        }

        runCommandInContainer(INSTALL_MCP_CONFORMANCE_PACKAGE);
        final String actualMcpConformanceVersion = runCommandInContainer(CHECK_MCP_CONFORMANCE_PACKAGE_VERSION).trim();
        assertTrue("Incorrect @modelcontextprotocol/conformance version, expected " + MCP_CONFORMANCE_TEST_VERSION + " got " + actualMcpConformanceVersion,
                   actualMcpConformanceVersion.equals(MCP_CONFORMANCE_TEST_VERSION));
    }

    private static String runCommandInContainer(String command) throws IOException, InterruptedException {
        Container.ExecResult result = container.execInContainer("sh", "-c", command);
        assertEquals(0, result.getExitCode());
        return result.getStdout();
    }

    private static boolean artifactoryConfigIsPresent() {
        String forceExternalRepo = System.getProperty(ARTIFACTORY_FORCE_EXTERNAL_KEY);
        if (isValidProperty(forceExternalRepo) && Boolean.parseBoolean(forceExternalRepo)) {
            return false;
        }

        artUser = System.getProperty(ARTIFACTORY_USER_KEY);
        artToken = System.getProperty(ARTIFACTORY_TOKEN_KEY);
        artServer = System.getProperty(ARTIFACTORY_SERVER_KEY);

        return isValidProperty(artUser) && isValidProperty(artToken) && isValidProperty(artServer);
    }

    private static boolean isValidProperty(String property) {
        return property != null && !property.isBlank() && !property.startsWith("${");
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWMCM0014E", // An Internal Server Error occurred whilst processing the JSON-RPC request.
                          "CWMCM0010E", // Tool method threw an unexpected exception
                          "CWMCM0011E" // An internal server error occurred
        );
    }

    @Test
    public void testModelContextProtocolConformance() throws Exception {
        checkMCPServerConformanceWithTest("server-initialize");
        checkMCPServerConformanceWithTest("tools-list");
        checkMCPServerConformanceWithTest("tools-call-simple-text");
        checkMCPServerConformanceWithTest("tools-call-image");
        checkMCPServerConformanceWithTest("tools-call-audio");
        checkMCPServerConformanceWithTest("tools-call-error");
    }

    /**
     * Run the MCP conformance test scenario using nodejs on the container
     *
     * @param scenarioName
     * @throws IOException
     * @throws InterruptedException
     */
    private void checkMCPServerConformanceWithTest(String scenarioName) throws IOException, InterruptedException {
        String m = "checkMCPServerConformanceWithTest";
        String command = "npx --no-install @modelcontextprotocol/conformance@0.1.9 server --url " + MCP_SERVER_URL_FROM_CONTAINER + " --scenario " + scenarioName;
        Log.info(getClass(), m, "Running test command: " + command);
        Container.ExecResult result = container.execInContainer("sh", "-c", command);
        Log.info(getClass(), m, "Stdout: " + result.getStdout());
        Log.info(getClass(), m, "Stderr: " + result.getStderr());
        assertThat("MCP Conformance test scenario " + scenarioName + " failed", result.getStdout(), containsString("SUCCESS"));
    }
}