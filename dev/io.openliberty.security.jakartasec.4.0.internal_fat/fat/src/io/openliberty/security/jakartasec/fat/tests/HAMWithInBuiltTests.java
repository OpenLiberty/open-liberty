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
import multiple.ham.custom.hams.CustomHAMOne;

/**
 * Tests appSecurity-6.0
 * Tests a custom HAM with one in-built HAM
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class HAMWithInBuiltTests extends BaseJakartaSecurity40Test {

    private static final Class<?> c = MultipleHAMDuplicateTests.class;

    public static final String APP_NAME = "HAMWithInBuiltApp";
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
        HAMWithInBuiltTests instance = new HAMWithInBuiltTests();
        WebArchive multipleHamApp = ShrinkWrap.create(WebArchive.class,
                                                      APP_NAME + ".war").addPackage("ham.with.in.built").addClass(CustomHAMOne.class).addClass(MultipleHAMProtectedResource.class);

        // The URL is not expected to be modified during this test scope
        url = "http://localhost:" + server.getHttpDefaultPort() + CONTEXT_ROOT + RESOURCE_PATH;

        ShrinkHelper.exportDropinAppToServer(server, multipleHamApp, DeployOptions.SERVER_ONLY);

        instance.startServer();
    }

    /*
     * CustomHAMOne has the most priority with Priority = 100, the in-built HAM's have less priority than custom HAM's.
     *
     * Assert that we find the following message in trace.log:
     *
     * Order of HttpAuthenticationMechanisms found (the first one will be used if its prioritization is unique -
     *
     * @Priority for application HAMs and HAM type - Oidc/CustomForm/Form/Basic - for in-built HAMs):
     * CustomHAMOne Priority = 100, BasicHttpAuthenticationMechanism
     *
     */
    @Test
    public void testCustomHamsWithInBuiltHamPrioritization() throws Exception {

        executeGetRequestWithTraceMark(url, 200);

        // Check that warning appears in trace
        assertStringInTrace("Warning message should appear in log", Jakartasec40TestConstants.HAM_ORDER_FOUND_MESSAGE);
        assertStringInTrace("Warning message should appear in log", Jakartasec40TestConstants.CUSTOM_WITH_INBUILT_PRIORITY_ORDER_MESSAGE);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        HAMWithInBuiltTests instance = new HAMWithInBuiltTests();
        instance.stopServer();
    }
}
