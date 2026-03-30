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

import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PASSWORD_XOR_VALID;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PRODUCTION_USE_WARNING_MSG;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_BILL;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_JASMINE;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_LISA;
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
 * Tests for multiple applications with specified in-memory id stores having various password encoding schemes.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class MultipleAppsInMemoryIdStoresTests extends BaseJakartaSecurity40Test {

    private static final Class<?> c = MultipleAppsInMemoryIdStoresTests.class;

    public static final String APP_NAME = "IdentityStore";
    public static final String APP_NAME2 = "IdentityStore2";

    private static final String CONTEXT_ROOT = "/" + APP_NAME;
    private static final String CONTEXT_ROOT2 = "/" + APP_NAME2;
    private static final String RESOURCE_PATH = "/resource/test";

    private static String url;
    private static String url2;

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
        MultipleAppsInMemoryIdStoresTests instance = new MultipleAppsInMemoryIdStoresTests();
        instance.logInfo("setUp", "Starting server setup...");

        url = instance.buildUrl(CONTEXT_ROOT, RESOURCE_PATH);
        url2 = instance.buildUrl(CONTEXT_ROOT2, RESOURCE_PATH);

        // Create the separate archives each one containing a in-memory id store definition with different priority
        WebArchive multiapp1 = ShrinkWrap.create(WebArchive.class,
                                                 APP_NAME + ".war").addClass(InMemoryIdentityStoreApplication.class).addClass(InMemoryIdentityStoreProtectedResource.class).addAsWebInfResource(new File("test-applications/inmemory/WEB-INF/web.xml"));

        ShrinkHelper.exportDropinAppToServer(server, multiapp1, DeployOptions.SERVER_ONLY);

        WebArchive multiapp2 = ShrinkWrap.create(WebArchive.class,
                                                 APP_NAME2 + ".war").addClass(InMemoryIdentityStoreHighestPriorityApplication.class).addClass(InMemoryIdentityStoreProtectedResource.class).addAsWebInfResource(new File("test-applications/inmemory/WEB-INF/web.xml"));

        ShrinkHelper.exportDropinAppToServer(server, multiapp2, DeployOptions.SERVER_ONLY);

        instance.startServer();
    }

    /**
     * Test different definitions of in-memory identity stores on multiple apps.
     * Assert that they operate on application scope and there is no interlock
     *
     */
    @Test
    public void testInMemStoresInMultiApps() throws Exception {
        logInfo("testInMemStoresInMultiApps", "Testing the in-memory id stores in multiple applications");

        //USER_BILL exists as user only in the first id store, yet with different password; thus it returns 403
        executeGetRequest(url, USER_BILL, VALID_PASSWORD, 403);
        executeGetRequest(url2, USER_BILL, VALID_PASSWORD, 401);

        //USER_LISA credentials exist only in one of the id stores; thus it returns 401 on the second id store
        executeGetRequest(url, USER_LISA, PASSWORD_XOR_VALID, 200);
        executeGetRequest(url2, USER_LISA, PASSWORD_XOR_VALID, 401);

        //USER_JASMINE credentials exist on both id stores with valid passwords
        executeGetRequest(url, USER_JASMINE, PASSWORD_XOR_VALID, 200);
        executeGetRequest(url2, USER_JASMINE, PASSWORD_XOR_VALID, 200);

        logInfo("testInMemStoresInMultiApps", "Test passed");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Expected warnings and errors during testing
        if (server != null && server.isStarted()) {
            server.stopServer(PRODUCTION_USE_WARNING_MSG);
        }
    }
}
