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

import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.BADLY_DECODED_PWD_ERROR_MSG;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PRODUCTION_USE_WARNING_MSG;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_JOHNNY;
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
import inmemory.identity.store.InMemoryIdentityStoreProtectedResource;

/**
 * Tests for InMemoryIdentityStoreDefinition with various password encoding schemes.
 * Tests the negative scenario of a badly encoded password on user credentials.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class InMemoryIdStoreBadlyEncodedPwdTests extends BaseJakartaSecurity40Test {

    private static final Class<?> c = InMemoryIdStoreBadlyEncodedPwdTests.class;

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
        InMemoryIdStoreBadlyEncodedPwdTests instance = new InMemoryIdStoreBadlyEncodedPwdTests();
        instance.logInfo("setUp", "Starting server setup...");

        // The URL is not expected to be modified during this test scope
        url = instance.buildUrl(CONTEXT_ROOT, RESOURCE_PATH);

        // Create the web application
        WebArchive app = ShrinkWrap.create(WebArchive.class,
                                           APP_NAME + ".war").addClass(InMemoryIdentityStoreApplication.class).addClass(InMemoryIdentityStoreProtectedResource.class).addAsWebInfResource(new File("test-applications/inmemory/WEB-INF/web.xml"));

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

        instance.startServer();
    }

    /**
     * Negative test: User with badly encoded password.
     * User "johnny" has invalid {xor} encoding which should cause a decode error.
     * Should receive 401 Unauthorized and error message in log.
     */
    @Test
    public void testBadlyEncodedPassword() throws Exception {
        logInfo("testBadlyEncodedPassword", "Testing badly encoded password");

        // Mark log to check for error message
        getServer().setMarkToEndOfLog();

        // Should get 401 Unauthorized
        executeGetRequest(url, USER_JOHNNY, VALID_PASSWORD, 401);

        // Verify error message appears in log
        assertStringInLog("Decode error message should appear in log", BADLY_DECODED_PWD_ERROR_MSG, 5000);

        logInfo("testBadlyEncodedPassword", "Test passed - decode error correctly logged");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(PRODUCTION_USE_WARNING_MSG, BADLY_DECODED_PWD_ERROR_MSG);
        }
    }
}
