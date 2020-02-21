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

import static com.ibm.ws.security.saml.Constants.ATTRIBUTE_SAML20_REQUEST;
import static com.ibm.ws.security.saml.Constants.HTTP_ATTRIBUTE_SP_INITIATOR;
import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.keyId;
import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.keyServicePID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.SERVICE_RANKING;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.Constants.EndpointType;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContextBuilder;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.saml2.UserCredentialResolver;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletRequest;

import test.common.SharedOutputManager;

/**
 * Unit test for {@link SAMLRequestTAI} class.
 */
public class SAMLRequestTAITest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final Cache cache = common.getCache();
    public static final HttpServletRequest request1 = mockery.mock(SRTServletRequest.class, "request1");// common.getServletRequest();
    public static final HttpServletResponse response = common.getServletResponse();
    private static final HttpSession session = common.getSession();
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final SsoSamlService ssoService = common.getSsoService();
    private static final WebProviderAuthenticatorHelper authHelper = common.getAuthHelper();
    private static final WebAppSecurityConfig webAppSecConfig = common.getWebAppSecConfig();
    private static final SsoSamlService ssoService2 = mockery.mock(SsoSamlService.class, "ssoService2");
    private static final PrintWriter writer = mockery.mock(PrintWriter.class, "printWriter");
    private static final AuthenticationFilter authFiler = mockery.mock(AuthenticationFilter.class);
    private static final AuthenticationFilter authFiler2 = mockery.mock(AuthenticationFilter.class, "authFilter2");
    private static final BasicMessageContext<?, ?, ?> basicMessageContext = common.getBasicMessageContext();
    private static final BasicMessageContextBuilder<?, ?, ?> basicMessageContextBuilder = common.getBasicMessageContextBuilder();
    private static final MetadataProvider metadataProvider = common.getMetadataProvider();
    private static final SsoRequest ssoRequest = common.getSsoRequest();
    private static final AuthenticationFilter authFilter = mockery.mock(AuthenticationFilter.class, "authFilter");

    private static final ComponentContext cc = mockery.mock(ComponentContext.class, "componentContext");
    @SuppressWarnings("unchecked")
    private static final ServiceReference<SsoSamlService> ssoServiceRef = mockery.mock(ServiceReference.class, "ssoServiceRef");
    @SuppressWarnings("unchecked")
    private static final ServiceReference<SsoSamlService> ssoServiceRef2 = mockery.mock(ServiceReference.class, "ssoServiceRef2");
    @SuppressWarnings("unchecked")
    private static final ServiceReference<AuthenticationFilter> authFilterServiceRef = mockery.mock(ServiceReference.class, "authFilterRef");
    @SuppressWarnings("unchecked")
    private static final ConcurrentServiceReferenceMap<String, SsoSamlService> reqSsoServiceRef = mockery.mock(ConcurrentServiceReferenceMap.class, "reqSsoServiceRef");
    @SuppressWarnings("unchecked")
    private static final Iterator<SsoSamlService> iterator = mockery.mock(Iterator.class, "iterator");

    private static final String ID = "keyId";
    private static final String PROVIDER_ID = "providerId";
    private static final Long SECURITY_SERVICE_ID = 1l;
    private static final Integer SECURITY_SERVICE_RANKING = 1;
    private static final String LOGIN_PAGE_URL = "https://domain.com/login?company=ibm";
    private static final String REQUESTED_PAGE_URL = "https://domain.com/resource";
    private static final String METHOD = "POST";
    private static final String ERROR_PAGE_URL = "ErrorPageURL";
    private static StringTokenizer PARAMETER_NAMES = new StringTokenizer("param1 param2 param3");

    private static HashMap map = new HashMap();

    @SuppressWarnings("rawtypes")
    static BasicMessageContextBuilder<?, ?, ?> instance = new BasicMessageContextBuilder();
    private SAMLRequestTAI requestTai;

    @BeforeClass
    public static void setUp() throws Exception {
        outputMgr.trace("*=all");
        final MetadataProviderException e = new MetadataProviderException();
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
        BasicMessageContextBuilder.setInstance(basicMessageContextBuilder);

        mockery.checking(new Expectations() {
            {
                allowing((IExtendedRequest) request1).getCookieValueAsBytes(with(any(String.class)));
                allowing((IExtendedRequest) request1).getRequestURL();
                will(returnValue(new StringBuffer(REQUESTED_PAGE_URL)));
                allowing((IExtendedRequest) request1).getQueryString(); //
                will(returnValue(null)); //
                allowing((IExtendedRequest) request1).getContextPath();
                will(returnValue("not/a/contextPath"));
                one((IExtendedRequest) request1).getMethod();
                will(returnValue(METHOD));
                one((IExtendedRequest) request1).getParameterNames();
                will(returnValue(PARAMETER_NAMES));
                allowing((IExtendedRequest) request1).getParameterValues(with(any(String.class)));
                one((IExtendedRequest) request1).getContentType();
                allowing((IExtendedRequest) request1).setAttribute(with(any(String.class)), with(any(String.class)));
                allowing((IServletRequest) request1).getInputStreamData();
                will(returnValue(map));
                allowing((IExtendedRequest) request1).setInputStreamData(map);

                allowing((IExtendedRequest) request1).getParameter("SAMLRequest");
                will(returnValue(null));

                one(response).setStatus(200);
                allowing(response).setHeader(with(any(String.class)), with(any(String.class)));
                one(response).addCookie(with(any(Cookie.class)));
                one(response).setDateHeader(with(any(String.class)), with(any(Long.class)));
                one(response).setContentType(with(any(String.class)));
                one(response).getWriter();
                will(returnValue(writer));
                allowing(response).setStatus(HttpServletResponse.SC_FORBIDDEN);

                one(writer).println(with(any(String.class)));
                one(writer).flush();

                allowing(ssoServiceRef).getProperty(SAMLRequestTAI.KEY_ID);
                will(returnValue(ID));
                allowing(ssoServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(SECURITY_SERVICE_ID));
                allowing(ssoServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(SECURITY_SERVICE_RANKING));

                one(ssoService).getAuthHelper();
                will(returnValue(authHelper));
                allowing(ssoService).getProviderId();
                will(returnValue(PROVIDER_ID));
                allowing(ssoService).getConfig();
                will(returnValue(ssoConfig));
                one(ssoService).getAcsCookieCache(PROVIDER_ID);
                will(returnValue(cache));
                allowing(ssoService).isInboundPropagation();
                will(returnValue(false));

                allowing(ssoConfig).getErrorPageURL();
                will(returnValue(ERROR_PAGE_URL));

                allowing(cc).locateService(SAMLRequestTAI.KEY_SSO_SAML_SERVICE, ssoServiceRef);
                will(returnValue(ssoService));

                allowing(ssoService).isEnabled();
                will(returnValue(true));

                one(cc).locateService(SAMLRequestTAI.KEY_FILTER, authFilterServiceRef);
                will(returnValue(authFiler));

                one(cache).put(with(any(String.class)), with(any(Object.class)));

                one(authFilterServiceRef).getProperty(SsoServiceImpl.KEY_SERVICE_PID);
                will(returnValue(keyServicePID));
                one(authFilterServiceRef).getProperty(SsoServiceImpl.KEY_ID);
                will(returnValue(keyId));
                one(authFilterServiceRef).getProperty(SERVICE_ID);
                will(returnValue(0l));
                one(authFilterServiceRef).getProperty(SERVICE_RANKING);
                will(returnValue(0l));

                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecConfig)));

                allowing(basicMessageContextBuilder).buildIdp(request1, response, ssoService);
                will(returnValue(basicMessageContext));

                allowing(basicMessageContext).getMetadataProvider();
                will(returnValue(metadataProvider));

                allowing(metadataProvider).getMetadata();
                will(throwException(e));

                allowing(ssoRequest).getSsoSamlService();
                will(returnValue(ssoService));

                allowing(ssoConfig).createSession();
                will(returnValue(with(any(Boolean.class))));
                allowing(request1).getSession(true);
                will(returnValue(session));
            }
        });
    }

    @AfterClass
    public static void tearDown() {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(null);
        BasicMessageContextBuilder.setInstance(instance);
        outputMgr.trace("*=all=disabled");
    }

    @Before
    public void initializeActivateRequestTai() {
        requestTai = new SAMLRequestTAI();
        requestTai.activate(cc, null);
    }

    @After
    public void deactivateRequestTai() {
        if (requestTai != null) {
            requestTai.deactivate(cc);
        }
    }

    @Test
    public void testDeleteMe() {
        // TODO - Delete me
    }

    //@Test
    public void testNegotiateValidateandEstablishTrust() throws WebTrustAssociationFailedException {
        mockery.checking(new Expectations() {
            {
                allowing(request1).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
                one(request1).getAttribute(com.ibm.ws.security.saml.Constants.SAML_SAMLEXCEPTION_FOUND);
                will(returnValue((SamlException) null));
                one(request1).getAttribute(with(any(String.class)));
                will(returnValue(ID));
                one(ssoConfig).getLoginPageURL();
                will(returnValue(LOGIN_PAGE_URL));
            }
        });
        requestTai.setSsoSamlService(ssoServiceRef);
        TAIResult result = requestTai.negotiateValidateandEstablishTrust(request1, response);
        assertNotNull("TAI result should not be null", result);
    }

    @SuppressWarnings("unchecked")
    //@Test
    public void testIsTargetInterceptor() throws WebTrustAssociationException {
        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getAuthFilter(with(any(ConcurrentServiceReferenceMap.class)));
                will(returnValue(null));
                allowing(request1).getParameter("SAMLRequest");
                will(returnValue(null));
                allowing(request1).getAttribute("FormLogoutExitPage");
                will(returnValue(null));

            }
        });

        requestTai.setSsoSamlService(ssoServiceRef);
        boolean result = requestTai.isTargetInterceptor(request1);
        assertTrue("The result shoul be true and got false", result);
    }

    //@Test
    public void testChangeAuthFilter() {
        requestTai.setAuthFilter(authFilterServiceRef);
        AuthenticationFilter result = requestTai.getAuthFilter(keyServicePID);
        assertEquals(authFiler, result);

        @SuppressWarnings("unchecked")
        final ServiceReference<AuthenticationFilter> authFilterServiceRef2 = mockery.mock(ServiceReference.class, "authFilterServiceRef2");

        mockery.checking(new Expectations() {
            {
                atMost(2).of(authFilterServiceRef2).getProperty(SsoServiceImpl.KEY_SERVICE_PID);
                will(returnValue(keyServicePID));
                atMost(2).of(authFilterServiceRef2).getProperty(SsoServiceImpl.KEY_ID);
                will(returnValue(keyId));
                atMost(2).of(authFilterServiceRef2).getProperty(SERVICE_ID);
                will(returnValue(0l));
                atMost(2).of(authFilterServiceRef2).getProperty(SERVICE_RANKING);
                will(returnValue(0l));

                one(cc).locateService(SAMLRequestTAI.KEY_FILTER, authFilterServiceRef2);
                will(returnValue(authFiler2));
                allowing(request1).getAttribute("FormLogoutExitPage");
                will(returnValue(null));

            }
        });
        requestTai.updatedAuthFilter(authFilterServiceRef2);
        result = requestTai.getAuthFilter(keyServicePID);
        assertEquals(authFiler2, result);

        requestTai.unsetAuthFilter(authFilterServiceRef2);
        result = requestTai.getAuthFilter(keyServicePID);
        assertNull(result);
    }

    //@Test
    public void testChangeSsoService() {
        SsoSamlService result;

        requestTai.setSsoSamlService(ssoServiceRef);
        result = requestTai.getSsoSamlService(ID);
        assertEquals(ssoService, result);

        mockery.checking(new Expectations() {
            {
                atMost(2).of(ssoServiceRef2).getProperty(SAMLRequestTAI.KEY_ID);
                will(returnValue(ID));
                atMost(2).of(ssoServiceRef2).getProperty(Constants.SERVICE_ID);
                will(returnValue(SECURITY_SERVICE_ID));
                atMost(2).of(ssoServiceRef2).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(SECURITY_SERVICE_RANKING));

                one(cc).locateService(SAMLRequestTAI.KEY_SSO_SAML_SERVICE, ssoServiceRef2);
                will(returnValue(ssoService2));
                allowing(request1).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
            }
        });

        requestTai.updatedSsoSamlService(ssoServiceRef2);
        result = requestTai.getSsoSamlService(ID);
        assertEquals(ssoService2, result);

        requestTai.unsetSsoSamlService(ssoServiceRef2);
        result = requestTai.getSsoSamlService(ID);
        assertNull(result);
    }

    //@Test
    public void testNegotiateValidateandEstablishTrust_NullSPInitiatorId() {
        mockery.checking(new Expectations() {
            {
                one(request1).getAttribute(com.ibm.ws.security.saml.Constants.SAML_SAMLEXCEPTION_FOUND);
                will(returnValue((SamlException) null));
                one(request1).getAttribute(with(any(String.class)));
                will(returnValue(null));
                allowing(request1).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
            }
        });
        try {
            requestTai.negotiateValidateandEstablishTrust(request1, response);
            fail("WebTrustAssociationFailedException was not thrown");
        } catch (WebTrustAssociationFailedException ex) {
            assertTrue("Expected to receive the message 'CWWKS5063E' but it was not received.",
                       ex.getMessage().contains("CWWKS5063E"));
        }
    }

    //@Test
    public void testNegotiateValidateandEstablishTrust_GoodSPInitiatorId() throws SamlException, MetadataProviderException, IOException {

        requestTai.setSsoSamlService(ssoServiceRef);
        mockery.checking(new Expectations() {
            {
                one(request1).getAttribute(com.ibm.ws.security.saml.Constants.SAML_SAMLEXCEPTION_FOUND);
                will(returnValue((SamlException) null));
                one(request1).getAttribute(HTTP_ATTRIBUTE_SP_INITIATOR);
                will(returnValue(ID));
                one(request1).getAttribute(ATTRIBUTE_SAML20_REQUEST);
                will(returnValue(ssoRequest));

                one(ssoConfig).getLoginPageURL();
                will(returnValue(null));

                one(response).sendRedirect(ERROR_PAGE_URL);
                allowing(request1).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
            }
        });
        try {
            TAIResult taiResult = requestTai.negotiateValidateandEstablishTrust(request1, response);
            assertTrue("Expected to receive the status code '" + HttpServletResponse.SC_FORBIDDEN + "' but it was not received.",
                       (taiResult != null) && (taiResult.getStatus() == HttpServletResponse.SC_FORBIDDEN));
        } catch (WebTrustAssociationFailedException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    //@Test
    public void testNegotiateValidateandEstablishTrust_BadErrorPageUrl() throws IOException {
        final IOException e = new IOException();
        requestTai.setSsoSamlService(ssoServiceRef);

        mockery.checking(new Expectations() {
            {
                one(request1).getAttribute(com.ibm.ws.security.saml.Constants.SAML_SAMLEXCEPTION_FOUND);
                will(returnValue((SamlException) null));
                one(request1).getAttribute(HTTP_ATTRIBUTE_SP_INITIATOR);
                will(returnValue(ID));
                one(request1).getAttribute(ATTRIBUTE_SAML20_REQUEST);
                will(returnValue(ssoRequest));

                one(ssoConfig).getLoginPageURL();
                will(returnValue(null));

                one(response).sendRedirect(ERROR_PAGE_URL);
                will(throwException(e));
                allowing(request1).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
            }
        });
        try {
            requestTai.negotiateValidateandEstablishTrust(request1, response);
            fail("WebTrustAssociationFailedException was not thrown");
        } catch (WebTrustAssociationFailedException ex) {
            // expected exception
        }
    }

    @SuppressWarnings("unchecked")
    //@Test
    public void testFindSpSpecificFirst_SeveralSP() {
        mockery.checking(new Expectations() {
            {
                atMost(2).of(reqSsoServiceRef).size();
                will(returnValue(2));
                allowing(reqSsoServiceRef).getServices();
                will(returnValue(iterator));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).hasNext();
                will(returnValue(false));
                allowing(iterator).next();
                will(returnValue(ssoService));

                allowing(ssoConfig).getAuthFilter(with(any(ConcurrentServiceReferenceMap.class)));
                will(returnValue(authFilter));

                allowing(authFilter).isAccepted(with(any(HttpServletRequest.class)));
                will(returnValue(true));
                allowing(request1).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
            }
        });
        Boolean result = requestTai.findSpSpecificFirst(request1, reqSsoServiceRef, EndpointType.RESPONSE);
        assertTrue("Expected to receive a false value but it was not received.", !result);
    }

    @SuppressWarnings("unchecked")
    //@Test
    public void testUserResolver() {
        final ServiceReference<UserCredentialResolver> ref1 = mockery.mock(ServiceReference.class, "ref1");
        final ServiceReference<UserCredentialResolver> ref2 = mockery.mock(ServiceReference.class, "ref2");
        final String KEY_1 = "key_1";
        final String KEY_2 = "key_2";

        mockery.checking(new Expectations() {
            {
                allowing(ref1).getProperty(SAMLRequestTAI.KEY_SERVICE_PID);
                will(returnValue(KEY_1));
                allowing(ref1).getProperty(SERVICE_ID);
                will(returnValue(1l));
                allowing(ref1).getProperty(SERVICE_RANKING);
                will(returnValue(1));

                allowing(ref2).getProperty(SAMLRequestTAI.KEY_SERVICE_PID);
                will(returnValue(KEY_2));
                allowing(ref2).getProperty(SERVICE_ID);
                will(returnValue(1l));
                allowing(ref2).getProperty(SERVICE_RANKING);
                will(returnValue(1));
                allowing(request1).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
            }
        });

        requestTai.setUserResolver(ref1);
        assertEquals("Expected to receive the object ref1 but it was not received", ref1, requestTai.userResolverRef.getReference(KEY_1));

        requestTai.updatedUserResolver(ref2);
        assertEquals("Expected to receive the object ref2 but it was not received", ref2, requestTai.userResolverRef.getReference(KEY_2));

        requestTai.unsetUserResolver(ref1);
        assertTrue("Expected to receive a null value but it was not received", requestTai.userResolverRef.getReference(KEY_1) == null);
        requestTai.unsetUserResolver(ref2);
        assertTrue("Expected to receive a null value but it was not received", requestTai.userResolverRef.getReference(KEY_2) == null);
    }

}
