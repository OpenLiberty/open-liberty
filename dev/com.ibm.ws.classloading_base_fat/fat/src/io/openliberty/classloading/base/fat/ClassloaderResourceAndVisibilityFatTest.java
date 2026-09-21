/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 * Consolidated classloading tests verifying OSGi bundle resource access,
 * resource loading via {@code Class.getResource()} / {@code ClassLoader.getResources()},
 * and API type class visibility.
 *
 * <p>Consolidates tests previously in {@code BundleResourceTest}, {@code ResourceLoadingTest},
 * and {@code ClassVisibilityTest} (originally from {@code com.ibm.ws.classloading_bvt}).
 */
@RunWith(FATRunner.class)
public class ClassloaderResourceAndVisibilityFatTest {

    private static final String CONTEXT_ROOT = "loaderApp";

    private static final LibertyServer SERVER = LibertyServerFactory.getLibertyServer("classloading_resource_visibility_FAT");

    private static final String BUNDLE_A_NAME       = "test.classloading.bvt.bundle.a";
    private static final String BUNDLE_B_NAME       = "test.classloading.bvt.bundle.b";
    private static final String BUNDLE_FEATURE_NAME = "classloadingfat-testbundles-1.0";
    private static final String TEST_FEATURE_NAME   = "classloadingfat-testfeature-1.0";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive loaderAppWar = ShrinkHelper.buildDefaultApp(
                "loaderApp.war",
                "io.openliberty.classloading.bvt.app");
        ShrinkHelper.exportAppToServer(SERVER, loaderAppWar,
                                       com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY);
        SERVER.installUserBundle(BUNDLE_A_NAME);
        SERVER.installUserBundle(BUNDLE_B_NAME);
        SERVER.installUserFeature(BUNDLE_FEATURE_NAME);
        SERVER.installSystemFeature(TEST_FEATURE_NAME);
        SERVER.startServer("ClassloaderResourceAndVisibilityFatTest.log");
        SERVER.waitForStringInLog("CWWKT0016I.*loaderApp");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            // CWWKO0221E: port already in use — can occur in local dev environments
            // when another process holds the FAT test port.
            SERVER.stopServer("CWWKO0221E");
        } finally {
            SERVER.uninstallSystemFeature(TEST_FEATURE_NAME);
            SERVER.uninstallUserFeature(BUNDLE_FEATURE_NAME);
            SERVER.uninstallUserBundle(BUNDLE_A_NAME);
            SERVER.uninstallUserBundle(BUNDLE_B_NAME);
        }
    }

    // =======================================================================
    // Section 1: Bundle A resource tests
    // =======================================================================

    /**
     * A plain (non-META-INF/resources) resource located at
     * {@code /test/classloading/resources/HelloWorld.txt} inside bundle A.
     */
    @Test
    public void testOrdinaryResource() throws Exception {
        checkResourceContents("/test/classloading/resources/HelloWorld.txt", "Hello, World!");
    }

    /**
     * A resource with a dotted name inside {@code META-INF/resources/} in bundle A
     * (e.g. the file is literally named {@code test.classloading.resources.HelloWorld.txt}).
     */
    @Test
    public void testDottedResourceInBundleMetaInfResourcesFolder() throws Exception {
        checkResourceContents("META-INF/resources/test.classloading.resources.HelloWorld.txt", "Hello, World.");
    }

    /**
     * An empty file with a dotted name inside {@code META-INF/resources/} in bundle A.
     */
    @Test
    public void testEmptyDottedResourceInBundleMetaInfResourcesFolder() throws Exception {
        checkResourceContents("META-INF/resources/test.classloading.resources.EmptyFile.txt", "");
    }

    /**
     * A resource inside a dotted directory under {@code META-INF/resources/} in bundle A.
     */
    @Test
    public void testResourceInDottedFolder() throws Exception {
        checkResourceContents("META-INF/resources/test.classloading.resources/HelloWorld.txt", "Hello, world.");
    }

    /**
     * An empty resource inside a dotted directory under {@code META-INF/resources/} in bundle A.
     */
    @Test
    public void testEmptyResourceInDottedFolder() throws Exception {
        checkResourceContents("META-INF/resources/test.classloading.resources/EmptyFile.txt", "");
    }

    /**
     * A resource path that does not exist — the servlet should return {@code "null"}.
     */
    @Test
    public void testNonExistentResource() throws Exception {
        checkResourceContents("META-INF/resources/NotHere.txt", "null");
    }

    /**
     * Badly formed resource names should not cause errors; the servlet returns {@code "null"}
     * for each.
     */
    @Test
    public void testBadlyNamedResources_dotDot() throws Exception {
        checkResourceContents("..", "null");
    }

    @Test
    public void testBadlyNamedResources_colon() throws Exception {
        checkResourceContents(":", "null");
    }

    @Test
    public void testBadlyNamedResources_mixedSpecialChars() throws Exception {
        checkResourceContents("://\\:", "null");
    }

    // =======================================================================
    // Section 2: Bundle B resource tests
    // =======================================================================

    /**
     * A plain resource inside bundle B (uses {@code .text} extension).
     */
    @Test
    public void testOrdinaryResourceInBundleB() throws Exception {
        checkResourceContents("/test/classloading/resources/HelloWorld.text", "Hello, World!");
    }

    /**
     * A dotted-name resource inside {@code META-INF/resources/} in bundle B.
     */
    @Test
    public void testDottedResourceInBundleMetaInfResourcesFolderInBundleB() throws Exception {
        checkResourceContents("META-INF/resources/test.classloading.resources.HelloWorld.text", "Hello, World.");
    }

    /**
     * A resource inside a dotted directory under {@code META-INF/resources/} in bundle B.
     */
    @Test
    public void testResourceInDottedFolderInBundleB() throws Exception {
        checkResourceContents("META-INF/resources/test.classloading.resources/HelloWorld.text", "Hello, world.");
    }

    // =======================================================================
    // Section 3: Class.getResource() tests
    // =======================================================================

    /**
     * {@code Class.getResource("")} on {@code HttpServlet} should return the URL
     * ending in {@code /javax/servlet/http/}.
     */
    @Test
    public void testClassResourceLoading_httpServletPackage() throws Exception {
        String response = checkClassResource("javax.servlet.http.HttpServlet", "");
        assertResponseEndsWith(response.split("[\r\n]+"), "/javax/servlet/http/");
    }

    /**
     * {@code Class.getResource("/")} on {@code HttpServlet} should return a non-null URL.
     */
    @Test
    public void testClassResourceLoading_httpServletRoot() throws Exception {
        String response = checkClassResource("javax.servlet.http.HttpServlet", "/");
        assertResponseIsNotNull(response);
    }

    /**
     * {@code Class.getResource("")} on the servlet class itself should return
     * a URL ending in the servlet's package directory.
     */
    @Test
    public void testClassResourceLoading_servletPackage() throws Exception {
        String response = checkClassResource("", "");
        assertResponseEndsWith(response.split("[\r\n]+"), "/io/openliberty/classloading/bvt/app/");
    }

    /**
     * {@code Class.getResource("/")} on the servlet class should return a non-null URL.
     */
    @Test
    public void testClassResourceLoading_servletRoot() throws Exception {
        String response = checkClassResource("", "/");
        assertResponseIsNotNull(response);
    }

    /**
     * {@code Class.getResource("ResourceLoader.class")} on the servlet class should
     * return a URL ending in the {@code .class} file path inside the WAR.
     * Liberty returns a {@code wsjar:} URL of the form
     * {@code wsjar:file://.../loaderApp.war!/WEB-INF/classes/...}.
     */
    @Test
    public void testClassResourceLoading_classFile() throws Exception {
        String response = checkClassResource("", "ResourceLoader.class");
        assertResponseEndsWith(response.split("[\r\n]+"), "/loaderApp.war!/WEB-INF/classes/io/openliberty/classloading/bvt/app/ResourceLoader.class");
    }

    /**
     * {@code Class.getResource("ResourceLoader.java")} should return {@code null}
     * because source files are not on the classpath at runtime.
     */
    @Test
    public void testClassResourceLoading_javaSourceNotFound() throws Exception {
        String response = checkClassResource("", "ResourceLoader.java");
        assertResponseContains(response.split("[\r\n]+"), "null");
    }

    // =======================================================================
    // Section 4: ClassLoader.getResources() tests
    // =======================================================================

    /**
     * {@code ClassLoader.getResources("")} should return at least two roots:
     * one for {@code WEB-INF/classes} and one for the WAR root.
     * Liberty returns {@code wsjar:} URLs of the form
     * {@code wsjar:file://.../loaderApp.war!/WEB-INF/classes/} and
     * {@code wsjar:file://.../loaderApp.war!/}.
     */
    @Test
    public void testClassLoaderResourceLoading_emptyPath() throws Exception {
        String[] classloaderRoots = new String[] { "/loaderApp.war!/WEB-INF/classes/", "/loaderApp.war!/" };
        String[] lines = checkClassloaderResource("").split("[\r\n]+");
        assertTrue(
            "Expected at least " + classloaderRoots.length + " roots for empty path, got " + lines.length
            + ". Lines: " + Arrays.asList(lines),
            lines.length >= classloaderRoots.length);
        assertResponseEndsWith(lines, classloaderRoots);
    }

    /**
     * {@code ClassLoader.getResources("/")} should return the same roots as
     * {@link #testClassLoaderResourceLoading_emptyPath()}.
     */
    @Test
    public void testClassLoaderResourceLoading_rootSlash() throws Exception {
        String[] classloaderRoots = new String[] { "/loaderApp.war!/WEB-INF/classes/", "/loaderApp.war!/" };
        String[] lines = checkClassloaderResource("/").split("[\r\n]+");
        assertTrue(
            "Expected at least " + classloaderRoots.length + " roots for root '/', got " + lines.length
            + ". Lines: " + Arrays.asList(lines),
            lines.length >= classloaderRoots.length);
        assertResponseEndsWith(lines, classloaderRoots);
    }

    /**
     * {@code ClassLoader.getResources("/test")} should return exactly one entry.
     */
    @Test
    public void testClassLoaderResourceLoading_testPath() throws Exception {
        String[] lines = checkClassloaderResource("/test").split("[\r\n]+");
        assertEquals(
            "Expected exactly one entry for path '/test'. Lines: " + Arrays.asList(lines),
            1, lines.length);
    }

    /**
     * {@code ClassLoader.getResources("...ResourceLoader.class")} should return a
     * {@code wsjar:} URL ending in the class file path inside the WAR.
     */
    @Test
    public void testClassLoaderResourceLoading_classFile() throws Exception {
        String[] lines = checkClassloaderResource("io/openliberty/classloading/bvt/app/ResourceLoader.class").split("[\r\n]+");
        assertResponseEndsWith(lines, "/loaderApp.war!/WEB-INF/classes/io/openliberty/classloading/bvt/app/ResourceLoader.class");
    }

    /**
     * {@code ClassLoader.getResources("...ResourceLoader.java")} should return an
     * empty string because source is not on the runtime classpath.
     */
    @Test
    public void testClassLoaderResourceLoading_javaSourceNotFound() throws Exception {
        String response = checkClassloaderResource("io/openliberty/classloading/bvt/app/ResourceLoader.java");
        assertTrue("Expected empty response for .java resource, got: " + response, response.isEmpty());
    }

    // =======================================================================
    // Section 5: Class Visibility tests (API / Internal / Hidden)
    // =======================================================================

    /**
     * {@code javax.servlet.http.HttpServlet} is in the {@code servlet-4.0} feature
     * and must be visible to all web applications.
     */
    @Test
    public void testVisible_httpServlet() throws Exception {
        checkClassLoad("javax.servlet.http.HttpServlet", true);
    }

    /**
     * {@code com.ibm.ws.ffdc.FFDC} is exposed as internal API by the test feature
     * and should therefore be visible.
     */
    @Test
    public void testVisible_ffdc() throws Exception {
        checkClassLoad("com.ibm.ws.ffdc.FFDC", true);
    }

    /**
     * {@code com.ibm.ejs.ras.Tr} is exposed as {@code type="api"} by the test feature
     * and should therefore be visible. This exercises the {@code type="api"}
     * package-export path, distinct from {@code type="internal"}.
     */
    @Test
    public void testVisible_ejsRasTr() throws Exception {
        checkClassLoad("com.ibm.ejs.ras.Tr", true);
    }

    /**
     * {@code com.ibm.ws.artifact.url.URLService} is an internal Liberty service
     * and must not be exposed to applications.
     */
    @Test
    public void testHidden_artifactUrlService() throws Exception {
        checkClassLoad("com.ibm.ws.artifact.url.URLService", false);
    }

    /**
     * {@code com.ibm.ws.artifact.api.ArtifactContainer} is an internal Liberty SPI
     * and must not be exposed to applications.
     */
    @Test
    public void testHidden_artifactContainer() throws Exception {
        checkClassLoad("com.ibm.ws.artifact.api.ArtifactContainer", false);
    }

    /**
     * {@code com.ibm.ws.anno.classsource.ClassSource} is an internal annotation-processing
     * class and must not be visible to applications.
     */
    @Test
    public void testHidden_annoClassSource() throws Exception {
        checkClassLoad("com.ibm.ws.anno.classsource.ClassSource", false);
    }

    /**
     * {@code com.ibm.wsspi.kernel.filemonitor.FileMonitor} is a kernel SPI
     * and must not be exposed to applications.
     */
    @Test
    public void testHidden_fileMonitor() throws Exception {
        checkClassLoad("com.ibm.wsspi.kernel.filemonitor.FileMonitor", false);
    }

    // =======================================================================
    // Helpers
    // =======================================================================

    private void checkResourceContents(String resource, String expectedContent) throws Exception {
        String actualContent = getResourceContent(resource);
        assertEquals("Should be able to retrieve content for resource '" + resource + "'", expectedContent, actualContent);
    }

    private String getResourceContent(String resource) throws Exception {
        URL url = new URL("http://localhost:" + SERVER.getHttpDefaultPort()
                          + "/" + CONTEXT_ROOT
                          + "?resource=" + URLEncoder.encode(resource, "utf-8"));
        String response = HttpUtils.getHttpResponseAsString(url);
        return response != null ? response.replaceAll("[\r\n]+$", "") : null;
    }

    private String checkClassResource(String classname, String resource) throws Exception {
        URL url = new URL("http://localhost:" + SERVER.getHttpDefaultPort()
                          + "/" + CONTEXT_ROOT
                          + "?classname=" + URLEncoder.encode(classname, "utf-8")
                          + "&resource=" + URLEncoder.encode(resource, "utf-8"));
        String response = HttpUtils.getHttpResponseAsString(url).trim();
        assertFalse("Request failed: " + url + " :: " + response, response.contains("FAILURE"));
        return response;
    }

    private String checkClassloaderResource(String resource) throws Exception {
        URL url = new URL("http://localhost:" + SERVER.getHttpDefaultPort()
                          + "/" + CONTEXT_ROOT
                          + "?classloaderResource=" + URLEncoder.encode(resource, "utf-8"));
        String response = HttpUtils.getHttpResponseAsString(url).trim();
        assertFalse("Request failed: " + url + " :: " + response, response.contains("FAILURE"));
        return response;
    }

    private void checkClassLoad(String classname, boolean expectedSuccess) throws Exception {
        String actualContent = getClassLoadResponse(classname).trim();
        String expected = expectedSuccess ? "SUCCESS" : "FAILURE";
        assertEquals((expectedSuccess ? "SHOULD " : "SHOULD NOT ")
                     + "be able to load class '" + classname + "'",
                     expected, actualContent);
    }

    private String getClassLoadResponse(String classname) throws Exception {
        URL url = new URL("http://localhost:" + SERVER.getHttpDefaultPort()
                          + "/" + CONTEXT_ROOT
                          + "?classname=" + URLEncoder.encode(classname, "utf-8"));
        return HttpUtils.getHttpResponseAsString(url);
    }

    private void assertResponseIsNotNull(String response) {
        org.junit.Assert.assertNotNull("Response should not be null", response);
        assertFalse("Response unexpectedly contained literal 'null': " + response,
                    response.trim().equals("null"));
    }

    private void assertResponseEndsWith(String[] responseLines, String... expectedSuffixes) {
        outer:
        for (String suffix : expectedSuffixes) {
            for (String line : responseLines) {
                if (line.contains(suffix))
                    continue outer;
            }
            org.junit.Assert.fail("Response lines " + Arrays.asList(responseLines)
                                  + " did not contain expected suffix: " + suffix);
        }
    }

    private void assertResponseContains(String[] responseLines, String expected) {
        for (String line : responseLines) {
            if (line.contains(expected))
                return;
        }
        org.junit.Assert.fail("Response lines " + Arrays.asList(responseLines)
                              + " did not contain expected substring: " + expected);
    }
}
