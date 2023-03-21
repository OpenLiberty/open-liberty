/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.fat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpRestClient.prototype.PrototypeRestClientClientTestServlet;

/*
 * The purpose of this test is to provide an empty canvas for rapid/easy test experimentation,
 * as well as providing and example of FAT best practices.
 *
 * This Test should never have any real tests, if you use this Test to create a test that should
 * be added permanently, create a new FAT Test using this test as a template.
 */
@RunWith(FATRunner.class)
public class PrototypeRestClientTest extends FATServletClient {

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    final static String SERVER_NAME = "mpRestClient.prototype";

    // To avoid bogus timeout build-breaks on slow Windows hardware only run a few versions on
    // Windows.
    @ClassRule
    public static RepeatTests r;
    static {
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) {
            r = MicroProfileActions.repeat(SERVER_NAME,
                                           MicroProfileActions.MP13, //mpRestClient-1.0
                                           MicroProfileActions.MP20, //mpRestClient-1.1
                                           MicroProfileActions.MP22, // 1.2
                                           MicroProfileActions.MP30, // 1.3
                                           MicroProfileActions.MP33, // 1.4
                                           MicroProfileActions.MP40, // 2.0
                                           MicroProfileActions.MP50, // 3.0
                                           MicroProfileActions.MP60);// 3.0+EE10

        } else {
            r = MicroProfileActions.repeat(SERVER_NAME,
                                           MicroProfileActions.MP13, //mpRestClient-1.0
                                           MicroProfileActions.MP60);// 3.0+EE10

        }
    }
    private static final String appName = "prototype";

    // Third party libs are copied to ${buildDir}/autoFVT/appLibs/prototype in build.gradle
//    private static final String libs = "appLibs/prototype";

    @Server(SERVER_NAME)
    @TestServlet(servlet = PrototypeRestClientClientTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Build an application and export it to the dropins directory
        ShrinkHelper.defaultDropinApp(server, appName, "mpRestClient.prototype");

        // Build an application, add third party libs, and manuall export to the dropins directory
//        WebArchive app = ShrinkHelper.buildDefaultApp(appName, "com.ibm.ws.jaxrs.fat.prototype");
//        app.addAsLibraries(new File(libs).listFiles());
//        ShrinkHelper.exportDropinAppToServer(server, app);
//        server.addInstalledAppForValidation(appName);

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer("Prototype.log", true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (server != null) {
            server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines
        }
    }

    @Before
    public void beforeTest() {}

    @After
    public void afterTest() {}
}
