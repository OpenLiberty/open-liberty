/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class InMemoryOidcSessionCacheTest {

    private InMemoryOidcSessionCache cache;

    @Before
    public void setup() throws Exception {
        cache = new InMemoryOidcSessionCache();
    }

    @Test
    public void test_insert() {
        String sub = "testsub";
        String sid = "testsid";
        String oidcSessionId = "testoidcsessionid";

        boolean inserted = cache.insertSession(sub, sid, oidcSessionId);

        assertTrue("Session should have been inserted.", inserted);
    }

    @Test
    public void test_insert_sidIsNull() {
        String sub = "testsub";
        String sid = null;
        String oidcSessionId = "testoidcsessionid";

        boolean inserted = cache.insertSession(sub, sid, oidcSessionId);

        assertTrue("Session with no sid have been inserted.", inserted);
    }

    @Test
    public void test_insert_sidIsEmpty() {
        String sub = "testsub";
        String sid = "";
        String oidcSessionId = "testoidcsessionid";

        boolean inserted = cache.insertSession(sub, sid, oidcSessionId);

        assertTrue("Session with empty sid should have been inserted.", inserted);
    }

    @Test
    public void test_insert_subIsNull() {
        String sub = null;
        String sid = "testsid";
        String oidcSessionId = "testoidcsessionid";

        boolean inserted = cache.insertSession(sub, sid, oidcSessionId);

        assertFalse("Session with no sub should not have been inserted.", inserted);
    }

    @Test
    public void test_insert_subIsEmpty() {
        String sub = "";
        String sid = "testsid";
        String oidcSessionId = "testoidcsessionid";

        boolean inserted = cache.insertSession(sub, sid, oidcSessionId);

        assertFalse("Session with empty sub should not have been inserted.", inserted);
    }

    @Test
    public void test_insert_duplicateSid() {
        String sub = "testsub";
        String sid = "testsid";
        String oidcSessionId1 = "testoidcsessionid1";
        String oidcSessionId2 = "testoidcsessionid2";

        boolean inserted1 = cache.insertSession(sub, sid, oidcSessionId1);
        boolean inserted2 = cache.insertSession(sub, sid, oidcSessionId2);

        assertTrue("First session should have been inserted normally.", inserted1);
        assertFalse("Second session with duplicate sid should not have been.", inserted2);
    }

    @Test
    public void test_insert_duplicateNullSid() {
        String sub = "testsub";
        String sid = null;
        String oidcSessionId1 = "testoidcsessionid1";
        String oidcSessionId2 = "testoidcsessionid2";

        boolean inserted1 = cache.insertSession(sub, sid, oidcSessionId1);
        boolean inserted2 = cache.insertSession(sub, sid, oidcSessionId2);

        assertTrue("First session should have inserted normally.", inserted1);
        assertTrue("Second session with duplicate null sid should also have been inserted.", inserted2);
    }

    @Test
    public void test_insert_duplicateEmptySid() {
        String sub = "testsub";
        String sid = "";
        String oidcSessionId1 = "testoidcsessionid1";
        String oidcSessionId2 = "testoidcsessionid2";

        boolean inserted1 = cache.insertSession(sub, sid, oidcSessionId1);
        boolean inserted2 = cache.insertSession(sub, sid, oidcSessionId2);

        assertTrue("First session should have inserted normally.", inserted1);
        assertTrue("Second session with duplicate empty sid should also been inserted.", inserted2);
    }

    @Test
    public void test_insert_oidcSessionIdIsNull() {
        String sub = "testsub";
        String sid = "testsid";
        String oidcSessionId = null;

        boolean inserted = cache.insertSession(sub, sid, oidcSessionId);

        assertFalse("Session with no oidc session id should not have been inserted.", inserted);
    }

    @Test
    public void test_insert_oidcSessionIdIsEmpty() {
        String sub = "testsub";
        String sid = "testsid";
        String oidcSessionId = "";

        boolean inserted = cache.insertSession(sub, sid, oidcSessionId);

        assertFalse("Session with empty oidc session id should not have been inserted.", inserted);
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

        boolean invalidated = cache.invalidateSessionBySessionId("testsub", "testoidcsessionid1");

        assertTrue("Session should have been invalidated.", invalidated);
        verifyInvalidatedSessions(true, false, false, false);
    }

    @Test
    public void test_invalidateSessionBySessionId_sub2() {
        populateCache();

        boolean invalidated = cache.invalidateSessionBySessionId("testsub2", "testoidcsessionid4");

        assertTrue("Session should have been invalidated.", invalidated);
        verifyInvalidatedSessions(false, false, false, true);
    }

    @Test
    public void test_invalidateSessionBySessionId_alreadyInvalidated() {
        populateCache();
        cache.invalidateSessionBySessionId("testsub", "testoidcsessionid1");

        boolean invalidated = cache.invalidateSessionBySessionId("testsub", "testoidcsessionid1");

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
        String oidcSessionId = "testoidcsessionid";
        cache.insertSession(sub, sid, oidcSessionId);
        cache.invalidateSession(sub, sid);

        boolean removed = cache.removeInvalidatedSession(oidcSessionId);

        assertTrue("Invalidated session should have been removed.", removed);
    }

    @Test
    public void test_removeInvalidatedSession_sessionIsNull() {
        String sub = "testsub";
        String sid = "testsid";
        String oidcSessionId = "testoidcsessionid";
        cache.insertSession(sub, sid, oidcSessionId);
        cache.invalidateSession(sub, sid);

        boolean removed = cache.removeInvalidatedSession(null);

        assertFalse("Should not be able to remove a session without providing the oidc session id.", removed);
    }

    @Test
    public void test_removeInvalidatedSession_sessionIsEmpty() {
        String sub = "testsub";
        String sid = "testsid";
        String oidcSessionId = "testoidcsessionid";
        cache.insertSession(sub, sid, oidcSessionId);
        cache.invalidateSession(sub, sid);

        boolean removed = cache.removeInvalidatedSession("");

        assertFalse("Should not be able to remove a session with an empty oidc session id.", removed);
    }

    @Test
    public void test_removeInvalidatedSession_sessionDoesNotExist() {
        String sub = "testsub";
        String sid = "testsid";
        String oidcSessionId = "testoidcsessionid";
        cache.insertSession(sub, sid, oidcSessionId);
        cache.invalidateSession(sub, sid);

        boolean removed = cache.removeInvalidatedSession("doesnotexist");

        assertFalse("Should not be able to remove a session whose oidc session id does not exist.", removed);
    }

    @Test
    public void test_isSessionInvalidated() {
        String sub = "testsub";
        String sid = "testsid";
        String oidcSessionId = "testoidcsessionid";
        cache.insertSession(sub, sid, oidcSessionId);
        cache.invalidateSession(sub, sid);

        boolean isInvalidated = cache.isSessionInvalidated(oidcSessionId);

        assertTrue("Should have returned that the session is invalidated.", isInvalidated);
    }

    @Test
    public void test_isSessionInvalidated_sessionIsNull() {
        String sub = "testsub";
        String sid = "testsid";
        String oidcSessionId = "testoidcsessionid";
        cache.insertSession(sub, sid, oidcSessionId);
        cache.invalidateSession(sub, sid);

        boolean isInvalidated = cache.isSessionInvalidated(null);

        assertFalse("Should not be able to check if a session is invalidated if no oidc session id is provided.", isInvalidated);
    }

    @Test
    public void test_isSessionInvalidated_sessionIsEmpty() {
        String sub = "testsub";
        String sid = "testsid";
        String oidcSessionId = "testoidcsessionid";
        cache.insertSession(sub, sid, oidcSessionId);
        cache.invalidateSession(sub, sid);

        boolean isInvalidated = cache.isSessionInvalidated("");

        assertFalse("Should not be able to check if a session is invalidated if the oidc session id is empty.", isInvalidated);
    }

    @Test
    public void test_isSessionInvalidated_sessionHasNotBeenInvalidated() {
        String sub = "testsub";
        String sid = "testsid";
        String oidcSessionId = "testoidcsessionid";
        cache.insertSession(sub, sid, oidcSessionId);

        boolean isInvalidated = cache.isSessionInvalidated(oidcSessionId);

        assertFalse("Should have returned that the session is not invalid.", isInvalidated);
    }

    private void populateCache() {
        cache.insertSession("testsub", "", "testoidcsessionid1");
        cache.insertSession("testsub", "", "testoidcsessionid2");
        cache.insertSession("testsub", "testsid", "testoidcsessionid3");
        cache.insertSession("testsub2", "testsid2", "testoidcsessionid4");
    }

    private void verifyInvalidatedSessions(boolean invalidated1, boolean invalidated2, boolean invalidated3, boolean invalidated4) {
        assertEquals(getAssertEqualsMessage("testoidcsessionid1", invalidated1), invalidated1, cache.isSessionInvalidated("testoidcsessionid1"));
        assertEquals(getAssertEqualsMessage("testoidcsessionid2", invalidated2), invalidated2, cache.isSessionInvalidated("testoidcsessionid2"));
        assertEquals(getAssertEqualsMessage("testoidcsessionid3", invalidated3), invalidated3, cache.isSessionInvalidated("testoidcsessionid3"));
        assertEquals(getAssertEqualsMessage("testoidcsessionid4", invalidated4), invalidated4, cache.isSessionInvalidated("testoidcsessionid4"));
    }

    private String getAssertEqualsMessage(String oidcSessionId, boolean invalidated) {
        return oidcSessionId + " should " + (invalidated ? "" : "not ") + "be invalidated.";
    }

}
