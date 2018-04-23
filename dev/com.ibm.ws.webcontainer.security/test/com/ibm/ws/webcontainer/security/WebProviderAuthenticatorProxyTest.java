package com.ibm.ws.webcontainer.security;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

public class WebProviderAuthenticatorProxyTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private WebProviderAuthenticatorProxy webProviderAuthenticatorProxy;

    private AtomicServiceReference<SecurityService> securityServiceRef;
    private AtomicServiceReference<TAIService> taiServiceRef;
    private ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef;
    private WebAppSecurityConfig webAppSecurityConfig;
    private ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private SecurityMetadata securityMetadata;

    @Before
    public void setUp() throws Exception {
        securityServiceRef = mockery.mock(AtomicServiceReference.class, "securityServiceRef");
        taiServiceRef = mockery.mock(AtomicServiceReference.class, "taiServiceRef");
        interceptorServiceRef = mockery.mock(ConcurrentServiceReferenceMap.class, "interceptorServiceRef");
        webAppSecurityConfig = mockery.mock(WebAppSecurityConfig.class);
        webAuthenticatorRef = mockery.mock(ConcurrentServiceReferenceMap.class, "webAuthenticatorRef");
        webProviderAuthenticatorProxy = new WebProviderAuthenticatorProxy(securityServiceRef, taiServiceRef, interceptorServiceRef, webAppSecurityConfig, webAuthenticatorRef);
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        securityMetadata = mockery.mock(SecurityMetadata.class);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Ignore
    @Test
    public void testHandleJaspi() {
        WebRequest webRequest = new WebRequestImpl(request, response, securityMetadata, webAppSecurityConfig);
        HashMap<String, Object> props = null;
        AuthenticationResult authenticationResult = webProviderAuthenticatorProxy.handleJaspi(webRequest, props);
    }

}
