/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.usernametoken;

import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import java.io.File;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@SkipForRepeat({ EE9_FEATURES })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CxfSSLUNTNonceTimeOutTests extends SSLTestCommon {

    static private final Class<?> thisClass = CxfSSLUNTNonceTimeOutTests.class;

    static final private String serverName = "com.ibm.ws.wssecurity_fat.ssl";
    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setup";
        String copyFromFile = "";

        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("jaxws-2.2")) {
            copyFromFile = System.getProperty("user.dir") +
                           File.separator +
                           server.getPathToAutoFVTNamedServer() +
                           "server_customize.xml";
        }
        if (features.contains("jaxws-2.3")) {
            copyFromFile = System.getProperty("user.dir") +
                           File.separator +
                           server.getPathToAutoFVTNamedServer() +
                           "server_customize_ee8.xml";
        }

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
    //5/2021 added PrivilegedActionExc, NoSuchMethodExc as a result of java11 and ee8
    //@AllowedFFDC(value = { "java.util.MissingResourceException", "java.lang.ClassNotFoundException", "java.security.PrivilegedActionException",
    //                       "java.lang.NoSuchMethodException" })
    @AllowedFFDC(value = { "java.util.MissingResourceException" })
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
    @AllowedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException", "org.apache.ws.security.WSSecurityException" })
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
