/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.visibility.tests.vistest;

import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForAppClientAsEjbLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForAppClientAsWarLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForCommonLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForEarLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForEjb;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForEjbAppClientLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForEjbAsAppClientLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForEjbAsEjbLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForEjbAsWarLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForEjbLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForEjbWarLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForOverrideLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForPrivateLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForRuntimeExtRegular;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForRuntimeExtSeeApp;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForStandaloneWar;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForStandaloneWarLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForWar;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForWarAppClientLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForWarLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestModuleForWarWebinfLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromAppClient;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromAppClientAsAppClientLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromAppClientAsEjbLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromAppClientAsWarLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromAppClientLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromCommonLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromEarLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromEjb;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromEjbAppClientLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromEjbAsAppClientLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromEjbAsEjbLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromEjbAsWarLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromEjbLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromEjbWarLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromOverrideLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromPrivateLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromRuntimeExtRegular;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromRuntimeExtSeeAll;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromStandaloneWar;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromStandaloneWarLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromWar;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromWarAppClientLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromWarLib;
import static com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.doTestVisibilityFromWarWebinfLib;

import java.util.Map;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.cdi.visibility.tests.vistest.VisTestSetup.Location;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class VisTest extends FATServletClient {

    public static final String SERVER_NAME = "visTestServer";
    public static final String CLIENT_NAME = "visTestClient";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE11, EERepeatActions.EE8, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    public static LibertyServer server;
    public static LibertyClient client = LibertyClientFactory.getLibertyClient(CLIENT_NAME);

    public static Logger LOG = Logger.getLogger(VisTest.class.getName());

    private static Map<Location, String> appClientResults = null;

    @BeforeClass
    public static void setUp() throws Exception {
        appClientResults = VisTestSetup.setUp(server, client, LOG);
    }

    @Test
    public void testVisibilityFromEjb() throws Exception {
        doTestVisibilityFromEjb(server);
    }

    @Test
    public void testVisibilityFromWar() throws Exception {
        doTestVisibilityFromWar(server);
    }

    @Test
    public void testVisibilityFromAppClient() throws Exception {
        doTestVisibilityFromAppClient(appClientResults);
    }

    @Test
    public void testVisibilityFromEjbLib() throws Exception {
        doTestVisibilityFromEjbLib(server);
    }

    @Test
    public void testVisibilityFromWarLib() throws Exception {
        doTestVisibilityFromWarLib(server);
    }

    @Test
    public void testVisibilityFromWarWebinfLib() throws Exception {
        doTestVisibilityFromWarWebinfLib(server);
    }

    @Test
    public void testVisibilityFromAppClientLib() throws Exception {
        doTestVisibilityFromAppClientLib(appClientResults);
    }

    @Test
    public void testVisibilityFromEjbWarLib() throws Exception {
        doTestVisibilityFromEjbWarLib(server);
    }

    @Test
    public void testVisibilityFromEjbAppClientLib() throws Exception {
        doTestVisibilityFromEjbAppClientLib(server);
    }

    @Test
    public void testVisibilityFromWarAppClientLib() throws Exception {
        doTestVisibilityFromWarAppClientLib(server);
    }

    @Test
    public void testVisibilityFromEarLib() throws Exception {
        doTestVisibilityFromEarLib(server);
    }

    @Test
    public void testVisibilityFromEjbAsEjbLib() throws Exception {
        doTestVisibilityFromEjbAsEjbLib(server);
    }

    @Test
    public void testVisibilityFromEjbAsWarLib() throws Exception {
        doTestVisibilityFromEjbAsWarLib(server);
    }

    @Test
    public void testVisibilityFromEjbAsAppClientLib() throws Exception {
        doTestVisibilityFromEjbAsAppClientLib(server);
    }

    @Test
    public void testVisibilityFromAppClientAsEjbLib() throws Exception {
        doTestVisibilityFromAppClientAsEjbLib(server);
    }

    @Test
    public void testVisibilityFromAppClientAsWarLib() throws Exception {
        doTestVisibilityFromAppClientAsWarLib(server);
    }

    @Test
    public void testVisibilityFromAppClientAsAppClientLib() throws Exception {
        doTestVisibilityFromAppClientAsAppClientLib(appClientResults);
    }

    @Test
    public void testVisibilityFromCommonLib() throws Exception {
        doTestVisibilityFromCommonLib(server);
    }

    @Test
    public void testVisibilityFromPrivateLib() throws Exception {
        doTestVisibilityFromPrivateLib(server);
    }

    @Test
    public void testVisibilityFromOverrideLib() throws Exception {
        doTestVisibilityFromOverrideLib(server);
    }

    @Test
    public void testVisibilityFromRuntimeExtRegular() throws Exception {
        doTestVisibilityFromRuntimeExtRegular(server, appClientResults);
    }

    @Test
    public void testVisibilityFromRuntimeExtSeeAll() throws Exception {
        doTestVisibilityFromRuntimeExtSeeAll(server, appClientResults);
    }

    @Test
    public void testVisibilityFromStandaloneWar() throws Exception {
        doTestVisibilityFromStandaloneWar(server);
    }

    @Test
    public void testVisibilityFromStandaloneWarLib() throws Exception {
        doTestVisibilityFromStandaloneWarLib(server);
    }

    @Test
    public void testModuleForEjb() throws Exception {
        doTestModuleForEjb(server);
    }

    @Test
    public void testModuleForWar() throws Exception {
        doTestModuleForWar(server);
    }

    @Test
    public void testModuleForEjbLib() throws Exception {
        doTestModuleForEjbLib(server);
    }

    @Test
    public void testModuleForWarLib() throws Exception {
        doTestModuleForWarLib(server);
    }

    @Test
    public void testModuleForWarWebinfLib() throws Exception {
        doTestModuleForWarWebinfLib(server);
    }

    @Test
    public void testModuleForEjbWarLib() throws Exception {
        doTestModuleForEjbWarLib(server);
    }

    @Test
    public void testModuleForEjbAppClientLib() throws Exception {
        doTestModuleForEjbAppClientLib(server);
    }

    @Test
    public void testModuleForWarAppClientLib() throws Exception {
        doTestModuleForWarAppClientLib(server);
    }

    @Test
    public void testModuleForEarLib() throws Exception {
        doTestModuleForEarLib(server);
    }

    @Test
    public void testModuleForEjbAsEjbLib() throws Exception {
        doTestModuleForEjbAsEjbLib(server);
    }

    @Test
    public void testModuleForEjbAsWarLib() throws Exception {
        doTestModuleForEjbAsWarLib(server);
    }

    @Test
    public void testModuleForEjbAsAppClientLib() throws Exception {
        doTestModuleForEjbAsAppClientLib(server);
    }

    @Test
    public void testModuleForAppClientAsEjbLib() throws Exception {
        doTestModuleForAppClientAsEjbLib(server);
    }

    @Test
    public void testModuleForAppClientAsWarLib() throws Exception {
        doTestModuleForAppClientAsWarLib(server);
    }

    @Test
    public void testModuleForCommonLib() throws Exception {
        doTestModuleForCommonLib(server);
    }

    @Test
    public void testModuleForPrivateLib() throws Exception {
        doTestModuleForPrivateLib(server);
    }

    @Test
    public void testModuleForOverrideLib() throws Exception {
        doTestModuleForOverrideLib(server);
    }

    @Test
    public void testModuleForRuntimeExtRegular() throws Exception {
        doTestModuleForRuntimeExtRegular(server);
    }

    @Test
    public void testModuleForRuntimeExtSeeApp() throws Exception {
        doTestModuleForRuntimeExtSeeApp(server);
    }

    @Test
    public void testModuleForStandaloneWar() throws Exception {
        doTestModuleForStandaloneWar(server);
    }

    @Test
    public void testModuleForStandaloneWarLib() throws Exception {
        doTestModuleForStandaloneWarLib(server);
    }

    @AfterClass
    public static void afterClass() throws Exception {

        //We put this in an AutoCloseable because try-with-resource will order the exceptions correctly.
        //That means an exception from stopServer will by the primary exception, and any errors from
        //uninstallSystemFeature will be recorded as suppressed exceptions.
        AutoCloseable uninstallFeatures = () -> {
            server.uninstallSystemFeature("visTest-1.2");
            server.uninstallSystemFeature("visTest-3.0");
        };

        try (AutoCloseable c = uninstallFeatures) {
            if (server != null) {
                server.stopServer();
            }
        }
    }
}
