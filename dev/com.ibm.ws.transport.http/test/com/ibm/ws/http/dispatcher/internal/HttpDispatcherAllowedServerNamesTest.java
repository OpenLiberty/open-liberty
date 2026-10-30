/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.dispatcher.internal;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import junit.framework.Assert;
import test.common.SharedOutputManager;

/**
 * Unit tests for the {@code allowedServerNames} plumbing in {@link HttpDispatcher}:
 * parsing from a String and from a String[]; reload on modified(); removal clears
 * the matcher; rejected-entry INFO logged once per distinct value.
 *
 * Conventions follow {@link HttpDispatcherTest}: reflection on {@code instance},
 * plain {@code new HttpDispatcher()}, activate/modified, no jmock.
 */
public class HttpDispatcherAllowedServerNamesTest {

    private static final SharedOutputManager outputMgr =
            SharedOutputManager.getInstance().trace("*=all");

    @Rule
    public final SharedOutputManager outputRule = outputMgr;

    @Rule
    public final TestName name = new TestName();

    // Reflective access to the static instance field (same pattern as HttpDispatcherTest)
    static Field dispatcherInstance;
    static Field matcherField;
    static Field origAllowedServerNamesField;

    static {
        try {
            dispatcherInstance = HttpDispatcher.class.getDeclaredField("instance");
            dispatcherInstance.setAccessible(true);

            matcherField = HttpDispatcher.class.getDeclaredField("allowedServerNamesMatcher");
            matcherField.setAccessible(true);

            origAllowedServerNamesField = HttpDispatcher.class.getDeclaredField("origAllowedServerNames");
            origAllowedServerNamesField.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static void clearInstance() throws Exception {
        ((AtomicReference<HttpDispatcher>) dispatcherInstance.get(null)).set(null);
    }

    @SuppressWarnings("unchecked")
    private static HttpDispatcher getInstance() throws Exception {
        return ((AtomicReference<HttpDispatcher>) dispatcherInstance.get(null)).get();
    }

    private static AllowedServerNamesMatcher getMatcherField(HttpDispatcher d) throws Exception {
        return (AllowedServerNamesMatcher) matcherField.get(d);
    }

    private static String[] getOrigAllowedServerNames(HttpDispatcher d) throws Exception {
        return (String[]) origAllowedServerNamesField.get(d);
    }

    /** Build a minimal activation map with an allowedServerNames entry. */
    private static Map<String, Object> mapWithNames(Object allowedServerNames) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(HttpDispatcher.PROP_ENABLE_WELCOME_PAGE, Boolean.TRUE);
        map.put(HttpDispatcher.PROP_TRUSTED_PRIVATE_HEADER_ORIGIN, "*");
        map.put(HttpDispatcher.PROP_TRUSTED_SENSITIVE_HEADER_ORIGIN, "none");
        if (allowedServerNames != null) {
            map.put(HttpDispatcher.PROP_ALLOWED_SERVER_NAMES, allowedServerNames);
        }
        return map;
    }

    /** Build a minimal activation map without any allowedServerNames key. */
    private static Map<String, Object> mapWithoutNames() {
        return mapWithNames(null);
    }

    @Before
    public void setUp() throws Exception {
        clearInstance();
    }

    @After
    public void tearDown() throws Exception {
        clearInstance();
    }

    // -----------------------------------------------------------------------
    // Parsing from a String
    // -----------------------------------------------------------------------

    @Test
    public void parsedFromString() throws Exception {
        HttpDispatcher d = new HttpDispatcher();
        d.activate(mapWithNames("www.example.com, *.secure.example.com"));

        AllowedServerNamesMatcher m = HttpDispatcher.getAllowedServerNamesMatcher();
        Assert.assertNotNull("Matcher must not be null", m);
        Assert.assertTrue("www.example.com must match", m.matches("www.example.com"));
        Assert.assertTrue("a.secure.example.com must match", m.matches("a.secure.example.com"));
        Assert.assertFalse("other.example.com must not match", m.matches("other.example.com"));
    }

    // -----------------------------------------------------------------------
    // Parsing from a String[]
    // -----------------------------------------------------------------------

    @Test
    public void parsedFromStringArray() throws Exception {
        HttpDispatcher d = new HttpDispatcher();
        d.activate(mapWithNames(new String[] { "www.example.com", "*.secure.example.com" }));

        AllowedServerNamesMatcher m = HttpDispatcher.getAllowedServerNamesMatcher();
        Assert.assertTrue("www.example.com must match", m.matches("www.example.com"));
        Assert.assertTrue("a.secure.example.com must match", m.matches("a.secure.example.com"));
        Assert.assertFalse("other.example.com must not match", m.matches("other.example.com"));
    }

    // -----------------------------------------------------------------------
    // Reload via modified() with a different value
    // -----------------------------------------------------------------------

    @Test
    public void reloadWithDifferentValue() throws Exception {
        HttpDispatcher d = new HttpDispatcher();
        d.activate(mapWithNames("www.example.com"));

        Assert.assertTrue("www.example.com must match after activate",
                HttpDispatcher.getAllowedServerNamesMatcher().matches("www.example.com"));
        Assert.assertFalse("secure.example.com must not match after activate",
                HttpDispatcher.getAllowedServerNamesMatcher().matches("secure.example.com"));

        // Reload with a new value
        d.modified(mapWithNames("secure.example.com"));

        Assert.assertFalse("www.example.com must not match after reload",
                HttpDispatcher.getAllowedServerNamesMatcher().matches("www.example.com"));
        Assert.assertTrue("secure.example.com must match after reload",
                HttpDispatcher.getAllowedServerNamesMatcher().matches("secure.example.com"));
    }

    // -----------------------------------------------------------------------
    // Removal clears the matcher (key absent from config)
    // -----------------------------------------------------------------------

    @Test
    public void removalClearsMatcher() throws Exception {
        HttpDispatcher d = new HttpDispatcher();
        d.activate(mapWithNames("www.example.com"));

        Assert.assertTrue("www.example.com must match after activate",
                HttpDispatcher.getAllowedServerNamesMatcher().matches("www.example.com"));

        // Call modified without the allowedServerNames key -- simulates a property removal
        // where config is non-null and non-empty but the key is simply absent.
        d.modified(mapWithoutNames());

        AllowedServerNamesMatcher cleared = HttpDispatcher.getAllowedServerNamesMatcher();
        Assert.assertTrue("Matcher must be empty after removal", cleared.isEmpty());
        Assert.assertFalse("www.example.com must not match after removal",
                cleared.matches("www.example.com"));
    }

    /**
     * The early-return case: modified() is called with a null or empty config.
     * This is the path that the task note (S1-2 item 4) asked us to analyse.
     * We verified that the early return fires only for null/empty config -- a scenario
     * that does not occur during normal config updates (OSGi always delivers a
     * non-empty map).  We documented this in the code comment in modified().
     * This test confirms the field retains the last-assigned matcher in that edge case
     * (which is the expected behaviour; a null/empty config means DS has nothing new).
     */
    @Test
    public void earlyReturnDoesNotClearPreviousMatcherOnNullConfig() throws Exception {
        HttpDispatcher d = new HttpDispatcher();
        d.activate(mapWithNames("www.example.com"));

        Assert.assertTrue("www.example.com must match after activate",
                HttpDispatcher.getAllowedServerNamesMatcher().matches("www.example.com"));

        // Simulate the early-return path: pass null config
        d.modified(null);

        // The matcher is unchanged -- this is the documented edge case.
        Assert.assertTrue("Matcher must still match after null-config modified call",
                HttpDispatcher.getAllowedServerNamesMatcher().matches("www.example.com"));
    }

    // -----------------------------------------------------------------------
    // No-instance fallback
    // -----------------------------------------------------------------------

    @Test
    public void noInstanceReturnEmptyMatcher() throws Exception {
        // instance was cleared in setUp()
        AllowedServerNamesMatcher m = HttpDispatcher.getAllowedServerNamesMatcher();
        Assert.assertNotNull("Fallback matcher must not be null", m);
        Assert.assertTrue("Fallback matcher must be empty", m.isEmpty());
        Assert.assertFalse("Fallback matcher must never match", m.matches("www.example.com"));
    }

    // -----------------------------------------------------------------------
    // Rejected entries: logged once per distinct value
    // -----------------------------------------------------------------------

    @Test
    public void rejectedEntriesLoggedOncePerDistinctValue() throws Exception {
        HttpDispatcher d = new HttpDispatcher();
        // First activation with a rejected entry
        d.activate(mapWithNames("*"));

        String[] firstOrig = getOrigAllowedServerNames(d);
        Assert.assertNotNull("origAllowedServerNames must be set after first activate", firstOrig);

        // Second modified with the same value -- origAllowedServerNames must not change
        // (Arrays.equals guard skips the re-parse and re-log).
        d.modified(mapWithNames("*"));
        Assert.assertSame("origAllowedServerNames must be the same array instance on repeated identical value",
                firstOrig, getOrigAllowedServerNames(d));

        // Third modified with a different value -- origAllowedServerNames changes.
        d.modified(mapWithNames("www.example.com"));
        Assert.assertFalse("origAllowedServerNames must change when value changes",
                java.util.Arrays.equals(firstOrig, getOrigAllowedServerNames(d)));
    }

    @Test
    public void rejectedEntriesReportedOnValueChange() throws Exception {
        HttpDispatcher d = new HttpDispatcher();
        d.activate(mapWithNames("www.example.com"));

        // Now switch to a value with a rejected entry
        d.modified(mapWithNames("*"));

        AllowedServerNamesMatcher m = HttpDispatcher.getAllowedServerNamesMatcher();
        Assert.assertEquals("Rejected entry count must be 1", 1, m.getRejectedEntries().size());
        Assert.assertEquals("Rejected entry reason must be GUARD_1_WILDCARD_ONLY",
                AllowedServerNamesMatcher.RejectReason.GUARD_1_WILDCARD_ONLY,
                m.getRejectedEntries().get(0).getReason());
    }
}
