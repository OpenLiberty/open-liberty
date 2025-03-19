/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
public class ActiveConditionTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.active.condition");

    @Test
    public void testActiveCondition() throws Exception {

        server.startServer();
        String result = server.waitForStringInLogUsingMark("ACTIVE CONDITION - activate");
        assertNotNull("No ACTIVE CONDITION found", result);
        assertTrue("Active test failed: " + result, result.contains("passed"));

        server.stopServer(false);
        result = server.waitForStringInLogUsingMark("ACTIVE CONDITION - deactivate");
        assertNotNull("No ACTIVE CONDITION found", result);
        assertTrue("Deactivate test failed: " + result, result.contains("passed"));
    }

    @BeforeClass
    public static void installFeatures() throws Exception {
        server.installSystemFeature("test.active.condition-1.0");
        server.installSystemBundle("test.active.condition");
    }

    @AfterClass
    public static void uninstallFeatures() throws Exception {
        server.uninstallSystemFeature("test.active.condition-1.0");
        server.uninstallSystemBundle("test.active.condition");
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
        server.postStopServerArchive();
    }

}
