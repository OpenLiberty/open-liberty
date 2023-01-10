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

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi20.fat.apps.secureAsyncEvents.SecureAsyncEventsServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * These tests verify the use of asynchronous events in CDI2.0 as per http://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#firing_events_asynchronously
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SecureAsyncEventsTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi20SecureAsyncEventsServer";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE9, EERepeatActions.EE10, EERepeatActions.EE8);

    public static final String APP_NAME = "secureAsyncEventsApp";

    @Server(SERVER_NAME)
    @TestServlets({ @TestServlet(servlet = SecureAsyncEventsServlet.class, contextRoot = APP_NAME) }) //FULL

    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackages(true, SecureAsyncEventsServlet.class.getPackage());

        ShrinkHelper.exportAppToServer(server, app, DeployOptions.SERVER_ONLY);

        server.startServer();

        assertNotNull("CWWKF0011I.* not received on server", server.waitForStringInLog("CWWKF0011I.*")); // wait for server is ready to run a smarter planet
        assertNotNull("Security service did not report it was ready", server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("CWWKS4105I.* not received on server", server.waitForStringInLog("CWWKS4105I.*")); // wait for LTPA key to be available
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
