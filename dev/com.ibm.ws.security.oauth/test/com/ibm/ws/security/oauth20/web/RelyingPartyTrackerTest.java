/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import test.common.SharedOutputManager;

public class RelyingPartyTrackerTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.oauth.*=all");

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private final WebAppSecurityConfig config = mockery.mock(WebAppSecurityConfig.class);
    private final Cookie cookie = mockery.mock(Cookie.class);

    private ReferrerURLCookieHandler handler;

    RelyingPartyTracker tracker;

    class TestRelyingPartyTracker extends RelyingPartyTracker {
        public TestRelyingPartyTracker(HttpServletRequest request, HttpServletResponse response) {
            super(request, response);
        }

        @Override
        ReferrerURLCookieHandler getReferrerURLCookieHandler() {
            return handler;
        }
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        handler = new ReferrerURLCookieHandler(config);
        tracker = new TestRelyingPartyTracker(request, response);

        // Set some expectations for things that don't overly matter for testing this class
        mockery.checking(new Expectations() {
            {
                allowing(config).getHttpOnlyCookies();
                will(returnValue(false));
                allowing(config).getSSORequiresSSL();
                will(returnValue(false));
                allowing(config).getSameSiteCookie();
                will(returnValue("Disabled"));
                allowing(request).getPathInfo();
                allowing(request).getRequestURI();
                allowing(request).getRequestURL();
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        // outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_trackRelyingParty_noCookies() {
        String clientId = "myClientId";
        mockery.checking(new Expectations() {
            {
                one(request).getCookies();
                will(returnValue(null));
                one(response).addCookie(with(any(Cookie.class)));
            }
        });
        tracker.trackRelyingParty(clientId);
    }

    @Test
    public void test_createNewRelyingPartyTrackingCookie_emptyClientId() {
        String clientId = "";
        Cookie result = tracker.createNewRelyingPartyTrackingCookie(request, handler, clientId);
        assertEquals("Created cookie does not have the correct name.", RelyingPartyTracker.TRACK_RELYING_PARTY_COOKIE_NAME, result.getName());
        assertEquals("Created cookie does not have the correctly encoded value.", clientId, result.getValue());
        assertEquals("Created cookie does not have the correct path.", "/", result.getPath());
    }

    @Test
    public void test_createNewRelyingPartyTrackingCookie_simpleClientId() {
        String clientId = "myClientId";
        Cookie result = tracker.createNewRelyingPartyTrackingCookie(request, handler, clientId);
        assertEquals("Created cookie does not have the correct name.", RelyingPartyTracker.TRACK_RELYING_PARTY_COOKIE_NAME, result.getName());
        // Each entry should be encoded, then the whole cookie value will be encoded
        String expectedValue = tracker.encodeValue(tracker.encodeValue(clientId));
        assertEquals("Created cookie does not have the correctly encoded value.", expectedValue, result.getValue());
        assertEquals("Created cookie does not have the correct path.", "/", result.getPath());
    }

    @Test
    public void test_createNewRelyingPartyTrackingCookie_complexClientId() {
        String clientId = "my !@#$%^&*(),./ id";
        Cookie result = tracker.createNewRelyingPartyTrackingCookie(request, handler, clientId);
        assertEquals("Created cookie does not have the correct name.", RelyingPartyTracker.TRACK_RELYING_PARTY_COOKIE_NAME, result.getName());
        // Each entry should be encoded, then the whole cookie value will be encoded
        String expectedValue = tracker.encodeValue(tracker.encodeValue(clientId));
        assertEquals("Created cookie does not have the correctly encoded value.", expectedValue, result.getValue());
        assertEquals("Created cookie does not have the correct path.", "/", result.getPath());
    }

    @Test
    public void test_updateExistingTrackingCookie_nullValue() {
        Cookie trackingCookie = new Cookie("name", null);
        String clientId = "clientId";
        Cookie result = tracker.updateExistingTrackingCookie(trackingCookie, clientId, handler);
        assertEquals("Updated cookie does not have the correct name.", RelyingPartyTracker.TRACK_RELYING_PARTY_COOKIE_NAME, result.getName());
        // Each entry should be encoded, then the whole cookie value will be encoded
        String expectedValue = tracker.encodeValue(tracker.encodeValue(clientId));
        assertEquals("Updated cookie does not have the correct value.", expectedValue, result.getValue());
    }

    @Test
    public void test_updateExistingTrackingCookie_emptyValue() {
        Cookie trackingCookie = new Cookie("name", "");
        String clientId = "clientId";
        Cookie result = tracker.updateExistingTrackingCookie(trackingCookie, clientId, handler);
        assertEquals("Updated cookie does not have the correct name.", RelyingPartyTracker.TRACK_RELYING_PARTY_COOKIE_NAME, result.getName());
        // Each entry should be encoded, then the whole cookie value will be encoded
        String expectedValue = tracker.encodeValue(tracker.encodeValue(clientId));
        assertEquals("Updated cookie does not have the correct value.", expectedValue, result.getValue());
    }

    @Test
    public void test_updateExistingTrackingCookie() {
        String client1 = "client1";
        String client2 = "client2";
        Cookie trackingCookie = new Cookie("name", tracker.encodeValue(tracker.encodeValue(client1) + "," + tracker.encodeValue(client2)));
        String clientId = "new client ID";
        Cookie result = tracker.updateExistingTrackingCookie(trackingCookie, clientId, handler);
        assertEquals("Updated cookie does not have the correct name.", RelyingPartyTracker.TRACK_RELYING_PARTY_COOKIE_NAME, result.getName());
        // Each entry should be encoded, then the whole cookie value will be encoded
        String expectedValue = tracker.encodeValue(tracker.encodeValue(client1) + "," + tracker.encodeValue(client2) + "," + tracker.encodeValue(clientId));
        assertEquals("Updated cookie does not have the correct value.", expectedValue, result.getValue());
    }

    @Test
    public void test_updateExistingCookieValue_nullExistingCookieValue() {
        String existingCookieValue = null;
        String clientId = "clientId";
        Cookie result = tracker.updateExistingCookieValue(existingCookieValue, clientId, handler);
        assertEquals("Updated cookie does not have the correct name.", RelyingPartyTracker.TRACK_RELYING_PARTY_COOKIE_NAME, result.getName());
        String expectedValue = tracker.encodeValue(tracker.encodeValue(clientId));
        assertEquals("Updated cookie does not have the correct value.", expectedValue, result.getValue());
    }

    @Test
    public void test_updateExistingCookieValue() {
        String client1 = "1";
        String client2 = "2";
        String existingCookieValue = tracker.encodeValue(tracker.encodeValue(client1) + "," + tracker.encodeValue(client2));
        String clientId = "3";
        Cookie result = tracker.updateExistingCookieValue(existingCookieValue, clientId, handler);
        assertEquals("Updated cookie does not have the correct name.", RelyingPartyTracker.TRACK_RELYING_PARTY_COOKIE_NAME, result.getName());
        String expectedValue = tracker.encodeValue(tracker.encodeValue(client1) + "," + tracker.encodeValue(client2) + "," + tracker.encodeValue(clientId));
        assertEquals("Updated cookie does not have the correct value.", expectedValue, result.getValue());
    }

    @Test
    public void test_updateExistingCookieValue_tryToAddExistingClient() {
        String client1 = "1";
        String client2 = "2";
        String existingCookieValue = tracker.encodeValue(tracker.encodeValue(client1) + "," + tracker.encodeValue(client2));
        String clientId = client2;
        Cookie result = tracker.updateExistingCookieValue(existingCookieValue, clientId, handler);
        assertEquals("Updated cookie does not have the correct name.", RelyingPartyTracker.TRACK_RELYING_PARTY_COOKIE_NAME, result.getName());
        assertEquals("Updated cookie should have had the same value as the original cookie value.", existingCookieValue, result.getValue());
    }

    @Test
    public void test_getExistingTrackedClientIds_emptyCookieValue() {
        String rawExistingCookieValue = "";
        List<String> result = tracker.getExistingTrackedClientIds(rawExistingCookieValue);
        assertTrue("Input was not encoded properly, so the result should have been empty. Result was: " + result, result.isEmpty());
    }

    @Test
    public void test_getExistingTrackedClientIds_notEncoded() {
        String rawExistingCookieValue = "my client";
        List<String> result = tracker.getExistingTrackedClientIds(rawExistingCookieValue);
        assertTrue("Input was not encoded properly, so the result should have been empty. Result was: " + result, result.isEmpty());
    }

    @Test
    public void test_getExistingTrackedClientIds_singleEntry_insufficientlyEncoded() {
        String expectedEntry = "myClientId";
        String rawExistingCookieValue = tracker.encodeValue(expectedEntry);
        List<String> result = tracker.getExistingTrackedClientIds(rawExistingCookieValue);
        assertTrue("Input was not encoded properly, so the result should have been empty. Result was: " + result, result.isEmpty());
    }

    @Test
    public void test_getExistingTrackedClientIds_singleEntry_properlyEncoded() {
        String expectedEntry = "myClientId";
        String rawExistingCookieValue = tracker.encodeValue(tracker.encodeValue(expectedEntry));
        List<String> result = tracker.getExistingTrackedClientIds(rawExistingCookieValue);
        assertEquals("Resulting set should contained a single entry, but did not. Result was: " + result, 1, result.size());
        assertTrue("Resulting set did not contain the expected entry. Result was: " + result, result.contains(expectedEntry));
    }

    @Test
    public void test_getExistingTrackedClientIds_multipleEntries_insufficientlyEncoded() {
        String client1 = "client1";
        String client2 = "client2";
        String rawExistingCookieValue = tracker.encodeValue(client1) + "," + tracker.encodeValue(client2);
        List<String> result = tracker.getExistingTrackedClientIds(rawExistingCookieValue);
        assertTrue("Input was not encoded properly, so the result should have been empty. Result was: " + result, result.isEmpty());
    }

    @Test
    public void test_getExistingTrackedClientIds_multipleEntries_properlyEncoded() {
        String client1 = "client1";
        String client2 = "client2";
        String rawExistingCookieValue = tracker.encodeValue(tracker.encodeValue(client1) + "," + tracker.encodeValue(client2));
        List<String> result = tracker.getExistingTrackedClientIds(rawExistingCookieValue);
        assertFalse("Input was encoded properly, so the result should not have been empty.", result.isEmpty());
        assertEquals("Result did not contain the expected number of entries.", 2, result.size());
        assertTrue("Result was missing the expected \"" + client1 + "\" entry. Result was: " + result, result.contains(client1));
        assertTrue("Result was missing the expected \"" + client2 + "\" entry. Result was: " + result, result.contains(client2));
    }

    @Test
    public void test_createCookieValue_emptyListOfClientIds() {
        List<String> clientIdList = new ArrayList<String>();
        String result = tracker.createCookieValue(clientIdList);
        assertEquals("Cookie value for an empty set input should have been an empty string.", "", result);
    }

    @Test
    public void test_createCookieValue_singleEntry() {
        String clientId = "my client ID!";
        List<String> clientIdList = new ArrayList<String>();
        clientIdList.add(clientId);
        String result = tracker.createCookieValue(clientIdList);
        // Each individual entry should be encoded, and the overall result should be encoded
        String expectedResult = tracker.encodeValue(tracker.encodeValue(clientId));
        assertEquals("Result did not match the expected cookie value.", expectedResult, result);
    }

    @Test
    public void test_createCookieValue_multipleEntries() {
        List<String> clientIdList = new ArrayList<String>();
        String client1 = "client 1";
        String client2 = "! client,2";
        clientIdList.add(client1);
        clientIdList.add(client2);
        String result = tracker.createCookieValue(clientIdList);
        // Each individual entry should be encoded, and the overall result should be encoded
        String client1Enc = tracker.encodeValue(client1);
        String client2Enc = tracker.encodeValue(client2);
        String expectedResult = tracker.encodeValue(client1Enc + "," + client2Enc);
        assertEquals("Result did not equal the expected result.", expectedResult, result);
    }

    @Test
    public void test_encodeValue_emptyInput() {
        String input = "";
        String result = tracker.encodeValue(input);
        assertEquals("Result did not match the expected value.", input, result);
    }

    @Test
    public void test_encodeValue() {
        String input = "my client 1 & other";
        String result = tracker.encodeValue(input);
        String expectedResult = "bXkgY2xpZW50IDEgJiBvdGhlcg==";
        assertEquals("Result did not match the expected value.", expectedResult, result);
    }

    @Test
    public void test_decodeValue_emptyInput() {
        String input = "";
        String result = tracker.decodeValue(input);
        assertNull("Input was not encoded correctly, so the result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_decodeValue_inputNotEncoded() {
        String input = "my client 1 & other";
        String result = tracker.decodeValue(input);
        assertNull("Input was not encoded correctly, so the result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_decodeValue() {
        String input = "MSwyJjN8NA==";
        String result = tracker.decodeValue(input);
        String expectedResult = "1,2&3|4";
        assertEquals("Decoding value did not match the expected value.", expectedResult, result);
    }

}
