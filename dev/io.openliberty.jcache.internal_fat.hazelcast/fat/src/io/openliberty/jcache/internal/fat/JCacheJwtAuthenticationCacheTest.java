/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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

import java.nio.file.Paths;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.jcache.internal.fat.plugins.TestPluginHelper;

/**
 * Contains distributed JCache authentication cache tests for JWT SSO.
 */
// TODO JtiNonceCache (both of them) for Oidc and mpJwt...
@SkipIfSysProp("skip.tests=true")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JCacheJwtAuthenticationCacheTest extends BaseTestCase {
    private static final Integer TTL_SECONDS = TestPluginHelper.getTestPlugin().skipTtlTest() ? null : 15;

    @Server("io.openliberty.jcache.internal.fat.jwt.auth.cache.1")
    public static LibertyServer server1;

    @Server("io.openliberty.jcache.internal.fat.jwt.auth.cache.2")
    public static LibertyServer server2;

    private static BasicAuthClient basicAuthClient1;
    private static BasicAuthClient basicAuthClient2;

    @BeforeClass
    public static void beforeClass() {
        assumeShouldNotSkipTests();

        /*
         * Transform apps for EE9+.
         */
        if (JakartaEE9Action.isActive()) {
            JakartaEE9Action.transformApp(Paths.get(server1.getServerRoot() + "/apps/basicauth.war"));
            JakartaEE9Action.transformApp(Paths.get(server2.getServerRoot() + "/apps/basicauth.war"));
        } else if (JakartaEE10Action.isActive()) {
            JakartaEE10Action.transformApp(Paths.get(server1.getServerRoot() + "/apps/basicauth.war"));
            JakartaEE10Action.transformApp(Paths.get(server2.getServerRoot() + "/apps/basicauth.war"));
        }
    }

    @Before
    public void before() throws Exception {
        UUID groupName = UUID.randomUUID();

        /*
         * Start server 1.
         */
        server1.addInstalledAppForValidation("basicauth");
        startServer1(server1, groupName.toString(), 25000, TTL_SECONDS);
        waitForDefaultHttpsEndpoint(server1);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);

        /*
         * Start server 2.
         */
        server2.addInstalledAppForValidation("basicauth");
        startServer2(server2, groupName.toString());
        waitForDefaultHttpsEndpoint(server2);
        waitForCreatedOrExistingJCache(server2, AUTH_CACHE_NAME);

        basicAuthClient1 = new SSLBasicAuthClient(server1);
        basicAuthClient2 = new SSLBasicAuthClient(server2);
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
             * Stop the servers in the reverse order they were started.
             */
            try {
                stopServer(server2);
            } finally {
                stopServer(server1);
            }
        }
    }

    /**
     * Test caching of JWT cookies in the distributed JCache authentication cache. This
     * test will use two Liberty servers that share the distributed JCache authentication cache.
     *
     * <pre>
     * 1. Call server1 using basic authentication. This will result in a cache miss and will insert an entry
     *    into the distributed authentication cache.
     * 2. Call server1 using the JWT cookie from the first request. This will result in an authentication cache hit since the first
     *    request to server1 cached it in the distributed authentication cache.
     * 3. Call server2 using the JWT cookie from the first request. This will result in an authentication cache hit since the first
     *    request to server1 cached it in the distributed authentication cache.
     * 4. Wait for the TTL to expire so the cached entry has been evicted. Make a call to server 1 using the JWT cookie from the
     *    first request. This will result in a cache miss.
     * </pre>
     *
     * @throws Exception if the test fails for some unforeseen reason.
     */
    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    public void authCache_jwt() throws Exception {
        /*
         * Configure the client to use JWT cookies.
         */
        basicAuthClient1.setSSOCookieName("JWT");
        basicAuthClient2.setSSOCookieName("JWT");

        /*
         * 1. Call server1 using basic authentication. This will result in a cache miss and will insert an entry
         * into the distributed authentication cache.
         */
        String response = basicAuthClient1.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient1.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        String jwtCookie = basicAuthClient1.getCookieFromLastLogin();

        /*
         * 2. Call server1 using the JWT cookie from the first request. This will result in an authentication cache hit since the first
         * request to server1 cached it in the distributed authentication cache.
         */
        resetMarksInLogs(server1);
        response = basicAuthClient1.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_ALL_ROLE, jwtCookie);
        assertTrue("Did not get the expected response", basicAuthClient1.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheJwtAuthCacheHit(true, server1, jwtCookie);

        /*
         * 3. Call server2 using the JWT cookie from the first request. This will result in an authentication cache hit since the first
         * request to server1 cached it in the distributed authentication cache.
         */
        resetMarksInLogs(server2);
        response = basicAuthClient2.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_ALL_ROLE, jwtCookie);
        assertTrue("Did not get the expected response", basicAuthClient2.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheJwtAuthCacheHit(true, server2, jwtCookie);

        /*
         * 4. Wait for the TTL to expire so the cached entry has been evicted. Make a call to server 1 using the JWT cookie from the
         * first request. This will result in a cache miss.
         */
        if (!TestPluginHelper.getTestPlugin().skipTtlTest()) {
            Thread.sleep((TTL_SECONDS + 2) * 1000);
            resetMarksInLogs(server1);
            response = basicAuthClient1.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_ALL_ROLE, basicAuthClient1.getCookieFromLastLogin());
            assertTrue("Did not get the expected response", basicAuthClient1.verifyResponse(response, USER1_NAME, false, false));
            assertResponseContainsCustomCredentials(response);
            assertJCacheJwtAuthCacheHit(false, server1, basicAuthClient1.getCookieFromLastLogin());
        }
    }

}
