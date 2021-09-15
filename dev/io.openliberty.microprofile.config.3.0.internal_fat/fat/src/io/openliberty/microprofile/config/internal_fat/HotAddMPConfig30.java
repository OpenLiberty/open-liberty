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
package io.openliberty.microprofile.config.internal_fat;

import java.util.Collections;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.config.internal_fat.apps.hotadd.HotAddMPConfig30Servlet;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class HotAddMPConfig30 extends FATServletClient {

    public static final String SERVER_NAME = "HotAddMPConfig30";
    public static final String APP_NAME = "hotAddMPConfig30App";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        DeployOptions[] deployOptions = { DeployOptions.SERVER_ONLY };
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        app.addClasses(HotAddMPConfig30Servlet.class);
        ShrinkHelper.exportAppToServer(server, app, deployOptions);
        server.copyFileToLibertyServerRoot(SERVER_NAME + "/noMPConfig/HotAddMPConfig.xml");
        server.copyFileToLibertyServerRoot(SERVER_NAME + "/withApp/HotAddApp.xml");
        server.startServerAndValidate(true, true, false); //don't validate because the app won't have started properly
    }

    @AfterClass
    public static void tearDown() throws Exception {
        //CWNEN0047W: Resource annotations on the fields of the io.openliberty.microprofile.config.internal_fat.apps.hotadd.HotAddMPConfig30Servlet class will be ignored.
        //The annotations could not be obtained because of the exception : java.lang.NoClassDefFoundError: org.eclipse.microprofile.config.Config
        server.stopServer("CWNEN0047W.*HotAddMPConfig30Servlet");
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
        copyConfigFileToLibertyServerRoot(SERVER_NAME + "/withMPConfig/HotAddMPConfig.xml", APP_NAME);

        runTest(server, APP_NAME + "/", "configTest");
    }
}
