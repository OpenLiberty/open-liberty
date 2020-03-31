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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.impl.HandlerFactory;
import com.ibm.ws.security.saml.sso20.acs.AcsHandler;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 * Unit test the {@link com.ibm.ws.security.saml.sso20.web.EndpointServices} class.
 */
public class EndpointServicesTest {

    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @SuppressWarnings("unchecked")
    private static final ServiceReference<SecurityService> SECURITY_SERVICE_MCK = mockery.mock(ServiceReference.class, "securityService");

    @SuppressWarnings("unchecked")
    private static final ServiceReference<SsoSamlService> SERVICE_REFERENCE_SSO_SERVICE_MCK = mockery.mock(ServiceReference.class);

    private static final ComponentContext COMPONENT_CONTEXT_MCK = mockery.mock(ComponentContext.class);
    private static final IExtendedRequest HTTP_SERVLET_REQUEST_MCK = mockery.mock(IExtendedRequest.class);
    private static final HttpServletResponse HTTP_SERVLET_RESPONSE_MCK = mockery.mock(HttpServletResponse.class);
    private static final SsoRequest SSO_REQUEST_MCK = mockery.mock(SsoRequest.class);
    private static final SsoSamlService SSO_SERVICE_MCK = mockery.mock(SsoSamlService.class);
    private static final AcsHandler ACS_HANDLER_MCK = mockery.mock(AcsHandler.class);
    //private static final StringBuffer sb = mockery.mock(StringBuffer.class, "sb");
    //private static final CommonMockObjects common = new CommonMockObjects();
    private static final SsoConfig ssoConfig = mockery.mock(SsoConfig.class);;
    private static final String SSO_SERVICE = "simpleService";
    private static final Long SSO_SERVICE_ID = 9999L;
    private static final Long SSO_SERVICE_RANKING = 0L;

    private static final String TEST_URL = "https://localhost:8010/formlogin/SimpleServlet";

    private EndpointServices endpointServices;

    /**
     * This setup an EndpointServices object with a saml service and a security service, and then active it.
     */
    @Before
    public void setUp() {
        endpointServices = new EndpointServices();

        mockery.checking(new Expectations() {
            {
                one(SERVICE_REFERENCE_SSO_SERVICE_MCK).getProperty(with(any(String.class)));
                will(returnValue(SSO_SERVICE));
                one(SERVICE_REFERENCE_SSO_SERVICE_MCK).getProperty(with(any(String.class)));
                will(returnValue(SSO_SERVICE_ID));
                one(SERVICE_REFERENCE_SSO_SERVICE_MCK).getProperty(with(any(String.class)));
                will(returnValue(SSO_SERVICE_RANKING));
                //allowing(ssoConfig).isHttpsRequired();
                //will(returnValue(true));
            }
        });

        endpointServices.setSamlService(SERVICE_REFERENCE_SSO_SERVICE_MCK);
        endpointServices.setSecurityService(SECURITY_SERVICE_MCK);
        endpointServices.activate(COMPONENT_CONTEXT_MCK);
    }

    /**
     * Verify that every expectations are satisfied
     */
    @AfterClass
    public static void tearDown() {
        mockery.assertIsSatisfied();
    }

    /**
     * Test that getSamlService throws a SamlException after receiving an IOException by {@link HttpServletResponse#sendError(int, String)}. The test will fail if other exception
     * is thrown.
     */
    @Test(expected = SamlException.class)
    public void getSamlServiceShouldThrowSamlExceptionIfHttpServletResponseThrowsIOException() throws SamlException, IOException {
        mockery.checking(new Expectations() {
            {
                one(SERVICE_REFERENCE_SSO_SERVICE_MCK).getProperty(with(any(String.class)));
                will(returnValue(SSO_SERVICE));
                one(SERVICE_REFERENCE_SSO_SERVICE_MCK).getProperty(with(any(String.class)));
                will(returnValue(SSO_SERVICE_ID));
                one(SERVICE_REFERENCE_SSO_SERVICE_MCK).getProperty(with(any(String.class)));
                will(returnValue(SSO_SERVICE_RANKING));
            }
        });
        endpointServices.unsetSamlService(SERVICE_REFERENCE_SSO_SERVICE_MCK);

        mockery.checking(new Expectations() {
            {
                one(HTTP_SERVLET_REQUEST_MCK).getAttribute(with(any(String.class)));
                will(returnValue(SSO_REQUEST_MCK));
                one(SSO_REQUEST_MCK).getProviderName();
                will(returnValue(SSO_SERVICE));
                one(SSO_REQUEST_MCK).getProviderName();
                will(returnValue(SSO_SERVICE));
                //one(HTTP_SERVLET_RESPONSE_MCK).sendError(with(any(Integer.class)), with(any(String.class)));
                //will(throwException(new IOException()));
            }
        });

        endpointServices.handleSamlRequest(HTTP_SERVLET_REQUEST_MCK, HTTP_SERVLET_RESPONSE_MCK);
    }

    /**
     * Test that getSamlService send an error from a HttpServletResponse if the SsoSamlService from a SsoRequest is null.
     */
    @Test
    public void getSamlServiceShoulSendErrorFromAHttpServletResponseIfSsoServiceIsNull() throws SamlException, IOException {

        mockery.checking(new Expectations() {
            {
                one(SERVICE_REFERENCE_SSO_SERVICE_MCK).getProperty(with(any(String.class)));
                will(returnValue(SSO_SERVICE));
                one(SERVICE_REFERENCE_SSO_SERVICE_MCK).getProperty(with(any(String.class)));
                will(returnValue(SSO_SERVICE_ID));
                one(SERVICE_REFERENCE_SSO_SERVICE_MCK).getProperty(with(any(String.class)));
                will(returnValue(SSO_SERVICE_RANKING));
            }
        });

        //Just one of unsetSamlService or deactivate is necessary to make SsoService null.
        endpointServices.unsetSamlService(SERVICE_REFERENCE_SSO_SERVICE_MCK);
        endpointServices.deactivate(COMPONENT_CONTEXT_MCK);

        mockery.checking(new Expectations() {
            {
                one(HTTP_SERVLET_REQUEST_MCK).getAttribute(with(any(String.class)));
                will(returnValue(SSO_REQUEST_MCK));
                one(SSO_REQUEST_MCK).getProviderName();
                will(returnValue(SSO_SERVICE));
                one(SSO_REQUEST_MCK).getProviderName();
                will(returnValue(SSO_SERVICE));
                //one(HTTP_SERVLET_RESPONSE_MCK).sendError(with(any(Integer.class)), with(any(String.class)));
            }
        });

        try {
            endpointServices.handleSamlRequest(HTTP_SERVLET_REQUEST_MCK, HTTP_SERVLET_RESPONSE_MCK);
            fail("did not get the expected SamlException");
        } catch (SamlException e) {
            // this is what we expect
        }

    }

    /**
     * Test that a saml request can be handled correctly with the necessary parameters.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void handleSamlRequesWithNoFailures() throws IOException, SamlException {

        //Handling a saml request with a saml service and a security service
        mockery.checking(new Expectations() {
            {
                one(HTTP_SERVLET_REQUEST_MCK).getAttribute(with(any(String.class)));
                will(returnValue(SSO_REQUEST_MCK));
                one(SSO_REQUEST_MCK).getProviderName();
                will(returnValue(SSO_SERVICE));
                one(COMPONENT_CONTEXT_MCK).locateService(with(any(String.class)), with(any(ServiceReference.class)));
                will(returnValue(SSO_SERVICE_MCK));
                one(SSO_SERVICE_MCK).isEnabled(); //
                will(returnValue(true)); //
                one(SSO_REQUEST_MCK).setSsoSamlService(with(any(SsoSamlService.class)));
                one(SSO_SERVICE_MCK).getConfig();
                will(returnValue(ssoConfig)); //Aruna

                one(HTTP_SERVLET_REQUEST_MCK).getRequestURL();
                will(returnValue(new StringBuffer(TEST_URL))); //Aruna
                one(ssoConfig).isHttpsRequired();
                will(returnValue(true));

                one(COMPONENT_CONTEXT_MCK).locateService(with(any(String.class)), with(any(ServiceReference.class)));
                will(returnValue(SECURITY_SERVICE_MCK));

                one(SSO_REQUEST_MCK).getSamlVersion();
                will(returnValue(Constants.SamlSsoVersion.SAMLSSO20));
                one(SSO_REQUEST_MCK).getType();
                will(returnValue(Constants.EndpointType.ACS));

                //This is where we set a mocked SsoHandler to avoid testing other parts of the code
                HandlerFactory.setAcsHandler(ACS_HANDLER_MCK);

                one(ACS_HANDLER_MCK).handleRequest(with(any(HttpServletRequest.class)), with(any(HttpServletResponse.class)), with(any(SsoRequest.class)), with(any(Map.class)));
            }
        });

        endpointServices.handleSamlRequest(HTTP_SERVLET_REQUEST_MCK, HTTP_SERVLET_RESPONSE_MCK);

        //Handling a saml request with no security services, this means the service should be null 
        endpointServices.unsetSecurityService(SECURITY_SERVICE_MCK);

        Assert.assertNull(endpointServices.securityServiceRef.getService());

        mockery.checking(new Expectations() {
            {
                one(HTTP_SERVLET_REQUEST_MCK).getAttribute(with(any(String.class)));
                will(returnValue(SSO_REQUEST_MCK));
                one(SSO_REQUEST_MCK).getProviderName();
                will(returnValue(SSO_SERVICE));
                one(SSO_SERVICE_MCK).isEnabled(); //
                will(returnValue(true));

                one(SSO_REQUEST_MCK).setSsoSamlService(with(any(SsoSamlService.class)));
                one(SSO_SERVICE_MCK).getConfig();
                will(returnValue(ssoConfig)); //Aruna

                one(HTTP_SERVLET_REQUEST_MCK).getRequestURL();
                will(returnValue(new StringBuffer(TEST_URL))); //Aruna
                one(ssoConfig).isHttpsRequired();
                will(returnValue(true));
                one(SSO_REQUEST_MCK).getSamlVersion();
                will(returnValue(Constants.SamlSsoVersion.SAMLSSO20));
                one(SSO_REQUEST_MCK).getType();
                will(returnValue(Constants.EndpointType.ACS));

                //This is where we set a mocked SsoHandler to avoid testing other parts of the code
                HandlerFactory.setAcsHandler(ACS_HANDLER_MCK);

                one(ACS_HANDLER_MCK).handleRequest(with(any(HttpServletRequest.class)), with(any(HttpServletResponse.class)), with(any(SsoRequest.class)), with(any(Map.class)));
            }
        });

        try {
            endpointServices.handleSamlRequest(HTTP_SERVLET_REQUEST_MCK, HTTP_SERVLET_RESPONSE_MCK);
        } catch (SamlException e) {
            fail("Should not get a SamlException but get one");
        }

    }
}
