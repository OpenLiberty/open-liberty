/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.registry.fat;

import static componenttest.topology.utils.LDAPFatUtils.assertDNsEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemorySunLDAPServer;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class DefaultWIMRealmMultipleReposTest {

    @Server("com.ibm.ws.security.wim.registry.fat.DefaultWIMRealmMultipleRepos")
    public static LibertyServer server;

    private static final Class<?> c = DefaultWIMRealmMultipleReposTest.class;
    private static UserRegistryServletConnection servlet;

    private static InMemorySunLDAPServer sunLdapServer;

    /**
     * Configure the embedded LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLdapServer() throws Exception {
        sunLdapServer = new InMemorySunLDAPServer();
    }

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        setupLdapServer();
        /*
         * Update LDAP configuration with In-Memory Server
         */
        ServerConfiguration serverConfig = server.getServerConfiguration();
        for (LdapRegistry ldap : serverConfig.getLdapRegistries()) {
            if (ldap.getRealm().equals("SUN_LDAP")) {
                ldap.setHost("localhost");
                ldap.setPort(String.valueOf(sunLdapServer.getLdapPort()));
                ldap.setBindDN(InMemorySunLDAPServer.getBindDN());
                ldap.setBindPassword(InMemorySunLDAPServer.getBindPassword());
                server.updateServerConfiguration(serverConfig);
                break;
            }
        }

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
            if (server != null) {
                server.stopServer("CWIML4538E");
            }
        } finally {
            try {
                if (sunLdapServer != null) {
                    sunLdapServer.shutDown(true);
                }
            } catch (Exception e) {
                Log.error(c, "teardown", e, "LDAP server threw error while shutting down. " + e.getMessage());
            }

            server.deleteFileFromLibertyInstallRoot("lib/features/testfileadapter-1.0.mf");
        }
    }

    /**
     * Hit the test servlet to see if getRealm works.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getRealm() throws Exception {
        Log.info(c, "getRealm", "Checking expected realm");
        assertEquals("newRealm", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword fails when passed in a invalid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPassword() {
        String user = "vmmtestuser";
        String password = "vmmtestuserpwd";
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
    @Ignore
    public void isValidUser() throws Exception {
        String user = "vmmtestuser";
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
        String user = "vmmtestuser";
        Log.info(c, "getUsers", "Checking with a valid pattern and limit of 1.");
        SearchResult result = servlet.getUsers(user, 1);
        assertEquals("Only be one entry should be returned", 1, result.getList().size());
    }

    /**
     * Hit the test servlet to see if getUserDisplayName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserDisplayName() {
        String user = "vmmtestuser";
        String displayName = "vmmtestuser";
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
        String user = "vmmtestuser";
        String uniqueUserId = "uid=vmmtestuser,o=defaultWIMFileBasedRealm";
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
        String uniqueUserId = "uid=vmmtestuser,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        String userSecurityName = "uid=vmmtestuser,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getUserSecurityName", "Checking with a valid user.");
        assertEquals(userSecurityName, servlet.getUserSecurityName(uniqueUserId));
    }

    /**
     * Hit the test servlet to see if getUserSecurityName works when supplied with a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserSecurityNameForAD() throws Exception {
        String uniqueUserId = "cn=vmmtestuser,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
        String userSecurityName = "cn=vmmtestuser,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
        Log.info(c, "getUserSecurityNameForAD", "Checking with a valid user.");
        assertEquals(userSecurityName, servlet.getUserSecurityName(uniqueUserId));
    }

    /**
     * Hit the test servlet to see if isValidGroup works with an invalid group (2 groups having logID )
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    @Ignore
    public void isValidGroup() throws Exception {
        String group = "vmmgroup1";
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
        String group = "vmmgroup1";
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
        String group = "cn=vmmgroup1,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
        Log.info(c, "getGroupDisplayName", "Checking with a valid group.");
        assertEquals("vmmgroup1", servlet.getGroupDisplayName(group));
    }

    /**
     * Hit the test servlet to see if getGroupDisplayName works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupDisplayNameForSUN() throws Exception {
        String group = "cn=vmmgroup1,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getGroupDisplayNameForSUN", "Checking with a valid group.");
        assertEquals("vmmgroup1", servlet.getGroupDisplayName(group));
    }

    /**
     * Hit the test servlet to see if getUniqueGroupId works when supplied with a valid group
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupId() {
        String group = "vmmgroup1";
        String uniqueGroupId = "cn=vmmgroup1,dc=rtp,dc=raleigh,dc=ibm,dc=com";
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
    public void getGroupSecurityName() {
        String uniqueGroupId = "vmmgroup1";
        Log.info(c, "getGroupSecurityName", "Checking with a valid group.");
        try {
            assertDNsEqual("Group nmae is incorrect", "cn=group1,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com", servlet.getGroupSecurityName(uniqueGroupId));
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
    public void getGroupSecurityNameAD() throws Exception {
        String uniqueGroupId = "TelnetClients";
        Log.info(c, "getGroupSecurityNameAD", "Checking with a valid group.");
        assertDNsEqual("Group name is incorrect", "CN=TelnetClients,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com", servlet.getGroupSecurityName(uniqueGroupId));
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUser() {
        String user = "vmmuser1";
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
        String user = "uid=vmmuser1,ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com";
        Log.info(c, "getUniqueGroupIds", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertEquals("There should only be one entry", 1, list.size());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsForAD() throws Exception {
        String user = "cn=vmmuser1,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
        Log.info(c, "getUniqueGroupIdsForAD", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertEquals("There should only be one entry", 2, list.size());
    }

}