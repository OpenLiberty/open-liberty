/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static componenttest.topology.utils.LDAPFatUtils.assertContainsIgnoreCase;
import static componenttest.topology.utils.LDAPFatUtils.assertEqualsIgnoreCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.LDAPUtils;

/**
 * This FAT will verify that ldapRegistry-3.0 feature can be configured to work when running against
 * a RACF (SDBM) LDAP server, which is not LDAP v3 compliant.
 *
 * <p/>
 * Currently this test is disabled since we don't have a dedicated RACF (SDBM) LDAP server. If
 * that changes, the server.xml for this FAT will need to be updated to point to the correct
 * LDAP server and group and user IDs will need to be updated appropriately.
 */
@Ignore("Disabled because there is no FVT RACF(SDBM) LDAP server.")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class RacfSdbmLdapWithBasicTest {

    private static final Class<?> c = RacfSdbmLdapWithBasicTest.class;
    private static UserRegistryServletConnection servlet;

    @Server("com.ibm.ws.security.wim.adapter.ldap.fat.racf.sdbm.basic")
    public static LibertyServer libertyServer;

    private static final String RACF_GROUP_NAME = "PKIGRP";
    private static final String RACF_GROUP_DISPLAY_NAME = "racfid=pkigrp,profiletype=group,cn=racf";
    private static final String RACF_UNIQUE_GROUP_ID = "racfid=pkigrp,profiletype=group,cn=racf";
    private static final String RACF_GROUP_SECURITY_NAME = "racfid=pkigrp,profiletype=group,cn=racf";

    private static final String RACF_USER_LOGIN_NAME = "mozart";
    private static final String RACF_USER_DISPLAY_NAME = "racfid=mozart,profiletype=user,cn=racf";
    private static final String RACF_UNIQUE_USER_ID = "racfid=mozart,profiletype=user,cn=racf";
    private static final String RACF_USER_SECURITY_NAME = "racfid=mozart,profiletype=user,cn=racf";
    private static final String RACF_USER_PASSWORD = "newpass";

    private static final String RACF_GROUP1_DN = "RACFID=PKIGRP,PROFILETYPE=GROUP,CN=RACF";
    private static final String RACF_GROUP2_DN = "RACFID=SVTGRP,PROFILETYPE=GROUP,CN=RACF";
    private static final String GROUP3_DN = "RACFID=WIZARDS,PROFILETYPE=GROUP,CN=RACF";

    private static final String BASIC_USER = "basicuser";
    private static final String BASIC_USER_PASSWORD = "password";
    private static final String BASIC_GROUP = "basicgroup";

    @BeforeClass
    public static void beforeClass() throws Exception {
        /*
         * Add LDAP variables to bootstrap properties file
         */
        LDAPUtils.addLDAPVariables(libertyServer);
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
    }

    @Test
    public void checkPassword_RACF() throws Exception {
        assertEqualsIgnoreCase("The user security name returned from checkPassword(...) does not match expected.", RACF_USER_SECURITY_NAME,
                               servlet.checkPassword(RACF_USER_LOGIN_NAME, RACF_USER_PASSWORD));
    }

    @Test
    public void getUserSecurityName_RACF() throws Exception {
        assertEqualsIgnoreCase("The user security name returned from getUserSecurityName(...) does not match expected.", RACF_USER_SECURITY_NAME,
                               servlet.getUserSecurityName(RACF_UNIQUE_USER_ID));
    }

    @Test
    public void getUniqueUserId_RACF() throws Exception {
        assertEqualsIgnoreCase("The unique user ID returned from getUniqueUserId(...) does not match expected.", RACF_UNIQUE_USER_ID,
                               servlet.getUniqueUserId(RACF_USER_SECURITY_NAME));
    }

    @Test
    public void getUserDisplayName_RACF() throws Exception {
        assertEqualsIgnoreCase("The user display name returned from getUserDisplayName(...) does not match expected.", RACF_USER_DISPLAY_NAME,
                               servlet.getUserDisplayName(RACF_USER_SECURITY_NAME));
    }

    @Test
    public void getGroupSecurityName_RACF() throws Exception {
        assertEqualsIgnoreCase("The group security name returned from getGroupSecurityName(...) does not match expected.", RACF_GROUP_SECURITY_NAME,
                               servlet.getGroupSecurityName(RACF_UNIQUE_GROUP_ID));
    }

    @Test
    public void getUniqueGroupId_RACF() throws Exception {
        assertEqualsIgnoreCase("The unique group ID returned from getUniqueGroupId(...) does not match expected.", RACF_UNIQUE_GROUP_ID,
                               servlet.getUniqueUserId(RACF_GROUP_SECURITY_NAME));
    }

    @Test
    public void getGroupDisplayName_RACF() throws Exception {
        assertEqualsIgnoreCase("The group display name returned from getGroupDisplayName(...) does not match expected.", RACF_GROUP_DISPLAY_NAME,
                               servlet.getGroupDisplayName(RACF_GROUP_SECURITY_NAME));
    }

    @Test
    public void getGroups_RACF() throws Exception {
        SearchResult result = servlet.getGroups(RACF_GROUP_NAME, 0);
        List<String> groups = result.getList();
        assertEquals(1, groups.size());
        assertContainsIgnoreCase(RACF_GROUP_SECURITY_NAME, groups);
    }

    @Test
    public void getUsers_RACF() throws Exception {
        SearchResult result = servlet.getUsers(RACF_USER_DISPLAY_NAME, 0);
        List<String> users = result.getList();
        assertEquals(1, users.size());
        assertContainsIgnoreCase(RACF_USER_SECURITY_NAME, users);
    }

    @Test
    public void getGroupsForUser_RACF() throws Exception {
        List<String> groups = servlet.getGroupsForUser(RACF_USER_SECURITY_NAME);
        assertEquals(3, groups.size());
        assertContainsIgnoreCase(RACF_GROUP1_DN, groups);
        assertContainsIgnoreCase(RACF_GROUP2_DN, groups);
        assertContainsIgnoreCase(GROUP3_DN, groups);
    }

    @Test
    public void getUniqueGroupIdsForUser_RACF() throws Exception {
        List<String> groupIds = servlet.getUniqueGroupIdsForUser(RACF_UNIQUE_USER_ID);
        assertEquals(3, groupIds.size());
        assertContainsIgnoreCase(RACF_GROUP1_DN, groupIds);
        assertContainsIgnoreCase(RACF_GROUP2_DN, groupIds);
        assertContainsIgnoreCase(GROUP3_DN, groupIds);
    }

    @Test
    public void getUsersForGroup_RACF() throws Exception {
        SearchResult result = servlet.getUsersForGroup(RACF_GROUP_SECURITY_NAME, 0);
        List<String> users = result.getList();
        assertEquals(15, users.size());
        assertContainsIgnoreCase("RACFID=BLEIER,PROFILETYPE=USER,CN=RACF", users);
        assertContainsIgnoreCase("RACFID=MOZART,PROFILETYPE=USER,CN=RACF", users);
        assertContainsIgnoreCase("RACFID=TULLY,PROFILETYPE=USER,CN=RACF", users);
        assertContainsIgnoreCase("RACFID=PKISERV,PROFILETYPE=USER,CN=RACF", users);
        assertContainsIgnoreCase("RACFID=PKISRVD,PROFILETYPE=USER,CN=RACF", users);
        assertContainsIgnoreCase("RACFID=EBLUM,PROFILETYPE=USER,CN=RACF", users);
        assertContainsIgnoreCase("RACFID=CHRISG,PROFILETYPE=USER,CN=RACF", users);
        assertContainsIgnoreCase("RACFID=DEBBIEA,PROFILETYPE=USER,CN=RACF", users);
        assertContainsIgnoreCase("RACFID=MEGA,PROFILETYPE=USER,CN=RACF", users);
        assertContainsIgnoreCase("RACFID=WFCHOI,PROFILETYPE=USER,CN=RACF", users);
        assertContainsIgnoreCase("RACFID=TGREEN,PROFILETYPE=USER,CN=RACF", users);
        assertContainsIgnoreCase("RACFID=WEGLINS,PROFILETYPE=USER,CN=RACF", users);
        assertContainsIgnoreCase("RACFID=RIVADE,PROFILETYPE=USER,CN=RACF", users);
        assertContainsIgnoreCase("RACFID=FANHUAW,PROFILETYPE=USER,CN=RACF", users);
        assertContainsIgnoreCase("RACFID=WCHOI,PROFILETYPE=USER,CN=RACF", users);
    }

    @Test
    @ExpectedFFDC("javax.naming.OperationNotSupportedException") // TODO RACF doesn't like racfid=<NAME> filters where <NAME> is over 8 chars
    public void checkPassword_Basic() throws Exception {
        assertEqualsIgnoreCase("The user security name returned from checkPassword(...) does not match expected.", BASIC_USER,
                               servlet.checkPassword(BASIC_USER, BASIC_USER_PASSWORD));
    }

    @Test
    public void getUserSecurityName_Basic() throws Exception {
        assertEqualsIgnoreCase("The user security name returned from getUserSecurityName(...) does not match expected.", BASIC_USER,
                               servlet.getUserSecurityName(BASIC_USER));
    }

    @Test
    public void getUniqueUserId_Basic() throws Exception {
        assertEqualsIgnoreCase("The unique user ID returned from getUniqueUserId(...) does not match expected.", BASIC_USER,
                               servlet.getUniqueUserId(BASIC_USER));
    }

    @Test
    public void getUserDisplayName_Basic() throws Exception {
        assertEqualsIgnoreCase("The user display name returned from getUserDisplayName(...) does not match expected.", BASIC_USER,
                               servlet.getUserDisplayName(BASIC_USER));
    }

    @Test
    public void getGroupSecurityName_Basic() throws Exception {
        assertEqualsIgnoreCase("The group security name returned from getGroupSecurityName(...) does not match expected.", BASIC_GROUP,
                               servlet.getGroupSecurityName(BASIC_GROUP));
    }

    @Test
    public void getUniqueGroupId_Basic() throws Exception {
        assertEqualsIgnoreCase("The unique group ID returned from getUniqueGroupId(...) does not match expected.", BASIC_GROUP,
                               servlet.getUniqueUserId(BASIC_GROUP));
    }

    @Test
    public void getGroupDisplayName_Basic() throws Exception {
        assertEqualsIgnoreCase("The group display name returned from getGroupDisplayName(...) does not match expected.", BASIC_GROUP,
                               servlet.getGroupDisplayName(BASIC_GROUP));
    }

    @Test
    @ExpectedFFDC({ "com.ibm.wsspi.security.wim.exception.WIMSystemException", "javax.naming.OperationNotSupportedException" }) // TODO RACF doesn't like racfid=<NAME> filters where <NAME> is over 8 chars
    public void getGroups_Basic() throws Exception {
        SearchResult result = servlet.getGroups(BASIC_GROUP, 0);
        List<String> groups = result.getList();
        assertEquals(1, groups.size());
        assertContainsIgnoreCase(BASIC_GROUP, groups);
    }

    @Test
    @ExpectedFFDC({ "com.ibm.wsspi.security.wim.exception.WIMSystemException", "javax.naming.OperationNotSupportedException" }) // TODO RACF doesn't like racfid=<NAME> filters where <NAME> is over 8 chars
    public void getUsers_Basic() throws Exception {
        SearchResult result = servlet.getUsers(BASIC_USER, 0);
        List<String> users = result.getList();
        assertEquals(1, users.size());
        assertContainsIgnoreCase(BASIC_USER, users);
    }

    @Test
    public void getGroupsForUser_Basic() throws Exception {
        List<String> groups = servlet.getGroupsForUser(BASIC_USER);
        assertEquals(1, groups.size());
        assertContainsIgnoreCase(BASIC_GROUP, groups);
    }

    @Test
    public void getUniqueGroupIdsForUser_Basic() throws Exception {
        List<String> groupIds = servlet.getUniqueGroupIdsForUser(BASIC_USER);
        assertEquals(1, groupIds.size());
        assertContainsIgnoreCase(BASIC_GROUP, groupIds);
    }

    @Test
    public void getUsersForGroup_Basic() throws Exception {
        SearchResult result = servlet.getUsersForGroup(BASIC_GROUP, 0);
        List<String> users = result.getList();
        assertEquals(1, users.size());
        assertContainsIgnoreCase(BASIC_USER, users);
    }
}
