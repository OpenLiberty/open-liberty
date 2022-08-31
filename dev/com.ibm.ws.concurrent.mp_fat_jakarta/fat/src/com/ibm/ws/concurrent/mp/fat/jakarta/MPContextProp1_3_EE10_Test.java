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
package com.ibm.ws.concurrent.mp.fat.jakarta;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import concurrent.mp.fat.v13.ee10.web.MPContextProp1_3_EE10_TestServlet;

// TODO This temporarily runs with MP Context Propagation 1.3 because 2.0 isn't available yet.
//      See the comments in FATSuite.java
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class MPContextProp1_3_EE10_Test extends FATServletClient {

    private static final String APP_NAME = "MPContextProp1_3_EE10_App";

    @Server("com.ibm.ws.concurrent.mp.fat.1.3.ee10")
    @TestServlet(servlet = MPContextProp1_3_EE10_TestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "concurrent.mp.fat.v13.ee10.web");

        // This JAR file contains both Jakarta EE and MicroProfile context providers mixed together
        JavaArchive customContextProviders = ShrinkWrap.create(JavaArchive.class, "customContextProviders2.jar")
                        .addPackage("org.test.ee.context.priority")
                        .addAsServiceProvider(jakarta.enterprise.concurrent.spi.ThreadContextProvider.class,
                                              org.test.ee.context.priority.PriorityContextProvider.class)
                        .addPackage("org.test.mp.context.priority")
                        .addAsServiceProvider(org.eclipse.microprofile.context.spi.ThreadContextProvider.class,
                                              org.test.mp.context.priority.PriorityContextProvider.class);
        ShrinkHelper.exportToServer(server, "lib", customContextProviders);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
