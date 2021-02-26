/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE8_FULL;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi12.test.web1.NoInjectionServlet;
import com.ibm.ws.cdi12.test.web1.SharedLibraryServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.EERepeatTests;
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
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE8_FULL);

    public static final String SHARED_NO_INJECT_APP_NAME = "sharedLibraryNoInjectionApp";
    public static final String SHARED_LIB_APP_NAME = "sharedLibraryAppWeb1";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = SharedLibraryServlet.class, contextRoot = SHARED_LIB_APP_NAME), //FULL
                    @TestServlet(servlet = NoInjectionServlet.class, contextRoot = SHARED_NO_INJECT_APP_NAME) }) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            WebArchive sharedLibraryAppWeb = ShrinkWrap.create(WebArchive.class, "sharedLibraryAppWeb1.war")
                                                       .addClass(com.ibm.ws.cdi12.test.web1.SharedLibraryServlet.class);

            WebArchive sharedLibraryNoInjectionApp = ShrinkWrap.create(WebArchive.class, "sharedLibraryNoInjectionApp.war")
                                                               .addClass(com.ibm.ws.cdi12.test.web1.NoInjectionServlet.class);

            JavaArchive sharedLibrary = ShrinkWrap.create(JavaArchive.class, "sharedLibrary.jar")
                                                  .addClass(com.ibm.ws.cdi12.test.shared.NonInjectedHello.class)
                                                  .addClass(com.ibm.ws.cdi12.test.shared.InjectedHello.class);

            ShrinkHelper.exportAppToServer(server, sharedLibraryNoInjectionApp, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportAppToServer(server, sharedLibraryAppWeb, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportToServer(server, "InjectionSharedLibrary", sharedLibrary, DeployOptions.SERVER_ONLY);
        }

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
