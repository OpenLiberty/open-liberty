/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class RemoteServerInclude {
    private static LibertyServer serverIncludeHost = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.remote.include.host");
    private static LibertyServer serverIncludeClient = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.remote.include.client");

    @BeforeClass
    public static void startServer() throws Exception {
        serverIncludeHost.startServer();
        assertNotNull("war did not start", serverIncludeHost.waitForStringInLog("CWWKT0016I: Web application available"));
    }

    @Test
    public void testRemoteInclude() throws Exception {
        try {
            serverIncludeClient.startServer();
            assertNotNull("Expected feature \"osgiConsole-1.0\" not installed.",
                          serverIncludeClient.waitForStringInLog("CWWKF0012I: The server installed the following features: .*osgiConsole-1.0"));
        } finally {
            if (serverIncludeClient != null && serverIncludeClient.isStarted()) {
                serverIncludeClient.stopServer();
            }
        }
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (serverIncludeHost != null && serverIncludeHost.isStarted()) {
            serverIncludeHost.stopServer();
        }
    }

}