/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.netty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.beans.Transient;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;

import io.openliberty.http.netty.channel.utils.HeaderValidator;
import io.openliberty.http.netty.channel.utils.HeaderValidator.FieldType;

/**
 * Unit tests for the {@link HeaderValidator} class.
 * 
 * This class verifies the behavior of the header validator across multiple
 * scenarios and edge cases based on RFC 7230. Current testing coverage includes:
 * 
 * Validation of proper header names and values.
 * Rejection of invalid characters in either header names or values (per RFC 7230).
 * Handling of null, empty, and whitespace leading/trailing fields.
 * Enforcement of a configurable header field size limit.
 * Proper normalization of fields (lowercase or trimming when applicable).
 */
public class HeaderValidatorTests {

    private HttpChannelConfig config;
    private String boundary;

    @Before
    public void setup(){
        config = mock(HttpChannelConfig.class);
        when(config.getLimitOfFieldSize()).thenReturn(100);

        char[] chars = new char[100];
        Arrays.fill(chars, 'a');
        boundary = new String(chars);
    }

    @Test
    public void testProcessValidHeaderName() {
        String token = "Content-Type";
        String result = HeaderValidator.process(token, HeaderValidator.FieldType.NAME, config);
        assertThat(result, is("content-type"));
    }

    

    @Test
    public void testValidHeaderValue() {
        String token = "text/html; charset=UTF-8";
        String result = HeaderValidator.process(token, HeaderValidator.FieldType.VALUE, config);
        assertThat(result, is(token));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyNameToken(){
        HeaderValidator.process("", HeaderValidator.FieldType.NAME, config);
    }

    @Test
    public void testEmptyValueToken() {
        String result = HeaderValidator.process(null, HeaderValidator.FieldType.VALUE, config);
        assertThat(result, is(""));
    }

    @Test
    public void testValidHeaderNameNormalization() {
        String token = " X-CUSTOM-HEADER ";
        String result = HeaderValidator.process(token, HeaderValidator.FieldType.NAME, config);
        assertThat(result, is("x-custom-header"));
        //Should trim and lowercase a header name during normalization
    }

    @Test
    public void testValidHeaderValueNormalization() {
        String token = " X-CUSTOM-VALUE ";
        String result = HeaderValidator.process(token, HeaderValidator.FieldType.VALUE, config);
        assertThat(result, is(token.trim()));
        //Should trim but not lowercase a header value during normalization
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateHeaderNameExceedsMaxLength() {
        String token = boundary+"1"; // Exceeds max length (100)
        HeaderValidator.process(token, HeaderValidator.FieldType.NAME, config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateHeaderValueExceedsMaxLength() {
        String token = boundary+ "1"; // Exceeds max length (100)
        HeaderValidator.process(token, HeaderValidator.FieldType.VALUE, config);
    }

    @Test 
    public void testValidateTokenExceedsMaxLengthWithTrailingWhitespace(){
        String token = boundary + " ";
        String result = HeaderValidator.process(token, FieldType.NAME, config);
        assertThat(result, is(boundary));
        //No exception should be thrown since, after trimming, header field is not 
        //larger than limit size.
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidHeaderNameCharacters() {
        String token = "Invalid Header@Name!";
        HeaderValidator.process(token, HeaderValidator.FieldType.NAME, config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testControlCharactersInHeaderValue() {
        String token = "Invalid\u0001Value"; // Contains control character (0x01)
        HeaderValidator.process(token, HeaderValidator.FieldType.VALUE, config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonASCIICharactersInHeaderValue() {
        String token = "NonASCIIValue\u00E9"; // Contains non-ASCII character (é)
        HeaderValidator.process(token, HeaderValidator.FieldType.VALUE, config);
    }

    @Test
    public void testHeaderValueWithValidControlCharacters() {
        String token = "Valid\tHeaderValue"; // Contains horizontal tab (0x09), which is allowed
        String result = HeaderValidator.process(token, HeaderValidator.FieldType.VALUE, config);
        assertThat(result, is(token));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithNullHeaderNameThrowsException() {
        HeaderValidator.process(null, HeaderValidator.FieldType.NAME, config);
    }
}
