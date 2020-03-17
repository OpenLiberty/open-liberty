/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.probe.jdbc.fat;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class RequestProbeJDBCTest {
    @Server("JDBCServer")
    public static LibertyServer server;

    private static final String MESSAGE_LOG = "logs/messages.log";
    private static final String TRACE_LOG = "logs/trace.log";
    private static List<String> regTDs;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "jdbcTestPrj_All", "com.ibm.ws.request.timing");
        server.setServerConfigurationFile("server_RT.xml");
        server.startServer();
    }

    @Test
    public void testReqProbeJDBCTDTypes() throws Exception {
        String configUpdate = setConfig("server_RT.xml");
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", configUpdate);
        CommonTasks.writeLogMsg(Level.INFO, "Started server with Request Timing feature");
        createRequests(11000);
        server.waitForStringInLogUsingMark("TRAS0112W", 5000);

        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
        // retry
        if (lines.isEmpty()) {
            CommonTasks.writeLogMsg(Level.INFO, "---->Retry because no slow request warning found!");
            Thread.sleep(60000); // Sleep for 60sec
            createRequests(11000);
            server.waitForStringInLogUsingMark("TRAS0112W", 5000);
            lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
        }
        assertTrue("No slow request warning found!", (lines.size() > 0));
        CommonTasks.writeLogMsg(Level.INFO, "---->Slow Request Timer Warning : " + lines.get(lines.size() - 1));

        lines = server.findStringsInFileInLibertyServerRoot("ms", MESSAGE_LOG);
        CommonTasks.writeLogMsg(Level.INFO, "---->No of events : " + lines.size());

        regTDs = new ArrayList<String>();
        regTDs.add("websphere.datasource.executeUpdate");
        regTDs.add("websphere.datasource.execute");
        regTDs.add("websphere.datasource.executeQuery");
        regTDs.add("websphere.datasource.psExecuteUpdate");
        regTDs.add("websphere.datasource.psExecute");
        regTDs.add("websphere.datasource.psExecuteQuery");
        regTDs.add("websphere.datasource.rsInsertRow");
        regTDs.add("websphere.datasource.rsUpdateRow");
        regTDs.add("websphere.datasource.rsCancelRowUpdates");
        regTDs.add("websphere.datasource.rsDeleteRow");

        for (String line : lines) {
            Iterator<String> td = regTDs.iterator();
            while (td.hasNext()) {
                if (line.contains(td.next())) {
                    td.remove();
                }
            }
        }
        boolean carryon = true;
        if (lines.get(lines.size() - 1).contains("ms + ")) {
            carryon = false;

            CommonTasks.writeLogMsg(Level.INFO, "*****  SKIPPING THIS TEST.. MACHINE IS SLOW.. STILL THE EVENT IS IN PROGRESS... *****");
        }
        if (carryon) {
            if (regTDs.size() != 0) {
                for (String td : regTDs) {
                    CommonTasks.writeLogMsg(Level.INFO, "------> JDBC TD not found :  " + td);
                }

                fail("Not all the JDBC TDs found");
            }
        }
        CommonTasks.writeLogMsg(Level.INFO, "***** All JDBC TDs found ******* ");
    }

    @Test
    public void testAllJdbcTDsRegistered() throws Exception {
        String configUpdate = setConfig("server_original.xml");
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", configUpdate);
        CommonTasks.writeLogMsg(Level.INFO, "Started server with Request Timing feature");
        createRequests(10);

        fetchRegisteredTDsFromTraceLog();

        assertTrue("No TDs registered!", regTDs.size() > 0);
        CommonTasks.writeLogMsg(Level.INFO, "--------->No of TDs found : " + regTDs.size());

        int missed = lookForTDsInTrace(regTDs);
        assertTrue("No. of JDBC TDs not registered :  " + missed, (missed == 0));
        CommonTasks.writeLogMsg(Level.INFO, "********* All JDBC TDs are registered *********");
    }

    private void createRequests(int delayInMilliSeconds) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_All?sleepTime=" + delayInMilliSeconds);
        CommonTasks.writeLogMsg(Level.INFO, "Calling jdbcTestPrj_3 Application with URL=" + url.toString());

        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        br.readLine();
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

    private int lookForTDsInTrace(List<String> JDBCTDs) throws Exception {
        List<String> tds = new ArrayList<String>();
        tds.add("com/ibm/ws/rsadapter/jdbc/WSJdbcStatement|executeUpdate|all|websphere.datasource.executeUpdate");
        tds.add("com/ibm/ws/rsadapter/jdbc/WSJdbcStatement|execute|all|websphere.datasource.execute");
        tds.add("com/ibm/ws/rsadapter/jdbc/WSJdbcPreparedStatement|execute|all|websphere.datasource.psExecute");
        tds.add("com/ibm/ws/rsadapter/jdbc/WSJdbcPreparedStatement|executeQuery|all|websphere.datasource.psExecuteQuery");
        tds.add("com/ibm/ws/rsadapter/jdbc/WSJdbcPreparedStatement|executeUpdate|all|websphere.datasource.psExecuteUpdate");
        tds.add("com/ibm/ws/rsadapter/jdbc/WSJdbcResultSet|cancelRowUpdates|all|websphere.datasource.rsCancelRowUpdates");
        tds.add("com/ibm/ws/rsadapter/jdbc/WSJdbcResultSet|deleteRow|all|websphere.datasource.rsDeleteRow");
        tds.add("com/ibm/ws/rsadapter/jdbc/WSJdbcResultSet|insertRow|all|websphere.datasource.rsInsertRow");
        tds.add("com/ibm/ws/rsadapter/jdbc/WSJdbcResultSet|updateRow|all|websphere.datasource.rsUpdateRow");

        for (String td : JDBCTDs) {
            Iterator<String> tdDetail = tds.iterator();
            while (tdDetail.hasNext()) {
                if (td.contains(tdDetail.next())) {
                    CommonTasks.writeLogMsg(Level.INFO, "----> TD found : " + td);
                    tdDetail.remove();
                }
            }
        }
        int missed = tds.size();

        if (missed != 0) {
            Iterator<String> tdDetail = tds.iterator();
            while (tdDetail.hasNext()) {
                CommonTasks.writeLogMsg(Level.INFO, "----> TD not found : " + tdDetail.next());
            }
        }
        return missed;
    }

    @After
    public void tearDown() throws Exception {
        regTDs = null;
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

    private String setConfig(String fileName) throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(fileName);
        return server.waitForStringInLogUsingMark("CWWKG0017I.*|CWWKG0018I.*");
    }
}
