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
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
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

@RunWith(FATRunner.class)
public class UpgradeReadListenerHttpUnit {

    private static final Logger LOG = Logger.getLogger(UpgradeReadListenerHttpUnit.class.getName());

    private static final String LIBERTY_READ_WRITE_LISTENER_APP_NAME = "LibertyReadWriteListenerTest";

    @Server("servlet31_wcServer")
    public static LibertyServer server;

    private static final String UPGRADE_HANDLER_SERVLET_URL = "/LibertyReadWriteListenerTest/UpgradeHandlerTestServlet";
    private static String URLString;

    private static ArrayList<Socket> connections;

    @BeforeClass
    public static void setupClass() throws Exception {
        URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + UPGRADE_HANDLER_SERVLET_URL;
        connections = new ArrayList<Socket>();
        WebArchive libertyReadWriteListenerApp = ShrinkHelper.buildDefaultApp(LIBERTY_READ_WRITE_LISTENER_APP_NAME + ".war",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.readListener",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.writeListener",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.upgradeHandler");
        libertyReadWriteListenerApp = (WebArchive) ShrinkHelper.addDirectory(libertyReadWriteListenerApp, "test-applications/LibertyReadWriteListenerTest.war/resources");

        // Export the application.
        ShrinkHelper.exportDropinAppToServer(server, libertyReadWriteListenerApp);

        // Start the server and use the class name so we can find logs easily.
        server.startServer(UpgradeReadListenerHttpUnit.class.getSimpleName() + ".log");
    }

    //After each test is complete it will add it's connection to the list of connections to close. Then when the tests are all done it will close them all at once
    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("\n UpgradeReadListenerHttpUnit Cleaning up the connections");
        for (Socket s : connections) {
            if (!(s.isClosed())) // check if socket is closed before closing it.
            {
                s.shutdownInput(); // shutting down socket input.
                s.shutdownOutput(); // shutting down socket output.
                s.close(); //finally we close the socket.
            }
        }

        connections.clear();
        connections = null;
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE8015E:.*");
        }
    }

    @Test
    /*
     * Basic Test for upgrade, where no upgrade header is set. The servlet should not upgrade
     * to a new protocol. Servlet returns a string "NoUpgrade" without any upgrade happenning.
     * Test passes
     */
    public void testNoUpgrade() throws Exception {
        LOG.info("\n *****************START**********UpgradeReadListenerHttpUnit: RUNNING TEST : testNoUpgrade");
        URL url = null;
        HttpURLConnection con = null;
        InputStream is = null;
        BufferedReader rd = null;

        try {
            url = new URL(URLString);
        } catch (MalformedURLException e1) {
            LOG.info("error in creating url in test : testNoUpgrade ");
            e1.printStackTrace();
            fail("Exception from request in testNoUpgrade: " + e1.getMessage() + "exception is: " + e1.toString());
        }
        try {
            //set up the connection
            con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("TestName", "testNoUpgrade");
            con.connect();
            //get response code
            int ResponseCode = con.getResponseCode();
            LOG.info("Response code in testNoUpgrade() in class UpgradeReadListenerHttpUnit is " + ResponseCode);
            StringBuilder sb = new StringBuilder();
            String line = "";
            //get input from the servlet.
            is = con.getInputStream();
            rd = new BufferedReader(new InputStreamReader(is));
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            LOG.info("Contents of sb in testNoUpgrade() in class UpgradeReadListenerHttpUnit is " + sb.toString());
            // check no Upgrade happens.
            assertEquals("NoUpgrade", sb.toString());
        } catch (Exception e) {

            LOG.info("exception in test testNoUpgrade()  in class UpgradeReadListenerHttpUnit ");
            e.printStackTrace();
            fail("Exception from request in testNoUpgrade: " + e.getMessage() + "exception is: " + e.toString());
        } finally {
            //closing input stream , read buffer and connection

            is.close();
            rd.close();
            con.disconnect();
            LOG.info("\n *****************FINISH**********UpgradeReadListenerHttpUnit: RUNNING TEST : testNoUpgrade");
        }

    }

    /*
     * Basic test for upgrade: read small data, where we send a small data String ("0123456789abcdefghijklmnopqrstuvwxyz") and make sure that
     * the servlet has received this data. Test passes.
     */
    @Test
    public void testUpgradeReadListenerSmallData() throws Exception {

        LOG.info("\n *****************START**********UpgradeReadListenerHttpUnit: RUNNING TEST : testUpgradeReadListenerSmallData");
        Socket s = null;
        try {
            s = CreateSocketConnection();
            String testString = "0123456789abcdefghijklmnopqrstuvwxyz";
            String line1 = upgradeClientServerInputOutput(URLString, "testUpgradeReadListenerSmallData", testString, 0, s);
            LOG.info("Returned line from server helper method is " + line1);
            assertEquals(testString, line1);
        } catch (Exception e) {
            LOG.info("exception in test testUpgradeReadListenerSmallData()  in class UpgradeReadListenerHttpUnit ");
            e.printStackTrace();
            fail("Exception from request in testUpgradeReadListenerSmallData: " + e.getMessage() + "exception is: " + e.toString());

        }

        finally {
            connections.add(s);
            LOG.info("\n *****************FINISH**********UpgradeReadListenerHttpUnit: RUNNING TEST : testUpgradeReadListenerSmallData");
        }

    }

    /*
     * send large data sizes to the readListener in upgrade mode . We send
     * a series of 1s of a specified data size,and ask server to send back
     * the size of the received data. Test passes.
     */
    @Test
    public void testUpgradeReadListenerLargeData() throws Exception {

        LOG.info("\n *****************START**********UpgradeReadListenerHttpUnit: RUNNING TEST: testUpgradeReadListenerLargeData");
        Socket s = null;
        try {

            s = CreateSocketConnection();
            String testString = "";
            long PostDataSize = 1000000;
            String line1 = upgradeClientServerInputOutput(URLString, "testUpgradeReadListenerLargeData", testString, 1000000, s);
            LOG.info("Returned line from serverhelper method is " + line1);
            // check if size read, is equal to data size sent.
            assertEquals(Long.toString(PostDataSize), line1);
        } catch (Exception e) {
            LOG.info("exception in test testUpgradeReadListenerLargeData()  in class UpgradeReadListenerHttpUnit");
            e.printStackTrace();
            fail("Exception from request in testUpgradeReadListenerLargeData: " + e.getMessage() + "exception is: " + e.toString());

        } finally {
            connections.add(s);
            LOG.info("\n *****************FINISH**********UpgradeReadListenerHttpUnit: RUNNING TEST: testUpgradeReadListenerLargeData");
        }

    }

    /*
     * send small data to be read in the handler.
     * Test passes.
     */
    @Test
    public void testUpgradeReadSmallDataInHandler() throws Exception {

        LOG.info("\n *****************START**********UpgradeReadListenerHttpUnit: RUNNING TEST : testUpgradeReadSmallDataInHandler");
        Socket s = null;
        try {

            s = CreateSocketConnection();
            String testString = "123456789abcdefghijklmnopqrstuvwxyz";
            String line1 = upgradeClientServerInputOutput(URLString, "testUpgradeReadSmallDataInHandler", testString, 0, s);
            LOG.info("Returned line from serverhelper method is " + line1);
            assertEquals("OK", line1);
        } catch (Exception e) {
            LOG.info("exception in test testUpgradeReadSmallDataInHandler()  in class UpgradeReadListenerHttpUnit ");
            e.printStackTrace();
            fail("Exception from request in testUpgradeReadSmallDataInHandler: " + e.getMessage() + "exception is: " + e.toString());

        } finally {
            connections.add(s);
            LOG.info("\n *****************FINISH**********UpgradeReadListenerHttpUnit: RUNNING TEST : testUpgradeReadSmallDataInHandler");
        }

    }

    /*
     * read small data, where we send a small data String ("0123456789") and make sure that
     * the servlet has received this data. Test passes.
     */
    @Test
    public void testUpgrade_WriteListener_From_ReadListener() throws Exception {

        LOG.info("\n *****************START**********UpgradeReadListenerHttpUnit: RUNNING TEST : testUpgrade_WL_From_RL");
        Socket s = null;
        try {
            s = CreateSocketConnection();
            String testString = "0123456789";
            String line1 = upgradeClientServerInputOutput(URLString, "testUpgrade_WriteListener_From_ReadListener", testString, 0, s);
            LOG.info("Returned line from server helper method is " + line1);
            assertEquals(testString, line1);
        } catch (Exception e) {
            LOG.info("exception in test testUpgrade_WL_From_RL()  in class UpgradeReadListenerHttpUnit ");
            e.printStackTrace();
            fail("Exception from request in testUpgrade_WL_From_RL: " + e.getMessage() + "exception is: " + e.toString());

        }

        finally {
            connections.add(s);
            LOG.info("\n *****************FINISH**********UpgradeReadListenerHttpUnit: RUNNING TEST : testUpgrade_WL_From_RL");
        }

    }

    /*
     * Verify in the ReadListener that the Contexts have transfered properly
     * Test passes
     */
    @Test
    public void testRead_ContextTransferProperly_WhenUpgradeRLSet() throws Exception {
        LOG.info("\n *****************START**********UpgradeReadListenerHttpUnit: RUNNING TEST : testRead_ContextTransferProperly_WhenUpgradeRLSet");
        Socket s = null;

        try {
            s = CreateSocketConnection();
            String testString = "javax.naming.NameNotFoundException: java:comp/UserTransaction";
            String line1 = upgradeClientServerInputOutput(URLString, "testRead_ContextTransferProperly_WhenUpgradeRLSet", testString, 0, s);

            LOG.info("Returned line from serverhelper method is " + line1);

            assertEquals("PASSED", line1);
        } catch (Exception e) {
            LOG.info("exception in test testRead_ContextTransferProperly_WhenUpgradeRLSet()  in class UpgradeReadListenerHttpUnit ");
            e.printStackTrace();
            fail("Exception from request in testRead_ContextTransferProperly_WhenUpgradeRLSet: " + e.getMessage() + "exception is: " + e.toString());
        } finally {
            connections.add(s);
            LOG.info("\n *****************FINISH**********UpgradeReadListenerHttpUnit: RUNNING TEST : testRead_ContextTransferProperly_WhenUpgradeRLSet");
        }
    }

    // helper method to connect to the server, through a socket. The method returns a connection socket.
    private Socket CreateSocketConnection() throws Exception {
        URL url = new URL(URLString);
        String host = url.getHost();
        int port = url.getPort();
        Socket s = new Socket(host, port); // create socket with host and port derived from url
        return s;
    }

    //helper method common to all tests, that read and write from the server, send and receive appropriate output and input for each test through a socket, passed from the test.
    private String upgradeClientServerInputOutput(String urlToConnect, String testName, String DataToSend, long DataSentSize, Socket soc) throws Exception {
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(soc.getOutputStream()));
        BufferedReader input = new BufferedReader(new InputStreamReader(soc.getInputStream()));
        String line1 = "";

        //setup a socket connection to the servlet for testing upgrade.

        String CRLF = "\r\n";

        // send post message to servlet requesting for upgrade.

        output.write("POST " + urlToConnect + " HTTP/1.1" + CRLF);
        output.write("Host: " + urlToConnect + CRLF);
        output.write("TestType: " + testName + CRLF);
        output.write("Upgrade: TestUpgrade" + CRLF);
        output.write("Connection: Upgrade" + CRLF);
        output.write(CRLF);
        output.flush();
        //test if upgrade response message is received back

        String line = "";
        while ((line = input.readLine()) != null) {
            LOG.info("\t" + line);
            if (line.trim().equals(""))
                break;
        }
        LOG.info("Received Upgrade response from server, now upgraded");
        LOG.info("Sending data to the server");

        //send input to the servlet. Varies according to test.

        if ((testName.equals("testUpgradeReadListenerSmallData")) || (testName.equals("testUpgradeReadSmallDataInHandler"))
            || (testName.equals("testUpgradeReadListenerSmallDataThrowException")) || (testName.equals("testUpgrade_WriteListener_From_ReadListener"))
            || testName.equals("testRead_ContextTransferProperly_WhenUpgradeRLSet")) {

            output.write(DataToSend);
            output.flush();
            LOG.info("Sent the test string, reading the data");
        } else if (testName.equals("testUpgradeReadListenerLargeData")) {
            for (long i = 0; i < DataSentSize; i++) {
                output.write(1);
            }
            output.write("Sending Data Complete");
            output.flush();
        }

        //get output from the servlet

        line1 = input.readLine();

        LOG.info("Read from the test app: " + line1);
        //return data to the tests, read from the server.
        return line1;

    }
}
