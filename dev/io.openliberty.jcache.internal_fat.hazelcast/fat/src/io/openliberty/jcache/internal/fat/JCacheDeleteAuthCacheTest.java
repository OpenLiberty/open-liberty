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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.ServletClient;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * Contains distributed JCache authentication cache tests for the DeleteAuthCache Mbean.
 */
@SkipIfSysProp("skip.tests=true")
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class JCacheDeleteAuthCacheTest extends BaseTestCase {

    private static BasicAuthClient basicAuthClient1;
    private static BasicAuthClient basicAuthClient2;
    private static final String ENDPOINT_DELETE_AUTH_CACHE_MBEAN = "/IBMJMXConnectorREST/mbeans/WebSphere:service=com.ibm.websphere.security.authentication.cache.DeleteAuthCache/operations/removeAllEntries";

    @Server("io.openliberty.jcache.internal.fat.delete.auth.cache.1")
    public static LibertyServer server1;

    @Server("io.openliberty.jcache.internal.fat.delete.auth.cache.2")
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
         * Start server1.
         */
        server1.addInstalledAppForValidation("basicauth");
        startServer1(server1, groupName, null, null);
        basicAuthClient1 = new BasicAuthClient(server1);
        waitForDefaultHttpsEndpoint(server1);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);

        /*
         * Start server2.
         */
        server2.addInstalledAppForValidation("basicauth");
        startServer2(server2, groupName);
        basicAuthClient2 = new BasicAuthClient(server2);
        waitForDefaultHttpsEndpoint(server2);
        waitForCreatedOrExistingJCache(server2, AUTH_CACHE_NAME);
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
     * This test will test that the DeleteAuthCache MBean clears the authentication cache when using JCache.
     *
     * <pre>
     * 1. Call server1 using basic authentication. This will result in a cache miss and will insert an entry into the cache.
     * 2. Call server1 using basic authentication. This will result in an authentication cache hit.
     * 3. Call server2 using basic authentication. This will result in an authentication cache hit,
     *    since it shares a distributed cache with server1.
     * 4. Call the DeleteAuthCache MBean on server1. This will clear the cache and output an
     *    informational message notifying that the authentication cache's JCache has been cleared.
     * 5. Call server1 using basic authentication. This will result in a cache miss and will insert an entry into the cache.
     * 6. Call the DeleteAuthCache MBean on server2. This will clear the cache and output an
     *    informational message notifying that the authentication cache's JCache has been cleared.
     * 7. Call server2 using basic authentication. This will result in a cache miss and will insert an entry into the cache.
     * </pre>
     *
     * @throws Exception if the test fails for some unforeseen reason.
     */
    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    public void deleteAuthCache_mbean() throws Exception {
        /*
         * 1. Call server1 using basic authentication. This will result in a cache miss and will insert an entry into the cache.
         */
        String response = basicAuthClient1.accessProtectedServletWithAuthorizedCredentials(ServletClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient1.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheBasicAuthCacheHit(false, server1);

        /*
         * 2. Call server1 using basic authentication. This will result in an authentication cache hit.
         */
        basicAuthClient1.resetClientState();
        response = basicAuthClient1.accessProtectedServletWithAuthorizedCredentials(ServletClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient1.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheBasicAuthCacheHit(true, server1);

        /*
         * 3. Call server2 using basic authentication. This will result in an authentication cache hit,
         * since it shares a distributed cache with server1.
         */
        response = basicAuthClient2.accessProtectedServletWithAuthorizedCredentials(ServletClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient2.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheBasicAuthCacheHit(true, server2);

        /*
         * 4. Call the DeleteAuthCache MBean on server1. This will clear the cache and output an
         * informational message notifying that the authentication cache's JCache has been cleared.
         */
        String content = HttpUtils.performPost(server1, ENDPOINT_DELETE_AUTH_CACHE_MBEAN, 200, "application/json", USER1_NAME, USER1_PASSWORD, "application/json",
                                               "{}");
        assertEquals("The response content of the DeleteAuthCache MBean REST request was not the expected value.", "{\"value\":null,\"type\":null}", content);
        assertAuthCacheJCacheCleared(server1);

        /*
         * Hazelcast seems to need time to allow to clear remotely. Seems this shouldn't be the case.
         */
        Log.info(getClass(), "deleteAuthCache_mbean", "SLEEPING...");
        Thread.sleep(10000);

        /*
         * 5. Call server2 using basic authentication. This will result in a cache miss and will insert an entry into the cache.
         */
        basicAuthClient2.resetClientState();
        resetMarksInLogs(server2);
        response = basicAuthClient2.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient2.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheBasicAuthCacheHit(false, server2);

        /*
         * 6. Call the DeleteAuthCache MBean on server2. This will clear the cache and output an
         * informational message notifying that the authentication cache's JCache has been cleared.
         */
        content = HttpUtils.performPost(server2, ENDPOINT_DELETE_AUTH_CACHE_MBEAN, 200, "application/json", USER1_NAME, USER1_PASSWORD, "application/json",
                                        "{}");
        assertEquals("The response content of the DeleteAuthCache MBean REST request was not the expected value.", "{\"value\":null,\"type\":null}", content);
        assertAuthCacheJCacheCleared(server2);

        /*
         * Hazelcast seems to need time to allow to clear remotely. Seems this shouldn't be the case.
         */
        Log.info(getClass(), "deleteAuthCache_mbean", "SLEEPING...");
        Thread.sleep(10000);

        /*
         * 7. Call server1 using basic authentication. This will result in a cache miss and will insert an entry into the cache.
         */
        basicAuthClient1.resetClientState();
        resetMarksInLogs(server1);
        response = basicAuthClient1.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient1.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheBasicAuthCacheHit(false, server1);
    }

}
