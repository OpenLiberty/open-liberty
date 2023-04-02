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

import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient.LogoutOption;
import com.ibm.ws.webcontainer.security.test.servlets.SSLFormLoginClient;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Contains distributed JCache logged out server restart cookie cache test for LTPA.
 * This won't work in hazelcast because hazelcast runs the cache on the Liberty server.
 */
@SkipIfSysProp("skip.tests=true")
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES })
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JCacheLtpaLoggedOutCookieCacheServerRestartTest extends BaseTestCase {

    // This server will stay up for cache topologies where the servers are members of the cache.
    @Server("io.openliberty.jcache.internal.fat.ltpa.cookie.cache.restart.1")
    public static LibertyServer server1;

    // This server will be restarted.
    @Server("io.openliberty.jcache.internal.fat.ltpa.cookie.cache.restart.2")
    public static LibertyServer server2;

    private static FormLoginClient formLoginClient2;

    @BeforeClass
    public static void beforeClass() throws Exception {
        assumeShouldNotSkipTests();
    }

    @Before
    public void before() throws Exception {
        UUID groupName = UUID.randomUUID();

        /*
         * Start server1.
         */
        server1.addInstalledAppForValidation("formlogin");
        startServer1(server1, groupName.toString(), null, null);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);
        waitForDefaultHttpsEndpoint(server1);

        /*
         * Start server1.
         */
        server2.addInstalledAppForValidation("formlogin");
        startServer2(server2, groupName.toString());
        waitForCreatedOrExistingJCache(server2, AUTH_CACHE_NAME);
        waitForDefaultHttpsEndpoint(server2);

        formLoginClient2 = new SSLFormLoginClient(server2, FormLoginClient.DEFAULT_SERVLET_NAME, FormLoginClient.DEFAULT_CONTEXT_ROOT);
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
     * Test caching of logged out LTPA cookies in the distributed JCache logged out cookie cache.
     * One server (server1) will remain up to maintain the cache in topologies where the Liberty
     * servers are members of the cache.
     *
     * <pre>
     * 1. Call server2 using form authentication. This will result in a cache miss and will insert an entry
     *    into the distributed authentication cache.
     * 2. Call server2 with the LTPA cookie. This will result in a cache miss because there are no
     *    cookies in the logged out cookie cache.
     * 3. Logout the LTPA cookie. This will store the LTPA cookie in the distributed JCache logged out cookie cache.
     * 4. Call server2 with the LTPA cookie to ensure a cache hit before restarting the server.
     * 5. Restart the server
     * 6. Call server2 with the LTPA cookie. This will result in a LogOut cache hit because
     *    the distributed logged out cookie cache has maintained entries across the sever restart.
     * </pre>
     *
     * @throws Exception if the test fails for some unforeseen reason.
     */
    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    public void loggedOutCookieCache_ltpa_restart() throws Exception {
        /**
         * 1. Call server2 using form authentication. This will result in a cache miss because there are no
         * cookies in the logged out cookie cache.
         *
         * There are a few requests here.
         *
         * <pre>
         * 1. GET /formlogin/AllRoleServlet
         *    - Responds with redirect to login.jsp
         * 2. GET /formlogin/login.jsp
         * 3. POST formlogin/j_security_check.
         *    - Responds with redirect to endpoint and sends back the LTPA cookie
         * 4. GET /formlogin/AllRoleServlet
         *    - Uses LTPA cookie to make the request
         *    - Should result in a cache miss.
         * </pre>
         */
        String response = formLoginClient2.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        String ltpaCookie = formLoginClient2.getCookieFromLastLogin();
        assertTrue("Did not get the expected response", formLoginClient2.verifyResponse(response, USER1_NAME, true, false));
        assertResponseContainsCustomCredentials(response);
        assertLtpaLoggedOutCookieCacheHit(false, server2, ltpaCookie);

        /*
         * 2. Call server2 with the LTPA cookie. This will result in a cache miss because there are no
         * cookies in the logged out cookie cache.
         */
        resetMarksInLogs(server2);
        response = formLoginClient2.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_ALL_ROLE, ltpaCookie);
        assertTrue("Did not get the expected response", formLoginClient2.verifyResponse(response, USER1_NAME, true, false));
        assertResponseContainsCustomCredentials(response);
        assertLtpaLoggedOutCookieCacheHit(false, server2, ltpaCookie);

        /*
         * 3. Logout the LTPA cookie. This will store the LTPA cookie in the distributed JCache logged out cookie cache.
         */
        resetMarksInLogs(server2);
        formLoginClient2.formLogout(LogoutOption.LOGOUT_DEFAULT_PAGE);

        /*
         * 4. Call server2 with the LTPA cookie to ensure a cache hit before restarting the server.
         */
        resetMarksInLogs(server2);
        assertTrue("Was not redirected to the login page.", formLoginClient2.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_ALL_ROLE, ltpaCookie));
        assertResponseContainsCustomCredentials(response);
        assertLtpaLoggedOutCookieCacheHit(true, server2, ltpaCookie);

        /*
         * 5. Restart the server
         */
        server2.restartServer();
        waitForCreatedOrExistingJCache(server2, AUTH_CACHE_NAME);
        waitForDefaultHttpsEndpoint(server2);

        /*
         * 6. Call server2 with the LTPA cookie. This will result in a LogOut cache hit because
         * the distributed logged out cookie cache has maintained entries across the sever restart.
         */
        resetMarksInLogs(server2);
        assertTrue("Was not redirected to the login page.", formLoginClient2.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_ALL_ROLE, ltpaCookie));
        assertResponseContainsCustomCredentials(response);
        assertLtpaLoggedOutCookieCacheHit(true, server2, ltpaCookie);
    }
}
