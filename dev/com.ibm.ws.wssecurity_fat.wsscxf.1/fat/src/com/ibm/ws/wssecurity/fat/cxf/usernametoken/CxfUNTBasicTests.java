/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.usernametoken;

import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;
import com.ibm.ws.wssecurity.fat.utils.common.SharedTools;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.CheckpointRule;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.CheckpointRule.ServerMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.vulnerability.LeakedPasswordChecker;

@SkipForRepeat({ RepeatWithEE7cbh20.ID })
@RunWith(FATRunner.class)
@CheckpointTest(alwaysRun = true)
public class CxfUNTBasicTests {

    static final private String serverName = "com.ibm.ws.wssecurity_fat";
    @Server(serverName)
    public static LibertyServer server;

    static private final Class<?> thisClass = CxfUNTBasicTests.class;

    private static String untClientUrl = "";

    private static String httpPortNumber = "";

    private static LeakedPasswordChecker leakedPasswordChecker;

    @ClassRule
    public static CheckpointRule checkpointRule = new CheckpointRule()
                                                     .setConsoleLogName(CxfUNTBasicTests.class.getSimpleName()+ ".log")
                                                     .setServerSetup(CxfUNTBasicTests::serverSetUp)
                                                     .setServerStart(CxfUNTBasicTests::serverStart)
                                                     .setServerTearDown(CxfUNTBasicTests::serverTearDown)
                                                     .addUnsupportedRepeatIDs(EmptyAction.ID, RepeatWithEE7cbh20.ID );

    public static LibertyServer serverSetUp(ServerMode mode) throws Exception {

        ShrinkHelper.defaultDropinApp(server, "untclient", "com.ibm.ws.wssecurity.fat.untclient", "fats.cxf.basic.wssec", "fats.cxf.basic.wssec.types");
        ShrinkHelper.defaultDropinApp(server, "untoken", "com.ibm.ws.wssecurity.fat.untoken");
        httpPortNumber = "" + server.getHttpDefaultPort();

        untClientUrl = "http://localhost:" + httpPortNumber +
                       "/untclient/CxfUntSvcClient";

        leakedPasswordChecker = new LeakedPasswordChecker(server);
        return server;

    }

    public static void serverStart(ServerMode mode, LibertyServer server) throws Exception {
        server.startServer();// check CWWKS0008I: The security service is ready.
        SharedTools.waitForMessageInLog(server, "CWWKS0008I");
        server.waitForStringInLog("port " + httpPortNumber);
    }

    public static void serverTearDown(ServerMode mode, LibertyServer server) throws Exception {
        if (server != null && server.isStarted()) {
            //orig from CL
            //server.stopServer();
            //11/2020 pass in the error reference to stopServer; otherwise, it will be logged as FAT error
            //OL LibertyServer class has slight different code than CL:
            //if (!isServerExemptFromChecking(regIgnore)) { ...else {
            //Log.info(c, method, "Skipping log validation on server " + getServerName());
            //OL doesn't have the above code now
            //CWWKW0226E: The user [user1] could not be validated. Verify that the user name and password credentials that were provided are correct.
            //CWWKW0226E: The user [baduser123] could not be validated. Verify that the user name and password credentials that were provided are correct.
            server.stopServer("CWWKW0226E");
        }
    }

    @Test
    public void testAlwaysRun() throws Exception {
        // Get current size of heap in bytes
        long heapSize = Runtime.getRuntime().totalMemory();

        // Get maximum size of heap in bytes. The heap cannot grow beyond this size.// Any attempt will result in an OutOfMemoryException.
        long heapMaxSize = Runtime.getRuntime().maxMemory();

        // Get amount of free memory within the heap in bytes. This size will increase // after garbage collection and decrease as new objects are created.
        long heapFreeSize = Runtime.getRuntime().freeMemory();
        Log.info(thisClass, "testAlwaysRun", "current heap size: " + heapSize);
        Log.info(thisClass, "testAlwaysRun", "maximum heap size: " + heapMaxSize);
        Log.info(thisClass, "testAlwaysRun", "free memory  size: " + heapFreeSize);

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf unt web service.
     *
     */

    @Test
    public void testUntCxfSvcClient() throws Exception {

        String thisMethod = "testUntCxfSvcClient";
        String expectedResponse = "This is WSSECFVT CXF Web Service.";
        String respReceived = null;

        try {

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();
            Log.info(thisClass, thisMethod, "New WebConversation");

            // Invoke the service client - servlet
            request = new GetMethodWebRequest(untClientUrl);
            Log.info(thisClass, thisMethod, "Invoke service client - servlet: New GetMethodWebRequest");

            request.setParameter("httpDefaultPort", httpPortNumber);
            request.setParameter("untClient", "cxf");

            // Invoke the client
            response = wc.getResponse(request);
            Log.info(thisClass, thisMethod, "invoke client: wc.getResponse");

            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from CXF UNT Service client: " + respReceived);

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception occurred:");
            System.err.println("Exception: " + e);
            throw e;
        }

        assertTrue("The testUntSvcClient test failed",
                   respReceived.contains(expectedResponse));
        Log.info(thisClass, thisMethod, "assertTrue");

        leakedPasswordChecker.checkForPasswordInTrace("security</wsse:Password>");

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf unt web service.
     *
     */

    @Test
    public void testUntWssecSvcClient() throws Exception {

        String thisMethod = "testUntWssecSvcClient";
        String expectedResponse = "This is WSSECFVT CXF Web Service.";
        String respReceived = null;

        try {

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();
            Log.info(thisClass, thisMethod, "New WebConversation");

            // Invoke the service client - servlet
            request = new GetMethodWebRequest(untClientUrl);
            Log.info(thisClass, thisMethod, "invoke service client servlet - New GetMethodWebRequest");

            request.setParameter("httpDefaultPort", httpPortNumber);
            request.setParameter("untClient", "ibm");

            // Invoke the client
            response = wc.getResponse(request);
            Log.info(thisClass, thisMethod, "invoke client - wc.getResponse");

            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from IBM UNT Service client: " + respReceived);
        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception occurred:");
            Log.error(thisClass, thisMethod, e, "Exception: ");
            throw e;
        }

        assertTrue("The testUntWssecSvcClient test failed",
                   respReceived.contains(expectedResponse));
        Log.info(thisClass, thisMethod, "assertTrue");

        return;

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with an invalid password
     * in username token. The client request is expected to be rejected with
     * an appropriate exception.
     *
     */

    @Test
    public void testUntCxfBadPswd() {

        String thisMethod = "testUntCxfBadPswd";
        String expectedResponse = "The security token could not be authenticated or authorized";
        String respReceived = null;

        try {

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Invoke the service client - servlet
            request = new GetMethodWebRequest(untClientUrl);

            request.setParameter("httpDefaultPort", httpPortNumber);
            request.setParameter("untClient", "cxfbadpswd");

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from CXF UNT Service client: " + respReceived);

        } catch (Exception ex) {

            respReceived = ex.getMessage();
            Log.info(thisClass, thisMethod, "Exception occurred:" + respReceived);
        }

        assertTrue("The testUntCxfBadPswd test failed",
                   respReceived.contains(expectedResponse));

        leakedPasswordChecker.checkForPasswordInTrace("badpswd123</wsse:Password>");

        return;

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with an invalid username
     * in username token. The client request is expected to be rejected with
     * an appropriate exception.
     *
     */

    @Test
    public void testUntCxfBadUserID() {

        String thisMethod = "testUntCxfBaduserID";
        String expectedResponse = "The security token could not be authenticated or authorized";
        String respReceived = null;

        try {

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Invoke the service client - servlet
            request = new GetMethodWebRequest(untClientUrl);

            request.setParameter("httpDefaultPort", httpPortNumber);
            request.setParameter("untClient", "cxfbaduser");

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from CXF UNT Service client: " + respReceived);

        } catch (Exception ex) {

            respReceived = ex.getMessage();
            Log.info(thisClass, thisMethod, "Exception occurred:" + respReceived);
        }

        assertTrue("The testUntCxfBadUserID test failed",
                   respReceived.contains(expectedResponse));

        return;

    }
}
