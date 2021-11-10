/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.test;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class HotAddMPConfig extends FATServletClient {

    public static final String SERVER_NAME = "HotAddMPConfig";
    public static final String APP_NAME = "hotAddMPConfigApp";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        DeployOptions[] options = { DeployOptions.SERVER_ONLY };
        ShrinkHelper.defaultApp(server, APP_NAME, options, "com.ibm.ws.microprofile.config.hotadd.*");
        server.copyFileToLibertyServerRoot("HotAddMPConfig/noMPConfig/HotAddMPConfig.xml");
        server.copyFileToLibertyServerRoot("HotAddMPConfig/withApp/HotAddApp.xml");
        server.startServerAndValidate(true, true, false); //don't validate because the app won't have started properly
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWNEN0047W.*HotAddMPConfigServlet");
    }

    /**
     * Copy a server config file to the server root and wait for notification that the server config has been updated
     *
     * @param filename
     * @throws Exception
     */
    private static void copyConfigFileToLibertyServerRoot(String filename, String appName) throws Exception {
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot(filename);

        if (appName == null) {
            server.waitForConfigUpdateInLogUsingMark(null, false);
        } else {
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(appName), false);
        }

        Thread.sleep(ConfigConstants.DEFAULT_DYNAMIC_REFRESH_INTERVAL * 2); // We need this pause so that the MP config change is picked up through the polling mechanism
    }

//Not needed when there is just one test and it makes the logs harder to read
//If a second test is added then this will be needed to reset the config to its original state
    //    @Before
    //    public void resetConfigFile() throws Exception {
    //        copyConfigFileToLibertyServerRoot("HotAddMPConfig/noMPConfig/HotAddMPConfig.xml", null);
    //        copyConfigFileToLibertyServerRoot("HotAddMPConfig/withApp/HotAddApp.xml", null);
    //    }

    @Test
    public void testHotAddMPConfig() throws Exception {
        //Add in the mpConfig feature, which should allow the application to start cleanly
        copyConfigFileToLibertyServerRoot("HotAddMPConfig/withMPConfig/HotAddMPConfig.xml", APP_NAME);

        runTest(server, "hotAddMPConfigApp/HotAddMPConfigServlet", "configTest");
    }
}
