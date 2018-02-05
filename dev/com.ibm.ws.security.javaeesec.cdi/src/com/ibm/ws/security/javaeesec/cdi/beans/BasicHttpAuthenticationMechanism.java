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

import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.security.auth.Subject;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.wsspi.security.token.AttributeNameConstants;

@Default
@ApplicationScoped
public class BasicHttpAuthenticationMechanism implements HttpAuthenticationMechanism {
    ModulePropertiesProvider mpp = null;

    private static final TraceComponent tc = Tr.register(BasicHttpAuthenticationMechanism.class);

    private String realmName = null;
    private final String DEFAULT_REALM = "defaultRealm";
    private Utils utils = new Utils();

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        setRealmName();
        Subject clientSubject = httpMessageContext.getClientSubject();
        String authHeader = httpMessageContext.getRequest().getHeader("Authorization");

        if (authHeader == null) {
            status = handleNoAuthorizationHeader(httpMessageContext);
        } else {
            status = handleAuthorizationHeader(authHeader, clientSubject, httpMessageContext);
        }

        return status;
    }

    private void setRealmName() {
        mpp = getModulePropertiesProvider();
        if (mpp != null) {
            Properties props = mpp.getAuthMechProperties(BasicHttpAuthenticationMechanism.class);
            if (props != null) {
                realmName = (String)props.get(JavaEESecConstants.REALM_NAME);
            }
        }
        if (realmName == null || realmName.trim().isEmpty()) {
            Tr.warning(tc, "JAVAEESEC_CDI_WARNING_NO_REALM_NAME");
            realmName = DEFAULT_REALM;
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

    @SuppressWarnings("unchecked")
    private AuthenticationStatus handleAuthorizationHeader(@Sensitive String authorizationHeader, Subject clientSubject,
                                                           HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        int rspStatus = HttpServletResponse.SC_FORBIDDEN;
        if (authorizationHeader.startsWith("Basic ")) {
            String encodedHeader = authorizationHeader.substring(6);
            String basicAuthHeader = decodeCookieString(encodedHeader);

            if (isAuthorizationHeaderValid(basicAuthHeader)) { // BasicAuthenticationCredential.isValid does not work
                BasicAuthenticationCredential basicAuthCredential = new BasicAuthenticationCredential(encodedHeader);
                status = utils.validateUserAndPassword(getCDI(), realmName, clientSubject, basicAuthCredential, httpMessageContext);
                if (status == AuthenticationStatus.SUCCESS) {
                    httpMessageContext.getMessageInfo().getMap().put("javax.servlet.http.authType", "JASPI_AUTH");
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