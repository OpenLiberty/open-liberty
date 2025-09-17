/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.microprofile.health.file.healthcheck.fat;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.HealthFileUtils;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                SimpleFileBasedHealthCheckTest.class,
                ConfigTest.class,
                MPConfigDefaultValuesTest.class
})

public class FATSuite {

    static final int MAX_ALL_FILES_EXIST_RETRY = 8;

    public static boolean isFilesCreated(File serverRootDirFile) throws InterruptedException {
        return isAllHealthCheckFilesCreated(serverRootDirFile, MAX_ALL_FILES_EXIST_RETRY);
    }

    public static boolean isAllHealthCheckFilesCreated(File serverRootDirFile, int retries) throws InterruptedException {
        String methodName = "isFilesCreated";
        int attemptNumber = 1;

        while (attemptNumber <= retries) {

            boolean isDirExist = HealthFileUtils.getHealthDirFile(serverRootDirFile).exists();
            boolean isStartedExist = HealthFileUtils.getStartFile(serverRootDirFile).exists();
            boolean isLiveExist = HealthFileUtils.getLiveFile(serverRootDirFile).exists();
            boolean isReadyExist = HealthFileUtils.getReadyFile(serverRootDirFile).exists();

            if (isDirExist && isStartedExist && isLiveExist && isReadyExist) {
                Log.info(FATSuite.class, methodName,
                         String.format("Succesfully verified all health check files created at attempt: %d. Max attempt is: %d.", attemptNumber, retries));
                return true;
            }

            Log.info(FATSuite.class, methodName,
                     String.format("At attempt %d with the following: HealthDir[%s], startedFile[%s], liveFile[%s], readyFile[%s].Max attempt is: %d.", attemptNumber, isDirExist,
                                   isStartedExist, isLiveExist, isReadyExist, retries));

            TimeUnit.MILLISECONDS.sleep(250);
            attemptNumber++;
        }

        return false;
    }

}
