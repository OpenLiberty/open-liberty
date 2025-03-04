/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.Props;

import componenttest.containers.substitution.LibertyImageNameSubstitutor;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.ExternalTestService;

@SuppressWarnings("deprecation")
public class TestContainerSuite {

    private static final Class<?> c = TestContainerSuite.class;

    private static boolean setupComplete = false;

    /*
     * THIS METHOD CALL IS REQUIRED TO USE TESTCONTAINERS PLEASE READ:
     *
     * Testcontainers caches data in a properties file located at $HOME/.testcontainers.properties
     * The setupTestcontainers() method will clear and reset the values in this property file.
     *
     * By default, testcontainers will attempt to run against a local docker instance and pull from DockerHub.
     * If you want testcontainers to run against a remote docker host to mirror the behavior of an RTC build
     * Then, set property: -Dfat.test.use.remote.docker=true
     * This will only work if you are on the IBM network.
     *
     * We will set the following properties:
     * 1. docker.client.strategy:
     * Default: [Depends on local OS]
     * Custom : org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy
     * Purpose: This is the strategy testcontainers uses to locate and run against a remote docker instance.
     *
     * 2. image.substitutor:
     * Default: [none]
     * Custom : componenttest.containers.ArtifactoryImageNameSubstitutor
     * Purpose: This defines a strategy for substituting image names.
     * This is so that we can use a private docker repository to cache docker images
     * to avoid the docker pull limits.
     * Example: foo/bar:1.0 it will get changed to [ARTIFACTORY_REGISTRY]/wasliberty-docker-remote/foo/bar:1.0
     */
    static {
        Log.info(TestContainerSuite.class, "<init>", "Setting up testcontainers");
        setupTestcontainers();
    }

    @ClassRule
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void after() {
            Log.info(TestContainerSuite.class, "after", "Assert all container images have been declared");
            ImageVerifier.assertImages();
        }
    };

    /**
     * <pre>
     * By default, Testcontainers will cache the DockerClient strategy in <code>~/.testcontainers.properties</code>.
     *
     * Calling this method in the FATSuite class is REQUIRED for any fat project that uses Testcontainers.
     * This is a safety measure to ensure that we run with the correct docker.client.stategy property
     * for each FATSuite run.
     */
    private static void setupTestcontainers() {
        if (setupComplete)
            return;
        configureLogging();
        generateTestcontainersConfig();
        setupComplete = true;
    }

    /**
     * Configures system properties for the SLF4J simpleLogger.
     * This will output SLF4J logs to a file instead of System.err which is the default behavior.
     *
     * TODO Consider creating a custom SLF4J logger that sends output to the com.ibm.websphere.simplicity.log.Log (non trivial)
     */
    private static void configureLogging() {
        final String m = "configureLogging";
        if (System.getProperty("org.slf4j.simpleLogger.logFile") == null) {
            String logFile = Props.getInstance().getFileProperty(Props.DIR_LOG).getAbsoluteFile() + "/testcontainer.log";

            Log.info(c, m, "The SLF4J simpleLogger is being configured with a logFile of " + logFile);

            // Output location
            System.setProperty("org.slf4j.simpleLogger.logFile", logFile);

            // Output pattern
            System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
            System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "[MM/dd/yyyy HH:mm:ss:SSS z]");
        } else {
            Log.info(c, m, "The SLF4J simpleLogger was already configured with a logFile of " +
                           System.getProperty("org.slf4j.simpleLogger.logFile") +
                           ". Therefore expect testcontainers logs to be found in the aformentioned location");
        }

        // Levels
        System.setProperty("org.slf4j.simpleLogger.log.org.testcontainers", "debug");
        System.setProperty("org.slf4j.simpleLogger.log.tc", "debug");
        System.setProperty("org.slf4j.simpleLogger.log.com.github.dockerjava", "warn");
        System.setProperty("org.slf4j.simpleLogger.log.com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.wire", "off");

    }

    private static void generateTestcontainersConfig() {
        final String m = "generateTestcontainersConfig";
        final File testcontainersConfigFile = new File(System.getProperty("user.home"), ".testcontainers.properties");

        Properties tcProps = new Properties();

        //Create new config file or load existing config properties
        if (testcontainersConfigFile.exists()) {
            Log.info(c, m, "Testcontainers config already exists at: " + testcontainersConfigFile.getAbsolutePath());
            try {
                try (FileInputStream in = new FileInputStream(testcontainersConfigFile)) {
                    tcProps.load(in);
                }
                Files.delete(testcontainersConfigFile.toPath());
            } catch (IOException e) {
                Log.error(c, "generateTestcontainersConfig", e);
                throw new RuntimeException(e);
            }
        } else {
            Log.info(c, m, "Testcontainers config being created at: " + testcontainersConfigFile.getAbsolutePath());
        }

        //If using remote docker then setup strategy
        if (useRemoteDocker()) {
            try {
                ExternalTestService.getService("docker-engine", ExternalDockerClientFilter.instance());
            } catch (Exception e) {
                Log.error(c, "generateTestcontainersConfig", e);
                throw new RuntimeException(e);
            }

            if (ExternalDockerClientFilter.instance().isValid()) {
                tcProps.setProperty("docker.client.strategy", EnvironmentAndSystemPropertyClientProviderStrategy.class.getCanonicalName());
                tcProps.setProperty("docker.host", ExternalDockerClientFilter.instance().getHost());
                tcProps.setProperty("docker.tls.verify", ExternalDockerClientFilter.instance().getVerify());
                tcProps.setProperty("docker.cert.path", ExternalDockerClientFilter.instance().getCertPath());

                // If we are using a remote docker host while running locally, set timeout to 5s, otherwise set timeout to 10s
                // NOTE: if we want to increase this timeout in the future, we also need to increase the timeout of
                // the ExternalDockerClientFilter which tests the connection to the docker host prior.
                tcProps.setProperty("client.ping.timeout", FATRunner.FAT_TEST_LOCALRUN ? "5" : "10");

                tcProps.setProperty("tinyimage.container.image", "public.ecr.aws/docker/library/alpine:3.17");
            } else {
                Log.warning(c, "Unable to find valid External Docker Client");
            }
        } else {
            tcProps.remove("docker.client.strategy");
            tcProps.remove("docker.host");
            tcProps.remove("docker.tls.verify");
            tcProps.remove("docker.cert.path");
            tcProps.remove("client.ping.timeout");
            tcProps.remove("tinyimage.container.image");
        }

        //Always use LibertyImageNameSubstitutor
        tcProps.setProperty("image.substitutor", LibertyImageNameSubstitutor.class.getCanonicalName().toString());

        try {
            tcProps.store(new FileOutputStream(testcontainersConfigFile), "Modified by FAT framework");
            Log.info(c, m, "Testcontainers config properties: " + tcProps.toString());
        } catch (IOException e) {
            Log.error(c, "generateTestcontainersConfig", e);
            throw new RuntimeException(e);
        }
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
