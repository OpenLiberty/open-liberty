/*******************************************************************************
 * Copyright (c) 2016, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.archaius.impl.fat.tests.SharedLibUserTestServlet;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.config.fat.repeat.ConfigRepeatActions;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SharedLibTest extends FATServletClient {

    public static final String SERVER_NAME = "SharedLibUserServer";
    public static final String APP_NAME = "sharedLibUser";

    @Server(SERVER_NAME)
    @TestServlet(servlet = SharedLibUserTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ConfigRepeatActions.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        //TODO differentiate the pacakage names for these two jars.
        JavaArchive sharedLib_jar = ShrinkWrap.create(JavaArchive.class, "sharedLib.jar")
                                              .addClass("com.ibm.ws.microprofile.archaius.impl.fat.tests.PingableSharedLibClass")
                                              .addAsManifestResource(new File("test-applications/sharedLib.jar/resources/META-INF/microprofile-config.json"),
                                                                     "microprofile-config.json")
                                              .addAsManifestResource(new File("test-applications/sharedLib.jar/resources/META-INF/microprofile-config.properties"),
                                                                     "microprofile-config.properties")
                                              .addAsManifestResource(new File("test-applications/sharedLib.jar/resources/META-INF/microprofile-config.xml"),
                                                                     "microprofile-config.xml");

        JavaArchive sharedLibUser_jar = ShrinkWrap.create(JavaArchive.class, APP_NAME + ".jar")
                                                  .addClass("com.ibm.ws.microprofile.archaius.impl.fat.tests.SharedLibUserTestServlet");

        WebArchive sharedLibUser_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                                 .addAsLibrary(sharedLibUser_jar)
                                                 .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                                                 .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml");

        ShrinkHelper.exportAppToServer(server, sharedLibUser_war, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "shared", sharedLib_jar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
