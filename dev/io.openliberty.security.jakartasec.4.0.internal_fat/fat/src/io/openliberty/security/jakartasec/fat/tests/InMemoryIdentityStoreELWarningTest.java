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

import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.EL_WARNING_MSG;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_JASMINE;
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
import inmemory.identity.store.ELExceptionInMemoryIdentityStoreApplication;
import inmemory.identity.store.InMemoryIdentityStoreProtectedResource;

/**
 * Tests the invalid priorityExpression: "SOME RUBBISH" in InMemoryIdentityStoreDefinition
 * Value cannot be resolved to a property so it throws an ELException in the InMemoryIdentityStoreDefinitionWrapper and should be caught with a warning shown
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class InMemoryIdentityStoreELWarningTest extends BaseJakartaSecurity40Test {

    private static final Class<?> c = InMemoryIdentityStoreELWarningTest.class;

    public static final String APP_NAME = "InvalidIdentityStore";
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
        InMemoryIdentityStoreELWarningTest instance = new InMemoryIdentityStoreELWarningTest();
        instance.logInfo("setUp", "Starting server setup...");

        // The URL is not expected to be modified during this test scope
        url = instance.buildUrl(CONTEXT_ROOT, RESOURCE_PATH);

        // Create the web application
        WebArchive app = ShrinkWrap.create(WebArchive.class,
                                           APP_NAME + ".war").addClass(ELExceptionInMemoryIdentityStoreApplication.class).addClass(InMemoryIdentityStoreProtectedResource.class).addAsWebInfResource(new File("test-applications/inmemory/WEB-INF/web.xml"));

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

        instance.startServer();
    }

    /**
     * Test authentication still works as expected despite an invalid PriorityExpression given in the definition
     */
    @Test
    public void testPlainTextPassword() throws Exception {

        logInfo("testPlainTextPassword", "Testing plain text password authentication");

        String response = getResponseFromGetRequest(url, USER_JASMINE, VALID_PASSWORD, 200);

        assertNotNull("Response should not be null", response);
        assertTrue("Response should contain SUCCESS", response.contains("SUCCESS"));
        assertTrue("Response should contain username", response.contains(USER_JASMINE));

        assertNotNull("EL warning message should appear in log once", server.findStringsInLogs(EL_WARNING_MSG)); // The (EL) expression used for the annotation attribute cannot be resolved
        logInfo("testPlainTextPassword", "Test passed");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        InMemoryIdentityStoreELWarningTest instance = new InMemoryIdentityStoreELWarningTest();
        // Expected warnings and errors during testing
        instance.stopServer("CWWKS2600W", //an identity store that is configured by using an @InMemoryIdentityStoreDefinition annotation was detected within this application. Do not use this definition in a production environment
                            EL_WARNING_MSG); // The (EL) expression used for the annotation attribute cannot be resolved

    }
}
