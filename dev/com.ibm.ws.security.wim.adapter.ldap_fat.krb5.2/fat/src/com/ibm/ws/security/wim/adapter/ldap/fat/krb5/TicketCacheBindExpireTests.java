/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap.fat.krb5;

import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.ContextPool;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests Kerberos bind (GSSAPI) for Ldap, using primarily the krb5TicketCache
 *
 * The tests in this bucket deal with expiring tickets. Separated in their own bucket or
 * we tend to get "Requested start time is later than end time" bleed into other tests.
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 9)
public class TicketCacheBindExpireTests extends CommonBindTest {

    private static final Class<?> c = TicketCacheBindExpireTests.class;

    @BeforeClass
    public static void setStopMessages() {
        stopStrings = new String[] { "CWIML4520E" };
    }

    /**
     * Create a ticketCache with a short expiration. Confirm that we fail once the ticket is expired.
     *
     * The ticket is marked as renewable, we should not have to manually renew.
     *
     * @throws Exception
     */
    @Test
    public void ticketExpiresAtRuntimeNoContextPool() throws Exception {
        Log.info(c, testName.getMethodName(), "Provide an expiring ticket, renew at runtime");

        String expiringTicketCache = ApacheDSandKDC.createTicketCacheShortLife(true);

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCache();
        ldap.setKrb5TicketCache(expiringTicketCache);
        newServer.getLdapRegistries().add(ldap);
        addKerberosConfig(newServer);
        updateConfigDynamically(server, newServer);

        // Mark file so we can check for trace updates
        server.setTraceMarkToEndOfDefaultTrace();

        loginUser(); // createDirContext will do the initial bind

        loginUser(); // principal will be fetched from the KerberosService cache and renewed.

        /*
         * Tried to check the ccache directly, but endTime is not updated.
         * Checking trace that we had to do a renew.
         */
        String renewTrace = "Successfully renewed ticket"; // trace from  LRUCache for KerberosService, could vary on JDK types

        assertNotNull("Expected to see trace that we renewed the ticket: " + renewTrace, server.waitForStringInTraceUsingMark(renewTrace));

    }

    /**
     * Create a ticketCache with a short expiration. Confirm that we fail once the ticket is expired.
     *
     * The ticket is marked as renewable, we should not have to manually renew.
     *
     * @throws Exception
     */
    @Test
    public void ticketExpiresAtRuntimeWithContextPool() throws Exception {
        Log.info(c, testName.getMethodName(), "Provide an expiring ticket, renew at runtime, context pool is enabled");

        String expiringTicketCache = ApacheDSandKDC.createTicketCacheShortLife(true);

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCacheWithContextPoolWithoutCaches();
        ldap.setKrb5TicketCache(expiringTicketCache);
        ldap.setContextPool(new ContextPool(true, 0, 2, 1, "25s", "25s"));
        newServer.getLdapRegistries().add(ldap);
        addKerberosConfig(newServer);
        updateConfigDynamically(server, newServer);

        // Mark file so we can check for trace updates
        server.setTraceMarkToEndOfDefaultTrace();

        /*
         * Tried to check the ccache directly, but endTime is not updated.
         * Checking trace that we had to do a renew.
         */
        String renewTrace = "Successfully renewed ticket"; // trace from  LRUCache for KerberosService

        /*
         * Since we'll reuse the dirContext in the context pool, we won't renew until we get an error from
         * the Ldap server.
         */
        boolean foundRenewMessage = false;
        Log.info(c, testName.getMethodName(), "Sleep for ticket expiration time or ticket renews. Max Time: " + ApacheDSandKDC.MIN_LIFE);
        long maxTime = System.currentTimeMillis() + ApacheDSandKDC.MIN_LIFE;
        while (System.currentTimeMillis() < maxTime) {
            Thread.sleep(30000);
            servlet.checkPassword(vmmUser1, vmmUser1pwd);

            if (!server.findStringsInLogsAndTrace(renewTrace).isEmpty()) {
                foundRenewMessage = true;
                break;
            }
        }

        assertTrue("Expected to see trace that we renewed the ticket: " + renewTrace, foundRenewMessage);

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

        Log.info(c, testName.getMethodName(), "Sleep for ticket expiration time or login fails. Max Time: " + ApacheDSandKDC.MIN_LIFE);
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
    public void ticketExpiresAtRuntimeNoRenewWithContextPool() throws Exception {
        Log.info(c, testName.getMethodName(), "Provide an expiring ticket that is not renewable, context pool is enabled");

        String expiringTicketCache = ApacheDSandKDC.createTicketCacheShortLife(false);

        ServerConfiguration newServer = emptyConfiguration.clone();
        LdapRegistry ldap = getLdapRegistryWithTicketCacheWithContextPoolWithoutCaches();
        ldap.setKrb5TicketCache(expiringTicketCache);
        ldap.setContextPool(new ContextPool(true, 0, 2, 1, "25s", "25s"));
        newServer.getLdapRegistries().add(ldap);

        addKerberosConfig(newServer);
        updateConfigDynamically(server, newServer);

        loginUser();

        Log.info(c, testName.getMethodName(), "Sleep for ticket expiration time or login fails. Max Time: " + ApacheDSandKDC.MIN_LIFE);
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
