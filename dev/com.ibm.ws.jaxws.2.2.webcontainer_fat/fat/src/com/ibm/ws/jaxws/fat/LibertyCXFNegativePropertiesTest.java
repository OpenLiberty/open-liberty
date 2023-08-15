/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.jaxws.fat.util.ExplodedShrinkHelper;
import com.ibm.ws.jaxws.fat.util.TestUtils;
import com.ibm.ws.properties.test.servlet.LibertyCXFNegativePropertiesTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/*
 * Positive tests checking behavior changes after 2 property settings
 * Details are on top of test methods
 *
 * Usage of waitForStringInTraceUsingMark cut the runtime significantly
 */
@RunWith(FATRunner.class)
public class LibertyCXFNegativePropertiesTest {

    public static final String APP_NAME = "libertyCXFProperty";

    @Server("LibertyCXFNegativePropertiesTestServer")
    @TestServlet(servlet = LibertyCXFNegativePropertiesTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ExplodedShrinkHelper.explodedApp(server, APP_NAME, "com.ibm.ws.properties.test.client.stub",
                                         "com.ibm.ws.properties.test.service",
                                         "com.ibm.ws.properties.test.servlet");

        TestUtils.publishFileToServer(server,
                                      "LibertyCXFPropertiesTest", "service-image.wsdl",
                                      "apps/libertyCXFProperty.war/WEB-INF/wsdl", "service-image.wsdl");

        TestUtils.publishFileToServer(server,
                                      "LibertyCXFPropertiesTest", "client-image.wsdl",
                                      "apps/libertyCXFProperty.war/WEB-INF/wsdl", "image.wsdl");

        server.startServer("LibertyCXFPropertiesTest.log");

        server.waitForStringInLog("CWWKF0011I");

        assertNotNull("SSL service needs to be started for tests, but the HTTPS was never started", server.waitForStringInLog("CWWKO0219I.*ssl"));
    }

    @AfterClass
    public static void tearDown() throws Exception {

        // @Test = testCxfUnsupportedPolicyProperty()
        assertNotNull("Since cxf.ignore.unsupported.policy is not enabled, invalid alternative policies are not supported",
                      server.waitForStringInLog("None of the policy alternatives can be satisfied"));

        assertNull("Since cxf.ignore.unsupported.policy is not enabled, Unsupported policy assertions won't be ignored",
                   server.waitForStringInTraceUsingMark("WARNING: Unsupported policy assertions will be ignored"));

        // @Test = testCxfUsedAlternativePolicyProperty()
        assertNotNull("Since cxf.ignore.unsupported.policy is not enabled, used alternative policies are not put as alternatives",
                      server.waitForStringInTraceUsingMark("Verified policies for inbound message"));

        assertNull("Since cxf.ignore.unsupported.policy is not enabled, checkEffectivePolicy will be called",
                   server.waitForStringInTraceUsingMark("WARNING: checkEffectivePolicy will not be called"));

        // @Test = testCxfAttachmentOutputProperty()
        assertNotNull("Since cxf.multipart.attachment is not enabled, ",
                      server.waitForStringInTraceUsingMark("--uuid:"));

        if (server != null && server.isStarted()) {
            // Ignore different SSL connection errors for negative test cases
            server.stopServer("SRVE0777E", "SRVE0315E", "CWWKO0801E");
        }
    }
}
