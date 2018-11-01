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
public class URAPIs_ADLDAPTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.ad");
    private static final Class<?> c = URAPIs_ADLDAPTest.class;
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
            server.stopServer("CWIML4529E", "CWIML4537E", "CWIML1018E");
        } finally {
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
        }
    }

    /**
     * Hit the test servlet to see if getUsersForGroup works when supplied with a valid group.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForGroupWithValidGroup() throws Exception {
        String group = "vmmgroup1";
        Log.info(c, "getUsersForGroupWithValidGroup", "Checking with a valid group.");
        SearchResult result = servlet.getUsersForGroup(group, 0);
        List<String> list = result.getList();
        assertTrue(list.contains("vmmuser1"));
    }

    /**
     * Hit the test servlet to see if getUsersForGroup works when supplied with a valid group.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForGroupWithValidGroupWithLimit() throws Exception {
        String group = "vmmgroup1";
        Log.info(c, "getUsersForGroupWithValidGroupWithLimit", "Checking with a valid group.");
        SearchResult result = servlet.getUsersForGroup(group, 2);
        List<String> list = result.getList();
        assertEquals("There should be two entries", 2, list.size());
    }

    /**
     * Hit the test servlet to see if getUsersForGroup works when supplied with a valid group.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForGroupWithLimitWithNestedGroup() throws Exception {
        String group = "vmmNestedGroup";
        Log.info(c, "getUsersForGroupWithLimitWithNestedGroup", "Checking with a valid group.");
        SearchResult result = servlet.getUsersForGroup(group, 10);
        List<String> list = result.getList();
        assertTrue(list.contains("vmmuser2"));
        assertTrue(list.contains("vmmuser1"));
        assertTrue(list.contains("vmmtestuser"));
        assertTrue(list.contains("vmmuser4"));
        assertTrue(list.contains("vmmuser3"));
        assertEquals("There should be five entries", 5, list.size());

    }

    /**
     * Hit the test servlet to see if getUsersForGroup works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForGroupWithInvalidGroup() {
        String group = "invalidGroup";
        Log.info(c, "getUsersForGroupWithInvalidGroup", "Checking with an invalid group.");
        try {
            servlet.getUsersForGroup(group, 0);
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
     * Hit the test servlet to see if getRealm works.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getRealm() throws Exception {
        Log.info(c, "getRealm", "Checking expected realm");
        assertEquals("ADRealm", servlet.getRealm());
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
        assertEquals("There should only be one entry", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersByDN() throws Exception {
        String user = "CN=vmmtestuser,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
        Log.info(c, "getUsersByDN", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should only be one entry", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithAsteriskWildcard() throws Exception {
        String user = "vmmtes*";
        Log.info(c, "getUsersWithAsteriskWildcard", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should only be one entry", 1, result.getList().size());
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
        assertEquals("There should only be two entries", 2, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithWildcard() throws Exception {
        String user = "vmmtest*use*";
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
     * Hit the test servlet to see if getUsers give search result exceeds exception works when passed in a valid user pattern
     * and a limit of 850, while maxSearchResults is set to 800; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersForMaxSearchResultException() {
        // This test will only be executed when using physical LDAP server
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = "*";
        Log.info(c, "getUsersForMaxSearchResultException", "Checking with a valid pattern and limit of 850.");
        try {
            servlet.getUsers(user, 850);
        } catch (RegistryException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        server.waitForStringInLog("CWIML1018E");
        assertTrue("Results more than maxSearchResults should throw exception", true);
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
        String user = "vmmtestuser";
        String uniqueUserId = "CN=vmmtestuser,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
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
    public void getUserSecurityNameWithInvalidUser() {
        String user = "cn=invalid,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
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
     * Hit the test servlet to see if isValidGroup works with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroup() throws Exception {
        String group = "TelnetClients";
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
        String group = "TelnetClients";
        Log.info(c, "getGroups", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("There should only be one entry", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsByDN() throws Exception {
        String group = "cn=TelnetClients,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
        Log.info(c, "getGroupsByDN", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("There should only be one entry", 1, result.getList().size());
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
        assertEquals("There should only be one entry", 2, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should only expect to find more entries but as limit 2, we should only get 2 groups
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithAsteriskWildcard1() throws Exception {
        String group = "TelnetClie*";
        Log.info(c, "getGroupsWithAsteriskWildcard1", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("There should only be one entry", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should only expect to find more entries but as limit 2, we should only get 2 groups
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithAsteriskWildcard2() throws Exception {
        String group = "Tel*Clie*";
        Log.info(c, "getGroupsWithAsteriskWildcard1", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
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
     * Hit the test servlet to see if getGroups works when passed in a invalid group pattern
     * and a limit of 2; should only expect to find no entry
     * This verifies the various required bundles got installed and are working.
     */
    /*
     * @Test
     *
     * @ExpectedFFDC(value = { "com.ibm.ws.security.registry.RegistryException", "com.ibm.websphere.wim.exception.SearchControlException" })
     * public void getGroupsWithInvalidLimit() {
     * String group = "TelnetClients";
     * Log.info(c, "getGroupsWithInvalidLimit", "Checking with a valid pattern and limit of -1.");
     * try {
     * servlet.getGroups(group, -1);
     * } catch (RegistryException e) {
     * // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
     * e.printStackTrace();
     * }
     * server.waitForStringInLog("CWIML1022E"); //CWIML1022E The '-1' count limit specified in the SearchControl data object is invalid.
     * assertTrue("An invalid limit should cause RegistryException in SearchControl", true);
     * }
     */

    /**
     * Hit the test servlet to see if getGroups works when supplied with an invalid pattern for
     * the group pattern and a limit of 2; should expect to find no entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithValidPatternLimitLessThanZero() throws Exception {
        String group = "TelnetClients";
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
        //String group = "CN=TelnetClients,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
        String group = "TelnetClients";
        Log.info(c, "getGroupDisplayName", "Checking with a valid group.");
        assertEquals("TelnetClients", servlet.getGroupDisplayName(group));
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameWithInvalidGroup() {
        String group = "CN=invalidgroup,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
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
            assertTrue("An invalid group should cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameWithEntityOutOfRealmScope() {
        String group = "CN=invalidgroup";
        Log.info(c, "getGroupDisplayNameWithEntityOutOfRealmScope", "Checking with an invalid group.");
        try {
            servlet.getGroupDisplayName(group);
        } catch (EntryNotFoundException e) {
            assertTrue("An invalid group should cause RegistryException", false);
        } catch (RegistryException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML0515E")) {
                assertTrue("An invalid group should cause RegistryException", true);
            } else {
                assertTrue("An invalid group should cause RegistryException error CWIML0515E", false);
            }
        }
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupId() throws Exception {
        String group = "TelnetClients";
        String uniqueGroupId = "CN=TelnetClients,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
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
            assertTrue("An invalid group should cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityName() throws Exception {
        String uniqueGroupId = "CN=TelnetClients,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
        Log.info(c, "getGroupSecurityName", "Checking with a valid group.");
        assertEquals("TelnetClients", servlet.getGroupSecurityName(uniqueGroupId));
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithInvalidGroup() {
        String group = "CN=invalid,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
        Log.info(c, "getGroupSecurityNameWithInvalidGroup", "Checking with an invalid group.");
        try {
            servlet.getGroupSecurityName(group);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML4527E")) {
                assertTrue("An invalid group should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid group should cause EntryNotFoundException error CWIML4527E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid group should cause EntryNotFoundException", false);
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
            assertTrue("An invalid group should cause EntryNotFoundException", false);
        }
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with an invalid group (EntityOutOfRealmScope)
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithEntityOutOfRealmScope() {
        String group = "CN=invalid";
        Log.info(c, "getGroupSecurityNameWithUniqueName", "Checking with an invalid group.");
        try {
            servlet.getGroupSecurityName(group);
        } catch (EntryNotFoundException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("CWIML0515E")) {
                assertTrue("An invalid group should cause EntryNotFoundException", true);
            } else {
                assertTrue("An invalid group should cause EntryNotFoundException error CWIML0515E", false);
            }
        } catch (RegistryException e) {
            assertTrue("An invalid group should cause EntryNotFoundException", false);
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
        assertTrue(list.contains("vmmgroup1"));
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUserWithNoMembershipForUser() throws Exception {
        String user = "vmmuser5";
        Log.info(c, "getGroupsForUserWithNoMembershipForUser", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertEquals("There should not be any entry", 0, list.size());
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
        String user = "cn=vmmuser1,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
        Log.info(c, "getUniqueGroupIds", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertEquals("There should only be 3 entries", 3, list.size());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a invalid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsWithInvalidUser() {
        String user = "uid=invalid,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
        Log.info(c, "getUniqueGroupIdsForUser", "Checking with a valid user.");
        try {
            servlet.getUniqueGroupIdsForUser(user);
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
        String user = "cn=invalid";
        Log.info(c, "getUniqueGroupIdsForUser", "Checking with a valid user.");
        try {
            servlet.getUniqueGroupIdsForUser(user);
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

}