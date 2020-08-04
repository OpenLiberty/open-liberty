/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi20.fat.tests;

import static org.junit.Assert.assertNotNull;

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
import secureAsyncEventsApp.web.SecureAsyncEventsServlet;

/**
 * These tests verify the use of asynchronous events in CDI2.0 as per http://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#firing_events_asynchronously
 */
@RunWith(FATRunner.class)
public class SecureAsyncEventsTest extends FATServletClient {
    public static final String APP_NAME = "secureAsyncEventsApp";

    // We'll create an app with a Servlet.
    @Server("cdi20SecureAsyncEventsServer")
    @TestServlets({ @TestServlet(servlet = SecureAsyncEventsServlet.class, path = APP_NAME + "/secureasyncevents") })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'secureAsyncEventsApp.war' once it's written to a file
        // Include the 'beanManagerLookupApp.web' package and all of it's java classes and sub-packages
        // Include a simple index.jsp static file in the root of the WebArchive
        WebArchive app1 = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addPackages(true,
                                                                                             "secureAsyncEventsApp.web").addAsWebInfResource(new File("test-applications/"
                                                                                                                                                      + APP_NAME
                                                                                                                                                      + "/resources/index.jsp"));
        // Write the WebArchive to 'publish/servers/cdi20SecureAsyncEventsServer/apps/secureAsyncEventsApp.war' and print the contents
        ShrinkHelper.exportAppToServer(server1, app1);

        server1.startServer();

        assertNotNull("CWWKF0011I.* not received on server", server1.waitForStringInLog("CWWKF0011I.*")); // wait for server is ready to run a smarter planet
        assertNotNull("Security service did not report it was ready", server1.waitForStringInLog("CWWKS0008I"));
        assertNotNull("CWWKS4105I.* not received on server", server1.waitForStringInLog("CWWKS4105I.*")); // wait for LTPA key to be available
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer();
    }
}
