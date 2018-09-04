/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal;

import java.util.Arrays;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;

/**
 * Extends from OidcLogin so we can have a different metatype element with all the defaults set for Google.
 *
 * This class provide two service:
 * . One is for the googleConfig which extends from the generic OAuth2LoginConfig
 * . The other is for JwtConsumerConfig. This make googleLogin does not need to define an additional jJwtConsumerConfig
 * .. So, we can reuse the jwksUri and sslRef defined in the googleLogin.
 */
@Component(name = "com.ibm.ws.security.social.google", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, service = { SocialLoginConfig.class, JwtConsumerConfig.class }, property = { "service.vendor=IBM", "type=googleLogin" })
public class GoogleLoginConfigImpl extends OidcLoginConfigImpl {
    public static final TraceComponent tc = Tr.register(GoogleLoginConfigImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    @Override
    protected void setRequiredConfigAttributes(Map<String, Object> props) {
        this.clientId = getRequiredConfigAttribute(props, KEY_clientId);
        this.clientSecret = getRequiredSerializableProtectedStringConfigAttribute(props, KEY_clientSecret);
    }

    @Override
    protected void setOptionalConfigAttributes(Map<String, Object> props) throws SocialLoginException {
        this.authorizationEndpoint = configUtils.getConfigAttribute(props, KEY_authorizationEndpoint);
        this.tokenEndpoint = configUtils.getConfigAttribute(props, KEY_tokenEndpoint);
        this.jwksUri = configUtils.getConfigAttribute(props, KEY_jwksUri);
        this.scope = configUtils.getConfigAttribute(props, KEY_scope);
        this.userNameAttribute = configUtils.getConfigAttribute(props, KEY_userNameAttribute);
        this.mapToUserRegistry = configUtils.getBooleanConfigAttribute(props, KEY_mapToUserRegistry, this.mapToUserRegistry);
        this.sslRef = configUtils.getConfigAttribute(props, KEY_sslRef);
        this.authFilterRef = configUtils.getConfigAttribute(props, KEY_authFilterRef);
        this.isClientSideRedirectSupported = configUtils.getBooleanConfigAttribute(props, KEY_isClientSideRedirectSupported, this.isClientSideRedirectSupported);
        this.displayName = configUtils.getConfigAttribute(props, KEY_displayName);
        this.website = configUtils.getConfigAttribute(props, KEY_website);
        this.tokenEndpointAuthMethod = configUtils.getConfigAttribute(props, KEY_tokenEndpointAuthMethod);
        this.redirectToRPHostAndPort = configUtils.getConfigAttribute(props, KEY_redirectToRPHostAndPort);
        this.issuer = configUtils.getConfigAttribute(props, KEY_ISSUER);
        this.realmNameAttribute = configUtils.getConfigAttribute(props, KEY_realmNameAttribute);
        this.groupNameAttribute = configUtils.getConfigAttribute(props, KEY_groupNameAttribute);
        this.userUniqueIdAttribute = configUtils.getConfigAttribute(props, KEY_userUniqueIdAttribute);
        this.clockSkewMsec = configUtils.getIntegerConfigAttribute(props, KEY_CLOCKSKEW, this.clockSkewMsec);
        this.signatureAlgorithm = configUtils.getConfigAttribute(props, KEY_SIGNATURE_ALGORITHM);
    }

    @Override
    protected void debug() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "" + this);
            Tr.debug(tc, KEY_clientId + " = " + clientId);
            Tr.debug(tc, KEY_clientSecret + " is null = " + (clientSecret == null));
            Tr.debug(tc, KEY_authorizationEndpoint + " = " + authorizationEndpoint);
            Tr.debug(tc, KEY_tokenEndpoint + " = " + tokenEndpoint);
            Tr.debug(tc, KEY_jwksUri + " = " + jwksUri);
            Tr.debug(tc, KEY_scope + " = " + scope);
            Tr.debug(tc, KEY_userNameAttribute + " = " + userNameAttribute);
            Tr.debug(tc, KEY_mapToUserRegistry + " = " + mapToUserRegistry);
            Tr.debug(tc, KEY_sslRef + " = " + sslRef);
            Tr.debug(tc, KEY_authFilterRef + " = " + authFilterRef);
            Tr.debug(tc, CFG_KEY_jwtRef + " = " + jwtRef);
            Tr.debug(tc, CFG_KEY_jwtClaims + " = " + ((jwtClaims == null) ? null : Arrays.toString(jwtClaims)));
            Tr.debug(tc, KEY_isClientSideRedirectSupported + " = " + isClientSideRedirectSupported);
            Tr.debug(tc, KEY_displayName + " = " + displayName);
            Tr.debug(tc, KEY_website + " = " + website);
            Tr.debug(tc, KEY_tokenEndpointAuthMethod + " = " + tokenEndpointAuthMethod);
            Tr.debug(tc, KEY_redirectToRPHostAndPort + " = " + redirectToRPHostAndPort);
            Tr.debug(tc, KEY_ISSUER + " = " + issuer);
            Tr.debug(tc, KEY_realmNameAttribute + " = " + realmNameAttribute);
            Tr.debug(tc, KEY_groupNameAttribute + " = " + groupNameAttribute);
            Tr.debug(tc, KEY_userUniqueIdAttribute + " = " + userUniqueIdAttribute);
            Tr.debug(tc, KEY_CLOCKSKEW + " = " + clockSkewMsec);
            Tr.debug(tc, KEY_SIGNATURE_ALGORITHM + " = " + signatureAlgorithm);
        }
    }

    @Override
    public String getSignatureAlgorithm() {
        if (jwksUri == null) {
            return null;
        } else {
            return signatureAlgorithm;
        }
    }

}
