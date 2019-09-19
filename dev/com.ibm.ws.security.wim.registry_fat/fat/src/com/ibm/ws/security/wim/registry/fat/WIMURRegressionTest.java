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
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.Realm;
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
 * Regression tests for fixes made to the WIM UserRegistry component.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class WIMURRegressionTest {

    @Server("com.ibm.ws.security.wim.registry.fat.WIMURRegression")
    public static LibertyServer libertyServer;

    private static final Class<?> c = WIMURRegressionTest.class;
    private static UserRegistryServletConnection servlet;
    private static InMemoryLDAPServer ds = null;
    private static ServerConfiguration startConfiguration = null;

    /** The default realm name as defined in ProfileManager.DEFAULT_REALM_NAME. */
    private static final String DEFAULT_REALM_NAME = "WIMRegistry";

    /** The realm name for the LDAP repository. */
    private static final String LDAP_REALM_NAME = "LdapRealm";

    private static final String BASE_DN = "o=ibm.com";
    private static final String USER = "test//";
    private static final String USER_DN = "uid=" + USER + "," + BASE_DN;
    private static final String USER_PASSWORD = "password";

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
         * Create users.
         */
        entry = new Entry(USER_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", USER);
        entry.addAttribute("sn", USER + "_sn");
        entry.addAttribute("cN", USER + "_cn");
        entry.addAttribute("userPassword", USER_PASSWORD);
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
        ldapRegistry.setRealm(LDAP_REALM_NAME);
        ldapRegistry.setHost("localhost");
        ldapRegistry.setPort(String.valueOf(ds.getListenPort()));
        ldapRegistry.setBindDN(InMemoryLDAPServer.getBindDN());
        ldapRegistry.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldapRegistry.setTimestampFormat("yyyyMMddHHmmss.SSSSZ"); // 20180730202338.850-0000Z

        return ldapRegistry;
    }

    /**
     * Regression test for Open Liberty GitHub issue 5337.
     *
     * When there is no configured realm in federated repositories and the user name contains the realm delimiter,
     * calls to the WIMUserRegistry methods will fail when they try to parse the realm and user ID from the input
     * value.
     *
     * @throws Exception if the test failed for some unforeseen reason.
     */
    @Test
    public void npeInSeparateIDAndRealm() throws Exception {

        ServerConfiguration clone = startConfiguration.clone();
        createLdapRegistry(clone);
        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        /*
         * This call would result in a NPE prior to the fix due to the lack of realms being defined.
         */
        assertEquals(USER_DN, servlet.getUserSecurityName(USER_DN));
    }

    /**
     * A regression test for some changes made in Open Liberty GitHub issue 5337.
     *
     * A few tests to ensure that the realm name is returned as expected for various configurations. Internally,
     * WIM was not handling the realm name consistently.
     *
     * @throws Exception if the test failed for some unforeseen reason.
     */
    @Test
    public void getRealmName() throws Exception {

        /*
         * Clear the server configuration so there are no federated repositories.
         */
        ServerConfiguration clone = startConfiguration.clone();
        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        /*
         * No realm and no repository defined? Default realm name.
         */
        assertEquals(DEFAULT_REALM_NAME, servlet.getRealm());

        /*
         * Configure a single registry. Return the name of the first / only realm.
         */
        clone = startConfiguration.clone();
        createLdapRegistry(clone);
        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);
        assertEquals(LDAP_REALM_NAME, servlet.getRealm());

        /*
         * Configure the name of the realm explicitly.
         */
        final String FEDERATED_REALM = "FederatedRealm";
        clone = startConfiguration.clone();
        createLdapRegistry(clone);
        FederatedRepository federatedRepository = new FederatedRepository();
        Realm realm = new Realm();
        realm.setName(FEDERATED_REALM);
        realm.getParticipatingBaseEntries().add(new BaseEntry(BASE_DN));
        federatedRepository.setPrimaryRealm(realm);
        clone.setFederatedRepositoryElement(federatedRepository);
        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);
        assertEquals(FEDERATED_REALM, servlet.getRealm());
    }
}