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

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServerFactory;

public class CxfSSLUNTNonceTimeOutTests extends SSLTestCommon {

    static private final Class<?> thisClass = CxfSSLUNTNonceTimeOutTests.class;

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setup";
        String copyFromFile = System.getProperty("user.dir") +
                              File.separator +
                              server.getPathToAutoFVTNamedServer() +
                              "server_customize.xml";
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.wssecurity_fat.ssl");
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
