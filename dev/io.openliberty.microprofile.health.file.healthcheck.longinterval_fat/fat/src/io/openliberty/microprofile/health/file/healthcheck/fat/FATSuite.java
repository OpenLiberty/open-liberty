/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health.file.healthcheck.fat;

import java.io.File;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.HealthFileUtils;

@RunWith(Suite.class)
@SuiteClasses({
    AlwaysPassesTest.class,
    LongIntervalHealthCheckTest.class
})
public class FATSuite {

    /**
     * Checks if all health check files (started, ready, live) have been created.
     * Retries up to 2 seconds with 250ms cycles to account for timing differences
     * between FAT test execution and server file creation.
     *
     * @param serverRootDirFile The server root directory
     * @return true if all files exist, false otherwise
     */
    public static boolean isFilesCreated(File serverRootDirFile) {
        final String METHOD_NAME = "isFilesCreated";
        final int MAX_RETRIES = 8; // 8 * 250ms = 2 seconds
        final int SLEEP_MS = 250;

        for (int i = 0; i < MAX_RETRIES; i++) {
            File healthDir = HealthFileUtils.getHealthDirFile(serverRootDirFile);
            File startFile = HealthFileUtils.getStartFile(serverRootDirFile);
            File readyFile = HealthFileUtils.getReadyFile(serverRootDirFile);
            File liveFile = HealthFileUtils.getLiveFile(serverRootDirFile);

            boolean allExist = healthDir.exists() && startFile.exists() && readyFile.exists() && liveFile.exists();

            if (allExist) {
                Log.info(FATSuite.class, METHOD_NAME, "All health check files created successfully");
                return true;
            }

            Log.info(FATSuite.class, METHOD_NAME,
                     String.format("Retry %d/%d: healthDir=%b, started=%b, ready=%b, live=%b",
                                   i + 1, MAX_RETRIES, healthDir.exists(), startFile.exists(),
                                   readyFile.exists(), liveFile.exists()));

            try {
                Thread.sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        Log.info(FATSuite.class, METHOD_NAME, "Timeout waiting for health check files to be created");
        return false;
    }
}
