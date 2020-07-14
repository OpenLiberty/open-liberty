/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import javax.net.SocketFactory;

import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.InvalidConfigurationException;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;

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
    private static final boolean USE_REMOTE_DOCKER = Boolean.getBoolean("fat.test.use.remote.docker");

    /**
     * Used to specify a particular docker host machine to run with. For example: -Dfat.test.docker.host=some-docker-host.mycompany.com
     */
    private static final String USE_DOCKER_HOST = System.getProperty("fat.test.docker.host");

    /**
     * By default, Testcontainrs will cache the DockerClient strategy in <code>~/.testcontainers.properties</code>.
     * It is not necessary to call this method whenever Testcontainers is used, but if you want to be able to
     * automatically switch between using your local Docker install, or a remote Docker host, call this method
     * in FATSuite beforeClass setup.
     */
    public static void clearTestcontainersConfig() {
        File testcontainersConfigFile = new File(System.getProperty("user.home"), ".testcontainers.properties");
        if (!testcontainersConfigFile.exists())
            return;

        Log.info(c, "clearTestcontainersConfig", "Resetting testcontainers property file at: " + testcontainersConfigFile.getAbsolutePath());
        try {
            Properties tcProps = new Properties();
            tcProps.load(new FileInputStream(testcontainersConfigFile));
            tcProps.remove("docker.client.strategy");
            Files.deleteIfExists(testcontainersConfigFile.toPath());
            tcProps.store(new FileOutputStream(testcontainersConfigFile), "Modified by FAT framework");
        } catch (IOException e) {
            Log.error(c, "clearTestcontainersConfig", e);
        }
    }

    @Override
    public void test() throws InvalidConfigurationException {
        try {
            ExternalTestService.getService("docker-engine", new AvailableDockerHostFilter());
        } catch (Exception e) {
            Log.error(c, "test", e, "Unable to locate any healthy docker-engine instances");
            throw new InvalidConfigurationException("Unable to locate any healthy docker-engine instances", e);
        }
    }

    private class AvailableDockerHostFilter implements ExternalTestServiceFilter {
        @Override
        public boolean isMatched(ExternalTestService dockerService) {
            String m = "tryDockerHost";
            String dockerHostURL = "tcp://" + dockerService.getAddress() + ":" + dockerService.getPort();
            Log.info(c, m, "Checking if Docker host " + dockerHostURL + " is available and healthy...");

            if (USE_DOCKER_HOST != null && !dockerHostURL.contains(USE_DOCKER_HOST)) {
                Log.info(c, m, "Will not select " + dockerHostURL + " because " + USE_DOCKER_HOST + " was specifically requested.");
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
                Log.info(c, m, "If you need to connect to any currently running docker containers manaully, export the following environment variables in your terminal:");
                Log.info(c, m, "export DOCKER_HOST=" + dockerHostURL);
                Log.info(c, m, "export DOCKER_TLS_VERIFY=1");
                Log.info(c, m, "export DOCKER_CERT_PATH=" + certDir.getAbsolutePath());
            }
            return true;
        }

        public void test() throws InvalidConfigurationException {
            final String m = "test";
            final int maxAttempts = FATRunner.FAT_TEST_LOCALRUN ? 1 : 4; // attempt up to 4 times for remote builds
            config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            client = getClientForConfig(config);
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
                    // Using the Testcontainers API directly causes intermittent failures with ping
                    // Instead of using their mechanism, attempt to use a manually constructed ping
                    // try (PingCmd ping = client.pingCmd()) {
                    //     ping.exec();
                    // }
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

    private static boolean useRemoteDocker() {
        return !FATRunner.FAT_TEST_LOCALRUN || // this is a remote run
               USE_REMOTE_DOCKER || // or if remote docker hosts are specifically requested
               USE_DOCKER_HOST != null; // or if a specific docker host machine was requested
    }

}
