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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.regex.Matcher;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServerFactory;

import test.HelloA;
import test.HelloB;
import test.HelloC;
import web.SharedLibraryServlet;

/**
 * Tests for api type visibility with class providers (mock JCA resource adapters).
 * Extends ApiTypeVisibilityLibraryFatTest and re-runs the test matrix using
 * classProviderRef instead of commonLibraryRef.
 * Migrated from WS-CD-Open com.ibm.ws.classloading.ApiTypeVisibilityClassProviderFatTest.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class ApiTypeVisibilityClassProviderFatTest extends ApiTypeVisibilityLibraryFatTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = LibertyServerFactory.getLibertyServer("classloader_FAT_Server");
        server.installSystemBundle("mock.jca");
        server.installSystemFeature("mock.jca-1.0");
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

        SharedLibFatTest.setConfig(server, "ApiTypeVisibilityClassProvider/server.xml",
                                   ApiTypeVisibilityClassProviderFatTest.class.getSimpleName());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // CWWKL0033W is expected — some apps are configured with mismatched API types for the class provider
        server.stopServer("CWWKL0033W");
        server.uninstallSystemBundle("mock.jca");
        server.uninstallSystemFeature("mock.jca-1.0");
        server.uninstallSystemFeature("classloadingfatlibertytestfeature-1.0");
    }

    @Override
    @Test
    @Ignore("not testing private libraries because there is no equivalent for class providers")
    public void test1__AppWithDefaultApis_UsingLibraryThruAPrivateRef() {}

    @Override
    @Test
    @Ignore("not testing private libraries because there is no equivalent for class providers")
    public void test6__AppWithTwoApiTypes_UsingLibraryThruAPrivateRef() {}

    @Override
    @Test
    @Ignore("not testing private libraries because there is no equivalent for class providers")
    public void test11_AppWithExplicitApi_UsingLibraryThruAPrivateRef() {}

    @Override
    String getExpectedAppName(ApiTypeSet expectedAppSet, ApiTypeSet expectedLibSet) {
        return "app" + expectedAppSet + "_ra" + expectedLibSet;
    }

    @Override
    void expectMismatchedApiTypes(String appName, ApiTypeSet expectedLoaderSet, ApiTypeSet expectedProviderSet) {
        // app should have come up with one error
        String error = server.waitForStringInLog("CWWKL0033W:.*" + appName, 5000L);
        assertNotNull("There should be an error in the log for app " + appName, error);
        // find the actual app set and lib set
        Matcher matcher = API_SET_PATTERN.matcher(error);
        assertTrue("The first set of double square brackets should contain the loader's apiTypeVisibility", matcher.find());
        Set<String> actualLoaderSet = splitAndSort(matcher.group(1));
        assertTrue("The second set of double square brackets should contain the provider's apiTypeVisibility", matcher.find());
        Set<String> actualProviderSet = splitAndSort(matcher.group(1));
        assertEquals("Check the loader's apiTypeVisibility", expectedLoaderSet.types, actualLoaderSet);
        assertEquals("Check the provider's apiTypeVisibility", expectedProviderSet.types, actualProviderSet);
    }
}
