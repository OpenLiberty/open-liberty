/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class ClassloadingTest extends FATServletClient {

    public static final String JSF_APP = "jsfApp";
    public static final String JSF_EAR_APP = "jsfEarApp";
    public static final String NO_JSF_APP = "noJsfApp";

    @Server("jsf.container.2.2_fat.config")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Build test app with JSF (Mojarra) and a test servlet
        WebArchive jsfApp = ShrinkHelper.buildDefaultApp(JSF_APP, "jsf.container.bean", "jsf.container.nojsf.web");
        FATSuite.addMojarra(jsfApp);
        jsfApp = (WebArchive) ShrinkHelper.addDirectory(jsfApp, "test-applications/jsfApp/resources");
        ShrinkHelper.exportAppToServer(server, jsfApp);
        server.addInstalledAppForValidation(JSF_APP);

        // Build test app with just a test servlet (i.e. no JSF usage)
        ShrinkHelper.defaultApp(server, NO_JSF_APP, "jsf.container.nojsf.web");

        // Build test WAR in EAR application with JSF API+impl in WAR
        EnterpriseArchive jsfEarApp = ShrinkWrap.create(EnterpriseArchive.class, JSF_EAR_APP + ".ear")
                        .addAsModule(ShrinkHelper.buildDefaultApp(JSF_EAR_APP, "jsf.container.bean", "jsf.container.nojsf.web")
                                        .addAsWebResource(new File("test-applications/jsfApp/resources/TestBean.xhtml"))
                                        .addAsLibraries(new File("publish/files/mojarra/").listFiles()));
        jsfEarApp = (EnterpriseArchive) ShrinkHelper.addDirectory(jsfEarApp, "test-applications/jsfApp/resources");
        ShrinkHelper.exportAppToServer(server, jsfEarApp);

        // Create some jar that we can use as a library
        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, "someLib.jar")
                        .addPackage("jsf.container.somelib");
        ShrinkHelper.exportToServer(server, "lib", libJar);
    }

    @After
    public void afterEach() throws Exception {
        server.stopServer();
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
        server.setServerConfigurationFile("server_" + testName.getMethodName() + ".xml");
        server.startServer(testName.getMethodName() + ".log");

        // Verify that basic JSF works in a WAR
        HttpUtils.findStringInReadyUrl(server, '/' + JSF_APP + "/TestBean.jsf",
                                       "CDI Bean value:",
                                       ":CDIBean::PostConstructCalled:");
        HttpUtils.findStringInReadyUrl(server, '/' + JSF_APP + "/TestBean.jsf",
                                       "JSF Bean value:",
                                       ":JSFBean::PostConstructCalled:");

        // Verify that basic JSF works in an EAR
        HttpUtils.findStringInReadyUrl(server, '/' + JSF_EAR_APP + "/TestBean.jsf",
                                       "CDI Bean value:",
                                       ":CDIBean::PostConstructCalled:");
        HttpUtils.findStringInReadyUrl(server, '/' + JSF_EAR_APP + "/TestBean.jsf",
                                       "JSF Bean value:",
                                       ":JSFBean::PostConstructCalled:");

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
