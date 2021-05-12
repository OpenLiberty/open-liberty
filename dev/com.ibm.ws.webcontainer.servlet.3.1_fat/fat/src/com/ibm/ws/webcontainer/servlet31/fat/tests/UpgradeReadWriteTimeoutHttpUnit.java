/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static org.junit.Assert.assertEquals;

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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * The Servlet 3.1 test bucket class to test out the custom properties added for Servlet 3.1
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class UpgradeReadWriteTimeoutHttpUnit {

    private static final Logger LOG = Logger.getLogger(UpgradeReadWriteTimeoutHttpUnit.class.getName());

    private static final String LIBERTY_READ_WRITE_LISTENER_APP_NAME = "LibertyReadWriteListenerTest";

    @Server("servlet31_wcServerReadWriteUpgradeTimeoutTests")
    public static LibertyServer readWriteUpgradeTimeoutServer;

    private static final String UPGRADE_HANDLER_SERVLET_URL = "/LibertyReadWriteListenerTest/UpgradeHandlerTestServlet";

    /*
     * Test for the Upgrade timeout custom property which was added. For this test the property
     * is set to 5 seconds
     */

    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the war app and add the dependencies
        WebArchive LibertyReadWriteListenerApp = ShrinkHelper.buildDefaultApp(LIBERTY_READ_WRITE_LISTENER_APP_NAME + ".war",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.readListener",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.writeListener",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.upgradeHandler");
        LibertyReadWriteListenerApp = (WebArchive) ShrinkHelper.addDirectory(LibertyReadWriteListenerApp, "test-applications/LibertyReadWriteListenerTest.war/resources");

        // Export the application.
        ShrinkHelper.exportDropinAppToServer(readWriteUpgradeTimeoutServer, LibertyReadWriteListenerApp);

        // Start the server and use the class name so we can find logs easily.
        readWriteUpgradeTimeoutServer.startServer(UpgradeReadWriteTimeoutHttpUnit.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        if (readWriteUpgradeTimeoutServer != null && readWriteUpgradeTimeoutServer.isStarted()) {
            readWriteUpgradeTimeoutServer.stopServer();
        }
    }

    @Test
    public void testUpgradeReadListenerTimeout() throws IOException, InterruptedException {

        try {

            String URLString = "http://" + readWriteUpgradeTimeoutServer.getHostname() + ":" + readWriteUpgradeTimeoutServer.getHttpDefaultPort() + UPGRADE_HANDLER_SERVLET_URL;
            URL url = new URL(URLString);
            BufferedWriter output = null;
            BufferedReader input = null;

            //setup a socket connection to the servlet for testing upgrade.

            String host = url.getHost();
            int port = url.getPort();
            Socket s = null;
            s = new Socket(host, port);
            output = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            String CRLF = "\r\n";
            LOG.info("url for testUpgradeReadListenerTimeout() is  " + URLString);
            LOG.info("Running testUpgradeReadListenerTimeout() test, send dataString  ");
            output.write("POST " + URLString + " HTTP/1.1" + CRLF);
            output.write("Host: " + url + CRLF);
            output.write("TestType: testUpgradeReadListenerTimeout" + CRLF);
            output.write("Upgrade: TestUpgrade" + CRLF);
            output.write("Connection: Upgrade" + CRLF);
            output.write(CRLF);
            output.flush();
            LOG.info("finished writing upgrade request to the server in test testUpgradeReadListenerTimeout()");
            LOG.info("Reading upgrade response from the server in testUpgradeReadListenerTimeout()");

            //test if upgrade response message is received back
            input = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String line = "";
            while ((line = input.readLine()) != null) {
                LOG.info("\t" + line);
                if (line.trim().equals(""))
                    break;
            }
            LOG.info("Received Upgrade response from server, now upgraded");
            LOG.info("Sending data to the server");

            //Send a small piece of data to the servlet, then wait 10 seconds, then send the other piece
            //On the servlet side it should timeout within 5 seconds then send back a response, which
            //is the first testString, then something saying it was a timeout
            String testString = "0123456789";
            String testStringContinued = "abcdefghijklmnopqrstuvwxyz";
            output.write(testString);
            output.flush();
            LOG.info("Sent the first part of the test string");

            Thread.sleep(10000);

            output.write(testStringContinued);

            output.flush();
            LOG.info("Sent the test string, reading the data");

            //Read the first part of the output from the server

            String line1 = input.readLine();
            LOG.info("Read from the test app: " + line1);

            LOG.info("Done with the test!");

            //close input and output streams and socket.
            output.close();
            input.close();
            s.close();

            //test if servlet read our data and sent it back.
            assertEquals(testString + ", A Timeout has been triggered", line1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Test for the Upgrade timeout custom property which was added. For this test the property
     * is set to 5 seconds
     */
    @Test
    public void testUpgradeWriteListenerTimeout() throws IOException, InterruptedException {

        try {

            readWriteUpgradeTimeoutServer.setMarkToEndOfLog();
            String URLString = "http://" + readWriteUpgradeTimeoutServer.getHostname() + ":" + readWriteUpgradeTimeoutServer.getHttpDefaultPort() + UPGRADE_HANDLER_SERVLET_URL;
            URL url = new URL(URLString);
            BufferedWriter output = null;
            BufferedReader input = null;

            //setup a socket connection to the servlet for testing upgrade.

            String host = url.getHost();
            int port = url.getPort();
            Socket s = null;
            s = new Socket(host, port);
            output = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            String CRLF = "\r\n";
            LOG.info("url for testUpgradeWriteListenerTimeout() is  " + URLString);
            LOG.info("Running testUpgradeWriteListenerTimeout() test, send dataString  ");
            output.write("POST " + URLString + " HTTP/1.1" + CRLF);
            output.write("Host: " + url + CRLF);
            output.write("TestType: test_Timeout_UpgradeWL" + CRLF);
            output.write("Upgrade: TestUpgrade" + CRLF);
            output.write("Connection: Upgrade" + CRLF);
            output.write(CRLF);
            output.flush();
            LOG.info("finished writing upgrade request to the server in test testUpgradeWriteListenerTimeout()");
            LOG.info("Reading upgrade response from the server in testUpgradeWriteListenerTimeout()");

            //test if upgrade response message is received back
            input = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String line = "";
            while ((line = input.readLine()) != null) {
                LOG.info("\t" + line);
                if (line.trim().equals(""))
                    break;
            }
            LOG.info("Received Upgrade response from server, now upgraded");
            LOG.info("Sending data to the server");

            //At this point we just want to not read for a long while. The reasoning for this
            //is the TCP receive buffers should get full and eventually trigger an async write
            //at the server side.

            String stringInLogs = readWriteUpgradeTimeoutServer
                            .waitForStringInLogUsingMark("test_Timeout_UpgradeWL : Timeout occurred during the test",
                                                         15000);

            assertEquals(true, stringInLogs.contains("test_Timeout_UpgradeWL : Timeout occurred during the test"));

            output.close();
            input.close();
            s.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
