/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jaxws.config.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This test suite tests the following variations of the <webServiceClient enableDefaultValidation="true/false" /> server.xml configuration:
 *
 * 1.) enableDefaultalidation="true" - "global" setting - wrong element content
 * 3.) enableSchemValidation="true" serviceName="SayHelloService" - targeted client setting - wrong element content
 *
 * This Test uses a Mock endpoint to create "bad" responses that the client will fail to unmarshall unless enableDefaultValidation=false
 *
 * @see io.openliberty.jaxws.fat.mock.endpoint.MockJaxwsEndpointServlet
 *
 *      Each test uses a specific server.xml configuration that's reconfigured at test start. It calls the @see IgnoreUnexpectedElementTestServiceServlet servlet
 *      along with a HTTP request parameter. That parameter is added to the SOAP Request, and when the mock JAX-WS endpoint receives the request, it checks for the string
 *      and correlates it to the "bad" response the endpoint then returns. After the client receives the inbound response, the test checks the response from the
 *      IgnoreUnexpectedElementTestServiceServlet servlet
 *      for the specified response or exception in the server logs.
 *
 *      TODO: Test Dispatch clients
 */
@RunWith(FATRunner.class)
public class EnableDefaultValidationTest {

    Logger LOG = Logger.getLogger("EnableDefaultValidationTest.class");

    // Max timeout set to 5 minutes in milliseconds
    private final static int REQUEST_TIMEOUT = 300000;

    @Server("EnableDefaultValidationTestServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
    	
        ShrinkHelper.defaultDropinApp(server, "testWebServiceClient", "io.openliberty.samples.jaxws.client",
                                      "io.openliberty.samples.jaxws.client.handler",
                                      "io.openliberty.samples.jaxws.client.servlet", "io.openliberty.jaxws.fat.mock.endpoint");

        server.startServer("EnableDefaultValidationTest.log");

        assertNotNull("Application hello does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + "testWebServiceClient"));
    }

    /**
     * Have to reconfig to the initial server.xml after each test, since each configuration
     * might be repeated across multiple tests
     *
     * @throws Exception
     */
    @After
    public void reconfigOnAfter() throws Exception {
        server.reconfigureServer("EnableDefaultValidationTestServer/server.xml", "CWWKG0017I");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    // Global True Tests

    /**
     * Test the global setting of enableSchemaValidation=true in server.xml, with the wrong content in the <response> element
     * The response will contain an additional child element <WrongElementContent> which will cause an Unexpected Element exception
     * with the default schema validation enabled.
     *
     * Server config - global-true-server.xml
     * Expected response - Unmarshalling Error: unexpected element
     *
     * @throws Exception
     */
    @Test
    public void testWrongElementContentGlobalDefaultValidationTrue() throws Exception {
        server.reconfigureServer("EnableDefaultValidationTestServer/global-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=WrongElementContent");
        // Log response to output.txt
        LOG.info("testWrongElementContentGlobalDefaultValidationTrue - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: Expected elements are (none) in server logs",
                      server.waitForStringInLog("local:\"WrongElementContent\""));
    }

    // GLOBAL FALSE TESTS

    /**
     * Test the targeted setting of enableDefaultValidation=true in server.xml, with the wrong content in the <response> element
     * The response will contain an additional child element <WrongElementContent> which will cause an Unexpected Element exception
     * with the default schema validation enabled.
     *
     * Server config - servicename-true-server.xml
     * Expected response - Unmarshalling Error: unexpected element
     *
     * @throws Exception
     */
    @Test
    public void testWrongElementContentServiceNameDefaultValidationTrue() throws Exception {
        server.reconfigureServer("EnableDefaultValidationTestServer/servicename-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=WrongElementContent");
        // Log response to output.txt
        LOG.info("testWrongElementContentServiceNameDefaultValidationTrue - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: WrongElementContent is not found in server logs",
                      server.waitForStringInLog("local:\"WrongElementContent\""));
    }
    
    
    private String runTest(LibertyServer server, String pathAndParams) throws ProtocolException, IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + pathAndParams);
        Log.info(this.getClass(), "assertResponseNotNull", "Calling Application with URL=" + url.toString());

        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        return line;
    }
}
