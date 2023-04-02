/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.jcache.internal.fat.docker;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.time.Duration;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.SimpleLogConsumer;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.jcache.internal.fat.HttpUtils;

/**
 * A {@link Testcontainers} implementation for the Keycloak IDP.
 */
public class KeycloakContainer extends GenericContainer<KeycloakContainer> {
    private Class<?> CLASS = KeycloakContainer.class;

    private static final String IMAGE_NAME = "jboss/keycloak";
    private static final String IMAGE_VERSION = "16.1.1";

    private static final int HTTP_PORT = 8080;
    private static final int HTTPS_PORT = 8443;

    /*
     * Defined realms for the Keycloak IDP. The default realm is a restricted word,
     * so we create a test realm to use for tests. This prevents the word from
     * needing to be used anywhere else other than this class.
     *
     * If future versions of Keycloak come pre-configured with a suitable realm
     * name we can simply use that realm.
     */
    static final String DEFAULT_REALM = "master";
    public static final String TEST_REALM = "TestRealm";

    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_PASS = "password";

    private KeycloakAdmin keycloakAdmin;

    /**
     * Construct a new {@link KeycloakContainer}.
     */
    public KeycloakContainer() {
        this(IMAGE_NAME + ":" + IMAGE_VERSION);
    }

    /**
     * Construct a new {@link InfinispanContainer}.
     *
     * @param imageName The name of the image.
     */
    public KeycloakContainer(final String imageName) {
        super(imageName);

        /*
         * Configure some environment variables to set the administrative user and force
         * usage of the embedded H2 database. On some systems, the container tries to use
         * a different database, and it will fail to start.
         */
        withEnv("KEYCLOAK_USER", ADMIN_USER);
        withEnv("KEYCLOAK_PASSWORD", ADMIN_PASS);
        withEnv("DB_VENDOR", "h2");

        /*
         * Consume logs from the container and expose the HTTP ports.
         */
        withLogConsumer(new SimpleLogConsumer(CLASS, "KEYCLOAK"));
        withExposedPorts(HTTPS_PORT, HTTP_PORT);

        /*
         * Wait to finish startup until we get HTTP responses of 200 for the root context.
         */
        WaitAllStrategy strategy = new WaitAllStrategy(WaitAllStrategy.Mode.WITH_OUTER_TIMEOUT);
        strategy.withStartupTimeout(Duration.ofMinutes(2));
        strategy.withStrategy(Wait.forListeningPort());
        strategy.withStrategy(Wait.forHttp("/").forPort(HTTP_PORT).forStatusCode(200));
        waitingFor(strategy);

        /*
         * Finally, create the administrative API instance.
         */
        keycloakAdmin = new KeycloakAdmin(this);
    }

    @Override
    public void start() {
        Log.info(CLASS, "start", "Starting " + CLASS.getName() + " testcontainer...");
        try {
            super.start();
            Log.info(CLASS, "start", CLASS.getName() + " testcontainer started.");
        } catch (RuntimeException e) {
            Log.error(CLASS, "start", e, CLASS.getName() + " testcontainer failed to start.");
            throw e;
        }

        /*
         * Create the test realm.
         *
         * The default realm the Keycloak docker image ships configured with is a restricted word,
         * so to avoid it proliferating across our source and configuration, create a new realm here.
         *
         * We can still use the same admin user from the default realm.
         */
        long end = System.currentTimeMillis() + 60000; // Retry for 1 minute.
        while (true) {
            try {
                keycloakAdmin.createRealm(TEST_REALM);
                break;
            } catch (Exception e) {
                if (System.currentTimeMillis() > end) {
                    Log.error(CLASS, "start", e, CLASS.getName() + " failed to create realm " + TEST_REALM + " .");
                    throw new RuntimeException("Failed to create realm " + TEST_REALM, e);
                } else {
                    Log.error(CLASS, "start", e, CLASS.getName() + " failed to create realm " + TEST_REALM + " . Retrying in 5 seconds...");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        // Ignore.
                    }
                }
            }
        }
    }

    /**
     * Get the remote HTTPS port on the container.
     *
     * @return the remote HTTPS port.
     */
    public Integer getRemoteHttpPort() {
        return getMappedPort(HTTP_PORT);
    }

    /**
     * Get the remote HTTPS port on the container.
     *
     * @return the remote HTTPS port.
     */
    public Integer getRemoteHttpsPort() {
        return getMappedPort(HTTPS_PORT);
    }

    /**
     * Get the {@link KeycloakAdmin} instance to make admin API request to the Keycloak server.
     *
     * @return The {@link KeycloakAdmin} instance.
     */
    public KeycloakAdmin getKeycloakAdmin() {
        return keycloakAdmin;
    }

    /**
     * Get the root REST HTTP endpoint.
     *
     * @return The root REST HTTP endpoint.
     */
    public String getRootHttpEndpoint() {
        return "http://" + getHost() + ":" + getRemoteHttpPort() + "/auth";
    }

    /**
     * Get the root REST HTTPS endpoint.
     *
     * @return The root REST HTTPS endpoint.
     */
    public String getRootHttpsEndpoint() {
        return "https://" + getHost() + ":" + getRemoteHttpsPort() + "/auth";
    }

    /**
     * Download and install the Keycloak OpenId configuration into the server's resources/security directory.
     *
     * @param realm  The keycloak realm.
     * @param server The Liberty server to copy the file to.
     * @throws Exception If there was an issue downloading or installing the configuration file from Keycloak.
     */
    public void downloadOpenidConfiguration(String realm, LibertyServer server) throws Exception {
        /*
         * Create the request.
         */
        HttpGet request = new HttpGet(getRootHttpEndpoint() + "/realms/" + realm + "/.well-known/openid-configuration");

        /*
         * Create an HTTP client and execute the request.
         */
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                    throw new Exception("Unable to retrieve the Openid configuration for the realm. HTTP status code: " + response.getStatusLine().getStatusCode());
                }

                /*
                 * Write the response to the OpenId configuration file.
                 */
                String file = server.getServerRoot() + "/resources/security/" + realm + "-openIdConfiguration.json";
                try (PrintWriter out = new PrintWriter(file)) {
                    out.println(KeycloakUtils.getStringResponse(response));
                }
            }
        }
    }

    /**
     * Download and install the Keycloak SAML IDP metadata descriptor into the server's resources/security directory.
     *
     * @param realm   The keycloak realm.
     * @param servers The Liberty servers to copy the file to.
     * @throws Exception If there was an issue downloading or installing the metadata file from Keycloak.
     */
    public void downloadSamlDescriptor(String realm, LibertyServer... servers) throws Exception {
        final String methodName = "downloadSamlDescriptor";

        /*
         * Create the request.
         */
        HttpGet request = new HttpGet(getRootHttpsEndpoint() + "/realms/" + realm + "/protocol/saml/descriptor");

        /*
         * Create an HTTP client and execute the request.
         */
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                    throw new Exception("Unable to retrieve the SAML descriptor for the realm. HTTP status code: " + response.getStatusLine().getStatusCode());
                }

                /*
                 * Write the response to the metadata file in the server.
                 */
                String content = KeycloakUtils.getStringResponse(response);
                Log.info(CLASS, methodName, "SAML descriptor file content: \n\n" + content);
                for (LibertyServer server : servers) {
                    String file = server.getServerRoot() + "/resources/security/" + realm + "-samlIdpMetadata.xml";
                    Log.info(CLASS, methodName, "Writing SAML descriptor file to " + file);
                    try (PrintWriter out = new PrintWriter(file)) {
                        out.println(content);
                    }
                }
            }
        }
    }

    /**
     * Create a trust store from the Keycloak certificate and copy it to <code>/resources/security/truststore.p12</code>
     * for each of the servers.
     *
     * @param password The password for the trust store.
     * @param servers  The servers to copy to.
     * @throws Exception If there was an error creating the keystore(s).
     */
    public void createTrustFromKeycloak(String password, LibertyServer... servers) throws Exception {
        /*
         * Get the TLS certificate for the Keycloak server and copy to the Liberty servers.
         */
        try {
            Certificate[] certs = HttpUtils.getServerCertificates(getRootHttpsEndpoint());

            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            ks.setEntry("keycloak", new KeyStore.TrustedCertificateEntry(certs[0]), null);

            for (LibertyServer server : servers) {
                ks.store(new FileOutputStream(server.getServerRoot() + "/resources/security/truststore.p12"), password.toCharArray());
            }

        } catch (Exception e) {
            throw new Exception("Failed to generate truststore with Keycloak certificate.", e);
        }
    }
}
