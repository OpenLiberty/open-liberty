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
package suite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.TxShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class SimpleFS2PCCloudTest extends FATServletClient {

    private FileLock fLock;
    private FileChannel fChannel;
    private static final int LOG_SEARCH_TIMEOUT = 300000;
    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/SimpleFS2PCCloudServlet";
    private static final String APP_PATH = "../com.ibm.ws.transaction.cloud_fat.base/";
    protected static final int FScloud2ServerPort = 9992;
    protected static final int FScloud2ServerPortA = 9993;
    protected static final int FScloud2ServerPortB = 9994;

    @Server("FSCLOUD001")
    public static LibertyServer server1;

    @Server("FSCLOUD002")
    public static LibertyServer server2;

    @Server("longLeaseLengthFSServer1")
    public static LibertyServer longLeaseLengthFSServer1;

    @Server("longLeaseLengthFSFail")
    public static LibertyServer longLeaseLengthFSFail;

    @Server("FSCLOUD002.lease10")
    public static LibertyServer server2lease10;

    @Server("longLeaseLengthFSFailA")
    public static LibertyServer longLeaseLengthFSFailA;

    @Server("longLeaseLengthFSFailB")
    public static LibertyServer longLeaseLengthFSFailB;

    public static String[] serverNames = new String[] {
                                                        "FSCLOUD001",
                                                        "FSCLOUD002",
                                                        "longLeaseLengthFSServer1",
                                                        "longLeaseLengthFSFail",
                                                        "FSCLOUD002.lease10",
                                                        "longLeaseLengthFSFailA",
                                                        "longLeaseLengthFSFailB",
    };
    private LibertyServer[] serversToCleanup;
    private static final String[] toleratedMsgs = new String[] { ".*" };

    @BeforeClass
    public static void setUp() throws Exception {

        TxShrinkHelper.buildDefaultApp(server1, APP_NAME, APP_PATH, "servlets.*");
        TxShrinkHelper.buildDefaultApp(server2, APP_NAME, APP_PATH, "servlets.*");
        TxShrinkHelper.buildDefaultApp(longLeaseLengthFSServer1, APP_NAME, APP_PATH, "servlets.*");
        TxShrinkHelper.buildDefaultApp(longLeaseLengthFSFail, APP_NAME, APP_PATH, "servlets.*");
        TxShrinkHelper.buildDefaultApp(server2lease10, APP_NAME, APP_PATH, "servlets.*");
        TxShrinkHelper.buildDefaultApp(longLeaseLengthFSFailA, APP_NAME, APP_PATH, "servlets.*");
        TxShrinkHelper.buildDefaultApp(longLeaseLengthFSFailB, APP_NAME, APP_PATH, "servlets.*");

        server1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        server2.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        server2.setHttpDefaultPort(server2.getHttpSecondaryPort());
        longLeaseLengthFSServer1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        longLeaseLengthFSFail.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        server2lease10.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        longLeaseLengthFSFailA.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        longLeaseLengthFSFailB.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
    }

    @After
    public void cleanup() throws Exception {
        // If any servers have been added to the serversToCleanup array, we'll stop them now
        // test is long gone so we don't care about messages & warnings anymore
        if (serversToCleanup != null && serversToCleanup.length > 0) {
            FATUtils.stopServers(toleratedMsgs, serversToCleanup);
            serversToCleanup = null;
        }
        // Clean up XA resource file
        server1.deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);
    }

    /**
     * The purpose of this test is as a control to verify that single server recovery is working.
     *
     * The FSCloud001 server is started and halted by a servlet that leaves an indoubt transaction.
     * FSCloud001 is restarted and transaction recovery verified.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testFSBaseRecovery() throws Exception {
        // Start Server1
        FATUtils.startServers(server1);

        try {
            FATUtils.recoveryTest(server1, SERVLET_NAME, "Core");
        } finally {
            // Lastly stop server1
            FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W" }, server1); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected
        }
    }

    /**
     * The purpose of this test is to verify simple peer transaction recovery.
     *
     * The FSCloud001 server is started and halted by a servlet that leaves an indoubt transaction.
     * FSCloud002, a peer server as it belongs to the same recovery group is started and recovery the
     * transaction that belongs to FSCloud001.
     *
     * @throws Exception
     */
    @Test
    public void testFSRecoveryTakeover() throws Exception {
        final String method = "testFSRecoveryTakeover";
        StringBuilder sb = null;
        String id = "Core";

        final String tranlog = "tranlog/tranlog";
        final String partnerlog = "tranlog/partnerlog";

        // Start Server1
        FATUtils.startServers(server1);

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, SERVLET_NAME, "setupRec" + id);
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);

        assertNotNull(server1.getServerName() + " did not crash properly", server1.waitForStringInLog(XAResourceImpl.DUMP_STATE));

        // At this point server1's recovery log files should (absolutely!) be present
        assertTrue(server1.getServerName() + " has no transaction log", server1.fileExistsInLibertyServerRoot(tranlog));
        assertTrue(server1.getServerName() + " has no partner log", server1.fileExistsInLibertyServerRoot(partnerlog));

        // Now start server2
        server2.setHttpDefaultPort(FScloud2ServerPort);
        ProgramOutput po = server2.startServerAndValidate(false, true, true);
        if (po.getReturnCode() != 0) {
            Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
            Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
            Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
            fail("Could not restart " + server2.getServerName());
        }

        try {
            assertNotNull(server2.getServerName() + " did not recover for " + server1.getServerName(),
                          server2.waitForStringInTrace("Performed recovery for " + server1.getServerName(), FATUtils.LOG_SEARCH_TIMEOUT));

            // Check to see that the peer recovery log files have been deleted
            assertFalse(server1.getServerName() + " transaction log has not been deleted", server1.fileExistsInLibertyServerRoot(tranlog));
            assertFalse(server1.getServerName() + " partner log has not been deleted", server1.fileExistsInLibertyServerRoot(partnerlog));
        } finally {
            FATUtils.stopServers(server2);
        }
    }

    /**
     * The purpose of this test is to verify correct behaviour when peer servers compete for a log.
     *
     * The FSCloud001 server is started and halted by a servlet that leaves an indoubt transaction.
     *
     * A lock is then taken on FSCloud001's lease file. This simulates the situation where a peer server
     * has acquired the lock and is recovering the logs.
     * FSCloud001 is restarted but should fail to acquire the lease to its recovery logs.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.ws.recoverylog.spi.RecoveryFailedException" })
    // defect 227411, if FScloud002 starts slowly, then access to FScloud001's indoubt tx
    // XAResources may need to be retried (tx recovery is, in such cases, working as designed.
    public void testFSRecoveryCompeteForLog() throws Exception {
        final String method = "testFSRecoveryCompeteForLog";
        StringBuilder sb = null;
        String id = "Core";

        // Start Server1
        FATUtils.startServers(server1);

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, SERVLET_NAME, "setupRec" + id);
        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);
        assertNotNull(server1.getServerName() + " did not crash properly", server1.waitForStringInLog(XAResourceImpl.DUMP_STATE));

        // Now is the time to take the filesys lock
        assertTrue("Could not lock the lease file belonging to FSCLOUD001", lockServerLease("FSCLOUD001"));

        // Pull in a new server.xml file that ensures that we have a long (5 minute) timeout
        // for the lease, otherwise we may decide that we CAN delete and renew our own lease.

        // Now re-start cloud1
        longLeaseLengthFSServer1.startServerExpectFailure("recovery-log-fail.log", false, true);

        // Server appears to have failed as expected. Check for log failure string
        assertNotNull("Recovery logs should have failed", longLeaseLengthFSServer1.waitForStringInLog("RECOVERY_LOG_FAILED"));

        // defect 210055: Now we need to tidy up the environment, start by releasing the lock.
        releaseServerLease("FSCLOUD001");

        // And allow server2 to clear up for its peer.
        server2.setHttpDefaultPort(FScloud2ServerPort);
        ProgramOutput po = server2.startServerAndValidate(false, true, true);
        if (po.getReturnCode() != 0) {
            Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
            Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
            Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
            fail("Could not restart " + server2.getServerName());
        }

        try {
            // Server appears to have started ok. Check for 2 key strings to see whether peer recovery has succeeded
            assertNotNull(server2.getServerName() + " did not recover for " + server1.getServerName(),
                          server2.waitForStringInTrace("Performed recovery for " + server1.getServerName(), FATUtils.LOG_SEARCH_TIMEOUT));
        } finally {
            FATUtils.stopServers(server2);
        }
    }

    @Test
    @AllowedFFDC(value = { "java.lang.IllegalStateException" })
    public void testPeerTakeoverFailure() throws Exception {
        final String method = "testPeerTakeoverFailure";

        serversToCleanup = new LibertyServer[] { longLeaseLengthFSFail, server2lease10 };

        longLeaseLengthFSFail.setFFDCChecking(false);
        server2lease10.setHttpDefaultPort(FScloud2ServerPort);
        try {
            FATUtils.startServers(longLeaseLengthFSFail);
        } catch (Exception e) {
            Log.error(this.getClass(), method, e);
            // If we're here, the test will fail but we need to make sure the server has stopped so the next test has a chance
            FATUtils.stopServers(longLeaseLengthFSFail);
            throw e;
        }

        try {
            Thread.sleep(1000 * 20);
        } catch (Exception ex) {
            Log.info(this.getClass(), method, "sleep ended early");
        }

        try {
            FATUtils.startServers(server2lease10);
        } catch (Exception e) {
            Log.error(this.getClass(), method, e);
            // If we're here, the test will fail but we need to make sure the server has stopped so the next test has a chance
            FATUtils.stopServers(server2lease10);
            throw e;
        }

        // server2 does not know that server1 has a much longer leaseTimeout configured so it will prematurely
        // (from server1's point of view) attempt to acquire server1's log. In the filesystem case, the takeover will
        // fail because server1 still holds its coordination lock.

        //  Check for key string to see whether peer recovery has failed
        assertNotNull("peer recovery unexpectedly succeeded",
                      server2lease10.waitForStringInTrace("Peer Recovery failed for server with recovery identity longLeaseLengthFSFail", LOG_SEARCH_TIMEOUT));

        FATUtils.stopServers(longLeaseLengthFSFail, server2lease10);

        Log.info(this.getClass(), method, "test complete");
    }

    @Test
    @AllowedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMultiPeerTakeoverFailure() throws Exception {
        final String method = "testMultiPeerTakeoverFailure";

        serversToCleanup = new LibertyServer[] { longLeaseLengthFSFail, server2lease10, longLeaseLengthFSFailA, longLeaseLengthFSFailB };

        longLeaseLengthFSFail.setFFDCChecking(false);
        server2lease10.setHttpDefaultPort(FScloud2ServerPort);
        longLeaseLengthFSFailA.setHttpDefaultPort(FScloud2ServerPortA);
        longLeaseLengthFSFailB.setHttpDefaultPort(FScloud2ServerPortB);
        try {
            FATUtils.startServers(longLeaseLengthFSFail, longLeaseLengthFSFailA, longLeaseLengthFSFailB);
        } catch (Exception e) {
            Log.error(this.getClass(), method, e);
            // If we're here, the test will fail but we need to make sure the server has stopped so the next test has a chance
            FATUtils.stopServers(longLeaseLengthFSFail, longLeaseLengthFSFailA, longLeaseLengthFSFailB);
            throw e;
        }

        try {
            Thread.sleep(1000 * 20);
        } catch (Exception ex) {
            Log.info(this.getClass(), method, "sleep ended early");
        }

        try {
            FATUtils.startServers(server2lease10);
        } catch (Exception e) {
            Log.error(this.getClass(), method, e);
            // If we're here, the test will fail but we need to make sure the server has stopped so the next test has a chance
            FATUtils.stopServers(server2lease10);
            throw e;
        }
        // server2 does not know that server1 has a much longer leaseTimeout configured so it will prematurely
        // (from server1's point of view) attempt to acquire server1's log. In the filesystem case, the takeover will
        // fail because server1 still holds its coordination lock.

        //  Check for key string to see whether peer recovery has failed
        assertNotNull("First peer recovery unexpectedly succeeded",
                      server2lease10.waitForStringInTrace("Peer Recovery failed for server with recovery identity longLeaseLengthFSFail", LOG_SEARCH_TIMEOUT));

        FATUtils.stopServers(longLeaseLengthFSFail, longLeaseLengthFSFailA, longLeaseLengthFSFailB, server2lease10);

        Log.info(this.getClass(), method, "test complete");
    }

    @Test
    @AllowedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMultiPeerTakeover() throws Exception {
        final String method = "testMultiPeerTakeover";
        StringBuilder sb = null;
        String id = "Core";
        serversToCleanup = new LibertyServer[] { longLeaseLengthFSFail, server2lease10, longLeaseLengthFSFailA, longLeaseLengthFSFailB };

        longLeaseLengthFSFail.setFFDCChecking(false);
        server2lease10.setHttpDefaultPort(FScloud2ServerPort);
        longLeaseLengthFSFailA.setHttpDefaultPort(FScloud2ServerPortA);
        longLeaseLengthFSFailB.setHttpDefaultPort(FScloud2ServerPortB);
        try {
            FATUtils.startServers(longLeaseLengthFSFail, longLeaseLengthFSFailA, longLeaseLengthFSFailB);
        } catch (Exception e) {
            Log.error(this.getClass(), method, e);
            // If we're here, the test will fail but we need to make sure the server has stopped so the next test has a chance
            FATUtils.stopServers(longLeaseLengthFSFail, longLeaseLengthFSFailA, longLeaseLengthFSFailB);
            throw e;
        }

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(longLeaseLengthFSFail, SERVLET_NAME, "setupRec" + id);
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), method, "longLeaseLengthFSFail: setupRec for " + id + " returned: " + sb);

        assertNotNull(longLeaseLengthFSFail.getServerName() + " did not crash properly", longLeaseLengthFSFail.waitForStringInLog(XAResourceImpl.DUMP_STATE));

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(longLeaseLengthFSFailA, SERVLET_NAME, "setupRec" + id);
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), method, "longLeaseLengthFSFailA: setupRec for " + id + " returned: " + sb);

        assertNotNull(longLeaseLengthFSFailA.getServerName() + " did not crash properly", longLeaseLengthFSFailA.waitForStringInLog(XAResourceImpl.DUMP_STATE));

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(longLeaseLengthFSFailB, SERVLET_NAME, "setupRec" + id);
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), method, "longLeaseLengthFSFailB: setupRec for " + id + " returned: " + sb);

        assertNotNull(longLeaseLengthFSFailB.getServerName() + " did not crash properly", longLeaseLengthFSFailB.waitForStringInLog(XAResourceImpl.DUMP_STATE));

        try {
            Thread.sleep(1000 * 20);
        } catch (Exception ex) {
            Log.info(this.getClass(), method, "sleep ended early");
        }

        try {
            FATUtils.startServers(server2lease10);
        } catch (Exception e) {
            Log.error(this.getClass(), method, e);
            // If we're here, the test will fail but we need to make sure the server has stopped so the next test has a chance
            FATUtils.stopServers(server2lease10);
            throw e;
        }

        //  Check for key strings to see whether peer recovery has succeded
        assertNotNull(server2lease10.getServerName() + " did not recover for " + longLeaseLengthFSFail.getServerName(),
                      server2lease10.waitForStringInTrace("Performed recovery for " + longLeaseLengthFSFail.getServerName(), FATUtils.LOG_SEARCH_TIMEOUT));
        assertNotNull(server2lease10.getServerName() + " did not recover for " + longLeaseLengthFSFailA.getServerName(),
                      server2lease10.waitForStringInTrace("Performed recovery for " + longLeaseLengthFSFailA.getServerName(), FATUtils.LOG_SEARCH_TIMEOUT));
        assertNotNull(server2lease10.getServerName() + " did not recover for " + longLeaseLengthFSFailB.getServerName(),
                      server2lease10.waitForStringInTrace("Performed recovery for " + longLeaseLengthFSFailB.getServerName(), FATUtils.LOG_SEARCH_TIMEOUT));

        FATUtils.stopServers(longLeaseLengthFSFail, longLeaseLengthFSFailA, longLeaseLengthFSFailB, server2lease10);

        Log.info(this.getClass(), method, "test complete");
    }

    private boolean lockServerLease(String recoveryId) throws Exception {
        final String method = "lockServerLease";
        boolean claimedLock = false;
        Log.info(this.getClass(), method, "lock for " + recoveryId);
        // Read the appropriate lease file (equivalent to a record in the DB table)
        String installRoot = server1.getInstallRoot();

        Log.info(this.getClass(), method, "install root is: " + installRoot);
        String leaseFileString = installRoot + File.separator + "usr" +
                                 File.separator + "shared" +
                                 File.separator + "leases" +
                                 File.separator + "defaultGroup" +
                                 File.separator + recoveryId;

        Log.info(this.getClass(), method, "lease file to lock is: " + leaseFileString);
        final File leaseFile = new File(leaseFileString);

        // If the peer lease file does not exist then we can return early. This also prevents us from re-creating the file if it has
        // already been deleted by another peer. We could probably do this all a little more neatly if we didn't have to maintain Java6
        // compatibility but given the need for Java6, the best that we can do is to check for file existence and avoid the possibility
        // of re-creating the file - new RandomAccessFile(..., "rw") WILL create a new file - in merely attempting to acquire a lock (which
        // requires "rw" mode)
        boolean success = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                boolean fileExists = true;
                if (leaseFile == null || !leaseFile.exists()) {
                    fileExists = false;
                }
                return fileExists;
            }
        });
        if (!success) {
            return false;
        }

        // At this point we are ready to acquire a lock on the lease file prior to attempting to read it.
        fChannel = AccessController.doPrivileged(new PrivilegedAction<FileChannel>() {
            @Override
            public FileChannel run() {
                FileChannel theChannel = null;
                try {
                    // Open for read-write, in order to use the locking scheme
                    theChannel = new RandomAccessFile(leaseFile, "rw").getChannel();
                } catch (FileNotFoundException e) {
                    Log.info(this.getClass(), method, "Caught FileNotFound exception when trying to lock lease file");
                    theChannel = null;
                }
                return theChannel;
            }
        });

        try {
            // Try acquiring the lock without blocking. This method returns
            // null or throws an exception if the file is already locked.
            if (fChannel != null) {
                fLock = fChannel.tryLock();

                if (fLock != null) {
                    Log.info(this.getClass(), method, "We have claimed the lock for file - " + leaseFile);
                    claimedLock = true;
                }
            }
        } catch (OverlappingFileLockException e) {
            // File is already locked in this thread or virtual machine, We're not expecting this to happen. Log the event
            Log.info(this.getClass(), method, "The file aleady appears to be locked in another thread");
        } catch (IOException e) {
            // We're not expecting this to happen. Log the event
            Log.info(this.getClass(), method, "Caught an IOException");
        }

        // Tidy up if we failed to claim lock
        if (!claimedLock) {
            if (fChannel != null)
                try {
                    fChannel.close();
                } catch (IOException e) {
                    Log.info(this.getClass(), method, "Caught an IOException on channel close");
                }
        }

        Log.info(this.getClass(), method, "lockServerLease processing complete - claimed lock " + claimedLock);
        return claimedLock;
    }

    public boolean releaseServerLease(String recoveryIdentity) throws Exception {
        final String method = "releaseServerLease";
        Log.info(this.getClass(), method, "release for " + recoveryIdentity);

        // Release the lock - if it is not null!
        if (fLock != null) {
            Log.info(this.getClass(), method, "release lock");
            fLock.release();
        }

        // Close the channel
        if (fChannel != null) {
            Log.info(this.getClass(), method, "close channel");
            fChannel.close();
        }

        Log.info(this.getClass(), method, "releaseServerLease processing complete");
        return true;
    }
}
