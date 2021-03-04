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
import static org.junit.Assert.assertNotNull;

import org.apache.directory.server.core.api.DirectoryService;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.security.wim.ConfigConstants;
import com.ibm.websphere.simplicity.config.Kerberos;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests the bindAuthMechanism for regression (simple, none).
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class SimpleBindTest extends CommonBindTest {

    private static final Class<?> c = SimpleBindTest.class;

    /**
     * Semi-regression test for golden path, set bindAuthMechanism to simple.
     *
     * @throws Exception
     */
    @Test
    public void basicLoginChecksForSimple() throws Exception {
        Log.info(c, testName.getMethodName(), "Run basic login checks with bindAuthMechanism set to simple");

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithSimpleBind();
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        baselineTests();
    }

    /**
     * Regression test, should log in as usual without setting bindAuthMechanism
     *
     * @throws Exception
     */
    @Test
    public void basicLoginChecksNoBindAuth() throws Exception {
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
     * Get an LdapRegistry with Simple bindAuthMechanism set and bindDN and bindPassword
     *
     * @return
     */
    private LdapRegistry getLdapRegistryWithSimpleBind() {
        return LdapKerberosUtils.getSimpleBind(ldapServerHostName, LDAP_PORT);
    }

    //@Test
    // to-do kristip: test still in-progress
    public void swapFromSimpleToKerberos() throws Exception {
        Log.info(c, testName.getMethodName(), "Start with Simple bind");

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithSimpleBind();
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        loginUser();

        Log.info(c, testName.getMethodName(), "Add a valid keytab and config file, should be no change to LdapRegistry");
        // add valid keytab and config, should be no-change
        Kerberos kerb = newServer.getKerberos();
        kerb.configFile = configFile;
        kerb.keytab = keytabFile;

        updateConfigDynamically(server, newServer);
        // check that LdapRegistry didn't recycle
        loginUser();

        Log.info(c, testName.getMethodName(), "Add a krb5Principal and enable kerberos, LdapRegistry should restart");
        ldap = LdapKerberosUtils.getKrb5PrincipalName(ldapServerHostName, LDAP_PORT);
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        // LdapRegistry should have recycled
        loginUser();

        Log.info(c, testName.getMethodName(), "Remove the keytab, add a ticketCache, LdapRegistry should restart");
        // remove keytab, add cache
        kerb.keytab = null;
        ldap = LdapKerberosUtils.getTicketCache(ldapServerHostName, LDAP_PORT, ticketCacheFile);
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        // LdapRegistry should have recycled
        loginUser();

        Log.info(c, testName.getMethodName(), "Swap back to simple bind, LdapRegistry should restart");
        ldap = getLdapRegistryWithSimpleBind();
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        // LdapRegistry should have recycled
        loginUser();

    }
}
