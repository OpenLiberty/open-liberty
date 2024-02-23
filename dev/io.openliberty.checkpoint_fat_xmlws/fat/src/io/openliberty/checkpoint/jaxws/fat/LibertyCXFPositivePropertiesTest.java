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
package io.openliberty.checkpoint.jaxws.fat;

import static io.openliberty.checkpoint.jaxws.suite.FATSuite.configureEnvVariable;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import io.openliberty.checkpoint.jaxws.fat.util.ExplodedShrinkHelper;
import io.openliberty.checkpoint.jaxws.fat.util.TestUtils;
import io.openliberty.checkpoint.jaxws.suite.FATSuite;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.checkpoint.testapp.jaxws.props.servlet.LibertyCXFPositivePropertiesTestServlet;

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
 * io.openliberty.checkpoint.testapp.jaxws.props.service.ImageService
 * io.openliberty.checkpoint.testapp.jaxws.props.service.ImageServiceTwo
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
@CheckpointTest
public class LibertyCXFPositivePropertiesTest {

    public static final String APP_NAME = "libertyCXFProperty";

    private static final String SERVER_NAME = "LibertyCXFPositivePropertiesTestServer";

    @Server(SERVER_NAME)
    @TestServlet(servlet = LibertyCXFPositivePropertiesTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        ExplodedShrinkHelper.explodedApp(server, APP_NAME, "io.openliberty.checkpoint.testapp.jaxws.props.client.stub",
                                         "io.openliberty.checkpoint.testapp.jaxws.props.service",
                                         "io.openliberty.checkpoint.testapp.jaxws.props.servlet");

        TestUtils.publishFileToServer(server,
                                      "LibertyCXFPropertiesTest", "service-image.wsdl",
                                      "apps/libertyCXFProperty.war/WEB-INF/wsdl", "service-image.wsdl");

        TestUtils.publishFileToServer(server,
                                      "LibertyCXFPropertiesTest", "client-image.wsdl",
                                      "apps/libertyCXFProperty.war/WEB-INF/wsdl", "image.wsdl");

        // For EE10, we test all the properties tested in the other repeats plus the additional Woodstox configuration property
        if (JakartaEEAction.isEE10OrLaterActive()) {
            server.getServerBootstrapPropertiesFile().delete();
            server.getServerBootstrapPropertiesFile()
                            .copyFromSource(new RemoteFile(server.getMachine(), server.pathToAutoFVTTestFiles
                                                                                + "/LibertyCXFPropertiesTest/woodstox-true-bootstrap.properties"),
                                            false,
                                            true);
        }

        CheckpointInfo checkpointInfo = new CheckpointInfo(CheckpointPhase.AFTER_APP_START, true, //
                        server -> {
                            assertNotNull("'CWWKZ0001I: ' message not found in log.",
                                          server.waitForStringInLogUsingMark("CWWKZ0001I:.*libertyCXFProperty", 0));
                            if (JakartaEEAction.isEE10OrLaterActive()) {
                                // Woodstox StAX provider is disabled for these tests, assert disabling it is shown in logs.
                                assertNotNull("The org.apache.cxf.stax.allowInsecureParser property failed to disable the Woodstox StAX Provider",
                                              server.waitForStringInTraceUsingMark("The System Property `org.apache.cxf.stax.allowInsecureParser` is set, using JRE's StAX Provider"));

                            }
                            configureEnvVariable(server, Collections.singletonMap("TEST_HOST", "localhost"));
                        });

        // TODO need to fix the runtime to not do an application restart when configuring webservices-bnd.
        // Decided NOT to make any changes to runtime. Maintain the current behavior of restarting the application when a change in the webservices-bnd configuration is detected(https://github.com/OpenLiberty/open-liberty/issues/27089). 
        checkpointInfo.setAssertNoAppRestartOnRestore(false);

        server.setCheckpoint(checkpointInfo);
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

        if (server != null && server.isStarted()) {
            server.stopServer("CWWKO0801E");
        }
    }

}
