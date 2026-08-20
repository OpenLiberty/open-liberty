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
package io.openliberty.jsonp22.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.jsonp22.fat.provider.NoopJsonProvider;
import io.openliberty.jsonp22.fat.web.BundledProviderServlet;
import io.openliberty.jsonp22.fat.web.FeatureProviderServlet;
import io.openliberty.jsonp22.fat.web.SystemPropertyProviderServlet;

@RunWith(FATRunner.class)
public class JSONP22CustomProviderTest extends FATServletClient {

    @Server("jsonp2.2.customProvider.fat")
    @TestServlets({
                    @TestServlet(servlet = FeatureProviderServlet.class, contextRoot = "ProviderFeatureApp"),
                    @TestServlet(servlet = BundledProviderServlet.class, contextRoot = "BundledProviderApp")
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Guard against stale system property from a prior test run on this JVM.
        System.clearProperty("jakarta.json.provider");

        WebArchive providerFeatureApp = ShrinkWrap.create(WebArchive.class, "ProviderFeatureApp.war")
                        .addClass(FeatureProviderServlet.class)
                        .addClass(SystemPropertyProviderServlet.class)
                        .addClass(NoopJsonProvider.class);
        ShrinkHelper.exportAppToServer(server, providerFeatureApp);

        String johnzonJarPath = server.getServerSharedPath() + "resources/johnzon/2.1.0/johnzon-core.jar";
        WebArchive bundledProviderApp = ShrinkWrap.create(WebArchive.class, "BundledProviderApp.war")
                        .addClass(BundledProviderServlet.class)
                        .addAsLibrary(new File(johnzonJarPath));
        ShrinkHelper.exportAppToServer(server, bundledProviderApp);

        server.addInstalledAppForValidation("ProviderFeatureApp");
        server.addInstalledAppForValidation("BundledProviderApp");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Isolation test with explicit cache-warm ordering.
     *
     * Warm BundledProviderApp's Johnzon cache entry, then assert that ProviderFeatureApp
     * still gets Parsson. A naïve single-entry cache keyed on anything other than the
     * classloader would fail this test.
     */
    @Test
    public void testProviderIsolationAfterCacheWarm() throws Exception {
        // One call is enough to populate the cache entry for BundledProviderApp's classloader.
        runTest(server, "BundledProviderApp/BundledProviderServlet", "testBundledProvider");
        // Feature app must still see Parsson
        runTest(server, "ProviderFeatureApp/FeatureProviderServlet", "testFeatureProvider");
    }

    /**
     * System-property override test, run sequentially to avoid concurrent mutation of
     * the global jakarta.json.provider system property racing with other tests.
     */
    @Test
    public void testSystemPropertyProvider() throws Exception {
        runTest(server, "ProviderFeatureApp/SystemPropertyProviderServlet", "testCacheRestoredAfterSystemPropertyCleared");
    }
}
