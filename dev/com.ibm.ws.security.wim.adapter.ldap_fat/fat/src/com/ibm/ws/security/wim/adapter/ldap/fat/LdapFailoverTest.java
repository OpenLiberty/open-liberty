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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.ContextPool;
import com.ibm.websphere.simplicity.config.wim.FailoverServers;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.SearchResultsCache;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;
import com.unboundid.ldap.sdk.Entry;

import componenttest.annotation.ExpectedFFDC;
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
public class LdapFailoverTest {

    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.return.to.primary");
    private static final Class<?> c = LdapFailoverTest.class;
    private static UserRegistryServletConnection servlet;

    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
    private static ServerConfiguration emptyConfiguration = null;

    private static InMemoryLDAPServer ds1;
    private static InMemoryLDAPServer ds2;
    private static final String BASE_DN = "o=ibm.com";
    private static final String USER = "user";
    private static final String USER_DN = "uid=" + USER + "," + BASE_DN;
    private static final String PWD = "usrpwd";

    @BeforeClass
    public static void setupClass() throws Exception {
        setupLibertyServer();
        setupLdapServers();
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
                if (ds1 != null) {
                    ds1.shutDown(true);
                }
                if (ds2 != null) {
                    ds2.shutDown(true);
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
    private static void setupLdapServers() throws Exception {
        ds1 = new InMemoryLDAPServer(BASE_DN);
        ds2 = new InMemoryLDAPServer(BASE_DN);

        for (InMemoryLDAPServer server : new InMemoryLDAPServer[] { ds1, ds2 }) {

            Entry entry = new Entry(BASE_DN);
            entry.addAttribute("objectclass", "organization");
            entry.addAttribute("o", "ibm.com");
            server.add(entry);

            entry = new Entry(USER_DN);
            entry.addAttribute("objectclass", "inetorgperson");
            entry.addAttribute("uid", USER);
            entry.addAttribute("sn", USER);
            entry.addAttribute("cn", USER);
            entry.addAttribute("userPassword", PWD);
            server.add(entry);
        }
    }

    private static void updateLibertyServer(Boolean returnToPrimaryServer, Integer PrimaryServerQueryTimeInterval) throws Exception {
        ServerConfiguration server = emptyConfiguration.clone();

        LdapRegistry ldap = new LdapRegistry();
        ldap.setId("ldap1");
        ldap.setRealm("LDAPRealm");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds1.getLdapPort()));
        ldap.setIgnoreCase(true);
        ldap.setBaseDN(BASE_DN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setContextPool(new ContextPool(true, 1, 0, 3, "0s", "3000s"));
        FailoverServers failover = new FailoverServers("failoverLdapServers", new String[][] { { "localhost", String.valueOf(ds2.getLdapPort()) } });

        ldap.setFailoverServer(failover);
        ldap.setReturnToPrimaryServer(returnToPrimaryServer);
        SearchResultsCache src = new SearchResultsCache();
        src.setEnabled(false); // disable search cache so we can look up the same user over and over again
        ldap.setLdapCache(new LdapCache(null, src));
        ldap.setPrimaryServerQueryTimeInterval(PrimaryServerQueryTimeInterval);
        server.getLdapRegistries().add(0, ldap);
        updateConfigDynamically(libertyServer, server);

    }

    /**
     * Check if the returnPrimaryServer and primaryServerQueryInterval are working as designed. This test will shut
     * down the primary server, wait for the time interval specified in the PrimaryServerQueryInterval, then turn the
     * primary server back on. Before the PrimaryServerQueryInterval has elapsed, the test will make sure that JNDI calls
     * are being made to the failover server
     *
     * @throws Exception If there was an unexpected exception.
     */
    @Test
    @ExpectedFFDC(value = "javax.naming.CommunicationException")
    public void testReturnToPrimaryServer() throws Exception {
        updateLibertyServer(true, 1);
        String primaryServerJNDICall = "JNDI_CALL search\\(Name,String,SearchControls\\) \\[ldap://localhost:" + String.valueOf(ds1.getLdapPort()) + "\\]";
        String failoverServerJNDICall = "JNDI_CALL search\\(Name,String,SearchControls\\) \\[ldap://localhost:" + String.valueOf(ds2.getLdapPort()) + "\\]";

        String returnUser = servlet.checkPassword(USER_DN, PWD);
        List<String> trMsgs = libertyServer.findStringsInLogsAndTraceUsingMark(primaryServerJNDICall);
        assertFalse("Should find: " + primaryServerJNDICall, trMsgs.isEmpty());
        assertNotNull("Should find user on checkPassword using " + USER_DN, returnUser);

        /* shutting the primary server down */
        ds1.shutDown();
        returnUser = servlet.checkPassword(USER_DN, PWD);
        trMsgs = libertyServer.findStringsInLogsAndTraceUsingMark(failoverServerJNDICall);
        assertFalse("Should find: " + failoverServerJNDICall, trMsgs.isEmpty());
        assertNotNull("Should find user on checkPassword using " + USER_DN, returnUser);

        /* restarting the primary server */
        ds1.getLdapServer().startListening();
        libertyServer.setMarkToEndOfLog(libertyServer.getMostRecentTraceFile());
        Thread.sleep(10000L);

        /* Call before return to primary query interval expires. We should stay on the fail-over server. */
        returnUser = servlet.checkPassword(USER_DN, PWD);
        trMsgs = libertyServer.findStringsInLogsAndTraceUsingMark(failoverServerJNDICall);
        assertFalse("Should find: " + failoverServerJNDICall, trMsgs.isEmpty());
        assertNotNull("Should find user on checkPassword using " + USER_DN, returnUser);
        libertyServer.setMarkToEndOfLog(libertyServer.getMostRecentTraceFile());

        /* Wait for primary query interval to expire then call again. We should return to the primary. */
        Thread.sleep(60000L);
        returnUser = servlet.checkPassword(USER_DN, PWD);
        trMsgs = libertyServer.findStringsInLogsAndTraceUsingMark(primaryServerJNDICall);
        assertFalse("Should find: " + primaryServerJNDICall, trMsgs.isEmpty());
        assertNotNull("Should find user on checkPassword using " + USER_DN, returnUser);

        updateLibertyServer(false, 1);

        /* shutting the primary server down */
        ds1.shutDown();

        /* Wait until enough time has passed that we can create a new context pool */
        Thread.sleep(3000L);

        returnUser = servlet.checkPassword(USER_DN, PWD);
        trMsgs = libertyServer.findStringsInLogsAndTraceUsingMark(failoverServerJNDICall);
        assertNotNull("Should find user on checkPassword using " + USER_DN, returnUser);

        /* restarting the primary server */
        ds1.getLdapServer().startListening();
        Thread.sleep(61000L);
        libertyServer.setMarkToEndOfLog(libertyServer.getMostRecentTraceFile());
        returnUser = servlet.checkPassword(USER_DN, PWD);
        assertNotNull("Should find user on checkPassword using " + USER_DN, returnUser);
        trMsgs = libertyServer.findStringsInLogsAndTraceUsingMark(failoverServerJNDICall);
        assertFalse("Should find: " + failoverServerJNDICall, trMsgs.isEmpty());
    }

}