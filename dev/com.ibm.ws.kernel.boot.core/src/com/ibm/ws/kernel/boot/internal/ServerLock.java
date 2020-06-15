/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.Debug;
import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.ProcessStatus.State;

/**
 *
 */
public class ServerLock {

    /**
     * @param bootProps
     *            {@link BootstrapConfig} containing all established/calculated
     *            values for server name and directories.
     * @return constructed ServerLock
     */
    public static ServerLock createTestLock(BootstrapConfig bootProps) {
        String serverName = bootProps.getProcessName();
        File serverWorkArea = bootProps.getWorkareaFile(null);

        ServerLock serverLock = new ServerLock(serverName, serverWorkArea);
        return serverLock;
    }

    private static final boolean EXISTS = true;
    private static final boolean CAN_WRITE = false;

    private static class FileCheckAction implements PrivilegedExceptionAction<Boolean> {

        private final File file;
        private final boolean existsOrCanWrite;

        FileCheckAction(File file, boolean existsOrCanWrite) {
            this.file = file;
            this.existsOrCanWrite = existsOrCanWrite;
        }

        @Override
        public Boolean run() throws Exception {
            return existsOrCanWrite ? file.exists() : file.canWrite();
        }
    }

    /**
     * Create a server lock and make sure server, workarea, and slock file exist
     * and are writiable.
     *
     * @param bootProps
     *            {@link BootstrapConfig} containing all established/calculated
     *            values for server name and directories.
     * @return constructed ServerLock
     *
     * @throws LaunchException
     *             exception thrown if server directories/files are not writable.
     */
    public static ServerLock createServerLock(BootstrapConfig bootProps) {
        String serverName = bootProps.getProcessName();
        File serverDir = bootProps.getConfigFile(null);
        File serverOutputDir = bootProps.getOutputFile(null);
        File serverWorkArea = bootProps.getWorkareaFile(null);

        ServerLock serverLock = new ServerLock(serverName, serverWorkArea);

        // Don't create serverDir if it doesn't exist
        // (--create will be required to create new server)

        Boolean fileExists = null;
        final File sDir = serverDir;
        try {
            fileExists = AccessController.doPrivileged(new FileCheckAction(sDir, EXISTS));
        } catch (Exception ex) {
        }
        if (fileExists != null && !(fileExists.booleanValue())) {
            return serverLock;
        }

        boolean writable = true;

        final File soDir = serverOutputDir;
        try {
            fileExists = AccessController.doPrivileged(new FileCheckAction(soDir, EXISTS));
        } catch (Exception ex) {
        }

        if (fileExists != null && !(fileExists.booleanValue())) {
            // if (!serverOutputDir.exists())
            writable = serverWorkArea.mkdirs();
        }

        Boolean canWrite = null;
        try {
            canWrite = AccessController.doPrivileged(new FileCheckAction(soDir, CAN_WRITE));
        } catch (Exception ex) {
        }

        if (!writable || ((canWrite != null) && !(canWrite.booleanValue()))) {
            //if (!writable || !serverOutputDir.canWrite()) {
            throw new LaunchException("Write permission required for server output directory, check directory permissions", MessageFormat.format(BootstrapConstants.messages.getString("error.serverDirPermission"),
                                                                                                                                                 serverOutputDir.getAbsolutePath()));
        }

        final File swArea = serverWorkArea;
        try {
            fileExists = AccessController.doPrivileged(new FileCheckAction(swArea, EXISTS));
        } catch (Exception ex) {
        }

        if (fileExists != null && !fileExists.booleanValue())
            //if (!serverWorkArea.exists())
            writable = serverWorkArea.mkdirs();

        try {
            canWrite = AccessController.doPrivileged(new FileCheckAction(swArea, CAN_WRITE));
        } catch (Exception ex) {
        }

        if (!writable || (canWrite != null && !canWrite.booleanValue())) {
            //if (!writable || !serverWorkArea.canWrite()) {
            throw new LaunchException("Can not create server workarea, check directory permissions", MessageFormat.format(BootstrapConstants.messages.getString("error.serverDirPermission"),
                                                                                                                          serverWorkArea.getAbsolutePath()));
        }

        writable = true;

        final File slf = serverLock.lockFile;
        try {
            fileExists = AccessController.doPrivileged(new FileCheckAction(slf, EXISTS));
        } catch (Exception ex) {
        }

        //if (!serverLock.lockFile.exists()) {

        if (fileExists != null && !fileExists.booleanValue()) {

            try {
                writable = serverLock.lockFile.createNewFile();
            } catch (IOException e) {
                writable = false;
            }
        }

        try {
            canWrite = AccessController.doPrivileged(new FileCheckAction(slf, CAN_WRITE));
        } catch (Exception ex) {
        }

        if (!writable || !canWrite) {
            throw new LaunchException("Can not create or write to lock file, check file permissions", MessageFormat.format(BootstrapConstants.messages.getString("error.serverDirPermission"),
                                                                                                                           serverLock.lockFile.getAbsolutePath()));
        }
        return serverLock;
    }

    /** Name of the server: used for trace */
    private final String serverName;

    /**
     * File object to keep track of the lock file on disk
     * used for deleting the lock file when releaseServerLock is called
     */
    private final File lockFile;

    /**
     * A lock for the server: the lock file is created in the server's workarea,
     * and is obtained to prevent the server/workarea from being used by another
     * process.
     * This lock should not be released until/unless the process is exiting.
     */
    private volatile FileLock serverLock = null;

    /**
     * File channel that backs the server lock file: this channel should
     * not be closed until/unless the process is exiting.
     */
    private volatile FileChannel lockFileChannel = null;

    /**
     * Create a ServerLock file that can be used to ensure that only one server
     * is using a server directory/server workarea. This file can be used either
     * to create/obtain the server lock, or to check the status of another process
     * holding the server lock.
     * <P>
     * This handles a single server running per JVM. Trying to run multiple servers
     * or multiple instances of a same server within same JVM is not supported.
     */
    private ServerLock(String serverName, File serverWorkArea) throws LaunchException {
        this.serverName = serverName;
        this.lockFile = new File(serverWorkArea, BootstrapConstants.S_LOCK_FILE);
    }

    /**
     * Obtain the lock that will prevent other servers from starting using this
     * directory/workarea.
     * <P>
     * This handles a single server running per JVM. Trying to run multiple servers
     * or multiple instances of a same server within same JVM is not supported.
     *
     * @throws LaunchException
     *             exception thrown if there's a problem getting the lock or
     *             another instance of this server is running
     */
    public synchronized void obtainServerLock() {

        getServerLock();

        // if the lock is null or is invalid, the server process is already running
        if (serverLock == null || !serverLock.isValid()) {
            serverLock = null;
            lockFileChannel = null;

            String lockFilePath = lockFile.getAbsolutePath();
            LaunchException le = new LaunchException("Server(" + serverName + ") is already running.  lockFile="
                                                     + lockFilePath, MessageFormat.format(BootstrapConstants.messages.getString("error.serverAlreadyRunning"),
                                                                                          serverName, lockFilePath));
            le.setReturnCode(ReturnCode.REDUNDANT_ACTION_STATUS);
            throw le;
        }
    }

    /**
     * Try to obtain server lock
     *
     * @return true if server lock was obtained (!null & valid)
     */
    private synchronized boolean getServerLock() {

        Boolean fileExists = Boolean.FALSE;
        final File sDir = lockFile;
        try {
            fileExists = AccessController.doPrivileged(new FileCheckAction(sDir, EXISTS));
        } catch (Exception ex) {
            // Oh well.
        }
        if (!(fileExists.booleanValue()))
            return false;

        // Change to the "locking" state
        FileOutputStream fos = null;
        FileChannel fc = null;
        try {
            fos = new FileOutputStream(lockFile);
            fc = fos.getChannel();
            lockFileChannel = fc;

            // Try for a short period of time to obtain the lock
            // (if status mechanisms temporarily grab the file, we want to wait
            // to try to grab it.. )
            for (int i = 0; i < BootstrapConstants.MAX_POLL_ATTEMPTS; i++) {
                serverLock = fc.tryLock();
                if (serverLock != null) {
                    break;
                } else {
                    try {
                        Thread.sleep(BootstrapConstants.POLL_INTERVAL_MS);
                    } catch (InterruptedException e) {
                    }
                }
            }
        } catch (OverlappingFileLockException e) {
            // If we encounter an exception obtaining the lock, we can try
            // try closing the the relevant streams (channel first)
            if (!Utils.tryToClose(fc))
                Utils.tryToClose(fos);
        } catch (IOException e) {
            // If we encounter an exception obtaining the lock, we can try
            // try closing the the relevant streams (channel first)
            if (!Utils.tryToClose(fc)) // try to close the wrapping-channel first.
                Utils.tryToClose(fos);
        }

        return serverLock != null && serverLock.isValid();
    }

    /**
     * Try to obtain the server lock file: this immediately releases
     * if it is able to obtain the file.
     *
     * @return true if lock could be obtained
     * @throws IOException if an exception occurred while trying the lock.
     */
    private synchronized boolean tryServerLock() throws IOException {
        if (!lockFile.exists() || lockFileChannel == null)
            return false;

        boolean lockObtained = true;

        try {
            serverLock = lockFileChannel.tryLock();

            // if server lock is not valid, it could not be obtained
            if (serverLock == null || !serverLock.isValid())
                lockObtained = false;
        } catch (OverlappingFileLockException e) {
            lockObtained = false;
        } catch (IOException e) {
            lockObtained = false;
        } finally {
            // make sure we release so the other process can obtain the lock file
            if (serverLock != null)
                serverLock.release();
        }

        return lockObtained;
    }

    /**
     * @return true if lock file exists.
     */
    public boolean lockFileExists() {

        Boolean fileExists = Boolean.FALSE;
        final File sDir = lockFile;
        try {
            fileExists = AccessController.doPrivileged(new FileCheckAction(sDir, EXISTS));
        } catch (Exception ex) {
            // Oh well.
        }
        if (!(fileExists.booleanValue())) {
            return false;
        } else {
            return true;
        }

    }

    /**
     * Release server lock.
     *
     * @throws IOException
     */
    public synchronized void releaseServerLock() {
        if (serverLock != null) {
            try {
                serverLock.release();
            } catch (IOException e) {
            } finally {
                Utils.tryToClose(lockFileChannel);
            }
            serverLock = null;
            lockFileChannel = null;
        }
    }

    /**
     * Wait until the server lock file can be obtained: Used when stopping
     * the server to wait until the server has terminated.
     * <p>
     * This is a separate process using the attach API to stop the server.
     *
     * @return {@link ReturnCode#OK} if server lock file can be obtained within
     *         3 seconds (see {@link #getServerLock()}, will otherwise
     *         return {@link ReturnCode#ERROR_SERVER_STOP}
     */
    public synchronized ReturnCode waitForStop() {

        // if the lock is null or is invalid, the lock could not be obtained within the timeout
        if (!getServerLock()) {
            serverLock = null;
            lockFileChannel = null;
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.stopServerError"),
                                                    serverName, lockFile.getAbsolutePath()));
            return ReturnCode.ERROR_SERVER_STOP;
        }

        releaseServerLock();
        return ReturnCode.OK;
    }

    /**
     * This is counter to obtainLock / waitForLock:
     * Here we want to quickly try to obtain the lock, if it
     * succeeds, we want to release the lock, and try again
     * after waiting a small delay.
     * <p>
     * This is used to test whether or not the service has started
     *
     * @return {@link ReturnCode#OK} if server has started (lockFile unavailable),
     *         or {@link ReturnCode#ERROR_SERVER_START} if server (other process) did not
     *         start/reserve the lockfile within a reasonable time
     */
    public ReturnCode waitForStart(ProcessStatus ps) {
        FileOutputStream fos = null;
        FileChannel fc = null;

        try {
            // Try for a short period of time to obtain the lock
            // (if status mechanisms temporarily grab the file, we want to wait
            // to try to grab it.. )
            int attempts = 0;
            boolean fileObtained = true;

            int waitCycles = this.calculateWaitCyclesFromWaitTime();

            while (fileObtained && attempts++ < waitCycles) {
                try {
                    // wait: allow other process to obtain the lock file
                    Thread.sleep(BootstrapConstants.POLL_INTERVAL_MS);

                    if (lockFile.exists()) {
                        try {
                            if (lockFileChannel == null) {
                                fos = new FileOutputStream(lockFile);
                                fc = fos.getChannel();
                                lockFileChannel = fc;
                            }

                            fileObtained = tryServerLock();
                        } catch (IOException e) {
                            Debug.printStackTrace(e);
                            return ReturnCode.ERROR_SERVER_START;
                        }
                    }
                } catch (InterruptedException ie) {
                }

                // If we obtained the file lock, then the server did not start.
                // If we also detect that the server process is not possibly
                // running, then there is no reason to continue polling.  By
                // checking this condition, we can poll for longer without
                // worrying about "hanging" when the server process fails to
                // launch altogether (invalid JAVA_HOME, JVM options, etc.).
                if (fileObtained && (ps.isPossiblyRunning() == State.NO)) {
                    Debug.println("Server start error: file lock obtained, and server process is not running.");
                    return ReturnCode.ERROR_SERVER_START;
                }
            }

            if (fileObtained) {
                Debug.println("Server start error: file lock obtained, and server process is running.");
                // Server did not start
                return ReturnCode.ERROR_SERVER_START;
            }
        } finally {
            releaseServerLock();
        }
        return ReturnCode.OK;
    }

    /**
     * Calculate the number of times to wait 500ms
     */
    private int calculateWaitCyclesFromWaitTime() {

        String serverWaitTime = System.getProperty(BootstrapConstants.SERVER_START_WAIT_TIME);

        if ((serverWaitTime == null) || serverWaitTime.trim().equals("")) {
            return BootstrapConstants.MAX_POLL_ATTEMPTS;
        }

        int waitTime = 0;
        try {
            waitTime = Integer.parseInt(System.getProperty(BootstrapConstants.SERVER_START_WAIT_TIME));

            if (waitTime < 1) {
                return 1;
            }
        } catch (Throwable t) {

            return BootstrapConstants.MAX_POLL_ATTEMPTS;
        }
        int waitCycles = (int) (TimeUnit.MILLISECONDS.convert(waitTime, TimeUnit.SECONDS) / BootstrapConstants.POLL_INTERVAL_MS);

        return waitCycles;
    }

    /**
     * Test if server is running by attempting to obtain the server
     * lock file.
     *
     * @return true if server is running, false if not.
     */
    public boolean testServerRunning() {
        if (!lockFile.exists()) {
            // Server not started/running: lock file does not exist
            return false;
        }

        FileOutputStream fos = null;
        FileChannel fc = null;
        try {
            fos = new FileOutputStream(lockFile);
            fc = fos.getChannel();
            lockFileChannel = fc;

            if (tryServerLock()) {
                // we could obtain the server lock, server is not running
                return false;
            }
        } catch (IOException e) {
            Debug.printStackTrace(e);
        }
        return true;
    }

    /**
     * Create a marker file when the server is running to determine if the JVM terminated normally.
     * This file is deleted automatically when the JVM exits normally, on a normal server stop.
     * If the marker file already exists when the server is starting, it assumes the JVM abended or
     * the JVM process was forcefully terminated. In this case, we mark the bootstrap properties to
     * do a full clean of the workarea, to remove any possible corruption that might have occurred
     * as a result of the JVM abend.
     *
     * The other existing files such as .sLock and .sCommand weren't not reused for this purpose
     * because they each had ties in to other code and scripts that have expections on them that
     * the server running marker file could not work with (such as slock is always expected to
     * exist)
     *
     * This utlilty relies on the server lock already being obtained (for synchronization) and that
     * a server workspace clean is done after it is executed.
     *
     * This method also expects the server directory and workarea directories exists. The server lock
     * file creation will ensure those things will exist.
     */
    public static void createServerRunningMarkerFile(BootstrapConfig bootConfig) {
        File serverWorkArea = bootConfig.getWorkareaFile(null);
        File serverRunningMarkerFile = null;
        try {
            serverRunningMarkerFile = new File(serverWorkArea, BootstrapConstants.SERVER_RUNNING_FILE);
            serverRunningMarkerFile.deleteOnExit();
            boolean newFile = serverRunningMarkerFile.createNewFile();
            if (!newFile)
                bootConfig.forceCleanStart();
        } catch (IOException e) {
            throw new LaunchException("Can not create or write to server running marker file, check file permissions", MessageFormat.format(BootstrapConstants.messages.getString("error.serverDirPermission"),
                                                                                                                                            serverRunningMarkerFile.getAbsolutePath()), e);
        }
    }
}
