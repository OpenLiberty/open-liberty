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
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.SERVER_CONFIG_UPDATE_MESSAGES_REGEX;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_JASMINE;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.WEB_APP_SECURITY_CONFIGURATION_UPDATED;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
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
 * Tests for InMemoryIdentityStore enabling element on server configuration.
 * Tests several scenarios with specific server configuration files
 * (missing element, existing element set to false/true).
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class InMemoryIdentityStoreEnablementTests extends BaseJakartaSecurity40Test {

    private static final Class<?> c = InMemoryIdentityStoreEnablementTests.class;

    public static final String APP_NAME = "IdentityStoreEnablement";
    private static final String CONTEXT_ROOT = "/" + APP_NAME;
    private static final String RESOURCE_PATH = "/resource/test";
    public static final String SERVER_XML_ID_STORE_DISABLED = "inMemoryIdStoreDisabled.xml";
    public static final String SERVER_XML_ID_STORE_MISSING_ELEMENT = "inMemoryIdStoreNoElement.xml";

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
    public static void initialSetup() throws Exception {
        // Save the server configuration before all tests.
        server.saveServerConfiguration();
    }

    @Before
    public void testSetUp() throws Exception {
        InMemoryIdentityStoreEnablementTests instance = new InMemoryIdentityStoreEnablementTests();
        logInfo("testSetUp", "Starting server setup for the test scenario...");

        // The URL is not expected to be modified during this test scope
        url = instance.buildUrl(CONTEXT_ROOT, RESOURCE_PATH);

        // Create the web application
        WebArchive app = ShrinkWrap.create(WebArchive.class,
                                           APP_NAME + ".war").addClass(InMemoryIdentityStoreApplication.class).addClass(InMemoryIdentityStoreProtectedResource.class).addAsWebInfResource(new File("test-applications/inmemory/WEB-INF/web.xml"));

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

        instance.startServer();
    }

    @After
    public void testTearDown() throws Exception {
        server.stopServer(PRODUCTION_USE_WARNING_MSG);

        // Restore the server configuration, after each test case.
        server.restoreServerConfiguration();
    }

    /**
     * Test that the specified in-memory identity store is only involved in the authentication work flow
     * if it is explicitly enabled in the server.xml via the new attribute allowInMemoryIdentityStores
     * A custom server configuration is used with no such element specified (defaulted to false if missing)
     */
    @Test
    public void testInMemStoreNotAllowedIfElementIsAbsent() throws Exception {
        logInfo("testInMemStoreNotAllowedIfElementIsAbsent", "Testing that in-mem identity store is not allowed if the element is missing on the server config");

        testAuthenticationWithDifferentServerConfig(SERVER_XML_ID_STORE_MISSING_ELEMENT);

        logInfo("testInMemStoreNotAllowedIfElementIsAbsent", "Test passed");
    }

    /**
     * Test that the specified in-memory identity store is not used during the authentication process
     * when the use of this store is not enabled via server XML configuration.
     * A custom server configuration is used with allowInMemoryIdentityStores = false.
     */
    @Test
    public void testInMemStoreCustomConfigIsNotAllowed() throws Exception {
        logInfo("testInMemStoreCustomConfigIsNotAllowed", "Testing that in-mem identity store is not allowed when element is set to false");

        testAuthenticationWithDifferentServerConfig(SERVER_XML_ID_STORE_DISABLED);

        logInfo("testInMemStoreCustomConfigIsNotAllowed", "Test passed");
    }

    /**
     * Reusable test logic to test the authentication of a GET-request
     * on a restarted application with an updated server configuration
     *
     * @param fileName the name of the custom server xml file
     * @throws Exception
     */
    private void testAuthenticationWithDifferentServerConfig(String fileName) throws Exception {
        //1. Test with the existing server xml that allows the specified in-memory id store
        executeGetRequest(url, USER_JASMINE, INVALID_PASSWORD, 401);

        //2. Replace the server configuration file with the parameter
        String updatedMessage = setServerConfig(fileName, server.getConsoleLogFile(), server);
        assertNotNull("The server config change was not completed", updatedMessage);

        //3. Check and confirm that the element has now changed
        String logContent = waitForStringInLog(WEB_APP_SECURITY_CONFIGURATION_UPDATED, 2000);

        if (logContent != null) {
            assertTrue("The following properties were modified: allowInMemoryIdentityStores",
                       logContent.contains(WEB_APP_SECURITY_CONFIGURATION_UPDATED));
        }

        //4. Restart the application with the new server configuration
        server.restartDropinsApplication(APP_NAME + ".war");

        //5. Should get 403 since in-memory id-store is no longer involved in authentication
        executeGetRequest(url, USER_JASMINE, INVALID_PASSWORD, 403);
    }

    /**
     * Update the server configuration with a specified file
     * Wait for message that indicates the config change
     *
     * @param fileName the name of the custom server xml file
     * @param logFile
     * @param server
     * @return the matching line in the log, or null if no matches
     *         appear before the timeout expires
     * @throws Exception
     */
    private static String setServerConfig(String fileName, RemoteFile logFile, LibertyServer server) throws Exception {
        server.setMarkToEndOfLog(logFile);
        server.setServerConfigurationFile(fileName);
        return server.waitForStringInLogUsingMark(SERVER_CONFIG_UPDATE_MESSAGES_REGEX);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
