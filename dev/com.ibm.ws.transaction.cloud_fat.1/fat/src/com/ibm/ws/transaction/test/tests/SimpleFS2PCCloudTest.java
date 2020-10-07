/*******************************************************************************
 * Copyright (c) 2019,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.web.SimpleFS2PCCloudServlet;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class SimpleFS2PCCloudTest extends FATServletClient {

    private FileLock fLock;
    private FileChannel fChannel;
    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/SimpleFS2PCCloudServlet";
    protected static final int FScloud2ServerPort = 9992;
    private static final long LOG_SEARCH_TIMEOUT = 300000;

    @Server("com.ibm.ws.transaction_FSCLOUD001")
    @TestServlet(servlet = SimpleFS2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server1;

    @Server("com.ibm.ws.transaction_FSCLOUD002")
    @TestServlet(servlet = SimpleFS2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server2;

    @Server("longLeaseLengthFSServer1")
    @TestServlet(servlet = SimpleFS2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer longLeaseLengthFSServer1;

    @BeforeClass
    public static void setUp() throws Exception {

        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server1, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(server2, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(longLeaseLengthFSServer1, APP_NAME, "com.ibm.ws.transaction.*");

        server1.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
        server2.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
        longLeaseLengthFSServer1.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // server1.stopServer("WTRN0075W", "WTRN0076W"); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected
    }

    @After
    public void cleanup() throws Exception {
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
        final String method = "testFSBaseRecovery";
        StringBuilder sb = null;
        String id = "Core";

        // Start Server1
        server1.startServer();

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, SERVLET_NAME, "setupRec" + id);
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);

        server1.waitForStringInLog("Dump State:");

        // Now re-start cloud1
        ProgramOutput po = server1.startServerAndValidate(false, true, true);
        if (po.getReturnCode() != 0) {
            Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
            Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
            Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
            Exception ex = new Exception("Could not restart the server");
            Log.error(this.getClass(), "recoveryTest", ex);
            throw ex;
        }

        // Server appears to have started ok. Check for key string to see whether recovery has succeeded
        server1.waitForStringInTrace("Performed recovery for FScloud001");

        // Lastly stop server1
        server1.stopServer("WTRN0075W", "WTRN0076W"); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected
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

        // Start Server1
        server1.startServer();

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, SERVLET_NAME, "setupRec" + id);
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);

        server1.waitForStringInLog("Dump State:");

        // Now start server2
        server2.setHttpDefaultPort(FScloud2ServerPort);
        ProgramOutput po = server2.startServerAndValidate(false, true, true);

        if (po.getReturnCode() != 0) {
            Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
            Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
            Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
            Exception ex = new Exception("Could not restart the server");
            Log.error(this.getClass(), "recoveryTest", ex);
            throw ex;
        }

        server2.waitForStringInTrace("Performed recovery for FScloud001");
        server2.stopServer();
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
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "java.lang.IllegalStateException" })
    // defect 227411, if FScloud002 starts slowly, then access to FScloud001's indoubt tx
    // XAResources may need to be retried (tx recovery is, in such cases, working as designed.
    public void testFSRecoveryCompeteForLog() throws Exception {
        final String method = "testFSRecoveryCompeteForLog";
        StringBuilder sb = null;
        String id = "Core";

        // Start Server1
        server1.startServer();

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, SERVLET_NAME, "setupRec" + id);
        } catch (Throwable e) {
        }

        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);
        server1.waitForStringInLog("Dump State:");
        // Now is the time to take the filesys lock
        boolean lockSuccess = lockServerLease("FScloud001");
        if (!lockSuccess) {
            Exception ex = new Exception("Could not lock the lease file belonging to FScloud001");
            Log.error(this.getClass(), "recoveryTestCompeteForLock", ex);
            throw ex;
        }

        // Pull in a new server.xml file that ensures that we have a long (5 minute) timeout
        // for the lease, otherwise we may decide that we CAN delete and renew our own lease.

        // Now re-start cloud1
        longLeaseLengthFSServer1.startServerExpectFailure("recovery-log-fail.log", false, true);

        // Server appears to have failed as expected. Check for log failure string
        if (longLeaseLengthFSServer1.waitForStringInLog("RECOVERY_LOG_FAILED") == null) {
            Exception ex = new Exception("Recovery logs should have failed");
            Log.error(this.getClass(), "recoveryTestCompeteForLock", ex);
            throw ex;
        }

        // defect 210055: Now we need to tidy up the environment, start by releasing the lock.
        releaseServerLease("FScloud001");

        // And allow server2 to clear up for its peer.
        server2.setHttpDefaultPort(FScloud2ServerPort);
        ProgramOutput po = server2.startServerAndValidate(false, true, true);

        if (po.getReturnCode() != 0) {
            Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
            Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
            Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
            Exception ex = new Exception("Could not restart the server");
            Log.error(this.getClass(), "recoveryTest", ex);
            throw ex;
        }

        // Server appears to have started ok. Check for 2 key strings to see whether peer recovery has succeeded
        server2.waitForStringInTrace("Performed recovery for FScloud001");
        server2.stopServer();
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
