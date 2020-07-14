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
package io.openliberty.security.common.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.common.http.SupportedHttpMethodHandler.HttpMethod;
import test.common.SharedOutputManager;

public class SupportedHttpMethodHandlerTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.common.*=all");

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);

    private SupportedHttpMethodHandler handler;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        handler = new SupportedHttpMethodHandler(request, response);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_isValidHttpMethodForRequest() throws IOException {
        assertTrue("Default behavior should be to allow all HTTP methods for request.", handler.isValidHttpMethodForRequest(HttpMethod.GET));
    }

    @Test
    public void test_sendUnsupportedMethodResponse() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        });
        handler.sendUnsupportedMethodResponse();
    }

    @Test
    public void test_setAllowHeaderAndSendResponse_nullSupportedMethods() {
        mockery.checking(new Expectations() {
            {
                one(response).setStatus(HttpServletResponse.SC_OK);
            }
        });
        handler.setAllowHeaderAndSendResponse(null);
    }

    @Test
    public void test_setAllowHeaderAndSendResponse_noSupportedMethods() {
        Set<HttpMethod> supportedMethods = new HashSet<HttpMethod>();
        mockery.checking(new Expectations() {
            {
                one(response).setHeader("Allow", "");
                one(response).setStatus(HttpServletResponse.SC_OK);
            }
        });
        handler.setAllowHeaderAndSendResponse(supportedMethods);
    }

    @Test
    public void test_setAllowHeaderAndSendResponse() {
        Set<HttpMethod> supportedMethods = new HashSet<HttpMethod>();
        supportedMethods.add(HttpMethod.GET);
        mockery.checking(new Expectations() {
            {
                one(response).setHeader("Allow", "GET");
                one(response).setStatus(HttpServletResponse.SC_OK);
            }
        });
        handler.setAllowHeaderAndSendResponse(supportedMethods);
    }

    @Test
    public void test_buildHeaderValue_nullInput() {
        String result = handler.buildHeaderValue(null);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_buildHeaderValue_emptySet() {
        Set<HttpMethod> input = new HashSet<HttpMethod>();
        String result = handler.buildHeaderValue(input);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Header value should have been an empty string but was [" + result + "].", result.isEmpty());
    }

    @Test
    public void test_buildHeaderValue_singleEntry() {
        Set<HttpMethod> input = new HashSet<HttpMethod>();
        input.add(HttpMethod.GET);
        String result = handler.buildHeaderValue(input);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Header value did not match the expected value.", "GET", result);
    }

    @Test
    public void test_buildHeaderValue_multipleEntries() {
        Set<HttpMethod> input = new HashSet<HttpMethod>();
        input.add(HttpMethod.GET);
        input.add(HttpMethod.HEAD);
        input.add(HttpMethod.POST);
        String result = handler.buildHeaderValue(input);
        assertNotNull("Result should not have been null but was.", result);
        String possibleValue1 = "GET, HEAD, POST";
        String possibleValue2 = "GET, POST, HEAD";
        String possibleValue3 = "HEAD, GET, POST";
        String possibleValue4 = "HEAD, POST, GET";
        String possibleValue5 = "POST, HEAD, GET";
        String possibleValue6 = "POST, GET, HEAD";
        assertTrue("Header value did not match one of the expected values. Result was: [" + result + "]",
                possibleValue1.equals(result) || possibleValue2.equals(result) || possibleValue3.equals(result) || possibleValue4.equals(result) || possibleValue5.equals(result) || possibleValue6.equals(result));
    }

}