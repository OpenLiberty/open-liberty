/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache.internal.fat;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

/**
 * This test will test a JCache provider being used directly from an application.
 * This doesn't necessarily mean the JCache provider is using JCache, just that the provider
 * can load and is not regressed by our JCache feature.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(JakartaEE9Action.ID) // No value gained
public class JCacheProviderInAppTest extends BaseTestCase {
    private final Class<?> CLASS = JCacheProviderInAppTest.class;

    @Server("io.openliberty.jcache.internal.fat.provider.in.app.1")
    public static LibertyServer server1;

    @Before
    public void before() throws Exception {
        String groupName = UUID.randomUUID().toString();

        /*
         * Start server 1.
         */
        server1.addInstalledAppForValidation("providerinapp");
        startServer1(server1, groupName, null, null);
        waitForDefaultHttpsEndpoint(server1);
        waitForCachingProvider(server1, AUTH_CACHE_NAME);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);
    }

    @After
    public void after() throws Exception {

        try {
            /*
             * We should not have cleared the authentication cache's JCache.
             */
            assertAuthCacheJCacheNotCleared(server1);
        } finally {
            stopServer(server1, "SRVE0777E");
        }
    }

    /**
     * Test the JCache provider from within an application (which doesn't use our internal JCache services).
     *
     * @throws Exception If the test fails for some unforeseen reason.
     */
    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    public void testProviderInApp() throws Exception {
        final String METHOD_NAME = "testProviderInApp";

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
