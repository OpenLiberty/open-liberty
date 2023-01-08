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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Configuration for the following top-level elements:
 *
 * <ul>
 * <li>openidConnectClient</li>
 * </ul>
 */
public class OpenidConnectClient extends ConfigElement {

    private Boolean accessTokenCacheEnabled;
    private String accessTokenCacheTimeout;
    private Boolean accessTokenInLtpaCookie;
    private Boolean allowCustomCacheKey;
    private String audiences;
    private String authFilterRef;
    private String authenticationTimeLimit;
    private Boolean authnSessionDisabled;
    private String authorizationEndpointUrl;
    private String clientId;
    private String clientSecret;
    private String clockSkew;
    private Boolean createSession;
    private Boolean disableIssChecking;
    private Boolean disableLtpaCookie;
    private String discoveryEndpointUrl;
    private String discoveryPollingRate;
    private String forwardLoginParameter;
    private String grantType;
    private String groupIdentifier;
    private String headerName;
    private Boolean hostNameVerificationEnabled;
    private Boolean httpsRequired;
    private String inboundPropagation;
    private Boolean includeIdTokenInSubject;
    private String initialStateCacheCapacity;
    private Boolean isClientSideRedirectSupported;
    private String issuerIdentifier;
    private String jwkClientId;
    private String jwkClientSecret;
    private String jwkEndpointUrl;
    private String jwtAccessTokenRemoteValidation;
    private String keyManagementKeyAlias;
    private Boolean mapIdentityToRegistryUser;
    private Boolean nonceEnabled;
    private String reAuthnCushion;
    private Boolean reAuthnOnAccessTokenExpire;
    private String realmIdentifier;
    private String realmName;
    private String redirectJunctionPath;
    private String redirectToRPHostAndPort;
    private String resource;
    private String responseType;
    private String scope;
    private String signatureAlgorithm;
    private String sslRef;
    private String tokenEndpointAuthMethod;
    private String tokenEndpointUrl;
    private Boolean tokenReuse;
    private String trustAliasName;
    private String trustStoreRef;
    private String uniqueUserIdentifier;
    private Boolean useSystemPropertiesForHttpClientConnections;
    private String userIdentifier;
    private String userIdentityToCreateSubject;
    private Boolean userInfoEndpointEnabled;
    private String userInfoEndpointUrl;
    private String validationEndpointUrl;
    private String validationMethod;
    private TokenParameter tokenParameter;
    // TODO authzParameter

    /**
     * @return the accessTokenCacheEnabled
     */
    public Boolean getAccessTokenCacheEnabled() {
        return accessTokenCacheEnabled;
    }

    /**
     * @param accessTokenCacheEnabled the accessTokenCacheEnabled to set
     */
    @XmlAttribute(name = "accessTokenCacheEnabled")
    public void setAccessTokenCacheEnabled(Boolean accessTokenCacheEnabled) {
        this.accessTokenCacheEnabled = accessTokenCacheEnabled;
    }

    /**
     * @return the accessTokenCacheTimeout
     */
    public String getAccessTokenCacheTimeout() {
        return accessTokenCacheTimeout;
    }

    /**
     * @param accessTokenCacheTimeout the accessTokenCacheTimeout to set
     */
    @XmlAttribute(name = "accessTokenCacheTimeout")
    public void setAccessTokenCacheTimeout(String accessTokenCacheTimeout) {
        this.accessTokenCacheTimeout = accessTokenCacheTimeout;
    }

    /**
     * @return the accessTokenInLtpaCookie
     */
    public Boolean getAccessTokenInLtpaCookie() {
        return accessTokenInLtpaCookie;
    }

    /**
     * @param accessTokenInLtpaCookie the accessTokenInLtpaCookie to set
     */
    @XmlAttribute(name = "accessTokenInLtpaCookie")
    public void setAccessTokenInLtpaCookie(Boolean accessTokenInLtpaCookie) {
        this.accessTokenInLtpaCookie = accessTokenInLtpaCookie;
    }

    /**
     * @return the allowCustomCacheKey
     */
    public Boolean getAllowCustomCacheKey() {
        return allowCustomCacheKey;
    }

    /**
     * @param allowCustomCacheKey the allowCustomCacheKey to set
     */
    @XmlAttribute(name = "allowCustomCacheKey")
    public void setAllowCustomCacheKey(Boolean allowCustomCacheKey) {
        this.allowCustomCacheKey = allowCustomCacheKey;
    }

    /**
     * @return the audiences
     */
    public String getAudiences() {
        return audiences;
    }

    /**
     * @param audiences the audiences to set
     */
    @XmlAttribute(name = "audiences")
    public void setAudiences(String audiences) {
        this.audiences = audiences;
    }

    /**
     * @return the authFilterRef
     */
    public String getAuthFilterRef() {
        return authFilterRef;
    }

    /**
     * @param authFilterRef the authFilterRef to set
     */
    @XmlAttribute(name = "authFilterRef")
    public void setAuthFilterRef(String authFilterRef) {
        this.authFilterRef = authFilterRef;
    }

    /**
     * @return the authenticationTimeLimit
     */
    public String getAuthenticationTimeLimit() {
        return authenticationTimeLimit;
    }

    /**
     * @param authenticationTimeLimit the authenticationTimeLimit to set
     */
    @XmlAttribute(name = "authenticationTimeLimit")
    public void setAuthenticationTimeLimit(String authenticationTimeLimit) {
        this.authenticationTimeLimit = authenticationTimeLimit;
    }

    /**
     * @return the authnSessionDisabled
     */
    public Boolean getAuthnSessionDisabled() {
        return authnSessionDisabled;
    }

    /**
     * @param authnSessionDisabled the authnSessionDisabled to set
     */
    @XmlAttribute(name = "authnSessionDisabled")
    public void setAuthnSessionDisabled(Boolean authnSessionDisabled) {
        this.authnSessionDisabled = authnSessionDisabled;
    }

    /**
     * @return the authorizationEndpointUrl
     */
    public String getAuthorizationEndpointUrl() {
        return authorizationEndpointUrl;
    }

    /**
     * @param authorizationEndpointUrl the authorizationEndpointUrl to set
     */
    @XmlAttribute(name = "authorizationEndpointUrl")
    public void setAuthorizationEndpointUrl(String authorizationEndpointUrl) {
        this.authorizationEndpointUrl = authorizationEndpointUrl;
    }

    /**
     * @return the clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @param clientId the clientId to set
     */
    @XmlAttribute(name = "clientId")
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * @return the clientSecret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * @param clientSecret the clientSecret to set
     */
    @XmlAttribute(name = "clientSecret")
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * @return the clockSkew
     */
    public String getClockSkew() {
        return clockSkew;
    }

    /**
     * @param clockSkew the clockSkew to set
     */
    @XmlAttribute(name = "clientSecret")
    public void setClockSkew(String clockSkew) {
        this.clockSkew = clockSkew;
    }

    /**
     * @return the createSession
     */
    public Boolean getCreateSession() {
        return createSession;
    }

    /**
     * @param createSession the createSession to set
     */
    @XmlAttribute(name = "createSession")
    public void setCreateSession(Boolean createSession) {
        this.createSession = createSession;
    }

    /**
     * @return the disableIssChecking
     */
    public Boolean getDisableIssChecking() {
        return disableIssChecking;
    }

    /**
     * @param disableIssChecking the disableIssChecking to set
     */
    @XmlAttribute(name = "disableIssChecking")
    public void setDisableIssChecking(Boolean disableIssChecking) {
        this.disableIssChecking = disableIssChecking;
    }

    /**
     * @return the disableLtpaCookie
     */
    public Boolean getDisableLtpaCookie() {
        return disableLtpaCookie;
    }

    /**
     * @param disableLtpaCookie the disableLtpaCookie to set
     */
    @XmlAttribute(name = "disableLtpaCookie")
    public void setDisableLtpaCookie(Boolean disableLtpaCookie) {
        this.disableLtpaCookie = disableLtpaCookie;
    }

    /**
     * @return the discoveryEndpointUrl
     */
    public String getDiscoveryEndpointUrl() {
        return discoveryEndpointUrl;
    }

    /**
     * @param discoveryEndpointUrl the discoveryEndpointUrl to set
     */
    @XmlAttribute(name = "discoveryEndpointUrl")
    public void setDiscoveryEndpointUrl(String discoveryEndpointUrl) {
        this.discoveryEndpointUrl = discoveryEndpointUrl;
    }

    /**
     * @return the discoveryPollingRate
     */
    public String getDiscoveryPollingRate() {
        return discoveryPollingRate;
    }

    /**
     * @param discoveryPollingRate the discoveryPollingRate to set
     */
    @XmlAttribute(name = "discoveryPollingRate")
    public void setDiscoveryPollingRate(String discoveryPollingRate) {
        this.discoveryPollingRate = discoveryPollingRate;
    }

    /**
     * @return the forwardLoginParameter
     */
    public String getForwardLoginParameter() {
        return forwardLoginParameter;
    }

    /**
     * @param forwardLoginParameter the forwardLoginParameter to set
     */
    @XmlAttribute(name = "forwardLoginParameter")
    public void setForwardLoginParameter(String forwardLoginParameter) {
        this.forwardLoginParameter = forwardLoginParameter;
    }

    /**
     * @return the grantType
     */
    public String getGrantType() {
        return grantType;
    }

    /**
     * @param grantType the grantType to set
     */
    @XmlAttribute(name = "grantType")
    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    /**
     * @return the groupIdentifier
     */
    public String getGroupIdentifier() {
        return groupIdentifier;
    }

    /**
     * @param groupIdentifier the groupIdentifier to set
     */
    @XmlAttribute(name = "groupIdentifier")
    public void setGroupIdentifier(String groupIdentifier) {
        this.groupIdentifier = groupIdentifier;
    }

    /**
     * @return the headerName
     */
    public String getHeaderName() {
        return headerName;
    }

    /**
     * @param headerName the headerName to set
     */
    @XmlAttribute(name = "headerName")
    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    /**
     * @return the hostNameVerificationEnabled
     */
    public Boolean getHostNameVerificationEnabled() {
        return hostNameVerificationEnabled;
    }

    /**
     * @param hostNameVerificationEnabled the hostNameVerificationEnabled to set
     */
    @XmlAttribute(name = "hostNameVerificationEnabled")
    public void setHostNameVerificationEnabled(Boolean hostNameVerificationEnabled) {
        this.hostNameVerificationEnabled = hostNameVerificationEnabled;
    }

    /**
     * @return the httpsRequired
     */
    public Boolean getHttpsRequired() {
        return httpsRequired;
    }

    /**
     * @param httpsRequired the httpsRequired to set
     */
    @XmlAttribute(name = "httpsRequired")
    public void setHttpsRequired(Boolean httpsRequired) {
        this.httpsRequired = httpsRequired;
    }

    /**
     * @return the inboundPropagation
     */
    public String getInboundPropagation() {
        return inboundPropagation;
    }

    /**
     * @param inboundPropagation the inboundPropagation to set
     */
    @XmlAttribute(name = "inboundPropagation")
    public void setInboundPropagation(String inboundPropagation) {
        this.inboundPropagation = inboundPropagation;
    }

    /**
     * @return the includeIdTokenInSubject
     */
    public Boolean getIncludeIdTokenInSubject() {
        return includeIdTokenInSubject;
    }

    /**
     * @param includeIdTokenInSubject the includeIdTokenInSubject to set
     */
    @XmlAttribute(name = "includeIdTokenInSubject")
    public void setIncludeIdTokenInSubject(Boolean includeIdTokenInSubject) {
        this.includeIdTokenInSubject = includeIdTokenInSubject;
    }

    /**
     * @return the initialStateCacheCapacity
     */
    public String getInitialStateCacheCapacity() {
        return initialStateCacheCapacity;
    }

    /**
     * @param initialStateCacheCapacity the initialStateCacheCapacity to set
     */
    @XmlAttribute(name = "initialStateCacheCapacity")
    public void setInitialStateCacheCapacity(String initialStateCacheCapacity) {
        this.initialStateCacheCapacity = initialStateCacheCapacity;
    }

    /**
     * @return the isClientSideRedirectSupported
     */
    public Boolean getIsClientSideRedirectSupported() {
        return isClientSideRedirectSupported;
    }

    /**
     * @param isClientSideRedirectSupported the isClientSideRedirectSupported to set
     */
    @XmlAttribute(name = "isClientSideRedirectSupported")
    public void setIsClientSideRedirectSupported(Boolean isClientSideRedirectSupported) {
        this.isClientSideRedirectSupported = isClientSideRedirectSupported;
    }

    /**
     * @return the issuerIdentifier
     */
    public String getIssuerIdentifier() {
        return issuerIdentifier;
    }

    /**
     * @param issuerIdentifier the issuerIdentifier to set
     */
    @XmlAttribute(name = "issuerIdentifier")
    public void setIssuerIdentifier(String issuerIdentifier) {
        this.issuerIdentifier = issuerIdentifier;
    }

    /**
     * @return the jwkClientId
     */
    public String getJwkClientId() {
        return jwkClientId;
    }

    /**
     * @param jwkClientId the jwkClientId to set
     */
    @XmlAttribute(name = "jwkClientId")
    public void setJwkClientId(String jwkClientId) {
        this.jwkClientId = jwkClientId;
    }

    /**
     * @return the jwkClientSecret
     */
    public String getJwkClientSecret() {
        return jwkClientSecret;
    }

    /**
     * @param jwkClientSecret the jwkClientSecret to set
     */
    @XmlAttribute(name = "jwkClientSecret")
    public void setJwkClientSecret(String jwkClientSecret) {
        this.jwkClientSecret = jwkClientSecret;
    }

    /**
     * @return the jwkEndpointUrl
     */
    public String getJwkEndpointUrl() {
        return jwkEndpointUrl;
    }

    /**
     * @param jwkEndpointUrl the jwkEndpointUrl to set
     */
    @XmlAttribute(name = "jwkEndpointUrl")
    public void setJwkEndpointUrl(String jwkEndpointUrl) {
        this.jwkEndpointUrl = jwkEndpointUrl;
    }

    /**
     * @return the jwtAccessTokenRemoteValidation
     */
    public String getJwtAccessTokenRemoteValidation() {
        return jwtAccessTokenRemoteValidation;
    }

    /**
     * @param jwtAccessTokenRemoteValidation the jwtAccessTokenRemoteValidation to set
     */
    @XmlAttribute(name = "jwtAccessTokenRemoteValidation")
    public void setJwtAccessTokenRemoteValidation(String jwtAccessTokenRemoteValidation) {
        this.jwtAccessTokenRemoteValidation = jwtAccessTokenRemoteValidation;
    }

    /**
     * @return the keyManagementKeyAlias
     */
    public String getKeyManagementKeyAlias() {
        return keyManagementKeyAlias;
    }

    /**
     * @param keyManagementKeyAlias the keyManagementKeyAlias to set
     */
    @XmlAttribute(name = "keyManagementKeyAlias")
    public void setKeyManagementKeyAlias(String keyManagementKeyAlias) {
        this.keyManagementKeyAlias = keyManagementKeyAlias;
    }

    /**
     * @return the mapIdentityToRegistryUser
     */
    public Boolean getMapIdentityToRegistryUser() {
        return mapIdentityToRegistryUser;
    }

    /**
     * @param mapIdentityToRegistryUser the mapIdentityToRegistryUser to set
     */
    @XmlAttribute(name = "mapIdentityToRegistryUser")
    public void setMapIdentityToRegistryUser(Boolean mapIdentityToRegistryUser) {
        this.mapIdentityToRegistryUser = mapIdentityToRegistryUser;
    }

    /**
     * @return the nonceEnabled
     */
    public Boolean getNonceEnabled() {
        return nonceEnabled;
    }

    /**
     * @param nonceEnabled the nonceEnabled to set
     */
    @XmlAttribute(name = "nonceEnabled")
    public void setNonceEnabled(Boolean nonceEnabled) {
        this.nonceEnabled = nonceEnabled;
    }

    /**
     * @return the reAuthnCushion
     */
    public String getReAuthnCushion() {
        return reAuthnCushion;
    }

    /**
     * @param reAuthnCushion the reAuthnCushion to set
     */
    @XmlAttribute(name = "reAuthnCushion")
    public void setReAuthnCushion(String reAuthnCushion) {
        this.reAuthnCushion = reAuthnCushion;
    }

    /**
     * @return the reAuthnOnAccessTokenExpire
     */
    public Boolean getReAuthnOnAccessTokenExpire() {
        return reAuthnOnAccessTokenExpire;
    }

    /**
     * @param reAuthnOnAccessTokenExpire the reAuthnOnAccessTokenExpire to set
     */
    @XmlAttribute(name = "reAuthnOnAccessTokenExpire")
    public void setReAuthnOnAccessTokenExpire(Boolean reAuthnOnAccessTokenExpire) {
        this.reAuthnOnAccessTokenExpire = reAuthnOnAccessTokenExpire;
    }

    /**
     * @return the realmIdentifier
     */
    public String getRealmIdentifier() {
        return realmIdentifier;
    }

    /**
     * @param realmIdentifier the realmIdentifier to set
     */
    @XmlAttribute(name = "realmIdentifier")
    public void setRealmIdentifier(String realmIdentifier) {
        this.realmIdentifier = realmIdentifier;
    }

    /**
     * @return the realmName
     */
    public String getRealmName() {
        return realmName;
    }

    /**
     * @param realmName the realmName to set
     */
    @XmlAttribute(name = "realmName")
    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    /**
     * @return the redirectJunctionPath
     */
    public String getRedirectJunctionPath() {
        return redirectJunctionPath;
    }

    /**
     * @param redirectJunctionPath the redirectJunctionPath to set
     */
    @XmlAttribute(name = "redirectJunctionPath")
    public void setRedirectJunctionPath(String redirectJunctionPath) {
        this.redirectJunctionPath = redirectJunctionPath;
    }

    /**
     * @return the redirectToRPHostAndPort
     */
    public String getRedirectToRPHostAndPort() {
        return redirectToRPHostAndPort;
    }

    /**
     * @param redirectToRPHostAndPort the redirectToRPHostAndPort to set
     */
    @XmlAttribute(name = "redirectToRPHostAndPort")
    public void setRedirectToRPHostAndPort(String redirectToRPHostAndPort) {
        this.redirectToRPHostAndPort = redirectToRPHostAndPort;
    }

    /**
     * @return the resource
     */
    public String getResource() {
        return resource;
    }

    /**
     * @param resource the resource to set
     */
    @XmlAttribute(name = "resource")
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     * @return the responseType
     */
    public String getResponseType() {
        return responseType;
    }

    /**
     * @param responseType the responseType to set
     */
    @XmlAttribute(name = "responseType")
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    /**
     * @return the scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * @param scope the scope to set
     */
    @XmlAttribute(name = "scope")
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * @return the signatureAlgorithm
     */
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    /**
     * @param signatureAlgorithm the signatureAlgorithm to set
     */
    @XmlAttribute(name = "signatureAlgorithm")
    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    /**
     * @return the sslRef
     */
    public String getSslRef() {
        return sslRef;
    }

    /**
     * @param sslRef the sslRef to set
     */
    @XmlAttribute(name = "sslRef")
    public void setSslRef(String sslRef) {
        this.sslRef = sslRef;
    }

    /**
     * @return the tokenEndpointAuthMethod
     */
    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    /**
     * @param tokenEndpointAuthMethod the tokenEndpointAuthMethod to set
     */
    @XmlAttribute(name = "tokenEndpointAuthMethod")
    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    /**
     * @return the tokenEndpointUrl
     */
    public String getTokenEndpointUrl() {
        return tokenEndpointUrl;
    }

    /**
     * @param tokenEndpointUrl the tokenEndpointUrl to set
     */
    @XmlAttribute(name = "tokenEndpointUrl")
    public void setTokenEndpointUrl(String tokenEndpointUrl) {
        this.tokenEndpointUrl = tokenEndpointUrl;
    }

    /**
     * @return the tokenParameter
     */
    public TokenParameter getTokenParameter() {
        return tokenParameter;
    }

    /**
     * @param tokenParameter the tokenParameter to set
     */
    @XmlElement(name = "tokenParameter")
    public void setTokenParameter(TokenParameter tokenParameter) {
        this.tokenParameter = tokenParameter;
    }

    /**
     * @return the tokenReuse
     */
    public Boolean getTokenReuse() {
        return tokenReuse;
    }

    /**
     * @param tokenReuse the tokenReuse to set
     */
    @XmlAttribute(name = "tokenReuse")
    public void setTokenReuse(Boolean tokenReuse) {
        this.tokenReuse = tokenReuse;
    }

    /**
     * @return the trustAliasName
     */
    public String getTrustAliasName() {
        return trustAliasName;
    }

    /**
     * @param trustAliasName the trustAliasName to set
     */
    @XmlAttribute(name = "trustAliasName")
    public void setTrustAliasName(String trustAliasName) {
        this.trustAliasName = trustAliasName;
    }

    /**
     * @return the trustStoreRef
     */
    public String getTrustStoreRef() {
        return trustStoreRef;
    }

    /**
     * @param trustStoreRef the trustStoreRef to set
     */
    @XmlAttribute(name = "trustStoreRef")
    public void setTrustStoreRef(String trustStoreRef) {
        this.trustStoreRef = trustStoreRef;
    }

    /**
     * @return the uniqueUserIdentifier
     */
    public String getUniqueUserIdentifier() {
        return uniqueUserIdentifier;
    }

    /**
     * @param uniqueUserIdentifier the uniqueUserIdentifier to set
     */
    @XmlAttribute(name = "uniqueUserIdentifier")
    public void setUniqueUserIdentifier(String uniqueUserIdentifier) {
        this.uniqueUserIdentifier = uniqueUserIdentifier;
    }

    /**
     * @return the useSystemPropertiesForHttpClientConnections
     */
    public Boolean getUseSystemPropertiesForHttpClientConnections() {
        return useSystemPropertiesForHttpClientConnections;
    }

    /**
     * @param useSystemPropertiesForHttpClientConnections the useSystemPropertiesForHttpClientConnections to set
     */
    @XmlAttribute(name = "useSystemPropertiesForHttpClientConnections")
    public void setUseSystemPropertiesForHttpClientConnections(Boolean useSystemPropertiesForHttpClientConnections) {
        this.useSystemPropertiesForHttpClientConnections = useSystemPropertiesForHttpClientConnections;
    }

    /**
     * @return the userIdentifier
     */
    public String getUserIdentifier() {
        return userIdentifier;
    }

    /**
     * @param userIdentifier the userIdentifier to set
     */
    @XmlAttribute(name = "userIdentifier")
    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    /**
     * @return the userIdentityToCreateSubject
     */
    public String getUserIdentityToCreateSubject() {
        return userIdentityToCreateSubject;
    }

    /**
     * @param userIdentityToCreateSubject the userIdentityToCreateSubject to set
     */
    @XmlAttribute(name = "userIdentityToCreateSubject")
    public void setUserIdentityToCreateSubject(String userIdentityToCreateSubject) {
        this.userIdentityToCreateSubject = userIdentityToCreateSubject;
    }

    /**
     * @return the userInfoEndpointEnabled
     */
    public Boolean getUserInfoEndpointEnabled() {
        return userInfoEndpointEnabled;
    }

    /**
     * @param userInfoEndpointEnabled the userInfoEndpointEnabled to set
     */
    @XmlAttribute(name = "userInfoEndpointEnabled")
    public void setUserInfoEndpointEnabled(Boolean userInfoEndpointEnabled) {
        this.userInfoEndpointEnabled = userInfoEndpointEnabled;
    }

    /**
     * @return the userInfoEndpointUrl
     */
    public String getUserInfoEndpointUrl() {
        return userInfoEndpointUrl;
    }

    /**
     * @param userInfoEndpointUrl the userInfoEndpointUrl to set
     */
    @XmlAttribute(name = "userInfoEndpointUrl")
    public void setUserInfoEndpointUrl(String userInfoEndpointUrl) {
        this.userInfoEndpointUrl = userInfoEndpointUrl;
    }

    /**
     * @return the validationEndpointUrl
     */
    public String getValidationEndpointUrl() {
        return validationEndpointUrl;
    }

    /**
     * @param validationEndpointUrl the validationEndpointUrl to set
     */
    @XmlAttribute(name = "validationEndpointUrl")
    public void setValidationEndpointUrl(String validationEndpointUrl) {
        this.validationEndpointUrl = validationEndpointUrl;
    }

    /**
     * @return the validationMethod
     */
    public String getValidationMethod() {
        return validationMethod;
    }

    /**
     * @param validationMethod the validationMethod to set
     */
    @XmlAttribute(name = "validationMethod")
    public void setValidationMethod(String validationMethod) {
        this.validationMethod = validationMethod;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (accessTokenCacheEnabled != null) {
            sb.append("accessTokenCacheEnabled=\"").append(accessTokenCacheEnabled).append("\" ");
        }
        if (accessTokenCacheTimeout != null) {
            sb.append("accessTokenCacheTimeout=\"").append(accessTokenCacheTimeout).append("\" ");
        }
        if (accessTokenInLtpaCookie != null) {
            sb.append("accessTokenInLtpaCookie=\"").append(accessTokenInLtpaCookie).append("\" ");
        }
        if (allowCustomCacheKey != null) {
            sb.append("allowCustomCacheKey=\"").append(allowCustomCacheKey).append("\" ");
        }
        if (audiences != null) {
            sb.append("audiences=\"").append(audiences).append("\" ");
        }
        if (authenticationTimeLimit != null) {
            sb.append("authenticationTimeLimit=\"").append(authenticationTimeLimit).append("\" ");
        }
        if (authFilterRef != null) {
            sb.append("authFilterRef=\"").append(authFilterRef).append("\" ");
        }
        if (authnSessionDisabled != null) {
            sb.append("authnSessionDisabled=\"").append(authnSessionDisabled).append("\" ");
        }
        if (authorizationEndpointUrl != null) {
            sb.append("authorizationEndpointUrl=\"").append(authorizationEndpointUrl).append("\" ");
        }
        if (clientId != null) {
            sb.append("clientId=\"").append(clientId).append("\" ");
        }
        if (clientSecret != null) {
            sb.append("clientSecret").append(clientSecret).append("\" ");
        }
        if (clockSkew != null) {
            sb.append("clockSkew=\"").append(clockSkew).append("\" ");
        }
        if (createSession != null) {
            sb.append("createSession=\"").append(createSession).append("\" ");
        }
        if (disableIssChecking != null) {
            sb.append("disableIssChecking=\"").append(disableIssChecking).append("\" ");
        }
        if (disableLtpaCookie != null) {
            sb.append("disableLtpaCookie=\"").append(disableLtpaCookie).append("\" ");
        }
        if (discoveryEndpointUrl != null) {
            sb.append("discoveryEndpointUrl=\"").append(discoveryEndpointUrl).append("\" ");
        }
        if (discoveryPollingRate != null) {
            sb.append("discoveryPollingRate=\"").append(discoveryPollingRate).append("\" ");
        }
        if (forwardLoginParameter != null) {
            sb.append("forwardLoginParameter=\"").append(forwardLoginParameter).append("\" ");
        }
        if (grantType != null) {
            sb.append("grantType=\"").append(grantType).append("\" ");
        }
        if (groupIdentifier != null) {
            sb.append("groupIdentifier=\"").append(groupIdentifier).append("\" ");
        }
        if (headerName != null) {
            sb.append("headerName=\"").append(headerName).append("\" ");
        }
        if (hostNameVerificationEnabled != null) {
            sb.append("hostNameVerificationEnabled=\"").append(hostNameVerificationEnabled).append("\" ");
        }
        if (httpsRequired != null) {
            sb.append("httpsRequired=\"").append(httpsRequired).append("\" ");
        }
        if (inboundPropagation != null) {
            sb.append("inboundPropagation=\"").append(inboundPropagation).append("\" ");
        }
        if (includeIdTokenInSubject != null) {
            sb.append("includeIdTokenInSubject=\"").append(includeIdTokenInSubject).append("\" ");
        }
        if (initialStateCacheCapacity != null) {
            sb.append("initialStateCacheCapacity=\"").append(initialStateCacheCapacity).append("\" ");
        }
        if (isClientSideRedirectSupported != null) {
            sb.append("isClientSideRedirectSupported=\"").append(isClientSideRedirectSupported).append("\" ");
        }
        if (issuerIdentifier != null) {
            sb.append("issuerIdentifier=\"").append(issuerIdentifier).append("\" ");
        }
        if (jwkClientId != null) {
            sb.append("jwkClientId=\"").append(jwkClientId).append("\" ");
        }
        if (jwkClientSecret != null) {
            sb.append("jwkClientSecret=\"").append(jwkClientSecret).append("\" ");
        }
        if (jwkEndpointUrl != null) {
            sb.append("jwkEndpointUrl=\"").append(jwkEndpointUrl).append("\" ");
        }
        if (jwtAccessTokenRemoteValidation != null) {
            sb.append("jwtAccessTokenRemoteValidation=\"").append(jwtAccessTokenRemoteValidation).append("\" ");
        }
        if (keyManagementKeyAlias != null) {
            sb.append("keyManagementKeyAlias=\"").append(keyManagementKeyAlias).append("\" ");
        }
        if (mapIdentityToRegistryUser != null) {
            sb.append("mapIdentityToRegistryUser=\"").append(mapIdentityToRegistryUser).append("\" ");
        }
        if (nonceEnabled != null) {
            sb.append("nonceEnabled=\"").append(nonceEnabled).append("\" ");
        }
        if (realmIdentifier != null) {
            sb.append("realmIdentifier=\"").append(realmIdentifier).append("\" ");
        }
        if (realmName != null) {
            sb.append("realmName=\"").append(realmName).append("\" ");
        }
        if (reAuthnCushion != null) {
            sb.append("reAuthnCushion=\"").append(reAuthnCushion).append("\" ");
        }
        if (reAuthnOnAccessTokenExpire != null) {
            sb.append("reAuthnOnAccessTokenExpire=\"").append(reAuthnOnAccessTokenExpire).append("\" ");
        }
        if (redirectJunctionPath != null) {
            sb.append("redirectJunctionPath=\"").append(redirectJunctionPath).append("\" ");
        }
        if (redirectToRPHostAndPort != null) {
            sb.append("redirectToRPHostAndPort=\"").append(redirectToRPHostAndPort).append("\" ");
        }
        if (resource != null) {
            sb.append("resource=\"").append(resource).append("\" ");
        }
        if (responseType != null) {
            sb.append("responseType=\"").append(responseType).append("\" ");
        }
        if (scope != null) {
            sb.append("scope=\"").append(scope).append("\" ");
        }
        if (signatureAlgorithm != null) {
            sb.append("signatureAlgorithm=\"").append(signatureAlgorithm).append("\" ");
        }
        if (sslRef != null) {
            sb.append("sslRef=\"").append(sslRef).append("\" ");
        }
        if (tokenEndpointAuthMethod != null) {
            sb.append("tokenEndpointAuthMethod=\"").append(tokenEndpointAuthMethod).append("\" ");
        }
        if (tokenEndpointUrl != null) {
            sb.append("tokenEndpointUrl=\"").append(tokenEndpointUrl).append("\" ");
        }
        if (tokenParameter != null) {
            sb.append("tokenParameter=\"").append(tokenParameter).append("\" ");
        }
        if (tokenReuse != null) {
            sb.append("tokenReuse=\"").append(tokenReuse).append("\" ");
        }
        if (trustAliasName != null) {
            sb.append("trustAliasName=\"").append(trustAliasName).append("\" ");
        }
        if (trustStoreRef != null) {
            sb.append("trustStoreRef=\"").append(trustStoreRef).append("\" ");
        }
        if (uniqueUserIdentifier != null) {
            sb.append("uniqueUserIdentifier=\"").append(uniqueUserIdentifier).append("\" ");
        }
        if (userIdentifier != null) {
            sb.append("userIdentifier=\"").append(userIdentifier).append("\" ");
        }
        if (userIdentityToCreateSubject != null) {
            sb.append("userIdentityToCreateSubject=\"").append(userIdentityToCreateSubject).append("\" ");
        }
        if (userInfoEndpointEnabled != null) {
            sb.append("userInfoEndpointEnabled=\"").append(userInfoEndpointEnabled).append("\" ");
        }
        if (userInfoEndpointUrl != null) {
            sb.append("userInfoEndpointUrl=\"").append(userInfoEndpointUrl).append("\" ");
        }
        if (useSystemPropertiesForHttpClientConnections != null) {
            sb.append("useSystemPropertiesForHttpClientConnections=\"").append(useSystemPropertiesForHttpClientConnections).append("\" ");
        }
        if (validationEndpointUrl != null) {
            sb.append("validationEndpointUrl=\"").append(validationEndpointUrl).append("\" ");
        }
        if (validationMethod != null) {
            sb.append("validationMethod=\"").append(validationMethod).append("\" ");
        }

        sb.append("}");

        return sb.toString();
    }
}
