/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.tai;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.common.jwk.utils.JsonUtils;
import com.ibm.ws.security.jwt.internal.ConsumerUtil;
import com.ibm.ws.security.jwt.internal.JwtConsumerConfigImpl;
import com.ibm.ws.security.mp.jwt.MicroProfileJwtConfig;
import com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException;
import com.ibm.ws.security.mp.jwt.impl.DefaultJsonWebTokenImpl;
import com.ibm.wsspi.security.token.AttributeNameConstants;

import test.common.SharedOutputManager;

/**
 *
 */
public class TAIMappingHelperTest {
    protected final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.mp.jwt.*=all");
    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        //        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /**
     * Test method for {@link com.ibm.ws.security.mp.jwt.tai.TAIMappingHelper#getRealm(java.lang.String)}.
     */
    @Test
    public void testGetRealm() {
        try {
            TAIMappingHelper helper = new TAIMappingHelper(null, null);
            String realm = helper.getRealm("/issuer");
            assertEquals("the realms do not match!", "/issuer", realm);
        } catch (MpJwtProcessingException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void testIsRealmEndsWithSlash() {
        try {
            TAIMappingHelper helper = new TAIMappingHelper(null, null);
            String realm = helper.getRealm("http://localhost:9999/issuer/");
            assertEquals("the realms do not match!", "http://localhost:9999/issuer", realm);
        } catch (MpJwtProcessingException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void testGetRealmWithOneChar() {
        try {
            TAIMappingHelper helper = new TAIMappingHelper(null, null);
            String realm = helper.getRealm("i/");
            assertEquals("the realms do not match!", "i", realm);
        } catch (MpJwtProcessingException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void testClaimsProcessedTheSame() {
        //decoded payload claims : {"token_type":"Bearer","aud":"aud1","sub":"testuser","upn":"testuser","groups":["group1-abc","group2-def","testuser-group"],"iss":"https://9.24.8.103:8947/jwt/jwkEnabled","exp":1504212390,"iat":1504205190}
        String jwt = "eyJraWQiOiJXYlVqSEN5b3V5ZEoySEFKc1dOMSIsImFsZyI6IlJTMjU2In0.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXVkIjoiYXVkMSIsInN1YiI6InRlc3R1c2VyIiwidXBuIjoidGVzdHVzZXIiLCJncm91cHMiOlsiZ3JvdXAxLWFiYyIsImdyb3VwMi1kZWYiLCJ0ZXN0dXNlci1ncm91cCJdLCJpc3MiOiJodHRwczovLzkuMjQuOC4xMDM6ODk0Ny9qd3QvandrRW5hYmxlZCIsImV4cCI6MTUwNDIxMjM5MCwiaWF0IjoxNTA0MjA1MTkwfQ.egKHKw1hfEAmZMBwI1_vPFxZIzXd9UWjLqz1MvlcvT3FHNKyV3CQ8KVSb-DrHll5J57QgJxY_vBiKgZgKkDJn6rKB4LNivV-_mcsCWawjKkmFDjesMLiSFKLfLWpfbt7qVbnRNT7ysMlXMDDJguRHRj_l1M70VAQT9gaCrPsoMvDAzOtTBS0iLnRATFCddYwQsw82Ma4rfTo5Hq-ouQWgRYerxkNswZJRnahsUKoSh4ptjYBmNySBTIF7X0WL9q0gr3SzJA59rLbQLaIhLzv8lYn7GRL05ifegQX41y11peG0_-ySN3nvaYvynwwbVvsJhRWbOc9B9LiYCX_qpxuXA";

        try {
            ConsumerUtil consumerUtil = new ConsumerUtil(null);
            JwtConsumerConfigImpl config = new JwtConsumerConfigImpl() {
                @Override
                public boolean isValidationRequired() {
                    return false;
                }
            };
            JwtToken jwtToken = consumerUtil.parseJwt(jwt, config);

            String payload = JsonUtils.getPayload(jwtToken.compact());
            payload = JsonUtils.decodeFromBase64String(payload);
            Map<String, Object> claims = JsonUtils.claimsFromJsonObject(payload);

            // The claims variable is the old way that the claims were created in
            // TAIMappingHelper.  This test validates that the old way matches with the
            // new way where we use the claims from the jwtToken.
            assertEquals(claims, jwtToken.getClaims());

            TAIMappingHelper helper = new TAIMappingHelper(jwtToken);
            helper.createJwtPrincipalAndPopulateCustomProperties(jwtToken, false);
            JsonWebToken mpJwtToken = helper.getJwtPrincipal();

            // The claims from JsonWebToken were previously calculated using the payload (json string)
            // from JwtToken.  This test validates that the new logic using the claims from JwtToken
            // matches getting the Claims from the payload by using clone on the JsonWebToken since
            // clone uses the payload (json string) to parse the claims.
            JsonWebToken clone = ((DefaultJsonWebTokenImpl) mpJwtToken).clone();

            Set<String> mpClaimNames = mpJwtToken.getClaimNames();
            Set<String> expectedMpClaimNames = clone.getClaimNames();
            assertEquals(mpClaimNames, expectedMpClaimNames);
            assertEquals(9, mpClaimNames.size());

            for (String mpClaimName : mpClaimNames) {
                Object expectedClaim = clone.getClaim(mpClaimName);
                Object claim = mpJwtToken.getClaim(mpClaimName);
                if (expectedClaim instanceof Object[]) {
                    assertArrayEquals((Object[]) expectedClaim, (Object[]) claim);
                } else {
                    assertEquals(expectedClaim, claim);
                }
            }
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    private MicroProfileJwtConfig buildMockConfig(String realmNameValue, String realmIdentifierValue) {
        MicroProfileJwtConfig mockConfig = mockery.mock(MicroProfileJwtConfig.class, "config_" + realmNameValue + "_" + realmIdentifierValue);
        mockery.checking(new Expectations() {
            {
                allowing(mockConfig).getRealmName();
                will(returnValue(realmNameValue));
                allowing(mockConfig).getRealmIdentifier();
                will(returnValue(realmIdentifierValue));
                allowing(mockConfig).getUserNameAttribute();
                will(returnValue("upn"));
                allowing(mockConfig).getGroupNameAttribute();
                will(returnValue("groups"));
                allowing(mockConfig).getMapToUserRegistry();
                will(returnValue(false));
            }
        });
        return mockConfig;
    }

    @Test
    public void testSetRealm_realmNameTakesPrecedence() {
        // When realmName is set, it should take precedence over anything else
        // Test this via the getRealm / setRealm logic by creating a helper with null jwtToken
        // and then calling getRealm() directly
        try {
            // Use the existing JWT from testClaimsProcessedTheSame which has iss "https://9.24.8.103:8947/jwt/jwkEnabled"
            String jwt = "eyJraWQiOiJXYlVqSEN5b3V5ZEoySEFKc1dOMSIsImFsZyI6IlJTMjU2In0.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXVkIjoiYXVkMSIsInN1YiI6InRlc3R1c2VyIiwidXBuIjoidGVzdHVzZXIiLCJncm91cHMiOlsiZ3JvdXAxLWFiYyIsImdyb3VwMi1kZWYiLCJ0ZXN0dXNlci1ncm91cCJdLCJpc3MiOiJodHRwczovLzkuMjQuOC4xMDM6ODk0Ny9qd3QvandrRW5hYmxlZCIsImV4cCI6MTUwNDIxMjM5MCwiaWF0IjoxNTA0MjA1MTkwfQ.egKHKw1hfEAmZMBwI1_vPFxZIzXd9UWjLqz1MvlcvT3FHNKyV3CQ8KVSb-DrHll5J57QgJxY_vBiKgZgKkDJn6rKB4LNivV-_mcsCWawjKkmFDjesMLiSFKLfLWpfbt7qVbnRNT7ysMlXMDDJguRHRj_l1M70VAQT9gaCrPsoMvDAzOtTBS0iLnRATFCddYwQsw82Ma4rfTo5Hq-ouQWgRYerxkNswZJRnahsUKoSh4ptjYBmNySBTIF7X0WL9q0gr3SzJA59rLbQLaIhLzv8lYn7GRL05ifegQX41y11peG0_-ySN3nvaYvynwwbVvsJhRWbOc9B9LiYCX_qpxuXA";
            ConsumerUtil consumerUtil = new ConsumerUtil(null);
            JwtConsumerConfigImpl jwtConfig = new JwtConsumerConfigImpl() {
                @Override
                public boolean isValidationRequired() {
                    return false;
                }
            };
            JwtToken jwtToken = consumerUtil.parseJwt(jwt, jwtConfig);

            MicroProfileJwtConfig mpJwtConfig = buildMockConfig("StaticRealm", "tenant");
            TAIMappingHelper helper = new TAIMappingHelper(jwtToken, mpJwtConfig);
            helper.createJwtPrincipalAndPopulateCustomProperties(jwtToken, false);

            // realmName="StaticRealm" should win regardless of token claims
            Object realm = helper.getCustomProperties().get(AttributeNameConstants.WSCREDENTIAL_REALM);
            assertEquals("Expected realmName to take precedence", "StaticRealm", realm);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void testSetRealm_fallbackToIss_realmIdentifierClaimAbsent() {
        // When realmName=null and the realmIdentifier claim is absent from the token,
        // the realm should fall back to the iss claim value (trailing slash stripped).
        // The existing JWT has iss "https://9.24.8.103:8947/jwt/jwkEnabled" (no trailing slash).
        try {
            String jwt = "eyJraWQiOiJXYlVqSEN5b3V5ZEoySEFKc1dOMSIsImFsZyI6IlJTMjU2In0.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXVkIjoiYXVkMSIsInN1YiI6InRlc3R1c2VyIiwidXBuIjoidGVzdHVzZXIiLCJncm91cHMiOlsiZ3JvdXAxLWFiYyIsImdyb3VwMi1kZWYiLCJ0ZXN0dXNlci1ncm91cCJdLCJpc3MiOiJodHRwczovLzkuMjQuOC4xMDM6ODk0Ny9qd3QvandrRW5hYmxlZCIsImV4cCI6MTUwNDIxMjM5MCwiaWF0IjoxNTA0MjA1MTkwfQ.egKHKw1hfEAmZMBwI1_vPFxZIzXd9UWjLqz1MvlcvT3FHNKyV3CQ8KVSb-DrHll5J57QgJxY_vBiKgZgKkDJn6rKB4LNivV-_mcsCWawjKkmFDjesMLiSFKLfLWpfbt7qVbnRNT7ysMlXMDDJguRHRj_l1M70VAQT9gaCrPsoMvDAzOtTBS0iLnRATFCddYwQsw82Ma4rfTo5Hq-ouQWgRYerxkNswZJRnahsUKoSh4ptjYBmNySBTIF7X0WL9q0gr3SzJA59rLbQLaIhLzv8lYn7GRL05ifegQX41y11peG0_-ySN3nvaYvynwwbVvsJhRWbOc9B9LiYCX_qpxuXA";
            ConsumerUtil consumerUtil = new ConsumerUtil(null);
            JwtConsumerConfigImpl jwtConfig = new JwtConsumerConfigImpl() {
                @Override
                public boolean isValidationRequired() {
                    return false;
                }
            };
            JwtToken jwtToken = consumerUtil.parseJwt(jwt, jwtConfig);

            // realmIdentifier="tenant" but there is no "tenant" claim in this JWT
            MicroProfileJwtConfig mpJwtConfig = buildMockConfig(null, "tenant");
            TAIMappingHelper helper = new TAIMappingHelper(jwtToken, mpJwtConfig);
            helper.createJwtPrincipalAndPopulateCustomProperties(jwtToken, false);

            // Should fall back to iss claim
            Object realm = helper.getCustomProperties().get(AttributeNameConstants.WSCREDENTIAL_REALM);
            assertEquals("Expected realm to fall back to iss claim",
                         "https://9.24.8.103:8947/jwt/jwkEnabled", realm);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void testSetRealm_noConfig_fallbackToIss() {
        // No config (null) path: TAIMappingHelper(JwtToken) uses "upn"/"groups"/false/null
        // so realmIdentifier defaults to REALM_CLAIM="realm", which is absent → falls back to iss
        try {
            String jwt = "eyJraWQiOiJXYlVqSEN5b3V5ZEoySEFKc1dOMSIsImFsZyI6IlJTMjU2In0.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXVkIjoiYXVkMSIsInN1YiI6InRlc3R1c2VyIiwidXBuIjoidGVzdHVzZXIiLCJncm91cHMiOlsiZ3JvdXAxLWFiYyIsImdyb3VwMi1kZWYiLCJ0ZXN0dXNlci1ncm91cCJdLCJpc3MiOiJodHRwczovLzkuMjQuOC4xMDM6ODk0Ny9qd3QvandrRW5hYmxlZCIsImV4cCI6MTUwNDIxMjM5MCwiaWF0IjoxNTA0MjA1MTkwfQ.egKHKw1hfEAmZMBwI1_vPFxZIzXd9UWjLqz1MvlcvT3FHNKyV3CQ8KVSb-DrHll5J57QgJxY_vBiKgZgKkDJn6rKB4LNivV-_mcsCWawjKkmFDjesMLiSFKLfLWpfbt7qVbnRNT7ysMlXMDDJguRHRj_l1M70VAQT9gaCrPsoMvDAzOtTBS0iLnRATFCddYwQsw82Ma4rfTo5Hq-ouQWgRYerxkNswZJRnahsUKoSh4ptjYBmNySBTIF7X0WL9q0gr3SzJA59rLbQLaIhLzv8lYn7GRL05ifegQX41y11peG0_-ySN3nvaYvynwwbVvsJhRWbOc9B9LiYCX_qpxuXA";
            ConsumerUtil consumerUtil = new ConsumerUtil(null);
            JwtConsumerConfigImpl jwtConfig = new JwtConsumerConfigImpl() {
                @Override
                public boolean isValidationRequired() {
                    return false;
                }
            };
            JwtToken jwtToken = consumerUtil.parseJwt(jwt, jwtConfig);

            // Use the no-config constructor
            TAIMappingHelper helper = new TAIMappingHelper(jwtToken);
            helper.createJwtPrincipalAndPopulateCustomProperties(jwtToken, false);

            Object realm = helper.getCustomProperties().get(AttributeNameConstants.WSCREDENTIAL_REALM);
            assertEquals("Expected realm to be iss claim value when no config",
                         "https://9.24.8.103:8947/jwt/jwkEnabled", realm);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }
}
