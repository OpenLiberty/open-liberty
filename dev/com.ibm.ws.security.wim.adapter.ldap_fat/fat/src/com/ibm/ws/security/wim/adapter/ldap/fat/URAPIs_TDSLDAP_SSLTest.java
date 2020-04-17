/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryTDSLDAPServer;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class URAPIs_TDSLDAP_SSLTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.tds.ssl");
    private static final Class<?> c = URAPIs_TDSLDAP_SSLTest.class;
    private static UserRegistryServletConnection servlet;

    /** Test rule for testing for expected exceptions. */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private static InMemoryTDSLDAPServer ldapServer;

    /**
     * Setup the test case.
     *
     * @throws Exception If the setup failed for some reason.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        setupLdapServer();
        setupLibertyServer();
    }

    /**
     * Tear down the test.
     */
    @AfterClass
    public static void teardownClass() throws Exception {
        try {
            if (server != null) {
                server.stopServer("CWIML4529E", "CWIML4537E", "CWPKI0041W");
            }
        } finally {
            try {
                if (ldapServer != null) {
                    ldapServer.shutDown(true);
                }
            } catch (Exception e) {
                Log.error(c, "teardown", e, "LDAP server threw error while shutting down. " + e.getMessage());
            }
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
        }
    }

    /**
     * Setup the Liberty server. This server will start with very basic configuration. The tests
     * will configure the server dynamically.
     *
     * @throws Exception If there was an issue setting up the Liberty server.
     */
    public static void setupLibertyServer() throws Exception {
        /*
         * Add LDAP variables to bootstrap properties file
         */
        LDAPUtils.addLDAPVariables(server);
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
        server.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        server.addInstalledAppForValidation("userRegistry");

        /*
         * Update LDAP configuration with In-Memory Server
         */
        ServerConfiguration serverConfig = server.getServerConfiguration();
        LdapRegistry ldap = serverConfig.getLdapRegistries().get(0);
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ldapServer.getLdapsPort()));
        ldap.setBindDN(InMemoryTDSLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryTDSLDAPServer.getBindPassword());
        server.updateServerConfiguration(serverConfig);
        /*
         * Make sure the application has come up before proceeding
         */
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

    /**
     * Configure the embedded LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLdapServer() throws Exception {
        ldapServer = new InMemoryTDSLDAPServer();
    }

    /**
     * Hit the test servlet to see if getUsersForGroup works when supplied with a valid group.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForGroupWithValidGroup() throws Exception {
        String group = "vmmgroup1";
        Log.info(c, "getUsersForGroupWithValidGroup", "Checking with a valid group.");
        SearchResult result = servlet.getUsersForGroup(group, 0);
        List<String> list = result.getList();
        assertTrue(list.contains("vmmuser1"));
    }

    /**
     * Hit the test servlet to see if getUsersForGroup works when supplied with a valid group.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForGroupWithValidGroupWithLimit() throws Exception {
        String group = "vmmgrp1";
        Log.info(c, "getUsersForGroupWithValidGroupWithLimit", "Checking with a valid group.");
        SearchResult result = servlet.getUsersForGroup(group, 2);
        List<String> list = result.getList();
        assertEquals("There should be two entries", 2, list.size());
    }

    /**
     * Hit the test servlet to see if getUsersForGroup works when supplied with a valid group.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForGroupWithLimitWithDyanmicGroup() throws Exception {
        String group = "dynamicGroup1";
        Log.info(c, "getUsersForGroupWithLimitWithDyanmicGroup", "Checking with a valid group.");
        SearchResult result = servlet.getUsersForGroup(group, 2);
        List<String> list = result.getList();
        assertEquals("There should be two entries", 2, list.size());
    }

    /**
     * Hit the test servlet to see if getUsersForGroup works when supplied with a valid group.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForGroupWithLimitWithNestedGroup1() throws Exception {
        String group = "vmm_nestedGrp";
        Log.info(c, "getUsersForGroupWithLimitWithNestedGroup1", "Checking with a valid group.");
        SearchResult result = servlet.getUsersForGroup(group, 10);
        List<String> list = result.getList();
        assertTrue(list.contains("vmmuser1"));
        assertTrue(list.contains("vmmuser2"));
        assertTrue(list.contains("vmmuser3"));
    }

    /**
     * Hit the test servlet to see if getUsersForGroup works when supplied with a valid group.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForGroupWithLimitWithNestedGroup2() throws Exception {
        // This test will only be executed when using physical LDAP server
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String group = "nested_g1";
        Log.info(c, "getUsersForGroupWithLimitWithNestedGroup2", "Checking with a valid group.");
        SearchResult result = servlet.getUsersForGroup(group, 10);
        List<String> list = result.getList();
        assertTrue(list.contains("topng_user1"));
        assertTrue(list.contains("ng_user1"));
    }

    /**
     * Hit the test servlet to see if getUsersForGroup works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForGroupWithInvalidGroup() throws Exception {
        String group = "invalidGroup";
        Log.info(c, "getUsersForGroupWithInvalidGroup", "Checking with an invalid group.");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML4001E");

        servlet.getUsersForGroup(group, 0);
    }

    /**
     * Hit the test servlet to see if getRealm works.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getRealm() throws Exception {
        Log.info(c, "getRealm", "Checking expected realm");
        assertEquals("TDSSSLRealm", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPassword() throws Exception {
        String user = "vmmtestuser";
        String password = "vmmtestuserpwd";
        Log.info(c, "checkPassword", "Checking good credentials");
        assertEquals("Authentication should succeed.",
                     "vmmtestuser", servlet.checkPassword(user, password));
        //passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Hit the test servlet to see if checkPassword fails when passed in a invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithInvalidUser() throws Exception {
        String user = "invalid";
        String password = "testuserpwd";
        Log.info(c, "checkPasswordWithInvalidUser", "Checking good credentials");

        assertNull("Authentication should fail.", servlet.checkPassword(user, password));
        assertNotNull("An invalid user should cause RegistryException with No principal is found message", server.waitForStringInLog("CWIML4537E"));
        //passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithBadCredentials() throws Exception {
        String user = "vmmtestuser";
        String password = "badPassword";
        Log.info(c, "checkPasswordWithBadCredentials", "Checking bad credentials");
        assertNull("Authentication should not succeed.",
                   servlet.checkPassword(user, password));
        assertNotNull("Expected CWIML4529E message.", server.waitForStringInLog("CWIML4529E"));
        //passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Hit the test servlet to see if isValidUser works with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidUser() throws Exception {
        String user = "vmmtestuser";
        Log.info(c, "isValidUser", "Checking with a valid user");
        assertTrue("User validation should succeed.",
                   servlet.isValidUser(user));
    }

    /**
     * Hit the test servlet to see if isValidUser works with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidUserWithInvalidUser() throws Exception {
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
    public void getUsers() throws Exception {
        String user = "vmmtestuser";
        Log.info(c, "getUsers", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should only be one entry", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 5; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithAsteriskWildcard() throws Exception {
        String user = "vmmtes*";
        Log.info(c, "getUsersWithAsteriskWildcard", "Checking with a valid pattern and limit of 5.");
        SearchResult result = servlet.getUsers(user, 5);
        assertEquals("There should only be 5 entries", 5, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithAsteriskWildcard1() throws Exception {
        String user = "*";
        Log.info(c, "getUsersWithAsteriskWildcard1", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should only be two entries", 2, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithWildcard() throws Exception {
        // This test will only be executed when using physical LDAP server
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = "vmmtest*use*$";
        Log.info(c, "getUsers", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should only be one entry", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a invalid user pattern
     * and a limit of 2; should only expect to find no entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithInvalidUser() throws Exception {
        String user = "invalid";
        Log.info(c, "getUsersWithInvalidUser", "Checking with a invalid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should only be no entries", 0, result.getList().size());
    }

    /*
     * @Test
     *
     * @ExpectedFFDC(value = { "com.ibm.ws.security.registry.RegistryException", "com.ibm.websphere.wim.exception.SearchControlException" })
     * public void getUsersWithLimitLessThanZero() {
     * String user = "*";
     * Log.info(c, "getUsersWithLimitLessThanZero", "Checking with a valid pattern and limit of -1.");
     * try {
     * servlet.getUsers(user, -1);
     * } catch (RegistryException e) {
     * // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
     * e.printStackTrace();
     * }
     * server.waitForStringInLog("CWIML1022E"); //CWIML1022E The '-1' count limit specified in the SearchControl data object is invalid.
     * assertTrue("An invalid limit should cause RegistryException in SearchControl", true);
     * }
     */

    /**
     * Hit the test servlet to see if getUsers works when supplied with an invalid pattern for
     * the user pattern and a limit of 2; should expect to find no entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithValidPatternLimitLessThanZero() throws Exception {
        String user = "*";
        Log.info(c, "getUsersWithValidPatternLimitLessThanZero", "Checking with a valid pattern and limit of -1.");
        SearchResult result = servlet.getUsers(user, -1);
        assertEquals("There should be no entries", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserDisplayName() throws Exception {
        String user = "vmmtestuser";
        String displayName = "vmmtestuser";
        Log.info(c, "getUserDisplayName", "Checking with a valid user.");
        assertEquals(displayName, servlet.getUserDisplayName(user));
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works(Throws exception) when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserDisplayNameWithInvalidUser() throws Exception {
        String user = "invalidUser";
        Log.info(c, "getUserDisplayNameWithInvalidUser", "Checking with an invalid user.");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML4001E");

        servlet.getUserDisplayName(user);
    }

    /**
     * Hit the test servlet to see if getUniqueUserId works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueUserId() throws Exception {
        String user = "vmmtestuser";
        String uniqueUserId = "cn=vmmtestuser,o=ibm,c=us";
        Log.info(c, "getUniqueUserId", "Checking with a valid user.");
        //assertEquals("", uniqueUserId, servlet.getUniqueUserId(user));
        assertDNsEqual("Unique names should be equal ", uniqueUserId, servlet.getUniqueUserId(user));
    }

    /**
     * Hit the test servlet to see if getUniqueUserId works when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueUserIdWithInvalidUser() throws Exception {
        String user = "invalidUser";
        Log.info(c, "getUniqueUserIdWithInvalidUser", "Checking with an invalid user.");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML4001E");

        servlet.getUniqueUserId(user);
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityName() throws Exception {
        String user = "vmmtestuser";
        String securityName = "vmmtestuser";
        Log.info(c, "getUserSecurityName", "Checking with a valid user.");
        assertEquals(securityName, servlet.getUserSecurityName(user));
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityNameWithInvalidUser() throws Exception {
        String user = "cn=invalid,o=ibm,c=us";
        Log.info(c, "getUserSecurityNameWithInvalidUser", "Checking with an invalid user.");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML4527E");

        servlet.getUserSecurityName(user);
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with an invalid user (EntityOutOfRealmScope)
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityNameWithEntityOutOfRealmScope() throws Exception {
        String user = "uid=invalid";
        Log.info(c, "getUserSecurityNameWithEntityOutOfRealmScope", "Checking with an invalid user.");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML0515E");

        servlet.getUserSecurityName(user);
    }

    /**
     * Hit the test servlet to see if isValidGroup works with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroup() throws Exception {
        String group = "vmmgrp1";
        Log.info(c, "isValidGroup", "Checking with a valid group");
        assertTrue("Group validation should succeed.",
                   servlet.isValidGroup(group));
    }

    /**
     * Hit the test servlet to see if isValidGroup works with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroupWithInvalidGroup() throws Exception {
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
    public void getGroups() throws Exception {
        String group = "vmmgrp1";
        Log.info(c, "getGroups", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("There should only be one entry", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should only expect to find more entries but as limit 2, we should only get 2 groups
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithAsteriskWildcard() throws Exception {
        String group = "*";
        Log.info(c, "getGroupsWithAsteriskWildcard", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("There should only be 2 entries", 2, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should only expect to find more entries but as limit 2, we should only get 2 groups
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithAsteriskWildcard1() throws Exception {
        String group = "vmmgrp*";
        Log.info(c, "getGroupsWithAsteriskWildcard1", "Checking with a valid pattern and limit of 5.");
        SearchResult result = servlet.getGroups(group, 5);
        assertEquals("There should only be 4 entries", 4, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; we should only get 7 groups
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithAsteriskWildcard2() throws Exception {
        String group = "vmmg*p*";
        Log.info(c, "getGroupsWithAsteriskWildcard1", "Checking with a valid pattern and limit of 11.");
        SearchResult result = servlet.getGroups(group, 11);
        assertEquals("There should only be 11 entries", 11, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a invalid group pattern
     * and a limit of 2; should only expect to find no entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithInvalidGroup() throws Exception {
        String group = "invalidgroup";
        Log.info(c, "getGroupsWithInvalidGroup", "Checking with a invalid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("There should only be no entry", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a invalid group pattern
     * and a limit of 2; should only expect to find no entry
     * This verifies the various required bundles got installed and are working.
     */
    /*
     * @Test
     *
     * @ExpectedFFDC(value = { "com.ibm.ws.security.registry.RegistryException", "com.ibm.websphere.wim.exception.SearchControlException" })
     * public void getGroupsWithInvalidLimit() {
     * String group = "grp1";
     * Log.info(c, "getGroupsWithInvalidLimit", "Checking with a valid pattern and limit of -1.");
     * try {
     * servlet.getGroups(group, -1);
     * } catch (RegistryException e) {
     * // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
     * e.printStackTrace();
     * }
     * server.waitForStringInLog("CWIML1022E"); //CWIML1022E The '-1' count limit specified in the SearchControl data object is invalid.
     * assertTrue("An invalid limit should cause RegistryException in SearchControl", true);
     * }
     */

    /**
     * Hit the test servlet to see if getGroups works when supplied with an invalid pattern for
     * the group pattern and a limit of 2; should expect to find no entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithValidPatternLimitLessThanZero() throws Exception {
        String group = "grp1";
        Log.info(c, "getGroupsWithValidPatternLimitLessThanZero", "Checking with a valid pattern and limit of -1.");
        SearchResult result = servlet.getGroups(group, -1);
        assertEquals("There should be no entries", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayName() throws Exception {
        String group = "vmmgrp1";
        Log.info(c, "getGroupDisplayName", "Checking with a valid group.");
        assertEquals("vmmgrp1", servlet.getGroupDisplayName(group));
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameWithInvalidGroup() throws Exception {
        String group = "cn=invalidgroup,o=ibm,c=us";
        Log.info(c, "getGroupDisplayNameWithInvalidGroup", "Checking with an invalid group.");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML4527E");

        servlet.getGroupDisplayName(group);
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameWithEntityOutOfRealmScope() throws Exception {
        String group = "cn=invalidgroup";
        Log.info(c, "getGroupDisplayNameWithEntityOutOfRealmScope", "Checking with an invalid group.");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML0515E");

        servlet.getGroupDisplayName(group);
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupId() throws Exception {
        String group = "vmmgrp1";
        String uniqueGroupId = "cn=vmmgrp1,o=ibm,c=us";
        Log.info(c, "getUniqueGroupId", "Checking with a valid group.");
        assertDNsEqual("UniqueGroupId is incorrect", uniqueGroupId, servlet.getUniqueGroupId(group));
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdWithInvalidGroup() throws Exception {
        String group = "invalidGroup";
        Log.info(c, "getUniqueGroupIdWithInvalidGroup", "Checking with an invalid group.");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML4001E");

        servlet.getUniqueGroupId(group);
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityName() throws Exception {
        String uniqueGroupId = "cn=vmmgrp1,o=ibm,c=us";
        Log.info(c, "getGroupSecurityName", "Checking with a valid group.");
        assertEquals("vmmgrp1", servlet.getGroupSecurityName(uniqueGroupId));
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithInvalidGroup() throws Exception {
        String group = "cn=invalid,o=ibm,c=us";
        Log.info(c, "getGroupSecurityNameWithInvalidGroup", "Checking with an invalid group.");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML4527E");

        servlet.getGroupSecurityName(group);
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group (InvalidUniqueName)
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithInvalidUniqueName() throws Exception {
        String group = "invalid";
        Log.info(c, "getGroupSecurityNameWithUniqueName", "Checking with an invalid group.");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML4001E");

        servlet.getGroupSecurityName(group);
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group (EntityOutOfRealmScope)
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithEntityOutOfRealmScope() throws Exception {
        String group = "uid=invalid";
        Log.info(c, "getGroupSecurityNameWithUniqueName", "Checking with an invalid group.");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML0515E");

        servlet.getGroupSecurityName(group);
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUser() throws Exception {
        String user = "vmmuser1";
        Log.info(c, "getGroupsForUser", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertTrue(list.contains("vmmgroup1"));
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForDynamicMember() throws Exception {
        String user = "vmmLibertyUserUID";
        Log.info(c, "getGroupsForDynamicMember", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertTrue(list.contains("dynamicGroup1"));
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user in a nested group.
     * This test expects that both the top level and nested group will be returned.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUserForNestedGroup() throws Exception {
        // This test will only be executed when using physical LDAP server
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = "ng_user1";
        Log.info(c, "getGroupsForUserForNestedGroup", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertTrue(list.contains("nested_g1") && list.contains("embedded_group1"));
        assertEquals("There should be two entries", 2, list.size());
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a invalid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUserWithInvalidUser() throws Exception {
        String user = "invalidUser";
        Log.info(c, "getGroupsForUserWithInvalidUser", "Checking with an invalid user.");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML4001E");

        servlet.getGroupsForUser(user);
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIds() throws Exception {
        String user = "cn=vmmuser1,o=ibm,c=us";
        Log.info(c, "getUniqueGroupIds", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertTrue(list.contains("cn=vmmgroup1,o=ibm,c=us"));
        assertEquals("There should only be 6 entries", 6, list.size());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsForDynamicMember() throws Exception {
        String user = "cn=vmmLibertyUser,o=ibm,c=us";
        Log.info(c, "getUniqueGroupIdsForDynamicMember", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertTrue(list.contains("cn=dynamicGroup1,o=ibm,c=us"));
        assertEquals("There should only be one entry", 1, list.size());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user in a nested group.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsForUserForNestedGroup() throws Exception {
        // This test will only be executed when using physical LDAP server
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = "cn=ng_user1,o=ibm,c=us";
        Log.info(c, "getUniqueGroupIdsForUserForNestedGroup", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertTrue(list.contains("cn=nested_g1,o=ibm,c=us") && list.contains("cn=embedded_group1,o=ibm,c=us"));
        assertEquals("There should be two entries", 2, list.size());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a invalid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsWithInvalidUser() throws Exception {
        String user = "uid=invalid,o=ibm,c=us";
        Log.info(c, "getUniqueGroupIdsForUser", "Checking with a valid user.");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML4527E");

        servlet.getUniqueGroupIdsForUser(user);
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a invalid uniqueName.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsWithInvalidUniqueName() throws Exception {
        String user = "invalid";
        Log.info(c, "getUniqueGroupIdsForUser", "Checking with a valid user.");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML4001E");

        servlet.getUniqueGroupIdsForUser(user);
    }
}