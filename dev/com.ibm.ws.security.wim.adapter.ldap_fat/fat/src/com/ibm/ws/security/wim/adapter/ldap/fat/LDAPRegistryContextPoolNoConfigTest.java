/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.AttributesCache;
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

/**
 * Confirm correct defaults are used for the Custom Context pool. PI81923
 * Confirm correct/expected defaults are used for Search and Attribute cache. PI81954
 *
 * Test updated in 2022 to use the InMemoryLDAPServer instead of standalone LDAP server.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LDAPRegistryContextPoolNoConfigTest {
    /*
     * Re-using this server.xml to have a mostly empty server.xml.
     */
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.loginproperty");
    private static final Class<?> c = LDAPRegistryContextPoolNoConfigTest.class;
    private static UserRegistryServletConnection servlet;

    private static ServerConfiguration emptyConfiguration = null;

    private static InMemoryLDAPServer ds1;
    private static final String BASE_DN = "o=ibm.com";
    private static final String USER = "vmmtestuser";
    private static final String USER_DN = "uid=" + USER + "," + BASE_DN;
    private static final String PWD = "vmmtestuserpwd";

    /**
     * Setup the Liberty server and local LDAP server
     *
     * @throws Exception If there was an issue setting up the Liberty server.
     */
    @BeforeClass
    public static void setup() throws Exception {
        setupLdapServers();

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

        emptyConfiguration = server.getServerConfiguration();

        updateLibertyServer();

        servlet.getRealm();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");

        try {
            if (server != null) {
                server.stopServer();
            }
        } finally {
            try {
                if (ds1 != null) {
                    ds1.shutDown(true);
                }
            } catch (Exception e) {
                Log.error(c, "teardown", e, "LDAP server threw error while shutting down. " + e.getMessage());
            }
        }
        server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
    }

    /**
     * Hit the test servlet to see if getRealm works.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getRealm() throws Exception {
        Log.info(c, "getRealm", "Checking expected realm");
        assertEquals("Realm check failed", "TDSSSLRealm", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPassword() throws Exception {
        Log.info(c, "checkPassword", "Checking good credentials");
        assertEquals("Authentication should succeed.",
                     USER_DN, servlet.checkPassword(USER, PWD));
    }

    /**
     * Test that the defaults for the caches matches the defaults in the metatype.xml, setting caches to
     * enabled=true.
     *
     * @throws Exception
     */
    @Test
    public void testCustomContextNoConfig() throws Exception {
        Log.info(c, "testCustomContextNoConfig", "Entering test testCustomContextNoConfig");

        // Checking that these are correctly logged in the trace
        // We were setting the wrong defaults -- bad maxpool and preferred pool size
        String tr = "InitPoolSize: 1";
        List<String> errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "MaxPoolSize: 0";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "PrefPoolSize: 3";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "PoolTimeOut: 0";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "PoolWaitTime: 3000";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        Log.info(c, "testCustomContextNoConfig", "Check cache config timeouts");

        tr = "CacheTimeOut: 1200000";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());
        assertEquals("Should have found 2 entries -- attributes and search cache, " + tr, 2, errMsgs.size());

        tr = "CacheSize: 2000";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());
        assertEquals("Should have found 2 entries -- attributes and search cache, " + tr, 2, errMsgs.size());

        tr = "CacheSizeLimit: 2000";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "CacheResultSizeLimit: 2000";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());
    }

    /**
     * Update the liberty server with an InMemoryLDAPServer.
     *
     * @throws Exception
     */
    private static void updateLibertyServer() throws Exception {
        ServerConfiguration serverConfig = emptyConfiguration.clone();

        LdapRegistry ldap = new LdapRegistry();
        ldap.setId("ldap1");
        ldap.setRealm("TDSSSLRealm");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds1.getLdapPort()));
        ldap.setIgnoreCase(true);
        ldap.setBaseDN(BASE_DN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");

        /*
         * Original test re-used the server.xml from com.ibm.ws.security.wim.adapter.ldap.fat.tds.sslref which had the
         * search and attributes cache enabled explicitly, but no additional attributes set.
         */
        AttributesCache attr = new AttributesCache();
        attr.setEnabled(true);
        SearchResultsCache src = new SearchResultsCache();
        src.setEnabled(true);
        ldap.setLdapCache(new LdapCache(attr, src));

        serverConfig.getLdapRegistries().add(0, ldap);

        updateConfigDynamically(server, serverConfig);

    }

    /**
     * Configure the embedded LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLdapServers() throws Exception {
        ds1 = new InMemoryLDAPServer(BASE_DN);

        Entry entry = new Entry(BASE_DN);
        entry.addAttribute("objectclass", "organization");
        entry.addAttribute("o", "ibm.com");
        ds1.add(entry);

        entry = new Entry(USER_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", USER);
        entry.addAttribute("sn", USER);
        entry.addAttribute("cn", USER);
        entry.addAttribute("userPassword", PWD);
        ds1.add(entry);

    }

}