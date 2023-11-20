/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

public abstract class CloudFATServletClient extends FATServletClient {

    private static boolean _isDerby;

    public static LibertyServer server1;
    public static LibertyServer server2;

    public static String APP_NAME;
    public static String SERVLET_NAME;

    private static SetupRunner _runner;

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

        try {
            assertNotNull(server2.getServerName() + " did not recover for " + server1RecoveryId,
                          server2.waitForStringInTraceUsingMark("Performed recovery for " + server1RecoveryId,
                                                                FATUtils.LOG_SEARCH_TIMEOUT));

            // Check to see that the peer recovery log files have been deleted
            checkLogAbsence();
        } finally {
            FATUtils.stopServers(server2);
        }
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