/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import com.ibm.ws.webcontainer.security.CookieHelper;

public class CookieHelperTest {
    private final Mockery mock = new JUnit4Mockery();
    private final HttpServletResponse resp = mock.mock(HttpServletResponse.class);

    /**
     * getCookieValue shall return null if the Cookies array
     * is null.
     */
    @Test
    public void getCookieValue_nullCookies() {
        assertNull(CookieHelper.getCookieValue(null, null));
        assertFalse(CookieHelper.hasCookie(null, null));
    }

    /**
     * getCookieValue shall return null if the Cookies array
     * is empty.
     */
    @Test
    public void getCookieValue_emptyCookies() {
        Cookie[] cookies = new Cookie[] {};
        assertNull(CookieHelper.getCookieValue(cookies, null));
        assertFalse(CookieHelper.hasCookie(cookies, null));
    }

    /**
     * getCookieValue shall return null if the Cookies array
     * does not contain the specified name.
     */
    @Test
    public void getCookieValue_nonExistingCookie() {
        Cookie[] cookies = new Cookie[] { new Cookie("cookieName", "cookieValue") };
        String name = "IDontExist";
        assertNull(CookieHelper.getCookieValue(cookies, name));
        assertFalse(CookieHelper.hasCookie(cookies, name));
    }

    /**
     * getCookieValue shall return the value of the matching
     * Cookie which exists in the Cookies array.
     */
    @Test
    public void getCookieValue_existingCookieEmptyValue() {
        String name = "cookieName";
        String value = "";
        Cookie[] cookies = new Cookie[] { new Cookie(name, value) };
        assertEquals(value, CookieHelper.getCookieValue(cookies, name));
        assertTrue(CookieHelper.hasCookie(cookies, name));
    }

    /**
     * getCookieValue shall return the value of the matching
     * Cookie which exists in the Cookies array.
     */
    @Test
    public void getCookieValue_existingCookie() {
        String name = "cookieName";
        String value = "cookieValue";
        Cookie[] cookies = new Cookie[] { new Cookie(name, value) };
        assertEquals(value, CookieHelper.getCookieValue(cookies, name));
        assertTrue(CookieHelper.hasCookie(cookies, name));
    }

    /**
     * getCookieValue shall return the value of the matching
     * Cookie which exists in the Cookies array, ignoring case.
     */
    @Test
    public void getCookieValue_existingCookieDifferentCase() {
        String name = "cookieName";
        String value = "cookieValue";
        Cookie[] cookies = new Cookie[] { new Cookie(name, value) };
        assertEquals(value, CookieHelper.getCookieValue(cookies, "COOKIENAME"));
        assertTrue(CookieHelper.hasCookie(cookies, "COOKIENAME"));
    }

    /**
     * getCookieValue shall return the value of the matching
     * Cookie which exists in the Cookies array.
     */
    @Test
    public void getCookieValue_manyCookies() {
        String name = "cookieName";
        String value = "cookieValue";
        Cookie[] cookies = new Cookie[] {
                                          new Cookie("name1", "value1"),
                                          new Cookie(name, value)
        };
        assertEquals(value, CookieHelper.getCookieValue(cookies, name));
        assertTrue(CookieHelper.hasCookie(cookies, name));
    }

    /**
     * getCookieValue shall return the value of the first matching
     * Cookie which exists in the Cookies array.
     */
    @Test
    public void getCookieValue_multipleCookies() {
        String name = "cookieName";
        String value1 = "cookieValue1";
        String value2 = "cookieValue2";
        Cookie[] cookies = new Cookie[] {
                                          new Cookie("name1", "value1"),
                                          new Cookie(name, value1),
                                          new Cookie("name2", "value2"),
                                          new Cookie(name, value2)
        };
        assertEquals(value1, CookieHelper.getCookieValue(cookies, name));
        assertTrue(CookieHelper.hasCookie(cookies, name));
    }

    /**
     * getCookieValues shall return null if the Cookies array
     * is null.
     */
    @Test
    public void getCookieValues_nullCookies() {
        assertNull(CookieHelper.getCookieValues(null, null));
    }

    /**
     * getCookieValues shall return null if the Cookies array
     * is empty.
     */
    @Test
    public void getCookieValues_emptyCookies() {
        Cookie[] cookies = new Cookie[] {};
        assertNull(CookieHelper.getCookieValues(cookies, null));
    }

    /**
     * getCookieValues shall return null if the Cookies array
     * does not contain the specified name.
     */
    @Test
    public void getCookieValues_nonExistingCookie() {
        Cookie[] cookies = new Cookie[] { new Cookie("cookieName", "cookieValue") };
        String name = "IDontExist";
        assertNull(CookieHelper.getCookieValue(cookies, name));
    }

    /**
     * getCookieValues shall return String[] of the values for the Cookies
     * matching the specified Cookie name in the Cookies array.
     */
    @Test
    public void getCookieValues_onlyOneCookie() {
        String name = "cookieName";
        String value = "cookieValue";
        Cookie[] cookies = new Cookie[] { new Cookie(name, value) };
        String[] result = CookieHelper.getCookieValues(cookies, name);
        assertEquals(1, result.length);
        assertEquals(value, result[0]);
    }

    /**
     * getCookieValues shall return String[] of the values for the Cookies
     * matching the specified Cookie name in the Cookies array, ignoring case.
     */
    @Test
    public void getCookieValues_onlyOneCookieDifferentCase() {
        String name = "cookieName";
        String value = "cookieValue";
        Cookie[] cookies = new Cookie[] { new Cookie(name, value) };
        String[] result = CookieHelper.getCookieValues(cookies, "COOKIENAME");
        assertEquals(1, result.length);
        assertEquals(value, result[0]);
    }

    /**
     * getCookieValues shall return String[] of the values for the Cookies
     * matching the specified Cookie name in the Cookies array.
     */
    @Test
    public void getCookieValues_onlyOneMatchingCookie() {
        String name = "cookieName";
        String value = "cookieValue";
        Cookie[] cookies = new Cookie[] {
                                          new Cookie("name1", "value1"),
                                          new Cookie(name, value)
        };
        String[] result = CookieHelper.getCookieValues(cookies, name);
        assertEquals(1, result.length);
        assertEquals(value, result[0]);
    }

    /**
     * getCookieValues shall return String[] of the values for the Cookies
     * matching the specified Cookie name in the Cookies array.
     */
    @Test
    public void getCookieValues_multipleCookies() {
        String name = "cookieName";
        String value1 = "cookieValue1";
        String value2 = "cookieValue2";
        Cookie[] cookies = new Cookie[] {
                                          new Cookie("name1", "value1"),
                                          new Cookie(name, value1),
                                          new Cookie("name2", "value2"),
                                          new Cookie(name, value2)
        };
        String[] result = CookieHelper.getCookieValues(cookies, name);
        assertEquals(2, result.length);
        assertEquals(value1, result[0]);
        assertEquals(value2, result[1]);
    }

    /**
     * addCookiesToResponse does not handle null arguments.
     */
    @Test(expected = NullPointerException.class)
    public void addCookiesToResponse_nullList() {
        CookieHelper.addCookiesToResponse(null, resp);
    }

    @Test(expected = NullPointerException.class)
    public void addCookiesToResponse_nullResponse() {
        List<Cookie> cookieList = new ArrayList<Cookie>();
        cookieList.add(new Cookie("abc", "123"));
        CookieHelper.addCookiesToResponse(cookieList, null);
    }

    @Test
    public void addCookiesToResponse() {
        final Cookie cookie1 = new Cookie("cookie1", "123");
        final Cookie cookie2 = new Cookie("cookie2", "123");
        List<Cookie> cookieList = new ArrayList<Cookie>();
        cookieList.add(cookie1);
        cookieList.add(cookie2);

        mock.checking(new Expectations() {
            {
                one(resp).addCookie(cookie1);
                one(resp).addCookie(cookie2);
            }
        });

        CookieHelper.addCookiesToResponse(cookieList, resp);
        mock.assertIsSatisfied();
    }

    @Test
    public void test_splitValueIntoMaximumLengthChunks_negativeChunkLength() {
        String cookieValue = "chocolate chip";
        int maxValueLength = -1;
        String[] valueChunks = CookieHelper.splitValueIntoMaximumLengthChunks(cookieValue, maxValueLength);
        assertNotNull(valueChunks);
        assertEquals("Should have returned an empty array, but got: " + Arrays.toString(valueChunks), 0, valueChunks.length);
    }

    @Test
    public void test_splitValueIntoMaximumLengthChunks_zeroChunkLength() {
        String cookieValue = "chocolate chip";
        int maxValueLength = 0;
        String[] valueChunks = CookieHelper.splitValueIntoMaximumLengthChunks(cookieValue, maxValueLength);
        assertNotNull(valueChunks);
        assertEquals("Should have returned an empty array, but got: " + Arrays.toString(valueChunks), 0, valueChunks.length);
    }

    @Test
    public void test_splitValueIntoMaximumLengthChunks_nullCookieValue() {
        String cookieValue = null;
        int maxValueLength = 10;
        String[] valueChunks = CookieHelper.splitValueIntoMaximumLengthChunks(cookieValue, maxValueLength);
        assertNotNull(valueChunks);
        assertEquals("Should have returned an empty array, but got: " + Arrays.toString(valueChunks), 0, valueChunks.length);
    }

    @Test
    public void test_splitValueIntoMaximumLengthChunks_emptyCookieValue() {
        String cookieValue = "";
        int maxValueLength = 10;
        String[] valueChunks = CookieHelper.splitValueIntoMaximumLengthChunks(cookieValue, maxValueLength);
        assertNotNull(valueChunks);
        assertEquals("Should have returned an empty array, but got: " + Arrays.toString(valueChunks), 0, valueChunks.length);
    }

    @Test
    public void test_splitValueIntoMaximumLengthChunks_stringLengthShorterThanChunkLength() {
        String cookieValue = "abc";
        int maxValueLength = 10;
        String[] valueChunks = CookieHelper.splitValueIntoMaximumLengthChunks(cookieValue, maxValueLength);
        assertNotNull(valueChunks);
        assertEquals("Array was not the expected length. Got: " + Arrays.toString(valueChunks), 1, valueChunks.length);
    }

    @Test
    public void test_splitValueIntoMaximumLengthChunks_stringLengthEqualsChunkLength() {
        String cookieValue = "abc";
        int maxValueLength = 3;
        String[] valueChunks = CookieHelper.splitValueIntoMaximumLengthChunks(cookieValue, maxValueLength);
        assertNotNull(valueChunks);
        assertEquals("Array was not the expected length. Got: " + Arrays.toString(valueChunks), 1, valueChunks.length);
    }

    @Test
    public void test_splitValueIntoMaximumLengthChunks_stringLengthLongerThanChunkLength() {
        String cookieValue = "1234567890";
        int maxValueLength = 3;
        String[] valueChunks = CookieHelper.splitValueIntoMaximumLengthChunks(cookieValue, maxValueLength);
        assertNotNull(valueChunks);
        assertEquals("Array was not the expected length. Got: " + Arrays.toString(valueChunks), 4, valueChunks.length);
        assertEquals("First chunk did not match expected value.", "123", valueChunks[0]);
        assertEquals("Second chunk did not match expected value.", "456", valueChunks[1]);
        assertEquals("Third chunk did not match expected value.", "789", valueChunks[2]);
        assertEquals("Fourth chunk did not match expected value.", "0", valueChunks[3]);
    }

}
