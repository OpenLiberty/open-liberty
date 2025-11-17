/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
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
package com.ibm.ws.app.manager.war.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

import com.ibm.ws.artifact.zip.cache.ZipCachingProperties;

/**
 * File and zip file utilities.
 */
public class ZipUtils {
    private static final TraceComponent tc = Tr.register(ZipUtils.class);

    // Retry parameters:
    //
    // Total of twice the zip.reaper.slow.pend.max.

    public static final int RETRY_COUNT;
    public static final long RETRY_AMOUNT = 50; // Split into 50ms wait periods.
    public static final int RETRY_LIMIT = 1000; // Don't ever wait more than a second.

    static {
        if ( ZipCachingProperties.ZIP_CACHE_REAPER_MAX_PENDING == 0 ) {
            // MAX_PENDING == 0 means the reaper cache is disabled.
            // Allow just one retry for normal zip processing.
            RETRY_COUNT = 1;

        } else {
            // The quiesce time is expected to be no greater than twice the largest
            // wait time set for the zip file cache.  Absent new activity, the reaper
            // cache will never wait longer than twice the largest wait time.
            // (The maximum wait time is more likely capped at 20% above the largest
            // wait time.  That is changed to 100% as an added safety margin.)
            //
            // The quiesce time will not be correct if the reaper thread is starved
            // and is prevented from releasing zip files.
            long totalAmount = ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MAX * 2;
            if ( totalAmount <= 0 ) {
                // The pending max is supposed to be greater than zero.  Put in safe
                // values just in case it isn't.
                RETRY_COUNT = 1;
            } else {
                // The slow pending max is not expected to be set to values larger
                // than tenth's of seconds.  To make the limit explicit, don't accept
                // a retry limit which is more than 1/2 second.
                if ( totalAmount > RETRY_LIMIT ) {
                    totalAmount = RETRY_LIMIT; // 1/2 second * 2 == 1 second
                }

                // Conversion to int is safe: The total amount must be
                // greater than 0 and less than or equal to 1000.  The
                // retry count will be greater than 1 and less then or
                // equal to 20.
                int retryCount = (int) (totalAmount / RETRY_AMOUNT);
                if ( totalAmount % RETRY_AMOUNT > 0 ) {
                    retryCount++;
                }
                RETRY_COUNT = retryCount;
            }
        }
    }

    /**
     * Attempt to recursively delete a target file.
     *
     * Answer null if the delete was successful. Answer the first
     * file which could not be deleted if the delete failed.
     *
     * If the delete fails, wait 400 MS then try again. Do this
     * for the entire delete operation, not for each file deletion.
     *
     * A test must be done to verify that the file exists before
     * invoking this method: If the file does not exist, the
     * delete operation will fail.
     *
     * @param file The file to delete recursively.
     *
     * @return Null if the file was deleted. The first file which
     *         could not be deleted if the file could not be deleted.
     */
    @Trivial
    public static File deleteWithRetry(File file) {
        String methodName = "deleteWithRetry";

        String filePath;
        if (tc.isDebugEnabled()) {
            filePath = file.getAbsolutePath();
            Tr.debug(tc, methodName + ": Recursively delete [ " + filePath + " ]");
        } else {
            filePath = null;
        }

        File firstFailure = delete(file);
        if (firstFailure == null) {
            if (filePath != null) {
                Tr.debug(tc, methodName + ": Successful first delete [ " + filePath + " ]");
            }
            return null;
        }

        if (filePath != null) {
            Tr.debug(tc, methodName + ": Failed first delete [ " + filePath + " ]: Sleep up to 50 ms and retry");
        }

        // Extract can occur with the server running, and not long after activity
        // on the previously extracted archives.
        //
        // If the first delete attempt failed, try again, up to a limit based on
        // the expected quiesce time of the zip file cache.

        File retryFailure = firstFailure;
        int tryNo;
        for (tryNo = 0; (retryFailure != null) && tryNo < RETRY_COUNT; tryNo++) {
            try {
                Thread.sleep(RETRY_AMOUNT);
            } catch (InterruptedException e) {
                // FFDC
            }
            retryFailure = delete(file);
        }

        if (retryFailure == null) {
            if (filePath != null) {
                Tr.debug(tc, methodName + ": Successful delete of [ " + filePath + " ] after [ " + tryNo + " ] retries");
            }
            return null;
        } else {
            if (filePath != null) {
                Tr.debug(tc, methodName + ": Failed to delete [ " + filePath + " ]. Tried [ " + (tryNo + 1) + " ] times.");
            }
            return retryFailure;
        }
    }

    /**
     * Recursively deletes the specified file or directory.
     * <p>
     * - If the input is a symbolic link, the link itself is deleted but not followed.<br>
     * - If the input is a directory, all contents (files and subdirectories) are recursively deleted.<br>
     * - The method attempts to delete all files and directories under the specified root, even after encountering failures.<br>
     * - If any deletion fails, the method continues with the remaining files and directories.
     * </p>
     *
     * @param file the file or directory to delete; must not be {@code null}
     * @return {@code null} if the entire tree was successfully deleted;<br>
     *         otherwise, returns the first {@link File} that failed to be deleted
     * @throws IllegalArgumentException if {@code file} is {@code null}
     */
    public static File delete(File file) {
        String methodName = "delete";

        if (file == null) {
            throw new IllegalArgumentException("File must not be null");
        }

        Path path = file.toPath();

        // If the root file or directory is a symlink, we delete the link and we're done
        if (Files.isSymbolicLink(path)) {
            if (!file.delete()) {
                return file;
            }
            return null;
        }

        final File[] failedFile = new File[1]; // first failure will be stored here

        if (file.isDirectory()) {
            try {
                // Note that Files.walkFileTree does not follow symbolic links by default.
                // We never want to follow symbolic links.
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path subPath, BasicFileAttributes attrs) {
                        File f = subPath.toFile();
                        if (!f.delete() && failedFile[0] == null) {
                            failedFile[0] = f;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                        // Log the exception if there was one, but don't stop the walk
                        if (exc != null && tc.isDebugEnabled()) {
                            Tr.debug(tc, methodName + ": Exception while visiting directory [ " + dir + " ]: " + exc.getMessage());
                        }

                        // Don't delete the root directory here — it will be deleted after walkFileTree finishes
                        if (!dir.equals(path)) {
                            File d = dir.toFile();
                            if (!d.delete() && failedFile[0] == null) {
                                failedFile[0] = d;
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        if (failedFile[0] == null) {
                            failedFile[0] = file.toFile();
                        }
                        return FileVisitResult.CONTINUE;
                    }

                });
            } catch (IOException e) {
                // An exception occurred in Files.walkFileTree when trying to access a file.
                // Here, we do not know which file caused the failure, and the traversal stops.
                // This is an unlikely event, since we've overridden visitFileFailed.
                if (failedFile[0] == null) {
                    failedFile[0] = file; // report root directory by default
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + ": Exception walking file tree - " + e.getMessage());
                }
            }
        }

        // Delete the root directory or file last
        if (!file.delete()) {
            if (failedFile[0] == null) {
                failedFile[0] = file;
            }
        }
        return failedFile[0];
    }

    //

    public static final boolean IS_EAR = true;
    public static final boolean IS_NOT_EAR = false;

    private static final String WAR_EXTENSION = ".war";

    /**
     * Unpack a source archive into a target directory.
     *
     * If the source archive is a WAR, package web module archives as well.
     *
     * This operation is not smart enough to avoid unpacking WAR files
     * in an application library folder.  However, that is very unlikely to
     * happen.
     *
     * @param source The source archive which is to be unpacked.
     * @param target The directory into which to unpack the source archive.
     * @param isEar Control parameter: Is the source archive an EAR file.
     *     When the source archive is an EAR, unpack nested WAR files.
     * @param lastModified The last modified value to use for the expanded
     *     archive.
     *
     * @throws IOException Thrown in case of a failure.
     */
    public static void unzip(
        File source, File target,
        boolean isEar, long lastModified) throws IOException {

        byte[] transferBuffer = new byte[16 * 1024];
        unzip(source, target, isEar, lastModified, transferBuffer);
    }

    @Trivial
    public static void unzip(
        File source, File target,
        boolean isEar, long lastModified,
        byte[] transferBuffer) throws IOException {

        String methodName = "unzip";

        String sourcePath = source.getAbsolutePath();
        String targetPath = target.getAbsolutePath();

        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + ": Source [ " + sourcePath + " ] Size [ " + source.length() + " ]");
            Tr.debug(tc, methodName + ": Target [ " + targetPath + " ]");
        }

        if ( !source.exists() ) {
            throw new IOException("Source [ " + sourcePath + " ] does not exist");
        } else if ( !source.isFile() ) {
            throw new IOException("Source [ " + sourcePath + " ] is not a simple file");
        } else if ( !target.exists() ) {
            throw new IOException("Target [ " + targetPath + " ] does not exist");
        } else if ( !target.isDirectory() ) {
            throw new IOException("Target [ " + targetPath + " ] is not a directory");
        }

        List<Object[]> warData = ( isEar ? new ArrayList<Object[]>() : null );

        ZipFile sourceZip = new ZipFile(source);
        try {
            Enumeration<? extends ZipEntry> sourceEntries = sourceZip.entries();
            while ( sourceEntries.hasMoreElements() ) {
                ZipEntry sourceEntry = sourceEntries.nextElement();

                String sourceEntryName = sourceEntry.getName();
                if ( reachesOut(sourceEntryName) ) {
                    Tr.error(tc, "error.file.outside.archive", sourceEntryName, sourcePath);
                    continue;
                }

                String targetFileName;
                Object[] nextWarData;
                if ( isEar && !sourceEntry.isDirectory() && sourceEntryName.endsWith(WAR_EXTENSION) ) {
                    for ( int tmpNo = 0;
                          sourceZip.getEntry( targetFileName = sourceEntryName + ".tmp" + tmpNo ) != null;
                          tmpNo++ ) {
                        // Empty
                    }
                    nextWarData = new Object[] { sourceEntryName, targetFileName, null };
                } else {
                    targetFileName = sourceEntryName;
                    nextWarData = null;
                }

                File targetFile = new File(target, targetFileName);

                if ( sourceEntry.isDirectory() ) {
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + ": Directory [ " + sourceEntryName + " ]");
                    }

                    if ( !targetFile.exists() && !targetFile.mkdirs() ) {
                        throw new IOException("Failed to create directory [ + " + targetFile.getAbsolutePath() + " ]");
                    }

                } else {
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + ": Simple file [ " + sourceEntryName + " ] [ " + sourceEntry.getSize() + " ]");
                    }

                    File targetParent = targetFile.getParentFile();
                    if ( !targetParent.mkdirs() && !targetParent.exists() ) {
                        throw new IOException("Failed to create directory [ " + targetParent.getAbsolutePath() + " ]");
                    }

                    transfer(sourceZip, sourceEntry, targetFile, transferBuffer); // throws IOException
                }

                // If the entry doesn't provide a meaningful last modified time,
                // use the parent file's last modified time.

                long entryModified = sourceEntry.getTime();
                if ( entryModified == -1 ) {
                    entryModified = lastModified;
                }
                targetFile.setLastModified(entryModified);

                if ( nextWarData != null ) {
                    nextWarData[2] = Long.valueOf(entryModified);

                    warData.add(nextWarData);
                }
            }

        } finally {
            if ( sourceZip != null ) {
                sourceZip.close();
            }
        }

        if ( isEar ) {
            for ( Object[] nextWarData : warData ) {
                String unpackedWarName = (String) nextWarData[0];
                String packedWarName = (String) nextWarData[1];
                long warLastModified = ((Long) nextWarData[2]).longValue();

                File unpackedWarFile = new File(target, unpackedWarName);
                if ( !unpackedWarFile.exists() && !unpackedWarFile.mkdirs() ) {
                    throw new IOException("Failed to create [ " + unpackedWarFile.getAbsolutePath() + " ]");
                }

                File packedWarFile = new File(target, packedWarName);

                unzip(packedWarFile, unpackedWarFile, IS_NOT_EAR, warLastModified, transferBuffer);

                if ( !packedWarFile.delete() ) {
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + ": Failed to delete temporary WAR [ " + packedWarFile.getAbsolutePath() + " ]");
                    }
                }
            }
        }

        // Do this last: The extraction into the target will update
        // the target time stamp.  We need the time stamp to be the time stamp
        // of the source.

        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + ": Set last modified [ " + lastModified + " ]");
        }
        target.setLastModified(lastModified); 
    }

    @Trivial
    private static void transfer(
        ZipFile sourceZip, ZipEntry sourceEntry,
        File targetFile,
        byte[] transferBuffer) throws IOException {

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = sourceZip.getInputStream(sourceEntry);
            outputStream = new FileOutputStream(targetFile);

            int lastRead;
            while ( (lastRead = inputStream.read(transferBuffer)) != -1) {
                outputStream.write(transferBuffer, 0, lastRead);
            }

        } finally {
            if ( inputStream != null ) {
                inputStream.close();
            }
            if ( outputStream != null ) {
                outputStream.close();
            }
        }
    }

    private static boolean reachesOut(String entryPath) {
        if ( !entryPath.contains("..") ) {
            return false;
        }

        String normalizedPath = PathUtils.normalizeUnixStylePath(entryPath);
        return PathUtils.isNormalizedPathAbsolute(normalizedPath); // Leading ".." or "/.."

        // The following is very inefficient ... and doesn't work when there
        // are symbolic links.
        // return targetFile.getCanonicalPath().startsWith(targetDirectory.getCanonicalPath() + File.separator);
    }
}
