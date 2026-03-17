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
import io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants;
import multiple.ham.common.MultipleHAMProtectedResource;

/**
 * Tests appSecurity-6.0
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class MultipleHAMInbuiltTests extends BaseJakartaSecurity40Test {
    private static final Class<?> c = MultipleHAMInbuiltTests.class;

    public static final String APP_NAME = "MultipleHAMInbuiltApp";
    private static final String CONTEXT_ROOT = "/" + APP_NAME;
    private static final String RESOURCE_PATH = "/resource/test";

    private static String url = null;

    @Server(MULTIPLE_HAM_SERVER_NAME)
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
        MultipleHAMInbuiltTests instance = new MultipleHAMInbuiltTests();
        WebArchive multipleHamApp = ShrinkWrap.create(WebArchive.class,
                                                      APP_NAME + ".war").addPackage("multiple.ham.inbuilt").addClass(MultipleHAMProtectedResource.class);

        // The URL is not expected to be modified during this test scope
        url = "http://localhost:" + server.getHttpDefaultPort() + CONTEXT_ROOT + RESOURCE_PATH;

        ShrinkHelper.exportDropinAppToServer(server, multipleHamApp, DeployOptions.SERVER_ONLY);

        instance.startServer();
    }

    /*
     * Assert that we find the following message in trace.log:
     *
     * Order of HttpAuthenticationMechanisms found (the first one will be used if its prioritization is unique -
     *
     * @Priority for application HAMs and HAM type - Oidc/CustomForm/Form/Basic - for in-built HAMs):
     * FormAuthenticationMechanism, BasicHttpAuthenticationMechanism
     *
     */
    @Test
    public void testMultipleHAMInbuiltPrioritization() throws Exception {

        // Mark the trace before making HTTP connection and execute request
        executeGetRequestWithTraceMark(url, 200);

        // Check that messages appear in trace
        assertStringInTrace("Warning message should appear in log",
                            Jakartasec40TestConstants.HAM_ORDER_FOUND_MESSAGE);
        assertStringInTrace("Warning message should appear in log",
                            Jakartasec40TestConstants.INBUILT_HAM_PRIORITY_ORDER_MESSAGE);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MultipleHAMInbuiltTests instance = new MultipleHAMInbuiltTests();
        instance.stopServer();
    }

}
