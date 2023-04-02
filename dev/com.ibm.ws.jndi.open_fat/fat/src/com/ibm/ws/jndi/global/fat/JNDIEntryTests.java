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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

/**
 * Test to make sure that JNDIEntry registers the correct services
 */
@RunWith(FATRunner.class)
public class JNDIEntryTests extends FATServletClient {

    @Server("jndi_entry_fat")
    public static LibertyServer jndi_entry_fat_server;

    @Server("jndi_entry_decode")
    public static LibertyServer jndi_entry_decode_server;

    @Server("jndi_entry_id_update")
    public static LibertyServer jndi_entry_id_update_server;

    @Server("jndi_entry_dynamic_update")
    public static LibertyServer jndi_entry_dynamic_update_server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ShrinkHelper.exportDropinAppToServer(jndi_entry_fat_server, FATSuite.READ_JNDI_ENTRY_WAR);
        ShrinkHelper.exportDropinAppToServer(jndi_entry_decode_server, FATSuite.READ_JNDI_ENTRY_WAR);
        ShrinkHelper.exportDropinAppToServer(jndi_entry_id_update_server, FATSuite.READ_JNDI_ENTRY_WAR);
        ShrinkHelper.exportDropinAppToServer(jndi_entry_dynamic_update_server, FATSuite.READ_JNDI_ENTRY_WAR);
    }

    /**
     * This test makes sure that the JNDI Entry registers appropriate services for JNDI entries and also that they get unregistered.
     *
     * @throws Exception
     */
    @Test
    public void testJNDIEntry() throws Exception {
        // Grab the server
        jndi_entry_fat_server.startServer();
        try {
            /*
             * Wait for two debug level messages saying that the JNDI entries are registered.
             * Use a fairly short time out as we've already waited for the app to start
             * so this should already have appeared.
             */
            assertEquals("No debug message in the trace.log saying the two jndi entries defined in the server.xml were registered", 2,
                         jndi_entry_fat_server.waitForMultipleStringsInLog(2, ".*Registering JNDIEntry", 10000, jndi_entry_fat_server.getMatchingLogFile("trace.log")));

            // Now make sure that the OSGi services were registered using a test servlet
            HttpUtils.findStringInUrl(jndi_entry_fat_server, "/ReadJndiEntry/ReadJndiEntry",
                                      "JNDI Entry found for stringJndiEntry", "Value of JNDI Entry is: String Value",
                                      "JNDI Entry found for doubleJndiEntry", "Value of JNDI Entry is: 2.0");

            // Delete the entries and make sure they are unregistered
            ServerConfiguration serverConfig = jndi_entry_fat_server.getServerConfiguration();
            serverConfig.getJndiEntryElements().clear();
            jndi_entry_fat_server.updateServerConfiguration(serverConfig);

            // Wait for the update
            assertNotNull("The server configuration was not updated", jndi_entry_fat_server.waitForStringInLog("CWWKG0017I"));

            // Also wait for the debug trace messages saying that the services were unregistered
            assertEquals("No debug message in the trace.log saying the two jndi entries deleted from the server.xml were unregistered", 2,
                         jndi_entry_fat_server.waitForMultipleStringsInLog(2, ".*Unregistering JNDIEntry", 10000, jndi_entry_fat_server.getMatchingLogFile("trace.log")));

            // Now repeat the test with the servlet, it shouldn't find any entries anymore

            HttpUtils.findStringInUrl(jndi_entry_fat_server, "/ReadJndiEntry/ReadJndiEntry",
                                      "javax.naming.NameNotFoundException: stringJndiEntry",
                                      "javax.naming.NameNotFoundException: doubleJndiEntry");
        } finally {
            jndi_entry_fat_server.stopServer();
        }
    }

    @Test
    public void testJNDIDecode() throws Exception {
        // Grab the server
        jndi_entry_decode_server.startServer();
        try {
            /*
             * Wait for a debug level message saying that the JNDI entry is registered.
             * Use a fairly short time out as we've already waited for the app to start
             * so this should already have appeared.
             */
            assertEquals("No debug message in the trace.log saying  a jndi entry defined in the server.xml was registered", 1,
                         jndi_entry_decode_server.waitForMultipleStringsInLog(1, ".*Registering JNDIEntry", 10000, jndi_entry_decode_server.getMatchingLogFile("trace.log")));

            String decryptedStringValue = "foobar";
            String decryptedIntValue = "12345";
            // Check to make sure the decrypted JndiEntry value for "{xor}OTAwPT4t" is returned
            HttpUtils.findStringInUrl(jndi_entry_decode_server, "/ReadJndiEntry/ReadJndiEntry",
                                      "JNDI Entry found for stringJndiEntry", "Value of JNDI Entry is: " + decryptedStringValue);
            HttpUtils.findStringInUrl(jndi_entry_decode_server, "/ReadJndiEntry/ReadJndiEntry",
                                      "JNDI Entry found for stringJndiEntry", "Value of JNDI Entry is: " + decryptedIntValue);
        } finally {
            jndi_entry_decode_server.stopServer();
        }
    }

//    /**
//     * This test makes sure that changing the ID of the JNDI entry does not prevent it being registered correctly.
//     *
//     * @throws Exception
//     */
    @Test
    public void testJNDIEntryIDUpate() throws Exception {
        // Grab the server
        jndi_entry_id_update_server.startServer();
        try {
            /*
             * Wait for two debug level messages saying that the JNDI entries are registered.
             * Use a fairly short time out as we've already waited for the app to start
             * so this should already have appeared.
             */
            assertEquals("No debug message in the trace.log saying the two jndi entries defined in the server.xml were registered, found "
                         + jndi_entry_id_update_server.getServerConfiguration().getJndiEntryElements(), 2,
                         jndi_entry_id_update_server.waitForMultipleStringsInLog(2, ".*Registering JNDIEntry", 10000, jndi_entry_id_update_server.getMatchingLogFile("trace.log")));

            // Now make sure that the OSGi services were registered using a test servlet
            HttpUtils.findStringInUrl(jndi_entry_id_update_server, "/ReadJndiEntry/ReadJndiEntry",
                                      "JNDI Entry found for stringJndiEntry", "Value of JNDI Entry is: String Value",
                                      "JNDI Entry found for doubleJndiEntry", "Value of JNDI Entry is: 2.0");

            // Now switch the server configuration to check that we don't get a non-unique attribute value error
            jndi_entry_id_update_server.setServerConfigurationFile("JNDIEntryUpdate/IDUpdate.xml");
            ServerConfiguration config = jndi_entry_id_update_server.getServerConfiguration();
            jndi_entry_id_update_server.updateServerConfiguration(config);

            // Wait for the update
            assertNotNull("The server configuration was not updated", jndi_entry_id_update_server.waitForStringInLog("CWWKG0017I"));

            // Now make sure that the OSGi services are still correctly registered
            HttpUtils.findStringInUrl(jndi_entry_id_update_server, "/ReadJndiEntry/ReadJndiEntry",
                                      "JNDI Entry found for stringJndiEntry", "Value of JNDI Entry is: 2.0",
                                      "JNDI Entry found for doubleJndiEntry", "Value of JNDI Entry is: String Value");

        } finally {
            jndi_entry_id_update_server.stopServer();
        }
    }

    /**
     * This test is to check that creating a new JNDI entry gets registered correctly.
     *
     * @throws Exception
     */
    @Test
    public void testDynamicJNDIEntryUpdate() throws Exception {
        // Grab the server
        jndi_entry_dynamic_update_server.startServer();
        try {
            /*
             * Wait for two debug level messages saying that the JNDI entries are registered.
             * Use a fairly short time out as we've already waited for the app to start
             * so this should already have appeared.
             */
            assertEquals("No debug message in the trace.log saying the two jndi entries defined in the server.xml were registered, found "
                         + jndi_entry_dynamic_update_server.getServerConfiguration().getJndiEntryElements(), 1,
                         jndi_entry_dynamic_update_server.waitForMultipleStringsInLog(1, ".*Registering JNDIEntry", 10000,
                                                                                      jndi_entry_dynamic_update_server.getMatchingLogFile("trace.log")));

            // Now make sure that the OSGi services were registered using a test servlet
            HttpUtils.findStringInUrl(jndi_entry_dynamic_update_server, "/ReadJndiEntry/ReadJndiEntry",
                                      "javax.naming.NameNotFoundException: stringJndiEntry",
                                      "JNDI Entry found for doubleJndiEntry", "Value of JNDI Entry is: 2.0");

            // Now switch the server configuration to check that we don't get a non-unique attribute value error
            jndi_entry_dynamic_update_server.setServerConfigurationFile("JNDIEntryUpdate/uncommentedServer.xml");
            ServerConfiguration config = jndi_entry_dynamic_update_server.getServerConfiguration();
            jndi_entry_dynamic_update_server.updateServerConfiguration(config);

            // Wait for the update
            assertNotNull("The server configuration was not updated", jndi_entry_dynamic_update_server.waitForStringInLog("CWWKG0017I"));

            // Now make sure that the OSGi services are still correctly registered
            HttpUtils.findStringInUrl(jndi_entry_dynamic_update_server, "/ReadJndiEntry/ReadJndiEntry",
                                      "JNDI Entry found for stringJndiEntry", "Value of JNDI Entry is: 2.0",
                                      "JNDI Entry found for doubleJndiEntry", "Value of JNDI Entry is: String Value");

        } finally {
            jndi_entry_dynamic_update_server.stopServer();
        }
    }
}
