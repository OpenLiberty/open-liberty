/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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

package com.ibm.ws.wssecurity.fat.cxf.usernametoken;

import static componenttest.annotation.SkipForRepeat.EE10_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;
import com.ibm.ws.wssecurity.fat.utils.common.SharedTools;
import com.ibm.ws.wssecurity.fat.utils.common.UpdateWSDLPortNum;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@SkipForRepeat({ RepeatWithEE7cbh20.ID, EE9_FEATURES, EE10_FEATURES })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CxfUNTNonceTests {

    static final private String serverName = "com.ibm.ws.wssecurity_fat";
    @Server(serverName)
    public static LibertyServer server;

    private final Class<?> thisClass = CxfUNTBasicTests.class;

    private static String untClientUrl = "";

    private static String httpPortNumber = "";

    /**
     * Sets up any configuration required for running the OAuth tests.
     * Currently, it just starts the server, which should start the
     * applications in dropins.
     */
    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, "untclient", "com.ibm.ws.wssecurity.fat.untclient", "fats.cxf.basic.wssec", "fats.cxf.basic.wssec.types");
        ShrinkHelper.defaultDropinApp(server, "untoken", "com.ibm.ws.wssecurity.fat.untoken");

        server.startServer();// check CWWKS0008I: The security service is ready.
        SharedTools.waitForMessageInLog(server, "CWWKS0008I");
        httpPortNumber = "" + server.getHttpDefaultPort();

        server.waitForStringInLog("port " + httpPortNumber);

        untClientUrl = "http://localhost:" + httpPortNumber +
                       "/untclient/CxfUntSvcClient";

        return;

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf unt web service by sending nonce and timestamp in the SOAP request.
     *
     */

    @Test
    public void testUntNonceAndCreated() throws Exception {

        String thisMethod = "testUntNonceAndCreated";
        String expectedResponse = "This is WSSECFVT CXF Web Service.";
        String respReceived = null;

        try {

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Invoke the service client - servlet
            request = new GetMethodWebRequest(untClientUrl);

            request.setParameter("httpDefaultPort", httpPortNumber);
            request.setParameter("untClient", "nonce1");

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from IBM UNT Service client: " + respReceived);
        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception occurred:");
            Log.error(thisClass, thisMethod, e, "Exception: ");
            throw e;
        }

        assertTrue("The testUntWssecSvcClient test failed",
                   respReceived.contains(expectedResponse));

        return;

    }

    /**
     * TestDescription:
     *
     * In this scenario, the Web service provider expects Nonce and Created in the
     * Username token, but the client does not send Nonce and Created in the request.
     * The client request is expected to be rejected with an appropriate exception.
     *
     */

    @Test
    public void testUntNonceExpected() {

        String thisMethod = "testUntNonceExpected";
        // String expectedResponse = "Nonce and Created elements are missing";
        String expectedResponse = "Username Token Created policy not enforced, Username Token Nonce policy not enforced";
        String respReceived = null;
        String WSDL_PORT_NUM = "8010"; // hard coded port number in client WSDL
        UpdateWSDLPortNum newWsdl = null;
        boolean updateNeeded = true;

        try {

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Invoke the service client - servlet
            request = new GetMethodWebRequest(untClientUrl);

            request.setParameter("httpDefaultPort", httpPortNumber);
            request.setParameter("untClient", "nonceExpected");
            String clientWsdl = System.getProperty("user.dir") + File.separator +
                                "cxfclient-policies" + File.separator + "ClientUsrTokenWebSvc.wsdl";

            // Check if port number in WSDL is same as server's http port number
            if (httpPortNumber.equals(WSDL_PORT_NUM)) {
                updateNeeded = false;
                request.setParameter("clntWsdlLocation", clientWsdl);
            } else { // port number needs to be updated
                String newClientWsdl = System.getProperty("user.dir") + File.separator +
                                       "cxfclient-policies" + File.separator + "ClientUsrTokenWebSvcNew.wsdl";
                newWsdl = new UpdateWSDLPortNum(clientWsdl, newClientWsdl);
                newWsdl.updatePortNum(WSDL_PORT_NUM, httpPortNumber);
                request.setParameter("clntWsdlLocation", newClientWsdl);
            }

            // Invoke the service client
            response = wc.getResponse(request);
            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from UNT Service client: " + respReceived);

            // Remove the new WSDL file
            if (updateNeeded) {
                newWsdl.removeWSDLFile();
            }
        } catch (Exception ex) {

            Log.info(thisClass, thisMethod, "UsrToken Nonce exception received: ");
            // ex.printStackTrace();
            String exMsg = ex.getMessage();
            Log.info(thisClass, thisMethod, "NonceExpected Exception: " + exMsg);
            // Set response received to Exception in this case
            respReceived = exMsg;
        }

        assertTrue("Unexpected exception received: " + respReceived,
                   respReceived.contains(expectedResponse));

        return;

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}
