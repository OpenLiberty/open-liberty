/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.Attribute;
import com.ibm.websphere.simplicity.config.wim.AttributeConfiguration;
import com.ibm.websphere.simplicity.config.wim.AttributesCache;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapFilters;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.SearchResultsCache;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;
import com.ibm.ws.security.wim.test.VmmServiceServletConnection;
import com.unboundid.ldap.sdk.Entry;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/**
 * Contains a collection of tests designed to prevent regression of fixed defects.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LDAPRegressionTest {
    private static final Class<?> c = LDAPRegressionTest.class;

    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.ldap.fat.regression");
    private static UserRegistryServletConnection urServlet;
    private static VmmServiceServletConnection vmmServlet;
    private static ServerConfiguration basicConfiguration = null;
    private static InMemoryLDAPServer ds;
    private static final String BASE_DN = "o=ibm,c=us";
    private static final String USER_1 = "user1";
    private static final String USER_1_DN = "uid=" + USER_1 + "," + BASE_DN;
    private static final String USER_2 = "Bob (Contractor)";
    private static final String USER_2_DN = "uid=" + USER_2 + "," + BASE_DN;

    /**
     * Setup the test case.
     *
     * @throws Exception If the setup failed for some reason.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        setupLibertyServer();
        setupLdapServer();
    }

    /**
     * Tear down the test.
     */
    @AfterClass
    public static void teardownClass() throws Exception {
        try {
            if (libertyServer != null) {
                libertyServer.stopServer("CWIML4523E");
            }
        } finally {
            if (ds != null) {
                try {
                    ds.shutDown(true);
                } catch (Exception e) {
                    Log.error(c, "teardown", e, "LDAP server threw error while shutting down. " + e.getMessage());
                }
            }

            libertyServer.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
            libertyServer.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/vmmapi-1.0.mf");
        }
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
        libertyServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/vmmapi-1.0.mf");
        libertyServer.addInstalledAppForValidation("vmmService");
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

        Log.info(c, "setUp", "Creating UserRegistry servlet connection the server");
        urServlet = new UserRegistryServletConnection(libertyServer.getHostname(), libertyServer.getHttpDefaultPort());

        if (urServlet.getRealm() == null) {
            Thread.sleep(5000);
            urServlet.getRealm();
        }

        Log.info(c, "setUp", "Creating VMM servlet connection the server");
        vmmServlet = new VmmServiceServletConnection(libertyServer.getHostname(), libertyServer.getHttpDefaultPort());

        /*
         * The original server configuration has no registry or Federated Repository configuration.
         */
        basicConfiguration = libertyServer.getServerConfiguration();
    }

    /**
     * Configure the embedded LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLdapServer() throws Exception {
        ds = new InMemoryLDAPServer(BASE_DN);

        /*
         * Add the partition entries.
         */
        Entry entry = new Entry(BASE_DN);
        entry.addAttribute("objectclass", "organization");
        entry.addAttribute("o", "ibm");
        ds.add(entry);

        /*
         * Create user1.
         */
        entry = new Entry(USER_1_DN);
        entry.addAttribute("objectclass", "wiminetorgperson");
        entry.addAttribute("uid", USER_1);
        entry.addAttribute("sn", USER_1);
        entry.addAttribute("cn", USER_1);
        entry.addAttribute("nickName", USER_1 + " nick name");
        entry.addAttribute("userPassword", "password");
        ds.add(entry);

        /*
         * Create user2.
         */
        entry = new Entry(USER_2_DN);
        entry.addAttribute("objectclass", "wiminetorgperson");
        entry.addAttribute("uid", USER_2);
        entry.addAttribute("sn", USER_2);
        entry.addAttribute("cn", USER_2);
        entry.addAttribute("nickName", USER_2 + " nick name");
        entry.addAttribute("userPassword", "password");
        ds.add(entry);
    }

    /**
     * Create a basic LDAP registry configuration element for the specified server configuration.
     * The registry will be added to the configuration.
     *
     * @param serverConfig The server configuration to add the LDAP registry to.
     * @return The new LDAP registry configuration element.
     */
    private static LdapRegistry createLdapRegistry(ServerConfiguration serverConfig) {
        /*
         * Create and add the new LDAP registry to the server configuration.
         */
        LdapRegistry ldapRegistry = new LdapRegistry();
        serverConfig.getLdapRegistries().add(ldapRegistry);

        /*
         * Configure the LDAP registry.
         */
        ldapRegistry.setBaseDN(BASE_DN);
        ldapRegistry.setLdapType("Custom");
        ldapRegistry.setRealm("LdapRealm");
        ldapRegistry.setHost("localhost");
        ldapRegistry.setPort(String.valueOf(ds.getLdapPort()));
        ldapRegistry.setBindDN(ds.getBindDN());
        ldapRegistry.setBindPassword(ds.getBindPassword());
        ldapRegistry.setCustomFilters(new LdapFilters("(&(uid=%v)(objectclass=wiminetorgperson))", "(&(cn=%v)(objectclass=groupofnames))", null, null, null));

        return ldapRegistry;
    }

    /**
     * Check that the AttributesCache is timing out at the right now and not getting reset
     * when the SearchResultsCache is updated.
     *
     * Added for Open Liberty issue 5064 where the attributesCache is reset when new entries are put into the
     * SearchCache, basically making the attributesCache act as a last access time instead of a
     * creation time cache.
     *
     * The sleeps in this test are required -- we're timing out the Search and Attributes caches.
     *
     * This test also requires security trace to be enabled in the LdapConnection and Cache classes.
     *
     * @throws Exception If there was an unexpected exception.
     */
    @Test
    public void testAttributeCacheTimeout() throws Exception {
        Log.info(c, "testAttributeCacheTimeout", "Entering test testAttributeCacheTimeout");

        ServerConfiguration clone = basicConfiguration.clone();
        LdapRegistry ldap = createLdapRegistry(clone);
        ldap.setLdapCache(new LdapCache(new AttributesCache(true, 4444, 2222, "5s"), new SearchResultsCache(true, 5555, 3333, "2s")));
        updateConfigDynamically(libertyServer, clone);

        assertEquals("LdapRealm", urServlet.getRealm());

        SearchResult result = urServlet.getUsers(USER_1, 5);
        assertEquals("There should only be 1 entry", 1, result.getList().size());

        Thread.sleep(3000); // sleep long enough to timeout searchCache, but not attributesCache;

        // reset log marker
        libertyServer.setMarkToEndOfLog(libertyServer.getMostRecentTraceFile());
        String trTrue = "size: 1 newEntry: true";
        List<String> trMsgs = libertyServer.findStringsInLogsAndTraceUsingMark(trTrue);
        assertTrue("Should not have found, " + trTrue, trMsgs.isEmpty());

        // access user again
        result = urServlet.getUsers(USER_1, 5);
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
        result = urServlet.getUsers(USER_1, 5);
        assertEquals("There should only be 1 entry", 1, result.getList().size());

        trMsgs = libertyServer.findStringsInLogsAndTraceUsingMark(trTrue);
        assertFalse("Should have found, " + trTrue, trMsgs.isEmpty());
    }

    /**
     * Test the fix for Open Liberty issue 5376.
     *
     * The error manifests itself when the LDAP registry contains a user filter with an attribute value
     * assertion (uid=%v) whose attribute name (uid) matches a PersonAccount property name (uid) that
     * has been mapped to an LDAP attribute (nickName) whose value does NOT match the original LDAP attribute (uid).
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void testGetAttributesByUniqueName() throws Exception {
        Log.info(c, "testGetAttributesByUniqueName", "Entering test testGetAttributesByUniqueName");

        ServerConfiguration clone = basicConfiguration.clone();

        /*
         * Create an LDAP registry.
         */
        LdapRegistry ldapRegistry = createLdapRegistry(clone);

        /*
         * Configure the PersonAccount property 'uid' to the LDAP attribute 'nickName'. The
         * 'nickName' value does NOT match the 'uid' value.
         */
        AttributeConfiguration attributeConfiguration = new AttributeConfiguration();
        attributeConfiguration.getAttributes().add(new Attribute("nickName", "uid", "PersonAccount", null, null));
        ldapRegistry.setAttributeConfiguration(attributeConfiguration);

        /*
         * Apply the changes.
         */
        updateConfigDynamically(libertyServer, clone);

        /*
         * Get the user security name. This call will fail with an EntityNotFoundException from
         * LdapConnection.getAttributesByUniqueName() if the error is encountered.
         */
        assertEquals(USER_1_DN, urServlet.getUserSecurityName(USER_1));
    }

    /**
     * Test the fix for Open Liberty issue 10462.
     *
     * A InvalidSearchFilterException would occur when a principal expression passed
     * into the search method contained a paren.
     *
     * @throws Exception if the test fails for some reason.
     */
    @Test
    public void testPrincipalSearchExpressionWithParen() throws Exception {
        String methodName = "testPrincipalSearchExpressionWithParen";
        Log.info(c, methodName, "Entering test " + methodName);

        ServerConfiguration clone = basicConfiguration.clone();

        /*
         * Create an LDAP registry.
         */
        createLdapRegistry(clone);

        /*
         * Apply the changes.
         */
        updateConfigDynamically(libertyServer, clone);

        /*
         * Test with left paren search pattern.
         */
        String expression = URLEncoder.encode("@xsi:type='PersonAccount' and principalName='*(*'", StandardCharsets.UTF_8.toString());
        String results = vmmServlet.makeServletMethodCallWithException("searchWithExpression", "?method=searchWithExpression&expression=" + expression);
        assertNotNull(results);
        List<String> listResults = VmmServiceServletConnection.convertToList("searchWithExpression", results);
        assertEquals(1, listResults.size());
        assertEquals("uid=Bob (Contractor),o=ibm,c=us", listResults.get(0));

        /*
         * Test with right paren search pattern.
         */
        expression = URLEncoder.encode("@xsi:type='PersonAccount' and principalName='*)*'", StandardCharsets.UTF_8.toString());
        results = vmmServlet.makeServletMethodCallWithException("searchWithExpression", "?method=searchWithExpression&expression=" + expression);
        assertNotNull(results);
        listResults = VmmServiceServletConnection.convertToList("searchWithExpression", results);
        assertEquals(1, listResults.size());
        assertEquals("uid=Bob (Contractor),o=ibm,c=us", listResults.get(0));

        /*
         * Test with right paren search pattern.
         */
        expression = URLEncoder.encode("@xsi:type='PersonAccount' and principalName='*(*)*'", StandardCharsets.UTF_8.toString());
        results = vmmServlet.makeServletMethodCallWithException("searchWithExpression", "?method=searchWithExpression&expression=" + expression);
        assertNotNull(results);
        listResults = VmmServiceServletConnection.convertToList("searchWithExpression", results);
        assertEquals(1, listResults.size());
        assertEquals("uid=Bob (Contractor),o=ibm,c=us", listResults.get(0));
    }

    /**
     * Verify that we issue an error and throw an exception when a user filter without a %v is found.
     */
    @Test
    public void testUserFilterWithoutPercentV() throws Exception {
        ServerConfiguration clone = basicConfiguration.clone();
        LdapRegistry ldap = createLdapRegistry(clone);
        ldap.getCustomFilters().setUserFilter("(uid=someuser)");

        updateConfigDynamically(libertyServer, clone);

        assertFalse("Did not find CWIML4523E in log", libertyServer.waitForStringInLogUsingMark("CWIML4523E.*uid=someuser.*userFilter") == null);
    }

    /**
     * Verify that we issue an error and throw an exception when a group filter without a %v is found.
     */
    @Test
    public void testGroupFilterWithoutPercentV() throws Exception {
        ServerConfiguration clone = basicConfiguration.clone();
        LdapRegistry ldap = createLdapRegistry(clone);
        ldap.getCustomFilters().setGroupFilter("(cn=somegroup)");

        updateConfigDynamically(libertyServer, clone);

        assertFalse("Did not find CWIML4523E in log", libertyServer.waitForStringInLogUsingMark("CWIML4523E.*cn=somegroup.*groupFilter") == null);
    }
}
