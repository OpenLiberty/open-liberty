/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package com.ibm.ws.microprofile.rest.client.fat;

import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpRestClient10.multiClientCdi.MultiClientCdiTestServlet;

@RunWith(FATRunner.class)
public class MultiClientCdiTest extends FATServletClient {

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    final static String SERVER_NAME = "mpRestClient10.multi.client.cdi";

    // To avoid bogus timeout build-breaks on slow Windows hardware only run a few versions on 
    // Windows.
    @ClassRule
    public static RepeatTests r;
    static {
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) {
            r = MicroProfileActions.repeat(SERVER_NAME, 
                                           MicroProfileActions.MP13, //mpRestClient-1.0
                                           MicroProfileActions.MP20, //mpRestClient-1.1
                                           MicroProfileActions.MP22, // 1.2
                                           MicroProfileActions.MP30, // 1.3
                                           MicroProfileActions.MP33, // 1.4
                                           MicroProfileActions.MP40, // 2.0
                                           MicroProfileActions.MP50, // 3.0
                                           MicroProfileActions.MP60);// 3.0+EE10

        } else {
            r = MicroProfileActions.repeat(SERVER_NAME, 
                                           MicroProfileActions.MP13, //mpRestClient-1.0 
                                           MicroProfileActions.MP60);// 3.0+EE10

        }
    }

    private static final String appName = "multiClientCdiApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = MultiClientCdiTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, appName, "mpRestClient10.multiClientCdi");
        ShrinkHelper.defaultDropinApp(server, appName + "2", "mpRestClient10.multiClientCdi"); // install app twice
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines
    }

    // The following tests verify that the second app can do the same as the first app.
    @Test
    public void testCanInvokeDifferentClients() throws Exception {
        runTest(server, appName + "2/MultiClientCdiTestServlet", "testCanInvokeDifferentClients");
    }

    @Test
    public void testSameClientsGetSameResults() throws Exception {
        runTest(server, appName + "2/MultiClientCdiTestServlet", "testSameClientsGetSameResults");
    }
}