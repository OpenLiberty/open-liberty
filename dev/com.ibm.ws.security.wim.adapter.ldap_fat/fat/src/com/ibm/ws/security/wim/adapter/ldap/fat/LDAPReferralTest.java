/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.AttributesCache;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.SearchResultsCache;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/**
 * Test referral handling for the LDAP registry.
 *
 * <p/>
 * This tests uses 2 embedded ApacheDS servers. The delegate server has a referral to the subordinate
 * server where a user and group exist. The test will verify that those entities are not available
 * through referrals from the delegate server.
 *
 * <p />
 * There is an issue where ApacheDS does not appear to be processing referrals for subtrees correctly
 * which has restricted the type of testing I can do. I have opened the following defect:
 *
 * <p/>
 * https://issues.apache.org/jira/browse/DIRSERVER-2198
 *
 * <p/>
 * TODO: Once this issue has been resolved and the build with the fix is integrated into our builds, the search base
 * in {@link #getServerConfig(EmbeddedADS, String)} should be updated to use the {@link #DELEGATE_DN}. Additionally the
 * {@link #setupDelegateServer()}, {@link #setupSubordinateServer()}, {@link #assertFollowResults()} and
 * {@link #assertIgnoreResults()} methods should be updated to uncomment commented out code and update
 * any failing test assertions.
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class LDAPReferralTest {

    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.referral");
    private static final Class<?> c = LDAPReferralTest.class;
    private static UserRegistryServletConnection servlet;

    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
    private static ServerConfiguration emptyConfiguration = null;

    private static EmbeddedApacheDS delegateServer = null;
    private static final String DELEGATE_DN = "o=ibm,c=us";
//    private static final String DELEGATE_USER_DN = "uid=user1," + DELEGATE_DN;
//    private static final String DELEGATE_GROUP_DN = "cn=group1," + DELEGATE_DN;

    private static EmbeddedApacheDS subordinateServer = null;
    private static final String SUBORDINATE_DN = "ou=subtree,o=ibm,c=us";
    private static final String SUBORDINATE_USER_PRINCIPAL = "user2";
    private static final String SUBORDINATE_USER_DN = "uid=" + SUBORDINATE_USER_PRINCIPAL + "," + SUBORDINATE_DN;
    private static final String SUBORDINATE_GROUP_DN = "cn=group2," + SUBORDINATE_DN;

    /**
     * Setup the test case.
     *
     * @throws Exception If the setup failed for some reason.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        setupLibertyServer();
        setupSubordinateServer();
        setupDelegateServer();
    }

    /**
     * Tear down the test.
     */
    @AfterClass
    public static void teardownClass() throws Exception {
        if (libertyServer != null) {
            try {
                libertyServer.stopServer();
            } catch (Exception e) {
                Log.error(c, "teardown", e, "Liberty server threw error while stopping. " + e.getMessage());
            }
        }
        if (delegateServer != null) {
            try {
                delegateServer.stopService();
            } catch (Exception e) {
                Log.error(c, "teardown", e, "Delegate LDAP server threw error while stopping. " + e.getMessage());
            }
        }
        if (subordinateServer != null) {
            try {
                subordinateServer.stopService();
            } catch (Exception e) {
                Log.error(c, "teardown", e, "Subordinate LDAP server threw error while stopping. " + e.getMessage());
            }
        }

        libertyServer.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
    }

    /**
     * Setup the Liberty server. This server will start with very basic configuration. The tests
     * will configure the server dynamically.
     *
     * @throws Exception If there was an issue setting up the Liberty server.
     */
    private static void setupLibertyServer() throws Exception {
        /*
         * Add LDAP variables to bootstrap properties file
         */
        LDAPUtils.addLDAPVariables(libertyServer);
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

        Log.info(c, "setUp", "Creating servlet connection the server");
        servlet = new UserRegistryServletConnection(libertyServer.getHostname(), libertyServer.getHttpDefaultPort());

        if (servlet.getRealm() == null) {
            Thread.sleep(5000);
            servlet.getRealm();
        }

        /*
         * The original server configuration has no registry or Federated Repository configuration.
         */
        emptyConfiguration = libertyServer.getServerConfiguration();
    }

    /**
     * Configure the delegate server. This is the LDAP server who will refer to the subordinate server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupSubordinateServer() throws Exception {
        subordinateServer = new EmbeddedApacheDS("subordinate");
        subordinateServer.addPartition("testing", SUBORDINATE_DN);
        subordinateServer.startServer();

        /*
         * Add the partition entries.
         */
        Entry entry = subordinateServer.newEntry(SUBORDINATE_DN);
        entry.add("objectclass", "organizationalunit");
        entry.add("ou", "subtree");
        subordinateServer.add(entry);

        /*
         * Create the user and group.
         */
        entry = subordinateServer.newEntry(SUBORDINATE_USER_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", SUBORDINATE_USER_PRINCIPAL);
        entry.add("sn", SUBORDINATE_USER_PRINCIPAL);
        entry.add("cn", SUBORDINATE_USER_PRINCIPAL);
        entry.add("userPassword", "password");
        subordinateServer.add(entry);

        entry = subordinateServer.newEntry(SUBORDINATE_GROUP_DN);
        entry.add("objectclass", "groupofnames");
        entry.add("cn", "group2");
//        entry.add("member", DELEGATE_USER_DN);
        entry.add("member", SUBORDINATE_USER_DN);
        subordinateServer.add(entry);
    }

    /**
     * Configure the delegate server. This is the LDAP server who will refer to the subordinate server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupDelegateServer() throws Exception {
        delegateServer = new EmbeddedApacheDS("delegate");
        delegateServer.addPartition("testing", DELEGATE_DN);
        delegateServer.startServer();

        /*
         * Add the partition entries.
         */
        Entry entry = delegateServer.newEntry(DELEGATE_DN);
        entry.add("objectclass", "organization");
        entry.add("o", "ibm");
        delegateServer.add(entry);

        /*
         * Create the user and group.
         */
//        entry = delegateServer.newEntry(DELEGATE_USER_DN);
//        entry.add("objectclass", "inetorgperson");
//        entry.add("uid", "user1");
//        entry.add("sn", "user1");
//        entry.add("cn", "user1");
//        entry.add("userPassword", "password");
//        delegateServer.add(entry);
//
//        entry = delegateServer.newEntry(DELEGATE_GROUP_DN);
//        entry.add("objectclass", "groupofnames");
//        entry.add("cn", "group1");
//        entry.add("member", DELEGATE_USER_DN);
//        entry.add("member", SUBORDINATE_USER_DN);
//        delegateServer.add(entry);

        /*
         * Referral to the subordinate server that contains a subtree of this server's base DN.
         */
        entry = subordinateServer.newEntry(SUBORDINATE_DN);
        entry.add("objectclass", "referral", "extensibleobject");
        entry.add("ou", "subtree");
        entry.add("ref", "ldap://localhost:" + subordinateServer.getLdapServer().getPort() + "/" + SUBORDINATE_DN);
        delegateServer.add(entry);
    }

    /**
     * Convenience method to configure the Liberty server with an {@link LdapRegistry} configuration that
     * will connect to {@link #delegateServer}.
     *
     * @param referral The 'referral' attribute value to set. Null indicates to leave it unset.
     * @param referal The 'referal' attribute value to set. Null indicates to leave it unset.
     * @throws Exception If there was an error configuring the server.
     */
    private static void updateLibertyServer(String referral, String referal) throws Exception {
        ServerConfiguration server = emptyConfiguration.clone();

        LdapRegistry ldap = new LdapRegistry();

        ldap.setRealm("LDAPRealm");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(delegateServer.getLdapServer().getPort()));
        ldap.setBaseDN(SUBORDINATE_DN);
        ldap.setBindDN(EmbeddedApacheDS.getBindDN());
        ldap.setBindPassword(EmbeddedApacheDS.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setLdapCache(new LdapCache(new AttributesCache(false, 0, 0, "0s"), new SearchResultsCache(false, 0, 0, "0s")));

        ldap.setReferral(referral);
        ldap.setReferal(referal);

        server.getLdapRegistries().add(ldap);
        updateConfigDynamically(libertyServer, server);
    }

    /**
     * Make assertions on the results of when the 'referral' attribute was set to 'follow'.
     *
     * @throws Exception If there was an unexpected exception.
     */
    private static void assertFollowResults() throws Exception {
//        assertEquals(DELEGATE_USER_DN, servlet.checkPassword(DELEGATE_USER_DN, "password"));
        assertNull("Should not be able to bind with user from referral.", servlet.checkPassword(SUBORDINATE_USER_DN, "password"));

//        assertEquals("group1", servlet.getGroupDisplayName(DELEGATE_GROUP_DN));
        assertEquals("group2", servlet.getGroupDisplayName(SUBORDINATE_GROUP_DN));

        List<String> results = servlet.getGroups("*group*", 0).getList();
//        assertTrue("Missing group '" + DELEGATE_GROUP_DN + "'. Results: " + results, results.contains(DELEGATE_GROUP_DN));
        assertTrue("Missing group '" + SUBORDINATE_GROUP_DN + "'. Results: " + results, results.contains(SUBORDINATE_GROUP_DN));

//        assertEquals(DELEGATE_GROUP_DN, servlet.getGroupSecurityName("group1"));
        assertEquals(SUBORDINATE_GROUP_DN, servlet.getGroupSecurityName("group2"));

//        results = servlet.getGroupsForUser(DELEGATE_USER_DN);
//        assertTrue("Missing group '" + DELEGATE_GROUP_DN + "'. Results: " + results, results.contains(DELEGATE_GROUP_DN));
//        assertTrue("Missing group '" + SUBORDINATE_GROUP_DN + "'. Results: " + results, results.contains(SUBORDINATE_GROUP_DN));
//        results = servlet.getGroupsForUser(SUBORDINATE_USER_DN);
//        assertTrue("Missing group '" + DELEGATE_GROUP_DN + "'. Results: " + results, results.contains(DELEGATE_GROUP_DN));
//        assertTrue("Missing group '" + SUBORDINATE_GROUP_DN + "'. Results: " + results, results.contains(SUBORDINATE_GROUP_DN));

//        assertEquals(DELEGATE_GROUP_DN, servlet.getUniqueGroupId(DELEGATE_GROUP_DN));
        assertEquals(SUBORDINATE_GROUP_DN, servlet.getUniqueGroupId(SUBORDINATE_GROUP_DN));

//        results = servlet.getUniqueGroupIdsForUser("user1");
//        assertTrue("Missing group 'group1'. Results: " + results, results.contains(DELEGATE_GROUP_DN));
//        assertTrue("Missing group 'group2'. Results: " + results, results.contains(SUBORDINATE_GROUP_DN"));
        results = servlet.getUniqueGroupIdsForUser(SUBORDINATE_USER_PRINCIPAL);
//        assertTrue("Missing group 'group1'. Results: " + results, results.contains(DELEGATE_GROUP_DN"));
        assertTrue("Missing group 'group2'. Results: " + results, results.contains(SUBORDINATE_GROUP_DN));

//        assertEquals(DELEGATE_USER_DN, servlet.getUniqueUserId(DELEGATE_USER_DN));
        assertEquals(SUBORDINATE_USER_DN, servlet.getUniqueUserId(SUBORDINATE_USER_DN));

//        assertEquals("user1", servlet.getUserDisplayName(DELEGATE_USER_DN));
        assertEquals(SUBORDINATE_USER_PRINCIPAL, servlet.getUserDisplayName(SUBORDINATE_USER_DN));

        results = servlet.getUsers("*user*", 0).getList();
//        assertTrue("Missing user '" + DELEGATE_USER_DN + "'. Results: " + results, results.contains(DELEGATE_USER_DN));
        assertTrue("Missing user '" + SUBORDINATE_USER_DN + "'. Results: " + results, results.contains(SUBORDINATE_USER_DN));

//        assertEquals(DELEGATE_USER_DN, servlet.getUserSecurityName("user1"));
        assertEquals(SUBORDINATE_USER_DN, servlet.getUserSecurityName(SUBORDINATE_USER_PRINCIPAL));

//        results = servlet.getUsersForGroup(DELEGATE_GROUP_DN, 0).getList();
//        assertTrue("Missing user '" + DELEGATE_USER_DN + "'. Results: " + results, results.contains(DELEGATE_USER_DN));
//        assertTrue("Missing user '" + SUBORDINATE_USER_DN + "'. Results: " + results, results.contains(SUBORDINATE_USER_DN));
        results = servlet.getUsersForGroup(SUBORDINATE_GROUP_DN, 0).getList();
//        assertTrue("Missing user '" + DELEGATE_USER_DN + "'. Results: " + results, results.contains(DELEGATE_USER_DN));
        assertTrue("Missing user '" + SUBORDINATE_USER_DN + "'. Results: " + results, results.contains(SUBORDINATE_USER_DN));

//        assertTrue("Expected '" + DELEGATE_GROUP_DN + "' to be valid group.", servlet.isValidGroup(DELEGATE_GROUP_DN));
        assertTrue("Expected 'group2' to be valid group.", servlet.isValidGroup("group2")); // Group security name mapping defaults to 'cn'

//        assertTrue("Expected '" + DELEGATE_USER_DN + "' to be valid user.", servlet.isValidUser(DELEGATE_USER_DN));
        assertTrue("Expected '" + SUBORDINATE_USER_DN + "' to be valid user.", servlet.isValidUser(SUBORDINATE_USER_DN));
    }

    /**
     * Make assertions on the results of when the 'referral' attribute was set to 'ignore'.
     *
     * @throws Exception If there was an unexpected exception.
     */
    private static void assertIgnoreResults() throws Exception {

        /*
         * This test can be removed after the ApacheDS referral issue has been fixed. Currently just
         * check that instead of getting a continuation reference, that we get an error.
         */
        try {
            servlet.getGroupDisplayName(SUBORDINATE_DN);
            fail("Excected RegistryException.");
        } catch (RegistryException e) {
            /*
             * We will get an error trying to convert the "referral" entity into a "Group" entity. This occurs
             * b/c ignoring referrals results in us receiving the referral LDAP entity itself (even though
             * it doesn't match the filter). Again, this should go away when we are not using the referral
             * itself as the search base.
             */
            Log.info(c, "assertIgnoreResults", "Exception is " + e.getMessage());
            assertNotNull("Exception should have an error message", e.getMessage());
        }

//      assertEquals(DELEGATE_USER_DN, servlet.checkPassword(DELEGATE_USER_DN, "password"));
//      assertNull("Should not be able to bind with user from referral.", servlet.checkPassword(SUBORDINATE_USER_DN, "password"));

//      assertEquals("group1", servlet.getGroupDisplayName(DELEGATE_GROUP_DN));
//        try {
//            servlet.getGroupDisplayName(SUBORDINATE_GROUP_DN);
//            fail("Expected EntryNotFoundException");
//        } catch (EntryNotFoundException e) {
//            // Expected.
//        }

//        List<String> results = servlet.getGroups("*group*", 0).getList();
//      assertTrue("Missing group '" + DELEGATE_GROUP_DN + "'. Results: " + results, results.contains(DELEGATE_GROUP_DN));
//        assertFalse("Found unexpected group '" + SUBORDINATE_GROUP_DN + "'. Results: " + results, results.contains(SUBORDINATE_GROUP_DN));

//      assertEquals(DELEGATE_GROUP_DN, servlet.getGroupSecurityName("group1"));
//        try {
//            servlet.getGroupSecurityName("group2");
//            fail("Expected EntryNotFoundException");
//        } catch (EntryNotFoundException e) {
//            // Expected.
//        }

//      results = servlet.getGroupsForUser(DELEGATE_USER_DN);
//      assertTrue("Missing group '" + DELEGATE_GROUP_DN + "'. Results: " + results, results.contains(DELEGATE_GROUP_DN));
//      assertTrue("Missing group '" + SUBORDINATE_GROUP_DN + "'. Results: " + results, results.contains(SUBORDINATE_GROUP_DN));
//      results = servlet.getGroupsForUser(SUBORDINATE_USER_DN);
//      assertTrue("Missing group '" + DELEGATE_GROUP_DN + "'. Results: " + results, results.contains(DELEGATE_GROUP_DN));
//      assertTrue("Missing group '" + SUBORDINATE_GROUP_DN + "'. Results: " + results, results.contains(SUBORDINATE_GROUP_DN));

//      assertEquals(DELEGATE_GROUP_DN, servlet.getUniqueGroupId(DELEGATE_GROUP_DN));
//        try {
//            servlet.getUniqueGroupId(SUBORDINATE_GROUP_DN);
//            fail("Expected EntryNotFoundException");
//        } catch (EntryNotFoundException e) {
//            // Expected.
//        }

//      results = servlet.getUniqueGroupIdsForUser("user1");
//      assertTrue("Missing group 'group1'. Results: " + results, results.contains(DELEGATE_GROUP_DN));
//      assertTrue("Missing group 'group2'. Results: " + results, results.contains(SUBORDINATE_GROUP_DN"));
//        results = servlet.getUniqueGroupIdsForUser(SUBORDINATE_USER_PRINCIPAL);
//      assertTrue("Missing group '"+DELEGATE_GROUP_DN+"'. Results: " + results, results.contains(DELEGATE_GROUP_DN"));
//        assertFalse("Found unexpected group '" + SUBORDINATE_GROUP_DN + "'. Results: " + results, results.contains(SUBORDINATE_GROUP_DN));

//      assertEquals(DELEGATE_USER_DN, servlet.getUniqueUserId(DELEGATE_USER_DN));
//        try {
//            servlet.getUniqueUserId(SUBORDINATE_USER_DN);
//            fail("Expected EntryNotFoundException");
//        } catch (EntryNotFoundException e) {
//            // Expected.
//        }

//      assertEquals("user1", servlet.getUserDisplayName(DELEGATE_USER_DN));
//        try {
//            servlet.getUserDisplayName(SUBORDINATE_USER_DN);
//            fail("Expected EntryNotFoundException");
//        } catch (EntryNotFoundException e) {
//            // Expected.
//        }

//        results = servlet.getUsers("*user*", 0).getList();
//      assertTrue("Missing user '" + DELEGATE_USER_DN + "'. Results: " + results, results.contains(DELEGATE_USER_DN));
//        assertFalse("Found unexpected user '" + SUBORDINATE_USER_DN + "'. Results: " + results, results.contains(SUBORDINATE_USER_DN));

//      assertEquals(DELEGATE_USER_DN, servlet.getUserSecurityName("user1"));
//        try {
//            servlet.getUserSecurityName(SUBORDINATE_USER_PRINCIPAL);
//            fail("Expected EntryNotFoundException");
//        } catch (EntryNotFoundException e) {
//            // Expected.
//        }

//      results = servlet.getUsersForGroup(DELEGATE_GROUP_DN, 0).getList();
//      assertTrue("Missing user '" + DELEGATE_USER_DN + "'. Results: " + results, results.contains(DELEGATE_USER_DN));
//      assertTrue("Missing user '" + SUBORDINATE_USER_DN + "'. Results: " + results, results.contains(SUBORDINATE_USER_DN));
//        try {
//            servlet.getUsersForGroup(SUBORDINATE_GROUP_DN, 0);
//            fail("Expected EntryNotFoundException");
//        } catch (EntryNotFoundException e) {
//            // Expected.
//        }

//      assertTrue("Expected '" + DELEGATE_GROUP_DN + "' to be valid group.", servlet.isValidGroup(DELEGATE_GROUP_DN));
//        assertFalse("Expected 'group2' to NOT be valid group.", servlet.isValidGroup("group2"));

//      assertTrue("Expected '" + DELEGATE_USER_DN + "' to be valid user.", servlet.isValidUser(DELEGATE_USER_DN));
//        assertFalse("Expected '" + SUBORDINATE_USER_DN + "' to NOT be valid user.", servlet.isValidUser(SUBORDINATE_USER_DN));
    }

    /**
     * Test with referral unset. It will default to 'ignore'.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    public void testReferralDefault() throws Exception {
        updateLibertyServer(null, null);

        assertIgnoreResults();
    }

    /**
     * Test with 'referral' set to 'follow'.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    public void testReferralFollow() throws Exception {
        updateLibertyServer("follow", null);

        assertFollowResults();
    }

    /**
     * Test with 'referral' set to 'ignore'.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    public void testReferralIgnore() throws Exception {
        updateLibertyServer("ignore", null);

        assertIgnoreResults();
    }

    /**
     * Test with 'referal' set to 'follow'.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    public void testReferalFollow() throws Exception {
        updateLibertyServer(null, "follow");

        assertFollowResults();
    }

    /**
     * Test with 'referal' set to 'ignore'.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    public void testReferalIgnore() throws Exception {
        updateLibertyServer(null, "ignore");

        assertIgnoreResults();
    }

    /**
     * Test with 'referral' and 'referal' both set. The old 'referal'
     * attribute value should take precedence as there is no
     * default value set which means it must have been consciously set by the user.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    public void testReferralReferal() throws Exception {
        updateLibertyServer("follow", "ignore");

        assertIgnoreResults();
    }

    /**
     * Test with an invalid 'referral' attribute value.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    public void testReferralInvalid() throws Exception {
        updateLibertyServer("invalid", null);

        final String CWWKG0032W = "CWWKG0032W: Unexpected value specified for property \\[referral]";
        List<String> errorResults = libertyServer.findStringsInLogsAndTraceUsingMark(CWWKG0032W);
        assertTrue("Did not find '" + CWWKG0032W + "' in trace: " + errorResults, !errorResults.isEmpty());

        assertIgnoreResults();
    }

    /**
     * Test with an invalid 'referral' attribute value.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    public void testReferalInvalid() throws Exception {
        updateLibertyServer(null, "invalid");

        final String CWWKG0032W = "CWWKG0032W: Unexpected value specified for property \\[referal]";
        List<String> errorResults = libertyServer.findStringsInLogsAndTraceUsingMark(CWWKG0032W);
        assertTrue("Did not find '" + CWWKG0032W + "' in trace: " + errorResults, !errorResults.isEmpty());

        assertIgnoreResults();
    }
}