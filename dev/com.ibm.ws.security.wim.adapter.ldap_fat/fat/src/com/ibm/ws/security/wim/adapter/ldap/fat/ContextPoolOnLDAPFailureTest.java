/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static com.ibm.ws.security.wim.adapter.ldap.fat.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.FailoverServers;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.Server;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/**
 * Porting PM95697, creating extra context pools after an LDAP error
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ContextPoolOnLDAPFailureTest {

    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.context.pool.on.ldap.fail");
    private static final Class<?> c = ContextPoolOnLDAPFailureTest.class;
    private static UserRegistryServletConnection servlet;

    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
    private static ServerConfiguration emptyConfiguration = null;

    private static EmbeddedApacheDS ldapServer = null;
    private static EmbeddedApacheDS ldapServerFailover = null;
    private static final String SUB_DN = "o=ibm,c=us";
    private static final String USER_BASE_DN = "ou=TestUsers,ou=Test,o=ibm,c=us";
    private static final String USER = "user";
    private static final String USER_DN = "uid=" + USER + "," + USER_BASE_DN;

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
        if (libertyServer != null) {
            try {
                libertyServer.stopServer("CWIML4520E");
            } catch (Exception e) {

            }
        }
        if (ldapServer != null) {
            try {
                ldapServer.stopService();
            } catch (Exception e) {
                // Ignore
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
        ldapServer = new EmbeddedApacheDS("primaryLDAP");
        ldapServer.addPartition("users", USER_BASE_DN);
        ldapServer.startServer();

        ldapServerFailover = new EmbeddedApacheDS("backupLDAP");
        ldapServerFailover.addPartition("users", USER_BASE_DN);
        ldapServerFailover.startServer();

        updateLdapServer(ldapServer);
        updateLdapServer(ldapServerFailover);
    }

    private static void updateLdapServer(EmbeddedApacheDS ldap) throws Exception {
        /*
         * Add the partition entries.
         */
        Entry entry = ldap.newEntry(USER_BASE_DN);
        entry.add("objectclass", "organizationalunit");
        entry.add("ou", "Test");
        entry.add("ou", "TestUsers");
        ldap.add(entry);

        entry = ldap.newEntry(USER_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", USER);
        entry.add("sn", USER);
        entry.add("cn", USER);
        entry.add("userPassword", "password");
        ldap.add(entry);

        for (int i = 0; i < 1000; i++) {
            entry = ldap.newEntry("uid=" + USER + i + "," + USER_BASE_DN);
            entry.add("objectclass", "inetorgperson");
            entry.add("uid", USER + i);
            entry.add("sn", USER + i);
            entry.add("cn", USER + i);
            entry.add("userPassword", "password");
            ldap.add(entry);
        }
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
        ldap.setPort(String.valueOf(ldapServer.getLdapServer().getPort()));
        // ldap.setBaseDN(USER_BASE_DN);
        ldap.setBaseDN(SUB_DN);
        ldap.setBindDN(EmbeddedApacheDS.getBindDN());
        ldap.setBindPassword(EmbeddedApacheDS.getBindPassword());
        ldap.setLdapType("Custom");

        ConfigElementList<Server> servers = new ConfigElementList<Server>();
        servers.add(new Server("localhost", String.valueOf(ldapServerFailover.getLdapServer().getPort())));

        FailoverServers fs = new FailoverServers();
        fs.setServers(servers);
        ldap.setFailoverServer(fs);

        server.getLdapRegistries().add(ldap);
        updateConfigDynamically(libertyServer, server);

    }

    /**
     * Run several clients and stop the primary LDAP server. One of the clients
     * should dump the context pool.
     *
     * FFDC related to communications exceptions are expected.
     *
     * @throws Exception If there was an unexpected exception.
     */
    @Test
    @AllowedFFDC(value = { "javax.naming.CommunicationException", "javax.naming.ServiceUnavailableException" })
    public void contextPoolOnFailure() throws Exception {
        final String method = "contextPoolOnFailure";

        int numThreads = 20;
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < numThreads; i++) {
            Thread th = new Thread(String.valueOf(i)) {
                @Override
                public void run() {
                    try {
                        servlet.getUsers("uid=" + USER + getName() + "," + USER_BASE_DN, 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            };
            threads.add(th);
        }
        for (Thread th : threads) {
            th.start();
        }
        for (Thread th : threads) {
            th.join();
        }

        Thread restartLDAP = new Thread() {
            @Override
            public void run() {
                try {
                    Log.info(c, method, "Request ldap stop");
                    ldapServer.stopServer();
                    Log.info(c, method, "ldap stopped");
                } catch (Exception e) {
                    Log.info(c, method, "ldap exception");
                    e.printStackTrace();
                }
            }

        };

        threads = new ArrayList<Thread>();
        for (int i = 0; i < numThreads; i++) {
            Thread th = new Thread(String.valueOf(i)) {
                @Override
                public void run() {
                    try {
                        Log.info(c, method, "User started: " + getName());
                        for (int j = 0; j < 100; j++) {
                            servlet.checkPassword("uid=" + USER + getName() + "," + USER_BASE_DN, "password");
                            Thread.sleep(50);
                        }
                        Log.info(c, method, "User stopped: " + getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            };
            threads.add(th);
        }

        for (Thread th : threads) {
            th.start();
        }
        restartLDAP.start();
        for (Thread th : threads) {
            th.join();
        }
        restartLDAP.join();

        String tr = "Pool refreshed";
        List<String> errMsgs = libertyServer.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

    }

}