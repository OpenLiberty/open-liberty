/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.visibility.tests.overridelib;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.visibility.tests.overridelib.overrideBeanClassWar.OverrideBeanClassTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests for CDI from shared libraries
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OverrideLibraryTest extends FATServletClient {

    public static final String SERVER_NAME = "overrideBeanClassServer";

//    @ClassRule
//    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE11, EERepeatActions.EE9, EERepeatActions.EE8);

    public static final String OVERRIDE_BEAN_CLASS_APP_NAME = "overrideBeanClass";

    @Server(SERVER_NAME)
    @TestServlet(servlet = OverrideBeanClassTestServlet.class, contextRoot = OVERRIDE_BEAN_CLASS_APP_NAME) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            WebArchive overrideBeanClassWar = ShrinkWrap.create(WebArchive.class, OVERRIDE_BEAN_CLASS_APP_NAME + ".war")
                                                        .addPackage(OverrideBeanClassTestServlet.class.getPackage())
                                                        .addAsWebInfResource(OverrideBeanClassTestServlet.class.getResource("beans.xml"), "beans.xml");

            ShrinkHelper.exportAppToServer(server, overrideBeanClassWar, DeployOptions.SERVER_ONLY);
            server.copyFileToLibertyServerRoot("beanJars", null, "jarA.jar");
            server.copyFileToLibertyServerRoot("beanJars", null, "jarB.jar");
        }

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
