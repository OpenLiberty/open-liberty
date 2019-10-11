/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.common.internal.encoder.Base64Coder;

import test.common.SharedOutputManager;

/**
 *
 */
public class HashUtilsTest {
    private static final String CLIENT_ID = "client_id";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String SCOPE = "scope";
    private static final String RESOURCE_ID = "resource_id";
    private static final int KEY_LIFETIME_SECONDS = 60;

    private static SharedOutputManager outputMgr;

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    @Test
    public void testNoUrlsafeEncode() {
        SecureRandom sr = new SecureRandom();
        byte[] code = new byte[32];
        sr.nextBytes(code);

        String code_verifier = getEncoded(code);
        String code_challenge = getDigest(code_verifier, "nourlsafe");

        String challenge = HashUtils.encodedDigest(code_verifier, "SHA-256", "US-ASCII");

        org.junit.Assert.assertNotSame("code verifier is encoded non url safe", challenge, code_challenge);

    }

    @Test
    public void testEncodeUsingInternal() {
        SecureRandom sr = new SecureRandom();
        byte[] code = new byte[32];
        sr.nextBytes(code);

        String code_verifier = getEncodedUsingLocalEncoder(code);
        String code_challenge = getDigest(code_verifier, "local");

        String challenge = HashUtils.encodedDigest(code_verifier, "SHA-256", "US-ASCII");

        org.junit.Assert.assertNotSame("code verifier is encoded using local", challenge, code_challenge);

    }

    @Test
    public void testUrlsafeEncode() {
        SecureRandom sr = new SecureRandom();
        byte[] code = new byte[32];
        sr.nextBytes(code);

        String code_verifier = getUrlSafeEncoded(code);
        String code_challenge = getDigest(code_verifier, "urlsafe");

        String challenge = HashUtils.encodedDigest(code_verifier, "SHA-256", "US-ASCII");

        org.junit.Assert.assertEquals("code verifier is encoded and url safe and should be same", challenge, code_challenge);

    }

    private String getEncoded(byte[] input) {
        if (input != null) {
            return org.apache.commons.codec.binary.Base64.encodeBase64String(input);
        }
        return null;
    }

    private String getUrlSafeEncoded(byte[] input) {
        if (input != null) {
            return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(input);
        }
        return null;
    }

    private String getEncodedUsingLocalEncoder(byte[] input) {
        if (input != null) {
            return Base64Coder.base64EncodeToString(input);
        }
        return null;
    }

    private String getDigest(String verifier, String encoderType) {
        byte[] bytes = null;
        try {
            bytes = verifier.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        md.update(bytes, 0, bytes.length);
        byte[] digest = md.digest();
        String challenge = null;
        if ("local".equals(encoderType)) {
            return getEncodedUsingLocalEncoder(digest);
        } else if ("urlsafe".equals(encoderType)) {
            return getUrlSafeEncoded(digest);
        } else if ("nourlsafe".equals(encoderType)) {
            return getEncoded(digest);
        }

        return challenge;
    }

    // @Test
    // public void testIsValid() {
    // ConsentCacheKey key = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, SCOPE, RESOURCE_ID, 2);
    // assertTrue("Cache key should be valid.", key.isValid());
    // try {
    // Thread.sleep(2000);
    // for (int i = 0; i < 10; i++) {
    // Thread.sleep(500);
    // if (key.isValid())
    // break;
    // }
    // assertFalse("Cache key should not be valid.", key.isValid());
    // } catch (InterruptedException ex) {
    // Thread.currentThread().interrupt();
    // fail("Sleep was interrupted");
    // }
    //
    // }

}
