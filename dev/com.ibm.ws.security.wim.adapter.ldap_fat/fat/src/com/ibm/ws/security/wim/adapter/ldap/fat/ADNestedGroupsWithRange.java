/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static componenttest.topology.utils.LDAPFatUtils.assertDNsEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/**
 * Tests OGLH10144
 * Use a test AD server with a user that is a memberof a group where the group is also a memberof > 1500 groups (use the director group type).
 */
@Ignore("Disabled because there is no FVT AD LDAP server with the correct configuration to test exceeding the query range.")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ADNestedGroupsWithRange {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.ldap.fat.ad.range");
    private static final Class<?> c = ADNestedGroupsWithRange.class;
    private static UserRegistryServletConnection servlet;

    private static ServerConfiguration serverConfiguration = null;

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
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

        Log.info(c, "setUp", "Creating servlet connection the server");
        servlet = new UserRegistryServletConnection(server.getHostname(), server.getHttpDefaultPort());

        if (servlet.getRealm() == null) {
            Thread.sleep(5000);
            servlet.getRealm();
        }

        /*
         * The original server configuration has no registry or Federated Repository configuration.
         */
        serverConfiguration = server.getServerConfiguration();
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
     * Hit the test servlet to see if getRealm works.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getRealm() throws Exception {
        Log.info(c, "getRealm", "Checking expected realm");
        assertEquals("SampleLdapADRealm", servlet.getRealm());
    }

    /**
     *
     * Assumes that rangeUser is a memberOf aGroup and that aGroup is a memberOf >1500 groups.
     */
    @Test
    public void getUserWithNestedGroupsExceedingADRange() throws Exception {
        String user = "rangeUser";
        String password = "password";
        Log.info(c, "checkPasswordWithGoodCredentials", "Checking good credentials");
        assertDNsEqual("Authentication should succeed.",
                       "CN=rangeUser rangeUser,OU=UserNestedGroups,DC=vmm,DC=com", servlet.checkPassword(user, password));

        Log.info(c, "getGroupsForUserWithValidUser", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertFalse("Should have retrieved groups for the user", list.isEmpty());

        String targetNestedGroup = "CN=purple50,OU=UserNestedGroups,DC=vmm,DC=com";
        Log.info(c, "checkPasswordWithGoodCredentials", "returned list " + list);
        assertTrue("Looking for nested group " + targetNestedGroup, list.contains(targetNestedGroup));

        targetNestedGroup = "CN=purple1550,OU=UserNestedGroups,DC=vmm,DC=com";
        assertTrue("Looking for nested group " + targetNestedGroup, list.contains(targetNestedGroup));

        assertTrue("Did not return enough nested groups to test that range query was exceeded. Expected > 1500, was " + list.size(), 1500 < list.size());
    }

}