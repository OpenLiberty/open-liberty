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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests Kerberos bind (GSSAPI) for Ldap, using primarily the krb5TicketCache
 *
 * The tests in this bucket are longer running. Server restarts, restarting ApacheDS,
 * waiting for expiring tickets, etc.
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
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
     * Create a ticketCache with a short expiration. Confirm that we fail once the ticket is expired.
     *
     * The ticket is marked as renewable, we should not have to manually renew.
     *
     * @throws Exception
     */
    @Test
    public void ticketExpiresAtRuntime() throws Exception {
        Log.info(c, testName.getMethodName(), "Provide an expiring ticket, renew at runtime");

        String expiringTicketCache = ApacheDSandKDC.createTicketCacheShortLife(true);

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        ldap.setKrb5TicketCache(expiringTicketCache);
        newServer.getLdapRegistries().add(ldap);
        addKerberosConfig(newServer);
        updateConfigDynamically(server, newServer);

        // Mark file so we can check for trace updates
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        loginUser(); // createDirContext will do the initial bind

        loginUser(); // principal will be fetch from the KerberosService cache and renewed.

        /*
         * Tried to check the ccache directly, but endTime is not updated.
         * Checking trace that we had to do a renew.
         */
        String renewTrace = "Successfully renewed ticket"; // trace from  LRUCache for KerberosService

        assertNotNull("Expected to see trace that we renewed the ticket: " + renewTrace, server.waitForStringInTraceUsingMark(renewTrace));

    }

    /**
     * Create a ticketCache with a short expiration. Confirm that we fail once the ticket is expired.
     *
     * The ticket is marked as not renewable, manually get an updated ticketCache and check that we
     * can login again.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("javax.security.auth.login.LoginException")
    public void ticketExpiresAtRuntimeNoRenew() throws Exception {
        Log.info(c, testName.getMethodName(), "Provide an expiring ticket that is not renewable");

        String expiringTicketCache = ApacheDSandKDC.createTicketCacheShortLife(false);

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        ldap.setKrb5TicketCache(expiringTicketCache);
        newServer.getLdapRegistries().add(ldap);
        addKerberosConfig(newServer);
        updateConfigDynamically(server, newServer);

        loginUser();

        Log.info(c, testName.getMethodName(), "Sleep for ticket expiration time. Max Time: " + ApacheDSandKDC.MIN_LIFE);
        long maxTime = System.currentTimeMillis() + ApacheDSandKDC.MIN_LIFE;
        while (System.currentTimeMillis() < maxTime) {
            Thread.sleep(30000);
            if (servlet.checkPassword(vmmUser1, vmmUser1pwd) != null) {
                Log.info(c, testName.getMethodName(), "Login not expired, sleep again.");
            } else {
                break;
            }
        }

        Log.info(c, testName.getMethodName(), "Sleep complete! Log in user again, expect to fail.");
        loginUserShouldFail();

        Log.info(c, testName.getMethodName(), "Updating a fresh ticketCache, should be able to login again.");
        expiringTicketCache = ApacheDSandKDC.createTicketCacheShortLife(true);
        ldap.setKrb5TicketCache(expiringTicketCache);
        updateConfigDynamically(server, newServer);

        loginUser();

    }

}
