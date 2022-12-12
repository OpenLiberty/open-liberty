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

import static componenttest.topology.utils.LDAPFatUtils.assertDNsEqual;
import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.Attribute;
import com.ibm.websphere.simplicity.config.wim.AttributeConfiguration;
import com.ibm.websphere.simplicity.config.wim.AttributesCache;
import com.ibm.websphere.simplicity.config.wim.ExtendedProperty;
import com.ibm.websphere.simplicity.config.wim.FederatedRepository;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapFilters;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.LoginProperty;
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
import componenttest.topology.utils.LDAPFatUtils;

/**
 * Test the loginProperty. A user should be able to login with any loginProperty -
 * with or without the userFilter defined.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LoginPropertyTest {
    //Empty server.xml. We create the config here.
    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.loginproperty");
    private static final Class<?> c = LoginPropertyTest.class;
    private static UserRegistryServletConnection servlet;

    /**
     * This is the original ServerConfiguration we use. This is overridden in each test based
     * on the necessary config/properties.
     */
    private static ServerConfiguration originalConfiguration = null;

    private static InMemoryLDAPServer ds;
    private static final String DN = "o=ibm,c=us";
    private static final String userUid = "user1";
    private static final String USER_DN = "uid=" + userUid + "," + DN;
    private static final String userCn = "user1cn";
    private static final String userSn = "user1sn";

    /**
     * Setup the test case.
     *
     * @throws Exception If the setup failed for some reason.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        setupLibertyServer();
        setupldapServer();
    }

    /**
     * Tear down the test.
     */
    @AfterClass
    public static void teardownClass() throws Exception {
        try {
            if (libertyServer != null) {
                libertyServer.stopServer("CWIML4537E", "CWIML4505W", "CWIML4506E");
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
         * Backup the original server configuration.
         */
        originalConfiguration = libertyServer.getServerConfiguration();
    }

    /**
     * Configure the embedded LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupldapServer() throws Exception {
        ds = new InMemoryLDAPServer(DN);

        Entry entry = new Entry(DN);
        entry.addAttribute("objectclass", "top");
        entry.addAttribute("objectclass", "domain");
        ds.add(entry);

        /*
         * Create the user and group.
         */
        entry = new Entry(USER_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", userUid);
        entry.addAttribute("sn", userSn);
        entry.addAttribute("cn", userCn);
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

    }

    /**
     * Convenience method to configure the Liberty server with an {@link LdapRegistry} configuration that
     * will connect to {@link #ldapServer}.
     *
     * @throws Exception If there was an error configuring the server.
     */
    private static void updateLibertyServer(boolean addFilters, boolean addExtendedProperty, String... loginProps) throws Exception {
        ServerConfiguration server = originalConfiguration.clone();

        LdapRegistry ldap = new LdapRegistry();

        ldap.setRealm("LDAPRealm");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds.getLdapPort()));
        ldap.setBaseDN(DN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setLdapCache(new LdapCache(new AttributesCache(false, 0, 0, "0s"), new SearchResultsCache(false, 0, 0, "0s")));

        //Create an extended property, newproperty, and map it to SN.
        if (addExtendedProperty) {
            ldap.setAttributeConfiguration(new AttributeConfiguration());
            ldap.getAttributeConfiguration().getAttributes().add(new Attribute("sn", "newproperty", "PersonAccount", null, null));
            FederatedRepository federatedRepository = LDAPFatUtils.createFederatedRepository(server, "LDAPRealm", new String[] { ldap.getBaseDN() });
            ExtendedProperty ep = new ExtendedProperty();
            ep.setEntityType("PersonAccount");
            ep.setDataType("String");
            ep.setName("newproperty");
            federatedRepository.getExtendedProperties().add(ep);
        }
        for (String prop : loginProps) {
            ldap.addLoginProperty(new LoginProperty(prop));
        }

        if (addFilters) {
            ldap.setCustomFilters(new LdapFilters("(&(cn=%v)(objectclass=inetorgperson))", null, "user:postalCode", null, null));
        }
        server.getLdapRegistries().add(ldap);
        updateConfigDynamically(libertyServer, server);
    }

    /**
     * Configuration:
     *
     * UserFilter: not explicitly configured
     * LoginProperties: none
     *
     * Expected Results:
     *
     * User can login with UID, but not with CN or SN.
     *
     * Additionally, getUserDisplayName will return the principalName
     * which we expect to be UID.
     *
     * @throws Exception If there was an unexpected exception.
     */
    @Test
    public void testDefaultConfig() throws Exception {
        updateLibertyServer(false, false);
        assertDNsEqual("Authentication should succeed.",
                       USER_DN, servlet.checkPassword(userUid, "password"));
        assertNull("Authentication should not succeed.", servlet.checkPassword(userCn, "password"));
        assertNull("Authentication should not succeed.", servlet.checkPassword(userSn, "password"));
        assertEquals(userUid, servlet.getUserDisplayName(USER_DN));
    }

    /**
     * Configuration:
     *
     * UserFilter: (&(cn=%v)(objectclass=inetorgperson))
     * LoginProperties: none
     *
     * Expected Results:
     *
     * User can login with CN (userFilter), but not with UID or SN.
     *
     *
     * Additionally, getUserDisplayName will return the principalName
     * which we expect to be CN because of the userFilter.
     *
     * @throws Exception If there was an unexpected exception.
     */
    @Test
    public void testNoLoginPropertyWithFilter() throws Exception {
        updateLibertyServer(true, false);
        assertDNsEqual("Authentication should succeed.",
                       USER_DN, servlet.checkPassword(userCn, "password"));
        assertNull("Authentication should not succeed.", servlet.checkPassword(userUid, "password"));
        assertNull("Authentication should not succeed.", servlet.checkPassword(userSn, "password"));
        assertEquals(userCn, servlet.getUserDisplayName(USER_DN));
    }

    /**
     * Configuration:
     *
     * UserFilter: (&(cn=%v)(objectclass=inetorgperson))
     * LoginProperties: SN
     *
     * Expected Results:
     *
     * User can login with SN (loginProperty), but not with CN (userFilter) or UID.
     * When logging in with SN, a warning should be issued that indicates the dynamically
     * generated filter is used instead of the userFilter.
     *
     * Additionally, getUserDisplayName will return the principalName
     * which we expect to be SN because the loginProperty takes precedence.
     *
     * @throws Exception If there was an unexpected exception.
     */
    @Test
    public void testLoginPropertyWithFilter() throws Exception {
        updateLibertyServer(true, false, "sn");
        assertDNsEqual("Authentication should succeed.",
                       USER_DN, servlet.checkPassword(userSn, "password"));
        assertNotNull("Should find CWIML4505W message.", libertyServer.waitForStringInLog("CWIML4505W"));
        assertNull("Authentication should not succeed.", servlet.checkPassword(userCn, "password"));
        assertNull("Authentication should not succeed.", servlet.checkPassword(userUid, "password"));
        assertEquals(userSn, servlet.getUserDisplayName(USER_DN));
    }

    /**
     * Configuration:
     *
     * UserFilter: not explicitly configured
     * LoginProperties: SN
     *
     * Expected Results:
     *
     * User can login with SN, but not with CN or UID.
     *
     * Additionally, getUserDisplayName will return the principalName
     * which we expect to be SN.
     *
     * @throws Exception If there was an unexpected exception.
     */
    @Test
    public void testLoginProperty() throws Exception {
        updateLibertyServer(false, false, "sn");
        assertDNsEqual("Authentication should succeed.",
                       USER_DN, servlet.checkPassword(userSn, "password"));
        assertNull("Authentication should not succeed.", servlet.checkPassword(userUid, "password"));
        assertNull("Authentication should not succeed.", servlet.checkPassword(userCn, "password"));
        assertEquals(userSn, servlet.getUserDisplayName(USER_DN));
    }

    /**
     * Configuration:
     *
     * UserFilter: not explicitly configured
     * LoginProperties: CN, SN
     *
     * Expected Results:
     *
     * User can login with CN and SN, but not with UID.
     *
     * Additionally, getUserDisplayName will return the principalName
     * which we expect to be CN because this is the first listed loginProperty.
     *
     * @throws Exception If there was an unexpected exception.
     */
    @Test
    public void testMultipleLoginProperty() throws Exception {
        updateLibertyServer(false, false, "cn", "sn");
        assertDNsEqual("Authentication should succeed.",
                       USER_DN, servlet.checkPassword(userSn, "password"));
        assertDNsEqual("Authentication should succeed.",
                       USER_DN, servlet.checkPassword(userCn, "password"));
        assertNull("Authentication should not succeed.", servlet.checkPassword(userUid, "password"));
        assertEquals(userCn, servlet.getUserDisplayName(USER_DN));
    }

    /**
     * Define the loginProperty with a "bad" attribute (not a WIM PersonAccount property).
     * This should throw an INVALID_LOGIN_PROPERTIES error.
     *
     * Then update the config with extended property "newproperty" and map it to SN LDAP attribute.
     * Add "newproperty" as a loginProperty and login with SN. This should pass.
     *
     * @throws Exception If there was an unexpected exception.
     */
    @Test
    public void testBadLoginProperty() throws Exception {
        updateLibertyServer(false, false, "newproperty");
        assertNotNull("Should find CWIML4505W message.", libertyServer.waitForStringInLog("CWIML4506E"));
        updateLibertyServer(false, true, "newproperty");
        assertDNsEqual("Authentication should succeed.",
                       USER_DN, servlet.checkPassword(userSn, "password"));
    }
}