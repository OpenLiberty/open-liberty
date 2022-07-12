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

    private final static String CLIENT_SECRET = "abcd";

    private final static String CONFIG_ID = "testConfigId";
    private final static String ISS = "https://localhost";
    private final static String SUB = "testSub";
    private final static String SID = "testSid";
    private final static String TIMESTAMP = "12345";

    // session id's are in the format 'AES(Base64(CONFIG_ID):Base64(ISS):Base64(SUB):Base64(SID):Base64(TIMESTAMP), CLIENT_SECRET)'
    private final static String SESSION_ID_NORMAL = "fb58fe21d28e8fa08aa606be6c027f13b9481d33442c4c25db455d40cee3d3a8a30c71128053e3fcafc1c31c96385357456bc4603869d2fe7485ac6b73ca843ac998f6d4b23afb80e33160fc75adcceb";
    private final static String SESSION_ID_CONFIG_ID_IS_EMPTY = "d5431bab1c592377f590457f98aa4f0345ebbf825bdc146513c0547f037be12c8a3a29855ef820d4b289e0f558630adff3fdb9124707d4deb50a0a98005b2c09";
    private final static String SESSION_ID_ISS_IS_EMPTY = "fb58fe21d28e8fa08aa606be6c027f13f871f4a697537c27dc4e1d1dedac805e70ab31dc4403fd0956838bc5c5a59e18f5be95c6fb3e7f698210c4029837ab78";
    private final static String SESSION_ID_SUB_IS_EMPTY = "fb58fe21d28e8fa08aa606be6c027f13b9481d33442c4c25db455d40cee3d3a8af74c02af7415304095b5d19c639e3e98e9aa91681bc342457203503d25dd0628f31abfc187b1fe0e87d0f09511f94f7";
    private final static String SESSION_ID_SID_IS_EMPTY = "fb58fe21d28e8fa08aa606be6c027f13b9481d33442c4c25db455d40cee3d3a8a30c71128053e3fcafc1c31c963853570ea2a2d7fe51e55f5bd91220125684166ddde5c6dd51e2a0e907537ddafc01e7";
    private final static String SESSION_ID_TIMESTAMP_IS_EMPTY = "fb58fe21d28e8fa08aa606be6c027f13b9481d33442c4c25db455d40cee3d3a8a30c71128053e3fcafc1c31c96385357456bc4603869d2fe7485ac6b73ca843ab2e2b9fa523588d5dab3ec577b67f63f";
    private final static String SESSION_ID_CLIENT_SECRET_IS_NULL = "eebf15647ba0100fd2cca2f4347de44f9269ccb4aa8e7dd30bd1f9359aef8cc46f77d7e580e533900a236f598ad14e2828ed8e081d3c893bd666373d7df38bcba7aa31eed3b51524e93c911e4351f592";
    private final static String SESSION_ID_DOES_NOT_HAVE_FIVE_PARTS = "fb58fe21d28e8fa08aa606be6c027f139551aa16fb9bad37a89d9923ea7cb6c5";
    private final static String SESSION_ID_INVALID = "invalidSessionId";

    @Test
    public void test_createSessionId() throws OidcSessionException {
        OidcSessionInfo sessionInfo = new OidcSessionInfo(CONFIG_ID, ISS, SUB, SID, TIMESTAMP, CLIENT_SECRET);

        assertEquals("Expected to the return the session id in the format 'AES(Base64(CONFIG_ID):Base64(ISS):Base64(SUB):Base64(SID):Base64(TIMESTAMP), CLIENT_SECRET)'.", SESSION_ID_NORMAL, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_configIdIsEmpty() throws OidcSessionException {
        OidcSessionInfo sessionInfo = new OidcSessionInfo("", ISS, SUB, SID, TIMESTAMP, CLIENT_SECRET);

        assertEquals("Expected to the return the session id in the format 'AES(:Base64(ISS):Base64(SUB):Base64(SID):Base64(TIMESTAMP), CLIENT_SECRET)'.", SESSION_ID_CONFIG_ID_IS_EMPTY, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_issIsEmpty() throws OidcSessionException {
        OidcSessionInfo sessionInfo = new OidcSessionInfo(CONFIG_ID, "", SUB, SID, TIMESTAMP, CLIENT_SECRET);

        assertEquals("Expected to the return the session id in the format 'AES(Base64(CONFIG_ID)::Base64(SUB):Base64(SID):Base64(TIMESTAMP), CLIENT_SECRET)'.", SESSION_ID_ISS_IS_EMPTY, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_subIsEmpty() throws OidcSessionException {
        OidcSessionInfo sessionInfo = new OidcSessionInfo(CONFIG_ID, ISS, "", SID, TIMESTAMP, CLIENT_SECRET);

        assertEquals("Expected to the return the session id in the format 'AES(Base64(CONFIG_ID):Base64(ISS)::Base64(SID):Base64(TIMESTAMP), CLIENT_SECRET)'.", SESSION_ID_SUB_IS_EMPTY, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_sidIsEmpty() throws OidcSessionException {
        OidcSessionInfo sessionInfo = new OidcSessionInfo(CONFIG_ID, ISS, SUB, "", TIMESTAMP, CLIENT_SECRET);

        assertEquals("Expected to the return the session id in the format 'AES(Base64(CONFIG_ID):Base64(ISS):Base64(SUB)::Base64(TIMESTAMP), CLIENT_SECRET)'.", SESSION_ID_SID_IS_EMPTY, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_timestampIsEmpty() throws OidcSessionException {
        OidcSessionInfo sessionInfo = new OidcSessionInfo(CONFIG_ID, ISS, SUB, SID, "", CLIENT_SECRET);

        assertEquals("Expected to the return the session id in the format 'AES(Base64(CONFIG_ID):Base64(ISS):Base64(SUB):Base64(SID):, CLIENT_SECRET)'.", SESSION_ID_TIMESTAMP_IS_EMPTY, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_configIdIsNull() throws OidcSessionException {
        OidcSessionInfo sessionInfo = new OidcSessionInfo(null, ISS, SUB, SID, TIMESTAMP, CLIENT_SECRET);

        assertEquals("Expected to replace the configId with an empty string.", SESSION_ID_CONFIG_ID_IS_EMPTY, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_issIsNull() throws OidcSessionException {
        OidcSessionInfo sessionInfo = new OidcSessionInfo(CONFIG_ID, null, SUB, SID, TIMESTAMP, CLIENT_SECRET);

        assertEquals("Expected to replace the iss with an empty string.", SESSION_ID_ISS_IS_EMPTY, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_subIsNull() throws OidcSessionException {
        OidcSessionInfo sessionInfo = new OidcSessionInfo(CONFIG_ID, ISS, null, SID, TIMESTAMP, CLIENT_SECRET);

        assertEquals("Expected to replace the sub with an empty string.", SESSION_ID_SUB_IS_EMPTY, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_sidIsNull() throws OidcSessionException {
        OidcSessionInfo sessionInfo = new OidcSessionInfo(CONFIG_ID, ISS, SUB, null, TIMESTAMP, CLIENT_SECRET);

        assertEquals("Expected to replace the sid with an empty string.", SESSION_ID_SID_IS_EMPTY, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_timestampIsNull() throws OidcSessionException {
        OidcSessionInfo sessionInfo = new OidcSessionInfo(CONFIG_ID, ISS, SUB, SID, null, CLIENT_SECRET);

        assertEquals("Expected to replace the timestamp with an empty string.", SESSION_ID_TIMESTAMP_IS_EMPTY, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_clientSecretIsNull() throws OidcSessionException {
        OidcSessionInfo sessionInfo = new OidcSessionInfo(CONFIG_ID, ISS, SUB, SID, TIMESTAMP, null);

        assertEquals("Expected a default secret to be used.", SESSION_ID_CLIENT_SECRET_IS_NULL, sessionInfo.getSessionId());
    }

    @Test
    public void test_getSessionInfo() throws OidcSessionException {
        setupRequestExpectations(SESSION_ID_NORMAL);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, CLIENT_SECRET);

        assertEquals(CONFIG_ID, sessionInfo.getConfigId());
        assertEquals(ISS, sessionInfo.getIss());
        assertEquals(SUB, sessionInfo.getSub());
        assertEquals(SID, sessionInfo.getSid());
        assertEquals(TIMESTAMP, sessionInfo.getTimestamp());
    }

    @Test
    public void test_getSessionInfo_configIdWasEmptyOrNull() throws OidcSessionException {
        setupRequestExpectations(SESSION_ID_CONFIG_ID_IS_EMPTY);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, CLIENT_SECRET);

        assertEquals("", sessionInfo.getConfigId());
        assertEquals(ISS, sessionInfo.getIss());
        assertEquals(SUB, sessionInfo.getSub());
        assertEquals(SID, sessionInfo.getSid());
        assertEquals(TIMESTAMP, sessionInfo.getTimestamp());
    }

    @Test
    public void test_getSessionInfo_issWasEmptyOrNull() throws OidcSessionException {
        setupRequestExpectations(SESSION_ID_ISS_IS_EMPTY);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, CLIENT_SECRET);

        assertEquals(CONFIG_ID, sessionInfo.getConfigId());
        assertEquals("", sessionInfo.getIss());
        assertEquals(SUB, sessionInfo.getSub());
        assertEquals(SID, sessionInfo.getSid());
        assertEquals(TIMESTAMP, sessionInfo.getTimestamp());
    }

    @Test
    public void test_getSessionInfo_subWasEmptyOrNull() throws OidcSessionException {
        setupRequestExpectations(SESSION_ID_SUB_IS_EMPTY);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, CLIENT_SECRET);

        assertEquals(CONFIG_ID, sessionInfo.getConfigId());
        assertEquals(ISS, sessionInfo.getIss());
        assertEquals("", sessionInfo.getSub());
        assertEquals(SID, sessionInfo.getSid());
        assertEquals(TIMESTAMP, sessionInfo.getTimestamp());
    }

    @Test
    public void test_getSessionInfo_sidWasEmptyOrNull() throws OidcSessionException {
        setupRequestExpectations(SESSION_ID_SID_IS_EMPTY);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, CLIENT_SECRET);

        assertEquals(CONFIG_ID, sessionInfo.getConfigId());
        assertEquals(ISS, sessionInfo.getIss());
        assertEquals(SUB, sessionInfo.getSub());
        assertEquals("", sessionInfo.getSid());
        assertEquals(TIMESTAMP, sessionInfo.getTimestamp());
    }

    @Test
    public void test_getSessionInfo_timestampWasEmptyOrNull() throws OidcSessionException {
        setupRequestExpectations(SESSION_ID_TIMESTAMP_IS_EMPTY);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, CLIENT_SECRET);

        assertEquals(CONFIG_ID, sessionInfo.getConfigId());
        assertEquals(ISS, sessionInfo.getIss());
        assertEquals(SUB, sessionInfo.getSub());
        assertEquals(SID, sessionInfo.getSid());
        assertEquals("", sessionInfo.getTimestamp());
    }

    @Test
    public void test_getSessionInfo_sessionIdIsNull() throws OidcSessionException {
        setupRequestExpectations(null);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, CLIENT_SECRET);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test
    public void test_getSessionInfo_sessionIdIsEmpty() throws OidcSessionException {
        setupRequestExpectations("");

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, CLIENT_SECRET);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test
    public void test_getSessionInfo_decodedSessionIdDoesNotHaveFiveParts() throws OidcSessionException {
        setupRequestExpectations(SESSION_ID_DOES_NOT_HAVE_FIVE_PARTS);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, CLIENT_SECRET);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test(expected = OidcSessionException.class)
    public void test_getSessionInfo_sessionIdIsInvalid() throws OidcSessionException {
        setupRequestExpectations(SESSION_ID_INVALID);

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, CLIENT_SECRET);
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
