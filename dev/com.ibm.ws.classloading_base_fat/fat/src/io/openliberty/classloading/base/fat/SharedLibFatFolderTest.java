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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

import test.HelloA;
import web.SharedLibraryServlet;

/**
 * Tests for adding folder and file support to shared libraries.
 * Migrated from WS-CD-Open SharedLibFatFolderTest.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class SharedLibFatFolderTest {

    private static final String SUCCESS_MESSAGE = "Success";
    private static final String SERVER_NAME = "classloader_folder_FAT";
    private static int ERROR_TIMEOUT = 10000;

    private static final LibertyServer server;
    static {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        try {
            server.setServerConfigurationFile("FolderAndFile/server.xml");
        } catch (Exception e) {
            // if this fails, tests will fail with appropriate errors
        }
    }

    private final Class<?> c = SharedLibFatFolderTest.class;

    public String _testName = "";

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void startServer() throws Exception {
        server.installSystemFeature("classloadingfatlibertyinternals-1.0");

        // Build and deploy sharedLib.war — same servlet as SharedLibFatTest
        WebArchive sharedLibWar = ShrinkWrap.create(WebArchive.class, "sharedLib.war")
                .addClass(SharedLibraryServlet.class);
        ShrinkHelper.addDirectory(sharedLibWar, "test-applications/sharedLib.war/resources");
        ShrinkHelper.exportToServer(server, "apps", sharedLibWar, DeployOptions.SERVER_ONLY);

        // Stage HelloA.class into folder1/test/ so testClassloadingFromFolder can load it
        // via the shared library's <folder> element.  The original Ant build-test.xml did this
        // by copying build/classes-libraryA/**/*.class into folder1/.
        stageClassToFolder1(HelloA.class);

        server.startServer("folderTests.log");
        server.waitForStringInLog("CWWKF0011I");
        Thread.sleep(ERROR_TIMEOUT);
    }

    /**
     * Copies the .class file for {@code cls} into
     * {@code <serverRoot>/libs/folder1/<packagePath>/<ClassName>.class}
     * so that the shared library's {@code <folder>} element exposes it for classloading.
     */
    private static void stageClassToFolder1(Class<?> cls) throws Exception {
        String classResourcePath = cls.getName().replace('.', '/') + ".class";
        File destFile = new File(server.getServerRoot() + "/libs/folder1/" + classResourcePath);
        destFile.getParentFile().mkdirs();
        try (InputStream in = cls.getClassLoader().getResourceAsStream(classResourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Cannot find class resource: " + classResourcePath);
            }
            Files.copy(in, destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Before
    public void setTestName() throws Exception {
        _testName = name.getMethodName();
        Log.info(c, _testName, "===== Starting test " + _testName + " =====");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKL0012W", "CWWKL0013W");
        }
        server.uninstallSystemFeature("classloadingfatlibertyinternals-1.0");
    }

    private String test() throws Exception {
        return test(server, "", _testName);
    }

    private static String test(LibertyServer srv, String appname, String testUri) throws Exception {
        URL url = new URL("http://" + srv.getHostname() + ":" + srv.getHttpDefaultPort()
                + appname + "/sharedLib/test?testName=" + testUri);
        String output = HttpUtils.getHttpResponseAsString(url);
        assertTrue(output, output.trim().contains(SUCCESS_MESSAGE));
        return output;
    }

    /**
     * Tests that file element data can be retrieved from a shared library.
     */
    @Test
    public void testFiles() throws Exception {
        test();
    }

    /**
     * Tests that folder element data can be retrieved from a shared library.
     */
    @Test
    public void testFolders() throws Exception {
        test();
    }

    /**
     * Tests that .class files can be loaded directly from a folder.
     */
    @Test
    public void testClassloadingFromFolder() throws Exception {
        String className = "test.HelloA";
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                + "/sharedLib?testName=testCommonLibraryLoadClass&className=" + className);
        String result = HttpUtils.getHttpResponseAsString(url);
        result = result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
        assertTrue("Class " + className + " should have loaded" + result, result.contains(className));
        assertFalse("Should not have mentioned an exception" + result, result.contains("Exception"));
        assertFalse("Should not have mentioned an error" + result, result.contains("Error"));
    }

    /**
     * A jar file placed in a folder should not expose the classes in that jar.
     */
    @Test
    public void testNotLoadedFromJarInFolder() throws Exception {
        // HelloB is in a jar and so should not be found
        String className = "test.HelloB";
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                + "/sharedLib?testName=testCommonLibraryLoadClass&className=" + className);
        String result = HttpUtils.getHttpResponseAsString(url);
        result = result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
        assertTrue("Class " + className + " should not have loaded. " + result, result.contains(className));
        assertTrue("Should have mentioned a CNFE", result.contains("ClassNotFoundException"));
    }

    /**
     * Tests that a class can be loaded from a jar file specifically defined in config via &lt;file&gt;.
     */
    @Test
    public void testClassloadingFromJarFileInFolder() throws Exception {
        String className = "test.HelloC";
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                + "/sharedLib?testName=testCommonLibraryLoadClass&className=" + className);
        String result = HttpUtils.getHttpResponseAsString(url);
        result = result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
        assertTrue("Class " + className + " should have loaded" + result, result.contains(className));
        assertFalse("Should not have mentioned an exception" + result, result.contains("Exception"));
        assertFalse("Should not have mentioned an error" + result, result.contains("Error"));
    }

    /**
     * Resource to load is in a subdirectory.
     */
    @Test
    public void testResourceLoadingFromSubDir() throws Exception {
        String resourceFile = "com/ibm/fileTestInSubDir.properties";
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                + "/sharedLib?testName=testCommonLibraryReadDirectoryResources&resourceName=" + resourceFile);
        String result = HttpUtils.getHttpResponseAsString(url);
        result = result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
        assertTrue("Resource " + resourceFile + " should have loaded" + result, result.contains(resourceFile));
    }

    /**
     * Resource to load is in a subdirectory — with leading slash.
     */
    @Test
    public void testResourceLoadingFromSubDirWithLeadingSlash() throws Exception {
        String resourceFile = "/com/ibm/fileTestInSubDir.properties";
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                + "/sharedLib?testName=testCommonLibraryReadDirectoryResources&resourceName=" + resourceFile);
        String result = HttpUtils.getHttpResponseAsString(url);
        result = result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
        assertTrue("Resource " + resourceFile + " should have loaded" + result, result.contains(resourceFile));
    }

    /**
     * Tests loading a resource file from a classloader.
     */
    @Test
    public void testResourceLoading() throws Exception {
        String resourceFile = "folderTest.properties";
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                + "/sharedLib?testName=testCommonLibraryReadDirectoryResources&resourceName=" + resourceFile);
        String result = HttpUtils.getHttpResponseAsString(url);
        result = result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
        assertTrue("Resource " + resourceFile + " should have loaded" + result, result.contains(resourceFile));
    }

    /**
     * Tests that CWWKL0012W is logged for a missing file.
     */
    @Test
    public void Error12FileMissing() throws Exception {
        String expecting = "CWWKL0012.*NotAFile";
        List<String> s = server.findStringsInLogs(expecting);
        assertFalse("Expecting error message in log " + expecting, s.isEmpty());
    }

    /**
     * Tests that CWWKL0012W is logged when a folder is specified instead of a file.
     */
    @Test
    public void Error12FolderSpecifiedInsteadOfFile() throws Exception {
        String expecting = "CWWKL0012.*libs/folder1";
        List<String> s = server.findStringsInLogs(expecting);
        assertFalse("Expecting error message in log " + expecting, s.isEmpty());
    }

    /**
     * Tests that CWWKL0013W is logged for a missing folder.
     */
    @Test
    public void Error13FolderMissing() throws Exception {
        String expecting = "CWWKL0013.*NotAFolder";
        List<String> s = server.findStringsInLogs(expecting);
        assertFalse("Expecting error message in log " + expecting, s.isEmpty());
    }

    /**
     * Tests that CWWKL0013W is logged when a folder element points to a file.
     */
    @Test
    public void Error13FolderIsActuallyAFile() throws Exception {
        String expecting = "CWWKL0013.*libs/files/fileTest.properties";
        List<String> s = server.findStringsInLogs(expecting);
        assertFalse("Expecting error message in log " + expecting, s.isEmpty());
    }

    /**
     * Tests relative location support — folder and file elements with relative paths.
     */
    @Test
    public void testRelativeLocation() throws Exception {
        String previousLogRoot = server.getLogsRoot();
        try {
            changeOutputDirAndConfig("RelativePaths/server.xml");

            // Resource in a relative <folder>
            String resourceFile = "com/ibm/fileTestInSubDir.properties";
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                    + "/sharedLib?testName=testCommonLibraryReadDirectoryResources&resourceName=" + resourceFile);
            String result = HttpUtils.getHttpResponseAsString(url);
            result = result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
            assertTrue("Resource " + resourceFile + " should have loaded" + result, result.contains(resourceFile));

            // HelloB is in a jar and so should not be found
            String className = "test.HelloB";
            url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                    + "/sharedLib?testName=testCommonLibraryLoadClass&className=" + className);
            result = HttpUtils.getHttpResponseAsString(url);
            result = result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
            assertTrue("Class " + className + " should not have loaded. " + result, result.contains(className));
            assertTrue("Should have mentioned a CNFE", result.contains("ClassNotFoundException"));

            // class is in <file> with relative reference
            className = "test.HelloC";
            url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                    + "/sharedLib?testName=testCommonLibraryLoadClass&className=" + className);
            result = HttpUtils.getHttpResponseAsString(url);
            result = result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
            assertTrue("Class " + className + " should have loaded" + result, result.contains(className));
            assertFalse("Should not have mentioned an exception" + result, result.contains("Exception"));
            assertFalse("Should not have mentioned an error" + result, result.contains("Error"));

            // HelloD is in commonLibrary with dot in path
            className = "test.HelloD";
            url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                    + "/sharedLib?testName=testCommonLibraryLoadClass&className=" + className);
            result = HttpUtils.getHttpResponseAsString(url);
            result = result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
            assertTrue("Class " + className + " should have loaded" + result, result.contains(className));
            assertFalse("Should not have mentioned an exception" + result, result.contains("Exception"));
            assertFalse("Should not have mentioned an error" + result, result.contains("Error"));

        } finally {
            resetDefaults(previousLogRoot);
        }
    }

    /**
     * Tests relative fileset support — filesets with relative paths.
     */
    @Test
    public void testRelativeFileset() throws Exception {
        String previousLogRoot = server.getLogsRoot();
        try {
            changeOutputDirAndConfig("RelativeFileset/server.xml");

            // HelloA is not in a jar so this should not work
            String className = "test.HelloA";
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                    + "/sharedLib?testName=testCommonLibraryLoadClass&className=" + className);
            String result = HttpUtils.getHttpResponseAsString(url);
            result = result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
            assertTrue("Class " + className + " should not have loaded. " + result, result.contains(className));
            assertTrue("Should have mentioned a CNFE", result.contains("ClassNotFoundException"));

            // HelloB is in an absolute path so should not work
            className = "test.HelloB";
            url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                    + "/sharedLib?testName=testCommonLibraryLoadClass&className=" + className);
            result = HttpUtils.getHttpResponseAsString(url);
            result = result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
            assertTrue("Class " + className + " should not have loaded. " + result, result.contains(className));
            assertTrue("Should have mentioned a CNFE", result.contains("ClassNotFoundException"));

            // class is in <file> with relative reference
            className = "test.HelloC";
            url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                    + "/sharedLib?testName=testCommonLibraryLoadClass&className=" + className);
            result = HttpUtils.getHttpResponseAsString(url);
            result = result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
            assertTrue("Class " + className + " should have loaded" + result, result.contains(className));
            assertFalse("Should not have mentioned an exception" + result, result.contains("Exception"));
            assertFalse("Should not have mentioned an error" + result, result.contains("Error"));

            // HelloD is in commonLibrary with dot in path
            className = "test.HelloD";
            url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                    + "/sharedLib?testName=testCommonLibraryLoadClass&className=" + className);
            result = HttpUtils.getHttpResponseAsString(url);
            result = result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
            assertTrue("Class " + className + " should have loaded" + result, result.contains(className));
            assertFalse("Should not have mentioned an exception" + result, result.contains("Exception"));
            assertFalse("Should not have mentioned an error" + result, result.contains("Error"));

        } finally {
            resetDefaults(previousLogRoot);
        }
    }

    private void changeOutputDirAndConfig(String newConfig) throws Exception {
        server.stopServer("CWWKL0012W", "CWWKL0013W");
        server.setServerConfigurationFile(newConfig);

        String path = server.getServerRoot() + "/NewServerOutputDir";
        FileWriter fw = new FileWriter(server.getServerRoot() + "/server.env");
        BufferedWriter out = new BufferedWriter(fw);
        out.write("WLP_OUTPUT_DIR=" + path);
        out.close();

        server.setLogsRoot(path + "/" + SERVER_NAME + "/logs/");
        server.startServer();
        server.waitForStringInLog("CWWKF0011I");
        Thread.sleep(ERROR_TIMEOUT);
    }

    private void resetDefaults(String defaultLocation) throws Exception {
        server.stopServer("CWWKL0012W", "CWWKL0013W");
        server.setLogsRoot(defaultLocation);
        server.deleteFileFromLibertyServerRoot("server.env");
        server.setServerConfigurationFile("FolderAndFile/server.xml");
        server.startServer("folderTests.log");
    }
}
