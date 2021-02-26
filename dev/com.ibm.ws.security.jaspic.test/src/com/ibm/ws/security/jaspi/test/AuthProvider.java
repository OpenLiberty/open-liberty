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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.MessagePolicy.ProtectionPolicy;
import javax.security.auth.message.MessagePolicy.Target;
import javax.security.auth.message.MessagePolicy.TargetPolicy;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.ClientAuthConfig;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * This JASPI authentication provider calls AuthModule to perform authentication
 * when validateRequest is called.
 */
public class AuthProvider implements AuthConfigProvider {

    private final Map<String, String> props;
    private final AuthConfigFactory factory;

    public AuthProvider(Map<String, String> props, AuthConfigFactory factory) {
        this.props = props;
        this.factory = factory;
    }

    @Override
    public ServerAuthConfig getServerAuthConfig(String layer,
                                                String appContext,
                                                CallbackHandler handler) throws AuthException, SecurityException {
        return new AuthConfig(appContext, handler, props);
    }

    @Override
    public ClientAuthConfig getClientAuthConfig(String layer,
                                                String appContext,
                                                CallbackHandler handler) throws AuthException, SecurityException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh() {}

    private static class AuthConfig implements ServerAuthConfig {

        private final CallbackHandler handler;
        private final Map<String, String> properties;
        private final String appContext;

        public AuthConfig(String appContext, CallbackHandler handler, Map<String, String> properties) {
            this.handler = handler;
            this.properties = properties != null ? new HashMap<String, String>(properties) : new HashMap<String, String>();
            this.appContext = appContext;
        }

        @Override
        public ServerAuthContext getAuthContext(String authContextID,
                                                Subject serviceSubject,
                                                Map properties) throws AuthException {
            HashMap<String, String> props = new HashMap<String, String>(this.properties);
            if (properties != null) {
                props.putAll(properties);
            }
            return new AuthContext(authContextID, serviceSubject, props, handler);
        }

        @Override
        public String getAppContext() {
            return appContext;
        }

        @Override
        public String getAuthContextID(MessageInfo messageInfo) throws IllegalArgumentException {
            if (!(messageInfo.getRequestMessage() instanceof HttpServletRequest || messageInfo.getResponseMessage() instanceof HttpServletResponse)) {
                throw new IllegalArgumentException();
            }
            if (messageInfo != null) {
                Object obj = messageInfo.getMap().get("javax.security.auth.message.MessagePolicy.isMandatory");
                if (Boolean.valueOf((String) obj)) {
                    return "JASPI_PROTECTED";
                } else {
                    return "JASPI_UNPROTECTED";
                }
            }
            return null;
        }

        @Override
        public String getMessageLayer() {
            return "HttpServlet";
        }

        @Override
        public boolean isProtected() {
            return true;
        }

        @Override
        public void refresh() {}

    }

    private static class AuthContext implements ServerAuthContext {

        private static Logger log = Logger.getLogger(AuthContext.class.getName());
        private final ServerAuthModule module;
        private final CallbackHandler handler;

        public AuthContext(String authContextID, Subject serviceSubject, Map properties, CallbackHandler handler) {
            boolean isMandatory = "JASPI_PROTECTED".equals(authContextID);
            log.info(authContextID + " isMandatory=" + isMandatory);
            this.handler = handler;
            module = new AuthModule();
            Map<String, String> props = new HashMap<String, String>();
            if (properties != null)
                props.putAll(properties);
            try {
                MessagePolicy requestPolicy = newMessagePolicy(isMandatory, newProtectionPolicy(ProtectionPolicy.AUTHENTICATE_SENDER));
                MessagePolicy responsePolicy = null;
                module.initialize(requestPolicy, responsePolicy, handler, props);
            } catch (AuthException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
            log.info("cleanSubject");
        }

        @Override
        public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
            log.info("secureResponse");
            AuthStatus status = module.secureResponse(messageInfo, serviceSubject);
            log.info("messageInfo=" + messageInfo + " status=" + status);
            return status;
        }

        @Override
        public AuthStatus validateRequest(MessageInfo messageInfo,
                                          Subject clientSubject,
                                          Subject serviceSubject) throws AuthException {
            log.info("validateRequest");
            AuthStatus status = module.validateRequest(messageInfo, clientSubject, serviceSubject);
            log.info("messageInfo=" + messageInfo + " status=" + status);
            return status;
        }

        private MessagePolicy newMessagePolicy(boolean isMandatory, ProtectionPolicy policy) {
            TargetPolicy[] policies = new TargetPolicy[] { new TargetPolicy((Target[]) null, policy) };
            return new MessagePolicy(policies, isMandatory);
        }

        private ProtectionPolicy newProtectionPolicy(final String id) {
            return new ProtectionPolicy() {

                @Override
                public String getID() {
                    return id;
                }
            };
        }
    }
}
