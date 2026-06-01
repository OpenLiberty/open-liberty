/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health.internal_fat.shared;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Utility class for health check file operations in FAT tests.
 * This class provides common methods for working with health check files
 * across multiple test projects.
 */
public class HealthFileUtils {

    public static final String SHOULD_HAVE = " should have been created.";
    public static final String SHOULD_NOT_HAVE = " should not have been created.";

    public static final String HEALTH_DIR_SHOULD_HAVE = "/health" + SHOULD_HAVE;
    public static final String HEALTH_DIR_SHOULD_NOT_HAVE = "/health" + SHOULD_NOT_HAVE;

    public static final String LIVE_SHOULD_HAVE = "/health/live" + SHOULD_HAVE;
    public static final String LIVE_SHOULD_NOT_HAVE = "/health/live" + SHOULD_NOT_HAVE;

    public static final String STARTED_SHOULD_HAVE = "/health/started" + SHOULD_HAVE;
    public static final String STARTED_SHOULD_NOT_HAVE = "/health/started" + SHOULD_NOT_HAVE;

    public static final String READY_SHOULD_HAVE = "/health/ready" + SHOULD_HAVE;
    public static final String READY_SHOULD_NOT_HAVE = "/health/ready" + SHOULD_NOT_HAVE;

    public static final int MAX_ALL_FILES_EXIST_RETRY = 8;

    private static void log(String method, String msg) {
        Log.info(HealthFileUtils.class, method, msg);
    }

    /**
     * Get the last modified time of a file.
     *
     * @param file the file to check
     * @return time in ms since epoch, or -1 if file does not exist
     */
    public static long getLastModifiedTime(File file) {
        final String METHOD_NAME = "getLastModifiedTime";

        if (!file.exists()) {
            log(METHOD_NAME, String.format("File %s does not exist", file.getAbsolutePath()));
            return -1;
        }

        return file.lastModified();
    }

    /**
     * Get the last modified time of a file using NIO.
     *
     * @param file the file to check
     * @return time in millis since epoch, or -1 if file does not exist
     * @throws IOException if an I/O error occurs
     */
    public static long getLastModifiedTimeNIO(File file) throws IOException {
        final String METHOD_NAME = "getLastModifiedTimeNIO";

        if (!file.exists()) {
            log(METHOD_NAME, String.format("File %s does not exist", file.getAbsolutePath()));
            return -1;
        }

        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        return attr.lastModifiedTime().toMillis();
    }

    /**
     * Get the creation time of a file.
     *
     * @param file the file to check
     * @return time in millis since epoch, or -1 if file does not exist
     * @throws IOException if an I/O error occurs
     */
    public static long getCreatedTime(File file) throws IOException {
        final String METHOD_NAME = "getCreatedTime";

        if (!file.exists()) {
            log(METHOD_NAME, String.format("File %s does not exist", file.getAbsolutePath()));
            return -1;
        }

        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        return attr.creationTime().toMillis();
    }

    /**
     * Check if a file's last modified time is within the specified duration.
     *
     * @param file     the file to check
     * @param duration the duration to check against
     * @return true if the file was modified within the duration, false otherwise
     */
    public static boolean isLastModifiedTimeWithinLast(File file, Duration duration) {
        final String METHOD_NAME = "isLastModifiedTimeWithinLast";

        if (!file.exists()) {
            log(METHOD_NAME, String.format("File %s does not exist", file.getAbsolutePath()));
            return false;
        }

        long currTimeMilli = System.currentTimeMillis();
        long lastMod = getLastModifiedTime(file);
        long diff = (currTimeMilli - lastMod);

        log(METHOD_NAME, String.format("The current time is [%d]. The last modified time was [%d]. The differene is [%d]", currTimeMilli, lastMod, diff));

        return diff <= duration.toMillis();
    }

    /**
     * Get the health directory file.
     *
     * @param serverRootDirFile the server root directory
     * @return the health directory file
     */
    public static File getHealthDirFile(File serverRootDirFile) {
        File healthDirFile = new File(serverRootDirFile, "health");
        return healthDirFile;
    }

    /**
     * Get the started file.
     *
     * @param serverRootDirFile the server root directory
     * @return the started file
     */
    public static File getStartFile(File serverRootDirFile) {
        File startedFile = new File(getHealthDirFile(serverRootDirFile), HealthCheckFileName.STARTED_FILE.getFileName());
        return startedFile;
    }

    /**
     * Get the ready file.
     *
     * @param serverRootDirFile the server root directory
     * @return the ready file
     */
    public static File getReadyFile(File serverRootDirFile) {
        File readyFile = new File(getHealthDirFile(serverRootDirFile), HealthCheckFileName.READY_FILE.getFileName());
        return readyFile;
    }

    /**
     * Get the live file.
     *
     * @param serverRootDirFile the server root directory
     * @return the live file
     */
    public static File getLiveFile(File serverRootDirFile) {
        File liveFile = new File(getHealthDirFile(serverRootDirFile), HealthCheckFileName.LIVE_FILE.getFileName());
        return liveFile;
    }

    /**
     * Check if all health check files have been created.
     *
     * @param serverRootDirFile the server root directory
     * @return true if all files exist, false otherwise
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public static boolean isFilesCreated(File serverRootDirFile) throws InterruptedException {
        return isAllHealthCheckFilesCreated(serverRootDirFile, MAX_ALL_FILES_EXIST_RETRY);
    }

    /**
     * Check if all health check files have been created with a specified number of retries.
     *
     * @param serverRootDirFile the server root directory
     * @param retries           the number of retries
     * @return true if all files exist, false otherwise
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public static boolean isAllHealthCheckFilesCreated(File serverRootDirFile, int retries) throws InterruptedException {
        String methodName = "isAllHealthCheckFilesCreated";
        int attemptNumber = 1;

        while (attemptNumber <= retries) {

            boolean isDirExist = HealthFileUtils.getHealthDirFile(serverRootDirFile).exists();
            boolean isStartedExist = HealthFileUtils.getStartFile(serverRootDirFile).exists();
            boolean isLiveExist = HealthFileUtils.getLiveFile(serverRootDirFile).exists();
            boolean isReadyExist = HealthFileUtils.getReadyFile(serverRootDirFile).exists();

            if (isDirExist && isStartedExist && isLiveExist && isReadyExist) {
                Log.info(HealthFileUtils.class, methodName,
                         String.format("Succesfully verified all health check files created at attempt: %d. Max attempt is: %d.", attemptNumber, retries));
                return true;
            }

            Log.info(HealthFileUtils.class, methodName,
                     String.format("At attempt %d with the following: HealthDir[%s], startedFile[%s], liveFile[%s], readyFile[%s].Max attempt is: %d.", attemptNumber, isDirExist,
                                   isStartedExist, isLiveExist, isReadyExist, retries));

            TimeUnit.MILLISECONDS.sleep(250);
            attemptNumber++;
        }

        return false;
    }

    enum HealthCheckFileName {
        STARTED_FILE("started"),
        READY_FILE("ready"),
        LIVE_FILE("live");

        private final String fileName;

        HealthCheckFileName(String fileName) {
            this.fileName = fileName;
        }

        String getFileName() {
            return fileName;
        }
    }
}

// Made with Bob
