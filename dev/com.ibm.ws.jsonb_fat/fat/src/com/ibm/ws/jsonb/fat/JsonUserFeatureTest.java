/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package com.ibm.ws.jsonb.fat;

import static com.ibm.ws.jsonb.fat.FATSuite.PROVIDER_YASSON;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test for JSON-P is placed in the JSON-B bucket because it is convenient to access the Johnzon library here.
 * Consider if we should move to the JSON-P bucket once that is written.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JsonUserFeatureTest extends FATServletClient {

    @Server("com.ibm.ws.jsonp.container.userfeature.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(JsonUserFeatureTest.class, "setUp", "=====> Start JsonUserFeatureTest");

        FATSuite.configureImpls(server);
        server.startServer();

        if (JakartaEEAction.isEE10OrLaterActive()) { //TODO possibly back port these info messages to EE9 and EE8
            assertTrue(!server.findStringsInLogsAndTrace("CWWKJ0350I").isEmpty());
            assertTrue(!server.findStringsInLogsAndTrace("CWWKJ0351I").isEmpty());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE0700W: .*ServiceThatRequiresJson"); // Sometimes DS reports this warning when it attempts to get the ref, but ultimately the svc component activates successfully

        Log.info(JsonUserFeatureTest.class, "tearDown", "<===== Stop JsonUserFeatureTest");
    }

    // Test a user feature with a service component that injects JsonProvider (from the bell)
    // as a declarative service. Validate the expected provider is used.
    @Test
    public void testJsonpFromUserFeature() throws Exception {
        String found;
        server.resetLogMarks();
        assertNotNull(found = server.waitForStringInLogUsingMark("TEST3: JsonProvider obtained from declarative services"));
        assertTrue(found, found.contains(FATSuite.getJsonpProviderClassName()));

        assertNotNull(found = server.waitForStringInLogUsingMark("TEST4"));
        assertTrue(found, found.contains("\"weight\""));
        assertTrue(found, found.contains("171"));
    }

    @Test
    public void testJsonbFromUserFeature() throws Exception {
        // Scrape messages.log to verify that 'ServiceThatRequiresJsonb' has activated
        // using Johnzon for jsonp and Yasson for jsonb
        String found;
        server.resetLogMarks();
        assertNotNull(found = server.waitForStringInLogUsingMark("TEST1: JsonbProvider obtained from declarative services"));
        assertTrue(found, found.contains(PROVIDER_YASSON));
        assertNotNull(found = server.waitForStringInLogUsingMark("TEST1.1: JsonProvider obtained from declarative services"));
        assertTrue(found, found.contains(FATSuite.getJsonpProviderClassName()));
        assertNotNull(found = server.waitForStringInLogUsingMark("TEST2"));
        assertTrue(found, found.contains("success"));
        assertTrue(found, found.contains("\"Rochester\""));
        assertTrue(found, found.contains("\"Minnesota\""));
        assertTrue(found, found.contains("55901"));
        assertTrue(found, found.contains("410"));
    }
}
