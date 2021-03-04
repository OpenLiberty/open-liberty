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
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Kerberos;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.adapter.ldap.LdapConstants;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests Kerberos bind (GSSAPI) for Ldap, doing various tests for the Kerberos config file
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class Krb5ConfigTest extends CommonBindTest {

    private static final Class<?> c = Krb5ConfigTest.class;

    @BeforeClass
    public static void setStopMessages() {
        stopStrings = new String[] { "CWIML4520E", "CWWKS4345E", "CWIML4508E", "CWIML4507E", "CWIML4529E" };
    }

    /**
     * Run golden path tests for bindAuthMech=GSSAPI, with a Kerberos element config file defined in
     * a JVM property with TicketCache. (TicketCacheBindTest and KeytabBindTest cover defining the config attribute
     * in the Kerberos config).
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords(LdapKerberosUtils.BIND_PASSWORD)
    public void basicLoginChecksConfigJVM_TicketCache() throws Exception {
        Log.info(c, testName.getMethodName(), "Run basic login checks with a standard configuration");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        addKerberosConfig(newServer);
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        baselineTests();
    }

    /**
     * Run golden path tests for bindAuthMech=GSSAPI, with a Kerberos element config file defined in
     * a JVM property with TicketCache. (TicketCacheBindTest and KeytabBindTest cover defining the config attribute
     * in the Kerberos config).
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords(LdapKerberosUtils.BIND_PASSWORD)
    public void basicLoginChecksConfigJVM_keytab() throws Exception {
        Log.info(c, testName.getMethodName(), "Run basic login checks with a standard configuration");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryForKeytab();
        addKerberosConfigAndKeytab(newServer);
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        baselineTests();
    }

    /**
     * Provide an empty config, the file exists, but no content
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.naming.NamingException", "javax.security.auth.login.LoginException" })
    @Test
    public void emptyConfigFile_TicketCache() throws Exception {
        Log.info(c, testName.getMethodName(), "Run an empty config using ticketCache");

        File emptyConfig = File.createTempFile("emptyConfig", "krb");
        emptyConfig.deleteOnExit();

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        newServer.getLdapRegistries().add(ldap);
        Kerberos kerb = newServer.getKerberos();
        kerb.configFile = emptyConfig.getAbsolutePath();
        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Login expected to fail, config exists, but is empty.");
        loginUserShouldFail();

        assertFalse("Expected to find Kerberos bind failure: CWIML4520E", server.findStringsInLogsAndTraceUsingMark("CWIML4520E").isEmpty());
    }

    /**
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.naming.NamingException", "javax.security.auth.login.LoginException" })
    @Test
    public void emptyConfigFile_Keytab() throws Exception {
        Log.info(c, testName.getMethodName(), "Run an empty config using keytab");

        File emptyConfig = File.createTempFile("emptyConfig", "krb");
        emptyConfig.deleteOnExit();

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryForKeytab();
        newServer.getLdapRegistries().add(ldap);
        Kerberos kerb = newServer.getKerberos();
        kerb.keytab = keytabFile;
        kerb.configFile = emptyConfig.getAbsolutePath();
        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Login expected to fail, config exists, but is empty.");
        loginUserShouldFail();

        assertFalse("Expected to find Kerberos bind failure: CWIML4520E", server.findStringsInLogsAndTraceUsingMark("CWIML4520E").isEmpty());

    }

    /**
     * Add a config file that does not exist, using ticketCache
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.security.auth.login.LoginException", "javax.naming.AuthenticationException" })
    @Test
    public void noConfig_ticketCache() throws Exception {
        Log.info(c, testName.getMethodName(), "Run with a config that does not exist, using ticketCache");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        newServer.getLdapRegistries().add(ldap);
        Kerberos kerb = newServer.getKerberos();
        kerb.configFile = "thisfiledoesnotexist.krb";

        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Login should fail with config file that doesn't exist.");

        loginUserShouldFail();
        assertFalse("Expected to find Kerberos bind failure: CWWKS4345E", server.findStringsInLogsAndTraceUsingMark("CWWKS4345E").isEmpty());

    }

    /**
     * Add a config file that does not exist, using keytab
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.security.auth.login.LoginException", "javax.naming.AuthenticationException" })
    @Test
    public void noConfig_keytab() throws Exception {
        Log.info(c, testName.getMethodName(), "Run with a config that does not exist, using ticketCache");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryForKeytab();
        newServer.getLdapRegistries().add(ldap);
        Kerberos kerb = newServer.getKerberos();
        kerb.keytab = keytabFile;
        kerb.configFile = "thisfiledoesnotexist.krb";

        updateConfigDynamically(server, newServer);

        Log.info(c, testName.getMethodName(), "Login should fail with config file that doesn't exist.");

        loginUserShouldFail();
        assertFalse("Expected to find Kerberos bind failure: CWWKS4345E", server.findStringsInLogsAndTraceUsingMark("CWWKS4345E").isEmpty());
    }

    /**
     * Start with a valid config defined, swap to a bad config, then a valid config.
     *
     * ContextPool is enabled and should reset the contextPool when the config is updated.
     *
     * Use a keytab as well.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "com.ibm.wsspi.security.wim.exception.WIMSystemException", "javax.naming.NamingException" })
    public void swapConfig_keytab() throws Exception {
        Log.info(c, testName.getMethodName(), "Start with keytab and valid config");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryForKeytabWithContextPool();
        newServer.getLdapRegistries().add(ldap);
        Kerberos kerb = addKerberosConfigAndKeytab(newServer);
        updateConfigDynamically(server, newServer);

        bodySwapConfig(newServer, kerb);
    }

    /**
     * Start with a valid config defined, swap to a bad config, then a valid config.
     *
     * ContextPool is enabled and should reset the contextPool when the config is updated.
     *
     * Use a ticketCache as well
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "com.ibm.wsspi.security.wim.exception.WIMSystemException", "javax.naming.AuthenticationException" })
    public void swapConfig_ticketCache() throws Exception {
        Log.info(c, testName.getMethodName(), "Start with keytab and valid config");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCacheWithContextPool();
        newServer.getLdapRegistries().add(ldap);
        Kerberos kerb = addKerberosConfig(newServer);
        updateConfigDynamically(server, newServer);

        bodySwapConfig(newServer, kerb);
    }

    /**
     * Verify user can log in with standard config. Swap to a bad config file, then a valid config.
     *
     * ContextPool is enabled and should reset the contextPool when the config is updated.
     *
     * @param newServer
     * @param kerb
     * @throws Exception
     */
    private void bodySwapConfig(ServerConfiguration newServer, Kerberos kerb) throws Exception {
        loginUser();

        Log.info(c, testName.getMethodName(), "Update to a bad config, LdapRegistry should update and then fail to login");
        kerb.configFile = ApacheDSandKDC.createInvalidConfigFile("invalidConfig-", KDC_PORT);
        updateConfigDynamically(server, newServer);

        assertNotNull("Should have created the contextPool after the config update. Trace msg expected: " + LdapConstants.KERBEROS_UDPATE_MSG,
                      server.waitForStringInTrace(LdapConstants.KERBEROS_UDPATE_MSG));

        Log.info(c, testName.getMethodName(), "User login should fail -- search cache should also be cleared on keytab update");
        loginUserShouldFail();

        Log.info(c, testName.getMethodName(), "Update to a valid config, LdapRegistry should update and then successfully login");
        kerb.configFile = configFile;
        updateConfigDynamically(server, newServer);

        assertNotNull("Should have created the contextPool after the config update. Trace msg expected: " + LdapConstants.KERBEROS_UDPATE_MSG,
                      server.waitForStringInTrace(LdapConstants.KERBEROS_UDPATE_MSG));

        loginUser();
    }

    /**
     * Remove the valid config during runtime
     *
     * @throws Exception
     */
    @AllowedFFDC("javax.security.auth.login.LoginException")
    @Test
    public void removedConfig_TicketCache() throws Exception {
        Log.info(c, testName.getMethodName(), "Remove the valid config during runtime.");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        newServer.getLdapRegistries().add(ldap);
        Kerberos kerb = addKerberosConfig(newServer);

        updateConfigDynamically(server, newServer);

        bodyRemovedConfig(newServer, kerb, "CWIML4507E", false);

    }

    /**
     * Remove the valid config during runtime
     *
     * @throws Exception
     */
    @AllowedFFDC("javax.security.auth.login.LoginException")
    @Test
    public void removedConfig_keytab() throws Exception {
        Log.info(c, testName.getMethodName(), "Remove the valid config during runtime.");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        newServer.getLdapRegistries().add(ldap);
        Kerberos kerb = addKerberosConfig(newServer);

        updateConfigDynamically(server, newServer);

        bodyRemovedConfig(newServer, kerb, "CWIML4508E", false);

    }

    /**
     * Remove the valid config during runtime
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.wsspi.security.wim.exception.WIMSystemException", "javax.security.auth.login.LoginException", "javax.naming.AuthenticationException" })
    @Test
    public void removedConfig_TicketCacheContextPool() throws Exception {
        Log.info(c, testName.getMethodName(), "Remove the valid config during runtime.");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCacheWithContextPool();
        newServer.getLdapRegistries().add(ldap);
        Kerberos kerb = addKerberosConfig(newServer);

        updateConfigDynamically(server, newServer);

        bodyRemovedConfig(newServer, kerb, "CWIML4507E", true);

    }

    /**
     * Remove the valid config during runtime
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "com.ibm.wsspi.security.wim.exception.WIMSystemException", "javax.naming.NamingException" })
    public void removedConfig_keytabContextPool() throws Exception {
        Log.info(c, testName.getMethodName(), "Remove the valid config during runtime.");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryForKeytabWithContextPool();
        newServer.getLdapRegistries().add(ldap);
        Kerberos kerb = addKerberosConfigAndKeytab(newServer);

        updateConfigDynamically(server, newServer);

        bodyRemovedConfig(newServer, kerb, "CWIML4508E", true);

    }

    private void bodyRemovedConfig(ServerConfiguration newServer, Kerberos kerb, String failMessage, boolean contextPool) throws Exception {
        loginUser();

        Log.info(c, testName.getMethodName(), "Remove the config, we should fail as the system config doesn't work for us.");
        kerb.configFile = null;
        updateConfigDynamically(server, newServer);

        if (contextPool) {
            assertNotNull("Should have created the contextPool after the config update. Trace msg expected: " + LdapConstants.KERBEROS_UDPATE_MSG,
                          server.waitForStringInTrace(LdapConstants.KERBEROS_UDPATE_MSG));
        }

        loginUserShouldFail();
        /*
         * This error message need to be adjusted as we run on different test machines. On my box with no
         * krb.ini, the root failure message is
         * Caused by: GSSException: Invalid name provided (Mechanism level: KrbException: Cannot locate default realm)
         * which is caught and logged by CWIML4520E. But on another box, if it finds a default realm in a default config, it
         * may have a different message.
         */
        boolean noCWIML4520E = server.findStringsInLogsAndTraceUsingMark("CWIML4520E").isEmpty();
        boolean noLoginFailure = server.findStringsInLogsAndTraceUsingMark(failMessage).isEmpty();
        assertFalse("Expected to find Kerberos bind failure: either " + "CWIML4520E or " + failMessage, noCWIML4520E && noLoginFailure);

        kerb.configFile = configFile;
        updateConfigDynamically(server, newServer);

        if (contextPool) {
            assertNotNull("Should have created the contextPool after the config update. Trace msg expected: " + LdapConstants.KERBEROS_UDPATE_MSG,
                          server.waitForStringInTrace(LdapConstants.KERBEROS_UDPATE_MSG));
        }
        loginUser();
    }
}
