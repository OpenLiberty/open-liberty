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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;


import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/** 
 * 
 * This test class tests the <service-ref> configuration with the <webservices-bnd> binding configuration. 
 * This configuration can be set in both the ibm-ws-bnd.xml and server.xml, so tests should cover both uses cases. 
 * 
 * Example of this configuration looks like this: 
 * 
 * <webservices-bnd >
 *       <service-ref name="service/SimpleEchoService">
 *             <properties enableLoggingInOutInterceptor="true" />
 *       </service-ref>
 * </webservices-bnd>
 * 
 * The service-ref name attribute must match the @WebServiceRef(name="sevice/SimpleEchoService") or the value in the <service-ref> DD for injection/JDNI look ups. 
 * 
 * These tests use a "serviceRef" request parameter to tell the test servlet which instance of the @WebServiceRef client to use, as each client is mapped
 * to a specific configuration in either the ibm-ws-bnd.xml or server.xml files. Using multiple client instances prevents us from having to reload the server/application by changing
 * config for the same client instance. 
 * 
 * We use the the @see com.ibm.ws.test.stubclient packages from the com.ibm.ws.jaxws.2.2.webcontainer_fat project for this test, with the modifications to 
 * the @see com.ibm.ws.test.stubclient.client.SimpleStubClientServlet change to use cdi injected @WebServiceRef client, which this configuration requires. 
 * 
 * The test uses the @see com.ibm.ws.test.stubclient.SimpleEcho as the Web Service, which simply returns whatever string it was passed in the request back to the client
 */
@RunWith(FATRunner.class)
public class WebServiceBndServiceRefPropertiesConfigTest {
    @Server("WebServiceRefBndConfigTestServer")
    public static LibertyServer server;

    private static final String APP_NAME = "simpleTestService";
    
    // Max timeout set to 5 minutes in milliseconds
    private final static int REQUEST_TIMEOUT = 300000;

    private static final String SERVLET_PATH = "/" + APP_NAME + "/SimpleStubClientServlet";

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server, APP_NAME, "io.openliberty.jaxws.fat.stubclient",
                                      "io.openliberty.jaxws.fat.stubclient.client");
        server.startServer();
        

        assertNotNull("Application hello does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));
    }
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
    
    /**
     * This tests configuring the enableLoggingInOutInterceptor=true in the server.xml file. This tests invokes the test servlet, which uses a @WebServiceRef service client
     * to send a simple soap request to the SimpleEcho Web Service. 
     * 
     * If the configuration is properly applied, the inbound/outbound SOAP Request/Responses will show up in the logs with 
     * the "REQ_OUT" "RESP_IN". The test verifies expected messages exist in the logs. 
     * 
     * 
     * The test passes serviceRef="server" to tell the SimpleStubClientServlet to use the @WebServiceRef client that's bound to the <service-ref> in the server.xml file
     * 
     * @throws Exception
     */
    @Test
    public void testEnableLoggingInOutInterceptorServer() throws Exception {


    	   String serviceRef = "server";
            String response = invokeService(serviceRef); // Call the SimpleStubClientServlet invoke the SimpleService Web Service
            assertTrue("Expected response to contain Pass but contained: " + response, response.contains("Pass"));
            
            // Verify message.log contains the SOAP Message logging
            List<String> dumpInMessages = server.findStringsInLogs("REQ_OUT");
            List<String> dumpOutMessages = server.findStringsInLogs("RESP_IN");
            assertTrue("Can't find inBoundMessage, the return inboundmessage is: " + dumpInMessages.toString(), !dumpInMessages.isEmpty());
            assertTrue("Can't find outBoundMessage, the return outboundmessage is: " + dumpOutMessages.toString(), !dumpOutMessages.isEmpty());
    }
    
    /**
     * This tests configuring the enableLoggingInOutInterceptor=true in the ibm-ws-bnd.xml file. This tests invokes the test servlet, which uses a @WebServiceRef service client
     * to send a simple soap request to the SimpleEcho Web Service. 
     * 
     * If the configuration is properly applied, the inbound/outbound SOAP Request/Responses will show up in the logs with 
     * the "REQ_OUT" "RESP_IN". The test verifies expected messages exist in the logs. 
     * 
     * The test passes serviceRef="bnd" to tell the SimpleStubClientServlet to use the @WebServiceRef client that's bound to the <service-ref> in the ibm-ws-bnd.xml file
     * 
     * @throws Exception
     */
    @Test
    public void testEnableLoggingInOutInterceptorIbmWsBnd() throws Exception {

        	String serviceRef = "bnd";
            String response = invokeService(serviceRef); // Call the SimpleStubClientServlet invoke the SimpleService Web Service
            assertTrue("Expected response to contain Pass but contained: " + response, response.contains("Pass"));
            

            // Verify message.log contains the SOAP Message logging
            List<String> dumpInMessages = server.findStringsInLogs("REQ_OUT");
            List<String> dumpOutMessages = server.findStringsInLogs("RESP_IN");
            assertTrue("Can't find inBoundMessage, the return inboundmessage is: " + dumpInMessages.toString(), !dumpInMessages.isEmpty());
            assertTrue("Can't find outBoundMessage, the return outboundmessage is: " + dumpOutMessages.toString(), !dumpOutMessages.isEmpty());
    }


    /**
     * util method to make HTTPUrlConnection to the test servlet
     * @param serviceRef 
     * 
     * @returns: the response from the test servlet - SimpleStubClientServlet
     */
    private String invokeService(String serviceRef) throws Exception {
        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append(SERVLET_PATH).append("?").append("serviceRef=").append(serviceRef);
        String urlStr = sBuilder.toString();

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        return line;

    }


}
