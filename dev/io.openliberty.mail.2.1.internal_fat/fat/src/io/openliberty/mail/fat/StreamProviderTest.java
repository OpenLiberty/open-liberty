/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.mail.fat;

import static org.junit.Assert.assertNotNull;

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

    @BeforeClass
    public static void setup() throws Exception {
        if (server.isStarted() != true) {
            server.startServer();
            // Pause for application to start properly and server to say it's listening on ports
            server.waitForStringInLog("port " + server.getHttpDefaultPort());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKZ0013E");
        }
    }

    /**
     * TestDescription:
     *
     * Tests newly added StreamProvider class to Jakarta Mail 2.1 by invoking getStreamProvider method
     * from Session class in StreamProviderSessionServlet servlet.
     */
    @Test
    public void testNewStreamProvider() throws Exception {

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestingApp/StreamProviderSessionServlet");
        Log.info(c, "testNewStreamProvider",
                 "Calling MailSession Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);

        String line = br.readLine();

        // Assert that the message has been received by the smtpServer
        assertNotNull("FAIL: getStreamProvider method of Session class failed to be invoked", line);
    }

    /**
     * TestDescription:
     *
     * Tests newly added StreamProvider class to Jakarta Mail 2.1 by testing Base64 Encoder method
     * from StreamProvider class in StreamProviderSessionServlet servlet.
     */
    @Test
    public void testBase64Encoder() throws Exception {

    }

    /**
     * TestDescription:
     *
     * Tests newly added StreamProvider class to Jakarta Mail 2.1 by testing Binary Encoder method
     * from StreamProvider class in StreamProviderSessionServlet servlet.
     */
    @Test
    public void testBinaryEncoder() throws Exception {

    }

    /**
     * TestDescription:
     *
     * Tests newly added StreamProvider class to Jakarta Mail 2.1 by testing Q Encoder method
     * from StreamProvider class in StreamProviderSessionServlet servlet.
     */
    @Test
    public void testQEncoder() throws Exception {

    }

    /**
     * TestDescription:
     *
     * Tests newly added StreamProvider class to Jakarta Mail 2.1 by testing QP Encoder method
     * from StreamProvider class in StreamProviderSessionServlet servlet.
     */
    @Test
    public void testQPEncoder() throws Exception {

    }

    /**
     * TestDescription:
     *
     * Tests newly added StreamProvider class to Jakarta Mail 2.1 by testing UU Encoder method
     * from StreamProvider class in StreamProviderSessionServlet servlet.
     */
    @Test
    public void testUUEncoder() throws Exception {

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
        con.setRequestMethod("GET");
        return con;
    }
}
