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
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.keys.HmacKey;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import io.openliberty.security.rest.jwt.tai.RestJwtTAI;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class RestJwtFATTest {

    private static final Class<?> c = RestJwtFATTest.class;

    private static final String ISSUER = "IBMJMXConnectorRESTIssuer";
    private static final String AUDIENCE = "IBMJMXConnectorREST";
    private static final String SHARED_KEY = "superSecretSharedKeyForRestJwtTesting123456";
    private static final String WRONG_KEY = "wrongSecretSharedKeyThatWillFailValidation1";


    @Server("RestJwtServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";
        // The restJwt-1.0 auto-feature is provisioned from the product image because jwt-1.0 and
        // restConnector-2.0 are enabled in server.xml.
        Log.info(c, methodName, "Starting RestJwtServer...");
        server.startServer();
        assertNotNull("IBMJMXConnectorREST application was not ready",
                      server.waitForStringInLog("SRVE0242I.*IBMJMXConnectorREST"));
        assertNotNull("Default HTTPS endpoint did not start", server.waitForDefaultHTTPEndpointSSLStart());
        Log.info(c, methodName, "RestJwtServer started successfully.");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        String methodName = "tearDown";
        Log.info(c, methodName, "Stopping RestJwtServer...");
        server.stopServer();
        Log.info(c, methodName, "RestJwtServer stopped.");
    }

    @Test
    public void testValidJwtWithAdminRole_Success() throws Exception {
        String token = createJwtToken("adminUser", Collections.singletonList("liberty-admin"), ISSUER, SHARED_KEY, 10);
        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", token);
        int responseCode = conn.getResponseCode();
        assertEquals("Expected 200 OK for valid admin JWT", HttpURLConnection.HTTP_OK, responseCode);
    }

    /**
     * RestJwtTAI sets disableLtpaCookie=true, so a successful Bearer authentication
     * must not return an LTPA cookie to the client.
     */
    @Test
    public void testValidJwt_NoLtpaCookieInResponse() throws Exception {
        String token = createJwtToken("adminUser", Collections.singletonList("liberty-admin"), ISSUER, SHARED_KEY, 10);
        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", token);
        assertEquals("Expected 200 OK for valid admin JWT", HttpURLConnection.HTTP_OK, conn.getResponseCode());
        List<String> setCookies = conn.getHeaderFields().get("Set-Cookie");
        if (setCookies != null) {
            for (String cookie : setCookies) {
                assertFalse("Response must not contain an LTPA cookie: " + cookie, cookie.contains("LtpaToken"));
            }
        }
    }

    @Test
    public void testValidJwtWithReaderRole_Success() throws Exception {
        String token = createJwtToken("readerUser", Collections.singletonList("liberty-reader"), ISSUER, SHARED_KEY, 10);
        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", token);
        int responseCode = conn.getResponseCode();
        assertEquals("Expected 200 OK for valid reader JWT", HttpURLConnection.HTTP_OK, responseCode);
    }

    @Test
    public void testValidJwtWithUnauthorizedRole_Forbidden() throws Exception {
        String token = createJwtToken("unauthorizedUser", Collections.singletonList("unauthorizedGroup"), ISSUER, SHARED_KEY, 10);
        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", token);
        int responseCode = conn.getResponseCode();
        assertEquals("Expected 403 Forbidden for user without required roles", HttpURLConnection.HTTP_FORBIDDEN, responseCode);
    }

    @Test
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void testInvalidSignatureJwt_Unauthorized() throws Exception {
        String token = createJwtToken("adminUser", Collections.singletonList("liberty-admin"), ISSUER, WRONG_KEY, 10);
        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", token);
        int responseCode = conn.getResponseCode();
        assertEquals("Expected 401 Unauthorized for JWT with invalid signature", HttpURLConnection.HTTP_UNAUTHORIZED, responseCode);
    }

    @Test
    public void testExpiredJwt_Unauthorized() throws Exception {
        String token = createJwtToken("adminUser", Collections.singletonList("liberty-admin"), ISSUER, SHARED_KEY, -10);
        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", token);
        int responseCode = conn.getResponseCode();
        assertEquals("Expected 401 Unauthorized for expired JWT", HttpURLConnection.HTTP_UNAUTHORIZED, responseCode);
    }

    @Test
    public void testMissingToken_Unauthorized() throws Exception {
        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", null);
        int responseCode = conn.getResponseCode();
        assertEquals("Expected 401 Unauthorized when Authorization header is omitted", HttpURLConnection.HTTP_UNAUTHORIZED, responseCode);
    }

    @Test
    public void testMalformedBearerToken_Unauthorized() throws Exception {
        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", "not.a.valid.jwt.token");
        int responseCode = conn.getResponseCode();
        assertEquals("Expected 401 Unauthorized for malformed Bearer token", HttpURLConnection.HTTP_UNAUTHORIZED, responseCode);
    }

    /**
     * An LTPA token is not a JWT. When it is sent as the Bearer token, it has no REST issuer or REST
     * audience, so RestJwtTAI does not process the request and the user is not authenticated.
     */
    @Test
    public void testLtpaTokenAsBearerToken_Unauthorized() throws Exception {
        String ltpaToken = getLtpaTokenViaBasicAuth("adminUser", "password");
        assertNotNull("Could not obtain an LtpaToken2 cookie using basic auth", ltpaToken);

        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", ltpaToken);
        assertEquals("Expected 401 Unauthorized when an LTPA token is sent as the Bearer token",
                     HttpURLConnection.HTTP_UNAUTHORIZED, conn.getResponseCode());
    }

    /**
     * A valid, signed JWT from an issuer other than the REST issuer is not processed by RestJwtTAI.
     */
    @Test
    public void testJwtFromOtherIssuer_Unauthorized() throws Exception {
        String token = createJwtToken("adminUser", Collections.singletonList("liberty-admin"), "https://other.issuer", SHARED_KEY, 10);
        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", token);
        assertEquals("Expected 401 Unauthorized for a JWT from another issuer", HttpURLConnection.HTTP_UNAUTHORIZED, conn.getResponseCode());
    }

    /**
     * A valid, signed JWT from the REST issuer but for another audience is not processed by RestJwtTAI.
     */
    @Test
    public void testJwtForOtherAudience_Unauthorized() throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER);
        claims.setAudience("someOtherAudience");
        claims.setSubject("adminUser");
        claims.setStringListClaim("groups", Collections.singletonList("liberty-admin"));
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setIssuedAtToNow();
        claims.setGeneratedJwtId();

        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", signClaims(claims, SHARED_KEY));
        assertEquals("Expected 401 Unauthorized for a JWT for another audience", HttpURLConnection.HTTP_UNAUTHORIZED, conn.getResponseCode());
    }

    /**
     * The REST admin endpoint only accepts a JWT. A valid LTPA token sent in the LtpaToken2
     * cookie (no Bearer header) must be rejected, not authenticated by LTPA SSO.
     */
    @Test
    public void testLtpaTokenInLtpaToken2Cookie_Unauthorized() throws Exception {
        String ltpaToken = getLtpaTokenViaBasicAuth("adminUser", "password");
        assertNotNull("Could not obtain an LtpaToken2 cookie using basic auth", ltpaToken);

        HttpURLConnection conn = sendRequestWithCookie(RestJwtTAI.ADMIN_REST_ENDPOINT, "LtpaToken2=" + ltpaToken);
        assertEquals("Expected 401 Unauthorized when only an LtpaToken2 cookie is sent",
                     HttpURLConnection.HTTP_UNAUTHORIZED, conn.getResponseCode());
    }

    @Test
    public void testUsernameExtraction_UpnClaim() throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER);
        claims.setAudience(AUDIENCE);
        claims.setClaim("upn", "adminUser");
        claims.setStringListClaim("groups", Collections.singletonList("liberty-admin"));
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setIssuedAtToNow();
        claims.setGeneratedJwtId();

        String token = signClaims(claims, SHARED_KEY);
        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", token);
        assertEquals("Expected 200 OK when username is in 'upn' claim", HttpURLConnection.HTTP_OK, conn.getResponseCode());
    }

    @Test
    public void testUsernameExtraction_PreferredUsernameClaim() throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER);
        claims.setAudience(AUDIENCE);
        claims.setClaim("preferred_username", "adminUser");
        claims.setStringListClaim("groups", Collections.singletonList("liberty-admin"));
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setIssuedAtToNow();
        claims.setGeneratedJwtId();

        String token = signClaims(claims, SHARED_KEY);
        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", token);
        assertEquals("Expected 200 OK when username is in 'preferred_username' claim", HttpURLConnection.HTTP_OK, conn.getResponseCode());
    }

    @Test
    public void testUsernameExtraction_UsernameClaim() throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER);
        claims.setAudience(AUDIENCE);
        claims.setClaim("username", "adminUser");
        claims.setStringListClaim("groups", Collections.singletonList("liberty-admin"));
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setIssuedAtToNow();
        claims.setGeneratedJwtId();

        String token = signClaims(claims, SHARED_KEY);
        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", token);
        assertEquals("Expected 200 OK when username is in 'username' claim", HttpURLConnection.HTTP_OK, conn.getResponseCode());
    }

    @Test
    public void testGroupMapping_CommaSeparatedString() throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER);
        claims.setAudience(AUDIENCE);
        claims.setSubject("adminUser");
        claims.setClaim("groups", "liberty-admin, otherGroup");
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setIssuedAtToNow();
        claims.setGeneratedJwtId();

        String token = signClaims(claims, SHARED_KEY);
        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", token);
        assertEquals("Expected 200 OK when groups claim is a comma-separated string", HttpURLConnection.HTTP_OK, conn.getResponseCode());
    }

    @Test
    public void testGroupMapping_RolesClaim() throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER);
        claims.setAudience(AUDIENCE);
        claims.setSubject("adminUser");
        claims.setStringListClaim("roles", Collections.singletonList("liberty-admin"));
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setIssuedAtToNow();
        claims.setGeneratedJwtId();

        String token = signClaims(claims, SHARED_KEY);
        HttpURLConnection conn = sendRequest(RestJwtTAI.ADMIN_REST_ENDPOINT, "GET", token);
        assertEquals("Expected 200 OK when groups are provided via 'roles' claim", HttpURLConnection.HTTP_OK, conn.getResponseCode());
    }

    private String createJwtToken(String username, List<String> groups, String issuer, String sharedKey, int expiryMinutes) throws Exception {
        JwtClaims claims = new JwtClaims();
        if (issuer != null) {
            claims.setIssuer(issuer);
        }
        claims.setAudience(AUDIENCE);
        if (username != null) {
            claims.setSubject(username);
        }
        if (groups != null) {
            claims.setStringListClaim("groups", groups);
        }
        if (expiryMinutes > 0) {
            claims.setExpirationTimeMinutesInTheFuture(expiryMinutes);
        } else {
            NumericDate exp = NumericDate.now();
            exp.addSeconds(expiryMinutes * 60);
            claims.setExpirationTime(exp);
        }
        claims.setIssuedAtToNow();
        claims.setGeneratedJwtId();

        return signClaims(claims, sharedKey);
    }

    private String signClaims(JwtClaims claims, String sharedKey) throws Exception {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
        jws.setKey(new HmacKey(sharedKey.getBytes("UTF-8")));
        return jws.getCompactSerialization();
    }

    /**
     * Authenticates with basic auth (RestJwtTAI is not involved because there is no Bearer header)
     * and returns the value of the LtpaToken2 cookie that the server issues.
     */
    private String getLtpaTokenViaBasicAuth(String user, String password) throws Exception {
        int port = server.getHttpDefaultSecurePort();
        URL url = new URL("https", server.getHostname(), port, RestJwtTAI.ADMIN_REST_ENDPOINT);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setSSLSocketFactory(getTrustAllSocketFactory());
        conn.setHostnameVerifier(getTrustAllHostnameVerifier());
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        String basic = java.util.Base64.getEncoder().encodeToString((user + ":" + password).getBytes("UTF-8"));
        conn.setRequestProperty("Authorization", "Basic " + basic);
        conn.connect();
        assertEquals("Basic auth request should succeed", HttpURLConnection.HTTP_OK, conn.getResponseCode());
        List<String> setCookies = conn.getHeaderFields().get("Set-Cookie");
        if (setCookies != null) {
            for (String cookie : setCookies) {
                if (cookie.startsWith("LtpaToken2=")) {
                    return cookie.substring("LtpaToken2=".length(), cookie.indexOf(';') > 0 ? cookie.indexOf(';') : cookie.length());
                }
            }
        }
        return null;
    }

    private HttpURLConnection sendRequestWithCookie(String path, String cookie) throws Exception {
        int port = server.getHttpDefaultSecurePort();
        URL url = new URL("https", server.getHostname(), port, path);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setSSLSocketFactory(getTrustAllSocketFactory());
        conn.setHostnameVerifier(getTrustAllHostnameVerifier());
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Cookie", cookie);
        conn.connect();
        return conn;
    }

    private HttpURLConnection sendRequest(String path, String method, String bearerToken) throws Exception {
        int port = server.getHttpDefaultSecurePort();
        URL url = new URL("https", server.getHostname(), port, path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection) conn).setSSLSocketFactory(getTrustAllSocketFactory());
            ((HttpsURLConnection) conn).setHostnameVerifier(getTrustAllHostnameVerifier());
        }
        conn.setRequestMethod(method);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setDoInput(true);
        if (bearerToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }
        conn.connect();
        return conn;
    }

    private static SSLSocketFactory getTrustAllSocketFactory() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
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
            }
        };
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        return sc.getSocketFactory();
    }

    private static HostnameVerifier getTrustAllHostnameVerifier() {
        return (hostname, session) -> true;
    }
}
