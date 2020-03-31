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
package com.ibm.ws.security.saml.sso20.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.ws.security.saml.sso20.internal.utils.UserData;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

import test.common.SharedOutputManager;

public class SAMLResponseTAITest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final CommonMockObjects common = new CommonMockObjects();
    private final Mockery mockery = common.getMockery();

    private final AuthenticationFilter authFilter = mockery.mock(AuthenticationFilter.class);
    private final Cache cache = common.getCache();
    private final HttpServletResponse response = common.getServletResponse();
    private final HttpServletRequest request = common.getServletRequest();
    private final SsoConfig ssoConfig = common.getSsoConfig();
    private final SsoRequest ssoRequest = common.getSsoRequest();
    private final SsoSamlService ssoService = common.getSsoService();
    private final UserData userData = common.getUserData();
    private final SAMLRequestTAI activatedRequestTai = mockery.mock(SAMLRequestTAI.class);
    @SuppressWarnings("unchecked")
    private final ConcurrentServiceReferenceMap<String, SsoSamlService> activatedSsoServiceRef = mockery.mock(ConcurrentServiceReferenceMap.class);
    @SuppressWarnings("unchecked")
    private final Iterator<SsoSamlService> iterator = mockery.mock(Iterator.class);

    private final SAMLResponseTAI responseTAI = new mockSAMLResponseTAI();
    private final Subject subject = new Subject();

    private static final String PROVIDER_ID = "b07b804c";
    private static final String PROVIDER_NAME = "sp";
    private static byte[] cookieValueBytes = { 'i', 'd', 'p', '_', 'i', 'n', 'i', 't', 'i', 'a', 'l', '_' };

//    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(null);

//        mockery.checking(new Expectations() {
//            {
//                allowing(request).getAttribute(com.ibm.ws.security.saml.Constants.SAML_SAMLEXCEPTION_FOUND);
//                will(returnValue((SamlException) null));
//                allowing(request).getAttribute(com.ibm.ws.security.saml.Constants.ATTRIBUTE_SAML20_REQUEST);
//                will(returnValue(ssoRequest));
//                allowing(ssoRequest).isInboundPropagation();
//                will(returnValue(false));
//                allowing(request).getContextPath();
//                will(returnValue("not/a/contextPath"));
//                allowing(request).setAttribute(with(any(String.class)), with(any(SsoRequest.class)));
//
//                allowing(ssoRequest).getSsoSamlService();
//                will(returnValue(ssoService));
//                allowing(ssoRequest).getProviderName();
//                will(returnValue(PROVIDER_NAME));
//                allowing(ssoRequest).setLocationAdminRef(with(any(AtomicServiceReference.class)));
//                allowing(ssoRequest).getSsoConfig();
//                will(returnValue(ssoConfig));
//
//                one(ssoService).getAuthHelper();
//                will(returnValue(null));
//                allowing(ssoService).getProviderId();
//                will(returnValue(PROVIDER_ID));
//                allowing(ssoService).getConfig();
//                will(returnValue(ssoConfig));
//                allowing(ssoService).getAcsCookieCache(PROVIDER_NAME);
//                will(returnValue(cache));
//
//                allowing(activatedSsoServiceRef).size();
//                will(returnValue(1));
//                allowing(activatedSsoServiceRef).getServices();
//                will(returnValue(iterator));
//                allowing(activatedSsoServiceRef).getService(PROVIDER_NAME);
//                will(returnValue(ssoService));
//                allowing(ssoService).isInboundPropagation();
//                will(returnValue(false));
//
//                allowing(iterator).next();
//                will(returnValue(ssoService));
//
//                allowing(ssoConfig).getAuthFilter(with(any(ConcurrentServiceReferenceMap.class)));
//                will(returnValue(authFilter));
//                allowing(ssoConfig).getReAuthnCushion();
//                will(returnValue(0l));
//                allowing(ssoConfig).isReAuthnOnAssertionExpire();
//                will(returnValue(false));
//
//                allowing(authFilter).isAccepted(with(any(HttpServletRequest.class)));
//                will(returnValue(true));
//            }
//        });
    }

    @Before
    public void before() {
        SAMLResponseTAI.setActivatedRequestTai(activatedRequestTai);
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    @After
    public void after() {
        SAMLResponseTAI.setTheActivatedSsoSamlServiceRef(SAMLResponseTAI.respSsoSamlServiceRef);
        mockery.assertIsSatisfied();
    }

    //@Test - this is not a valid test and depends on an NPE in the runtime
    public void testNegotiateValidateandEstablishTrust_ExistentUserData() {
        negotiateValidateandEstablishTrustBasicExpectations();
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).getUserData();
                will(returnValue(userData));
                one(ssoService).getAuthHelper();
                will(returnValue(null));
                one(ssoService).getProviderId();
                will(returnValue(PROVIDER_ID));
                one(ssoService).getConfig();
                will(returnValue(ssoConfig));
            }
        });
        try {
            TAIResult taiResult = responseTAI.negotiateValidateandEstablishTrust(request, response);
            assertTrue("Expected to receive the status code '" + HttpServletResponse.SC_FORBIDDEN + "' but it was not received.",
                       (taiResult != null) && (taiResult.getStatus() == HttpServletResponse.SC_FORBIDDEN));
        } catch (WebTrustAssociationFailedException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testNegotiateValidateandEstablishTrust_NullUserData() {
        negotiateValidateandEstablishTrustBasicExpectations();
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).getUserData();
                will(returnValue(null));
                one(ssoRequest).isDisableLtpaCookie();
                will(returnValue(false));
            }
        });
        try {
            responseTAI.negotiateValidateandEstablishTrust(request, response);
            fail("WebTrustAssociationFailedException was not thrown");
        } catch (WebTrustAssociationFailedException ex) {
            assertTrue("Expected to receive the message 'CWWKS5063E' but it was not received.",
                       ex.getMessage().contains("CWWKS5063E"));
        }
    }

    @Test
    public void testNegotiateValidateandEstablishTrust_DisabledLtpaCookie() throws WebTrustAssociationFailedException {
        final TAIResult taiResult = new TAIResult(0, "", subject);

        negotiateValidateandEstablishTrustBasicExpectations();
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).getUserData();
                will(returnValue(null));
                one(ssoRequest).isDisableLtpaCookie();
                will(returnValue(true));
                one(ssoRequest).setType(Constants.EndpointType.REQUEST);

                one(activatedRequestTai).negotiateValidateandEstablishTrust(with(any(HttpServletRequest.class)), with(any(HttpServletResponse.class)));
                will(returnValue(taiResult));
            }
        });
        try {
            TAIResult testResult = responseTAI.negotiateValidateandEstablishTrust(request, response);
            assertEquals("Expected to receive the correct TAIResult value but it was not received.",
                         testResult, taiResult);
        } catch (WebTrustAssociationFailedException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testValidateSubject() {
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).getSsoConfig();
                will(returnValue(ssoConfig));
                one(ssoConfig).getReAuthnCushion();
                will(returnValue(0l));
                allowing(ssoConfig).isReAuthnOnAssertionExpire();
                will(returnValue(false));
            }
        });
        Boolean result = responseTAI.validateSubject(subject, request, response, ssoRequest);
        assertTrue("Expected to receive a false value but it was not received.", !result);
    }

    @Test
    public void testIsTargetInterceptor_NoACSCookie() throws WebTrustAssociationException {
        SAMLResponseTAI.setTheActivatedSsoSamlServiceRef(activatedSsoServiceRef);

        isTargetInterceptorBasicExpectations(null);
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).isDisableLtpaCookie();
                will(returnValue(false));

            }
        });

        boolean result = responseTAI.isTargetInterceptor(request);
        assertFalse("Expected to receive a false value but it was not received.", result);
    }

    @Test
    public void testIsTargetInterceptor_HandledWithCookie() throws WebTrustAssociationException {
        SAMLResponseTAI.setTheActivatedSsoSamlServiceRef(activatedSsoServiceRef);

        isTargetInterceptorBasicExpectations(cookieValueBytes);
        cookieCacheExpectations(userData);

        boolean result = responseTAI.isTargetInterceptor(request);
        assertTrue("Expected to receive a true value but it was not received.", result);
    }

    @Test
    public void testIsTargetInterceptor_DisabledLtpaCookie() throws WebTrustAssociationException {
        SAMLResponseTAI.setTheActivatedSsoSamlServiceRef(activatedSsoServiceRef);

        isTargetInterceptorBasicExpectations(cookieValueBytes);
        cookieCacheExpectations(null);
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).isDisableLtpaCookie();
                will(returnValue(true));

            }
        });
        boolean result = responseTAI.isTargetInterceptor(request);
        assertTrue("Expected to receive a true value but it was not received.", result);
    }

    static class mockSAMLResponseTAI extends SAMLResponseTAI {
        public boolean bTestRemoveSpCookie = false;

        @Override
        void removeInvalidSpCookie(HttpServletRequest req, HttpServletResponse resp, SsoRequest samlRequest) {
            if (bTestRemoveSpCookie) {
                super.removeInvalidSpCookie(req, resp, samlRequest);
            }
        }

    }

    private void negotiateValidateandEstablishTrustBasicExpectations() {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(com.ibm.ws.security.saml.Constants.SAML_SAMLEXCEPTION_FOUND);
                will(returnValue(null));
                one(request).getAttribute(com.ibm.ws.security.saml.Constants.ATTRIBUTE_SAML20_REQUEST);
                will(returnValue(ssoRequest));
                one(ssoRequest).getSsoSamlService();
                will(returnValue(ssoService));
                one(ssoService).isInboundPropagation();
                will(returnValue(false));
                allowing(ssoRequest).getProviderName();
                will(returnValue(PROVIDER_NAME));
                one((IExtendedRequest) request).getCookieValueAsBytes(with(any(String.class)));
                will(returnValue(null));
            }
        });
    }

    private void isTargetInterceptorBasicExpectations(final byte[] acsCookieValueBytes) {
        isUnprotectedUrlForSamlExpectations();
        findSpSpecificFirstExpectations();
        handledWithCookieExpectations(acsCookieValueBytes);
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(com.ibm.ws.security.saml.Constants.ATTRIBUTE_SAML20_REQUEST);
                will(returnValue(ssoRequest));
                one(ssoService).isInboundPropagation();
                will(returnValue(false));
                allowing(ssoRequest).setLocationAdminRef(with(any(AtomicServiceReference.class)));
            }
        });
    }

    private void isUnprotectedUrlForSamlExpectations() {
        mockery.checking(new Expectations() {
            {
                allowing(request).getContextPath();
                will(returnValue("not/a/contextPath"));
            }
        });
    }

    private void findSpSpecificFirstExpectations() {
        mockery.checking(new Expectations() {
            {
                allowing(activatedSsoServiceRef).size();
                will(returnValue(1));
                one(activatedSsoServiceRef).getServices();
                will(returnValue(iterator));
                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(ssoService));
                allowing(ssoRequest).isInboundPropagation();
                will(returnValue(false));
                one(ssoService).isEnabled();
                will(returnValue(true));
                allowing(ssoService).getProviderId();
                will(returnValue(PROVIDER_ID));
                allowing(ssoService).getConfig();
                will(returnValue(ssoConfig));
                one(ssoConfig).getAuthFilter(with(any(ConcurrentServiceReferenceMap.class)));
                will(returnValue(authFilter));
                one(authFilter).isAccepted(with(any(HttpServletRequest.class)));
                will(returnValue(true));
                one(iterator).hasNext();
                will(returnValue(false));
                allowing(request).setAttribute(with(any(String.class)), with(any(SsoRequest.class)));
            }
        });
    }

    private void handledWithCookieExpectations(final byte[] acsCookieValueBytes) {
        mockery.checking(new Expectations() {
            {
                allowing(ssoRequest).getProviderName();
                will(returnValue(PROVIDER_NAME));
                allowing((IExtendedRequest) request).getCookieValueAsBytes(with(any(String.class)));
                will(returnValue(acsCookieValueBytes));
            }
        });
    }

    private void cookieCacheExpectations(final UserData userData) {
        mockery.checking(new Expectations() {
            {
                allowing(activatedSsoServiceRef).getService(PROVIDER_NAME);
                will(returnValue(ssoService));
                allowing(ssoService).getAcsCookieCache(PROVIDER_NAME);
                will(returnValue(cache));
                allowing(cache).get(with(any(String.class)));
                will(returnValue(userData));
            }
        });
        if (userData != null) {
            mockery.checking(new Expectations() {
                {
                    allowing(ssoRequest).setUserData(userData);
                    one(cache).remove(with(any(String.class)));

                }
            });
        }
    }

}
