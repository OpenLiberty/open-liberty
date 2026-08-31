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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.jsonp22.fat.checker.CacheCheckerServlet;
import io.openliberty.jsonp22.fat.checker.ClassLoaderRef;
import io.openliberty.jsonp22.fat.web.BundledProviderServlet;
import io.openliberty.jsonp22.fat.web.ClassLoaderRegistrationServlet;

/**
 * Tests that undeploying a WAR that populated the JsonProvider WeakHashMap cache
 * allows its classloader to be garbage collected — confirming the cache does not
 * hold a strong reference that would cause a memory leak on redeploy/undeploy cycles.
 *
 * Marked FULL because the GC loop can take up to 5 minutes and GC is non-deterministic.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSONP22ProviderCacheGCTest {

    private static final Logger LOG = Logger.getLogger(JSONP22ProviderCacheGCTest.class.getName());

    private static final String CHECKER_SERVLET  = "/CacheCheckerApp/CacheCheckerServlet";
    private static final String REGISTER_SERVLET = "/BundledProviderApp/ClassLoaderRegistrationServlet";

    @Server("jsonp2.2.cacheGC.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Export gcTestLib.jar to ${shared.resource.dir}/gcTestLib/ so the server.xml
        // <library> element can find it before either WAR is started.
        // The path is relative to the server root; "../../shared/resources" navigates
        // from wlp/usr/servers/<serverName>/ up to wlp/usr/ and into shared/resources/.
        JavaArchive gcTestLib = ShrinkWrap.create(JavaArchive.class, "gcTestLib.jar")
                        .addClass(ClassLoaderRef.class);
        ShrinkHelper.exportToServer(server, "../../shared/resources/gcTestLib", gcTestLib);

        // CacheCheckerApp: always-present, reads from ClassLoaderRef via the shared library.
        WebArchive checkerApp = ShrinkWrap.create(WebArchive.class, "CacheCheckerApp.war")
                        .addClass(CacheCheckerServlet.class);
        ShrinkHelper.exportAppToServer(server, checkerApp);

        // BundledProviderApp: deployed to apps/ (not dropins/) so server.xml applies
        // commonLibraryRef. Will be removed mid-test via server config update.
        String johnzonJarPath = server.getServerSharedPath() + "resources/johnzon/2.1.0/johnzon-core.jar";
        WebArchive bundledProviderApp = ShrinkWrap.create(WebArchive.class, "BundledProviderApp.war")
                        .addClass(BundledProviderServlet.class)
                        .addClass(ClassLoaderRegistrationServlet.class)
                        .addAsLibrary(new File(johnzonJarPath));
        ShrinkHelper.exportAppToServer(server, bundledProviderApp);

        server.addInstalledAppForValidation("CacheCheckerApp");
        server.addInstalledAppForValidation("BundledProviderApp");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Steps:
     * 1. Hit BundledProviderApp/ClassLoaderRegistrationServlet to warm the JsonProvider
     *    cache for its classloader and register that classloader in ClassLoaderRef
     *    (stored in the gcTestLib shared library classloader, visible to both WARs).
     * 2. Confirm the checker can see the classloader (WeakRef not yet null).
     * 3. Undeploy BundledProviderApp by removing it from the server configuration and
     *    waiting for Liberty's CWWKZ0009I (app stopped) message.
     * 4. Poll the checker for up to 300 s; GC is requested server-side on each check call.
     * 5. Assert the checker reports "null" — the classloader was collected, meaning
     *    the WeakHashMap entry was cleaned up and holds no strong reference.
     */
    @Test
    public void testClassLoaderGCdAfterUndeploy() throws Exception {
        // Step 1: warm the provider cache and register BundledProviderApp's classloader.
        assertEquals("registered", getServlet(REGISTER_SERVLET));

        // Step 2: classloader must be registered and live before undeploy.
        // "never-registered" means warmAndRegister() did not run — fail fast.
        String beforeUndeploy = getServlet(CHECKER_SERVLET);
        assertFalse("ClassLoader must be registered and live before undeploy; got: " + beforeUndeploy,
                    "null".equals(beforeUndeploy.trim()) || "never-registered".equals(beforeUndeploy.trim()));

        // Step 3: undeploy BundledProviderApp by removing it from the server configuration.
        // This is a clean graceful stop — Liberty logs CWWKZ0009I when the app is fully stopped.
        server.setMarkToEndOfLog();
        ServerConfiguration config = server.getServerConfiguration();
        config.removeApplicationsByName("BundledProviderApp");
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
        assertNotNull("BundledProviderApp did not stop after config update",
                      server.waitForStringInLogUsingMark("CWWKZ0009I.*BundledProviderApp"));
        server.removeInstalledAppForValidation("BundledProviderApp");

        // Step 4: poll until classloader is collected or timeout.
        // System.gc() is NOT called here — this is the FAT client JVM; the Liberty server is a
        // separate process. GC hints are issued server-side inside CacheCheckerServlet.
        String afterUndeploy = null;
        for (int i = 0; i < 300; i++) {
            Thread.sleep(1000);
            afterUndeploy = getServlet(CHECKER_SERVLET);
            if ("null".equals(afterUndeploy.trim())) {
                break;
            }
            if (i % 30 == 29) {
                LOG.info("Still waiting for classloader GC after " + (i + 1) + "s; last response: " + afterUndeploy);
            }
        }

        // Step 5: assert collected — emit diagnostics first if it failed
        assertEquals("WAR classloader must be GC'd after undeploy; WeakHashMap cache must not hold a strong reference",
                     "null", afterUndeploy.trim());
    }

    /**
     * GET the given server-relative path and return the trimmed response body.
     * Retries up to 3 times with a short back-off to tolerate transient unavailability.
     */
    private String getServlet(String path) throws Exception {
        Exception last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return HttpUtils.getHttpResponseAsString(server, path).trim();
            } catch (Exception e) {
                LOG.warning("getServlet(" + path + ") attempt " + (attempt + 1) + " failed: " + e.getMessage());
                last = e;
                Thread.sleep(500);
            }
        }
        throw last;
    }
}
