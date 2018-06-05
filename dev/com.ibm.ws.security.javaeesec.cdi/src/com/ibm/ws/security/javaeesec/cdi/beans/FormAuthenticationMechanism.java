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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.CDI;
import javax.security.auth.Subject;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.security.token.AttributeNameConstants;

@Default
@ApplicationScoped
@LoginToContinue
public class FormAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final TraceComponent tc = Tr.register(FormAuthenticationMechanism.class);

    private final Utils utils;

    public FormAuthenticationMechanism() {
        utils = new Utils();
    }

    // this is for unit test.
    protected FormAuthenticationMechanism(Utils utils) {
        this.utils = utils;
    }

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

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
            status = utils.handleAuthenticate(getCDI(), JavaEESecConstants.DEFAULT_REALM, cred, clientSubject, httpMessageContext);
        } else {
            HttpServletRequest req = httpMessageContext.getRequest();
            HttpServletResponse rsp = httpMessageContext.getResponse();
            String username = null;
            String password = null;
            // in order to preserve the post parameter, unless the target url is j_security_check, do not read
            // j_username and j_password.
            String uri = req.getRequestURI();
            if (uri.contains("/j_security_check")) {
                username = req.getParameter("j_username");
                password = req.getParameter("j_password");
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "URI : " + uri + ", j_username : " + username);
            }

            if (httpMessageContext.isAuthenticationRequest()) {
                if (username != null && password != null) {
                    status = handleFormLogin(username, password, rsp, clientSubject, httpMessageContext);
                } else {
                    status = AuthenticationStatus.SEND_CONTINUE;
                }
            } else {
                if (username == null || password == null) {
                    if (httpMessageContext.isProtected() == false) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "both isAuthenticationRequest and isProtected returns false. returing NOT_DONE,");
                        }
                        status = AuthenticationStatus.NOT_DONE;
                    } else {
                        status = AuthenticationStatus.SEND_CONTINUE;
                    }
                } else {
                    status = handleFormLogin(username, password, rsp, clientSubject, httpMessageContext);
                }
            }
        }

        return status;
    }

    @Override
    public AuthenticationStatus secureResponse(HttpServletRequest request,
                                               HttpServletResponse response,
                                               HttpMessageContext httpMessageContext) throws AuthenticationException {
        return AuthenticationStatus.SUCCESS;
    }

    @Override
    public void cleanSubject(HttpServletRequest request,
                             HttpServletResponse response,
                             HttpMessageContext httpMessageContext) {

    }

    /**
     * note that both username and password should not be null.
     */

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private AuthenticationStatus handleFormLogin(String username, @Sensitive String password, HttpServletResponse rsp, Subject clientSubject,
                                                 HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        int rspStatus = HttpServletResponse.SC_UNAUTHORIZED;
        UsernamePasswordCredential credential = new UsernamePasswordCredential(username, password);
        status = utils.validateUserAndPassword(getCDI(), JavaEESecConstants.DEFAULT_REALM, clientSubject, credential, httpMessageContext);
        if (status == AuthenticationStatus.SUCCESS) {
            Map messageInfoMap = httpMessageContext.getMessageInfo().getMap();
            messageInfoMap.put("javax.servlet.http.authType", "FORM");
            messageInfoMap.put("javax.servlet.http.registerSession", Boolean.TRUE.toString());
            rspStatus = HttpServletResponse.SC_OK;
        } else if (status == AuthenticationStatus.NOT_DONE) {
            // set SC_OK, since if the target is not protected, it'll be processed.
            rspStatus = HttpServletResponse.SC_OK;
        } else {
            // TODO: Audit invalid user or password
        }
        rsp.setStatus(rspStatus);
        return status;
    }

    protected CDI getCDI() {
        return CDI.current();
    }

}
