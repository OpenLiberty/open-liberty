/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi20.fat.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import cdiContainerConfigApp.web.CDIContainerConfigServlet;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * These tests verify that you can disable implict bean archives via config
 */
@RunWith(FATRunner.class)
public class CDIContainerConfigTest extends FATServletClient {
    public static final String APP_NAME = "cdiContainerConfigApp";

    // We'll create an app with 2 Servlets.
    @Server("cdi20ConfigServer")
    @TestServlets({ @TestServlet(servlet = CDIContainerConfigServlet.class, path = APP_NAME + "/cdiContainerConfigApp") })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'cdiContainerConfigApp.war' once it's written to a file
        // Include the 'cdiContainerConfigApp.web' package and all of it's java classes and sub-packages
        // Include a simple index.jsp static file in the root of the WebArchive
        WebArchive app1 = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        app1.addPackages(true, "cdiContainerConfigApp.web");
        app1.addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/index.jsp"));
        app1.addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/beans.xml"));

        JavaArchive implicitJar = ShrinkWrap.create(JavaArchive.class, "implicit.jar");
        implicitJar.addPackages(true, "cdiContainerConfigApp.implicit");
        app1.addAsLibrary(implicitJar);

        JavaArchive explicitJar = ShrinkWrap.create(JavaArchive.class, "explicit.jar");
        explicitJar.addPackages(true, "cdiContainerConfigApp.explicit");
        explicitJar.addAsManifestResource(new File("test-applications/" + APP_NAME + "/resources/beans.xml"));
        app1.addAsLibrary(explicitJar);

        // Write the WebArchive to 'publish/servers/cdi20BasicServer/apps/cdiContainerConfigApp.war' and print the contents
        ShrinkHelper.exportAppToServer(server1, app1);

        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer();
    }
}
