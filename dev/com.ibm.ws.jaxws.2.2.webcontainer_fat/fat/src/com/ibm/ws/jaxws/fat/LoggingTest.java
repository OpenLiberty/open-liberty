/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
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
 * Tests addition of LoggingFeature when trace and debug are on.
 * Basically we supposed to see SOAP request and response in XML format in log file(s)
 * March - 2026 - Modified tests to cover positive and negative scenarios
 * testing dynamic configuration and EnableLoggingInOutInterceptor
 */
@RunWith(FATRunner.class)
public class LoggingTest {

    @Server("LoggingServer")
    public static LibertyServer server;

    private static URL WSDL_URL;

    private static final String APP_NAME = "helloApp";

    private static final int CONN_TIMEOUT = 5;

    private static Logger log = Logger.getLogger(LoggingTest.class.getName());

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "com.ibm.ws.jaxws.test.wsr.server",
                                                      "com.ibm.ws.jaxws.test.wsr.servlet",
                                                      "com.ibm.ws.jaxws.test.wsr.server.impl",
                                                      "com.ibm.ws.jaxws.test.wsr.server.stub",
                                                      "com.ibm.ws.jaxws.fat.util");
        ShrinkHelper.exportAppToServer(server, app);

        server.startServer();

        assertNotNull("Application hello does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));

        WSDL_URL = new URL(new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/").append(APP_NAME).append("/PeopleService?wsdl").toString());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKW0056W");
        }
    }

    /**
     * Test if LoggingFeature works with a basic JAX-WS web service
     * then disable trace and do negative test to test dynamic configuration without server restart
     * then set EnableLoggingInOutInterceptor to true and disable trace to test the property alone
     * Didn't create multiple test for each due to test framework issues and efficiency
     *
     * @throws Exception
     */
    @Test
    public void testFeatureLog() throws Exception {

        // **********************
        // *** Positive tests ***
        // **********************
        log.info("*** < Trace is enabled for positive test > ***");

        String result = null;
        // Positive test proxy
        result = invokeServlet("proxy");
        assertTrue("Proxy with trace: Expected response is not received, obtained result: " + result, result.equals("Hello World"));
        assertNotNull("Proxy with trace: Logging Feature enablement failed!", server.waitForStringInLog("<return>Hello World</return>"));

        // Positive test dispatch
        result = invokeServlet("dispatch");
        assertTrue("Dispatch with trace: Expected response is not received, obtained result: " + result, result.equals("Dispatch invoke success: true"));
        assertNotNull("Dispatch with trace: Logging Feature enablement failed!", server.waitForStringInLog("<return>Hello from dispatch World</return>"));

        // *********************
        // *** Disable trace ***
        // *********************
        log.info("*** < Trace is disabled for negative test > ***");
        server.reconfigureServer("LoggingServer/NegativeLoggingFeature.xml", "CWWKG0017I");

        // Negative test proxy
        result = invokeServlet("proxy");
        assertTrue("Proxy without trace: Expected response is not received testing dynamic logging, obtained result: " + result, result.equals("Hello World"));
        assertNull("Proxy without trace: Dynamic configuration failed!", server.waitForStringInLog("<return>Hello World</return>"));

        // Negative test dispatch
        result = invokeServlet("dispatch");
        assertTrue("Dispatch without trace: Expected response is not received testing dynamic logging, obtained result: " + result, result.equals("Dispatch invoke success: true"));
        assertNull("Dispatch without trace: Dynamic configuration failed!", server.waitForStringInLog("<return>Hello World</return>"));

        // *******************************************************************************************************
        // *** Test web service reference enableLoggingInOutInterceptor property enabled without trace enabled ***
        // *******************************************************************************************************
        log.info("*** < web service reference enableLoggingInOutInterceptor is enabled without trace > ***");
        server.reconfigureServer("LoggingServer/EnableLoggingInOutInterceptor.xml", "CWWKG0017I");

        // enableLoggingInOutInterceptor test web service reference
        result = invokeServlet("wsref");
        assertTrue("Proxy with web service reference enableLoggingInOutInterceptor: Expected response is not received, obtained result: " + result,
                   result.equals("Hello World"));
        assertNotNull("Proxy with web service reference enableLoggingInOutInterceptor: EnableLoggingInOutInterceptor enablement failed!",
                      server.waitForStringInLog("<return>Hello World</return>"));

        // enableLoggingInOutInterceptor test dispatch
        result = invokeServlet("dispatch");
        assertTrue("Dispatch with web service reference enableLoggingInOutInterceptor: Expected response is not received, obtained result: " + result,
                   result.equals("Dispatch invoke success: true"));
        assertNotNull("Dispatch with web service reference enableLoggingInOutInterceptor: EnableLoggingInOutInterceptor enablement is failed!",
                      server.waitForStringInLog("<return>Hello World</return>"));

        // *******************************************************************************************************************
        // *** Test without web service reference enableLoggingInOutInterceptor property enabled and without trace enabled ***
        // *******************************************************************************************************************
        log.info("*** < web service reference EnableLoggingInOutInterceptor property and trace are disabled > ***");
        server.reconfigureServer("LoggingServer/NegativeLoggingFeature.xml", "CWWKG0017I");

        // enableLoggingInOutInterceptor test web service reference
        result = invokeServlet("wsref");
        assertTrue("Proxy without web service reference enableLoggingInOutInterceptor property: Expected response is not received testing dynamic logging, obtained result: "
                   + result, result.equals("Hello World"));
        assertNull("Proxy without web service reference enableLoggingInOutInterceptor property: Dynamic configuration failed!",
                   server.waitForStringInLog("<return>Hello World</return>"));

        // enableLoggingInOutInterceptor test dispatch
        result = invokeServlet("dispatch");
        assertTrue("Dispatch without web service reference enableLoggingInOutInterceptor property: Expected response is not received testing dynamic logging, obtained result:"
                   + result,
                   result.equals("Dispatch invoke success: true"));
        assertNull("Dispatch without web service reference enableLoggingInOutInterceptor property: Dynamic configuration failed!",
                   server.waitForStringInLog("<return>Hello World</return>"));

        // ****************************************************************************************************
        // *** Test with endpoint info enableLoggingInOutInterceptor property enabled without trace enabled ***
        // ****************************************************************************************************
        log.info("*** < Endpoint info enableLoggingInOutInterceptor property is enabled without trace > ***");
        server.reconfigureServer("LoggingServer/EnableLoggingInOutInterceptorFromEndpoint.xml", "CWWKG0017I");

        // enableLoggingInOutInterceptor test endpoint property
        result = invokeServlet("proxy");
        assertTrue("Proxy with endpoint info enableLoggingInOutInterceptor property: Expected response is not received, obtained result: " + result,
                   result.equals("Hello World"));
        assertNotNull("Proxy with endpoint info enableLoggingInOutInterceptor property: EnableLoggingInOutInterceptor enablement failed!",
                      server.waitForStringInLog("<return>Hello World</return>"));

        // enableLoggingInOutInterceptor test dispatch endpoint property
        result = invokeServlet("dispatch");
        assertTrue("Dispatch with endpoint info enableLoggingInOutInterceptor property: Expected response is not received, obtained result: " + result,
                   result.equals("Dispatch invoke success: true"));
        assertNotNull("Dispatch with endpoint info enableLoggingInOutInterceptor property: EnableLoggingInOutInterceptor enablement is failed!",
                      server.waitForStringInLog("<return>Hello World</return>"));

        // *******************************************************************************************************
        // *** Test without endpoint info enableLoggingInOutInterceptor property enabled without trace enabled ***
        // *******************************************************************************************************
        log.info("*** < EnableLoggingInOutInterceptor and trace are disabled > ***");
        server.reconfigureServer("LoggingServer/NegativeLoggingFeature.xml", "CWWKG0017I");

        // enableLoggingInOutInterceptor test endpoint property
        result = invokeServlet("proxy");
        assertTrue("Proxy without endpoint info enableLoggingInOutInterceptor property: Expected response is not received testing dynamic logging, obtained result: "
                   + result, result.equals("Hello World"));
        assertNull("Proxy without endpoint info enableLoggingInOutInterceptor property: Dynamic configuration failed!",
                   server.waitForStringInLog("<return>Hello World</return>"));

        // enableLoggingInOutInterceptor test dispatch endpoint property
        result = invokeServlet("dispatch");
        assertTrue("Dispatch without endpoint info enableLoggingInOutInterceptor property: Expected response is not received testing dynamic logging, obtained result:" + result,
                   result.equals("Dispatch invoke success: true"));
        assertNull("Dispatch without endpoint info enableLoggingInOutInterceptor property: Dynamic configuration failed!",
                   server.waitForStringInLog("<return>Hello World</return>"));

        // *******************************************************************************************************************
        // *** Check back if we can enable endpoint info enableLoggingInOutInterceptor property back without trace enabled ***
        // *******************************************************************************************************************
        log.info("*** < Check back endpoint info enableLoggingInOutInterceptor property is enabled without trace > ***");
        server.reconfigureServer("LoggingServer/EnableLoggingInOutInterceptorFromEndpoint.xml", "CWWKG0017I");

        // enableLoggingInOutInterceptor test endpoint property
        result = invokeServlet("proxy");
        assertTrue("Proxy with endpoint info enableLoggingInOutInterceptor property: Expected response is not received, obtained result: " + result,
                   result.equals("Hello World"));
        assertNotNull("Proxy with endpoint info enableLoggingInOutInterceptor property: EnableLoggingInOutInterceptor enablement failed!",
                      server.waitForStringInLog("<return>Hello World</return>"));
    }

    private String invokeServlet(String webServiceType) throws ProtocolException, IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/LoggingTestServlet?WSType=" + webServiceType);
        log.info("Servlet URL: " + url.toString());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        return br.readLine();
    }
}
