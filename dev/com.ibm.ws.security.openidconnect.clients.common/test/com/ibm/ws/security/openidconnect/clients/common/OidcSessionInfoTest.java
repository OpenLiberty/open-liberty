/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

/**
 *
 */
public class OidcSessionInfoTest extends CommonTestClass {

    private static final String CLIENT_SECRET = "myClientSecret";

    protected final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    protected final ConvergedClientConfig clientConfig = mockery.mock(ConvergedClientConfig.class);

    @Before
    public void before() {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getClientSecret();
                will(returnValue(CLIENT_SECRET));
            }
        });
    }

    @Test
    public void test_createSessionId() {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = "testSid";
        String exp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, exp, clientConfig);
        String expectedSesssionId = "dGVzdENvbmZpZ0lk:aHR0cHM6Ly9sb2NhbGhvc3Q=:dGVzdFN1Yg==:dGVzdFNpZA==:MTIzNDU=_3ssUE3zEBGw4jK8MkAp6udTSo9PaqTVpJgoE4BwmqUs=";

        assertEquals("Expected to return the session id in the format 'Base64(configId):Base64(sub):Base64(sid):Base64(exp)_Signature(SessionId, ClientSecret)'.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_sidIsEmpty() {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = "";
        String exp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, exp, clientConfig);
        String expectedSesssionId = "dGVzdENvbmZpZ0lk:aHR0cHM6Ly9sb2NhbGhvc3Q=:dGVzdFN1Yg==::MTIzNDU=_cPjhjKp9RSS1oESEIBaiK3x+GDs3Rft83urh4KaDYg8=";

        assertEquals("Expected to the return the session id in the format 'Base64(configId):Base64(sub):Base64(sid):Base64(exp)_Signature(SessionId, ClientSecret)'.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_configIdIsNull() {
        String configId = null;
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = "testSid";
        String exp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, exp, clientConfig);
        String expectedSesssionId = ":aHR0cHM6Ly9sb2NhbGhvc3Q=:dGVzdFN1Yg==:dGVzdFNpZA==:MTIzNDU=_hG1yWN/mOMMxzHGsS8KnpfXKP3e4C74BxDDJZtQQMzk=";

        assertEquals("Expected to replace the configId with an empty string.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_issIsNull() {
        String configId = "testConfigId";
        String iss = null;
        String sub = "testSub";
        String sid = "testSid";
        String exp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, exp, clientConfig);
        String expectedSesssionId = "dGVzdENvbmZpZ0lk::dGVzdFN1Yg==:dGVzdFNpZA==:MTIzNDU=_XViM+YzwC84l+DMxTjTqgaH8oVqkWMmjfsQP6kDgZKk=";

        assertEquals("Expected to replace the sub with an empty string.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_subIsNull() {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = null;
        String sid = "testSid";
        String exp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, exp, clientConfig);
        String expectedSesssionId = "dGVzdENvbmZpZ0lk:aHR0cHM6Ly9sb2NhbGhvc3Q=::dGVzdFNpZA==:MTIzNDU=_JTmRlEJgi/mUGSFs3Jg8j1BOcc/faLKDIxdBE5XlFV4=";

        assertEquals("Expected to replace the sub with an empty string.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_sidIsNull() {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = null;
        String exp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, exp, clientConfig);
        String expectedSesssionId = "dGVzdENvbmZpZ0lk:aHR0cHM6Ly9sb2NhbGhvc3Q=:dGVzdFN1Yg==::MTIzNDU=_cPjhjKp9RSS1oESEIBaiK3x+GDs3Rft83urh4KaDYg8=";

        assertEquals("Expected to replace the sid with an empty string.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_expIsNull() {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = "testSid";
        String exp = null;

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, exp, clientConfig);
        String expectedSesssionId = "dGVzdENvbmZpZ0lk:aHR0cHM6Ly9sb2NhbGhvc3Q=:dGVzdFN1Yg==:dGVzdFNpZA==:_vb86W5kVUmHqMfboDo/jG491Zk5axpk0UqNVPqk5oR4=";

        assertEquals("Expected to replace the exp with an empty string.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_getSessionInfo() {
        String sessionId = "dGVzdENvbmZpZ0lk:aHR0cHM6Ly9sb2NhbGhvc3Q=:dGVzdFN1Yg==:dGVzdFNpZA==:MTIzNDU=_3ssUE3zEBGw4jK8MkAp6udTSo9PaqTVpJgoE4BwmqUs=";
        setupRequestExpectations(sessionId);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, clientConfig);

        assertEquals("testConfigId", sessionInfo.getConfigId());
        assertEquals("https://localhost", sessionInfo.getIss());
        assertEquals("testSub", sessionInfo.getSub());
        assertEquals("testSid", sessionInfo.getSid());
        assertEquals("12345", sessionInfo.getExp());
    }

    @Test
    public void test_getSessinoInfo_sidWasEmptyOrNull() {
        String sessionId = "dGVzdENvbmZpZ0lk:aHR0cHM6Ly9sb2NhbGhvc3Q=:dGVzdFN1Yg==::MTIzNDU=_cPjhjKp9RSS1oESEIBaiK3x+GDs3Rft83urh4KaDYg8=";
        setupRequestExpectations(sessionId);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, clientConfig);

        assertEquals("testConfigId", sessionInfo.getConfigId());
        assertEquals("https://localhost", sessionInfo.getIss());
        assertEquals("testSub", sessionInfo.getSub());
        assertEquals("", sessionInfo.getSid());
        assertEquals("12345", sessionInfo.getExp());
    }

    @Test
    public void test_getSessionInfo_sessionIdIsNull() {
        String sessionId = null;
        setupRequestExpectations(sessionId);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, clientConfig);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test
    public void test_getSessionInfo_sessionIdIsEmpty() {
        String sessionId = "";
        setupRequestExpectations(sessionId);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, clientConfig);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test
    public void test_getSessionInfo_decodedSessionIdDoesNotHaveFourParts() {
        String sessionId = "dGVzdENvbmZpZ0lk:dGVzdFN1Yg==";
        setupRequestExpectations(sessionId);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, clientConfig);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test
    public void test_getSessionInfo_sessionIdIsInvalid() {
        String sessionId = "invalidSessionId";
        setupRequestExpectations(sessionId);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, clientConfig);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    private void setupRequestExpectations(String sessionId) {
        mockery.checking(new Expectations() {
            {
                one(request).getCookies();
                will(returnValue(new Cookie[] { createSessionCookie(sessionId) }));
            }
        });
    }

    private Cookie createSessionCookie(String cookieValue) {
        return new Cookie(ClientConstants.WAS_OIDC_SESSION, cookieValue);
    }
}
