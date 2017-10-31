package com.ibm.ws.security.javaeesec.cdi.beans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.callback.PasswordValidationCallback;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.CallerPrincipal;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.jaspi.JaspiMessageInfo;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class BasicHttpAuthenticationMechanismTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String IS_MANDATORY_POLICY = "javax.security.auth.message.MessagePolicy.isMandatory";

    private BasicHttpAuthenticationMechanism mechanism;
    private final String realmName = "My Basic Realm";
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpMessageContext httpMessageContext;
    private Subject clientSubject;
    private String authzHeader;
    @SuppressWarnings("rawtypes")
    private CDI cdi;
    private IdentityStoreHandler identityStoreHandler;
    private String principalName;
    private CallerPrincipal callerPrincipal;
    private Set<String> groups;
    private CredentialValidationResult validResult;
    private CallbackHandler callbackHandler;
    private ModulePropertiesProvider mpp;

    @Before
    public void setUp() {
        cdi = mockery.mock(CDI.class);
        mechanism = new BasicHttpAuthenticationMechanism() {
            @SuppressWarnings("rawtypes")
            @Override
            protected CDI getCDI() {
                return cdi;
            }
        };
        realmName = "My Basic Realm";
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        httpMessageContext = mockery.mock(HttpMessageContext.class);
        clientSubject = new Subject();
        authzHeader = "Basic " + Base64Coder.base64Encode("user1:user1pwd");
        identityStoreHandler = mockery.mock(IdentityStoreHandler.class);
        principalName = "user1";
        callerPrincipal = new CallerPrincipal(principalName);
        groups = new HashSet<String>();
        validResult = new CredentialValidationResult(callerPrincipal, groups);
        callbackHandler = mockery.mock(CallbackHandler.class);
        mpp = mockery.mock(ModulePropertiesProvider.class);
        mechanism.setMPP(mpp);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testValidateRequest() throws Exception {
        preInvokePathForProtectedResource(authzHeader).withIdentityStoreHandlerResult(validResult);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
        assertSubjectContents(realmName, principalName);
    }

    @Test
    public void testValidateRequestUniqueId() throws Exception {
        String storeId = "storeId";
        String callerUniqueId = "callerUniqueId";
        validResult = new CredentialValidationResult(storeId, callerPrincipal, "callerDn", callerUniqueId, groups);
        preInvokePathForProtectedResource(authzHeader).withIdentityStoreHandlerResult(validResult);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
        assertSubjectContents(storeId, callerUniqueId);
    }

    @Test
    public void testValidateRequestAndUnprotectedResource() throws Exception {
        preInvokePathForUnprotectedResource(authzHeader).withIdentityStoreHandlerResult(validResult);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
    }

    @Test
    public void testValidateRequestAuthenticatePath() throws Exception {
        authenticatePathForProtectedResource(authzHeader).withIdentityStoreHandlerResult(validResult);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
        assertSubjectContents(realmName, principalName);
    }

    @Test
    public void testValidateRequestAuthenticatePathAndUnprotectedResource() throws Exception {
        authenticatePathForUnprotectedResource(authzHeader).withIdentityStoreHandlerResult(validResult);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
    }

    @Test
    public void testValidateRequestWithInvalidResult() throws Exception {
        preInvokePathForProtectedResource(authzHeader).withIdentityStoreHandlerResult(CredentialValidationResult.INVALID_RESULT);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestWithInvalidResultAndUnprotectedResource() throws Exception {
        preInvokePathForUnprotectedResource(authzHeader).withIdentityStoreHandlerResult(CredentialValidationResult.INVALID_RESULT);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestAuthenticatePathWithInvalidResult() throws Exception {
        authenticatePathForProtectedResource(authzHeader).withIdentityStoreHandlerResult(CredentialValidationResult.INVALID_RESULT);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestAuthenticatePathWithInvalidResultAndUnprotectedResource() throws Exception {
        authenticatePathForUnprotectedResource(authzHeader).withIdentityStoreHandlerResult(CredentialValidationResult.INVALID_RESULT);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestWithAuthorizationHeaderWithoutValue() throws Exception {
        String badAuthzHeader = "Basic ";
        preInvokePathForProtectedResource(badAuthzHeader);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestWithAuthorizationHeaderWithSingleBlankValue() throws Exception {
        String badAuthzHeader = "Basic  ";
        preInvokePathForProtectedResource(badAuthzHeader);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestWithAuthorizationHeaderWithMultipleBlanksValue() throws Exception {
        String badAuthzHeader = "Basic     ";
        preInvokePathForProtectedResource(badAuthzHeader);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestWithAuthorizationHeaderWithoutColon() throws Exception {
        String badAuthzHeader = "Basic " + Base64Coder.base64Encode("headerWithoutColon");
        preInvokePathForProtectedResource(badAuthzHeader);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestWithAuthorizationHeaderWithoutUser() throws Exception {
        String badAuthzHeader = "Basic " + Base64Coder.base64Encode(":headerWithoutUser");
        preInvokePathForProtectedResource(badAuthzHeader);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestWithAuthorizationHeaderWithoutPassword() throws Exception {
        String badAuthzHeader = "Basic " + Base64Coder.base64Encode("headerWithoutPassword:");
        preInvokePathForProtectedResource(badAuthzHeader);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestWithoutAuthorizationHeader() throws Exception {
        preInvokePathForProtectedResource(null);
        setModulePropertiesProvider(realmName);
        assertMechanismChallenges();
    }

    @Test
    public void testValidateRequestWithoutAuthorizationHeaderAndUnprotectedResource() throws Exception {
        preInvokePathForUnprotectedResource(null).doesNotChallengeAuthorizationHeader();
        setModulePropertiesProvider(realmName);

        AuthenticationStatus status = mechanism.validateRequest(request, response, httpMessageContext);
        assertEquals("The AuthenticationStatus must be AuthenticationStatus.NOT_DONE.", AuthenticationStatus.NOT_DONE, status);
    }

    @Test
    public void testValidateRequestAuthenticatePathWithoutAuthorizationHeader() throws Exception {
        authenticatePathForProtectedResource(null);
        setModulePropertiesProvider(realmName);
        assertMechanismChallenges();
    }

    @Test
    public void testValidateRequestAuthenticatePathWithoutAuthorizationHeaderAndUnprotectedResource() throws Exception {
        authenticatePathForUnprotectedResource(null);
        setModulePropertiesProvider(realmName);
        assertMechanismChallenges();
    }

    @Test
    public void testValidateRequestFallbackToRegistry() throws Exception {
        preInvokePathForProtectedResource(authzHeader).withIdentityStoreHandlerResult(CredentialValidationResult.NOT_VALIDATED_RESULT);
        withRegistryPathExpectations(true);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
    }

    @Test
    public void testValidateRequestFallbackToRegistryInvalidUser() throws Exception {
        preInvokePathForProtectedResource(authzHeader).withIdentityStoreHandlerResult(CredentialValidationResult.NOT_VALIDATED_RESULT);
        withRegistryPathExpectations(false);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestAndUnprotectedResourceFallbackToRegistry() throws Exception {
        preInvokePathForUnprotectedResource(authzHeader).withIdentityStoreHandlerResult(CredentialValidationResult.NOT_VALIDATED_RESULT);
        withRegistryPathExpectations(true);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
    }

    @Test
    public void testValidateRequestAndUnprotectedResourceFallbackToRegistryInvalidUser() throws Exception {
        preInvokePathForUnprotectedResource(authzHeader).withIdentityStoreHandlerResult(CredentialValidationResult.NOT_VALIDATED_RESULT);
        withRegistryPathExpectations(false);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestAuthenticatePathFallbackToRegistry() throws Exception {
        authenticatePathForProtectedResource(authzHeader).withIdentityStoreHandlerResult(CredentialValidationResult.NOT_VALIDATED_RESULT);
        withRegistryPathExpectations(true);
        setModulePropertiesProvider(realmName);

        assertValidateRequestSUCCESS();
    }

    @Test
    public void testValidateRequestAuthenticatePathFallbackToRegistryInvalidUser() throws Exception {
        authenticatePathForProtectedResource(authzHeader).withIdentityStoreHandlerResult(CredentialValidationResult.NOT_VALIDATED_RESULT);
        withRegistryPathExpectations(false);
        setModulePropertiesProvider(realmName);

        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestAuthenticatePathAndUnprotectedResourceFallbackToRegistry() throws Exception {
        authenticatePathForUnprotectedResource(authzHeader).withIdentityStoreHandlerResult(CredentialValidationResult.NOT_VALIDATED_RESULT);
        withRegistryPathExpectations(true);
        setModulePropertiesProvider(realmName);

        assertValidateRequestSUCCESS();
    }

    @Test
    public void testValidateRequestAuthenticatePathAndUnprotectedResourceFallbackToRegistryInvalidUser() throws Exception {
        authenticatePathForUnprotectedResource(authzHeader).withIdentityStoreHandlerResult(CredentialValidationResult.NOT_VALIDATED_RESULT);
        withRegistryPathExpectations(false);
        setModulePropertiesProvider(realmName);

        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestWithIdentityStoreHandlerUnsatisfiedFallbackToRegistry() throws Exception {
        preInvokePathForProtectedResource(authzHeader).withoutIdentityStoreHandler(true, false);
        withRegistryPathExpectations(true);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
    }

    @Test
    public void testValidateRequestWithIdentityStoreHandlerAmbiguousFallbackToRegistry() throws Exception {
        preInvokePathForProtectedResource(authzHeader).withoutIdentityStoreHandler(false, true);
        withRegistryPathExpectations(true);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
    }

    @Test
    public void testValidateRequestWithIdentityStoreHandlerUnsatisfiedFallbackToRegistryInvalidUser() throws Exception {
        preInvokePathForProtectedResource(authzHeader).withoutIdentityStoreHandler(true, false);
        withRegistryPathExpectations(false);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestWithIdentityStoreHandlerAmbiguousFallbackToRegistryInvalidUser() throws Exception {
        preInvokePathForProtectedResource(authzHeader).withoutIdentityStoreHandler(false, true);
        withRegistryPathExpectations(false);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestWithIdentityStoreHandlerUnsatisfiedAndUnprotectedResourceFallbackToRegistry() throws Exception {
        preInvokePathForUnprotectedResource(authzHeader).withoutIdentityStoreHandler(true, false);
        withRegistryPathExpectations(true);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
    }

    @Test
    public void testValidateRequestWithIdentityStoreHandlerAmbiguousAndUnprotectedResourceFallbackToRegistry() throws Exception {
        preInvokePathForUnprotectedResource(authzHeader).withoutIdentityStoreHandler(false, true);
        withRegistryPathExpectations(true);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
    }

    @Test
    public void testValidateRequestWithIdentityStoreHandlerUnsatisfiedAndUnprotectedResourceFallbackToRegistryInvalidUser() throws Exception {
        preInvokePathForUnprotectedResource(authzHeader).withoutIdentityStoreHandler(true, false);
        withRegistryPathExpectations(false);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestWithIdentityStoreHandlerAmbiguousAndUnprotectedResourceFallbackToRegistryInvalidUser() throws Exception {
        preInvokePathForUnprotectedResource(authzHeader).withoutIdentityStoreHandler(false, true);
        withRegistryPathExpectations(false);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestAuthenticatePathWithIdentityStoreHandlerUnsatisfiedFallbackToRegistry() throws Exception {
        authenticatePathForUnprotectedResource(authzHeader).withoutIdentityStoreHandler(true, false);
        withRegistryPathExpectations(true);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
    }

    @Test
    public void testValidateRequestAuthenticatePathWithIdentityStoreHandlerAmbiguousFallbackToRegistry() throws Exception {
        authenticatePathForUnprotectedResource(authzHeader).withoutIdentityStoreHandler(false, true);
        withRegistryPathExpectations(true);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
    }

    @Test
    public void testValidateRequestAuthenticatePathWithIdentityStoreHandlerUnsatisfiedFallbackToRegistryInvalidUser() throws Exception {
        authenticatePathForUnprotectedResource(authzHeader).withoutIdentityStoreHandler(true, false);
        withRegistryPathExpectations(false);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestAuthenticatePathWithIdentityStoreHandlerAmbiguousFallbackToRegistryInvalidUser() throws Exception {
        authenticatePathForUnprotectedResource(authzHeader).withoutIdentityStoreHandler(false, true);
        withRegistryPathExpectations(false);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestAuthenticatePathWithIdentityStoreHandlerUnsatisfiedAndUnprotectedResourceFallbackToRegistry() throws Exception {
        authenticatePathForUnprotectedResource(authzHeader).withoutIdentityStoreHandler(true, false);
        withRegistryPathExpectations(true);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
    }

    @Test
    public void testValidateRequestAuthenticatePathWithIdentityStoreHandlerAmbiguousAndUnprotectedResourceFallbackToRegistry() throws Exception {
        authenticatePathForUnprotectedResource(authzHeader).withoutIdentityStoreHandler(false, true);
        withRegistryPathExpectations(true);
        setModulePropertiesProvider(realmName);
        assertValidateRequestSUCCESS();
    }

    @Test
    public void testValidateRequestAuthenticatePathWithIdentityStoreHandlerUnsatisfiedAndUnprotectedResourceFallbackToRegistryInvalidUser() throws Exception {
        authenticatePathForUnprotectedResource(authzHeader).withoutIdentityStoreHandler(true, false);
        withRegistryPathExpectations(false);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testValidateRequestAuthenticatePathWithIdentityStoreHandlerAmbiguousAndUnprotectedResourceFallbackToRegistryInvalidUser() throws Exception {
        authenticatePathForUnprotectedResource(authzHeader).withoutIdentityStoreHandler(false, true);
        withRegistryPathExpectations(false);
        setModulePropertiesProvider(realmName);
        assertValidateRequestFAILURE();
    }

    @Test
    public void testSecureResponse() throws Exception {
        AuthenticationStatus status = mechanism.secureResponse(request, response, httpMessageContext);
        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SUCCESS.", AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void testCleanSubject() {
        mockery.checking(new Expectations() {
            {
                one(httpMessageContext).cleanClientSubject();
            }
        });
        mechanism.cleanSubject(request, response, httpMessageContext);
    }

    private BasicHttpAuthenticationMechanismTest preInvokePathForProtectedResource(String authzHeader) {
        setHttpMessageContextExpectations(true).withAuthorizationHeader(authzHeader).withAuthenticationRequest(false);
        return this;
    }

    private BasicHttpAuthenticationMechanismTest preInvokePathForUnprotectedResource(String authzHeader) {
        setHttpMessageContextExpectations(false).withAuthorizationHeader(authzHeader).withAuthenticationRequest(false);
        return this;
    }

    private BasicHttpAuthenticationMechanismTest authenticatePathForProtectedResource(String authzHeader) {
        setHttpMessageContextExpectations(true).withAuthorizationHeader(authzHeader).withAuthenticationRequest(true);
        return this;
    }

    private BasicHttpAuthenticationMechanismTest authenticatePathForUnprotectedResource(String authzHeader) {
        setHttpMessageContextExpectations(false).withAuthorizationHeader(authzHeader).withAuthenticationRequest(true);
        return this;
    }

    private BasicHttpAuthenticationMechanismTest setHttpMessageContextExpectations(final boolean mandatory) {
        final MessageInfo messageInfo = createMessageInfo(mandatory);
        mockery.checking(new Expectations() {
            {
                one(httpMessageContext).getClientSubject();
                will(returnValue(clientSubject));
                one(httpMessageContext).getRequest();
                will(returnValue(request));
                allowing(httpMessageContext).getResponse();
                will(returnValue(response));
                allowing(httpMessageContext).getMessageInfo();
                will(returnValue(messageInfo));
                allowing(httpMessageContext).isProtected();
                will(returnValue(mandatory));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private MessageInfo createMessageInfo(boolean mandatory) {
        MessageInfo messageInfo = new JaspiMessageInfo(request, response);
        messageInfo.getMap().put(IS_MANDATORY_POLICY, Boolean.toString(mandatory));
        return messageInfo;
    }

    private BasicHttpAuthenticationMechanismTest withAuthenticationRequest(final boolean value) {
        mockery.checking(new Expectations() {
            {
                allowing(httpMessageContext).isAuthenticationRequest();
                will(returnValue(value));
            }
        });
        return this;
    }

    private BasicHttpAuthenticationMechanismTest withAuthorizationHeader(final String value) {
        mockery.checking(new Expectations() {
            {
                one(request).getHeader("Authorization");
                will(returnValue(value));
            }
        });
        return this;
    }

    private void withIdentityStoreHandlerResult(CredentialValidationResult result) {
        withIdentityStoreHandler(identityStoreHandler).withResult(result);
    }

    private BasicHttpAuthenticationMechanismTest withResult(final CredentialValidationResult result) {
        mockery.checking(new Expectations() {
            {
                one(identityStoreHandler).validate(with(new Matcher<BasicAuthenticationCredential>() {

                    @Override
                    public void describeTo(Description description) {}

                    @Override
                    public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {}

                    @Override
                    public boolean matches(Object obj) {
                        if (obj instanceof BasicAuthenticationCredential) {
                            BasicAuthenticationCredential basicAuthCredential = (BasicAuthenticationCredential) obj;
                            return "user1".equals(basicAuthCredential.getCaller()) && "user1pwd".equals(basicAuthCredential.getPasswordAsString());
                        } else {
                            return false;
                        }
                    }
                }));
                will(returnValue(result));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private BasicHttpAuthenticationMechanismTest withIdentityStoreHandler(final IdentityStoreHandler identityStoreHandler) {
        final Instance<IdentityStoreHandler> storeHandlerInstance = mockery.mock(Instance.class);

        mockery.checking(new Expectations() {
            {
                one(cdi).select(IdentityStoreHandler.class);
                will(returnValue(storeHandlerInstance));
                one(storeHandlerInstance).isUnsatisfied();
                will(returnValue(false));
                one(storeHandlerInstance).isAmbiguous();
                will(returnValue(false));
                one(storeHandlerInstance).get();
                will(returnValue(identityStoreHandler));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private BasicHttpAuthenticationMechanismTest withoutIdentityStoreHandler(final boolean unsatisfied, final boolean ambiguous) {
        final Instance<IdentityStoreHandler> storeHandlerInstance = mockery.mock(Instance.class);

        mockery.checking(new Expectations() {
            {
                one(cdi).select(IdentityStoreHandler.class);
                will(returnValue(storeHandlerInstance));
                allowing(storeHandlerInstance).isUnsatisfied();
                will(returnValue(unsatisfied));
                allowing(storeHandlerInstance).isAmbiguous();
                will(returnValue(ambiguous));
                never(storeHandlerInstance).get();
            }
        });
        return this;
    }

    private BasicHttpAuthenticationMechanismTest withRegistryPathExpectations(final boolean callbackResult) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(httpMessageContext).getHandler();
                will(returnValue(callbackHandler));
                one(callbackHandler).handle(with(new Matcher<Callback[]>() {

                    @Override
                    public void describeTo(Description description) {}

                    @Override
                    public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {}

                    @Override
                    public boolean matches(Object obj) {
                        if (obj instanceof Callback[]) {
                            Callback[] callbacks = (Callback[]) obj;
                            Callback callback = callbacks[0];
                            if (callback instanceof PasswordValidationCallback) {
                                PasswordValidationCallback pwcb = (PasswordValidationCallback) callback;
                                pwcb.setResult(callbackResult);
                                return "user1".equals(pwcb.getUsername()) && "user1pwd".equals(new String(pwcb.getPassword()));
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    }
                }));
            }
        });
        return this;
    }

    private BasicHttpAuthenticationMechanismTest withResponseStatus(final int responseStatus) {
        mockery.checking(new Expectations() {
            {
                one(response).setStatus(responseStatus);
            }
        });
        return this;
    }

    private BasicHttpAuthenticationMechanismTest setModulePropertiesProvider(final String realmName) {
        final Properties props = new Properties();
        props.put(JavaEESecConstants.REALM_NAME, realmName);
        mockery.checking(new Expectations() {
            {
                one(mpp).getAuthMechProperties(BasicHttpAuthenticationMechanism.class);
                will(returnValue(props));
            }
        });
        return this;
    }

    private void assertValidateRequestSUCCESS() throws AuthenticationException {
        withResponseStatus(HttpServletResponse.SC_OK);
        AuthenticationStatus status = mechanism.validateRequest(request, response, httpMessageContext);
        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SUCCESS.", AuthenticationStatus.SUCCESS, status);
    }

    private void assertValidateRequestFAILURE() throws AuthenticationException {
        withResponseStatus(HttpServletResponse.SC_FORBIDDEN);

        AuthenticationStatus status = mechanism.validateRequest(request, response, httpMessageContext);
        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SEND_FAILURE.", AuthenticationStatus.SEND_FAILURE, status);
    }

    private void assertMechanismChallenges() throws AuthenticationException {
        challengesAuthorizationHeader().withResponseStatus(HttpServletResponse.SC_UNAUTHORIZED);;
        AuthenticationStatus status = mechanism.validateRequest(request, response, httpMessageContext);
        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SEND_CONTINUE.", AuthenticationStatus.SEND_CONTINUE, status);
        assertEquals("The realm name must be set in the MessageInfo's map.", realmName,
                     httpMessageContext.getMessageInfo().getMap().get(AttributeNameConstants.WSCREDENTIAL_REALM));
    }

    private BasicHttpAuthenticationMechanismTest challengesAuthorizationHeader() {
        mockery.checking(new Expectations() {
            {
                one(response).setHeader("WWW-Authenticate", "Basic realm=\"" + realmName + "\"");
            }
        });
        return this;
    }

    private BasicHttpAuthenticationMechanismTest doesNotChallengeAuthorizationHeader() {
        mockery.checking(new Expectations() {
            {
                never(response).setHeader(with("WWW-Authenticate"), with(any(String.class)));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private void assertSubjectContents(String realmName, String uniqueId) {
        Hashtable<String, ?> customProperties = getSubjectHashtable();
        assertEquals("The assertion key must be set in the subject.", Boolean.TRUE, customProperties.get(AuthenticationConstants.INTERNAL_ASSERTION_KEY));
        assertEquals("The unique id must be set in the subject.", "user:" + realmName + "/" + uniqueId, customProperties.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID));
        assertEquals("The user id must be set in the subject.", principalName, customProperties.get(AttributeNameConstants.WSCREDENTIAL_USERID));
        assertEquals("The security name must be set in the subject.", principalName, customProperties.get(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME));
        List<String> subjectGroups = (List<String>) customProperties.get(AttributeNameConstants.WSCREDENTIAL_GROUPS);
        assertTrue("The groups must be set in the subject.", groups.containsAll(subjectGroups) && subjectGroups.containsAll(groups));
        assertEquals("The realm name must be set in the subject.", realmName, customProperties.get(AttributeNameConstants.WSCREDENTIAL_REALM));
        assertNull("The cache key must not be set in the subject.", customProperties.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY));
    }

    private Hashtable<String, ?> getSubjectHashtable() {
        String[] hashtableLoginProperties = { AttributeNameConstants.WSCREDENTIAL_UNIQUEID,
                                              AttributeNameConstants.WSCREDENTIAL_USERID,
                                              AttributeNameConstants.WSCREDENTIAL_SECURITYNAME,
                                              AttributeNameConstants.WSCREDENTIAL_GROUPS,
                                              AttributeNameConstants.WSCREDENTIAL_REALM,
                                              AttributeNameConstants.WSCREDENTIAL_CACHE_KEY,
                                              AuthenticationConstants.INTERNAL_ASSERTION_KEY };
        return new SubjectHelper().getHashtableFromSubject(clientSubject, hashtableLoginProperties);
    }

}
