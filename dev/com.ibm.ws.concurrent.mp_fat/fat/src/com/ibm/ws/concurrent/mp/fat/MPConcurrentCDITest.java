/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.fat;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.test.context.location.CityContextProvider;
import org.test.context.location.StateContextProvider;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import concurrent.mp.fat.cdi.web.MPConcurrentCDITestServlet;

@RunWith(FATRunner.class)
public class MPConcurrentCDITest extends FATServletClient {

    private static final String CDI_APP = "MPConcurrentCDIApp";

    @ClassRule
    public static RepeatTests r = MPContextPropActions.repeat("MPConcurrentCDITestServer", MPContextPropActions.CTX10, MPContextPropActions.CTX12);

    @Server("MPConcurrentCDITestServer")
    @TestServlet(servlet = MPConcurrentCDITestServlet.class, contextRoot = CDI_APP)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, CDI_APP, "concurrent.mp.fat.cdi.web");

        JavaArchive customContextProviders = ShrinkWrap.create(JavaArchive.class, "customContextProviders.jar")
                        .addPackage("org.test.context.location")
                        .addAsServiceProvider(ThreadContextProvider.class, CityContextProvider.class, StateContextProvider.class);
        ShrinkHelper.exportToServer(server, "lib", customContextProviders, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKC1158E"); // expected by testCDIContextPropagationBeanFirstUsedInCompletionStage
    }
}
