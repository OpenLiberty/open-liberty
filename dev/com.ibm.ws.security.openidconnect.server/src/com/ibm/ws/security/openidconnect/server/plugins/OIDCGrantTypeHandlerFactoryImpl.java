/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.plugins;

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.osgi.service.component.ComponentContext;

import com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandler;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandlerFactory;
import com.ibm.oauth.core.internal.oauth20.granttype.impl.OAuth20GrantTypeHandlerAppTokenAndPasswordImpl;
import com.ibm.oauth.core.internal.oauth20.granttype.impl.OAuth20GrantTypeHandlerClientCredentialsImpl;
import com.ibm.oauth.core.internal.oauth20.granttype.impl.OAuth20GrantTypeHandlerResourceOwnerCredentialsImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.plugins.JwtGrantTypeHandlerFactory;
import com.ibm.ws.security.openidconnect.server.internal.Utils;

public class OIDCGrantTypeHandlerFactoryImpl implements OAuth20GrantTypeHandlerFactory {

    private static final TraceComponent tc = Tr.register(OIDCGrantTypeHandlerFactoryImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        // do nothing for now
    }

    protected void deactivate(ComponentContext cc, Map<String, Object> properties) {
        // do nothing for now
    }

    protected void modified(ComponentContext cc, Map<String, Object> properties) {
        // do nothing for now
    }

    public String toStringHelper(Set<String> s) {
        StringBuilder sb = new StringBuilder("{");
        String sep = "";
        for (String str : s) {
            sb.append(sep).append(str);
            sep = ",";
        }
        return sb.append("}").toString();
    }

    @Override
    public synchronized OAuth20GrantTypeHandler getHandler(String providerId, String grantType, OAuth20ConfigProvider config) throws OAuthException {
        
        OAuth20GrantTypeHandler result = null;
        if (grantType != null) {
            if (!OAuth20Constants.ALL_GRANT_TYPES_SET.contains(grantType)) {
                String validGrantTypesAsString = toStringHelper(OAuth20Constants.ALL_GRANT_TYPES_SET);
                Tr.error(tc, "OIDC_SERVER_INVALID_GRANT_TYPE_ERR", new Object[] { grantType, validGrantTypesAsString });
                throw new OAuth20InvalidGrantTypeException("security.oauth20.error.invalid.granttype", grantType);
            }
            if (grantType.equals(OAuth20Constants.GRANT_TYPE_APP_TOKEN) || grantType.equals(OAuth20Constants.GRANT_TYPE_APP_PASSWORD)) {
                        result = getCustomGTHandlerInstance(providerId, config, grantType);
            } else {
                if (!config.isGrantTypeAllowed(grantType)) {  
                    String allowedGrantTypesAsString = "";
                    if (config instanceof OAuth20Provider) {
                        allowedGrantTypesAsString = Utils.toString(((OAuth20Provider) config).getGrantTypesAllowed()); //TODO
                    }
                    Tr.error(tc, "OIDC_SERVER_GRANT_TYPE_NOT_ALLOWED_ERR", new Object[] { grantType, allowedGrantTypesAsString });
                    throw new OAuthConfigurationException(OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED, grantType, null);
                }
    
                if (grantType.equals(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE)) {
                    result = new OIDCGrantTypeHandlerCodeImpl();
                } else if (grantType.equals(OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS)) {
                    result = new OAuth20GrantTypeHandlerClientCredentialsImpl();
                } else if (grantType.equals(OAuth20Constants.GRANT_TYPE_PASSWORD)) {
                    result = new OAuth20GrantTypeHandlerResourceOwnerCredentialsImpl();
                } else if (grantType.equals(OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN)) {
                    result = new OIDCGrantTypeHandlerRefreshImpl();
                } else if (grantType.equals(OAuth20Constants.GRANT_TYPE_JWT)) {
                    result = getJwtHandlerInstance(providerId, config);
                }
            }    
        }
        if (result == null) {
            // User may have tried an implicit grant request
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Cannot get handler to process the grant type : ", grantType);
            }
            throw new OAuth20InvalidGrantTypeException("security.oauth20.error.invalid.granttype", grantType);
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
        String handlerClassName = "com.ibm.ws.security.oauth20.jwt.GrantTypeCustomizedHandlerJwtImpl";
        OAuth20GrantTypeHandler instance = null;
        JwtGrantTypeHandlerFactory jwtHandler = null;

        try {
            @SuppressWarnings("unchecked")
            Class<JwtGrantTypeHandlerFactory> handlerClass = (Class<JwtGrantTypeHandlerFactory>) Class.forName(handlerClassName);
            try {
                jwtHandler = (JwtGrantTypeHandlerFactory) handlerClass.newInstance();;
                jwtHandler.setHandlerInfo(providerId, (com.ibm.ws.security.oauth20.api.OAuth20Provider) config);
                instance = jwtHandler.getHandlerInstance();
            } catch (Exception e) {
                Tr.error(tc, "JWT_UNEXPECTED_EXCEPTION_ERR", e.toString());
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Get an unexpected exception ", e);
                }
            }

        } catch (ClassNotFoundException e) {
            // com.ibm.ws.security.oauth20.jwt.GrantTypeCustomizedHandlerJwtImpl is always included in the Oidc feature
            Tr.error(tc, "JWT_UNEXPECTED_EXCEPTION_ERR", e.toString());
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Get an unexpected exception ", e);
            }
        }

        return instance;
    }
    
    public synchronized OAuth20GrantTypeHandler getCustomGTHandlerInstance(String providerId, OAuth20ConfigProvider config, String gt)
                    throws OAuth20Exception {
                String methodName = "getCustomGTHandlerInstance";
                return new OAuth20GrantTypeHandlerAppTokenAndPasswordImpl(gt, (com.ibm.ws.security.oauth20.api.OAuth20Provider) config);
            }

}
