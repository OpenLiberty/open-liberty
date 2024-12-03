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
 * This test suite tests the following variations of the <webService enableSchemaValidation="true/false" /> server.xml configuration:
 *
 * 1.) enableSchemValidation="true" - "global" setting
 * 2.) enableschemaValiation="false" - "global" setting
 * 3.) enableSchemValidation="true" portName="SayHelloService" - portName setting
 * 4.) enableSchemValidation="false" portName="SayHelloService" - portName setting
 * 5.) Combination of 1 and 4
 * 6.) Combination of 2 and 3
 * 7.) enableSchemValidation="false" PortName="IncorrectPortName" - dispatchMsged client setting with the wrong PortName
 *
 * This Test uses a Mock endpoint to create "bad" responses that the client will fail to unmarshall unless enableSchemaValidation=false
 *
 * @see io.openliberty.jaxws.fat.mock.endpoint.MockJaxwsEndpointServlet
 *
 *      Each test uses a specific server.xml configuration that's reconfigured at test start. It calls the @see SimpleDispatchClientServlet servlet
 *      along with a HTTP request parameter. That parameter is added to the SOAP Request, and when the mock JAX-WS endpoint receives the request, it checks for the string
 *      and correlates it to the "bad" response the endpoint then returns. After the client recieves the inbound response, the test checks the response from the
 *      SimpleDispatchClientServlet servlet
 *      for the specified response or exception in the server logs.
 *
 */
@RunWith(FATRunner.class)
public class EnableSchemaValidationWebServiceTest {

    Logger LOG = Logger.getLogger("EnableSchemaValidationWebServiceTest.class");

    private static final String APP_NAME = "simpleTestService";

    // Max timeout set to 5 minutes in milliseconds
    private final static int REQUEST_TIMEOUT = 300000;

    @Server("EnableSchemaValidationWebServiceTestServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
    	
        
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "io.openliberty.jaxws.fat.stubclient",
                        "io.openliberty.jaxws.fat.stubclient.client");

        server.startServer("EnableSchemaValidationWebServiceTest.log");

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
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/server.xml", "CWWKG0017I");
    }


    // Global True Tests
    
    /**
     * Test the global setting of enableSchemaValidation=true in server.xml, with an additional element <arg0> following the <request> element
     * which will return a successful response because default validation is off by default.
     *
     * Server config - global-true-server.xml
     * Expected response - cvc-complex-type.2.4.d
     *
     * @throws Exception
     */
    @Test
    public void testAddedElementGlobalSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/global-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=AddedElement
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=AddedElement");
        // Log response to output.txt
        LOG.info("testAddedElementGlobalSchemaValidationTrue - Response = " + response);

        assertTrue("Expected Unmarshalling Error: cvc-complex-type.2.4.d, but was " + response, response.contains("cvc-complex-type.2.4.d"));
    }
    

    /**
     * Test the global setting of enableSchemaValidation=true in server.xml, with the wrong element name where <arg0> element is expected
     * which will cause an cvc-complex-type.2.4.a exception with the default schema validation enabled.
     *
     * Server config - global-true-server.xml
     * Expected response - Unmarshalling Error: cvc-complex-type.2.4.a
     *
     * @throws Exception
     */
    @Test
    public void testWrongElementNameGlobalSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/global-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementName
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementName");
        // Log response to output.txt
        LOG.info("testWrongElementNameGlobalSchemaValidationTrue - Response = " + response);


        assertTrue("Expected Unmarshalling Error: cvc-complex-type.2.4.a, but was " + response, response.contains("cvc-complex-type.2.4.a"));
    }

    /**
     * Test the global setting of enableSchemaValidation=true in server.xml, with the wrong namespace in the <arg0> element
     * which will cause an cvc-complex-type.2.4.a exception with the default schema validation enabled.
     *
     * Server config - global-true-server.xml
     * Expected response - cvc-complex-type.2.4.a
     *
     * @throws Exception
     */
    @Test
    public void testWrongNamespaceGlobalSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/global-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongNamespace
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongNamespace");
        // Log response to output.txt
        LOG.info("testWrongNamespaceGlobalSchemaValidationTrue - Response = " + response);


        assertTrue("Expected Unmarshalling Error: cvc-complex-type.2.4.a, but was " + response, response.contains("cvc-complex-type.2.4.a"));
    }

    /**
     * Test the global setting of enableSchemaValidation=true in server.xml, with the wrong content in the <arg0> element
     * The response will contain an addtional child element <WrongElementContent> which will cause an Unexected Element exception
     * with the default schema validation enabled.
     *
     * Server config - global-true-server.xml
     * Expected response - cvc-type.3.1.2
     *
     * @throws Exception
     */
    @Test
    public void testWrongElementContentGlobalSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/global-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementContent
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementContent");
        // Log response to output.txt
        LOG.info("testWrongElementContentGlobalSchemaValidationTrue - Response = " + response);

        assertTrue("Expected Unmarshalling Error: cvc-type.3.1.2, but was " + response, response.contains("cvc-type.3.1.2"));
    }

    // GLOBAL FALSE TESTS

    /**
     * Test the global setting of enableSchemaValidation=false in server.xml, with an additional element <arg0> following the <sayHello> element
     * which will cause s No child element is expected at this point exception with the default schema validation enabled.
     *
     * Server config - global-false-server.xml
     * Expected response - AddedElement
     *
     * @throws 
     */
    @Test
    public void testAddedElementGlobalSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/global-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=AddedElement
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=AddedElement");
        // Log response to output.txt
        LOG.info("testAddedElementGlobalSchemaValidationTrue - Response = " + response);

        assertTrue("Expected echo response from server, but was " + response, response.contains("echo"));
    }
    /**
     * Test the global setting of enableSchemaValidation=false in server.xml, with an additional element <arg0> following the <sayHello> element
     * which will cause s No child element is expected at this point exception with the default schema validation enabled.
     *
     * Server config - global-false-server.xml
     * Expected response - Unmarshalling Error: cvc-complex-type.2.4.a
     *
     * @throws 
     */
    @Test
    public void testWrongElementGlobalSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/global-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementName
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementName");
        // Log response to output.txt
        LOG.info("testAddedElementGlobalSchemaValidationTrue - Response = " + response);

        assertTrue("Expected echo response from server, but was " + response, response.contains("echo"));
    }
    /**
     * Test the global setting of enableSchemaValidation=false in server.xml, with an additional element <arg0> following the <sayHello> element
     * which will cause s No child element is expected at this point exception with the default schema validation enabled.
     *
     * Server config - global-false-server.xml
     * Expected response - WrongNamespace
     *
     * @throws 
     */
    @Test
    public void testWrongNamespaceGlobalSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/global-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongNamespace
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongNamespace");
        // Log response to output.txt
        LOG.info("testAddedElementGlobalSchemaValidationTrue - Response = " + response);

        assertTrue("Expected echo response from server, but was " + response, response.contains("echo"));
    }
    /**
     * Test the global setting of enableSchemaValidation=false in server.xml, with an additional element <arg0> following the <sayHello> element
     * which will cause s No child element is expected at this point exception with the default schema validation enabled.
     *
     * Server config - global-false-server.xml
     * Expected response - WrongElementContent
     *
     * @throws 
     */
    @Test
    public void testWrongElementContentGlobalSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/global-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementContent
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementContent");
        // Log response to output.txt
        LOG.info("testAddedElementGlobalSchemaValidationTrue - Response = " + response);

        assertTrue("Expected echo response from server, but was " + response, response.contains("echo"));
    }
    
    
    // PORTNAME True Tests
    
    /**
     * Test the portName setting of enableSchemaValidation=true in server.xml, with an additional element <arg0> following the <sayHello> element
     * which will return a successful response because default validation is off by default.
     *
     * Server config - portname-true-server.xml
     * Expected response - cvc-complex-type.2.4.d
     *
     * @throws Exception
     */
    @Test
    public void testAddedElementPortNameSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/portname-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=AddedElement
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=AddedElement");
        // Log response to output.txt
        LOG.info("testAddedElementGlobalSchemaValidationTrue - Response = " + response);

        assertTrue("Expected Unmarshalling Error: cvc-complex-type.2.4.d, but was " + response, response.contains("cvc-complex-type.2.4.d"));
    }
    

    /**
     * Test the portName setting of enableSchemaValidation=true in server.xml, with the wrong element name where <arg0> element is expected
     * which will cause an cvc-complex-type.2.4.a exception with the default schema validation enabled.
     *
     * Server config - portname-true-server.xml
     * Expected response - Unmarshalling Error: cvc-complex-type.2.4.a
     *
     * @throws Exception
     */
    @Test
    public void testWrongElementNamePortNameSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/portname-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementName
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementName");
        // Log response to output.txt
        LOG.info("testWrongElementNameGlobalSchemaValidationTrue - Response = " + response);


        assertTrue("Expected Unmarshalling Error: cvc-complex-type.2.4.a, but was " + response, response.contains("cvc-complex-type.2.4.a"));
    }

    /**
     * Test the portName setting of enableSchemaValidation=true in server.xml, with the wrong namespace in the <arg0> element
     * which will cause an cvc-complex-type.2.4.a exception with the default schema validation enabled.
     *
     * Server config - portname-true-server.xml
     * Expected response - cvc-complex-type.2.4.a
     *
     * @throws Exception
     */
    @Test
    public void testWrongNamespacePortNameSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/portname-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongNamespace
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongNamespace");
        // Log response to output.txt
        LOG.info("testWrongNamespaceGlobalSchemaValidationTrue - Response = " + response);


        assertTrue("Expected Unmarshalling Error: cvc-complex-type.2.4.a, but was " + response, response.contains("cvc-complex-type.2.4.a"));
    }

    /**
     * Test the portName setting of enableSchemaValidation=true in server.xml, with the wrong content in the <arg0> element
     * The response will contain an addtional child element <WrongElementContent> which will cause an Unexected Element exception
     * with the default schema validation enabled.
     *
     * Server config - portname-true-server.xml
     * Expected response - cvc-type.3.1.2
     *
     * @throws Exception
     */
    @Test
    public void testWrongElementContentPortNameSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/portname-true-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementContent
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementContent");
        // Log response to output.txt
        LOG.info("testWrongElementContentGlobalSchemaValidationTrue - Response = " + response);

        assertTrue("Expected Unmarshalling Error: cvc-type.3.1.2, but was " + response, response.contains("cvc-type.3.1.2"));
    }

    // PORTNAME FALSE TESTS

    /**
     * Test the portName setting of enableSchemaValidation=false in server.xml, with an additional element <arg0> following the <sayHello> element
     * which will cause s No child element is expected at this point exception with the default schema validation enabled.
     *
     * Server config - portname-false-server.xml
     * Expected response - AddedElement
     *
     * @throws 
     */
    @Test
    public void testAddedElementPortNameSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/portname-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=AddedElement
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=AddedElement");
        // Log response to output.txt
        LOG.info("testAddedElementGlobalSchemaValidationTrue - Response = " + response);

        assertTrue("Expected echo response from server, but was " + response, response.contains("echo"));
    }
    /**
     * Test the portName setting of enableSchemaValidation=false in server.xml, with an additional element <arg0> following the <sayHello> element
     * which will cause s No child element is expected at this point exception with the default schema validation enabled.
     *
     * Server config - portname-false-server.xml
     * Expected response - Unmarshalling Error: cvc-complex-type.2.4.a
     *
     * @throws 
     */
    @Test
    public void testWrongElementPortNameSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/portname-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementName
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementName");
        // Log response to output.txt
        LOG.info("testAddedElementGlobalSchemaValidationTrue - Response = " + response);

        assertTrue("Expected echo response from server, but was " + response, response.contains("echo"));
    }
    /**
     * Test the portName setting of enableSchemaValidation=false in server.xml, with an additional element <arg0> following the <sayHello> element
     * which will cause s No child element is expected at this point exception with the default schema validation enabled.
     *
     * Server config - portname-false-server.xml
     * Expected response - WrongNamespace
     *
     * @throws 
     */
    @Test
    public void testWrongNamespacePortNameSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/portname-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongNamespace
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongNamespace");
        // Log response to output.txt
        LOG.info("testAddedElementGlobalSchemaValidationTrue - Response = " + response);

        assertTrue("Expected echo response from server, but was " + response, response.contains("echo"));
    }
    /**
     * Test the portName setting of enableSchemaValidation=false in server.xml, with an additional element <arg0> following the <sayHello> element
     * which will cause s No child element is expected at this point exception with the default schema validation enabled.
     *
     * Server config - portname-false-server.xml
     * Expected response - WrongElementContent
     *
     * @throws 
     */
    @Test
    public void testWrongElementContentPortNameSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/portname-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementContent
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=WrongElementContent");
        // Log response to output.txt
        LOG.info("testAddedElementGlobalSchemaValidationTrue - Response = " + response);

        assertTrue("Expected echo response from server, but was " + response, response.contains("echo"));
    }
    
    // portName and Global Tests

    /**
     * Test the portName takes precedent over the global setting of enableSchemaValidation=true in server.xml, with an additional element <arg0> following the <sayHello> element
     * which will causes No child element is expected at this point exception with the default schema validation enabled.
     *
     * Server config - portname-true-global-false-server.xml
     * Expected response - Unmarshalling Error: cvc-complex-type.2.4.d
     *
     * @throws Exception
     */
    @Test
    public void testAddedElementPortNameTrueGlobalSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/portname-true-global-false-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=AddedElement
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=AddedElement");
        // Log response to output.txt
        LOG.info("testAddedElementPortNameTrueGlobalSchemaValidationFalse - Response = " + response);

        assertTrue("Expected Unmarshalling Error: cvc-complex-type.2.4.d, but was " + response, response.contains("cvc-complex-type.2.4.d"));
    }

    /**
     * Test the portName takes precedent over the global setting of enableSchemaValidation=false in server.xml, with an additional element <arg0> following the <sayHello> element
     * element
     * which is ignored when schema validation is disabled.
     *
     * Server config - PortName-false-global-truee-server.xml
     * Expected response - Hello, AddedElement
     *
     * @throws Exception
     */
    @Test
    public void testAddedElementPortNameFalseGlobalSchemaValidationTrue() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/portname-false-global-true-server.xml", "CWWKG0017I");


        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=AddedElement
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=AddedElement");
        // Log response to output.txt
        LOG.info("testAddedElementPortNameFalseGlobalSchemaValidationTrue - Response = " + response);

        assertTrue("Expected echo response from server, but was " + response, response.contains("echo"));
    }

    // incorrect portName tests

    /**
     * Test the when the incorrect portName is given with enableSchemaValidation is set to true server.xml, with an additional element <arg0> following the <sayHello> element
     * will causes No child element is expected at this point exception with the default schema validation enabled.
     *
     * Server config - incorrect-portname-server.xml
     * Expected response - echo
     *
     * @throws Exception
     */
    @Test
    public void testAddedElementIncorrectPortNameSchemaValidationFalse() throws Exception {
        server.reconfigureServer("EnableSchemaValidationWebServiceTestServer/incorrect-portname-server.xml", "CWWKG0017I");

        // Visit the URL:  http://hostName:hostPort/simpleTestService/SimpleDispatchClientServlet?dispatchMsg=AddedElement
        String response = runTest(server, "simpleTestService/SimpleDispatchClientServlet?dispatchMsg=AddedElement");
        // Log response to output.txt
        LOG.info("testAddedElementIncorrectPortNameSchemaValidatinFalse - Response = " + response);

        assertTrue("Expected echo response from server, but was " + response, response.contains("echo"));
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
