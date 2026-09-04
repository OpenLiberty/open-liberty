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

import static io.openliberty.classloading.base.fat.ApiTypeVisibilityLibraryFatTest.ApiTypeSet.ALLTYPES;
import static io.openliberty.classloading.base.fat.ApiTypeVisibilityLibraryFatTest.ApiTypeSet.DEFAULTS;
import static io.openliberty.classloading.base.fat.ApiTypeVisibilityLibraryFatTest.ApiTypeSet.EMPTYSET;
import static io.openliberty.classloading.base.fat.ApiTypeVisibilityLibraryFatTest.ApiTypeSet.EXPLICIT;
import static io.openliberty.classloading.base.fat.ApiTypeVisibilityLibraryFatTest.ApiTypeSet.PRIVLIBS;
import static io.openliberty.classloading.base.fat.ApiTypeVisibilityLibraryFatTest.ApiTypeSet.SPECONLY;
import static io.openliberty.classloading.base.fat.ApiTypeVisibilityLibraryFatTest.ApiTypeSet.SPEC_3RD;
import static java.util.Collections.EMPTY_LIST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

import test.HelloA;
import test.HelloB;
import test.HelloC;
import web.SharedLibraryServlet;

/**
 * Test matching of library and application api type sets.
 * Migrated from WS-CD-Open com.ibm.ws.classloading.ApiTypeVisibilityLibraryFatTest.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class ApiTypeVisibilityLibraryFatTest {

    // NOTE: test numbers correspond to test matrix described in server.xml
    @Test public void test1__AppWithDefaultApis_UsingLibraryThruAPrivateRef() throws Exception {expectApp(DEFAULTS, PRIVLIBS);}
    @Test public void test2__AppWithDefaultApis_UsingLibraryWithDefaultApis() throws Exception {expectApp(DEFAULTS, DEFAULTS);}
    @Test public void test2a_AppWithDefaultApis_UsingLibraryWithExplicitApi() throws Exception {expectApp(DEFAULTS, EXPLICIT);}
    @Test public void test3__AppWithDefaultApis_UsingLibraryWithEmptyApiSet() throws Exception {expectApp(DEFAULTS, EMPTYSET);}
    @Test public void test4__AppWithDefaultApis_UsingLibraryWithAllApiTypes() throws Exception {expectApp(DEFAULTS, ALLTYPES);}
    @Test public void test5__AppWithDefaultApis_UsingLibraryWithTwoApiTypes() throws Exception {expectApp(DEFAULTS, SPEC_3RD);}
    @Test public void test6__AppWithTwoApiTypes_UsingLibraryThruAPrivateRef() throws Exception {expectApp(SPEC_3RD, PRIVLIBS);}
    @ExpectedFFDC({ "jakarta.servlet.UnavailableException", "java.lang.NoClassDefFoundError" })
    @Test public void test7__AppWithEmptyApiSet_UsingLibraryWithEmptyApiSet() throws Exception {expectApp(EMPTYSET, EMPTYSET);}
    @Test public void test7a_AppWithTwoApiTypes_UsingLibraryWithTwoApiTypes() throws Exception {expectApp(SPEC_3RD, SPEC_3RD);}
    @Test public void test7b_AppWithExplicitApi_UsingLibraryWithDefaultApis() throws Exception {expectApp(EXPLICIT, DEFAULTS);}
    @Test public void test8__AppWithAllApiTypes_UsingLibraryWithTwoApiTypes() throws Exception {expectApp(ALLTYPES, SPEC_3RD);}
    @Test public void test9__AppWithOnlySpecApi_UsingLibraryWithTwoApiTypes() throws Exception {expectApp(SPECONLY, SPEC_3RD);}
    @Test public void test10_AppWithTwoApiTypes_UsingLibraryWithDefaultApis() throws Exception {expectApp(SPEC_3RD, DEFAULTS);}
    @Test public void test11_AppWithExplicitApi_UsingLibraryThruAPrivateRef() throws Exception {expectApp(EXPLICIT, PRIVLIBS);}

    static final Pattern API_SET_PATTERN = Pattern.compile("\\[\\[([^]]*)\\]\\]"); // matches "[[foo]]" and captures "foo" as group 1

    private static final Map<String, String> EXAMPLE_TYPES = new HashMap<String, String>();
    static {
        EXAMPLE_TYPES.put("spec", "jakarta.servlet.Servlet");
        EXAMPLE_TYPES.put("ibm-api", "com.ibm.websphere.servlet.session.IBMSession");
        EXAMPLE_TYPES.put("api", "com.ibm.wsspi.classloading.ResourceProvider");
        EXAMPLE_TYPES.put("third-party", "org.osgi.util.tracker.ServiceTracker");
    }

    private static final Comparator<String> ORDERING = new Comparator<String>() {

        @Override
        public int compare(String s1, String s2) {
            int o1 = ordinal(s1);
            int o2 = ordinal(s2);
            // if they are both unknown strings, use natural ordering
            return ((o1 & o2) == 4) ? s1.compareTo(s2) : o1 - o2;
        }

        private int ordinal(String s) {
            if (s.equals("spec"))
                return 0;
            if (s.equals("ibm-api"))
                return 1;
            if (s.equals("api"))
                return 2;
            if (s.equals("third-party"))
                return 3;
            return 4;
        }

    };

    enum ApiTypeSet {
        PRIVLIBS("no need to check - everything should match"),
        EMPTYSET(""),
        SPECONLY("spec"),
        SPEC_3RD("spec,third-party"),
        DEFAULTS("spec,ibm-api,api,stable"),
        EXPLICIT("spec,ibm-api,api"),
        ALLTYPES("spec,ibm-api,api,third-party");
        final Set<String> types;

        ApiTypeSet(String types) {
            this.types = splitAndSort(types);
        }
    }

    static LibertyServer server = null;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = LibertyServerFactory.getLibertyServer("classloader_FAT_Server");
        server.installSystemFeature("classloadingfatlibertytestfeature-1.0");

        // Deploy sharedLib.war
        WebArchive sharedLibWar = ShrinkWrap.create(WebArchive.class, "sharedLib.war")
                .addClass(SharedLibraryServlet.class);
        ShrinkHelper.addDirectory(sharedLibWar, "test-applications/sharedLib.war/resources");
        ShrinkHelper.exportToServer(server, "apps", sharedLibWar);

        // Deploy library jars needed by this test
        ShrinkHelper.exportToServer(server, "SharedLibraryA",
                ShrinkHelper.buildJavaArchive("sharedLibraryA.jar", HelloA.class.getPackage().getName()));
        ShrinkHelper.exportToServer(server, "SharedLibraryB",
                ShrinkHelper.buildJavaArchive("sharedLibraryB.jar", HelloB.class.getPackage().getName()));
        ShrinkHelper.exportToServer(server, "SharedLibraryC",
                ShrinkHelper.buildJavaArchive("sharedLibraryC.jar", HelloC.class.getPackage().getName()));

        SharedLibFatTest.setConfig(server, "ApiTypeVisibilityLibrary/server.xml", ApiTypeVisibilityLibraryFatTest.class.getSimpleName());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // CWWKL0008W is expected — some apps are deliberately configured with mismatched API types
        server.stopServer("CWWKL0008W");
        server.uninstallSystemFeature("classloadingfatlibertytestfeature-1.0");
    }

    private void expectApp(final ApiTypeSet expectedAppSet, final ApiTypeSet expectedLibSet) throws Exception {
        String appName = getExpectedAppName(expectedAppSet, expectedLibSet);
        server.waitForStringInLog("CWWKZ0001I:.*" + appName);
        boolean appAvailable = pingApp(appName);
        boolean appCanSeeSpecAPIs = expectedAppSet.types.contains("spec");
        if (expectedAppSet == expectedLibSet || expectedLibSet == ApiTypeSet.PRIVLIBS || expectedAppSet.types.equals(expectedLibSet.types)) {
            // wait for string in log
            server.waitForStringInTrace("CLASS (LOAD|FAIL).*" + appName);
            // app should have come up without api-type related errors
            List<String> strings = server.findStringsInLogsAndTrace("CWWKL0...E:.*" + appName);
            assertEquals("There should be no errors in the log for app " + appName, EMPTY_LIST, strings);
            // check app availability (check this last because it is the least informative error condition)
        } else {
            expectMismatchedApiTypes(appName, expectedAppSet, expectedLibSet);
        }
        assertEquals("App should have been available if it could see spec APIs", appCanSeeSpecAPIs, appAvailable);

        // now test loading the types
        if (expectedAppSet == EMPTYSET) {
            // special case: cannot even load servlet class so app will fail
            String result = loadClass(appName, "java.lang.Object");
            System.out.println(result);
            assertTrue("App should fail to load", result.contains("SRVE0203E"));
        } else {
            expectAppToLoadClass(appName, "java.lang.Object");
            for (Map.Entry<String,String> entry: EXAMPLE_TYPES.entrySet()) {
                if (expectedAppSet.types.contains(entry.getKey()))
                    expectAppToLoadClass(appName, entry.getValue());
                else
                    expectAppNotToLoadClass(appName, entry.getValue());
            }
        }
    }

    void expectMismatchedApiTypes(String appName, ApiTypeSet expectedAppSet, ApiTypeSet expectedLibSet) {
        // app should have come up with one error
        String error = server.waitForStringInLog("CWWKL0008W:.*" + appName);
        assertNotNull("There should be an error in the log for app " + appName, error);
        // find the actual app set and lib set
        Matcher matcher = API_SET_PATTERN.matcher(error);
        assertTrue("The first set of double square brackets should contain the library's apiTypeVisibility", matcher.find());
        Set<String> actualLibTypes = splitAndSort(matcher.group(1));
        assertTrue("The second set of double square brackets should contain the application's apiTypeVisibility", matcher.find());
        Set<String> actualAppTypes = splitAndSort(matcher.group(1));
        assertEquals("Check the application's apiTypeVisibility", expectedAppSet.types, actualAppTypes);
        assertEquals("Check the library's apiTypeVisibility", expectedLibSet.types, actualLibTypes);
    }

    String getExpectedAppName(ApiTypeSet expectedAppSet, ApiTypeSet expectedLibSet) {
        return "app" + expectedAppSet + "_lib" + expectedLibSet;
    }

    String getErrorFromLog(String appName) {
        return server.waitForStringInLog("CWWKL0008E:.*" + appName);
    }

    private void expectAppToLoadClass(String appName, String className) throws Exception {
        String result = loadClass(appName, className);
        System.out.println(result);
        assertTrue("Class " + className + " should have loaded for app " + appName, result.contains(className));
        assertFalse("Should not have mentioned an exception", result.contains("Exception"));
        assertFalse("Should not have mentioned an error", result.contains("Error"));
    }

    private void expectAppNotToLoadClass(String appName, String className) throws Exception {
        String result = loadClass(appName, className);
        System.out.println(result);
        assertTrue("Class " + className + " should not have loaded for app " + appName, result.contains(className));
        assertTrue("Should have mentioned a CNFE for " + className, result.contains("ClassNotFoundException"));
    }

    static SortedSet<String> splitAndSort(String contents) {
        TreeSet<String> set = new TreeSet<String>(ORDERING);
        set.addAll(Arrays.asList(contents.split(" *, *")));
        return set;
    }

    private String loadClass(String appName, String className) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + appName + "?testName=testCommonLibraryLoadClass&className=" + className);
        String result = HttpUtils.getHttpResponseAsString(url);

        // Strip off the last line.separator
        return result.substring(0, result.lastIndexOf(System.getProperty("line.separator")));
    }

    private boolean pingApp(String appName) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + appName + "?testName=ping");
        String response = HttpUtils.getHttpResponseAsString(url);
        System.out.println("Response received: [" + response + "]");
        return response != null && response.trim().equals("ACK");
    }
}
