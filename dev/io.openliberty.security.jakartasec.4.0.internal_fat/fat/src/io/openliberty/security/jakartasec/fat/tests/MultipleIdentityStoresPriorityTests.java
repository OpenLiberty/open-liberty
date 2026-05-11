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

import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.DB_USER_ALICE;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.ID_STORE_FOUND_MSG;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.ID_STORE_VALIDATION_SUCCESS_MSG;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PRODUCTION_USE_WARNING_MSG;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.VALID_PASSWORD;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
 * This test class verifies that when a user exists in both stores, the higher priority store is used
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class MultipleIdentityStoresPriorityTests extends BaseJakartaSecurity40Test {

    private static final Class<?> c = MultipleIdentityStoresPriorityTests.class;

    public static final String APP_NAME = "MultipleIdentityStores";
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
        MultipleIdentityStoresPriorityTests instance = new MultipleIdentityStoresPriorityTests();
        instance.logInfo("setUp", "Starting server setup...");

        // Build URLs
        url = instance.buildUrl(CONTEXT_ROOT, RESOURCE_PATH);

        // Create the web application with both in-memory and custom database identity stores
        WebArchive app = ShrinkWrap.create(WebArchive.class,
                                           APP_NAME + ".war").addClass(MultipleIdentityStoresApplication.class).addClass(CustomDatabaseIdentityStore.class).addClass(MultipleIdentityStoresProtectedResource.class).addClass(CustomIdentityStoreHandler.class).addAsWebInfResource(new File("test-applications/multipleidentitystores/WEB-INF/web.xml"));

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

        instance.startServer();
    }

    /**
     * Test the priority order by checking which store validates first.
     * When alice authenticates with in-memory password, database store should not be called.
     */
    @Test
    public void testPriorityOrder() throws Exception {
        logInfo("testPriorityOrder", "Testing identity store priority order");

        // Authenticate alice with in-memory store password
        executeGetRequest(url, DB_USER_ALICE, VALID_PASSWORD, 200);

        String inMemIdStoreName = String.format(ID_STORE_FOUND_MSG, "IdentityStore");
        String customDbIdStoreName = String.format(ID_STORE_FOUND_MSG, "CustomDatabaseIdentityStore");

        //Both stores are found
        assertNotNull("InMemoryIdentityStore should be found", waitForStringInLog(inMemIdStoreName));
        assertNotNull("CustomDatabaseIdentityStore should be found", waitForStringInLog(customDbIdStoreName));

        // The in-memory store should validate successfully, so database store should not be called
        // for credential validation (it might be called for groups, but not for validation)
        String inMemIdStoreValidated = String.format(ID_STORE_VALIDATION_SUCCESS_MSG, "IdentityStore");
        String customDbIdStoreValidated = String.format(ID_STORE_VALIDATION_SUCCESS_MSG, "CustomDatabaseIdentityStore");
        assertNotNull("InMemoryIdentityStore should be validated", waitForStringInLog(inMemIdStoreValidated));
        assertNull("CustomDatabaseIdentityStore should not be validated", waitForStringInLog(customDbIdStoreValidated));

        logInfo("testPriorityOrder", "Test passed - priority order verified");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(PRODUCTION_USE_WARNING_MSG);
        }
    }
}
