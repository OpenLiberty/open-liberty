/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.usernametoken;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
//Added 10/2020
import org.junit.runner.RunWith;

//Added 10/2020
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
//Added 10/2020
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
//Added 11/2020
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

//Added 11/2020, this test used to be run as LITE in CL, but takes more than 3 min to run, which is set to run with Full mode now
@Mode(TestMode.FULL)
//Added 10/2020
@RunWith(FATRunner.class)
public class CxfSSLUNTNonceTimeOutTests extends SSLTestCommon {

    static private final Class<?> thisClass = CxfSSLUNTNonceTimeOutTests.class;

    //Added 10/2020
    static final private String serverName = "com.ibm.ws.wssecurity_fat.ssl";
    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setup";
        String copyFromFile = System.getProperty("user.dir") +
                              File.separator +
                              server.getPathToAutoFVTNamedServer() +
                              "server_customize.xml";
        //orig from CL:
        //server = LibertyServerFactory.getLibertyServer("com.ibm.ws.wssecurity_fat.ssl");

        //Added 10/2020
        ShrinkHelper.defaultDropinApp(server, "untsslclient", "com.ibm.ws.wssecurity.fat.untsslclient", "fats.cxf.basicssl.wssec", "fats.cxf.basicssl.wssec.types");
        ShrinkHelper.defaultDropinApp(server, "untoken", "com.ibm.ws.wssecurity.fat.untoken");
        PrepInitServer serverObject = new PrepInitServer();
        serverObject.prepareInit(server);

        try {
            String serverFileLoc = (new File(server.getServerConfigurationPath().replace('\\', '/'))).getParent();
            Log.info(thisClass, "reconfigServer", "Copying: " + copyFromFile
                                                  + " to " + serverFileLoc);
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(),
                                                   serverFileLoc, "server.xml", copyFromFile);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }

        initServer();
    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid
     * username/password in the username token. The call to the service client
     * is made using https. The call to the server is also made using https.
     * TransportBinding and Nonce are specified in the wsdl. A hard-coded
     * message is passed twice. The seconds time is after 1.5 minutes
     * The nonce cache is set to 1 minute and created set to 2.5 minutes
     * Since Nonce cache is expired and the Nonce will be OK
     * and Created is not expired
     * The request should be OK.
     *
     */
    @Test
    public void testCxfUntHardcodedReplayOneAndMoreMinutesSSL() throws Exception {

        //reconfigAndRestartServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_customize.xml");
        genericTest("testCxfUntReplayOneAndMoreMinutesSSL", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA7Service", "UrnBasicPlcyBA7",
                    "true", "",
                    "Response: WSSECFVT FVTVersion_ba07",
                    "The test expected a succesful message from the server.");
        // Make sure the server.xml is set back to server_orig.xml
        // This will be done by next test case: TwoAndMoreMinutes
        // reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");
    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid
     * username/password in the username token. The call to the service client
     * is made using https. The call to the server is also made using https.
     * TransportBinding and Nonce are specified in the wsdl. A hard-coded
     * message is passed twice. The seconds time is after 3 minutes
     * The nonce cache is set to 1 minute and created set to 2.5 minutes
     * Since Nonce cache is expired and the Nonce will be OK
     * But the created is expired. So, it ought to get "The message has expired"
     * exception
     */
    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCxfUntHardcodedReplayTwoAndMoreMinutesSSL() throws Exception {
        // Make sure the server.xml is set to server_customize.xml
        // This was done by previous test: OneAndMoreMinutes
        //  reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_customize.xml");
        genericTest("testCxfUntReplayTwoAndMoreMinutesSSL", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA6Service", "UrnBasicPlcyBA6",
                    "true", "", msgExpires,
                    "Second call to FVTVersionBA6Service should have failed");
        //reconfigAndRestartServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_orig.xml");
    }

}
