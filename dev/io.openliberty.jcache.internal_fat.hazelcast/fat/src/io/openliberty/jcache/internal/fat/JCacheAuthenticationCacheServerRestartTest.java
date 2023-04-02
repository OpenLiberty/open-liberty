/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.jcache.internal.fat;

import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.ServletClient;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Contains distributed JCache authentication cache tests for server restart.
 */
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES })
@SkipIfSysProp("skip.tests=true")
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JCacheAuthenticationCacheServerRestartTest extends BaseTestCase {

    private static BasicAuthClient basicAuthClient2;

    // This server will stay up for cache topologies where the servers are members of the cache.
    @Server("io.openliberty.jcache.internal.fat.auth.cache.restart.1")
    public static LibertyServer server1;

    // This server will be restarted.
    @Server("io.openliberty.jcache.internal.fat.auth.cache.restart.2")
    public static LibertyServer server2;

    @BeforeClass
    public static void beforeClass() {
        assumeShouldNotSkipTests();
    }

    @Before
    public void before() throws Exception {
        String groupName = UUID.randomUUID().toString();

        /*
         * Start server 1.
         */
        server1.addInstalledAppForValidation("basicauth");
        startServer1(server1, groupName, null, null);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);
        waitForDefaultHttpsEndpoint(server1);

        /*
         * Start server 2.
         */
        server2.addInstalledAppForValidation("basicauth");
        startServer2(server2, groupName);
        basicAuthClient2 = new BasicAuthClient(server2);
        waitForCreatedOrExistingJCache(server2, AUTH_CACHE_NAME);
        waitForDefaultHttpsEndpoint(server2);
    }

    @After
    public void after() throws Exception {
        try {
            /*
             * We should not have cleared the authentication cache's JCache.
             */
            assertAuthCacheJCacheNotCleared(server1);
            assertAuthCacheJCacheNotCleared(server2);
        } finally {
            /*
             * Stop servers in the reverse order they were started.
             */
            try {
                stopServer(server2);
            } finally {
                stopServer(server1);
            }
        }
    }

    /**
     * Test caching of LTPA cookies in the distributed JCache authentication cache with
     * a server restart. One server (server1) will remain up to maintain the cache in
     * topologies where the Liberty servers are members of the cache.
     *
     * <pre>
     * 1. Call server2 using basic authentication. This will result in a cache miss and will insert an entry
     *    into the distributed authentication cache.
     * 2. Call server2 using LTPA cookie from the first request to show a cache hit.
     * 3. Restart server2.
     * 4. Call server2 using the LTPA cookie from the first request. This will result in an authentication cache hit since the request
     *    to server2 cached it in the distributed authentication cache and the server restart should not clear the cache.
     * </pre>
     *
     * @throws Exception if the test fails for some unforeseen reason.
     */
    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    public void authCache_restart() throws Exception {

        /*
         * 1. Call server2 using basic authentication. This will result in a cache miss and will insert an entry
         * into the distributed authentication cache.
         */
        String response = basicAuthClient2.accessProtectedServletWithAuthorizedCredentials(ServletClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient2.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheBasicAuthCacheHit(false, server2);

        /*
         * 2. Call server2 using LTPA cookie from the first request to verify the entry is
         * cached before restarting the server.
         */
        response = basicAuthClient2.accessProtectedServletWithAuthorizedCookie(ServletClient.PROTECTED_ALL_ROLE, basicAuthClient2.getCookieFromLastLogin());
        assertTrue("Did not get the expected response", basicAuthClient2.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheLtpaAuthCacheHit(true, server2, basicAuthClient2.getCookieFromLastLogin());

        /*
         * 3. Restart server2.
         */
        server2.restartServer();

        /*
         * 4. Call server2 using LTPA token from first request. This will result in a cache hit because
         * the distributed authentication cache has maintained entries across the sever restart.
         */
        response = basicAuthClient2.accessProtectedServletWithAuthorizedCookie(ServletClient.PROTECTED_ALL_ROLE, basicAuthClient2.getCookieFromLastLogin());
        assertTrue("Did not get the expected response", basicAuthClient2.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheLtpaAuthCacheHit(true, server2, basicAuthClient2.getCookieFromLastLogin());

    }
}
