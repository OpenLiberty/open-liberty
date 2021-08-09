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

import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.WebRequest;

/**
 *
 */
public class CertificateLoginAuthenticator implements WebAuthenticator {

    private static final TraceComponent tc = Tr.register(CertificateLoginAuthenticator.class);
    private AuthenticationService authenticationService = null;
    private SSOCookieHelper ssoCookieHelper = null;
    public static final String PEER_CERTIFICATES = "javax.net.ssl.peer_certificates";

    public CertificateLoginAuthenticator(AuthenticationService authnServ,
                                         SSOCookieHelper ssoCookieHelper) {
        authenticationService = authnServ;
        this.ssoCookieHelper = ssoCookieHelper;
    }

    /** {@inheritDoc} */
    @Override
    public AuthenticationResult authenticate(WebRequest webRequest) {
        HttpServletRequest req = webRequest.getHttpServletRequest();
        HttpServletResponse res = webRequest.getHttpServletResponse();
        return authenticate(req, res, null);
    }

    @Override
    @FFDCIgnore(AuthenticationException.class)
    public AuthenticationResult authenticate(HttpServletRequest req, HttpServletResponse res, HashMap props) {
        AuthenticationResult authResult = null;
        X509Certificate certChain[] = (X509Certificate[]) req.getAttribute(PEER_CERTIFICATES);
        if (certChain == null || certChain.length == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The CLIENT-CERT authentication failed because no client certificate was found.");
            }
            authResult = new AuthenticationResult(AuthResult.FAILURE, "The CLIENT-CERT authentication failed because no client certificate was found.");
            authResult.setAuditCredType(AuditEvent.CRED_TYPE_CERTIFICATE);
            authResult.setAuditCredValue("UNAUTHORIZED");
            authResult.setAuditOutcome(AuditEvent.OUTCOME_FAILURE);
            return authResult;
        }
        try {
            String thisAuthMech = JaasLoginConfigConstants.SYSTEM_WEB_INBOUND;
            AuthenticationData authenticationData = new WSAuthenticationData();
            authenticationData.set(AuthenticationData.HTTP_SERVLET_REQUEST, req);
            authenticationData.set(AuthenticationData.HTTP_SERVLET_RESPONSE, res);
            authenticationData.set(AuthenticationData.CERTCHAIN, certChain);
            Subject authenticatedSubject = authenticationService.authenticate(thisAuthMech, authenticationData, null);
            String certDN = certChain[0].getSubjectX500Principal().getName();
            authResult = new AuthenticationResult(AuthResult.SUCCESS, authenticatedSubject, AuditEvent.CRED_TYPE_CERTIFICATE, certDN, AuditEvent.OUTCOME_SUCCESS);
        } catch (AuthenticationException e) {
            String certDN = certChain[0].getSubjectX500Principal().getName();
            authResult = new AuthenticationResult(AuthResult.FAILURE, e.getMessage(), AuditEvent.CRED_TYPE_CERTIFICATE, certDN, AuditEvent.OUTCOME_DENIED);
        }
        authResult.certdn = certChain[0].getSubjectX500Principal().getName();
        if (authResult.getStatus() == AuthResult.SUCCESS) {
            ssoCookieHelper.addSSOCookiesToResponse(authResult.getSubject(), req, res);
        }
        return authResult;

    }
}
