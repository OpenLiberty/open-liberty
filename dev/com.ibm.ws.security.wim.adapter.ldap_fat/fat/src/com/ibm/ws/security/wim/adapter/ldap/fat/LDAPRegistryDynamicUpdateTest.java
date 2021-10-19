/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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
import static componenttest.topology.utils.LDAPFatUtils.createADLdapRegistry;
import static componenttest.topology.utils.LDAPFatUtils.createFederatedRepository;
import static componenttest.topology.utils.LDAPFatUtils.createTDSLdapRegistry;
import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.Attribute;
import com.ibm.websphere.simplicity.config.wim.AttributeConfiguration;
import com.ibm.websphere.simplicity.config.wim.AttributesCache;
import com.ibm.websphere.simplicity.config.wim.ContextPool;
import com.ibm.websphere.simplicity.config.wim.ExternalIdAttribute;
import com.ibm.websphere.simplicity.config.wim.FailoverServers;
import com.ibm.websphere.simplicity.config.wim.FederatedRepository;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapEntityType;
import com.ibm.websphere.simplicity.config.wim.LdapFilters;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.RealmPropertyMapping;
import com.ibm.websphere.simplicity.config.wim.SearchResultsCache;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemorySunLDAPServer;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPFatUtils;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class LDAPRegistryDynamicUpdateTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.dynamic");
    private static final Class<?> c = LDAPRegistryDynamicUpdateTest.class;
    private static UserRegistryServletConnection servlet;

    private static final String USERNAME = "vmmtestuser";
    private static final String USER_PASSWORD = "vmmtestuserpwd";

    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
    private static ServerConfiguration emptyConfiguration = null;

    private static InMemorySunLDAPServer sunLdapServer;

    /**
     * Configure the embedded LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLdapServer() throws Exception {
        sunLdapServer = new InMemorySunLDAPServer();
    }

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        setupLdapServer();
        // Add LDAP variables to bootstrap properties file
        LDAPUtils.addLDAPVariables(server);
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
        server.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        server.addInstalledAppForValidation("userRegistry");
        server.startServer(c.getName() + ".log");

        //Make sure the application has come up before proceeding
        assertNotNull("Application userRegistry does not appear to have started.",
                      server.waitForStringInLog("CWWKZ0001I:.*userRegistry"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("Server did not came up",
                      server.waitForStringInLog("CWWKF0011I"));

        Log.info(c, "setUp", "Creating servlet connection the server");
        servlet = new UserRegistryServletConnection(server.getHostname(), server.getHttpDefaultPort());

        if (servlet.getRealm() == null) {
            Thread.sleep(5000);
            servlet.getRealm();
        }

        /*
         * The original server configuration has no registry or Federated Repository configuration.
         */
        emptyConfiguration = server.getServerConfiguration();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        try {
            server.stopServer("CWIMK0004E", "CWIML4537E", "CWIML4538E");
        } finally {
            try {
                if (sunLdapServer != null) {
                    sunLdapServer.shutDown(true);
                }
            } catch (Exception e) {
                Log.error(c, "teardown", e, "LDAP server threw error while shutting down. " + e.getMessage());
            }
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
        }
    }

    /*
     * Test dynamic changes for LDAP registry configuration
     * 1. Configure 2 LDAP registries dynamically
     * 2. Verify update to configuration is successful. Call checkPassword, it should fail and check for message CWIML4529E: in traces/
     * 3. Configure 1 LDAP registry dynamically
     * 4. Verify update to configuration is successful. Call checkPassword, it should be successful and should return uniqueName of user
     */
    @Test
    public void configureTwoLDAPsThenOneLDAPDynamically() throws Exception {
        Log.info(c, "configureTwoLDAPsThenOneLDAPDynamically", "Entering test configureTwoLDAPsThenOneLDAPDynamically");

        /*
         * Server is already started. Change server configuration to to have 2 LDAP registry configured.
         */
        ServerConfiguration clone = emptyConfiguration.clone();
        LdapRegistry ldap1 = createTDSLdapRegistry(clone, "LDAP_TDS", "SampleLdapIDSRealm");
        LdapRegistry ldap2 = createADLdapRegistry(clone, "LDAP_AD", "SampleLdapADRealm");
        createFederatedRepository(clone, "TwoLDAPRealm", new String[] { ldap1.getBaseDN(), ldap2.getBaseDN() });
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        assertNull("Authentication should not succeed.", servlet.checkPassword(USERNAME, USER_PASSWORD));
        assertEquals("TwoLDAPRealm", servlet.getRealm());
        server.waitForStringInLog("CWIML4538E"); // CWIML4538E: The user registry operation could not be completed. More than one record exists for the vmmtestuser principal name in the configured user registries. The principal name must be unique across all the user registries.

        /*
         * Now change server configuration to to have only LDAP registry configured.
         */
        clone = emptyConfiguration.clone();
        LdapRegistry ldap = createTDSLdapRegistry(clone, "LDAP", "SampleLdapIDSRealm");
        FederatedRepository federatedRepository = createFederatedRepository(clone, "OneLDAPRealm", new String[] { ldap.getBaseDN() });
        federatedRepository.getPrimaryRealm().setUniqueUserIdMapping(new RealmPropertyMapping("uniqueName", "uniqueName"));
        federatedRepository.getPrimaryRealm().setUserSecurityNameMapping(new RealmPropertyMapping("principalName", "principalName"));
        federatedRepository.getPrimaryRealm().setUserDisplayNameMapping(new RealmPropertyMapping("principalName", "principalName"));
        federatedRepository.getPrimaryRealm().setUniqueGroupIdMapping(new RealmPropertyMapping("uniqueName", "uniqueName"));
        federatedRepository.getPrimaryRealm().setGroupSecurityNameMapping(new RealmPropertyMapping("cn", "cn"));
        federatedRepository.getPrimaryRealm().setGroupDisplayNameMapping(new RealmPropertyMapping("cn", "cn"));
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        assertEquals("Authentication should succeed.", USERNAME, servlet.checkPassword(USERNAME, USER_PASSWORD));
        assertEquals("OneLDAPRealm", servlet.getRealm());
        assertDNsEqual("Unique names should be equal ", "cn=vmmtestuser,o=ibm,c=us", servlet.getUniqueUserId(USERNAME));
    }

    /*
     * Test dynamic changes for LDAP registry configuration
     * 1. Change the configuration to have 1 LDAP registry but using and different LDAP server and different base entries dynamically
     * 2. Verify update to configuration is successful. Call checkPassword and getRealm it should be successful
     */
    @Test
    public void changeLDAPServerConfigDynamically() throws Exception {
        Log.info(c, "changeLDAPServerConfigDynamically", "Entering test changeLDAPServerConfigDynamically");

        ServerConfiguration clone = emptyConfiguration.clone();
        LdapRegistry ldap = createTDSLdapRegistry(clone, "LDAP", "SampleLdapIDSRealm");
        createFederatedRepository(clone, "TDSRealm", new String[] { ldap.getBaseDN() });
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * Verify checkPassord and getRealm calls are successful
         */
        assertDNsEqual("Authentication should succeed.", "cn=vmmtestuser,o=ibm,c=us", servlet.checkPassword(USERNAME, USER_PASSWORD));
        assertEquals("TDSRealm", servlet.getRealm());

        /*
         * Change server configuration to to have only LDAP registry configured, but with different LDAP server.
         */
        clone = emptyConfiguration.clone();
        ldap = createSunLdapRegistry(clone, "LDAP", null, "o=vmm");
        createFederatedRepository(clone, "vmmldaprealm", new String[] { ldap.getName() });

        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        assertDNsEqual("Authentication should succeed.", "uid=vmmtestuser,ou=users,o=vmm", servlet.checkPassword(USERNAME, USER_PASSWORD));
        assertDNsEqual("Authentication should succeed.", "uid=persona1,ou=users,o=vmm", servlet.checkPassword("persona1", "ppersona1"));
        assertEquals("vmmldaprealm", servlet.getRealm());
    }

    /**
     * Helper method to fill in the InMemorySunLDAPServer info
     *
     * @param serverConfiguration
     * @param id
     * @param realm
     * @param name
     * @return
     */
    private static LdapRegistry createSunLdapRegistry(ServerConfiguration serverConfiguration, String id, String realm, String name) {
        return LDAPFatUtils.createSunLdapRegistry(serverConfiguration, id, realm, name, String.valueOf(sunLdapServer.getLdapPort()), InMemorySunLDAPServer.getBindDN(),
                                                  InMemorySunLDAPServer.getBindPassword());
    }

    /*
     * Test dynamic changes for LDAP registry configuration
     * 1. Change the configuration to have 1 LDAP registry with config for ldapEntityType, attribute, cache and context pool defined
     * 2. Verify update to configuration is successful.
     */
    @Test
    public void changeLDAPRegistryDetailedConfigDynamically() throws Exception {
        Log.info(c, "changeLDAPRegistryDetailedConfigDynamically", "Entering test changeLDAPRegistryDetailedConfigDynamically");

        /*
         * Change server configuration to to have only LDAP registry configured
         */
        ServerConfiguration clone = emptyConfiguration.clone();
        LdapRegistry ldap = createSunLdapRegistry(clone, "LDAP", null, "o=vmm");

        ldap.getLdapEntityTypes().add(new LdapEntityType("group", "(ObjectClass=ldapsubentry)", new String[] { "ldapsubentry" }, null));
        ldap.getLdapEntityTypes().add(new LdapEntityType("personAccount", null, new String[] { "inetOrgPerson" }, null));

        ldap.setAttributeConfiguration(new AttributeConfiguration());
        ldap.getAttributeConfiguration().getAttributes().add(new Attribute("userPassword", "password", "PersonAccount", null, null));
        ldap.getAttributeConfiguration().getAttributes().add(new Attribute("telephoneNumber", "cn", "PersonAccount", null, null));
        ldap.getAttributeConfiguration().getAttributes().add(new Attribute("krbPrincipalName", "kerberosId", "PersonAccount", null, null));

        ldap.setContextPool(new ContextPool(true, 1, 0, 3, "0s", "3000s"));
        ldap.setLdapCache(new LdapCache(new AttributesCache(true, 4000, 2000, "1200s"), new SearchResultsCache(true, 2000, 1000, "600s")));
        ldap.setFailoverServer(new FailoverServers("failoverLdapServers", new String[][] { { "localhost", String.valueOf(sunLdapServer.getLdapPort()) } }));

        FederatedRepository federatedRepository = createFederatedRepository(clone, "vmmldaprealm", new String[] { ldap.getName() });
        federatedRepository.getPrimaryRealm().setUserDisplayNameMapping(new RealmPropertyMapping("principalName", "cn"));
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * call checkPassword
         */
        assertDNsEqual("Authentication should succeed.", "uid=vmmtestuser,ou=users,o=vmm", servlet.checkPassword(USERNAME, USER_PASSWORD));

        /*
         * call getUserDisplayName and expect it to return telephone no as <userDisplayNameMapping propertyForInput="principalName" propertyForOutput="cn"/> and <attribute
         * name="telephoneNumber" propertyName="cn" entityType="PersonAccount" />
         */
        assertEquals("1 919 555 5555", servlet.getUserDisplayName("vmmtestuser"));

        /*
         * call getUniqueGroupIdsForUser and expect it to return 1 group, as it is member of one group
         */
        List<String> list = servlet.getUniqueGroupIdsForUser("uid=vmmuser1,ou=users,o=vmm");
        assertEquals("There should only be 1 entries", 1, list.size());
    }

    /*
     * Test dynamic changes for LDAP registry configuration
     * 1. Change the configuration to have 2 LDAP registries federated without federatedRepository tag in server xml
     * 2. Verify update to configuration is successful. Call checkPassword should not be successful and getUsers should return 2 users
     * 3. Change the configuration to have 1 LDAP registry deferated without federatedRepository tag in server xml
     * 4. Verify update to configuration is successful. Call checkPassword and getRealm should successful
     */
    @Test
    public void twoLDAPThenOneConfiguredWithoutFederationDynamically() throws Exception {
        Log.info(c, "twoLDAPThenOneConfiguredWithoutFederationDynamically", "Entering test twoLDAPThenOneConfiguredWithoutFederationDynamically");

        /*
         * Change server configuration to two LDAP registry configured without federatedRepository tag
         */
        ServerConfiguration clone = emptyConfiguration.clone();
        createSunLdapRegistry(clone, "LDAP", null, null);
        createADLdapRegistry(clone, "LDAP_AD", "SampleLdapADRealm");
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * call checkPassword
         */
        assertNull("Authentication should not succeed.", servlet.checkPassword(USERNAME, USER_PASSWORD));
        server.waitForStringInLog("CWIML4538E");

        /*
         * call getUsers
         */
        SearchResult result = servlet.getUsers("vmmtestuser", 5);
        assertEquals("There should only be 2 entries", 2, result.getList().size());

        /*
         * Change server configuration to 1 LDAP registry configured without federatedRepository tag
         */
        clone = emptyConfiguration.clone();
        createTDSLdapRegistry(clone, null, null);
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * call checkPassword
         */
        assertDNsEqual("Authentication should succeed.", "cn=vmmtestuser,o=ibm,c=us", servlet.checkPassword(USERNAME, USER_PASSWORD));
        assertEquals("LdapRegistry", servlet.getRealm());
    }

    /*
     * Test dynamic changes for LDAP registry configuration
     * 1. Change the configuration to add appSecurity-1.0 in features and remove appSecurity-2.0, servlet-1.0 and ldapRegistry-3.0
     * 2. Verify update to configuration is successful. Call checkPassword and getRealm should be successful
     */
    @Test
    public void updateAppSecurity2ToAppSecurity1FeatureTest() throws Exception {
        Log.info(c, "updateAppSecurity2ToAppSecurity1FeatureTest", "Entering test updateAppSecurity2ToAppSecurity1FeatureTest");

        /*
         * Add appSecurity-1.0 in features and remove appSecurity-2.0, servlet-3.0 and ldapRegistry-3.0. As servlet-1.0 is updated, it will cause application to restart
         */
        ServerConfiguration clone = emptyConfiguration.clone();
        clone.getFeatureManager().getFeatures().clear();
        clone.getFeatureManager().getFeatures().add("appSecurity-1.0");
        clone.getFeatureManager().getFeatures().add("securitylibertyinternals-1.0");

        LdapRegistry ldap = createTDSLdapRegistry(clone, null, null);
        createFederatedRepository(clone, "TDSRealm", new String[] { ldap.getBaseDN() });
        updateConfigDynamically(server, clone, true);

        /*
         * call checkPassword
         */
        assertDNsEqual("Authentication should succeed.", "cn=vmmtestuser,o=ibm,c=us", servlet.checkPassword(USERNAME, USER_PASSWORD));
        assertEquals("TDSRealm", servlet.getRealm());
    }

    /*
     * Test dynamic changes for LDAP registry configuration
     * 1. Change the userFilter config to have mail as login property
     * 2. Verify update to configuration is successful. Call checkPassword with mail, it should be successful and with uid it should fail
     * 3. Call getUserDisplayName to get output as mail
     */
    @Test
    public void ldapConfigUpdateLoginPropertyTest() throws Exception {
        Log.info(c, "ldapConfigUpdateLoginPropertyTest", "Entering test ldapConfigUpdateLoginPropertyTest");

        /*
         * Set original server config
         */
        ServerConfiguration clone = emptyConfiguration.clone();
        LdapRegistry ldap = createTDSLdapRegistry(clone, "LDAP", "SampleLdapIDSRealm");
        createFederatedRepository(clone, "TDSRealm", new String[] { ldap.getBaseDN() });
        updateConfigDynamically(server, clone, true);

        /*
         * Verify checkPassord and getRealm calls are successful
         */
        assertDNsEqual("Authentication should succeed.", "cn=vmmtestuser,o=ibm,c=us", servlet.checkPassword(USERNAME, USER_PASSWORD));
        assertEquals("TDSRealm", servlet.getRealm());

        /*
         * Change server config to have mail as principal name rather than uid (userFilter="(&(mail=%v)(objectclass=ePerson))")
         */
        clone = emptyConfiguration.clone();
        ldap = createTDSLdapRegistry(clone, "LDAP", "SampleLdapIDSRealm");
        ldap.setIdsFilters(new LdapFilters("(&(mail=%v)(objectclass=ePerson))", "(&(cn=%v)(|(objectclass=groupOfNames)(objectclass=groupOfUniqueNames)(objectclass=groupOfURLs)))", "*:uid", "*:cn", "groupOfNames:member;groupOfUniqueNames:uniqueMember"));
        createFederatedRepository(clone, "TDSRealm", new String[] { ldap.getBaseDN() });
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * Login should be successful with input as mail
         */
        assertDNsEqual("Authentication should succeed as mail.", "cn=vmmLibertyUser,o=ibm,c=us", servlet.checkPassword("vmmLibertyUser@ibm.com", "vmmLibertyUser"));

        /*
         * And should fail with input as uid
         */
        assertNull("Authentication should not succeed.", servlet.checkPassword("vmmuser1", "vmmuser1"));
        server.waitForStringInLog("CWIML4537E");

        /*
         * Call getUserDisplayName to get principalName(mail) as output
         */
        assertEquals("vmmtestuser@ibm.com", servlet.getUserDisplayName("cn=vmmtestuser,o=ibm,c=us"));
    }

    /*
     * Test dynamic changes for LDAP registry configuration
     * 1. Change the userFilter config to have sn and mail as login properties
     * 2. Verify update to configuration is successful. Call checkPassword with sn and mail, it should be successful and with uid it should fail
     * 3. Call getUserDisplayName to get output as sn, as it is first login property
     */
    @Test
    public void ldapConfigMultipleLoginPropertiesTest() throws Exception {
        Log.info(c, "ldapConfigMultipleLoginPropertiesTest", "Entering test ldapConfigMultipleLoginPropertiesTest");

        /*
         * Change server config to have sn and mail as principal name rather than uid (userFilter="(&(|(sn=%v)(mail=%v))(objectclass=ePerson))")
         */
        ServerConfiguration clone = emptyConfiguration.clone();
        LdapRegistry ldap = createTDSLdapRegistry(clone, "LDAP", "SampleLdapIDSRealm");
        ldap.setIdsFilters(new LdapFilters("(&(|(sn=%v)(mail=%v))(objectclass=ePerson))", "(&(cn=%v)(|(objectclass=groupOfNames)(objectclass=groupOfUniqueNames)(objectclass=groupOfURLs)))", "*:uid", "*:cn", "groupOfNames:member;groupOfUniqueNames:uniqueMember"));
        createFederatedRepository(clone, "TDSRealm", new String[] { ldap.getBaseDN() });
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * Login should be successful with input as sn
         */
        assertDNsEqual("Authentication should succeed with sn.", "cn=vmmLibertyUser,o=ibm,c=us", servlet.checkPassword("vmmLibertyUserSN", "vmmLibertyUser"));

        /*
         * Login should be successful with input as mail
         */
        assertDNsEqual("Authentication should succeed with mail.", "cn=vmmLibertyUser,o=ibm,c=us", servlet.checkPassword("vmmLibertyUser@ibm.com", "vmmLibertyUser"));

        /*
         * And should fail with input as uid
         */
        assertNull("Authentication should not succeed with uid.", servlet.checkPassword("vmmuser1", "vmmuser1"));
        server.waitForStringInLog("CWIML4537E");

        /*
         * Call getUserDisplayName to get principalName(sn) as output as it is first login property
         */
        assertEquals("vmmuser1sn", servlet.getUserDisplayName("cn=vmmuser1,o=ibm,c=us"));
    }

    /*
     * Test dynamic changes for LDAP registry configuration
     * 1. Change config dynamically to have original server config and verify that getUserDisplayName, getUniqueUserId, getUserSecurityName to get pricipalName, uniqueName and
     * uniqueName respectively
     * 2. Now change the server config to have telephonenumber attr mapped to photoURL property and outputProperty for userDisplyName is photoURL and mail attr mapped to
     * photoURLThumbnail property and outputProperty for uniqueUserId is photoURLThumbnail
     * 3. Call getUserDisplayName, getUniqueUserId, getUserSecurityName to get telephonenumber, mail and mail respectively
     */
    @Test
    public void dynamicallyUpdateLdapConfigToHaveAttrMapping() throws Exception {
        Log.info(c, "dynamicallyUpdateLdapConfigToHaveAttrMapping", "Entering test dynamicallyUpdateLdapConfigToHaveAttrMapping");

        /*
         * Set original server config
         */
        ServerConfiguration clone = emptyConfiguration.clone();
        LdapRegistry ldap = createTDSLdapRegistry(clone, "LDAP", "SampleLdapIDSRealm");
        createFederatedRepository(clone, "TDSRealm", new String[] { ldap.getBaseDN() });
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * Verify that getUserDisplayName, getUniqueUserId, getUserSecurityName to get pricipalName,
         * uniqueName and uniqueName respectively
         */
        assertEquals("vmmtestuser", servlet.getUserDisplayName("vmmtestuser"));
        assertDNsEqual(null, "cn=vmmtestuser,o=ibm,c=us", servlet.getUniqueUserId("vmmtestuser"));
        assertDNsEqual(null, "cn=vmmtestuser,o=ibm,c=us", servlet.getUserSecurityName("vmmtestuser"));

        /*
         * Now change the server config to have telephonenumber attr mapped to photoURL property and
         * outputProperty for userDisplyName is photoURL and mail attr mapped to photoURLThumbnail
         * property and outputProperty for uniqueUserId is photoURLThumbnail
         */
        clone = clone.clone();

        AttributeConfiguration attributeConfiguration = new AttributeConfiguration();
        attributeConfiguration.getAttributes().add(new Attribute("telephoneNumber", "photoURL", "PersonAccount", null, null));
        attributeConfiguration.getAttributes().add(new Attribute("sn", "photoURLThumbnail", "PersonAccount", null, null));
        clone.getLdapRegistries().get(0).setAttributeConfiguration(attributeConfiguration);

        FederatedRepository federatedRepository = createFederatedRepository(clone, "TDSRealm", new String[] { ldap.getBaseDN() });
        federatedRepository.getPrimaryRealm().setUserDisplayNameMapping(new RealmPropertyMapping("photoURL", "photoURL"));
        federatedRepository.getPrimaryRealm().setUniqueUserIdMapping(new RealmPropertyMapping("photoURLThumbnail", "photoURLThumbnail"));
        federatedRepository.getPrimaryRealm().setUserSecurityNameMapping(new RealmPropertyMapping("cn", "cn"));
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * getUserDisplayName --> photoURL (mapped to telephoneNumber)
         * getUniqueUserId --> photURLThumbnail (mapped to sn)
         * getUserSecurityName --> cn
         */
        String userSecurityName = "vmmtestuser";
        String userDisplayName = "1 919 555 5555";
        String uniqueUserId = "vmmtestusersn";
        assertEquals(userDisplayName, servlet.getUserDisplayName(userSecurityName));
        assertEquals(uniqueUserId, servlet.getUniqueUserId(userSecurityName));
        assertEquals(userSecurityName, servlet.getUserSecurityName(uniqueUserId));
    }

    /*
     * Test dynamic changes for LDAP registry configuration
     * 1. Change config dynamically to have original server config and verify that getUserDisplayName, getUniqueUserId, getUserSecurityName to get pricipalName, uniqueName and
     * uniqueName respectively
     * 2. Now change the server config to have postOfficeBox attr mapped to photoURL property and outputProperty for userDisplyName is photoURL.
     * Also change userFilters to have photoURL as login property, and as photoURL us mapped to postOfficeBox attribute from LDAP. postOfficeBox will act as login property
     * 3. Call checkpassword with input as value of postOfficeBox to successfully return it postOfficeBox value and getUserDisplayName to get postOfficeBox.
     */
    @Test
    @Ignore("User cn=vmmattruser,o=ibm,c=us does not exist.")
    public void updateLdapConfigToHaveNonVMMPropertyAsLoginProperty() throws Exception {
        Log.info(c, "updateLdapConfigToHaveNonVMMPropertyAsLoginProperty", "Entering test updateLdapConfigToHaveNonVMMPropertyAsLoginProperty");

        /*
         * Set original server config
         */
        ServerConfiguration clone = emptyConfiguration.clone();
        LdapRegistry ldap = createTDSLdapRegistry(clone, "LDAP", "SampleLdapIDSRealm");
        createFederatedRepository(clone, "TDSRealm", new String[] { ldap.getBaseDN() });
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * Verify that getUserDisplayName, getUniqueUserId, getUserSecurityName to get pricipalName, uniqueName and uniqueName respectively
         */
        assertEquals("vmmattruser", servlet.getUserDisplayName("vmmattruser"));
        assertDNsEqual(null, "cn=vmmattruser,o=ibm,c=us", servlet.getUniqueUserId("vmmattruser"));
        assertDNsEqual(null, "cn=vmmattruser,o=ibm,c=us", servlet.getUserSecurityName("vmmattruser"));

        /*
         * Now change the server config to have postOfficeBox attr mapped to photoURL property and outputProperty for userDisplyName is photoURL. Also change userFilters to have
         * photoURL as login property, and as photoURL us mapped to postOfficeBox attribute from LDAP. postOfficeBox will act as login property
         */
        clone = emptyConfiguration.clone();
        ldap = createTDSLdapRegistry(clone, "LDAP", "SampleLdapIDSRealm");

        ldap.setHost("${ldap.server.4.name}");
        ldap.setPort("${ldap.server.4.port}");
        ldap.setBindDN("${ldap.server.4.bindDN}");
        ldap.setBindPassword("${ldap.server.4.bindPassword}");
        ldap.setFailoverServer(new FailoverServers("failoverLdapServers", new String[][] { { "${ldap.server.1.name}", "${ldap.server.1.port}" },
                                                                                           { "${ldap.server.5.name}", "${ldap.server.5.port}" } }));
        ldap.setIdsFilters(new LdapFilters("(&(photoURL=%v)(objectclass=ePerson))", null, "*:photoURL", null, null));

        ldap.setAttributeConfiguration(new AttributeConfiguration());
        ldap.getAttributeConfiguration().getAttributes().add(new Attribute("postOfficeBox", "photoURL", "PersonAccount", null, null));

        FederatedRepository federatedRepository = createFederatedRepository(clone, "TDSRealm", new String[] { ldap.getBaseDN() });
        federatedRepository.getPrimaryRealm().setUserDisplayNameMapping(new RealmPropertyMapping("principalName", "photoURL"));
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * Call checkpassword with input as postOfficeBox and password to get postOfficeBox as return from it and getUserDisplayName to get postOfficeBox
         */
        assertDNsEqual("Authentication should succeed.", "cn=vmmattruser,o=ibm,c=us", servlet.checkPassword("chinchwad", "vmmattruserpwd"));
        assertEquals("chinchwad", servlet.getUserDisplayName("vmmattruser")); //photoURL is mapped to postOfficeBox
    }

    /*
     * Test case sensitive authentication.
     */
    @Test
    public void testAuthenticationCase() throws Exception {
        Log.info(c, "testAuthenticationCase", "Entering test testAuthenticationCase");

        /*
         * Update to ignore case.
         */
        ServerConfiguration clone = emptyConfiguration.clone();
        LdapRegistry ldap = createTDSLdapRegistry(clone, "LDAP", "SampleLdapIDSRealm");
        createFederatedRepository(clone, "TDSRealm", new String[] { ldap.getBaseDN() });
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * Verify checkPassord call is successful
         */
        assertDNsEqual("Authentication should succeed.", "cn=vmmtestuser,o=ibm,c=us", servlet.checkPassword(USERNAME.toUpperCase(), USER_PASSWORD));

        /*
         * Update to NOT ignore case.
         */
        clone = clone.clone();
        clone.getLdapRegistries().get(0).setIgnoreCase(false);
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * Verify checkPassord call is successful
         */
        assertNull("Authentication should fail.", servlet.checkPassword(USERNAME.toUpperCase(), USER_PASSWORD));
    }

    /*
     * Test External Id mapping.
     */
    @Test
    public void testExternalId() throws Exception {
        Log.info(c, "testExternalId", "Entering test testExternalId");

        // Assume.assumeTrue(LDAPUtils.USE_LOCAL_LDAP_SERVER);

        /*
         * No external ID mapping.
         */
        ServerConfiguration clone = emptyConfiguration.clone();
        createSunLdapRegistry(clone, "LDAP", null, null);
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * Verify checkPassord call is successful
         */
        assertEquals("Authentication should succeed.", "uid=noNSUID,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com", servlet.checkPassword("noNSUID", "password"));

        /*
         * Map the external ID.
         */
        clone = clone.clone();
        clone.getLdapRegistries().get(0).setAttributeConfiguration(new AttributeConfiguration());
        clone.getLdapRegistries().get(0).getAttributeConfiguration().getExternalIdAttributes().add(new ExternalIdAttribute("cn", "PersonAccount", null, false));

        FederatedRepository federatedRepository = createFederatedRepository(clone, "SampleLdapSUNRealm", new String[] { "dc=rtp,dc=raleigh,dc=ibm,dc=com" });
        federatedRepository.getPrimaryRealm().setUserSecurityNameMapping(new RealmPropertyMapping("principalName", "uniqueId"));

        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * Verify checkPassord call is successful
         */
        String result = servlet.checkPassword("noNSUID", "password");
        System.out.println("result = " + result);
        assertEquals("Authentication should succeed.", "noNSUIDCN", result);
    }

    @Test
    public void testImplicitEmptyParticipatingBaseEntry() throws Exception {
        Log.info(c, "testImplicitEmptyParticipatingBaseEntry", "Entering test testImplicitEmptyParticipatingBaseEntry");

        /*
         * This will test an empty base entry for an LDAP registry without explicitly providing
         * federated repository configuration.
         */
        ServerConfiguration clone = emptyConfiguration.clone();
        LdapRegistry ldap = createTDSLdapRegistry(clone, "LDAP", "SampleLdapIDSRealm");
        ldap.setBaseDN("");
        ldap.setRecursiveSearch(true);
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * Validate the new configuration.
         */
        assertEquals("SampleLdapIDSRealm", servlet.getRealm());

        assertTrue("User validation should succeed.", servlet.isValidUser("vmmtestuser"));
        assertEquals("There should only be one entry", 1, servlet.getUsers("vmmtestuser", 2).getList().size());
        assertEquals("vmmtestuser", servlet.getUserDisplayName("cn=vmmtestuser,o=ibm,c=us"));
        assertDNsEqual("getUniqueUserId returned incorrect value", "cn=vmmtestuser,o=ibm,c=us", servlet.getUniqueUserId("vmmtestuser"));
        assertEquals("getUserSecurityName returned incorrect value", "cn=vmmtestuser,o=ibm,c=us", servlet.getUserSecurityName("vmmtestuser"));
        List<String> groupsForUser = servlet.getGroupsForUser("vmmuser1");
        assertTrue("Expected group 'vmmgroup1' returned for user. Results were: " + groupsForUser, groupsForUser.contains("cn=vmmgroup1,o=ibm,c=us"));

        assertTrue("Group validation should succeed.", servlet.isValidGroup("vmmgrp1"));
        assertEquals("There should only be one entry", 1, servlet.getGroups("vmmgrp1", 2).getList().size());
        assertEquals("vmmgrp1", servlet.getGroupDisplayName("cn=vmmgrp1,o=ibm,c=us"));
        assertDNsEqual("getUniqueGroupId returned incorrect value", "cn=vmmgrp1,o=ibm,c=us", servlet.getUniqueGroupId("vmmgrp1"));
        assertDNsEqual("getGroupSecurityName returned incorrect value", "cn=vmmgrp1,o=ibm,c=us", servlet.getGroupSecurityName("vmmgrp1"));
        List<String> usersForGroup = servlet.getUsersForGroup("vmmgroup1", 0).getList();
        assertTrue("Expected user 'vmmuser1' in group. Results were: " + usersForGroup, usersForGroup.contains("cn=vmmuser1,o=ibm,c=us"));
    }

    @Test
    public void testExplicitEmptyParticipatingBaseEntry() throws Exception {
        Log.info(c, "testExplicitEmptyParticipatingBaseEntry", "Entering test testExplicitEmptyParticipatingBaseEntry");

        /*
         * This will test an empty base entry for an LDAP registry by explicitly providing
         * federated repository configuration (with an empty participatingBaseEntry).
         */
        ServerConfiguration clone = emptyConfiguration.clone();
        LdapRegistry ldap = createTDSLdapRegistry(clone, "LDAP", "SampleLdapIDSRealm");
        ldap.setBaseDN("");
        ldap.setRecursiveSearch(true);
        createFederatedRepository(clone, "FederatedRealm", new String[] { "" });
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        /*
         * Validate the new configuration.
         */
        assertEquals("FederatedRealm", servlet.getRealm());

        assertTrue("User validation should succeed.", servlet.isValidUser("vmmtestuser"));
        assertEquals("There should only be one entry", 1, servlet.getUsers("vmmtestuser", 2).getList().size());
        assertEquals("vmmtestuser", servlet.getUserDisplayName("cn=vmmtestuser,o=ibm,c=us"));
        assertDNsEqual("getUniqueUserId returned incorrect value", "cn=vmmtestuser,o=ibm,c=us", servlet.getUniqueUserId("vmmtestuser"));
        assertEquals("getUserSecurityName returned incorrect value", "cn=vmmtestuser,o=ibm,c=us", servlet.getUserSecurityName("vmmtestuser"));
        List<String> groupsForUser = servlet.getGroupsForUser("vmmuser1");
        assertTrue("Expected group 'vmmgroup1' returned for user. Results were: " + groupsForUser, groupsForUser.contains("cn=vmmgroup1,o=ibm,c=us"));

        assertTrue("Group validation should succeed.", servlet.isValidGroup("vmmgrp1"));
        assertEquals("There should only be one entry", 1, servlet.getGroups("vmmgrp1", 2).getList().size());
        assertEquals("vmmgrp1", servlet.getGroupDisplayName("cn=vmmgrp1,o=ibm,c=us"));
        assertDNsEqual("getUniqueGroupId returned incorrect value", "cn=vmmgrp1,o=ibm,c=us", servlet.getUniqueGroupId("vmmgrp1"));
        assertDNsEqual("getGroupSecurityName returned incorrect value", "cn=vmmgrp1,o=ibm,c=us", servlet.getGroupSecurityName("vmmgrp1"));
        List<String> usersForGroup = servlet.getUsersForGroup("vmmgroup1", 0).getList();
        assertTrue("Expected user 'vmmuser1' in group. Results were: " + usersForGroup, usersForGroup.contains("cn=vmmuser1,o=ibm,c=us"));
    }

    @Test
    public void testCustomContextAndCache() throws Exception { // for issue 4813
        Log.info(c, "testCustomContextAndCache", "Entering test testCustomContextAndCache");

        ServerConfiguration clone = emptyConfiguration.clone();
        LdapRegistry ldap = createTDSLdapRegistry(clone, "LDAP", "SampleLdapIDSRealm");
        ldap.setContextPool(new ContextPool(true, 17, 19, 18, "1700ms", "1600ms"));
        ldap.setLdapCache(new LdapCache(new AttributesCache(true, 4444, 2222, "700s"), new SearchResultsCache(true, 5555, 3333, "777s")));
        createFederatedRepository(clone, "OneLDAPRealm", new String[] { ldap.getBaseDN() });
        updateConfigDynamically(server, clone, shouldWaitForAppToStart(clone));

        assertEquals("OneLDAPRealm", servlet.getRealm());

        /*
         * Checking that these are correctly logged in the trace
         * Issue 4813 found that the pool timeout was not being loaded correctly
         * <contextPool enabled="true" initialSize="17" maxSize="19" timeout="1700ms" waitTime="1600ms" preferredSize="18"/>
         */
        String tr = "InitPoolSize: 17";
        List<String> errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "MaxPoolSize: 19";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "PrefPoolSize: 18";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "PoolTimeOut: 2"; // Rounded from 1700ms to 2s.
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "PoolWaitTime: 1600";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        Log.info(c, "testCustomContextAndCache", "Check cache config settings");
        // <searchResultsCache size="5555" timeout="777s" enabled="true" resultsSizeLimit="3333" />

        tr = "CacheTimeOut: 700000";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "CacheSize: 4444";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "CacheSizeLimit: 2222";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "CacheTimeOut: 777000";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "CacheSize: 5555";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "CacheResultSizeLimit: 3333";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());
    }

    /**
     * Determine whether we should wait for the user registry servlet to start. Some of the tests remove the
     * servlet feature and we need to wait for it to reload or we will fail.
     *
     * @param updated The new configuration for the server.
     * @return True if we should wait for the user registry servlet to start.
     * @throws Exception If we failed to get the current server configuration.
     */
    private static boolean shouldWaitForAppToStart(ServerConfiguration updated) throws Exception {
        return !server.getServerConfiguration().getFeatureManager().getFeatures().contains("servlet-3.1") &&
               updated.getFeatureManager().getFeatures().contains("servlet-3.1");
    }
}
