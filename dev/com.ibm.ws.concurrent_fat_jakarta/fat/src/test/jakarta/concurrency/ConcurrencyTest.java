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
import test.jakarta.concurrency.web.ConcurrencyTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class ConcurrencyTest extends FATServletClient {

    public static final String APP_NAME = "ConcurrencyTestApp";

    @Server("com.ibm.ws.concurrent.fat.jakarta")
    @TestServlet(servlet = ConcurrencyTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Test application ConcurrencyTestApp.ear [ConcurrencyTestWeb.war, application.xml]
        WebArchive ConcurrencyTestWeb = ShrinkHelper.buildDefaultApp("ConcurrencyTestWeb", "test.jakarta.concurrency.web");
        EnterpriseArchive ConcurrencyTestApp = ShrinkWrap.create(EnterpriseArchive.class, "ConcurrencyTestApp.ear");
        ConcurrencyTestApp.addAsModule(ConcurrencyTestWeb);
        ShrinkHelper.addDirectory(ConcurrencyTestApp, "test-applications/ConcurrencyTestApp/resources");
        ShrinkHelper.exportAppToServer(server, ConcurrencyTestApp);

        ShrinkHelper.defaultApp(server, APP_NAME, "test.jakarta.concurrency.web");

        // fake third-party library that also include a thread context provider
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
