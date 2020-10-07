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

package com.ibm.ws.security.wim.registry.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.BaseEntry;
import com.ibm.websphere.simplicity.config.wim.FederatedRepository;
import com.ibm.websphere.simplicity.config.wim.LdapFilters;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.Realm;
import com.ibm.websphere.simplicity.config.wim.RealmPropertyMapping;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;
import com.unboundid.ldap.sdk.Entry;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.LDAPFatUtils;
import componenttest.topology.utils.LDAPUtils;

/**
 * Some tests for handling of multi-valued output mappings for the WIMUserRegistry bridge in federated respositories.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class MultiValuedOutputPropMappingTest {

    @Server("com.ibm.ws.security.wim.registry.fat.MultiValuedOutputPropMapping")
    public static LibertyServer libertyServer;

    private static final Class<?> c = MultiValuedOutputPropMappingTest.class;
    private static UserRegistryServletConnection servlet;
    private static InMemoryLDAPServer ds = null;
    private static ServerConfiguration startConfiguration = null;

    private static final String BASE_DN = "o=ibm.com";
    private static final String GROUP_CN = "group1";
    private static final String GROUP_DN = "cn=" + GROUP_CN + "," + BASE_DN;
    private static final String GROUP_DISPLAY_NAME = GROUP_CN + "_displayName";

    private static final String USER_UID = "user1";
    private static final String USER_DN = "uid=" + USER_UID + "," + BASE_DN;
    private static final String USER_PASSWORD = "password";
    private static final String USER_DISPLAY_NAME = USER_UID + "_displayName";

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
            try {
                if (ds != null) {
                    ds.shutDown(true);
                }
            } catch (Exception e) {
                Log.error(c, "teardown", e, "LDAP server threw error while shutting down. " + e.getMessage());
            }
        }
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
        Log.info(c, "setUpLibertyServer", "Starting the server... (will wait for userRegistry servlet to start)");
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
         * The original server configuration.
         */
        startConfiguration = libertyServer.getServerConfiguration();
    }

    /**
     * Configure the embedded LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLdapServer() throws Exception {
        ds = new InMemoryLDAPServer(BASE_DN);
        Log.info(c, "setUpldapServer", "populate LDAP Server");

        /*
         * Add the partition entries.
         */
        Entry entry = new Entry(BASE_DN);
        entry.addAttribute("objectclass", "organization");
        entry.addAttribute("o", "ibm.com");
        ds.add(entry);

        /*
         * Create user.
         */
        entry = new Entry(USER_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", USER_UID);
        entry.addAttribute("sn", USER_UID + "_sn");
        entry.addAttribute("cn", USER_UID + "_cn");
        entry.addAttribute("userPassword", USER_PASSWORD);
        entry.addAttribute("displayName", USER_DISPLAY_NAME);
        ds.add(entry);

        /*
         * Create group.
         */
        entry = new Entry(GROUP_DN);
        entry.addAttribute("objectclass", "wimGroupOfNames");
        entry.addAttribute("cn", GROUP_CN);
        entry.addAttribute("displayName", GROUP_DISPLAY_NAME);
        entry.addAttribute("member", USER_DN);
        ds.add(entry);

    }

    /**
     * Create a basic LDAP registry configuration element for the specified server configuration.
     * The registry will be added to the configuration.
     *
     * @param serverConfig The server configuration to add the LDAP registry to.
     * @return The new LDAP registry configuration element.
     */
    private static LdapRegistry createLdapRegistry(ServerConfiguration serverConfig) {
        /*
         * Create and add the new LDAP registry to the server configuration.
         */
        LdapRegistry ldapRegistry = new LdapRegistry();
        serverConfig.getLdapRegistries().add(ldapRegistry);

        /*
         * Configure the LDAP registry.
         */
        ldapRegistry.setBaseDN(BASE_DN);
        ldapRegistry.setLdapType("Custom");
        ldapRegistry.setRealm("LdapRealm");
        ldapRegistry.setHost("localhost");
        ldapRegistry.setPort(String.valueOf(ds.getLdapPort()));
        ldapRegistry.setBindDN(InMemoryLDAPServer.getBindDN());
        ldapRegistry.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldapRegistry.setTimestampFormat("yyyyMMddHHmmss.SSSSZ"); // 20180730202338.850-0000Z
        ldapRegistry.setCustomFilters(new LdapFilters("(&(|(objectClass=inetorgperson))(displayName=%v))", "(&(|(objectClass=groupOfNames))(displayName=%v))", null, null, null));

        return ldapRegistry;
    }

    private static FederatedRepository createFederatedRepository(ServerConfiguration serverConfig) {
        FederatedRepository federatedRepository = new FederatedRepository();
        serverConfig.setFederatedRepositoryElement(federatedRepository);

        Realm primaryRealm = new Realm();
        federatedRepository.setPrimaryRealm(primaryRealm);

        primaryRealm.setName("FederatedRealm");
        primaryRealm.getParticipatingBaseEntries().add(new BaseEntry(BASE_DN));

        /*
         * Configure the group output mappings to use a multi-valued Group property. In this case,
         * we used displayName (even though it is single-valued in LDAP schema).
         *
         * The totality of the mappings really doen't make sense, but we are doing this for testing.
         */
        primaryRealm.setGroupDisplayNameMapping(new RealmPropertyMapping("displayName", "displayName"));
        primaryRealm.setGroupSecurityNameMapping(new RealmPropertyMapping("displayName", "displayName"));
        primaryRealm.setUniqueGroupIdMapping(new RealmPropertyMapping("displayName", "displayName"));

        /*
         * Configure the user output mappings to use a multi-valued PersonAccount property. In this case,
         * we used displayName (even though it is single-valued in LDAP schema).
         *
         * The totality of the mappings really doen't make sense, but we are doing this for testing.
         */
        primaryRealm.setUserDisplayNameMapping(new RealmPropertyMapping("displayName", "displayName"));
        primaryRealm.setUserSecurityNameMapping(new RealmPropertyMapping("displayName", "displayName"));
        primaryRealm.setUniqueUserIdMapping(new RealmPropertyMapping("displayName", "displayName"));

        return federatedRepository;
    }

    /**
     * Regression test for Open Liberty GitHub issue 7539.
     *
     * This failure manifested in ClassCastExceptions whenever a multi-valued Group or PersonAccount
     * property was used as the output property of a federated repository input-output property mapping.
     *
     * @throws Exception if the test failed for some unforeseen reason.
     */
    @Test
    public void multiValuedOutputProperties() throws Exception {

        ServerConfiguration clone = startConfiguration.clone();
        createLdapRegistry(clone);
        createFederatedRepository(clone);
        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        /*
         * All these calls would previously throw a ClassCastException when trying to map the WIM PersonAccount
         * or Group property to the WIMUserRegistry's return result when the mapped output property for the
         * call was a multi-valued PersonAccount or Group property.
         */
        assertEquals(GROUP_DISPLAY_NAME, servlet.getGroupDisplayName(GROUP_DISPLAY_NAME));
        assertEquals(GROUP_DISPLAY_NAME, servlet.getGroupSecurityName(GROUP_DISPLAY_NAME));
        assertEquals(GROUP_DISPLAY_NAME, servlet.getUniqueGroupId(GROUP_DISPLAY_NAME));

        assertEquals(USER_DISPLAY_NAME, servlet.getUserDisplayName(USER_DISPLAY_NAME));
        assertEquals(USER_DISPLAY_NAME, servlet.getUserSecurityName(USER_DISPLAY_NAME));
        assertEquals(USER_DISPLAY_NAME, servlet.getUniqueUserId(USER_DISPLAY_NAME));
        assertEquals(USER_DISPLAY_NAME, servlet.checkPassword(USER_DISPLAY_NAME, USER_PASSWORD));
    }
}
