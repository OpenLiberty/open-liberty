/*******************************************************************************
* Copyright (c) 2016, 2020 IBM Corporation and others.
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
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.appConfig.classLoaders.test.ClassLoadersTestServlet;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfigActions;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfigActions.Version;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ClassLoadersTest extends FATServletClient {

    public static final String APP_NAME = "classLoaders";

    @ClassRule
    public static RepeatTests r = RepeatConfigActions.repeat("ClassLoadersServer", Version.CONFIG13_EE7, Version.LATEST);

    @Server("ClassLoadersServer")
    @TestServlet(servlet = ClassLoadersTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive classLoaders_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                                .addPackages(true, "com.ibm.ws.microprofile.appConfig.classLoaders.test")
                                                .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                                                .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                                                .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.properties"),
                                                                       "microprofile-config.properties")
                                                .add(new FileAsset(new File("test-applications/" + APP_NAME + ".war/resources/CUSTOM-DIR/META-INF/microprofile-config.properties")),
                                                     "/CUSTOM-DIR/META-INF/microprofile-config.properties")
                                                .addAsWebInfResource(new File("test-applications/" + APP_NAME
                                                                              + ".war/resources/WEB-INF/classes/META-INF/microprofile-config.properties"),
                                                                     "classes/META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, classLoaders_war);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE0921W", "CWWKE0912W");
    }

}
