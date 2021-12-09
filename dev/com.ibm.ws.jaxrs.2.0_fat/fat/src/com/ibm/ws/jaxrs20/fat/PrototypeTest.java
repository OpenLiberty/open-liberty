/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs.fat.prototype.PrototypeClientTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/*
 * The purpose of this test is to provide an empty canvas for rapid/easy test experimentation,
 * as well as providing and example of FAT best practices.
 *
 * This Test should never have any real tests, if you use this Test to create a test that should
 * be added permanently, create a new FAT Test using this test as a template.
 */
@RunWith(FATRunner.class)
public class PrototypeTest extends FATServletClient {

    private static final String appName = "prototype";

    // Third party libs are copied to ${buildDir}/autoFVT/appLibs/prototype in build.gradle
    private static final String libs = "appLibs/prototype";

    @Server("com.ibm.ws.jaxrs.fat.prototype")
    @TestServlet(servlet = PrototypeClientTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Build an application and export it to the dropins directory
        ShrinkHelper.defaultDropinApp(server, appName, "com.ibm.ws.jaxrs.fat.prototype");

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
