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
package com.ibm.ws.request.probe.servlet.fat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
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
public class RequestProbeServletTDTest {
    @Server("ServletServer")
    public static LibertyServer server;

    private static final String MESSAGE_LOG = "logs/messages.log";

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "ServletTest", "com.ibm.ws.request.timing");
        server.setServerConfigurationFile("server_original.xml");
        server.startServer();
    }

    @Test
    public void testReqProbeServletTDs() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "Started server with Event Logging feature");
        createRequest(1);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("END", MESSAGE_LOG);
        assertTrue("No END logs found!", (lines.size() > 0));

        for (String record : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "-----> END : " + record);
        }
        List<String> servletTDs = new ArrayList<String>();
        servletTDs.add("websphere.servlet.service");
        servletTDs.add("websphere.session.getAttribute");
        servletTDs.add("websphere.session.setAttribute");
        //Not adding rest of the TDs as they cannot be seen if their isCounter() returns true.

        for (String line : lines) {
            Iterator<String> td = servletTDs.iterator();
            while (td.hasNext()) {
                if (line.contains(td.next())) {
                    CommonTasks.writeLogMsg(Level.INFO, "------> Found  TD : " + line);
                    td.remove();
                }
            }
        }
        if (servletTDs.size() != 0) {
            for (String td : servletTDs) {
                CommonTasks.writeLogMsg(Level.INFO, "------> Servlet TD not found :  " + td);
            }
            fail("Not all the Servlet TDs found");

        }
        CommonTasks.writeLogMsg(Level.INFO, "***** All Servlet TDs found ******* ");

    }

    private void createRequest(int delayInMilliSeconds) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/ServletTest?sleepTime=" + delayInMilliSeconds);
        CommonTasks.writeLogMsg(Level.INFO, "Calling ServletTest Application with URL=" + url.toString());

        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        br.readLine();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
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