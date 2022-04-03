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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.ws.webcontainer.security.internal.LoggedOutJwtSsoCookieCache;

import componenttest.topology.impl.LibertyServer;
import io.openliberty.jcache.internal.fat.plugins.TestPluginHelper;

/**
 * Abstract test class that can extended from concrete test classes to user common testing functionality.
 */
public abstract class BaseTestCase {

    protected static final String USER1_NAME = "user1";
    protected static final String USER1_PASSWORD = "user1Password";
    protected static final String USER1_PASSWORD_HASH = "hQM3o73+TAyCGqHR7no3DJLDgBUTLpyH7mWR0e7bHhqIqlfbL1gMLxtRP+nfJ0XZUnriq4NGpAgW4WGdnq92cw==";

    protected static final String JCACHE_HIT = "JCache HIT for key ";
    protected static final String JCACHE_MISS = "JCache MISS for key ";
    protected static final String JCACHE_HIT_USER1_BASICAUTH = JCACHE_HIT + "BasicRealm:user1:" + encodeForRegex(USER1_PASSWORD_HASH);
    protected static final String JCACHE_MISS_USER1_BASICAUTH = JCACHE_MISS + "BasicRealm:user1:" + encodeForRegex(USER1_PASSWORD_HASH);
    protected static final String JCACHE_CLEAR_ALL_ENTRIES = "CWWKS1125I";

    protected static final String IN_MEMORY_HIT = "In-memory cache HIT for key ";
    protected static final String IN_MEMORY_MISS = "In-memory cache MISS for key ";
    protected static final String IN_MEMORY_HIT_USER1_BASICAUTH = IN_MEMORY_HIT + "BasicRealm:user1:" + encodeForRegex(USER1_PASSWORD_HASH);
    protected static final String IN_MEMORY_MISS_USER1_BASICAUTH = IN_MEMORY_MISS + "BasicRealm:user1:" + encodeForRegex(USER1_PASSWORD_HASH);

    protected static final String AUTH_CACHE_NAME = "AuthCache";
    protected static final String COOKIE_CACHE_NAME = "LoggedOutCookieCache";

    @Rule
    public final TestName testName = new TestName();

    @Before
    public void beforeTest() throws Exception {
        TestPluginHelper.getTestPlugin().beforeTest();

        Log.info(getClass(), "beforeTest", ">>>>>>>>>>>>>>>>>>> ENTERING TEST " + getClass().getName() + "." + testName.getMethodName());
    }

    @After
    public void afterTest() throws Exception {
        Log.info(getClass(), "afterTest", "<<<<<<<<<<<<<<<<<<< EXITING TEST " + getClass().getName() + "." + testName.getMethodName());
    }

    /**
     * Start server 1.
     *
     * @param server           The server to start.
     * @param groupName        The group name (Hazelcast).
     * @param authCacheMaxSize The maximum size of the 'AuthCache' cache. 25000 if null.
     * @param authCacheTtl     The time to live for the 'AuthCache' cache. 600s if null.
     * @throws Exception If starting the server failed for some unforeseen reason.
     */
    protected static void startServer1(LibertyServer server, String groupName, Integer authCacheMaxSize, Integer authCacheTtl) throws Exception {

        /*
         * Go to the specific provider's setup.
         */
        TestPluginHelper.getTestPlugin().setupServer1(server, groupName, authCacheMaxSize, authCacheTtl);

        /*
         * Start the server.
         */
        server.startServer();

        /*
         * Wait for each server to finish startup.
         */
        assertNotNull("Security service did not come up",
                      server.waitForStringInLog("CWWKS0008I")); // CWWKS0008I: The security service is ready.
        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I")); // CWWKF0008I: Feature update completed
        assertNotNull("Server did not came up",
                      server.waitForStringInLog("CWWKF0011I")); // CWWKF0011I: The server is ready to run a smarter planet.

    }

    /**
     * Start server 2.
     *
     * @param server    The server to start.
     * @param groupName The group name (Hazelcast).
     * @throws Exception If starting the server failed for some unforeseen reason.
     */
    protected static void startServer2(LibertyServer server, String groupName) throws Exception {

        /*
         * Go to the specific provider's setup.
         */
        TestPluginHelper.getTestPlugin().setupServer2(server, groupName);

        /*
         * Setup HTTP(S) ports.
         */
        server.useSecondaryHTTPPort();

        /*
         * Start the server.
         */
        server.startServer();

        /*
         * Wait for each server to finish startup.
         */
        assertNotNull("Security service did not come up",
                      server.waitForStringInLog("CWWKS0008I")); // CWWKS0008I: The security service is ready.
        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I")); // CWWKF0008I: Feature update completed
        assertNotNull("Server did not came up",
                      server.waitForStringInLog("CWWKF0011I")); // CWWKF0011I: The server is ready to run a smarter planet.
    }

    /**
     * Stop the server with the expected exceptions.
     *
     * @param server             The server to stop.
     * @param expectedExceptions The expected exceptions.
     * @throws Exception if there was an error stopping the server.
     */
    protected static void stopServer(LibertyServer server, String... expectedExceptions) throws Exception {
        if (server != null && server.isStarted()) {
            try {
                server.stopServer(expectedExceptions);
                server = null;
            } catch (Exception e) {
                Log.error(BaseTestCase.class, "stopServer", e, "Encountered error stopping server " + server.getServerName());
                throw e;
            }
        }
    }

    /**
     * Reset the marks in all Liberty logs.
     *
     * @param server The server for the logs to reset the marks.
     * @throws Exception If there was an error resetting the marks.
     */
    public static void resetMarksInLogs(LibertyServer server) throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setMarkToEndOfLog(server.getMostRecentTraceFile());
    }

    /**
     * Assert whether the response contains the subject with the custom principal and credentials.
     *
     * @param response The response to check.
     */
    protected static void assertResponseContainsCustomCredentials(String response) {
        assertTrue("Did not find the custom principal in the subject.", response.contains("Principal: CustomPrincipal"));
        assertTrue("Did not find the custom public credential in the subject.", response.contains("Public Credential: CustomPublicCredential"));
        assertTrue("Did not find the custom private credential in the subject.", response.contains("Private Credential: CustomPrivateCredential"));
    }

    /**
     * Assert whether there was or was not a basic auth (user/password) cache hit to the JCache.
     *
     * @param expectCacheHit Whether to expect there was a cache hit.
     * @param server         The server to check.
     * @throws Exception If the check failed for some unforeseen reason.
     */
    protected static void assertJCacheBasicAuthCacheHit(boolean expectCacheHit, LibertyServer server) throws Exception {
        if (expectCacheHit) {
            assertFalse("Request should have resulted in an JCache auth cache hit for: " + JCACHE_HIT_USER1_BASICAUTH,
                        server.findStringsInLogsAndTraceUsingMark(JCACHE_HIT_USER1_BASICAUTH).isEmpty());
        } else {
            assertFalse("Request should have resulted in an JCache auth cache miss for: " + JCACHE_MISS_USER1_BASICAUTH,
                        server.findStringsInLogsAndTraceUsingMark(JCACHE_MISS_USER1_BASICAUTH).isEmpty());
        }
    }

    /**
     * Assert whether there was or was not a basic auth (user/password) cache hit to the in-memory cache.
     *
     * @param expectCacheHit Whether to expect there was a cache hit.
     * @param server         The server to check.
     * @throws Exception If the check failed for some unforeseen reason.
     */
    protected static void assertInMemoryBasicAuthCacheHit(boolean expectCacheHit, LibertyServer server) throws Exception {
        if (expectCacheHit) {
            assertFalse("Request should have resulted in an in-memory auth cache hit for: " + IN_MEMORY_HIT_USER1_BASICAUTH,
                        server.findStringsInLogsAndTraceUsingMark(IN_MEMORY_HIT_USER1_BASICAUTH).isEmpty());
        } else {
            assertFalse("Request should have resulted in an in-memory auth cache miss for: " + IN_MEMORY_MISS_USER1_BASICAUTH,
                        server.findStringsInLogsAndTraceUsingMark(IN_MEMORY_MISS_USER1_BASICAUTH).isEmpty());
        }
    }

    /**
     * Assert whether there was or was not an LTPA authentication cache hit to the JCache.
     *
     * @param expectCacheHit Whether to expect there was a cache hit.
     * @param server         The server to check the logs for the cache hit.
     * @param cookie         The LTPA cookie to check for.
     * @throws Exception If the check failed for some unforeseen reason.
     */
    protected static void assertLtpaAuthCacheHit(boolean expectCacheHit, LibertyServer server, String cookie) throws Exception {
        /*
         * Make the cookie regex friendly.
         */
        String encodedCookie = encodeForRegex(cookie);

        if (expectCacheHit) {
            assertFalse("Request should have resulted in an auth cache hit for LTPA cookie " + cookie + ".",
                        server.findStringsInLogsAndTraceUsingMark(JCACHE_HIT + encodedCookie).isEmpty());
        } else {
            assertFalse("Request should have resulted in an auth cache miss for LTPA cookie " + cookie + ".",
                        server.findStringsInLogsAndTraceUsingMark(JCACHE_MISS + encodedCookie).isEmpty());
        }
    }

    /**
     * Assert whether there was or was not a JWT authentication cache hit to the JCache.
     *
     * @param expectCacheHit Whether to expect there was a cache hit.
     * @param server         The server to check the logs for the cache hit.
     * @param cookie         The JWT cookie to check for.
     * @throws Exception If the check failed for some unforeseen reason.
     */
    protected static void assertJCacheJwtAuthCacheHit(boolean expectCacheHit, LibertyServer server, String cookie) throws Exception {
        String hashedCookie = HashUtils.digest(cookie);
        String encodedCookie = encodeForRegex(hashedCookie);

        if (expectCacheHit) {
            assertFalse("Request should have resulted in an auth cache hit for JWT (hashed) cookie " + hashedCookie + ".",
                        server.findStringsInLogsAndTraceUsingMark(JCACHE_HIT + encodedCookie).isEmpty());
        } else {
            assertFalse("Request should have resulted in an auth cache miss for JWT (hashed) cookie " + hashedCookie + ".",
                        server.findStringsInLogsAndTraceUsingMark(JCACHE_MISS + encodedCookie).isEmpty());
        }
    }

    /**
     * Assert whether there was or was not a SAML authentication cache hit to the JCache.
     *
     * @param expectCacheHit Whether to expect there was a cache hit.
     * @param server         The server to check the logs for the cache hit.
     * @param key            The key to check for.
     * @throws Exception If the check failed for some unforeseen reason.
     */
    protected static void assertJCacheSamlAuthCacheHit(boolean expectCacheHit, LibertyServer server, String key) throws Exception {
        String encodedCookie = encodeForRegex(key);

        if (expectCacheHit) {
            assertFalse("Request should have resulted in a JCache auth cache hit for SAML cache key " + key + ".",
                        server.findStringsInLogsAndTraceUsingMark(JCACHE_HIT + encodedCookie).isEmpty());
        } else {
            assertFalse("Request should have resulted in a JCache auth cache miss for SAML cache key " + key + ".",
                        server.findStringsInLogsAndTraceUsingMark(JCACHE_MISS + encodedCookie).isEmpty());
        }
    }

    /**
     * Assert whether there was or was not a SAML authentication cache hit to the in-memory cache.
     *
     * @param expectCacheHit Whether to expect there was a cache hit.
     * @param server         The server to check the logs for the cache hit.
     * @param key            The key to check for.
     * @throws Exception If the check failed for some unforeseen reason.
     */
    protected static void assertInMemorySamlAuthCacheHit(boolean expectCacheHit, LibertyServer server, String key) throws Exception {
        String encodedKey = encodeForRegex(key);

        if (expectCacheHit) {
            assertFalse("Request should have resulted in an in-memory auth cache hit for SAML cache key " + key + ".",
                        server.findStringsInLogsAndTraceUsingMark(IN_MEMORY_HIT + encodedKey).isEmpty());
        } else {
            assertFalse("Request should have resulted in an in-memory auth cache miss for SAML cache key " + key + ".",
                        server.findStringsInLogsAndTraceUsingMark(IN_MEMORY_MISS + encodedKey).isEmpty());
        }
    }

    /**
     * Assert whether there was or was not a logged out cookie cache hit for the specified LTPA cookie.
     *
     * @param expectCacheHit Whether to expect there was a cache hit.
     * @param server         The server to check the logs for the cache hit.
     * @param cookie         The LTPA cookie to check for.
     * @throws Exception If the check failed for some unforeseen reason.
     */
    protected static void assertLtpaLoggedOutCookieCacheHit(boolean expectCacheHit, LibertyServer server, String cookie) throws Exception {
        String encodedCookie = encodeForRegex(cookie);

        if (expectCacheHit) {
            assertFalse("Request should have resulted in an logged out cookie cache hit for LTPA (hashed) cookie " + cookie + ".",
                        server.findStringsInLogsAndTraceUsingMark(JCACHE_HIT + encodedCookie).isEmpty());
        } else {
            assertFalse("Request should have resulted in an logged out cookie cache miss for LTPA (hashed) cookie " + cookie + ".",
                        server.findStringsInLogsAndTraceUsingMark(JCACHE_MISS + encodedCookie).isEmpty());
        }
    }

    /**
     * Assert whether there was or was not a logged out cookie cache hit for the specified JWT cookie.
     *
     * @param expectCacheHit Whether to expect there was a cache hit.
     * @param server         The server to check the logs for the cache hit.
     * @param cookie         The JWT cookie to check for.
     * @throws Exception If the check failed for some unforeseen reason.
     */
    protected static void assertJwtLoggedOutCookieCacheHit(boolean expectCacheHit, LibertyServer server, String cookie) throws Exception {
        String hashedCookie = LoggedOutJwtSsoCookieCache.toDigest(cookie);
        String encodedCookie = encodeForRegex(hashedCookie);

        if (expectCacheHit) {
            assertFalse("Request should have resulted in an logged out cookie cache hit for JWT (hashed) cookie " + hashedCookie + ".",
                        server.findStringsInLogsAndTraceUsingMark(JCACHE_HIT + encodedCookie).isEmpty());
        } else {
            assertFalse("Request should have resulted in an logged out cookie cache miss for JWT (hashed) cookie " + hashedCookie + ".",
                        server.findStringsInLogsAndTraceUsingMark(JCACHE_MISS + encodedCookie).isEmpty());
        }
    }

    /**
     * Assert the JCache authentication cache logged that it was NOT cleared.
     *
     * @param server The server to check the logs.
     * @throws Exception If the check failed for some unforeseen reason.
     */
    protected static void assertAuthCacheJCacheNotCleared(LibertyServer server) throws Exception {
        assertTrue("Should not have found any " + JCACHE_CLEAR_ALL_ENTRIES + " messages in the log, indicating that the authentication cache cleared the JCache entries .",
                   server.findStringsInLogs(JCACHE_CLEAR_ALL_ENTRIES).isEmpty());
    }

    /**
     * Assert the JCache authentication cache logged that it was cleared.
     *
     * @param server The server to check the logs.
     * @throws Exception If the check failed for some unforeseen reason.
     */
    protected static void assertAuthCacheJCacheCleared(LibertyServer server) throws Exception {
        assertFalse("Should have found a " + JCACHE_CLEAR_ALL_ENTRIES + " message in the log, indicating that the authentication cache cleared the JCache entries .",
                    server.findStringsInLogs(JCACHE_CLEAR_ALL_ENTRIES).isEmpty());
    }

    /**
     * Encode a string to include in a regular expression.
     *
     * @param string String to encode.
     * @return The encoded string.
     */
    private static String encodeForRegex(String string) {
        /*
         * Make the cookie regex friendly.
         */
        string = string.replaceAll("\\\\", "\\\\\\"); // MUST BE FIRST
        string = string.replaceAll("\\[", "\\\\[");
        string = string.replaceAll("\\{", "\\\\{");
        string = string.replaceAll("\\^", "\\\\^");
        string = string.replaceAll("\\$", "\\\\$");
        string = string.replaceAll("\\.", "\\\\.");
        string = string.replaceAll("\\|", "\\\\|");
        string = string.replaceAll("\\?", "\\\\?");
        string = string.replaceAll("\\*", "\\\\*");
        string = string.replaceAll("\\+", "\\\\+");
        string = string.replaceAll("\\(", "\\\\(");
        string = string.replaceAll("\\)", "\\\\)");
        return string;
    }

    /**
     * Wait for a messaging indicating an existing JCache cache has been found.
     *
     * @param server    The server to check the logs for the message.
     * @param cacheName The name of the cache to check for the message.
     */
    protected static void waitForExistingJCache(LibertyServer server, String cacheName) {
        assertNotNull("Expected message indicating existing cache " + cacheName + " was found.", server.waitForStringInLog("CWLJC0002I:.*" + cacheName + ".*was found", 60000));
    }

    /**
     * Wait for a message indicating a JCache cache was created.
     *
     * @param server    The server to check the logs for the message.
     * @param cacheName The name of the cache to check for the message.
     */
    protected static void waitForCreatedJCache(LibertyServer server, String cacheName) {
        assertNotNull("Expected message indicating cache " + cacheName + " was created.", server.waitForStringInLog("CWLJC0001I:.*" + cacheName + ".*was created", 60000));
    }

    /**
     * Wait for a message indicating the JCache provider that was used for the named cache.
     *
     * @param server    The server to check the logs for the message.
     * @param cacheName The name of the cache to check for the message.
     */
    protected static void waitForCachingProvider(LibertyServer server, String cacheName) {
        assertNotNull("Expected caching provider would be loaded for " + cacheName + ".",
                      server.waitForStringInLog("CWLJC0003I:.*" + cacheName + ".*" + TestPluginHelper.getTestPlugin().getCachingProviderName()));
    }
}
