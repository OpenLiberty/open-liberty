/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.PasswordExpiredException;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;

/**
 *
 */
public class BasicAuthAuthenticatorTest {
    private final Mockery mock = new JUnit4Mockery();
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse rsp = mock.mock(HttpServletResponse.class);
    private final WebRequest webRequest = mock.mock(WebRequest.class);
    private final AuthenticationService authnService = mock.mock(AuthenticationService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);
    private static AuthenticationData authData = new WSAuthenticationData();
    private final SSOCookieHelper ssoCookieHelper = mock.mock(SSOCookieHelper.class);
    private final WebAppSecurityConfig webAppSecurityConfig = mock.mock(WebAppSecurityConfig.class);
    private final LoginConfiguration loginConfiguration = mock.mock(LoginConfiguration.class);
    private final SecurityMetadata securityMetadata = mock.mock(SecurityMetadata.class);
    private final String user = "user1";
    private final String password = "user1pwd";
    private final String realm = "WebRealm";
    private final String defaultRealm = "Default Realm";
    private BasicAuthAuthenticator basicAuthenticator;

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(webRequest).getHttpServletRequest();
                will(returnValue(req));
                allowing(webRequest).getHttpServletResponse();
                will(returnValue(rsp));
                allowing(userRegistry).getRealm();
                will(returnValue(realm));
            }
        });
        basicAuthenticator = new BasicAuthAuthenticator(authnService, userRegistry, ssoCookieHelper, webAppSecurityConfig);
    }

    @Factory
    private static Matcher<AuthenticationData> matchingAuthenticationData(AuthenticationData authData) {
        return new AuthenticationDataMatcher(authData);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.BasicAuthAuthenticator#authenticate(com.ibm.ws.webcontainer.security.WebRequest)} .
     */
    @Test
    public void testAuthenticate() throws Exception {
        final Subject authSubject = new Subject();
        final AuthenticationData authData = new WSAuthenticationData();
        authData.set(AuthenticationData.USERNAME, user);
        authData.set(AuthenticationData.PASSWORD, password.toCharArray());

        mock.checking(new Expectations() {
            {
                allowing(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue("Basic dXNlcjE6dXNlcjFwd2Q="));
                allowing(req).getHeader("Authorization-Encoding");
                will(returnValue(null));
                allowing(authnService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)), with(matchingAuthenticationData(authData)),
                                                    with(equal((Subject) null)));
                will(returnValue(authSubject));
                one(ssoCookieHelper).addSSOCookiesToResponse(authSubject, req, rsp);
                allowing(webRequest).getSecurityMetadata();
                allowing(loginConfiguration).getRealmName();
                allowing(webAppSecurityConfig).getDisplayAuthenticationRealm();
                will(returnValue(true));

            }
        });
        AuthenticationResult authResult = basicAuthenticator.authenticate(webRequest);
        assertEquals("User should be " + user, user, authResult.getUserName());
        assertEquals("Password should be " + password, password, authResult.password);
        assertEquals("Status should be SUCCESS.", AuthResult.SUCCESS, authResult.getStatus());
        assertEquals("Subject sould be the authenticated subject.", authSubject, authResult.getSubject());
    }


    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.BasicAuthAuthenticator#authenticate(com.ibm.ws.webcontainer.security.WebRequest)} .
     */
    @Test
    public void testAuthenticate_UTF8() throws Exception {
        final Subject authSubject = new Subject();
        final AuthenticationData authData = new WSAuthenticationData();
        String multiUser = new String(new byte[] { 0x00, 0x4e, 0x01, 0x4e, 0x03, 0x4e }, "UTF-16LE");
        String userId = "U" + multiUser + "_" + "0";
        String multiPwd = new String(new byte[] { 0x00, 0x5e, 0x01, 0x5e, 0x03, 0x5e }, "UTF-16LE");
        String userPwd = "P" + multiPwd + "_" + "1";
        authData.set(AuthenticationData.USERNAME, userId);
        authData.set(AuthenticationData.PASSWORD, userPwd.toCharArray());

        mock.checking(new Expectations() {
            {
                allowing(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue("Basic VeS4gOS4geS4g18wOlDluIDluIHluINfMQ=="));
                allowing(req).getHeader("Authorization-Encoding");
                will(returnValue("UTF-8"));
                allowing(authnService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)), with(matchingAuthenticationData(authData)),
                                                    with(equal((Subject) null)));
                will(returnValue(authSubject));
                one(ssoCookieHelper).addSSOCookiesToResponse(authSubject, req, rsp);
                allowing(webRequest).getSecurityMetadata();
                allowing(loginConfiguration).getRealmName();
                allowing(webAppSecurityConfig).getDisplayAuthenticationRealm();
                will(returnValue(true));

            }
        });
        AuthenticationResult authResult = basicAuthenticator.authenticate(webRequest);
        assertEquals("User should be " + userId, userId, authResult.getUserName());
        assertEquals("Password should be " + userPwd, userPwd, authResult.password);
        assertEquals("Status should be SUCCESS.", AuthResult.SUCCESS, authResult.getStatus());
        assertEquals("Subject sould be the authenticated subject.", authSubject, authResult.getSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.BasicAuthAuthenticator#authenticate(com.ibm.ws.webcontainer.security.WebRequest)} .
     */
    @Test
    public void testAuthenticate_challengeNullHeader() {

        mock.checking(new Expectations() {
            {
                allowing(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue(null));
                allowing(webRequest).getSecurityMetadata();
            }
        });

        AuthenticationResult authResult = basicAuthenticator.authenticate(webRequest);
        assertEquals("Status should be SEND_401.", AuthResult.SEND_401, authResult.getStatus());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.BasicAuthAuthenticator#authenticate(com.ibm.ws.webcontainer.security.WebRequest)} .
     */
    @Test
    public void testAuthenticate_challengeInvalidHeader() {
        mock.checking(new Expectations() {
            {
                allowing(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue("Bad header"));
                allowing(webRequest).getSecurityMetadata();
            }
        });

        AuthenticationResult authResult = basicAuthenticator.authenticate(webRequest);
        assertEquals("Status should be SEND_401.", AuthResult.SEND_401, authResult.getStatus());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.BasicAuthAuthenticator#authenticate(com.ibm.ws.webcontainer.security.WebRequest)} .
     */
    @Test
    public void testAuthenticate_badAuthorizationHeader() {
        final Subject authSubject = new Subject();
        authData.set(AuthenticationData.USERNAME, user);
        authData.set(AuthenticationData.PASSWORD, password.toCharArray());
        try {
            mock.checking(new Expectations() {
                {
                    allowing(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                    will(returnValue("Basic dXNlcjFwd2Q="));
                    allowing(req).getHeader("Authorization-Encoding");
                    will(returnValue(null));
                    allowing(authnService).authenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authData, null);
                    will(returnValue(authSubject));
                    allowing(webRequest).getSecurityMetadata();
                }
            });
        } catch (AuthenticationException e) {
            fail("Unexpected AuthenticationException" + e);
        }
        AuthenticationResult authResult = basicAuthenticator.authenticate(webRequest);
        assertEquals("Status should be SEND_401.", AuthResult.SEND_401, authResult.getStatus());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.BasicAuthAuthenticator#authenticate(com.ibm.ws.webcontainer.security.WebRequest)} .
     */
    @Test
    public void testAuthenticate_authnFailure() {
        
        authData.set(AuthenticationData.USERNAME, user);
        authData.set(AuthenticationData.PASSWORD, password.toCharArray());
        try {
            mock.checking(new Expectations() {
                {
                    allowing(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                    will(returnValue("Basic dXNlcjE6dXNlcjFwd2Q="));
                    allowing(req).getHeader("Authorization-Encoding");
                    will(returnValue(null));
                    allowing(authnService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)), with(matchingAuthenticationData(authData)),
                                                        with(equal((Subject) null)));
                    will(throwException(new AuthenticationException("authn failed")));
                    allowing(webRequest).getSecurityMetadata();
                }
            });
        } catch (AuthenticationException e) {
            fail("Unexpected AuthenticationException" + e);
        }
        AuthenticationResult authResult = basicAuthenticator.authenticate(webRequest);
        assertEquals("Status should be SEND_401.", AuthResult.SEND_401, authResult.getStatus());
    }
    @Test
    public void testAuthenticate_passwordExpired() throws Exception {
        final Subject authSubject = new Subject();
        final AuthenticationData authData = new WSAuthenticationData();
        authData.set(AuthenticationData.USERNAME, user);
        authData.set(AuthenticationData.PASSWORD, password.toCharArray());

        mock.checking(new Expectations() {
            {
                allowing(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue("Basic dXNlcjE6dXNlcjFwd2Q="));
                allowing(req).getHeader("Authorization-Encoding");
                will(returnValue(null));
                allowing(authnService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)), with(matchingAuthenticationData(authData)),
                                                    with(equal((Subject) null)));
                will(throwException(new com.ibm.ws.security.authentication.PasswordExpiredException("authn failed")));
                one(ssoCookieHelper).addSSOCookiesToResponse(authSubject, req, rsp);
                allowing(webRequest).getSecurityMetadata();
                allowing(loginConfiguration).getRealmName();
                allowing(webAppSecurityConfig).getDisplayAuthenticationRealm();
                will(returnValue(true));

            }
        });
        AuthenticationResult authResult = basicAuthenticator.authenticate(webRequest);
        assertEquals("PasswordExpired", true, authResult.getPasswordExpired());
    }
    @Test
    public void testAuthenticate_userRevoked() throws Exception {
        final Subject authSubject = new Subject();
        final AuthenticationData authData = new WSAuthenticationData();
        authData.set(AuthenticationData.USERNAME, user);
        authData.set(AuthenticationData.PASSWORD, password.toCharArray());

        mock.checking(new Expectations() {
            {
                allowing(req).getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME);
                will(returnValue("Basic dXNlcjE6dXNlcjFwd2Q="));
                allowing(req).getHeader("Authorization-Encoding");
                will(returnValue(null));
                allowing(authnService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)), with(matchingAuthenticationData(authData)),
                                                    with(equal((Subject) null)));
                will(throwException(new com.ibm.ws.security.authentication.UserRevokedException("authn failed")));
                one(ssoCookieHelper).addSSOCookiesToResponse(authSubject, req, rsp);
                allowing(webRequest).getSecurityMetadata();
                allowing(loginConfiguration).getRealmName();
                allowing(webAppSecurityConfig).getDisplayAuthenticationRealm();
                will(returnValue(true));

            }
        });
        AuthenticationResult authResult = basicAuthenticator.authenticate(webRequest);
        assertEquals("PasswordExpired", true, authResult.getUserRevoked());
    }
    /**
     * Test for null request
     */
    @Test(expected = NullPointerException.class)
    public void testGetBasicAuthRealmName_null() {
        basicAuthenticator.getBasicAuthRealmName(null);
    }

    /**
     * Test for null loginConfig
     */
    public void testGetBasicAuthRealmName_nullLoginConfig() {
        mock.checking(new Expectations() {
            {
                one(webRequest).getSecurityMetadata();
                will(returnValue(securityMetadata));
                one(securityMetadata).getLoginConfiguration();
                will(returnValue(null));
            }
        });
        assertEquals("The realm name should be Default Realm", defaultRealm, basicAuthenticator.getBasicAuthRealmName(webRequest));
    }

    /**
     * Test for web.xml file have null realm element
     */
    public void testGetBasicAuthRealmName_nullRealmName() {
        mock.checking(new Expectations() {
            {
                one(webRequest).getSecurityMetadata();
                will(returnValue(securityMetadata));
                one(securityMetadata).getLoginConfiguration();
                will(returnValue(loginConfiguration));
                one(loginConfiguration).getRealmName();
                will(returnValue(null));
            }
        });
        assertEquals("The realm name should be Default Realm", defaultRealm, basicAuthenticator.getBasicAuthRealmName(webRequest));
    }

    /**
     * Test for realm name defined in the web.xml
     */
    public void testGetBasicAuthRealmName_web_xml_realm() {
        mock.checking(new Expectations() {
            {
                one(webRequest).getSecurityMetadata();
                will(returnValue(securityMetadata));
                one(securityMetadata).getLoginConfiguration();
                will(returnValue(loginConfiguration));
                one(loginConfiguration).getRealmName();
                will(returnValue("web_xml_realm"));
            }
        });
        assertEquals("The realm name should be web_xml_realm", "web_xml_realm", basicAuthenticator.getBasicAuthRealmName(webRequest));
    }

    /**
     * Test for user registry realm name
     */
    public void testGetBasicAuthRealmName_userRegistry_realm() {
        mock.checking(new Expectations() {
            {
                one(webRequest).getSecurityMetadata();
                will(returnValue(securityMetadata));
                one(securityMetadata).getLoginConfiguration();
                will(returnValue(loginConfiguration));
                one(loginConfiguration).getRealmName();
                will(returnValue(null));
                one(webAppSecurityConfig).getDisplayAuthenticationRealm();
                will(returnValue(true));
                one(userRegistry).getRealm();
                one(returnValue(realm));

            }
        });
        assertEquals("The realm name should be webRealm", realm, basicAuthenticator.getBasicAuthRealmName(webRequest));
    }

    /**
     * Test for do not display the realm name
     */
    public void testGetBasicAuthRealmName_dontDisplayRealm() {
        mock.checking(new Expectations() {
            {
                one(webRequest).getSecurityMetadata();
                will(returnValue(securityMetadata));
                one(securityMetadata).getLoginConfiguration();
                will(returnValue(loginConfiguration));
                one(loginConfiguration).getRealmName();
                will(returnValue(null));
                one(webAppSecurityConfig).getDisplayAuthenticationRealm();
                will(returnValue(false));

            }
        });
        assertEquals("The realm name should be Default Realm", defaultRealm, basicAuthenticator.getBasicAuthRealmName(webRequest));
    }
}
