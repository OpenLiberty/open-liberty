/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.wssecurity.fat.cxf.nowssec;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;
import com.ibm.ws.wssecurity.fat.utils.common.SharedTools;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@SkipForRepeat({ RepeatWithEE7cbh20.ID })
@RunWith(FATRunner.class)
public class CxfNoWssecTests {

    static final private String serverName = "com.ibm.ws.wssecurity_fat";
    @Server(serverName)

    public static LibertyServer server;

    private final Class<?> thisClass = CxfNoWssecTests.class;

    private static final String SERVICE_NS = "http://wssec.basic.cxf.fats";

    // 10/2020 this line gets NPE in OL junit framework
    //private static final String wsdlLocation = "http://localhost:" +
    //                server.getHttpDefaultPort() +
    //                "/nowssec/SOAPService1?wsdl";

    private static String wsdlLocation;

    private static String serviceClientUrl = "";
    private static String httpPortNumber = "";
    private static final StringReader reqMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\"><invoke>WSSECFVT Version: 2.0</invoke></soapenv:Body></soapenv:Envelope>");

    /**
     * Sets up any configuration required for running the OAuth tests.
     * Currently, it just starts the server, which should start the
     * applications in dropins.
     */
    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, "cxfclient", "com.ibm.ws.wssecurity.fat.cxfclient", "fats.cxf.basic.wssec", "fats.cxf.basic.wssec.types");
        ShrinkHelper.defaultDropinApp(server, "nowssec", "com.ibm.ws.wssecurity.fat.nowssec");

        server.startServer();// check CWWKS0008I: The security service is ready.

        wsdlLocation = "http://localhost:" + server.getHttpDefaultPort() + "/nowssec/SOAPService1?wsdl";

        SharedTools.waitForMessageInLog(server, "CWWKS0008I");
        httpPortNumber = "" + server.getHttpDefaultPort();

        server.waitForStringInLog("port " + httpPortNumber);

        serviceClientUrl = "http://localhost:" + httpPortNumber +
                           "/cxfclient/CxfNoWssecSvcClient";

        return;

    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service.
     * There is no ws-security policy included in the request message.
     *
     */

    //11/2020 comment out for OL due to error:
    //2020-11-19-23:41:23:397 com.sun.xml.internal.ws.spi.db.DatabindingException: Unknown JAXBContext implementation: class com.sun.xml.bind.v2.runtime.JAXBContextImpl
    //at dispSOAPMsg.invoke(soapReq1);
    //@Test
    /*
     * public void testCxfClientNoWssec() throws Exception {
     *
     * String thisMethod = "testCxfClientNoWssec";
     * String expectedResponse = "This is WSSECFVT CXF Web Service.";
     *
     * URL wsdlURL = new URL(wsdlLocation);
     * Log.info(thisClass, thisMethod, "wsdlURL is: " + wsdlURL);
     *
     * QName serviceName1 = new QName(SERVICE_NS, "SOAPService1");
     * QName portName1 = new QName(SERVICE_NS, "SoapPort1");
     *
     * SOAPService1 service1 = new SOAPService1(wsdlURL, serviceName1);
     * Log.info(thisClass, thisMethod, "service1 is: " + service1);
     *
     * Source src = new StreamSource(reqMsg);
     * Log.info(thisClass, thisMethod, "stream source src with reqmsg is: " + src);
     *
     * MessageFactory factory = MessageFactory.newInstance();
     * SOAPMessage soapReq1 = factory.createMessage();
     * soapReq1.getSOAPPart().setContent(src);
     * soapReq1.saveChanges();
     * Log.info(thisClass, thisMethod, "after soapReq1 lines");
     *
     * Dispatch<SOAPMessage> dispSOAPMsg = service1.createDispatch(portName1,
     * SOAPMessage.class, Mode.MESSAGE);
     * Log.info(thisClass, thisMethod, "after dispSOAPMsg service1.createDispatch ");
     *
     * Log.info(thisClass, thisMethod, "Invoking server through Dispatch interface using SOAPMessage");
     *
     * Log.info(thisClass, thisMethod, "before dispSOAPMsg.invoke soapReq1");
     * SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq1);
     * Log.info(thisClass, thisMethod, "after dispSOAPMsg.invoke soapReq1");
     *
     * Log.info(thisClass, thisMethod, "CXF Client Response SOAP Body contents: " + soapResp.getSOAPBody().getTextContent());
     *
     * String respText = soapResp.getSOAPBody().getTextContent();
     *
     * assertTrue("The testCxfClientToCxfWebSvc test failed",
     * respText.contains(expectedResponse));
     *
     * return;
     *
     * }
     */
    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes
     * a simple jax-ws cxf we b service.
     *
     */

    @Test
    public void testNoWssecCxfSvcClient() throws Exception {

        String thisMethod = "testNoWssecCxfSvcClient";
        String expectedResponse = "This is WSSECFVT CXF Web Service.";
        String respReceived = null;

        try {

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Invoke the service client - servlet
            request = new GetMethodWebRequest(serviceClientUrl);

            request.setParameter("httpDefaultPort", httpPortNumber);
            // request.setParameter("targetWSName", "NoWssecWebSvc");

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from CXF Service client: " + respReceived);

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception occurred:");
            System.err.println("Exception: " + e);
            throw e;
        }

        assertTrue("The testNoWssecCxfSvcClient test failed",
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
