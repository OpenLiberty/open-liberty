/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.security.internal.BasicAuthAuthenticator;
import com.ibm.ws.webcontainer.security.internal.ChallengeReply;
import com.ibm.ws.webcontainer.security.internal.DenyReply;
import com.ibm.ws.webcontainer.security.internal.FormLoginAuthenticator;
import com.ibm.ws.webcontainer.security.internal.HTTPSRedirectHandler;
import com.ibm.ws.webcontainer.security.internal.RedirectReply;
import com.ibm.ws.webcontainer.security.internal.TAIChallengeReply;
import com.ibm.ws.webcontainer.security.internal.WebAppSecurityConfigImpl;
import com.ibm.ws.webcontainer.security.internal.WebAuthenticatorFactoryImpl;
import com.ibm.ws.webcontainer.security.internal.WebReply;
import com.ibm.ws.webcontainer.security.internal.WebSecurityCollaboratorException;
import com.ibm.ws.webcontainer.security.metadata.FormLoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.MatchResponse;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraint;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.security.metadata.WebResourceCollection;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.security.SecurityViolationException;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class WebAppSecurityCollaboratorImplTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private static final String HTTP_GET = "GET";
    private static final String HTTP_TRACE = "TRACE";
    private static final String AUTH_METHOD_BASIC = "BASIC";
    private static final String AUTH_METHOD_FORM = "FORM";
    private final static String REALM_NAME = "WebRealm";
    private final static String APP_NAME = "app1";
    private static final String SECURITY_ROLE = "aRole";
    private final static List<String> URL_PATTERN_DD_LIST = new ArrayList<String>();
    private final static String URL_PATTERN_DD = "urlPatternDD";
    private static final String UNAUTHENTICATED = "UNAUTHENTICATED";

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final HttpServletRequest commonReq = mock.mock(HttpServletRequest.class, "commonReq");
    private final IExtendedRequest extendedReq = mock.mock(IExtendedRequest.class, "extendedReq");
    private final HttpServletResponse commongResp = mock.mock(HttpServletResponse.class, "commongResp");
    private final ServiceReference<SecurityService> securityServiceRef = mock.mock(ServiceReference.class, "securityServiceRef");

    private final SecurityService securityService = mock.mock(SecurityService.class);
    private final UnauthenticatedSubjectService unauthSubjSrv = mock.mock(UnauthenticatedSubjectService.class);

    private final WebAuthenticatorFactory authenticatorFactory = new WebAuthenticatorFactoryImpl();

    private final TAIService taiService = mock.mock(TAIService.class, "commonTaiService");

    private final ServiceReference<TAIService> taiServiceRef = mock.mock(ServiceReference.class, "taiServiceRef");
    private final WebProviderAuthenticatorProxy providerAuthenticatorProxy = mock.mock(WebProviderAuthenticatorProxy.class, "providerAuthenticatorProxy");
    private final AuthorizationService authzService = mock.mock(AuthorizationService.class);
    private final UserRegistryService userRegistryService = mock.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);
    private final WebAuthenticator authenticator = mock.mock(WebAuthenticator.class);
    private final WebRequest commonWebRequest = mock.mock(WebRequest.class, "commonWebRequest");
    private final SecurityMetadata commonSecurityMetadata = mock.mock(SecurityMetadata.class, "commonSecurityMetadata");
    private final WebAppConfig webAppConfig = mock.mock(WebAppConfig.class);
    private final LoginConfiguration commonloginConfiguration = mock.mock(LoginConfiguration.class, "commonloginConfiguration");
    private final Map<String, Object> configProps = new HashMap<String, Object>();
    private final SubjectManager subjectManager = new SubjectManager();
    private final Vector<String> requiredRoles = new Vector<String>();
    private WebAppSecurityCollaboratorImpl secColl = null;
    private boolean setUnsupportedAuthMech = false;
    private AuthenticationResult authResult;
    private WebModuleMetaData wmmd;
    private final Subject delegationSubject = new Subject();
    private final BasicAuthAuthenticator basicAuthenticator = mock.mock(BasicAuthAuthenticator.class);
    private final FormLoginAuthenticator formLoginAuthenticator = mock.mock(FormLoginAuthenticator.class);
    private final WebAppSecurityConfig webAppSecurityConfig = mock.mock(WebAppSecurityConfig.class);
    private final PostParameterHelper postParameterHelper = mock.mock(PostParameterHelper.class);
    private final AtomicServiceReference<SecurityService> securityAtomicServiceRef = mock.mock(AtomicServiceReference.class, "securityAtomicServiceRef");
    private final AtomicServiceReference<TAIService> taiAtomicServiceRef = mock.mock(AtomicServiceReference.class, "taiAtomicServiceRef");
    private final WebAuthenticatorProxy authenticatorProxyForTest = new WebAuthenticatorProxyTestDouble(webAppSecurityConfig, postParameterHelper, securityAtomicServiceRef, taiAtomicServiceRef);
    final AuthenticationService authenticationService = mock.mock(AuthenticationService.class);
    final HttpServletRequest req = mock.mock(HttpServletRequest.class, "req");
    final HttpServletResponse res = mock.mock(HttpServletResponse.class, "res");

    class WebAppSecurityCollaboratorImplTestDouble extends WebAppSecurityCollaboratorImpl {
        public WebAppSecurityCollaboratorImplTestDouble() {
            super();
        }

        public WebAppSecurityCollaboratorImplTestDouble(SubjectHelper subjectHelper,
                                                        SubjectManager subjectManager,
                                                        HTTPSRedirectHandler httpsRedirectHandler) {
            super(subjectHelper, subjectManager, httpsRedirectHandler);
        }

        @Override
        protected WebAuthenticatorProxy getWebAuthenticatorProxy() {
            return authenticatorProxyForTest;
        }

        /** {@inheritDoc} */
        @Override
        public boolean unsupportedAuthMech() {
            if (setUnsupportedAuthMech)
                return true;
            else
                return false;
        }

        /**
         * {@inheritDoc} Override the normal flow which looks on the thread.
         * We want to return our mock in this case.
         */
        @Override
        public SecurityMetadata getSecurityMetadata() {
            return commonSecurityMetadata;
        }
    }

    class RedirectReplyTestDouble extends RedirectReply {
        RedirectReplyTestDouble(String url) {
            super(url, null);
        }

        /** {@inheritDoc} */
        @Override
        public int getStatusCode() {
            return HttpServletResponse.SC_SEE_OTHER;
        }
    }

    class InternalErrorReply extends WebReply {

        protected InternalErrorReply() {
            super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        @Override
        public void writeResponse(HttpServletResponse rsp) throws IOException {
            sendError(rsp);
        }
    }

    class HTTPSRedirectHandlerDouble extends HTTPSRedirectHandler {
        private final boolean shouldRedirect;

        HTTPSRedirectHandlerDouble(boolean shouldRedirect) {
            this.shouldRedirect = shouldRedirect;
        }

        @Override
        public boolean shouldRedirectToHttps(WebRequest webRequest) {
            return shouldRedirect;
        }

        @Override
        public WebReply getHTTPSRedirectWebReply(HttpServletRequest req) {
            return new RedirectReply("", null);
        }
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        URL_PATTERN_DD_LIST.add(URL_PATTERN_DD);
    }

    @Before
    public void setUp() throws Exception {
        subjectManager.clearSubjects();
        configProps.put(WebAppSecurityConfigImpl.CFG_KEY_FAIL_OVER_TO_BASICAUTH, false);
        configProps.put(WebAppSecurityConfigImpl.CFG_KEY_USE_AUTH_DATA_FOR_UNPROTECTED, true);

        createSecurityServiceExpectations();
        createComponentContextExpectations();
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(WebAppSecurityCollaboratorImpl.KEY_TAI_SERVICE, taiServiceRef);
                will(returnValue(taiService));
                allowing(taiService).getTais(false);

                allowing(userRegistryService).isUserRegistryConfigured();
                will(returnValue(true));
                allowing(webAppConfig).getApplicationName();
                will(returnValue(APP_NAME));
                allowing(authenticationService).getAuthCacheService();
            }
        });

        // secColl needs to be activated to be useful, this simulates DS
        // creating and starting the class
        secColl = new WebAppSecurityCollaboratorImpl();
        secColl.setAuthenticatorFactory(authenticatorFactory);
        secColl.setSecurityService(securityServiceRef);
        secColl.setTaiService(taiServiceRef);
        secColl.setUnauthenticatedSubjectService(unauthSubjSrv);
        secColl.activate(cc, configProps);
    }

    private void createComponentContextExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(WebAppSecurityCollaboratorImpl.KEY_SECURITY_SERVICE, securityServiceRef);
                will(returnValue(securityService));
            }
        });
    }

    private void createAuthenticatorProxyCommonExpectations() {
        createTAIServiceExpectations();
        createPostParameterHelperExpectations();
    }

    private void createTAIServiceExpectations() {
        final Map<String, TrustAssociationInterceptor> tais = new HashMap<String, TrustAssociationInterceptor>();
        mock.checking(new Expectations() {
            {
                allowing(taiAtomicServiceRef).getService();
                will(returnValue(taiService));
                allowing(taiService).getTais(false);
                will(returnValue(tais));
            }
        });
    }

    private void createSecurityServiceExpectations() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(securityAtomicServiceRef).getService();
                will(returnValue(securityService));
                allowing(securityService).getAuthenticationService();
                will(returnValue(authenticationService));
                allowing(authenticationService).delegate(SECURITY_ROLE, APP_NAME);
                will(returnValue(delegationSubject));
                allowing(securityService).getAuthorizationService();
                will(returnValue(authzService));
                allowing(securityService).getUserRegistryService();
                will(returnValue(userRegistryService));
                allowing(userRegistryService).getUserRegistry();
                will(returnValue(userRegistry));
            }
        });
    }

    private void createPostParameterHelperExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(postParameterHelper).restore(with(any(HttpServletRequest.class)), with(any(HttpServletResponse.class)));
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        mock.assertIsSatisfied();
    }

    private void setTestWebModuleMetaDataOnThread(final WebModuleMetaData webModuleMetaData) {
        WebComponentMetaData webComponentMetaData = createTestWebComponentMetaData(webModuleMetaData);
        ComponentMetaDataAccessorImpl cmda = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        cmda.beginContext(webComponentMetaData);
    }

    private WebComponentMetaData createTestWebComponentMetaData(final WebModuleMetaData webModuleMetaData) {
        final WebComponentMetaData webComponentMetaData = mock.mock(WebComponentMetaData.class);
        mock.checking(new Expectations() {
            {
                allowing(webComponentMetaData).getModuleMetaData();
                will(returnValue(webModuleMetaData));
                allowing(webModuleMetaData).getConfiguration();
                will(returnValue(webAppConfig));
            }
        });
        return webComponentMetaData;
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#preInvoke()}.
     *
     * @throws SecurityViolationException
     */
    @Test
    public void preInvoke() throws SecurityViolationException {
        subjectManager.setCallerSubject(new Subject());
        subjectManager.setInvocationSubject(new Subject());
        assertNull("preinvoke with no args should return null", secColl.preInvoke());
        assertNull("invocationSubject should be null", subjectManager.getInvocationSubject());
        assertNull("receivedSubject should be null", subjectManager.getCallerSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#preInvoke(java.lang.String)}.
     *
     * @throws IOException
     * @throws SecurityViolationException
     */
    @Test
    public void preInvokeString() throws Exception {
        wmmd = createWebModuleMetaDataMock();
        setTestWebModuleMetaDataOnThread(wmmd);
        final Subject subject = createHashtableSubject();

        createTestSpecificUserRegistryExpectations("someRealm");
        mock.checking(new Expectations() {
            {
                one(unauthSubjSrv).getUnauthenticatedSubject();
                will(returnValue(subject));
            }
        });

        WebSecurityContext secContext = (WebSecurityContext) secColl.preInvoke("servletName");
        assertSame("Thread invocationSubject should be the delegation Subject",
                   delegationSubject, subjectManager.getInvocationSubject());
        assertNull("Saved receivedSubject should be null, but is: " + secContext.getReceivedSubject(),
                   secContext.getReceivedSubject());
        assertNull("Saved invocationSubject should be null, but is: " + secContext.getReceivedSubject(),
                   secContext.getReceivedSubject());
    }

    /**
     * @return
     */
    private Subject createHashtableSubject() {
        final Subject subject = new Subject();
        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, UNAUTHENTICATED);
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID,
                      AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER, "someRealm", UNAUTHENTICATED));
        subject.getPublicCredentials().add(hashtable);
        return subject;
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#postInvoke(java.lang.Object)}.
     *
     * @throws ServletException
     */
    @Test
    public void postInvoke_null() throws ServletException {
        Subject invSubject = new Subject();
        Subject recvSubject = new Subject();
        subjectManager.setCallerSubject(recvSubject);
        subjectManager.setInvocationSubject(invSubject);

        secColl.postInvoke(null);

        assertSame("invocationSubject was altered and should not have been",
                   invSubject, subjectManager.getInvocationSubject());
        assertSame("receivedSubject was altered and should not have been",
                   recvSubject, subjectManager.getCallerSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#postInvoke(java.lang.Object)}.
     *
     * @throws ServletException
     */
    @Test
    public void postInvoke() throws ServletException {
        Subject invSubject = new Subject();
        Subject recvSubject = new Subject();
        WebSecurityContext secContext = new WebSecurityContext(invSubject, recvSubject);

        secColl.postInvoke(secContext);

        assertSame("invocationSubject was not altered and should have been",
                   invSubject, subjectManager.getInvocationSubject());
        assertSame("receivedSubject was not altered and should have been",
                   recvSubject, subjectManager.getCallerSubject());
    }

    /**
     * Test method for
     * {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#preInvoke(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, boolean)}
     * .
     *
     * @throws Exception
     */
    @Test
    public void preInvokeMain() throws Exception {
        wmmd = createWebModuleMetaDataMock();
        setTestWebModuleMetaDataOnThread(wmmd);

        final Subject invSubject = new Subject();
        final Subject recvSubject = new Subject();
        subjectManager.setInvocationSubject(invSubject);
        subjectManager.setCallerSubject(recvSubject);

        createTestSpecificHttpServletRequestExpectations(commonReq, HTTP_GET, "/", "unprotectedPreInvokeMainTestServlet",
                                                         false, "defaultMethod");
        configProps.put("useAuthenticationDataForUnprotectedResource", false);
        secColl.modified(configProps);

        mock.checking(new Expectations() {
            {
                allowing(userRegistry).getRealm();
                one(taiService).isInvokeForUnprotectedURI();
                will(returnValue(false));
            }
        });

        WebSecurityContext secContext = (WebSecurityContext) secColl.preInvoke(commonReq, commongResp, "test", true);
        assertSame("Thread invocationSubject should be delegation Subject",
                   delegationSubject, subjectManager.getInvocationSubject());
        assertEquals("invocationSubject was not set",
                     invSubject, secContext.getInvokedSubject());
        assertEquals("receivedSubject was not set",
                     recvSubject, secContext.getReceivedSubject());
    }

    /**
     * Test method for
     * {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#preInvoke(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, boolean)}
     * .
     *
     * @throws Exception a SecurityViolationException is expected because the checkDefaultMethod is set to <code>TRACE</code>.
     */
    @Test(expected = SecurityViolationException.class)
    public void preInvokeMain_throwException() throws Exception {
        final Subject subject = createHashtableSubject();
        wmmd = createWebModuleMetaDataMock();
        setTestWebModuleMetaDataOnThread(wmmd);
        createTestSpecificHttpServletRequestExpectations(commonReq, HTTP_GET, "/", "unprotectedPreInvokeMain_throwExceptionServlet",
                                                         false, HTTP_TRACE);
        createTestSpecificUserRegistryExpectations("someRealm");
        mock.checking(new Expectations() {
            {
                one(unauthSubjSrv).getUnauthenticatedSubject();
                will(returnValue(subject));
            }
        });
        secColl.preInvoke(commonReq, commongResp, "test", true);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#handleException(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, com.ibm.ws.webcontainer.security.internal.WebSecurityCollaboratorException)}
     * .
     *
     * @throws IOException
     * @throws ServletException
     * @throws ClassCastException
     */
    @Test
    public void testHandleException_challenge() throws ClassCastException, ServletException, IOException {
        String realm = "realm";
        createTestSpecificHttpServletResponseExpectations(commongResp);
        ChallengeReply reply = new ChallengeReply(realm);
        WebSecurityCollaboratorException wse = new WebSecurityCollaboratorException(reply);
        secColl.handleException(commonReq, commongResp, wse);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#handleException(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, com.ibm.ws.webcontainer.security.internal.WebSecurityCollaboratorException)}
     * .
     *
     * @throws IOException
     */
    @Test
    public void testHandleException_deny() throws IOException {
        final String reason = "reason";
        createHttpServletResponseErrorExpectations(commongResp, 403, reason);
        DenyReply reply = new DenyReply(reason);
        WebSecurityCollaboratorException wse = new WebSecurityCollaboratorException(reply);
        try {
            secColl.handleException(commonReq, commongResp, wse);
        } catch (ServletException e) {
            fail("Unexpected ServletException: " + e);
            e.printStackTrace();
        } catch (IOException ioe) {
            fail("Unexpected IOException: " + ioe);
            ioe.printStackTrace();
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#handleException(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, com.ibm.ws.webcontainer.security.internal.WebSecurityCollaboratorException)}
     * .
     *
     * @throws IOException
     * @throws ServletException
     * @throws ClassCastException
     */
    @Test
    public void testHandleException_redirect() throws IOException, ClassCastException, ServletException {
        final String url = "url";
        createHttpServletResponseRedirectionExpectations(commongResp, url, "");
        RedirectReply reply = new RedirectReply(url, null);
        WebSecurityCollaboratorException wse = new WebSecurityCollaboratorException("redirect", reply);
        secColl.handleException(commonReq, commongResp, wse);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#handleException(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, com.ibm.ws.webcontainer.security.internal.WebSecurityCollaboratorException)}
     * .
     *
     * @throws IOException
     * @throws ServletException
     * @throws ClassCastException
     */
    @Test
    public void testHandleException_redirect302() throws IOException, ClassCastException, ServletException {
        final String url = "url";
        createHttpServletResponseRedirectionExpectations(commongResp, url, "");
        RedirectReplyTestDouble reply = new RedirectReplyTestDouble(url);
        WebSecurityCollaboratorException wse = new WebSecurityCollaboratorException("redirect", reply);
        secColl.handleException(commonReq, commongResp, wse);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#handleException(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, com.ibm.ws.webcontainer.security.internal.WebSecurityCollaboratorException)}
     * .
     */
    @Test(expected = ServletException.class)
    public void testHandleException_InternalServerError() throws Exception {
        InternalErrorReply reply = new InternalErrorReply();
        WebSecurityCollaboratorException wse = new WebSecurityCollaboratorException(reply);
        HttpServletRequest req = createHttpServletRequest();
        createTestSpecificHttpServletRequestExpectations(req, HTTP_GET, "/", "testServlet", "http://testserver.ibm.com:1234/test/testServlet", "key=value", true, "defaultMethod");
        secColl.handleException(req, commongResp, wse);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#authenticateRequest(com.ibm.ws.webcontainer.security.WebRequest)}.
     */
    @Test
    public void testAuthenticateRequest() {
        createAuthenticatorProxyCommonExpectations();
        FormLoginConfiguration formLoginConfiguration = createFormLoginConfiguration();
        LoginConfiguration loginConfiguration = createLoginConfiguration(AUTH_METHOD_FORM, formLoginConfiguration);
        SecurityMetadata securityMetadata = createSecurityMetadata(loginConfiguration);
        HttpServletRequest req = createHttpServletRequest();
        HttpServletResponse resp = createHttpServletResponse();
        final WebRequest webRequest = createWebRequest(securityMetadata, req, resp, true);
        final AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, (String) null);
        createFormLoginAuthenticatorExpectations(webRequest, authResult);

        secColl = new WebAppSecurityCollaboratorImplTestDouble();
        mock.checking(new Expectations() {
            {
                allowing(taiService).getTais(true);
                allowing(providerAuthenticatorProxy).authenticate(webRequest);
                will(returnValue(authResult));
            }
        });
        AuthenticationResult newAuthResult = secColl.authenticateRequest(webRequest);

        assertEquals("Authentication result is not correct: ", authResult, newAuthResult);
    }

    private FormLoginConfiguration createFormLoginConfiguration() {
        final FormLoginConfiguration formLoginConfiguration = mock.mock(FormLoginConfiguration.class);
        return formLoginConfiguration;
    }

    private LoginConfiguration createLoginConfiguration(final String authenticationMethod, final FormLoginConfiguration formLoginConfiguration) {
        final LoginConfiguration loginConfiguration = mock.mock(LoginConfiguration.class);
        mock.checking(new Expectations() {
            {
                allowing(loginConfiguration).getAuthenticationMethod();
                will(returnValue(authenticationMethod));
                allowing(loginConfiguration).getFormLoginConfiguration();
                will(returnValue(formLoginConfiguration));
            }
        });
        return loginConfiguration;
    }

    private SecurityMetadata createSecurityMetadata(final LoginConfiguration loginConfiguration) {
        final SecurityMetadata securityMetadata = mock.mock(SecurityMetadata.class);
        mock.checking(new Expectations() {
            {
                allowing(securityMetadata).getLoginConfiguration();
                will(returnValue(loginConfiguration));

            }
        });
        return securityMetadata;
    }

    private HttpServletRequest createHttpServletRequest() {
        final HttpServletRequest req = mock.mock(HttpServletRequest.class);
        mock.checking(new Expectations() {
            {
                allowing(req).getCookies();
                will(returnValue(null));
                allowing(taiService).getTais(true);
            }
        });
        return req;
    }

    private void createTestSpecificHttpServletRequestExpectations(final HttpServletRequest req, final String httpMethod,
                                                                  final String servletPath, final String pathInfo,
                                                                  final boolean secure, final String checkDefaultMethod) {
        createTestSpecificHttpServletRequestExpectations(req, httpMethod, servletPath, pathInfo, null, null, secure, checkDefaultMethod);
    }

    private void createTestSpecificHttpServletRequestExpectations(final HttpServletRequest req, final String httpMethod,
                                                                  final String servletPath, final String pathInfo,
                                                                  final String requestURL, final String queryString,
                                                                  final boolean secure, final String checkDefaultMethod) {
        mock.checking(new Expectations() {
            {
                allowing(req).getMethod();
                will(returnValue(httpMethod));
                allowing(req).getServletPath();
                will(returnValue(servletPath));
                allowing(req).getPathInfo();
                will(returnValue(pathInfo));
                allowing(req).getRequestURL();
                will(returnValue(requestURL == null ? new StringBuffer() : new StringBuffer(requestURL)));
                allowing(req).getQueryString();
                will(returnValue(queryString));
                allowing(req).isSecure();
                will(returnValue(secure));
                allowing(req).getAttribute("com.ibm.ws.webcontainer.security.checkdefaultmethod");
                will(returnValue(checkDefaultMethod));
                allowing(req).getAttribute("com.ibm.ws.webcontainer.security.webmodulemetadata");
                will(returnValue(null));
                allowing(req).setAttribute("com.ibm.ws.webcontainer.security.webmodulemetadata", wmmd);
            }
        });
    }

    private HttpServletResponse createHttpServletResponse() {
        final HttpServletResponse resp = mock.mock(HttpServletResponse.class);
        return resp;
    }

    private void createTestSpecificHttpServletResponseExpectations(final HttpServletResponse resp) {
        mock.checking(new Expectations() {
            {
                allowing(resp).setStatus(401);
                allowing(resp).setHeader("WWW-Authenticate", "Basic realm=\"realm\"");
            }
        });
    }

    private void createHttpServletResponseErrorExpectations(final HttpServletResponse resp, final int error, final String reason) throws IOException {
        mock.checking(new Expectations() {
            {
                allowing(resp).sendError(error, reason);
            }
        });
    }

    private void createHttpServletResponseRedirectionExpectations(final HttpServletResponse resp, final String url, final String redirect) throws IOException {
        mock.checking(new Expectations() {
            {
                allowing(resp).isCommitted();
                will(returnValue(false));
                allowing(resp).encodeURL(url);
                allowing(resp).sendRedirect(redirect);
            }
        });
    }

    /**
     * Creates a WebRequest mock object with its expectations.
     * Only expectations related to the WebRequest mock object are placed here.
     */
    private WebRequest createWebRequest(final SecurityMetadata securityMetadata,
                                        final HttpServletRequest req,
                                        final HttpServletResponse resp,
                                        final boolean formLoginRedirectEnabled) {
        final WebRequest webRequest = mock.mock(WebRequest.class);
        final LoginConfiguration loginConfig = securityMetadata.getLoginConfiguration();
        final FormLoginConfiguration formLoginConfiguration;
        if (loginConfig != null) {
            formLoginConfiguration = loginConfig.getFormLoginConfiguration();
        } else {
            formLoginConfiguration = null;
        }

        mock.checking(new Expectations() {
            {
                allowing(webRequest).getSecurityMetadata();
                will(returnValue(securityMetadata));
                allowing(webRequest).getHttpServletRequest();
                will(returnValue(req));
                allowing(webRequest).getHttpServletResponse();
                will(returnValue(resp));
                allowing(webRequest).getLoginConfig();
                will(returnValue(loginConfig));
                allowing(webRequest).getFormLoginConfiguration();
                will(returnValue(formLoginConfiguration));
                allowing(webRequest).isUnprotectedURI();
                allowing(taiService).getTais(false);
            }
        });
        return webRequest;
    }

    private void createTestSpecificWebRequestExpectations(final WebRequest webRequest,
                                                          final List<String> requiredRoles,
                                                          final boolean accessPrecluded) {
        mock.checking(new Expectations() {
            {
                allowing(webRequest).getRequiredRoles();
                will(returnValue(requiredRoles));
                allowing(webRequest).isAccessPrecluded();
                will(returnValue(accessPrecluded));
                allowing(webRequest).getApplicationName();
                will(returnValue(APP_NAME));
                allowing(taiService).getTais(false);
            }
        });
    }

    /**
     * Creates the FormLoginAuthenticator expectations.
     * Only the FormLoginAuthenticator mock object expectations are placed here.
     */
    private void createFormLoginAuthenticatorExpectations(final WebRequest webRequest, final AuthenticationResult authenticationResult) {
        mock.checking(new Expectations() {
            {
                allowing(formLoginAuthenticator).authenticate(webRequest);
                will(returnValue(authenticationResult));
                allowing(taiService).getTais(false);
            }
        });
    }

    /**
     * Creates the BasicAuthAuthenticator expectations.
     * Only the BasicAuthAuthenticator mock object expectations are placed here.
     */
    private void createBasicAuthenticatorExpectations(final WebRequest webRequest,
                                                      final AuthenticationResult authenticationResult) {
        mock.checking(new Expectations() {
            {
                allowing(basicAuthenticator).authenticate(webRequest);
                will(returnValue(authenticationResult));
                allowing(taiService).getTais(false);
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebAppSecurityCollaboratorImpl#authorize(com.ibm.ws.webcontainer.security.internal.AuthenticationResult,
     * java.lang.String, java.lang.String, javax.security.auth.Subject, java.util.List<String>)} .
     */
    @Test
    public void testAuthorizeWithRoles() {
        AuthenticationResult authResult = new AuthenticationResult(AuthResult.UNKNOWN, (String) null);
        createTestSpecificIsAuthorizedExpectations(APP_NAME, null, null, true);

        boolean isAuthorized = secColl.authorize(authResult, APP_NAME, "uriName", null, null);
        assertTrue("authorize should return true.", isAuthorized);
    }

    private void createTestSpecificIsAuthorizedExpectations(final String resourceName,
                                                            final Collection<String> requiredRoles,
                                                            final Subject subject,
                                                            final boolean authorized) {
        mock.checking(new Expectations() {
            {
                allowing(authzService).isAuthorized(resourceName, requiredRoles, subject);
                will(returnValue(authorized));
            }
        });
    }

    private void createTestSpecificIsEveryoneGrantedExpectations(final String resourceName,
                                                                 final Collection<String> requiredRoles,
                                                                 final boolean everyoneGranted) {
        mock.checking(new Expectations() {
            {
                allowing(authzService).isEveryoneGranted(resourceName, requiredRoles);
                will(returnValue(everyoneGranted));
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebAppSecurityCollaboratorImpl#authorize(com.ibm.ws.webcontainer.security.internal.AuthenticationResult,
     * java.lang.String, java.lang.String, javax.security.auth.Subject, java.util.List<String>)} .
     */
    @Test
    public void authorize_failedKnownUserName() {
        AuthenticationResult authResult = new AuthenticationResult(AuthResult.UNKNOWN, (String) null);
        authResult.realm = "realm";
        authResult.username = "userName";
        createTestSpecificIsAuthorizedExpectations(APP_NAME, null, null, false);

        boolean isAuthorized = secColl.authorize(authResult, APP_NAME, "uriName", null, null);
        assertFalse("authorize should return true.", isAuthorized);

        assertTrue("Expected message was not logged",
                   outputMgr.checkForStandardOut("CWWKS9104A: Authorization failed for user userName:realm while invoking " + APP_NAME
                                                 + " on uriName. The user is not granted access to any of the required roles: null."));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebAppSecurityCollaboratorImpl#authorize(com.ibm.ws.webcontainer.security.internal.AuthenticationResult,
     * java.lang.String, java.lang.String, javax.security.auth.Subject, java.util.List<String>)} .
     */
    @Test
    public void authorize_failedUnknownUserName() {
        final Subject authSubject = new Subject();
        authSubject.getPrincipals().add(new WSPrincipal("subjectUserName", "accessId", "method"));
        AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, authSubject);
        authResult.realm = "realm";
        createTestSpecificIsAuthorizedExpectations(APP_NAME, null, authSubject, false);

        boolean isAuthorized = secColl.authorize(authResult, APP_NAME, "uriName", null, null);
        assertFalse("authorize should return true.", isAuthorized);

        assertTrue("Expected message was not logged",
                   outputMgr.checkForStandardOut("CWWKS9104A: Authorization failed for user subjectUserName while invoking " + APP_NAME
                                                 + " on uriName. The user is not granted access to any of the required roles: null."));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebAppSecurityCollaboratorImpl#authorize(com.ibm.ws.webcontainer.security.internal.AuthenticationResult,
     * java.lang.String, java.lang.String, javax.security.auth.Subject, java.util.List<String>)} .
     */
    @Test
    public void authorize_failedUnknownRealm() {
        final Subject authSubject = new Subject();
        authSubject.getPrincipals().add(new WSPrincipal("subjectUserName", "accessId", "method"));
        AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, authSubject);
        authResult.username = "userName";
        createTestSpecificIsAuthorizedExpectations(APP_NAME, null, authSubject, false);

        boolean isAuthorized = secColl.authorize(authResult, APP_NAME, "uriName", null, null);
        assertFalse("authorize should return true.", isAuthorized);

        assertTrue("Expected message was not logged",
                   outputMgr.checkForStandardOut("CWWKS9104A: Authorization failed for user userName:DEFAULT while invoking " + APP_NAME
                                                 + " on uriName. The user is not granted access to any of the required roles: null."));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebAppSecurityCollaboratorImpl#determineWebReply((HttpServletRequest commonReq, Subject receivedSubject,
     * String
     * uriName, WebRequest commonWebRequest)} .
     */
    @Test
    public void testDetermineWebReplyWithConstraints_successful() {
        secColl = new WebAppSecurityCollaboratorImplTestDouble();
        final AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, (String) null);
        requiredRoles.clear();
        requiredRoles.add("role1");

        createAuthenticatorProxyCommonExpectations();
        LoginConfiguration loginConfiguration = createLoginConfiguration(AUTH_METHOD_BASIC, null);
        SecurityMetadata securityMetadata = createSecurityMetadata(loginConfiguration);
        HttpServletRequest req = createHttpServletRequest();
        createTestSpecificHttpServletRequestExpectations(req, HTTP_GET, null, null, true, "defaultMethod");
        HttpServletResponse resp = createHttpServletResponse();
        final WebRequest webRequest = createWebRequest(securityMetadata, req, resp, false);
        createTestSpecificWebRequestExpectations(webRequest, requiredRoles, false);
        createBasicAuthenticatorExpectations(webRequest, authResult);
        createTestSpecificIsEveryoneGrantedExpectations(APP_NAME, requiredRoles, false);
        createTestSpecificIsAuthorizedExpectations(APP_NAME, requiredRoles, null, true);

        secColl.setAuthenticatorFactory(authenticatorFactory);
        secColl.setSecurityService(securityServiceRef);
        secColl.setTaiService(taiServiceRef);
        secColl.activate(cc, configProps);

        mock.checking(new Expectations() {
            {
                allowing(taiService).getTais(true);
                allowing(userRegistry).getRealm();
                allowing(providerAuthenticatorProxy).authenticate(webRequest);
                will(returnValue(authResult));
            }
        });

        WebReply reply = secColl.determineWebReply(null, "uriName", webRequest);
        assertEquals("Reply status code should be OK.", HttpServletResponse.SC_OK, reply.getStatusCode());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebAppSecurityCollaboratorImpl#determineWebReply((HttpServletRequest commonReq, Subject receivedSubject,
     * String
     * uriName, WebRequest commonWebRequest)} .
     */
    @Test
    public void testDetermineWebReply_authnFailed() throws Exception {
        secColl = new WebAppSecurityCollaboratorImplTestDouble();
        final AuthenticationResult authResult = new AuthenticationResult(AuthResult.FAILURE, (String) null);
        requiredRoles.clear();
        requiredRoles.add("role1");

        createAuthenticatorProxyCommonExpectations();
        LoginConfiguration loginConfiguration = createLoginConfiguration(AUTH_METHOD_BASIC, null);
        SecurityMetadata securityMetadata = createSecurityMetadata(loginConfiguration);
        HttpServletRequest req = createHttpServletRequest();
        createTestSpecificHttpServletRequestExpectations(req, HTTP_GET, null, null, true, "defaultMethod");
        HttpServletResponse resp = createHttpServletResponse();
        final WebRequest webRequest = createWebRequest(securityMetadata, req, resp, false);
        createTestSpecificWebRequestExpectations(webRequest, requiredRoles, false);
        createBasicAuthenticatorExpectations(webRequest, authResult);
        createTestSpecificIsEveryoneGrantedExpectations(APP_NAME, requiredRoles, false);
        createTestSpecificUserRegistryExpectations("someRealm");

        secColl.setAuthenticatorFactory(authenticatorFactory);
        secColl.setSecurityService(securityServiceRef);
        secColl.setTaiService(taiServiceRef);
        secColl.activate(cc, configProps);
        mock.checking(new Expectations() {
            {
                allowing(taiService).getTais(true);
                allowing(providerAuthenticatorProxy).authenticate(webRequest);
                will(returnValue(authResult));
            }
        });

        WebReply reply = secColl.determineWebReply(null, "uriName", webRequest);
        assertEquals("Reply status code should be 403.", HttpServletResponse.SC_FORBIDDEN, reply.getStatusCode());
        assertEquals("Reply status message should be AuthenticationFailed.", "AuthenticationFailed", reply.message);
    }

    private void createTestSpecificUserRegistryExpectations(final String realm) throws RegistryException {
        mock.checking(new Expectations() {
            {
                allowing(userRegistryService).getUserRegistry();
                will(returnValue(userRegistry));
                allowing(userRegistry).getRealm();
                will(returnValue(realm));
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebAppSecurityCollaboratorImpl#determineWebReply((HttpServletRequest commonReq, Subject receivedSubject,
     * String
     * uriName, WebRequest commonWebRequest)} .
     */
    @Test
    public void testDetermineWebReply_authzFailed() {
        secColl = new WebAppSecurityCollaboratorImplTestDouble();
        final Subject subject = new Subject();
        subject.getPrincipals().add(new WSPrincipal("user1", "user:BasicRealm/user1", WSPrincipal.AUTH_METHOD_PASSWORD));
        final AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, subject);
        requiredRoles.clear();
        requiredRoles.add("role1");

        createAuthenticatorProxyCommonExpectations();
        LoginConfiguration loginConfiguration = createLoginConfiguration(AUTH_METHOD_BASIC, null);
        SecurityMetadata securityMetadata = createSecurityMetadata(loginConfiguration);
        HttpServletRequest req = createHttpServletRequest();
        createTestSpecificHttpServletRequestExpectations(req, HTTP_GET, null, null, true, "defaultMethod");
        HttpServletResponse resp = createHttpServletResponse();
        final WebRequest webRequest = createWebRequest(securityMetadata, req, resp, false);
        createTestSpecificWebRequestExpectations(webRequest, requiredRoles, false);
        createBasicAuthenticatorExpectations(webRequest, authResult);
        createTestSpecificIsEveryoneGrantedExpectations(APP_NAME, requiredRoles, false);
        createTestSpecificIsAuthorizedExpectations(APP_NAME, requiredRoles, subject, false);

        mock.checking(new Expectations() {
            {
                allowing(userRegistry).getRealm();
                allowing(providerAuthenticatorProxy).authenticate(webRequest);
                will(returnValue(authResult));
            }
        });

        secColl.setAuthenticatorFactory(authenticatorFactory);
        secColl.setSecurityService(securityServiceRef);
        secColl.setTaiService(taiServiceRef);
        secColl.activate(cc, configProps);

        WebReply reply = secColl.determineWebReply(null, "uriName", webRequest);
        assertEquals("Reply status code should be 403.", HttpServletResponse.SC_FORBIDDEN, reply.getStatusCode());
        assertEquals("Reply status message should be AuthorizationFailed.", "AuthorizationFailed", reply.message);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebAppSecurityCollaboratorImpl#(AuthenticationResult authResult, String realm)} .
     */
    @Test
    public void testCreateReplyForAuthnFailure_challenge() {
        String realm = "myRealm";
        final AuthenticationResult authResult = new AuthenticationResult(AuthResult.SEND_401, (String) null);
        WebReply reply = secColl.createReplyForAuthnFailure(authResult, realm);
        assertEquals("The status code should be 401", HttpServletResponse.SC_UNAUTHORIZED, reply.getStatusCode());
        assertTrue("The message should have the realm name.", reply.message.contains(realm));
    }

    @Test
    public void testCreateReplyForAuthnFailure_taiChallenge() {
        String realm = "myRealm";
        final AuthenticationResult authResult = new AuthenticationResult(AuthResult.TAI_CHALLENGE, "TrustAssociation Interception redirecting", HttpServletResponse.SC_MOVED_TEMPORARILY);
        WebReply reply = secColl.createReplyForAuthnFailure(authResult, realm);
        assertTrue("The reply should be a TAIChallenge reply.", reply instanceof TAIChallengeReply);
        assertEquals("The status code should be the same as in the authentication result TAI challenge code.", authResult.getTAIChallengeCode(), reply.getStatusCode());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebAppSecurityCollaboratorImpl#(AuthenticationResult authResult, String realm)} .
     */
    @Test
    public void testCreateReplyForAuthnFailure_redirect() {
        final AuthenticationResult authResult = new AuthenticationResult(AuthResult.REDIRECT, (String) null);
        WebReply reply = secColl.createReplyForAuthnFailure(authResult, null);
        assertEquals("The status code should be redirect", HttpServletResponse.SC_MOVED_TEMPORARILY, reply.getStatusCode());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebAppSecurityCollaboratorImpl#(AuthenticationResult authResult, String realm)} .
     */
    @Test
    public void testCreateReplyForAuthnFailure_continue() {
        final AuthenticationResult authResult = new AuthenticationResult(AuthResult.CONTINUE, (String) null);
        WebReply reply = secColl.createReplyForAuthnFailure(authResult, null);
        assertEquals("The status code should be 403", HttpServletResponse.SC_FORBIDDEN, reply.getStatusCode());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#performInitialChecks(WebRequest commonWebRequest, String uriName)}.
     */
    @Test
    public void testPerformChecksToSkipAuthnAuthz_invalidUri() {
        mock.checking(new Expectations() {
            {
                allowing(commonWebRequest).getHttpServletRequest();
                will(returnValue(commonReq));
                allowing(commonWebRequest).getRequiredRoles();
                will(returnValue(requiredRoles));
                allowing(commonReq).getMethod();
                will(returnValue(null));
                allowing(commonWebRequest).getSecurityMetadata();
                will(returnValue(commonSecurityMetadata));
            }
        });
        WebReply reply = secColl.performInitialChecks(commonWebRequest, "");
        assertEquals("Web reply status code should be 403", HttpServletResponse.SC_FORBIDDEN, reply.getStatusCode());
        assertTrue("Web reply message should have info about invalid URI", reply.message.contains("Invalid URI"));

        reply = secColl.performInitialChecks(commonWebRequest, null);
        assertEquals("Web reply status code should be 403", HttpServletResponse.SC_FORBIDDEN, reply.getStatusCode());
        assertTrue("Web reply message should have info about invalid URI", reply.message.contains("Invalid URI"));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#performInitialChecks(WebRequest commonWebRequest, String uriName)}.
     */
    @Test
    public void testPerformChecksToSkipAuthnAuthz_unsupportedAuthMech() {
        mock.checking(new Expectations() {
            {
                allowing(commonWebRequest).getHttpServletRequest();
                will(returnValue(commonReq));
                allowing(commonWebRequest).getRequiredRoles();
                will(returnValue(requiredRoles));
                allowing(commonWebRequest).getLoginConfig();
                will(returnValue(null));
                allowing(commonReq).getMethod();
                will(returnValue(null));
                allowing(commonWebRequest).getSecurityMetadata();
                will(returnValue(commonSecurityMetadata));

            }
        });
        setUnsupportedAuthMech = true;
        secColl = new WebAppSecurityCollaboratorImplTestDouble();
        WebReply reply = secColl.performInitialChecks(commonWebRequest, "uriName");
        assertEquals("Web reply status code should be 403", HttpServletResponse.SC_FORBIDDEN, reply.getStatusCode());
        assertTrue("Web reply message should have info about unsupported auth mech", reply.message.contains("not supported"));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#performInitialChecks(WebRequest commonWebRequest, String uriName)}.
     */
    @Test
    public void testPerformChecksToSkipAuthnAuthz_sslRedirect() {
        requiredRoles.clear();
        requiredRoles.add("role1");
        mock.checking(new Expectations() {
            {
                allowing(commonWebRequest).getHttpServletRequest();
                will(returnValue(commonReq));
                allowing(commonReq).getMethod();
                will(returnValue(null));
                allowing(commonWebRequest).getRequiredRoles();
                will(returnValue(requiredRoles));
                allowing(commonWebRequest).getLoginConfig();
                will(returnValue(null));
                allowing(commonWebRequest).isAccessPrecluded();
                will(returnValue(false));
                allowing(commonWebRequest).getSecurityMetadata();
                will(returnValue(commonSecurityMetadata));

            }
        });
        setUnsupportedAuthMech = false;

        secColl = new WebAppSecurityCollaboratorImplTestDouble(null, null, new HTTPSRedirectHandlerDouble(true));
        WebReply reply = secColl.performInitialChecks(commonWebRequest, "uriName");
        assertEquals("Web reply status code should be 302", HttpServletResponse.SC_MOVED_TEMPORARILY, reply.getStatusCode());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl#performInitialChecks(WebRequest commonWebRequest, String uriName)}.
     */
    @Test
    public void testPerformChecksToSkipAuthnAuthz_everyoneAllowed() {
        final WebModuleMetaData wmmd = createWebModuleMetaDataMock();
        setTestWebModuleMetaDataOnThread(wmmd);

        mock.checking(new Expectations() {
            {
                allowing(commonWebRequest).getHttpServletRequest();
                will(returnValue(commonReq));
                allowing(commonWebRequest).getRequiredRoles();
                will(returnValue(requiredRoles));
                allowing(commonWebRequest).isSSLRequired();
                will(returnValue(false));
                allowing(commonWebRequest).isAccessPrecluded();
                will(returnValue(false));
                allowing(commonWebRequest).getLoginConfig();
                will(returnValue(null));
                allowing(commonWebRequest).getApplicationName();
                will(returnValue(APP_NAME));
                allowing(commonReq).getMethod();
                will(returnValue(null));
                allowing(commonReq).isSecure();
                will(returnValue(true));
                allowing(commonReq).getAttribute("com.ibm.ws.webcontainer.security.checkdefaultmethod");
                will(returnValue("defaultMethod"));
                one(authzService).isEveryoneGranted(APP_NAME, requiredRoles);
                will(returnValue(true));
                allowing(commonWebRequest).setUnprotectedURI(true);
                one(taiService).isInvokeForUnprotectedURI();
                will(returnValue(false));
                allowing(commonWebRequest).isAccessPrecluded();
                will(returnValue(false));
                allowing(commonWebRequest).getSecurityMetadata();
                will(returnValue(commonSecurityMetadata));

            }
        });
        requiredRoles.clear();
        requiredRoles.add("role1");

        WebReply reply = secColl.performInitialChecks(commonWebRequest, "uriName");
        assertEquals("Web reply status code should be 200", HttpServletResponse.SC_OK, reply.getStatusCode());
    }

    @Test
    public void performInitialChecks_loginPage_FORM() {
        final FormLoginConfiguration formLoginConfig = createFormLoginConfiguration();
        final LoginConfiguration loginConfig = createLoginConfiguration(LoginConfiguration.FORM, formLoginConfig);
        final WebModuleMetaData wmmd = createWebModuleMetaDataMock();
        setTestWebModuleMetaDataOnThread(wmmd);

        mock.checking(new Expectations() {
            {
                allowing(commonWebRequest).getHttpServletRequest();
                will(returnValue(commonReq));
                allowing(commonReq).getMethod();
                will(returnValue(null));
                allowing(commonWebRequest).getRequiredRoles();
                will(returnValue(requiredRoles));

                allowing(commonWebRequest).getLoginConfig();
                will(returnValue(loginConfig));
                allowing(commonWebRequest).getApplicationName();
                will(returnValue(APP_NAME));

                one(formLoginConfig).getLoginPage();
                will(returnValue("login.html"));
                one(formLoginConfig).getErrorPage();
                will(returnValue("loginError.html"));

                allowing(commonWebRequest).isAccessPrecluded();
                will(returnValue(false));
                allowing(commonReq).isSecure();
                will(returnValue(true));

                allowing(commonWebRequest).getSecurityMetadata();
                will(returnValue(commonSecurityMetadata));

            }
        });

        WebReply reply = secColl.performInitialChecks(commonWebRequest, "login.html");
        assertEquals("Web reply status code should be 200", HttpServletResponse.SC_OK, reply.getStatusCode());
    }

    @Test
    public void performInitialChecks_errorPage_CLIENT_withFormFailover() {
        final FormLoginConfiguration formLoginConfig = createFormLoginConfiguration();
        final LoginConfiguration loginConfig = createLoginConfiguration(LoginConfiguration.CLIENT_CERT, formLoginConfig);
        final WebModuleMetaData wmmd = createWebModuleMetaDataMock();
        setTestWebModuleMetaDataOnThread(wmmd);

        mock.checking(new Expectations() {
            {
                allowing(commonWebRequest).getHttpServletRequest();
                will(returnValue(commonReq));
                allowing(commonReq).getMethod();
                will(returnValue(null));
                allowing(commonWebRequest).getRequiredRoles();
                will(returnValue(requiredRoles));

                allowing(commonWebRequest).getLoginConfig();
                will(returnValue(loginConfig));
                allowing(commonWebRequest).getApplicationName();
                will(returnValue(APP_NAME));

                one(formLoginConfig).getLoginPage();
                will(returnValue("login.html"));
                one(formLoginConfig).getErrorPage();
                will(returnValue("loginError.html"));

                allowing(commonWebRequest).isAccessPrecluded();
                will(returnValue(false));
                allowing(commonReq).isSecure();
                will(returnValue(true));

                allowing(commonWebRequest).getSecurityMetadata();
                will(returnValue(commonSecurityMetadata));

            }
        });

        configProps.put(WebAppSecurityConfigImpl.CFG_KEY_ALLOW_FAIL_OVER_TO_AUTH_METHOD, "FORM");
        secColl.modified(configProps);
        WebReply reply = secColl.performInitialChecks(commonWebRequest, "loginError.html");
        assertEquals("Web reply status code should be 200", HttpServletResponse.SC_OK, reply.getStatusCode());
    }

    @Test
    public void performInitialChecks_errorPage_CLIENT_noFormFailover() {
        final FormLoginConfiguration formLoginConfig = createFormLoginConfiguration();
        final LoginConfiguration loginConfig = createLoginConfiguration(LoginConfiguration.CLIENT_CERT, formLoginConfig);
        final WebModuleMetaData wmmd = createWebModuleMetaDataMock();
        setTestWebModuleMetaDataOnThread(wmmd);

        mock.checking(new Expectations() {
            {
                allowing(commonWebRequest).getHttpServletRequest();
                will(returnValue(commonReq));
                allowing(commonReq).getMethod();
                will(returnValue(null));
                allowing(commonWebRequest).getRequiredRoles();
                will(returnValue(requiredRoles));

                allowing(commonWebRequest).getLoginConfig();
                will(returnValue(loginConfig));
                allowing(commonWebRequest).getApplicationName();
                will(returnValue(APP_NAME));

                one(formLoginConfig).getLoginPage();
                will(returnValue("login.html"));
                one(formLoginConfig).getErrorPage();
                will(returnValue("loginError.html"));

                one(commonReq).getDispatcherType();
                will(returnValue(DispatcherType.ERROR));

                allowing(commonWebRequest).isAccessPrecluded();
                will(returnValue(false));
                allowing(commonReq).isSecure();
                will(returnValue(true));

                allowing(commonWebRequest).getSecurityMetadata();
                will(returnValue(commonSecurityMetadata));

            }
        });

        WebReply reply = secColl.performInitialChecks(commonWebRequest, "loginError.html");
        assertEquals("Web reply status code should be 200", HttpServletResponse.SC_OK, reply.getStatusCode());
    }

    @Test
    public void getUserPrincipal_noSubjectOnThread() {
        assertNull("No subject on the thread means null principal",
                   secColl.getUserPrincipal());
    }

    @Test
    public void getUserPrincipal_subjectWithoutPrincipal() {
        Subject callerSubject = new Subject();
        subjectManager.setCallerSubject(callerSubject);
        assertNull("A subject without any principals has not been authenticated",
                   secColl.getUserPrincipal());
    }

    @Test
    public void getUserPrincipal_subjectWithoutWSPrincipal() {
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(new X500Principal("CN=user1"));
        Subject callerSubject = new Subject();
        callerSubject.getPrincipals().addAll(principals);
        subjectManager.setCallerSubject(callerSubject);
        assertNull("A subject without WSPrincipal has not gone through our login",
                   secColl.getUserPrincipal());
    }

    @Test
    public void getUserPrincipal_subjectWithWSPrincipal() {
        WSPrincipal principal = new WSPrincipal("user1", "user:realm/user1", WSPrincipal.AUTH_METHOD_PASSWORD);
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(principal);
        Subject callerSubject = new Subject();
        callerSubject.getPrincipals().addAll(principals);
        subjectManager.setCallerSubject(callerSubject);
        assertNull("The subject is unauthenticated ", secColl.getUserPrincipal());
    }

//    @Test(expected = IllegalStateException.class)
    public void getUserPrincipal_subjectWithMultipleWSPrincipal() {
        WSPrincipal principal1 = new WSPrincipal("user1", "user:realm/user1", WSPrincipal.AUTH_METHOD_PASSWORD);
        WSPrincipal principal2 = new WSPrincipal("user2", "user:realm/user2", WSPrincipal.AUTH_METHOD_PASSWORD);
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(principal1);
        principals.add(principal2);
        Subject callerSubject = new Subject();
        callerSubject.getPrincipals().addAll(principals);
        subjectManager.setCallerSubject(callerSubject);
        secColl.getUserPrincipal();
    }

    /**
     * isUserInRole(null) returns false.
     */
    @Test
    public void isUserInRole_null() {
        assertFalse("A null role value should return false",
                    secColl.isUserInRole(null, extendedReq));
    }

    /**
     * isUserInRole() returns false when unauthenticated.
     */
    @Test
    public void isUserInRole_unauthenticated() {
        assertFalse("Unauthenticated should return false",
                    secColl.isUserInRole("SomeRole", extendedReq));
    }

    /**
     * isUserInRole returns false when the specified role can not
     * be mapped to a real role.
     */
    @Test
    public void isUserInRole_noRealRole() {
        final Subject callerSubject = new Subject();
        subjectManager.setCallerSubject(callerSubject);

        final IWebAppDispatcherContext wadc = mock.mock(IWebAppDispatcherContext.class, "webAppDispatcher");
        final RequestProcessor reqProc = mock.mock(RequestProcessor.class, "requestProcessor");
        mock.checking(new Expectations() {
            {
                one(extendedReq).getWebAppDispatcherContext();
                will(returnValue(wadc));
                one(wadc).getCurrentServletReference();
                will(returnValue(reqProc));
                one(reqProc).getName();
                will(returnValue("ServletName"));
                one(commonSecurityMetadata).getSecurityRoleReferenced(with(any(String.class)), with(any(String.class)));
                will(returnValue(null));
            }
        });

        secColl = new WebAppSecurityCollaboratorImplTestDouble();
        assertFalse("If the role name is not defined, then should respond false",
                    secColl.isUserInRole("SomeNonExistentRole", extendedReq));
    }

    /**
     * isUserInRole returns false when the authenticated subject is not
     * in that role.
     */
    @Test
    public void isUserInRole_authenticatedNotInRole() {
        final Subject callerSubject = new Subject();
        subjectManager.setCallerSubject(callerSubject);

        final IWebAppDispatcherContext wadc = mock.mock(IWebAppDispatcherContext.class, "webAppDispatcher");
        final RequestProcessor reqProc = mock.mock(RequestProcessor.class, "requestProcessor");
        final WebModuleMetaData wmmd = createWebModuleMetaDataMock();
        setTestWebModuleMetaDataOnThread(wmmd);

        mock.checking(new Expectations() {
            {
                one(extendedReq).getWebAppDispatcherContext();
                will(returnValue(wadc));
                one(wadc).getCurrentServletReference();
                will(returnValue(reqProc));
                one(reqProc).getName();
                will(returnValue("ServletName"));
                one(commonSecurityMetadata).getSecurityRoleReferenced(with(any(String.class)), with(any(String.class)));
                will(returnValue("SomeRole"));
                one(authzService).isAuthorized(with(any(String.class)), with(any(List.class)), with(equal(callerSubject)));
                will(returnValue(false));
                allowing(commonWebRequest).getSecurityMetadata();
                will(returnValue(commonSecurityMetadata));
            }
        });

        createActivatedWebAppSecurityCollaboratorTestDouble();
        assertFalse("Authenticated and not in role should return false",
                    secColl.isUserInRole("SomeRole", extendedReq));
    }

    /**
     * isUserInRole returns true only when the authenticated subject is
     * in that role.
     */
    @Test
    public void isUserInRole_authenticatedInRole() {
        final Subject callerSubject = new Subject();
        subjectManager.setCallerSubject(callerSubject);

        final IWebAppDispatcherContext wadc = mock.mock(IWebAppDispatcherContext.class, "webAppDispatcher");
        final RequestProcessor reqProc = mock.mock(RequestProcessor.class, "requestProcessor");
        final WebModuleMetaData wmmd = createWebModuleMetaDataMock();
        setTestWebModuleMetaDataOnThread(wmmd);

        mock.checking(new Expectations() {
            {
                one(extendedReq).getWebAppDispatcherContext();
                will(returnValue(wadc));
                one(wadc).getCurrentServletReference();
                will(returnValue(reqProc));
                one(reqProc).getName();
                will(returnValue("ServletName"));
                one(commonSecurityMetadata).getSecurityRoleReferenced(with(any(String.class)), with(any(String.class)));
                will(returnValue("SomeRole"));
                one(authzService).isAuthorized(with(any(String.class)), with(any(List.class)), with(equal(callerSubject)));
                will(returnValue(true));
                allowing(commonWebRequest).getSecurityMetadata();
                will(returnValue(commonSecurityMetadata));
            }
        });

        createActivatedWebAppSecurityCollaboratorTestDouble();
        assertTrue("Authenticated and in role should return true",
                   secColl.isUserInRole("SomeRole", extendedReq));
    }

    @Test
    public void testGetSecurityMetaData() {
        final WebModuleMetaData webModuleMetaData = mock.mock(WebModuleMetaData.class);
        mock.checking(new Expectations() {
            {
                allowing(webModuleMetaData).getSecurityMetaData();
                will(returnValue(commonSecurityMetadata));
            }
        });

        setTestWebModuleMetaDataOnThread(webModuleMetaData);
        SecurityMetadata actualSecurityMetadata = secColl.getSecurityMetadata();
        assertEquals("The security meta data must be the one set on the thread.", commonSecurityMetadata, actualSecurityMetadata);
    }

    private WebModuleMetaData createWebModuleMetaDataMock() {
        final SecurityMetadata securityMetadata = createTestSecurityMetadata();
        final WebModuleMetaData webModuleMetaDataMock = mock.mock(WebModuleMetaData.class);
        mock.checking(new Expectations() {
            {
                allowing(webModuleMetaDataMock).setSecurityMetaData(with(any(SecurityMetadata.class)));
                allowing(webModuleMetaDataMock).getSecurityMetaData();
                will(returnValue(securityMetadata));
                allowing(webModuleMetaDataMock).getAnnotatedSecurityMetaData();
                will(returnValue(null));
            }
        });
        return webModuleMetaDataMock;
    }

    private SecurityMetadata createTestSecurityMetadata() {
        final SecurityConstraintCollection securityConstraintCollection = mock.mock(SecurityConstraintCollection.class);
        final MatchResponse unprotectedMatchResponse = new MatchResponse(Collections.EMPTY_LIST, false, false);
        List<String> protectedServletRoles = new ArrayList<String>();
        protectedServletRoles.add("employeeRole");
        final SecurityMetadata securityMetadataMock = mock.mock(SecurityMetadata.class, "securityMetadataMock_createTestSecurityMetadata");
        mock.checking(new Expectations() {
            {
                allowing(securityMetadataMock).getLoginConfiguration();
                will(returnValue(null));
                allowing(securityMetadataMock).getSecurityConstraintCollection();
                will(returnValue(securityConstraintCollection));
                allowing(securityConstraintCollection).getMatchResponse(with("/unprotectedPreInvokeMainTestServlet"), with(HTTP_GET));
                will(returnValue(unprotectedMatchResponse));
                allowing(securityConstraintCollection).getMatchResponse(with("/unprotectedPreInvokeMain_throwExceptionServlet"), with(HTTP_GET));
                will(returnValue(unprotectedMatchResponse));
                allowing(securityMetadataMock).getSecurityRoleReferenced(with(any(String.class)), with(any(String.class)));
                will(returnValue(SECURITY_ROLE));
                allowing(securityMetadataMock).getRunAsRoleForServlet(with(any(String.class)));
                will(returnValue(SECURITY_ROLE));
                allowing(securityMetadataMock).isSyncToOSThreadRequested();
                will(returnValue(false));
            }
        });
        return securityMetadataMock;
    }

    /**
     * Test authenticate() method with a caller subject already authenticated.
     *
     * @throws IOException
     */
    @Test
    public void testAuthenticateMethodCallerSubjectAlreadyAuthenticated() throws ServletException, IOException {
        final Subject subject = createAuthenticatedSubject();
        subjectManager.setCallerSubject(subject);
        assertTrue(secColl.authenticate(commonReq, commongResp));
    }

    @Test
    public void testAuthenticateMethodFailure() throws Exception {
        secColl = new WebAppSecurityCollaboratorImplTestDouble2();
        final WebModuleMetaData wmmd = createWebModuleMetaDataMock();
        setTestWebModuleMetaDataOnThread(wmmd);
        final AuthenticationResult authResult = new AuthenticationResult(AuthResult.FAILURE, (String) null);
        this.authResult = authResult;

        createTestSpecificUserRegistryExpectations("aRealm");

        mock.checking(new Expectations() {
            {
                allowing(commonSecurityMetadata).getLoginConfiguration();
                will(returnValue(commonloginConfiguration));
                allowing(commonloginConfiguration).getAuthenticationMethod();
                will(returnValue("BASIC"));
                allowing(commonReq).getCookies();
                will(returnValue(null));
                allowing(authenticator).authenticate(commonWebRequest);
                will(returnValue(authResult));
                one(commongResp).isCommitted();
                will(returnValue(false));
                one(commongResp).sendError(403, "AuthenticationFailed");
            }
        });
        configProps.put(WebAppSecurityConfigImpl.CFG_KEY_SINGLE_SIGN_ON_ENABLED, false);
        secColl.setAuthenticatorFactory(authenticatorFactory);
        secColl.setSecurityService(securityServiceRef);
        secColl.setTaiService(taiServiceRef);
        secColl.activate(cc, configProps);
        assertFalse(secColl.authenticate(commonReq, commongResp));
    }

    @Test
    public void testAuthenticateMethodSuccess() throws Exception {
        Subject subject = createAuthenticatedSubject();
        secColl = new WebAppSecurityCollaboratorImplTestDouble2();
        final AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, subject);
        final WebModuleMetaData wmmd = createWebModuleMetaDataMock();
        setTestWebModuleMetaDataOnThread(wmmd);
        this.authResult = authResult;

        mock.checking(new Expectations() {
            {
                allowing(userRegistry).getRealm();
                one(commongResp).isCommitted();
                will(returnValue(false));
                one(commonReq).getAttribute("com.ibm.ws.security.javaeesec.donePostLoginProcess");
                will(returnValue(null));
            }
        });

        configProps.put(WebAppSecurityConfigImpl.CFG_KEY_SINGLE_SIGN_ON_ENABLED, false);
        secColl.setAuthenticatorFactory(authenticatorFactory);
        secColl.setSecurityService(securityServiceRef);
        secColl.setTaiService(taiServiceRef);
        secColl.activate(cc, configProps);
        secColl.authenticate(commonReq, commongResp);
        assertEquals(subject, subjectManager.getCallerSubject());
        assertEquals(subject, subjectManager.getInvocationSubject());
    }

    public Subject createAuthenticatedSubject() {
        final List<String> roles = new ArrayList<String>();
        final List<String> groupIds = new ArrayList<String>();
        WSCredentialImpl credential = new WSCredentialImpl("realm", "securityName", "uniqueSecurityName", "UNAUTHENTICATED", "primaryGroupId", "accessId", roles, groupIds);
        final Subject subject = new Subject();
        subject.getPublicCredentials().add(credential);
        return subject;
    }

    class WebAppSecurityCollaboratorImplTestDouble2 extends WebAppSecurityCollaboratorImpl {
        public WebAppSecurityCollaboratorImplTestDouble2() {
            super();
        }

        public WebAppSecurityCollaboratorImplTestDouble2(WebAppSecurityConfig config) {
            super(null, null, null, config);
        }

        /** {@inheritDoc} */
        @Override
        public AuthenticationResult authenticateRequest(WebRequest webRequest) {
            return authResult;
        }
    }

    @Test
    public void getBasicAuthAuthenticator_ecounterRegistryException() throws Exception {
        final ComponentContext cc = mock.mock(ComponentContext.class, "cc-getBasicAuthAuthenticator_ecounterRegistryException");
        final SecurityService securityService = mock.mock(SecurityService.class, "securityService-getBasicAuthAuthenticator_ecounterRegistryException");
        final UserRegistryService userRegistryService = mock.mock(UserRegistryService.class, "userRegistryService-getBasicAuthAuthenticator_ecounterRegistryException");

        mock.checking(new Expectations() {
            {
                allowing(cc).getBundleContext();
                allowing(cc).locateService(WebAppSecurityCollaboratorImpl.KEY_SECURITY_SERVICE, securityServiceRef);
                will(returnValue(securityService));

                allowing(cc).locateService(WebAppSecurityCollaboratorImpl.KEY_TAI_SERVICE, taiServiceRef);
                will(returnValue(taiService));

                allowing(securityService).getUserRegistryService();
                will(returnValue(userRegistryService));
                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(true));
                one(userRegistryService).getUserRegistry();
                will(throwException(new RegistryException("Expected")));
                allowing(securityService).getAuthenticationService();
            }
        });

        secColl = new WebAppSecurityCollaboratorImpl(null, null, null);
        secColl.setAuthenticatorFactory(authenticatorFactory);
        secColl.setSecurityService(securityServiceRef);
        secColl.setTaiService(taiServiceRef);
        secColl.activate(cc, configProps);
        assertNull("If a RegistryException is encountered, no BasicAuthAuthenticator should be created",
                   secColl.getBasicAuthAuthenticator());
    }

    @Test
    public void getBasicAuthAuthenticator() throws Exception {
        secColl = new WebAppSecurityCollaboratorImpl(null, null, null);
        secColl.setAuthenticatorFactory(authenticatorFactory);
        secColl.setSecurityService(securityServiceRef);
        secColl.setTaiService(taiServiceRef);
        secColl.activate(cc, configProps);

        createTestSpecificUserRegistryExpectations(REALM_NAME);

        assertNotNull("If all of the required collaborators are available, a BasicAuthAuthenticator should be created",
                      secColl.getBasicAuthAuthenticator());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebAppSecurityCollaboratorImpl#getURIsInSecurityConstraints(String, String, String, List<String>
     * urlPatternsInAnnotation)}.
     *
     * Assert that the methods returns null when there is not exact match for a url pattern
     * between the annotation and the deployment descriptor.
     */
    @Test
    public void testGetURIsInSecurityConstraints_noConflicts() {
        secColl = new WebAppSecurityCollaboratorImplTestDouble();
        List<String> urlPatternsInAnnotation = new ArrayList<String>();
        urlPatternsInAnnotation.add("urlPatterFromAnno");
        final SecurityConstraintCollection secConstrCollection = mock.mock(SecurityConstraintCollection.class);
        final List<SecurityConstraint> secConstraints = createSecurityConstraints();

        mock.checking(new Expectations() {
            {
                allowing(commonSecurityMetadata).getSecurityConstraintCollection();
                will(returnValue(secConstrCollection));
                allowing(secConstrCollection).getSecurityConstraints();
                will(returnValue(secConstraints));

            }
        });
        List<String> urlPatternConflicts = secColl.getURIsInSecurityConstraints(null, null, null, urlPatternsInAnnotation);
        assertNull("The method getURIsInSecurityConstraints should return null because there are no conflicts.", urlPatternConflicts);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebAppSecurityCollaboratorImpl#getURIsInSecurityConstraints(String, String, String, List<String>
     * urlPatternsInAnnotation)}.
     *
     * Assert that the methods returns the list conflicting url pattern when there is an exact match for a url pattern
     * between the annotation and the deployment descriptor.
     */
    @Test
    public void testGetURIsInSecurityConstraints_withConflicts() {
        secColl = new WebAppSecurityCollaboratorImplTestDouble();
        List<String> urlPatternsInAnnotation = new ArrayList<String>();
        urlPatternsInAnnotation.add("urlPatterFromAnno");
        urlPatternsInAnnotation.add(URL_PATTERN_DD);
        final SecurityConstraintCollection secConstrCollection = mock.mock(SecurityConstraintCollection.class);
        final List<SecurityConstraint> secConstraints = createSecurityConstraints();

        mock.checking(new Expectations() {
            {
                allowing(commonSecurityMetadata).getSecurityConstraintCollection();
                will(returnValue(secConstrCollection));
                allowing(secConstrCollection).getSecurityConstraints();
                will(returnValue(secConstraints));

            }
        });
        List<String> urlPatternConflicts = secColl.getURIsInSecurityConstraints(null, null, null, urlPatternsInAnnotation);
        assertEquals(
                     "The method getURIsInSecurityConstraints should return a list containing the url pattern in the dd because it exists in the annotation and causes a conflict.",
                     URL_PATTERN_DD_LIST, urlPatternConflicts);
    }

    /**
     * Tests that no subject is set if persist cred is disabled.
     */
    @Test
    public void optionallyAuthenticateUnprotectedResource_PesistCred_Disabled() {
        SubjectManager subjectManager = new SubjectManager();
        subjectManager.clearSubjects();

        mock.checking(new Expectations() {
            {
                one(webAppSecurityConfig).isUseAuthenticationDataForUnprotectedResourceEnabled();
                will(returnValue(false));
            }
        });
        secColl = new WebAppSecurityCollaboratorImpl(null, null, null, webAppSecurityConfig);
        secColl.optionallyAuthenticateUnprotectedResource(commonWebRequest);

        assertNull("Nothing to persist, no subject should be set.", subjectManager.getCallerSubject());
    }

    /**
     * Tests that no subject is set if no authentication data is available.
     */
    @Test
    public void optionallyAuthenticateUnprotectedResource_NoAuthenticationData() {
        SubjectManager subjectManager = new SubjectManager();
        subjectManager.clearSubjects();

        mock.checking(new Expectations() {
            {
                one(webAppSecurityConfig).isUseAuthenticationDataForUnprotectedResourceEnabled();
                will(returnValue(true));
                one(commonWebRequest).getRequiredRoles();
                will(returnValue(new ArrayList<String>()));
                one(commonWebRequest).setUnprotectedURI(true); // alow it
                one(commonWebRequest).getHttpServletRequest(); //
                will(returnValue(commonReq)); //
                one(commonWebRequest).hasAuthenticationData();
                will(returnValue(false));
            }
        });
        secColl = new WebAppSecurityCollaboratorImpl(null, null, null, webAppSecurityConfig);
        secColl.setAuthenticatorFactory(authenticatorFactory);
        secColl.optionallyAuthenticateUnprotectedResource(commonWebRequest);

        assertNull("Nothing to persist, no subject should be set.",
                   subjectManager.getCallerSubject());
    }

    /**
     * Tests that no subject is set if the resource is protected.
     */
    @Test
    public void optionallyAuthenticateUnprotectedResource_ProtectedResource() {
        SubjectManager subjectManager = new SubjectManager();
        subjectManager.clearSubjects();

        final List<String> roles = new ArrayList<String>();
        roles.add("Role1");
        mock.checking(new Expectations() {
            {
                //one(commonWebRequest).getHttpServletRequest(); //
                //will(returnValue(commonReq)); //
                //one(commonWebRequest).hasAuthenticationData();
                //will(returnValue(true));
                one(commonWebRequest).getRequiredRoles();
                will(returnValue(roles));
                one(commonWebRequest).getApplicationName();
                will(returnValue("myApp"));
                one(authzService).isEveryoneGranted("myApp", roles);
                will(returnValue(false));
            }
        });
        secColl = new WebAppSecurityCollaboratorImpl(null, null, null, webAppSecurityConfig);
        secColl.setAuthenticatorFactory(authenticatorFactory);
        secColl.setSecurityService(securityServiceRef);
        secColl.activate(cc, configProps);
        secColl.optionallyAuthenticateUnprotectedResource(commonWebRequest);

        assertNull("Nothing to persist, no subject should be set.",
                   subjectManager.getCallerSubject());
    }

    /**
     * Tests that the appropriate subject is set if the resource is unprotected (no required roles).
     */
    @Test
    public void optionallyAuthenticateUnprotectedResource_NoRequiredRoles() {
        SubjectManager subjectManager = new SubjectManager();
        subjectManager.clearSubjects();

        final Subject authenticatedSubject = createAuthenticatedSubject();
        authResult = new AuthenticationResult(AuthResult.SUCCESS, authenticatedSubject);

        final List<String> roles = new ArrayList<String>();
        mock.checking(new Expectations() {
            {
                one(webAppSecurityConfig).isUseAuthenticationDataForUnprotectedResourceEnabled();
                will(returnValue(true));
                one(commonWebRequest).hasAuthenticationData();
                will(returnValue(true));
                one(commonWebRequest).getRequiredRoles();
                will(returnValue(roles));

                one(commonWebRequest).disableFormLoginRedirect();
                one(commonWebRequest).setUnprotectedURI(true);
            }
        });
        secColl = new WebAppSecurityCollaboratorImplTestDouble2(webAppSecurityConfig);
        secColl.setAuthenticatorFactory(authenticatorFactory);
        secColl.optionallyAuthenticateUnprotectedResource(commonWebRequest);

        assertSame("The request for the unprotected resource must use the authenticated subject.",
                   authenticatedSubject, subjectManager.getCallerSubject());
    }

    /**
     * Tests that the appropriate subject is set if the resource is unprotected (everyone is granted).
     */
    @Test
    public void optionallyAuthenticateUnprotectedResource_EveryoneGranted() {
        SubjectManager subjectManager = new SubjectManager();
        subjectManager.clearSubjects();

        final Subject authenticatedSubject = createAuthenticatedSubject();
        authResult = new AuthenticationResult(AuthResult.SUCCESS, authenticatedSubject);

        final List<String> roles = new ArrayList<String>();
        roles.add("Role1");
        mock.checking(new Expectations() {
            {
                one(commonWebRequest).hasAuthenticationData();
                will(returnValue(true));
                one(commonWebRequest).getRequiredRoles();
                will(returnValue(roles));
                one(commonWebRequest).getApplicationName();
                will(returnValue("myApp"));
                one(authzService).isEveryoneGranted("myApp", roles);
                will(returnValue(true));

                one(commonWebRequest).disableFormLoginRedirect();
                one(commonWebRequest).setUnprotectedURI(true);
            }
        });
        secColl = new WebAppSecurityCollaboratorImplTestDouble2(webAppSecurityConfig);
        secColl.setAuthenticatorFactory(authenticatorFactory);
        secColl.setSecurityService(securityServiceRef);
        secColl.activate(cc, configProps);
        secColl.optionallyAuthenticateUnprotectedResource(commonWebRequest);

        assertSame("The request for the unprotected resource must use the authenticated subject.",
                   authenticatedSubject, subjectManager.getCallerSubject());
    }

    /**
     * Tests that authentication data is used for an unprotected resource access.
     */
    @Test
    public void setAuthenticatedSubjectIfNeeded_PesistCred_Unprotected_Basic() {
        secColl = new WebAppSecurityCollaboratorImplTestDouble2();
        final Subject authenticatedSubject = createAuthenticatedSubject();
        authResult = new AuthenticationResult(AuthResult.SUCCESS, authenticatedSubject);
        SubjectManager subjectManager = new SubjectManager();
        subjectManager.clearSubjects();

        secColl.setAuthenticatedSubjectIfNeeded(commonWebRequest);

        assertSame("The request for the unprotected resource must use the authenticated subject.",
                   authenticatedSubject, subjectManager.getCallerSubject());
    }

    @Test
    public void setAuthenticatedSubjectIfNeeded_PesistCred_Unprotected_Basic_Unsuccessful_SubjectNotSet() {
        secColl = new WebAppSecurityCollaboratorImplTestDouble2();
        authResult = new AuthenticationResult(AuthResult.SEND_401, "Redirect message.");
        Subject previousSubject = new Subject();
        SubjectManager subjectManager = new SubjectManager();
        subjectManager.clearSubjects();
        subjectManager.setCallerSubject(previousSubject);

        secColl.setAuthenticatedSubjectIfNeeded(commonWebRequest);

        assertSame("The authenticated subject must not be set if authentication is not possible for the unprotected resource.",
                   previousSubject, subjectManager.getCallerSubject());
    }

    @Test
    public void setAuthenticatedSubjectIfNeeded_PesistCred_Unprotected_Form_SubjectNotSet() {
        secColl = new WebAppSecurityCollaboratorImplTestDouble2();
        authResult = null;
        SubjectManager subjectManager = new SubjectManager();
        subjectManager.clearSubjects();

        secColl.setAuthenticatedSubjectIfNeeded(commonWebRequest);

        assertNull("The authenticated subject must not be set if authentication is not possible for the unprotected resource.",
                   subjectManager.getCallerSubject());
    }

    class WebAppSecurityCollaboratorImplTestDouble3 extends WebAppSecurityCollaboratorImpl {
        private SecurityMetadata sm = null;

        public WebAppSecurityCollaboratorImplTestDouble3(SecurityMetadata sm) {
            super();
            this.sm = sm;
        }

        /**
         * {@inheritDoc} Override the normal flow which looks on the thread.
         * We want to return our mock in this case.
         */
        @Override
        public SecurityMetadata getSecurityMetadata() {
            return this.sm;
        }
    }

    @Test
    public void testUnsupportedAuthMechNullSM() throws Exception {
        secColl = new WebAppSecurityCollaboratorImplTestDouble3(null);
        assertFalse("When SecurityMetadata is null, unsupportedAuthMech should return false",
                    secColl.unsupportedAuthMech());
    }

    @Test
    public void testUnsupportedAuthMechNullLC() throws Exception {
        secColl = new WebAppSecurityCollaboratorImplTestDouble3(commonSecurityMetadata);
        mock.checking(new Expectations() {
            {
                allowing(commonSecurityMetadata).getLoginConfiguration();
                will(returnValue((LoginConfiguration) null));
            }
        });
        assertFalse("When LoginConfig is null, unsupportedAuthMech should return false",
                    secColl.unsupportedAuthMech());
    }

    @Test
    public void testUnsupportedAuthMechNullAM() throws Exception {
        secColl = new WebAppSecurityCollaboratorImplTestDouble3(commonSecurityMetadata);
        mock.checking(new Expectations() {
            {
                allowing(commonSecurityMetadata).getLoginConfiguration();
                will(returnValue(commonloginConfiguration));
                allowing(commonloginConfiguration).getAuthenticationMethod();
                will(returnValue((String) null));
            }
        });
        assertFalse("When AuthenticationMethod is null, unsupportedAuthMech should return false",
                    secColl.unsupportedAuthMech());
    }

    @Test
    public void testUnsupportedAuthMechDigest() throws Exception {
        secColl = new WebAppSecurityCollaboratorImplTestDouble3(commonSecurityMetadata);
        mock.checking(new Expectations() {
            {
                allowing(commonSecurityMetadata).getLoginConfiguration();
                will(returnValue(commonloginConfiguration));
                allowing(commonloginConfiguration).getAuthenticationMethod();
                will(returnValue("DIGEST"));
            }
        });
        assertTrue("When AuthenticationMethod is DIGEST, unsupportedAuthMech should return true",
                   secColl.unsupportedAuthMech());
    }

    @Test
    public void testUnsupportedAuthMechBasic() throws Exception {
        secColl = new WebAppSecurityCollaboratorImplTestDouble3(commonSecurityMetadata);
        mock.checking(new Expectations() {
            {
                allowing(commonSecurityMetadata).getLoginConfiguration();
                will(returnValue(commonloginConfiguration));
                allowing(commonloginConfiguration).getAuthenticationMethod();
                will(returnValue("BASIC"));
            }
        });
        assertFalse("When AuthenticationMethod is BASIC, unsupportedAuthMech should return false",
                    secColl.unsupportedAuthMech());
    }

    private void createActivatedWebAppSecurityCollaboratorTestDouble() {
        secColl = new WebAppSecurityCollaboratorImplTestDouble();
        secColl.setAuthenticatorFactory(authenticatorFactory);
        secColl.setSecurityService(securityServiceRef);
        secColl.setTaiService(taiServiceRef);
        secColl.activate(cc, configProps);
    }

    /**
     * @return
     */
    private List<SecurityConstraint> createSecurityConstraints() {
        final List<SecurityConstraint> secConstraints = new ArrayList<SecurityConstraint>();

        List<WebResourceCollection> webResourceCollections = new ArrayList<WebResourceCollection>();
        WebResourceCollection webResCollection = new WebResourceCollection(URL_PATTERN_DD_LIST, new ArrayList<String>());
        webResourceCollections.add(webResCollection);
        SecurityConstraint secConstraint = new SecurityConstraint(webResourceCollections, new ArrayList<String>(), false, false, false, false);
        secConstraints.add(secConstraint);
        return secConstraints;
    }

    class WebAuthenticatorProxyTestDouble extends WebAuthenticatorProxy {

        public WebAuthenticatorProxyTestDouble(WebAppSecurityConfig webAppSecurityConfig, PostParameterHelper postParameterHelper,
                                               AtomicServiceReference<SecurityService> securityServiceRef, AtomicServiceReference<TAIService> taiServiceRef) {
            super(webAppSecurityConfig, postParameterHelper, securityServiceRef, providerAuthenticatorProxy);
        }

        @Override
        protected BasicAuthAuthenticator createBasicAuthenticator() throws RegistryException {
            return basicAuthenticator;
        }

        @Override
        protected FormLoginAuthenticator createFormLoginAuthenticator(WebRequest webRequest) {
            return formLoginAuthenticator;
        }

    }

}