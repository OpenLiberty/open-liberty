/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.web.Simple2PCCloudServlet;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode
@RunWith(FATRunner.class)
public class DualServerPeerLockingTest extends DualServerDynamicTestBase {
    @Server("com.ibm.ws.transaction_LKCLOUD001")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer firstLockingServer;

    @Server("com.ibm.ws.transaction_LKCLOUD002")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer secondLockingServer;

    @BeforeClass
    public static void setUp() throws Exception {
        System.out.println("NYTRACE: DualServerPeerLockingTest.setUp called");
        server1 = firstLockingServer;
        server2 = secondLockingServer;
        servletName = "transaction/Simple2PCCloudServlet";
        cloud1RecoveryIdentity = "cloud001";
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server1, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(server2, APP_NAME, "com.ibm.ws.transaction.*");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // server1.stopServer("WTRN0075W", "WTRN0076W"); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected
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
    @Mode(TestMode.LITE)
    public void testDynamicCloudRecovery007() throws Exception {
        dynamicTest(7, 2);
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
    @Mode(TestMode.LITE)
    public void testDynamicCloudRecovery090() throws Exception {
        dynamicTest(90, 3);
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
    @Mode(TestMode.LITE)
    public void testDefaultAttributesCloudRecovery007() throws Exception {

        // switch to default configuration
        server1.copyFileToLibertyServerRoot("defaultAttributesServer1/server.xml");
        server2.copyFileToLibertyServerRoot("defaultAttributesServer2/server.xml");

        dynamicTest(7, 2);

        // Ensure we have the "original" server.xml at the end of the test.
        server1.copyFileToLibertyServerRoot("originalServer1/server.xml");
        server2.copyFileToLibertyServerRoot("originalServer2/server.xml");
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
    @Mode(TestMode.LITE)
    public void testLocalServerAcquiresLogImmediately() throws Exception {
        int test = 1;
        final String method = "testLocalServerAcquiresLogImmediately";
        final String id = String.format("%03d", test);
        StringBuilder sb = null;
        boolean testFailed = false;
        String testFailureString = "";

        // Start Server1
        server1.startServer();

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
            server1.stopServer(null);
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

        }

        tidyServerAfterTest(server1);
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
    @Mode(TestMode.LITE)
    public void testPeerServerDoesNotAcquireLogs() throws Exception {
        int test = 2;

        final String method = "testPeerServerDoesNotAcquireLogs";
        final String id = String.format("%03d", test);
        StringBuilder sb = null;
        boolean testFailed = false;
        String testFailureString = "";

        // Start Server1
        server1.startServer();

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
            server2.setHttpDefaultPort(Cloud2ServerPort);
            // switch to new configuration
            server2.copyFileToLibertyServerRoot("longPeerStaleTimeServer2/server.xml");
            ProgramOutput po = server2.startServerAndValidate(false, true, true);

            if (po.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not start server2");
                Log.error(this.getClass(), "dynamicTest", ex);
                throw ex;
            }

            // wait for 2nd server to attempt (but fail) to perform peer recovery
            int numStringOccurrences = server2.waitForMultipleStringsInLog(2, "PEER RECOVER server with recovery identity cloud001");
            if (numStringOccurrences < 2) {
                testFailed = true;
                testFailureString = "Second server did not attempt peer recovery at least 2 times, attempted " + numStringOccurrences;
            }
            if (!testFailed && (server2.waitForStringInLog("HADB Peer locking failed for server") == null)) {
                testFailed = true;
                testFailureString = "Server2 did not report that HADB Peer locking had failed";
            }

            //Stop server2
            if (!testFailed) {
                server2.stopServer(null);
            }
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
            server1.stopServer(null);
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

        }

        tidyServerAfterTest(server1);
        tidyServerAfterTest(server2);

        // Ensure we have the "original" server.xml at the end of the test.
        server2.copyFileToLibertyServerRoot("originalServer2/server.xml");

        // XA resource data is cleared in setup servlet methods. Probably should do it here.
        if (testFailed)
            fail(testFailureString);
    }

    /**
     * This test verifies that a Liberty server CANNOT recover its own logs if they have been locked for recovery by a peer
     * server where HADB Locking is enabled and the lock is of long duration.
     *
     * The acquisition of Cloud001's logs by Cloud002 is simulated - in practice Cloud002 will assert ownership of
     * Cloud001's logs and recover them. This test drives a servlet to manually change the ownership of the logs
     * in the control row. The Cloud002 server is started, the servlet is run and Cloud002 stopped.
     *
     * The Cloud001 server is started with a server.xml that includes attributes to enable and configure HADB
     * peer locking such that a lock on the local logs is of long duration. The trace logs are checked to verify that no local recovery
     * has occurred. Cloud001's server.xml is reset and the server is restarted to tidy up.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "com.ibm.ws.recoverylog.spi.RecoveryFailedException", "java.lang.RuntimeException" })
    public void testLocalServerCannotReAcquireLogs() throws Exception {
        int test = 3;

        final String method = "testLocalServerCannotReAcquireLogs";
        final String id = String.format("%03d", test);
        StringBuilder sb = null;
        boolean testFailed = false;
        String testFailureString = "";

        // Start Server2
        server2.startServer();

        // Set the owner of our recovery logs to a peer in the control row through a servlet
        // This simulates a peer's acquisition of our recovery logs.
        try {
            sb = runTestWithResponse(server2, servletName, "setPeerOwnership");

        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "setPeerOwnership" + id + " returned: " + sb);

        server2.stopServer();
        if (!testFailed) {

            // switch to new configuration for 1st server
            server1.copyFileToLibertyServerRoot("longLocalStaleTimeServer1/server.xml");
            // restart 1st server
            //
            // Under the HADB locking scheme, with the parameters set in XXXXX, the server cannot re-aqcuire the logs
            server1.startServerAndValidate(false, true, true);

            // wait for server to attempt (but fail) to perform local recovery
            if (!testFailed && (server1.waitForStringInLog("HADB Peer locking, local recovery failed") == null)) {
                testFailed = true;
                testFailureString = "Server1 did not report that local recovery has failed where HADB Peer locking scheme is enabled";
            }

            //Stop server1
            if (!testFailed) {
                server1.stopServer(null);
            }
        }

        tidyServerAfterTest(server1);

        // Ensure we have the "original" server.xml at the end of the test.
        server1.copyFileToLibertyServerRoot("originalServer1/server.xml");
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
    @Mode(TestMode.LITE)
    public void testSetLatchLocalServer() throws Exception {
        int test = 1;
        final String method = "testSetLatchLocalServer";
        final String id = String.format("%03d", test);
        StringBuilder sb = null;
        boolean testFailed = false;
        String testFailureString = "";

        // switch to configuration with HADB peer locking disabled
        server1.copyFileToLibertyServerRoot("peerLockingDisabledServer1/server.xml");
        // Start Server1
        server1.startServer();

        // Set the latch in the control row through a servlet
        try {
            sb = runTestWithResponse(server1, servletName, "setLatch");

        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "setLatch" + id + " returned: " + sb);

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
            // switch to configuration with HADB peer locking ENABLED
            server1.copyFileToLibertyServerRoot("peerLockingEnabledServer1/server.xml");

            // restart 1st server
            //
            // Under the HADB locking scheme, the server should be able to acquire the logs immediately and proceed
            // with recovery.
            server1.startServerAndValidate(false, true, true);

            if (server1.waitForStringInTrace("WTRN0133I") == null) {
                testFailed = true;
                testFailureString = "Recovery incomplete on first server";
            }

            if (!testFailed && (server1.waitForStringInTrace("Claim the local logs for the local server") == null)) {
                testFailed = true;
                testFailureString = "Server failed to claim logs";
            }

            // check resource states
            if (!testFailed) {
                Log.info(this.getClass(), method, "calling checkRec" + id);
                try {
                    sb = runTestWithResponse(server1, servletName, "checkRec" + id);
                } catch (Exception e) {
                    Log.error(this.getClass(), "dynamicTest", e);
                    throw e;
                }
                Log.info(this.getClass(), method, "checkRec" + id + " returned: " + sb);

                // Bounce first server to clear log
                server1.stopServer(null);
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
            }
        }

        tidyServerAfterTest(server1);

        // Ensure we have the "original" server.xml at the end of the test.
        server1.copyFileToLibertyServerRoot("originalServer1/server.xml");

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
    @Mode(TestMode.LITE)
    public void testSetLatchPeerServer() throws Exception {
        int test = 1;
        final String method = "testSetLatchPeerServer";
        final String id = String.format("%03d", test);
        StringBuilder sb = null;
        boolean testFailed = false;
        String testFailureString = "";

        // switch to configuration with HADB peer locking disabled
        server1.copyFileToLibertyServerRoot("peerLockingDisabledServer1/server.xml");
        // Start Server1
        server1.startServer();

        // Set the latch in the control row through a servlet
        try {
            sb = runTestWithResponse(server1, servletName, "setLatch");

        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "setLatch" + id + " returned: " + sb);

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
            server2.setHttpDefaultPort(Cloud2ServerPort);
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
            server2.stopServer(null);

            // restart 1st server
            server1.startServerAndValidate(false, true, true);

            if (server1.waitForStringInTrace("WTRN0133I") == null) {
                testFailed = true;
                testFailureString = "Recovery incomplete on first server";
            }
        }

        if (!testFailed) {

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
            server1.stopServer(null);
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

        }

        tidyServerAfterTest(server1);
        tidyServerAfterTest(server2);

        // Ensure we have the "original" server.xml at the end of the test.
        server1.copyFileToLibertyServerRoot("originalServer1/server.xml");
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
    @Mode(TestMode.LITE)
    public void testColdStartLocalAndPeerServer() throws Exception {

        // Delete existing DB files, so that the tables that support transaction recovery
        // are created from scratch.
        server1.deleteFileFromLibertyInstallRoot("/usr/shared/resources/data/tranlogdb");
        dynamicTest(7, 2);
    }
}