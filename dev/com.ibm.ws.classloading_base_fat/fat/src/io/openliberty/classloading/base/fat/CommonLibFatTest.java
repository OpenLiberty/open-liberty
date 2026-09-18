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

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

import com.ibm.ws.classloading.test.ejb.MyStartupSingletonBean;
import test.HelloA;
import test.HelloB;
import test.HelloC;
import test.HelloD;
import web.SharedLibraryServlet;

/**
 * Tests for common shared libraries.
 * Migrated from WS-CD-Open com.ibm.ws.classloading.CommonLibFatTest.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class CommonLibFatTest {

    static final String SHAREDLIB1_STARTED = "CWWKZ0001I:.*sharedLib1";
    static final String SHAREDLIB2_STARTED = "CWWKZ0001I:.*sharedLib2";
    static final String SHAREDLIB3_STARTED = "CWWKZ0001I:.*sharedLib3";
    static final String SHAREDLIB4_STARTED = "CWWKZ0001I:.*sharedLib4";
    static final String SHAREDLIB5_STARTED = "CWWKZ0001I:.*sharedLib5";

    static LibertyServer server = null;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = LibertyServerFactory.getLibertyServer("classloader_FAT_Server");
        server.installSystemFeature("classloadingfatlibertyinternals-1.0");

        // Deploy sharedLib.war (same as SharedLibFatTest.setUpClass)
        WebArchive sharedLibWar = ShrinkWrap.create(WebArchive.class, "sharedLib.war")
                .addClass(SharedLibraryServlet.class);
        ShrinkHelper.addDirectory(sharedLibWar, "test-applications/sharedLib.war/resources");
        ShrinkHelper.exportToServer(server, "apps", sharedLibWar);

        // Deploy library jars — include root-level resource.txt for CommonLib resource tests
        JavaArchive libA = ShrinkHelper.buildJavaArchive("sharedLibraryA.jar", HelloA.class.getPackage().getName())
                .addAsResource(new org.jboss.shrinkwrap.api.asset.StringAsset("resource:libraryA"), "resource.txt")
                .addAsResource(org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE, "test/.keep");
        ShrinkHelper.exportToServer(server, "SharedLibraryA", libA, DeployOptions.OVERWRITE);

        JavaArchive libB = ShrinkHelper.buildJavaArchive("sharedLibraryB.jar", HelloB.class.getPackage().getName())
                .addAsResource(new org.jboss.shrinkwrap.api.asset.StringAsset("resource:libraryB"), "resource.txt")
                .addAsResource(org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE, "test/.keep");
        ShrinkHelper.exportToServer(server, "SharedLibraryB", libB, DeployOptions.OVERWRITE);

        JavaArchive libC = ShrinkHelper.buildJavaArchive("sharedLibraryC.jar", HelloC.class.getPackage().getName())
                .addAsResource(new org.jboss.shrinkwrap.api.asset.StringAsset("resource:libraryC"), "resource.txt")
                .addAsResource(org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE, "test/.keep");
        ShrinkHelper.exportToServer(server, "SharedLibraryC", libC, DeployOptions.OVERWRITE);

        // Deploy sharedLibEJB.ear
        JavaArchive ejbJar = ShrinkHelper.buildJavaArchive("sharedLibEJB.jar",
                MyStartupSingletonBean.class.getPackage().getName());
        EnterpriseArchive ejbEar = ShrinkWrap.create(EnterpriseArchive.class, "sharedLibEJB.ear")
                .addAsModule(ejbJar);
        ShrinkHelper.exportToServer(server, "apps", ejbEar);

        SharedLibFatTest.setConfig(server, "CommonLibrary/server.xml", "CommonLibFatTest",
                                   SHAREDLIB1_STARTED,
                                   SHAREDLIB2_STARTED,
                                   SHAREDLIB3_STARTED,
                                   SHAREDLIB4_STARTED,
                                   SHAREDLIB5_STARTED);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
        server.uninstallSystemFeature("classloadingfatlibertyinternals-1.0");
    }

    /**
     * Test that library classes loaded by different apps via the "libraryRef"
     * attribute have different instance IDs (hashCodes); i.e. they are different
     * instances of the class, loaded separately by each app's classloader.
     *
     * Also test that library classes loaded by different apps via the "commonLibraryRef"
     * attribute have the SAME instance IDs(hashCodes); i.e they are the same instance
     * of the class, loaded by the library's classloader, to which each app delegates.
     */
    @Test
    public void testCommonLibraryLoadClass() throws Exception {
        testCommonLibraryLoadClassHelper("test.HelloA");
        testCommonLibraryLoadClassHelper("test.HelloB");
    }

    /**
     * Test that AppClassLoader.getResource() returns a resource from
     * one of the common library classloaders.
     */
    @Test
    public void testCommonLibraryReadResource() throws Exception {
        final String uri3 = "/sharedLib3/test";
        final String testName = "testName=testCommonLibraryReadResource";
        final String resourceName = "resourceName=resource.txt";

        String res3 = test(createURL(server, uri3, testName + "&" + resourceName));

        // Right now we don't guarantee the order in which the commonLibraryRefs are
        // listed and loaded by the code, and thus we don't guarantee the order of delegation
        // to the common library classloaders. So I can't be sure which classloader the resource
        // was loaded from. App sharedLib3 refs libraryA and libraryB, so it's one or the other.
        assertTrue(res3.equals("resource:libraryA") || res3.equals("resource:libraryB"));
    }

    /**
     * Test that AppClassLoader.getResource() returns a directory resource from
     * one of the common library classloaders.
     */
    @Test
    public void testCommonLibraryReadDirectoryResource() throws Exception {
        final String uri3 = "/sharedLib3/test";
        final String testName = "testName=testCommonLibraryReadDirectoryResource";
        final String resourceName = "resourceName=test";

        String res3 = test(createURL(server, uri3, testName + "&" + resourceName));

        // Right now we don't guarantee to the order in which the commonLibraryRefs are
        // listed and loaded by the code, and thus we don't guarantee the order of delegation
        // to the common library classloaders. So I can't be sure which classloader the resource
        // was loaded from. App sharedLib3 refs libraryA and libraryB, so it's one or the other.
        assertTrue(res3.endsWith("test"));
    }

    /**
     * Test that AppClassLoader.getResources() returns all matching resources from
     * all common library classloaders.
     */
    @Test
    public void testCommonLibraryReadDirectoryResources() throws Exception {
        final String uri3 = "/sharedLib3/test";
        final String uri4 = "/sharedLib4/test";
        final String testName = "testName=testCommonLibraryReadDirectoryResources";
        final String resourceName = "resourceName=test";

        String res3 = test(createURL(server, uri3, testName + "&" + resourceName));
        String res4 = test(createURL(server, uri4, testName + "&" + resourceName));

        // App sharedLib3 refs libraryA and libraryB.
        StringTokenizer tokenizer = new StringTokenizer(res3, "***");
        assertEquals(tokenizer.countTokens(), 2);

        // App sharedLib4 refs libraryA, libraryB, and libraryC.
        tokenizer = new StringTokenizer(res4, "***");
        assertEquals(tokenizer.countTokens(), 3);
    }

    /**
     * Test that AppClassLoader.getResources() returns all matching directory resources from
     * all common library classloaders.
     */
    @Test
    public void testCommonLibraryReadResources() throws Exception {
        final String uri3 = "/sharedLib3/test";
        final String uri4 = "/sharedLib4/test";
        final String testName = "testName=testCommonLibraryReadResources";
        final String resourceName = "resourceName=resource.txt";

        String res3 = test(createURL(server, uri3, testName + "&" + resourceName));
        String res4 = test(createURL(server, uri4, testName + "&" + resourceName));

        // Right now we don't guarantee to the order in which the commonLibraryRefs are
        // listed and loaded by the code, and thus we don't guarantee the order of delegation
        // to the common library classloaders.

        // App sharedLib3 refs libraryA and libraryB.
        assertThat("Did not see the resource from AppSharedLibraryA", res3, containsString("resource:libraryA"));
        assertThat("Did not see the resource from AppSharedLibraryB", res3, containsString("resource:libraryB"));

        // App sharedLib4 refs libraryA, libraryB, and libraryC.
        assertThat("Did not see the resource from AppSharedLibraryA", res4, containsString("resource:libraryA"));
        assertThat("Did not see the resource from AppSharedLibraryB", res4, containsString("resource:libraryB"));
        assertThat("Did not see the resource from AppSharedLibraryC", res4, containsString("resource:libraryC"));
    }

    /**
     * Test that AppClassLoader.getResource() returns the resource from
     * the libraryRef instead of the one from commonLibraryRef. The libraryRef
     * is loaded directly into the AppClassLoader and thus has higher precedence
     * than the commonLibraryRefs.
     */
    @Test
    public void testCommonLibraryReadResourceSearchOrder() throws Exception {
        final String uri5 = "/sharedLib5/test";
        String testName = "testName=testCommonLibraryReadResource";
        final String resourceName = "resourceName=resource.txt";

        String res5 = test(createURL(server, uri5, testName + "&" + resourceName));

        // App sharedLib5 contains libraryRef="A" and commonLibraryRef="B". A will thus
        // have order precedence over B (AppClassLoader is searched before common library
        // classloaders).
        assertTrue(res5.equals("resource:libraryA"));

        // Now test that getResources returns resources from both the libraryRefs and
        // the commonLibraryRefs.
        testName = "testName=testCommonLibraryReadResources";
        res5 = test(createURL(server, uri5, testName + "&" + resourceName));

        // App sharedLib5 refs libraryA and libraryB, so both should be read.
        assertEquals("resource:libraryA;resource:libraryB;", res5);
    }

    /**
     * Helper method for testCommonLibraryLoadClass.
     */
    private void testCommonLibraryLoadClassHelper(String className) throws Exception {
        final String uri1 = "/sharedLib1/test";
        final String uri2 = "/sharedLib2/test";
        final String uri3 = "/sharedLib3/test";
        final String uri4 = "/sharedLib4/test";
        final String testName = "testName=testCommonLibraryLoadClass";
        className = "className=" + className;

        final String class1 = test(createURL(server, uri1, testName + "&" + className));
        final String class2 = test(createURL(server, uri2, testName + "&" + className));
        final String class3 = test(createURL(server, uri3, testName + "&" + className));
        final String class4 = test(createURL(server, uri4, testName + "&" + className));

        // Apps sharedLib3 and sharedLib4 use commonLibraryRef, so they should both
        // see the same instance of the class.
        assertEquals("sharedLib3 and sharedLib4 should both find the same class", class3, class4);

        // Apps sharedLib1 and sharedLib2 use libraryRef, so they should see different
        // instances of the class.
        assertThat("sharedLib1 should find a different class from sharedLib2", class1, not(class2));
        assertThat("sharedLib1 should find a different class from sharedLib3", class1, not(class3));
        assertThat("sharedLib2 should find a different class from sharedLib3", class2, not(class3));
    }

    /**
     * Static helper method to create a URL.
     */
    private static URL createURL(LibertyServer server, String uri, String queryString) throws MalformedURLException {
        return new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + uri + "?" + queryString);
    }

    /**
     * Invoke the given URL and return the output.
     */
    private static String test(URL url) throws Exception {
        String result = HttpUtils.getHttpResponseAsString(url);

        // Strip off the last line.separator
        return result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
    }
}
