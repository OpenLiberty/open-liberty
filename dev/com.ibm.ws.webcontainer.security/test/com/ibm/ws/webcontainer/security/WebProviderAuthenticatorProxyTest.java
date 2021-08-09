package com.ibm.ws.webcontainer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;
import com.ibm.wsspi.security.token.SingleSignonToken;

public class WebProviderAuthenticatorProxyTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final AuthenticationResult SSO_CONTINUE = new AuthenticationResult(AuthResult.CONTINUE, "SSO said continue...");

    private WebProviderAuthenticatorProxy webProviderAuthenticatorProxy;

    private AtomicServiceReference<SecurityService> securityServiceRef;
    private SecurityService securityService;
    private AuthenticationService authenticationService;
    private AuthCacheService authCacheService;
    private AtomicServiceReference<TAIService> taiServiceRef;
    private ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef;
    private WebAppSecurityConfig webAppSecurityConfig;
    private ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef;
    private JaspiService jaspiService;
    private WebAuthenticator ssoAuthenticator;
    private PostParameterHelper postParameterHelper;
    private SSOCookieHelper ssoCookieHelper;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private SecurityMetadata securityMetadata;
    private WebRequest webRequest;
    private HashMap<String, Object> props;
    private Subject clientSubject;
    private Subject ssoSubject;
    private AuthenticationResult jaspicSUCCESS;
    private AuthenticationResult ssoSUCCESS;

    @Before
    public void setUp() throws Exception {
        securityServiceRef = mockery.mock(AtomicServiceReference.class, "securityServiceRef");
        securityService = mockery.mock(SecurityService.class, "securityService");
        authenticationService = mockery.mock(AuthenticationService.class);
        authCacheService = mockery.mock(AuthCacheService.class);
        taiServiceRef = mockery.mock(AtomicServiceReference.class, "taiServiceRef");
        interceptorServiceRef = mockery.mock(ConcurrentServiceReferenceMap.class, "interceptorServiceRef");
        webAppSecurityConfig = mockery.mock(WebAppSecurityConfig.class);
        webAuthenticatorRef = mockery.mock(ConcurrentServiceReferenceMap.class, "webAuthenticatorRef");
        jaspiService = mockery.mock(JaspiService.class);
        ssoAuthenticator = mockery.mock(WebAuthenticator.class, "ssoAuthenticator");
        postParameterHelper = mockery.mock(PostParameterHelper.class);
        ssoCookieHelper = mockery.mock(SSOCookieHelper.class);
        webProviderAuthenticatorProxy = new WebProviderAuthenticatorProxyTestDouble(securityServiceRef, taiServiceRef, interceptorServiceRef, webAppSecurityConfig, webAuthenticatorRef);
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        securityMetadata = mockery.mock(SecurityMetadata.class);
        webRequest = new WebRequestImpl(request, response, securityMetadata, webAppSecurityConfig);
        props = null;

        clientSubject = new Subject();
        clientSubject.getPrincipals().add(new WSPrincipal("clientSubject", "clientSubjectAccessId", "JASPIC"));
        ssoSubject = new Subject();
        ssoSubject.getPrincipals().add(new WSPrincipal("ssoSubject", "ssoAccessId", "SSO"));
        jaspicSUCCESS = new AuthenticationResult(AuthResult.SUCCESS, clientSubject);
        ssoSUCCESS = new AuthenticationResult(AuthResult.SUCCESS, ssoSubject);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testHandleJaspi_NoJaspiService() {
        withJaspiService(null);
        HashMap<String, Object> props = null;
        AuthenticationResult authenticationResult = webProviderAuthenticatorProxy.handleJaspi(webRequest, props);
        assertEquals("The authentication result status must be CONTINUE.", AuthResult.CONTINUE, authenticationResult.getStatus());
    }

    @Test
    public void testHandleJaspi_NoSSO_JaspicAuthenticationSUCCESS() {
        withAuthenticationsFlow(SSO_CONTINUE, jaspicSUCCESS);
        withAuditExpectations(null);
        withSuccessfulAuthenticationNoClientSubjectTokenUsage();

        AuthenticationResult authenticationResult = webProviderAuthenticatorProxy.handleJaspi(webRequest, props);

        assertResultWithAuditing(jaspicSUCCESS, authenticationResult);
    }

    @Test
    public void testHandleJaspi_NoSSO_JaspicAuthenticationSUCCESS_ProviderCommitedResponse() {
        withAuthenticationsFlow(SSO_CONTINUE, jaspicSUCCESS);
        withAuditExpectations(null);
        withSuccessfulAuthenticationExpectations(true);
        withTokenUsage(clientSubject, null);

        AuthenticationResult authenticationResult = webProviderAuthenticatorProxy.handleJaspi(webRequest, props);

        assertResultWithAuditing(jaspicSUCCESS, authenticationResult);
    }

    @Test
    public void testHandleJaspi_NoSSO_JaspicAuthenticationSUCCESS_ProviderRequestsJaspicSession() {
        withAuthenticationsFlow(SSO_CONTINUE, jaspicSUCCESS);
        withAuditExpectations(null);
        withSuccessfulAuthenticationExpectations(false);
        withJaspicSessionToken(clientSubject);

        AuthenticationResult authenticationResult = webProviderAuthenticatorProxy.handleJaspi(webRequest, props);

        assertResultWithAuditing(jaspicSUCCESS, authenticationResult);
    }

    @Test
    public void testHandleJaspi_NoSSO_JaspicAuthenticationFAILURE() {
        driveUnsuccessfulJaspicWithAudit(new AuthenticationResult(AuthResult.FAILURE, "Failure message"));
    }

    @Test
    public void testHandleJaspi_NoSSO_JaspicAuthentication401() {
        driveUnsuccessfulJaspicWithAudit(new AuthenticationResult(AuthResult.SEND_401, "realmName"));
    }

    @Test
    public void testHandleJaspi_NoSSO_JaspicAuthenticationREDIRECT() {
        driveUnsuccessfulJaspicWithAudit(new AuthenticationResult(AuthResult.REDIRECT, "loginUrl"));
    }

    @Test
    public void testHandleJaspi_NoSSO_JaspicAuthenticationRETURN() {
        driveUnsuccessfulJaspicWithAudit(new AuthenticationResult(AuthResult.RETURN, "details"));
    }

    @Test
    public void testHandleJaspi_NoSSO_JaspicAuthenticationCONTINUE() {
        AuthenticationResult authenticationResult = driveUnsuccessfulJaspic(new AuthenticationResult(AuthResult.CONTINUE, "No JASPIC provider."));
        assertNull("The audit credential type must not be set.", authenticationResult.getAuditCredType());
    }

    @Test
    public void testHandleJaspi_JaspicSessionSSO() {
        withAuthenticationsFlow(ssoSUCCESS, jaspicSUCCESS);
        withAuditExpectations(null);
        withTokenUsage(ssoSubject, AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JASPIC);
        withSuccessfulAuthenticationNoClientSubjectTokenUsage();

        AuthenticationResult authenticationResult = webProviderAuthenticatorProxy.handleJaspi(webRequest, props);

        assertResultWithAuditing(jaspicSUCCESS, authenticationResult);
        assertEquals("The JASPIC session subject must be set.", ssoSubject, webRequest.getProperties().get("javax.servlet.http.registerSession.subject"));
    }

    @Test
    public void testHandleJaspi_JaspicSessionSSO_NewAuthenticationDoesNotSetSessionSubject() {
        withNewAuthenticationFlow(ssoSUCCESS, jaspicSUCCESS);
        withAuditExpectations(null);
        withTokenUsage(ssoSubject, AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JASPIC);
        withSuccessfulAuthenticationNoClientSubjectTokenUsage();

        AuthenticationResult authenticationResult = webProviderAuthenticatorProxy.handleJaspi(webRequest, props);

        assertAuthenticationResultIsFromJaspic(jaspicSUCCESS, authenticationResult);
        assertNull("The JASPIC session subject must not be set.", webRequest.getProperties());
    }

    @Test
    public void testHandleJaspi_JSR375FormSSO() {
        withAuthenticationsFlow(ssoSUCCESS, jaspicSUCCESS);
        withAuditExpectations(null);
        withTokenUsage(ssoSubject, AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JSR375_FORM);
        withSuccessfulAuthenticationNoClientSubjectTokenUsage();

        AuthenticationResult authenticationResult = webProviderAuthenticatorProxy.handleJaspi(webRequest, props);

        assertResultWithAuditing(jaspicSUCCESS, authenticationResult);
        assertEquals("The JASPIC session subject must be set.", ssoSubject, webRequest.getProperties().get("javax.servlet.http.registerSession.subject"));
    }

    @Test
    public void testHandleJaspi_JSR375FormSSO_NewAuthenticationDoesNotSetSessionSubject() {
        withNewAuthenticationFlow(ssoSUCCESS, jaspicSUCCESS);
        withAuditExpectations(null);
        withTokenUsage(ssoSubject, AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JSR375_FORM);
        withSuccessfulAuthenticationNoClientSubjectTokenUsage();

        AuthenticationResult authenticationResult = webProviderAuthenticatorProxy.handleJaspi(webRequest, props);

        assertAuthenticationResultIsFromJaspic(jaspicSUCCESS, authenticationResult);
        assertNull("The JASPIC session subject must not be set.", webRequest.getProperties());
    }

    @Test
    public void testHandleJaspi_JaspicFormSSO() {
        withGeneralExpectations();
        withSSOResult(ssoSUCCESS);
        withSSOForJaspic(false);
        withClearCacheData(ssoSubject);
        doesNotAuthenticateWithJaspic();
        withAuditExpectations(null);
        withTokenUsage(ssoSubject, AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JASPIC_FORM);
        withSuccessfulAuthenticationExpectations(false);
        doesNotRemoveSSOCookie();
        createsLogoutCookies();

        AuthenticationResult authenticationResult = webProviderAuthenticatorProxy.handleJaspi(webRequest, props);

        assertEquals("The authentication result must be as reported by SSO.", ssoSUCCESS, authenticationResult);
        assertAuditCredentialType(authenticationResult);
        assertNull("The JASPIC session subject must not be set.", webRequest.getProperties());
    }

    @Test
    public void testHandleJaspi_SSO_SUCCESS() {
        withGeneralExpectations();
        withSSOResult(ssoSUCCESS);
        withSSOForJaspic(true);
        doesNotAuthenticateWithJaspic();
        withAuditExpectations(null);
        withTokenUsage(ssoSubject, null);
        withSuccessfulAuthenticationExpectations(false);
        doesNotRemoveSSOCookie();

        AuthenticationResult authenticationResult = webProviderAuthenticatorProxy.handleJaspi(webRequest, props);

        assertEquals("The authentication result must be as reported by SSO.", ssoSUCCESS, authenticationResult);
        assertNull("The JASPIC session subject must not be set.", webRequest.getProperties());
    }

    // Form Login Processing

    @Test
    public void testAuthenticate_JaspicAuthenticationSUCCESS() throws Exception {
        withFormLoginFlow(jaspicSUCCESS);
        withAuditExpectations(null);
        withSuccessfulAuthenticationExpectations(false);
        withTokenUsage(clientSubject, AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JASPIC_FORM);
        doesNotRemoveSSOCookie();
        registersSession(clientSubject);

        AuthenticationResult authenticationResult = webProviderAuthenticatorProxy.handleJaspi(webRequest, props);

        assertResultWithAuditing(jaspicSUCCESS, authenticationResult);
    }

    // Flows

    private void withAuthenticationsFlow(AuthenticationResult ssoResult, AuthenticationResult jaspicResult) {
        withGeneralExpectations();
        withAuthenticationResults(ssoResult, jaspicResult);
    }

    private void withNewAuthenticationFlow(AuthenticationResult ssoResult, AuthenticationResult jaspicResult) {
        withJaspiService(jaspiService);
        withNewAuthentication(true);
        withAuthenticationResults(ssoResult, jaspicResult);
    }

    private void driveUnsuccessfulJaspicWithAudit(AuthenticationResult jaspicResult) {
        AuthenticationResult authenticationResult = driveUnsuccessfulJaspic(jaspicResult);
        assertAuditCredentialType(authenticationResult);
    }

    private AuthenticationResult driveUnsuccessfulJaspic(AuthenticationResult jaspicResult) {
        withUnsuccessfulJaspic(jaspicResult);

        AuthenticationResult authenticationResult = webProviderAuthenticatorProxy.handleJaspi(webRequest, props);

        assertAuthenticationResultIsFromJaspic(jaspicResult, authenticationResult);
        return authenticationResult;
    }

    private void withUnsuccessfulJaspic(AuthenticationResult jaspicResult) {
        withAuthenticationsFlow(SSO_CONTINUE, jaspicResult);
        withAuditExpectations(null);

        doesNotRemoveSSOCookie();
    }

    private void withFormLoginFlow(final AuthenticationResult jaspicResult) throws Exception {
        withJaspiService(jaspiService);
        props = new HashMap<String, Object>();
        props.put("authType", "FORM_LOGIN");

        mockery.checking(new Expectations() {
            {
                one(jaspiService).authenticate(request, response, props);
                will(returnValue(jaspicResult));
            }
        });
    }

    // General Expectations

    private void withGeneralExpectations() {
        withJaspiService(jaspiService);
        withNewAuthentication(false);
    }

    private void withJaspiService(final JaspiService jaspiAuthenticator) {
        mockery.checking(new Expectations() {
            {
                allowing(webAuthenticatorRef).getService("com.ibm.ws.security.jaspi");
                will(returnValue(jaspiAuthenticator));
            }
        });
    }

    private void withNewAuthentication(final boolean newAuthentication) {
        mockery.checking(new Expectations() {
            {
                allowing(jaspiService).isProcessingNewAuthentication(request);
                will(returnValue(newAuthentication));
            }
        });
    }

    // Authentication Expectations

    private void withAuthenticationResults(AuthenticationResult ssoResult, AuthenticationResult jaspicResult) {
        withSSOResult(ssoResult);
        withJaspicResult(webRequest, jaspicResult);
        if (jaspicResult != null) {
            withSSOForJaspic(false);
        }
    }

    private void withSSOResult(final AuthenticationResult ssoResult) {
        mockery.checking(new Expectations() {
            {
                allowing(ssoAuthenticator).authenticate(webRequest);
                will(returnValue(ssoResult));
            }
        });
    }

    private void withJaspicResult(final WebRequest webRequest, final AuthenticationResult jaspicResult) {
        mockery.checking(new Expectations() {
            {
                one(jaspiService).authenticate(webRequest);
                will(returnValue(jaspicResult));
            }
        });
    }

    private void withSSOForJaspic(final boolean useLtpaSSOForJaspic) {
        mockery.checking(new Expectations() {
            {
                allowing(webAppSecurityConfig).isUseLtpaSSOForJaspic();
                will(returnValue(useLtpaSSOForJaspic));
            }
        });
    }

    private void withClearCacheData(Subject subject) {
        final String securityName = "user1";
        final String realm = "testRealm";
        mockery.checking(new Expectations() {
            {
                allowing(securityServiceRef).getService();
                will(returnValue(securityService));
                allowing(securityService).getAuthenticationService();
                will(returnValue(authenticationService));
                allowing(authenticationService).getAuthCacheService();
                will(returnValue(authCacheService));
                allowing(authCacheService).remove(realm + ":" + securityName);

            }
        });
        WSCredentialImpl credential = new WSCredentialImpl(realm, securityName, "uniqueSecurityName", "UNAUTHENTICATED", "primaryGroupId", "accessId", null, null);
        subject.getPublicCredentials().add(credential);
    }

    private void doesNotAuthenticateWithJaspic() {
        mockery.checking(new Expectations() {
            {
                never(jaspiService).authenticate(webRequest);
            }
        });
    }

    // Audit Expectations

    private void withAuditExpectations(final String authorizationHeader) {
        mockery.checking(new Expectations() {
            {
                allowing(request).getHeader("Authorization");
                will(returnValue(authorizationHeader));
                allowing(request).getParameter("j_username");
                will(returnValue(null));
            }
        });
    }

    // Authentication Success Expectations

    private void withSuccessfulAuthenticationNoClientSubjectTokenUsage() {
        withSuccessfulAuthenticationExpectations(false);
        withTokenUsage(clientSubject, null);
        removesSSOCookie();
    }

    private void withSuccessfulAuthenticationExpectations(boolean responseCommitted) {
        withCommittedResponse(responseCommitted);
        withCookieHelper();

        if (responseCommitted) {
            doesNotRestorePostParameters();
            doesNotRemoveSSOCookie();
        } else {
            restoresPostParameters();
        }
    }

    private void withCommittedResponse(final boolean committed) {
        mockery.checking(new Expectations() {
            {
                allowing(response).isCommitted();
                will(returnValue(committed));
            }
        });
    }

    private void restoresPostParameters() {
        mockery.checking(new Expectations() {
            {
                one(postParameterHelper).restore(request, response);
            }
        });
    }

    private void doesNotRestorePostParameters() {
        mockery.checking(new Expectations() {
            {
                never(postParameterHelper).restore(request, response);
            }
        });
    }

    private void withCookieHelper() {
        mockery.checking(new Expectations() {
            {
                allowing(webAppSecurityConfig).createSSOCookieHelper();
                will(returnValue(ssoCookieHelper));
            }
        });
    }

    private void withTokenUsage(final Subject subject, final String usage) {
        final String[] attributeValues = usage != null ? new String[] { usage } : null;
        final SingleSignonToken ssoToken = mockery.mock(SingleSignonToken.class, subject.getPrincipals().iterator().next().getName());

        mockery.checking(new Expectations() {
            {
                allowing(ssoCookieHelper).getDefaultSSOTokenFromSubject(subject);
                will(returnValue(ssoToken));
                allowing(ssoToken).getAttributes(AuthenticationConstants.INTERNAL_AUTH_PROVIDER);
                will(returnValue(attributeValues));
            }
        });
    }

    private void removesSSOCookie() {
        mockery.checking(new Expectations() {
            {
                one(ssoCookieHelper).removeSSOCookieFromResponse(response);
            }
        });
    }

    private void doesNotRemoveSSOCookie() {
        mockery.checking(new Expectations() {
            {
                never(ssoCookieHelper).removeSSOCookieFromResponse(response);
            }
        });
    }

    private void createsLogoutCookies() {
        mockery.checking(new Expectations() {
            {
                one(ssoCookieHelper).createLogoutCookies(request, response);
            }
        });
    }

    private void withJaspicSessionToken(final Subject subject) {
        Map<String, Object> jaspicProps = new HashMap<String, Object>();
        jaspicProps.put("javax.servlet.http.registerSession", "true");
        webRequest.setProperties(jaspicProps);
        registersSession(subject);
    }

    protected void registersSession(final Subject subject) {
        mockery.checking(new Expectations() {
            {
                one(ssoCookieHelper).addSSOCookiesToResponse(subject, request, response);
            }
        });
    }

    // Results' Assertions

    private void assertResultWithAuditing(AuthenticationResult jaspicResult, AuthenticationResult authenticationResult) {
        assertAuthenticationResultIsFromJaspic(jaspicResult, authenticationResult);
        assertAuditCredentialType(authenticationResult);
    }

    protected void assertAuthenticationResultIsFromJaspic(AuthenticationResult jaspicResult, AuthenticationResult authenticationResult) {
        assertEquals("The authentication result must be as reported by JASPIC.", jaspicResult, authenticationResult);
    }

    protected void assertAuditCredentialType(AuthenticationResult authenticationResult) {
        assertEquals("The audit credential type must be set.", "JASPIC", authenticationResult.getAuditCredType());
    }

    // Test Doubles

    private class WebProviderAuthenticatorProxyTestDouble extends WebProviderAuthenticatorProxy {

        public WebProviderAuthenticatorProxyTestDouble(AtomicServiceReference<SecurityService> securityServiceRef, AtomicServiceReference<TAIService> taiServiceRef,
                                                       ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef,
                                                       WebAppSecurityConfig webAppSecurityConfig, ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef) {
            super(securityServiceRef, taiServiceRef, interceptorServiceRef, webAppSecurityConfig, null, null, null, null, webAuthenticatorRef, null);
        }

        @Override
        public WebAuthenticator getSSOAuthenticator(WebRequest webRequest, String ssoCookieName) {
            return ssoAuthenticator;
        }

        @Override
        public void restorePostParams(WebRequest webRequest) {
            postParameterHelper.restore(webRequest.getHttpServletRequest(), webRequest.getHttpServletResponse());
        }

    }

}