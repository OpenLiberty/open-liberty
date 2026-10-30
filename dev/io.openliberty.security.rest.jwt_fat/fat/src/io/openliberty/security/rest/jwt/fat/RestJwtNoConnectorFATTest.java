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
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.keys.HmacKey;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import io.openliberty.security.rest.jwt.tai.RestJwtTAI;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Negative test: with only jwt-1.0 enabled (no restConnector-2.0) the restJwt-1.0 auto-feature must not be
 * provisioned, so RestJwtTAI must not be created and must not process any request.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class RestJwtNoConnectorFATTest {

    private static final Class<?> c = RestJwtNoConnectorFATTest.class;

    private static final String ISSUER = "https://localhost:9443/jwt";
    private static final String SHARED_KEY = "superSecretSharedKeyForRestJwtTesting123456";

    // Anything that would only be logged if the restJwt bundle was started or the TAI was invoked
    private static final String TAI_TRACE_REGEX = "RestJwtTAI|restJwtTAI|io\\.openliberty\\.security\\.rest\\.jwt\\.tai";

    @Server("RestJwtNoConnectorServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(c, "setUp", "Starting RestJwtNoConnectorServer (jwt-1.0 without restConnector-2.0)...");
        server.startServer();
        assertNotNull("Default HTTPS endpoint did not start", server.waitForDefaultHTTPEndpointSSLStart());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Without restConnector-2.0 the auto-feature's provision capability is not satisfied, so the bundle is never
     * started and the TAI component is never registered.
     */
    @Test
    public void testRestJwtTaiNotActivated() throws Exception {
        List<String> found = server.findStringsInTrace(TAI_TRACE_REGEX);
        assertTrue("RestJwtTAI should not be activated without restConnector-2.0, but found: " + found, found.isEmpty());
    }

    /**
     * A request carrying a valid Bearer JWT for the REST admin path must not be handled by RestJwtTAI. The
     * IBMJMXConnectorREST application does not exist, so the server answers 404 and the TAI is never invoked.
     */
    @Test
    public void testBearerRequestNotProcessedByRestJwtTai() throws Exception {
        String token = createJwtToken("adminUser", "liberty-admin");

        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, token);
        assertEquals("Expected 404 because the REST connector application is not present", HttpURLConnection.HTTP_NOT_FOUND,
                     conn.getResponseCode());

        List<String> found = server.findStringsInTrace(TAI_TRACE_REGEX);
        assertTrue("RestJwtTAI should not process the request, but found: " + found, found.isEmpty());
    }

    private static String createJwtToken(String username, String group) throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER);
        claims.setSubject(username);
        claims.setStringListClaim("groups", Collections.singletonList(group));
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setIssuedAtToNow();
        claims.setGeneratedJwtId();

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
        jws.setKey(new HmacKey(SHARED_KEY.getBytes("UTF-8")));
        return jws.getCompactSerialization();
    }

    private static HttpURLConnection sendRequest(String path, String bearerToken) throws Exception {
        URL url = new URL("https", server.getHostname(), server.getHttpDefaultSecurePort(), path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (conn instanceof HttpsURLConnection) {
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
            ((HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
            ((HttpsURLConnection) conn).setHostnameVerifier((hostname, session) -> true);
        }
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        conn.connect();
        return conn;
    }
}
