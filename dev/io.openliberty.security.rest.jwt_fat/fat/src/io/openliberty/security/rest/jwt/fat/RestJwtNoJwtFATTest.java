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
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
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

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Negative test: with only restConnector-2.0 enabled (no jwt-1.0) the restJwt-1.0 auto-feature must not be
 * provisioned, so RestJwtTAI must not be created and must not process any request. This is the mirror image of
 * {@link RestJwtNoConnectorFATTest}.
 *
 * Unlike that test, the IBMJMXConnectorREST application is present here. A Bearer token is therefore
 * not handled by any authenticator and the application answers 401, rather than 404. A request that uses
 * Basic authentication still works, which shows that the 401 is caused by the missing JWT support and not by a
 * broken server.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@SkipForRepeat({ SkipForRepeat.EE9_OR_LATER_FEATURES })
public class RestJwtNoJwtFATTest {

    private static final Class<?> c = RestJwtNoJwtFATTest.class;

    // The token uses the REST issuer and audience, so RestJwtTAI would claim it if it were active
    private static final String ISSUER = "IBMJMXConnectorRESTIssuer";
    private static final String AUDIENCE = "IBMJMXConnectorREST";
    private static final String SHARED_KEY = "superSecretSharedKeyForRestJwtTesting123456";

    // Anything that would only be logged if the restJwt bundle was started or the TAI was invoked
    private static final String TAI_TRACE_REGEX = "RestJwtTAI|restJwtTAI|io\\.openliberty\\.security\\.rest\\.jwt\\.tai";

    @Server("RestJwtNoJwtServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(c, "setUp", "Starting RestJwtNoJwtServer (restConnector-2.0 without jwt-1.0)...");
        server.startServer();
        assertNotNull("IBMJMXConnectorREST application was not ready",
                      server.waitForStringInLog("SRVE0242I.*IBMJMXConnectorREST"));
        assertNotNull("Default HTTPS endpoint did not start", server.waitForDefaultHTTPEndpointSSLStart());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Without jwt-1.0 the auto-feature's provision capability is not satisfied, so the bundle is never
     * started and the TAI component is never registered.
     */
    @Test
    public void testRestJwtTaiNotActivated() throws Exception {
        List<String> found = server.findStringsInTrace(TAI_TRACE_REGEX);
        assertTrue("RestJwtTAI should not be activated without jwt-1.0, but found: " + found, found.isEmpty());
    }

    /**
     * Control: the REST connector is up and reachable, and it still accepts Basic authentication. Without this
     * the next test could pass just because the server or the application is not working.
     */
    @Test
    public void testBasicAuthStillWorks() throws Exception {
        HttpURLConnection conn = sendRequest("Basic " + Base64.getEncoder().encodeToString("adminUser:password".getBytes(StandardCharsets.UTF_8)));
        assertEquals("Expected 200 OK for Basic authentication, which shows the REST connector is running",
                     HttpURLConnection.HTTP_OK, conn.getResponseCode());
    }

    /**
     * A request carrying a Bearer JWT for the REST admin path must not be handled by RestJwtTAI. The
     * IBMJMXConnectorREST application exists, but nothing validates a Bearer token, so the request is
     * unauthenticated and the server answers 401 even though the token is valid and the user has the
     * administrator role.
     */
    @Test
    public void testBearerRequestNotProcessedByRestJwtTai() throws Exception {
        String token = createJwtToken("adminUser", "liberty-admin");

        HttpURLConnection conn = sendRequest("Bearer " + token);
        assertEquals("Expected 401 because nothing in the server can authenticate a Bearer token without jwt-1.0",
                     HttpURLConnection.HTTP_UNAUTHORIZED, conn.getResponseCode());

        List<String> found = server.findStringsInTrace(TAI_TRACE_REGEX);
        assertTrue("RestJwtTAI should not process the request, but found: " + found, found.isEmpty());
    }

    private static String createJwtToken(String username, String group) throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER);
        claims.setAudience(AUDIENCE);
        claims.setSubject(username);
        claims.setStringListClaim("groups", Collections.singletonList(group));
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setIssuedAtToNow();
        claims.setGeneratedJwtId();

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
        jws.setKey(new HmacKey(SHARED_KEY.getBytes(StandardCharsets.UTF_8)));
        return jws.getCompactSerialization();
    }

    private static HttpURLConnection sendRequest(String authorizationHeader) throws Exception {
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
        conn.setRequestProperty("Authorization", authorizationHeader);
        conn.connect();
        return conn;
    }
}
