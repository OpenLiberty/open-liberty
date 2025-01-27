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
 * Rejection of invalid characters in header names per RFC 7230.
 * Handling of extended ASCII characters.
 * Handling of null, empty, and whitespace leading/trailing fields.
 * Enforcement of a configurable header field size limit.
 * Handling of header folding
 * Proper normalization of fields (lowercase or trimming when applicable).
 */
public class HeaderValidatorTests {

    private HttpChannelConfig config;
    private final static char SPACE = ' ';
    private static String boundary;
    private final FieldType NAME = FieldType.NAME;
    private final FieldType VALUE = FieldType.VALUE;

    @Before
    public void setup(){
        config = mock(HttpChannelConfig.class);
        when(config.getLimitOfFieldSize()).thenReturn(100);
        when(config.isHeaderValidationEnabled()).thenReturn(true);

        char[] chars = new char[100];
        Arrays.fill(chars, 'a');
        boundary = new String(chars);
    }

    @Test
    public void testProcessValidHeaderNameNormalized() {
        String token = "Content-Type";
        String result = HeaderValidator.process(token, NAME, config);
        assertThat(result, is("content-type"));
    }

    

    @Test
    public void testValidHeaderValue() {
        String token = "text/html; charset=UTF-8";
        String result = HeaderValidator.process(token, VALUE, config);
        assertThat(result, is(token));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyNameTokenThrows(){
        HeaderValidator.process("", NAME, config);
    }

    @Test
    public void testEmptyValueToken() {
        String result = HeaderValidator.process(null, VALUE, config);
        assertThat(result, is(""));
    }

    @Test
    public void testValidHeaderNameNormalization() {
        String token = " X-CUSTOM-HEADER ";
        String result = HeaderValidator.process(token, NAME, config);
        assertThat(result, is("x-custom-header"));
        //Should trim and lowercase a header name during normalization
    }

    @Test
    public void testValidHeaderValueNormalization() {
        String token = " X-CUSTOM-VALUE ";
        String result = HeaderValidator.process(token, VALUE, config);
        assertThat(result, is(token.trim()));
        //Should trim but not lowercase a header value during normalization
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateHeaderNameExceedsMaxLength() {
        String token = boundary+"1"; // Exceeds max length (100)
        HeaderValidator.process(token, NAME, config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateHeaderValueExceedsMaxLength() {
        String token = boundary+ "1"; // Exceeds max length (100)
        HeaderValidator.process(token, VALUE, config);
    }

    @Test 
    public void testValidateTokenExceedsMaxLengthWithTrailingWhitespace(){
        String token = boundary + " ";
        String result = HeaderValidator.process(token, NAME, config);
        assertThat(result, is(boundary));
        //No exception should be thrown since, after trimming, header field is not 
        //larger than limit size.
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidHeaderNameCharacters() {
        String token = "Invalid@Name";
        HeaderValidator.process(token, NAME, config);
    }

    @Test 
    public void testValidationDisableSkips(){
        when(config.isHeaderValidationEnabled()).thenReturn(false);
        String token = "Invalid@Name";
        String result = HeaderValidator.process(token, NAME, config);
        assertThat(result, is("invalid@name"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCRButNoLF(){
        HeaderValidator.process("CRChar\rNoLF", VALUE, config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLFNotFollowedBySpace(){
        HeaderValidator.process("LFChar\nNoSpace", VALUE, config);
    }

    @Test 
    public void testCRLFFollowedBySpaceIsFormatted(){
        // CRLF followed by space is valid; CRLF should be replaced with whitespace
        String token = "foo\r\n bar";
        String result = HeaderValidator.process(token, VALUE, config);
        assertThat(result, is("foo"+SPACE+SPACE+SPACE+"bar"));
    }

    @Test 
    public void testCRLFFollowedByTabIsFormatted(){
        // CRLF followed by tab is valid; CRLF should be replaced with whitespace
        String token = "foo\r\n\tbar";
        String result = HeaderValidator.process(token, VALUE, config);
        assertThat(result, is("foo"+SPACE+SPACE+"\tbar"));
    }


    @Test
    public void testPritableASCII() {
        String token = "!#$%&'*+-.^_`|~09AZaz"; 
        String result = HeaderValidator.process(token, HeaderValidator.FieldType.VALUE, config);
        assertThat(result, is(token));
    }

    @Test
    public void testExtendedASCIICharactersInHeaderValue() {
        String token = "NonASCIIValue\u00E9"; // Contains non-ASCII character (é)
        String result = HeaderValidator.process(token, HeaderValidator.FieldType.VALUE, config);
        assertThat(result, is(token));
    }

    @Test
    public void testHeaderValueWithValidControlCharacters() {
        String token = "Valid\tHeaderValue"; // Contains horizontal tab (0x09), which is allowed
        String result = HeaderValidator.process(token, HeaderValidator.FieldType.VALUE, config);
        assertThat(result, is(token));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullHeaderNameThrowsException() {
        HeaderValidator.process(null, HeaderValidator.FieldType.NAME, config);
    }

    @Test 
    public void testMaskedCRLFReplacedWithQuestionMark(){
        // Suppose there's a unicode char whose low byte is either 0d or 0a
        // For instance, 0x010a or 0x030d
        // Code is expected to replace character with '?'
        char badLFChar = (char) 0x010a;
        char badCRChar = (char) 0x030d;

        String token = "foo"+badLFChar+"bar"+badCRChar+"baz";
        String result = HeaderValidator.process(token, FieldType.VALUE, config);
        assertThat(result, is("foo?bar?baz"));
    }
}
