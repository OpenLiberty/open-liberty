/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf.container.fat.FATSuite;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class ClassloadingTest extends FATServletClient {

    public static final String JSF_APP = "jsfApp";
    public static final String JSF_EAR_APP = "jsfEarApp";
    public static final String NO_JSF_APP = "noJsfApp";

    @Server("jsf.container.2.3_fat.config")
    public static LibertyServer server;

    private static boolean isEE10;
    private static boolean isEE9;

    @BeforeClass
    public static void setUp() throws Exception {

        isEE10 = JakartaEE10Action.isActive();
        isEE9 = JakartaEE9Action.isActive();
        // Build test app with JSF (Mojarra) and a test servlet
        WebArchive jsfApp = ShrinkHelper.buildDefaultApp(JSF_APP, "jsf.container.bean", "jsf.container.nojsf.web");

        // Don't add the managed bean package for EE10
        if (!isEE10) {
            jsfApp.addPackage("jsf.container.bean.jsf23");
        }
        jsfApp = (WebArchive) ShrinkHelper.addDirectory(jsfApp, "publish/files/permissions");
        FATSuite.addMojarra(jsfApp);
        ShrinkHelper.exportAppToServer(server, jsfApp);
        server.addInstalledAppForValidation(JSF_APP);

        // Build test app with just a test servlet (i.e. no JSF usage)
        ShrinkHelper.defaultApp(server, NO_JSF_APP, "jsf.container.nojsf.web");

        String mojarraLibraryLocation;
        WebArchive mojarraAppWar;

        // Multiple checks due to the managed bean refactoring for faces 4.0
        if (isEE10) {
            mojarraLibraryLocation = "publish/files/mojarra40/";
            mojarraAppWar = ShrinkHelper.buildDefaultApp(JSF_EAR_APP, "jsf.container.bean", "jsf.container.nojsf.web")
                            .addAsWebResource(new File("test-applications/jsfApp/resources/TestBean.xhtml"))
                            .addAsLibraries(new File(mojarraLibraryLocation).listFiles());
        } else if (isEE9) {
            mojarraLibraryLocation = "publish/files/mojarra30/";
            mojarraAppWar = ShrinkHelper.buildDefaultApp(JSF_EAR_APP, "jsf.container.bean", "jsf.container.bean.jsf23", "jsf.container.nojsf.web")
                            .addAsWebResource(new File("test-applications/jsfApp/resources/TestBean.xhtml"))
                            .addAsLibraries(new File(mojarraLibraryLocation).listFiles());
        } else {
            mojarraLibraryLocation = "publish/files/mojarra/";
            mojarraAppWar = ShrinkHelper.buildDefaultApp(JSF_EAR_APP, "jsf.container.bean", "jsf.container.bean.jsf23", "jsf.container.nojsf.web")
                            .addAsWebResource(new File("test-applications/jsfApp/resources/TestBean.xhtml"))
                            .addAsLibraries(new File(mojarraLibraryLocation).listFiles());
        }
        // Build test WAR in EAR application with JSF API+impl in WAR
        EnterpriseArchive jsfEarApp = ShrinkWrap.create(EnterpriseArchive.class, JSF_EAR_APP + ".ear")
                        .addAsModule(mojarraAppWar);

        jsfEarApp = (EnterpriseArchive) ShrinkHelper.addDirectory(jsfEarApp, "publish/files/permissions");
        ShrinkHelper.exportAppToServer(server, jsfEarApp);

        // Create some jar that we can use as a library
        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, "someLib.jar")
                        .addPackage("jsf.container.somelib");
        ShrinkHelper.exportToServer(server, "lib", libJar);
    }

    @After
    public void afterEach() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testPrivateLib() throws Exception {
        runTest();
    }

    @Test
    public void testCommonLib() throws Exception {
        runTest();
    }

    @Test
    public void testGlobalLib() throws Exception {
        runTest();
    }

    private void runTest() throws Exception {
        if (isEE10) {
            server.setServerConfigurationFile("server_" + testName.getMethodName().replace("_EE10_FEATURES", "") + ".xml");
        } else if (isEE9) {
            server.setServerConfigurationFile("server_" + testName.getMethodName().replace("_EE9_FEATURES", "") + ".xml");
        } else {
            server.setServerConfigurationFile("server_" + testName.getMethodName() + ".xml");
        }

        server.startServer(testName.getMethodName() + ".log");

        // Verify that basic JSF works in a WAR
        HttpUtils.findStringInReadyUrl(server, '/' + JSF_APP + "/TestBean.jsf",
                                       "CDI Bean value:",
                                       ":CDIBean::PostConstructCalled:");
        if (!isEE10) {
            HttpUtils.findStringInReadyUrl(server, '/' + JSF_APP + "/TestBean.jsf",
                                           "JSF Bean value:",
                                           ":JSFBean::PostConstructCalled:");
        }

        // Verify that basic JSF works in an EAR
        HttpUtils.findStringInReadyUrl(server, '/' + JSF_EAR_APP + "/TestBean.jsf",
                                       "CDI Bean value:",
                                       ":CDIBean::PostConstructCalled:");
        if (!isEE10) {
            HttpUtils.findStringInReadyUrl(server, '/' + JSF_EAR_APP + "/TestBean.jsf",
                                           "JSF Bean value:",
                                           ":JSFBean::PostConstructCalled:");
        }

        // Verify non-JSF functionality works in JSF-enabled WAR app
        FATServletClient.runTest(server, JSF_APP + "/TestServlet", "testServletWorking");
        FATServletClient.runTest(server, JSF_APP + "/TestServlet", "useExternalLib");

        // Verify non-JSF functionality works in JSF-enabled EAR app
        FATServletClient.runTest(server, JSF_EAR_APP + "/TestServlet", "testServletWorking");
        FATServletClient.runTest(server, JSF_EAR_APP + "/TestServlet", "useExternalLib");

        // Verify that using a non-JSF app works
        FATServletClient.runTest(server, NO_JSF_APP + "/TestServlet", "testServletWorking");
        FATServletClient.runTest(server, NO_JSF_APP + "/TestServlet", "useExternalLib");
    }
}
