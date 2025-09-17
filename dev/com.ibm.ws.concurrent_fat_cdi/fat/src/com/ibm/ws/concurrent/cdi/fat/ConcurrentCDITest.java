/*******************************************************************************
 * Copyright (c) 2017,2025 IBM Corporation and others.
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
import concurrent.cdi.web.ConcurrentCDIServlet;
import concurrent.cdi4.web.ConcurrentCDI4Servlet;
import concurrent.cdi4.webapp.ConcurrentCDIAdditionalServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class ConcurrentCDITest extends FATServletClient {

    public static final String APP_NAME = "concurrentCDIApp";
    public static final String APP_NAME_EE10 = "concurrentCDI4App";
    public static final String WEBAPP_NAME_EE10 = "concurrentCDI4WebApp";

    @Server("concurrent_fat_cdi")
    @TestServlets({
                    @TestServlet(servlet = ConcurrentCDIServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = ConcurrentCDI4Servlet.class, contextRoot = APP_NAME_EE10),
                    @TestServlet(servlet = ConcurrentCDIAdditionalServlet.class, contextRoot = WEBAPP_NAME_EE10)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create location-context.jar
        // fake third-party library that includes a thread context provider
        JavaArchive locationContextProviderJar = ShrinkWrap.create(JavaArchive.class, "location-context.jar")
                        .addPackage("concurrent.cdi.context.location")
                        .addAsServiceProvider(ThreadContextProvider.class.getName(),
                                              "concurrent.cdi.context.location.LocationContextProvider");
        ShrinkHelper.exportToServer(server, "lib", locationContextProviderJar);

        // Create concurrentCDIApp.ear
        JavaArchive cdiExtensionJar = ShrinkWrap
                        .create(JavaArchive.class, "cdi-extension.jar")
                        .addPackage("concurrent.cdi.ext")
                        .addAsServiceProvider(Extension.class.getName(),
                                              "concurrent.cdi.ext.ConcurrentCDIExtension");

        WebArchive concurrentCDIWeb = ShrinkHelper
                        .buildDefaultApp("concurrentCDIWeb",
                                         "concurrent.cdi.web")
                        .addAsLibrary(cdiExtensionJar);
        ShrinkHelper.addDirectory(concurrentCDIWeb,
                                  "test-applications/concurrentCDIWeb/resources");

        JavaArchive concurrentCDIEJBLib = ShrinkHelper.buildJavaArchive("concurrentCDIEJB", "concurrent.cdi.ejb.anno");

        JavaArchive concurrentCDIEJB = ShrinkHelper.buildJavaArchive("concurrentCDIEJB", "concurrent.cdi.ejb");
        ShrinkHelper.addDirectory(concurrentCDIEJB, "test-applications/concurrentCDIEJB/resources");

        EnterpriseArchive concurrentCDIApp = ShrinkWrap.create(EnterpriseArchive.class, "concurrentCDIApp.ear");
        concurrentCDIApp.addAsModule(concurrentCDIWeb);
        concurrentCDIApp.addAsModule(concurrentCDIEJB);
        concurrentCDIApp.addAsLibraries(concurrentCDIEJBLib);
        ShrinkHelper.addDirectory(concurrentCDIApp, "test-applications/concurrentCDIApp/resources");
        ShrinkHelper.exportAppToServer(server, concurrentCDIApp);

        // Create concurrentCDI4App.war
        // TODO Adding "concurrent.cu3.web" to the following would cause conflict with app-defined ManagedExecutorService.
        // There is a spec proposal to detect conflict and avoid automatically adding the bean.
        ShrinkHelper.defaultDropinApp(server, APP_NAME_EE10, "concurrent.cdi4.web");

        // Create concurrentCDIWeb2.war
        ShrinkHelper.defaultDropinApp(server, WEBAPP_NAME_EE10, "concurrent.cdi4.webapp");

        server.startServer();
        runTest(server, APP_NAME_EE10 + '/' + ConcurrentCDI4Servlet.class.getSimpleName(), "initTransactionService");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(
                          "CWWKC1101E.*scheduled-executor-without-app-context", // tests lack of context from scheduled executor thread
                          "CWWKE1205E" // test case intentionally causes startTimeout to be exceeded
        );
    }
}
