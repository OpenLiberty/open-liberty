/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.security.oauth20.pkce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.oauth.core.api.error.oauth20.InvalidGrantException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class ProofKeyForCodeExchangeTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String ALLOWED_CHARS_REGEX = "[a-zA-Z0-9._~-]";

    private static final String CWOAU0033E_MISSING_PARAMETER = "CWOAU0033E";
    private static final String CWOAU0079E_INVALID_CHALLENGE_METHOD = "CWOAU0079E";
    private static final String CWOAU0080E_CODE_VERIFIER_LENGTH_ERROR = "CWOAU0080E";
    private static final String CWOAU0081E_CODE_CHALLENGE_DOES_NOT_MATCH_VERIFIER = "CWOAU0081E";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_generateCodeVerifier() {
        String codeVerifier = ProofKeyForCodeExchange.generateCodeVerifier();
        assertNotNull("Code verifier should not have been null.", codeVerifier);

        assertTrue("Code verifier contained a character outside the allowed set " + ALLOWED_CHARS_REGEX, codeVerifier.matches(ALLOWED_CHARS_REGEX + "+"));
        assertEquals("Code verifier did not have the expected length. Code verifier was [" + codeVerifier + "].", 43, codeVerifier.length());

        String codeVerifier2 = ProofKeyForCodeExchange.generateCodeVerifier();
        assertFalse("Two calls to generate code verifiers should not have produced the same value.", codeVerifier2.equals(codeVerifier));
    }

    @Test
    public void test_verifyCodeChallenge_nullVerifier() {
        String codeVerifier = null;
        String codeChallenge = "codechallenge";
        String codeChallengeMethod = PkceMethodS256.CHALLENGE_METHOD;
        try {
            ProofKeyForCodeExchange.verifyCodeChallenge(codeVerifier, codeChallenge, codeChallengeMethod);
            fail("Should have thrown an exception but didn't.");
        } catch (OAuth20MissingParameterException e) {
            // Expected
            verifyException(e, CWOAU0033E_MISSING_PARAMETER + ".*" + "code_verifier");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Caught unexpected exception: " + e);
        }
    }

    @Test
    public void test_verifyCodeChallenge_emptyVerifier() {
        String codeVerifier = "";
        String codeChallenge = "codechallenge";
        String codeChallengeMethod = PkceMethodS256.CHALLENGE_METHOD;
        try {
            ProofKeyForCodeExchange.verifyCodeChallenge(codeVerifier, codeChallenge, codeChallengeMethod);
            fail("Should have thrown an exception but didn't.");
        } catch (InvalidGrantException e) {
            // Expected
            verifyException(e, CWOAU0080E_CODE_VERIFIER_LENGTH_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Caught unexpected exception: " + e);
        }
    }

    @Test
    public void test_verifyCodeChallenge_unknownChallengeMethod() {
        String codeVerifier = getStringOfLength(ProofKeyForCodeExchange.CODE_VERIFIER_MIN_LENGTH);
        String codeChallenge = "codechallenge";
        String codeChallengeMethod = "42";
        try {
            ProofKeyForCodeExchange.verifyCodeChallenge(codeVerifier, codeChallenge, codeChallengeMethod);
            fail("Should have thrown an exception but didn't.");
        } catch (InvalidGrantException e) {
            // Expected
            verifyException(e, CWOAU0079E_INVALID_CHALLENGE_METHOD);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Caught unexpected exception: " + e);
        }
    }

    @Test
    public void test_verifyCodeChallenge_invalidChallenge() {
        String codeVerifier = getStringOfLength(ProofKeyForCodeExchange.CODE_VERIFIER_MIN_LENGTH);
        String codeChallenge = "codechallenge";
        String codeChallengeMethod = PkceMethodS256.CHALLENGE_METHOD;
        try {
            ProofKeyForCodeExchange.verifyCodeChallenge(codeVerifier, codeChallenge, codeChallengeMethod);
            fail("Should have thrown an exception but didn't.");
        } catch (InvalidGrantException e) {
            // Expected
            verifyException(e, CWOAU0081E_CODE_CHALLENGE_DOES_NOT_MATCH_VERIFIER);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Caught unexpected exception: " + e);
        }
    }

    @Test
    public void test_verifyCodeChallenge_validChallenge() {
        String codeVerifier = getStringOfLength(ProofKeyForCodeExchange.CODE_VERIFIER_MIN_LENGTH);
        ProofKeyForCodeExchangeMethod pkceMethod = new PkceMethodS256();
        String codeChallenge = pkceMethod.generateCodeChallenge(codeVerifier);
        String codeChallengeMethod = PkceMethodS256.CHALLENGE_METHOD;
        try {
            ProofKeyForCodeExchange.verifyCodeChallenge(codeVerifier, codeChallenge, codeChallengeMethod);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Caught unexpected exception: " + e);
        }
    }

    @Test
    public void test_isCodeVerifierLengthAcceptable_null() {
        String codeVerifier = null;
        assertFalse("Code verifier [" + codeVerifier + "] should not be considered to have an acceptable length.", ProofKeyForCodeExchange.isCodeVerifierLengthAcceptable(codeVerifier));
    }

    @Test
    public void test_isCodeVerifierLengthAcceptable_emptyString() {
        String codeVerifier = "";
        assertFalse("Code verifier [" + codeVerifier + "] (length " + codeVerifier.length() + ") should not be considered to have an acceptable length.", ProofKeyForCodeExchange.isCodeVerifierLengthAcceptable(codeVerifier));
    }

    @Test
    public void test_isCodeVerifierLengthAcceptable_minMinus1() {
        String codeVerifier = getStringOfLength(ProofKeyForCodeExchange.CODE_VERIFIER_MIN_LENGTH - 1);
        assertFalse("Code verifier [" + codeVerifier + "] (length " + codeVerifier.length() + ") should not be considered to have an acceptable length.", ProofKeyForCodeExchange.isCodeVerifierLengthAcceptable(codeVerifier));
    }

    @Test
    public void test_isCodeVerifierLengthAcceptable_min() {
        String codeVerifier = getStringOfLength(ProofKeyForCodeExchange.CODE_VERIFIER_MIN_LENGTH);
        assertTrue("Code verifier [" + codeVerifier + "] (length " + codeVerifier.length() + ") should be considered to have an acceptable length.", ProofKeyForCodeExchange.isCodeVerifierLengthAcceptable(codeVerifier));
    }

    @Test
    public void test_isCodeVerifierLengthAcceptable_max() {
        String codeVerifier = getStringOfLength(ProofKeyForCodeExchange.CODE_VERIFIER_MAX_LENGTH);
        assertTrue("Code verifier [" + codeVerifier + "] (length " + codeVerifier.length() + ") should be considered to have an acceptable length.", ProofKeyForCodeExchange.isCodeVerifierLengthAcceptable(codeVerifier));
    }

    @Test
    public void test_isCodeVerifierLengthAcceptable_maxPlus1() {
        String codeVerifier = getStringOfLength(ProofKeyForCodeExchange.CODE_VERIFIER_MAX_LENGTH + 1);
        assertFalse("Code verifier [" + codeVerifier + "] (length " + codeVerifier.length() + ") should not be considered to have an acceptable length.", ProofKeyForCodeExchange.isCodeVerifierLengthAcceptable(codeVerifier));
    }

    private String getStringOfLength(int length) {
        String result = "";
        for (int i = 1; i <= length; i++) {
            result += "a";
        }
        return result;
    }

}
