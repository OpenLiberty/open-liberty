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
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.security.enterprise.credential.Credential;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;

@Default
@ApplicationScoped
@LoginToContinue
public class CustomFormAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final TraceComponent tc = Tr.register(CustomFormAuthenticationMechanism.class);
    private Utils utils = new Utils();

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        Subject clientSubject = httpMessageContext.getClientSubject();
        @SuppressWarnings("unchecked")
        AuthenticationParameters authParams = httpMessageContext.getAuthParameters();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "AuthenticationParameters : " + authParams);
        }
        if (authParams == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No AuthenticationParameters object, redirecting");
            }
            status = AuthenticationStatus.SEND_CONTINUE;
        } else {
            Credential cred = authParams.getCredential();
            if (cred == null) {
                if (!httpMessageContext.isAuthenticationRequest() && !httpMessageContext.isProtected()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "both isAuthenticationRequest and isProtected return false. returing NOT_DONE,");
                    }
                    status = AuthenticationStatus.NOT_DONE;
                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "No Credential object, redirecting");
                    }
                    status = AuthenticationStatus.SEND_CONTINUE;
                }
            } else {
                boolean newAuth = authParams.isNewAuthentication();
                status = handleFormLogin(cred, newAuth == true ? null : httpMessageContext.getResponse(), clientSubject, httpMessageContext);
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

    private AuthenticationStatus handleFormLogin(@Sensitive Credential credential, HttpServletResponse rsp, Subject clientSubject,
                                                 HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = utils.handleAuthenticate(getCDI(), JavaEESecConstants.DEFAULT_REALM, credential, clientSubject, httpMessageContext);
        int rspStatus;
        if (status == AuthenticationStatus.SUCCESS) {
            rspStatus = HttpServletResponse.SC_OK;
        } else {
            rspStatus = HttpServletResponse.SC_FORBIDDEN;
            // TODO: Audit invalid user or password
        }
        if (rsp != null) {
            rsp.setStatus(rspStatus);
        }
        return status;
    }

    protected CDI getCDI() {
        return CDI.current();
    }

}
