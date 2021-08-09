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

import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.ContextPool;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.SearchResultsCache;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;
import com.unboundid.ldap.sdk.Entry;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/**
 * Ensure we're timing out the LDAP context pool when expected.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ContextPoolTimeoutTest {

    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.context.pool.timeout");
    private static final Class<?> c = ContextPoolTimeoutTest.class;
    private static UserRegistryServletConnection servlet;

    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
    private static ServerConfiguration emptyConfiguration = null;

    private static InMemoryLDAPServer ds;
    private static final String SUB_DN = "o=ibm,c=us";
    private static final String USER_BASE_DN = "ou=TestUsers,ou=Test,o=ibm,c=us";
    private static final String USER = "user";
    private static final String USER_DN = "uid=" + USER + "," + USER_BASE_DN;
    private static final String PWD = "usrpwd";

    /**
     * Setup the test case.
     *
     * @throws Exception If the setup failed for some reason.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        setupLibertyServer();
        setupldapServer();
        updateLibertyServer();
    }

    /**
     * Tear down the test.
     */
    @AfterClass
    public static void teardownClass() throws Exception {
        try {
            if (libertyServer != null) {
                libertyServer.stopServer();
            }
        } finally {
            try {
                if (ds != null) {
                    ds.shutDown(true);
                }
            } catch (Exception e) {
                Log.error(c, "teardown", e, "LDAP server threw error while shutting down. " + e.getMessage());
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
        ds = new InMemoryLDAPServer(SUB_DN);

        Entry entry = new Entry(SUB_DN);
        entry.addAttribute("objectclass", "top");
        entry.addAttribute("objectclass", "domain");
        ds.add(entry);
        /*
         * Add the partition entries.
         */
        entry = new Entry("ou=Test,o=ibm,c=us");
        entry.addAttribute("objectclass", "organizationalunit");
        entry.addAttribute("ou", "Test");
        ds.add(entry);

        entry = new Entry(USER_BASE_DN);
        entry.addAttribute("objectclass", "organizationalunit");
        entry.addAttribute("ou", "Test");
        entry.addAttribute("ou", "TestUsers");
        ds.add(entry);

        entry = new Entry(USER_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", USER);
        entry.addAttribute("sn", USER);
        entry.addAttribute("cn", USER);
        entry.addAttribute("userPassword", PWD);
        ds.add(entry);

    }

    /**
     * Convenience method to configure the Liberty server with an {@link LdapRegistry} configuration that
     * will connect to {@link #ldapServer}.
     *
     * @throws Exception If there was an error configuring the server.
     */
    private static void updateLibertyServer() throws Exception {
        ServerConfiguration server = emptyConfiguration.clone();

        LdapRegistry ldap = new LdapRegistry();
        ldap.setId("ldap1");
        ldap.setRealm("LDAPRealm");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds.getLdapPort()));
        ldap.setBaseDN(SUB_DN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");
        ContextPool cp = new ContextPool(true, 0, 2, 1, "2s", "5s");
        ldap.setContextPool(cp);
        SearchResultsCache src = new SearchResultsCache();
        src.setEnabled(false); // disable search cache so we can look up the same user over and over again
        ldap.setLdapCache(new LdapCache(null, src));

        server.getLdapRegistries().add(ldap);
        updateConfigDynamically(libertyServer, server);

    }

    /**
     *
     * Check that we hit the timeout block of code for the context pool
     */
    @Test
    public void testContextPoolOnTimeout() throws Exception {

        int numMsg = 0;

        // starter timeout is 2 seconds
        String returnUser = servlet.checkPassword(USER_DN, PWD);
        assertNotNull("Should find user on checkPassword using " + USER_DN, returnUser);
        Thread.sleep(3100); // see the timeout on the ContextPool settings for the LDAPRegistry, this sleep should be longer
        returnUser = servlet.checkPassword(USER_DN, PWD);
        assertNotNull("Should find user on checkPassword using " + USER_DN, returnUser);
        String traceTimeout = "ContextPool: context is time out"; // depends on trace logged in LdapConnection.getDirContext
        List<String> errMsgs = libertyServer.findStringsInLogsAndTrace(traceTimeout);
        assertFalse("Should find: " + traceTimeout, errMsgs.isEmpty());
        assertEquals("Should find: " + traceTimeout, ++numMsg, errMsgs.size());

        // Send in setting less than 1 second. Should run up to 1 second.
        ServerConfiguration config = libertyServer.getServerConfiguration();
        ConfigElementList<LdapRegistry> ldaps = config.getLdapRegistries();
        LdapRegistry ldap = null;
        if (!ldaps.isEmpty() && ldaps.size() == 1) {
            ldap = ldaps.get(0);
        }
        ldap.setContextPool(new ContextPool(true, 0, 2, 1, "500ms", "5s"));
        updateConfigDynamically(libertyServer, config);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        Thread.sleep(2100);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        errMsgs = libertyServer.findStringsInLogsAndTrace(traceTimeout);
        assertFalse("Should find: " + traceTimeout, errMsgs.isEmpty());
        assertEquals("Should find: " + traceTimeout, ++numMsg, errMsgs.size());

        // Send in partial seconds, should round to 2 seconds
        ldap.setContextPool(new ContextPool(true, 0, 2, 1, "1500ms", "5s"));
        updateConfigDynamically(libertyServer, config);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        Thread.sleep(3100);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        errMsgs = libertyServer.findStringsInLogsAndTrace(traceTimeout);
        assertFalse("Should find: " + traceTimeout, errMsgs.isEmpty());
        assertEquals("Should find: " + traceTimeout, ++numMsg, errMsgs.size());

        // no time marker, should be treated as 2 ms
        ldap.setContextPool(new ContextPool(true, 0, 2, 1, "2", "5s"));
        updateConfigDynamically(libertyServer, config);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        Thread.sleep(2100);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        errMsgs = libertyServer.findStringsInLogsAndTrace(traceTimeout);
        assertFalse("Should find: " + traceTimeout, errMsgs.isEmpty());
        assertEquals("Should find: " + traceTimeout, ++numMsg, errMsgs.size());

        // set to 0, do not timeout
        ldap.setContextPool(new ContextPool(true, 0, 2, 1, "0", "5s"));
        updateConfigDynamically(libertyServer, config);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        Thread.sleep(2100);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        errMsgs = libertyServer.findStringsInLogsAndTrace(traceTimeout);
        assertFalse("Should find: " + traceTimeout, errMsgs.isEmpty());
        assertEquals("Should find: " + traceTimeout, numMsg, errMsgs.size()); // same number of messages as before

        // negative number, config processing drops the negative
        ldap.setContextPool(new ContextPool(true, 0, 2, 1, "-2s", "5s"));
        updateConfigDynamically(libertyServer, config);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        Thread.sleep(3100);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        errMsgs = libertyServer.findStringsInLogsAndTrace(traceTimeout);
        assertFalse("Should find: " + traceTimeout, errMsgs.isEmpty());
        assertEquals("Should find: " + traceTimeout, ++numMsg, errMsgs.size());

        // should round down to 2 seconds
        ldap.setContextPool(new ContextPool(true, 0, 2, 1, "2100", "5s"));
        updateConfigDynamically(libertyServer, config);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        Thread.sleep(3100);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        errMsgs = libertyServer.findStringsInLogsAndTrace(traceTimeout);
        assertFalse("Should find: " + traceTimeout, errMsgs.isEmpty());
        assertEquals("Should find: " + traceTimeout, ++numMsg, errMsgs.size());

        // optional for local runs. Commenting out for delivery
        ldap.setContextPool(new ContextPool(true, 0, 2, 1, "1m", "5s"));
        updateConfigDynamically(libertyServer, config);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        Thread.sleep(61100);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        errMsgs = libertyServer.findStringsInLogsAndTrace(traceTimeout);
        assertFalse("Should find: " + traceTimeout, errMsgs.isEmpty());
        assertEquals("Should find: " + traceTimeout, ++numMsg, errMsgs.size());
    }

}