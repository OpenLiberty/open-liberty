/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache.internal.fat;

import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.ServletClient;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.jcache.internal.fat.plugins.TestPluginHelper;

/**
 * Contains distributed JCache authentication cache tests for LTPA and basic authentication.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@SuppressWarnings("restriction")
public class JCacheAuthenticationCacheTest extends BaseTestCase {

    private static BasicAuthClient basicAuthClient1;
    private static BasicAuthClient basicAuthClient2;
    private static final int TTL_SECONDS = 10;

    @Server("io.openliberty.jcache.internal.fat.auth.cache.1")
    public static LibertyServer server1;

    @Server("io.openliberty.jcache.internal.fat.auth.cache.2")
    public static LibertyServer server2;

    @Before
    public void before() throws Exception {
        String groupName = UUID.randomUUID().toString();

        /*
         * Start server 1.
         */
        server1.addInstalledAppForValidation("basicauth");
        startServer1(server1, groupName, 25000, TTL_SECONDS);
        basicAuthClient1 = new BasicAuthClient(server1);
        waitForCachingProvider(server1, AUTH_CACHE_NAME);
        if (TestPluginHelper.getTestPlugin().cacheShouldExistBeforeTest()) {
            waitForExistingJCache(server1, AUTH_CACHE_NAME);
        } else {
            waitForCreatedJCache(server1, AUTH_CACHE_NAME);
        }

        /*
         * Start server 2.
         */
        server2.addInstalledAppForValidation("basicauth");
        startServer2(server2, groupName);
        basicAuthClient2 = new BasicAuthClient(server2);
        waitForCachingProvider(server2, AUTH_CACHE_NAME);
        waitForExistingJCache(server2, AUTH_CACHE_NAME);
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
     * Test caching of basic authentication credentials in the distributed JCache authentication cache. This
     * test will use two Liberty servers that share the distributed JCache authentication cache.
     *
     * <pre>
     * 1. Call server1 using basic authentication. This will result in a cache miss and will insert an entry
     *    into the distributed authentication cache.
     * 2. Call server2 using basic authentication. This will result in an authentication cache hit since the request
     *    to server1 cached it in the distributed authentication cache.
     * 3. Wait TTL seconds to make the same request to server2 again. This time the entry will have been evicted since
     *    the TTL elapsed and we will get a cache miss.
     * </pre>
     *
     * @throws Exception if the test fails for some unforeseen reason.
     */
    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    public void authCache_basicauth() throws Exception {

        /*
         * 1. Call server1 using basic authentication. This will result in a cache miss and will insert an entry
         * into the distributed authentication cache.
         */
        String response = basicAuthClient1.accessProtectedServletWithAuthorizedCredentials(ServletClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient1.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertBasicAuthCacheHit(false, server1);

        /*
         * 2. Call server2 using basic authentication. This will result in an authentication cache hit since the request
         * to server1 cached it in the distributed authentication cache.
         */
        response = basicAuthClient2.accessProtectedServletWithAuthorizedCredentials(ServletClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient2.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertBasicAuthCacheHit(true, server2);

        /*
         * 3. Wait TTL seconds to make the same request to server2 again. This time the entry will have been evicted since
         * the TTL elapsed and we will get a cache miss.
         */
        if (!TestPluginHelper.getTestPlugin().skipTtlTest()) {
            Thread.sleep((TTL_SECONDS + 2) * 1000);
            basicAuthClient2.resetClientState(); // Clear tokens, etc
            resetMarksInLogs(server2);
            response = basicAuthClient2.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
            assertTrue("Did not get the expected response", basicAuthClient2.verifyResponse(response, USER1_NAME, false, false));
            assertResponseContainsCustomCredentials(response);
            assertBasicAuthCacheHit(false, server2);
        }
    }

    /**
     * Test caching of LTPA cookies in the distributed JCache authentication cache. This
     * test will use two Liberty servers that share the distributed JCache authentication cache.
     *
     * <pre>
     * 1. Call server1 using basic authentication. This will result in a cache miss and will insert an entry
     *    into the distributed authentication cache.
     * 2. Call server2 using the LTPA cookie from the first request. This will result in an authentication cache hit since the request
     *    to server1 cached it in the distributed authentication cache.
     * 3. Wait TTL seconds to make the same request to server2 again. This time the entry will have been evicted since
     *    the TTL elapsed and we will get a cache miss.
     * </pre>
     *
     * @throws Exception if the test fails for some unforeseen reason.
     */
    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    public void authCache_ltpa() throws Exception {
        /*
         * 1. Call server1 using basic authentication. This will result in a cache miss and will insert an entry
         * into the distributed authentication cache.
         */
        String response = basicAuthClient1.accessProtectedServletWithAuthorizedCredentials(ServletClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient1.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertBasicAuthCacheHit(false, server1);

        /*
         * 2. Call server2 using the LTPA cookie from the first request. This will result in an authentication cache hit since the request
         * to server1 cached it in the distributed authentication cache.
         */
        response = basicAuthClient2.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_ALL_ROLE, basicAuthClient1.getCookieFromLastLogin());
        assertTrue("Did not get the expected response", basicAuthClient2.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertLtpaAuthCacheHit(true, server2, basicAuthClient1.getCookieFromLastLogin());

        /*
         * 3. Wait TTL seconds to make the same request to server2 again. This time the entry will have been evicted since
         * the TTL elapsed and we will get a cache miss.
         */
        if (!TestPluginHelper.getTestPlugin().skipTtlTest()) {
            Thread.sleep((TTL_SECONDS * 2) * 1000);
            resetMarksInLogs(server2);
            response = basicAuthClient2.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_ALL_ROLE, basicAuthClient1.getCookieFromLastLogin());
            assertTrue("Did not get the expected response", basicAuthClient2.verifyResponse(response, USER1_NAME, false, false));
            assertResponseContainsCustomCredentials(response);
            assertLtpaAuthCacheHit(false, server2, basicAuthClient1.getCookieFromLastLogin());
        }
    }
}
