/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;

import javax.net.SocketFactory;

import org.testcontainers.dockerclient.InvalidConfigurationException;
import org.testcontainers.shaded.com.github.dockerjava.core.DefaultDockerClientConfig;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.ExternalTestService;
import componenttest.topology.utils.ExternalTestServiceFilter;
import componenttest.topology.utils.HttpsRequest;

/**
 * This class tests and filters docker host services so that we can find
 * a valid docker host as quickly as possible
 */
public class ExternalDockerClientFilter implements ExternalTestServiceFilter {

    private static final Class<?> c = ExternalDockerClientFilter.class;

    /**
     * Used to specify a particular docker host machine to run with. For example: -Dfat.test.docker.host=some-docker-host.mycompany.com
     * Helpful when a developer wants to have repeatable behavior, or is remotely connected to a specific docker host.
     */
    private static final String FORCE_DOCKER_HOST = System.getProperty("fat.test.docker.host");
    private static final String CHECK_PORT_AVAILABLE = System.getProperty("fat.test.docker.host.port");

    private boolean valid;
    private String host;
    private String hostname;
    private String verify;
    private String certPath;

    //Singleton class
    private static ExternalDockerClientFilter instance;

    private ExternalDockerClientFilter() {}

    public static ExternalDockerClientFilter instance() {
        if (instance == null) {
            instance = new ExternalDockerClientFilter();
        }
        return instance;
    }

    public boolean isForced() {
        return FORCE_DOCKER_HOST != null;
    }

    public boolean checkPortAvailability() {
        try {
            return CHECK_PORT_AVAILABLE != null && Integer.valueOf(CHECK_PORT_AVAILABLE) >= 0;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    /**
     * Determines if this docker host is healthy.
     *
     * Note: Testcontainers will re-test this host using their own API.
     * At that time if testcontainers cannot complete testing we may
     * request a new docker host from this filter.
     */
    @Override
    public boolean isMatched(ExternalTestService dockerService) {
        String m = "isMatched";
        String dockerHostURL = "tcp://" + dockerService.getAddress() + ":" + dockerService.getPort();
        Log.info(c, m, "Checking if Docker host " + dockerHostURL + " is available and healthy...");

        if (isForced() && !dockerHostURL.contains(FORCE_DOCKER_HOST)) {
            Log.info(c, m, "Will not select " + dockerHostURL + " because " + FORCE_DOCKER_HOST + " was specifically requested.");
            return false;
        }

        if (checkPortAvailability()) {
            if (!isPortAvailable(dockerService.getAddress(), Integer.valueOf(CHECK_PORT_AVAILABLE))) {
                Log.info(c, m, "Will not select " + dockerHostURL + " because port " + CHECK_PORT_AVAILABLE + " was not available.");
                return false;
            }
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

        hostname = dockerService.getHostname();
        host = dockerHostURL;
        verify = "1";
        certPath = certDir.getAbsolutePath();

        try {
            test();
        } catch (InvalidConfigurationException e) {
            Log.error(c, m, e, "ExternalService " + dockerService.getAddress() + ':' + dockerService.getPort() + " with props=" +
                               dockerService.getProperties() + " failed with " + e.getLocalizedMessage());
            throw e;
        }

        Log.info(c, m, "Docker host " + dockerHostURL + " is healthy.");

        // Provide information on how to manually connect to the machine if running locally
        if (FATRunner.FAT_TEST_LOCALRUN) {
            Log.info(c, m, "If you need to connect to any currently running docker containers manually, export the following environment variables in your terminal:\n" +
                           "export DOCKER_HOST=" + host + "\n" +
                           "export DOCKER_TLS_VERIFY=" + verify + "\n" +
                           "export DOCKER_CERT_PATH=" + certPath);
        }
        return valid = true;
    }

    /**
     * isPortAvailable() is used to determine if the docker service has a specific host port available.
     * This is used as filter criteria for tests that need a specific host port available.
     *
     * @param  host The host name or address to check
     * @param  port The port on the host to test
     * @return      True if the host does not accept a socket connection on the given port, false otherwise.
     */
    private boolean isPortAvailable(String host, int port) {
        String prefix = "tcp://";
        if (host.startsWith(prefix)) {
            host = host.substring(prefix.length());
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            return false;
        } catch (IOException ex) {
            // An exception here means the host wasn't listening on the port specified. This means the port should be available
            // for the client to attach once it's started.
            Log.info(c, "isPortAvailable", "Port " + port + " is available on host " + host);
        }
        return true;

    }

    /**
     * Fail fast here! Never repeat this test!
     *
     * You might be tempted to add a repeat here when our Fyre systems are being flaky,
     * but the point of this test is to filter out bad systems fast and go onto the next.
     *
     * @throws InvalidConfigurationException
     */
    private void test() throws InvalidConfigurationException {
        final String m = "test";

        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder() //
                        .withRegistryUsername(null) //
                        .withDockerHost(host) //
                        .withDockerTlsVerify(verify) //
                        .withDockerCertPath(certPath) //
                        .build();

        try {
            String dockerHost = config.getDockerHost().toASCIIString().replace("tcp://", "https://");
            Log.info(c, m, "Pinging URL: " + dockerHost);
            SocketFactory sslSf = config.getSSLConfig().getSSLContext().getSocketFactory();
            String resp = new HttpsRequest(dockerHost + "/_ping") //
                            .timeout(10_000) // 10 seconds
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

    private void writeFile(File outFile, String content) {
        try {
            Files.deleteIfExists(outFile.toPath());
            Files.write(outFile.toPath(), content.getBytes());
        } catch (IOException e) {
            Log.error(c, "writeFile", e);
            throw new RuntimeException(e);
        }
        Log.info(c, "writeFile", "Wrote property to: " + outFile.getAbsolutePath());
    }

    //GETTERS

    public boolean isValid() {
        return valid;
    }

    /*
     * @return - location of remote docker host. Example: tcp://127.0.0.1:65239
     */
    public String getHost() {
        return host;
    }

    public String getHostname() {
        return hostname;
    }

    public String getVerify() {
        return verify;
    }

    public String getCertPath() {
        return certPath;
    }
}
