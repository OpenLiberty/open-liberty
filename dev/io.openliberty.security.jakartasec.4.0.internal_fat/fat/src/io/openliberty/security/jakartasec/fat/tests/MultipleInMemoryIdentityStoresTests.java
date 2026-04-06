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
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PASSWORD_XOR_VALID;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PRODUCTION_USE_WARNING_MSG;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_BILL;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_JASMINE;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.VALID_PASSWORD;

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
import inmemory.identity.store.InMemoryIdentityStoreHighestPriorityApplication;
import inmemory.identity.store.InMemoryIdentityStoreProtectedResource;

/**
 * Tests for multiple in-memory id stores with various password encoding schemes.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class MultipleInMemoryIdentityStoresTests extends BaseJakartaSecurity40Test {

    private static final Class<?> c = MultipleInMemoryIdentityStoresTests.class;

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
        MultipleInMemoryIdentityStoresTests instance = new MultipleInMemoryIdentityStoresTests();
        instance.logInfo("setUp", "Starting server setup...");

        // The URL is not expected to be modified during this test scope
        url = instance.buildUrl(CONTEXT_ROOT, RESOURCE_PATH);

        // Create the web application containing two in-memory id stores
        WebArchive app = ShrinkWrap.create(WebArchive.class,
                                           APP_NAME + ".war").addClass(InMemoryIdentityStoreApplication.class).addClass(InMemoryIdentityStoreHighestPriorityApplication.class).addClass(InMemoryIdentityStoreProtectedResource.class).addAsWebInfResource(new File("test-applications/inmemory/WEB-INF/web.xml"));

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

        instance.startServer();
    }

    /**
     * Test the priority when more than one in-memory identity stores are defined.
     * Two in-memory id stores are involved in the same war; one with priority 5 and one with 10
     * The one with the lower value in priority element takes precedence.
     *
     */
    @Test
    public void testInMemStoresPriority() throws Exception {
        logInfo("testInMemStoresPriority", "Testing that the id store with the lower value in priority element takes precedence over the the other");

        // Should get 401 - this credential does not exist in either of the id stores
        executeGetRequest(url, USER_BILL, VALID_PASSWORD, 401);

        // Should get 401 - this credential exists only on the less prioritised id-store (with priority=10)
        executeGetRequest(url, USER_BILL, PASSWORD_XOR_VALID, 401);

        // Should get 200 - this credential exists on the highest prioritised id-store (with priority=5)
        executeGetRequest(url, USER_JASMINE, PASSWORD_XOR_VALID, 200);

        // Should get 200 - the user exists on the highest prioritised id-store with different password
        executeGetRequest(url, USER_JASMINE, INVALID_PASSWORD, 401);

        logInfo("testInMemStoresPriority", "Test passed");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Expected warnings and errors during testing
        if (server != null && server.isStarted()) {
            server.stopServer(PRODUCTION_USE_WARNING_MSG);
        }
    }
}
