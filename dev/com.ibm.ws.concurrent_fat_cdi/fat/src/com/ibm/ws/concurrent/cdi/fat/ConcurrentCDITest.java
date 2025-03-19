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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import concurrent.cdi4.web.ConcurrentCDI4Servlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class ConcurrentCDITest extends FATServletClient {

    public static final String APP_NAME = "concurrentCDIApp";
    public static final String APP_NAME_EE10 = "concurrentCDI4App";

    @Server("concurrent_fat_cdi")
    @TestServlets({
                    @TestServlet(servlet = ConcurrentCDI4Servlet.class, contextRoot = APP_NAME_EE10)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // fake third-party library that includes a thread context provider
        JavaArchive locationContextProviderJar = ShrinkWrap.create(JavaArchive.class, "location-context.jar")
                        .addPackage("concurrent.cdi.context.location")
                        .addAsServiceProvider(ThreadContextProvider.class.getName(),
                                              "concurrent.cdi.context.location.LocationContextProvider");
        ShrinkHelper.exportToServer(server, "lib", locationContextProviderJar);

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

        JavaArchive concurrentCDIEJB = ShrinkHelper.buildJavaArchive("concurrentCDIEJB", "concurrent.cdi.ejb");
        ShrinkHelper.addDirectory(concurrentCDIEJB, "test-applications/concurrentCDIEJB/resources");

        EnterpriseArchive concurrentCDIApp = ShrinkWrap.create(EnterpriseArchive.class, "concurrentCDIApp.ear");
        concurrentCDIApp.addAsModule(concurrentCDIWeb);
        concurrentCDIApp.addAsModule(concurrentCDIEJB);
        ShrinkHelper.addDirectory(concurrentCDIApp, "test-applications/concurrentCDIApp/resources");
        ShrinkHelper.exportAppToServer(server, concurrentCDIApp);

        // TODO Adding "concurrent.cu3.web" to the following would cause conflict with app-defined ManagedExecutorService.
        // There is a spec proposal to detect conflict and avoid automatically adding the bean.
        ShrinkHelper.defaultDropinApp(server, APP_NAME_EE10, "concurrent.cdi4.web");
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

    @Test
    public void testContextServiceWithUnrecognizedQualifier() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testEJBSelectContextServiceQualifiedFromAppDD() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testExtensionAddsAsynchronous() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInheritAsynchronous() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectContextServiceDefaultInstance() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectContextServiceQualifiedFromAnno() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectContextServiceQualifiedFromAppDD() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectContextServiceQualifiedFromWebDD() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectManagedExecutorServiceDefaultInstance() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectManagedExecutorServiceQualifiedFromAnno() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectManagedExecutorServiceQualifiedFromWebDD() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectManagedScheduledExecutorServiceDefaultInstance() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectManagedScheduledExecutorServiceQualifiedFromAnno() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectManagedScheduledExecutorServiceQualifiedFromWebDD() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectManagedThreadFactoryDefaultInstance() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectManagedThreadFactoryQualifiedFromAnno() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectManagedThreadFactoryQualifiedFromAppDD() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testInjectManagedThreadFactoryQualifiedFromWebDD() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testLookUpManagedThreadFactory() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testObserveStartup() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testOverrideContextServiceQualifiersViaDD() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testOverrideManagedExecutorQualifiersViaWebDD() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testOverrideManagedScheduledExecutorQualifiersViaWebDD() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testOverrideManagedThreadFactoryQualifiersViaWebDD() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testPostConstruct() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testQualifierEquals() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testQualifierHashCode() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testQualifierToString() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testSelectContextServiceDefaultInstance() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testSelectContextServiceQualified() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testSelectManagedThreadFactoryDefaultInstance() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @Test
    public void testSelectManagedThreadFactoryQualified() throws Exception {
        runTest(server, APP_NAME, testName);
    }
}
