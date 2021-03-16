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

import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertFalse;

import java.io.File;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Kerberos;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests Kerberos bind (GSSAPI) for Ldap, using primarily the krb5TicketCache.
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class TicketCacheBindTest extends CommonBindTest {

    private static final Class<?> c = TicketCacheBindTest.class;

    @BeforeClass
    public static void setStopMessages() {
        stopStrings = new String[] { "CWIML4507E", "CWIML4513E", "CWIML4515E", "CWIML4520E", "CWIML4529E", "CWIML0004E", "CWWKE0701E", "CWWKS3005E" };
    }

    /**
     * Run golden path tests for bindAuthMech=GSSAPI, a valid krbPrincipalName and valid krb5TicketCache
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords(LdapKerberosUtils.BIND_PASSWORD)
    public void basicLoginChecks() throws Exception {
        Log.info(c, testName.getMethodName(), "Run basic login checks with a standard configuration");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        addKerberosConfig(newServer);
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        baselineTests();
    }

    /**
     * Run golden path tests for bindAuthMech=GSSAPI, a valid krbPrincipalName and valid krb5TicketCache
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords(LdapKerberosUtils.BIND_PASSWORD)
    @AllowedFFDC("javax.naming.NamingException") // temporary, remove when Issue #16231 is fixed
    public void basicLoginChecksWithContextPool() throws Exception {
        Log.info(c, testName.getMethodName(), "Run basic login checks with a standard configuration");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCacheWithContextPool();
        addKerberosConfig(newServer);
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        baselineTests();
    }

    /**
     * Provide an empty ticketCache, the file exists, but no content
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.naming.NamingException", "javax.security.auth.login.LoginException" })
    @Test
    public void emptyTicketCache() throws Exception {
        Log.info(c, testName.getMethodName(), "Run an empty ticket cache");

        File emptyCC = File.createTempFile("empty", "cc");
        emptyCC.deleteOnExit();

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        ldap.setKrb5TicketCache(emptyCC.getAbsolutePath());
        newServer.getLdapRegistries().add(ldap);
        addKerberosConfig(newServer);
        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Login expected to fail, ticketCache exists, but is empty");
        loginUserShouldFail();

        assertFalse("Expected to find Kerberos bind failure: CWIML4507E", server.findStringsInLogsAndTraceUsingMark("CWIML4507E").isEmpty());
    }

    /**
     * Supply a readonly file for the ticketCache
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.naming.NamingException", "javax.security.auth.login.LoginException" })
    @Test
    public void readOnlyTicketCache() throws Exception {
        /*
         * Setting the file to unreadable only works on *nix systems.
         */
        Assume.assumeTrue(!LdapKerberosUtils.isWindows("readOnlyTicketCache"));

        Log.info(c, testName.getMethodName(), "Run with an unreadable ticketCache");

        File unreadableFile = File.createTempFile("unreadable", ".cc");
        unreadableFile.setReadable(false);
        unreadableFile.deleteOnExit();

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        ldap.setKrb5TicketCache(unreadableFile.getAbsolutePath());
        newServer.getLdapRegistries().add(ldap);
        addKerberosConfig(newServer);
        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Login expected to fail, ticketCache is unreadable");
        loginUserShouldFail();
        assertFalse("Expected to find Kerberos bind failure: CWIML4513E", server.findStringsInLogsAndTraceUsingMark("CWIML4513E").isEmpty());
    }

    /**
     * Supply a file that doesn't exist for the ticketCache
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.naming.NamingException", "javax.security.auth.login.LoginException" })
    @Test
    public void nonExistentTicketCache() throws Exception {
        Log.info(c, testName.getMethodName(), "Run with a nonexistent ticketCache");

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        ldap.setKrb5TicketCache("fileThatDoesNotExist.cc");
        newServer.getLdapRegistries().add(ldap);
        addKerberosConfig(newServer);
        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Login expected to fail, ticketCache isn't a valid file");
        loginUserShouldFail();
        assertFalse("Expected to find Kerberos bind failure: " + "CWIML4515E", server.findStringsInLogsAndTraceUsingMark("CWIML4515E").isEmpty());
    }

    /**
     * Provide a principalName that doesn't exist in the cache
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.naming.NamingException", "javax.security.auth.login.LoginException" })
    @Test
    public void badPrincipalName() throws Exception {
        Log.info(c, testName.getMethodName(), "Run with a bad principal name");

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        ldap.setKrb5Principal("badPrincipalName");
        newServer.getLdapRegistries().add(ldap);
        addKerberosConfig(newServer);
        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Login expected to fail, config has a bad principalName");
        loginUserShouldFail();

        assertFalse("Expected to find Kerberos bind failure: CWIML4507E", server.findStringsInLogsAndTraceUsingMark("CWIML4507E").isEmpty());
    }

    /**
     * Do not provide a principal name, should print an exception and the servlet login should fail.
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.naming.NamingException", "javax.security.auth.login.LoginException", "com.ibm.wsspi.security.wim.exception.MissingInitPropertyException" })
    @Test
    public void missingPrincipalName() throws Exception {
        Log.info(c, testName.getMethodName(), "Run without a principal name");

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        ldap.setKrb5Principal(null);
        newServer.getLdapRegistries().add(ldap);
        addKerberosConfig(newServer);
        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Login expected to fail, config is missing the principalName");
        loginUserShouldFail();

        assertFalse("Expected to find Kerberos bind failure: CWIML0004E", server.findStringsInLogsAndTraceUsingMark("CWIML0004E").isEmpty());
    }

    /**
     * Provide an empty principal name
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.naming.NamingException", "javax.security.auth.login.LoginException" })
    @Test
    public void emptyPrincipalName() throws Exception {
        Log.info(c, testName.getMethodName(), "Run with an empty principal name");

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        ldap.setKrb5Principal("");
        newServer.getLdapRegistries().add(ldap);
        addKerberosConfig(newServer);
        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Login expected to fail, the krb5PrincipalName is empty");
        loginUserShouldFail();

        assertFalse("Expected to find missing principal name: CWWKE0701E", server.findStringsInLogsAndTraceUsingMark("CWWKE0701E").isEmpty());
    }

    /**
     * Start with a valid ticketCache, add a valid keytab, change the ticketCache to a bad one, then remove the bad ticketCache.
     *
     * All logins should be successful as we'll try the ticketCache, if configured, and then the keytab, if configured.
     *
     * @throws Exception
     */
    @Test
    public void swapToKeytab() throws Exception {
        Log.info(c, testName.getMethodName(), "Start with a ticket cache");

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        addKerberosConfig(newServer);
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        loginUser();

        Log.info(c, testName.getMethodName(), "Add valid keytab, should be successful");
        Kerberos kerb = newServer.getKerberos();
        kerb.configFile = configFile;
        kerb.keytab = keytabFile;

        loginUser();

        Log.info(c, testName.getMethodName(), "Change the ticketCache to a bad config, should be successful because we'll use the keytab");
        ldap.setKrb5TicketCache("badCache.cc");

        loginUser();

        Log.info(c, testName.getMethodName(), "Remove the bad ticketCache, should be successful because we'll use the keytab");
        ldap.setKrb5TicketCache(null);

        loginUser();
    }

    /**
     * Start with an expired, but previously valid ticketcache
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.naming.NamingException", "javax.security.auth.login.LoginException" })
    @Test
    public void startWithExpiredTicketCache() throws Exception {
        Log.info(c, testName.getMethodName(), "Start with an expired ticketCache");

        String expiredCache = expiredTicketCache;
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        ldap.setKrb5TicketCache(expiredCache);
        newServer.getLdapRegistries().add(ldap);
        addKerberosConfig(newServer);
        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Login expected to fail, the ticketCache is expired");
        loginUserShouldFail();
        assertFalse("Expected to find Kerberos bind failure: CWIML4520E", server.findStringsInLogsAndTraceUsingMark("CWIML4520E").isEmpty());

        Log.info(c, testName.getMethodName(), "Update with valid ticketCache, next login should succeed");
        resetTicketCache(ldap);
        updateConfigDynamically(server, newServer);

        loginUser();
    }

    /**
     * Provide a config file with a bad port for the KDC server
     *
     * @throws Exception
     */
    @Test
    public void configFileWrongKDCPort() throws Exception {
        Log.info(c, testName.getMethodName(), "Update to a config file with a bad port for the KDC server");

        String badConfigFile = ApacheDSandKDC.createConfigFile("badConfig-", -1);

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        newServer.getLdapRegistries().add(ldap);
        Kerberos kerb = newServer.getKerberos();
        kerb.configFile = badConfigFile;

        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Login expected to fail, the krb.conf file has a bad port defined for the KDC server.");
        loginUserShouldFail();

        assertFalse("Expected to find Kerberos bind failure: CWIML4520E", server.findStringsInLogsAndTraceUsingMark("CWIML4520E").isEmpty());
    }

}
