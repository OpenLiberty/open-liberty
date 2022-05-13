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
public class OidcSessionHelperTest {

    @Test
    public void test_createSessionId() {
        String configId = "testConfigId";
        String sub = "testSub";
        String sid = "testSid";
        String timestamp = "12345";

        String actualSessionId = OidcSessionHelper.createSessionId(configId, sub, sid, timestamp);
        String expectedSesssionId = "dGVzdENvbmZpZ0lkLHRlc3RTdWIsdGVzdFNpZCwxMjM0NQ==";

        assertEquals("Should have concatenated the parts using ',' and then base64 encoding the result.", expectedSesssionId, actualSessionId);
    }

    @Test
    public void test_createSessionId_emptySid() {
        String configId = "testConfigId";
        String sub = "testSub";
        String sid = "";
        String timestamp = "12345";

        String actualSessionId = OidcSessionHelper.createSessionId(configId, sub, sid, timestamp);
        String expectedSesssionId = "dGVzdENvbmZpZ0lkLHRlc3RTdWIsLDEyMzQ1";

        assertEquals("Should have concatenated the parts using ',' and then base64 encoding the result.", expectedSesssionId, actualSessionId);
    }

    @Test
    public void test_getSessionInfo() {
        String sessionId = "dGVzdENvbmZpZ0lkLHRlc3RTdWIsdGVzdFNpZCwxMjM0NQ==";

        OidcSessionInfo sessionInfo = OidcSessionHelper.getSessionInfo(sessionId);

        assertEquals("testConfigId", sessionInfo.getConfigId());
        assertEquals("testSub", sessionInfo.getSub());
        assertEquals("testSid", sessionInfo.getSid());
        assertEquals("12345", sessionInfo.getTimestamp());
    }

    @Test
    public void test_getSessinoInfo_sidWasEmpty() {
        String sessionId = "dGVzdENvbmZpZ0lkLHRlc3RTdWIsLDEyMzQ1";

        OidcSessionInfo sessionInfo = OidcSessionHelper.getSessionInfo(sessionId);

        assertEquals("testConfigId", sessionInfo.getConfigId());
        assertEquals("testSub", sessionInfo.getSub());
        assertEquals("", sessionInfo.getSid());
        assertEquals("12345", sessionInfo.getTimestamp());
    }

    @Test
    public void test_getSessionInfo_sessionIdIsNull() {
        String sessionId = null;

        OidcSessionInfo sessionInfo = OidcSessionHelper.getSessionInfo(sessionId);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test
    public void test_getSessionInfo_sessionIdIsEmpty() {
        String sessionId = "";

        OidcSessionInfo sessionInfo = OidcSessionHelper.getSessionInfo(sessionId);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test
    public void test_getSessionInfo_decodedSessionIdDoesNotHaveFourParts() {
        String sessionId = "dGVzdENvbmZpZ0lkLHRlc3RTdWI=";

        OidcSessionInfo sessionInfo = OidcSessionHelper.getSessionInfo(sessionId);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }

    @Test
    public void test_getSessionInfo_sessionIdIsInvalid() {
        String sessionId = "invalidSessionId";

        OidcSessionInfo sessionInfo = OidcSessionHelper.getSessionInfo(sessionId);

        assertNull("Expected the sessionInfo to be null, since the sessionId was invalid.", sessionInfo);
    }
}
