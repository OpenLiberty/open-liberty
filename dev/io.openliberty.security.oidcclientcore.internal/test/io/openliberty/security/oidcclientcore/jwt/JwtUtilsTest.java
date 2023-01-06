/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.jwt;

import static org.junit.Assert.fail;

import org.jmock.Expectations;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.security.test.common.jwt.utils.JwtUnitTestUtils;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import io.openliberty.security.common.jwt.jwk.RemoteJwkData;
import io.openliberty.security.common.jwt.jws.JwsSignatureVerifier.Builder;
import io.openliberty.security.common.jwt.jws.JwsVerificationKeyHelper;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import test.common.SharedOutputManager;

public class JwtUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String CWWKS2520E_SIG_ALG_IN_HEADER_NOT_ALLOWED = "CWWKS2520E";

    private final OidcClientConfig oidcClientConfig = mockery.mock(OidcClientConfig.class);
    private final JsonWebStructure jsonWebStructure = mockery.mock(JsonWebStructure.class);
    private final JwsVerificationKeyHelper.Builder jwsVerificationKeyHelperBuilder = mockery.mock(JwsVerificationKeyHelper.Builder.class);
    private final OidcProviderMetadata providerMetadata = mockery.mock(OidcProviderMetadata.class);

    private final String SUB = "some_user@domain.com";
    private final String SECRET = "shared_secret_key";
    private final ProtectedString SECRET_PROTECTED_STRING = new ProtectedString(SECRET.toCharArray());
    private final String JWKS_URI = "https://localhost/jwk";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
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
    public void test_verifyJwsAlgHeaderAndCreateJwsSignatureVerifierBuilder_jwtAlgNotSupported() throws Exception {
        String[] expectedAlgsSupported = new String[] { "RS256", "ES256" };
        JwtContext jwtContext = createSimpleJwtContext();

        try {
            JwtUtils.verifyJwsAlgHeaderAndCreateJwsSignatureVerifierBuilder(jwtContext, oidcClientConfig, expectedAlgsSupported);
            fail("Should have thrown an exception, but didn't.");
        } catch (Exception e) {
            verifyException(e, CWWKS2520E_SIG_ALG_IN_HEADER_NOT_ALLOWED + ".+" + "HS256" + ".+" + "RS256, ES256");
        }
    }

    @Test
    public void test_verifyJwsAlgHeaderAndCreateJwsSignatureVerifierBuilder_jwtAlgSupported() throws Exception {
        String[] expectedAlgsSupported = new String[] { "HS256", "RS256" };
        mockery.checking(new Expectations() {
            {
                allowing(oidcClientConfig).getClientId();
                allowing(oidcClientConfig).getClientSecret();
                will(returnValue(SECRET_PROTECTED_STRING));
            }
        });
        JwtContext jwtContext = createSimpleJwtContext();

        Builder builder = JwtUtils.verifyJwsAlgHeaderAndCreateJwsSignatureVerifierBuilder(jwtContext, oidcClientConfig, expectedAlgsSupported);
        // Ensure the builder can be used to validate the signature
        builder.build().validateJwsSignature(jwtContext);
    }

    @Test
    public void test_setKeyData_jwsMissingAlgHeader() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(jsonWebStructure).getAlgorithmHeaderValue();
                will(returnValue(null));
            }
        });
        JwtUtils.setKeyData(jsonWebStructure, oidcClientConfig, jwsVerificationKeyHelperBuilder);
    }

    @Test
    public void test_setKeyData_unsigned() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(jsonWebStructure).getAlgorithmHeaderValue();
                will(returnValue("none"));
            }
        });
        JwtUtils.setKeyData(jsonWebStructure, oidcClientConfig, jwsVerificationKeyHelperBuilder);
    }

    @Test
    public void test_setKeyData_hs256() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(jsonWebStructure).getAlgorithmHeaderValue();
                will(returnValue("HS256"));
                one(oidcClientConfig).getClientSecret();
                will(returnValue(SECRET_PROTECTED_STRING));
                one(jwsVerificationKeyHelperBuilder).sharedSecret(SECRET_PROTECTED_STRING);
            }
        });
        JwtUtils.setKeyData(jsonWebStructure, oidcClientConfig, jwsVerificationKeyHelperBuilder);
    }

    @Test
    public void test_setKeyData_rs256() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(jsonWebStructure).getAlgorithmHeaderValue();
                will(returnValue("RS256"));
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(providerMetadata).getJwksURI();
                will(returnValue(JWKS_URI));
                one(oidcClientConfig).getJwksConnectTimeout();
                will(returnValue(500));
                one(oidcClientConfig).getJwksReadTimeout();
                will(returnValue(500));
                one(jwsVerificationKeyHelperBuilder).remoteJwkData(with(any(RemoteJwkData.class)));
            }
        });
        JwtUtils.setKeyData(jsonWebStructure, oidcClientConfig, jwsVerificationKeyHelperBuilder);
    }

    private JwtContext createSimpleJwtContext() throws Exception {
        JSONObject claims = new JSONObject();
        claims.put("sub", SUB);
        String jws = JwtUnitTestUtils.getHS256Jws(claims, SECRET);
        return JwtParsingUtils.parseJwtWithoutValidation(jws);
    }

}
