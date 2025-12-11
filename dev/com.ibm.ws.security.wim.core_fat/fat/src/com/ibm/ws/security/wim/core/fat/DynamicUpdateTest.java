/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.core.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/**
 * Test dynamic change to the server configuration.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class DynamicUpdateTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.core.fat.dynamic");
    private static final Class<?> c = DynamicUpdateTest.class;
    private static UserRegistryServletConnection servlet = new UserRegistryServletConnection(server.getHostname(), server.getHttpDefaultPort());;

    private static final String USERNAME = "vmmtestuser";
    private static final String USER_PASSWORD = "vmmtestuserpwd";
    private static final String UNIQUE_NAME = "cn=vmmtestuser,o=ibm,c=us";
    private static final String UNIQUE_NAME_AD = "CN=vmmtestuser,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com@secondaryRealm";

    /*
     * private static final String CHANGE_PRIMARY_REALM_NAME = "dynamicUpdate/different_primary_realm_name.xml";
     * private static final String PRIMARY_REALM_WITH_MULTIPLE_REPO = "dynamicUpdate/primary_realm_with_multiple_repo.xml";
     * private static final String DEAULT_WIMREGISTRY_REALM = "dynamicUpdate/default_wimRegistry_realm.xml";
     */

    private static final String PRIMARY_SERVER_XML = "dynamicUpdate/primary_server.xml";
    private static final String WITHOUT_PRIMARY_REALM = "dynamicUpdate/without_primary_realm.xml";
    private static final String WITH_PRIMARY_REALM_WITH_UR_MAPPING = "dynamicUpdate/with_UR_mapping.xml";
    private static final String TWO_LDAPS_AND_ONE_UNDER_PRIMARY_REALM = "dynamicUpdate/multiple_ldaps_and_single_under_realm.xml";
    private static final String TWO_LDAPS_AND_TWO_UNDER_PRIMARY_REALM = "dynamicUpdate/multiple_ldaps_and_multiple_under_realm.xml";
    private static final String TWO_LDAPS_AND_TWO_REALMS = "dynamicUpdate/multiple_ldaps_and_multiple_realms.xml";
    protected static String serverConfigurationFile = PRIMARY_SERVER_XML;

    @BeforeClass
    public static void setUp() throws Exception {

        /*
         * Transform any applications into EE9 when necessary.
         */
        FATSuite.transformApps(server, "dropins/userRegistry.war");

        // Add LDAP variables to bootstrap properties file
        LDAPUtils.addLDAPVariables(server);
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");

        // install our user feature
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

        serverConfigurationFile = PRIMARY_SERVER_XML;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");

        try {
            server.stopServer("CWIML1018E", "CWIML4538E", "CWWKG0027W", "CWWKE1102W");
            //added "CWWKE1102W" to ignore server quiesce timeouts due to slow test machines
        } finally {
            server.removeInstalledAppForValidation("userRegistry");
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
        }
    }

    /**
     * This is an internal method used to set the server.xml
     */
    private static void setServerConfiguration(String serverXML) throws Exception {
        if (!serverConfigurationFile.equals(serverXML)) {
            // Update server.xml
            Log.info(c, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);

            // set a new mark
            server.setMarkToEndOfLog(server.getDefaultLogFile());

            // Update the server xml
            server.setServerConfigurationFile("/" + serverXML);

            // Wait for CWWKG0017I and CWWKF0008I to appear in logs after we started the config update
            Log.info(c, "setServerConfiguration",
                     "waitForStringInLogUsingMark: CWWKG0017I: The server configuration was successfully updated.");
            server.waitForStringInLogUsingMark("CWWKG0017I"); //CWWKG0017I: The server configuration was successfully updated in 0.2 seconds.

            serverConfigurationFile = serverXML;
        } else {
            Log.info(c, "setServerConfiguration", "serverConfig already set to " + serverXML + ", no config update.");
        }
    }

    /*
     * Test dynamic changes to realm.
     * 1. Login with primary realm present in configuration
     * 2. Remove primary realm from configuration
     * 3. Login again, this time it should login with user registries realm
     */
    @Test
    public void loginAfterRemovingPrimaryRealmTest() throws Exception {
        Log.info(c, "loginAfterRemovingPrimaryRealmTest", "Entering test loginAfterRemovingPrimaryRealmTest");

        //Change server configuration to add UR mapping attr under primary realm
        setServerConfiguration(PRIMARY_SERVER_XML);

        assertEquals("Authentication should succeed.", UNIQUE_NAME, servlet.checkPassword(USERNAME, USER_PASSWORD));

        //Change server configuration to remove primary realm
        setServerConfiguration(WITHOUT_PRIMARY_REALM);

        //assertEquals("SampleLdapIDSRealm", servlet.getRealm());
        assertEquals("Authentication should succeed.", UNIQUE_NAME, servlet.checkPassword(USERNAME, USER_PASSWORD));
    }

    /*
     * Test dynamic changes to realm.
     * 1. Call getRealm API. Value should be that of the explicitly set primary realm name.
     * 2. Remove primary realm from configuration
     * 3. Call getRealm API again. It should match that of the registry.
     */
    @Test
    public void getRealmAfterRemovingPrimaryRealm() throws Exception {
        Log.info(c, "getRealmAfterRemovingPrimaryRealm", "Checking expected realm");

        //Change server configuration to add UR mapping attr under primary realm
        setServerConfiguration(PRIMARY_SERVER_XML);
        assertEquals("defaultWIMFileBasedRealm", servlet.getRealm());

        //Change server configuration to remove primary realm
        setServerConfiguration(WITHOUT_PRIMARY_REALM);
        assertEquals("SampleLdapIDSRealm", servlet.getRealm());
    }

    /*
     * Test dynamic changes to realm.
     * 1. Call checkcPassword API
     * 2. Change server configuration to add UR mapping attr under primary realm
     * 3. Verify config is updated by calling checkPassword and getUsers
     */
    @Test
    public void changeFederatedRepositoryConfigDynamically() throws Exception {
        Log.info(c, "changeFederatedRepositoryConfigDynamically", "Entering test changeFederatedRepositoryConfigDynamically");

        //Change server configuration to remove primary realm
        setServerConfiguration(WITHOUT_PRIMARY_REALM);

        assertEquals("Authentication should succeed.", UNIQUE_NAME, servlet.checkPassword(USERNAME, USER_PASSWORD));

        //Change server configuration to add UR mapping attr under primary realm
        setServerConfiguration(WITH_PRIMARY_REALM_WITH_UR_MAPPING);

        assertEquals("Authentication should succeed.", USERNAME, servlet.checkPassword(USERNAME, USER_PASSWORD));

        try {
            // As maxSearchResults is changed to 5 we should get MaxSearchResultExceededException
            servlet.getUsers("*", 6);
            fail("Expected RegistryException.");
        } catch (RegistryException re) {
            String msg = re.getMessage();
            assertTrue("Expected a CWIML1018E exception message. Message: " + msg, msg.contains("CWIML1018E:"));
        }
    }

    /*
     * Test dynamic changes to realm.
     * 1. Call checkcPassword API
     * 2. Change server configuration to add one more ldapRegistry configuration, but do not add the same to participatingBaseEntry
     * 3. Call checkcPassword API again, it should be successful
     * 4. Change server configuration to add one more base entry to participatingBaseEntry
     * 5. Verify configuration is updated by calling checkPassword, getRealm and getUsers
     */
    @Test
    public void addParticipatingBaseEntryDynamically() throws Exception {
        Log.info(c, "addParticipatingBaseEntryDynamically", "Entering test addParticipatingBaseEntryDynamically");

        //Change server configuration to add UR mapping attr under primary realm
        setServerConfiguration(WITH_PRIMARY_REALM_WITH_UR_MAPPING);

        assertEquals("Authentication should succeed.", USERNAME, servlet.checkPassword(USERNAME, USER_PASSWORD));

        //Change server configuration to add one more ldapRegistry configuration, but do not add the same to participatingBaseEntry
        setServerConfiguration(TWO_LDAPS_AND_ONE_UNDER_PRIMARY_REALM);

        assertEquals("Authentication should succeed.", UNIQUE_NAME, servlet.checkPassword(USERNAME, USER_PASSWORD));

        //Change server configuration to add one more base entry to participatingBaseEntry
        setServerConfiguration(TWO_LDAPS_AND_TWO_UNDER_PRIMARY_REALM);

        assertEquals("multiple_repos", servlet.getRealm());

        assertNull("Authentication should succeed.", servlet.checkPassword(USERNAME, USER_PASSWORD));

        SearchResult result = servlet.getUsers(USERNAME, 4);
        assertEquals("There should only be 2 entries", 2, result.getList().size());
        server.waitForStringInLog("CWIML4538E"); //DuplicateLogonIdException
    }

    /*
     * Test dynamic changes to realm.
     * 1. Change the server config to have two ldapRegistry and two realms dynamically
     */
    @Test
    public void addOneMoreRealmDynamically() throws Exception {
        Log.info(c, "addOneMoreRealmDynamically", "Entering test addOneMoreRealmDynamically");

        //Change server configuration to add two ldapRegistry and two realms
        setServerConfiguration(TWO_LDAPS_AND_TWO_REALMS);

        assertEquals("Authentication should succeed.", UNIQUE_NAME, servlet.checkPassword(USERNAME, USER_PASSWORD));

        assertEquals("Authentication should succeed.", UNIQUE_NAME_AD.toLowerCase(),
                     servlet.checkPassword(USERNAME + "@secondaryRealm", USER_PASSWORD).toLowerCase());
    }
}