/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.AttributesCache;
import com.ibm.websphere.simplicity.config.wim.BaseEntry;
import com.ibm.websphere.simplicity.config.wim.FederatedRepository;
import com.ibm.websphere.simplicity.config.wim.GroupProperties;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapEntityType;
import com.ibm.websphere.simplicity.config.wim.LdapFilters;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.MemberAttribute;
import com.ibm.websphere.simplicity.config.wim.MembershipAttribute;
import com.ibm.websphere.simplicity.config.wim.RdnProperty;
import com.ibm.websphere.simplicity.config.wim.Realm;
import com.ibm.websphere.simplicity.config.wim.RealmPropertyMapping;
import com.ibm.websphere.simplicity.config.wim.SearchResultsCache;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SuppressWarnings("restriction")
@Ignore("Ignore this test until remote LDAP tests are re-enabled.")
public class FATTestIDS_allIbmGroups {

    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.ids.allIbmGroups");
    private static final Class<?> c = FATTestIDS_allIbmGroups.class;
    private static UserRegistryServletConnection servlet;
    private static ServerConfiguration emptyConfiguration = null;
    private static EmbeddedApacheDS ldapServer = null;

    private static final String LDAP_PARTITION = "dc=domain,dc=com";
    private static final String USER_1 = "user1";
    private static final String USER_1_DN = "cn=" + USER_1 + ",ou=users," + LDAP_PARTITION;
    private static final String GROUP_1 = "group1";
    private static final String GROUP_1_DN = "cn=" + GROUP_1 + ",ou=groups," + LDAP_PARTITION;
    private static final String GROUP_4 = "group4";

    /**
     * Setup the test case.
     *
     * @throws Exception If the setup failed for some reason.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        setupLibertyServer();
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
     * Convenience method to configure the Liberty server with an {@link LdapRegistry} configuration that
     * will connect to {@link #ldapServer}.
     *
     * @throws Exception If there was an error configuring the server.
     */
    private static void updateLibertyServer(boolean useFilters) throws Exception {
        ServerConfiguration server = emptyConfiguration.clone();

        LdapRegistry ldap = new LdapRegistry();
        server.getLdapRegistries().add(ldap);

        ldap.setRealm("LDAPRealm");
        ldap.setHost("HOST"); // TODO Configure TDS LDAP server.
        ldap.setPort("PORT"); // TODO Configure TDS LDAP server.
        ldap.setBindDN("BINDDN"); // TODO Configure TDS LDAP server.
        ldap.setBindPassword("PASSWORD");// TODO Configure TDS LDAP server.
        ldap.setLdapType("IBM TIVOLI DIRECTORY SERVER");
        ldap.setBaseDN(LDAP_PARTITION);

        /*
         * Disable the cache.
         */
        ldap.setLdapCache(new LdapCache(new AttributesCache(false, 0, 0, "0s"), new SearchResultsCache(false, 0, 0, "0s")));

        if (useFilters) {
            /*
             * Configure group membership.
             *
             * TODO If we don't use the userIdMap, when we get the user back from getUsersForGroup, the user's DN has the first
             * RDN name replaced. For example, cn=user1 becomes uid=user1.
             */
            ldap.setIdsFilters(new LdapFilters("(&(uid=%v)(objectclass=inetorgperson))", "(&(cn=%v)(objectclass=groupofnames))", "*:cn", null, "groupOfNames:ibm-allmembers;personaccount:ibm-allgroups"));

        } else {
            /*
             * Configure LDAP entities.
             *
             * getUsersForGroup will fail without adding 'cn' to the PersonAccount entity. The DN for user1
             * starts with cn and there is a check to make sure that the DN starts with a valid RDN.
             *
             * 'rdnProperty' is a private configuration item, customers could not use this to work around...
             */
            LdapEntityType pa = new LdapEntityType("PersonAccount", null, new String[] { "inetorgperson" }, null);
            pa.getRdnProperties().add(new RdnProperty("cn", new String[] { "inetorgperson" }));
            ldap.getLdapEntityTypes().add(pa);
            ldap.getLdapEntityTypes().add(new LdapEntityType("Group", null, new String[] { "groupofnames" }, null));

            /*
             * Configure group membership.
             */
            GroupProperties groupProperties = new GroupProperties();
            groupProperties.setMemberAttribute(new MemberAttribute(null, "ibm-allmembers", "groupofnames", "all"));
            groupProperties.setMembershipAttribute(new MembershipAttribute("ibm-allgroups", "all"));
            ldap.setGroupProperties(groupProperties);
        }

        /*
         * Configure federated repositories to return 'cn' for groups.
         */
        FederatedRepository federatedRepository = new FederatedRepository();
        Realm primaryRealm = new Realm();
        primaryRealm.setName("FederatedRealm");
        primaryRealm.getParticipatingBaseEntries().add(new BaseEntry(LDAP_PARTITION));
        primaryRealm.setGroupSecurityNameMapping(new RealmPropertyMapping("cn", "cn"));
        federatedRepository.setPrimaryRealm(primaryRealm);
        server.setFederatedRepositoryElement(federatedRepository);

        updateConfigDynamically(libertyServer, server);
    }

    @Test
    public void getUniqueGroupIdsForUser1() throws Exception {
        updateLibertyServer(false);

        List<String> results = servlet.getUniqueGroupIdsForUser(USER_1);
        assertEquals("Unexpected number of groups returned.", 2, results.size());
        assertTrue("Missing group1 from results. Results: " + results.toString(), results.contains("cn=group1,ou=groups,dc=domain,DC=COM"));
        assertTrue("Missing group1 from results. Results: " + results.toString(), results.contains("o=group4,ou=groups,dc=domain,DC=COM"));
    }

    @Test
    public void getUniqueGroupIdsForUser2() throws Exception {
        updateLibertyServer(true);

        List<String> results = servlet.getUniqueGroupIdsForUser(USER_1);
        assertEquals("Unexpected number of groups returned.", 2, results.size());
        assertTrue("Missing group1 from results. Results: " + results.toString(), results.contains("cn=group1,ou=groups,dc=domain,DC=COM"));
        assertTrue("Missing group4 from results. Results: " + results.toString(), results.contains("o=group4,ou=groups,dc=domain,DC=COM"));
    }

    @Test
    public void getGroupsForUser1() throws Exception {
        updateLibertyServer(false);

        List<String> results = servlet.getGroupsForUser(USER_1_DN);
        assertEquals("Unexpected number of groups returned.", 2, results.size());
        assertTrue("Missing group1 from results. Results: " + results.toString(), results.contains(GROUP_1));
        assertTrue("Missing group4 from results. Results: " + results.toString(), results.contains(GROUP_4));
    }

    @Test
    public void getGroupsForUser2() throws Exception {
        updateLibertyServer(true);

        List<String> results = servlet.getGroupsForUser(USER_1_DN);
        assertEquals("Unexpected number of groups returned.", 2, results.size()); // FAILS WITHOUT FIX TO GETMEMBERSBYOPERATIONALATTRIBUTE SINCE 'CN' IS NOT RETRIEVED
        assertTrue("Missing group1 from results. Results: " + results.toString(), results.contains(GROUP_1));
        assertTrue("Missing group4 from results. Results: " + results.toString(), results.contains(GROUP_4));
    }

    @Test
    public void getGroupSecurityName1() throws Exception {
        updateLibertyServer(false);

        String result = servlet.getGroupSecurityName(GROUP_1);
        assertEquals("Wrong group security name returned.", GROUP_1, result);
    }

    @Test
    public void getGroupSecurityName2() throws Exception {
        updateLibertyServer(true);

        String result = servlet.getGroupSecurityName(GROUP_1);
        assertEquals("Wrong group security name returned.", GROUP_1, result);
    }

    @Test
    public void getGroupDisplayName1() throws Exception {
        updateLibertyServer(false);

        String result = servlet.getGroupDisplayName(GROUP_1);
        assertEquals("Wrong group display name returned.", GROUP_1, result);
    }

    @Test
    public void getGroupDisplayName2() throws Exception {
        updateLibertyServer(true);

        String result = servlet.getGroupDisplayName(GROUP_1);
        assertEquals("Wrong group display name returned.", GROUP_1, result);
    }

    @Test
    public void getUniqueGroupId1() throws Exception {
        updateLibertyServer(false);

        String result = servlet.getUniqueGroupId(GROUP_1);
        assertEquals("Wrong unique group ID returned.", GROUP_1_DN, result);
    }

    @Test
    public void getUniqueGroupId2() throws Exception {
        updateLibertyServer(true);

        String result = servlet.getUniqueGroupId(GROUP_1);
        assertEquals("Wrong unique group ID returned.", GROUP_1_DN, result);
    }

    @Test
    public void getUsersForGroup1() throws Exception {
        updateLibertyServer(false);

        List<String> results = servlet.getUsersForGroup(GROUP_1, 0).getList();
        assertEquals("Unexpected number of users returned.", 1, results.size());
        assertTrue("Missing user1 from results. Results: " + results, results.contains(USER_1_DN));
    }

    @Test
    public void getUsersForGroup2() throws Exception {
        updateLibertyServer(true);

        List<String> results = servlet.getUsersForGroup(GROUP_1, 0).getList();
        assertEquals("Unexpected number of users returned.", 1, results.size());
        assertTrue("Missing user1 from results. Results: " + results, results.contains(USER_1_DN));
    }
}