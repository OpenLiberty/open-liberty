/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.jaxws.fat.util.ExplodedShrinkHelper;
import com.ibm.ws.jaxws.fat.util.TestUtils;
import com.ibm.ws.properties.test.servlet.LibertyCXFPositivePropertiesTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/*
 * Positive tests checking behavior changes after 2 property settings
 * Details are on top of test methods
 *
 * Usage of waitForStringInTraceUsingMark cut the runtime significantly
 *
 * Due to consistent timeouts between the test harness and the Liberty server,
 * the test methods have been moved to LibertyCXFPositivePropertiesTestServlet
 *
 * Since the assertions need the logs though, the asserts are checked at test tear down.
 * There are three test cases, and each test generates different messages in the trace.
 *
 * The individual tests are in the LibertyCXFPositivePropertiesTestServlet class.
 * The properties tested are set in the LibertyCXFPositivePropertiesTestServer/bootstrap.property file
 *
 * The tests use these Web Services:
 *
 * com.ibm.ws.properties.test.service.ImageService
 * com.ibm.ws.properties.test.service.ImageServiceTwo
 *
 * The tests also use these client stubs:
 *
 * com.ibm.ws.test.client.stub
 *
 * Properties checked by this Test Suite:
 *
 * cxf.multipart.attachment
 * cxf.ignore.unsupported.policy
 */
@RunWith(FATRunner.class)
public class LibertyCXFPositivePropertiesTest {

    public static final String APP_NAME = "libertyCXFProperty";

    @Server("LibertyCXFPositivePropertiesTestServer")
    @TestServlet(servlet = LibertyCXFPositivePropertiesTestServlet.class, contextRoot = APP_NAME)
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

        // For EE10, we test all the properties tested in the other repeats plus the additional Woodstox configuration property
        if (JakartaEEAction.isEE10OrLaterActive()) {
            server.getServerBootstrapPropertiesFile().delete(); // In the next line we are forcing overwrite, however, we are deleting here to be sure.
            server.getServerBootstrapPropertiesFile().copyFromSource(new RemoteFile(server.getMachine(), server.pathToAutoFVTTestFiles
                                                                                                         + "/LibertyCXFPropertiesTest/woodstox-true-bootstrap.properties"),
                                                                     false,
                                                                     true);
        }

        server.startServer("LibertyCXFPropertiesTest.log");

        server.waitForStringInLog("CWWKF0011I");

        assertNotNull("SSL service needs to be started for tests, but the HTTPS was never started", server.waitForStringInLog("CWWKO0219I.*ssl"));
    }

    @AfterClass
    public static void tearDown() throws Exception {

        // @Test = testCxfPropertyAttachmentOutputPolicy()
        assertNotNull("The test testCxfPropertyAttachmentOutputPolicy() failed, and 'cxf.multipart.attachment' was not configured",
                      server.waitForStringInTraceUsingMark("skipAttachmentOutput: getAttachments returned"));

        // @Test = testCxfPropertyUnsupportedPolicy()
        assertNotNull("The test testCxfPropertyUnsupportedPolicy() failed, and 'cxf.ignore.unsupported.policy' was not configured",
                      server.waitForStringInTraceUsingMark("WARNING: checkEffectivePolicy will not be called"));

        // @Test = testCxfPropertyUsedAlternativePolicy()
        assertNotNull("The test testCxfPropertyUsedAlternativePolicy failed, and 'cxf.ignore.unsupported.policy' was not configured",
                      server.waitForStringInTraceUsingMark("WARNING: Unsupported policy assertions will be ignored"));

        if (JakartaEEAction.isEE10OrLaterActive()) {
            // Woodstox StAX provider is disabled for these tests, assert disabling it is shown in logs.
            assertNotNull("The org.apache.cxf.stax.allowInsecureParser property failed to disable the Woodstox StAX Provider",
                          server.waitForStringInTraceUsingMark("The System Property `org.apache.cxf.stax.allowInsecureParser` is set, using JRE's StAX Provider"));

        }

        if (server != null && server.isStarted()) {
            server.stopServer("CWWKO0801E");
        }
    }

}
