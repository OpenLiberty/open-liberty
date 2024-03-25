/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
import mpRestClient10.handleresponses.ClientTestServlet;

@RunWith(FATRunner.class)
public class HandleResponsesTest extends FATServletClient {

    final static String REMOTE_SERVER_NAME = "mpRestClient10.remoteServer";
    final static String SERVER_NAME = "mpRestClient10.handleresponses";

    @ClassRule
    public static RepeatTests r = FATSuite.repeatMP13Up(SERVER_NAME, REMOTE_SERVER_NAME);

    private static final String appName = "handleresponsesApp";

    /*
     * We need two servers to clearly distiguish that the "client" server
     * only has the client features enabled - it includes mpRestClient-1.0
     * which includes the jaxrsClient-2.0 feature, but not the jaxrs-2.0
     * feature that contains server code. The client should be able to
     * work on its own - by splitting out the "server" server into it's
     * own server, we can verify this.
     */
    @Server(SERVER_NAME)
    @TestServlet(servlet = ClientTestServlet.class, contextRoot = appName)
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
        remoteAppServer.waitForStringInLog("CWWKO0219I.*ssl"); // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host *  (IPv6) port 8020.

        ShrinkHelper.defaultDropinApp(server, appName, new DeployOptions[] {DeployOptions.SERVER_ONLY}, "mpRestClient10.handleresponses");
        if (JakartaEEAction.isEE9Active()) {
            server.changeFeatures(Arrays.asList("componenttest-2.0", "mpRestClient-3.0", "ssl-1.0", "jsonb-2.0"));
        } else if (JakartaEEAction.isEE10Active()) {
            server.changeFeatures(Arrays.asList("componenttest-2.0", "mpRestClient-3.0", "ssl-1.0", "jsonb-3.0", "servlet-6.0"));
        } else if (JakartaEEAction.isEE11Active()) {
            server.changeFeatures(Arrays.asList("componenttest-2.0", "mpRestClient-3.0", "ssl-1.0", "jsonb-3.0", "servlet-6.1"));
        }
        server.startServer();
        server.waitForStringInLog("CWWKO0219I.*ssl"); // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host *  (IPv6) port 8020.
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines
        remoteAppServer.stopServer("CWWKE1102W");
    }
}