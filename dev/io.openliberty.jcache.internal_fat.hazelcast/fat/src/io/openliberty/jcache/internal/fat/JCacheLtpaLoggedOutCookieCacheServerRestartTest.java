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

import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient.LogoutOption;
import com.ibm.ws.webcontainer.security.test.servlets.SSLFormLoginClient;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Contains distributed JCache logged out server restart cookie cache test for LTPA.
 * This won't work in hazelcast because hazelcast runs the cache on the Liberty server.
 */
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JCacheLtpaLoggedOutCookieCacheServerRestartTest extends BaseTestCase {
    @Server("io.openliberty.jcache.internal.fat.ltpa.cookie.cache.restart.1")
    public static LibertyServer server1;

    private static FormLoginClient formLoginClient1;

    @Before
    public void before() throws Exception {
        UUID groupName = UUID.randomUUID();

        /*
         * Start server1.
         */
        server1.addInstalledAppForValidation("formlogin");
        startServer1(server1, groupName.toString(), null, null);
        waitForCachingProvider(server1, COOKIE_CACHE_NAME);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);
        waitForDefaultHttpsEndpoint(server1);

        formLoginClient1 = new SSLFormLoginClient(server1, FormLoginClient.DEFAULT_SERVLET_NAME, FormLoginClient.DEFAULT_CONTEXT_ROOT);
    }

    @After
    public void after() throws Exception {
        /*
         * Stop the server.
         */
        stopServer(server1);
    }

    /**
     * Test caching of logged out LTPA cookies in the distributed JCache logged out cookie cache.
     *
     * <pre>
     * 1. Call server1 using form authentication. This will result in a cache miss and will insert an entry
     *    into the distributed authentication cache.
     * 2. Call server1 with the LTPA cookie. This will result in a cache miss because there are no
     *    cookies in the logged out cookie cache.
     * 3. Call server2 with the LTPA cookie. This will result in a cache miss because there are no
     *    cookies in the logged out cookie cache.
     * 4. Logout the LTPA cookie. This will store the LTPA cookie in the distributed JCache logged out cookie cache.
     * 5. Restart both servers.
     * 6. Call server1 with the LTPA cookie. This will result in a cache hit because it has been logged out.
     * 7. Call server2 with the LTPA cookie. This will result in a cache hit because it has been logged out.
     * </pre>
     *
     * @throws Exception if the test fails for some unforeseen reason.
     */
    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    public void loggedOutCookieCache_ltpa() throws Exception {
        /**
         * 1. Call server1 using form authentication. This will result in a cache miss because there are no
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
        String response = formLoginClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        String ltpaCookie = formLoginClient1.getCookieFromLastLogin();
        assertTrue("Did not get the expected response", formLoginClient1.verifyResponse(response, USER1_NAME, true, false));
        assertResponseContainsCustomCredentials(response);
        assertLtpaLoggedOutCookieCacheHit(false, server1, ltpaCookie);

        /*
         * 2. Call server1 with the LTPA cookie. This will result in a cache miss because there are no
         * cookies in the logged out cookie cache.
         */
        resetMarksInLogs(server1);
        response = formLoginClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_ALL_ROLE, ltpaCookie);
        assertTrue("Did not get the expected response", formLoginClient1.verifyResponse(response, USER1_NAME, true, false));
        assertResponseContainsCustomCredentials(response);
        assertLtpaLoggedOutCookieCacheHit(false, server1, ltpaCookie);

        /*
         * 3. Logout the LTPA cookie. This will store the LTPA cookie in the distributed JCache logged out cookie cache.
         */
        resetMarksInLogs(server1);
        formLoginClient1.formLogout(LogoutOption.LOGOUT_DEFAULT_PAGE);

        /*
         * 4. Call server1 with the LTPA cookie to ensure a cache hit before restarting the server.
         */
        resetMarksInLogs(server1);
        assertTrue("Was not redirected to the login page.", formLoginClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_ALL_ROLE, ltpaCookie));
        assertResponseContainsCustomCredentials(response);
        assertLtpaLoggedOutCookieCacheHit(true, server1, ltpaCookie);

        /*
         * 5. Restart the server
         */
        server1.restartServer();
        waitForCachingProvider(server1, COOKIE_CACHE_NAME);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);
        waitForDefaultHttpsEndpoint(server1);

        /*
         * 6. Call server1 with the LTPA cookie. This will result in a LogOut cache hit because
         * the distributed logged out cookie cache has maintained entries across the sever restart.
         */
        resetMarksInLogs(server1);
        assertTrue("Was not redirected to the login page.", formLoginClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_ALL_ROLE, ltpaCookie));
        assertResponseContainsCustomCredentials(response);
        assertLtpaLoggedOutCookieCacheHit(true, server1, ltpaCookie);
    }
}
