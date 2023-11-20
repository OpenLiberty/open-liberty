/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.jaxws.suite.FATSuite;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * This is to test the @WebServiceRef annotation works in the JAX-WS client
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class WebServiceRefTest {

    private static final String SERVER_NAME = "WebServiceRefTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;
    private static String BASE_URL;
    private static final int CONN_TIMEOUT = 5;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "helloClient", "io.openliberty.checkpoint.testapp.jaxws.wsr.client",
                                      "io.openliberty.checkpoint.testapp.jaxws.wsr.server.stub",
                                      "io.openliberty.checkpoint.jaxws.fat.util");
        ShrinkHelper.defaultDropinApp(server, "helloClientDDMerge", "io.openliberty.checkpoint.testapp.jaxws.wsr.clientddmerge",
                                      "io.openliberty.checkpoint.testapp.jaxws.wsr.server.stub",
                                      "io.openliberty.checkpoint.jaxws.fat.util");
        ShrinkHelper.defaultDropinApp(server, "helloClientServiceResource", "io.openliberty.checkpoint.testapp.jaxws.wsr.clientserviceresource",
                                      "io.openliberty.checkpoint.testapp.jaxws.wsr.server.stub",
                                      "io.openliberty.checkpoint.jaxws.fat.util");
        ShrinkHelper.defaultDropinApp(server, "helloServer", "io.openliberty.checkpoint.testapp.jaxws.wsr.server",
                                      "io.openliberty.checkpoint.testapp.jaxws.wsr.server.impl",
                                      "io.openliberty.checkpoint.jaxws.fat.util");

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                             server -> {
                                 assertNotNull("'CWWKZ0001I: ' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I.*helloServer", 0));
                                 assertNotNull("'CWWKZ0001I: ' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I.*helloClient", 0));
                                 assertNotNull("'CWWKZ0001I: ' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I.*helloClientDDMerge", 0));
                                 assertNotNull("'CWWKZ0001I: ' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I.*helloClientServiceResource", 0));
                             });
        server.startServer("WebServiceRefTest.log");

        BASE_URL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * TestDescription: Using WebServiceRef annotation in client to reference JAX-WS Service or Port type instance.
     * Condition:
     * - Client servlets uses @WebServiceRef annotation
     * - No <service-ref> in web.xml
     * Result:
     * - response contains "Hello World"
     */
    @Test
    public void testWebServiceRefAnnoatation() throws Exception {
        String clientUrlStr = BASE_URL + "/helloClient";

        checkClientHelloWorld(clientUrlStr, "ServiceInjectionNormalServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionObjectMemberServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionObjectTypeServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionServiceMemberServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionServiceTypeServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionMultiTargetsServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionClassLevelServlet");

        checkClientHelloWorld(clientUrlStr, "PortTypeInjectionNormalServlet");
        checkClientHelloWorld(clientUrlStr, "PortTypeInjectionObjectTypeServlet");
        checkClientHelloWorld(clientUrlStr, "PortTypeInjectionClassLevelServlet");

    }

    /**
     * TestDescription: Using WebServiceRef annotation along with deployment descriptor in client to reference JAX-WS Service or Port type instance.
     * Condition:
     * - Client servlets uses @WebServiceRef annotation, but the information is not complete.
     * - Define <service-ref> in web.xml
     * Result:
     * - response contains "Hello World"
     */
    @Test
    public void testWebServiceRefDDMerge() throws Exception {
        String clientUrlStr = BASE_URL + "/helloClientDDMerge";

        checkClientHelloWorld(clientUrlStr, "ServiceInjectionClassLevelServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionMemberLevelServlet");

        checkClientHelloWorld(clientUrlStr, "PortTypeInjectionClassLevelServlet");
        checkClientHelloWorld(clientUrlStr, "PortTypeInjectionMemberLevelServlet");

    }

    /**
     * TestDescription: Using Resource annotation along with deployment descriptor in client to reference JAX-WS Service instance.
     * Condition:
     * - Client servlets uses @Resource annotation to reference a JAX-WS Service.
     * - Define <service-ref> in web.xml
     * Result:
     * - response contains "Hello World"
     */
    @Test
    public void testWebServiceRefServiceResource() throws Exception {
        String clientUrlStr = BASE_URL + "/helloClientServiceResource";

        checkClientHelloWorld(clientUrlStr, "ServiceInjectionClassLevelServlet");
        checkClientHelloWorld(clientUrlStr, "ServiceInjectionMemberLevelServlet");

    }

    private void checkClientHelloWorld(String clientUrlStr, String servletName) throws Exception {
        URL url = new URL(clientUrlStr + "/" + servletName + "?target=World");
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        assertTrue("The Web service can not be invoked successfully from " + clientUrlStr + "/" + servletName, line.contains("Hello World"));
    }

}
