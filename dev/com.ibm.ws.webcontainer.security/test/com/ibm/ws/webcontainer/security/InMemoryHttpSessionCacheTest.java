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
package com.ibm.ws.webcontainer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class InMemoryHttpSessionCacheTest {

    private InMemoryHttpSessionCache cache;

    @Before
    public void setup() throws Exception {
        cache = new InMemoryHttpSessionCache();
    }

    @Test
    public void test_insert() {
        String sub = "testsub";
        String sid = "testsid";
        String httpSessionId = "testhttpsessionid";

        cache.insertSession(sub, sid, httpSessionId);

        assertTrue("Session should be active.", cache.isSessionActive(httpSessionId));
    }

    @Test
    public void test_insert_sidIsNull() {
        String sub = "testsub";
        String sid = null;
        String httpSessionId = "testhttpsessionid";

        cache.insertSession(sub, sid, httpSessionId);

        assertTrue("Session with no sid should be active.", cache.isSessionActive(httpSessionId));
    }

    @Test
    public void test_insert_sidIsEmpty() {
        String sub = "testsub";
        String sid = "";
        String httpSessionId = "testhttpsessionid";

        cache.insertSession(sub, sid, httpSessionId);

        assertTrue("Session with empty sid should be active.", cache.isSessionActive(httpSessionId));
    }

    @Test
    public void test_insert_subIsNull() {
        String sub = null;
        String sid = "testsid";
        String httpSessionId = "testhttpsessionid";

        cache.insertSession(sub, sid, httpSessionId);

        assertFalse("Session with no sub should be inactive.", cache.isSessionActive(httpSessionId));
    }

    @Test
    public void test_insert_subIsEmpty() {
        String sub = "";
        String sid = "testsid";
        String httpSessionId = "testhttpsessionid";

        cache.insertSession(sub, sid, httpSessionId);

        assertFalse("Session with empty sub should be inactive.", cache.isSessionActive(httpSessionId));
    }

    @Test
    public void test_insert_duplicateSid() {
        String sub = "testsub";
        String sid = "testsid";
        String httpSessionId1 = "testhttpsessionid1";
        String httpSessionId2 = "testhttpsessionid2";

        cache.insertSession(sub, sid, httpSessionId1);
        cache.insertSession(sub, sid, httpSessionId2);

        assertTrue("First session should have inserted normally.", cache.isSessionActive(httpSessionId1));
        assertFalse("Second session with duplicate sid should be inactive.", cache.isSessionActive(httpSessionId2));
    }

    @Test
    public void test_insert_duplicateNullSid() {
        String sub = "testsub";
        String sid = null;
        String httpSessionId1 = "testhttpsessionid1";
        String httpSessionId2 = "testhttpsessionid2";

        cache.insertSession(sub, sid, httpSessionId1);
        cache.insertSession(sub, sid, httpSessionId2);

        assertTrue("First session should have inserted normally.", cache.isSessionActive(httpSessionId1));
        assertTrue("Second session with duplicate null sid should also be active.", cache.isSessionActive(httpSessionId2));
    }

    @Test
    public void test_insert_duplicateEmptySid() {
        String sub = "testsub";
        String sid = "";
        String httpSessionId1 = "testhttpsessionid1";
        String httpSessionId2 = "testhttpsessionid2";

        cache.insertSession(sub, sid, httpSessionId1);
        cache.insertSession(sub, sid, httpSessionId2);

        assertTrue("First session should have inserted normally.", cache.isSessionActive(httpSessionId1));
        assertTrue("Second session with duplicate empty sid should also be active.", cache.isSessionActive(httpSessionId2));
    }

    @Test
    public void test_remove() {
        populateCache();

        cache.removeSession("testsub", "testsid");

        verifySessionCache(true, true, false, true);
    }

    @Test
    public void test_remove_sidIsNull() {
        populateCache();

        cache.removeSession("testsub", null);

        verifySessionCache(false, false, false, true);
    }

    @Test
    public void test_remove_sidIsEmpty() {
        populateCache();

        cache.removeSession("testsub", "");

        verifySessionCache(false, false, false, true);
    }

    @Test
    public void test_remove_subIsNull() {
        populateCache();

        cache.removeSession(null, "testhttpsessionid3");

        verifySessionCache(true, true, true, true);
    }

    @Test
    public void test_remove_subIsEmpty() {
        populateCache();

        cache.removeSession("", "testhttpsessionid3");

        verifySessionCache(true, true, true, true);
    }

    @Test
    public void test_remove_sessionNotExisting() {
        populateCache();

        cache.removeSession("testsub", "doesnotexist");

        verifySessionCache(true, true, true, true);
    }

    @Test
    public void test_isActive_sessionNotExisting() {
        populateCache();

        assertFalse("Non-existing session should not be active.", cache.isSessionActive("doesnotexist"));
    }

    private void populateCache() {
        cache.insertSession("testsub", "", "testhttpsessionid1");
        cache.insertSession("testsub", "", "testhttpsessionid2");
        cache.insertSession("testsub", "testsid", "testhttpsessionid3");
        cache.insertSession("testsub2", "testsid2", "testhttpsessionid4");
    }

    private void verifySessionCache(boolean active1, boolean active2, boolean active3, boolean active4) {
        assertEquals(getAssertEqualsMessage(active1), active1, cache.isSessionActive("testhttpsessionid1"));
        assertEquals(getAssertEqualsMessage(active2), active2, cache.isSessionActive("testhttpsessionid2"));
        assertEquals(getAssertEqualsMessage(active3), active3, cache.isSessionActive("testhttpsessionid3"));
        assertEquals(getAssertEqualsMessage(active4), active4, cache.isSessionActive("testhttpsessionid4"));
    }

    private String getAssertEqualsMessage(boolean active) {
        return "Session should be " + (active ? "active" : "inactive") + ".";
    }

}
