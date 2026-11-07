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

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
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

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class RestJwtFATTest {

    private static final Class<?> c = RestJwtFATTest.class;

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
        String token = createToken(FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.ADMIN_GROUPS));
        assertEquals("Expected 200 OK for a valid JWT with a server: access ID subject and the liberty-admin group access ID in groups",
                     HttpURLConnection.HTTP_OK, FATSuite.getResponseCode(server, token));
    }

    /**
     * RestJwtTAI sets disableLtpaCookie=true, so a successful Bearer authentication
     * must not return an LTPA cookie to the client.
     */
    @Test
    public void testValidJwt_NoLtpaCookieInResponse() throws Exception {
        String token = createToken(FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.ADMIN_GROUPS));
        HttpURLConnection conn = FATSuite.sendRequest(server, token);
        assertEquals("Expected 200 OK for a valid JWT with a server: access ID subject and the liberty-admin group access ID in groups",
                     HttpURLConnection.HTTP_OK, conn.getResponseCode());
        List<String> setCookies = conn.getHeaderFields().get("Set-Cookie");
        if (setCookies != null) {
            for (String cookie : setCookies) {
                assertFalse("Response must not contain an LTPA cookie: " + cookie, cookie.contains("LtpaToken"));
            }
        }
    }

    @Test
    public void testValidJwtWithReaderRole_Success() throws Exception {
        String token = createToken(FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.READER_GROUPS));
        assertEquals("Expected 200 OK for a valid JWT with a server: access ID subject and the liberty-reader group access ID in groups",
                     HttpURLConnection.HTTP_OK, FATSuite.getResponseCode(server, token));
    }

    @Test
    public void testValidJwtWithUnauthorizedRole_Forbidden() throws Exception {
        String token = createToken(FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.UNAUTHORIZED_GROUPS));
        assertEquals("Expected 403 Forbidden for a valid JWT with a server: access ID subject and only the unauthorizedGroup group access ID, which has no role",
                     HttpURLConnection.HTTP_FORBIDDEN,
                     FATSuite.getResponseCode(server, token));
    }

    /**
     * The node agent uses a server: access ID (SERVER_ID in the other tests). A user: access ID with the same
     * groups must be accepted too.
     */
    @Test
    public void testUserAccessIdWithAdminGroup_Success() throws Exception {
        String token = createToken(FATSuite.createClaims(FATSuite.USER_ID, FATSuite.ADMIN_GROUPS));
        assertEquals("Expected 200 OK for a valid JWT with a user: access ID subject and the liberty-admin group access ID in groups",
                     HttpURLConnection.HTTP_OK,
                     FATSuite.getResponseCode(server, token));
    }

    @Test
    public void testUserAccessIdWithPlainGroupNames_Success() throws Exception {
        String token = createToken(FATSuite.createClaims(FATSuite.USER_ID, FATSuite.PLAIN_ADMIN_GROUPS));
        assertEquals("Expected 200 OK for a valid JWT with a user: access ID subject and the plain group name liberty-admin in groups",
                     HttpURLConnection.HTTP_OK,
                     FATSuite.getResponseCode(server, token));
    }

    /**
     * Only the groups are mapped to the roles, so a token without a groups claim is authenticated but has no role.
     */
    @Test
    public void testNoGroups_Forbidden() throws Exception {
        String token = createToken(FATSuite.createClaims(FATSuite.SERVER_ID, null));
        assertEquals("Expected 403 Forbidden for a valid JWT with a server: access ID subject and no groups claim, because only groups are mapped to roles",
                     HttpURLConnection.HTTP_FORBIDDEN,
                     FATSuite.getResponseCode(server, token));
    }

    @Test
    public void testEmptyGroups_Forbidden() throws Exception {
        String token = createToken(FATSuite.createClaims(FATSuite.SERVER_ID, Collections.<String> emptyList()));
        assertEquals("Expected 403 Forbidden for a valid JWT with a server: access ID subject and an empty groups claim, because only groups are mapped to roles",
                     HttpURLConnection.HTTP_FORBIDDEN,
                     FATSuite.getResponseCode(server, token));
    }

    @Test
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void testInvalidSignatureJwt_Unauthorized() throws Exception {
        String token = FATSuite.createHs256Jwt(FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.ADMIN_GROUPS), FATSuite.WRONG_KEY);
        assertEquals("Expected 401 Unauthorized for a JWT with the admin subject and groups but signed with the wrong key",
                     HttpURLConnection.HTTP_UNAUTHORIZED,
                     FATSuite.getResponseCode(server, token));
    }

    @Test
    public void testExpiredJwt_Unauthorized() throws Exception {
        JwtClaims claims = FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.ADMIN_GROUPS);
        NumericDate expired = NumericDate.now();
        expired.addSeconds(-600);
        claims.setExpirationTime(expired);
        assertEquals("Expected 401 Unauthorized for a JWT with the admin subject and groups but an expiration time in the past",
                     HttpURLConnection.HTTP_UNAUTHORIZED,
                     FATSuite.getResponseCode(server, createToken(claims)));
    }

    /**
     * jwtConsumer does not allow token reuse by default, so presenting the same token (same iss and jti)
     * a second time is rejected as a replay.
     */
    @Test
    public void testReplayedJwt_Unauthorized() throws Exception {
        String token = createToken(FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.ADMIN_GROUPS));
        assertEquals("Expected 200 OK the first time a valid admin JWT is used", HttpURLConnection.HTTP_OK,
                     FATSuite.getResponseCode(server, token));
        assertEquals("Expected 401 Unauthorized when the same admin JWT (same iss and jti) is replayed and tokenReuse is off",
                     HttpURLConnection.HTTP_UNAUTHORIZED,
                     FATSuite.getResponseCode(server, token));
    }

    /**
     * With tokenReuse="true" on the jwtConsumer, the same token (same iss and jti) is accepted repeatedly.
     */
    @Test
    public void testReplayedJwt_AllowedWhenTokenReuseEnabled() throws Exception {
        server.saveServerConfiguration();
        server.setMarkToEndOfLog();
        server.setServerConfigurationFromFilePath("server_tokenReuse.xml");
        try {
            assertNotNull("The server configuration update was not completed",
                          server.waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I"));
            String token = createToken(FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.ADMIN_GROUPS));
            assertEquals("Expected 200 OK the first time a valid admin JWT is used", HttpURLConnection.HTTP_OK,
                         FATSuite.getResponseCode(server, token));
            assertEquals("Expected 200 OK when the same admin JWT (same iss and jti) is replayed and tokenReuse is true",
                         HttpURLConnection.HTTP_OK,
                         FATSuite.getResponseCode(server, token));
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I");
        }
    }
    /**
     * With tokenReuse="true" the same token is accepted on every request, here five times, and a user: access ID
     * with plain group names is accepted too. The jwtConsumer records the iss and jti of a token only while
     * tokenReuse is false, so the uses with tokenReuse="true" are not recorded. After the configuration is restored,
     * the first use of that token is accepted and is recorded, and the next use is rejected as a replay.
     */
    @Test
    public void testReplayedJwt_AllowedManyTimesWhenTokenReuseEnabled_RejectedAfterRestore() throws Exception {
        server.saveServerConfiguration();
        server.setMarkToEndOfLog();
        server.setServerConfigurationFromFilePath("server_tokenReuse.xml");
        String token = createToken(FATSuite.createClaims(FATSuite.USER_ID, FATSuite.PLAIN_ADMIN_GROUPS));
        try {
            assertNotNull("The server configuration update was not completed",
                          server.waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I"));
            for (int i = 1; i <= 5; i++) {
                assertEquals("Expected 200 OK for use " + i + " of the same user: access ID admin JWT when tokenReuse is true",
                             HttpURLConnection.HTTP_OK,
                             FATSuite.getResponseCode(server, token));
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I");
        }

        // tokenReuse is off again. The earlier uses were not recorded, so the first use is accepted and recorded.
        assertEquals("Expected 200 OK the first use of the admin JWT after tokenReuse is turned off, because the uses while tokenReuse was true were not recorded",
                     HttpURLConnection.HTTP_OK, FATSuite.getResponseCode(server, token));
        assertEquals("Expected 401 Unauthorized when the same admin JWT (same iss and jti) is replayed after tokenReuse is turned off",
                     HttpURLConnection.HTTP_UNAUTHORIZED, FATSuite.getResponseCode(server, token));
    }


    @Test
    public void testMissingToken_Unauthorized() throws Exception {
        assertEquals("Expected 401 Unauthorized when Authorization header is omitted", HttpURLConnection.HTTP_UNAUTHORIZED,
                     FATSuite.getResponseCode(server, null));
    }

    @Test
    public void testMalformedBearerToken_Unauthorized() throws Exception {
        assertEquals("Expected 401 Unauthorized for malformed Bearer token", HttpURLConnection.HTTP_UNAUTHORIZED,
                     FATSuite.getResponseCode(server, "not.a.valid.jwt.token"));
    }

    /**
     * A valid, signed JWT from an issuer other than the REST issuer is not processed by RestJwtTAI.
     */
    @Test
    public void testJwtFromOtherIssuer_Unauthorized() throws Exception {
        JwtClaims claims = FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.ADMIN_GROUPS);
        claims.setIssuer(FATSuite.OTHER_ISSUER);
        assertEquals("Expected 401 Unauthorized for a JWT from another issuer", HttpURLConnection.HTTP_UNAUTHORIZED,
                     FATSuite.getResponseCode(server, createToken(claims)));
    }

    /**
     * A valid, signed JWT from the REST issuer but for another audience is not processed by RestJwtTAI.
     */
    @Test
    public void testJwtForOtherAudience_Unauthorized() throws Exception {
        JwtClaims claims = FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.ADMIN_GROUPS);
        claims.setAudience(FATSuite.OTHER_AUDIENCE);
        assertEquals("Expected 401 Unauthorized for a JWT for another audience", HttpURLConnection.HTTP_UNAUTHORIZED,
                     FATSuite.getResponseCode(server, createToken(claims)));
    }

    @Test
    public void testUsernameExtraction_UpnClaim() throws Exception {
        JwtClaims claims = FATSuite.createClaims(null, FATSuite.ADMIN_GROUPS);
        claims.setClaim("upn", FATSuite.SERVER_ID);
        assertEquals("Expected 200 OK for a valid JWT with the server: access ID in the 'upn' claim and no sub claim, with the admin group access ID in groups",
                     HttpURLConnection.HTTP_OK,
                     FATSuite.getResponseCode(server, createToken(claims)));
    }

    @Test
    public void testUsernameExtraction_PreferredUsernameClaim() throws Exception {
        JwtClaims claims = FATSuite.createClaims(null, FATSuite.ADMIN_GROUPS);
        claims.setClaim("preferred_username", FATSuite.SERVER_ID);
        assertEquals("Expected 200 OK for a valid JWT with the server: access ID in the 'preferred_username' claim and no sub claim, with the admin group access ID in groups",
                     HttpURLConnection.HTTP_OK,
                     FATSuite.getResponseCode(server, createToken(claims)));
    }

    @Test
    public void testUsernameExtraction_UsernameClaim() throws Exception {
        JwtClaims claims = FATSuite.createClaims(null, FATSuite.ADMIN_GROUPS);
        claims.setClaim("username", FATSuite.SERVER_ID);
        assertEquals("Expected 200 OK for a valid JWT with the server: access ID in the 'username' claim and no sub claim, with the admin group access ID in groups",
                     HttpURLConnection.HTTP_OK,
                     FATSuite.getResponseCode(server, createToken(claims)));
    }

    @Test
    public void testGroupMapping_CommaSeparatedString() throws Exception {
        JwtClaims claims = FATSuite.createClaims(FATSuite.SERVER_ID, null);
        claims.setClaim("groups", FATSuite.ADMIN_AND_OTHER_GROUPS_CSV);
        assertEquals("Expected 200 OK for a valid JWT with a server: access ID subject and a comma-separated groups string that includes the admin group access ID",
                     HttpURLConnection.HTTP_OK,
                     FATSuite.getResponseCode(server, createToken(claims)));
    }

    /**
     * None of the groups in the comma-separated string has a role, so the string is split and every entry is checked
     * but none grants access.
     */
    @Test
    public void testGroupMapping_CommaSeparatedStringWithoutRole_Forbidden() throws Exception {
        JwtClaims claims = FATSuite.createClaims(FATSuite.SERVER_ID, null);
        claims.setClaim("groups", FATSuite.OTHER_AND_UNAUTHORIZED_GROUPS_CSV);
        assertEquals("Expected 403 Forbidden for a valid JWT with a server: access ID subject and a comma-separated groups string of otherGroup and unauthorizedGroup, which have no role",
                     HttpURLConnection.HTTP_FORBIDDEN,
                     FATSuite.getResponseCode(server, createToken(claims)));
    }

    /**
     * The node agent builds the JWT with a server access ID as the subject and plain group names. The plain group
     * names are expanded to group:<realm>/<name>, using the realm of the server access ID, and so match the role
     * mapping.
     */
    @Test
    public void testGroupMapping_PlainGroupNames() throws Exception {
        String token = createToken(FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.PLAIN_ADMIN_GROUPS));
        assertEquals("Expected 200 OK for a valid JWT with a server: access ID subject and the plain group name liberty-admin in groups",
                     HttpURLConnection.HTTP_OK,
                     FATSuite.getResponseCode(server, token));
    }

    /**
     * The subject is a plain name and the groups are plain names too, so neither carries an access ID. There is no
     * realm to build the access IDs from and the token is rejected.
     */
    @Test
    public void testPlainUserSubject_Unauthorized() throws Exception {
        String token = createToken(FATSuite.createClaims(FATSuite.PLAIN_USER, FATSuite.PLAIN_ADMIN_GROUPS));
        assertEquals("Expected 401 Unauthorized for a valid JWT whose subject is the plain user name plainUser and whose groups has only the plain group name liberty-admin, so there is no access ID to take the realm from",
                     HttpURLConnection.HTTP_UNAUTHORIZED, FATSuite.getResponseCode(server, token));
    }

    /**
     * The subject is a plain user name, but the groups carry the access ID. The realm is taken from the group access
     * ID, so the user gets that realm and the group matches the role mapping.
     */
    @Test
    public void testPlainUserSubjectWithGroupAccessId_Success() throws Exception {
        String token = createToken(FATSuite.createClaims(FATSuite.PLAIN_USER, FATSuite.ADMIN_GROUPS));
        assertEquals("Expected 200 OK for a valid JWT whose subject is the plain user name plainUser and whose groups has the liberty-admin group access ID, which gives the realm",
                     HttpURLConnection.HTTP_OK, FATSuite.getResponseCode(server, token));
    }

    /**
     * The subject is the node agent name without the server:<realm>/server: prefix, but the groups carry the access
     * ID. The realm is taken from the group access ID.
     */
    @Test
    public void testPlainServerSubjectWithGroupAccessId_Success() throws Exception {
        String token = createToken(FATSuite.createClaims(FATSuite.PLAIN_SERVER, FATSuite.ADMIN_GROUPS));
        assertEquals("Expected 200 OK for a valid JWT whose subject is the plain node agent name ndcell_node1_nodeagent and whose groups has the liberty-admin group access ID, which gives the realm",
                     HttpURLConnection.HTTP_OK, FATSuite.getResponseCode(server, token));
    }

    @Test
    public void testGroupMapping_RolesClaim() throws Exception {
        JwtClaims claims = FATSuite.createClaims(FATSuite.SERVER_ID, null);
        claims.setStringListClaim("roles", FATSuite.ADMIN_GROUPS);
        assertEquals("Expected 200 OK for a valid JWT with a server: access ID subject and the admin group access ID in the 'roles' claim",
                     HttpURLConnection.HTTP_OK,
                     FATSuite.getResponseCode(server, createToken(claims)));
    }

    @Test
    public void testGroupMapping_GroupClaim() throws Exception {
        JwtClaims claims = FATSuite.createClaims(FATSuite.SERVER_ID, null);
        claims.setStringListClaim("group", FATSuite.ADMIN_GROUPS);
        assertEquals("Expected 200 OK for a valid JWT with a server: access ID subject and the admin group access ID in the 'group' claim",
                     HttpURLConnection.HTTP_OK,
                     FATSuite.getResponseCode(server, createToken(claims)));
    }

    /** @return the claims signed with the shared key of the jwtConsumer */
    private static String createToken(JwtClaims claims) throws Exception {
        return FATSuite.createHs256Jwt(claims, FATSuite.SHARED_KEY);
    }
}
