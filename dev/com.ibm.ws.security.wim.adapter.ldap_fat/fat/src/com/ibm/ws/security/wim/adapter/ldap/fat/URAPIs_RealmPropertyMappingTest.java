/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static componenttest.topology.utils.LDAPFatUtils.createFederatedRepository;
import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;

import com.ibm.websphere.simplicity.config.BasicRegistry;
import com.ibm.websphere.simplicity.config.BasicRegistry.Group;
import com.ibm.websphere.simplicity.config.BasicRegistry.User;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.AttributesCache;
import com.ibm.websphere.simplicity.config.wim.FederatedRepository;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.RealmPropertyMapping;
import com.ibm.websphere.simplicity.config.wim.SearchResultsCache;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;
import com.unboundid.ldap.sdk.Entry;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

// TODO MOVE TO REGISTRY FATS.
// TODO Create defect for customRegistrySample-1.0, as it should be used as a user feature.

/**
 * Test realm WIMUserRegistry input / output property mappings.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class URAPIs_RealmPropertyMappingTest {

    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.realm.mapping");
    private static final Class<?> c = URAPIs_RealmPropertyMappingTest.class;
    private static UserRegistryServletConnection servlet;

    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
    private static ServerConfiguration EMPTY_SERVER_CONFIGURATION = null;

    private static InMemoryLDAPServer ds;

    // LDAP registry variables.
    private static final String LDAP_BASE_DN = "o=ldap";
    private static final String LDAP_USER_UID = "ldap_user";
    private static final String LDAP_USER_CN = "ldap_usercn";
    private static final String LDAP_USER_SN = "ldap_usersn";
    private static final String LDAP_USER_DN = "uid=" + LDAP_USER_UID + "," + LDAP_BASE_DN;
    private static final String LDAP_GROUP_CN = "ldap_group";
    private static final String LDAP_GROUP_DN = "cn=" + LDAP_GROUP_CN + "," + LDAP_BASE_DN;

    // Basic registry variables.
    private static final String BASIC_BASE_DN = "o=BasicRealm"; // o=<REALMNAME>
    private static final String BASIC_USER = "basic_user";
    private static final String BASIC_GROUP = "basic_group";

    // Custom UserRegistry variables.
    private static final String CUR_BASE_DN = "o=customRealm"; // o=<REALMNAME>
    private static final String CUR_USER_NAME = "cur_user";
    private static final String CUR_USER_UID = "cur_userid";
    private static final String CUR_GROUP_NAME = "cur_group";
    private static final String CUR_GROUP_GID = "567";

    // CustomRepository variables.
    private static final String CR_BASE_DN = "o=ibm,c=us";
    private static final String CR_USER_CN = "adminUser";
    private static final String CR_USER_DN = "cn=" + CR_USER_CN + "," + CR_BASE_DN;
    private static final String CR_GROUP_CN = "adminGroup";
    private static final String CR_GROUP_DN = "cn=" + CR_GROUP_CN + "," + CR_BASE_DN;

    /**
     * Setup the test case.
     *
     * @throws Exception If the setup failed for some reason.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        setupLibertyServer();
        setupLDAPServer();
        setupCustomUserRegistry();
    }

    /**
     * Tear down the test.
     *
     * @throws Exception If the teardown failed for some reason.
     */
    @AfterClass
    public static void teardownClass() throws Exception {
        /*
         * Stop the Liberty and LDAP servers.
         */
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

        /*
         * Delete any files we copied to the test server and uninstall the user bundles.
         */
        libertyServer.deleteFileFromLibertyInstallRoot("lib/features/webspheresecuritylibertyinternals-1.0.mf");
        libertyServer.deleteFileFromLibertyInstallRoot("lib/features/customRegistrySample-1.0.mf");
        libertyServer.deleteFileFromLibertyInstallRoot("lib/com.ibm.ws.security.registry_test.custom_1.0.jar");
        libertyServer.uninstallUserBundle("com.ibm.ws.security.wim.repository_test.custom_1.0");
        libertyServer.uninstallUserFeature("customRepositorySample-1.0");
    }

    /**
     * The custom UserRegistry will contain one user. It is based on some files that contains the
     * users and groups.
     *
     * @throws Exception If writing to the users and groups files failed.
     */
    private static void setupCustomUserRegistry() throws Exception {

        /**
         * Write the custom UserRegistry users file.
         *
         * <pre>
         * # Format:
         * # name:passwd:uid:gids:display name
         * # where name   = userId/userName of the user
         * #       passwd = password of the user
         * #       uid    = uniqueId of the user
         * #       gid    = groupIds of the groups that the user belongs to
         * #       display name = a (optional) display name for the user.
         * </pre>
         */
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(new File(libertyServer.getServerRoot() + "/resources/security/users.props")));
            writer.write(CUR_USER_NAME);
            writer.write(":password:");
            writer.write(CUR_USER_UID);
            writer.write(":");
            writer.write(CUR_GROUP_GID);
            writer.write(":CUR User");
            writer.newLine();
        } finally {
            writer.close();
        }

        /**
         * Write the custom UserRegistry groups file.
         *
         * <pre>
         * # Format:
         * # name:passwd:uid:gids:display name
         * # where name   = groupId of the group
         * #       gid    = uniqueId of the group
         * #       users  = list of all the userIds that the group contains
         * #       display name = a (optional) display name for the group.
         * </pre>
         */
        try {
            writer = new BufferedWriter(new FileWriter(new File(libertyServer.getServerRoot() + "/resources/security/groups.props")));
            writer.write(CUR_GROUP_NAME + ":" + CUR_GROUP_GID + ":" + CUR_USER_NAME + ":CUR Group");
            writer.newLine();
        } finally {
            writer.close();
        }
    }

    /**
     * Configure the embedded LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLDAPServer() throws Exception {
        ds = new InMemoryLDAPServer(LDAP_BASE_DN);

        Entry entry = new Entry(LDAP_BASE_DN);
        entry.addAttribute("objectclass", "top");
        entry.addAttribute("objectclass", "domain");
        ds.add(entry);

        /*
         * Create a user.
         */
        entry = new Entry(LDAP_USER_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", LDAP_USER_UID);
        entry.addAttribute("sn", LDAP_USER_SN);
        entry.addAttribute("cn", LDAP_USER_CN);
        ds.add(entry);

        /*
         * Create a group.
         */
        entry = new Entry(LDAP_GROUP_DN);
        entry.addAttribute("objectclass", "groupofnames");
        entry.addAttribute("cn", LDAP_GROUP_CN);
        entry.addAttribute("member", LDAP_USER_DN);
        ds.add(entry);
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
        libertyServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/customRegistrySample-1.0.mf");
        libertyServer.copyFileToLibertyInstallRoot("lib", "internalfeatures/com.ibm.ws.security.registry_test.custom_1.0.jar");
        libertyServer.addInstalledAppForValidation("userRegistry");
        libertyServer.installUserBundle("com.ibm.ws.security.wim.repository_test.custom_1.0");
        libertyServer.installUserFeature("customRepositorySample-1.0");
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
        EMPTY_SERVER_CONFIGURATION = libertyServer.getServerConfiguration();
    }

    /**
     * Update the Liberty server configuration dynamically.
     *
     * @throws Exception If the server could not be updated.
     */
    private static ServerConfiguration getBasicLibertyConfiguration() throws Exception {
        ServerConfiguration server = EMPTY_SERVER_CONFIGURATION.clone();

        /*
         * Configure the LDAP registry.
         */
        LdapRegistry ldap = new LdapRegistry();
        server.getLdapRegistries().add(ldap);
        ldap.setRealm("LDAPRealm");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds.getLdapPort()));
        ldap.setBaseDN(LDAP_BASE_DN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setLdapCache(new LdapCache(new AttributesCache(false, 0, 0, "0s"), new SearchResultsCache(false, 0, 0, "0s")));

        /*
         * Configure the basic registry.
         */
        BasicRegistry basic = new BasicRegistry();
        server.getBasicRegistries().add(basic);
        basic.setId("basic");
        basic.setRealm("BasicRealm");
        User user = new User();
        user.setId(BASIC_USER);
        user.setName(BASIC_USER);
        user.setPassword("password");
        basic.getUsers().add(user);
        Group group = new Group();
        group.setId(BASIC_GROUP);
        group.setName(BASIC_GROUP);
        basic.getGroups().add(group);

        /*
         * Configure the custom UserRegistry.
         */
        server.getFeatureManager().getFeatures().add("customRegistrySample-1.0");
        String xml = "<fileRegistrySample usersFile=\"${server.config.dir}/resources/security/users.props\" groupsFile=\"${server.config.dir}/resources/security/groups.props\" />";
        List<Element> unknownElements = new ArrayList<Element>();
        unknownElements.add(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes())).getDocumentElement());
        server.addUnknownElements(unknownElements);

        /*
         * Configure the custom Repository.
         */
        server.getFeatureManager().getFeatures().add("usr:customRepositorySample-1.0");

        /*
         * Configure the federated repository.
         */
        String[] participatingEntries = new String[] { LDAP_BASE_DN, BASIC_BASE_DN, CUR_BASE_DN, CR_BASE_DN };
        createFederatedRepository(server, "FederatedRealm", participatingEntries);

        return server;
    }

    /**
     * Add userSecurityNameMapping configuration.
     *
     * @param serverConfig The {@link ServerConfiguration} instance to update.
     * @param mapping An array of size 2 that contains the input and output user security name mapping properties.
     */
    private void addUserSecurityNameMapping(ServerConfiguration serverConfig, String[] mapping) {
        FederatedRepository federatedRepository = serverConfig.getFederatedRepository();
        if (mapping != null && mapping.length == 2) {
            federatedRepository.getPrimaryRealm().setUserSecurityNameMapping(new RealmPropertyMapping(mapping[0], mapping[1]));
        }
    }

    /**
     * Add userDisplayNameMapping configuration.
     *
     * @param serverConfig The {@link ServerConfiguration} instance to update.
     * @param mapping An array of size 2 that contains the input and output user display name mapping properties.
     */
    private void addUserDisplayNameMapping(ServerConfiguration serverConfig, String[] mapping) {
        FederatedRepository federatedRepository = serverConfig.getFederatedRepository();
        if (mapping != null && mapping.length == 2) {
            federatedRepository.getPrimaryRealm().setUserDisplayNameMapping(new RealmPropertyMapping(mapping[0], mapping[1]));
        }
    }

    /**
     * Add uniqueUserIdMapping configuration.
     *
     * @param serverConfig The {@link ServerConfiguration} instance to update.
     * @param mapping An array of size 2 that contains the input and output unique user ID mapping properties.
     */
    private void addUniqueUserIdMapping(ServerConfiguration serverConfig, String[] mapping) {
        FederatedRepository federatedRepository = serverConfig.getFederatedRepository();
        if (mapping != null && mapping.length == 2) {
            federatedRepository.getPrimaryRealm().setUniqueUserIdMapping(new RealmPropertyMapping(mapping[0], mapping[1]));
        }
    }

    /**
     * Add groupSecurityNameMapping configuration.
     *
     * @param serverConfig The {@link ServerConfiguration} instance to update.
     * @param mapping An array of size 2 that contains the input and output group security name mapping properties.
     */
    private void addGroupSecurityNameMapping(ServerConfiguration serverConfig, String[] mapping) {
        FederatedRepository federatedRepository = serverConfig.getFederatedRepository();
        if (mapping != null && mapping.length == 2) {
            federatedRepository.getPrimaryRealm().setGroupSecurityNameMapping(new RealmPropertyMapping(mapping[0], mapping[1]));
        }
    }

    /**
     * Add groupDisplayNameMapping configuration.
     *
     * @param serverConfig The {@link ServerConfiguration} instance to update.
     * @param mapping An array of size 2 that contains the input and output group display name mapping properties.
     */
    private void addGroupDisplayNameMapping(ServerConfiguration serverConfig, String[] mapping) {
        FederatedRepository federatedRepository = serverConfig.getFederatedRepository();
        if (mapping != null && mapping.length == 2) {
            federatedRepository.getPrimaryRealm().setGroupDisplayNameMapping(new RealmPropertyMapping(mapping[0], mapping[1]));
        }
    }

    /**
     * Add uniqueGroupIdMapping configuration.
     *
     * @param serverConfig The {@link ServerConfiguration} instance to update.
     * @param mapping An array of size 2 that contains the input and output unique group ID mapping properties.
     */
    private void addUniqueGroupIdMapping(ServerConfiguration serverConfig, String[] mapping) {
        FederatedRepository federatedRepository = serverConfig.getFederatedRepository();
        if (mapping != null && mapping.length == 2) {
            federatedRepository.getPrimaryRealm().setUniqueGroupIdMapping(new RealmPropertyMapping(mapping[0], mapping[1]));
        }
    }

    /**
     * This test will NOT provide userSecurityNameMapping configuration and will default to
     * the following property mappings:
     *
     * <pre>
     * Input property:  principalName (default)
     * Output property: uniqueName    (default)
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void userSecurityNameMapping1() throws Exception {
        // Should default to principalName and uniqueName from metatype.xml.
        ServerConfiguration config = getBasicLibertyConfiguration();
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_DN, servlet.getUserSecurityName(LDAP_USER_DN));
        assertEquals(LDAP_USER_DN, servlet.getUserSecurityName(LDAP_USER_UID));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_USER, servlet.getUserSecurityName(BASIC_USER));

        /*
         * Custom UserRegistry
         */
        assertEquals(CUR_USER_NAME, servlet.getUserSecurityName(CUR_USER_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_DN, servlet.getUserSecurityName(CR_USER_DN));
        assertEquals(CR_USER_DN, servlet.getUserSecurityName(CR_USER_CN));
    }

    /**
     * This test will provide userSecurityNameMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  principalName
     * Output property: uniqueName
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void userSecurityNameMapping2() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUserSecurityNameMapping(config, new String[] { "principalName", "uniqueName" }); // Defaults from metatype.xml
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_DN, servlet.getUserSecurityName(LDAP_USER_DN));
        assertEquals(LDAP_USER_DN, servlet.getUserSecurityName(LDAP_USER_UID));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_USER, servlet.getUserSecurityName(BASIC_USER));

        /*
         * Custom UserRegistry
         */
        assertEquals(CUR_USER_NAME, servlet.getUserSecurityName(CUR_USER_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_DN, servlet.getUserSecurityName(CR_USER_DN));
        assertEquals(CR_USER_DN, servlet.getUserSecurityName(CR_USER_CN));
    }

    /**
     * This test will provide userSecurityNameMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  principalName
     * Output property: principalName
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void userSecurityNameMapping3() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUserSecurityNameMapping(config, new String[] { "principalName", "principalName" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_UID, servlet.getUserSecurityName(LDAP_USER_DN));
        assertEquals(LDAP_USER_UID, servlet.getUserSecurityName(LDAP_USER_UID));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_USER, servlet.getUserSecurityName(BASIC_USER));

        /*
         * Custom UserRegistry
         */
        assertEquals(CUR_USER_NAME, servlet.getUserSecurityName(CUR_USER_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_CN, servlet.getUserSecurityName(CR_USER_DN));
        assertEquals(CR_USER_CN, servlet.getUserSecurityName(CR_USER_CN));
    }

    /**
     * This test will provide userSecurityNameMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  principalName
     * Output property: cn
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void userSecurityNameMapping4() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUserSecurityNameMapping(config, new String[] { "principalName", "cn" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_CN, servlet.getUserSecurityName(LDAP_USER_DN));
        assertEquals(LDAP_USER_CN, servlet.getUserSecurityName(LDAP_USER_UID));

        /*
         * BasicRegistry and Custom UserRegistry don't support CN property.
         */

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_CN, servlet.getUserSecurityName(CR_USER_DN));
        assertEquals(CR_USER_CN, servlet.getUserSecurityName(CR_USER_CN));
    }

    /**
     * This test will provide userSecurityNameMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  cn
     * Output property: cn
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void userSecurityNameMapping5() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUserSecurityNameMapping(config, new String[] { "cn", "cn" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_CN, servlet.getUserSecurityName(LDAP_USER_DN));
        assertEquals(LDAP_USER_CN, servlet.getUserSecurityName(LDAP_USER_CN));

        /*
         * BasicRegistry and Custom UserRegistry don't support CN property.
         */

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_CN, servlet.getUserSecurityName(CR_USER_DN));
        assertEquals(CR_USER_CN, servlet.getUserSecurityName(CR_USER_CN));
    }

    /**
     * This test will provide userSecurityNameMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  principalName
     * Output property: uniqueId
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void userSecurityNameMapping6() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUserSecurityNameMapping(config, new String[] { "principalName", "uniqueId" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_DN, servlet.getUserSecurityName(LDAP_USER_DN).toLowerCase());
        assertEquals(LDAP_USER_DN, servlet.getUserSecurityName(LDAP_USER_UID).toLowerCase());

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_USER, servlet.getUserSecurityName(BASIC_USER));

        /*
         * Custom UserRegistry
         */
        assertEquals(CUR_USER_UID, servlet.getUserSecurityName(CUR_USER_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_DN, servlet.getUserSecurityName(CR_USER_DN));
        assertEquals(CR_USER_DN, servlet.getUserSecurityName(CR_USER_CN));
    }

    /**
     * This test will NOT provide uniqueUserIdMapping configuration and will default to
     * the following property mappings:
     *
     * <pre>
     * Input property:  uniqueName (default)
     * Output property: uniqueName (default)
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void uniqueUserIdMapping1() throws Exception {
        // Should default to uniqueName and uniqueName from metatype.xml.
        ServerConfiguration config = getBasicLibertyConfiguration();
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_DN, servlet.getUniqueUserId(LDAP_USER_DN));
        assertEquals(LDAP_USER_DN, servlet.getUniqueUserId(LDAP_USER_UID));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_USER, servlet.getUniqueUserId(BASIC_USER));

        /*
         * Custom UserRegistry
         *
         * The getUniqueUserId() method will return the uniqueID when requesting
         * uniqueName when the returned uniqueName isn't a DN.
         */
        assertEquals(CUR_USER_UID, servlet.getUniqueUserId(CUR_USER_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_DN, servlet.getUniqueUserId(CR_USER_DN));
        assertEquals(CR_USER_DN, servlet.getUniqueUserId(CR_USER_CN));
    }

    /**
     * This test will provide uniqueUserIdMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  uniqueName
     * Output property: uniqueName
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void uniqueUserIdMapping2() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUniqueUserIdMapping(config, new String[] { "uniqueName", "uniqueName" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_DN, servlet.getUniqueUserId(LDAP_USER_DN));
        assertEquals(LDAP_USER_DN, servlet.getUniqueUserId(LDAP_USER_UID));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_USER, servlet.getUniqueUserId(BASIC_USER));

        /*
         * Custom UserRegistry
         *
         * The getUniqueUserId() method will return the uniqueID when requesting
         * uniqueName when the returned uniqueName isn't a DN.
         */
        assertEquals(CUR_USER_UID, servlet.getUniqueUserId(CUR_USER_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_DN, servlet.getUniqueUserId(CR_USER_DN));
        assertEquals(CR_USER_DN, servlet.getUniqueUserId(CR_USER_CN));
    }

    /**
     * This test will provide uniqueUserIdMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  uniqueName
     * Output property: principalName
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void uniqueUserIdMapping3() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUniqueUserIdMapping(config, new String[] { "uniqueName", "principalName" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_UID, servlet.getUniqueUserId(LDAP_USER_DN));
        assertEquals(LDAP_USER_UID, servlet.getUniqueUserId(LDAP_USER_UID));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_USER, servlet.getUniqueUserId(BASIC_USER));

        /*
         * Custom UserRegistry
         */
        assertEquals(CUR_USER_NAME, servlet.getUniqueUserId(CUR_USER_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_CN, servlet.getUniqueUserId(CR_USER_DN));
        assertEquals(CR_USER_CN, servlet.getUniqueUserId(CR_USER_CN));
    }

    /**
     * This test will provide uniqueUserIdMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  uniqueName
     * Output property: cn
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void uniqueUserIdMapping4() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUniqueUserIdMapping(config, new String[] { "uniqueName", "cn" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_CN, servlet.getUniqueUserId(LDAP_USER_DN));
        assertEquals(LDAP_USER_CN, servlet.getUniqueUserId(LDAP_USER_UID));

        /*
         * BasicRegistry and Custom UserRegistry don't support CN property.
         */

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_CN, servlet.getUniqueUserId(CR_USER_DN));
        assertEquals(CR_USER_CN, servlet.getUniqueUserId(CR_USER_CN));
    }

    /**
     * This test will provide uniqueUserIdMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  cn
     * Output property: cn
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void uniqueUserIdMapping5() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUniqueUserIdMapping(config, new String[] { "cn", "cn" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_CN, servlet.getUniqueUserId(LDAP_USER_DN));
        assertEquals(LDAP_USER_CN, servlet.getUniqueUserId(LDAP_USER_UID));

        /*
         * BasicRegistry and Custom UserRegistry don't support CN property.
         */

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_CN, servlet.getUniqueUserId(CR_USER_DN));
        assertEquals(CR_USER_CN, servlet.getUniqueUserId(CR_USER_CN));
    }

    /**
     * This test will provide uniqueUserIdMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  uniqueName
     * Output property: uniqueId
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void uniqueUserIdMapping6() throws Exception {
        ServerConfiguration serverConfig = getBasicLibertyConfiguration();
        addUniqueUserIdMapping(serverConfig, new String[] { "uniqueName", "uniqueId" });
        updateConfigDynamically(libertyServer, serverConfig);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_DN, servlet.getUniqueUserId(LDAP_USER_DN).toLowerCase());
        assertEquals(LDAP_USER_DN, servlet.getUniqueUserId(LDAP_USER_UID).toLowerCase());

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_USER, servlet.getUniqueUserId(BASIC_USER));

        /*
         * Custom UserRegistry
         */
        assertEquals(CUR_USER_UID, servlet.getUniqueUserId(CUR_USER_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_DN, servlet.getUniqueUserId(CR_USER_DN));
        assertEquals(CR_USER_DN, servlet.getUniqueUserId(CR_USER_CN));
    }

    /**
     * This test will NOT provide userDisplayNameMapping configuration and will default to
     * the following property mappings:
     *
     * <pre>
     * Input property:  principalName (default)
     * Output property: principalName (default)
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void userDisplayNameMapping1() throws Exception {
        // Should default to principalName and principalName from metatype.xml.
        ServerConfiguration config = getBasicLibertyConfiguration();
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_UID, servlet.getUserDisplayName(LDAP_USER_DN));
        assertEquals(LDAP_USER_UID, servlet.getUserDisplayName(LDAP_USER_UID));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_USER, servlet.getUserDisplayName(BASIC_USER));

        /*
         * Custom UserRegistry
         */
        assertEquals(CUR_USER_NAME, servlet.getUserDisplayName(CUR_USER_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_CN, servlet.getUserDisplayName(CR_USER_DN));
        assertEquals(CR_USER_CN, servlet.getUserDisplayName(CR_USER_CN));
    }

    /**
     * This test will provide userDisplayNameMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  principalName
     * Output property: principalName
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void userDisplayNameMapping2() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUserDisplayNameMapping(config, new String[] { "principalName", "principalName" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_UID, servlet.getUserDisplayName(LDAP_USER_DN));
        assertEquals(LDAP_USER_UID, servlet.getUserDisplayName(LDAP_USER_UID));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_USER, servlet.getUserDisplayName(BASIC_USER));

        /*
         * Custom UserRegistry
         */
        assertEquals(CUR_USER_NAME, servlet.getUserDisplayName(CUR_USER_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_CN, servlet.getUserDisplayName(CR_USER_DN));
        assertEquals(CR_USER_CN, servlet.getUserDisplayName(CR_USER_CN));
    }

    /**
     * This test will provide userDisplayNameMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  principalName
     * Output property: uniqueName
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void userDisplayNameMapping3() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUserDisplayNameMapping(config, new String[] { "principalName", "uniqueName" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_DN, servlet.getUserDisplayName(LDAP_USER_DN));
        assertEquals(LDAP_USER_DN, servlet.getUserDisplayName(LDAP_USER_UID));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_USER, servlet.getUserDisplayName(BASIC_USER));

        /*
         * Custom UserRegistry
         */
        assertEquals(CUR_USER_NAME, servlet.getUserDisplayName(CUR_USER_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_DN, servlet.getUserDisplayName(CR_USER_DN));
        assertEquals(CR_USER_DN, servlet.getUserDisplayName(CR_USER_CN));
    }

    /**
     * This test will provide userDisplayNameMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  principalName
     * Output property: cn
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void userDisplayNameMapping4() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUserDisplayNameMapping(config, new String[] { "principalName", "cn" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_CN, servlet.getUserDisplayName(LDAP_USER_DN));
        assertEquals(LDAP_USER_CN, servlet.getUserDisplayName(LDAP_USER_UID));

        /*
         * BasicRegistry and Custom UserRegistry don't support CN property.
         */

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_CN, servlet.getUserDisplayName(CR_USER_DN));
        assertEquals(CR_USER_CN, servlet.getUserDisplayName(CR_USER_CN));
    }

    /**
     * This test will provide userDisplayNameMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  cn
     * Output property: cn
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void userDisplayNameMapping5() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUserDisplayNameMapping(config, new String[] { "cn", "cn" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_CN, servlet.getUserDisplayName(LDAP_USER_DN));
        assertEquals(LDAP_USER_CN, servlet.getUserDisplayName(LDAP_USER_UID));

        /*
         * BasicRegistry and Custom UserRegistry don't support CN property.
         */

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_CN, servlet.getUserDisplayName(CR_USER_DN));
        assertEquals(CR_USER_CN, servlet.getUserDisplayName(CR_USER_CN));
    }

    /**
     * This test will provide userDisplayNameMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  principalName
     * Output property: uniqueId
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void userDisplayNameMapping6() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUserDisplayNameMapping(config, new String[] { "principalName", "uniqueId" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_USER_DN, servlet.getUserDisplayName(LDAP_USER_DN).toLowerCase());
        assertEquals(LDAP_USER_DN, servlet.getUserDisplayName(LDAP_USER_UID).toLowerCase());

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_USER, servlet.getUserDisplayName(BASIC_USER));

        /*
         * Custom UserRegistry
         */
        assertEquals(CUR_USER_UID, servlet.getUserDisplayName(CUR_USER_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_USER_DN, servlet.getUserDisplayName(CR_USER_DN));
        assertEquals(CR_USER_DN, servlet.getUserDisplayName(CR_USER_CN));
    }

    /**
     * This test will NOT provide groupSecurityNameMapping configuration and will default to
     * the following property mappings:
     *
     * <pre>
     * Input property:  cn         (default)
     * Output property: uniqueName (default)
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void groupSecurityNameMapping1() throws Exception {
        // Should default to cn and cn from metatype.xml.
        ServerConfiguration config = getBasicLibertyConfiguration();
        updateConfigDynamically(libertyServer, config);

        /*
         * TODO The metatype.xml says the mapping is cn / cn, but the TypeMappings has
         * cn / uniqueName.
         */

        /*
         * LDAP
         */
        assertEquals(LDAP_GROUP_DN, servlet.getGroupSecurityName(LDAP_GROUP_CN));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_GROUP, servlet.getGroupSecurityName(BASIC_GROUP));

        /*
         * Custom UserRegistry
         */
        assertEquals(CUR_GROUP_NAME, servlet.getGroupSecurityName(CUR_GROUP_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_GROUP_DN, servlet.getGroupSecurityName(CR_GROUP_CN));
    }

    /**
     * This test will provide groupSecurityNameMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  cn
     * Output property: uniqueName
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void groupSecurityNameMapping2() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addGroupSecurityNameMapping(config, new String[] { "cn", "uniqueName" }); // Defaults from metatype.xml
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_GROUP_DN, servlet.getGroupSecurityName(LDAP_GROUP_CN));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_GROUP, servlet.getGroupSecurityName(BASIC_GROUP));

        /*
         * Custom UserRegistry
         */
        assertEquals(CUR_GROUP_NAME, servlet.getGroupSecurityName(CUR_GROUP_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_GROUP_DN, servlet.getGroupSecurityName(CR_GROUP_CN));
    }

    /**
     * This test will provide groupSecurityNameMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  cn
     * Output property: cn
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void groupSecurityNameMapping3() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addGroupSecurityNameMapping(config, new String[] { "cn", "cn" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_GROUP_CN, servlet.getGroupSecurityName(LDAP_GROUP_CN));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_GROUP, servlet.getGroupSecurityName(BASIC_GROUP));

        /*
         * Custom UserRegistry
         */
        assertEquals(CUR_GROUP_NAME, servlet.getGroupSecurityName(CUR_GROUP_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_GROUP_CN, servlet.getGroupSecurityName(CR_GROUP_CN));
    }

    /**
     * This test will NOT provide uniqueGroupIdMapping configuration and will default to
     * the following property mappings:
     *
     * <pre>
     * Input property:  cn         (default)
     * Output property: uniqueName (default)
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void uniqueGroupIdMapping1() throws Exception {
        // Should default to cn and uniqueName from metatype.xml.
        ServerConfiguration config = getBasicLibertyConfiguration();
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_GROUP_DN, servlet.getUniqueGroupId(LDAP_GROUP_DN));
        assertEquals(LDAP_GROUP_DN, servlet.getUniqueGroupId(LDAP_GROUP_CN));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_GROUP, servlet.getUniqueGroupId(BASIC_GROUP));

        /*
         * Custom UserRegistry
         *
         * The getUniqueGroupId() method will return the uniqueID when requesting
         * uniqueName when the returned uniqueName isn't a DN.
         */
        assertEquals(CUR_GROUP_GID, servlet.getUniqueGroupId(CUR_GROUP_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_GROUP_DN, servlet.getUniqueGroupId(CR_GROUP_DN));
        assertEquals(CR_GROUP_DN, servlet.getUniqueGroupId(CR_GROUP_CN));
    }

    /**
     * This test will provide uniqueGroupIdMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  cn
     * Output property: uniqueName
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void uniqueGroupIdMapping2() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUniqueGroupIdMapping(config, new String[] { "cn", "uniqueName" }); // Defaults from metatype.xml
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_GROUP_DN, servlet.getUniqueGroupId(LDAP_GROUP_DN));
        assertEquals(LDAP_GROUP_DN, servlet.getUniqueGroupId(LDAP_GROUP_CN));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_GROUP, servlet.getUniqueGroupId(BASIC_GROUP));

        /*
         * Custom UserRegistry
         *
         * The getUniqueGroupId() method will return the uniqueID when requesting
         * uniqueName when the returned uniqueName isn't a DN.
         */
        assertEquals(CUR_GROUP_GID, servlet.getUniqueGroupId(CUR_GROUP_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_GROUP_DN, servlet.getUniqueGroupId(CR_GROUP_DN));
        assertEquals(CR_GROUP_DN, servlet.getUniqueGroupId(CR_GROUP_CN));
    }

    /**
     * This test will provide uniqueGroupIdMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  uniqueName
     * Output property: cn
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void uniqueGroupIdMapping3() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addUniqueGroupIdMapping(config, new String[] { "uniqueName", "cn" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_GROUP_CN, servlet.getUniqueGroupId(LDAP_GROUP_DN));
        assertEquals(LDAP_GROUP_CN, servlet.getUniqueGroupId(LDAP_GROUP_CN));

        /*
         * BasicRegistry
         */
        assertEquals(BASIC_GROUP, servlet.getUniqueGroupId(BASIC_GROUP));

        /*
         * Custom UserRegistry
         *
         * The getUniqueGroupId() method will return the uniqueID when requesting
         * uniqueName when the returned uniqueName isn't a DN.
         */
        assertEquals(CUR_GROUP_NAME, servlet.getUniqueGroupId(CUR_GROUP_NAME));

        /*
         * Custom Repository
         */
        assertEquals(CR_GROUP_CN, servlet.getUniqueGroupId(CR_GROUP_DN));
        assertEquals(CR_GROUP_CN, servlet.getUniqueGroupId(CR_GROUP_CN));
    }

    /**
     * This test will NOT provide groupDisplayNameMapping configuration and will default to
     * the following property mappings:
     *
     * <pre>
     * Input property:  cn (default)
     * Output property: cn (default)
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void groupDisplayNameMapping1() throws Exception {
        // Should default to cn and cn from metatype.xml.
        ServerConfiguration config = getBasicLibertyConfiguration();
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_GROUP_CN, servlet.getGroupDisplayName(LDAP_GROUP_DN));
        assertEquals(LDAP_GROUP_CN, servlet.getGroupDisplayName(LDAP_GROUP_CN));

        /*
         * BasicRegistry and Custom UserRegistry don't support CN property.
         */

        /*
         * Custom Repository
         */
        assertEquals(CR_GROUP_CN, servlet.getGroupDisplayName(CR_GROUP_DN));
        assertEquals(CR_GROUP_CN, servlet.getGroupDisplayName(CR_GROUP_CN));
    }

    /**
     * This test will provide groupDisplayNameMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  cn
     * Output property: cn
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void groupDisplayNameMapping2() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addGroupDisplayNameMapping(config, new String[] { "cn", "cn" }); // Defaults from metatype.xml
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_GROUP_CN, servlet.getGroupDisplayName(LDAP_GROUP_DN));
        assertEquals(LDAP_GROUP_CN, servlet.getGroupDisplayName(LDAP_GROUP_CN));

        /*
         * BasicRegistry and Custom UserRegistry don't support CN property.
         */

        /*
         * Custom Repository
         */
        assertEquals(CR_GROUP_CN, servlet.getGroupDisplayName(CR_GROUP_DN));
        assertEquals(CR_GROUP_CN, servlet.getGroupDisplayName(CR_GROUP_CN));
    }

    /**
     * This test will provide groupDisplayNameMapping configuration with the following
     * property mappings:
     *
     * <pre>
     * Input property:  uniqueName
     * Output property: uniqueName
     * </pre>
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void groupDisplayNameMapping3() throws Exception {
        ServerConfiguration config = getBasicLibertyConfiguration();
        addGroupDisplayNameMapping(config, new String[] { "uniqueName", "uniqueName" });
        updateConfigDynamically(libertyServer, config);

        /*
         * LDAP
         */
        assertEquals(LDAP_GROUP_DN, servlet.getGroupDisplayName(LDAP_GROUP_DN));
        assertEquals(LDAP_GROUP_DN, servlet.getGroupDisplayName(LDAP_GROUP_CN));

        /*
         * BasicRegistry and Custom UserRegistry don't support CN property.
         */

        /*
         * Custom Repository
         */
        assertEquals(CR_GROUP_DN, servlet.getGroupDisplayName(CR_GROUP_DN));
        assertEquals(CR_GROUP_DN, servlet.getGroupDisplayName(CR_GROUP_CN));
    }
}