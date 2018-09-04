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
import static org.junit.Assert.assertTrue;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.AttributesCache;
import com.ibm.websphere.simplicity.config.wim.FederatedRepository;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapFilters;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
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
import componenttest.topology.utils.LDAPFatUtils;
import componenttest.topology.utils.LDAPUtils;

/**
 * A user was trying to use a user filter and userIDMap to login with one attribute and have the user
 * returned as a different attribute.
 *
 * Wrote this test to confirm that the input/output mappings on the
 * FederatedRepository elements can achieve this.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class InputOutputMappingTest {

    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.input.output.mapping");
    private static final Class<?> c = InputOutputMappingTest.class;
    private static UserRegistryServletConnection servlet;

    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
    private static ServerConfiguration emptyConfiguration = null;

    private static EmbeddedApacheDS ldapServer = null;
    private static final String DN = "o=ibm,c=us";
    private static final String USER_DN = "uid=user1," + DN;
    private static final String userCode = "user1code";
    private static final String userMail = "user1@ibm.com";

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
        ldapServer.addPartition("testing", DN);
        ldapServer.startServer();

        /*
         * Add the partition entries.
         */
        Entry entry = ldapServer.newEntry(DN);
        entry.add("objectclass", "organization");
        entry.add("o", "ibm");
        ldapServer.add(entry);

        /*
         * Create the user and group.
         */
        entry = ldapServer.newEntry(USER_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", "user1");
        entry.add("sn", "user1");
        entry.add("cn", "user1");
        entry.add("userPassword", "password");
        entry.add("postalCode", userCode); // login in with this user
        entry.add("mail", userMail); // return this
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

        ldap.setRealm("LDAPRealm");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ldapServer.getLdapServer().getPort()));
        ldap.setBaseDN(DN);
        ldap.setBindDN(EmbeddedApacheDS.getBindDN());
        ldap.setBindPassword(EmbeddedApacheDS.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setLdapCache(new LdapCache(new AttributesCache(false, 0, 0, "0s"), new SearchResultsCache(false, 0, 0, "0s")));

        /*
         * Include both postalCode and mail in the filter b/c postalCode is used to login and mail
         * is necessary to find the user since it will be configured as the output property for all
         * of the user registry mappings.
         */
        ldap.setCustomFilters(new LdapFilters("(&(|(postalCode=%v)(mail=%v))(objectclass=inetorgperson))", null, "user:postalCode", null, null));

        FederatedRepository federatedRepository = LDAPFatUtils.createFederatedRepository(server, "LDAPRealm", new String[] { ldap.getBaseDN() });
        federatedRepository.getPrimaryRealm().setUserDisplayNameMapping(new RealmPropertyMapping("principalName", "mail"));
        federatedRepository.getPrimaryRealm().setUniqueUserIdMapping(new RealmPropertyMapping("principalName", "mail"));
        federatedRepository.getPrimaryRealm().setUserSecurityNameMapping(new RealmPropertyMapping("principalName", "mail"));

        server.getLdapRegistries().add(ldap);
        updateConfigDynamically(libertyServer, server);
    }

    /**
     * Log in the with the user's postalCode. Make sure all "get" security class map to the user's mail attribute.
     *
     * @throws Exception If there was an unexpected exception.
     */
    @Test
    public void testUserMap() throws Exception {

        /*
         * Login with the distinguished name. We expect that the call will return the mail address from
         * the call since the userSecurityName output mapping is configured to return the "mail" attribute.
         */
        String userSecurityName = servlet.checkPassword(userCode, "password");
        assertEquals("User did not return in expected format for checkPassword", userMail, userSecurityName);

        /*
         * See if the user is valid.
         */
        assertTrue("Expected '" + userCode + "' to be valid user.", servlet.isValidUser(userSecurityName));

        /*
         * Get the unique user ID using the security name returned above. We expect that the call will return
         * the mail address from the call since the uniqueUserId output mapping is configured to return the "mail"
         * attribute.
         */
        String uniqueUserId = servlet.getUniqueUserId(userSecurityName);
        assertEquals("User did not return in expected format for getUniqueUserId", userMail, uniqueUserId);

        /*
         * Get the security name using the unique user ID returned above. We expect that the call will return
         * the mail address from the call since the userSecurityName output mapping is configured to return the "mail"
         * attribute.
         *
         * This call requires that the user search filter includes the "mail" attribute.
         */
        userSecurityName = servlet.getUserSecurityName(uniqueUserId);
        assertEquals("User did not return in expected format for getUserSecurityName", userMail, userSecurityName);

        /*
         * Get the user display name using the security name returned above. We expect that the call will return
         * the mail address from the call since the userDisplayName output mapping is configured to return the "mail"
         * attribute.
         */
        String userDisplayName = servlet.getUserDisplayName(userSecurityName);
        assertEquals("User did not return in expected format for getUserDisplayName", userMail, userDisplayName);
    }
}