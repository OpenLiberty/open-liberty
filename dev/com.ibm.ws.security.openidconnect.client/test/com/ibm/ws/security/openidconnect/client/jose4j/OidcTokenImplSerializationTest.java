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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.jose4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import org.jose4j.jwt.JwtClaims;
import org.junit.Test;

/**
 * Test serialization of the OidcTokenImpl class. This class needs to maintain deserialization across versions.
 */
public class OidcTokenImplSerializationTest {

    private static final String JSON_JWT_CLAIMS = "{\"exp\":1664996437,\"iat\":1664996137,\"auth_time\":1664996137,\"jti\":\"b251ab22-3a6b-4c89-8547-7bbcf5767524\",\"iss\":\"https://localhost:49651/auth/realms/TestRealm\",\"aud\":\"oidc_client\",\"sub\":\"f9b8f6fe-fda6-4877-a0aa-fb5c3cd4eec0\",\"typ\":\"ID\",\"azp\":\"oidc_client\",\"session_state\":\"6c85b894-8239-4160-9f6f-e257da155f00\",\"at_hash\":\"SsqeiiQ58-dKbwuQYQ4ZmA\",\"acr\":\"1\",\"sid\":\"6c85b894-8239-4160-9f6f-e257da155f00\",\"email_verified\":false,\"preferred_username\":\"testuser\",\"email\":\"testuser@liberty.org\",\"nbf\":123456789,\"nonce\":\"123456789\"}";
    private static final String ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJXSzhwUGcyQVJrRXZ4T08xekgwWTR1dy1DVmxHZFkyV1dPNGt0UDY3OXZRIn0.eyJleHAiOjE2NjQ5OTY0MzcsImlhdCI6MTY2NDk5NjEzNywiYXV0aF90aW1lIjoxNjY0OTk2MTM3LCJqdGkiOiI1NjFlNzY3OC1jYzRjLTQ4NTItYTgzYy0xZWU2OTkxYjZlZDkiLCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo0OTY1MS9hdXRoL3JlYWxtcy9UZXN0UmVhbG0iLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiZjliOGY2ZmUtZmRhNi00ODc3LWEwYWEtZmI1YzNjZDRlZWMwIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoib2lkY19jbGllbnQiLCJzZXNzaW9uX3N0YXRlIjoiNmM4NWI4OTQtODIzOS00MTYwLTlmNmYtZTI1N2RhMTU1ZjAwIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwczovL2xvY2FsaG9zdDo4MDIwIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIiwiZGVmYXVsdC1yb2xlcy10ZXN0cmVhbG0iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBlbWFpbCBwcm9maWxlIiwic2lkIjoiNmM4NWI4OTQtODIzOS00MTYwLTlmNmYtZTI1N2RhMTU1ZjAwIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0dXNlciIsImVtYWlsIjoidGVzdHVzZXJAbGliZXJ0eS5vcmcifQ.Kd7HoeQe1bUuQaVIGrFvvJgBXXkduZ75C7g3X_2qQW1iTIcyl_riBEgQxtFn9BhDG9p-tkJl1EHobKA7fPSue8_ZPDQAATw5C4EvbrBh9uOCGsMewZXsOdeT6sawFgOlZ0uIIn5U1RqxfvcXvVrIO4b1hYprfBRoxVNDHWPBGuqwCob9EM9Br2KPApn4u5c-tFalnT5H0HNYz_Ct6xILzCgf3pmAz3WXndqkBcR7s7XuF86NglnVwoT6ErDRSP5-jvA3SGaI-zq4zgExtBlgXlbgjYJjJTDTWzwLWr5jTSv7LKuIF9IAwPrgxKj4Gf7la-8UzlGdpYqC2W696Av8Yw";
    private static final String REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI1NDlhZmYwNy03YzA3LTQzNjEtOWMzNy1kMmZmYjhlOTk5MzEifQ.eyJleHAiOjE2NjQ5OTc5MzcsImlhdCI6MTY2NDk5NjEzNywianRpIjoiMzZkMTczNmEtMGIyNC00Yjk5LTg4M2YtYjQyYmM3NTljMTQ2IiwiaXNzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6NDk2NTEvYXV0aC9yZWFsbXMvVGVzdFJlYWxtIiwiYXVkIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6NDk2NTEvYXV0aC9yZWFsbXMvVGVzdFJlYWxtIiwic3ViIjoiZjliOGY2ZmUtZmRhNi00ODc3LWEwYWEtZmI1YzNjZDRlZWMwIiwidHlwIjoiUmVmcmVzaCIsImF6cCI6Im9pZGNfY2xpZW50Iiwic2Vzc2lvbl9zdGF0ZSI6IjZjODViODk0LTgyMzktNDE2MC05ZjZmLWUyNTdkYTE1NWYwMCIsInNjb3BlIjoib3BlbmlkIGVtYWlsIHByb2ZpbGUiLCJzaWQiOiI2Yzg1Yjg5NC04MjM5LTQxNjAtOWY2Zi1lMjU3ZGExNTVmMDAifQ.hJXxihlSC8m6CRvo9k3_P1lQOwS2uYLO16iU6LBxhX0";
    private static final String CLIENT_ID = "oidc_client";
    private static final String TOKEN_TYPE = "IDToken";

    private static final String SERIALIZED_FILE_VERS_1 = "test-resources/testdata/ser-files/OidcTokenImpl_1.ser";

    @SuppressWarnings("restriction")
    @Test
    public void testSerialization_Ver1() throws Exception {
        /*
         * Deserialize the object from the serialized file.
         */
        OidcTokenImpl object = null;
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(SERIALIZED_FILE_VERS_1))) {
            object = (OidcTokenImpl) input.readObject();
        }
        assertNotNull("OidcTokenImpl instance could not be read from the serialized file.", object);

        /*
         * Verify the data from the instance matches the expected.
         */
        JwtClaims jwtClaims = JwtClaims.parse(JSON_JWT_CLAIMS);
        assertEquals("The getAccessToken() method returned an unexpected value.", ACCESS_TOKEN, object.getAccessToken());
        assertEquals("The getAccessTokenHash() method returned an unexpected value.", "SsqeiiQ58-dKbwuQYQ4ZmA", object.getAccessTokenHash());
        assertEquals("The getAllClaims() method returned an unexpected value.", jwtClaims.getClaimsMap(), object.getAllClaims());
        assertEquals("The getAllClaimsAsJson() method returned an unexpected value.", JSON_JWT_CLAIMS, object.getAllClaimsAsJson());
        assertEquals("The getAudience() method returned an unexpected value.", Arrays.asList("oidc_client"), object.getAudience());
        assertEquals("The getAuthorizationTimeSeconds() value was an unexpected value.", 0L, object.getAuthorizationTimeSeconds());
        assertEquals("The getAuthorizedParty() method returned an unexpected value.", null, object.getAuthorizedParty());
        assertEquals("The getClaim() method returned an unexpected value.", "testuser", object.getClaim("preferred_username"));
        assertEquals("The getClaim() method returned an unexpected value.", "testuser@liberty.org", object.getClaim("email"));
        assertEquals("The getClassReference() method returned an unexpected value.", null, object.getClassReference());
        assertEquals("The getClientId() method returned an unexpected value.", CLIENT_ID, object.getClientId());
        assertEquals("The getExpirationTimeSeconds() method returned an unexpected value.", 1664996437L, object.getExpirationTimeSeconds());
        assertEquals("The getIssuedAtTimeSeconds() method returned an unexpected value.", 1664996137L, object.getIssuedAtTimeSeconds());
        assertEquals("The getIssuer() method returned an unexpected value.", "https://localhost:49651/auth/realms/TestRealm", object.getIssuer());
        assertEquals("The getJwtClaims() method returned an unexpected value.", jwtClaims.getRawJson(), object.getJwtClaims().getRawJson()); // No equals method in JwtClaims
        assertEquals("The getJwtId() method returned an unexpected value.", "b251ab22-3a6b-4c89-8547-7bbcf5767524", object.getJwtId());
        assertEquals("The getMethodsReferences() method returned an unexpected value.", null, object.getMethodsReferences());
        assertEquals("The getNonce() method returned an unexpected value.", "123456789", object.getNonce());
        assertEquals("The getNotBeforeTimeSeconds() method returned an unexpected value.", 123456789L, object.getNotBeforeTimeSeconds());
        assertEquals("The getRefreshToken() method returned an unexpected value.", REFRESH_TOKEN, object.getRefreshToken());
        assertEquals("The getSubject() method returned an unexpected value.", "f9b8f6fe-fda6-4877-a0aa-fb5c3cd4eec0", object.getSubject());
        assertEquals("The getTokenTypeNoSpace() method returned an unexpected value.", TOKEN_TYPE, object.getTokenTypeNoSpace());
        assertEquals("The getType() method returned an unexpected value.", "Bearer", object.getType());
    }

    /**
     * Method used to create and serialize the OidcTokenImpl for testing.
     *
     * If OidcTokenImpl changes, previously serialized versions of OidcTokenImpl must
     * remain deserializable. Use this method to create a new OidcTokenImpl_x.ser
     * file, replacing the x with the current version + 1. Then write a test
     * that deserializes that version and all previous OidcTokenImpl_x.ser files.
     */
    public static void main(String[] args) throws Exception {
        final String filename = "test-resources/testdata/ser-files/OidcTokenImpl_x.ser";

        /*
         * Create OidcTokenImpl instance.
         */
        OidcTokenImpl object = new OidcTokenImpl(JwtClaims.parse(JSON_JWT_CLAIMS), ACCESS_TOKEN, REFRESH_TOKEN, CLIENT_ID, TOKEN_TYPE);

        /*
         * Serialize the object to a file.
         */
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(filename))) {
            output.writeObject(object);
        }
    }
}
