/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class FATTest_SearchBase {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.searchbase");
    private static final Class<?> c = FATTest_SearchBase.class;
    private static UserRegistryServletConnection servlet;

    private static final String VALID_SERVER_XML = "dynamicUpdate/server_validSearchBase.xml";
    protected static String serverConfigurationFile = VALID_SERVER_XML;
    private static final String MULTIPLE_SEARCHBASE_SERVER_XML = "dynamicUpdate/server_multipleSearchBase.xml";
    private static final String INVALID_SERVER_XML = "dynamicUpdate/server_invalidSearchBase.xml";

    @BeforeClass
    public static void setUp() throws Exception {
        // Add LDAP variables to bootstrap properties file
        LDAPUtils.addLDAPVariables(server);
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
        server.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        server.addInstalledAppForValidation("userRegistry");
        server.startServer(c.getName() + ".log");

        //Make sure the application has come up before proceeding
        assertNotNull("Application userRegistry does not appear to have started.",
                      server.waitForStringInLog("CWWKZ0001I:.*userRegistry"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("Server did not came up",
                      server.waitForStringInLog("CWWKF0011I"));

        Log.info(c, "setUp", "Creating servlet connection the server");
        servlet = new UserRegistryServletConnection(server.getHostname(), server.getHttpDefaultPort());

        if (servlet.getRealm() == null) {
            Thread.sleep(5000);
            servlet.getRealm();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");

        try {
            server.stopServer();
        } finally {
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
        }
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and no limit; should only expect to find three entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsers() throws Exception {
        Assume.assumeTrue(LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = "*";
        setServerConfiguration(VALID_SERVER_XML);
        Log.info(c, "getUsers", "Checking with a valid pattern and without any limit.");
        SearchResult result = servlet.getUsers(user, 0);
        assertEquals("There should be three entries :" + result.toString(), 3, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and no limit; should only expect to find three entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroups() throws Exception {
        Assume.assumeTrue(LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String group = "*";
        setServerConfiguration(VALID_SERVER_XML);
        Log.info(c, "getUsers", "Checking with a valid pattern and without any limit.");
        SearchResult result = servlet.getGroups(group, 0);
        assertEquals("There should be three entries :" + result.toString(), 3, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers and getGroups works when passed in a valid user pattern
     * and no limit; should only expect to find two entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersandGroupswithInvalidSearchBase() throws Exception {
        Assume.assumeTrue(LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = "*";
        setServerConfiguration(INVALID_SERVER_XML);
        Log.info(c, "getUsers", "Checking with a valid pattern and without any limit.");
        SearchResult result = servlet.getUsers(user, 0);
        assertEquals("There should be zero entries :" + result.toString(), 0, result.getList().size());
        String group = "*";
        Log.info(c, "getGroups", "Checking with a valid pattern and without any limit.");
        result = servlet.getGroups(group, 0);
        assertEquals("There should be zero entries :" + result.toString(), 0, result.getList().size());
    }

    @Test
    public void getUsersandGroupswithMultipleSearchBase() throws Exception {
        Assume.assumeTrue(LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = "*";
        setServerConfiguration(MULTIPLE_SEARCHBASE_SERVER_XML);
        Log.info(c, "getUsers", "Checking with a valid pattern and without any limit.");
        SearchResult result = servlet.getUsers(user, 0);
        assertEquals("There should be four entries :" + result.toString(), 5, result.getList().size());
        String group = "*";
        Log.info(c, "getGroups", "Checking with a valid pattern and without any limit.");
        result = servlet.getGroups(group, 0);
        assertEquals("There should be four entries :" + result.toString(), 5, result.getList().size());
    }

    /**
     * This is an internal method used to set the server.xml
     */
    private static void setServerConfiguration(String serverXML) throws Exception {
        if (!serverConfigurationFile.equals(serverXML)) {
            // Update server.xml
            Log.info(c, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);

            // set a new mark
            server.setMarkToEndOfLog(server.getDefaultLogFile());
            server.setTraceMarkToEndOfDefaultTrace();

            // Update the server xml
            server.setServerConfigurationFile("/" + serverXML);

            // Wait for CWWKG0017I and CWWKF0008I to appear in logs after we started the config update
            Log.info(c, "setServerConfiguration",
                     "waitForStringInLogUsingMark: CWWKG0017I: The server configuration was successfully updated.");
            assertNotNull("Did not find CWWKG0017I message.", server.waitForStringInLogUsingMark("CWWKG0017I")); //CWWKG0017I: The server configuration was successfully updated in 0.2 seconds.

            // We really need to wait until VMMService.updatedConfiguredRepository() finishes so that base entries,
            // etc. are all updated.
            Log.info(c, "setServerConfiguration",
                     "waitForStringInTraceUsingMark: VMMService.*updatedConfiguredRepository Exit");
            assertNotNull("Did not find updatedConfiguredRepository exit statement.", server.waitForStringInTraceUsingMark("VMMService.*updatedConfiguredRepository Exit"));

            serverConfigurationFile = serverXML;
        }
    }

}
