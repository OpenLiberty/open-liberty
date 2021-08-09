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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentInternal;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandler;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.BaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

public class IDTokenFactory extends OAuth20TokenFactory {

    private static final String SHARED_KEY = "sharedKey";
    private static final String CLASS = IDTokenFactory.class.getName();
    private static Logger log = Logger.getLogger(CLASS);
    private OAuth20ComponentInternal component;

    public IDTokenFactory(OAuth20ComponentInternal component) {
        super(component);
        this.component = component;
    }

    /**
     * Create an OAuth20 ID token if scope openid is specified
     * 
     * @param tokenMap
     *            A Map <String, String[]> containing the following parameters:
     *            <ul>
     *            <li>CLIENT_ID - client id from request
     *            <li>USERNAME - username that authorized the grant
     *            <li>REDIRECT_URI - redirect uri from request
     *            <li>STATE_ID - state id, if none is present, it is the
     *            responsibility of OAuth20TokenTypeHandler to generate one.
     *            <li>SCOPE - scope from the grant being used
     *            <li>LIFETIME - remaining seconds to the max authorization
     *            grant lifetime (applicable to authorization code and resource
     *            owner password creds only)
     *            </ul>
     *            As well as any additional parameters that should be passed to
     *            the configured OAuth20TokenTypeHandler implementation
     * @return
     */
    public OAuth20Token createIDToken(Map<String, String[]> tokenMap) {
        String methodName = "createIDToken";
        log.entering(CLASS, methodName);
        OAuth20Token token = null;
        boolean finestLoggable = log.isLoggable(Level.FINEST);

        try {
            Map<String, String[]> idTokenMap = new HashMap<String, String[]>();
            idTokenMap.putAll(tokenMap);

            // Check if "openid" is in the scopes otherwise return null
            String[] scopes = tokenMap.get(OAuth20Constants.SCOPE);
            boolean issueIDToken = false;
            for (String scope : scopes) {
                if (OIDCConstants.SCOPE_OPENID.equals(scope)) {
                    issueIDToken = true;
                }
            }
            if (!issueIDToken) {
                return (OAuth20Token) null;
            }
            String componentId = component.getParentComponentInstance().getInstanceId();
            idTokenMap.put(OAuth20Constants.COMPONENTID, new String[] { componentId });

            OAuth20ConfigProvider config = component.get20Configuration();
            int lifetime = config.getTokenLifetimeSeconds();

            String lifeStr;
            if (idTokenMap.containsKey(OAuth20Constants.LIFETIME)) {
                int remainingLifetime = Integer.parseInt(OAuth20Util.getValueFromMap(OAuth20Constants.LIFETIME, idTokenMap));

                if (remainingLifetime < lifetime) {
                    lifetime = remainingLifetime;
                }
            }
            lifeStr = Integer.toString(lifetime);

            if (finestLoggable) {
                log.logp(Level.FINEST, CLASS, methodName, "Creating id token with remaining lifetime: " + lifeStr + " seconds");
            }

            idTokenMap.put(OAuth20Constants.LIFETIME, new String[] { (lifeStr) });

            int length = config.getAccessTokenLength();
            String lengthStr = Integer.toString(length);
            idTokenMap.put(OAuth20Constants.LENGTH, new String[] { lengthStr });

            OAuth20TokenTypeHandler handler = null;
            try {
                handler = OIDCTokenTypeHandlerFactory.getIDTokenHandler(component);
                if (handler != null) {
                    String clientId = OAuth20Util.getValueFromMap(OAuth20Constants.CLIENT_ID, tokenMap);
                    OidcOAuth20ClientProvider clientProvider = config.getClientProvider();
                    OidcBaseClient oidcClient = clientProvider.get(clientId);
                    if (oidcClient instanceof BaseClient) {
                        BaseClient baseClient = (BaseClient) oidcClient;
                        String sharedKey = baseClient.getClientSecret();
                        idTokenMap.put(SHARED_KEY, new String[] { sharedKey });
                    }
                    token = handler.createToken(idTokenMap);
                    if (token != null) {
                        OidcServerConfig oidcServerConfig = OIDCProvidersConfig.getOidcServerConfigForOAuth20Provider(componentId);
                        if (oidcServerConfig != null && oidcServerConfig.cacheIDToken()) {
                            super.persistToken(token);
                        }
                    }
//                    if (token != null && token.isPersistent()) {
//                        //((OAuth20TokenImpl)token).setAccessTokenKey(OAuth20Util.getValueFromMap(OAuth20Constants.ACCESS_TOKEN, idTokenMap));
//                        super.persistToken(token);
//                    }
                    idTokenMap.remove(SHARED_KEY);
                }
            } catch (OAuthConfigurationException e) {
                // shouldn't happen, but if it does, log the exception
                log.throwing(CLASS, methodName, e);
            } catch (OidcServerException e) {
                log.throwing(CLASS, methodName, e);
            }

        } finally {
            log.exiting(CLASS, methodName);
        }

        return token;
    }

}
