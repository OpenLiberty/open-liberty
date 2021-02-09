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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapFilters;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;
import com.unboundid.ldap.sdk.Entry;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
public class URAPIs_IDMapping_Test {

    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.ldap.fat.id.mapping");
    private static final Class<?> c = URAPIs_IDMapping_Test.class;
    private static UserRegistryServletConnection servlet;

    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
    private static ServerConfiguration emptyConfiguration = null;

    private static InMemoryLDAPServer ds;
    private static final String BASE_DN = "o=ibm,c=us";

    private static final String RESIDENTIALPERSON_CN = "residentialperson_cn";
    private static final String RESIDENTIALPERSON_SN = "residentialperson_sn";
    private static final String RESIDENTIALPERSON_L = "residentialperson_l";
    private static final String RESIDENTIALPERSON_DN = "cn=" + RESIDENTIALPERSON_CN + "," + BASE_DN;

    private static final String INETORGPERSON_CN = "inetorgperson_cn";
    private static final String INETORGPERSON_SN = "inetorgperson_sn";
    private static final String INETORGPERSON_UID = "inetorgperson_uid";
    private static final String INETORGPERSON_DN = "uid=" + INETORGPERSON_UID + "," + BASE_DN;

    private static final String GROUPOFNAMES_CN = "groupofnames_cn";
    private static final String GROUPOFNAMES_OU = "groupofnames_ou";
    private static final String GROUPOFNAMES_O = "groupofnames_o";
    private static final String GROUPOFNAMES_DN = "cn=" + GROUPOFNAMES_CN + "," + BASE_DN;

    private static final String GROUPOFUNIQUENAMES_CN = "groupofuniquenames_cn";
    private static final String GROUPOFUNIQUENAMES_OU = "groupofuniquenames_ou";
    private static final String GROUPOFUNIQUENAMES_O = "groupofuniquenames_o";
    private static final String GROUPOFUNIQUENAMES_DN = "cn=" + GROUPOFUNIQUENAMES_CN + "," + BASE_DN;

    /**
     * Setup the test case.
     *
     * @throws Exception If the setup failed for some reason.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        setupLibertyServer();
        setupLDAPServer();
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
            try {
                if (ds != null) {
                    ds.shutDown(true);
                }
            } catch (Exception e) {
                Log.error(c, "teardown", e, "LDAP server threw error while shutting down. " + e.getMessage());
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
     * Configure the ldap server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLDAPServer() throws Exception {
        ds = new InMemoryLDAPServer(BASE_DN);

        Entry entry = new Entry(BASE_DN);
        entry.addAttribute("objectclass", "top");
        entry.addAttribute("objectclass", "domain");
        ds.add(entry);

        /*
         * Create the users and groups.
         */
        entry = new Entry(RESIDENTIALPERSON_DN);
        entry.addAttribute("objectclass", "residentialperson");
        entry.addAttribute("sn", RESIDENTIALPERSON_SN);
        entry.addAttribute("cn", RESIDENTIALPERSON_CN);
        entry.addAttribute("l", RESIDENTIALPERSON_L);
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

        entry = new Entry(INETORGPERSON_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", INETORGPERSON_UID);
        entry.addAttribute("sn", INETORGPERSON_SN);
        entry.addAttribute("cn", INETORGPERSON_CN);
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

        entry = new Entry(GROUPOFNAMES_DN);
        entry.addAttribute("objectclass", "groupofnames");
        entry.addAttribute("cn", GROUPOFNAMES_CN);
        entry.addAttribute("ou", GROUPOFNAMES_OU);
        entry.addAttribute("o", GROUPOFNAMES_O);
        entry.addAttribute("member", RESIDENTIALPERSON_DN);
        entry.addAttribute("member", INETORGPERSON_DN);
        ds.add(entry);

        entry = new Entry(GROUPOFUNIQUENAMES_DN);
        entry.addAttribute("objectclass", "groupofuniquenames");
        entry.addAttribute("cn", GROUPOFUNIQUENAMES_CN);
        entry.addAttribute("ou", GROUPOFUNIQUENAMES_OU);
        entry.addAttribute("o", GROUPOFUNIQUENAMES_O);
        entry.addAttribute("uniquemember", RESIDENTIALPERSON_DN);
        entry.addAttribute("uniquemember", INETORGPERSON_DN);
        ds.add(entry);
    }

    public void updateLibertyServer(String userIdMap, String groupIdMap, String groupMemberIdMap) throws Exception {

        ServerConfiguration server = emptyConfiguration.clone();

        LdapRegistry ldap = new LdapRegistry();

        ldap.setRealm("LDAPRealm");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds.getLdapPort()));
        ldap.setBaseDN(BASE_DN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");

        String userFilter = "(&(|(objectclass=inetorgperson)(objectclass=residentialperson))(|(uid=%v)(cn=%v)))";
        String groupFilter = "(&(|(objectclass=groupofnames)(objectclass=groupofuniquenames))(cn=%v))";
        ldap.setCustomFilters(new LdapFilters(userFilter, groupFilter, userIdMap, groupIdMap, groupMemberIdMap));

        server.getLdapRegistries().add(ldap);
        updateConfigDynamically(libertyServer, server);
    }

    /**
     * Test when the userIdMap is NOT set and therefore the default userDisplayName output mapping property
     * ('principalName') should take precedence.
     *
     * @throws Exception If the test fails for some unforeseen reason.
     */
    @Test
    public void userIdMap_Default() throws Exception {
        updateLibertyServer(null, null, null);

        assertEquals(RESIDENTIALPERSON_L, servlet.getUserDisplayName(RESIDENTIALPERSON_CN));
        assertEquals(INETORGPERSON_UID, servlet.getUserDisplayName(INETORGPERSON_UID));
    }

    /**
     * Test when the userIdMap has all classes mapped to use "sn" using the wildcard ("*").
     *
     * @throws Exception If the test fails for some unforeseen reason.
     *
     */
    @Test
    public void userIdMap_Wildcard() throws Exception {
        updateLibertyServer("*:sn", null, null);

        assertEquals(RESIDENTIALPERSON_SN, servlet.getUserDisplayName(RESIDENTIALPERSON_CN));
        assertEquals(INETORGPERSON_SN, servlet.getUserDisplayName(INETORGPERSON_UID));
    }

    /**
     * Test when the userIdMap has multiple object classes with different mappings.
     *
     * @throws Exception
     */
    @Test
    public void userIdMap_ClassSpecific() throws Exception {
        updateLibertyServer("inetorgperson:sn;eperson:cn", null, null);

        assertEquals(RESIDENTIALPERSON_CN, servlet.getUserDisplayName(RESIDENTIALPERSON_CN));
        assertEquals(INETORGPERSON_SN, servlet.getUserDisplayName(INETORGPERSON_UID));
    }

    /**
     * Test when the groupIdMap is NOT set and therefore the default groupDisplayName output mapping property
     * ('cn') should take precedence.
     *
     * @throws Exception If the test fails for some unforeseen reason.
     */
    @Test
    public void groupIdMap_Default() throws Exception {
        updateLibertyServer(null, null, null);

        assertEquals(GROUPOFNAMES_CN, servlet.getGroupDisplayName(GROUPOFNAMES_CN));
        assertEquals(GROUPOFUNIQUENAMES_CN, servlet.getGroupDisplayName(GROUPOFUNIQUENAMES_CN));
    }

    /**
     * Test when the groupIdMap has all classes mapped to use "ou" using the wildcard ("*").
     *
     * @throws Exception If the test fails for some unforeseen reason.
     */
    @Test
    public void groupIdMap_Wildcard() throws Exception {
        updateLibertyServer(null, "*:ou", null);

        assertEquals(GROUPOFNAMES_OU, servlet.getGroupDisplayName(GROUPOFNAMES_CN));
        assertEquals(GROUPOFUNIQUENAMES_OU, servlet.getGroupDisplayName(GROUPOFUNIQUENAMES_CN));
    }

    /**
     * Test when the groupIdMap has multiple object classes with different mappings.
     *
     * @throws Exception If the test fails for some unforeseen reason.
     */
    @Test
    public void groupIdMap_ClassSpecific() throws Exception {
        updateLibertyServer(null, "groupofnames:ou;groupofuniquenames:o", null);

        assertEquals(GROUPOFNAMES_OU, servlet.getGroupDisplayName(GROUPOFNAMES_CN));
        assertEquals(GROUPOFUNIQUENAMES_O, servlet.getGroupDisplayName(GROUPOFUNIQUENAMES_CN));
    }

    /**
     * Test when the groupMemberIdMap is NOT set and therefore the default userDisplayName output mapping property
     * ('principalName') should take precedence.
     *
     * @throws Exception If the test fails for some unforeseen reason.
     */
    @Test
    public void groupMemberIdMap_Default() throws Exception {
        updateLibertyServer(null, null, null);

        List<String> groups = servlet.getGroupsForUser(RESIDENTIALPERSON_CN);
        assertEquals("Unexpected number of groups returned. Groups were: " + groups, 0, groups.size());

        groups = servlet.getGroupsForUser(INETORGPERSON_UID);
        assertEquals("Unexpected number of groups returned. Groups were: " + groups, 0, groups.size());

    }

    /**
     * Test when the groupMemberIdMap has all classes mapped to use "ou" using the wildcard ("*").
     *
     * @throws Exception If the test fails for some unforeseen reason.
     */
    @Test
    public void groupMemberIdMap_Wildcard() throws Exception {
        updateLibertyServer(null, null, "*:member");

        List<String> groups = servlet.getGroupsForUser(RESIDENTIALPERSON_CN);
        assertEquals("Unexpected number of groups returned. Groups were: " + groups, 1, groups.size());
        assertTrue("Missing group member. Received: " + groups, groups.contains(GROUPOFNAMES_CN));

        groups = servlet.getGroupsForUser(INETORGPERSON_UID);
        assertEquals("Unexpected number of groups returned. Groups were: " + groups, 1, groups.size());
        assertTrue("Missing group member. Received: " + groups, groups.contains(GROUPOFNAMES_CN));
    }

    /**
     * Test when the groupMemberIdMap has multiple object classes with different mappings.
     *
     * @throws Exception If the test fails for some unforeseen reason.
     */
    @Test
    public void groupMemberIdMap_ClassSpecific() throws Exception {
        updateLibertyServer(null, null, "groupofnames:member;groupofuniquenames:uniquemember");

        List<String> groups = servlet.getGroupsForUser(RESIDENTIALPERSON_CN);
        assertEquals("Unexpected number of groups returned. Groups were: " + groups, 2, groups.size());
        assertTrue("Missing group member. Received: " + groups, groups.contains(GROUPOFNAMES_CN));
        assertTrue("Missing group member. Received: " + groups, groups.contains(GROUPOFUNIQUENAMES_CN));

        groups = servlet.getGroupsForUser(INETORGPERSON_UID);
        assertEquals("Unexpected number of groups returned. Groups were: " + groups, 2, groups.size());
        assertTrue("Missing group member. Received: " + groups, groups.contains(GROUPOFNAMES_CN));
        assertTrue("Missing group member. Received: " + groups, groups.contains(GROUPOFUNIQUENAMES_CN));
    }
}
