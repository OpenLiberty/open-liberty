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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

public class InMemoryOidcSessionCacheTest extends CommonTestClass {

    private final String issuer = "https://localhost";
    private final String configId = "myOidcClientConfig";
    private final String clientSecret = "myClientSecret";

    private final ConvergedClientConfig clientConfig = mockery.mock(ConvergedClientConfig.class);

    OidcSessionInfo sessionEmptySid1;
    OidcSessionInfo sessionEmptySid2;
    OidcSessionInfo sessionNonEmptySid;
    OidcSessionInfo sessionDiffSub;

    private InMemoryOidcSessionCache cache;

    @Before
    public void before() {
        cache = new InMemoryOidcSessionCache();
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getClientSecret();
                will(returnValue(clientSecret));
            }
        });
        sessionEmptySid1 = new OidcSessionInfo(configId, issuer, "testsub", "", "1234", clientConfig);
        sessionEmptySid2 = new OidcSessionInfo(configId, issuer, "testsub", "", "2345", clientConfig);
        sessionNonEmptySid = new OidcSessionInfo(configId, issuer, "testsub", "testsid", "3456", clientConfig);
        sessionDiffSub = new OidcSessionInfo(configId, issuer, "testsub2", "testsid2", "4567", clientConfig);
    }

    @Test
    public void test_insert() {
        String sub = "testsub";
        String sid = "testsid";
        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, issuer, sub, sid, "1234", clientConfig);

        boolean inserted = cache.insertSession(sessionInfo);

        assertTrue("Session should have been inserted.", inserted);
    }

    @Test
    public void test_insert_sidIsNull() {
        String sub = "testsub";
        String sid = null;
        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, issuer, sub, sid, "1234", clientConfig);

        boolean inserted = cache.insertSession(sessionInfo);

        assertTrue("Session with no sid have been inserted.", inserted);
    }

    @Test
    public void test_insert_sidIsEmpty() {
        String sub = "testsub";
        String sid = "";
        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, issuer, sub, sid, "1234", clientConfig);

        boolean inserted = cache.insertSession(sessionInfo);

        assertTrue("Session with empty sid should have been inserted.", inserted);
    }

    @Test
    public void test_insert_subIsNull() {
        String sub = null;
        String sid = "testsid";
        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, issuer, sub, sid, "1234", clientConfig);

        boolean inserted = cache.insertSession(sessionInfo);

        assertFalse("Session with no sub should not have been inserted.", inserted);
    }

    @Test
    public void test_insert_subIsEmpty() {
        String sub = "";
        String sid = "testsid";
        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, issuer, sub, sid, "1234", clientConfig);

        boolean inserted = cache.insertSession(sessionInfo);

        assertFalse("Session with empty sub should not have been inserted.", inserted);
    }

    @Test
    public void test_insert_duplicateSid() {
        String sub = "testsub";
        String sid = "testsid";
        OidcSessionInfo sessionInfo1 = new OidcSessionInfo(configId, issuer, sub, sid, "1234", clientConfig);
        OidcSessionInfo sessionInfo2 = new OidcSessionInfo(configId, issuer, sub, sid, "2345", clientConfig);

        boolean inserted1 = cache.insertSession(sessionInfo1);
        boolean inserted2 = cache.insertSession(sessionInfo2);

        assertTrue("First session should have been inserted normally.", inserted1);
        assertFalse("Second session with duplicate sid should not have been.", inserted2);
    }

    @Test
    public void test_insert_duplicateNullSid() {
        String sub = "testsub";
        String sid = null;
        OidcSessionInfo sessionInfo1 = new OidcSessionInfo(configId, issuer, sub, sid, "1234", clientConfig);
        OidcSessionInfo sessionInfo2 = new OidcSessionInfo(configId, issuer, sub, sid, "2345", clientConfig);

        boolean inserted1 = cache.insertSession(sessionInfo1);
        boolean inserted2 = cache.insertSession(sessionInfo2);

        assertTrue("First session should have inserted normally.", inserted1);
        assertTrue("Second session with duplicate null sid should also have been inserted.", inserted2);
    }

    @Test
    public void test_insert_duplicateEmptySid() {
        String sub = "testsub";
        String sid = "";
        OidcSessionInfo sessionInfo1 = new OidcSessionInfo(configId, issuer, sub, sid, "1234", clientConfig);
        OidcSessionInfo sessionInfo2 = new OidcSessionInfo(configId, issuer, sub, sid, "2345", clientConfig);

        boolean inserted1 = cache.insertSession(sessionInfo1);
        boolean inserted2 = cache.insertSession(sessionInfo2);

        assertTrue("First session should have inserted normally.", inserted1);
        assertTrue("Second session with duplicate empty sid should also been inserted.", inserted2);
    }

    @Test
    public void test_invalidateSession() {
        populateCache();

        boolean invalidated = cache.invalidateSession("testsub", "testsid");

        assertTrue("Session should have been invalidated.", invalidated);
        verifyInvalidatedSessions(false, false, true, false);
    }

    @Test
    public void test_invalidateSession_sub2() {
        populateCache();

        boolean invalidated = cache.invalidateSession("testsub2", "testsid2");

        assertTrue("Session should have been invalidated.", invalidated);
        verifyInvalidatedSessions(false, false, false, true);
    }

    @Test
    public void test_invalidateSession_alreadyInvalidated() {
        populateCache();
        cache.invalidateSession("testsub", "testsid");

        boolean invalidated = cache.invalidateSession("testsub", "testsid");

        assertFalse("Should return false if the session has already been invalidated.", invalidated);
        verifyInvalidatedSessions(false, false, true, false);
    }

    @Test
    public void test_invalidateSession_sidIsNull() {
        populateCache();

        boolean invalidated = cache.invalidateSession("testsub", null);

        assertFalse("Sessions should not have been invalidated.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_invalidateSession_sidIsEmpty() {
        populateCache();

        boolean invalidated = cache.invalidateSession("testsub", "");

        assertFalse("Sessions should not have been invalidated.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_invalidateSession_sidDoesNotExist() {
        populateCache();

        boolean invalidated = cache.invalidateSession("testsub", "doesnotexist");

        assertFalse("Should not be able to invalidate a session for a sid that does not exist.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_invalidateSession_subIsNull() {
        populateCache();

        boolean invalidated = cache.invalidateSession(null, "testsid");

        assertFalse("Should not be able to invalidate a session if no sub is provided.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_invalidateSession_subIsEmpty() {
        populateCache();

        boolean invalidated = cache.invalidateSession("", "testsid");

        assertFalse("Should not be able to invalidate a session if the sub is empty.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_invalidateSession_subDoesNotExist() {
        populateCache();

        boolean invalidated = cache.invalidateSession("doesnotexist", "testsid");

        assertFalse("Should not be able to invalidate a session if the sub does not exist.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_invalidateSessionBySessionId() {
        populateCache();

        boolean invalidated = cache.invalidateSessionBySessionId("testsub", sessionEmptySid1.getSessionId());

        assertTrue("Session should have been invalidated.", invalidated);
        verifyInvalidatedSessions(true, false, false, false);
    }

    @Test
    public void test_invalidateSessionBySessionId_sub2() {
        populateCache();

        boolean invalidated = cache.invalidateSessionBySessionId("testsub2", sessionDiffSub.getSessionId());

        assertTrue("Session should have been invalidated.", invalidated);
        verifyInvalidatedSessions(false, false, false, true);
    }

    @Test
    public void test_invalidateSessionBySessionId_alreadyInvalidated() {
        populateCache();
        cache.invalidateSessionBySessionId("testsub", sessionEmptySid1.getSessionId());

        boolean invalidated = cache.invalidateSessionBySessionId("testsub", sessionEmptySid1.getSessionId());

        assertFalse("Should return false if the session has already been invalidated.", invalidated);
        verifyInvalidatedSessions(true, false, false, false);
    }

    @Test
    public void test_invalidateSessionBySessionId_sidIsNull() {
        populateCache();

        boolean invalidated = cache.invalidateSessionBySessionId("testsub", null);

        assertFalse("Sessions should not have been invalidated.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_invalidateSessionBySessionId_sidIsEmpty() {
        populateCache();

        boolean invalidated = cache.invalidateSessionBySessionId("testsub", "");

        assertFalse("Sessions should not have been invalidated.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_invalidateSessionBySessionId_sidDoesNotExist() {
        populateCache();

        boolean invalidated = cache.invalidateSessionBySessionId("testsub", "doesnotexist");

        assertFalse("Should not be able to invalidate a session for a sid that does not exist.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_invalidateSessionBySessionId_subIsNull() {
        populateCache();

        boolean invalidated = cache.invalidateSessionBySessionId(null, "testsid");

        assertFalse("Should not be able to invalidate a session if no sub is provided.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_invalidateSessionBySessionId_subIsEmpty() {
        populateCache();

        boolean invalidated = cache.invalidateSessionBySessionId("", "testsid");

        assertFalse("Should not be able to invalidate a session if the sub is empty.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_invalidateSessionBySessionId_subDoesNotExist() {
        populateCache();

        boolean invalidated = cache.invalidateSessionBySessionId("doesnotexist", "testsid");

        assertFalse("Should not be able to invalidate a session if the sub does not exist.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_invalidateSessions() {
        populateCache();

        boolean invalidated = cache.invalidateSessions("testsub");

        assertTrue("Should have been able to invalidate sessions.", invalidated);
        verifyInvalidatedSessions(true, true, true, false);
    }

    @Test
    public void test_invalidateSessions_sub2() {
        populateCache();

        boolean invalidated = cache.invalidateSessions("testsub2");

        assertTrue("Should have been able to invalidate sessions.", invalidated);
        verifyInvalidatedSessions(false, false, false, true);
    }

    @Test
    public void test_invalidateSessions_subIsNull() {
        populateCache();

        boolean invalidated = cache.invalidateSessions(null);

        assertFalse("Should not be able to invalidate sessions if no sub is provided.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_invalidateSessions_subIsEmpty() {
        populateCache();

        boolean invalidated = cache.invalidateSessions("");

        assertFalse("Should not be able to invalidate sessions if the sub is empty.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_invalidateSessions_subDoesNotExist() {
        populateCache();

        boolean invalidated = cache.invalidateSessions("doesnotexist");

        assertFalse("Should not be able to invalidate sessions if the sub does not exist.", invalidated);
        verifyInvalidatedSessions(false, false, false, false);
    }

    @Test
    public void test_removeInvalidatedSession() {
        String sub = "testsub";
        String sid = "testsid";
        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, issuer, sub, sid, "1234", clientConfig);

        cache.insertSession(sessionInfo);
        cache.invalidateSession(sub, sid);

        boolean removed = cache.removeInvalidatedSession(sessionInfo);

        assertTrue("Invalidated session should have been removed.", removed);
    }

    @Test
    public void test_removeInvalidatedSession_sessionIsNull() {
        String sub = "testsub";
        String sid = "testsid";
        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, issuer, sub, sid, "1234", clientConfig);

        cache.insertSession(sessionInfo);
        cache.invalidateSession(sub, sid);

        boolean removed = cache.removeInvalidatedSession(null);

        assertFalse("Should not be able to remove a session without providing the oidc session id.", removed);
    }

    @Test
    public void test_removeInvalidatedSession_sessionDoesNotExist() {
        String sub = "testsub";
        String sid = "testsid";
        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, issuer, sub, sid, "1234", clientConfig);

        cache.insertSession(sessionInfo);
        cache.invalidateSession(sub, sid);

        OidcSessionInfo doesnotexist = new OidcSessionInfo(configId, issuer, sub, sid, "9876", clientConfig);
        boolean removed = cache.removeInvalidatedSession(doesnotexist);

        assertFalse("Should not be able to remove a session whose oidc session id does not exist.", removed);
    }

    @Test
    public void test_isSessionInvalidated() {
        String sub = "testsub";
        String sid = "testsid";
        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, issuer, sub, sid, "1234", clientConfig);

        cache.insertSession(sessionInfo);
        cache.invalidateSession(sub, sid);

        boolean isInvalidated = cache.isSessionInvalidated(sessionInfo);

        assertTrue("Should have returned that the session is invalidated.", isInvalidated);
    }

    @Test
    public void test_isSessionInvalidated_sessionIsNull() {
        String sub = "testsub";
        String sid = "testsid";
        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, issuer, sub, sid, "1234", clientConfig);

        cache.insertSession(sessionInfo);
        cache.invalidateSession(sub, sid);

        boolean isInvalidated = cache.isSessionInvalidated(null);

        assertFalse("Should not be able to check if a session is invalidated if no oidc session id is provided.", isInvalidated);
    }

    @Test
    public void test_isSessionInvalidated_sessionHasNotBeenInvalidated() {
        String sub = "testsub";
        String sid = "testsid";
        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, issuer, sub, sid, "1234", clientConfig);

        cache.insertSession(sessionInfo);

        boolean isInvalidated = cache.isSessionInvalidated(sessionInfo);

        assertFalse("Should have returned that the session is not invalid.", isInvalidated);
    }

    private void populateCache() {
        cache.insertSession(sessionEmptySid1);
        cache.insertSession(sessionEmptySid2);
        cache.insertSession(sessionNonEmptySid);
        cache.insertSession(sessionDiffSub);
    }

    private void verifyInvalidatedSessions(boolean invalidatedEmptySid1, boolean invalidatedEmptySid2, boolean invalidatedNonEmptySid, boolean invalidatedDiffSub) {
        assertEquals(getAssertEqualsMessage(sessionEmptySid1.getSessionId(), invalidatedEmptySid1), invalidatedEmptySid1, cache.isSessionInvalidated(sessionEmptySid1));
        assertEquals(getAssertEqualsMessage(sessionEmptySid2.getSessionId(), invalidatedEmptySid2), invalidatedEmptySid2, cache.isSessionInvalidated(sessionEmptySid2));
        assertEquals(getAssertEqualsMessage(sessionNonEmptySid.getSessionId(), invalidatedNonEmptySid), invalidatedNonEmptySid, cache.isSessionInvalidated(sessionNonEmptySid));
        assertEquals(getAssertEqualsMessage(sessionDiffSub.getSessionId(), invalidatedDiffSub), invalidatedDiffSub, cache.isSessionInvalidated(sessionDiffSub));
    }

    private String getAssertEqualsMessage(String oidcSessionId, boolean invalidated) {
        return oidcSessionId + " should " + (invalidated ? "" : "not ") + "be invalidated.";
    }

}
