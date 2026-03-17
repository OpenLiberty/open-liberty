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
import multiple.ham.custom.hams.CustomHAMOneOperator;
import multiple.ham.custom.hams.CustomHAMTwoAdmin;
import multiple.ham.custom.handlers.BaseHAMHandler;
import multiple.ham.custom.handlers.CustomHAMNoTesterHandler;
import multiple.ham.inbuilt.SingleHAMQualifierApplication;

/**
 * Tests appSecurity-6.0
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class SingleHAMInbuiltCustomQualifierTests extends BaseJakartaSecurity40Test {

    private static final Class<?> c = SingleHAMInbuiltCustomQualifierTests.class;

    public static final String APP_NAME = "SingleHAMInbuiltCustomQualifierApp";
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
        SingleHAMInbuiltCustomQualifierTests instance = new SingleHAMInbuiltCustomQualifierTests();
        WebArchive multipleHamApp = ShrinkWrap.create(WebArchive.class,
                                                      APP_NAME + ".war").addClass(SingleHAMQualifierApplication.class).addPackage("multiple.ham.common.qualifiers").addClass(MultipleHAMProtectedResource.class).addClass(BaseHAMHandler.class).addClass(CustomHAMNoTesterHandler.class).addClass(CustomHAMOneOperator.class).addClass(CustomHAMTwoAdmin.class);

        // The URL is not expected to be modified during this test scope
        url = "http://localhost:" + server.getHttpDefaultPort() + CONTEXT_ROOT + RESOURCE_PATH;

        ShrinkHelper.exportDropinAppToServer(server, multipleHamApp, DeployOptions.SERVER_ONLY);

        instance.startServer();
    }

    /*
     * Assert that we find the custom output messages in trace.log
     * indicating that all qualifier HAMs have been injected successfully in the Custom HAM Handler
     *
     */
    @Test
    public void testQualifiersInjectionCustomOutput() throws Exception {

        executeGetRequestWithTraceMark(url, 200);

        // Check the injection of adminHAM as inbuilt basic HAM
        assertStringInTrace("Output message for adminHAM injection as CustomHAMTwoAdmin not found",
                            Jakartasec40TestConstants.CUSTOM_HAM_TWO_ADMIN_QUALIFIER_MESSAGE);
        // Check the injection of userHAM as inbuilt basic HAM
        assertStringInTrace("Output message for userHAM injection as BasicHAM not found",
                            Jakartasec40TestConstants.HAM_BASIC_USER_QUALIFIER_MESSAGE);
        // Check the injection of operatorHAM as inbuilt CustomForm HAM
        assertStringInTrace("Output message for operatorHAM injection as CustomHAMOneOperator not found",
                            Jakartasec40TestConstants.CUSTOM_HAM_ONE_OPERATOR_QUALIFIER_MESSAGE);
    }

    /*
     * Assert that the custom HAM prioritization is printed as it is defined on the custom handler.
     * Since we explicitly prioritized the Admin qualifier on CustomHAMTwoAdmin, that should be the expected output.
     *
     */
    @Test
    public void testCustomHAMQualifierPriorityOutput() throws Exception {

        // Mark the trace before making HTTP connection and execute request
        executeGetRequestWithTraceMark(url, 200);

        assertStringInTrace("Output message for custom prioritization on inbuilt and custom HAMs with qualifiers not found",
                            Jakartasec40TestConstants.HAM_CUSTOM_HANDLER_NO_TESTER_PRIORITY_MESSAGE);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        SingleHAMInbuiltCustomQualifierTests instance = new SingleHAMInbuiltCustomQualifierTests();
        instance.stopServer();
    }

}
