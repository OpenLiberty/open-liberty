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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonParseException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonMappingException;
import org.testcontainers.shaded.com.github.dockerjava.core.DockerClientConfig;
import org.testcontainers.shaded.com.github.dockerjava.core.DockerConfigFile;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.Props;

import componenttest.containers.registry.Registry;
import componenttest.containers.substitution.LibertyImageNameSubstitutor;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.ExternalTestService;

@SuppressWarnings("deprecation")
public class TestContainerSuite {

    private static final Class<?> c = TestContainerSuite.class;

    private static final Path configSource = Paths.get(System.getProperty("user.home"), ".testcontainers.properties");
    private static final Path configBackup = Paths.get(System.getProperty("java.io.tmpdir"), ".testcontainers.backup.properties");

    /**
     * THIS METHOD CALL IS REQUIRED TO USE TESTCONTAINERS PLEASE READ:
     *
     * Testcontainers caches data in a properties file located at $HOME/.testcontainers.properties
     * The {@link #generateConfig()} method will backup the existing config and generate new values in this property file.
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
     * Custom : componenttest.containers.substituiton.LibertyImageNameSubstitutor
     * Purpose: This defines a strategy for substituting image names.
     * This is so that we can use a private docker repository to cache docker images
     * to avoid the docker pull limits.
     */
    static {
        Log.info(TestContainerSuite.class, "<init>", "Setting up testcontainers");
        configureLogging();
        verifyDockerConfig();
        generateConfig();
    }

    @ClassRule
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void after() {
            Log.info(TestContainerSuite.class, "after", "Tearing down testcontainers");
//          restoreConfig(); //TODO re-enable once WL tests are updated to only extend TestContainerSuite on suite classes and not test classes
            ImageVerifier.assertImages();
        }
    };

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

    /**
     * Verify that if an existing docker config file exists that the file
     * is well-formed (json) and can be parsed.
     *
     * If the file is not well-formed, an error will be thrown when run locally,
     * or deleted when running in a build.
     */
    private static void verifyDockerConfig() {
        final String m = "verifyDockerConfig";

        final File configFile = new File(Registry.DEFAULT_CONFIG_DIR, "config.json");

        if (!configFile.exists()) {
            Log.info(c, m, "Docker config file did not exist, nothing to verify: " + configFile.getAbsolutePath());
            return;
        }

        try {
            DockerConfigFile.loadConfig(DockerClientConfig.getDefaultObjectMapper(),
                                        Registry.DEFAULT_CONFIG_DIR.getAbsolutePath());
            Log.info(c, m, "The docker config file is well-formed and parsable: " + configFile.getAbsolutePath());
        } catch (IOException e) {
            // NOTE JsonMappingException/JsonParseException are shaded classes,
            // if Testcontainers unshades these classes switch to the version from fasterxml
            if (e.getCause() == null || !(e.getCause() instanceof JsonMappingException || e.getCause() instanceof JsonParseException)) {
                Log.error(c, m, e);
                throw new RuntimeException("Failed to load docker config file: " + configFile.getAbsolutePath(), e);
            }

            String content = null;
            try {
                content = new String(Files.readAllBytes(configFile.toPath()));
            } catch (IOException e1) {
                // ignore - only attempting to read config file for debug purposes
            }

            String message = "Failed to parse docker config file because it was malformed: " + configFile.getAbsolutePath() +
                             (content == null ? "" : System.lineSeparator() + "Content was: " +
                                                     System.lineSeparator() + content);

            if (FATRunner.FAT_TEST_LOCALRUN) {
                Log.error(c, m, e);
                throw new RuntimeException(message, e);
            } else {
                Log.warning(c, message);
                Log.info(c, m, "Delete docker config file: " + configFile.getAbsolutePath() + " successfully deleted: " + configFile.delete());
            }
        }
    }

    /**
     * Moves existing ~/.testcontainers.properties file (if present) to a backup location.
     * Then generates a new ~/.testcontainers.properties file in it's place.
     *
     * The new properties file will be configured with only the image name substitutor or
     * the properties necessary to connect and use a remote docker host if one is required
     * by the {@link #useRemoteDocker()} method.
     */
    private static void generateConfig() {
        final String m = "generateConfig";

        Properties tcProps = new Properties();

        //Create new config file or load existing config properties
        if (configSource.toFile().exists()) {
            Log.info(c, m, "Testcontainers config already exists at: " + configSource.toAbsolutePath());

            if (!swapConfigFiles(configSource, configBackup)) {
                throw new RuntimeException("Could not backup existing Testcontainers config.");
            }
        } else {
            Log.info(c, m, "Testcontainers config being created at: " + configSource.toAbsolutePath());
        }

        //If using remote docker then setup strategy
        if (useRemoteDocker()) {
            try {
                ExternalTestService.getService("docker-engine", ExternalDockerClientFilter.instance());
            } catch (Exception e) {
                Log.error(c, m, e);
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
        }

        //Always use LibertyImageNameSubstitutor
        tcProps.setProperty("image.substitutor", LibertyImageNameSubstitutor.class.getCanonicalName().toString());

        try {
            tcProps.store(new FileOutputStream(configSource.toFile()), "Modified by FAT framework");
            Log.info(c, m, "Testcontainers config properties: " + tcProps.toString());
        } catch (IOException e) {
            Log.error(c, m, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Moves the .testcontainers.properties file from the backup location (if present)
     * into it's original location at ~/.testcontainers.properties.
     */
    private static void restoreConfig() {
        if (!swapConfigFiles(configBackup, configSource)) {
            throw new RuntimeException("Could not restore original Testcontainers config.");
        }
    }

    /**
     * Swaps the source and destination files
     *
     * @param  source      the source file
     * @param  destination to destination file
     * @return             true iff the swap was successful or unnecessary, false otherwise.
     */
    private static final boolean swapConfigFiles(Path source, Path destination) {
        final String m = "swapConfigFiles";

        // If the source file does not exist then cannot swap
        if (!source.toFile().exists() || !source.toFile().isFile()) {
            Log.info(c, m, "Source file " + source + " does not exist. Skipping swap.");
            return true;
        }

        // If the destination file exists we need to swap
        if (destination.toFile().exists() && destination.toFile().isFile()) {
            Path temp = Paths.get(System.getProperty("java.io.tmpdir"), ".testcontainers.temp.properties");

            Log.info(c, m, "Swapping file " + source
                           + " with file " + destination
                           + " via temporary file " + temp);

            try {
                Files.move(destination, temp);
                Files.move(source, destination);
                Files.move(temp, source);
            } catch (Exception e) {
                Log.error(c, m, e);
                return false;
            }

            return true;
        }

        // Source exists but destination does not, therefore perform a simple rename
        Log.info(c, m, "Moving file " + source
                       + " to file " + destination);

        try {
            Files.move(source, destination);
        } catch (Exception e) {
            Log.error(c, m, e);
            return false;
        }

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
