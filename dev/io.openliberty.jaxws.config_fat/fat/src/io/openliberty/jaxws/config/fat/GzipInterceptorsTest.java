/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
import java.util.Map;
import java.util.HashMap;

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
 * This test suite tests the cxf.add.gzip.in.interceptor and cxf.add.gzip.out.interceptor JVM System properties:
 *
 * 1.) Test default behavior of GZIP interceptors: GZIPOutInterceptor and GZIPInInterceptor.
 * 2.) Test JVM System properties: cxf.add.gzip.out.interceptor="true" and cxf.add.gzip.in.interceptor="true"
 *     for enabling GZIPOutInterceptor and GZIPInInterceptor.
 */

@RunWith(FATRunner.class)
public class GzipInterceptorsTest {

    Logger LOG = Logger.getLogger("GzipInterceptorsTest.class");

    // Max timeout set to 5 minutes in milliseconds
    private final static int REQUEST_TIMEOUT = 300000;
    private final static String GZIP_OUT_INTERCEPTOR_PROP = "-Dcxf.add.gzip.out.interceptor";
    private final static String GZIP_IN_INTERCEPTOR_PROP =  "-Dcxf.add.gzip.in.interceptor";
    private final static String GZIPOUT_INTERCEPTOR_INVOKED =  "Inside handleMessage of GZIPOutInterceptor";
    private final static String GZIPIN_INTERCEPTOR_INVOKED =   "Inside handleMessage of GZIPInInterceptor";
    private static final String APP_NAME = "testWebServiceClient";

    @Server("GzipInterceptorsTestServer")
    public static LibertyServer gzipServer;

    @BeforeClass
    public static void setUp() throws Exception {
    	
        ShrinkHelper.defaultDropinApp(gzipServer, "testWebServiceClient", "io.openliberty.samples.jaxws.client",
                                      "io.openliberty.samples.jaxws.client.handler",
                                      "io.openliberty.samples.jaxws.client.servlet", "io.openliberty.jaxws.fat.mock.endpoint");

        gzipServer.startServer("GzipInterceptorsTest.log");

        assertNotNull("Application hello does not appear to have started.", gzipServer.waitForStringInLog("CWWKZ0001I:.*" + "testWebServiceClient"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (gzipServer != null && gzipServer.isStarted()) {
            gzipServer.stopServer();
        }
    }

    /**
     * 1. Test the default behavior of CXF for GZIPOutInterceptor and GZIPInInterceptor interceptors, which are
     *    not enabled. The handleMessage() methods of the GZIP interceptors should not be invoked.
     * 2. Test the JVM properties: cxf.add.gzip.out.interceptor=true and cxf.add.gzip.in.interceptor=true, which 
     *    should enable the GZIP interceptors and cause the handleMessage() methods of GZIPOutInterceptor and 
     *    GZIPInInterceptor to be invoked.
     * 
     * Server config - server.xml
     * Expected response -  Hello, AddedElement
     *
     * @throws Exception
     */
    @Test
    public void testEnableGZIPInterceptors() throws Exception {

        String response = runTest(gzipServer, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
	LOG.info("testEnableGZIPInterceptors - Response = " + response);

	assertTrue("Expected successful response, but response was " + response,
			response.contains("Hello, AddedElement"));

        // Test default behavior - gzip interceptors should not be enabled
	String findStr1 = gzipServer.waitForStringInTrace(GZIPOUT_INTERCEPTOR_INVOKED, 15000);
	String findStr2 = gzipServer.waitForStringInTrace(GZIPIN_INTERCEPTOR_INVOKED, 15000);
	assertTrue("Unexpeced output [" + GZIPOUT_INTERCEPTOR_INVOKED + "]  found in the server log", findStr1 == null);
	assertTrue("Unexpeced output [" + GZIPIN_INTERCEPTOR_INVOKED + "]  found in the server log", findStr2 == null);

        // Test enabling gzip interceptors by setting JVM properties
	Map<String, String> gzipOptions = new HashMap<>();
        gzipOptions.put("-Dcom.ibm.ws.beta.edition", "true");
        gzipOptions.put(GZIP_OUT_INTERCEPTOR_PROP, "true");
        gzipOptions.put(GZIP_IN_INTERCEPTOR_PROP, "true");
	gzipServer.setJvmOptions(gzipOptions);
	gzipServer.stopServer();
        gzipServer.startServer("EnableGzipInterceptorsTest.log");
        gzipServer.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME);

        response = runTest(gzipServer, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
	LOG.info("testEnableGZIPInterceptors - Response 2 = " + response);

	assertTrue("Expected successful response 2, but response was " + response,
			response.contains("Hello, AddedElement"));

	findStr1 = gzipServer.waitForStringInTrace(GZIPOUT_INTERCEPTOR_INVOKED, 15000);
	findStr2 = gzipServer.waitForStringInTrace(GZIPIN_INTERCEPTOR_INVOKED, 15000);

	assertTrue("Unable to find the output [" + GZIPOUT_INTERCEPTOR_INVOKED + "]  in the server log", findStr1 != null);
	assertTrue("Unable to find the output [" + GZIPIN_INTERCEPTOR_INVOKED + "]  in the server log", findStr2 != null);

    }

    /**
     * 1. Test the default behavior of CXF for GZIPOutInterceptor and GZIPInInterceptor interceptors, which are
     *    not enabled. The handleMessage() methods of the GZIP interceptors should not be invoked.
     * 2. Test the requestcontext properties: cxf.add.gzip.out.interceptor=true and cxf.add.gzip.in.interceptor=true, 
     *    which should enable the GZIP interceptors and cause the handleMessage() methods of GZIPOutInterceptor and 
     *    GZIPInInterceptor to be invoked.
     * 
     * Server config - server.xml
     * Expected response -  Hello, AddedElement
     *
     * @throws Exception
     */
    @Test
    public void testEnableGZIPUsingReqContextProps() throws Exception {

        // Test default behavior - gzip interceptors should not be enabled, make sure JVM props are disabled.
	Map<String, String> gzipOptions = new HashMap<>();
        gzipOptions.put("-Dcom.ibm.ws.beta.edition", "true");
        gzipOptions.put(GZIP_OUT_INTERCEPTOR_PROP, "false");
        gzipOptions.put(GZIP_IN_INTERCEPTOR_PROP, "false");
	gzipServer.setJvmOptions(gzipOptions);
	gzipServer.stopServer();
        gzipServer.startServer("EnableGZIPUsingReqContextProps.log");
        gzipServer.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME);

        String response = runTest(gzipServer, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=AddedElement");
        // Log response to output.txt
	LOG.info("testEnableGZIPInterceptors - Response = " + response);

	assertTrue("Expected successful response, but response was " + response,
			response.contains("Hello, AddedElement"));

	String findStr1 = gzipServer.waitForStringInTrace(GZIPOUT_INTERCEPTOR_INVOKED, 15000);
	String findStr2 = gzipServer.waitForStringInTrace(GZIPIN_INTERCEPTOR_INVOKED, 15000);
	assertTrue("Unexpeced output [" + GZIPOUT_INTERCEPTOR_INVOKED + "]  found in the server log", findStr1 == null);
	assertTrue("Unexpeced output [" + GZIPIN_INTERCEPTOR_INVOKED + "]  found in the server log", findStr2 == null);

        // Test enabling gzip interceptors by setting requestcontext properties on client request

        response = runTest(gzipServer, "testWebServiceClient/IgnoreUnexpectedElementTestServiceServlet?target=AddReqContextProps");
        // Log response to output.txt
	LOG.info("testEnableGZIPInterceptors - Response 2 = " + response);

	assertTrue("Expected successful response 2, but response was " + response,
			response.contains("Hello, AddedElement"));

	findStr1 = gzipServer.waitForStringInTrace(GZIPOUT_INTERCEPTOR_INVOKED, 15000);
	findStr2 = gzipServer.waitForStringInTrace(GZIPIN_INTERCEPTOR_INVOKED, 15000);

	assertTrue("Unable to find the output [" + GZIPOUT_INTERCEPTOR_INVOKED + "]  in the server log", findStr1 != null);
	assertTrue("Unable to find the output [" + GZIPIN_INTERCEPTOR_INVOKED + "]  in the server log", findStr2 != null);

    }

    private String runTest(LibertyServer server, String pathAndParams) throws Exception {
        // Set the mark to end of log before running test so searching the trace log doesn't get the trace messages from the previous test
        gzipServer.setTraceMarkToEndOfDefaultTrace();

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + pathAndParams);
        Log.info(this.getClass(), "assertResponseNotNull", "Calling Application with URL=" + url.toString());

        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        return line;
    }

}
