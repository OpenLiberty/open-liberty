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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.Test;

import io.openliberty.security.jakartasec.JakartaSec30Constants;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.identitystore.openid.JwtClaims;

public class JwtClaimsImplTest {

    private static final String STRING_VALUE = "aStringValue";
    private static final String ARRAY_STRING_VALUE_1 = "arrayStringValue1";
    private static final String ARRAY_STRING_VALUE_2 = "arrayStringValue2";
    private static final Instant NOW_INSTANT = Instant.now();
    private static final String NUMERIC_DATE_VALUE_STRING = String.valueOf(NOW_INSTANT.getEpochSecond());
    private static final String NUMERIC_DATE_ONE_HOUR_FROM_NOW_VALUE_STRING = String.valueOf(NOW_INSTANT.plus(Duration.ofHours(1)).getEpochSecond());
    private static final String NUMERIC_DATE_10_MINUTES_FROM_NOW_VALUE_STRING = String.valueOf(NOW_INSTANT.plus(Duration.ofMinutes(10)).getEpochSecond());
    private static final String NUMERIC_DATE_10_MINUTES_BEFORE_NOW_VALUE_STRING = String.valueOf(NOW_INSTANT.minus(Duration.ofMinutes(10)).getEpochSecond());

    private final Map<String, Object> emptyClaimsMap = Collections.emptyMap();
    private final JwtClaims emptyJwtClaims = new JwtClaimsImpl(emptyClaimsMap);

    @Test
    public void testGetIssuer() throws InvalidJwtException {
        JwtClaims jwtClaims = createJwtClaimsWithStringClaim(OpenIdConstant.ISSUER_IDENTIFIER, STRING_VALUE);

        Optional<String> optionalString = jwtClaims.getIssuer();

        assertEquals("The value of the 'iss' claim must be set.", STRING_VALUE, optionalString.get());
    }

    @Test
    public void testGetSubject() throws InvalidJwtException {
        JwtClaims jwtClaims = createJwtClaimsWithStringClaim(OpenIdConstant.SUBJECT_IDENTIFIER, STRING_VALUE);

        Optional<String> optionalString = jwtClaims.getSubject();

        assertEquals("The value of the 'sub' claim must be set.", STRING_VALUE, optionalString.get());
    }

    @Test
    public void testGetAudience() throws InvalidJwtException {
        Map<String, Object> claimsMap = org.jose4j.jwt.JwtClaims.parse("{\"" + OpenIdConstant.AUDIENCE + "\":[\"" + ARRAY_STRING_VALUE_1 + "\",\"" + ARRAY_STRING_VALUE_2
                                                                       + "\"]}").getClaimsMap();
        JwtClaims jwtClaims = new JwtClaimsImpl(claimsMap);

        List<String> audienceClaimValue = jwtClaims.getAudience();

        assertTrue("The value must be set in the 'aud' claim.", audienceClaimValue.contains(ARRAY_STRING_VALUE_1));
        assertTrue("The value must be set in the 'aud' claim.", audienceClaimValue.contains(ARRAY_STRING_VALUE_2));
    }

    @Test
    public void testGetExpirationTime() throws InvalidJwtException {
        JwtClaims jwtClaims = createJwtClaimsWithNumericClaim(OpenIdConstant.EXPIRATION_IDENTIFIER, NUMERIC_DATE_VALUE_STRING);

        Optional<Instant> optionalInstant = jwtClaims.getExpirationTime();

        assertEquals("The value of the 'exp' claim must be set.", Instant.ofEpochSecond(Long.parseLong(NUMERIC_DATE_VALUE_STRING)), optionalInstant.get());
    }

    @Test
    public void testIsExpired_expired() throws InvalidJwtException, InterruptedException {
        JwtClaims jwtClaims = createJwtClaimsWithNumericClaim(OpenIdConstant.EXPIRATION_IDENTIFIER, NUMERIC_DATE_VALUE_STRING);
        Clock clock = Clock.systemUTC();
        boolean required = false;
        Duration skew = Duration.ZERO;
        Thread.sleep(2);

        assertTrue("The jwt must be expired when 'exp' claim is already past and there is no clock skew.", jwtClaims.isExpired(clock, required, skew));
    }

    @Test
    public void testIsExpired_skew() throws InvalidJwtException, InterruptedException {
        JwtClaims jwtClaims = createJwtClaimsWithNumericClaim(OpenIdConstant.EXPIRATION_IDENTIFIER, NUMERIC_DATE_VALUE_STRING);
        Clock clock = Clock.systemUTC();
        boolean required = false;
        Duration skew = Duration.ofMinutes(5);
        Thread.sleep(2);

        assertFalse("The jwt must not be expired when 'exp' claim is within the clock skew.", jwtClaims.isExpired(clock, required, skew));
    }

    @Test
    public void testIsExpired_notExpired() throws InvalidJwtException, InterruptedException {
        JwtClaims jwtClaims = createJwtClaimsWithNumericClaim(OpenIdConstant.EXPIRATION_IDENTIFIER, NUMERIC_DATE_ONE_HOUR_FROM_NOW_VALUE_STRING);
        Clock clock = Clock.systemUTC();
        boolean required = false;
        Duration skew = Duration.ZERO;

        assertFalse("The jwt must not be expired when 'exp' claim is in the future.", jwtClaims.isExpired(clock, required, skew));
    }

    @Test
    public void testIsExpired_required() throws InvalidJwtException, InterruptedException {
        Clock clock = Clock.systemUTC();
        boolean required = true;
        Duration skew = Duration.ZERO;

        assertTrue("The jwt must be expired when 'exp' claim is missing and required is true.", emptyJwtClaims.isExpired(clock, required, skew));
    }

    @Test
    public void testIsExpired_notRequired() throws InvalidJwtException, InterruptedException {
        Clock clock = Clock.systemUTC();
        boolean required = false;
        Duration skew = Duration.ZERO;

        assertFalse("The jwt must not be expired when 'exp' claim is missing and required is false.", emptyJwtClaims.isExpired(clock, required, skew));
    }

    @Test
    public void testNotBeforeTime() throws InvalidJwtException {
        JwtClaims jwtClaims = createJwtClaimsWithNumericClaim(JakartaSec30Constants.NOT_BEFORE_IDENTIFIER, NUMERIC_DATE_VALUE_STRING);

        Optional<Instant> optionalInstant = jwtClaims.getNotBeforeTime();

        assertEquals("The value of the 'nbf' claim must be set.", Instant.ofEpochSecond(Long.parseLong(NUMERIC_DATE_VALUE_STRING)), optionalInstant.get());
    }

    @Test
    public void testIsBeforeValidity_before() throws InvalidJwtException, InterruptedException {
        JwtClaims jwtClaims = createJwtClaimsWithNumericClaim(JakartaSec30Constants.NOT_BEFORE_IDENTIFIER, NUMERIC_DATE_ONE_HOUR_FROM_NOW_VALUE_STRING);
        Clock clock = Clock.systemUTC();
        boolean required = false;
        Duration skew = Duration.ZERO;

        assertTrue("The jwt must be before validity when 'nbf' claim is in the future.", jwtClaims.isBeforeValidity(clock, required, skew));
    }

    @Test
    public void testIsBeforeValidity_after() throws InvalidJwtException, InterruptedException {
        JwtClaims jwtClaims = createJwtClaimsWithNumericClaim(JakartaSec30Constants.NOT_BEFORE_IDENTIFIER, NUMERIC_DATE_VALUE_STRING);
        Clock clock = Clock.systemUTC();
        boolean required = false;
        Duration skew = Duration.ZERO;
        Thread.sleep(2);

        assertFalse("The jwt must not be before validity when 'nbf' claim is already past and there is no clock skew.", jwtClaims.isBeforeValidity(clock, required, skew));
    }

    @Test
    public void testIsBeforeValidity_outsideSkew() throws InvalidJwtException, InterruptedException {
        JwtClaims jwtClaims = createJwtClaimsWithNumericClaim(JakartaSec30Constants.NOT_BEFORE_IDENTIFIER, NUMERIC_DATE_10_MINUTES_FROM_NOW_VALUE_STRING);
        Clock clock = Clock.systemUTC();
        boolean required = false;
        Duration skew = Duration.ofMinutes(5);

        assertTrue("The jwt must be before validity when 'nbf' claim is not within the clock skew.", jwtClaims.isBeforeValidity(clock, required, skew));
    }

    @Test
    public void testIsBeforeValidity_withinSkew() throws InvalidJwtException, InterruptedException {
        JwtClaims jwtClaims = createJwtClaimsWithNumericClaim(JakartaSec30Constants.NOT_BEFORE_IDENTIFIER, NUMERIC_DATE_10_MINUTES_FROM_NOW_VALUE_STRING);
        Clock clock = Clock.systemUTC();
        boolean required = false;
        Duration skew = Duration.ofMinutes(20);

        assertFalse("The jwt must not be before validity when 'nbf' claim is within the clock skew.", jwtClaims.isBeforeValidity(clock, required, skew));
    }

    @Test
    public void testIsBeforeValidity_required() throws InvalidJwtException, InterruptedException {
        Clock clock = Clock.systemUTC();
        boolean required = true;
        Duration skew = Duration.ZERO;

        assertTrue("The jwt must be before validity when 'nbf' claim is missing and required is true.", emptyJwtClaims.isBeforeValidity(clock, required, skew));
    }

    @Test
    public void testIsBeforeValidity_notRequired() throws InvalidJwtException, InterruptedException {
        Clock clock = Clock.systemUTC();
        boolean required = false;
        Duration skew = Duration.ZERO;

        assertFalse("The jwt must not be before validity when 'nbf' claim is missing and required is false.", emptyJwtClaims.isBeforeValidity(clock, required, skew));
    }

    @Test
    public void testIsValid_afterNbf_notExpired_valid() throws InvalidJwtException, InterruptedException {
        JwtClaims jwtClaims = createJwtClaimsForValidityTests(NUMERIC_DATE_VALUE_STRING, NUMERIC_DATE_ONE_HOUR_FROM_NOW_VALUE_STRING);

        assertTrue("The jwt must be valid when 'nbf' claim is already past and 'exp' claim is in the future.", jwtClaims.isValid());
    }

    @Test
    public void testIsValid_afterNbf_expired_notValid() throws InvalidJwtException, InterruptedException {
        JwtClaims jwtClaims = createJwtClaimsForValidityTests(NUMERIC_DATE_VALUE_STRING, NUMERIC_DATE_10_MINUTES_BEFORE_NOW_VALUE_STRING);

        assertFalse("The jwt must not be valid when 'exp' claim is already past and is not within a 1 minute clock skew.", jwtClaims.isValid());
    }

    @Test
    public void testIsValid_beforeNbf_notExpired_notValid() throws InvalidJwtException, InterruptedException {
        JwtClaims jwtClaims = createJwtClaimsForValidityTests(NUMERIC_DATE_ONE_HOUR_FROM_NOW_VALUE_STRING, NUMERIC_DATE_ONE_HOUR_FROM_NOW_VALUE_STRING);

        assertFalse("The jwt must not be valid when 'nbf' claim is in the future and is not within a 1 minute clock skew.", jwtClaims.isValid());
    }

    @Test
    public void testGetIssuedAt() throws InvalidJwtException {
        JwtClaims jwtClaims = createJwtClaimsWithNumericClaim(JakartaSec30Constants.ISSUED_AT_IDENTIFIER, NUMERIC_DATE_VALUE_STRING);

        Optional<Instant> optionalInstant = jwtClaims.getIssuedAt();

        assertEquals("The value of the 'iat' claim must be set.", Instant.ofEpochSecond(Long.parseLong(NUMERIC_DATE_VALUE_STRING)), optionalInstant.get());
    }

    @Test
    public void testGetJwtId() throws InvalidJwtException {
        JwtClaims jwtClaims = createJwtClaimsWithStringClaim(JakartaSec30Constants.JWT_ID_IDENTIFIER, STRING_VALUE);

        Optional<String> optionalString = jwtClaims.getJwtId();

        assertEquals("The value of the 'jti' claim must be set.", STRING_VALUE, optionalString.get());
    }

    private JwtClaims createJwtClaimsWithStringClaim(String name, String value) throws InvalidJwtException {
        Map<String, Object> claimsMapWithString = org.jose4j.jwt.JwtClaims.parse("{\"" + name + "\":\"" + value + "\"}").getClaimsMap();
        return new JwtClaimsImpl(claimsMapWithString);
    }

    private JwtClaims createJwtClaimsWithNumericClaim(String name, String value) throws InvalidJwtException {
        Map<String, Object> claimsMap = org.jose4j.jwt.JwtClaims.parse("{\"" + name + "\":" + value + "}").getClaimsMap();
        return new JwtClaimsImpl(claimsMap);
    }

    private JwtClaims createJwtClaimsForValidityTests(String nbfValue, String expValue) throws InvalidJwtException {
        Map<String, Object> claimsMap = org.jose4j.jwt.JwtClaims.parse("{\"" + JakartaSec30Constants.NOT_BEFORE_IDENTIFIER + "\":" + nbfValue + ",\""
                                                                       + OpenIdConstant.EXPIRATION_IDENTIFIER + "\":" + expValue
                                                                       + "}").getClaimsMap();
        return new JwtClaimsImpl(claimsMap);
    }

}
