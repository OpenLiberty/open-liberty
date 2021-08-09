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

/*
 * TODO The test reuses the same file data for both tests and therefore they use the
 * same users and base entries. These tests should be updated to use file registries
 * with unique users.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class MultipleReposTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.file.fat.multiplerepos");
    private static final Class<?> c = MultipleReposTest.class;
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
            server.stopServer("CWIML4538E");
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
        assertEquals("WIMRegistry", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword fails when passed in a invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPassword() {
        String user = "admin";
        String password = "admin";
        Log.info(c, "checkPassword", "Checking good credentials");
        try {
            servlet.checkPassword(user, password);
        } catch (RegistryException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        server.waitForStringInLog("CWIML4538E");
        assertTrue("Two users with same userid should cause DuplicateLogonIdException", true);
    }

    /**
     * Hit the test servlet to see if isValidUser works with an invalid user (2 users having same logon ID)
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidUser() throws Exception {
        String user = "admin";
        Log.info(c, "isValidUser", "Checking with an invalid user");
        assertFalse("User validation should fail.",
                    servlet.isValidUser(user));
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 1; should only expect to find one entry, but in repo it should found 2 entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsers() throws Exception {
        String user = "admin";
        Log.info(c, "getUsers", "Checking with a valid pattern and limit of 1.");
        SearchResult result = servlet.getUsers(user, 1);
        System.out.println("user : " + result.getList().toString());
        assertEquals("Only be one entry should be returned", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserDisplayName() {
        String user = "admin";
        String displayName = "admin";
        Log.info(c, "getUserDisplayName", "Checking with a valid user.");
        try {
            assertEquals(displayName, servlet.getUserDisplayName(user));
        } catch (EntryNotFoundException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        } catch (RegistryException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        server.waitForStringInLog("CWIML4538E");
        assertTrue("Two users with same userid should cause EntityNotFoundException", true);
    }

    /**
     * Hit the test servlet to see if getUniqueUserId works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueUserId() {
        String user = "admin";
        String uniqueUserId = "uid=admin,o=defaultWIMFileBasedRealm";
        Log.info(c, "getUniqueUserId", "Checking with a valid user.");
        try {
            assertDNsEqual("UniqueUserId is incorrect", uniqueUserId, servlet.getUniqueUserId(user));
        } catch (EntryNotFoundException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        } catch (RegistryException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        server.waitForStringInLog("CWIML4538E");
        assertTrue("Two users with same userid should cause EntityNotFoundException", true);
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityName() throws Exception {
        String user = "uid=admin,o=defaultWIMFileBasedRealm";
        // String uniqueUserId = "uid=admin,o=defaultWIMFileBasedRealm";
        String securityName = "admin";
        Log.info(c, "getUserSecurityName", "Checking with a valid user.");
        assertEquals(securityName, servlet.getUserSecurityName(user));
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityNameForFILE2() throws Exception {
        String user = "uid=admin,o=defaultWIMFileBasedRealm1";
        String securityName = "admin";
        Log.info(c, "getUserSecurityNameForFILE2", "Checking with a valid user.");
        assertEquals(securityName, servlet.getUserSecurityName(user));
    }

    /**
     * Hit the test servlet to see if isValidGroup works with an invalid group (2 groups having logID )
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroup() throws Exception {
        String group = "group1";
        Log.info(c, "isValidGroupWithInvalidGroup", "Checking with an invalid group");
        assertFalse("Group validation should fail.",
                    servlet.isValidGroup(group));
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 1; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroups() throws Exception {
        String group = "group1";
        Log.info(c, "getGroups", "Checking with a valid pattern and limit of 1.");
        SearchResult result = servlet.getGroups(group, 1);
        assertEquals("There should only be one entry", 1, result.getList().size());
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
     * Hit the test servlet to see if getGroupDisplayName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameForFILE2() throws Exception {
        String group = "cn=group1,o=defaultWIMFileBasedRealm1";
        Log.info(c, "getGroupDisplayNameForFILE2", "Checking with a valid group.");
        assertEquals("group1", servlet.getGroupDisplayName(group));
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupId() {
        String group = "group1";
        String uniqueGroupId = "cn=group1,o=defaultWIMFileBasedRealm";
        Log.info(c, "getUniqueGroupId", "Checking with a valid group.");
        try {
            assertDNsEqual("UniqueGroupId is incorrect", uniqueGroupId, servlet.getUniqueGroupId(group));
        } catch (EntryNotFoundException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        } catch (RegistryException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        server.waitForStringInLog("CWIML4538E");
        assertTrue("Two users with same userid should cause EntityNotFoundException", true);
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
     * Hit the test servlet to see if getGroupSecurityName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameFILE2() throws Exception {
        String uniqueGroupId = "cn=group1,o=defaultWIMFileBasedRealm1";
        Log.info(c, "getGroupSecurityNameFILE2", "Checking with a valid group.");
        assertEquals("group1", servlet.getGroupSecurityName(uniqueGroupId));
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUser() {
        String user = "user1";
        Log.info(c, "getGroupsForUser", "Checking with a valid user.");
        try {
            servlet.getGroupsForUser(user);
        } catch (EntryNotFoundException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        } catch (RegistryException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        server.waitForStringInLog("CWIML4538E");
        assertTrue("Two users with same userid should cause EntityNotFoundException", true);
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
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsForFILE2() throws Exception {
        String user = "uid=user1,o=defaultWIMFileBasedRealm1";
        Log.info(c, "getUniqueGroupIdsForFILE2", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertEquals("There should only be one entry", 1, list.size());
    }

}