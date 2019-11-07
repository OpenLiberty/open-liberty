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
@Ignore("Disabled becasue there is no FVT RACF(SDBM) LDAP server.")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class RacfSdbmLdapTest {

    private static final Class<?> c = RacfSdbmLdapTest.class;
    private static UserRegistryServletConnection servlet;

    @Server("com.ibm.ws.security.wim.adapter.ldap.fat.racf.sdbm")
    public static LibertyServer libertyServer;

    private static final String GROUP_DISPLAY_NAME = "pkigrp";
    private static final String UNIQUE_GROUP_ID = "racfid=pkigrp,profiletype=group,cn=racf";
    private static final String GROUP_SECURITY_NAME = "racfid=pkigrp,profiletype=group,cn=racf";

    private static final String USER_LOGIN_NAME = "mozart";
    private static final String USER_DISPLAY_NAME = "mozart";
    private static final String UNIQUE_USER_ID = "racfid=mozart,profiletype=user,cn=racf";
    private static final String USER_SECURITY_NAME = "mozart";
    private static final String USER_PASSWORD = "newpass";

    private static final String GROUP1_DN = "RACFID=PKIGRP,PROFILETYPE=GROUP,CN=RACF";
    private static final String GROUP2_DN = "RACFID=SVTGRP,PROFILETYPE=GROUP,CN=RACF";
    private static final String GROUP3_DN = "RACFID=WIZARDS,PROFILETYPE=GROUP,CN=RACF";

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
    public void checkPassword() throws Exception {
        assertEqualsIgnoreCase("The user security name returned from checkPassword(...) does not match expected.", USER_SECURITY_NAME,
                               servlet.checkPassword(USER_LOGIN_NAME, USER_PASSWORD));
    }

    @Test
    public void getUserSecurityName() throws Exception {
        assertEqualsIgnoreCase("The user security name returned from getUserSecurityName(...) does not match expected.", USER_SECURITY_NAME,
                               servlet.getUserSecurityName(UNIQUE_USER_ID));
    }

    @Test
    public void getUniqueUserId() throws Exception {
        assertEqualsIgnoreCase("The unique user ID returned from getUniqueUserId(...) does not match expected.", UNIQUE_USER_ID,
                               servlet.getUniqueUserId(USER_SECURITY_NAME));
    }

    @Test
    public void getUserDisplayName() throws Exception {
        assertEqualsIgnoreCase("The user display name returned from getUserDisplayName(...) does not match expected.", USER_DISPLAY_NAME,
                               servlet.getUserDisplayName(USER_SECURITY_NAME));
    }

    @Test
    public void getGroupSecurityName() throws Exception {
        assertEqualsIgnoreCase("The group security name returned from getGroupSecurityName(...) does not match expected.", GROUP_SECURITY_NAME,
                               servlet.getGroupSecurityName(UNIQUE_GROUP_ID));
    }

    @Test
    public void getUniqueGroupId() throws Exception {
        assertEqualsIgnoreCase("The unique group ID returned from getUniqueGroupId(...) does not match expected.", UNIQUE_GROUP_ID,
                               servlet.getUniqueUserId(GROUP_SECURITY_NAME));
    }

    @Test
    public void getGroupDisplayName() throws Exception {
        assertEqualsIgnoreCase("The group display name returned from getGroupDisplayName(...) does not match expected.", GROUP_DISPLAY_NAME,
                               servlet.getGroupDisplayName(GROUP_SECURITY_NAME));
    }

    @Test
    public void getGroups() throws Exception {
        SearchResult result = servlet.getGroups(GROUP_DISPLAY_NAME, 0);
        List<String> groups = result.getList();
        assertEquals(1, groups.size());
        assertContainsIgnoreCase(GROUP_SECURITY_NAME, groups);
    }

    @Test
    public void getUsers() throws Exception {
        SearchResult result = servlet.getUsers(USER_DISPLAY_NAME, 0);
        List<String> users = result.getList();
        assertEquals(1, users.size());
        assertContainsIgnoreCase(USER_SECURITY_NAME, users);
    }

    @Test
    public void getGroupsForUser() throws Exception {
        List<String> groups = servlet.getGroupsForUser(USER_SECURITY_NAME);
        assertEquals(3, groups.size());
        assertContainsIgnoreCase(GROUP1_DN, groups);
        assertContainsIgnoreCase(GROUP2_DN, groups);
        assertContainsIgnoreCase(GROUP3_DN, groups);
    }

    @Test
    public void getUniqueGroupIdsForUser() throws Exception {
        List<String> groupIds = servlet.getUniqueGroupIdsForUser(UNIQUE_USER_ID);
        assertEquals(3, groupIds.size());
        assertContainsIgnoreCase(GROUP1_DN, groupIds);
        assertContainsIgnoreCase(GROUP2_DN, groupIds);
        assertContainsIgnoreCase(GROUP3_DN, groupIds);
    }

    @Test
    public void getUsersForGroup() throws Exception {
        SearchResult result = servlet.getUsersForGroup(GROUP_SECURITY_NAME, 0);
        List<String> users = result.getList();
        assertEquals(15, users.size());
        assertContainsIgnoreCase("BLEIER", users);
        assertContainsIgnoreCase("MOZART", users);
        assertContainsIgnoreCase("TULLY", users);
        assertContainsIgnoreCase("PKISERV", users);
        assertContainsIgnoreCase("PKISRVD", users);
        assertContainsIgnoreCase("EBLUM", users);
        assertContainsIgnoreCase("CHRISG", users);
        assertContainsIgnoreCase("DEBBIEA", users);
        assertContainsIgnoreCase("MEGA", users);
        assertContainsIgnoreCase("WFCHOI", users);
        assertContainsIgnoreCase("TGREEN", users);
        assertContainsIgnoreCase("WEGLINS", users);
        assertContainsIgnoreCase("RIVADE", users);
        assertContainsIgnoreCase("FANHUAW", users);
        assertContainsIgnoreCase("WCHOI", users);
    }
}
