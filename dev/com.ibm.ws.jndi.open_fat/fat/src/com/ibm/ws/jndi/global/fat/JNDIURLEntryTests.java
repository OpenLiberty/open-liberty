/*
 * =============================================================================
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
 * =============================================================================
 */
package com.ibm.ws.jndi.global.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test to make sure that JNDIEntry registers the correct services
 */
@RunWith(FATRunner.class)
public class JNDIURLEntryTests {

    @Server("jndi_url_entry_fat")
    public static LibertyServer jndi_url_entry_fat_server;

    @Server("jndi_url_entry_unknown_fat")
    public static LibertyServer jndi_url_entry_unknown_fat_server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ShrinkHelper.exportDropinAppToServer(jndi_url_entry_fat_server, FATSuite.READ_JNDI_URL_ENTRY_WAR);
        ShrinkHelper.exportDropinAppToServer(jndi_url_entry_unknown_fat_server, FATSuite.READ_JNDI_URL_ENTRY_WAR);
    }

    /**
     * This test makes sure that the JNDI Entry registers appropriate services for JNDI entries and also that they get unregistered.
     *
     * @throws Exception
     */
    @Test
    public void testJNDIURLEntry() throws Exception {
        // Grab the server
        jndi_url_entry_fat_server.startServer();
        try {
            /*
             * Wait for two debug level messages saying that the JNDI entries are registered.
             * Use a fairly short time out as we've already waited for the app to start
             * so this should already have appeared.
             */
            assertNotNull("No debug message in the trace.log saying the single jndi url entry defined in the server.xml was registered",
                          jndi_url_entry_fat_server.waitForStringInLog(".*Registering JNDIURLEntry", 10000, jndi_url_entry_fat_server.getMatchingLogFile("trace.log")));

            // Now make sure that the OSGi services were registered using a test servlet
            HttpUtils.findStringInUrl(jndi_url_entry_fat_server, "/ReadJndiURLEntry/ReadJndiURLEntry?jndiName=stringJndiURLEntry",
                                      "JNDI URL Entry found for stringJndiURLEntry", "Value of JNDI URL Entry is: http://w3.ibm.com");

            // Delete the entries and make sure they are unregistered
            ServerConfiguration serverConfig = jndi_url_entry_fat_server.getServerConfiguration();
            serverConfig.getJndiURLEntryElements().clear();
            jndi_url_entry_fat_server.updateServerConfiguration(serverConfig);

            // Wait for the update
            assertNotNull("The server configuration was not updated", jndi_url_entry_fat_server.waitForStringInLog("CWWKG0017I"));

            // Also wait for the debug trace messages saying that the services were unregistered
            assertNotNull("No debug message in the trace.log saying the jndi url entry deleted from the server.xml was unregistered",
                          jndi_url_entry_fat_server.waitForStringInLog(".*Unregistering JNDIURLEntry", 10000, jndi_url_entry_fat_server.getMatchingLogFile("trace.log")));

            // Now repeat the test with the servlet, it shouldn't find any entries anymore

            HttpUtils.findStringInUrl(jndi_url_entry_fat_server, "/ReadJndiURLEntry/ReadJndiURLEntry?jndiName=stringJndiURLEntry",
                                      "javax.naming.NameNotFoundException: stringJndiURLEntry");
        } finally {
            jndi_url_entry_fat_server.stopServer();
        }
    }

    /**
     * This tests that we print a useful error message when a jndiURLEntry's url contains
     * a protocol that is unknown - i.e. there is no registered protocol handler.
     */
    @Test
    @ExpectedFFDC("java.net.MalformedURLException")
    public void testUnknownProtocol() throws Exception {
        // Grab the server
        jndi_url_entry_unknown_fat_server.startServer();
        try {
            /*
             * Wait for two debug level messages saying that the JNDI entries are registered.
             * Use a fairly short time out as we've already waited for the app to start
             * so this should already have appeared.
             */
            assertNotNull("No debug message in the trace.log saying the single jndi url entry defined in the server.xml was registered",
                          jndi_url_entry_unknown_fat_server.waitForStringInLog(".*Registering JNDIURLEntry", 10000,
                                                                               jndi_url_entry_unknown_fat_server.getMatchingLogFile("trace.log")));

            // Check that the error was logged:
            // (WI 234130) message matching must only use non-translated arguments, and in any order, because some translations reorder arguments.
            assertNotNull("Expected error message indicating failure to bind URL to JNDI was not found",
                          jndi_url_entry_unknown_fat_server.waitForStringInLog("CWWKN0010E(?=.*unknownProtocolJndiURLEntry)(?=.*java.net.MalformedURLException)", 10000));
        } finally {
            jndi_url_entry_unknown_fat_server.stopServer("CWWKN0010E");
        }
    }
}
