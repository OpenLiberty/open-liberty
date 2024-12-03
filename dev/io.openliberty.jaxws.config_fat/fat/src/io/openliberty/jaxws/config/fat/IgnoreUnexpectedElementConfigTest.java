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
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This test suite tests the <webServiceClient ignoreUnkownElements="true/false" /> server.xml configuration.
 *
 * The Web Service is defined in the testHandlerProvider application, which uses a handler to mimic a new version of a
 * Web Service implementation by adding an additional element in the response of the Web Service.
 *
 * Since the client, SayHelloService, is an "older" version, it is unaware of the additional element and our JAX-WS implementation by default
 * will throw an unknown element exception. The configuration allows a user to ignore the additional elements and successfully marshall the response.
 *
 * The tests cover a variety of configurations, with "global" client configuration that applies to all web service clients, clients with a specified
 * serviceName, clients with both global and serviceName defined to test precedence (serviceName overrides global), or mismatching serviceNames that will not
 * apply configuration to the client being invoked.
 *
 * TODO: Test Dispatch clients
 */
@RunWith(FATRunner.class)
public class IgnoreUnexpectedElementConfigTest {

    Logger LOG = Logger.getLogger("IgnoreUnexpectedElementConfigTest.class");


    // Max timeout set to 5 minutes in milliseconds
    private final static int REQUEST_TIMEOUT = 300000;

    @Server("IgnoreUnexpectedElementConfigTestServer")
    public static LibertyServer addedElementServer;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(addedElementServer, "testWebServiceClient", "io.openliberty.samples.jaxws.client",
                                      "io.openliberty.samples.jaxws.client.servlet", "io.openliberty.jaxws.fat.mock.endpoint");

        addedElementServer.startServer("IgnoreUnexpectedElementConfigTest.log");

        assertNotNull("Application hello does not appear to have started.", addedElementServer.waitForStringInLog("CWWKZ0001I:.*" + "testWebServiceClient"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (addedElementServer != null && addedElementServer.isStarted()) {
            addedElementServer.stopServer();
        }
    }

    /**
     * Test the global setting of ignoreUnknownElement=true in server.xml
     *
     * Server config - global-true-server.xml
     * Expected response - Hello,AddedElement
     *
     * @throws Exception
     */
    @Test
    public void testGlobalIgnoreUnexpectedElementTrue() throws Exception {
        addedElementServer.reconfigureServer("IgnoreUnexpectedElementConfigTestServer/global-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/TestServiceServlet?target=user
        String response = runTest(addedElementServer, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testGlobalIgnoreUnexpectedElementTrue - Response = " + response);

        assertTrue("Expected successful response, but response was " + response,
                   response.contains("Hello, AddedElement"));
    }

    /**
     * Test the global setting of ignoreUnknownElement=false in server.xml
     *
     * Server config - global-false-server.xml
     * Expected response - Unmarshalling Error: unexpected element
     *
     * @throws Exception
     */
    @Test
    public void testGlobalIgnoreUnexpectedElementFalse() throws Exception {
        addedElementServer.reconfigureServer("IgnoreUnexpectedElementConfigTestServer/global-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/TestServiceServlet?target=user
        String response = runTest(addedElementServer, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testGlobalIgnoreUnexpectedElementFalse - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: unexpected element exception is not thrown",
                      addedElementServer.waitForStringInLog("xml.ws.soap.SOAPFaultException: Unmarshalling Error: unexpected element "));
    }

    /**
     * Test the serviceName with ignoreUnknownElement=true in server.xml
     *
     * Server config - servicename-true-server.xml
     * Expected response - Hello,AddedElement
     *
     * @throws Exception
     */
    @Test
    public void testServiceNameIgnoreUnexpectedElementTrue() throws Exception {
        addedElementServer.reconfigureServer("IgnoreUnexpectedElementConfigTestServer/servicename-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/TestServiceServlet?target=user
        String response = runTest(addedElementServer, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testServiceNameIgnoreUnexpectedElementTrue - Response = " + response);

        assertTrue("Expected successful response, but response was " + response,
                   response.contains("Hello, AddedElement"));
    }

    /**
     * Test the serviceName with ignoreUnknownElement=true in server.xml
     *
     * Server config - servicename-false-server.xml
     * Expected response - Unmarshalling Error: unexpected element
     *
     * @throws Exception
     */
    @Test
    public void testServiceNameIgnoreUnexpectedElementFalse() throws Exception {
        addedElementServer.reconfigureServer("IgnoreUnexpectedElementConfigTestServer/servicename-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/TestServiceServlet?target=user
        String response = runTest(addedElementServer, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testServiceNameIgnoreUnexpectedElementFalse - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: unexpected element exception is not thrown",
                      addedElementServer.waitForStringInLog("xml.ws.soap.SOAPFaultException: Unmarshalling Error: unexpected element "));
    }

    /**
     * Test the serviceName and global configs with ignoreUnknownElement=true in server.xml
     *
     * Server config - servicename-true-global-true-server.xml
     * Expected response - Hello,AddedElement
     *
     * @throws Exception
     */
    @Test
    public void testServiceNameTrueAndGlobalTrueIgnoreUnexpectedElement() throws Exception {
        addedElementServer.reconfigureServer("IgnoreUnexpectedElementConfigTestServer/servicename-true-global-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/TestServiceServlet?target=user
        String response = runTest(addedElementServer, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testServiceNameIgnoreUnexpectedElementTrue - Response = " + response);

        assertTrue("Expected successful response, but response was " + response,
                   response.contains("Hello, AddedElement"));
    }

    /**
     * Test the serviceName and global configs with ignoreUnknownElement=false in server.xml
     *
     * Server config - servicename-false-global-false-server.xml
     * Expected response - Unmarshalling Error: unexpected element
     *
     * @throws Exception
     */
    @Test
    public void testServiceNameFalseAndGlobalFalseIgnoreUnexpectedElement() throws Exception {
        addedElementServer.reconfigureServer("IgnoreUnexpectedElementConfigTestServer/servicename-false-global-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/TestServiceServlet?target=user
        String response = runTest(addedElementServer, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testServiceNameIgnoreUnexpectedElementFalse - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: unexpected element exception is not thrown",
                      addedElementServer.waitForStringInLog("xml.ws.soap.SOAPFaultException: Unmarshalling Error: unexpected element "));
    }

    /**
     * Test the serviceName and global configs with mis-mataching ignoreUnknownElement elements in server.xml
     *
     * Server config - servicename-true-global-false-server.xml
     * Expected response - Hello,AddedElement
     *
     * @throws Exception
     */
    @Test
    public void testServiceNameTrueAndGlobalFalseIgnoreUnexpectedElement() throws Exception {
        addedElementServer.reconfigureServer("IgnoreUnexpectedElementConfigTestServer/servicename-true-global-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/TestServiceServlet?target=user
        String response = runTest(addedElementServer, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testServiceNameIgnoreUnexpectedElementTrue - Response = " + response);

        assertTrue("Expected successful response, but response was " + response,
                   response.contains("Hello, AddedElement"));
    }

    /**
     * Test the serviceName and global configs with mis-mataching ignoreUnknownElement elements in server.xml
     *
     * Server config - servicename-false-global-true-server.xml
     * Expected response - Unmarshalling Error: unexpected element
     *
     * @throws Exception
     */
    @Test
    public void testServiceNameFalseAndGlobalTrueIgnoreUnexpectedElement() throws Exception {
        addedElementServer.reconfigureServer("IgnoreUnexpectedElementConfigTestServer/servicename-false-global-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/TestServiceServlet?target=user
        String response = runTest(addedElementServer, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testServiceNameIgnoreUnexpectedElementFalse - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: unexpected element exception is not thrown",
                      addedElementServer.waitForStringInLog("xml.ws.soap.SOAPFaultException: Unmarshalling Error: unexpected element "));
    }

    /**
     * Test an incorrect serviceName attribute doesn't apply the config for ignoreUnexpectedElements
     *
     * Server config - incorrect-servicename-server.xml
     * Expected response - Unmarshalling Error: unexpected element
     *
     * @throws Exception
     */
    @Test
    public void testIncorrectServiceNameIgnoreUnexpectedElement() throws Exception {
        addedElementServer.reconfigureServer("IgnoreUnexpectedElementConfigTestServer/incorrect-servicename-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/TestServiceServlet?target=user
        String response = runTest(addedElementServer, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testServiceNameIgnoreUnexpectedElementFalse - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: unexpected element exception is not thrown",
                      addedElementServer.waitForStringInLog("xml.ws.soap.SOAPFaultException: Unmarshalling Error: unexpected element "));
    }

    private String runTest(LibertyServer server, String pathAndParams) throws ProtocolException, IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + pathAndParams);
        LOG.info("Calling Application with URL=" + url.toString());

        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        return line;
    }
}
