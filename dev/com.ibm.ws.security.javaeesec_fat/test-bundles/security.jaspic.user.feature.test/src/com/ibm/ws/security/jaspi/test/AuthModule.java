/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaspi.test;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
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
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/*
 * This JASPI authentication module performs basic and form auth when validateRequest is called,
 * depending on the request's authType. 
 */
public class AuthModule implements ServerAuthModule {

    private static Logger log = Logger.getLogger(AuthModule.class.getName());
    private static Class[] supportedMessageTypes = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
    private static final String IS_MANDATORY_POLICY = "javax.security.auth.message.MessagePolicy.isMandatory";
    private static final String REGISTER_SESSION = "javax.servlet.http.registerSession";
    private static final String JASPI_USER = "com.ibm.websphere.jaspi.user";
    private static final String JASPI_PASSWORD = "com.ibm.websphere.jaspi.password";
    private static final String JASPI_WEB_REQUEST = "com.ibm.websphere.jaspi.request";
    private MessagePolicy requestPolicy;
    private CallbackHandler handler;
    private Map<String, String> options;
    private String cpcbType = null;

    private enum CBvalues {
        YES, NO, MANUAL
    };

    @Override
    public Class[] getSupportedMessageTypes() {
        return supportedMessageTypes;
    }

    class JASPIHttpServletRequestWrapper extends HttpServletRequestWrapper {
        public JASPIHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            if ("hasWrapper".equals(name)) {
                return "true";
            }
            return "false";
        }
    }

    class JASPIPrincipal implements Principal {
        private String name = null;

        public JASPIPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    class JASPIHttpServletResponseWrapper extends HttpServletResponseWrapper {

        public JASPIHttpServletResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public String getHeader(String name) {
            if ("hasWrapper".equals(name)) {
                return "true";
            }
            return "false";
        }
    }

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, Map options) throws AuthException {
        this.requestPolicy = requestPolicy;
        this.handler = handler;
        this.options = new HashMap<String, String>();
        if (options != null) {
            this.options.putAll(options);
        }
        log.info("initialize " + AuthModule.class.getSimpleName() + " requestPolicy=" + requestPolicy + ", responsePolicy=" + responsePolicy + ", handler=" + handler
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
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        log.info("cleanSubject");
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        log.log(Level.FINE, "enter secureResponse", new Object[] { messageInfo, serviceSubject });
        HttpServletResponse rsp = (HttpServletResponse) messageInfo.getResponseMessage();
        try {
            rsp.getWriter().println("JASPI secureResponse called with auth provider=" + options.get("provider.name"));
        } catch (Exception e) {
            log.info(this.getClass().getName() + " failed to write to response object.");
        }
        log.log(Level.FINE, "exit secureResponse");
        return AuthStatus.SEND_SUCCESS;
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        log.log(Level.FINE, "enter validateRequest", new Object[] { messageInfo, clientSubject });
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

        // userPrincipal will be set by the runtime if a previous request resulted in registerSession=true
        Principal userPrincipal = req.getUserPrincipal();
        if (userPrincipal != null) {
            log.info("userPrincipal.getName: " + userPrincipal.getName());
        } else {
            log.info("UserPrincipal is null ");
        }

        String authHeader = req.getHeader("Authorization");
        log.info("Authorization=[" + authHeader + "]");

        String methodName = req.getParameter("method");
        log.info("Request parameter: method=" + methodName);

        cpcbType = req.getParameter("cpcbType");
        log.info("Request parameter: cpcbType=" + cpcbType);

        // By default, use all callback -- PasswordValidation, CallerPrincipal, GroupPrincipal
        String[] useCallbacks = { "YES", "YES", "YES" };

        //Set cacheKey
        String cacheKey = "Jaspi:JASPIee5EAR:default_host" + req.getContextPath().substring(1);

        // If test method is registerSession, then set callback property for javax.servlet.http.registerSession=true
        if (methodName != null && methodName.equalsIgnoreCase("registerSession")) {
            log.info("Set registerSession=true so that provider will set javax.servlet.http.registerSession=true in msgMap");
            msgMap.put(REGISTER_SESSION, Boolean.TRUE.toString().toLowerCase());
        }

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

        } else if (methodName != null && methodName.equalsIgnoreCase("wrap")) {
            log.info("Wrap the request and response");
            // Wrap the request so the invoked servlet can invoke a method on the wrapper
            messageInfo.setRequestMessage(new JASPIHttpServletRequestWrapper((HttpServletRequest) messageInfo.getRequestMessage()));
            // Wrap the response so the invoked servlet can invoke a method on the wrapper
            messageInfo.setResponseMessage(new JASPIHttpServletResponseWrapper((HttpServletResponse) messageInfo.getResponseMessage()));
            status = AuthStatus.SUCCESS;

        } else if (methodName != null && methodName.equalsIgnoreCase("processRegisteredSession")) {
            if (userPrincipal != null) {
                log.info("If userPrincipal already set by runtime, then process callerPrincipal callback to establish subject and return AuthStatus.SUCCESS");
                useCallbacks[0] = "NO";
                useCallbacks[2] = "NO";
                handleCallbacks(clientSubject, userPrincipal.getName(), useCallbacks);
                status = AuthStatus.SUCCESS;
            }

        } else {
            String queryString = req.getQueryString();
            if (queryString != null && queryString.startsWith("PVCB")) {
                String[] queryInfo = queryString.split("&");
                if (queryInfo.length == 3) {
                    for (int i = 0; i < queryInfo.length; i++)
                        useCallbacks[i] = queryInfo[i].substring(5);
                    log.info("Overriding Callback Settings:\n PasswordValidation-" + useCallbacks[0] + "\nCallerPrincipal" + useCallbacks[1] + "\nGroupPrincipal" + useCallbacks[2]);
                }
            }

            if ("BASIC".equals(authType) || authType == null) {
                if (authHeader == null) {
                    // If isMandatory=false, the servlet is unprotected and the provider will not authenticate and will return SUCCESS
                    if (msgMap.get(IS_MANDATORY_POLICY).equalsIgnoreCase("FALSE")) {
                        log.info("BasicAuth request with isMandatory=false does not require JASPI authentication and returns success");
                        status = AuthStatus.SUCCESS;
                    }
                    else
                        // if isMandatory=true, this indicates a protected servlet which requires authentication, so must challenge if basic auth header is null
                        status = setChallengeAuthorizationHeader(rsp);
                } else {
                    status = handleAuthorizationHeader(authHeader, rsp, msgMap, clientSubject, useCallbacks, cacheKey);
                }

            } else if ("FORM".equals(authType)) {
                log.info("requestURL=" + req.getRequestURL() + ", requestURI=" + req.getRequestURI());
                String username = req.getParameter("j_username");
                String password = req.getParameter("j_password");
                log.info("j_username=" + username);

                //Added description for form submit - contains callback info for 2nd validateRequest call
                String description = req.getParameter("j_description");
                if (description != null && description.startsWith("PVCB")) {
                    log.info("j_description=" + username);
                    String[] callbackInfo = description.split("&");
                    if (callbackInfo.length == 3) {
                        for (int i = 0; i < callbackInfo.length; i++)
                            useCallbacks[i] = callbackInfo[i].substring(5);
                    }
                }

                if (username != null && password != null) {
                    status = handleUserPassword(username, password, rsp, msgMap, clientSubject, useCallbacks, cacheKey);
                } else {
                    // Process RequestDispatcher forward or include if specified by test request parameter
                    if ((methodName != null && (methodName.equalsIgnoreCase("forward") || methodName.equalsIgnoreCase("include"))))
                    {
                        log.info("Acquiring a RequestDispatcher.");
                        RequestDispatcher rd = req.getRequestDispatcher("loginJaspi.jsp");
                        {
                            try {
                                if (methodName.equalsIgnoreCase("include")) {
                                    log.info("RequestDispatcher is including a loginJaspi.jsp");
                                    rd.include(req, rsp);
                                } else {
                                    log.info("RequestDispatcher is forwarding to loginJaspi.jsp");
                                    rd.forward(req, rsp);
                                }
                            } catch (ServletException e) {
                                log.info("Exception caught including loginJaspi.jsp " + e);
                            } catch (IOException e) {
                                log.info("Exception caught including loginJaspi.jsp " + e);
                            }
                        }
                    }

                    status = AuthStatus.SEND_CONTINUE;
                    rsp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
                }
            } else {
                throw new AuthException("Certificate Authentication is not supported by this module.");
            }
        }

        try {
            rsp.getWriter().println("JASPI validateRequest called with auth provider=" + options.get("provider.name"));
        } catch (Exception e) {
            log.info(this.getClass().getName() + " failed to write to response object.");
        }

        log.log(Level.FINE, "exit validateRequest", status);
        return status;

    }

    private AuthStatus handleUserPassword(String user, String password, HttpServletResponse rsp, Map<String, String> msgMap, Subject clientSubject, String[] useCallbacks,
                                          String cacheKey)
                    throws AuthException {
        log.log(Level.FINE, "enter handleUserPassword", new Object[] { user, password, msgMap, clientSubject, useCallbacks, cacheKey });
        int rspStatus = HttpServletResponse.SC_OK;
        log.info("Authenticating user=" + user);
        AuthStatus status = validateUserAndPassword(clientSubject, user, password, useCallbacks, cacheKey);
        if (status == AuthStatus.SUCCESS) {
            handleCallbacks(clientSubject, user, useCallbacks);
            msgMap.put("javax.servlet.http.authType", "JASPI_AUTH");

        } else {
            rspStatus = HttpServletResponse.SC_FORBIDDEN;
            log.info("Invalid user or password");

        }
        rsp.setStatus(rspStatus);
        log.log(Level.FINE, "exit handleUserPassword", status);
        return status;
    }

    private AuthStatus handleAuthorizationHeader(String authHeader, HttpServletResponse rsp, Map<String, String> msgMap, Subject clientSubject, String[] useCallbacks,
                                                 String cacheKey)
                    throws AuthException {
        log.log(Level.FINE, "enter handleAuthorizationHeader", new Object[] { authHeader, msgMap, clientSubject, useCallbacks, cacheKey });
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
                    msgMap.put("javax.servlet.http.authType", "JASPI_AUTH");
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
        log.log(Level.FINE, "exit handleAuthorizationHeader", status);
        return status;
    }

    private AuthStatus setChallengeAuthorizationHeader(HttpServletResponse rsp) {
        log.log(Level.FINE, "enter setChallengeAuthorizationHeader");
        String realmName = options.get("realm.name");
        rsp.setHeader("WWW-Authenticate", "Basic realm=\"" + realmName + "\"");
        log.info("Challenge WWW-Authenticate header = Basic realm=\"" + realmName + "\"");
        rsp.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        log.log(Level.FINE, "exit setChallengeAuthorizationHeader");
        return AuthStatus.SEND_CONTINUE;
    }

    private String getUserName(String basicAuthHeader) {
        log.log(Level.FINE, "enter getUserName", basicAuthHeader);
        String uid = null;
        if (isAuthorizationHeaderValid(basicAuthHeader)) {
            int index = basicAuthHeader.indexOf(':');
            uid = basicAuthHeader.substring(0, index);
        } else {
            log.info(basicAuthHeader + "Authorization header is not valid: " + basicAuthHeader);
        }
        log.log(Level.FINE, "exit getUserName", uid);
        return uid;
    }

    private String getPassword(String basicAuthHeader) {
        log.log(Level.FINE, "enter getPassword", basicAuthHeader);
        String pw = null;
        if (isAuthorizationHeaderValid(basicAuthHeader)) {
            int index = basicAuthHeader.indexOf(':');
            pw = basicAuthHeader.substring(index + 1);
        } else {
            log.info(basicAuthHeader + "Authorization header is not valid: " + basicAuthHeader);
        }
        log.log(Level.FINE, "exit getPassword", pw);
        return pw;
    }

    private boolean isAuthorizationHeaderValid(String basicAuthHeader) {
        log.log(Level.FINE, "enter isAuthorizationHeaderValid", basicAuthHeader);
        int index = -1;
        boolean isNotValid = basicAuthHeader == null || basicAuthHeader.isEmpty() || (index = basicAuthHeader.indexOf(':')) <= 0 || index == basicAuthHeader.length() - 1;
        log.log(Level.FINE, "exit isAuthorizationHeaderValid", !isNotValid);
        return !isNotValid;
    }

    private void handleCallbacks(Subject clientSubject, String userName, String[] useCallbacks) throws AuthException {
        log.log(Level.FINE, "enter handleCallbacks", new Object[] { clientSubject, userName, useCallbacks });
        log.log(Level.FINE, "handleCallbacks cpcbType: " + cpcbType);
        Callback[] callbacks = new Callback[2];
        int index = 0;
        // caller principal
        switch (CBvalues.valueOf(useCallbacks[1])) {
            case NO:
                break; // skip
            default:
                CallerPrincipalCallback cpcb;
                if (cpcbType != null && cpcbType.equals("JASPI_PRINCIPAL"))
                    cpcb = new CallerPrincipalCallback(clientSubject, new JASPIPrincipal(userName));
                else
                    cpcb = new CallerPrincipalCallback(clientSubject, userName);
                callbacks[index] = cpcb;
                index++;
                log.log(Level.FINE, "added callback", cpcb);
                break;
        }
        // group principal
        switch (CBvalues.valueOf(useCallbacks[2])) {
            case NO:
                break; // skip
            default:
                GroupPrincipalCallback gpcb = new GroupPrincipalCallback(clientSubject, new String[] { options.get("group.name") });
                callbacks[index] = gpcb;
                index++;
                log.log(Level.FINE, "added callback", gpcb);
                break;
        }
        if (index > 0) {
            Callback[] cbs = new Callback[index];
            for (int i = 0; i < index; i++) {
                cbs[i] = callbacks[i];
            }
            try {
                log.log(Level.FINE, "handling callbacks: ", cbs);
                handler.handle(cbs);
            } catch (Exception e) {
                e.printStackTrace();
                throw new AuthException(e.toString());
            }
        }
        log.log(Level.FINE, "exit handleCallbacks");
    }

    private AuthStatus validateUserAndPassword(Subject clientSubject, String user, String password, String[] useCallbacks, String cacheKey) throws AuthException {
        log.log(Level.FINE, "enter validateUserAndPassword", new Object[] { user, password, clientSubject, useCallbacks, cacheKey });
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
        log.log(Level.FINE, "exit validateUserAndPassword", status);
        return status;
    }

    private void manualAddUserAndPassword(Subject clientSubject, String user, String password, String cacheKey) {
        log.log(Level.FINE, "enter manualAddUserAndPassword", new Object[] { user, password, clientSubject, cacheKey });
        Hashtable<String, Object> cred = new Hashtable<String, Object>();
        cred.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, cacheKey);
        cred.put(AttributeNameConstants.WSCREDENTIAL_USERID, user);
        cred.put(AttributeNameConstants.WSCREDENTIAL_PASSWORD, password);
        clientSubject.getPrivateCredentials().add(cred);
    }

    private String decodeCookieString(String cookieString)
    {
        try
        {
            return Base64Coder.base64Decode(cookieString);
        } catch (Exception e)
        {
            return null;
        }
    }

}
