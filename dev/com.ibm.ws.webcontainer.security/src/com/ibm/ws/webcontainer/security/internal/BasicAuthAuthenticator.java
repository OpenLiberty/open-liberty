/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import java.util.HashMap;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;

/**
 *
 */
public class BasicAuthAuthenticator implements WebAuthenticator {

    private static final TraceComponent tc = Tr.register(BasicAuthAuthenticator.class);
    public static final String BASIC_AUTH_HEADER_NAME = "Authorization";

    private AuthenticationService authenticationService = null;
    private UserRegistry userRegistry = null;
    private SSOCookieHelper ssoCookieHelper = null;
    private WebAppSecurityConfig config = null;

    public BasicAuthAuthenticator(AuthenticationService authnServ,
                                  UserRegistry ur,
                                  SSOCookieHelper ssoCookieHelper,
                                  WebAppSecurityConfig config) {
        authenticationService = authnServ;
        userRegistry = ur;
        this.ssoCookieHelper = ssoCookieHelper;
        this.config = config;
    }

    @Override
    public AuthenticationResult authenticate(WebRequest webRequest) {
        HttpServletRequest req = webRequest.getHttpServletRequest();
        HttpServletResponse res = webRequest.getHttpServletResponse();
        String realm = getBasicAuthRealmName(webRequest);
        AuthenticationResult result = null;
        result = handleBasicAuth(realm, req, res);
        if (result.getStatus() == AuthResult.SUCCESS) {
            ssoCookieHelper.addSSOCookiesToResponse(result.getSubject(), req, res);
        }
        return result;
    }

    @Override
    public AuthenticationResult authenticate(HttpServletRequest req, HttpServletResponse res, HashMap<String, Object> props) {
        AuthenticationResult result = null;
        String realm = "defaultRealm";
        if (userRegistry != null && config.getDisplayAuthenticationRealm()) {
            realm = userRegistry.getRealm();
        }
        result = handleBasicAuth(realm, req, res);
        if (result.getStatus() == AuthResult.SUCCESS) {
            ssoCookieHelper.addSSOCookiesToResponse(result.getSubject(), req, res);
        }
        return result;
    }

    /**
     * handleBasicAuth generates AuthenticationResult
     * This routine invokes basicAuthenticate which also generates AuthenticationResult.
     */
    private AuthenticationResult handleBasicAuth(String inRealm, HttpServletRequest req, HttpServletResponse res) {

        AuthenticationResult result = null;
        String hdrValue = req.getHeader(BASIC_AUTH_HEADER_NAME);
        if (hdrValue == null || !hdrValue.startsWith("Basic ")) {
            result = new AuthenticationResult(AuthResult.SEND_401, inRealm, AuditEvent.CRED_TYPE_BASIC, null, AuditEvent.OUTCOME_CHALLENGE);
            return result;
        }
        // Parse the username & password from the header.
        String encoding = req.getHeader("Authorization-Encoding");

        hdrValue = decodeBasicAuth(hdrValue.substring(6), encoding);

        int idx = hdrValue.indexOf(':');
        if (idx < 0) {
            result = new AuthenticationResult(AuthResult.SEND_401, inRealm, AuditEvent.CRED_TYPE_BASIC, null, AuditEvent.OUTCOME_CHALLENGE);
            return result;
        }

        String username = hdrValue.substring(0, idx);
        String password = hdrValue.substring(idx + 1);

        return basicAuthenticate(inRealm, username, password, req, res);
    }

    @FFDCIgnore(AuthenticationException.class)
    public AuthenticationResult basicAuthenticate(String realm, String username, @Sensitive String password, HttpServletRequest req, HttpServletResponse res) {
        AuthenticationResult authResult = null;
        try {
            String thisAuthMech = JaasLoginConfigConstants.SYSTEM_WEB_INBOUND;
            AuthenticationData authenticationData = createAuthenticationData(realm, username, password, req, res);
            Subject authenticatedSubject = authenticationService.authenticate(thisAuthMech, authenticationData, null);
            authResult = new AuthenticationResult(AuthResult.SUCCESS, authenticatedSubject, AuditEvent.CRED_TYPE_BASIC, username, AuditEvent.OUTCOME_SUCCESS);
        } catch (AuthenticationException e) {
            
            authResult = new AuthenticationResult(AuthResult.SEND_401, e.getMessage(), AuditEvent.CRED_TYPE_BASIC, username, AuditEvent.OUTCOME_DENIED);

            if (e instanceof com.ibm.ws.security.authentication.PasswordExpiredException) {  
                authResult.passwordExpired = true;
            } else if (e instanceof com.ibm.ws.security.authentication.UserRevokedException) {
                authResult.userRevoked = true;
            }
        }
        authResult.realm = realm;
        authResult.username = username;
        authResult.password = password;
        return authResult;
    }

    @Trivial
    protected AuthenticationData createAuthenticationData(String realm, String username, @Sensitive String password, HttpServletRequest req, HttpServletResponse res) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.USERNAME, username);
        authenticationData.set(AuthenticationData.PASSWORD, password.toCharArray());
        authenticationData.set(AuthenticationData.HTTP_SERVLET_REQUEST, req);
        authenticationData.set(AuthenticationData.HTTP_SERVLET_RESPONSE, res);
        return authenticationData;
    }

    /**
     * Return a realm name if it's defined in the web.xml file. If it's not defined in the web.xml
     * and displayAuthenticationRealm is set to true, then return the userRegistry realm. Otherwise return
     * the realm as "Default Realm".
     *
     * @param webRequest
     * @return realm
     */
    protected String getBasicAuthRealmName(WebRequest webRequest) {
        SecurityMetadata securityMetadata = webRequest.getSecurityMetadata();
        if (securityMetadata != null) {
            LoginConfiguration loginConfig = securityMetadata.getLoginConfiguration();
            if (loginConfig != null && loginConfig.getRealmName() != null) {
                return loginConfig.getRealmName();
            }
            if (config.getDisplayAuthenticationRealm()) {
                return userRegistry.getRealm();
            }
        }
        String realm = "defaultRealm";
        return realm;
    }

    // 100133: note
    // 1. Both data and encoding should not be null, since there is no null object check prior to introspect them.
    // 2. This method intentionally returns empty string in case of error. With that, a caller doesn't need to check null object prior to introspect it.
    @Sensitive
    protected String decodeBasicAuth(String data, String encoding) {
        String output = "";
        byte decodedByte[] = null;
        decodedByte = Base64Coder.base64DecodeString(data);
        if (decodedByte != null && decodedByte.length > 0) {
            boolean decoded = false;
            if (encoding != null) {
                try {
                    output = new String(decodedByte, encoding);
                    decoded = true;
                } catch (Exception e) {
                    // fall back not to use encoding..
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "An exception is caught using the encoder: " + encoding + ". The exception is: " + e.getMessage());
                    }
                }
            }
            if (!decoded) {
                output = new String(decodedByte);
            }
        }
        return output;
    }
}
