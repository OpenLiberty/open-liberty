/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot.archive;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * Unit tests for the sensitive-value filtering regexes used in
 * {@link FilteredDirEntryConfig}.
 */
public class FilteredDirEntryConfigRegexTest {

    @Rule
    public TestName testName = new TestName();

    private static final String FILTER_REGEX = "\"(\\{aes\\}|\\{xor\\}).*\"";
    private static final String WLP_PASSWORD_ENCRYPTION_REGEX = "wlp\\.password\\.encryption\\.key=.*$";
    private static final String WLP_AES_ENCRYPTION_REGEX = "wlp\\.aes\\.encryption\\.key=.*$";
    private static final String OBSCURED_VALUE = "\"*****\"";

    private final Pattern obscuredValuePattern = Pattern.compile(FILTER_REGEX);
    private final Pattern wlpPasswordEncryptionPattern = Pattern.compile(WLP_PASSWORD_ENCRYPTION_REGEX, Pattern.MULTILINE);
    private final Pattern wlpAesEncryptionPattern = Pattern.compile(WLP_AES_ENCRYPTION_REGEX, Pattern.MULTILINE);

    @Test
    public void testAesEncodedValueIsObscured() {
        String input = "password=\"{aes}AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRob\"";
        String result = obscuredValuePattern.matcher(input).replaceAll(OBSCURED_VALUE);
        assertTrue("AES-encoded password should be replaced with *****", result.contains("*****"));
        assertFalse("Original AES value should not remain in output", result.contains("{aes}"));
    }

    @Test
    public void testAesEncodedValueShortToken() {
        String input = "keyStore password=\"{aes}abc123\"";
        String result = obscuredValuePattern.matcher(input).replaceAll(OBSCURED_VALUE);
        assertTrue("Short AES token should be replaced", result.contains("*****"));
        assertFalse("Original AES token should not remain", result.contains("{aes}"));
    }

    @Test
    public void testXorEncodedValueIsObscured() {
        String input = "password=\"{xor}Lz4sLCgwLTs=\"";
        String result = obscuredValuePattern.matcher(input).replaceAll(OBSCURED_VALUE);
        assertTrue("XOR-encoded password should be replaced with *****", result.contains("*****"));
        assertFalse("Original XOR value should not remain in output", result.contains("{xor}"));
    }

    @Test
    public void testXorEncodedValueShortToken() {
        String input = "password=\"{xor}xy\"";
        String result = obscuredValuePattern.matcher(input).replaceAll(OBSCURED_VALUE);
        assertTrue("Short XOR token should be replaced", result.contains("*****"));
        assertFalse("Original XOR token should not remain", result.contains("{xor}"));
    }

    @Test
    public void testPlainPasswordNotObscured() {
        String input = "password=\"myPlainPassword\"";
        String result = obscuredValuePattern.matcher(input).replaceAll(OBSCURED_VALUE);
        assertTrue("Plain password value should remain unchanged", result.contains("myPlainPassword"));
    }

    @Test
    public void testEmptyValueNotObscured() {
        String input = "password=\"\"";
        String result = obscuredValuePattern.matcher(input).replaceAll(OBSCURED_VALUE);
        assertTrue("Empty password value should remain unchanged", result.contains("\"\""));
    }

    @Test
    public void testWlpPasswordEncryptionKeyIsObscured() {
        String input = "wlp.password.encryption.key=mysecretkey123";
        String result = wlpPasswordEncryptionPattern.matcher(input).replaceAll("wlp.password.encryption.key=*****");
        assertTrue("wlp.password.encryption.key value should be replaced", result.contains("*****"));
        assertFalse("Original key value should not remain", result.contains("mysecretkey123"));
    }

    @Test
    public void testWlpPasswordEncryptionKeyObscuredInMultilineContent() {
        String input = "some.other.property=value\nwlp.password.encryption.key=topsecret\nanother.property=foo";
        String result = wlpPasswordEncryptionPattern.matcher(input).replaceAll("wlp.password.encryption.key=*****");
        assertTrue("wlp.password.encryption.key should be replaced in multiline content", result.contains("*****"));
        assertFalse("Original secret should not remain", result.contains("topsecret"));
        assertTrue("Other properties should be untouched", result.contains("some.other.property=value"));
    }

    @Test
    public void testWlpAesEncryptionKeyIsObscured() {
        String input = "wlp.aes.encryption.key=myaeskey456";
        String result = wlpAesEncryptionPattern.matcher(input).replaceAll("wlp.aes.encryption.key=*****");
        assertTrue("wlp.aes.encryption.key value should be replaced", result.contains("*****"));
        assertFalse("Original AES key value should not remain", result.contains("myaeskey456"));
    }

    @Test
    public void testWlpAesEncryptionKeyObscuredInMultilineContent() {
        String input = "foo=bar\nwlp.aes.encryption.key=supersecretaes\nbaz=qux";
        String result = wlpAesEncryptionPattern.matcher(input).replaceAll("wlp.aes.encryption.key=*****");
        assertTrue("wlp.aes.encryption.key should be replaced in multiline content", result.contains("*****"));
        assertFalse("Original AES key should not remain", result.contains("supersecretaes"));
        assertTrue("Other properties should be untouched", result.contains("foo=bar"));
    }

    @Test
    public void testUnrelatedContentNotModified() {
        String input = "feature=mpHealth-4.0\nhttpEndpoint host=\"*\" httpPort=\"9080\"";
        String result = obscuredValuePattern.matcher(input).replaceAll(OBSCURED_VALUE);
        result = wlpPasswordEncryptionPattern.matcher(result).replaceAll("wlp.password.encryption.key=*****");
        result = wlpAesEncryptionPattern.matcher(result).replaceAll("wlp.aes.encryption.key=*****");
        assertTrue("Unrelated config content should remain unchanged", result.equals(input));
    }
}
