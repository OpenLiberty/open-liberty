/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/*
 * Test UpgradeHandler which is sent together with first batch of data (i.e send data before "101 Switching Protocols" response is received)
 * Application's ReadListener should be notified of data available for reading.
 * Application is then read and verify if all expected data has received then echo back all the received data with "PASS" string.
 * Otherwise, reply with "FAIL", if there is any missing data, for this client to assert.
 */

@RunWith(FATRunner.class)
public class UpgradeReadListenerSendImmediateData {
    private static final Logger LOG = Logger.getLogger(UpgradeReadListenerSendImmediateData.class.getName());

    private static final String APP_NAME = "UpgradeReadListenerSendImmediateData";
    private static final String WAR_NAME = APP_NAME + ".war";
    private static final String SERVLET = "TestServlet";
    private static final String UPGRADE_HANDLER_SERVLET_URL = "/" + APP_NAME + "/" + SERVLET;
    private static String URLString;
    private static final int DATA_SIZE = 50; //If modify, update app TestReadListener.DATA_SIZE as well.

    @Server("servlet31_upgradeReadListenerNoWaitServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setupClass() throws Exception {
        LOG.info("Setup : servlet31_upgradeReadListenerNoWaitServer");

        URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + UPGRADE_HANDLER_SERVLET_URL;
        WebArchive upgradeReadListenerApp = ShrinkHelper.buildDefaultApp(WAR_NAME, "readlistener");

        //needs permissions.xml for socket
        upgradeReadListenerApp = (WebArchive) ShrinkHelper.addDirectory(upgradeReadListenerApp, "test-applications/" + WAR_NAME + "/resources");

        // Export the application.
        ShrinkHelper.exportDropinAppToServer(server, upgradeReadListenerApp);
        server.startServer(UpgradeReadListenerSendImmediateData.class.getSimpleName() + ".log");

        LOG.info("Setup : startServer, ready for test");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testUpgradeReadListenerSendImmediateData() throws Exception {
        LOG.info("\n RUNNING TEST : testUpgradeReadListenerSendImmediateData");
        Socket s = null;
        try {
            s = CreateSocketConnection();
            String responseText = upgradeAndSendImmediateData(URLString, DATA_SIZE, s);
            assertTrue("Expecting PASS response but found [" + responseText + "]", responseText.contains("PASS"));
        } catch (Exception e) {
            LOG.info(" Exception testUpgradeReadListenerSendImmediateData ");
            e.printStackTrace();
            fail("Exception from request in testUpgradeReadListenerSendImmediateData: " + e.getMessage() + "exception is: " + e.toString());
        } finally {
            if (!(s.isClosed())) {
                s.shutdownInput();
                s.shutdownOutput();
                s.close();
            }

            LOG.info("\n FINISH RUNNING TEST : testUpgradeReadListenerSendImmediateData");
        }
    }

    private Socket CreateSocketConnection() throws Exception {
        URL url = new URL(URLString);
        String host = url.getHost();
        int port = url.getPort();
        Socket s = new Socket(host, port); // create socket with host and port derived from url
        return s;
    }

    /*
     * Send Upgrade headers, followed immediately by data.
     * Then sleep to slow down the send to allow some data arrive to the server before 101 while others arrive after 101
     */
    private String upgradeAndSendImmediateData(String urlToConnect, int dataSize, Socket soc) throws Exception {
        LOG.info(" upgradeAndSendImmediateData ENTER");
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(soc.getOutputStream()));
        BufferedReader input = new BufferedReader(new InputStreamReader(soc.getInputStream()));
        String CRLF = "\r\n";
        int sleep = 300;

        output.write("POST " + urlToConnect + " HTTP/1.1" + CRLF);
        output.write("Host: " + urlToConnect + CRLF);
        output.write("Upgrade: TestUpgrade" + CRLF);
        output.write("Connection: Upgrade" + CRLF);
        output.write(CRLF);
        output.flush();

        //Send data immediately after the headers
        writeFlush(output, "BEGIN,Data_0,");

        //Data in this loop can make to server before or after `101 Switching Protocols`.
        for (int i = 1; i <= dataSize; i++) {
            writeFlush(output, "Data_" + i + ",");
            if (i % 5 == 0) { //simulate slow sending
                Thread.sleep(sleep);
            }

            if (i == dataSize) {
                writeFlush(output, "END");
            }
        }

        String line = "";
        LOG.info("Response =============");

        //keep reading the response until either PASS or FAIL return.
        while ((line = input.readLine()) != null) {
            LOG.info("\t" + line);

            if (line.contains("PASS")) {
                LOG.info("Response contains PASS");
                break;
            } else if (line.contains("FAIL")) {
                LOG.info("Response contains FAIL");
                break;
            }
        }

        LOG.info("upgradeAndSendImmediateData EXIT");

        return line;
    }

    private static void writeFlush(BufferedWriter out, String data) throws IOException {
        if (data != null) {
            out.write(data);
        }
        out.flush();
    }
}
