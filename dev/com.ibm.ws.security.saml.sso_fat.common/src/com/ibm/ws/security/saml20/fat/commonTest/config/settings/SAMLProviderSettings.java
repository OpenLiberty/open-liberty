/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml20.fat.commonTest.config.settings;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.config.settings.BaseConfigSettings;

public class SAMLProviderSettings extends BaseConfigSettings {

    private Class<?> thisClass = SAMLProviderSettings.class;

    public static final String CONFIG_ELEMENT_NAME = "samlWebSso20";

    public static final String VAR_SAML_PROVIDER_ID = "${saml.provider.id}";
    public static final String VAR_SAML_PROVIDER_INBOUND_PROPAGATION = "${saml.provider.inboundPropagation}";
    public static final String VAR_SAML_PROVIDER_WANT_ASSERTIONS_SIGNED = "${saml.provider.wantAssertionsSigned}";
    public static final String VAR_SAML_PROVIDER_SIG_METHOD_ALG = "${saml.provider.signatureMethodAlgorithm}";
    public static final String VAR_SAML_PROVIDER_CREATE_SESSION = "${saml.provider.createSession}";
    public static final String VAR_SAML_PROVIDER_AUTHN_REQ_SIGNED = "${saml.provider.authnRequestsSigned}";
    public static final String VAR_SAML_PROVIDER_FORCE_AUTHN = "${saml.provider.forceAuthn}";
    public static final String VAR_SAML_PROVIDER_IS_PASSIVE = "${saml.provider.isPassive}";
    public static final String VAR_SAML_PROVIDER_ALLOW_CREATE = "${saml.provider.allowCreate}";
    public static final String VAR_SAML_PROVIDER_AUTHN_CONTEXT_CLASS_REF = "${saml.provider.authnContextClassRef}";
    public static final String VAR_SAML_PROVIDER_AUTHN_CONTEXT_COMP_TYPE = "${saml.provider.authnContextComparisonType}";
    public static final String VAR_SAML_PROVIDER_NAME_ID_FORMAT = "${saml.provider.nameIDFormat}";
    public static final String VAR_SAML_PROVIDER_CUSTOMIZE_NAME_ID_FORMAT = "${saml.provider.customizeNameIDFormat}";
    public static final String VAR_SAML_PROVIDER_IDP_METADATA = "${saml.provider.idpMetadata}";
    public static final String VAR_SAML_PROVIDER_KEYSTORE_REF = "${saml.provider.keyStoreRef}";
    public static final String VAR_SAML_PROVIDER_KEY_ALIAS = "${saml.provider.keyAlias}";
    public static final String VAR_SAML_PROVIDER_LOGIN_PAGE_URL = "${saml.provider.loginPageURL}";
    public static final String VAR_SAML_PROVIDER_ERROR_PAGE_URL = "${saml.provider.errorPageURL}";
    public static final String VAR_SAML_PROVIDER_CLOCK_SKEW = "${saml.provider.clockSkew}";
    public static final String VAR_SAML_PROVIDER_TOKEN_REP_TIMEOUT = "${saml.provider.tokenReplayTimeout}";
    public static final String VAR_SAML_PROVIDER_SESSION_NOT_ON_OR_AFTER = "${saml.provider.sessionNotOnOrAfter}";
    public static final String VAR_SAML_PROVIDER_USER_IDENTIFIER = "${saml.provider.userIdentifier}";
    public static final String VAR_SAML_PROVIDER_GROUP_IDENTIFIER = "${saml.provider.groupIdentifier}";
    public static final String VAR_SAML_PROVIDER_USER_UNIQUE_IDENTIFIER = "${saml.provider.userUniqueIdentifier}";
    public static final String VAR_SAML_PROVIDER_REALM_IDENTIFIER = "${saml.provider.realmIdentifier}";
    public static final String VAR_SAML_PROVIDER_INCLUDE_TOKEN_IN_SUBJECT = "${saml.provider.includeTokenInSubject}";
    public static final String VAR_SAML_PROVIDER_MAP_TO_USER_REG = "${saml.provider.mapToUserRegistry}";
    public static final String VAR_SAML_PROVIDER_PKIX_TRUST_ENGINE = "${saml.provider.pkixTrustEngine}";
    public static final String VAR_SAML_PROVIDER_AUTH_FILTER_REF = "${saml.provider.authFilterRef}";
    public static final String VAR_SAML_PROVIDER_DISABLE_LTPA_COOKIE = "${saml.provider.disableLtpaCookie}";
    public static final String VAR_SAML_PROVIDER_REALM_NAME = "${saml.provider.realmName}";
    public static final String VAR_SAML_PROVIDER_AUTHN_REQ_TIME = "${saml.provider.authnRequestTime}";
    public static final String VAR_SAML_PROVIDER_ENABLED = "${saml.provider.enabled}";
    public static final String VAR_SAML_PROVIDER_HTTPS_REQUIRED = "${saml.provider.httpsRequired}";
    public static final String VAR_SAML_PROVIDER_ALLOW_CUSTOM_CACHE_KEY = "${saml.provider.allowCustomCacheKey}";
    public static final String VAR_SAML_PROVIDER_SP_HOST_AND_PORT = "${saml.provider.spHostAndPort}";
    public static final String VAR_SAML_PROVIDER_HEADER_NAME = "${saml.provider.headerName}";
    public static final String VAR_SAML_PROVIDER_AUDIENCES = "${saml.provider.audiences}";

    public static final String ATTR_SAML_PROVIDER_ID = "id";
    public static final String ATTR_SAML_PROVIDER_INBOUND_PROPAGATION = "inboundPropagation";
    public static final String ATTR_SAML_PROVIDER_WANT_ASSERTIONS_SIGNED = "wantAssertionsSigned";
    public static final String ATTR_SAML_PROVIDER_SIG_METHOD_ALG = "signatureMethodAlgorithm";
    public static final String ATTR_SAML_PROVIDER_CREATE_SESSION = "createSession";
    public static final String ATTR_SAML_PROVIDER_AUTHN_REQ_SIGNED = "authnRequestsSigned";
    public static final String ATTR_SAML_PROVIDER_FORCE_AUTHN = "forceAuthn";
    public static final String ATTR_SAML_PROVIDER_IS_PASSIVE = "isPassive";
    public static final String ATTR_SAML_PROVIDER_ALLOW_CREATE = "allowCreate";
    public static final String ATTR_SAML_PROVIDER_AUTHN_CONTEXT_CLASS_REF = "authnContextClassRef";
    public static final String ATTR_SAML_PROVIDER_AUTHN_CONTEXT_COMP_TYPE = "authnContextComparisonType";
    public static final String ATTR_SAML_PROVIDER_NAME_ID_FORMAT = "nameIDFormat";
    public static final String ATTR_SAML_PROVIDER_CUSTOMIZE_NAME_ID_FORMAT = "customizeNameIDFormat";
    public static final String ATTR_SAML_PROVIDER_IDP_METADATA = "idpMetadata";
    public static final String ATTR_SAML_PROVIDER_KEYSTORE_REF = "keyStoreRef";
    public static final String ATTR_SAML_PROVIDER_KEY_ALIAS = "keyAlias";
    public static final String ATTR_SAML_PROVIDER_LOGIN_PAGE_URL = "loginPageURL";
    public static final String ATTR_SAML_PROVIDER_ERROR_PAGE_URL = "errorPageURL";
    public static final String ATTR_SAML_PROVIDER_CLOCK_SKEW = "clockSkew";
    public static final String ATTR_SAML_PROVIDER_TOKEN_REP_TIMEOUT = "tokenReplayTimeout";
    public static final String ATTR_SAML_PROVIDER_SESSION_NOT_ON_OR_AFTER = "sessionNotOnOrAfter";
    public static final String ATTR_SAML_PROVIDER_USER_IDENTIFIER = "userIdentifier";
    public static final String ATTR_SAML_PROVIDER_GROUP_IDENTIFIER = "groupIdentifier";
    public static final String ATTR_SAML_PROVIDER_USER_UNIQUE_IDENTIFIER = "userUniqueIdentifier";
    public static final String ATTR_SAML_PROVIDER_REALM_IDENTIFIER = "realmIdentifier";
    public static final String ATTR_SAML_PROVIDER_INCLUDE_TOKEN_IN_SUBJECT = "includeTokenInSubject";
    public static final String ATTR_SAML_PROVIDER_MAP_TO_USER_REG = "mapToUserRegistry";
    public static final String ATTR_SAML_PROVIDER_PKIX_TRUST_ENGINE = "pkixTrustEngine";
    public static final String ATTR_SAML_PROVIDER_AUTH_FILTER_REF = "authFilterRef";
    public static final String ATTR_SAML_PROVIDER_DISABLE_LTPA_COOKIE = "disableLtpaCookie";
    public static final String ATTR_SAML_PROVIDER_REALM_NAME = "realmName";
    public static final String ATTR_SAML_PROVIDER_AUTHN_REQ_TIME = "authnRequestTime";
    public static final String ATTR_SAML_PROVIDER_ENABLED = "enabled";
    public static final String ATTR_SAML_PROVIDER_HTTPS_REQUIRED = "httpsRequired";
    public static final String ATTR_SAML_PROVIDER_ALLOW_CUSTOM_CACHE_KEY = "allowCustomCacheKey";
    public static final String ATTR_SAML_PROVIDER_SP_HOST_AND_PORT = "spHostAndPort";
    public static final String ATTR_SAML_PROVIDER_HEADER_NAME = "headerName";
    public static final String ATTR_SAML_PROVIDER_AUDIENCES = "audiences";

    protected String id = "sp1";
    protected String inboundPropagation = null;
    protected String wantAssertionsSigned = null;
    protected String signatureMethodAlgorithm = "SHA1";
    protected String createSession = null;
    protected String authnRequestsSigned = null;
    protected String forceAuthn = null;
    protected String isPassive = null;
    protected String allowCreate = null;
    protected String authnContextClassRef = null;
    protected String authnContextComparisonType = null;
    protected String nameIDFormat = null;
    protected String customizeNameIDFormat = null;
    protected String idpMetadata = "${server.config.dir}/imports/${tfimIdpServer}/Fed1MetaData.xml";
    protected String keyStoreRef = "samlKeyStore";
    protected String keyAlias = "sslspservercert";
    protected String loginPageURL = null;
    protected String errorPageURL = null;
    protected String clockSkew = null;
    protected String tokenReplayTimeout = null;
    protected String sessionNotOnOrAfter = null;
    protected String userIdentifier = null;
    protected String groupIdentifier = null;
    protected String userUniqueIdentifier = null;
    protected String realmIdentifier = null;
    protected String includeTokenInSubject = null;
    protected String mapToUserRegistry = null;
    protected String pkixTrustEngine = null;
    protected String authFilterRef = "myAuthFilter1";
    protected String disableLtpaCookie = null;
    protected String realmName = null;
    protected String authnRequestTime = null;
    protected String enabled = null;
    protected String httpsRequired = null;
    protected String allowCustomCacheKey = null;
    protected String spHostAndPort = null;
    protected String headerName = null;
    protected String audiences = null;

    private Map<String, String> propsMap = new HashMap<String, String>();

    protected static final String[] pureSamlAttributes = new String[] {
            ATTR_SAML_PROVIDER_AUTHN_REQ_SIGNED,
            ATTR_SAML_PROVIDER_FORCE_AUTHN,
            ATTR_SAML_PROVIDER_IS_PASSIVE,
            ATTR_SAML_PROVIDER_ALLOW_CREATE,
            ATTR_SAML_PROVIDER_AUTHN_CONTEXT_CLASS_REF,
            ATTR_SAML_PROVIDER_AUTHN_CONTEXT_COMP_TYPE,
            ATTR_SAML_PROVIDER_NAME_ID_FORMAT,
            ATTR_SAML_PROVIDER_CUSTOMIZE_NAME_ID_FORMAT,
            ATTR_SAML_PROVIDER_IDP_METADATA,
            ATTR_SAML_PROVIDER_LOGIN_PAGE_URL,
            ATTR_SAML_PROVIDER_ERROR_PAGE_URL,
            ATTR_SAML_PROVIDER_TOKEN_REP_TIMEOUT,
            ATTR_SAML_PROVIDER_SESSION_NOT_ON_OR_AFTER,
            ATTR_SAML_PROVIDER_INCLUDE_TOKEN_IN_SUBJECT,
            ATTR_SAML_PROVIDER_HTTPS_REQUIRED,
            ATTR_SAML_PROVIDER_ALLOW_CUSTOM_CACHE_KEY,
            ATTR_SAML_PROVIDER_SP_HOST_AND_PORT,
            ATTR_SAML_PROVIDER_CREATE_SESSION
    };

    protected static final String[] pureInboundPropagationAttributes = new String[] {
            ATTR_SAML_PROVIDER_HEADER_NAME,
            ATTR_SAML_PROVIDER_AUDIENCES
    };

    public SAMLProviderSettings() {
        configElementName = CONFIG_ELEMENT_NAME;

        setProps(id, inboundPropagation, wantAssertionsSigned, signatureMethodAlgorithm,
                createSession, authnRequestsSigned, forceAuthn, isPassive, allowCreate, authnContextClassRef,
                authnContextComparisonType, nameIDFormat, customizeNameIDFormat, idpMetadata, keyStoreRef, keyAlias,
                loginPageURL, errorPageURL, clockSkew, tokenReplayTimeout, sessionNotOnOrAfter, userIdentifier,
                groupIdentifier, userUniqueIdentifier, realmIdentifier, includeTokenInSubject, mapToUserRegistry,
                pkixTrustEngine, authFilterRef, disableLtpaCookie, realmName, authnRequestTime, enabled,
                httpsRequired, allowCustomCacheKey, spHostAndPort, headerName, audiences);
    }

    protected SAMLProviderSettings(String id, String inboundPropagation, String wantAssertionsSigned, String signatureMethodAlgorithm,
            String createSession, String authnRequestsSigned, String forceAuthn, String isPassive, String allowCreate, String authnContextClassRef,
            String authnContextComparisonType, String nameIDFormat, String customizeNameIDFormat, String idpMetadata, String keyStoreRef, String keyAlias,
            String loginPageURL, String errorPageURL, String clockSkew, String tokenReplayTimeout, String sessionNotOnOrAfter, String userIdentifier,
            String groupIdentifier, String userUniqueIdentifier, String realmIdentifier, String includeTokenInSubject, String mapToUserRegistry,
            String pkixTrustEngine, String authFilterRef, String disableLtpaCookie, String realmName, String authnRequestTime, String enabled,
            String httpsRequired, String allowCustomCacheKey, String spHostAndPort, String headerName, String audiences) {

        this();

        setProps(id, inboundPropagation, wantAssertionsSigned, signatureMethodAlgorithm,
                createSession, authnRequestsSigned, forceAuthn, isPassive, allowCreate, authnContextClassRef,
                authnContextComparisonType, nameIDFormat, customizeNameIDFormat, idpMetadata, keyStoreRef, keyAlias,
                loginPageURL, errorPageURL, clockSkew, tokenReplayTimeout, sessionNotOnOrAfter, userIdentifier,
                groupIdentifier, userUniqueIdentifier, realmIdentifier, includeTokenInSubject, mapToUserRegistry,
                pkixTrustEngine, authFilterRef, disableLtpaCookie, realmName, authnRequestTime, enabled,
                httpsRequired, allowCustomCacheKey, spHostAndPort, headerName, audiences);
    }

    private void setProps(String id, String inboundPropagation, String wantAssertionsSigned, String signatureMethodAlgorithm,
            String createSession, String authnRequestsSigned, String forceAuthn, String isPassive, String allowCreate, String authnContextClassRef,
            String authnContextComparisonType, String nameIDFormat, String customizeNameIDFormat, String idpMetadata, String keyStoreRef, String keyAlias,
            String loginPageURL, String errorPageURL, String clockSkew, String tokenReplayTimeout, String sessionNotOnOrAfter, String userIdentifier,
            String groupIdentifier, String userUniqueIdentifier, String realmIdentifier, String includeTokenInSubject, String mapToUserRegistry,
            String pkixTrustEngine, String authFilterRef, String disableLtpaCookie, String realmName, String authnRequestTime, String enabled,
            String httpsRequired, String allowCustomCacheKey, String spHostAndPort, String headerName, String audiences) {

        propsMap.put(ATTR_SAML_PROVIDER_ID, id);
        propsMap.put(ATTR_SAML_PROVIDER_INBOUND_PROPAGATION, inboundPropagation);
        propsMap.put(ATTR_SAML_PROVIDER_WANT_ASSERTIONS_SIGNED, wantAssertionsSigned);
        propsMap.put(ATTR_SAML_PROVIDER_SIG_METHOD_ALG, signatureMethodAlgorithm);
        propsMap.put(ATTR_SAML_PROVIDER_CREATE_SESSION, createSession);
        propsMap.put(ATTR_SAML_PROVIDER_AUTHN_REQ_SIGNED, authnRequestsSigned);
        propsMap.put(ATTR_SAML_PROVIDER_FORCE_AUTHN, forceAuthn);
        propsMap.put(ATTR_SAML_PROVIDER_IS_PASSIVE, isPassive);
        propsMap.put(ATTR_SAML_PROVIDER_ALLOW_CREATE, allowCreate);
        propsMap.put(ATTR_SAML_PROVIDER_AUTHN_CONTEXT_CLASS_REF, authnContextClassRef);
        propsMap.put(ATTR_SAML_PROVIDER_AUTHN_CONTEXT_COMP_TYPE, authnContextComparisonType);
        propsMap.put(ATTR_SAML_PROVIDER_NAME_ID_FORMAT, nameIDFormat);
        propsMap.put(ATTR_SAML_PROVIDER_CUSTOMIZE_NAME_ID_FORMAT, customizeNameIDFormat);
        propsMap.put(ATTR_SAML_PROVIDER_IDP_METADATA, idpMetadata);
        propsMap.put(ATTR_SAML_PROVIDER_KEYSTORE_REF, keyStoreRef);
        propsMap.put(ATTR_SAML_PROVIDER_KEY_ALIAS, keyAlias);
        propsMap.put(ATTR_SAML_PROVIDER_LOGIN_PAGE_URL, loginPageURL);
        propsMap.put(ATTR_SAML_PROVIDER_ERROR_PAGE_URL, errorPageURL);
        propsMap.put(ATTR_SAML_PROVIDER_CLOCK_SKEW, clockSkew);
        propsMap.put(ATTR_SAML_PROVIDER_TOKEN_REP_TIMEOUT, tokenReplayTimeout);
        propsMap.put(ATTR_SAML_PROVIDER_SESSION_NOT_ON_OR_AFTER, sessionNotOnOrAfter);
        propsMap.put(ATTR_SAML_PROVIDER_USER_IDENTIFIER, userIdentifier);
        propsMap.put(ATTR_SAML_PROVIDER_GROUP_IDENTIFIER, groupIdentifier);
        propsMap.put(ATTR_SAML_PROVIDER_USER_UNIQUE_IDENTIFIER, userUniqueIdentifier);
        propsMap.put(ATTR_SAML_PROVIDER_REALM_IDENTIFIER, realmIdentifier);
        propsMap.put(ATTR_SAML_PROVIDER_INCLUDE_TOKEN_IN_SUBJECT, includeTokenInSubject);
        propsMap.put(ATTR_SAML_PROVIDER_MAP_TO_USER_REG, mapToUserRegistry);
        propsMap.put(ATTR_SAML_PROVIDER_PKIX_TRUST_ENGINE, pkixTrustEngine);
        propsMap.put(ATTR_SAML_PROVIDER_AUTH_FILTER_REF, authFilterRef);
        propsMap.put(ATTR_SAML_PROVIDER_DISABLE_LTPA_COOKIE, disableLtpaCookie);
        propsMap.put(ATTR_SAML_PROVIDER_REALM_NAME, realmName);
        propsMap.put(ATTR_SAML_PROVIDER_AUTHN_REQ_TIME, authnRequestTime);
        propsMap.put(ATTR_SAML_PROVIDER_ENABLED, enabled);
        propsMap.put(ATTR_SAML_PROVIDER_HTTPS_REQUIRED, httpsRequired);
        propsMap.put(ATTR_SAML_PROVIDER_ALLOW_CUSTOM_CACHE_KEY, allowCustomCacheKey);
        propsMap.put(ATTR_SAML_PROVIDER_SP_HOST_AND_PORT, spHostAndPort);
        propsMap.put(ATTR_SAML_PROVIDER_HEADER_NAME, headerName);
        propsMap.put(ATTR_SAML_PROVIDER_AUDIENCES, audiences);
    }

    @Override
    public SAMLProviderSettings createShallowCopy() {
        return new SAMLProviderSettings(getId(), getInboundPropagation(), getWantAssertionsSigned(), getSignatureMethodAlgorithm(),
                getCreateSession(), getAuthnRequestsSigned(), getForceAuthn(), getIsPassive(), getAllowCreate(), getAuthnContextClassRef(),
                getAuthnContextComparisonType(), getNameIDFormat(), getCustomizeNameIDFormat(), getIdpMetadata(), getKeyStoreRef(), getKeyAlias(),
                getLoginPageURL(), getErrorPageURL(), getClockSkew(), getTokenReplayTimeout(), getSessionNotOnOrAfter(), getUserIdentifier(),
                getGroupIdentifier(), getUserUniqueIdentifier(), getRealmIdentifier(), getIncludeTokenInSubject(), getMapToUserRegistry(),
                getPkixTrustEngine(), getAuthFilterRef(), getDisableLtpaCookie(), getRealmName(), getAuthnRequestTime(), getEnabled(),
                getHttpsRequired(), getAllowCustomCacheKey(), getSpHostAndPort(), getHeaderName(), getAudiences());
    }

    @Override
    public SAMLProviderSettings copyConfigSettings() {
        return copyConfigSettings(this);
    }

    @Override
    public Map<String, String> getConfigAttributesMap() {
        String method = "getConfigAttributesMap";
        if (debug) {
            Log.info(thisClass, method, "Getting map of SAML provider attributes");
        }
        return propsMap;
    }

    @Override
    public Map<String, String> getConfigSettingsVariablesMap() {
        String method = "getConfigSettingsVariablesMap";
        if (debug) {
            Log.info(thisClass, method, "Getting map of SAML provider settings config variables");
        }
        Map<String, String> settings = new HashMap<String, String>();

        settings.put(VAR_SAML_PROVIDER_ID, getId());
        settings.put(VAR_SAML_PROVIDER_INBOUND_PROPAGATION, getInboundPropagation());
        settings.put(VAR_SAML_PROVIDER_WANT_ASSERTIONS_SIGNED, getWantAssertionsSigned());
        settings.put(VAR_SAML_PROVIDER_SIG_METHOD_ALG, getSignatureMethodAlgorithm());
        settings.put(VAR_SAML_PROVIDER_CREATE_SESSION, getCreateSession());
        settings.put(VAR_SAML_PROVIDER_AUTHN_REQ_SIGNED, getAuthnRequestsSigned());
        settings.put(VAR_SAML_PROVIDER_FORCE_AUTHN, getForceAuthn());
        settings.put(VAR_SAML_PROVIDER_IS_PASSIVE, getIsPassive());
        settings.put(VAR_SAML_PROVIDER_ALLOW_CREATE, getAllowCreate());
        settings.put(VAR_SAML_PROVIDER_AUTHN_CONTEXT_CLASS_REF, getAuthnContextClassRef());
        settings.put(VAR_SAML_PROVIDER_AUTHN_CONTEXT_COMP_TYPE, getAuthnContextComparisonType());
        settings.put(VAR_SAML_PROVIDER_NAME_ID_FORMAT, getNameIDFormat());
        settings.put(VAR_SAML_PROVIDER_CUSTOMIZE_NAME_ID_FORMAT, getCustomizeNameIDFormat());
        settings.put(VAR_SAML_PROVIDER_IDP_METADATA, getIdpMetadata());
        settings.put(VAR_SAML_PROVIDER_KEYSTORE_REF, getKeyStoreRef());
        settings.put(VAR_SAML_PROVIDER_KEY_ALIAS, getKeyAlias());
        settings.put(VAR_SAML_PROVIDER_LOGIN_PAGE_URL, getLoginPageURL());
        settings.put(VAR_SAML_PROVIDER_ERROR_PAGE_URL, getErrorPageURL());
        settings.put(VAR_SAML_PROVIDER_CLOCK_SKEW, getClockSkew());
        settings.put(VAR_SAML_PROVIDER_TOKEN_REP_TIMEOUT, getTokenReplayTimeout());
        settings.put(VAR_SAML_PROVIDER_SESSION_NOT_ON_OR_AFTER, getSessionNotOnOrAfter());
        settings.put(VAR_SAML_PROVIDER_USER_IDENTIFIER, getUserIdentifier());
        settings.put(VAR_SAML_PROVIDER_GROUP_IDENTIFIER, getGroupIdentifier());
        settings.put(VAR_SAML_PROVIDER_USER_UNIQUE_IDENTIFIER, getUserUniqueIdentifier());
        settings.put(VAR_SAML_PROVIDER_REALM_IDENTIFIER, getRealmIdentifier());
        settings.put(VAR_SAML_PROVIDER_INCLUDE_TOKEN_IN_SUBJECT, getIncludeTokenInSubject());
        settings.put(VAR_SAML_PROVIDER_MAP_TO_USER_REG, getMapToUserRegistry());
        settings.put(VAR_SAML_PROVIDER_PKIX_TRUST_ENGINE, getPkixTrustEngine());
        settings.put(VAR_SAML_PROVIDER_AUTH_FILTER_REF, getAuthFilterRef());
        settings.put(VAR_SAML_PROVIDER_DISABLE_LTPA_COOKIE, getDisableLtpaCookie());
        settings.put(VAR_SAML_PROVIDER_REALM_NAME, getRealmName());
        settings.put(VAR_SAML_PROVIDER_AUTHN_REQ_TIME, getAuthnRequestTime());
        settings.put(VAR_SAML_PROVIDER_ENABLED, getEnabled());
        settings.put(VAR_SAML_PROVIDER_HTTPS_REQUIRED, getHttpsRequired());
        settings.put(VAR_SAML_PROVIDER_ALLOW_CUSTOM_CACHE_KEY, getAllowCustomCacheKey());
        settings.put(VAR_SAML_PROVIDER_SP_HOST_AND_PORT, getSpHostAndPort());
        settings.put(VAR_SAML_PROVIDER_HEADER_NAME, getHeaderName());
        settings.put(VAR_SAML_PROVIDER_AUDIENCES, getAudiences());

        return settings;
    }

    /********************************* Private member getters and setters *********************************/

    public void setId(String id) {
        propsMap.put(ATTR_SAML_PROVIDER_ID, id);
    }

    public String getId() {
        return propsMap.get(ATTR_SAML_PROVIDER_ID);
    }

    public void setInboundPropagation(String inboundPropagation) {
        propsMap.put(ATTR_SAML_PROVIDER_INBOUND_PROPAGATION, inboundPropagation);
    }

    public String getInboundPropagation() {
        return propsMap.get(ATTR_SAML_PROVIDER_INBOUND_PROPAGATION);
    }

    public void setWantAssertionsSigned(String wantAssertionsSigned) {
        propsMap.put(ATTR_SAML_PROVIDER_WANT_ASSERTIONS_SIGNED, wantAssertionsSigned);
    }

    public String getWantAssertionsSigned() {
        return propsMap.get(ATTR_SAML_PROVIDER_WANT_ASSERTIONS_SIGNED);
    }

    public void setSignatureMethodAlgorithm(String signatureMethodAlgorithm) {
        propsMap.put(ATTR_SAML_PROVIDER_SIG_METHOD_ALG, signatureMethodAlgorithm);
    }

    public String getSignatureMethodAlgorithm() {
        return propsMap.get(ATTR_SAML_PROVIDER_SIG_METHOD_ALG);
    }

    public void setCreateSession(String createSession) {
        propsMap.put(ATTR_SAML_PROVIDER_CREATE_SESSION, createSession);
    }

    public String getCreateSession() {
        return propsMap.get(ATTR_SAML_PROVIDER_CREATE_SESSION);
    }

    public void setAuthnRequestsSigned(String authnRequestsSigned) {
        propsMap.put(ATTR_SAML_PROVIDER_AUTHN_REQ_SIGNED, authnRequestsSigned);
    }

    public String getAuthnRequestsSigned() {
        return propsMap.get(ATTR_SAML_PROVIDER_AUTHN_REQ_SIGNED);
    }

    public void setForceAuthn(String forceAuthn) {
        propsMap.put(ATTR_SAML_PROVIDER_FORCE_AUTHN, forceAuthn);
    }

    public String getForceAuthn() {
        return propsMap.get(ATTR_SAML_PROVIDER_FORCE_AUTHN);
    }

    public void setIsPassive(String isPassive) {
        propsMap.put(ATTR_SAML_PROVIDER_IS_PASSIVE, isPassive);
    }

    public String getIsPassive() {
        return propsMap.get(ATTR_SAML_PROVIDER_IS_PASSIVE);
    }

    public void setAllowCreate(String allowCreate) {
        propsMap.put(ATTR_SAML_PROVIDER_ALLOW_CREATE, allowCreate);
    }

    public String getAllowCreate() {
        return propsMap.get(ATTR_SAML_PROVIDER_ALLOW_CREATE);
    }

    public void setAuthnContextClassRef(String authnContextClassRef) {
        propsMap.put(ATTR_SAML_PROVIDER_AUTHN_CONTEXT_CLASS_REF, authnContextClassRef);
    }

    public String getAuthnContextClassRef() {
        return propsMap.get(ATTR_SAML_PROVIDER_AUTHN_CONTEXT_CLASS_REF);
    }

    public void setAuthnContextComparisonType(String authnContextComparisonType) {
        propsMap.put(ATTR_SAML_PROVIDER_AUTHN_CONTEXT_COMP_TYPE, authnContextComparisonType);
    }

    public String getAuthnContextComparisonType() {
        return propsMap.get(ATTR_SAML_PROVIDER_AUTHN_CONTEXT_COMP_TYPE);
    }

    public void setNameIDFormat(String nameIDFormat) {
        propsMap.put(ATTR_SAML_PROVIDER_NAME_ID_FORMAT, nameIDFormat);
    }

    public String getNameIDFormat() {
        return propsMap.get(ATTR_SAML_PROVIDER_NAME_ID_FORMAT);
    }

    public void setCustomizeNameIDFormat(String customizeNameIDFormat) {
        propsMap.put(ATTR_SAML_PROVIDER_CUSTOMIZE_NAME_ID_FORMAT, customizeNameIDFormat);
    }

    public String getCustomizeNameIDFormat() {
        return propsMap.get(ATTR_SAML_PROVIDER_CUSTOMIZE_NAME_ID_FORMAT);
    }

    public void setIdpMetadata(String idpMetadata) {
        propsMap.put(ATTR_SAML_PROVIDER_IDP_METADATA, idpMetadata);
    }

    public String getIdpMetadata() {
        return propsMap.get(ATTR_SAML_PROVIDER_IDP_METADATA);
    }

    public void setKeyStoreRef(String keyStoreRef) {
        propsMap.put(ATTR_SAML_PROVIDER_KEYSTORE_REF, keyStoreRef);
    }

    public String getKeyStoreRef() {
        return propsMap.get(ATTR_SAML_PROVIDER_KEYSTORE_REF);
    }

    public void setKeyAlias(String keyAlias) {
        propsMap.put(ATTR_SAML_PROVIDER_KEY_ALIAS, keyAlias);
    }

    public String getKeyAlias() {
        return propsMap.get(ATTR_SAML_PROVIDER_KEY_ALIAS);
    }

    public void setLoginPageURL(String loginPageURL) {
        propsMap.put(ATTR_SAML_PROVIDER_LOGIN_PAGE_URL, loginPageURL);
    }

    public String getLoginPageURL() {
        return propsMap.get(ATTR_SAML_PROVIDER_LOGIN_PAGE_URL);
    }

    public void setErrorPageURL(String errorPageURL) {
        propsMap.put(ATTR_SAML_PROVIDER_ERROR_PAGE_URL, errorPageURL);
    }

    public String getErrorPageURL() {
        return propsMap.get(ATTR_SAML_PROVIDER_ERROR_PAGE_URL);
    }

    public void setClockSkew(String clockSkew) {
        propsMap.put(ATTR_SAML_PROVIDER_CLOCK_SKEW, clockSkew);
    }

    public String getClockSkew() {
        return propsMap.get(ATTR_SAML_PROVIDER_CLOCK_SKEW);
    }

    public void setTokenReplayTimeout(String tokenReplayTimeout) {
        propsMap.put(ATTR_SAML_PROVIDER_TOKEN_REP_TIMEOUT, tokenReplayTimeout);
    }

    public String getTokenReplayTimeout() {
        return propsMap.get(ATTR_SAML_PROVIDER_TOKEN_REP_TIMEOUT);
    }

    public void setSessionNotOnOrAfter(String sessionNotOnOrAfter) {
        propsMap.put(ATTR_SAML_PROVIDER_SESSION_NOT_ON_OR_AFTER, sessionNotOnOrAfter);
    }

    public String getSessionNotOnOrAfter() {
        return propsMap.get(ATTR_SAML_PROVIDER_SESSION_NOT_ON_OR_AFTER);
    }

    public void setUserIdentifier(String userIdentifier) {
        propsMap.put(ATTR_SAML_PROVIDER_USER_IDENTIFIER, userIdentifier);
    }

    public String getUserIdentifier() {
        return propsMap.get(ATTR_SAML_PROVIDER_USER_IDENTIFIER);
    }

    public void setGroupIdentifier(String groupIdentifier) {
        propsMap.put(ATTR_SAML_PROVIDER_GROUP_IDENTIFIER, groupIdentifier);
    }

    public String getGroupIdentifier() {
        return propsMap.get(ATTR_SAML_PROVIDER_GROUP_IDENTIFIER);
    }

    public void setUserUniqueIdentifier(String userUniqueIdentifier) {
        propsMap.put(ATTR_SAML_PROVIDER_USER_UNIQUE_IDENTIFIER, userUniqueIdentifier);
    }

    public String getUserUniqueIdentifier() {
        return propsMap.get(ATTR_SAML_PROVIDER_USER_UNIQUE_IDENTIFIER);
    }

    public void setRealmIdentifier(String realmIdentifier) {
        propsMap.put(ATTR_SAML_PROVIDER_REALM_IDENTIFIER, realmIdentifier);
    }

    public String getRealmIdentifier() {
        return propsMap.get(ATTR_SAML_PROVIDER_REALM_IDENTIFIER);
    }

    public void setIncludeTokenInSubject(String includeTokenInSubject) {
        propsMap.put(ATTR_SAML_PROVIDER_INCLUDE_TOKEN_IN_SUBJECT, includeTokenInSubject);
    }

    public String getIncludeTokenInSubject() {
        return propsMap.get(ATTR_SAML_PROVIDER_INCLUDE_TOKEN_IN_SUBJECT);
    }

    public void setMapToUserRegistry(String mapToUserRegistry) {
        propsMap.put(ATTR_SAML_PROVIDER_MAP_TO_USER_REG, mapToUserRegistry);
    }

    public String getMapToUserRegistry() {
        return propsMap.get(ATTR_SAML_PROVIDER_MAP_TO_USER_REG);
    }

    public void setPkixTrustEngine(String pkixTrustEngine) {
        propsMap.put(ATTR_SAML_PROVIDER_PKIX_TRUST_ENGINE, pkixTrustEngine);
    }

    public String getPkixTrustEngine() {
        return propsMap.get(ATTR_SAML_PROVIDER_PKIX_TRUST_ENGINE);
    }

    public void setAuthFilterRef(String authFilterRef) {
        propsMap.put(ATTR_SAML_PROVIDER_AUTH_FILTER_REF, authFilterRef);
    }

    public String getAuthFilterRef() {
        return propsMap.get(ATTR_SAML_PROVIDER_AUTH_FILTER_REF);
    }

    public void setDisableLtpaCookie(String disableLtpaCookie) {
        propsMap.put(ATTR_SAML_PROVIDER_DISABLE_LTPA_COOKIE, disableLtpaCookie);
    }

    public String getDisableLtpaCookie() {
        return propsMap.get(ATTR_SAML_PROVIDER_DISABLE_LTPA_COOKIE);
    }

    public void setRealmName(String realmName) {
        propsMap.put(ATTR_SAML_PROVIDER_REALM_NAME, realmName);
    }

    public String getRealmName() {
        return propsMap.get(ATTR_SAML_PROVIDER_REALM_NAME);
    }

    public void setAuthnRequestTime(String authnRequestTime) {
        propsMap.put(ATTR_SAML_PROVIDER_AUTHN_REQ_TIME, authnRequestTime);
    }

    public String getAuthnRequestTime() {
        return propsMap.get(ATTR_SAML_PROVIDER_AUTHN_REQ_TIME);
    }

    public void setEnabled(String enabled) {
        propsMap.put(ATTR_SAML_PROVIDER_ENABLED, enabled);
    }

    public String getEnabled() {
        return propsMap.get(ATTR_SAML_PROVIDER_ENABLED);
    }

    public void setHttpsRequired(String httpsRequired) {
        propsMap.put(ATTR_SAML_PROVIDER_HTTPS_REQUIRED, httpsRequired);
    }

    public String getHttpsRequired() {
        return propsMap.get(ATTR_SAML_PROVIDER_HTTPS_REQUIRED);
    }

    public void setAllowCustomCacheKey(String allowCustomCacheKey) {
        propsMap.put(ATTR_SAML_PROVIDER_ALLOW_CUSTOM_CACHE_KEY, allowCustomCacheKey);
    }

    public String getAllowCustomCacheKey() {
        return propsMap.get(ATTR_SAML_PROVIDER_ALLOW_CUSTOM_CACHE_KEY);
    }

    public void setSpHostAndPort(String spHostAndPort) {
        propsMap.put(ATTR_SAML_PROVIDER_SP_HOST_AND_PORT, spHostAndPort);
    }

    public String getSpHostAndPort() {
        return propsMap.get(ATTR_SAML_PROVIDER_SP_HOST_AND_PORT);
    }

    /********************************* SAML propagation attributes *********************************/

    public void setHeaderName(String headerName) {
        propsMap.put(ATTR_SAML_PROVIDER_HEADER_NAME, headerName);
    }

    public String getHeaderName() {
        return propsMap.get(ATTR_SAML_PROVIDER_HEADER_NAME);
    }

    public void setAudiences(String audiences) {
        propsMap.put(ATTR_SAML_PROVIDER_AUDIENCES, audiences);
    }

    public String getAudiences() {
        return propsMap.get(ATTR_SAML_PROVIDER_AUDIENCES);
    }

    /*************************** Helpers to nullify pure SAML or SAML propagation attributes ***************************/

    /**
     * Sets all of the "pure" SAML config attributes to null.
     */
    public void nullifyPureSamlAttributes() {
        for (String pureSamlAttr : pureSamlAttributes) {
            propsMap.put(pureSamlAttr, null);
        }
    }

    /**
     * Sets all of the SAML token propagation related attributes to null.
     */
    public void nullifyPropagationAttributes() {
        for (String pureInboundAttr : pureInboundPropagationAttributes) {
            propsMap.put(pureInboundAttr, null);
        }
    }

    @Override
    public void printConfigSettings() {
        String thisMethod = "printConfigSettings";

        String indent = "  ";
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_ID + ": " + getId());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_INBOUND_PROPAGATION + ": " + getInboundPropagation());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_WANT_ASSERTIONS_SIGNED + ": " + getWantAssertionsSigned());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_SIG_METHOD_ALG + ": " + getSignatureMethodAlgorithm());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_CREATE_SESSION + ": " + getCreateSession());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_AUTHN_REQ_SIGNED + ": " + getAuthnRequestsSigned());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_FORCE_AUTHN + ": " + getForceAuthn());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_IS_PASSIVE + ": " + getIsPassive());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_ALLOW_CREATE + ": " + getAllowCreate());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_AUTHN_CONTEXT_CLASS_REF + ": " + getAuthnContextClassRef());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_AUTHN_CONTEXT_COMP_TYPE + ": " + getAuthnContextComparisonType());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_NAME_ID_FORMAT + ": " + getNameIDFormat());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_CUSTOMIZE_NAME_ID_FORMAT + ": " + getCustomizeNameIDFormat());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_IDP_METADATA + ": " + getIdpMetadata());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_KEYSTORE_REF + ": " + getKeyStoreRef());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_KEY_ALIAS + ": " + getKeyAlias());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_LOGIN_PAGE_URL + ": " + getLoginPageURL());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_ERROR_PAGE_URL + ": " + getErrorPageURL());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_CLOCK_SKEW + ": " + getClockSkew());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_TOKEN_REP_TIMEOUT + ": " + getTokenReplayTimeout());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_SESSION_NOT_ON_OR_AFTER + ": " + getSessionNotOnOrAfter());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_USER_IDENTIFIER + ": " + getUserIdentifier());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_GROUP_IDENTIFIER + ": " + getGroupIdentifier());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_USER_UNIQUE_IDENTIFIER + ": " + getUserUniqueIdentifier());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_REALM_IDENTIFIER + ": " + getRealmIdentifier());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_INCLUDE_TOKEN_IN_SUBJECT + ": " + getIncludeTokenInSubject());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_MAP_TO_USER_REG + ": " + getMapToUserRegistry());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_PKIX_TRUST_ENGINE + ": " + getPkixTrustEngine());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_AUTH_FILTER_REF + ": " + getAuthFilterRef());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_DISABLE_LTPA_COOKIE + ": " + getDisableLtpaCookie());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_REALM_NAME + ": " + getRealmName());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_AUTHN_REQ_TIME + ": " + getAuthnRequestTime());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_ENABLED + ": " + getEnabled());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_HTTPS_REQUIRED + ": " + getHttpsRequired());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_ALLOW_CUSTOM_CACHE_KEY + ": " + getAllowCustomCacheKey());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_SP_HOST_AND_PORT + ": " + getSpHostAndPort());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_HEADER_NAME + ": " + getHeaderName());
        Log.info(thisClass, thisMethod, indent + ATTR_SAML_PROVIDER_AUDIENCES + ": " + getAudiences());

    }

}
