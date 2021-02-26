/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import static org.junit.Assert.fail;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import io.openliberty.security.common.http.SupportedHttpMethodHandler.HttpMethod;
import io.openliberty.security.oauth20.web.OAuthSupportedHttpMethodHandler;
import test.common.SharedOutputManager;

/*
 *
 */

@SuppressWarnings("restriction")
public class OAuth20EndpointServletTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;
    private final Mockery context = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final HttpServletRequest request = context.mock(HttpServletRequest.class);
    private final HttpServletResponse response = context.mock(HttpServletResponse.class);
    private final ServletConfig scfg = context.mock(ServletConfig.class);
    private final ServletContext sc = context.mock(ServletContext.class);
    private final BundleContext bc = context.mock(BundleContext.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<OAuth20EndpointServices> sr = context.mock(ServiceReference.class);
    private final OAuth20EndpointServices oes = context.mock(OAuth20EndpointServices.class);
    private final OAuthSupportedHttpMethodHandler supportedHttpMethodHandler = context.mock(OAuthSupportedHttpMethodHandler.class);

    @SuppressWarnings("serial")
    private class MockedOAuth20EndpointServlet extends OAuth20EndpointServlet {
        @Override
        OAuthSupportedHttpMethodHandler getOAuthSupportedHttpMethodHandler(HttpServletRequest request, HttpServletResponse response) {
            return supportedHttpMethodHandler;
        }
    };

    @Before
    public void setUp() {
    }

    private void setupNormalExpectations() throws Exception {
        context.checking(new Expectations() {
            {
                allowing(scfg).getServletContext();
                will(returnValue(sc));
                allowing(sc).getAttribute("osgi-bundlecontext");
                will(returnValue(bc));
                one(bc).getService(sr);
                will(returnValue(oes));
                allowing(bc).getServiceReference(OAuth20EndpointServices.class);
                will(returnValue(sr));
                one(oes).handleOAuthRequest(request, response, sc);
            }
        });
    }

    /*
     * test doGet with valid params.
     * expect result is no error.
     */
    @Test
    public void testDoGetValid() {

        try {
            setupNormalExpectations();
            OAuth20EndpointServlet oeservlet = new MockedOAuth20EndpointServlet();
            context.checking(new Expectations() {
                {
                    one(supportedHttpMethodHandler).isValidHttpMethodForRequest(HttpMethod.GET);
                    will(returnValue(true));
                }
            });
            oeservlet.init(scfg);
            oeservlet.init();
            oeservlet.doGet(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception is caught");
        }
    }

    /*
     * test doHead with valid params.
     * expect result is no error.
     */
    @Test
    public void testDoHeadValid() {

        try {
            setupNormalExpectations();
            OAuth20EndpointServlet oeservlet = new MockedOAuth20EndpointServlet();
            context.checking(new Expectations() {
                {
                    one(supportedHttpMethodHandler).isValidHttpMethodForRequest(HttpMethod.HEAD);
                    will(returnValue(true));
                }
            });
            oeservlet.init(scfg);
            oeservlet.init();
            oeservlet.doHead(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception is caught");
        }
    }

    /*
     * test doDelete with valid params.
     * expect result is no error.
     */
    @Test
    public void testDoDeleteValid() {

        try {
            OAuth20EndpointServlet oeservlet = new MockedOAuth20EndpointServlet();
            context.checking(new Expectations() {
                {
                    one(supportedHttpMethodHandler).isValidHttpMethodForRequest(HttpMethod.DELETE);
                    will(returnValue(true));
                }
            });
            setupNormalExpectations();
            oeservlet.init(scfg);
            oeservlet.init();
            oeservlet.doDelete(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception is caught");
        }
    }

    /*
     * test doPut with valid params.
     * expect result is no error.
     */
    @Test
    public void testDoPutValid() {

        try {
            setupNormalExpectations();
            OAuth20EndpointServlet oeservlet = new MockedOAuth20EndpointServlet();
            context.checking(new Expectations() {
                {
                    one(supportedHttpMethodHandler).isValidHttpMethodForRequest(HttpMethod.PUT);
                    will(returnValue(true));
                }
            });
            oeservlet.init(scfg);
            oeservlet.init();
            oeservlet.doPut(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception is caught");
        }
    }

    /*
     * test doGet with null oidcEndPointServiceRef.
     * expect result is ServletException.
     */
    @Test
    public void testDoGetNullOepsr() {

        try {
            context.checking(new Expectations() {
                {
                    allowing(scfg).getServletContext();
                    will(returnValue(sc));
                    allowing(sc).getAttribute("osgi-bundlecontext");
                    will(returnValue(bc));
                    one(bc).getService(sr);
                    will(returnValue(null));
                    allowing(bc).getServiceReference(OAuth20EndpointServices.class);
                    will(returnValue(null));
                    one(oes).handleOAuthRequest(request, response, sc);
                }
            });

            OAuth20EndpointServlet oeservlet = new MockedOAuth20EndpointServlet();
            context.checking(new Expectations() {
                {
                    one(supportedHttpMethodHandler).isValidHttpMethodForRequest(HttpMethod.GET);
                    will(returnValue(true));
                }
            });
            oeservlet.init(scfg);
            oeservlet.init();
            oeservlet.doGet(request, response);
            fail("A ServletException is not thrown");
        } catch (ServletException se) {
            // this is an expected result.
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception is caught");
        }
    }

}