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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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

@RunWith(FATRunner.class)
public class UpgradeWriteListenerHttpUnit {

    @Server("servlet31_wcServer")
    public static LibertyServer server;

    private static final String LIBERTY_READ_WRITE_LISTENER_APP_NAME = "LibertyReadWriteListenerTest";

    private static String URLString;
    private static final Logger LOG = Logger.getLogger(UpgradeWriteListenerHttpUnit.class.getName());
    private BufferedReader input = null;

    @BeforeClass
    public static void setupClass() throws Exception {
        URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/LibertyReadWriteListenerTest/UpgradeHandlerTestServlet";

        WebArchive libertyReadWriteListenerApp = ShrinkHelper.buildDefaultApp(LIBERTY_READ_WRITE_LISTENER_APP_NAME + ".war",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.readListener",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.writeListener",
                                                                              "com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.upgradeHandler");
        libertyReadWriteListenerApp = (WebArchive) ShrinkHelper.addDirectory(libertyReadWriteListenerApp, "test-applications/LibertyReadWriteListenerTest.war/resources");

        // Export the application.
        ShrinkHelper.exportDropinAppToServer(server, libertyReadWriteListenerApp);

        // Start the server and use the class name so we can find logs easily.
        server.startServer(UpgradeWriteListenerHttpUnit.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE8015E:.*", "SRVE0918E:.*", "SRVE9009E:.*", "SRVE9005E:.*");
        }
    }

    /*
     * This test expects small data to be written out by the server after the upgrade.
     *
     * The servlet upgrades, and then sets a writeListener.
     * Then, the write Listener writes out a small data chunk to this client .
     *
     * This "Test passes" if the client verifies the data is the expected data.
     */
    @Test
    public void test_SmallData_UpgradeWL() throws Exception {

        LOG.info("\n *****************START**********UpgradeWriteListenerHttpUnit: RUNNING TEST: test_SmallData_UpgradeWL");
        Socket s = null;
        boolean callUpgrade = false;
        String line1 = "";
        try {
            s = CreateSocketConnection();
            callUpgrade = checkif_UpgradeRecvd(0, "test_SmallData_UpgradeWL", s);
            if (callUpgrade == true) {
                try {
                    line1 = writeListener_SmallDataSize(s);
                } catch (Exception e) {
                    LOG.info("exception as a result of the socket, socket s is " + s);
                    e.printStackTrace();
                }

                LOG.info("Read from the test app: " + line1);
                String test = "0123456789";
                //check if data received, from write by writeListener is the expected data.
                assertEquals(test, line1);
            } else {
                fail("TEST FAILURE: test_SmallData_UpgradeWL: " + " upgrade request to server failed, upgrade did not happen ");
            }

        } catch (Exception e) {
            LOG.info("exception in test test_SmallData_UpgradeWL()  in class UpgradeWriteListenerHttpUnit ");
            e.printStackTrace();
            fail("Exception from request in test_SmallData_UpgradeWL: " + e.getMessage() + "exception is: " + e.toString());
        } finally {
            // close the socket
            CloseSocketConnection(s);
            LOG.info("\n *****************FINISH**********UpgradeWriteListenerHttpUnit: RUNNING TEST: test_SmallData_UpgradeWL");
        }

    }

    /*
     * This test expects large data to be written out by the server after the upgrade.
     *
     * The servlet upgrades, and then sets a writeListener.
     * The writeListener writes out large data to the client by writing data chunks that are in a queue data structure.
     * multiple chunks of data are written out from the server side, to together constitute a large data size.
     *
     * This "Test passes" if the client verifies the size of the data received from the server is equal to a expected data size.
     */
    @Test
    public void test_LargeDataInChunks_UpgradeWL() throws Exception {
        // test async write Listener by asking the servlet to send large chunk data of size 1000000 bytes.

        LOG.info("\n *****************START**********UpgradeWriteListenerHttpUnit: RUNNING TEST : test_LargeDataInChunks_UpgradeWL");
        Socket s = null;
        int expectedResponseSize = 1000000;
        boolean callUpgrade = false;
        try {
            s = CreateSocketConnection();
            callUpgrade = checkif_UpgradeRecvd(expectedResponseSize, "test_LargeDataInChunks_UpgradeWL", s);
            if (callUpgrade == true) {
                int actualResponseSize = readResponse_returnTotal(s);// call helper method to run test for writeListener with large data sizes.
                LOG.info("Returned data from server is " + actualResponseSize);
                assertEquals(expectedResponseSize, actualResponseSize); //check if the correct dataSize is received.
            } else {
                fail("TEST FAILURE: test_LargeDataInChunks_UpgradeWL: " + " upgrade request to server failed, upgrade did not happen ");
            }
        } catch (Exception e) {
            LOG.info("exception in test test_LargeDataInChunks_UpgradeWL  in class UpgradeWriteListenerHttpUnit ");
            e.printStackTrace();
            fail("Exception from request in test_LargeDataInChunks_UpgradeWL: " + e.getMessage() + "exception is: " + e.toString());

        } finally {
            // close the socket.
            CloseSocketConnection(s);
            LOG.info("\n *****************FINISH**********UpgradeWriteListenerHttpUnit: RUNNING TEST : test_LargeDataInChunks_UpgradeWL");
        }

    }

    /*
     * This test expects large data to be written out by the server after the upgrade.
     *
     *
     * One large data chunk is written out from the WriteListener.
     *
     * This "Test passes" if the client verifies the size of the data received from the server is equal to a expected data size.
     */

    @Test
    public void test_SingleWriteLargeData1000000__UpgradeWL() throws Exception {
        // test async write Listener by asking the servlet to send large chunk data of size 1000000 bytes.

        LOG.info("\n *****************START**********UpgradeWriteListenerHttpUnit: RUNNING TEST : test_SingleWriteLargeData1000000__UpgradeWL");
        Socket s = null;
        int expectedResponseSize = 1000000;
        boolean callUpgrade = false;
        try {
            s = CreateSocketConnection();
            callUpgrade = checkif_UpgradeRecvd(expectedResponseSize, "test_SingleWriteLargeData1000000__UpgradeWL", s);
            if (callUpgrade == true) {
                int actualResponseSize = readResponse_returnTotal(s);// call helper method to run test for writeListener with large data sizes.
                LOG.info("Returned data from server is " + actualResponseSize);
                assertEquals(expectedResponseSize, actualResponseSize); //check if the correct dataSize is received.
            } else {
                fail("TEST FAILURE:test_SingleWriteLargeData1000000__UpgradeWL: " + " upgrade request to server failed, upgrade did not happen ");
            }

        } catch (Exception e) {
            LOG.info("exception in test test_SingleWriteLargeData1000000__UpgradeWL()  in class UpgradeWriteListenerHttpUnit ");
            e.printStackTrace();
            fail("Exception from request in test_SingleWriteLargeData1000000__UpgradeWL: " + e.getMessage() + "exception is: " + e.toString());

        } finally {
            CloseSocketConnection(s);
            LOG.info("\n *****************FINISH**********UpgradeWriteListenerHttpUnit: RUNNING TEST : test_SingleWriteLargeData1000000__UpgradeWL");
        }
    }

    /*
     * This test expects small data to be written out by the server, from the handler without a writeListener, after the upgrade.
     *
     * This "Test passes" if the client verifies the data is the expected data.
     */
    @Test
    public void test_SmallDataInHandler_NoWriteListener() throws Exception {

        LOG.info("\n *****************START**********UpgradeWriteListenerHttpUnit: RUNNING TEST : test_SmallDataInHandler_NoWriteListener");
        Socket s = null;
        boolean callUpgrade = false;
        String line1 = "";
        try {
            s = CreateSocketConnection();
            callUpgrade = checkif_UpgradeRecvd(0, "test_SmallDataInHandler_NoWriteListener", s);
            if (callUpgrade == true) {

                line1 = writeListener_SmallDataSize(s);
                LOG.info("Read from the test app: " + line1);
                String test = "0123456789";
                //check if data received, from write by Handler is the expected data.
                assertEquals(test, line1);
            } else {
                fail("TEST FAILURE: test_SmallDataInHandler_NoWriteListener: " + " upgrade request to server failed, upgrade did not happen ");
            }

        } catch (Exception e) {
            LOG.info("exception in test test_SmallDataInHandler_NoWriteListener()  in class UpgradeWriteListenerHttpUnit ");
            e.printStackTrace();
            fail("Exception from request in test_SmallDataInHandler_NoWriteListener: " + e.getMessage() + "exception is: " + e.toString());

        } finally {
            CloseSocketConnection(s);
            LOG.info("\n *****************FINISH**********UpgradeWriteListenerHttpUnit: RUNNING TEST : test_SmallDataInHandler_NoWriteListener");
        }
    }

    /*
     * This Test closes the web connection and then closes the webContainer.
     *
     * added some sleep after setWriteListener is called from handler. write small data from listener and then call close.
     * We need to check on behavior on webcontainer close when the listener thread finishes faster and had already called closed.
     *
     * This "Test passes" if the client verifies the data is the expected data.
     */

    @Test
    public void test_Close_WebConnection_Container_UpgradeWL() throws Exception {

        LOG.info("\n *****************START**********UpgradeWriteListenerHttpUnit: RUNNING TEST: test_Close_WebConnection_Container_UpgradeWL");
        Socket s = null;
        boolean callUpgrade = false;
        String line1 = "";
        try {
            s = CreateSocketConnection();
            callUpgrade = checkif_UpgradeRecvd(0, "test_Close_WebConnection_Container_UpgradeWL", s);
            if (callUpgrade == true) {

                line1 = writeListener_SmallDataSize(s);
                LOG.info("Read from the test app: " + line1);
                String test = "0123456789";
                //check if data received, from write by writeListener is the expected data.
                assertEquals(test, line1);
            } else {
                fail("TEST FAILURE: test_Close_WebConnection_Container_UpgradeWL: " + " upgrade request to server failed, upgrade did not happen ");
            }
        } catch (Exception e) {
            LOG.info("exception in test_Close_WebConnection_Container_UpgradeWL()  in class UpgradeWriteListenerHttpUnit ");
            e.printStackTrace();
            fail("Exception from request in test_Close_WebConnection_Container_UpgradeWL: " + e.getMessage() + "exception is: " + e.toString());

        } finally {
            CloseSocketConnection(s);
            LOG.info("\n *****************FINISH**********UpgradeWriteListenerHttpUnit: RUNNING TEST: test_Close_WebConnection_Container_UpgradeWL");
        }
    }

    @Test
    public void test_ContextTransferProperly_UpgradeWL() throws Exception {

        LOG.info("\n *****************START**********UpgradeWriteListenerHttpUnit: RUNNING TEST: test_ContextTransferProperly_UpgradeWL");
        Socket s = null;
        boolean callUpgrade = false;
        String line1 = "";
        try {
            s = CreateSocketConnection();
            callUpgrade = checkif_UpgradeRecvd(0, "test_ContextTransferProperly_UpgradeWL", s);
            if (callUpgrade == true) {

                line1 = writeListener_SmallDataSize(s);
                LOG.info("Read from the test app: " + line1);
                String test = "javax.naming.NameNotFoundException: java:comp/UserTransaction";
                //check if data received, from write by writeListener is the expected data.
                assertEquals(test, line1);
            } else {
                fail("TEST FAILURE: test_ContextTransferProperly_UpgradeWL: " + " upgrade request to server failed, upgrade did not happen ");
            }
        } catch (Exception e) {
            LOG.info("exception in test_ContextTransferProperly_UpgradeWL()  in class UpgradeWriteListenerHttpUnit ");
            e.printStackTrace();
            fail("Exception from request in test_ContextTransferProperly_UpgradeWL: " + e.getMessage() + "exception is: " + e.toString());

        } finally {
            CloseSocketConnection(s);
            LOG.info("\n *****************FINISH**********UpgradeWriteListenerHttpUnit: RUNNING TEST: test_ContextTransferProperly_UpgradeWL");
        }
    }

    //This is misspelled on purpose
    @Test
    @Mode(TestMode.LITE)
    public void TestWrite_DontCheckisRedy_fromUpgradeWL() throws Exception {
        // Make sure the test framework knows that SRVE0918E is expected

        server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
        String testToCall = "TestWrite_DontCheckisRedy_fromUpgradeWL";

        LOG.info("\n *****************START**********UpgradeWriteListenerHttpUnit: RUNNING TEST:" + testToCall + "*************");

        Socket s = null;
        boolean callUpgrade = false;
        int expectedResponseSize = 100;
        try {
            s = CreateSocketConnection();
            callUpgrade = checkif_UpgradeRecvd(expectedResponseSize, testToCall, s);
            if (callUpgrade) {
                int actualResponseSize = readResponse_returnTotal(s);// call helper method to run test for writeListener with large data sizes.
                LOG.info("Returned data from server is " + actualResponseSize);
                assertEquals(expectedResponseSize, actualResponseSize); //check if the correct dataSize is received.

                //Also check for exception in logs

                String message = server.waitForStringInLogUsingMark("SRVE0918E");
                LOG.info(testToCall + " Entries found in log : " + message);
                assertNotNull("Could not find message", message);
            } else {
                fail("TEST FAILURE: " + testToCall + " upgrade request to server failed, upgrade did not happen ");
            }
        } catch (Exception e) {
            LOG.info("exception in test " + testToCall + " in class UpgradeWriteListenerHttpUnit ");
            e.printStackTrace();
            fail("Message from request in " + e.getMessage() + "exception is: " + e.toString());

        } finally {
            // close the socket.
            CloseSocketConnection(s);
            server.setMarkToEndOfLog();
            LOG.info("\n *****************FINISH**********UpgradeWriteListenerHttpUnit: RUNNING TEST :" + testToCall + "*************");
        }
    }

    @Test
    @Mode(TestMode.LITE)
    public void TestWriteFromHandler_AftersetWL() throws Exception {
        // Make sure the test framework knows that SRVE0918E is expected

        server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
        String testToCall = "TestWriteFromHandler_AftersetWL";

        LOG.info("\n *****************START**********UpgradeWriteListenerHttpUnit: RUNNING TEST:" + testToCall + "*************");
        int expectedResponseSize = 1000;
        Socket s = null;
        boolean callUpgrade = false;
        try {
            s = CreateSocketConnection();
            callUpgrade = checkif_UpgradeRecvd(expectedResponseSize, testToCall, s);
            if (callUpgrade) {
                int actualResponseSize = readResponse_returnTotal(s);// call helper method to run test for writeListener with large data sizes.
                LOG.info("Returned data from server is " + actualResponseSize);
                assertEquals(expectedResponseSize, actualResponseSize); //check if the correct dataSize is received.

                //Also check for this message in logs
//                String msg = "isReady always false from the thread which sets the WL";
//
//                String message = SHARED_SERVER.getLibertyServer().waitForStringInLogUsingMark(msg);
//                LOG.info(testToCall + " Entries found in log : " + message);
//                assertNotNull("Could not find message", message);

                //Also check for exception in logs
                String message = server.waitForStringInLogUsingMark("SRVE0918E");
                LOG.info(testToCall + " Entries found in log : " + message);
                assertNotNull("Could not find message", message);
            } else {
                fail("TEST FAILURE: " + testToCall + " upgrade request to server failed, upgrade did not happen ");
            }
        } catch (Exception e) {
            LOG.info("exception in test " + testToCall + " in class UpgradeWriteListenerHttpUnit ");
            e.printStackTrace();
            fail("Message from request in " + e.getMessage() + "exception is: " + e.toString());

        } finally {
            // close the socket.
            CloseSocketConnection(s);
            server.setMarkToEndOfLog();
            LOG.info("\n *****************FINISH**********UpgradeWriteListenerHttpUnit: RUNNING TEST :" + testToCall + "*************");
        }
    }

    /*
     * this test registers a second WriteListener. This is not permitted and the test throws an IllegalStateException.
     * test passes
     */
    @Test
    @Mode(TestMode.LITE)
    public void TestUpgrade_ISE_setSecondWriteListener() throws Exception {

        String testToCall = "TestUpgrade_ISE_setSecondWriteListener";
        // Make sure the test framework knows that SRVE9009E is expected

        server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
        LOG.info("\n *****************START**********UpgradeWriteListenerHttpUnit: RUNNING TEST:" + testToCall + "*************");
        int expectedResponseSize = 1000;
        Socket s = null;
        boolean callUpgrade = false;
        try {
            s = CreateSocketConnection();
            callUpgrade = checkif_UpgradeRecvd(expectedResponseSize, testToCall, s);
            if (callUpgrade) {
                int actualResponseSize = readResponse_returnTotal(s);// call helper method to run test for writeListener with large data sizes.
                LOG.info("Returned data from server is " + actualResponseSize);
                assertEquals(expectedResponseSize, actualResponseSize); //check if the correct dataSize is received.

                String expectedData1 = "java.lang.IllegalStateException";
                String expectedData2 = "SRVE9009E";

                String message = server.waitForStringInLogUsingMark(expectedData1);
                LOG.info(testToCall + " Entries found in log : " + message);
                assertNotNull("Could not find message", message);

                message = server.waitForStringInLogUsingMark(expectedData2);
                LOG.info(testToCall + " Entries found in log : " + message);
                assertNotNull("Could not find message", message);
            } else {
                fail("TEST FAILURE: " + testToCall + " upgrade request to server failed, upgrade did not happen ");
            }
        } catch (Exception e) {
            LOG.info("exception in test " + testToCall + " in class UpgradeWriteListenerHttpUnit ");
            e.printStackTrace();
            fail("Message from request in " + e.getMessage() + "exception is: " + e.toString());

        } finally {
            // close the socket.
            CloseSocketConnection(s);
            server.setMarkToEndOfLog();
            LOG.info("\n *****************FINISH**********UpgradeWriteListenerHttpUnit: RUNNING TEST :" + testToCall + "*************");
        }
    }

    @Test
    @Mode(TestMode.LITE)
    public void TestUpgrade_NPE_setNullWriteListener() throws Exception {

        String testToCall = "TestUpgrade_NPE_setNullWriteListener";
        // Make sure the test framework knows that SRVE9014E is expected

        server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
        LOG.info("\n *****************START**********UpgradeWriteListenerHttpUnit: RUNNING TEST:" + testToCall + "*************");
        int expectedResponseSize = 1000;
        Socket s = null;
        boolean callUpgrade = false;
        try {
            s = CreateSocketConnection();
            callUpgrade = checkif_UpgradeRecvd(expectedResponseSize, testToCall, s);
            if (callUpgrade) {
                int actualResponseSize = readResponse_returnTotal(s);// call helper method to run test for writeListener with large data sizes.
                LOG.info("Returned data from server is " + actualResponseSize);

                String expectedData = "SRVE9005E";

                String message = server.waitForStringInLogUsingMark(expectedData);
                LOG.info(testToCall + " Entries found in log : " + message);
                assertNotNull("Could not find message", message);

            } else {
                fail("TEST FAILURE: " + testToCall + " upgrade request to server failed, upgrade did not happen ");
            }
        } catch (Exception e) {
            LOG.info("exception in test " + testToCall + " in class UpgradeWriteListenerHttpUnit ");
            e.printStackTrace();
            fail("Message from request in " + e.getMessage() + "exception is: " + e.toString());

        } finally {
            // close the socket.
            CloseSocketConnection(s);
            server.setMarkToEndOfLog();
            LOG.info("\n *****************FINISH**********UpgradeWriteListenerHttpUnit: RUNNING TEST :" + testToCall + "*************");
        }
    }

    // TEST FINISH
    // helper method to connect to the server, through a socket. The method returns a connection socket.
    private Socket CreateSocketConnection() throws Exception {
        URL url = new URL(URLString);
        String host = url.getHost();
        int port = url.getPort();
        return (new Socket(host, port)); // create socket with host and port derived from url

    }

    //helper method to close down a socket, called from each test.
    private void CloseSocketConnection(Socket s) throws Exception {
        if (!(s.isClosed())) // check if socket is closed before closing it.
        {
            input = null;
            s.shutdownInput(); // shutting down socket input.
            s.shutdownOutput(); // shutting down socket output
            s.close(); //finally we close the socket.
        }

    }

    /**
     * @param ExpectdResponseSize
     * @param testName
     * @param soc
     * @return
     * @throws Exception
     */
    private boolean checkif_UpgradeRecvd(int ExpectdResponseSize, String testName, Socket soc) throws Exception {

        boolean upgradeRecvd = false;
        //setup connection to the servlet
        URL url = new URL(URLString);
        BufferedWriter output = null;
        //BufferedReader input = null;
        LOG.info("\n Request URL in upgradeHandler : " + URLString);

        try {
            output = new BufferedWriter(new OutputStreamWriter(soc.getOutputStream()));
            input = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            String CRLF = "\r\n";
            //send message to servlet asking to upgrade.
            LOG.info("sending the upgrade request to the server ");
            output.write("POST " + URLString + " HTTP/1.1" + CRLF);
            output.write("Host: " + url + CRLF);
            output.write("TestType: " + testName + CRLF);
            output.write("ContentSizeSent:" + Integer.toString(ExpectdResponseSize) + CRLF);
            output.write("Upgrade: TestUpgrade" + CRLF);
            output.write("Connection: Upgrade" + CRLF);
            output.write(CRLF);
            output.flush();
            LOG.info("finished writing upgrade request to the server");
            LOG.info("Reading upgrade response from the server");

            //get input message from the servlet confirming upgrade has  happen.
            //input = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            String line = "";
            while ((line = input.readLine()) != null) {
                LOG.info("\t" + line);
                if (line.trim().contains("101")) { // response shud add         HTTP/1.1 101 Switching Protocols
                    LOG.info("Received Upgrade response from server, now upgraded");
                    upgradeRecvd = true;
                }
                if (line.trim().equals(""))
                    break;
            }
        } catch (Exception e) {

            e.printStackTrace();
            LOG.info("exception caught in checkif_UpgardeRecvd method , throwing the same !!!");
            throw e;
        }
        return upgradeRecvd;
    }

    //helper method for writeListener with large data sizes
    private int readResponse_returnTotal(Socket soc) throws Exception {

        int total = 0;

        char[] dataBytes = new char[32768];
        StringBuilder sb = new StringBuilder();

        String line = "";
        while ((line = input.readLine()) != null) {
            LOG.info("\t" + line);
            total += line.length();
            sb.append(line);
            if (sb.toString().endsWith("/END")) {
                total = total - 4;
                break;
            }
        }

        LOG.info(total + " bytes read for the response for upgradeHandler.");

        return total;
    }

    private String writeListener_SmallDataSize(Socket soc) throws Exception {

        String line1 = "";

        try {

            //BufferedReader input = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            line1 = input.readLine();
            LOG.info("Read from the test app: " + line1);

        }

        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        //return data to the tests, read from the server.
        return line1;

    }

    // if server needs to be restarted after each test, uncomment @After section.
/*
 * @After
 * public void tearDown() throws Exception {
 * LOG.info("Restarting server !!! " + SHARED_SERVER.getServerName());
 * SHARED_SERVER.getLibertyServer().restartServer();
 * LOG.info("Finished restarting server !!! " + SHARED_SERVER.getServerName());
 *
 * }
 */

}
