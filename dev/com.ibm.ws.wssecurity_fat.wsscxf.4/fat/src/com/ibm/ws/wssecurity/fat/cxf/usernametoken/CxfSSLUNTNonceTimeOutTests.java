/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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

import static componenttest.annotation.SkipForRepeat.EE10_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EmptyAction;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@SkipForRepeat({ RepeatWithEE7cbh20.ID, EE9_FEATURES, EE10_FEATURES })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CxfSSLUNTNonceTimeOutTests extends SSLTestCommon {

    static private final Class<?> thisClass = CxfSSLUNTNonceTimeOutTests.class;

    static final private String serverName = "com.ibm.ws.wssecurity_fat.ssl";
    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        String copyFromFile = "";

        //issue 23060, 23418
        copyFromFile = System.getProperty("user.dir") +
                       File.separator +
                       server.getPathToAutoFVTNamedServer() +
                       "server_customize.xml";

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
    @ExpectedFFDC({ "org.apache.wss4j.common.ext.WSSecurityException" })
    public void testCxfUntHardcodedReplayOneAndMoreMinutesSSL() throws Exception {

        genericTest("testCxfUntReplayOneAndMoreMinutesSSL", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA7Service", "UrnBasicPlcyBA7",
                    "true", "",
                    // TODO is replay attack merited? "Response: WSSECFVT FVTVersion_ba07",
                    replayAttackNew,
                    "The test expected a replay attack message from the server.");

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
    @AllowedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    public void testCxfUntHardcodedReplayTwoAndMoreMinutesSSL() throws Exception {

        genericTest("testCxfUntReplayTwoAndMoreMinutesSSL", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA6Service", "UrnBasicPlcyBA6",
                    "true", "", msgExpires,
                    "Second call to FVTVersionBA6Service should have failed");

    }

}
