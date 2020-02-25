/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.web;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;

import test.common.SharedOutputManager;

/**
 * Unit test the {@link com.ibm.ws.security.saml.sso20.web.EndpointServlet} class.
 */
public class EndpointServletTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    public final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @Rule
    public TestName name = new TestName();

    //Mock objects
    private final HttpServletRequest servletRequest = mockery.mock(HttpServletRequest.class, "servletRequest");
    private final HttpServletResponse servletResponse = mockery.mock(HttpServletResponse.class, "servletResponse");

    private final ServletConfig servletConfig = mockery.mock(ServletConfig.class, "servletConfig");
    private final ServletContext servletContext = mockery.mock(ServletContext.class, "servletContext");
    private final BundleContext bundleContext = mockery.mock(BundleContext.class, "bundleContext");

    @SuppressWarnings("unchecked")
    private final ServiceReference<EndpointServices> serviceReference = mockery.mock(ServiceReference.class);
    private final SsoRequest ssoRequest = mockery.mock(SsoRequest.class, "ssoRequest");
    private final EndpointServices endpointServices = mockery.mock(EndpointServices.class, "endpointServices");

    //Object Under Test
    private static EndpointServlet endpointServlet = new EndpointServlet();
    private static final String OSGI = "osgi-bundlecontext";

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
    }

    @AfterClass
    public static void tearDownClass() {
        outputMgr.trace("*=all=disabled");
    }

    @Before
    public void setup() {
        mockery.checking(new Expectations() {
            {
                allowing(servletRequest).getAttribute(Constants.ATTRIBUTE_SAML20_REQUEST);
                will(returnValue(ssoRequest));

                allowing(servletConfig).getServletContext();
                will(returnValue(servletContext));

                allowing(servletContext).getAttribute(OSGI);
                will(returnValue(bundleContext));
            }
        });
    }

    @After
    public void tearDown() {
        //Cleaning up the OUT
        endpointServlet = new EndpointServlet();
        mockery.assertIsSatisfied();
    }

    /**
     * Test that {@link com.ibm.ws.security.saml.sso20.web.EndpointServlet#doGet(HttpServletRequest, HttpServletResponse)}
     * sends a 500 if the endpoint type of the saml
     * request is not {@link com.ibm.ws.security.saml.Constants.EndpointType#SAMLMETADATA}
     */
    @Test
    public void doGetMethodShouldThrowServletExceptionIfSamlRequestEndpointTypeIsNotMetadata() throws ServletException, IOException {
        mockery.checking(new Expectations() {
            {
                allowing(servletResponse).sendError(500, "INTERNAL ERROR");

                allowing(ssoRequest).getType();
                will(returnValue(null));
                /*
                 * one(ssoRequest).getType();
                 * will(returnValue(null));
                 * one(ssoRequest).getType();
                 * will(returnValue(null));
                 */
            }
        });

        endpointServlet.doGet(servletRequest, servletResponse);
    }

    /**
     * Test that {@link com.ibm.ws.security.saml.sso20.web.EndpointServlet#doPost(HttpServletRequest, HttpServletResponse)} handle a saml exception.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void doPostShouldHandleSamlException() {
        try {
            //Initializing endpointServlet
            mockery.checking(new Expectations() {
                {
                    one(bundleContext).getServiceReference(with(any(Class.class)));
                    will(returnValue(serviceReference));
                }
            });

            endpointServlet.init(servletConfig);

            //Calling doGet method an passing with no failures
            mockery.checking(new Expectations() {
                {
                    one(ssoRequest).getType();
                    will(returnValue(Constants.EndpointType.SAMLMETADATA));

                    //Expectations for the doPost call
                    one(bundleContext).getService(serviceReference);
                    will(returnValue(endpointServices));

                    one(endpointServices).handleSamlRequest(servletRequest, servletResponse);
                    will(throwException(new SamlException("saml exception")));

                    one(servletResponse).setStatus(403);

                    one(ssoRequest).getSsoSamlService();
                    will(returnValue(null));
                    one(ssoRequest).getSsoConfig();
                    will(returnValue(null));

                    one(servletRequest).getServerName();
                    will(returnValue("SERVER"));
                    one(servletRequest).isSecure();
                    will(returnValue(false));

                    one(servletResponse).sendRedirect(with(any(String.class)));
                }
            });

            endpointServlet.doGet(servletRequest, servletResponse);
        } catch (Exception e) {
            e.printStackTrace();
            fail(SamlUtil.dumpStackTrace(e, 6).toString());
        }
    }

    /**
     * Test that {@link com.ibm.ws.security.saml.sso20.web.EndpointServlet#doPost(HttpServletRequest, HttpServletResponse)} handle a saml request with no failures.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void doPostShouldHandleSamlRequest() {
        try {

            //Initializing endpointServlet
            mockery.checking(new Expectations() {
                {
                    one(bundleContext).getServiceReference(with(any(Class.class)));
                    will(returnValue(serviceReference));
                }
            });

            endpointServlet.init(servletConfig);

            //Calling doGet method an passing with no failures
            mockery.checking(new Expectations() {
                {
                    one(ssoRequest).getType();
                    will(returnValue(Constants.EndpointType.SAMLMETADATA));

                    //Expectations for the doPost call
                    one(bundleContext).getService(serviceReference);
                    will(returnValue(endpointServices));

                    one(endpointServices).handleSamlRequest(servletRequest, servletResponse);

                }
            });

            endpointServlet.doGet(servletRequest, servletResponse);
        } catch (Exception e) {
            e.printStackTrace();
            fail(SamlUtil.dumpStackTrace(e, 6).toString());
        }

    }

    /**
     * Test that getSamlEndpointServices() invoke the {@link com.ibm.ws.security.saml.sso20.web.EndpointServlet#init()} method and get the EndpointServices from a
     * bundleContext.after retrieving a null EndpointServices from the endpointServicesRef variable.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getSamlEndpointServicesShouldInvokeInitMethodAndRetrieveAgainAnEndpointServiceAfterGettingANullEnpointService() {

        try {
            //Defining endpointServlet one time before invoking doGet method and once when getSamlEndpointServices will be invoke it
            mockery.checking(new Expectations() {
                {
                    atMost(2).of(bundleContext).getServiceReference(with(any(Class.class)));
                    will(returnValue(serviceReference));
                }
            });

            endpointServlet.init(servletConfig);

            //Calling doGet method an passing with no failures
            mockery.checking(new Expectations() {
                {
                    one(ssoRequest).getType();
                    will(returnValue(Constants.EndpointType.SAMLMETADATA));

                    //Expectations for the doPost call
                    one(bundleContext).getService(serviceReference);
                    will(returnValue(null));

                    one(bundleContext).getService(serviceReference);
                    will(returnValue(endpointServices));

                    one(endpointServices).handleSamlRequest(servletRequest, servletResponse);
                }
            });

            endpointServlet.doGet(servletRequest, servletResponse);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }

    /**
     * Test that getSamlEndpointServices() throw a {@link javax.servlet.ServletException} if the endpointServices variable is null at the end of the execution of the method.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getSamlEndpointServicesShouldThrowServletExceptionIfEndPointServicesIsNull() throws ServletException, IOException {

        //Initializing endpointServlet
        mockery.checking(new Expectations() {
            {
                one(bundleContext).getServiceReference(with(any(Class.class)));
                will(returnValue(null));
            }
        });

        endpointServlet.init(servletConfig);

        //Calling doGet method
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).getType();
                will(returnValue(Constants.EndpointType.SAMLMETADATA));
                one(servletResponse).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL ERROR");
            }
        });

        endpointServlet.doGet(servletRequest, servletResponse);
    }
}
