/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.server.plugins;

import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.responsetype.impl.OAuth20ResponseTypeHandlerCodeImpl;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

public class OIDCResponseTypeHandlerCodeImpl extends OAuth20ResponseTypeHandlerCodeImpl {

    private static final String CLASS = OIDCResponseTypeHandlerCodeImpl.class.getName();
    private static Logger log = Logger.getLogger(CLASS);

    @Override
    public List<OAuth20Token> buildTokensResponseType(AttributeList attributeList, OAuth20TokenFactory tokenFactory, String redirectUri) {
        String methodName = "buildTokensResponseType";
        log.entering(CLASS, methodName);

        List<OAuth20Token> tokens = super.buildTokensResponseType(attributeList, tokenFactory, redirectUri);
        
        OAuth20Token code = tokens.get(0);

        OidcServerConfig oidcServerConfig = OIDCProvidersConfig.getOidcServerConfigForOAuth20Provider(code.getComponentId());
        if (hasThirdPartyClaims(oidcServerConfig) || hasMediatorSPI()) {
            OAuth20TokenCache tokenCache = tokenFactory.getOAuth20ComponentInternal().getTokenCache();
            cacheThirdPartyIDToken(code, tokenCache);
        }

        log.exiting(CLASS, methodName);
        return tokens;
    }

    private boolean hasThirdPartyClaims(OidcServerConfig oidcServerConfig) {
        return oidcServerConfig != null && !oidcServerConfig.getThirdPartyIDTokenClaims().isEmpty();
    }

    private boolean hasMediatorSPI() {
        return ConfigUtils.getIdTokenMediatorService().size() > 0;
    }

    private void cacheThirdPartyIDToken(OAuth20Token code, OAuth20TokenCache tokenCache) {
        SubjectHelper subjectHelper = new SubjectHelper();
        Hashtable<String, ?> hashtableFromRunAsSubject = subjectHelper.getHashtableFromRunAsSubject();
        if (hashtableFromRunAsSubject != null) {
            String thirdPartyIdTokenId = OAuth20Constants.THIRD_PARTY_ID_TOKEN_PREFIX + code.getTokenString();
            String thirdPartyIdTokenString = (String) hashtableFromRunAsSubject.get(OAuth20Constants.ID_TOKEN);
            if (thirdPartyIdTokenString != null) {
                OAuth20Token tokenCacheEntry = new IDTokenImpl(
                        thirdPartyIdTokenId,
                        thirdPartyIdTokenString,
                        code.getComponentId(),
                        code.getClientId(),
                        code.getUsername(),
                        code.getRedirectUri(),
                        code.getStateId(),
                        code.getScope(),
                        code.getLifetimeSeconds(),
                        null,
                        OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);

                // save third-party token in token cache to pick up when token endpoint is called
                tokenCache.add(tokenCacheEntry.getId(), tokenCacheEntry, tokenCacheEntry.getLifetimeSeconds());
            }
        }
    }

}
