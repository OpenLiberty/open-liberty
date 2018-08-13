/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.cdi.beans;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.security.auth.Subject;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.credential.Credential;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;
import com.ibm.ws.webcontainer.security.util.WebConfigUtils;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.security.token.AttributeNameConstants;

@Default
@ApplicationScoped
public class BasicHttpAuthenticationMechanism implements HttpAuthenticationMechanism {
    ModulePropertiesProvider mpp = null;

    private static final TraceComponent tc = Tr.register(BasicHttpAuthenticationMechanism.class);

    private String realmName = "";

    private final Utils utils;

    public BasicHttpAuthenticationMechanism() {
        utils = new Utils();
    }

    // this is for unit test.
    protected BasicHttpAuthenticationMechanism(Utils utils) {
        this.utils = utils;
    }

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        if (httpMessageContext.getRequest().getUserPrincipal() != null) {
            httpMessageContext.getResponse().setStatus(HttpServletResponse.SC_OK);
            return AuthenticationStatus.SUCCESS;
        }

        setRealmName();
        Subject clientSubject = httpMessageContext.getClientSubject();

        AuthenticationParameters authParams = httpMessageContext.getAuthParameters();
        Credential cred = null;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "AuthenticationParameters : " + authParams);
        }
        if (authParams != null) {
            cred = authParams.getCredential();
        }
        if (cred != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Credential is found.");
            }
            status = utils.handleAuthenticate(getCDI(), realmName, cred, clientSubject, httpMessageContext);
        } else {
            String authHeader = httpMessageContext.getRequest().getHeader("Authorization");

            if (authHeader == null) {
                status = handleNoAuthorizationHeader(httpMessageContext);
            } else {
                status = handleAuthorizationHeader(authHeader, clientSubject, httpMessageContext);
            }
        }
        return status;
    }

    private void setRealmName() {
        mpp = getModulePropertiesProvider();
        if (mpp != null) {
            Properties props = mpp.getAuthMechProperties(BasicHttpAuthenticationMechanism.class);
            if (props != null) {
                realmName = (String) props.get(JavaEESecConstants.REALM_NAME);
            }
        }
    }

    private AuthenticationStatus handleNoAuthorizationHeader(HttpMessageContext httpMessageContext) {
        AuthenticationStatus status;
        if (httpMessageContext.isAuthenticationRequest() == false && httpMessageContext.isProtected() == false) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "both isAuthenticationRequest and isProtected returns false. returing NOT_DONE,");
            }
            status = AuthenticationStatus.NOT_DONE;
        } else {
            status = setChallengeAuthorizationHeader(httpMessageContext);
        }
        return status;
    }

    @SuppressWarnings("unchecked")
    private AuthenticationStatus setChallengeAuthorizationHeader(HttpMessageContext httpMessageContext) {
        HttpServletResponse rsp = httpMessageContext.getResponse();
        rsp.setHeader("WWW-Authenticate", "Basic realm=\"" + realmName + "\"");
        rsp.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        httpMessageContext.getMessageInfo().getMap().put(AttributeNameConstants.WSCREDENTIAL_REALM, realmName);

        return AuthenticationStatus.SEND_CONTINUE;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private AuthenticationStatus handleAuthorizationHeader(@Sensitive String authorizationHeader, Subject clientSubject,
                                                           HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        int rspStatus = HttpServletResponse.SC_UNAUTHORIZED;
        if (authorizationHeader.startsWith("Basic ")) {
            String encodedHeader = authorizationHeader.substring(6);
            String basicAuthHeader = decodeCookieString(encodedHeader);

            if (isAuthorizationHeaderValid(basicAuthHeader)) { // BasicAuthenticationCredential.isValid does not work
                BasicAuthenticationCredential basicAuthCredential = new BasicAuthenticationCredential(encodedHeader);
                status = utils.validateUserAndPassword(getCDI(), realmName, clientSubject, basicAuthCredential, httpMessageContext);
                if (status == AuthenticationStatus.SUCCESS) {
                    Map messageInfoMap = httpMessageContext.getMessageInfo().getMap();
                    messageInfoMap.put("javax.servlet.http.authType", "BASIC");
                    messageInfoMap.put("javax.servlet.http.registerSession", Boolean.TRUE.toString());
                    rspStatus = HttpServletResponse.SC_OK;
                } else if (status == AuthenticationStatus.NOT_DONE) {
                    // set SC_OK, since if the target is not protected, it'll be processed.
                    rspStatus = HttpServletResponse.SC_OK;
                }
            }
        }
        httpMessageContext.getResponse().setStatus(rspStatus);
        return status;
    }

    @Sensitive
    private String decodeCookieString(@Sensitive String cookieString) {
        try {
            return Base64Coder.base64Decode(cookieString);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAuthorizationHeaderValid(@Sensitive String basicAuthHeader) {
        int index = -1;
        boolean isNotValid = basicAuthHeader == null || basicAuthHeader.isEmpty() || (index = basicAuthHeader.indexOf(':')) <= 0 || index == basicAuthHeader.length() - 1;
        return !isNotValid;
    }

    @SuppressWarnings("rawtypes")
    protected CDI getCDI() {
        return CDI.current();
    }

    @SuppressWarnings("unchecked")
    protected ModulePropertiesProvider getModulePropertiesProvider() {
        Instance<ModulePropertiesProvider> modulePropertiesProivderInstance = getCDI().select(ModulePropertiesProvider.class);
        if (modulePropertiesProivderInstance != null) {
            return modulePropertiesProivderInstance.get();
        }
        return null;
    }

    // this is for unit test.
    protected void setMPP(ModulePropertiesProvider mpp) {
        this.mpp = mpp;
    }
}