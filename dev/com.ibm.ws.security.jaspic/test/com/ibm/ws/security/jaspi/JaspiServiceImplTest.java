/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaspi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import com.ibm.websphere.security.auth.WSLoginFailedException;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.JaspiService;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.WebRequestImpl;
import com.ibm.ws.webcontainer.security.WebSecurityContext;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.MatchResponse;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public class JaspiServiceImplTest {

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private JaspiService jaspiService = null;
    private WebRequest mockWebRequest;

    private Subject authenticatedSubject;
    private Map<String, String> msgMap;
    private final AuthException bob = new AuthException("bob");

    private final ServerAuthContext mockServerAuthContext = mock.mock(ServerAuthContext.class, "mockServerAuthContext");
    private final AuthConfigProvider mockAuthConfigProvider = mock.mock(AuthConfigProvider.class); //, "mockAuthConfigProvider");
    private final ServerAuthConfig mockServerAuthConfig = mock.mock(ServerAuthConfig.class, "mockServerAuthConfig");
    private final MessageInfo mockMessageInfo = mock.mock(MessageInfo.class, "mockMessageInfo");
    private final WebSecurityContext mockWebSecurityContext = mock.mock(WebSecurityContext.class, "mockWebSecurityContext");
    private final HttpServletRequest mockReq = mock.mock(HttpServletRequest.class, "mockReq");
    private final HttpServletResponse mockRsp = mock.mock(HttpServletResponse.class, "mockRsp");
    private final MatchResponse mockMatchResponse = mock.mock(MatchResponse.class, "mockMatchResponse");
    private final JaspiService.JaspiAuthContext mockJaspiAuthContext = mock.mock(JaspiServiceImpl.PostInvokeJaspiContext.class);
    private final WebAppConfig mockWebAppConfig = mock.mock(WebAppConfig.class);
    private final SecurityMetadata mockSecurityMetadata = mock.mock(SecurityMetadata.class);
    private final WebAppSecurityConfig mockWebAppSecurityConfig = mock.mock(WebAppSecurityConfig.class);
    private final LoginConfiguration mockLoginConfiguration = mock.mock(LoginConfiguration.class);
    private final WebProviderAuthenticatorHelper mockWebProviderAuthenticatorHelper = mock.mock(WebProviderAuthenticatorHelper.class);
    private final ServletContext mockServletContext = mock.mock(ServletContext.class);

    public Hashtable<String, Object> loginHashtable;

    WebRequest newWebRequest() {
        WebRequest webReq = new WebRequestImpl(mockReq, mockRsp, "appName", mockWebSecurityContext, mockSecurityMetadata, mockMatchResponse, mockWebAppSecurityConfig);
        return webReq;
    }

    void setupFactory() {
        AuthConfigFactory factory = new ProviderRegistry();
        AuthConfigFactory.setFactory(factory);
        factory.registerConfigProvider(mockAuthConfigProvider, "HttpServlet", "default_host /context-root", "test provider");
    }

    Subject setupSubject(Hashtable<?, ?> ht) throws Exception {
        Subject subj = new Subject();
        subj.getPrivateCredentials().add(ht);
        return subj;
    }

    Hashtable<?, ?> setupHashtable(String cacheKey) {
        Hashtable ht = new Hashtable();
        ht.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, cacheKey);
        return ht;
    }

    @Test
    public void testPostInvoke() throws Exception {
        jaspiService = new JaspiServiceImpl();
        mock.checking(new Expectations() {
            {
                allowing(mockWebSecurityContext).getReceivedSubject();
                will(returnValue(authenticatedSubject));
                allowing(mockWebSecurityContext).getJaspiAuthContext();
                will(returnValue(mockJaspiAuthContext));
                allowing(mockJaspiAuthContext).runSecureResponse();
                will(returnValue(true));
                allowing(mockJaspiAuthContext).getServerAuthContext();
                will(returnValue(mockServerAuthContext));
                allowing(mockJaspiAuthContext).getMessageInfo();
                will(returnValue(mockMessageInfo));
                allowing(mockServerAuthConfig).getAuthContext(with(any(String.class)), with(any(Subject.class)), with(any(Map.class)));
                will(returnValue(mockServerAuthContext));
                allowing(mockServerAuthContext).secureResponse((with(equal(mockMessageInfo))), with(any(Subject.class)));
                will(returnValue(AuthStatus.SEND_SUCCESS));
            }
        });
        jaspiService.postInvoke(mockWebSecurityContext);
    }

    @Test(expected = AuthenticationException.class)
    public void testSecureResponseInvalidStatus() throws Exception {
        jaspiService = new JaspiServiceImpl();
        mock.checking(new Expectations() {
            {
                allowing(mockWebSecurityContext).getReceivedSubject();
                will(returnValue(authenticatedSubject));
                allowing(mockWebSecurityContext).getJaspiAuthContext();
                will(returnValue(mockJaspiAuthContext));
                allowing(mockJaspiAuthContext).runSecureResponse();
                will(returnValue(true));
                allowing(mockJaspiAuthContext).getServerAuthContext();
                will(returnValue(mockServerAuthContext));
                allowing(mockJaspiAuthContext).getMessageInfo();
                will(returnValue(mockMessageInfo));
                allowing(mockServerAuthContext).secureResponse((with(equal(mockMessageInfo))), with(any(Subject.class)));
                will(returnValue(AuthStatus.FAILURE));
            }
        });
        jaspiService.postInvoke(mockWebSecurityContext);
    }

    @Test(expected = AuthenticationException.class)
    public void testSecureResponseThrowsAuthException() throws Exception {
        jaspiService = new JaspiServiceImpl();
        mock.checking(new Expectations() {
            {
                allowing(mockWebSecurityContext).getReceivedSubject();
                will(returnValue(authenticatedSubject));
                allowing(mockWebSecurityContext).getJaspiAuthContext();
                will(returnValue(mockJaspiAuthContext));
                allowing(mockJaspiAuthContext).runSecureResponse();
                will(returnValue(true));
                allowing(mockJaspiAuthContext).getServerAuthContext();
                will(returnValue(mockServerAuthContext));
                allowing(mockJaspiAuthContext).getMessageInfo();
                will(returnValue(mockMessageInfo));
                allowing(mockServerAuthContext).secureResponse((with(equal(mockMessageInfo))), with(any(Subject.class)));
                will(throwException(bob));
            }
        });
        jaspiService.postInvoke(mockWebSecurityContext);
    }

    @Test
    public void testAuthhenticateSuccess() throws Exception {
        setupFactory();
        WebAuthenticator jaspiWebAuthenticator = new TestDoubleJaspiServiceImpl();
        HashMap<String, Object> props = new HashMap<String, Object>() {};
        props.put("webAppConfig", mockWebAppConfig);
        props.put("securityMetadata", mockSecurityMetadata);
        props.put("webAppSecurityConfig", mockWebAppSecurityConfig);
        final AuthenticationResult authenticationResult = new AuthenticationResult(AuthResult.SUCCESS, new Subject());
        mock.checking(new Expectations() {
            {
                allowing(mockAuthConfigProvider).getServerAuthConfig(with(equal("HttpServlet")), with(equal("default_host /context-root")), with(any(CallbackHandler.class)));
                will(returnValue(mockServerAuthConfig));
                allowing(mockServerAuthConfig).getAuthContext(with(any(String.class)), with(any(Subject.class)), with(any(Map.class)));
                will(returnValue(mockServerAuthContext));
                allowing(mockReq).getMethod();
                allowing(mockReq).getServletContext();
                will(returnValue(mockServletContext));
                allowing(mockServletContext).setAttribute(with(any(String.class)), with(any(Object.class)));
                allowing(mockServletContext).getAttribute(with(any(String.class)));
                will(returnValue(null));
                allowing(mockServerAuthContext).validateRequest(with(any(MessageInfo.class)), with(any(Subject.class)), with(any(Subject.class)));
                will(returnValue(AuthStatus.SUCCESS));
                allowing(mockWebAppConfig).getApplicationName();
                will(returnValue("bob"));
                allowing(mockWebAppConfig).getVirtualHostName();
                will(returnValue("default_host"));
                allowing(mockWebAppConfig).getContextRoot();
                will(returnValue("/context-root"));
                allowing(mockWebAppConfig).getModuleName();
                will(returnValue("bobModuleName"));
                allowing(mockSecurityMetadata).getLoginConfiguration();
                will(returnValue(mockLoginConfiguration));
                allowing(mockLoginConfiguration).getAuthenticationMethod();
                will(returnValue(LoginConfiguration.BASIC));
                allowing(mockServerAuthConfig).getAuthContextID(with(any(MessageInfo.class)));
                will(returnValue("bobContextID"));
                allowing(mockWebProviderAuthenticatorHelper).loginWithHashtable(with(any(HttpServletRequest.class)),
                                                                                with(any(HttpServletResponse.class)),
                                                                                with(any(Subject.class)));
                will(returnValue(authenticationResult));
            }
        });
        loginHashtable = new Hashtable<String, Object>();
        loginHashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, "bob");
        AuthenticationResult result = jaspiWebAuthenticator.authenticate(mockReq, mockRsp, props);
        assertNotNull(result);
        assertEquals(result.getStatus(), AuthResult.SUCCESS);
    }

    @Test
    public void testLoginNotNeededForUnauthenticatedUserName() throws Exception {
        setupFactory();
        WebAuthenticator jaspiWebAuthenticator = new TestDoubleJaspiServiceImpl();
        HashMap<String, Object> props = new HashMap<String, Object>() {};
        props.put("webAppConfig", mockWebAppConfig);
        props.put("securityMetadata", mockSecurityMetadata);
        props.put("webAppSecurityConfig", mockWebAppSecurityConfig);
        final AuthenticationResult authenticationResult = new AuthenticationResult(AuthResult.SUCCESS, new Subject());
        mock.checking(new Expectations() {
            {
                allowing(mockAuthConfigProvider).getServerAuthConfig(with(equal("HttpServlet")), with(equal("default_host /context-root")), with(any(CallbackHandler.class)));
                will(returnValue(mockServerAuthConfig));
                allowing(mockServerAuthConfig).getAuthContext(with(any(String.class)), with(any(Subject.class)), with(any(Map.class)));
                will(returnValue(mockServerAuthContext));
                allowing(mockReq).getMethod();
                allowing(mockServerAuthContext).validateRequest(with(any(MessageInfo.class)), with(any(Subject.class)), with(any(Subject.class)));
                will(returnValue(AuthStatus.SUCCESS));
                allowing(mockWebAppConfig).getApplicationName();
                will(returnValue("bob"));
                allowing(mockWebAppConfig).getVirtualHostName();
                will(returnValue("default_host"));
                allowing(mockWebAppConfig).getContextRoot();
                will(returnValue("/context-root"));
                allowing(mockWebAppConfig).getModuleName();
                will(returnValue("bobModuleName"));
                allowing(mockSecurityMetadata).getLoginConfiguration();
                will(returnValue(mockLoginConfiguration));
                allowing(mockLoginConfiguration).getAuthenticationMethod();
                will(returnValue(LoginConfiguration.BASIC));
                allowing(mockServerAuthConfig).getAuthContextID(with(any(MessageInfo.class)));
                will(returnValue("bobContextID"));
                allowing(mockWebProviderAuthenticatorHelper).loginWithHashtable(with(any(HttpServletRequest.class)),
                                                                                with(any(HttpServletResponse.class)),
                                                                                with(any(Subject.class)));
                will(returnValue(authenticationResult));
                allowing(mockReq).getServletContext();
                will(returnValue(mockServletContext));
                allowing(mockServletContext).setAttribute(with(any(String.class)), with(any(Object.class)));
                allowing(mockServletContext).getAttribute(with(any(String.class)));
                will(returnValue(null));
            }
        });
        loginHashtable = new Hashtable<String, Object>();
        loginHashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, "UNAUTHENTICATED");
        AuthenticationResult result = jaspiWebAuthenticator.authenticate(mockReq, mockRsp, props);
        assertNotNull(result);
        assertEquals(result.getStatus(), AuthResult.SUCCESS);
    }

    @Test
    public void testAuthenticateFailure() throws Exception {
        setupFactory();
        WebAuthenticator jaspiWebAuthenticator = new TestDoubleJaspiServiceImpl();
        HashMap<String, Object> props = new HashMap<String, Object>() {};
        props.put("webAppConfig", mockWebAppConfig);
        props.put("securityMetadata", mockSecurityMetadata);
        props.put("webAppSecurityConfig", mockWebAppSecurityConfig);
        final AuthenticationResult authenticationResult = new AuthenticationResult(AuthResult.SUCCESS, new Subject());
        mock.checking(new Expectations() {
            {
                allowing(mockReq).getRequestURI();
                will(returnValue("/bob"));
                allowing(mockAuthConfigProvider).getServerAuthConfig(with(equal("HttpServlet")), with(equal("default_host /context-root")), with(any(CallbackHandler.class)));
                will(returnValue(mockServerAuthConfig));
                allowing(mockServerAuthConfig).getAuthContext(with(any(String.class)), with(any(Subject.class)), with(any(Map.class)));
                will(returnValue(mockServerAuthContext));
                allowing(mockReq).getMethod();
                allowing(mockServerAuthContext).validateRequest(with(any(MessageInfo.class)), with(any(Subject.class)), with(any(Subject.class)));
                will(returnValue(AuthStatus.FAILURE));
                allowing(mockWebAppConfig).getApplicationName();
                will(returnValue("bob"));
                allowing(mockWebAppConfig).getVirtualHostName();
                will(returnValue("default_host"));
                allowing(mockWebAppConfig).getContextRoot();
                will(returnValue("/context-root"));
                allowing(mockWebAppConfig).getModuleName();
                will(returnValue("bobModuleName"));
                allowing(mockSecurityMetadata).getLoginConfiguration();
                will(returnValue(mockLoginConfiguration));
                allowing(mockLoginConfiguration).getAuthenticationMethod();
                will(returnValue(LoginConfiguration.BASIC));
                allowing(mockServerAuthConfig).getAuthContextID(with(any(MessageInfo.class)));
                will(returnValue("bobContextID"));
                allowing(mockWebProviderAuthenticatorHelper).loginWithHashtable(with(any(HttpServletRequest.class)),
                                                                                with(any(HttpServletResponse.class)),
                                                                                with(any(Subject.class)));
                will(returnValue(authenticationResult));
                allowing(mockReq).getServletContext();
                will(returnValue(mockServletContext));
                allowing(mockServletContext).setAttribute(with(any(String.class)), with(any(Object.class)));
                allowing(mockServletContext).getAttribute(with(any(String.class)));
                will(returnValue(null));
            }
        });
        loginHashtable = new Hashtable<String, Object>();
        loginHashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, "bob");
        AuthenticationResult result = jaspiWebAuthenticator.authenticate(mockReq, mockRsp, props);
        assertNotNull(result);
        assertEquals(result.getStatus(), AuthResult.FAILURE);
    }

    @Test
    public void testValidateRequestThrowsAuthException() throws Exception {
        setupFactory();
        WebAuthenticator jaspiWebAuthenticator = new TestDoubleJaspiServiceImpl();
        HashMap<String, Object> props = new HashMap<String, Object>() {};
        props.put("webAppConfig", mockWebAppConfig);
        props.put("securityMetadata", mockSecurityMetadata);
        props.put("webAppSecurityConfig", mockWebAppSecurityConfig);
        final AuthenticationResult authenticationResult = new AuthenticationResult(AuthResult.SUCCESS, new Subject());
        mock.checking(new Expectations() {
            {
                allowing(mockAuthConfigProvider).getServerAuthConfig(with(equal("HttpServlet")), with(equal("default_host /context-root")), with(any(CallbackHandler.class)));
                will(returnValue(mockServerAuthConfig));
                allowing(mockServerAuthConfig).getAuthContext(with(any(String.class)), with(any(Subject.class)), with(any(Map.class)));
                will(returnValue(mockServerAuthContext));
                allowing(mockReq).getMethod();
                allowing(mockServerAuthContext).validateRequest(with(any(MessageInfo.class)), with(any(Subject.class)), with(any(Subject.class)));
                will(throwException(new AuthException()));
                allowing(mockWebAppConfig).getApplicationName();
                will(returnValue("bob"));
                allowing(mockWebAppConfig).getVirtualHostName();
                will(returnValue("default_host"));
                allowing(mockWebAppConfig).getContextRoot();
                will(returnValue("/context-root"));
                allowing(mockWebAppConfig).getModuleName();
                will(returnValue("bobModuleName"));
                allowing(mockSecurityMetadata).getLoginConfiguration();
                will(returnValue(mockLoginConfiguration));
                allowing(mockLoginConfiguration).getAuthenticationMethod();
                will(returnValue(LoginConfiguration.BASIC));
                allowing(mockServerAuthConfig).getAuthContextID(with(any(MessageInfo.class)));
                will(returnValue("bobContextID"));
                allowing(mockWebProviderAuthenticatorHelper).loginWithHashtable(with(any(HttpServletRequest.class)),
                                                                                with(any(HttpServletResponse.class)),
                                                                                with(any(Subject.class)));
                will(returnValue(authenticationResult));
                allowing(mockReq).getServletContext();
                will(returnValue(mockServletContext));
                allowing(mockServletContext).setAttribute(with(any(String.class)), with(any(Object.class)));
                allowing(mockServletContext).getAttribute(with(any(String.class)));
                will(returnValue(null));
            }
        });
        loginHashtable = new Hashtable<String, Object>();
        loginHashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, "bob");
        AuthenticationResult result = jaspiWebAuthenticator.authenticate(mockReq, mockRsp, props);
        assertNotNull(result);
        assertEquals(result.getStatus(), AuthResult.FAILURE);
    }

    //@Test
    public void testValidateRequestThrowsWSLoginFailedException() throws Exception {
        WebAuthenticator jaspiWebAuthenticator = new TestDoubleJaspiServiceImpl();
        HashMap<String, Object> props = new HashMap<String, Object>() {};
        props.put("webAppConfig", mockWebAppConfig);
        props.put("securityMetadata", mockSecurityMetadata);
        props.put("webAppSecurityConfig", mockWebAppSecurityConfig);
        final AuthenticationResult authenticationResult = new AuthenticationResult(AuthResult.SUCCESS, new Subject());
        mock.checking(new Expectations() {
            {
                allowing(mockReq).getRequestURI();
                will(returnValue("/bob"));
                allowing(mockReq).getMethod();
                allowing(mockWebAppConfig).getApplicationName();
                will(returnValue("bob"));
                allowing(mockWebAppConfig).getVirtualHostName();
                will(returnValue("default_host"));
                allowing(mockWebAppConfig).getContextRoot();
                will(returnValue("/context-root"));
                allowing(mockWebAppConfig).getModuleName();
                will(returnValue("bobModuleName"));
                allowing(mockSecurityMetadata).getLoginConfiguration();
                will(returnValue(mockLoginConfiguration));
                allowing(mockLoginConfiguration).getAuthenticationMethod();
                will(returnValue(LoginConfiguration.BASIC));
                allowing(mockWebProviderAuthenticatorHelper).loginWithHashtable(with(any(HttpServletRequest.class)),
                                                                                with(any(HttpServletResponse.class)),
                                                                                with(any(Subject.class)));
                will(throwException(new WSLoginFailedException("bob")));
                allowing(mockReq).getServletContext();
                will(returnValue(mockServletContext));
                allowing(mockServletContext).setAttribute(with(any(String.class)), with(any(Object.class)));
                allowing(mockServletContext).getAttribute(with(any(String.class)));
                will(returnValue(null));
            }
        });
        loginHashtable = new Hashtable<String, Object>();
        loginHashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, "bob");
        AuthenticationResult result = jaspiWebAuthenticator.authenticate(mockReq, mockRsp, props);
        assertNotNull(result);
        assertEquals(result.getStatus(), AuthResult.FAILURE);
    }

    @Test
    public void testSubjectHasNoCustomHashtableKey() throws Exception {
        setupFactory();
        WebAuthenticator jaspiWebAuthenticator = new TestDoubleJaspiServiceImpl();
        HashMap<String, Object> props = new HashMap<String, Object>() {};
        props.put("webAppConfig", mockWebAppConfig);
        props.put("securityMetadata", mockSecurityMetadata);
        props.put("webAppSecurityConfig", mockWebAppSecurityConfig);
        final AuthenticationResult authenticationResult = new AuthenticationResult(AuthResult.FAILURE, (Subject) null);
        mock.checking(new Expectations() {
            {
                allowing(mockAuthConfigProvider).getServerAuthConfig(with(equal("HttpServlet")), with(equal("default_host /context-root")), with(any(CallbackHandler.class)));
                will(returnValue(mockServerAuthConfig));
                allowing(mockServerAuthConfig).getAuthContext(with(any(String.class)), with(any(Subject.class)), with(any(Map.class)));
                will(returnValue(mockServerAuthContext));
                allowing(mockReq).getMethod();
                allowing(mockServerAuthContext).validateRequest(with(any(MessageInfo.class)), with(any(Subject.class)), with(any(Subject.class)));
                will(returnValue(AuthStatus.SUCCESS));
                allowing(mockWebAppConfig).getApplicationName();
                will(returnValue("bob"));
                allowing(mockWebAppConfig).getVirtualHostName();
                will(returnValue("default_host"));
                allowing(mockWebAppConfig).getContextRoot();
                will(returnValue("/context-root"));
                allowing(mockWebAppConfig).getModuleName();
                will(returnValue("bobModuleName"));
                allowing(mockSecurityMetadata).getLoginConfiguration();
                will(returnValue(mockLoginConfiguration));
                allowing(mockLoginConfiguration).getAuthenticationMethod();
                will(returnValue(LoginConfiguration.BASIC));
                allowing(mockServerAuthConfig).getAuthContextID(with(any(MessageInfo.class)));
                will(returnValue("bobContextID"));
                allowing(mockWebProviderAuthenticatorHelper).loginWithHashtable(with(any(HttpServletRequest.class)),
                                                                                with(any(HttpServletResponse.class)),
                                                                                with(any(Subject.class)));
                will(returnValue(authenticationResult));
                allowing(mockReq).getServletContext();
                will(returnValue(mockServletContext));
                allowing(mockServletContext).setAttribute(with(any(String.class)), with(any(Object.class)));
                allowing(mockServletContext).getAttribute(with(any(String.class)));
                will(returnValue(null));
            }
        });
        loginHashtable = new Hashtable<String, Object>();
        AuthenticationResult result = jaspiWebAuthenticator.authenticate(mockReq, mockRsp, props);
        assertNotNull(result);
        assertEquals(result.getStatus(), AuthResult.FAILURE);
    }

    @Test
    public void testLoginSubjectIsNull() throws Exception {
        setupFactory();
        WebAuthenticator jaspiWebAuthenticator = new TestDoubleJaspiServiceImpl();
        HashMap<String, Object> props = new HashMap<String, Object>() {};
        props.put("webAppConfig", mockWebAppConfig);
        props.put("securityMetadata", mockSecurityMetadata);
        props.put("webAppSecurityConfig", mockWebAppSecurityConfig);
        final AuthenticationResult authenticationResult = new AuthenticationResult(AuthResult.SUCCESS, (Subject) null);
        mock.checking(new Expectations() {
            {
                allowing(mockAuthConfigProvider).getServerAuthConfig(with(equal("HttpServlet")), with(equal("default_host /context-root")), with(any(CallbackHandler.class)));
                will(returnValue(mockServerAuthConfig));
                allowing(mockServerAuthConfig).getAuthContext(with(any(String.class)), with(any(Subject.class)), with(any(Map.class)));
                will(returnValue(mockServerAuthContext));
                allowing(mockReq).getMethod();
                allowing(mockServerAuthContext).validateRequest(with(any(MessageInfo.class)), with(any(Subject.class)), with(any(Subject.class)));
                will(returnValue(AuthStatus.SUCCESS));
                allowing(mockWebAppConfig).getApplicationName();
                will(returnValue("bob"));
                allowing(mockWebAppConfig).getVirtualHostName();
                will(returnValue("default_host"));
                allowing(mockWebAppConfig).getContextRoot();
                will(returnValue("/context-root"));
                allowing(mockWebAppConfig).getModuleName();
                will(returnValue("bobModuleName"));
                allowing(mockSecurityMetadata).getLoginConfiguration();
                will(returnValue(mockLoginConfiguration));
                allowing(mockLoginConfiguration).getAuthenticationMethod();
                will(returnValue(LoginConfiguration.BASIC));
                allowing(mockServerAuthConfig).getAuthContextID(with(any(MessageInfo.class)));
                will(returnValue("bobContextID"));
                allowing(mockServerAuthConfig).getAuthContext(with("bobContextID"), with(any(Subject.class)), with(any(Map.class)));
                will(returnValue(mockServerAuthContext));
                allowing(mockWebProviderAuthenticatorHelper).loginWithHashtable(with(any(HttpServletRequest.class)),
                                                                                with(any(HttpServletResponse.class)),
                                                                                with(any(Subject.class)));
                will(returnValue(authenticationResult));
                allowing(mockReq).getServletContext();
                will(returnValue(mockServletContext));
                allowing(mockServletContext).setAttribute(with(any(String.class)), with(any(Object.class)));
                allowing(mockServletContext).getAttribute(with(any(String.class)));
                will(returnValue(null));
            }
        });
        loginHashtable = new Hashtable<String, Object>();
        AuthenticationResult result = jaspiWebAuthenticator.authenticate(mockReq, mockRsp, props);
        assertNotNull(result);
        assertEquals(result.getStatus(), AuthResult.FAILURE);
    }

    @Test
    public void testAuthContextFromProvider() throws Exception {
        TestDoubleJaspiServiceImpl jaspiService = new TestDoubleJaspiServiceImpl();
        mock.checking(new Expectations() {
            {
                allowing(mockWebSecurityContext).getJaspiAuthContext();
                will(returnValue(mockJaspiAuthContext));
                allowing(mockJaspiAuthContext).getServerAuthContext();
                will(returnValue(mockServerAuthContext));
                allowing(mockReq).getMethod();
            }
        });
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        ServerAuthContext sac = jaspiService.getServerAuthContext(jaspiRequest, mockAuthConfigProvider);
        assertNotNull(sac);
    }

    @Test
    public void testJaspiAuthContext() throws Exception {
        TestDoubleJaspiServiceImpl jaspiService = new TestDoubleJaspiServiceImpl();
        mock.checking(new Expectations() {
            {
                allowing(mockWebSecurityContext).getJaspiAuthContext();
                will(returnValue(mockJaspiAuthContext));
                allowing(mockJaspiAuthContext).getServerAuthContext();
                will(returnValue(mockServerAuthContext));
            }
        });
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        JaspiService.JaspiAuthContext jac = jaspiService.getJaspiAuthContext(jaspiRequest, mockAuthConfigProvider);
        assertNotNull(jac);
    }

    @Test(expected = AuthenticationException.class)
    public void testJaspiAuthContextThrowsAuthenticationException() throws Exception {
        TestDoubleJaspiServiceImpl jaspiService = new TestDoubleJaspiServiceImpl();
        mock.checking(new Expectations() {
            {
                allowing(mockWebSecurityContext).getJaspiAuthContext();
                will(throwException(new AuthenticationException("bob")));
            }
        });
        JaspiRequest jaspiRequest = new JaspiRequest(newWebRequest(), mockWebAppConfig);
        JaspiService.JaspiAuthContext jac = jaspiService.getJaspiAuthContext(jaspiRequest, mockAuthConfigProvider);
        assertNotNull(jac);
    }

    /**
     * Test double needed for various things that are not mockable
     */
    class TestDoubleJaspiServiceImpl extends JaspiServiceImpl {

        @Override
        public Hashtable<String, Object> getCustomCredentials(final Subject clientSubject) {
            return loginHashtable;
        }

        @Override
        protected synchronized WebProviderAuthenticatorHelper getWebProviderAuthenticatorHelper() {
            return mockWebProviderAuthenticatorHelper;
        }

        @Override
        public Subject getUnauthenticatedSubject() {
            return new Subject();
        }
    }
}
