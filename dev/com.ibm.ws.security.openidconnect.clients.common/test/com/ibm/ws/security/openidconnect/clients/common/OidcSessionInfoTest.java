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

import org.junit.Test;

/**
 *
 */
public class OidcSessionInfoTest {

    @Test
    public void test_createSessionId() {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = "testSid";
        String timestamp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, timestamp);
        String expectedSesssionId = "dGVzdENvbmZpZ0lk:aHR0cHM6Ly9sb2NhbGhvc3Q=:dGVzdFN1Yg==:dGVzdFNpZA==:MTIzNDU=";

        assertEquals("Expected to the return the session id in the format 'Base64(configId):Base64(sub):Base64(sid):Base64(timestamp)'.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_sidIsEmpty() {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = "";
        String timestamp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, timestamp);
        String expectedSesssionId = "dGVzdENvbmZpZ0lk:aHR0cHM6Ly9sb2NhbGhvc3Q=:dGVzdFN1Yg==::MTIzNDU=";

        assertEquals("Expected to the return the session id in the format 'Base64(configId):Base64(sub):Base64(sid):Base64(timestamp)'.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_configIdIsNull() {
        String configId = null;
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = "testSid";
        String timestamp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, timestamp);
        String expectedSesssionId = ":aHR0cHM6Ly9sb2NhbGhvc3Q=:dGVzdFN1Yg==:dGVzdFNpZA==:MTIzNDU=";

        assertEquals("Expected to replace the configId with an empty string.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_issIsNull() {
        String configId = "testConfigId";
        String iss = null;
        String sub = "testSub";
        String sid = "testSid";
        String timestamp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, timestamp);
        String expectedSesssionId = "dGVzdENvbmZpZ0lk::dGVzdFN1Yg==:dGVzdFNpZA==:MTIzNDU=";

        assertEquals("Expected to replace the sub with an empty string.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_subIsNull() {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = null;
        String sid = "testSid";
        String timestamp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, timestamp);
        String expectedSesssionId = "dGVzdENvbmZpZ0lk:aHR0cHM6Ly9sb2NhbGhvc3Q=::dGVzdFNpZA==:MTIzNDU=";

        assertEquals("Expected to replace the sub with an empty string.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_sidIsNull() {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = null;
        String timestamp = "12345";

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, timestamp);
        String expectedSesssionId = "dGVzdENvbmZpZ0lk:aHR0cHM6Ly9sb2NhbGhvc3Q=:dGVzdFN1Yg==::MTIzNDU=";

        assertEquals("Expected to replace the sid with an empty string.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_createSessionId_timestampIsNull() {
        String configId = "testConfigId";
        String iss = "https://localhost";
        String sub = "testSub";
        String sid = "testSid";
        String timestamp = null;

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, timestamp);
        String expectedSesssionId = "dGVzdENvbmZpZ0lk:aHR0cHM6Ly9sb2NhbGhvc3Q=:dGVzdFN1Yg==:dGVzdFNpZA==:";

        assertEquals("Expected to replace the timestamp with an empty string.", expectedSesssionId, sessionInfo.getSessionId());
    }

    @Test
    public void test_getSessionInfo() {
        String sessionId = "dGVzdENvbmZpZ0lk:aHR0cHM6Ly9sb2NhbGhvc3Q=:dGVzdFN1Yg==:dGVzdFNpZA==:MTIzNDU=";

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(sessionId);

        assertEquals("testConfigId", sessionInfo.getConfigId());
        assertEquals("https://localhost", sessionInfo.getIss());
        assertEquals("testSub", sessionInfo.getSub());
        assertEquals("testSid", sessionInfo.getSid());
        assertEquals("12345", sessionInfo.getTimestamp());
    }

    @Test
    public void test_getSessinoInfo_sidWasEmptyOrNull() {
        String sessionId = "dGVzdENvbmZpZ0lk:aHR0cHM6Ly9sb2NhbGhvc3Q=:dGVzdFN1Yg==::MTIzNDU=";

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(sessionId);

        assertEquals("testConfigId", sessionInfo.getConfigId());
        assertEquals("https://localhost", sessionInfo.getIss());
        assertEquals("testSub", sessionInfo.getSub());
        assertEquals("", sessionInfo.getSid());
        assertEquals("12345", sessionInfo.getTimestamp());
    }

    @Test
    public void test_getSessionInfo_sessionIdIsNull() {
        String sessionId = null;

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(sessionId);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test
    public void test_getSessionInfo_sessionIdIsEmpty() {
        String sessionId = "";

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(sessionId);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test
    public void test_getSessionInfo_decodedSessionIdDoesNotHaveFourParts() {
        String sessionId = "dGVzdENvbmZpZ0lk:dGVzdFN1Yg==";

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(sessionId);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test
    public void test_getSessionInfo_sessionIdIsInvalid() {
        String sessionId = "invalidSessionId";

        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(sessionId);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }
}
