/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

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

public class PublicFacingErrorServletTest extends CommonTestClass {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private final PrintWriter writer = mockery.mock(PrintWriter.class);

    PublicFacingErrorServlet servlet = new PublicFacingErrorServlet();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        servlet = new PublicFacingErrorServlet();
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

    @Test
    public void test_doGet() {
        try {
            writeErrorExpectations();
            servlet.doGet(request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_doPost() {
        try {
            writeErrorExpectations();
            servlet.doPost(request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private void writeErrorExpectations() throws IOException {
        final String localeStr = "en";
        final Locale locale = new Locale(localeStr);
        mockery.checking(new Expectations() {
            {
                one(response).getWriter();
                will(returnValue(writer));
                one(request).getLocale();
                will(returnValue(locale));
                one(writer).print("CWOAU0073E: An authentication error occurred. Try closing the web browser and authenticating again, or contact the site administrator if the problem persists.");
                one(writer).close();
            }
        });
    }

}