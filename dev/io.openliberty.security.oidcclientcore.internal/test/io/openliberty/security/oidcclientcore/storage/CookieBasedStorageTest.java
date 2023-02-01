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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.common.web.WebSSOUtils;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;

import test.common.SharedOutputManager;

public class CookieBasedStorageTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private final ReferrerURLCookieHandler referrerURLCookieHandler = mockery.mock(ReferrerURLCookieHandler.class);
    private final WebSSOUtils webSSOUtils = mockery.mock(WebSSOUtils.class);
    private final Cookie cookie = mockery.mock(Cookie.class);

    private final String testSSODomain = "testDomain";
    private final String testCookieName = "testName";
    private final String testCookieValue = "testValue";

    private CookieBasedStorage storage;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        storage = new CookieBasedStorage(request, response, referrerURLCookieHandler) {
            {
                this.webSsoUtils = webSSOUtils;
            }
        };
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_store() {
        mockery.checking(new Expectations() {
            {
                one(referrerURLCookieHandler).createCookie(testCookieName, testCookieValue, request);
                will(returnValue(cookie));
                one(webSSOUtils).getSsoDomain(request);
                will(returnValue(testSSODomain));
                one(cookie).setDomain(testSSODomain);
                one(response).addCookie(cookie);
            }
        });

        storage.store(testCookieName, testCookieValue);
    }

    @Test
    public void test_store_noDomain() {
        mockery.checking(new Expectations() {
            {
                one(referrerURLCookieHandler).createCookie(testCookieName, testCookieValue, request);
                will(returnValue(cookie));
                one(webSSOUtils).getSsoDomain(request);
                will(returnValue(null));
                one(response).addCookie(cookie);
            }
        });

        storage.store(testCookieName, testCookieValue);
    }

    @Test
    public void test_store_withStorageProperties() {
        CookieStorageProperties storageProperties = new CookieStorageProperties();
        storageProperties.setSecure(true);
        storageProperties.setHttpOnly(true);
        storageProperties.setStorageLifetimeSeconds(60);

        mockery.checking(new Expectations() {
            {
                one(referrerURLCookieHandler).createCookie(testCookieName, testCookieValue, request);
                will(returnValue(cookie));
                one(webSSOUtils).getSsoDomain(request);
                will(returnValue(testSSODomain));
                one(cookie).setDomain(testSSODomain);
                one(cookie).setSecure(true);
                one(cookie).setHttpOnly(true);
                one(cookie).setMaxAge(60);
                one(response).addCookie(cookie);
            }
        });

        storage.store(testCookieName, testCookieValue, storageProperties);
    }

    @Test
    public void test_get() {
        mockery.checking(new Expectations() {
            {
                one(request).getCookies();
                will(returnValue(new Cookie[] { cookie }));
                one(cookie).getName();
                will(returnValue(testCookieName));
                one(cookie).getValue();
                will(returnValue(testCookieValue));
            }
        });

        String value = storage.get(testCookieName);

        assertEquals("Expected the values to be equal.", testCookieValue, value);
    }

    @Test
    public void test_get_doesNotExist() {
        String doesNotExistCookieName = "doesNotExist";
        mockery.checking(new Expectations() {
            {
                one(request).getCookies();
                will(returnValue(new Cookie[] { cookie }));
                one(cookie).getName();
                will(returnValue(testCookieValue));
            }
        });

        String value = storage.get(doesNotExistCookieName);

        assertNull("Expected the value to be null if the cookie name does not exist in the cookies.", value);
    }

    @Test
    public void test_get_nullCookies() {
        mockery.checking(new Expectations() {
            {
                one(request).getCookies();
                will(returnValue(null));
            }
        });

        String value = storage.get(testCookieName);

        assertNull("Expected the value to be null if no cookies were sent.", value);
    }

    @Test
    public void test_remove() {
        mockery.checking(new Expectations() {
            {
                one(referrerURLCookieHandler).invalidateCookie(request, response, testCookieName, true);
            }
        });

        storage.remove(testCookieName);
    }

}
