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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.identitystore;

import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.SUBJECT_IDENTIFIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import io.openliberty.security.jakartasec.tokens.AccessTokenImpl;
import io.openliberty.security.jakartasec.tokens.AccessTokenImplSerializationTest;
import io.openliberty.security.jakartasec.tokens.AccessTokenImplTest;
import io.openliberty.security.jakartasec.tokens.IdentityTokenImpl;
import io.openliberty.security.jakartasec.tokens.IdentityTokenImplSerializationTest;
import io.openliberty.security.jakartasec.tokens.IdentityTokenImplTest;
import io.openliberty.security.jakartasec.tokens.OpenIdClaimsImpl;
import io.openliberty.security.jakartasec.tokens.OpenIdClaimsImplTest;
import io.openliberty.security.jakartasec.tokens.RefreshTokenImpl;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.identitystore.openid.AccessToken;
import jakarta.security.enterprise.identitystore.openid.IdentityToken;
import jakarta.security.enterprise.identitystore.openid.OpenIdClaims;
import jakarta.security.enterprise.identitystore.openid.RefreshToken;

/**
 * Serialization test for OpenIdContext.
 */
public class OpenIdContextImplSerializationTest {

    // ProviderMetadata set
    private static final String SERIALIZED_FILE_VERS_1 = "test-resources/testdata/ser-files/OpenIdContextImpl_1.ser";

    // ProviderMetadata not set (set to null)
    private static final String SERIALIZED_FILE_VERS_2 = "test-resources/testdata/ser-files/OpenIdContextImpl_2.ser";

    private static final String ISSUER = "https://localhost:9443/oidc/endpoint/OP/authorize";

    private static final String TOKEN_STRING = "tokenString";

    /**
     * Test serialization with the providerMetadata populated
     *
     * @throws Exception
     */
    @Test
    public void testOpenIdContextSerialization_Ver1() throws Exception {
        /*
         * Deserialize the object from the serialized file.
         */
        OpenIdContextImpl object = null;
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(SERIALIZED_FILE_VERS_1))) {
            object = (OpenIdContextImpl) input.readObject();
        }

        verifyObjectIdContextImpl(object, true);
    }

    /**
     * Test serialization with providerMetadata not set
     *
     * @throws Exception
     */
    @Test
    public void testOpenIdContextSerialization_Ver2() throws Exception {
        /*
         * Deserialize the object from the serialized file.
         */
        OpenIdContextImpl object = null;
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(SERIALIZED_FILE_VERS_2))) {
            object = (OpenIdContextImpl) input.readObject();
        }

        verifyObjectIdContextImpl(object, false);
    }

    /**
     * Walk though all the expected values after deserialization.
     *
     * @param object
     * @param hasProviderMetadata
     */
    private void verifyObjectIdContextImpl(OpenIdContextImpl object, boolean hasProviderMetadata) {
        assertNotNull("OpenIdContextImpl instance could not be read from the serialized file.", object);

        /*
         * Check the AccessToken
         */
        AccessTokenImpl accessToken = (AccessTokenImpl) object.getAccessToken();
        assertNotNull("AccessTokenImpl was null from OpenIdContextImpl.", accessToken);
        AccessTokenImplSerializationTest.verifyAccessTokenContents(accessToken);

        /*
         * Check the OpenIdClaims
         */
        OpenIdClaimsImpl claims = (OpenIdClaimsImpl) object.getClaims();
        assertNotNull("OpenIdClaimsImpl instance could not be read from the serialized file.", claims);
        assertEquals("Incorrect subject on OpenIdClaimsImpl", AccessTokenImplTest.SUBJECT_IN_ACCESS_TOKEN,
                     claims.getSubject());

        /*
         * getClaimsJson is not serialized, but we should recreate it from the
         * OpenIdClaims.
         */
        JsonObject claimsJson = object.getClaimsJson();
        assertNotNull("OpenIdClaimsImpl as Json could not be read from the serialized file.", claimsJson);
        assertEquals("Incorrect subject on OpenIdClaimsImpl", AccessTokenImplTest.SUBJECT_IN_ACCESS_TOKEN,
                     claimsJson.getString(OpenIdConstant.SUBJECT_IDENTIFIER));

        /*
         * Check the IdentityToken
         */
        IdentityTokenImpl identityToken = (IdentityTokenImpl) object.getIdentityToken();
        assertNotNull("IdentityTokenImpl was null from OpenIdContextImpl.", accessToken);
        IdentityTokenImplSerializationTest.verifyIdentityTokenContents(identityToken);

        /*
         * Check the rest of the fields
         */
        assertEquals("The getClientId() method returned an unexpected value.", OpenIdContextImplTest.clientID,
                     object.getClientId());

        Optional<Long> expiresIn = object.getExpiresIn();
        assertTrue("Should have exiresIn set", expiresIn.isPresent());
        assertNotNull("expiresIn should not be null", expiresIn.get());
        assertEquals("The getExpiresIn() method returned an unexpected value.", AccessTokenImplTest.ONE_HOUR,
                     expiresIn.get());

        JsonObject providerMetatdata = object.getProviderMetadata();
        if (hasProviderMetadata) {
            assertNotNull("providerMetatdata was null from OpenIdContextImpl.", providerMetatdata);
            assertEquals("Incorrect providerMetatdata", ISSUER, providerMetatdata.getString(OpenIdConstant.ISSUER));
        } else {
            assertNull("providerMetatdata was not null from OpenIdContextImpl.", providerMetatdata);
        }

        Optional<RefreshToken> refreshToken = object.getRefreshToken();
        assertTrue("Should have a refreshToken", refreshToken.isPresent());
        assertNotNull("RefreshToken should not be null", refreshToken.get());
        assertEquals("The refreshToken has an unexpected value", TOKEN_STRING, refreshToken.get().getToken());

        assertEquals("The getState() method returned an unexpected value.", OpenIdContextImplTest.STATE,
                     object.getState());

        assertEquals("The getSubject() method returned an unexpected value",
                     AccessTokenImplTest.SUBJECT_IN_ACCESS_TOKEN, object.getSubject());

        assertEquals("The getTokenType() method returned an unexpected value", OpenIdContextImplTest.TOKEN_TYPE_BEARER,
                     object.getTokenType());
    }

    /**
     * Method used to create and serialize the OpenIdContextImpl for testing.
     *
     * If OpenIdContextImpl changes, previously serialized versions of
     * OpenIdContextImpl must remain deserializable. Use this method to create a new
     * OpenIdContextImpl_x.ser file, replacing the x with the current version + 1.
     * Then write a test that deserializes that version and all previous
     * OpenIdContextImpl_x.ser files.
     *
     * You will then need to wait for an hour to successfully run the unit test so
     * the "isExpired" is marked as true for the AccessToken
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Create a serialized OpenIdContext file");
        final String filename = "test-resources/testdata/ser-files/OpenIdContextImpl_1.ser"; // change x to a version
                                                                                             // number now, or after
                                                                                             // creation

        /*
         * Create supporting objects
         */
        Map<String, Object> accessTokenClaimsMap = AccessTokenImplTest.createClaimsMap(AccessTokenImplSerializationTest.JWT_ACCESS_TOKEN_STRING);

        AccessToken jwtAccessTokenToWrite = new AccessTokenImpl(AccessTokenImplSerializationTest.JWT_ACCESS_TOKEN_STRING, accessTokenClaimsMap, AccessTokenImplTest.A_MINUTE_AGO, AccessTokenImplTest.ONE_HOUR, AccessTokenImplTest.TOKEN_MIN_VALIDITY_10_MILLIS);

        Map<String, Object> idTokenClaimsMap = IdentityTokenImplTest.createClaimsMap(IdentityTokenImplSerializationTest.JWT_ID_TOKEN_STRING);

        IdentityToken identityTokenToWrite = new IdentityTokenImpl(IdentityTokenImplSerializationTest.JWT_ID_TOKEN_STRING, idTokenClaimsMap, IdentityTokenImplTest.TOKEN_MIN_VALIDITY_10_MILLIS);

        OpenIdClaims openIdClaimsToWrite = OpenIdClaimsImplTest.createOpenIdClaimsWithStringClaim(SUBJECT_IDENTIFIER,
                                                                                                  AccessTokenImplTest.SUBJECT_IN_ACCESS_TOKEN);

        /*
         * Optionally swap providerMetadata for null, to verify we handle a null providerMetadata correctly
         */
        JsonObject providerMetadata = Json.createObjectBuilder().add(OpenIdConstant.ISSUER, ISSUER).build();

        RefreshToken refreshToken = new RefreshTokenImpl(TOKEN_STRING);

        /*
         * Create OpenIdContextImpl
         */
        OpenIdContextImpl openIdContextToWrite = new OpenIdContextImpl(AccessTokenImplTest.SUBJECT_IN_ACCESS_TOKEN, OpenIdContextImplTest.TOKEN_TYPE_BEARER, jwtAccessTokenToWrite, identityTokenToWrite, openIdClaimsToWrite, providerMetadata, OpenIdContextImplTest.STATE, true, OpenIdContextImplTest.clientID);

        /*
         * Set additional items
         */
        openIdContextToWrite.setRefreshToken(refreshToken);

        openIdContextToWrite.setExpiresIn(AccessTokenImplTest.ONE_HOUR);

        /*
         * Serialize the object to a file.
         */
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(filename))) {
            output.writeObject(openIdContextToWrite);
        }
    }
};
