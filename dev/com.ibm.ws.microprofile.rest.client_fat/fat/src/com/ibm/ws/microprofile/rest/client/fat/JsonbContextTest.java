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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpRestClient12.jsonbContext.JsonbContextTestServlet;

@RunWith(FATRunner.class)
public class JsonbContextTest extends FATServletClient {

    final static String REMOTE_SERVER_NAME = "mpRestClient10.remoteServer";
    final static String SERVER_NAME = "mpRestClient12.jsonbContext";

    @ClassRule
    public static RepeatTests r = FATSuite.repeatMP22Up(SERVER_NAME, REMOTE_SERVER_NAME);

    private static final String appName = "jsonbContextApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = JsonbContextTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @Server(REMOTE_SERVER_NAME)
    public static LibertyServer remoteAppServer;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(remoteAppServer, "basicRemoteApp", new DeployOptions[] {DeployOptions.OVERWRITE, DeployOptions.SERVER_ONLY}, "remoteApp.basic");
        if (JakartaEEAction.isEE9OrLaterActive()) {
            remoteAppServer.changeFeatures(Arrays.asList("componenttest-2.0", "restfulWS-3.0", "ssl-1.0", "jsonb-2.0"));
        }
        remoteAppServer.startServer();

        ShrinkHelper.defaultDropinApp(server, appName, new DeployOptions[] {DeployOptions.SERVER_ONLY}, "mpRestClient12.jsonbContext");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines
        remoteAppServer.stopServer("CWWKE1102W");
    }
}