/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.feature.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.lang.reflect.Field;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class envVarVersionlessTest {

    public static final String SERVER_EE8 = "ee8toMP";
    public static final String SERVER_SERV4_HEALTH = "Servlet4toHealth";
    public static final String SERVER_SERV4_METRICS = "Servlet4toMetrics";

    @Test
    public void envVarEE8Test() throws Exception {
        String methodName = "envVarEE8Test";

        //log out the method name "Entering methodName..."

        LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_EE8);
        server.addEnvVar("PREFERRED_FEATURE_VERSIONS", "mpMetrics-2.3,mpHealth-2.2");
        server.startServer();

        assertNotNull(server.waitForStringInLog("mpMetrics-2.3"));
        assertNotNull(server.waitForStringInLog("mpHealth-2.2"));

        server.stopServer("CWWKF0001E");
    }

    @Test
    public void envVarServ4HealthTest() throws Exception {
        String methodName = "envVarServ4HealthTest";

        //log out the method name "Entering methodName..."

        LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_SERV4_HEALTH);
        server.addEnvVar("PREFERRED_FEATURE_VERSIONS", "mpMetrics-2.3,mpHealth-2.2");
        server.startServer();

        assertNotNull(server.waitForStringInLog("mpHealth-2.2"));

        server.stopServer("CWWKF0001E");
    }

    @Test
    public void envVarServ4MetricsTest() throws Exception {
        String methodName = "envVarServ4MetricsTest";

        //log out the method name "Entering methodName..."

        LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_SERV4_METRICS);
        server.addEnvVar("PREFERRED_FEATURE_VERSIONS", "mpMetrics-3.0,mpHealth-2.2");
        server.startServer();

        assertNotNull(server.waitForStringInLog("mpMetrics-3.0"));

        server.stopServer("CWWKF0001E");
    }
}
