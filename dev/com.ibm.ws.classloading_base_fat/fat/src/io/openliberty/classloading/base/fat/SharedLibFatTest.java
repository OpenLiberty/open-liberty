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

import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

import com.ibm.ws.classloading.test.base.MyBaseClass;
import com.ibm.ws.classloading.test.ejb.MyStartupSingletonBean;
import test.HelloA;
import test.HelloB;
import test.HelloC;
import test.HelloD;
import web.SharedLibraryServlet; // raw HttpServlet + web.xml approach preserved

/**
 * Tests for the shared library service migrated from WS-CD-Open SharedLibFatTest.
 * Tests are driven by URL-dispatched test methods in SharedLibraryServlet.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class SharedLibFatTest {

    private static final String SUCCESS_MESSAGE = "Success";
    private static final int TIMEOUT = 30000;

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("classloader_FAT_Server");

    private static final Class<?> c = SharedLibFatTest.class;

    public String _testName = "";

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUpClass() throws Exception {
        server.installSystemFeature("classloadingfatlibertyinternals-1.0");

        // ---- Build sharedLib.war ----
        // The servlet dispatches tests by URL parameter; it extends HttpServlet directly (not FATServlet)
        // and needs OSGi context, so it keeps raw HttpServlet + web.xml approach.
        WebArchive sharedLibWar = ShrinkWrap.create(WebArchive.class, "sharedLib.war")
                .addClass(SharedLibraryServlet.class);
        ShrinkHelper.addDirectory(sharedLibWar, "test-applications/sharedLib.war/resources");
        ShrinkHelper.exportToServer(server, "apps", sharedLibWar, DeployOptions.SERVER_ONLY);

        // ---- Build app.ear (sharedLib.war + application.xml giving /SharedLibraryTest context) ----
        // The application.xml maps sharedLib.war to context root /SharedLibraryTest
        EnterpriseArchive appEar = ShrinkWrap.create(EnterpriseArchive.class, "app.ear")
                .addAsModule(sharedLibWar)
                .addAsManifestResource(
                        new org.jboss.shrinkwrap.api.asset.StringAsset(
                            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<application id=\"Application_ID\" version=\"1.4\"\n" +
                            "  xmlns=\"http://java.sun.com/xml/ns/j2ee\"\n" +
                            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                            "  xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/application_1_4.xsd\">\n" +
                            "  <description>Shared Library app</description>\n" +
                            "  <display-name>SharedLibraryTest</display-name>\n" +
                            "  <module id=\"SharedLibraryTest\">\n" +
                            "    <web>\n" +
                            "      <web-uri>sharedLib.war</web-uri>\n" +
                            "      <context-root>/SharedLibraryTest</context-root>\n" +
                            "    </web>\n" +
                            "  </module>\n" +
                            "</application>"),
                        "application.xml");
        ShrinkHelper.exportToServer(server, "apps", appEar, DeployOptions.SERVER_ONLY);

        // ---- Build and deploy sharedLibraryA.jar -> SharedLibraryA/ ----
        JavaArchive libA = ShrinkHelper.buildJavaArchive("sharedLibraryA.jar",
                HelloA.class.getPackage().getName());
        ShrinkHelper.exportToServer(server, "SharedLibraryA", libA, DeployOptions.SERVER_ONLY);

        // ---- Build and deploy sharedLibraryB.jar -> SharedLibraryB/ ----
        JavaArchive libB = ShrinkHelper.buildJavaArchive("sharedLibraryB.jar",
                HelloB.class.getPackage().getName());
        ShrinkHelper.exportToServer(server, "SharedLibraryB", libB, DeployOptions.SERVER_ONLY);

        // ---- Build and deploy sharedLibraryC.jar -> SharedLibraryC/ ----
        JavaArchive libC = ShrinkHelper.buildJavaArchive("sharedLibraryC.jar",
                HelloC.class.getPackage().getName());
        ShrinkHelper.exportToServer(server, "SharedLibraryC", libC, DeployOptions.SERVER_ONLY);

        // ---- Build and deploy sharedLibraryD.jar -> lib/global/ (global library jar) ----
        JavaArchive libD = ShrinkHelper.buildJavaArchive("sharedLibraryD.jar",
                HelloD.class.getPackage().getName());
        ShrinkHelper.exportToServer(server, "lib/global", libD, DeployOptions.SERVER_ONLY);

        // ---- Deploy sharedLibraryBase1.jar -> SharedLibraryBase/ ----
        // HelloE.class and sharedLibraryBase2.jar are pre-placed as static server resources
        // under publish/servers/classloader_FAT_Server/ and do not need runtime deployment.
        JavaArchive libBase1 = ShrinkHelper.buildJavaArchive("sharedLibraryBase1.jar",
                MyBaseClass.class.getPackage().getName());
        ShrinkHelper.exportToServer(server, "SharedLibraryBase", libBase1, DeployOptions.SERVER_ONLY);

        // ---- Build and deploy sharedLibEJB.ear -> apps/ ----
        JavaArchive ejbJar = ShrinkHelper.buildJavaArchive("sharedLibEJB.jar",
                MyStartupSingletonBean.class.getPackage().getName());
        EnterpriseArchive ejbEar = ShrinkWrap.create(EnterpriseArchive.class, "sharedLibEJB.ear")
                .addAsModule(ejbJar);
        ShrinkHelper.exportToServer(server, "apps", ejbEar, DeployOptions.SERVER_ONLY);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        server.uninstallSystemFeature("classloadingfatlibertyinternals-1.0");
    }

    @Before
    public void setTestName() throws Exception {
        _testName = name.getMethodName();
        Log.info(c, _testName, "===== Starting test " + _testName + " =====");
    }

    @After
    public void tearDown() throws Exception {
        // Some tests intentionally trigger server warnings/errors — allow them here.
        // CWWKG0033W: referenced config element not found (ErrorMissingLibrary, ErrorMissingFileset tests)
        // CWWKL0012W: file does not exist (ErrorArchive test)
        // CWWKM0101E: could not process archive data (ErrorArchive test — NotAJar files)
        server.stopServer("CWWKG0033W", "CWWKL0012W", "CWWKM0101E");
        Log.info(c, _testName, "===== Ending test " + _testName + " =====");
    }

    /**
     * Simple test to verify that FilesetRef attribute works.
     */
    @Test
    public void testFilesetReference() throws Exception {
        setConfigWaitForAppStart("SharedLib/OneFileset/server.xml", "CWWKZ0001I:.*sharedLib");
        test();
    }

    /**
     * In the Fileset indicated there is a single jar file.
     * Verifies that this file is picked up by the Shared Library.
     */
    @Test
    public void testOneFileset() throws Exception {
        setConfigWaitForAppStart("SharedLib/OneFileset/server.xml", "CWWKZ0001I:.*sharedLib");
        test();
    }

    /**
     * Same as testOneFileset but uses a filesetRef instead of nested config.
     */
    @Test
    public void testOneFilesetRef() throws Exception {
        setConfigWaitForAppStart("SharedLib/OneFileset/server.xml", "CWWKZ0001I:.*sharedLib");
        test();
    }

    /**
     * Verifies a library with three filesets exposes all three jars.
     */
    @Test
    public void testThreeFilesets() throws Exception {
        setConfigWaitForAppStart("SharedLib/ThreeFilesets/server.xml", "CWWKZ0001I:.*sharedLib");
        test();
    }

    /**
     * Same as testThreeFilesets but uses filesetRef attributes.
     */
    @Test
    public void testThreeFilesetsRef() throws Exception {
        setConfigWaitForAppStart("SharedLib/ThreeFilesets/server.xml", "CWWKZ0001I:.*sharedLib");
        test();
    }

    /**
     * The fileset starts with one jar; the test dynamically adds another and verifies detection.
     */
    @Test
    public void testFilesetChange() throws Exception {
        setConfigWaitForAppStart("SharedLib/Main/server.xml", "CWWKZ0001I:.*sharedLib");
        test();
    }

    /**
     * @see testFilesetChange but with filesetRef
     */
    @Test
    public void testFilesetChangeRef() throws Exception {
        setConfigWaitForAppStart("SharedLib/Main/server.xml", "CWWKZ0001I:.*sharedLib");
        test();
    }

    /**
     * Test that shared libraries are available from an application.
     */
    @Test
    public void testAppClassloader() throws Exception {
        setConfigWaitForAppStart("SharedLib/Main/server.xml", "CWWKZ0001I:.*sharedLib");
        test();
    }

    /**
     * Test that resources are available from an application.
     */
    @Test
    public void testResourceDiscovery() throws Exception {
        setConfigWaitForAppStart("SharedLib/Main/server.xml", "CWWKZ0001I:.*sharedLib");
        test();
    }

    /**
     * Multi-part test: verifies config changes are picked up by the classloader.
     */
    @Test
    public void testConfigChange() throws Exception {
        setConfigWaitForAppStart("SharedLib/Main/server.xml", "CWWKZ0001I:.*sharedLib");
        test();

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("SharedLib/Changed/server.xml");
        String logCheck = server.waitForStringInLogUsingMark("CWWKG0017I", TIMEOUT);
        assertNotNull("Server did not update", logCheck);
        String s = server.waitForStringInLogUsingMark("CWWKZ0003I:.*sharedLib");
        assertNotNull("Application has not started", s);
        test(server, "", "testConfigChangeAfter");

        // Put things back
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("SharedLib/Main/server.xml");
        logCheck = server.waitForStringInLogUsingMark("CWWKG0017I", TIMEOUT);
        assertNotNull("Server did not update", logCheck);
        s = server.waitForStringInLogUsingMark("CWWKZ0003I:.*sharedLib");
        assertNotNull("Application has not started: sharedLib", s);
        test();
    }

    /**
     * @see testConfigChange but with filesetRef
     */
    @Test
    public void testConfigChangeRef() throws Exception {
        setConfigWaitForAppStart("SharedLib/Main/server.xml", "CWWKZ0001I:.*sharedLib");
        test();

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("SharedLib/Changed/server.xml");
        String logCheck = server.waitForStringInLogUsingMark("CWWKG0017I", TIMEOUT);
        assertNotNull("Server did not update", logCheck);
        String s = server.waitForStringInLogUsingMark("CWWKZ0003I:.*sharedLib");
        assertNotNull("Application has not started", s);
        test(server, "", "testConfigChangeAfterRef");

        // Put things back
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("SharedLib/Main/server.xml");
        logCheck = server.waitForStringInLogUsingMark("CWWKG0017I", TIMEOUT);
        assertNotNull("Server did not update", logCheck);
        s = server.waitForStringInLogUsingMark("CWWKZ0003I:.*sharedLib");
        assertNotNull("Application has not started: sharedLib", s);
        test();
    }

    /**
     * Test a application classloader with a simple jar file.
     */
    @Test
    public void testClassloaderSimple() throws Exception {
        setConfigWaitForAppStart("SharedLib/Simple/server.xml", "CWWKZ0001I:.*sharedLib");
        test();
    }

    /**
     * Test for missing shared library — expects CWWKG0033W warning.
     */
    @Test
    public void testClassloaderErrorMissingLibrary() throws Exception {
        setConfigWaitForAppStart("SharedLib/ErrorMissingLibrary/server.xml", "CWWKZ0001I:.*sharedLib");
        String s = server.waitForStringInLog("CWWKG0033W.*AppSharedLibrary_Error", TIMEOUT);
        assertNotNull("Expected error message did not happen", s);
    }

    /**
     * Test for missing shared Fileset — expects CWWKG0033W warning.
     */
    @Test
    public void testClassloaderErrorMissingFileset() throws Exception {
        setConfigWaitForAppStart("SharedLib/ErrorMissingFileset/server.xml", "CWWKZ0001I:.*sharedLib");
        String s = server.waitForStringInLog("CWWKG0033W.*missing_Fileset", TIMEOUT);
        assertNotNull("Expected error message did not happen", s);
    }

    @Test
    @Ignore("waiting for defect 58101")
    public void testClassloaderDirectory() throws Exception {
        setConfigWaitForAppStart("SharedLib/Main/server.xml", "CWWKZ0001I:.*sharedLib");
        test();
    }

    /**
     * Test for shared library with fileset that has no scanInterval set.
     */
    @Test
    public void testClassloaderNoScan() throws Exception {
        setConfigWaitForAppStart("SharedLib/NoScan/server.xml", "CWWKZ0001I:.*sharedLib");
        test();
    }

    /**
     * Test a shared library from an EAR application.
     */
    @Test
    public void testClassloaderEar() throws Exception {
        setConfigWaitForAppStart("SharedLib/Ear/server.xml", "CWWKZ0001I:.*SharedLibraryTest");
        test(server, "/SharedLibraryTest", _testName);
    }

    /**
     * Test for the global default shared library.
     * Sets a static in a class loaded by one app; verifies a different EAR reads it.
     */
    @Test
    public void testGlobalLibrary() throws Exception {
        setConfigWaitForAppStart("SharedLib/GlobalLibrary/server.xml",
                "CWWKZ0001I:.*sharedLib", "CWWKZ0001I:.*SharedLibraryTest");
        test();
        test(server, "/SharedLibraryTest", "testGlobalLibraryPart2");
    }

    /**
     * Test for global default shared library using a folder of classes (not a jar).
     */
    @Test
    public void testGlobalLibraryFolder() throws Exception {
        setConfigWaitForAppStart("SharedLib/GlobalLibrary/server.xml",
                "CWWKZ0001I:.*sharedLib", "CWWKZ0001I:.*SharedLibraryTest");
        test();
        test(server, "/SharedLibraryTest", "testGlobalLibraryFolderPart2");
    }

    @Test
    public void compareClassLoaders() throws Exception {
        setConfigWaitForAppStart("SharedLib/GlobalLibrary/server.xml",
                "CWWKZ0001I:.*sharedLib", "CWWKZ0001I:.*SharedLibraryTest");
        test();
    }

    /**
     * Test that files that are not archives are rejected with CWWKL0012W.
     */
    @Test
    @AllowedFFDC("java.util.zip.ZipException")
    public void testClassloaderErrorArchive() throws Exception {
        setConfigWaitForAppStart("SharedLib/ErrorArchive/server.xml", "CWWKZ0001I:.*SharedLibraryTest");
        String s = server.waitForStringInLog("CWWKL0012W:.*DoesntExist.jar");
        assertNotNull("Should have seen error message CWWKL0012W: DoesntExist.jar", s);
    }

    /**
     * Tests APAR PI17830: switching a common library to a private library with the same ID
     * correctly triggers a full classloader refresh.
     */
    @Test
    public void testConfigUpdateWithContentChanges() throws Exception {
        if (!server.isJava2SecurityEnabled()) {
            server.setServerConfigurationFile("SharedLib/SharedLibConfigUpdate/server.xml");
            server.startServer("testConfigUpdateWithContentChanges.log");
            String ejbOutput = server.waitForStringInLogUsingLastOffset("MyBaseClass - 1");
            assertNotNull("StartupEJB with common library 1 did not start or print expected PostConstruct message", ejbOutput);

            server.setServerConfigurationFile("SharedLib/SharedLibConfigUpdate/server2.xml");
            ejbOutput = server.waitForStringInLogUsingLastOffset("MyBaseClass - 2");
            assertNotNull("StartupEJB with common library 2 did not restart or print expected PostConstruct message", ejbOutput);

            server.setServerConfigurationFile("SharedLib/SharedLibConfigUpdate/server3.xml");
            ejbOutput = server.waitForStringInLogUsingLastOffset("MyBaseClass - 1");
            assertNotNull("StartupEJB with private library 1 did not restart or print expected PostConstruct message", ejbOutput);
        }
    }

    // ---- helpers ----

    private String test() throws Exception {
        return test(server, "", _testName);
    }

    private static String test(LibertyServer srv, String appname, String testUri) throws Exception {
        URL url = new URL("http://" + srv.getHostname() + ":" + srv.getHttpDefaultPort()
                + appname + "/sharedLib/test?testName=" + testUri);
        String output = HttpUtils.getHttpResponseAsString(url);
        assertNotNull(output);
        assertNotNull(output.trim());
        assertTrue("url:'" + url + "' appname:'" + appname + "' output:'" + output + "' testUri:'" + testUri + "'",
                output.trim().contains(SUCCESS_MESSAGE));
        return output;
    }

    private void setConfigWaitForAppStart(String config, String... msgs) throws Exception {
        setConfig(server, config, _testName, msgs);
        server.waitForStringInLog("CWWKF0011I");
    }

    static void setConfig(LibertyServer srv, String config, String testName, String... msgs) throws Exception {
        srv.setServerConfigurationFile(config);
        srv.startServer(testName + ".log");
        for (String m : msgs) {
            String s = srv.waitForStringInLog(m);
            assertNotNull("Message " + m + " was not found in server log", s);
        }
    }
}
