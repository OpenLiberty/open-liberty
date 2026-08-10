/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.oidc.tests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

import com.ibm.websphere.simplicity.config.OpenidConnectClient;
import com.ibm.websphere.simplicity.config.SSL;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.containers.SimpleLogConsumer;
import componenttest.security.utils.SSLUtils;
import componenttest.topology.impl.LibertyServer;

/**
 * A self-contained Keycloak testcontainer used by both {@link OidcTests} and
 * {@link OidcAuthorizationFlowTests}.
 *
 * <p>Extending {@link org.testcontainers.containers.GenericContainer} lets the container
 * own its own TLS keypair, realm setup, and Liberty server configuration helpers rather
 * than scattering that state across test classes.
 *
 * <p>Both test classes declare {@link #INSTANCE} as a {@code @ClassRule}.
 * Testcontainers is idempotent: starting an already-running container is a no-op, so the
 * container starts once and is stopped after the last class that holds a reference releases it.
 * When either class runs in isolation the container still starts and stops correctly.
 */
@SuppressWarnings("resource")
public class KeycloakContainer extends org.testcontainers.containers.GenericContainer<KeycloakContainer> {

    static final String TAG = "26.6.2";
    private static final String IMAGE = "quay.io/keycloak/keycloak:" + TAG;

    static final String REALM = "mcp-realm";
    static final String LIBERTY_CLIENT_ID = "liberty-mcp-server-conf-client";
    static final String PUBLIC_CLIENT_ID = "mcp-public-client";
    static final String TEST_PASSWORD = "123";

    // TLS / HTTP-client state created once, reused across both test classes
    private KeyStore keycloakKeystore = null;
    private KeyStore keycloakTruststore = null;
    private SSLContext keycloakSslContext = null;
    private HttpClient keycloakHttpClient = null;

    // Realm state populated by setupRealm()
    private String confidentialClientUUID = null;
    private String confidentialClientSecret = null;
    private String publicClientUUID = null;
    private boolean realmConfigured = false;

    // Constructor
    public KeycloakContainer() {
        super(IMAGE);
        withExposedPorts(8080, 8443);
        withStartupAttempts(3);
        withCopyToContainer(buildKeycloakKeystore(), "/keystore.p12");
        withEnv("KEYCLOAK_ADMIN", "admin");
        withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin");
        withCommand("start-dev",
                    "--https-key-store-file=/keystore.p12",
                    "--https-key-store-password=password",
                    "--hostname-strict=false");
        withLogConsumer(new SimpleLogConsumer(KeycloakContainer.class, "keycloak-container"));
        waitingFor(Wait.forLogMessage(".*Listening on:.*", 1)
                       .withStartupTimeout(Duration.ofMinutes(2)));
    }

    /**
     * Returns the base HTTPS URL of this Keycloak instance,
     * e.g. {@code https://localhost:44203}.
     */
    public String getBaseUrl() {
        return "https://" + getHost() + ":" + getMappedPort(8443);
    }

    /**
     * Creates the test realm, clients, users, groups, and protocol mappers inside
     * Keycloak. Idempotent — a second call is a no-op.
     *
     * @throws Exception if any {@code kcadm.sh} command fails
     */
    public synchronized void setupRealm() throws Exception {
        if (realmConfigured) {
            return;
        }

        // Authenticate kcadm against the master realm
        run("/opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 " +
            "--realm master --user admin --password admin");

        // Create application realm
        run("/opt/keycloak/bin/kcadm.sh create realms -s realm=" + REALM + " -s enabled=true");

        // Confidential client (used by Liberty as the resource server)
        confidentialClientUUID = run(
                                     "/opt/keycloak/bin/kcadm.sh create clients -r " + REALM + " " +
                                     "-s clientId=" + LIBERTY_CLIENT_ID + " " +
                                     "-s name=" + LIBERTY_CLIENT_ID + " " +
                                     "-s enabled=true " +
                                     "-s publicClient=false " +
                                     "-s clientAuthenticatorType=client-secret " +
                                     "-s standardFlowEnabled=true " +
                                     "-s directAccessGrantsEnabled=true " +
                                     "-s serviceAccountsEnabled=true " +
                                     "-s 'redirectUris=[\"*\"]' " +
                                     "-s 'webOrigins=[\"*\"]' " +
                                     "-i").trim();

        String secretResponse = run(
                                    "/opt/keycloak/bin/kcadm.sh get clients/" + confidentialClientUUID + "/client-secret -r " + REALM).trim();
        confidentialClientSecret = extractClientSecret(secretResponse);

        // Public client (used by tests to obtain tokens via ROPC)
        publicClientUUID = run(
                               "/opt/keycloak/bin/kcadm.sh create clients -r " + REALM + " " +
                               "-s clientId=" + PUBLIC_CLIENT_ID + " " +
                               "-s name=" + PUBLIC_CLIENT_ID + " " +
                               "-s enabled=true " +
                               "-s publicClient=true " +
                               "-s standardFlowEnabled=true " +
                               "-s directAccessGrantsEnabled=true " +
                               "-s authorizationServicesEnabled=false " +
                               "-s 'redirectUris=[\"*\"]' " +
                               "-s 'webOrigins=[\"*\"]' " +
                               "-i").trim();

        // Regular user
        String regularUserId = run(
                                   "/opt/keycloak/bin/kcadm.sh create users -r " + REALM + " " +
                                   "-s username=user@example.com " +
                                   "-s email=user@example.com " +
                                   "-s firstName=Test " +
                                   "-s lastName=Admin " +
                                   "-s enabled=true " +
                                   "-s emailVerified=true " +
                                   "-s 'requiredActions=[]' " +
                                   "-i").trim();
        setNonTemporaryPassword(regularUserId);

        // Admin user
        String adminUserId = run(
                                 "/opt/keycloak/bin/kcadm.sh create users -r " + REALM + " " +
                                 "-s username=admin@example.com " +
                                 "-s email=admin@example.com " +
                                 "-s firstName=Test " +
                                 "-s lastName=User " +
                                 "-s enabled=true " +
                                 "-s emailVerified=true " +
                                 "-s 'requiredActions=[]' " +
                                 "-i").trim();
        setNonTemporaryPassword(adminUserId);

        // Groups
        String userGroupId = run(
                                 "/opt/keycloak/bin/kcadm.sh create groups -r " + REALM + " -s name=mcp-user -i").trim();
        String adminGroupId = run(
                                  "/opt/keycloak/bin/kcadm.sh create groups -r " + REALM + " -s name=mcp-admin -i").trim();

        // Add regular user → mcp-user
        run("/opt/keycloak/bin/kcadm.sh update users/" + regularUserId + "/groups/" + userGroupId + " " +
            "-r " + REALM + " -s realm=" + REALM + " -s userId=" + regularUserId + " -s groupId=" + userGroupId + " -n");

        // Add admin user → mcp-user, mcp-admin
        run("/opt/keycloak/bin/kcadm.sh update users/" + adminUserId + "/groups/" + userGroupId + " " +
            "-r " + REALM + " -s realm=" + REALM + " -s userId=" + adminUserId + " -s groupId=" + userGroupId + " -n");
        run("/opt/keycloak/bin/kcadm.sh update users/" + adminUserId + "/groups/" + adminGroupId + " " +
            "-r " + REALM + " -s realm=" + REALM + " -s userId=" + adminUserId + " -s groupId=" + adminGroupId + " -n");

        // Group-membership mapper on the public client
        run("/opt/keycloak/bin/kcadm.sh create clients/" + publicClientUUID + "/protocol-mappers/models " +
            "-r " + REALM + " " +
            "-s name=groups " +
            "-s protocol=openid-connect " +
            "-s protocolMapper=oidc-group-membership-mapper " +
            "-s 'config.\"claim.name\"=groups' " +
            "-s 'config.\"full.path\"=false' " +
            "-s 'config.\"id.token.claim\"=true' " +
            "-s 'config.\"access.token.claim\"=true' " +
            "-s 'config.\"userinfo.token.claim\"=true' " +
            "-s 'config.\"introspection.token.claim\"=true'");

        // Audience mapper on the public client
        run("/opt/keycloak/bin/kcadm.sh create clients/" + publicClientUUID + "/protocol-mappers/models " +
            "-r " + REALM + " " +
            "-s name=liberty-mcp-server-audience " +
            "-s protocol=openid-connect " +
            "-s protocolMapper=oidc-audience-mapper " +
            "-s 'config.\"included.client.audience\"=" + LIBERTY_CLIENT_ID + "' " +
            "-s 'config.\"id.token.claim\"=false' " +
            "-s 'config.\"access.token.claim\"=true' " +
            "-s 'config.\"introspection.token.claim\"=true'");

        realmConfigured = true;
    }

    /**
     * Writes the live Keycloak coordinates into the given Liberty server's configuration.
     * Safe to call whether or not the server is running — uses
     * {@link LibertyServer#updateServerConfiguration} which writes directly to disk.
     *
     * @param targetServer the Liberty server to configure
     * @throws Exception if the configuration update fails
     */
    public void updateServerConfig(LibertyServer targetServer) throws Exception {
        // Write the trust store to the server's autoFVT test-files directory
        String trustStoreName = "keycloakTrustStore.p12";
        File file = new File(targetServer.pathToAutoFVTTestFiles + trustStoreName);
        try (FileOutputStream os = new FileOutputStream(file)) {
            getOrCreateTruststore().store(os, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write Keycloak trust store", e);
        }
        targetServer.copyFileToLibertyServerRoot(trustStoreName);

        ServerConfiguration config = targetServer.getServerConfiguration().clone();

        var ks = new com.ibm.websphere.simplicity.config.KeyStore();
        ks.setLocation(trustStoreName);
        ks.setId("keycloak-trust");
        config.getKeyStores().add(ks);

        SSL ssl = config.getSSLById("defaultSSLConfig");
        ssl.setTrustStoreRef("keycloak-trust");

        OpenidConnectClient oidcClient = config.getOpenidConnectClients().get(0);
        oidcClient.setClientId(LIBERTY_CLIENT_ID);
        oidcClient.setClientSecret(confidentialClientSecret);
        oidcClient.setJwkEndpointUrl(getBaseUrl() + "/realms/" + REALM + "/protocol/openid-connect/certs");
        oidcClient.setIssuerIdentifier(getBaseUrl() + "/realms/" + REALM);
        oidcClient.setAudiences(LIBERTY_CLIENT_ID);

        targetServer.updateServerConfiguration(config);
        targetServer.waitForStringInLogUsingMark("CWWKG001[7-8]I");
    }

    /**
     * Returns an {@link HttpClient} configured to trust this Keycloak instance's
     * self-signed TLS certificate.
     */
    public HttpClient getHttpClient() {
        if (keycloakHttpClient != null) {
            return keycloakHttpClient;
        }
        try {
            SSLContext sslContext = getSslContext();
            keycloakHttpClient = HttpClient.newBuilder()
                                           .sslContext(sslContext)
                                           .build();
            return keycloakHttpClient;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Keycloak HTTP client", e);
        }
    }

    // Package-private helpers (used by OidcTests)

    /**
     * Returns the PKCS12 keystore that Keycloak uses for its HTTPS listener,
     * wrapped as a {@link Transferable} for {@link #withCopyToContainer}.
     * Called during construction before the container starts.
     */
    private Transferable buildKeycloakKeystore() {
        createKeyStores();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            keycloakKeystore.store(baos, "password".toCharArray());
            return Transferable.of(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialise Keycloak keystore", e);
        }
    }

    // Private helpers
    private void createKeyStores() {
        if (keycloakKeystore != null && keycloakTruststore != null) {
            return;
        }
        try {
            KeyPair keypair = SSLUtils.generateKeyPair();
            String hostname = DockerClientFactory.instance().dockerHostIpAddress();
            Certificate cert = SSLUtils.selfSign(keypair, "dn=" + hostname, List.of(hostname));

            KeyStore keystore = KeyStore.getInstance("pkcs12");
            keystore.load(null, null);
            keystore.setKeyEntry("key", keypair.getPrivate(), "password".toCharArray(), new Certificate[] { cert });

            KeyStore truststore = KeyStore.getInstance("pkcs12");
            truststore.load(null, null);
            truststore.setCertificateEntry("cert", cert);

            keycloakKeystore = keystore;
            keycloakTruststore = truststore;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Keycloak keystores", e);
        }
    }

    private KeyStore getOrCreateTruststore() {
        createKeyStores();
        return keycloakTruststore;
    }

    private SSLContext getSslContext() throws Exception {
        if (keycloakSslContext != null) {
            return keycloakSslContext;
        }
        createKeyStores();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(keycloakTruststore);
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
        keycloakSslContext = sslContext;
        return keycloakSslContext;
    }

    private void setNonTemporaryPassword(String userId) throws Exception {
        run("/opt/keycloak/bin/kcadm.sh update users/" + userId + "/reset-password " +
            "-r " + REALM + " -s type=password -s value=" + TEST_PASSWORD + " -s temporary=false -n");
        run("/opt/keycloak/bin/kcadm.sh update users/" + userId + " " +
            "-r " + REALM + " -s enabled=true -s emailVerified=true -s 'requiredActions=[]'");
    }

    private String run(String command) throws IOException, InterruptedException {
        Container.ExecResult result = execInContainer("sh", "-c", command);
        if (result.getExitCode() != 0) {
            throw new RuntimeException("Keycloak command failed: " + command +
                                       "\nStdout: " + result.getStdout() +
                                       "\nStderr: " + result.getStderr());
        }
        return result.getStdout();
    }

    private static String extractClientSecret(String response) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"").matcher(response);
        if (!m.find()) {
            throw new RuntimeException("Could not extract Keycloak client secret from: " + response);
        }
        return m.group(1);
    }
}
