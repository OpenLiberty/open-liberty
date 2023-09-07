/*******************************************************************************
 * Copyright (c) 1997, 2022 IBM Corporation and others.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

//------------------------------------------------------------------------------
// Class: RLSUtils
//------------------------------------------------------------------------------
/**
 * Common utility functions for the Recovery Log Service
 */
public class RLSUtils {
    private static final TraceComponent tc = Tr.register(RLSUtils.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    public interface Operation {
        public boolean act() throws Exception;
    }

    /**
     * The maximum wait time to retry an operation
     *
     * Note: This does not handle when the server is prevented
     * from running. In a typical case, where application files
     * are updated after accessing the server files, if the server
     * is prevented from running while an application file has a
     * pending close, the usual pend time may be exceeded without
     * the file being closed.
     */
    public static final long STANDARD_RETRY_MAX_INTERVAL_NS = TimeUnit.SECONDS.toNanos(10);

    /**
     * The amount of time to wait before retrying
     */
    public static final long STANDARD_RETRY_INTERVAL_NS = TimeUnit.MILLISECONDS.toNanos(100); // 0.1s

    /**
     * Lookup string that allows character digit lookup by index value.
     * ie _digits[9] == '9' etc.
     */
    private final static String _digits = "0123456789abcdef";

    /**
     * This field is intended to be used by callers of toHexString to limit the
     * maximum number of bytes that will be output from trace points. If RAS
     * trace points try and output large amounts of information, OutOfMemory
     * failures can occur.
     */
    public static final int MAX_DISPLAY_BYTES = 32;

    /**
     * The size, in bytes, of the data used to persist a boolean value
     */
    protected static final int BOOLEAN_SIZE = 1;
    /**
     * The size, in bytes, of the data used to persist a short value
     */
    protected static final int SHORT_SIZE = 2;
    /**
     * The size, in bytes, of the data used to persist a int value
     */
    protected static final int INT_SIZE = 4;
    /**
     * The size, in bytes, of the data used to persist a long value
     */
    protected static final int LONG_SIZE = 8;

    /**
     * The string delimeter for UNC file name
     */
    protected static final String UNC_HEADER = new String(File.separator + File.separator);

    /**
     * It is not safe to access different instances of a File object to test for and
     * then create a directory hierarchy from different threads. This can occur when
     * the user is issuing multiple concurrent openLog calls. In order to make this
     * safe, we must synchronize on a static object when we prepare the directory
     * structure for a recovery log in the openLog method.
     */
    private static Object _directoryCreationLock = new Object();

    //------------------------------------------------------------------------------
    // Method: Utils.toHexString
    //------------------------------------------------------------------------------
    /**
     * Converts a byte array into a printable hex string.
     *
     * @param byteSource The byte array source.
     *
     * @return String printable hex string or "null"
     */
    @Trivial
    public static String toHexString(byte[] byteSource) {
        if (byteSource == null) {
            return "null";
        } else {
            return toHexString(byteSource, byteSource.length);
        }
    }

    //------------------------------------------------------------------------------
    // Method: Utils.toHexString
    //------------------------------------------------------------------------------
    /**
     * Converts a byte array into a printable hex string.
     *
     * @param byteSource The byte array source.
     * @param bytes      The number of bytes to display.
     *
     * @return String printable hex string or "null"
     */
    @Trivial
    public static String toHexString(byte[] byteSource, int bytes) {
        StringBuffer result = null;
        boolean truncated = false;

        if (byteSource != null) {
            if (bytes > byteSource.length) {
                // If the number of bytes to display is larger than the available number of
                // bytes, then reset the number of bytes to display to be the available
                // number of bytes.
                bytes = byteSource.length;
            } else if (bytes < byteSource.length) {
                // If we are displaying less bytes than are available then detect this
                // 'truncation' condition.
                truncated = true;
            }

            result = new StringBuffer(bytes * 2);
            for (int i = 0; i < bytes; i++) {
                result.append(_digits.charAt((byteSource[i] >> 4) & 0xf));
                result.append(_digits.charAt(byteSource[i] & 0xf));
            }

            if (truncated) {
                result.append("... (" + bytes + "/" + byteSource.length + ")");
            } else {
                result.append("(" + bytes + ")");
            }
        } else {
            result = new StringBuffer("null");
        }

        return (result.toString());
    }

    public static String FQHAMCompatibleServerName(String cell, String node, String server) {
        return cell + "\\" + node + "\\" + server;
    }

    public static boolean createDirectoryTree(String requiredDirectoryTree) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createDirectoryTree", requiredDirectoryTree);

        boolean exists = true;

        // Check to see if the required log directory already exists. If not then create
        // it. This must be serialized as this type of File access is not thread safe. Note
        // also that this is created a single directory at a time. This may seem odd, but it
        // provides protection against two servers trying to create a common directory path
        // at a sime time. One will create the directory, the other will detect that its create
        // failed but the directory now exists and continue on. Only if an attempt to create the
        // directory fails and the directory still does not exist will this be reported as an
        // exception.
        //
        // Code added by PK35957 to handle UNC file specification
        // For example //server/share/directory path
        synchronized (_directoryCreationLock) {
            File target = new File(requiredDirectoryTree);

            // Only proceed if the directory does not exist.
            if (!target.exists()) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Creating directory tree", requiredDirectoryTree);

                Stack<File> pathStack = new Stack<File>();

                while (target != null) {
                    pathStack.push(target);
                    target = target.getParentFile();
                }

                while (!pathStack.empty() && exists) {
                    target = pathStack.pop();
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Checking path to " + target.getAbsolutePath());

                    if (!target.exists()) {
                        // Don't try if the target is just //
                        if (target.getAbsolutePath().equals(UNC_HEADER)) // PK35957
                        { // PK35957
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Ignoring " + target.getAbsolutePath() + " - is " + UNC_HEADER); // PK35957
                            continue; // PK35957
                        } // PK35957
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Creating path to " + target.getAbsolutePath());
                        boolean created = target.mkdirs();

                        if (created) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Created path to " + target.getAbsolutePath());
                        } else {
                            if (target.getAbsolutePath().startsWith(UNC_HEADER)) // PK35957
                            { // PK35957
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Ignoring " + target.getAbsolutePath() + " - starts with " + UNC_HEADER); // PK35957                                                                                                // PK35957
                                continue; // PK35957
                            } // PK35957
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Did not create path to " + target.getAbsolutePath());

                            if (!target.exists()) {
                                if (tc.isEventEnabled())
                                    Tr.event(tc, "Unable to create directory tree");
                                exists = false;
                            } else {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Path to " + target.getAbsolutePath() + " already exists");
                            }
                        }
                    } else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Path to " + target.getAbsolutePath() + " already exists");
                    }
                }
            }
            // This check is in case a UNC specified file could not be created. We do not set exists to false in the creation path,
            // since we get failures trying to create the server and share part of the path, but need to continue trying
            if (exists && !target.exists()) // PK35957
            { // PK35957
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Did not create path to " + target.getAbsolutePath()); // PK35957
                exists = false; // PK35957
            } // PK35957

        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createDirectoryTree", exists);
        return exists;
    }

    /**
     * Attempt an operation. Retry the operation, possibly several times,
     * across a specified retry interval.
     *
     * @param op          The operation which is being performed.
     * @param fullRetryNs The full retry duration.
     *
     * @return True or false telling if the operation was successful.
     *
     * @throws Exception
     */
    public static boolean retry(Operation op, long fullRetryNs) throws Exception {
        // First try is immediate.
        if (op.act()) {
            return true;
        }

        // Don't retry unless a retry interval was specified.
        if (fullRetryNs == 0) {
            return false;
        }

        // Make sure the retry interval is usable.
        if (fullRetryNs < 0) {
            throw new IllegalArgumentException("Full retry interval (" + fullRetryNs + ") is less then 0");
        }

        // Delay and retry, in increments of the partial retry interval, until
        // at least the full retry interval has elapsed.

        // Time accounting here has ambiguity: Should operation time be considered a part of
        // the time slept?
        //
        // For the intended cases, file deletion and file renaming, the operation time can be
        // considerable.

        long finalNs = System.nanoTime() + fullRetryNs;
        do {
            sleep(STANDARD_RETRY_INTERVAL_NS); // throws InterruptedException
            if (op.act()) {
                return true;
            }
        } while (System.nanoTime() < finalNs);

        return false;
    }

    /**
     * A more accurate version of {@link Thread#sleep}: Use nano-second units,
     * and do not rely on the actual sleep duration being at least the requested
     * duration.
     *
     * Thread.sleep() is repeated until System.nanoTime() shows that the thread
     * has waited at least the specified time in milliseconds.
     *
     * @param requestedNs The requested sleep time, in nano-seconds.
     *
     * @return The time slept, in nano-seconds.
     *
     * @throws InterruptedException Thrown if if the sleep is
     *                                  interrupted.
     */
    private static long sleep(long requestedNs) throws InterruptedException {
        long startNs = System.nanoTime();

        long elapsedNs = 0L;
        long remainingNs = requestedNs;

        while (remainingNs > 0) {
            TimeUnit.NANOSECONDS.sleep(remainingNs); // throws InterruptedException

            elapsedNs = System.nanoTime() - startNs;
            remainingNs = requestedNs - elapsedNs;
        }

        return elapsedNs;
    }

    public static boolean deleteDirectory(File dir) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "deleteDirectory", dir.getAbsolutePath());

        if (!dir.exists()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "deleteDirectory", true);
            return true;
        }

        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if (!deleteDirectory(file)) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "deleteDirectory", false);
                    return false;
                }
            } else {
                if (!deleteFile(file)) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "deleteDirectory", false);
                    return false;
                }
            }
        }

        boolean ret = deleteFile(dir);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "deleteDirectory", ret);
        return ret;
    }

    private static boolean deleteFile(File file) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "deleteFile", file.getAbsolutePath());
        Path path = file.toPath();
        try {
            if (!Files.exists(path)) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deleteFile", true);
                return true;
            }

            boolean ret = Files.deleteIfExists(path);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "deleteFile", ret);
            return ret;
        } catch (IOException e) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "deleteFile", false);
            return false;
        }
    }

    /**
     * Move a filesystem directory to a hidden trash directory for subsequent deletion
     *
     * @param directoryToBeDeleted
     * @return
     */
    public static void archiveAndDeleteDirectoryTree(File directoryToBeDeleted) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "archiveAndDeleteDirectoryTree", directoryToBeDeleted.getAbsolutePath());
        String trashDir = directoryToBeDeleted.getParent() + File.separator + ".trash";
        createDirectoryTree(trashDir);
        File to = new File(trashDir + File.separator + directoryToBeDeleted.getName());

        directoryToBeDeleted.renameTo(to);

        File trashDirFile = new File(trashDir);
        for (File file : trashDirFile.listFiles()) {
            deleteDirectoryTreeTrash(file);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "archiveAndDeleteDirectoryTree");
    }

    /**
     * Recursively delete a filesystem directory and its contents.
     *
     * @param directoryToBeDeleted
     */
    public static void deleteDirectoryTreeTrash(File directoryToBeDeleted) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "deleteDirectoryTreeTrash", directoryToBeDeleted.getAbsolutePath());
        Path rootPath = Paths.get(directoryToBeDeleted.getAbsolutePath());

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (tc.isEntryEnabled())
                        Tr.debug(tc, "deleting file ", file.toString());
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "visitFile: failed to delete because:", e);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    if (tc.isEntryEnabled())
                        Tr.debug(tc, "deleting dir ", dir.toString());
                    try {
                        Files.delete(dir);
                    } catch (IOException e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "postVisitDirectory: failed to delete because:", e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "deleteDirectoryTreeTrash: failed to delete because:", e);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "deleteDirectoryTreeTrash");
        return;
    }
}
