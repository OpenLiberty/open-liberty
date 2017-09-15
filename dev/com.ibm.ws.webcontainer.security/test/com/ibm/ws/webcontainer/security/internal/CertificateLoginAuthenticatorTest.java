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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

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
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebRequest;

/**
 *
 */
public class CertificateLoginAuthenticatorTest {
    private final Mockery mock = new JUnit4Mockery();
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse rsp = mock.mock(HttpServletResponse.class);
    private final WebRequest webRequest = mock.mock(WebRequest.class);
    private final AuthenticationService authnService = mock.mock(AuthenticationService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);
    private final AuthenticationData authData = new WSAuthenticationData();
    private final SSOCookieHelper ssoCookieHelper = mock.mock(SSOCookieHelper.class);
    private X509Certificate certChain[];
    private final String realm = "WebRealm";
    private CertificateLoginAuthenticator certLoginAuthenticator;

    @Before
    public void setUp() throws Exception {
        InputStream inStream = new FileInputStream("publish" + File.separator + "certificates" + File.separator + "gooduser.cer");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
        inStream.close();
        certChain = new X509Certificate[] { cert, null };
        mock.checking(new Expectations() {
            {
                allowing(webRequest).getHttpServletRequest();
                will(returnValue(req));
                allowing(webRequest).getHttpServletResponse();
                will(returnValue(rsp));
                allowing(userRegistry).getRealm();
                will(returnValue(realm));
                allowing(req).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(certChain));
            }
        });
        certLoginAuthenticator =
                        new CertificateLoginAuthenticator(authnService, ssoCookieHelper);
    }

    @Factory
    private static Matcher<AuthenticationData> matchingAuthenticationData(AuthenticationData authData) {
        return new AuthenticationDataMatcher(authData);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.CertificateLoginAuthenticator#authenticate(com.ibm.ws.webcontainer.security.WebRequest)} .
     */
    @Test
    public void testAuthenticate() throws Exception {
        final Subject authSubject = new Subject();
        mock.checking(new Expectations() {
            {
                allowing(authnService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)), with(matchingAuthenticationData(authData)),
                                                    with(equal((Subject) null)));
                will(returnValue(authSubject));
                one(ssoCookieHelper).addSSOCookiesToResponse(authSubject, req, rsp);
            }
        });
        AuthenticationResult authResult = certLoginAuthenticator.authenticate(webRequest);
        String certdn = certChain[0].getSubjectX500Principal().getName();
        assertEquals("Cert dn should be " + certdn, certdn, authResult.getCertificateDN());
        assertEquals("Status should be SUCCESS.", AuthResult.SUCCESS, authResult.getStatus());
        assertEquals("Subject sould be the authenticated subject.", authSubject, authResult.getSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.CertificateLoginAuthenticator#authenticate(com.ibm.ws.webcontainer.security.WebRequest)} .
     */
    @Test
    public void testAuthenticate_authnFailure() {
        try {
            mock.checking(new Expectations() {
                {
                    allowing(authnService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND)), with(matchingAuthenticationData(authData)),
                                                        with(equal((Subject) null)));
                    will(throwException(new AuthenticationException("authn failed")));
                }
            });
        } catch (AuthenticationException e) {
            fail("Unexpected AuthenticationException" + e);
        }
        AuthenticationResult authResult = certLoginAuthenticator.authenticate(webRequest);
        assertEquals("Status should be FAILURE.", AuthResult.FAILURE, authResult.getStatus());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.CertificateLoginAuthenticator#authenticate(com.ibm.ws.webcontainer.security.WebRequest)} .
     */
    @Test
    public void testAuthenticate_nullCertChain() {
        final WebRequest webRequest = mock.mock(WebRequest.class, "nullCertChain_webRequest");
        final HttpServletRequest req = mock.mock(HttpServletRequest.class, "nullCertChain_req");
        mock.checking(new Expectations() {
            {
                allowing(webRequest).getHttpServletRequest();
                will(returnValue(req));
                allowing(webRequest).getHttpServletResponse();
                allowing(req).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(null));
            }
        });
        AuthenticationResult authResult = certLoginAuthenticator.authenticate(webRequest);
        assertEquals("Status should be FAILURE.", AuthResult.FAILURE, authResult.getStatus());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.CertificateLoginAuthenticator#authenticate(com.ibm.ws.webcontainer.security.WebRequest)} .
     */
    @Test
    public void testAuthenticate_emptyCertChain() {
        final WebRequest webRequest = mock.mock(WebRequest.class, "emptyCertChain_webRequest");
        final HttpServletRequest req = mock.mock(HttpServletRequest.class, "emptyCertChain_req");
        mock.checking(new Expectations() {
            {
                allowing(webRequest).getHttpServletRequest();
                will(returnValue(req));
                allowing(webRequest).getHttpServletResponse();
                allowing(req).getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
                will(returnValue(new X509Certificate[] {}));
            }
        });
        AuthenticationResult authResult = certLoginAuthenticator.authenticate(webRequest);
        assertEquals("Status should be FAILURE.", AuthResult.FAILURE, authResult.getStatus());
    }
}
