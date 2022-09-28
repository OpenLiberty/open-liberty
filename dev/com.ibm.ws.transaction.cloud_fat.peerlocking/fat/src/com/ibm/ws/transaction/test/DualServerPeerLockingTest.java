/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.test.tests.DualServerDynamicTestBase;
import com.ibm.ws.transaction.web.Simple2PCCloudServlet;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class DualServerPeerLockingTest extends DualServerDynamicTestBase {
    @Server("com.ibm.ws.transaction_LKCLOUD001")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server1;

    @Server("com.ibm.ws.transaction_LKCLOUD002")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server2;

    @Server("defaultAttributesServer1")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer defaultAttributesServer1;

    @Server("defaultAttributesServer2")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer defaultAttributesServer2;

    @Server("longPeerStaleTimeServer1")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer longPeerStaleTimeServer1;

    @Server("longPeerStaleTimeServer2")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer longPeerStaleTimeServer2;

    @Server("peerLockingDisabledServer1")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer peerLockingDisabledServer1;

    @Server("peerLockingEnabledServer1")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer peerLockingEnabledServer1;

    public static String[] serverNames = new String[] {
                                                        "com.ibm.ws.transaction_LKCLOUD001",
                                                        "com.ibm.ws.transaction_LKCLOUD002",
                                                        "defaultAttributesServer1",
                                                        "defaultAttributesServer2",
                                                        "longPeerStaleTimeServer1",
                                                        "longPeerStaleTimeServer2",
                                                        "peerLockingDisabledServer1",
                                                        "peerLockingEnabledServer1",
    };

    @BeforeClass
    public static void setUp() throws Exception {
        System.out.println("NYTRACE: DualServerPeerLockingTest.setUp called");
        servletName = APP_NAME + "/Simple2PCCloudServlet";
        cloud1RecoveryIdentity = "cloud001";
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server1, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(server2, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(defaultAttributesServer1, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(defaultAttributesServer2, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(longPeerStaleTimeServer1, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(longPeerStaleTimeServer2, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(peerLockingDisabledServer1, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(peerLockingEnabledServer1, APP_NAME, "com.ibm.ws.transaction.*");

        server1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        server2.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        server2.useSecondaryHTTPPort();
        defaultAttributesServer1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        defaultAttributesServer2.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        defaultAttributesServer2.useSecondaryHTTPPort();
        longPeerStaleTimeServer1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        longPeerStaleTimeServer2.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        longPeerStaleTimeServer2.useSecondaryHTTPPort();
        peerLockingDisabledServer1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        peerLockingEnabledServer1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
    }

    @After
    public void cleanDB() throws Exception {
        server1.deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
    }

    /**
     * This test verifies no regression in transaction recovery behaviour with a pair of servers that have HADB Locking enabled.
     *
     * The Cloud001 server is started and a servlet invoked to halt leaving an indoubt transaction. The Cloud002 server
     * peer recovers the indoubt. Cloud001 is restarted and transaction recovery verified.
     *
     * @throws Exception
     */
    @Test
    public void testDynamicCloudRecovery007() throws Exception {
        dynamicTest(server1, server2, 7, 2);
    }

    /**
     * This test verifies no regression in transaction recovery behaviour with a pair of servers that have HADB Locking enabled.
     *
     * The Cloud001 server is started and a servlet invoked to halt leaving an indoubt transaction. The Cloud002 server
     * peer recovers the indoubt. Cloud001 is restarted and transaction recovery verified.
     *
     * @throws Exception
     */
    @Test
    public void testDynamicCloudRecovery090() throws Exception {
        dynamicTest(server1, server2, 90, 3);
    }

    /**
     * This test repeats testDynamicCloudRecovery007 with HADB Locking enabled but allowing timeBetweenHeartbeats,
     * peerTimeBeforeStale and localTimeBeforeStale attributes to default.
     *
     * The Cloud001 server is started and a servlet invoked to halt leaving an indoubt transaction. The Cloud002 server
     * peer recovers the indoubt. Cloud001 is restarted and transaction recovery verified.
     *
     * @throws Exception
     */
    @Test
    public void testDefaultAttributesCloudRecovery007() throws Exception {
        dynamicTest(defaultAttributesServer1, defaultAttributesServer2, 7, 2);
    }

    /**
     * This test simulates the process by which a homeserver can aggressively reclaim its logs while a peer is in the process
     * of recovering those logs.
     *
     * The Cloud001 server is started and a servlet invoked to halt leaving an indoubt transaction. The Cloud002 server
     * peer recovers the indoubt. But ownership of the transaction logs reverts to Cloud001 even as the Cloud002 server
     * is recovering. Cloud002 should end its recovery processing quietly. Cloud001 is restarted and transaction recovery
     * verified.
     *
     * @throws Exception
     */
    @Test
    public void testDynamicCloudRecoveryInterruptedPeerRecovery() throws Exception {
        dynamicTest(server1, server2, "InterruptedPeerRecovery", 2);
    }

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

        // Start Server1
        FATUtils.startServers(server1);

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, servletName, "setupRec" + id);
        } catch (Throwable e) {
            // as expected
            Log.error(this.getClass(), method, e); // TODO remove this
        }
        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);

        // wait for 1st server to have gone away
        if (server1.waitForStringInLog("Dump State:") == null) {
            testFailed = true;
            testFailureString = "First server did not crash";
        }

        if (!testFailed) {
            // restart 1st server
            //
            // Under the HADB locking scheme, the server should be able to acquire the logs immediately and proceed
            // with recovery.
            server1.startServerAndValidate(false, true, true);

            if (server1.waitForStringInTrace("WTRN0133I") == null) {
                testFailed = true;
                testFailureString = "Recovery incomplete on first server";
            }

            // check resource states
            Log.info(this.getClass(), method, "calling checkRec" + id);
            try {
                sb = runTestWithResponse(server1, servletName, "checkRec" + id);
            } catch (Exception e) {
                Log.error(this.getClass(), "dynamicTest", e);
                throw e;
            }
            Log.info(this.getClass(), method, "checkRec" + id + " returned: " + sb);

            // Bounce first server to clear log
            FATUtils.stopServers(server1);
            server1.startServerAndValidate(false, true, true);

            // Check log was cleared
            if (server1.waitForStringInTrace("WTRN0135I") == null) {
                testFailed = true;
                testFailureString = "Transactions left in transaction log on first server";
            }
            if (!testFailed && (server1.waitForStringInTrace("WTRN0134I.*0") == null)) {
                testFailed = true;
                testFailureString = "XAResources left in partner log on first server";
            }

            FATUtils.stopServers(server1);
        }

        tidyServersAfterTest(server1);
        // XA resource data is cleared in setup servlet methods. Probably should do it here.
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
        StringBuilder sb = null;
        boolean testFailed = false;
        String testFailureString = "";

        try {
            // Start Server1
            FATUtils.startServers(server1);

            try {
                // We expect this to fail since it is gonna crash the server
                sb = runTestWithResponse(server1, servletName, "setupRec" + id);
            } catch (Throwable e) {
                // as expected
                Log.error(this.getClass(), method, e); // TODO remove this
            }
            Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);

            // wait for 1st server to have gone away
            if (server1.waitForStringInLog("Dump State:") == null) {
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
                if (!testFailed && (longPeerStaleTimeServer2.waitForStringInLog("HADB Peer Recovery failed for server") == null)) {
                    testFailed = true;
                    testFailureString = "Server2 did not report that HADB Peer recovery had failed";
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
                ProgramOutput po = server1.startServerAndValidate(false, true, true);
                if (po.getReturnCode() != 0) {
                    Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                    Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                    Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                    Exception ex = new Exception("Could not restart server1");
                    Log.error(this.getClass(), method, ex);
                    throw ex;
                }

                if (server1.waitForStringInTrace("WTRN0133I") == null) {
                    testFailed = true;
                    testFailureString = "Recovery incomplete on first server";
                }

                // check resource states
                Log.info(this.getClass(), method, "calling checkRec" + id);
                try {
                    sb = runTestWithResponse(server1, servletName, "checkRec" + id);
                } catch (Exception e) {
                    Log.error(this.getClass(), "dynamicTest", e);
                    throw e;
                }
                Log.info(this.getClass(), method, "checkRec" + id + " returned: " + sb);

                // Bounce first server to clear log
                FATUtils.stopServers(server1);
                po = server1.startServerAndValidate(false, true, true);
                if (po.getReturnCode() != 0) {
                    Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                    Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                    Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                    Exception ex = new Exception("Could not bounce server1");
                    Log.error(this.getClass(), method, ex);
                    throw ex;
                }

                // Check log was cleared
                if (server1.waitForStringInTrace("WTRN0135I") == null) {
                    testFailed = true;
                    testFailureString = "Transactions left in transaction log on first server";
                }
                if (!testFailed && (server1.waitForStringInTrace("WTRN0134I.*0") == null)) {
                    testFailed = true;
                    testFailureString = "XAResources left in partner log on first server";
                }

            }
        } finally {
            FATUtils.stopServers(server1);
            tidyServersAfterTest(server1, longPeerStaleTimeServer2);
        }

        // XA resource data is cleared in setup servlet methods. Probably should do it here.
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
        StringBuilder sb = null;
        boolean testFailed = false;
        String testFailureString = "";

        // Start Server2
        server2.setHttpDefaultPort(server2.getHttpSecondaryPort());
        FATUtils.startServers(server2);

        // Set the owner of our recovery logs to a peer in the control row through a servlet
        // This simulates a peer's acquisition of our recovery logs.
        try {
            sb = runTestWithResponse(server2, servletName, "setPeerOwnership");

        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "setPeerOwnership" + id + " returned: " + sb);

        FATUtils.stopServers(server2);
        if (!testFailed) {
            // restart 1st server
            //
            // Under the HADB locking scheme, the local server SHOULD aqcuire the logs
            longPeerStaleTimeServer1.startServerAndValidate(false, true, true);

            // wait for server to attempt to perform local recovery
            if (!testFailed && (longPeerStaleTimeServer1.waitForStringInTrace("Claim the logs for the local server") == null)) {
                testFailed = true;
                testFailureString = "Server failed to claim logs";
            }

            //Stop server1
            if (!testFailed) {
                FATUtils.stopServers(longPeerStaleTimeServer1);
            }
        }

        tidyServersAfterTest(longPeerStaleTimeServer1);

        // XA resource data is cleared in setup servlet methods. Probably should do it here.
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
        StringBuilder sb = null;
        boolean testFailed = false;
        String testFailureString = "";

        // switch to configuration with HADB peer locking disabled
        // Start Server1
        FATUtils.startServers(peerLockingDisabledServer1);

        // Set the latch in the control row through a servlet
        try {
            sb = runTestWithResponse(peerLockingDisabledServer1, servletName, "setLatch");

        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "setLatch" + id + " returned: " + sb);

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(peerLockingDisabledServer1, servletName, "setupRec" + id);
        } catch (Throwable e) {
            // as expected
            Log.error(this.getClass(), method, e); // TODO remove this
        }
        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);

        // wait for 1st server to have gone away
        if (peerLockingDisabledServer1.waitForStringInLog("Dump State:") == null) {
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

            if (!testFailed && (peerLockingEnabledServer1.waitForStringInTrace("Claim the logs for the local server") == null)) {
                testFailed = true;
                testFailureString = "Server failed to claim logs";
            }

            // check resource states
            if (!testFailed) {
                Log.info(this.getClass(), method, "calling checkRec" + id);
                try {
                    sb = runTestWithResponse(peerLockingEnabledServer1, servletName, "checkRec" + id);
                } catch (Exception e) {
                    Log.error(this.getClass(), "dynamicTest", e);
                    throw e;
                }
                Log.info(this.getClass(), method, "checkRec" + id + " returned: " + sb);

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

                FATUtils.stopServers(peerLockingEnabledServer1);
            }
        }

        tidyServersAfterTest(peerLockingEnabledServer1);

        // XA resource data is cleared in setup servlet methods. Probably should do it here.
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
        StringBuilder sb = null;
        boolean testFailed = false;
        String testFailureString = "";

        // switch to configuration with HADB peer locking disabled
        // Start Server1
        FATUtils.startServers(peerLockingDisabledServer1);

        // Set the latch in the control row through a servlet
        try {
            sb = runTestWithResponse(peerLockingDisabledServer1, servletName, "setLatch");

        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "setLatch" + id + " returned: " + sb);

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(peerLockingDisabledServer1, servletName, "setupRec" + id);
        } catch (Throwable e) {
            // as expected
            Log.error(this.getClass(), method, e); // TODO remove this
        }
        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);

        // wait for 1st server to have gone away
        if (peerLockingDisabledServer1.waitForStringInLog("Dump State:") == null) {
            testFailed = true;
            testFailureString = "First server did not crash";
        }

        // Now start server2
        if (!testFailed) {
            server2.setHttpDefaultPort(server2.getHttpSecondaryPort());
            ProgramOutput po = server2.startServerAndValidate(false, true, true);

            if (po.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not start server2");
                Log.error(this.getClass(), "dynamicTest", ex);
                throw ex;
            }

            // wait for 2nd server to perform peer recovery
            if (server2.waitForStringInTrace("Performed recovery for " + cloud1RecoveryIdentity) == null) {
                testFailed = true;
                testFailureString = "Second server did not perform peer recovery";
            }

            if (!testFailed && (server2.waitForStringInTrace("Claim peer logs from a peer server") == null)) {
                testFailed = true;
                testFailureString = "Server failed to claim peer logs";
            }
        }

        // flush the resource states
        if (!testFailed) {

            try {
                sb = runTestWithResponse(server2, servletName, "dumpState");
                Log.info(this.getClass(), method, sb.toString());
            } catch (Exception e) {
                Log.error(this.getClass(), method, e);
                throw e;
            }

            //Stop server2
            FATUtils.stopServers(server2);

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
            try {
                sb = runTestWithResponse(peerLockingDisabledServer1, servletName, "checkRec" + id);
            } catch (Exception e) {
                Log.error(this.getClass(), "dynamicTest", e);
                throw e;
            }
            Log.info(this.getClass(), method, "checkRec" + id + " returned: " + sb);

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

            FATUtils.stopServers(peerLockingDisabledServer1);
        }

        tidyServersAfterTest(peerLockingDisabledServer1, server2);

        // XA resource data is cleared in setup servlet methods. Probably should do it here.
        if (testFailed)
            fail(testFailureString);
    }

    /**
     * This test is a repeat of testDynamicCloudRecovery007, except we ensure that both
     * servers are cold started by deleting the Derby file that contains the Transaction
     * Recovery log tables.
     *
     * @throws Exception
     */
    @Test
    public void testColdStartLocalAndPeerServer() throws Exception {

        // Delete existing DB files, so that the tables that support transaction recovery
        // are created from scratch.
        server1.deleteFileFromLibertyInstallRoot("/usr/shared/resources/data/tranlogdb");
        dynamicTest(server1, server2, 7, 2);
    }

    @Override
    public void dynamicTest(LibertyServer server1, LibertyServer server2, int test, int resourceCount) throws Exception {
        String testSuffix = String.format("%03d", test);
        dynamicTest(server1, server2, testSuffix, resourceCount);
    }

    private void dynamicTest(LibertyServer server1, LibertyServer server2, String testSuffix, int resourceCount) throws Exception {
        final String method = "dynamicTest";

        StringBuilder sb = null;

        // Start Server1
        FATUtils.startServers(server1);

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, servletName, "setupRec" + testSuffix);
        } catch (IOException e) {
            // as expected
        }
        Log.info(this.getClass(), method, "setupRec" + testSuffix + " returned: " + sb);

        // wait for 1st server to have gone away
        assertNotNull(server1.getServerName() + " did not crash", server1.waitForStringInTrace(XAResourceImpl.DUMP_STATE));

        // Start Server2
        FATUtils.startServers(server2);

        // wait for 2nd server to perform peer recovery
        assertNotNull(server2.getServerName() + " did not perform peer recovery",
                      server2.waitForStringInTrace("Performed recovery for " + cloud1RecoveryIdentity, FATUtils.LOG_SEARCH_TIMEOUT));

        // flush the resource states
        try {
            sb = runTestWithResponse(server2, servletName, "dumpState");
            Log.info(this.getClass(), method, sb.toString());
        } catch (Exception e) {
            Log.error(this.getClass(), method, e);
            throw e;
        }

        //Stop server2
        FATUtils.stopServers(server2);

        // restart 1st server
        FATUtils.startServers(server1);

        assertNotNull("Recovery incomplete on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0133I", FATUtils.LOG_SEARCH_TIMEOUT));

        // check resource states
        Log.info(this.getClass(), method, "calling checkRec" + testSuffix);

        sb = runTestWithResponse(server1, servletName, "checkRec" + testSuffix);

        Log.info(this.getClass(), method, "checkRec" + testSuffix + " returned: " + sb);

        // Bounce first server to clear log
        FATUtils.stopServers(new String[] { "CWWKN0005W" }, server1);
        FATUtils.startServers(server1);

        // Check log was cleared

        assertNotNull("Transactions left in transaction log on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0135I", FATUtils.LOG_SEARCH_TIMEOUT));
        assertNotNull("XAResources left in partner log on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0134I.*0", FATUtils.LOG_SEARCH_TIMEOUT));

        FATUtils.stopServers(server1);

        tidyServersAfterTest(server1, server2);
        // XA resource data is cleared in setup servlet methods. Probably should do it here.
    }

    @Override
    public void setUp(LibertyServer server) throws Exception {
    }
}
