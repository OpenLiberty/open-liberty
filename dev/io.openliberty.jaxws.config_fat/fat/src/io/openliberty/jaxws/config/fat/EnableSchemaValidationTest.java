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
 * This test suite tests the following variations of the <webServiceClient enableSchemaValidation="true/false" /> server.xml configuration:
 *
 * 1.) enableSchemValidation="true" - "global" setting
 * 2.) enableschemaValiation="false" - "global" setting
 * 3.) enableSchemValidation="true" serviceName="SayHelloService" - targeted client setting
 * 4.) enableSchemValidation="false" serviceName="SayHelloService" - targeted client setting
 * 5.) Combination of 1 and 4
 * 6.) Combination of 2 and 3
 * 7.) enableSchemValidation="false" serviceName="IncorrectServiceName" - targeted client setting with the wrong serviceName
 *
 * This Test uses a Mock endpoint to create "bad" responses that the client will fail to unmarshall unless enableSchemaValidation=false
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
public class EnableSchemaValidationTest {

    Logger LOG = Logger.getLogger("EnableSchemaValidationTest.class");
    

    private static final String APP_NAME = "testWebServiceClient";

    // Max timeout set to 5 minutes in milliseconds
    private final static int REQUEST_TIMEOUT = 300000;

    @Server("EnableSchemaValidationTestServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
    	
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "io.openliberty.samples.jaxws.client",
                                      "io.openliberty.samples.jaxws.client.handler",
                                      "io.openliberty.samples.jaxws.client.servlet", "io.openliberty.jaxws.fat.mock.endpoint");

        server.startServer("EnableSchemaValidationTest.log");

        assertNotNull("Application " + APP_NAME + " does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
    
    /**
     * Have to reconfig to the initial server.xml after each test, since each configuration
     * might be repeated across multiple tests
     *
     * @throws Exception
     */
    @After
    public void reconfigOnAfter() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/server.xml", "CWWKG0017I");
    }

    
    // Global True Tests

    /**
     * Test the global setting of enableSchemaValidation=true in server.xml, with an additional element <arg0> following the <response> element
     * which will cause s No child element is expected at this point exception with the default schema validation enabled.
     *
     * Server config - global-true-server.xml
     * Expected response - Unmarshalling Error: cvc-complex-type.2.4.a
     *
     * @throws Exception
     */
    @Test
    public void testAddedElementGlobalSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/global-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testAddedElementGlobalSchemaValidationTrue - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: unexpected element exception is not thrown",
                      server.waitForStringInLog("No child element is expected at this point."));
    }

    /**
     * Test the global setting of enableSchemaValidation=true in server.xml, with the wrong element name where <response> element is expected
     * which will cause an cvc-complex-type.2.4.a exception with the default schema validation enabled.
     *
     * Server config - global-true-server.xml
     * Expected response - Unmarshalling Error: cvc-complex-type.2.4.a
     *
     * @throws Exception
     */
    @Test
    public void testWrongElementNameGlobalSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/global-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=WrongElementName");
        // Log response to output.txt
        LOG.info("testWrongElementNameGlobalSchemaValidationTrue - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: cvc-complex-type.2.4.a  in server logs",
                      server.waitForStringInLog("cvc-complex-type.2.4.a"));
    }

    /**
     * Test the global setting of enableSchemaValidation=true in server.xml, with the wrong namespace in the <response> element
     * which will cause an cvc-complex-type.2.4.a exception with the default schema validation enabled.
     *
     * Server config - global-true-server.xml
     * Expected response - Unmarshalling Error: cvc-complex-type.2.4.a
     *
     * @throws Exception
     */
    @Test
    public void testWrongNamespaceGlobalSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/global-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=WrongNamespace");
        // Log response to output.txt
        LOG.info("testWrongNamespaceGlobalSchemaValidationTrue - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: cvc-complex-type.2.4.a  in server logs",
                      server.waitForStringInLog("cvc-complex-type.2.4.a"));
    }

    /**
     * Test the global setting of enableSchemaValidation=true in server.xml, with the wrong content in the <response> element
     * The response will contain an addtional child element <WrongElementContent> which will cause an Unexected Element exception
     * with the default schema validation enabled.
     *
     * Server config - global-true-server.xml
     * Expected response - Unmarshalling Error: unexpected element
     *
     * @throws Exception
     */
    @Test
    public void testWrongElementContentGlobalSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/global-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=WrongElementContent");
        // Log response to output.txt
        LOG.info("testWrongElementContentGlobalSchemaValidationTrue - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: Expected elements are (none) in server logs",
                      server.waitForStringInLog("local:\"WrongElementContent\""));
    }

    // GLOBAL FALSE TESTS

    /**
     * Test the global setting of enableSchemaValidation=false in server.xml, with an additional element <arg0> following the <response> element
     * which is ignored when schema validation is disabled.
     *
     * Server config - global-false-server.xml
     * Expected response - Hello, AddedElement
     *
     * @throws Exception
     */
    @Test
    public void testAddedElementGlobalSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/global-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testAddedElementGlobalSchemaValidationFalse - Response = " + response);

        assertTrue("Expected successful response, but response was " + response,
                   response.contains("Hello, AddedElement"));
    }

    /**
     * Test the global setting of enableSchemaValidation=false in server.xml, with the wrong element name where <response> element is expected
     * which with schema valdation disabled will just be ignored and the response will be null when unmarshalled
     *
     * Server config - global-false-server.xml
     * Expected response - null
     *
     * @throws Exception
     */
    @Test
    public void testWrongElementNameGlobalSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/global-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=WrongElementName");
        // Log response to output.txt
        LOG.info("testWrongElementNameGlobalSchemaValidationFalse - Response = " + response);

        assertTrue("Expected successful response, but response was " + response,
                   response.contains("null"));
    }

    /**
     * Test the global setting of enableSchemaValidation=false in server.xml, with the wrong namespace set on <response> element
     * which with schema valdation disabled will just be ignored and the response will be null when unmarshalled
     *
     * Server config - global-false-server.xml
     * Expected response - null
     *
     * @throws Exception
     */
    @Test
    public void testWrongNamespaceGlobalSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/global-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=WrongNamespace");
        // Log response to output.txt
        LOG.info("testWrongNamespaceGlobalSchemaValidationFalse - Response = " + response);

        assertTrue("Expected successful response, but response was " + response,
                   response.contains("null"));
    }

    /**
     * Test the global setting of enableSchemaValidation=false in server.xml, with wrong child elements of the <response> element
     * which with schema valdation disabled will just be ignored and the response will be empty since response no longer contains a valid value.
     *
     * Server config - global-false-server.xml
     * Expected response - null
     *
     * @throws Exception
     */
    @Test
    public void testWrongElementContentGlobalSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/global-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=WrongElementContent");
        // Log response to output.txt
        LOG.info("testWrongElementContentGlobalSchemaValidationFalse - Response = " + response);

        assertTrue("Expected successful response, but response was " + response,
                   response.contains(": "));
    }

    // serviceName True Tests

    /**
     * Test the targeted setting of enableSchemaValidation=true in server.xml, with an additional element <arg0> following the <response> element
     * which will cause s No child element is expected at this point exception with the default schema validation enabled.
     *
     * Server config - servicename-true-server.xml
     * Expected response - Unmarshalling Error: No child element is expected at this point.
     *
     * @throws Exception
     */
    @Test
    public void testAddedElementServiceNameSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/servicename-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testAddedElementServiceNameSchemaValidationTrue - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: No child element is expected at this point.",
                      server.waitForStringInLog("No child element is expected at this point."));
    }

    /**
     * Test the targeted setting of enableSchemaValidation=true in server.xml, with the wrong element name where <response> element is expected
     * which will cause an cvc-complex-type.2.4.a exception with the default schema validation enabled.
     *
     * Server config - servicename-true-server.xml
     * Expected response - Unmarshalling Error: cvc-complex-type.2.4.a
     *
     * @throws Exception
     */
    @Test
    public void testWrongElementNameServiceNameSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/servicename-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=WrongElementName");
        // Log response to output.txt
        LOG.info("testWrongElementNameServiceNameSchemaValidationTrue - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: cvc-complex-type.2.4.a not found in server logs",
                      server.waitForStringInLog("cvc-complex-type.2.4.a"));
    }

    /**
     * Test the targeted setting of enableSchemaValidation=true in server.xml, with the wrong namespace in the <response> element
     * which will cause an cvc-complex-type.2.4.a exception with the default schema validation enabled.
     *
     * Server config - servicename-true-server.xml
     * Expected response - Unmarshalling Error: cvc-complex-type.2.4.a
     *
     * @throws Exception
     */
    @Test
    public void testWrongNamespaceServiceNameSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/servicename-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=WrongNamespace");
        // Log response to output.txt
        LOG.info("testWrongNamespaceServiceNameSchemaValidationTrue - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: cvc-complex-type.2.4.a not found in server logs",
                      server.waitForStringInLog("cvc-complex-type.2.4.a"));
    }

    /**
     * Test the targeted setting of enableSchemaValidation=true in server.xml, with the wrong content in the <response> element
     * The response will contain an addtional child element <WrongElementContent> which will cause an Unexected Element exception
     * with the default schema validation enabled.
     *
     * Server config - servicename-true-server.xml
     * Expected response - Unmarshalling Error: unexpected element
     *
     * @throws Exception
     */
    @Test
    public void testWrongElementContentServiceNameSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/servicename-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=WrongElementContent");
        // Log response to output.txt
        LOG.info("testWrongElementContentServiceNameSchemaValidationTrue - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: WrongelementContent is not found in server logs",
                      server.waitForStringInLog("local:\"WrongElementContent\""));
    }

    // serviceName FALSE TESTS

    /**
     * Test the targeted setting of enableSchemaValidation=false in server.xml, with an additional element <arg0> following the <response> element
     * which is ignored when schema validation is disabled.
     *
     * Server config - servicename-false-server.xml
     * Expected response - Hello, AddedElement
     *
     * @throws Exception
     */
    @Test
    public void testAddedElementServiceNameSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/servicename-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testAddedElementServiceNameSchemaValidationFalse - Response = " + response);

        assertTrue("Expected successful response, but response was " + response,
                   response.contains("Hello, AddedElement"));
    }

    /**
     * Test the targeted setting of enableSchemaValidation=false in server.xml, with the wrong element name where <response> element is expected
     * which with schema valdation disabled will just be ignored and the response will be null when unmarshalled
     *
     * Server config - servicename-false-server.xml
     * Expected response - null
     *
     * @throws Exception
     */
    @Test
    public void testWrongElementNameServiceNameSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/servicename-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=WrongElementName");
        // Log response to output.txt
        LOG.info("testWrongElementNameServiceNameSchemaValidationFalse - Response = " + response);

        assertTrue("Expected successful response, but response was " + response,
                   response.contains("null"));
    }

    /**
     * Test the targeted setting of enableSchemaValidation=false in server.xml, with the wrong namespace set on <response> element
     * which with schema valdation disabled will just be ignored and the response will be null when unmarshalled
     *
     * Server config - servicename-false-server.xml
     * Expected response - null
     *
     * @throws Exception
     */
    @Test
    public void testWrongNamespaceServiceNameSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/servicename-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=WrongNamespace");
        // Log response to output.txt
        LOG.info("testWrongNamespaceServiceNameSchemaValidationFalse - Response = " + response);

        assertTrue("Expected successful response, but response was " + response,
                   response.contains("null"));
    }

    /**
     * Test the targeted setting of enableSchemaValidation=false in server.xml, with wrong child elements of the <response> element
     * which with schema valdation disabled will just be ignored and the response will be empty since response no longer contains a valid value.
     *
     * Server config - servicename-false-server.xml
     * Expected response - null
     *
     * @throws Exception
     */
    @Test
    public void testWrongElementContentServiceNameSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/servicename-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=WrongElementContent");
        // Log response to output.txt
        LOG.info("testWrongElementContentServiceNameSchemaValidationFalse - Response = " + response);

        assertTrue("Expected successful response, but response was " + response,
                   response.contains(": "));
    }

    // serviceName and Global Tests

    /**
     * Test the serviceName takes precedent over the global setting of enableSchemaValidation=true in server.xml, with an additional element <arg0> following the <response> element
     * which will causes No child element is expected at this point exception with the default schema validation enabled.
     *
     * Server config - servicename-true-global-false-server.xml
     * Expected response - Unmarshalling Error: cvc-complex-type.2.4.a
     *
     * @throws Exception
     */
    @Test
    public void testAddedElementServiceNameTrueGlobalSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/servicename-true-global-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testAddedElementServiceNameTrueGlobalSchemaValidationFalse - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: No child element is expected at this point exception is not thrown",
                      server.waitForStringInLog("No child element is expected at this point."));
    }

    /**
     * Test the serviceName takes precedent over the global setting of enableSchemaValidation=false in server.xml, with an additional element <arg0> following the <response>
     * element
     * which is ignored when schema validation is disabled.
     *
     * Server config - servicename-false-global-truee-server.xml
     * Expected response - Hello, AddedElement
     *
     * @throws Exception
     */
    @Test
    public void testAddedElementServiceNameFalseGlobalSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/servicename-false-global-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testAddedElementServiceNameFalseGlobalSchemaValidationTrue - Response = " + response);

        assertTrue("Expected successful response, but response was " + response,
                   response.contains("Hello, AddedElement"));
    }

    // incorrect serviceName tests

    /**
     * Test the when the incorrect serviceName is given with enableSchemaValidation is set to true server.xml, with an additional element <arg0> following the <response> element
     * will cause s No child element is expected at this point exception with the default schema validation enabled.
     *
     * Server config - incorrect-servicename-server.xml
     * Expected response - SAXParseException.*unexpected element
     *
     * @throws Exception
     */
    @Test
    public void testAddedElementIncorrectServiceNameSchemaValidatinFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationTestServer/incorrect-servicename-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=user
        String response = runTest(server, APP_NAME + "/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
        LOG.info("testAddedElementIncorrectServiceNameSchemaValidatinFalse - Response = " + response);

        assertNull("Expected null response from server, but was" + response, response);
        assertNotNull("Expected Unmarshalling Error: unexpected element exception is not thrown",
                      server.waitForStringInLog("unexpected element"));
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
