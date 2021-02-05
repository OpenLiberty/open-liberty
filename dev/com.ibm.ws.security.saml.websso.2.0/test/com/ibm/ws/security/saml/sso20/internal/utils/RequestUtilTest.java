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
package com.ibm.ws.security.saml.sso20.internal.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

import test.common.SharedOutputManager;

public class RequestUtilTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final String acsProviderId = "acsProviderId";
    private final String protocol = "HTTP";
    private final String providerId = "providerId";
    private final String samlCtxPath = "saml/ctx/path";
    private final String serverName = "myServerName";
    private final String cookieName = "WASReqURL";
    private final int serverPort = 8080;
    private final String relayState = "RPID%3Dhttps%253A%252F%252Frelyingpartyapp%26wctx%3Dappid%253D45%2526foo%253Dbar";
    private final String inResponseTo = "inResponseTo";

    private final String privateAttribute = "SecurityRedirectPort";
    private final int privateAttributeValue = 80;

    private static final Cache cache = mockery.mock(Cache.class);
    private static final HttpServletRequest httpServletRequest = mockery.mock(HttpServletRequest.class);
    private static final HttpServletResponse httpServletResponse = mockery.mock(HttpServletResponse.class);
    private static final SsoSamlService ssoService = mockery.mock(SsoSamlService.class);
    private static final SsoConfig ssoConfig = mockery.mock(SsoConfig.class);
    private static final ForwardRequestInfo requestInfo = mockery.mock(ForwardRequestInfo.class);
    private static final WebAppSecurityConfig webAppSecurity = mockery.mock(WebAppSecurityConfig.class);
    private static final IExtendedRequest iExtended = mockery.mock(IExtendedRequest.class);
    private static final SRTServletRequest srtServletRequest = mockery.mock(SRTServletRequest.class);
    private static final HttpServletRequestWrapper httpServletRequestWrapper = mockery.mock(HttpServletRequestWrapper.class);
    private static final PrivateKey privateKey = mockery.mock(PrivateKey.class);
    private static final java.security.cert.X509Certificate X509Certificate = mockery.mock(java.security.cert.X509Certificate.class);
    private static final PublicKey publicKey = mockery.mock(PublicKey.class);
    private static final Exception exception = mockery.mock(Exception.class);
    private static final BasicMessageContext<?, ?, ?> basicMessageContext = mockery.mock(BasicMessageContext.class);
    private static final SsoRequest ssoRequest = mockery.mock(SsoRequest.class);
    @SuppressWarnings("unchecked")
    private static final ConcurrentServiceReferenceMap<String, SsoSamlService> ssoSamlServiceRef = mockery.mock(ConcurrentServiceReferenceMap.class);

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
        mockery.checking(new Expectations() {
            {
                allowing(ssoConfig).getSpHostAndPort();
                will(returnValue(null));
                allowing(httpServletRequest).isSecure();//
                will(returnValue(true));//
                allowing(httpServletRequest).getScheme();//
                will(returnValue("https"));//
            }
        });
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    @After
    public void after() {
        mockery.assertIsSatisfied();
    }

    @Test
    public void cacheRequestInfo() {
        mockery.checking(new Expectations() {
            {
                one(ssoService).getProviderId();
                will(returnValue(providerId));
                one(ssoService).getAcsCookieCache(providerId);
                will(returnValue(cache));

                one(cache).put(providerId, requestInfo);
            }
        });
        RequestUtil.cacheRequestInfo(providerId, ssoService, requestInfo);
    }

    public void getAcsUrlDifferentTest() {
        mockery.checking(new Expectations() {
            {
                one(httpServletRequest).getServerName();
                will(returnValue(serverName));

                one(httpServletRequest).getServerPort();
                will(returnValue(serverPort));

                //one(httpServletRequest).getProtocol();
                //will(returnValue(protocol));
            }
        });
        RequestUtil.getAcsUrl(httpServletRequestWrapper, samlCtxPath, acsProviderId, ssoConfig);
    }

    @Test
    public void getAcsUrlTest() {
        mockery.checking(new Expectations() {
            {
                one(httpServletRequest).getServerName();
                will(returnValue(serverName));

                one(httpServletRequest).getServerPort();
                will(returnValue(serverPort));

                //one(httpServletRequest).getProtocol();
                //will(returnValue(protocol));
            }
        });
        RequestUtil.getAcsUrl(httpServletRequest, samlCtxPath, acsProviderId, ssoConfig);
    }

    @Test
    public void getEntityUrlTest() {
        mockery.checking(new Expectations() {
            {
                one(httpServletRequest).getServerName();
                will(returnValue(serverName));

                one(httpServletRequest).getServerPort();
                will(returnValue(serverPort));

                //one(httpServletRequest).getProtocol();
                //will(returnValue(protocol));
            }
        });
        RequestUtil.getEntityUrl(httpServletRequest, samlCtxPath, acsProviderId, ssoConfig);
    }

    @Test
    public void getRedirectPortFromRequest() {
        mockery.checking(new Expectations() {
            {
                one(srtServletRequest).setPrivateAttribute(privateAttribute, privateAttributeValue);
                one(srtServletRequest).getPrivateAttribute(privateAttribute);
                will(returnValue(privateAttributeValue));
            }
        });
        int number = 80;
        srtServletRequest.setPrivateAttribute(privateAttribute, number);
        RequestUtil.getRedirectPortFromRequest(srtServletRequest);
    }

    @Test
    public void getCtxRootUrl() {
        mockery.checking(new Expectations() {
            {
                one(httpServletRequest).getServerName();
                will(returnValue(serverName));

                one(httpServletRequest).getServerPort();
                will(returnValue(serverPort));

                //one(httpServletRequest).getProtocol();
                //will(returnValue(protocol));
            }
        });
        RequestUtil.getCtxRootUrl(httpServletRequest, samlCtxPath, ssoConfig);
    }

    @Test
    public void getWrappedServletRequestObjectTest() {
        mockery.checking(new Expectations() {
            {
                allowing(httpServletRequestWrapper).getRequest();
                will(returnValue(httpServletRequest));
            }
        });
        RequestUtil.getWrappedServletRequestObject(httpServletRequestWrapper);
    }

    //@Test
    public void createCookieTest() {
        mockery.checking(new Expectations() {
            {
                one(webAppSecurity).isIncludePathInWASReqURL();
                will(returnValue(true));
                one(httpServletRequest).getContextPath();
                will(returnValue(samlCtxPath));
                one(webAppSecurity).getHttpOnlyCookies();
                will(returnValue(true));
                one(webAppSecurity).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecurity).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecurity)));
                one(httpServletResponse).addCookie(with(any(Cookie.class)));
            }
        });

        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecurity);
        RequestUtil.createCookie(httpServletRequest, httpServletResponse, cookieName, "mycookievalue");
    }

    //@Test
    public void getCookieID() throws SamlException {
        mockery.checking(new Expectations() {
            {
                one(iExtended).getCookieValueAsBytes(cookieName);
            }
        });
        RequestUtil.getCookieId(iExtended, httpServletResponse, cookieName);
    }

    //@Test(expected = SamlException.class)
    public void getDecryptingCredentialWithNullPrivateKeyTest() throws SamlException, KeyStoreException, CertificateException {
        mockery.checking(new Expectations() {
            {
                one(ssoService).getPrivateKey();
                will(returnValue(null));

                one(ssoService).getProviderId();
                will(returnValue(providerId));

                one(ssoService).getConfig();
                will(returnValue(ssoConfig));

                one(ssoConfig).getKeyStoreRef();
                will(returnValue("unitTestKeyStoreRef"));
            }
        });
        RequestUtil.getDecryptingCredential(ssoService);
    }

    //@Test(expected = Exception.class)
    public void getDecryptingCredentialThrowsExceptionTest() throws SamlException, KeyStoreException, CertificateException {
        mockery.checking(new Expectations() {
            {
                one(ssoService).getPrivateKey();
                will(returnValue(exception));
            }
        });
        RequestUtil.getDecryptingCredential(ssoService);
    }

    //@Test(expected = SamlException.class)
    public void getSigningCredentialWithoutPrivateKeyTest() throws SamlException, KeyStoreException, CertificateException {
        mockery.checking(new Expectations() {
            {
                one(ssoService).getPrivateKey();
                will(returnValue(null));

                one(ssoService).getProviderId();
                will(returnValue(providerId));

                one(ssoService).getConfig();
                will(returnValue(ssoConfig));

                one(ssoConfig).getKeyStoreRef();
                will(returnValue("unitTestKeyStoreRef"));
            }
        });
        RequestUtil.getSigningCredential(ssoService);
    }

    //@Test(expected = SamlException.class)
    public void getSigningCredentialWhenCertificateIsNullTest() throws SamlException, KeyStoreException, CertificateException {
        mockery.checking(new Expectations() {
            {
                one(ssoService).getPrivateKey();
                will(returnValue(privateKey));
                one(ssoService).getSignatureCertificate();
                will(returnValue(null));

                one(ssoService).getProviderId();
                will(returnValue(providerId));

                one(ssoService).getConfig();
                will(returnValue(ssoConfig));

                one(ssoConfig).getKeyStoreRef();
                will(returnValue("unitTestKeyStoreRef"));
            }
        });
        RequestUtil.getSigningCredential(ssoService);
    }

    //@Test(expected = Exception.class)
    public void getSigningCredentialThrowsExceptionTest() throws SamlException, KeyStoreException, CertificateException {
        mockery.checking(new Expectations() {
            {
                one(ssoService).getPrivateKey();
                will(returnValue(privateKey));
                one(ssoService).getSignatureCertificate();
                will(returnValue(exception));

            }
        });
        RequestUtil.getSigningCredential(ssoService);
    }

    //@Test
    public void getSigningCredentialTest() throws SamlException, KeyStoreException, CertificateException {
        mockery.checking(new Expectations() {
            {
                one(ssoService).getPrivateKey();
                will(returnValue(privateKey));
                one(ssoService).getSignatureCertificate();
                will(returnValue(X509Certificate));

                allowing(X509Certificate).getPublicKey();
                will(returnValue(publicKey));
            }
        });
        RequestUtil.getSigningCredential(ssoService);
    }

    //@Test
    public void getUserNameWhenSubjectIsNullTest() {
        RequestUtil.getUserName(null);
    }

    //@Test
    public void getUserNameTest() {
        Subject subject = new Subject();
        RequestUtil.getUserName(subject);
    }

    //@Test(expected = SamlException.class)
    public void validateInResponseToWhenRequestInfoIsNotNullTest() throws SamlException {
        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getCachedRequestInfo();
                will(returnValue(requestInfo));

                one(basicMessageContext).getExternalRelayState();
                will(returnValue(relayState));

                one(requestInfo).getInResponseToId();
                will(returnValue(relayState));
            }
        });
        RequestUtil.validateInResponseTo(basicMessageContext, inResponseTo);
    }

    //@Test(expected = SamlException.class)
    public void validateInResponseToWhenRequestInfoIsNullTest() throws SamlException {
        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getCachedRequestInfo();
                will(returnValue(null));

                one(basicMessageContext).getExternalRelayState();
                will(returnValue(relayState));
            }
        });
        RequestUtil.validateInResponseTo(basicMessageContext, inResponseTo);
    }

    //@Test
    public void testIsUnprocessedAcsCookiePresent_nullProviderName() throws Exception {
        setProviderNameAndCookieValueExpectations(null, null);

        boolean result = RequestUtil.isUnprocessedAcsCookiePresent(ssoSamlServiceRef, iExtended, ssoRequest);
        assertFalse("Request with a missing ACS cookie should not be considered to have ACS cookies.", result);
    }

    //@Test
    public void testIsUnprocessedAcsCookiePresent_nullCookieValue() throws Exception {
        setProviderNameAndCookieValueExpectations(providerId, null);

        boolean result = RequestUtil.isUnprocessedAcsCookiePresent(ssoSamlServiceRef, iExtended, ssoRequest);
        assertFalse("Request with a missing ACS cookie should not be considered to have ACS cookies.", result);
    }

    //@Test
    public void testIsUnprocessedAcsCookiePresent_emptyCookieValue() throws Exception {
        final String value = "";
        setProviderNameAndCookieValueExpectations(providerId, value);

        boolean result = RequestUtil.isUnprocessedAcsCookiePresent(ssoSamlServiceRef, iExtended, ssoRequest);
        assertFalse("Request with an empty ACS cookie should not be considered to have ACS cookies.", result);
    }

    //@Test
    public void testIsUnprocessedAcsCookiePresent_missingServiceForProvider() throws Exception {
        final String value = "some value";
        setProviderNameAndCookieValueExpectations(providerId, value);

        mockery.checking(new Expectations() {
            {
                one(ssoSamlServiceRef).getService(providerId);
                will(returnValue(null));
            }
        });

        boolean result = RequestUtil.isUnprocessedAcsCookiePresent(ssoSamlServiceRef, iExtended, ssoRequest);
        assertFalse("Request should not be considered to have ACS cookies if service for provider is missing.", result);
    }

    //@Test
    public void testIsUnprocessedAcsCookiePresent_missingCache() throws Exception {
        final String value = "some value";
        setProviderNameAndCookieValueExpectations(providerId, value);

        mockery.checking(new Expectations() {
            {
                one(ssoSamlServiceRef).getService(providerId);
                will(returnValue(ssoService));
                one(ssoService).getAcsCookieCache(providerId);
                will(returnValue(null));
            }
        });

        boolean result = RequestUtil.isUnprocessedAcsCookiePresent(ssoSamlServiceRef, iExtended, ssoRequest);
        assertFalse("Request should not be considered to have ACS cookies if ACS cache for provider is missing.", result);
    }

    //@Test
    public void testIsUnprocessedAcsCookiePresent_cookieNotInCache() throws Exception {
        final String value = "some value";
        setProviderNameAndCookieValueExpectations(providerId, value);

        mockery.checking(new Expectations() {
            {
                one(ssoSamlServiceRef).getService(providerId);
                will(returnValue(ssoService));
                one(ssoService).getAcsCookieCache(providerId);
                will(returnValue(cache));
                one(cache).get(value);
                will(returnValue(null));
            }
        });

        boolean result = RequestUtil.isUnprocessedAcsCookiePresent(ssoSamlServiceRef, iExtended, ssoRequest);
        assertFalse("Request with an empty ACS cookie should not be considered to have ACS cookies.", result);
    }

    //@Test
    public void testIsUnprocessedAcsCookiePresent_cookieInCache() throws Exception {
        final String value = "some value";
        final String cookieCachedValue = "cached value";
        setProviderNameAndCookieValueExpectations(providerId, value);

        mockery.checking(new Expectations() {
            {
                one(ssoSamlServiceRef).getService(providerId);
                will(returnValue(ssoService));
                one(ssoService).getAcsCookieCache(providerId);
                will(returnValue(cache));
                one(cache).get(value);
                will(returnValue(cookieCachedValue));
            }
        });

        boolean result = RequestUtil.isUnprocessedAcsCookiePresent(ssoSamlServiceRef, iExtended, ssoRequest);
        assertTrue("Request with a matching value in the cache for the ACS cookie value should be considered to have ACS cookies.", result);
    }

    private void setProviderNameAndCookieValueExpectations(final String providerName, final String cookieValue) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).getProviderName();
                will(returnValue(providerName));
                one(iExtended).getCookieValueAsBytes(with(any(String.class)));
                will(returnValue((cookieValue == null) ? null : cookieValue.getBytes("UTF-8")));
            }
        });
    }

}
