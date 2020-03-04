/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * This test bucket tests the server startup process.
 */
public class ServerStartAsServiceTest {
    private static final Class<?> c = ServerStartAsServiceTest.class;

    private static final String SERVER_NAME_1 = "ServerStartAsServiceTest1";
    private static final String SERVER_NAME_2 = "ServerStartAsServiceTest2";

    private static LibertyServer server;

    @ClassRule
    public static final TestRule onWinRule = new OnlyRunOnWinRule();

    @Before
    public void before() {
        final String METHOD_NAME = "before";
        if (server != null) {
            Log.info(c, METHOD_NAME, "server variable not null at start of test; setting to null.");
            server = null;
        }
    }

    @After
    public void after() throws Exception {
        final String METHOD_NAME = "after";
        if (server != null && server.isStarted()) {
            Log.info(c, METHOD_NAME, "stopping server (potentially again) and nulling the 'server' variable");
            server.stopServer();
            server = null;
        }
    }

    @Test
    /**
     * test Liberty Server to Register, Start, Stop and Unregister as a Windows Service
     *
     * @throws Exception
     */
    public void testWinServiceLifeCycle() throws Exception {
        final String METHOD_NAME = "testWinServiceLifeCycle";
        Log.info(c, METHOD_NAME, ".. entering ..");

        Log.info(c, METHOD_NAME, "calling LibertyServerFactory.getLibertyServer(SERVER_NAME, ON): " + SERVER_NAME_1);
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME_1, LibertyServerFactory.WinServiceOption.ON);

        try {
            Log.info(c, METHOD_NAME, "calling server.startServer()");
            server.startServer();
        } catch (Exception e) {
            // try to clean up the win service entry and start again
            Log.info(c, METHOD_NAME, "server.startServer() failed in " + METHOD_NAME + ". Exception was " + e.getMessage());
            Log.info(c, METHOD_NAME, "Attempting server.stopServer()...");
            server.stopServer();
        }

        Log.info(c, METHOD_NAME, "calling server.waitForStringInLog('CWWKF0011I')");
        server.waitForStringInLog("CWWKF0011I");

        assertTrue("the server should have been started", server.isStarted());

        Log.info(c, METHOD_NAME, "calling server.stopServer(): " + SERVER_NAME_1);
        server.stopServer();

        Log.info(c, METHOD_NAME, ".. exiting ..");
    }

    @Test
    /**
     * test Liberty Server to Register, Start, Stop and Unregister as a Windows Service
     * test snoop can be installed and accessed.
     *
     * @throws Exception
     */
    public void testWinServiceAppAccess() throws Exception {
        final String METHOD_NAME = "testWinServiceAppAccess";
        Log.info(c, METHOD_NAME, ".. entering ..");

        Log.info(c, METHOD_NAME, "calling LibertyServerFactory.getLibertyServer(SERVER_NAME, ON): " + SERVER_NAME_2);
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME_2, LibertyServerFactory.WinServiceOption.ON);

        try {
            Log.info(c, METHOD_NAME, "calling server.startServer()");
            server.startServer();
        } catch (Exception e) {
            // try to clean up the win service entry
            server.stopServer();
        }

        Log.info(c, METHOD_NAME, "calling server.waitForStringInLog('CWWKF0011I')");
        server.waitForStringInLog("CWWKF0011I");

        assertTrue("the server should have been started", server.isStarted());

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop");
        Log.info(c, METHOD_NAME, "Calling Snoop Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();
        assertTrue("The response did not contain the \'Snoop Servlet\'",
                   line.contains("Snoop Servlet"));

        Log.info(c, METHOD_NAME, "return line: " + line);

        Log.info(c, METHOD_NAME, "calling server.stopServer(): " + SERVER_NAME_2);
        server.stopServer();

        Log.info(c, METHOD_NAME, ".. exiting ..");
    }

    /**
     * This method is used to get a connection stream from an HTTP connection. It
     * gives the output from the webpage that it gets from the connection
     *
     * @param con The connection to the HTTP address
     * @return The Output from the webpage
     */
    private static BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        return br;
    }

    /**
     * This method creates a connection to a webpage and then reutrns the connection
     *
     * @param url The Http Address to connect to
     * @return The connection to the http address
     */
    private static HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }

}
