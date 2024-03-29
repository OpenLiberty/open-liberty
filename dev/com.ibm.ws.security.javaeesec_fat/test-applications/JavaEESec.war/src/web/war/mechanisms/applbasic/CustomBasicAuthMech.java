/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.mechanisms.applbasic;

import java.util.Base64;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ApplicationScoped
public class CustomBasicAuthMech implements HttpAuthenticationMechanism {

    protected static String sourceClass = CustomBasicAuthMech.class.getName();
    private final Logger logger = Logger.getLogger(sourceClass);

    private final String realmName = "CustomBasicRealm";

    @Inject
    private IdentityStoreHandler identityStoreHandler;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        logger.entering(sourceClass, "validateRequest", new Object[] { request, response, httpMessageContext });
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        Subject clientSubject = httpMessageContext.getClientSubject();
        String authHeader = httpMessageContext.getRequest().getHeader("Authorization");

        if (httpMessageContext.isAuthenticationRequest()) {
            AuthenticationParameters authParams = httpMessageContext.getAuthParameters();

            if (authParams != null) {
                Credential credential = authParams.getCredential();
                status = validateWithIdentityStore(clientSubject, credential, identityStoreHandler, httpMessageContext);
                if (status == AuthenticationStatus.SUCCESS) {
                    httpMessageContext.getMessageInfo().getMap().put("javax.servlet.http.authType", "SERVLET10_AUTH_MECH");
                }
            } else {
                if (authHeader == null) {
                    status = setChallengeAuthorizationHeader(httpMessageContext.getResponse());
                } else {
                    status = handleAuthorizationHeader(authHeader, clientSubject, httpMessageContext);
                }
            }
        } else {
            if (authHeader == null) {
                if (httpMessageContext.isProtected() == false) {
                    status = AuthenticationStatus.NOT_DONE;
                } else {
                    status = setChallengeAuthorizationHeader(httpMessageContext.getResponse());
                }
            } else {
                status = handleAuthorizationHeader(authHeader, clientSubject, httpMessageContext);
            }
        }

        logger.exiting(sourceClass, "validateRequest", status);
        return status;
    }

    private AuthenticationStatus setChallengeAuthorizationHeader(HttpServletResponse rsp) {
        rsp.setHeader("WWW-Authenticate", "Basic realm=\"" + realmName + "\"");
        rsp.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        return AuthenticationStatus.SEND_CONTINUE;
    }

    @SuppressWarnings("unchecked")
    private AuthenticationStatus handleAuthorizationHeader(String authHeader, Subject clientSubject, HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        if (authHeader.startsWith("Basic ")) {
            String encodedHeader = authHeader.substring(6);
            String basicAuthHeader = decodeCookieString(encodedHeader);

            if (isAuthorizationHeaderValid(basicAuthHeader)) { // BasicAuthenticationCredential.isValid does not work
                BasicAuthenticationCredential basicAuthCredential = new BasicAuthenticationCredential(encodedHeader);
                status = validateWithIdentityStore(clientSubject, basicAuthCredential, identityStoreHandler, httpMessageContext);
                if (status == AuthenticationStatus.SUCCESS) {
                    httpMessageContext.getMessageInfo().getMap().put("javax.servlet.http.authType", "SERVLET10_AUTH_MECH");
                }
            } else {
                logger.info("Basic Auth header is not valid.");
            }
        }
        return status;
    }

    private String decodeCookieString(String cookieString) {
        try {
            return new String(Base64.getDecoder().decode(cookieString));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isAuthorizationHeaderValid(String basicAuthHeader) {
        int index = -1;
        boolean isNotValid = basicAuthHeader == null || basicAuthHeader.isEmpty() || (index = basicAuthHeader.indexOf(':')) <= 0 || index == basicAuthHeader.length() - 1;
        return !isNotValid;
    }

    private AuthenticationStatus validateWithIdentityStore(Subject clientSubject, Credential credential, IdentityStoreHandler identityStoreHandler,
                                                           HttpMessageContext httpMessageContext) {
        logger.entering(sourceClass, "validateWithIdentityStore", new Object[] { clientSubject, credential, httpMessageContext });
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        CredentialValidationResult result = identityStoreHandler.validate(credential);
        if (result.getStatus() == CredentialValidationResult.Status.VALID) {
            status = httpMessageContext.notifyContainerAboutLogin(result);
        } else if (result.getStatus() == CredentialValidationResult.Status.NOT_VALIDATED) {
            status = AuthenticationStatus.NOT_DONE;
        }

        logger.exiting(sourceClass, "validateWithIdentityStore", status);
        return status;
    }
}
