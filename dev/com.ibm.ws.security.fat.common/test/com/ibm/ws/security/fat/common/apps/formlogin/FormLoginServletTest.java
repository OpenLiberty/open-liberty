/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.apps.formlogin;

import java.io.PrintWriter;
import java.security.Principal;

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

import test.common.SharedOutputManager;

public class FormLoginServletTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private final PrintWriter writer = mockery.mock(PrintWriter.class);
    private final Principal principal = mockery.mock(Principal.class);
    private final Cookie cookie = mockery.mock(Cookie.class);

    FormLoginServlet servlet = new FormLoginServlet();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        servlet = new FormLoginServlet();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /************************************** doGet **************************************/

    /**
     * Tests:
     * - Tests the basic information that is pulled out of requests sent to the FormLoginServlet
     */
    @Test
    public void test_doGet() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(response).getWriter();
                    will(returnValue(writer));
                    one(writer).println("ServletName: " + servlet.getClass().getSimpleName());
                    one(request).getRequestURL();
                    will(returnValue(new StringBuffer()));
                    one(request).getAuthType();
                    one(request).getRemoteUser();
                    allowing(request).getUserPrincipal();
                    will(returnValue(principal));
                    one(principal).getName();
                    allowing(request).isUserInRole(with(any(String.class)));
                    one(request).getParameter("role");
                    one(request).getCookies();
                    will(returnValue(new Cookie[] { cookie }));
                    one(cookie).getName();
                    one(cookie).getValue();
                    one(request).getParameter("logout");
                    one(request).logout();
                    one(writer).write(with(any(String.class)));
                    one(writer).flush();
                    one(writer).close();
                }
            });
            servlet.doGet(request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
