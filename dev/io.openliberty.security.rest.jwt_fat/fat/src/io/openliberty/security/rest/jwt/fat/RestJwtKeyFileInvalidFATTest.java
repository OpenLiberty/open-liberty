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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
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

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * The jwkEndpointUrl of the defaultJwtConsumer (file:${server.config.dir}resources/security/jwtPublic.pem
 * in server.xml) points at a file that is missing, unreadable or does not hold a public key. The
 * jwtConsumer cannot get a key, so RestJwtTAI must reject the request.
 *
 * The file URL is fixed for the life of the server, but RestJwtTAI reads the file on
 * every request, so each test changes the state of the file instead of restarting the server. A
 * valid key is never served, so no key can be cached from an earlier request.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class RestJwtKeyFileInvalidFATTest {

    private static final Class<?> c = RestJwtKeyFileInvalidFATTest.class;

    private static final String ISSUER = "IBMJMXConnectorRESTIssuer";
    private static final String AUDIENCE = "IBMJMXConnectorREST";
    private static final String MBEANS_ENDPOINT = "/IBMJMXConnectorREST/mbeans";

    @Server("RestJwtKeyFileServer")
    public static LibertyServer server;

    private static File keyFile;
    private static String token;

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        token = createRs256Token(pair);

        // Must be the file named by jwkEndpointUrl in the RestJwtKeyFileServer server.xml
        keyFile = new File(server.getServerRoot() + "/resources/security/jwtPublic.pem");
        keyFile.getParentFile().mkdirs();
        deleteKeyFile();

        Log.info(c, methodName, "Starting RestJwtKeyFileServer with the jwkEndpointUrl file " + keyFile.getAbsolutePath());
        server.startServer();
        assertNotNull("IBMJMXConnectorREST application was not ready",
                      server.waitForStringInLog("SRVE0242I.*IBMJMXConnectorREST"));
        assertNotNull("Default HTTPS endpoint did not start", server.waitForDefaultHTTPEndpointSSLStart());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("CWWKS6\\d+E", "CWWKS5\\d+E", "CWWKG0\\d+E");
        } finally {
            deleteKeyFile();
        }
    }

    @Before
    public void resetKeyFile() throws Exception {
        deleteKeyFile();
    }

    @Test
    public void testKeyFileMissing_Unauthorized() throws Exception {
        assertEquals("The key file must not exist", false, keyFile.exists());
        assertUnauthorized();
    }

    @Test
    public void testKeyFileUnreadable_Unauthorized() throws Exception {
        Files.write(keyFile.toPath(), "-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----\n".getBytes(StandardCharsets.US_ASCII));
        keyFile.setReadable(false, false);
        // root can read a file whatever its permissions, so the case cannot be created then
        assumeTrue(!keyFile.canRead());
        assertUnauthorized();
    }

    @Test
    public void testKeyFileWithoutPublicKey_Unauthorized() throws Exception {
        Files.write(keyFile.toPath(), "this is not a public key".getBytes(StandardCharsets.US_ASCII));
        assertUnauthorized();
    }

    @Test
    public void testKeyFileEmpty_Unauthorized() throws Exception {
        Files.write(keyFile.toPath(), new byte[0]);
        assertUnauthorized();
    }

    private void assertUnauthorized() throws Exception {
        server.setMarkToEndOfLog();
        assertEquals("Expected 401 Unauthorized when the public key cannot be read from " + keyFile,
                     HttpURLConnection.HTTP_UNAUTHORIZED, sendRequest(token).getResponseCode());
        assertNotNull("RestJwtTAI did not report an error validating the JWT",
                      server.waitForStringInTraceUsingMark("Error validating JWT token"));
    }

    private static void deleteKeyFile() {
        if (keyFile != null && keyFile.exists()) {
            keyFile.setReadable(true, false);
            keyFile.delete();
        }
    }

    private static String createRs256Token(KeyPair pair) throws Exception {
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
        jws.setKey(pair.getPrivate());
        return jws.getCompactSerialization();
    }

    private static HttpURLConnection sendRequest(String bearerToken) throws Exception {
        URL url = new URL("https", server.getHostname(), server.getHttpDefaultSecurePort(), MBEANS_ENDPOINT);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
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
