/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.appConfig.dynamicSources.test.DynamicSourcesTestServlet;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class DynamicSourcesTest extends FATServletClient {

    public static final String APP_NAME = "dynamicSources";

    @Server("DynamicSourcesServer")
    @TestServlet(servlet = DynamicSourcesTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    // Don't repeat against mpConfig > 1.4 since polling behaviour changed.
    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat("DynamicSourcesServer", MicroProfileActions.MP12, MicroProfileActions.MP20);

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive dynamicSources_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                                  .addPackages(true, "com.ibm.ws.microprofile.appConfig.dynamicSources.test")
                                                  .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                                                  .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                                                  .addAsManifestResource(new File("test-applications/" + APP_NAME
                                                                                  + ".war/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource"),
                                                                         "services/org.eclipse.microprofile.config.spi.ConfigSource");

        ShrinkHelper.exportDropinAppToServer(server, dynamicSources_war, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMCG0016E"); //On shutdown allow "The server is unable to cancel the asynchronous update thread."
    }

}
