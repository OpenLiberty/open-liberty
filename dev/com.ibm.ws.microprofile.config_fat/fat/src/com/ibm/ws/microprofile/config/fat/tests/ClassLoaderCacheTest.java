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
import org.junit.AfterClass;
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
    public static final String EAR_NAME = APP_NAME + ".ear";
    public static final String WAR1 = APP_NAME + "1";
    public static final String WAR2 = APP_NAME + "2";
    public static final String WAR1_NAME = WAR1 + ".war";
    public static final String WAR2_NAME = WAR2 + ".war";

    @Server("ClassLoaderCacheServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        JavaArchive testAppUtils = SharedShrinkWrapApps.getTestAppUtilsJar();

        WebArchive classLoaderCache1_war = ShrinkWrap.create(WebArchive.class, WAR1_NAME)
                        .addAsLibrary(testAppUtils)
                        .addPackage("com.ibm.ws.microprofile.appConfig.classLoaderCache1.test");

        WebArchive classLoaderCache2_war = ShrinkWrap.create(WebArchive.class, WAR2_NAME)
                        .addAsLibrary(testAppUtils)
                        .addPackage("com.ibm.ws.microprofile.appConfig.classLoaderCache2.test");

        EnterpriseArchive classLoaderCache_ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME)
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".ear/resources/META-INF/application.xml"), "application.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".ear/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsModule(classLoaderCache1_war)
                        .addAsModule(classLoaderCache2_war);

        ShrinkHelper.exportDropinAppToServer(server, classLoaderCache_ear);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testClassLoaderCache() throws Exception {
        FATServletClient.runTest(server, WAR1, "testClassLoaderCache");
        FATServletClient.runTest(server, WAR2, "testClassLoaderCache");
        server.restartDropinsApplication(EAR_NAME);
        Thread.sleep(1000);
        FATServletClient.runTest(server, WAR1, "testClassLoaderCache");
        FATServletClient.runTest(server, WAR2, "testClassLoaderCache");
    }

}
