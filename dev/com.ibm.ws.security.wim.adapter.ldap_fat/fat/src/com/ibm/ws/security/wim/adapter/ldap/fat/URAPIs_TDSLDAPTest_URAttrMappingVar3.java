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
import org.junit.Ignore;
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

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class URAPIs_TDSLDAPTest_URAttrMappingVar3 {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.tds.attrMappingVar3");
    private static final Class<?> c = URAPIs_TDSLDAPTest_URAttrMappingVar3.class;
    private static UserRegistryServletConnection servlet;

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
        assertEquals("TDSRealm_URAttrMap3", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPassword() throws Exception {
        String user = "vmmLibertyUser@ibm.com";
        String password = "vmmLibertyUser";
        Log.info(c, "checkPassword", "Checking good credentials");
        assertEquals("Authentication should succeed.",
                     "vmmLibertyUserUID", servlet.checkPassword(user, password)); // Expected is vmmLibertyUserUID as output mapping for UserSecurityName is "uid"
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithSecondLoginProperty() throws Exception {
        String user = "vmmLibertyUser"; // Their are 2 login properties configures. so login should be successful with these inputs as well.
        String password = "vmmLibertyUser";
        Log.info(c, "checkPasswordWithSecondLoginProperty", "Checking good credentials");
        assertEquals("Authentication should succeed.",
                     "vmmLibertyUserUID", servlet.checkPassword(user, password));
    }

    /**
     * Hit the test servlet to see if isValidUser works with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidUser() throws Exception {
        String user = "cn=vmmtestuser,o=ibm,c=us";
        Log.info(c, "isValidUser", "Checking with a valid user");
        assertTrue("User validation should succeed.",
                   servlet.isValidUser(user));
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
        SearchResult result = servlet.getUsers(user, 3);
        assertEquals("There should only be one entry", 3, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserDisplayName() throws Exception {
        String user = "cn=vmmtestuser,o=ibm,c=us";
        String displayName = "cn=vmmtestuser,o=ibm,c=us";
        Log.info(c, "getUserDisplayName", "Checking with a valid user.");
        assertDNsEqual("External names should be equal ", displayName, servlet.getUserDisplayName(user));
    }

    /**
     * Hit the test servlet to see if getUniqueUserId works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueUserId() throws Exception {
        String user = "cn=vmmtestuser,o=ibm,c=us";
        String uniqueUserId = "cn=vmmtestuser,o=ibm,c=us";
        Log.info(c, "getUniqueUserId", "Checking with a valid user.");
        assertDNsEqual("External names should be equal ", uniqueUserId, servlet.getUniqueUserId(user));
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Ignore
    @Test
    public void getUserSecurityName() throws Exception {
        // String user = "cn=testuser,o=ibm,c=us";
        String user = "cn=vmmtestuser,o=vmm";
        // String uniqueUserId = "cn=testuser,o=ibm,c=us";
        String uniqueUserId = "vmmtestuser";

        Log.info(c, "getUserSecurityName", "Checking with a valid user.");

        assertEquals("External names should be equal ", uniqueUserId, servlet.getUserSecurityName(user));
    }

    /**
     * Hit the test servlet to see if isValidGroup works with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void isValidGroup() throws Exception {
        String group = "cn=vmmgrp1,o=vmm";
        Log.info(c, "isValidGroup", "Checking with a valid group");
        assertTrue("Group validation should succeed.",
                   servlet.isValidGroup(group));
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should only expect to find more entries but as limit 4, we should only get 3 groups
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsWithAsteriskWildcard1() throws Exception {
        String group = "vmmgrp*";
        Log.info(c, "getGroupsWithAsteriskWildcard1", "Checking with a valid pattern and limit of 4.");
        SearchResult result = servlet.getGroups(group, 5);
        System.out.println("Groups : " + result.getList().toString());
        assertEquals("There should only be 4 entries", 4, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayName() throws Exception {
        String group = "cn=vmmgrp1,o=vmm";
        String userDisplayName = "vmmgrp1";
        Log.info(c, "getGroupDisplayName", "Checking with a valid group.");
        assertEquals("CNs should be equal ", userDisplayName, servlet.getGroupDisplayName(group));
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupId() throws Exception {
        String group = "cn=vmmgrp1,o=vmm";
        String uniqueGroupId = "cn=vmmgrp1,o=vmm";
        Log.info(c, "getUniqueGroupId", "Checking with a valid group.");
        assertDNsEqual("UniqueNames should be equal ", uniqueGroupId, servlet.getUniqueGroupId(group));
    }

    /**
     * Hit the test servlet to see if getGroupSecurityName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupSecurityName() throws Exception {
        String uniqueGroupId = "cn=vmmgrp1,o=ibm,c=us";
        String userSecurityName = "vmmgrp1";
        Log.info(c, "getGroupSecurityName", "Checking with a valid group.");
        assertEquals("CNs should be equal ", userSecurityName, servlet.getGroupSecurityName(uniqueGroupId));
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
        assertEquals("There should only be 3 entries", 3, list.size());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIds() throws Exception {
        String user = "cn=vmmuser1,o=vmm";
        Log.info(c, "getUniqueGroupIds", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertEquals("There should only be 3 entries", 3, list.size());
    }

}