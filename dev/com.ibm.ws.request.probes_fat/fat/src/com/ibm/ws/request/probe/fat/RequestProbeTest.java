/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.probe.fat;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class RequestProbeTest {
    @Server("ProbeServer")
    public static LibertyServer server;

    private static final String MESSAGE_LOG = "logs/messages.log";

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "jdbcTestPrj_3", "com.ibm.ws.request.timing");
        server.setServerConfigurationFile("server_RT2.xml");
        server.startServer();
    }

    @Test
    @Mode(TestMode.FULL)
    public void testReqTimingDynamicDisable() throws Exception {
        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "Started server with Slow Request Timing  : 2s");
        createRequest(2100);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
        int previous = lines.size();
        CommonTasks.writeLogMsg(Level.INFO, "---->No. of Slow Request Timer Warnings : " + previous);
        assertTrue("No slow request warning found!", (previous > 0));

        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "-----> Updating server configuration to REMOVE Request Timing feature..");
        server.setServerConfigurationFile("server_NoRT.xml");

        server.waitForStringInLogUsingMark("CWWKF0013I", 30000);
        server.waitForStringInLogUsingMark("CWWKG0017I", 10000);
        server.waitForStringInLogUsingMark("CWWKT0016I", 10000);

        Thread.sleep(60000); // Sleep for one minute
        createRequest(2100);

        lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
        assertTrue("slow request warning found!", (lines.size() == previous));

        CommonTasks.writeLogMsg(Level.INFO, "********** Request Timing works when added dynamically **********");
    }

    private void createRequest(int duration) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_3?sleepTime=" + duration);
        CommonTasks.writeLogMsg(Level.INFO, "Calling jdbcTestPrj_3 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        br.readLine();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("TRAS0112W");
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
     * @param url The Http Address to connect to
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

    @Before
    public void setupTestStart() throws Exception {
        if (server != null && !server.isStarted()) {
            server.setServerConfigurationFile("server_original.xml");
            server.startServer();
        }
    }

}