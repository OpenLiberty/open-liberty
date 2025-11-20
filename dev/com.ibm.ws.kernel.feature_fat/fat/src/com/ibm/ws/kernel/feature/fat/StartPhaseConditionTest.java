/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class StartPhaseConditionTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.startphase.condition");

    @Test
    public void testStartPhaseCondition() throws Exception {

        server.startServer();
        server.stopServer(false);
        // The test has a different service component for each phase filter.
        // After starting and stopping the server we should see the following messages for PASSED
        String[] phases = { //
                            "SERVICE_EARLY", //
                            "SERVICE", //
                            "SERVICE_LATE", //
                            "CONTAINER_EARLY", //
                            "CONTAINER", //
                            "CONTAINER_LATE", //
                            "APPLICATION_EARLY", //
                            "APPLICATION", //
                            "APPLICATION_LATE", //
                            "ACTIVE" //
        };
        for (String phase : phases) {
            checkOnActivate(phase);
        }
        for (String phase : phases) {
            checkOnDeactivate(phase);
        }
    }

    private void checkOnActivate(String phase) {
        String found = server.waitForStringInLog("ON_ACTIVATE TESTING StartPhaseCondition: " + phase + " - ");
        assertNotNull("No message found activate for phase: " + phase, found);
        assertTrue("Test failed: " + found, found.contains("PASSED"));
    }

    private void checkOnDeactivate(String phase) {
        String found = server.waitForStringInLog("ON_DEACTIVATE TESTING StartPhaseCondition: " + phase + " - ");
        assertNotNull("No message found activate for phase: " + phase, found);
        assertTrue("Test failed: " + found, found.contains("PASSED"));
    }

    @BeforeClass
    public static void installFeatures() throws Exception {
        server.installSystemFeature("test.startphase.condition-1.0");
        server.installSystemBundle("test.startphase.condition");
    }

    @AfterClass
    public static void uninstallFeatures() throws Exception {
        server.uninstallSystemFeature("test.startphase.condition-1.0");
        server.uninstallSystemBundle("test.startphase.condition");
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
        server.postStopServerArchive();
    }

}
