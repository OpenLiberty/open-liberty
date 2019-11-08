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
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class RequestProbeTestEnableDisable {
    @Server("ProbeServer")
    public static LibertyServer server;

    private static final String MESSAGE_LOG = "logs/messages.log";

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "jdbcTestPrj_3", "com.ibm.ws.request.timing");
        server.startServer();
    }

    @Test
    public void testRequestTimingDynamicEnableDisable() throws Exception {
        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "$$$$ ----> Started server without RT feature... ");

        CommonTasks.writeLogMsg(Level.INFO, "$$$$-----> Updating server configuration to ADD Request Timing feature..");
        enableRequestTiming();

        boolean featureFound = isRequestTimingEnabled();
        assertTrue("Request Timing Feature is not added..", featureFound);
        CommonTasks.writeLogMsg(Level.INFO, "********* Added Request Timing Feature..! *********");

        createRequest(3000);
        int previous = fetchNoOfslowRequestWarnings();
        // retry
        if (previous == 0) {
            CommonTasks.writeLogMsg(Level.INFO, "$$$$ -----> Retry because no slow request warning found!");
            Thread.sleep(60000);
            createRequest(3000);
            previous = fetchNoOfslowRequestWarnings();
        }
        assertTrue("No slow request warning found!", (previous > 0));

        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "$$$$ -----> Updating server configuration to REMOVE Request Timing feature..");
        disableRequestTiming();

        createRequest(3000);

        int current = fetchNoOfslowRequestWarnings();
        assertTrue("slow request warning found!", ((current - previous) == 0));

        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "$$$$-----> Updating server configuration to RE-ADD Request Timing feature..");
        enableRequestTiming();

        featureFound = isRequestTimingEnabled();
        assertTrue("Request Timing Feature is not added..", featureFound);
        CommonTasks.writeLogMsg(Level.INFO, "********* Added Request Timing Feature..! *********");
        Thread.sleep(60000);

        createRequest(4000);
        current = fetchNoOfslowRequestWarnings();
        CommonTasks.writeLogMsg(Level.INFO, "---->previous : " + previous + "current no of slow requets warnings : " + current);

        assertTrue("No slow request warning found!", ((current - previous) > 0));

        CommonTasks.writeLogMsg(Level.INFO, "********** Request Timing works when added / removed / re-added dynamically **********");
    }

    private void createRequest(int duration) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_3?sleepTime=" + duration);
        CommonTasks.writeLogMsg(Level.INFO, "Calling jdbcTestPrj_3 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        br.readLine();
    }

    private boolean isRequestTimingEnabled() throws Exception {
        server.waitForStringInLogUsingMark("CWWKF0012I", 60000);
        List<String> lines = server.findStringsInFileInLibertyServerRoot("CWWKF0012I", MESSAGE_LOG);
        if (lines.get(lines.size() - 1).contains("requestTiming-1.0")) {
            return true;
        }
        return false;
    }

    private int fetchNoOfslowRequestWarnings() throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
        CommonTasks.writeLogMsg(Level.INFO, "---->No. of Slow Request Timer Warnings : " + lines.size());
        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "----> slow request warning : " + line);
        }
        return lines.size();
    }

    private void enableRequestTiming() throws Exception {
        server.setServerConfigurationFile("server_RT2.xml");
        server.waitForStringInLogUsingMark("CWWKF0012I", 30000);
        server.waitForStringInLogUsingMark("CWWKG0017I", 10000);
    }

    private void disableRequestTiming() throws Exception {
        server.setServerConfigurationFile("server_NoRT.xml");

        server.waitForStringInLogUsingMark("CWWKF0013I", 30000);
        server.waitForStringInLogUsingMark("CWWKG0017I", 10000);
        server.waitForStringInLogUsingMark("CWWKT0016I", 10000);
        Thread.sleep(60000);
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
            server.startServer();
        }
    }

}