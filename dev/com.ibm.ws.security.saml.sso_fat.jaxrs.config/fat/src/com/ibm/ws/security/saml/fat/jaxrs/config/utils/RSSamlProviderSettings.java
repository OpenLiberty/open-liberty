/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.fat.jaxrs.config.utils;

import com.ibm.ws.security.saml20.fat.commonTest.config.settings.SAMLProviderSettings;

public class RSSamlProviderSettings extends SAMLProviderSettings {

    private static final Class<?> thisClass = RSSamlProviderSettings.class;

    public RSSamlProviderSettings() {
        super();
        setInboundPropagation("required");
    }

    protected RSSamlProviderSettings(String id, String inboundPropagation, String wantAssertionsSigned, String signatureMethodAlgorithm,
            String createSession, String authnRequestsSigned, String forceAuthn, String isPassive, String allowCreate, String authnContextClassRef,
            String authnContextComparisonType, String nameIDFormat, String customizeNameIDFormat, String idpMetadata, String keyStoreRef, String keyAlias,
            String loginPageURL, String errorPageURL, String clockSkew, String tokenReplayTimeout, String sessionNotOnOrAfter, String userIdentifier,
            String groupIdentifier, String userUniqueIdentifier, String realmIdentifier, String includeTokenInSubject, String mapToUserRegistry,
            String pkixTrustEngine, String authFilterRef, String disableLtpaCookie, String realmName, String authnRequestTime, String enabled,
            String httpsRequired, String allowCustomCacheKey, String spHostAndPort, String headerName, String audiences) {

        super(id, inboundPropagation, wantAssertionsSigned, signatureMethodAlgorithm,
                createSession, authnRequestsSigned, forceAuthn, isPassive, allowCreate, authnContextClassRef,
                authnContextComparisonType, nameIDFormat, customizeNameIDFormat, idpMetadata, keyStoreRef, keyAlias,
                loginPageURL, errorPageURL, clockSkew, tokenReplayTimeout, sessionNotOnOrAfter, userIdentifier,
                groupIdentifier, userUniqueIdentifier, realmIdentifier, includeTokenInSubject, mapToUserRegistry,
                pkixTrustEngine, authFilterRef, disableLtpaCookie, realmName, authnRequestTime, enabled,
                httpsRequired, allowCustomCacheKey, spHostAndPort, headerName, audiences);
    }

    @Override
    public RSSamlProviderSettings createShallowCopy() {
        return new RSSamlProviderSettings(getId(), getInboundPropagation(), getWantAssertionsSigned(), getSignatureMethodAlgorithm(),
                getCreateSession(), getAuthnRequestsSigned(), getForceAuthn(), getIsPassive(), getAllowCreate(), getAuthnContextClassRef(),
                getAuthnContextComparisonType(), getNameIDFormat(), getCustomizeNameIDFormat(), getIdpMetadata(), getKeyStoreRef(), getKeyAlias(),
                getLoginPageURL(), getErrorPageURL(), getClockSkew(), getTokenReplayTimeout(), getSessionNotOnOrAfter(), getUserIdentifier(),
                getGroupIdentifier(), getUserUniqueIdentifier(), getRealmIdentifier(), getIncludeTokenInSubject(), getMapToUserRegistry(),
                getPkixTrustEngine(), getAuthFilterRef(), getDisableLtpaCookie(), getRealmName(), getAuthnRequestTime(), getEnabled(),
                getHttpsRequired(), getAllowCustomCacheKey(), getSpHostAndPort(), getHeaderName(), getAudiences());
    }

    @Override
    public RSSamlProviderSettings copyConfigSettings() {
        return copyConfigSettings(this);
    }

}
