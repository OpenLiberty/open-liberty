/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
package tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;

import componenttest.annotation.AllowedFFDC;
import componenttest.topology.impl.LibertyServer;

public abstract class CloudFATServletClient extends CloudTestBase {

    private static boolean _isDerby;

    public static String APP_NAME;
    public static String SERVLET_NAME;

    protected static SetupRunner _runner;

    public static LibertyServer longLeaseCompeteServer1;
    public static LibertyServer server2fastcheck;

    protected static void initialize(LibertyServer s1, LibertyServer s2, String appName, String servletName) {
        initialize(s1, s2, appName, servletName, null);
    }

    protected static void initialize(LibertyServer s1, LibertyServer s2, String appName, String servletName, SetupRunner runner) {
        server1 = s1;
        server2 = s2;

        APP_NAME = appName;
        SERVLET_NAME = appName + servletName;

        _runner = runner;
    }

    /**
     * The purpose of this test is to verify that simple peer transaction recovery works multiple times.
     *
     * The FSCloud001 server is started and halted by a servlet that leaves an indoubt transaction.
     * FSCloud002, a peer server as it belongs to the same recovery group is started and recovery the
     * transaction that belongs to FSCloud001. Twice.
     *
     * @throws Exception
     */
    @Test
    public void testDoubleRecoveryTakeover() throws Exception {
        final String method = "testDoubleRecoveryTakeover";
        StringBuilder sb = null;
        String id = "Core";

        if (_isDerby) {
            return;
        }

        serversToCleanup = new LibertyServer[] { server1, server2 };

        // Start Server1
        FATUtils.startServers(_runner, server1);

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, SERVLET_NAME, "setupRec" + id);
        } catch (IOException e) {
        }
        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);

        assertNotNull(server1.getServerName() + " did not crash properly", server1.waitForStringInTraceUsingMark(XAResourceImpl.DUMP_STATE));

        // At this point server1's recovery log should (absolutely!) be present
        checkLogPresence();

        String server1RecoveryId = server1.getServerConfiguration().getTransaction().getRecoveryIdentity().replaceAll("\\W", "");

        // Now start server2
        FATUtils.startServers(_runner, server2);
        server2.resetLogMarks();
        assertNotNull(server2.getServerName() + " did not recover for " + server1RecoveryId,
                      server2.waitForStringInTraceUsingMark("Performed recovery for " + server1RecoveryId,
                                                            FATUtils.LOG_SEARCH_TIMEOUT));

        // Check to see that the peer recovery log has been deleted
        checkLogAbsence();

        server2.setTraceMarkToEndOfDefaultTrace();

        // Start Server1
        FATUtils.startServers(_runner, server1);

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, SERVLET_NAME, "setupRec" + id);
        } catch (IOException e) {
        }
        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);

        assertNotNull(server1.getServerName() + " did not crash properly", server1.waitForStringInTraceUsingMark(XAResourceImpl.DUMP_STATE));

        assertNotNull(server2.getServerName() + " did not recover for " + server1RecoveryId,
                      server2.waitForStringInTraceUsingMark("Performed recovery for " + server1RecoveryId,
                                                            FATUtils.LOG_SEARCH_TIMEOUT));

        // Check to see that the peer recovery log files have been deleted
        checkLogAbsence();
    }

    @Before
    public void before() {
        if (isDerby()) {
            longLeaseCompeteServer1.setFFDCChecking(true);
            server2fastcheck.setFFDCChecking(true);
        }
    }

    /**
     * Test aggressive takeover of recovery logs by a home server
     * Server2 will resume peer recovery (and fail) after server1 has reclaimed its logs
     *
     * @throws Exception
     */
    @Test
    public void testAggressiveTakeover1() throws Exception {
        final String method = "testAggressiveTakeover1";
        StringBuilder sb = null;

        if (!isDerby()) { // Embedded Derby cannot support tests with concurrent server startup
            serversToCleanup = new LibertyServer[] { longLeaseCompeteServer1, server2fastcheck };

            FATUtils.startServers(_runner, longLeaseCompeteServer1);

            try {
                // We expect this to fail since it is gonna crash the server
                sb = runTestWithResponse(longLeaseCompeteServer1, SERVLET_NAME, "setupRecForAggressiveTakeover1");
            } catch (IOException e) {
            }
            Log.info(this.getClass(), method, "back from runTestWithResponse, sb is " + sb);

            // wait for 1st server to have gone away
            assertNotNull(longLeaseCompeteServer1.getServerName() + " did not crash", longLeaseCompeteServer1.waitForStringInLog(XAResourceImpl.DUMP_STATE));
            longLeaseCompeteServer1.postStopServerArchive(); // must explicitly collect since crashed server
            // The server has been halted but its status variable won't have been reset because we crashed it. In order to
            // setup the server for a restart, set the server state manually.
            longLeaseCompeteServer1.setStarted(false);

            // Now start server2
            server2fastcheck.setHttpDefaultPort(Integer.getInteger("HTTP_secondary"));
            FATUtils.startServers(_runner, server2fastcheck);

            // Now start server1
            FATUtils.startServers(_runner, longLeaseCompeteServer1);

            assertNotNull("Peer recovery was not interrupted",
                          server2fastcheck.waitForStringInTrace("WTRN0107W: Server with identity cloud0021 attempted but failed to recover the logs of peer server cloud0011",
                                                                FATUtils.LOG_SEARCH_TIMEOUT));

            // Server appears to have started ok. Check for key string to see whether recovery has succeeded, irrespective of what server2fastcheck has done
            assertNotNull("Local recovery failed", longLeaseCompeteServer1.waitForStringInLog("CWRLS0012I:", FATUtils.LOG_SEARCH_TIMEOUT));

            FATUtils.runWithRetries(() -> runTestWithResponse(longLeaseCompeteServer1, SERVLET_NAME, "checkRecAggressiveTakeover").toString());
        }
    }

    /**
     * Test aggressive takeover of recovery logs by a home server. Take over at different point in server2's processing.
     * Server2 will continually try (and fail) to recover server1 until it shuts down
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC(value = { "com.ibm.tx.jta.XAResourceNotAvailableException", "java.lang.RuntimeException" }) // should be expected but..... Derby
    public void testAggressiveTakeover2() throws Exception {
        final String method = "testAggressiveTakeover2";
        StringBuilder sb = null;

        if (!isDerby()) { // Embedded Derby cannot support tests with concurrent server startup
            serversToCleanup = new LibertyServer[] { longLeaseCompeteServer1, server2fastcheck };

            FATUtils.startServers(_runner, longLeaseCompeteServer1);

            try {
                // We expect this to fail since it is gonna crash the server
                sb = runTestWithResponse(longLeaseCompeteServer1, SERVLET_NAME, "setupRecForAggressiveTakeover2");
            } catch (IOException e) {
            }
            Log.info(this.getClass(), method, "back from runTestWithResponse, sb is " + sb);

            // wait for 1st server to have gone away
            assertNotNull(longLeaseCompeteServer1.getServerName() + " did not crash", longLeaseCompeteServer1.waitForStringInLog(XAResourceImpl.DUMP_STATE));
            longLeaseCompeteServer1.postStopServerArchive(); // must explicitly collect since crashed server
            // The server has been halted but its status variable won't have been reset because we crashed it. In order to
            // setup the server for a restart, set the server state manually.
            longLeaseCompeteServer1.setStarted(false);

            // Now start server2
            server2fastcheck.setHttpDefaultPort(Integer.getInteger("HTTP_secondary"));
            FATUtils.startServers(_runner, server2fastcheck);

            // Now start server1. This will continually try and fail to recover server1
            FATUtils.startServers(_runner, longLeaseCompeteServer1);

            // Server appears to have started ok. Check for key string to see whether recovery has succeeded, irrespective of what server2fastcheck has done
            assertNotNull("Local recovery failed", longLeaseCompeteServer1.waitForStringInLog("CWRLS0012I:", FATUtils.LOG_SEARCH_TIMEOUT));

            FATUtils.runWithRetries(() -> runTestWithResponse(longLeaseCompeteServer1, SERVLET_NAME, "checkRecAggressiveTakeover").toString());
        }
    }

    protected abstract void setupRedundantLease(String serverName) throws Exception;

    protected abstract boolean checkRedundantLeaseExists(String serverName) throws Exception;

    /*
     * Setup a lease for a non-existant server and check it gets deleted without creating bogus new recovery logs
     */
    @Test
    public void testRedundantLeaseDeletion() throws Exception {

        if (this.getClass().getCanonicalName().contains("DBRotationTest")) {
            Log.info(CloudFATServletClient.class, "testRedundantLeaseDeletion", "Not implemented for DB tests yet");
            return;
        }

        final String nonexistantServerName = "nonexistant";

        serversToCleanup = new LibertyServer[] { server2fastcheck };

        setupRedundantLease(nonexistantServerName);

        // Now start server2
        server2fastcheck.setHttpDefaultPort(Integer.getInteger("HTTP_secondary"));
        FATUtils.startServers(_runner, server2fastcheck);

        assertNotNull(server2fastcheck.getServerName() + " did not perform peer recovery",
                      server2fastcheck.waitForStringInTrace("< peerRecoverServers Exit"));

        assertFalse("Redundant lease was not deleted", checkRedundantLeaseExists(nonexistantServerName));
    }

    public static void setDerby() {
        _isDerby = true;
    }

    public static boolean isDerby() {
        return _isDerby;
    }

    /**
     * @throws Exception
     */
    protected abstract void checkLogPresence() throws Exception;

    /**
     * @throws Exception
     */
    protected abstract void checkLogAbsence() throws Exception;
}