/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.AttributesCache;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapEntityType;
import com.ibm.websphere.simplicity.config.wim.LdapFilters;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.SearchResultsCache;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;
import com.unboundid.ldap.sdk.Entry;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPFatUtils;

/**
 * The LDAP registry will exclude members from a group if the members if the members RDN does not match one of those
 * that are configured. This test will verify that adding the RDNs includes the group member.
 *
 * TODO If / when we support rdnProperty, we need to add tests here for that functionality.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LdapRdnTest {
    private static final Class<?> c = LdapRdnTest.class;

    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.rdn");
    private static UserRegistryServletConnection urServlet;
    private static ServerConfiguration baseConfiguration = null;
    private static InMemoryLDAPServer ds;

    private static final String LDAP_BASE_DN = "o=acme.com";

    private static final String USER_UID_RDN_NAME = "user1";
    private static final String USER_UID_RDN_DN = "uid=" + USER_UID_RDN_NAME + "," + LDAP_BASE_DN;

    private static final String USER_CN_RDN_NAME = "user2";
    private static final String USER_CN_RDN_DN = "cn=" + USER_CN_RDN_NAME + "," + LDAP_BASE_DN;

    private static final String USER_SN_RDN_NAME = "user3";
    private static final String USER_SN_RDN_DN = "sn=" + USER_SN_RDN_NAME + "," + LDAP_BASE_DN;

    private static final String GROUP1_NAME = "group1";
    private static final String GROUP1_DN = "cn=" + GROUP1_NAME + "," + LDAP_BASE_DN;

    /**
     * Setup the test case.
     *
     * @throws Exception If the setup failed for some reason.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        setupLibertyServer();
        setupLdapServer();
    }

    /**
     * Tear down the test.
     */
    @AfterClass
    public static void teardownClass() throws Exception {
        try {
            if (libertyServer != null) {
                libertyServer.stopServer();
            }
        } finally {
            if (ds != null) {
                try {
                    ds.shutDown(true);
                } catch (Exception e) {
                    Log.error(c, "teardown", e, "LDAP server threw error while shutting down. " + e.getMessage());
                }
            }

            libertyServer.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
        }
    }

    /**
     * Setup the Liberty server. This server will start with very basic configuration. The tests
     * will configure the server dynamically.
     *
     * @throws Exception If there was an issue setting up the Liberty server.
     */
    private static void setupLibertyServer() throws Exception {
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
        libertyServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        libertyServer.addInstalledAppForValidation("userRegistry");
        libertyServer.startServer(c.getName() + ".log");

        /*
         * Make sure the application has come up before proceeding
         */
        assertNotNull("Application userRegistry does not appear to have started.",
                      libertyServer.waitForStringInLog("CWWKZ0001I:.*userRegistry"));
        assertNotNull("Security service did not report it was ready",
                      libertyServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("Server did not came up",
                      libertyServer.waitForStringInLog("CWWKF0011I"));

        Log.info(c, "setUp", "Creating UserRegistry servlet connection the server");
        urServlet = new UserRegistryServletConnection(libertyServer.getHostname(), libertyServer.getHttpDefaultPort());

        if (urServlet.getRealm() == null) {
            Thread.sleep(5000);
            urServlet.getRealm();
        }

        /*
         * The original server configuration.
         */
        baseConfiguration = libertyServer.getServerConfiguration();
    }

    /**
     * Configure the embedded LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLdapServer() throws Exception {
        ds = new InMemoryLDAPServer(LDAP_BASE_DN);

        /*
         * Add the partition entries.
         */
        Entry entry = new Entry(LDAP_BASE_DN);
        entry.addAttribute("objectclass", "organization");
        entry.addAttribute("o", "acme.com");
        ds.add(entry);

        /*
         * Create some users.
         */
        entry = new Entry(USER_UID_RDN_DN);
        entry.addAttribute("objectclass", "wiminetorgperson");
        entry.addAttribute("uid", USER_UID_RDN_NAME);
        entry.addAttribute("sn", USER_UID_RDN_NAME);
        entry.addAttribute("cn", USER_UID_RDN_NAME);
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

        entry = new Entry(USER_CN_RDN_DN);
        entry.addAttribute("objectclass", "wiminetorgperson");
        entry.addAttribute("uid", USER_CN_RDN_NAME);
        entry.addAttribute("sn", USER_CN_RDN_NAME);
        entry.addAttribute("cn", USER_CN_RDN_NAME);
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

        entry = new Entry(USER_SN_RDN_DN);
        entry.addAttribute("objectclass", "wiminetorgperson");
        entry.addAttribute("uid", USER_SN_RDN_NAME);
        entry.addAttribute("sn", USER_SN_RDN_NAME);
        entry.addAttribute("cn", USER_SN_RDN_NAME);
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

        /*
         * Create some groups.
         */
        entry = new Entry(GROUP1_DN);
        entry.addAttribute("objectclass", "wimgroupofnames");
        entry.addAttribute("cn", GROUP1_NAME);
        entry.addAttribute("member", USER_UID_RDN_DN);
        entry.addAttribute("member", USER_CN_RDN_DN);
        entry.addAttribute("member", USER_SN_RDN_DN);
        ds.add(entry);
    }

    /**
     * Create a basic LDAP registry configuration element for the specified server configuration.
     * The registry will be added to the configuration.
     *
     * @param serverConfig The server configuration to add the LDAP registry to.
     * @param userIdMap    The userIdMap value to configure.
     * @return The new LDAP registry configuration element.
     */
    private static LdapRegistry createLdapRegistry(ServerConfiguration serverConfig, String userIdMap) {
        /*
         * Create and add the new LDAP registry to the server configuration.
         */
        LdapRegistry ldapRegistry = new LdapRegistry();
        serverConfig.getLdapRegistries().add(ldapRegistry);

        /*
         * Configure the LDAP registry.
         */
        ldapRegistry.setBaseDN(LDAP_BASE_DN);
        ldapRegistry.setLdapType("Custom");
        ldapRegistry.setRealm("LdapRealm");
        ldapRegistry.setHost("localhost");
        ldapRegistry.setPort(String.valueOf(ds.getLdapPort()));
        ldapRegistry.setBindDN(ds.getBindDN());
        ldapRegistry.setBindPassword(ds.getBindPassword());
        ldapRegistry.setCustomFilters(new LdapFilters("(&(|(uid=%v)(cn=%v)(sn=%v))(objectclass=inetorgperson))", "(&(cn=%v)(objectclass=groupofnames))", userIdMap, null, "groupOfNames:member"));
        ldapRegistry.setLdapCache(new LdapCache(new AttributesCache(false, null, null, null), new SearchResultsCache(false, null, null, null)));

        return ldapRegistry;
    }

    /**
     * Test adding RDNs using the userIdMap value of "inetorgperson:cn".
     *
     * The expectation is that the user with the CN RDN will be included.
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useridmap_INETORGPERSON_CN() throws Exception {
        runTest("inetorgperson:cn", USER_CN_RDN_DN);
    }

    /**
     * Test adding RDNs using the userIdMap value of "inetorgperson:cn".
     *
     * The expectation is that the user with the SN RDN will be included.
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useridmap_INETORGPERSON_SN() throws Exception {
        runTest("inetorgperson:sn", USER_SN_RDN_DN);
    }

    /**
     * Test adding RDNs using the userIdMap value of "inetorgperson:uid".
     *
     * The expectation is that the user with the UID RDN will be included.
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useridmap_INETORGPERSON_UID() throws Exception {
        runTest("inetorgperson:uid", USER_UID_RDN_DN);
    }

    /**
     * Test adding RDNs using the userIdMap value of "inetorgperson:uid;inetorgperson:cn".
     *
     * The expectation is that the users with the CN and UID RDNs will be included.
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useridmap_INETORGPERSON_UID_and_INETORGPERSON_CN() throws Exception {
        runTest("inetorgperson:uid;inetorgperson:cn", USER_UID_RDN_DN, USER_CN_RDN_DN);
    }

    /**
     * Test adding RDNs using the userIdMap value of "inetorgperson:uid;inetorgperson:sn".
     *
     * The expectation is that the users with the UID and SN RDNs will be included.
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useridmap_INETORGPERSON_UID_and_INETORGPERSON_SN() throws Exception {
        runTest("inetorgperson:uid;inetorgperson:sn", USER_UID_RDN_DN, USER_SN_RDN_DN);
    }

    /**
     * Test adding RDNs using the userIdMap value of "inetorgperson:uid;inetorgperson:cn;inetorgperson:sn".
     *
     * The expectation is that the users with the UID, CN and SN RDNs will be included.
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useridmap_INETORGPERSON_UID_and_INETORGPERSON_CN_and_INETORGPERSON_SN() throws Exception {
        runTest("inetorgperson:uid;inetorgperson:cn;inetorgperson:sn", USER_UID_RDN_DN, USER_CN_RDN_DN, USER_SN_RDN_DN);
    }

    /**
     * Test adding RDNs using the userIdMap value of "*:uid".
     *
     * The expectation is that the users with the UID RDN will be included.
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useridmap_WILCARD_UID() throws Exception {
        runTest("*:uid", USER_UID_RDN_DN);
    }

    /**
     * Test adding RDNs using the userIdMap value of "*:cn".
     *
     * The expectation is that the users with the CN RDN will be included.
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useridmap_WILCARD_CN() throws Exception {
        runTest("*:cn", USER_CN_RDN_DN);
    }

    /**
     * Test adding RDNs using the userIdMap value of "*:sn".
     *
     * The expectation is that the users with the SN RDN will be included.
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useridmap_WILCARD_SN() throws Exception {
        runTest("*:sn", USER_SN_RDN_DN);
    }

    /**
     * Test adding RDNs using the userIdMap value of "*:uid;*:cn".
     *
     * The expectation is that the users with the UID and CN RDNs will be included.
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useridmap_WILCARD_UID_and_WILDCARD_CN() throws Exception {
        runTest("*:uid;*:cn", USER_UID_RDN_DN, USER_CN_RDN_DN);
    }

    /**
     * Test adding RDNs using the userIdMap value of "*:uid;*:sn".
     *
     * The expectation is that the users with the UID and SN RDNs will be included.
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useridmap_WILCARD_UID_and_WILDCARD_SN() throws Exception {
        runTest("*:uid;*:sn", USER_UID_RDN_DN, USER_SN_RDN_DN);
    }

    /**
     * Test adding RDNs using the userIdMap value of "*:sn;*:cn".
     *
     * The expectation is that the users with the SN and CN RDNs will be included.
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useridmap_WILCARD_SN_and_WILDCARD_CN() throws Exception {
        runTest("*:sn;*:cn", USER_SN_RDN_DN, USER_CN_RDN_DN);
    }

    /**
     * Test adding RDNs using the userIdMap value of "*:uid;*:cn;*:sn".
     *
     * The expectation is that the users with the UID, CN and SN RDNs will be included.
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useridmap_WILCARD_UID_and_WILDCARD_CN_and_WILDCARD_SN() throws Exception {
        runTest("*:uid;*:cn;*:sn", USER_UID_RDN_DN, USER_CN_RDN_DN, USER_SN_RDN_DN);
    }

    /**
     * Run some operations that will verify the functionality of the RDNs included in userIdMap.
     *
     * @param userIdMap       The userIdMap to configure the server with.
     * @param expectedMembers The expected members.
     * @throws Exception If the test failed for some unforeseen reason.
     */
    private static void runTest(String userIdMap, String... expectedMembers) throws Exception {
        ServerConfiguration clone = baseConfiguration.clone();
        createLdapRegistry(clone, userIdMap);
        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        /*
         * Start by looking up groups for users. Each user should be a member of
         * the group as it doesn't appear there is an RDN check for user group lookups.
         */
        List<String> results = urServlet.getGroupsForUser(USER_UID_RDN_DN);
        assertMembershipResults("User " + USER_UID_RDN_DN + " should be a member of " + GROUP1_DN, results, GROUP1_DN);

        results = urServlet.getGroupsForUser(USER_CN_RDN_DN);
        assertMembershipResults("User " + USER_CN_RDN_DN + " should be a member of " + GROUP1_DN, results, GROUP1_DN);

        results = urServlet.getGroupsForUser(USER_SN_RDN_DN);
        assertMembershipResults("User " + USER_SN_RDN_DN + " should be a member of " + GROUP1_DN, results, GROUP1_DN);

        /*
         * RDNs do play in on members being returned for group membership. Check against
         * the passed in expected members.
         */
        SearchResult sr = urServlet.getUsersForGroup(GROUP1_DN, 0);
        assertMembershipResults("The expected members were not found.", sr.getList(), expectedMembers);
    }

    /**
     * Assert that group membership is as expected.
     *
     * @param message  The message to print on failure.
     * @param actual   The actual members or groups.
     * @param expected The expected members or groups.
     */
    private static void assertMembershipResults(String message, Collection<String> actual, String... expected) {
        assertEquals(message + " The expected and actual size were different. Expected: " + Arrays.toString(expected) + ", Actual: " + actual, expected.length, actual.size());

        for (String expect : expected) {
            assertTrue(message + "The '" + expect + "'expected value was not found in the actual result. Expected: " + Arrays.toString(expected) + ", Actual: " + actual,
                       actual.contains(expect));
        }
    }
}
