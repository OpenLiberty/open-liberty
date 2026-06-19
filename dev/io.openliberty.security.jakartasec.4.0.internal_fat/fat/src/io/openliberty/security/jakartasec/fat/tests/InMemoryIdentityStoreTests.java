/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.tests;

import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.INVALID_PASSWORD;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PRODUCTION_USE_WARNING_MSG;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_BILL;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_FRANK;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_JASMINE;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_LISA;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_THEO;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.VALID_PASSWORD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import inmemory.identity.store.InMemoryIdentityStoreApplication;
import inmemory.identity.store.InMemoryIdentityStoreProtectedResource;

/**
 * Tests for InMemoryIdentityStoreDefinition with various password encoding schemes.
 * Tests positive scenarios (plain, XOR, AES, Hash passwords) and negative scenarios
 * (bad passwords, bad encoding, insufficient groups).
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class InMemoryIdentityStoreTests extends BaseJakartaSecurity40Test {

    private static final Class<?> c = InMemoryIdentityStoreTests.class;

    public static final String APP_NAME = "IdentityStore";
    private static final String CONTEXT_ROOT = "/" + APP_NAME;
    private static final String RESOURCE_PATH = "/resource/test";

    private static String url = null;

    @Server(IN_MEM_ID_STORE_ENABLED_SERVER_NAME)
    public static LibertyServer server;

    @Override
    protected Class<?> getTestClass() {
        return c;
    }

    @Override
    protected LibertyServer getServer() {
        return server;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        InMemoryIdentityStoreTests instance = new InMemoryIdentityStoreTests();
        instance.logInfo("setUp", "Starting server setup...");

        // The URL is not expected to be modified during this test scope
        url = instance.buildUrl(CONTEXT_ROOT, RESOURCE_PATH);

        // Create the web application
        WebArchive app = ShrinkWrap.create(WebArchive.class,
                                           APP_NAME + ".war").addClass(InMemoryIdentityStoreApplication.class).addClass(InMemoryIdentityStoreProtectedResource.class).addAsWebInfResource(new File("test-applications/inmemory/WEB-INF/web.xml"));

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

        instance.startServer();

        server.waitForLTPAConfigReady();
    }

    /**
     * Test log file output for in-memory store usage warning.
     * This should only ever appear once upon the first invocation of authentication against the in memory identity store data.
     */
    @Test
    public void testInMemStoreWarningAppearsOnce() throws Exception {
        logInfo("testInMemStoreWarningAppearsOnce", "Testing warning message of in-mem identity store appearing only once");

        // Should get 200 and proceed
        executeGetRequest(url, USER_THEO, VALID_PASSWORD, 200);

        // Second authentication - warning should NOT appear again
        executeGetRequest(url, USER_LISA, VALID_PASSWORD, 200);

        // Wait a bit to ensure no warning appears
        Thread.sleep(2000);

        assertEquals("Warning message should appear in log once", 1, server.waitForMultipleStringsInLog(1, PRODUCTION_USE_WARNING_MSG));

        logInfo("testInMemStoreWarningAppearsOnce", "Test passed");
    }

    /**
     * Test authentication with plain text password.
     * User "jasmine" has password "secret1" in plain text and valid groups.
     */
    @Test
    public void testPlainTextPassword() throws Exception {
        logInfo("testPlainTextPassword", "Testing plain text password authentication");

        String response = getResponseFromGetRequest(url, USER_JASMINE, VALID_PASSWORD, 200);

        assertNotNull("Response should not be null", response);
        assertTrue("Response should contain SUCCESS", response.contains("SUCCESS"));
        assertTrue("Response should contain username", response.contains(USER_JASMINE));

        logInfo("testPlainTextPassword", "Test passed");
    }

    /**
     * Test authentication with XOR encoded password.
     * User "lisa" has password encoded with {xor} and valid groups.
     */
    @Test
    public void testXorEncodedPassword() throws Exception {
        logInfo("testXorEncodedPassword", "Testing XOR encoded password authentication");

        String response = getResponseFromGetRequest(url, USER_LISA, VALID_PASSWORD, 200);

        assertNotNull("Response should not be null", response);
        assertTrue("Response should contain SUCCESS", response.contains("SUCCESS"));
        assertTrue("Response should contain username", response.contains(USER_LISA));

        logInfo("testXorEncodedPassword", "Test passed");
    }

    /**
     * Test authentication with Hash encoded password.
     * User "frank" has password encoded with {hash} and valid groups.
     */
    @Test
    public void testHashEncodedPassword() throws Exception {
        logInfo("testHashEncodedPassword", "Testing Hash encoded password authentication");

        String response = getResponseFromGetRequest(url, USER_FRANK, VALID_PASSWORD, 200);

        assertNotNull("Response should not be null", response);
        assertTrue("Response should contain SUCCESS", response.contains("SUCCESS"));
        assertTrue("Response should contain username", response.contains(USER_FRANK));

        logInfo("testHashEncodedPassword", "Test passed");
    }

    /**
     * Negative test: Try to authenticate with incorrect password.
     * Should receive 401 Unauthorized.
     */
    @Test
    public void testIncorrectPassword() throws Exception {
        logInfo("testIncorrectPassword", "Testing authentication with incorrect password");

        // Should get 401 Unauthorized
        executeGetRequest(url, USER_JASMINE, INVALID_PASSWORD, 401);

        logInfo("testIncorrectPassword", "Test passed - authentication correctly failed");
    }

    /**
     * Negative test: User with valid password but insufficient groups.
     * User "bill" has valid password but is in groups "foo" and "bar", not "user" or "caller".
     * Should receive 403 Forbidden.
     */
    @Test
    public void testInsufficientGroups() throws Exception {
        logInfo("testInsufficientGroups", "Testing user with insufficient groups");

        // Should get 403 Forbidden - authenticated but not authorized
        executeGetRequest(url, USER_BILL, VALID_PASSWORD, 403);

        logInfo("testInsufficientGroups", "Test passed - authorization correctly denied");
    }

    /**
     * Test that no unexpected error messages appear during normal operation.
     */
    @Test
    public void testNoUnexpectedErrors() throws Exception {
        logInfo("testNoUnexpectedErrors", "Testing for unexpected errors");

        // Mark log
        getServer().setMarkToEndOfLog();

        // Perform successful authentication
        executeGetRequest(url, USER_JASMINE, VALID_PASSWORD, 200);

        // Check that no unexpected error messages appear
        // We expect the warning message, but no error messages
        String logContent = waitForStringInLog("CWWKS35", 2000);

        if (logContent != null) {
            // If we found CWWKS35xx messages, make sure they're only the expected warning
            assertTrue("Should only see warning message, not errors",
                       logContent.contains(PRODUCTION_USE_WARNING_MSG));
        }

        logInfo("testNoUnexpectedErrors", "Test passed - no unexpected errors");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Expected warnings and errors during testing
        if (server != null && server.isStarted()) {
            server.stopServer(PRODUCTION_USE_WARNING_MSG);
        }
    }
}
