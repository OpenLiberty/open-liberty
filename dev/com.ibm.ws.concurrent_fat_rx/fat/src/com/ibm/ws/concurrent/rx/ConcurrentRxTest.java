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
package com.ibm.ws.concurrent.rx;

import java.io.File;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.test.context.location.StateContextProvider;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.ConcurrentRxTestServlet;

@RunWith(FATRunner.class)
public class ConcurrentRxTest extends FATServletClient {

    @Server("com.ibm.ws.concurrent.fat.rx")
    @TestServlet(servlet = ConcurrentRxTestServlet.class, path = "concurrentrxfat/ConcurrentRxTestServlet")
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        server1.copyFileToLibertyInstallRoot("lib/features/", "features/managedCompletableFuture-1.0.mf");

        WebArchive app = ShrinkWrap.create(WebArchive.class, "concurrentrxfat.war")//
                        .addPackages(true, "web")//
                        .addAsWebInfResource(new File("test-applications/concurrentrxfat/resources/index.jsp"));
        ShrinkHelper.exportAppToServer(server1, app);

        JavaArchive customContextProviders = ShrinkWrap.create(JavaArchive.class, "customContextProviders.jar")
                        .addPackage("org.test.context.location")
                        .addAsServiceProvider(ThreadContextProvider.class, StateContextProvider.class);
        ShrinkHelper.exportToServer(server1, "lib", customContextProviders);

        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer();
        server1.deleteFileFromLibertyInstallRoot("lib/features/managedCompletableFuture-1.0.mf");
    }
}
