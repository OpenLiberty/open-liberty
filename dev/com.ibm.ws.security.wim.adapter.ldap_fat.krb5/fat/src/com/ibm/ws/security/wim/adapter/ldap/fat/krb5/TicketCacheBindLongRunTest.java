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
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests Kerberos bind (GSSAPI) for Ldap, using primarily the krb5TicketCache
 *
 * The tests in this bucket are longer running. Server restarts, restarting ApacheDS, etc.
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 9)
public class TicketCacheBindLongRunTest extends CommonBindTest {

    private static final Class<?> c = TicketCacheBindLongRunTest.class;

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
    @AllowedFFDC({ "javax.naming.CommunicationException", "javax.security.auth.login.LoginException" })
    public void restartLdapServerNoContextPool() throws Exception {
        Log.info(c, testName.getMethodName(), "Stop and restart the ApacheDS servers");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        addKerberosConfig(newServer);
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
    @AllowedFFDC({ "javax.naming.CommunicationException", "javax.security.auth.login.LoginException" })
    public void restartLdapServerWithContextPool() throws Exception {
        Log.info(c, testName.getMethodName(), "Stop and restart the ApacheDS servers");
        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCacheWithContextPool();
        addKerberosConfig(newServer);
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);

        bodyOfRestartServer();
    }

    /**
     * Verify that when we dynamic add a <kerberos/> and <ldapRegistry/> config, we do a Kerberos modify
     * before a LdapRegistry init to ensure we load with the valid config file.
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords({ LdapKerberosUtils.BIND_PASSWORD, LdapApacheDSandKDC.vmmUser1pwd })
    public void dynamimcUpdateLoop() throws Exception {
        int numUpdates = 30;

        Log.info(c, testName.getMethodName(), "Run login check with dynamic update " + numUpdates + " times.");

        for (int i = 0; i < numUpdates; i++) {
            ServerConfiguration newServer = emptyConfiguration.clone();
            LdapRegistry ldap = getLdapRegistryWithTicketCacheWithContextPool();
            addKerberosConfig(newServer);
            newServer.getLdapRegistries().add(ldap);
            updateConfigDynamically(server, newServer);

            loginUser();

            resetServerConfig();
        }
    }

    /**
     * Verify that when we start the server with <kerberos/> and <ldapRegistry/> config, we do a Kerberos init
     * before a LdapRegistry init to ensure we load with the valid config file.
     *
     * @throws Exception
     */
    @Test
    @CheckForLeakedPasswords({ LdapKerberosUtils.BIND_PASSWORD, LdapApacheDSandKDC.vmmUser1pwd })
    public void serverRestartLoop() throws Exception {
        int numUpdates = 5;

        Log.info(c, testName.getMethodName(), "Run login check with sever restart " + numUpdates + " times.");

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCacheWithContextPool();
        addKerberosConfig(newServer);
        newServer.getLdapRegistries().add(ldap);
        updateConfigDynamically(server, newServer);
        loginUser();

        for (int i = 0; i < numUpdates; i++) {
            server.stopServer("CWIML4520E"); // have to supply stop server message here as it picks up the message from the LdapRestart tests
            server.startServer();
            startupChecks();

            Log.info(c, "setUp", "Creating servlet connection the server");
            servlet = new UserRegistryServletConnection(server.getHostname(), server.getHttpDefaultPort());

            loginUser();
        }
    }

}
