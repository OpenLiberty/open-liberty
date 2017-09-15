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
package com.ibm.ws.rest.handler.internal.servlet;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl;
import com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl.HandlerInfo;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTHandlerContainer;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

import test.common.junit.matchers.RegexMatcher;

/**
 *
 */
public class RESTProxyServletTest {

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final PrintWriter mockWriter = mock.mock(PrintWriter.class);
    private final HttpServletRequest request = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock.mock(HttpServletResponse.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<RESTHandlerContainer> containerRef = mock.mock(ServiceReference.class);
    private final RESTHandlerContainerImpl containerImpl = mock.mock(RESTHandlerContainerImpl.class);

    @SuppressWarnings("unchecked")
    private final Iterator<String> containerHandlers = mock.mock(Iterator.class);
    private final RESTHandler handler = mock.mock(RESTHandler.class);
    private final HandlerInfo handlerPair = mock.mock(HandlerInfo.class);

    private RESTProxyServlet servlet;

    @Before
    public void setup() throws UnsupportedEncodingException {
        mock.checking(new Expectations() {
            {
                one(request).setCharacterEncoding("UTF-8");
            }
        });
        servlet = new RESTProxyServlet();
    }

    @After
    public void tearDown() {
        servlet = null;
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.servlet.RESTProxyServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void service_noContainer() throws Exception {
        final HttpSession hs = mock.mock(HttpSession.class);
        final ServletContext sc = mock.mock(ServletContext.class);
        final BundleContext bc = mock.mock(BundleContext.class);
        mock.checking(new Expectations() {
            {
                one(request).getSession();
                will(returnValue(hs));

                one(hs).getServletContext();
                will(returnValue(sc));

                one(sc).getAttribute("osgi-bundlecontext");
                will(returnValue(bc));

                one(bc).getServiceReference(RESTHandlerContainer.class);
                will(returnValue(null));
            }
        });

        try {
            servlet.service(request, response);
            fail("Should have thrown a ServletException");
        } catch (ServletException e) {
            assertTrue("Thrown ServletException did not start with CWWKO1001E. Message: " + e.getMessage(),
                       e.getMessage().startsWith("CWWKO1001E"));
        }
    }

    /**
     * Set up the mock to return the container.
     */
    private void setContainerMock() {
        final HttpSession hs = mock.mock(HttpSession.class);
        final ServletContext sc = mock.mock(ServletContext.class);
        final BundleContext bc = mock.mock(BundleContext.class);
        mock.checking(new Expectations() {
            {
                one(request).getSession();
                will(returnValue(hs));

                one(hs).getServletContext();
                will(returnValue(sc));

                one(sc).getAttribute("osgi-bundlecontext");
                will(returnValue(bc));

                one(bc).getServiceReference(RESTHandlerContainer.class);
                will(returnValue(containerRef));

                one(bc).getService(containerRef);
                will(returnValue(containerImpl));
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.servlet.RESTProxyServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void service_postRootURL() throws Exception {
        setContainerMock();

        mock.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/"));

                one(request).getMethod();
                will(returnValue("POST"));

                one(response).sendError(405);
            }
        });

        servlet.service(request, response);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.servlet.RESTProxyServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void service_putRootURL() throws Exception {
        setContainerMock();

        mock.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/"));

                one(request).getMethod();
                will(returnValue("PUT"));

                one(response).sendError(405);
            }
        });

        servlet.service(request, response);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.servlet.RESTProxyServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void service_deleteRootURL() throws Exception {
        setContainerMock();

        mock.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/"));

                one(request).getMethod();
                will(returnValue("DELETE"));

                one(response).sendError(405);
            }
        });

        servlet.service(request, response);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.servlet.RESTProxyServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void service_getRootURLNoHandlers() throws Exception {
        setContainerMock();

        mock.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/"));

                one(request).getMethod();
                will(returnValue("GET"));

                one(response).setContentType("application/json");
                one(response).setCharacterEncoding("UTF-8");

                one(response).getWriter();
                will(returnValue(mockWriter));

                one(containerImpl).registeredKeys();
                will(returnValue(null));

                one(mockWriter).write("{\"version\":1,\"roots\":[]}");
            }
        });

        servlet.service(request, response);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.servlet.RESTProxyServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void service_getRootURLWithHandlers() throws Exception {
        setContainerMock();

        mock.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/"));

                one(request).getMethod();
                will(returnValue("GET"));

                one(response).setContentType("application/json");
                one(response).setCharacterEncoding("UTF-8");

                one(response).getWriter();
                will(returnValue(mockWriter));

                one(containerImpl).registeredKeys();
                will(returnValue(containerHandlers));

                one(containerHandlers).hasNext();
                will(returnValue(true));

                one(containerHandlers).next();
                will(returnValue("/handler"));

                one(containerHandlers).hasNext();
                will(returnValue(false));

                one(mockWriter).write("{\"version\":1,\"roots\":[\"/handler\"]}");
            }
        });

        servlet.service(request, response);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.servlet.RESTProxyServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void service_noMatchDelegate() throws Exception {
        setContainerMock();

        mock.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/handler"));

                allowing(request).getRequestURI();
                will(returnValue("/ibm/api/handler"));

                one(request).getMethod();
                will(returnValue("GET"));

                one(response).setContentType("application/json");

                one(response).getWriter();
                will(returnValue(mockWriter));

                one(containerImpl).handleRequest(with(any(RESTRequest.class)), with(any(RESTResponse.class)));

                one(containerImpl).getHandler("/handler");
                will(returnValue(null));

                one(response).sendError(with(404), with(new RegexMatcher("CWWKO1000E:.*/handler")));
            }
        });

        servlet.service(request, response);
    }
}
