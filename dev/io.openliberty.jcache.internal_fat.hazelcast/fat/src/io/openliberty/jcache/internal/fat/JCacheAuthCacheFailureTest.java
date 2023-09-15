/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.ServletClient;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * Test failover to the local in-memory authentication cache when there are failures with retrieving
 * objects from the JCache.
 */
@SkipIfSysProp("skip.tests=true")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JCacheAuthCacheFailureTest extends BaseTestCase {
    private static BasicAuthClient basicAuthClient1;
    private static BasicAuthClient basicAuthClient2;

    @Server("io.openliberty.jcache.internal.fat.auth.cache.failure.1")
    public static LibertyServer server1;

    @Server("io.openliberty.jcache.internal.fat.auth.cache.failure.2")
    public static LibertyServer server2;

    @BeforeClass
    public static void beforeClass() {
        assumeShouldNotSkipTests();

        /*
         * Transform apps for EE9+.
         */
        if (JakartaEEAction.isEE9OrLaterActive()) {
            JakartaEEAction.transformApp(Paths.get(server1.getServerRoot() + "/apps/basicauth.war"));
            JakartaEEAction.transformApp(Paths.get(server2.getServerRoot() + "/apps/basicauth.war"));
        }
    }

    @Before
    public void before() throws Exception {
        String groupName = UUID.randomUUID().toString();

        /*
         * Start server 1.
         */
        server1.addInstalledAppForValidation("basicauth");
        startServer1(server1, groupName, 25000, 600);
        basicAuthClient1 = new BasicAuthClient(server1);
        waitForDefaultHttpsEndpoint(server1);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);

        /*
         * Start server 2.
         */
        server2.addInstalledAppForValidation("basicauth");
        startServer2(server2, groupName);
        basicAuthClient2 = new BasicAuthClient(server2);
        waitForDefaultHttpsEndpoint(server2);
        waitForCreatedOrExistingJCache(server2, AUTH_CACHE_NAME);
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
     * Assert whether the response contains the subject with the NonSerializablePrincipal.
     *
     * @param response The response to check.
     */
    protected static void assertResponseContainsNonSerializablePrincipal(String response) {
        assertTrue("Did not find the custom principal in the subject.", response.contains("Principal: NonSerializablePrincipal"));
    }

    /**
     * Assert whether the logs / trace contain an indication of a SerializationException.
     *
     * @param server The server to check.
     * @throws Exception If the check failed for some unforeseen reason.
     */
    protected static void assertSerializationError(LibertyServer server) throws Exception {
        /*
         * Currently this is just in debug / trace.
         */
        assertFalse("Request should have resulted in an error: " + JCACHE_HIT_USER1_BASICAUTH,
                    server.findStringsInLogsAndTraceUsingMark("CWLJC0009E: Error serializing object to the").isEmpty());
    }

    /**
     * Test failure to serialize objects and put them in the JCache. In this instance, the objects should be
     * placed in the in-memory authentication cache.
     *
     * <pre>
     * 1. Call server1 using basic authentication. This will result in a cache miss for both the JCache and remote caches.
     *    Since the NonSerializablePrincipal inserted into the Subject by the LoginModule is not Serializable it will fail to
     *    be inserted into the JCache and be inserted into the in-memory cache instead.
     * 2. Call server1 again using basic authentication. This will result in a JCache authentication cache miss,
     *    but an in-memory authentication cache hit.
     * 3. Call server2 using basic authentication. This will result in a cache miss for both the JCache and remote caches.
     *    Since the NonSerializablePrincipal inserted into the Subject by the LoginModule is not Serializable it will fail to
     *    be inserted into the JCache and be inserted into the in-memory cache instead.
     * 4. Call server2 again using basic authentication. This will result in a JCache authentication cache miss,
     *    but an in-memory authentication cache hit.
     * </pre>
     *
     * @throws Exception If the test failed failed for some unforeseen reason.
     */
    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    @ExpectedFFDC({ "java.io.NotSerializableException" })
    public void serializationError_FailTo_InMemory() throws Exception {

        /*
         * We expect serialization error messages.
         */
        server1.addIgnoredErrors(Arrays.asList("CWLJC0009E"));
        server2.addIgnoredErrors(Arrays.asList("CWLJC0009E"));

        /*
         * 1. Call server1 using basic authentication. This will result in a cache miss for both the JCache and remote caches.
         * Since the NonSerializablePrincipal inserted into the Subject by the LoginModule is not Serializable it will fail to
         * be inserted into the JCache and be inserted into the in-memory cache instead.
         */
        String response = basicAuthClient1.accessProtectedServletWithAuthorizedCredentials(ServletClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient1.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsNonSerializablePrincipal(response);
        assertJCacheBasicAuthCacheHit(false, server1);
        assertInMemoryBasicAuthCacheHit(false, server1);
        assertSerializationError(server1);

        /*
         * 2. Call server1 again using basic authentication. This will result in a JCache authentication cache miss,
         * but an in-memory authentication cache hit.
         */
        basicAuthClient1.resetClientState();
        response = basicAuthClient1.accessProtectedServletWithAuthorizedCredentials(ServletClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient1.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsNonSerializablePrincipal(response);
        assertJCacheBasicAuthCacheHit(false, server1);
        assertInMemoryBasicAuthCacheHit(true, server1);
        assertSerializationError(server1);

        /*
         * 3. Call server2 using basic authentication. This will result in a cache miss for both the JCache and remote caches.
         * Since the NonSerializablePrincipal inserted into the Subject by the LoginModule is not Serializable it will fail to
         * be inserted into the JCache and be inserted into the in-memory cache instead.
         */
        response = basicAuthClient2.accessProtectedServletWithAuthorizedCredentials(ServletClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient2.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsNonSerializablePrincipal(response);
        assertJCacheBasicAuthCacheHit(false, server2);
        assertInMemoryBasicAuthCacheHit(false, server2);
        assertSerializationError(server2);

        /*
         * 4. Call server1 again using basic authentication. This will result in a JCache authentication cache miss,
         * but an in-memory authentication cache hit.
         */
        basicAuthClient2.resetClientState();
        response = basicAuthClient2.accessProtectedServletWithAuthorizedCredentials(ServletClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient2.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsNonSerializablePrincipal(response);
        assertJCacheBasicAuthCacheHit(false, server2);
        assertInMemoryBasicAuthCacheHit(true, server2);
        assertSerializationError(server2);
    }
}
