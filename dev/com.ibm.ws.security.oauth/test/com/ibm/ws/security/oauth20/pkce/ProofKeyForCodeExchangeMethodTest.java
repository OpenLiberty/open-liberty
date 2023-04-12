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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class ProofKeyForCodeExchangeMethodTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

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
    public void test_getInstance_codeChallengeMethodNull() {
        String codeChallengeMethod = null;
        ProofKeyForCodeExchangeMethod instance = ProofKeyForCodeExchangeMethod.getInstance(codeChallengeMethod);
        assertEquals("Null code challenge method should not have produced a PKCE method instance.", null, instance);
    }

    @Test
    public void test_getInstance_codeChallengeMethodEmpty() {
        String codeChallengeMethod = "";
        ProofKeyForCodeExchangeMethod instance = ProofKeyForCodeExchangeMethod.getInstance(codeChallengeMethod);
        assertEquals("Empty code challenge method should not have produced a PKCE method instance.", null, instance);
    }

    @Test
    public void test_getInstance_codeChallengeMethodUnknown() {
        String codeChallengeMethod = "RS256";
        ProofKeyForCodeExchangeMethod instance = ProofKeyForCodeExchangeMethod.getInstance(codeChallengeMethod);
        assertEquals("Unsupported/unknown code challenge method should not have produced a PKCE method instance.", null, instance);
    }

    @Test
    public void test_getInstance_codeChallengeMethodPlain() {
        String codeChallengeMethod = "plain";
        ProofKeyForCodeExchangeMethod instance = ProofKeyForCodeExchangeMethod.getInstance(codeChallengeMethod);
        assertNotNull("Should have created a valid PKCE method based on " + codeChallengeMethod + " code challenge method.", instance);
        assertEquals("Did not get the expected code challenge method type from the PKCE instance.", "plain", instance.getCodeChallengeMethod());
    }

    @Test
    public void test_getInstance_codeChallengeMethodS256() {
        String codeChallengeMethod = "S256";
        ProofKeyForCodeExchangeMethod instance = ProofKeyForCodeExchangeMethod.getInstance(codeChallengeMethod);
        assertNotNull("Should have created a valid PKCE method based on " + codeChallengeMethod + " code challenge method.", instance);
        assertEquals("Did not get the expected code challenge method type from the PKCE instance.", "S256", instance.getCodeChallengeMethod());
    }

    @Test
    public void test_isValidCodeChallengeMethod_null() {
        String codeChallengeMethod = null;
        assertFalse("Null code challenge method should not be considered a valid method.", ProofKeyForCodeExchangeMethod.isValidCodeChallengeMethod(codeChallengeMethod));
    }

    @Test
    public void test_isValidCodeChallengeMethod_empty() {
        String codeChallengeMethod = "";
        assertFalse("Empty code challenge method should not be considered a valid method.", ProofKeyForCodeExchangeMethod.isValidCodeChallengeMethod(codeChallengeMethod));
    }

    @Test
    public void test_isValidCodeChallengeMethod_unknown() {
        String codeChallengeMethod = "unknown";
        assertFalse("Unknown code challenge method should not be considered a valid method.", ProofKeyForCodeExchangeMethod.isValidCodeChallengeMethod(codeChallengeMethod));
    }

    @Test
    public void test_isValidCodeChallengeMethod_plain() {
        String codeChallengeMethod = "plain";
        assertTrue("Plain code challenge method should be considered a valid method.", ProofKeyForCodeExchangeMethod.isValidCodeChallengeMethod(codeChallengeMethod));
    }

    @Test
    public void test_isValidCodeChallengeMethod_S256() {
        String codeChallengeMethod = "S256";
        assertTrue("S256 code challenge method should be considered a valid method.", ProofKeyForCodeExchangeMethod.isValidCodeChallengeMethod(codeChallengeMethod));
    }

}
