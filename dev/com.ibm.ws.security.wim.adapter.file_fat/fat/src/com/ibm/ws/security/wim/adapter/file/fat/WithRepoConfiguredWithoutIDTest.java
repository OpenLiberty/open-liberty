/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.file.fat;

import static componenttest.topology.utils.LDAPFatUtils.assertDNsEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class WithRepoConfiguredWithoutIDTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.file.fat.repowithoutid");
    private static final Class<?> c = WithRepoConfiguredWithoutIDTest.class;
    private static UserRegistryServletConnection servlet;

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");

        server.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");

        // Copy internal feature and bundle to liberty root
        server.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/testfileadapter-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib", "com.ibm.ws.security.wim.adapter.file_1.0.jar");

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

    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        try {
            server.stopServer("CWIML4537E");
        } finally {
            server.deleteFileFromLibertyInstallRoot("lib/features/testfileadapter-1.0.mf");
            server.deleteFileFromLibertyInstallRoot("lib/com.ibm.ws.security.wim.adapter.file_1.0.jar");
        }
    }

    /**
     * Hit the test servlet to see if getRealm works.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getRealm() throws Exception {
        Log.info(c, "getRealm", "Checking expected realm");
        assertEquals("defaultWIMFileBasedRealm", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPassword() throws Exception {
        String user = "admin";
        String password = "admin";
        Log.info(c, "checkPassword", "Checking good credentials");
        assertEquals("Authentication should succeed.",
                     "admin", servlet.checkPassword(user, password));
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
        String user = "admin";
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
        String user = "admin";
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
        String user = "admin";
        String displayName = "admin";
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
            } else {
                assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E", false);
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
        String user = "admin";
        String uniqueUserId = "uid=admin,o=defaultWIMFileBasedRealm";
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
            } else {
                assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E", false);
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
        String user = "uid=admin,o=defaultWIMFileBasedRealm";
        // String uniqueUserId = "uid=admin,o=defaultWIMFileBasedRealm";
        String uniqueUserId = "admin";
        Log.info(c, "getUserSecurityName", "Checking with a valid user.");
        assertEquals(uniqueUserId, servlet.getUserSecurityName(user));
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityNameWithInvalidUser() {
        String user = "uid=invalid,o=defaultWIMFileBasedRealm";
        Log.info(c, "getUserSecurityNameWithInvalidUser", "Checking with an invalid user.");
        try {
            servlet.getUserSecurityName(user);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4001E")) {
                assertTrue("An invalid user should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E", false);
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
        } catch (RegistryException e) {
            fail("An invalid user should cause EntryNotFoundException error CWIML0515E");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML0515E", errorMessage.contains("CWIML0515E"));
        }
    }

    /**
     * Hit the test servlet to see if isValidGroup works with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroup() throws Exception {
        String group = "group1";
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
        String group = "group1";
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
        String group = "group1";
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
        String group = "cn=group1,o=defaultWIMFileBasedRealm";
        Log.info(c, "getGroupDisplayName", "Checking with a valid group.");
        assertEquals("group1", servlet.getGroupDisplayName(group));
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameWithInvalidGroup() {
        String group = "cn=invalidgroup,o=defaultWIMFileBasedRealm";
        Log.info(c, "getGroupDisplayNameWithInvalidGroup", "Checking with an invalid group.");
        try {
            servlet.getGroupDisplayName(group);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4001E")) {
                assertTrue("An invalid user should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user should cause EntryNotFoundException", false);
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
            assertTrue("An invalid user should cause RegistryException", false);
        } catch (RegistryException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML0515E")) {
                assertTrue("An invalid user should cause RegistryException", true);
            } else {
                assertTrue("An invalid user should cause RegistryException error CWIML0515E", false);
            }
        }
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupId() throws Exception {
        String group = "group1";
        String uniqueGroupId = "cn=group1,o=defaultWIMFileBasedRealm";
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
                assertTrue("An invalid user should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user should cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityName() throws Exception {
        String uniqueGroupId = "cn=group1,o=defaultWIMFileBasedRealm";
        Log.info(c, "getGroupSecurityName", "Checking with a valid group.");
        assertEquals("group1", servlet.getGroupSecurityName(uniqueGroupId));
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithInvalidGroup() {
        String group = "cn=invalid,o=defaultWIMFileBasedRealm";
        Log.info(c, "getGroupSecurityNameWithInvalidGroup", "Checking with an invalid group.");
        try {
            servlet.getGroupSecurityName(group);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4001E")) {
                assertTrue("An invalid user should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user should cause EntryNotFoundException", false);
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
                assertTrue("An invalid user should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user should cause EntryNotFoundException", false);
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
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML0515E")) {
                assertTrue("An invalid user should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid user should cause EntryNotFoundException error CWIML0515E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user should cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUser() throws Exception {
        String user = "user1";
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
        String user = "admin";
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
                assertTrue("An invalid user should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user should cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIds() throws Exception {
        String user = "uid=user1,o=defaultWIMFileBasedRealm";
        Log.info(c, "getUniqueGroupIds", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertEquals("There should only be one entry", 1, list.size());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a invalid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsWithInvalidUser() {
        String user = "uid=invalid,o=defaultWIMFileBasedRealm";
        Log.info(c, "getUniqueGroupIdsForUser", "Checking with a valid user.");
        try {
            servlet.getGroupsForUser(user);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4001E")) {
                assertTrue("An invalid user should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user should cause EntryNotFoundException", false);
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
            servlet.getGroupsForUser(user);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4001E")) {
                assertTrue("An invalid user should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid user should cause EntryNotFoundException", false);
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
            servlet.getGroupsForUser(user);
        } catch (EntryNotFoundException e) {
            assertTrue("An invalid user should cause RegistryException", false);
        } catch (RegistryException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML0515E")) {
                assertTrue("An invalid user should cause RegistryException", true);
            } else {
                assertTrue("An invalid user should cause RegistryException error CWIML0515E", false);
            }
        }
    }

}