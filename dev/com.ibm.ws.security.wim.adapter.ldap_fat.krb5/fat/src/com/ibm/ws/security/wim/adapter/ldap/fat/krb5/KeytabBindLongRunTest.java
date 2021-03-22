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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests Kerberos bind (GSSAPI) for Ldap, using primarily the Kerberos keytab and doing
 * longer running tests (server or ldap restarts).
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 9)
public class KeytabBindLongRunTest extends CommonBindTest {

    private static final Class<?> c = KeytabBindLongRunTest.class;

    @BeforeClass
    public static void setStopMessages() {
        stopStrings = new String[] { "CWIML4520E" };
    }

    /**
     * Stop and restart the ldapServer and verify that we will fail when the ldapServer is down and succeed when it is up again.
     *
     * Run with context pool and caches disabled
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "javax.naming.CommunicationException", "javax.security.auth.login.LoginException", "javax.naming.NamingException" })
    public void restartLdapServerNoContextPool() throws Exception {
        Log.info(c, testName.getMethodName(), "Stop and restart the ApacheDS servers, context pool is disabled");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryForKeytab();
        addKerberosConfigAndKeytab(newServer);
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        bodyOfRestartServer();
    }

    /**
     * Stop and restart the ldapServer and verify that we will fail when the ldapServer is down and succeed when it is up again.
     *
     * Run with context pool and caches enabled
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "javax.naming.CommunicationException", "javax.security.auth.login.LoginException", "javax.naming.NamingException" })
    public void restartLdapServerWithContextPool() throws Exception {
        Log.info(c, testName.getMethodName(), "Stop and restart the ApacheDS servers, contextpool is enabled");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryForKeytabWithContextPool();
        addKerberosConfigAndKeytab(newServer);
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        bodyOfRestartServer();
    }
}
