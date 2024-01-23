/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.concurrency;

import jakarta.enterprise.concurrent.spi.ThreadContextProvider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
import test.jakarta.concurrency31.web.Concurrency31TestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 21)
public class Concurrency31Test extends FATServletClient {

    public static final String APP_NAME = "Concurrency31TestApp";

    @Server("com.ibm.ws.concurrent.fat.jakarta.ee11")
    @TestServlet(servlet = Concurrency31TestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Test application ConcurrencyTestApp.ear [ConcurrencyTestEJB.jar, ConcurrencyTestWeb.war, application.xml]

        WebArchive Concurrency31TestWeb = ShrinkHelper.buildDefaultApp("Concurrency31TestWeb", "test.jakarta.concurrency31.web");
        ShrinkHelper.addDirectory(Concurrency31TestWeb, "test-applications/Concurrency31TestWeb/resources");

        EnterpriseArchive Concurrency31TestApp = ShrinkWrap.create(EnterpriseArchive.class, "Concurrency31TestApp.ear");
        Concurrency31TestApp.addAsModule(Concurrency31TestWeb);
        ShrinkHelper.addDirectory(Concurrency31TestApp, "test-applications/Concurrency31TestApp/resources");
        ShrinkHelper.exportAppToServer(server, Concurrency31TestApp);

        // fake third-party library that also includes a thread context provider
        JavaArchive locationUtilsContextProviderJar = ShrinkWrap.create(JavaArchive.class, "location-utils.jar")
                        .addPackage("test.context.location")
                        .addAsServiceProvider(ThreadContextProvider.class.getName(),
                                              "test.context.location.ZipCodeContextProvider");
        ShrinkHelper.exportToServer(server, "lib", locationUtilsContextProviderJar);

        // fake thread context provider on its own (this will be made available via a bell)
        JavaArchive priorityContextProviderJar = ShrinkWrap.create(JavaArchive.class, "priority-context.jar")
                        .addPackage("test.context.priority")
                        .addAsServiceProvider(ThreadContextProvider.class.getName(),
                                              "test.context.priority.PriorityContextProvider");
        ShrinkHelper.exportToServer(server, "lib", priorityContextProviderJar);

        // fake third-party library that includes multiple thread context providers
        JavaArchive statUtilsContextProviderJar = ShrinkWrap.create(JavaArchive.class, "stat-utils.jar")
                        .addPackage("test.context.list")
                        .addPackage("test.context.timing")
                        .addAsServiceProvider(ThreadContextProvider.class.getName(),
                                              "test.context.list.ListContextProvider",
                                              "test.context.timing.TimestampContextProvider");
        ShrinkHelper.exportToServer(server, "lib", statUtilsContextProviderJar);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
