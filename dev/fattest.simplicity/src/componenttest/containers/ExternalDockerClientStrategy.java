/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package componenttest.containers;

import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.InvalidConfigurationException;
import org.testcontainers.dockerclient.TransportConfig;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.ExternalTestService;

/**
 * This class is discovered by Testcontainers via META-INF/services/org.testcontainers.dockerclient.DockerClientProviderStrategy
 * Nothing else should have to directly reference this class, which is important because we will rely on
 * the FAT that wants to use Testcontainers to provide the org.testcontainers.* classes imported by this class.
 * <p>
 * If you are in the IBM network and want to use an external Docker host, run the FAT with
 *
 * <pre>
 * <code>-Dfat.test.use.remote.docker=true</code>
 * </pre>
 */
public class ExternalDockerClientStrategy extends DockerClientProviderStrategy {

    private static final Class<?> c = ExternalDockerClientStrategy.class;

    private TransportConfig transportConfig;

    private static final boolean usingRemoteDocker = useRemoteDocker();

    private static final int PRIORITY = usingRemoteDocker ? 900 : 0;

    @Override
    public TransportConfig getTransportConfig() throws InvalidConfigurationException {
        if (transportConfig != null)
            return transportConfig;

        try {
            ExternalTestService.getService("docker-engine", ExternalDockerClientFilter.instance());
            transportConfig = TransportConfig.builder() //
                            .dockerHost(ExternalDockerClientFilter.instance().getConfig().getDockerHost()) //
                            .sslConfig(ExternalDockerClientFilter.instance().getConfig().getSSLConfig()) //
                            .build();
        } catch (Exception e) {
            Log.error(c, "getTransportConfig", e, "Unable to locate any healthy docker-engine instances");
            throw new InvalidConfigurationException("Unable to locate any healthy docker-engine instances", e);
        }

        return transportConfig;
    }

    /**
     * Each unsuccessful test will generate a new transport config
     * Test multiple times when running against our remote docker hosts.
     *
     * Fyre networking is flaky, if we get a positive result here, then there
     * is a good change the Fyre network is healthy enough to test against.
     */
    @Override
    protected boolean test() {
        int retry = usingRemoteDocker ? 3 : 1;

        while (retry > 0) {
            Log.info(c, "test", "Verifying strategy, retry countdown: " + retry);

            if (super.test()) {
                Log.info(c, "test", "Verified strategy, using transport config for host " + transportConfig.getDockerHost());
                return true;
            } else {
                Log.info(c, "test", "Unverified strategy, throwing away transport config for host " + transportConfig.getDockerHost());
                transportConfig = null;
                retry--;
            }
        }

        Log.warning(c, "Verification failed for any transport config obtained from consul");
        return false;
    }

    @Override
    public String getDescription() {
        return "Uses a remote docker-host service via ExternalTestService if available. Priority: " + PRIORITY;
    }

    @Override
    protected int getPriority() {
        return PRIORITY;
    }

    @Override
    protected boolean isPersistable() {
        return false;
    }

    @Override
    protected boolean isApplicable() {
        // this strategy is always applicable, but sometimes it is max priority and sometimes it is min priority
        return true;
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
    private static boolean useRemoteDocker() {
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
