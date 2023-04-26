/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.jwt.utils;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;

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

import test.common.SharedOutputManager;

public class IssuerUtilsTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String REQ_CONTEXT_PATH = "/myCtxPath";
    private static final String REQ_SERVLET_PATH = "/myServletPath";

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
    }

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    protected final HttpServletRequest request = mock.mock(HttpServletRequest.class, "request");

    @Test
    public void test_getCalculatedIssuerIdFromRequest_defaultInsecurePort() {
        mock.checking(new Expectations() {
            {
                allowing(request).getScheme();
                will(returnValue("http"));
                allowing(request).getServerName();
                will(returnValue("localhost"));
                allowing(request).getServerPort();
                will(returnValue(80));
                allowing(request).getContextPath();
                will(returnValue(REQ_CONTEXT_PATH));
                allowing(request).getServletPath();
                will(returnValue(REQ_SERVLET_PATH));
                allowing(request).getPathInfo();
                will(returnValue(null));
            }
        });
        String expectedPath = "http://localhost" + REQ_CONTEXT_PATH + REQ_SERVLET_PATH;

        String calculatedPath = IssuerUtils.getCalculatedIssuerIdFromRequest(request);

        assertEquals("did not get the expected full context servlet path", expectedPath, calculatedPath);
    }

    @Test
    public void test_getCalculatedIssuerIdFromRequest_customInsecurePort() {
        mock.checking(new Expectations() {
            {
                allowing(request).getScheme();
                will(returnValue("http"));
                allowing(request).getServerName();
                will(returnValue("localhost"));
                allowing(request).getServerPort();
                will(returnValue(81));
                allowing(request).getContextPath();
                will(returnValue(REQ_CONTEXT_PATH));
                allowing(request).getServletPath();
                will(returnValue(REQ_SERVLET_PATH));
                allowing(request).getPathInfo();
                will(returnValue(null));
            }
        });
        String expectedPath = "http://localhost:81" + REQ_CONTEXT_PATH + REQ_SERVLET_PATH;

        String calculatedPath = IssuerUtils.getCalculatedIssuerIdFromRequest(request);

        assertEquals("did not get the expected full context servlet path", expectedPath, calculatedPath);
    }

    @Test
    public void test_getCalculatedIssuerIdFromRequest_defaultSecurePort() {
        mock.checking(new Expectations() {
            {
                allowing(request).getScheme();
                will(returnValue("https"));
                allowing(request).getServerName();
                will(returnValue("localhost"));
                allowing(request).getServerPort();
                will(returnValue(443));
                allowing(request).getContextPath();
                will(returnValue(REQ_CONTEXT_PATH));
                allowing(request).getServletPath();
                will(returnValue(REQ_SERVLET_PATH));
                allowing(request).getPathInfo();
                will(returnValue(null));
            }
        });
        String expectedPath = "https://localhost" + REQ_CONTEXT_PATH + REQ_SERVLET_PATH;

        String calculatedPath = IssuerUtils.getCalculatedIssuerIdFromRequest(request);

        assertEquals("did not get the expected full context servlet path", expectedPath, calculatedPath);
    }

    @Test
    public void test_getCalculatedIssuerIdFromRequest_customSecurePort() {
        mock.checking(new Expectations() {
            {
                allowing(request).getScheme();
                will(returnValue("https"));
                allowing(request).getServerName();
                will(returnValue("localhost"));
                allowing(request).getServerPort();
                will(returnValue(444));
                allowing(request).getContextPath();
                will(returnValue(REQ_CONTEXT_PATH));
                allowing(request).getServletPath();
                will(returnValue(REQ_SERVLET_PATH));
                allowing(request).getPathInfo();
                will(returnValue(null));
            }
        });
        String expectedPath = "https://localhost:444" + REQ_CONTEXT_PATH + REQ_SERVLET_PATH;

        String calculatedPath = IssuerUtils.getCalculatedIssuerIdFromRequest(request);

        assertEquals("did not get the expected full context servlet path", expectedPath, calculatedPath);
    }

    @Test
    public void test_getCalculatedIssuerIdFromRequest_emptyPathInfo() {
        mock.checking(new Expectations() {
            {
                allowing(request).getScheme();
                will(returnValue("https"));
                allowing(request).getServerName();
                will(returnValue("localhost"));
                allowing(request).getServerPort();
                will(returnValue(443));
                allowing(request).getContextPath();
                will(returnValue(REQ_CONTEXT_PATH));
                allowing(request).getServletPath();
                will(returnValue(REQ_SERVLET_PATH));
                allowing(request).getPathInfo();
                will(returnValue(""));
            }
        });
        String expectedPath = "https://localhost" + REQ_CONTEXT_PATH + REQ_SERVLET_PATH;

        String calculatedPath = IssuerUtils.getCalculatedIssuerIdFromRequest(request);

        assertEquals("did not get the expected full context servlet path", expectedPath, calculatedPath);
    }

    @Test
    public void test_getCalculatedIssuerIdFromRequest_simplePathInfo() {
        final String initialPath = "/path";
        mock.checking(new Expectations() {
            {
                allowing(request).getScheme();
                will(returnValue("https"));
                allowing(request).getServerName();
                will(returnValue("localhost"));
                allowing(request).getServerPort();
                will(returnValue(443));
                allowing(request).getContextPath();
                will(returnValue(REQ_CONTEXT_PATH));
                allowing(request).getServletPath();
                will(returnValue(REQ_SERVLET_PATH));
                allowing(request).getPathInfo();
                will(returnValue(initialPath));
            }
        });
        String expectedPath = "https://localhost" + REQ_CONTEXT_PATH + REQ_SERVLET_PATH + initialPath;

        String calculatedPath = IssuerUtils.getCalculatedIssuerIdFromRequest(request);

        assertEquals("did not get the expected full context servlet path", expectedPath, calculatedPath);
    }

    @Test
    public void test_getCalculatedIssuerIdFromRequest_extendedPathInfo() {
        final String initialPath = "/path";
        mock.checking(new Expectations() {
            {
                allowing(request).getScheme();
                will(returnValue("https"));
                allowing(request).getServerName();
                will(returnValue("localhost"));
                allowing(request).getServerPort();
                will(returnValue(443));
                allowing(request).getContextPath();
                will(returnValue(REQ_CONTEXT_PATH));
                allowing(request).getServletPath();
                will(returnValue(REQ_SERVLET_PATH));
                allowing(request).getPathInfo();
                will(returnValue(initialPath + "/info/that/is/extra"));
            }
        });
        String expectedPath = "https://localhost" + REQ_CONTEXT_PATH + REQ_SERVLET_PATH + initialPath;

        String calculatedPath = IssuerUtils.getCalculatedIssuerIdFromRequest(request);

        assertEquals("did not get the expected full context servlet path", expectedPath, calculatedPath);
    }

}
