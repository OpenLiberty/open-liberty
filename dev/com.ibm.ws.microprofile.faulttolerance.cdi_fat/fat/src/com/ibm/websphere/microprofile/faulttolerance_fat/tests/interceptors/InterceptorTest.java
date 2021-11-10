/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.interceptors;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.faulttolerance.fat.repeat.RepeatFaultTolerance;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants;

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
 * Tests for interactions between fault tolerance and application interceptors
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class InterceptorTest extends FATServletClient {

    @ClassRule
    public static RepeatTests r = RepeatFaultTolerance.repeat("AsyncFaultTolerance", TestMode.FULL, MicroProfileActions.MP50, MicroProfileActions.MP41, MicroProfileActions.MP22);

    private static final String APP_NAME = "ftInterceptors";

    @Server("AsyncFaultTolerance")
    @TestServlet(contextRoot = APP_NAME, servlet = InterceptorTestServlet.class)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(InterceptorTestServlet.class.getPackage())
                        .addClass(TestConstants.class);
        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
        server.deleteFileFromLibertyServerRoot("dropins/" + APP_NAME + ".war");
    }
}
