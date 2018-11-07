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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.EntryNotFoundException;
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
@Mode(TestMode.FULL)
public class URAPIs_TDS_EmptyInputsTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.tds.emptyInput");
    private static final Class<?> c = URAPIs_TDS_EmptyInputsTest.class;
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
            server.stopServer("CWIML4541E");
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
        assertEquals("SampleLdapIDSRealm", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithEmptyPrincipalName() throws Exception {
        String user = "";
        String password = "badPassword";
        Log.info(c, "checkPasswordWithEmptyPrincipalName", "Checking with empty principal name");
        assertNull("Authentication should not succeed with empty principal name.",
                   servlet.checkPassword(user, password));
        //server.waitForStringInLog("CWIML4536E:");
        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in an invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithEmptyPassword() throws Exception {
        String user = "testuser";
        String password = "";
        Log.info(c, "checkPasswordWithEmptyPrincipalName", "Checking with empty password");
        assertNull("Authentication should not succeed with empty password.",
                   servlet.checkPassword(user, password));
        server.waitForStringInLog("CWIML4541E:");
    }

    /**
     * Hit the test servlet to see if isValidUser works with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidUser() throws Exception {
        String user = "";
        Log.info(c, "isValidUser", "Checking with a empty input");
        assertFalse("User validation should not succeed.",
                    servlet.isValidUser(user));
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsers() throws Exception {
        String user = "";
        Log.info(c, "getUsers", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("There should only be no entry", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserDisplayName() throws Exception {
        String user = "";

        Log.info(c, "getUserDisplayName", "Checking with a empty input");
        assertNull("Empty input fot getUserDisplayName should cause invalid identifier exception", servlet.getUserDisplayName(user));
        server.waitForStringInLog("CWIML1010E:");
    }

    /**
     * Hit the test servlet to see if getUniqueUserId works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueUserId() throws Exception {
        String user = "";
        Log.info(c, "getUniqueUserId", "Checking with a empty input");
        assertNull("Empty input fot getUniqueUserId should cause invalid identifier exception", servlet.getUniqueUserId(user));
        server.waitForStringInLog("CWIML1010E:");
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserSecurityName() throws Exception {
        String user = "";

        Log.info(c, "getUserSecurityName", "Checking with a empty input.");
        assertNull("Empty input fot getUserSecurityName should cause invalid identifier exception", servlet.getUniqueUserId(user));
        server.waitForStringInLog("CWIML1010E:");
    }

    /**
     * Hit the test servlet to see if isValidGroup works with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroup() throws Exception {
        String group = "";
        Log.info(c, "isValidGroup", "Checking with a empty input.");
        assertFalse("Group validation should not succeed.",
                    servlet.isValidGroup(group));
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should only expect to find more entries but as limit 2, we should only get 2 groups
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroups() throws Exception {
        String group = "";
        Log.info(c, "getGroups", "Checking with a empty pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("There should only be 0 entries", 0, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupDisplayName() throws Exception {
        String group = "";
        Log.info(c, "getGroupDisplayName", "Checking with a empty input.");
        assertNull("Empty input fot getGroupDisplayName should cause invalid identifier exception", servlet.getGroupDisplayName(group));
        server.waitForStringInLog("CWIML1010E:");
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupId() throws Exception {
        String group = "";
        Log.info(c, "getUniqueGroupId", "Checking with a empty input.");
        assertNull("Empty input fot getUniqueGroupId should cause invalid identifier exception", servlet.getUniqueGroupId(group));
        server.waitForStringInLog("CWIML1010E:");
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupSecurityName() throws Exception {
        String uniqueGroupId = "";
        Log.info(c, "getGroupSecurityName", "Checking with a empty input.");
        assertNull("Empty input fot getGroupSecurityName should cause invalid identifier exception", servlet.getGroupSecurityName(uniqueGroupId));
        server.waitForStringInLog("CWIML1010E:");
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupsForUser() throws Exception {
        String user = "";
        Log.info(c, "getGroupsForUser", "Checking with a empty input.");
        assertNull("Empty input fot getGroupsForUser should cause invalid identifier exception", servlet.getGroupsForUser(user));
        server.waitForStringInLog("CWIML1010E:");
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupIds() throws Exception {
        String user = "";
        Log.info(c, "getUniqueGroupIds", "Checking with a empty input.");
        assertNull("Empty input fot getUniqueGroupIds should cause invalid identifier exception", servlet.getUniqueGroupIdsForUser(user));
        server.waitForStringInLog("CWIML1010E:");
    }

}
