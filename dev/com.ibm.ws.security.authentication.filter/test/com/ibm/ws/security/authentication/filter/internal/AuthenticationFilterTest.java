/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.filter.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

public class AuthenticationFilterTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    final static String MY_HOST_NAME = "myHostName";
    final static String MY_AUTH_FILTER = "myAuthFilter";

    private final HttpServletRequest req = mock.mock(HttpServletRequest.class);

    @Test
    public void testConstructor() throws Exception {
        final String methodName = "testConstructor";
        try {
            AuthenticationFilterImpl authFilter = new AuthenticationFilterImpl();
            assertNotNull("Authentication filter should not be null", authFilter);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetAuthFilterConfig_null() throws Exception {
        final String methodName = "testGetAuthFilterConfig_null";
        try {
            AuthenticationFilterImpl authFilter = new AuthenticationFilterImpl();
            authFilter.setProcessAll(true);
            authFilter.init(null);
            assertNull("config ID should be null", authFilter.getAuthFilterConfig(null));
            assertTrue("no authFilter all request will be accepted", authFilter.isAccepted(req));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetAuthFilterConfig() throws Exception {
        final String methodName = "testGetAuthFilterConfig";
        final Map<String, Object> props = new Hashtable<String, Object>();
        props.put(AuthFilterConfig.KEY_ID, MY_AUTH_FILTER);
        final Map<String, Object> p = new Hashtable<String, Object>();
        p.put("host.0.name", MY_HOST_NAME);
        p.put("host.0.matchType", AuthFilterConfig.MATCH_TYPE_NOT_CONTAIN);
        props.putAll(p);

        try {
            AuthenticationFilterImpl authFilter = new AuthenticationFilterImpl();
            authFilter.activate(props);
            assertEquals("authFilter ID should be " + MY_AUTH_FILTER, MY_AUTH_FILTER, authFilter.getAuthFilterConfig(props));
            assertTrue("Expected message was not logged",
                       outputMgr.checkForMessages("CWWKS4358I:"));

            authFilter.modify(props);
            assertTrue("Expected message was not logged",
                       outputMgr.checkForMessages("CWWKS4359I:"));

            authFilter.deactivate(props);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testAuthFilterNotContainSingleValue() throws Exception {
        final String methodName = "testAuthFilterNotContainSingleValue";
        final Map<String, Object> props = new Hashtable<String, Object>();
        props.put(AuthFilterConfig.KEY_ID, MY_AUTH_FILTER);
        final Map<String, Object> p = new Hashtable<String, Object>();
        final String KEY_URL = "request-url";
        final String URL_1 = "/url1";
        final String URL_NOTCONTAIN = "/urlNotContain";
        p.put("requestUrl.0.urlPattern", URL_1);
        p.put("requestUrl.0.matchType", AuthFilterConfig.MATCH_TYPE_NOT_CONTAIN);
        props.putAll(p);

        mock.checking(new Expectations() {
            {
                oneOf(req).getHeader(KEY_URL);
                will(returnValue(URL_NOTCONTAIN));
                oneOf(req).getHeader(KEY_URL);
                will(returnValue(URL_1));
            }
        });

        try {
            AuthenticationFilterImpl authFilter = new AuthenticationFilterImpl();
            authFilter.activate(props);
            assertEquals("authFilter ID should be " + MY_AUTH_FILTER, MY_AUTH_FILTER, authFilter.getAuthFilterConfig(props));
            assertTrue("Expected message was not logged",
                       outputMgr.checkForMessages("CWWKS4358I:"));
            assertTrue("authFilter should return true if url does not match", authFilter.isAccepted(req));
            assertFalse("authFilter should return false if url_1 matches", authFilter.isAccepted(req));

            authFilter.deactivate(props);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testAuthFilterNotContainMultipleValues() throws Exception {
        final String methodName = "testAuthFilterNotContainMultipleValues";
        final Map<String, Object> props = new Hashtable<String, Object>();
        props.put(AuthFilterConfig.KEY_ID, MY_AUTH_FILTER);
        final Map<String, Object> p = new Hashtable<String, Object>();
        final String KEY_URL = "request-url";
        final String URL_1 = "/url1";
        final String URL_2 = "/url2";
        final String URL_3 = "/url3";
        final String URL_NOTCONTAIN = "/urlNotContain";
        p.put("requestUrl.0.urlPattern", URL_1 + "|" + URL_2 + "|" + URL_3);
        p.put("requestUrl.0.matchType", AuthFilterConfig.MATCH_TYPE_NOT_CONTAIN);
        props.putAll(p);

        mock.checking(new Expectations() {
            {
                oneOf(req).getHeader(KEY_URL);
                will(returnValue(URL_1));
                oneOf(req).getHeader(KEY_URL);
                will(returnValue(URL_2));
                oneOf(req).getHeader(KEY_URL);
                will(returnValue(URL_3));
                oneOf(req).getHeader(KEY_URL);
                will(returnValue(URL_NOTCONTAIN));
            }
        });

        try {
            AuthenticationFilterImpl authFilter = new AuthenticationFilterImpl();
            authFilter.activate(props);
            assertEquals("authFilter ID should be " + MY_AUTH_FILTER, MY_AUTH_FILTER, authFilter.getAuthFilterConfig(props));
            assertTrue("Expected message was not logged",
                       outputMgr.checkForMessages("CWWKS4358I:"));
            assertFalse("authFilter should return false if url_1 matches", authFilter.isAccepted(req));
            assertFalse("authFilter should return false if url_2 matches", authFilter.isAccepted(req));
            assertFalse("authFilter should return false if url_3 matches", authFilter.isAccepted(req));
            assertTrue("authFilter should return true if url does not match", authFilter.isAccepted(req));

            authFilter.deactivate(props);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
