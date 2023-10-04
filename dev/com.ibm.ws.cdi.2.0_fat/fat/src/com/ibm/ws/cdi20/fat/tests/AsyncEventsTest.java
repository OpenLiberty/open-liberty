/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi20.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi20.fat.apps.asyncEvents.AsyncEventsServlet;
import com.ibm.ws.cdi20.fat.apps.asyncEvents.CakeArrival;
import com.ibm.ws.cdi20.fat.apps.asyncEvents.CakeObserver;
import com.ibm.ws.cdi20.fat.apps.asyncEvents.CakeReport;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * These tests verify the use of asynchronous events in CDI2.0 as per http://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#firing_events_asynchronously
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AsyncEventsTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi20AsyncEventsServer";

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    public static final String APP_NAME = "asyncEventsApp";

    @Server(SERVER_NAME)
    @TestServlets({ @TestServlet(servlet = AsyncEventsServlet.class, contextRoot = APP_NAME) }) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addClass(AsyncEventsServlet.class)
                                   .addClass(CakeArrival.class)
                                   .addClass(CakeObserver.class)
                                   .addClass(CakeReport.class);

        ShrinkHelper.exportAppToServer(server, app, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
