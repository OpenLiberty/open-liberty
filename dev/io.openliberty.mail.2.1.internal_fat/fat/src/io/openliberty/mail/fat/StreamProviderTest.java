/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.mail.fat;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class StreamProviderTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("mailSessionTestServer");
    private final Class<?> c = StreamProviderTest.class;

    private static String stringURL = null;
    private static boolean streamProviderTestPassed = true;
    private static final String STREAM_PROVIDER_ERROR_MESSAGE = "Failed to get stream provider from session. This test won't get executed.";

    @BeforeClass
    public static void setup() throws Exception {
        if (server.isStarted() != true) {
            server.startServer();
            // Pause for application to start properly and server to say it's listening on ports
            server.waitForStringInLog("port " + server.getHttpDefaultPort());
        }
        stringURL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestingApp/StreamProviderSessionServlet";

    }

    @AfterClass
    public static void tearDown() throws Exception {

        String resourceWarning = server.waitForStringInLog("expected resource not found:");

        // If this assert fails its because the default mail cap files needed to set default encoding/decoding via
        // the activation framework are missing are not visible to the mail-2.x spec. Check the bnd file configuration
        // to ensure bundle has proper resources included
        assertNull("FAIL: One of the Jakarta Mail resources in /META-INF/ directory is not availible to the application",
                   resourceWarning);

        if (server != null && server.isStarted()) {
            server.stopServer("CWWKZ0013E");
        }
    }

    /**
     * TestDescription:
     *
     * Tests newly added StreamProvider class to Jakarta Mail 2.1 by testing Base64 Encoder method
     * from StreamProvider class in StreamProviderSessionServlet servlet.
     */
    @Test
    public void testBase64() throws Exception {

        String ret = connectAndReturn("testBase64");

        // When we can't get stream provider, none of these test could run
        assertTrue(STREAM_PROVIDER_ERROR_MESSAGE, streamProviderTestPassed);

        assertTrue("BASE64EncoderStream couldn't get created by session.getStreamProvider().outputBase64(outputStream) in StreamProviderSessionServlet",
                   ret.contains("BASE64EncoderStream"));
        assertTrue("BASE64DecoderStream couldn't get created by session.getStreamProvider().inputBase64(inputStream) in StreamProviderSessionServlet",
                   ret.contains("BASE64DecoderStream"));
    }

    /**
     * TestDescription:
     *
     * Tests newly added StreamProvider class to Jakarta Mail 2.1 by testing Binary Encoder method
     * from StreamProvider class in StreamProviderSessionServlet servlet.
     */
    @Test
    public void testBinary() throws Exception {

        String ret = connectAndReturn("testBinary");

        // When we can't get stream provider, none of these test could run
        assertTrue(STREAM_PROVIDER_ERROR_MESSAGE, streamProviderTestPassed);

        // session.getStreamProvider().outputBinary(outputStream) creates the exact text entered no test required
        assertTrue("ByteArrayInputStream couldn't get created by session.getStreamProvider().inputBinary(inputStream) in StreamProviderSessionServlet",
                   ret.contains("ByteArrayInputStream"));
    }

    /**
     * TestDescription:
     *
     * Tests newly added StreamProvider class to Jakarta Mail 2.1 by testing Q Encoder method
     * from StreamProvider class in StreamProviderSessionServlet servlet.
     */
    @Test
    public void testQ() throws Exception {

        String ret = connectAndReturn("testQ");

        // When we can't get stream provider, none of these test could run
        assertTrue(STREAM_PROVIDER_ERROR_MESSAGE, streamProviderTestPassed);

        assertTrue("QEncoderStream couldn't get created by session.getStreamProvider().outputQ(outputStream) in StreamProviderSessionServlet",
                   ret.contains("QEncoderStream"));
        assertTrue("QDecoderStream couldn't get created by session.getStreamProvider().inputQ(inputStream) in StreamProviderSessionServlet",
                   ret.contains("QDecoderStream"));
    }

    /**
     * TestDescription:
     *
     * Tests newly added StreamProvider class to Jakarta Mail 2.1 by testing QP Encoder method
     * from StreamProvider class in StreamProviderSessionServlet servlet.
     */
    @Test
    public void testQP() throws Exception {

        String ret = connectAndReturn("testQP");

        // When we can't get stream provider, none of these test could run
        assertTrue(STREAM_PROVIDER_ERROR_MESSAGE, streamProviderTestPassed);

        assertTrue("QPEncoderStream couldn't get created by session.getStreamProvider().outputQP(outputStream) in StreamProviderSessionServlet",
                   ret.contains("QPEncoderStream"));
        assertTrue("QPDecoderStream couldn't get created by session.getStreamProvider().inputQP(inputStream) in StreamProviderSessionServlet",
                   ret.contains("QPDecoderStream"));
    }

    /**
     * TestDescription:
     *
     * Tests newly added StreamProvider class to Jakarta Mail 2.1 by testing UU Encoder method
     * from StreamProvider class in StreamProviderSessionServlet servlet.
     */
    @Test
    public void testUU() throws Exception {

        String ret = connectAndReturn("testUU");

        // When we can't get stream provider, none of these test could run
        assertTrue(STREAM_PROVIDER_ERROR_MESSAGE, streamProviderTestPassed);

        assertTrue("UUEncoderStream couldn't get created by session.getStreamProvider().outputUU(outputStream) in StreamProviderSessionServlet",
                   ret.contains("UUEncoderStream"));
        assertTrue("BASE64DecoderStream couldn't get created by session.getStreamProvider().inputUU(inputStream) in StreamProviderSessionServlet",
                   ret.contains("UUDecoderStream"));
    }

    /**
     * This method is used to create a URL with request parameter, connect
     * get connection stream and provide output from the servlet as String
     *
     * @param name of the test code to be run on the servlet
     * @return output from the servlet
     */
    private String connectAndReturn(String testName) throws IOException {

        if (true == streamProviderTestPassed) { // Do not run test code since one of the tests failed to get stream provider
            URL url = new URL(stringURL + "?testName=" + testName);
            Log.info(c, testName,
                     "Calling MailSession Application with URL=" + url.toString());
            HttpURLConnection con = getHttpConnection(url);
            BufferedReader br = getConnectionStream(con);

            String line = br.readLine();

            Log.info(c, testName, "return String=" + line);

            // We can't invoke getStreamProvider from Session class, setting this flag to stop execution of other tests
            if (line == null)
                streamProviderTestPassed = false;

            return line;
        } else {
            return "";
        }
    }

    /**
     * This method is used to get a connection stream from an HTTP connection. It
     * gives the output from the webpage that it gets from the connection
     *
     * @param con The connection to the HTTP address
     * @return The Output from the webpage
     */
    private BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        return br;
    }

    /**
     * This method creates a connection to a webpage and then reutrns the connection
     *
     * @param url   The Http Address to connect to
     * @param param The Http request parameter
     * @return The connection to the http address
     */
    private HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("POST");
        return con;
    }
}
