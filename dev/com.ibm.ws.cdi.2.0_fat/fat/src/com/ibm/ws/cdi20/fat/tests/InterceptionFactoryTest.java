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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import interceptionFactoryApp.web.InterceptionFactoryServlet;

/**
 * These tests verify that you can look up the bean manager as per http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#provider
 */
@RunWith(FATRunner.class)
public class InterceptionFactoryTest extends FATServletClient {
    public static final String APP_NAME = "interceptionFactoryApp";

    @Server("cdi20BasicServer")
    @TestServlets({ @TestServlet(servlet = InterceptionFactoryServlet.class, path = APP_NAME + "/interceptionFactoryTest") })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'interceptionFactoryApp.war' once it's written to a file
        // Include the 'interceptionFactoryApp.web' package and all of it's java classes and sub-packages
        // Include a simple index.jsp static file in the root of the WebArchive
        WebArchive app1 = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "interceptionFactoryApp.web")
                        .addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/index.jsp"));
        // Write the WebArchive to 'publish/servers/cdi20BasicServer/apps/interceptionFactoryApp.war' and print the contents
        ShrinkHelper.exportDropinAppToServer(server1, app1);
        server1.addInstalledAppForValidation(APP_NAME);
        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer();
    }
}
