/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
/**
 *
 */
public class ForwardRequestInfoTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.saml.sso20.*=all");
    @Rule
    public TestRule managerRule = outputMgr;
    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();
    private static HttpServletResponse response = common.getServletResponse();

    @Rule
    public final TestName testName = new TestName();
    private ForwardRequestInfo forwardRequest = null;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
        outputMgr.trace("*=all=disabled");
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        forwardRequest = new ForwardRequestInfo("http://idp/login");
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
        System.out.println("Exiting test: " + testName.getMethodName());
    }

    @Test
    public void testHandleFragmentCookiesAndNonce_JavaScript() {
        String methodName = "testHandleFragmentCookiesAndNonce_JavaScript";

        String js = forwardRequest.handleFragmentCookiesAndNonce(response);
        assertNotNull(js);
        //do a cursory check that we're getting back a JavaScript header
        String jslower = js.toLowerCase();
        assertTrue(jslower.contains("<script"));
        assertTrue(jslower.contains("</script>"));
    }

    @Test
    public void testHandleFragmentCookiesAndNonce_MaxAgeNotSet() {
        String methodName = "testHandleFragmentCookiesAndNoncez_MaxAgeNotSet";

        String js = forwardRequest.handleFragmentCookiesAndNonce(response);
        assertNotNull(js);
        //check for the default value
        assertTrue(js.contains("Max-Age="+String.valueOf(10*60)));
    }

    @Test
    public void testHandleFragmentCookiesAndNonce_MaxAgeSet() {
        String methodName = "testHandleFragmentCookiesAndNoncez_MaxAgeSet";

        long maxAgeSec = 300;   //5 minutes
        forwardRequest.setFragmentCookieMaxAge(maxAgeSec*1000); 
        String js = forwardRequest.handleFragmentCookiesAndNonce(response);
        assertNotNull(js);
        assertTrue(js.contains("Max-Age="+String.valueOf(maxAgeSec)));
    }

    @Test
    public void testHandleFragmentCookiesAndNonce_SetNonce() {
        String methodName = "testHandleFragmentCookiesAndNonce_SetNonce";
        mockery.checking(new Expectations() {
            {
                one(response).addHeader(with(any(String.class)), with(any(String.class)));
            }
        });

        forwardRequest.setCspHeader("script-src 'self' 'nonce-%NONCE%' ; object-src 'self'; frame-src 'self'");
        String js = forwardRequest.handleFragmentCookiesAndNonce(response);
        assertNotNull(js);
        assertTrue(js.contains("nonce="));
    }

    @Test
    public void testHandleFragmentCookiesAndNonce_NoNonce() {
        String methodName = "testHandleFragmentCookiesAndNonce_NoNonce";
        mockery.checking(new Expectations() {
            {
                one(response).addHeader(with(any(String.class)), with(any(String.class)));
            }
        });

        forwardRequest.setCspHeader("script-src 'self' ; object-src 'self'; frame-src 'self'");
        String js = forwardRequest.handleFragmentCookiesAndNonce(response);
        assertNotNull(js);
        assertFalse(js.contains("nonce="));
    }

    @Test
    public void testHandleFragmentCookiesAndNonce_NoceNull() {
        String methodName = "testHandleFragmentCookiesAndNonce_NoceNull";
        mockery.checking(new Expectations() {
            {
                never(response).addHeader(with(any(String.class)), with(any(String.class)));
            }
        });

        forwardRequest.setCspHeader(null);
        String js = forwardRequest.handleFragmentCookiesAndNonce(response);
        assertNotNull(js);
        assertFalse(js.contains("nonce="));
    }

}
