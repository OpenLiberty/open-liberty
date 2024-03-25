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

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpRestClient11.async.AsyncTestServlet;

/**
 * This test should only be run with mpRestClient-1.1 and above.
 * Note that mpRestClient-1.1 can work with Java EE 7.0 (MP 1.4) and EE 8 (MP 2.0).
 */
@RunWith(FATRunner.class)
public class AsyncMethodTest extends FATServletClient {

    final static String SERVER_NAME = "mpRestClient11.async";

    @ClassRule
    public static RepeatTests r = FATSuite.repeatMP20Up(SERVER_NAME);

    private static final String appName = "asyncApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = AsyncTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, appName, new DeployOptions[] { DeployOptions.SERVER_ONLY }, "mpRestClient11.async");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        try {
            // check for error that occurs if cannot handle CompletionStage<?> generic type in JsonBProvider
            List<String> jsonbProviderErrors = server.findStringsInLogs("E Problem with reading the data.*CompletionStage");
            assertTrue("Found JsonBProvider errors in log file",
                       jsonbProviderErrors == null || jsonbProviderErrors.isEmpty());
        } finally {
            server.stopServer("CWWKE1102W",  //ignore server quiesce timeouts due to slow test machines
                              "CWWKF0033E"); //ignore this error for mismatch with jsonb-1.0 and Java EE 7
        }
    }
}