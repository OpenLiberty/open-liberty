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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.Test;

import jakarta.security.enterprise.identitystore.openid.Claims;

public class ClaimsImplTest {

    private static final String STRING_CLAIM = "string";
    private static final String STRING_VALUE = "string value";
    private static final String NUMERIC_DATE_CLAIM = "instant";
    private static final String NUMERIC_DATE_VALUE_STRING = String.valueOf(Instant.now().getEpochSecond());
    private static final String ARRAY_STRING_CLAIM = "arrayString";
    private static final String ARRAY_STRING_VALUE_1 = "arrayStringValue1";
    private static final String ARRAY_STRING_VALUE_2 = "arrayStringValue2";
    private static final String INT_CLAIM = "integer";
    private static final int INT_VALUE = 10;
    private static final String INT_CLAIM_VALUE_STRING = Integer.toString(INT_VALUE);
    private static final OptionalInt OPTIONAL_INT_VALUE = OptionalInt.of(INT_VALUE);
    private static final String LONG_CLAIM = "long";
    private static final long LONG_VALUE = 10;
    private static final String LONG_CLAIM_VALUE_STRING = Long.toString(LONG_VALUE);
    private static final OptionalLong OPTIONAL_LONG_VALUE = OptionalLong.of(LONG_VALUE);
    private static final String DOUBLE_CLAIM = "double";
    private static final double DOUBLE_VALUE = 9.9999;
    private static final String DOUBLE_CLAIM_VALUE_STRING = Double.toString(DOUBLE_VALUE);
    private static final OptionalDouble OPTIONAL_DOUBLE_VALUE = OptionalDouble.of(DOUBLE_VALUE);
    private static final String NESTED_CLAIM = "nested";
    private static final String NOT_A_NUMBER = "notANumber";

    private final Map<String, Object> emptyClaimsMap = Collections.emptyMap();

    private final Claims emptyClaims = new ClaimsImpl(emptyClaimsMap);

    @Test
    public void testGetStringClaim() throws InvalidJwtException {
        Claims claims = createClaimsWithStringClaim(STRING_CLAIM, STRING_VALUE);

        Optional<String> optionalString = claims.getStringClaim(STRING_CLAIM);

        assertEquals("The String value must be set.", STRING_VALUE, optionalString.get());
    }

    @Test
    public void testGetStringClaim_empty() {
        Optional<String> expectedValue = Optional.empty();

        Optional<String> optionalString = emptyClaims.getStringClaim(STRING_CLAIM);

        assertEquals("The string value must be empty optional.", expectedValue, optionalString);
    }

    @Test
    public void testGetNumericDateClaim() throws InvalidJwtException {
        Claims claims = createClaimsWithNumericClaim(NUMERIC_DATE_CLAIM, NUMERIC_DATE_VALUE_STRING);

        Optional<Instant> optionalInstant = claims.getNumericDateClaim(NUMERIC_DATE_CLAIM);

        assertEquals("The Instant value must be set.", Instant.ofEpochSecond(Long.parseLong(NUMERIC_DATE_VALUE_STRING)), optionalInstant.get());
    }

    @Test
    public void testGetNumericDateClaim_empty() {
        Optional<Instant> expectedValue = Optional.empty();

        Optional<Instant> optionalInstant = emptyClaims.getNumericDateClaim(NUMERIC_DATE_CLAIM);

        assertEquals("The Instant value must be empty optional.", expectedValue, optionalInstant);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNumericDateClaim_notANumber() throws InvalidJwtException {
        Claims claims = createClaimsWithStringClaim(NUMERIC_DATE_CLAIM, NOT_A_NUMBER);

        claims.getNumericDateClaim(NUMERIC_DATE_CLAIM);
    }

    @Test
    public void testGetArrayStringClaim() throws InvalidJwtException {
        Claims claims = createClaimsWithArrayStringClaim();

        List<String> arrayStringClaimValue = claims.getArrayStringClaim(ARRAY_STRING_CLAIM);

        assertTrue("The value must be set in list.", arrayStringClaimValue.contains(ARRAY_STRING_VALUE_1));
        assertTrue("The value must be set in list.", arrayStringClaimValue.contains(ARRAY_STRING_VALUE_2));
    }

    @Test
    public void testGetArrayStringClaim_immutable() throws InvalidJwtException {
        Claims claims = createClaimsWithArrayStringClaim();

        List<String> arrayStringClaimValue = claims.getArrayStringClaim(ARRAY_STRING_CLAIM);

        assertTrue("The value must be set in list.", arrayStringClaimValue.contains(ARRAY_STRING_VALUE_1));
        assertTrue("The value must be set in list.", arrayStringClaimValue.contains(ARRAY_STRING_VALUE_2));

        arrayStringClaimValue.add(STRING_VALUE);

        List<String> anotherArrayStringClaimValue = claims.getArrayStringClaim(ARRAY_STRING_CLAIM);
        assertFalse("The value must not be set in list.", anotherArrayStringClaimValue.contains(STRING_VALUE));
    }

    @Test
    public void testGetArrayStringClaim_empty() {
        List<String> arrayStringClaimValue = emptyClaims.getArrayStringClaim(ARRAY_STRING_CLAIM);

        assertTrue("The list must be empty.", arrayStringClaimValue.isEmpty());
    }

    @Test
    public void testGetIntClaim() throws InvalidJwtException {
        Claims claims = createClaimsWithNumericClaim(INT_CLAIM, INT_CLAIM_VALUE_STRING);

        OptionalInt optionalInt = claims.getIntClaim(INT_CLAIM);

        assertEquals("The integer value must be set.", OPTIONAL_INT_VALUE, optionalInt);
    }

    @Test
    public void testGetIntClaim_empty() {
        OptionalInt expectedValue = OptionalInt.empty();

        OptionalInt optionalInt = emptyClaims.getIntClaim(INT_CLAIM);

        assertEquals("The integer value must be empty optional.", expectedValue, optionalInt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetIntClaim_notANumber() throws InvalidJwtException {
        Claims claims = createClaimsWithStringClaim(INT_CLAIM, NOT_A_NUMBER);

        claims.getIntClaim(INT_CLAIM);
    }

    @Test
    public void testGetLongClaim() throws InvalidJwtException {
        Claims claims = createClaimsWithNumericClaim(LONG_CLAIM, LONG_CLAIM_VALUE_STRING);

        OptionalLong optionalLong = claims.getLongClaim(LONG_CLAIM);

        assertEquals("The long value must be set.", OPTIONAL_LONG_VALUE, optionalLong);
    }

    @Test
    public void testGetLongClaim_empty() {
        OptionalLong expectedValue = OptionalLong.empty();

        OptionalLong optionalLong = emptyClaims.getLongClaim(LONG_CLAIM);

        assertEquals("The long value must be empty optional.", expectedValue, optionalLong);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetLongClaim_notANumber() throws InvalidJwtException {
        Claims claims = createClaimsWithStringClaim(LONG_CLAIM, NOT_A_NUMBER);

        claims.getLongClaim(LONG_CLAIM);
    }

    @Test
    public void testGetDoubleClaim() throws InvalidJwtException {
        Claims claims = createClaimsWithNumericClaim(DOUBLE_CLAIM, DOUBLE_CLAIM_VALUE_STRING);

        OptionalDouble optionalDouble = claims.getDoubleClaim(DOUBLE_CLAIM);

        assertEquals("The double value must be set.", OPTIONAL_DOUBLE_VALUE, optionalDouble);
    }

    @Test
    public void testGetDoubleClaim_empty() {
        OptionalDouble expectedValue = OptionalDouble.empty();

        OptionalDouble optionalDouble = emptyClaims.getDoubleClaim(DOUBLE_CLAIM);

        assertEquals("The double value must be empty optional.", expectedValue, optionalDouble);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDoubleClaim_notANumber() throws InvalidJwtException {
        Claims claims = createClaimsWithStringClaim(DOUBLE_CLAIM, NOT_A_NUMBER);

        claims.getDoubleClaim(DOUBLE_CLAIM);
    }

    @Test
    public void testGetNestedClaim() throws InvalidJwtException {
        Map<String, Object> claimsMap = org.jose4j.jwt.JwtClaims.parse("{\"" + NESTED_CLAIM + "\":" + "{\"" + STRING_CLAIM + "\":\"" + STRING_VALUE + "\"}}").getClaimsMap();
        ClaimsImpl claims = new ClaimsImpl(claimsMap);

        Optional<Claims> optionalNested = claims.getNested(NESTED_CLAIM);
        Claims nestedClaims = optionalNested.get();

        assertNotNull("The nested claims value must be set.", nestedClaims);
        assertEquals("Values in the nested claims must be set.", STRING_VALUE, nestedClaims.getStringClaim(STRING_CLAIM).get());
    }

    @Test
    public void testGetNestedClaim_empty() {
        Optional<Claims> expectedValue = Optional.empty();

        Optional<Claims> optionalNested = emptyClaims.getNested(NESTED_CLAIM);

        assertEquals("The nested value must be empty optional.", expectedValue, optionalNested);
    }

    private Claims createClaimsWithStringClaim(String name, String value) throws InvalidJwtException {
        Map<String, Object> claimsMapWithString = org.jose4j.jwt.JwtClaims.parse("{\"" + name + "\":\"" + value + "\"}").getClaimsMap();
        return new ClaimsImpl(claimsMapWithString);
    }

    private Claims createClaimsWithNumericClaim(String name, String value) throws InvalidJwtException {
        Map<String, Object> claimsMap = org.jose4j.jwt.JwtClaims.parse("{\"" + name + "\":" + value + "}").getClaimsMap();
        return new ClaimsImpl(claimsMap);
    }

    private Claims createClaimsWithArrayStringClaim() throws InvalidJwtException {
        Map<String, Object> claimsMapWithArrayString = org.jose4j.jwt.JwtClaims.parse("{\"" + ARRAY_STRING_CLAIM + "\":[\"" + ARRAY_STRING_VALUE_1 + "\",\"" + ARRAY_STRING_VALUE_2
                                                                                      + "\"]}").getClaimsMap();
        return new ClaimsImpl(claimsMapWithArrayString);
    }

}
