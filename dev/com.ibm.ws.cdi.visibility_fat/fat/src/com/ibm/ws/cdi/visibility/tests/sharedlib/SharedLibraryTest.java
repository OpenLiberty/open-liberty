/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.visibility.tests.sharedlib;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.visibility.tests.sharedlib.crossInjectionLib1Jar.CrossInjectionBean;
import com.ibm.ws.cdi.visibility.tests.sharedlib.crossInjectionLib2Jar.Parent;
import com.ibm.ws.cdi.visibility.tests.sharedlib.crossInjectionWar.CrossInjectionTestServlet;
import com.ibm.ws.cdi.visibility.tests.sharedlib.injectionWar.InjectionTestServlet;
import com.ibm.ws.cdi.visibility.tests.sharedlib.nonInjectionWar.NoInjectionTestServlet;
import com.ibm.ws.cdi.visibility.tests.sharedlib.sharedLibraryJar.InjectedHello;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests for CDI from shared libraries
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class SharedLibraryTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12SharedLibraryServer";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE11, EERepeatActions.EE9, EERepeatActions.EE8);

    public static final String SHARED_NO_INJECT_APP_NAME = "sharedLibraryNoInjection";
    public static final String SHARED_INJECT_APP_NAME = "sharedLibraryInjection";
    public static final String CROSS_INJECT_APP_NAME = "crossInjection";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = InjectionTestServlet.class, contextRoot = SHARED_INJECT_APP_NAME), //FULL
                    @TestServlet(servlet = NoInjectionTestServlet.class, contextRoot = SHARED_NO_INJECT_APP_NAME),
                    @TestServlet(servlet = CrossInjectionTestServlet.class, contextRoot = CROSS_INJECT_APP_NAME) }) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            WebArchive injectionWar = ShrinkWrap.create(WebArchive.class, SHARED_INJECT_APP_NAME + ".war")
                                                .addPackage(InjectionTestServlet.class.getPackage())
                                                .addAsWebInfResource(InjectionTestServlet.class.getResource("beans.xml"), "beans.xml");

            WebArchive noInjectionWar = ShrinkWrap.create(WebArchive.class, SHARED_NO_INJECT_APP_NAME + ".war")
                                                  .addPackage(NoInjectionTestServlet.class.getPackage())
                                                  .addAsWebInfResource(NoInjectionTestServlet.class.getResource("beans.xml"), "beans.xml");

            JavaArchive sharedLibrary = ShrinkWrap.create(JavaArchive.class, "sharedLibrary.jar")
                                                  .addPackage(InjectedHello.class.getPackage())
                                                  .addAsManifestResource(InjectedHello.class.getResource("beans.xml"), "beans.xml");

            /// cross injection archives begin
            WebArchive crossInjectionWar = ShrinkWrap.create(WebArchive.class, CROSS_INJECT_APP_NAME + ".war")
                                                     .addPackage(CrossInjectionTestServlet.class.getPackage())
                                                     .addAsWebInfResource(CrossInjectionTestServlet.class.getResource("beans.xml"), "beans.xml");

            JavaArchive crossInjectionLib1Jar = ShrinkWrap.create(JavaArchive.class, "crossInjectionLib1.jar")
                                                          .addPackage(CrossInjectionBean.class.getPackage())
                                                          .addAsManifestResource(CrossInjectionBean.class.getResource("beans.xml"), "beans.xml");

            JavaArchive crossInjectionLib2Jar = ShrinkWrap.create(JavaArchive.class, "crossInjectionLib2.jar")
                                                          .addPackage(Parent.class.getPackage())
                                                          .addAsManifestResource(Parent.class.getResource("beans.xml"), "beans.xml");

            /// cross injection archives end

            ShrinkHelper.exportAppToServer(server, noInjectionWar, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportAppToServer(server, injectionWar, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportAppToServer(server, crossInjectionWar, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportToServer(server, "InjectionSharedLibrary", sharedLibrary, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportToServer(server, "commonLibraryCrossInjectionTest", crossInjectionLib1Jar, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportToServer(server, "commonLibraryCrossInjectionTest", crossInjectionLib2Jar, DeployOptions.SERVER_ONLY);
        }

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
