/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.cdi.beans;

import static io.openliberty.security.oidcclientcore.authentication.JakartaOidcAuthorizationRequest.IS_CONTAINER_INITIATED_FLOW;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.security.javaeesec.cdi.beans.Utils;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.jakartasec.JakartaSec30Constants;
import io.openliberty.security.jakartasec.TestOpenIdAuthenticationMechanismDefinition;
import io.openliberty.security.oidcclientcore.client.Client;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException.ValidationResult;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import io.openliberty.security.oidcclientcore.exceptions.UnsupportedResponseTypeException;
import io.openliberty.security.oidcclientcore.http.OriginalResourceRequest;
import io.openliberty.security.oidcclientcore.storage.OidcClientStorageConstants;
import io.openliberty.security.oidcclientcore.token.JakartaOidcTokenRequest;
import io.openliberty.security.oidcclientcore.token.TokenResponse;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class OidcHttpAuthenticationMechanismTest {

    private static final String REDIRECTION_URL = "authzEndPointUrlWithQuery";
    private static final ProviderAuthenticationResult REDIRECTION_PROVIDER_AUTH_RESULT = createRedirectionResult(REDIRECTION_URL);
    private static final String END_SESSION_REDIRECT_URI = "endSessionUrl";
    private static final ProviderAuthenticationResult END_SESSION_REDIRECT_URI_PROVIDER_AUTH_RESULT = createRedirectionResult(END_SESSION_REDIRECT_URI);
    private static final ProviderAuthenticationResult AUTHORIZATION_REQUEST_FAILURE_PROVIDER_AUTH_RESULT = new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
    private static final ProviderAuthenticationResult LOCAL_LOGOUT_FAILURE_PROVIDER_AUTH_RESULT = new ProviderAuthenticationResult(AuthResult.FAILURE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    private static final AuthenticationResponseException AUTHENTICATION_RESPONSE_EXCEPTION_INVALID_RESULT = new AuthenticationResponseException(ValidationResult.INVALID_RESULT, "clientId", "nlsMessage");
    private static final TokenRequestException TOKEN_REQUEST_EXCEPTION = new TokenRequestException("clientId", "message");
    private static final String JASPIC_PROVIDER_PERFORMED_REQUEST_LOGOUT = "JASPIC_PROVIDER_PERFORMED_REQUEST_LOGOUT";

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static ProviderAuthenticationResult createRedirectionResult(String redirectionUrl) {
        return new ProviderAuthenticationResult(AuthResult.REDIRECT_TO_PROVIDER, HttpServletResponse.SC_OK, null, null, null, redirectionUrl);
    }

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpMessageContext httpMessageContext;
    private Client client;
    private Subject clientSubject;
    @SuppressWarnings("rawtypes")
    private CDI cdi;
    private ModulePropertiesProvider mpp;
    private Utils utils;
    private MessageInfo messageInfo;
    private Map<String, Object> messageInfoMap;
    private TokenResponse tokenResponse;

    private OidcClientConfig oidcClientConfig;
    private AuthenticationParameters authParams;
    private OriginalResourceRequest originalResourceRequest;
    private HttpSession httpSession;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        cdi = mockery.mock(CDI.class);
        mpp = mockery.mock(ModulePropertiesProvider.class);
        utils = mockery.mock(Utils.class);
        messageInfo = mockery.mock(MessageInfo.class);
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        httpMessageContext = mockery.mock(HttpMessageContext.class);
        client = mockery.mock(Client.class);
        tokenResponse = mockery.mock(TokenResponse.class);
        oidcClientConfig = mockery.mock(OidcClientConfig.class);
        authParams = mockery.mock(AuthenticationParameters.class);
        originalResourceRequest = mockery.mock(OriginalResourceRequest.class);
        httpSession = mockery.mock(HttpSession.class);
        messageInfoMap = new HashMap<String, Object>();
        clientSubject = new Subject();
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testValidateRequest_authenticationRequest_redirects() throws Exception {

        mechanismValidatesRequestForProtectedResource(); // protected resource, no new authentication
        containerInitatedFlowTrue();

        clientStartsFlow(REDIRECTION_PROVIDER_AUTH_RESULT);
        mechanismRedirectsTo(REDIRECTION_URL);
        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        AuthenticationStatus authenticationStatus = mechanism.validateRequest(request, response, httpMessageContext);

        assertEquals("The AuthenticationStatus must be SEND_CONTINUE.", AuthenticationStatus.SEND_CONTINUE, authenticationStatus);
    }

    @Test
    public void testValidateRequest_authenticationRequest_fails() throws Exception {
        mechanismValidatesRequestForProtectedResource();
        containerInitatedFlowTrue();
        clientStartsFlow(AUTHORIZATION_REQUEST_FAILURE_PROVIDER_AUTH_RESULT);
        mechanismSetsResponseUnauthorized();
        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        AuthenticationStatus authenticationStatus = mechanism.validateRequest(request, response, httpMessageContext);

        assertEquals("The AuthenticationStatus must be SEND_FAILURE.", AuthenticationStatus.SEND_FAILURE, authenticationStatus);
    }

    @Test
    public void testValidateRequest_unprotectedResource() throws Exception {
        mechanismValidatesRequestForUnprotectedResource(false, false); // not a protected resource, not a new authentication
        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        AuthenticationStatus authenticationStatus = mechanism.validateRequest(request, response, httpMessageContext);
        assertEquals("The AuthenticationStatus must be NOT_DONE.", AuthenticationStatus.NOT_DONE, authenticationStatus);
    }

    @Test
    public void testValidateRequest_unprotectedResource_callerAuthentication() throws Exception {
        mechanismValidatesRequestForUnprotectedResource(false, true); // not a protected resource
        containerInitatedFlowFalse();
        clientStartsFlow(REDIRECTION_PROVIDER_AUTH_RESULT);
        mechanismRedirectsTo(REDIRECTION_URL);
        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        AuthenticationStatus authenticationStatus = mechanism.validateRequest(request, response, httpMessageContext);
        assertEquals("The AuthenticationStatus must be SEND_CONTINUE.", AuthenticationStatus.SEND_CONTINUE, authenticationStatus);
    }

    @Test
    public void testValidateRequest_unprotectedResource_callerAuthentication_newAuthentication() throws Exception {
        mechanismValidatesRequestForUnprotectedResource(true, true); // not a protected resource, new authentication
        containerInitatedFlowFalse();
        clientStartsFlow(REDIRECTION_PROVIDER_AUTH_RESULT);
        mechanismRedirectsTo(REDIRECTION_URL);
        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        AuthenticationStatus authenticationStatus = mechanism.validateRequest(request, response, httpMessageContext);
        assertEquals("The AuthenticationStatus must be SEND_CONTINUE.", AuthenticationStatus.SEND_CONTINUE, authenticationStatus);
    }

    @Test
    public void testValidateRequest_callbackRequest() throws Exception {
        mechanismReceivesCallbackFromOP();
        clientContinuesFlow(createSuccessfulProviderAuthenticationResult());
        mechanismValidatesTokens(AuthenticationStatus.SUCCESS);
        withMessageInfo();
        withoutRestoreOriginalRequest();
        mechanismSetsResponseStatus(HttpServletResponse.SC_OK);

        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        AuthenticationStatus authenticationStatus = mechanism.validateRequest(request, response, httpMessageContext);

        assertEquals("The AuthenticationStatus must be SUCCESS.", AuthenticationStatus.SUCCESS, authenticationStatus);
    }

    /**
     * Expect SEND_CONTINUE rather than SEND_FAILURE since JaspiServiceImpl converts a SEND_FAILURE to AuthenticationResult(AuthResult.RETURN, detail)
     * and the WebContainerSecurityCollaboratorImpl allows access to unprotected resources for AuthResult.RETURN. SEND_CONTINUE will prevent this by properly
     * returning a 401 and not continue to the redirectUri.
     */
    @Test
    public void testValidateRequest_callbackRequest_continueFlowAuthenticationResponseException_fails() throws Exception {
        mechanismReceivesCallbackFromOP();
        clientContinuesFlowThrowsException(AUTHENTICATION_RESPONSE_EXCEPTION_INVALID_RESULT);
        mechanismSetsResponseStatus(HttpServletResponse.SC_UNAUTHORIZED);

        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        AuthenticationStatus authenticationStatus = mechanism.validateRequest(request, response, httpMessageContext);

        assertEquals("The AuthenticationStatus must be SEND_CONTINUE.", AuthenticationStatus.SEND_CONTINUE, authenticationStatus);
    }

    /**
     * Expect SEND_CONTINUE rather than SEND_FAILURE since JaspiServiceImpl converts a SEND_FAILURE to AuthenticationResult(AuthResult.RETURN, detail)
     * and the WebContainerSecurityCollaboratorImpl allows access to unprotected resources for AuthResult.RETURN. SEND_CONTINUE will prevent this by properly
     * returning a 401 and not continue to the redirectUri.
     */
    @Test
    public void testValidateRequest_callbackRequest_continueFlowTokenResponseException_fails() throws Exception {
        mechanismReceivesCallbackFromOP();
        clientContinuesFlowThrowsException(TOKEN_REQUEST_EXCEPTION);
        mechanismSetsResponseStatus(HttpServletResponse.SC_UNAUTHORIZED);

        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        AuthenticationStatus authenticationStatus = mechanism.validateRequest(request, response, httpMessageContext);

        assertEquals("The AuthenticationStatus must be SEND_CONTINUE.", AuthenticationStatus.SEND_CONTINUE, authenticationStatus);
    }

    @Test
    public void testValidateRequest_callbackRequest_restoreOriginalRequest() throws Exception {
        mechanismReceivesCallbackFromOP();
        clientContinuesFlow(createSuccessfulProviderAuthenticationResult());
        mechanismValidatesTokens(AuthenticationStatus.SUCCESS);
        withMessageInfo();
        withRestoreOriginalRequest();
        mechanismSetsResponseStatus(HttpServletResponse.SC_OK);

        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        AuthenticationStatus authenticationStatus = mechanism.validateRequest(request, response, httpMessageContext);

        assertEquals("The AuthenticationStatus must be SUCCESS.", AuthenticationStatus.SUCCESS, authenticationStatus);
    }

    @Test
    public void testCleanSubject() throws Exception {
        setModulePropertiesProvider();
        withRequestAndResponseFromHttpMessageContext();
        mechanismCheckingForExpiredToken(false);
        clientPerformsLogout(END_SESSION_REDIRECT_URI_PROVIDER_AUTH_RESULT);
        mechanismRedirectsTo(END_SESSION_REDIRECT_URI);

        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        mechanism.cleanSubject(request, response, httpMessageContext);
    }

    @Test
    public void testCleanSubject_localLogoutFailure() throws Exception {
        setModulePropertiesProvider();
        withRequestAndResponseFromHttpMessageContext();
        mechanismCheckingForExpiredToken(false);
        clientPerformsLogout(LOCAL_LOGOUT_FAILURE_PROVIDER_AUTH_RESULT);
        mechanismDoesNotRedirect();

        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        mechanism.cleanSubject(request, response, httpMessageContext);
    }

    @Test
    public void testCleanSubject_reauthorizationRequestFailure() throws Exception {
        setModulePropertiesProvider();
        withRequestAndResponseFromHttpMessageContext();
        mechanismCheckingForExpiredToken(false);
        clientPerformsLogout(AUTHORIZATION_REQUEST_FAILURE_PROVIDER_AUTH_RESULT);
        mechanismDoesNotRedirect();

        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        mechanism.cleanSubject(request, response, httpMessageContext);
    }

    @Test
    public void testCleanSubject_checkingExpiredToken_skip() throws Exception {
        setModulePropertiesProvider();
        withRequestAndResponseFromHttpMessageContext();
        mechanismCheckingForExpiredToken(true);
        mechanismDoesNotRedirect();

        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        mechanism.cleanSubject(request, response, httpMessageContext);
    }

    private void mechanismValidatesRequestForProtectedResource() {
        setModulePropertiesProvider();
        setHttpMessageContextExpectations(true, false, false);
        withoutJaspicSessionPrincipal();
        doesNotContainStateParam();
    }

    private void mechanismValidatesRequestForUnprotectedResource(boolean newAuthentication, boolean callerAuthentication) {
        setModulePropertiesProvider();
        setHttpMessageContextExpectations(false, newAuthentication, callerAuthentication);
        withoutJaspicSessionPrincipal();
        doesNotContainStateParam();
    }

    private void mechanismReceivesCallbackFromOP() {
        setModulePropertiesProvider();
        setHttpMessageContextExpectations(false, false, false);
        withoutJaspicSessionPrincipal();
        withCallbackRequest();
    }

    private void mechanismCheckingForExpiredToken(boolean checkingForExpiredToken) {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute("CHECKING_FOR_EXPIRED_TOKEN");
                will(returnValue(checkingForExpiredToken));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void setModulePropertiesProvider() {
        OpenIdAuthenticationMechanismDefinition openIdAuthenticationMechanismDefinition = TestOpenIdAuthenticationMechanismDefinition.getInstanceofAnnotation(null);
        final Properties props = new Properties();
        props.put(JakartaSec30Constants.OIDC_ANNOTATION, openIdAuthenticationMechanismDefinition);
        final Instance<ModulePropertiesProvider> mppi = mockery.mock(Instance.class, "mppi");
        mockery.checking(new Expectations() {
            {
                one(cdi).select(ModulePropertiesProvider.class);
                will(returnValue(mppi));
                one(mppi).get();
                will(returnValue(mpp));
            }
        });
    }

    private void setHttpMessageContextExpectations(boolean protectedResource, boolean newAuthentication, boolean callerAuthentication) {
        withRequestAndResponseFromHttpMessageContext();

        mockery.checking(new Expectations() {
            {
                allowing(httpMessageContext).getClientSubject();
                will(returnValue(clientSubject));
                allowing(httpMessageContext).isProtected();
                will(returnValue(protectedResource));
                allowing(httpMessageContext).getAuthParameters();
                will(returnValue(authParams));
                allowing(httpMessageContext).isAuthenticationRequest();
                will(returnValue(callerAuthentication));
                allowing(authParams).isNewAuthentication();
                will(returnValue(newAuthentication));
            }
        });
    }

    private void withRequestAndResponseFromHttpMessageContext() {
        mockery.checking(new Expectations() {
            {
                allowing(httpMessageContext).getRequest();
                will(returnValue(request));
                allowing(httpMessageContext).getResponse();
                will(returnValue(response));
            }
        });
    }

    private void withMessageInfo() {
        mockery.checking(new Expectations() {
            {
                allowing(httpMessageContext).getMessageInfo();
                will(returnValue(messageInfo));
                allowing(messageInfo).getMap();
                will(returnValue(messageInfoMap));
            }
        });
    }

    private void withRestoreOriginalRequest() {
        String state = "myState";
        String stateHash = io.openliberty.security.oidcclientcore.utils.Utils.getStrHashCode(state);
        String storedMethod = "R0VU"; // encoded GET

        mockery.checking(new Expectations() {
            {
                one(client).getOidcClientConfig();
                will(returnValue(oidcClientConfig));
                one(oidcClientConfig).isRedirectToOriginalResource();
                will(returnValue(true));
                one(oidcClientConfig).isUseSession();
                will(returnValue(true));
                one(request).getParameter(OpenIdConstant.STATE);
                will(returnValue(state));
                one(request).getSession();
                will(returnValue(httpSession));
                one(httpSession).getAttribute(OidcClientStorageConstants.WAS_OIDC_REQ_METHOD + stateHash);
                will(returnValue(storedMethod));
                one(oidcClientConfig).isUseSession();
                will(returnValue(true));
                one(httpMessageContext).setRequest(originalResourceRequest);
                one(messageInfo).setRequestMessage(originalResourceRequest);
            }
        });
    }

    private void withoutRestoreOriginalRequest() {
        mockery.checking(new Expectations() {
            {
                one(client).getOidcClientConfig();
                will(returnValue(oidcClientConfig));
                one(oidcClientConfig).isRedirectToOriginalResource();
                will(returnValue(false));
            }
        });
    }

    private void withoutJaspicSessionPrincipal() {
        mockery.checking(new Expectations() {
            {
                one(request).getUserPrincipal();
                will(returnValue(null));
            }
        });
    }

    private void withCallbackRequest() {
        mockery.checking(new Expectations() {
            {
                one(request).getParameter(OpenIdConstant.STATE);
                will(returnValue("aStateValue"));
            }
        });
    }

    private void clientStartsFlow(ProviderAuthenticationResult providerAuthenticationResult) throws UnsupportedResponseTypeException {
        mockery.checking(new Expectations() {
            {
                one(client).startFlow(request, response);
                will(returnValue(providerAuthenticationResult));
            }
        });
    }

    private void clientContinuesFlow(ProviderAuthenticationResult providerAuthenticationResult) throws UnsupportedResponseTypeException, AuthenticationResponseException, TokenRequestException {
        mockery.checking(new Expectations() {
            {
                one(client).continueFlow(request, response);
                will(returnValue(providerAuthenticationResult));
            }
        });
    }

    private void clientPerformsLogout(ProviderAuthenticationResult providerAuthenticationResult) {
        mockery.checking(new Expectations() {
            {
                one(client).logout(with(request), with(response), with(any(String.class)));
                will(returnValue(providerAuthenticationResult));
                one(request).setAttribute(JASPIC_PROVIDER_PERFORMED_REQUEST_LOGOUT, "true");
            }
        });
    }

    private void clientContinuesFlowThrowsException(Exception exception) throws UnsupportedResponseTypeException, AuthenticationResponseException, TokenRequestException {
        mockery.checking(new Expectations() {
            {
                one(client).continueFlow(request, response);
                will(throwException(exception));
            }
        });
    }

    private void mechanismRedirectsTo(String location) {
        mockery.checking(new Expectations() {
            {
                one(httpMessageContext).redirect(location);
                will(returnValue(AuthenticationStatus.SEND_CONTINUE));
            }
        });
    }

    private void mechanismDoesNotRedirect() {
        mockery.checking(new Expectations() {
            {
                never(httpMessageContext).redirect(with(any(String.class)));
            }
        });
    }

    private void mechanismSetsResponseUnauthorized() {
        mockery.checking(new Expectations() {
            {
                one(httpMessageContext).responseUnauthorized();
                will(returnValue(AuthenticationStatus.SEND_FAILURE));
            }
        });
    }

    private void mechanismValidatesTokens(AuthenticationStatus status) throws AuthenticationException {
        mockery.checking(new Expectations() {
            {
                // TODO: Check for issuer as the realm name
                allowing(cdi).select(OpenIdContext.class);
                will(returnValue(null)); // TODO: Return a mock for Instance<OpenIdContext> for coverage.
                one(utils).handleAuthenticate(with(cdi), with(JavaEESecConstants.DEFAULT_REALM), with(aNonNull(Credential.class)), with(clientSubject), with(httpMessageContext));
                will(returnValue(status));
            }
        });
    }

    private void mechanismSetsResponseStatus(int responseStatus) {
        mockery.checking(new Expectations() {
            {
                one(response).setStatus(responseStatus);
            }
        });
    }

    private void doesNotContainStateParam() {
        mockery.checking(new Expectations() {
            {
                one(request).getParameter(OpenIdConstant.STATE);
                will(returnValue(null));
            }
        });
    }

    private void containerInitatedFlowTrue() {
        mockery.checking(new Expectations() {
            {
                one(request).setAttribute(IS_CONTAINER_INITIATED_FLOW, true);
            }
        });
    }

    private void containerInitatedFlowFalse() {
        mockery.checking(new Expectations() {
            {
                one(request).setAttribute(IS_CONTAINER_INITIATED_FLOW, false);
            }
        });
    }

    private ProviderAuthenticationResult createSuccessfulProviderAuthenticationResult() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        customProperties.put(JakartaOidcTokenRequest.AUTH_RESULT_CUSTOM_PROP_TOKEN_RESPONSE, tokenResponse);
        return new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, null, null, customProperties, null);
    }

    private class TestOidcHttpAuthenticationMechanism extends OidcHttpAuthenticationMechanism {

        @SuppressWarnings("rawtypes")
        @Override
        protected CDI getCDI() {
            return cdi;
        }

        @Override
        protected Utils getUtils() {
            return utils;
        }

        @Override
        protected Client getClient(HttpServletRequest request) {
            return client;
        }

        @Override
        protected OriginalResourceRequest recreateOriginalResourceRequest(HttpServletRequest request, HttpServletResponse response, boolean useSession) {
            return originalResourceRequest;
        }

    }

}
