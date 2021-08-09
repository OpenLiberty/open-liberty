/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.jaspi.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.MessagePolicy.ProtectionPolicy;
import javax.security.auth.message.MessagePolicy.TargetPolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/*
 * This JASPI authentication module performs basic and form auth when validateRequest is called,
 * depending on the request's authType.
 */
public class AuthModule_1 implements ServerAuthModule {

    private static Logger log = Logger.getLogger(AuthModule_1.class.getName());
    private static Class[] supportedMessageTypes = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
    private static final String IS_MANDATORY_POLICY = "javax.security.auth.message.MessagePolicy.isMandatory";
    private static final String JASPI_USER = "com.ibm.websphere.jaspi.user";
    private static final String JASPI_PASSWORD = "com.ibm.websphere.jaspi.password";
    private static final String JASPI_WEB_REQUEST = "com.ibm.websphere.jaspi.request";
    private MessagePolicy requestPolicy;
    private MessagePolicy responsePolicy;
    private CallbackHandler handler;
    private Map<String, String> options;

    private enum CBvalues {
        YES, NO, MANUAL
    };

    @Override
    public Class[] getSupportedMessageTypes() {
        return supportedMessageTypes;
    }

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, Map options) throws AuthException {
        this.requestPolicy = requestPolicy;
        this.responsePolicy = responsePolicy;
        this.handler = handler;
        this.options = new HashMap<String, String>();
        if (options != null) {
            this.options.putAll(options);
        }
        log.info("initialize " + AuthModule_1.class.getSimpleName() + " requestPolicy=" + requestPolicy + ", responsePolicy=" + responsePolicy + ", handler=" + handler
                 + ", options=" + this.options);
        if (requestPolicy != null && requestPolicy.getTargetPolicies() != null) {
            for (TargetPolicy target : requestPolicy.getTargetPolicies()) {
                ProtectionPolicy protectionPolicy = target.getProtectionPolicy();
                if (protectionPolicy != null) {
                    log.info("target request ProtectionPolicy=" + protectionPolicy.getID());
                }
            }
        }
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {}

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        HttpServletResponse rsp = (HttpServletResponse) messageInfo.getResponseMessage();
        try {
            rsp.getWriter().println("JASPI secureResponse called with auth provider=" + options.get("provider.name"));
        } catch (Exception e) {
            log.info(this.getClass().getName() + " failed to write to response object.");
        }

        return AuthStatus.SEND_SUCCESS;
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        if (requestPolicy == null || messageInfo == null) {
            return AuthStatus.SUCCESS;
        }

        Map<String, String> msgMap = messageInfo.getMap();
        log.info("MessageInfo Map: " + msgMap);
        boolean isAuthenticate = "authenticate".equalsIgnoreCase(msgMap.get(JASPI_WEB_REQUEST));
        boolean isLogin = "login".equalsIgnoreCase(msgMap.get(JASPI_WEB_REQUEST));
        AuthStatus status = AuthStatus.SEND_FAILURE;
        HttpServletRequest req = (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse rsp = (HttpServletResponse) messageInfo.getResponseMessage();

        String authType = req.getAuthType();
        log.info("AuthType: " + authType);
        String authHeader = req.getHeader("Authorization");
        log.info("Authorization=[" + authHeader + "]");

        // PasswordValidation, CallerPrincipal, GroupPrincipal
        String[] useCallbacks = { "YES", "YES", "YES" };
        //TODO: simplify cacheKey
        String cacheKey = "Jaspi:JASPIee5EAR:default_host" + req.getContextPath().substring(1);

        if (isLogin) {
            log.info("request is for method login()");
            String username = msgMap.get(JASPI_USER);
            String password = msgMap.get(JASPI_PASSWORD);
            status = handleUserPassword(username, password, rsp, msgMap, clientSubject, useCallbacks, cacheKey);

        } else if (isAuthenticate) {
            log.info("request is for method authenticate()");
            if (authHeader == null) {
                status = setChallengeAuthorizationHeader(rsp);
            } else {
                status = handleAuthorizationHeader(authHeader, rsp, msgMap, clientSubject, useCallbacks, cacheKey);
            }

        } else if (requestPolicy.isMandatory() || Boolean.valueOf(msgMap.get(IS_MANDATORY_POLICY))) {
            String queryString = req.getQueryString();
            if (queryString != null && queryString.startsWith("PVCB")) {
                String[] queryInfo = queryString.split("&");
                if (queryInfo.length == 3) {
                    for (int i = 0; i < queryInfo.length; i++)
                        useCallbacks[i] = queryInfo[i].substring(5);
                    log.info("Overriding Callback Settings:\n PasswordValidation-" + useCallbacks[0] + "\nCallerPrincipal" + useCallbacks[1] + "\nGroupPrincipal"
                             + useCallbacks[2]);
                }
            }

            if ("BASIC".equals(authType) || authType == null) {
                if (authHeader == null) {
                    status = setChallengeAuthorizationHeader(rsp);
                } else {
                    status = handleAuthorizationHeader(authHeader, rsp, msgMap, clientSubject, useCallbacks, cacheKey);
                }

            } else if ("FORM".equals(authType)) {
                log.info("requestURL=" + req.getRequestURL() + ", requestURI=" + req.getRequestURI());
                String username = req.getParameter("j_username");
                String password = req.getParameter("j_password");
                log.info("j_username=" + username);
                if (username != null && password != null) {
                    status = handleUserPassword(username, password, rsp, msgMap, clientSubject, useCallbacks, cacheKey);
                } else {
                    status = AuthStatus.SEND_CONTINUE;
                    rsp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
                }
            } else {
                throw new AuthException("Certificate Authentication is not supported by this module.");
            }

        } else {
            rsp.setStatus(HttpServletResponse.SC_OK);
            status = AuthStatus.SUCCESS;
        }
        //log.info("AuthStatus="+status+", HttpServletResponse status="+rsp.getStatus());

        try {
            rsp.getWriter().println("JASPI validateRequest called with auth provider=" + options.get("provider.name"));
        } catch (Exception e) {
            log.info(this.getClass().getName() + " failed to write to response object.");
        }

        return status;
    }

    private AuthStatus handleUserPassword(String user, String password, HttpServletResponse rsp, Map<String, String> msgMap, Subject clientSubject, String[] useCallbacks,
                                          String cacheKey) throws AuthException {

        int rspStatus = HttpServletResponse.SC_OK;
        log.info("Authenticating user=" + user);
        AuthStatus status = validateUserAndPassword(clientSubject, user, password, useCallbacks, cacheKey);
        if (status == AuthStatus.SUCCESS) {
            handleCallbacks(clientSubject, user, useCallbacks);
            msgMap.put("javax.servlet.http.authType", "JaspiBasicAuthExample");
        } else {
            rspStatus = HttpServletResponse.SC_FORBIDDEN;
            log.info("Invalid user or password");
        }
        rsp.setStatus(rspStatus);
        return status;
    }

    private AuthStatus handleAuthorizationHeader(String authHeader, HttpServletResponse rsp, Map<String, String> msgMap, Subject clientSubject, String[] useCallbacks,
                                                 String cacheKey) throws AuthException {

        AuthStatus status = AuthStatus.SEND_FAILURE;
        int rspStatus = HttpServletResponse.SC_FORBIDDEN;
        if (authHeader.startsWith("Basic ")) {
            String basicAuthHeader = decodeCookieString(authHeader.substring(6));
            String uid = getUserName(basicAuthHeader);
            String pw = getPassword(basicAuthHeader);
            log.info("user=" + uid);
            if (isAuthorizationHeaderValid(basicAuthHeader)) {
                status = validateUserAndPassword(clientSubject, uid, pw, useCallbacks, cacheKey);
                if (status == AuthStatus.SUCCESS) {
                    rspStatus = HttpServletResponse.SC_OK;
                    handleCallbacks(clientSubject, uid, useCallbacks);
                    msgMap.put("javax.servlet.http.authType", "JaspiBasicAuthExample");
                } else {
                    log.info("Invalid user or password");
                }
            } else {
                log.info("Both user and password must be non-null and non-empty.");
            }
        } else {
            log.info("Authorization header does not begin with \"Basic \"");
        }
        rsp.setStatus(rspStatus);
        return status;
    }

    private AuthStatus setChallengeAuthorizationHeader(HttpServletResponse rsp) {
        String realmName = options.get("realm.name");
        rsp.setHeader("WWW-Authenticate", "Basic realm=\"" + realmName + "\"");
        log.info("Challenge WWW-Authenticate header = Basic realm=\"" + realmName + "\"");
        rsp.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        return AuthStatus.SEND_CONTINUE;
    }

    private String getUserName(String basicAuthHeader) {
        if (isAuthorizationHeaderValid(basicAuthHeader)) {
            int index = basicAuthHeader.indexOf(':');
            String uid = basicAuthHeader.substring(0, index);
            return uid;
        } else {
            log.info(basicAuthHeader + "Authorization header is not valid: " + basicAuthHeader);
            return null;
        }
    }

    private String getPassword(String basicAuthHeader) {
        if (isAuthorizationHeaderValid(basicAuthHeader)) {
            int index = basicAuthHeader.indexOf(':');
            String pw = basicAuthHeader.substring(index + 1);
            return pw;
        } else {
            log.info(basicAuthHeader + "Authorization header is not valid: " + basicAuthHeader);
            return null;
        }
    }

    private boolean isAuthorizationHeaderValid(String basicAuthHeader) {
        int index = -1;
        boolean isNotValid = basicAuthHeader == null || basicAuthHeader.isEmpty() || (index = basicAuthHeader.indexOf(':')) <= 0 || index == basicAuthHeader.length() - 1;
        return !isNotValid;
    }

    private void handleCallbacks(Subject clientSubject, String userName, String[] useCallbacks) throws AuthException {
        Collection<Callback> cbs = new ArrayList<Callback>();
        switch (CBvalues.valueOf(useCallbacks[1])) {

            // skip caller principal handling
            case NO:
                break;

            // use JaspiCallbackHandler for CallerPrincipalCallback
            default:
                CallerPrincipalCallback cpcb = new CallerPrincipalCallback(clientSubject, userName);
                cbs.add(cpcb);
                break;
        }

        switch (CBvalues.valueOf(useCallbacks[2])) {

            // skip group principal handling
            case NO:
                break;

            // use JaspiCallbackHandler for GroupPrincipalCallback
            default:
                GroupPrincipalCallback gpcb = new GroupPrincipalCallback(clientSubject, new String[] { options.get("group.name") });
                cbs.add(gpcb);
                break;
        }

        if (cbs.size() > 0) {
            try {
                Object[] callbacks = cbs.toArray();
                if (callbacks instanceof Callback[])
                    handler.handle((Callback[]) callbacks);
            } catch (Exception e) {
                e.printStackTrace();
                throw new AuthException(e.toString());
            }
        }
    }

    private AuthStatus validateUserAndPassword(Subject clientSubject, String user, String password, String[] useCallbacks, String cacheKey) throws AuthException {
        AuthStatus status = AuthStatus.SEND_FAILURE;
        switch (CBvalues.valueOf(useCallbacks[0])) {

            // skip password validation
            case NO:
                status = AuthStatus.SUCCESS;
                break;

            // manually add user and password to HashTable in subject
            case MANUAL:
                status = AuthStatus.SUCCESS;
                manualAddUserAndPassword(clientSubject, user, password, cacheKey);
                break;

            // use JaspiCallbackHandler for PasswordValidationCallback
            default:
                log.info("validate password for user=" + user + " clientSubject=" + clientSubject);
                if (handler != null) {
                    PasswordValidationCallback pwcb = new PasswordValidationCallback(clientSubject, user, password.toCharArray());
                    try {
                        handler.handle(new Callback[] { pwcb });
                        boolean isValidPassword = pwcb.getResult();
                        log.info("isValidPassword? " + isValidPassword);
                        if (isValidPassword) {
                            status = AuthStatus.SUCCESS;
                        }
                    } catch (Exception e) {
                        throw new AuthException(e.toString());
                    }
                }
                break;
        }
        return status;
    }

    private void manualAddUserAndPassword(Subject clientSubject, String user, String password, String cacheKey) {
        log.info("Manually adding user " + user);

        Hashtable<String, Object> cred = new Hashtable<String, Object>();
        cred.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, cacheKey);
        cred.put(AttributeNameConstants.WSCREDENTIAL_USERID, user);
        cred.put(AttributeNameConstants.WSCREDENTIAL_PASSWORD, password);

        clientSubject.getPrivateCredentials().add(cred);
    }

    private String decodeCookieString(String cookieString) {
        try {
            return Base64Coder.base64Decode(cookieString);
        } catch (Exception e) {
            return null;
        }
    }
}
