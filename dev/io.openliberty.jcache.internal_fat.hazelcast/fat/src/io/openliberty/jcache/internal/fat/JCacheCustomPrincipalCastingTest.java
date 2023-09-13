/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
import static org.junit.Assert.fail;

import java.nio.file.Paths;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * Contains distributed JCache authentication cache tests that verify classloading works with
 * the object instances that are created during deserialization from the distributed JCache
 * authentication cache.
 *
 * <p/>The tests use an application that casts principals and credentials
 * stored on the subject (that has been deserialized from the distributed JCache) and the
 * expectation is that when the JCache, the application, and the login module that inserts
 * the credentials all use the same library, that there should be no issues with casting.
 */
@SkipIfSysProp("skip.tests=true")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JCacheCustomPrincipalCastingTest extends BaseTestCase {

    private static BasicAuthClient basicAuthClient1;
    private static BasicAuthClient basicAuthClient2;
    private static final String CONTEXT_ROOT = "/subjectcast";
    private static final String URL_PATTERN = "/SubjectCastServlet";

    @Server("io.openliberty.jcache.internal.fat.auth.cache.casting.1")
    public static LibertyServer server1;

    @Server("io.openliberty.jcache.internal.fat.auth.cache.casting.2")
    public static LibertyServer server2;

    private String groupName;

    private static ServerConfiguration SERVER1_ORIGINAL_CONFIG;
    private static ServerConfiguration SERVER2_ORIGINAL_CONFIG;

    @BeforeClass
    public static void beforeClass() {
        assumeShouldNotSkipTests();

        /*
         * Backup the original / starting configurations for each server.
         */
        try {
            SERVER1_ORIGINAL_CONFIG = server1.getServerConfiguration().clone();
            SERVER2_ORIGINAL_CONFIG = server2.getServerConfiguration().clone();
        } catch (Exception e) {
            fail("Failed to clone the server configurations: " + e);
        }

        /*
         * Transform apps for EE9+.
         */
        if (JakartaEEAction.isEE9OrLaterActive()) {
            JakartaEEAction.transformApp(Paths.get(server1.getServerRoot() + "/apps/subjectcast.war"));
            JakartaEEAction.transformApp(Paths.get(server2.getServerRoot() + "/apps/subjectcast.war"));
        }
    }

    @Before
    public void before() throws Exception {
        groupName = UUID.randomUUID().toString();
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
             * Stop the servers in reverse order they were started.
             */
            try {
                stopServer(server2);
            } finally {
                stopServer(server1);
            }
        }
    }

    /**
     * Test that ClassCastExceptions ARE NOT thrown when the JCache deserializing a Subject with custom credentials and principals
     * uses the same library reference as the application.
     *
     * <pre>
     * 1. Start server1.
     * 2. Start server2.
     * 3. Send a request to the subjectcast servlet on server1. This will result in a cache miss and will insert an entry
     *    into the distributed authentication cache. The subject returned contains a custom principal and custom
     *    credentials that were added by the custom login module. No ClassCastException is expected in the subjectcast
     *    servlet since the application and the login module are using the same library.
     * 4. Send a request to the subjectcast servlet on server2. This will result in a cache hit, which will load the
     *    subject from the distributed JCache authentication cache. The subject returned contains a custom principal and custom
     *    credentials that now deserialized from the distributed JCache. No ClassCastException is expected in the subjectcast
     *    servlet since the application and the JCache are using the same library.
     * </pre>
     *
     * @throws Exception if the test fails for some unforeseen reason.
     */
    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    public void custom_principal_no_classcastexception() throws Exception {

        /*
         * 1. Start server1.
         */
        server1.addInstalledAppForValidation("subjectcast");
        server1.updateServerConfiguration(SERVER1_ORIGINAL_CONFIG);
        startServer1(server1, groupName, null, null);
        basicAuthClient1 = new BasicAuthClient(server1, "Basic Authentication", "ServletName: SubjectCastServlet", CONTEXT_ROOT);
        waitForDefaultHttpsEndpoint(server1);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);

        /*
         * 2. Start server2.
         */
        server2.addInstalledAppForValidation("subjectcast");
        server2.updateServerConfiguration(SERVER2_ORIGINAL_CONFIG);
        startServer2(server2, groupName);
        basicAuthClient2 = new BasicAuthClient(server2, "Basic Authentication", "ServletName: SubjectCastServlet", CONTEXT_ROOT);
        waitForDefaultHttpsEndpoint(server2);
        waitForCreatedOrExistingJCache(server2, AUTH_CACHE_NAME);

        /*
         * 3. Send a request to the subjectcast servlet on server1. This will result in a cache miss and will insert an entry
         * into the distributed authentication cache. The subject returned contains a custom principal and custom
         * credentials that were added by the custom login module. No ClassCastException is expected in the subjectcast
         * servlet since the application and the login module are using the same library.
         */
        String response = basicAuthClient1.accessProtectedServletWithAuthorizedCredentials(URL_PATTERN, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient1.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheBasicAuthCacheHit(false, server1);
        assertResponseContainsClassCastException(response, false);

        /*
         * 4. Send a request to the subjectcast servlet on server2. This will result in a cache hit, which will load the
         * subject from the distributed JCache authentication cache. The subject returned contains a custom principal and custom
         * credentials that now deserialized from the distributed JCache. No ClassCastException is expected in the subjectcast
         * servlet since the application and the JCache are using the same library.
         */
        response = basicAuthClient2.accessProtectedServletWithAuthorizedCookie(URL_PATTERN, basicAuthClient1.getCookieFromLastLogin());
        assertTrue("Did not get the expected response", basicAuthClient2.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheLtpaAuthCacheHit(true, server2, basicAuthClient1.getCookieFromLastLogin());
        assertResponseContainsClassCastException(response, false);
    }

    /**
     * Test that ClassCastExceptions ARE thrown when the JCache deserializing a Subject with custom credentials and principals
     * does NOT use the same library reference as the application.
     * <p/>
     * NOTE: This is just to test expected behavior, in comparison to the {@link #custom_principal_no_classcastexception()} test.
     *
     * <pre>
     * 1. Start the server1 replacing the applications library with one that does not match
     *    either the library used by custom login module or the authentication cache's JCache.
     * 2. Start the server2 replacing the applications library with one that does not match
     *    either the library used by custom login module or the authentication cache's JCache.
     * 3. Send a request to the subjectcast servlet on server1. This will result in a cache miss and will insert an entry
     *    into the distributed authentication cache. The subject returned contains a custom principal and custom
     *    credentials that were added by the custom login module. A ClassCastException is expected in the subjectcast
     *    servlet since the application and the login module are using a different library.
     * 4. Send a 2nd request to the subjectcast servlet on server1. This will result in a cache hit, which will load the
     *    subject from the distributed JCache authentication cache. The subject returned contains a custom principal and custom
     *    credentials that now deserialized from the distributed JCache. A ClassCastException is expected in the subjectcast
     *    servlet since the application and the JCache are using the different libraries.
     * 5. Send a request to the subjectcast servlet on server2. This will result in a cache hit, which will load the
     *    subject from the distributed JCache authentication cache. The subject returned contains a custom principal and custom
     *    credentials that now deserialized from the distributed JCache. A ClassCastException is expected in the subjectcast
     *    servlet since the application and the JCache are using the different libraries.
     * </pre>
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    public void custom_principal_expect_classcastexception() throws Exception {

        /*
         * 1. Start the server1 replacing the applications library with one that does not match
         * either the library used by custom login module or the authentication cache's JCache.
         */
        ServerConfiguration serverConfig = SERVER1_ORIGINAL_CONFIG.clone();
        Application app = getApplication(serverConfig, "subjectcast");
        app.getClassloaders().get(0).getCommonLibraryRefs().clear();
        app.getClassloaders().get(0).getCommonLibraryRefs().add("ClassCastingAppLib");
        server1.updateServerConfiguration(serverConfig);
        server1.addInstalledAppForValidation("subjectcast");
        startServer1(server1, groupName, null, null);
        basicAuthClient1 = new BasicAuthClient(server1, "Basic Authentication", "ServletName: SubjectCastServlet", CONTEXT_ROOT);
        waitForDefaultHttpsEndpoint(server1);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);

        /*
         * 2. Start the server2 replacing the applications library with one that does not match
         * either the library used by custom login module or the authentication cache's JCache.
         */
        serverConfig = SERVER2_ORIGINAL_CONFIG.clone();
        app = getApplication(serverConfig, "subjectcast");
        app.getClassloaders().get(0).getCommonLibraryRefs().clear();
        app.getClassloaders().get(0).getCommonLibraryRefs().add("ClassCastingAppLib");
        server2.updateServerConfiguration(serverConfig);
        server2.addInstalledAppForValidation("subjectcast");
        startServer2(server2, groupName);
        basicAuthClient2 = new BasicAuthClient(server2, "Basic Authentication", "ServletName: SubjectCastServlet", CONTEXT_ROOT);
        waitForDefaultHttpsEndpoint(server2);
        waitForCreatedOrExistingJCache(server2, AUTH_CACHE_NAME);

        /*
         * 3. Send a request to the subjectcast servlet on server1. This will result in a cache miss and will insert an entry
         * into the distributed authentication cache. The subject returned contains a custom principal and custom
         * credentials that were added by the custom login module. A ClassCastException is expected in the subjectcast
         * servlet since the application and the login module are using a different library.
         */
        String response = basicAuthClient1.accessProtectedServletWithAuthorizedCredentials(URL_PATTERN, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", basicAuthClient1.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheBasicAuthCacheHit(false, server1);
        assertResponseContainsClassCastException(response, true);

        /*
         * 4. Send a 2nd request to the subjectcast servlet on server1. This will result in a cache hit, which will load the
         * subject from the distributed JCache authentication cache. The subject returned contains a custom principal and custom
         * credentials that now deserialized from the distributed JCache. A ClassCastException is expected in the subjectcast
         * servlet since the application and the JCache are using the different libraries.
         */
        response = basicAuthClient1.accessProtectedServletWithAuthorizedCookie(URL_PATTERN, basicAuthClient1.getCookieFromLastLogin());
        assertTrue("Did not get the expected response", basicAuthClient1.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheLtpaAuthCacheHit(true, server1, basicAuthClient1.getCookieFromLastLogin());
        assertResponseContainsClassCastException(response, true);

        /*
         * 5. Send a request to the subjectcast servlet on server2. This will result in a cache hit, which will load the
         * subject from the distributed JCache authentication cache. The subject returned contains a custom principal and custom
         * credentials that now deserialized from the distributed JCache. A ClassCastException is expected in the subjectcast
         * servlet since the application and the JCache are using the different libraries.
         */
        response = basicAuthClient2.accessProtectedServletWithAuthorizedCookie(URL_PATTERN, basicAuthClient1.getCookieFromLastLogin());
        assertTrue("Did not get the expected response", basicAuthClient2.verifyResponse(response, USER1_NAME, false, false));
        assertResponseContainsCustomCredentials(response);
        assertJCacheLtpaAuthCacheHit(true, server2, basicAuthClient1.getCookieFromLastLogin());
        assertResponseContainsClassCastException(response, true);
    }

    /**
     * Assert that the SubjectCastServlet application outputs the expected ClassCastException messages.
     *
     * @param response        The response from the servlet.
     * @param expectException Whether to expect the ClassCastException in the response.
     */
    private static void assertResponseContainsClassCastException(String response, boolean expectException) {
        if (expectException) {
            assertTrue("Did not find ClassCastException for CustomPrincipal in response.", response.contains("Error casting CustomPrincipal: CustomPrincipal"));
            assertTrue("Did not find ClassCastException for CustomPrivateCredential in response.",
                       response.contains("Error casting CustomPrivateCredential: CustomPrivateCredential"));
            assertTrue("Did not find ClassCastException for CustomPublicCredential in response.",
                       response.contains("Error casting CustomPublicCredential: CustomPublicCredential"));
        } else {
            assertTrue("Did not find successful class cast for CustomPrincipal in response.", response.contains("Successfully cast CustomPrincipal"));
            assertTrue("Did not find successful class cast for CustomPrincipal in response.", response.contains("Successfully cast CustomPrivateCredential"));
            assertTrue("Did not find successful class cast for CustomPrincipal in response.", response.contains("Successfully cast CustomPublicCredential"));
        }
    }

    /**
     * Get the named {@link Application} from the server configuration.
     *
     * @param serverConfig The {@link ServerConfiguration} to get the application configuration from.
     * @param name         The name of the application.
     * @return The application configuration
     */
    private static Application getApplication(ServerConfiguration serverConfig, String name) {
        for (Application app : serverConfig.getApplications()) {
            if (name.equals(app.getName())) {
                return app;
            }
        }
        fail("Did not find application " + name + " in server configuration.");
        return null;
    }
}
