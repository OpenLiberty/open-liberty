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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
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
public class URAPIs_Federation_2LDAPs_2RealmsTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.tworealms");
    private static final Class<?> c = URAPIs_Federation_2LDAPs_2RealmsTest.class;
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
            server.stopServer();
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
        assertEquals("PrimaryRealm", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithUserUnderPrimaryRealm() throws Exception {
        String user = "vmmtestuser";
        String password = "vmmtestuserpwd";
        Log.info(c, "checkPasswordWithUserUnderPrimaryRealm", "Checking good credentials");
        assertDNsEqual("Authentication should succeed.", "cn=vmmtestuser,o=ibm,c=us", servlet.checkPassword(user, password));
        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithUserUnderSecondaryRealm() throws Exception {
        String user = "vmmtestuser@SecondaryRealm"; // @ is delimiter specified in config for secondary realm
        String password = "vmmtestuserpwd";
        Log.info(c, "checkPasswordWithUserUnderSecondaryRealm", "Checking good credentials");
        assertEquals("Authentication should succeed.",
                     "vmmtestuser@SecondaryRealm", servlet.checkPassword(user, password));
        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Hit the test servlet to see if isValidUser works with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidUserForUserUnderPrimaryRealm() throws Exception {
        String user = "vmmtestuser";
        Log.info(c, "isValidUserForUserUnderPrimaryRealm", "Checking with a valid user");
        assertTrue("User validation should succeed.",
                   servlet.isValidUser(user));
    }

    /**
     * Hit the test servlet to see if isValidUser works with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidUserForUserUnderSecondaryRealm() throws Exception {
        String user = "vmmtestuser@SecondaryRealm";
        Log.info(c, "isValidUserForUserUnderSecondaryRealm", "Checking with a valid user");
        assertTrue("User validation should succeed.",
                   servlet.isValidUser(user));
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersOnPrimaryRealm() throws Exception {
        String user = "vmmtestuser";
        Log.info(c, "getUsers", "Checking with a valid pattern and limit of 4.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("The number of entries did not match.", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersOnSecondaryRealm() throws Exception {
        String user = "vmmtestuser@SecondaryRealm";
        Log.info(c, "getUsers", "Checking with a valid pattern and limit of 4.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("The number of entries did not match.", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 5; should only expect to find 3 entries
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithAsteriskWildcardOnPrimaryRealm() throws Exception {
        String user = "vmmtes*";
        Log.info(c, "getUsersWithAsteriskWildcardOnPrimaryRealm", "Checking with a valid pattern and limit of 5.");
        SearchResult result = servlet.getUsers(user, 5);
        assertEquals("The number of entries did not match.", 5, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUsersWithAsteriskWildcardOnSecondaryRealm() throws Exception {
        String user = "vmmtest*@SecondaryRealm";
        Log.info(c, "getUsersWithAsteriskWildcardOnSecondaryRealm", "Checking with a valid pattern on secondary and limit of 2.");
        SearchResult result = servlet.getUsers(user, 2);
        assertEquals("The number of entries did not match.", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserDisplayNameWithUserInPrimaryRealm() throws Exception {
        String user = "vmmtestuser";
        String displayName = "vmmtestuser";
        Log.info(c, "getUserDisplayNameWithUserInPrimaryRealm", "Checking with a valid user.");
        assertEquals(displayName, servlet.getUserDisplayName(user));
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserDisplayNameWithUserInSecondaryRealm() throws Exception {
        String user = "vmmtestuser@SecondaryRealm";
        String displayName = "vmmtestuser";
        Log.info(c, "getUserDisplayNameWithUserInSecondaryRealm", "Checking with a valid user.");
        assertEquals(displayName, servlet.getUserDisplayName(user));
    }

    /**
     * Hit the test servlet to see if getUniqueUserId works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueUserIdWithUserInPrimaryRealm() throws Exception {
        String user = "vmmtestuser";
        String uniqueUserId = "cn=vmmtestuser,o=ibm,c=us";
        Log.info(c, "getUniqueUserIdWithUserInPrimaryRealm", "Checking with a valid user.");
        assertDNsEqual("DNs should be equal ", uniqueUserId, servlet.getUniqueUserId(user));
    }

    /**
     * Hit the test servlet to see if getUniqueUserId works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueUserIdWithUserInSecondaryRealm() throws Exception {
        String user = "vmmtestuser@SecondaryRealm";
        String uniqueUserId = "vmmtestuser@SecondaryRealm";
        Log.info(c, "getUniqueUserIdWithUserInSecondaryRealm", "Checking with a valid user.");
        assertEquals(uniqueUserId, servlet.getUniqueUserId(user));
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityNameWithUserPrimaryRealm() throws Exception {
        String user = "cn=vmmtest,o=ibm,c=us";
        String securityName = "cn=vmmtest,o=ibm,c=us";

        Log.info(c, "getUserSecurityNameWithUserPrimaryRealm", "Checking with a valid user.");
        assertDNsEqual("DNs shouls be equal ", securityName, servlet.getUserSecurityName(user));
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityNameWithUserSecondaryRealm() throws Exception {
        String user = "cn=vmmtestuser,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com@SecondaryRealm";
        String securityName = "vmmtestuser"; // vmmtestuser@SecondaryRealm

        Log.info(c, "getUserSecurityNameWithUserSecondaryRealm", "Checking with a valid user.");
        assertEquals(securityName, servlet.getUserSecurityName(user));
    }

    /**
     * Hit the test servlet to see if isValidGroup works with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroupWithGroupUnderPrimaryRealm() throws Exception {
        String group = "vmmgrp1";
        Log.info(c, "isValidGroupWithGroupUnderPrimaryRealm", "Checking with a valid group");
        assertTrue("Group validation should succeed.",
                   servlet.isValidGroup(group));
    }

    /**
     * Hit the test servlet to see if isValidGroup works with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroupWithGroupUnderSecondaryRealm() throws Exception {
        String group = "Group Policy Creator Owners@SecondaryRealm";
        Log.info(c, "isValidGroupWithGroupUnderSecondaryRealm", "Checking with a valid group");
        assertTrue("Group validation should succeed.",
                   servlet.isValidGroup(group));
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsOnPrimaryRealm() throws Exception {
        String group = "vmmgroup1";
        Log.info(c, "getGroupsOnPrimaryRealm", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("The number of entries did not match.", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should only expect to find one entry
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsOnSecondaryRealm() throws Exception {
        String group = "vmmgroup1@SecondaryRealm";
        Log.info(c, "getGroupsOnSecondaryRealm", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 2);
        assertEquals("The number of entries did not match.", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 4; should only expect to find more entries but as limit 4, we should only get 4 groups
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithAsteriskWildcardOnPrimaryRealm() throws Exception {
        String group = "vmmgr*";
        Log.info(c, "getGroupsWithAsteriskWildcardOnPrimaryRealm", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 4);
        assertEquals("The number of entries did not match.", 4, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 4; should only expect to find more entries but as limit 4, we should only get 4 groups
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithAsteriskWildcardOnSecondaryRealm() throws Exception {
        String group = "vmmgroup*@SecondaryRealm";
        Log.info(c, "getGroupsWithAsteriskWildcardOnSecondaryRealm", "Checking with a valid pattern and limit of 2.");
        SearchResult result = servlet.getGroups(group, 4);
        assertEquals("The number of entries did not match.", 4, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameWithGroupPrimaryRealm() throws Exception {
        String group = "vmmgrp1";
        Log.info(c, "getGroupDisplayNameWithGroupPrimaryRealm", "Checking with a valid group.");
        assertEquals("vmmgrp1", servlet.getGroupDisplayName(group));
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameWithGroupSecondaryRealm() throws Exception {
        String group = "Group Policy Creator Owners@SecondaryRealm";
        Log.info(c, "getGroupDisplayNameWithGroupSecondaryRealm", "Checking with a valid group.");
        assertEquals("Group Policy Creator Owners", servlet.getGroupDisplayName(group));
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdWithGroupInPrimaryRealm() throws Exception {
        String group = "vmmgrp1";
        String uniqueGroupId = "cn=vmmgrp1,o=ibm,c=us";
        Log.info(c, "getUniqueGroupIdWithGroupInPrimaryRealm", "Checking with a valid group.");
        assertDNsEqual(null, uniqueGroupId, servlet.getUniqueGroupId(group));
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdWithGroupInSecondaryRealm() throws Exception {
        String group = "Group Policy Creator Owners@SecondaryRealm";
        String uniqueGroupId = "Group Policy Creator Owners";
        Log.info(c, "getUniqueGroupIdWithGroupInSecondaryRealm", "Checking with a valid group.");
        assertEquals("Both CNs should be equal ", uniqueGroupId, servlet.getUniqueGroupId(group));
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithTDSGroup() throws Exception {
        String uniqueGroupId = "cn=vmmgrp1,o=ibm,c=us";
        Log.info(c, "getGroupSecurityNameWithTDSGroup", "Checking with a valid group.");
        assertEquals("vmmgrp1", servlet.getGroupSecurityName(uniqueGroupId));
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityNameWithADGroup() throws Exception {
        String uniqueGroupId = "CN=Group Policy Creator Owners,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com@SecondaryRealm";
        Log.info(c, "getGroupSecurityNameWithADGroup", "Checking with a valid group.");
        assertEquals("Group Policy Creator Owners", servlet.getGroupSecurityName(uniqueGroupId));
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUserWithUserPrimaryRealm() throws Exception {
        String user = "user1g1";
        Log.info(c, "getGroupsForUserWithUserPrimaryRealm", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertTrue(list.contains("grp1"));
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUserWithUserSecondaryRealm() throws Exception {
        String user = "WIMUser1@SecondaryRealm";
        Log.info(c, "getGroupsForUserWithUserSecondaryRealm", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertEquals("The number of entries did not match.", 1, list.size());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsWithUserInPrimaryRealm() throws Exception {
        String user = "cn=vmmuser1,o=ibm,c=us";
        Log.info(c, "getUniqueGroupIdsWithUserInPrimaryRealm", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertEquals("The number of entries did not match.", 2, list.size());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsWithUserInSecondaryRealm() throws Exception {
        String user = "cn=WIMUser1,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com@SecondaryRealm";
        Log.info(c, "getUniqueGroupIdsWithUserInSecondaryRealm", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertEquals("The number of entries did not match.", 1, list.size());
    }
}