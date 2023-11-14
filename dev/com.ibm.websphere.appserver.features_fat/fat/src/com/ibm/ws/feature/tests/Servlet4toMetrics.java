/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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
public class Servlet4toMetrics {

    public static final String SERVER_NAME = "Servlet4toMetrics";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @AfterClass
    public static void tearDown() throws Exception {
        if(server.isStarted()) {
            server.stopServer("CWWKF0001E");
        }
    }

    @Test
    public void servlet4MetricsTest() throws Exception {
        String methodName = "servlet4Test";

        //log out the method name "Entering methodName..."

        LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        server.startServer();

        assertNotNull(server.waitForStringInLog("mpMetrics-2.3"));

        server.stopServer("CWWKF0001E");
    }
}
