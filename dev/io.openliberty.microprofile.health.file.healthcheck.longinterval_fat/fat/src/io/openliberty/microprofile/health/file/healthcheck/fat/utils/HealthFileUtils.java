/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health.file.healthcheck.fat.utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Utilities for file-based health checks used by FAT tests.
 */
public class HealthFileUtils {
    private static void log(String method, String msg) {
        Log.info(HealthFileUtils.class, method, msg);
    }

    // ------------------------------
    // Existing helpers
    // ------------------------------

    public static long getLastModifiedTime(File file) {
        final String METHOD_NAME = "getLastModifiedTime";

        if (!file.exists()) {
            log(METHOD_NAME, String.format("File %s does not exist", file.getAbsolutePath()));
            return -1;
        }

        return file.lastModified();
    }

    public static boolean isLastModifiedTimeWithinLast(File file, Duration duration) {
        final String METHOD_NAME = "isLastModifiedTimeWithinLast";

        if (!file.exists()) {
            log(METHOD_NAME, String.format("File %s does not exist", file.getAbsolutePath()));
            return false;
        }

        long currTimeMilli = System.currentTimeMillis();
        long lastMod = getLastModifiedTime(file);
        long diff = (currTimeMilli - lastMod);

        log(METHOD_NAME, String.format("The current time is [%d]. The last modified time was [%d]. The difference is [%d]", currTimeMilli, lastMod, diff));

        return diff <= duration.toMillis();
    }

    public static File getHealthDirFile(File serverRootDirFile) {
        return new File(serverRootDirFile, "health");
    }

    public static File getStartFile(File serverRootDirFile) {
        return new File(getHealthDirFile(serverRootDirFile), HealthCheckFileName.STARTED_FILE.getFileName());
    }

    public static File getReadyFile(File serverRootDirFile) {
        return new File(getHealthDirFile(serverRootDirFile), HealthCheckFileName.READY_FILE.getFileName());
    }

    public static File getLiveFile(File serverRootDirFile) {
        return new File(getHealthDirFile(serverRootDirFile), HealthCheckFileName.LIVE_FILE.getFileName());
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

    // ------------------------------
    // New polling/wait helpers
    // ------------------------------

    private static boolean sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os != null && os.toLowerCase().contains("win");
    }

    /** Apply a small cushion to timeouts on Windows runners (file I/O/locking tends to be slower). */
    private static long adjustSecondsForWindows(long seconds) {
        return isWindows() ? Math.round(seconds * 1.5) : seconds;
    }

    /**
     * Generic poll-until with a timeout.
     *
     * @return true if condition became true within the timeout; false otherwise.
     */
    public static boolean waitUntil(BooleanSupplier condition, long timeoutSeconds, long pollMillis) {
        if (condition == null)
            return false;
        long start = System.nanoTime();
        long timeoutNanos = Duration.ofSeconds(adjustSecondsForWindows(timeoutSeconds)).toNanos();
        long poll = Math.max(25L, pollMillis);
        while (System.nanoTime() - start < timeoutNanos) {
            if (condition.getAsBoolean())
                return true;
            if (!sleepQuietly(poll))
                return false;
        }
        return condition.getAsBoolean();
    }

    /** Wait for a file to appear. */
    public static boolean waitForFileExists(File f, long timeoutSeconds, long pollMillis) {
        return waitUntil(() -> f != null && f.exists(), timeoutSeconds, pollMillis);
    }

    /** Wait for a file to disappear. */
    public static boolean waitForFileMissing(File f, long timeoutSeconds, long pollMillis) {
        return waitUntil(() -> f == null || !f.exists(), timeoutSeconds, pollMillis);
    }

    /**
     * Wait until the file's lastModified becomes greater than the given baseline.
     * If the baseline is negative (e.g., file didn't exist before), the first existence counts as an update.
     */
    public static boolean waitForUpdateSince(File f, long baselineMillis, long timeoutSeconds, long pollMillis) {
        return waitUntil(() -> {
            if (f == null || !f.exists())
                return false;
            long lm = f.lastModified();
            return (baselineMillis < 0) ? lm > 0 : lm > baselineMillis;
        }, timeoutSeconds, pollMillis);
    }

    /**
     * Verify that a file does NOT update beyond the given baseline for the whole window.
     * Returns true if no update occurred during the window.
     * If the file doesn't exist, it's also considered "no update".
     */
    public static boolean waitForNoUpdateSince(File f, long baselineMillis, long windowSeconds, long pollMillis) {
        long start = System.nanoTime();
        long window = Duration.ofSeconds(adjustSecondsForWindows(windowSeconds)).toNanos();
        long poll = Math.max(25L, pollMillis);
        while (System.nanoTime() - start < window) {
            if (f != null && f.exists()) {
                long lm = f.lastModified();
                if (baselineMillis >= 0 && lm > baselineMillis) {
                    return false; // it updated
                }
                if (baselineMillis < 0 && lm > 0) {
                    return false; // considered an update from "non-existent" baseline
                }
            }
            if (!sleepQuietly(poll))
                return false;
        }
        return true; // no update observed in the window
    }

    /** Wait for a file to contain a specific substring. */
    public static boolean waitForFileContains(Path p, String needle, long timeoutSeconds, long pollMillis) {
        return waitUntil(() -> {
            try {
                if (p == null || needle == null)
                    return false;
                if (!Files.exists(p))
                    return false;
                List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.contains(needle))
                        return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }, timeoutSeconds, pollMillis);
    }
}
