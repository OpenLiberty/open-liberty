/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpRestClient11.cdiPropsAndProviders.CdiPropsAndProvidersClient;
import mpRestClient11.cdiPropsAndProviders.CdiPropsAndProvidersTestServlet;
import mpRestClient11.cdiPropsAndProviders.UnusedBeanWithMPRestClient;
import mpRestClient11.cdiPropsAndProviders.UnusedClient;

@RunWith(FATRunner.class)
public class CdiPropsAndProvidersTest extends FATServletClient {

    private static final String appName = "cdiPropsAndProvidersApp";
    final static String SERVER_NAME = "mpRestClient11.cdiPropsAndProviders";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
        .andWith(FATSuite.MP_REST_CLIENT("1.2", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT("1.3", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT("1.4", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT_WITH_CONFIG("2.0", SERVER_NAME));

    @Server(SERVER_NAME)
    @TestServlet(servlet = CdiPropsAndProvidersTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @Server("mpRestClient10.remoteServer")
    public static LibertyServer remoteAppServer;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(remoteAppServer, "basicRemoteApp", "remoteApp.basic");
        remoteAppServer.startServer();

        ShrinkHelper.defaultDropinApp(server, appName, "mpRestClient11.cdiPropsAndProviders");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        try {
            List<String> requestScopedIntfMsgs = server.findStringsInLogs("CWWKW0750I.*" + CdiPropsAndProvidersClient.class.getName());
            assertNotNull("Did not find expected CWWKW0750I message about request scoped interfaces", requestScopedIntfMsgs);
            assertEquals("Found unexpected number of CWWKW0750I messages about request scoped interfaces (should be 1)",
                         1, requestScopedIntfMsgs.size());
            String requestScopedIntfMsg = requestScopedIntfMsgs.get(0);
            assertTrue("Did not find expected @Dependent interface injected into @RequestScoped bean in CWWKW0750I message: " + requestScopedIntfMsg,
                       requestScopedIntfMsg.contains(UnusedClient.class.getName() + "(" + UnusedBeanWithMPRestClient.class.getName() + ")"));
        } finally {
            server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines
            remoteAppServer.stopServer("CWWKE1102W");
        }
    }
}