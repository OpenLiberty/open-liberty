/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.backchannellogout.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.ws.security.jwt.config.ConsumerUtils;
import com.ibm.ws.security.openidconnect.backchannellogout.BackchannelLogoutException;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcSessionCache;
import com.ibm.ws.security.openidconnect.clients.common.OidcSessionInfo;
import com.ibm.ws.security.openidconnect.clients.common.OidcSessionsStore;
import com.ibm.ws.security.openidconnect.token.IDTokenValidationFailedException;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.security.test.common.jwt.utils.JwtUnitTestUtils;

import test.common.SharedOutputManager;

public class LogoutTokenValidatorTest extends CommonTestClass {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.openidconnect.common.*=all=enabled");

    final String CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS = "CWWKS1536E";
    final String CWWKS1537E_OIDC_CLIENT_JWE_REQUIRED_BUT_TOKEN_NOT_JWE = "CWWKS1537E";
    final String CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR = "CWWKS1543E";
    final String CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS = "CWWKS1545E";
    final String CWWKS1546E_LOGOUT_TOKEN_MISSING_SUB_AND_SID = "CWWKS1546E";
    final String CWWKS1547E_LOGOUT_TOKEN_EVENTS_CLAIM_WRONG_TYPE = "CWWKS1547E";
    final String CWWKS1548E_LOGOUT_TOKEN_EVENTS_CLAIM_MISSING_EXPECTED_MEMBER = "CWWKS1548E";
    final String CWWKS1549E_LOGOUT_TOKEN_CONTAINS_NONCE_CLAIM = "CWWKS1549E";
    final String CWWKS1550E_LOGOUT_TOKEN_EVENTS_MEMBER_VALUE_NOT_JSON = "CWWKS1550E";
    final String CWWKS1551E_LOGOUT_TOKEN_DUP_JTI = "CWWKS1551E";
    final String CWWKS1552E_NO_RECENT_SESSIONS_WITH_CLAIMS = "CWWKS1552E";

    final String CWWKS1751E_OIDC_IDTOKEN_VERIFY_ISSUER_ERR = "CWWKS1751E";
    final String CWWKS1754E_OIDC_IDTOKEN_VERIFY_AUD_ERR = "CWWKS1754E";
    final String CWWKS1778E_OIDC_JWT_SIGNATURE_VERIFY_MISSING_SIGNATURE_ERR = "CWWKS1778E";
    final String CWWKS1784E_JWT_MISSING_ISSUER = "CWWKS1784E";

    final String CONFIG_ID = "myConfigId";
    final String CLIENT_ID = "client01";
    final String SHARED_SECRET = "secret";
    final String ISSUER = "https://localhost/oidc/provider/OP";
    final String TOKEN_ENDPOINT = ISSUER + "/token";
    final String SUBJECT = "testuser";
    final String SID = "jwtsid";
    final String EVENTS_MEMBER_KEY = "http://schemas.openid.net/event/backchannel-logout";
    final String CLIENT_SECRET = "myClientSecret";

    final ConvergedClientConfig clientConfig = mockery.mock(ConvergedClientConfig.class);
    final ConsumerUtils consumerUtils = mockery.mock(ConsumerUtils.class);
    final OidcSessionCache oidcSessionCache = mockery.mock(OidcSessionCache.class);

    LogoutTokenValidator validator;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        validator = new LogoutTokenValidator(clientConfig);
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getId();
                will(returnValue(CONFIG_ID));
                allowing(clientConfig).getClientSecret();
                will(returnValue(CLIENT_SECRET));
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_validateToken_nullString() throws Exception {
        String logoutTokenString = null;
        try {
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS);
        }
    }

    @Test
    public void test_validateToken_emptyString() throws Exception {
        String logoutTokenString = "";
        try {
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS);
        }
    }

    @Test
    public void test_validateToken_notAJwt() throws Exception {
        String logoutTokenString = "This is not a jwt.";
        try {
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS);
        }
    }

    @Test
    public void test_validateToken_jwsRequired_jweFormat() throws Exception {
        String logoutTokenString = "aaa.bbb.ccc.ddd.eee";
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS);
        }
    }

    @Test
    public void test_validateToken_jweRequired_jwsFormat() throws Exception {
        String logoutTokenString = "aaa.bbb.ccc";
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue("someAlias"));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1537E_OIDC_CLIENT_JWE_REQUIRED_BUT_TOKEN_NOT_JWE);
        }
    }

    @Test
    public void test_validateToken_jwsUnsigned_configAlgHs256() throws Exception {
        String logoutTokenString = JwtUnitTestUtils.encode(JwtUnitTestUtils.getJwsHeader("none")) + "." + JwtUnitTestUtils.encode("{}") + ".";
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1778E_OIDC_JWT_SIGNATURE_VERIFY_MISSING_SIGNATURE_ERR);
        }
    }

    @Test
    public void test_validateToken_jwsUnsigned_configAlgNone_emptyClaims() throws Exception {
        String logoutTokenString = JwtUnitTestUtils.encode(JwtUnitTestUtils.getJwsHeader("none")) + "." + JwtUnitTestUtils.encode("{}") + ".";
        try {
            setConfigExpectations("none", null, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + "iss, aud, iat, exp, jti, events");
        }
    }

    @Test
    public void test_validateToken_jwsUnsigned_minimumClaims() throws Exception {
        JSONObject claims = getMinimumClaimsNoSid();
        String logoutTokenString = JwtUnitTestUtils.encode(JwtUnitTestUtils.getJwsHeader("none")) + "." + JwtUnitTestUtils.encode(claims) + ".";
        try {
            setConfigExpectations("none", null, 300L, ISSUER);
            setSuccessfulOptionalTokenValidationExpectations((String) claims.get(Claims.SUBJECT));

            validator.validateToken(logoutTokenString);
        } catch (BackchannelLogoutException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_validateToken_hs256_keyMismatch() throws Exception {
        JSONObject claims = new JSONObject();
        String logoutTokenString = JwtUnitTestUtils.getHS256Jws(claims, "secret2");
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + "Invalid JWS Signature");
        }
    }

    @Test
    public void test_validateToken_hs256_emptyClaims() throws Exception {
        JSONObject claims = new JSONObject();
        String logoutTokenString = JwtUnitTestUtils.getHS256Jws(claims, SHARED_SECRET);
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + "iss, aud, iat, exp, jti, events");
        }
    }

    @Test
    public void test_validateToken_hs256_minimumClaims() throws Exception {
        JSONObject claims = getMinimumClaimsNoSid();
        String logoutTokenString = JwtUnitTestUtils.getHS256Jws(claims, SHARED_SECRET);
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);
            setSuccessfulOptionalTokenValidationExpectations((String) claims.get(Claims.SUBJECT));

            validator.validateToken(logoutTokenString);
        } catch (BackchannelLogoutException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_verifyAllRequiredClaimsArePresent_emptyClaims() throws Exception {
        JSONObject jsonClaims = new JSONObject();
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifyAllRequiredClaimsArePresent(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + "iss, aud, iat, exp, jti, events");
        }
    }

    @Test
    public void test_verifyAllRequiredClaimsArePresent_missingIss() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        String claimToRemove = Claims.ISSUER;
        jsonClaims.remove(claimToRemove);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifyAllRequiredClaimsArePresent(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + claimToRemove + "[^,]");
        }
    }

    @Test
    public void test_verifyAllRequiredClaimsArePresent_missingAud() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        String claimToRemove = Claims.AUDIENCE;
        jsonClaims.remove(claimToRemove);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifyAllRequiredClaimsArePresent(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + claimToRemove + "[^,]");
        }
    }

    @Test
    public void test_verifyAllRequiredClaimsArePresent_missingIat() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        String claimToRemove = Claims.ISSUED_AT;
        jsonClaims.remove(claimToRemove);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifyAllRequiredClaimsArePresent(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + claimToRemove + "[^,]");
        }
    }

    @Test
    public void test_verifyAllRequiredClaimsArePresent_missingExp() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        String claimToRemove = Claims.EXPIRATION;
        jsonClaims.remove(claimToRemove);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifyAllRequiredClaimsArePresent(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + claimToRemove + "[^,]");
        }
    }

    @Test
    public void test_verifyAllRequiredClaimsArePresent_missingJti() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        String claimToRemove = Claims.ID;
        jsonClaims.remove(claimToRemove);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifyAllRequiredClaimsArePresent(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + claimToRemove + "[^,]");
        }
    }

    @Test
    public void test_verifyAllRequiredClaimsArePresent_missingEvents() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        String claimToRemove = "events";
        jsonClaims.remove(claimToRemove);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifyAllRequiredClaimsArePresent(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + claimToRemove + "[^,]");
        }
    }

    @Test
    public void test_verifyAllRequiredClaimsArePresent_valid() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifyAllRequiredClaimsArePresent(claims);
        } catch (Exception e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_verifyIssAudIatExpClaims_badIss() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        jsonClaims.put(Claims.ISSUER, "https://localhost/oidc/provider/NOPE");
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            setConfigExpectations("HS256", null, 300L, ISSUER);

            validator.verifyIssAudIatExpClaims(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (IDTokenValidationFailedException e) {
            verifyException(e, CWWKS1751E_OIDC_IDTOKEN_VERIFY_ISSUER_ERR);
        }
    }

    @Test
    public void test_verifyIssAudIatExpClaims_badAud() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        jsonClaims.put(Claims.AUDIENCE, "client02");
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            setConfigExpectations("HS256", null, 300L, ISSUER);

            validator.verifyIssAudIatExpClaims(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (IDTokenValidationFailedException e) {
            verifyException(e, CWWKS1754E_OIDC_IDTOKEN_VERIFY_AUD_ERR);
        }
    }

    @Test
    public void test_verifySubAndOrSidPresent_missingSubAndSid() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        jsonClaims.remove(Claims.SUBJECT);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifySubAndOrSidPresent(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1546E_LOGOUT_TOKEN_MISSING_SUB_AND_SID);
        }
    }

    @Test
    public void test_verifySubAndOrSidPresent_hasSidMissingSub() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        jsonClaims.remove(Claims.SUBJECT);
        jsonClaims.put("sid", SID);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifySubAndOrSidPresent(claims);
        } catch (Exception e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_verifySubAndOrSidPresent_hasSubMissingSid() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifySubAndOrSidPresent(claims);
        } catch (Exception e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_verifyEventsClaim_wrongType() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        jsonClaims.put("events", "string");
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifyEventsClaim(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1547E_LOGOUT_TOKEN_EVENTS_CLAIM_WRONG_TYPE);
        }
    }

    @Test
    public void test_verifyEventsClaim_missingRequiredMember() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        JSONObject eventsValue = new JSONObject();
        eventsValue.put("entry1", "value1");
        eventsValue.put("entry2", true);
        jsonClaims.put("events", eventsValue);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifyEventsClaim(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1548E_LOGOUT_TOKEN_EVENTS_CLAIM_MISSING_EXPECTED_MEMBER);
        }
    }

    @Test
    public void test_verifyEventsClaim_memberValueWrongType() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        JSONObject eventsValue = new JSONObject();
        eventsValue.put(EVENTS_MEMBER_KEY, "string");
        jsonClaims.put("events", eventsValue);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifyEventsClaim(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1550E_LOGOUT_TOKEN_EVENTS_MEMBER_VALUE_NOT_JSON);
        }
    }

    @Test
    public void test_verifyEventsClaim_valid() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifyEventsClaim(claims);
        } catch (Exception e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_verifyNonceClaimNotPresent_noncePresent() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        jsonClaims.put("nonce", "somevalue");
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifyNonceClaimNotPresent(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1549E_LOGOUT_TOKEN_CONTAINS_NONCE_CLAIM);
        }
    }

    @Test
    public void test_verifyNonceClaimNotPresent_valid() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        try {
            validator.verifyNonceClaimNotPresent(claims);
        } catch (Exception e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_verifyTokenWithSameJtiNotRecentlyReceived_malformedJti() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        jsonClaims.put(Claims.ID, true);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());

        try {
            validator.verifyTokenWithSameJtiNotRecentlyReceived(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (MalformedClaimException e) {
            // Expected
        }
    }

    @Test
    public void test_verifyTokenWithSameJtiNotRecentlyReceived_missingJti() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        jsonClaims.remove(Claims.ID);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());

        // A JWT missing the jti claim should essentially be ignored
        validator.verifyTokenWithSameJtiNotRecentlyReceived(claims);
        validator.verifyTokenWithSameJtiNotRecentlyReceived(claims);
    }

    @Test
    public void test_verifyTokenWithSameJtiNotRecentlyReceived_differentJti() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());
        jsonClaims.put(Claims.ID, testName.getMethodName() + "2");
        JwtClaims claims2 = JwtClaims.parse(jsonClaims.toString());

        long clockSkew = 10;
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getClockSkew();
                will(returnValue(clockSkew * 1000));
            }
        });
        // Two different jtis should be allowed
        validator.verifyTokenWithSameJtiNotRecentlyReceived(claims);
        validator.verifyTokenWithSameJtiNotRecentlyReceived(claims2);
    }

    @Test
    public void test_verifyTokenWithSameJtiNotRecentlyReceived_reusedJti() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());

        long clockSkew = 10;
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getClockSkew();
                will(returnValue(clockSkew * 1000));
            }
        });
        // A reused jti should not be allowed
        validator.verifyTokenWithSameJtiNotRecentlyReceived(claims);
        try {
            validator.verifyTokenWithSameJtiNotRecentlyReceived(claims);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1551E_LOGOUT_TOKEN_DUP_JTI);
        }
    }

    @Test
    public void test_verifySubClaimMatchesRecentSession_subNotFoundInCache() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());

        Map<String, OidcSessionsStore> subToSessionsMap = createSubMap("some other person", SID);

        mockery.checking(new Expectations() {
            {
                one(oidcSessionCache).getSubMap();
                will(returnValue(subToSessionsMap));
            }
        });
        try {
            validator.verifySubAndSidClaimsMatchRecentSession(claims, oidcSessionCache);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1552E_NO_RECENT_SESSIONS_WITH_CLAIMS + ".*" + ISSUER + ".*" + SUBJECT + ".*" + null);
        }
    }

    @Test
    public void test_verifySubAndSidClaimsMatchRecentSession_issuerDoesNotMatch() throws Exception {
        String tokenIssuer = "http://otherissuer";
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        jsonClaims.put("sid", SID);
        jsonClaims.put(Claims.ISSUER, tokenIssuer);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());

        Map<String, OidcSessionsStore> subToSessionsMap = createSubMap(SUBJECT, SID);

        mockery.checking(new Expectations() {
            {
                one(oidcSessionCache).getSubMap();
                will(returnValue(subToSessionsMap));
            }
        });
        try {
            validator.verifySubAndSidClaimsMatchRecentSession(claims, oidcSessionCache);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1552E_NO_RECENT_SESSIONS_WITH_CLAIMS + ".*" + tokenIssuer + ".*" + SUBJECT + ".*" + SID);
        }
    }

    @Test
    public void test_verifySubAndSidClaimsMatchRecentSession_issuerMatches() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        jsonClaims.put("sid", SID);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());

        Map<String, OidcSessionsStore> subToSessionsMap = createSubMap(SUBJECT, SID);

        mockery.checking(new Expectations() {
            {
                one(oidcSessionCache).getSubMap();
                will(returnValue(subToSessionsMap));
            }
        });
        validator.verifySubAndSidClaimsMatchRecentSession(claims, oidcSessionCache);
    }

    @Test
    public void test_findSessionMatchingIssAndSid_sidNotNull() throws Exception {
        OidcSessionsStore sessionDataForSub = new OidcSessionsStore();
        OidcSessionInfo oidcSessionInfo = new OidcSessionInfo(CONFIG_ID, ISSUER, SUBJECT, SID, "1234", clientConfig);
        sessionDataForSub.insertSession(oidcSessionInfo.getSid(), oidcSessionInfo);

        OidcSessionInfo session = validator.findSessionMatchingIssAndSid(sessionDataForSub, ISSUER, SID);
        assertNotNull("Should have found a session, but didn't.", session);
        assertEquals("Retrieved session did not match the original session.", oidcSessionInfo, session);
    }

    @Test
    public void test_findSessionMatchingIssAndSid_sidNull() throws Exception {
        OidcSessionsStore sessionDataForSub = new OidcSessionsStore();
        OidcSessionInfo oidcSessionInfo = new OidcSessionInfo(CONFIG_ID, ISSUER, SUBJECT, null, "1234", clientConfig);
        sessionDataForSub.insertSession(null, oidcSessionInfo);

        OidcSessionInfo session = validator.findSessionMatchingIssAndSid(sessionDataForSub, ISSUER, null);
        assertNotNull("Should have found a session, but didn't.", session);
        assertEquals("Retrieved session did not match the original session.", oidcSessionInfo, session);
    }

    @Test
    public void test_findSessionMatchingIssAndNonNullSid_noSessions() throws Exception {
        OidcSessionsStore sessionDataForSub = new OidcSessionsStore();

        OidcSessionInfo session = validator.findSessionMatchingIssAndNonNullSid(sessionDataForSub, ISSUER, SID);
        assertNull("Should not have found a session, but found: " + session, session);
    }

    @Test
    public void test_findSessionMatchingIssAndNonNullSid_oneSession_sidDoesNotMatch() throws Exception {
        OidcSessionsStore sessionDataForSub = new OidcSessionsStore();
        String sid = "some other sid";
        OidcSessionInfo oidcSessionInfo = new OidcSessionInfo(CONFIG_ID, ISSUER, SUBJECT, sid, "1234", clientConfig);
        sessionDataForSub.insertSession(sid, oidcSessionInfo);

        OidcSessionInfo session = validator.findSessionMatchingIssAndNonNullSid(sessionDataForSub, ISSUER, SID);
        assertNull("Should not have found a session, but found: " + session, session);
    }

    @Test
    public void test_findSessionMatchingIssAndNonNullSid_multipleSessions_sidDoesNotMatch() throws Exception {
        OidcSessionsStore sessionDataForSub = new OidcSessionsStore();
        OidcSessionInfo oidcSessionInfo1 = new OidcSessionInfo(CONFIG_ID, ISSUER, SUBJECT, null, "1234", clientConfig);
        OidcSessionInfo oidcSessionInfo2 = new OidcSessionInfo(CONFIG_ID, ISSUER, SUBJECT, "sid1", "2345", clientConfig);
        OidcSessionInfo oidcSessionInfo3 = new OidcSessionInfo(CONFIG_ID, ISSUER, SUBJECT, "sid2", "3456", clientConfig);
        sessionDataForSub.insertSession(null, oidcSessionInfo1);
        sessionDataForSub.insertSession(oidcSessionInfo2.getSid(), oidcSessionInfo2);
        sessionDataForSub.insertSession(oidcSessionInfo3.getSid(), oidcSessionInfo3);

        OidcSessionInfo session = validator.findSessionMatchingIssAndNonNullSid(sessionDataForSub, ISSUER, SID);
        assertNull("Should not have found a session, but found: " + session, session);
    }

    @Test
    public void test_findSessionMatchingIssAndNonNullSid_oneSession_issDoesNotMatch() throws Exception {
        OidcSessionsStore sessionDataForSub = new OidcSessionsStore();
        OidcSessionInfo oidcSessionInfo = new OidcSessionInfo(CONFIG_ID, "some other issuer", SUBJECT, SID, "1234", clientConfig);
        sessionDataForSub.insertSession(null, oidcSessionInfo);

        OidcSessionInfo session = validator.findSessionMatchingIssAndNonNullSid(sessionDataForSub, ISSUER, SID);
        assertNull("Should not have found a session, but found: " + session, session);
    }

    @Test
    public void test_findSessionMatchingIssAndNonNullSid_oneSession_issMatches() throws Exception {
        OidcSessionsStore sessionDataForSub = new OidcSessionsStore();
        OidcSessionInfo oidcSessionInfo = new OidcSessionInfo(CONFIG_ID, ISSUER, SUBJECT, SID, "1234", clientConfig);
        sessionDataForSub.insertSession(oidcSessionInfo.getSid(), oidcSessionInfo);

        OidcSessionInfo session = validator.findSessionMatchingIssAndNonNullSid(sessionDataForSub, ISSUER, SID);
        assertNotNull("Should have found a session, but didn't.", session);
        assertEquals("Retrieved session did not match the original session.", oidcSessionInfo, session);
    }

    @Test
    public void test_findSessionMatchingIssAndNonNullSid_multipleSessions_sidMatch_issDoesNotMatch() throws Exception {
        OidcSessionsStore sessionDataForSub = new OidcSessionsStore();
        OidcSessionInfo oidcSessionInfo1 = new OidcSessionInfo(CONFIG_ID, ISSUER, SUBJECT, null, "1234", clientConfig);
        OidcSessionInfo oidcSessionInfo2 = new OidcSessionInfo(CONFIG_ID, ISSUER, SUBJECT, "sid1", "2345", clientConfig);
        OidcSessionInfo oidcSessionInfo3 = new OidcSessionInfo(CONFIG_ID, "https://otherissuer", SUBJECT, SID, "3456", clientConfig);
        sessionDataForSub.insertSession(null, oidcSessionInfo1);
        sessionDataForSub.insertSession(oidcSessionInfo2.getSid(), oidcSessionInfo2);
        sessionDataForSub.insertSession(oidcSessionInfo3.getSid(), oidcSessionInfo3);

        OidcSessionInfo session = validator.findSessionMatchingIssAndNonNullSid(sessionDataForSub, ISSUER, SID);
        assertNull("Should not have found a session, but found: " + session, session);
    }

    @Test
    public void test_findSessionMatchingIssAndNonNullSid_multipleSessions_sidMatch_issMatches() throws Exception {
        OidcSessionsStore sessionDataForSub = new OidcSessionsStore();
        OidcSessionInfo oidcSessionInfo1 = new OidcSessionInfo(CONFIG_ID, ISSUER, SUBJECT, null, "1234", clientConfig);
        OidcSessionInfo oidcSessionInfo2 = new OidcSessionInfo(CONFIG_ID, ISSUER, SUBJECT, "sid1", "2345", clientConfig);
        OidcSessionInfo oidcSessionInfo3 = new OidcSessionInfo(CONFIG_ID, ISSUER, SUBJECT, SID, "3456", clientConfig);
        sessionDataForSub.insertSession(null, oidcSessionInfo1);
        sessionDataForSub.insertSession(oidcSessionInfo2.getSid(), oidcSessionInfo2);
        sessionDataForSub.insertSession(oidcSessionInfo3.getSid(), oidcSessionInfo3);

        OidcSessionInfo session = validator.findSessionMatchingIssAndNonNullSid(sessionDataForSub, ISSUER, SID);
        assertNotNull("Should have found a session, but didn't.", session);
        assertEquals("Retrieved session did not match the original session.", oidcSessionInfo3, session);
    }

    @Test
    public void test_findSessionMatchingOnlyIss_noSessions() throws Exception {
        OidcSessionsStore sessionDataForSub = new OidcSessionsStore();

        OidcSessionInfo session = validator.findSessionMatchingOnlyIss(sessionDataForSub, ISSUER);
        assertNull("Should not have found a session, but found: " + session, session);
    }

    @Test
    public void test_findSessionMatchingOnlyIss_oneSession_issDoesNotMatch() throws Exception {
        OidcSessionsStore sessionDataForSub = new OidcSessionsStore();
        OidcSessionInfo oidcSessionInfo = new OidcSessionInfo(CONFIG_ID, "some other issuer", SUBJECT, null, "1234", clientConfig);
        sessionDataForSub.insertSession(null, oidcSessionInfo);

        OidcSessionInfo session = validator.findSessionMatchingOnlyIss(sessionDataForSub, ISSUER);
        assertNull("Should not have found a session, but found: " + session, session);
    }

    @Test
    public void test_findSessionMatchingOnlyIss_oneSession_issMatches() throws Exception {
        OidcSessionsStore sessionDataForSub = new OidcSessionsStore();
        OidcSessionInfo oidcSessionInfo = new OidcSessionInfo(CONFIG_ID, ISSUER, SUBJECT, null, "1234", clientConfig);
        sessionDataForSub.insertSession(null, oidcSessionInfo);

        OidcSessionInfo session = validator.findSessionMatchingOnlyIss(sessionDataForSub, ISSUER);
        assertNotNull("Should have found a session, but didn't.", session);
        assertEquals("Retrieved session did not match the original session.", oidcSessionInfo, session);
    }

    @Test
    public void test_findSessionMatchingOnlyIss_multipleSessions_noIssMatches() throws Exception {
        OidcSessionsStore sessionDataForSub = new OidcSessionsStore();
        OidcSessionInfo oidcSessionInfo1 = new OidcSessionInfo(CONFIG_ID, "https://issuer1", SUBJECT, null, "1234", clientConfig);
        OidcSessionInfo oidcSessionInfo2 = new OidcSessionInfo(CONFIG_ID, "https://issuer1", SUBJECT, "sid1", "2345", clientConfig);
        OidcSessionInfo oidcSessionInfo3 = new OidcSessionInfo(CONFIG_ID, "https://issuer2", SUBJECT, "sid2", "3456", clientConfig);
        sessionDataForSub.insertSession(null, oidcSessionInfo1);
        sessionDataForSub.insertSession(oidcSessionInfo2.getSid(), oidcSessionInfo2);
        sessionDataForSub.insertSession(oidcSessionInfo3.getSid(), oidcSessionInfo3);

        OidcSessionInfo session = validator.findSessionMatchingOnlyIss(sessionDataForSub, ISSUER);
        assertNull("Should not have found a session, but found: " + session, session);
    }

    @Test
    public void test_findSessionMatchingOnlyIss_multipleSessions_issMatches() throws Exception {
        OidcSessionsStore sessionDataForSub = new OidcSessionsStore();
        OidcSessionInfo oidcSessionInfo1 = new OidcSessionInfo(CONFIG_ID, "https://issuer1", SUBJECT, null, "1234", clientConfig);
        OidcSessionInfo oidcSessionInfo2 = new OidcSessionInfo(CONFIG_ID, ISSUER, SUBJECT, "sid1", "2345", clientConfig);
        OidcSessionInfo oidcSessionInfo3 = new OidcSessionInfo(CONFIG_ID, "https://issuer2", SUBJECT, "sid2", "3456", clientConfig);
        sessionDataForSub.insertSession(null, oidcSessionInfo1);
        sessionDataForSub.insertSession(oidcSessionInfo2.getSid(), oidcSessionInfo2);
        sessionDataForSub.insertSession(oidcSessionInfo3.getSid(), oidcSessionInfo3);

        OidcSessionInfo session = validator.findSessionMatchingOnlyIss(sessionDataForSub, ISSUER);
        assertNotNull("Should have found a session, but didn't.", session);
        assertEquals("Retrieved session did not match the expected session.", oidcSessionInfo2, session);
    }

    @Test
    public void test_verifySidClaimMatchesRecentSession_malformedSid() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        jsonClaims.put("sid", 123);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());

        try {
            validator.verifySidClaimMatchesRecentSession(claims, oidcSessionCache);
            fail("Should have thrown an exception but didn't.");
        } catch (MalformedClaimException e) {
            // Expected
        }
    }

    @Test
    public void test_verifySidClaimMatchesRecentSession_sidMissing() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());

        // Token doesn't have to contain a sid claim
        validator.verifySidClaimMatchesRecentSession(claims, oidcSessionCache);
    }

    @Test
    public void test_verifySidClaimMatchesRecentSession_noCachedSessions() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        jsonClaims.put("sid", SID);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());

        Map<String, OidcSessionsStore> subToSessionsMap = createSubMap(null, null);

        mockery.checking(new Expectations() {
            {
                one(oidcSessionCache).getSubMap();
                will(returnValue(subToSessionsMap));
            }
        });
        try {
            validator.verifySidClaimMatchesRecentSession(claims, oidcSessionCache);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1552E_NO_RECENT_SESSIONS_WITH_CLAIMS + ".*" + SID);
        }
    }

    @Test
    public void test_verifySidClaimMatchesRecentSession_sidNotFoundInCache() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        jsonClaims.put("sid", SID);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());

        Map<String, OidcSessionsStore> subToSessionsMap = createSubMap(SUBJECT, "some other sid");

        mockery.checking(new Expectations() {
            {
                one(oidcSessionCache).getSubMap();
                will(returnValue(subToSessionsMap));
            }
        });
        try {
            validator.verifySidClaimMatchesRecentSession(claims, oidcSessionCache);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1552E_NO_RECENT_SESSIONS_WITH_CLAIMS + ".*" + SID);
        }
    }

    @Test
    public void test_verifySidClaimMatchesRecentSession_sidFound() throws Exception {
        JSONObject jsonClaims = getMinimumClaimsNoSid();
        jsonClaims.put("sid", SID);
        JwtClaims claims = JwtClaims.parse(jsonClaims.toString());

        Map<String, OidcSessionsStore> subToSessionsMap = createSubMap(SUBJECT, SID);

        mockery.checking(new Expectations() {
            {
                one(oidcSessionCache).getSubMap();
                will(returnValue(subToSessionsMap));
            }
        });

        validator.verifySidClaimMatchesRecentSession(claims, oidcSessionCache);
    }

    private JSONObject getMinimumClaimsNoSid() {
        JSONObject claims = new JSONObject();
        claims.put(Claims.ISSUER, ISSUER);
        claims.put(Claims.AUDIENCE, CLIENT_ID);
        claims.put(Claims.ISSUED_AT, System.currentTimeMillis() / 1000);
        claims.put(Claims.EXPIRATION, (long) claims.get(Claims.ISSUED_AT) + 120);
        claims.put(Claims.ID, testName.getMethodName());
        claims.put("events", getValidEventsEntry());
        claims.put(Claims.SUBJECT, SUBJECT);
        return claims;
    }

    private JSONObject getValidEventsEntry() {
        JSONObject events = new JSONObject();
        events.put(EVENTS_MEMBER_KEY, new JSONObject());
        return events;
    }

    private void setConfigExpectations(String signatureAlgorithm, String sharedKey, long clockSkew, String issuerIdentifier) {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getKeyManagementKeyAlias();
                will(returnValue(null));
                allowing(clientConfig).getSignatureAlgorithm();
                will(returnValue(signatureAlgorithm));
                allowing(clientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                allowing(clientConfig).getClockSkewInSeconds();
                will(returnValue(clockSkew));
                allowing(clientConfig).getClockSkew();
                will(returnValue(clockSkew * 1000));
                allowing(clientConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        if (sharedKey != null) {
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getSharedKey();
                    will(returnValue(sharedKey));
                }
            });
        }
    }

    private void setSuccessfulOptionalTokenValidationExpectations(String sub) {
        Map<String, OidcSessionsStore> subToSessionsMap = createSubMap(sub, null);

        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getOidcSessionCache();
                will(returnValue(oidcSessionCache));
                one(oidcSessionCache).getSubMap();
                will(returnValue(subToSessionsMap));
            }
        });
    }

    private Map<String, OidcSessionsStore> createSubMap(String sub, String sid) {
        Map<String, OidcSessionsStore> subToSessionsMap = new HashMap<>();
        if (sub != null || sid != null) {
            OidcSessionsStore sessionStore = new OidcSessionsStore();
            OidcSessionInfo oidcSessionInfo = new OidcSessionInfo(HashUtils.digest(CONFIG_ID), HashUtils.digest(ISSUER), HashUtils.digest(sub), HashUtils.digest(sid), "1234", clientConfig);
            sessionStore.insertSession(HashUtils.digest(sid), oidcSessionInfo);
            subToSessionsMap.put(HashUtils.digest(sub), sessionStore);
        }
        return subToSessionsMap;
    }

}
