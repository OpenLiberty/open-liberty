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

import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.AES_ENCRYPTED_PWD_WARNING_MSG;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PRODUCTION_USE_WARNING_MSG;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_SALLY;
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
import inmemory.identity.store.InMemoryIdentityStoreApplication;
import inmemory.identity.store.InMemoryIdentityStoreProtectedResource;

/**
 * Tests for InMemoryIdentityStoreDefinition with various password encoding schemes.
 * Tests the scenario of a AES encrypted password on user credentials.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class InMemoryIdStoreAesEncodedPwdTests extends BaseJakartaSecurity40Test {

    private static final Class<?> c = InMemoryIdStoreAesEncodedPwdTests.class;

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
        InMemoryIdStoreAesEncodedPwdTests instance = new InMemoryIdStoreAesEncodedPwdTests();
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
     * Test authentication with AES encoded password.
     * User "sally" has password encoded with {aes} and valid groups.
     * Also test that the encryption warning message appears in logs
     */
    @Test
    public void testAesEncodedPassword() throws Exception {
        logInfo("testAesEncodedPassword", "Testing AES encoded password authentication");

        String response = getResponseFromGetRequest(url, USER_SALLY, VALID_PASSWORD, 200);

        assertNotNull("Response should not be null", response);
        assertTrue("Response should contain SUCCESS", response.contains("SUCCESS"));
        assertTrue("Response should contain username", response.contains(USER_SALLY));

        // Verify warning message appears in log
        assertStringInLog("Decode error message should appear in log", AES_ENCRYPTED_PWD_WARNING_MSG, 5000);

        logInfo("testAesEncodedPassword", "Test passed");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(PRODUCTION_USE_WARNING_MSG, AES_ENCRYPTED_PWD_WARNING_MSG);
        }
    }
}
