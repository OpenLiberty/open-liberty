/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.pkce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.oauth20.pkce.PkceMethodPlain;
import com.ibm.ws.security.oauth20.pkce.PkceMethodS256;
import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequestParameters;
import test.common.SharedOutputManager;

public class ProofKeyForCodeExchangeHelperTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final AuthorizationRequestParameters parameters = mockery.mock(AuthorizationRequestParameters.class);

    ProofKeyForCodeExchangeHelper helper = new ProofKeyForCodeExchangeHelper();

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
    public void test_generateAndAddPkceParametersToAuthzRequest_unknownChallengeMethod() {
        String state = testName.getMethodName();
        helper.generateAndAddPkceParametersToAuthzRequest("unknown", state, parameters);
        assertEquals("Should not have found a cached code verifier, but did.", null, ProofKeyForCodeExchangeCache.getCachedCodeVerifier(state));
    }

    @Test
    public void test_generateAndAddPkceParametersToAuthzRequest_plain() {
        String state = testName.getMethodName();
        mockery.checking(new Expectations() {
            {
                one(parameters).addParameter(with(any(String.class)), with(any(String.class)));
                one(parameters).addParameter(with(any(String.class)), with(any(String.class)));
            }
        });
        helper.generateAndAddPkceParametersToAuthzRequest(PkceMethodPlain.CHALLENGE_METHOD, state, parameters);

        String cachedCodeVerifier = ProofKeyForCodeExchangeCache.getCachedCodeVerifier(state);
        assertNotNull("Should have found a cached code verifier, but did not.", cachedCodeVerifier);
    }

    @Test
    public void test_generateAndAddPkceParametersToAuthzRequest_S256() {
        String state = testName.getMethodName();
        mockery.checking(new Expectations() {
            {
                one(parameters).addParameter(with(any(String.class)), with(any(String.class)));
                one(parameters).addParameter(with(any(String.class)), with(any(String.class)));
            }
        });
        helper.generateAndAddPkceParametersToAuthzRequest(PkceMethodS256.CHALLENGE_METHOD, state, parameters);

        String cachedCodeVerifier = ProofKeyForCodeExchangeCache.getCachedCodeVerifier(state);
        assertNotNull("Should have found a cached code verifier, but did not.", cachedCodeVerifier);
    }

}
