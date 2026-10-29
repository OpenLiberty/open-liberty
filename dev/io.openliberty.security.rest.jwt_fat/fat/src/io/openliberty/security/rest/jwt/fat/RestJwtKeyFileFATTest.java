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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * RestJwtTAI still validates the token with the jwtConsumer, but the RS256 public key is read from
 * the file named by the jwkEndpointUrl of the defaultJwtConsumer in server.xml:
 * jwkEndpointUrl="file:${server.config.dir}resources/security/jwtPublic.pem"
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class RestJwtKeyFileFATTest {

    private static final Class<?> c = RestJwtKeyFileFATTest.class;

    private static final String ISSUER = "IBMJMXConnectorRESTIssuer";
    private static final String AUDIENCE = "IBMJMXConnectorREST";
    private static final String MBEANS_ENDPOINT = "/IBMJMXConnectorREST/mbeans";

    @Server("RestJwtKeyFileServer")
    public static LibertyServer server;

    private static PrivateKey signingKey;
    private static PrivateKey otherKey;

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";

        // Generate the key pairs at run time so that no private key is checked in.
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        signingKey = pair.getPrivate();
        otherKey = generator.generateKeyPair().getPrivate();

        File keyFile = new File(server.getServerRoot() + "/resources/security/jwtPublic.pem");
        keyFile.getParentFile().mkdirs();
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                     + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(pair.getPublic().getEncoded())
                     + "\n-----END PUBLIC KEY-----\n";
        Files.write(keyFile.toPath(), pem.getBytes(StandardCharsets.US_ASCII));

        Log.info(c, methodName, "Starting RestJwtKeyFileServer with the jwkEndpointUrl file " + keyFile.getAbsolutePath());
        server.startServer();
        assertNotNull("IBMJMXConnectorREST application was not ready",
                      server.waitForStringInLog("SRVE0242I.*IBMJMXConnectorREST"));
        assertNotNull("Default HTTPS endpoint did not start", server.waitForDefaultHTTPEndpointSSLStart());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testTokenSignedWithKeyFromFile_Success() throws Exception {
        String token = createRs256Token("adminUser", "liberty-admin", signingKey);
        assertEquals("Expected 200 OK when the signature matches the public key file",
                     HttpURLConnection.HTTP_OK, sendRequest(token).getResponseCode());
    }

    @Test
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void testTokenSignedWithOtherKey_Unauthorized() throws Exception {
        String token = createRs256Token("adminUser", "liberty-admin", otherKey);
        assertEquals("Expected 401 Unauthorized when the signature does not match the public key file",
                     HttpURLConnection.HTTP_UNAUTHORIZED, sendRequest(token).getResponseCode());
    }

    private static String createRs256Token(String user, String group, PrivateKey key) throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER);
        claims.setAudience(AUDIENCE);
        claims.setSubject(user);
        claims.setStringListClaim("groups", Collections.singletonList(group));
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
