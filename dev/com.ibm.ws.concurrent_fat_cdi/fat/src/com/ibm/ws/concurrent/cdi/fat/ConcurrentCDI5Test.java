/*******************************************************************************
 * Copyright (c) 2017,2026 IBM Corporation and others.
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
package com.ibm.ws.concurrent.cdi.fat;

import jakarta.enterprise.concurrent.spi.ThreadContextProvider;
import jakarta.enterprise.inject.spi.Extension;

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
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import concurrent.cdi4_1.web.ConcurrentCDI4_1Servlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 21)
public class ConcurrentCDI5Test extends FATServletClient {

    public static final String APP_NAME = "concurrentCDI4_1App";
    public static final String APP_NAME_EE10 = "concurrentCDI4App";
    public static final String WEBAPP_NAME_EE10 = "concurrentCDI4WebApp";

    @Server("concurrent_fat_cdi5")
    @TestServlets({
                    @TestServlet(servlet = ConcurrentCDI4_1Servlet.class,
                                 contextRoot = APP_NAME,
                                 minJavaLevel = 21)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create location-context.jar
        // fake third-party library that includes a thread context provider
        JavaArchive locationContextProviderJar = ShrinkWrap.create(JavaArchive.class,
                                                                   "location-context.jar")
                        .addPackage("concurrent.cdi.context.location")
                        .addAsServiceProvider(ThreadContextProvider.class.getName(),
                                              "concurrent.cdi.context.location.LocationContextProvider");
        ShrinkHelper.exportToServer(server, "lib", locationContextProviderJar);

        // Create concurrentCDI4_1App.ear
        JavaArchive cdiExtensionJar = ShrinkWrap
                        .create(JavaArchive.class, "cdi-extension.jar")
                        .addPackage("concurrent.cdi4_1.ext")
                        .addAsServiceProvider(Extension.class.getName(),
                                              "concurrent.cdi4_1.ext.ConcurrentCDIExtension");

        WebArchive concurrentCDI4_1Web = ShrinkHelper
                        .buildDefaultApp("concurrentCDI4_1Web",
                                         "concurrent.cdi4_1.web")
                        .addAsLibrary(cdiExtensionJar);
        ShrinkHelper.addDirectory(concurrentCDI4_1Web,
                                  "test-applications/concurrentCDI4_1Web/resources");

        JavaArchive concurrentCDI4_1EJBLib = ShrinkHelper
                        .buildJavaArchive("concurrentCDI4_1EJB",
                                          "concurrent.cdi4_1.ejb.anno");

        JavaArchive concurrentCDI4_1EJB = ShrinkHelper
                        .buildJavaArchive("concurrentCDI4_1EJB",
                                          "concurrent.cdi4_1.ejb");
        ShrinkHelper.addDirectory(concurrentCDI4_1EJB,
                                  "test-applications/concurrentCDI4_1EJB/resources");

        EnterpriseArchive concurrentCDI4_1App = ShrinkWrap
                        .create(EnterpriseArchive.class,
                                "concurrentCDI4_1App.ear");
        concurrentCDI4_1App.addAsModule(concurrentCDI4_1Web);
        concurrentCDI4_1App.addAsModule(concurrentCDI4_1EJB);
        concurrentCDI4_1App.addAsLibraries(concurrentCDI4_1EJBLib);
        ShrinkHelper.addDirectory(concurrentCDI4_1App,
                                  "test-applications/concurrentCDI4_1App/resources");
        ShrinkHelper.exportAppToServer(server, concurrentCDI4_1App);

        // Create concurrentCDI4_1Shared.jar
        JavaArchive concurrentCDI4_1Shared = ShrinkHelper
                        .buildJavaArchive("concurrentCDI4_1Shared",
                                          "concurrent.ejb.shared");
        ShrinkHelper.exportToServer(server, "lib", concurrentCDI4_1Shared);

        // Create concurrentCDI4_1EJBStandalone.jar
        JavaArchive concurrentCDI4_1EJBStandalone = ShrinkHelper
                        .buildJavaArchive("concurrentCDI4_1EJBStandalone",
                                          "concurrent.ejb.standalone.jar");
        ShrinkHelper.exportAppToServer(server, concurrentCDI4_1EJBStandalone);

        // Create concurrentCDI4_1RAR.rar
        ShrinkHelper.defaultRar(server, "concurrentCDI4_1RAR", "concurrent.rar");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(
                          "CWWKN0005W", // JNDI copes with race conditions between multiple components registering namespaces by letting the first thread win and throwing this message.
                          "CWWKC1101E.*scheduled-executor-without-app-context", // tests lack of context from scheduled executor thread
                          "CWWKE1205E" // test case intentionally causes startTimeout to be exceeded
        );
    }
}
