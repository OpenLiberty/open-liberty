/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import com.ibm.websphere.simplicity.config.wim.DynamicMemberAttribute;
import com.ibm.websphere.simplicity.config.wim.GroupProperties;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapEntityType;
import com.ibm.websphere.simplicity.config.wim.LdapFilters;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.MemberAttribute;
import com.ibm.websphere.simplicity.config.wim.MembershipAttribute;
import com.ibm.websphere.simplicity.config.wim.SearchResultsCache;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;
import com.ibm.ws.security.wim.adapter.ldap.LdapConstants;
import com.unboundid.ldap.sdk.Entry;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPFatUtils;

/**
 * Test the member and membership attribute scope settings..
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LDAPMemberAttributeScopeTest {
    private static final Class<?> c = LDAPMemberAttributeScopeTest.class;

    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.ldap.fat.scope");
    private static UserRegistryServletConnection urServlet;
    private static ServerConfiguration baseConfiguration = null;
    private static InMemoryLDAPServer ds;
    private static final String MEMBER_BASE_DN = "o=member";
    private static final String MEMBERSHIP_BASE_DN = "o=membership";

    // Users
    private static final String LEVEL1_USER_NAME = "user_level1";
    private static final String MEMBER_LEVEL1_USER_DN = "uid=" + LEVEL1_USER_NAME + "," + MEMBER_BASE_DN;
    private static final String MEMBERSHIP_LEVEL1_USER_DN = "uid=" + LEVEL1_USER_NAME + "," + MEMBERSHIP_BASE_DN;
    private static final String LEVEL2_USER_NAME = "user_level2";
    private static final String MEMBER_LEVEL2_USER_DN = "uid=" + LEVEL2_USER_NAME + "," + MEMBER_BASE_DN;
    private static final String MEMBERSHIP_LEVEL2_USER_DN = "uid=" + LEVEL2_USER_NAME + "," + MEMBERSHIP_BASE_DN;
    private static final String DYNAMIC_USER_NAME = "user_dynamic";
    private static final String MEMBER_DYNAMIC_USER_DN = "uid=" + DYNAMIC_USER_NAME + "," + MEMBER_BASE_DN;
    private static final String MEMBERSHIP_DYNAMIC_USER_DN = "uid=" + DYNAMIC_USER_NAME + "," + MEMBERSHIP_BASE_DN;

    // Groups
    private static final String LEVEL1_GROUP_NAME = "group_level1";
    private static final String MEMBER_LEVEL1_GROUP_DN = "cn=" + LEVEL1_GROUP_NAME + "," + MEMBER_BASE_DN;
    private static final String MEMBERSHIP_LEVEL1_GROUP_DN = "cn=" + LEVEL1_GROUP_NAME + "," + MEMBERSHIP_BASE_DN;
    private static final String LEVEL2_GROUP_NAME = "group_level2";
    private static final String MEMBER_LEVEL2_GROUP_DN = "cn=" + LEVEL2_GROUP_NAME + "," + MEMBER_BASE_DN;
    private static final String MEMBERSHIP_LEVEL2_GROUP_DN = "cn=" + LEVEL2_GROUP_NAME + "," + MEMBERSHIP_BASE_DN;
    private static final String DYNAMIC_GROUP_NAME = "group_dynamic";
    private static final String MEMBER_DYNAMIC_GROUP_DN = "cn=" + DYNAMIC_GROUP_NAME + "," + MEMBER_BASE_DN;
    private static final String MEMBERSHIP_DYNAMIC_GROUP_DN = "cn=" + DYNAMIC_GROUP_NAME + "," + MEMBERSHIP_BASE_DN;

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
         * The original server configuration has no registry or Federated Repository configuration.
         */
        baseConfiguration = libertyServer.getServerConfiguration();
    }

    /**
     * Configure the embedded LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLdapServer() throws Exception {
        ds = new InMemoryLDAPServer(MEMBER_BASE_DN, MEMBERSHIP_BASE_DN);

        /*
         * Add the partition entries.
         */
        Entry entry = new Entry(MEMBER_BASE_DN);
        entry.addAttribute("objectclass", "organization");
        entry.addAttribute("o", "member");
        ds.add(entry);

        entry = new Entry(MEMBERSHIP_BASE_DN);
        entry.addAttribute("objectclass", "organization");
        entry.addAttribute("o", "membership");
        ds.add(entry);

        /**
         * Create the following users and groups for both MEMBER and MEMBERSHIP attribute tests. We
         * create two separate sets of groups for MEMBER and MEMBERSHIP tests because when a MEMBERSHIP
         * attribute is configured, if the membership attribute is not found on the group it will fall
         * back to the member attribute, which leads to unclear results.
         *
         * <pre>
         * group_level1
         *   user_level1
         *   group_level2
         *     user_level2
         *   group_dynamic
         *     user_dynamic
         * </pre>
         */

        /*
         * Users
         */
        entry = new Entry(MEMBER_LEVEL1_USER_DN);
        entry.addAttribute("objectclass", "wiminetorgperson");
        entry.addAttribute("uid", LEVEL1_USER_NAME);
        entry.addAttribute("sn", LEVEL1_USER_NAME);
        entry.addAttribute("cn", LEVEL1_USER_NAME);
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

        entry = new Entry(MEMBER_LEVEL2_USER_DN);
        entry.addAttribute("objectclass", "wiminetorgperson");
        entry.addAttribute("uid", LEVEL2_USER_NAME);
        entry.addAttribute("sn", LEVEL2_USER_NAME);
        entry.addAttribute("cn", LEVEL2_USER_NAME);
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

        entry = new Entry(MEMBERSHIP_LEVEL1_USER_DN);
        entry.addAttribute("objectclass", "wiminetorgperson");
        entry.addAttribute("uid", LEVEL1_USER_NAME);
        entry.addAttribute("sn", LEVEL1_USER_NAME);
        entry.addAttribute("cn", LEVEL1_USER_NAME);
        entry.addAttribute("memberof", MEMBERSHIP_LEVEL1_GROUP_DN);
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

        entry = new Entry(MEMBERSHIP_LEVEL2_USER_DN);
        entry.addAttribute("objectclass", "wiminetorgperson");
        entry.addAttribute("uid", LEVEL2_USER_NAME);
        entry.addAttribute("sn", LEVEL2_USER_NAME);
        entry.addAttribute("cn", LEVEL2_USER_NAME);
        entry.addAttribute("memberof", MEMBERSHIP_LEVEL2_GROUP_DN);
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

        entry = new Entry(MEMBER_DYNAMIC_USER_DN);
        entry.addAttribute("objectclass", "wiminetorgperson");
        entry.addAttribute("uid", DYNAMIC_USER_NAME);
        entry.addAttribute("sn", DYNAMIC_USER_NAME);
        entry.addAttribute("cn", DYNAMIC_USER_NAME);
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

        entry = new Entry(MEMBERSHIP_DYNAMIC_USER_DN);
        entry.addAttribute("objectclass", "wiminetorgperson");
        entry.addAttribute("uid", DYNAMIC_USER_NAME);
        entry.addAttribute("sn", DYNAMIC_USER_NAME);
        entry.addAttribute("cn", DYNAMIC_USER_NAME);
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

        /*
         * Groups
         */
        entry = new Entry(MEMBER_LEVEL1_GROUP_DN);
        entry.addAttribute("objectclass", "wimgroupofnames");
        entry.addAttribute("cn", LEVEL1_GROUP_NAME);
        entry.addAttribute("member", MEMBER_LEVEL1_USER_DN);
        entry.addAttribute("member", MEMBER_LEVEL2_GROUP_DN);
        entry.addAttribute("member", MEMBER_DYNAMIC_GROUP_DN);
        ds.add(entry);

        entry = new Entry(MEMBER_LEVEL2_GROUP_DN);
        entry.addAttribute("objectclass", "wimgroupofnames");
        entry.addAttribute("cn", LEVEL2_GROUP_NAME);
        entry.addAttribute("member", MEMBER_LEVEL2_USER_DN);
        ds.add(entry);

        entry = new Entry(MEMBERSHIP_LEVEL1_GROUP_DN);
        entry.addAttribute("objectclass", "wimgroupofnames");
        entry.addAttribute("cn", LEVEL1_GROUP_NAME);
        ds.add(entry);

        entry = new Entry(MEMBERSHIP_LEVEL2_GROUP_DN);
        entry.addAttribute("objectclass", "wimgroupofnames");
        entry.addAttribute("cn", LEVEL2_GROUP_NAME);
        entry.addAttribute("memberof", MEMBERSHIP_LEVEL1_GROUP_DN);
        ds.add(entry);

        entry = new Entry(MEMBER_DYNAMIC_GROUP_DN);
        entry.addAttribute("objectclass", "groupofurls");
        entry.addAttribute("cn", DYNAMIC_GROUP_NAME);
        entry.addAttribute("memberurl", "ldap:///" + MEMBER_BASE_DN + "??sub?(uid=*dynamic*)");
        ds.add(entry);

        entry = new Entry(MEMBERSHIP_DYNAMIC_GROUP_DN);
        entry.addAttribute("objectclass", "groupofurls");
        entry.addAttribute("cn", DYNAMIC_GROUP_NAME);
        entry.addAttribute("memberurl", "ldap:///" + MEMBERSHIP_BASE_DN + "??sub?(uid=*dynamic*)");
        ds.add(entry);
    }

    /**
     * Create a basic LDAP registry configuration element for the specified server configuration.
     * The registry will be added to the configuration.
     *
     * @param serverConfig The server configuration to add the LDAP registry to.
     * @param baseDN The base DN to use for the LDAP registry.
     * @param memberAttrs The memberAttribute configurations to use, if any.
     * @param membershipAttr The membershipAttr to use, if any.
     * @return The new LDAP registry configuration element.
     */
    private static LdapRegistry createLdapRegistry(ServerConfiguration serverConfig, String baseDN, MemberAttribute[] memberAttrs, MembershipAttribute membershipAttr) {
        /*
         * Create and add the new LDAP registry to the server configuration.
         */
        LdapRegistry ldapRegistry = new LdapRegistry();
        serverConfig.getLdapRegistries().add(ldapRegistry);

        /*
         * Configure the LDAP registry.
         */
        ldapRegistry.setBaseDN(baseDN);
        ldapRegistry.setLdapType("Custom");
        ldapRegistry.setRealm("LdapRealm");
        ldapRegistry.setHost("localhost");
        ldapRegistry.setPort(String.valueOf(ds.getLdapPort()));
        ldapRegistry.setBindDN(ds.getBindDN());
        ldapRegistry.setBindPassword(ds.getBindPassword());
        ldapRegistry.setCustomFilters(new LdapFilters("(&(uid=%v)(objectclass=inetorgperson))", "(&(cn=%v)(|(objectclass=groupofnames)(objectclass=groupofurls)))", null, null, null));
        ldapRegistry.setLdapCache(new LdapCache(new AttributesCache(false, null, null, null), new SearchResultsCache(false, null, null, null)));
        ldapRegistry.getLdapEntityTypes().add(new LdapEntityType("Group", null, new String[] { "groupofnames", "groupofurls" }, null));
        ldapRegistry.getLdapEntityTypes().add(new LdapEntityType("PersonAccount", null, new String[] { "inetorgperson" }, null));

        GroupProperties groupProperties = new GroupProperties();
        ldapRegistry.setGroupProperties(groupProperties);
        groupProperties.setDynamicMemberAttribute(new DynamicMemberAttribute("memberurl", "groupofurls"));

        if (memberAttrs != null && memberAttrs.length > 0) {
            ldapRegistry.setRecursiveSearch(true); // TODO This should happen automatically.
            for (MemberAttribute memberAttr : memberAttrs) {
                groupProperties.getMemberAttributes().add(memberAttr);
            }
        }

        if (membershipAttr != null) {
            ldapRegistry.setRecursiveSearch(true); // TODO This should happen automatically.
            groupProperties.setMembershipAttribute(membershipAttr);
        }

        return ldapRegistry;
    }

    /**
     * Test a memberAttribute whose scope is set to "direct". Direct scoped member attributes
     * only return their direct members, thus this will require nested calls to derive
     * group membership.
     *
     * @throws Exception if the test failed for some unforeseen reason.
     */
    @Test
    public void memberAttribute_scope_direct() throws Exception {

        ServerConfiguration clone = baseConfiguration.clone();
        MemberAttribute[] memberAttrs = new MemberAttribute[1];
        memberAttrs[0] = new MemberAttribute(null, "member", "groupofnames", LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP_STRING);
        createLdapRegistry(clone, MEMBER_BASE_DN, memberAttrs, null);
        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        List<String> results = urServlet.getGroupsForUser(MEMBER_LEVEL1_USER_DN);
        assertMembershipResults("With direct scope, the direct member should only be a member of the root group.", results, MEMBER_LEVEL1_GROUP_DN);

        results = urServlet.getGroupsForUser(MEMBER_LEVEL2_USER_DN);
        assertMembershipResults("With direct scope, the nested member should be a member of the root and nested group.", results, MEMBER_LEVEL1_GROUP_DN, MEMBER_LEVEL2_GROUP_DN);

        results = urServlet.getGroupsForUser(MEMBER_DYNAMIC_USER_DN);
        assertMembershipResults("With direct scope, the dynamic member should be a member of the root and dynamic group.", results, MEMBER_LEVEL1_GROUP_DN,
                                MEMBER_DYNAMIC_GROUP_DN);

        /*
         * TODO user_dynamic should be returned, but LdapAdapter.getMembersByMember() does not consider nested dynamic groups.
         * See: https://github.com/OpenLiberty/open-liberty/issues/14249
         */
        results = urServlet.getUsersForGroup(MEMBER_LEVEL1_GROUP_DN, 0).getList();
        assertMembershipResults("With direct scope, the root group should have both the direct and nested members.", results, MEMBER_LEVEL1_USER_DN, MEMBER_LEVEL2_USER_DN);

        results = urServlet.getUsersForGroup(MEMBER_LEVEL2_GROUP_DN, 0).getList();
        assertMembershipResults("With direct scope, the nested group should have only the nested member.", results, MEMBER_LEVEL2_USER_DN);

        results = urServlet.getUsersForGroup(MEMBER_DYNAMIC_GROUP_DN, 0).getList();
        assertMembershipResults("With direct scope, the dynamic group should have only the dynamic member.", results, MEMBER_DYNAMIC_USER_DN);
    }

    /**
     * Test a memberAttribute whose scope is set to "nested". Nested scoped member attributes
     * return their direct members and nested members, thus this will NOT require nested calls
     * to derive group membership.
     *
     * @throws Exception if the test failed for some unforeseen reason.
     */
    @Test
    public void memberAttribute_scope_nested() throws Exception {

        ServerConfiguration clone = baseConfiguration.clone();
        MemberAttribute[] memberAttrs = new MemberAttribute[1];
        memberAttrs[0] = new MemberAttribute(null, "member", "groupofnames", LdapConstants.LDAP_NESTED_GROUP_MEMBERSHIP_STRING);
        createLdapRegistry(clone, MEMBER_BASE_DN, memberAttrs, null);
        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        List<String> results = urServlet.getGroupsForUser(MEMBER_LEVEL1_USER_DN);
        assertMembershipResults("With nested scope, the direct member should only be a member of the root group.", results, MEMBER_LEVEL1_GROUP_DN);

        /*
         * TODO group_level1 is returned b/c recursiveSearch=true overrides search scope in LdapAdapter.getGroupsByMember()
         * See: https://github.com/OpenLiberty/open-liberty/issues/13878
         */
        results = urServlet.getGroupsForUser(MEMBER_LEVEL2_USER_DN);
        assertMembershipResults("With nested scope, the nested member should only be a member of the nested group.", results, MEMBER_LEVEL1_GROUP_DN, MEMBER_LEVEL2_GROUP_DN);

        /*
         * TODO group_level1 is returned b/c recursiveSearch=true overrides search scope in LdapAdapter.getGroupsByMember()
         * See: https://github.com/OpenLiberty/open-liberty/issues/13878
         */
        results = urServlet.getGroupsForUser(MEMBER_DYNAMIC_USER_DN);
        assertMembershipResults("With nested scope, the dynamic member should be a member of the dynamic group.", results, MEMBER_LEVEL1_GROUP_DN, MEMBER_DYNAMIC_GROUP_DN);

        /*
         * TODO user_level1 is returned b/c recursiveSearch=true overrides search scope in LdapAdapter.getMembersByMember()
         * See: https://github.com/OpenLiberty/open-liberty/issues/13878
         */
        results = urServlet.getUsersForGroup(MEMBER_LEVEL1_GROUP_DN, 0).getList();
        assertMembershipResults("With nested scope, the root group should have the direct member.", results, MEMBER_LEVEL1_USER_DN, MEMBER_LEVEL2_USER_DN);

        results = urServlet.getUsersForGroup(MEMBER_LEVEL2_GROUP_DN, 0).getList();
        assertMembershipResults("With nested scope, the nested group should have only the nested member.", results, MEMBER_LEVEL2_USER_DN);

        results = urServlet.getUsersForGroup(MEMBER_DYNAMIC_GROUP_DN, 0).getList();
        assertMembershipResults("With nested scope, the dynamic group should have only the dynamic member.", results, MEMBER_DYNAMIC_USER_DN);
    }

    /**
     * Test a memberAttribute whose scope is set to "all". All scoped member attributes
     * return their direct, nested and dynamic members, thus this will NOT require nested calls
     * to derive group membership.
     *
     * @throws Exception if the test failed for some unforeseen reason.
     */
    @Test
    public void memberAttribute_scope_all() throws Exception {

        ServerConfiguration clone = baseConfiguration.clone();
        MemberAttribute[] memberAttrs = new MemberAttribute[1];
        memberAttrs[0] = new MemberAttribute(null, "member", "groupofnames", LdapConstants.LDAP_ALL_GROUP_MEMBERSHIP_STRING);
        createLdapRegistry(clone, MEMBER_BASE_DN, memberAttrs, null);
        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        List<String> results = urServlet.getGroupsForUser(MEMBER_LEVEL1_USER_DN);
        assertMembershipResults("With all scope, the direct member should only be a member of the root group.", results, MEMBER_LEVEL1_GROUP_DN);

        /*
         * TODO group_level1 is returned b/c recursiveSearch=true overrides search scope in LdapAdapter.getGroupsByMember()
         * See: https://github.com/OpenLiberty/open-liberty/issues/13878
         */
        results = urServlet.getGroupsForUser(MEMBER_LEVEL2_USER_DN);
        assertMembershipResults("With all scope, the nested member should only be a member of the nested group.", results, MEMBER_LEVEL1_GROUP_DN, MEMBER_LEVEL2_GROUP_DN);

        /*
         * TODO group_level1 is returned b/c recursiveSearch=true overrides search scope in LdapAdapter.getGroupsByMember()
         * See: https://github.com/OpenLiberty/open-liberty/issues/13878
         */
        results = urServlet.getGroupsForUser(MEMBER_DYNAMIC_USER_DN);
        assertMembershipResults("With all scope, the dynamic member should be a member of the dynamic group.", results, MEMBER_LEVEL1_GROUP_DN, MEMBER_DYNAMIC_GROUP_DN);

        /*
         * TODO user_level1 is returned b/c recursiveSearch=true overrides search scope in LdapAdapter.getMembersByMember()
         * See: https://github.com/OpenLiberty/open-liberty/issues/13878
         */
        results = urServlet.getUsersForGroup(MEMBER_LEVEL1_GROUP_DN, 0).getList();
        assertMembershipResults("With all scope, the root group should have the direct member.", results, MEMBER_LEVEL1_USER_DN, MEMBER_LEVEL2_USER_DN);

        results = urServlet.getUsersForGroup(MEMBER_LEVEL2_GROUP_DN, 0).getList();
        assertMembershipResults("With all scope, the nested group should have only the nested member.", results, MEMBER_LEVEL2_USER_DN);

        results = urServlet.getUsersForGroup(MEMBER_DYNAMIC_GROUP_DN, 0).getList();
        assertMembershipResults("With all scope, the dynamic group should have only the dynamic member.", results, MEMBER_DYNAMIC_USER_DN);
    }

    /**
     * Test a membershipAttribute whose scope is set to "direct". Direct scoped membership attributes
     * only return their direct memberships, thus this will require nested calls to derive
     * group membership.
     *
     * @throws Exception if the test failed for some unforeseen reason.
     */
    @Test
    public void membershipAttribute_scope_direct() throws Exception {

        ServerConfiguration clone = baseConfiguration.clone();
        MembershipAttribute membershipAttr = new MembershipAttribute("memberof", LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP_STRING);
        createLdapRegistry(clone, MEMBERSHIP_BASE_DN, null, membershipAttr);
        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        List<String> results = urServlet.getGroupsForUser(MEMBERSHIP_LEVEL1_USER_DN);
        assertMembershipResults("With direct scope, the direct member should only be a member of the root group.", results, MEMBERSHIP_LEVEL1_GROUP_DN);

        results = urServlet.getGroupsForUser(MEMBERSHIP_LEVEL2_USER_DN);
        assertMembershipResults("With direct scope, the nested member should be a member of the root and nested group.", results, MEMBERSHIP_LEVEL1_GROUP_DN,
                                MEMBERSHIP_LEVEL2_GROUP_DN);

        results = urServlet.getGroupsForUser(MEMBERSHIP_DYNAMIC_USER_DN);
        assertMembershipResults("With direct scope, the dynamic member should be a member of the root and dynamic group.", results, MEMBERSHIP_DYNAMIC_GROUP_DN);

        /*
         * TODO user_dynamic should be returned, but LdapAdapter.getMembersByMembership() does not consider nested dynamic groups.
         * See: https://github.com/OpenLiberty/open-liberty/issues/14249
         */
        results = urServlet.getUsersForGroup(MEMBERSHIP_LEVEL1_GROUP_DN, 0).getList();
        assertMembershipResults("With direct scope, the root group should have both the direct and nested members.", results, MEMBERSHIP_LEVEL1_USER_DN, MEMBERSHIP_LEVEL2_USER_DN);

        results = urServlet.getUsersForGroup(MEMBERSHIP_LEVEL2_GROUP_DN, 0).getList();
        assertMembershipResults("With direct scope, the nested group should have only the nested member.", results, MEMBERSHIP_LEVEL2_USER_DN);

        /*
         * TODO LdapAdapter.getGroupsByMembership() does not consider dynamic groups. This is inconsistent with LdapAdapter.getMembersByMembership().
         * See: https://github.com/OpenLiberty/open-liberty/issues/13877
         */
        results = urServlet.getUsersForGroup(MEMBERSHIP_DYNAMIC_GROUP_DN, 0).getList();
        assertMembershipResults("With direct scope, the dynamic group should have only the dynamic member.", results);
    }

    /**
     * Test a membershipAttribute whose scope is set to "nested". Nested scoped membership attributes
     * return their direct members and nested memberships, thus this will NOT require nested calls
     * to derive group membership.
     *
     * @throws Exception if the test failed for some unforeseen reason.
     */
    @Test
    public void membershipAttribute_scope_nested() throws Exception {

        ServerConfiguration clone = baseConfiguration.clone();
        MembershipAttribute membershipAttr = new MembershipAttribute("memberof", LdapConstants.LDAP_NESTED_GROUP_MEMBERSHIP_STRING);
        createLdapRegistry(clone, MEMBERSHIP_BASE_DN, null, membershipAttr);
        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        List<String> results = urServlet.getGroupsForUser(MEMBERSHIP_LEVEL1_USER_DN);
        assertMembershipResults("With nested scope, the direct member should only be a member of the root group.", results, MEMBERSHIP_LEVEL1_GROUP_DN);

        /*
         * TODO group_level1 is returned b/c recursiveSearch=true overrides search scope in LdapAdapter.getGroupsByMembership()
         * See: https://github.com/OpenLiberty/open-liberty/issues/13878
         */
        results = urServlet.getGroupsForUser(MEMBERSHIP_LEVEL2_USER_DN);
        assertMembershipResults("With nested scope, the nested member should only be a member of the nested group.", results, MEMBERSHIP_LEVEL1_GROUP_DN,
                                MEMBERSHIP_LEVEL2_GROUP_DN);

        results = urServlet.getGroupsForUser(MEMBERSHIP_DYNAMIC_USER_DN);
        assertMembershipResults("With nested scope, the dynamic member should be a member of the dynamic group.", results, MEMBERSHIP_DYNAMIC_GROUP_DN);

        /*
         * TODO user_level2 is returned b/c recursiveSearch=true overrides search scope in LdapAdapter.getMembersByMembership()
         * See: https://github.com/OpenLiberty/open-liberty/issues/13878
         */
        results = urServlet.getUsersForGroup(MEMBERSHIP_LEVEL1_GROUP_DN, 0).getList();
        assertMembershipResults("With nested scope, the root group should have the direct member.", results, MEMBERSHIP_LEVEL1_USER_DN, MEMBERSHIP_LEVEL2_USER_DN);

        results = urServlet.getUsersForGroup(MEMBERSHIP_LEVEL2_GROUP_DN, 0).getList();
        assertMembershipResults("With nested scope, the nested group should have only the nested member.", results, MEMBERSHIP_LEVEL2_USER_DN);

        /*
         * TODO LdapAdapter.getGroupsByMembership() does not consider dynamic groups. This is inconsistent with LdapAdapter.getMembersByMembership().
         * See: https://github.com/OpenLiberty/open-liberty/issues/13877
         */
        results = urServlet.getUsersForGroup(MEMBERSHIP_DYNAMIC_GROUP_DN, 0).getList();
        assertMembershipResults("With nested scope, the dynamic group should have only the dynamic member.", results);
    }

    /**
     * Test a membershipAttribute whose scope is set to "all". All scoped membership attributes
     * return their direct, nested and dynamic memberships, thus this will NOT require nested calls
     * to derive group membership.
     *
     * @throws Exception if the test failed for some unforeseen reason.
     */
    @Test
    public void membershipAttribute_scope_all() throws Exception {

        ServerConfiguration clone = baseConfiguration.clone();
        MembershipAttribute membershipAttr = new MembershipAttribute("memberof", LdapConstants.LDAP_ALL_GROUP_MEMBERSHIP_STRING);
        createLdapRegistry(clone, MEMBERSHIP_BASE_DN, null, membershipAttr);
        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        List<String> results = urServlet.getGroupsForUser(MEMBERSHIP_LEVEL1_USER_DN);
        assertMembershipResults("With all scope, the direct member should only be a member of the root group.", results, MEMBERSHIP_LEVEL1_GROUP_DN);

        /*
         * TODO group_level1 is returned b/c recursiveSearch=true overrides search scope in LdapAdapter.getGroupsByMembership()
         * See: https://github.com/OpenLiberty/open-liberty/issues/13878
         */
        results = urServlet.getGroupsForUser(MEMBERSHIP_LEVEL2_USER_DN);
        assertMembershipResults("With all scope, the nested member should only be a member of the nested group.", results, MEMBERSHIP_LEVEL1_GROUP_DN, MEMBERSHIP_LEVEL2_GROUP_DN);

        results = urServlet.getGroupsForUser(MEMBERSHIP_DYNAMIC_USER_DN);
        assertMembershipResults("With all scope, the dynamic member should be a member of the dynamic group.", results, MEMBERSHIP_DYNAMIC_GROUP_DN);

        /*
         * TODO user_level2 is returned b/c recursiveSearch=true overrides search scope in LdapAdapter.getMembersByMembership()
         * See: https://github.com/OpenLiberty/open-liberty/issues/13878
         */
        results = urServlet.getUsersForGroup(MEMBERSHIP_LEVEL1_GROUP_DN, 0).getList();
        assertMembershipResults("With all scope, the root group should have the direct member.", results, MEMBERSHIP_LEVEL1_USER_DN, MEMBERSHIP_LEVEL2_USER_DN);

        results = urServlet.getUsersForGroup(MEMBERSHIP_LEVEL2_GROUP_DN, 0).getList();
        assertMembershipResults("With all scope, the nested group should have only the nested member.", results, MEMBERSHIP_LEVEL2_USER_DN);

        /*
         * TODO LdapAdapter.getGroupsByMembership() does not consider dynamic groups. This is inconsistent with LdapAdapter.getMembersByMembership().
         * See: https://github.com/OpenLiberty/open-liberty/issues/13877
         */
        results = urServlet.getUsersForGroup(MEMBERSHIP_DYNAMIC_GROUP_DN, 0).getList();
        assertMembershipResults("With all scope, the dynamic group should have only the dynamic member.", results);
    }

    /**
     * Assert that group membership is as expected.
     *
     * @param message The message to print on failure.
     * @param actual The actual members or groups.
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
