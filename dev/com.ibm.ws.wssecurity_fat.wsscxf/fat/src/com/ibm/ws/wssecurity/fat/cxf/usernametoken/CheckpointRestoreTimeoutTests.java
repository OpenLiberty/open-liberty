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

import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;

import java.io.File;
import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@SkipForRepeat({ NO_MODIFICATION, EE9_FEATURES })
@SkipIfCheckpointNotSupported
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CheckpointRestoreTimeoutTests extends SSLTestCommon {

    static private final Class<?> thisClass = CheckpointRestoreTimeoutTests.class;

    static final private String serverName = "com.ibm.ws.wssecurity_fat.ssl";
    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setup";
        String copyFromFile = "";

        ShrinkHelper.defaultDropinApp(server, "untsslclient", "com.ibm.ws.wssecurity.fat.untsslclient", "fats.cxf.basicssl.wssec", "fats.cxf.basicssl.wssec.types");
        ShrinkHelper.defaultDropinApp(server, "untoken", "com.ibm.ws.wssecurity.fat.untoken");
        // Add jvm.options
        server.copyFileToLibertyServerRoot("serversettings/jvm.options");
        PrepInitServer serverObject = new PrepInitServer();
        serverObject.prepareInit(server);
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("jaxws-2.3")) {
            copyFromFile = System.getProperty("user.dir") +
                           File.separator +
                           server.getPathToAutoFVTNamedServer() +
                           "server_customize_ee8.xml";
        }

        try {
            String serverFileLoc = (new File(server.getServerConfigurationPath().replace('\\', '/'))).getParent();

            Log.info(thisClass, "reconfigServer", "Copying: " + copyFromFile
                                                  + " to " + serverFileLoc);
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(),
                                                   serverFileLoc, "server.xml", copyFromFile);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }

    /**
     * ws-security.usernametoken.timeToLive="60"
     */
    @Test
    @AllowedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException" })
    public void testMsgExpiresAfterTimout() throws Exception {
        checkpointAndRestoreServer(null);

        genericTest("testCxfUntReplayTwoAndMoreMinutesSSL", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA6Service", "UrnBasicPlcyBA6",
                    "true", "", msgExpires,
                    "Second call to FVTVersionBA6Service should have failed");

    }

    /**
     * ws-security.usernametoken.timeToLive="180"
     */

    @Test
    @AllowedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException" })
    public void testSuccessMsgBeforeTimeout() throws Exception {
        checkpointAndRestoreServer("serversettings/server_timeout.xml");

        genericTest("testCxfUntReplayTwoAndMoreMinutesSSL", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA6Service", "UrnBasicPlcyBA6",
                    "true", "", "Response: WSSECFVT FVTVersion_ba06",
                    "The test expected a succesful message from the server.");

    }

    @After
    public void stopServer() throws Exception {
        try {
            printMethodName("tearDown");
            if (server != null && server.isStarted()) {
                server.stopServer();
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

}
