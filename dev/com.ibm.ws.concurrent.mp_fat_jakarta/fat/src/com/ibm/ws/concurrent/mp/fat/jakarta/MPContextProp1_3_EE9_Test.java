/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.fat.jakarta;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.test.mp.context.priority.PriorityContextProvider;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import concurrent.mp.fat.v13.ee9.web.MPContextProp1_3_EE9_TestServlet;

@RunWith(FATRunner.class)
public class MPContextProp1_3_EE9_Test extends FATServletClient {

    private static final String APP_NAME = "MPContextProp1_3_EE9_App";

    @Server("com.ibm.ws.concurrent.mp.fat.1.3.ee9")
    @TestServlet(servlet = MPContextProp1_3_EE9_TestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "concurrent.mp.fat.v13.ee9.web");

        JavaArchive customContextProviders = ShrinkWrap.create(JavaArchive.class, "customContextProviders.jar")
                        .addPackage("org.test.mp.context.priority")
                        .addAsServiceProvider(ThreadContextProvider.class, PriorityContextProvider.class);
        ShrinkHelper.exportToServer(server, "lib", customContextProviders);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
