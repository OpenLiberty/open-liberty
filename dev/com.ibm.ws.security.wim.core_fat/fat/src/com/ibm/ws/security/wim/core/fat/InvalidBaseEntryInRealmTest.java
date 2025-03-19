/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.core.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/**
 * This tests a configuration where the realm's only participating base entry is invalid
 * and doesn't match any known repository. The expectation is that no calls will succeed.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class InvalidBaseEntryInRealmTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.core.fat.invalidBaseEntryInRealm");
    private static final Class<?> c = InvalidBaseEntryInRealmTest.class;
    private static UserRegistryServletConnection servlet;

    /** Test rule for testing for expected exceptions. */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void setUp() throws Exception {

        /*
         * Transform any applications into EE9 when necessary.
         */
        FATSuite.transformApps(server, "dropins/userRegistry.war");

        // Add LDAP variables to bootstrap properties file
        LDAPUtils.addLDAPVariables(server);
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
        // install our user feature

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
            server.stopServer("CWIML0515E", "CWWKE1102W");
            //added "CWWKE1102W" to ignore server quiesce timeouts due to slow test machines
        } finally {
            server.removeInstalledAppForValidation("userRegistry");
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
        }
    }

    /**
     * Hit the test servlet to see if getRealm works.
     */
    @Test
    public void getRealm() throws Exception {
        Log.info(c, "getRealm", "Checking expected realm");
        assertEquals("defaultWIMFileBasedRealm", servlet.getRealm());
    }

    @Test
    public void checkPassword() throws Exception {
        String user = "testuser";
        String password = "testuserpwd";
        Log.info(c, "checkPassword", "No valid participating base entries...");

        assertNull(servlet.checkPassword(user, password));
    }

    @Test
    public void isValidUser() throws Exception {
        String user = "invalidUser";
        Log.info(c, "isValidUser", "No valid participating base entries...");

        expectedException.expect(RegistryException.class);
        expectedException.expectMessage("CWIML0515E");

        servlet.isValidUser(user);
    }

    @Test
    public void getUsers() throws Exception {
        String user = "testuser";
        Log.info(c, "getUsers", "No valid participating base entries...");

        assertNotNull(servlet.getUsers(user, 2));
    }

    @Test
    public void getUserDisplayName() throws Exception {
        String user = "testuser";
        Log.info(c, "getUserDisplayName", "No valid participating base entries...");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML0515E");

        servlet.getUserDisplayName(user);
    }

    @Test
    public void getUniqueUserId() throws Exception {
        String user = "testuser";
        Log.info(c, "getUniqueUserId", "No valid participating base entries...");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML0515E");

        servlet.getUniqueUserId(user);
    }

    @Test
    public void getUserSecurityName() throws Exception {
        String user = "cn=testuser,o=ibm,c-us";
        Log.info(c, "getUserSecurityName", "No valid participating base entries...");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML4001E");

        servlet.getUserSecurityName(user);
    }

    @Test
    public void isValidGroup() throws Exception {
        String group = "group1";
        Log.info(c, "isValidGroup", "No valid participating base entries...");

        expectedException.expect(RegistryException.class);
        expectedException.expectMessage("CWIML0515E");

        servlet.isValidGroup(group);
    }

    @Test
    public void getGroups() throws Exception {
        String group = "group1";
        Log.info(c, "getGroups", "No valid participating base entries...");

        assertNotNull(servlet.getGroups(group, 2));
    }

    @Test
    public void getGroupDisplayName() throws Exception {
        String group = "cn=group1,o=o=ibm,c=us";
        Log.info(c, "getGroupDisplayName", "No valid participating base entries...");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML0515E");

        servlet.getGroupDisplayName(group);
    }

    @Test
    public void getUniqueGroupId() throws Exception {
        String group = "group1";
        Log.info(c, "getUniqueGroupId", "No valid participating base entries...");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML0515E");

        servlet.getUniqueGroupId(group);
    }

    @Test
    public void getGroupSecurityName() throws Exception {
        String uniqueGroupId = "cn=group1,o=ibm,c=us";
        Log.info(c, "getGroupSecurityName", "No valid participating base entries...");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML0515E");

        servlet.getGroupSecurityName(uniqueGroupId);
    }

    @Test
    public void getGroupsForUser() throws Exception {
        String user = "samples";
        Log.info(c, "getGroupsForUser", "No valid participating base entries...");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML0515E");

        servlet.getGroupsForUser(user);
    }

    @Test
    public void getUniqueGroupIds() throws Exception {
        String user = "cn=user1,o=ibm,c=us";
        Log.info(c, "getUniqueGroupIds", "No valid participating base entries...");

        expectedException.expect(EntryNotFoundException.class);
        expectedException.expectMessage("CWIML0515E");

        servlet.getGroupsForUser(user);
    }
}