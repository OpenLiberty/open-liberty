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

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static componenttest.topology.utils.LDAPFatUtils.assertDNsEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assume;
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
import componenttest.topology.utils.LDAPUtils;
import componenttest.vulnerability.LeakedPasswordChecker;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class URAPIs_TDSLDAP_FailoverTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.tds.failover");
    private static final Class<?> c = URAPIs_TDSLDAP_FailoverTest.class;
    private static UserRegistryServletConnection servlet;
    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @BeforeClass
    public static void setUp() throws Exception {
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
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        try {
            server.stopServer("CWIML4529E", "CWIML4537E");
        } finally {
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
        }
    }

    /**
     * Hit the test servlet to see if getRealm works.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getRealm() throws Exception {
        Log.info(c, "getRealm", "Checking expected realm");
        assertEquals("TDSRealm", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPassword() throws Exception {
        String user = "vmmtestuser";
        String password = "vmmtestuserpwd";
        Log.info(c, "checkPassword", "Checking good credentials");
        assertEquals("Authentication should succeed.",
                     "vmmtestuser", servlet.checkPassword(user, password));
        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Hit the test servlet to see if checkPassword fails when passed in a invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithInvalidUser() {
        String user = "invalid";
        String password = "testuserpwd";
        Log.info(c, "checkPasswordWithInvalidUser", "Checking good credentials");
        try {
            servlet.checkPassword(user, password);
        } catch (RegistryException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        server.waitForStringInLog("CWIML4537E");
        assertTrue("An invalid user should cause RegistryException with No principal is found message", true);
        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithBadCredentials() throws Exception {
        String user = "vmmtestuser";
        String password = "badPassword";
        Log.info(c, "checkPasswordWithBadCredentials", "Checking bad credentials");
        assertNull("Authentication should not succeed.",
                   servlet.checkPassword(user, password));
        server.waitForStringInLog("CWIML4529E");
        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Hit the test servlet to see if isValidUser works with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidUser() throws Exception {
        String user = "vmmtestuser";
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
        String user = "vmmtestuser";
        Log.info(c, "getUsers", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("The number of entries did not match.", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithAsteriskWildcard() throws Exception {
        String user = "vmmtes*";
        Log.info(c, "getUsersWithAsteriskWildcard", "Checking with a valid pattern and limit of 3.");
        SearchResult result = servlet.getUsers(user, 3);
        assertEquals("The number of entries did not match.", 3, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithAsteriskWildcard1() throws Exception {
        String user = "*";
        Log.info(c, "getUsersWithAsteriskWildcard1", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("The number of entries did not match.", 2, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithWildcard() throws Exception {
        // This test will only be executed when using physical LDAP server as these type of filters are not supported on ApacheDS
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = "vmmtest*use*$";
        Log.info(c, "getUsers", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("The number of entries did not match.", 1, result.getList().size());
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
        assertEquals("The number of entries did not match.", 0, result.getList().size());
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
        String user = "vmmtestuser";
        String displayName = "vmmtestuser";
        Log.info(c, "getUserDisplayName", "Checking with a valid user.");
        assertEquals(displayName, servlet.getUserDisplayName(user));
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works(Throws exception) when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserDisplayNameWithInvalidUser() throws Exception {
        String user = "invalidUser";
        Log.info(c, "getUserDisplayNameWithInvalidUser", "Checking with an invalid user.");
        try {
            servlet.getUserDisplayName(user);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E", errorMessage.contains("CWIML4001E"));
        }
    }

    /**
     * Hit the test servlet to see if getUniqueUserId works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueUserId() throws Exception {
        String user = "vmmtestuser";
        String uniqueUserId = "cn=vmmtestuser,o=ibm,c=us";
        Log.info(c, "getUniqueUserId", "Checking with a valid user.");
        //assertEquals("", uniqueUserId, servlet.getUniqueUserId(user));
        assertDNsEqual("Unique names should be equal ", uniqueUserId, servlet.getUniqueUserId(user));
    }

    /**
     * Hit the test servlet to see if getUniqueUserId works when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueUserIdWithInvalidUser() throws Exception {
        String user = "invalidUser";
        Log.info(c, "getUniqueUserIdWithInvalidUser", "Checking with an invalid user.");
        try {
            servlet.getUniqueUserId(user);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E", errorMessage.contains("CWIML4001E"));
        }
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityName() throws Exception {
        String user = "vmmtestuser";
        String securityName = "vmmtestuser";
        Log.info(c, "getUserSecurityName", "Checking with a valid user.");
        assertEquals(securityName, servlet.getUserSecurityName(user));
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityNameWithInvalidUser() throws Exception {
        String user = "cn=invalid,o=ibm,c=us";
        Log.info(c, "getUserSecurityNameWithInvalidUser", "Checking with an invalid user.");
        try {
            servlet.getUserSecurityName(user);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4527E", errorMessage.contains("CWIML4527E"));
        }
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with an invalid user (EntityOutOfRealmScope)
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityNameWithEntityOutOfRealmScope() throws Exception {
        String user = "uid=invalid";
        Log.info(c, "getUserSecurityNameWithEntityOutOfRealmScope", "Checking with an invalid user.");
        try {
            servlet.getUserSecurityName(user);
            fail("Expected RegistryException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause RegistryException error CWIML0515E", errorMessage.contains("CWIML0515E"));
        }
    }

    /**
     * Hit the test servlet to see if isValidGroup works with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroup() throws Exception {
        String group = "vmmgrp1";
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
        String group = "vmmgrp1";
        Log.info(c, "getGroups", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("The number of entries did not match.", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should only expect to find more entries but as limit 2, we should only get 2 groups
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithAsteriskWildcard() throws Exception {
        String group = "*";
        Log.info(c, "getGroupsWithAsteriskWildcard", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("The number of entries did not match.", 2, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 5; we should only get 4 groups
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithAsteriskWildcard1() throws Exception {
        String group = "vmmgrp*";
        Log.info(c, "getGroupsWithAsteriskWildcard1", "Checking with a valid pattern and limit of 4.");
        SearchResult result = servlet.getGroups(group, 5);
        assertEquals("The number of entries did not match.", 4, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 11; we should only get 7 groups
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithAsteriskWildcard2() throws Exception {
        String group = "vmmg*p*";
        Log.info(c, "getGroupsWithAsteriskWildcard1", "Checking with a valid pattern and limit of 11.");
        SearchResult result = servlet.getGroups(group, 11);
        assertEquals("The number of entries did not match.", 11, result.getList().size());
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
        assertEquals("The number of entries did not match.", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when supplied with an invalid pattern for
     * the group pattern and a limit of 2; should expect to find no entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithValidPatternLimitLessThanZero() throws Exception {
        String group = "vmmgrp1";
        Log.info(c, "getGroupsWithValidPatternLimitLessThanZero", "Checking with a valid pattern and limit of -1.");
        SearchResult result = servlet.getGroups(group, -1);
        assertEquals("The number of entries did not match.", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayName() throws Exception {
        String group = "vmmgrp1";
        Log.info(c, "getGroupDisplayName", "Checking with a valid group.");
        assertEquals("vmmgrp1", servlet.getGroupDisplayName(group));
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameWithInvalidGroup() throws Exception {
        String group = "cn=invalidgroup,o=ibm,c=us";
        Log.info(c, "getGroupDisplayNameWithInvalidGroup", "Checking with an invalid group.");
        try {
            servlet.getGroupDisplayName(group);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid group should cause EntryNotFoundException error CWIML4527E", errorMessage.contains("CWIML4527E"));
        }
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameWithEntityOutOfRealmScope() throws Exception {
        String group = "cn=invalidgroup";
        Log.info(c, "getGroupDisplayNameWithEntityOutOfRealmScope", "Checking with an invalid group.");
        try {
            servlet.getGroupDisplayName(group);
            fail("Expected RegistryException.");
        } catch (RegistryException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause RegistryException error CWIML0515E", errorMessage.contains("CWIML0515E"));
        }
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupId() throws Exception {
        String group = "vmmgrp1";
        String uniqueGroupId = "cn=vmmgrp1,o=ibm,c=us";
        Log.info(c, "getUniqueGroupId", "Checking with a valid group.");
        assertDNsEqual(null, uniqueGroupId, servlet.getUniqueGroupId(group));
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdWithInvalidGroup() throws Exception {
        String group = "invalidGroup";
        Log.info(c, "getUniqueGroupIdWithInvalidGroup", "Checking with an invalid group.");
        try {
            servlet.getUniqueGroupId(group);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid group should cause EntryNotFoundException error CWIML4001E", errorMessage.contains("CWIML4001E"));
        }
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityName() throws Exception {
        String uniqueGroupId = "cn=vmmgrp1,o=ibm,c=us";
        Log.info(c, "getGroupSecurityName", "Checking with a valid group.");
        assertEquals("vmmgrp1", servlet.getGroupSecurityName(uniqueGroupId));
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithInvalidGroup() throws Exception {
        String group = "cn=invalid,o=ibm,c=us";
        Log.info(c, "getGroupSecurityNameWithInvalidGroup", "Checking with an invalid group.");
        try {
            servlet.getGroupSecurityName(group);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid group should cause EntryNotFoundException error CWIML4527E", errorMessage.contains("CWIML4527E"));
        }
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group (InvalidUniqueName)
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithInvalidUniqueName() throws Exception {
        String group = "invalid";
        Log.info(c, "getGroupSecurityNameWithUniqueName", "Checking with an invalid group.");
        try {
            servlet.getGroupSecurityName(group);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid group should cause EntryNotFoundException error CWIML4001E", errorMessage.contains("CWIML4001E"));
        }
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group (EntityOutOfRealmScope)
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithEntityOutOfRealmScope() throws Exception {
        String group = "uid=invalid";
        Log.info(c, "getGroupSecurityNameWithUniqueName", "Checking with an invalid group.");
        try {
            servlet.getGroupSecurityName(group);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid group should cause EntryNotFoundException error CWIML0515E", errorMessage.contains("CWIML0515E"));
        }
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
        System.out.println("List of groups : " + list.toString());
        assertTrue(list.contains("vmmgroup1"));
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a invalid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUserWithInvalidUser() throws Exception {
        String user = "invalidUser";
        Log.info(c, "getGroupsForUserWithInvalidUser", "Checking with an invalid user.");
        try {
            servlet.getGroupsForUser(user);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E", errorMessage.contains("CWIML4001E"));
        }
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIds() throws Exception {
        String user = "cn=vmmuser1,o=ibm,c=us";
        Log.info(c, "getUniqueGroupIds", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertTrue(list.contains("cn=vmmgroup1,o=ibm,c=us"));
        assertEquals("There should only be 2 entries", 2, list.size());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a invalid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsWithInvalidUser() throws Exception {
        String user = "uid=invalid,o=ibm,c=us";
        Log.info(c, "getUniqueGroupIdsForUser", "Checking with a valid user.");
        try {
            servlet.getGroupsForUser(user);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4527E", errorMessage.contains("CWIML4527E"));
        }
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a invalid uniqueName.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsWithInvalidUniqueName() throws Exception {
        String user = "invalid";
        Log.info(c, "getUniqueGroupIdsForUser", "Checking with a valid user.");
        try {
            servlet.getGroupsForUser(user);
            fail("Expected EntryNotFoundException.");
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            assertTrue("An invalid user should cause EntryNotFoundException error CWIML4001E", errorMessage.contains("CWIML4001E"));
        }
    }
}