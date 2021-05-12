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
import static org.junit.Assert.assertTrue;

import org.apache.directory.server.core.api.DirectoryService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.security.wim.ConfigConstants;
import com.ibm.websphere.simplicity.config.Kerberos;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests the bindAuthMechanism for regression (simple, none).
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@MinimumJavaLevel(javaLevel = 9)
public class SimpleBindTest extends CommonBindTest {

    private static final Class<?> c = SimpleBindTest.class;

    @BeforeClass
    public static void setStopMessages() {
        stopStrings = new String[] { "CWIML4529E", "CWWKG0032W", "CWIML4520E" };
    }

    /**
     * Semi-regression test for golden path, set bindAuthMechanism to simple.
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords(LdapKerberosUtils.BIND_PASSWORD)
    public void basicLoginChecksForSimple() throws Exception {
        Log.info(c, testName.getMethodName(), "Run basic login checks with bindAuthMechanism set to simple");

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithSimpleBind();
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        baselineTests();
    }

    /**
     * et bindAuthMechanism to something invalid
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords(LdapKerberosUtils.BIND_PASSWORD)
    public void invalidBindAuthMechanism() throws Exception {
        Log.info(c, testName.getMethodName(), "Set bindAuthMechanism to an invalid value.");

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithSimpleBind();
        ldap.setBindAuthMechanism("badAuthMech");
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        assertFalse("Expected to find invalid config warning: CWWKG0032W", server.findStringsInLogsAndTraceUsingMark("CWWKG0032W").isEmpty());
    }

    /**
     * Regression test, should log in as usual without setting bindAuthMechanism
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords(LdapKerberosUtils.BIND_PASSWORD)
    public void basicLoginChecksSimple_withoutBindAuth() throws Exception {
        Log.info(c, testName.getMethodName(), "Run basic login checks with no bindAuthMechanism set.");

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithSimpleBind();
        ldap.setBindAuthMechanism(null);
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        baselineTests();
    }

    /**
     * Set bindAuthMech to none and tests with/without the DirectoryService allowing anonymous bind.
     *
     * @throws Exception
     */
    @AllowedFFDC("javax.naming.NoPermissionException")
    @Test
    public void basicLoginChecksNoneBindAuth() throws Exception {
        Log.info(c, testName.getMethodName(), "Run basic login checks with bindAuthMech of none, with and without allowing anon access.");

        DirectoryService ds = ApacheDSandKDC.getDirectoryService();
        assertNotNull("DirectoryService is null, cannot update anon access.", ds);
        try {
            Log.info(c, testName.getMethodName(), "Updating DirectoryService to allow anonymous bind.");
            ds.setAllowAnonymousAccess(true);

            ServerConfiguration newServer = emptyConfiguration.clone();
            LdapRegistry ldap = getLdapRegistryWithSimpleBind();
            ldap.setBindDN(null);
            ldap.setBindPassword(null);
            ldap.setBindAuthMechanism(ConfigConstants.CONFIG_AUTHENTICATION_TYPE_NONE);
            newServer.getLdapRegistries().add(ldap);
            updateConfigDynamically(server, newServer);

            Log.info(c, testName.getMethodName(), "Basic login should be successful with bindAuth=none, allowed by DirectoryService.");
            baselineTests();
        } finally {
            Log.info(c, testName.getMethodName(), "Updating DirectoryService to block anonymous bind.");
            ds.setAllowAnonymousAccess(false);
        }

        Log.info(c, testName.getMethodName(), "Basic logins should fail with bindAuth=none, blocked by DirectoryService.");
        loginUserShouldFail();
    }

    /**
     * Regression test. Do not set bindAuthMech and run tests with/without DirectoryService allowing anonymous bind.
     *
     * @throws Exception
     */
    @AllowedFFDC("javax.naming.NoPermissionException")
    @Test
    public void basicLoginChecksNone_withoutBindAuth() throws Exception {
        Log.info(c, testName.getMethodName(), "Run basic login checks with none implied, with allowing anon access.");

        DirectoryService ds = ApacheDSandKDC.getDirectoryService();
        assertNotNull("DirectoryService is null, cannot update anon access.", ds);
        try {
            Log.info(c, testName.getMethodName(), "Updating DirectoryService to allow anonymous bind.");
            ds.setAllowAnonymousAccess(true);

            ServerConfiguration newServer = emptyConfiguration.clone();
            LdapRegistry ldap = getLdapRegistryWithSimpleBind();
            ldap.setBindDN(null);
            ldap.setBindPassword(null);
            ldap.setBindAuthMechanism(null);
            newServer.getLdapRegistries().add(ldap);
            updateConfigDynamically(server, newServer);

            Log.info(c, testName.getMethodName(), "Basic login should be successful with bindAuth=none, allowed by DirectoryService.");
            baselineTests();
        } finally {
            Log.info(c, testName.getMethodName(), "Updating DirectoryService to block anonymous bind.");
            ds.setAllowAnonymousAccess(false);
        }

        Log.info(c, testName.getMethodName(), "Basic logins should fail with bindAuth=none, blocked by DirectoryService.");
        loginUserShouldFail();
    }

    /**
     * Get an LdapRegistry with Simple bindAuthMechanism set and bindDN and bindPassword
     *
     * @return
     */
    private LdapRegistry getLdapRegistryWithSimpleBind() {
        return LdapKerberosUtils.getSimpleBind(ldapServerHostName, LDAP_PORT);
    }

    /**
     * Rotate config from simple to Kerberos to simple and verify that we update LdapRegistry at the
     * correct times.
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords(LdapKerberosUtils.BIND_PASSWORD)
    public void swapFromSimpleToKerberos() throws Exception {
        Log.info(c, testName.getMethodName(), "Start with Simple bind");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithSimpleBind();
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        loginUser();

        Log.info(c, testName.getMethodName(), "Add a valid keytab and config file, should be no change to LdapRegistry (modify received should no-op)");
        Kerberos kerb = newServer.getKerberos();
        kerb.configFile = configFile;
        kerb.keytab = keytabFile;
        updateConfigDynamically(server, newServer);
        assertFalse("Expected to find Kerberos file updates: CWWKS4346I", server.findStringsInLogsAndTraceUsingMark("CWWKS4346I").isEmpty());
        loginUser();
        String traceMsg = "Kerberos is enabled and the KerberosService was updated";
        assertTrue("LdapRegistry should not update and log: " + traceMsg, server.findStringsInLogsAndTraceUsingMark(traceMsg).isEmpty());

        Log.info(c, testName.getMethodName(), "Add a krb5Principal and enable kerberos, LdapRegistry should restart");
        ldap.setKrb5Principal(bindPrincipalName);
        ldap.setBindAuthMechanism(ConfigConstants.CONFIG_BIND_AUTH_KRB5);
        updateConfigDynamically(server, newServer);
        loginUser();
        String ldapModify = "LdapAdapter.*> modified Entry";
        assertFalse("LdapRegistry should modify: " + ldapModify, server.findStringsInLogsAndTraceUsingMark(ldapModify).isEmpty());

        Log.info(c, testName.getMethodName(), "Swap to a different valid config file, LdapRegistry should process a modify.");
        String altConfigFile = ApacheDSandKDC.createConfigFile("altConfig-", KDC_PORT, true, false);
        kerb.configFile = altConfigFile;
        updateConfigDynamically(server, newServer);
        loginUser();
        assertFalse("LdapRegistry should update and log: " + traceMsg, server.findStringsInLogsAndTraceUsingMark(traceMsg).isEmpty());

        Log.info(c, testName.getMethodName(), "Remove the keytab, add a ticketCache, LdapRegistry should restart");
        kerb.keytab = null;
        ldap.setBindAuthMechanism(ConfigConstants.CONFIG_BIND_AUTH_KRB5);
        ldap.setKrb5Principal(bindPrincipalName);
        ldap.setKrb5TicketCache(ticketCacheFile);
        updateConfigDynamically(server, newServer);

        loginUser();
        assertFalse("LdapRegistry should modify: " + ldapModify, server.findStringsInLogsAndTraceUsingMark(ldapModify).isEmpty());

        Log.info(c, testName.getMethodName(), "Swap back to simple bind, LdapRegistry should restart");
        ldap.setBindAuthMechanism(ConfigConstants.CONFIG_AUTHENTICATION_TYPE_SIMPLE);
        ldap.setBindDN(LdapKerberosUtils.BIND_SIMPLE_DN);
        ldap.setBindPassword(LdapKerberosUtils.BIND_PASSWORD);
        updateConfigDynamically(server, newServer);
        loginUser();
        assertFalse("LdapRegistry should modify: " + ldapModify, server.findStringsInLogsAndTraceUsingMark(ldapModify).isEmpty());

        Log.info(c, testName.getMethodName(), "Remove the kerberos config, should be no change to LdapRegistry (modify received should no-op)");
        kerb = null;
        updateConfigDynamically(server, newServer);
        loginUser();
        assertTrue("LdapRegistry should not update and log: " + traceMsg, server.findStringsInLogsAndTraceUsingMark(traceMsg).isEmpty());

    }
}
