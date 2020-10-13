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

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTestAction;
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
    public static RepeatTests r = RepeatTests.withoutModification()
        .andWith(FATSuite.MP_REST_CLIENT("1.2", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT("1.3", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT("1.4", SERVER_NAME))
        .andWith(FATSuite.MP_REST_CLIENT("2.0", SERVER_NAME));

    private static final String appName = "asyncApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = AsyncTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, appName, "mpRestClient11.async");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        try {
            // check for error that occurs if cannot handle CompletionStage<?> generic type in JsonBProvider
            List<String> jsonbProviderErrors = server.findStringsInLogs("E Problem with reading the data");
            assertTrue("Found JsonBProvider errors in log file", 
                       jsonbProviderErrors == null || jsonbProviderErrors.isEmpty());
        } finally {
            server.stopServer("CWWKF0033E"); //ignore this error for mismatch with jsonb-1.0 and Java EE 7
        }
    }
}