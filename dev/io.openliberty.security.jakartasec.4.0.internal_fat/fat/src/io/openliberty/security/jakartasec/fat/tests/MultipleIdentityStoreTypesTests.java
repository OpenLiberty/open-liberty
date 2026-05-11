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

import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.DB_PASSWORD;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.DB_USER_ALICE;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.DB_USER_CHARLIE;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.DB_USER_RORY;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.ID_STORE_VALIDATION_FAIL_MSG;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.ID_STORE_VALIDATION_SUCCESS_MSG;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.INVALID_PASSWORD;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PRODUCTION_USE_WARNING_MSG;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_JASMINE;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_LISA;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.VALID_PASSWORD;
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
import multiple.identity.stores.CustomDatabaseIdentityStore;
import multiple.identity.stores.MultipleIdentityStoresApplication;
import multiple.identity.stores.MultipleIdentityStoresProtectedResource;
import multiple.identity.stores.handlers.CustomIdentityStoreHandler;

/**
 * Tests for multiple identity stores of different types (in-memory and custom database).
 * This test class verifies:
 * 1. Both identity stores can coexist in the same application
 * 2. Priority mechanism works correctly - lower priority number = higher priority
 * 3. In-memory store (priority 100) is checked before database store (priority 200)
 * 4. Users can authenticate against either store based on where they are defined
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class MultipleIdentityStoreTypesTests extends BaseJakartaSecurity40Test {

    private static final Class<?> c = MultipleIdentityStoreTypesTests.class;

    public static final String APP_NAME = "MultipleIdentityStores";
    private static final String CONTEXT_ROOT = "/" + APP_NAME;
    private static final String RESOURCE_PATH = "/resource/test";
    private static final String USER_PATH = "/resource/user";
    private static final String DBUSER_PATH = "/resource/dbuser";
    private static final String MEMORYUSER_PATH = "/resource/memoryuser";

    private static String url = null;
    private static String userUrl = null;
    private static String dbuserUrl = null;
    private static String memoryuserUrl = null;

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
        MultipleIdentityStoreTypesTests instance = new MultipleIdentityStoreTypesTests();
        instance.logInfo("setUp", "Starting server setup...");

        // Build URLs
        url = instance.buildUrl(CONTEXT_ROOT, RESOURCE_PATH);
        userUrl = instance.buildUrl(CONTEXT_ROOT, USER_PATH);
        dbuserUrl = instance.buildUrl(CONTEXT_ROOT, DBUSER_PATH);
        memoryuserUrl = instance.buildUrl(CONTEXT_ROOT, MEMORYUSER_PATH);

        // Create the web application with both in-memory and custom database identity stores
        WebArchive app = ShrinkWrap.create(WebArchive.class,
                                           APP_NAME + ".war").addClass(MultipleIdentityStoresApplication.class).addClass(CustomDatabaseIdentityStore.class).addClass(MultipleIdentityStoresProtectedResource.class).addClass(CustomIdentityStoreHandler.class).addAsWebInfResource(new File("test-applications/multipleidentitystores/WEB-INF/web.xml"));

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

        instance.startServer();
    }

    /**
     * Test that a user defined only in the in-memory identity store can authenticate successfully.
     * User "jasmine" exists only in the in-memory store with plain text password.
     */
    @Test
    public void testInMemoryStoreUser() throws Exception {
        logInfo("testInMemoryStoreUser", "Testing authentication with in-memory store user");

        String response = getResponseFromGetRequest(url, USER_JASMINE, VALID_PASSWORD, 200);

        assertNotNull("Response should not be null", response);
        assertTrue("Response should contain SUCCESS", response.contains("SUCCESS"));
        assertTrue("Response should contain username", response.contains(USER_JASMINE));

        logInfo("testInMemoryStoreUser", "Test passed - in-memory store user authenticated successfully");
    }

    /**
     * Test that a user defined only in the database identity store can authenticate successfully.
     * User "rory" exists only in the database store.
     */
    @Test
    public void testDatabaseStoreUser() throws Exception {
        logInfo("testDatabaseStoreUser", "Testing authentication with database store user");

        String response = getResponseFromGetRequest(url, DB_USER_RORY, DB_PASSWORD, 200);

        assertNotNull("Response should not be null", response);
        assertTrue("Response should contain SUCCESS", response.contains("SUCCESS"));
        assertTrue("Response should contain username", response.contains(DB_USER_RORY));

        logInfo("testDatabaseStoreUser", "Test passed - database store user authenticated successfully");
    }

    /**
     * Test priority mechanism when a user exists in both stores.
     * User "alice" exists in both stores with different passwords and groups.
     * The in-memory store has higher priority (100 vs 200), so it should be used.
     * In-memory store: alice/reallysecretpassw0rd with groups "memoryuser", "user"
     * Database store: alice/dbpassword123 with groups "dbuser", "caller"
     */
    @Test
    public void testPriorityWithUserInBothStores() throws Exception {
        logInfo("testPriorityWithUserInBothStores", "Testing priority when user exists in both stores");

        // Should authenticate with in-memory store password (higher priority)
        String response = getResponseFromGetRequest(url, DB_USER_ALICE, VALID_PASSWORD, 200);

        assertNotNull("Response should not be null", response);
        assertTrue("Response should contain SUCCESS", response.contains("SUCCESS"));
        assertTrue("Response should contain username", response.contains(DB_USER_ALICE));

        // Verify user has groups from in-memory store (memoryuser)
        String memoryResponse = getResponseFromGetRequest(memoryuserUrl, DB_USER_ALICE, VALID_PASSWORD, 200);
        assertNotNull("Memory user response should not be null", memoryResponse);
        assertTrue("Should have access to memoryuser role", memoryResponse.contains("SUCCESS"));

        logInfo("testPriorityWithUserInBothStores", "Test passed - in-memory store took priority");
    }

    /**
     * Test that database store password is checked on both stores and is validated successfully in the one with lower priority.
     * Since in-memory store has higher priority, it should be checked first and fail to validate the user.
     * The database password should work as the next in-line id store.
     */
    @Test
    public void testUserCredentialOnBothStores() throws Exception {
        logInfo("testUserCredentialOnBothStores",
                "Testing that a user rendential is validated in both stored, respecting the priority");

        // Should pass with database password since the credential exists there
        executeGetRequest(url, DB_USER_ALICE, DB_PASSWORD, 200);

        // The prioritised in-memory store should fail to validate that credential,
        // so database store should be called for credential validation as the next in-line id store.
        String inMemIdStoreNotValidated = String.format(ID_STORE_VALIDATION_FAIL_MSG, "IdentityStore");
        String customDbIdStoreValidated = String.format(ID_STORE_VALIDATION_SUCCESS_MSG, "CustomDatabaseIdentityStore");

        assertNotNull("InMemoryIdentityStore should fail to validate the user", waitForStringInLog(inMemIdStoreNotValidated));
        assertNotNull("CustomDatabaseIdentityStore should be validate the user", waitForStringInLog(customDbIdStoreValidated));

        logInfo("testUserCredentialOnBothStores",
                "Test passed - database password correctly validaded on the lower priority store");
    }

    /**
     * Test that users from database store have correct groups.
     * User "charlie" is only in database store with groups "dbadmin", "user".
     */
    @Test
    public void testDatabaseStoreUserGroups() throws Exception {
        logInfo("testDatabaseStoreUserGroups", "Testing database store user has correct groups");

        // Should have access to user role
        String userResponse = getResponseFromGetRequest(userUrl, DB_USER_CHARLIE, DB_PASSWORD, 200);
        assertNotNull("User response should not be null", userResponse);
        assertTrue("Should have access to user role", userResponse.contains("SUCCESS"));

        // Should NOT have access to dbuser role (charlie has dbadmin, not dbuser)
        executeGetRequest(dbuserUrl, DB_USER_CHARLIE, DB_PASSWORD, 403);

        logInfo("testDatabaseStoreUserGroups", "Test passed - database store user has correct groups");
    }

    /**
     * Test that user from database store with dbuser group can access dbuser endpoint.
     * User "alice" in database store has "dbuser" and "caller" groups.
     * But since alice exists in in-memory store with higher priority, we test with "rory".
     */
    @Test
    public void testDatabaseStoreDbUserAccess() throws Exception {
        logInfo("testDatabaseStoreDbUserAccess", "Testing database store user with dbuser group");

        // Rory has dbuser group in database store
        String response = getResponseFromGetRequest(dbuserUrl, DB_USER_RORY, DB_PASSWORD, 200);
        assertNotNull("Response should not be null", response);
        assertTrue("Should have access to dbuser role", response.contains("SUCCESS"));

        logInfo("testDatabaseStoreDbUserAccess", "Test passed - database user accessed dbuser endpoint");
    }

    /**
     * Test that invalid credentials fail for both stores.
     */
    @Test
    public void testInvalidCredentials() throws Exception {
        logInfo("testInvalidCredentials", "Testing authentication with invalid credentials");

        // Invalid password for in-memory store user
        executeGetRequest(url, USER_JASMINE, INVALID_PASSWORD, 401);

        // Invalid password for database store user
        executeGetRequest(url, DB_USER_RORY, INVALID_PASSWORD, 401);

        // Invalid user
        executeGetRequest(url, "nonexistent", VALID_PASSWORD, 401);

        logInfo("testInvalidCredentials", "Test passed - invalid credentials correctly rejected");
    }

    /**
     * Test that in-memory store user with XOR encoded password works.
     * User "lisa" has XOR encoded password in in-memory store.
     */
    @Test
    public void testInMemoryStoreXorEncodedPassword() throws Exception {
        logInfo("testInMemoryStoreXorEncodedPassword", "Testing in-memory store with XOR encoded password");

        String response = getResponseFromGetRequest(url, USER_LISA, VALID_PASSWORD, 200);

        assertNotNull("Response should not be null", response);
        assertTrue("Response should contain SUCCESS", response.contains("SUCCESS"));
        assertTrue("Response should contain username", response.contains(USER_LISA));

        logInfo("testInMemoryStoreXorEncodedPassword", "Test passed - XOR encoded password worked");
    }

    /**
     * Test that both identity stores are being used by checking server logs.
     * This verifies that both stores are initialized and active.
     */
    @Test
    public void testBothStoresActive() throws Exception {
        logInfo("testBothStoresActive", "Testing that both identity stores are active");

        // Authenticate with in-memory store user
        executeGetRequest(url, USER_JASMINE, VALID_PASSWORD, 200);

        // Authenticate with database store user
        executeGetRequest(url, DB_USER_RORY, DB_PASSWORD, 200);

        // Check that database store was invoked
        String logContent = waitForStringInLog("CustomDatabaseIdentityStore", 5000);
        assertNotNull("Database identity store should be invoked", logContent);

        logInfo("testBothStoresActive", "Test passed - both identity stores are active");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(PRODUCTION_USE_WARNING_MSG);
        }
    }
}
