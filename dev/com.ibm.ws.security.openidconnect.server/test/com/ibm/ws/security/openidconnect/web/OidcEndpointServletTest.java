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
package com.ibm.ws.security.openidconnect.web;

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
import io.openliberty.security.openidconnect.web.OidcSupportedHttpMethodHandler;
import test.common.SharedOutputManager;

/*
 *
 */

@SuppressWarnings("restriction")
public class OidcEndpointServletTest {
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
    private final ServiceReference<OidcEndpointServices> sr = context.mock(ServiceReference.class);
    private final OidcEndpointServices oes = context.mock(OidcEndpointServices.class);
    private final OidcSupportedHttpMethodHandler supportedHttpMethodHandler = context.mock(OidcSupportedHttpMethodHandler.class);

    @SuppressWarnings("serial")
    private class MockedOidcEndpointServlet extends OidcEndpointServlet {
        @Override
        OidcSupportedHttpMethodHandler getOidcSupportedHttpMethodHandler(HttpServletRequest request, HttpServletResponse response) {
            return supportedHttpMethodHandler;
        }
    };

    @Before
    public void setUp() {
    }

    /*
     * test doGet with valid params.
     * expect result is no error.
     */
    @Test
    public void testDoGetValid() {

        try {
            context.checking(new Expectations() {
                {
                    allowing(scfg).getServletContext();
                    will(returnValue(sc));
                    allowing(sc).getAttribute("osgi-bundlecontext");
                    will(returnValue(bc));
                    one(bc).getService(sr);
                    will(returnValue(oes));
                    allowing(bc).getServiceReference(OidcEndpointServices.class);
                    will(returnValue(sr));
                    one(supportedHttpMethodHandler).isValidHttpMethodForRequest(HttpMethod.GET);
                    will(returnValue(true));
                    one(oes).handleOidcRequest(request, response, sc);
                }
            });

            OidcEndpointServlet oeservlet = new MockedOidcEndpointServlet();
            oeservlet.init(scfg);
            oeservlet.init();
            oeservlet.doGet(request, response);
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
                    allowing(bc).getServiceReference(OidcEndpointServices.class);
                    will(returnValue(null));
                    one(supportedHttpMethodHandler).isValidHttpMethodForRequest(HttpMethod.GET);
                    will(returnValue(true));
                    one(oes).handleOidcRequest(request, response, sc);
                }
            });

            OidcEndpointServlet oeservlet = new MockedOidcEndpointServlet();
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