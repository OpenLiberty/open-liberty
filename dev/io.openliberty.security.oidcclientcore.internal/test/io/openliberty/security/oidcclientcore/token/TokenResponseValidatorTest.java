/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.security.test.common.jwt.utils.JwtUnitTestUtils;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.config.MetadataUtils;
import io.openliberty.security.oidcclientcore.config.OidcMetadataService;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import test.common.SharedOutputManager;

public class TokenResponseValidatorTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final OidcClientConfig oidcClientConfig = mockery.mock(OidcClientConfig.class);
    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private final OidcProviderMetadata providerMetadata = mockery.mock(OidcProviderMetadata.class);
    private final OidcMetadataService oidcMetadataService = mockery.mock(OidcMetadataService.class);

    private final String SUB = "some_user@domain.com";
    private final String ISS = "https://localhost/some/issuer";
    private final String SECRET = "shared_secret_key";

    TokenResponseValidator validator;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        validator = new TokenResponseValidator(oidcClientConfig, request, response);

        MetadataUtils metadataUtils = new MetadataUtils();
        metadataUtils.setOidcMetadataService(oidcMetadataService);
    }

    @After
    public void tearDown() {
        MetadataUtils metadataUtils = new MetadataUtils();
        metadataUtils.unsetOidcMetadataService(oidcMetadataService);

        mockery.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    // TODO - validate

    // TODO - getJwtContextForIdToken

    // TODO - getJwtClaimsFromIdTokenContext

    // TODO - validateIdTokenClaimsAndGetIssuer

    @Test
    public void test_validateIdTokenFormat_hs256_missingRequiredClaim() throws Exception {
        JSONObject discoveryData = new JSONObject();
        JSONArray signingAlgsSupported = getSigningAlgsSupported("HS256", "RS256");
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED, signingAlgsSupported);

        mockery.checking(new Expectations() {
            {
                allowing(oidcClientConfig).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(providerMetadata).getIdTokenSigningAlgorithmsSupported();
                will(returnValue(null));
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
                allowing(oidcClientConfig).getClientId();
                one(oidcClientConfig).getClientSecret();
                will(returnValue(new ProtectedString(SECRET.toCharArray())));
            }
        });
        JwtContext jwtContext = createSimpleJwtContext();

        try {
            JwtClaims claims = validator.validateIdTokenFormat(jwtContext, ISS);
            fail("Should have thrown an exception, but didn't. Got claims: " + claims.getRawJson());
        } catch (InvalidJwtException e) {
            // Expected
        }
    }

    @Test
    public void test_validateIdTokenFormat_hs256() throws Exception {
        JSONObject discoveryData = new JSONObject();
        JSONArray signingAlgsSupported = getSigningAlgsSupported("HS256", "RS256");
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED, signingAlgsSupported);

        mockery.checking(new Expectations() {
            {
                allowing(oidcClientConfig).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(providerMetadata).getIdTokenSigningAlgorithmsSupported();
                will(returnValue(null));
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
                allowing(oidcClientConfig).getClientId();
                one(oidcClientConfig).getClientSecret();
                will(returnValue(new ProtectedString(SECRET.toCharArray())));
            }
        });
        JwtContext jwtContext = createMinimumValidJwtContext();

        JwtClaims claims = validator.validateIdTokenFormat(jwtContext, ISS);
        assertEquals(jwtContext.getJwtClaims().getClaimsMap(), claims.getClaimsMap());
    }

    @Test
    public void test_validateIdTokenFormat_hs256_badIssuer() throws Exception {
        JSONObject discoveryData = new JSONObject();
        JSONArray signingAlgsSupported = getSigningAlgsSupported("HS256", "RS256");
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED, signingAlgsSupported);

        mockery.checking(new Expectations() {
            {
                allowing(oidcClientConfig).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(providerMetadata).getIdTokenSigningAlgorithmsSupported();
                will(returnValue(null));
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
                allowing(oidcClientConfig).getClientId();
                one(oidcClientConfig).getClientSecret();
                will(returnValue(new ProtectedString(SECRET.toCharArray())));
            }
        });
        JwtContext jwtContext = createMinimumValidJwtContext();

        try {
            validator.validateIdTokenFormat(jwtContext, "some expected issuer");
            fail("Should have thrown an exception, but didn't.");
        } catch (InvalidJwtException e) {
            // Expected
        }
    }

    @Test
    public void test_getSigningAlgorithmsAllowed_algsInProviderMetadata() throws Exception {
        String[] expectedAlgsSupported = new String[] { "RS256", "none" };
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(providerMetadata).getIdTokenSigningAlgorithmsSupported();
                will(returnValue(expectedAlgsSupported));
            }
        });
        String[] algs = validator.getSigningAlgorithmsAllowed();
        if (algs == null || algs.length != expectedAlgsSupported.length) {
            fail("Did not find the expected number of algorithms supported. Got: " + Arrays.toString(algs));
        }
        assertEquals(Arrays.asList(expectedAlgsSupported), Arrays.asList(algs));
    }

    @Test
    public void test_getSigningAlgorithmsAllowed_algsNotInProviderMetadata_algsNotInDiscoveryMetadata() throws Exception {
        JSONObject discoveryData = new JSONObject();
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(null));
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
                one(oidcClientConfig).getClientId();
                one(oidcClientConfig).getProviderURI();
            }
        });
        String[] algs = validator.getSigningAlgorithmsAllowed();
        if (algs == null || algs.length != 1) {
            fail("Did not find the expected number of algorithms supported. Got: " + Arrays.toString(algs));
        }
        assertEquals("Should have defaulted to RS256, but did not.", "RS256", algs[0]);
    }

    @Test
    public void test_getSigningAlgorithmsAllowed_algsNotInProviderMetadata_algsInDiscoveryMetadata() throws Exception {
        JSONObject discoveryData = new JSONObject();
        JSONArray signingAlgsSupported = getSigningAlgsSupported("HS256");
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED, signingAlgsSupported);
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(providerMetadata).getIdTokenSigningAlgorithmsSupported();
                will(returnValue(null));
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
            }
        });
        String[] algs = validator.getSigningAlgorithmsAllowed();
        if (algs == null || algs.length != 1) {
            fail("Did not find the expected number of algorithms supported. Got: " + Arrays.toString(algs));
        }
        assertEquals("HS256", algs[0]);
    }

    private JSONArray getSigningAlgsSupported(String... algs) {
        JSONArray algsSupported = new JSONArray();
        for (String alg : algs) {
            algsSupported.add(alg);
        }
        return algsSupported;
    }

    private JwtContext createSimpleJwtContext() throws Exception {
        JSONObject claims = new JSONObject();
        claims.put("sub", SUB);
        String jws = JwtUnitTestUtils.getHS256Jws(claims, SECRET);
        return JwtParsingUtils.parseJwtWithoutValidation(jws);
    }

    private JwtContext createMinimumValidJwtContext() throws Exception {
        JSONObject claims = new JSONObject();
        claims.put("sub", SUB);
        claims.put("iss", ISS);
        claims.put("exp", System.currentTimeMillis() + (60 * 1000));
        String jws = JwtUnitTestUtils.getHS256Jws(claims, SECRET);
        return JwtParsingUtils.parseJwtWithoutValidation(jws);
    }

}
