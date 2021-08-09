/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.internal.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

/**
 * Testing various paths (with and without variables, and negative ones)
 */
public class HandlerPathTest {

    private static final String TEST_URL_1 = "/myURL/abc/";
    private static final String TEST_URL_2 = "/myURL/abc/other";
    private static final String TEST_URL_3 = "/myURL123/abc/";
    private static final String TEST_URL_4 = "/myURL?query=1&foo=bar";
    private static final String TEST_URL_5 = "/myURL/abc?city=Markham";
    private static final String TEST_URL_6 = "/other/abc?city=Markham";
    private static final String TEST_URL_7 = "/long/url/with/various/sections";
    private static final String TEST_URL_8 = "/ibm/bob?city=Markham";
    private static final String TEST_URL_9 = "/ibm/customer/123/address";
    private static final String TEST_URL_10 = "/myURL/abc/o/123";
    private static final String TEST_URL_11 = "/myURL/blah/abc/o/123";
    private static final String TEST_URL_12 = "/ibm/customer/abc_123/address";
    private static final String TEST_URL_13 = "/ibm/customer/abc_123/blah/address";

    @Test(expected = RuntimeException.class)
    public void testVariableRegistrationsException() {
        new HandlerPath("/{}/abc");
    }

    @Test(expected = RuntimeException.class)
    public void testVariableRegistrationsException_2() {
        new HandlerPath("/{{url}/abc");
    }

    @Test(expected = RuntimeException.class)
    public void testVariableRegistrationsException_4() {
        new HandlerPath("/{url/?}/abc");
    }

    @Test
    public void testVariableRegistrations() {
        String registeredPath = "/{url}/abc";
        HandlerPath handler = new HandlerPath(registeredPath);
        assertEquals(5, handler.length());
        assertTrue(handler.matches(TEST_URL_1));
        assertTrue(handler.matches(TEST_URL_2));
        assertTrue(handler.matches(TEST_URL_3));
        assertFalse(handler.matches(TEST_URL_4));
        assertTrue(handler.matches(TEST_URL_5));
        assertTrue(handler.matches(TEST_URL_6));
        assertTrue(handler.matches(TEST_URL_10));
        assertTrue(handler.matches(TEST_URL_11));
        assertEquals("myURL", handler.mapVariables(TEST_URL_1).get("url"));
        assertEquals("myURL123", handler.mapVariables(TEST_URL_3).get("url"));
        assertEquals("other", handler.mapVariables(TEST_URL_6).get("url"));

        registeredPath = "/myURL/{id}";
        handler = new HandlerPath(registeredPath);
        assertEquals(7, handler.length());
        assertTrue(handler.matches(TEST_URL_1));
        assertTrue(handler.matches(TEST_URL_2));
        assertFalse(handler.matches(TEST_URL_3));
        assertFalse(handler.matches(TEST_URL_4));
        assertTrue(handler.matches(TEST_URL_5));
        assertFalse(handler.matches(TEST_URL_6));
        assertTrue(handler.matches(TEST_URL_10));
        assertTrue(handler.matches(TEST_URL_11));
        assertEquals(null, handler.mapVariables(TEST_URL_1).get("url"));
        assertEquals("abc/", handler.mapVariables(TEST_URL_1).get("id"));
        assertEquals("blah/abc/o/123", handler.mapVariables(TEST_URL_11).get("id"));

        registeredPath = "/myURL/{id1}/{id2}";
        handler = new HandlerPath(registeredPath);
        assertEquals(8, handler.length());
        assertFalse(handler.matches(TEST_URL_1));
        assertTrue(handler.matches(TEST_URL_2));
        assertFalse(handler.matches(TEST_URL_3));
        assertFalse(handler.matches(TEST_URL_4));
        assertFalse(handler.matches(TEST_URL_5));
        assertTrue(handler.matches(TEST_URL_10));
        assertTrue(handler.matches(TEST_URL_11));

        Map<String, String> map = handler.mapVariables(TEST_URL_2);
        assertEquals(2, map.size());
        assertEquals("abc", map.get("id1"));
        assertEquals("other", map.get("id2"));

        map = handler.mapVariables(TEST_URL_10);
        assertEquals(2, map.size());
        assertEquals("abc/o", map.get("id1"));
        assertEquals("123", map.get("id2"));

        map = handler.mapVariables(TEST_URL_11);
        assertEquals(2, map.size());
        assertEquals("blah/abc/o", map.get("id1"));
        assertEquals("123", map.get("id2"));

        registeredPath = "/ibm/customer/{id1}/address";
        handler = new HandlerPath(registeredPath);
        assertEquals(22, handler.length());
        assertTrue(handler.matches(TEST_URL_9));
        assertTrue(handler.matches(TEST_URL_12));
        assertTrue(handler.matches(TEST_URL_13));
        assertEquals("123", handler.mapVariables(TEST_URL_9).get("id1"));
        assertEquals("abc_123", handler.mapVariables(TEST_URL_12).get("id1"));

        registeredPath = "/{company}/customer/{id}/address";
        handler = new HandlerPath(registeredPath);
        assertEquals(19, handler.length());
        assertTrue(handler.matches(TEST_URL_9));
        assertTrue(handler.matches(TEST_URL_12));
        assertTrue(handler.matches(TEST_URL_13));

        map = handler.mapVariables(TEST_URL_9);
        assertEquals(2, map.size());
        assertEquals("ibm", map.get("company"));
        assertEquals("123", map.get("id"));

        map = handler.mapVariables(TEST_URL_12);
        assertEquals(2, map.size());
        assertEquals("ibm", map.get("company"));
        assertEquals("abc_123", map.get("id"));
    }

    @Test
    public void testNonVariableRegistrations() {
        String registeredPath = "/myURL";
        HandlerPath handler = new HandlerPath(registeredPath);
        assertEquals(6, handler.length());
        assertTrue(handler.matches(TEST_URL_1));
        assertTrue(handler.matches(TEST_URL_2));
        assertFalse(handler.matches(TEST_URL_3));
        assertTrue(handler.matches(TEST_URL_4));
        assertTrue(handler.matches(TEST_URL_5));
        assertFalse(handler.matches(TEST_URL_6));

        registeredPath = "/myURL/abc";
        handler = new HandlerPath(registeredPath);
        assertEquals(10, handler.length());
        assertTrue(handler.matches(TEST_URL_1));
        assertTrue(handler.matches(TEST_URL_2));
        assertFalse(handler.matches(TEST_URL_3));
        assertFalse(handler.matches(TEST_URL_4));
        assertTrue(handler.matches(TEST_URL_5));
        assertFalse(handler.matches(TEST_URL_6));

        registeredPath = "/myURL/abc/"; //end-trailing slashes are removed
        handler = new HandlerPath(registeredPath);
        assertEquals(10, handler.length());
        assertTrue(handler.matches(TEST_URL_1));
        assertTrue(handler.matches(TEST_URL_2));
        assertFalse(handler.matches(TEST_URL_3));
        assertFalse(handler.matches(TEST_URL_4));
        assertTrue(handler.matches(TEST_URL_5));
        assertFalse(handler.matches(TEST_URL_6));

        registeredPath = "/myURL/abc/o";
        handler = new HandlerPath(registeredPath);
        assertEquals(12, handler.length());
        assertFalse(handler.matches(TEST_URL_1));
        assertFalse(handler.matches(TEST_URL_2));
        assertFalse(handler.matches(TEST_URL_3));
        assertFalse(handler.matches(TEST_URL_4));
        assertFalse(handler.matches(TEST_URL_5));
        assertFalse(handler.matches(TEST_URL_6));
        assertTrue(handler.matches(TEST_URL_10));

        registeredPath = "/long/url/with/various";
        handler = new HandlerPath(registeredPath);
        assertEquals(22, handler.length());
        assertTrue(handler.matches(TEST_URL_7));
        assertFalse(handler.matches(TEST_URL_8));
    }

}
