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

import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapEntityType;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;
import com.unboundid.ldap.sdk.Entry;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPFatUtils;
import componenttest.topology.utils.LDAPUtils;

/**
 * A user wanted to have users and groups in different BaseDNs. Make sure
 * we can use 2 search bases and exclude users from other BaseDNs.
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class URAPIs_UserGroupSearchBases {

    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.ldap.fat.user.group.search.bases");
    private static final Class<?> c = URAPIs_UserGroupSearchBases.class;
    private static UserRegistryServletConnection servlet;

    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
    private static ServerConfiguration emptyConfiguration = null;

    //private static InMemoryDirectoryServerConfig config;
    //private static InMemoryDirectoryServer ds;
    static InMemoryLDAPServer ds;
    private static final String SUB_DN = "o=ibm,c=us";
    private static final String USER_BASE_DN = "ou=TestUsers,ou=Test,o=ibm,c=us";
    private static final String GROUP_BASE_DN = "ou=DevGroups,ou=Dev,o=ibm,c=us";
    private static final String USER = "user1";
    private static final String USER_DN = "uid=" + USER + "," + USER_BASE_DN;
    private static final String GROUP = "group1";
    private static final String GROUP_DN = "cn=" + GROUP + "," + GROUP_BASE_DN;

    private static final String BAD_USER_BASE_DN = "ou=BadUsers,o=ibm,c=us";
    private static final String BAD_USER = "baduser1";
    private static final String BAD_USER_DN = "uid=" + BAD_USER + "," + BAD_USER_BASE_DN;

    /**
     * Setup the test case.
     *
     * @throws Exception If the setup failed for some reason.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        setupLibertyServer();
        setupldapServer();
        updateLibertyServer();
    }

    /**
     * Tear down the test.
     */
    @AfterClass
    public static void teardownClass() throws Exception {
        try {
            if (libertyServer != null) {
                libertyServer.stopServer("CWIML4537E");
            }
        } finally {
            if (ds != null) {
                ds.shutDown(true);
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
     * Configure the embedded LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupldapServer() throws Exception {

        ds = new InMemoryLDAPServer(SUB_DN);

        Entry entry = new Entry(SUB_DN);
        entry.addAttribute("objectclass", "top");
        entry.addAttribute("objectclass", "domain");
        ds.add(entry);
        /*
         * Add the partition entries.
         */
        entry = new Entry("ou=Test,o=ibm,c=us");
        entry.addAttribute("objectclass", "organizationalunit");
        entry.addAttribute("ou", "Test");
        ds.add(entry);

        entry = new Entry(USER_BASE_DN);
        entry.addAttribute("objectclass", "organizationalunit");
        entry.addAttribute("ou", "Test");
        entry.addAttribute("ou", "TestUsers");
        ds.add(entry);

        entry = new Entry(BAD_USER_BASE_DN);
        entry.addAttribute("objectclass", "organizationalunit");
        entry.addAttribute("ou", "BadUsers");
        ds.add(entry);

        entry = new Entry("ou=Dev,o=ibm,c=us");
        entry.addAttribute("objectclass", "organizationalunit");
        entry.addAttribute("ou", "Dev");
        ds.add(entry);

        entry = new Entry(GROUP_BASE_DN);
        entry.addAttribute("objectclass", "organizationalunit");
        entry.addAttribute("ou", "Dev");
        entry.addAttribute("ou", "DevGroups");
        ds.add(entry);

        /*
         * Create the user and group.
         */
        entry = new Entry(USER_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", USER);
        entry.addAttribute("sn", USER);
        entry.addAttribute("cn", USER);
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

        entry = new Entry(BAD_USER_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", BAD_USER);
        entry.addAttribute("sn", BAD_USER);
        entry.addAttribute("cn", BAD_USER);
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

        entry = new Entry(GROUP_DN);
        entry.addAttribute("objectclass", "groupofnames");
        entry.addAttribute("cn", GROUP);
        entry.addAttribute("member", USER_DN);
        entry.addAttribute("member", BAD_USER_DN);
        ds.add(entry);
    }

    /**
     * Convenience method to configure the Liberty server with an {@link LdapRegistry} configuration that
     * will connect to {@link #ldapServer}.
     *
     * @throws Exception If there was an error configuring the server.
     */
    private static void updateLibertyServer() throws Exception {
        ServerConfiguration server = emptyConfiguration.clone();

        LdapRegistry ldap = new LdapRegistry();
        ldap.setId("ldap1");
        ldap.setRealm("LDAPRealm");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds.getLdapPort()));
        ldap.setBaseDN(SUB_DN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");
        LdapEntityType type1 = new LdapEntityType("Group", null, new String[] { "groupOfNames" }, new String[] { GROUP_BASE_DN });
        LdapEntityType type2 = new LdapEntityType("PersonAccount", null, new String[] { "inetOrgPerson" }, new String[] { USER_BASE_DN });
        ldap.getLdapEntityTypes().add(type1);
        ldap.getLdapEntityTypes().add(type2);

        LDAPFatUtils.createFederatedRepository(server, "LDAPRealmFed", new String[] { SUB_DN });

        server.getLdapRegistries().add(ldap);

        updateConfigDynamically(libertyServer, server);
    }

    /**
     *
     * @throws Exception If there was an unexpected exception.
     */
    @Test
    public void testSearchBase() throws Exception {

        // check for valid user and group
        String returnUser = servlet.checkPassword(USER_DN, "password");
        assertNotNull("Should find user on checkPassword using " + USER_DN, returnUser);

        assertTrue("Expected '" + USER_DN + "' to be valid user.", servlet.isValidUser(USER_DN));

        List<String> groups = servlet.getGroupsForUser(USER_DN);
        assertFalse("Should have found groups", groups.isEmpty());
        assertTrue("Group should include " + GROUP + " returned " + groups, groups.contains(GROUP_DN));

        returnUser = servlet.getUniqueUserId(USER);
        assertNotNull("Should find user " + USER, returnUser);
        assertEquals("Wrong unique ID returned for " + USER, USER_DN, returnUser);

        // check that the bad user is excluded
        returnUser = servlet.checkPassword(BAD_USER_DN, "password");
        assertNull(BAD_USER + " should not be able to login", returnUser);

        assertFalse(BAD_USER + " should not be a valid user", servlet.isValidUser(BAD_USER_DN));

        try {
            returnUser = servlet.getUniqueUserId(BAD_USER);
            fail("Should not find user " + BAD_USER);
        } catch (Exception e) {
            //  expected
        }

    }

}