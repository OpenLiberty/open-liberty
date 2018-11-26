/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.AttributesCache;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapFilters;
import com.ibm.websphere.simplicity.config.wim.SearchResultsCache;
import com.ibm.websphere.simplicity.log.Log;
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
            server.stopServer("CWIML4529E");
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
        updateConfigDynamically(server, serverConfiguration);

        Log.info(c, "getRealm", "Checking expected realm");
        assertEquals("SampleLdapADRealm", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithGoodCredentials() throws Exception {
        // This test will only be executed when using physical LDAP server as these type of filters are not supported on ApacheDS
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        updateConfigDynamically(server, serverConfiguration);

        String user = "vmmtestuser";
        String password = "vmmtestuserpwd";
        Log.info(c, "checkPasswordWithGoodCredentials", "Checking good credentials");
        assertDNsEqual("Authentication should succeed.",
                       "CN=vmmtestuser,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com", servlet.checkPassword(user, password));

        passwordChecker.checkForPasswordInAnyFormat(password);
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
     * Hit the test servlet to see if isValidUser works with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidUserWithValidUser() throws Exception {
        // This test will only be executed when using physical LDAP server as these type of filters are not supported on ApacheDS
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        updateConfigDynamically(server, serverConfiguration);

        String user = "vmmtestuser";
        Log.info(c, "isValidUserWithValidUser", "Checking with a valid user");
        assertTrue("User validation should succeed.",
                   servlet.isValidUser(user));
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
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithValidPatternReturnsOnlyOneEntry() throws Exception {
        // This test will only be executed when using physical LDAP server as these type of filters are not supported on ApacheDS
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        updateConfigDynamically(server, serverConfiguration);

        String user = "vmmtestuser";
        Log.info(c, "getUsersWithValidPatternReturnsOnlyOneEntry", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should only be one entry", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when supplied with a wildcard for
     * the user pattern and a limit of 2; should expect to find 2 entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithWildcardPatternReturnsTwoEntries() throws Exception {
        // This test will only be executed when using physical LDAP server as these type of filters are not supported on ApacheDS
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        updateConfigDynamically(server, serverConfiguration);

        String user = "*";
        Log.info(c, "getUsersWithWildcardPatternReturnsTwoEntries", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should only be two entries", 2, result.getList().size());
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
     * Hit the test servlet to see if getUniqueUserId works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueUserIdWithValidUser() throws Exception {
        // This test will only be executed when using physical LDAP server as these type of filters are not supported on ApacheDS
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        updateConfigDynamically(server, serverConfiguration);

        String user = "vmmtestuser";
        String uniqueUserId = "CN=vmmtestuser,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";

        Log.info(c, "getUniqueUserIdWithValidUser", "Checking with a valid user.");
        assertDNsEqual("", uniqueUserId, servlet.getUniqueUserId(user));
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
     * Hit the test servlet to see if getUserSecurityName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityNameWithValidUser() throws Exception {
        // This test will only be executed when using physical LDAP server as these type of filters are not supported on ApacheDS
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        updateConfigDynamically(server, serverConfiguration);

        String uniqueUserId = "vmmtestuser";
        String userSecurityName = "CN=vmmtestuser,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";

        Log.info(c, "getUserSecurityNameWithValidUser", "Checking with a valid user.");
        assertEquals(userSecurityName, servlet.getUserSecurityName(uniqueUserId));
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
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUserWithValidUser() throws Exception {
        // This test will only be executed when using physical LDAP server as these type of filters are not supported on ApacheDS
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        updateConfigDynamically(server, serverConfiguration);

        String user = "vmmuser1";
        Log.info(c, "getGroupsForUserWithValidUser", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertTrue(!list.isEmpty());
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

    /**
     * Test Active Directory's LDAP_MATCHING_RULE_IN_CHAIN matching rule OID for memberof in the user filter.
     */
    @Test
    public void ldapMatchingRuleInChain_MemberOf() throws Exception {
        // This test will only be executed when using physical LDAP server as these type of filters are not supported on ApacheDS
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        Log.info(c, "ldapMatchingRuleInChain_MemberOf", "Checking memberof LDAP_MATCHING_RULE_IN_CHAIN rule OID.");

        /*
         * First test WITHOUT the matching rule in the filter. We expect to find all of the users.
         */
        ServerConfiguration clone = serverConfiguration.clone();
        clone.getLdapRegistries().get(0).setLdapCache(new LdapCache(new AttributesCache(false, null, null, null), new SearchResultsCache(false, null, null, null)));
        LdapFilters filters = clone.getActivedLdapFilterProperties().get(0);
        filters.setUserFilter("(&(sAMAccountName=%v)(objectclass=user))");
        updateConfigDynamically(server, clone);

        assertEquals("Expected to find user 'vmmuser1'.", 1, servlet.getUsers("vmmuser1", 0).getList().size());
        assertEquals("Expected to find user 'vmmuser2'.", 1, servlet.getUsers("vmmuser2", 0).getList().size());
        assertEquals("Expected to find user 'vmmuser3'.", 1, servlet.getUsers("vmmuser3", 0).getList().size());
        assertEquals("Expected to find user 'vmmuser4'.", 1, servlet.getUsers("vmmuser4", 0).getList().size());

        /*
         * Test the login path.
         */
        assertNotNull("Authentication should succeed.", servlet.checkPassword("vmmtestuser", "vmmtestuserpwd"));

        /*
         * Update the filter to include the memberof matching rule in the filter. We should find
         * 'vmmuser4' as that user is a direct member of 'vmmgroup4'. User 'vmmuser3' is a nested
         * group member since 'vmmgroup3' is a member of 'vmmgroup4' and 'vmmuser3' is a member of
         * 'vmmgroup3'.
         */
        filters.setUserFilter("(&(sAMAccountName=%v)(objectclass=user)(memberof:1.2.840.113556.1.4.1941:=CN=vmmgroup4,CN=Users,DC=secfvt2,DC=austin,DC=ibm,DC=com))");
        clone.getLdapRegistries().get(0).setCertificateFilter(""); // TODO Remove when https://github.com/OpenLiberty/open-liberty/issues/657 is complete
        updateConfigDynamically(server, clone);

        assertEquals("Expected to not find user 'vmmuser1'.", 0, servlet.getUsers("vmmuser1", 0).getList().size());
        assertEquals("Expected to not find user 'vmmuser2'.", 0, servlet.getUsers("vmmuser2", 0).getList().size());
        assertEquals("Expected to find user 'vmmuser3'.", 1, servlet.getUsers("vmmuser3", 0).getList().size());
        assertEquals("Expected to find user 'vmmuser4'.", 1, servlet.getUsers("vmmuser4", 0).getList().size());

        /*
         * Test the login path.
         */
        assertNotNull("Authentication should succeed.", servlet.checkPassword("vmmtestuser", "vmmtestuserpwd"));
    }

    @Test
    public void userAccountControlFlag() throws Exception {
        // This test will only be executed when using physical LDAP server as these type of filters are not supported on ApacheDS
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        Log.info(c, "userAccountControlFlag", "Checking memberof LDAP_MATCHING_RULE_IN_CHAIN rule OID.");

        /*
         * Test WITHOUT the userAccountControl attribute in the filter. The disabled 'guest'
         * account will be returned.
         */
        ServerConfiguration clone = serverConfiguration.clone();
        clone.getLdapRegistries().get(0).setLdapCache(new LdapCache(new AttributesCache(false, null, null, null), new SearchResultsCache(false, null, null, null)));
        LdapFilters filters = clone.getActivedLdapFilterProperties().get(0);
        filters.setUserFilter("(&(sAMAccountName=%v)(objectclass=user))");
        updateConfigDynamically(server, clone);

        assertEquals("Expected to find user 'guest'.", 1, servlet.getUsers("guest", 0).getList().size());

        /*
         * Test the login path.
         */
        assertNotNull("Authentication should succeed.", servlet.checkPassword("vmmtestuser", "vmmtestuserpwd"));

        /*
         * Test WITH the userAccountControl attribute in the filter. The disabled 'guest'
         * account will NOT be returned.
         */
        filters.setUserFilter("(&(sAMAccountName=%v)(objectclass=user)(!(userAccountControl:1.2.840.113556.1.4.803:=2)))");
        clone.getLdapRegistries().get(0).setCertificateFilter(""); // TODO Remove when https://github.com/OpenLiberty/open-liberty/issues/657 is complete
        updateConfigDynamically(server, clone);

        assertEquals("Expected to not find user 'guest'.", 0, servlet.getUsers("guest", 0).getList().size());

        /*
         * Test the login path.
         */
        assertNotNull("Authentication should succeed.", servlet.checkPassword("vmmtestuser", "vmmtestuserpwd"));
    }
}