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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
public class RequestProbeServletRegTDTest {
    @Server("ServletServer")
    public static LibertyServer server;

    private static final String TRACE_LOG = "logs/trace.log";
    private static List<String> regTDs;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "ServletTest", "com.ibm.ws.request.timing");
        server.setServerConfigurationFile("server_original.xml");
        server.startServer();
    }

    @Test
    @Mode(TestMode.FULL)
    public void testAllServletTDsRegistered() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "Started server with Event Logging feature");
        createRequest(1);

        fetchRegisteredTDsFromTraceLog();

        assertTrue("No TDs registered!", regTDs.size() > 0);
        CommonTasks.writeLogMsg(Level.INFO, "--------->Total no of TDs found : " + regTDs.size());

        int missed = lookForServletTDs(regTDs);
        assertTrue("No of Servlet TDs not found " + missed, (missed == 0));
        CommonTasks.writeLogMsg(Level.INFO, "******* All servlet TDs are registered *******");
    }

    private int lookForServletTDs(List<String> TDs) {
        HashMap<String, String> servletTDDetails = new HashMap<String, String>();
        servletTDDetails.put("websphere.servlet.service", "service");
        servletTDDetails.put("websphere.servlet.destroy", "destroy");
        servletTDDetails.put("websphere.session.getAttribute", "getAttribute");
        servletTDDetails.put("websphere.session.setAttribute", "setAttribute");
        servletTDDetails.put("websphere.session.sessionAccessed", "sessionAccessed");
        servletTDDetails.put("websphere.session.sessionCreated", "sessionCreated");
        servletTDDetails.put("websphere.session.sessionDestroyedByTimeout", "sessionDestroyedByTimeout");
        servletTDDetails.put("websphere.session.sessionDestroyed", "sessionDestroyed");
        servletTDDetails.put("websphere.session.sessionLiveCountDec", "sessionLiveCountDec");
        servletTDDetails.put("websphere.session.sessionLiveCountInc", "sessionLiveCountInc");
        servletTDDetails.put("websphere.session.sessionReleased", "sessionReleased");
        servletTDDetails.put("websphere.session.dbSessionDestroyedByTimeout", "sessionDestroyedByTimeout");
        servletTDDetails.put("websphere.session.dbSessionDestroyed", "sessionDestroyed");

        for (String td : TDs) {
            Iterator<Map.Entry<String, String>> tdDetail = servletTDDetails.entrySet().iterator();
            while (tdDetail.hasNext()) {
                Map.Entry<String, String> entry = tdDetail.next();
                if (td.contains(entry.getValue())) {
                    tdDetail.remove();
                }
            }
        }
        int missed = servletTDDetails.size();
        if (missed != 0) {
            Iterator<Map.Entry<String, String>> tdDetail = servletTDDetails.entrySet().iterator();
            while (tdDetail.hasNext()) {
                CommonTasks.writeLogMsg(Level.INFO, "-----> TD NOT found : " + tdDetail.next().getKey());
            }
        }
        return missed;
    }

    private void fetchRegisteredTDsFromTraceLog() throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot("", TRACE_LOG);
        regTDs = new ArrayList<String>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line.contains("> setRequestProbeMetaDataProvider")) {
                regTDs.add(lines.get(i + 1).trim());
            }
        }
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