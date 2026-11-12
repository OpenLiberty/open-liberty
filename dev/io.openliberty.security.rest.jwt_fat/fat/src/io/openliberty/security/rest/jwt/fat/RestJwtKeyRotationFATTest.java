/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.security.rest.jwt.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Key rotation: the public key file named by the jwkEndpointUrl of the JMXConnectorRESTConsumer is replaced
 * with the public key of a new key pair while the server is running.
 *
 * The jwtConsumer keeps the key it read from the file until its configuration is reloaded, so overwriting
 * the file is not enough on its own. Each rotation in this test replaces the file and then changes the
 * rotation.clockSkew variable in server.xml, which reloads the jwtConsumer. After the reload, tokens signed with
 * the new private key must be accepted and tokens signed with the previous private key must be rejected,
 * without restarting the server.
 *
 * Every request uses a token with a new jti claim because the jwtConsumer accepts a given jti only once.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@SkipForRepeat({ SkipForRepeat.EE9_OR_LATER_FEATURES })
public class RestJwtKeyRotationFATTest {

    private static final Class<?> c = RestJwtKeyRotationFATTest.class;

    private static final String ISSUER = "IBMJMXConnectorRESTIssuer";
    private static final String AUDIENCE = "IBMJMXConnectorREST";

    private static final String ROTATION_VARIABLE = "rotation.clockSkew";
    private static final String CLOCK_SKEW_A = "60s";
    private static final String CLOCK_SKEW_B = "61s";

    @Server("RestJwtKeyRotationServer")
    public static LibertyServer server;

    private static KeyPairGenerator generator;
    private static File keyFile;
    private static boolean useClockSkewB;

    // The private key whose public key is currently in the key file
    private static PrivateKey currentKey;

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";

        // Generate the key pairs at run time so that no private key is checked in.
        generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        // Must be the file named by jwkEndpointUrl in the RestJwtKeyRotationServer server.xml
        keyFile = new File(server.getServerRoot() + "/resources/security/jwtPublic.pem");
        keyFile.getParentFile().mkdirs();
        currentKey = writeNewKeyPair();

        Log.info(c, methodName, "Starting RestJwtKeyRotationServer with the jwkEndpointUrl file " + keyFile.getAbsolutePath());
        server.startServer();
        assertNotNull("IBMJMXConnectorREST application was not ready",
                      server.waitForStringInLog("SRVE0242I.*IBMJMXConnectorREST"));
        assertNotNull("Default HTTPS endpoint did not start", server.waitForDefaultHTTPEndpointSSLStart());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer();
        } finally {
            if (keyFile != null) {
                keyFile.delete();
            }
        }
    }

    /**
     * Each test starts from a freshly rotated key, so that the tests do not depend on their order.
     */
    @Before
    public void startWithNewKey() throws Exception {
        rotateKey();
    }

    /**
     * After the rotation the token signed with the new private key is accepted and the token signed with the
     * previous private key is rejected.
     */
    @Test
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void testRotatedKey_NewKeyAccepted_OldKeyRejected() throws Exception {
        PrivateKey oldKey = currentKey;
        assertEquals("Expected 200 OK for a token signed with the current key before the rotation",
                     HttpURLConnection.HTTP_OK, sendRequest(createRs256Token(oldKey)).getResponseCode());

        PrivateKey newKey = rotateKey();

        assertEquals("Expected 200 OK for a token signed with the new key after the rotation",
                     HttpURLConnection.HTTP_OK, sendRequest(createRs256Token(newKey)).getResponseCode());
        assertRejected(createRs256Token(oldKey), "a token signed with the previous key");
    }

    /**
     * The key can be rotated more than once. Only the key that is in the file at the moment of the request is
     * trusted, so neither of the two earlier keys is accepted any more.
     */
    @Test
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void testRotatedTwice_OnlyLatestKeyAccepted() throws Exception {
        PrivateKey firstKey = currentKey;
        PrivateKey secondKey = rotateKey();
        PrivateKey thirdKey = rotateKey();

        assertEquals("Expected 200 OK for a token signed with the latest key",
                     HttpURLConnection.HTTP_OK, sendRequest(createRs256Token(thirdKey)).getResponseCode());
        assertRejected(createRs256Token(secondKey), "a token signed with the key before the latest");
        assertRejected(createRs256Token(firstKey), "a token signed with the oldest key");
    }

    /**
     * A rejected token is a 401 because of the signature, not because the server is unable to
     * authenticate anyone: a token signed with the current key still works afterwards.
     */
    @Test
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void testRotatedKey_RejectedTokenDoesNotAffectValidTokens() throws Exception {
        PrivateKey oldKey = currentKey;
        PrivateKey newKey = rotateKey();

        assertRejected(createRs256Token(oldKey), "a token signed with the previous key");
        assertEquals("Expected 200 OK for a token signed with the new key after a rejected request",
                     HttpURLConnection.HTTP_OK, sendRequest(createRs256Token(newKey)).getResponseCode());
    }

    /**
     * Replaces the public key in the file with the public key of a new key pair and reloads the jwtConsumer.
     *
     * @return the private key that matches the public key now in the file
     */
    private static PrivateKey rotateKey() throws Exception {
        currentKey = writeNewKeyPair();
        reloadJwtConsumer();
        return currentKey;
    }

    private static PrivateKey writeNewKeyPair() throws Exception {
        KeyPair pair = generator.generateKeyPair();
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                     + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(pair.getPublic().getEncoded())
                     + "\n-----END PUBLIC KEY-----\n";
        Files.write(keyFile.toPath(), pem.getBytes(StandardCharsets.US_ASCII));
        return pair.getPrivate();
    }

    /**
     * Changes the variable that the clockSkew of the jwtConsumer refers to, which makes the server reload the
     * jwtConsumer and drop the public key that it cached from the key file.
     */
    private static void reloadJwtConsumer() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Variable variable = config.getVariables().getBy("name", ROTATION_VARIABLE);
        assertNotNull("The " + ROTATION_VARIABLE + " variable is missing from the server.xml", variable);
        useClockSkewB = !useClockSkewB;
        variable.setValue(useClockSkewB ? CLOCK_SKEW_B : CLOCK_SKEW_A);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        assertFalse("The server configuration update was not completed",
                    server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet()).isEmpty());
    }

    private static void assertRejected(String token, String description) throws Exception {
        server.setMarkToEndOfLog();
        assertEquals("Expected 401 Unauthorized for " + description,
                     HttpURLConnection.HTTP_UNAUTHORIZED, sendRequest(token).getResponseCode());
        assertNotNull("RestJwtTAI did not report an error validating " + description,
                      server.waitForStringInTraceUsingMark("Error validating JWT token"));
    }

    private static String createRs256Token(PrivateKey key) throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER);
        claims.setAudience(AUDIENCE);
        claims.setSubject("adminUser");
        claims.setStringListClaim("groups", Collections.singletonList("liberty-admin"));
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setIssuedAtToNow();
        claims.setGeneratedJwtId();

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        jws.setKey(key);
        return jws.getCompactSerialization();
    }

    private static HttpURLConnection sendRequest(String bearerToken) throws Exception {
        URL url = new URL("https", server.getHostname(), server.getHttpSecondarySecurePort(), FATSuite.REST_MBEANS_ENDPOINT);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        } }, new java.security.SecureRandom());
        conn.setSSLSocketFactory(sc.getSocketFactory());
        conn.setHostnameVerifier((hostname, session) -> true);
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        conn.connect();
        return conn;
    }
}
