/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package io.openliberty.classloading.base.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests for classloader delegation.
 * All tests follow the same format. A unique URL for each test returns either
 * success or a message containing the test failure.
 * Migrated from WS-CD-Open com.ibm.ws.classloading.ClassloaderDelegationFatTest.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class ClassloaderDelegationFatTest {

    private static final String EMBEDDED_LIB_EAR = "TestUsingAppBundledLibraries";
    private static final String SHARED_LIB_EAR = "TestUsingSharedLibraries";

    private static final LibertyServer defaultServer = LibertyServerFactory.getLibertyServer("classloader_delegation_default_FAT_Server");
    private static final LibertyServer parentFirstServer = LibertyServerFactory.getLibertyServer("classloader_delegation_parentFirst_FAT_Server");
    private static final LibertyServer parentLastServer = LibertyServerFactory.getLibertyServer("classloader_delegation_parentLast_FAT_Server");

    @BeforeClass
    public static void setUp() throws Exception {
        defaultServer.installSystemFeature("classloadingfatlibertyinternals-1.0");

        // Build TestUsingSharedLibraries.ear — WAR only contains the servlet+filter;
        // commons-io and slf4j are loaded at runtime from the server's libs/ fileset.
        // WAR named same as EAR base name so context root = /TestUsingSharedLibraries
        WebArchive sharedLibWar = ShrinkWrap.create(WebArchive.class, SHARED_LIB_EAR + ".war")
                .addClass(test.delegation.shared.GetTempDirectoryServlet.class)
                .addClass(test.delegation.shared.TestFilter.class);
        ShrinkHelper.addDirectory(sharedLibWar, "test-applications/TestUsingSharedLibraries.war/resources");
        EnterpriseArchive sharedLibEar = ShrinkWrap.create(EnterpriseArchive.class, SHARED_LIB_EAR + ".ear")
                .addAsModule(sharedLibWar);

        // Build TestUsingAppBundledLibraries.ear — libs are embedded in the EAR's lib/ dir
        // WAR named same as EAR base name so context root = /TestUsingAppBundledLibraries
        WebArchive bundledLibWar = ShrinkWrap.create(WebArchive.class, EMBEDDED_LIB_EAR + ".war")
                .addClass(test.delegation.bundled.GetTempDirectoryServlet.class)
                .addClass(test.delegation.bundled.TestFilter.class);
        ShrinkHelper.addDirectory(bundledLibWar, "test-applications/TestUsingAppBundledLibraries.war/resources");
        final String libsDir = "publish/servers/classloader_delegation_default_FAT_Server/libs/";
        EnterpriseArchive bundledLibEar = ShrinkWrap.create(EnterpriseArchive.class, EMBEDDED_LIB_EAR + ".ear")
                .addAsModule(bundledLibWar)
                .addAsLibrary(importJar(libsDir + "commons-io-2.0.1.jar"))
                .addAsLibrary(importJar(libsDir + "slf4j-api-1.6.1.jar"))
                .addAsLibrary(importJar(libsDir + "slf4j-ext-1.6.1.jar"))
                .addAsLibrary(importJar(libsDir + "slf4j-log4j12-1.6.1.jar"));

        // Deploy to all three delegation servers (OVERWRITE in case pre-built stale EARs exist)
        for (LibertyServer s : new LibertyServer[]{defaultServer, parentFirstServer, parentLastServer}) {
            ShrinkHelper.exportToServer(s, "apps", sharedLibEar, com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.OVERWRITE);
            ShrinkHelper.exportToServer(s, "apps", bundledLibEar, com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.OVERWRITE);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        defaultServer.uninstallSystemFeature("classloadingfatlibertyinternals-1.0");
    }

    /**
     * Test that default server configuration puts parent first (slf4j loaded from runtime bundle)
     */
    @Test
    public void testDefaultDelegationConfig() throws Exception {
        final String method = "testDefaultDelegationConfig";
        try {
            defaultServer.addInstalledAppForValidation(EMBEDDED_LIB_EAR);
            defaultServer.addInstalledAppForValidation(SHARED_LIB_EAR);
            defaultServer.startServer(method + ".log");
            assertNotNull("Application has not started: " + EMBEDDED_LIB_EAR, defaultServer.waitForStringInLog("CWWKZ0001I.* " + EMBEDDED_LIB_EAR));
            assertNotNull("Application has not started: " + SHARED_LIB_EAR, defaultServer.waitForStringInLog("CWWKZ0001I.* " + SHARED_LIB_EAR));

            String testURL = "/" + EMBEDDED_LIB_EAR + "/GetTempDirectory";
            String result = test(defaultServer, testURL);
            assertTrue("Unexpected response from test app: " + result, result.startsWith("<html><body><br /><p>FileUtils.getTempDirectory()"));
            // check system.out from app
            String logLine = defaultServer.waitForStringInLog("BundledLibsTest org.slf4j.MDC =>");
            assertNotNull("BundledLibsTest Message indicating lib used from runtime was not found in log", logLine);

            testURL = "/" + SHARED_LIB_EAR + "/GetTempDirectory";
            result = test(defaultServer, testURL);
            assertTrue("Unexpected response from test app: " + result, result.startsWith("<html><body><br /><p>FileUtils.getTempDirectory()"));
            // check system.out from app
            logLine = defaultServer.waitForStringInLog("SharedLibsTest org.slf4j.MDC =>");
            assertNotNull("SharedLibsTest Message indicating lib used from runtime was not found in log", logLine);

        } finally {
            defaultServer.stopServer();
        }
    }

    /*
     * Test that parentFirst server configuration puts parent first (slf4j loaded from runtime bundle)
     */
    @Test
    public void testParentFirstDelegationConfigEmbedded() throws Exception {
        final String method = "testParentFirstDelegationConfigEmbedded";
        try {
            parentFirstServer.addInstalledAppForValidation(EMBEDDED_LIB_EAR);
            parentFirstServer.addInstalledAppForValidation(SHARED_LIB_EAR);
            parentFirstServer.startServer(method + ".log");
            assertNotNull("Application has not started: " + EMBEDDED_LIB_EAR, parentFirstServer.waitForStringInLog("CWWKZ0001I.* " + EMBEDDED_LIB_EAR));
            assertNotNull("Application has not started: " + SHARED_LIB_EAR, parentFirstServer.waitForStringInLog("CWWKZ0001I.* " + SHARED_LIB_EAR));

            String testURL = "/" + EMBEDDED_LIB_EAR + "/GetTempDirectory";
            String result = test(parentFirstServer, testURL);
            assertTrue("Unexpected response from test app: " + result, result.startsWith("<html><body><br /><p>FileUtils.getTempDirectory()"));
            // check system.out from app
            String logLine = parentFirstServer.waitForStringInLog("BundledLibsTest org.slf4j.MDC =>");
            assertNotNull("BundledLibsTest Message indicating lib used from runtime was not found in log", logLine);

            testURL = "/" + SHARED_LIB_EAR + "/GetTempDirectory";
            result = test(parentFirstServer, testURL);
            assertTrue("Unexpected response from test app: " + result, result.startsWith("<html><body><br /><p>FileUtils.getTempDirectory()"));
            // check system.out from app
            logLine = parentFirstServer.waitForStringInLog("SharedLibsTest org.slf4j.MDC =>");
            assertNotNull("SharedLibsTest Message indicating lib used from runtime was not found in log", logLine);

        } finally {
            parentFirstServer.stopServer();
        }
    }

    /*
     * Test that parentLast server configuration puts parent last (slf4j loaded from application jar)
     */
    @Test
    public void testParentLastDelegationConfigEmbedded() throws Exception {
        final String method = "testParentLastDelegationConfigEmbedded";
        try {
            parentLastServer.addInstalledAppForValidation(EMBEDDED_LIB_EAR);
            parentLastServer.addInstalledAppForValidation(SHARED_LIB_EAR);
            parentLastServer.startServer(method + ".log");
            assertNotNull("Application has not started: " + EMBEDDED_LIB_EAR, parentLastServer.waitForStringInLog("CWWKZ0001I.* " + EMBEDDED_LIB_EAR));
            assertNotNull("Application has not started: " + SHARED_LIB_EAR, parentLastServer.waitForStringInLog("CWWKZ0001I.* " + SHARED_LIB_EAR));

            String testURL = "/" + EMBEDDED_LIB_EAR + "/GetTempDirectory";
            String result = test(parentLastServer, testURL);
            assertTrue("Unexpected response from test app: " + result, result.startsWith("<html><body><br /><p>FileUtils.getTempDirectory()"));
            // check system.out from app
            String logLine = parentLastServer.waitForStringInLog("BundledLibsTest org.slf4j.MDC => wsjar:file", 500);
            assertNotNull("BundledLibsTest Message indicating lib used from application was not found in log", logLine);

            testURL = "/" + SHARED_LIB_EAR + "/GetTempDirectory";
            result = test(parentLastServer, testURL);
            assertTrue("Unexpected response from test app: " + result, result.startsWith("<html><body><br /><p>FileUtils.getTempDirectory()"));
            // check system.out from app
            logLine = parentLastServer.waitForStringInLog("SharedLibsTest org.slf4j.MDC => wsjar:file", 500);
            assertNotNull("SharedLibsTest Message indicating lib used from application was not found in log", logLine);

        } finally {
            parentLastServer.stopServer();
        }
    }

    private String test(LibertyServer server, String testUri) throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + testUri);
        return HttpUtils.getHttpResponseAsString(url);
    }

    private static JavaArchive importJar(String relativePath) {
        File file = new File(relativePath);
        return ShrinkWrap.create(ZipImporter.class, file.getName())
                .importFrom(file)
                .as(JavaArchive.class);
    }
}
