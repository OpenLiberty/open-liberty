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

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;


@MinimumJavaLevel(javaLevel = 11)
@RunWith(FATRunner.class)
public class Servlet6toMetrics {

    public static final String SERVER_NAME = "Servlet6toMetrics";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @After
    public void tearDown() throws Exception {
        server.stopServer("CWWKF0001E");
    }

    @Test
    public void servlet6MetricsMaxTest() throws Exception {
        String envVar = "mpMetrics-5.1,mpMetrics-5.0,mpMetrics-4.0,mpMetrics-3.0,mpMetrics-2.3,mpMetrics-2.2,mpMetrics-2.0,mpMetrics-1.1,mpMetrics-1.0";

        //log out the method name "Entering methodName..."

        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        server.addEnvVar("PREFERRED_FEATURE_VERSIONS", envVar);
        server.startServer();

        assertNotNull("Expected mpMetrics-5.1 after features resolved but got: " + server.findStringsInLogs("CWWKF0012I: The server installed the following features:"), server.waitForStringInLog("mpMetrics-5.1"));

        server.stopServer("CWWKF0001E");
    }

    @Test
    public void servlet6MetricsMinTest() throws Exception {
        String envVar = "mpMetrics-1.0,mpMetrics-1.1,mpMetrics-2.0,mpMetrics-2.2,mpMetrics-2.3,mpMetrics-3.0,mpMetrics-4.0,mpMetrics-5.0,mpMetrics-5.1";

        //log out the method name "Entering methodName..."

        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        server.addEnvVar("PREFERRED_FEATURE_VERSIONS", envVar);
        server.startServer();

        assertNotNull("Expected mpMetrics-5.0 after features resolved but got: " + server.findStringsInLogs("CWWKF0012I: The server installed the following features:"), server.waitForStringInLog("mpMetrics-5.0"));

        server.stopServer("CWWKF0001E");
    }
}
