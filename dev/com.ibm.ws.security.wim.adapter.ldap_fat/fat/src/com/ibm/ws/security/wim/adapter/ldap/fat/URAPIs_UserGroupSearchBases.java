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
import static org.junit.Assert.assertFalse;
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
import com.ibm.websphere.simplicity.config.wim.LdapEntityType;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

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

    private static EmbeddedApacheDS ldapServer = null;
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
        if (libertyServer != null) {
            try {
                libertyServer.stopServer();
            } catch (Exception e) {
                Log.error(c, "teardown", e, "Liberty server threw error while stopping. " + e.getMessage());
            }
        }
        if (ldapServer != null) {
            try {
                ldapServer.stopService();
            } catch (Exception e) {
                Log.error(c, "teardown", e, "LDAP server threw error while stopping. " + e.getMessage());
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
        ldapServer = new EmbeddedApacheDS("myLDAP");
        ldapServer.addPartition("users", USER_BASE_DN);
        ldapServer.addPartition("groups", GROUP_BASE_DN);
        ldapServer.addPartition("groups", BAD_USER_BASE_DN);
        ldapServer.startServer();

        /*
         * Add the partition entries.
         */
        Entry entry = ldapServer.newEntry(USER_BASE_DN);
        entry.add("objectclass", "organizationalunit");
        entry.add("ou", "Test");
        entry.add("ou", "TestUsers");
        ldapServer.add(entry);

        entry = ldapServer.newEntry(BAD_USER_BASE_DN);
        entry.add("objectclass", "organizationalunit");
        ldapServer.add(entry);

        entry = ldapServer.newEntry(GROUP_BASE_DN);
        entry.add("objectclass", "organizationalunit");
        entry.add("ou", "Dev");
        entry.add("ou", "DevGroups");
        ldapServer.add(entry);

        /*
         * Create the user and group.
         */
        entry = ldapServer.newEntry(USER_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", USER);
        entry.add("sn", USER);
        entry.add("cn", USER);
        entry.add("userPassword", "password");
        ldapServer.add(entry);

        entry = ldapServer.newEntry(BAD_USER_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", BAD_USER);
        entry.add("sn", BAD_USER);
        entry.add("cn", BAD_USER);
        entry.add("userPassword", "password");
        ldapServer.add(entry);

        entry = ldapServer.newEntry(GROUP_DN);
        entry.add("objectclass", "groupofnames");
        entry.add("cn", GROUP);
        entry.add("member", USER_DN);
        entry.add("member", BAD_USER_DN);
        ldapServer.add(entry);
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
        ldap.setPort(String.valueOf(ldapServer.getLdapServer().getPort()));
        ldap.setBaseDN(SUB_DN);
        ldap.setBindDN(EmbeddedApacheDS.getBindDN());
        ldap.setBindPassword(EmbeddedApacheDS.getBindPassword());
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