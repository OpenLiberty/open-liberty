/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
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
package com.ibm.ws.recoverylog.spi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.time.Instant;
import java.util.stream.Stream;

import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.util.Utils;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@SuppressWarnings("removal")
public class FileSharedServerLeaseLog extends LeaseLogImpl implements SharedServerLeaseLog {

    // The file system directory where the tx recovery logs are stored. This location is retrieved from the RecoveryLogManager at server startup.
    Path _tranRecoveryLogDirStem;
    String _localRecoveryIdentity;
    Path _leaseLogDirectory;
    String _recoveryGroup;
    // The file system directory where the lease files will be stored
    Path _serverInstallLeaseLogDir;
    boolean leaseLogWrittenInThisRun;
    Path _controlFile;
    Path _localLeasePath;
    private Path _localLeaseLockPath;
    private int _leaseTimeout;
    private static final String LOCK_SUFFIX = ".lock";

    // Some design points here
    //
    // -> The FileSharedServerLeaseLog is a singleton class.
    //
    // A "leaselog" directory is used to manage the leases that belong to the servers in a recovery group.
    //
    // A file is used for each server in the recovery group.
    //
    // A "control" file is created and used as an entity on which to lock when doing gross activities in
    // the directory, such as,
    //
    // 1. Getting the list of peers
    // 2. Deleting the lease file that belongs to a server
    //
    // <wlp_usr_dir>/shared/leaselog/recoveryGroupName/
    //                                    |- control
    //                                    |- server01
    //                                    |- server02
    //                                    |- server03
    //                                    etc
    //
    // -> When a server is started a new file is created in the directory for that server, so that each server that is part of the group of peer recovery servers
    // will own a file in the directory. Each file can be termed the "lease file" for a specific server.
    //
    // -> A cold started server will write the location of its recovery logs (a fully qualified file name string) into the lease file. This is the only information
    // that is stored in the file. The information is used by a peer if it is determined that a peer server needs to perform peer recovery.
    //
    // -> Each active server will periodically update the timestamp on its lease file. Additionally each active server will periodically inspect the timestamps on
    // the lease files that belong to the other servers, the peers, in the group. If the timestamp of a lease file is determined to be too old, then the server that
    // owned the lease file is considered to have failed and a peer server may take ownership of the lease file and recover the transaction logs that belong to the
    // failed server.
    //
    // -> A lease file is deleted when the server that owns it shuts down cleanly or when a peer server has recovered the logs that belong to a failed server.
    //
    // -> Note that the lease file mechanism has no bearing on the nature or location of the transaction recovery logs that store information on in-filght transactions
    // managed by the Transaction Manager.
    //
    // -> Furthermore, the process of coordinating access to lease files can be managed through file locks on the lease files to prevent more than one peer server
    // attempting to recover the in-flight transactions that belong to a failed server.
    //
    // 1. We'll always maintain a lock for working against our local server's lease
    // 2. We'll maintain a single lock for working against possibly more than one peer. But we'll only work against one peer at a time.
    //

    private FileLock _peerLeaseLock;
    private FileLock _localLeaseLock;

    private FileChannel _localLeaseLockChannel;

    // Singleton instance of the FileSystem Lease Log class
    private static FileSharedServerLeaseLog _instance;

    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(FileSharedServerLeaseLog.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    /**
     * Access the singleton instance of the FileSystem Lease log.
     *
     * @return ChannelFrameworkImpl
     * @throws IOException
     */
    @FFDCIgnore(IOException.class)
    public static FileSharedServerLeaseLog getFileSharedServerLeaseLog(Path logDirStem, String localRecoveryIdentity, String recoveryGroup) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getFileSharedServerLeaseLog", logDirStem, localRecoveryIdentity, recoveryGroup);

        if (_instance == null) {
            try {
                _instance = new FileSharedServerLeaseLog(logDirStem, localRecoveryIdentity, recoveryGroup);
            } catch (IOException e) {
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getFileSharedServerLeaseLog", _instance);
        return _instance;
    }

    @SuppressWarnings({ "deprecation" })
    private FileSharedServerLeaseLog(Path logDirStem, String localRecoveryIdentity, String recoveryGroup) throws IOException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "FileSharedServerLeaseLog", logDirStem, localRecoveryIdentity, recoveryGroup);

        // append the recovery group to the directory
        if (recoveryGroup == null)
            recoveryGroup = "defaultGroup";
        _recoveryGroup = recoveryGroup;
        _localRecoveryIdentity = localRecoveryIdentity;

        final Path leasesDir = logDirStem.getParent();

        final ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();
        final Path wlpUserDir = Paths.get(cp.getUserDir());
        final Path serverOutputDir = wlpUserDir.resolve(Paths.get("servers", cp.getServerName()));

        if (tc.isDebugEnabled())
            Tr.debug(tc, "leasesDirCanonicalPath: {0}\nserverOutputDir: {1}\nwlpUserDir: {2}", leasesDir, serverOutputDir, wlpUserDir);

        if (leasesDir.equals(serverOutputDir) || (localRecoveryIdentity == null || localRecoveryIdentity.trim().isEmpty())) {
            _serverInstallLeaseLogDir = wlpUserDir.resolve(Paths.get("shared", "leases", recoveryGroup));
        } else {
            _serverInstallLeaseLogDir = leasesDir.resolve(Paths.get("leases", recoveryGroup));
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "_serverInstallLeaseLogDir: " + _serverInstallLeaseLogDir.toString());

        // Cache the supplied information
        _tranRecoveryLogDirStem = logDirStem;

        if (_leaseLogDirectory == null) {
            _leaseLogDirectory = _serverInstallLeaseLogDir; // logDirectory = _multiScopeRecoveryLog.getLogDirectory()

            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    try {
                        if (!Files.isDirectory(_leaseLogDirectory)) {
                            Files.createDirectories(_leaseLogDirectory);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Created: {0}", _leaseLogDirectory);
                        }

                        _controlFile = FileSystems.getDefault().getPath(_serverInstallLeaseLogDir.toString(), "control");

                        if (!Files.exists(_controlFile)) {
                            Files.createFile(_controlFile);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Created: {0}", _controlFile);
                        }
                    } catch (IOException e) {
                        FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.FileSharedServerLeaseLog.setLeaseLog", "191");
                    }
                    return null;
                }
            });
        }

        _localLeasePath = _serverInstallLeaseLogDir.resolve(localRecoveryIdentity);
        _localLeaseLockPath = _serverInstallLeaseLogDir.resolve(localRecoveryIdentity + LOCK_SUFFIX);
        _localLeaseLockChannel = FileChannel.open(_localLeaseLockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ);

        // Take a lock on our own logs
        _localLeaseLock = _localLeaseLockChannel.tryLock();
        if (_localLeaseLock != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Locked local lease {0} {1}", _localLeaseLockPath, _localLeaseLock);
            }
        } else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "FileSharedServerLeaseLog", "Could not lock local lease");
            throw new IOException();
        }

        try (FileChannel localLeaseChannel = FileChannel.open(_localLeasePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ);) {
            ByteBuffer byteBuffer = null;
            if (tc.isDebugEnabled()) {
                byteBuffer = ByteBuffer.allocate((int) localLeaseChannel.size());
                localLeaseChannel.position(0);
                localLeaseChannel.read(byteBuffer);
                byteBuffer.flip();
                String line = new String(byteBuffer.array());
                Tr.debug(tc, "Originally {0} lease file length {1} contains {2}", _localRecoveryIdentity, line.length(), line);
            }
            byteBuffer = ByteBuffer.wrap(_tranRecoveryLogDirStem.toString().getBytes());
            localLeaseChannel.position(0);
            localLeaseChannel.write(byteBuffer);
            byteBuffer = ByteBuffer.wrap(("\n" + getBackendURL()).getBytes());
            localLeaseChannel.write(byteBuffer);
            localLeaseChannel.force(false);
            if (tc.isDebugEnabled()) {
                byteBuffer = ByteBuffer.allocate((int) localLeaseChannel.size());
                localLeaseChannel.position(0);
                localLeaseChannel.read(byteBuffer);
                byteBuffer.flip();
                String line = new String(byteBuffer.array());
                Tr.debug(tc, "On writing {0} lease file length {1} contains {2}", _localRecoveryIdentity, line.length(), line);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "FileSharedServerLeaseLog", new Object[] { _localLeasePath, _localLeaseLockChannel });
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#updateServerLease(java.lang.String, java.lang.String, int)
     */
    @Override
    public void updateServerLease(String recoveryIdentity, String recoveryGroup, boolean isServerStartup) throws IOException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "updateServerLease", recoveryIdentity, recoveryGroup, isServerStartup, _tranRecoveryLogDirStem, this);

        Files.setLastModifiedTime(_localLeasePath, FileTime.from(Instant.now()));

        if (tc.isEntryEnabled())
            Tr.exit(tc, "updateServerLease", _localLeaseLock);
    }

    /*
     * (non-Javadoc)
     *
     * Belts and braces here. Nio works differently on different platforms.
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#deleteServerLease(java.lang.String)
     */
    @Override
    @FFDCIgnore({ FileNotFoundException.class, FileSystemException.class, NoSuchFileException.class, Throwable.class })
    public void deleteServerLease(final String recoveryIdentity, boolean isPeerServer) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "deleteServerLease", this, recoveryIdentity, isPeerServer);

        // Is a lease file (equivalent to a record in the DB table) available for deletion
        final Path leaseFile = _serverInstallLeaseLogDir.resolve(recoveryIdentity);

        // At this point we are ready to acquire a lock on the control file prior to attempting to delete the server's file.
        // Block until we can acquire the lock on the control file.

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Block until we acquire the lock on the control file");
        try (FileChannel theChannel = new RandomAccessFile(_controlFile.toFile(), "rw").getChannel(); FileLock lock = theChannel.lock();) {
            // If we are about to delete a peer lease file, then do a check to be sure that a new instance
            // of the peer has not "recently" started.
            boolean attemptDelete = true;
            if (!recoveryIdentity.equals(_localRecoveryIdentity)) {
                final PeerLeaseData pld = new PeerLeaseData(recoveryIdentity, Files.getLastModifiedTime(leaseFile), _leaseTimeout);
                if (!pld.isExpired()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "The lease file has not expired, do not attempt deletion");
                    attemptDelete = false;
                }
            }

            // Attempt to delete the lease file
            if (attemptDelete) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Prepare to delete file {0}", leaseFile);
                Files.deleteIfExists(leaseFile);
                Files.deleteIfExists(_serverInstallLeaseLogDir.resolve(recoveryIdentity + LOCK_SUFFIX));
            }
        } catch (FileNotFoundException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "{0} is already deleted", _serverInstallLeaseLogDir);
        } catch (NoSuchFileException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "{0} is already deleted", _serverInstallLeaseLogDir);
        } catch (FileSystemException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception deleting lease file: ", e);
        } catch (IOException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception deleting lease file: ", e);
        } finally {
            try (Stream<Path> files = Files.list(_serverInstallLeaseLogDir)) {
                final long fileCount = files.peek(p -> {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Lease directory contents: ", p);
                }).count();

                // If only the control file remains, delete it and its parent recovery group directory
                if (fileCount == 1 && Files.exists(_controlFile)) { // Must be the control file
                    try {
                        Files.delete(_controlFile);
                        Files.delete(_serverInstallLeaseLogDir);
                    } catch (IOException e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Couldn't delete remaining files in {0}: {1}", _serverInstallLeaseLogDir, e);
                    }

                }
            } catch (FileNotFoundException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "{0} is already deleted", _serverInstallLeaseLogDir);
            } catch (NoSuchFileException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "{0} is already deleted", _serverInstallLeaseLogDir);
            } catch (FileSystemException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Error deleting in {0}: {1}", _serverInstallLeaseLogDir, e);
            } catch (IOException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Error deleting in {0}: {1}", _serverInstallLeaseLogDir, e);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "deleteServerLease", this);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#claimPeerLeaseForRecovery(java.lang.String, int)
     */
    @Override
    @FFDCIgnore(PrivilegedActionException.class)
    public boolean claimPeerLeaseForRecovery(String recoveryIdentityToRecover, String myRecoveryIdentity, LeaseInfo leaseInfo) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "claimPeerLeaseForRecovery", recoveryIdentityToRecover, myRecoveryIdentity, this);
        // What we need to do is to extract the log location for peer servers and put them somewhere to be used in TxRecoveryAgentImp.initiateRecovery.
        boolean claimedLease = false;

        // At this point we are ready to acquire a lock on the lease file prior to attempting to read it.
        try {
            // If we are about to recover a peer lease file, then do a check to be sure that no other server instance
            // has "recently" recovered it.
            boolean attemptClaim = true;
            // Read the appropriate lease file (equivalent to a record in the DB table)
            final Path leaseFile = _serverInstallLeaseLogDir.resolve(recoveryIdentityToRecover);

            // Get the timestamp when the lease file was last touched.
            @SuppressWarnings("deprecation")
            final FileTime newleaseTime = AccessController.doPrivileged(new PrivilegedExceptionAction<FileTime>() {
                @Override
                public FileTime run() throws IOException {
                    return Files.getLastModifiedTime(leaseFile);
                }
            });

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "recoveryId: {0}, new leaseTime: {1}", recoveryIdentityToRecover, Utils.traceTime(newleaseTime));
            }

            final PeerLeaseData pld = new PeerLeaseData(recoveryIdentityToRecover, newleaseTime, _leaseTimeout);
            if (!pld.isExpired()) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "The lease file has not expired, or does not exist do not attempt recovery");
                attemptClaim = false;
            }

            // Attempt to claim the lease file
            if (attemptClaim) {
                if (lockPeerLease(recoveryIdentityToRecover)) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Attempt to read lease file");

                    try (FileChannel channel = FileChannel.open(leaseFile, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                        final long fileSize = channel.size();
                        ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
                        channel.read(buffer);

                        buffer.flip();

                        // Read file. Gives us both lines
                        String line = new String(buffer.array());

                        if (tc.isDebugEnabled()) {
                            Tr.info(tc, "On reading " + recoveryIdentityToRecover + " lease file length " + line.length() + " contains " + line);
                        }
                        int newline = line.indexOf("\n");

                        // strip off the backend URL
                        if (newline > 0)
                            line = line.substring(0, newline);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "String is now " + line + " of length " + line.length());
                        // Set the string into the LeaseInfo object
                        leaseInfo.setLeaseDetail(new File(line));

                        // Replace second line with our own backendURL
                        ByteBuffer myBackendURL = null;
                        long filePos = 0;
                        if (newline > 0) {
                            channel.truncate(newline + 1);
                            myBackendURL = ByteBuffer.wrap(getBackendURL().getBytes());
                            filePos = newline + 1;
                        } else {
                            myBackendURL = ByteBuffer.wrap(("\n" + getBackendURL()).getBytes());
                            filePos = fileSize;
                        }
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Write in our own backendURL {0} from file position {1}", myBackendURL, filePos);
                        channel.write(myBackendURL, filePos);
                        channel.force(false);

                        if (tc.isDebugEnabled()) {
                            buffer = ByteBuffer.allocate((int) channel.size());
                            channel.position(0);
                            channel.read(buffer);
                            buffer.flip();
                            line = new String(buffer.array());
                            Tr.info(tc, "On writing " + recoveryIdentityToRecover + " lease file length " + line.length() + " contains " + line);
                        }

                        claimedLease = true;
                    }
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Failed to lock or read lease file");
                }
            }

            // Don't want this to have "unexpired" the lease time
            Files.setLastModifiedTime(leaseFile, newleaseTime);
        } catch (IOException | PrivilegedActionException e) {
            // We're not expecting this to happen. Log the event
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Caught an IOException - " + e);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "claimPeerLeaseForRecovery", claimedLease);
        return claimedLease;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#getLeasesForPeers(com.ibm.ws.recoverylog.spi.PeerLeaseTable, java.lang.String)
     */
    @SuppressWarnings({ "deprecation" })
    @Override
    public void getLeasesForPeers(final PeerLeaseTable peerLeaseTable, String recoveryGroup) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getLeasesForPeers", peerLeaseTable, recoveryGroup, _localLeaseLock, this);

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                // We'll start by trying to lock the control file, if we can't do it this time around, then so be it, we
                // assume that someone else is either getting peer leases or deleting peer leases.
                try (FileChannel theChannel = FileChannel.open(_controlFile, StandardOpenOption.READ, StandardOpenOption.WRITE);
                                FileLock lock = theChannel.tryLock();
                                Stream<Path> files = Files.list(_leaseLogDirectory);) {
                    files.filter(path -> !Files.isDirectory(path)).filter(path -> 0 != path.compareTo(_controlFile)).filter(path -> !path.toString().endsWith(LOCK_SUFFIX)).filter(path -> !path.getFileName().toString().equals(_localRecoveryIdentity)).forEach(peer -> {
                        try {
                            peerLeaseTable.addPeerEntry(new PeerLeaseData(peer.getFileName().toString(), Files.getLastModifiedTime(peer), _leaseTimeout));
                        } catch (IOException e) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Exception getting last modified time for {0}\n{1}", peer, e);
                        }
                    });
                } catch (Exception e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Exception locking lease control file: ", e);
                }

                return null;
            }
        });

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getLeasesForPeers", this);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#lockPeerLease(java.lang.String)
     */
    @Override
    public boolean lockPeerLease(String recoveryIdentity) {

        // Read the appropriate lease file (equivalent to a record in the DB table)
        final Path leaseLockFile = _serverInstallLeaseLogDir.resolve(recoveryIdentity + LOCK_SUFFIX);

        // If the peer lease file does not exist then we can return early. This also prevents us from re-creating the file if it has
        // already been deleted by another peer. We could probably do this all a little more neatly if we didn't have to maintain Java6
        // compatibility but given the need for Java6, the best that we can do is to check for file existence and avoid the possibility
        // of re-creating the file - new RandomAccessFile(..., "rw") WILL create a new file - in merely attempting to acquire a lock (which
        // requires "rw" mode)

        try {
            _peerLeaseLock = FileChannel.open(leaseLockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ).tryLock();
        } catch (IOException e) {
        }

        return _peerLeaseLock != null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#releasePeerLease(java.lang.String)
     */
    @Override
    public void releasePeerLease(String recoveryIdentity) throws Exception {
        if (_peerLeaseLock != null) {
            _peerLeaseLock.acquiredBy().close();
            _peerLeaseLock = null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#setPeerRecoveryLeaseTimeout(int)
     */
    @Override
    public void setPeerRecoveryLeaseTimeout(int leaseTimeout) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setPeerRecoveryLeaseTimeout", leaseTimeout);

        // Store the Lease Timeout
        _leaseTimeout = leaseTimeout;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setPeerRecoveryLeaseTimeout", this);
    }

    @Override
    @FFDCIgnore({ FileNotFoundException.class, IOException.class, Throwable.class })
    public String getBackendURL(String recoveryId) {
        String filename = _serverInstallLeaseLogDir + File.separator + recoveryId;
        String ret = null;
        int retries = 0;

        if (tc.isEntryEnabled())
            Tr.entry(tc, "getBackendURL", filename);

        do {
            // Want the second line out of the file
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                // discard first line
                reader.readLine();
                // return the second
                ret = reader.readLine();

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getBackendURL", ret);
                return ret;
            } catch (FileNotFoundException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "getBackendURL: Lease file not found. Recovery is probably done.");
                break;
            } catch (IOException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "getBackendURL: Lease was probably being renewed. Wait 500ms and try again.", e);
            }

            // Retry for 30s
            if (retries++ > 60) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "getBackendURL: Couldn't access lease file even after retrying.");
                break;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
            }
        } while (true);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getBackendURL", ret);
        return ret;
    }
}