/*******************************************************************************
* Copyright (c) 2016, 2018 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/

package com.ibm.ws.microprofile.config.fat.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class ClassLoaderCacheTest extends FATServletClient {

    public static final String APP_NAME = "classLoaderCache";

    public static final String EARA = APP_NAME + "A";
    public static final String EARB = APP_NAME + "B";
    public static final String EARA_NAME = EARA + ".ear";
    public static final String EARB_NAME = EARB + ".ear";

    public static final String WARA1 = EARA + "1";
    public static final String WARA2 = EARA + "2";
    public static final String WARA1_NAME = WARA1 + ".war";
    public static final String WARA2_NAME = WARA2 + ".war";

    public static final String WARB1 = EARB + "1";
    public static final String WARB2 = EARB + "2";
    public static final String WARB1_NAME = WARB1 + ".war";
    public static final String WARB2_NAME = WARB2 + ".war";

    @Server("ClassLoaderCacheServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        JavaArchive testAppUtils = SharedShrinkWrapApps.getTestAppUtilsJar();

        WebArchive classLoaderCacheA1_war = ShrinkWrap.create(WebArchive.class, WARA1_NAME)
                        .addAsLibrary(testAppUtils)
                        .addPackage("com.ibm.ws.microprofile.appConfig.classLoaderCache1.test");

        WebArchive classLoaderCacheA2_war = ShrinkWrap.create(WebArchive.class, WARA2_NAME)
                        .addAsLibrary(testAppUtils)
                        .addPackage("com.ibm.ws.microprofile.appConfig.classLoaderCache2.test");

        WebArchive classLoaderCacheB1_war = ShrinkWrap.create(WebArchive.class, WARB1_NAME)
                        .addAsLibrary(testAppUtils)
                        .addPackage("com.ibm.ws.microprofile.appConfig.classLoaderCache1.test");

        WebArchive classLoaderCacheB2_war = ShrinkWrap.create(WebArchive.class, WARB2_NAME)
                        .addAsLibrary(testAppUtils)
                        .addPackage("com.ibm.ws.microprofile.appConfig.classLoaderCache2.test");

        EnterpriseArchive classLoaderCacheA_ear = ShrinkWrap.create(EnterpriseArchive.class, EARA_NAME)
                        .addAsManifestResource(new File("test-applications/" + EARA_NAME + "/resources/META-INF/application.xml"), "application.xml")
                        .addAsManifestResource(new File("test-applications/" + EARA_NAME + "/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsModule(classLoaderCacheA1_war)
                        .addAsModule(classLoaderCacheA2_war);

        EnterpriseArchive classLoaderCacheB_ear = ShrinkWrap.create(EnterpriseArchive.class, EARB_NAME)
                        .addAsManifestResource(new File("test-applications/" + EARB_NAME + "/resources/META-INF/application.xml"), "application.xml")
                        .addAsManifestResource(new File("test-applications/" + EARB_NAME + "/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsModule(classLoaderCacheB1_war)
                        .addAsModule(classLoaderCacheB2_war);

        ShrinkHelper.exportDropinAppToServer(server, classLoaderCacheA_ear);
        ShrinkHelper.exportDropinAppToServer(server, classLoaderCacheB_ear);
    }

    @Before
    public void startServer() throws Exception {
        server.startServer();
    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }

    @Test
    public void testClassLoaderCache() throws Exception {
        FATServletClient.runTest(server, WARA1, "testClassLoaderCache");
        FATServletClient.runTest(server, WARA2, "testClassLoaderCache");
        server.restartDropinsApplication(EARA_NAME);
        Thread.sleep(1000);
        FATServletClient.runTest(server, WARA1, "testClassLoaderCache");
        FATServletClient.runTest(server, WARA2, "testClassLoaderCache");
    }

    @Test
    public void testMultiApplication() throws Exception {
        FATServletClient.runTest(server, WARA1, "testClassLoaderCache");
        FATServletClient.runTest(server, WARA2, "testClassLoaderCache");
        FATServletClient.runTest(server, WARB1, "testClassLoaderCache");
        FATServletClient.runTest(server, WARB2, "testClassLoaderCache");
        server.removeDropinsApplications(EARB_NAME);
        Thread.sleep(1000);
        FATServletClient.runTest(server, WARA1, "testClassLoaderCache");
        FATServletClient.runTest(server, WARA2, "testClassLoaderCache");
    }

}
