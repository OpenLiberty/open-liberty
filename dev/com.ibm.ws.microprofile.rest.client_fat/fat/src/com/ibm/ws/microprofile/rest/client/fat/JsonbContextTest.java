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

import java.util.Arrays;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpRestClient12.jsonbContext.JsonbContextTestServlet;

@RunWith(FATRunner.class)
public class JsonbContextTest extends FATServletClient {

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    final static String SERVER_NAME = "mpRestClient12.jsonbContext";

    // To avoid bogus timeout build-breaks on slow Windows hardware only run a few versions on 
    // Windows.
    @ClassRule
    public static RepeatTests r;
    static {
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) {
            r = MicroProfileActions.repeat(SERVER_NAME, 
                                           MicroProfileActions.MP22, // 1.2
                                           MicroProfileActions.MP30, // 1.3
                                           MicroProfileActions.MP33, // 1.4
                                           MicroProfileActions.MP40, // 2.0
                                           MicroProfileActions.MP50, // 3.0
                                           MicroProfileActions.MP60);// 3.0+EE10

        } else {
            r = MicroProfileActions.repeat(SERVER_NAME, 
                                           MicroProfileActions.MP22, // 1.2 
                                           MicroProfileActions.MP60);// 3.0+EE10

        }
    }

    private static final String appName = "jsonbContextApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = JsonbContextTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @Server("mpRestClient10.remoteServer")
    public static LibertyServer remoteAppServer;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(remoteAppServer, "basicRemoteApp", new DeployOptions[] {DeployOptions.OVERWRITE}, "remoteApp.basic");
        if (JakartaEE9Action.isActive() | JakartaEE10Action.isActive()) {
            remoteAppServer.changeFeatures(Arrays.asList("componenttest-2.0", "restfulWS-3.0", "ssl-1.0", "jsonb-2.0"));
        }
        remoteAppServer.startServer();

        ShrinkHelper.defaultDropinApp(server, appName, "mpRestClient12.jsonbContext");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines
        remoteAppServer.stopServer("CWWKE1102W");
    }
}