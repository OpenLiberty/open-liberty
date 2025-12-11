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
 *
 * For example, before the user registries are loaded.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class ConfigManagerInitModifyTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.core.fat.mod");
    private static final Class<?> c = ConfigManagerInitModifyTest.class;

    private static final String ADD_REG = "dynamicUpdate/basic_reg.xml";
    private static final String NO_REG = "dynamicUpdate/no_reg.xml";

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");

        server.startServer(c.getName() + ".log");
        assertNotNull("Server did not came up",
                      server.waitForStringInLog("CWWKF0011I"));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");

        server.stopServer("CWIMK0004E", "CWWKE1102W");
        //added "CWWKE1102W" to ignore server quiesce timeouts due to slow test machines

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
     * Add a registry and update the participating base entries, we should not print a CWIMK0004E message indicating we're missing
     * base entries. Then update the config again and remote the registry, we should print out a CWIMK0004E message.
     */
    @Test
    public void testParticipatingBaseEntriesVerification() throws Exception {
        Log.info(c, "testParticipatingBaseEntriesVerification", "Entering test testParticipatingBaseEntriesVerification");

        /*
         * Update to a config with a BasicRegistry and update the participatingBaseEntries to cause a modify to ConfigManager
         */
        Log.info(c, "testParticipatingBaseEntriesVerification", "Adding BasicRegistry with updated participatingBaseEntries");
        setServerConfiguration(ADD_REG);

        assertTrue("Should not find CWIMK0004E in the logs", server.findStringsInLogs("CWIMK0004E").isEmpty());

        /*
         * Update to a config and remove the BasicRegistry, intentionally causing missing baseEntries
         */
        Log.info(c, "testParticipatingBaseEntriesVerification", "Update to only a FedRepo config, missing the matching participatingBaseEntries");
        setServerConfiguration(NO_REG);

        assertNotNull("Should find CWIMK0004E in the logs.", server.waitForStringInLog("CWIMK0004E"));

    }

}