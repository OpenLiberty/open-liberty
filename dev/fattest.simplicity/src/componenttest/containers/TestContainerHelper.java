/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;

public class TestContainerHelper {

    private static final Class<?> c = TestContainerHelper.class;

    private TestContainerHelper() {
        //Utility class
    }

    /**
     * Determines if we are going to attempt to run against a remote
     * docker host, or a local docker host.
     *
     * Priority:
     * 1. System Property: fat.test.use.remote.docker
     * 2. System Property: fat.test.docker.host -> REMOTE
     * 3. System: GITHUB_ACTIONS -> LOCAL
     * 4. System: WINDOWS -> REMOTE
     * 5. System: ARM -> REMOTE
     *
     * default (!!! fat.test.localrun)
     *
     * @return true, we are running against a remote docker host, false otherwise.
     */
    public static boolean useRemoteDocker() {
        boolean result;
        String reason;

        do {
            //State 1: fat.test.use.remote.docker should always be honored first
            if (System.getProperty("fat.test.use.remote.docker") != null) {
                result = Boolean.getBoolean("fat.test.use.remote.docker");
                reason = "fat.test.use.remote.docker set to " + result;
                break;
            }

            //State 2: User provided a remote docker host, assume they want to use the remote host
            if (ExternalDockerClientFilter.instance().isForced()) {
                result = true;
                reason = "fat.test.docker.host was configured";
                break;
            }

            //State 3: Github actions build should always use local
            if (Boolean.parseBoolean(System.getenv("GITHUB_ACTIONS"))) {
                result = false;
                reason = "GitHub Actions Build";
                break;
            }

            //State 4: Earlier version of TestContainers didn't support docker for windows
            // Assume a user on windows with no other preferences will want to use a remote host.
            if (System.getProperty("os.name", "unknown").toLowerCase().contains("windows")) {
                result = true;
                reason = "Local operating system is Windows. Default container support not guaranteed.";
                break;
            }

            //State 5: ARM architecture can cause performance/starting issues with x86 containers, so also assume remote as the default.
            if (FATRunner.ARM_ARCHITECTURE) {
                result = true;
                reason = "CPU architecture is ARM. x86 container support and performance not guaranteed.";
                break;
            }

            // Default, use local docker for local runs, and remote docker for remote (RTC) runs
            result = !FATRunner.FAT_TEST_LOCALRUN;
            reason = "fat.test.localrun set to " + FATRunner.FAT_TEST_LOCALRUN;
        } while (false);

        reason = result ? //
                        "Remote docker host will be the highest priority. Reason: " + reason : //
                        "Local docker host will be the highest priority. Reason: " + reason;

        Log.info(c, "useRemoteDocker", reason);
        return result;
    }

}
