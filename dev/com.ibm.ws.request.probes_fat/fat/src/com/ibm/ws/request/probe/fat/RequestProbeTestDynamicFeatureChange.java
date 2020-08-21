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

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class RequestProbeTestDynamicFeatureChange {
    @Server("ProbeServer")
    public static LibertyServer server;
    private static final String MESSAGE_LOG = "logs/messages.log";

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "jdbcTestPrj_3", "com.ibm.ws.request.timing");
        server.setServerConfigurationFile("server_NoFeatures.xml");
        server.startServer();
    }

    private int fetchNoOfslowRequestWarnings() throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot("TRAS0112W", MESSAGE_LOG);
        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "----> slow request warning : " + line);
        }
        return lines.size();
    }

    private int fetchNoENDWarnings() throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot("I END", MESSAGE_LOG);
        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "----> END warning : " + line);
        }
        return lines.size();
    }

    @Test
    public void testRequestTimingFeatureRemoval() throws Exception {
        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "$$$$ ---->Updating server configuration : ADDING RequestTiming and EventLogging Features");
        server.setServerConfigurationFile("server_RT_EL.xml");
        waitForFeatureAddition();

        assertTrue("Expected both RequestTiming and EventLogging features to be enabled.", (isEventLoggingEnabled() && isRequestTimingEnabled()));

        createRequest(5000);
        int slow = fetchNoOfslowRequestWarnings();
        int end = fetchNoENDWarnings();
        // retry
        if (slow == 0) {
            CommonTasks.writeLogMsg(Level.INFO, "$$$$ -----> Retry because no slow request warning found!");
            Thread.sleep(60000);
            createRequest(5000);
            slow = fetchNoOfslowRequestWarnings();
            end = fetchNoENDWarnings();
        }
        CommonTasks.writeLogMsg(Level.INFO, "----> Slow Warnings : " + slow);
        CommonTasks.writeLogMsg(Level.INFO, "----> END Warnings : " + end);
        assertTrue("No Slow Request timing record found!", slow > 0);
        assertTrue("No EventLogging record found!", end > 0);

        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "$$$$ ---->Updating server configuration : Removing EventLogging Feature");
        server.setServerConfigurationFile("server_RT.xml");
        waitForFeatureRemoval();

        createRequest(5000);

        CommonTasks.writeLogMsg(Level.INFO, "----> Slow Warnings : " + (fetchNoOfslowRequestWarnings() - slow));
        CommonTasks.writeLogMsg(Level.INFO, "----> END Warnings : " + (fetchNoENDWarnings() - end));

        assertTrue("No Slow Request timing record found!", ((fetchNoOfslowRequestWarnings() - slow) > 0));
        assertTrue("EventLogging record found!", ((fetchNoENDWarnings() - end) == 0));

        assertTrue("RequestTiming feature is not removed!", isRequestTimingEnabled());
    }

    @Test
    public void testEventLoggingFeatureRemoval() throws Exception {
        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "$$$$ ---->Updating server configuration : ADDING EventLogging Feature");
        server.setServerConfigurationFile("server_EL.xml");
        waitForFeatureAddition();

        createRequest(5000);
        int end = fetchNoENDWarnings();
        CommonTasks.writeLogMsg(Level.INFO, "----> END Warnings : " + fetchNoENDWarnings());

        assertTrue("Expected EventLogging features to be enabled.", isEventLoggingEnabled());

        assertTrue("No EventLogging warning found!", (end > 0));

        server.setMarkToEndOfLog();
        CommonTasks.writeLogMsg(Level.INFO, "$$$$ ---->Updating server configuration : REMOVING EventLogging , ADDING RequestTiming");
        server.setServerConfigurationFile("server_RT.xml");
        waitForFeatureRemoval();

        createRequest(5000);
        int slow = fetchNoOfslowRequestWarnings();
        int end_new = fetchNoENDWarnings() - end;
        CommonTasks.writeLogMsg(Level.INFO, "----> Slow Warnings : " + slow);
        CommonTasks.writeLogMsg(Level.INFO, "----> END Warnings : " + end_new);

        assertTrue("EventLogging feature is not removed!", isEventLoggingRemoved());

        assertTrue("Expected RequestTiming warning.", (slow > 0));

        assertTrue("Found EventLogging warning!", (end_new == 0));
    }

//    public void testRequestTimingRemoveInThread() throws Exception {
//        this.logStep("$$$ ---> Server Configuration : Enable RequestTiming");
//        server.setServerConfigurationFile("server_RT.xml");
//        waitForFeatureAddition();
//        this.logStepCompleted();
//
//        assertTrue("RequestTiming is not Enabled.", isRequestTimingEnabled());
//        RequestThread request = new RequestThread(90000);
//        RemoveFeatureThread remove = new RemoveFeatureThread();
//
//        request.start();
//        remove.start();
//
//        request.join();
//        remove.join();
//
//        //waitForFeatureRemoval();
//
//        assertTrue("RequestTiming is not Removed", isRequestTimingRemoved());
//
//        assertTrue("Exceptions found in log..", checkForExceptionInLog());
//
//    }

    private boolean checkForExceptionInLog() throws Exception {
        boolean found = false;
        List<String> lines = server.findStringsInFileInLibertyServerRoot("exception", MESSAGE_LOG);
        if (lines.size() == 0) {
            for (String line : lines) {
                CommonTasks.writeLogMsg(Level.INFO, "---> exception : " + line);
            }
            found = true;
        } else {
            lines = server.findStringsInFileInLibertyServerRoot("Exception", MESSAGE_LOG);
            if (lines.size() == 0) {
                for (String line : lines) {
                    CommonTasks.writeLogMsg(Level.INFO, "---> Exception : " + line);
                }
                found = true;
            }
        }
        return found;
    }

    public void createRequest(long duration) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jdbcTestPrj_3?sleepTime=" + duration);
        CommonTasks.writeLogMsg(Level.INFO, "Calling jdbcTestPrj_3 Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        br.readLine();
    }

    class RequestThread extends Thread {

        long duration = 11000;

        public RequestThread() {
        }

        public RequestThread(long duration) {
            this.duration = duration;
        }

        @Override
        public void run() {
            try {
                createRequest(duration);
                CommonTasks.writeLogMsg(Level.INFO, "---> Finished Running Application");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class RemoveFeatureThread extends Thread {
        @Override
        public void run() {
            try {
                Thread.sleep(5000);
                CommonTasks.writeLogMsg(Level.INFO, "$$$$$ ---> Removing RequestTiming Feature");
                server.setServerConfigurationFile("server_NoRT.xml");
                waitForFeatureRemoval();
                CommonTasks.writeLogMsg(Level.INFO, "----> Server Configuration complete");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isRequestTimingEnabled() throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot("CWWKF0012I", MESSAGE_LOG);
        boolean enabled = false;
        for (String line : lines) {
            if (line.contains("requestTiming-1.0")) {
                enabled = true;
            }
        }
        return enabled;
    }

    private boolean isEventLoggingEnabled() throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot("CWWKF0012I", MESSAGE_LOG);
        boolean enabled = false;
        for (String line : lines) {
            if (line.contains("eventLogging-1.0")) {
                enabled = true;
            }
        }
        return enabled;
    }

    private boolean isRequestTimingRemoved() throws Exception {
        server.waitForStringInLogUsingLastOffset("CWWKF0013I", 60000);
        List<String> lines = server.findStringsInFileInLibertyServerRoot("CWWKF0013I", MESSAGE_LOG);
        boolean removed = false;
        for (String line : lines) {
            if (line.contains("requestTiming-1.0")) {
                removed = true;
            }
        }
        return removed;
    }

    private boolean isEventLoggingRemoved() throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot("CWWKF0013I", MESSAGE_LOG);
        boolean removed = false;
        for (String line : lines) {
            if (line.contains("eventLogging-1.0")) {
                removed = true;
            }
        }
        return removed;
    }

    private void waitForFeatureAddition() throws Exception {
        Thread.sleep(2000);
        server.waitForStringInLogUsingMark("CWWKG0017I", 30000);
        server.waitForStringInLogUsingMark("CWWKF0012I", 30000);
        printMessageToFATlog("CWWKF0012I");
        server.waitForStringInLogUsingMark("CWWKF0008I", 60000);
        printMessageToFATlog("CWWKF0008I");
    }

    private void printMessageToFATlog(String searchText) throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot(searchText, MESSAGE_LOG);
        CommonTasks.writeLogMsg(Level.INFO, "----> Results of " + searchText + " search : " + lines.size());
        for (String line : lines) {
            CommonTasks.writeLogMsg(Level.INFO, "----> line : " + line);
        }

    }

    private void waitForFeatureRemoval() throws Exception {
        Thread.sleep(2000);
        server.waitForStringInLogUsingMark("CWWKG0017I", 30000); //server config updated message
        server.waitForStringInLogUsingMark("CWWKF0013I", 30000); // feature removed message
    }

    @After
    public void tearDown() throws Exception {
        //server.setServerConfigurationFile("server_NoFeatures.xml");
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
        server.setServerConfigurationFile("server_NoFeatures.xml");
        if (server != null && !server.isStarted()) {
            server.startServer();
        }
    }

}