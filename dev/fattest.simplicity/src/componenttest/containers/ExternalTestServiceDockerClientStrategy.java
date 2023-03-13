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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import java.util.function.Predicate;

import javax.net.SocketFactory;

import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.InvalidConfigurationException;
import org.testcontainers.dockerclient.TransportConfig;
import org.testcontainers.shaded.com.github.dockerjava.core.DefaultDockerClientConfig;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.ExternalTestService;
import componenttest.topology.utils.ExternalTestServiceFilter;
import componenttest.topology.utils.HttpsRequest;

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
public class ExternalTestServiceDockerClientStrategy extends DockerClientProviderStrategy {

    private static final Class<?> c = ExternalTestServiceDockerClientStrategy.class;

    @Deprecated
    /**
     * Use of a service filter should be used with extreme caution.
     * There is a possibility of filtering out all available docker engines available to a build machine.
     * Resulting in an unavoidable failed FAT bucket.
     * If certain docker engines are performing poorly please raise an issue.
     */
    public static Predicate<ExternalTestService> serviceFilter = null;

    /**
     * Used to specify a particular docker host machine to run with. For example: -Dfat.test.docker.host=some-docker-host.mycompany.com
     */
    private static final String USE_DOCKER_HOST = System.getProperty("fat.test.docker.host");

    private DefaultDockerClientConfig config;
    private TransportConfig transportConfig;

    static boolean setupComplete = false;

    /**
     * Used to specify if we plan on running against a remote docker host, or a local docker host.
     *
     * @see #useRemoteDocker()
     */
    public static final boolean USE_REMOTE_DOCKER_HOST = useRemoteDocker();

    /**
     * Used to specify if we plan on running using the artifactory name substituion class, or not.
     *
     * @see #useArtifactorySubstitutor()
     */
    public static final boolean USE_ARTIFACTORY_NAME_SUBSTITUTION = useArtifactorySubstitutor();

    /**
     * <pre>
     * By default, Testcontainers will cache the DockerClient strategy in <code>~/.testcontainers.properties</code>.
     *
     * Calling this method in the FATSuite class is REQUIRED for any fat project that uses Testcontainers.
     * This is a safety measure to ensure that we run with the correct docker.client.stategy property
     * for each FATSuite run.
     *
     * Example Usage:
     *
     * &#64;RunWith(Suite.class)
     * &#64;SuiteClasses({ FailoverTest.class })
     * public class FATSuite {
     *   static {
     *     ExternalTestServiceDockerClientStrategy.setupTestcontainers();
     *   }
     * }
     * </pre>
     */
    public static void setupTestcontainers() {
        String m = "setupTestcontainers";
        Log.entering(c, m, setupComplete);
        if (setupComplete)
            return;
        generateTestcontainersConfig();
        generateArtifactorySubstitutorConfig();
        setupComplete = true;
        Log.exiting(c, m, setupComplete);
    }

    private static void generateTestcontainersConfig() {
        File testcontainersConfigFile = new File(System.getProperty("user.home"), ".testcontainers.properties");

        Log.info(c, "generateTestcontainersConfig", "Resetting testcontainers property file at: " + testcontainersConfigFile.getAbsolutePath());
        try {
            Properties tcProps = new Properties();
            if (testcontainersConfigFile.exists()) {
                FileInputStream tcPropsInputStream = new FileInputStream(testcontainersConfigFile);
                tcProps.load(tcPropsInputStream);
                tcProps.remove("docker.client.strategy");
                tcPropsInputStream.close(); // avoids delete failing on windows
                Files.delete(testcontainersConfigFile.toPath());
            }
            tcProps.setProperty("image.substitutor", ArtifactoryImageNameSubstitutor.class.getCanonicalName().toString());
            tcProps.store(new FileOutputStream(testcontainersConfigFile), "Modified by FAT framework");
        } catch (IOException e) {
            Log.error(c, "generateTestcontainersConfig", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate a config file at ~/.docker/config.json if a private docker registry will be used
     * Or if a config.json already exists, make sure that the private registry is listed. If not, add
     * the private registry to the existing config
     */
    private static void generateDockerConfig(String registry, String authToken) {
        final String m = "generateDockerConfig";

        File configDir = new File(System.getProperty("user.home"), ".docker");
        File configFile = new File(configDir, "config.json");
        String contents = "";

        String privateAuth = "\t\t\"" + registry + "\": {\n" +
                             "\t\t\t\"auth\": \"" + authToken + "\",\n"
                             + "\t\t\t\"email\": null\n" + "\t\t}";
        if (configFile.exists()) {
            Log.info(c, m, "Config already exists at: " + configFile.getAbsolutePath());
            try {
                for (String line : Files.readAllLines(configFile.toPath()))
                    contents += line + '\n';
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            logConfigContents(m, "Original contents", contents);
            int authsIndex = contents.indexOf("\"auths\"");
            boolean replacedAuth = false;

            if (contents.contains(registry)) {
                Log.info(c, m, "Config already contains the private registry: " + registry);
                int registryIndex = contents.indexOf(registry, authsIndex);
                int authIndex = contents.indexOf("\"auth\":", registryIndex);
                int authIndexEnd = contents.indexOf(',', authIndex) + 1;
                String authSubstring = contents.substring(authIndex, authIndexEnd);
                if (authSubstring.contains(authToken)) {
                    Log.info(c, m, "Config already contains the correct authToken for registry: " + registry);
                    return;
                } else {
                    replacedAuth = true;
                    Log.info(c, m, "Replacing auth token for registry: " + registry);
                    contents = contents.replace(authSubstring, "\"auth\": \"" + authToken + "\",");
                }
            }

            if (authsIndex >= 0 && !replacedAuth) {
                Log.info(c, m, "Other auths exist. Need to add private registry: " + registry);
                int splitAt = contents.indexOf('{', authsIndex);
                String firstHalf = contents.substring(0, splitAt + 1);
                String secondHalf = contents.substring(splitAt + 1);
                contents = firstHalf + '\n' + privateAuth + ",\n" + secondHalf;
            } else if (!replacedAuth) {
                Log.info(c, m, "No auths exist. Adding auth block");
                int splitAt = contents.indexOf('{');
                String firstHalf = contents.substring(0, splitAt + 1);
                String secondHalf = contents.substring(splitAt + 1);
                String delimiter = secondHalf.contains("{") ? "," : "";
                contents = firstHalf + "\n\t\"auths\": {\n" + privateAuth + "\n\t}" + delimiter + secondHalf;
            }
        } else {
            configDir.mkdirs();
            Log.info(c, m, "Using remote docker so generating a private registry config file at: "
                           + configFile.getAbsolutePath());
            contents = "{\n\t\"auths\": {\n" + privateAuth + "\n\t}\n}";
        }
        logConfigContents(m, "New config.json contents are", contents);
        configFile.delete();
        writeFile(configFile, contents);
    }

    /**
     * Log the contents of a config file that may contain authentication data which should be redacted.
     *
     * @param method
     * @param msg
     * @param contents
     */
    private static void logConfigContents(String method, String msg, String contents) {
        String sanitizedContents = contents.replaceAll("\"auth\": \".*\"", "\"auth\": \"****Token Redacted****\"");
        Log.info(c, method, msg + ":\n" + sanitizedContents);
    }

    private static void generateArtifactorySubstitutorConfig() {
        // If we are using local docker host then we won't substitute names so skip this step.
        if (!USE_ARTIFACTORY_NAME_SUBSTITUTION)
            return;

        generateDockerConfig(ArtifactoryImageNameSubstitutor.getPrivateRegistry(), ArtifactoryImageNameSubstitutor.getPrivateRegistryAuthToken());
    }

    @Override
    public TransportConfig getTransportConfig() throws InvalidConfigurationException {
        if (transportConfig != null)
            return transportConfig;

        try {
            ExternalTestService.getService("docker-engine", new AvailableDockerHostFilter());
        } catch (Exception e) {
            Log.error(c, "test", e, "Unable to locate any healthy docker-engine instances");
            throw new InvalidConfigurationException("Unable to locate any healthy docker-engine instances", e);
        }
        return transportConfig = TransportConfig.builder() //
                        .dockerHost(config.getDockerHost()) //
                        .sslConfig(config.getSSLConfig()) //
                        .build();
    }

    private class AvailableDockerHostFilter implements ExternalTestServiceFilter {
        private static final int THIRTY_SECONDS = 30000;

        @Override
        public boolean isMatched(ExternalTestService dockerService) {
            String m = "isMatched";
            String dockerHostURL = "tcp://" + dockerService.getAddress() + ":" + dockerService.getPort();
            Log.info(c, m, "Checking if Docker host " + dockerHostURL + " is available and healthy...");

            if (USE_DOCKER_HOST != null && !dockerHostURL.contains(USE_DOCKER_HOST)) {
                Log.info(c, m, "Will not select " + dockerHostURL + " because " + USE_DOCKER_HOST + " was specifically requested.");
                return false;
            }

            if (serviceFilter != null && !serviceFilter.test(dockerService)) {
                Log.info(c, m, "Will not select " + dockerHostURL + " because custom service filter returned 'false'");
                return false;
            }

            String ca = dockerService.getProperties().get("ca.pem");
            String cert = dockerService.getProperties().get("cert.pem");
            String key = dockerService.getProperties().get("key.pem");

            if (ca == null || cert == null || key == null) {
                Log.info(c, m, "Will not select " + dockerHostURL
                               + " because dockerService did not contain one or more of the authentication properties:"
                               + " [ca.pem, cert.pem, key.pem].");
                return false;
            }

            File certDir = new File("docker-certificates");
            certDir.mkdirs();
            writeFile(new File(certDir, "ca.pem"), ca);
            writeFile(new File(certDir, "cert.pem"), cert);
            writeFile(new File(certDir, "key.pem"), key);

            System.setProperty("DOCKER_HOST", dockerHostURL);
            System.setProperty("DOCKER_TLS_VERIFY", "1");
            System.setProperty("DOCKER_CERT_PATH", certDir.getAbsolutePath());

            try {
                test();
            } catch (InvalidConfigurationException e) {
                Log.error(c, m, e, "ExternalService " + dockerService.getAddress() + ':' + dockerService.getPort() + " with props=" +
                                   dockerService.getProperties() + " failed with " + getDescription());
                throw e;
            }
            Log.info(c, m, "Docker host " + dockerHostURL + " is healthy.");

            // Provide information on how to manually connect to the machine if running locally
            if (FATRunner.FAT_TEST_LOCALRUN) {
                Log.info(c, m, "If you need to connect to any currently running docker containers manually, export the following environment variables in your terminal:\n" +
                               "export DOCKER_HOST=" + dockerHostURL + "\n" +
                               "export DOCKER_TLS_VERIFY=1\n" +
                               "export DOCKER_CERT_PATH=" + certDir.getAbsolutePath());
            }
            return true;
        }

        public void test() throws InvalidConfigurationException {
            final String m = "test";

            config = DefaultDockerClientConfig.createDefaultConfigBuilder() //
                            .withRegistryUsername(null) //
                            .withDockerHost(System.getProperty("DOCKER_HOST")) //
                            .withDockerTlsVerify(System.getProperty("DOCKER_TLS_VERIFY")) //
                            .withDockerCertPath(System.getProperty("DOCKER_CERT_PATH")) //
                            .build();

            try {
                String dockerHost = config.getDockerHost().toASCIIString().replace("tcp://", "https://");
                Log.info(c, m, "Pinging URL: " + dockerHost);
                SocketFactory sslSf = config.getSSLConfig().getSSLContext().getSocketFactory();
                String resp = new HttpsRequest(dockerHost + "/_ping") //
                                .timeout(THIRTY_SECONDS)
                                .sslSocketFactory(sslSf) //
                                .run(String.class);
                Log.info(c, m, "Ping successful. Response: " + resp);
                return;
            } catch (InvalidConfigurationException e) {
                throw e;
            } catch (Throwable t) {
                throw new InvalidConfigurationException("Ping failed", t);
            }
        }
    }

    private static void writeFile(File outFile, String content) {
        try {
            Files.deleteIfExists(outFile.toPath());
            Files.write(outFile.toPath(), content.getBytes());
        } catch (IOException e) {
            Log.error(c, "writeFile", e);
            throw new RuntimeException(e);
        }
        Log.info(c, "writeFile", "Wrote property to: " + outFile.getAbsolutePath());
    }

    @Override
    public String getDescription() {
        return "Uses a local Docker install or a remote docker-host service via ExternalTestService.";
    }

    @Override
    protected int getPriority() {
        return USE_REMOTE_DOCKER_HOST ? 900 : 0;
    }

    @Override
    protected boolean isPersistable() {
        return true;
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
            if (USE_DOCKER_HOST != null) {
                result = true;
                reason = "fat.test.docker.host set to " + USE_DOCKER_HOST;
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
            // ARM architecture can cause performance/starting issues with x86 containers, so also
            // assume remote as the default.
            if (System.getProperty("os.name", "unknown").toLowerCase().contains("windows")) {
                result = true;
                reason = "Local operating system is Windows. Default container support not guaranteed.";
                break;
            }
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
                        "Running against remote docker host. Reason: " + reason : //
                        "Running against local docker host. Reason: " + reason;

        Log.info(c, "useRemoteDocker", reason);
        return result;
    }

    /**
     * Determines if we are going to use the Artifactory name substitutor.
     *
     * Priority:
     * 1. System Property: fat.test.use.artifactory.substitution
     *
     * default (USE_REMOTE_DOCKER_HOST)
     *
     * NOTE: There are situations were we will still decide NOT to apply the
     * substitution for synthetic images, and programmatically committed images.
     * This will be determined on an image-to-image basis
     *
     * @see    ArtifactoryImageNameSubstitutor#apply(org.testcontainers.utility.DockerImageName)
     *
     * @return true, we are using the substitutor, false otherwise.
     */
    private static boolean useArtifactorySubstitutor() {
        boolean result;
        String reason;

        do {
            //State 1: fat.test.use.artifactory.substitution should always be honored first
            if (System.getProperty("fat.test.use.artifactory.substitution") != null) {
                result = Boolean.getBoolean("fat.test.use.artifactory.substitution");
                reason = "fat.test.use.artifactory.substitution set to " + result;

                if (result == false && USE_REMOTE_DOCKER_HOST == true) {
                    Log.warning(c, reason + System.lineSeparator()
                                   + "Based on a priority system we decided to use a remote docker host for testing. "
                                   + "Therefore, we cannot honor the request to NOT use artifactory. "
                                   + "To resolve this issue either remove the fat.test.use.artifactory.substitution property, "
                                   + "or force this test to use the a local docker host using fat.test.use.remote.docker=false.");

                    throw new IllegalStateException("Cannot set fat.test.use.artifactory.substitution to false when USE_REMOTE_DOCKER_HOST=true. See logs for more details.");
                }

                break;
            }

            // Default, use USE_REMOTE_DOCKER_HOST to determine if we use substitution
            result = USE_REMOTE_DOCKER_HOST;
            reason = "USE_REMOTE_DOCKER_HOST set to " + USE_REMOTE_DOCKER_HOST;
        } while (false);

        reason = result ? //
                        "Using Artifactory Substitution for docker images. Reason: " + reason : //
                        "Using Docker default repository for docker images. Reason: " + reason;

        Log.info(c, "useArtifactorySubstitutor", reason);
        return result;
    }
}
