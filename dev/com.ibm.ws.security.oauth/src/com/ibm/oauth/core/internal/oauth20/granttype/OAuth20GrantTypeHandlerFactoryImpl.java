/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.oauth20.granttype;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.granttype.impl.OAuth20GrantTypeHandlerAppTokenAndPasswordImpl;
import com.ibm.oauth.core.internal.oauth20.granttype.impl.OAuth20GrantTypeHandlerClientCredentialsImpl;
import com.ibm.oauth.core.internal.oauth20.granttype.impl.OAuth20GrantTypeHandlerCodeImpl;
import com.ibm.oauth.core.internal.oauth20.granttype.impl.OAuth20GrantTypeHandlerRefreshImpl;
import com.ibm.oauth.core.internal.oauth20.granttype.impl.OAuth20GrantTypeHandlerResourceOwnerCredentialsImpl;
import com.ibm.ws.security.oauth20.plugins.JwtGrantTypeHandlerFactory;

public class OAuth20GrantTypeHandlerFactoryImpl implements OAuth20GrantTypeHandlerFactory {

    final static String CLASS = OAuth20GrantTypeHandlerFactoryImpl.class.getName();
    final static Logger _log = Logger.getLogger(CLASS);

    @Override
    public synchronized OAuth20GrantTypeHandler getHandler(String providerId, String grantType, OAuth20ConfigProvider config)
            throws OAuthException {
        String methodName = "getHandler";
        _log.entering(CLASS, methodName, new Object[] { grantType });
        OAuth20GrantTypeHandler result = null;
        try {
            if (grantType != null) {

                if (!OAuth20Constants.ALL_GRANT_TYPES_SET.contains(grantType)) {
                    throw new OAuth20InvalidGrantTypeException("security.oauth20.error.invalid.granttype", grantType);
                }
                if (grantType.equals(OAuth20Constants.GRANT_TYPE_APP_TOKEN) || grantType.equals(OAuth20Constants.GRANT_TYPE_APP_PASSWORD)) {
                    result = getCustomGTHandlerInstance(providerId, config, grantType);
                } else {
                    if (!config.isGrantTypeAllowed(grantType)) {
                        throw new OAuthConfigurationException("security.oauth.error.mismatch.granttype.exception",
                                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                                grantType, null);
                    }
                    if (grantType
                            .equals(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE)) {
                        result = new OAuth20GrantTypeHandlerCodeImpl();
                    } else if (grantType
                            .equals(OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS)) {
                        result = new OAuth20GrantTypeHandlerClientCredentialsImpl();
                    } else if (grantType
                            .equals(OAuth20Constants.GRANT_TYPE_PASSWORD)) {
                        result = new OAuth20GrantTypeHandlerResourceOwnerCredentialsImpl();
                    } else if (grantType
                            .equals(OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN)) {
                        result = new OAuth20GrantTypeHandlerRefreshImpl(config);
                    } else if (grantType
                            .equals(OAuth20Constants.GRANT_TYPE_JWT)) {
                        result = getJwtHandlerInstance(providerId, config);
                    }
                }     
            }
            if (result == null) {
                // User may have tried an implicit grant request
                throw new OAuth20InvalidGrantTypeException("security.oauth20.error.invalid.granttype", grantType);
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
        return result;
    }

    /**
     * Get back an instance of OAuth20GrantTypeHandler
     *
     * @return
     * @throws OAuth20Exception
     */
    public synchronized OAuth20GrantTypeHandler getJwtHandlerInstance(String providerId, OAuth20ConfigProvider config)
            throws OAuth20Exception {
        String methodName = "getJwtHandlerInstance";
        String handlerClassName = "com.ibm.ws.security.oauth20.jwt.GrantTypeCustomizedHandlerJwtImpl";
        OAuth20GrantTypeHandler instance = null;
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        JwtGrantTypeHandlerFactory jwtHandler = null;

        try {
            @SuppressWarnings("unchecked")
            Class<JwtGrantTypeHandlerFactory> handlerClass = (Class<JwtGrantTypeHandlerFactory>) Class.forName(handlerClassName);
            try {
                jwtHandler = handlerClass.newInstance();
                ;
                jwtHandler.setHandlerInfo(providerId, (com.ibm.ws.security.oauth20.api.OAuth20Provider) config);
                instance = jwtHandler.getHandlerInstance();
            } catch (Exception e) {
                if (finestLoggable) {
                    _log.logp(Level.FINEST, CLASS, methodName, "Get an unpexted exception", e);
                }
            }

        } catch (ClassNotFoundException e) {
            _log.logp(Level.FINEST, CLASS, methodName, "Do not find class:" + handlerClassName + " If the OP Server intends to handle " +
                    "urn:ietf:params:oauth:grant-type:jwt-bearer, please make sure your server has included openidConnectServer feature");
            if (finestLoggable) {
                _log.logp(Level.FINEST, CLASS, methodName, "Do not find class:" + handlerClassName + " Please make sure your server has included openidConnectServer feature");
            }
        }

        return instance;
    }

    public synchronized OAuth20GrantTypeHandler getCustomGTHandlerInstance(String providerId, OAuth20ConfigProvider config, String gt)
            throws OAuth20Exception {
        String methodName = "getCustomGTHandlerInstance";

        boolean finestLoggable = _log.isLoggable(Level.FINEST);

        return new OAuth20GrantTypeHandlerAppTokenAndPasswordImpl(gt, (com.ibm.ws.security.oauth20.api.OAuth20Provider) config);
    }
}
