/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.GroupProperties;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.MemberAttribute;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;
import com.unboundid.ldap.sdk.Entry;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPFatUtils;
import componenttest.topology.utils.LDAPUtils;

/**
 * Some regression tests for the com.ibm.ws.security.wim.core component.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class WimCoreRegressionTest {
    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.core.fat.WimCoreRegression");
    private static final Class<?> c = WimCoreRegressionTest.class;
    private static UserRegistryServletConnection servlet;
    private static InMemoryLDAPServer ds = null;
    private static ServerConfiguration startConfiguration = null;

    private static final String BASE_DN = "o=ibm.com";
    private static final String GROUP_CN = "group1";
    private static final String GROUP_DN = "cn=" + GROUP_CN + "," + BASE_DN;

    private static final String USER_UID = "user1";
    private static final String USER_DN = "uid=" + USER_UID + "," + BASE_DN;
    private static final String USER_PASSWORD = "password";

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
                libertyServer.stopServer("CWWKE1102W");
                //added "CWWKE1102W" to ignore server quiesce timeouts due to slow test machines
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
    }

    /**
     * Setup the Liberty server. This server will start with very basic configuration. The tests
     * will configure the server dynamically.
     *
     * @throws Exception If there was an issue setting up the Liberty server.
     */
    private static void setupLibertyServer() throws Exception {

        /*
         * Transform any applications into EE9 when necessary.
         */
        FATSuite.transformApps(libertyServer, "dropins/userRegistry.war");

        /*
         * Add LDAP variables to bootstrap properties file
         */
        LDAPUtils.addLDAPVariables(libertyServer);
        Log.info(c, "setUpLibertyServer", "Starting the server... (will wait for userRegistry servlet to start)");
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
         * The original server configuration.
         */
        startConfiguration = libertyServer.getServerConfiguration();
    }

    /**
     * Configure the in-memory LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLdapServer() throws Exception {
        ds = new InMemoryLDAPServer(BASE_DN);
        Log.info(c, "setUpldapServer", "populate LDAP Server");

        /*
         * Add the partition entries.
         */
        Entry entry = new Entry(BASE_DN);
        entry.addAttribute("objectclass", "organization");
        entry.addAttribute("o", "ibm.com");
        ds.add(entry);

        /*
         * Create user.
         */
        entry = new Entry(USER_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", USER_UID);
        entry.addAttribute("sn", USER_UID + "_sn");
        entry.addAttribute("cn", USER_UID + "_cn");
        entry.addAttribute("userPassword", USER_PASSWORD);
        ds.add(entry);

        /*
         * Create group.
         */
        entry = new Entry(GROUP_DN);
        entry.addAttribute("objectclass", "wimGroupOfNames");
        entry.addAttribute("cn", GROUP_CN);
        entry.addAttribute("member", USER_DN);

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
        ldapRegistry.setBindDN(InMemoryLDAPServer.getBindDN());
        ldapRegistry.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldapRegistry.setTimestampFormat("yyyyMMddHHmmss.SSSSZ"); // 20180730202338.850-0000Z
        GroupProperties groupProperties = new GroupProperties();
        groupProperties.getMemberAttributes().add(new MemberAttribute(null, "member", "groupOfNames", "direct"));
        groupProperties.getMemberAttributes().add(new MemberAttribute(null, "ibm-groupMember", "wimGroupOfNames", "direct"));
        ldapRegistry.setGroupProperties(groupProperties);

        return ldapRegistry;
    }

    /**
     * Regression test for Open Liberty GitHub issue 8034.
     *
     * This particular case occurred due to the way that cached attributes are stored in the LDAP registry. The
     * LDAP registry stores any attributes that are not found in the LDAP cache with a null value. Since the
     * group being retrieved didn't have a value for 'ibm-groupMember', it was stored in the cache with a
     * null value. On the next lookup, the search returned that null value when doing membership lookup
     * and the UniqueNameHelper.getValidDN(...) call would error with an NPE when trying to parse the null
     * as a DN.
     *
     * @throws Exception if the test failed for some unforeseen reason.
     */
    @Test
    public void multiValuedOutputProperties() throws Exception {

        ServerConfiguration clone = startConfiguration.clone();
        createLdapRegistry(clone);
        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        /*
         * In the error case, the group lookup would succeed on the first call b/c the JNDI
         * call to LDAP would not contain the attribute with the null value.
         */
        SearchResult result = servlet.getUsersForGroup(GROUP_DN, 0);
        assertEquals("Expected user " + USER_DN + " to be a member of group " + GROUP_DN, Arrays.asList(new String[] { USER_DN }), result.getList());

        /*
         * The second call would fail with the NPE since the group membership lookup would
         * get a response from the LDAP cache that contained the 'ibm-groupMember' attribute
         * with a null value. The code would check to see if the contained value was a DN
         * by calling UniqueNameHelper.getValidDN and that would fail with the NPE.
         */
        result = servlet.getUsersForGroup(GROUP_DN, 0);
        assertEquals("Expected user " + USER_DN + " to be a member of group " + GROUP_DN, Arrays.asList(new String[] { USER_DN }), result.getList());
    }
}