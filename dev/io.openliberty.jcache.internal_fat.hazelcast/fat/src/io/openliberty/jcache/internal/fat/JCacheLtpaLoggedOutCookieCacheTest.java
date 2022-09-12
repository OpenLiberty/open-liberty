/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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

import java.nio.file.Paths;
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
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

/**
 * Contains distributed JCache logged out cookie cache tests for LTPA.
 */
@SkipIfSysProp("skip.tests=true")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JCacheLtpaLoggedOutCookieCacheTest extends BaseTestCase {
    @Server("io.openliberty.jcache.internal.fat.ltpa.cookie.cache.1")
    public static LibertyServer server1;

    @Server("io.openliberty.jcache.internal.fat.ltpa.cookie.cache.2")
    public static LibertyServer server2;

    private static FormLoginClient formLoginClient1;
    private static FormLoginClient formLoginClient2;

    @BeforeClass
    public static void beforeClass() {
        assumeShouldNotSkipTests();

        /*
         * Transform apps for EE9+.
         */
        if (JakartaEE9Action.isActive()) {
            JakartaEE9Action.transformApp(Paths.get(server1.getServerRoot() + "/apps/formlogin.war"));
            JakartaEE9Action.transformApp(Paths.get(server2.getServerRoot() + "/apps/formlogin.war"));
        }
    }

    @Before
    public void before() throws Exception {
        UUID groupName = UUID.randomUUID();

        /*
         * Start server1.
         */
        server1.addInstalledAppForValidation("formlogin");
        startServer1(server1, groupName.toString(), null, null);
        waitForDefaultHttpsEndpoint(server1);
        waitForCachingProvider(server1, COOKIE_CACHE_NAME);
        waitForCreatedOrExistingJCache(server1, COOKIE_CACHE_NAME);

        /*
         * Start server2.
         */
        server2.addInstalledAppForValidation("formlogin");
        startServer2(server2, groupName.toString());
        waitForDefaultHttpsEndpoint(server2);
        waitForCachingProvider(server2, COOKIE_CACHE_NAME);
        waitForCreatedOrExistingJCache(server2, COOKIE_CACHE_NAME);

        formLoginClient1 = new SSLFormLoginClient(server1, FormLoginClient.DEFAULT_SERVLET_NAME, FormLoginClient.DEFAULT_CONTEXT_ROOT);
        formLoginClient2 = new SSLFormLoginClient(server2, FormLoginClient.DEFAULT_SERVLET_NAME, FormLoginClient.DEFAULT_CONTEXT_ROOT);
    }

    @After
    public void after() throws Exception {
        /*
         * Stop the servers in the reverse order they were started.
         */
        try {
            stopServer(server2);
        } finally {
            stopServer(server1);
        }
    }

    /**
     * Test caching of logged out LTPA cookies in the distributed JCache logged out cookie cache.
     * This test will use two Liberty servers that share the distributed JCache logged out cookie
     * cache.
     *
     * <pre>
     * 1. Call server1 using form authentication. This will result in a cache miss and will insert an entry
     *    into the distributed authentication cache.
     * 2. Call server1 with the LTPA cookie. This will result in a cache miss because there are no
     *    cookies in the logged out cookie cache.
     * 3. Call server2 with the LTPA cookie. This will result in a cache miss because there are no
     *    cookies in the logged out cookie cache.
     * 4. Logout the LTPA cookie. This will store the LTPA cookie in the distributed JCache logged out cookie cache.
     * 5. Call server1 with the LTPA cookie. This will result in a cache hit because it has been logged out.
     * 6. Call server2 with the LTPA cookie. This will result in a cache hit because it has been logged out.
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
         * 3. Call server2 with the LTPA cookie. This will result in a cache miss because there are no
         * cookies in the logged out cookie cache.
         */
        resetMarksInLogs(server2);
        response = formLoginClient2.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_ALL_ROLE, ltpaCookie);
        assertTrue("Did not get the expected response", formLoginClient2.verifyResponse(response, USER1_NAME, true, false));
        assertResponseContainsCustomCredentials(response);
        assertLtpaLoggedOutCookieCacheHit(false, server2, ltpaCookie);

        /*
         * 4. Logout the LTPA cookie. This will store the LTPA cookie in the distributed JCache logged out cookie cache.
         */
        resetMarksInLogs(server1);
        formLoginClient1.formLogout(LogoutOption.LOGOUT_DEFAULT_PAGE);

        /*
         * 5. Call server1 with the LTPA cookie. This will result in a cache hit because it has been logged out.
         */
        resetMarksInLogs(server1);
        assertTrue("Was not redirected to the login page.", formLoginClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_ALL_ROLE, ltpaCookie));
        assertResponseContainsCustomCredentials(response);
        assertLtpaLoggedOutCookieCacheHit(true, server1, ltpaCookie);

        /*
         * 6. Call server2 with the LTPA cookie. This will result in a cache hit because it has been logged out.
         */
        resetMarksInLogs(server2);
        assertTrue("Was not redirected to the login page.", formLoginClient2.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_ALL_ROLE, ltpaCookie));
        assertResponseContainsCustomCredentials(response);
        assertLtpaLoggedOutCookieCacheHit(true, server2, ltpaCookie);
    }
}
