/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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
import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapFilters;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;
import com.ibm.ws.apacheds.PopulateDefaultLdapConfig;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;
import componenttest.vulnerability.LeakedPasswordChecker;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class FATTestAD {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.ldap.fat.ad");
    private static final Class<?> c = FATTestAD.class;
    private static UserRegistryServletConnection servlet;
    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);

    private static ServerConfiguration serverConfiguration = null;

    private static EmbeddedApacheDS ldapServer = null;

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

        serverConfiguration = server.getServerConfiguration();
        ldapServer = PopulateDefaultLdapConfig.setupLdapServerAD(ldapServer, c.getSimpleName());
        assertNotNull("Failed to setup EmbeddedApacheDS server", ldapServer);
        updateLibertyServer();

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

    /**
     * Update the Liberty server with the correct LDAP info for an embedded ApacheDS
     *
     * @throws Exception If the update failed for some reason.
     */
    private static void updateLibertyServer() throws Exception {
        final String methodName = "updateLibertyServer";
        Log.info(c, methodName, "Starting Liberty server update to embedded ApacheDS");

        ServerConfiguration serverConfig = serverConfiguration.clone();

        boolean foundLdapToUpdate = false;

        String ldapID = "LDAP";

        for (LdapRegistry ldap : serverConfig.getLdapRegistries()) {
            if (ldap.getId().equals(ldapID)) { // from "com.ibm.ws.security.registry.ldap.fat.ad"
                ldap.setLdapType("Custom");
                ldap.setHost("localhost");
                ldap.setPort(String.valueOf(ldapServer.getLdapServer().getPort()));
                ldap.setBindDN(EmbeddedApacheDS.getBindDN());
                ldap.setBindPassword(EmbeddedApacheDS.getBindPassword());

                LdapFilters filter = serverConfig.getActivedLdapFilterProperties().get(0);

                assertNotNull("Should have a filter to convert", filter);

                ldap.setCustomFilters(new LdapFilters(filter.getUserFilter(), filter.getGroupFilter(), filter.getUserIdMap(), filter.getGroupIdMap(), filter
                                .getGroupMemberIdMap()));

                serverConfig.getActivedLdapFilterProperties().clear();

                foundLdapToUpdate = true;
            }
        }

        assertTrue("Did not find an LDAP id to match " + ldapID, foundLdapToUpdate);

        updateConfigDynamically(server, serverConfig);

        Log.info(c, methodName, "Finished Liberty server update");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");

        try {
            server.stopServer("CWIML4529E");
        } finally {

            if (ldapServer != null) {
                ldapServer.stopService();
            }
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");

        }

    }

    /**
     * Hit the test servlet to see if getRealm works.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getRealm() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        Log.info(c, "getRealm", "Checking expected realm");
        assertEquals("SampleLdapADRealm", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithBadCredentials() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String user = "vmmtestuser";
        String password = "badPassword";
        Log.info(c, "checkPasswordWithBadCredentials", "Checking bad credentials");
        assertNull("Authentication should not succeed.", servlet.checkPassword(user, password));
        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Hit the test servlet to see if isValidUser works with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidUserWithInvalidUser() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String user = "invalidUser";
        Log.info(c, "isValidUserWithInvalidUser", "Checking with an invalid user");
        assertFalse("User validation should fail.",
                    servlet.isValidUser(user));
    }

    /**
     * Hit the test servlet to see if getUsers works when supplied with an invalid pattern for
     * the user pattern and a limit of 2; should expect to find no entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithInvalidPatternReturnsNoEntries() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String user = "invalidUser";
        Log.info(c, "getUsersWithInvalidPatternReturnsNoEntries", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should be no entries", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when supplied with an invalid pattern for
     * the user pattern and a limit of 2; should expect to find no entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithValidPatternLimitLessThanZero() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String user = "vmmtestuser";
        Log.info(c, "getUsersWithValidPatternLimitLessThanZero", "Checking with a valid pattern and limit of -1.");
        SearchResult result = servlet.getUsers(user, -1);
        assertEquals("There should be no entries", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserDisplayNameWithValidUser() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String user = "CN=vmmtestuser,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
        String displayName = "vmmtestuser";
        Log.info(c, "getUserDisplayNameWithValidUser", "Checking with a valid user.");
        assertEquals(displayName, servlet.getUserDisplayName(user));
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserDisplayNameWithInvalidUser() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String user = "invalidUser";
        Log.info(c, "getUserDisplayNameWithInvalidUser", "Checking with an invalid user.");
        servlet.getUserDisplayName(user);
    }

    /**
     * Hit the test servlet to see if getUniqueUserId works when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueUserIdWithInvalidUser() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String user = "invalidUser";
        Log.info(c, "getUniqueUserIdWithInvalidUser", "Checking with an invalid user.");
        servlet.getUniqueUserId(user);
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserSecurityNameWithInvalidUser() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String user = "invalidUser";
        Log.info(c, "getUserSecurityNameWithInvalidUser", "Checking with an invalid user.");
        servlet.getUserSecurityName(user);
    }

    /**
     * Hit the test servlet to see if isValidGroup works with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroupWithValidGroup() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String group = "TelnetClients";
        Log.info(c, "isValidGroupWithValidGroup", "Checking with a valid group");
        assertTrue("Group validation should succeed.",
                   servlet.isValidGroup(group));
    }

    /**
     * Hit the test servlet to see if isValidGroup works with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroupWithInvalidGroup() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String group = "invalidGroup";
        Log.info(c, "isValidGroupWithInvalidGroup", "Checking with an invalid group");
        assertFalse("Group validation should fail.",
                    servlet.isValidGroup(group));
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithValidPatternReturnsOnlyOneEntry() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String group = "TelnetClients";
        Log.info(c, "getGroupsWithValidPatternReturnsOnlyOneEntry", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("There should only be one entry", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when supplied with a wildcard for
     * the group pattern and a limit of 2; should expect to find 2 entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithWildcardPatternReturnsTwoEntries() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String group = "*";
        Log.info(c, "getGroupsWithWildcardPatternReturnsTwoEntries", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("There should only be two entries", 2, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when supplied with an invalid pattern for
     * the group pattern and a limit of 2; should expect to find no entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithInvalidPatternReturnsNoEntries() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String group = "invalidGroup";
        Log.info(c, "getGroupsWithInvalidPatternReturnsNoEntries", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("There should be no entries", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when supplied with an invalid pattern for
     * the group pattern and a limit of 2; should expect to find no entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithValidPatternLimitLessThanZero() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String group = "TelnetClients";
        Log.info(c, "getGroupsWithValidPatternLimitLessThanZero", "Checking with a valid pattern and limit of -1.");
        SearchResult result = servlet.getGroups(group, -1);
        assertEquals("There should be no entries", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameWithValidGroup() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String group = "CN=TelnetClients,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
        Log.info(c, "getGroupDisplayNameWithValidUser", "Checking with a valid group.");
        assertEquals("TelnetClients", servlet.getGroupDisplayName(group));
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupDisplayNameWithInvalidGroup() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String group = "invalidGroup";
        Log.info(c, "getGroupDisplayNameWithInvalidGroup", "Checking with an invalid group.");
        servlet.getGroupDisplayName(group);
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdWithValidGroup() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String group = "TelnetClients";
        String uniqueGroupId = "CN=TelnetClients,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";

        Log.info(c, "getUniqueGroupIdWithValidGroup", "Checking with a valid group.");
        assertDNsEqual(null, uniqueGroupId, servlet.getUniqueGroupId(group));
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupIdWithInvalidGroup() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String group = "invalidGroup";
        Log.info(c, "getUniqueGroupIdWithInvalidGroup", "Checking with an invalid group.");
        servlet.getUniqueGroupId(group);
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithValidGroup() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String group = "TelnetClients";
        String uniqueGroupId = "CN=TelnetClients,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";

        Log.info(c, "getGroupSecurityNameWithValidGroup", "Checking with a valid group.");
        assertDNsEqual(null, uniqueGroupId, servlet.getGroupSecurityName(group));
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupSecurityNameWithInvalidGroup() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String group = "invalidGroup";
        Log.info(c, "getGroupSecurityNameWithInvalidGroup", "Checking with an invalid group.");
        servlet.getGroupSecurityName(group);
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a invalid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupsForUserWithInvalidUser() throws Exception {
        updateConfigDynamically(server, serverConfiguration);

        String user = "invalidUser";
        Log.info(c, "getGroupsForUserWithInvalidUser", "Checking with an invalid user.");
        servlet.getGroupsForUser(user);
    }

}