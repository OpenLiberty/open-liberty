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
package com.ibm.ws.webservices.handler.fat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.jaxrs.fat.globalhandler.simple.SimpleGlobalHandlerClientTestServlet;


/*
 * This test demonstrates the minimum viable code needed to configure GlobalHandler support
 * with JAX-RS/Restful-WS.
 */
@RunWith(FATRunner.class)
//@SkipForRepeat({ EE8FeatureReplacementAction.ID, JakartaEE9Action.ID, JakartaEE10Action.ID })
public class SimpleGlobalHandlerTest extends FATServletClient {

    private static final String appName = "SimpleGlobalHandler";

    @Server("SimpleGlobalHandlerServer")
    @TestServlet(servlet = SimpleGlobalHandlerClientTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Build an application and export it to the dropins directory
        ShrinkHelper.defaultDropinApp(server, appName, "io.openliberty.jaxrs.fat.globalhandler.simple");

        /*
         * Build and install the user feature (wlp/usr/extension/lib)
         *
         * Note: EE10+ features MUST include the following Subsystem-Contents:
         *
         * io.openliberty.globalhandler-1.0; type=“osgi.subsystem.feature” // EE10+ requires the user to bring in globalhandler-1.0
         * com.ibm.websphere.appserver.servlet-6.0; type=“osgi.subsystem.feature” // globalhandler-1.0 requires the servlet-6.0 feature
         */
        ShrinkHelper.defaultUserFeatureArchive(server, "MySimpleGlobalHandler", "io.openliberty.jaxrs.fat.globalhandler.simple.userbundle");
        TestUtils.installUserFeature(server, "MySimpleGlobalHandlerFeature");
        Thread.sleep(5000);

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer("SimpleGlobalHandlerServer.log", true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (server != null) {
//            server.uninstallUserFeature("MySimpleGlobalHandlerFeature");
            server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines
        }
    }

    @Before
    public void beforeTest() {}

    @After
    public void afterTest() {}
}
