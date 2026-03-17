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
import multiple.ham.common.MultipleHAMProtectedResource;
import multiple.ham.custom.hams.CustomHAMOne;
import multiple.ham.custom.hams.CustomHAMThree;
import multiple.ham.custom.hams.CustomHAMTwo;

/**
 * Tests appSecurity-6.0
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class MultipleHAMCustomTests extends BaseJakartaSecurity40Test {

    private static final Class<?> c = MultipleHAMCustomTests.class;

    public static final String APP_NAME = "CustomHAMApp";
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
        MultipleHAMCustomTests instance = new MultipleHAMCustomTests();
        WebArchive multipleHamApp = ShrinkWrap.create(WebArchive.class,
                                                      APP_NAME + ".war").addPackage("multiple.ham.custom").addClass(MultipleHAMProtectedResource.class).addClass(CustomHAMOne.class).addClass(CustomHAMTwo.class).addClass(CustomHAMThree.class);

        // The URL is not expected to be modified during this test scope
        url = instance.buildUrl(CONTEXT_ROOT, RESOURCE_PATH);

        ShrinkHelper.exportDropinAppToServer(server, multipleHamApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    /*
     * Assert that we find the following message in trace.log:
     *
     * Order of HttpAuthenticationMechanisms found (the first one will be used if its prioritization is unique -
     *
     * @Priority for application HAMs and HAM type - Oidc/CustomForm/Form/Basic - for in-built HAMs):
     * CustomHAMThree Priority = 300, CustomHAMTwo Priority = 200, CustomHAMOne Priority = 100
     *
     * CustomHAMThree has the most priority with Priority = 300, CustomHAMOne has the least priority with Priority = 100
     */
    @Test
    public void testCustomHamsPrioritization() throws Exception {
        String startOfMessage = "Order of HttpAuthenticationMechanisms found";
        String prioritizationOrder = "CustomHAMThree Priority = 300, CustomHAMTwo Priority = 200, CustomHAMOne Priority = 100";

        // Mark the trace before making HTTP connection and execute request
        executeGetRequestWithTraceMark(url, 200);

        // Check that messages appear in trace
        assertStringInTrace("Warning message should appear in log", startOfMessage);
        assertStringInTrace("Warning message should appear in log", prioritizationOrder);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MultipleHAMCustomTests instance = new MultipleHAMCustomTests();
        instance.stopServer();
    }

}
