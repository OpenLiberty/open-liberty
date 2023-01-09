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
package io.openliberty.security.jakartasec.tokens;

import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.ADDRESS;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.BIRTHDATE;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.EMAIL;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.EMAIL_VERIFIED;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.FAMILY_NAME;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.GENDER;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.GIVEN_NAME;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.LOCALE;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.MIDDLE_NAME;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.NAME;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.NICKNAME;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.PHONE_NUMBER;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.PHONE_NUMBER_VERIFIED;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.PICTURE;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.PREFERRED_USERNAME;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.PROFILE;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.SUBJECT_IDENTIFIER;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.UPDATED_AT;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.WEBSITE;
import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.ZONEINFO;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.security.enterprise.identitystore.openid.OpenIdClaims;

public class OpenIdClaimsImplTest {

    private static final String STRING_VALUE = "string value";

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetSubject() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(SUBJECT_IDENTIFIER, STRING_VALUE);

        assertUserinfoClaim(SUBJECT_IDENTIFIER, STRING_VALUE, openIdClaims.getSubject());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSubject_missing() throws InvalidJwtException {
        Map<String, Object> emptyClaimsMap = Collections.emptyMap();
        OpenIdClaims openIdClaims = new OpenIdClaimsImpl(emptyClaimsMap);

        openIdClaims.getSubject();
    }

    @Test
    public void testGetName() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(NAME, STRING_VALUE);

        assertUserinfoClaim(NAME, STRING_VALUE, openIdClaims.getName().get());
    }

    @Test
    public void testGetFamilyName() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(FAMILY_NAME, STRING_VALUE);

        assertUserinfoClaim(FAMILY_NAME, STRING_VALUE, openIdClaims.getFamilyName().get());
    }

    @Test
    public void testGetGivenName() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(GIVEN_NAME, STRING_VALUE);

        assertUserinfoClaim(GIVEN_NAME, STRING_VALUE, openIdClaims.getGivenName().get());
    }

    @Test
    public void testGetMiddleName() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(MIDDLE_NAME, STRING_VALUE);

        assertUserinfoClaim(MIDDLE_NAME, STRING_VALUE, openIdClaims.getMiddleName().get());
    }

    @Test
    public void testGetNickname() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(NICKNAME, STRING_VALUE);

        assertUserinfoClaim(NICKNAME, STRING_VALUE, openIdClaims.getNickname().get());
    }

    @Test
    public void testGetPreferredUsername() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(PREFERRED_USERNAME, STRING_VALUE);

        assertUserinfoClaim(PREFERRED_USERNAME, STRING_VALUE, openIdClaims.getPreferredUsername().get());
    }

    @Test
    public void testGetProfile() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(PROFILE, STRING_VALUE);

        assertUserinfoClaim(PROFILE, STRING_VALUE, openIdClaims.getProfile().get());
    }

    @Test
    public void testGetPicture() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(PICTURE, STRING_VALUE);

        assertUserinfoClaim(PICTURE, STRING_VALUE, openIdClaims.getPicture().get());
    }

    @Test
    public void testGetGender() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(GENDER, STRING_VALUE);

        assertUserinfoClaim(GENDER, STRING_VALUE, openIdClaims.getGender().get());
    }

    @Test
    public void testGetBirthdate() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(BIRTHDATE, STRING_VALUE);

        assertUserinfoClaim(BIRTHDATE, STRING_VALUE, openIdClaims.getBirthdate().get());
    }

    @Test
    public void testGetZoneinfo() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(ZONEINFO, STRING_VALUE);

        assertUserinfoClaim(ZONEINFO, STRING_VALUE, openIdClaims.getZoneinfo().get());
    }

    @Test
    public void testGetLocale() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(LOCALE, STRING_VALUE);

        assertUserinfoClaim(LOCALE, STRING_VALUE, openIdClaims.getLocale().get());
    }

    @Test
    public void testGetUpdatedAt() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(UPDATED_AT, STRING_VALUE);

        assertUserinfoClaim(UPDATED_AT, STRING_VALUE, openIdClaims.getUpdatedAt().get());
    }

    @Test
    public void testGetEmail() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(EMAIL, STRING_VALUE);

        assertUserinfoClaim(EMAIL, STRING_VALUE, openIdClaims.getEmail().get());
    }

    @Test
    public void testGetEmailVerified() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(EMAIL_VERIFIED, STRING_VALUE);

        assertUserinfoClaim(EMAIL_VERIFIED, STRING_VALUE, openIdClaims.getEmailVerified().get());
    }

    @Test
    public void testGetAddress() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(ADDRESS, STRING_VALUE);

        assertUserinfoClaim(ADDRESS, STRING_VALUE, openIdClaims.getAddress().get());
    }

    @Test
    public void testGetPhoneNumber() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(PHONE_NUMBER, STRING_VALUE);

        assertUserinfoClaim(PHONE_NUMBER, STRING_VALUE, openIdClaims.getPhoneNumber().get());
    }

    @Test
    public void testGetPhoneNumberVerified() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(PHONE_NUMBER_VERIFIED, STRING_VALUE);

        assertUserinfoClaim(PHONE_NUMBER_VERIFIED, STRING_VALUE, openIdClaims.getPhoneNumberVerified().get());
    }

    @Test
    public void testGetWebsite() throws InvalidJwtException {
        OpenIdClaims openIdClaims = createOpenIdClaimsWithStringClaim(WEBSITE, STRING_VALUE);

        assertUserinfoClaim(WEBSITE, STRING_VALUE, openIdClaims.getWebsite().get());
    }

    public static OpenIdClaims createOpenIdClaimsWithStringClaim(String name, String value) throws InvalidJwtException {
        Map<String, Object> claimsMapWithString = org.jose4j.jwt.JwtClaims.parse("{\"" + name + "\":\"" + value + "\"}").getClaimsMap();
        return new OpenIdClaimsImpl(claimsMapWithString);
    }

    public void assertUserinfoClaim(String name, String expectedValue, String actualValue) throws InvalidJwtException {
        assertEquals("The '" + name + "' claim must be set.", expectedValue, actualValue);
    }

}
