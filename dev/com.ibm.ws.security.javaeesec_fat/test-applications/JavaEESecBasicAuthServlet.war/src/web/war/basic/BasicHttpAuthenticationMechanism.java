/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.basic;

import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.CallerPrincipal;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Default
@ApplicationScoped
public class BasicHttpAuthenticationMechanism implements HttpAuthenticationMechanism {
    private final String realmName = "App BAMech";
    private static Logger log = Logger.getLogger(BasicHttpAuthenticationMechanism.class.getName());

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        log.info("validateRequest");
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        Subject clientSubject = httpMessageContext.getClientSubject();
        @SuppressWarnings("unchecked")
        Map<String, String> msgMap = httpMessageContext.getMessageInfo().getMap();
        CallbackHandler handler = httpMessageContext.getHandler();
        HttpServletRequest req = httpMessageContext.getRequest();
        HttpServletResponse rsp = httpMessageContext.getResponse();
        String authHeader = req.getHeader("Authorization");

        if (httpMessageContext.isAuthenticationRequest()) {
            if (authHeader == null) {
                status = setChallengeAuthorizationHeader(rsp);
            } else {
                status = handleAuthorizationHeader(authHeader, rsp, msgMap, clientSubject, handler);
            }
        } else {
            if (authHeader == null) {
                if (httpMessageContext.isProtected() == false) {
                    log.info("Both isAuthenticationRequest and isProtected returns false. returing NOT_DONE,");
                    status = AuthenticationStatus.NOT_DONE;
                } else {
                    status = setChallengeAuthorizationHeader(rsp);
                }
            } else {
                status = handleAuthorizationHeader(authHeader, rsp, msgMap, clientSubject, handler);
            }
        }

        return status;
    }

    @Override
    public AuthenticationStatus secureResponse(HttpServletRequest request,
                                               HttpServletResponse response,
                                               HttpMessageContext httpMessageContext) throws AuthenticationException {
        log.info("secureResponse");
        return AuthenticationStatus.SUCCESS;
    }

    @Override
    public void cleanSubject(HttpServletRequest request,
                             HttpServletResponse response,
                             HttpMessageContext httpMessageContext) {

    }

    private AuthenticationStatus setChallengeAuthorizationHeader(HttpServletResponse rsp) {
        rsp.setHeader("WWW-Authenticate", "Basic realm=\"" + realmName + "\"");
        rsp.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        return AuthenticationStatus.SEND_CONTINUE;
    }

    private AuthenticationStatus handleAuthorizationHeader(String authHeader, HttpServletResponse rsp, Map<String, String> msgMap, Subject clientSubject,
                                                           CallbackHandler handler) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        int rspStatus = HttpServletResponse.SC_FORBIDDEN;
        if (authHeader.startsWith("Basic ")) {
            String encodedHeader = authHeader.substring(6);
            String basicAuthHeader = decodeCookieString(encodedHeader);

            if (isAuthorizationHeaderValid(basicAuthHeader)) { // BasicAuthenticationCredential.isValid does not work
                BasicAuthenticationCredential basicAuthCredential = new BasicAuthenticationCredential(encodedHeader);
                CredentialValidationResult result = validateUserAndPassword(basicAuthCredential);

                if (result.getStatus() == CredentialValidationResult.Status.VALID) {
                    handleCallbacks(clientSubject, result.getCallerPrincipal(), getGroups(result), handler);
                    msgMap.put("javax.servlet.http.authType", "JASPI_AUTH");
                    rspStatus = HttpServletResponse.SC_OK;
                    status = AuthenticationStatus.SUCCESS;
                } else if (result.getStatus() == CredentialValidationResult.Status.NOT_VALIDATED) {
                    // TODO: error message. no identitystore.
                    status = AuthenticationStatus.NOT_DONE;
                } else {
                    // TODO: Audit invalid user or password
                }
            } else {
                // TODO: Determine if serviceability message is needed
            }
        } else {
            // TODO: Determine if serviceability message is needed
        }
        rsp.setStatus(rspStatus);
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

    private CredentialValidationResult validateUserAndPassword(BasicAuthenticationCredential credential) {
        CredentialValidationResult result = CredentialValidationResult.NOT_VALIDATED_RESULT;
        IdentityStoreHandler identityStoreHandler = getIdentityStoreHandler();
        if (identityStoreHandler != null) {
            result = identityStoreHandler.validate(credential);
        }
        return result;
    }

    private IdentityStoreHandler getIdentityStoreHandler() {
        IdentityStoreHandler identityStoreHandler = null;
        Instance<IdentityStoreHandler> storeHandlerInstance = CDI.current().select(IdentityStoreHandler.class);
        if (storeHandlerInstance.isUnsatisfied() == false && storeHandlerInstance.isAmbiguous() == false) {
            identityStoreHandler = storeHandlerInstance.get();
        }
        return identityStoreHandler;
    }

    private void handleCallbacks(Subject clientSubject, CallerPrincipal principal, String[] groups, CallbackHandler handler) throws AuthenticationException {
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new CallerPrincipalCallback(clientSubject, principal);
        callbacks[1] = new GroupPrincipalCallback(clientSubject, groups);

        try {
            handler.handle(callbacks);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AuthenticationException(e.toString());
        }
    }

    protected String[] getGroups(CredentialValidationResult result) {
        String[] groups = null;
        Set<String> groupSet = result.getCallerGroups();
        if (groupSet != null && !groupSet.isEmpty()) {
            groups = groupSet.toArray(new String[groupSet.size()]);
        }
        return groups;
    }

}
