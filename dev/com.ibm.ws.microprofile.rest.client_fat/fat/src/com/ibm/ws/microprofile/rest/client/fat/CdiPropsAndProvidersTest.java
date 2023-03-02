/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpRestClient11.cdiPropsAndProviders.CdiPropsAndProvidersClient;
import mpRestClient11.cdiPropsAndProviders.CdiPropsAndProvidersTestServlet;
import mpRestClient11.cdiPropsAndProviders.UnusedBeanWithMPRestClient;
import mpRestClient11.cdiPropsAndProviders.UnusedClient;

@RunWith(FATRunner.class)
public class CdiPropsAndProvidersTest extends FATServletClient {

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    private static final String appName = "cdiPropsAndProvidersApp";
    final static String SERVER_NAME = "mpRestClient11.cdiPropsAndProviders";

    // To avoid bogus timeout build-breaks on slow Windows hardware only run a few versions on 
    // Windows.
   @ClassRule
    public static RepeatTests r;
    static {
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) {
            r = MicroProfileActions.repeat(SERVER_NAME, 
                                           MicroProfileActions.MP20, //mpRestClient-1.1
                                           MicroProfileActions.MP22, // 1.2
                                           MicroProfileActions.MP30, // 1.3
                                           MicroProfileActions.MP33, // 1.4
                                           MicroProfileActions.MP40); // 2.0

        } else {
            r = MicroProfileActions.repeat(SERVER_NAME, 
                                           MicroProfileActions.MP20, //mpRestClient-1.1
                                           MicroProfileActions.MP60);// 3.0+EE10

        }
    }

    @Server(SERVER_NAME)
    @TestServlet(servlet = CdiPropsAndProvidersTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @Server("mpRestClient10.remoteServer")
    public static LibertyServer remoteAppServer;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(remoteAppServer, "basicRemoteApp", new DeployOptions[] {DeployOptions.OVERWRITE}, "remoteApp.basic");
        if (JakartaEE9Action.isActive()) {
            remoteAppServer.changeFeatures(Arrays.asList("componenttest-2.0", "restfulWS-3.0", "ssl-1.0", "jsonb-2.0"));
        }
        remoteAppServer.startServer();

        ShrinkHelper.defaultDropinApp(server, appName, "mpRestClient11.cdiPropsAndProviders");
        if (JakartaEE9Action.isActive()) {
            server.changeFeatures(Arrays.asList("componenttest-2.0", "mpRestClient-3.0", "mpConfig-3.0", "cdi-3.0", "jsonb-2.0"));
        }
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        try {
            if (!JakartaEE9Action.isActive()) {
                List<String> requestScopedIntfMsgs = server.findStringsInLogs("CWWKW0750I.*" + CdiPropsAndProvidersClient.class.getName());
                assertNotNull("Did not find expected CWWKW0750I message about request scoped interfaces", requestScopedIntfMsgs);
                assertEquals("Found unexpected number of CWWKW0750I messages about request scoped interfaces (should be 1)",
                         1, requestScopedIntfMsgs.size());
                String requestScopedIntfMsg = requestScopedIntfMsgs.get(0);
                assertTrue("Did not find expected @Dependent interface injected into @RequestScoped bean in CWWKW0750I message: " + requestScopedIntfMsg,
                       requestScopedIntfMsg.contains(UnusedClient.class.getName() + "(" + UnusedBeanWithMPRestClient.class.getName() + ")"));
            }
        } finally {
            server.stopServer("CWWKE1102W", "CWWKZ0002E");  //ignore server quiesce timeouts due to slow test machines
            remoteAppServer.stopServer("CWWKE1102W");
        }
    }
}