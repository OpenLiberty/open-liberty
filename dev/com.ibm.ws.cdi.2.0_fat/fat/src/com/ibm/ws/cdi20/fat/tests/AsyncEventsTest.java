/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi20.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE8_FULL;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9_FULL;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import asyncEventsApp.web.AsyncEventsServlet;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatTests;
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
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9_FULL, EE8_FULL);

    public static final String APP_NAME = "asyncEventsApp";

    @Server(SERVER_NAME)
    @TestServlets({ @TestServlet(servlet = AsyncEventsServlet.class, contextRoot = APP_NAME) }) //FULL
    
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackages(true, APP_NAME + ".web")
                                   .addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/index.jsp"));
        
        ShrinkHelper.exportAppToServer(server, app, DeployOptions.SERVER_ONLY); 

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
