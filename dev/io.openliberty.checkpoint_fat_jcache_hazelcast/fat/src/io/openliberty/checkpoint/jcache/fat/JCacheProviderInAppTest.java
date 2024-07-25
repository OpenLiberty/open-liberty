/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package io.openliberty.checkpoint.jcache.fat;

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.HttpSessionCache;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This test will test a JCache provider being used directly from an application.
 * This doesn't necessarily mean the JCache provider is using JCache, just that the provider
 * can load and is not regressed by our JCache feature.
 */
@SkipIfSysProp("skip.tests=true")
@RunWith(FATRunner.class)
@CheckpointTest
public class JCacheProviderInAppTest extends BaseTestCase {
    private final Class<?> CLASS = JCacheProviderInAppTest.class;

    private static final String SERVER1 = "io.openliberty.jcache.internal.fat.provider.in.app.1";

    @Server(SERVER1)
    public static LibertyServer server1;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.defaultMPRepeat(new String[] { SERVER1 });

    private String groupName;

    @BeforeClass
    public static void beforeClass() {
        assumeShouldNotSkipTests();
    }

    @Before
    public void before() throws Exception {
        groupName = UUID.randomUUID().toString();

        /*
         * Transform apps for EE9+.
         */
        if (JakartaEEAction.isEE9OrLaterActive()) {
            JakartaEEAction.transformApp(Paths.get(server1.getServerRoot() + "/apps/providerinapp.war"));
        }
    }

    @After
    public void after() throws Exception {

        try {
            /*
             * We should not have cleared the authentication cache's JCache.
             */
            assertAuthCacheJCacheNotCleared(server1);
        } finally {
            /*
             * SESN0306E, CWWKE1102W and CWWKE1107W - Occur due to session cache invalidation reaper not shutting down during server shutdown.
             */
            stopServer(server1, "SRVE0777E", "SESN0306E", "CWWKE1102W", "CWWKE1107W");
        }
    }

    /**
     * Test the JCache provider from within an application (which doesn't use our internal JCache services).
     *
     * @throws Exception If the test fails for some unforeseen reason.
     */
    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    @AllowedFFDC({ "com.ibm.websphere.objectgrid.ObjectGridRuntimeException", "com.ibm.websphere.objectgrid.TransactionException" })
    public void providerInApp_default() throws Exception {
        final String METHOD_NAME = "providerInApp_default";

        startServer1(server1, groupName, null, null);
        waitForDefaultHttpsEndpoint(server1);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);

        HttpGet getRequest = new HttpGet("http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/providerinapp/ProviderInAppServlet");
        try (CloseableHttpClient httpClient = HttpUtils.getInsecureHttpsClient(USER1_NAME, USER1_PASSWORD)) {

            /*
             * Call the ProviderInApp servlet.
             */
            Log.info(CLASS, METHOD_NAME, "Calling the ProviderInApp servlet at: " + getRequest.getURI());
            try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
                assertEquals("Expected 200 status code.", HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            }
        }
    }

    /**
     * Test the JCache provider from within an application (which doesn't use our internal JCache services). We also
     * will be enabling the sessionCache feature to force use of both the gateway and core jcache bundles.
     *
     * @throws Exception If the test fails for some unforeseen reason.
     */
    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    @AllowedFFDC({ "javax.cache.event.CacheEntryListenerException", "com.ibm.websphere.objectgrid.ObjectGridRuntimeException",
                   "com.ibm.websphere.objectgrid.TransactionException" })
    public void providerInApp_sessioncache() throws Exception {
        final String METHOD_NAME = "providerInApp_sessioncache";

        /*
         * Configure the server to use sessionCache-1.0. This will use both the core and gateway jcache bundles.
         */
        ServerConfiguration config = server1.getServerConfiguration().clone();
        config.getFeatureManager().getFeatures().add("sessionCache-1.0");
        HttpSessionCache sessionCache = new HttpSessionCache();
        sessionCache.setCacheManagerRef("CacheManager");
        config.getHttpSessionCaches().add(sessionCache);
        server1.updateServerConfiguration(config);

        startServer1(server1, groupName, null, null);
        waitForDefaultHttpsEndpoint(server1);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);

        HttpGet getRequest = new HttpGet("http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/providerinapp/ProviderInAppServlet");
        try (CloseableHttpClient httpClient = HttpUtils.getInsecureHttpsClient(USER1_NAME, USER1_PASSWORD)) {

            /*
             * Call the ProviderInApp servlet.
             */
            Log.info(CLASS, METHOD_NAME, "Calling the ProviderInApp servlet at: " + getRequest.getURI());
            try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
                assertEquals("Expected 200 status code.", HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            }
        }
    }
}
