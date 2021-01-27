/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    public static Predicate<ExternalTestService> serviceFilter = null;

    /**
     * Used to specify a particular docker host machine to run with. For example: -Dfat.test.docker.host=some-docker-host.mycompany.com
     */
    private static final String USE_DOCKER_HOST = System.getProperty("fat.test.docker.host");
    private static final boolean USE_REMOTE_DOCKER = Boolean.getBoolean("fat.test.use.remote.docker") || USE_DOCKER_HOST != null;

    private DefaultDockerClientConfig config;
    private TransportConfig transportConfig;

    private static boolean setupComplete = false;

    /**
     * <pre>
     * By default, Testcontainrs will cache the DockerClient strategy in <code>~/.testcontainers.properties</code>.
     *
     * Calling this method in the FATSuite class is REQUIRED for any fat project that uses testconatiners.
     * This is a safety measure to ensure that we run with the correct docker.client.stategy property
     * for each FATSuite run.
     *
     * Example Useage:
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
        if (setupComplete)
            return;
        generateTestcontainersConfig();
        generateDockerConfig();
        setupComplete = true;
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
    private static void generateDockerConfig() {
        final String m = "generateDockerConfig";
        if (!useRemoteDocker())
            return;

        File configDir = new File(System.getProperty("user.home"), ".docker");
        File configFile = new File(configDir, "config.json");
        String contents = "";

        String privateAuth = "\t\t\"" + ArtifactoryImageNameSubstitutor.getPrivateRegistry() + "\": {\n" +
                             "\t\t\t\"auth\": \"" + ArtifactoryImageNameSubstitutor.getPrivateRegistryAuthToken() + "\",\n"
                             + "\t\t\t\"email\": null\n" + "\t\t}";
        if (configFile.exists()) {
            Log.info(c, m, "Config already exists at: " + configFile.getAbsolutePath());
            try {
                for (String line : Files.readAllLines(configFile.toPath()))
                    contents += line + '\n';
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Log.info(c, m, "Original contents:\n" + contents);
            if (contents.contains(ArtifactoryImageNameSubstitutor.getPrivateRegistry())) {
                Log.info(c, m, "Config already contains private registry");
                return;
            }
            int authIndex = contents.indexOf("\"auths\"");
            if (authIndex >= 0) {
                Log.info(c, m, "Other auths exist. Need to add private registry");
                int splitAt = contents.indexOf('{', authIndex);
                String firstHalf = contents.substring(0, splitAt + 1);
                String secondHalf = contents.substring(splitAt + 1);
                contents = firstHalf + '\n' + privateAuth + ",\n" + secondHalf;
            } else {
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
        Log.info(c, m, "New config.json contents are:\n" + contents);
        configFile.delete();
        writeFile(configFile, contents);
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
        return transportConfig = TransportConfig.builder()
                        .dockerHost(config.getDockerHost())
                        .sslConfig(config.getSSLConfig())
                        .build();
    }

    private class AvailableDockerHostFilter implements ExternalTestServiceFilter {
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

            System.setProperty("DOCKER_HOST", dockerHostURL);
            File certDir = new File("docker-certificates");
            certDir.mkdirs();
            writeFile(new File(certDir, "ca.pem"), dockerService.getProperties().get("ca.pem"));
            writeFile(new File(certDir, "cert.pem"), dockerService.getProperties().get("cert.pem"));
            writeFile(new File(certDir, "key.pem"), dockerService.getProperties().get("key.pem"));
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
                Log.info(c, m, "If you need to connect to any currently running docker containers manaully, export the following environment variables in your terminal:\n" +
                               "export DOCKER_HOST=" + dockerHostURL + "\n" +
                               "export DOCKER_TLS_VERIFY=1\n" +
                               "export DOCKER_CERT_PATH=" + certDir.getAbsolutePath());
            }
            return true;
        }

        public void test() throws InvalidConfigurationException {
            final String m = "test";
            final int maxAttempts = FATRunner.FAT_TEST_LOCALRUN ? 1 : 4; // attempt up to 4 times for remote builds

            config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                            .withRegistryUsername(null)
                            .withDockerHost(System.getProperty("DOCKER_HOST"))
                            .withDockerTlsVerify(System.getProperty("DOCKER_TLS_VERIFY"))
                            .withDockerCertPath(System.getProperty("DOCKER_CERT_PATH"))
                            .build();

            Throwable firstIssue = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    Log.info(c, m, "Attempting to ping docker daemon. Attempt [" + attempt + "]");
                    String dockerHost = config.getDockerHost().toASCIIString().replace("tcp://", "https://");
                    Log.info(c, m, "  Pinging URL: " + dockerHost);
                    SocketFactory sslSf = config.getSSLConfig().getSSLContext().getSocketFactory();
                    String resp = new HttpsRequest(dockerHost + "/_ping")
                                    .sslSocketFactory(sslSf)
                                    .run(String.class);
                    Log.info(c, m, "  Ping successful. Response: " + resp);
                    return;
                } catch (Throwable t) {
                    Log.error(c, m, t, "  Ping failed.");
                    if (firstIssue == null)
                        firstIssue = t;
                    if (attempt < maxAttempts) {
                        int sleepForSec = 15;
                        Log.info(c, m, "Waiting " + sleepForSec + " seconds before attempting again");
                        try {
                            Thread.sleep(sleepForSec * 1000);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
            if (firstIssue instanceof InvalidConfigurationException)
                throw (InvalidConfigurationException) firstIssue;
            else
                throw new InvalidConfigurationException("Ping failed", firstIssue);
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
        return useRemoteDocker() ? 900 : 0;
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

    public static boolean useRemoteDocker() {
        if (USE_REMOTE_DOCKER) {
            return true; // remote docker explicitly requested
        }
        if (Boolean.parseBoolean(System.getenv("GITHUB_ACTIONS"))) {
            return false; // always use local docker for GH Actions
        }
        // Otherwise, use local docker for local runs, and remote docker for remote (RTC) runs
        return !FATRunner.FAT_TEST_LOCALRUN;
    }

}
