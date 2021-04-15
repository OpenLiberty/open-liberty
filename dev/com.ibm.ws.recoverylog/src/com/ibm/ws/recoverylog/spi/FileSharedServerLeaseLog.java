/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.util.Utils;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
public class FileSharedServerLeaseLog implements SharedServerLeaseLog {

    // The file system directory where the tx recovery logs are stored. This location is retrieved from the RecoveryLogManager at server startup.
    static String _tranRecoveryLogDirStem;
    static String _localRecoveryIdentity;
    static File _leaseLogDirectory;
    // The file system directory where the lease files will be stored
    static File _serverInstallLeaseLogDir;
    boolean leaseLogWrittenInThisRun;
    static File _controlFile;
    private int _leaseTimeout;
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

    LeaseLock _peerLeaseLock;
    LeaseLock _localLeaseLock;

    // Singleton instance of the FileSystem Lease Log class
    private static final FileSharedServerLeaseLog _fileLeaseLog = new FileSharedServerLeaseLog();

    //to prevent creating another instance of Singleton
    public FileSharedServerLeaseLog() {}

    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(FileSharedServerLeaseLog.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    /**
     * Access the singleton instance of the FileSystem Lease log.
     *
     * @return ChannelFrameworkImpl
     */
    public static FileSharedServerLeaseLog getFileSharedServerLeaseLog(String logDirStem, String localRecoveryIdentity, String recoveryGroup) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getFileSharedServerLeaseLog", new Object[] { logDirStem, localRecoveryIdentity, recoveryGroup });

        if (_serverInstallLeaseLogDir == null)
            setLeaseLog(logDirStem, localRecoveryIdentity, recoveryGroup);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getFileSharedServerLeaseLog", _fileLeaseLog);
        return _fileLeaseLog;
    }

    private static void setLeaseLog(String tranRecoveryLogDirStem, String localRecoveryIdentity, String recoveryGroup) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setLeaseLog", new Object[] { tranRecoveryLogDirStem, localRecoveryIdentity, recoveryGroup });

        // append the recovery group to the directory
        if (recoveryGroup == null)
            recoveryGroup = "defaultGroup";

        try {
            final File leasesDir = new File(tranRecoveryLogDirStem).getParentFile();

            if (leasesDir.getCanonicalPath().equals(new File(System.getenv("WLP_USER_DIR") + File.separator + "servers" + File.separator
                                                             + ConfigurationProviderManager.getConfigurationProvider().getServerName()).getCanonicalPath())
                ||
                (localRecoveryIdentity == null || localRecoveryIdentity.trim().isEmpty())) {
                _serverInstallLeaseLogDir = new File(System.getenv("WLP_USER_DIR") + File.separator + "shared" + File.separator + "leases" + File.separator + recoveryGroup);
            } else {
                _serverInstallLeaseLogDir = new File(leasesDir.getCanonicalPath() + File.separator + "leases" + File.separator
                                                     + recoveryGroup);
            }
        } catch (IOException e) {
            FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.FileSharedServerLeaseLog.setLeaseLog", "139");
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "_serverInstallLeaseLogDir: " + _serverInstallLeaseLogDir.getAbsolutePath());

        // Cache the supplied information
        if (tranRecoveryLogDirStem != null) {
            _tranRecoveryLogDirStem = tranRecoveryLogDirStem;

            if (_leaseLogDirectory == null) {
                _leaseLogDirectory = _serverInstallLeaseLogDir; // logDirectory = _multiScopeRecoveryLog.getLogDirectory()
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Have instatiated directory: " + _leaseLogDirectory.getAbsolutePath());

                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        if (_leaseLogDirectory.exists()) {
                            if (_leaseLogDirectory.isDirectory()) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Lease log directory is in place as expected, instantiate control file");
                                _controlFile = new File(_serverInstallLeaseLogDir + String.valueOf(File.separatorChar) + "control");
                            }
                        } else {
                            // There is no lease log directory or control file. These now need to be created.
                            try {
                                _controlFile = new File(_serverInstallLeaseLogDir + String.valueOf(File.separatorChar) + "control");
                                if (_leaseLogDirectory.mkdirs()) {
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Lease log directory has been created");
                                    if (_controlFile.createNewFile()) {
                                        if (tc.isDebugEnabled())
                                            Tr.debug(tc, "Control has been created: " + _controlFile.getAbsolutePath());
                                    }
                                }
                            } catch (IOException e) {
                                // We're not expecting this to happen. Log the event
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Caught an IOException: " + e);
                            }
                        }
                        return null;
                    }
                });
            }
        }

        _localRecoveryIdentity = localRecoveryIdentity;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setLeaseLog");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#updateServerLease(java.lang.String, java.lang.String, int)
     */
    @Override
    public void updateServerLease(String recoveryIdentity, String recoveryGroup, boolean isServerStartup) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "updateServerLease", new Object[] { recoveryIdentity, recoveryGroup, isServerStartup, this });

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Using recoveryIdentity " + recoveryIdentity + " and log directory " + _tranRecoveryLogDirStem);

        // At this point we already have a lock on the file prior to attempting to write to it.
        // And if we have successfully written to the lease file in this instantiation of the server, then
        // we don't want to rewrite, each time we update the lease.
        try {
            // Only "touch" the server file if it belongs to the local server
            if (recoveryIdentity.equals(_localRecoveryIdentity)) {
                if (_localLeaseLock != null) {
                    // Write the Transaction Log directory string to the file
                    if (!leaseLogWrittenInThisRun) {
                        try {
                            //fooWriter.write(_logDirStem); // was "\n"
                            ByteBuffer byteBuffer = null;
                            FileChannel fChannel = _localLeaseLock.getFileChannel();
                            byteBuffer = ByteBuffer.wrap(_tranRecoveryLogDirStem.getBytes());
                            fChannel.write(byteBuffer);
                            fChannel.force(false);
                            leaseLogWrittenInThisRun = true;
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Have written \"" + byteBuffer.toString() + "\" to lease file");
                        } catch (IOException iox) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Caught I/O exception when trying to write to file");
                        }
                    }

                    // "Touch" the file for this server
                    final File leaseFile = new File(_serverInstallLeaseLogDir + String.valueOf(File.separatorChar) + recoveryIdentity);
                    if (leaseLogWrittenInThisRun) {
                        boolean success = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                            @Override
                            public Boolean run() {
                                return leaseFile.setLastModified(System.currentTimeMillis());
                            }
                        });
                        if (!success) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Unable to set the last modification time for " + leaseFile);
                        }
                    }
                }
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Don't modify time as this is not the local server");
            }
        } catch (OverlappingFileLockException e) {
            // File is already locked in this thread or virtual machine, We're not expecting this to happen. Log the event
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The file aleady appears to be locked in another thread");
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "updateServerLease", this);

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#deleteServerLease(java.lang.String)
     */
    @Override
    public void deleteServerLease(final String recoveryIdentity) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "deleteServerLease", new Object[] { recoveryIdentity, this });

        // Is a lease file (equivalent to a record in the DB table) available for deletion
        final File leaseFile = new File(_serverInstallLeaseLogDir + String.valueOf(File.separatorChar) + recoveryIdentity);

        // At this point we are ready to acquire a lock on the control file prior to attempting to delete the server's file.
        FileLock lock = null;
        FileChannel channel = AccessController.doPrivileged(new PrivilegedAction<FileChannel>() {
            @Override
            public FileChannel run() {
                FileChannel theChannel = null;
                try {
                    theChannel = new RandomAccessFile(_controlFile, "rw").getChannel();
                } catch (FileNotFoundException e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Caught FileNotFound exception when trying to lock control file");
                    theChannel = null;
                }
                return theChannel;
            }
        });

        try {
            // Block until we can acquire the lock on the control file.
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Block until we acquire the lock on the control file");
            lock = channel.lock();

            if (lock != null) {
                // Delete the leaseFile
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        boolean success = false;

                        try {
                            // If we are about to delete a peer lease file, then do a check to be sure that a new instance
                            // of the peer has not "recently" started.
                            boolean attemptDelete = true;
                            if (!recoveryIdentity.equals(_localRecoveryIdentity)) {
                                final long leaseTime = leaseFile.lastModified();

                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "recoveryId: " + recoveryIdentity + ", leaseTime: " + Utils.traceTime(leaseTime));
                                }

                                PeerLeaseData pld = new PeerLeaseData(recoveryIdentity, leaseTime, _leaseTimeout);
                                if (!pld.isExpired()) {
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "The lease file has not expired, do not attempt deletion");
                                    attemptDelete = false;
                                }
                            }

                            // Attempt to delete the lease file
                            if (attemptDelete) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Attempt to delete file " + leaseFile.getName() + ", in dir " + _serverInstallLeaseLogDir);
                                success = leaseFile.delete();
                                if (success) {
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Successfully deleted lease file");
                                } else {
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Failed to delete lease file");
                                }
                            }
                        } catch (SecurityException se) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Caught SecurityException " + se);
                        }
                        return null;
                    }
                });

            }
        } catch (OverlappingFileLockException e) {
            // File is already locked in this thread or virtual machine, We're not expecting this to happen. Log the event
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The control file aleady appears to be locked in another thread");
        } catch (IOException e) {
            // We're not expecting this to happen. Log the event
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Caught an IOException");
        } finally {
            // Release the lock - if it is not null!
            if (lock != null) {
                lock.release();
            }
            // Close the channel
            channel.close();
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
    public boolean claimPeerLeaseForRecovery(String recoveryIdentityToRecover, String myRecoveryIdentity, LeaseInfo leaseInfo) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "claimPeerLeaseForRecovery", new Object[] { recoveryIdentityToRecover, myRecoveryIdentity, this });
        // What we need to do is to extract the log location for peer servers and put them somewhere to be used in TxRecoveryAgentImp.initiateRecovery.
        boolean claimedLease = false;

        FileChannel fChannel = null;
        // At this point we are ready to acquire a lock on the lease file prior to attempting to read it.
        try {
            // If we are about to recover a peer lease file, then do a check to be sure that no other server instance
            // has "recently" recovered it.
            boolean attemptClaim = true;
            // Read the appropriate lease file (equivalent to a record in the DB table)
            final File leaseFile = new File(_serverInstallLeaseLogDir + String.valueOf(File.separatorChar) + recoveryIdentityToRecover);

            // Get the timestamp when the lease file was last touched.
            final long newleaseTime = AccessController.doPrivileged(new PrivilegedAction<Long>() {
                @Override
                public Long run() {
                    return leaseFile.lastModified();
                }
            });

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "recoveryId: " + recoveryIdentityToRecover + ", new leaseTime: " + Utils.traceTime(newleaseTime));
            }

            PeerLeaseData pld = new PeerLeaseData(recoveryIdentityToRecover, newleaseTime, _leaseTimeout);
            if (newleaseTime == 0 || !pld.isExpired()) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "The lease file has not expired, or does not exist do not attempt recovery");
                attemptClaim = false;
            }

            // Attempt to claim the lease file
            if (attemptClaim) {
                if (lockPeerLease(recoveryIdentityToRecover)) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Attempt to read lease file");

                    fChannel = _peerLeaseLock.getFileChannel();
                    if (fChannel != null) {
                        long fileSize = fChannel.size();
                        ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
                        fChannel.read(buffer);

                        buffer.flip();

                        // Read first (and only) line in file
                        String line = new String(buffer.array());

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Lease file contained " + line);

                        // Set the string into the LeaseInfo object
                        leaseInfo.setLeaseDetail(new File(line));
                        claimedLease = true;
                    } else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Lease Lock's channel was null");
                    }

                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Failed to lock or read lease file");
                }
            }
        } catch (IOException e) {
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
    @Override
    public void getLeasesForPeers(final PeerLeaseTable peerLeaseTable, String recoveryGroup) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getLeasesForPeers", new Object[] { peerLeaseTable, recoveryGroup, this });

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {

                // We'll start by trying to lock the control file, if we can't do it this time around, then so be it, we
                // assume that someone else is either getting peer leases or deleting peer leases.
                FileChannel theChannel = null;
                FileLock lock = null;
                try {
                    theChannel = new RandomAccessFile(_controlFile, "rw").getChannel();
                } catch (FileNotFoundException e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Caught FileNotFound exception when trying to lock control file");
                    theChannel = null;
                }

                if (theChannel != null) {
                    // Non blocking attempt to acquire the lock on the control file.
                    try {
                        lock = theChannel.tryLock();
                    } catch (IOException e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Caught IOException when trying to lock control file - " + e);
                        theChannel = null;
                    }

                    if (lock != null) {

                        File thePeerFiles[] = _leaseLogDirectory.listFiles();
                        // Now process through the peers we need to handle
                        if (thePeerFiles != null) {
                            for (File peerFile : thePeerFiles) {
                                if (!peerFile.isDirectory()) {
                                    final String recoveryId = peerFile.getName();
                                    //Skip over the control file
                                    if (!recoveryId.equals(_controlFile.getName())) {
                                        final long leaseTime = peerFile.lastModified();

                                        if (tc.isEventEnabled()) {
                                            Tr.event(tc, "Lease Table: read recoveryId: " + recoveryId);
                                            Tr.event(tc, "Lease Table: read leaseTime: " + Utils.traceTime(leaseTime));
                                        }

                                        PeerLeaseData pld = new PeerLeaseData(recoveryId, leaseTime, _leaseTimeout);

                                        peerLeaseTable.addPeerEntry(pld);
                                    } else {
                                        if (tc.isDebugEnabled())
                                            Tr.debug(tc, "Exclude the control file from the list");
                                    }
                                }
                            }
                        } else {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "No peer servers found");
                        }
                    }
                }

                // Tidy up before we leave
                if (lock != null) {
                    try {
                        lock.release();
                    } catch (IOException e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Caught an IOException on lock release");
                    }
                }
                // Close the channel
                if (theChannel != null) {
                    try {
                        theChannel.close();
                    } catch (IOException e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Caught an IOException on channel close");
                    }
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
    @FFDCIgnore({ OverlappingFileLockException.class })
    public boolean lockPeerLease(String recoveryIdentity) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "lockPeerLease", new Object[] { recoveryIdentity, this });
        // What we need to do is to extract the log location for peer servers and put them somewhere to be used in TxRecoveryAgentImp.initiateRecovery.
        boolean claimedLock = false;
        // Read the appropriate lease file (equivalent to a record in the DB table)
        final File leaseFile = new File(_serverInstallLeaseLogDir + String.valueOf(File.separatorChar) + recoveryIdentity);

        FileLock fLock = null;
        FileChannel fChannel = null;

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
            if (tc.isEntryEnabled())
                Tr.exit(tc, "lockPeerLease", false);
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
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Caught FileNotFound exception when trying to lock lease file");
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
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "We have claimed the lock for file - " + leaseFile);
                    claimedLock = true;
                    _peerLeaseLock = new LeaseLock(recoveryIdentity, fLock, fChannel);
                }
            }
        } catch (OverlappingFileLockException e) {
            // File is already locked in this thread or virtual machine, We're not expecting this to happen. Log the event
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The file aleady appears to be locked in another thread");
        } catch (IOException e) {
            // We're not expecting this to happen. Log the event
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Caught an IOException");
        }

        // Tidy up if we failed to claim lock
        if (!claimedLock) {
            if (fChannel != null)
                try {
                    fChannel.close();
                } catch (IOException e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Caught an IOException on channel close");
                }
            _localLeaseLock = null;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "lockPeerLease", claimedLock);
        return claimedLock;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#releasePeerLease(java.lang.String)
     */
    @Override
    public boolean releasePeerLease(String recoveryIdentity) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "releasePeerLease", new Object[] { recoveryIdentity, this });
        // Release the lock - if it is not null!
        FileLock fLock = null;
        FileChannel fChannel = null;
        if (_peerLeaseLock != null) {
            String recIdentity = _peerLeaseLock.getRecoveryIdentity();
            if (recoveryIdentity.equals(recIdentity)) {
                fLock = _peerLeaseLock.getFileLock();
                if (fLock != null) {
                    fLock.release();
                }
                // Close the channel
                fChannel = _peerLeaseLock.getFileChannel();
                if (fChannel != null)
                    fChannel.close();
                _peerLeaseLock = null;
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "The locks identity which was " + recIdentity + " did not match the requested identity which was " + recoveryIdentity);
            }
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The lease lock was unexpectedly null");
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "releasePeerLease");
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#lockLocalLease(java.lang.String)
     */
    @Override
    public boolean lockLocalLease(String recoveryIdentity) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "lockLocalLease", new Object[] { recoveryIdentity, this });
        // What we need to do is to extract the log location for peer servers and put them somewhere to be used in TxRecoveryAgentImp.initiateRecovery.
        boolean claimedLock = false;
        // Read the appropriate lease file (equivalent to a record in the DB table)
        final File leaseFile = new File(_serverInstallLeaseLogDir + String.valueOf(File.separatorChar) + recoveryIdentity);

        FileLock fLock = null;
        FileChannel fChannel = null;

        // At this point we are ready to acquire a lock on the lease file prior to attempting to read it.
        fChannel = AccessController.doPrivileged(new PrivilegedAction<FileChannel>() {
            @Override
            public FileChannel run() {
                FileChannel theChannel = null;
                try {
                    theChannel = new RandomAccessFile(leaseFile, "rw").getChannel();
                } catch (FileNotFoundException e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Caught FileNotFound exception when trying to lock lease file");
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
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "We have claimed the lock for file - " + leaseFile);
                    claimedLock = true;
                    _localLeaseLock = new LeaseLock(recoveryIdentity, fLock, fChannel);
                }
            }
        } catch (OverlappingFileLockException e) {
            // File is already locked in this thread or virtual machine, We're not expecting this to happen. Log the event
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The file aleady appears to be locked in another thread");
        } catch (IOException e) {
            // We're not expecting this to happen. Log the event
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Caught an IOException");
        }

        // Tidy up if we failed to claim lock
        if (!claimedLock) {
            if (fChannel != null)
                try {
                    fChannel.close();
                } catch (IOException e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Caught an IOException on channel close");
                }
            _localLeaseLock = null;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "lockLocalLease", claimedLock);
        return claimedLock;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#releaseLocalLease(java.lang.String)
     */
    @Override
    public boolean releaseLocalLease(String recoveryIdentity) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "releaseLocalLease", new Object[] { recoveryIdentity, this });
        // Release the lock - if it is not null!
        FileLock fLock = null;
        FileChannel fChannel = null;
        if (_localLeaseLock != null) {
            String recIdentity = _localLeaseLock.getRecoveryIdentity();
            if (recoveryIdentity.equals(recIdentity)) {
                fLock = _localLeaseLock.getFileLock();
                if (fLock != null) {
                    fLock.release();
                }
                // Close the channel
                fChannel = _localLeaseLock.getFileChannel();
                if (fChannel != null)
                    fChannel.close();
                _localLeaseLock = null;
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "The locks identity which was " + recIdentity + " did not match the requested identity which was " + recoveryIdentity);
            }
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The lease lock was unexpectedly null");
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "releaseLocalLease");
        return true;
    }

    private class LeaseLock {
        private final String _recoveryIdentity;
        FileLock _leaseFileLock = null;
        FileChannel _leaseChannel = null;

        // Constructor
        public LeaseLock(String recIdentity, FileLock fLock, FileChannel fChannel) {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "LeaseLock", new Object[] { recIdentity, fLock, fChannel });
            _recoveryIdentity = recIdentity;
            _leaseFileLock = fLock;
            _leaseChannel = fChannel;

            if (tc.isEntryEnabled())
                Tr.exit(tc, "LeaseLock", this);
        }

        public FileLock getFileLock() {
            return _leaseFileLock;
        }

        public FileChannel getFileChannel() {
            return _leaseChannel;
        }

        public String getRecoveryIdentity() {
            return _recoveryIdentity;
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

    /**
     * Signals to the Lease Log that the server is stopping.
     */
    @Override
    public void serverStopping() {
        // No-op in filesystem implementation
    }
}
