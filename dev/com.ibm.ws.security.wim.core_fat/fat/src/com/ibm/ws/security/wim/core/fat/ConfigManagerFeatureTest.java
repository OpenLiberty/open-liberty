/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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

package com.ibm.ws.security.wim.core.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test that we aren't printing out missing participatingBaseEntries messages at the incorrect times (CWIMK0004E)
 * when there are custom registries or repositories to load as features.
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class ConfigManagerFeatureTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.core.fat.mod");
    private static final Class<?> c = ConfigManagerFeatureTest.class;

    private static final String ADD_REG = "dynamicUpdate/userRegistry.xml";

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(c, "setUp", "Starting the server...");

        server.installUserBundle("com.ibm.ws.security.wim.repository_test.custom_1.0");
        server.installUserFeature("customRepositorySample-1.0");

        server.startServer(c.getName() + ".log");
        assertNotNull("Server did not came up",
                      server.waitForStringInLog("CWWKF0011I"));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");

        server.stopServer("CWWKE1102W"); //ignore server quiesce timeouts due to slow test machines

        server.uninstallUserBundle("com.ibm.ws.security.wim.repository_test.custom_1.0");
        server.uninstallUserFeature("customRepositorySample-1.0");

    }

    /**
     * This is an internal method used to set the server.xml
     */
    private static void setServerConfiguration(String serverXML) throws Exception {

        // Update server.xml
        Log.info(c, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);

        // set a new mark
        server.setMarkToEndOfLog(server.getDefaultLogFile());

        // Update the server xml
        server.setServerConfigurationFile("/" + serverXML);

        // Wait for CWWKG0017I and CWWKF0008I to appear in logs after we started the config update
        Log.info(c, "setServerConfiguration",
                 "waitForStringInLogUsingMark: CWWKG0017I: The server configuration was successfully updated.");
        server.waitForStringInLogUsingMark("CWWKG0017I"); //CWWKG0017I: The server configuration was successfully updated in 0.2 seconds.

    }

    /**
     * Add a custom registry and update the participating base entries, we should not print a CWIMK0004E message indicating we're missing
     * base entries.
     */
    @Test
    public void testParticipatingBaseEntriesVerification() throws Exception {
        Log.info(c, "testParticipatingBaseEntriesVerification", "Entering test testParticipatingBaseEntriesVerification");

        /*
         * Update to a config with Custom Repository and update the participatingBaseEntries to cause a modify to ConfigManager
         */
        Log.info(c, "testParticipatingBaseEntriesVerification", "Adding Custom User Repository with updated participatingBaseEntries");
        setServerConfiguration(ADD_REG);

        List<String> msg = server.findStringsInLogs("CWIMK0004E");
        assertTrue("Should not find CWIMK0004E in the logs: " + msg, msg.isEmpty());

    }

}