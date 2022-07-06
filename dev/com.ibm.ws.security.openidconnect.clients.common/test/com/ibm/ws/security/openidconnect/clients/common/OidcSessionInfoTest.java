/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

public class OidcSessionInfoTest extends CommonTestClass {

    protected final HttpServletRequest request = mockery.mock(HttpServletRequest.class);

    private static String clientSecret = "abcd";

    @Test
    public void test_createSessionId() throws OidcSessionException {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = "testSid";
        String timestamp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, timestamp, clientSecret);
        String expectedSesssionId = "fb58fe21d28e8fa08aa606be6c027f13b9481d33442c4c25db455d40cee3d3a8a30c71128053e3fcafc1c31c96385357456bc4603869d2fe7485ac6b73ca843ac998f6d4b23afb80e33160fc75adcceb";

        assertEquals("Expected to the return the session id in the format 'AES(Base64(configId):Base64(sub):Base64(sid):Base64(timestamp), clientSecret)'.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_sidIsEmpty() throws OidcSessionException {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = "";
        String timestamp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, timestamp, clientSecret);
        String expectedSesssionId = "fb58fe21d28e8fa08aa606be6c027f13b9481d33442c4c25db455d40cee3d3a8a30c71128053e3fcafc1c31c963853570ea2a2d7fe51e55f5bd91220125684166ddde5c6dd51e2a0e907537ddafc01e7";

        assertEquals("Expected to the return the session id in the format 'AES(Base64(configId):Base64(sub):Base64(sid):Base64(timestamp), clientSecet)'.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_configIdIsNull() throws OidcSessionException {
        String configId = null;
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = "testSid";
        String timestamp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, timestamp, clientSecret);
        String expectedSesssionId = "d5431bab1c592377f590457f98aa4f0345ebbf825bdc146513c0547f037be12c8a3a29855ef820d4b289e0f558630adff3fdb9124707d4deb50a0a98005b2c09";

        assertEquals("Expected to replace the configId with an empty string.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_issIsNull() throws OidcSessionException {
        String configId = "testConfigId";
        String iss = null;
        String sub = "testSub";
        String sid = "testSid";
        String timestamp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, timestamp, clientSecret);
        String expectedSesssionId = "fb58fe21d28e8fa08aa606be6c027f13f871f4a697537c27dc4e1d1dedac805e70ab31dc4403fd0956838bc5c5a59e18f5be95c6fb3e7f698210c4029837ab78";

        assertEquals("Expected to replace the sub with an empty string.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_subIsNull() throws OidcSessionException {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = null;
        String sid = "testSid";
        String timestamp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, timestamp, clientSecret);
        String expectedSesssionId = "fb58fe21d28e8fa08aa606be6c027f13b9481d33442c4c25db455d40cee3d3a8af74c02af7415304095b5d19c639e3e98e9aa91681bc342457203503d25dd0628f31abfc187b1fe0e87d0f09511f94f7";

        assertEquals("Expected to replace the sub with an empty string.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_sidIsNull() throws OidcSessionException {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = null;
        String timestamp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, timestamp, clientSecret);
        String expectedSessionId = "fb58fe21d28e8fa08aa606be6c027f13b9481d33442c4c25db455d40cee3d3a8a30c71128053e3fcafc1c31c963853570ea2a2d7fe51e55f5bd91220125684166ddde5c6dd51e2a0e907537ddafc01e7";

        assertEquals("Expected to replace the sid with an empty string.", expectedSessionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_timestampIsNull() throws OidcSessionException {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = "testSid";
        String timestamp = null;

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, timestamp, clientSecret);
        String expectedSesssionId = "fb58fe21d28e8fa08aa606be6c027f13b9481d33442c4c25db455d40cee3d3a8a30c71128053e3fcafc1c31c96385357456bc4603869d2fe7485ac6b73ca843ab2e2b9fa523588d5dab3ec577b67f63f";

        assertEquals("Expected to replace the timestamp with an empty string.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_getSessionInfo() throws OidcSessionException {
        String sessionId = "fb58fe21d28e8fa08aa606be6c027f13b9481d33442c4c25db455d40cee3d3a8a30c71128053e3fcafc1c31c96385357456bc4603869d2fe7485ac6b73ca843ac998f6d4b23afb80e33160fc75adcceb";
        setupRequestExpectations(sessionId);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, clientSecret);

        assertEquals("testConfigId", sessionInfo.getConfigId());
        assertEquals("https://localhost", sessionInfo.getIss());
        assertEquals("testSub", sessionInfo.getSub());
        assertEquals("testSid", sessionInfo.getSid());
        assertEquals("12345", sessionInfo.getTimestamp());
    }

    @Test
    public void test_getSessinoInfo_sidWasEmptyOrNull() throws OidcSessionException {
        String sessionId = "fb58fe21d28e8fa08aa606be6c027f13b9481d33442c4c25db455d40cee3d3a8a30c71128053e3fcafc1c31c963853570ea2a2d7fe51e55f5bd91220125684166ddde5c6dd51e2a0e907537ddafc01e7";
        setupRequestExpectations(sessionId);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, clientSecret);

        assertEquals("testConfigId", sessionInfo.getConfigId());
        assertEquals("https://localhost", sessionInfo.getIss());
        assertEquals("testSub", sessionInfo.getSub());
        assertEquals("", sessionInfo.getSid());
        assertEquals("12345", sessionInfo.getTimestamp());
    }

    @Test
    public void test_getSessionInfo_sessionIdIsNull() throws OidcSessionException {
        String sessionId = null;
        setupRequestExpectations(sessionId);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, clientSecret);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test
    public void test_getSessionInfo_sessionIdIsEmpty() throws OidcSessionException {
        String sessionId = "";
        setupRequestExpectations(sessionId);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, clientSecret);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test
    public void test_getSessionInfo_decodedSessionIdDoesNotHaveFourParts() throws OidcSessionException {
        String sessionId = "fb58fe21d28e8fa08aa606be6c027f139551aa16fb9bad37a89d9923ea7cb6c5";
        setupRequestExpectations(sessionId);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, clientSecret);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test(expected = OidcSessionException.class)
    public void test_getSessionInfo_sessionIdIsInvalid() throws OidcSessionException {
        String sessionId = "invalidSessionId";
        setupRequestExpectations(sessionId);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, clientSecret);
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
