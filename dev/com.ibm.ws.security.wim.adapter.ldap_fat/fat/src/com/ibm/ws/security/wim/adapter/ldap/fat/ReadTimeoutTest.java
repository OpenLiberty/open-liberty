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

import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/**
 * Confirm correct defaults are used for the Custom Context pool. PI81923
 * Confirm correct/expected defaults are used for Search and Attribute cache. PI81954
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ReadTimeoutTest {
    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.readtimeout");
    private static final Class<?> c = ReadTimeoutTest.class;
    private static UserRegistryServletConnection servlet;

    private static ServerConfiguration emptyConfiguration = null;

    private static EmbeddedApacheDS ldapServer = null;
    private static final String SUB_DN = "o=ibm,c=us";
    private static final String USER_BASE_DN = "ou=TestUsers,ou=Test,o=ibm,c=us";
    private static final String USER = "user1";
    private static final String USER_DN = "uid=" + USER + "," + USER_BASE_DN;

    /**
     * Setup the test case.
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
        ldapServer.startServer();

        /*
         * Add the partition entries.
         */
        Entry entry = ldapServer.newEntry(USER_BASE_DN);
        entry.add("objectclass", "organizationalunit");
        entry.add("ou", "Test");
        entry.add("ou", "TestUsers");
        ldapServer.add(entry);

        /*
         * Create the user.
         */
        entry = ldapServer.newEntry(USER_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", USER);
        entry.add("sn", USER);
        entry.add("cn", USER);
        entry.add("userPassword", "password");
        ldapServer.add(entry);
    }

    /**
     * Convenience method to configure the Liberty server with an {@link LdapRegistry} configuration that
     * will connect to {@link #ldapServer}.
     *
     * @throws Exception If there was an error configuring the server.
     */
    private static void updateLdapRegistry() throws Exception {
        ServerConfiguration server = emptyConfiguration.clone();

        LdapRegistry ldap = new LdapRegistry();
        ldap.setId("ldap1");
        ldap.setRealm("LDAPRealm");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ldapServer.getLdapServer().getPort()));
        ldap.setBaseDN(SUB_DN);
        ldap.setBindDN(EmbeddedApacheDS.getBindDN());
        ldap.setBindPassword(EmbeddedApacheDS.getBindPassword());
        ldap.setLdapType("Custom");

        server.getLdapRegistries().add(ldap);
        updateConfigDynamically(libertyServer, server);

    }

    /*
     * Checking trace logs to see if the server trace logs contain the default read timeout and can be updated with a new read Timeout
     *
     * Note: Trace logs aren't representative of actual timeout. However, we can not figure out a easy way to force a read timeout
     */

    @Test
    public void testCustomReadTimeout() throws Exception {
        updateLdapRegistry();
        ServerConfiguration config = libertyServer.getServerConfiguration();
        LdapRegistry ldap = config.getLdapRegistries().get(0);

        String findstr = ", iReadTimeout=null,";
        List<String> errMsgs = libertyServer.findStringsInLogsAndTrace(findstr);
        assertFalse("Should have found, " + findstr, errMsgs.isEmpty());

        //getting the LDAP registry service up
        String returnUser = servlet.checkPassword(USER_DN, "password");

        findstr = ", iReadTimeout=60000,";
        errMsgs = libertyServer.findStringsInLogsAndTrace(findstr);
        assertFalse("Should have found, " + findstr, errMsgs.isEmpty());

        ldap.setReadTimeout("3000");
        updateConfigDynamically(libertyServer, config);

        //resetting the LDAP registry service
        returnUser = servlet.checkPassword(USER_DN, "password");

        findstr = ", iReadTimeout=3000,";
        errMsgs = libertyServer.findStringsInLogsAndTrace(findstr);
        assertFalse("Should have found, " + findstr, errMsgs.isEmpty());
    }

}
