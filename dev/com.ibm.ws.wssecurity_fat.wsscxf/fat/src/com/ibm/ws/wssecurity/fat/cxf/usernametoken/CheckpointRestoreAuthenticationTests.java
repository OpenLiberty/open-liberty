/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.usernametoken;

import static componenttest.annotation.SkipForRepeat.EE8_FEATURES;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.SharedTools;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@SkipForRepeat({ NO_MODIFICATION, EE8_FEATURES })
@SkipIfCheckpointNotSupported
@RunWith(FATRunner.class)
public class CheckpointRestoreAuthenticationTests {

    static final private String serverName = "com.ibm.ws.wssecurity_fat";
    @Server(serverName)
    public static LibertyServer server;

    private final Class<?> thisClass = CheckpointRestoreAuthenticationTests.class;

    private static String untClientUrl = "";

    private static String httpPortNumber = "";

    /**
     * Sets up any configuration required for running the OAuth tests.
     * Currently, it just starts the server, which should start the
     * applications in dropins.
     */
    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, "untclient", "com.ibm.ws.wssecurity.fat.untclient", "fats.cxf.basic.wssec", "fats.cxf.basic.wssec.types");
        ShrinkHelper.defaultDropinApp(server, "untoken", "com.ibm.ws.wssecurity.fat.untoken");

        // Add jvm.options
        server.copyFileToLibertyServerRoot("serversettings/jvm.options");

        httpPortNumber = "" + server.getHttpDefaultPort();
        untClientUrl = "http://localhost:" + httpPortNumber +
                       "/untclient/CxfUntSvcClient";
    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf unt web service.
     *
     */

    @Test
    public void testCheckpointRestore() throws Exception {
        String thisMethod = "testCheckpointRestore";
        String expectedResponse = "This is WSSECFVT CXF Web Service.";
        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
        server.startServer();// check CWWKS0008I: The security service is ready.

        server.checkpointRestore();

        SharedTools.waitForMessageInLog(server, "CWWKS0008I");

        server.waitForStringInLog("port " + httpPortNumber);

        String respReceived = invokeUntWssecSvcClient(thisMethod);

        assertTrue("Expected response not received",
                   respReceived.contains(expectedResponse));

        server.stopServer();
    }

    @Test
    public void testCheckpointRestoreWithWrongUser() throws Exception {
        String thisMethod = "testCheckpointRestoreWrongUser";
        String expectedResponse = "This is WSSECFVT CXF Web Service.";

        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
        server.startServer();// check CWWKS0008I: The security service is ready.

        server.setServerConfigurationFile("serversettings/server_wrong_user.xml");

        server.checkpointRestore();

        SharedTools.waitForMessageInLog(server, "CWWKS0008I");

        server.waitForStringInLog("port " + httpPortNumber);

        String respReceived = invokeUntWssecSvcClient(thisMethod);

        assertFalse("It should not contain the expected response",
                    respReceived.contains(expectedResponse));

        //CWWKW0226E: The user [baduser] could not be validated. Verify that the user name and password credentials that were provided are correct.
        assertTrue("Expected error message  CWWKW0226E not found", server.findStringsInLogs("CWWKW0226E") != null);

        server.stopServer("CWWKW0226E");
    }

    @Test
    public void testCheckpointRestoreWithWrongPassword() throws Exception {
        String thisMethod = "testCheckpointRestoreWrongPassword";
        String expectedResponse = "This is WSSECFVT CXF Web Service.";
        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
        server.startServer();// check CWWKS0008I: The security service is ready.

        server.setServerConfigurationFile("serversettings/server_wrong_password.xml");

        server.checkpointRestore();

        SharedTools.waitForMessageInLog(server, "CWWKS0008I");

        server.waitForStringInLog("port " + httpPortNumber);

        String respReceived = invokeUntWssecSvcClient(thisMethod);

        assertFalse("It should not contain the expected response",
                    respReceived.contains(expectedResponse));

        //CWWKW0226E: The user [user1] could not be validated. Verify that the user name and password credentials that were provided are correct.
        assertTrue("Expected error message  CWWKW0226E not found", server.findStringsInLogs("CWWKW0226E") != null);

        server.stopServer("CWWKW0226E");
    }

    /**
     * @param thisMethod
     * @return
     * @throws Exception
     */
    private String invokeUntWssecSvcClient(String thisMethod) throws Exception {
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
        return respReceived;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKW0226E");
        }
    }

}
