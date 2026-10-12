/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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
package componenttest.topology.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Utilities for working with files
 */
public class FileUtils {
    private static final Class<?> c = FileUtils.class;

    private static final boolean enableLogging = false; // Enable for debug. This can be quite verbose.

    /**
     * Recursively deletes the supplied file or the files within the supplied directory and
     * then deletes the directory itself.
     *
     * @param  file        The file or directory to delete
     * @throws IOException If there was an error deleting the file, the directory or its contents.
     */
    public static void recursiveDelete(File file) throws IOException {
        final String methodName = "recursiveDelete(File)";

        if (file.exists()) {
            /*
             * If a directory, delete all contents first.
             */
            if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    if (child.isDirectory()) {
                        recursiveDelete(child);
                    } else {
                        deleteWithRetry(child, methodName);
                    }
                }
            }

            /*
             * Finally delete the directory (or file) itself.
             */
            if (enableLogging) {
                Log.info(c, methodName, "Deleting " + file);
            }
            deleteWithRetry(file, methodName);
        }
    }

    /**
     * Delete a single file or empty directory, retrying on {@link FileSystemException}
     * to tolerate transient Windows file-handle release delays (e.g. a server process
     * that has logically stopped but whose OS handle on a lock file has not yet been
     * fully released by the kernel). On POSIX systems {@code Files.delete} never throws
     * {@code FileSystemException} for an open file (unlink succeeds regardless), so the
     * retry path is unreachable on Linux/Mac and no OS guard is needed.
     *
     * @param file       the file or empty directory to delete
     * @param methodName caller method name used in log messages
     * @throws IOException if the file cannot be deleted after all retries
     */
    private static void deleteWithRetry(File file, String methodName) throws IOException {
        final int MAX_ATTEMPTS = 4;
        final long RETRY_DELAY_MS = 500;

        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            if (attempt > 1) {
                Log.info(c, methodName,
                         "Sleep " + RETRY_DELAY_MS + " (ms)" +
                         " for delete attempt " + attempt + " of " + MAX_ATTEMPTS);
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Preserve the thread interrupt status.
                    break;
                }
            }

            try {
                Files.delete(file.toPath());
                return; // success

            } catch (FileSystemException fse) {
                // Windows: file handle not yet released by OS. Log and retry.
                Log.error(c, methodName, fse, "Failed to delete " + file.getAbsolutePath() + ": " + fse.getMessage());
                lastException = fse;

            } catch (IOException ioe) {
                // Non-transient failure (permissions, no such file, etc.) — rethrow immediately.
                Log.error(c, methodName, ioe, "Failed to delete " + file.getAbsolutePath() + ": " + ioe.getMessage());
                throw ioe;
            }
        }

        Log.error(c, methodName, lastException, "Failed to delete after " + MAX_ATTEMPTS + " attempts: " + file.getAbsolutePath());
        throw lastException;
    }

    /**
     * Copies the contents of a source file into the destination file. All parent directories
     * for the destination file will be created.
     *
     * @param  sourceFile  The file to copy.
     * @param  destFile    The file to copy to.
     * @throws IOException If an I/O error occurs copying the file.
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        final String methodName = "copyFile(File,File)";

        if (enableLogging) {
            Log.info(c, methodName, "Copying " + sourceFile + " to " + destFile);
        }

        /*
         * Create parent directories if necessary.
         */
        File parentFile = destFile.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            if (enableLogging) {
                Log.info(c, methodName, "Creating parent directory " + parentFile);
            }
            try {
                Files.createDirectories(parentFile.toPath());
            } catch (IOException ioe) {
                Log.error(c, methodName, ioe, "Failed to create directory " + parentFile);
                throw ioe;
            }
        }

        try {
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            Log.error(c, methodName, ioe, "Failed to copy file " + sourceFile + " to " + destFile);
            throw ioe;
        }
    }

    /**
     * Copy the source directory and its contents to the target directory.
     *
     * @param  source      The directory to make a copy of.
     * @param  target      The directory to copy to.
     * @throws IOException If an I/O error occurs copying the directory.
     */
    public static void copyDirectory(File source, File target) throws IOException {
        final String methodName = "copyDirectory(File,File)";

        if (source.isDirectory()) {
            if (!target.exists()) {
                if (enableLogging) {
                    Log.info(c, methodName, "Creating directory " + target);
                }
                try {
                    Files.createDirectories(target.toPath());
                } catch (IOException ioe) {
                    Log.error(c, methodName, ioe, "Failed to create directory " + target);
                    throw ioe;
                }
            }
            if (enableLogging) {
                Log.info(c, methodName, "Copying directory " + source + " to " + target);
            }

            String[] children = source.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(source, children[i]),
                              new File(target, children[i]));
            }
        } else {
            copyFile(source, target);
        }
    }

    /**
     * Read the File specified by the input path to a String.
     *
     * @param  file        The file to read.
     * @return             The file contents as a String.
     * @throws IOException If an I/O error occurs reading the file.
     */
    public static String readFile(String file) throws IOException {
        File f = new File(file);
        if (!f.exists() || f.isDirectory())
            throw new FileNotFoundException(file);

        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(f));
        try {
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line).append('\n');
        } finally {
            br.close();
        }
        return sb.toString();
    }
}
