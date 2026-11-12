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

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Key;
import java.security.PrivateKey;
import java.security.SecureRandom;
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
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureEE10RepeatAction;
import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureEE11RepeatAction;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                RestJwtFATTest.class,
                RestJwtNoConnectorFATTest.class,
                RestJwtNoJwtFATTest.class,
                RestJwtKeyFileFATTest.class,
                RestJwtKeyFileInvalidFATTest.class,
                RestJwtKeyRotationFATTest.class
})
public class FATSuite {

    /*
     * The restJwt-1.0 auto feature supports EE10 and tolerates EE11, so the EE10 and EE11 repeats are run.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestFeatureEE10RepeatAction().forServerConfigPaths("publish/servers"))
                    .andWith(new SecurityTestFeatureEE11RepeatAction().forServerConfigPaths("publish/servers"));

    public static final String REST_MBEANS_ENDPOINT = "/IBMJMXConnectorREST/mbeans";

    // Token issuer and audience that RestJwtTAI processes, and values that it must not process
    public static final String ISSUER = "IBMJMXConnectorRESTIssuer";
    public static final String AUDIENCE = "IBMJMXConnectorREST";
    public static final String OTHER_ISSUER = "https://other.issuer";
    public static final String OTHER_AUDIENCE = "someOtherAudience";

    // HS256 shared key of the jwtConsumer in server.xml, and a key that does not match it
    public static final String SHARED_KEY = "superSecretSharedKeyForRestJwtTesting123456";
    public static final String WRONG_KEY = "wrongSecretSharedKeyThatWillFailValidation1";

    /*
     * The node agent builds the JWT with a server or user access ID as the subject and plain group names. The FAT
     * servers have no user registry, so RestJwtTAI takes the realm from the access ID and builds the group access IDs
     * group:<realm>/<name> that the role mappings in server.xml use. Only the groups are mapped to the roles. If the
     * subject is a plain name, the realm is taken from a group that is a group access ID. If neither the subject nor
     * any group is an access ID, there is no realm and the token is rejected.
     */
    public static final String REALM = "defaultWIMFileBasedRealm";

    /**
     * The access ID of the node agent, server:<realm>/server:<cell>_<node>_nodeagent. Here the cell is ndcell
     * and the node is node1. The node agent can authenticate to the REST connector with a server: or a user: access
     * ID, and most tests use this one.
     */
    public static final String SERVER_ID = "server:" + REALM + "/server:ndcell_node1_nodeagent";

    /** A user access ID, user:<realm>/<name>, which the node agent can also send. Used by the tests that cover a user: subject */
    public static final String USER_ID = "user:" + REALM + "/adminUser";

    /** A user name that is not an access ID. RestJwtTAI needs a group access ID to get the realm for it */
    public static final String PLAIN_USER = "plainUser";

    /** The node agent name without the server:<realm>/server: prefix. RestJwtTAI needs a group access ID to get the realm for it */
    public static final String PLAIN_SERVER = "ndcell_node1_nodeagent";

    public static final String ADMIN_GROUP = "liberty-admin";
    public static final String READER_GROUP = "liberty-reader";
    public static final String OTHER_GROUP = "otherGroup";
    public static final String UNAUTHORIZED_GROUP = "unauthorizedGroup";

    public static final String ADMIN_GROUP_ACCESS_ID = "group:" + REALM + "/" + ADMIN_GROUP;
    public static final String READER_GROUP_ACCESS_ID = "group:" + REALM + "/" + READER_GROUP;
    public static final String OTHER_GROUP_ACCESS_ID = "group:" + REALM + "/" + OTHER_GROUP;
    public static final String UNAUTHORIZED_GROUP_ACCESS_ID = "group:" + REALM + "/" + UNAUTHORIZED_GROUP;

    public static final List<String> ADMIN_GROUPS = Collections.singletonList(ADMIN_GROUP_ACCESS_ID);
    public static final List<String> READER_GROUPS = Collections.singletonList(READER_GROUP_ACCESS_ID);
    public static final List<String> UNAUTHORIZED_GROUPS = Collections.singletonList(UNAUTHORIZED_GROUP_ACCESS_ID);
    /** Plain group names, as the node agent sends them. They are expanded to group access IDs with the realm of the subject */
    public static final List<String> PLAIN_ADMIN_GROUPS = Collections.singletonList(ADMIN_GROUP);

    /** A comma-separated groups claim that includes the administrator group */
    public static final String ADMIN_AND_OTHER_GROUPS_CSV = ADMIN_GROUP_ACCESS_ID + ", " + OTHER_GROUP_ACCESS_ID;

    /** A comma-separated groups claim of groups that have no role */
    public static final String OTHER_AND_UNAUTHORIZED_GROUPS_CSV = OTHER_GROUP_ACCESS_ID + ", " + UNAUTHORIZED_GROUP_ACCESS_ID;

    /**
     * Creates the claims of a token that RestJwtTAI accepts: the REST issuer and audience, the given subject and
     * groups, a 10 minute expiry and a unique jti. A null subject or null groups leaves that claim out. A test
     * changes a claim by setting or unsetting it on the returned object.
     */
    public static JwtClaims createClaims(String subject, List<String> groups) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER);
        claims.setAudience(AUDIENCE);
        if (subject != null) {
            claims.setSubject(subject);
        }
        if (groups != null) {
            claims.setStringListClaim("groups", groups);
        }
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setIssuedAtToNow();
        claims.setGeneratedJwtId();
        return claims;
    }

    /** @return the claims signed as a compact JWT with HS256 and the shared key */
    public static String createHs256Jwt(JwtClaims claims, String sharedKey) throws Exception {
        return sign(claims, AlgorithmIdentifiers.HMAC_SHA256, new HmacKey(sharedKey.getBytes("UTF-8")));
    }

    /** @return the claims signed as a compact JWT with RS256 and the private key */
    public static String createRs256Jwt(JwtClaims claims, PrivateKey privateKey) throws Exception {
        return sign(claims, AlgorithmIdentifiers.RSA_USING_SHA256, privateKey);
    }

    private static String sign(JwtClaims claims, String algorithm, Key key) throws Exception {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setAlgorithmHeaderValue(algorithm);
        jws.setKey(key);
        return jws.getCompactSerialization();
    }

    /** @return an SSL context that trusts every server certificate, for the self-signed FAT servers */
    public static SSLContext getTrustAllSSLContext() throws Exception {
        TrustManager[] trustAll = new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        } };
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAll, new SecureRandom());
        return sc;
    }

    /**
     * Sends a GET request for the REST mbeans endpoint on the secondary secure port of the server.
     *
     * @param bearerToken the JWT to send in the Authorization header, or null to send no Authorization header
     * @return the connected connection
     */
    public static HttpURLConnection sendRequest(LibertyServer server, String bearerToken) throws Exception {
        URL url = new URL("https", server.getHostname(), server.getHttpSecondarySecurePort(), REST_MBEANS_ENDPOINT);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setSSLSocketFactory(getTrustAllSSLContext().getSocketFactory());
        conn.setHostnameVerifier((hostname, session) -> true);
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        if (bearerToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }
        conn.connect();
        return conn;
    }

    /** @return the HTTP response code of {@link #sendRequest(LibertyServer, String)} */
    public static int getResponseCode(LibertyServer server, String bearerToken) throws Exception {
        return sendRequest(server, bearerToken).getResponseCode();
    }
}
