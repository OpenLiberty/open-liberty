/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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

import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class DualServerPeerLockingTest2 extends DualServerPeerLockingTest {
    /**
     * This test verifies that a server that crashes with an indoubt transaction is able to immediately
     * recover the transaction on restart, where HADB Locking is enabled.
     *
     * The Cloud001 server is started and a servlet invoked to halt leaving an indoubt transaction. Cloud001 is
     * restarted and transaction recovery verified.
     *
     * @throws Exception
     */
    @Test
    public void testLocalServerAcquiresLogImmediately() throws Exception {
        int test = 1;
        final String method = "testLocalServerAcquiresLogImmediately";
        final String id = String.format("%03d", test);
        StringBuilder sb = null;
        boolean testFailed = false;
        String testFailureString = "";

        servers = new LibertyServer[] { firstServer };

        // Start Server1
        FATUtils.startServers(firstServer);

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(firstServer, servletName, "setupRec" + id);
        } catch (Throwable e) {
            // as expected
            Log.error(this.getClass(), method, e); // TODO remove this
        }
        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);

        // wait for 1st server to have gone away
        if (firstServer.waitForStringInLog("Dump State:") == null) {
            testFailed = true;
            testFailureString = "First server did not crash";
        }

        firstServer.postStopServerArchive(); // explicitly collect logs

        if (!testFailed) {
            // restart 1st server
            //
            // Under the HADB locking scheme, the server should be able to acquire the logs immediately and proceed
            // with recovery.
            firstServer.startServerAndValidate(false, true, true);

            if (firstServer.waitForStringInTrace("WTRN0133I") == null) {
                testFailed = true;
                testFailureString = "Recovery incomplete on first server";
            }

            // check resource states
            Log.info(this.getClass(), method, "calling checkRec" + id);
            try {
                sb = runTestWithResponse(firstServer, servletName, "checkRec" + id);
            } catch (Exception e) {
                Log.error(this.getClass(), "dynamicTest", e);
                firstServer.postStopServerArchive(); // explicitly collect logs
                throw e;
            }
            Log.info(this.getClass(), method, "checkRec" + id + " returned: " + sb);

            // Bounce first server to clear log
            FATUtils.stopServers(firstServer);
            firstServer.startServerAndValidate(false, true, true);

            // Check log was cleared
            if (firstServer.waitForStringInTrace("WTRN0135I") == null) {
                testFailed = true;
                testFailureString = "Transactions left in transaction log on first server";
            }
            if (!testFailed && (firstServer.waitForStringInTrace("WTRN0134I.*0") == null)) {
                testFailed = true;
                testFailureString = "XAResources left in partner log on first server";
            }
        }

        if (testFailed)
            fail(testFailureString);
    }

    /**
     * This test verifies that a Liberty server CANNOT recover the logs belonging to a peer server that has crashed
     * with an indoubt transaction where HADB Locking is enabled and the lock is of long duration.
     *
     * The Cloud001 server is started with HADB Locking enabled and a servlet invoked to halt leaving an indoubt
     * transaction. The Cloud002 server is started with server.xml that includes attributes to enable and configure HADB
     * peer locking such that a peer lock is of long duration. The trace logs are checked to verify that no peer recovery
     * has occurred.
     *
     * The Cloud001 server is restarted and transaction recovery verified.
     *
     * @throws Exception
     */
    @Test
    public void testPeerServerDoesNotAcquireLogs() throws Exception {
        int test = 2;

        final String method = "testPeerServerDoesNotAcquireLogs";
        final String id = String.format("%03d", test);
        boolean testFailed = false;
        String testFailureString = "";

        servers = new LibertyServer[] { firstServer, longPeerStaleTimeServer2 };

        // Start Server1
        FATUtils.startServers(firstServer);

        try {
            // We expect this to fail since it is gonna crash the server
            runTest(firstServer, servletName, "setupRec" + id);
        } catch (Throwable e) {
            // as expected
        }

        // wait for 1st server to have gone away
        if (firstServer.waitForStringInLog("Dump State:") == null) {
            testFailed = true;
            testFailureString = "First server did not crash";
        }

        // Now start server2
        if (!testFailed) {
            longPeerStaleTimeServer2.setHttpDefaultPort(longPeerStaleTimeServer2.getHttpSecondaryPort());
            ProgramOutput po = longPeerStaleTimeServer2.startServerAndValidate(false, true, true);
            if (po.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not start server2");
                Log.error(this.getClass(), method, ex);
                throw ex;
            }

            // wait for 2nd server to attempt (but fail) to perform peer recovery
            int numStringOccurrences = longPeerStaleTimeServer2.waitForMultipleStringsInLog(2, "CWRLS0011I.*cloud001", 60000);
            if (numStringOccurrences < 2) {
                testFailed = true;
                testFailureString = "Second server did not attempt peer recovery at least 2 times, attempted " + numStringOccurrences;
            }
            if (!testFailed && (longPeerStaleTimeServer2.waitForStringInLog("Peer recovery will not be attempted, this server was unable to claim the logs") == null)) {
                testFailed = true;
                testFailureString = "Server2 did not report that Peer recovery had failed";
            }

            //Stop server2
            if (!testFailed) {
                FATUtils.stopServers(longPeerStaleTimeServer2);
            }
        }

        if (!testFailed) {
            // restart 1st server
            //
            // Under the HADB locking scheme, the server should be able to acquire the logs immediately and proceed
            // with recovery. server2 will still have the lease at this point so we'll have to wait the leaseLength
            // (20 seconds) before this will definitely succeed
            Thread.sleep(20000);
            ProgramOutput po = firstServer.startServerAndValidate(false, true, true);
            if (po.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not restart server1");
                Log.error(this.getClass(), method, ex);
                throw ex;
            }

            if (firstServer.waitForStringInTrace("WTRN0133I") == null) {
                testFailed = true;
                testFailureString = "Recovery incomplete on first server";
            }

            // check resource states
            Log.info(this.getClass(), method, "calling checkRec" + id);
            try {
                runTest(firstServer, servletName, "checkRec" + id);
            } catch (Exception e) {
                Log.error(this.getClass(), "dynamicTest", e);
                throw e;
            }

            // Bounce first server to clear log
            FATUtils.stopServers(firstServer);
            po = firstServer.startServerAndValidate(false, true, true);
            if (po.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not bounce server1");
                Log.error(this.getClass(), method, ex);
                throw ex;
            }

            // Check log was cleared
            if (firstServer.waitForStringInTrace("WTRN0135I") == null) {
                testFailed = true;
                testFailureString = "Transactions left in transaction log on first server";
            }
            if (!testFailed && (firstServer.waitForStringInTrace("WTRN0134I.*0") == null)) {
                testFailed = true;
                testFailureString = "XAResources left in partner log on first server";
            }
        }

        if (testFailed)
            fail(testFailureString);
    }

    /**
     * This test verifies that a Liberty server DOES recover its own logs if they have been locked for recovery by a peer
     * server where HADB Locking is enabled and the lock is of long duration.
     *
     * The acquisition of Cloud001's logs by Cloud002 is simulated - in practice Cloud002 will assert ownership of
     * Cloud001's logs and recover them. This test drives a servlet to manually change the ownership of the logs
     * in the control row. The Cloud002 server is started, the servlet is run and Cloud002 stopped.
     *
     * The Cloud001 server is started with a server.xml that includes attributes to enable and configure HADB
     * peer locking. Local recovery should occur. Cloud001's server.xml is reset and the server is restarted to tidy up.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC(value = { "com.ibm.ws.recoverylog.spi.RecoveryFailedException", "java.lang.RuntimeException" })
    public void testLocalServerDoesAcquireLogs() throws Exception {
        int test = 3;

        final String method = "testLocalServerDoesAcquireLogs";
        final String id = String.format("%03d", test);
        boolean testFailed = false;
        String testFailureString = "";

        servers = new LibertyServer[] { secondServer, longPeerStaleTimeServer1 };

        // Start Server2
        secondServer.setHttpDefaultPort(secondServer.getHttpSecondaryPort());
        FATUtils.startServers(secondServer);

        // Set the owner of our recovery logs to a peer in the control row through a servlet
        // This simulates a peer's acquisition of our recovery logs.
        runTest(secondServer, servletName, "setPeerOwnership");

        FATUtils.stopServers(secondServer);
        if (!testFailed) {
            // restart 1st server
            //
            // Under the HADB locking scheme, the local server SHOULD aqcuire the logs
            longPeerStaleTimeServer1.startServerAndValidate(false, true, true);

            // wait for server to attempt to perform local recovery
            if (!testFailed && (longPeerStaleTimeServer1.waitForStringInTrace("Claim the partner_log for the local server") == null)) {
                testFailed = true;
                testFailureString = "Server failed to claim logs";
            }
        }

        if (testFailed)
            fail(testFailureString);
    }

    /**
     * The purpose of this test is to simulate the change in use of the RUSECTION_ID column in the control
     * row depending on whether HADB Locking is enabled or not.
     *
     * The Cloud001 server is started with HADB Locking disabled (no peer locking attributes in server.xml) and a
     * servlet invoked to set the latch in the RUSECTION_ID column in the control row of the WAS_PARTNER_LOG table.
     * The server is then halted by a servlet that leaves an indoubt transaction. Cloud001's server.xml is replaced
     * with a new server.xml that includes attributes to enable and configure HADB peer locking. The server is
     * restarted and transaction recovery verified.
     *
     * @throws Exception
     */
    @Test
    public void testSetLatchLocalServer() throws Exception {
        int test = 1;
        final String method = "testSetLatchLocalServer";
        final String id = String.format("%03d", test);
        boolean testFailed = false;
        String testFailureString = "";

        servers = new LibertyServer[] { peerLockingDisabledServer1, peerLockingEnabledServer1 };

        // switch to configuration with HADB peer locking disabled
        // Start Server1
        FATUtils.startServers(peerLockingDisabledServer1);

        // Set the latch in the control row through a servlet
        runTest(peerLockingDisabledServer1, servletName, "setLatch");

        try {
            // We expect this to fail since it is gonna crash the server
            runTest(peerLockingDisabledServer1, servletName, "setupRec" + id);
        } catch (IOException e) {
            // as expected
        }

        // wait for 1st server to have gone away
        if (peerLockingDisabledServer1.waitForStringInLog(XAResourceImpl.DUMP_STATE) == null) {
            testFailed = true;
            testFailureString = "First server did not crash";
        }

        if (!testFailed) {
            // switch to configuration with HADB peer locking ENABLED

            // restart 1st server
            //
            // Under the HADB locking scheme, the server should be able to acquire the logs immediately and proceed
            // with recovery.
            peerLockingEnabledServer1.startServerAndValidate(false, true, true);

            if (peerLockingEnabledServer1.waitForStringInTrace("WTRN0133I") == null) {
                testFailed = true;
                testFailureString = "Recovery incomplete on first server";
            }

            if (!testFailed && (peerLockingEnabledServer1.waitForStringInTrace("Claim the partner_log for the local server") == null)) {
                testFailed = true;
                testFailureString = "Server failed to claim logs";
            }

            // check resource states
            if (!testFailed) {
                Log.info(this.getClass(), method, "calling checkRec" + id);
                runTest(peerLockingEnabledServer1, servletName, "checkRec" + id);

                // Bounce first server to clear log
                FATUtils.stopServers(peerLockingEnabledServer1);
                peerLockingEnabledServer1.startServerAndValidate(false, true, true);

                // Check log was cleared
                if (peerLockingEnabledServer1.waitForStringInTrace("WTRN0135I") == null) {
                    testFailed = true;
                    testFailureString = "Transactions left in transaction log on first server";
                }
                if (!testFailed && (peerLockingEnabledServer1.waitForStringInTrace("WTRN0134I.*0") == null)) {
                    testFailed = true;
                    testFailureString = "XAResources left in partner log on first server";
                }
            }
        }

        if (testFailed)
            fail(testFailureString);
    }

    /**
     * The purpose of this test is to simulate the change in use of the RUSECTION_ID column in the control
     * row depending on whether HADB Locking is enabled or not.
     *
     * The Cloud001 server is started with HADB Locking disabled (no peer locking attributes in server.xml) and a
     * servlet invoked to set the latch in the RUSECTION_ID column in the control row of the WAS_PARTNER_LOG table.
     * The server is then halted by a servlet that leaves an indoubt transaction.
     *
     * The Cloud002 server is started (its server.xml includes attributes to enable and configure HADB peer locking).
     * Successful peer recovery is verified. Finally, the Cloud001 server is re-started with HADB Locking disabled.
     *
     * @throws Exception
     */
    @Test
    public void testSetLatchPeerServer() throws Exception {
        int test = 1;
        final String method = "testSetLatchPeerServer";
        final String id = String.format("%03d", test);
        boolean testFailed = false;
        String testFailureString = "";

        servers = new LibertyServer[] { peerLockingDisabledServer1, secondServer };

        // switch to configuration with HADB peer locking disabled
        // Start Server1
        FATUtils.startServers(peerLockingDisabledServer1);

        // Set the latch in the control row through a servlet
        runTest(peerLockingDisabledServer1, servletName, "setLatch");

        try {
            // We expect this to fail since it is gonna crash the server
            runTest(peerLockingDisabledServer1, servletName, "setupRec" + id);
        } catch (Throwable e) {
            // as expected
        }

        // wait for 1st server to have gone away
        if (peerLockingDisabledServer1.waitForStringInLog(XAResourceImpl.DUMP_STATE) == null) {
            testFailed = true;
            testFailureString = "First server did not crash";
        }

        // Now start server2
        if (!testFailed) {
            secondServer.setHttpDefaultPort(secondServer.getHttpSecondaryPort());
            ProgramOutput po = secondServer.startServerAndValidate(false, true, true);

            if (po.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not start server2");
                Log.error(this.getClass(), "dynamicTest", ex);
                throw ex;
            }

            // wait for 2nd server to perform peer recovery
            if (secondServer.waitForStringInTrace("Performed recovery for " + cloud1RecoveryIdentity) == null) {
                testFailed = true;
                testFailureString = "Second server did not perform peer recovery";
            }

            if (!testFailed && (secondServer.waitForStringInTrace("Claim peer partner_log from a peer server") == null)) {
                testFailed = true;
                testFailureString = "Server failed to claim peer logs";
            }
        }

        // flush the resource states
        if (!testFailed) {
            runTest(secondServer, servletName, "dumpState");

            //Stop server2
            FATUtils.stopServers(secondServer);

            // restart 1st server
            peerLockingDisabledServer1.startServerAndValidate(false, true, true);

            if (peerLockingDisabledServer1.waitForStringInTrace("WTRN0133I") == null) {
                testFailed = true;
                testFailureString = "Recovery incomplete on first server";
            }
        }

        if (!testFailed) {
            // check resource states
            Log.info(this.getClass(), method, "calling checkRec" + id);
            runTest(peerLockingDisabledServer1, servletName, "checkRec" + id);

            // Bounce first server to clear log
            FATUtils.stopServers(peerLockingDisabledServer1);
            peerLockingDisabledServer1.startServerAndValidate(false, true, true);

            // Check log was cleared
            if (peerLockingDisabledServer1.waitForStringInTrace("WTRN0135I") == null) {
                testFailed = true;
                testFailureString = "Transactions left in transaction log on first server";
            }
            if (!testFailed && (peerLockingDisabledServer1.waitForStringInTrace("WTRN0134I.*0") == null)) {
                testFailed = true;
                testFailureString = "XAResources left in partner log on first server";
            }
        }

        if (testFailed)
            fail(testFailureString);
    }
}