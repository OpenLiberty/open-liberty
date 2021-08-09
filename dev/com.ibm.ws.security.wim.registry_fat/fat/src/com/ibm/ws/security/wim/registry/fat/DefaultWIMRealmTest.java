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

package com.ibm.ws.security.wim.registry.fat;

import static componenttest.topology.utils.LDAPFatUtils.assertDNsEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemorySunLDAPServer;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class DefaultWIMRealmTest {

    @Server("com.ibm.ws.security.wim.registry.fat.DefaultWIMRealm")
    public static LibertyServer server;

    private static final Class<?> c = DefaultWIMRealmTest.class;
    private static UserRegistryServletConnection servlet;

    private static InMemorySunLDAPServer ldapServer;

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        setupLdapServer();
        setupLibertyServer();
    }

    /**
     * Setup the Liberty server. This server will start with very basic configuration. The tests
     * will configure the server dynamically.
     *
     * @throws Exception If there was an issue setting up the Liberty server.
     */
    public static void setupLibertyServer() throws Exception {
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
        server.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        server.addInstalledAppForValidation("userRegistry");

        /*
         * Update LDAP configuration with In-Memory Server
         */
        ServerConfiguration serverConfig = server.getServerConfiguration();
        LdapRegistry ldap = serverConfig.getLdapRegistries().get(0);
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ldapServer.getLdapPort()));
        ldap.setBindDN(InMemorySunLDAPServer.getBindDN());
        ldap.setBindPassword(InMemorySunLDAPServer.getBindPassword());
        server.updateServerConfiguration(serverConfig);

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
    }

    /**
     * Configure the embedded LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLdapServer() throws Exception {
        ldapServer = new InMemorySunLDAPServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        try {
            if (server != null) {
                server.stopServer("CWIML4537E");
            }
        } finally {
            try {
                if (ldapServer != null) {
                    ldapServer.shutDown(true);
                }
            } catch (Exception e) {
                Log.error(c, "teardown", e, "LDAP server threw error while shutting down. " + e.getMessage());
            }

            server.deleteFileFromLibertyInstallRoot("lib/features/testfileadapter-1.0.mf");
        }
    }

    /**
     * Hit the test servlet to see if getRealm works.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getRealm() throws Exception {
        Log.info(c, "getRealm", "Checking expected realm");
        assertEquals("WIMRegistry", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPassword() throws Exception {
        String user = "persona1";
        String password = "ppersona1";
        Log.info(c, "checkPassword", "Checking good credentials");
        assertDNsEqual("Authentication should succeed.",
                       "uid=persona1,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com", servlet.checkPassword(user, password));
    }

    /**
     * Hit the test servlet to see if checkPassword fails when passed in a invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithInvalidUser() {
        String user = "admin123";
        String password = "admin";
        Log.info(c, "checkPassword", "Checking good credentials");
        try {
            servlet.checkPassword(user, password);
        } catch (RegistryException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        server.waitForStringInLog("CWIML4537E");
        assertTrue("An invalid user should cause RegistryException with No principal is found message", true);
    }

    /**
     * Hit the test servlet to see if isValidUser works with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidUser() throws Exception {
        String user = "persona1";
        Log.info(c, "isValidUser", "Checking with a valid user");
        assertTrue("User validation should succeed.",
                   servlet.isValidUser(user));
    }

    /**
     * Hit the test servlet to see if isValidUser works with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidUserWithInvalidUser() throws Exception {
        String user = "invalidUser";
        Log.info(c, "isValidUserWithInvalidUser", "Checking with an invalid user");
        assertFalse("User validation should fail.",
                    servlet.isValidUser(user));
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsers() throws Exception {
        String user = "persona1";
        Log.info(c, "getUsers", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should only be one entry", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a invalid user pattern
     * and a limit of 2; should only expect to find no entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithInvalidUser() throws Exception {
        String user = "invalid";
        Log.info(c, "getUsersWithInvalidUser", "Checking with a invalid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should only be one entry", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when supplied with an invalid pattern for
     * the user pattern and a limit of 2; should expect to find no entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithValidPatternLimitLessThanZero() throws Exception {
        String user = "*";
        Log.info(c, "getUsersWithValidPatternLimitLessThanZero", "Checking with a valid pattern and limit of -1.");
        SearchResult result = servlet.getUsers(user, -1);
        assertEquals("There should be no entries", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserDisplayName() throws Exception {
        String user = "persona1";
        String displayName = "persona1";
        Log.info(c, "getUserDisplayName", "Checking with a valid user.");
        assertEquals(displayName, servlet.getUserDisplayName(user));
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works(Throws exception) when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserDisplayNameWithInvalidUser() {
        String user = "invalidUser";
        Log.info(c, "getUserDisplayNameWithInvalidUser", "Checking with an invalid user.");
        try {
            servlet.getUserDisplayName(user);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4001E")) {
                assertTrue("An invalid user should cause EntryNotFoundException", true);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user should cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getUniqueUserId works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueUserId() throws Exception {
        String user = "persona1";
        String uniqueUserId = "uid=persona1,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getUniqueUserId", "Checking with a valid user.");
        assertDNsEqual("UniqueUserId is incorrect", uniqueUserId, servlet.getUniqueUserId(user));
    }

    /**
     * Hit the test servlet to see if getUniqueUserId works when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueUserIdWithInvalidUser() {
        String user = "invalidUser";
        Log.info(c, "getUniqueUserIdWithInvalidUser", "Checking with an invalid user.");
        try {
            servlet.getUniqueUserId(user);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4001E")) {
                assertTrue("An invalid user should cause EntryNotFoundException", true);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user should cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityName() throws Exception {
        String uniqueUserId = "uid=persona1,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        String userSecurityName = "uid=persona1,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getUserSecurityName", "Checking with a valid user.");
        assertEquals(userSecurityName, servlet.getUserSecurityName(uniqueUserId));
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    @AllowedFFDC(value = { "java.lang.NullPointerException" })
    public void getUserSecurityNameWithInvalidUser() {
        String user = "uid=invalid,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getUserSecurityNameWithInvalidUser", "Checking with an invalid user.");
        try {
            servlet.getUserSecurityName(user);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4527E")) {
                assertTrue("An invalid user should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid user should cause EntryNotFoundException error CWIML4527E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user should cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with an invalid user (EntityOutOfRealmScope)
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityNameWithEntityOutOfRealmScope() {
        String user = "uid=invalid";
        Log.info(c, "getUserSecurityNameWithEntityOutOfRealmScope", "Checking with an invalid user.");
        try {
            servlet.getUserSecurityName(user);
        } catch (EntryNotFoundException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        } catch (RegistryException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        //server.waitForStringInLog("CWIML0515E"); //CWIML0515E The 'uid=testuser' entity is not in the scope of the 'defined' realm.
        assertTrue("An invalid user should cause EntryNotFoundException", true);
    }

    /**
     * Hit the test servlet to see if isValidGroup works with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroup() throws Exception {
        String group = "vmmgroup1";
        Log.info(c, "isValidGroup", "Checking with a valid group");
        assertTrue("Group validation should succeed.",
                   servlet.isValidGroup(group));
    }

    /**
     * Hit the test servlet to see if isValidGroup works with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroupWithInvalidGroup() throws Exception {
        String group = "invalidGroup";
        Log.info(c, "isValidGroupWithInvalidGroup", "Checking with an invalid group");
        assertFalse("Group validation should fail.",
                    servlet.isValidGroup(group));
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroups() throws Exception {
        String group = "vmmgroup1";
        Log.info(c, "getGroups", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        System.out.println("Groups : " + result.getList().toString());
        assertEquals("There should only be one entry", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a invalid group pattern
     * and a limit of 2; should only expect to find no entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithInvalidGroup() throws Exception {
        String group = "invalidgroup";
        Log.info(c, "getGroupsWithInvalidGroup", "Checking with a invalid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("There should only be one entry", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when supplied with an invalid pattern for
     * the group pattern and a limit of 2; should expect to find no entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithValidPatternLimitLessThanZero() throws Exception {
        String group = "vmmgroup1";
        Log.info(c, "getGroupsWithValidPatternLimitLessThanZero", "Checking with a valid pattern and limit of -1.");
        SearchResult result = servlet.getGroups(group, -1);
        assertEquals("There should be no entries", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayName() throws Exception {
        String group = "cn=vmmgroup1,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getGroupDisplayName", "Checking with a valid group.");
        assertEquals("vmmgroup1", servlet.getGroupDisplayName(group));
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    @AllowedFFDC(value = { "java.lang.NullPointerException" })
    public void getGroupDisplayNameWithInvalidGroup() {
        String group = "cn=invalidgroup,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getGroupDisplayNameWithInvalidGroup", "Checking with an invalid group.");
        try {
            servlet.getGroupDisplayName(group);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4527E")) {
                assertTrue("An invalid group should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid group should cause EntryNotFoundException error CWIML4527E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user group cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameWithEntityOutOfRealmScope() {
        String group = "cn=invalidgroup";
        Log.info(c, "getGroupDisplayNameWithEntityOutOfRealmScope", "Checking with an invalid group.");
        try {
            servlet.getGroupDisplayName(group);
        } catch (EntryNotFoundException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        } catch (RegistryException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        //server.waitForStringInLog("CWIML0515E");
        assertTrue("An invalid group should cause EntryNotFoundException", true);
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupId() throws Exception {
        String group = "vmmgroup1";
        String uniqueGroupId = "cn=vmmgroup1,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getUniqueGroupId", "Checking with a valid group.");
        assertDNsEqual("UniqueGroupId is incorrect", uniqueGroupId, servlet.getUniqueGroupId(group));
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdWithInvalidGroup() {
        String group = "invalidGroup";
        Log.info(c, "getUniqueGroupIdWithInvalidGroup", "Checking with an invalid group.");
        try {
            servlet.getUniqueGroupId(group);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4001E")) {
                assertTrue("An invalid group should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid group should cause EntryNotFoundException error CWIML4001E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user group cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityName() throws Exception {
        String uniqueGroupId = "vmmgroup1";
        Log.info(c, "getGroupSecurityName", "Checking with a valid group.");
        assertDNsEqual("Group name is incorrect", "cn=vmmgroup1,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com", servlet.getGroupSecurityName(uniqueGroupId));
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    @AllowedFFDC(value = { "java.lang.NullPointerException" })
    public void getGroupSecurityNameWithInvalidGroup() {
        String group = "cn=invalid,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getGroupSecurityNameWithInvalidGroup", "Checking with an invalid group.");
        try {
            servlet.getGroupSecurityName(group);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4001E")) {
                assertTrue("An invalid group should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid group should cause EntryNotFoundException error CWIML4001E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user group cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group (InvalidUniqueName)
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithInvalidUniqueName() {
        String group = "invalid";
        Log.info(c, "getGroupSecurityNameWithUniqueName", "Checking with an invalid group.");
        try {
            servlet.getGroupSecurityName(group);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4001E")) {
                assertTrue("An invalid group should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid group should cause EntryNotFoundException error CWIML4001E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user group cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group (EntityOutOfRealmScope)
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithEntityOutOfRealmScope() {
        String group = "uid=invalid";
        Log.info(c, "getGroupSecurityNameWithUniqueName", "Checking with an invalid group.");
        try {
            servlet.getGroupSecurityName(group);
        } catch (EntryNotFoundException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        } catch (RegistryException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        //server.waitForStringInLog("CWIML0515E");
        assertTrue("An invalid group should cause EntryNotFoundException", true);
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUser() throws Exception {
        String user = "vmmuser1";
        Log.info(c, "getGroupsForUser", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertEquals("There should only be one entry", 1, list.size());
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUserWithNoMembershipForUser() throws Exception {
        String user = "vmmtestuser";
        Log.info(c, "getGroupsForUserWithNoMembershipForUser", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertEquals("There should only be one entry", 0, list.size());
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a invalid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUserWithInvalidUser() {
        String user = "invalidUser";
        Log.info(c, "getGroupsForUserWithInvalidUser", "Checking with an invalid user.");
        try {
            servlet.getGroupsForUser(user);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4001E")) {
                assertTrue("An invalid group should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid group should cause EntryNotFoundException error CWIML4001E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user group cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIds() throws Exception {
        String user = "uid=vmmuser1,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getUniqueGroupIds", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertEquals("There should only be one entry", 1, list.size());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a invalid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    @AllowedFFDC(value = { "java.lang.NullPointerException" })
    public void getUniqueGroupIdsWithInvalidUser() {
        String user = "uid=invalid,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getUniqueGroupIdsForUser", "Checking with a valid user.");
        try {
            servlet.getUniqueGroupIdsForUser(user);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4527E")) {
                assertTrue("An invalid group should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid group should cause EntryNotFoundException error CWIML4527E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user group cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a invalid uniqueName.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsWithInvalidUniqueName() {
        String user = "invalid";
        Log.info(c, "getUniqueGroupIdsForUser", "Checking with a valid user.");
        try {
            servlet.getUniqueGroupIdsForUser(user);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4001E")) {
                assertTrue("An invalid group should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid group should cause EntryNotFoundException error CWIML4001E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user group cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a invalid user(EntityOutOfRealmScope).
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsWithEntityOutOfRealmScope() {
        String user = "uid=invalid";
        Log.info(c, "getUniqueGroupIdsForUser", "Checking with a valid user.");
        try {
            servlet.getUniqueGroupIdsForUser(user);
        } catch (EntryNotFoundException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        } catch (RegistryException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        //server.waitForStringInLog("CWIML0515E");
        assertTrue("An invalid user should cause EntryNotFoundException", true);
    }

}