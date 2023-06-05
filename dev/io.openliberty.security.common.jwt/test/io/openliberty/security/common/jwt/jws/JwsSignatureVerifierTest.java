/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.common.jwt.jws;

import static org.junit.Assert.fail;

import java.util.Arrays;

import org.jose4j.jwt.consumer.JwtContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.security.test.common.jwt.utils.JwtUnitTestUtils;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import io.openliberty.security.common.jwt.exceptions.SignatureAlgorithmNotInAllowedList;
import test.common.SharedOutputManager;

public class JwsSignatureVerifierTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String CWWKS2520E_SIG_ALG_IN_HEADER_NOT_ALLOWED = "CWWKS2520E";
    private static final String CWWKS2521E_SIGNING_KEY_NOT_SPECIFIED = "CWWKS2521E";

    private final String SUB = "some_user@domain.com";
    private final String SECRET = "shared_secret_key";

    JwsSignatureVerifier.Builder verifierBuilder;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        verifierBuilder = new JwsSignatureVerifier.Builder();
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_verifyJwsAlgHeaderOnly_noAlgorithmsAllowed() throws Exception {
        JwtContext jwtContext = createJwtContext();

        try {
            JwsSignatureVerifier.verifyJwsAlgHeaderOnly(jwtContext, Arrays.asList("none"));
            fail("Should have thrown an exception but didn't.");
        } catch (SignatureAlgorithmNotInAllowedList e) {
            verifyException(e, CWWKS2520E_SIG_ALG_IN_HEADER_NOT_ALLOWED);
        }
    }

    @Test
    public void test_verifyJwsAlgHeaderOnly_expectedSigAlgDoesNotMatch() throws Exception {
        JwtContext jwtContext = createJwtContext();

        try {
            JwsSignatureVerifier.verifyJwsAlgHeaderOnly(jwtContext, Arrays.asList("none"));
            fail("Should have thrown an exception but didn't.");
        } catch (SignatureAlgorithmNotInAllowedList e) {
            verifyException(e, CWWKS2520E_SIG_ALG_IN_HEADER_NOT_ALLOWED);
        }
    }

    @Test
    public void test_verifyJwsAlgHeaderOnly_expectSigAlgMatches() throws Exception {
        JwtContext jwtContext = createJwtContext();

        JwsSignatureVerifier.verifyJwsAlgHeaderOnly(jwtContext, Arrays.asList("HS256"));
    }

    @Test
    public void test_verifyJwsAlgHeaderOnly_notSignedByAllowedSigAlg() throws Exception {
        JwtContext jwtContext = createJwtContext();

        try {
            JwsSignatureVerifier.verifyJwsAlgHeaderOnly(jwtContext, Arrays.asList("none", "RS256", "HS512"));
            fail("Should have thrown an exception but didn't.");
        } catch (SignatureAlgorithmNotInAllowedList e) {
            verifyException(e, CWWKS2520E_SIG_ALG_IN_HEADER_NOT_ALLOWED);
        }
    }

    @Test
    public void test_verifyJwsAlgHeaderOnly_signedByAllowedSigAlg() throws Exception {
        JwtContext jwtContext = createJwtContext();

        JwsSignatureVerifier.verifyJwsAlgHeaderOnly(jwtContext, Arrays.asList("none", "RS256", "HS512", "HS256"));
    }

    @Test
    public void test_validateJwsSignature_noSigAlgExpectationsSet() throws Exception {
        JwtContext jwtContext = createJwtContext();

        JwsSignatureVerifier verifier = verifierBuilder.build();
        try {
            verifier.validateJwsSignature(jwtContext);
            fail("Should have thrown an exception but didn't.");
        } catch (Exception e) {
            verifyException(e, CWWKS2520E_SIG_ALG_IN_HEADER_NOT_ALLOWED);
        }
    }

    @Test
    public void test_validateJwsSignature_expectedSigAlgDoesNotMatch() throws Exception {
        JwtContext jwtContext = createJwtContext();

        verifierBuilder.signatureAlgorithm("HS512");
        JwsSignatureVerifier verifier = verifierBuilder.build();
        try {
            verifier.validateJwsSignature(jwtContext);
            fail("Should have thrown an exception but didn't.");
        } catch (Exception e) {
            verifyException(e, CWWKS2520E_SIG_ALG_IN_HEADER_NOT_ALLOWED);
        }
    }

    @Test
    public void test_validateJwsSignature_expectSigAlgMatches_noKeySpecified() throws Exception {
        JwtContext jwtContext = createJwtContext();

        verifierBuilder.signatureAlgorithm("HS256");
        JwsSignatureVerifier verifier = verifierBuilder.build();
        try {
            verifier.validateJwsSignature(jwtContext);
            fail("Should have thrown an exception but didn't.");
        } catch (Exception e) {
            verifyException(e, CWWKS2521E_SIGNING_KEY_NOT_SPECIFIED);
        }
    }

    @Test
    public void test_validateJwsSignature_expectSigAlgMatches_keyMismatch() throws Exception {
        JwtContext jwtContext = createJwtContext();

        verifierBuilder.signatureAlgorithm("HS256");
        verifierBuilder.key(JwtUnitTestUtils.getHS256Key("some key mismatch"));
        JwsSignatureVerifier verifier = verifierBuilder.build();
        try {
            verifier.validateJwsSignature(jwtContext);
            fail("Should have thrown an exception but didn't.");
        } catch (Exception e) {
            verifyException(e, "InvalidJwtSignatureException");
        }
    }

    @Test
    public void test_validateJwsSignature_expectSigAlgMatches_keyMatch() throws Exception {
        JwtContext jwtContext = createJwtContext();

        verifierBuilder.signatureAlgorithm("HS256");
        verifierBuilder.key(JwtUnitTestUtils.getHS256Key(SECRET));
        JwsSignatureVerifier verifier = verifierBuilder.build();
        verifier.validateJwsSignature(jwtContext);
    }

    @Test
    public void test_validateJwsSignature_unsigned() throws Exception {
        JwtContext jwtContext = createUnsignedJwtContext();

        verifierBuilder.signatureAlgorithm("none");
        JwsSignatureVerifier verifier = verifierBuilder.build();
        verifier.validateJwsSignature(jwtContext);
    }

    private JwtContext createUnsignedJwtContext() throws Exception {
        JSONObject claims = new JSONObject();
        claims.put("sub", SUB);
        JSONObject header = JwtUnitTestUtils.getJwsHeader("none");
        String jws = JwtUnitTestUtils.encode(header.toString()) + "." + JwtUnitTestUtils.encode(claims.toString()) + ".";
        return JwtParsingUtils.parseJwtWithoutValidation(jws);
    }

    private JwtContext createJwtContext() throws Exception {
        JSONObject claims = new JSONObject();
        claims.put("sub", SUB);
        String jws = JwtUnitTestUtils.getHS256Jws(claims, SECRET);
        return JwtParsingUtils.parseJwtWithoutValidation(jws);
    }

}
