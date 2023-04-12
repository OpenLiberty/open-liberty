/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.oauth20.pkce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.oauth.core.api.error.oauth20.InvalidGrantException;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class PkceMethodPlainTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String CWOAU0081E_CODE_CHALLENGE_DOES_NOT_MATCH_VERIFIER = "CWOAU0081E";

    PkceMethodPlain pkceMethod = new PkceMethodPlain();

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
    public void test_generateCodeChallenge_nullVerifier() {
        String codeVerifier = null;
        String codeChallenge = pkceMethod.generateCodeChallenge(codeVerifier);
        assertEquals("Generated code challenge should have matched the input.", codeVerifier, codeChallenge);
    }

    @Test
    public void test_generateCodeChallenge_emptyVerifier() {
        String codeVerifier = "";
        String codeChallenge = pkceMethod.generateCodeChallenge(codeVerifier);
        assertEquals("Generated code challenge should have matched the input.", codeVerifier, codeChallenge);
    }

    @Test
    public void test_generateCodeChallenge_nonEmptyVerifier() {
        String codeVerifier = "this is my code verifier";
        String codeChallenge = pkceMethod.generateCodeChallenge(codeVerifier);
        assertEquals("Generated code challenge should have matched the input.", codeVerifier, codeChallenge);
    }

    @Test
    public void test_validate_bothNull() {
        String codeVerifier = null;
        String codeChallenge = null;
        try {
            pkceMethod.validate(codeVerifier, codeChallenge);
        } catch (InvalidGrantException e) {
            fail("Should not have thrown an exception but got " + e);
        }
    }

    @Test
    public void test_validate_verifierNonNull_challengeNull() {
        String codeVerifier = "not null";
        String codeChallenge = null;
        try {
            pkceMethod.validate(codeVerifier, codeChallenge);
            fail("Should have thrown an exception but didn't.");
        } catch (InvalidGrantException e) {
            // Expected
            verifyException(e, CWOAU0081E_CODE_CHALLENGE_DOES_NOT_MATCH_VERIFIER);
        }
    }

    @Test
    public void test_validate_verifierNull_challengeNonNull() {
        String codeVerifier = null;
        String codeChallenge = "not null";
        try {
            pkceMethod.validate(codeVerifier, codeChallenge);
            fail("Should have thrown an exception but didn't.");
        } catch (InvalidGrantException e) {
            // Expected
            verifyException(e, CWOAU0081E_CODE_CHALLENGE_DOES_NOT_MATCH_VERIFIER);
        }
    }

    @Test
    public void test_validate_mismatch() {
        String codeVerifier = "value1";
        String codeChallenge = "value2";
        try {
            pkceMethod.validate(codeVerifier, codeChallenge);
            fail("Should have thrown an exception but didn't.");
        } catch (InvalidGrantException e) {
            // Expected
            verifyException(e, CWOAU0081E_CODE_CHALLENGE_DOES_NOT_MATCH_VERIFIER);
        }
    }

    @Test
    public void test_validate_match() {
        String codeVerifier = "value1";
        String codeChallenge = codeVerifier;
        try {
            pkceMethod.validate(codeVerifier, codeChallenge);
        } catch (InvalidGrantException e) {
            fail("Should not have thrown an exception but got " + e);
        }
    }

}
