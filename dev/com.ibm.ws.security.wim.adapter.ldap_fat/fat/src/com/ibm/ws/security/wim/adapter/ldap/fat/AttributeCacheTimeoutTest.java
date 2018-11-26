/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static componenttest.topology.utils.LDAPFatUtils.createFederatedRepository;
import static componenttest.topology.utils.LDAPFatUtils.createTDSLdapRegistry;
import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.AttributesCache;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.SearchResultsCache;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/**
 * Check that the AttributesCache is timing out at the right now and not getting reset
 * when the SearchResultsCache is updated.
 *
 * Added for OL Issue 5064 where the attributesCache is reset when new entries are put into the
 * SearchCache, basically making the attributesCache act as a last access time instead of a
 * creation time cache.
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AttributeCacheTimeoutTest {

    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.ldap.fat.attr.timeout");
    private static final Class<?> c = AttributeCacheTimeoutTest.class;
    private static UserRegistryServletConnection servlet;

    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
    private static ServerConfiguration emptyConfiguration = null;

    private static EmbeddedApacheDS ldapServer = null;
    private static final String SUB_DN = "o=ibm,c=us";
    private static final String USER_BASE_DN = "ou=TestUsers,ou=Test,o=ibm,c=us";
    private static final String GROUP_BASE_DN = "ou=DevGroups,ou=Dev,o=ibm,c=us";
    private static final String USER = "user1";
    private static final String USER_DN = "uid=" + USER + "," + USER_BASE_DN;
    private static final String GROUP = "group1";
    private static final String GROUP_DN = "cn=" + GROUP + "," + GROUP_BASE_DN;

    /**
     * Setup the test case.
     *
     * @throws Exception If the setup failed for some reason.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        setupLibertyServer();
        setupldapServer();
    }

    /**
     * Tear down the test.
     */
    @AfterClass
    public static void teardownClass() throws Exception {
        if (libertyServer != null) {
            try {
                libertyServer.stopServer();
            } catch (Exception e) {
                Log.error(c, "teardown", e, "Liberty server threw error while stopping. " + e.getMessage());
            }
        }
        if (ldapServer != null) {
            try {
                ldapServer.stopService();
            } catch (Exception e) {
                Log.error(c, "teardown", e, "LDAP server threw error while stopping. " + e.getMessage());
            }
        }

        libertyServer.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
    }

    /**
     * Setup the Liberty server. This server will start with very basic configuration. The tests
     * will configure the server dynamically.
     *
     * @throws Exception If there was an issue setting up the Liberty server.
     */
    private static void setupLibertyServer() throws Exception {
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

        /*
         * The original server configuration has no registry or Federated Repository configuration.
         */
        emptyConfiguration = libertyServer.getServerConfiguration();
    }

    /**
     * Configure the embedded LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupldapServer() throws Exception {
        ldapServer = new EmbeddedApacheDS("myLDAP");
        ldapServer.addPartition("users", USER_BASE_DN);
        ldapServer.addPartition("groups", GROUP_BASE_DN);
        ldapServer.startServer();

        /*
         * Add the partition entries.
         */
        Entry entry = ldapServer.newEntry(USER_BASE_DN);
        entry.add("objectclass", "organizationalunit");
        entry.add("ou", "Test");
        entry.add("ou", "TestUsers");
        ldapServer.add(entry);

        entry = ldapServer.newEntry(GROUP_BASE_DN);
        entry.add("objectclass", "organizationalunit");
        entry.add("ou", "Dev");
        entry.add("ou", "DevGroups");
        ldapServer.add(entry);

        /*
         * Create the user and group.
         */
        entry = ldapServer.newEntry(USER_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", USER);
        entry.add("sn", USER);
        entry.add("cn", USER);
        entry.add("userPassword", "password");
        ldapServer.add(entry);

        entry = ldapServer.newEntry(GROUP_DN);
        entry.add("objectclass", "groupofnames");
        entry.add("cn", GROUP);
        entry.add("member", USER_DN);
        ldapServer.add(entry);
    }

    /**
     * The sleeps in this test are required -- we're timing out the Search and Attributes caches.
     *
     * This test also requires security trace to be enabled in the LdapConnection and Cache classes.
     *
     * @throws Exception If there was an unexpected exception.
     */
    @Test
    public void testAttributeCacheTimeout() throws Exception {
        Log.info(c, "testAttributeCacheTimeout", "Entering test testAttributeCacheTimeout");

        ServerConfiguration clone = emptyConfiguration.clone();
        LdapRegistry ldap = createTDSLdapRegistry(clone, "LDAP", "SampleLdapIDSRealm");
        ldap.setLdapCache(new LdapCache(new AttributesCache(true, 4444, 2222, "5s"), new SearchResultsCache(true, 5555, 3333, "2s")));
        createFederatedRepository(clone, "OneLDAPRealm", new String[] { ldap.getBaseDN() });
        updateConfigDynamically(libertyServer, clone);

        assertEquals("OneLDAPRealm", servlet.getRealm());

        SearchResult result = servlet.getUsers(USER, 5);
        assertEquals("There should only be 1 entry", 1, result.getList().size());

        Thread.sleep(3000); // sleep long enough to timeout searchCache, but not attributesCache;

        // reset log marker
        libertyServer.setMarkToEndOfLog(libertyServer.getMostRecentTraceFile());
        String trTrue = "size: 1 newEntry: true";
        List<String> trMsgs = libertyServer.findStringsInLogsAndTraceUsingMark(trTrue);
        assertTrue("Should not have found, " + trTrue, trMsgs.isEmpty());

        // access user again
        result = servlet.getUsers(USER, 5);
        assertEquals("There should only be 1 entry", 1, result.getList().size());

        // sleep to allow attributes cache to timeout.
        Thread.sleep(2500);

        trMsgs = libertyServer.findStringsInLogsAndTraceUsingMark(trTrue);
        assertTrue("Should not have found, " + trTrue, trMsgs.isEmpty());

        String trFalse = "size: 1 newEntry: false";
        trMsgs = libertyServer.findStringsInLogsAndTraceUsingMark(trFalse);
        assertFalse("Should have found, " + trFalse, trMsgs.isEmpty());

        String tr = "Evicting tertiaryTable cache AttributesCache";
        trMsgs = libertyServer.findStringsInLogsAndTraceUsingMark(tr);
        assertFalse("Should have found, " + tr, trMsgs.isEmpty());

        libertyServer.setMarkToEndOfLog(libertyServer.getMostRecentTraceFile());

        // access user again, this should be a new cache entry
        result = servlet.getUsers(USER, 5);
        assertEquals("There should only be 1 entry", 1, result.getList().size());

        trMsgs = libertyServer.findStringsInLogsAndTraceUsingMark(trTrue);
        assertFalse("Should have found, " + trTrue, trMsgs.isEmpty());

    }

}