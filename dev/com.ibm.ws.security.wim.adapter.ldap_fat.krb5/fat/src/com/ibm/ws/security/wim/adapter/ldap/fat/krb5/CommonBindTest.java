/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap.fat.krb5;

import static componenttest.topology.utils.LDAPFatUtils.assertDNsEqual;
import static componenttest.topology.utils.LDAPFatUtils.assertDNsEqualKrb5;
import static componenttest.topology.utils.LDAPFatUtils.createFederatedRepository;
import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Kerberos;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.FederatedRepository;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils;
import com.unboundid.ldap.sdk.LDAPConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;

/**
 * Base UserLoginTest, the ApacheDS server is started globally by the FatSuite.
 *
 */
@RunWith(FATRunner.class)
public class CommonBindTest {

    private static final Class<?> c = CommonBindTest.class;

    @Rule
    public TestName testName = new TestName();

    protected static UserRegistryServletConnection servlet;
    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);

    protected static ServerConfiguration emptyConfiguration = null;

    protected static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.ldap.fat.krb5.base");

    public static final String BASE_DN = LdapKerberosUtils.BASE_DN;

    public static final String DOMAIN = LdapKerberosUtils.DOMAIN;

    public static String bindPrincipalName = LdapKerberosUtils.BIND_PRINCIPAL_NAME;

    public static String ldapServerHostName = LdapKerberosUtils.HOSTNAME;

    protected static String ticketCacheFile = null;

    protected static String keytabFile = null;

    protected static String configFile = null;

    protected static final String expiredTicketCache = "credCacheExpired.cc";

    protected static final String wrongUserKeytab = "wrongUser.keytab";

    protected static final String vmmUser1 = "vmmUser1";
    protected static final String vmmUser1DN = "uid=vmmUser1," + BASE_DN;
    protected static final String vmmUser1pwd = "password";

    protected static final String vmmGroup1 = "vmmGroup1";
    protected static final String vmmGroup1DN = "cn=" + vmmGroup1 + "," + BASE_DN;

    protected static InMemoryLDAPServer ds1;
    protected static final String UNBOUNDID_BASE_DN = "o=ibm.com";
    protected static final String UNBOUNDID_USER = "unboundUser";
    protected static final String UNBOUNDID_USER_DN = "uid=" + UNBOUNDID_USER + "," + UNBOUNDID_BASE_DN;
    protected static final String UNBOUNDID_PWD = "usrpwd";

    static int LDAP_PORT = ApacheDSandKDC.LDAP_PORT;
    static int KDC_PORT = ApacheDSandKDC.KDC_PORT;

    protected static String[] stopStrings = null;

    @BeforeClass
    public static void setup() throws Exception {
        ticketCacheFile = ApacheDSandKDC.getDefaultTicketCacheFile();
        assertNotNull("TicketCacheFile is null", ticketCacheFile);
        Log.info(c, "setUp", "TicketCache file: " + ticketCacheFile);

        configFile = ApacheDSandKDC.getDefaultConfigFile();
        assertNotNull("ConfigFile is null", configFile);
        Log.info(c, "setUp", "Config file: " + configFile);

        keytabFile = ApacheDSandKDC.getDefaultKeytabFile();
        assertNotNull("Keytab is null", keytabFile);
        Log.info(c, "setUp", "Keytab file: " + keytabFile);

        server.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");

        server.startServer();

        startupChecks();

        Log.info(c, "setUp", "Creating servlet connection the server");
        servlet = new UserRegistryServletConnection(server.getHostname(), server.getHttpDefaultPort());

        if (servlet.getRealm() == null) {
            Thread.sleep(5000);
            assertNotNull("UserRegistryServlet isn't accessible", servlet.getRealm());
        }

        server.copyFileToLibertyServerRoot(expiredTicketCache);
        server.copyFileToLibertyServerRoot(wrongUserKeytab);

        if (emptyConfiguration == null) {
            emptyConfiguration = server.getServerConfiguration();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (server != null) {
                Log.info(c, "tearDown", "stop strings provided: " + Arrays.toString(stopStrings));
                server.stopServer(stopStrings);
            }
        } finally {
            stopUnboundIDLdapServer();
        }
    }

    /**
     * Dynamically reset the server config between each test so we don't
     * accidentally drag along other ldap registries from previous tests.
     *
     * @throws Exception
     */
    @After
    public void resetServerConfig() throws Exception {
        Log.info(c, testName.getMethodName(), "Reset server config.");
        server.setJvmOptions(Arrays.asList("-Dsun.security.krb5.debug=true", "-Dcom.ibm.security.krb5.krb5Debug=true"));
        ServerConfiguration newServer = emptyConfiguration.clone();
        updateConfigDynamically(server, newServer);
    }

    /**
     * A series of "Basics" that all the variations of config should pass, good login, bad login, isvalid, getusers/getgroups.
     *
     * @throws Exception
     */
    public void baselineTests() throws Exception {
        String methodName = "baselineTests";
        Log.info(c, methodName, "Checking good credentials");
        loginUser();

        String password = "badPassword";
        Log.info(c, methodName, "Checking bad credentials");
        assertNull("Authentication should fail.", servlet.checkPassword(vmmUser1, password));
        passwordChecker.checkForPasswordInAnyFormat(password);

        Log.info(c, methodName, "Checking with a valid user");
        assertTrue("User validation should succeed.",
                   servlet.isValidUser(vmmUser1));

        Log.info(c, methodName, "Getting groups for user");
        List<String> list = servlet.getGroupsForUser(vmmUser1);
        assertTrue("Did not find expected group: " + vmmGroup1 + ". Returned list was: " + list, list.contains(vmmGroup1DN));

        Log.info(c, methodName, "Getting users for group");
        SearchResult result = servlet.getUsersForGroup(vmmGroup1, 0);
        list = result.getList();
        assertTrue("Did not find expected user: " + vmmUser1DN + ". Returned list was: " + list, list.contains(vmmUser1DN));
    }

    /**
     * Update the server to have a Kerberos service with the default config file.
     *
     * @param newServer
     * @return
     */
    public static Kerberos addKerberosConfig(ServerConfiguration newServer) {
        Kerberos kerb = newServer.getKerberos();
        kerb.configFile = configFile;
        return kerb;
    }

    /**
     * Update the server to have a Kerberos service with the default keytab and config file.
     *
     * @param newServer
     * @return
     */
    public Kerberos addKerberosConfigAndKeytab(ServerConfiguration newServer) {
        Kerberos kerb = newServer.getKerberos();
        kerb.configFile = configFile;
        kerb.keytab = keytabFile;

        return kerb;
    }

    /**
     * Run a checkpassword on a basic user from the ApacheDS server.
     *
     * @throws Exception
     */
    public void loginUser() throws Exception {
        assertDNsEqualKrb5("Authentication should succeed for " + vmmUser1,
                           vmmUser1DN, servlet.checkPassword(vmmUser1, vmmUser1pwd));
    }

    /**
     * Run a checkpassword on a basic user from the ApacheDS server, but expect it to fail. This
     * could fail with a null user or an exception.
     */
    public void loginUserShouldFail() throws Exception {
        try {
            String dnReturned = servlet.checkPassword(vmmUser1, vmmUser1pwd);
            if (dnReturned == null) {
                // expected
                return;
            }
            fail("Running a check password on " + vmmUser1 + " should have failed. Returned: " + dnReturned);
        } catch (Exception e) {
            // expected
        }
    }

    /**
     * Run a checkpassword on a basic user from the UnboundID server.
     *
     * @throws Exception
     */
    public void loginUserUnboundID() throws Exception {
        assertDNsEqual("Authentication should succeed for " + UNBOUNDID_USER,
                       UNBOUNDID_USER_DN, servlet.checkPassword(UNBOUNDID_USER, UNBOUNDID_PWD));
    }

    /**
     * Run a checkpassword on a basic user from the UnboundID server, but expect it to fail. This
     * could fail with a null user or an exception.
     */
    public void loginUserShouldFailUnboundID() throws Exception {
        try {
            String dnReturned = servlet.checkPassword(UNBOUNDID_USER, UNBOUNDID_USER);
            if (dnReturned == null) {
                // expected
                return;
            }
            fail("Running a check password on " + UNBOUNDID_USER + " should have failed.");
        } catch (Exception e) {
            // expected
        }
    }

    /**
     * Configure the embedded LDAP server from the UnboundID package for use with multiple registries.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    public static void setupUnboundIDLdapServer() throws Exception {
        Log.info(c, "setupUnboundIDLdapServer", "Setting up the unbound ID server for " + UNBOUNDID_BASE_DN);
        if (ds1 != null) {
            Log.info(c, "setupUnboundIDLdapServer", "Unbound ID server already setup.");
        }
        ds1 = new InMemoryLDAPServer(UNBOUNDID_BASE_DN);

        com.unboundid.ldap.sdk.Entry entry = new com.unboundid.ldap.sdk.Entry(UNBOUNDID_BASE_DN);
        entry.addAttribute("objectclass", "organization");
        entry.addAttribute("o", "ibm.com");
        ds1.add(entry);

        entry = new com.unboundid.ldap.sdk.Entry(UNBOUNDID_USER_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", UNBOUNDID_USER);
        entry.addAttribute("sn", UNBOUNDID_USER);
        entry.addAttribute("cn", UNBOUNDID_USER);
        entry.addAttribute("userPassword", UNBOUNDID_PWD);
        ds1.add(entry);

        // Double check we can get a connection
        LDAPConnection conn = ds1.getLdapServer().getConnection();
        conn.close();

        Log.info(c, "setupUnboundIDLdapServer", "Set up the unbound ID server");
    }

    /**
     * Stop the UnboundID Ldap Server
     *
     * @throws Exception
     */
    public static void stopUnboundIDLdapServer() throws Exception {
        Log.info(c, "stopUnboundIDLdapServer", "Stopping the unbound ID server");

        if (ds1 != null) {
            ds1.shutDown(true);
            ds1 = null;
        }

        Log.info(c, "stopUnboundIDLdapServer", "Stopped the unbound ID server");
    }

    /**
     * Create an LdapRegistry config for the UnboundID LDAP server
     *
     * @return
     */
    public static LdapRegistry getLdapRegistryForUnboundID() {
        return LdapKerberosUtils.getUnboundIDRegistry(ds1.getLdapPort(), UNBOUNDID_BASE_DN);
    }

    /**
     * Create, start and add the UnboundID Ldap registry. Do various tests with default allowOps=false.
     *
     * @param newServer
     * @throws Exception
     */
    public void bodyOfMultiRegistryTest(ServerConfiguration newServer) throws Exception {
        Log.info(c, testName.getMethodName(), "Stop and restart the ApacheDS servers");
        setupUnboundIDLdapServer();

        LdapRegistry unboundID = getLdapRegistryForUnboundID();
        newServer.getLdapRegistries().add(unboundID);

        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Stop all of the ApacheDS servers");
        ApacheDSandKDC.stopAllServers();

        Log.info(c, testName.getMethodName(), "With allowOp=false, both registries should fail to login");
        loginUserShouldFailUnboundID();
        loginUserShouldFail();

        Log.info(c, testName.getMethodName(), "Start all of the ApacheDS servers");
        ApacheDSandKDC.startAllServers();

        Log.info(c, testName.getMethodName(), "After apacheDS restart, all logins should succeed.");
        loginUserUnboundID();
        loginUser();
    }

    /**
     * Create, start and add the UnboundID Ldap registry. Do various tests with default allowOps=true
     *
     * @param newServer
     * @throws Exception
     */
    public void bodyOfMultiRegistryTestAllowOp(ServerConfiguration newServer) throws Exception {
        Log.info(c, testName.getMethodName(), "Stop and restart the ApacheDS servers");
        setupUnboundIDLdapServer();

        LdapRegistry unboundID = getLdapRegistryForUnboundID();
        newServer.getLdapRegistries().add(unboundID);

        // Enable allowOpIfRepoDown
        FederatedRepository federatedRepository = createFederatedRepository(newServer, "OneLDAPRealm", new String[] { BASE_DN, UNBOUNDID_BASE_DN });
        federatedRepository.getPrimaryRealm().setAllowOpIfRepoDown(true);
        updateConfigDynamically(server, newServer);

        // Stop ApacheDS, with default behavior, we should not fail on the other registry
        ApacheDSandKDC.stopAllServers();

        loginUserUnboundID();
        loginUserShouldFail();

        // Start Apache DS, should succeed
        ApacheDSandKDC.startAllServers();

        loginUserUnboundID();
        loginUser();

    }

    /**
     * Reset LdapRegistry to the valid ticketCache
     *
     * @param ldap
     */
    protected void resetTicketCache(LdapRegistry ldap) {
        ldap.setKrb5TicketCache(ticketCacheFile);
    }

    /**
     * Return a basic LdapRegistry with the default ticketCacheFile, caches and context pool disabled
     *
     * @return
     */
    protected LdapRegistry getLdapRegistryWithTicketCache() {
        return LdapKerberosUtils.getTicketCacheWithoutContextPool(ldapServerHostName, LDAP_PORT, ticketCacheFile);
    }

    /**
     * Return a basic LdapRegistry with the default ticketCacheFile, caches and context pool enabled
     *
     * @return
     */
    protected LdapRegistry getLdapRegistryWithTicketCacheWithContextPool() {
        return LdapKerberosUtils.getTicketCache(ldapServerHostName, LDAP_PORT, ticketCacheFile, false, false);
    }

    /**
     * Return a basic LdapRegistry with the default ticketCacheFile, context pool enabled and caches disabled
     *
     * @return
     */
    protected LdapRegistry getLdapRegistryWithTicketCacheWithContextPoolWithoutCaches() {
        return LdapKerberosUtils.getTicketCache(ldapServerHostName, LDAP_PORT, ticketCacheFile, false, true);
    }

    /**
     * Return a basic LdapRegistry with the krb5Principal, ready to add a keytab to the Kerberos config, caches and context pool disabled
     *
     * @return
     */
    protected LdapRegistry getLdapRegistryForKeytab() {
        return LdapKerberosUtils.getKrb5PrincipalNameWithoutContextPool(ldapServerHostName, LDAP_PORT);
    }

    /**
     * Return a basic LdapRegistry with the krb5Principal, ready to add a keytab to the Kerberos config, caches and context pool disabled
     *
     * @return
     */
    protected LdapRegistry getLdapRegistryForKeytabWithContextPool() {
        return LdapKerberosUtils.getKrb5PrincipalName(ldapServerHostName, LDAP_PORT, false, false);
    }

    /**
     * Stop all the ApacheDS servers, logins will fail. Restart the ApacheDS servers, LdapRegistry should
     * recover and logins succeed again.
     *
     * @throws Exception
     */
    public void bodyOfRestartServer() throws Exception {
        loginUser();

        Log.info(c, testName.getMethodName(), "Stop all of the ApacheDS servers");
        ApacheDSandKDC.stopAllServers();

        loginUserShouldFail();

        Log.info(c, testName.getMethodName(), "Start all of the ApacheDS servers");
        ApacheDSandKDC.startAllServers();

        Log.info(c, testName.getMethodName(), "After apacheDS restart, all logins should succeed.");
        loginUser();
    }

    /**
     * Test krb5Principal name failures with and without a Domain name attached (user vs user@dDOMAIN)
     *
     * @param newServer
     * @param ldap
     * @param failMessage
     * @throws Exception
     */
    public void bodyOfBadPrincipleName(ServerConfiguration newServer, LdapRegistry ldap, String failMessage) throws Exception {
        loginUserShouldFail();
        /*
         * The same base exception is not always thrown, two options here, either confirms that we tried to use an
         * invalid krb.config file and couldn't find a realm name for the principal.
         */
        boolean foundBadPrincipalName = !server.findStringsInLogsAndTraceUsingMark("CWIML4512E").isEmpty();
        boolean foundRealmNotFound = !server.findStringsInLogsAndTraceUsingMark("Cannot locate default realm").isEmpty(); // java message, could change in future
        assertTrue("Expected to find Kerberos bind failure: Either `CWIML4512E` or `Cannot locate default realm`", foundBadPrincipalName || foundRealmNotFound);

        newServer.getKerberos().configFile = configFile; // reset to valid config file so the realm name is found
        ldap.setKrb5Principal("badPrincipalName2@" + DOMAIN);
        updateConfigDynamically(server, newServer);

        loginUserShouldFail();
        assertFalse("Expected to find Kerberos bind failure: " + failMessage, server.findStringsInLogsAndTraceUsingMark(failMessage).isEmpty());

        newServer.getKerberos().configFile = configFile; // reset to valid config file so the realm name is found
        ldap.setKrb5Principal("badPrincipalName5");
        updateConfigDynamically(server, newServer);

        loginUserShouldFail();
        assertFalse("Expected to find Kerberos principalName failure: " + failMessage, server.findStringsInLogsAndTraceUsingMark(failMessage).isEmpty());
    }

    /**
     * Standard server startup message checks.
     */
    public static void startupChecks() {
        assertNotNull("Application userRegistry does not appear to have started.",
                      server.waitForStringInLog("CWWKZ0001I:.*userRegistry"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("Server did not came up",
                      server.waitForStringInLog("CWWKF0011I"));
    }
}
