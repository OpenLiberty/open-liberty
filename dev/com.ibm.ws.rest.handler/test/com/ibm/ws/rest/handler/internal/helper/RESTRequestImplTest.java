/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.internal.helper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl;
import com.ibm.wsspi.rest.handler.RESTRequest;

/**
 *
 */
public class RESTRequestImplTest {
    private final Mockery mock = new JUnit4Mockery();
    private final HttpServletRequest httpRequest = mock.mock(HttpServletRequest.class);

    private RESTRequest restRequest = null;

    @Before
    public void setup() throws UnsupportedEncodingException
    {
        mock.checking(new Expectations() {
            {
                one(httpRequest).setCharacterEncoding("UTF-8");
            }
        });
        restRequest = new ServletRESTRequestImpl(httpRequest);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl#getInput()}.
     */
    @Test
    public void getInput() throws Exception {
        mock.checking(new Expectations() {
            {
                one(httpRequest).getReader();
                will(returnValue(null));
            }
        });

        assertNull("FAIL: the mock was supposed to return null, and we should get that back",
                   restRequest.getInput());
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.helper.RESTRequestImpl#getInputStream()}.
     */
    @Test
    public void getInputStream() throws Exception {
        mock.checking(new Expectations() {
            {
                one(httpRequest).getInputStream();
                will(returnValue(null));
            }
        });

        assertNull("FAIL: the mock was supposed to return null, and we should get that back",
                   restRequest.getInputStream());
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl#getHeader(java.lang.String)}.
     */
    @Test
    public void getHeader() throws Exception {
        mock.checking(new Expectations() {
            {
                one(httpRequest).getHeader("testHeader");
                will(returnValue("mockedValue"));
            }
        });

        assertEquals("FAIL: did not get expected (mocked) header value",
                     "mockedValue", restRequest.getHeader("testHeader"));
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl#getMethod()}.
     */
    @Test
    public void getMethod() throws Exception {
        mock.checking(new Expectations() {
            {
                one(httpRequest).getMethod();
                will(returnValue("GET"));
            }
        });

        assertEquals("FAIL: did not get expected (mocked) method",
                     "GET", restRequest.getMethod());
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl#getCompleteURL()}.
     */
    @Test
    public void getCompleteURL_noQueryParams() throws Exception {
        mock.checking(new Expectations() {
            {
                one(httpRequest).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/ibm/api/myRoot/myPath")));

                one(httpRequest).getQueryString();
                will(returnValue(null));
            }
        });

        assertEquals("FAIL: did not get expected (mocked) URL",
                     "https://localhost:9443/ibm/api/myRoot/myPath", restRequest.getCompleteURL());
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl#getCompleteURL()}.
     */
    @Test
    public void getCompleteURL_emptyQueryParams() throws Exception {
        mock.checking(new Expectations() {
            {
                one(httpRequest).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/ibm/api/myRoot/myPath")));

                one(httpRequest).getQueryString();
                will(returnValue(""));
            }
        });

        assertEquals("FAIL: did not get expected (mocked) URL",
                     "https://localhost:9443/ibm/api/myRoot/myPath", restRequest.getCompleteURL());
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl#getCompleteURL()}.
     */
    @Test
    public void getCompleteURL_withQueryParams() throws Exception {
        mock.checking(new Expectations() {
            {
                one(httpRequest).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/ibm/api/myRoot/myPath")));

                one(httpRequest).getQueryString();
                will(returnValue("param1=value1,param2=value2"));
            }
        });

        assertEquals("FAIL: did not get expected (mocked) complete URL",
                     "https://localhost:9443/ibm/api/myRoot/myPath?param1=value1,param2=value2", restRequest.getCompleteURL());
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl#getURL()}.
     */
    @Test
    public void getURL() {
        mock.checking(new Expectations() {
            {
                one(httpRequest).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/ibm/api/myRoot/myPath")));
            }
        });

        assertEquals("FAIL: did not get expected (mocked) URL",
                     "https://localhost:9443/ibm/api/myRoot/myPath", restRequest.getURL());
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl#getURI()}.
     */
    @Test
    public void getURI() {
        mock.checking(new Expectations() {
            {
                one(httpRequest).getRequestURI();
                will(returnValue("/ibm/api/myRoot/myPath"));
            }
        });

        assertEquals("FAIL: did not get expected (mocked) URI",
                     "/ibm/api/myRoot/myPath", restRequest.getURI());
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl#getPath()}.
     */
    @Test
    public void getPath() {
        mock.checking(new Expectations() {
            {
                one(httpRequest).getPathInfo();
                will(returnValue("/myRoot/myPath"));
            }
        });

        assertEquals("FAIL: did not get expected (mocked) path",
                     "/myRoot/myPath", restRequest.getPath());
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl#getQueryString()}.
     */
    @Test
    public void getQueryString() {
        mock.checking(new Expectations() {
            {
                one(httpRequest).getQueryString();
                will(returnValue("param1=value1,param2=value2"));
            }
        });

        assertEquals("FAIL: did not get expected (mocked) query string",
                     "param1=value1,param2=value2", restRequest.getQueryString());
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl#getParameter(java.lang.String)}.
     */
    @Test
    public void getParameter() throws Exception {
        mock.checking(new Expectations() {
            {
                one(httpRequest).getParameter("testParam");
                will(returnValue("mockedValue"));
            }
        });

        assertEquals("FAIL: did not get expected (mocked) parameter value",
                     "mockedValue", restRequest.getParameter("testParam"));
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl#getParameterValues(java.lang.String)}.
     */
    @Test
    public void getParameterValues() throws Exception {
        mock.checking(new Expectations() {
            {
                one(httpRequest).getParameterValues("testParam");
                will(returnValue(new String[] { "mockedValue" }));
            }
        });

        assertArrayEquals("FAIL: did not get expected (mocked) parameter value",
                          new String[] { "mockedValue" }, restRequest.getParameterValues("testParam"));
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl#getParameterMap()}.
     */
    @Test
    public void getParameterMap() throws Exception {
        final Map<String, String[]> map = new HashMap<String, String[]>();
        mock.checking(new Expectations() {
            {
                one(httpRequest).getParameterMap();
                will(returnValue(map));
            }
        });

        assertSame("FAIL: did not get back the expected (mock) parameter map",
                   map, restRequest.getParameterMap());
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl#getUserPrincipal()}.
     */
    @Test
    public void getUserPrincipal() throws Exception {
        final Principal principal = mock.mock(Principal.class);
        mock.checking(new Expectations() {
            {
                one(httpRequest).getUserPrincipal();
                will(returnValue(principal));
            }
        });

        assertSame("FAIL: did not get expected (mocked) user principal",
                   principal, restRequest.getUserPrincipal());
    }

}
