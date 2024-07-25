/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import tests.CloudFATServletClient;

@RunWith(FATRunner.class)
public class SimpleFS2PCCloudTest extends CloudFATServletClient {

    private FileLock fLock;
    private FileChannel fChannel;
    protected static final int FScloud2ServerPort = 9992;
    private static final String v1Length = "v1Length";
    protected Path leaseFile;

    @Server("FSCLOUD001")
    public static LibertyServer s1;

    @Server("FSCLOUD002")
    public static LibertyServer s2;

    @Server("FSCLOUD001.longleasecompete")
    public static LibertyServer s3;

    @Server("FSCLOUD002.fastcheck")
    public static LibertyServer s4;

    public static String[] serverNames = new String[] {
                                                        "FSCLOUD001",
                                                        "FSCLOUD002",
                                                        "FSCLOUD001.longleasecompete",
                                                        "FSCLOUD002.fastcheck",
    };

    @Override
    protected void checkLogPresence() throws Exception {
        assertTrue(server1.getServerName() + " transaction log has been deleted", server1.fileExistsInLibertyServerRoot("tranlog/tranlog"));
        assertTrue(server1.getServerName() + " partner log has been deleted", server1.fileExistsInLibertyServerRoot("tranlog/partnerlog"));
    }

    @Override
    protected void checkLogAbsence() throws Exception {
        assertFalse(server1.getServerName() + " transaction log has not been deleted", server1.fileExistsInLibertyServerRoot("tranlog/tranlog"));
        assertFalse(server1.getServerName() + " partner log has not been deleted", server1.fileExistsInLibertyServerRoot("tranlog/partnerlog"));
    }

    @BeforeClass
    public static void setUp() throws Exception {
        initialize(s1, s2, "transaction", "/SimpleFS2PCCloudServlet");

        longLeaseCompeteServer1 = s3;
        server2fastcheck = s4;

        final WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "servlets.*");
        final DeployOptions[] dO = new DeployOptions[0];

        ShrinkHelper.exportAppToServer(server1, app, dO);
        ShrinkHelper.exportAppToServer(server2, app, dO);
        ShrinkHelper.exportAppToServer(longLeaseCompeteServer1, app, dO);

        server1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        server2.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        server2.setHttpDefaultPort(server2.getHttpSecondaryPort());
        longLeaseCompeteServer1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
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
        serversToCleanup = new LibertyServer[] { server1 };

        // Start Server1
        FATUtils.startServers(server1);

        FATUtils.recoveryTest(server1, SERVLET_NAME, "Core");
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
        serversToCleanup = new LibertyServer[] { server1, server2 };

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
        checkLogPresence();

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
            checkLogAbsence();
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

        serversToCleanup = new LibertyServer[] { server1, longLeaseCompeteServer1, server2 };

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
        longLeaseCompeteServer1.startServerExpectFailure("recovery-log-fail.log", false, true);

        // Server appears to have failed as expected. Check for log failure string
        assertNotNull("Recovery logs should have failed", longLeaseCompeteServer1.waitForStringInLog("RECOVERY_LOG_FAILED"));

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

        // Server appears to have started ok. Check for 2 key strings to see whether peer recovery has succeeded
        assertNotNull(server2.getServerName() + " did not recover for " + server1.getServerName(),
                      server2.waitForStringInTrace("Performed recovery for " + server1.getServerName(), FATUtils.LOG_SEARCH_TIMEOUT));
    }

    // Check that we can now:
    // 1. Tolerate a v1 log on startup
    // 2. Tolerate a peer with a v1 log
    @Test
    public void testBackwardCompatibility() throws Exception {
        final String method = "testBackwardCompatibility";

        final String defaultBackendURL = "\nhttp://localhost:9080";

        serversToCleanup = new LibertyServer[] { server1 };

        // Edit the lease files
        setupV1LeaseLogs(server1, server2);

        String s1Length = server1.getEnvVar(v1Length);
        String s2Length = server2.getEnvVar(v1Length);

        // Start Server1
        FATUtils.startServers(server1);
        server1.clearLogMarks();
        // Check whether the peer lease has been updated with the owner/backendURL combo.
        assertNotNull("Artificial lease not set up",
                      server1.waitForStringInTraceUsingMark("Originally " + server1.getServerName() + " lease file length " + s1Length,
                                                            FATUtils.LOG_SEARCH_TIMEOUT));
        int newLength = Integer.parseInt(s1Length) + defaultBackendURL.length();
        assertNotNull("Artificial lease not updated",
                      server1.waitForStringInTraceUsingMark("On writing " + server1.getServerName() + " lease file length " + newLength, FATUtils.LOG_SEARCH_TIMEOUT));
        // Check for key string to see whether the home lease has been updated with the owner/backendURL combo.
        assertNotNull("Home lease not set up",
                      server1.waitForStringInTraceUsingMark("On reading " + server2.getServerName() + " lease file length " + s2Length,
                                                            FATUtils.LOG_SEARCH_TIMEOUT));
        newLength = Integer.parseInt(s2Length) + defaultBackendURL.length();
        // Check for key string to see whether the peer lease has been updated with the owner/backendURL combo.
        assertNotNull("Peer lease not updated",
                      server1.waitForStringInTraceUsingMark("On writing " + server2.getServerName() + " lease file length " + newLength, FATUtils.LOG_SEARCH_TIMEOUT));
    }

    private void setupV1LeaseLogs(LibertyServer... servers) throws Exception {
        final String method = "setupV1LeaseLogs";
        for (LibertyServer s : servers) {
            final File groupDir = new File(s.getInstallRoot() +
                                           File.separator + "usr" +
                                           File.separator + "shared" +
                                           File.separator + "leases" +
                                           File.separator + "defaultGroup");

            final File leaseFile = new File(groupDir, s.getServerName()); // Have arranged for recoveryIdentity to be server name

            if (leaseFile.exists()) {
                Log.info(getClass(), "method", (leaseFile.delete() ? "Deleted" : "Failed to delete") + " lease file: " + leaseFile);
            }

            if (!groupDir.exists()) {
                Log.info(getClass(), method, (groupDir.mkdirs() ? "Created" : "Failed to create") + " lease directory: " + groupDir);
            }

            Log.info(getClass(), method, (leaseFile.createNewFile() ? "Created" : "Failed to create") + " lease file: " + leaseFile);

            final String logdir = s.getInstallRoot() +
                                  File.separator + "usr" +
                                  File.separator + "servers" +
                                  File.separator + s.getServerName() +
                                  File.separator + "tranlog";

            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws IOException {
                    try (RandomAccessFile randomAccessFile = new RandomAccessFile(leaseFile, "rw");
                                    FileChannel fileChannel = randomAccessFile.getChannel()) {
                        final ByteBuffer bb = ByteBuffer.wrap(logdir.getBytes());
                        fileChannel.write(bb);
                        s.addEnvVar(v1Length, Integer.toString(logdir.length()));
                        fileChannel.force(false);
                    }
                    return null;
                }
            });
        }
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

    // Remove this method when aggressive takeover works on FS
    @Override
    public void testAggressiveTakeover1() throws Exception {
        Log.info(this.getClass(), "testAggressiveTakeover1", "Aggressive takeover doesn't yet work for FS logs");
    }

    // Remove this method when aggressive takeover works on FS
    @Override
    public void testAggressiveTakeover2() throws Exception {
        Log.info(this.getClass(), "testAggressiveTakeover2", "Aggressive takeover doesn't yet work for FS logs");
    }

    @Override
    protected void setupOrphanLease(LibertyServer server, String path, String serverName) throws Exception {
        final String method = "setupOrphanLease";

        Path leasesDir = Paths.get(server.getInstallRoot(),
                                   "usr",
                                   "shared",
                                   "leases");

        Path leaseDir = Paths.get(leasesDir.toString(), "defaultGroup");
        Files.createDirectories(leaseDir);

        Path controlFile = Paths.get(leaseDir.toString(), "control");
        try {
            Files.createFile(controlFile);
            Log.info(getClass(), method, "Created control file: " + controlFile);
        } catch (FileAlreadyExistsException e) {
        }

        leaseFile = FileSystems.getDefault().getPath(leaseDir.toString(), serverName);
        Files.createFile(leaseFile);
        Log.info(getClass(), method, "Created lease file: " + leaseFile);

        final String leaseContents = server.getInstallRoot() +
                                     File.separator + "usr" +
                                     File.separator + "servers" +
                                     File.separator + serverName +
                                     File.separator + "tranlog" +
                                     "\nhttp://localhost:9080";

        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
            @Override
            public Void run() throws IOException {
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(leaseFile.toFile(), "rw");
                                FileChannel fileChannel = randomAccessFile.getChannel()) {
                    final ByteBuffer bb = ByteBuffer.wrap(leaseContents.getBytes());
                    fileChannel.write(bb);
                    fileChannel.force(false);
                }

                return null;
            }
        });

        // Set modification time to 5 minutes ago
        Files.setLastModifiedTime(leaseFile, FileTime.from(Instant.now().minus(5, ChronoUnit.MINUTES)));
    }

    private void delete(File dir) throws IOException { // For later
        if (dir.exists()) {
            try (Stream<Path> paths = Files.walk(dir.toPath())) {
                paths.sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
            }
        }
    }

    @Override
    protected boolean checkOrphanLeaseExists(LibertyServer server, String path, String serverName) {
        final boolean result = leaseFile.toFile().exists();
        Log.info(getClass(), "checkOrphanLeaseExists", "" + result);
        return result;
    }

    @Override
    protected void setupBatchesOfOrphanLeases(LibertyServer server1, LibertyServer server2, String path) throws Exception {
        Log.info(getClass(), "setupBatchesOfOrphanLeases", "");

        // 20 leases for random servers
        int i;
        for (i = 0; i < 20; i++) {
            setupOrphanLease(server1, null, UUID.randomUUID().toString().replaceAll("\\W", ""));
        }
    }

    @Override
    protected String logsMissingMarker() {
        return "Doing peer recovery but .*tranlog is missing";
    }
}
