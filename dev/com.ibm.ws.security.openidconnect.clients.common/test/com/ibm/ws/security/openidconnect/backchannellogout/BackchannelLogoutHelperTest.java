/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.backchannellogout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class BackchannelLogoutHelperTest extends CommonTestClass {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.openidconnect.common.*=all=enabled");

    private final String CWWKS1542E_BACKCHANNEL_LOGOUT_REQUEST_MISSING_PARAMETER = "CWWKS1542E";
    private final String REQUEST_URI = "/oidcclient/backchannel_logout/myClient";

    final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    final ConvergedClientConfig clientConfig = mockery.mock(ConvergedClientConfig.class);

    BackchannelLogoutHelper helper;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
        System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "true");
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        helper = new BackchannelLogoutHelper(request, response, clientConfig);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        System.clearProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY);
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_handleBackchannelLogoutRequest_error() {
        mockery.checking(new Expectations() {
            {
                one(request).getMethod();
                will(returnValue("POST"));
                one(request).getParameter(BackchannelLogoutHelper.LOGOUT_TOKEN_PARAM_NAME);
                will(returnValue(null));
                one(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
                one(request).getRequestURI();
                will(returnValue(REQUEST_URI));
                one(response).setHeader("Cache-Control", "no-cache, no-store");
                one(response).setHeader("Pragma", "no-cache");
            }
        });
        helper.handleBackchannelLogoutRequest();
    }

    @Test
    public void test_validateRequestAndGetLogoutTokenParameter_httpMethodNotPost() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getMethod();
                    will(returnValue("GET"));
                }
            });
            String token = helper.validateRequestAndGetLogoutTokenParameter();
            fail("Should have thrown an exception but didn't. Got token: [" + token + "].");
        } catch (BackchannelLogoutException e) {
            assertEquals("Did not get the expected status code in the response. Error message: " + e.getMessage(), HttpServletResponse.SC_METHOD_NOT_ALLOWED, e.getResponseCode());
        }
    }

    @Test
    public void test_validateRequestAndGetLogoutTokenParameter_missingLogoutTokenParameter() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getMethod();
                    will(returnValue("POST"));
                    one(request).getParameter(BackchannelLogoutHelper.LOGOUT_TOKEN_PARAM_NAME);
                    will(returnValue(null));
                }
            });
            String token = helper.validateRequestAndGetLogoutTokenParameter();
            fail("Should have thrown an exception but didn't. Got token: [" + token + "].");
        } catch (BackchannelLogoutException e) {
            assertEquals("Did not get the expected status code in the response. Error message: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST, e.getResponseCode());
            verifyException(e, CWWKS1542E_BACKCHANNEL_LOGOUT_REQUEST_MISSING_PARAMETER);
        }
    }

    @Test
    public void test_validateRequestAndGetLogoutTokenParameter_emptyLogoutTokenParameter() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getMethod();
                    will(returnValue("POST"));
                    one(request).getParameter(BackchannelLogoutHelper.LOGOUT_TOKEN_PARAM_NAME);
                    will(returnValue(""));
                }
            });
            String token = helper.validateRequestAndGetLogoutTokenParameter();
            fail("Should have thrown an exception but didn't. Got token: [" + token + "].");
        } catch (BackchannelLogoutException e) {
            assertEquals("Did not get the expected status code in the response. Error message: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST, e.getResponseCode());
            verifyException(e, CWWKS1542E_BACKCHANNEL_LOGOUT_REQUEST_MISSING_PARAMETER);
        }
    }

}
