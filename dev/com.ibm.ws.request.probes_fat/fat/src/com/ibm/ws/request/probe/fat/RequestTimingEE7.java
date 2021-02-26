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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

// The following test is originally from com.ibm.ws.request.probes_fat_java7
@RunWith(FATRunner.class)
public class RequestTimingEE7 {
    @Server("RequestTimingEE7Server")
    public static LibertyServer server;

    private static final String MESSAGE_LOG = "logs/messages.log";
    private final Class<?> c = RequestTimingEE7.class;

    private static final String APP_NAME = "jdbcTestPrj_3_EE7";

    @BeforeClass
    public static void beforeClass() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "jdbcTestPrj_3_EE7", "com.ibm.ws.request.probe");
        server.startServer();
    }

    @Test
    public void testSlowRequestTimingEE7() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("server_request_timing_ee_7.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        server.setMarkToEndOfLog();

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_3_EE7?sleepTime=11000");
        Log.info(c, "testEventLogging", "Calling jdbcTestPrj_3 Application with URL=" + url.toString());

        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        br.readLine();

        server.waitForStringInLogUsingMark("TRAS0112W");

        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);

        CommonTasks.writeLogMsg(Level.INFO, " Size : " + lines.size());
        assertTrue("No slow request warning found!", lines.size() > 0);

        String warning = lines.get(0);
        CommonTasks.writeLogMsg(Level.INFO, "---->Slow Request Timer Warning : " + warning);
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

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("TRAS0112W");
        }
    }

}