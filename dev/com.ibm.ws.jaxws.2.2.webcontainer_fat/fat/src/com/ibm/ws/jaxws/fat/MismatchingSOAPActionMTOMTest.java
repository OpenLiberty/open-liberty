/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests based off MTOMTest which have been added for testing SOAPAction headers when set on the request and the ability for an MTOM enabled
 * Web Service Endpoint to handle them depending on if allowing for mismatching Actions are set in the server.xml configuration
 */
@RunWith(FATRunner.class)
public class MismatchingSOAPActionMTOMTest {
    private static final int REQUEST_TIMEOUT = 10;

    @Server("MTOMTestServer")
    public static LibertyServer server;

    @Server("MTOMClientTestServer")
    public static LibertyServer clientServer;

    protected String SERVLET_PATH = "/testMTOMClient/MTOMClientServlet";
    protected String SERVLET_PATH2 = "/testMTOMClient_withDD/MTOMDDClient";

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        server.setHttpDefaultPort(9988);
        clientServer.setHttpDefaultPort(9888);

        ShrinkHelper.defaultApp(server, "testMTOM", "com.ibm.jaxws.MTOM");

        ShrinkHelper.defaultDropinApp(clientServer, "testMTOMClient", "com.ibm.jaxws.MTOM.servlet",
                                      "mtomservice");

        ShrinkHelper.defaultDropinApp(server, "testMTOMClient_withDD", "com.ibm.jaxws.MTOM.dd.servlet",
                                      "mtomservice.dd");

        server.startServer("MTOMTest.log");
        // Pause for application to start successfully
        server.waitForStringInLog("CWWKZ0001I.*testMTOM");

        clientServer.startServer("MTOMTest.log");

        clientServer.waitForStringInLog("CWWKZ0001I.*testMTOMClient");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE0777E", "SRVE0315E");
        }

        if (clientServer != null && clientServer.isStarted()) {
            clientServer.stopServer("SRVE0777E", "SRVE0315E");
        }
    }

    /**
     * TestDescription: Tests an MTOM enabled Web Service endpoint with SOAPAction being set on the request will throw a SOAPFault when allowNonMatchingToDefaultSoapAction=false is
     * set in the server.xml
     * Result:
     * - response contains "The given SOAPAction " + soapAction + " does not match an operation"
     *
     * @throws IOException
     */
    @Test
    @ExpectedFFDC("javax.xml.ws.soap.SOAPFaultException")
    public void testMTOMWithMismatchingSOAPActionWithExpectedException() throws Exception {

        server.reconfigureServer("MTOMTestServer/allowNonMatchingToDefaultSoapAction-false.xml", "CWWKG0017I");

        String soapAction = "Mismatch";
        int port = server.getHttpDefaultPort();
        StringBuilder urlBuilder = new StringBuilder("http://").append(clientServer.getHostname()).append(":").append(clientServer.getHttpDefaultPort()).append(SERVLET_PATH).append("?service=MTOMService").append("&port=").append(port).append("&setSoapAction=").append(soapAction);

        List<String> expectedResponses = new ArrayList<String>(1);
        expectedResponses.add("The given SOAPAction Mismatch does not match an operation.");
        assertTrue(printExpectedResponses(expectedResponses, false),
                   checkExpectedResponses(urlBuilder.toString(), expectedResponses, false, HttpURLConnection.HTTP_INTERNAL_ERROR));
    }

    /**
     * TestDescription: Tests an MTOM enabled Web Service endpoint with SOAPAction being set on the request will return appropriate response when
     * allowNonMatchingToDefaultSoapAction=true is
     * set in the server.xml
     * Result:
     * - response contains "getAttachment() returned"
     *
     * @throws IOException
     */
    @Test
    public void testMTOMWithMismatchingSOAPActionWithAllowingMismatchToDefaultSoapAction() throws Exception {

        server.reconfigureServer("MTOMTestServer/allowNonMatchingToDefaultSoapAction-true.xml", "CWWKG0017I");

        String soapAction = "Mismatch";
        int port = server.getHttpDefaultPort();
        StringBuilder urlBuilder = new StringBuilder("http://").append(clientServer.getHostname()).append(":").append(clientServer.getHttpDefaultPort()).append(SERVLET_PATH).append("?service=MTOMService").append("&port=").append(port).append("&setSoapAction=").append(soapAction);

        List<String> expectedResponses = new ArrayList<String>(1);
        expectedResponses.add("getAttachment() returned");
        assertTrue(printExpectedResponses(expectedResponses, false), checkExpectedResponses(urlBuilder.toString(), expectedResponses, false, HttpURLConnection.HTTP_OK));
    }

    /**
     * TestDescription: Tests an MTOM enabled Web Service endpoint with SOAPAction being set on the request will throw a SOAPFault when allowNonMatchingSoapAction=false is
     * set in the server.xml
     * Result:
     * - response contains "The given SOAPAction " + soapAction + " does not match an operation"
     *
     * @throws IOException
     */
    @Test
    @ExpectedFFDC("javax.xml.ws.soap.SOAPFaultException")
    public void testMTOMWithMismatchingSOAPActionWithNonMatchingExpectedException() throws Exception {

        server.reconfigureServer("MTOMTestServer/allowNonMatchingSoapAction-false.xml", "CWWKG0017I");

        String soapAction = "Mismatch";
        int port = server.getHttpDefaultPort();
        StringBuilder urlBuilder = new StringBuilder("http://").append(clientServer.getHostname()).append(":").append(clientServer.getHttpDefaultPort()).append(SERVLET_PATH).append("?service=MTOMService").append("&port=").append(port).append("&setSoapAction=").append(soapAction);

        List<String> expectedResponses = new ArrayList<String>(1);
        expectedResponses.add("The given SOAPAction Mismatch does not match an operation.");
        assertTrue(printExpectedResponses(expectedResponses, false),
                   checkExpectedResponses(urlBuilder.toString(), expectedResponses, false, HttpURLConnection.HTTP_INTERNAL_ERROR));
    }

    /**
     * TestDescription: Tests an MTOM enabled Web Service endpoint with SOAPAction being set on the request will return appropriate response when allowNonMatchingSoapAction=true is
     * set in the server.xml
     * Result:
     * - response contains "getAttachment() returned"
     *
     * @throws IOException
     */
    @Test
    public void testMTOMWithMismatchingSOAPActionWithAllowNonMatchingSoapAction() throws Exception {

        server.reconfigureServer("MTOMTestServer/allowNonMatchingSoapAction-true.xml", "CWWKG0017I");

        String soapAction = "Mismatch";
        int port = server.getHttpDefaultPort();
        StringBuilder urlBuilder = new StringBuilder("http://").append(clientServer.getHostname()).append(":").append(clientServer.getHttpDefaultPort()).append(SERVLET_PATH).append("?service=MTOMService").append("&port=").append(port).append("&setSoapAction=").append(soapAction);

        List<String> expectedResponses = new ArrayList<String>(1);
        expectedResponses.add("getAttachment() returned");
        assertTrue(printExpectedResponses(expectedResponses, false), checkExpectedResponses(urlBuilder.toString(), expectedResponses, false, HttpURLConnection.HTTP_OK));
    }

    /**
     * TestDescription: When ibm-mtom-enabled is set to false
     * MTOM is disabled only for response
     *
     * <webservice-endpoint-properties ibm-mtom-enabled="false" />
     *
     * Result:
     * - response Content-Type is expected to be text/xml
     *
     * @throws IOException
     */
    @Test
    public void testDisableMtomForResponse() throws Exception {

        server.reconfigureServer("MTOMTestServer/disableMtomForResponse.xml", "CWWKG0017I");

        String soapAction = "";
        int port = server.getHttpDefaultPort();
        StringBuilder urlBuilder = new StringBuilder("http://").append(clientServer.getHostname()).append(":").append(clientServer.getHttpDefaultPort()).append(SERVLET_PATH).append("?service=MTOMService").append("&port=").append(port).append("&setSoapAction=").append(soapAction);

        List<String> expectedResponses = new ArrayList<String>(1);
        expectedResponses.add("Content-Type -> text/xml");
        assertTrue(printExpectedResponses(expectedResponses, false), checkExpectedResponses(urlBuilder.toString(), expectedResponses, false, HttpURLConnection.HTTP_OK));
    }

    private boolean checkExpectedResponses(String servletUrl, List<String> expectedResponses, boolean exact, int responseCode) throws IOException {
        Log.info(this.getClass(), testName.getMethodName(), "Calling Application with URL=" + servletUrl);

        try {
            HttpURLConnection con = HttpUtils.getHttpConnection(new URL(servletUrl), responseCode, REQUEST_TIMEOUT);
            StringBuilder sb = new StringBuilder();
            BufferedReader br = null;
            try {
                br = HttpUtils.getConnectionStream(con);
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    line = br.readLine();
                }
            } finally {
                if (br != null) {
                    br.close();
                }
            }

            String responseContent = sb.toString();

            Log.info(MismatchingSOAPActionMTOMTest.class, "checkExpectedResponses", "responseContent = " + responseContent);
            if (exact) { //the response content must contain all the expect strings
                for (String expectStr : expectedResponses) {
                    if (!responseContent.contains(expectStr)) {
                        return false;
                    }
                }
                return true;
            } else { //the response content could contain one of the expect strings
                for (String expectStr : expectedResponses) {
                    if (responseContent.contains(expectStr)) {
                        return true;
                    }
                }
                return false;
            }
        } catch (IOException e) {
            // Return true if the exception contains the expected response code
            if (e.getMessage().contains(Integer.toString(responseCode))) {
                return true;
            }
        }

        return false;

    }

    private String printExpectedResponses(List<String> expectedResponses, boolean exact) {
        StringBuilder sb = new StringBuilder("The expected output in server log is ");
        if (!exact && expectedResponses.size() > 1) {
            sb.append("one of ");
        }
        sb.append("[");
        for (int i = 0; i < expectedResponses.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"" + expectedResponses.get(i) + "\"");
        }
        sb.append("]");
        return sb.toString();
    }

}
