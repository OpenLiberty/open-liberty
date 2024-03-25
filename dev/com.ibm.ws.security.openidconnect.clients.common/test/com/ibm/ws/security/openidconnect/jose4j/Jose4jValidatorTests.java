/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.jose4j;

import static org.junit.Assert.fail;

import java.security.Key;
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

import com.ibm.ws.security.openidconnect.clients.common.OidcClientRequest;
import com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

/**
 *
 */
public class Jose4jValidatorTests extends CommonTestClass {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.openidconnect.common.*=all=enabled");

    final String CWWKS1785E_JWT_HAS_LOGOUT_TOKEN_TYP_HEADER = "CWWKS1785E";
    final String CWWKS1786E_JWT_HAS_LOGOUT_TOKEN_EVENTS_CLAIM = "CWWKS1786E";

    final String BACKCHANNEL_LOGOUT_EVENT = "http://schemas.openid.net/event/backchannel-logout";

    final String ISSUER = "https://localhost/oidc/provider/OP";
    final String CLIENT_ID = "client01";
    final String SIGNATURE_ALGORITHM = "RS256";

    final Key key = mockery.mock(Key.class);
    final OidcClientRequest oidcClientRequest = mockery.mock(OidcClientRequest.class);
    final JwtClaims jwtClaims = mockery.mock(JwtClaims.class);

    Jose4jValidator validator;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        validator = new Jose4jValidator(key, 0, ISSUER, CLIENT_ID, SIGNATURE_ALGORITHM, oidcClientRequest);
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
    public void test_verifyHeaderType_nullType() {
        try {
            validator.verifyHeaderType(null);
        } catch (JWTTokenValidationFailedException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_verifyHeaderType_emptyType() {
        try {
            validator.verifyHeaderType("");
        } catch (JWTTokenValidationFailedException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_verifyHeaderType_JWTType() {
        try {
            validator.verifyHeaderType("JWT");
        } catch (JWTTokenValidationFailedException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_verifyHeaderType_LogoutJWTType() {
        try {
            validator.verifyHeaderType("logout+jwt");
            fail("Should have thrown an exception but didn't.");
        } catch (JWTTokenValidationFailedException e) {
            verifyException(e, CWWKS1785E_JWT_HAS_LOGOUT_TOKEN_TYP_HEADER);
        }
    }

    @Test
    public void test_verifyEvents_nullEventsClaim() throws MalformedClaimException {
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtClaims).getClaimValue("events", Map.class);
                    will(returnValue(null));
                }
            });
            validator.verifyEventsClaim(jwtClaims);
        } catch (JWTTokenValidationFailedException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_verifyEvents_emptyEventsClaim() throws MalformedClaimException {
        Map<String, Object> events = new HashMap<>();
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtClaims).getClaimValue("events", Map.class);
                    will(returnValue(events));
                }
            });
            validator.verifyEventsClaim(jwtClaims);
        } catch (JWTTokenValidationFailedException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_verifyEvents_eventsClaimWithRandomEvent() throws MalformedClaimException {
        Map<String, Object> events = new HashMap<>();
        events.put("random123", new HashMap<>());
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtClaims).getClaimValue("events", Map.class);
                    will(returnValue(events));
                }
            });
            validator.verifyEventsClaim(jwtClaims);
        } catch (JWTTokenValidationFailedException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_verifyEvents_eventsClaimWithBackchannelLogoutEvent() throws MalformedClaimException {
        Map<String, Object> events = new HashMap<>();
        events.put(BACKCHANNEL_LOGOUT_EVENT, new HashMap<>());
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtClaims).getClaimValue("events", Map.class);
                    will(returnValue(events));
                }
            });
            validator.verifyEventsClaim(jwtClaims);
            fail("Should have thrown an exception but didn't.");
        } catch (JWTTokenValidationFailedException e) {
            verifyException(e, CWWKS1786E_JWT_HAS_LOGOUT_TOKEN_EVENTS_CLAIM);
        }
    }

}
